package com.shadowcrypt.game.model

enum class RoomEventType {
    TreasureChest,
    Shrine,
    Fountain
}

data class Interactable(
    val position: Position,
    val type: RoomEventType,
    val used: Boolean = false
)
