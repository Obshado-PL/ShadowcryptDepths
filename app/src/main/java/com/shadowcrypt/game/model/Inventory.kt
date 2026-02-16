package com.shadowcrypt.game.model

data class Equipment(
    val weapon: Item? = null,
    val armor: Item? = null,
    val accessory: Item? = null
) {
    fun getSlot(slot: EquipSlot): Item? = when (slot) {
        EquipSlot.Weapon -> weapon
        EquipSlot.Armor -> armor
        EquipSlot.Accessory -> accessory
    }

    fun withSlot(slot: EquipSlot, item: Item?): Equipment = when (slot) {
        EquipSlot.Weapon -> copy(weapon = item)
        EquipSlot.Armor -> copy(armor = item)
        EquipSlot.Accessory -> copy(accessory = item)
    }

    val totalAtk: Int get() = listOfNotNull(weapon, armor, accessory).sumOf { it.atk }
    val totalDef: Int get() = listOfNotNull(weapon, armor, accessory).sumOf { it.def }
    val totalMag: Int get() = listOfNotNull(weapon, armor, accessory).sumOf { it.mag }
    val totalSpd: Int get() = listOfNotNull(weapon, armor, accessory).sumOf { it.spd }
    val totalHpBonus: Int get() = listOfNotNull(weapon, armor, accessory).sumOf { it.hpBonus }
}

data class Inventory(
    val items: List<Item> = emptyList(),
    val capacity: Int = 12
) {
    val isFull: Boolean get() = items.size >= capacity
    val freeSlots: Int get() = capacity - items.size
}
