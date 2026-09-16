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
import com.personal.ai.shivai.core.memory.MemoryEntity
import com.personal.ai.shivai.core.tools.*
import com.personal.ai.shivai.core.voice.VoiceController
import com.personal.ai.shivai.core.voice.VoiceState
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
    private val heuristicBrain = HeuristicLocalBrain()
    private val aiProvider = OpenAiCompatibleProvider()

    private val toolRegistry = ToolRegistry().apply {
        register(AppLauncherTool(appManager))
        register(AccessibilityClickTool())
        register(AccessibilityInputTool())
        register(GlobalNavigationTool(appManager))
        register(DelayTool())
        register(WebSearchTool(application))
        register(ClipboardTool(application))
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

    fun saveMemory(key: String, content: String, category: String) {
        viewModelScope.launch {
            memoryDao.insertMemory(MemoryEntity(key = key, content = content, category = category))
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceController.release()
    }
}
