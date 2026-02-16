package com.shadowcrypt.game.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    data class Settings(
        val soundEnabled: Boolean = true,
        val soundVolume: Float = 0.8f,
        val hapticEnabled: Boolean = true,
        val tutorialSeen: Boolean = false,
        val difficulty: String = "Normal",
        val fontScale: Float = 1.0f,
        val highContrastMode: Boolean = false
    )

    private companion object {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val SOUND_VOLUME = floatPreferencesKey("sound_volume")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            soundEnabled = prefs[SOUND_ENABLED] ?: true,
            soundVolume = prefs[SOUND_VOLUME] ?: 0.8f,
            hapticEnabled = prefs[HAPTIC_ENABLED] ?: true,
            tutorialSeen = prefs[TUTORIAL_SEEN] ?: false,
            difficulty = prefs[DIFFICULTY] ?: "Normal",
            fontScale = prefs[FONT_SCALE] ?: 1.0f,
            highContrastMode = prefs[HIGH_CONTRAST] ?: false
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setSoundVolume(volume: Float) {
        context.dataStore.edit { it[SOUND_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    }

    suspend fun setTutorialSeen() {
        context.dataStore.edit { it[TUTORIAL_SEEN] = true }
    }

    suspend fun setDifficulty(difficulty: String) {
        context.dataStore.edit { it[DIFFICULTY] = difficulty }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE] = scale.coerceIn(0.8f, 1.5f) }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }

}
