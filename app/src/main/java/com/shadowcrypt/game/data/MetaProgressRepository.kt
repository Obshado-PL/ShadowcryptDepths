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
        val runHistory: List<RunRecord> = emptyList()
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
            runHistory = parseRunHistory(prefs[RUN_HISTORY] ?: emptySet())
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
            val totalKills = prefs[TOTAL_KILLS] ?: 0
            val bestFloor = prefs[BEST_FLOOR] ?: 0
            val totalRuns = prefs[TOTAL_RUNS] ?: 0
            val victories = prefs[VICTORIES] ?: 0

            if (totalKills >= 1) current.add("first_blood")
            if (bestFloor >= 5) current.add("floor_5")
            if (bestFloor >= 10) current.add("floor_10")
            if (totalKills >= 100) current.add("slayer_100")
            if (victories >= 1) current.add("first_victory")
            if (totalRuns >= 10) current.add("runs_10")

            prefs[ACHIEVEMENTS] = current
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
