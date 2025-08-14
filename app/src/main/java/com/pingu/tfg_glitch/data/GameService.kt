package com.pingu.tfg_glitch.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.delay
import android.util.Log
import kotlin.random.Random
import kotlin.math.max
import java.util.Locale

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
        val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
        return (1..6)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    /**
     * Creates a new game in Firestore and returns the game code and the host's player ID.
     */
    suspend fun createGame(hostName: String, farmerType: String): Pair<String, String> {
        val gameId = generateGameCode()
        val selectedObjectives = allObjectives.shuffled(Random).take(3).toMutableList()

        val hostDocRef = playersCollection.document()
        val hostPlayerId = hostDocRef.id

        val newGame = Game(
            id = gameId,
            activeObjectives = selectedObjectives,
            hostPlayerId = hostPlayerId,
            roundStartPlayerId = hostPlayerId // El host empieza la primera ronda
        )
        gamesCollection.document(newGame.id).set(newGame).await()

        val hostPlayer = Player(
            id = hostPlayerId,
            gameId = newGame.id,
            name = hostName,
            farmerType = farmerType,
            money = 10,
            glitchEnergy = 0,
            hasUsedStandardReroll = false,
            hasUsedFreeReroll = false,
            hasUsedActiveSkill = false,
            // ... el resto de campos se inicializan por defecto
        )
        hostDocRef.set(hostPlayer).await()

        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(newGame.id)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null) {
                game.playerIds.add(hostPlayerId)
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
        val newGame = Game(id = gameId, activeObjectives = selectedObjectives)
        gamesCollection.document(newGame.id).set(newGame).await()

        val playerDocRef = playersCollection.document()
        val playerId = playerDocRef.id
        val oneMobilePlayer = Player(
            id = playerId,
            gameId = newGame.id,
            name = "Jugador Un Móvil",
            money = 10,
            glitchEnergy = 0,
            // ... el resto de campos se inicializan por defecto
        )
        playerDocRef.set(oneMobilePlayer).await()

        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(newGame.id)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null) {
                game.playerIds.add(playerId)
                game.hostPlayerId = playerId
                game.currentPlayerTurnId = playerId
                game.roundStartPlayerId = playerId
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
     * Adds a new player to the game by name and returns the newly generated player ID.
     */
    suspend fun addPlayerToGameByName(gameId: String, playerName: String, farmerType: String): String {
        val gameDoc = gamesCollection.document(gameId).get().await()
        if (!gameDoc.exists()) {
            throw Exception("Game with ID $gameId does not exist.")
        }

        val newPlayerDocRef = playersCollection.document()
        val newPlayerId = newPlayerDocRef.id
        val newPlayer = Player(
            id = newPlayerId,
            gameId = gameId,
            name = playerName,
            farmerType = farmerType,
            money = 10,
            glitchEnergy = 0,
            // ... el resto de campos se inicializan por defecto
        )

        newPlayerDocRef.set(newPlayer).await()

        db.runTransaction { transaction ->
            val gameRef = gamesCollection.document(gameDoc.id)
            val game = transaction.get(gameRef).toObject<Game>()
            if (game != null && !game.playerIds.contains(newPlayerId)) {
                game.playerIds.add(newPlayerId)
                if (game.currentPlayerTurnId == null) {
                    game.currentPlayerTurnId = newPlayerId
                }
                transaction.set(gameRef, game)
            }
        }.await()

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
            Log.d("GameService", "Game $gameId marked as ended successfully using hasGameEnded (abrupt).")
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
            Log.d("GameService", "Game $gameId marked as ended by points successfully. Data remains for score screen.")
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
            Log.e("GameService", "Error adjusting manual bonus PV for player $playerId: ${e.message}", e)
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
        hasUsedStandardReroll: Boolean,
        mysteryButtonsRemaining: Int
    ) {
        val playerRef = playersCollection.document(playerId)
        playerRef.update(
            mapOf(
                "currentDiceRoll" to diceRoll.map { it.name },
                "rollPhase" to rollPhase,
                "hasUsedStandardReroll" to hasUsedStandardReroll,
                "mysteryButtonsRemaining" to mysteryButtonsRemaining
            )
        ).await()
    }

    /**
     * Manually adjusts a player's money and glitch energy.
     */
    suspend fun adjustPlayerResourcesManually(playerId: String, moneyDelta: Int, energyDelta: Int): Boolean {
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
        updatePlayerDiceState(playerId, diceResults, 1, false, 0)
    }

    /**
     * Simulates rerolling selected dice and returns the new resulting symbols.
     */
    suspend fun rerollDice(playerId: String, currentDiceRoll: List<DadoSimbolo>, keptDiceIndices: List<Int>) {
        val newRoll = currentDiceRoll.mapIndexed { index, symbol ->
            if (keptDiceIndices.contains(index)) symbol else DadoSimbolo.values().random()
        }
        updatePlayerDiceState(playerId, newRoll, 1, true, 0)
    }

    suspend fun useEngineerFreeReroll(playerId: String, diceIndex: Int) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            if (player.farmerType == "Ingeniero Glitch" && !player.hasUsedFreeReroll && player.currentDiceRoll.isNotEmpty()) {
                val newRoll = player.currentDiceRoll.toMutableList()
                newRoll[diceIndex] = DadoSimbolo.values().random()
                player.currentDiceRoll = newRoll
                player.hasUsedFreeReroll = true
                transaction.set(playerRef, player)
            }
        }.await()
    }

    suspend fun changeDiceSymbol(playerId: String, diceIndex: Int, newSymbol: DadoSimbolo) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            if (player.farmerType == "Ingeniero Glitch" && player.glitchEnergy >= 1 && player.currentDiceRoll.isNotEmpty() && !player.hasUsedActiveSkill) {
                val newRoll = player.currentDiceRoll.toMutableList()
                newRoll[diceIndex] = newSymbol
                player.currentDiceRoll = newRoll
                player.glitchEnergy -= 1
                player.hasUsedActiveSkill = true
                transaction.set(playerRef, player)
            }
        }.await()
    }

    suspend fun useVisionarySkill(playerId: String) {
        val playerRef = playersCollection.document(playerId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            if (player.farmerType == "Visionaria Píxel" && player.glitchEnergy >= 1 && !player.hasUsedActiveSkill) {
                player.glitchEnergy -= 1
                player.hasUsedActiveSkill = true
                transaction.set(playerRef, player)
            }
        }.await()
    }

    suspend fun applyMerchantPriceBoost(gameId: String, playerId: String, cropId: String) {
        val playerRef = playersCollection.document(playerId)
        val gameRef = gamesCollection.document(gameId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            val game = transaction.get(gameRef).toObject<Game>() ?: return@runTransaction

            if (player.farmerType == "Comerciante Sombrío" && player.glitchEnergy >= 1 && !player.hasUsedActiveSkill) {
                player.glitchEnergy -= 1
                player.hasUsedActiveSkill = true
                game.temporaryPriceBoosts[cropId] = (game.temporaryPriceBoosts[cropId] ?: 0) + 2
                transaction.set(playerRef, player)
                transaction.set(gameRef, game)
            }
        }.await()
    }

    suspend fun applyMerchantBonusAndFinishMarket(gameId: String, playerId: String, cropsSoldCount: Int) {
        val playerRef = playersCollection.document(playerId)
        val gameRef = gamesCollection.document(gameId)
        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: return@runTransaction
            val game = transaction.get(gameRef).toObject<Game>() ?: return@runTransaction

            if (player.farmerType == "Comerciante Sombrío") {
                val bonus = cropsSoldCount / 3
                player.money += bonus
            }

            if (!game.playersFinishedMarket.contains(playerId)) {
                game.playersFinishedMarket.add(playerId)
            }
            transaction.set(playerRef, player)
            transaction.set(gameRef, game)
        }.await()
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

            val encounter = allMysteryEncounters.find { it.id == player.activeMysteryId } as? MinigameEncounter
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
        var plantCount = 0
        var growthCount = 0
        val feedbackMessage = StringBuilder("Efectos de los dados aplicados:\n")

        for (symbol in diceResults) {
            when (symbol) {
                DadoSimbolo.MONEDA -> moneyChange += 2
                DadoSimbolo.ENERGIA -> energyChange += 1
                DadoSimbolo.MISTERIO -> mysteryEventsCount += 1
                DadoSimbolo.PLANTAR -> plantCount += 1
                DadoSimbolo.CRECIMIENTO -> growthCount += 1
                else -> {}
            }
        }

        db.runTransaction { transaction ->
            val player = transaction.get(playerRef).toObject<Player>() ?: throw Exception("Player not found")
            player.money += moneyChange
            player.glitchEnergy += energyChange
            player.rollPhase = 2
            player.mysteryButtonsRemaining = mysteryEventsCount
            player.activeMysteryId = null
            player.lastMysteryResult = null
            transaction.set(playerRef, player)
        }.await()

        if (moneyChange > 0) feedbackMessage.append("Ganaste $moneyChange monedas.💰\n")
        if (energyChange > 0) feedbackMessage.append("Ganaste $energyChange energía Glitch.⚡\n")
        if (plantCount > 0) feedbackMessage.append("Puedes plantar $plantCount cultivo(s).🌱\n")
        if (growthCount > 0) feedbackMessage.append("Puedes aplicar $growthCount crecimiento(s).➕\n")
        if (diceResults.any { it == DadoSimbolo.GLITCH }) feedbackMessage.append("Obtuviste dados Glitch (resuelve físicamente).🌀\n")
        if (mysteryEventsCount > 0) feedbackMessage.append("Obtuviste $mysteryEventsCount dados de Misterio (resuelve en la app).❓\n")

        return feedbackMessage.toString()
    }

    /**
     * Allows a player to sell a crop from their inventory.
     */
    suspend fun sellCrop(playerId: String, cropId: String, quantity: Int, marketPrice: Int): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>() ?: throw Exception("Player not found")
                val inventoryItem = player.inventario.find { it.id == cropId }
                if (inventoryItem == null || inventoryItem.cantidad < quantity) {
                    throw Exception("Insufficient inventory")
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
                val player = transaction.get(playerRef).toObject<Player>() ?: throw Exception("Player not found")
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
     * Removes a specified quantity of a crop from a player's inventory (Host action).
     */
    suspend fun removeCropFromInventory(playerId: String, cropId: String, quantityToRemove: Int): Boolean {
        val playerRef = playersCollection.document(playerId)
        return try {
            db.runTransaction { transaction ->
                val player = transaction.get(playerRef).toObject<Player>()
                    ?: throw Exception("Player not found for inventory adjustment.")

                val inventoryItem = player.inventario.find { it.id == cropId }
                    ?: throw Exception("Crop not found in player's inventory.")

                if (inventoryItem.cantidad < quantityToRemove) {
                    throw Exception("Cannot remove more crops than the player has.")
                }

                inventoryItem.cantidad -= quantityToRemove

                if (inventoryItem.cantidad == 0) {
                    player.inventario.remove(inventoryItem)
                }

                transaction.set(playerRef, player)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("GameService", "Failed to remove crop from inventory for player $playerId: ${e.message}", e)
            false
        }
    }

    /**
     * Advances the turn to the next player in the game or switches to the market phase.
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

                val playerIds = game.playerIds.sorted()
                val allPlayersFinishedActions = game.playersFinishedTurn.size == playerIds.size

                if (allPlayersFinishedActions) {
                    game.roundPhase = "MARKET_PHASE"
                    game.playersFinishedTurn.clear()
                    game.currentPlayerTurnId = null
                    // La limpieza de estado de los jugadores se hace en advanceRound
                    transaction.set(gameRef, game)
                    "market_phase"
                } else {
                    val currentIndex = playerIds.indexOf(currentPlayerId)
                    if (currentIndex == -1) return@runTransaction null
                    val nextIndex = (currentIndex + 1) % playerIds.size
                    val nextPlayerId = playerIds[nextIndex]
                    game.currentPlayerTurnId = nextPlayerId

                    // Limpiar estado del jugador que termina
                    currentPlayer.currentDiceRoll = emptyList()
                    currentPlayer.rollPhase = 0
                    currentPlayer.hasUsedStandardReroll = false
                    currentPlayer.hasUsedFreeReroll = false
                    currentPlayer.hasUsedActiveSkill = false
                    currentPlayer.mysteryButtonsRemaining = 0
                    currentPlayer.activeMysteryId = null
                    currentPlayer.lastMysteryResult = null

                    transaction.set(gameRef, game)
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
     * Permite al host forzar el paso de turno de un jugador atascado.
     */
    suspend fun forceAdvanceTurn(gameId: String): String? {
        val gameRef = gamesCollection.document(gameId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                if (game == null) return@runTransaction null

                val stuckPlayerId = game.currentPlayerTurnId ?: return@runTransaction null
                val stuckPlayerRef = playersCollection.document(stuckPlayerId)
                val stuckPlayer = transaction.get(stuckPlayerRef).toObject<Player>()

                if (!game.playersFinishedTurn.contains(stuckPlayerId)) {
                    game.playersFinishedTurn.add(stuckPlayerId)
                }

                val playerIds = game.playerIds.sorted()
                val allPlayersFinishedActions = game.playersFinishedTurn.size == playerIds.size

                if (allPlayersFinishedActions) {
                    game.roundPhase = "MARKET_PHASE"
                    game.playersFinishedTurn.clear()
                    game.currentPlayerTurnId = null
                } else {
                    val currentIndex = playerIds.indexOf(stuckPlayerId)
                    if (currentIndex == -1) return@runTransaction null
                    val nextIndex = (currentIndex + 1) % playerIds.size
                    game.currentPlayerTurnId = playerIds[nextIndex]
                }

                if (stuckPlayer != null) {
                    stuckPlayer.currentDiceRoll = emptyList()
                    stuckPlayer.rollPhase = 0
                    stuckPlayer.hasUsedStandardReroll = false
                    stuckPlayer.hasUsedFreeReroll = false
                    stuckPlayer.hasUsedActiveSkill = false
                    stuckPlayer.mysteryButtonsRemaining = 0
                    stuckPlayer.activeMysteryId = null
                    stuckPlayer.lastMysteryResult = null
                    transaction.set(stuckPlayerRef, stuckPlayer)
                }

                transaction.set(gameRef, game)
                game.roundPhase
            }.await()
        } catch (e: Exception) {
            Log.e("GameService", "Error forcing turn advance in game $gameId: ${e.message}", e)
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
            Log.e("GameService", "Error marking player as finished market in game $gameId: ${e.message}", e)
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
     * Advances the game to the next round (host only).
     */
    suspend fun advanceRound(gameId: String): Boolean {
        val gameRef = gamesCollection.document(gameId)
        return try {
            db.runTransaction { transaction ->
                val game = transaction.get(gameRef).toObject<Game>()
                if (game == null || game.playersFinishedMarket.size != game.playerIds.size) return@runTransaction false

                val basePrices = allCrops.associate { it.id to it.valorVentaBase }
                val currentPrices = game.marketPrices
                val newMarketPrices = MarketPrices(
                    trigo = calculateNewPrice(currentPrices.trigo, basePrices["trigo"] ?: 3),
                    maiz = calculateNewPrice(currentPrices.maiz, basePrices["maiz"] ?: 4),
                    patata = calculateNewPrice(currentPrices.patata, basePrices["patata"] ?: 3),
                    tomateCuadrado = calculateNewPrice(currentPrices.tomateCuadrado, basePrices["tomate_cuadrado"] ?: 6),
                    maizArcoiris = calculateNewPrice(currentPrices.maizArcoiris, basePrices["maiz_arcoiris"] ?: 7),
                    brocoliCristal = calculateNewPrice(currentPrices.brocoliCristal, basePrices["brocoli_cristal"] ?: 6),
                    pimientoExplosivo = calculateNewPrice(currentPrices.pimientoExplosivo, basePrices["pimiento_explosivo"] ?: 8)
                )

                var newEvent: GlitchEvent? = null
                if (Random.nextDouble() < 0.60) {
                    val possibleEvents = if (game.supplyFailureActive) eventosGlitch.filter { it.name != "Fallo de Suministro" } else eventosGlitch
                    if (possibleEvents.isNotEmpty()) newEvent = possibleEvents.random()
                }

                game.marketPrices = newMarketPrices
                game.lastEvent = newEvent
                game.supplyFailureActive = newEvent?.name == "Fallo de Suministro" || game.supplyFailureActive
                game.signalInterferenceActive = newEvent?.name == "Interferencia de Señal"

                val playerIds = game.playerIds.sorted()
                val currentStartIndex = playerIds.indexOf(game.roundStartPlayerId ?: game.hostPlayerId)
                val nextStartIndex = (currentStartIndex + 1) % playerIds.size
                val newRoundStartPlayerId = playerIds[nextStartIndex]

                game.roundStartPlayerId = newRoundStartPlayerId
                game.currentPlayerTurnId = newRoundStartPlayerId
                game.roundPhase = "PLAYER_ACTIONS"
                game.playersFinishedMarket.clear()
                game.temporaryPriceBoosts.clear()

                playerIds.forEach { pId ->
                    val pRef = playersCollection.document(pId)
                    val p = transaction.get(pRef).toObject<Player>()
                    if (p != null) {
                        p.currentDiceRoll = emptyList()
                        p.rollPhase = 0
                        p.hasUsedStandardReroll = false
                        p.hasUsedFreeReroll = false
                        p.hasUsedActiveSkill = false
                        p.mysteryButtonsRemaining = 0
                        p.activeMysteryId = null
                        p.lastMysteryResult = null
                        transaction.set(pRef, p)
                    }
                }

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

                val basePrices = allCrops.associate { it.id to it.valorVentaBase }
                val currentPrices = game.marketPrices
                val newMarketPrices = MarketPrices(
                    trigo = calculateNewPrice(currentPrices.trigo, basePrices["trigo"] ?: 3),
                    maiz = calculateNewPrice(currentPrices.maiz, basePrices["maiz"] ?: 4),
                    patata = calculateNewPrice(currentPrices.patata, basePrices["patata"] ?: 3),
                    tomateCuadrado = calculateNewPrice(currentPrices.tomateCuadrado, basePrices["tomate_cuadrado"] ?: 6),
                    maizArcoiris = calculateNewPrice(currentPrices.maizArcoiris, basePrices["maiz_arcoiris"] ?: 7),
                    brocoliCristal = calculateNewPrice(currentPrices.brocoliCristal, basePrices["brocoli_cristal"] ?: 6),
                    pimientoExplosivo = calculateNewPrice(currentPrices.pimientoExplosivo, basePrices["pimiento_explosivo"] ?: 8)
                )

                var newEvent: GlitchEvent? = null
                if (Random.nextDouble() < 0.60) {
                    val possibleEvents = if (game.supplyFailureActive) eventosGlitch.filter { it.name != "Fallo de Suministro" } else eventosGlitch
                    if (possibleEvents.isNotEmpty()) newEvent = possibleEvents.random()
                }

                game.marketPrices = newMarketPrices
                game.lastEvent = newEvent
                game.supplyFailureActive = newEvent?.name == "Fallo de Suministro" || game.supplyFailureActive
                game.signalInterferenceActive = newEvent?.name == "Interferencia de Señal"
                game.roundPhase = "PLAYER_ACTIONS"
                game.temporaryPriceBoosts.clear()

                transaction.set(gameRef, game)
                true
            }.await()
        } catch (e: Exception) {
            false
        }
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
                            feedback = "¡Objetivo de Monedas completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback = "Necesitas ${objective.targetValue} monedas. Tienes ${player.money}."
                        }
                    }
                    "total_harvest" -> {
                        val totalHarvested = player.inventario.sumOf { it.cantidad }
                        if (totalHarvested >= objective.targetValue) {
                            canClaim = true
                            feedback = "¡Objetivo de Cosecha Total completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback = "Necesitas cosechar ${objective.targetValue} cultivos. Llevas $totalHarvested."
                        }
                    }
                    "specific_harvest" -> {
                        val specificCropCount = player.inventario.find { it.id == objective.targetCropId }?.cantidad ?: 0
                        if (specificCropCount >= objective.targetValue) {
                            canClaim = true
                            feedback = "¡Objetivo de Cosecha de ${objective.targetCropId?.replaceFirstChar { it.uppercaseChar() }} completado! Ganaste ${objective.rewardPV} PV."
                        } else {
                            feedback = "Necesitas ${objective.targetValue} ${objective.targetCropId?.replaceFirstChar { it.uppercaseChar() }}. Tienes $specificCropCount."
                        }
                    }
                    "dice_roll_all_same" -> {
                        val firstSymbol = player.currentDiceRoll.firstOrNull()
                        val allSame = firstSymbol != null && player.currentDiceRoll.all { it == firstSymbol }
                        if (allSame && player.currentDiceRoll.isNotEmpty()) {
                            canClaim = true
                            feedback = "¡Objetivo de Tirada Perfecta completado! Ganaste ${objective.rewardPV} PV."
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
            Log.e("GameService", "Error al reclamar objetivo $objectiveId para jugador $playerId: ${e.message}", e)
            "Error al reclamar objetivo: ${e.message}"
        }
    }
}
