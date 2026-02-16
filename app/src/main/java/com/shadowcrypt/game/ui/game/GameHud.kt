package com.shadowcrypt.game.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.math.min
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HealthRedDark
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold
import com.shadowcrypt.game.ui.theme.XpGoldDark

@Composable
fun TopHud(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val isLowHp = state.player.hpFraction < 0.25f
    val hpBarColor by animateColorAsState(
        targetValue = if (isLowHp) Color(0xFFFF4444) else HealthRed,
        animationSpec = if (isLowHp) {
            infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "hpPulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HudBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "HP ${state.player.hp}/${state.player.effectiveMaxHp}",
                style = MaterialTheme.typography.bodySmall,
                color = hpBarColor
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HealthRedDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.player.hpFraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(hpBarColor)
                )
            }
            Text(
                text = "Lv${state.player.level} ${state.player.xp}/${state.player.xpToNext}XP",
                style = MaterialTheme.typography.bodySmall,
                color = XpGold
            )
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(XpGoldDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.player.xpFraction)
                        .clip(RoundedCornerShape(3.dp))
                        .background(XpGold)
                )
            }

            // Active buff pills
            if (state.player.activeBuffs.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (buff in state.player.activeBuffs) {
                        val label = buildString {
                            if (buff.atkBonus != 0) append("ATK+${buff.atkBonus} ")
                            if (buff.defBonus != 0) append("DEF+${buff.defBonus} ")
                            if (buff.magBonus != 0) append("MAG+${buff.magBonus} ")
                            if (buff.spdBonus != 0) append("SPD+${buff.spdBonus} ")
                            append("(${buff.turnsRemaining})")
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = DungeonPurple80,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DungeonPurple80.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }

        // Floor progress column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "F${state.player.currentFloor}/10 ${state.dungeon.theme.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DungeonPurple80.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(state.player.currentFloor / 10f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DungeonPurple80)
                )
            }
        }

        Text(
            text = "T:${state.turnCount}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
fun MessageLog(
    messages: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HudBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val displayMessages = messages.takeLast(3)
        displayMessages.forEachIndexed { index, msg ->
            Text(
                text = "> $msg",
                style = MaterialTheme.typography.bodySmall,
                color = if (index == displayMessages.lastIndex)
                    TextPrimary else TextSecondary.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun Minimap(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val dungeon = state.dungeon

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HudBackground)
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.size(100.dp)) {
            val pxPerTile = min(size.width / dungeon.width, size.height / dungeon.height)

            for (row in 0 until dungeon.height) {
                for (col in 0 until dungeon.width) {
                    val pos = Position(col, row)
                    val vis = state.visibilityMap[pos] ?: Visibility.Hidden
                    if (vis == Visibility.Hidden) continue

                    val tile = dungeon.grid[row][col]
                    val color = when {
                        tile == Tile.Wall -> Color(0xFF555555)
                        tile == Tile.StairsDown || tile == Tile.StairsUp -> StairsColor
                        else -> Color(0xFF333333)
                    }
                    val dimmed = if (vis == Visibility.Explored) 0.5f else 1f

                    drawRect(
                        color = color.copy(alpha = dimmed),
                        topLeft = Offset(col * pxPerTile, row * pxPerTile),
                        size = Size(pxPerTile, pxPerTile)
                    )
                }
            }

            // Draw visible enemies
            for (enemy in state.enemies) {
                if (!enemy.isAlive) continue
                if (state.visibilityMap[enemy.position] != Visibility.Visible) continue
                drawRect(
                    color = EnemyColor,
                    topLeft = Offset(
                        enemy.position.x * pxPerTile,
                        enemy.position.y * pxPerTile
                    ),
                    size = Size(pxPerTile, pxPerTile)
                )
            }

            // Draw player
            drawRect(
                color = PlayerColor,
                topLeft = Offset(
                    state.player.position.x * pxPerTile,
                    state.player.position.y * pxPerTile
                ),
                size = Size(pxPerTile, pxPerTile)
            )
        }
    }
}
