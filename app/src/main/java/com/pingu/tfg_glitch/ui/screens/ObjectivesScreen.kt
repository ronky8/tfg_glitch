package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Objective
import com.pingu.tfg_glitch.ui.theme.AccentGreen
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.GlitchRed
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextLight
import com.pingu.tfg_glitch.ui.theme.TextWhite
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope // Importación necesaria
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // Icono para objetivo reclamado
import androidx.compose.material.icons.filled.Star // Icono para PV
// REMOVIDO: import androidx.compose.runtime.mutableStateOf
// REMOVIDO: import androidx.compose.runtime.setValue
// REMOVIDO: import androidx.compose.ui.graphics.Color
// REMOVIDO: import androidx.compose.ui.text.input.KeyboardType
// REMOVIDO: import androidx.compose.foundation.text.KeyboardOptions
// REMOVIDO: import com.pingu.tfg_glitch.data.Player // Ya no es necesario importar Player aquí para ajuste manual


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectivesScreen(gameId: String, currentPlayerId: String) {
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    // REMOVIDO: val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // REMOVIDO: isHost ya no es necesario aquí para el ajuste manual de PV
    // val isHost = remember(game, currentPlayerId) { game?.hostPlayerId == currentPlayerId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Objetivos de Juego",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Información del jugador actual (simplificada para esta pantalla)
            currentPlayer?.let { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(text = "Jugador: ${player.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(text = "Monedas: ${player.money} 💰", fontSize = 18.sp, color = TextLight)
                            Text(text = "Energía Glitch: ${player.glitchEnergy} ⚡", fontSize = 18.sp, color = TextLight)
                        }
                    }
                }
            }

            // REMOVIDO: La sección de ajuste manual de PV se ha movido a PlayerManagementScreen
            /*
            if (isHost && allPlayers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ajustar PV de Jugadores (Solo Host)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // Lista desplegable para seleccionar jugador
                var expanded by remember { mutableStateOf(false) }
                var selectedPlayerForAdjustment by remember { mutableStateOf<Player?>(null) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedPlayerForAdjustment?.name ?: "Selecciona un jugador",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Jugador") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
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

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allPlayers.forEach { player ->
                            DropdownMenuItem(
                                text = { Text(player.name, color = TextLight) },
                                onClick = {
                                    selectedPlayerForAdjustment = player
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                selectedPlayerForAdjustment?.let { playerToAdjust ->
                    ManualPVAdjustmentSection(
                        playerId = playerToAdjust.id,
                        onAdjustPV = { targetPlayerId, pvDelta ->
                            coroutineScope.launch {
                                val success = gameService.adjustPlayerManualBonusPV(targetPlayerId, pvDelta)
                                if (success) {
                                    snackbarHostState.showSnackbar(
                                        message = "PV de ${playerToAdjust.name} ajustados.",
                                        duration = SnackbarDuration.Short
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "Error al ajustar PV de ${playerToAdjust.name}.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            */

            // Lista de objetivos
            game?.activeObjectives?.let { objectives ->
                if (objectives.isEmpty()) {
                    Text(text = "No hay objetivos activos en esta partida.", color = TextLight)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(objectives) { objective ->
                            val isClaimed = currentPlayer?.objectivesClaimed?.contains(objective.id) ?: false
                            val cardColor = if (isClaimed) AccentGreen.copy(alpha = 0.3f) else DarkCard
                            val textColor = if (isClaimed) AccentGreen else TextLight

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = objective.description,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = "Puntos de Victoria", tint = AccentYellow, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${objective.rewardPV} PV",
                                            fontSize = 16.sp,
                                            color = AccentYellow
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (isClaimed) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Reclamado", tint = AccentGreen, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("¡Objetivo Reclamado!", color = AccentGreen, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val result = gameService.claimObjective(gameId, currentPlayerId, objective.id)
                                                    snackbarHostState.showSnackbar(
                                                        message = result,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(12.dp)
                                        ) {
                                            Text("Reclamar Objetivo", color = TextWhite, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AccentPurple)
                Text(text = "Cargando objetivos...", color = TextLight, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// REMOVIDO: ManualPVAdjustmentSection se ha movido a PlayerManagementScreen
/*
@Composable
fun ManualPVAdjustmentSection(
    playerId: String,
    onAdjustPV: (String, Int) -> Unit
) {
    val pvAdjustmentInput = remember { mutableStateOf("0") }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(
            text = "PV a ajustar:",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentPurple
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = pvAdjustmentInput.value,
                onValueChange = { newValue ->
                    pvAdjustmentInput.value = newValue.filter { it.isDigit() || it == '-' }
                },
                label = { Text("Cantidad") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val delta = pvAdjustmentInput.value.toIntOrNull() ?: 0
                    if (delta != 0) {
                        onAdjustPV(playerId, delta)
                        pvAdjustmentInput.value = "0"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir PV", tint = TextWhite)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = {
                    val delta = pvAdjustmentInput.value.toIntOrNull() ?: 0
                    if (delta != 0) {
                        onAdjustPV(playerId, -delta) // Restar
                        pvAdjustmentInput.value = "0"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Restar PV", tint = TextWhite)
            }
        }
    }
}
*/

@Preview(showBackground = true)
@Composable
fun ObjectivesScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ObjectivesScreen(gameId = "sampleGame123", currentPlayerId = "samplePlayer456")
        }
    }
}
