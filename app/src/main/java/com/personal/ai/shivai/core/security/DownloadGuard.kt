package com.personal.ai.shivai.core.security

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Locale

class DownloadGuard {

    private val suspiciousDoubleExtensions = listOf(
        ".pdf.apk", ".doc.apk", ".docx.apk", ".jpg.apk", ".png.apk",
        ".pdf.jar", ".txt.vbs", ".zip.exe", ".mp4.apk"
    )

    fun inspectFile(file: File): FileGuardResult {
        if (!file.exists() || !file.canRead()) {
            return FileGuardResult(
                fileName = file.name,
                fileSize = 0,
                sha256 = "N/A",
                mimeType = "unknown",
                riskLevel = SecurityRiskLevel.HIGH,
                warnings = listOf("File does not exist or access was denied.")
            )
        }

        val name = file.name.lowercase(Locale.ROOT)
        val size = file.length()
        val warnings = mutableListOf<String>()

        // 1. Double extension check
        for (pattern in suspiciousDoubleExtensions) {
            if (name.endsWith(pattern)) {
                warnings.add("Deceptive double extension detected ($pattern)")
            }
        }

        // 2. SHA-256 Hash calculation
        val sha256 = calculateSha256(file)

        // 3. Header inspection (Magic Bytes: "PK" for zip/apk)
        val header = readHeaderBytes(file, 4)
        val isZipOrApk = header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

        if (name.endsWith(".apk") && !isZipOrApk) {
            warnings.add("File has .apk extension but invalid PK zip magic header (corrupted or spoofed).")
        }

        val riskLevel = when {
            warnings.any { it.contains("double extension") } -> SecurityRiskLevel.CRITICAL
            warnings.isNotEmpty() -> SecurityRiskLevel.HIGH
            name.endsWith(".apk") -> SecurityRiskLevel.MEDIUM
            else -> SecurityRiskLevel.SAFE
        }

        return FileGuardResult(
            fileName = file.name,
            fileSize = size,
            sha256 = sha256,
            mimeType = if (name.endsWith(".apk")) "application/vnd.android.package-archive" else "application/octet-stream",
            riskLevel = riskLevel,
            warnings = warnings
        )
    }

    private fun calculateSha256(file: File): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "error:${e.message}"
        }
    }

    private fun readHeaderBytes(file: File, count: Int): ByteArray {
        return try {
            FileInputStream(file).use { fis ->
                val buf = ByteArray(count)
                val read = fis.read(buf)
                if (read > 0) buf.copyOf(read) else ByteArray(0)
            }
        } catch (e: Exception) {
            ByteArray(0)
        }
    }
}
