package com.shadowcrypt.game.model

import kotlin.math.abs
import kotlin.math.max

/** A single cell in the dungeon grid */
enum class Tile(val walkable: Boolean, val transparent: Boolean) {
    Floor(walkable = true, transparent = true),
    Wall(walkable = false, transparent = false),
    Door(walkable = true, transparent = false),
    StairsDown(walkable = true, transparent = true),
    StairsUp(walkable = true, transparent = true),
    Trap(walkable = true, transparent = true),
    Water(walkable = true, transparent = true),
    Lava(walkable = false, transparent = true),
    Pillar(walkable = false, transparent = false)
}

/** Grid coordinate using x = column, y = row */
data class Position(val x: Int, val y: Int) {

    fun move(direction: Direction): Position =
        Position(x + direction.dx, y + direction.dy)

    fun distanceTo(other: Position): Int =
        abs(x - other.x) + abs(y - other.y)

    fun chebyshevTo(other: Position): Int =
        max(abs(x - other.x), abs(y - other.y))

    fun cardinalNeighbors(): List<Position> =
        Direction.cardinal().map { move(it) }
}

/** Movement directions with delta offsets */
enum class Direction(val dx: Int, val dy: Int) {
    Up(0, -1),
    Down(0, 1),
    Left(-1, 0),
    Right(1, 0),
    UpLeft(-1, -1),
    UpRight(1, -1),
    DownLeft(-1, 1),
    DownRight(1, 1);

    companion object {
        fun cardinal(): List<Direction> = listOf(Up, Down, Left, Right)
    }
}

/** Fog of war visibility state for a tile */
enum class Visibility {
    Hidden,
    Explored,
    Visible
}
