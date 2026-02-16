package com.shadowcrypt.game.data

import android.content.Context
import com.shadowcrypt.game.engine.DungeonGenerator
import com.shadowcrypt.game.engine.FovEngine
import com.shadowcrypt.game.model.ActiveBuff
import com.shadowcrypt.game.model.AiBehavior
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.Equipment
import com.shadowcrypt.game.model.EquipSlot
import com.shadowcrypt.game.model.FloorItem
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.GameStatus
import com.shadowcrypt.game.model.Interactable
import com.shadowcrypt.game.model.Inventory
import com.shadowcrypt.game.model.Item
import com.shadowcrypt.game.model.ItemCategory
import com.shadowcrypt.game.model.ItemEffect
import com.shadowcrypt.game.model.Player
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Quest
import com.shadowcrypt.game.model.QuestType
import com.shadowcrypt.game.model.Rarity
import com.shadowcrypt.game.model.RoomEventType
import com.shadowcrypt.game.model.StatusEffect
import com.shadowcrypt.game.model.Tile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

object GameSaveManager {

    private const val SAVE_FILE = "game_save.json"

    fun hasSave(context: Context): Boolean {
        return File(context.filesDir, SAVE_FILE).exists()
    }

    fun save(context: Context, state: GameState) {
        val data = toSaveData(state)
        val jsonStr = json.encodeToString(data)
        File(context.filesDir, SAVE_FILE).writeText(jsonStr)
    }

