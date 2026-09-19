package com.personal.ai.shivai.core.termux

import java.util.Locale

enum class TermuxRiskLevel {
    SAFE,
    LOW_RISK,
    HIGH_RISK,
    DESTRUCTIVE
}

data class TermuxCommandAssessment(
    val command: String,
    val riskLevel: TermuxRiskLevel,
    val isAllowed: Boolean,
    val requiresConfirmation: Boolean,
    val reason: String
)

object TermuxCommandPolicy {

    private val destructivePatterns = listOf(
        "rm -rf /", "rm -rf *", "rm -r /", "mkfs", "dd if=", ":(){ :|:& };:",
        "> /dev/sda", "wipe", "format", "chmod -r 777 /"
    )

    private val highRiskTokens = listOf(
        "rm ", "rmdir", "git push", "git reset --hard", "chmod", "chown",
        "pkill", "kill -9", "curl | sh", "wget | sh", "pip install", "apt install",
        "pkg install"
    )

    private val lowRiskTokens = listOf(
        "mkdir", "touch", "cp", "mv", "git commit", "git add", "git checkout",
        "git branch", "tar", "unzip", "python", "gradle"
    )

    private val safePrefixes = listOf(
        "ls", "pwd", "date", "whoami", "uname", "echo", "cat", "git status",
        "git log", "git diff", "git show", "which", "head", "tail", "grep",
        "wc", "df", "free", "uptime"
    )

    fun evaluate(rawCommand: String): TermuxCommandAssessment {
        val trimmed = rawCommand.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        if (trimmed.isBlank()) {
            return TermuxCommandAssessment(
                command = rawCommand,
                riskLevel = TermuxRiskLevel.SAFE,
                isAllowed = false,
                requiresConfirmation = false,
                reason = "Empty command."
            )
        }

        // 1. Check Destructive
        for (pattern in destructivePatterns) {
            if (lower.contains(pattern)) {
                return TermuxCommandAssessment(
                    command = rawCommand,
                    riskLevel = TermuxRiskLevel.DESTRUCTIVE,
                    isAllowed = false,
                    requiresConfirmation = true,
                    reason = "Command contains destructive pattern '$pattern'. Execution blocked by security policy."
                )
            }
        }

        // 2. Check High Risk
        for (token in highRiskTokens) {
            if (lower.contains(token)) {
                return TermuxCommandAssessment(
                    command = rawCommand,
                    riskLevel = TermuxRiskLevel.HIGH_RISK,
                    isAllowed = true,
                    requiresConfirmation = true,
                    reason = "Command alters system state or performs network installation. Explicit confirmation required."
                )
            }
        }

        // 3. Check Low Risk
        for (token in lowRiskTokens) {
            if (lower.contains(token)) {
                return TermuxCommandAssessment(
                    command = rawCommand,
                    riskLevel = TermuxRiskLevel.LOW_RISK,
                    isAllowed = true,
                    requiresConfirmation = false,
                    reason = "Low risk file/git operation."
                )
            }
        }

        // 4. Check Safe Whitelist
        val firstWord = lower.split(Regex("\\s+")).firstOrNull() ?: ""
        val matchesSafe = safePrefixes.any { lower.startsWith(it) || firstWord == it }

        if (matchesSafe) {
            return TermuxCommandAssessment(
                command = rawCommand,
                riskLevel = TermuxRiskLevel.SAFE,
                isAllowed = true,
                requiresConfirmation = false,
                reason = "Read-only inspection / diagnostic command."
            )
        }

        return TermuxCommandAssessment(
            command = rawCommand,
            riskLevel = TermuxRiskLevel.HIGH_RISK,
            isAllowed = true,
            requiresConfirmation = true,
            reason = "Unclassified command requires explicit user confirmation before dispatch."
        )
    }
}
