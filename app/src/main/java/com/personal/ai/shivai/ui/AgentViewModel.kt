package com.personal.ai.shivai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ai.shivai.core.agent.*
import com.personal.ai.shivai.core.ai.*
import com.personal.ai.shivai.core.automation.AppManager
import com.personal.ai.shivai.core.automation.ShivAccessibilityService
import com.personal.ai.shivai.core.memory.AgentDatabase
import com.personal.ai.shivai.core.memory.ChatMessageEntity
import com.personal.ai.shivai.core.security.*
import com.personal.ai.shivai.core.tools.*
import com.personal.ai.shivai.core.voice.VoiceController
import com.personal.ai.shivai.core.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    val appManager = AppManager(application)
    private val db = AgentDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()

    val stopController = EmergencyStopController()
    private val safetyEngine = SafetyEngine()
    private val heuristicBrain = HeuristicLocalBrain()
    private val aiProvider = OpenAiCompatibleProvider()

    // Phase 2 & 3: Cyber Security & Privacy Shield
    val securityAgent = CyberSecurityAgent(application)
    val activeThreats: StateFlow<List<SecurityThreat>> = securityAgent.activeThreats
    val quarantinedFiles = securityAgent.quarantineManager.quarantinedFiles
    val overallSecurityRisk: StateFlow<SecurityRiskLevel> = securityAgent.overallRisk

    // Phase 3: Sensitive App Privacy Mode & Payment Protection
    val isPrivacyModeActive: StateFlow<Boolean> = PrivacyModeController.isPrivacyModeActive
    val activeSensitiveCategory: StateFlow<SensitiveAppCategory?> = PrivacyModeController.currentCategory
    val privacyLogs: StateFlow<List<PrivacyEvent>> = PrivacyModeController.privacyEventLogs
    val isShieldEnabled: StateFlow<Boolean> = PrivacyModeController.isShieldEnabled

    private val toolRegistry = ToolRegistry().apply {
        register(AppLauncherTool(appManager))
        register(AccessibilityClickTool())
        register(AccessibilityInputTool())
        register(GlobalNavigationTool(appManager))
        register(DelayTool())
        register(WebSearchTool(application, securityAgent))
        register(ClipboardTool(application))
        // Security Tools
        register(ApkScannerTool(securityAgent))
        register(LinkScannerTool(securityAgent))
        register(QuarantineTool(securityAgent))
    }

    val taskExecutor = TaskExecutor(toolRegistry, stopController, safetyEngine)

    val currentPackage: StateFlow<String> = ShivAccessibilityService.currentPackage
    val isAccessibilityActive: StateFlow<Boolean> = ShivAccessibilityService.isServiceActive
    val executionLogs = taskExecutor.logs
    val agentStatus = taskExecutor.currentStatus

    private val _confirmationPrompt = MutableStateFlow<String?>(null)
    val confirmationPrompt: StateFlow<String?> = _confirmationPrompt.asStateFlow()
    private var confirmationCallback: ((Boolean) -> Unit)? = null

    val voiceController = VoiceController(application) { command ->
        submitGoal(command)
    }
    val voiceState: StateFlow<VoiceState> = voiceController.voiceState

    val conversationMessages = memoryDao.getMessages("default_conv")

    fun submitGoal(goal: String) {
        if (goal.isBlank()) return
        viewModelScope.launch {
            memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "user", content = goal))

            val currentPkg = ShivAccessibilityService.currentPackage.value

            // Payment Shield Pre-flight Check (Phase 3)
            val paymentCheck = PaymentShield.evaluateAction(goal, currentPkg)
            if (paymentCheck.isBlocked) {
                val warning = PaymentShield.formatWarning(paymentCheck)
                memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = warning))
                voiceController.speak("Payment Shield active. Automation is suspended for financial and credential safety. Please proceed manually.")
                return@launch
            }

            val plan = heuristicBrain.parseGoal(goal, currentPkg)

            if (plan == null) {
                val screenSummary = ShivAccessibilityService.instance?.captureDeviceContext()?.toSemanticSummary() ?: ""
                val aiResponse = aiProvider.generateCompletion(
                    listOf(AiMessage("user", goal)),
                    screenSummary
                )
                val responseText = aiResponse.getOrDefault("Command acknowledged.")
                memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = responseText))
                voiceController.speak(responseText)
                return@launch
            }

            val job = launch {
                taskExecutor.executePlan(
                    plan = plan,
                    onStatusSpeech = { statusText ->
                        voiceController.speak(statusText)
                    },
                    requestUserConfirmation = { preview ->
                        _confirmationPrompt.value = preview
                        kotlin.coroutines.suspendCoroutine { cont ->
                            confirmationCallback = { decision ->
                                _confirmationPrompt.value = null
                                cont.resumeWith(Result.success(decision))
                            }
                        }
                    }
                )
            }
            stopController.registerJob(job)
        }
    }

    fun confirmAction(approved: Boolean) {
        confirmationCallback?.invoke(approved)
        confirmationCallback = null
    }

    fun triggerEmergencyStop() {
        val result = stopController.triggerEmergencyStop()
        voiceController.stopSpeaking()
        voiceController.stopListening()
        viewModelScope.launch {
            memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = result))
        }
    }

    fun toggleVoice() {
        if (voiceState.value is VoiceState.Listening) {
            voiceController.stopListening()
        } else {
            voiceController.startListening("hi-IN")
        }
    }

    fun scanUrlManually(url: String) {
        viewModelScope.launch {
            val result = securityAgent.evaluateUrl(url)
            val msg = "Link Scan [${result.domain}]: ${result.riskLevel.name}. Indicators: ${result.indicators.joinToString("; ").ifBlank { "Clean" }}"
            memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = msg))
            voiceController.speak("Link analysis complete. Risk level is ${result.riskLevel.name}")
        }
    }

    fun scanApkManually(path: String) {
        viewModelScope.launch {
            val result = securityAgent.evaluateApk(path)
            val msg = "APK Scan [${result.packageName}]: ${result.riskLevel.name}. Suspicious perms: ${result.suspiciousPermissions.size}"
            memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = msg))
            voiceController.speak("APK scan complete. Risk level is ${result.riskLevel.name}")
        }
    }

    fun togglePrivacyShield(enabled: Boolean) {
        PrivacyModeController.setShieldEnabled(enabled)
    }

    fun clearPrivacyLogs() {
        PrivacyModeController.clearLogs()
    }

    fun restoreQuarantine(id: String) {
        viewModelScope.launch {
            securityAgent.quarantineManager.restore(id)
        }
    }

    fun deleteQuarantine(id: String) {
        viewModelScope.launch {
            securityAgent.quarantineManager.deletePermanently(id)
        }
    }

    fun dismissThreat(id: String) {
        securityAgent.clearThreat(id)
    }

    override fun onCleared() {
        super.onCleared()
        voiceController.release()
    }
}
