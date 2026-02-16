package com.shadowcrypt.game.model

/** A rectangular room within the dungeon. Coordinates define the floor area. */
data class Room(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val type: RoomType = RoomType.Normal,
    val shape: RoomShape = RoomShape.Rectangular
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

    /** Returns walkable floor positions for this room's shape. */
    fun floorPositions(): List<Position> = when (shape) {
        RoomShape.Rectangular -> positions()
        RoomShape.Circular -> circularPositions()
        RoomShape.LShaped -> lShapedPositions()
        RoomShape.Cross -> crossPositions()
        RoomShape.Pillared -> pillaredPositions()
    }

    private fun circularPositions(): List<Position> {
        val cx = x + width / 2.0
        val cy = y + height / 2.0
        val rx = (width - 1) / 2.0
        val ry = (height - 1) / 2.0
        return (y..bottom).flatMap { row ->
            (x..right).mapNotNull { col ->
                val dx = (col - cx) / rx
                val dy = (row - cy) / ry
                if (dx * dx + dy * dy <= 1.0) Position(col, row) else null
            }
        }
    }

    private fun lShapedPositions(): List<Position> {
        val splitX = x + width / 2
        val splitY = y + height / 2
        return (y..bottom).flatMap { row ->
            (x..right).mapNotNull { col ->
                // Horizontal arm: full width, top half
                // Vertical arm: left half, full height
                if (row <= splitY || col <= splitX) Position(col, row) else null
            }
        }
    }

    private fun crossPositions(): List<Position> {
        val armWidth = (width / 3).coerceAtLeast(2)
        val armHeight = (height / 3).coerceAtLeast(2)
        val hStart = x + (width - armWidth) / 2
        val hEnd = hStart + armWidth - 1
        val vStart = y + (height - armHeight) / 2
        val vEnd = vStart + armHeight - 1
        return (y..bottom).flatMap { row ->
            (x..right).mapNotNull { col ->
                // Horizontal band (center rows, full width) OR vertical band (center cols, full height)
                if (row in vStart..vEnd || col in hStart..hEnd) Position(col, row) else null
            }
        }
    }

    private fun pillaredPositions(): List<Position> {
        return (y..bottom).flatMap { row ->
            (x..right).mapNotNull { col ->
                // Exclude pillar positions (every 3 tiles, offset 2 from edges, not on border)
                val isPillar = (row - y) >= 2 && (col - x) >= 2 &&
                        row < bottom && col < right &&
                        (row - y - 2) % 3 == 0 && (col - x - 2) % 3 == 0
                if (isPillar) null else Position(col, row)
            }
        }
    }

    fun contains(pos: Position): Boolean =
        pos.x in x..right && pos.y in y..bottom

    fun overlaps(other: Room, buffer: Int = 1): Boolean =
        x - buffer <= other.right &&
                right + buffer >= other.x &&
                y - buffer <= other.bottom &&
                bottom + buffer >= other.y
}
