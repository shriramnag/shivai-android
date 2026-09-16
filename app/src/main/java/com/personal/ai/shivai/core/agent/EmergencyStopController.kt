package com.personal.ai.shivai.core.agent

import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

class EmergencyStopController {
    private val stopFlag = AtomicBoolean(false)
    private var activeJob: Job? = null

    fun registerJob(job: Job) {
        activeJob = job
        stopFlag.set(false)
    }

    fun triggerEmergencyStop(): String {
        stopFlag.set(true)
        activeJob?.cancel()
        activeJob = null
        return "Emergency Stop Activated. Active tasks, automation sequences, and voice feedback immediately cancelled."
    }

    fun isStopRequested(): Boolean = stopFlag.get()

    fun reset() {
        stopFlag.set(false)
        activeJob = null
    }
}
