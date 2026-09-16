package com.personal.ai.shivai.core.security

import java.util.Locale

enum class SensitiveAppCategory {
    UPI_PAYMENT,
    BANKING,
    PASSWORD_MANAGER,
    AUTHENTICATOR,
    SYSTEM_CREDENTIALS,
    CRYPTO_WALLET
}

object SensitiveAppRegistry {

    private val upiPackages = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "in.org.npci.upiapp",                    // BHIM UPI
        "com.dreamplug.androidapp",              // CRED
        "com.mobikwik_new",                      // MobiKwik
        "com.freecharge.android",                // Freecharge
        "com.samsung.android.spay",              // Samsung Pay
        "com.samsung.android.spaymini",          // Samsung Pay Mini
        "com.myairtelapp",                       // Airtel Thanks
        "com.jio.myjio"                          // MyJio
    )

    private val bankingPackages = setOf(
        "com.sbi.lotusintouch",                  // YONO SBI
        "com.sbi.SBIFreedomPlus",                // YONO Lite SBI
        "com.snapwork.hdfc",                     // HDFC Bank MobileBanking
        "com.csam.icici.bank.imobile",           // ICICI iMobile Pay
        "com.axis.mobile",                       // Axis Mobile
        "com.msf.kbank.mobile",                  // Kotak 811
        "com.pnb.pnbone",                        // PNB ONE
        "com.bob.bobworld",                      // bob World
        "com.canarabank.mobility",               // Canara ai1
        "com.infrasofttech.uboi",                // Union Bank Vyom
        "com.indusind.mpassbook",                // IndusInd IndusMobile
        "com.idfcfirstbank.optimus",             // IDFC FIRST Mobile Banking
        "com.rblbank.mobank",                    // RBL MoBank
        "com.fss.fednet",                        // Federal Bank FedMobile
        "com.yesbank",                           // YES Bank MobileBanking
        "com.sc.eCommerce.mobile.in"             // Standard Chartered India
    )

    private val passwordManagerPackages = setOf(
        "com.x8bit.bitwarden",                   // Bitwarden
        "com.agilebits.onepassword",             // 1Password
        "com.lastpass.lpandroid",                // LastPass
        "com.dashlane",                          // Dashlane
        "keepass2android.keepass2android",       // KeePass2Android
        "keepass2android.keepass2android_nonet", // KeePass2Android offline
        "io.enpass.app",                         // Enpass
        "me.proton.pass",                        // Proton Pass
        "com.nordpass.android",                  // NordPass
        "com.keepersecurity.android"             // Keeper
    )

    private val authenticatorPackages = setOf(
        "com.google.android.apps.authenticator2", // Google Authenticator
        "com.azure.authenticator",               // Microsoft Authenticator
        "com.authy.authy",                       // Twilio Authy
        "com.duosecurity.duomobile",             // Duo Mobile
        "com.beemdevelopment.aegis",             // Aegis Authenticator
        "org.fedorahosted.freeotp"               // FreeOTP
    )

    private val systemCredentialPackages = setOf(
        "com.android.keyguard",
        "com.android.systemui.biometrics",
        "com.google.android.gms.auth",
        "com.android.credentialmanager",
        "com.android.settings.password"
    )

    private val cryptoPackages = setOf(
        "io.metamask",                           // MetaMask
        "com.wallet.crypto.trustapp",            // Trust Wallet
        "com.binance.dev",                       // Binance
        "com.coinbase.android",                  // Coinbase
        "com.coindcx.btc",                       // CoinDCX
        "com.wazirx.trade"                       // WazirX
    )

    private val sensitiveKeywords = listOf(
        "otp", "one-time password", "verification code", "auth code", "security code",
        "pin", "mpin", "upi pin", "atm pin", "passcode", "cvv", "cvc", "card number",
        "debit card", "credit card", "expiry date", "password", "netbanking password",
        "secret key", "seed phrase", "private key", "ओटीपी", "पिन", "पासवर्ड"
    )

    private val paymentActionKeywords = listOf(
        "pay", "pay ₹", "pay rs", "proceed to pay", "make payment", "send money",
        "transfer now", "confirm transfer", "submit upi pin", "enter upi pin",
        "approve transaction", "confirm payment", "पैसे भेजें", "भुगतान करें"
    )

    fun isSensitivePackage(packageName: String): Boolean {
        val lower = packageName.lowercase(Locale.ROOT)
        if (upiPackages.contains(lower) ||
            bankingPackages.contains(lower) ||
            passwordManagerPackages.contains(lower) ||
            authenticatorPackages.contains(lower) ||
            systemCredentialPackages.contains(lower) ||
            cryptoPackages.contains(lower)
        ) {
            return true
        }

        // Substring / heuristic patterns for financial and security applications
        return lower.contains(".bank.") ||
                lower.contains(".banking.") ||
                lower.contains(".wallet.") ||
                lower.contains("authenticator") ||
                lower.contains("passwordmanager")
    }

    fun getAppCategory(packageName: String): SensitiveAppCategory? {
        val lower = packageName.lowercase(Locale.ROOT)
        return when {
            upiPackages.contains(lower) -> SensitiveAppCategory.UPI_PAYMENT
            bankingPackages.contains(lower) || lower.contains(".bank.") || lower.contains(".banking.") -> SensitiveAppCategory.BANKING
            passwordManagerPackages.contains(lower) || lower.contains("password") -> SensitiveAppCategory.PASSWORD_MANAGER
            authenticatorPackages.contains(lower) || lower.contains("authenticator") -> SensitiveAppCategory.AUTHENTICATOR
            systemCredentialPackages.contains(lower) -> SensitiveAppCategory.SYSTEM_CREDENTIALS
            cryptoPackages.contains(lower) || lower.contains(".wallet.") -> SensitiveAppCategory.CRYPTO_WALLET
            else -> null
        }
    }

    fun isSensitiveField(
        text: String?,
        contentDescription: String?,
        viewId: String?,
        isPassword: Boolean
    ): Boolean {
        if (isPassword) return true

        val combined = "${text ?: ""} ${contentDescription ?: ""} ${viewId ?: ""}".lowercase(Locale.ROOT)
        return sensitiveKeywords.any { keyword ->
            combined.contains(keyword)
        }
    }

    fun isPaymentAction(
        text: String?,
        contentDescription: String?,
        viewId: String?
    ): Boolean {
        val combined = "${text ?: ""} ${contentDescription ?: ""} ${viewId ?: ""}".lowercase(Locale.ROOT)
        return paymentActionKeywords.any { keyword ->
            combined.contains(keyword)
        }
    }

    fun getTotalRegisteredAppsCount(): Int {
        return upiPackages.size + bankingPackages.size + passwordManagerPackages.size +
                authenticatorPackages.size + systemCredentialPackages.size + cryptoPackages.size
    }
}
