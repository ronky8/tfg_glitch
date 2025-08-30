package com.pingu.tfg_glitch.data

import java.util.UUID

/**
 * Data class to represent the overall state of a game.
 * This will be stored as a single document in Firestore.
 */
data class Game(
    val id: String = "", // Ahora el ID se genera en GameService para ser de 6 caracteres
    val playerIds: MutableList<String> = mutableListOf(), // IDs of players in this game
    var hostPlayerId: String? = null, // ID of the player who created the game
    var isStarted: Boolean = false,
    var hasGameEnded: Boolean = false,
    var marketPrices: MarketPrices = initialMarketPrices, // ¡Ahora usa initialMarketPrices del archivo consolidado GameData.kt!
    var lastEvent: GlitchEvent? = null, // ¡Ahora usa GlitchEvent del archivo consolidado GameData.kt!
    var supplyFailureActive: Boolean = false, // Persistent effect from "Fallo de Suministro"
    var signalInterferenceActive: Boolean = false, // Temporary effect from "Interferencia de Señal"
    var currentPlayerTurnId: String? = null, // ID del jugador cuyo turno es actual
    var roundPhase: String = "PLAYER_ACTIONS", // Fase actual de la ronda: "PLAYER_ACTIONS", "MARKET_PHASE"
    val playersFinishedTurn: MutableList<String> = mutableListOf(), // Jugadores que han terminado su fase de acciones
    val playersFinishedMarket: MutableList<String> = mutableListOf(), // Jugadores que han terminado su fase de mercado
    val activeObjectives: MutableList<Objective> = mutableListOf(), // Objetivos activos en esta partida
    val claimedObjectivesByPlayer: MutableMap<String, String> = mutableMapOf(), // CORREGIDO: Ahora almacena el ID del jugador que lo reclamó
    var playerOrder: MutableList<String> = mutableListOf(), // [¡NUEVO!] Orden de los jugadores
    var roundNumber: Int = 0,
    var roundObjective: Objective? = null // ¡CORREGIDO! Nuevo objetivo de ronda rotatorio.
)
