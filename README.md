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
├── MainActivity.kt           → Single-activity entry point with sound lifecycle hooks
├── ShadowcryptApp.kt         → Application class + ServiceLocator (4 services + settings sync)
├── data/
│   ├── model/
│   │   ├── GameSettings.kt       → Sound/music/haptics toggle state
│   │   ├── MetaProgress.kt       → Run stats, best floor, unlocked class IDs
│   │   └── UnlockCondition.kt    → 4 unlock conditions with check lambdas
│   ├── SettingsDataStore.kt      → DataStore Preferences wrapper for settings
│   ├── MetaProgressDataStore.kt  → DataStore Preferences wrapper for meta-progress
│   ├── SettingsRepository.kt     → Toggle methods over settings DataStore
│   └── MetaProgressRepository.kt → Run recording, unlock evaluation, atomic writes
├── haptic/
│   ├── HapticEvent.kt            → 8 vibration events with timing/amplitude patterns
│   └── HapticManager.kt          → Vibrator API wrapper (API 31+ and fallback)
├── sound/
│   └── SoundManager.kt           → SoundPool + MediaPlayer framework (silent no-ops, no assets)
├── engine/
│   ├── GameEngine.kt          → Core game loop: state transitions, movement, combat integration
│   ├── GameAction.kt          → Sealed interface for player actions (Move, Wait, Descend, item actions)
│   ├── ai/
│   │   └── EnemyAi.kt         → Enemy turn processing: aggressive chase, patrol wander
│   ├── combat/
│   │   └── CombatEngine.kt    → Damage formula, attack resolution, XP/level-up math
│   ├── dungeon/
│   │   ├── BspNode.kt         → BSP tree node for dungeon space partitioning
│   │   ├── DungeonGenerator.kt→ Procedural dungeon generation (rooms, corridors, doors, stairs)
│   │   ├── EnemySpawner.kt    → Floor-scaled enemy placement per room
│   │   └── Room.kt            → Room data class with geometry helpers
│   ├── fov/
│   │   └── Shadowcaster.kt    → 8-octant recursive shadowcasting for fog of war
│   ├── item/
│   │   ├── ItemGenerator.kt   → Rarity rolling, stat rolling, item creation, enemy drop rolls
│   │   ├── ItemNames.kt       → Thematic name tables per type and rarity tier
│   │   └── ItemSpawner.kt     → Ground item placement in dungeon rooms
│   └── model/                 → Immutable data classes (Position, Direction, Tile, GameState, ItemData, etc.)
└── ui/
    ├── game/
    │   ├── DungeonCanvas.kt   → Canvas-based tile/enemy/player rendering with camera system
    │   ├── DpadOverlay.kt     → D-pad controls, GameHud (HP/XP/ATK/DEF bars), SwipeDetector
    │   ├── GameScreen.kt      → Main game screen with loading, playing, descending, game-over states
    │   ├── GameUiState.kt     → Sealed UI state: Loading, Playing, Descending, GameOver
    │   ├── GameViewModel.kt   → Bridges engine to UI via StateFlow, handles death/inventory transitions
    │   └── InventoryOverlay.kt→ Modal inventory UI: equipment slots, item grid, detail panel
    ├── classselect/
    │   ├── ClassSelectScreen.kt  → Class selection: 4 class cards with lock/unlock gating
    │   └── ClassSelectViewModel.kt → Exposes unlocked class IDs from meta-progression
    ├── settings/
    │   ├── SettingsScreen.kt     → Sound, music, and haptics toggle switches
    │   └── SettingsViewModel.kt  → Settings state + toggle methods via repository
    ├── unlocks/
    │   ├── UnlocksScreen.kt      → Run stats summary + class unlock status cards
    │   └── UnlocksViewModel.kt   → Meta-progress state from repository
    ├── mainmenu/              → Main menu screen (title, new run, unlocks, settings)
    ├── navigation/            → Type-safe navigation routes and screen transitions
    └── theme/                 → Dark dungeon color palette, monospace typography, Material 3 theme
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

