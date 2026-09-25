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
 * Groq AI Provider — FREE, fast, Hindi support.
 *
 * NOTE: Groq model names vary by account and deployment. We keep the model configurable and add a
 * safe fallback chain so the app continues to work when a specific model is unavailable.
 */
class GroqAiProvider(
    private var apiKey: String = "",
    private var modelName: String = "llama-3.3-70b-versatile"
) : AiProvider {

    companion object {
        private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val DEFAULT_MODEL = "llama-3.3-70b-versatile"
        private val FALLBACK_MODELS = listOf(
            DEFAULT_MODEL,
            "llama-3.1-8b-instant",
            "llama-3.3-70b-specdec",
            "meta-llama/llama-4-scout-17b-16e-instruct"
        )

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
    fun setModel(model: String) {
        val normalized = model.trim()
        if (normalized.isNotBlank()) this.modelName = normalized
    }
    fun hasApiKey(): Boolean = apiKey.isNotBlank()
    fun getModelName(): String = modelName.ifBlank { DEFAULT_MODEL }

    private fun candidateModels(): List<String> {
        val selected = getModelName()
        val list = mutableListOf<String>()
        if (selected.isNotBlank()) list.add(selected)
        FALLBACK_MODELS.filter { it != selected && it.isNotBlank() }.forEach { list.add(it) }
        return list.distinct().ifEmpty { listOf(DEFAULT_MODEL) }
    }

    private fun buildFriendlyGroqMessage(raw: String, httpCode: Int): String {
        val clean = raw.ifBlank { "Groq AI request failed." }
        return when {
            clean.contains("does not exist", ignoreCase = true) ||
                clean.contains("do not have access", ignoreCase = true) ||
                clean.contains("not found", ignoreCase = true) ->
                "चुना गया Groq model इस account के लिए उपलब्ध नहीं है। Settings में अलग supported model चुनें।"

            clean.contains("invalid api", ignoreCase = true) ||
                clean.contains("unauthorized", ignoreCase = true) ||
                httpCode == 401 ->
                "Groq API key गलत है या expired हो गई है। नए key को फिर से डालें।"

            clean.contains("rate limit", ignoreCase = true) ||
                clean.contains("too many requests", ignoreCase = true) ||
                httpCode == 429 ->
                "Groq request limit पूरी हो गई है। थोड़ी देर बाद फिर से कोशिश करें।"

            clean.contains("network", ignoreCase = true) ||
                clean.contains("timeout", ignoreCase = true) ||
                clean.contains("connection", ignoreCase = true) ->
                "Groq से कनेक्शन नहीं बन पाया। Internet चेक करें और फिर कोशिश करें।"

            else -> clean
        }
    }

    override suspend fun generateCompletion(
        messages: List<AiMessage>,
        screenContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("NO_KEY"))
        }

        val candidateModels = candidateModels()
        var lastFailure: Exception? = null

        for (candidate in candidateModels) {
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
                    put("model", candidate)
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
                val rawResponse = response.body?.string() ?: "{}"
                val json = JSONObject(rawResponse)

                if (!response.isSuccessful) {
                    val errText = json.optJSONObject("error")?.optString("message") ?: "Error ${response.code}"
                    val friendly = buildFriendlyGroqMessage(errText, response.code)
                    lastFailure = Exception(friendly)

                    if (candidate == candidateModels.last()) {
                        return@withContext Result.failure(lastFailure!!)
                    }

                    val shouldRetry = errText.contains("does not exist", ignoreCase = true) ||
                        errText.contains("do not have access", ignoreCase = true) ||
                        errText.contains("not found", ignoreCase = true) ||
                        errText.contains("model", ignoreCase = true)

                    if (!shouldRetry) {
                        return@withContext Result.failure(lastFailure!!)
                    }
                    continue
                }

                val text = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()

                return@withContext Result.success(text)
            } catch (e: Exception) {
                lastFailure = e
                if (candidate == candidateModels.last()) {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(lastFailure ?: Exception("Groq AI request failed."))
    }
}
