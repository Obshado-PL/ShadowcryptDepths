package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.model.DungeonFloor
import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Room
import com.shadowcrypt.game.model.RoomShape
import com.shadowcrypt.game.model.RoomType
import com.shadowcrypt.game.model.Tile
import kotlin.random.Random

/**
 * Generates dungeon floors using Binary Space Partitioning.
 * BSP ensures rooms never overlap and corridors connect all rooms.
 * Supports varied room shapes, special room types, and structured hazard patterns.
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
            val fallbackRoom = Room(width / 4, height / 4, width / 2, height / 2)
            rooms.add(fallbackRoom)
            root.room = fallbackRoom
        }

        // Assign room types and shapes
        assignRoomTypes(rooms, floorNumber, random)
        assignRoomShapes(rooms, theme, random)

        connectRooms(root, grid, random)
        rooms.forEach { carveRoom(grid, it) }
        placeDoors(grid, rooms)

        // Ensure every room center is a floor tile (required for corridor connections)
        for (room in rooms) {
            val c = room.center
            if (c.y in grid.indices && c.x in grid[0].indices && grid[c.y][c.x] != Tile.Floor) {
                grid[c.y][c.x] = Tile.Floor
            }
        }

        // Place stairs
        val firstRoom = rooms.first()
        val lastRoom = rooms.last()
        val playerStart = firstRoom.center
        val stairsDownPos = lastRoom.center
        grid[stairsDownPos.y][stairsDownPos.x] = Tile.StairsDown

        val stairsUpPos: Position? = if (floorNumber > 1) {
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

        placeHazards(grid, floorNumber, theme, rooms, playerStart, stairsDownPos, stairsUpPos, random)

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

    // ===== Room Type & Shape Assignment =====

    private fun assignRoomTypes(rooms: MutableList<Room>, floorNumber: Int, random: Random) {
        if (rooms.size <= 2) return // only first & last room, keep Normal

        val specialWeights = listOf(
            RoomType.Normal to 40,
            RoomType.TreasureVault to 10,
            RoomType.Arena to 15,
            RoomType.TrapGauntlet to (if (floorNumber >= 3) 10 else 5),
            RoomType.ShrineRoom to 12,
            RoomType.Library to (if (floorNumber >= 5) 10 else 6),
            RoomType.Armory to (if (floorNumber >= 4) 10 else 5)
        )

        for (i in 1 until rooms.lastIndex) {
            val type = weightedSelect(specialWeights, random)
            rooms[i] = rooms[i].copy(type = type)
        }
    }

    private fun assignRoomShapes(rooms: MutableList<Room>, theme: FloorTheme, random: Random) {
        val shapeWeights = when (theme) {
            FloorTheme.Crypt -> listOf(
                RoomShape.Rectangular to 35,
                RoomShape.Pillared to 30,
                RoomShape.Cross to 15,
                RoomShape.LShaped to 20
            )
            FloorTheme.Sewers -> listOf(
                RoomShape.Rectangular to 40,
                RoomShape.LShaped to 25,
                RoomShape.Pillared to 15,
                RoomShape.Cross to 10,
                RoomShape.Circular to 10
            )
            FloorTheme.Caverns -> listOf(
                RoomShape.Circular to 35,
                RoomShape.Rectangular to 25,
                RoomShape.Pillared to 15,
                RoomShape.Cross to 15,
                RoomShape.LShaped to 10
            )
            FloorTheme.Inferno -> listOf(
                RoomShape.Rectangular to 30,
                RoomShape.Cross to 25,
                RoomShape.Pillared to 20,
                RoomShape.Circular to 15,
                RoomShape.LShaped to 10
            )
            FloorTheme.Void -> listOf(
                RoomShape.Cross to 30,
                RoomShape.Circular to 25,
                RoomShape.Rectangular to 20,
                RoomShape.LShaped to 15,
                RoomShape.Pillared to 10
            )
        }

        for (i in rooms.indices) {
            if (rooms[i].width >= 5 && rooms[i].height >= 5) {
                val shape = weightedSelect(shapeWeights, random)
                rooms[i] = rooms[i].copy(shape = shape)
            }
        }
    }

    private fun <T> weightedSelect(weights: List<Pair<T, Int>>, random: Random): T {
        val totalWeight = weights.sumOf { it.second }
        var roll = random.nextInt(totalWeight)
        for ((item, weight) in weights) {
            roll -= weight
            if (roll < 0) return item
        }
        return weights.last().first
    }

    // ===== Room Connection =====

    private fun connectRooms(node: BspNode, grid: Array<Array<Tile>>, random: Random): Room? {
        if (node.room != null) return node.room

        val leftRoom = node.left?.let { connectRooms(it, grid, random) }
        val rightRoom = node.right?.let { connectRooms(it, grid, random) }

        if (leftRoom != null && rightRoom != null) {
            carveCorridor(grid, leftRoom.center, rightRoom.center, random)
        }

        return leftRoom ?: rightRoom
    }

    // ===== Shape-Aware Carving =====

    private fun carveRoom(grid: Array<Array<Tile>>, room: Room) {
        when (room.shape) {
            RoomShape.Rectangular -> carveRectangular(grid, room)
            RoomShape.Circular -> carveCircular(grid, room)
            RoomShape.LShaped -> carveLShaped(grid, room)
            RoomShape.Cross -> carveCross(grid, room)
            RoomShape.Pillared -> carvePillared(grid, room)
        }
    }

    private fun carveRectangular(grid: Array<Array<Tile>>, room: Room) {
        for (row in room.y..room.bottom) {
            for (col in room.x..room.right) {
                if (row in grid.indices && col in grid[0].indices) {
                    grid[row][col] = Tile.Floor
                }
            }
        }
    }

    private fun carveCircular(grid: Array<Array<Tile>>, room: Room) {
        val cx = room.x + room.width / 2.0
        val cy = room.y + room.height / 2.0
        val rx = (room.width - 1) / 2.0
        val ry = (room.height - 1) / 2.0
        for (row in room.y..room.bottom) {
            for (col in room.x..room.right) {
                if (row in grid.indices && col in grid[0].indices) {
                    val dx = (col - cx) / rx
                    val dy = (row - cy) / ry
                    if (dx * dx + dy * dy <= 1.0) {
                        grid[row][col] = Tile.Floor
                    }
                }
            }
        }
    }

    private fun carveLShaped(grid: Array<Array<Tile>>, room: Room) {
        val splitX = room.x + room.width / 2
        val splitY = room.y + room.height / 2
        for (row in room.y..room.bottom) {
            for (col in room.x..room.right) {
                if (row in grid.indices && col in grid[0].indices) {
                    // Horizontal arm: full width, top half
                    // Vertical arm: left half, full height
                    if (row <= splitY || col <= splitX) {
                        grid[row][col] = Tile.Floor
                    }
                }
            }
        }
    }

    private fun carveCross(grid: Array<Array<Tile>>, room: Room) {
        val armWidth = (room.width / 3).coerceAtLeast(2)
        val armHeight = (room.height / 3).coerceAtLeast(2)
        val hStart = room.x + (room.width - armWidth) / 2
        val hEnd = hStart + armWidth - 1
        val vStart = room.y + (room.height - armHeight) / 2
        val vEnd = vStart + armHeight - 1
        for (row in room.y..room.bottom) {
            for (col in room.x..room.right) {
                if (row in grid.indices && col in grid[0].indices) {
                    // Horizontal band (center rows, full width) OR vertical band (center cols, full height)
                    if (row in vStart..vEnd || col in hStart..hEnd) {
                        grid[row][col] = Tile.Floor
                    }
                }
            }
        }
    }

    private fun carvePillared(grid: Array<Array<Tile>>, room: Room) {
        // First carve full rectangle
        carveRectangular(grid, room)
        // Then place pillars on a grid pattern (every 3 tiles, offset 2 from edges)
        for (row in (room.y + 2)..room.bottom step 3) {
            for (col in (room.x + 2)..room.right step 3) {
                if (row in grid.indices && col in grid[0].indices
                    && row < room.bottom && col < room.right
                ) {
                    grid[row][col] = Tile.Pillar
                }
            }
        }
    }

    // ===== Corridors =====

    private fun carveCorridor(
        grid: Array<Array<Tile>>,
        from: Position,
        to: Position,
        random: Random
    ) {
        val wide = random.nextInt(100) < 25 // 25% chance of wide corridor
        val corridorWidth = if (wide) 2 else 1

        if (random.nextBoolean()) {
            carveHorizontalTunnel(grid, from.x, to.x, from.y, corridorWidth)
            carveVerticalTunnel(grid, from.y, to.y, to.x, corridorWidth)
        } else {
            carveVerticalTunnel(grid, from.y, to.y, from.x, corridorWidth)
            carveHorizontalTunnel(grid, from.x, to.x, to.y, corridorWidth)
        }

        // 15% chance of a dead-end alcove at the corridor bend
        if (random.nextInt(100) < 15) {
            val bendPos = if (random.nextBoolean()) Position(to.x, from.y) else Position(from.x, to.y)
            carveAlcove(grid, bendPos, random)
        }
    }

    private fun carveHorizontalTunnel(
        grid: Array<Array<Tile>>, x1: Int, x2: Int, y: Int, width: Int = 1
    ) {
        val start = minOf(x1, x2)
        val end = maxOf(x1, x2)
        for (x in start..end) {
            for (dy in 0 until width) {
                val row = y + dy
                if (row in grid.indices && x in grid[0].indices) {
                    if (grid[row][x] == Tile.Wall) {
                        grid[row][x] = Tile.Floor
                    }
                }
            }
        }
    }

    private fun carveVerticalTunnel(
        grid: Array<Array<Tile>>, y1: Int, y2: Int, x: Int, width: Int = 1
    ) {
        val start = minOf(y1, y2)
        val end = maxOf(y1, y2)
        for (y in start..end) {
            for (dx in 0 until width) {
                val col = x + dx
                if (y in grid.indices && col in grid[0].indices) {
                    if (grid[y][col] == Tile.Wall) {
                        grid[y][col] = Tile.Floor
                    }
                }
            }
        }
    }

    private fun carveAlcove(grid: Array<Array<Tile>>, origin: Position, random: Random) {
        val dirs = Direction.cardinal()
        val dir = dirs[random.nextInt(dirs.size)]
        val length = random.nextInt(2, 4)
        for (i in 1..length) {
            val nx = origin.x + dir.dx * i
            val ny = origin.y + dir.dy * i
            if (ny in grid.indices && nx in grid[0].indices && grid[ny][nx] == Tile.Wall) {
                grid[ny][nx] = Tile.Floor
            }
        }
    }

    // ===== Door Placement =====

    private fun placeDoors(grid: Array<Array<Tile>>, rooms: List<Room>) {
        for (room in rooms) {
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

        val wallAboveBelow = (y - 1 >= 0 && grid[y - 1][x] == Tile.Wall) &&
                (y + 1 < grid.size && grid[y + 1][x] == Tile.Wall)
        val wallLeftRight = (x - 1 >= 0 && grid[y][x - 1] == Tile.Wall) &&
                (x + 1 < grid[0].size && grid[y][x + 1] == Tile.Wall)

        if (wallAboveBelow || wallLeftRight) {
            grid[y][x] = Tile.Door
        }
    }

    // ===== Hazard Patterns =====

    private fun placeHazards(
        grid: Array<Array<Tile>>,
        floorNumber: Int,
        theme: FloorTheme,
        rooms: List<Room>,
        playerStart: Position,
        stairsDown: Position,
        stairsUp: Position?,
        random: Random
    ) {
        val excluded = mutableSetOf(playerStart, stairsDown)
        stairsUp?.let { excluded.add(it) }

        // Per-room hazard patterns
        for (room in rooms) {
            if (room.center == playerStart) continue

            if (room.type == RoomType.TrapGauntlet) {
                placeTrapRows(grid, room, excluded, random)
                continue
            }

            val density = room.type.trapDensity
            if (density <= 0f) continue

            placeRoomHazardPattern(grid, room, theme, density, excluded, random)
        }

        // Corridor-level hazards (theme flavor)
        placeCorridorHazards(grid, theme, rooms, floorNumber, excluded, random)
    }

    private fun placeRoomHazardPattern(
        grid: Array<Array<Tile>>,
        room: Room,
        theme: FloorTheme,
        density: Float,
        excluded: Set<Position>,
        random: Random
    ) {
        when (theme) {
            FloorTheme.Crypt -> placeCryptTraps(grid, room, density, excluded, random)
            FloorTheme.Sewers -> placeWaterChannels(grid, room, excluded)
            FloorTheme.Caverns -> placeWaterPool(grid, room, excluded, random)
            FloorTheme.Inferno -> placeLavaPools(grid, room, excluded, random)
            FloorTheme.Void -> placeVoidTraps(grid, room, density, excluded, random)
        }
    }

    /** Crypt: trap clusters near room edges */
    private fun placeCryptTraps(
        grid: Array<Array<Tile>>,
        room: Room,
        density: Float,
        excluded: Set<Position>,
        random: Random
    ) {
        val count = (2 * density).toInt().coerceAtLeast(1)
        val edgePositions = room.floorPositions().filter { pos ->
            pos !in excluded &&
                    grid[pos.y][pos.x] == Tile.Floor &&
                    (pos.x == room.x || pos.x == room.right || pos.y == room.y || pos.y == room.bottom)
        }.shuffled(random)

        for (pos in edgePositions.take(count)) {
            grid[pos.y][pos.x] = Tile.Trap
        }
    }

    /** Sewers: water channel along one wall */
    private fun placeWaterChannels(
        grid: Array<Array<Tile>>,
        room: Room,
        excluded: Set<Position>
    ) {
        // Water strip along the bottom row of the room
        for (col in room.x..room.right) {
            val pos = Position(col, room.bottom)
            if (pos !in excluded && grid[pos.y][pos.x] == Tile.Floor) {
                grid[pos.y][pos.x] = Tile.Water
            }
        }
    }

    /** Caverns: 2x2 or 3x3 water pool in a corner */
    private fun placeWaterPool(
        grid: Array<Array<Tile>>,
        room: Room,
        excluded: Set<Position>,
        random: Random
    ) {
        if (room.width < 4 || room.height < 4) return
        val poolSize = if (room.width >= 6 && room.height >= 6) 3 else 2

        // Pick a random corner
        val cornerX = if (random.nextBoolean()) room.x else room.right - poolSize + 1
        val cornerY = if (random.nextBoolean()) room.y else room.bottom - poolSize + 1

        for (dy in 0 until poolSize) {
            for (dx in 0 until poolSize) {
                val pos = Position(cornerX + dx, cornerY + dy)
                if (pos !in excluded && pos.y in grid.indices && pos.x in grid[0].indices
                    && grid[pos.y][pos.x] == Tile.Floor
                ) {
                    grid[pos.y][pos.x] = Tile.Water
                }
            }
        }
    }

    /** Inferno: small lava pools near room edges */
    private fun placeLavaPools(
        grid: Array<Array<Tile>>,
        room: Room,
        excluded: Set<Position>,
        random: Random
    ) {
        if (room.width < 5 || room.height < 5) return

        // Place 1-2 small lava pools (2x1 or 1x2) near edges
        val poolCount = random.nextInt(1, 3)
        val edgeCandidates = room.floorPositions().filter { pos ->
            pos !in excluded &&
                    grid[pos.y][pos.x] == Tile.Floor &&
                    (pos.x == room.x + 1 || pos.x == room.right - 1 ||
                            pos.y == room.y + 1 || pos.y == room.bottom - 1)
        }.shuffled(random)

        for (pos in edgeCandidates.take(poolCount * 2)) {
            grid[pos.y][pos.x] = Tile.Lava
        }
    }

    /** Void: geometric trap patterns */
    private fun placeVoidTraps(
        grid: Array<Array<Tile>>,
        room: Room,
        density: Float,
        excluded: Set<Position>,
        random: Random
    ) {
        // Diagonal line of traps through the room
        val count = (3 * density).toInt().coerceAtLeast(1)
        val step = if (random.nextBoolean()) 1 else -1
        var cx = if (step > 0) room.x + 1 else room.right - 1
        var cy = room.y + 1
        var placed = 0

        while (placed < count && cy < room.bottom && cx in room.x..room.right) {
            val pos = Position(cx, cy)
            if (pos !in excluded && grid[pos.y][pos.x] == Tile.Floor) {
                grid[pos.y][pos.x] = Tile.Trap
                placed++
            }
            cx += step
            cy++
        }
    }

    /** Trap gauntlet: parallel rows of traps across the room */
    private fun placeTrapRows(
        grid: Array<Array<Tile>>,
        room: Room,
        excluded: Set<Position>,
        random: Random
    ) {
        val rowCount = (room.height / 3).coerceIn(2, 4)
        val spacing = room.height / (rowCount + 1)

        for (i in 1..rowCount) {
            val trapRow = room.y + spacing * i
            if (trapRow > room.bottom) continue

            for (col in room.x..room.right) {
                // Leave a gap for the player to weave through
                if (random.nextInt(100) < 30) continue
                val pos = Position(col, trapRow)
                if (pos !in excluded && pos.y in grid.indices && pos.x in grid[0].indices
                    && grid[pos.y][pos.x] == Tile.Floor
                ) {
                    grid[pos.y][pos.x] = Tile.Trap
                }
            }
        }
    }

    /** Corridor-level hazards: theme-specific flavor tiles in corridors */
    private fun placeCorridorHazards(
        grid: Array<Array<Tile>>,
        theme: FloorTheme,
        rooms: List<Room>,
        floorNumber: Int,
        excluded: Set<Position>,
        random: Random
    ) {
        // Find corridor tiles (floor tiles not inside any room)
        val roomPositions = rooms.flatMap { it.floorPositions() }.toHashSet()
        val corridorTiles = mutableListOf<Position>()
        for (row in grid.indices) {
            for (col in grid[0].indices) {
                val pos = Position(col, row)
                if (grid[row][col] == Tile.Floor && pos !in roomPositions && pos !in excluded) {
                    corridorTiles.add(pos)
                }
            }
        }

        val hazardChance = (5 + floorNumber * 2).coerceAtMost(20)
        for (pos in corridorTiles) {
            if (random.nextInt(100) >= hazardChance) continue

            val tile = when (theme) {
                FloorTheme.Crypt -> Tile.Trap
                FloorTheme.Sewers -> Tile.Water
                FloorTheme.Caverns -> if (random.nextBoolean()) Tile.Water else Tile.Trap
                FloorTheme.Inferno -> Tile.Trap
                FloorTheme.Void -> Tile.Trap
            }
            grid[pos.y][pos.x] = tile
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