| Class   | HP  | ATK | DEF | Unlock Condition   |
|---------|-----|-----|-----|--------------------|
| Warrior | 120 | 8   | 5   | Available at start |
| Rogue   | 80  | 12  | 2   | Complete 5 runs    |
| Mage    | 70  | 14  | 1   | Complete 10 runs   |
| Cleric  | 100 | 7   | 4   | Reach Floor 5      |

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

This project is under active development. All 6 core phases are **complete**.

### Development Phases

- [x] **Phase 1 — Foundation** *(Complete)*
  - Gradle project setup with version catalog
  - Jetpack Compose + Material 3 dark theme (dungeon color palette, monospace typography)
  - Type-safe navigation with animated screen transitions
  - Main menu screen (New Run, Unlocks, Settings)
  - ServiceLocator scaffolding for future services
  - Edge-to-edge display, release signing config
- [x] **Phase 2 — Core Engine** *(Complete)*
  - BSP dungeon generation (rooms, L-shaped corridors, doors, stairs)
  - Grid-based player movement with swipe and d-pad controls
  - Fog of war via 8-octant recursive shadowcasting (radius 8)
  - Canvas-based tile rendering with player-centered camera (15-tile viewport)
  - 5 floor themes with distinct color palettes (Crypt, Sewers, Caverns, Inferno, Void)
  - Floor descent with transition overlay
- [x] **Phase 3 — Combat & Enemies** *(Complete)*
  - Bump-to-attack combat with `max(1, atk - def/2)` damage formula
  - 10 enemy types across 5 floor themes with floor-scaled stats
  - Enemy AI: aggressive (greedy Manhattan chase) and patrol (random wander)
  - XP/leveling system (+5 HP, +1 ATK, +1 DEF per level, full heal on level-up)
  - 4 class-specific base stat profiles (Warrior, Rogue, Mage, Cleric)
  - Game over screen with run statistics (floor reached, enemies slain, level, turns)
  - Enemy rendering as colored diamonds (red normal, purple bosses)
- [x] **Phase 4 — Items & Inventory** *(Complete)*
  - 5 item types (weapon, armor, accessory, potion, scroll) across 5 rarity tiers
  - Floor-scaled loot generation with thematic names (dark fantasy)
  - Equipment system: 3 slots (weapon, armor, accessory) with stat bonuses affecting combat
  - Consumables: health potions (heal 15-999 HP) and damage scrolls (AoE, 2-5 tile radius)
  - Auto-pickup on walk with inventory full warning
  - Enemy item drops (35% base, higher for bosses)
  - Inventory overlay UI: equipment slots, 4x4 item grid, detail panel, equip/use/drop actions
  - Ground item rendering as rarity-colored squares on dungeon canvas
  - ATK/DEF effective stats displayed in HUD
- [x] **Phase 5 — Floors & Bosses** *(Complete)*
  - Boss encounters: Bone Warden (floor 5) and Shadowcrypt Lord (floor 10) with guaranteed Legendary drops
  - Boss rendering: larger diamond with inner white glow, boss-specific AI detection ranges
  - Floor 10 cap: no stairs down, victory condition on final boss kill
  - Victory screen ("VICTORY" in gold) vs defeat screen ("YOU HAVE FALLEN" in red)
  - Class selection screen: 4 class cards with HP/ATK/DEF stats and descriptions
  - Halved regular enemy count on boss floors, end room reserved for boss
- [x] **Phase 6 — Polish & Meta-Progression** *(Complete)*
  - DataStore Preferences persistence for settings (3 toggles) and meta-progress (run stats, unlocks)
  - Meta-progression: 4 unlock conditions evaluated atomically on run completion, newly unlocked classes displayed on game over
  - Class select lock/unlock gating: locked classes grayed out with condition text, unlocked classes selectable
  - Settings screen: sound effects, background music, and haptic feedback toggles with live sync to managers
  - Unlocks screen: run stats summary + 4 class cards with LOCKED/UNLOCKED status
  - Haptic feedback: 8 event-specific vibration patterns triggered via ViewModel state-diffing
  - Sound infrastructure: SoundPool (SFX) + MediaPlayer (BGM) framework with lifecycle hooks (silent no-ops until assets added)

## Privacy

Shadowcrypt Depths does not collect, store, or transmit any personal data. All game data is stored locally on the device using Android DataStore.

## License

All rights reserved.
