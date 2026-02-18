package com.shadowcrypt.game.engine.model

import com.shadowcrypt.game.engine.dungeon.Room

data class DungeonLevel(
    val width: Int,
    val height: Int,
    val tiles: List<List<Tile>>,
    val playerStart: Position,
    val stairsDownPos: Position,
    val stairsUpPos: Position?,
    val floorNumber: Int,
    val rooms: List<Room> = emptyList(),
    val startRoomIndex: Int = 0,
    val endRoomIndex: Int = 0
)
