package com.roozyar.app

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

val ACCENTS = listOf(Color(0xFF0F766E), Color(0xFFD97706), Color(0xFF6D28D9), Color(0xFFBE123C))

@Composable
fun RoozTheme(dark: Boolean, accent: Int, content: @Composable () -> Unit) {
    val p = ACCENTS[accent.coerceIn(0, ACCENTS.lastIndex)]
    val scheme = if (dark) darkColorScheme(
        primary = lerp(p, Color.White, 0.4f), onPrimary = Color(0xFF111111),
        background = Color(0xFF121413), surface = Color(0xFF1B1E1D),
        surfaceVariant = Color(0xFF262A29), secondaryContainer = lerp(p, Color.Black, 0.5f)
    ) else lightColorScheme(
        primary = p, onPrimary = Color.White,
        background = Color(0xFFFAF9F6), surface = Color.White,
        surfaceVariant = Color(0xFFF0EEE8), secondaryContainer = lerp(p, Color.White, 0.8f)
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
