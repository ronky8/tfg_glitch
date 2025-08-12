package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

// Composable para la pantalla principal del menú
@Composable
fun MainMenuScreen(
    onStartGame: () -> Unit,
    onViewRules: () -> Unit,
    onOneMobileMode: () -> Unit // Nuevo callback para el modo un móvil
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Granja Glitch",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = AccentYellow,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(text = "Empezar Partida Multijugador", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOneMobileMode, // Botón para el modo un móvil
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(text = "Modo Un Móvil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onViewRules,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(text = "Ver Reglas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MainMenuScreen(onStartGame = {}, onViewRules = {}, onOneMobileMode = {})
        }
    }
}
