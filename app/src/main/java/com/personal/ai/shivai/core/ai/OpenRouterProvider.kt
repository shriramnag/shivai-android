package com.personal.ai.shivai.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * OpenRouter AI Provider
 * Supports multiple models via OpenRouter API
 * Lifetime free tier with usage tracking
 */
class OpenRouterProvider : AiProvider {

    private var apiKey: String = ""
    private var selectedModel: String = DEFAULT_MODEL
    private var usageTokens: Long = 0L

    companion object {
        const val DEFAULT_MODEL = "openrouter/auto"
        const val API_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        
        // Popular free/cheap models
        val AVAILABLE_MODELS = listOf(
            "openrouter/auto",           // Auto-select best available
            "meta-llama/llama-2-7b",     // Meta Llama 2
            "mistralai/mistral-7b",      // Mistral 7B
            "teknium/openhermes-2.5",    // Open Hermes
            "gryphe/mythomist-7b",       // MythoMist 7B
            "undi95/remm-slerp-l2-13b"   // RemM Slerp
        )
    }

    override fun setApiKey(key: String) {
        this.apiKey = key.trim()
    }

    override fun hasApiKey(): Boolean = apiKey.isNotBlank()

    override fun getModelName(): String = selectedModel

    fun setModel(model: String) {
        if (model.isNotBlank()) {
            selectedModel = model
        }
    }

    fun getAvailableModels(): List<String> = AVAILABLE_MODELS

    fun getUsageTokens(): Long = usageTokens

    override suspend fun generateCompletion(
        messages: List<AiMessage>,
        context: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!hasApiKey()) {
                return@withContext Result.failure(
                    Exception("NO_KEY: OpenRouter API key not set")
                )
            }

            val response = callOpenRouterApi(messages, context)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callOpenRouterApi(
        messages: List<AiMessage>,
        context: String
    ): String {
        val url = URL(API_ENDPOINT)
        val connection = url.openConnection() as HttpsURLConnection

        try {
            // Prepare request
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("HTTP-Referer", "com.personal.ai.shivai")
            connection.doOutput = true

            // Build request body
            val requestBody = JSONObject().apply {
                put("model", selectedModel)
                put("temperature", 0.7)
                put("max_tokens", 1024)
                
                val messageArray = JSONArray()
                
                // Add context if available
                if (context.isNotBlank()) {
                    messageArray.put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Context: $context")
                    })
                }
                
                // Add conversation messages
                messages.forEach { msg ->
                    messageArray.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
                
                put("messages", messageArray)
            }

            // Send request
            connection.outputStream.write(requestBody.toString().toByteArray())
            connection.outputStream.flush()

            // Read response
            val responseCode = connection.responseCode
            val responseStream = if (responseCode == 200) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = responseStream.bufferedReader().readText()
            val jsonResponse = JSONObject(response)

            // Track usage
            if (jsonResponse.has("usage")) {
                val usage = jsonResponse.getJSONObject("usage")
                usageTokens += usage.optLong("total_tokens", 0L)
            }

            // Extract reply
            if (jsonResponse.has("choices")) {
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return message.getString("content")
                }
            }

            return "OpenRouter response error: ${jsonResponse.optString("error", "Unknown")}"

        } finally {
            connection.disconnect()
        }
    }
}
