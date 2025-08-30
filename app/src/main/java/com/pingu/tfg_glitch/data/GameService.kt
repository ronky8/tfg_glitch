package com.pingu.tfg_glitch.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.delay
import android.util.Log
import com.google.firebase.firestore.Transaction
import kotlin.random.Random
import kotlin.math.max

/**
 * Service for managing multiplayer game logic.
 */
class GameService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val gamesCollection = db.collection("games")
    private val playersCollection = db.collection("players")

    /**
     * Generates a random 6-character alphanumeric code for the game.
     */
    private fun generateGameCode(): String {
        val charPool: List<Char> = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    /**
     * [¡NUEVO!] Obtiene la lista de granjeros que aún no han sido elegidos en una partida.
     */
    fun getAvailableGranjeros(gameId: String): Flow<List<Granjero>> = callbackFlow {
        val gameRef = gamesCollection.document(gameId)
        // Usamos una corrutina para esperar el resultado de la llamada inicial
        try {
            val gameSnapshot = gameRef.get().await()

            if (!gameSnapshot.exists()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            // Creamos un listener que se mantiene abierto hasta que el Flow se cancele
            val listener = playersCollection.whereEqualTo("gameId", gameId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(
                            "GameService",
                            "Error listening to players in game $gameId: ${error.message}",
                            error
                        )
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val assignedGranjeroIds = snapshot.documents.mapNotNull {
                            it.toObject<Player>()?.granjero?.id
                        }
                        val available = allGranjeros.filter { it.id !in assignedGranjeroIds }
                        trySend(available)
                    }
                }
            awaitClose { listener.remove() }

        } catch (e: Exception) {
            Log.e(
                "GameService",
                "Error getting available granjeros for game $gameId: ${e.message}",
                e
            )
            close(e)
        }
    }


    /**
     * [MODIFICADO] Creates a new game in Firestore and returns the game code and the host's player ID.
     */
    suspend fun createGame(hostName: String, granjeroId: String): Pair<String, String> {
        val gameId = generateGameCode()
        val selectedObjectives = allObjectives.shuffled(Random).take(3).toMutableList()
        val newGame = Game(
            id = gameId,
            activeObjectives = selectedObjectives,
            marketPrices = initialMarketPrices
        )
        gamesCollection.document(newGame.id).set(newGame).await()

        val hostDocRef = playersCollection.document()
        val hostPlayerId = hostDocRef.id

        val hostGranjero = allGranjeros.find { it.id == granjeroId }
            ?: throw Exception("Granjero with ID $granjeroId not found.")

        val hostPlayer = Player(
            id = hostPlayerId,
            gameId = newGame.id,
            name = hostName,
            money = 5,
            glitchEnergy = 1,
            granjero = hostGranjero
        )
        hostDocRef.set(hostPlayer).await()

        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(newGame.id)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null) {
                game.playerIds.add(hostPlayerId)
                game.hostPlayerId = hostPlayerId
                game.currentPlayerTurnId = hostPlayerId
                game.roundPhase = "PLAYER_ACTIONS"
                transaction.set(gameRef, game)
            }
        }.await()

        return Pair(newGame.id, hostPlayerId)
    }

    /**
     * Creates a new game and a single player for "One Mobile Mode".
     */
    suspend fun createOneMobileGame(): Pair<String, String> {
        val gameId = generateGameCode()
        val selectedObjectives = allObjectives.shuffled(Random).take(3).toMutableList()
        val newGame = Game(
            id = gameId,
            activeObjectives = selectedObjectives,
            marketPrices = initialMarketPrices
        )
        gamesCollection.document(newGame.id).set(newGame).await()

        val playerDocRef = playersCollection.document()
        val playerId = playerDocRef.id
        val oneMobilePlayer = Player(
            id = playerId,
            gameId = newGame.id,
            name = "Director de Juego",
            money = 5,
            glitchEnergy = 1,
            granjero = allGranjeros.random()
        )
        playerDocRef.set(oneMobilePlayer).await()

        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(newGame.id)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null) {
                game.playerIds.add(playerId)
                game.hostPlayerId = playerId
                game.currentPlayerTurnId = playerId
                game.roundPhase = "PLAYER_ACTIONS"
                transaction.set(gameRef, game)
            }
        }.await()

        return Pair(newGame.id, playerId)
    }


    /**
     * Gets the real-time game state.
     */
    fun getGame(gameId: String): Flow<Game?> = callbackFlow {
        val subscription = gamesCollection.document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("GameService", "Error listening to game $gameId: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                val game = snapshot?.toObject<Game>()
                trySend(game)
            }
        awaitClose { subscription.remove() }
    }

    /**
     * Adds a player to an existing game.
     */
    suspend fun addPlayerToGame(gameId: String, playerId: String) {
        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(gameId)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null && !game.playerIds.contains(playerId)) {
                game.playerIds.add(playerId)
                transaction.set(gameRef, game)
            }
        }.await()
    }

    /**
     * [MODIFICADO] Adds a new player to the game by name and returns the newly generated player ID.
     */
    suspend fun addPlayerToGameByName(
        gameId: String,
        playerName: String,
        granjeroId: String
    ): String {
        val gameDoc = gamesCollection.document(gameId).get().await()
        if (!gameDoc.exists()) {
            throw Exception("La partida con el código $gameId no existe.")
        }
        val game = gameDoc.toObject<Game>()
        if (game?.isStarted == true) {
            throw Exception("La partida ya ha comenzado.")
        }
        if (game?.playerIds?.size ?: 0 >= 4) {
            throw Exception("La partida está llena.")
        }

        val selectedGranjero = allGranjeros.find { it.id == granjeroId }
            ?: throw Exception("El granjero seleccionado no es válido.")

        val newPlayerDocRef = playersCollection.document()
        val newPlayerId = newPlayerDocRef.id
        val newPlayer = Player(
            id = newPlayerId,
            gameId = gameId,
            name = playerName,
            money = 5,
            glitchEnergy = 1,
            granjero = selectedGranjero
        )
        newPlayerDocRef.set(newPlayer).await()
        gamesCollection.document(gameId).update("playerIds", FieldValue.arrayUnion(newPlayerId))
            .await()
        return newPlayerId
    }

    /**
     * Starts a game.
     */
    suspend fun startGame(gameId: String) {
        gamesCollection.document(gameId).update("isStarted", true).await()
    }

    /**
     * Updates the market prices, the last event, and event effects of a game.
     */
    suspend fun updateGameMarketAndEvent(
        gameId: String,
        marketPrices: MarketPrices,
        lastEvent: GlitchEvent?,
        supplyFailureActive: Boolean,
        signalInterferenceActive: Boolean
    ) {
        val gameRef = gamesCollection.document(gameId)
        gameRef.update(
            mapOf(
                "marketPrices" to marketPrices,
                "lastEvent" to lastEvent,
                "supplyFailureActive" to supplyFailureActive,
                "signalInterferenceActive" to signalInterferenceActive
            )
        ).await()
    }

    /**
     * Marks a game as ended in Firestore.
     */
    suspend fun markGameAsEnded(gameId: String) {
        try {
            gamesCollection.document(gameId).update("hasGameEnded", true).await()
            Log.d(
                "GameService",
                "Game $gameId marked as ended successfully using hasGameEnded (abrupt)."
            )
        } catch (e: Exception) {
            Log.e("GameService", "Error marking game $gameId as ended: ${e.message}", e)
            throw e
        }
    }

    /**
     * Ends the game and triggers final score calculation.
     */
    suspend fun endGameByPoints(gameId: String) {
        try {
            val gameRef = gamesCollection.document(gameId)
            gameRef.update("hasGameEnded", true).await()
            Log.d(
                "GameService",
                "Game $gameId marked as ended by points successfully. Data remains for score screen."
            )
        } catch (e: Exception) {
            Log.e("GameService", "Error ending game by points for game $gameId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Performs the actual deletion of a game and all its associated players from Firestore.
     */
    suspend fun cleanUpGameData(gameId: String) {
        val batch: WriteBatch = db.batch()
        val gameRef = gamesCollection.document(gameId)
        batch.delete(gameRef)
        val playersQuery = playersCollection.whereEqualTo("gameId", gameId).get().await()
        for (document in playersQuery.documents) {
            batch.delete(document.reference)
        }
        batch.commit().await()
        Log.d("GameService", "Game $gameId and associated players cleaned up successfully.")
    }

    /**
     * Updates the resources of a player by their ID.
     */
    suspend fun updatePlayerResources(playerId: String, money: Int, glitchEnergy: Int) {
        val playerRef = playersCollection.document(playerId)
        playerRef.update(
            mapOf(
                "money" to money,
                "glitchEnergy" to glitchEnergy
            )
        ).await()
    }

    /**
     * Updates the manual bonus PV of a player.
     */
    suspend fun adjustPlayerManualBonusPV(playerId: String, pvDelta: Int): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                if (player == null) {
                    throw Exception("Jugador no encontrado para el ajuste manual de PV.")
                }
                player.manualBonusPV += pvDelta
                transaction.set(playerRef, player)
                null
            }.await()
            true
        } catch (e: Exception) {
            Log.e(
                "GameService",
                "Error adjusting manual bonus PV for player $playerId: ${e.message}",
                e
            )
            false
        }
    }


    /**
     * Updates the name of a player by their ID.
     */
    suspend fun updatePlayerName(playerId: String, newName: String) {
        val playerRef = playersCollection.document(playerId)
        playerRef.update("name", newName).await()
    }

    /**
     * Updates a player's inventory.
     */
    suspend fun updatePlayerInventory(playerId: String, inventory: List<CultivoInventario>) {
        val playerRef = playersCollection.document(playerId)
        playerRef.update("inventario", inventory).await()
    }

    /**
     * Updates the dice-related state of a player in Firestore.
     */
    suspend fun updatePlayerDiceState(
        playerId: String,
        diceRoll: List<DadoSimbolo>,
        rollPhase: Int,
        hasRerolled: Boolean,
        mysteryButtonsRemaining: Int
    ) {
        val playerRef = playersCollection.document(playerId)
        playerRef.update(
            mapOf(
                "currentDiceRoll" to diceRoll.map { it.name },
                "rollPhase" to rollPhase,
                "hasRerolled" to hasRerolled,
                "mysteryButtonsRemaining" to mysteryButtonsRemaining
            )
        ).await()
    }

    /**
     * Manually adjusts a player's money and glitch energy.
     */
    suspend fun adjustPlayerResourcesManually(
        playerId: String,
        moneyDelta: Int,
        energyDelta: Int
    ): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                if (player == null) {
                    throw Exception("Jugador no encontrado para el ajuste manual de recursos.")
                }
                player.money += moneyDelta
                player.glitchEnergy += energyDelta
                transaction.set(playerRef, player)
                null
            }.await()
            true
        } catch (e: Exception) {
            Log.e("GameService", "Error adjusting resources for player $playerId: ${e.message}", e)
            false
        }
    }


    /**
     * Deletes a player by their ID.
     */
    suspend fun deletePlayer(playerId: String) {
        playersCollection.document(playerId).delete().await()
    }

    /**
     * Simulates the rolling of 4 custom dice and returns the resulting symbols.
     */
    suspend fun rollDice(playerId: String) {
        val diceResults = List(4) { DadoSimbolo.values().random() }
        playersCollection.document(playerId).update(
            mapOf(
                "currentDiceRoll" to diceResults.map { it.name },
                "rollPhase" to 1,
                "hasRerolled" to false,
                "haUsadoPasivaIngeniero" to false,
                "haUsadoHabilidadActiva" to false // Se resetea al tirar
            )
        ).await()
    }

    /**
     * Simulates rerolling selected dice and returns the new resulting symbols.
     */
    suspend fun rerollDice(
        playerId: String,
        currentDiceRoll: List<DadoSimbolo>,
        keptDiceIndices: List<Int>
    ) {
        val newRoll = currentDiceRoll.mapIndexed { index, symbol ->
            if (keptDiceIndices.contains(index)) symbol else DadoSimbolo.values().random()
        }
        playersCollection.document(playerId).update(
            mapOf(
                "currentDiceRoll" to newRoll.map { it.name },
                "hasRerolled" to true
            )
        ).await()
    }

    /**
     * Starts a mystery encounter for a player.
     */
    suspend fun startMysteryEncounter(playerId: String, isOneMobileMode: Boolean = false) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            if (!isOneMobileMode) {
                if (player.mysteryButtonsRemaining <= 0) return@runTransaction
                player.mysteryButtonsRemaining -= 1
            }
            val encounter = allMysteryEncounters.random()
            player.activeMysteryId = encounter.id
            player.lastMysteryResult = null
            transaction.set(playerRef, player)
        }.await()
    }

    /**
     * Resolves the outcome of a mystery encounter based on player choice.
     */
    suspend fun resolveMysteryOutcome(playerId: String, choiceId: String? = null) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>()
            if (player?.activeMysteryId == null) return@runTransaction

            val encounter = allMysteryEncounters.find { it.id == player.activeMysteryId }
            if (encounter == null) {
                player.activeMysteryId = null
                transaction.set(playerRef, player)
                return@runTransaction
            }

            val outcome: MysteryOutcome? = when (encounter) {
                is DecisionEncounter -> encounter.choices.find { it.id == choiceId }?.outcome
                is RandomEventEncounter -> {
                    val totalWeight = encounter.outcomes.sumOf { it.second }
                    var randomPoint = Random.nextInt(totalWeight)
                    var chosenOutcome: MysteryOutcome? = null
                    for ((possibleOutcome, weight) in encounter.outcomes) {
                        if (randomPoint < weight) {
                            chosenOutcome = possibleOutcome
                            break
                        }
                        randomPoint -= weight
                    }
                    chosenOutcome
                }

                else -> null
            }

            if (outcome != null) {
                player.money += outcome.moneyChange
                player.glitchEnergy += outcome.energyChange
                player.lastMysteryResult = outcome.description
            }
            player.activeMysteryId = null
            transaction.set(playerRef, player)
        }.await()
    }

    /**
     * Resolves the outcome of a minigame encounter.
     */
    suspend fun resolveMinigameOutcome(playerId: String, wasSuccessful: Boolean) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>()
            if (player?.activeMysteryId == null) return@runTransaction

            val encounter =
                allMysteryEncounters.find { it.id == player.activeMysteryId } as? MinigameEncounter
            if (encounter == null) {
                player.activeMysteryId = null
                transaction.set(playerRef, player)
                return@runTransaction
            }

            val outcome = if (wasSuccessful) encounter.successOutcome else encounter.failureOutcome

            player.money += outcome.moneyChange
            player.glitchEnergy += outcome.energyChange
            player.lastMysteryResult = outcome.description

            player.activeMysteryId = null
            transaction.set(playerRef, player)
        }.await()
    }

    /**
     * Clears the mystery result so the dialog doesn't reappear.
     */
    suspend fun clearMysteryResult(playerId: String) {
        val playerRef = playersCollection.document(playerId)
        try {
            playerRef.update("lastMysteryResult", null).await()
        } catch (e: Exception) {
            Log.e("GameService", "Error clearing mystery result for player $playerId", e)
        }
    }

    /**
     * Applies the effects of the dice roll to the player's resources and game state.
     */
    suspend fun applyDiceEffects(playerId: String, diceResults: List<DadoSimbolo>): String {
        val playerRef = playersCollection.document(playerId)
        var moneyChange = 0
        var energyChange = 0
        var mysteryEventsCount = 0
        var glitchDiceCount = 0
        var plantDiceCount = 0
        var growthDiceCount = 0
        val feedbackMessage = StringBuilder("Resumen de tu tirada:\n")

        for (symbol in diceResults) {
            when (symbol) {
                DadoSimbolo.MONEDA -> moneyChange += 2
                DadoSimbolo.ENERGIA -> energyChange += 1
                DadoSimbolo.MISTERIO -> mysteryEventsCount += 1
                DadoSimbolo.GLITCH -> glitchDiceCount += 1
                DadoSimbolo.PLANTAR -> plantDiceCount += 1
                DadoSimbolo.CRECIMIENTO -> growthDiceCount += 1
            }
        }

        db.runTransaction { transaction ->
            val player =
                transaction.get(playerRef).toObject<Player>() ?: throw Exception("Player not found")
            player.money += moneyChange
            player.glitchEnergy += energyChange
            player.rollPhase = 2
            player.mysteryButtonsRemaining = mysteryEventsCount
            player.activeMysteryId = null
            player.lastMysteryResult = null
            transaction.set(playerRef, player)
        }.await()

        if (moneyChange > 0) feedbackMessage.append("• Ganaste $moneyChange monedas.💰\n")
        if (energyChange > 0) feedbackMessage.append("• Ganaste $energyChange energía Glitch.⚡\n")
        if (growthDiceCount > 0) feedbackMessage.append("• Tienes $growthDiceCount acciones de crecimiento (aplica manualmente).➕\n")
        if (plantDiceCount > 0) feedbackMessage.append("• Puedes plantar $plantDiceCount cultivos de tu mano (aplica costes físicamente).\n")
        if (glitchDiceCount > 0) feedbackMessage.append("• ¡Puedes robar $glitchDiceCount cartas de Mejora Glitch! (Resuelve físicamente).\n")
        if (mysteryEventsCount > 0) feedbackMessage.append("• Tienes $mysteryEventsCount dados de Misterio (resuelve en la app).❓\n")

        return feedbackMessage.toString()
    }

    /**
     * [CORREGIDO] Permite a un jugador vender un cultivo de su inventario.
     * La función ahora calcula el precio de venta real basándose en el estado de la partida,
     * para asegurar que los eventos de mercado temporales se apliquen correctamente.
     */
    suspend fun sellCrop(gameId: String, playerId: String, cropId: String, quantity: Int): Boolean {
        val playerRef = playersCollection.document(playerId)
        val gameRef = gamesCollection.document(gameId)

        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                    ?: throw Exception("Player not found")
                val game =
                    transaction.get(gameRef).toObject<Game>() ?: throw Exception("Game not found")

                val inventoryItem = player.inventario.find { it.id == cropId }
                if (inventoryItem == null || inventoryItem.cantidad < quantity) {
                    throw Exception("Insufficient inventory")
                }

                // Calcular el precio de venta real, aplicando el evento temporal si está activo.
                var marketPrice = when (cropId) {
                    "zanahoria" -> game.marketPrices.zanahoria
                    "maiz" -> game.marketPrices.maiz
                    "patata" -> game.marketPrices.patata
                    "tomateCubico" -> game.marketPrices.tomateCubico
                    "maizArcoiris" -> game.marketPrices.maizArcoiris
                    "brocoliCristal" -> game.marketPrices.brocoliCristal
                    "pimientoExplosivo" -> game.marketPrices.pimientoExplosivo
                    else -> 0
                }

                // Aplicar el efecto de Interferencia de Señal si está activo.
                if (game.signalInterferenceActive) {
                    marketPrice = max(1, marketPrice / 2)
                }

                inventoryItem.cantidad -= quantity
                if (inventoryItem.cantidad == 0) {
                    player.inventario.remove(inventoryItem)
                }
                player.money += quantity * marketPrice
                transaction.set(playerRef, player)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("GameService", "Sell crop transaction failed for $playerId", e)
            false
        }
    }

    /**
     * Adds a crop to the player's digital inventory.
     */
    suspend fun addCropToInventory(
        playerId: String,
        cropId: String,
        cropName: String,
        valorVentaBase: Int,
        pvFinalJuego: Int
    ): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                    ?: throw Exception("Player not found")
                val existingCrop = player.inventario.find { it.id == cropId }
                if (existingCrop != null) {
                    existingCrop.cantidad += 1
                } else {
                    val newCrop = CultivoInventario(
                        id = cropId,
                        nombre = cropName,
                        cantidad = 1,
                        valorVentaBase = valorVentaBase,
                        pvFinalJuego = pvFinalJuego
                    )
                    player.inventario.add(newCrop)
                }
                transaction.set(playerRef, player)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("GameService", "Failed to add crop to inventory for $playerId", e)
            false
        }
    }

    /**
     * [MODIFICADO] Advances the turn to the next player in the game or switches to the market phase.
     */
    suspend fun advanceTurn(gameId: String, currentPlayerId: String): String? {
        val gameRef = gamesCollection.document(gameId)
        val playerRef = playersCollection.document(currentPlayerId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                val currentPlayer = transaction.get(playerRef).toObject<Player>()
                if (game == null || currentPlayer == null) return@runTransaction null

                if (!game.playersFinishedTurn.contains(currentPlayerId)) {
                    game.playersFinishedTurn.add(currentPlayerId)
                }
                val playerOrder = game.playerOrder
                val allPlayersFinishedActions = game.playersFinishedTurn.size == playerOrder.size
                if (allPlayersFinishedActions) {
                    game.roundPhase = "MARKET_PHASE"
                    game.playersFinishedTurn.clear()
                    game.currentPlayerTurnId = null
                    transaction.set(gameRef, game)
                    "market_phase"
                } else {
                    val currentIndex = playerOrder.indexOf(currentPlayerId)
                    if (currentIndex == -1) return@runTransaction null
                    val nextIndex = (currentIndex + 1) % playerOrder.size
                    val nextPlayerId = playerOrder[nextIndex]
                    game.currentPlayerTurnId = nextPlayerId

                    // Reset state for the player whose turn just ended
                    currentPlayer.currentDiceRoll = emptyList()
                    currentPlayer.rollPhase = 0
                    currentPlayer.hasRerolled = false
                    currentPlayer.haUsadoPasivaIngeniero = false
                    currentPlayer.haUsadoHabilidadActiva =
                        false // ¡NUEVO! Resetea la habilidad activa
                    currentPlayer.mysteryButtonsRemaining = 0
                    currentPlayer.activeMysteryId = null
                    currentPlayer.lastMysteryResult = null
                    transaction.set(playerRef, currentPlayer)
                    nextPlayerId
                }
            }.await()
        } catch (e: Exception) {
            Log.e("GameService", "Error advancing turn in game $gameId: ${e.message}", e)
            null
        }
    }

    /**
     * Marks that a player has finished their actions in the market phase.
     */
    suspend fun playerFinishedMarket(gameId: String, playerId: String) {
        val gameRef = gamesCollection.document(gameId)
        try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                if (game != null && !game.playersFinishedMarket.contains(playerId)) {
                    game.playersFinishedMarket.add(playerId)
                    transaction.set(gameRef, game)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(
                "GameService",
                "Error marking player as finished market in game $gameId: ${e.message}",
                e
            )
        }
    }

    /**
     * Calculates the new price for a crop with a tendency towards its base value.
     */
    private fun calculateNewPrice(currentPrice: Int, basePrice: Int): Int {
        val probIncrease = when {
            currentPrice < basePrice -> 70
            currentPrice > basePrice -> 30
            else -> 50
        }
        val change = if (Random.nextInt(1, 101) <= probIncrease) 1 else -1
        return max(1, currentPrice + change)
    }

    /**
     * [CORREGIDO] Advances the game to the next round (host only).
     * Ahora, el efecto de "Interferencia de Señal" se resetea correctamente al
     * pasar de ronda, evitando que los precios se queden reducidos para siempre.
     */
    suspend fun advanceRound(gameId: String): Boolean {
        val gameRef = gamesCollection.document(gameId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                if (game == null) return@runTransaction false

                val playersInGame = game.playerIds.mapNotNull { playerId ->
                    val playerRef = playersCollection.document(playerId)
                    transaction.get(playerRef).toObject<Player>()
                }

                if (game.playersFinishedMarket.size != playersInGame.size) {
                    return@runTransaction false
                }

                // Apply Global Event Effects
                when (game.lastEvent?.name) {
                    "Impuesto Sorpresa" -> {
                        playersInGame.forEach { player ->
                            if (player.money >= 10) {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "money",
                                    player.money - 3
                                )
                            }
                        }
                    }

                    "Fuga de Energía" -> {
                        playersInGame.forEach { player ->
                            if (player.glitchEnergy >= 1) {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "glitchEnergy",
                                    player.glitchEnergy - 1
                                )
                            } else {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "money",
                                    player.money - 2
                                )
                            }
                        }
                    }

                    "Bonus del Sindicato" -> {
                        playersInGame.forEach { player ->
                            transaction.update(
                                playersCollection.document(player.id),
                                "money",
                                player.money + 2
                            )
                        }
                    }
                }

                // Update Market Prices & Round Event
                val basePrices = allCrops.associate { it.id to it.valorVentaBase }
                var currentPrices = game.marketPrices
                var newEvent: GlitchEvent? = null
                var newSupplyFailureActive = game.supplyFailureActive
                var newSignalInterferenceActive = false

                // Reset temporary effects from previous round
                if (game.signalInterferenceActive) newSignalInterferenceActive = false

                // Calculate new base market prices
                val newMarketPrices = MarketPrices(
                    zanahoria = calculateNewPrice(
                        currentPrices.zanahoria,
                        basePrices["zanahoria"] ?: 3
                    ),
                    maiz = calculateNewPrice(currentPrices.maiz, basePrices["maiz"] ?: 4),
                    patata = calculateNewPrice(currentPrices.patata, basePrices["patata"] ?: 3),
                    tomateCubico = calculateNewPrice(
                        currentPrices.tomateCubico,
                        basePrices["tomateCubico"] ?: 6
                    ),
                    maizArcoiris = calculateNewPrice(
                        currentPrices.maizArcoiris,
                        basePrices["maizArcoiris"] ?: 7
                    ),
                    brocoliCristal = calculateNewPrice(
                        currentPrices.brocoliCristal,
                        basePrices["brocoliCristal"] ?: 6
                    ),
                    pimientoExplosivo = calculateNewPrice(
                        currentPrices.pimientoExplosivo,
                        basePrices["pimientoExplosivo"] ?: 8
                    )
                )

                // Generate new round event and apply its effects
                val playersWithGlitchEnergy = playersInGame.filter { it.glitchEnergy > 0 }
                if (Random.nextDouble() < 0.60 && playersWithGlitchEnergy.isNotEmpty()) {
                    val possibleEvents =
                        if (game.supplyFailureActive) eventosGlitch.filter { it.name != "Fallo de Suministro" } else eventosGlitch
                    if (possibleEvents.isNotEmpty()) newEvent = possibleEvents.random()
                }

                when (newEvent?.name) {
                    "Fallo de Suministro" -> newSupplyFailureActive = true
                    "Interferencia de Señal" -> newSignalInterferenceActive = true
                    "Fiebre del Oro" -> {
                        currentPrices = newMarketPrices.copy(
                            maiz = newMarketPrices.maiz + 2,
                            patata = newMarketPrices.patata + 2
                        )
                    }

                    "Aumento de Demanda" -> {
                        currentPrices = newMarketPrices.copy(
                            zanahoria = newMarketPrices.zanahoria + 1,
                            maiz = newMarketPrices.maiz + 1,
                            patata = newMarketPrices.patata + 1
                        )
                    }

                    "Cosecha Mutante Exitosa" -> {
                        currentPrices = newMarketPrices.copy(
                            tomateCubico = newMarketPrices.tomateCubico + 1,
                            maizArcoiris = newMarketPrices.maizArcoiris + 1,
                            brocoliCristal = newMarketPrices.brocoliCristal + 1,
                            pimientoExplosivo = newMarketPrices.pimientoExplosivo + 1
                        )
                    }
                }

                // Reset players state for the new round
                playersInGame.forEach { player ->
                    val playerRef = playersCollection.document(player.id)
                    transaction.set(
                        playerRef, player.copy(
                            currentDiceRoll = emptyList(),
                            rollPhase = 0,
                            hasRerolled = false,
                            haUsadoPasivaIngeniero = false,
                            haUsadoHabilidadActiva = false,
                            mysteryButtonsRemaining = 0,
                            activeMysteryId = null,
                            lastMysteryResult = null
                        )
                    )
                }

                // Update game state for the new round
                game.marketPrices = currentPrices
                game.lastEvent = newEvent
                game.supplyFailureActive = newSupplyFailureActive
                game.signalInterferenceActive = newSignalInterferenceActive
                game.roundPhase = "PLAYER_ACTIONS"
                game.playersFinishedMarket.clear()
                game.roundNumber += 1
                game.currentPlayerTurnId = game.playerOrder.getOrNull(game.roundNumber % game.playerOrder.size)
                transaction.set(gameRef, game)

                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Advances the game to the next round specifically for "One Mobile Mode".
     */
    suspend fun advanceOneMobileRound(gameId: String): Boolean {
        val gameRef = gamesCollection.document(gameId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                if (game == null) return@runTransaction false

                val playersInGame = game.playerIds.mapNotNull { playerId ->
                    val playerRef = playersCollection.document(playerId)
                    transaction.get(playerRef).toObject<Player>()
                }

                // Apply Global Event Effects
                when (game.lastEvent?.name) {
                    "Impuesto Sorpresa" -> {
                        playersInGame.forEach { player ->
                            if (player.money >= 10) {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "money",
                                    player.money - 3
                                )
                            }
                        }
                    }

                    "Fuga de Energía" -> {
                        playersInGame.forEach { player ->
                            if (player.glitchEnergy >= 1) {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "glitchEnergy",
                                    player.glitchEnergy - 1
                                )
                            } else {
                                transaction.update(
                                    playersCollection.document(player.id),
                                    "money",
                                    player.money - 2
                                )
                            }
                        }
                    }

                    "Bonus del Sindicato" -> {
                        playersInGame.forEach { player ->
                            transaction.update(
                                playersCollection.document(player.id),
                                "money",
                                player.money + 2
                            )
                        }
                    }
                }

                // Update Market Prices & Round Event
                val basePrices = allCrops.associate { it.id to it.valorVentaBase }
                var currentPrices = game.marketPrices
                var newEvent: GlitchEvent? = null
                var newSupplyFailureActive = game.supplyFailureActive
                var newSignalInterferenceActive = false

                // Reset temporary effects from previous round
                if (game.signalInterferenceActive) newSignalInterferenceActive = false

                // Calculate new base market prices
                val newMarketPrices = MarketPrices(
                    zanahoria = calculateNewPrice(
                        currentPrices.zanahoria,
                        basePrices["zanahoria"] ?: 3
                    ),
                    maiz = calculateNewPrice(currentPrices.maiz, basePrices["maiz"] ?: 4),
                    patata = calculateNewPrice(currentPrices.patata, basePrices["patata"] ?: 3),
                    tomateCubico = calculateNewPrice(
                        currentPrices.tomateCubico,
                        basePrices["tomateCubico"] ?: 6
                    ),
                    maizArcoiris = calculateNewPrice(
                        currentPrices.maizArcoiris,
                        basePrices["maizArcoiris"] ?: 7
                    ),
                    brocoliCristal = calculateNewPrice(
                        currentPrices.brocoliCristal,
                        basePrices["brocoliCristal"] ?: 6
                    ),
                    pimientoExplosivo = calculateNewPrice(
                        currentPrices.pimientoExplosivo,
                        basePrices["pimientoExplosivo"] ?: 8
                    )
                )

                // Generate new round event and apply its effects
                val playersWithGlitchEnergy = playersInGame.filter { it.glitchEnergy > 0 }
                if (Random.nextDouble() < 0.60 && playersWithGlitchEnergy.isNotEmpty()) {
                    val possibleEvents =
                        if (game.supplyFailureActive) eventosGlitch.filter { it.name != "Fallo de Suministro" } else eventosGlitch
                    if (possibleEvents.isNotEmpty()) newEvent = possibleEvents.random()
                }

                when (newEvent?.name) {
                    "Fallo de Suministro" -> newSupplyFailureActive = true
                    "Interferencia de Señal" -> newSignalInterferenceActive = true
                    "Fiebre del Oro" -> {
                        currentPrices = newMarketPrices.copy(
                            maiz = newMarketPrices.maiz + 2,
                            patata = newMarketPrices.patata + 2
                        )
                    }

                    "Aumento de Demanda" -> {
                        currentPrices = newMarketPrices.copy(
                            zanahoria = newMarketPrices.zanahoria + 1,
                            maiz = newMarketPrices.maiz + 1,
                            patata = newMarketPrices.patata + 1
                        )
                    }

                    "Cosecha Mutante Exitosa" -> {
                        currentPrices = newMarketPrices.copy(
                            tomateCubico = newMarketPrices.tomateCubico + 1,
                            maizArcoiris = newMarketPrices.maizArcoiris + 1,
                            brocoliCristal = newMarketPrices.brocoliCristal + 1,
                            pimientoExplosivo = newMarketPrices.pimientoExplosivo + 1
                        )
                    }
                }

                // Reset players state for the new round
                playersInGame.forEach { player ->
                    val playerRef = playersCollection.document(player.id)
                    transaction.set(
                        playerRef, player.copy(
                            currentDiceRoll = emptyList(),
                            rollPhase = 0,
                            hasRerolled = false,
                            haUsadoPasivaIngeniero = false,
                            haUsadoHabilidadActiva = false,
                            mysteryButtonsRemaining = 0,
                            activeMysteryId = null,
                            lastMysteryResult = null
                        )
                    )
                }

                // Update game state for the new round
                game.marketPrices = currentPrices
                game.lastEvent = newEvent
                game.supplyFailureActive = newSupplyFailureActive
                game.signalInterferenceActive = newSignalInterferenceActive
                game.roundPhase = "PLAYER_ACTIONS"
                game.playersFinishedMarket.clear()
                game.roundNumber += 1
                game.currentPlayerTurnId = game.playerOrder.firstOrNull() // Director de juego
                transaction.set(gameRef, game)

                true
            }.await()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updatePlayerOrderAndStartGame(gameId: String, orderedPlayerIds: List<String>) {
        val gameRef = gamesCollection.document(gameId)
        db.runTransaction { transaction ->
            val game = transaction.get(gameRef).toObject<Game>() ?: return@runTransaction
            game.playerOrder = orderedPlayerIds.toMutableList()
            game.isStarted = true
            game.currentPlayerTurnId = orderedPlayerIds.firstOrNull()
            game.roundNumber = 1
            transaction.set(gameRef, game)
        }.await()
    }

    /**
     * Allows the host to force the game to advance to the next player.
     * Use this when a player's turn is stuck.
     */
    suspend fun forceAdvanceTurn(gameId: String, currentStuckPlayerId: String) {
        val gameRef = gamesCollection.document(gameId)
        val playerRef = playersCollection.document(currentStuckPlayerId)
        db.runTransaction { transaction ->
            val game = transaction.get(gameRef).toObject<Game>()
            val stuckPlayer = transaction.get(playerRef).toObject<Player>()

            if (game != null && stuckPlayer != null) {
                // Find next player in the established order
                val playerOrder = game.playerOrder
                val currentIndex = playerOrder.indexOf(currentStuckPlayerId)
                if (currentIndex != -1) {
                    val nextIndex = (currentIndex + 1) % playerOrder.size
                    val nextPlayerId = playerOrder[nextIndex]
                    game.currentPlayerTurnId = nextPlayerId

                    // Reset stuck player's state
                    stuckPlayer.currentDiceRoll = emptyList()
                    stuckPlayer.rollPhase = 0
                    stuckPlayer.hasRerolled = false
                    stuckPlayer.haUsadoPasivaIngeniero = false
                    stuckPlayer.haUsadoHabilidadActiva = false
                    stuckPlayer.mysteryButtonsRemaining = 0
                    stuckPlayer.activeMysteryId = null
                    stuckPlayer.lastMysteryResult = null

                    transaction.set(playerRef, stuckPlayer)
                    transaction.set(gameRef, game)
                }
            }
        }.await()
    }

    /**
     * [¡NUEVO!] Usa la habilidad activa del Ingeniero Glitch para cambiar la cara de un dado.
     */
    suspend fun usarActivableIngeniero(
        playerId: String,
        dieIndex: Int,
        nuevoSimbolo: DadoSimbolo
    ): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                    ?: throw Exception("Player not found")
                if (player.granjero?.id == "ingeniero_glitch" &&
                    player.glitchEnergy >= 1 &&
                    !player.haUsadoHabilidadActiva && // ¡NUEVO! Comprueba si ya se usó
                    dieIndex in player.currentDiceRoll.indices
                ) {

                    player.glitchEnergy -= 1
                    player.haUsadoHabilidadActiva = true // ¡NUEVO! Marca la habilidad como usada
                    val newDiceRoll = player.currentDiceRoll.toMutableList()
                    newDiceRoll[dieIndex] = nuevoSimbolo
                    player.currentDiceRoll = newDiceRoll
                    transaction.set(playerRef, player)
                    true // Éxito
                } else {
                    false // No se pudo usar
                }
            }.await()
        } catch (e: Exception) {
            Log.e("GameService", "Error en usarActivableIngeniero: ${e.message}", e)
            false
        }
    }

    /**
     * Usa la habilidad pasiva del Ingeniero Glitch para relanzar un solo dado.
     */
    suspend fun usarPasivaIngeniero(playerId: String, dieIndex: Int) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            if (player.granjero?.id == "ingeniero_glitch" && !player.haUsadoPasivaIngeniero && dieIndex in player.currentDiceRoll.indices) {
                val newDiceRoll = player.currentDiceRoll.toMutableList()
                newDiceRoll[dieIndex] = DadoSimbolo.values().random()
                player.currentDiceRoll = newDiceRoll
                player.haUsadoPasivaIngeniero = true
                transaction.set(playerRef, player)
            }
        }.await()
    }

    /**
     * Attempts to claim an objective for a player.
     */
    suspend fun claimObjective(gameId: String, playerId: String, objectiveId: String): String {
        val gameRef = gamesCollection.document(gameId)
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                val player = transaction.get(playerRef).toObject<Player>()
                if (game == null || player == null) return@runTransaction "Error: Partida o jugador no encontrados."

                val objective = allObjectives.find { it.id == objectiveId }
                if (objective == null) return@runTransaction "Error: Objetivo no encontrado."

                if (!game.activeObjectives.any { it.id == objective.id }) return@runTransaction "Este objetivo no está activo."
                if (player.objectivesClaimed.contains(objectiveId)) return@runTransaction "Ya has reclamado este objetivo."

                var canClaim = false
                var feedback = ""

                when (objective.type) {
                    "money" -> {
                        if (player.money >= objective.targetValue) {
                            canClaim = true
                            feedback =
                                "¡Objetivo de Monedas completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback =
                                "Necesitas ${objective.targetValue} monedas. Tienes ${player.money}."
                        }
                    }

                    "total_harvest" -> {
                        val totalHarvested = player.inventario.sumOf { it.cantidad }
                        if (totalHarvested >= objective.targetValue) {
                            canClaim = true
                            feedback =
                                "¡Objetivo de Cosecha Total completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback =
                                "Necesitas cosechar ${objective.targetValue} cultivos. Llevas $totalHarvested."
                        }
                    }

                    "specific_harvest" -> {
                        val specificCropCount =
                            player.inventario.find { it.id == objective.targetCropId }?.cantidad
                                ?: 0
                        if (specificCropCount >= objective.targetValue) {
                            canClaim = true
                            val cropName = allCrops.find { it.id == objective.targetCropId }?.nombre
                                ?: "cultivo"
                            feedback =
                                "¡Objetivo de Cosecha de $cropName completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            val cropName = allCrops.find { it.id == objective.targetCropId }?.nombre
                                ?: "cultivos"
                            feedback =
                                "Necesitas ${objective.targetValue} de $cropName. Tienes $specificCropCount."
                        }
                    }

                    "dice_roll_all_same" -> {
                        val firstSymbol = player.currentDiceRoll.firstOrNull()
                        val allSame =
                            firstSymbol != null && player.currentDiceRoll.all { it == firstSymbol }
                        if (allSame && player.currentDiceRoll.isNotEmpty()) {
                            canClaim = true
                            feedback =
                                "¡Objetivo de Tirada Perfecta completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback = "Necesitas una tirada de 4 dados con el mismo símbolo."
                        }
                    }

                    else -> feedback = "Tipo de objetivo desconocido."
                }

                if (canClaim) {
                    player.objectivesClaimed.add(objective.id)
                    transaction.set(playerRef, player)
                    return@runTransaction feedback
                } else {
                    return@runTransaction feedback
                }
            }.await()
        } catch (e: Exception) {
            Log.e(
                "GameService",
                "Error al reclamar objetivo $objectiveId para jugador $playerId: ${e.message}",
                e
            )
            "Error al reclamar objetivo: ${e.message}"
        }
    }
}
