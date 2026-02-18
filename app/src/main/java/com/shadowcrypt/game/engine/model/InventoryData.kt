package com.shadowcrypt.game.engine.model

data class InventoryData(
    val items: List<ItemData> = emptyList(),
    val equipment: EquipmentData = EquipmentData(),
    val maxSlots: Int = 16
) {
    val isFull: Boolean get() = items.size >= maxSlots

    fun addItem(item: ItemData): InventoryData? {
        if (isFull) return null
        return copy(items = items + item)
    }

    fun removeItem(itemId: Int): InventoryData {
        return copy(items = items.filter { it.id != itemId })
    }

    fun findItem(itemId: Int): ItemData? = items.find { it.id == itemId }
}
