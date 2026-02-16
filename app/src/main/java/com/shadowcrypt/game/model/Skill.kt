package com.shadowcrypt.game.model

/** Targeting mode determines how a skill selects its targets. */
enum class SkillTarget {
    /** Hits a single enemy the player is facing or nearest visible */
    SingleEnemy,
    /** Hits all visible enemies within range */
    AllEnemies,
    /** Affects the player (self-buff/heal) */
    Self,
    /** Hits enemies in a radius around a point */
    AreaOfEffect
}

/** A class ability with cooldown and effect. */
data class Skill(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String,
    val cooldown: Int,
    val target: SkillTarget,
    val range: Int = 1,
    val damageMultiplier: Float = 0f,
    val healAmount: Int = 0,
    val buff: ActiveBuff? = null,
    val statusToInflict: ActiveBuff? = null,
    val aoeRadius: Int = 0
)

/** All skills organized by class. */
object Skills {

    // === Warrior ===
    val ShieldBash = Skill(
        id = "shield_bash", displayName = "Shield Bash", emoji = "\uD83D\uDEE1\uFE0F",
        description = "Stun an adjacent enemy for 2 turns.",
        cooldown = 5, target = SkillTarget.SingleEnemy, range = 1,
        damageMultiplier = 0.5f,
        statusToInflict = ActiveBuff("Stun", turnsRemaining = 2, statusEffect = StatusEffect.Stun)
    )
    val Cleave = Skill(
        id = "cleave", displayName = "Cleave", emoji = "\u2694\uFE0F",
        description = "Strike all adjacent enemies for 80% ATK damage.",
        cooldown = 4, target = SkillTarget.AllEnemies, range = 1,
        damageMultiplier = 0.8f
    )
    val WarCry = Skill(
        id = "war_cry", displayName = "War Cry", emoji = "\uD83D\uDCA2",
        description = "Boost ATK and DEF by 4 for 5 turns.",
        cooldown = 8, target = SkillTarget.Self,
        buff = ActiveBuff("War Cry", atkBonus = 4, defBonus = 4, turnsRemaining = 5)
    )

    // === Rogue ===
    val Backstab = Skill(
        id = "backstab", displayName = "Backstab", emoji = "\uD83D\uDDE1\uFE0F",
        description = "Deal 200% damage to an adjacent enemy. Always crits.",
        cooldown = 5, target = SkillTarget.SingleEnemy, range = 1,
        damageMultiplier = 2.0f
    )
    val SmokeBomb = Skill(
        id = "smoke_bomb", displayName = "Smoke Bomb", emoji = "\uD83D\uDCA8",
        description = "Boost evasion (+8 SPD) for 4 turns.",
        cooldown = 6, target = SkillTarget.Self,
        buff = ActiveBuff("Smoke Bomb", spdBonus = 8, turnsRemaining = 4)
    )
    val PoisonBlade = Skill(
        id = "poison_blade", displayName = "Poison Blade", emoji = "\uD83E\uDDEA",
        description = "Strike and poison an adjacent enemy (5 dmg/turn, 4 turns).",
        cooldown = 5, target = SkillTarget.SingleEnemy, range = 1,
        damageMultiplier = 0.6f,
        statusToInflict = ActiveBuff("Poison", turnsRemaining = 4, statusEffect = StatusEffect.Poison, dotDamage = 5)
    )

    // === Mage ===
    val Fireball = Skill(
        id = "fireball", displayName = "Fireball", emoji = "\uD83D\uDD25",
        description = "Launch a fireball that damages enemies in a 2-tile radius.",
        cooldown = 4, target = SkillTarget.AreaOfEffect, range = 4,
        damageMultiplier = 1.2f, aoeRadius = 2,
        statusToInflict = ActiveBuff("Burn", turnsRemaining = 2, statusEffect = StatusEffect.Burn, dotDamage = 3)
    )
    val Blink = Skill(
        id = "blink", displayName = "Blink", emoji = "\u2728",
        description = "Teleport to a random safe position nearby.",
        cooldown = 6, target = SkillTarget.Self
    )
    val FrostNova = Skill(
        id = "frost_nova", displayName = "Frost Nova", emoji = "\u2744\uFE0F",
        description = "Freeze all visible enemies for 2 turns.",
        cooldown = 8, target = SkillTarget.AllEnemies, range = 5,
        statusToInflict = ActiveBuff("Stun", turnsRemaining = 2, statusEffect = StatusEffect.Stun)
    )

    // === Cleric ===
    val HolyLight = Skill(
        id = "holy_light", displayName = "Holy Light", emoji = "\u2B50",
        description = "Heal yourself for 30% of max HP.",
        cooldown = 5, target = SkillTarget.Self,
        healAmount = 30 // percentage
    )
    val Smite = Skill(
        id = "smite", displayName = "Smite", emoji = "\u26A1",
        description = "Deal 150% MAG damage to an enemy within range 3.",
        cooldown = 4, target = SkillTarget.SingleEnemy, range = 3,
        damageMultiplier = 1.5f
    )
    val Sanctuary = Skill(
        id = "sanctuary", displayName = "Sanctuary", emoji = "\uD83D\uDEE1\uFE0F",
        description = "Boost DEF by 6 and cure all status effects.",
        cooldown = 7, target = SkillTarget.Self,
        buff = ActiveBuff("Sanctuary", defBonus = 6, turnsRemaining = 5)
    )

    fun forClass(classId: String): List<Skill> = when (classId) {
        "warrior" -> listOf(ShieldBash, Cleave, WarCry)
        "rogue" -> listOf(Backstab, SmokeBomb, PoisonBlade)
        "mage" -> listOf(Fireball, Blink, FrostNova)
        "cleric" -> listOf(HolyLight, Smite, Sanctuary)
        else -> emptyList()
    }
}
