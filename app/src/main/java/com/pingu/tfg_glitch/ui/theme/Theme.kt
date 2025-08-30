package com.pingu.tfg_glitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nueva paleta de colores oscura basada en Material Design 3
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryText,
    secondary = SecondaryGreen,
    onSecondary = Color.Black,
    tertiary = InfoBlue,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceText,
    onSurfaceVariant = TextLight, // Un color de texto secundario más suave
    surfaceVariant = DarkCard, // Para tarjetas y elementos elevados
    error = ErrorRed,
    onError = Color.Black
)

// Nueva paleta de colores clara (puedes personalizarla si quieres un modo claro)
private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    secondary = SecondaryGreen,
    tertiary = InfoBlue,
    background = Color(0xFFF5F5F5), // Un gris muy claro
    surface = Color.White,
    onSurface = Color.Black,
    error = WarningRed,
    onError = Color.White
)

@Composable
fun GranjaGlitchAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Forzamos el tema oscuro por ahora, ya que el diseño original estaba pensado así.
    // Puedes cambiar `true` por `darkTheme` si quieres habilitar el modo claro.
    val colorScheme = if (true) {
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
