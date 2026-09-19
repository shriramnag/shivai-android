package com.personal.ai.shivai

import com.personal.ai.shivai.core.agent.SafetyEngine
import com.personal.ai.shivai.core.agent.TrustLevel
import com.personal.ai.shivai.core.termux.TermuxCommandPolicy
import com.personal.ai.shivai.core.termux.TermuxRiskLevel
import org.junit.Assert.*
import org.junit.Test

class TermuxPolicyAndSafetyTest {

    private val safetyEngine = SafetyEngine()

    @Test
    fun testSafeCommands() {
        val cmds = listOf("ls -la", "pwd", "git status", "git log -n 5", "date", "whoami", "uname -a")
        for (cmd in cmds) {
            val assessment = TermuxCommandPolicy.evaluate(cmd)
            assertEquals("Expected SAFE for: $cmd", TermuxRiskLevel.SAFE, assessment.riskLevel)
            assertTrue("Expected allowed for: $cmd", assessment.isAllowed)
            assertFalse("Expected no confirmation for: $cmd", assessment.requiresConfirmation)
        }
    }

    @Test
    fun testLowRiskCommands() {
        val cmds = listOf("mkdir test_dir", "touch readme.txt", "git commit -m 'test'", "cp a.txt b.txt")
        for (cmd in cmds) {
            val assessment = TermuxCommandPolicy.evaluate(cmd)
            assertEquals("Expected LOW_RISK for: $cmd", TermuxRiskLevel.LOW_RISK, assessment.riskLevel)
            assertTrue("Expected allowed for: $cmd", assessment.isAllowed)
            assertFalse("Expected no confirmation for: $cmd", assessment.requiresConfirmation)
        }
    }

    @Test
    fun testHighRiskCommands() {
        val cmds = listOf("git push origin main", "rm file.txt", "git reset --hard", "pip install requests")
        for (cmd in cmds) {
            val assessment = TermuxCommandPolicy.evaluate(cmd)
            assertEquals("Expected HIGH_RISK for: $cmd", TermuxRiskLevel.HIGH_RISK, assessment.riskLevel)
            assertTrue("Expected allowed for: $cmd", assessment.isAllowed)
            assertTrue("Expected confirmation required for: $cmd", assessment.requiresConfirmation)
        }
    }

    @Test
    fun testDestructiveCommands() {
        val cmds = listOf("rm -rf /", "rm -rf *", "mkfs /dev/sda", ":(){ :|:& };:")
        for (cmd in cmds) {
            val assessment = TermuxCommandPolicy.evaluate(cmd)
            assertEquals("Expected DESTRUCTIVE for: $cmd", TermuxRiskLevel.DESTRUCTIVE, assessment.riskLevel)
            assertFalse("Expected blocked (not allowed) for: $cmd", assessment.isAllowed)
        }
    }

    @Test
    fun testSafetyEngineTermuxIntegration() {
        val safeRisk = safetyEngine.evaluateRisk(
            goal = "Check status",
            tool = "termux",
            action = "execute",
            commandParam = "git status"
        )
        assertEquals(TrustLevel.SAFE, safeRisk)
        assertFalse(safetyEngine.isConfirmationRequired(safeRisk))

        val highRisk = safetyEngine.evaluateRisk(
            goal = "Push to remote",
            tool = "termux",
            action = "execute",
            commandParam = "git push"
        )
        assertEquals(TrustLevel.HIGH, highRisk)
        assertTrue(safetyEngine.isConfirmationRequired(highRisk))

        val destructiveRisk = safetyEngine.evaluateRisk(
            goal = "Format",
            tool = "termux",
            action = "execute",
            commandParam = "rm -rf /"
        )
        assertEquals(TrustLevel.CRITICAL, destructiveRisk)
        assertTrue(safetyEngine.isConfirmationRequired(destructiveRisk))
    }
}
