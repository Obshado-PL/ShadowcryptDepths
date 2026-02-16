package com.shadowcrypt.game.model

/** Difficulty modes that scale enemy stats, drop rates, and trap damage. */
enum class Difficulty(
    val displayName: String,
    val enemyStatScale: Float,
    val dropRateMultiplier: Float,
    val trapDamageBonus: Int,
    val enemyCountBonus: Int
) {
    Easy("Easy", 0.8f, 1.3f, -1, -1),
    Normal("Normal", 1.0f, 1.0f, 0, 0),
    Hard("Hard", 1.2f, 0.8f, 2, 1),
    Nightmare("Nightmare", 1.4f, 0.6f, 3, 2)
}

/** Complete snapshot of the game at any point in time */
data class GameState(
    val dungeon: DungeonFloor,
    val player: Player,
    val enemies: List<Enemy> = emptyList(),
    val enemiesKilled: Int = 0,
    val visibilityMap: Map<Position, Visibility>,
    val turnCount: Int = 0,
    val status: GameStatus = GameStatus.Playing,
    val messageLog: List<String> = emptyList(),
    val seed: Long,
    val floorItems: List<FloorItem> = emptyList(),
    val interactables: List<Interactable> = emptyList(),
    val lastTrapTriggered: Boolean = false,
    val lastCritical: Boolean = false,
    val difficulty: Difficulty = Difficulty.Normal
) {
    fun withMessage(msg: String): GameState =
        copy(messageLog = (messageLog + msg).takeLast(50))
}

enum class GameStatus {
    Playing,
    Victory,
    Dead
}
