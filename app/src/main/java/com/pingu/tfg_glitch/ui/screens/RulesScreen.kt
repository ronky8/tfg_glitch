package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // ¡IMPORTACIÓN AÑADIDA!
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme

// --- Data Structure for Rules ---
private data class RuleSection(val title: String, val content: List<String>)

// --- Game Manual Content ---
private val gameManual = listOf(
    RuleSection(
        "1. Introducción al Juego", listOf(
            "¡Bienvenido a Granja Glitch! En este juego, encarnas a un granjero que ha heredado una parcela de tierra afectada por extraños 'glitches'. Tu objetivo es dominar este caos, manipulando el volátil mercado para convertir los fallos digitales en una fortuna.",
            "Número de Jugadores: 2-4",
            "Duración: 30-60 minutos",
            "Edad: 10+",
            "Objetivo del Juego: Acumular la mayor cantidad de Puntos de Victoria (PV) al final de la partida."
        )
    ),
    RuleSection(
        "2. Componentes del Juego", listOf(
            "4 Cartas de Granjero, Mazo Principal (Semillas y Catástrofes), Mazo de Mejoras Glitch, Dados Personalizados (4 por jugador), Tokens (Monedas, Energía, Crecimiento, Cultivos) y la Aplicación Complementaria."
        )
    ),
    RuleSection(
        "3. Preparación", listOf(
            "1. Configurar la App: Inicia una nueva partida en la app.",
            "2. Elegir Granjero: Cada jugador elige una Carta de Granjero.",
            "3. Preparar Mazos: Baraja el mazo de Mejoras. Mezcla las Catástrofes con las Semillas para crear el Mazo Principal. Crea un mercado inicial de 3 cartas de Semilla.",
            "4. Repartir Recursos: Cada jugador recibe 5 Monedas, 1 Energía, 4 Dados y una mano inicial de 5 cartas.",
            "5. Primer Jugador: Se determina aleatoriamente o por la regla de 'quien haya regado una planta más recientemente'."
        )
    ),
    RuleSection(
        "4. Flujo de Juego", listOf(
            "El juego se desarrolla en rondas, cada una con dos fases: Fase de Acciones y Fase de Mercado Glitch.",
            "4.1. Fase de Acciones: Por turnos, cada jugador:",
            "- Roba 1 carta (si es Catástrofe, la resuelve y roba otra).",
            "- Añade 1 marcador de crecimiento a cada uno de sus cultivos plantados.",
            "- Tira sus 4 dados (puede relanzar una vez).",
            "- Usa los símbolos de los dados para realizar acciones: 🌱 Plantar, ➕ Añadir Crecimiento, 💰 Ganar Monedas, ⚡ Ganar Energía, ❓ Activar Misterio en la app, 🌀 Robar Mejora Glitch.",
            "- Puede usar la habilidad activable de su Granjero una vez por turno.",
            "4.2. Fase de Mercado Glitch: Una vez todos han actuado:",
            "- La app actualiza los precios del mercado.",
            "- Por orden de turno, los jugadores venden sus cultivos cosechados y reclaman objetivos en la app."
        )
    ),
    RuleSection(
        "5. Fin de la Partida", listOf(
            "La partida termina cuando se resuelve un número determinado de Catástrofes (6 para 2 jugadores, 7 para 3, 8 para 4), pero no antes de completar 8 rondas.",
            "Cuando se cumple la condición, se termina la ronda actual y se juega una última ronda completa."
        )
    ),
    RuleSection(
        "6. Puntuación Final", listOf(
            "Los Puntos de Victoria (PV) se calculan a partir de:",
            "- Objetivos de la App.",
            "- 1 PV por cada 5 Monedas.",
            "- PV por tokens de Cultivo no vendidos (por definir).",
            "- PV de Habilidades Especiales.",
            "En caso de empate, gana el jugador con más Energía Glitch."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reglas del Juego") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al menú principal"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Manual de Granja Glitch",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
            }
            items(gameManual) { section ->
                RuleSectionItem(section)
            }
        }
    }
}

@Composable
private fun RuleSectionItem(section: RuleSection) {
    Column {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        for (paragraph in section.content) {
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RulesScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            RulesScreen(onBack = {})
        }
    }
}

