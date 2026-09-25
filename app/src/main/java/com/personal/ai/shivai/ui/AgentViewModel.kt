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
import com.personal.ai.shivai.core.phone.CallState
import com.personal.ai.shivai.core.phone.CallStateMonitor
import com.personal.ai.shivai.core.security.*
import com.personal.ai.shivai.core.termux.TermuxBridge
import com.personal.ai.shivai.core.tools.*
import com.personal.ai.shivai.core.voice.*
import com.personal.ai.shivai.core.web.WebResearchAgent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    val appManager = AppManager(application)
    private val db = AgentDatabase.getInstance(application)
    private val memoryDao = db.memoryDao()

    val stopController = EmergencyStopController()
    private val safetyEngine = SafetyEngine()

    // ── AI Provider: Groq (FREE) ──────────────────────────────────────────
    private val groqProvider = GroqAiProvider()

    // ── Network & Offline Brain ───────────────────────────────────────────
    val connectivityMonitor = NetworkConnectivityMonitor(application)
    val isOnline: StateFlow<Boolean>     = connectivityMonitor.isOnline
    val networkType: StateFlow<String>   = connectivityMonitor.networkType
    val offlineBrain = AdvancedOfflineBrain()

    // ── Cyber Security ────────────────────────────────────────────────────
    val securityAgent   = CyberSecurityAgent(application)
    val activeThreats: StateFlow<List<SecurityThreat>>   = securityAgent.activeThreats
    val quarantinedFiles                                  = securityAgent.quarantineManager.quarantinedFiles
    val overallSecurityRisk: StateFlow<SecurityRiskLevel> = securityAgent.overallRisk

    // ── Privacy & Payment ─────────────────────────────────────────────────
    val isPrivacyModeActive: StateFlow<Boolean>           = PrivacyModeController.isPrivacyModeActive
    val activeSensitiveCategory: StateFlow<SensitiveAppCategory?> = PrivacyModeController.currentCategory
    val privacyLogs: StateFlow<List<PrivacyEvent>>        = PrivacyModeController.privacyEventLogs
    val isShieldEnabled: StateFlow<Boolean>               = PrivacyModeController.isShieldEnabled

    // ── Extended Agents ───────────────────────────────────────────────────
    private val webResearchAgent     = WebResearchAgent(securityAgent = securityAgent)
    private val fileAgent            = FileAgent(application)
    private val termuxBridge         = TermuxBridge(application)
    private val systemHealthMonitor  = SystemHealthMonitor(application)
    private val permissionIntelligence = PermissionIntelligence(application)

    // ── Phone Call Monitor ────────────────────────────────────────────────
    val callMonitor = CallStateMonitor(application)

    // ── Tool Registry ─────────────────────────────────────────────────────
    private val toolRegistry = ToolRegistry().apply {
        register(AppLauncherTool(appManager))
        register(AccessibilityClickTool())
        register(AccessibilityInputTool())
        register(GlobalNavigationTool(appManager))
        register(DelayTool())
        register(WebSearchTool(application, securityAgent))
        register(ClipboardTool(application))
        register(DeviceControlTool(application))
        register(ApkScannerTool(securityAgent))
        register(LinkScannerTool(securityAgent))
        register(QuarantineTool(securityAgent))
        register(ListAppsTool(appManager))
        register(WebResearchTool(webResearchAgent))
        register(FileAgentTool(fileAgent))
        register(TermuxTool(termuxBridge))
        register(SystemHealthTool(systemHealthMonitor, permissionIntelligence))
    }

    val taskExecutor  = TaskExecutor(toolRegistry, stopController, safetyEngine)
    val missionManager = MissionManager(toolRegistry, safetyEngine, stopController, memoryDao = memoryDao)

    val currentPackage: StateFlow<String>       = ShivAccessibilityService.currentPackage
    val isAccessibilityActive: StateFlow<Boolean> = ShivAccessibilityService.isServiceActive
    val executionLogs                             = taskExecutor.logs
    val agentStatus                               = taskExecutor.currentStatus

    private val _confirmationPrompt = MutableStateFlow<String?>(null)
    val confirmationPrompt: StateFlow<String?> = _confirmationPrompt.asStateFlow()
    private var confirmationCallback: ((Boolean) -> Unit)? = null

    // Groq API key state
    private val _groqKeySet = MutableStateFlow(false)
    val groqKeySet: StateFlow<Boolean> = _groqKeySet.asStateFlow()

    private val _groqModelName = MutableStateFlow(groqProvider.getModelName())
    val groqModelName: StateFlow<String> = _groqModelName.asStateFlow()

    // Continuous voice mode state
    private val _isContinuousVoiceOn = MutableStateFlow(false)
    val isContinuousVoiceOn: StateFlow<Boolean> = _isContinuousVoiceOn.asStateFlow()

    // ── Voice ─────────────────────────────────────────────────────────────
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

    init {
        // Start phone call monitoring
        callMonitor.register()

        // Auto-announce incoming calls
        viewModelScope.launch {
            callMonitor.callEvent.collect { event ->
                if (event.state == CallState.RINGING) {
                    val alert = callMonitor.buildVoiceAlert(event)
                    voiceController.speak(alert)
                    saveMessage("assistant", alert)
                }
            }
        }
    }

    // ── Groq API Key Setup ────────────────────────────────────────────────
    fun setGroqApiKey(key: String) {
        setGroqConfig(key, _groqModelName.value)
    }

    fun setGroqConfig(key: String, model: String? = null) {
        val normalizedKey = key.trim()
        if (normalizedKey.isNotBlank()) {
            groqProvider.setApiKey(normalizedKey)
        }
        val normalizedModel = model?.trim().orEmpty()
        if (normalizedModel.isNotBlank()) {
            groqProvider.setModel(normalizedModel)
        }
        _groqModelName.value = groqProvider.getModelName()
        _groqKeySet.value = groqProvider.hasApiKey()
        if (_groqKeySet.value) {
            val msg = "Groq AI connect हो गया! अब मैं full power में हूँ। पूछिए कुछ भी।"
            viewModelScope.launch {
                saveMessage("assistant", msg)
                voiceController.speak(msg)
            }
        }
    }

    // ── Continuous Voice Toggle ───────────────────────────────────────────
    fun toggleContinuousVoice() {
        if (_isContinuousVoiceOn.value) {
            voiceController.stopContinuousConversation()
            _isContinuousVoiceOn.value = false
            voiceController.speak("Conversation mode बंद।")
        } else {
            _isContinuousVoiceOn.value = true
            voiceController.startContinuousConversation()
            voiceController.speak("Conversation mode चालू। बोलिए, मैं सुन रहा हूँ।")
        }
    }

    // ── Main Goal Submission ──────────────────────────────────────────────
    fun submitGoal(goal: String) {
        if (goal.isBlank()) return
        viewModelScope.launch {
            saveMessage("user", goal)
            val currentPkg = ShivAccessibilityService.currentPackage.value

            // Payment Shield
            val paymentCheck = PaymentShield.evaluateAction(goal, currentPkg)
            if (paymentCheck.isBlocked) {
                val warning = PaymentShield.formatWarning(paymentCheck)
                respond(warning)
                return@launch
            }

            // Try Offline Brain first
            val offlineResult = offlineBrain.parseGoal(goal, currentPkg)

            // Missing slot
            if (offlineResult.requiresSlotPrompt && offlineResult.missingSlotName != null) {
                val slotPrompt = PendingSlotPrompt(
                    intent = offlineResult.intent,
                    targetSlot = offlineResult.missingSlotName,
                    promptMessageHindi = offlineResult.slotPromptHindi ?: "कृपया अधिक जानकारी दें।",
                    promptMessageEnglish = offlineResult.slotPromptEnglish ?: "Please provide more details.",
                    accumulatedSlots = offlineResult.slots.toMutableMap()
                )
                saveMessage("assistant", slotPrompt.promptMessageHindi)
                voiceManager.requestSlotValue(slotPrompt)
                return@launch
            }

            // Direct offline answer
            if (offlineResult.plan == null && offlineResult.directSpeechResponse != null
                && offlineResult.intent != "UNKNOWN_OFFLINE") {
                respond(offlineResult.directSpeechResponse)
                voiceManager.recordTurn(goal, offlineResult.directSpeechResponse, offlineResult.intent)
                return@launch
            }

            // Executable plan
            if (offlineResult.plan != null) {
                val feedback = offlineResult.directSpeechResponse ?: "काम शुरू हो रहा है।"
                voiceController.speak(feedback)
                val job = launch {
                    taskExecutor.executePlan(
                        plan = offlineResult.plan,
                        onStatusSpeech = { voiceController.speak(it) },
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
                voiceManager.recordTurn(goal, feedback, offlineResult.intent)
                return@launch
            }

            // Offline fallback — no internet
            if (!isOnline.value) {
                val fallback = "Internet नहीं है और यह command offline में नहीं होता। WiFi या data चालू करें।"
                respond(fallback)
                return@launch
            }

            // Phase 2: Web search / latest news / current-info routing
            if (webResearchAgent.isWebSearchNeeded(goal)) {
                val research = webResearchAgent.research(goal)
                val response = when {
                    research.sources.isNotEmpty() -> {
                        val header = if (research.warnings.isNotEmpty()) {
                            "⚠️ कुछ सुरक्षित नहीं स्रोत हटाए गए हैं।\n\n"
                        } else {
                            ""
                        }
                        "$header${research.summary}"
                    }
                    research.summary.isNotBlank() -> research.summary
                    else -> "आज की खबर / ऑनलाइन जानकारी अभी उपलब्ध नहीं है।"
                }
                respond(response)
                return@launch
            }

            // Online: Groq AI
            if (groqProvider.hasApiKey()) {
                val screenSummary = ShivAccessibilityService.instance?.captureDeviceContext()?.toSemanticSummary() ?: ""
                val aiResult = groqProvider.generateCompletion(
                    listOf(AiMessage("user", goal)),
                    screenSummary
                )
                val reply = aiResult.getOrElse {
                    if (it.message?.contains("NO_KEY", true) == true) {
                        "Groq AI key नहीं है। Settings में जाकर free key डालें — groq.com पर बनाएँ।"
                    } else {
                        it.message ?: "कुछ गड़बड़ हुई: दोबारा कोशिश करें।"
                    }
                }
                respond(reply)
                voiceManager.recordTurn(goal, reply, "GROQ_AI")
            } else {
                // No API key yet
                val noKey = "Groq AI key नहीं है। Settings में जाकर free key डालें — groq.com पर बनाएँ।"
                respond(noKey)
            }
        }
    }

    private suspend fun respond(text: String) {
        saveMessage("assistant", text)
        voiceController.speak(text)
    }

    private suspend fun saveMessage(role: String, content: String) {
        memoryDao.insertMessage(ChatMessageEntity(conversationId = "default_conv", role = role, content = content))
    }

    fun confirmAction(approved: Boolean) {
        confirmationCallback?.invoke(approved)
        confirmationCallback = null
    }

    fun triggerEmergencyStop() {
        val result = stopController.triggerEmergencyStop()
        voiceManager.reset()
        viewModelScope.launch { saveMessage("assistant", result) }
    }

    fun toggleVoice() {
        toggleContinuousVoice()
    }

    fun scanUrlManually(url: String) {
        viewModelScope.launch {
            val result = securityAgent.evaluateUrl(url)
            val msg = "Link Scan [${result.domain}]: ${result.riskLevel.name}. ${result.indicators.joinToString("; ").ifBlank { "Clean" }}"
            respond(msg)
        }
    }

    fun togglePrivacyShield(enabled: Boolean) = PrivacyModeController.setShieldEnabled(enabled)
    fun clearPrivacyLogs() = PrivacyModeController.clearLogs()

    fun restoreQuarantine(id: String) { viewModelScope.launch { securityAgent.quarantineManager.restore(id) } }
    fun deleteQuarantine(id: String)  { viewModelScope.launch { securityAgent.quarantineManager.deletePermanently(id) } }
    fun dismissThreat(id: String)     { securityAgent.clearThreat(id) }

    override fun onCleared() {
        super.onCleared()
        connectivityMonitor.release()
        voiceController.release()
        callMonitor.unregister()
    }
}
