package com.pingu.tfg_glitch.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import android.util.Log


class FirestoreService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val playersCollection = db.collection("players")
    private val gamesCollection = db.collection("games")

    fun getPlayer(playerId: String): Flow<Player?> = callbackFlow {
        val docRef = playersCollection.document(playerId)
        val subscription = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreService", "Error listening to player $playerId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            // Asegurarse de que el ID del documento se asigna a la propiedad 'id' del objeto Player
            val player = snapshot?.toObject<Player>()?.copy(id = snapshot.id)
            trySend(player)
        }
        awaitClose { subscription.remove() }
    }

    fun getPlayersInGame(gameId: String): Flow<List<Player>> = callbackFlow {
        val query = playersCollection.whereEqualTo("gameId", gameId)
        val subscription = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreService", "Error listening to players in game $gameId: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }
            // Asegurarse de que el ID de cada documento se asigna a la propiedad 'id' de cada objeto Player
            val players = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<Player>()?.copy(id = doc.id)
            } ?: emptyList()
            trySend(players)
        }
        awaitClose { subscription.remove() }
    }

}
