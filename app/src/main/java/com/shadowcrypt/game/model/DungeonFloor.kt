package com.shadowcrypt.game.model

/** A complete generated dungeon floor. Grid uses [y][x] indexing: grid[row][col]. */
data class DungeonFloor(
    val floorNumber: Int,
    val theme: FloorTheme,
    val width: Int,
    val height: Int,
    val grid: Array<Array<Tile>>,
    val rooms: List<Room>,
    val playerStart: Position,
    val stairsDown: Position,
    val stairsUp: Position?
) {
    fun tileAt(pos: Position): Tile? =
        if (inBounds(pos)) grid[pos.y][pos.x] else null

    fun inBounds(pos: Position): Boolean =
        pos.x in 0 until width && pos.y in 0 until height

    fun isWalkable(pos: Position): Boolean =
        tileAt(pos)?.walkable == true

    fun isTransparent(pos: Position): Boolean =
        tileAt(pos)?.transparent == true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DungeonFloor) return false
        return floorNumber == other.floorNumber &&
                width == other.width && height == other.height
    }

    override fun hashCode(): Int =
        31 * floorNumber + 31 * width + height
}
