package com.shadowcrypt.game.engine.fov

import com.shadowcrypt.game.engine.model.Position

object Shadowcaster {

    // Octant transformation multipliers: each row is (xx, xy, yx, yy)
    // Maps (col, row) in octant-local space to (dx, dy) in world space
    private val OCTANT_MULTIPLIERS = arrayOf(
        intArrayOf(1, 0, 0, 1),
        intArrayOf(0, 1, 1, 0),
        intArrayOf(0, -1, 1, 0),
        intArrayOf(-1, 0, 0, 1),
        intArrayOf(-1, 0, 0, -1),
        intArrayOf(0, -1, -1, 0),
        intArrayOf(0, 1, -1, 0),
        intArrayOf(1, 0, 0, -1)
    )

    fun computeFov(
        origin: Position,
        radius: Int,
        isOpaque: (Position) -> Boolean,
        width: Int,
        height: Int
    ): Set<Position> {
        val visible = mutableSetOf(origin)

        for (octant in 0..7) {
            castLight(
                visible = visible,
                origin = origin,
                radius = radius,
                isOpaque = isOpaque,
                width = width,
                height = height,
                row = 1,
                startSlope = 1.0f,
                endSlope = 0.0f,
                mult = OCTANT_MULTIPLIERS[octant]
            )
        }

        return visible
    }

    private fun castLight(
        visible: MutableSet<Position>,
        origin: Position,
        radius: Int,
        isOpaque: (Position) -> Boolean,
        width: Int,
        height: Int,
        row: Int,
        startSlope: Float,
        endSlope: Float,
        mult: IntArray
    ) {
        if (startSlope < endSlope) return

        var currentStart = startSlope
        val radiusSq = radius * radius

        for (j in row..radius) {
            var dx = -j - 1
            val dy = -j
            var blocked = false

            while (dx <= 0) {
                dx++
                // Transform octant-local coordinates to world coordinates
                val mapX = origin.x + dx * mult[0] + dy * mult[1]
                val mapY = origin.y + dx * mult[2] + dy * mult[3]

                // Skip out of bounds
                if (mapX < 0 || mapX >= width || mapY < 0 || mapY >= height) continue

                val leftSlope = (dx.toFloat() - 0.5f) / (dy.toFloat() + 0.5f)
                val rightSlope = (dx.toFloat() + 0.5f) / (dy.toFloat() - 0.5f)

                if (currentStart < rightSlope) continue
                if (endSlope > leftSlope) break

                // Check if within radius (circular FOV)
                if (dx * dx + dy * dy <= radiusSq) {
                    visible.add(Position(mapX, mapY))
                }

                val pos = Position(mapX, mapY)
                if (blocked) {
                    if (isOpaque(pos)) {
                        currentStart = rightSlope
                    } else {
                        blocked = false
                        // currentStart was already updated
                    }
                } else {
                    if (isOpaque(pos) && j < radius) {
                        blocked = true
                        castLight(
                            visible = visible,
                            origin = origin,
                            radius = radius,
                            isOpaque = isOpaque,
                            width = width,
                            height = height,
                            row = j + 1,
                            startSlope = currentStart,
                            endSlope = rightSlope,
                            mult = mult
                        )
                        currentStart = rightSlope
                    }
                }
            }
            if (blocked) break
        }
    }
}
