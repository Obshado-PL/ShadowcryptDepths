package com.shadowcrypt.game.ui.unlocks

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.model.EnemyTypes
import kotlinx.coroutines.launch
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.VoidAccent

private data class AchievementDef(
    val id: String,
    val name: String,
    val description: String
)

private val achievements = listOf(
    AchievementDef("first_blood", "First Blood", "Slay your first enemy"),
    AchievementDef("floor_5", "Halfway There", "Reach floor 5"),
    AchievementDef("floor_10", "Deep Dweller", "Reach floor 10"),
    AchievementDef("slayer_100", "Centurion", "Slay 100 enemies total"),
    AchievementDef("first_victory", "Conqueror", "Win the game"),
    AchievementDef("runs_10", "Veteran", "Complete 10 runs")
)

private data class UpgradeDef(val stat: String, val label: String, val icon: String)

@Composable
fun UnlocksScreen(onBack: () -> Unit) {
    val progress by ServiceLocator.metaProgressRepository.progress.collectAsStateWithLifecycle(
        initialValue = MetaProgressRepository.MetaProgress()
    )
    val scope = rememberCoroutineScope()
    val repo = ServiceLocator.metaProgressRepository

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
                text = "UNLOCKS",
                style = MaterialTheme.typography.headlineLarge,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HudBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "STATISTICS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DungeonAmber80
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatRow("Total Runs", "${progress.totalRuns}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Total Kills", "${progress.totalKills}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Total Floors", "${progress.totalFloors}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Best Floor", "${progress.bestFloor}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("High Score", "${progress.highScore}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Victories", "${progress.victories}")
                Spacer(modifier = Modifier.height(8.dp))
                StatRow("Soul Gems", "${progress.soulGems}")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permanent Upgrades
            Text(
                text = "PERMANENT UPGRADES",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonAmber80
            )
            Spacer(modifier = Modifier.height(12.dp))

            val upgrades = listOf(
                UpgradeDef("hp", "HP +5", "HP"),
                UpgradeDef("atk", "ATK +1", "ATK"),
                UpgradeDef("def", "DEF +1", "DEF"),
                UpgradeDef("mag", "MAG +1", "MAG"),
                UpgradeDef("spd", "SPD +1", "SPD")
            )
            for (upgrade in upgrades) {
                val level = when (upgrade.stat) {
                    "hp" -> progress.upgradeHp
                    "atk" -> progress.upgradeAtk
                    "def" -> progress.upgradeDef
                    "mag" -> progress.upgradeMag
                    "spd" -> progress.upgradeSpd
                    else -> 0
                }
                val cost = repo.upgradeCost(level)
                val canAfford = progress.soulGems >= cost && level < 10
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HudBackground)
                        .then(
                            if (canAfford) Modifier.clickable {
                                scope.launch { repo.purchaseUpgrade(upgrade.stat) }
                            } else Modifier
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${upgrade.icon} ${upgrade.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (level >= 10) "MAX" else "Lv$level  [$cost gems]",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (canAfford) DungeonAmber80 else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bestiary
            Text(
                text = "BESTIARY",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonAmber80
            )
            Spacer(modifier = Modifier.height(12.dp))

            val allEnemies = EnemyTypes.all
            Text(
                text = "${progress.discoveredEnemies.size} / ${allEnemies.size} Discovered",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            for (enemy in allEnemies) {
                val discovered = enemy.id in progress.discoveredEnemies
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(HudBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (discovered) {
                        Text(
                            text = enemy.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "HP:${enemy.baseHp} ATK:${enemy.baseAtk} DEF:${enemy.baseDef}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "???",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ACHIEVEMENTS",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonAmber80
            )

            Spacer(modifier = Modifier.height(12.dp))

            val unlockedCount = progress.unlockedAchievements.size
            Text(
                text = "$unlockedCount / ${achievements.size} Unlocked",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            for (achievement in achievements) {
                val unlocked = achievement.id in progress.unlockedAchievements
                AchievementCard(
                    name = achievement.name,
                    description = if (unlocked) achievement.description else "???",
                    unlocked = unlocked
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
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

@Composable
private fun AchievementCard(
    name: String,
    description: String,
    unlocked: Boolean
) {
    val accentColor = if (unlocked) DungeonAmber80 else TextSecondary.copy(alpha = 0.4f)
    val textColor = if (unlocked) TextPrimary else TextSecondary.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HudBackground)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Achievement icon (checkmark or lock)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = if (unlocked) 0.2f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (unlocked) "\u2713" else "?",
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unlocked) accentColor else textColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
    }
}
