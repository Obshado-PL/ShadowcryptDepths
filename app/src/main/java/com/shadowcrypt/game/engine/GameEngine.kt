package com.shadowcrypt.game.engine

import com.shadowcrypt.game.engine.ai.EnemyAi
import com.shadowcrypt.game.engine.combat.CombatEngine
import com.shadowcrypt.game.engine.dungeon.DungeonGenerator
import com.shadowcrypt.game.engine.dungeon.EnemySpawner
import com.shadowcrypt.game.engine.fov.Shadowcaster
import com.shadowcrypt.game.engine.item.ItemGenerator
import com.shadowcrypt.game.engine.item.ItemSpawner
import com.shadowcrypt.game.engine.model.Direction
import com.shadowcrypt.game.engine.model.EnemyData
import com.shadowcrypt.game.engine.model.EquipmentSlot
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.engine.model.ItemType
import com.shadowcrypt.game.engine.model.PlayerData
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.engine.model.Tile
import com.shadowcrypt.game.engine.model.Visibility
import com.shadowcrypt.game.engine.model.toEquipmentSlot
import kotlin.math.abs
import kotlin.random.Random

object GameEngine {

    private const val VISION_RADIUS = 8

    fun createInitialState(classId: String, seed: Long): GameState {
        val floorNumber = 1
        val random = Random(seed + floorNumber.toLong())
        val dungeon = DungeonGenerator.generate(
            floorNumber = floorNumber,
            random = random
        )

        val (baseHp, baseAtk, baseDef) = classBaseStats(classId)
        val player = PlayerData(
            position = dungeon.playerStart,
            hp = baseHp,
            maxHp = baseHp,
            attack = baseAtk,
            defense = baseDef,
            xp = 0,
            level = 1,
            xpToNextLevel = CombatEngine.xpForLevel(2),
            classId = classId,
            floorNumber = floorNumber,
            enemiesKilled = 0
        )

        val enemyRandom = Random(seed + floorNumber.toLong() * 1000)
        val enemies = EnemySpawner.spawnEnemies(
            rooms = dungeon.rooms,
            startRoomIndex = dungeon.startRoomIndex,
            floorNumber = floorNumber,
            tiles = dungeon.tiles,
            playerStart = dungeon.playerStart,
            random = enemyRandom
        )

        val itemRandom = Random(seed + floorNumber.toLong() * 2000)
        val occupiedPositions = enemies.map { it.position }.toSet() + dungeon.playerStart
        val groundItems = ItemSpawner.spawnGroundItems(
            rooms = dungeon.rooms,
            startRoomIndex = dungeon.startRoomIndex,
            floorNumber = floorNumber,
            tiles = dungeon.tiles,
            occupiedPositions = occupiedPositions,
            startingItemId = 0,
            random = itemRandom
        )

        val initialState = GameState(
            dungeon = dungeon,
            player = player,
            enemies = enemies,
            visibility = createBlankVisibility(dungeon.width, dungeon.height),
            turnCount = 0,
            seed = seed,
            groundItems = groundItems,
            nextItemId = groundItems.size
        )
        return recalculateFov(initialState)
    }

    fun processAction(state: GameState, action: GameAction): GameState {
        if (state.isPlayerDead) return state
        return when (action) {
            is GameAction.Move -> handleMove(state, action.direction)
            is GameAction.Wait -> handleWait(state)
            is GameAction.DescendStairs -> handleDescend(state)
            is GameAction.UseItem -> handleUseItem(state, action.itemId)
            is GameAction.EquipItem -> handleEquipItem(state, action.itemId)
            is GameAction.UnequipItem -> handleUnequipItem(state, action.slot)
            is GameAction.DropItem -> handleDropItem(state, action.itemId)
        }
    }

