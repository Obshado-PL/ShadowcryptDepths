package com.shadowcrypt.game.ui.classselect

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadowcrypt.game.data.model.UnlockCondition
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.PlayerColor
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.TextSecondary
import com.shadowcrypt.game.ui.theme.XpGold

private data class ClassInfo(
    val id: String,
    val name: String,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val description: String
)

private val classes = listOf(
    ClassInfo("warrior", "Warrior", 120, 8, 5, "Stalwart defender. Endures punishing blows and strikes back with force."),
    ClassInfo("rogue", "Rogue", 80, 12, 2, "Swift shadow. Fragile but deals devastating damage."),
    ClassInfo("mage", "Mage", 70, 14, 1, "Arcane wielder. Glass cannon that obliterates with raw power."),
    ClassInfo("cleric", "Cleric", 100, 7, 4, "Holy warrior. Good survivability with moderate offense.")
)

@Composable
fun ClassSelectScreen(
    onClassSelected: (String) -> Unit,
    viewModel: ClassSelectViewModel = viewModel()
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    val unlockedClassIds by viewModel.unlockedClassIds.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GameBackground)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CHOOSE YOUR CLASS",
                style = MaterialTheme.typography.headlineMedium,
                color = DungeonPurple80
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select a class to begin your descent",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            for (classInfo in classes) {
                val isSelected = selectedId == classInfo.id
                val isUnlocked = classInfo.id in unlockedClassIds
                val borderColor = when {
                    isSelected -> DungeonPurple80
                    !isUnlocked -> TextSecondary.copy(alpha = 0.15f)
                    else -> TextSecondary.copy(alpha = 0.3f)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            when {
                                !isUnlocked -> HudBackground.copy(alpha = 0.5f)
                                isSelected -> DungeonPurple80.copy(alpha = 0.1f)
                                else -> HudBackground
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .then(
                            if (isUnlocked) {
                                Modifier.clickable {
                                    selectedId = classInfo.id
                                    onClassSelected(classInfo.id)
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(16.dp)
                ) {
                    val contentAlpha = if (isUnlocked) 1f else 0.5f

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = classInfo.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isUnlocked) DungeonPurple80 else TextSecondary.copy(alpha = 0.5f)
                        )
                        if (!isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = HealthRed.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = classInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = contentAlpha)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatLabel("HP", classInfo.hp, HealthRed.copy(alpha = contentAlpha))
                        StatLabel("ATK", classInfo.atk, PlayerColor.copy(alpha = contentAlpha))
                        StatLabel("DEF", classInfo.def, RarityUncommon.copy(alpha = contentAlpha))
                    }

                    if (!isUnlocked) {
                        val condition = UnlockCondition.entries.find { it.classId == classInfo.id }
                        if (condition != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlock: ${condition.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = XpGold.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Text(
            text = " $value",
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
