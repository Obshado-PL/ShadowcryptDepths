package com.shadowcrypt.game.model

enum class AiBehavior {
    Aggressive,
    Patrol,
    Ranged,
    Ambush,
    Support,
    Boss
}

data class Enemy(
    val id: Int,
    val typeId: String,
    val position: Position,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val mag: Int,
    val spd: Int,
    val xpReward: Int,
    val behavior: AiBehavior,
    val alertedByPlayer: Boolean = false,
    val displayName: String,
    val isBoss: Boolean = false,
    val bossPhase: Int = 1,
    val activeBuffs: List<ActiveBuff> = emptyList()
) {
    val isAlive: Boolean get() = hp > 0
    val hpFraction: Float get() = hp.toFloat() / maxHp.toFloat()
    val effectiveAtk: Int get() = atk + activeBuffs.sumOf { it.atkBonus }
    val effectiveDef: Int get() = def + activeBuffs.sumOf { it.defBonus }
    val effectiveSpd: Int get() = spd + activeBuffs.sumOf { it.spdBonus }
    val isStunned: Boolean get() = activeBuffs.any { it.statusEffect == StatusEffect.Stun }
}
