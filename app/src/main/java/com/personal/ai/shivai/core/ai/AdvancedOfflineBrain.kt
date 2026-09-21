package com.personal.ai.shivai.core.ai

import com.personal.ai.shivai.core.agent.TaskPlan
import com.personal.ai.shivai.core.agent.TaskStep
import com.personal.ai.shivai.core.agent.TrustLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class OfflineIntentResult(
    val intent: String,
    val slots: Map<String, String>,
    val plan: TaskPlan?,
    val directSpeechResponse: String?,
    val requiresSlotPrompt: Boolean = false,
    val missingSlotName: String? = null,
    val slotPromptHindi: String? = null,
    val slotPromptEnglish: String? = null
)

class AdvancedOfflineBrain {

    fun parseGoal(goal: String, currentPackage: String = ""): OfflineIntentResult {
        val clean = goal.trim().lowercase(Locale.ROOT)
        val taskId = "TASK-${UUID.randomUUID().toString().take(8)}"

        // 0. Greetings & Smalltalk (highest priority — answer instantly offline)
        if (clean in listOf(
                "hello", "hi", "hey", "हेलो", "हैलो", "नमस्ते", "नमस्कार",
                "हेलो शिव", "हेलो शिव ai", "हेलो सीबीआई", "hello shiv", "hello ai",
                "good morning", "good evening", "good night", "शुभ प्रभात", "शुभ रात्रि"
            )
        ) {
            return OfflineIntentResult(
                intent = "GREETING",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "नमस्ते! मैं Shiv AI हूँ — आपका पर्सनल ऑन-डिवाइस AI असिस्टेंट। मैं आपकी क्या सहायता कर सकता हूँ?"
            )
        }

        // Coding / creation requests — honest offline response
        if (clean.contains("game") || clean.contains("गेम") ||
            clean.contains("बनाओ") || clean.contains("बना सकते") ||
            clean.contains("code") || clean.contains("कोड") ||
            clean.contains("app बनाओ") || clean.contains("website")) {
            return OfflineIntentResult(
                intent = "CODING_REQUEST",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "मैं अभी coding और app/game बनाने के लिए cloud AI पर निर्भर हूँ। कृपया internet connect करें और API key सेट करें, फिर मैं पूरी मदद कर सकता हूँ।"
            )
        }

        // 1. Navigation Intents
        if (clean in listOf("home", "go home", "होम", "होम स्क्रीन", "होम पर जाओ", "home jao", "home par jao")) {
            return OfflineIntentResult(
                intent = "NAVIGATE_HOME",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Navigate to Android Home", "global_navigation", "home", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "Going to home screen."
            )
        }

        if (clean in listOf("back", "go back", "वापस", "वापस जाओ", "पीछे जाओ", "back jao")) {
            return OfflineIntentResult(
                intent = "NAVIGATE_BACK",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Navigate Back", "global_navigation", "back", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "Going back."
            )
        }

        if (clean in listOf("recents", "recent apps", "रीसेंट", "टास्क", "recent apps kholo")) {
            return OfflineIntentResult(
                intent = "NAVIGATE_RECENTS",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Open Recent Apps", "global_navigation", "recents", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "Opening recent apps."
            )
        }

        // 2. Hardware / Device Controls
        if (clean.contains("torch on") || clean.contains("flashlight on") || clean.contains("टॉर्च चालू") || clean.contains("लाइट जलाओ") || clean.contains("torch chalu")) {
            return OfflineIntentResult(
                intent = "TORCH_ON",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Turn ON Flashlight", "device_control", "torch_on", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "टॉर्च चालू कर दी गई है।"
            )
        }

        if (clean.contains("torch off") || clean.contains("flashlight off") || clean.contains("टॉर्च बंद") || clean.contains("लाइट बंद") || clean.contains("torch band")) {
            return OfflineIntentResult(
                intent = "TORCH_OFF",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Turn OFF Flashlight", "device_control", "torch_off", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "टॉर्च बंद कर दी गई है।"
            )
        }

        if (clean.contains("volume up") || clean.contains("आवाज़ बढ़ाओ") || clean.contains("aawaz badhao") || clean.contains("volume badhao")) {
            return OfflineIntentResult(
                intent = "VOLUME_UP",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Increase Volume", "device_control", "volume_up", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "वॉल्यूम बढ़ा दिया गया है।"
            )
        }

        if (clean.contains("volume down") || clean.contains("आवाज़ कम") || clean.contains("aawaz kam") || clean.contains("volume kam")) {
            return OfflineIntentResult(
                intent = "VOLUME_DOWN",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Decrease Volume", "device_control", "volume_down", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "वॉल्यूम कम कर दिया गया है।"
            )
        }

        if (clean.contains("mute") || clean.contains("म्यूट") || clean.contains("आवाज़ बंद") || clean.contains("silent karo")) {
            return OfflineIntentResult(
                intent = "MUTE",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Mute Audio", "device_control", "mute", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "आवाज़ म्यूट कर दी गई है।"
            )
        }

        if (clean.contains("wifi") && (clean.contains("setting") || clean.contains("kholo") || clean.contains("on") || clean.contains("खोल"))) {
            return OfflineIntentResult(
                intent = "WIFI_SETTINGS",
                slots = mapOf("target" to "wifi"),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Open Wi-Fi Settings", "device_control", "open_settings", mapOf("target" to "wifi"), TrustLevel.LOW)
                    )
                ),
                directSpeechResponse = "Opening Wi-Fi settings."
            )
        }

        if (clean.contains("bluetooth") && (clean.contains("setting") || clean.contains("kholo") || clean.contains("on") || clean.contains("खोल"))) {
            return OfflineIntentResult(
                intent = "BLUETOOTH_SETTINGS",
                slots = mapOf("target" to "bluetooth"),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Open Bluetooth Settings", "device_control", "open_settings", mapOf("target" to "bluetooth"), TrustLevel.LOW)
                    )
                ),
                directSpeechResponse = "Opening Bluetooth settings."
            )
        }

        // 3. Timer Detection
        val timerRegex = Regex("(\\d+)\\s*(?:minute|min|मिनट|sec|second|सेकंड)\\s*(?:ka)?\\s*(?:timer|अलार्म)?", RegexOption.IGNORE_CASE)
        val timerMatch = timerRegex.find(clean)
        if (clean.contains("timer") && timerMatch != null) {
            val amount = timerMatch.groupValues[1].toIntOrNull() ?: 5
            val isSeconds = clean.contains("sec") || clean.contains("सेकंड")
            val totalSeconds = if (isSeconds) amount else amount * 60
            return OfflineIntentResult(
                intent = "SET_TIMER",
                slots = mapOf("seconds" to totalSeconds.toString()),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Set Timer", "device_control", "set_timer", mapOf("seconds" to totalSeconds.toString()), TrustLevel.LOW)
                    )
                ),
                directSpeechResponse = "$amount ${if (isSeconds) "सेकंड" else "मिनट"} का टाइमर लगा दिया गया है।"
            )
        }

        // 4. Time & Date Direct Answers (Offline)
        if (clean.contains("time") || clean.contains("समय") || clean.contains("kitne baje") || clean.contains("कितने बजे")) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return OfflineIntentResult(
                intent = "GET_TIME",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "अभी समय $timeStr है।"
            )
        }

        if (clean.contains("date") || clean.contains("तारीख") || clean.contains("दिन") || clean.contains("aaj kaun sa din")) {
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            return OfflineIntentResult(
                intent = "GET_DATE",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "आज $dateStr है।"
            )
        }

        // 5. Calling / Phone Dialing
        val callPatterns = listOf(
            Regex("(?:call|phone|डायल|कॉल)\\s*(?:karo|lagao|to)?\\s*([a-zA-Z0-9 ]+)", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z0-9 ]+?)\\s*(?:ko)?\\s*(?:call|phone|कॉल)\\s*(?:lagao|karo)", RegexOption.IGNORE_CASE)
        )
        for (p in callPatterns) {
            val match = p.find(goal)
            if (match != null) {
                val contactName = match.groupValues[1].trim()
                if (contactName.isNotBlank() && contactName !in listOf("karo", "lagao", "kisko", "phone", "call")) {
                    return OfflineIntentResult(
                        intent = "DIAL_CALL",
                        slots = mapOf("target" to contactName),
                        plan = TaskPlan(
                            taskId = taskId,
                            originalGoal = goal,
                            steps = mutableListOf(
                                TaskStep("S-1", "Dial $contactName", "device_control", "dial_contact", mapOf("target" to contactName), TrustLevel.LOW)
                            )
                        ),
                        directSpeechResponse = "$contactName के लिए डायलर खोला जा रहा है।"
                    )
                }
            }
        }
        if (clean == "call" || clean == "call karo" || clean == "phone lagao" || clean == "कॉल करो") {
            return OfflineIntentResult(
                intent = "DIAL_CALL",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = null,
                requiresSlotPrompt = true,
                missingSlotName = "target",
                slotPromptHindi = "आप किसे कॉल करना चाहते हैं?",
                slotPromptEnglish = "Who would you like to call?"
            )
        }

        // 6. Screen Reading Context
        if (clean.contains("स्क्रीन पर क्या") || clean.contains("what is on screen") || clean.contains("read screen") || clean.contains("स्क्रीन पढ़ो")) {
            return OfflineIntentResult(
                intent = "READ_SCREEN",
                slots = emptyMap(),
                plan = TaskPlan(
                    taskId = taskId,
                    originalGoal = goal,
                    steps = mutableListOf(
                        TaskStep("S-1", "Read Screen Context", "accessibility_input", "read", emptyMap(), TrustLevel.SAFE)
                    )
                ),
                directSpeechResponse = "स्क्रीन का विश्लेषण किया जा रहा है।"
            )
        }

        // 7. App Launching Patterns
        val openPatterns = listOf(
            Regex("(?:open|launch|start)\\s+([a-zA-Z0-9 ]+)", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z0-9 ]+?)\\s*(?:खोलो|kholo|chalu karo|start karo)", RegexOption.IGNORE_CASE)
        )
        for (p in openPatterns) {
            val match = p.find(goal)
            if (match != null) {
                val appName = match.groupValues[1].trim()
                if (appName.isNotBlank() && appName !in listOf("kholo", "chalu karo", "app")) {
                    return OfflineIntentResult(
                        intent = "LAUNCH_APP",
                        slots = mapOf("target" to appName),
                        plan = TaskPlan(
                            taskId = taskId,
                            originalGoal = goal,
                            steps = mutableListOf(
                                TaskStep("S-1", "Launch Application $appName", "app_launcher", "launch", mapOf("target" to appName), TrustLevel.LOW)
                            )
                        ),
                        directSpeechResponse = "$appName खोला जा रहा है।"
                    )
                }
            }
        }

        // 8. Offline Smalltalk & Assistant Info
        if (clean.contains("who are you") || clean.contains("aap kaun ho") || clean.contains("tum kaun ho") || clean.contains("तुम्हारा नाम क्या है")) {
            return OfflineIntentResult(
                intent = "IDENTITY",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "मैं Shiv AI हूँ — आपका पर्सनल और सुरक्षित ऑन-डिवाइस AI असिस्टेंट।"
            )
        }

        if (clean.contains("kaise ho") || clean.contains("how are you") || clean.contains("kya haal")) {
            return OfflineIntentResult(
                intent = "GREETING",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "मैं बिल्कुल ठीक हूँ। आज मैं आपकी क्या सहायता कर सकता हूँ?"
            )
        }

        if (clean.contains("thank") || clean.contains("dhanyawad") || clean.contains("shukriya") || clean.contains("धन्यवाद")) {
            return OfflineIntentResult(
                intent = "THANK_YOU",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "आपका स्वागत है! किसी और मदद की आवश्यकता हो तो बताएं।"
            )
        }

        if (clean.contains("kya kar sakte ho") || clean.contains("what can you do") || clean.contains("features") || clean.contains("मदद")) {
            return OfflineIntentResult(
                intent = "HELP",
                slots = emptyMap(),
                plan = null,
                directSpeechResponse = "मैं ऐप्स खोल सकता हूँ, कॉल मिला सकता हूँ, टाइमर लगा सकता हूँ, टॉर्च और वॉल्यूम नियंत्रित कर सकता हूँ, और स्क्रीन पढ़ सकता हूँ — इंटरनेट के बिना भी।"
            )
        }

        // 9. Generic Offline Fallback
        return OfflineIntentResult(
            intent = "UNKNOWN_OFFLINE",
            slots = emptyMap(),
            plan = null,
            directSpeechResponse = "यह कमांड ऑफ़लाइन प्रोसेस नहीं हो सकी। आप ऐप्स खोलने, कॉल करने, टाइमर सेट करने, टॉर्च/वॉल्यूम कंट्रोल करने, या स्क्रीन पढ़ने के लिए कह सकते हैं।"
        )
    }
}
