package com.shadowcrypt.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.shadowcrypt.game.data.model.GameSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        GameSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            musicEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true
        )
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun updateMusicEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.MUSIC_ENABLED] = enabled }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }
}
