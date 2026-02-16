package com.shadowcrypt.game.ui.classselect

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.model.CharacterClass
import com.shadowcrypt.game.ui.theme.CavernAccent
import com.shadowcrypt.game.ui.theme.CryptAccent
import com.shadowcrypt.game.ui.theme.DungeonAmber80
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.SewerAccent
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.VoidAccent

private fun isClassUnlocked(classId: String, progress: MetaProgressRepository.MetaProgress): Boolean =
    when (classId) {
        "warrior" -> true
        "rogue" -> progress.totalRuns >= 5
        "mage" -> progress.totalRuns >= 10
        "cleric" -> progress.bestFloor >= 5
        else -> true
    }

private fun unlockConditionText(classId: String): String = when (classId) {
    "rogue" -> "Complete 5 runs to unlock"
    "mage" -> "Complete 10 runs to unlock"
    "cleric" -> "Reach floor 5 to unlock"
    else -> ""
}

@Composable
fun ClassSelectScreen(
    onClassSelected: (classId: String) -> Unit
) {
    val classes = listOf(
        CharacterClass.fromId("warrior"),
        CharacterClass.fromId("rogue"),
        CharacterClass.fromId("mage"),
        CharacterClass.fromId("cleric")
    )

    val progress by ServiceLocator.metaProgressRepository.progress.collectAsStateWithLifecycle(
        initialValue = MetaProgressRepository.MetaProgress()
    )

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
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CHOOSE YOUR CLASS",
                style = MaterialTheme.typography.headlineLarge,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(24.dp))

            for (charClass in classes) {
                val unlocked = isClassUnlocked(charClass.id, progress)
                ClassCard(
                    charClass = charClass,
                    accentColor = classAccentColor(charClass.id),
                    unlocked = unlocked,
                    lockText = unlockConditionText(charClass.id),
                    onClick = { if (unlocked) onClassSelected(charClass.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ClassCard(
    charClass: CharacterClass,
    accentColor: Color,
    unlocked: Boolean,
    lockText: String,
    onClick: () -> Unit
) {
    val cardAlpha = if (unlocked) 1f else 0.4f
    val cardModifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(HudBackground)
        .then(if (unlocked) Modifier.clickable { onClick() } else Modifier)
        .padding(16.dp)

    Column(modifier = cardModifier.alpha(cardAlpha)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = charClass.displayName.uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = accentColor
            )
            if (!unlocked) {
                Text(
                    text = "LOCKED",
                    style = MaterialTheme.typography.labelLarge,
                    color = HealthRed.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (unlocked) {
            Text(
                text = charClass.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            Text(
                text = lockText,
                style = MaterialTheme.typography.bodyMedium,
                color = HealthRed.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn("HP", charClass.baseHp, accentColor, isHighlight = charClass.baseHp >= 50)
            StatColumn("ATK", charClass.baseAtk, accentColor, isHighlight = charClass.baseAtk >= 8)
            StatColumn("DEF", charClass.baseDef, accentColor, isHighlight = charClass.baseDef >= 6)
            StatColumn("MAG", charClass.baseMag, accentColor, isHighlight = charClass.baseMag >= 8)
            StatColumn("SPD", charClass.baseSpd, accentColor, isHighlight = charClass.baseSpd >= 9)
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: Int,
    accentColor: Color,
    isHighlight: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isHighlight) accentColor else TextPrimary
        )
    }
}

private fun classAccentColor(classId: String): Color = when (classId) {
    "warrior" -> CryptAccent
    "rogue" -> SewerAccent
    "mage" -> CavernAccent
    "cleric" -> DungeonAmber80
    else -> DungeonPurple80
}
