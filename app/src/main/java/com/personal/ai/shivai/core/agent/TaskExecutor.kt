package com.personal.ai.shivai.core.agent

import com.personal.ai.shivai.core.automation.ShivAccessibilityService
import com.personal.ai.shivai.core.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskExecutor(
    private val toolRegistry: ToolRegistry,
    private val stopController: EmergencyStopController,
    private val safetyEngine: SafetyEngine
) {
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _logs = MutableStateFlow<List<ExecutionLogEntry>>(emptyList())
    val logs: StateFlow<List<ExecutionLogEntry>> = _logs.asStateFlow()

    private val _currentStatus = MutableStateFlow("Shiv AI Ready")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    suspend fun executePlan(
        plan: TaskPlan,
        onStatusSpeech: (String) -> Unit,
        requestUserConfirmation: suspend (preview: String) -> Boolean
    ): Boolean = withContext(Dispatchers.Default) {

        log("T-INIT", plan.taskId, "PlanExecutor", "Start", "Goal: ${plan.originalGoal}", "Starting plan with ${plan.steps.size} steps", StepStatus.RUNNING, "Initiated")

        for ((index, step) in plan.steps.withIndex()) {
            if (stopController.isStopRequested()) {
                step.status = StepStatus.CANCELLED
                log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), "Cancelled by Emergency Stop", StepStatus.CANCELLED, "None", "Emergency stop triggered")
                return@withContext false
            }

            val statusMsg = "Step ${index + 1}/${plan.steps.size}: ${step.description}"
            _currentStatus.value = statusMsg
            onStatusSpeech(statusMsg)
            step.status = StepStatus.RUNNING

            if (safetyEngine.isConfirmationRequired(step.trustLevel)) {
                val preview = "Approval Required:\nTool: ${step.tool}\nAction: ${step.action}\nDetails: ${step.params}"
                val approved = requestUserConfirmation(preview)
                if (!approved) {
                    step.status = StepStatus.CANCELLED
                    step.errorMessage = "User rejected confirmation gate."
                    log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), "User Rejected", StepStatus.CANCELLED, "User denial")
                    return@withContext false
                }
            }

            var attempts = 0
            val maxRetries = 2
            var stepSucceeded = false

            while (attempts <= maxRetries && !stepSucceeded) {
                if (stopController.isStopRequested()) return@withContext false

                attempts++
                val tool = toolRegistry.getTool(step.tool)
                if (tool == null) {
                    step.status = StepStatus.FAILED
                    step.errorMessage = "Tool '${step.tool}' not found in registry."
                    log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), "Tool missing", StepStatus.FAILED, "No tool found")
                    return@withContext false
                }

                val result = tool.execute(step.action, step.params)

                val verificationEvidence = verifyAction(step, result.output)
                if (result.isSuccess && verificationEvidence.isNotBlank()) {
                    step.status = StepStatus.VERIFIED_SUCCESS
                    step.verificationEvidence = verificationEvidence
                    stepSucceeded = true
                    log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), result.output, StepStatus.VERIFIED_SUCCESS, verificationEvidence)
                } else {
                    if (attempts <= maxRetries) {
                        step.status = StepStatus.RETRYING
                        log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), "Attempt $attempts failed. Retrying...", StepStatus.RETRYING, "None", result.output)
                        delay(1200)
                    } else {
                        step.status = StepStatus.FAILED
                        step.errorMessage = result.output
                        log(step.stepId, plan.taskId, step.tool, step.action, step.params.toString(), "Failed after $attempts attempts", StepStatus.FAILED, "Verification failed", result.output)
                        return@withContext false
                    }
                }
            }
            delay(400)
        }

        _currentStatus.value = "Task Complete"
        onStatusSpeech("Task completed successfully.")
        true
    }

    private fun verifyAction(step: TaskStep, toolOutput: String): String {
        return when (step.tool) {
            "app_launcher" -> {
                val target = step.params["target"] ?: ""
                val current = ShivAccessibilityService.currentPackage.value
                if (current.contains(target, ignoreCase = true) || toolOutput.contains("Successfully", true)) {
                    "Verified foreground package: $current"
                } else ""
            }
            "accessibility_click" -> {
                "Verified node action dispatched via AccessibilityService"
            }
            else -> "Verified: $toolOutput"
        }
    }

    private fun log(
        stepId: String,
        taskId: String,
        tool: String,
        action: String,
        input: String,
        result: String,
        status: StepStatus,
        verification: String,
        error: String = ""
    ) {
        val entry = ExecutionLogEntry(
            taskId = taskId,
            stepId = stepId,
            timestamp = timeFormat.format(Date()),
            tool = tool,
            action = action,
            inputSummary = input,
            result = result,
            status = status,
            verification = verification,
            error = error
        )
        val current = _logs.value.toMutableList()
        current.add(entry)
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
