package com.shadowcrypt.game.engine.item

import com.shadowcrypt.game.engine.dungeon.Room
import com.shadowcrypt.game.engine.model.ItemData
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.engine.model.Tile
import kotlin.random.Random

object ItemSpawner {

    fun spawnGroundItems(
        rooms: List<Room>,
        startRoomIndex: Int,
        floorNumber: Int,
        tiles: List<List<Tile>>,
        occupiedPositions: Set<Position>,
        startingItemId: Int,
        random: Random
    ): List<Pair<ItemData, Position>> {
        val items = mutableListOf<Pair<ItemData, Position>>()
        val occupied = occupiedPositions.toMutableSet()
        var nextId = startingItemId

        for ((index, room) in rooms.withIndex()) {
            if (index == startRoomIndex) continue

            if (random.nextDouble() > 0.40) continue

            val pos = findItemPosition(room, tiles, occupied, random) ?: continue
            val item = ItemGenerator.generateItem(floorNumber, nextId++, random)
            items.add(Pair(item, pos))
            occupied.add(pos)
        }

        return items
    }

    private fun findItemPosition(
        room: Room,
        tiles: List<List<Tile>>,
        occupied: Set<Position>,
        random: Random
    ): Position? {
        repeat(20) {
            val x = room.x + random.nextInt(room.width)
            val y = room.y + random.nextInt(room.height)
            val pos = Position(x, y)
            if (y in tiles.indices && x in tiles[0].indices &&
                tiles[y][x] == Tile.FLOOR &&
                pos !in occupied
            ) {
                return pos
            }
        }
        return null
    }
}
