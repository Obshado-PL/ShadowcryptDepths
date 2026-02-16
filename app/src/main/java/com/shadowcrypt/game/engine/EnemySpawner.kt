package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.EnemyType
import com.shadowcrypt.game.model.EnemyTypes
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Room
import kotlin.random.Random

object EnemySpawner {

    fun spawnForFloor(
        floorNumber: Int,
        rooms: List<Room>,
        playerStart: Position,
        random: Random,
        difficulty: Difficulty = Difficulty.Normal
    ): List<Enemy> {
        val enemies = mutableListOf<Enemy>()
        var nextId = 0

        val playerRoom = rooms.indexOfFirst { it.contains(playerStart) }
        val spawnableRooms = rooms.filterIndexed { index, _ -> index != playerRoom }

        val bossType = EnemyTypes.bossForFloor(floorNumber)
        val bossRoomIndex = if (bossType != null) spawnableRooms.lastIndex else -1

        val availableTypes = EnemyTypes.forFloor(floorNumber)
        if (availableTypes.isEmpty()) return enemies

        for ((index, room) in spawnableRooms.withIndex()) {
            val isBossRoom = index == bossRoomIndex && bossType != null

            if (isBossRoom) {
                enemies.add(
                    createEnemy(nextId++, bossType!!, room.center, floorNumber, random, difficulty)
                )
                val minionCount = random.nextInt(1, 3)
                val minionPositions = getSpawnPositions(
                    room, minionCount, enemies.map { it.position }.toSet(), random
                )
                for (pos in minionPositions) {
                    val type = weightedSelect(availableTypes, random)
                    enemies.add(createEnemy(nextId++, type, pos, floorNumber, random, difficulty))
                }
            } else {
                val (minEnemies, maxEnemies) = when {
                    floorNumber <= 4 -> 2 to 4
                    floorNumber <= 8 -> 3 to 5
                    else -> 4 to 6
                }
                val adjustedMin = (minEnemies + difficulty.enemyCountBonus).coerceAtLeast(1)
                val adjustedMax = (maxEnemies + difficulty.enemyCountBonus).coerceAtLeast(adjustedMin)
                val count = random.nextInt(adjustedMin, adjustedMax + 1)
                val positions = getSpawnPositions(
                    room, count, enemies.map { it.position }.toSet(), random
                )
                for (pos in positions) {
                    val type = weightedSelect(availableTypes, random)
                    enemies.add(createEnemy(nextId++, type, pos, floorNumber, random, difficulty))
                }
            }
        }

        return enemies
    }

    private fun createEnemy(
        id: Int,
        type: EnemyType,
        position: Position,
        floorNumber: Int,
        random: Random,
        difficulty: Difficulty = Difficulty.Normal
    ): Enemy {
        val floorsAboveMin = (floorNumber - type.minFloor).coerceAtLeast(0)
        val scale = (1.0f + floorsAboveMin * 0.10f) * difficulty.enemyStatScale

        val scaledHp = (type.baseHp * scale).toInt()
        return Enemy(
            id = id,
            typeId = type.id,
            position = position,
            hp = scaledHp,
            maxHp = scaledHp,
            atk = (type.baseAtk * scale).toInt(),
            def = (type.baseDef * scale).toInt(),
            mag = (type.baseMag * scale).toInt(),
            spd = type.baseSpd,
            xpReward = (type.baseXpReward * scale).toInt(),
            behavior = type.behavior,
            displayName = type.displayName,
            isBoss = type.isBoss
        )
    }

    private fun weightedSelect(types: List<EnemyType>, random: Random): EnemyType {
        val totalWeight = types.sumOf { it.spawnWeight }
        if (totalWeight <= 0) return types.first()
        var roll = random.nextInt(totalWeight)
        for (type in types) {
            roll -= type.spawnWeight
            if (roll < 0) return type
        }
        return types.last()
    }

    private fun getSpawnPositions(
        room: Room,
        count: Int,
        occupied: Set<Position>,
        random: Random
    ): List<Position> {
        val available = room.positions()
            .filter { it !in occupied && it != room.center }
            .toMutableList()
        available.shuffle(random)
        return available.take(count)
    }
}
