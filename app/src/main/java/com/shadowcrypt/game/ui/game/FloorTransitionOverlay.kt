package com.shadowcrypt.game.ui.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.FloorTheme
import com.shadowcrypt.game.ui.theme.CavernAccent
import com.shadowcrypt.game.ui.theme.CryptAccent
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.InfernoAccent
import com.shadowcrypt.game.ui.theme.SewerAccent
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.VoidAccent

@Composable
fun FloorTransitionOverlay(
    targetFloor: Int,
    theme: FloorTheme?,
    modifier: Modifier = Modifier
) {
    val themeColor = when (theme) {
        FloorTheme.Crypt -> CryptAccent
        FloorTheme.Sewers -> SewerAccent
        FloorTheme.Caverns -> CavernAccent
        FloorTheme.Inferno -> InfernoAccent
        FloorTheme.Void -> VoidAccent
        null -> DungeonPurple80
    }

    val infiniteTransition = rememberInfiniteTransition(label = "transition")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dots"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "\u2B07\uFE0F",
                style = MaterialTheme.typography.displayMedium,
                color = themeColor.copy(alpha = dotAlpha)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Descending...",
                style = MaterialTheme.typography.headlineSmall,
                color = themeColor
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Floor $targetFloor",
                style = MaterialTheme.typography.displaySmall,
                color = themeColor.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = theme?.displayName ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.5f)
            )
        }
    }
}
