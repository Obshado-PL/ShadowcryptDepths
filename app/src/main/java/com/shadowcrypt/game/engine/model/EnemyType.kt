package com.shadowcrypt.game.engine.model

enum class EnemyType(
    val displayName: String,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseXpReward: Int,
    val detectionRange: Int,
    val behavior: EnemyBehavior,
    val minFloor: Int,
    val maxFloor: Int
) {
    // Crypt (floors 1-2)
    SKELETON("Skeleton", baseHp = 12, baseAttack = 4, baseDefense = 1, baseXpReward = 10, detectionRange = 6, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 1, maxFloor = 3),
    ZOMBIE("Zombie", baseHp = 18, baseAttack = 3, baseDefense = 2, baseXpReward = 12, detectionRange = 4, behavior = EnemyBehavior.PATROL, minFloor = 1, maxFloor = 3),

    // Sewers (floors 3-4)
    GIANT_RAT("Giant Rat", baseHp = 8, baseAttack = 5, baseDefense = 0, baseXpReward = 8, detectionRange = 7, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 2, maxFloor = 5),
    SLIME("Slime", baseHp = 20, baseAttack = 3, baseDefense = 3, baseXpReward = 14, detectionRange = 3, behavior = EnemyBehavior.PATROL, minFloor = 3, maxFloor = 5),

    // Caverns (floors 5-6)
    SPIDER("Spider", baseHp = 14, baseAttack = 7, baseDefense = 2, baseXpReward = 18, detectionRange = 6, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 4, maxFloor = 7),
    GOLEM("Golem", baseHp = 30, baseAttack = 6, baseDefense = 5, baseXpReward = 25, detectionRange = 4, behavior = EnemyBehavior.PATROL, minFloor = 5, maxFloor = 7),

    // Inferno (floors 7-8)
    FIRE_IMP("Fire Imp", baseHp = 16, baseAttack = 9, baseDefense = 3, baseXpReward = 22, detectionRange = 7, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 6, maxFloor = 9),
    DEMON("Demon", baseHp = 35, baseAttack = 10, baseDefense = 5, baseXpReward = 35, detectionRange = 6, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 7, maxFloor = 9),

    // Void (floors 9-10)
    WRAITH("Wraith", baseHp = 20, baseAttack = 11, baseDefense = 2, baseXpReward = 30, detectionRange = 8, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 8, maxFloor = 10),
    SHADOW("Shadow", baseHp = 40, baseAttack = 12, baseDefense = 6, baseXpReward = 45, detectionRange = 7, behavior = EnemyBehavior.AGGRESSIVE, minFloor = 9, maxFloor = 10)
}

enum class EnemyBehavior {
    AGGRESSIVE,
    PATROL
}
