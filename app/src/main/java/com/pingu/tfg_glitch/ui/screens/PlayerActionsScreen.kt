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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.automirrored.filled.Help
import kotlin.math.max


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

@Composable
fun PixelDice(
    symbol: DadoSimbolo,
    isKept: Boolean,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    val (color, text) = when (symbol) {
        DadoSimbolo.GLITCH -> GlitchMagenta to "G"
        DadoSimbolo.CRECIMIENTO -> AccentGreen to "+"
        DadoSimbolo.ENERGIA -> AccentYellow to "E"
        DadoSimbolo.MONEDA -> GlitchLime to "$"
        DadoSimbolo.MISTERIO -> GlitchCyan to "?"
        DadoSimbolo.PLANTAR -> Color.Green to "P"
    }

    val borderColor = if (isKept) GlitchCyan else PixelText.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(52.dp)
            .border(BorderStroke(2.dp, borderColor))
            .background(PixelCard)
            .clickable(onClick = onClick, enabled = isEnabled),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionsScreen(
    gameId: String,
    currentPlayerId: String,
    hasShownTurnPopup: Boolean,
    onTurnPopupShown: () -> Unit
) {
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    var showDiceRollAnimation by remember { mutableStateOf(false) }
    var showDiceEffectDialog by remember { mutableStateOf(false) }
    var diceEffectMessage by remember { mutableStateOf("") }
    val keptDiceIndices = remember { mutableStateListOf<Int>() }

    var showMysteryEncounterDialog by remember { mutableStateOf(false) }
    var showMysteryResultDialog by remember { mutableStateOf(false) }
    var showChangeSymbolDialog by remember { mutableStateOf(false) }
    var showMerchantBoostDialog by remember { mutableStateOf(false) }
    var showVisionaryConfirmDialog by remember { mutableStateOf(false) }

    var showTurnStartDialog by remember { mutableStateOf(false) }

    val playerState = remember(currentPlayer) {
        object {
            val currentDiceRoll = currentPlayer?.currentDiceRoll ?: emptyList()
            val rollPhase = currentPlayer?.rollPhase ?: 0
            val hasUsedStandardReroll = currentPlayer?.hasUsedStandardReroll ?: false
            val hasUsedFreeReroll = currentPlayer?.hasUsedFreeReroll ?: false
            val hasUsedActiveSkill = currentPlayer?.hasUsedActiveSkill ?: false
            val mysteryButtonsRemaining = currentPlayer?.mysteryButtonsRemaining ?: 0
            val activeMysteryId = currentPlayer?.activeMysteryId
            val lastMysteryResult = currentPlayer?.lastMysteryResult
        }
    }

    val isMyTurn = game?.currentPlayerTurnId == currentPlayerId
    val isPlayerActionsPhase = game?.roundPhase == "PLAYER_ACTIONS"
    val canPerformActions = isMyTurn && isPlayerActionsPhase

    val cosechableCrops = allCrops

    LaunchedEffect(isMyTurn, hasShownTurnPopup) {
        if (isMyTurn && !hasShownTurnPopup) {
            delay(500)
            showTurnStartDialog = true
            onTurnPopupShown()
        }
    }

    LaunchedEffect(playerState.activeMysteryId, playerState.lastMysteryResult) {
        showMysteryEncounterDialog = playerState.activeMysteryId != null
        showMysteryResultDialog = playerState.lastMysteryResult != null
    }

    Scaffold(
        containerColor = PixelBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GRANJA GLITCH",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GlitchCyan,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            currentPlayer?.let { player ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PixelCard),
                    shape = RoundedCornerShape(0.dp),
                    border = BorderStroke(2.dp, GlitchCyan.copy(alpha = 0.7f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = player.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PixelText)
                            Row {
                                Text(text = "${player.money} 💰", fontSize = 18.sp, color = GlitchLime)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "${player.glitchEnergy} ⚡", fontSize = 18.sp, color = AccentYellow)
                            }
                        }
                        Text(text = "Clase: ${player.farmerType}", fontSize = 16.sp, color = GlitchCyan)
                    }
                }

                Text(
                    text = "Inventario Cosechado",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (player.inventario.isEmpty()) {
                    Text(text = ">> INVENTARIO VACIO <<", color = PixelText.copy(alpha = 0.7f))
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .border(BorderStroke(2.dp, GlitchCyan.copy(alpha = 0.5f)))
                            .padding(8.dp)
                    ) {
                        LazyColumn {
                            items(player.inventario) { item: CultivoInventario ->
                                Text(text = "${item.nombre} (x${item.cantidad})", color = PixelText)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cosechar Cultivos",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PixelText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 150.dp)
                ) {
                    items(cosechableCrops) { crop: CartaSemilla ->
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    gameService.addCropToInventory(
                                        currentPlayerId,
                                        crop.id,
                                        crop.nombre,
                                        crop.valorVentaBase,
                                        crop.pvFinalJuego
                                    )
                                }
                            },
                            enabled = canPerformActions,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PixelCard),
                            border = BorderStroke(1.dp, GlitchCyan.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            Text(text = crop.nombre, fontSize = 12.sp, textAlign = TextAlign.Center, color = PixelText)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "--- SISTEMA DE DADOS ---",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlitchMagenta,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (showDiceRollAnimation) {
                    CircularProgressIndicator(modifier = Modifier.size(52.dp), color = GlitchCyan)
                } else if (playerState.currentDiceRoll.isNotEmpty() || playerState.rollPhase > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        playerState.currentDiceRoll.forEachIndexed { index, symbol ->
                            Box(contentAlignment = Alignment.TopEnd) {
                                PixelDice(
                                    symbol = symbol,
                                    isKept = keptDiceIndices.contains(index),
                                    isEnabled = playerState.rollPhase == 1 && canPerformActions,
                                    onClick = {
                                        if (keptDiceIndices.contains(index)) keptDiceIndices.remove(index)
                                        else keptDiceIndices.add(index)
                                    }
                                )
                                if (player.farmerType == "Ingeniero Glitch" && !playerState.hasUsedFreeReroll && playerState.rollPhase == 1 && canPerformActions) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                gameService.useEngineerFreeReroll(currentPlayerId, index)
                                            }
                                        },
                                        modifier = Modifier.size(20.dp).offset(x = 8.dp, y = (-8).dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Reroll Gratis", tint = GlitchCyan)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                when (playerState.rollPhase) {
                    0 -> {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    showDiceRollAnimation = true
                                    delay(250)
                                    gameService.rollDice(currentPlayerId)
                                    showDiceRollAnimation = false
                                    keptDiceIndices.clear()
                                }
                            },
                            enabled = canPerformActions,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlitchMagenta),
                            border = BorderStroke(2.dp, PixelText)
                        ) {
                            Text(">> TIRAR DADOS <<", color = PixelText, fontWeight = FontWeight.Bold)
                        }
                    }
                    1 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        showDiceRollAnimation = true
                                        delay(250)
                                        gameService.rerollDice(currentPlayerId, playerState.currentDiceRoll, keptDiceIndices.toList())
                                        showDiceRollAnimation = false
                                        keptDiceIndices.clear()
                                    }
                                },
                                enabled = !playerState.hasUsedStandardReroll && keptDiceIndices.isNotEmpty() && canPerformActions,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PixelCard),
                                border = BorderStroke(2.dp, GlitchCyan)
                            ) {
                                Text("RELANZAR", color = GlitchCyan)
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        diceEffectMessage = gameService.applyDiceEffects(currentPlayerId, playerState.currentDiceRoll)
                                        showDiceEffectDialog = true
                                    }
                                },
                                enabled = canPerformActions,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                border = BorderStroke(2.dp, PixelCard)
                            ) {
                                Text("CONFIRMAR", color = PixelCard.copy(alpha=0.9f), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    2 -> {
                        if (playerState.mysteryButtonsRemaining > 0) {
                            Button(
                                onClick = { coroutineScope.launch { gameService.startMysteryEncounter(currentPlayerId) } },
                                enabled = canPerformActions,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GlitchCyan),
                                border = BorderStroke(2.dp, PixelCard)
                            ) {
                                Text("RESOLVER MISTERIO (${playerState.mysteryButtonsRemaining})", color = PixelCard.copy(alpha=0.9f), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Button(
                            onClick = { coroutineScope.launch { gameService.advanceTurn(gameId, currentPlayerId) } },
                            enabled = canPerformActions,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PixelCard),
                            border = BorderStroke(2.dp, GlitchRed)
                        ) {
                            Text("TERMINAR TURNO", color = GlitchRed)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                when (player.farmerType) {
                    "Ingeniero Glitch" -> {
                        Button(
                            onClick = { showChangeSymbolDialog = true },
                            enabled = player.glitchEnergy >= 1 && canPerformActions && !playerState.hasUsedActiveSkill && playerState.rollPhase > 0,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                            border = BorderStroke(2.dp, PixelCard)
                        ) {
                            Text("Activar Habilidad (1⚡)", color = PixelCard.copy(alpha=0.9f), fontWeight = FontWeight.Bold)
                        }
                    }
                    "Visionaria Píxel" -> {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    gameService.useVisionarySkill(currentPlayerId)
                                    showVisionaryConfirmDialog = true
                                }
                            },
                            enabled = player.glitchEnergy >= 1 && canPerformActions && !playerState.hasUsedActiveSkill,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                            border = BorderStroke(2.dp, PixelCard)
                        ) {
                            Text("Ver Futuro (1⚡)", color = PixelCard.copy(alpha=0.9f), fontWeight = FontWeight.Bold)
                        }
                        Text("Recordatorio: Robas 1 carta extra al resolver una Carta de Catástrofe.", color = TextLight, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                    }
                    "Botánica Mutante" -> {
                        Text("Recordatorio: +1 crecimiento al plantar mutados. Habilidad: Paga 1🌀 para añadir +1 crecimiento temporal a un cultivo.", color = TextLight, textAlign = TextAlign.Center)
                    }
                    "Comerciante Sombrío" -> {
                        Button(
                            onClick = { showMerchantBoostDialog = true },
                            enabled = canPerformActions && !playerState.hasUsedActiveSkill,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                            border = BorderStroke(2.dp, PixelCard)
                        ) {
                            Text("Aumentar Precio (1🌀)", color = PixelCard.copy(alpha=0.9f), fontWeight = FontWeight.Bold)
                        }
                    }
                }

            } ?: Column(modifier=Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
                CircularProgressIndicator(color = GlitchCyan)
                Text("CARGANDO DATOS DEL JUGADOR...", color = PixelText, modifier = Modifier.padding(top=16.dp))
            }
        }
    }

    if (showDiceEffectDialog) {
        AlertDialog(
            onDismissRequest = { showDiceEffectDialog = false },
            title = { Text("Efectos de tu Tirada", color = GlitchCyan) },
            text = { Text(diceEffectMessage, color = PixelText) },
            confirmButton = {
                Button(
                    onClick = { showDiceEffectDialog = false },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlitchCyan)
                ) { Text("OK", color = PixelCard) }
            },
            containerColor = PixelCard,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.border(BorderStroke(2.dp, GlitchCyan))
        )
    }

    if (showMysteryEncounterDialog && playerState.activeMysteryId != null) {
        val encounter = allMysteryEncounters.find { it.id == playerState.activeMysteryId }
        if (encounter != null) {
            MysteryEncounterDialog(
                encounter = encounter,
                onChoiceSelected = { choiceId ->
                    coroutineScope.launch { gameService.resolveMysteryOutcome(currentPlayerId, choiceId) }
                },
                onMinigameResult = { wasSuccessful ->
                    coroutineScope.launch { gameService.resolveMinigameOutcome(currentPlayerId, wasSuccessful) }
                }
            )
        }
    }

    if (showMysteryResultDialog && playerState.lastMysteryResult != null) {
        AlertDialog(
            onDismissRequest = { coroutineScope.launch { gameService.clearMysteryResult(currentPlayerId) } },
            title = { Text("Resultado del Misterio", color = GlitchCyan) },
            text = { Text(playerState.lastMysteryResult, color = PixelText) },
            confirmButton = {
                Button(
                    onClick = { coroutineScope.launch { gameService.clearMysteryResult(currentPlayerId) } },
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlitchCyan)
                ) { Text("ENTENDIDO", color = PixelCard) }
            },
            containerColor = PixelCard,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.border(BorderStroke(2.dp, GlitchCyan))
        )
    }

    if (showChangeSymbolDialog) {
        ChangeSymbolDialog(
            currentDice = currentPlayer?.currentDiceRoll ?: emptyList(),
            onDismiss = { showChangeSymbolDialog = false },
            onSymbolChanged = { diceIndex, newSymbol ->
                coroutineScope.launch {
                    gameService.changeDiceSymbol(currentPlayerId, diceIndex, newSymbol)
                    showChangeSymbolDialog = false
                }
            }
        )
    }

    if (showTurnStartDialog) {
        AlertDialog(
            onDismissRequest = { showTurnStartDialog = false },
            title = { Text("¡Es tu turno!") },
            text = { Text("Recuerda robar 1 carta del mazo y poner 1 marcador de crecimiento sobre cada uno de tus cultivos plantados.") },
            confirmButton = {
                Button(onClick = { showTurnStartDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showVisionaryConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showVisionaryConfirmDialog = false },
            title = { Text("Habilidad Activada") },
            text = { Text("Has usado 'Visión de Futuro'. Mira las 3 cartas superiores del mazo físico y sigue las reglas de la habilidad.") },
            confirmButton = {
                Button(onClick = { showVisionaryConfirmDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showMerchantBoostDialog) {
        val mutatedCrops = allCrops.filter { it.tipo == TipoCultivo.MUTADO }
        AlertDialog(
            onDismissRequest = { showMerchantBoostDialog = false },
            title = { Text("Aumentar Precio de Venta") },
            text = {
                Column {
                    Text("Elige un cultivo mutado para aumentar su precio en +2 esta ronda:")
                    mutatedCrops.forEach { crop ->
                        Button(onClick = {
                            coroutineScope.launch {
                                gameService.applyMerchantPriceBoost(gameId, currentPlayerId, crop.id)
                                showMerchantBoostDialog = false
                            }
                        }) {
                            Text(crop.nombre)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMerchantBoostDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ChangeSymbolDialog(
    currentDice: List<DadoSimbolo>,
    onDismiss: () -> Unit,
    onSymbolChanged: (Int, DadoSimbolo) -> Unit
) {
    var selectedDiceIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Símbolo de Dado", color = GlitchCyan) },
        text = {
            Column {
                if (selectedDiceIndex == null) {
                    Text("1. Selecciona el dado que quieres cambiar:", color = PixelText)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        currentDice.forEachIndexed { index, symbol ->
                            PixelDice(symbol = symbol, isKept = false, onClick = { selectedDiceIndex = index }, isEnabled = true)
                        }
                    }
                } else {
                    Text("2. Selecciona el nuevo símbolo:", color = PixelText)
                    Spacer(Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(DadoSimbolo.values()) { symbol ->
                            Button(onClick = { onSymbolChanged(selectedDiceIndex!!, symbol) }) {
                                Text(symbol.name.take(3))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GlitchRed)
            }
        },
        containerColor = PixelCard,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.border(BorderStroke(2.dp, GlitchCyan))
    )
}

@Composable
fun MysteryEncounterDialog(
    encounter: MysteryEncounter,
    onChoiceSelected: (String) -> Unit,
    onMinigameResult: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* No se puede cerrar sin elegir */ },
        title = { Text(encounter.title, color = GlitchMagenta, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(encounter.description, color = PixelText)
                if (encounter is MinigameEncounter) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ReactionTimeMinigame(onResult = onMinigameResult)
                }
            }
        },
        confirmButton = {
            when (encounter) {
                is DecisionEncounter -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        encounter.choices.forEach { choice ->
                            Button(
                                onClick = { onChoiceSelected(choice.id) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PixelCard),
                                border = BorderStroke(2.dp, GlitchCyan)
                            ) {
                                Text(choice.text, color = GlitchCyan)
                            }
                        }
                    }
                }
                is RandomEventEncounter -> {
                    Button(
                        onClick = { onChoiceSelected("random_event_continue") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchCyan)
                    ) {
                        Text("VER QUÉ SUCEDE...", color = PixelCard)
                    }
                }
                is MinigameEncounter -> {
                    // No se muestran botones aquí, el propio minijuego maneja la interacción.
                }
            }
        },
        containerColor = PixelCard,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.border(BorderStroke(2.dp, GlitchMagenta)),
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@Composable
fun ReactionTimeMinigame(onResult: (Boolean) -> Unit) {
    var gameState by remember { mutableStateOf("ready") } // ready, running, finished
    val infiniteTransition = rememberInfiniteTransition(label = "minigame_transition")

    val position by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "bar_position"
    )

    LaunchedEffect(Unit) {
        gameState = "running"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(PixelBackground)
                .border(1.dp, GlitchCyan),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(50.dp)
                    .background(GlitchLime.copy(alpha = 0.4f))
            )
            Box(
                Modifier
                    .offset(x = (position * 150).dp - 75.dp)
                    .width(8.dp)
                    .fillMaxHeight()
                    .background(GlitchMagenta)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (gameState == "running") {
                    gameState = "finished"
                    val success = position in 0.4f..0.6f
                    onResult(success)
                }
            },
            enabled = gameState == "running",
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GlitchMagenta)
        ) {
            Text(if (gameState == "running") "¡BLOQUEAR!" else "...", color = PixelText)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlayerActionsScreenPreview() {
    GranjaGlitchAppTheme {
        PlayerActionsScreen(gameId = "ABCDEF", currentPlayerId = "sample-player-id", hasShownTurnPopup = false, onTurnPopupShown = {})
    }
}