    fun load(context: Context): GameState? {
        val file = File(context.filesDir, SAVE_FILE)
        if (!file.exists()) return null
        return try {
            val data = json.decodeFromString<SaveData>(file.readText())
            fromSaveData(data)
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    fun deleteSave(context: Context) {
        File(context.filesDir, SAVE_FILE).delete()
    }

    // === Conversion ===

    private fun toSaveData(state: GameState): SaveData {
        val triggeredTraps = mutableListOf<SavePos>()
        for (row in 0 until state.dungeon.height) {
            for (col in 0 until state.dungeon.width) {
                // Original trap positions that are now Floor (trap was triggered)
                // We track this by checking if a floor tile is at a position not in the original gen
            }
        }
        // Since we can't know original vs generated, store all modified trap positions
        // Just regenerate and diff on load

        return SaveData(
            seed = state.seed,
            classId = state.player.classId,
            currentFloor = state.player.currentFloor,
            turnCount = state.turnCount,
            enemiesKilled = state.enemiesKilled,
            status = state.status.name,
            messageLog = state.messageLog.takeLast(20),
            lastTrapTriggered = state.lastTrapTriggered,
            lastCritical = state.lastCritical,
            player = toSavePlayer(state.player),
            enemies = state.enemies.map { toSaveEnemy(it) },
            floorItems = state.floorItems.map { toSaveFloorItem(it) },
            interactableUsed = state.interactables.map { it.used },
            gridOverrides = buildGridOverrides(state),
            difficulty = state.difficulty.name,
            quests = state.quests.map { SaveQuest(it.id, it.type.name, it.description, it.targetCount, it.progress, it.rewardXp, it.completed) }
        )
    }

    private fun buildGridOverrides(state: GameState): List<SaveGridOverride> {
        // Compare current grid with freshly generated to find mutations (triggered traps)
        val generator = DungeonGenerator()
        val fresh = generator.generate(state.player.currentFloor, state.seed)
        val overrides = mutableListOf<SaveGridOverride>()
        for (row in 0 until state.dungeon.height) {
            for (col in 0 until state.dungeon.width) {
                if (state.dungeon.grid[row][col] != fresh.grid[row][col]) {
                    overrides.add(SaveGridOverride(col, row, state.dungeon.grid[row][col].name))
                }
            }
        }
        return overrides
    }

    private fun fromSaveData(data: SaveData): GameState {
        val generator = DungeonGenerator()
        val fovEngine = FovEngine(viewRadius = 16)
        val dungeon = generator.generate(data.currentFloor, data.seed)

        // Apply grid overrides (triggered traps)
        for (override in data.gridOverrides) {
            val tile = Tile.entries.find { it.name == override.tile } ?: continue
            dungeon.grid[override.y][override.x] = tile
        }

        val player = fromSavePlayer(data.player, data.classId, data.currentFloor)
        val enemies = data.enemies.map { fromSaveEnemy(it) }
        val floorItems = data.floorItems.map { fromSaveFloorItem(it) }

        val interactables = generateInteractablesFromSave(
            dungeon.rooms, dungeon.playerStart, data.seed, data.currentFloor,
            enemies, floorItems, data.interactableUsed
        )

        val visible = fovEngine.computeVisible(player.position, dungeon)
        val visibilityMap = fovEngine.updateVisibilityMap(emptyMap(), visible)

        return GameState(
            dungeon = dungeon,
            player = player,
            enemies = enemies,
            enemiesKilled = data.enemiesKilled,
            visibilityMap = visibilityMap,
            turnCount = data.turnCount,
            status = GameStatus.entries.find { it.name == data.status } ?: GameStatus.Playing,
            messageLog = data.messageLog,
            seed = data.seed,
            floorItems = floorItems,
            interactables = interactables,
            lastTrapTriggered = data.lastTrapTriggered,
            lastCritical = data.lastCritical,
            difficulty = Difficulty.entries.find { it.name == data.difficulty } ?: Difficulty.Normal,
            quests = data.quests.map { sq ->
                Quest(sq.id, QuestType.entries.find { it.name == sq.type } ?: QuestType.KillEnemies,
                    sq.description, sq.targetCount, sq.progress, sq.rewardXp, sq.completed)
            }
        )
    }

    private fun generateInteractablesFromSave(
        rooms: List<com.shadowcrypt.game.model.Room>,
        playerStart: Position,
        seed: Long,
        floor: Int,
        enemies: List<Enemy>,
        floorItems: List<FloorItem>,
        usedFlags: List<Boolean>
    ): List<Interactable> {
        val occupied = enemies.map { it.position }.toSet() + floorItems.map { it.position }.toSet()
        val random = kotlin.random.Random(seed + floor * 4000L)
        val types = RoomEventType.entries.toTypedArray()
        val result = mutableListOf<Interactable>()

        for (room in rooms) {
            if (room.center == playerStart) continue
            if (random.nextInt(100) >= 30) continue
            val pos = room.center
            if (pos in occupied) continue
            val type = types[random.nextInt(types.size)]
            val used = result.size < usedFlags.size && usedFlags[result.size]
            result.add(Interactable(pos, type, used))
        }
        return result
    }

    // === Player ===

    private fun toSavePlayer(p: Player): SavePlayer = SavePlayer(
        x = p.position.x, y = p.position.y,
        level = p.level, xp = p.xp, xpToNext = p.xpToNext,
        hp = p.hp, maxHp = p.maxHp,
        atk = p.atk, def = p.def, mag = p.mag, spd = p.spd,
        inventoryItems = p.inventory.items.map { toSaveItem(it) },
        inventoryCapacity = p.inventory.capacity,
        weapon = p.equipment.weapon?.let { toSaveItem(it) },
        armor = p.equipment.armor?.let { toSaveItem(it) },
        accessory = p.equipment.accessory?.let { toSaveItem(it) },
        buffs = p.activeBuffs.map { toSaveBuff(it) },
        skillCooldowns = p.skillCooldowns,
        torchFuel = p.torchFuel,
        maxTorchFuel = p.maxTorchFuel,
        hunger = p.hunger,
        maxHunger = p.maxHunger
    )

    private fun fromSavePlayer(sp: SavePlayer, classId: String, floor: Int): Player = Player(
        position = Position(sp.x, sp.y),
        classId = classId,
        level = sp.level, xp = sp.xp, xpToNext = sp.xpToNext,
        hp = sp.hp, maxHp = sp.maxHp,
        atk = sp.atk, def = sp.def, mag = sp.mag, spd = sp.spd,
        currentFloor = floor,
        inventory = Inventory(sp.inventoryItems.map { fromSaveItem(it) }, sp.inventoryCapacity),
        equipment = Equipment(
            weapon = sp.weapon?.let { fromSaveItem(it) },
            armor = sp.armor?.let { fromSaveItem(it) },
            accessory = sp.accessory?.let { fromSaveItem(it) }
        ),
        activeBuffs = sp.buffs.map { fromSaveBuff(it) },
        skillCooldowns = sp.skillCooldowns,
        torchFuel = sp.torchFuel,
        maxTorchFuel = sp.maxTorchFuel,
        hunger = sp.hunger,
        maxHunger = sp.maxHunger
    )

    // === Enemy ===

    private fun toSaveEnemy(e: Enemy): SaveEnemy = SaveEnemy(
        id = e.id, typeId = e.typeId,
        x = e.position.x, y = e.position.y,
        hp = e.hp, maxHp = e.maxHp,
        atk = e.atk, def = e.def, mag = e.mag, spd = e.spd,
        xpReward = e.xpReward,
        behavior = e.behavior.name,
        alertedByPlayer = e.alertedByPlayer,
        displayName = e.displayName,
        isBoss = e.isBoss, bossPhase = e.bossPhase,
        buffs = e.activeBuffs.map { toSaveBuff(it) },
        isElite = e.isElite
    )

    private fun fromSaveEnemy(se: SaveEnemy): Enemy = Enemy(
        id = se.id, typeId = se.typeId,
        position = Position(se.x, se.y),
        hp = se.hp, maxHp = se.maxHp,
        atk = se.atk, def = se.def, mag = se.mag, spd = se.spd,
        xpReward = se.xpReward,
        behavior = AiBehavior.entries.find { it.name == se.behavior } ?: AiBehavior.Aggressive,
        alertedByPlayer = se.alertedByPlayer,
        displayName = se.displayName,
        isBoss = se.isBoss, bossPhase = se.bossPhase,
        activeBuffs = se.buffs.map { fromSaveBuff(it) },
        isElite = se.isElite
    )

    // === Item ===

    private fun toSaveItem(item: Item): SaveItem = SaveItem(
        id = item.id, templateId = item.templateId,
        displayName = item.displayName,
        category = item.category.name,
        equipSlot = item.equipSlot?.name,
        rarity = item.rarity.name,
        atk = item.atk, def = item.def, mag = item.mag, spd = item.spd,
        hpBonus = item.hpBonus,
        effectType = item.effect?.let { encodeEffect(it) },
        description = item.description
    )

    private fun fromSaveItem(si: SaveItem): Item = Item(
        id = si.id, templateId = si.templateId,
        displayName = si.displayName,
        category = ItemCategory.entries.find { it.name == si.category } ?: ItemCategory.Consumable,
        equipSlot = si.equipSlot?.let { name -> EquipSlot.entries.find { it.name == name } },
        rarity = Rarity.entries.find { it.name == si.rarity } ?: Rarity.Common,
        atk = si.atk, def = si.def, mag = si.mag, spd = si.spd,
        hpBonus = si.hpBonus,
        effect = si.effectType?.let { decodeEffect(it) },
        description = si.description
    )

    private fun encodeEffect(effect: ItemEffect): String = when (effect) {
        is ItemEffect.Heal -> "heal:${effect.amount}"
        is ItemEffect.BuffAtk -> "buffAtk:${effect.amount}:${effect.turns}"
        is ItemEffect.BuffDef -> "buffDef:${effect.amount}:${effect.turns}"
        is ItemEffect.BuffMag -> "buffMag:${effect.amount}:${effect.turns}"
        is ItemEffect.BuffSpd -> "buffSpd:${effect.amount}:${effect.turns}"
        is ItemEffect.Teleport -> "teleport"
        is ItemEffect.RevealMap -> "revealMap"
        is ItemEffect.FreezeEnemies -> "freeze:${effect.turns}"
        is ItemEffect.CureStatus -> "cureStatus"
        is ItemEffect.RestoreHunger -> "restoreHunger:${effect.amount}"
        is ItemEffect.RestoreTorch -> "restoreTorch:${effect.amount}"
    }

    private fun decodeEffect(str: String): ItemEffect? {
        val parts = str.split(":")
        return when (parts[0]) {
            "heal" -> ItemEffect.Heal(parts[1].toInt())
            "buffAtk" -> ItemEffect.BuffAtk(parts[1].toInt(), parts[2].toInt())
            "buffDef" -> ItemEffect.BuffDef(parts[1].toInt(), parts[2].toInt())
            "buffMag" -> ItemEffect.BuffMag(parts[1].toInt(), parts[2].toInt())
            "buffSpd" -> ItemEffect.BuffSpd(parts[1].toInt(), parts[2].toInt())
            "teleport" -> ItemEffect.Teleport
            "revealMap" -> ItemEffect.RevealMap
            "freeze" -> ItemEffect.FreezeEnemies(parts[1].toInt())
            "cureStatus" -> ItemEffect.CureStatus
            "restoreHunger" -> ItemEffect.RestoreHunger(parts[1].toInt())
            "restoreTorch" -> ItemEffect.RestoreTorch(parts[1].toInt())
            else -> null
        }
    }

    // === Buff ===

    private fun toSaveBuff(b: ActiveBuff): SaveBuff = SaveBuff(
        name = b.name,
        atkBonus = b.atkBonus, defBonus = b.defBonus,
        magBonus = b.magBonus, spdBonus = b.spdBonus,
        turnsRemaining = b.turnsRemaining,
        statusEffect = b.statusEffect?.name,
        dotDamage = b.dotDamage
    )

    private fun fromSaveBuff(sb: SaveBuff): ActiveBuff = ActiveBuff(
        name = sb.name,
        atkBonus = sb.atkBonus, defBonus = sb.defBonus,
        magBonus = sb.magBonus, spdBonus = sb.spdBonus,
        turnsRemaining = sb.turnsRemaining,
        statusEffect = sb.statusEffect?.let { name -> StatusEffect.entries.find { it.name == name } },
        dotDamage = sb.dotDamage
    )

    // === FloorItem ===

    private fun toSaveFloorItem(fi: FloorItem): SaveFloorItem = SaveFloorItem(
        item = toSaveItem(fi.item),
        x = fi.position.x, y = fi.position.y
    )

    private fun fromSaveFloorItem(sfi: SaveFloorItem): FloorItem = FloorItem(
        item = fromSaveItem(sfi.item),
        position = Position(sfi.x, sfi.y)
    )
}

// === Serializable Save Data Classes ===

@Serializable
data class SaveData(
    val seed: Long,
    val classId: String,
    val currentFloor: Int,
    val turnCount: Int,
    val enemiesKilled: Int,
    val status: String,
    val messageLog: List<String>,
    val lastTrapTriggered: Boolean,
    val lastCritical: Boolean,
    val player: SavePlayer,
    val enemies: List<SaveEnemy>,
    val floorItems: List<SaveFloorItem>,
    val interactableUsed: List<Boolean>,
    val gridOverrides: List<SaveGridOverride>,
    val difficulty: String = "Normal",
    val quests: List<SaveQuest> = emptyList()
)

@Serializable
data class SavePlayer(
    val x: Int, val y: Int,
    val level: Int, val xp: Int, val xpToNext: Int,
    val hp: Int, val maxHp: Int,
    val atk: Int, val def: Int, val mag: Int, val spd: Int,
    val inventoryItems: List<SaveItem>,
    val inventoryCapacity: Int,
    val weapon: SaveItem? = null,
    val armor: SaveItem? = null,
    val accessory: SaveItem? = null,
    val buffs: List<SaveBuff>,
    val skillCooldowns: Map<String, Int> = emptyMap(),
    val torchFuel: Int = 100,
    val maxTorchFuel: Int = 100,
    val hunger: Int = 100,
    val maxHunger: Int = 100
)

@Serializable
data class SaveEnemy(
    val id: Int, val typeId: String,
    val x: Int, val y: Int,
    val hp: Int, val maxHp: Int,
    val atk: Int, val def: Int, val mag: Int, val spd: Int,
    val xpReward: Int,
    val behavior: String,
    val alertedByPlayer: Boolean,
    val displayName: String,
    val isBoss: Boolean, val bossPhase: Int,
    val buffs: List<SaveBuff>,
    val isElite: Boolean = false
)

@Serializable
data class SaveItem(
    val id: Int, val templateId: String,
    val displayName: String,
    val category: String,
    val equipSlot: String? = null,
    val rarity: String,
    val atk: Int, val def: Int, val mag: Int, val spd: Int,
    val hpBonus: Int,
    val effectType: String? = null,
    val description: String
)

@Serializable
data class SaveBuff(
    val name: String,
    val atkBonus: Int, val defBonus: Int,
    val magBonus: Int, val spdBonus: Int,
    val turnsRemaining: Int,
    val statusEffect: String? = null,
    val dotDamage: Int
)

@Serializable
data class SaveFloorItem(
    val item: SaveItem,
    val x: Int, val y: Int
)

@Serializable
data class SavePos(val x: Int, val y: Int)

@Serializable
data class SaveQuest(
    val id: String,
    val type: String,
    val description: String,
    val targetCount: Int,
    val progress: Int,
    val rewardXp: Int,
    val completed: Boolean
)

@Serializable
data class SaveGridOverride(val x: Int, val y: Int, val tile: String)
