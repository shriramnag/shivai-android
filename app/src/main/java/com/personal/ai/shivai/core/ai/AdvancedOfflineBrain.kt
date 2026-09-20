// Line 29 in AdvancedOfflineBrain.kt
if (clean in listOf("home", "go home", "होम", "होम स्क्रीन", "होम पर जाओ", "home jao", "home par jao", "home screen par jao")) {
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
