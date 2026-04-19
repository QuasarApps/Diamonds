package com.example.diamonds.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Slightly darker surface used for top & bottom bars to create framing. */
val BarBackground = Color(0xFF1B2A4A)

private val LightColorScheme = lightColorScheme(
    surfaceContainer = BarBackground
)

@Composable
fun DiamondsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
