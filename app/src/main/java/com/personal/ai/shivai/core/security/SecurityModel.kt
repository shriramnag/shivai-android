package com.personal.ai.shivai.core.security

enum class SecurityRiskLevel {
    SAFE, LOW, MEDIUM, HIGH, CRITICAL
}

data class SecurityThreat(
    val id: String,
    val title: String,
    val risk: SecurityRiskLevel,
    val evidence: String,
    val recommendedAction: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formatAlert(): String {
        return """
            🚨 SECURITY ALERT
            Threat: $title
            Risk: ${risk.name}
            Evidence: $evidence
            Recommended action: $recommendedAction
        """.trimIndent()
    }
}

data class ApkAnalysisResult(
    val targetPathOrPackage: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val riskLevel: SecurityRiskLevel,
    val suspiciousPermissions: List<String>,
    val allPermissions: List<String>,
    val signatures: List<String>,
    val warnings: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class LinkAnalysisResult(
    val url: String,
    val domain: String,
    val riskLevel: SecurityRiskLevel,
    val indicators: List<String>,
    val isIpAddress: Boolean,
    val isLookalikeDomain: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class FileGuardResult(
    val fileName: String,
    val fileSize: Long,
    val sha256: String,
    val mimeType: String,
    val riskLevel: SecurityRiskLevel,
    val warnings: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuarantinedFile(
    val id: String,
    val originalPath: String,
    val quarantinePath: String,
    val fileName: String,
    val sha256: String,
    val reason: String,
    val riskLevel: SecurityRiskLevel,
    val timestamp: Long = System.currentTimeMillis()
)
