package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.DungeonFloor
import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Room
import com.shadowcrypt.game.model.Tile
import kotlin.random.Random

/**
 * Generates dungeon floors using Binary Space Partitioning.
 * BSP ensures rooms never overlap and corridors connect all rooms.
 */
class DungeonGenerator {

    fun generate(floorNumber: Int, seed: Long): DungeonFloor {
        val random = Random(seed + floorNumber * 1000L)
        val theme = FloorTheme.forFloor(floorNumber)
        val width = theme.gridWidth
        val height = theme.gridHeight

        val grid = Array(height) { Array(width) { Tile.Wall } }

        val root = BspNode(1, 1, width - 2, height - 2)
        split(root, random, minLeafSize = theme.minRoomSize + 3)

        placeRoomsInLeaves(root, random, theme)

        val rooms = mutableListOf<Room>()
        collectRooms(root, rooms)

        if (rooms.isEmpty()) {
            // Fallback: place a single room in the center
            val fallbackRoom = Room(width / 4, height / 4, width / 2, height / 2)
            rooms.add(fallbackRoom)
            root.room = fallbackRoom
        }

        connectRooms(root, grid, random)
        rooms.forEach { carveRoom(grid, it) }
        placeDoors(grid, rooms)

        // Place stairs
        val firstRoom = rooms.first()
        val lastRoom = rooms.last()
        val playerStart = firstRoom.center
        val stairsDownPos = lastRoom.center
        grid[stairsDownPos.y][stairsDownPos.x] = Tile.StairsDown

        val stairsUpPos: Position? = if (floorNumber > 1) {
            // Offset from player start within the first room
            val candidates = listOf(
                Position(firstRoom.centerX + 1, firstRoom.centerY),
                Position(firstRoom.centerX - 1, firstRoom.centerY),
                Position(firstRoom.centerX, firstRoom.centerY + 1),
                Position(firstRoom.centerX, firstRoom.centerY - 1)
            ).filter { firstRoom.contains(it) && grid[it.y][it.x] == Tile.Floor }

            val upPos = candidates.firstOrNull() ?: Position(firstRoom.centerX, firstRoom.centerY + 1).also {
                if (firstRoom.contains(it)) grid[it.y][it.x] = Tile.Floor
            }
            grid[upPos.y][upPos.x] = Tile.StairsUp
            upPos
        } else null

        placeHazards(grid, floorNumber, theme, playerStart, stairsDownPos, stairsUpPos, random)

        val dungeon = DungeonFloor(
            floorNumber = floorNumber,
            theme = theme,
            width = width,
            height = height,
            grid = grid,
            rooms = rooms,
            playerStart = playerStart,
            stairsDown = stairsDownPos,
            stairsUp = stairsUpPos
        )

        // Validate connectivity: flood-fill from player start must reach stairs down
        if (!isReachable(dungeon, playerStart, stairsDownPos)) {
            // Regenerate with a different seed offset
            return generate(floorNumber, seed + 7919L)
        }

        return dungeon
    }

    // ===== BSP Tree =====

    private data class BspNode(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
        var left: BspNode? = null,
        var right: BspNode? = null,
        var room: Room? = null
    )

    private fun split(node: BspNode, random: Random, minLeafSize: Int, depth: Int = 0) {
        if (depth >= 5) return
        if (node.width < minLeafSize * 2 && node.height < minLeafSize * 2) return

        val splitHorizontally = when {
            node.width > node.height * 1.25 -> false
            node.height > node.width * 1.25 -> true
            else -> random.nextBoolean()
        }

        if (splitHorizontally) {
            if (node.height < minLeafSize * 2) return
            val splitAt = random.nextInt(minLeafSize, node.height - minLeafSize + 1)
            node.left = BspNode(node.x, node.y, node.width, splitAt)
            node.right = BspNode(node.x, node.y + splitAt, node.width, node.height - splitAt)
        } else {
            if (node.width < minLeafSize * 2) return
            val splitAt = random.nextInt(minLeafSize, node.width - minLeafSize + 1)
            node.left = BspNode(node.x, node.y, splitAt, node.height)
            node.right = BspNode(node.x + splitAt, node.y, node.width - splitAt, node.height)
        }

        split(node.left!!, random, minLeafSize, depth + 1)
        split(node.right!!, random, minLeafSize, depth + 1)
    }

    private fun placeRoomsInLeaves(node: BspNode, random: Random, theme: FloorTheme) {
        if (node.left == null && node.right == null) {
            // Leaf node — place a room
            val maxW = minOf(theme.maxRoomSize, node.width - 2)
            val maxH = minOf(theme.maxRoomSize, node.height - 2)
            val minW = minOf(theme.minRoomSize, maxW)
            val minH = minOf(theme.minRoomSize, maxH)

            if (maxW < minW || maxH < minH) return

            val roomW = random.nextInt(minW, maxW + 1)
            val roomH = random.nextInt(minH, maxH + 1)
            val roomX = node.x + random.nextInt(1, (node.width - roomW).coerceAtLeast(1) + 1)
            val roomY = node.y + random.nextInt(1, (node.height - roomH).coerceAtLeast(1) + 1)

            node.room = Room(roomX, roomY, roomW, roomH)
            return
        }

        node.left?.let { placeRoomsInLeaves(it, random, theme) }
        node.right?.let { placeRoomsInLeaves(it, random, theme) }
    }

