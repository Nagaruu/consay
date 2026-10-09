package com.consay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = TextPrimary,
    primaryContainer = PurpleSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = PurplePrimaryVariant,
    onSecondary = TextPrimary,
    background = PurpleBackground,
    onBackground = TextPrimary,
    surface = PurpleSurface,
    onSurface = TextPrimary,
    surfaceVariant = PurpleSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = PurpleAccent,
    onError = TextPrimary
)

@Composable
fun ConSayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
