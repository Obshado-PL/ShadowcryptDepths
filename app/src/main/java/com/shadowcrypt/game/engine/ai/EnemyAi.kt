package com.shadowcrypt.game.engine.ai

import com.shadowcrypt.game.engine.combat.CombatEngine
import com.shadowcrypt.game.engine.model.Direction
import com.shadowcrypt.game.engine.model.EnemyBehavior
import com.shadowcrypt.game.engine.model.EnemyData
import com.shadowcrypt.game.engine.model.GameState
import com.shadowcrypt.game.engine.model.PlayerData
import com.shadowcrypt.game.engine.model.Position
import com.shadowcrypt.game.engine.model.Visibility
import kotlin.math.abs
import kotlin.random.Random

object EnemyAi {

    fun processEnemyTurns(state: GameState): GameState {
        val random = Random(state.seed + state.turnCount.toLong() * 31)
        var currentPlayer = state.player
        var currentEnemies = state.enemies
        val messages = mutableListOf<String>()
        var playerDead = false

        for (enemy in state.enemies) {
            if (playerDead) break
            val currentEnemy = currentEnemies.find { it.id == enemy.id } ?: continue
            if (currentEnemy.hp <= 0) continue

            val dist = manhattanDistance(currentEnemy.position, currentPlayer.position)
            val playerVisible = isEnemyAwareOfPlayer(currentEnemy, state)

            // Adjacent to player → attack
            if (dist == 1) {
                val (newPlayer, dmg, killed) = CombatEngine.enemyAttacksPlayer(
                    currentEnemy, currentPlayer
                )
                currentPlayer = newPlayer
                messages.add("${currentEnemy.type.displayName} hits you for $dmg!")
                if (killed) {
                    playerDead = true
                    messages.add("You have been slain!")
                }
                continue
            }

            // Determine movement
            val targetPos = when (currentEnemy.type.behavior) {
                EnemyBehavior.AGGRESSIVE -> {
                    if (playerVisible && dist <= currentEnemy.type.detectionRange) {
                        moveToward(currentEnemy.position, currentPlayer.position, state, currentEnemies)
                    } else {
                        randomWalk(currentEnemy.position, state, currentEnemies, random)
                    }
                }
                EnemyBehavior.PATROL -> {
                    randomWalk(currentEnemy.position, state, currentEnemies, random)
                }
            }

            if (targetPos != null && targetPos != currentEnemy.position) {
                currentEnemies = currentEnemies.map {
                    if (it.id == currentEnemy.id) it.copy(position = targetPos) else it
                }
            }
        }

        val combinedMessage = buildList {
            state.message?.let { add(it) }
            addAll(messages)
        }.joinToString(" ").ifEmpty { null }

        return state.copy(
            player = currentPlayer,
            enemies = currentEnemies,
            message = combinedMessage,
            isPlayerDead = playerDead
        )
    }

    private fun isEnemyAwareOfPlayer(enemy: EnemyData, state: GameState): Boolean {
        val pos = enemy.position
        return pos.y in state.visibility.indices &&
            pos.x in state.visibility[0].indices &&
            state.visibility[pos.y][pos.x] == Visibility.VISIBLE
    }

    private fun moveToward(
        from: Position,
        target: Position,
        state: GameState,
        enemies: List<EnemyData>
    ): Position? {
        val occupied = enemies.map { it.position }.toSet()
        return Direction.entries
            .map { from + it.toPosition() }
            .filter { pos ->
                pos.x in 0 until state.dungeon.width &&
                    pos.y in 0 until state.dungeon.height &&
                    state.dungeon.tiles[pos.y][pos.x].walkable &&
                    pos != state.player.position &&
                    pos !in occupied
            }
            .minByOrNull { manhattanDistance(it, target) }
    }

    private fun randomWalk(
        from: Position,
        state: GameState,
        enemies: List<EnemyData>,
        random: Random
    ): Position? {
        val occupied = enemies.map { it.position }.toSet()
        val candidates = Direction.entries
            .map { from + it.toPosition() }
            .filter { pos ->
                pos.x in 0 until state.dungeon.width &&
                    pos.y in 0 until state.dungeon.height &&
                    state.dungeon.tiles[pos.y][pos.x].walkable &&
                    pos != state.player.position &&
                    pos !in occupied
            }
        return if (candidates.isNotEmpty()) candidates[random.nextInt(candidates.size)] else null
    }

    private fun manhattanDistance(a: Position, b: Position): Int {
        return abs(a.x - b.x) + abs(a.y - b.y)
    }
}
