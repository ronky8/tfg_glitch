package com.pingu.tfg_glitch.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.components.PlayerInfoCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    // Nuevo estado para el diálogo de la Visionaria Píxel
    var showVisionaryReminderDialog by remember { mutableStateOf(false) }

    // Nuevo estado para el diálogo de la Botánica Mutante
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
                            PlayerInfoCardWithFarmer(player = player, onClickSkills = { showFarmerSkillsDialog = true })
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
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                                // Intentamos usar la habilidad, la lógica de coste y uso se maneja en GameService
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

    // Diálogo para la habilidad de la Botánica Mutante
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

    // Diálogo para la habilidad de la Visionaria Píxel
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

/**
 * [MODIFICADO] Combina PlayerInfoCard con el nombre del granjero y un botón de habilidades.
 */
@Composable
private fun PlayerInfoCardWithFarmer(player: Player, onClickSkills: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila principal con nombre del jugador y recursos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${player.money}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(Icons.Default.Paid, contentDescription = "Monedas", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${player.glitchEnergy}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Icon(Icons.Default.Bolt, contentDescription = "Energía Glitch", tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Fila con el nombre del granjero y el botón de habilidades
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    }
}

/**
 * [¡NUEVO!] Muestra la información del Granjero actual y sus habilidades en un diálogo.
 */
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

/**
 * [MODIFICADO] Muestra una parrilla de cultivos que se pueden "cosechar" (añadir al inventario).
 */
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

/**
 * [NUEVO] Componente que combina la visualización del inventario y la acción de cosecha.
 */
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
            Text(
                text = crop.nombre.first().toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
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

/**
 * Muestra los dados del jugador y su estado.
 */
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

/**
 * [MODIFICADO] Muestra los botones de acción principales según la fase de la tirada.
 * Se añaden los botones para las habilidades activas de la Botánica y la Visionaria.
 */
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
        if (canPerformActions && (playerState.granjero?.id == "botanica_mutante" || playerState.granjero?.id == "visionaria_pixel")) {
            HabilityButtons(playerState, canPerformActions, onUseBotanistActive, onUseVisionaryActive)
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
                        // Habilidades del Ingeniero
                        if (playerState.granjero?.id == "ingeniero_glitch") {
                            Button(
                                onClick = { showEngineerPassiveReroll = true },
                                enabled = canPerformActions && !playerState.haUsadoPasivaIngeniero,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar Reroll Pasivo")
                            }
                            Button(
                                onClick = onUseEngineerActive,
                                enabled = canPerformActions && playerState.glitchEnergy >= 1 && !playerState.haUsadoHabilidadActiva,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Activar Habilidad (1⚡)")
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

/**
 * [NUEVO] Componente auxiliar para agrupar los botones de habilidades activas.
 */
@Composable
private fun HabilityButtons(
    playerState: PlayerStateSnapshot,
    canPerformActions: Boolean,
    onUseBotanistActive: () -> Unit,
    onUseVisionaryActive: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Habilidad de la Botánica Mutante
        if (playerState.granjero?.id == "botanica_mutante") {
            OutlinedButton(
                onClick = onUseBotanistActive,
                enabled = canPerformActions && playerState.glitchEnergy >= 2 && !playerState.haUsadoHabilidadActiva,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.LocalFlorist, contentDescription = "Activar Habilidad Botánica", modifier = Modifier.size(18.dp))
                Text("Activar (2⚡)", style = MaterialTheme.typography.bodySmall)
            }
        }
        // Habilidad de la Visionaria Píxel
        if (playerState.granjero?.id == "visionaria_pixel") {
            OutlinedButton(
                onClick = onUseVisionaryActive,
                enabled = canPerformActions && playerState.glitchEnergy >= 1 && !playerState.haUsadoHabilidadActiva,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = "Activar Habilidad Visionaria", modifier = Modifier.size(18.dp))
                Text("Activar (1⚡)", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Un `data class` para crear una instantánea del estado del jugador.
 */
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

/**
 * Muestra una representación visual de un dado.
 */
@Composable
fun DiceView(symbol: DadoSimbolo, isKept: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    val (icon, color, contentDescription) = when (symbol) {
        DadoSimbolo.GLITCH -> Triple(Icons.Default.Style, MaterialTheme.colorScheme.error, "Robar Carta")
        DadoSimbolo.CRECIMIENTO -> Triple(Icons.Default.Add, MaterialTheme.colorScheme.tertiary, "Crecimiento")
        DadoSimbolo.ENERGIA -> Triple(Icons.Default.Bolt, MaterialTheme.colorScheme.secondary, "Energía")
        DadoSimbolo.MONEDA -> Triple(Icons.Default.Paid, MaterialTheme.colorScheme.primary, "Moneda")
        DadoSimbolo.MISTERIO -> Triple(Icons.AutoMirrored.Filled.Help, MaterialTheme.colorScheme.tertiary, "Misterio")
        DadoSimbolo.PLANTAR -> Triple(Icons.Default.Spa, MaterialTheme.colorScheme.primary, "Plantar")
    }

    val cardVariant = if (isKept) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.outlinedCardColors()
    }

    Card(
        modifier = Modifier.size(60.dp),
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(8.dp),
        colors = cardVariant,
        border = if (isKept) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = color, modifier = Modifier.size(32.dp))
        }
    }
}

/**
 * [¡NUEVO!] Diálogo para la habilidad activa del Ingeniero: cambiar la cara de un dado.
 */
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

/**
 * Diálogo para mostrar los encuentros de misterio.
 */
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
                    ReactionTimeMinigame(onResult = onMinigameResult)
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

/**
 * Minijuego de tiempo de reacción.
 */
@Composable
fun ReactionTimeMinigame(onResult: (Boolean) -> Unit) {
    var gameState by remember { mutableStateOf("ready") }
    val infiniteTransition = rememberInfiniteTransition("minigame_transition")
    val position by infiniteTransition.animateFloat(
        initialValue = -0.45f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "bar_position"
    )
    LaunchedEffect(Unit) { gameState = "running" }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.2f).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
            Box(Modifier.fillMaxHeight().width(8.dp).align(Alignment.Center).offset(x = (position * 150).dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (gameState == "running") {
                gameState = "finished"
                val success = position in -0.1f..0.1f
                onResult(success)
            }
        }, enabled = gameState == "running", modifier = Modifier.fillMaxWidth()) {
            Text(if (gameState == "running") "¡DETENER!" else "...")
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
