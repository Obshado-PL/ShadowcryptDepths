package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.engine.model.Direction
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HealthRedDark
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.StairsColor
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold
import com.shadowcrypt.game.ui.theme.XpGoldDark
import kotlin.math.abs

private val DPAD_BUTTON_SIZE = 56.dp
private val DPAD_GAP = 4.dp
private const val SWIPE_THRESHOLD = 50f
private const val INPUT_DEBOUNCE_MS = 100L

@Composable
fun GameHud(
    floorNumber: Int,
    hp: Int,
    maxHp: Int,
    level: Int,
    xp: Int,
    xpToNextLevel: Int,
    classId: String,
    message: String?,
    effectiveAttack: Int = 0,
    effectiveDefense: Int = 0,
    turnCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HudBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "F$floorNumber",
                style = MaterialTheme.typography.bodySmall,
                color = DungeonAmber80
            )
            Text(
                text = "T$turnCount",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = "Lv$level",
                style = MaterialTheme.typography.bodySmall,
                color = XpGold
            )
            // HP bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HP",
                    style = MaterialTheme.typography.labelSmall,
                    color = HealthRed
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 8.dp)
                        .background(HealthRedDark, RoundedCornerShape(4.dp))
                ) {
                    val fraction = if (maxHp > 0) hp.toFloat() / maxHp else 0f
                    Box(
                        modifier = Modifier
                            .size(width = 60.dp * fraction, height = 8.dp)
                            .background(HealthRed, RoundedCornerShape(4.dp))
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$hp/$maxHp",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            // XP bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = XpGold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 8.dp)
                        .background(XpGoldDark, RoundedCornerShape(4.dp))
                ) {
                    val xpFraction = if (xpToNextLevel > 0) xp.toFloat() / xpToNextLevel else 0f
                    Box(
                        modifier = Modifier
                            .size(width = 50.dp * xpFraction.coerceAtMost(1f), height = 8.dp)
                            .background(XpGold, RoundedCornerShape(4.dp))
                    )
                }
            }
            Text(
                text = "ATK:$effectiveAttack",
                style = MaterialTheme.typography.labelSmall,
                color = RarityEpic
            )
            Text(
                text = "DEF:$effectiveDefense",
                style = MaterialTheme.typography.labelSmall,
                color = RarityRare
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DpadOverlay(
    onDirection: (Direction) -> Unit,
    onWait: () -> Unit,
    onDescend: () -> Unit,
    canDescend: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Up button
        DpadButton(text = "\u25B2", onClick = { onDirection(Direction.UP) })

        Row(
            horizontalArrangement = Arrangement.spacedBy(DPAD_GAP)
        ) {
            // Left
            DpadButton(text = "\u25C0", onClick = { onDirection(Direction.LEFT) })
            // Center: descend or wait
            DpadButton(
                text = if (canDescend) "\u25BC\u25BC" else "\u00B7",
                onClick = { if (canDescend) onDescend() else onWait() },
                highlight = canDescend
            )
            // Right
            DpadButton(text = "\u25B6", onClick = { onDirection(Direction.RIGHT) })
        }

        // Down button
        DpadButton(text = "\u25BC", onClick = { onDirection(Direction.DOWN) })
    }
}

@Composable
private fun DpadButton(
    text: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(DPAD_BUTTON_SIZE),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlight) StairsColor.copy(alpha = 0.4f)
            else GameBackground.copy(alpha = 0.7f),
            contentColor = if (highlight) StairsColor else TextPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun SwipeDetector(
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var lastInputTime by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures { _, dragAmount ->
                val now = System.currentTimeMillis()
                if (now - lastInputTime < INPUT_DEBOUNCE_MS) return@detectDragGestures
                val (dx, dy) = dragAmount
                if (abs(dx) < SWIPE_THRESHOLD && abs(dy) < SWIPE_THRESHOLD) return@detectDragGestures

                lastInputTime = now
                if (abs(dx) > abs(dy)) {
                    onDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                } else {
                    onDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                }
            }
        }
    ) {
        content()
    }
}
