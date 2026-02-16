package com.shadowcrypt.game.ui.runhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunHistoryScreen(
    onBack: () -> Unit
) {
    val progress by ServiceLocator.metaProgressRepository.progress.collectAsStateWithLifecycle(
        initialValue = MetaProgressRepository.MetaProgress()
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUN HISTORY",
                style = MaterialTheme.typography.titleLarge,
                color = DungeonPurple80
            )
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BACK", color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Summary stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(HudBackground)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn("Runs", "${progress.totalRuns}")
            StatColumn("Best Floor", "${progress.bestFloor}")
            StatColumn("High Score", "${progress.highScore}")
            StatColumn("Victories", "${progress.victories}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (progress.runHistory.isEmpty()) {
            Text(
                text = "No runs recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            for (run in progress.runHistory) {
                RunEntry(run)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun RunEntry(run: MetaProgressRepository.RunRecord) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(run.timestamp))
    val resultColor = if (run.won) DungeonAmber80 else HealthRed
    val resultText = if (run.won) "VICTORY" else "F${run.floorReached}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(HudBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = resultText,
                style = MaterialTheme.typography.labelLarge,
                color = resultColor
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Score: ${run.score}",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            Text(
                text = "${run.enemiesKilled} kills, ${run.turnsTaken} turns",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
