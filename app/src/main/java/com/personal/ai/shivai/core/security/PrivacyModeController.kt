package com.personal.ai.shivai.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PrivacyEvent(
    val id: String = "PE-${UUID.randomUUID().toString().take(8)}",
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val category: SensitiveAppCategory?,
    val actionAttempted: String,
    val wasBlocked: Boolean,
    val reason: String
)

object PrivacyModeController {

    private val _isPrivacyModeActive = MutableStateFlow(false)
    val isPrivacyModeActive: StateFlow<Boolean> = _isPrivacyModeActive.asStateFlow()

    private val _currentCategory = MutableStateFlow<SensitiveAppCategory?>(null)
    val currentCategory: StateFlow<SensitiveAppCategory?> = _currentCategory.asStateFlow()

    private val _currentPackage = MutableStateFlow("")
    val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

    private val _isShieldEnabled = MutableStateFlow(true)
    val isShieldEnabled: StateFlow<Boolean> = _isShieldEnabled.asStateFlow()

    private val _privacyEventLogs = MutableStateFlow<List<PrivacyEvent>>(emptyList())
    val privacyEventLogs: StateFlow<List<PrivacyEvent>> = _privacyEventLogs.asStateFlow()

    fun onPackageChanged(newPackage: String) {
        _currentPackage.value = newPackage
        val isSensitive = SensitiveAppRegistry.isSensitivePackage(newPackage)
        val category = SensitiveAppRegistry.getAppCategory(newPackage)

        if (isSensitive) {
            _isPrivacyModeActive.value = true
            _currentCategory.value = category
            logEvent(
                PrivacyEvent(
                    packageName = newPackage,
                    category = category,
                    actionAttempted = "App Transition",
                    wasBlocked = true,
                    reason = "Sensitive ${category?.name ?: "APP"} opened. Automation lockdown and screen privacy active."
                )
            )
        } else {
            if (_isPrivacyModeActive.value) {
                logEvent(
                    PrivacyEvent(
                        packageName = newPackage,
                        category = null,
                        actionAttempted = "App Transition",
                        wasBlocked = false,
                        reason = "Exited sensitive app. Privacy mode returned to standby."
                    )
                )
            }
            _isPrivacyModeActive.value = false
            _currentCategory.value = null
        }
    }

    fun isActionAllowed(
        targetPackage: String,
        isPasswordOrCredential: Boolean,
        actionDescription: String
    ): Boolean {
        if (!_isShieldEnabled.value) return true

        if (isPasswordOrCredential) {
            logEvent(
                PrivacyEvent(
                    packageName = targetPackage,
                    category = SensitiveAppRegistry.getAppCategory(targetPackage),
                    actionAttempted = actionDescription,
                    wasBlocked = true,
                    reason = "Attempted interaction with password/PIN/credential field."
                )
            )
            return false
        }

        if (_isPrivacyModeActive.value || SensitiveAppRegistry.isSensitivePackage(targetPackage)) {
            val cat = SensitiveAppRegistry.getAppCategory(targetPackage) ?: _currentCategory.value
            logEvent(
                PrivacyEvent(
                    packageName = targetPackage,
                    category = cat,
                    actionAttempted = actionDescription,
                    wasBlocked = true,
                    reason = "Automation blocked: ${cat?.name ?: "SENSITIVE_APP"} is in foreground."
                )
            )
            return false
        }

        return true
    }

    fun maskNodeIfNeeded(
        text: String?,
        contentDesc: String?,
        viewId: String?,
        isPassword: Boolean
    ): Pair<String, String> {
        if (isPassword) {
            return Pair("[PROTECTED_PASSWORD_FIELD]", "[PROTECTED_PASSWORD_FIELD]")
        }

        val isSensitiveField = SensitiveAppRegistry.isSensitiveField(text, contentDesc, viewId, isPassword = false)
        if (isSensitiveField) {
            return Pair("[PROTECTED_CREDENTIAL_FIELD]", "[PROTECTED_CREDENTIAL_FIELD]")
        }

        if (_isPrivacyModeActive.value) {
            return Pair("[REDACTED_SENSITIVE_CONTENT]", "[REDACTED_SENSITIVE_CONTENT]")
        }

        return Pair(text ?: "", contentDesc ?: "")
    }

    fun setShieldEnabled(enabled: Boolean) {
        _isShieldEnabled.value = enabled
    }

    fun clearLogs() {
        _privacyEventLogs.value = emptyList()
    }

    private fun logEvent(event: PrivacyEvent) {
        val current = _privacyEventLogs.value.toMutableList()
        current.add(0, event)
        if (current.size > 100) {
            _privacyEventLogs.value = current.take(100)
        } else {
            _privacyEventLogs.value = current
        }
    }
}
