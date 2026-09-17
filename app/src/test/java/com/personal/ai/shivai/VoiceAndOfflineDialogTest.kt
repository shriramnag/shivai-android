package com.personal.ai.shivai

import com.personal.ai.shivai.core.ai.AdvancedOfflineBrain
import org.junit.Assert.*
import org.junit.Test

class VoiceAndOfflineDialogTest {

    private val offlineBrain = AdvancedOfflineBrain()

    @Test
    fun testNavigationIntents() {
        val homeResult = offlineBrain.parseGoal("home par jao")
        assertEquals("NAVIGATE_HOME", homeResult.intent)
        assertNotNull(homeResult.plan)
        assertEquals("global_navigation", homeResult.plan?.steps?.first()?.tool)
        assertEquals("home", homeResult.plan?.steps?.first()?.action)

        val backResult = offlineBrain.parseGoal("वापस जाओ")
        assertEquals("NAVIGATE_BACK", backResult.intent)
        assertEquals("back", backResult.plan?.steps?.first()?.action)

        val recentsResult = offlineBrain.parseGoal("recent apps")
        assertEquals("NAVIGATE_RECENTS", recentsResult.intent)
    }

    @Test
    fun testHardwareDeviceControls() {
        // Flashlight / Torch
        val torchOnHindi = offlineBrain.parseGoal("टॉर्च चालू करो")
        assertEquals("TORCH_ON", torchOnHindi.intent)
        assertEquals("device_control", torchOnHindi.plan?.steps?.first()?.tool)
        assertEquals("torch_on", torchOnHindi.plan?.steps?.first()?.action)

        val torchOffEng = offlineBrain.parseGoal("flashlight off")
        assertEquals("TORCH_OFF", torchOffEng.intent)
        assertEquals("torch_off", torchOffEng.plan?.steps?.first()?.action)

        // Volume Controls
        val volumeUp = offlineBrain.parseGoal("aawaz badhao")
        assertEquals("VOLUME_UP", volumeUp.intent)

        val volumeDown = offlineBrain.parseGoal("आवाज़ कम करो")
        assertEquals("VOLUME_DOWN", volumeDown.intent)

        val mute = offlineBrain.parseGoal("silent karo")
        assertEquals("MUTE", mute.intent)

        // Settings Shortcuts
        val wifi = offlineBrain.parseGoal("wifi settings kholo")
        assertEquals("WIFI_SETTINGS", wifi.intent)
        assertEquals("wifi", wifi.slots["target"])

        val bluetooth = offlineBrain.parseGoal("bluetooth setting kholo")
        assertEquals("BLUETOOTH_SETTINGS", bluetooth.intent)
        assertEquals("bluetooth", bluetooth.slots["target"])
    }

    @Test
    fun testTimerParsing() {
        val fiveMin = offlineBrain.parseGoal("5 minute ka timer lagao")
        assertEquals("SET_TIMER", fiveMin.intent)
        assertEquals("300", fiveMin.slots["seconds"])

        val thirtySec = offlineBrain.parseGoal("30 sec ka timer")
        assertEquals("SET_TIMER", thirtySec.intent)
        assertEquals("30", thirtySec.slots["seconds"])
    }

    @Test
    fun testDirectTimeAndDateResponses() {
        val timeQuery = offlineBrain.parseGoal("kitne baje hain")
        assertEquals("GET_TIME", timeQuery.intent)
        assertNull(timeQuery.plan)
        assertNotNull(timeQuery.directSpeechResponse)
        assertTrue(timeQuery.directSpeechResponse!!.contains("समय"))

        val dateQuery = offlineBrain.parseGoal("aaj kaun sa din hai")
        assertEquals("GET_DATE", dateQuery.intent)
        assertNull(dateQuery.plan)
        assertNotNull(dateQuery.directSpeechResponse)
        assertTrue(dateQuery.directSpeechResponse!!.contains("आज"))
    }

    @Test
    fun testPhoneDialingAndSlotFilling() {
        // Complete command
        val directCall = offlineBrain.parseGoal("Rahul ko call lagao")
        assertEquals("DIAL_CALL", directCall.intent)
        assertEquals("Rahul", directCall.slots["target"])
        assertNotNull(directCall.plan)

        // Incomplete command requiring slot prompt
        val incompleteCall = offlineBrain.parseGoal("call karo")
        assertEquals("DIAL_CALL", incompleteCall.intent)
        assertTrue(incompleteCall.requiresSlotPrompt)
        assertEquals("target", incompleteCall.missingSlotName)
        assertNotNull(incompleteCall.slotPromptHindi)
        assertNotNull(incompleteCall.slotPromptEnglish)
    }

    @Test
    fun testAppLauncherIntent() {
        val openWhatsapp = offlineBrain.parseGoal("WhatsApp kholo")
        assertEquals("LAUNCH_APP", openWhatsapp.intent)
        assertEquals("WhatsApp", openWhatsapp.slots["target"])

        val launchYoutube = offlineBrain.parseGoal("open YouTube")
        assertEquals("LAUNCH_APP", launchYoutube.intent)
        assertEquals("YouTube", launchYoutube.slots["target"])
    }

    @Test
    fun testOfflineSmalltalkAndIdentity() {
        val who = offlineBrain.parseGoal("aap kaun ho")
        assertEquals("IDENTITY", who.intent)
        assertTrue(who.directSpeechResponse!!.contains("Shiv AI"))

        val how = offlineBrain.parseGoal("kaise ho")
        assertEquals("GREETING", how.intent)

        val thanks = offlineBrain.parseGoal("dhanyawad")
        assertEquals("THANK_YOU", thanks.intent)

        val help = offlineBrain.parseGoal("kya kar sakte ho")
        assertEquals("HELP", help.intent)
    }

    @Test
    fun testGenericOfflineFallback() {
        val unknown = offlineBrain.parseGoal("kuch anokhi baat batao jisme koi match na ho")
        assertEquals("UNKNOWN_OFFLINE", unknown.intent)
        assertNull(unknown.plan)
        assertNotNull(unknown.directSpeechResponse)
        assertTrue(unknown.directSpeechResponse!!.contains("ऑफ़लाइन"))
    }
}
