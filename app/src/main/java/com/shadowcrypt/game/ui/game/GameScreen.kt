package com.shadowcrypt.game.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.data.model.UnlockCondition
import com.shadowcrypt.game.engine.GameAction
import com.shadowcrypt.game.engine.model.Tile
import com.shadowcrypt.game.engine.model.Visibility
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.ItemColor
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI

@Composable
fun GameScreen(
    onGameOver: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isInventoryOpen by viewModel.isInventoryOpen.collectAsStateWithLifecycle()
    val showDescendConfirm by viewModel.showDescendConfirm.collectAsStateWithLifecycle()
    val isCombatLogExpanded by viewModel.isCombatLogExpanded.collectAsStateWithLifecycle()
    val isMinimapVisible by viewModel.isMinimapVisible.collectAsStateWithLifecycle()
    val inspectedEnemy by viewModel.inspectedEnemy.collectAsStateWithLifecycle()

    // Screen shake animation
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }

    // Damage flash overlay
    var flashColor by remember { mutableStateOf(Color.Transparent) }

    // Floating damage numbers
    val activeFloaters = remember { mutableStateListOf<ActiveFloatingNumber>() }

    // Loot particles
    val activeParticles = remember { mutableStateListOf<ActiveParticle>() }

    // Canvas dimensions for inspect
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    // Collect visual events
    LaunchedEffect(Unit) {
        viewModel.visualEvents.collect { event ->
            when (event) {
                is VisualEvent.ScreenShake -> {
                    val shakeAmount = 8f * event.intensity
                    launch {
                        shakeX.snapTo(shakeAmount)
                        shakeX.animateTo(0f, tween(300))
                    }
                    launch {
                        shakeY.snapTo(-shakeAmount * 0.75f)
                        shakeY.animateTo(0f, tween(300))
                    }
                }
                is VisualEvent.DamageFlash -> {
                    launch {
                        flashColor = when (event.type) {
                            FlashType.DAMAGE -> HealthRed.copy(alpha = 0.25f)
                            FlashType.KILL -> Color.White.copy(alpha = 0.2f)
                        }
                        delay(120)
                        flashColor = Color.Transparent
                    }
                }
                is VisualEvent.DamageNumber -> {
                    launch {
                        val floater = ActiveFloatingNumber(
                            id = event.id,
                            worldX = event.worldPosition.x,
                            worldY = event.worldPosition.y,
                            text = when (event.type) {
                                DamageNumberType.DEALT -> "-${event.amount}"
                                DamageNumberType.RECEIVED -> "-${event.amount}"
                                DamageNumberType.HEAL -> "+${event.amount}"
                                DamageNumberType.XP -> "+${event.amount}xp"
                            },
                            color = when (event.type) {
                                DamageNumberType.DEALT -> Color.White
                                DamageNumberType.RECEIVED -> HealthRed
                                DamageNumberType.HEAL -> Color(0xFF44DD44)
                                DamageNumberType.XP -> XpGold
                            },
                            progress = 0f,
                            offsetY = 0f
                        )
                        activeFloaters.add(floater)
                        val steps = 20
                        for (i in 1..steps) {
                            delay(40)
                            val progress = i.toFloat() / steps
                            val idx = activeFloaters.indexOfFirst { it.id == floater.id }
                            if (idx >= 0) {
                                activeFloaters[idx] = activeFloaters[idx].copy(
                                    progress = progress,
                                    offsetY = progress * 40f
                                )
                            }
                        }
                        activeFloaters.removeAll { it.id == floater.id }
                    }
                }
                is VisualEvent.LootParticle -> {
                    launch {
                        val particleCount = 6
                        val particles = (0 until particleCount).map { i ->
                            ActiveParticle(
                                id = event.id * 100 + i,
                                worldX = event.worldPosition.x,
                                worldY = event.worldPosition.y,
                                colorHex = event.colorHex,
                                angle = (2f * PI.toFloat() / particleCount) * i,
                                progress = 0f
                            )
                        }
                        activeParticles.addAll(particles)
                        val steps = 15
                        for (s in 1..steps) {
                            delay(30)
                            val progress = s.toFloat() / steps
                            for (p in particles) {
                                val idx = activeParticles.indexOfFirst { it.id == p.id }
                                if (idx >= 0) {
                                    activeParticles[idx] = activeParticles[idx].copy(progress = progress)
                                }
                            }
                        }
                        for (p in particles) {
                            activeParticles.removeAll { it.id == p.id }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
    ) {
        when (val state = uiState) {
            is GameUiState.Loading -> {
                Text(
                    text = "Generating dungeon...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is GameUiState.Playing -> {
                val gameState = state.gameState
                val playerTile = gameState.dungeon.tiles
                    [gameState.player.position.y][gameState.player.position.x]
                val canDescend = playerTile == Tile.STAIRS_DOWN

                val density = LocalDensity.current

                SwipeDetector(
                    onDirection = { dir -> viewModel.onAction(GameAction.Move(dir)) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                canvasWidth = size.width.toFloat()
                                canvasHeight = size.height.toFloat()
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { offset ->
                                        viewModel.inspectAt(
                                            offset.x, offset.y,
                                            canvasWidth, canvasHeight
                                        )
                                    },
                                    onTap = {
                                        viewModel.dismissInspect()
                                    }
                                )
                            }
                    ) {
                        DungeonCanvas(
                            gameState = gameState,
                            shakeOffsetX = shakeX.value,
                            shakeOffsetY = shakeY.value,
                            activeFloatingNumbers = activeFloaters.toList(),
                            activeParticles = activeParticles.toList(),
                            modifier = Modifier.fillMaxSize()
                        )

                        // Damage flash overlay
                        if (flashColor != Color.Transparent) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(flashColor)
                            )
                        }
                    }
                }

                // HUD
                GameHud(
                    floorNumber = gameState.player.floorNumber,
                    hp = gameState.player.hp,
                    maxHp = gameState.player.effectiveMaxHp,
                    level = gameState.player.level,
                    xp = gameState.player.xp,
                    xpToNextLevel = gameState.player.xpToNextLevel,
                    classId = gameState.player.classId,
                    message = gameState.message,
                    effectiveAttack = gameState.player.effectiveAttack,
                    effectiveDefense = gameState.player.effectiveDefense,
                    turnCount = gameState.turnCount,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Minimap + toggle (top-right corner)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isMinimapVisible) {
                        MinimapOverlay(
                            gameState = gameState
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Button(
                        onClick = { viewModel.toggleMinimap() },
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameBackground.copy(alpha = 0.7f),
                            contentColor = TextSecondary
                        ),
                        contentPadding = ButtonDefaults.textButtonContentPadding
                    ) {
                        Text(
                            text = "M",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Combat log (below HUD, tappable to expand)
                CombatLogPanel(
                    combatLog = gameState.combatLog,
                    isExpanded = isCombatLogExpanded,
                    onToggle = { viewModel.toggleCombatLog() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 64.dp, start = 8.dp)
                )

                // D-pad (bottom-right)
                DpadOverlay(
                    onDirection = { dir -> viewModel.onAction(GameAction.Move(dir)) },
                    onWait = { viewModel.onAction(GameAction.Wait) },
                    onDescend = { viewModel.onAction(GameAction.DescendStairs) },
                    canDescend = canDescend,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )

                // Inventory button (bottom-left)
                Button(
                    onClick = { viewModel.toggleInventory() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GameBackground.copy(alpha = 0.7f),
                        contentColor = ItemColor
                    )
                ) {
                    Text(
                        text = "BAG",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Enemy inspect tooltip
                val enemy = inspectedEnemy
                if (enemy != null) {
                    EnemyInspectTooltip(
                        enemy = enemy,
                        density = density,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                // Inventory overlay
                if (isInventoryOpen) {
                    InventoryOverlay(
                        inventory = gameState.player.inventory,
                        onEquip = { viewModel.onAction(GameAction.EquipItem(it)) },
                        onUse = { viewModel.onAction(GameAction.UseItem(it)) },
                        onDrop = {
                            viewModel.onAction(GameAction.DropItem(it))
                            viewModel.closeInventory()
                        },
                        onUnequip = { viewModel.onAction(GameAction.UnequipItem(it)) },
                        onClose = { viewModel.closeInventory() }
                    )
                }

                // Descend confirmation dialog
                if (showDescendConfirm) {
                    val nextFloor = gameState.player.floorNumber + 1
                    AlertDialog(
                        onDismissRequest = { viewModel.cancelDescend() },
                        title = {
                            Text(
                                text = "DESCEND",
                                color = StairsColor,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        text = {
                            Text(
                                text = "Descend to Floor $nextFloor?",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.confirmDescend() }) {
                                Text("YES", color = StairsColor)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.cancelDescend() }) {
                                Text("NO", color = TextSecondary)
                            }
                        },
                        containerColor = GameBackground,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            is GameUiState.Descending -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GameBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Descending to Floor ${state.floorNumber}...",
                        style = MaterialTheme.typography.headlineLarge,
                        color = StairsColor
                    )
                }
            }

            is GameUiState.GameOver -> {
                val titleText = if (state.won) "VICTORY" else "YOU HAVE FALLEN"
                val titleColor = if (state.won) XpGold else HealthRed
                val newUnlocks by viewModel.newUnlocks.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GameBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.displayMedium,
                            color = titleColor
                        )

                        if (state.won) {
                            Text(
                                text = "The Shadowcrypt has been conquered!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Floor Reached: ${state.floorReached}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Enemies Slain: ${state.enemiesKilled}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Level: ${state.level}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = XpGold
                        )
                        Text(
                            text = "Turns: ${state.turnsTaken}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (newUnlocks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NEW UNLOCKS!",
                                style = MaterialTheme.typography.titleMedium,
                                color = XpGold
                            )
                            for (classId in newUnlocks) {
                                val condition = UnlockCondition.entries.find { it.classId == classId }
                                Text(
                                    text = condition?.displayName ?: classId.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DungeonPurple80
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onGameOver,
                            modifier = Modifier.width(220.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DungeonPurple80.copy(alpha = 0.3f),
                                contentColor = DungeonPurple80
                            )
                        ) {
                            Text(
                                text = "RETURN TO MENU",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== Minimap Overlay =====

@Composable
fun MinimapOverlay(
    gameState: com.shadowcrypt.game.engine.model.GameState,
    modifier: Modifier = Modifier
) {
    val dungeon = gameState.dungeon
    val visibility = gameState.visibility
    val player = gameState.player

    val mapSize = 100.dp

    Box(
        modifier = modifier
            .size(mapSize)
            .clip(RoundedCornerShape(8.dp))
            .background(GameBackground.copy(alpha = 0.8f))
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize().padding(2.dp)
        ) {
            val pixelPerTileX = size.width / dungeon.width
            val pixelPerTileY = size.height / dungeon.height

            for (y in 0 until dungeon.height) {
                for (x in 0 until dungeon.width) {
                    val vis = visibility[y][x]
                    if (vis == Visibility.UNEXPLORED) continue

                    val tile = dungeon.tiles[y][x]
                    if (tile == com.shadowcrypt.game.engine.model.Tile.WALL) continue

                    val color = when {
                        x == player.position.x && y == player.position.y -> PlayerColor
                        tile == com.shadowcrypt.game.engine.model.Tile.STAIRS_DOWN -> StairsColor
                        vis == Visibility.VISIBLE -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.2f)
                    }

                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            x * pixelPerTileX,
                            y * pixelPerTileY
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            pixelPerTileX.coerceAtLeast(1f),
                            pixelPerTileY.coerceAtLeast(1f)
                        )
                    )
                }
            }

            // Draw visible enemies as red dots
            for (enemy in gameState.enemies) {
                val vis = visibility[enemy.position.y][enemy.position.x]
                if (vis != Visibility.VISIBLE) continue
                drawCircle(
                    color = HealthRed,
                    radius = 2f,
                    center = androidx.compose.ui.geometry.Offset(
                        enemy.position.x * pixelPerTileX + pixelPerTileX / 2,
                        enemy.position.y * pixelPerTileY + pixelPerTileY / 2
                    )
                )
            }
        }
    }
}

// ===== Combat Log Panel =====

@Composable
fun CombatLogPanel(
    combatLog: List<String>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(200.dp)) {
        // Toggle button
        Box(
            modifier = Modifier
                .background(HudBackground, RoundedCornerShape(4.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isExpanded) "LOG \u25BC" else "LOG \u25B6",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (isExpanded && combatLog.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            val listState = rememberLazyListState()

            // Auto-scroll to bottom
            LaunchedEffect(combatLog.size) {
                if (combatLog.isNotEmpty()) {
                    listState.animateScrollToItem(combatLog.size - 1)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 150.dp)
                    .background(HudBackground, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(combatLog) { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// ===== Enemy Inspect Tooltip =====

@Composable
fun EnemyInspectTooltip(
    enemy: InspectedEnemy,
    density: androidx.compose.ui.unit.Density,
    modifier: Modifier = Modifier
) {
    val offsetX = with(density) { enemy.screenX.toDp() }
    val offsetY = with(density) { enemy.screenY.toDp() }

    Box(
        modifier = modifier
            .padding(start = offsetX.coerceAtMost(200.dp), top = (offsetY - 80.dp).coerceAtLeast(0.dp))
    ) {
        Column(
            modifier = Modifier
                .background(GameBackground.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            val nameColor = if (enemy.isBoss) com.shadowcrypt.game.ui.theme.BossColor else HealthRed
            Text(
                text = enemy.name,
                style = MaterialTheme.typography.titleSmall,
                color = nameColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            val hpFraction = enemy.hp.toFloat() / enemy.maxHp
            val hpColor = when {
                hpFraction > 0.5f -> Color(0xFF44DD44)
                hpFraction > 0.25f -> Color(0xFFDDDD44)
                else -> HealthRed
            }
            Text(
                text = "HP: ${enemy.hp}/${enemy.maxHp}",
                style = MaterialTheme.typography.bodySmall,
                color = hpColor
            )
            Text(
                text = "ATK: ${enemy.attack}  DEF: ${enemy.defense}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
