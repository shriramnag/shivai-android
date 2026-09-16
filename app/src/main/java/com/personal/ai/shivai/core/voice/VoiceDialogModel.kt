package com.personal.ai.shivai.core.voice

enum class DialogSessionState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    AWAITING_CONFIRMATION,
    AWAITING_SLOT_VALUE
}

data class DialogTurn(
    val turnId: String,
    val userUtterance: String,
    val assistantResponse: String,
    val recognizedIntent: String,
    val extractedSlots: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingSlotPrompt
