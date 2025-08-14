package com.pingu.tfg_glitch.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector // Importar ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.CultivoInventario
import com.pingu.tfg_glitch.data.DadoSimbolo
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Player
import com.pingu.tfg_glitch.data.allCrops
import com.pingu.tfg_glitch.data.CartaSemilla
import com.pingu.tfg_glitch.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.foundation.rememberScrollState // Importación necesaria
import androidx.compose.foundation.verticalScroll // Importación necesaria
import com.pingu.tfg_glitch.ui.components.MarketItem // Importar MarketItem desde el nuevo paquete
import kotlin.math.max


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(gameId: String, currentPlayerId: String) {
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var marketPrices by remember { mutableStateOf(com.pingu.tfg_glitch.data.initialMarketPrices) }
    var lastEvent by remember { mutableStateOf<com.pingu.tfg_glitch.data.GlitchEvent?>(null) }
    var supplyFailureActive by remember { mutableStateOf(false) }
    var signalInterferenceActive by remember { mutableStateOf(false) }
    var isSelling by remember { mutableStateOf(false) }
    var cropsSoldThisTurn by remember { mutableStateOf(0) }

    val isMarketPhase = game?.roundPhase == "MARKET_PHASE"
    val hasPlayerFinishedMarket = game?.playersFinishedMarket?.contains(currentPlayerId) ?: false

    LaunchedEffect(game) {
        game?.let {
            marketPrices = it.marketPrices
            lastEvent = it.lastEvent
            supplyFailureActive = it.supplyFailureActive
            signalInterferenceActive = it.signalInterferenceActive
        }
    }

    LaunchedEffect(isMarketPhase, currentPlayer?.inventario) {
        if (isMarketPhase && currentPlayer?.inventario.isNullOrEmpty() && !hasPlayerFinishedMarket) {
            coroutineScope.launch {
                gameService.playerFinishedMarket(gameId, currentPlayerId)
                snackbarHostState.showSnackbar(
                    message = "No tienes nada que vender. Turno de mercado saltado.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

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
            Text(
                text = "Mercado Glitch",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
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

                        val phaseText = when (game?.roundPhase) {
                            "PLAYER_ACTIONS" -> "Fase Actual: Acciones de Jugador"
                            "MARKET_PHASE" -> "Fase Actual: Mercado Glitch"
                            else -> "Cargando fase..."
                        }
                        val phaseColor = if (game?.roundPhase == "MARKET_PHASE") AccentGreen else TextLight
                        Text(
                            text = phaseText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = phaseColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            game?.let { g ->
                                val prices = g.marketPrices
                                val boosts = g.temporaryPriceBoosts
                                val isInterference = g.signalInterferenceActive
                                val cropList = listOf(
                                    "trigo" to (prices.trigo + (boosts["trigo"] ?: 0)),
                                    "maiz" to (prices.maiz + (boosts["maiz"] ?: 0)),
                                    "patata" to (prices.patata + (boosts["patata"] ?: 0)),
                                    "tomateCuadrado" to (prices.tomateCuadrado + (boosts["tomate_cuadrado"] ?: 0)),
                                    "maizArcoiris" to (prices.maizArcoiris + (boosts["maiz_arcoiris"] ?: 0)),
                                    "brocoliCristal" to (prices.brocoliCristal + (boosts["brocoli_cristal"] ?: 0)),
                                    "pimientoExplosivo" to (prices.pimientoExplosivo + (boosts["pimiento_explosivo"] ?: 0))
                                )
                                for (i in cropList.indices step 2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val price1 = if(isInterference) max(1, cropList[i].second / 2) else cropList[i].second
                                        MarketItem(cropList[i].first, price1, modifier = Modifier.weight(1f))

                                        if (i + 1 < cropList.size) {
                                            val price2 = if(isInterference) max(1, cropList[i+1].second / 2) else cropList[i+1].second
                                            MarketItem(cropList[i+1].first, price2, modifier = Modifier.weight(1f))
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (i + 2 < cropList.size) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Tu Inventario Cosechado",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreen,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    if (currentPlayer?.inventario.isNullOrEmpty()) {
                        Text(text = "Tu inventario está vacío.", color = TextLight, modifier = Modifier.padding(bottom = 16.dp))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCard)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            currentPlayer!!.inventario.forEach { item ->
                                val marketKey = com.pingu.tfg_glitch.data.getCropMarketKey(item.nombre)
                                val basePrice = when (marketKey) {
                                    "trigo" -> marketPrices.trigo + (game?.temporaryPriceBoosts?.get("trigo") ?: 0)
                                    "maiz" -> marketPrices.maiz + (game?.temporaryPriceBoosts?.get("maiz") ?: 0)
                                    "patata" -> marketPrices.patata + (game?.temporaryPriceBoosts?.get("patata") ?: 0)
                                    "tomateCuadrado" -> marketPrices.tomateCuadrado + (game?.temporaryPriceBoosts?.get("tomate_cuadrado") ?: 0)
                                    "maizArcoiris" -> marketPrices.maizArcoiris + (game?.temporaryPriceBoosts?.get("maiz_arcoiris") ?: 0)
                                    "brocoliCristal" -> marketPrices.brocoliCristal + (game?.temporaryPriceBoosts?.get("brocoli_cristal") ?: 0)
                                    "pimientoExplosivo" -> marketPrices.pimientoExplosivo + (game?.temporaryPriceBoosts?.get("pimiento_explosivo") ?: 0)
                                    else -> 0
                                }
                                val finalPrice = if (signalInterferenceActive) max(1, basePrice / 2) else basePrice

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "${item.nombre} (x${item.cantidad})", color = TextLight, fontSize = 16.sp)
                                    Text(text = "Venta: $finalPrice 💰", color = TextLight, fontSize = 16.sp)
                                    val buttonEnabled = item.cantidad > 0 && finalPrice > 0 && !isSelling && isMarketPhase && !hasPlayerFinishedMarket
                                    Button(
                                        onClick = {
                                            if (!isSelling) {
                                                isSelling = true
                                                coroutineScope.launch {
                                                    val success = gameService.sellCrop(currentPlayerId, item.id, 1, finalPrice)
                                                    if (success) {
                                                        if (currentPlayer?.farmerType == "Comerciante Sombrío") {
                                                            cropsSoldThisTurn++
                                                        }
                                                        snackbarHostState.showSnackbar(
                                                            message = "Vendiste 1 ${item.nombre} por $finalPrice monedas.",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    } else {
                                                        snackbarHostState.showSnackbar(
                                                            message = "No se pudo vender ${item.nombre}.",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                    }
                                                    isSelling = false
                                                }
                                            }
                                        },
                                        enabled = buttonEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (buttonEnabled) AccentYellow else Color.Gray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isSelling) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground)
                                        } else {
                                            Icon(Icons.Filled.Sell, contentDescription = "Vender", modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Vender 1", fontSize = 12.sp, color = DarkBackground)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    if (currentPlayer?.farmerType == "Comerciante Sombrío" && cropsSoldThisTurn > 0) {
                        val bonus = cropsSoldThisTurn / 3
                        Text("Bonus de venta actual: $bonus 💰", color = AccentYellow, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    if (game?.supplyFailureActive == true) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = GlitchRed),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Text(
                                text = "¡Fallo de Suministro Activo! El coste de plantado ha aumentado permanentemente.",
                                fontSize = 16.sp,
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    if (game?.signalInterferenceActive == true) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = GlitchBlue),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Text(
                                text = "¡Interferencia de Señal Activa! Precios de mercado reducidos a la mitad para esta ronda.",
                                fontSize = 16.sp,
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
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
                                text = "Evento Glitch de Ronda",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (game?.lastEvent != null) {
                                Text(
                                    text = game!!.lastEvent!!.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextLight,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = game!!.lastEvent!!.description,
                                    fontSize = 16.sp,
                                    color = TextLight,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "No ha ocurrido ningún evento en esta ronda.",
                                    fontSize = 16.sp,
                                    color = TextLight,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            if (currentPlayer == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AccentPurple)
                    Text(text = "Cargando datos del jugador...", color = TextLight, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (currentPlayer?.farmerType == "Comerciante Sombrío") {
                            gameService.applyMerchantBonusAndFinishMarket(gameId, currentPlayerId, cropsSoldThisTurn)
                        } else {
                            gameService.playerFinishedMarket(gameId, currentPlayerId)
                        }
                        snackbarHostState.showSnackbar(
                            message = "Has terminado tus acciones en el Mercado.",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                enabled = isMarketPhase && !hasPlayerFinishedMarket,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isMarketPhase && !hasPlayerFinishedMarket) GlitchBlue else Color.Gray),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = if (hasPlayerFinishedMarket) "Esperando a otros jugadores..." else "He Terminado en el Mercado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMarketScreen() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MarketScreen(gameId = "ABCDEF", currentPlayerId = "sample-player-id")
        }
    }
}
