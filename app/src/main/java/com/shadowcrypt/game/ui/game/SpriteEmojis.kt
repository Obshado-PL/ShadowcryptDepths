package com.shadowcrypt.game.ui.game

import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.model.ItemCategory
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.RoomEventType
import com.shadowcrypt.game.model.RoomType
import com.shadowcrypt.game.model.Tile

/** Returns emoji for a tile type, or null for Floor/Wall (handled separately). */
fun tileEmoji(tile: Tile): String? = when (tile) {
    Tile.Floor -> null
    Tile.Wall -> null
    Tile.Door -> "\uD83D\uDEAA"            // 🚪
    Tile.StairsDown -> "\u2B07\uFE0F"      // ⬇️
    Tile.StairsUp -> "\u2B06\uFE0F"        // ⬆️
    Tile.Trap -> "\u26A0\uFE0F"            // ⚠️
    Tile.Water -> "\uD83C\uDF0A"           // 🌊
    Tile.Lava -> "\uD83D\uDD25"            // 🔥
    Tile.Pillar -> "\uD83E\uDDF1"          // 🧱
}

/** Returns emoji for wall tiles based on floor theme. */
fun wallEmoji(theme: FloorTheme): String = when (theme) {
    FloorTheme.Crypt -> "\uD83E\uDDF1"     // 🧱
    FloorTheme.Sewers -> "\uD83D\uDFEB"    // 🟫
    FloorTheme.Caverns -> "\uD83E\uDEA8"   // 🪨
    FloorTheme.Inferno -> "\uD83D\uDFE5"   // 🟥
    FloorTheme.Void -> "\uD83D\uDFEA"      // 🟪
}

/**
 * Returns a decoration emoji for a floor tile, or null if no decoration.
 * Uses position hash for deterministic sparse placement (~15% of tiles).
 */
fun floorDecoration(pos: Position, roomType: RoomType, theme: FloorTheme): String? {
    // Deterministic hash to decide if this tile gets a decoration
    val hash = (pos.x * 7919 + pos.y * 6271) and 0x7FFFFFFF
    val chance = hash % 100

    // Room type decorations (placed more densely ~20%)
    if (roomType != RoomType.Normal) {
        if (chance < 20) {
            return roomTypeDecoration(roomType, hash)
        }
        return null
    }

    // Theme floor decorations (sparse ~12%)
    if (chance < 12) {
        return themeFloorDecoration(theme, hash)
    }
    return null
}

private fun roomTypeDecoration(type: RoomType, hash: Int): String = when (type) {
    RoomType.TreasureVault -> when (hash % 4) {
        0 -> "\uD83D\uDCB0"    // 💰
        1 -> "\u2728"           // ✨
        2 -> "\uD83D\uDC8E"    // 💎
        else -> "\uD83E\uDE99" // 🪙
    }
    RoomType.Arena -> when (hash % 3) {
        0 -> "\uD83D\uDDE1\uFE0F" // 🗡️
        1 -> "\uD83D\uDEE1\uFE0F" // 🛡️
        else -> "\uD83C\uDFF4"     // 🏴
    }
    RoomType.TrapGauntlet -> when (hash % 2) {
        0 -> "\u2620\uFE0F"    // ☠️
        else -> "\u26D4"        // ⛔
    }
    RoomType.ShrineRoom -> when (hash % 3) {
        0 -> "\uD83D\uDD6F\uFE0F" // 🕯️
        1 -> "\u2728"              // ✨
        else -> "\uD83C\uDF1F"     // 🌟
    }
    RoomType.Library -> when (hash % 4) {
        0 -> "\uD83D\uDCDA"    // 📚
        1 -> "\uD83D\uDCDC"    // 📜
        2 -> "\uD83D\uDCD6"    // 📖
        else -> "\uD83D\uDD6F\uFE0F" // 🕯️
    }
    RoomType.Armory -> when (hash % 4) {
        0 -> "\u2694\uFE0F"    // ⚔️
        1 -> "\uD83D\uDEE1\uFE0F" // 🛡️
        2 -> "\u2692\uFE0F"    // ⚒️
        else -> "\uD83E\uDDF2" // 🧲
    }
    RoomType.Normal -> "\u00B7" // fallback (shouldn't reach here)
}

private fun themeFloorDecoration(theme: FloorTheme, hash: Int): String = when (theme) {
    FloorTheme.Crypt -> when (hash % 5) {
        0 -> "\uD83D\uDD6F\uFE0F" // 🕯️
        1 -> "\uD83E\uDDB4"       // 🦴
        2 -> "\uD83D\uDD78\uFE0F" // 🕸️
        3 -> "\u26B0\uFE0F"       // ⚰️
        else -> "\uD83E\uDEA6"    // 🪦
    }
    FloorTheme.Sewers -> when (hash % 4) {
        0 -> "\uD83D\uDCA7"    // 💧
        1 -> "\uD83E\uDDA0"    // 🦠
        2 -> "\uD83C\uDF43"    // 🍃
        else -> "\uD83E\uDEAB"  // 🪫 (dripping pipe)
    }
    FloorTheme.Caverns -> when (hash % 4) {
        0 -> "\uD83E\uDEA8"    // 🪨
        1 -> "\uD83D\uDC8E"    // 💎
        2 -> "\uD83C\uDF44"    // 🍄
        else -> "\u2B50"        // ⭐ (crystal sparkle)
    }
    FloorTheme.Inferno -> when (hash % 4) {
        0 -> "\uD83D\uDD25"    // 🔥
        1 -> "\uD83C\uDF2B\uFE0F" // 🌫️ (smoke)
        2 -> "\u2668\uFE0F"    // ♨️
        else -> "\uD83E\uDEA8" // 🪨 (charred rock)
    }
    FloorTheme.Void -> when (hash % 4) {
        0 -> "\u2728"           // ✨
        1 -> "\uD83C\uDF00"    // 🌀
        2 -> "\uD83D\uDD73\uFE0F" // 🕳️
        else -> "\u269B\uFE0F" // ⚛️
    }
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
    "stone_golem" to "\uD83D\uDDFF",        // 🗿
    "crystal_sentinel" to "\uD83D\uDD37",   // 🔷
    "troglodyte" to "\uD83D\uDC79",         // 👹
    // Inferno
    "fire_imp" to "\uD83D\uDE08",           // 😈
    "lava_elemental" to "\uD83C\uDF0B",     // 🌋
    "hell_hound" to "\uD83D\uDC15",         // 🐕
    "infernal_priest" to "\uD83E\uDDD9",    // 🧙
    // Void
    "void_wraith" to "\uD83D\uDC64",        // 👤
    "phase_shifter" to "\uD83D\uDC7E",      // 👾
    "entropy_stalker" to "\uD83C\uDF11",     // 🌑
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
