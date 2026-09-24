package com.personal.ai.shivai.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Groq AI Provider — FREE, fast (Llama 3.3 70B), Hindi support
 * Get free API key: https://console.groq.com
 * Free limits: 14,400 requests/day, 6,000 tokens/minute
 */
class GroqAiProvider(
    private var apiKey: String = ""
) : AiProvider {

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL    = "llama-3.3-70b-versatile"

        private const val SYSTEM_PROMPT = """आप Shiv AI हैं — एक advanced personal AI agent जो Android phone पर चलता है।
आप Hindi, Hinglish और English तीनों में बात कर सकते हैं।
आप एक real human friend की तरह naturally बात करते हैं — formal नहीं, natural।
आप phone की हर activity देख सकते हैं, apps खोल सकते हैं, tasks पूरे कर सकते हैं।
हमेशा short और clear जवाब दें जब तक detail न माँगी जाए।
कभी भी "As an AI" या "I cannot" जैसी बातें मत कहें — हमेशा helpful रहें।"""
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun setApiKey(key: String) { this.apiKey = key.trim() }
    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    override suspend fun generateCompletion(
        messages: List<AiMessage>,
        screenContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("NO_KEY"))
        }
        try {
            val systemWithContext = if (screenContext.isNotBlank())
                "$SYSTEM_PROMPT\n\nअभी phone की screen:\n$screenContext"
            else SYSTEM_PROMPT

            val jsonMessages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemWithContext))
                messages.takeLast(8).forEach { msg ->
                    put(JSONObject().put("role", msg.role).put("content", msg.content))
                }
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", jsonMessages)
                put("max_tokens", 512)
                put("temperature", 0.7)
                put("stream", false)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            if (!response.isSuccessful) {
                val err = json.optJSONObject("error")?.optString("message") ?: "Error ${response.code}"
                return@withContext Result.failure(Exception(err))
            }

            val text = json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
