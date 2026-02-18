package com.shadowcrypt.game.engine.dungeon

import com.shadowcrypt.game.engine.model.DungeonLevel
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.engine.model.Tile
import kotlin.random.Random

object DungeonGenerator {

    private const val MIN_LEAF_SIZE = 8
    private const val MIN_ROOM_SIZE = 4
    private const val ROOM_PADDING = 2

    fun generate(
        width: Int = 50,
        height: Int = 40,
        floorNumber: Int,
        random: Random
    ): DungeonLevel {
        val grid = Array(height) { Array(width) { Tile.WALL } }

        // Build BSP tree
        val root = BspNode(1, 1, width - 2, height - 2)
        splitNode(root, random)

        // Place rooms in leaf nodes
        placeRooms(root, random)

        // Carve rooms into grid
        val rooms = root.allRooms()
        for (room in rooms) {
            carveRoom(grid, room)
        }

        // Connect rooms via corridors
        connectRooms(root, grid, random)

        // Place doors at corridor-room junctions
        placeDoors(grid, rooms)

        // Place stairs: up in first room, down in farthest room
        val startRoom = rooms.first()
        val endRoom = rooms.maxBy { room ->
            val dx = room.centerX - startRoom.centerX
            val dy = room.centerY - startRoom.centerY
            dx * dx + dy * dy
        }
        val endRoomIndex = rooms.indexOf(endRoom).coerceAtLeast(0)

        val stairsUpPos = Position(startRoom.centerX, startRoom.centerY)
        val stairsDownPos = Position(endRoom.centerX, endRoom.centerY)

        // Floor 10 is the final floor — no stairs down
        if (floorNumber < 10) {
            grid[stairsDownPos.y][stairsDownPos.x] = Tile.STAIRS_DOWN
        }
        if (floorNumber > 1) {
            grid[stairsUpPos.y][stairsUpPos.x] = Tile.STAIRS_UP
        }

        val playerStart = if (floorNumber == 1) {
            Position(startRoom.centerX, startRoom.centerY)
        } else {
            // Start near stairs up
            Position(stairsUpPos.x, stairsUpPos.y)
        }

        val tiles = grid.map { row -> row.toList() }

        val startRoomIndex = rooms.indexOfFirst { room ->
            playerStart.x in room.x..room.right && playerStart.y in room.y..room.bottom
        }.coerceAtLeast(0)

        return DungeonLevel(
            width = width,
            height = height,
            tiles = tiles,
            playerStart = playerStart,
            stairsDownPos = stairsDownPos,
            stairsUpPos = if (floorNumber > 1) stairsUpPos else null,
            floorNumber = floorNumber,
            rooms = rooms,
            startRoomIndex = startRoomIndex,
            endRoomIndex = endRoomIndex
        )
    }

    private fun splitNode(node: BspNode, random: Random) {
        if (node.width < MIN_LEAF_SIZE * 2 && node.height < MIN_LEAF_SIZE * 2) return

        // Choose split direction based on shape
        val splitHorizontally = when {
            node.width > node.height * 1.25 -> false
            node.height > node.width * 1.25 -> true
            else -> random.nextBoolean()
        }

        if (splitHorizontally) {
            if (node.height < MIN_LEAF_SIZE * 2) return
            val splitAt = MIN_LEAF_SIZE + random.nextInt(node.height - MIN_LEAF_SIZE * 2 + 1)
            node.left = BspNode(node.x, node.y, node.width, splitAt)
            node.right = BspNode(node.x, node.y + splitAt, node.width, node.height - splitAt)
        } else {
            if (node.width < MIN_LEAF_SIZE * 2) return
            val splitAt = MIN_LEAF_SIZE + random.nextInt(node.width - MIN_LEAF_SIZE * 2 + 1)
            node.left = BspNode(node.x, node.y, splitAt, node.height)
            node.right = BspNode(node.x + splitAt, node.y, node.width - splitAt, node.height)
        }

        splitNode(node.left!!, random)
        splitNode(node.right!!, random)
    }

    private fun placeRooms(node: BspNode, random: Random) {
        if (node.isLeaf) {
            val maxW = (node.width - ROOM_PADDING).coerceAtMost(node.width - 2)
            val maxH = (node.height - ROOM_PADDING).coerceAtMost(node.height - 2)
            if (maxW < MIN_ROOM_SIZE || maxH < MIN_ROOM_SIZE) return

            val roomW = MIN_ROOM_SIZE + random.nextInt((maxW - MIN_ROOM_SIZE + 1).coerceAtLeast(1))
            val roomH = MIN_ROOM_SIZE + random.nextInt((maxH - MIN_ROOM_SIZE + 1).coerceAtLeast(1))
            val roomX = node.x + random.nextInt((node.width - roomW).coerceAtLeast(1))
            val roomY = node.y + random.nextInt((node.height - roomH).coerceAtLeast(1))

            node.room = Room(roomX, roomY, roomW, roomH)
            return
        }
        node.left?.let { placeRooms(it, random) }
        node.right?.let { placeRooms(it, random) }
    }

