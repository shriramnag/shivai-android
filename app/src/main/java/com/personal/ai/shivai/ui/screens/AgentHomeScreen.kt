package com.personal.ai.shivai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ai.shivai.core.memory.ChatMessageEntity
import com.personal.ai.shivai.core.voice.DialogSessionState
import com.personal.ai.shivai.core.voice.VoiceState
import com.personal.ai.shivai.ui.AgentViewModel

@Composable
fun AgentHomeScreen(viewModel: AgentViewModel) {
    val currentApp by viewModel.currentPackage.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val agentStatus by viewModel.agentStatus.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.conversationMessages.collectAsState(initial = emptyList())
    val confirmationPrompt by viewModel.confirmationPrompt.collectAsState()

    // Phase 4: Connectivity & Dialog States
    val isOnline by viewModel.isOnline.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyModeActive.collectAsState()

    var textInput by remember { mutableStateOf("") }

    if (confirmationPrompt != null) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmAction(false) },
            title = { Text("Safety Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text(confirmationPrompt ?: "") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAction(true) }
                ) { Text("CONFIRM") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.confirmAction(false) }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Status Bar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = Card**Phase 4: Multi-Turn Voice Dialog & Advanced Offline Fallbacks** को **Shiv AI** के आर्किटेक्चर में पूरी तरह लागू (implement) कर दिया गया है।

मास्टर आर्किटेक्चर के **Zero-Cloud-Dependency & Continuous Dialogue Directives** के तहत, यह मॉड्यूल यह सुनिश्चित करता है कि:
1. **मल्टी-टर्न वॉयस डायलॉग स्टेट मशीन (Stateful Dialog Flow)**: 
   - छह स्पष्ट अवस्थाएँ (`IDLE`, `LISTENING`, `PROCESSING`, `SPEAKING`, `AWAITING_CONFIRMATION`, `AWAITING_SLOT_VALUE`)।
   - **स्लॉट फिलिंग (Slot Filling)**: यदि यूज़र केवल "कॉल करो" या "टाइमर लगाओ" कहता है, तो एजेंट पूछता है: *"आप किसे कॉल करना चाहते हैं?"* और उत्तर सुनकर स्लॉट भरकर कार्य पूरा करता है।
   - **इको सप्रेशन व ऑटो-लिसनिंग (Echo Suppression & Auto-Resume)**: जब एजेंट बोल रहा होता है, तब माइक्रोफ़ोन म्यूट रहता है ताकि वह अपनी ही आवाज़ न सुने। जैसे ही TTS बोलना समाप्त करता है (`onDone`), माइक्रोफ़ोन तुरंत अपने-आप सक्रिय होकर यूज़र के उत्तर की प्रतीक्षा करता है।
   - **हाँ/ना पुष्टि (Affirmative/Negative Resolution)**: हिंदी, हिंग्लिश और अंग्रेज़ी में प्राकृतिक स्वीकृति ("हाँ", "ठीक है", "ज़रूर", "yes", "proceed") या अस्वीकृति ("नहीं", "मत करो", "रोको", "no", "cancel") की पहचान।
2. **एडवांस्ड ऑफ़लाइन फॉलबैक इंजन (`AdvancedOfflineBrain`)**:
   - इंटरनेट न होने पर भी 100% ऑन-डिवाइस कार्य:
     - **हार्डवेयर व डिवाइस कंट्रोल**: टॉर्च/फ्लैशलाइट ऑन-ऑफ, वॉल्यूम अप/डाउन/म्यूट, वाई-फाई और ब्लूटूथ सेटिंग्स शॉर्टकट।
     - **टाइमर व घड़ी**: सेकंड/मिनट का टाइमर सेट करना, वर्तमान समय व तारीख़ बताना।
     - **नेविगेशन व ऐप लॉन्चर**: होम, बैक, रीसेंट ऐप्स, और किसी भी ऐप को खोलना।
     - **स्क्रीन एनालाइज़र**: स्क्रीन की सामग्री पढ़ना।
     - **ऑफ़लाइन स्मॉलटॉक व सुरक्षा फॉलबैक**: बिना नेटवर्क क्रैश या हैंग हुए उत्तर देना।
3. **नेटवर्क कनेक्टिविटी मॉनिटर (`NetworkConnectivityMonitor`)**:
   - Android `ConnectivityManager.NetworkCallback` का उपयोग करके रियल-टाइम नेटवर्क ट्रांजिशन को ट्रैक करता है (Wi-Fi, Cellular, Offline) और सहजता से ऑफ़लाइन-टू-ऑनलाइन और ऑनलाइन-टू-ऑफ़लाइन हैंडओवर करता है।

---

### Phase 4 में निर्मित व अपडेट किए गए मुख्य घटक

1. **`NetworkConnectivityMonitor.kt`**: रियल-टाइम नेटवर्क कनेक्टिविटी डिटेक्शन।
2. **`VoiceDialogModel.kt`**: `DialogSessionState`, `DialogTurn`, `PendingSlotPrompt`, और `PendingConfirmation` डेटा मॉडल।
3. **`VoiceController.kt` (अपडेटेड)**: `UtteranceProgressListener` के साथ `speak(text, onComplete)` कॉलबैक का समर्थन।
4. **`MultiTurnVoiceManager.kt`**: मल्टी-टर्न बातचीत, ऑटो-रिज्यूम लिसनिंग, और स्लॉट-फिलिंग कंट्रोलर।
5. **`AdvancedOfflineBrain.kt`**: डिवाइस कंट्रोल, टाइमर, फ़ोन डायलिंग, ऐप लॉन्चिंग, और ऑफ़लाइन संवाद के लिए समृद्ध NLU इंजन।
6. **`DeviceControlTool.kt`**: कैमरा टॉर्च (`CameraManager`), वॉल्यूम (`AudioManager`), और टाइमर (`AlarmClock`) हार्डवेयर टूल।
7. **`AgentViewModel.kt` (अपडेटेड)**: कनेक्टिविटी मॉनिटर, एडवांस्ड ऑफ़लाइन ब्रेन, और मल्टी-टर्न वॉयस मैनेजर का पूर्ण समन्वय।
8. **`AgentHomeScreen.kt` (अपडेटेड)**: लाइव नेटवर्क स्थिति बैज (`🟢 Wi-Fi Online` / `⚡ Offline Brain`) और वॉयस डायलॉग स्टेट डिस्प्ले।
9. **`VoiceAndOfflineDialogTest.kt`**: हार्डवेयर कंट्रोल, टाइमर, नेविगेशन, स्लॉट फिलिंग, और ऑफ़लाइन डायलॉग के लिए यूनिट टेस्ट्स।

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
