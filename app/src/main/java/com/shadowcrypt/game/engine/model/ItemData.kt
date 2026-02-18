package com.shadowcrypt.game.engine.model

data class ItemData(
    val id: Int,
    val name: String,
    val type: ItemType,
    val rarity: Rarity,
    val attackBonus: Int = 0,
    val defenseBonus: Int = 0,
    val maxHpBonus: Int = 0,
    val healAmount: Int = 0,
    val damageAmount: Int = 0,
    val damageRadius: Int = 0,
    val description: String = ""
)
