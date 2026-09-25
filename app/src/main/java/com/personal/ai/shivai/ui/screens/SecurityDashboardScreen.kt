package com.personal.ai.shivai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.personal.ai.shivai.ui.AgentViewModel

@Composable
fun SecurityDashboardScreen(viewModel: AgentViewModel) {
    val isOnline = viewModel.isOnline.collectAsState()
    val networkType = viewModel.networkType.collectAsState()
    val activeThreats = viewModel.activeThreats.collectAsState()
    val overallRisk = viewModel.overallSecurityRisk.collectAsState()
    val isPrivacyModeActive = viewModel.isPrivacyModeActive.collectAsState()
    val isAccessibilityActive = viewModel.isAccessibilityActive.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Security Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Network Status
        item {
            StatusCard(
                title = "Network Status",
                icon = if (isOnline.value) Icons.Default.CloudDone else Icons.Default.CloudOff,
                statusText = if (isOnline.value) "Online (${networkType.value})" else "Offline",
                statusColor = if (isOnline.value) Color.Green else Color.Red
            )
        }

        // Security Risk Level
        item {
            val riskColor = when (overallRisk.value?.name) {
                "CRITICAL" -> Color.Red
                "HIGH" -> Color(0xFFFF7043)
                "MEDIUM" -> Color(0xFFFFA726)
                "LOW" -> Color(0xFFAED581)
                else -> Color.Green
            }
            
            StatusCard(
                title = "Overall Security Risk",
                icon = Icons.Default.SecurityAlert,
                statusText = overallRisk.value?.name ?: "SAFE",
                statusColor = riskColor
            )
        }

        // Active Threats
        item {
            if (activeThreats.value.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🚨 Active Threats (${activeThreats.value.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        activeThreats.value.forEach { threat ->
                            Text(
                                "• ${threat.threatType}: ${threat.description}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                        Text("No active threats detected", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Privacy Mode Status
        item {
            StatusCard(
                title = "Privacy Mode",
                icon = if (isPrivacyModeActive.value) Icons.Default.Lock else Icons.Default.LockOpen,
                statusText = if (isPrivacyModeActive.value) "ACTIVE" else "Inactive",
                statusColor = if (isPrivacyModeActive.value) Color.Green else Color(0xFFBDBDBD)
            )
        }

        // Accessibility Status
        item {
            StatusCard(
                title = "Accessibility Service",
                icon = if (isAccessibilityActive.value) Icons.Default.HandymanTwoTone else Icons.Default.Close,
                statusText = if (isAccessibilityActive.value) "ACTIVE" else "Inactive",
                statusColor = if (isAccessibilityActive.value) Color.Green else Color(0xFFBDBDBD)
            )
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.togglePrivacyShield(!viewModel.isShieldEnabled.collectAsState().value) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Privacy Shield")
                }
                Button(
                    onClick = { viewModel.clearPrivacyLogs() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Logs")
                }
            }
        }

        // Emergency Stop Button
        item {
            Button(
                onClick = { viewModel.triggerEmergencyStop() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EMERGENCY STOP", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    icon: androidx.compose.material.icons.materialIcon,
    statusText: String,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(icon, contentDescription = null, tint = statusColor)
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            Text(
                statusText,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
