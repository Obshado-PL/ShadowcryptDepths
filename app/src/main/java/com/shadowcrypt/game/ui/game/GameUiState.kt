package com.shadowcrypt.game.ui.game

import com.shadowcrypt.game.engine.model.GameState

sealed interface GameUiState {
    data object Loading : GameUiState
    data class Playing(val gameState: GameState) : GameUiState
    data class Descending(val floorNumber: Int) : GameUiState
    data class GameOver(
        val floorReached: Int,
        val enemiesKilled: Int,
        val turnsTaken: Int,
        val level: Int
    ) : GameUiState
}
