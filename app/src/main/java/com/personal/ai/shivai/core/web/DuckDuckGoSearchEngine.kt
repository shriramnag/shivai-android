package com.personal.ai.shivai.core.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String,
    val isVerifiedSafe: Boolean = true
)

data class SearchResponse(
    val query: String,
    val items: List<SearchResultItem>,
    val instantAnswer: String = "",
    val source: String = "DuckDuckGo",
    val error: String? = null
)

class DuckDuckGoSearchEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {

    suspend fun search(query: String): SearchResponse = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext SearchResponse(query, emptyList(), error = "Empty search query")
        }

        try {
            val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name())
            val apiUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"

            val request = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "ShivAI-Android-Agent/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext SearchResponse(
                    query = query,
                    items = emptyList(),
                    error = "HTTP Error ${response.code}"
                )
            }

            val bodyString = response.body?.string() ?: ""
            val json = JSONObject(bodyString)

            val results = mutableListOf<SearchResultItem>()

            val heading = json.optString("Heading", "")
            val abstractText = json.optString("AbstractText", "")
            val abstractUrl = json.optString("AbstractURL", "")

            if (abstractText.isNotBlank()) {
                results.add(
                    SearchResultItem(
                        title = if (heading.isNotBlank()) heading else query,
                        snippet = abstractText,
                        url = if (abstractUrl.isNotBlank()) abstractUrl else "https://duckduckgo.com/?q=$encoded"
                    )
                )
            }

            val relatedTopics = json.optJSONArray("RelatedTopics")
            if (relatedTopics != null) {
                for (i in 0 until relatedTopics.length()) {
                    val topic = relatedTopics.optJSONObject(i) ?: continue
                    val text = topic.optString("Text", "")
                    val firstUrl = topic.optString("FirstURL", "")
                    if (text.isNotBlank() && firstUrl.isNotBlank()) {
                        results.add(
                            SearchResultItem(
                                title = text.take(60) + if (text.length > 60) "..." else "",
                                snippet = text,
                                url = firstUrl
                            )
                        )
                    }
                    if (results.size >= 5) break
                }
            }

            SearchResponse(
                query = query,
                items = results,
                instantAnswer = abstractText
            )
        } catch (e: Exception) {
            SearchResponse(
                query = query,
                items = emptyList(),
                error = e.message ?: "Failed to execute DuckDuckGo search"
            )
        }
    }
}
