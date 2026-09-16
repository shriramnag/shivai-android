package com.personal.ai.shivai

import com.personal.ai.shivai.core.ai.HeuristicLocalBrain
import org.junit.Assert.*
import org.junit.Test

class HeuristicLocalBrainTest {

    private val brain = HeuristicLocalBrain()

    @Test
    fun testGoHomeHindi() {
        val plan = brain.parseGoal("होम स्क्रीन पर जाओ", "com.some.app")
        assertNotNull(plan)
        assertEquals("global_navigation", plan?.steps?.firstOrNull()?.tool)
        assertEquals("home", plan?.steps?.firstOrNull()?.action)
    }

    @Test
    fun testGoBackHindi() {
        val plan = brain.parseGoal("वापस जाओ", "com.some.app")
        assertNotNull(plan)
        assertEquals("global_navigation", plan?.steps?.firstOrNull()?.tool)
        assertEquals("back", plan?.steps?.firstOrNull()?.action)
    }

    @Test
    fun testOpenAppHindi() {
        val plan = brain.parseGoal("WhatsApp खोलो", "com.some.app")
        assertNotNull(plan)
        assertEquals("app_launcher", plan?.steps?.firstOrNull()?.tool)
        assertEquals("WhatsApp", plan?.steps?.firstOrNull()?.params?.get("target"))
    }
}
