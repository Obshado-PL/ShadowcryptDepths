# Shadowcrypt Depths

A turn-based roguelike dungeon crawler for Android, built with Kotlin and Jetpack Compose.

## Game Overview

Descend into the Shadowcrypt — a procedurally generated dungeon filled with monsters, traps, and treasure. Each run is unique with randomized dungeon layouts, enemy placements, and loot drops. Death is permanent, but your legacy lives on through meta-progression unlocks that make future runs stronger.

### Planned Features

- **Procedurally Generated Dungeons** — Every run creates a unique dungeon using BSP (Binary Space Partitioning) room-and-corridor generation
- **Turn-Based Tactical Combat** — Move and attack on a grid. Every action matters when enemies close in from all sides
- **10 Dungeon Floors** across 5 themes: Crypt, Sewers, Caverns, Inferno, and Void
- **4 Character Classes** — Warrior, Rogue, Mage, and Cleric, each with unique abilities and playstyles
- **Permadeath with Meta-Progression** — Death resets your run, but permanent unlocks carry over (new classes, stat bonuses)
- **Full Loot System** — Weapons, armor, potions, scrolls, and accessories across 5 rarity tiers (Common to Legendary)
- **20+ Enemy Types** with distinct AI behaviors (aggressive, patrol, ranged, ambush, support, boss)
- **Boss Encounters** — Mini-boss on Floor 5 and Final Boss on Floor 10 with multi-phase mechanics
- **Fog of War** — Explore the unknown with recursive shadowcasting field-of-view
- **Pixel Art Style** — Retro 16-bit inspired visuals rendered via Compose Canvas

## Architecture

The project follows clean architecture principles with a pure Kotlin game engine separated from the Android UI layer.

### Current Structure

```
app/src/main/java/com/shadowcrypt/game/
├── MainActivity.kt       → Single-activity entry point with edge-to-edge display
├── ShadowcryptApp.kt     → Application class + ServiceLocator for shared services
└── ui/
    ├── mainmenu/          → Main menu screen (title, new run, unlocks, settings)
    ├── navigation/        → Type-safe navigation routes and screen transitions
    └── theme/             → Dark dungeon color palette, monospace typography, Material 3 theme
```

### Planned Structure (upcoming phases)

```
engine/  → Pure Kotlin game logic (dungeon gen, combat, AI, FOV, pathfinding)
model/   → Immutable data classes (no Android deps)
data/    → Android persistence layer (DataStore repositories)
audio/   → Sound management (SoundPool + MediaPlayer)
haptic/  → Vibration feedback
```

### Design Patterns

- **MVVM** — ViewModels bridge the pure engine to Compose UI via StateFlow
- **ServiceLocator** — Simple dependency injection without framework overhead
- **Canvas Rendering** — Dungeon tiles drawn as colored shapes on Compose Canvas
- **Data-Driven Design** — All game content (enemies, items, abilities) defined as type-safe Kotlin data classes

## Tech Stack

- **Language:** Kotlin 2.1
- **UI Framework:** Jetpack Compose with Material 3
- **Navigation:** Navigation Compose (type-safe routes with @Serializable)
- **Persistence:** DataStore Preferences
- **Serialization:** Kotlin Serialization
- **Architecture:** MVVM + StateFlow + Coroutines
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36

## Building

### Prerequisites

- Android SDK installed (API 36)
- Java 11+ (project uses JDK 11 target)

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## Game Design

### Character Classes

| Class   | HP | ATK | DEF | MAG | SPD | Unlock Condition  |
|---------|-----|-----|-----|-----|-----|-------------------|
| Warrior | 60  | 10  | 8   | 2   | 8   | Available at start |
| Rogue   | 40  | 8   | 4   | 3   | 14  | Complete 5 runs    |
| Mage    | 35  | 3   | 3   | 12  | 9   | Complete 10 runs   |
| Cleric  | 50  | 6   | 6   | 8   | 7   | Reach Floor 5      |

### Dungeon Themes

| Floors | Theme    | Key Feature                              |
|--------|----------|------------------------------------------|
| 1-2    | Crypt    | Tutorial enemies, basic loot             |
| 3-4    | Sewers   | Water tiles slow movement, poison enemies |
| 5      | Boss     | Bone Warden mini-boss encounter          |
| 5-6    | Caverns  | Wide rooms, spider and golem enemies     |
| 7-8    | Inferno  | Lava tiles deal damage, fire enemies     |
| 9      | Void     | Teleport traps, reality-warped enemies   |
| 10     | Final    | The Shadowcrypt Lord boss fight          |

## Project Status

This project is under active development. Currently preparing for **Phase 2**.

### Development Phases

- [x] **Phase 1 — Foundation** *(Complete)*
  - Gradle project setup with version catalog
  - Jetpack Compose + Material 3 dark theme (dungeon color palette, monospace typography)
  - Type-safe navigation with animated screen transitions
  - Main menu screen (New Run, Unlocks, Settings)
  - ServiceLocator scaffolding for future services
  - Edge-to-edge display, release signing config
- [ ] **Phase 2 — Core Engine**
  - Dungeon generation (BSP room-and-corridor algorithm)
  - Grid-based player movement
  - Fog of war (recursive shadowcasting)
  - Canvas-based tile rendering
- [ ] **Phase 3 — Combat & Enemies**
  - Turn-based combat system
  - Enemy types and AI behaviors
  - XP and leveling
- [ ] **Phase 4 — Items & Inventory**
  - Loot generation with rarity tiers
  - Equipment and consumables
  - Inventory management screen
- [ ] **Phase 5 — Floors & Bosses**
  - Multi-floor dungeon progression (10 floors, 5 themes)
  - Boss encounters (Floor 5 mini-boss, Floor 10 final boss)
  - Class selection screen (4 classes)
  - UI polish and game-over screen
- [ ] **Phase 6 — Polish & Meta-Progression**
  - Permanent unlock system
  - Sound effects and background music
  - Haptic feedback
  - Settings screen (audio, accessibility)
  - Final balancing and polish

## Privacy

Shadowcrypt Depths does not collect, store, or transmit any personal data. All game data is stored locally on the device using Android DataStore.

## License

All rights reserved.
