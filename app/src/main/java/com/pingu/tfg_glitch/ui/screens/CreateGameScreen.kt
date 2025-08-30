package com.pingu.tfg_glitch.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Granjero
import com.pingu.tfg_glitch.data.allGranjeros
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.launch

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGameScreen(
    onGameCreated: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var hostNameInput by remember { mutableStateOf("") }
    var selectedGranjeroId by remember { mutableStateOf<String?>(null) } // ¡NUEVO!
    var generatedGameCode by remember { mutableStateOf<String?>(null) }
    var generatedHostPlayerId by remember { mutableStateOf<String?>(null) }
    var showGameCodeDisplay by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Crear Partida") },
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
            AnimatedContent(targetState = showGameCodeDisplay, label = "CreateGameAnimation") { screenVisible ->
                if (!screenVisible) {
                    // Estado inicial: pedir nombre y granjero
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Elige tu nombre", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 24.dp))

                        OutlinedTextField(
                            value = hostNameInput,
                            onValueChange = { hostNameInput = it },
                            label = { Text("Nombre del Anfitrión") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Elige tu Granjero", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        GranjeroSelector(
                            granjeros = allGranjeros, // Como es el host, todos están disponibles
                            selectedId = selectedGranjeroId,
                            onSelect = { selectedGranjeroId = it }
                        )

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
                                        val (newGameId, hostPlayerId) = gameService.createGame(hostNameInput, selectedGranjeroId!!)
                                        generatedGameCode = newGameId
                                        generatedHostPlayerId = hostPlayerId
                                        showGameCodeDisplay = true
                                    } catch (e: Exception) {
                                        errorMessage = "Error al crear la partida."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isLoading && hostNameInput.isNotBlank() && selectedGranjeroId != null,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                Text("Crear Partida", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Estado final: mostrar código de partida
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Comparte el código",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 24.dp),
                            textAlign = TextAlign.Center
                        )

                        generatedGameCode?.let { code ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Código de Partida:",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Game Code", code)
                                            clipboard.setPrimaryClip(clip)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("¡Código copiado!")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copiar Código"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    generatedHostPlayerId?.let { playerId ->
                                        onGameCreated(code, playerId)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "¡Empezar!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
            Text("Cargando granjeros...")
        } else {
            granjeros.forEach { granjero ->
                val isSelected = granjero.id == selectedId
                val icon = getIconForGranjero(granjero.iconName)
                val colors = if (isSelected) {
                    IconButtonDefaults.filledIconButtonColors()
                } else {
                    IconButtonDefaults.outlinedIconButtonColors()
                }

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
fun CreateGameScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CreateGameScreen(onGameCreated = { _, _ -> }, onBack = {})
        }
    }
}
