package com.shadowcrypt.game.haptic

enum class HapticEvent(
    val timings: LongArray,
    val amplitudes: IntArray
) {
    PLAYER_ATTACK(
        timings = longArrayOf(0, 30),
        amplitudes = intArrayOf(0, 120)
    ),
    PLAYER_DAMAGE(
        timings = longArrayOf(0, 50, 30, 50),
        amplitudes = intArrayOf(0, 180, 0, 100)
    ),
    LEVEL_UP(
        timings = longArrayOf(0, 40, 60, 40, 60, 80),
        amplitudes = intArrayOf(0, 100, 0, 150, 0, 200)
    ),
    BOSS_ENCOUNTER(
        timings = longArrayOf(0, 100, 50, 100, 50, 200),
        amplitudes = intArrayOf(0, 200, 0, 200, 0, 255)
    ),
    ITEM_PICKUP(
        timings = longArrayOf(0, 20),
        amplitudes = intArrayOf(0, 80)
    ),
    DESCEND(
        timings = longArrayOf(0, 60, 40, 80),
        amplitudes = intArrayOf(0, 150, 0, 200)
    ),
    PLAYER_DEATH(
        timings = longArrayOf(0, 100, 50, 100, 50, 100, 50, 200),
        amplitudes = intArrayOf(0, 255, 0, 200, 0, 150, 0, 100)
    ),
    VICTORY(
        timings = longArrayOf(0, 50, 50, 50, 50, 50, 50, 150),
        amplitudes = intArrayOf(0, 100, 0, 150, 0, 200, 0, 255)
    )
}
