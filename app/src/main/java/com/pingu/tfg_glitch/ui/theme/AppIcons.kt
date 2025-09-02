package com.pingu.tfg_glitch.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.pingu.tfg_glitch.R // ¡Importante! Asegúrate de que esta línea se añade para acceder a tus recursos.

/**
 * Objeto central para gestionar los iconos personalizados de la aplicación.
 */
object AppIcons {

    // --- Iconos de Cultivos ---
    // Cuando importes tus SVG, reemplaza los painterResource por los correctos.
    // Ejemplo: val Zanahoria @Composable get() = painterResource(id = R.drawable.ic_cultivo_zanahoria)

    // --- Iconos de Dados ---
    // Ejemplo: val DadoEnergia @Composable get() = painterResource(id = R.drawable.ic_dado_energia)

    // --- Iconos de Recursos ---
    val Moneda: ImageVector = Icons.Default.Paid
    val Energia: ImageVector = Icons.Default.Star // Placeholder, podrías usar Bolt
}

/**
 * Función auxiliar para obtener el icono de un cultivo a partir de su ID.
 */
@Composable
fun getIconForCrop(cropId: String): ImageVector {
    // TODO: Reemplazar con los iconos reales una vez importados.
    return when (cropId) {
        "zanahoria" -> Icons.Default.Help // painterResource(id = R.drawable.ic_zanahoria)
        "trigo" -> Icons.Default.Help // painterResource(id = R.drawable.ic_trigo)
        "patata" -> Icons.Default.Help // painterResource(id = R.drawable.ic_patata)
        "tomateCubico" -> Icons.Default.Help // ... y así sucesivamente
        "maizArcoiris" -> Icons.Default.Help
        "brocoliCristal" -> Icons.Default.Help
        "pimientoExplosivo" -> Icons.Default.Help
        else -> Icons.Default.Help
    }
}

/**
 * Función auxiliar para obtener el icono de un símbolo de dado.
 */
@Composable
fun getIconForDice(symbolName: String): ImageVector {
    // TODO: Reemplazar con los iconos reales.
    return Icons.Default.Help
}
