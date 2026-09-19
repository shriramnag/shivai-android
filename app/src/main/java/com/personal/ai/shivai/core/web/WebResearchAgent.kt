package com.personal.ai.shivai.core.web

import com.personal.ai.shivai.core.security.CyberSecurityAgent
import com.personal.ai.shivai.core.security.SecurityRiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResearchSummary(
    val query: String,
    val summary: String,
    val sources: List<SearchResultItem>,
    val warnings: List<String> = emptyList()
)

class WebResearchAgent(
    private val searchEngine: DuckDuckGoSearchEngine = DuckDuckGoSearchEngine(),
    private val securityAgent: CyberSecurityAgent? = null
) {

    suspend fun research(query: String): ResearchSummary = withContext(Dispatchers.Default) {
        val searchResponse = searchEngine.search(query)

        if (searchResponse.items.isEmpty()) {
            val note = if (searchResponse.error != null) {
                "Search error: ${searchResponse.error}"
            } else {
                "No relevant web search results found for query: '$query'."
            }
            return@withContext ResearchSummary(
                query = query,
                summary = note,
                sources = emptyList()
            )
        }

        val safeSources = mutableListOf<SearchResultItem>()
        val warnings = mutableListOf<String>()

        for (item in searchResponse.items) {
            if (securityAgent != null) {
                val scan = securityAgent.evaluateUrl(item.url)
                if (scan.riskLevel == SecurityRiskLevel.CRITICAL || scan.riskLevel == SecurityRiskLevel.HIGH) {
                    warnings.add("Filtered unsafe result: ${item.url} (Risk: ${scan.riskLevel})")
                    continue
                }
            }
            safeSources.add(item)
        }

        val builder = StringBuilder()
        if (searchResponse.instantAnswer.isNotBlank()) {
            builder.append(searchResponse.instantAnswer).append("\n\n")
        }

        builder.append("Sources consulted:\n")
        safeSources.forEachIndexed { idx, src ->
            builder.append("${idx + 1}. [${src.title}](${src.url}): ${src.snippet}\n")
        }

        ResearchSummary(
            query = query,
            summary = builder.toString().trim(),
            sources = safeSources,
            warnings = warnings
        )
    }

    fun isWebSearchNeeded(userPrompt: String): Boolean {
        val lower = userPrompt.lowercase()
        val triggers = listOf(
            "search", "web", "latest", "news", "current", "weather", "today",
            "kya chal raha hai", "taza khabar", "aaj ka", "khojo", "find online"
        )
        return triggers.any { lower.contains(it) }
    }
}
