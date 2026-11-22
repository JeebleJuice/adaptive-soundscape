package dev.jeeblejuice.soundscape.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = darkColorScheme(
    primary = Color(0xFFC3E7CE),
    onPrimary = Color(0xFF123522),
    primaryContainer = Color(0xFF294B38),
    onPrimaryContainer = Color(0xFFD9F8E2),
    secondary = Color(0xFFB8CCC1),
    onSecondary = Color(0xFF23352C),
    background = Color(0xFF07100F),
    onBackground = Color(0xFFE4ECE8),
    surface = Color(0xFF0E1A18),
    onSurface = Color(0xFFE4ECE8),
    surfaceVariant = Color(0xFF1B2925),
    onSurfaceVariant = Color(0xFFB9C9C1),
    outline = Color(0xFF71857B),
)

@Composable
fun SoundscapeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
