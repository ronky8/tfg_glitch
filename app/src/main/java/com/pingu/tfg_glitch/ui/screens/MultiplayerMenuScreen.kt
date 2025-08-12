package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerMenuScreen(
    onBack: () -> Unit,
    onStartMultiplayerGame: () -> Unit,
    onJoinMultiplayerGame: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Multijugador") }, // Título simplificado
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Modo Multijugador",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = AccentYellow,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Spacer(modifier = Modifier.height(24.dp)) // Espaciado mejorado
            Button(
                onClick = onStartMultiplayerGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Crear Partida", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onJoinMultiplayerGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Text(text = "Unirse a Partida", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MultiplayerMenuScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MultiplayerMenuScreen(onBack = {}, onStartMultiplayerGame = {}, onJoinMultiplayerGame = {})
        }
    }
}
