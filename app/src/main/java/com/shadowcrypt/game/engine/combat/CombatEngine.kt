package com.shadowcrypt.game.engine.combat

import com.shadowcrypt.game.engine.model.EnemyData
import com.shadowcrypt.game.engine.model.PlayerData
import kotlin.math.pow

object CombatEngine {

    fun calculateDamage(attackerAttack: Int, defenderDefense: Int): Int {
        return maxOf(1, attackerAttack - defenderDefense / 2)
    }

    fun playerAttacksEnemy(player: PlayerData, enemy: EnemyData): Triple<EnemyData, Int, Boolean> {
        val damage = calculateDamage(player.effectiveAttack, enemy.defense)
        val newHp = (enemy.hp - damage).coerceAtLeast(0)
        val updatedEnemy = enemy.copy(hp = newHp)
        return Triple(updatedEnemy, damage, newHp <= 0)
    }

    fun enemyAttacksPlayer(enemy: EnemyData, player: PlayerData): Triple<PlayerData, Int, Boolean> {
        val damage = calculateDamage(enemy.attack, player.effectiveDefense)
        val newHp = (player.hp - damage).coerceAtLeast(0)
        val updatedPlayer = player.copy(hp = newHp)
        return Triple(updatedPlayer, damage, newHp <= 0)
    }

    fun checkLevelUp(player: PlayerData): Pair<PlayerData, Boolean> {
        if (player.xp >= player.xpToNextLevel) {
            val newLevel = player.level + 1
            val remainingXp = player.xp - player.xpToNextLevel
            val newMaxHp = player.maxHp + 5
            val effectiveMax = newMaxHp + player.inventory.equipment.totalMaxHpBonus
            return Pair(
                player.copy(
                    level = newLevel,
                    xp = remainingXp,
                    xpToNextLevel = xpForLevel(newLevel + 1),
                    maxHp = newMaxHp,
                    hp = effectiveMax,
                    attack = player.attack + 1,
                    defense = player.defense + 1
                ),
                true
            )
        }
        return Pair(player, false)
    }

    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        return (50 * 1.3.pow((level - 2).toDouble())).toInt()
    }
}
