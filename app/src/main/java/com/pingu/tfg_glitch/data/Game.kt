package com.pingu.tfg_glitch.data

import java.util.UUID

/**
 * Data class to represent the overall state of a game.
 * This will be stored as a single document in Firestore.
 */
data class Game(
    val id: String = "",
    val playerIds: MutableList<String> = mutableListOf(),
    var hostPlayerId: String? = null,
    var isStarted: Boolean = false,
    var hasGameEnded: Boolean = false,
    var catastrophesResolved: Int = 0,
    var marketPrices: MarketPrices = initialMarketPrices,
    var lastEvent: GlitchEvent? = null,
    var supplyFailureActive: Boolean = false,
    var signalInterferenceActive: Boolean = false,
    var currentPlayerTurnId: String? = null,
    var roundStartPlayerId: String? = null, // Para corregir el orden de turno
    var roundPhase: String = "PLAYER_ACTIONS",
    val playersFinishedTurn: MutableList<String> = mutableListOf(),
    val playersFinishedMarket: MutableList<String> = mutableListOf(),
    val activeObjectives: MutableList<Objective> = mutableListOf(),
    val claimedObjectivesByPlayer: MutableMap<String, MutableList<String>> = mutableMapOf(),
    val temporaryPriceBoosts: MutableMap<String, Int> = mutableMapOf() // Para habilidad del Comerciante (Global)
)
