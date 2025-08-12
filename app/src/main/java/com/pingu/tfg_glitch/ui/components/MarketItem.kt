package com.pingu.tfg_glitch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape // ¡IMPORTACIÓN AÑADIDA!
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.ui.theme.AccentGreen
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import java.util.Locale

// Función auxiliar para capitalizar la primera letra de cada palabra
fun String.capitalizeWords(): String = this.split(" ").joinToString(" ") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

// Composable para cada elemento del mercado (ahora compartido)
@Composable
fun MarketItem(cropName: String, price: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A5568)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp) // Padding reducido
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cropName.capitalizeWords(),
                fontSize = 16.sp, // Fuente más pequeña
                fontWeight = FontWeight.Medium,
                color = AccentGreen
            )
            Text(
                text = "$price 💰",
                fontSize = 20.sp, // Fuente más pequeña
                fontWeight = FontWeight.Bold,
                color = AccentYellow
            )
        }
    }
}
