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
        depth: Int,
        startSlope: Float,
        endSlope: Float,
        octant: Int,
        visible: MutableSet<Position>
    ) {
        if (startSlope < endSlope) return
        if (depth > viewRadius) return

        val xx = mult[octant][0]
        val xy = mult[octant][1]
        val yx = mult[octant][2]
        val yy = mult[octant][3]

        var nextStartSlope = startSlope

        for (j in depth..viewRadius) {
            var blocked = false
            for (col in j downTo 0) {
                val mapX = origin.x + col * xx + j * xy
                val mapY = origin.y + col * yx + j * yy
                val pos = Position(mapX, mapY)

                // Slope of the outer edge (closer to diagonal)
                val outerSlope = (col + 0.5f) / (j - 0.5f)
                // Slope of the inner edge (closer to axis)
                val innerSlope = (col - 0.5f) / (j + 0.5f)

                if (innerSlope > nextStartSlope) continue
                if (outerSlope < endSlope) break

                // Check if within circular radius
                val distSq = col * col + j * j
                if (distSq <= viewRadius * viewRadius && dungeon.inBounds(pos)) {
                    visible.add(pos)
                }

                val isBlocking = !dungeon.isTransparent(pos)

                if (blocked) {
                    if (isBlocking) {
                        nextStartSlope = innerSlope
                    } else {
                        blocked = false
                        nextStartSlope = innerSlope
                    }
                } else if (isBlocking) {
                    blocked = true
                    scanOctant(dungeon, origin, j + 1, nextStartSlope, outerSlope, octant, visible)
                    nextStartSlope = innerSlope
                }
            }
            if (blocked) break
        }
    }
}
