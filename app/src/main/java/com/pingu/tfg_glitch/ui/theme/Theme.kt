package com.pingu.tfg_glitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentGreen,
    tertiary = AccentYellow,
    background = DarkBackground,
    surface = DarkCard,
    onSurface = TextLight,
    error = GlitchRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPurple,
    onPrimary = Color.White,
    secondary = AccentGreen,
    tertiary = AccentYellow,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    error = GlitchRed,
    onError = Color.White
)

@Composable
fun GranjaGlitchAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
