package com.shadowcrypt.game.model

enum class Rarity(val displayName: String, val statMultiplier: Float) {
    Common("Common", 1.0f),
    Uncommon("Uncommon", 1.25f),
    Rare("Rare", 1.6f),
    Epic("Epic", 2.0f),
    Legendary("Legendary", 2.5f)
}

enum class EquipSlot { Weapon, Armor, Accessory }

enum class ItemCategory { Weapon, Armor, Accessory, Consumable }

sealed class ItemEffect {
    data class Heal(val amount: Int) : ItemEffect()
    data class BuffAtk(val amount: Int, val turns: Int) : ItemEffect()
    data class BuffDef(val amount: Int, val turns: Int) : ItemEffect()
    data class BuffMag(val amount: Int, val turns: Int) : ItemEffect()
    data class BuffSpd(val amount: Int, val turns: Int) : ItemEffect()
    data object Teleport : ItemEffect()
    data object RevealMap : ItemEffect()
    data class FreezeEnemies(val turns: Int) : ItemEffect()
    data object CureStatus : ItemEffect()
    data class RestoreHunger(val amount: Int) : ItemEffect()
    data class RestoreTorch(val amount: Int) : ItemEffect()
}

data class ItemTemplate(
    val id: String,
    val displayName: String,
    val category: ItemCategory,
    val equipSlot: EquipSlot? = null,
    val baseAtk: Int = 0,
    val baseDef: Int = 0,
    val baseMag: Int = 0,
    val baseSpd: Int = 0,
    val baseHpBonus: Int = 0,
    val effect: ItemEffect? = null,
    val minFloor: Int = 1,
    val maxFloor: Int = 10,
    val dropWeight: Int = 10,
    val description: String = ""
)

data class Item(
    val id: Int,
    val templateId: String,
    val displayName: String,
    val category: ItemCategory,
    val equipSlot: EquipSlot? = null,
    val rarity: Rarity,
    val atk: Int = 0,
    val def: Int = 0,
    val mag: Int = 0,
    val spd: Int = 0,
    val hpBonus: Int = 0,
    val effect: ItemEffect? = null,
    val description: String = ""
)

data class FloorItem(
    val item: Item,
    val position: Position
)
