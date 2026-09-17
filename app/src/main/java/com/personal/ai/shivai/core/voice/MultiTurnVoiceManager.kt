package com.personal.ai.shivai.core.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class MultiTurnVoiceManager(
    private val voiceController: VoiceController,
    private val scope: CoroutineScope,
    private val onExecuteGoal: (String) -> Unit
) {

    private val _sessionState = MutableStateFlow(DialogSessionState.IDLE)
    val sessionState: StateFlow<DialogSessionState> = _sessionState.asStateFlow()

    private val _dialogHistory = MutableStateFlow<List<DialogTurn>>(emptyList())
    val dialogHistory: StateFlow<List<DialogTurn>> = _dialogHistory.asStateFlow()

    private var pendingSlot: PendingSlotPrompt? = null
    private var pendingConfirmation: PendingConfirmation? = null

    private val affirmativeTokens = setOf(
        "yes", "yeah", "yep", "sure", "proceed", "confirm", "ok", "okay",
        "haan", "ha", "theek hai", "sahi hai", "kar do", "karo", "zaroor",
        "हाँ", "हाँजी", "ठीक है", "ज़रूर", "करो", "कर दो", "पुष्टि"
    )

    private val negativeTokens = setOf(
        "no", "nope", "cancel", "stop", "abort", "halt", "nevermind",
        "nahi", "na", "mat karo", "roko", "rahne do", "chhod do",
        "नहीं", "ना", "मत करो", "रोको", "रद्द करो", "कैंसल"
    )

    fun onUserSpeechReceived(utterance: String) {
        val clean = utterance.trim().lowercase(Locale.ROOT)
        if (clean.isBlank()) {
            _sessionState.value = DialogSessionState.IDLE
            return
        }

        // 1. Handle Pending Confirmation
        if (_sessionState.value == DialogSessionState.AWAITING_CONFIRMATION && pendingConfirmation != null) {
            val conf = pendingConfirmation!!
            when {
                isAffirmative(clean) -> {
                    pendingConfirmation = null
                    _sessionState.value = DialogSessionState.PROCESSING
                    voiceController.speak("पुष्टि प्राप्त हुई। कार्य शुरू किया जा रहा है।")
                    scope.launch { conf.onConfirmed() }
                }
                isNegative(clean) -> {
                    pendingConfirmation = null
                    _sessionState.value = DialogSessionState.IDLE
                    voiceController.speak("कार्य रद्द कर दिया गया।")
                    scope.launch { conf.onCancelled() }
                }
                else -> {
                    speakAndListenAgain("कृपया हाँ या नहीं बोलकर पुष्टि करें।")
                }
            }
            return
        }

        // 2. Handle Pending Slot Filling
        if (_sessionState.value == DialogSessionState.AWAITING_SLOT_VALUE && pendingSlot != null) {
            val slotPrompt = pendingSlot!!
            slotPrompt.accumulatedSlots[slotPrompt.targetSlot] = utterance.trim()
            pendingSlot = null
            _sessionState.value = DialogSessionState.PROCESSING

            val reconstructedGoal = when (slotPrompt.intent) {
                "DIAL_CALL" -> "call ${slotPrompt.accumulatedSlots["target"]}"
                "LAUNCH_APP" -> "open ${slotPrompt.accumulatedSlots["target"]}"
                "SET_TIMER" -> "${slotPrompt.accumulatedSlots["seconds"]} सेकंड का टाइमर लगाओ"
                else -> utterance
            }
            onExecuteGoal(reconstructedGoal)
            return
        }

        // 3. Normal Speech Processing
        _sessionState.value = DialogSessionState.PROCESSING
        onExecuteGoal(utterance)
    }

    fun requestConfirmation(
        summaryMessage: String,
        onConfirmed: suspend () -> Unit,
        onCancelled: suspend () -> Unit
    ) {
        pendingConfirmation = PendingConfirmation(summaryMessage, onConfirmed, onCancelled)
        _sessionState.value = DialogSessionState.AWAITING_CONFIRMATION
        speakAndListenAgain(summaryMessage)
    }

    fun requestSlotValue(slotPrompt: PendingSlotPrompt) {
        pendingSlot = slotPrompt
        _sessionState.value = DialogSessionState.AWAITING_SLOT_VALUE
        speakAndListenAgain(slotPrompt.promptMessageHindi)
    }

    fun recordTurn(
        userUtterance: String,
        assistantResponse: String,
        intent: String = "GENERAL",
        slots: Map<String, String> = emptyMap()
    ) {
        val turn = DialogTurn(
            turnId = "TURN-${UUID.randomUUID().toString().take(6)}",
            userUtterance = userUtterance,
            assistantResponse = assistantResponse,
            recognizedIntent = intent,
            extractedSlots = slots
        )
        val current = _dialogHistory.value.toMutableList()
        current.add(turn)
        if (current.size > 20) {
            _dialogHistory.value = current.takeLast(20)
        } else {
            _dialogHistory.value = current
        }
    }

    fun speakAndListenAgain(text: String) {
        _sessionState.value = DialogSessionState.SPEAKING
        voiceController.speak(text) {
            _sessionState.value = DialogSessionState.LISTENING
            voiceController.startListening("hi-IN")
        }
    }

    fun speakFinal(text: String) {
        _sessionState.value = DialogSessionState.SPEAKING
        voiceController.speak(text) {
            _sessionState.value = DialogSessionState.IDLE
        }
    }

    fun reset() {
        pendingConfirmation = null
        pendingSlot = null
        _sessionState.value = DialogSessionState.IDLE
        voiceController.stopListening()
        voiceController.stopSpeaking()
    }

    private fun isAffirmative(text: String): Boolean {
        return affirmativeTokens.any { token ->
            text == token || text.contains(token)
        }
    }

    private fun isNegative(text: String): Boolean {
        return negativeTokens.any { token ->
            text == token || text.contains(token)
        }
    }
}
