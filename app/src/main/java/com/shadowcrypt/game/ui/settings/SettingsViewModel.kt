package com.shadowcrypt.game.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.model.GameSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val settingsRepository = ServiceLocator.settingsRepository

    val settings: StateFlow<GameSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameSettings()
        )

    fun toggleSound() {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(!settings.value.soundEnabled)
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            settingsRepository.setMusicEnabled(!settings.value.musicEnabled)
        }
    }

    fun toggleHaptics() {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(!settings.value.hapticsEnabled)
        }
    }
}
