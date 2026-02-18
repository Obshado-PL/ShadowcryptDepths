package com.shadowcrypt.game.engine.item

import com.shadowcrypt.game.engine.model.EnemyType
import com.shadowcrypt.game.engine.model.ItemData
import com.shadowcrypt.game.engine.model.ItemType
import com.shadowcrypt.game.engine.model.Rarity
import kotlin.random.Random

object ItemGenerator {

    private const val ENEMY_DROP_CHANCE = 0.35

    fun rollDrop(
        enemyType: EnemyType,
        floorNumber: Int,
        nextItemId: Int,
        random: Random
    ): ItemData? {
        val dropChance = ENEMY_DROP_CHANCE + when (enemyType) {
            EnemyType.DEMON, EnemyType.SHADOW, EnemyType.GOLEM -> 0.25
            EnemyType.WRAITH, EnemyType.SPIDER, EnemyType.FIRE_IMP -> 0.10
            else -> 0.0
        }
        if (random.nextDouble() > dropChance) return null
        return generateItem(floorNumber, nextItemId, random)
    }

    fun generateBossDrop(floorNumber: Int, nextItemId: Int, random: Random): ItemData {
        val type = when (random.nextInt(3)) {
            0 -> ItemType.WEAPON
            1 -> ItemType.ARMOR
            else -> ItemType.ACCESSORY
        }
        return createItem(type, Rarity.LEGENDARY, nextItemId, random)
    }

    fun generateItem(floorNumber: Int, nextItemId: Int, random: Random): ItemData {
        val rarity = rollRarity(floorNumber, random)
        val type = rollItemType(random)
        return createItem(type, rarity, nextItemId, random)
    }

    private fun rollRarity(floorNumber: Int, random: Random): Rarity {
        val roll = random.nextInt(100)
        val table = rarityTable(floorNumber)
        var cumulative = 0
        for ((rarity, weight) in table) {
            cumulative += weight
            if (roll < cumulative) return rarity
        }
        return Rarity.COMMON
    }

    private fun rarityTable(floor: Int): List<Pair<Rarity, Int>> = when {
        floor <= 2 -> listOf(
            Rarity.COMMON to 60, Rarity.UNCOMMON to 28,
            Rarity.RARE to 10, Rarity.EPIC to 2, Rarity.LEGENDARY to 0
        )
        floor <= 4 -> listOf(
            Rarity.COMMON to 45, Rarity.UNCOMMON to 32,
            Rarity.RARE to 16, Rarity.EPIC to 6, Rarity.LEGENDARY to 1
        )
        floor <= 6 -> listOf(
            Rarity.COMMON to 30, Rarity.UNCOMMON to 30,
            Rarity.RARE to 24, Rarity.EPIC to 12, Rarity.LEGENDARY to 4
        )
        floor <= 8 -> listOf(
            Rarity.COMMON to 15, Rarity.UNCOMMON to 28,
            Rarity.RARE to 30, Rarity.EPIC to 18, Rarity.LEGENDARY to 9
        )
        else -> listOf(
            Rarity.COMMON to 5, Rarity.UNCOMMON to 20,
            Rarity.RARE to 32, Rarity.EPIC to 27, Rarity.LEGENDARY to 16
        )
    }

    private fun rollItemType(random: Random): ItemType {
        val roll = random.nextInt(100)
        return when {
            roll < 25 -> ItemType.WEAPON
            roll < 50 -> ItemType.ARMOR
            roll < 65 -> ItemType.ACCESSORY
            roll < 85 -> ItemType.POTION
            else -> ItemType.SCROLL
        }
    }

    private fun createItem(
        type: ItemType,
        rarity: Rarity,
        id: Int,
        random: Random
    ): ItemData = when (type) {
        ItemType.WEAPON -> createWeapon(rarity, id, random)
        ItemType.ARMOR -> createArmor(rarity, id, random)
        ItemType.ACCESSORY -> createAccessory(rarity, id, random)
        ItemType.POTION -> createPotion(rarity, id, random)
        ItemType.SCROLL -> createScroll(rarity, id, random)
    }

