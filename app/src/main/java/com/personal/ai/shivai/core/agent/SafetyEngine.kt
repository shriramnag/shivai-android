<<<< REPLACE
    fun evaluateRisk(
        goal: String,
        tool: String,
        action: String,
        targetPackage: String = ""
    ): TrustLevel {
==== WITH
    fun evaluateRisk(
        goal: String,
        tool: String,
        action: String,
        targetPackage: String = "",
        commandParam: String = ""
    ): TrustLevel {
        val lowerGoal = goal.lowercase(Locale.ROOT)

        // Sensitive App & Payment Protection
        if (targetPackage.isNotBlank() && SensitiveAppRegistry.isSensitivePackage(targetPackage)) {
            return TrustLevel.CRITICAL
        }

        val paymentAssessment = PaymentShield.evaluateAction(goal, targetPackage)
        if (paymentAssessment.isBlocked || paymentAssessment.riskLevel == com.personal.ai.shivai.core.security.SecurityRiskLevel.CRITICAL) {
            return TrustLevel.CRITICAL
        }

        // Termux safety evaluation
        if (tool == "termux" && commandParam.isNotBlank()) {
            val assessment = TermuxCommandPolicy.evaluate(commandParam)
            return when (assessment.riskLevel) {
                TermuxRiskLevel.DESTRUCTIVE -> TrustLevel.CRITICAL
                TermuxRiskLevel.HIGH_RISK -> TrustLevel.HIGH
                TermuxRiskLevel.LOW_RISK -> TrustLevel.LOW
                TermuxRiskLevel.SAFE -> TrustLevel.SAFE
            }
        }

        return when {
            criticalKeywords.any { lowerGoal.contains(it) } -> TrustLevel.CRITICAL
            tool == "quarantine_file" -> TrustLevel.MEDIUM
            tool in listOf("file_system", "file_agent") && action in listOf("delete", "move") -> TrustLevel.HIGH
            tool == "git_manager" && action in listOf("push", "reset") -> TrustLevel.HIGH
            tool == "accessibility_input" && action == "type" -> TrustLevel.MEDIUM
            tool in listOf("app_launcher", "global_navigation", "web_research", "system_health") -> TrustLevel.LOW
            else -> TrustLevel.SAFE
        }
    }
>>>>
