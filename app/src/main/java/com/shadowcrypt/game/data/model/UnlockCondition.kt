package com.shadowcrypt.game.data.model

enum class UnlockCondition(
    val classId: String,
    val displayName: String,
    val description: String,
    val check: (MetaProgress) -> Boolean
) {
    WARRIOR(
        classId = "warrior",
        displayName = "Warrior",
        description = "Always available",
        check = { true }
    ),
    ROGUE(
        classId = "rogue",
        displayName = "Rogue",
        description = "Complete 5 runs",
        check = { it.totalRuns >= 5 }
    ),
    MAGE(
        classId = "mage",
        displayName = "Mage",
        description = "Complete 10 runs",
        check = { it.totalRuns >= 10 }
    ),
    CLERIC(
        classId = "cleric",
        displayName = "Cleric",
        description = "Reach floor 5",
        check = { it.bestFloor >= 5 }
    )
}
