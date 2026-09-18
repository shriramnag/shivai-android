package com.personal.ai.shivai.core.security

import java.net.URI
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
