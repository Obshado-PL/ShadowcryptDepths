package com.shadowcrypt.game.model

data class EnemyType(
    val id: String,
    val displayName: String,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseMag: Int,
    val baseSpd: Int,
    val baseXpReward: Int,
    val behavior: AiBehavior,
    val minFloor: Int,
    val maxFloor: Int,
    val isBoss: Boolean = false,
    val spawnWeight: Int = 10
)

object EnemyTypes {
    // ===== Crypt (Floors 1-2) =====
    val Skeleton = EnemyType(
        id = "skeleton", displayName = "Skeleton",
        baseHp = 12, baseAtk = 4, baseDef = 2, baseMag = 0, baseSpd = 5,
        baseXpReward = 5, behavior = AiBehavior.Aggressive,
        minFloor = 1, maxFloor = 3, spawnWeight = 15
    )
    val Zombie = EnemyType(
        id = "zombie", displayName = "Zombie",
        baseHp = 18, baseAtk = 5, baseDef = 3, baseMag = 0, baseSpd = 3,
        baseXpReward = 7, behavior = AiBehavior.Patrol,
        minFloor = 1, maxFloor = 3, spawnWeight = 12
    )
    val GhostlyWisp = EnemyType(
        id = "ghostly_wisp", displayName = "Ghostly Wisp",
        baseHp = 8, baseAtk = 3, baseDef = 1, baseMag = 4, baseSpd = 8,
        baseXpReward = 6, behavior = AiBehavior.Ranged,
        minFloor = 1, maxFloor = 4, spawnWeight = 8
    )
    val CryptBat = EnemyType(
        id = "crypt_bat", displayName = "Crypt Bat",
        baseHp = 6, baseAtk = 3, baseDef = 1, baseMag = 0, baseSpd = 10,
        baseXpReward = 3, behavior = AiBehavior.Aggressive,
        minFloor = 1, maxFloor = 2, spawnWeight = 10
    )

    // ===== Sewers (Floors 3-4) =====
    val RatSwarm = EnemyType(
        id = "rat_swarm", displayName = "Rat Swarm",
        baseHp = 10, baseAtk = 6, baseDef = 1, baseMag = 0, baseSpd = 9,
        baseXpReward = 8, behavior = AiBehavior.Aggressive,
        minFloor = 3, maxFloor = 5, spawnWeight = 14
    )
    val SewerSlime = EnemyType(
        id = "sewer_slime", displayName = "Sewer Slime",
        baseHp = 22, baseAtk = 4, baseDef = 5, baseMag = 2, baseSpd = 3,
        baseXpReward = 10, behavior = AiBehavior.Patrol,
        minFloor = 3, maxFloor = 5, spawnWeight = 10
    )
    val ToxicToad = EnemyType(
        id = "toxic_toad", displayName = "Toxic Toad",
        baseHp = 15, baseAtk = 5, baseDef = 3, baseMag = 5, baseSpd = 6,
        baseXpReward = 9, behavior = AiBehavior.Ambush,
        minFloor = 3, maxFloor = 5, spawnWeight = 10
    )
    val PlagueRat = EnemyType(
        id = "plague_rat", displayName = "Plague Rat",
        baseHp = 8, baseAtk = 4, baseDef = 1, baseMag = 0, baseSpd = 11,
        baseXpReward = 6, behavior = AiBehavior.Aggressive,
        minFloor = 3, maxFloor = 4, spawnWeight = 12
    )

    // ===== Floor 3 Mini-Boss =====
    val CryptGuardian = EnemyType(
        id = "crypt_guardian", displayName = "Crypt Guardian",
        baseHp = 50, baseAtk = 9, baseDef = 6, baseMag = 3, baseSpd = 5,
        baseXpReward = 35, behavior = AiBehavior.Boss,
        minFloor = 3, maxFloor = 3, isBoss = true, spawnWeight = 0
    )

    // ===== Floor 5 Boss =====
    val BoneWarden = EnemyType(
        id = "bone_warden", displayName = "Bone Warden",
        baseHp = 80, baseAtk = 12, baseDef = 8, baseMag = 4, baseSpd = 6,
        baseXpReward = 60, behavior = AiBehavior.Boss,
        minFloor = 5, maxFloor = 5, isBoss = true, spawnWeight = 0
    )

    // ===== Caverns (Floors 5-6) =====
    val CaveSpider = EnemyType(
        id = "cave_spider", displayName = "Cave Spider",
        baseHp = 14, baseAtk = 7, baseDef = 3, baseMag = 0, baseSpd = 10,
        baseXpReward = 11, behavior = AiBehavior.Ambush,
        minFloor = 5, maxFloor = 7, spawnWeight = 12
    )
    val StoneGolem = EnemyType(
        id = "stone_golem", displayName = "Stone Golem",
        baseHp = 35, baseAtk = 10, baseDef = 10, baseMag = 0, baseSpd = 3,
        baseXpReward = 18, behavior = AiBehavior.Patrol,
        minFloor = 5, maxFloor = 7, spawnWeight = 8
    )
    val CrystalSentinel = EnemyType(
        id = "crystal_sentinel", displayName = "Crystal Sentinel",
        baseHp = 20, baseAtk = 6, baseDef = 6, baseMag = 7, baseSpd = 5,
        baseXpReward = 14, behavior = AiBehavior.Support,
        minFloor = 5, maxFloor = 7, spawnWeight = 6
    )
    val Troglodyte = EnemyType(
        id = "troglodyte", displayName = "Troglodyte",
        baseHp = 16, baseAtk = 8, baseDef = 4, baseMag = 0, baseSpd = 7,
        baseXpReward = 12, behavior = AiBehavior.Aggressive,
        minFloor = 5, maxFloor = 6, spawnWeight = 10
    )

