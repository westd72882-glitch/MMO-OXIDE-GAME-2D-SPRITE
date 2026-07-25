package com.example.wasteland

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF14171A)
val PanelDark = Color(0xFF1E2320)
val PanelDarker = Color(0xFF171A17)
val BorderMuted = Color(0xFF2C322C)
val AccentRust = Color(0xFFC96442)
val AccentGold = Color(0xFFD4A13B)
val TextPrimary = Color(0xFFE8E6DE)
val TextSecondary = Color(0xFF8A8F86)
val TextMuted = Color(0xFF6F7469)
val ResourceGreen = Color(0xFF8FAE6B)
val WarnRed = Color(0xFFA85C4C)

private val WastelandColors = darkColorScheme(
    background = BgDark,
    surface = PanelDark,
    primary = AccentRust,
    secondary = AccentGold,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun WastelandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WastelandColors,
        content = content
    )
}
