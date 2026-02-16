package com.shadowcrypt.game.ui.theme

import androidx.compose.ui.graphics.Color

// ===== App Theme Colors =====

// Primary brand colors (deep dungeon purple palette)
val DungeonPurple80 = Color(0xFFCBB8FF)
val DungeonGrey80 = Color(0xFFC2C0D4)
val DungeonAmber80 = Color(0xFFFFD699)

// ===== Dungeon Tile Colors =====

// Backgrounds
val GameBackground = Color(0xFF0D0B1A)       // Deep void black
val DungeonSurface = Color(0xFF1A1825)       // Dark dungeon surface

// Floor tiles (alternating for checkerboard pattern)
val FloorDark = Color(0xFF2A2A3A)            // Dark stone floor
val FloorLight = Color(0xFF343445)           // Light stone floor

// Wall tiles
val WallDark = Color(0xFF1A1A2A)             // Deep wall shadow
val WallAccent = Color(0xFF4A3A2A)           // Warm stone accent

// Special tiles
val DoorColor = Color(0xFF8B6914)            // Wooden door brown
val StairsColor = Color(0xFFFFD700)          // Golden stairs
val TrapColor = Color(0xFFCC3333)            // Danger red trap
val WaterColor = Color(0xFF2244AA)           // Deep water blue
val LavaColor = Color(0xFFFF4400)            // Molten lava orange

// ===== Entity Colors =====

val PlayerColor = Color(0xFF4488FF)          // Hero blue
val EnemyColor = Color(0xFFFF4444)           // Enemy red
val BossColor = Color(0xFFAA00FF)            // Boss purple
val ItemColor = Color(0xFFFFAA00)            // Item gold

// ===== Item Rarity Colors =====

val RarityCommon = Color(0xFFAAAAAA)         // Gray
val RarityUncommon = Color(0xFF44DD44)       // Green
val RarityRare = Color(0xFF4488FF)           // Blue
val RarityEpic = Color(0xFFAA44FF)           // Purple
val RarityLegendary = Color(0xFFFFAA00)      // Gold

// ===== Fog of War =====

val FogUnexplored = Color(0xFF000000)        // Solid black (never seen)
val FogExplored = Color(0x99000000)          // Semi-transparent (previously seen)

// ===== UI Colors =====

val HealthRed = Color(0xFFFF4444)            // HP bar color
val HealthRedDark = Color(0xFF661111)        // HP bar background
val XpGold = Color(0xFFFFD700)              // XP bar color
val XpGoldDark = Color(0xFF4A3A00)          // XP bar background
val TextPrimary = Color(0xFFFFFFFF)          // Main text white
val TextSecondary = Color(0xFFAAAAAA)        // Secondary text gray
val HudBackground = Color(0xCC000000)        // Semi-transparent HUD overlay

// ===== Floor Theme Colors =====

// Crypt (floors 1-2): gray stone, torch light
val CryptFloor = Color(0xFF2A2A3A)
val CryptWall = Color(0xFF3A3A4A)
val CryptAccent = Color(0xFFFF8844)          // Torch orange

// Sewers (floors 3-4): green-gray, damp
val SewerFloor = Color(0xFF2A3A2A)
val SewerWall = Color(0xFF3A4A3A)
val SewerAccent = Color(0xFF44AA44)          // Sewer green

// Caverns (floors 5-6): brown stone, crystal
val CavernFloor = Color(0xFF3A2A1A)
val CavernWall = Color(0xFF4A3A2A)
val CavernAccent = Color(0xFF88DDFF)         // Crystal blue

// Inferno (floors 7-8): red-orange, lava
val InfernoFloor = Color(0xFF3A1A1A)
val InfernoWall = Color(0xFF4A2A1A)
val InfernoAccent = Color(0xFFFF4400)        // Lava orange

// Void (floors 9-10): purple-black, shifting
val VoidFloor = Color(0xFF1A1A3A)
val VoidWall = Color(0xFF2A1A4A)
val VoidAccent = Color(0xFFAA44FF)           // Void purple
