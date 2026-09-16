package com.personal.ai.shivai

import com.personal.ai.shivai.core.agent.SafetyEngine
import com.personal.ai.shivai.core.agent.TrustLevel
import com.personal.ai.shivai.core.security.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PrivacyModeTest {

    private val safetyEngine = SafetyEngine()

    @Before
    fun setup() {
        PrivacyModeController.clearLogs()
        PrivacyModeController.setShieldEnabled(true)
        PrivacyModeController.onPackageChanged("com.personal.ai.shivai")
    }

    @Test
    fun testUpiAndBankingAppDetection() {
        // UPI payment apps
        val gpay = "com.google.android.apps.nbu.paisa.user"
        val phonepe = "com.phonepe.app"
        val paytm = "net.one97.paytm"
        val bhim = "in.org.npci.upiapp"

        assertTrue(SensitiveAppRegistry.isSensitivePackage(gpay))
        assertEquals(SensitiveAppCategory.UPI_PAYMENT, SensitiveAppRegistry.getAppCategory(gpay))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(phonepe))
        assertEquals(SensitiveAppCategory.UPI_PAYMENT, SensitiveAppRegistry.getAppCategory(phonepe))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(paytm))
        assertEquals(SensitiveAppCategory.UPI_PAYMENT, SensitiveAppRegistry.getAppCategory(paytm))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(bhim))
        assertEquals(SensitiveAppCategory.UPI_PAYMENT, SensitiveAppRegistry.getAppCategory(bhim))

        // Banking apps
        val sbi = "com.sbi.lotusintouch"
        val hdfc = "com.snapwork.hdfc"
        val icici = "com.csam.icici.bank.imobile"

        assertTrue(SensitiveAppRegistry.isSensitivePackage(sbi))
        assertEquals(SensitiveAppCategory.BANKING, SensitiveAppRegistry.getAppCategory(sbi))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(hdfc))
        assertEquals(SensitiveAppCategory.BANKING, SensitiveAppRegistry.getAppCategory(hdfc))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(icici))
        assertEquals(SensitiveAppCategory.BANKING, SensitiveAppRegistry.getAppCategory(icici))
    }

    @Test
    fun testPasswordManagerAndAuthenticatorDetection() {
        val bitwarden = "com.x8bit.bitwarden"
        val onePassword = "com.agilebits.onepassword"
        val googleAuth = "com.google.android.apps.authenticator2"
        val msAuth = "com.azure.authenticator"

        assertTrue(SensitiveAppRegistry.isSensitivePackage(bitwarden))
        assertEquals(SensitiveAppCategory.PASSWORD_MANAGER, SensitiveAppRegistry.getAppCategory(bitwarden))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(onePassword))
        assertEquals(SensitiveAppCategory.PASSWORD_MANAGER, SensitiveAppRegistry.getAppCategory(onePassword))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(googleAuth))
        assertEquals(SensitiveAppCategory.AUTHENTICATOR, SensitiveAppRegistry.getAppCategory(googleAuth))

        assertTrue(SensitiveAppRegistry.isSensitivePackage(msAuth))
        assertEquals(SensitiveAppCategory.AUTHENTICATOR, SensitiveAppRegistry.getAppCategory(msAuth))
    }

    @Test
    fun testNonSensitiveAppPassThrough() {
        val calc = "com.google.android.calculator"
        val clock = "com.google.android.deskclock"

        assertFalse(SensitiveAppRegistry.isSensitivePackage(calc))
        assertNull(SensitiveAppRegistry.getAppCategory(calc))

        assertFalse(SensitiveAppRegistry.isSensitivePackage(clock))
        assertNull(SensitiveAppRegistry.getAppCategory(clock))
    }

    @Test
    fun testSensitiveFieldDetection() {
        // Password flag
        assertTrue(SensitiveAppRegistry.isSensitiveField(null, null, null, isPassword = true))

        // OTP / PIN text
        assertTrue(SensitiveAppRegistry.isSensitiveField("Enter 6-digit OTP", null, "txt_otp", isPassword = false))
        assertTrue(SensitiveAppRegistry.isSensitiveField("Enter your UPI PIN", null, "edit_pin", isPassword = false))
        assertTrue(SensitiveAppRegistry.isSensitiveField(null, "CVV security code", "et_cvv", isPassword = false))
        assertTrue(SensitiveAppRegistry.isSensitiveField("खाता पासवर्ड दर्ज करें", null, null, isPassword = false))

        // Normal field
        assertFalse(SensitiveAppRegistry.isSensitiveField("Search messages", "Search", "search_box", isPassword = false))
    }

    @Test
    fun testPaymentShieldEvaluation() {
        val gpay = "com.google.android.apps.nbu.paisa.user"

        // Live payment in sensitive app
        val payAssessment = PaymentShield.evaluateAction("Pay ₹500 to Sharmaji", gpay)
        assertTrue(payAssessment.isBlocked)
        assertEquals(SecurityRiskLevel.CRITICAL, payAssessment.riskLevel)
        assertTrue(payAssessment.requiresManualUserAction)

        // Password / PIN entry attempt
        val pinAssessment = PaymentShield.evaluateAction("1234", gpay, isCredentialField = true)
        assertTrue(pinAssessment.isBlocked)
        assertEquals(SecurityRiskLevel.CRITICAL, pinAssessment.riskLevel)

        // Warning format test
        val warningText = PaymentShield.formatWarning(payAssessment)
        assertTrue(warningText.contains("PAYMENT SHIELD ACTIVATED"))
        assertTrue(warningText.contains("AUTOMATION PAUSED"))
    }

    @Test
    fun testPrivacyModeLockdownWorkflow() {
        assertFalse(PrivacyModeController.isPrivacyModeActive.value)

        // Transition to PhonePe
        PrivacyModeController.onPackageChanged("com.phonepe.app")
        assertTrue(PrivacyModeController.isPrivacyModeActive.value)
        assertEquals(SensitiveAppCategory.UPI_PAYMENT, PrivacyModeController.currentCategory.value)

        // Actions must be blocked
        val clickAllowed = PrivacyModeController.isActionAllowed(
            targetPackage = "com.phonepe.app",
            isPasswordOrCredential = false,
            actionDescription = "Click Pay"
        )
        assertFalse(clickAllowed)
        assertTrue(PrivacyModeController.privacyEventLogs.value.isNotEmpty())

        // Node masking check
        val (maskedText, maskedDesc) = PrivacyModeController.maskNodeIfNeeded(
            text = "Balance: ₹45,000",
            contentDesc = "Account details",
            viewId = "acc_balance",
            isPassword = false
        )
        assertEquals("[REDACTED_SENSITIVE_CONTENT]", maskedText)
        assertEquals("[REDACTED_SENSITIVE_CONTENT]", maskedDesc)

        // Exiting to standard app
        PrivacyModeController.onPackageChanged("com.google.android.calculator")
        assertFalse(PrivacyModeController.isPrivacyModeActive.value)
        assertNull(PrivacyModeController.currentCategory.value)
    }

    @Test
    fun testSafetyEngineIntegration() {
        val gpay = "com.google.android.apps.nbu.paisa.user"

        // Sensitive app evaluated as CRITICAL
        val riskLevel = safetyEngine.evaluateRisk("Check transaction", "accessibility_click", "click", gpay)
        assertEquals(TrustLevel.CRITICAL, riskLevel)
        assertTrue(safetyEngine.isConfirmationRequired(riskLevel))

        // Automation is rejected
        assertFalse(safetyEngine.isAutomationPermitted(gpay))
        assertFalse(safetyEngine.isAutomationPermitted("com.any.app", isPasswordOrPin = true))
        assertTrue(safetyEngine.isAutomationPermitted("com.google.android.calculator", isPasswordOrPin = false))
    }
}