    private fun collectRooms(node: BspNode, rooms: MutableList<Room>) {
        node.room?.let { rooms.add(it) }
        node.left?.let { collectRooms(it, rooms) }
        node.right?.let { collectRooms(it, rooms) }
    }

    private fun connectRooms(node: BspNode, grid: Array<Array<Tile>>, random: Random): Room? {
        if (node.room != null) return node.room

        val leftRoom = node.left?.let { connectRooms(it, grid, random) }
        val rightRoom = node.right?.let { connectRooms(it, grid, random) }

        if (leftRoom != null && rightRoom != null) {
            carveCorridor(grid, leftRoom.center, rightRoom.center, random)
        }

        return leftRoom ?: rightRoom
    }

    // ===== Carving =====

    private fun carveRoom(grid: Array<Array<Tile>>, room: Room) {
        for (row in room.y..room.bottom) {
            for (col in room.x..room.right) {
                if (row in grid.indices && col in grid[0].indices) {
                    grid[row][col] = Tile.Floor
                }
            }
        }
    }

    private fun carveCorridor(
        grid: Array<Array<Tile>>,
        from: Position,
        to: Position,
        random: Random
    ) {
        if (random.nextBoolean()) {
            carveHorizontalTunnel(grid, from.x, to.x, from.y)
            carveVerticalTunnel(grid, from.y, to.y, to.x)
        } else {
            carveVerticalTunnel(grid, from.y, to.y, from.x)
            carveHorizontalTunnel(grid, from.x, to.x, to.y)
        }
    }

    private fun carveHorizontalTunnel(grid: Array<Array<Tile>>, x1: Int, x2: Int, y: Int) {
        val start = minOf(x1, x2)
        val end = maxOf(x1, x2)
        for (x in start..end) {
            if (y in grid.indices && x in grid[0].indices) {
                if (grid[y][x] == Tile.Wall) {
                    grid[y][x] = Tile.Floor
                }
            }
        }
    }

    private fun carveVerticalTunnel(grid: Array<Array<Tile>>, y1: Int, y2: Int, x: Int) {
        val start = minOf(y1, y2)
        val end = maxOf(y1, y2)
        for (y in start..end) {
            if (y in grid.indices && x in grid[0].indices) {
                if (grid[y][x] == Tile.Wall) {
                    grid[y][x] = Tile.Floor
                }
            }
        }
    }

    // ===== Door Placement =====

    private fun placeDoors(grid: Array<Array<Tile>>, rooms: List<Room>) {
        for (room in rooms) {
            // Check the perimeter just outside the room
            for (col in (room.x - 1)..(room.right + 1)) {
                checkAndPlaceDoor(grid, col, room.y - 1)
                checkAndPlaceDoor(grid, col, room.bottom + 1)
            }
            for (row in (room.y - 1)..(room.bottom + 1)) {
                checkAndPlaceDoor(grid, room.x - 1, row)
                checkAndPlaceDoor(grid, room.right + 1, row)
            }
        }
    }

    private fun checkAndPlaceDoor(grid: Array<Array<Tile>>, x: Int, y: Int) {
        if (y !in grid.indices || x !in grid[0].indices) return
        if (grid[y][x] != Tile.Floor) return

        // A door candidate: floor tile with walls on opposite sides forming a chokepoint
        val wallAboveBelow = (y - 1 >= 0 && grid[y - 1][x] == Tile.Wall) &&
                (y + 1 < grid.size && grid[y + 1][x] == Tile.Wall)
        val wallLeftRight = (x - 1 >= 0 && grid[y][x - 1] == Tile.Wall) &&
                (x + 1 < grid[0].size && grid[y][x + 1] == Tile.Wall)

        if (wallAboveBelow || wallLeftRight) {
            grid[y][x] = Tile.Door
        }
    }

    // ===== Hazard Placement =====

    private fun placeHazards(
        grid: Array<Array<Tile>>,
        floorNumber: Int,
        theme: FloorTheme,
        playerStart: Position,
        stairsDown: Position,
        stairsUp: Position?,
        random: Random
    ) {
        val hazardCount = (2 + floorNumber).coerceAtMost(8)
        val excluded = mutableSetOf(playerStart, stairsDown)
        stairsUp?.let { excluded.add(it) }

        val candidates = mutableListOf<Position>()
        for (row in grid.indices) {
            for (col in grid[0].indices) {
                val pos = Position(col, row)
                if (grid[row][col] == Tile.Floor && pos !in excluded) {
                    candidates.add(pos)
                }
            }
        }

        candidates.shuffle(random)

        var placed = 0
        for (pos in candidates) {
            if (placed >= hazardCount) break

            val hazardTile = when (theme) {
                FloorTheme.Crypt -> Tile.Trap
                FloorTheme.Sewers -> Tile.Water
                FloorTheme.Caverns -> if (random.nextBoolean()) Tile.Trap else Tile.Water
                FloorTheme.Inferno -> if (random.nextInt(3) == 0) Tile.Lava else Tile.Trap
                FloorTheme.Void -> if (random.nextInt(4) == 0) Tile.Lava else Tile.Trap
            }

            grid[pos.y][pos.x] = hazardTile
            placed++
        }
    }

    // ===== Connectivity Validation =====

    private fun isReachable(dungeon: DungeonFloor, from: Position, to: Position): Boolean {
        val visited = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.add(from)
        visited.add(from)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == to) return true

            for (neighbor in current.cardinalNeighbors()) {
                if (neighbor !in visited && dungeon.isWalkable(neighbor)) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        return false
    }
}
