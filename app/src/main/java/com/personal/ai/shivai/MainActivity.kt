package com.personal.ai.shivai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.ai.shivai.ui.AgentViewModel
import com.personal.ai.shivai.ui.screens.AgentHomeScreen
import com.personal.ai.shivai.ui.screens.LiveConsoleScreen
import com.personal.ai.shivai.ui.theme.ShivAiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AgentViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            ShivAiTheme {
                val navController = rememberNavController()
                var selectedItem by remember { mutableStateOf(0) }
                val items = listOf("Agent", "Console")
                val icons = listOf(Icons.Default.SmartToy, Icons.Default.Terminal)

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, title ->
                                NavigationBarItem(
                                    icon = { Icon(icons[index], contentDescription = title) },
                                    label = { Text(title) },
                                    selected = selectedItem == index,
                                    onClick = {
                                        selectedItem = index
                                        navController.navigate(title.lowercase()) {
                                            popUpTo("agent") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "agent",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("agent") { AgentHomeScreen(viewModel) }
                        composable("console") { LiveConsoleScreen(viewModel) }
                    }
                }
            }
        }
    }
}
