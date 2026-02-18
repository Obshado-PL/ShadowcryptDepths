package com.shadowcrypt.game.engine.dungeon

import com.shadowcrypt.game.engine.model.EnemyData
import com.shadowcrypt.game.engine.model.EnemyType
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.engine.model.Tile
import kotlin.random.Random

object EnemySpawner {

    fun spawnEnemies(
        rooms: List<Room>,
        startRoomIndex: Int,
        floorNumber: Int,
        tiles: List<List<Tile>>,
        playerStart: Position,
        random: Random
    ): List<EnemyData> {
        val eligibleTypes = EnemyType.entries.filter {
            floorNumber in it.minFloor..it.maxFloor
        }
        if (eligibleTypes.isEmpty()) return emptyList()

        val enemies = mutableListOf<EnemyData>()
        val occupiedPositions = mutableSetOf(playerStart)
        var nextId = 0

        for ((index, room) in rooms.withIndex()) {
            if (index == startRoomIndex) continue

            val roomArea = room.width * room.height
            val baseCount = when {
                roomArea < 20 -> 1
                roomArea < 35 -> 2
                roomArea < 50 -> 3
                else -> 4
            }
            val enemyCount = (baseCount + floorNumber / 4).coerceAtMost(5)

            for (i in 0 until enemyCount) {
                val pos = findSpawnPosition(room, tiles, occupiedPositions, random) ?: continue
                val type = eligibleTypes[random.nextInt(eligibleTypes.size)]

                val scaledHp = (type.baseHp * (1.0 + 0.1 * (floorNumber - 1))).toInt()
                val scaledAttack = type.baseAttack + (floorNumber - 1) / 2
                val scaledDefense = type.baseDefense + (floorNumber - type.minFloor) / 3
                val scaledXp = type.baseXpReward + (floorNumber - 1) * 2

                enemies.add(
                    EnemyData(
                        id = nextId++,
                        type = type,
                        position = pos,
                        hp = scaledHp,
                        maxHp = scaledHp,
                        attack = scaledAttack,
                        defense = scaledDefense,
                        xpReward = scaledXp
                    )
                )
                occupiedPositions.add(pos)
            }
        }

        return enemies
    }

    private fun findSpawnPosition(
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
                tiles[y][x].walkable &&
                tiles[y][x] != Tile.STAIRS_DOWN &&
                tiles[y][x] != Tile.STAIRS_UP &&
                pos !in occupied
            ) {
                return pos
            }
        }
        return null
    }
}
