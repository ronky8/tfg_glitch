package com.pingu.tfg_glitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.getIconForCoin
import com.pingu.tfg_glitch.ui.theme.getIconForCrop
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
    onBackToMainMenu: () -> Unit,
    onAttemptExit: () -> Unit
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

    // Captura el gesto de retroceso y lo redirige a la lógica de confirmación
    BackHandler {
        onAttemptExit()
    }

    // --- UI Principal ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Director de Juego") },
                navigationIcon = {
                    IconButton(onClick = onAttemptExit) { // Botón también intenta salir
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
                    Text(errorMessage!!, textAlign = TextAlign.Center)
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
                    item {
                        RoundCounterCard(game?.roundNumber)
                    }
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
private fun RoundCounterCard(roundNumber: Int?) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Ronda Actual",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = roundNumber?.toString() ?: "-",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MarketPricesCard(marketPrices: MarketPrices?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Mercado Actual",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            if (marketPrices != null) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 200.dp) // Altura controlada
                ) {
                    items(allCrops, key = { it.id }) { crop ->
                        val price = when(crop.id) {
                            "zanahoria" -> marketPrices.zanahoria
                            "trigo" -> marketPrices.trigo
                            "patata" -> marketPrices.patata
                            "tomateCubico" -> marketPrices.tomateCubico
                            "maizArcoiris" -> marketPrices.maizArcoiris
                            "brocoliCristal" -> marketPrices.brocoliCristal
                            "pimientoExplosivo" -> marketPrices.pimientoExplosivo
                            else -> 0
                        }
                        MarketItem(cropId = crop.id, cropName = crop.nombre, price = price)
                    }
                }
            } else {
                Text("Cargando precios...")
            }
        }
    }
}

@Composable
private fun MarketItem(cropId: String, cropName: String, price: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = getIconForCrop(cropId),
            contentDescription = cropName,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.secondary
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$price",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                painter = getIconForCoin(),
                contentDescription = "Moneda",
                modifier = Modifier.size(18.dp).padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
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

@Preview(showBackground = true)
@Composable
fun OneMobileScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            OneMobileScreen(onBackToMainMenu = {}, onAttemptExit = {})
        }
    }
}

