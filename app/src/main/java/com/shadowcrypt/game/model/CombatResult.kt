package com.shadowcrypt.game.model

sealed class CombatResult {
    data class Miss(
        val attackerName: String,
        val defenderName: String
    ) : CombatResult()

    data class Hit(
        val attackerName: String,
        val defenderName: String,
        val damage: Int,
        val isCritical: Boolean,
        val defenderHpAfter: Int,
        val defenderKilled: Boolean
    ) : CombatResult()
}
