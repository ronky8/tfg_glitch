package com.pingu.tfg_glitch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.pingu.tfg_glitch.R
import com.pingu.tfg_glitch.data.DadoSimbolo


val placeholderIcon: Int = R.drawable.ic_placeholder

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

@Composable
fun getDiceBackground(): Painter {
    return painterResource(id = R.drawable.ic_background_dado)
}

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

@Composable
fun getIconForGranjero(granjeroId: String): Painter {
    val resourceId = when (granjeroId) {
        "ingeniero_glitch" -> R.drawable.ic_ingeniero
        "botanica_mutante" -> R.drawable.ic_botanica
        "comerciante_sombrio" -> R.drawable.ic_comerciante
        "visionaria_pixel" -> R.drawable.ic_visionaria
        else -> placeholderIcon
    }
    return painterResource(id = resourceId)
}

@Composable
fun getIconForCoin(): Painter {
    // Apunta a tu PNG/SVG para la moneda
    return painterResource(id = R.drawable.ic_moneda_sin)
}

@Composable
fun getIconForEnergy(): Painter {
    // Apunta a tu PNG/SVG para la energía
    return painterResource(id = R.drawable.ic_glitch)
}

@Composable
fun getIconForPV(): Painter {
    return painterResource(id = R.drawable.ic_pv_placeholder)
}

