package com.shadowcrypt.game.model

/** Immutable player character state */
data class Player(
    val position: Position,
    val classId: String,
    val level: Int = 1,
    val xp: Int = 0,
    val xpToNext: Int = 20,
    val hp: Int,
    val maxHp: Int,
    val atk: Int,
    val def: Int,
    val mag: Int,
    val spd: Int,
    val currentFloor: Int = 1,
    val inventory: Inventory = Inventory(),
    val equipment: Equipment = Equipment(),
    val activeBuffs: List<ActiveBuff> = emptyList()
) {
    val isAlive: Boolean get() = hp > 0
    val hpFraction: Float get() = hp.toFloat() / effectiveMaxHp.toFloat()
    val xpFraction: Float get() = if (xpToNext > 0) xp.toFloat() / xpToNext.toFloat() else 0f

    val effectiveAtk: Int get() = atk + equipment.totalAtk + activeBuffs.sumOf { it.atkBonus }
    val effectiveDef: Int get() = def + equipment.totalDef + activeBuffs.sumOf { it.defBonus }
    val effectiveMag: Int get() = mag + equipment.totalMag + activeBuffs.sumOf { it.magBonus }
    val effectiveSpd: Int get() = spd + equipment.totalSpd + activeBuffs.sumOf { it.spdBonus }
    val effectiveMaxHp: Int get() = maxHp + equipment.totalHpBonus

    companion object {
        fun create(characterClass: CharacterClass): Player = Player(
            position = Position(0, 0),
            classId = characterClass.id,
            hp = characterClass.baseHp,
            maxHp = characterClass.baseHp,
            atk = characterClass.baseAtk,
            def = characterClass.baseDef,
            mag = characterClass.baseMag,
            spd = characterClass.baseSpd
        )
    }
}
