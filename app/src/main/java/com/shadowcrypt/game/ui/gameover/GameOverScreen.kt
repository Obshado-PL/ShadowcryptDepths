package com.shadowcrypt.game.ui.gameover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary

import com.shadowcrypt.game.ui.theme.VoidAccent

@Composable
fun GameOverScreen(
    floorReached: Int,
    enemiesKilled: Int,
    turnsTaken: Int,
    score: Int,
    won: Boolean,
    lastMessages: String = "",
    onNewRun: () -> Unit,
    onTryAgain: () -> Unit
) {
    // Save run stats to meta-progression
    LaunchedEffect(Unit) {
        ServiceLocator.metaProgressRepository.recordRun(
            floorReached = floorReached,
            enemiesKilled = enemiesKilled,
            turnsTaken = turnsTaken,
            score = score,
            won = won
        )
    }

    val titleColor = if (won) DungeonAmber80 else HealthRed
    val titleText = if (won) "VICTORY" else "DEFEATED"
    val subtitleText = if (won) "You conquered the Shadowcrypt!" else "The darkness consumes you..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GameBackground,
                        VoidAccent.copy(alpha = 0.15f),
                        GameBackground
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.displayMedium,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RUN STATISTICS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DungeonPurple80
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatRow("Floor Reached", "$floorReached / 10")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Enemies Slain", "$enemiesKilled")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Turns Taken", "$turnsTaken")
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DungeonAmber80
                    )
                }
            }

            // Death recap
            if (lastMessages.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(HudBackground)
                        .padding(16.dp)
                ) {
                    Text(
                        text = if (won) "FINAL MOMENTS" else "DEATH RECAP",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (won) DungeonAmber80 else HealthRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    for (line in lastMessages.split("\n")) {
                        if (line.isNotBlank()) {
                            Text(
                                text = "> $line",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Buttons
            Button(
                onClick = onTryAgain,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DungeonPurple80.copy(alpha = 0.6f),
                    contentColor = Color(0xFF1A0A30)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "TRY AGAIN",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNewRun,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "MAIN MENU",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
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
            color = TextPrimary.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}
