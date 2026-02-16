package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.ActiveBuff
import com.shadowcrypt.game.model.EquipSlot
import com.shadowcrypt.game.model.FloorItem
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.ItemCategory
import com.shadowcrypt.game.model.ItemEffect
import com.shadowcrypt.game.model.Player
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.StatusEffect
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.math.min
import kotlin.random.Random

object InventoryEngine {

    fun pickUpItem(state: GameState, floorItem: FloorItem): GameState {
        val inv = state.player.inventory
        if (inv.isFull) {
            return state.withMessage("Inventory full! Drop something first.")
        }

        val newInventory = inv.copy(items = inv.items + floorItem.item)
        val newFloorItems = state.floorItems.filter { it.item.id != floorItem.item.id }
        val newPlayer = state.player.copy(inventory = newInventory)

        return state.copy(player = newPlayer, floorItems = newFloorItems)
            .withMessage("Picked up ${floorItem.item.displayName}.")
    }

    fun equipItem(state: GameState, itemId: Int): GameState {
        val player = state.player
        val item = player.inventory.items.find { it.id == itemId } ?: return state
        val slot = item.equipSlot ?: return state.withMessage("Can't equip that.")

        val currentlyEquipped = player.equipment.getSlot(slot)

        var newItems = player.inventory.items.filter { it.id != itemId }
        if (currentlyEquipped != null) {
            newItems = newItems + currentlyEquipped
        }

        val newEquipment = player.equipment.withSlot(slot, item)
        val newPlayer = player.copy(
            inventory = player.inventory.copy(items = newItems),
            equipment = newEquipment
        )

        val msg = if (currentlyEquipped != null) {
            "Equipped ${item.displayName}. Unequipped ${currentlyEquipped.displayName}."
        } else {
            "Equipped ${item.displayName}."
        }

        return state.copy(player = newPlayer).withMessage(msg)
    }

    fun unequipSlot(state: GameState, slot: EquipSlot): GameState {
        val player = state.player
        val item = player.equipment.getSlot(slot) ?: return state

        if (player.inventory.isFull) {
            return state.withMessage("Inventory full! Can't unequip.")
        }

        val newEquipment = player.equipment.withSlot(slot, null)
        val newInventory = player.inventory.copy(items = player.inventory.items + item)
        val newPlayer = player.copy(equipment = newEquipment, inventory = newInventory)

        return state.copy(player = newPlayer)
            .withMessage("Unequipped ${item.displayName}.")
    }

