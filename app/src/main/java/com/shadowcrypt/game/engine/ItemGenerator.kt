package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.FloorItem
import com.shadowcrypt.game.model.Item
import com.shadowcrypt.game.model.ItemEffect
import com.shadowcrypt.game.model.ItemTemplate
import com.shadowcrypt.game.model.ItemTemplates
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Rarity
import com.shadowcrypt.game.model.Room
import kotlin.random.Random

object ItemGenerator {

    private var nextItemId = 0

    fun resetIdCounter() {
        nextItemId = 0
    }

    fun generateItem(floor: Int, random: Random): Item {
        val templates = ItemTemplates.forFloor(floor)
        if (templates.isEmpty()) return createItem(ItemTemplates.HealthPotion, Rarity.Common)
        val template = weightedSelect(templates, random)
        val rarity = rollRarity(floor, random)
        return createItem(template, rarity)
    }

    fun generateFloorItems(
        floor: Int,
        rooms: List<Room>,
        playerStart: Position,
        occupiedPositions: Set<Position>,
        random: Random,
        difficulty: Difficulty = Difficulty.Normal
    ): List<FloorItem> {
        val floorItems = mutableListOf<FloorItem>()
        val usedPositions = occupiedPositions.toMutableSet()

        val playerRoom = rooms.find { it.contains(playerStart) }
        val dropChance = (15 * difficulty.dropRateMultiplier).toInt().coerceIn(5, 40)

        for (room in rooms) {
            if (room == playerRoom) continue
            if (random.nextInt(100) < dropChance) {
                val available = room.positions()
                    .filter { it !in usedPositions }
                if (available.isNotEmpty()) {
                    val pos = available[random.nextInt(available.size)]
                    val item = generateItem(floor, random)
                    floorItems.add(FloorItem(item, pos))
                    usedPositions.add(pos)
                }
            }
        }

        if (floor == 1 && floorItems.none { it.item.templateId == "health_potion" }) {
            val safeRooms = rooms.filter { it != playerRoom }
            if (safeRooms.isNotEmpty()) {
                val room = safeRooms[random.nextInt(safeRooms.size)]
                val available = room.positions().filter { it !in usedPositions }
                if (available.isNotEmpty()) {
                    val pos = available[random.nextInt(available.size)]
                    val potion = createItem(ItemTemplates.HealthPotion, Rarity.Common)
                    floorItems.add(FloorItem(potion, pos))
                }
            }
        }

        return floorItems
    }

    fun rollTreasureItem(floor: Int, random: Random): Item {
        val rarity = rollRarity(floor, random, minRarity = Rarity.Uncommon)
        val templates = ItemTemplates.forFloor(floor)
        val template = weightedSelect(templates, random)
        return createItem(template, rarity)
    }

    fun rollEnemyDrop(enemy: Enemy, floor: Int, random: Random, difficulty: Difficulty = Difficulty.Normal): Item? {
        val baseChance = if (enemy.isBoss) 100 else 25
        val dropChance = (baseChance * difficulty.dropRateMultiplier).toInt().coerceIn(5, 100)
        if (random.nextInt(100) >= dropChance) return null

        val rarity = if (enemy.isBoss) {
            rollRarity(floor, random, minRarity = Rarity.Rare)
        } else {
            rollRarity(floor, random)
        }

        val templates = ItemTemplates.forFloor(floor)
        if (templates.isEmpty()) return null
        val template = weightedSelect(templates, random)
        return createItem(template, rarity)
    }

    private fun createItem(
        template: ItemTemplate,
        rarity: Rarity
    ): Item {
        val mult = rarity.statMultiplier
        return Item(
            id = nextItemId++,
            templateId = template.id,
            displayName = if (rarity != Rarity.Common)
                "${rarity.displayName} ${template.displayName}"
            else template.displayName,
            category = template.category,
            equipSlot = template.equipSlot,
            rarity = rarity,
            atk = (template.baseAtk * mult).toInt(),
            def = (template.baseDef * mult).toInt(),
            mag = (template.baseMag * mult).toInt(),
            spd = (template.baseSpd * mult).toInt(),
            hpBonus = (template.baseHpBonus * mult).toInt(),
            effect = scaleEffect(template.effect, mult),
            description = template.description
        )
    }

    private fun scaleEffect(effect: ItemEffect?, mult: Float): ItemEffect? {
        return when (effect) {
            is ItemEffect.Heal -> ItemEffect.Heal((effect.amount * mult).toInt())
            is ItemEffect.BuffAtk -> ItemEffect.BuffAtk((effect.amount * mult).toInt(), effect.turns)
            is ItemEffect.BuffDef -> ItemEffect.BuffDef((effect.amount * mult).toInt(), effect.turns)
            is ItemEffect.BuffMag -> ItemEffect.BuffMag((effect.amount * mult).toInt(), effect.turns)
            is ItemEffect.BuffSpd -> ItemEffect.BuffSpd((effect.amount * mult).toInt(), effect.turns)
            is ItemEffect.Teleport -> effect
            is ItemEffect.RevealMap -> effect
            is ItemEffect.FreezeEnemies -> effect
            is ItemEffect.CureStatus -> effect
            null -> null
        }
    }

    private fun rollRarity(
        floor: Int,
        random: Random,
        minRarity: Rarity = Rarity.Common
    ): Rarity {
        val floorBonus = floor * 2
        val roll = random.nextInt(100)

        val rarity = when {
            roll < 2 + floorBonus / 2 -> Rarity.Legendary
            roll < 8 + floorBonus -> Rarity.Epic
            roll < 22 + floorBonus -> Rarity.Rare
            roll < 45 + floorBonus -> Rarity.Uncommon
            else -> Rarity.Common
        }

        return if (rarity.ordinal >= minRarity.ordinal) rarity else minRarity
    }

    private fun weightedSelect(templates: List<ItemTemplate>, random: Random): ItemTemplate {
        val totalWeight = templates.sumOf { it.dropWeight }
        if (totalWeight <= 0) return templates.first()
        var roll = random.nextInt(totalWeight)
        for (t in templates) {
            roll -= t.dropWeight
            if (roll < 0) return t
        }
        return templates.last()
    }
}
