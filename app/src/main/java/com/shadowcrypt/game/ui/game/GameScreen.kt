package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.engine.GameAction
import com.shadowcrypt.game.engine.model.Tile
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold

@Composable
fun GameScreen(
    onGameOver: () -> Unit = {},
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

                SwipeDetector(
                    onDirection = { dir -> viewModel.onAction(GameAction.Move(dir)) },
                    modifier = Modifier.fillMaxSize()
                ) {
                    DungeonCanvas(
                        gameState = gameState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                GameHud(
                    floorNumber = gameState.player.floorNumber,
                    hp = gameState.player.hp,
                    maxHp = gameState.player.maxHp,
                    level = gameState.player.level,
                    xp = gameState.player.xp,
                    xpToNextLevel = gameState.player.xpToNextLevel,
                    classId = gameState.player.classId,
                    message = gameState.message,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                DpadOverlay(
                    onDirection = { dir -> viewModel.onAction(GameAction.Move(dir)) },
                    onWait = { viewModel.onAction(GameAction.Wait) },
                    onDescend = { viewModel.onAction(GameAction.DescendStairs) },
                    canDescend = canDescend,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
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
                            text = "YOU HAVE FALLEN",
                            style = MaterialTheme.typography.displayMedium,
                            color = HealthRed
                        )

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
