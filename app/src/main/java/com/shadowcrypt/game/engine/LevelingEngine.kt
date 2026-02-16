package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.CharacterClass
import com.shadowcrypt.game.model.Player

object LevelingEngine {

    fun xpToNextLevel(currentLevel: Int): Int = 20 + currentLevel * 15

    fun awardXp(player: Player, xpGained: Int): Pair<Player, List<String>> {
        var p = player.copy(xp = player.xp + xpGained)
        val messages = mutableListOf<String>()

        while (p.xp >= p.xpToNext && p.level < 20) {
            val remainingXp = p.xp - p.xpToNext
            val newLevel = p.level + 1
            val charClass = CharacterClass.fromId(p.classId)
            val gains = levelUpGains(charClass)

            p = p.copy(
                level = newLevel,
                xp = remainingXp,
                xpToNext = xpToNextLevel(newLevel),
                maxHp = p.maxHp + gains.hp,
                hp = p.maxHp + gains.hp + p.equipment.totalHpBonus,
                atk = p.atk + gains.atk,
                def = p.def + gains.def,
                mag = p.mag + gains.mag,
                spd = p.spd + gains.spd
            )

            messages.add("LEVEL UP! You are now level $newLevel!")
            messages.add("+${gains.hp}HP +${gains.atk}ATK +${gains.def}DEF +${gains.mag}MAG +${gains.spd}SPD")
        }

        return p to messages
    }

    private fun levelUpGains(charClass: CharacterClass): LevelGains {
        return when (charClass.id) {
            "warrior" -> LevelGains(hp = 4, atk = 2, def = 2, mag = 0, spd = 1)
            "rogue" -> LevelGains(hp = 2, atk = 2, def = 1, mag = 0, spd = 2)
            "mage" -> LevelGains(hp = 2, atk = 0, def = 0, mag = 3, spd = 1)
            "cleric" -> LevelGains(hp = 3, atk = 1, def = 1, mag = 2, spd = 1)
            else -> LevelGains(hp = 3, atk = 1, def = 1, mag = 1, spd = 1)
        }
    }

    private data class LevelGains(
        val hp: Int, val atk: Int, val def: Int, val mag: Int, val spd: Int
    )
}
