package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.components.PlayerInfoCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.getIconForCrop
import kotlinx.coroutines.launch
import kotlin.math.max

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal del Mercado ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(gameId: String, currentPlayerId: String) {
    // --- Estados ---
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estado para el diálogo de venta
    var showSellDialog by remember { mutableStateOf(false) }
    var selectedCropForSale by remember { mutableStateOf<CultivoInventario?>(null) }

    // --- Estados Derivados ---
    val isMarketPhase by remember(game) { derivedStateOf { game?.roundPhase == "MARKET_PHASE" } }
    val hasPlayerFinishedMarket by remember(game, currentPlayerId) {
        derivedStateOf { game?.playersFinishedMarket?.contains(currentPlayerId) ?: false }
    }

    // Nuevo estado para el diálogo del comerciante
    var showMerchantReminderDialog by remember { mutableStateOf(false) }

    val currentMarketPrices = remember(game?.marketPrices, game?.signalInterferenceActive) {
        val prices = game?.marketPrices ?: initialMarketPrices
        if (game?.signalInterferenceActive == true) {
            prices.copy(
                zanahoria = max(1, prices.zanahoria / 2),
                trigo = max(1, prices.trigo / 2),
                patata = max(1, prices.patata / 2),
                tomateCubico = max(1, prices.tomateCubico / 2),
                maizArcoiris = max(1, prices.maizArcoiris / 2),
                brocoliCristal = max(1, prices.brocoliCristal / 2),
                pimientoExplosivo = max(1, prices.pimientoExplosivo / 2)
            )
        } else {
            prices
        }
    }


    // --- UI Principal ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenido desplazable de la pantalla
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    StatusHeader(game = game, player = currentPlayer)
                }
                item {
                    game?.let { EventSection(game = it) }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                item {
                    val inventoryMap = currentPlayer?.inventario?.associateBy { it.id } ?: emptyMap()
                    CombinedMarketAndInventoryView(
                        allCrops = allCrops,
                        inventory = inventoryMap,
                        marketPrices = currentMarketPrices,
                        enabled = isMarketPhase && !hasPlayerFinishedMarket,
                        onSellClick = { cropInventoryItem ->
                            selectedCropForSale = cropInventoryItem
                            showSellDialog = true
                        }
                    )
                }
            }

            // Botones de acción fijo en la parte inferior
            if (isMarketPhase) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botón para la habilidad activa del Comerciante Sombrío
                    if (currentPlayer?.granjero?.id == "comerciante_sombrio") {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val success = gameService.usarActivableComerciante(currentPlayerId)
                                    if (success) showMerchantReminderDialog = true
                                }
                            },
                            enabled = !hasPlayerFinishedMarket && !(currentPlayer?.haUsadoHabilidadActiva ?: false) && (currentPlayer?.glitchEnergy ?: 0) >= 1,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text("Activar Habilidad Comerciante (1⚡)")
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val bonusMoney = if(currentPlayer?.granjero?.id == "comerciante_sombrio") (currentPlayer?.cropsSoldThisMarketPhase ?: 0) / 2 else 0
                                gameService.playerFinishedMarket(gameId, currentPlayerId)
                                val snackbarMessage = if (bonusMoney > 0) {
                                    "Has terminado tus acciones en el Mercado. ¡Ganaste $bonusMoney💰 extra por la habilidad!"
                                } else {
                                    "Has terminado tus acciones en el Mercado."
                                }
                                snackbarHostState.showSnackbar(snackbarMessage)
                            }
                        },
                        enabled = !hasPlayerFinishedMarket,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(if (hasPlayerFinishedMarket) "Esperando a otros..." else "He Terminado en el Mercado")
                    }
                }
            }
        }
    }

    // Diálogo de venta mejorado
    if (showSellDialog && selectedCropForSale != null) {
        val crop = selectedCropForSale!!
        val maxQuantity = crop.cantidad
        var quantityToSell by remember { mutableStateOf(1) }

        AlertDialog(
            onDismissRequest = { showSellDialog = false },
            title = { Text("Vender ${crop.nombre}") },
            text = {
                Column {
                    val price = when (crop.id) {
                        "zanahoria" -> currentMarketPrices.zanahoria
                        "trigo" -> currentMarketPrices.trigo
                        "patata" -> currentMarketPrices.patata
                        "tomateCubico" -> currentMarketPrices.tomateCubico
                        "maizArcoiris" -> currentMarketPrices.maizArcoiris
                        "brocoliCristal" -> currentMarketPrices.brocoliCristal
                        "pimientoExplosivo" -> currentMarketPrices.pimientoExplosivo
                        else -> 0
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Precio actual: $price")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Paid, contentDescription = "Moneda", modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cantidad a vender:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (quantityToSell > 1) quantityToSell-- }) {
                            Icon(Icons.Default.Remove, contentDescription = "Restar")
                        }
                        Text("$quantityToSell", style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { if (quantityToSell < maxQuantity) quantityToSell++ }) {
                            Icon(Icons.Default.Add, contentDescription = "Sumar")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val price = when (crop.id) {
                                "zanahoria" -> currentMarketPrices.zanahoria
                                "trigo" -> currentMarketPrices.trigo
                                "patata" -> currentMarketPrices.patata
                                "tomateCubico" -> currentMarketPrices.tomateCubico
                                "maizArcoiris" -> currentMarketPrices.maizArcoiris
                                "brocoliCristal" -> currentMarketPrices.brocoliCristal
                                "pimientoExplosivo" -> currentMarketPrices.pimientoExplosivo
                                else -> 0
                            }
                            val totalGanancia = (quantityToSell * price)

                            val success = gameService.sellCrop(gameId, currentPlayerId, crop.id, quantityToSell, totalGanancia)
                            if (success) {
                                snackbarHostState.showSnackbar("Vendiste $quantityToSell ${crop.nombre} por $totalGanancia 💰.")
                            } else {
                                snackbarHostState.showSnackbar("Error al vender. Verifica tu inventario.")
                            }
                        }
                        showSellDialog = false
                    },
                    enabled = quantityToSell > 0
                ) {
                    Text("Vender")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSellDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo para la habilidad del Comerciante Sombrío
    if (showMerchantReminderDialog) {
        AlertDialog(
            onDismissRequest = { showMerchantReminderDialog = false },
            title = { Text("Habilidad Activa: Comerciante Sombrío") },
            text = { Text("Hasta el final de esta fase de Mercado, ganas 1 Moneda adicional por cada cultivo que vendas. Esto se aplicará automáticamente.") },
            confirmButton = {
                TextButton(onClick = { showMerchantReminderDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

/**
 * Muestra la información del estado actual de la partida y del jugador.
 */
@Composable
private fun StatusHeader(game: Game?, player: Player?) {
    val phaseText = when (game?.roundPhase) {
        "PLAYER_ACTIONS" -> "Fase de Acciones"
        "MARKET_PHASE" -> "Fase de Mercado"
        else -> "Cargando..."
    }
    val phaseColor = if (game?.roundPhase == "MARKET_PHASE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Mercado Glitch",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = phaseText,
            style = MaterialTheme.typography.titleMedium,
            color = phaseColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        player?.let {
            PlayerInfoCard(it) // Ahora es una función pública
        }
    }
}

/**
 * Muestra el inventario del jugador con opciones para vender.
 */
@Composable
private fun CombinedMarketAndInventoryView(
    allCrops: List<CartaSemilla>,
    inventory: Map<String, CultivoInventario>,
    marketPrices: MarketPrices,
    enabled: Boolean,
    onSellClick: (CultivoInventario) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Precios y tu Inventario",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(allCrops, key = { it.id }) { crop ->
                val inventoryItem = inventory[crop.id]
                val quantity = inventoryItem?.cantidad ?: 0
                val currentPrice = remember(crop.id, marketPrices) {
                    when (crop.id) {
                        "zanahoria" -> marketPrices.zanahoria
                        "trigo" -> marketPrices.trigo
                        "patata" -> marketPrices.patata
                        "tomateCubico" -> marketPrices.tomateCubico
                        "maizArcoiris" -> marketPrices.maizArcoiris
                        "brocoliCristal" -> marketPrices.brocoliCristal
                        "pimientoExplosivo" -> marketPrices.pimientoExplosivo
                        else -> 0
                    }
                }
                ElevatedCard(
                    onClick = {
                        if (inventoryItem != null) {
                            onSellClick(inventoryItem)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && quantity > 0,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Icono del cultivo
                        Icon(
                            imageVector = getIconForCrop(crop.id),
                            contentDescription = crop.nombre,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        // Cantidad en inventario
                        Text(
                            text = "x$quantity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Precio del cultivo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currentPrice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.Paid, contentDescription = "Moneda", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}


/**
 * Muestra el evento de la ronda y los efectos permanentes.
 */
@Composable
private fun EventSection(game: Game) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tarjeta del Evento de Ronda
        game.lastEvent?.let { event ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Indicadores de Efectos Persistentes
        if (game.supplyFailureActive) {
            InfoCard(
                text = "¡Fallo de Suministro Activo! El coste de plantado ha aumentado.",
                icon = Icons.Default.Warning,
                color = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            )
        }
        if (game.signalInterferenceActive) {
            InfoCard(
                text = "¡Interferencia de Señal Activa! Precios de mercado reducidos esta ronda.",
                icon = Icons.Default.Info,
                color = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            )
        }
    }
}

/**
 * Una tarjeta genérica para mostrar información o alertas.
 */
@Composable
private fun InfoCard(text: String, icon: ImageVector, color: CardColors) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = color) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(end = 16.dp))
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


// ========================================================================
// --- Preview ---
// ========================================================================

@Preview(showBackground = true)
@Composable
fun PreviewMarketScreen() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MarketScreen(gameId = "ABCDEF", currentPlayerId = "sample-player-id")
        }
    }
}
