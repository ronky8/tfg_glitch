package com.pingu.tfg_glitch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Configuración de la tipografía para el tema de la app.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace, // Usamos Monospace para simular una fuente pixel
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold, // Títulos en negrita
        fontSize = 24.sp, // Ligeramente más grande
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, // Un poco más grande para legibilidad
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
