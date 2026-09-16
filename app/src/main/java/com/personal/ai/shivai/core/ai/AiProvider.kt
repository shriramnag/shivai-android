package com.personal.ai.shivai.core.ai

data class AiMessage(
    val role: String,
    val content: String
)

interface AiProvider {
    suspend fun generateCompletion(
        messages: List<AiMessage>,
        screenContext: String
    ): Result<String>
}
