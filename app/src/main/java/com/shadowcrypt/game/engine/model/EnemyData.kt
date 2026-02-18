package com.shadowcrypt.game.engine.model

data class EnemyData(
    val id: Int,
    val type: EnemyType,
    val position: Position,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val xpReward: Int
)
