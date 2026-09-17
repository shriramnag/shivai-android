package com.personal.ai.shivai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Recognized(val text: String) : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceController(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    var onSpeechCompletedCallback: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    fun startListening(languageCode: String = "hi-IN") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.Error("Speech recognizer unavailable on this device.")
            return
        }

        stopSpeaking()
        destroyRecognizer()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(this@VoiceController)
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("hi-IN", "en-IN", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.startListening(intent)
        _voiceState.value = VoiceState.Listening
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.Idle
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!isTtsInitialized || textToSpeech == null) {
            onComplete?.invoke()
            return
        }

        this.onSpeechCompletedCallback = onComplete

        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        textToSpeech?.language = if (hasDevanagari) Locale("hi", "IN") else Locale("en", "IN")

        val utteranceId = "ShivUtterance_${System.currentTimeMillis()}"
        _voiceState.value = VoiceState.Speaking(text)
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        if (_voiceState.value is VoiceState.Speaking) {
            _voiceState.value = VoiceState.Idle
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.Idle
                    val callback = onSpeechCompletedCallback
                    onSpeechCompletedCallback = null
                    callback?.invoke()
                }
                override fun onError(utteranceId: String?) {**Phase 4: Multi-Turn Voice Dialog & Advanced Offline Fallbacks** को **Shiv AI** के आर्किटेक्चर में पूरी तरह लागू (implement) कर दिया गया है।

इस चरण के अंतर्गत, इंटरनेट कनेक्शन न होने पर भी सिस्टम पूरी तरह ऑन-डिवाइस कार्य करता है और वॉयस संवाद में एकाधिक टर्न (multi-turn context), स्लॉट-फ़िलिंग (slot-filling), और वॉयस कन्फर्मेशन को सहज रूप से संभालता है।

---

### Phase 4 में जोड़े व अपडेट किए गए घटक

1. **`NetworkConnectivityMonitor.kt`**:
   - Android `ConnectivityManager.NetworkCallback` का उपयोग करके रियल-टाइम इंटरनेट उपलब्धता (`isOnline: StateFlow<Boolean>`) और नेटवर्क प्रकार (Wi-Fi, Cellular, Offline) को ट्रैक करता है।
   - इंटरनेट न होने पर बिना किसी क्रैश या एरर के अपने-आप ऑन-डिवाइस इंजन पर स्विच हो जाता है।
2. **`AdvancedOfflineBrain.kt`**:
   - पूर्ववर्ती बेसिक हेयूरिस्टिक इंजन को एक शक्तिशाली ऑन-डिवाइस NLU इंजन में अपग्रेड किया गया है, जो हिंदी, इंग्लिश और हिंग्लिश में 100% ऑफ़लाइन काम करता है:
     - **हार्डवेयर नियंत्रण**: टॉर्च ऑन/ऑफ (`torch_on`, `torch_off`), वॉल्यूम नियंत्रण (`volume_up`, `volume_down`, `mute`), सिस्टम सेटिंग्स (Wi-Fi, Bluetooth).
     - **टाइमर और घड़ी**: "5 मिनट का टाइमर लगाओ" (सेकंड्स गणना), समय ("कितने बजे हैं"), तारीख ("आज कौन सा दिन है").
     - **फ़ोन डायलिंग और स्लॉट प्रॉम्प्टिंग**: "कॉल करो" पर संपर्क नाम पूछना, "राहुल को कॉल लगाओ" पर सीधे डायलर खोलना।
     - **सिस्टम नेविगेशन**: होम, बैक, रीसेंट ऐप्स।
     - **स्क्रीन वाचन**: "स्क्रीन पर क्या है", "स्क्रीन पढ़ो"।
     - **ऐप लॉन्चर**: हिंदी/इंग्लिश में ऐप्स खोलना।
     - **ऑफ़लाइन वार्तालाप**: पहचान ("आप कौन हो"), सहायता ("क्या कर सकते हो"), धन्यवाद।
     - **ऑफ़लाइन सेफ़्टी फ़ॉलबैक**: अज्ञात कमांड पर स्पष्ट मार्गदर्शन।
3. **`DeviceControlTool.kt`**:
   - `CameraManager` (टॉर्च), `AudioManager` (वॉल्यूम/म्यूट), `AlarmClock` (टाइमर), और `Settings` के लिए नेटिव `AgentTool`।
4. **`VoiceDialogModel.kt` & `MultiTurnVoiceManager.kt`**:
   - **Dialog State Machine**: `IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`, `AWAITING_CONFIRMATION`, `AWAITING_SLOT_VALUE`.
   - **Multi-Turn Slot Filling**: अधूरी कमांड्स (जैसे "कॉल करो") में गायब स्लॉट की पहचान कर यूज़र से पूछना ("आप किसे कॉल करना चाहते हैं?") और उत्तर मिलने पर कार्य पूरा करना।
   - **वॉयस कन्फर्मेशन**: संवेदनशील कार्यों के लिए "हाँ / ठीक है / Yes / Confirm" या "नहीं / Cancel / Roko" की वॉयस-पुष्टि लेना।
   - **इको-सप्रेशन और ऑटो-रिज्यूम**: जब एजेंट बोलता है तो माइक म्यूट रहता है; प्रश्न पूछने के तुरंत बाद माइक ऑटोमैटिकली चालू हो जाता है।
5. **`VoiceController.kt` (अपडेटेड)**:
   - `speak(text, onComplete)` में स्पीच कम्प्लीशन कॉलबैक जोड़ा गया ताकि मल्टी-टर्न लूप सुचारू रूप से चल सके।
6. **`AgentViewModel.kt` & `AgentHomeScreen.kt` (अपडेटेड)**:
   - ऑनलाइन/ऑफ़लाइन बैज (`🟢 Wi-Fi` / `⚡ Offline Brain Active`), एक्टिव डायलॉग स्टेट इंडिकेटर, और वॉयस इंटरेक्शन को स्क्रीन पर जोड़ा गया।
7. **`VoiceAndOfflineDialogTest.kt`**:
   - नेविगेशन, हार्डवेयर टॉर्च/वॉल्यूम, टाइमर पार्सिंग, फ़ोन स्लॉट-फ़िलिंग, और ऑफ़लाइन रिस्पॉन्स के लिए संपूर्ण यूनिट टेस्ट्स।

---

## Phase 4 का संपूर्ण कोड

### 1. `app/src/main/java/com/personal/ai/shivai/core/ai/NetworkConnectivityMonitor.kt`
```kotlin
package com.personal.ai.shivai.core.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkConnectivityMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkType = MutableStateFlow(determineNetworkType())
    val networkType: StateFlow<String> = _networkType.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            _networkType.value = determineNetworkType()
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkCurrentConnectivity()
            _networkType.value = determineNetworkType()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isOnline.value = hasInternet
            _networkType.value = determineNetworkType()
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (_: Exception) {}
    }

    fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun determineNetworkType(): String {
        val cm = connectivityManager ?: return "None"
        val activeNetwork = cm.activeNetwork ?: return "None"
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return "None"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Offline"
        }
    }

    fun release() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {}
    }
}
