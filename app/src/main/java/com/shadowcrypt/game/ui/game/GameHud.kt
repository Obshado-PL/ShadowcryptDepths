package com.shadowcrypt.game.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.Position
import com.shadowcrypt.game.model.Quest
import com.shadowcrypt.game.model.QuestType
import com.shadowcrypt.game.model.RoomType
import com.shadowcrypt.game.model.Tile
import com.shadowcrypt.game.model.Visibility
import kotlin.math.min
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HealthRedDark
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TextPrimary

import com.shadowcrypt.game.ui.theme.XpGold
import com.shadowcrypt.game.ui.theme.XpGoldDark

@Composable
fun TopHud(
    state: GameState,
    onUndo: () -> Unit = {},
    onPause: () -> Unit = {},
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

    val animatedHpFraction by animateFloatAsState(
        targetValue = state.player.hpFraction,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "hpBar"
    )
    val animatedXpFraction by animateFloatAsState(
        targetValue = state.player.xpFraction,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "xpBar"
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
                        .fillMaxWidth(animatedHpFraction.coerceIn(0f, 1f))
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
                        .fillMaxWidth(animatedXpFraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(XpGold)
                )
            }

            // Torch & Hunger indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val torchFraction = state.player.torchFuel.toFloat() / state.player.maxTorchFuel
                    val animatedTorchFraction by animateFloatAsState(
                        targetValue = torchFraction,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "torchBar"
                    )
                    val torchColor = when {
                        torchFraction > 0.5f -> Color(0xFFFFAA00)
                        torchFraction > 0.2f -> Color(0xFFFF7700)
                        else -> Color(0xFFFF3300)
                    }
                    Text(
                        text = "\uD83D\uDD25",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF333333))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedTorchFraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(torchColor)
                        )
                    }
                }
                // Hunger bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hungerFraction = state.player.hunger.toFloat() / state.player.maxHunger
                    val animatedHungerFraction by animateFloatAsState(
                        targetValue = hungerFraction,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                        label = "hungerBar"
                    )
                    val hungerColor = when {
                        hungerFraction > 0.5f -> Color(0xFF66BB6A)
                        hungerFraction > 0.2f -> Color(0xFFFF9800)
                        else -> Color(0xFFFF3300)
                    }
                    Text(
                        text = "\uD83C\uDF56",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF333333))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedHungerFraction.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(2.dp))
                                .background(hungerColor)
                        )
                    }
                }
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
            if (state.isDaily) {
                Text(
                    text = "DAILY",
                    style = MaterialTheme.typography.labelSmall,
                    color = DungeonAmber80,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DungeonAmber80.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(2.dp))
            }
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

        Column(horizontalAlignment = Alignment.End) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DungeonPurple80.copy(alpha = 0.15f))
                        .clickable { onUndo() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                        .background(DungeonPurple80.copy(alpha = 0.15f))
                        .clickable { onPause() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PAUSE",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
            Text(
                text = "T:${state.turnCount}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
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
                    TextPrimary else TextPrimary.copy(alpha = 0.5f),
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

    // Pre-compute room type lookup for special rooms
    val roomTypeAt = remember(dungeon) {
        HashMap<Long, RoomType>().also { map ->
            for (room in dungeon.rooms) {
                if (room.type == RoomType.Normal) continue
                for (ry in room.y..room.bottom) {
                    for (rx in room.x..room.right) {
                        map[rx.toLong() shl 32 or ry.toLong().and(0xFFFFFFFFL)] = room.type
                    }
                }
            }
        }
    }

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
                        else -> {
                            val roomType = roomTypeAt[col.toLong() shl 32 or row.toLong().and(0xFFFFFFFFL)]
                            when (roomType) {
                                RoomType.TreasureVault -> Color(0xFF665500)
                                RoomType.Arena -> Color(0xFF553333)
                                RoomType.ShrineRoom -> Color(0xFF335555)
                                RoomType.Library -> Color(0xFF333355)
                                RoomType.Armory -> Color(0xFF554433)
                                RoomType.TrapGauntlet -> Color(0xFF555533)
                                else -> Color(0xFF333333)
                            }
                        }
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

@Composable
fun QuestTracker(
    quests: List<Quest>,
    modifier: Modifier = Modifier
) {
    if (quests.isEmpty()) return

    Column(
        modifier = modifier
            .widthIn(max = 160.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(HudBackground)
            .padding(8.dp)
    ) {
        Text(
            text = "QUESTS",
            style = MaterialTheme.typography.labelSmall,
            color = DungeonPurple80
        )
        Spacer(Modifier.height(4.dp))

        for ((index, quest) in quests.withIndex()) {
            QuestRow(quest)
            if (index < quests.lastIndex) {
                Spacer(Modifier.height(3.dp))
            }
        }
    }
}

@Composable
private fun QuestRow(quest: Quest) {
    val icon = when (quest.type) {
        QuestType.KillEnemies -> "\u2694\uFE0F"
        QuestType.KillBoss -> "\uD83D\uDC80"
        QuestType.FindItem -> "\uD83D\uDCE6"
        QuestType.ReachStairs -> "\u2B07\uFE0F"
        QuestType.SurviveTurns -> "\u23F0"
    }

    val textColor = if (quest.completed) Color(0xFF66BB6A) else TextPrimary.copy(alpha = 0.8f)
    val progressColor = if (quest.completed) Color(0xFF66BB6A) else DungeonPurple80

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (quest.completed) "\u2705" else icon,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(progressColor.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(quest.progressFraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(1.dp))
                        .background(progressColor)
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "${quest.progress}/${quest.targetCount}",
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}
