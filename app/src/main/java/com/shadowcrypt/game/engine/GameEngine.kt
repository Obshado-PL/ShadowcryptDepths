package com.shadowcrypt.game.engine

import com.shadowcrypt.game.model.ActiveBuff
import com.shadowcrypt.game.model.CharacterClass
import com.shadowcrypt.game.model.CombatResult
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.FloorItem
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.GameStatus
import com.shadowcrypt.game.model.Interactable
import com.shadowcrypt.game.model.Player
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Quest
import com.shadowcrypt.game.model.QuestGenerator
import com.shadowcrypt.game.model.QuestType
import com.shadowcrypt.game.model.Room
import com.shadowcrypt.game.model.RoomEventType
import com.shadowcrypt.game.model.Skill
import com.shadowcrypt.game.model.SkillTarget
import com.shadowcrypt.game.model.Skills
import com.shadowcrypt.game.model.StatusEffect
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameEngine(
    private val dungeonGenerator: DungeonGenerator = DungeonGenerator(),
    private val fovEngine: FovEngine = FovEngine(viewRadius = 12)
) {

    fun newGame(
        classId: String, seed: Long, difficulty: Difficulty = Difficulty.Normal,
        hpBonus: Int = 0, atkBonus: Int = 0, defBonus: Int = 0, magBonus: Int = 0, spdBonus: Int = 0
    ): GameState {
        ItemGenerator.resetIdCounter()

        val characterClass = CharacterClass.fromId(classId)
        val player = if (hpBonus + atkBonus + defBonus + magBonus + spdBonus > 0) {
            Player.create(characterClass, hpBonus, atkBonus, defBonus, magBonus, spdBonus)
        } else {
            Player.create(characterClass)
        }
        val dungeon = dungeonGenerator.generate(floorNumber = 1, seed = seed)

        val placedPlayer = player.copy(
            position = dungeon.playerStart,
            currentFloor = 1
        )

        val visible = fovEngine.computeVisible(placedPlayer.position, dungeon)
        val visibilityMap = fovEngine.updateVisibilityMap(emptyMap(), visible)

        val enemies = EnemySpawner.spawnForFloor(
            floorNumber = 1,
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            random = Random(seed + 2000L),
            difficulty = difficulty
        )

        val floorItems = ItemGenerator.generateFloorItems(
            floor = 1,
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            occupiedPositions = enemies.map { it.position }.toSet(),
            random = Random(seed + 3000L),
            difficulty = difficulty
        )

        val interactables = generateInteractables(
            rooms = dungeon.rooms,
            playerStart = dungeon.playerStart,
            occupiedPositions = enemies.map { it.position }.toSet() +
                    floorItems.map { it.position }.toSet(),
            random = Random(seed + 4000L)
        )

        val quests = QuestGenerator.generateForFloor(1, seed)

        return GameState(
            dungeon = dungeon,
            player = placedPlayer,
            enemies = enemies,
            enemiesKilled = 0,
            visibilityMap = visibilityMap,
            turnCount = 0,
            status = GameStatus.Playing,
            messageLog = listOf(
                "You descend into the Shadowcrypt...",
                "Floor 1: ${dungeon.theme.displayName}",
                "Difficulty: ${difficulty.displayName}"
            ),
            seed = seed,
            floorItems = floorItems,
            interactables = interactables,
            difficulty = difficulty,
            quests = quests
        )
    }

    fun movePlayer(state: GameState, direction: Direction): GameState {
        if (state.status != GameStatus.Playing) return state

        val newPos = state.player.position.move(direction)

        if (!state.dungeon.isWalkable(newPos)) {
            return state.withMessage("You bump into a wall.")
        }

        // Check for bump attack
        val targetEnemy = state.enemies.find { it.position == newPos && it.isAlive }
        if (targetEnemy != null) {
            return resolveBumpAttack(state, targetEnemy)
        }

        // Move player
        val movedPlayer = state.player.copy(position = newPos)
        val visible = fovEngine.computeVisible(newPos, state.dungeon)
        val updatedVisibility = fovEngine.updateVisibilityMap(state.visibilityMap, visible)

        var newState = state.copy(
            player = movedPlayer,
            visibilityMap = updatedVisibility,
            turnCount = state.turnCount + 1,
            lastTrapTriggered = false,
            lastCritical = false
        )

        when (state.dungeon.tileAt(newPos)) {
            Tile.StairsDown -> newState = newState.withMessage("You see stairs going deeper...")
            Tile.StairsUp -> newState = newState.withMessage("Stairs lead back up.")
            Tile.Door -> newState = newState.withMessage("You pass through a doorway.")
            Tile.Trap -> {
                val trapDamage = (2 + state.player.currentFloor + state.difficulty.trapDamageBonus).coerceIn(2, 12)
                val newHp = (newState.player.hp - trapDamage).coerceAtLeast(0)
                newState = newState.copy(
                    player = newState.player.copy(hp = newHp),
                    lastTrapTriggered = true
                )
                newState = newState.withMessage("You triggered a trap! Took $trapDamage damage.")
                newState.dungeon.grid[newPos.y][newPos.x] = Tile.Floor
            }
            Tile.Water -> newState = newState.withMessage("You wade through water...")
            else -> {}
        }

        // Auto-pickup floor item
        val itemAtPos = newState.floorItems.find { it.position == newPos }
        if (itemAtPos != null) {
            newState = InventoryEngine.pickUpItem(newState, itemAtPos)
        }

        // Interact with room event
        val interactable = newState.interactables.find { it.position == newPos && !it.used }
        if (interactable != null) {
            newState = resolveInteraction(newState, interactable)
        }

        return endTurn(newState)
    }

    fun descendStairs(state: GameState): GameState {
        if (state.status != GameStatus.Playing) return state

        if (state.dungeon.tileAt(state.player.position) != Tile.StairsDown) {
            return state.withMessage("There are no stairs here.")
        }

        val nextFloor = state.player.currentFloor + 1

        if (nextFloor > 10) {
            return state.copy(
                status = GameStatus.Victory,
                messageLog = state.messageLog + "You have conquered the Shadowcrypt!"
            )
        }

        val newDungeon = dungeonGenerator.generate(
            floorNumber = nextFloor,
            seed = state.seed
        )

        val movedPlayer = state.player.copy(
            position = newDungeon.playerStart,
            currentFloor = nextFloor,
            torchFuel = min(state.player.maxTorchFuel, state.player.torchFuel + 30)
        )

        val visible = fovEngine.computeVisible(movedPlayer.position, newDungeon)
        val visibilityMap = fovEngine.updateVisibilityMap(emptyMap(), visible)

        val newEnemies = EnemySpawner.spawnForFloor(
            floorNumber = nextFloor,
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            random = Random(state.seed + nextFloor * 2000L),
            difficulty = state.difficulty
        )

        val newFloorItems = ItemGenerator.generateFloorItems(
            floor = nextFloor,
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            occupiedPositions = newEnemies.map { it.position }.toSet(),
            random = Random(state.seed + nextFloor * 3000L),
            difficulty = state.difficulty
        )

        val newInteractables = generateInteractables(
            rooms = newDungeon.rooms,
            playerStart = newDungeon.playerStart,
            occupiedPositions = newEnemies.map { it.position }.toSet() +
                    newFloorItems.map { it.position }.toSet(),
            random = Random(state.seed + nextFloor * 4000L)
        )

        val newQuests = QuestGenerator.generateForFloor(nextFloor, state.seed)

        return GameState(
            dungeon = newDungeon,
            player = movedPlayer,
            enemies = newEnemies,
            enemiesKilled = state.enemiesKilled,
            visibilityMap = visibilityMap,
            turnCount = state.turnCount,
            status = GameStatus.Playing,
            messageLog = state.messageLog + listOf(
                "You descend to floor $nextFloor...",
                newDungeon.theme.displayName
            ),
            seed = state.seed,
            floorItems = newFloorItems,
            interactables = newInteractables,
            difficulty = state.difficulty,
            quests = newQuests,
            isDaily = state.isDaily
        )
    }

    fun tapTile(state: GameState, target: Position): GameState {
        if (state.status != GameStatus.Playing) return state

        val visibility = state.visibilityMap[target]
        if (visibility != Visibility.Visible) return state

        // Ranged attack: tap on a visible enemy within attack range
        val targetEnemy = state.enemies.find { it.position == target && it.isAlive }
        if (targetEnemy != null) {
            val dist = state.player.position.distanceTo(target)
            val range = CharacterClass.fromId(state.player.classId).attackRange
            if (dist in 2..range) {
                return resolveRangedAttack(state, targetEnemy)
            }
        }

        val playerPos = state.player.position
        val dx = target.x - playerPos.x
        val dy = target.y - playerPos.y

        if (abs(dx) + abs(dy) == 1) {
            val direction = Direction.cardinal().find { it.dx == dx && it.dy == dy }
            if (direction != null) return movePlayer(state, direction)
        }

        val path = Pathfinding.findPath(playerPos, target, state.dungeon)
        if (path != null && path.isNotEmpty()) {
            val nextStep = path.first()
            val stepDx = nextStep.x - playerPos.x
            val stepDy = nextStep.y - playerPos.y
            val direction = Direction.cardinal().find { it.dx == stepDx && it.dy == stepDy }
            if (direction != null) return movePlayer(state, direction)
        }

        return state.withMessage("You can't reach there.")
    }

    // ===== Combat Integration =====

    private fun resolveBumpAttack(state: GameState, target: Enemy): GameState {
        return resolveAttack(state, target, "hit")
    }

    private fun resolveRangedAttack(state: GameState, target: Enemy): GameState {
        return resolveAttack(state, target, "cast at")
    }

    private fun resolveAttack(state: GameState, target: Enemy, verb: String): GameState {
        val combatRandom = Random(state.seed + state.turnCount * 31L)
        val result = CombatEngine.playerAttackEnemy(state.player, target, combatRandom)

        var newState = state.copy(
            turnCount = state.turnCount + 1,
            lastTrapTriggered = false,
            lastCritical = false
        )

        when (result) {
            is CombatResult.Miss -> {
                newState = newState.withMessage("You miss the ${target.displayName}!")
            }

            is CombatResult.Hit -> {
                newState = newState.copy(lastCritical = result.isCritical)
                val critText = if (result.isCritical) "CRITICAL! " else ""
                newState = newState.withMessage(
                    "${critText}You $verb ${target.displayName} for ${result.damage} damage."
                )

                if (result.defenderKilled) {
                    newState = newState.copy(
                        enemies = newState.enemies.filter { it.id != target.id },
                        enemiesKilled = newState.enemiesKilled + 1
                    ).withMessage("${target.displayName} is defeated!")

                    val (leveledPlayer, levelMsgs) = LevelingEngine.awardXp(
                        newState.player, target.xpReward
                    )
                    newState = newState.copy(player = leveledPlayer)
                    newState = newState.withMessage("+${target.xpReward} XP")
                    for (msg in levelMsgs) {
                        newState = newState.withMessage(msg)
                    }

                    // Loot drop
                    val lootRandom = Random(state.seed + state.turnCount * 41L + target.id)
                    val drop = ItemGenerator.rollEnemyDrop(
                        target, state.player.currentFloor, lootRandom, state.difficulty
                    )
                    if (drop != null) {
                        val floorItem = FloorItem(drop, target.position)
                        newState = newState.copy(floorItems = newState.floorItems + floorItem)
                        newState = newState.withMessage("${target.displayName} dropped ${drop.displayName}!")
                    }
                } else {
                    var updatedEnemy = target.copy(hp = result.defenderHpAfter)

                    // Roll for status effect on surviving enemy
                    val statusRandom = Random(state.seed + state.turnCount * 43L + target.id)
                    val statusBuff = CombatEngine.rollPlayerStatusEffect(
                        state.player.classId, statusRandom
                    )
                    if (statusBuff != null && updatedEnemy.activeBuffs.none {
                            it.statusEffect == statusBuff.statusEffect
                        }) {
                        updatedEnemy = updatedEnemy.copy(
                            activeBuffs = updatedEnemy.activeBuffs + statusBuff
                        )
                        val effectVerb = when (statusBuff.statusEffect) {
                            StatusEffect.Poison -> "poisoned"
                            StatusEffect.Burn -> "burning"
                            StatusEffect.Stun -> "stunned"
                            StatusEffect.Slow -> "slowed"
                            null -> "affected"
                        }
                        newState = newState.withMessage(
                            "${target.displayName} is $effectVerb!"
                        )
                    }

                    newState = newState.copy(
                        enemies = newState.enemies.map {
                            if (it.id == target.id) updatedEnemy else it
                        }
                    )
                }
            }
        }

        // Update FOV
        val visible = fovEngine.computeVisible(newState.player.position, newState.dungeon)
        val updatedVis = fovEngine.updateVisibilityMap(newState.visibilityMap, visible)
        newState = newState.copy(visibilityMap = updatedVis)

        return endTurn(newState)
    }

    private fun endTurn(state: GameState): GameState {
        // Enemy turns
        var s = processEnemyTurns(state)

        // Tick player DoT before decrementing turns
        val playerDot = s.player.activeBuffs
            .filter { it.statusEffect == StatusEffect.Poison || it.statusEffect == StatusEffect.Burn }
            .sumOf { it.dotDamage }
        if (playerDot > 0) {
            val newHp = max(0, s.player.hp - playerDot)
            s = s.copy(player = s.player.copy(hp = newHp))
                .withMessage("You take $playerDot damage from status effects!")
        }

        // Tick player buffs (decrement + remove expired)
        s = s.copy(player = InventoryEngine.tickBuffs(s.player))

        // Tick skill cooldowns
        val tickedCooldowns = s.player.skillCooldowns
            .mapValues { (_, cd) -> cd - 1 }
            .filterValues { it > 0 }
        s = s.copy(player = s.player.copy(skillCooldowns = tickedCooldowns))

        // Torch depletion (-1 per turn)
        val newTorch = (s.player.torchFuel - 1).coerceAtLeast(0)
        s = s.copy(player = s.player.copy(torchFuel = newTorch))
        if (newTorch == 20) {
            s = s.withMessage("Your torch flickers... visibility reduced!")
        } else if (newTorch == 0 && s.player.torchFuel > 0) {
            s = s.withMessage("Your torch has gone out! You can barely see!")
        }

        // Hunger depletion (-1 every 2 turns)
        if (s.turnCount % 2 == 0) {
            val newHunger = (s.player.hunger - 1).coerceAtLeast(0)
            s = s.copy(player = s.player.copy(hunger = newHunger))
            if (newHunger == 20) {
                s = s.withMessage("You're getting hungry...")
            } else if (newHunger == 0) {
                // Starvation damage
                val starveDmg = 2
                val newHp = max(1, s.player.hp - starveDmg)
                s = s.copy(player = s.player.copy(hp = newHp))
                    .withMessage("You're starving! Lost $starveDmg HP.")
            }
        }

        // Recalculate FOV with torch-adjusted radius
        val torchRadius = getTorchViewRadius(s.player.torchFuel)
        val torchFov = FovEngine(viewRadius = torchRadius)
        val torchVisible = torchFov.computeVisible(s.player.position, s.dungeon)
        val torchVisMap = torchFov.updateVisibilityMap(s.visibilityMap, torchVisible)
        s = s.copy(visibilityMap = torchVisMap)

        // Tick enemy DoT and buffs
        val updatedEnemies = s.enemies.map { enemy ->
            if (enemy.activeBuffs.isEmpty()) return@map enemy

            val dot = enemy.activeBuffs
                .filter { it.statusEffect == StatusEffect.Poison || it.statusEffect == StatusEffect.Burn }
                .sumOf { it.dotDamage }
            val newHp = if (dot > 0) max(0, enemy.hp - dot) else enemy.hp

            val tickedBuffs = enemy.activeBuffs
                .map { it.copy(turnsRemaining = it.turnsRemaining - 1) }
                .filter { it.turnsRemaining > 0 }

            enemy.copy(hp = newHp, activeBuffs = tickedBuffs)
        }

        // Handle enemies killed by DoT
        val dotKilled = updatedEnemies.filter { !it.isAlive && s.enemies.find { e -> e.id == it.id }?.isAlive == true }
        var newEnemies = updatedEnemies
        var player = s.player
        for (killed in dotKilled) {
            s = s.withMessage("${killed.displayName} succumbs to status effects!")
            newEnemies = newEnemies.filter { it.id != killed.id }
            val (leveledPlayer, levelMsgs) = LevelingEngine.awardXp(player, killed.xpReward)
            player = leveledPlayer
            s = s.withMessage("+${killed.xpReward} XP")
            for (msg in levelMsgs) {
                s = s.withMessage(msg)
            }
        }
        s = s.copy(enemies = newEnemies, player = player, enemiesKilled = s.enemiesKilled + dotKilled.size)

        // Update quest progress
        s = updateQuestProgress(s)

        // Check player death
        if (!s.player.isAlive) {
            s = s.copy(status = GameStatus.Dead)
                .withMessage("You have been slain...")
        }

        return s
    }

    private fun processEnemyTurns(state: GameState): GameState {
        val combatRandom = Random(state.seed + state.turnCount * 37L)
        return EnemyAI.processAllEnemies(state, combatRandom)
    }

    // ===== Skills =====

    fun castSkill(state: GameState, skillId: String): GameState {
        if (state.status != GameStatus.Playing) return state

        val skill = Skills.forClass(state.player.classId).find { it.id == skillId }
            ?: return state.withMessage("Unknown skill.")
        val cd = state.player.skillCooldowns[skillId] ?: 0
        if (cd > 0) return state.withMessage("${skill.displayName} on cooldown ($cd turns).")

        var s = state.copy(
            turnCount = state.turnCount + 1,
            lastTrapTriggered = false,
            lastCritical = false
        )

        // Put skill on cooldown
        val newCooldowns = s.player.skillCooldowns + (skillId to skill.cooldown)
        s = s.copy(player = s.player.copy(skillCooldowns = newCooldowns))

        val random = Random(state.seed + state.turnCount * 53L)

        when (skill.target) {
            SkillTarget.Self -> {
                s = resolveSelfSkill(s, skill, random)
            }
            SkillTarget.SingleEnemy -> {
                s = resolveSingleTargetSkill(s, skill, random)
            }
            SkillTarget.AllEnemies -> {
                s = resolveAllEnemiesSkill(s, skill, random)
            }
            SkillTarget.AreaOfEffect -> {
                s = resolveAoeSkill(s, skill, random)
            }
        }

        val visible = fovEngine.computeVisible(s.player.position, s.dungeon)
        val updatedVis = fovEngine.updateVisibilityMap(s.visibilityMap, visible)
        s = s.copy(visibilityMap = updatedVis)

        return endTurn(s)
    }

    private fun resolveSelfSkill(state: GameState, skill: Skill, random: Random): GameState {
        var s = state

        // Special: Blink = teleport
        if (skill.id == "blink") {
            val walkable = mutableListOf<Position>()
            for (row in 0 until s.dungeon.height) {
                for (col in 0 until s.dungeon.width) {
                    val pos = Position(col, row)
                    if (s.dungeon.isWalkable(pos) &&
                        s.enemies.none { it.position == pos } &&
                        pos != s.player.position
                    ) walkable.add(pos)
                }
            }
            if (walkable.isNotEmpty()) {
                val dest = walkable[random.nextInt(walkable.size)]
                s = s.copy(player = s.player.copy(position = dest))
                    .withMessage("You blink to a new location!")
            }
            return s
        }

        // Heal
        if (skill.healAmount > 0) {
            val heal = (s.player.effectiveMaxHp * skill.healAmount / 100)
                .coerceAtMost(s.player.effectiveMaxHp - s.player.hp)
            if (heal > 0) {
                s = s.copy(player = s.player.copy(hp = s.player.hp + heal))
                    .withMessage("${skill.displayName} heals you for $heal HP!")
            } else {
                s = s.withMessage("${skill.displayName}! You're already at full health.")
            }
        }

        // Buff
        if (skill.buff != null) {
            s = s.copy(
                player = s.player.copy(activeBuffs = s.player.activeBuffs + skill.buff)
            ).withMessage("${skill.displayName} activated!")
        }

        // Sanctuary: also cure status effects
        if (skill.id == "sanctuary") {
            val cleansed = s.player.activeBuffs.filter { it.statusEffect == null }
            s = s.copy(player = s.player.copy(activeBuffs = cleansed))
                .withMessage("All status effects cleansed!")
        }

        return s
    }

    private fun resolveSingleTargetSkill(state: GameState, skill: Skill, random: Random): GameState {
        // Find nearest visible enemy in range
        val targets = state.enemies.filter { enemy ->
            enemy.isAlive &&
            state.visibilityMap[enemy.position] == Visibility.Visible &&
            state.player.position.distanceTo(enemy.position) <= skill.range
        }.sortedBy { state.player.position.distanceTo(it.position) }

        val target = targets.firstOrNull()
            ?: return state.withMessage("No enemy in range for ${skill.displayName}.")

        return applySkillDamage(state, skill, target, random)
    }

    private fun resolveAllEnemiesSkill(state: GameState, skill: Skill, random: Random): GameState {
        val targets = state.enemies.filter { enemy ->
            enemy.isAlive &&
            state.visibilityMap[enemy.position] == Visibility.Visible &&
            state.player.position.distanceTo(enemy.position) <= skill.range
        }

        if (targets.isEmpty()) {
            return state.withMessage("No enemies in range for ${skill.displayName}.")
        }

        var s = state.withMessage("${skill.displayName}!")
        for (target in targets) {
            s = if (skill.damageMultiplier > 0f) {
                applySkillDamage(s, skill, target, random)
            } else if (skill.statusToInflict != null) {
                applySkillStatus(s, skill, target)
            } else s
        }
        return s
    }

    private fun resolveAoeSkill(state: GameState, skill: Skill, random: Random): GameState {
        // For AOE, find nearest enemy as center, then hit all within radius
        val center = state.enemies.filter { enemy ->
            enemy.isAlive &&
            state.visibilityMap[enemy.position] == Visibility.Visible &&
            state.player.position.distanceTo(enemy.position) <= skill.range
        }.minByOrNull { state.player.position.distanceTo(it.position) }
            ?: return state.withMessage("No enemies in range for ${skill.displayName}.")

        val targets = state.enemies.filter { enemy ->
            enemy.isAlive && enemy.position.distanceTo(center.position) <= skill.aoeRadius
        }

        var s = state.withMessage("${skill.displayName}!")
        for (target in targets) {
            s = applySkillDamage(s, skill, target, random)
        }
        return s
    }

    private fun applySkillDamage(state: GameState, skill: Skill, target: Enemy, random: Random): GameState {
        val player = state.player
        val useMagic = player.classId == "mage" || player.classId == "cleric"
        val baseDmg = if (useMagic) player.effectiveMag else player.effectiveAtk
        val defense = if (useMagic) target.effectiveDef / 4 else target.effectiveDef / 2
        var damage = max(1, ((baseDmg - defense) * skill.damageMultiplier).toInt() + random.nextInt(-1, 2))

        // Backstab always crits
        val isCrit = skill.id == "backstab"
        if (isCrit) damage = max(damage + 1, (damage * 1.5f).toInt())

        var s = state.copy(lastCritical = isCrit)
        val newHp = max(0, target.hp - damage)
        val critText = if (isCrit) "CRIT! " else ""

        if (newHp <= 0) {
            s = s.copy(
                enemies = s.enemies.filter { it.id != target.id },
                enemiesKilled = s.enemiesKilled + 1
            ).withMessage("${critText}${skill.displayName} deals $damage to ${target.displayName}! Defeated!")
            val (leveledPlayer, levelMsgs) = LevelingEngine.awardXp(s.player, target.xpReward)
            s = s.copy(player = leveledPlayer).withMessage("+${target.xpReward} XP")
            for (msg in levelMsgs) s = s.withMessage(msg)

            val lootRandom = Random(state.seed + state.turnCount * 41L + target.id)
            val drop = ItemGenerator.rollEnemyDrop(target, state.player.currentFloor, lootRandom, state.difficulty)
            if (drop != null) {
                s = s.copy(floorItems = s.floorItems + FloorItem(drop, target.position))
                    .withMessage("${target.displayName} dropped ${drop.displayName}!")
            }
        } else {
            var updatedEnemy = target.copy(hp = newHp)
            s = s.withMessage("${critText}${skill.displayName} deals $damage to ${target.displayName}.")

            // Apply status effect
            if (skill.statusToInflict != null &&
                updatedEnemy.activeBuffs.none { it.statusEffect == skill.statusToInflict.statusEffect }
            ) {
                updatedEnemy = updatedEnemy.copy(
                    activeBuffs = updatedEnemy.activeBuffs + skill.statusToInflict
                )
                val effectName = skill.statusToInflict.statusEffect?.name?.lowercase() ?: "affected"
                s = s.withMessage("${target.displayName} is $effectName!")
            }

            s = s.copy(enemies = s.enemies.map { if (it.id == target.id) updatedEnemy else it })
        }
        return s
    }

    private fun applySkillStatus(state: GameState, skill: Skill, target: Enemy): GameState {
        val status = skill.statusToInflict ?: return state
        if (target.activeBuffs.any { it.statusEffect == status.statusEffect }) return state

        val updatedEnemy = target.copy(activeBuffs = target.activeBuffs + status)
        val effectName = status.statusEffect?.name?.lowercase() ?: "affected"
        return state.copy(
            enemies = state.enemies.map { if (it.id == target.id) updatedEnemy else it }
        ).withMessage("${target.displayName} is $effectName!")
    }

    private fun updateQuestProgress(state: GameState): GameState {
        var s = state
        val updatedQuests = s.quests.map { quest ->
            if (quest.completed) return@map quest
            val newProgress = when (quest.type) {
                QuestType.KillEnemies -> s.enemiesKilled
                QuestType.SurviveTurns -> s.turnCount
                QuestType.KillBoss -> {
                    val bossAlive = s.enemies.any { it.isBoss && it.isAlive }
                    if (!bossAlive && s.enemies.isEmpty().not()) 0 else if (!bossAlive) 1 else 0
                }
                QuestType.FindItem, QuestType.ReachStairs -> quest.progress
            }
            quest.copy(progress = newProgress)
        }

        // Check for newly completed quests
        for (quest in updatedQuests) {
            val wasComplete = s.quests.find { it.id == quest.id }?.isComplete ?: false
            if (quest.isComplete && !wasComplete && !quest.completed) {
                val (leveledPlayer, msgs) = LevelingEngine.awardXp(s.player, quest.rewardXp)
                s = s.copy(player = leveledPlayer)
                    .withMessage("Quest complete: ${quest.description}! +${quest.rewardXp} XP")
                for (msg in msgs) s = s.withMessage(msg)
            }
        }

        s = s.copy(quests = updatedQuests.map { if (it.isComplete) it.copy(completed = true) else it })
        return s
    }

    private fun getTorchViewRadius(torchFuel: Int): Int = when {
        torchFuel > 60 -> 12
        torchFuel > 30 -> 9
        torchFuel > 10 -> 6
        torchFuel > 0 -> 4
        else -> 3
    }

    // ===== Room Events =====

    private fun generateInteractables(
        rooms: List<Room>,
        playerStart: Position,
        occupiedPositions: Set<Position>,
        random: Random
    ): List<Interactable> {
        val result = mutableListOf<Interactable>()
        val types = RoomEventType.entries.toTypedArray()

        for (room in rooms) {
            if (room.center == playerStart) continue

            val type: RoomEventType? = when {
                room.type.guaranteedInteractable != null -> room.type.guaranteedInteractable
                random.nextInt(100) < 30 -> types[random.nextInt(types.size)]
                else -> null
            }

            if (type == null) continue

            val pos = room.center
            if (pos in occupiedPositions) continue

            result.add(Interactable(pos, type))
        }

        return result
    }

    private fun resolveInteraction(state: GameState, interactable: Interactable): GameState {
        val marked = state.copy(
            interactables = state.interactables.map {
                if (it.position == interactable.position) it.copy(used = true) else it
            }
        )

        return when (interactable.type) {
            RoomEventType.TreasureChest -> {
                val lootRandom = Random(state.seed + state.turnCount * 47L)
                val drop = ItemGenerator.rollTreasureItem(state.player.currentFloor, lootRandom)
                val floorItem = FloorItem(drop, interactable.position)
                marked.copy(floorItems = marked.floorItems + floorItem)
                    .withMessage("You open a treasure chest! Found ${drop.displayName}!")
            }
            RoomEventType.Shrine -> {
                val shrineRandom = Random(state.seed + state.turnCount * 51L)
                val stats = listOf("ATK", "DEF", "MAG", "SPD")
                val stat = stats[shrineRandom.nextInt(stats.size)]
                val buff = when (stat) {
                    "ATK" -> ActiveBuff("Shrine ATK", atkBonus = 3, turnsRemaining = 30)
                    "DEF" -> ActiveBuff("Shrine DEF", defBonus = 3, turnsRemaining = 30)
                    "MAG" -> ActiveBuff("Shrine MAG", magBonus = 3, turnsRemaining = 30)
                    else -> ActiveBuff("Shrine SPD", spdBonus = 3, turnsRemaining = 30)
                }
                val newPlayer = marked.player.copy(
                    activeBuffs = marked.player.activeBuffs + buff
                )
                marked.copy(player = newPlayer)
                    .withMessage("The shrine grants you +3 $stat for 30 turns!")
            }
            RoomEventType.Fountain -> {
                val healAmount = min(
                    marked.player.effectiveMaxHp / 3,
                    marked.player.effectiveMaxHp - marked.player.hp
                )
                val hungerRestore = min(20, marked.player.maxHunger - marked.player.hunger)
                val newPlayer = marked.player.copy(
                    hp = marked.player.hp + healAmount,
                    hunger = marked.player.hunger + hungerRestore
                )
                if (healAmount > 0 || hungerRestore > 0) {
                    marked.copy(player = newPlayer)
                        .withMessage("You drink from the fountain. Restored $healAmount HP and $hungerRestore hunger.")
                } else {
                    marked.withMessage("You drink from the fountain. You feel refreshed.")
                }
            }
        }
    }
}
