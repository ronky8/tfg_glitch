package com.pingu.tfg_glitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.components.PlayerInfoCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.getDiceBackground
import com.pingu.tfg_glitch.ui.theme.getIconForCrop
import com.pingu.tfg_glitch.ui.theme.getIconForDice
import com.pingu.tfg_glitch.ui.theme.getIconForEnergy
import com.pingu.tfg_glitch.ui.theme.getIconForGranjero
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal de Acciones del Jugador ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerActionsScreen(gameId: String, currentPlayerId: String) {
    // --- Estados ---
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    // Estados para la UI
    var showDiceRollAnimation by remember { mutableStateOf(false) }
    var showDiceEffectDialog by remember { mutableStateOf(false) }
    var diceEffectMessage by remember { mutableStateOf("") }
    val keptDiceIndices = remember { mutableStateListOf<Int>() }
    var showMysteryEncounterDialog by remember { mutableStateOf(false) }
    var showMysteryResultDialog by remember { mutableStateOf(false) }
    var showChangeDiceFaceDialog by remember { mutableStateOf(false) }
    var showFarmerSkillsDialog by remember { mutableStateOf(false) }
    var showVisionaryReminderDialog by remember { mutableStateOf(false) }
    var showBotanistReminderDialog by remember { mutableStateOf(false) }


    // Estado derivado para evitar recomposiciones innecesarias
    val playerState by remember(currentPlayer) {
        derivedStateOf {
            currentPlayer?.let {
                PlayerStateSnapshot(
                    granjero = it.granjero,
                    glitchEnergy = it.glitchEnergy,
                    currentDiceRoll = it.currentDiceRoll,
                    rollPhase = it.rollPhase,
                    hasRerolled = it.hasRerolled,
                    haUsadoPasivaIngeniero = it.haUsadoPasivaIngeniero,
                    haUsadoHabilidadActiva = it.haUsadoHabilidadActiva,
                    mysteryButtonsRemaining = it.mysteryButtonsRemaining,
                    activeMysteryId = it.activeMysteryId,
                    lastMysteryResult = it.lastMysteryResult
                )
            }
        }
    }

    val isMyTurn = game?.currentPlayerTurnId == currentPlayerId
    val isPlayerActionsPhase = game?.roundPhase == "PLAYER_ACTIONS"
    val canPerformActions = isMyTurn && isPlayerActionsPhase

    // --- Efectos ---
    LaunchedEffect(playerState?.activeMysteryId, playerState?.lastMysteryResult) {
        showMysteryEncounterDialog = playerState?.activeMysteryId != null
        showMysteryResultDialog = playerState?.lastMysteryResult != null
    }

    // --- UI Principal ---
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (canPerformActions) "Es tu Turno" else "Esperando...",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (canPerformActions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        currentPlayer?.let { player ->
                            Column {
                                PlayerInfoCard(player = player)
                                Spacer(modifier = Modifier.height(8.dp))
                                PlayerFarmerInfo(player = player, onClickSkills = { showFarmerSkillsDialog = true })
                            }
                        }
                    }
                }

                item {
                    CombinedCropSection(
                        enabled = canPerformActions,
                        inventory = currentPlayer?.inventario ?: emptyList(),
                        onHarvest = { crop ->
                            coroutineScope.launch {
                                gameService.addCropToInventory(currentPlayerId, crop.id, crop.nombre, crop.valorVentaBase, crop.pvFinalJuego)
                            }
                        }
                    )
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                item {
                    playerState?.let {
                        DiceSection(
                            playerState = it,
                            canPerformActions = canPerformActions,
                            showDiceRollAnimation = showDiceRollAnimation,
                            keptDiceIndices = keptDiceIndices,
                            onToggleKeepDice = { index ->
                                if (keptDiceIndices.contains(index)) keptDiceIndices.remove(index)
                                else keptDiceIndices.add(index)
                            }
                        )
                    }
                }
            }

            // Botones de acción principales en la parte inferior
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                playerState?.let {
                    ActionButtons(
                        playerState = it,
                        canPerformActions = canPerformActions,
                        onRoll = {
                            coroutineScope.launch {
                                showDiceRollAnimation = true
                                delay(800)
                                gameService.rollDice(currentPlayerId)
                                showDiceRollAnimation = false
                                keptDiceIndices.clear()
                            }
                        },
                        onReroll = {
                            coroutineScope.launch {
                                showDiceRollAnimation = true
                                delay(800)
                                gameService.rerollDice(currentPlayerId, it.currentDiceRoll, keptDiceIndices.toList())
                                showDiceRollAnimation = false
                                keptDiceIndices.clear()
                            }
                        },
                        onConfirm = {
                            coroutineScope.launch {
                                diceEffectMessage = gameService.applyDiceEffects(currentPlayerId, it.currentDiceRoll)
                                showDiceEffectDialog = true
                            }
                        },
                        onStartMystery = { coroutineScope.launch { gameService.startMysteryEncounter(currentPlayerId) } },
                        onEndTurn = { coroutineScope.launch { gameService.advanceTurn(gameId, currentPlayerId) } },
                        onUseEngineerPassive = { dieIndex -> coroutineScope.launch { gameService.usarPasivaIngeniero(currentPlayerId, dieIndex) } },
                        onUseEngineerActive = { showChangeDiceFaceDialog = true },
                        onUseBotanistActive = {
                            coroutineScope.launch {
                                val success = gameService.usarActivableBotanica(currentPlayerId)
                                if (success) showBotanistReminderDialog = true
                            }
                        },
                        onUseVisionaryActive = {
                            coroutineScope.launch {
                                val success = gameService.usarActivableVisionaria(currentPlayerId)
                                if (success) showVisionaryReminderDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // --- Diálogos ---
    if (showDiceEffectDialog) {
        AlertDialog(
            onDismissRequest = { showDiceEffectDialog = false },
            title = { Text("Resumen de la Tirada") },
            text = { Text(diceEffectMessage) },
            confirmButton = { TextButton(onClick = { showDiceEffectDialog = false }) { Text("Aceptar") } }
        )
    }

    if (showChangeDiceFaceDialog && playerState != null) {
        ChangeDiceFaceDialog(
            currentDice = playerState!!.currentDiceRoll,
            onDismiss = { showChangeDiceFaceDialog = false },
            onConfirm = { dieIndex, newSymbol ->
                coroutineScope.launch {
                    gameService.usarActivableIngeniero(currentPlayerId, dieIndex, newSymbol)
                    showChangeDiceFaceDialog = false
                }
            }
        )
    }

    if (showMysteryEncounterDialog && playerState?.activeMysteryId != null) {
        val encounter = allMysteryEncounters.find { it.id == playerState!!.activeMysteryId }
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

    if (showMysteryResultDialog && playerState?.lastMysteryResult != null) {
        AlertDialog(
            onDismissRequest = { coroutineScope.launch { gameService.clearMysteryResult(currentPlayerId) } },
            title = { Text("Resultado del Misterio") },
            text = { Text(playerState!!.lastMysteryResult!!) },
            confirmButton = {
                TextButton(onClick = { coroutineScope.launch { gameService.clearMysteryResult(currentPlayerId) } }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showFarmerSkillsDialog && currentPlayer?.granjero != null) {
        FarmerSkillsDialog(granjero = currentPlayer!!.granjero!!) { showFarmerSkillsDialog = false }
    }

    if (showBotanistReminderDialog) {
        AlertDialog(
            onDismissRequest = { showBotanistReminderDialog = false },
            title = { Text("Habilidad Activa: Botánica Mutante") },
            text = { Text("Elige uno de tus cultivos. Hasta el final del turno, cuenta como si tuviera 2 Marcadores de Crecimiento adicionales para ser cosechado.") },
            confirmButton = {
                TextButton(onClick = { showBotanistReminderDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showVisionaryReminderDialog) {
        AlertDialog(
            onDismissRequest = { showVisionaryReminderDialog = false },
            title = { Text("Habilidad Activa: Visionaria Píxel") },
            text = { Text("Mira las 3 cartas superiores del Mazo Principal. Añade 1 a tu mano y coloca las otras 2 en la parte superior del mazo en el orden que elijas.") },
            confirmButton = {
                TextButton(onClick = { showVisionaryReminderDialog = false }) {
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
private fun PlayerFarmerInfo(player: Player, onClickSkills: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = getIconForGranjero(player.granjero?.id ?: ""),
            contentDescription = "Icono de Granjero",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = player.granjero?.nombre ?: "Sin Granjero",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onClickSkills, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Info, contentDescription = "Ver Habilidades")
        }
    }
}

@Composable
private fun FarmerSkillsDialog(granjero: Granjero, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = granjero.nombre) },
        text = {
            Column {
                Text(
                    text = "Pasiva: ${granjero.habilidadPasiva}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Activable (${granjero.costeActivacion}): ${granjero.habilidadActivable}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun CombinedCropSection(enabled: Boolean, inventory: List<CultivoInventario>, onHarvest: (CartaSemilla) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Inventario y Cosecha",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedCard {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.heightIn(max = 240.dp)
            ) {
                items(allCrops, key = { it.id }) { crop ->
                    val inventoryItem = inventory.find { it.id == crop.id }
                    val quantity = inventoryItem?.cantidad ?: 0
                    CombinedCropItem(
                        crop = crop,
                        quantity = quantity,
                        enabled = enabled,
                        onHarvest = onHarvest
                    )
                }
            }
        }
    }
}

@Composable
private fun CombinedCropItem(
    crop: CartaSemilla,
    quantity: Int,
    enabled: Boolean,
    onHarvest: (CartaSemilla) -> Unit
) {
    ElevatedCard(
        onClick = { onHarvest(crop) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = getIconForCrop(crop.id),
                contentDescription = crop.nombre,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "x$quantity",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DiceSection(
    playerState: PlayerStateSnapshot,
    canPerformActions: Boolean,
    showDiceRollAnimation: Boolean,
    keptDiceIndices: List<Int>,
    onToggleKeepDice: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Sistema de Dados",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDiceRollAnimation) {
                CircularProgressIndicator()
            } else if (playerState.currentDiceRoll.isNotEmpty()) {
                playerState.currentDiceRoll.forEachIndexed { index, symbol ->
                    val isKept = keptDiceIndices.contains(index)
                    DiceView(
                        symbol = symbol,
                        isKept = isKept,
                        onClick = { onToggleKeepDice(index) },
                        isEnabled = playerState.rollPhase == 1 && canPerformActions && !playerState.hasRerolled
                    )
                }
            } else {
                Text(
                    "Tira los dados para empezar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(
    playerState: PlayerStateSnapshot,
    canPerformActions: Boolean,
    onRoll: () -> Unit,
    onReroll: () -> Unit,
    onConfirm: () -> Unit,
    onStartMystery: () -> Unit,
    onEndTurn: () -> Unit,
    onUseEngineerPassive: (Int) -> Unit,
    onUseEngineerActive: () -> Unit,
    onUseBotanistActive: () -> Unit,
    onUseVisionaryActive: () -> Unit
) {
    var showEngineerPassiveReroll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botones de habilidades de los granjeros
        if (canPerformActions && playerState.granjero != null) {
            HabilityButtons(
                playerState = playerState,
                canPerformActions = canPerformActions,
                onUseBotanistActive = onUseBotanistActive,
                onUseVisionaryActive = onUseVisionaryActive,
                onUseEngineerActive = onUseEngineerActive
            )
        }


        AnimatedContent(targetState = playerState.rollPhase, label = "ActionButtonsAnimation") { phase ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (phase) {
                    0 -> {
                        Button(onClick = onRoll, enabled = canPerformActions, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Tirar Dados")
                        }
                    }
                    1 -> {
                        // Botones de reroll normal y confirmar
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onReroll, enabled = canPerformActions && !playerState.hasRerolled, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Relanzar")
                            }
                            Button(onClick = onConfirm, enabled = canPerformActions, modifier = Modifier.weight(1f).height(50.dp)) {
                                Text("Confirmar")
                            }
                        }
                        // Habilidad pasiva del Ingeniero
                        if (playerState.granjero?.id == "ingeniero_glitch") {
                            Button(
                                onClick = { showEngineerPassiveReroll = true },
                                enabled = canPerformActions && !playerState.haUsadoPasivaIngeniero,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar Reroll Pasivo")
                            }
                        }
                    }
                    2 -> {
                        // Botón de resolver misterio (opcional)
                        if (playerState.mysteryButtonsRemaining > 0) {
                            Button(onClick = onStartMystery, enabled = canPerformActions, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                Text("Resolver Misterio (${playerState.mysteryButtonsRemaining})")
                            }
                        }
                        // Botón para terminar el turno (siempre visible en la fase 2)
                        Button(onClick = onEndTurn, enabled = canPerformActions, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("Terminar Turno")
                        }
                    }
                }
            }
        }
    }

    // Diálogo para la pasiva del Ingeniero (no necesita cambios)
    if (showEngineerPassiveReroll) {
        AlertDialog(
            onDismissRequest = { showEngineerPassiveReroll = false },
            title = { Text("Reroll Pasivo") },
            text = {
                Column {
                    Text("Elige un dado para volver a tirar.")
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        playerState.currentDiceRoll.forEachIndexed { index, symbol ->
                            DiceView(symbol, false, true) {
                                onUseEngineerPassive(index)
                                showEngineerPassiveReroll = false
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({ showEngineerPassiveReroll = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HabilityButtons(
    playerState: PlayerStateSnapshot,
    canPerformActions: Boolean,
    onUseBotanistActive: () -> Unit,
    onUseVisionaryActive: () -> Unit,
    onUseEngineerActive: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        playerState.granjero?.let { granjero ->
            val buttonModifier = Modifier.weight(1f)
            when (granjero.id) {
                "botanica_mutante", "visionaria_pixel", "ingeniero_glitch" -> {
                    val (onClick, cost, enabled) = when (granjero.id) {
                        "botanica_mutante" -> Triple(onUseBotanistActive, 2, canPerformActions && playerState.glitchEnergy >= 2 && !playerState.haUsadoHabilidadActiva)
                        "visionaria_pixel" -> Triple(onUseVisionaryActive, 1, canPerformActions && playerState.glitchEnergy >= 1 && !playerState.haUsadoHabilidadActiva)
                        "ingeniero_glitch" -> Triple(onUseEngineerActive, 1, canPerformActions && playerState.glitchEnergy >= 1 && !playerState.haUsadoHabilidadActiva && playerState.rollPhase == 1)
                        else -> Triple({}, 0, false)
                    }

                    val buttonColors = if (granjero.id == "ingeniero_glitch") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()

                    Button(
                        onClick = onClick,
                        enabled = enabled,
                        modifier = buttonModifier,
                        colors = if(granjero.id != "ingeniero_glitch") ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            painter = getIconForGranjero(granjero.id),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Habilidad ($cost")
                            Icon(
                                painter = getIconForEnergy(),
                                contentDescription = "Energía",
                                modifier = Modifier.size(12.dp)
                            )
                            Text(")")
                        }
                    }
                }
            }
        }
    }
}

private data class PlayerStateSnapshot(
    val granjero: Granjero?,
    val glitchEnergy: Int,
    val currentDiceRoll: List<DadoSimbolo>,
    val rollPhase: Int,
    val hasRerolled: Boolean,
    val haUsadoPasivaIngeniero: Boolean,
    val haUsadoHabilidadActiva: Boolean,
    val mysteryButtonsRemaining: Int,
    val activeMysteryId: String?,
    val lastMysteryResult: String?
)

@Composable
fun DiceView(symbol: DadoSimbolo, isKept: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val cardColors = if (isKept) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = Color.Transparent)
    }

    Card(
        modifier = Modifier.size(60.dp),
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(8.dp),
        colors = cardColors,
        border = if (isKept) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Capa de Fondo
            Icon(
                painter = getDiceBackground(),
                contentDescription = null, // El fondo es decorativo
                modifier = Modifier.fillMaxSize(),
                tint = if (isKept) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
            // Capa del Símbolo
            Icon(
                painter = getIconForDice(symbol),
                contentDescription = symbol.name,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurface // Un color neutro que funciona bien sobre el fondo
            )
        }
    }
}

@Composable
fun ChangeDiceFaceDialog(
    currentDice: List<DadoSimbolo>,
    onDismiss: () -> Unit,
    onConfirm: (dieIndex: Int, newSymbol: DadoSimbolo) -> Unit
) {
    var selectedDieIndex by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habilidad: Forzar Resultado") },
        text = {
            Column {
                val index = selectedDieIndex
                if (index == null) {
                    Text("1. Selecciona el dado que quieres cambiar:")
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        currentDice.forEachIndexed { i, symbol ->
                            DiceView(symbol, false, true) { selectedDieIndex = i }
                        }
                    }
                } else {
                    Text("2. Selecciona la nueva cara para el dado:")
                    Spacer(Modifier.height(16.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DadoSimbolo.values()) { symbol ->
                            DiceView(symbol, false, true) { onConfirm(index, symbol) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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
        title = { Text(encounter.title) },
        text = {
            Column {
                Text(encounter.description, style = MaterialTheme.typography.bodyMedium)
                if (encounter is MinigameEncounter) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // [NUEVO] Selector de minijuegos
                    when (encounter.minigameType) {
                        "reaction_time" -> ReactionTimeMinigame(onResult = onMinigameResult)
                        "rapid_tap" -> RapidTapMinigame(onResult = onMinigameResult)
                        "memory_sequence" -> MemorySequenceMinigame(onResult = onMinigameResult)
                        "timing_challenge" -> TimingChallengeMinigame(onResult = onMinigameResult)
                        "code_breaking" -> CodeBreakingMinigame(onResult = onMinigameResult)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (encounter) {
                    is DecisionEncounter -> {
                        encounter.choices.forEach { choice ->
                            Button(onClick = { onChoiceSelected(choice.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(choice.text)
                            }
                        }
                    }
                    is RandomEventEncounter -> {
                        Button(onClick = { onChoiceSelected("random_event_continue") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Ver qué sucede...")
                        }
                    }
                    is MinigameEncounter -> {}
                }
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@Composable
fun ReactionTimeMinigame(onResult: (Boolean) -> Unit) {
    var gameState by remember { mutableStateOf("ready") }
    val infiniteTransition = rememberInfiniteTransition("minigame_transition")
    // Dificultad aumentada: la barra se mueve más rápido
    val position by infiniteTransition.animateFloat(
        initialValue = -0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "bar_position"
    )
    LaunchedEffect(Unit) { gameState = "running" }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Dificultad aumentada: la zona de acierto es más pequeña
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.1f).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
            Box(Modifier.fillMaxHeight().width(8.dp).align(Alignment.Center).offset(x = (position * 150).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (gameState == "running") {
                    gameState = "finished"
                    // Dificultad aumentada: la condición de éxito es más estricta
                    val success = position in -0.05f..0.05f
                    onResult(success)
                }
            },
            enabled = gameState == "running",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (gameState == "running") "¡DETENER!" else "...")
        }
    }
}

@Composable
fun RapidTapMinigame(onResult: (Boolean) -> Unit) {
    val gameDuration = 3500L // 3.5 segundos
    val targetTaps = 25 // 25 pulsaciones
    var taps by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(gameDuration / 1000 + 1) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < gameDuration) {
            delay(100L)
            val elapsed = System.currentTimeMillis() - startTime
            timeLeft = (gameDuration - elapsed) / 1000 + 1
        }
        if (!isFinished) {
            isFinished = true
            onResult(taps >= targetTaps)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("¡Pulsa el botón!", style = MaterialTheme.typography.titleMedium)
        Text("Objetivo: $targetTaps | Tiempo: $timeLeft s", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { (taps.toFloat() / targetTaps).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (!isFinished) taps++ },
            modifier = Modifier.fillMaxWidth().height(80.dp),
            enabled = !isFinished
        ) {
            Text(if (!isFinished) "¡PULSA! ($taps)" else "¡TIEMPO!", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun MemorySequenceMinigame(onResult: (Boolean) -> Unit) {
    val sequenceLength = 4
    val sequence = remember { List(sequenceLength) { DadoSimbolo.values().random() } }
    val playerInput = remember { mutableStateListOf<DadoSimbolo>() }
    var gameState by remember { mutableStateOf("showing") }
    var currentStepShown by remember { mutableStateOf(-1) }

    LaunchedEffect(gameState) {
        if (gameState == "showing") {
            delay(500) // Pausa inicial
            for (i in sequence.indices) {
                currentStepShown = i
                delay(600)
                currentStepShown = -1
                delay(300)
            }
            gameState = "playing"
        }
    }

    LaunchedEffect(playerInput.size) {
        if (gameState == "playing" && playerInput.isNotEmpty()) {
            val lastIndex = playerInput.lastIndex
            if (playerInput[lastIndex] != sequence[lastIndex]) {
                gameState = "result"
                onResult(false)
            } else if (playerInput.size == sequence.size) {
                gameState = "result"
                onResult(true)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val statusText = when(gameState) {
            "showing" -> "Memoriza la secuencia..."
            "playing" -> "Tu turno: ${playerInput.size}/${sequence.size}"
            else -> if (playerInput.toList() == sequence) "¡Correcto!" else "¡Fallaste!"
        }
        Text(statusText, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            (0 until sequenceLength).forEach { index ->
                val symbol = if (gameState == "showing" && currentStepShown == index) sequence[index] else null
                val inputSymbol = if(gameState == "playing") playerInput.getOrNull(index) else null

                val color by animateColorAsState(
                    targetValue = if (symbol != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "sequence_color_$index"
                )
                Box(
                    modifier = Modifier.size(40.dp).background(color, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (symbol != null) { // CORRECCIÓN: Mostrar el icono durante la fase de "showing"
                        Icon(painter = getIconForDice(symbol), contentDescription = null, modifier = Modifier.size(24.dp))
                    } else if (inputSymbol != null) { // Mantener el icono del input del jugador
                        Icon(painter = getIconForDice(inputSymbol), contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(DadoSimbolo.values()) { simbolo ->
                Button(
                    onClick = { if (gameState == "playing") playerInput.add(simbolo) },
                    enabled = gameState == "playing",
                    modifier = Modifier.size(60.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(painter = getIconForDice(simbolo), contentDescription = simbolo.name, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun TimingChallengeMinigame(onResult: (Boolean) -> Unit) {
    var gameState by remember { mutableStateOf("ready") }
    var targetPosition by remember { mutableStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "timing_challenge_transition")
    val position by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_position"
    )

    LaunchedEffect(Unit) {
        targetPosition = Random.nextFloat() * 1.4f - 0.7f // Zona aleatoria entre -0.7 y 0.7
        gameState = "running"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("¡Sincroniza el Pulso!", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.1f)
                .align(BiasAlignment(horizontalBias = targetPosition, verticalBias = 0f))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            Box(Modifier
                .size(15.dp)
                .align(BiasAlignment(horizontalBias = position, verticalBias = 0f))
                .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (gameState == "running") {
                    gameState = "finished"
                    val success = position in (targetPosition - 0.1f)..(targetPosition + 0.1f) // A slightly larger success zone for playability
                    onResult(success)
                }
            },
            enabled = gameState == "running",
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (gameState == "running") "¡SINCRONIZAR!" else "...")
        }
    }
}

@Composable
fun CodeBreakingMinigame(onResult: (Boolean) -> Unit) {
    val code = remember { (1..3).map { Random.nextInt(1, 10) }.joinToString("") }
    var playerInput by remember { mutableStateOf("") }
    var showCode by remember { mutableStateOf(true) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500) // Muestra el código por 1.5 segundos
        showCode = false
    }

    LaunchedEffect(playerInput) {
        if (playerInput.length == 3) {
            isFinished = true
            onResult(playerInput == code)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Introduce el código", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val displayText = when {
                showCode -> code
                isFinished && playerInput == code -> "¡CORRECTO!"
                isFinished -> "¡ERROR!"
                else -> playerInput.padEnd(3, '_')
            }
            Text(displayText, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false,
            modifier = Modifier.width(200.dp)
        ) {
            items((1..9).toList()) { number ->
                Button(
                    onClick = { if (playerInput.length < 3) playerInput += number.toString() },
                    enabled = !showCode && !isFinished
                ) {
                    Text(number.toString())
                }
            }
        }
    }
}


// ========================================================================
// --- Preview ---
// ========================================================================

@Preview(showBackground = true)
@Composable
fun PlayerActionsScreenPreview() {
    GranjaGlitchAppTheme {
        PlayerActionsScreen(gameId = "ABCDEF", currentPlayerId = "sample-player-id")
    }
}

