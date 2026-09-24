package com.personal.ai.shivai.core.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallEvent(
    val state: CallState,
    val callerNumber: String = "",
    val callerName: String = ""
)

enum class CallState {
    IDLE, RINGING, ACTIVE, ENDED
}

class CallStateMonitor(private val context: Context) {

    private val _callEvent = MutableStateFlow(CallEvent(CallState.IDLE))
    val callEvent: StateFlow<CallEvent> = _callEvent.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            val callerName = if (number.isNotBlank()) resolveContactName(number) else ""

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    _callEvent.value = CallEvent(CallState.RINGING, number, callerName)
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    _callEvent.value = CallEvent(CallState.ACTIVE, number, callerName)
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    _callEvent.value = CallEvent(CallState.IDLE)
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    fun unregister() {
        try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
    }

    private fun resolveContactName(number: String): String {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(number)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) it.getString(0) else ""
            } ?: ""
        } catch (_: Exception) { "" }
    }

    fun buildVoiceAlert(event: CallEvent): String {
        return when (event.state) {
            CallState.RINGING -> {
                val who = if (event.callerName.isNotBlank()) event.callerName
                          else if (event.callerNumber.isNotBlank()) event.callerNumber
                          else "अज्ञात नंबर"
                "$who का call आ रहा है। उठाना है या decline करना है?"
            }
            CallState.ACTIVE  -> "Call जुड़ गया।"
            CallState.ENDED   -> "Call समाप्त हो गई।"
            CallState.IDLE    -> ""
        }
    }
}
