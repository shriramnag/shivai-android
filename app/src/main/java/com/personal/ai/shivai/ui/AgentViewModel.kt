package com.personal.ai.shivai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.ai.shivai.core.agent.*
import com.personal.ai.shivai.core.ai.*
import com.personal.ai.shivai.core.automation.AppManager
import com.personal.ai.shivai.core.automation.ShivAccessibilityService
import com.personal.ai.shivai.core.files.FileAgent
import com.personal.ai.shivai.core.health.PermissionIntelligence
import com.personal.ai.shivai.core.health.SystemHealthMonitor
import com.personal.ai.shivai.core.memory.AgentDatabase
import com.personal.ai.shivai.core.memory.ChatMessageEntity
import com.personal.ai.shivai.core.missions.MissionManager
import com.personal.ai.shivai.core.security.*
import com.personal.ai.shivai.core.termux.TermuxBridge
import com.personal.ai.shivai.core.tools.*
import com.personal.ai.shivai.core.voice.*
import com.personal.ai.shivai.core.web.WebResearchAgent
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
    private val aiProvider = OpenAiCompatibleProvider()

    // Phase 4: Network Connectivity & Advanced Offline Brain
    val connectivityMonitor = NetworkConnectivityMonitor(application)
    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline
    val networkType: StateFlow<String> = connectivityMonitor.networkType
    val offlineBrain = AdvancedOfflineBrain()

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

    // Extended Agents
    private val webResearchAgent = WebResearchAgent(securityAgent = securityAgent)
    private val fileAgent = FileAgent(application)
    private val termuxBridge = TermuxBridge(application)
    private val systemHealthMonitor = SystemHealthMonitor(application)
    private val permissionIntelligence = PermissionIntelligence(application)

    private val toolRegistry = ToolRegistry().apply {
        register(AppLauncherTool(appManager))
        register(AccessibilityClickTool())
        register(AccessibilityInputTool())
        register(GlobalNavigationTool(appManager))
        register(DelayTool())
        register(WebSearchTool(application, securityAgent))
        register(ClipboardTool(application))
        // Phase 4: Device Hardware Control Tool
        register(DeviceControlTool(application))
        // Phase 2: Security Tools
        register(ApkScannerTool(securityAgent))
        register(LinkScannerTool(securityAgent))
        register(QuarantineTool(securityAgent))
        // Extended Tools
        register(WebResearchTool(webResearchAgent))
        register(FileAgentTool(fileAgent))
        register(TermuxTool(termuxBridge))
        register(SystemHealthTool(systemHealthMonitor, permissionIntelligence))
    }

    val taskExecutor = TaskExecutor(toolRegistry, stopController, safetyEngine)
    val missionManager = MissionManager(toolRegistry, safetyEngine, stopController, memoryDao = memoryDao)

    val currentPackage: StateFlow<String> = ShivAccessibilityService.currentPackage
    val isAccessibilityActive: StateFlow<Boolean> = ShivAccessibilityService.isServiceActive
    val executionLogs = taskExecutor.logs
    val agentStatus = taskExecutor.currentStatus

    private val _confirmationPrompt = MutableStateFlow<String?>(null)
    val confirmationPrompt: StateFlow<String?> = _confirmationPrompt.asStateFlow()
    private var confirmationCallback: ((Boolean) -> Unit)? = null

    // Phase 4: Multi-Turn Voice Controller and Dialog Manager
    val voiceController = VoiceController(application) { command ->
        voiceManager.onUserSpeechReceived(command)
    }
    val voiceState: StateFlow<VoiceState> = voiceController.voiceState

    val voiceManager: MultiTurnVoiceManager = MultiTurnVoiceManager(
        voiceController = voiceController,
        scope = viewModelScope,
        onExecuteGoal = { goal -> submitGoal(goal) }
    )
    val dialogState: StateFlow<DialogSessionState> = voiceManager.sessionState
    val dialogHistory: StateFlow<List<DialogTurn>> = voiceManager.dialogHistory

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
                voiceManager.speakFinal("Payment Shield active. Financial and credential operations must be done manually for your security.")
                voiceManager.recordTurn(goal, warning, "PAYMENT_BLOCKED")
                return@launch
            }

            // Phase 4: Parse with Advanced Offline Brain
            val offlineResult = offlineBrain.parseGoal(goal, currentPkg)

            // 1. Missing Slot Handling (Multi-Turn Slot Filling)
            if (offlineResult.requiresSlotPrompt && offlineResult.missingSlotName != null) {
                val slotPrompt = PendingSlotPrompt(
                    intent = offlineResult.intent,
                    targetSlot = offlineResult.missingSlotName,
                    promptMessageHindi = offlineResult.slotPromptHindi ?: "कृपया अतिरिक्त जानकारी प्रदान करें।",
                    promptMessageEnglish = offlineResult.slotPromptEnglish ?: "Please provide more details.",
                    accumulatedSlots = offlineResult.slots.toMutableMap()
                )
                memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = slotPrompt.promptMessageHindi))
                voiceManager.requestSlotValue(slotPrompt)
                return@launch
            }

            // 2. Direct Speech Answer (No Execution Plan Needed)
            if (offlineResult.plan == null && offlineResult.directSpeechResponse != null && offlineResult.intent != "UNKNOWN_OFFLINE") {
                val reply = offlineResult.directSpeechResponse
                memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = reply))
                voiceManager.speakFinal(reply)
                voiceManager.recordTurn(goal, reply, offlineResult.intent, offlineResult.slots)
                return@launch
            }

            // 3. Executable Plan from Offline Brain
            if (offlineResult.plan != null) {
                val feedbackSpeech = offlineResult.directSpeechResponse ?: "कार्रवाई की जा रही है।"
                voiceController.speak(feedbackSpeech)

                val job = launch {
                    taskExecutor.executePlan(
                        plan = offlineResult.plan,
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
                voiceManager.recordTurn(goal, feedbackSpeech, offlineResult.intent, offlineResult.slots)
                return@launch
            }

            // 4. Intent Not Matched in Local Brain -> Check Connectivity
            if (!isOnline.value) {
                val offlineFallback = offlineResult.directSpeechResponse
                    ?: "इंटरनेट कनेक्शन उपलब्ध नहीं है और यह कमांड ऑफ़लाइन समर्थित नहीं है।"
                memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = offlineFallback))
                voiceManager.speakFinal(offlineFallback)
                voiceManager.recordTurn(goal, offlineFallback, "OFFLINE_FALLBACK")
                return@launch
            }

            // 5. Online: Fallback to Cloud AI Provider
            val screenSummary = ShivAccessibilityService.instance?.captureDeviceContext()?.toSemanticSummary() ?: ""
            val aiResponse = aiProvider.generateCompletion(
                listOf(AiMessage("user", goal)),
                screenSummary
            )
            val responseText = aiResponse.getOrElse {
                "मैं यह समझ नहीं पाया। कृपया दोबारा कहें, या API key सेट करें ताकि मैं cloud AI से जवाब दे सकूँ।"
            }
            memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = "assistant", content = responseText))
            voiceManager.speakFinal(responseText)
            voiceManager.recordTurn(goal, responseText, "ONLINE_LLM")
        }
    }

    fun confirmAction(approved: Boolean) {
        confirmationCallback?.invoke(approved)
        confirmationCallback = null
    }

    fun triggerEmergencyStop() {
        val result = stopController.triggerEmergencyStop()
        voiceManager.reset()
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
        connectivityMonitor.release()
        voiceController.release()
    }
}
