package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary

@Composable
fun PauseOverlay(
    state: GameState,
    onResume: () -> Unit,
    onQuit: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PAUSED",
                style = MaterialTheme.typography.displayMedium,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Run stats summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudBackground)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CURRENT RUN",
                    style = MaterialTheme.typography.labelLarge,
                    color = DungeonPurple80
                )

                Spacer(modifier = Modifier.height(12.dp))

                StatRow("Floor", "${state.player.currentFloor} / 10")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Enemies Slain", "${state.enemiesKilled}")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Turns", "${state.turnCount}")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("Level", "${state.player.level}")
                Spacer(modifier = Modifier.height(6.dp))
                StatRow("HP", "${state.player.hp} / ${state.player.effectiveMaxHp}")
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (!showConfirm) {
                // Normal pause buttons
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .width(220.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DungeonPurple80.copy(alpha = 0.3f),
                        contentColor = DungeonPurple80
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showConfirm = true },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "QUIT TO MENU",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            } else {
                // Confirmation state
                Text(
                    text = "ARE YOU SURE?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = HealthRed
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Your progress will be lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onQuit,
                    modifier = Modifier
                        .width(220.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthRed.copy(alpha = 0.3f),
                        contentColor = HealthRed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "YES, QUIT",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showConfirm = false },
                    modifier = Modifier
                        .width(220.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
