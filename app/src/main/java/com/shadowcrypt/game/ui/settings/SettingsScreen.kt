package com.shadowcrypt.game.ui.settings

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineMedium,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(32.dp))

            SettingsToggleRow(
                label = "Sound Effects",
                checked = settings.soundEnabled,
                onToggle = { viewModel.toggleSound() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggleRow(
                label = "Background Music",
                checked = settings.musicEnabled,
                onToggle = { viewModel.toggleMusic() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsToggleRow(
                label = "Haptic Feedback",
                checked = settings.hapticsEnabled,
                onToggle = { viewModel.toggleHaptics() }
            )

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

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HudBackground, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = DungeonPurple80,
                checkedTrackColor = DungeonPurple80.copy(alpha = 0.3f),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = TextSecondary.copy(alpha = 0.2f)
            )
        )
    }
}
