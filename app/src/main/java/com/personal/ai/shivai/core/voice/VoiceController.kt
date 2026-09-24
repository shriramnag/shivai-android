package com.personal.ai.shivai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle      : VoiceState()
    object Listening : VoiceState()
    data class Recognized(val text: String) : VoiceState()
    data class Speaking(val text: String)   : VoiceState()
    data class Error(val message: String)   : VoiceState()
}

class VoiceController(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    // Continuous conversation mode
    private var isContinuousMode = false
    private var silenceHandler   = Handler(Looper.getMainLooper())
    private var silenceRunnable: Runnable? = null
    private val SILENCE_TIMEOUT_MS = 2000L  // 2 seconds silence → auto respond

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    var onSpeechCompletedCallback: (() -> Unit)? = null

    init { textToSpeech = TextToSpeech(context, this) }

    // ── Normal one-shot listen ────────────────────────────────────────────
    fun startListening(languageCode: String = "hi-IN") {
        isContinuousMode = false
        startRecognizer(languageCode)
    }

    // ── Continuous conversation mode ──────────────────────────────────────
    fun startContinuousConversation() {
        isContinuousMode = true
        startRecognizer("hi-IN")
    }

    fun stopContinuousConversation() {
        isContinuousMode = false
        cancelSilenceTimer()
        stopListening()
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Idle
        }
    }

    private fun startRecognizer(lang: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("Speech recognizer unavailable.")
            return
        }
        stopSpeaking()
        destroyRecognizer()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceController)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("hi-IN", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            if (isContinuousMode) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MS)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MS)
            }
        }
        speechRecognizer?.startListening(intent)
        _voiceState.value = VoiceState.Listening
    }

    // ── TTS ───────────────────────────────────────────────────────────────
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) { onComplete?.invoke(); return }
        cancelSilenceTimer()
        onSpeechCompletedCallback = onComplete
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        textToSpeech?.language = if (hasHindi) Locale("hi", "IN") else Locale("en", "IN")
        textToSpeech?.setSpeechRate(0.95f)
        textToSpeech?.setPitch(1.0f)
        val uid = "ShivUtterance_${System.currentTimeMillis()}"
        _voiceState.value = VoiceState.Speaking(text)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, uid)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) textToSpeech?.stop()
        if (_voiceState.value is VoiceState.Speaking) _voiceState.value = VoiceState.Idle
    }

    // ── Silence timer ─────────────────────────────────────────────────────
    private fun startSilenceTimer() {
        cancelSilenceTimer()
        silenceRunnable = Runnable {
            if (_voiceState.value is VoiceState.Listening && isContinuousMode) {
                speechRecognizer?.stopListening()
            }
        }
        silenceHandler.postDelayed(silenceRunnable!!, SILENCE_TIMEOUT_MS)
    }

    private fun cancelSilenceTimer() {
        silenceRunnable?.let { silenceHandler.removeCallbacks(it) }
        silenceRunnable = null
    }

    // ── RecognitionListener ───────────────────────────────────────────────
    override fun onResults(results: Bundle?) {
        cancelSilenceTimer()
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()?.trim() ?: ""
        if (text.isNotBlank()) {
            _voiceState.value = VoiceState.Recognized(text)
            onCommandRecognized(text)
        } else {
            _voiceState.value = VoiceState.Idle
            if (isContinuousMode) restartListeningAfterDelay()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull() ?: ""
        if (text.isNotBlank()) {
            _voiceState.value = VoiceState.Recognized(text)
            startSilenceTimer() // restart timer when partial result comes
        }
    }

    override fun onBeginningOfSpeech() { cancelSilenceTimer() }

    override fun onEndOfSpeech() {
        if (isContinuousMode) startSilenceTimer()
    }

    override fun onError(error: Int) {
        cancelSilenceTimer()
        val ignore = error == SpeechRecognizer.ERROR_NO_MATCH ||
                     error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        if (!ignore) {
            _voiceState.value = VoiceState.Error("Error $error")
        } else {
            _voiceState.value = VoiceState.Idle
        }
        if (isContinuousMode) restartListeningAfterDelay()
    }

    private fun restartListeningAfterDelay(delayMs: Long = 800L) {
        silenceHandler.postDelayed({
            if (isContinuousMode && _voiceState.value !is VoiceState.Speaking) {
                startRecognizer("hi-IN")
            }
        }, delayMs)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uid: String?) {}
                override fun onDone(uid: String?) {
                    _voiceState.value = VoiceState.Idle
                    val cb = onSpeechCompletedCallback; onSpeechCompletedCallback = null
                    cb?.invoke()
                    // In continuous mode, restart listening after AI speaks
                    if (isContinuousMode) {
                        silenceHandler.postDelayed({ startRecognizer("hi-IN") }, 300L)
                    }
                }
                override fun onError(uid: String?) {
                    _voiceState.value = VoiceState.Idle
                    val cb = onSpeechCompletedCallback; onSpeechCompletedCallback = null
                    cb?.invoke()
                    if (isContinuousMode) {
                        silenceHandler.postDelayed({ startRecognizer("hi-IN") }, 300L)
                    }
                }
            })
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun destroyRecognizer() {
        speechRecognizer?.destroy(); speechRecognizer = null
    }

    fun release() {
        isContinuousMode = false
        cancelSilenceTimer()
        destroyRecognizer()
        textToSpeech?.stop(); textToSpeech?.shutdown()
    }
}
