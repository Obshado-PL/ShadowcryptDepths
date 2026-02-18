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
        endRoomIndex: Int,
        floorNumber: Int,
        tiles: List<List<Tile>>,
        playerStart: Position,
        random: Random
    ): List<EnemyData> {
        val eligibleTypes = EnemyType.entries.filter {
            floorNumber in it.minFloor..it.maxFloor && !it.isBoss
        }

        val enemies = mutableListOf<EnemyData>()
        val occupiedPositions = mutableSetOf(playerStart)
        var nextId = 0

        val isBossFloor = floorNumber == 5 || floorNumber == 10

        // Spawn boss in end room on boss floors
        if (isBossFloor) {
            val bossType = if (floorNumber == 5) EnemyType.BONE_WARDEN else EnemyType.SHADOWCRYPT_LORD
            val endRoom = rooms.getOrNull(endRoomIndex)
            if (endRoom != null) {
                val bossPos = findSpawnPosition(endRoom, tiles, occupiedPositions, random)
                    ?: Position(endRoom.centerX, endRoom.centerY)

                val scaledHp = (bossType.baseHp * (1.0 + 0.1 * (floorNumber - 1))).toInt()
                val scaledAttack = bossType.baseAttack + (floorNumber - 1) / 2
                val scaledDefense = bossType.baseDefense
                val scaledXp = bossType.baseXpReward + (floorNumber - 1) * 2

                enemies.add(
                    EnemyData(
                        id = nextId++,
                        type = bossType,
                        position = bossPos,
                        hp = scaledHp,
                        maxHp = scaledHp,
                        attack = scaledAttack,
                        defense = scaledDefense,
                        xpReward = scaledXp
                    )
                )
                occupiedPositions.add(bossPos)
            }
        }

        if (eligibleTypes.isEmpty()) return enemies

        // Spawn regular enemies (halved on boss floors, skip end room on boss floors)
        for ((index, room) in rooms.withIndex()) {
            if (index == startRoomIndex) continue
            if (isBossFloor && index == endRoomIndex) continue

            val roomArea = room.width * room.height
            val baseCount = when {
                roomArea < 20 -> 1
                roomArea < 35 -> 2
                roomArea < 50 -> 3
                else -> 4
            }
            var enemyCount = (baseCount + floorNumber / 4).coerceAtMost(5)
            if (isBossFloor) {
                enemyCount = (enemyCount / 2).coerceAtLeast(1)
            }

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
