package com.personal.ai.shivai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ai.shivai.core.memory.ChatMessageEntity
import com.personal.ai.shivai.core.voice.DialogSessionState
import com.personal.ai.shivai.core.voice.VoiceState
import com.personal.ai.shivai.ui.AgentViewModel

@Composable
fun AgentHomeScreen(viewModel: AgentViewModel) {
    val currentApp by viewModel.currentPackage.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val agentStatus by viewModel.agentStatus.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val messages by viewModel.conversationMessages.collectAsState(initial = emptyList())
    val confirmationPrompt by viewModel.confirmationPrompt.collectAsState()

    // Phase 4: Connectivity & Dialog States
    val isOnline by viewModel.isOnline.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val isPrivacyActive by viewModel.isPrivacyModeActive.collectAsState()

    var textInput by remember { mutableStateOf("") }

    if (confirmationPrompt != null) {
        AlertDialog(
            onDismissRequest = { viewModel.confirmAction(false) },
            title = { Text("Safety Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text(confirmationPrompt ?: "") },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAction(true) }
                ) { Text("CONFIRM") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.confirmAction(false) }) { Text("CANCEL") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Status Bar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isAccessibilityActive) Color(0xFF4CAF50) else Color(0xFFF44336))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Active App: $currentApp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Online / Offline Indicator Badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = if (isOnline) "🟢 $networkType" else "⚡ Offline Brain",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            labelColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Status: $agentStatus",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Voice Dialog: ${dialogState.name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (isPrivacyActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3E2723)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Shield",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Privacy Lockdown: Banking/Payment screen masked.",
                        color = Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.triggerEmergencyStop() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
            Spacer(modifier = Modifier.width(8.dp))
            Text("EMERGENCY STOP", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        // Voice & Input Controller Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isListening = voiceState is VoiceState.Listening || dialogState == DialogSessionState.LISTENING
            val isSpeaking = voiceState is VoiceState.Speaking || dialogState == DialogSessionState.SPEAKING

            FloatingActionButton(
                onClick = { viewModel.toggleVoice() },
                containerColor = when {
                    isSpeaking -> Color(0xFF9C27B0)
                    isListening -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.Default.VolumeUp
                        isListening -> Icons.Default.MicOff
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Shiv AI से कहें (Hindi/English)...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.submitGoal(textInput)
                                textInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Submit")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}
