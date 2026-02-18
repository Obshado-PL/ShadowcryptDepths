package com.shadowcrypt.game.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.engine.GameAction
import com.shadowcrypt.game.engine.GameEngine
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.haptic.HapticEvent
import com.shadowcrypt.game.sound.SfxEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _visualEvents = MutableSharedFlow<VisualEvent>(extraBufferCapacity = 16)
    val visualEvents: SharedFlow<VisualEvent> = _visualEvents.asSharedFlow()

    /** Whether the descend confirmation dialog is showing */
    private val _showDescendConfirm = MutableStateFlow(false)
    val showDescendConfirm: StateFlow<Boolean> = _showDescendConfirm.asStateFlow()

    /** Whether the combat log is expanded */
    private val _isCombatLogExpanded = MutableStateFlow(false)
    val isCombatLogExpanded: StateFlow<Boolean> = _isCombatLogExpanded.asStateFlow()

    /** Whether the minimap is visible */
    private val _isMinimapVisible = MutableStateFlow(true)
    val isMinimapVisible: StateFlow<Boolean> = _isMinimapVisible.asStateFlow()

    /** Enemy being inspected (long-press) */
    private val _inspectedEnemy = MutableStateFlow<InspectedEnemy?>(null)
    val inspectedEnemy: StateFlow<InspectedEnemy?> = _inspectedEnemy.asStateFlow()

    private var nextEventId = 0L

    init {
        viewModelScope.launch(Dispatchers.Default) {
            val initialState = GameEngine.createInitialState(classId, seed)
            _uiState.value = GameUiState.Playing(initialState)
        }
    }

    fun onAction(action: GameAction) {
        val current = _uiState.value
        if (current !is GameUiState.Playing) return

        // Intercept descend to show confirmation
        if (action is GameAction.DescendStairs) {
            _showDescendConfirm.value = true
            return
        }

        processAction(current, action)
    }

    fun confirmDescend() {
        _showDescendConfirm.value = false
        val current = _uiState.value
        if (current !is GameUiState.Playing) return
        processAction(current, GameAction.DescendStairs)
    }

    fun cancelDescend() {
        _showDescendConfirm.value = false
    }

    private fun processAction(current: GameUiState.Playing, action: GameAction) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldState = current.gameState
            val newState = GameEngine.processAction(oldState, action)

            detectAndFireEvents(oldState, newState)

            // Floor change -> descending overlay
            if (newState.player.floorNumber != oldState.player.floorNumber) {
                hapticManager.trigger(HapticEvent.DESCEND)
                soundManager.playSfx(SfxEvent.DESCEND)
                _uiState.value = GameUiState.Descending(newState.player.floorNumber)
                delay(800)
            }

            // Victory -> show victory message briefly, then game over (won)
            if (newState.isVictorious) {
                hapticManager.trigger(HapticEvent.VICTORY)
                soundManager.playSfx(SfxEvent.VICTORY)
                _uiState.value = GameUiState.Playing(newState)
                delay(2000)
                recordRunEnd(newState, won = true)
                return@launch
            }

            // Player death -> show death message briefly, then game over
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
        val killedEnemies = old.enemies.filter { oldE ->
            new.enemies.none { it.id == oldE.id }
        }
        if (killedEnemies.isNotEmpty()) {
            hapticManager.trigger(HapticEvent.PLAYER_ATTACK)
            soundManager.playSfx(SfxEvent.ATTACK)
            soundManager.playSfx(SfxEvent.ENEMY_DEATH)
            for (killed in killedEnemies) {
                val damage = maxOf(1, old.player.effectiveAttack - killed.defense / 2)
                emitVisualEvent(VisualEvent.DamageNumber(nextEventId++, killed.position, damage, DamageNumberType.DEALT))
                emitVisualEvent(VisualEvent.DamageFlash(nextEventId++, killed.position, FlashType.KILL))
            }
            // XP gain — sum the xpReward of all killed enemies
            val xpGain = killedEnemies.sumOf { it.xpReward }
            if (xpGain > 0) {
                emitVisualEvent(VisualEvent.DamageNumber(nextEventId++, new.player.position, xpGain, DamageNumberType.XP))
            }
        } else {
            // Enemy took damage but not killed
            for (newE in new.enemies) {
                val oldE = old.enemies.find { it.id == newE.id } ?: continue
                if (newE.hp < oldE.hp) {
                    val dmg = oldE.hp - newE.hp
                    hapticManager.trigger(HapticEvent.PLAYER_ATTACK)
                    soundManager.playSfx(SfxEvent.ATTACK)
                    emitVisualEvent(VisualEvent.DamageNumber(nextEventId++, newE.position, dmg, DamageNumberType.DEALT))
                }
            }
        }

        // Player took damage
        if (new.player.hp < old.player.hp) {
            val dmg = old.player.hp - new.player.hp
            hapticManager.trigger(HapticEvent.PLAYER_DAMAGE)
            soundManager.playSfx(SfxEvent.DAMAGE)
            emitVisualEvent(VisualEvent.DamageNumber(nextEventId++, new.player.position, dmg, DamageNumberType.RECEIVED))
            emitVisualEvent(VisualEvent.ScreenShake(nextEventId++, new.player.position, intensity = (dmg.toFloat() / new.player.effectiveMaxHp).coerceIn(0.3f, 1f)))
            emitVisualEvent(VisualEvent.DamageFlash(nextEventId++, new.player.position, FlashType.DAMAGE))
        }

        // Heal from potion
        if (new.player.hp > old.player.hp && new.player.inventory.items.size < old.player.inventory.items.size) {
            val heal = new.player.hp - old.player.hp
            emitVisualEvent(VisualEvent.DamageNumber(nextEventId++, new.player.position, heal, DamageNumberType.HEAL))
        }

        // Level up
        if (new.player.level > old.player.level) {
            hapticManager.trigger(HapticEvent.LEVEL_UP)
            soundManager.playSfx(SfxEvent.LEVEL_UP)
        }

        // Item pickup — loot particle
        val pickedUpItems = old.groundItems.filter { (_, pos) ->
            pos == new.player.position && new.groundItems.none { (item, p) -> p == pos && item.id == it.first.id }
        }
        if (pickedUpItems.isNotEmpty()) {
            hapticManager.trigger(HapticEvent.ITEM_PICKUP)
            soundManager.playSfx(SfxEvent.ITEM_PICKUP)
            for ((item, pos) in pickedUpItems) {
                val colorHex = when (item.rarity) {
                    com.shadowcrypt.game.engine.model.Rarity.COMMON -> 0xFFAAAAAA
                    com.shadowcrypt.game.engine.model.Rarity.UNCOMMON -> 0xFF44DD44
                    com.shadowcrypt.game.engine.model.Rarity.RARE -> 0xFF4488FF
                    com.shadowcrypt.game.engine.model.Rarity.EPIC -> 0xFFAA44FF
                    com.shadowcrypt.game.engine.model.Rarity.LEGENDARY -> 0xFFFFAA00
                }
                emitVisualEvent(VisualEvent.LootParticle(nextEventId++, pos, colorHex))
            }
        }
    }

    private fun emitVisualEvent(event: VisualEvent) {
        _visualEvents.tryEmit(event)
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

    fun toggleCombatLog() {
        _isCombatLogExpanded.value = !_isCombatLogExpanded.value
    }

    fun toggleMinimap() {
        _isMinimapVisible.value = !_isMinimapVisible.value
    }

    fun inspectAt(screenX: Float, screenY: Float, canvasWidth: Float, canvasHeight: Float) {
        val current = _uiState.value
        if (current !is GameUiState.Playing) return
        val gameState = current.gameState
        val player = gameState.player

        val tileSize = canvasWidth / 15f
        val viewportHeightTiles = (canvasHeight / tileSize).toInt()

        val camX = (player.position.x - 15 / 2)
            .coerceIn(0, maxOf(0, gameState.dungeon.width - 15))
        val camY = (player.position.y - viewportHeightTiles / 2)
            .coerceIn(0, maxOf(0, gameState.dungeon.height - viewportHeightTiles))

        val worldX = camX + (screenX / tileSize).toInt()
        val worldY = camY + (screenY / tileSize).toInt()

        val enemy = gameState.enemies.find { it.position.x == worldX && it.position.y == worldY }
        if (enemy != null) {
            _inspectedEnemy.value = InspectedEnemy(
                name = enemy.type.displayName,
                hp = enemy.hp,
                maxHp = enemy.maxHp,
                attack = enemy.attack,
                defense = enemy.defense,
                isBoss = enemy.type.isBoss,
                screenX = screenX,
                screenY = screenY
            )
        } else {
            _inspectedEnemy.value = null
        }
    }

    fun dismissInspect() {
        _inspectedEnemy.value = null
    }
}

data class InspectedEnemy(
    val name: String,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val isBoss: Boolean,
    val screenX: Float,
    val screenY: Float
)
