package com.personal.ai.shivai.core.security

import java.util.Locale

data class PaymentShieldAssessment(
    val isBlocked: Boolean,
    val riskLevel: SecurityRiskLevel,
    val reason: String,
    val requiresManualUserAction: Boolean
)

object PaymentShield {

    private val paymentKeywords = listOf(
        "pay", "payment", "send money", "transfer", "gpay", "phonepe", "paytm",
        "upi", "bhim", "neft", "rtgs", "imps", "bhim upi", "पैसे ट्रांसफर", "पैसे भेजो", "पेमेंट"
    )

    fun evaluateAction(
        actionOrGoal: String,
        currentPackage: String,
        isCredentialField: Boolean = false
    ): PaymentShieldAssessment {
        val isSensitivePkg = SensitiveAppRegistry.isSensitivePackage(currentPackage)
        val category = SensitiveAppRegistry.getAppCategory(currentPackage)
        val lowerGoal = actionOrGoal.lowercase(Locale.ROOT)

        if (isCredentialField) {
            return PaymentShieldAssessment(
                isBlocked = true,
                riskLevel = SecurityRiskLevel.CRITICAL,
                reason = "Target element is a protected PIN/Password/OTP credential field. Automation strictly forbidden.",
                requiresManualUserAction = true
            )
        }

        if (isSensitivePkg && (category == SensitiveAppCategory.UPI_PAYMENT || category == SensitiveAppCategory.BANKING)) {
            val isPaymentTrigger = paymentKeywords.any { lowerGoal.contains(it) } ||
                    SensitiveAppRegistry.isPaymentAction(actionOrGoal, null, null)

            return if (isPaymentTrigger) {
                PaymentShieldAssessment(
                    isBlocked = true,
                    riskLevel = SecurityRiskLevel.CRITICAL,
                    reason = "Live payment authorization detected in ${category.name} app ($currentPackage). Automation paused for user safety.",
                    requiresManualUserAction = true
                )
            } else {
                PaymentShieldAssessment(
                    isBlocked = true,
                    riskLevel = SecurityRiskLevel.HIGH,
                    reason = "Sensitive banking/payment interface is active ($currentPackage). Automated interactions locked down.",
                    requiresManualUserAction = true
                )
            }
        }

        if (paymentKeywords.any { lowerGoal.contains(it) }) {
            return PaymentShieldAssessment(
                isBlocked = false,
                riskLevel = SecurityRiskLevel.MEDIUM,
                reason = "Financial transaction keywords detected. Proceed with explicit user confirmation.",
                requiresManualUserAction = false
            )
        }

        return PaymentShieldAssessment(
            isBlocked = false,
            riskLevel = SecurityRiskLevel.SAFE,
            reason = "No payment threat detected.",
            requiresManualUserAction = false
        )
    }

    fun formatWarning(assessment: PaymentShieldAssessment): String {
        return """
            🛡️ PAYMENT SHIELD ACTIVATED
            Status: AUTOMATION PAUSED
            Reason: ${assessment.reason}
            Notice: Shiv AI never asks for, views, stores, or inputs your UPI PIN, passwords, or OTPs. Please complete this step manually on your device.
        """.trimIndent()
    }
}
