package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.DungeonFloor
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Visibility

/**
 * Field of View engine using Recursive Shadowcasting.
 * Processes 8 octants around the player independently.
 */
class FovEngine(private val viewRadius: Int = 24) {

    fun computeVisible(origin: Position, dungeon: DungeonFloor): Set<Position> {
        val visible = mutableSetOf(origin)

        for (octant in 0..7) {
            scanOctant(dungeon, origin, 1, 1.0f, 0.0f, octant, visible)
        }

        return visible.filter { dungeon.inBounds(it) }.toSet()
    }

    fun updateVisibilityMap(
        currentMap: Map<Position, Visibility>,
        newVisible: Set<Position>
    ): Map<Position, Visibility> {
        val updated = mutableMapOf<Position, Visibility>()

        for ((pos, vis) in currentMap) {
            updated[pos] = if (vis == Visibility.Visible) Visibility.Explored else vis
        }

        for (pos in newVisible) {
            updated[pos] = Visibility.Visible
        }

        return updated
    }

    // Octant coordinate transform multipliers: xx, xy, yx, yy
    private val mult = arrayOf(
        intArrayOf(1, 0, 0, 1),
        intArrayOf(0, 1, 1, 0),
        intArrayOf(0, -1, 1, 0),
        intArrayOf(-1, 0, 0, 1),
        intArrayOf(-1, 0, 0, -1),
        intArrayOf(0, -1, -1, 0),
        intArrayOf(0, 1, -1, 0),
        intArrayOf(1, 0, 0, -1)
    )

    private fun scanOctant(
        dungeon: DungeonFloor,
        origin: Position,
        row: Int,
        startSlope: Float,
        endSlope: Float,
        octant: Int,
        visible: MutableSet<Position>
    ) {
        if (startSlope < endSlope) return
        if (row > viewRadius) return

        val xx = mult[octant][0]
        val xy = mult[octant][1]
        val yx = mult[octant][2]
        val yy = mult[octant][3]

        var nextStartSlope = startSlope
        var blocked = false

        var distance = row
        while (distance <= viewRadius && !blocked) {
            val dy = -distance
            for (dx in -distance..0) {
                val mapX = origin.x + dx * xx + dy * xy
                val mapY = origin.y + dx * yx + dy * yy
                val pos = Position(mapX, mapY)

                val leftSlope = (dx - 0.5f) / (distance + 0.5f)
                val rightSlope = (dx + 0.5f) / (distance - 0.5f)

                if (nextStartSlope < rightSlope) continue
                if (endSlope > leftSlope) break

                // Check if within circular radius
                val distSq = dx * dx + distance * distance
                if (distSq <= viewRadius * viewRadius) {
                    if (dungeon.inBounds(pos)) {
                        visible.add(pos)
                    }
                }

                val isBlocking = !dungeon.isTransparent(pos)

                if (blocked) {
                    if (isBlocking) {
                        nextStartSlope = rightSlope
                    } else {
                        blocked = false
                    }
                } else if (isBlocking && distance < viewRadius) {
                    blocked = true
                    scanOctant(dungeon, origin, distance + 1, nextStartSlope, leftSlope, octant, visible)
                    nextStartSlope = rightSlope
                }
            }
            if (blocked) break
            distance++
        }
    }
}
