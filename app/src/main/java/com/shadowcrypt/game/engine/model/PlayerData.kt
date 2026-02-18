package com.shadowcrypt.game.engine.model

data class PlayerData(
    val position: Position,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val xp: Int = 0,
    val level: Int = 1,
    val xpToNextLevel: Int = 50,
    val classId: String,
    val floorNumber: Int,
    val enemiesKilled: Int = 0,
    val inventory: InventoryData = InventoryData()
) {
    val effectiveAttack: Int
        get() = attack + inventory.equipment.totalAttackBonus

    val effectiveDefense: Int
        get() = defense + inventory.equipment.totalDefenseBonus

    val effectiveMaxHp: Int
        get() = maxHp + inventory.equipment.totalMaxHpBonus
}