    private fun handleMove(state: GameState, direction: Direction): GameState {
        val newPos = state.player.position + direction.toPosition()

        if (newPos.x < 0 || newPos.x >= state.dungeon.width ||
            newPos.y < 0 || newPos.y >= state.dungeon.height
        ) {
            return state
        }

        // Bump-to-attack: check if enemy occupies target
        val targetEnemy = state.enemies.find { it.position == newPos }
        if (targetEnemy != null) {
            return handlePlayerAttack(state, targetEnemy)
        }

        val targetTile = state.dungeon.tiles[newPos.y][newPos.x]
        if (!targetTile.walkable) {
            return state
        }

        val newPlayer = state.player.copy(position = newPos)
        var movedState = state.copy(
            player = newPlayer,
            turnCount = state.turnCount + 1,
            message = tileMessage(targetTile)
        )

        // Auto-pickup ground items
        movedState = tryPickupItem(movedState)

        val fovState = recalculateFov(movedState)
        return EnemyAi.processEnemyTurns(fovState)
    }

    private fun tryPickupItem(state: GameState): GameState {
        val playerPos = state.player.position
        val itemOnGround = state.groundItems.find { it.second == playerPos } ?: return state

        val (item, _) = itemOnGround
        val newInventory = state.player.inventory.addItem(item)

        if (newInventory == null) {
            return state.copy(
                message = buildMessage(state.message, "Inventory full! Cannot pick up ${item.name}.")
            )
        }

        return state.copy(
            player = state.player.copy(inventory = newInventory),
            groundItems = state.groundItems.filter { it.first.id != item.id },
            message = buildMessage(state.message, "Picked up ${item.name} [${item.rarity.displayName}]")
        )
    }

    private fun handlePlayerAttack(state: GameState, enemy: EnemyData): GameState {
        val (updatedEnemy, damage, killed) = CombatEngine.playerAttacksEnemy(
            state.player, enemy
        )

        val messages = mutableListOf<String>()
        messages.add("You hit ${enemy.type.displayName} for $damage!")

        var newPlayer = state.player
        var newEnemies = state.enemies
        var newGroundItems = state.groundItems
        var newNextItemId = state.nextItemId

        if (killed) {
            newEnemies = newEnemies.filter { it.id != enemy.id }
            newPlayer = newPlayer.copy(
                xp = newPlayer.xp + updatedEnemy.xpReward,
                enemiesKilled = newPlayer.enemiesKilled + 1
            )
            messages.add("${enemy.type.displayName} defeated! +${updatedEnemy.xpReward} XP")

            // Item drop
            val dropRandom = Random(state.seed + state.turnCount.toLong() * 7 + enemy.id.toLong())
            val drop = ItemGenerator.rollDrop(
                enemyType = enemy.type,
                floorNumber = state.player.floorNumber,
                nextItemId = newNextItemId,
                random = dropRandom
            )
            if (drop != null) {
                newGroundItems = newGroundItems + Pair(drop, enemy.position)
                newNextItemId++
                messages.add("${enemy.type.displayName} dropped ${drop.name}!")
            }

            val (leveledPlayer, didLevel) = CombatEngine.checkLevelUp(newPlayer)
            newPlayer = leveledPlayer
            if (didLevel) {
                messages.add("Level up! Now level ${newPlayer.level}!")
            }
        } else {
            newEnemies = newEnemies.map {
                if (it.id == enemy.id) updatedEnemy else it
            }
        }

        val attackState = state.copy(
            player = newPlayer,
            enemies = newEnemies,
            groundItems = newGroundItems,
            nextItemId = newNextItemId,
            turnCount = state.turnCount + 1,
            message = messages.joinToString(" ")
        )

        val fovState = recalculateFov(attackState)
        return EnemyAi.processEnemyTurns(fovState)
    }

    private fun handleWait(state: GameState): GameState {
        val waitState = state.copy(
            turnCount = state.turnCount + 1,
            message = null
        )
        return EnemyAi.processEnemyTurns(waitState)
    }

