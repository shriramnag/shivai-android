package com.personal.ai.shivai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ShivAI Modern Dark Theme
val NavyDeep      = Color(0xFF0A0E28)
val NavySurface   = Color(0xFF131830)
val NavyCard      = Color(0xFF1A2040)
val VioletPrimary = Color(0xFF7C4DFF)
val VioletLight   = Color(0xFF9E6FFF)
val GoldAccent    = Color(0xFFFFD700)
val TealSecondary = Color(0xFF00E5CC)
val ErrorRed      = Color(0xFFFF4757)
val TextPrimary   = Color(0xFFF0F2FF)
val TextSecondary = Color(0xFF8A92B2)

private val ShivAIDarkScheme = darkColorScheme(
    primary          = VioletPrimary,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFF3D1F99),
    secondary        = TealSecondary,
    onSecondary      = NavyDeep,
    background       = NavyDeep,
    onBackground     = TextPrimary,
    surface          = NavySurface,
    onSurface        = TextPrimary,
    surfaceVariant   = NavyCard,
    onSurfaceVariant = TextSecondary,
    error            = ErrorRed,
    onError          = Color.White,
    outline          = Color(0xFF2D3560),
)

@Composable
fun ShivAiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShivAIDarkScheme,
        content = content
    )
}
