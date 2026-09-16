package com.personal.ai.shivai.core.security

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

class APKSecurityScanner(private val context: Context) {

    private val dangerousPermissions = setOf(
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN"
    )

    fun scanApkFile(apkPath: String): ApkAnalysisResult {
        val file = File(apkPath)
        if (!file.exists() || !file.canRead()) {
            return ApkAnalysisResult(
                targetPathOrPackage = apkPath,
                packageName = "unknown",
                versionName = "unknown",
                versionCode = 0,
                riskLevel = SecurityRiskLevel.HIGH,
                suspiciousPermissions = emptyList(),
                allPermissions = emptyList(),
                signatures = emptyList(),
                warnings = listOf("APK file does not exist or is unreadable.")
            )
        }

        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        }

        val packageInfo = pm.getPackageArchiveInfo(apkPath, flags)
        if (packageInfo == null) {
            return ApkAnalysisResult(
                targetPathOrPackage = apkPath,
                packageName = "corrupt",
                versionName = "unknown",
                versionCode = 0,
                riskLevel = SecurityRiskLevel.HIGH,
                suspiciousPermissions = emptyList(),
                allPermissions = emptyList(),
                signatures = emptyList(),
                warnings = listOf("Failed to parse APK archive metadata. The file may be corrupted or obfuscated.")
            )
        }

        val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val suspicious = permissions.filter { dangerousPermissions.contains(it) }
        val warnings = mutableListOf<String>()

        // Dangerous Permission Combination Analysis
        val hasSms = permissions.any { it.contains("SMS") }
        val hasBoot = permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
        val hasOverlay = permissions.contains("android.permission.SYSTEM_ALERT_WINDOW")
        val hasInstall = permissions.contains("android.permission.REQUEST_INSTALL_PACKAGES")
        val hasAccessibility = permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE")

        if (hasSms && hasBoot) {
            warnings.add("SMS read/send combined with boot receiver (Spyware/Toll-fraud pattern)")
        }
        if (hasOverlay && hasInstall) {
            warnings.add("System overlay combined with package installer requests (Dropper/Tapjacking pattern)")
        }
        if (hasAccessibility) {
            warnings.add("Declares Accessibility Service binding (requires rigorous review)")
        }

        val signatures = extractSignatures(packageInfo)

        val riskLevel = when {
            warnings.size >= 2 -> SecurityRiskLevel.CRITICAL
            suspicious.size >= 4 -> SecurityRiskLevel.HIGH
            suspicious.isNotEmpty() -> SecurityRiskLevel.MEDIUM
            else -> SecurityRiskLevel.LOW
        }

        val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        return ApkAnalysisResult(
            targetPathOrPackage = apkPath,
            packageName = packageInfo.packageName ?: "unknown",
            versionName = packageInfo.versionName ?: "1.0",
            versionCode = vCode,
            riskLevel = riskLevel,
            suspiciousPermissions = suspicious,
            allPermissions = permissions,
            signatures = signatures,
            warnings = warnings
        )
    }

    private fun extractSignatures(info: PackageInfo): List<String> {
        val result = mutableListOf<String>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo
                if (signingInfo != null) {
                    val sigs = if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                    sigs?.forEach { sig ->
                        val md = MessageDigest.getInstance("SHA-256")
                        val digest = md.digest(sig.toByteArray())
                        result.add(digest.joinToString(":") { "%02X".format(it) })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.forEach { sig ->
                    val md = MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(sig.toByteArray())
                    result.add(digest.joinToString(":") { "%02X".format(it) })
                }
            }
        } catch (e: Exception) {
            result.add("Error extracting signature: ${e.message}")
        }
        return result
    }
}
