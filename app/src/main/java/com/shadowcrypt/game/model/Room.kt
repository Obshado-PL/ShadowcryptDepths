package com.shadowcrypt.game.model

/** A rectangular room within the dungeon. Coordinates define the floor area. */
data class Room(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val right: Int get() = x + width - 1
    val bottom: Int get() = y + height - 1
    val centerX: Int get() = x + width / 2
    val centerY: Int get() = y + height / 2
    val center: Position get() = Position(centerX, centerY)

    fun positions(): List<Position> =
        (y..bottom).flatMap { row ->
            (x..right).map { col -> Position(col, row) }
        }

    fun contains(pos: Position): Boolean =
        pos.x in x..right && pos.y in y..bottom

    fun overlaps(other: Room, buffer: Int = 1): Boolean =
        x - buffer <= other.right &&
                right + buffer >= other.x &&
                y - buffer <= other.bottom &&
                bottom + buffer >= other.y
}
