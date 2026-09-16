package com.personal.ai.shivai.core.security

import android.net.Uri
import java.util.Locale

class LinkSecurityScanner {

    private val suspiciousTlds = setOf(
        "top", "xyz", "club", "buzz", "work", "loan", "gq", "ml", "cf", "tk", "ga", "fit", "surf"
    )

    private val lookalikeTargets = mapOf(
        "google" to listOf("g00gle", "googel", "g0ogle", "goog1e"),
        "paypal" to listOf("paypa1", "paypai", "pay-pal", "paypal-security", "paypal-update"),
        "whatsapp" to listOf("whatsap", "whatapp", "whatsapp-verify", "wa-update"),
        "facebook" to listOf("faceb00k", "face-book", "fb-login"),
        "amazon" to listOf("amaz0n", "amazn", "amazon-delivery", "amzn-order"),
        "sbi" to listOf("sbi-yono-update", "yono-kyc", "sbi-rewards"),
        "paytm" to listOf("paytm-kyc", "paytm-cashback", "paytm-rewards")
    )

    private val dangerousFileExtensions = setOf(
        ".apk", ".dex", ".jar", ".vbs", ".sh", ".exe", ".scr", ".bat", ".cmd", ".ps1"
    )

    fun scanUrl(rawUrl: String): LinkAnalysisResult {
        val cleanUrl = rawUrl.trim()
        val uri = try {
            Uri.parse(cleanUrl)
        } catch (e: Exception) {
            return LinkAnalysisResult(
                url = cleanUrl,
                domain = "invalid",
                riskLevel = SecurityRiskLevel.HIGH,
                indicators = listOf("Malformed URL structure: ${e.message}"),
                isIpAddress = false,
                isLookalikeDomain = false
            )
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        val indicators = mutableListOf<String>()
        var isIp = false
        var isLookalike = false

        if (scheme != "https" && scheme != "http") {
            indicators.add("Unusual protocol scheme: '$scheme'")
        }
        if (scheme == "http") {
            indicators.add("Unencrypted HTTP transmission (no TLS)")
        }

        // Direct raw IP address detection
        val ipPattern = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        if (ipPattern.matches(host)) {
            isIp = true
            indicators.add("Host is a direct raw IP address instead of a domain name")
        }

        // Embedded credentials check (e.g. http://user:pass@host)
        if (uri.userInfo != null && uri.userInfo!!.isNotBlank()) {
            indicators.add("Embedded user credentials detected in URL (potential phishing decoy)")
        }

        // Lookalike / Typosquatting detection
        for ((legit, spoofs) in lookalikeTargets) {
            for (spoof in spoofs) {
                if (host.contains(spoof)) {
                    isLookalike = true
                    indicators.add("Possible lookalike domain targeting '$legit' (detected '$spoof')")
                }
            }
        }

        // Deceptive subdomain nesting (e.g., google.com.malicious-domain.xyz)
        val hostParts = host.split(".")
        if (hostParts.size > 3) {
            val registeredTld = hostParts.takeLast(2).joinToString(".")
            for (legit in lookalikeTargets.keys) {
                if (host.contains(legit) && !registeredTld.contains(legit)) {
                    indicators.add("Deceptive subdomain nesting: brand '$legit' found inside subdomain of '$registeredTld'")
                }
            }
        }

        // Suspicious TLDs
        val tld = hostParts.lastOrNull() ?: ""
        if (suspiciousTlds.contains(tld)) {
            indicators.add("Uses high-abuse top-level domain (.$tld)")
        }

        // Dangerous direct download extension in path
        val path = uri.path?.lowercase(Locale.ROOT) ?: ""
        for (ext in dangerousFileExtensions) {
            if (path.endsWith(ext)) {
                indicators.add("Direct link to executable / installer file type ($ext)")
            }
        }

        val risk = when {
            isLookalike || (isIp && path.endsWith(".apk")) -> SecurityRiskLevel.CRITICAL
            isIp || indicators.any { it.contains("Direct link to executable") } -> SecurityRiskLevel.HIGH
            indicators.size >= 2 -> SecurityRiskLevel.MEDIUM
            indicators.isNotEmpty() -> SecurityRiskLevel.LOW
            else -> SecurityRiskLevel.SAFE
        }

        return LinkAnalysisResult(
            url = cleanUrl,
            domain = host,
            riskLevel = risk,
            indicators = indicators,
            isIpAddress = isIp,
            isLookalikeDomain = isLookalike
        )
    }
}
