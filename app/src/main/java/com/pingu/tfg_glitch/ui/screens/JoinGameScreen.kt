package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Granjero
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGameScreen(
    onGameJoined: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var gameCodeInput by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("") }
    var selectedGranjeroId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // CORRECCIÓN: Se añaden estas dos variables
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isCodeValid = gameCodeInput.length == 6

    // Obtiene los granjeros disponibles solo cuando el código es válido
    val availableGranjeros by produceState<List<Granjero>>(initialValue = emptyList(), gameCodeInput) {
        if (isCodeValid) {
            gameService.getAvailableGranjeros(gameCodeInput).collect { granjeros ->
                value = granjeros
            }
        } else {
            value = emptyList()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // CORRECCIÓN: Se añade el snackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Unirse a Partida") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Introduce los datos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(
                value = gameCodeInput,
                onValueChange = { gameCodeInput = it.take(6).uppercase() },
                label = { Text("Código de Partida (6 caracteres)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage != null
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                label = { Text("Tu Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text("Elige tu Granjero", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            if (isCodeValid) {
                GranjeroSelector(
                    granjeros = availableGranjeros,
                    selectedId = selectedGranjeroId,
                    onSelect = { selectedGranjeroId = it }
                )
            } else {
                Text("Introduce un código de partida válido para ver los granjeros.", style = MaterialTheme.typography.bodySmall)
            }


            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val playerId = gameService.addPlayerToGameByName(gameCodeInput, playerNameInput, selectedGranjeroId!!)
                            onGameJoined(gameCodeInput, playerId)
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Error al unirse."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && isCodeValid && playerNameInput.isNotBlank() && selectedGranjeroId != null,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Unirse a la Aventura", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GranjeroSelector(
    granjeros: List<Granjero>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        if (granjeros.isEmpty()) {
            Text("Cargando...")
        } else {
            granjeros.forEach { granjero ->
                val isSelected = granjero.id == selectedId
                val icon = getIconForGranjero(granjero.iconName)
                val colors = if (isSelected) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.outlinedIconButtonColors()

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(granjero.nombre) } },
                    state = rememberTooltipState()
                ) {
                    IconButton(
                        onClick = { onSelect(granjero.id) },
                        modifier = Modifier.size(64.dp),
                        colors = colors
                    ) {
                        Icon(icon, contentDescription = granjero.nombre, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun getIconForGranjero(iconName: String): ImageVector {
    return when (iconName) {
        "engineering" -> Icons.Default.Engineering
        "local_florist" -> Icons.Default.LocalFlorist
        "storefront" -> Icons.Default.Storefront
        "visibility" -> Icons.Default.Visibility
        else -> Icons.Default.HelpOutline
    }
}

@Preview(showBackground = true)
@Composable
fun JoinGameScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            JoinGameScreen(onGameJoined = { _, _ -> }, onBack = {})
        }
    }
}
