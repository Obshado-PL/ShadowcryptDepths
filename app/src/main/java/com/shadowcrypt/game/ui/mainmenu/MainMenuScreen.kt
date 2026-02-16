package com.shadowcrypt.game.ui.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.VoidAccent

/**
 * Main menu screen for Shadowcrypt Depths.
 *
 * Shows the game title and primary navigation options:
 * - New Run: start a new dungeon crawl (goes to class selection)
 * - Unlocks: view permanent meta-progression
 * - Settings: sound, haptics, and accessibility options
 *
 * The visual style uses a dark gradient background with the game title
 * in a dramatic monospace font, setting the dungeon atmosphere immediately.
 */
@Composable
fun MainMenuScreen(
    onNewRun: () -> Unit,
    onContinue: (() -> Unit)? = null,
    onDailyChallenge: () -> Unit,
    onSettings: () -> Unit,
    onUnlocks: () -> Unit,
    onRunHistory: () -> Unit
) {
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
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ===== Game Title =====
            Text(
                text = "SHADOWCRYPT",
                style = MaterialTheme.typography.displayLarge,
                color = DungeonPurple80,
                textAlign = TextAlign.Center
            )
            Text(
                text = "DEPTHS",
                style = MaterialTheme.typography.displayMedium,
                color = DungeonAmber80,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "A Roguelike Dungeon Crawler",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            // ===== Menu Buttons =====

            // Continue button (only shown if save exists)
            if (onContinue != null) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .width(220.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DungeonAmber80.copy(alpha = 0.6f),
                        contentColor = Color(0xFF1A1000)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "CONTINUE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // New Run button (primary action)
            Button(
                onClick = onNewRun,
                modifier = Modifier
                    .width(220.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DungeonPurple80.copy(alpha = 0.6f),
                    contentColor = Color(0xFF1A0A30)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "NEW RUN",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Daily Challenge button
            Button(
                onClick = onDailyChallenge,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DungeonAmber80.copy(alpha = 0.6f),
                    contentColor = Color(0xFF1A1000)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "DAILY CHALLENGE",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unlocks button
            OutlinedButton(
                onClick = onUnlocks,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "UNLOCKS",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Run History button
            OutlinedButton(
                onClick = onRunHistory,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RUN HISTORY",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings button
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Version info
            Text(
                text = "v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
