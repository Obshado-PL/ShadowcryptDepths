package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.ActiveBuff
import com.shadowcrypt.game.model.CharacterClass
import com.shadowcrypt.game.model.CombatResult
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.FloorItem
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.GameStatus
import com.shadowcrypt.game.model.Interactable
import com.shadowcrypt.game.model.Player
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Room
import com.shadowcrypt.game.model.RoomEventType
import com.shadowcrypt.game.model.StatusEffect
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private val dungeonGenerator: DungeonGenerator = DungeonGenerator(),
    private val fovEngine: FovEngine = FovEngine(viewRadius = 8)
) {

    fun newGame(classId: String, seed: Long, difficulty: Difficulty = Difficulty.Normal): GameState {
        ItemGenerator.resetIdCounter()

        val characterClass = CharacterClass.fromId(classId)
        val player = Player.create(characterClass)
        val dungeon = dungeonGenerator.generate(floorNumber = 1, seed = seed)

        val placedPlayer = player.copy(
            position = dungeon.playerStart,
            currentFloor = 1
        )

        val visible = fovEngine.computeVisible(placedPlayer.position, dungeon)
        val visibilityMap = fovEngine.updateVisibilityMap(emptyMap(), visible)

        val enemies = EnemySpawner.spawnForFloor(
            floorNumber = 1,
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            random = Random(seed + 2000L),
            difficulty = difficulty
        )

        val floorItems = ItemGenerator.generateFloorItems(
            floor = 1,
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            occupiedPositions = enemies.map { it.position }.toSet(),
            random = Random(seed + 3000L),
            difficulty = difficulty
        )

        val interactables = generateInteractables(
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            occupiedPositions = enemies.map { it.position }.toSet() +
                    floorItems.map { it.position }.toSet(),
            random = Random(seed + 4000L)
        )

        return GameState(
            dungeon = dungeon,
            player = placedPlayer,
            enemies = enemies,
            enemiesKilled = 0,
            visibilityMap = visibilityMap,
            turnCount = 0,
            status = GameStatus.Playing,
            messageLog = listOf(
                "You descend into the Shadowcrypt...",
                "Floor 1: ${dungeon.theme.displayName}",
                "Difficulty: ${difficulty.displayName}"
            ),
            seed = seed,
            floorItems = floorItems,
            interactables = interactables,
            difficulty = difficulty
        )
    }

    fun movePlayer(state: GameState, direction: Direction): GameState {
        if (state.status != GameStatus.Playing) return state

        val newPos = state.player.position.move(direction)

        if (!state.dungeon.isWalkable(newPos)) {
            return state.withMessage("You bump into a wall.")
        }

        // Check for bump attack
        val targetEnemy = state.enemies.find { it.position == newPos && it.isAlive }
        if (targetEnemy != null) {
            return resolveBumpAttack(state, targetEnemy)
        }

        // Move player
        val movedPlayer = state.player.copy(position = newPos)
        val visible = fovEngine.computeVisible(newPos, state.dungeon)
        val updatedVisibility = fovEngine.updateVisibilityMap(state.visibilityMap, visible)

        var newState = state.copy(
            player = movedPlayer,
            visibilityMap = updatedVisibility,
            turnCount = state.turnCount + 1,
            lastTrapTriggered = false,
            lastCritical = false
        )

        when (state.dungeon.tileAt(newPos)) {
            Tile.StairsDown -> newState = newState.withMessage("You see stairs going deeper...")
            Tile.StairsUp -> newState = newState.withMessage("Stairs lead back up.")
            Tile.Door -> newState = newState.withMessage("You pass through a doorway.")
            Tile.Trap -> {
                val trapDamage = (2 + state.player.currentFloor + state.difficulty.trapDamageBonus).coerceIn(2, 12)
                val newHp = (newState.player.hp - trapDamage).coerceAtLeast(0)
                newState = newState.copy(
                    player = newState.player.copy(hp = newHp),
                    lastTrapTriggered = true
                )
                newState = newState.withMessage("You triggered a trap! Took $trapDamage damage.")
                newState.dungeon.grid[newPos.y][newPos.x] = Tile.Floor
            }
            Tile.Water -> newState = newState.withMessage("You wade through water...")
            else -> {}
        }

        // Auto-pickup floor item
        val itemAtPos = newState.floorItems.find { it.position == newPos }
        if (itemAtPos != null) {
            newState = InventoryEngine.pickUpItem(newState, itemAtPos)
        }

        // Interact with room event
        val interactable = newState.interactables.find { it.position == newPos && !it.used }
        if (interactable != null) {
            newState = resolveInteraction(newState, interactable)
        }

        return endTurn(newState)
    }

    fun descendStairs(state: GameState): GameState {
        if (state.status != GameStatus.Playing) return state

        if (state.dungeon.tileAt(state.player.position) != Tile.StairsDown) {
            return state.withMessage("There are no stairs here.")
        }

        val nextFloor = state.player.currentFloor + 1

        if (nextFloor > 10) {
            return state.copy(
                status = GameStatus.Victory,
                messageLog = state.messageLog + "You have conquered the Shadowcrypt!"
            )
        }

        val newDungeon = dungeonGenerator.generate(
            floorNumber = nextFloor,
            seed = state.seed
        )

        val movedPlayer = state.player.copy(
            position = newDungeon.playerStart,
            currentFloor = nextFloor
        )

        val visible = fovEngine.computeVisible(movedPlayer.position, newDungeon)
        val visibilityMap = fovEngine.updateVisibilityMap(emptyMap(), visible)

        val newEnemies = EnemySpawner.spawnForFloor(
            floorNumber = nextFloor,
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            random = Random(state.seed + nextFloor * 2000L),
            difficulty = state.difficulty
        )

        val newFloorItems = ItemGenerator.generateFloorItems(
            floor = nextFloor,
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            occupiedPositions = newEnemies.map { it.position }.toSet(),
            random = Random(state.seed + nextFloor * 3000L),
            difficulty = state.difficulty
        )

        val newInteractables = generateInteractables(
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            occupiedPositions = newEnemies.map { it.position }.toSet() +
                    newFloorItems.map { it.position }.toSet(),
            random = Random(state.seed + nextFloor * 4000L)
        )

        return GameState(
            dungeon = newDungeon,
            player = movedPlayer,
            enemies = newEnemies,
            enemiesKilled = state.enemiesKilled,
            visibilityMap = visibilityMap,
            turnCount = state.turnCount,
            status = GameStatus.Playing,
            messageLog = state.messageLog + listOf(
                "You descend to floor $nextFloor...",
                newDungeon.theme.displayName
            ),
            seed = state.seed,
            floorItems = newFloorItems,
            interactables = newInteractables,
            difficulty = state.difficulty
        )
    }

    fun tapTile(state: GameState, target: Position): GameState {
        if (state.status != GameStatus.Playing) return state

        val visibility = state.visibilityMap[target]
        if (visibility != Visibility.Visible) return state

        // Ranged attack: tap on a visible enemy within attack range
        val targetEnemy = state.enemies.find { it.position == target && it.isAlive }
        if (targetEnemy != null) {
            val dist = state.player.position.distanceTo(target)
            val range = CharacterClass.fromId(state.player.classId).attackRange
            if (dist in 2..range) {
                return resolveRangedAttack(state, targetEnemy)
            }
        }

        val playerPos = state.player.position
        val dx = target.x - playerPos.x
        val dy = target.y - playerPos.y

        if (abs(dx) + abs(dy) == 1) {
            val direction = Direction.cardinal().find { it.dx == dx && it.dy == dy }
            if (direction != null) return movePlayer(state, direction)
        }

        val path = Pathfinding.findPath(playerPos, target, state.dungeon)
        if (path != null && path.isNotEmpty()) {
            val nextStep = path.first()
            val stepDx = nextStep.x - playerPos.x
            val stepDy = nextStep.y - playerPos.y
            val direction = Direction.cardinal().find { it.dx == stepDx && it.dy == stepDy }
            if (direction != null) return movePlayer(state, direction)
        }

        return state.withMessage("You can't reach there.")
    }

    // ===== Combat Integration =====

    private fun resolveBumpAttack(state: GameState, target: Enemy): GameState {
        return resolveAttack(state, target, "hit")
    }

    private fun resolveRangedAttack(state: GameState, target: Enemy): GameState {
        return resolveAttack(state, target, "cast at")
    }

    private fun resolveAttack(state: GameState, target: Enemy, verb: String): GameState {
        val combatRandom = Random(state.seed + state.turnCount * 31L)
        val result = CombatEngine.playerAttackEnemy(state.player, target, combatRandom)

        var newState = state.copy(
            turnCount = state.turnCount + 1,
            lastTrapTriggered = false,
            lastCritical = false
        )

        when (result) {
            is CombatResult.Miss -> {
                newState = newState.withMessage("You miss the ${target.displayName}!")
            }

            is CombatResult.Hit -> {
                newState = newState.copy(lastCritical = result.isCritical)
                val critText = if (result.isCritical) "CRITICAL! " else ""
                newState = newState.withMessage(
                    "${critText}You $verb ${target.displayName} for ${result.damage} damage."
                )

                if (result.defenderKilled) {
                    newState = newState.copy(
                        enemies = newState.enemies.filter { it.id != target.id },
                        enemiesKilled = newState.enemiesKilled + 1
                    ).withMessage("${target.displayName} is defeated!")

                    val (leveledPlayer, levelMsgs) = LevelingEngine.awardXp(
                        newState.player, target.xpReward
                    )
                    newState = newState.copy(player = leveledPlayer)
                    newState = newState.withMessage("+${target.xpReward} XP")
                    for (msg in levelMsgs) {
                        newState = newState.withMessage(msg)
                    }

                    // Loot drop
                    val lootRandom = Random(state.seed + state.turnCount * 41L + target.id)
                    val drop = ItemGenerator.rollEnemyDrop(
                        target, state.player.currentFloor, lootRandom, state.difficulty
                    )
                    if (drop != null) {
                        val floorItem = FloorItem(drop, target.position)
                        newState = newState.copy(floorItems = newState.floorItems + floorItem)
                        newState = newState.withMessage("${target.displayName} dropped ${drop.displayName}!")
                    }
                } else {
                    var updatedEnemy = target.copy(hp = result.defenderHpAfter)

                    // Roll for status effect on surviving enemy
                    val statusRandom = Random(state.seed + state.turnCount * 43L + target.id)
                    val statusBuff = CombatEngine.rollPlayerStatusEffect(
                        state.player.classId, statusRandom
                    )
                    if (statusBuff != null && updatedEnemy.activeBuffs.none {
                            it.statusEffect == statusBuff.statusEffect
                        }) {
                        updatedEnemy = updatedEnemy.copy(
                            activeBuffs = updatedEnemy.activeBuffs + statusBuff
                        )
                        val effectVerb = when (statusBuff.statusEffect) {
                            StatusEffect.Poison -> "poisoned"
                            StatusEffect.Burn -> "burning"
                            StatusEffect.Stun -> "stunned"
                            StatusEffect.Slow -> "slowed"
                            null -> "affected"
                        }
                        newState = newState.withMessage(
                            "${target.displayName} is $effectVerb!"
                        )
                    }

                    newState = newState.copy(
                        enemies = newState.enemies.map {
                            if (it.id == target.id) updatedEnemy else it
                        }
                    )
                }
            }
        }

        // Update FOV
        val visible = fovEngine.computeVisible(newState.player.position, newState.dungeon)
        val updatedVis = fovEngine.updateVisibilityMap(newState.visibilityMap, visible)
        newState = newState.copy(visibilityMap = updatedVis)

        return endTurn(newState)
    }

    private fun endTurn(state: GameState): GameState {
        // Enemy turns
        var s = processEnemyTurns(state)

        // Tick player DoT before decrementing turns
        val playerDot = s.player.activeBuffs
            .filter { it.statusEffect == StatusEffect.Poison || it.statusEffect == StatusEffect.Burn }
            .sumOf { it.dotDamage }
        if (playerDot > 0) {
            val newHp = max(0, s.player.hp - playerDot)
            s = s.copy(player = s.player.copy(hp = newHp))
                .withMessage("You take $playerDot damage from status effects!")
        }

        // Tick player buffs (decrement + remove expired)
        s = s.copy(player = InventoryEngine.tickBuffs(s.player))

        // Tick enemy DoT and buffs
        val updatedEnemies = s.enemies.map { enemy ->
            if (enemy.activeBuffs.isEmpty()) return@map enemy

            val dot = enemy.activeBuffs
                .filter { it.statusEffect == StatusEffect.Poison || it.statusEffect == StatusEffect.Burn }
                .sumOf { it.dotDamage }
            val newHp = if (dot > 0) max(0, enemy.hp - dot) else enemy.hp

            val tickedBuffs = enemy.activeBuffs
                .map { it.copy(turnsRemaining = it.turnsRemaining - 1) }
                .filter { it.turnsRemaining > 0 }

            enemy.copy(hp = newHp, activeBuffs = tickedBuffs)
        }

        // Handle enemies killed by DoT
        val dotKilled = updatedEnemies.filter { !it.isAlive && s.enemies.find { e -> e.id == it.id }?.isAlive == true }
        var newEnemies = updatedEnemies
        var player = s.player
        for (killed in dotKilled) {
            s = s.withMessage("${killed.displayName} succumbs to status effects!")
            newEnemies = newEnemies.filter { it.id != killed.id }
            val (leveledPlayer, levelMsgs) = LevelingEngine.awardXp(player, killed.xpReward)
            player = leveledPlayer
            s = s.withMessage("+${killed.xpReward} XP")
            for (msg in levelMsgs) {
                s = s.withMessage(msg)
            }
        }
        s = s.copy(enemies = newEnemies, player = player, enemiesKilled = s.enemiesKilled + dotKilled.size)

        // Check player death
        if (!s.player.isAlive) {
            s = s.copy(status = GameStatus.Dead)
                .withMessage("You have been slain...")
        }

        return s
    }

    private fun processEnemyTurns(state: GameState): GameState {
        val combatRandom = Random(state.seed + state.turnCount * 37L)
        return EnemyAI.processAllEnemies(state, combatRandom)
    }

    // ===== Room Events =====

    private fun generateInteractables(
        rooms: List<Room>,
        playerStart: Position,
        occupiedPositions: Set<Position>,
        random: Random
    ): List<Interactable> {
        val result = mutableListOf<Interactable>()
        val types = RoomEventType.entries.toTypedArray()

        for (room in rooms) {
            if (room.center == playerStart) continue
            if (random.nextInt(100) >= 30) continue // 30% chance per room

            val pos = room.center
            if (pos in occupiedPositions) continue

            val type = types[random.nextInt(types.size)]
            result.add(Interactable(pos, type))
        }

        return result
    }

    private fun resolveInteraction(state: GameState, interactable: Interactable): GameState {
        val marked = state.copy(
            interactables = state.interactables.map {
                if (it.position == interactable.position) it.copy(used = true) else it
            }
        )

        return when (interactable.type) {
            RoomEventType.TreasureChest -> {
                val lootRandom = Random(state.seed + state.turnCount * 47L)
                val drop = ItemGenerator.rollTreasureItem(state.player.currentFloor, lootRandom)
                val floorItem = FloorItem(drop, interactable.position)
                marked.copy(floorItems = marked.floorItems + floorItem)
                    .withMessage("You open a treasure chest! Found ${drop.displayName}!")
            }
            RoomEventType.Shrine -> {
                val shrineRandom = Random(state.seed + state.turnCount * 51L)
                val stats = listOf("ATK", "DEF", "MAG", "SPD")
                val stat = stats[shrineRandom.nextInt(stats.size)]
                val buff = when (stat) {
                    "ATK" -> ActiveBuff("Shrine ATK", atkBonus = 3, turnsRemaining = 30)
                    "DEF" -> ActiveBuff("Shrine DEF", defBonus = 3, turnsRemaining = 30)
                    "MAG" -> ActiveBuff("Shrine MAG", magBonus = 3, turnsRemaining = 30)
                    else -> ActiveBuff("Shrine SPD", spdBonus = 3, turnsRemaining = 30)
                }
                val newPlayer = marked.player.copy(
                    activeBuffs = marked.player.activeBuffs + buff
                )
                marked.copy(player = newPlayer)
                    .withMessage("The shrine grants you +3 $stat for 30 turns!")
            }
            RoomEventType.Fountain -> {
                val healAmount = min(
                    marked.player.effectiveMaxHp / 3,
                    marked.player.effectiveMaxHp - marked.player.hp
                )
                if (healAmount > 0) {
                    val newPlayer = marked.player.copy(hp = marked.player.hp + healAmount)
                    marked.copy(player = newPlayer)
                        .withMessage("You drink from the fountain. Restored $healAmount HP.")
                } else {
                    marked.withMessage("You drink from the fountain. You feel refreshed.")
                }
            }
        }
    }
}
