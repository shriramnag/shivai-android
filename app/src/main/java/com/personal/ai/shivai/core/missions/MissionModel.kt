package com.personal.ai.shivai.core.missions

import com.personal.ai.shivai.core.agent.TaskPlan
import com.personal.ai.shivai.core.agent.TaskStep

enum class MissionState {
    IDLE,
    OBSERVE,
    UNDERSTAND,
    CONTEXT,
    MEMORY,
    PLAN,
    POLICY_CHECK,
    TOOL_SELECTION,
    EXECUTE,
    VERIFY,
    RECOVER_REPLAN,
    COMPLETE,
    FAILED,
    CANCELLED
}

data class Mission(
    val missionId: String,
    val rawGoal: String,
    var state: MissionState = MissionState.IDLE,
    var activePlan: TaskPlan? = null,
    val executionHistory: MutableList<TaskStep> = mutableListOf(),
    var currentStepIndex: Int = 0,
    var recoveryAttempts: Int = 0,
    val maxRecoveryAttempts: Int = 3,
    var finalResult: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class RecoveryProposal(
    val shouldReplan: Boolean,
    val recoveryMessage: String,
    val replacementSteps: List<TaskStep> = emptyList()
)
