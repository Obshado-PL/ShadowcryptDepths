package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.AiBehavior
import com.shadowcrypt.game.model.CombatResult
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.random.Random

object EnemyAI {

    fun processAllEnemies(state: GameState, random: Random): GameState {
        var currentState = state

        for (enemy in state.enemies) {
            if (!enemy.isAlive) continue
            val updated = currentState.enemies.find { it.id == enemy.id && it.isAlive }
                ?: continue
            currentState = processEnemy(currentState, updated, random)
            if (!currentState.player.isAlive) break
        }

        return currentState
    }

    private fun processEnemy(state: GameState, enemy: Enemy, random: Random): GameState {
        // Stunned enemies skip their turn
        if (enemy.isStunned) {
            return updateEnemyInState(state, enemy)
                .withMessage("${enemy.displayName} is stunned!")
        }

        val canSeePlayer = canEnemySeePlayer(state, enemy)

        val alertedEnemy = if (canSeePlayer && !enemy.alertedByPlayer) {
            enemy.copy(alertedByPlayer = true)
        } else {
            enemy
        }

        return when (alertedEnemy.behavior) {
            AiBehavior.Aggressive -> processAggressive(state, alertedEnemy, random)
            AiBehavior.Patrol -> processPatrol(state, alertedEnemy, canSeePlayer, random)
            AiBehavior.Ranged -> processRanged(state, alertedEnemy, canSeePlayer, random)
            AiBehavior.Ambush -> processAmbush(state, alertedEnemy, canSeePlayer, random)
            AiBehavior.Support -> processSupport(state, alertedEnemy, canSeePlayer, random)
            AiBehavior.Boss -> processBoss(state, alertedEnemy, canSeePlayer, random)
        }
    }

    private fun processAggressive(state: GameState, enemy: Enemy, random: Random): GameState {
        val dist = enemy.position.distanceTo(state.player.position)
        if (dist == 1) return resolveEnemyAttack(state, enemy, random)
        return moveEnemyToward(state, enemy, state.player.position)
    }

    private fun processPatrol(
        state: GameState, enemy: Enemy, canSeePlayer: Boolean, random: Random
    ): GameState {
        if (canSeePlayer || enemy.alertedByPlayer) {
            return processAggressive(state, enemy.copy(alertedByPlayer = true), random)
        }
        return moveEnemyRandomly(state, enemy, random)
    }

    private fun processRanged(
        state: GameState, enemy: Enemy, canSeePlayer: Boolean, random: Random
    ): GameState {
        if (!canSeePlayer && !enemy.alertedByPlayer) {
            return moveEnemyRandomly(state, enemy, random)
        }

        val dist = enemy.position.distanceTo(state.player.position)

        if (canSeePlayer && dist in 2..3) {
            return resolveEnemyAttack(state, enemy, random)
        }
        if (dist <= 1) {
            return moveEnemyAwayFrom(state, enemy, state.player.position)
        }
        return moveEnemyToward(state, enemy, state.player.position)
    }

    private fun processAmbush(
        state: GameState, enemy: Enemy, canSeePlayer: Boolean, random: Random
    ): GameState {
        val dist = enemy.position.distanceTo(state.player.position)
        if (enemy.alertedByPlayer || (canSeePlayer && dist <= 3)) {
            return processAggressive(state, enemy.copy(alertedByPlayer = true), random)
        }
        return updateEnemyInState(state, enemy)
    }

    private fun processSupport(
        state: GameState, enemy: Enemy, canSeePlayer: Boolean, random: Random
    ): GameState {
        val woundedAlly = state.enemies
            .filter { it.id != enemy.id && it.isAlive && it.hp < it.maxHp }
            .minByOrNull { it.position.distanceTo(enemy.position) }

        if (woundedAlly != null && woundedAlly.position.distanceTo(enemy.position) <= 3) {
            val healAmount = CombatEngine.supportHeal(enemy, woundedAlly, random)
            if (healAmount > 0) {
                val healedAlly = woundedAlly.copy(
                    hp = (woundedAlly.hp + healAmount).coerceAtMost(woundedAlly.maxHp)
                )
                val newEnemies = state.enemies.map {
                    when (it.id) {
                        healedAlly.id -> healedAlly
                        enemy.id -> enemy
                        else -> it
                    }
                }
                return state.copy(enemies = newEnemies)
                    .withMessage("${enemy.displayName} heals ${woundedAlly.displayName} for $healAmount HP.")
            }
        }

        val dist = enemy.position.distanceTo(state.player.position)
        return if (canSeePlayer && dist <= 4) {
            moveEnemyAwayFrom(state, enemy, state.player.position)
        } else {
            moveEnemyRandomly(state, enemy, random)
        }
    }

