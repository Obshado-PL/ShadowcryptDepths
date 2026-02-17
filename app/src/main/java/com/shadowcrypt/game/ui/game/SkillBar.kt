package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowcrypt.game.model.Skill
import com.shadowcrypt.game.model.SkillTarget
import com.shadowcrypt.game.model.Skills
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextPrimary


@Composable
fun SkillBar(
    classId: String,
    cooldowns: Map<String, Int>,
    onSkill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val skills = Skills.forClass(classId)
    if (skills.isEmpty()) return

    var inspectedSkill by remember { mutableStateOf<Skill?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (skill in skills) {
            val cd = cooldowns[skill.id] ?: 0
            val ready = cd <= 0
            SkillButton(
                skill = skill,
                cooldown = cd,
                ready = ready,
                onClick = { onSkill(skill.id) },
                onLongPress = { inspectedSkill = skill }
            )
        }
    }

    inspectedSkill?.let { skill ->
        SkillTooltipPopup(
            skill = skill,
            currentCooldown = cooldowns[skill.id] ?: 0,
            onDismiss = { inspectedSkill = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(
    skill: Skill,
    cooldown: Int,
    ready: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (ready) DungeonPurple80.copy(alpha = 0.3f)
                else HudBackground.copy(alpha = 0.5f)
            )
            .combinedClickable(
                onClick = { if (ready) onClick() },
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = skill.emoji,
            fontSize = 20.sp
        )
        if (!ready) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(HudBackground.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$cooldown",
                    style = MaterialTheme.typography.labelSmall,
                    color = DungeonAmber80
                )
            }
        }
    }
}

@Composable
private fun SkillTooltipPopup(
    skill: Skill,
    currentCooldown: Int,
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
            // Skill name + emoji
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = skill.emoji, fontSize = 28.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = skill.displayName.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = DungeonPurple80
                )
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )

            Spacer(Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SkillStat(
                    label = "TARGET",
                    value = when (skill.target) {
                        SkillTarget.SingleEnemy -> "Single"
                        SkillTarget.AllEnemies -> "All"
                        SkillTarget.Self -> "Self"
                        SkillTarget.AreaOfEffect -> "AoE r${skill.aoeRadius}"
                    }
                )
                if (skill.target != SkillTarget.Self) {
                    SkillStat(label = "RANGE", value = "${skill.range}")
                }
                SkillStat(
                    label = "CD",
                    value = if (currentCooldown > 0) "$currentCooldown/${skill.cooldown}" else "${skill.cooldown}"
                )
            }

            Spacer(Modifier.height(8.dp))

            // Damage info
            if (skill.damageMultiplier > 0f) {
                val pct = (skill.damageMultiplier * 100).toInt()
                Text(
                    text = "Damage: ${pct}% ATK",
                    style = MaterialTheme.typography.bodySmall,
                    color = HealthRed
                )
            }

            // Heal info
            if (skill.healAmount > 0) {
                Text(
                    text = "Heal: ${skill.healAmount}% Max HP",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF66BB6A)
                )
            }

            // Status effect inflicted
            skill.statusToInflict?.let { status ->
                Spacer(Modifier.height(4.dp))
                val effectName = status.statusEffect?.name ?: status.name
                val effectDetail = buildString {
                    append("Inflicts $effectName")
                    if (status.dotDamage > 0) append(" (${status.dotDamage} dmg/turn)")
                    append(" for ${status.turnsRemaining} turns")
                }
                Text(
                    text = effectDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = DungeonAmber80
                )
            }

            // Self buff
            skill.buff?.let { buff ->
                Spacer(Modifier.height(4.dp))
                val buffDetail = buildString {
                    append("Buff: ")
                    val parts = mutableListOf<String>()
                    if (buff.atkBonus != 0) parts.add("ATK+${buff.atkBonus}")
                    if (buff.defBonus != 0) parts.add("DEF+${buff.defBonus}")
                    if (buff.magBonus != 0) parts.add("MAG+${buff.magBonus}")
                    if (buff.spdBonus != 0) parts.add("SPD+${buff.spdBonus}")
                    append(parts.joinToString(", "))
                    append(" for ${buff.turnsRemaining} turns")
                }
                Text(
                    text = buffDetail,
                    style = MaterialTheme.typography.bodySmall,
                    color = DungeonPurple80
                )
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
private fun SkillStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary.copy(alpha = 0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
}