    private fun createWeapon(rarity: Rarity, id: Int, random: Random): ItemData {
        val (atkMin, atkMax, defMin, defMax, hpMin, hpMax) = when (rarity) {
            Rarity.COMMON -> Stats(1, 2, 0, 0, 0, 0)
            Rarity.UNCOMMON -> Stats(2, 4, 0, 1, 0, 0)
            Rarity.RARE -> Stats(4, 6, 1, 2, 0, 0)
            Rarity.EPIC -> Stats(6, 9, 2, 3, 0, 5)
            Rarity.LEGENDARY -> Stats(9, 12, 3, 4, 5, 10)
        }
        val name = ItemNames.randomName(ItemType.WEAPON, rarity, random)
        return ItemData(
            id = id,
            name = name,
            type = ItemType.WEAPON,
            rarity = rarity,
            attackBonus = rollRange(atkMin, atkMax, random),
            defenseBonus = rollRange(defMin, defMax, random),
            maxHpBonus = rollRange(hpMin, hpMax, random),
            description = "${rarity.displayName} weapon"
        )
    }

    private fun createArmor(rarity: Rarity, id: Int, random: Random): ItemData {
        val (atkMin, atkMax, defMin, defMax, hpMin, hpMax) = when (rarity) {
            Rarity.COMMON -> Stats(0, 0, 1, 2, 0, 5)
            Rarity.UNCOMMON -> Stats(0, 0, 2, 4, 5, 10)
            Rarity.RARE -> Stats(0, 1, 4, 6, 10, 15)
            Rarity.EPIC -> Stats(1, 2, 6, 9, 15, 25)
            Rarity.LEGENDARY -> Stats(2, 3, 9, 12, 25, 40)
        }
        val name = ItemNames.randomName(ItemType.ARMOR, rarity, random)
        return ItemData(
            id = id,
            name = name,
            type = ItemType.ARMOR,
            rarity = rarity,
            attackBonus = rollRange(atkMin, atkMax, random),
            defenseBonus = rollRange(defMin, defMax, random),
            maxHpBonus = rollRange(hpMin, hpMax, random),
            description = "${rarity.displayName} armor"
        )
    }

    private fun createAccessory(rarity: Rarity, id: Int, random: Random): ItemData {
        val (atkMin, atkMax, defMin, defMax, hpMin, hpMax) = when (rarity) {
            Rarity.COMMON -> Stats(1, 1, 1, 1, 3, 3)
            Rarity.UNCOMMON -> Stats(1, 2, 1, 2, 5, 8)
            Rarity.RARE -> Stats(2, 3, 2, 3, 8, 12)
            Rarity.EPIC -> Stats(3, 5, 3, 5, 12, 20)
            Rarity.LEGENDARY -> Stats(5, 7, 5, 7, 20, 30)
        }
        val name = ItemNames.randomName(ItemType.ACCESSORY, rarity, random)
        return ItemData(
            id = id,
            name = name,
            type = ItemType.ACCESSORY,
            rarity = rarity,
            attackBonus = rollRange(atkMin, atkMax, random),
            defenseBonus = rollRange(defMin, defMax, random),
            maxHpBonus = rollRange(hpMin, hpMax, random),
            description = "${rarity.displayName} accessory"
        )
    }

    private fun createPotion(rarity: Rarity, id: Int, random: Random): ItemData {
        val healAmount = when (rarity) {
            Rarity.COMMON -> 15
            Rarity.UNCOMMON -> 30
            Rarity.RARE -> 50
            Rarity.EPIC -> 999 // Full heal
            Rarity.LEGENDARY -> 999
        }
        val name = ItemNames.randomName(ItemType.POTION, rarity, random)
        val desc = if (healAmount >= 999) "Fully restores HP" else "Restores $healAmount HP"
        return ItemData(
            id = id,
            name = name,
            type = ItemType.POTION,
            rarity = rarity,
            healAmount = healAmount,
            description = desc
        )
    }

    private fun createScroll(rarity: Rarity, id: Int, random: Random): ItemData {
        val (damage, radius) = when (rarity) {
            Rarity.COMMON -> Pair(rollRange(5, 8, random), 2)
            Rarity.UNCOMMON -> Pair(rollRange(8, 15, random), 3)
            Rarity.RARE -> Pair(rollRange(12, 20, random), 4)
            Rarity.EPIC -> Pair(rollRange(20, 30, random), 4)
            Rarity.LEGENDARY -> Pair(rollRange(30, 45, random), 5)
        }
        val name = ItemNames.randomName(ItemType.SCROLL, rarity, random)
        return ItemData(
            id = id,
            name = name,
            type = ItemType.SCROLL,
            rarity = rarity,
            damageAmount = damage,
            damageRadius = radius,
            description = "Deals $damage damage in $radius-tile radius"
        )
    }

    private fun rollRange(min: Int, max: Int, random: Random): Int {
        if (min >= max) return min
        return min + random.nextInt(max - min + 1)
    }

    private data class Stats(
        val atkMin: Int, val atkMax: Int,
        val defMin: Int, val defMax: Int,
        val hpMin: Int, val hpMax: Int
    )
}
