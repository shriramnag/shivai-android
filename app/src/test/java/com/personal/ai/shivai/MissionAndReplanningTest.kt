package com.personal.ai.shivai

import com.personal.ai.shivai.core.agent.StepStatus
import com.personal.ai.shivai.core.agent.TaskStep
import com.personal.ai.shivai.core.agent.TrustLevel
import com.personal.ai.shivai.core.missions.RecoveryReplanningEngine
import org.junit.Assert.*
import org.junit.Test

class MissionAndReplanningTest {

    private val recoveryEngine = RecoveryReplanningEngine()

    @Test
    fun testAccessibilityClickRecoveryReplanning() {
        val failedStep = TaskStep(
            stepId = "step-click-1",
            description = "Click on Send button",
            tool = "accessibility_click",
            action = "click",
            params = mapOf("text" to "Send"),
            trustLevel = TrustLevel.SAFE,
            status = StepStatus.FAILED,
            errorMessage = "Node not found"
        )

        val proposal = recoveryEngine.generateRecovery(failedStep, attemptNumber = 1, maxAttempts = 3)
        assertTrue(proposal.shouldReplan)
        assertEquals(2, proposal.replacementSteps.size)
        assertEquals("delay", proposal.replacementSteps[0].tool)
        assertEquals("accessibility_click", proposal.replacementSteps[1].tool)
    }

    @Test
    fun testAppLaunchRecoveryReplanning() {
        val failedStep = TaskStep(
            stepId = "step-launch-1",
            description = "Launch WhatsApp",
            tool = "app_launcher",
            action = "launch",
            params = mapOf("target" to "WhatsApp"),
            trustLevel = TrustLevel.LOW,
            status = StepStatus.FAILED,
            errorMessage = "App launch timed out"
        )

        val proposal = recoveryEngine.generateRecovery(failedStep, attemptNumber = 1, maxAttempts = 3)
        assertTrue(proposal.shouldReplan)
        assertEquals(2, proposal.replacementSteps.size)
        assertEquals("global_navigation", proposal.replacementSteps[0].tool)
        assertEquals("home", proposal.replacementSteps[0].action)
        assertEquals("app_launcher", proposal.replacementSteps[1].tool)
    }

    @Test
    fun testMaxRecoveryAttemptsExhaustion() {
        val failedStep = TaskStep(
            stepId = "step-click-3",
            description = "Click on Confirm",
            tool = "accessibility_click",
            action = "click",
            params = mapOf("text" to "Confirm"),
            trustLevel = TrustLevel.SAFE,
            status = StepStatus.FAILED
        )

        val proposal = recoveryEngine.generateRecovery(failedStep, attemptNumber = 3, maxAttempts = 3)
        assertFalse(proposal.shouldReplan)
        assertTrue(proposal.recoveryMessage.contains("exhausted", ignoreCase = true))
        assertTrue(proposal.replacementSteps.isEmpty())
    }
}
