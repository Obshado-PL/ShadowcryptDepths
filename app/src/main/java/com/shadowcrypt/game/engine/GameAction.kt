package com.shadowcrypt.game.engine

import com.shadowcrypt.game.engine.model.Direction

sealed interface GameAction {
    data class Move(val direction: Direction) : GameAction
    data object Wait : GameAction
    data object DescendStairs : GameAction
}
