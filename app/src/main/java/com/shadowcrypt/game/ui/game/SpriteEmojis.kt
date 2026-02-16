package com.shadowcrypt.game.ui.game

import com.shadowcrypt.game.model.ItemCategory
import com.shadowcrypt.game.model.RoomEventType
import com.shadowcrypt.game.model.Tile

/** Returns emoji for a tile type, or null for Floor/Wall (keep rect only). */
fun tileEmoji(tile: Tile): String? = when (tile) {
    Tile.Floor -> null
    Tile.Wall -> null
    Tile.Door -> "\uD83D\uDEAA"            // 🚪
    Tile.StairsDown -> "\u2B07\uFE0F"      // ⬇️
    Tile.StairsUp -> "\u2B06\uFE0F"        // ⬆️
    Tile.Trap -> "\u26A0\uFE0F"            // ⚠️
    Tile.Water -> "\uD83C\uDF0A"           // 🌊
    Tile.Lava -> "\uD83D\uDD25"            // 🔥
}

/** Returns emoji for the player based on class ID. */
fun playerEmoji(classId: String): String = when (classId) {
    "warrior" -> "\u2694\uFE0F"             // ⚔️
    "rogue" -> "\uD83D\uDDE1\uFE0F"        // 🗡️
    "mage" -> "\uD83D\uDD2E"               // 🔮
    "cleric" -> "\u271D\uFE0F"             // ✝️
    else -> "\u2694\uFE0F"                  // fallback
}

private val ENEMY_EMOJI_MAP: Map<String, String> = mapOf(
    // Crypt
    "skeleton" to "\uD83D\uDC80",           // 💀
    "zombie" to "\uD83E\uDDDF",             // 🧟
    "ghostly_wisp" to "\uD83D\uDC7B",      // 👻
    "crypt_bat" to "\uD83E\uDD87",          // 🦇
    // Sewers
    "rat_swarm" to "\uD83D\uDC00",          // 🐀
    "sewer_slime" to "\uD83D\uDFE2",        // 🟢
    "toxic_toad" to "\uD83D\uDC38",         // 🐸
    "plague_rat" to "\uD83D\uDC01",         // 🐁
    // Caverns
    "cave_spider" to "\uD83D\uDD77\uFE0F",  // 🕷️
    "stone_golem" to "\uD83E\uDEA8",        // 🪨
    "crystal_sentinel" to "\uD83D\uDC8E",   // 💎
    "troglodyte" to "\uD83D\uDC79",         // 👹
    // Inferno
    "fire_imp" to "\uD83D\uDE08",           // 😈
    "lava_elemental" to "\uD83C\uDF0B",     // 🌋
    "hell_hound" to "\uD83D\uDC15",         // 🐕
    "infernal_priest" to "\uD83E\uDDD9",    // 🧙
    // Void
    "void_wraith" to "\uD83D\uDC64",        // 👤
    "phase_shifter" to "\uD83C\uDF00",      // 🌀
    "entropy_stalker" to "\uD83D\uDD73\uFE0F", // 🕳️
    "abyssal_eye" to "\uD83D\uDC41\uFE0F",  // 👁️
    // Bosses
    "bone_warden" to "\u2620\uFE0F",        // ☠️
    "shadowcrypt_lord" to "\uD83D\uDC7F"    // 👿
)

/** Returns emoji for an enemy by type ID. */
fun enemyEmoji(typeId: String): String =
    ENEMY_EMOJI_MAP[typeId] ?: "\uD83D\uDD34" // fallback: 🔴

/** Returns emoji for a floor item by category. */
fun itemCategoryEmoji(category: ItemCategory): String = when (category) {
    ItemCategory.Weapon -> "\u2694\uFE0F"       // ⚔️
    ItemCategory.Armor -> "\uD83D\uDEE1\uFE0F"  // 🛡️
    ItemCategory.Accessory -> "\uD83D\uDC8D"     // 💍
    ItemCategory.Consumable -> "\uD83E\uDDEA"    // 🧪
}

/** Returns emoji for a room event interactable. */
fun interactableEmoji(type: RoomEventType): String = when (type) {
    RoomEventType.TreasureChest -> "\uD83D\uDCE6"  // 📦
    RoomEventType.Shrine -> "\u26E9\uFE0F"          // ⛩️
    RoomEventType.Fountain -> "\u26F2"              // ⛲
}
