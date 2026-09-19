package com.personal.ai.shivai.core.missions

import com.personal.ai.shivai.core.agent.StepStatus
import com.personal.ai.shivai.core.agent.TaskStep
import com.personal.ai.shivai.core.agent.TrustLevel

class RecoveryReplanningEngine {

    fun generateRecovery(
        failedStep: TaskStep,
        attemptNumber: Int,
        maxAttempts: Int = 3
    ): RecoveryProposal {
        if (attemptNumber >= maxAttempts) {
            return RecoveryProposal(
                shouldReplan = false,
                recoveryMessage = "Max recovery attempts ($maxAttempts) exhausted for step '${failedStep.description}'."
            )
        }

        return when (failedStep.tool) {
            "accessibility_click" -> {
                val delayStep = TaskStep(
                    stepId = "${failedStep.stepId}-rec-delay",
                    description = "Wait for UI tree to stabilize before re-clicking",
                    tool = "delay",
                    action = "wait",
                    params = mapOf("millis" to "1500"),
                    trustLevel = TrustLevel.SAFE,
                    status = StepStatus.PENDING
                )
                val retryClick = failedStep.copy(
                    stepId = "${failedStep.stepId}-rec-retry",
                    status = StepStatus.PENDING
                )
                RecoveryProposal(
                    shouldReplan = true,
                    recoveryMessage = "Attempting UI stabilization delay and node re-click.",
                    replacementSteps = listOf(delayStep, retryClick)
                )
            }
            "app_launcher" -> {
                val homeStep = TaskStep(
                    stepId = "${failedStep.stepId}-rec-home",
                    description = "Navigate Home before retrying app launch",
                    tool = "global_navigation",
                    action = "home",
                    params = emptyMap(),
                    trustLevel = TrustLevel.LOW,
                    status = StepStatus.PENDING
                )
                val retryLaunch = failedStep.copy(
                    stepId = "${failedStep.stepId}-rec-launch",
                    status = StepStatus.PENDING
                )
                RecoveryProposal(
                    shouldReplan = true,
                    recoveryMessage = "Returning to Home screen before retrying application launch.",
                    replacementSteps = listOf(homeStep, retryLaunch)
                )
            }
            "web_search" -> {
                RecoveryProposal(
                    shouldReplan = false,
                    recoveryMessage = "Web search failed. Reverting to local heuristic brain response."
                )
            }
            else -> {
                RecoveryProposal(
                    shouldReplan = false,
                    recoveryMessage = "No automated recovery rule for tool '${failedStep.tool}'. Aborting step safely."
                )
            }
        }
    }
}
