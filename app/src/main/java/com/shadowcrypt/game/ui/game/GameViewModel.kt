package com.shadowcrypt.game.ui.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.GameSaveManager
import com.shadowcrypt.game.engine.GameEngine
import com.shadowcrypt.game.engine.InventoryEngine
import com.shadowcrypt.game.engine.Pathfinding
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.model.EquipSlot
import com.shadowcrypt.game.model.GameEvent
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.GameStatus
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.RoomType
import com.shadowcrypt.game.model.Visibility
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.XpGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val engine = GameEngine()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showInventory = MutableStateFlow(false)
    val showInventory: StateFlow<Boolean> = _showInventory.asStateFlow()

    private val _showPauseMenu = MutableStateFlow(false)
    val showPauseMenu: StateFlow<Boolean> = _showPauseMenu.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts: StateFlow<List<FloatingText>> = _floatingTexts.asStateFlow()

    private var previousState: GameState? = null

    private val _playerFlashUntil = MutableStateFlow(0L)
    val playerFlashUntil: StateFlow<Long> = _playerFlashUntil.asStateFlow()

    private val _transitionFloor = MutableStateFlow(0)
    val transitionFloor: StateFlow<Int> = _transitionFloor.asStateFlow()

    private val _transitionTheme = MutableStateFlow<FloorTheme?>(null)
    val transitionTheme: StateFlow<FloorTheme?> = _transitionTheme.asStateFlow()

    private var autoWalkJob: Job? = null

    fun initGame(
        classId: String, seed: Long, difficulty: Difficulty = Difficulty.Normal,
        hpBonus: Int = 0, atkBonus: Int = 0, defBonus: Int = 0, magBonus: Int = 0, spdBonus: Int = 0,
        isDaily: Boolean = false
    ) {
        if (_gameState.value != null) return

        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            val state = engine.newGame(classId, seed, difficulty, hpBonus, atkBonus, defBonus, magBonus, spdBonus)
                .copy(isDaily = isDaily)
            _gameState.value = state
            _isLoading.value = false
            // Start ambient music for the initial floor theme
            ServiceLocator.ambientPlayer.apply {
                setTheme(state.dungeon.theme)
                start()
            }
        }
    }

    fun loadSavedGame(context: android.content.Context) {
        if (_gameState.value != null) return

        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            val state = GameSaveManager.load(context)
            if (state != null) {
                _gameState.value = state
            }
            _isLoading.value = false
        }
    }

    private fun autoSave() {
        val state = _gameState.value ?: return
        if (state.status != GameStatus.Playing) return
        if (state.isDaily) return  // No saving daily runs
        val context = ServiceLocator.appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            GameSaveManager.save(context, state)
        }
    }

    fun deleteSave() {
        val context = ServiceLocator.appContext ?: return
        GameSaveManager.deleteSave(context)
    }

    fun move(direction: Direction) {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.Playing || _showPauseMenu.value) return
        cancelAutoWalk()

        viewModelScope.launch(Dispatchers.Default) {
            val newState = engine.movePlayer(current, direction)
            updateWithEvents(current, newState)
        }
    }

    fun tapTile(position: Position) {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.Playing || _showPauseMenu.value) return
        cancelAutoWalk()

        val dist = current.player.position.distanceTo(position)
        if (dist <= 1) {
            viewModelScope.launch(Dispatchers.Default) {
                val newState = engine.tapTile(current, position)
                updateWithEvents(current, newState)
            }
        } else {
            startAutoWalk(position)
        }
    }

    fun descendStairs() {
        val current = _gameState.value ?: return
        if (_showPauseMenu.value) return
        cancelAutoWalk()

        viewModelScope.launch(Dispatchers.Default) {
            val nextFloor = current.player.currentFloor + 1
            _transitionFloor.value = nextFloor
            _transitionTheme.value = FloorTheme.forFloor(nextFloor)
            _isLoading.value = true
            val newState = engine.descendStairs(current)
            fireEvents(listOf(GameEvent.FloorDescend))
            _gameState.value = newState
            _isLoading.value = false
            // Crossfade ambient to new floor theme
            ServiceLocator.ambientPlayer.setTheme(newState.dungeon.theme)
            autoSave()
        }
    }

    fun toggleInventory() {
        cancelAutoWalk()
        _showInventory.value = !_showInventory.value
    }

    fun closeInventory() {
        _showInventory.value = false
    }

    fun togglePause() {
        cancelAutoWalk()
        _showPauseMenu.value = !_showPauseMenu.value
        if (_showPauseMenu.value) _showInventory.value = false
    }

    fun closePause() {
        _showPauseMenu.value = false
    }

    fun equipItem(itemId: Int) {
        val current = _gameState.value ?: return
        val newState = InventoryEngine.equipItem(current, itemId)
        if (newState.player.equipment != current.player.equipment) {
            fireEvents(listOf(GameEvent.ItemEquip))
        }
        _gameState.value = newState
    }

    fun unequipSlot(slot: EquipSlot) {
        val current = _gameState.value ?: return
        _gameState.value = InventoryEngine.unequipSlot(current, slot)
    }

    fun useItem(itemId: Int) {
        val current = _gameState.value ?: return
        val newState = InventoryEngine.useItem(current, itemId)
        if (newState.player.inventory.items.size < current.player.inventory.items.size) {
            fireEvents(listOf(GameEvent.ItemUse))
        }
        _gameState.value = newState
    }

    fun dropItem(itemId: Int) {
        val current = _gameState.value ?: return
        _gameState.value = InventoryEngine.dropItem(current, itemId)
    }

    fun useSkill(skillId: String) {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.Playing || _showPauseMenu.value) return
        cancelAutoWalk()

        viewModelScope.launch(Dispatchers.Default) {
            val newState = engine.castSkill(current, skillId)
            updateWithEvents(current, newState)
        }
    }

    // ===== Auto-Walk =====

    private fun startAutoWalk(target: Position) {
        autoWalkJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val current = _gameState.value ?: break
                if (current.status != GameStatus.Playing) break
                if (current.player.position == target) break

                val path = Pathfinding.findPath(
                    current.player.position, target, current.dungeon
                )
                if (path.isNullOrEmpty()) break

                val nextStep = path.first()
                val dx = nextStep.x - current.player.position.x
                val dy = nextStep.y - current.player.position.y
                val dir = Direction.cardinal().find { it.dx == dx && it.dy == dy } ?: break

                // Stop before bumping into an enemy
                val enemyAt = current.enemies.find { it.position == nextStep && it.isAlive }
                if (enemyAt != null) break

                val oldVisibleEnemies = countVisibleEnemies(current)
                val newState = engine.movePlayer(current, dir)
                updateWithEvents(current, newState)

                // Stop conditions after moving
                if (newState.player.hp < current.player.hp) break
                if (newState.floorItems.size < current.floorItems.size) break
                if (newState.lastTrapTriggered) break
                if (countVisibleEnemies(newState) > oldVisibleEnemies) break

                delay(120L)
            }
        }
    }

    private fun countVisibleEnemies(state: GameState): Int =
        state.enemies.count { it.isAlive && state.visibilityMap[it.position] == Visibility.Visible }

    private fun cancelAutoWalk() {
        autoWalkJob?.cancel()
        autoWalkJob = null
    }

    // ===== Event Detection =====

    fun removeExpiredFloatingTexts(currentTimeMs: Long, durationMs: Long = 800L) {
        _floatingTexts.value = _floatingTexts.value.filter {
            currentTimeMs - it.createdAtMs < durationMs
        }
    }

    fun undoLastMove() {
        val prev = previousState ?: return
        _gameState.value = prev
        previousState = null
        _floatingTexts.value = emptyList()
    }

    private fun updateWithEvents(old: GameState, new: GameState) {
        previousState = old
        val events = detectEvents(old, new)
        fireEvents(events)
        createFloatingTexts(old, new)
        _gameState.value = new
        autoSave()

        // Discover visible enemy types for bestiary
        val visibleEnemyTypes = new.enemies
            .filter { it.isAlive && new.visibilityMap[it.position] == Visibility.Visible }
            .map { it.typeId }
            .toSet()
        if (visibleEnemyTypes.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                ServiceLocator.metaProgressRepository.discoverEnemies(visibleEnemyTypes)
            }
        }
    }

    private fun createFloatingTexts(old: GameState, new: GameState) {
        val newFloats = mutableListOf<FloatingText>()
        val now = System.currentTimeMillis()

        // Player took damage → flash red
        if (new.player.hp < old.player.hp) {
            val damage = old.player.hp - new.player.hp
            newFloats.add(FloatingText("-$damage", new.player.position, HealthRed, now))
            _playerFlashUntil.value = now + 200L
        }

        // Detect if last hit was critical
        val isCrit = new.lastCritical

        // Damage type color based on player class
        val dmgColor = when {
            isCrit -> XpGold
            new.player.classId == "mage" -> Color(0xFF00DDFF)    // cyan for magic
            new.player.classId == "cleric" -> Color(0xFFFFDD44)  // golden for holy
            else -> Color.White                                    // white for physical
        }

        // Enemies took damage
        for (newEnemy in new.enemies) {
            val oldEnemy = old.enemies.find { it.id == newEnemy.id } ?: continue
            if (newEnemy.hp < oldEnemy.hp) {
                val damage = oldEnemy.hp - newEnemy.hp
                val text = if (isCrit) "CRIT -$damage" else "-$damage"
                newFloats.add(FloatingText(text, newEnemy.position, dmgColor, now))
            }
        }

        // Enemies killed (removed from list)
        val newEnemyIds = new.enemies.map { it.id }.toSet()
        for (oldEnemy in old.enemies) {
            if (oldEnemy.id !in newEnemyIds && oldEnemy.isAlive) {
                val text = if (isCrit) "CRIT -${oldEnemy.hp}" else "-${oldEnemy.hp}"
                newFloats.add(FloatingText(text, oldEnemy.position, dmgColor, now))
            }
        }

        if (newFloats.isNotEmpty()) {
            _floatingTexts.value = _floatingTexts.value + newFloats
        }
    }

    private fun detectEvents(old: GameState, new: GameState): List<GameEvent> {
        val events = mutableListOf<GameEvent>()

        // Terminal states
        if (new.status == GameStatus.Dead && old.status != GameStatus.Dead) {
            events.add(GameEvent.PlayerDeath)
            return events
        }
        if (new.status == GameStatus.Victory && old.status != GameStatus.Victory) {
            events.add(GameEvent.Victory)
            return events
        }

        // Player moved
        if (new.player.position != old.player.position) {
            events.add(GameEvent.PlayerMove)
        }

        // Player took damage
        if (new.player.hp < old.player.hp) {
            events.add(GameEvent.PlayerHit)
        }

        // Check if player attacked (enemy took damage or was killed)
        val enemyTookDamage = new.enemies.any { newEnemy ->
            val oldEnemy = old.enemies.find { it.id == newEnemy.id }
            oldEnemy != null && newEnemy.hp < oldEnemy.hp
        }
        val enemyKilled = new.enemiesKilled > old.enemiesKilled

        if (enemyTookDamage || enemyKilled) {
            events.add(GameEvent.PlayerAttack)
        }

        // Enemies killed
        if (enemyKilled) {
            val oldBossIds = old.enemies.filter { it.isBoss }.map { it.id }.toSet()
            val newBossIds = new.enemies.filter { it.isBoss }.map { it.id }.toSet()
            if (oldBossIds.size > newBossIds.size) {
                events.add(GameEvent.BossKilled)
            } else {
                events.add(GameEvent.EnemyKilled)
            }
        }

        // Player leveled up
        if (new.player.level > old.player.level) {
            events.add(GameEvent.LevelUp)
        }

        // Item picked up
        if (new.floorItems.size < old.floorItems.size) {
            events.add(GameEvent.ItemPickup)
        }

        // Trap triggered
        if (new.lastTrapTriggered) {
            events.add(GameEvent.TrapTriggered)
        }

        // Entered a special room type
        if (new.player.position != old.player.position) {
            val oldRoom = old.dungeon.rooms.find { it.contains(old.player.position) }
            val newRoom = new.dungeon.rooms.find { it.contains(new.player.position) }
            if (newRoom != null && newRoom != oldRoom && newRoom.type != RoomType.Normal) {
                val roomEvent = when (newRoom.type) {
                    RoomType.TreasureVault -> GameEvent.EnterTreasureVault
                    RoomType.Arena -> GameEvent.EnterArena
                    RoomType.TrapGauntlet -> GameEvent.EnterTrapGauntlet
                    RoomType.ShrineRoom -> GameEvent.EnterShrineRoom
                    RoomType.Library -> GameEvent.EnterLibrary
                    RoomType.Armory -> GameEvent.EnterArmory
                    RoomType.Normal -> null
                }
                if (roomEvent != null) events.add(roomEvent)
            }
        }

        return events
    }

    override fun onCleared() {
        super.onCleared()
        ServiceLocator.ambientPlayer.stop()
    }

    private fun fireEvents(events: List<GameEvent>) {
        for (event in events) {
            ServiceLocator.soundManager.play(event)
            ServiceLocator.hapticManager.play(event)
        }
    }
}
