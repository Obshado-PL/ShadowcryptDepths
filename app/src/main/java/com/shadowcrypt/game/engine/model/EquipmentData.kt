package com.shadowcrypt.game.engine.model

data class EquipmentData(
    val weapon: ItemData? = null,
    val armor: ItemData? = null,
    val accessory: ItemData? = null
) {
    val totalAttackBonus: Int
        get() = (weapon?.attackBonus ?: 0) + (armor?.attackBonus ?: 0) + (accessory?.attackBonus ?: 0)

    val totalDefenseBonus: Int
        get() = (weapon?.defenseBonus ?: 0) + (armor?.defenseBonus ?: 0) + (accessory?.defenseBonus ?: 0)

    val totalMaxHpBonus: Int
        get() = (weapon?.maxHpBonus ?: 0) + (armor?.maxHpBonus ?: 0) + (accessory?.maxHpBonus ?: 0)

    fun getSlot(slot: EquipmentSlot): ItemData? = when (slot) {
        EquipmentSlot.WEAPON -> weapon
        EquipmentSlot.ARMOR -> armor
        EquipmentSlot.ACCESSORY -> accessory
    }

    fun equip(item: ItemData): Pair<EquipmentData, ItemData?> {
        val slot = item.type.toEquipmentSlot() ?: return Pair(this, null)
        val previousItem = getSlot(slot)
        val newEquipment = when (slot) {
            EquipmentSlot.WEAPON -> copy(weapon = item)
            EquipmentSlot.ARMOR -> copy(armor = item)
            EquipmentSlot.ACCESSORY -> copy(accessory = item)
        }
        return Pair(newEquipment, previousItem)
    }

    fun unequip(slot: EquipmentSlot): Pair<EquipmentData, ItemData?> {
        val item = getSlot(slot)
        val newEquipment = when (slot) {
            EquipmentSlot.WEAPON -> copy(weapon = null)
            EquipmentSlot.ARMOR -> copy(armor = null)
            EquipmentSlot.ACCESSORY -> copy(accessory = null)
        }
        return Pair(newEquipment, item)
    }
}

fun ItemType.toEquipmentSlot(): EquipmentSlot? = when (this) {
    ItemType.WEAPON -> EquipmentSlot.WEAPON
    ItemType.ARMOR -> EquipmentSlot.ARMOR
    ItemType.ACCESSORY -> EquipmentSlot.ACCESSORY
    else -> null
}
