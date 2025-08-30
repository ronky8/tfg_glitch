package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.components.MarketItem
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.launch

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal del Modo Un Móvil ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneMobileScreen(
    onBackToMainMenu: () -> Unit
) {
    // --- Estados ---
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

    // --- Efectos ---
    LaunchedEffect(Unit) {
        try {
            val (newGameId, newPlayerId) = gameService.createOneMobileGame()
            gameId = newGameId
            playerId = newPlayerId
        } catch (e: Exception) {
            errorMessage = "Error al iniciar: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(player?.activeMysteryId, player?.lastMysteryResult) {
        showMysteryEncounterDialog = player?.activeMysteryId != null
        showMysteryResultDialog = player?.lastMysteryResult != null
    }

    // --- UI Principal ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Director de Juego") },
                navigationIcon = {
                    IconButton(onClick = onBackToMainMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Column(
                    Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
                    Text(errorMessage!!, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBackToMainMenu) { Text("Volver al Menú") }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { MarketPricesCard(game?.marketPrices) }
                    item { GlitchEventCard(game?.lastEvent) }
                    item {
                        ActionButtons(
                            onActivateMystery = {
                                coroutineScope.launch {
                                    playerId?.let {
                                        gameService.startMysteryEncounter(it, isOneMobileMode = true)
                                    }
                                }
                            },
                            onAdvanceRound = {
                                coroutineScope.launch {
                                    gameId?.let { id ->
                                        val success = gameService.advanceOneMobileRound(id)
                                        snackbarHostState.showSnackbar(if (success) "¡Ronda avanzada!" else "Error.")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Diálogos ---
    val activeMysteryId = player?.activeMysteryId
    if (showMysteryEncounterDialog && activeMysteryId != null) {
        val encounter = allMysteryEncounters.find { it.id == activeMysteryId }
        if (encounter != null) {
            MysteryEncounterDialog( // Reutilizamos el diálogo de PlayerActionsScreen
                encounter = encounter,
                onChoiceSelected = { choiceId ->
                    coroutineScope.launch { playerId?.let { gameService.resolveMysteryOutcome(it, choiceId) } }
                },
                onMinigameResult = { wasSuccessful ->
                    coroutineScope.launch { playerId?.let { gameService.resolveMinigameOutcome(it, wasSuccessful) } }
                }
            )
        }
    }

    val lastMysteryResult = player?.lastMysteryResult
    if (showMysteryResultDialog && lastMysteryResult != null) {
        AlertDialog(
            onDismissRequest = { coroutineScope.launch { playerId?.let { gameService.clearMysteryResult(it) } } },
            title = { Text("Resultado del Misterio") },
            text = { Text(lastMysteryResult) },
            confirmButton = {
                TextButton(onClick = { coroutineScope.launch { playerId?.let { gameService.clearMysteryResult(it) } } }) {
                    Text("Entendido")
                }
            }
        )
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

@Composable
private fun MarketPricesCard(marketPrices: MarketPrices?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Mercado Actual",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (marketPrices != null) {
                val cropList = listOf(
                    "Zanahoria" to marketPrices.zanahoria,
                    "Trigo Común" to marketPrices.trigo,
                    "Patata Terrosa" to marketPrices.patata,
                    "Tomate Cúbico" to marketPrices.tomateCubico,
                    "Maíz Arcoíris" to marketPrices.maizArcoiris,
                    "Brócoli Cristal" to marketPrices.brocoliCristal,
                    "Pimiento Explosivo" to marketPrices.pimientoExplosivo
                )
                // Layout en dos columnas
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in cropList.indices step 2) {
                            MarketItem(cropName = cropList[i].first, price = cropList[i].second)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 1 until cropList.size step 2) {
                            MarketItem(cropName = cropList[i].first, price = cropList[i].second)
                        }
                    }
                }
            } else {
                Text("Cargando precios...")
            }
        }
    }
}

@Composable
private fun GlitchEventCard(event: GlitchEvent?) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Evento Glitch de Ronda",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (event != null) {
                Text(event.name, fontWeight = FontWeight.Bold)
                Text(event.description, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Sin evento esta ronda.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ActionButtons(onActivateMystery: () -> Unit, onAdvanceRound: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onActivateMystery,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Casino, "Activar Misterio")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Activar Misterio")
        }
        FilledTonalButton(
            onClick = onAdvanceRound,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Avanzar Ronda")
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Icon(Icons.Default.ChevronRight, "Avanzar Ronda")
        }
    }
}


// ========================================================================
// --- Preview ---
// ========================================================================

@Preview(showBackground = true)
@Composable
fun OneMobileScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            OneMobileScreen(onBackToMainMenu = {})
        }
    }
}

