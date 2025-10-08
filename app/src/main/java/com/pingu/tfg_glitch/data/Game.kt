package com.pingu.tfg_glitch.data

data class Game(
    val id: String = "",
    val playerIds: MutableList<String> = mutableListOf(),
    var hostPlayerId: String? = null,
    var isStarted: Boolean = false,
    var hasGameEnded: Boolean = false,
    var marketPrices: MarketPrices = initialMarketPrices,
    var lastEvent: GlitchEvent? = null,
    var supplyFailureActive: Boolean = false,
    var signalInterferenceActive: Boolean = false,
    var currentPlayerTurnId: String? = null, //
    var roundPhase: String = "PLAYER_ACTIONS",
    val playersFinishedTurn: MutableList<String> = mutableListOf(),
    val playersFinishedMarket: MutableList<String> = mutableListOf(),
    val activeObjectives: MutableList<Objective> = mutableListOf(),
    val claimedObjectivesByPlayer: MutableMap<String, String> = mutableMapOf(),
    var playerOrder: MutableList<String> = mutableListOf(),
    var roundNumber: Int = 0,
    var roundObjective: Objective? = null
)
