package com.shadowcrypt.game.model

/** Playable character class with base stats */
data class CharacterClass(
    val id: String,
    val displayName: String,
    val baseHp: Int,
    val baseAtk: Int,
    val baseDef: Int,
    val baseMag: Int,
    val baseSpd: Int,
    val description: String,
    val attackRange: Int = 1
) {
    companion object {
        val Warrior = CharacterClass(
            id = "warrior", displayName = "Warrior",
            baseHp = 60, baseAtk = 10, baseDef = 8, baseMag = 2, baseSpd = 8,
            description = "A stalwart fighter with high health and armor."
        )
        val Rogue = CharacterClass(
            id = "rogue", displayName = "Rogue",
            baseHp = 40, baseAtk = 8, baseDef = 4, baseMag = 3, baseSpd = 14,
            description = "A swift striker who excels at evasion and critical hits."
        )
        val Mage = CharacterClass(
            id = "mage", displayName = "Mage",
            baseHp = 35, baseAtk = 3, baseDef = 3, baseMag = 12, baseSpd = 9,
            description = "A powerful spellcaster with devastating magical attacks.",
            attackRange = 3
        )
        val Cleric = CharacterClass(
            id = "cleric", displayName = "Cleric",
            baseHp = 50, baseAtk = 6, baseDef = 6, baseMag = 8, baseSpd = 7,
            description = "A holy healer who balances offense and defense.",
            attackRange = 2
        )

        val all: Map<String, CharacterClass> = listOf(
            Warrior, Rogue, Mage, Cleric
        ).associateBy { it.id }

        fun fromId(id: String): CharacterClass = all[id] ?: Warrior
    }
}
