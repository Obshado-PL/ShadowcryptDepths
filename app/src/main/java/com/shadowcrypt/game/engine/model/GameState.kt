package com.shadowcrypt.game.engine.model

data class GameState(
    val dungeon: DungeonLevel,
    val player: PlayerData,
    val enemies: List<EnemyData> = emptyList(),
    val visibility: List<List<Visibility>>,
    val turnCount: Int,
    val seed: Long,
    val message: String? = null,
    val isPlayerDead: Boolean = false
)
