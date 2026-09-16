package com.personal.ai.shivai.core.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class CyberSecurityAgent(private val context: Context) {

    val linkScanner = LinkSecurityScanner()
    val apkScanner = APKSecurityScanner(context)
    val downloadGuard = DownloadGuard()
    val quarantineManager = QuarantineManager(context)

    private val _activeThreats = MutableStateFlow<List<SecurityThreat>>(emptyList())
    val activeThreats: StateFlow<List<SecurityThreat>> = _activeThreats.asStateFlow()

    private val _overallRisk = MutableStateFlow(SecurityRiskLevel.SAFE)
    val overallRisk: StateFlow<SecurityRiskLevel> = _overallRisk.asStateFlow()

    fun evaluateUrl(url: String): LinkAnalysisResult {
        val result = linkScanner.scanUrl(url)
        if (result.riskLevel == SecurityRiskLevel.CRITICAL || result.riskLevel == SecurityRiskLevel.HIGH) {
            recordThreat(
                title = "Suspicious Link Detected",
                risk = result.riskLevel,
                evidence = result.indicators.joinToString("; "),
                recommendedAction = "Do not open this link or input credentials.",
                source = url
            )
        }
        return result
    }

    fun evaluateApk(apkPath: String): ApkAnalysisResult {
        val result = apkScanner.scanApkFile(apkPath)
        if (result.riskLevel == SecurityRiskLevel.CRITICAL || result.riskLevel == SecurityRiskLevel.HIGH) {
            recordThreat(
                title = "Suspicious APK Permissions/Signature",
                risk = result.riskLevel,
                evidence = result.warnings.joinToString("; ").ifBlank { "Suspicious permissions: ${result.suspiciousPermissions.joinToString()}" },
                recommendedAction = "Quarantine or delete this APK before installing.",
                source = apkPath
            )
        }
        return result
    }

    fun inspectAndGuardFile(file: File): FileGuardResult {
        val result = downloadGuard.inspectFile(file)
        if (result.riskLevel == SecurityRiskLevel.CRITICAL || result.riskLevel == SecurityRiskLevel.HIGH) {
            recordThreat(
                title = "High-Risk File Detected",
                risk = result.riskLevel,
                evidence = result.warnings.joinToString("; "),
                recommendedAction = "Quarantine file to prevent automatic execution.",
                source = file.absolutePath
            )
        }
        return result
    }

    suspend fun quarantineFile(file: File, reason: String): Result<QuarantinedFile> {
        val guard = downloadGuard.inspectFile(file)
        return quarantineManager.quarantine(
            sourceFile = file,
            reason = reason,
            risk = guard.riskLevel,
            sha256 = guard.sha256
        )
    }

    private fun recordThreat(
        title: String,
        risk: SecurityRiskLevel,
        evidence: String,
        recommendedAction: String,
        source: String
    ) {
        val threat = SecurityThreat(
            id = "THREAT-${UUID.randomUUID().toString().take(8)}",
            title = title,
            risk = risk,
            evidence = evidence,
            recommendedAction = recommendedAction,
            source = source
        )
        val current = _activeThreats.value.toMutableList()
        current.add(0, threat)
        _activeThreats.value = current

        if (risk.ordinal > _overallRisk.value.ordinal) {
            _overallRisk.value = risk
        }
    }

    fun clearThreat(id: String) {
        _activeThreats.value = _activeThreats.value.filterNot { it.id == id }
        recalculateOverallRisk()
    }

    fun clearAllThreats() {
        _activeThreats.value = emptyList()
        _overallRisk.value = SecurityRiskLevel.SAFE
    }

    private fun recalculateOverallRisk() {
        val highest = _activeThreats.value.maxByOrNull { it.risk.ordinal }?.risk ?: SecurityRiskLevel.SAFE
        _overallRisk.value = highest
    }
}
