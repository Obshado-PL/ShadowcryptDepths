package com.shadowcrypt.game.ui.unlocks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.data.model.UnlockCondition
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold

@Composable
fun UnlocksScreen(
    onBack: () -> Unit,
    viewModel: UnlocksViewModel = viewModel()
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "UNLOCKS",
                style = MaterialTheme.typography.headlineMedium,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Total Runs: ${progress.totalRuns}  |  Best Floor: ${progress.bestFloor}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            for (condition in UnlockCondition.entries) {
                val isUnlocked = condition.classId in progress.unlockedClassIds
                val borderColor = if (isUnlocked) DungeonPurple80 else TextSecondary.copy(alpha = 0.2f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .background(
                            if (isUnlocked) DungeonPurple80.copy(alpha = 0.05f) else HudBackground,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = condition.displayName.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUnlocked) DungeonPurple80 else TextSecondary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnlocked) RarityUncommon else HealthRed.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = condition.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnlocked) TextSecondary else XpGold.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .width(220.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "BACK",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
