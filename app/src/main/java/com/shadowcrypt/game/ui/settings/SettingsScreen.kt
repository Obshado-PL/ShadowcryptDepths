package com.shadowcrypt.game.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.SettingsRepository
import com.shadowcrypt.game.model.Difficulty
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.VoidAccent
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = SettingsRepository.Settings()
    )
    val scope = rememberCoroutineScope()

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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "< BACK",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineLarge,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Audio section
            SectionContainer(title = "AUDIO") {
                ToggleRow(
                    label = "Sound Effects",
                    checked = settings.soundEnabled,
                    onCheckedChange = {
                        scope.launch { ServiceLocator.settingsRepository.setSoundEnabled(it) }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = settings.soundVolume,
                        onValueChange = {
                            scope.launch { ServiceLocator.settingsRepository.setSoundVolume(it) }
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = DungeonPurple80,
                            activeTrackColor = DungeonPurple80,
                            inactiveTrackColor = HudBackground
                        ),
                        enabled = settings.soundEnabled
                    )
                    Text(
                        text = "${(settings.soundVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Haptics section
            SectionContainer(title = "HAPTICS") {
                ToggleRow(
                    label = "Vibration Feedback",
                    checked = settings.hapticEnabled,
                    onCheckedChange = {
                        scope.launch { ServiceLocator.settingsRepository.setHapticEnabled(it) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Difficulty section
            SectionContainer(title = "DIFFICULTY") {
                Text(
                    text = "Applies to new runs only",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (diff in Difficulty.entries) {
                        val selected = settings.difficulty == diff.name
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) DungeonPurple80.copy(alpha = 0.3f)
                                    else HudBackground
                                )
                                .then(
                                    if (selected) Modifier.border(
                                        1.dp, DungeonPurple80, RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    scope.launch {
                                        ServiceLocator.settingsRepository.setDifficulty(diff.name)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = diff.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) DungeonPurple80 else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Accessibility section
            SectionContainer(title = "ACCESSIBILITY") {
                Text(
                    text = "Font Scale: ${String.format("%.1f", settings.fontScale)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = settings.fontScale,
                    onValueChange = {
                        scope.launch { ServiceLocator.settingsRepository.setFontScale(it) }
                    },
                    valueRange = 0.8f..1.5f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = DungeonPurple80,
                        activeTrackColor = DungeonPurple80,
                        inactiveTrackColor = HudBackground
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                ToggleRow(
                    label = "High Contrast Mode",
                    checked = settings.highContrastMode,
                    onCheckedChange = {
                        scope.launch { ServiceLocator.settingsRepository.setHighContrast(it) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionContainer(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HudBackground)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = DungeonAmber80
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DungeonPurple80,
                checkedTrackColor = DungeonPurple80.copy(alpha = 0.3f)
            )
        )
    }
}