    private fun processBoss(
        state: GameState, enemy: Enemy, canSeePlayer: Boolean, random: Random
    ): GameState {
        if (enemy.bossPhase == 1 && enemy.hpFraction <= 0.5f) {
            val enraged = enemy.copy(
                bossPhase = 2,
                atk = enemy.atk + 4,
                spd = enemy.spd + 2
            )
            val stateWithMsg = updateEnemyInState(state, enraged)
                .withMessage("${enemy.displayName} becomes enraged!")
            return processBoss(stateWithMsg, enraged, canSeePlayer, random)
        }

        val dist = enemy.position.distanceTo(state.player.position)
        if (dist == 1) return resolveEnemyAttack(state, enemy, random)
        return moveEnemyToward(state, enemy, state.player.position)
    }

    // ===== Helpers =====

    private fun canEnemySeePlayer(state: GameState, enemy: Enemy): Boolean {
        if (enemy.position.distanceTo(state.player.position) > 8) return false
        return state.visibilityMap[enemy.position] == Visibility.Visible
    }

    private fun resolveEnemyAttack(state: GameState, enemy: Enemy, random: Random): GameState {
        val result = CombatEngine.enemyAttackPlayer(enemy, state.player, random)
        return when (result) {
            is CombatResult.Miss -> {
                updateEnemyInState(state, enemy)
                    .withMessage("${result.attackerName} misses ${result.defenderName}!")
            }
            is CombatResult.Hit -> {
                var newPlayer = state.player.copy(hp = result.defenderHpAfter)
                var newState = updateEnemyInState(state.copy(player = newPlayer), enemy)
                    .withMessage("${result.attackerName} hits ${result.defenderName} for ${result.damage} damage.")

                // Roll for status effect
                val statusBuff = CombatEngine.rollEnemyStatusEffect(enemy.typeId, random)
                if (statusBuff != null) {
                    newPlayer = newState.player.copy(
                        activeBuffs = newState.player.activeBuffs + statusBuff
                    )
                    newState = newState.copy(player = newPlayer)
                        .withMessage("${enemy.displayName} inflicts ${statusBuff.name}!")
                }

                newState
            }
        }
    }

    private fun moveEnemyToward(state: GameState, enemy: Enemy, target: Position): GameState {
        val occupiedPositions = getOccupiedPositions(state, enemy)

        val path = Pathfinding.findPath(
            start = enemy.position,
            goal = target,
            dungeon = state.dungeon,
            blocked = occupiedPositions,
            maxDistance = 15,
            tileCost = { pos ->
                if (state.dungeon.tileAt(pos) == Tile.Trap) 5 else 1
            }
        )

        if (path != null && path.isNotEmpty()) {
            val nextPos = path.first()
            if (nextPos !in occupiedPositions) {
                return updateEnemyInState(state, enemy.copy(position = nextPos))
            }
        }

        return updateEnemyInState(state, enemy)
    }

    private fun moveEnemyAwayFrom(state: GameState, enemy: Enemy, threat: Position): GameState {
        val occupiedPositions = getOccupiedPositions(state, enemy)

        val bestMove = enemy.position.cardinalNeighbors()
            .filter {
                state.dungeon.isWalkable(it) && it !in occupiedPositions &&
                        state.dungeon.tileAt(it) != Tile.Trap
            }
            .maxByOrNull { it.distanceTo(threat) }

        return if (bestMove != null) {
            updateEnemyInState(state, enemy.copy(position = bestMove))
        } else {
            updateEnemyInState(state, enemy)
        }
    }

    private fun moveEnemyRandomly(state: GameState, enemy: Enemy, random: Random): GameState {
        val occupiedPositions = getOccupiedPositions(state, enemy)

        val walkable = enemy.position.cardinalNeighbors()
            .filter {
                state.dungeon.isWalkable(it) && it !in occupiedPositions &&
                        state.dungeon.tileAt(it) != Tile.Trap
            }

        if (walkable.isEmpty()) return updateEnemyInState(state, enemy)

        val target = walkable[random.nextInt(walkable.size)]
        return updateEnemyInState(state, enemy.copy(position = target))
    }

    private fun getOccupiedPositions(state: GameState, excludeEnemy: Enemy): Set<Position> {
        return state.enemies
            .filter { it.id != excludeEnemy.id && it.isAlive }
            .map { it.position }
            .toSet() + state.player.position
    }

    private fun updateEnemyInState(state: GameState, enemy: Enemy): GameState {
        return state.copy(enemies = state.enemies.map { if (it.id == enemy.id) enemy else it })
    }
}