    // ===== Inferno (Floors 7-8) =====
    val FireImp = EnemyType(
        id = "fire_imp", displayName = "Fire Imp",
        baseHp = 16, baseAtk = 5, baseDef = 3, baseMag = 9, baseSpd = 9,
        baseXpReward = 16, behavior = AiBehavior.Ranged,
        minFloor = 7, maxFloor = 9, spawnWeight = 12
    )
    val LavaElemental = EnemyType(
        id = "lava_elemental", displayName = "Lava Elemental",
        baseHp = 40, baseAtk = 12, baseDef = 8, baseMag = 6, baseSpd = 4,
        baseXpReward = 22, behavior = AiBehavior.Aggressive,
        minFloor = 7, maxFloor = 9, spawnWeight = 8
    )
    val HellHound = EnemyType(
        id = "hell_hound", displayName = "Hell Hound",
        baseHp = 20, baseAtk = 10, baseDef = 4, baseMag = 0, baseSpd = 12,
        baseXpReward = 18, behavior = AiBehavior.Aggressive,
        minFloor = 7, maxFloor = 8, spawnWeight = 10
    )
    val InfernalPriest = EnemyType(
        id = "infernal_priest", displayName = "Infernal Priest",
        baseHp = 22, baseAtk = 4, baseDef = 5, baseMag = 10, baseSpd = 6,
        baseXpReward = 20, behavior = AiBehavior.Support,
        minFloor = 7, maxFloor = 9, spawnWeight = 6
    )

    // ===== Floor 7 Mini-Boss =====
    val InfernalChampion = EnemyType(
        id = "infernal_champion", displayName = "Infernal Champion",
        baseHp = 100, baseAtk = 14, baseDef = 9, baseMag = 8, baseSpd = 7,
        baseXpReward = 80, behavior = AiBehavior.Boss,
        minFloor = 7, maxFloor = 7, isBoss = true, spawnWeight = 0
    )

    // ===== Void (Floors 9-10) =====
    val VoidWraith = EnemyType(
        id = "void_wraith", displayName = "Void Wraith",
        baseHp = 24, baseAtk = 11, baseDef = 5, baseMag = 8, baseSpd = 10,
        baseXpReward = 24, behavior = AiBehavior.Aggressive,
        minFloor = 9, maxFloor = 10, spawnWeight = 12
    )
    val PhaseShifter = EnemyType(
        id = "phase_shifter", displayName = "Phase Shifter",
        baseHp = 18, baseAtk = 8, baseDef = 3, baseMag = 10, baseSpd = 13,
        baseXpReward = 22, behavior = AiBehavior.Ranged,
        minFloor = 9, maxFloor = 10, spawnWeight = 8
    )
    val EntropyStalker = EnemyType(
        id = "entropy_stalker", displayName = "Entropy Stalker",
        baseHp = 20, baseAtk = 13, baseDef = 4, baseMag = 0, baseSpd = 11,
        baseXpReward = 26, behavior = AiBehavior.Ambush,
        minFloor = 9, maxFloor = 10, spawnWeight = 10
    )
    val AbyssalEye = EnemyType(
        id = "abyssal_eye", displayName = "Abyssal Eye",
        baseHp = 14, baseAtk = 5, baseDef = 4, baseMag = 12, baseSpd = 7,
        baseXpReward = 20, behavior = AiBehavior.Ranged,
        minFloor = 9, maxFloor = 10, spawnWeight = 8
    )

    // ===== Floor 10 Final Boss =====
    val ShadowcryptLord = EnemyType(
        id = "shadowcrypt_lord", displayName = "The Shadowcrypt Lord",
        baseHp = 150, baseAtk = 16, baseDef = 10, baseMag = 14, baseSpd = 8,
        baseXpReward = 200, behavior = AiBehavior.Boss,
        minFloor = 10, maxFloor = 10, isBoss = true, spawnWeight = 0
    )

    val all: List<EnemyType> = listOf(
        Skeleton, Zombie, GhostlyWisp, CryptBat,
        CryptGuardian,
        RatSwarm, SewerSlime, ToxicToad, PlagueRat,
        BoneWarden,
        CaveSpider, StoneGolem, CrystalSentinel, Troglodyte,
        InfernalChampion,
        FireImp, LavaElemental, HellHound, InfernalPriest,
        VoidWraith, PhaseShifter, EntropyStalker, AbyssalEye,
        ShadowcryptLord
    )

    fun forFloor(floor: Int): List<EnemyType> =
        all.filter { !it.isBoss && floor in it.minFloor..it.maxFloor }

    fun bossForFloor(floor: Int): EnemyType? = when (floor) {
        3 -> CryptGuardian
        5 -> BoneWarden
        7 -> InfernalChampion
        10 -> ShadowcryptLord
        else -> null
    }
}
