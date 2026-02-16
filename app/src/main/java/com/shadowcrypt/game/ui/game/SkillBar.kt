package com.shadowcrypt.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shadowcrypt.game.model.Skill
import com.shadowcrypt.game.model.Skills
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.TextSecondary

@Composable
fun SkillBar(
    classId: String,
    cooldowns: Map<String, Int>,
    onSkill: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val skills = Skills.forClass(classId)
    if (skills.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (skill in skills) {
            val cd = cooldowns[skill.id] ?: 0
            val ready = cd <= 0
            SkillButton(skill = skill, cooldown = cd, ready = ready) {
                onSkill(skill.id)
            }
        }
    }
}

@Composable
private fun SkillButton(
    skill: Skill,
    cooldown: Int,
    ready: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (ready) DungeonPurple80.copy(alpha = 0.3f)
                else HudBackground.copy(alpha = 0.5f)
            )
            .clickable(enabled = ready) { onClick() },
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
