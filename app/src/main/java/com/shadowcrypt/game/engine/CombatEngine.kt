package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.ActiveBuff
import com.shadowcrypt.game.model.CombatResult
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.Player
import com.shadowcrypt.game.model.StatusEffect
import kotlin.math.max
import kotlin.random.Random

object CombatEngine {

    fun playerAttackEnemy(player: Player, enemy: Enemy, random: Random): CombatResult {
        val hitChance = (75 + (player.effectiveSpd - enemy.effectiveSpd) * 3).coerceIn(30, 95)
        if (random.nextInt(100) >= hitChance) {
            return CombatResult.Miss("You", enemy.displayName)
        }

        val baseCritChance = if (player.classId == "rogue") 15 else 5
        val isCritical = random.nextInt(100) < baseCritChance

        val useMagic = player.classId == "mage" || player.classId == "cleric"
        val rawDamage = if (useMagic) {
            player.effectiveMag - enemy.effectiveDef / 4
        } else {
            player.effectiveAtk - enemy.effectiveDef / 2
        }

        val variance = random.nextInt(-2, 3)
        var damage = max(1, rawDamage + variance)
        if (isCritical) damage = max(damage + 1, (damage * 1.5f).toInt())

        val hpAfter = max(0, enemy.hp - damage)
        return CombatResult.Hit(
            attackerName = "You",
            defenderName = enemy.displayName,
            damage = damage,
            isCritical = isCritical,
            defenderHpAfter = hpAfter,
            defenderKilled = hpAfter <= 0
        )
    }

    fun enemyAttackPlayer(enemy: Enemy, player: Player, random: Random): CombatResult {
        val hitChance = (70 + (enemy.effectiveSpd - player.effectiveSpd) * 3).coerceIn(25, 90)
        if (random.nextInt(100) >= hitChance) {
            return CombatResult.Miss(enemy.displayName, "you")
        }

        val useMagic = enemy.mag > enemy.effectiveAtk
        val rawDamage = if (useMagic) {
            enemy.mag - player.effectiveDef / 4
        } else {
            enemy.effectiveAtk - player.effectiveDef / 2
        }

        val variance = random.nextInt(-2, 3)
        val damage = max(1, rawDamage + variance)

        val hpAfter = max(0, player.hp - damage)
        return CombatResult.Hit(
            attackerName = enemy.displayName,
            defenderName = "you",
            damage = damage,
            isCritical = false,
            defenderHpAfter = hpAfter,
            defenderKilled = hpAfter <= 0
        )
    }

    fun supportHeal(healer: Enemy, target: Enemy, random: Random): Int {
        val healAmount = max(1, healer.mag / 2 + random.nextInt(0, 3))
        return healAmount.coerceAtMost(target.maxHp - target.hp)
    }

    /** Returns a status effect buff the player's class can inflict, or null. */
    fun rollPlayerStatusEffect(classId: String, random: Random): ActiveBuff? {
        return when (classId) {
            "warrior" -> if (random.nextInt(100) < 15) {
                ActiveBuff("Stun", turnsRemaining = 1, statusEffect = StatusEffect.Stun)
            } else null
            "rogue" -> if (random.nextInt(100) < 20) {
                ActiveBuff("Poison", turnsRemaining = 3, statusEffect = StatusEffect.Poison, dotDamage = 3)
            } else null
            "mage" -> if (random.nextInt(100) < 20) {
                ActiveBuff("Burn", turnsRemaining = 2, statusEffect = StatusEffect.Burn, dotDamage = 4)
            } else null
            "cleric" -> if (random.nextInt(100) < 15) {
                ActiveBuff("Slow", spdBonus = -3, turnsRemaining = 3, statusEffect = StatusEffect.Slow)
            } else null
            else -> null
        }
    }

    /** Returns a status effect buff certain enemies can inflict, or null. */
    fun rollEnemyStatusEffect(typeId: String, random: Random): ActiveBuff? {
        return when (typeId) {
            "toxic_toad", "plague_rat" -> if (random.nextInt(100) < 25) {
                ActiveBuff("Poison", turnsRemaining = 3, statusEffect = StatusEffect.Poison, dotDamage = 2)
            } else null
            "fire_imp", "lava_elemental" -> if (random.nextInt(100) < 20) {
                ActiveBuff("Burn", turnsRemaining = 2, statusEffect = StatusEffect.Burn, dotDamage = 3)
            } else null
            "stone_golem" -> if (random.nextInt(100) < 10) {
                ActiveBuff("Stun", turnsRemaining = 1, statusEffect = StatusEffect.Stun)
            } else null
            "phase_shifter" -> if (random.nextInt(100) < 20) {
                ActiveBuff("Slow", spdBonus = -3, turnsRemaining = 3, statusEffect = StatusEffect.Slow)
            } else null
            else -> null
        }
    }
}
