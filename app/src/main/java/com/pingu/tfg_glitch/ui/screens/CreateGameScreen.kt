package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy


// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    onGameCreated: (String, String) -> Unit // Pasa el ID de la partida y el ID del jugador anfitrión
) {
    // Estado para el nombre del anfitrión
    var hostNameInput by remember { mutableStateOf("") }
    // Estado para el código de partida generado
    var generatedGameCode by remember { mutableStateOf<String?>(null) }
    // Estado para el ID del jugador anfitrión generado
    var generatedHostPlayerId by remember { mutableStateOf<String?>(null) }
    // Estado para controlar si se debe mostrar la pantalla del código de partida
    var showGameCodeDisplay by remember { mutableStateOf(false) }
    // Estado para controlar la carga
    var isLoading by remember { mutableStateOf(false) }
    // Estado para mostrar un mensaje de error
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Scope para lanzar corrutinas
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding de Scaffold
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Crear Nueva Partida",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = AccentYellow,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!showGameCodeDisplay) {
                // Campo de texto para el nombre del anfitrión
                OutlinedTextField(
                    value = hostNameInput,
                    onValueChange = { hostNameInput = it },
                    label = { Text("Tu Nombre (Anfitrión)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
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
                        // Iniciar la corrutina para crear la partida
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                val (newGameId, hostPlayerId) = gameService.createGame(hostNameInput)
                                generatedGameCode = newGameId
                                generatedHostPlayerId = hostPlayerId
                                showGameCodeDisplay = true // Muestra la pantalla del código
                            } catch (e: Exception) {
                                errorMessage = "Error al crear la partida: ${e.message}"
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
                    enabled = !isLoading && hostNameInput.isNotBlank() // Habilitar solo si el nombre no está vacío
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Crear Partida", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            } else {
                // Pantalla para mostrar el código de partida
                generatedGameCode?.let { code ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Código de Partida:",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextLight
                            )
                            Text(
                                text = code,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentYellow,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Comparte este código con tus amigos",
                                fontSize = 14.sp,
                                color = TextLight.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = ClipData.newPlainText("Game Code", code)
                                    clipboardManager.setPrimaryClip(clipData)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Código copiado al portapapeles",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar Código", tint = TextWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copiar Código", color = TextWhite)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            // Navega a la pantalla del juego con el ID de la partida y el ID del jugador anfitrión
                            generatedGameCode?.let { gameId ->
                                generatedHostPlayerId?.let { playerId ->
                                    onGameCreated(gameId, playerId)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(32.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(text = "Continuar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateGameScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CreateGameScreen(onGameCreated = { _, _ -> })
        }
    }
}
