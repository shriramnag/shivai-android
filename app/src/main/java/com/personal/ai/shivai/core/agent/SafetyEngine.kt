package com.personal.ai.shivai.core.agent

class SafetyEngine {

    private val criticalKeywords = listOf(
        "delete", "remove", "wipe", "format", "send money", "transfer", "pay",
        "purchase", "buy", "password", "factory reset", "पैसे भेजो", "डिलीट", "मिटाओ"
    )

    fun evaluateRisk(goal: String, tool: String, action: String): TrustLevel {
        val lowerGoal = goal.lowercase()
        return when {
            criticalKeywords.any { lowerGoal.contains(it) } -> TrustLevel.CRITICAL
            tool == "file_system" && action in listOf("delete", "move") -> TrustLevel.HIGH
            tool == "git_manager" && action in listOf("push", "reset") -> TrustLevel.HIGH
            tool == "accessibility_input" && action == "type" -> TrustLevel.MEDIUM
            tool in listOf("app_launcher", "global_navigation") -> TrustLevel.LOW
            else -> TrustLevel.SAFE
        }
    }

    fun isConfirmationRequired(level: TrustLevel): Boolean {
        return level == TrustLevel.HIGH || level == TrustLevel.CRITICAL
    }
}
