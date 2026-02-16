package com.shadowcrypt.game.model

enum class StatusEffect {
    Poison,
    Burn,
    Stun,
    Slow
}

data class ActiveBuff(
    val name: String,
    val atkBonus: Int = 0,
    val defBonus: Int = 0,
    val magBonus: Int = 0,
    val spdBonus: Int = 0,
    val turnsRemaining: Int,
    val statusEffect: StatusEffect? = null,
    val dotDamage: Int = 0
)
