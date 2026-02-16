package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.TextPrimary


@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground.copy(alpha = 0.92f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GameBackground)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "HOW TO PLAY",
                style = MaterialTheme.typography.titleLarge,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(4.dp))

            HintRow("D-PAD", "Move in 4 directions")
            HintRow("SWIPE", "Swipe on the map to move")
            HintRow("TAP TILE", "Walk to that tile or attack adjacent enemies")
            HintRow("LONG-PRESS", "Hold on an enemy to see its stats")
            HintRow("CENTER BTN", "Descend stairs when standing on them")
            HintRow("BAG", "Open inventory to equip/use items")
            HintRow("AUTO-WALK", "Tap far away to walk automatically")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap anywhere to begin",
                style = MaterialTheme.typography.bodySmall,
                color = DungeonAmber80,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HintRow(label: String, description: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary.copy(alpha = 0.7f)
        )
    }
}
