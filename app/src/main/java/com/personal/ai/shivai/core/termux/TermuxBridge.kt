package com.personal.ai.shivai.core.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TermuxExecutionResult(
    val isSuccess: Boolean,
    val assessment: TermuxCommandAssessment,
    val message: String,
    val commandDispatched: Boolean = false
)

class TermuxBridge(private val context: Context) {

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_IN_BACKGROUND"
        const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    }

    fun isTermuxInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun executeCommand(
        command: String,
        workDir: String = "/data/data/com.termux/files/home",
        inBackground: Boolean = true,
        userConfirmed: Boolean = false
    ): TermuxExecutionResult = withContext(Dispatchers.IO) {
        val assessment = TermuxCommandPolicy.evaluate(command)

        if (!assessment.isAllowed) {
            return@withContext TermuxExecutionResult(
                isSuccess = false,
                assessment = assessment,
                message = "Command rejected: ${assessment.reason}"
            )
        }

        if (assessment.requiresConfirmation && !userConfirmed) {
            return@withContext TermuxExecutionResult(
                isSuccess = false,
                assessment = assessment,
                message = "User confirmation required for ${assessment.riskLevel} action: '$command'"
            )
        }

        if (!isTermuxInstalled()) {
            return@withContext TermuxExecutionResult(
                isSuccess = false,
                assessment = assessment,
                message = "Termux is not installed on this device. Install Termux to enable CLI integration."
            )
        }

        try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setPackage(TERMUX_PACKAGE)
                putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
                putExtra(EXTRA_WORKDIR, workDir)
                putExtra(EXTRA_BACKGROUND, inBackground)
                putExtra(EXTRA_SESSION_ACTION, "0")
            }

            context.sendBroadcast(intent)
            TermuxExecutionResult(
                isSuccess = true,
                assessment = assessment,
                message = "Command safely dispatched to Termux environment.",
                commandDispatched = true
            )
        } catch (e: Exception) {
            TermuxExecutionResult(
                isSuccess = false,
                assessment = assessment,
                message = "Failed to dispatch command to Termux: ${e.message}"
            )
        }
    }
}
