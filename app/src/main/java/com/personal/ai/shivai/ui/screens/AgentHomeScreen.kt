package com.personal.ai.shivai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ai.shivai.core.memory.ChatMessageEntity
import com.personal.ai.shivai.core.voice.DialogSessionState
import com.personal.ai.shivai.core.voice.VoiceState
import com.personal.ai.shivai.ui.AgentViewModel
import com.personal.ai.shivai.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AgentHomeScreen(viewModel: AgentViewModel) {
    val currentApp      by viewModel.currentPackage.collectAsState()
    val isAccessibility by viewModel.isAccessibilityActive.collectAsState()
    val agentStatus     by viewModel.agentStatus.collectAsState()
    val voiceState      by viewModel.voiceState.collectAsState()
    val messages        by viewModel.conversationMessages.collectAsState(initial = emptyList())
    val confirmPrompt   by viewModel.confirmationPrompt.collectAsState()
    val isOnline        by viewModel.isOnline.collectAsState()
    val dialogState     by viewModel.dialogState.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyModeActive.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    if (confirmPrompt != null) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmAction(false) },
            containerColor = NavyCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Safety Check", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = { Text(confirmPrompt ?: "", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                Button(onClick = { viewModel.confirmAction(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)) {
                    Text("CONFIRM", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmAction(false) }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(NavyDeep)) {

        // Header
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A0A4A), NavySurface)))
            .padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(VioletLight, VioletPrimary))),
                    contentAlignment = Alignment.Center) {
                    Text("S", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shiv AI", fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextPrimary)
                    Text(if (isOnline) "● Online" else "⚡ Offline", fontSize = 11.sp,
                        color = if (isOnline) TealSecondary else GoldAccent, fontWeight = FontWeight.SemiBold)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (isAccessibility) Color(0x2200E5CC) else Color(0x22FF4757))
                    .padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(if (isAccessibility) "A11Y ✓" else "A11Y ✗", fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAccessibility) TealSecondary else ErrorRed)
                }
            }
        }

        // Status bar
        Row(modifier = Modifier.fillMaxWidth().background(NavySurface)
            .padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("App: ${currentApp.removePrefix("com.").take(30)}", fontSize = 11.sp,
                color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Text(agentStatus, fontSize = 11.sp, color = VioletLight, fontWeight = FontWeight.Medium)
        }

        // Privacy banner
        if (isPrivacyActive) {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2A1000))
                .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Privacy Shield Active", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Emergency Stop
        Button(onClick = { viewModel.triggerEmergencyStop() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF4757)),
            shape = RoundedCornerShape(0.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Stop, null, tint = ErrorRed, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("EMERGENCY STOP", color = ErrorRed, fontWeight = FontWeight.Black,
                fontSize = 13.sp, letterSpacing = 1.5.sp)
        }

        // Chat messages
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()
            .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp)) {
            items(messages) { msg -> ModernChatBubble(msg) }
        }

        // Input bar
        val isListening = voiceState is VoiceState.Listening || dialogState == DialogSessionState.LISTENING
        val isSpeaking  = voiceState is VoiceState.Speaking  || dialogState == DialogSessionState.SPEAKING

        Row(modifier = Modifier.fillMaxWidth().background(NavySurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(when {
                    isSpeaking  -> Brush.radialGradient(listOf(Color(0xFF9C27B0), VioletPrimary))
                    isListening -> Brush.radialGradient(listOf(ErrorRed, Color(0xFFB71C1C)))
                    else        -> Brush.radialGradient(listOf(VioletLight, VioletPrimary))
                }), contentAlignment = Alignment.Center) {
                IconButton(onClick = { viewModel.toggleVoice() }) {
                    Icon(when {
                        isSpeaking  -> Icons.Default.VolumeUp
                        isListening -> Icons.Default.MicOff
                        else        -> Icons.Default.Mic
                    }, "Voice", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = textInput, onValueChange = { textInput = it },
                placeholder = { Text("Shiv AI से कहें...", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier.weight(1f), singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VioletPrimary, unfocusedBorderColor = Color(0xFF2D3560),
                    focusedContainerColor = NavyCard, unfocusedContainerColor = NavyCard,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = VioletPrimary),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(onClick = { viewModel.submitGoal(textInput); textInput = "" }) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape)
                                .background(VioletPrimary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Send, "Send", tint = Color.White,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                })
        }
    }
}

@Composable
fun ModernChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom) {
        if (!isUser) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(VioletLight, VioletPrimary))),
                contentAlignment = Alignment.Center) {
                Text("S", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Spacer(Modifier.width(6.dp))
        }
        Box(modifier = Modifier.widthIn(max = 280.dp)
            .clip(RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp))
            .background(if (isUser)
                Brush.linearGradient(listOf(VioletPrimary, Color(0xFF5C2FCC)))
            else
                Brush.linearGradient(listOf(NavyCard, Color(0xFF1E2548))))
            .padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(message.content, color = if (isUser) Color.White else TextPrimary,
                fontSize = 14.sp, lineHeight = 20.sp)
        }
        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF2D3560)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
