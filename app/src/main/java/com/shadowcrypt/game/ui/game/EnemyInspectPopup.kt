package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.AiBehavior
import com.shadowcrypt.game.model.Enemy
import com.shadowcrypt.game.ui.theme.BossColor
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.EnemyColor
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HealthRedDark
import com.shadowcrypt.game.ui.theme.TextPrimary

@Composable
fun EnemyInspectPopup(
    enemy: Enemy,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .clip(RoundedCornerShape(12.dp))
                .background(GameBackground)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            // Enemy name + emoji
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = enemyEmoji(enemy.typeId),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = enemy.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            enemy.isBoss -> BossColor
                            enemy.isElite -> Color(0xFFFF8C00)
                            else -> EnemyColor
                        }
                    )
                    // Badges
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (enemy.isBoss) {
                            Badge("BOSS", BossColor)
                        }
                        if (enemy.isElite) {
                            Badge("ELITE", Color(0xFFFF8C00))
                        }
                        if (enemy.alertedByPlayer) {
                            Badge("ALERTED", Color.Yellow)
                        }
                        if (enemy.isStunned) {
                            Badge("STUNNED", Color(0xFF88AAFF))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // HP bar
            Text(
                text = "HP ${enemy.hp}/${enemy.maxHp}",
                style = MaterialTheme.typography.bodySmall,
                color = HealthRed
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(HealthRedDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(enemy.hpFraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(HealthRed)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock("ATK", enemy.effectiveAtk, if (enemy.effectiveAtk > enemy.atk) DungeonAmber80 else TextPrimary)
                StatBlock("DEF", enemy.effectiveDef, if (enemy.effectiveDef > enemy.def) DungeonAmber80 else TextPrimary)
                StatBlock("MAG", enemy.mag, TextPrimary)
                StatBlock("SPD", enemy.effectiveSpd, if (enemy.effectiveSpd > enemy.spd) DungeonAmber80 else TextPrimary)
            }

            Spacer(Modifier.height(12.dp))

            // Behavior
            val behaviorText = when (enemy.behavior) {
                AiBehavior.Aggressive -> "Aggressive - charges at you"
                AiBehavior.Patrol -> "Patrol - wanders until alerted"
                AiBehavior.Ranged -> "Ranged - attacks from distance"
                AiBehavior.Ambush -> "Ambush - waits to surprise"
                AiBehavior.Support -> "Support - buffs allies"
                AiBehavior.Boss -> "Boss - powerful and relentless"
            }
            Text(
                text = behaviorText,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.7f)
            )

            // Active buffs
            if (enemy.activeBuffs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Active Effects:",
                    style = MaterialTheme.typography.labelSmall,
                    color = DungeonPurple80
                )
                for (buff in enemy.activeBuffs) {
                    val label = buildString {
                        if (buff.atkBonus != 0) append("ATK+${buff.atkBonus} ")
                        if (buff.defBonus != 0) append("DEF+${buff.defBonus} ")
                        if (buff.spdBonus != 0) append("SPD+${buff.spdBonus} ")
                        append("(${buff.turnsRemaining} turns)")
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = DungeonAmber80
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Tap anywhere to dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun StatBlock(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary.copy(alpha = 0.5f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
    }
}
