package com.shadowcrypt.game.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.max

private val Context.dataStore by preferencesDataStore(name = "meta_progress")

class MetaProgressRepository(private val context: Context) {

    data class RunRecord(
        val timestamp: Long,
        val floorReached: Int,
        val enemiesKilled: Int,
        val turnsTaken: Int,
        val score: Int,
        val won: Boolean
    )

    data class MetaProgress(
        val totalRuns: Int = 0,
        val totalKills: Int = 0,
        val totalFloors: Int = 0,
        val highScore: Int = 0,
        val bestFloor: Int = 0,
        val victories: Int = 0,
        val unlockedAchievements: Set<String> = emptySet(),
        val runHistory: List<RunRecord> = emptyList(),
        val soulGems: Int = 0,
        val upgradeHp: Int = 0,
        val upgradeAtk: Int = 0,
        val upgradeDef: Int = 0,
        val upgradeMag: Int = 0,
        val upgradeSpd: Int = 0,
        val unlockedClasses: Set<String> = setOf("warrior"),
        val discoveredEnemies: Set<String> = emptySet()
    )

    private companion object {
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val TOTAL_KILLS = intPreferencesKey("total_kills")
        val TOTAL_FLOORS = intPreferencesKey("total_floors")
        val HIGH_SCORE = intPreferencesKey("high_score")
        val BEST_FLOOR = intPreferencesKey("best_floor")
        val VICTORIES = intPreferencesKey("victories")
        val ACHIEVEMENTS = stringSetPreferencesKey("achievements")
        val RUN_HISTORY = stringSetPreferencesKey("run_history")
        val SOUL_GEMS = intPreferencesKey("soul_gems")
        val UPGRADE_HP = intPreferencesKey("upgrade_hp")
        val UPGRADE_ATK = intPreferencesKey("upgrade_atk")
        val UPGRADE_DEF = intPreferencesKey("upgrade_def")
        val UPGRADE_MAG = intPreferencesKey("upgrade_mag")
        val UPGRADE_SPD = intPreferencesKey("upgrade_spd")
        val UNLOCKED_CLASSES = stringSetPreferencesKey("unlocked_classes")
        val DISCOVERED_ENEMIES = stringSetPreferencesKey("discovered_enemies")
    }

    val progress: Flow<MetaProgress> = context.dataStore.data.map { prefs ->
        MetaProgress(
            totalRuns = prefs[TOTAL_RUNS] ?: 0,
            totalKills = prefs[TOTAL_KILLS] ?: 0,
            totalFloors = prefs[TOTAL_FLOORS] ?: 0,
            highScore = prefs[HIGH_SCORE] ?: 0,
            bestFloor = prefs[BEST_FLOOR] ?: 0,
            victories = prefs[VICTORIES] ?: 0,
            unlockedAchievements = prefs[ACHIEVEMENTS] ?: emptySet(),
            runHistory = parseRunHistory(prefs[RUN_HISTORY] ?: emptySet()),
            soulGems = prefs[SOUL_GEMS] ?: 0,
            upgradeHp = prefs[UPGRADE_HP] ?: 0,
            upgradeAtk = prefs[UPGRADE_ATK] ?: 0,
            upgradeDef = prefs[UPGRADE_DEF] ?: 0,
            upgradeMag = prefs[UPGRADE_MAG] ?: 0,
            upgradeSpd = prefs[UPGRADE_SPD] ?: 0,
            unlockedClasses = prefs[UNLOCKED_CLASSES] ?: setOf("warrior"),
            discoveredEnemies = prefs[DISCOVERED_ENEMIES] ?: emptySet()
        )
    }

    suspend fun recordRun(
        floorReached: Int, enemiesKilled: Int, turnsTaken: Int = 0,
        score: Int, won: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOTAL_RUNS] = (prefs[TOTAL_RUNS] ?: 0) + 1
            prefs[TOTAL_KILLS] = (prefs[TOTAL_KILLS] ?: 0) + enemiesKilled
            prefs[TOTAL_FLOORS] = (prefs[TOTAL_FLOORS] ?: 0) + floorReached
            prefs[HIGH_SCORE] = max(prefs[HIGH_SCORE] ?: 0, score)
            prefs[BEST_FLOOR] = max(prefs[BEST_FLOOR] ?: 0, floorReached)
            if (won) {
                prefs[VICTORIES] = (prefs[VICTORIES] ?: 0) + 1
            }

