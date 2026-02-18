package com.shadowcrypt.game.engine.model

enum class Tile(val walkable: Boolean, val transparent: Boolean) {
    FLOOR(walkable = true, transparent = true),
    WALL(walkable = false, transparent = false),
    DOOR(walkable = true, transparent = false),
    STAIRS_DOWN(walkable = true, transparent = true),
    STAIRS_UP(walkable = true, transparent = true)
}