    private fun carveRoom(grid: Array<Array<Tile>>, room: Room) {
        for (y in room.y..room.bottom) {
            for (x in room.x..room.right) {
                if (y in grid.indices && x in grid[0].indices) {
                    grid[y][x] = Tile.FLOOR
                }
            }
        }
    }

    private fun connectRooms(node: BspNode, grid: Array<Array<Tile>>, random: Random) {
        if (node.isLeaf) return

        val leftNode = node.left ?: return
        val rightNode = node.right ?: return

        connectRooms(leftNode, grid, random)
        connectRooms(rightNode, grid, random)

        val leftRoom = leftNode.closestRoom() ?: return
        val rightRoom = rightNode.closestRoom() ?: return

        // Find closest points between the two rooms
        val (p1, p2) = closestRoomPoints(leftRoom, rightRoom)

        // Carve L-shaped corridor
        carveCorridor(grid, p1.x, p1.y, p2.x, p2.y, random)
    }

    private fun closestRoomPoints(a: Room, b: Room): Pair<Position, Position> {
        // Use room centers as connection points
        return Pair(
            Position(a.centerX, a.centerY),
            Position(b.centerX, b.centerY)
        )
    }

    private fun carveCorridor(
        grid: Array<Array<Tile>>,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        random: Random
    ) {
        var x = x1
        var y = y1

        // L-shaped: go horizontal first, then vertical (or vice versa)
        val horizontalFirst = random.nextBoolean()

        if (horizontalFirst) {
            // Horizontal segment
            while (x != x2) {
                if (y in grid.indices && x in grid[0].indices && grid[y][x] == Tile.WALL) {
                    grid[y][x] = Tile.FLOOR
                }
                x += if (x2 > x) 1 else -1
            }
            // Vertical segment
            while (y != y2) {
                if (y in grid.indices && x in grid[0].indices && grid[y][x] == Tile.WALL) {
                    grid[y][x] = Tile.FLOOR
                }
                y += if (y2 > y) 1 else -1
            }
        } else {
            // Vertical segment first
            while (y != y2) {
                if (y in grid.indices && x in grid[0].indices && grid[y][x] == Tile.WALL) {
                    grid[y][x] = Tile.FLOOR
                }
                y += if (y2 > y) 1 else -1
            }
            // Horizontal segment
            while (x != x2) {
                if (y in grid.indices && x in grid[0].indices && grid[y][x] == Tile.WALL) {
                    grid[y][x] = Tile.FLOOR
                }
                x += if (x2 > x) 1 else -1
            }
        }
        // Carve the final tile
        if (y in grid.indices && x in grid[0].indices && grid[y][x] == Tile.WALL) {
            grid[y][x] = Tile.FLOOR
        }
    }

    private fun placeDoors(grid: Array<Array<Tile>>, rooms: List<Room>) {
        for (room in rooms) {
            // Check tiles along room perimeter for corridor entrances
            val doorCandidates = mutableListOf<Position>()

            for (x in room.x..room.right) {
                checkDoorCandidate(grid, x, room.y - 1, doorCandidates)
                checkDoorCandidate(grid, x, room.bottom + 1, doorCandidates)
            }
            for (y in room.y..room.bottom) {
                checkDoorCandidate(grid, room.x - 1, y, doorCandidates)
                checkDoorCandidate(grid, room.right + 1, y, doorCandidates)
            }

            // Place doors at corridor entrances (limit to avoid cluttering)
            for (pos in doorCandidates) {
                if (isDoorPosition(grid, pos.x, pos.y)) {
                    grid[pos.y][pos.x] = Tile.DOOR
                }
            }
        }
    }

    private fun checkDoorCandidate(
        grid: Array<Array<Tile>>,
        x: Int, y: Int,
        candidates: MutableList<Position>
    ) {
        if (y !in grid.indices || x !in grid[0].indices) return
        if (grid[y][x] == Tile.FLOOR) {
            candidates.add(Position(x, y))
        }
    }

    private fun isDoorPosition(grid: Array<Array<Tile>>, x: Int, y: Int): Boolean {
        // A door should be in a 1-wide passage: walls on two opposite sides, floor on the other two
        val h = y in grid.indices && x - 1 in grid[0].indices && x + 1 in grid[0].indices
        val v = y - 1 in grid.indices && y + 1 in grid.indices && x in grid[0].indices

        if (!h || !v) return false

        val wallsLeftRight = !grid[y][x - 1].walkable && !grid[y][x + 1].walkable
        val wallsUpDown = !grid[y - 1][x].walkable && !grid[y + 1][x].walkable
        val floorLeftRight = grid[y][x - 1].walkable && grid[y][x + 1].walkable
        val floorUpDown = grid[y - 1][x].walkable && grid[y + 1][x].walkable

        return (wallsLeftRight && floorUpDown) || (wallsUpDown && floorLeftRight)
    }
}