    private fun handleDescend(state: GameState): GameState {
        val currentTile = state.dungeon.tiles[state.player.position.y][state.player.position.x]
        if (currentTile != Tile.STAIRS_DOWN) {
            return state.copy(message = "No stairs here.")
        }

        val newFloor = state.player.floorNumber + 1
        val random = Random(state.seed + newFloor.toLong())
        val newDungeon = DungeonGenerator.generate(
            floorNumber = newFloor,
            random = random
        )

        val newPlayer = state.player.copy(
            position = newDungeon.playerStart,
            floorNumber = newFloor
        )

        val enemyRandom = Random(state.seed + newFloor.toLong() * 1000)
        val newEnemies = EnemySpawner.spawnEnemies(
            rooms = newDungeon.rooms,
            startRoomIndex = newDungeon.startRoomIndex,
            floorNumber = newFloor,
            tiles = newDungeon.tiles,
            playerStart = newDungeon.playerStart,
            random = enemyRandom
        )

        val itemRandom = Random(state.seed + newFloor.toLong() * 2000)
        val occupiedByEnemies = newEnemies.map { it.position }.toSet() + newDungeon.playerStart
        val newGroundItems = ItemSpawner.spawnGroundItems(
            rooms = newDungeon.rooms,
            startRoomIndex = newDungeon.startRoomIndex,
            floorNumber = newFloor,
            tiles = newDungeon.tiles,
            occupiedPositions = occupiedByEnemies,
            startingItemId = state.nextItemId,
            random = itemRandom
        )

        val newState = GameState(
            dungeon = newDungeon,
            player = newPlayer,
            enemies = newEnemies,
            visibility = createBlankVisibility(newDungeon.width, newDungeon.height),
            turnCount = state.turnCount + 1,
            seed = state.seed,
            message = "You descend to floor $newFloor...",
            groundItems = newGroundItems,
            nextItemId = state.nextItemId + newGroundItems.size
        )

        return recalculateFov(newState)
    }

    private fun handleUseItem(state: GameState, itemId: Int): GameState {
        val item = state.player.inventory.findItem(itemId) ?: return state

        return when (item.type) {
            ItemType.POTION -> {
                val healAmount = if (item.healAmount >= 999) {
                    state.player.effectiveMaxHp - state.player.hp
                } else {
                    item.healAmount
                }
                val newHp = (state.player.hp + healAmount).coerceAtMost(state.player.effectiveMaxHp)
                val actualHeal = newHp - state.player.hp
                val newInventory = state.player.inventory.removeItem(itemId)
                state.copy(
                    player = state.player.copy(hp = newHp, inventory = newInventory),
                    message = "Used ${item.name}. Restored $actualHeal HP."
                )
            }
            ItemType.SCROLL -> {
                val playerPos = state.player.position
                var updatedEnemies = state.enemies
                val messages = mutableListOf("Used ${item.name}!")
                var newPlayer = state.player

                for (enemy in state.enemies) {
                    val dist = abs(playerPos.x - enemy.position.x) + abs(playerPos.y - enemy.position.y)
                    if (dist <= item.damageRadius) {
                        val newHp = (enemy.hp - item.damageAmount).coerceAtLeast(0)
                        if (newHp <= 0) {
                            updatedEnemies = updatedEnemies.filter { it.id != enemy.id }
                            newPlayer = newPlayer.copy(
                                xp = newPlayer.xp + enemy.xpReward,
                                enemiesKilled = newPlayer.enemiesKilled + 1
                            )
                            messages.add("${enemy.type.displayName} destroyed!")
                        } else {
                            updatedEnemies = updatedEnemies.map {
                                if (it.id == enemy.id) it.copy(hp = newHp) else it
                            }
                            messages.add("${enemy.type.displayName} takes ${item.damageAmount} damage!")
                        }
                    }
                }

                val (leveledPlayer, didLevel) = CombatEngine.checkLevelUp(newPlayer)
                if (didLevel) {
                    messages.add("Level up! Now level ${leveledPlayer.level}!")
                }

                val newInventory = leveledPlayer.inventory.removeItem(itemId)
                state.copy(
                    player = leveledPlayer.copy(inventory = newInventory),
                    enemies = updatedEnemies,
                    message = messages.joinToString(" ")
                )
            }
            else -> state
        }
    }

