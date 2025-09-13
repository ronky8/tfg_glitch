package com.pingu.tfg_glitch.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Extensión para obtener una instancia global de DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

class UserDataStore(private val context: Context) {
    companion object {
        private val GAME_ID_KEY = stringPreferencesKey("game_id")
        private val PLAYER_ID_KEY = stringPreferencesKey("player_id")
    }

    suspend fun saveSession(gameId: String, playerId: String) {
        context.dataStore.edit { preferences ->
            preferences[GAME_ID_KEY] = gameId
            preferences[PLAYER_ID_KEY] = playerId
        }
    }
    suspend fun readSession(): Pair<String, String>? {
        val preferences = context.dataStore.data.first()
        val gameId = preferences[GAME_ID_KEY]
        val playerId = preferences[PLAYER_ID_KEY]
        return if (gameId != null && playerId != null) {
            Pair(gameId, playerId)
        } else {
            null
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
