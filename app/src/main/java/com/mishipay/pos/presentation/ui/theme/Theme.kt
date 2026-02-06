package com.mishipay.pos.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// MishiPay color palette (from designs)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val LightGray = Color(0xFFF5F5F5)
val MediumGray = Color(0xFF9E9E9E)
val DarkGray = Color(0xFF424242)
val PriceGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)

private val MishiPayColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = LightGray,
    onSecondary = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = LightGray,
    onSurfaceVariant = DarkGray,
    error = ErrorRed,
    onError = White
)

@Composable
fun MishiPayTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MishiPayColorScheme,
        content = content
    )
}
