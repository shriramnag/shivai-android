package com.personal.ai.shivai.core.agent

enum class TrustLevel {
    SAFE, LOW, MEDIUM, HIGH, CRITICAL
}

enum class StepStatus {
    PENDING, RUNNING, VERIFIED_SUCCESS, RETRYING, FAILED, CANCELLED
}

data class TaskStep(
    val stepId: String,
    val description: String,
    val tool: String,
    val action: String,
    val params: Map<String, String>,
    val trustLevel: TrustLevel = TrustLevel.SAFE,
    var status: StepStatus = StepStatus.PENDING,
    var verificationEvidence: String = "",
    var errorMessage: String = ""
)

data class ExecutionLogEntry(
    val taskId: String,
    val stepId: String,
    val timestamp: String,
    val tool: String,
    val action: String,
    val inputSummary: String,
    val result: String,
    val status: StepStatus,
    val verification: String,
    val error: String = "",
    val nextAction: String = ""
)

data class TaskPlan(
    val taskId: String,
    val originalGoal: String,
    val steps: MutableList<TaskStep>
)
