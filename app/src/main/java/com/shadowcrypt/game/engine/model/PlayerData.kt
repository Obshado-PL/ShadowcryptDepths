package com.shadowcrypt.game.engine.model

data class PlayerData(
    val position: Position,
    val hp: Int,
    val maxHp: Int,
    val attack: Int,
    val defense: Int,
    val xp: Int = 0,
    val level: Int = 1,
    val xpToNextLevel: Int = 50,
    val classId: String,
    val floorNumber: Int,
    val enemiesKilled: Int = 0
)
