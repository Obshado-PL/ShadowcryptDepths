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
                val roomMult = room.type.enemyMultiplier
                val adjustedMin = ((minEnemies * roomMult).toInt() + difficulty.enemyCountBonus).coerceAtLeast(0)
                val adjustedMax = ((maxEnemies * roomMult).toInt() + difficulty.enemyCountBonus).coerceAtLeast(adjustedMin)
                if (adjustedMax == 0) continue // Skip rooms with no enemies (e.g. ShrineRoom, TrapGauntlet)
                val count = random.nextInt(adjustedMin.coerceAtLeast(1), adjustedMax + 1)
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

    private val elitePrefixes = listOf("Enraged", "Armored", "Venomous", "Swift", "Cursed")

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

        // Elite chance: 10% on floors 3+, scaling with difficulty
        val eliteChance = if (floorNumber >= 3 && !type.isBoss) {
            (10 * difficulty.enemyStatScale).toInt()
        } else 0
        val isElite = random.nextInt(100) < eliteChance

        val eliteScale = if (isElite) 1.4f else 1.0f
        val prefix = if (isElite) elitePrefixes[random.nextInt(elitePrefixes.size)] else null

        val scaledHp = (type.baseHp * scale * eliteScale).toInt()
        return Enemy(
            id = id,
            typeId = type.id,
            position = position,
            hp = scaledHp,
            maxHp = scaledHp,
            atk = (type.baseAtk * scale * eliteScale).toInt(),
            def = (type.baseDef * scale * eliteScale).toInt(),
            mag = (type.baseMag * scale * eliteScale).toInt(),
            spd = type.baseSpd + if (isElite && prefix == "Swift") 3 else 0,
            xpReward = (type.baseXpReward * scale * eliteScale * 1.5f).toInt(),
            behavior = type.behavior,
            displayName = if (prefix != null) "$prefix ${type.displayName}" else type.displayName,
            isBoss = type.isBoss,
            isElite = isElite
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
        val available = room.floorPositions()
            .filter { it !in occupied && it != room.center }
            .toMutableList()
        available.shuffle(random)
        return available.take(count)
    }
}
