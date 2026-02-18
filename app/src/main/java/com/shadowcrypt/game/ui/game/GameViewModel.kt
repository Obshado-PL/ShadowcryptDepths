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
import com.shadowcrypt.game.model.Rarity
import com.shadowcrypt.game.model.RoomType
import com.shadowcrypt.game.model.Skill
import com.shadowcrypt.game.model.Skills
import com.shadowcrypt.game.model.SkillTarget
import com.shadowcrypt.game.model.Visibility
import com.shadowcrypt.game.ui.theme.BossColor
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.RarityCommon
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityLegendary
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.RarityUncommon
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

    private val _canvasEffects = MutableStateFlow<List<CanvasEffect>>(emptyList())
    val canvasEffects: StateFlow<List<CanvasEffect>> = _canvasEffects.asStateFlow()

    private val _achievementToasts = MutableStateFlow<List<AchievementToast>>(emptyList())
    val achievementToasts: StateFlow<List<AchievementToast>> = _achievementToasts.asStateFlow()

    private val toastedAchievements = mutableSetOf<String>()
    private var lastSkillUsed: Skill? = null

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
            checkInGameAchievements(current, newState)
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

        lastSkillUsed = Skills.forClass(current.player.classId).find { it.id == skillId }

        viewModelScope.launch(Dispatchers.Default) {
            val newState = engine.castSkill(current, skillId)
            updateWithEvents(current, newState)
            lastSkillUsed = null
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
                    current.player.position, target, current.dungeon, maxDistance = 200
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

    // ===== Auto-Explore =====

    fun autoExplore() {
        val current = _gameState.value ?: return
        if (current.status != GameStatus.Playing || _showPauseMenu.value) return
        cancelAutoWalk()

        val target = findExploreTarget(current)
        if (target != null) {
            startAutoWalk(target)
        } else {
            _floatingTexts.value = _floatingTexts.value + FloatingText(
                "Nowhere to explore",
                current.player.position,
                androidx.compose.ui.graphics.Color.Gray,
                System.currentTimeMillis()
            )
        }
    }

    private fun findExploreTarget(state: GameState): Position? {
        val dungeon = state.dungeon
        val playerPos = state.player.position

        // Find frontier: explored/visible walkable tiles adjacent to Hidden tiles
        val frontier = mutableListOf<Position>()
        for ((pos, vis) in state.visibilityMap) {
            if (vis == Visibility.Hidden) continue
            if (!dungeon.isWalkable(pos)) continue
            val hasHiddenNeighbor = pos.cardinalNeighbors().any { neighbor ->
                dungeon.inBounds(neighbor) &&
                    (state.visibilityMap[neighbor] ?: Visibility.Hidden) == Visibility.Hidden
            }
            if (hasHiddenNeighbor) frontier.add(pos)
        }

        // Pick nearest frontier tile that has a valid path
        val sorted = frontier.sortedBy { it.distanceTo(playerPos) }
        for (candidate in sorted) {
            val path = Pathfinding.findPath(playerPos, candidate, dungeon, maxDistance = 200)
            if (path != null) return candidate
        }

        // No frontier found — head to stairs
        val stairsPath = Pathfinding.findPath(playerPos, dungeon.stairsDown, dungeon, maxDistance = 200)
        return if (stairsPath != null) dungeon.stairsDown else null
    }

    // ===== Event Detection =====

    fun removeExpiredFloatingTexts(currentTimeMs: Long, durationMs: Long = 800L) {
        _floatingTexts.value = _floatingTexts.value.filter {
            currentTimeMs - it.createdAtMs < durationMs
        }
    }

    fun removeExpiredCanvasEffects(currentTimeMs: Long) {
        _canvasEffects.value = _canvasEffects.value.filterNot { it.isExpired(currentTimeMs) }
    }

    fun removeExpiredToasts(currentTimeMs: Long) {
        _achievementToasts.value = _achievementToasts.value.filterNot { it.isExpired(currentTimeMs) }
    }

    fun undoLastMove() {
        val prev = previousState ?: return
        _gameState.value = prev
        previousState = null
        _floatingTexts.value = emptyList()
        _canvasEffects.value = emptyList()
    }

    private fun updateWithEvents(old: GameState, new: GameState) {
        previousState = old
        val events = detectEvents(old, new)
        fireEvents(events)
        createFloatingTexts(old, new)
        checkInGameAchievements(old, new)
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
        val newEffects = mutableListOf<CanvasEffect>()
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

        // Enemies killed (removed from list) → floating text + death effect
        val newEnemyIds = new.enemies.map { it.id }.toSet()
        for (oldEnemy in old.enemies) {
            if (oldEnemy.id !in newEnemyIds && oldEnemy.isAlive) {
                val text = if (isCrit) "CRIT -${oldEnemy.hp}" else "-${oldEnemy.hp}"
                newFloats.add(FloatingText(text, oldEnemy.position, dmgColor, now))
                newEffects.add(
                    CanvasEffect.DeathEffect(
                        position = oldEnemy.position,
                        createdAtMs = now,
                        color = if (oldEnemy.isBoss) BossColor else EnemyColor,
                        enemyTypeId = oldEnemy.typeId,
                        isBoss = oldEnemy.isBoss
                    )
                )
            }
        }

        // Item pickup effects
        val newFloorItemIds = new.floorItems.map { it.item.id }.toSet()
        for (oldFloorItem in old.floorItems) {
            if (oldFloorItem.item.id !in newFloorItemIds) {
                val rarityColor = when (oldFloorItem.item.rarity) {
                    Rarity.Common -> RarityCommon
                    Rarity.Uncommon -> RarityUncommon
                    Rarity.Rare -> RarityRare
                    Rarity.Epic -> RarityEpic
                    Rarity.Legendary -> RarityLegendary
                }
                newEffects.add(
                    CanvasEffect.PickupEffect(
                        position = oldFloorItem.position,
                        createdAtMs = now,
                        color = rarityColor
                    )
                )
            }
        }

        // Skill visual effects
        val skill = lastSkillUsed
        if (skill != null) {
            val skillColor = when (skill.target) {
                SkillTarget.Self -> Color(0xFF44DDFF)
                SkillTarget.SingleEnemy -> Color(0xFFFFDD44)
                SkillTarget.AllEnemies -> Color(0xFF88AAFF)
                SkillTarget.AreaOfEffect -> Color(0xFFFF6600)
            }

            when (skill.target) {
                SkillTarget.Self -> {
                    newEffects.add(
                        CanvasEffect.SkillEffect(
                            position = new.player.position,
                            createdAtMs = now,
                            color = skillColor,
                            effectType = SkillEffectType.SelfGlow,
                            durationMs = 600L
                        )
                    )
                }
                SkillTarget.SingleEnemy -> {
                    val affectedPositions = findAffectedPositions(old, new)
                    for (pos in affectedPositions) {
                        newEffects.add(
                            CanvasEffect.SkillEffect(
                                position = pos,
                                createdAtMs = now,
                                color = skillColor,
                                effectType = SkillEffectType.ImpactFlash,
                                durationMs = 350L
                            )
                        )
                    }
                }
                SkillTarget.AllEnemies -> {
                    val affectedPositions = findAffectedPositions(old, new)
                    for (pos in affectedPositions) {
                        newEffects.add(
                            CanvasEffect.SkillEffect(
                                position = pos,
                                createdAtMs = now,
                                color = skillColor,
                                effectType = SkillEffectType.MultiFlash,
                                durationMs = 400L
                            )
                        )
                    }
                }
                SkillTarget.AreaOfEffect -> {
                    val affectedPositions = findAffectedPositions(old, new)
                    if (affectedPositions.isNotEmpty()) {
                        val centerX = affectedPositions.sumOf { it.x } / affectedPositions.size
                        val centerY = affectedPositions.sumOf { it.y } / affectedPositions.size
                        newEffects.add(
                            CanvasEffect.SkillEffect(
                                position = Position(centerX, centerY),
                                createdAtMs = now,
                                color = skillColor,
                                effectType = SkillEffectType.AreaCircle,
                                radius = skill.aoeRadius,
                                durationMs = 500L
                            )
                        )
                    }
                }
            }
        }

        if (newFloats.isNotEmpty()) {
            _floatingTexts.value = _floatingTexts.value + newFloats
        }
        if (newEffects.isNotEmpty()) {
            _canvasEffects.value = _canvasEffects.value + newEffects
        }
    }

    private fun findAffectedPositions(old: GameState, new: GameState): List<Position> {
        val newEnemyIds = new.enemies.map { it.id }.toSet()
        val damaged = new.enemies.filter { newE ->
            val oldE = old.enemies.find { it.id == newE.id }
            oldE != null && (newE.hp < oldE.hp || newE.activeBuffs.size > oldE.activeBuffs.size)
        }.map { it.position }
        val killed = old.enemies.filter { it.id !in newEnemyIds && it.isAlive }.map { it.position }
        return damaged + killed
    }

    private fun checkInGameAchievements(old: GameState, new: GameState) {
        val now = System.currentTimeMillis()
        val newToasts = mutableListOf<AchievementToast>()

        for (def in AchievementDefs.inGameAchievements) {
            if (def.id in toastedAchievements) continue
            if (def.checkInGame(old, new)) {
                toastedAchievements.add(def.id)
                newToasts.add(
                    AchievementToast(
                        id = def.id,
                        title = def.title,
                        emoji = def.emoji,
                        createdAtMs = now
                    )
                )
            }
        }

        if (newToasts.isNotEmpty()) {
            _achievementToasts.value = _achievementToasts.value + newToasts
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
