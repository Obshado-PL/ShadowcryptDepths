package com.shadowcrypt.game.engine

import com.shadowcrypt.game.engine.model.Direction
import com.shadowcrypt.game.engine.model.EquipmentSlot

sealed interface GameAction {
    data class Move(val direction: Direction) : GameAction
    data object Wait : GameAction
    data object DescendStairs : GameAction
    data class UseItem(val itemId: Int) : GameAction
    data class EquipItem(val itemId: Int) : GameAction
    data class UnequipItem(val slot: EquipmentSlot) : GameAction
    data class DropItem(val itemId: Int) : GameAction
}
