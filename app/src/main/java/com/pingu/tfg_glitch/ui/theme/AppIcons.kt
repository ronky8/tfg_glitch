package com.pingu.tfg_glitch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.pingu.tfg_glitch.R // ¡Importante! Para acceder a tus recursos.
import com.pingu.tfg_glitch.data.DadoSimbolo

/**
 * Funciones para gestionar los iconos personalizados de la aplicación.
 * Apunta a los recursos que importes en la carpeta res/drawable.
 */

// Placeholder genérico por si un icono no se encuentra.
// Crea un PNG/SVG simple con un signo de interrogación y guárdalo como `ic_placeholder`
val placeholderIcon: Int = R.drawable.ic_placeholder

/**
 * Obtiene el icono correspondiente a un ID de cultivo.
 */
@Composable
fun getIconForCrop(cropId: String): Painter {
    val resourceId = when (cropId) {
        "zanahoria" -> R.drawable.ic_zanahoria
        "trigo" -> R.drawable.ic_trigo
        "patata" -> R.drawable.ic_patata
        "tomateCubico" -> R.drawable.ic_tomate
        "maizArcoiris" -> R.drawable.ic_maizarcoiris
        "brocoliCristal" -> R.drawable.ic_brocoli
        "pimientoExplosivo" -> R.drawable.ic_pimiento
        else -> placeholderIcon
    }
    return painterResource(id = resourceId)
}

/**
 * Obtiene el icono de fondo para los dados.
 */
@Composable
fun getDiceBackground(): Painter {
    // Asegúrate de tener un PNG/SVG llamado `ic_dado_fondo` en res/drawable
    return painterResource(id = R.drawable.ic_background_dado)
}

/**
 * Obtiene el icono correspondiente a un símbolo de dado.
 */
@Composable
fun getIconForDice(symbol: DadoSimbolo): Painter {
    val resourceId = when (symbol) {
        DadoSimbolo.GLITCH -> R.drawable.ic_cartas
        DadoSimbolo.CRECIMIENTO -> R.drawable.ic_crecimiento
        DadoSimbolo.ENERGIA -> R.drawable.ic_glitch
        DadoSimbolo.MONEDA -> R.drawable.ic_moneda
        DadoSimbolo.MISTERIO -> R.drawable.ic_misterio
        DadoSimbolo.PLANTAR -> R.drawable.ic_plantar
    }
    return painterResource(id = resourceId)
}

/**
 * Obtiene el icono correspondiente a un ID de granjero.
 */
@Composable
fun getIconForGranjero(granjeroId: String): Painter {
    val resourceId = when (granjeroId) {
        // TODO: Reemplazar con los nombres de tus PNG para los granjeros
        "ingeniero_glitch" -> placeholderIcon
        "botanica_mutante" -> placeholderIcon
        "comerciante_sombrio" -> placeholderIcon
        "visionaria_pixel" -> placeholderIcon
        else -> placeholderIcon
    }
    return painterResource(id = resourceId)
}

/**
 * Obtiene el icono para Moneda como Painter.
 */
@Composable
fun getIconForCoin(): Painter {
    // Apunta a tu PNG/SVG para la moneda
    return painterResource(id = R.drawable.ic_moneda_sin)
}

/**
 * Obtiene el icono para Energía como Painter.
 */
@Composable
fun getIconForEnergy(): Painter {
    // Apunta a tu PNG/SVG para la energía
    return painterResource(id = R.drawable.ic_glitch)
}

/**
 * [NUEVO] Obtiene el icono para los Puntos de Victoria (PV) como Painter.
 */
@Composable
fun getIconForPV(): Painter {
    return painterResource(id = R.drawable.ic_pv_placeholder)
}

