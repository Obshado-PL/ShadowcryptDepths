package com.shadowcrypt.game.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.engine.GameAction
import com.shadowcrypt.game.engine.GameEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classId: String = savedStateHandle["classId"] ?: "warrior"
    private val seed: Long = savedStateHandle["seed"] ?: System.currentTimeMillis()

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _isInventoryOpen = MutableStateFlow(false)
    val isInventoryOpen: StateFlow<Boolean> = _isInventoryOpen.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val initialState = GameEngine.createInitialState(classId, seed)
            _uiState.value = GameUiState.Playing(initialState)
        }
    }

    fun onAction(action: GameAction) {
        val current = _uiState.value
        if (current !is GameUiState.Playing) return

        viewModelScope.launch(Dispatchers.Default) {
            val newState = GameEngine.processAction(current.gameState, action)

            // Floor change → descending overlay
            if (newState.player.floorNumber != current.gameState.player.floorNumber) {
                _uiState.value = GameUiState.Descending(newState.player.floorNumber)
                delay(800)
            }

            // Victory → show victory message briefly, then game over (won)
            if (newState.isVictorious) {
                _uiState.value = GameUiState.Playing(newState)
                delay(2000)
                _uiState.value = GameUiState.GameOver(
                    floorReached = newState.player.floorNumber,
                    enemiesKilled = newState.player.enemiesKilled,
                    turnsTaken = newState.turnCount,
                    level = newState.player.level,
                    won = true
                )
                return@launch
            }

            // Player death → show death message briefly, then game over
            if (newState.isPlayerDead) {
                _uiState.value = GameUiState.Playing(newState)
                delay(1500)
                _uiState.value = GameUiState.GameOver(
                    floorReached = newState.player.floorNumber,
                    enemiesKilled = newState.player.enemiesKilled,
                    turnsTaken = newState.turnCount,
                    level = newState.player.level
                )
                return@launch
            }

            _uiState.value = GameUiState.Playing(newState)
        }
    }

    fun toggleInventory() {
        _isInventoryOpen.value = !_isInventoryOpen.value
    }

    fun closeInventory() {
        _isInventoryOpen.value = false
    }
}
