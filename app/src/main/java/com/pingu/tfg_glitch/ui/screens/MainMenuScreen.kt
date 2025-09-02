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
import com.pingu.tfg_glitch.ui.theme.AccentGold
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme

// Composable para la pantalla principal del menú
@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onViewRules: () -> Unit,
    onOneMobileMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Espaciador flexible para centrar el contenido verticalmente
        Spacer(Modifier.weight(1f))

        // Título de la aplicación
        Text(
            text = "Granja Glitch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = AccentGold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 64.dp)
        )

        // Contenedor para los botones de acción
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Botón para el modo multijugador
            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Partida Multijugador",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón para el modo un móvil
            Button(
                onClick = onOneMobileMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Modo Un Móvil",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botón para ver las reglas (estilo secundario)
            OutlinedButton(
                onClick = onViewRules,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Ver Reglas",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Espaciador flexible para centrar
        Spacer(Modifier.weight(1f))
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
