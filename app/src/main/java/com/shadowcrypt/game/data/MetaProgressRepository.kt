package com.shadowcrypt.game.data

import com.shadowcrypt.game.data.model.MetaProgress
import com.shadowcrypt.game.data.model.UnlockCondition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MetaProgressRepository(private val dataStore: MetaProgressDataStore) {

    val progress: Flow<MetaProgress> = dataStore.progressFlow

    /**
     * Records a completed run and checks for new class unlocks.
     * Returns the set of newly-unlocked class IDs (may be empty).
     */
    suspend fun recordRun(
        floorReached: Int,
        enemiesKilled: Int,
        won: Boolean
    ): Set<String> {
        val current = dataStore.progressFlow.first()

        // Simulate the post-run state to check unlock conditions
        val postRunState = current.copy(
            totalRuns = current.totalRuns + 1,
            bestFloor = maxOf(current.bestFloor, floorReached),
            totalEnemiesKilled = current.totalEnemiesKilled + enemiesKilled,
            hasWon = current.hasWon || won
        )

        val newUnlocks = UnlockCondition.entries
            .filter { it.check(postRunState) }
            .map { it.classId }
            .toSet() - current.unlockedClassIds

        dataStore.recordRunCompletion(
            floorReached = floorReached,
            enemiesKilled = enemiesKilled,
            won = won,
            newUnlocks = newUnlocks
        )

        return newUnlocks
    }
}
