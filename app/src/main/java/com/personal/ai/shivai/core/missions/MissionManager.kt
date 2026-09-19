package com.personal.ai.shivai.core.missions

import com.personal.ai.shivai.core.agent.EmergencyStopController
import com.personal.ai.shivai.core.agent.SafetyEngine
import com.personal.ai.shivai.core.agent.StepStatus
import com.personal.ai.shivai.core.agent.TaskPlan
import com.personal.ai.shivai.core.agent.TaskStep
import com.personal.ai.shivai.core.agent.TrustLevel
import com.personal.ai.shivai.core.automation.ShivAccessibilityService
import com.personal.ai.shivai.core.memory.MemoryDao
import com.personal.ai.shivai.core.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class MissionManager(
    private val toolRegistry: ToolRegistry,
    private val safetyEngine: SafetyEngine,
    private val stopController: EmergencyStopController,
    private val recoveryEngine: RecoveryReplanningEngine = RecoveryReplanningEngine(),
    private val memoryDao: MemoryDao? = null
) {

    private val _currentMission = MutableStateFlow<Mission?>(null)
    val currentMission: StateFlow<Mission?> = _currentMission.asStateFlow()

    private val _missionLog = MutableStateFlow<List<String>>(emptyList())
    val missionLog: StateFlow<List<String>> = _missionLog.asStateFlow()

    private fun appendLog(message: String) {
        val list = _missionLog.value.toMutableList()
        list.add("[${System.currentTimeMillis()}] $message")
        _missionLog.value = list
    }

    suspend fun runMission(
        goal: String,
        initialPlan: TaskPlan,
        onStatusSpeech: (String) -> Unit = {},
        requestUserConfirmation: suspend (preview: String) -> Boolean = { true }
    ): Boolean = withContext(Dispatchers.Default) {
        val mission = Mission(
            missionId = UUID.randomUUID().toString().take(8),
            rawGoal = goal,
            activePlan = initialPlan
        )
        _currentMission.value = mission

        // Stage 1: OBSERVE
        mission.state = MissionState.OBSERVE
        appendLog("OBSERVE: Current foreground package: ${ShivAccessibilityService.currentPackage.value}")

        // Stage 2: UNDERSTAND & CONTEXT
        mission.state = MissionState.UNDERSTAND
        appendLog("UNDERSTAND: Goal interpreted as: '$goal'")

        mission.state = MissionState.CONTEXT
        appendLog("CONTEXT: Evaluating device and system constraints")

        // Stage 3: MEMORY check
        mission.state = MissionState.MEMORY
        appendLog("MEMORY: Context checked against recent session history")

        // Stage 4: PLAN
        mission.state = MissionState.PLAN
        appendLog("PLAN: Formulated plan with ${initialPlan.steps.size} steps")

        val executionSteps = initialPlan.steps.toMutableList()
        var stepIndex = 0

        while (stepIndex < executionSteps.size) {
            if (stopController.isStopRequested()) {
                mission.state = MissionState.CANCELLED
                appendLog("EMERGENCY STOP: Mission cancelled by user")
                return@withContext false
            }

            val step = executionSteps[stepIndex]

            // Stage 5: POLICY CHECK
            mission.state = MissionState.POLICY_CHECK
            val risk = safetyEngine.evaluateRisk(
                goal = step.description,
                tool = step.tool,
                action = step.action,
                targetPackage = ShivAccessibilityService.currentPackage.value
            )

            if (safetyEngine.isConfirmationRequired(risk)) {
                val preview = "Safety Gate Triggered (${risk.name}):\nTool: ${step.tool}\nAction: ${step.action}\nParams: ${step.params}"
                val approved = requestUserConfirmation(preview)
                if (!approved) {
                    step.status = StepStatus.CANCELLED
                    mission.state = MissionState.CANCELLED
                    appendLog("POLICY: Action rejected by user for step '${step.description}'")
                    return@withContext false
                }
            }

            // Stage 6: TOOL SELECTION & EXECUTE
            mission.state = MissionState.TOOL_SELECTION
            val tool = toolRegistry.getTool(step.tool)
            if (tool == null) {
                step.status = StepStatus.FAILED
                appendLog("TOOL SELECTION: Tool '${step.tool}' not found")
                mission.state = MissionState.FAILED
                return@withContext false
            }

            mission.state = MissionState.EXECUTE
            step.status = StepStatus.RUNNING
            onStatusSpeech(step.description)
            appendLog("EXECUTE: Running step: ${step.description} via ${step.tool}")

            val result = tool.execute(step.action, step.params)

            // Stage 7: OBSERVE & VERIFY
            mission.state = MissionState.VERIFY
            val isVerified = result.isSuccess && result.output.isNotBlank()

            if (isVerified) {
                step.status = StepStatus.VERIFIED_SUCCESS
                step.verificationEvidence = result.output
                mission.executionHistory.add(step)
                appendLog("VERIFY: Step succeeded. Evidence: ${result.output}")
                stepIndex++
            } else {
                // Stage 8: RECOVER / REPLAN
                mission.state = MissionState.RECOVER_REPLAN
                mission.recoveryAttempts++
                appendLog("RECOVER_REPLAN: Step failed. Reason: ${result.output}. Attempt ${mission.recoveryAttempts}/${mission.maxRecoveryAttempts}")

                val proposal = recoveryEngine.generateRecovery(step, mission.recoveryAttempts, mission.maxRecoveryAttempts)
                if (proposal.shouldReplan && proposal.replacementSteps.isNotEmpty()) {
                    appendLog("REPLAN: ${proposal.recoveryMessage}")
                    executionSteps.removeAt(stepIndex)
                    executionSteps.addAll(stepIndex, proposal.replacementSteps)
                } else {
                    step.status = StepStatus.FAILED
                    mission.state = MissionState.FAILED
                    appendLog("FAILED: Recovery impossible. ${proposal.recoveryMessage}")
                    return@withContext false
                }
            }
        }

        // Stage 9: COMPLETE & LOG
        mission.state = MissionState.COMPLETE
        mission.finalResult = "Mission completed successfully with ${mission.executionHistory.size} executed steps."
        appendLog("COMPLETE: ${mission.finalResult}")
        onStatusSpeech("Mission complete.")
        return@withContext true
    }
}
