package com.shadowcrypt.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shadowcrypt.game.data.model.MetaProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.metaProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "meta_progress")

class MetaProgressDataStore(private val context: Context) {

    private object Keys {
        val TOTAL_RUNS = intPreferencesKey("total_runs")
        val BEST_FLOOR = intPreferencesKey("best_floor")
        val TOTAL_ENEMIES_KILLED = intPreferencesKey("total_enemies_killed")
        val HAS_WON = booleanPreferencesKey("has_won")
        val UNLOCKED_CLASS_IDS = stringSetPreferencesKey("unlocked_class_ids")
    }

    val progressFlow: Flow<MetaProgress> = context.metaProgressDataStore.data.map { prefs ->
        MetaProgress(
            totalRuns = prefs[Keys.TOTAL_RUNS] ?: 0,
            bestFloor = prefs[Keys.BEST_FLOOR] ?: 0,
            totalEnemiesKilled = prefs[Keys.TOTAL_ENEMIES_KILLED] ?: 0,
            hasWon = prefs[Keys.HAS_WON] ?: false,
            unlockedClassIds = prefs[Keys.UNLOCKED_CLASS_IDS] ?: setOf("warrior")
        )
    }

    suspend fun recordRunCompletion(
        floorReached: Int,
        enemiesKilled: Int,
        won: Boolean,
        newUnlocks: Set<String>
    ) {
        context.metaProgressDataStore.edit { prefs ->
            val currentRuns = prefs[Keys.TOTAL_RUNS] ?: 0
            val currentBestFloor = prefs[Keys.BEST_FLOOR] ?: 0
            val currentKills = prefs[Keys.TOTAL_ENEMIES_KILLED] ?: 0
            val currentUnlocks = prefs[Keys.UNLOCKED_CLASS_IDS] ?: setOf("warrior")

            prefs[Keys.TOTAL_RUNS] = currentRuns + 1
            prefs[Keys.BEST_FLOOR] = maxOf(currentBestFloor, floorReached)
            prefs[Keys.TOTAL_ENEMIES_KILLED] = currentKills + enemiesKilled
            if (won) prefs[Keys.HAS_WON] = true
            prefs[Keys.UNLOCKED_CLASS_IDS] = currentUnlocks + newUnlocks
        }
    }
}
