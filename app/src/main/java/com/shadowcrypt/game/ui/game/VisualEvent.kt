package com.shadowcrypt.game.ui.game

import com.shadowcrypt.game.engine.model.Position

sealed interface VisualEvent {
    val id: Long
    val worldPosition: Position

    data class DamageNumber(
        override val id: Long,
        override val worldPosition: Position,
        val amount: Int,
        val type: DamageNumberType
    ) : VisualEvent

    data class ScreenShake(
        override val id: Long,
        override val worldPosition: Position,
        val intensity: Float = 1f
    ) : VisualEvent

    data class DamageFlash(
        override val id: Long,
        override val worldPosition: Position,
        val type: FlashType
    ) : VisualEvent

    data class LootParticle(
        override val id: Long,
        override val worldPosition: Position,
        val colorHex: Long
    ) : VisualEvent
}

enum class DamageNumberType {
    DEALT,      // White — player hits enemy
    RECEIVED,   // Red — player takes damage
    HEAL,       // Green — potion heal
    XP          // Gold — XP gain
}

enum class FlashType {
    DAMAGE,     // Red flash overlay
    KILL        // White flash overlay
}
