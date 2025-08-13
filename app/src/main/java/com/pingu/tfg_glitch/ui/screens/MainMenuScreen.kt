package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextWhite

// Composable para la pantalla principal del menú
@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onViewRules: () -> Unit,
    onOneMobileMode: () -> Unit // Nuevo callback para el modo un móvil
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(0.5f))

        Text(
            text = "Granja Glitch",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = AccentYellow,
            textAlign = TextAlign.Center,
            lineHeight = 50.sp // Añadimos altura de línea para evitar cortes
        )

        Spacer(Modifier.weight(0.5f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = "Empezar Partida Multijugador",
                    fontSize = 18.sp, // Tamaño de fuente ligeramente reducido
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onOneMobileMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Modo Un Móvil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }

            Button(
                onClick = onViewRules,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Ver Reglas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
        Spacer(Modifier.weight(0.5f))
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainMenuScreen(onStartGame = {}, onViewRules = {}, onOneMobileMode = {})
        }
    }
}
