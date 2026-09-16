package com.personal.ai.shivai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.ai.shivai.core.agent.StepStatus
import com.personal.ai.shivai.ui.AgentViewModel

@Composable
fun LiveConsoleScreen(viewModel: AgentViewModel) {
    val logs by viewModel.executionLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Shiv AI Console & Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextButton(onClick = { viewModel.taskExecutor.clearLogs() }) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs.reversed()) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${log.timestamp} • ${log.tool}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            val statusColor = when (log.status) {
                                StepStatus.VERIFIED_SUCCESS -> Color(0xFF4CAF50)
                                StepStatus.FAILED -> Color(0xFFF44336)
                                StepStatus.RUNNING -> Color(0xFFFFA000)
                                else -> Color.Gray
                            }
                            Text(log.status.name, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Action: ${log.action} (${log.inputSummary})", fontSize = 12.sp)
                        Text("Result: ${log.result}", fontSize = 12.sp)
                        if (log.verification.isNotBlank()) {
                            Text("Evidence: ${log.verification}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
