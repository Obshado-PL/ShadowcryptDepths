package com.shadowcrypt.game.model

/** Visual and gameplay theme for a dungeon floor */
enum class FloorTheme(
    val displayName: String,
    val minRoomSize: Int,
    val maxRoomSize: Int,
    val maxRoomCount: Int,
    val gridWidth: Int,
    val gridHeight: Int
) {
    Crypt(
        displayName = "The Crypt",
        minRoomSize = 4, maxRoomSize = 8,
        maxRoomCount = 9,
        gridWidth = 40, gridHeight = 40
    ),
    Sewers(
        displayName = "The Sewers",
        minRoomSize = 3, maxRoomSize = 7,
        maxRoomCount = 11,
        gridWidth = 44, gridHeight = 44
    ),
    Caverns(
        displayName = "The Caverns",
        minRoomSize = 5, maxRoomSize = 12,
        maxRoomCount = 7,
        gridWidth = 48, gridHeight = 48
    ),
    Inferno(
        displayName = "The Inferno",
        minRoomSize = 4, maxRoomSize = 9,
        maxRoomCount = 9,
        gridWidth = 45, gridHeight = 45
    ),
    Void(
        displayName = "The Void",
        minRoomSize = 3, maxRoomSize = 10,
        maxRoomCount = 10,
        gridWidth = 50, gridHeight = 50
    );

    companion object {
        fun forFloor(floor: Int): FloorTheme = when (floor) {
            1, 2 -> Crypt
            3, 4 -> Sewers
            5, 6 -> Caverns
            7, 8 -> Inferno
            9, 10 -> Void
            else -> Crypt
        }
    }
}
