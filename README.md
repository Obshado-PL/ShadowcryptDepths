# Shadowcrypt Depths

A turn-based roguelike dungeon crawler for Android, built with Kotlin and Jetpack Compose.

## Game Overview

Descend into the Shadowcrypt — a procedurally generated dungeon filled with monsters, traps, and treasure. Each run is unique with randomized dungeon layouts, enemy placements, and loot drops. Death is permanent, but your legacy lives on through meta-progression unlocks that make future runs stronger.

### Key Features

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

The project follows clean architecture principles with a pure Kotlin game engine that has zero Android dependencies:

```
engine/  → Pure Kotlin game logic (dungeon gen, combat, AI, FOV, pathfinding)
model/   → Immutable data classes (no Android deps)
data/    → Android persistence layer (DataStore repositories)
ui/      → Jetpack Compose screens and components
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

This project is under active development. Current implementation progress:

- [x] Phase 1: Project scaffolding, Gradle setup, theme, navigation
- [ ] Phase 2: Core engine, dungeon generation, player movement, fog of war
- [ ] Phase 3: Combat system, enemies, AI, XP/leveling
- [ ] Phase 4: Items, inventory, loot generation
- [ ] Phase 5: Multiple floors, bosses, class selection, UI polish
- [ ] Phase 6: Meta-progression, audio, haptics, final polish

## Privacy

Shadowcrypt Depths does not collect, store, or transmit any personal data. All game data is stored locally on the device using Android DataStore.

## License

All rights reserved.
