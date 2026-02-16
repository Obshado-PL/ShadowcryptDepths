package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.Direction
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary

@Composable
fun DpadOverlay(
    onDirection: (Direction) -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 56.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DpadButton(
                label = "\u25B2",
                size = buttonSize,
                onClick = { onDirection(Direction.Up) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DpadButton(
                    label = "\u25C0",
                    size = buttonSize,
                    onClick = { onDirection(Direction.Left) }
                )

                Box(
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(DungeonPurple80.copy(alpha = 0.3f))
                        .clickable { onCenter() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u25CF",
                        style = MaterialTheme.typography.bodyLarge,
                        color = DungeonPurple80
                    )
                }

                DpadButton(
                    label = "\u25B6",
                    size = buttonSize,
                    onClick = { onDirection(Direction.Right) }
                )
            }

            DpadButton(
                label = "\u25BC",
                size = buttonSize,
                onClick = { onDirection(Direction.Down) }
            )
        }
    }
}

@Composable
private fun DpadButton(
    label: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(HudBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}
