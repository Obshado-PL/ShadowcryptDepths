package com.shadowcrypt.game.data

import com.shadowcrypt.game.data.model.GameSettings
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {

    val settings: Flow<GameSettings> = dataStore.settingsFlow

    suspend fun setSoundEnabled(enabled: Boolean) = dataStore.updateSoundEnabled(enabled)
    suspend fun setMusicEnabled(enabled: Boolean) = dataStore.updateMusicEnabled(enabled)
    suspend fun setHapticsEnabled(enabled: Boolean) = dataStore.updateHapticsEnabled(enabled)
}
