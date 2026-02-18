package com.shadowcrypt.game.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.engine.GameAction
import com.shadowcrypt.game.engine.GameEngine
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.haptic.HapticEvent
import com.shadowcrypt.game.sound.SfxEvent
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

    private val metaProgressRepository = ServiceLocator.metaProgressRepository
    private val hapticManager = ServiceLocator.hapticManager
    private val soundManager = ServiceLocator.soundManager

    private val _uiState = MutableStateFlow<GameUiState>(GameUiState.Loading)
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _isInventoryOpen = MutableStateFlow(false)
    val isInventoryOpen: StateFlow<Boolean> = _isInventoryOpen.asStateFlow()

    private val _newUnlocks = MutableStateFlow<Set<String>>(emptySet())
    val newUnlocks: StateFlow<Set<String>> = _newUnlocks.asStateFlow()

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
            val oldState = current.gameState
            val newState = GameEngine.processAction(oldState, action)

            detectAndFireEvents(oldState, newState)

            // Floor change → descending overlay
            if (newState.player.floorNumber != oldState.player.floorNumber) {
                hapticManager.trigger(HapticEvent.DESCEND)
                soundManager.playSfx(SfxEvent.DESCEND)
                _uiState.value = GameUiState.Descending(newState.player.floorNumber)
                delay(800)
            }

            // Victory → show victory message briefly, then game over (won)
            if (newState.isVictorious) {
                hapticManager.trigger(HapticEvent.VICTORY)
                soundManager.playSfx(SfxEvent.VICTORY)
                _uiState.value = GameUiState.Playing(newState)
                delay(2000)
                recordRunEnd(newState, won = true)
                return@launch
            }

            // Player death → show death message briefly, then game over
            if (newState.isPlayerDead) {
                hapticManager.trigger(HapticEvent.PLAYER_DEATH)
                soundManager.playSfx(SfxEvent.PLAYER_DEATH)
                _uiState.value = GameUiState.Playing(newState)
                delay(1500)
                recordRunEnd(newState, won = false)
                return@launch
            }

            _uiState.value = GameUiState.Playing(newState)
        }
    }

    private fun detectAndFireEvents(old: GameState, new: GameState) {
        // Player attacked (enemy killed or enemy HP decreased)
        if (new.player.enemiesKilled > old.player.enemiesKilled) {
            hapticManager.trigger(HapticEvent.PLAYER_ATTACK)
            soundManager.playSfx(SfxEvent.ATTACK)
            soundManager.playSfx(SfxEvent.ENEMY_DEATH)
        } else if (new.enemies.any { newE ->
                old.enemies.find { it.id == newE.id }?.let { it.hp > newE.hp } == true
            }
        ) {
            hapticManager.trigger(HapticEvent.PLAYER_ATTACK)
            soundManager.playSfx(SfxEvent.ATTACK)
        }

        // Player took damage
        if (new.player.hp < old.player.hp) {
            hapticManager.trigger(HapticEvent.PLAYER_DAMAGE)
            soundManager.playSfx(SfxEvent.DAMAGE)
        }

        // Level up
        if (new.player.level > old.player.level) {
            hapticManager.trigger(HapticEvent.LEVEL_UP)
            soundManager.playSfx(SfxEvent.LEVEL_UP)
        }

        // Item pickup
        if (new.player.inventory.items.size > old.player.inventory.items.size) {
            hapticManager.trigger(HapticEvent.ITEM_PICKUP)
            soundManager.playSfx(SfxEvent.ITEM_PICKUP)
        }
    }

    private suspend fun recordRunEnd(state: GameState, won: Boolean) {
        val unlocks = metaProgressRepository.recordRun(
            floorReached = state.player.floorNumber,
            enemiesKilled = state.player.enemiesKilled,
            won = won
        )
        _newUnlocks.value = unlocks

        _uiState.value = GameUiState.GameOver(
            floorReached = state.player.floorNumber,
            enemiesKilled = state.player.enemiesKilled,
            turnsTaken = state.turnCount,
            level = state.player.level,
            won = won
        )
    }

    fun toggleInventory() {
        _isInventoryOpen.value = !_isInventoryOpen.value
    }

    fun closeInventory() {
        _isInventoryOpen.value = false
    }
}
