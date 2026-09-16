package com.personal.ai.shivai

import com.personal.ai.shivai.core.security.DownloadGuard
import com.personal.ai.shivai.core.security.LinkSecurityScanner
import com.personal.ai.shivai.core.security.SecurityRiskLevel
import com.personal.ai.shivai.core.security.SecurityThreat
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CyberSecurityAgentTest {

    private val linkScanner = LinkSecurityScanner()
    private val downloadGuard = DownloadGuard()

    @Test
    fun testSafeUrlScanning() {
        val result = linkScanner.scanUrl("https://en.wikipedia.org/wiki/Android")
        assertEquals(SecurityRiskLevel.SAFE, result.riskLevel)
        assertTrue(result.indicators.isEmpty())
        assertFalse(result.isIpAddress)
        assertFalse(result.isLookalikeDomain)
    }

    @Test
    fun testLookalikePhishingUrlDetection() {
        val result = linkScanner.scanUrl("http://g00gle.com/login")
        assertEquals(SecurityRiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.isLookalikeDomain)
        assertTrue(result.indicators.any { it.contains("lookalike domain") })
    }

    @Test
    fun testRawIpApkDownloadDetection() {
        val result = linkScanner.scanUrl("http://192.168.1.50/malicious_update.apk")
        assertEquals(SecurityRiskLevel.CRITICAL, result.riskLevel)
        assertTrue(result.isIpAddress)
        assertTrue(result.indicators.any { it.contains("executable / installer") })
    }

    @Test
    fun testDoubleExtensionDetection() {
        val tempFile = File.createTempFile("salary_slip", ".pdf.apk")
        tempFile.writeText("sample data")
        tempFile.deleteOnExit()

        val guardResult = downloadGuard.inspectFile(tempFile)
        assertEquals(SecurityRiskLevel.CRITICAL, guardResult.riskLevel)
        assertTrue(guardResult.warnings.any { it.contains("double extension") })
        assertNotNull(guardResult.sha256)
    }

    @Test
    fun testSecurityAlertFormatting() {
        val threat = SecurityThreat(
            id = "T-101",
            title = "Phishing URL Intercepted",
            risk = SecurityRiskLevel.CRITICAL,
            evidence = "Lookalike domain spoofing google",
            recommendedAction = "Do not open or share credentials",
            source = "http://g00gle.com"
        )
        val alert = threat.formatAlert()
        assertTrue(alert.contains("🚨 SECURITY ALERT"))
        assertTrue(alert.contains("Threat: Phishing URL Intercepted"))
        assertTrue(alert.contains("Risk: CRITICAL"))
        assertTrue(alert.contains("Evidence: Lookalike domain spoofing google"))
    }
}
