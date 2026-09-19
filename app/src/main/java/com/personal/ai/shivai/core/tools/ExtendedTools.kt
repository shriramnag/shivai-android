package com.personal.ai.shivai.core.tools

import android.content.Context
import com.personal.ai.shivai.core.files.FileAgent
import com.personal.ai.shivai.core.health.PermissionIntelligence
import com.personal.ai.shivai.core.health.SystemHealthMonitor
import com.personal.ai.shivai.core.termux.TermuxBridge
import com.personal.ai.shivai.core.web.WebResearchAgent

class WebResearchTool(private val webAgent: WebResearchAgent) : AgentTool {
    override val name = "web_research"
    override val description = "Conducts privacy-preserving DuckDuckGo web research with link security scans."

    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val query = params["query"] ?: return ToolExecutionResult(false, "Missing 'query' parameter.")
        val research = webAgent.research(query)
        return ToolExecutionResult(
            isSuccess = research.sources.isNotEmpty(),
            output = research.summary
        )
    }
}

class FileAgentTool(private val fileAgent: FileAgent) : AgentTool {
    override val name = "file_agent"
    override val description = "Safely reads, writes, and inspects files within Android sandbox."

    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val path = params["path"] ?: ""
        return when (action.lowercase()) {
            "list" -> {
                val files = fileAgent.listFiles(path)
                val summary = if (files.isEmpty()) {
                    "No files found in directory."
                } else {
                    files.joinToString("\n") { "${it.name} (${it.sizeBytes} B, dir=${it.isDirectory})" }
                }
                ToolExecutionResult(true, summary)
            }
            "read" -> {
                if (path.isBlank()) return ToolExecutionResult(false, "Missing 'path' parameter.")
                val res = fileAgent.readFile(path)
                ToolExecutionResult(res.isSuccess, res.data ?: res.message)
            }
            "write" -> {
                if (path.isBlank()) return ToolExecutionResult(false, "Missing 'path' parameter.")
                val content = params["content"] ?: ""
                val res = fileAgent.writeFile(path, content)
                ToolExecutionResult(res.isSuccess, res.message)
            }
            "delete" -> {
                if (path.isBlank()) return ToolExecutionResult(false, "Missing 'path' parameter.")
                val res = fileAgent.deleteFile(path)
                ToolExecutionResult(res.isSuccess, res.message)
            }
            else -> ToolExecutionResult(false, "Unknown file action: $action")
        }
    }
}

class TermuxTool(private val termuxBridge: TermuxBridge) : AgentTool {
    override val name = "termux"
    override val description = "Executes validated CLI commands in Termux with risk policy enforcement."

    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val cmd = params["command"] ?: return ToolExecutionResult(false, "Missing 'command' parameter.")
        val confirmed = params["confirmed"]?.toBoolean() ?: false
        val res = termuxBridge.executeCommand(cmd, userConfirmed = confirmed)
        return ToolExecutionResult(res.isSuccess, res.message)
    }
}

class SystemHealthTool(
    private val healthMonitor: SystemHealthMonitor,
    private val permissionIntelligence: PermissionIntelligence
) : AgentTool {
    override val name = "system_health"
    override val description = "Monitors device battery, memory, storage, and audits permissions."

    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        return when (action.lowercase()) {
            "health" -> {
                val snapshot = healthMonitor.getHealthSnapshot()
                val summary = "Battery: ${snapshot.batteryPct}% (Charging: ${snapshot.isCharging}), RAM: ${snapshot.availableRamMb}MB/${snapshot.totalRamMb}MB, Free Storage: ${snapshot.freeStorageGb}GB, Network: ${snapshot.networkType}, Accessibility: ${if (snapshot.isAccessibilityActive) "Active" else "Inactive"}"
                ToolExecutionResult(true, summary)
            }
            "permissions" -> {
                val report = permissionIntelligence.auditPermissions()
                val details = report.items.joinToString("\n") {
                    "${it.title}: ${if (it.isGranted) "GRANTED" else "MISSING"}${if (it.isEssential) " (Essential)" else ""}"
                }
                ToolExecutionResult(true, "Permission Audit (Missing Essential: ${report.missingEssentialCount}):\n$details")
            }
            else -> ToolExecutionResult(false, "Unknown health action: $action (valid: health, permissions)")
        }
    }
}
