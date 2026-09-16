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
import com.personal.ai.shivai.core.security.SecurityRiskLevel
import com.personal.ai.shivai.core.security.SecurityThreat
import com.personal.ai.shivai.core.security.QuarantinedFile
import com.personal.ai.shivai.ui.AgentViewModel

@Composable
fun SecurityDashboardScreen(viewModel: AgentViewModel) {
    val overallRisk by viewModel.overallSecurityRisk.collectAsState()
    val threats by viewModel.activeThreats.collectAsState()
    val quarantined by viewModel.quarantinedFiles.collectAsState()

    var manualUrlInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        val statusColor = when (overallRisk) {
            SecurityRiskLevel.SAFE -> Color(0xFF4CAF50)
            SecurityRiskLevel.LOW -> Color(0xFF8BC34A)
            SecurityRiskLevel.MEDIUM -> Color(0xFFFFA000)
            SecurityRiskLevel.HIGH -> Color(0xFFFF5722)
            SecurityRiskLevel.CRITICAL -> Color(0xFFD32F2F)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Shiv AI Cyber Security Shield",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Overall Threat Level: ${overallRisk.name}",
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = manualUrlInput,
            onValueChange = { manualUrlInput = it },
            label = { Text("Enter URL to Scan for Phishing / Threat") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (manualUrlInput.isNotBlank()) {
                            viewModel.scanUrlManually(manualUrlInput)
                            manualUrlInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Security, contentDescription = "Scan URL")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Active Security Alerts (${threats.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary
        )

        if (threats.isEmpty()) {
            Text(
                text = "No active threats detected. All systems protected.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(threats) { threat ->
                    ThreatCard(threat = threat, onDismiss = { viewModel.dismissThreat(threat.id) })
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Quarantined Files (${quarantined.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.secondary
        )

        if (quarantined.isEmpty()) {
            Text(
                text = "No quarantined files.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(quarantined) { item ->
                    QuarantineCard(
                        item = item,
                        onRestore = { viewModel.restoreQuarantine(item.id) },
                        onDelete = { viewModel.deleteQuarantine(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatCard(threat: SecurityThreat, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🚨 ${threat.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = threat.risk.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Evidence: ${threat.evidence}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Action: ${threat.recommendedAction}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun QuarantineCard(
    item: QuarantinedFile,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "Reason: ${item.reason}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "SHA256: ${item.sha256.take(16)}...",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onRestore) {
                    Text("Restore", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontSize = 11.sp)
                }
            }
        }
    }
}
