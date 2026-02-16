package com.shadowcrypt.game.ui.game

import androidx.compose.ui.graphics.Color
import com.shadowcrypt.game.model.Position

data class FloatingText(
    val text: String,
    val position: Position,
    val color: Color,
    val createdAtMs: Long
)
