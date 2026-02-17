package com.shadowcrypt.game.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.model.GameStatus
import com.shadowcrypt.game.model.Visibility
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import kotlinx.coroutines.launch
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary


@Composable
fun GameScreen(
    classId: String,
    seed: Long,
    loadSave: Boolean = false,
    difficultyName: String = "Normal",
    upgradeHp: Int = 0,
    upgradeAtk: Int = 0,
    upgradeDef: Int = 0,
    upgradeMag: Int = 0,
    upgradeSpd: Int = 0,
    isDaily: Boolean = false,
    onGameOver: (floorReached: Int, enemiesKilled: Int, turnsTaken: Int, won: Boolean, lastMessages: String) -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(classId, seed, loadSave) {
        if (loadSave) {
            viewModel.loadSavedGame(context)
        } else {
            val diff = Difficulty.entries.find { it.name == difficultyName } ?: Difficulty.Normal
            viewModel.initGame(classId, seed, diff, upgradeHp, upgradeAtk, upgradeDef, upgradeMag, upgradeSpd, isDaily)
        }
    }

    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val transitionFloor by viewModel.transitionFloor.collectAsStateWithLifecycle()
    val transitionTheme by viewModel.transitionTheme.collectAsStateWithLifecycle()
    val showInventory by viewModel.showInventory.collectAsStateWithLifecycle()
    val showPauseMenu by viewModel.showPauseMenu.collectAsStateWithLifecycle()
    val floatingTexts by viewModel.floatingTexts.collectAsStateWithLifecycle()
    val canvasEffects by viewModel.canvasEffects.collectAsStateWithLifecycle()
    val achievementToasts by viewModel.achievementToasts.collectAsStateWithLifecycle()
    val playerFlashUntil by viewModel.playerFlashUntil.collectAsStateWithLifecycle()

    val settings by ServiceLocator.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = ServiceLocator.settingsRepository.run {
            com.shadowcrypt.game.data.SettingsRepository.Settings()
        }
    )
    var showTutorial by remember { mutableStateOf(false) }
    var inspectedEnemy by remember { mutableStateOf<Enemy?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(settings.tutorialSeen, gameState) {
        if (!settings.tutorialSeen && gameState != null) {
            showTutorial = true
        }
    }

    // Android back button opens pause menu (or closes overlays)
    BackHandler {
        when {
            showInventory -> viewModel.closeInventory()
            showPauseMenu -> viewModel.closePause()
            else -> viewModel.togglePause()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
    ) {
        when {
            isLoading && gameState == null -> {
                // Initial load: show spinner
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DungeonPurple80)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating dungeon...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            gameState != null -> {
                val state = gameState!!

                LaunchedEffect(state.status) {
                    if (state.status == GameStatus.Dead ||
                        state.status == GameStatus.Victory
                    ) {
                        val lastMsgs = state.messageLog.takeLast(5).joinToString("\n")
                        onGameOver(
                            state.player.currentFloor,
                            state.enemiesKilled,
                            state.turnCount,
                            state.status == GameStatus.Victory,
                            lastMsgs
                        )
                    }
                }

                // Animation loop for floating texts and effects
                LaunchedEffect(Unit) {
                    while (true) {
                        withFrameMillis {
                            val now = System.currentTimeMillis()
                            viewModel.removeExpiredFloatingTexts(now)
                            viewModel.removeExpiredCanvasEffects(now)
                            viewModel.removeExpiredToasts(now)
                        }
                    }
                }

                // Dungeon canvas (fills screen, behind HUD)
                DungeonCanvas(
                    state = state,
                    floatingTexts = floatingTexts,
                    canvasEffects = canvasEffects,
                    playerFlashUntil = playerFlashUntil,
                    onTileTap = { viewModel.tapTile(it) },
                    onTileLongPress = { pos ->
                        val enemy = state.enemies.find {
                            it.position == pos && it.isAlive &&
                                    state.visibilityMap[pos] == Visibility.Visible
                        }
                        inspectedEnemy = enemy
                    },
                    onSwipeMove = { dx, dy ->
                        val dir = Direction.entries.find { it.dx == dx && it.dy == dy }
                        if (dir != null) viewModel.move(dir)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top HUD
                TopHud(
                    state = state,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Achievement toast banner
                AchievementToastBanner(
                    toasts = achievementToasts,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Message log above D-pad
                MessageLog(
                    messages = state.messageLog,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 180.dp)
                )

                // Top-right column: Pause + Undo buttons + Minimap
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HudBackground)
                                .clickable { viewModel.undoLastMove() }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UNDO",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HudBackground)
                                .clickable { viewModel.togglePause() }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PAUSE",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }

                    Minimap(
                        state = state,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    QuestTracker(
                        quests = state.quests,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Auto-explore button (bottom-left, above BAG)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudBackground)
                        .clickable { viewModel.autoExplore() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXPLORE",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                // Inventory button (bottom-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HudBackground)
                        .clickable { viewModel.toggleInventory() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BAG ${state.player.inventory.items.size}/${state.player.inventory.capacity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                // Skill bar (left of D-pad)
                SkillBar(
                    classId = state.player.classId,
                    cooldowns = state.player.skillCooldowns,
                    onSkill = { viewModel.useSkill(it) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 200.dp, bottom = 36.dp)
                )

                // D-pad (bottom-right)
                DpadOverlay(
                    onDirection = { viewModel.move(it) },
                    onCenter = { viewModel.descendStairs() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 24.dp)
                )

                // Inventory overlay
                if (showInventory) {
                    InventoryOverlay(
                        state = state,
                        onEquip = { viewModel.equipItem(it) },
                        onUnequip = { viewModel.unequipSlot(it) },
                        onUse = { viewModel.useItem(it) },
                        onDrop = { viewModel.dropItem(it) },
                        onClose = { viewModel.closeInventory() }
                    )
                }

                // Pause overlay
                if (showPauseMenu) {
                    PauseOverlay(
                        state = state,
                        onResume = { viewModel.closePause() },
                        onQuit = {
                            onGameOver(
                                state.player.currentFloor,
                                state.enemiesKilled,
                                state.turnCount,
                                false,
                                "You retreated from the dungeon."
                            )
                        }
                    )
                }

                // Tutorial overlay (first run)
                if (showTutorial) {
                    TutorialOverlay(
                        onDismiss = {
                            showTutorial = false
                            scope.launch {
                                ServiceLocator.settingsRepository.setTutorialSeen()
                            }
                        }
                    )
                }

                // Enemy inspection popup
                inspectedEnemy?.let { enemy ->
                    EnemyInspectPopup(
                        enemy = enemy,
                        onDismiss = { inspectedEnemy = null }
                    )
                }

                // Floor transition overlay (shown during floor descend)
                AnimatedVisibility(
                    visible = isLoading && gameState != null,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    FloorTransitionOverlay(
                        targetFloor = transitionFloor,
                        theme = transitionTheme
                    )
                }
            }
        }
    }
}
