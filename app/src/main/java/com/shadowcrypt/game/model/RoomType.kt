package com.shadowcrypt.game.model

/** Purpose assigned to a room, affecting enemies, loot, and interactables. */
enum class RoomType(
    val displayName: String,
    val enemyMultiplier: Float,
    val itemDropBoost: Float,
    val guaranteedInteractable: RoomEventType?,
    val trapDensity: Float
) {
    Normal("Room", 1.0f, 1.0f, null, 1.0f),
    TreasureVault("Treasure Vault", 0.5f, 3.0f, RoomEventType.TreasureChest, 0.5f),
    Arena("Arena", 2.0f, 1.5f, null, 0.0f),
    TrapGauntlet("Trap Gauntlet", 0.0f, 2.0f, RoomEventType.TreasureChest, 5.0f),
    ShrineRoom("Shrine Room", 0.0f, 0.5f, RoomEventType.Shrine, 0.0f),
    Library("Library", 0.3f, 2.5f, null, 0.0f),
    Armory("Armory", 0.5f, 4.0f, null, 0.3f)
}