    private fun handleEquipItem(state: GameState, itemId: Int): GameState {
        val item = state.player.inventory.findItem(itemId) ?: return state
        if (item.type.toEquipmentSlot() == null) return state

        val inventoryWithoutItem = state.player.inventory.removeItem(itemId)
        val (newEquipment, previousItem) = inventoryWithoutItem.equipment.equip(item)

        var newInventory = inventoryWithoutItem.copy(equipment = newEquipment)
        if (previousItem != null) {
            newInventory = newInventory.addItem(previousItem) ?: newInventory
        }

        val effectiveMaxHp = state.player.maxHp + newEquipment.totalMaxHpBonus
        val newHp = state.player.hp.coerceAtMost(effectiveMaxHp)

        return state.copy(
            player = state.player.copy(inventory = newInventory, hp = newHp),
            message = "Equipped ${item.name}."
        )
    }

    private fun handleUnequipItem(state: GameState, slot: EquipmentSlot): GameState {
        val (newEquipment, removedItem) = state.player.inventory.equipment.unequip(slot)
        if (removedItem == null) return state

        val inventoryWithNewEquip = state.player.inventory.copy(equipment = newEquipment)
        val newInventory = inventoryWithNewEquip.addItem(removedItem)
            ?: return state.copy(message = "Inventory full! Cannot unequip.")

        val effectiveMaxHp = state.player.maxHp + newEquipment.totalMaxHpBonus
        val newHp = state.player.hp.coerceAtMost(effectiveMaxHp)

        return state.copy(
            player = state.player.copy(inventory = newInventory, hp = newHp),
            message = "Unequipped ${removedItem.name}."
        )
    }

    private fun handleDropItem(state: GameState, itemId: Int): GameState {
        val item = state.player.inventory.findItem(itemId) ?: return state
        val newInventory = state.player.inventory.removeItem(itemId)
        val newGroundItems = state.groundItems + Pair(item, state.player.position)

        return state.copy(
            player = state.player.copy(inventory = newInventory),
            groundItems = newGroundItems,
            message = "Dropped ${item.name}."
        )
    }

    private fun recalculateFov(state: GameState): GameState {
        val visiblePositions = Shadowcaster.computeFov(
            origin = state.player.position,
            radius = VISION_RADIUS,
            isOpaque = { pos ->
                pos.x < 0 || pos.x >= state.dungeon.width ||
                    pos.y < 0 || pos.y >= state.dungeon.height ||
                    !state.dungeon.tiles[pos.y][pos.x].transparent
            },
            width = state.dungeon.width,
            height = state.dungeon.height
        )

        val newVisibility = state.visibility.mapIndexed { y, row ->
            row.mapIndexed { x, current ->
                when {
                    Position(x, y) in visiblePositions -> Visibility.VISIBLE
                    current == Visibility.VISIBLE -> Visibility.EXPLORED
                    else -> current
                }
            }
        }

        return state.copy(visibility = newVisibility)
    }

    private fun createBlankVisibility(w: Int, h: Int): List<List<Visibility>> {
        return List(h) { List(w) { Visibility.UNEXPLORED } }
    }

    private fun classBaseStats(classId: String): Triple<Int, Int, Int> {
        return when (classId) {
            "warrior" -> Triple(120, 8, 5)
            "rogue" -> Triple(80, 12, 2)
            "mage" -> Triple(70, 14, 1)
            "cleric" -> Triple(100, 7, 4)
            else -> Triple(100, 8, 3)
        }
    }

    private fun tileMessage(tile: Tile): String? {
        return when (tile) {
            Tile.STAIRS_DOWN -> "You see stairs leading down..."
            Tile.STAIRS_UP -> "You see stairs leading up."
            Tile.DOOR -> "You pass through a door."
            else -> null
        }
    }

    private fun buildMessage(existing: String?, new: String): String {
        return if (existing != null) "$existing $new" else new
    }
}
