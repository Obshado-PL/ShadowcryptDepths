package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.TextPrimary

data class AchievementToast(
    val id: String,
    val title: String,
    val emoji: String,
    val createdAtMs: Long,
    val durationMs: Long = 3000L
) {
    fun isExpired(now: Long): Boolean = now - createdAtMs >= durationMs
    fun progress(now: Long): Float =
        ((now - createdAtMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

object AchievementDefs {
    data class AchievementDef(
        val id: String,
        val title: String,
        val emoji: String,
        val checkInGame: (old: GameState, new: GameState) -> Boolean
    )

    val inGameAchievements = listOf(
        AchievementDef(
            id = "first_blood",
            title = "First Blood",
            emoji = "\u2694\uFE0F",
            checkInGame = { old, new -> old.enemiesKilled == 0 && new.enemiesKilled > 0 }
        ),
        AchievementDef(
            id = "floor_5",
            title = "Halfway There",
            emoji = "\u2B07\uFE0F",
            checkInGame = { old, new ->
                old.player.currentFloor < 5 && new.player.currentFloor >= 5
            }
        ),
        AchievementDef(
            id = "floor_10",
            title = "Deep Dweller",
            emoji = "\uD83C\uDF11",
            checkInGame = { old, new ->
                old.player.currentFloor < 10 && new.player.currentFloor >= 10
            }
        )
    )
}

@Composable
fun AchievementToastBanner(
    toasts: List<AchievementToast>,
    modifier: Modifier = Modifier
) {
    if (toasts.isEmpty()) return

    Column(
        modifier = modifier
            .padding(top = 80.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (toast in toasts) {
            val now = System.currentTimeMillis()
            val progress = toast.progress(now)

            val alpha = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.8f -> (1f - progress) / 0.2f
                else -> 1f
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A0A30).copy(alpha = 0.9f * alpha))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = toast.emoji,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White.copy(alpha = alpha)
                )
                Column {
                    Text(
                        text = "ACHIEVEMENT UNLOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        color = DungeonAmber80.copy(alpha = alpha)
                    )
                    Text(
                        text = toast.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = alpha)
                    )
                }
                Text(
                    text = "\uD83C\uDFC6",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DungeonAmber80.copy(alpha = alpha)
                )
            }
        }
    }
}
