package com.shadowcrypt.game.data.model

data class MetaProgress(
    val totalRuns: Int = 0,
    val bestFloor: Int = 0,
    val totalEnemiesKilled: Int = 0,
    val hasWon: Boolean = false,
    val unlockedClassIds: Set<String> = setOf("warrior")
)
