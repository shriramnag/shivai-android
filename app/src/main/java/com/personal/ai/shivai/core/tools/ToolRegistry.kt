package com.personal.ai.shivai.core.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.personal.ai.shivai.core.automation.AppManager
import com.personal.ai.shivai.core.automation.ShivAccessibilityService
import kotlinx.coroutines.delay

data class ToolExecutionResult(
    val isSuccess: Boolean,
    val output: String
)

interface AgentTool {
    val name: String
    val description: String
    suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult
}

class ToolRegistry {
    private val tools = mutableMapOf<String, AgentTool>()

    fun register(tool: AgentTool) {
        tools[tool.name] = tool
    }

    fun getTool(name: String): AgentTool? = tools[name]
    fun listAll(): List<AgentTool> = tools.values.toList()
}

class AppLauncherTool(private val appManager: AppManager) : AgentTool {
    override val name = "app_launcher"
    override val description = "Launches installed Android applications."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val target = params["target"] ?: return ToolExecutionResult(false, "Missing 'target' app name.")
        val (ok, msg) = appManager.launchAppByName(target)
        return ToolExecutionResult(ok, msg)
    }
}

class AccessibilityClickTool : AgentTool {
    override val name = "accessibility_click"
    override val description = "Clicks an accessible element on the screen."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val text = params["text"] ?: return ToolExecutionResult(false, "Missing 'text' parameter.")
        val exact = params["exact"]?.toBoolean() ?: false
        val service = ShivAccessibilityService.instance
            ?: return ToolExecutionResult(false, "Accessibility Service is not enabled.")
        val clicked = service.clickElementByText(text, exact)
        return if (clicked) {
            ToolExecutionResult(true, "Clicked element matching '$text'.")
        } else {
            ToolExecutionResult(false, "Could not find clickable node for '$text'.")
        }
    }
}

class AccessibilityInputTool : AgentTool {
    override val name = "accessibility_input"
    override val description = "Inputs text into an active editable text field."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val text = params["text"] ?: return ToolExecutionResult(false, "Missing 'text' to input.")
        val index = params["index"]?.toIntOrNull() ?: 0
        val service = ShivAccessibilityService.instance
            ?: return ToolExecutionResult(false, "Accessibility Service is not enabled.")
        val success = service.setTextInput(text, index)
        return ToolExecutionResult(success, if (success) "Typed text successfully" else "Editable field not found")
    }
}

class GlobalNavigationTool(private val appManager: AppManager) : AgentTool {
    override val name = "global_navigation"
    override val description = "Performs safe Android system navigation: back, home, recents."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val service = ShivAccessibilityService.instance
            ?: return ToolExecutionResult(false, "Accessibility Service is not active.")
        val ok = when (action.lowercase()) {
            "back" -> service.performBack()
            "home" -> service.performHome()
            "recents" -> service.performRecents()
            "close" -> {
                appManager.safeExitForegroundApp()
                true
            }
            else -> false
        }
        return ToolExecutionResult(ok, "System navigation executed: $action")
    }
}

class DelayTool : AgentTool {
    override val name = "delay"
    override val description = "Delays execution for UI settlement."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val ms = params["millis"]?.toLongOrNull() ?: 1000L
        delay(ms)
        return ToolExecutionResult(true, "Waited ${ms}ms")
    }
}

class WebSearchTool(private val context: Context) : AgentTool {
    override val name = "web_search"
    override val description = "Launches web search via Android browser Intent."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val query = params["query"] ?: return ToolExecutionResult(false, "Missing search query.")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ToolExecutionResult(true, "Web search dispatched for: $query")
    }
}

class ClipboardTool(private val context: Context) : AgentTool {
    override val name = "clipboard"
    override val description = "Reads or writes system clipboard data with user awareness."
    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return if (action == "copy") {
            val content = params["text"] ?: ""
            cm.setPrimaryClip(ClipData.newPlainText("ShivAI", content))
            ToolExecutionResult(true, "Copied to clipboard.")
        } else {
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            ToolExecutionResult(true, "Clipboard: $text")
        }
    }
}
