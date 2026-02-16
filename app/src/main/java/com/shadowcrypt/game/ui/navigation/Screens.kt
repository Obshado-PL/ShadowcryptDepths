package com.shadowcrypt.game.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Shadowcrypt Depths.
 *
 * Each object/class represents a screen destination in the app.
 * Using @Serializable with Navigation Compose gives us type-safe
 * argument passing between screens — no more string-based routes!
 *
 * Screen flow:
 *   MainMenu → ClassSelect → Game(classId, seed) → GameOver(stats)
 *       ↑                                               |
 *       └───────────────────────────────────────────────┘
 *       ↕           ↕
 *    Settings     Unlocks
 */

/** The main menu screen shown when the app opens */
@Serializable
object MainMenuRoute

/** Character class selection screen before starting a run */
@Serializable
data class ClassSelectRoute(val isDaily: Boolean = false)

/**
 * The main dungeon gameplay screen.
 *
 * @param classId The selected character class identifier (e.g., "warrior", "rogue")
 * @param seed Random seed for dungeon generation (enables reproducible dungeons)
 */
@Serializable
data class GameRoute(
    val classId: String,
    val seed: Long = System.currentTimeMillis(),
    val loadSave: Boolean = false,
    val difficulty: String = "Normal",
    val upgradeHp: Int = 0,
    val upgradeAtk: Int = 0,
    val upgradeDef: Int = 0,
    val upgradeMag: Int = 0,
    val upgradeSpd: Int = 0
)

/**
 * Game over screen showing run statistics and meta-progression earned.
 *
 * @param floorReached The deepest floor the player reached
 * @param enemiesKilled Total enemies defeated during the run
 * @param turnsTaken Total turns taken during the run
 * @param score Calculated run score
 * @param won Whether the player defeated the final boss
 */
@Serializable
data class GameOverRoute(
    val floorReached: Int,
    val enemiesKilled: Int,
    val turnsTaken: Int,
    val score: Int,
    val won: Boolean,
    val lastMessages: String = ""
)

/** Settings screen for sound, haptics, and accessibility toggles */
@Serializable
object SettingsRoute

/** Unlocks screen showing all permanent meta-progression */
@Serializable
object UnlocksRoute

/** Run history screen showing past run records */
@Serializable
object RunHistoryRoute