            // Award soul gems: 5 per floor + 2 per kill + 50 for victory
            val gems = floorReached * 5 + enemiesKilled * 2 + if (won) 50 else 0
            prefs[SOUL_GEMS] = (prefs[SOUL_GEMS] ?: 0) + gems

            // Auto-unlock classes based on progress
            val classes = (prefs[UNLOCKED_CLASSES] ?: setOf("warrior")).toMutableSet()
            val totalRuns = prefs[TOTAL_RUNS] ?: 0
            val bestFloor = prefs[BEST_FLOOR] ?: 0
            val victories = prefs[VICTORIES] ?: 0
            if (totalRuns >= 1) classes.add("rogue")       // Unlock rogue after 1 run
            if (bestFloor >= 3) classes.add("mage")         // Unlock mage after reaching floor 3
            if (bestFloor >= 5) classes.add("cleric")       // Unlock cleric after reaching floor 5
            prefs[UNLOCKED_CLASSES] = classes

            // Save run to history (keep last 20)
            val history = (prefs[RUN_HISTORY] ?: emptySet()).toMutableSet()
            val entry = "${System.currentTimeMillis()}|$floorReached|$enemiesKilled|$turnsTaken|$score|$won"
            history.add(entry)
            if (history.size > 20) {
                val sorted = history.sortedByDescending {
                    it.split("|").firstOrNull()?.toLongOrNull() ?: 0L
                }
                prefs[RUN_HISTORY] = sorted.take(20).toSet()
            } else {
                prefs[RUN_HISTORY] = history
            }

            // Check and unlock achievements
            val current = prefs[ACHIEVEMENTS]?.toMutableSet() ?: mutableSetOf()
            val aTotalKills = prefs[TOTAL_KILLS] ?: 0

            if (aTotalKills >= 1) current.add("first_blood")
            if (bestFloor >= 5) current.add("floor_5")
            if (bestFloor >= 10) current.add("floor_10")
            if (aTotalKills >= 100) current.add("slayer_100")
            if (victories >= 1) current.add("first_victory")
            if (totalRuns >= 10) current.add("runs_10")

            prefs[ACHIEVEMENTS] = current
        }
    }

    fun upgradeCost(level: Int): Int = 10 + level * 15

    suspend fun purchaseUpgrade(stat: String): Boolean {
        var success = false
        context.dataStore.edit { prefs ->
            val gems = prefs[SOUL_GEMS] ?: 0
            val key = when (stat) {
                "hp" -> UPGRADE_HP
                "atk" -> UPGRADE_ATK
                "def" -> UPGRADE_DEF
                "mag" -> UPGRADE_MAG
                "spd" -> UPGRADE_SPD
                else -> return@edit
            }
            val level = prefs[key] ?: 0
            val cost = upgradeCost(level)
            if (gems >= cost && level < 10) {
                prefs[SOUL_GEMS] = gems - cost
                prefs[key] = level + 1
                success = true
            }
        }
        return success
    }

    suspend fun discoverEnemies(enemyTypeIds: Set<String>) {
        if (enemyTypeIds.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = (prefs[DISCOVERED_ENEMIES] ?: emptySet()).toMutableSet()
            current.addAll(enemyTypeIds)
            prefs[DISCOVERED_ENEMIES] = current
        }
    }

    private fun parseRunHistory(entries: Set<String>): List<RunRecord> {
        return entries.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size >= 6) {
                RunRecord(
                    timestamp = parts[0].toLongOrNull() ?: 0L,
                    floorReached = parts[1].toIntOrNull() ?: 0,
                    enemiesKilled = parts[2].toIntOrNull() ?: 0,
                    turnsTaken = parts[3].toIntOrNull() ?: 0,
                    score = parts[4].toIntOrNull() ?: 0,
                    won = parts[5].toBooleanStrictOrNull() ?: false
                )
            } else null
        }.sortedByDescending { it.timestamp }
    }

}
