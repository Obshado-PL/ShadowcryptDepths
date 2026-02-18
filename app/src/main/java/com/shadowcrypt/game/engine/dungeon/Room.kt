package com.shadowcrypt.game.engine.dungeon

data class Room(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val centerX: Int get() = x + width / 2
    val centerY: Int get() = y + height / 2
    val right: Int get() = x + width - 1
    val bottom: Int get() = y + height - 1
}
