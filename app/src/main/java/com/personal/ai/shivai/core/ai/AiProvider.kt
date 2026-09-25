package com.personal.ai.shivai.core.ai

/**
 * Abstract AI Provider Interface
 * Allows multiple AI backends without changing core agent logic
 */
interface AiProvider {
    
    fun setApiKey(key: String)
    
    fun hasApiKey(): Boolean
    
    fun getModelName(): String
    
    suspend fun generateCompletion(
        messages: List<AiMessage>,
        context: String = ""
    ): Result<String>
}

data class AiMessage(
    val role: String,  // "user" or "assistant" or "system"
    val content: String
)
