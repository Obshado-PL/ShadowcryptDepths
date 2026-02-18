package com.shadowcrypt.game.engine.model

private const val MAX_LOG_ENTRIES = 30

data class GameState(
    val dungeon: DungeonLevel,
    val player: PlayerData,
    val enemies: List<EnemyData> = emptyList(),
    val visibility: List<List<Visibility>>,
    val turnCount: Int,
    val seed: Long,
    val message: String? = null,
    val isPlayerDead: Boolean = false,
    val isVictorious: Boolean = false,
    val groundItems: List<Pair<ItemData, Position>> = emptyList(),
    val nextItemId: Int = 0,
    val combatLog: List<String> = emptyList()
) {
    fun appendLog(entry: String): GameState {
        val newLog = (combatLog + entry).takeLast(MAX_LOG_ENTRIES)
        return copy(combatLog = newLog)
    }
}
