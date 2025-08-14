package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.ui.theme.*
import kotlinx.coroutines.launch

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

@Composable
fun JoinGameScreen(
    onGameJoined: (String, String) -> Unit // Ahora también pasa el ID del jugador
) {
    var gameCodeInput by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("") }
    var selectedFarmer by remember { mutableStateOf<String?>(null) } // NUEVO: Estado para el granjero
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val farmers = listOf("Ingeniero Glitch", "Botánica Mutante", "Comerciante Sombrío", "Visionaria Píxel")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unirse a Partida",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = AccentYellow,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = gameCodeInput,
            onValueChange = { gameCodeInput = it.uppercase() },
            label = { Text("Código de Partida") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = TextLight,
                focusedLabelColor = AccentPurple,
                unfocusedLabelColor = TextLight,
                cursorColor = AccentPurple
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = playerNameInput,
            onValueChange = { playerNameInput = it },
            label = { Text("Tu Nombre") },
            modifier = Modifier.fillMaxWidth(),
            isError = playerNameInput.isBlank() && !isLoading,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = TextLight,
                focusedLabelColor = AccentPurple,
                unfocusedLabelColor = TextLight,
                cursorColor = AccentPurple
            )
        )
        Spacer(modifier = Modifier.height(24.dp))

        // NUEVO: Selector de Granjero
        Text("Elige tu Granjero", color = TextLight, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            farmers.forEach { farmer ->
                val isSelected = selectedFarmer == farmer
                Button(
                    onClick = { selectedFarmer = farmer },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AccentPurple else DarkCard
                    ),
                    border = if (isSelected) BorderStroke(2.dp, AccentYellow) else null
                ) {
                    Text(farmer.split(" ")[0], fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        }


        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = GlitchRed,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        val playerId = gameService.addPlayerToGameByName(gameCodeInput, playerNameInput, selectedFarmer!!)
                        onGameJoined(gameCodeInput, playerId)
                    } catch (e: Exception) {
                        errorMessage = "Error al unirse: ${e.message}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(16.dp),
            enabled = !isLoading && gameCodeInput.length == 6 && playerNameInput.isNotBlank() && selectedFarmer != null
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextWhite,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = "Unirse a Partida", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JoinGameScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            JoinGameScreen(onGameJoined = { _, _ -> })
        }
    }
}
