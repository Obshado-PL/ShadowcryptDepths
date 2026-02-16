package com.shadowcrypt.game.model

enum class QuestType {
    KillEnemies,
    KillBoss,
    FindItem,
    ReachStairs,
    SurviveTurns
}

data class Quest(
    val id: String,
    val type: QuestType,
    val description: String,
    val targetCount: Int,
    val progress: Int = 0,
    val rewardXp: Int,
    val completed: Boolean = false
) {
    val progressFraction: Float get() = if (targetCount > 0) progress.toFloat() / targetCount else 0f
    val isComplete: Boolean get() = progress >= targetCount
}

object QuestGenerator {
    fun generateForFloor(floor: Int, seed: Long): List<Quest> {
        val random = kotlin.random.Random(seed + floor * 5000L)
        val quests = mutableListOf<Quest>()

        // Always: kill quest
        val killCount = 3 + floor
        quests.add(
            Quest(
                id = "kill_$floor",
                type = QuestType.KillEnemies,
                description = "Defeat $killCount enemies",
                targetCount = killCount,
                rewardXp = killCount * 5
            )
        )

        // Floor 3+: survival quest
        if (floor >= 3 && random.nextInt(100) < 60) {
            val turns = 20 + floor * 5
            quests.add(
                Quest(
                    id = "survive_$floor",
                    type = QuestType.SurviveTurns,
                    description = "Survive $turns turns",
                    targetCount = turns,
                    rewardXp = turns / 2
                )
            )
        }

        // Boss floors: kill the boss
        if (EnemyTypes.bossForFloor(floor) != null) {
            val boss = EnemyTypes.bossForFloor(floor)!!
            quests.add(
                Quest(
                    id = "boss_$floor",
                    type = QuestType.KillBoss,
                    description = "Defeat ${boss.displayName}",
                    targetCount = 1,
                    rewardXp = boss.baseXpReward
                )
            )
        }

        return quests
    }
}
