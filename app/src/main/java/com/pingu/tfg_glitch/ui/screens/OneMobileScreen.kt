package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.*
import kotlinx.coroutines.launch
import android.util.Log
import com.pingu.tfg_glitch.ui.components.MarketItem


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneMobileScreen(
    onBackToMainMenu: () -> Unit
) {
    var gameId by remember { mutableStateOf<String?>(null) }
    var playerId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val game by gameId?.let { gameService.getGame(it).collectAsState(initial = null) } ?: remember { mutableStateOf(null) }
    val player by playerId?.let { firestoreService.getPlayer(it).collectAsState(initial = null) } ?: remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMysteryEncounterDialog by remember { mutableStateOf(false) }
    var showMysteryResultDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val (newGameId, newPlayerId) = gameService.createOneMobileGame()
            gameId = newGameId
            playerId = newPlayerId
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error al iniciar el modo un móvil: ${e.message}"
            isLoading = false
        }
    }

    LaunchedEffect(player?.activeMysteryId, player?.lastMysteryResult) {
        showMysteryEncounterDialog = player?.activeMysteryId != null
        showMysteryResultDialog = player?.lastMysteryResult != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Modo Un Móvil") },
                navigationIcon = {
                    IconButton(onClick = onBackToMainMenu) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Text("Iniciando modo un móvil...")
            } else if (errorMessage != null) {
                Text(text = "Error: $errorMessage", color = GlitchRed)
                Button(onClick = onBackToMainMenu) { Text("Volver") }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Mercado Actual", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                Spacer(modifier = Modifier.height(8.dp))
                                game?.marketPrices?.let { prices ->
                                    val cropList = listOf(
                                        "trigo" to prices.trigo, "maiz" to prices.maiz, "patata" to prices.patata,
                                        "tomateCuadrado" to prices.tomateCuadrado, "maizArcoiris" to prices.maizArcoiris,
                                        "brocoliCristal" to prices.brocoliCristal, "pimientoExplosivo" to prices.pimientoExplosivo
                                    )
                                    // --- [CORRECCIÓN] Código para mostrar MarketItems restaurado ---
                                    for (i in cropList.indices step 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MarketItem(cropList[i].first, cropList[i].second, modifier = Modifier.weight(1f))
                                            if (i + 1 < cropList.size) {
                                                MarketItem(cropList[i+1].first, cropList[i+1].second, modifier = Modifier.weight(1f))
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                        if (i + 2 < cropList.size) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                } ?: Text("Cargando precios...")
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Evento Glitch de Ronda", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AccentRed)
                                Text(game?.lastEvent?.name ?: "Sin evento", fontSize = 20.sp)
                                Text(game?.lastEvent?.description ?: "No ha ocurrido ningún evento en esta ronda.", textAlign = TextAlign.Center)
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    playerId?.let {
                                        gameService.startMysteryEncounter(it, isOneMobileMode = true)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp).padding(bottom = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlitchBlue)
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = "Activar Misterio")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activar Misterio")
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    gameId?.let { id ->
                                        val success = gameService.advanceOneMobileRound(id)
                                        snackbarHostState.showSnackbar(
                                            if (success) "¡Ronda avanzada!" else "Error al avanzar la ronda."
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Text("Avanzar Ronda")
                        }
                    }
                }
            }
        }
    }

    val activeMysteryId = player?.activeMysteryId
    if (showMysteryEncounterDialog && activeMysteryId != null) {
        val encounter = allMysteryEncounters.find { it.id == activeMysteryId }
        if (encounter != null) {
            MysteryEncounterDialog(
                encounter = encounter,
                onChoiceSelected = { choiceId ->
                    coroutineScope.launch {
                        playerId?.let { gameService.resolveMysteryOutcome(it, choiceId) }
                    }
                },
                onMinigameResult = { wasSuccessful ->
                    coroutineScope.launch {
                        playerId?.let { gameService.resolveMinigameOutcome(it, wasSuccessful) }
                    }
                }
            )
        }
    }

    val lastMysteryResult = player?.lastMysteryResult
    if (showMysteryResultDialog && lastMysteryResult != null) {
        AlertDialog(
            onDismissRequest = {
                coroutineScope.launch {
                    playerId?.let { gameService.clearMysteryResult(it) }
                }
            },
            title = { Text("Resultado del Misterio") },
            text = { Text(lastMysteryResult) },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        playerId?.let { gameService.clearMysteryResult(it) }
                    }
                }) { Text("Entendido") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OneMobileScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            OneMobileScreen(onBackToMainMenu = {})
        }
    }
}