    fun useItem(state: GameState, itemId: Int): GameState {
        val player = state.player
        val item = player.inventory.items.find { it.id == itemId } ?: return state

        if (item.category != ItemCategory.Consumable) {
            return state.withMessage("Can't use that.")
        }

        val effect = item.effect ?: return state.withMessage("Can't use that.")

        val newItems = player.inventory.items.filter { it.id != itemId }
        var newPlayer = player.copy(inventory = player.inventory.copy(items = newItems))
        var msg = ""

        when (effect) {
            is ItemEffect.Heal -> {
                val effectiveMax = newPlayer.effectiveMaxHp
                val healAmount = min(effect.amount, effectiveMax - newPlayer.hp)
                newPlayer = newPlayer.copy(hp = newPlayer.hp + healAmount)
                msg = "Used ${item.displayName}. Restored $healAmount HP."
            }
            is ItemEffect.BuffAtk -> {
                val buff = ActiveBuff("ATK Up", atkBonus = effect.amount, turnsRemaining = effect.turns)
                newPlayer = newPlayer.copy(activeBuffs = newPlayer.activeBuffs + buff)
                msg = "Used ${item.displayName}. +${effect.amount} ATK for ${effect.turns} turns."
            }
            is ItemEffect.BuffDef -> {
                val buff = ActiveBuff("DEF Up", defBonus = effect.amount, turnsRemaining = effect.turns)
                newPlayer = newPlayer.copy(activeBuffs = newPlayer.activeBuffs + buff)
                msg = "Used ${item.displayName}. +${effect.amount} DEF for ${effect.turns} turns."
            }
            is ItemEffect.BuffMag -> {
                val buff = ActiveBuff("MAG Up", magBonus = effect.amount, turnsRemaining = effect.turns)
                newPlayer = newPlayer.copy(activeBuffs = newPlayer.activeBuffs + buff)
                msg = "Used ${item.displayName}. +${effect.amount} MAG for ${effect.turns} turns."
            }
            is ItemEffect.BuffSpd -> {
                val buff = ActiveBuff("SPD Up", spdBonus = effect.amount, turnsRemaining = effect.turns)
                newPlayer = newPlayer.copy(activeBuffs = newPlayer.activeBuffs + buff)
                msg = "Used ${item.displayName}. +${effect.amount} SPD for ${effect.turns} turns."
            }
            is ItemEffect.Teleport -> {
                val safePositions = mutableListOf<Position>()
                val dungeon = state.dungeon
                val enemyPositions = state.enemies.filter { it.isAlive }.map { it.position }.toSet()
                for (row in 0 until dungeon.height) {
                    for (col in 0 until dungeon.width) {
                        val pos = Position(col, row)
                        if (dungeon.isWalkable(pos) && pos != newPlayer.position
                            && pos !in enemyPositions
                        ) {
                            safePositions.add(pos)
                        }
                    }
                }
                if (safePositions.isNotEmpty()) {
                    val target = safePositions[Random(state.seed + state.turnCount * 53L)
                        .nextInt(safePositions.size)]
                    newPlayer = newPlayer.copy(position = target)
                    msg = "Used ${item.displayName}. Teleported to a new location!"
                } else {
                    msg = "Used ${item.displayName}. Nothing happened..."
                }
            }
            is ItemEffect.RevealMap -> {
                val dungeon = state.dungeon
                val revealed = mutableMapOf<Position, Visibility>()
                for (row in 0 until dungeon.height) {
                    for (col in 0 until dungeon.width) {
                        val pos = Position(col, row)
                        val current = state.visibilityMap[pos] ?: Visibility.Hidden
                        revealed[pos] = if (current == Visibility.Visible) Visibility.Visible
                        else Visibility.Explored
                    }
                }
                return state.copy(player = newPlayer, visibilityMap = revealed)
                    .withMessage("Used ${item.displayName}. The entire floor is revealed!")
            }
            is ItemEffect.FreezeEnemies -> {
                val stunBuff = ActiveBuff(
                    "Frozen", turnsRemaining = effect.turns,
                    statusEffect = StatusEffect.Stun
                )
                val visibleEnemyIds = state.enemies
                    .filter { it.isAlive && state.visibilityMap[it.position] == Visibility.Visible }
                    .map { it.id }.toSet()
                val frozenEnemies = state.enemies.map { enemy ->
                    if (enemy.id in visibleEnemyIds && enemy.activeBuffs.none {
                            it.statusEffect == StatusEffect.Stun
                        }) {
                        enemy.copy(activeBuffs = enemy.activeBuffs + stunBuff)
                    } else enemy
                }
                return state.copy(player = newPlayer, enemies = frozenEnemies)
                    .withMessage("Used ${item.displayName}. Visible enemies are frozen for ${effect.turns} turns!")
            }
            is ItemEffect.CureStatus -> {
                val curedBuffs = newPlayer.activeBuffs.filter { it.statusEffect == null }
                newPlayer = newPlayer.copy(activeBuffs = curedBuffs)
                msg = "Used ${item.displayName}. Status effects cured!"
            }
            is ItemEffect.RestoreHunger -> {
                val restored = min(effect.amount, newPlayer.maxHunger - newPlayer.hunger)
                newPlayer = newPlayer.copy(hunger = newPlayer.hunger + restored)
                msg = "Used ${item.displayName}. Restored $restored hunger."
            }
            is ItemEffect.RestoreTorch -> {
                val restored = min(effect.amount, newPlayer.maxTorchFuel - newPlayer.torchFuel)
                newPlayer = newPlayer.copy(torchFuel = newPlayer.torchFuel + restored)
                msg = "Used ${item.displayName}. Restored $restored torch fuel."
            }
        }

        return state.copy(player = newPlayer).withMessage(msg)
    }

    fun dropItem(state: GameState, itemId: Int): GameState {
        val player = state.player
        val item = player.inventory.items.find { it.id == itemId } ?: return state

        val newItems = player.inventory.items.filter { it.id != itemId }
        val newPlayer = player.copy(inventory = player.inventory.copy(items = newItems))
        val droppedItem = FloorItem(item, player.position)
        val newFloorItems = state.floorItems + droppedItem

        return state.copy(player = newPlayer, floorItems = newFloorItems)
            .withMessage("Dropped ${item.displayName}.")
    }

    fun tickBuffs(player: Player): Player {
        val updated = player.activeBuffs
            .map { it.copy(turnsRemaining = it.turnsRemaining - 1) }
            .filter { it.turnsRemaining > 0 }
        return player.copy(activeBuffs = updated)
    }
}
