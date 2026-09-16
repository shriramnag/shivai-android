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

class OpenAiCompatibleProvider(
    private var baseUrl: String = "http://10.0.2.2:11434/v1",
    private var apiKey: String = "",
    private var modelName: String = "llama3.2:3b"
) : AiProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun updateConfig(url: String, key: String, model: String) {
        this.baseUrl = url.trimEnd('/')
        this.apiKey = key
        this.modelName = model
    }

    override suspend fun generateCompletion(
        messages: List<AiMessage>,
        screenContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = "You are Shiv AI, a personal operating agent on Android.\nScreen Context:\n$screenContext"

            val json = JSONObject().apply {
                put("model", modelName)
                put("temperature", 0.2)

                val msgArray = JSONArray()
                msgArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                for (m in messages) {
                    msgArray.put(JSONObject().apply {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
                put("messages", msgArray)
            }

            val requestBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(json.toString().toRequestBody("application/json".toMediaType()))

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()}"))
            }

            val body = response.body?.string() ?: "{}"
            val resObj = JSONObject(body)
            val content = resObj.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content") ?: ""

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
