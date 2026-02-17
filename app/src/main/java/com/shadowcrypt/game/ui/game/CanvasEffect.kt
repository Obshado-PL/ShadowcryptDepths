package com.shadowcrypt.game.ui.game

import androidx.compose.ui.graphics.Color
import com.shadowcrypt.game.model.Position

sealed class CanvasEffect {
    abstract val position: Position
    abstract val createdAtMs: Long
    abstract val durationMs: Long
    abstract val color: Color

    fun isExpired(currentTimeMs: Long): Boolean =
        currentTimeMs - createdAtMs >= durationMs

    fun progress(currentTimeMs: Long): Float =
        ((currentTimeMs - createdAtMs).toFloat() / durationMs).coerceIn(0f, 1f)

    data class DeathEffect(
        override val position: Position,
        override val createdAtMs: Long,
        override val color: Color,
        val enemyTypeId: String,
        val isBoss: Boolean = false,
        override val durationMs: Long = 500L
    ) : CanvasEffect()

    data class SkillEffect(
        override val position: Position,
        override val createdAtMs: Long,
        override val color: Color,
        val effectType: SkillEffectType,
        val radius: Int = 0,
        override val durationMs: Long = 400L
    ) : CanvasEffect()

    data class PickupEffect(
        override val position: Position,
        override val createdAtMs: Long,
        override val color: Color,
        override val durationMs: Long = 500L
    ) : CanvasEffect()
}

enum class SkillEffectType {
    ImpactFlash,
    AreaCircle,
    SelfGlow,
    MultiFlash
}
