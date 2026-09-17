package com.personal.ai.shivai.core.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings

class DeviceControlTool(private val context: Context) : AgentTool {
    override val name = "device_control"
    override val description = "Controls on-device hardware: torch/flashlight, volume, timer, and system settings."

    override suspend fun execute(action: String, params: Map<String, String>): ToolExecutionResult {
        return try {
            when (action.lowercase()) {
                "torch_on", "flashlight_on" -> {
                    val ok = setTorch(true)
                    ToolExecutionResult(ok, if (ok) "Torch turned ON" else "Torch unavailable")
                }
                "torch_off", "flashlight_off" -> {
                    val ok = setTorch(false)
                    ToolExecutionResult(ok, if (ok) "Torch turned OFF" else "Torch unavailable")
                }
                "volume_up" -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ToolExecutionResult(true, "Volume increased")
                }
                "volume_down" -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    ToolExecutionResult(true, "Volume decreased")
                }
                "mute" -> {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    ToolExecutionResult(true, "Volume muted")
                }
                "open_settings" -> {
                    val target = params["target"]?.lowercase() ?: "general"
                    val intent = when (target) {
                        "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                        "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        "display", "brightness" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                        "sound", "volume" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                        else -> Intent(Settings.ACTION_SETTINGS)
                    }.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(true, "Opened $target settings")
                }
                "set_timer" -> {
                    val seconds = params["seconds"]?.toIntOrNull() ?: 300
                    val message = params["message"] ?: "Shiv AI Timer"
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(true, "Set timer for $seconds seconds ($message)")
                }
                "dial_contact", "dial_number" -> {
                    val numberOrName = params["target"] ?: ""
                    val uri = if (numberOrName.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }) {
                        Uri.parse("tel:${numberOrName.trim()}")
                    } else {
                        Uri.parse("tel:")
                    }
                    val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    ToolExecutionResult(true, "Dialer opened for $numberOrName")
                }
                else -> ToolExecutionResult(false, "Unknown device control action: $action")
            }
        } catch (e: Exception) {
            ToolExecutionResult(false, "Device control failed: ${e.message}")
        }
    }

    private fun setTorch(enabled: Boolean): Boolean {
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facingBack = chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
                if (flashAvailable && facingBack) {
                    cm.setTorchMode(id, enabled)
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}
