package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Player
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextLight
import com.pingu.tfg_glitch.ui.theme.TextWhite
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.pingu.tfg_glitch.data.allObjectives // Importar la lista de objetivos globales
import kotlin.math.roundToInt // Para redondear la división de PV
import androidx.compose.runtime.LaunchedEffect // ¡IMPORTACIÓN AÑADIDA!
import kotlinx.coroutines.flow.firstOrNull // Importar firstOrNull para LaunchedEffect
import com.pingu.tfg_glitch.ui.theme.AccentGreen // Importación de color
import com.pingu.tfg_glitch.ui.theme.GlitchRed // Importación de color


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalScoreScreen(
    gameId: String, // Recibe el ID de la partida para cargar los datos finales
    onBackToMainMenu: () -> Unit
) {
    // Observa el estado de la partida y los jugadores para mostrar la puntuación
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // Determina si el jugador actual es el anfitrión (para el ajuste manual de PV)
    val currentPlayerId = remember { mutableStateOf<String?>(null) }
    val isHost = remember(game, currentPlayerId.value) {
        game?.hostPlayerId == currentPlayerId.value
    }

    // Para obtener el currentPlayerId real del usuario de la app (si es una partida multijugador)
    // Esto es un placeholder, en una app real, el currentPlayerId vendría de un sistema de autenticación.
    // Para propósitos de preview o testing, asumimos el host es el current player.
    LaunchedEffect(Unit) {
        val fetchedGame = gameService.getGame(gameId).firstOrNull() // Usar firstOrNull para obtener el valor una vez
        currentPlayerId.value = fetchedGame?.hostPlayerId
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Puntuación Final") },
                navigationIcon = {
                    IconButton(onClick = onBackToMainMenu) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¡Partida Terminada!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = AccentYellow,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Mostrar un indicador de carga si los datos aún no están disponibles
            if (game == null || allPlayers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AccentPurple)
                Text(text = "Cargando resultados...", color = TextLight, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(
                    text = "Resultados de la Partida: ${gameId}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(allPlayers) { player ->
                        // Calcular PV detallados
                        val moneyPV = player.money / 5

                        // Suma del coste de venta base de todos los cultivos no vendidos
                        val totalUnsoldValue = player.inventario.sumOf { it.valorVentaBase * it.cantidad }
                        // PV por cultivos no vendidos: (suma del coste de venta que se obtendría de todos dividido entre 3)
                        val unsoldCropsPV = (totalUnsoldValue.toDouble() / 3.0).roundToInt()

                        val objectivesPV = player.objectivesClaimed.sumOf { objId ->
                            allObjectives.find { it.id == objId }?.rewardPV ?: 0
                        }
                        val totalPV = moneyPV + unsoldCropsPV + objectivesPV + player.manualBonusPV // Sumar bonus manual

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Jugador: ${player.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                                Text(text = "Monedas Finales: ${player.money} 💰", fontSize = 14.sp, color = TextLight)
                                Text(text = "Cultivos No Vendidos: ${player.inventario.sumOf { it.cantidad }}", fontSize = 14.sp, color = TextLight)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "PV por Monedas: $moneyPV PV", fontSize = 14.sp, color = TextLight)
                                Text(text = "PV por Cultivos No Vendidos: $unsoldCropsPV PV", fontSize = 14.sp, color = TextLight)
                                Text(text = "PV por Objetivos Reclamados: $objectivesPV PV", fontSize = 14.sp, color = TextLight)
                                if (player.manualBonusPV != 0) {
                                    Text(text = "PV Ajuste Manual: ${player.manualBonusPV} PV", fontSize = 14.sp, color = TextLight)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "PV Total: $totalPV PV", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            // Limpiar los datos de la partida de Firestore cuando se vuelve al menú principal
                            gameService.cleanUpGameData(gameId)
                            onBackToMainMenu()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(32.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(text = "Volver al Menú Principal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                }
            }
        }
    }
}

// REMOVIDO: ManualPVAdjustmentSection se ha movido a PlayerManagementScreen

@Preview(showBackground = true)
@Composable
fun FinalScoreScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FinalScoreScreen(gameId = "sampleGame123", onBackToMainMenu = {})
        }
    }
}
