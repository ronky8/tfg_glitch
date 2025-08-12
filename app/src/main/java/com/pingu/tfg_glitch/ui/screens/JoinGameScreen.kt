package com.pingu.tfg_glitch.ui.screens

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
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.GlitchRed
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextLight
import com.pingu.tfg_glitch.ui.theme.TextWhite
import kotlinx.coroutines.launch

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

@Composable
fun JoinGameScreen(
    onGameJoined: (String, String) -> Unit // Ahora también pasa el ID del jugador
) {
    // Estado para el código de partida introducido por el usuario
    var gameCodeInput by remember { mutableStateOf("") }
    // Estado para el nombre del jugador introducido por el usuario
    var playerNameInput by remember { mutableStateOf("") }
    // Estado para controlar la carga
    var isLoading by remember { mutableStateOf(false) }
    // Estado para mostrar un mensaje de error
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Scope para lanzar corrutinas
    val coroutineScope = rememberCoroutineScope()

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
        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para el código de partida
        OutlinedTextField(
            value = gameCodeInput,
            onValueChange = { gameCodeInput = it.uppercase() },
            label = { Text("Código de Partida") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), // Teclado para códigos alfanuméricos
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

        // Campo de texto para el nombre del jugador
        OutlinedTextField(
            value = playerNameInput,
            onValueChange = { playerNameInput = it },
            label = { Text("Tu Nombre") },
            modifier = Modifier.fillMaxWidth(),
            isError = playerNameInput.isBlank() && !isLoading, // Validación visual para nombre vacío
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Teclado de texto normal
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

        // Mostrar un mensaje de error si existe
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = GlitchRed,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Iniciar la corrutina para unirse a la partida
                coroutineScope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        // Llama a la nueva función en GameService que añade el jugador por nombre
                        val playerId = gameService.addPlayerToGameByName(gameCodeInput, playerNameInput)
                        onGameJoined(gameCodeInput, playerId) // Pasa gameId y playerId
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
            enabled = !isLoading && gameCodeInput.length == 6 && playerNameInput.isNotBlank() // Habilitar solo si el código y nombre son válidos
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
