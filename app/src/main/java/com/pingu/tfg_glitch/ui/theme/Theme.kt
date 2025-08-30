package com.pingu.tfg_glitch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de colores oscura, basada en el diseño original
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

// Paleta de colores más clara, para un posible modo claro futuro
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

// Paleta "Pixel Glitch" más vibrante y con un estilo retro
private val PixelGlitchColorScheme = darkColorScheme(
    primary = GlitchPurple,
    onPrimary = Color.White,
    secondary = GlitchCyan,
    onSecondary = Color.Black,
    tertiary = GlitchMagenta,
    background = PixelBackground,
    surface = PixelCard,
    onSurface = GlitchText,
    onSurfaceVariant = GlitchText,
    surfaceVariant = GlitchDarkCard,
    error = GlitchRed,
    onError = Color.Black
)

// NUEVO: Paleta "TierraGlitch" con tonos neutros y un toque de lila
private val TierraGlitchColorScheme = darkColorScheme(
    primary = GlitchLila,
    onPrimary = Color.White,
    secondary = GlitchBeige,
    onSecondary = Color.Black,
    tertiary = GlitchMarronClaro,
    background = GlitchMarronOscuro,
    surface = GlitchGrisOscuro,
    onSurface = GlitchBeige,
    onSurfaceVariant = GlitchGrisClaro,
    surfaceVariant = GlitchMarronOscuro.copy(alpha = 0.8f),
    error = GlitchRed,
    onError = Color.Black
)

@Composable
fun GranjaGlitchAppTheme(
    // Por defecto, usa el tema oscuro, pero puedes cambiarlo aquí
    darkTheme: Boolean = true,
    // Puedes cambiar entre los temas disponibles
    // Ejemplo: `selectedTheme = "PixelGlitch"` para usar el nuevo tema
    selectedTheme: String = "Dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        "Dark" -> DarkColorScheme
        "Light" -> LightColorScheme
        "PixelGlitch" -> PixelGlitchColorScheme
        "TierraGlitch" -> TierraGlitchColorScheme // NUEVO
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
