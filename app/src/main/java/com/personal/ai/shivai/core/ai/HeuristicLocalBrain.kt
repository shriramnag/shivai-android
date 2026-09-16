package com.personal.ai.shivai.core.ai

import com.personal.ai.shivai.core.agent.TaskPlan
import com.personal.ai.shivai.core.agent.TaskStep
import com.personal.ai.shivai.core.agent.TrustLevel
import java.util.UUID

class HeuristicLocalBrain {

    fun parseGoal(goal: String, currentPackage: String): TaskPlan? {
        val clean = goal.trim().lowercase()
        val taskId = "TASK-${UUID.randomUUID().toString().take(8)}"

        if (clean in listOf("home", "go home", "होम", "होम स्क्रीन", "होम स्क्रीन पर जाओ", "होम पर जाओ")) {
            return TaskPlan(
                taskId = taskId,
                originalGoal = goal,
                steps = mutableListOf(
                    TaskStep("S-1", "Navigate to Android Home", "global_navigation", "home", emptyMap(), TrustLevel.SAFE)
                )
            )
        }

        if (clean in listOf("back", "go back", "वापस", "वापस जाओ", "पीछे जाओ")) {
            return TaskPlan(
                taskId = taskId,
                originalGoal = goal,
                steps = mutableListOf(
                    TaskStep("S-1", "Navigate Back", "global_navigation", "back", emptyMap(), TrustLevel.SAFE)
                )
            )
        }

        if (clean.contains("स्क्रीन पर क्या") || clean.contains("what is on screen") || clean.contains("read screen")) {
            return TaskPlan(
                taskId = taskId,
                originalGoal = goal,
                steps = mutableListOf(
                    TaskStep("S-1", "Capture and Read Screen Context", "screen_analyzer", "read_all", emptyMap(), TrustLevel.SAFE)
                )
            )
        }

        val openPatterns = listOf(
            Regex("(?:open|launch)\\s+([a-zA-Z0-9 ]+)", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z0-9 ]+?)\\s*(?:खोलो|kholo|start karo)", RegexOption.IGNORE_CASE)
        )
        for (p in openPatterns) {
            val match = p.find(goal)
            if (match != null) {
                val appName = match.groupValues[1].trim()
                if (appName.isNotBlank()) {
                    return TaskPlan(
                        taskId = taskId,
                        originalGoal = goal,
                        steps = mutableListOf(
                            TaskStep("S-1", "Launch Application $appName", "app_launcher", "launch", mapOf("target" to appName), TrustLevel.LOW)
                        )
                    )
                }
            }
        }

        return null
    }
}
