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

    // Estados para el mercado y eventos
    var marketPrices by remember { mutableStateOf(com.pingu.tfg_glitch.data.initialMarketPrices) } // Asegúrate de que esto apunta a GameData.kt
    var lastEvent by remember { mutableStateOf<com.pingu.tfg_glitch.data.GlitchEvent?>(null) } // Asegúrate de que esto apunta a GameData.kt
    var supplyFailureActive by remember { mutableStateOf(false) }
    var signalInterferenceActive by remember { mutableStateOf(false) }

    // Estado para controlar si una venta está en curso
    var isSelling by remember { mutableStateOf(false) }

    // Determina si es la fase de mercado
    val isMarketPhase = remember(game) {
        game?.roundPhase == "MARKET_PHASE"
    }

    // Determina si el jugador actual ha terminado en el mercado
    val hasPlayerFinishedMarket = remember(game, currentPlayerId) {
        game?.playersFinishedMarket?.contains(currentPlayerId) ?: false
    }

    // Actualiza los estados del mercado cuando el objeto 'game' cambia
    LaunchedEffect(game) {
        game?.let {
            marketPrices = it.marketPrices
            lastEvent = it.lastEvent
            supplyFailureActive = it.supplyFailureActive
            signalInterferenceActive = it.signalInterferenceActive
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica el padding de Scaffold
                .padding(horizontal = 16.dp), // Padding horizontal para el contenido
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título de la pantalla (siempre visible)
            Text(
                text = "Mercado Glitch",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )

            // Contenido principal de la pantalla (desplazable)
            LazyColumn(
                modifier = Modifier.weight(1f), // Ocupa todo el espacio disponible y permite el scroll
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 16.dp) // Padding al final del contenido desplazable
            ) {
                item {
                    // Información del jugador actual
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

                        // Indicador de fase
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
                    // Tarjeta del Mercado
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        // Usar Column y Rows en lugar de LazyVerticalGrid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp), // Padding ligeramente reducido aquí
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            game?.marketPrices?.let { prices ->
                                val cropList = listOf(
                                    "trigo" to prices.trigo,
                                    "maiz" to prices.maiz,
                                    "patata" to prices.patata,
                                    "tomateCuadrado" to prices.tomateCuadrado,
                                    "maizArcoiris" to prices.maizArcoiris,
                                    "brocoliCristal" to prices.brocoliCristal,
                                    "pimientoExplosivo" to prices.pimientoExplosivo
                                )
                                // Organizar manualmente en filas de 2 columnas
                                for (i in cropList.indices step 2) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp), // Espaciado reducido
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MarketItem(cropList[i].first, cropList[i].second, modifier = Modifier.weight(1f))
                                        if (i + 1 < cropList.size) {
                                            MarketItem(cropList[i+1].first, cropList[i+1].second, modifier = Modifier.weight(1f))
                                        } else {
                                            // Añadir un Spacer para ocupar el espacio si hay un número impar de ítems
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (i + 2 < cropList.size) { // Añadir espaciado vertical entre filas
                                        Spacer(modifier = Modifier.height(4.dp)) // Espaciado reducido
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Sección de Inventario Cosechado
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
                        // Usar Column con verticalScroll
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCard)
                                .verticalScroll(rememberScrollState()), // Permite el scroll interno
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            currentPlayer!!.inventario.forEach { item -> // Usar forEach para Column
                                val marketKey = com.pingu.tfg_glitch.data.getCropMarketKey(item.nombre)
                                val currentPrice = when (marketKey) {
                                    "trigo" -> marketPrices.trigo
                                    "maiz" -> marketPrices.maiz
                                    "patata" -> marketPrices.patata
                                    "tomateCuadrado" -> marketPrices.tomateCuadrado
                                    "maizArcoiris" -> marketPrices.maizArcoiris
                                    "brocoliCristal" -> marketPrices.brocoliCristal
                                    "pimientoExplosivo" -> marketPrices.pimientoExplosivo
                                    else -> 0
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard)
                                        .padding(8.dp), // El padding se aplica aquí a cada fila
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "${item.nombre} (x${item.cantidad})", color = TextLight, fontSize = 16.sp)
                                    Text(text = "Venta: $currentPrice 💰", color = TextLight, fontSize = 16.sp)
                                    val buttonEnabled = item.cantidad > 0 && currentPrice > 0 && !isSelling && isMarketPhase && !hasPlayerFinishedMarket
                                    Button(
                                        onClick = {
                                            if (!isSelling) {
                                                isSelling = true
                                                coroutineScope.launch {
                                                    val success = gameService.sellCrop(currentPlayerId, item.id, 1, currentPrice)
                                                    if (success) {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Vendiste 1 ${item.nombre} por $currentPrice monedas.",
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
                    // Indicadores de efectos permanentes
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
                    // Tarjeta del Evento Glitch de Ronda
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
                                text = "Evento Glitch de Ronda", // Título más específico
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentRed,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Usa el último evento del objeto Game de Firestore
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

            // Mensaje de carga si el jugador actual no se ha cargado (fuera del LazyColumn)
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

            // Botón "He Terminado en el Mercado" (ANCLADO AL FINAL)
            Button(
                onClick = {
                    coroutineScope.launch {
                        gameService.playerFinishedMarket(gameId, currentPlayerId) // Se pasa currentPlayerId dos veces
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
                    .padding(top = 8.dp), // Pequeño padding superior para separarlo
                colors = ButtonDefaults.buttonColors(containerColor = if (isMarketPhase && !hasPlayerFinishedMarket) GlitchBlue else Color.Gray),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = if (hasPlayerFinishedMarket) "Esperando a otros jugadores..." else "He Terminado en el Mercado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

// Composable para cada elemento del mercado
// Se ha movido a ui.components.MarketItem.kt
/*
@Composable
fun MarketItem(cropName: String, price: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4A5568)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cropName.capitalizeWords(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = AccentGreen
            )
            Text(
                text = "$price 💰",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AccentYellow
            )
        }
    }
}
*/

@Preview(showBackground = true)
@Composable
fun PreviewMarketScreen() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MarketScreen(gameId = "ABCDEF", currentPlayerId = "sample-player-id")
        }
    }
}
