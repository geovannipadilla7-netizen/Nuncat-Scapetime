package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nuncat_settings")

data class GamePreferences(
    val immersiveMode: Boolean = true,
    val keepScreenOn: Boolean = true,
)

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val IMMERSIVE_MODE = booleanPreferencesKey("immersive_mode")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val preferencesFlow: Flow<GamePreferences> = context.dataStore.data.map { prefs ->
        GamePreferences(
            immersiveMode = prefs[Keys.IMMERSIVE_MODE] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
        )
    }

    suspend fun setImmersiveMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IMMERSIVE_MODE] = enabled
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.KEEP_SCREEN_ON] = enabled
        }
    }
}
