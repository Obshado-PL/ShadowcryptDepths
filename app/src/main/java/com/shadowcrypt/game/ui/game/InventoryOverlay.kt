package com.shadowcrypt.game.ui.game

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shadowcrypt.game.model.EquipSlot
import com.shadowcrypt.game.model.GameState
import com.shadowcrypt.game.model.Item
import com.shadowcrypt.game.model.ItemCategory
import com.shadowcrypt.game.model.Rarity
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.RarityCommon
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityLegendary
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary

@Composable
fun InventoryOverlay(
    state: GameState,
    onEquip: (itemId: Int) -> Unit,
    onUnequip: (slot: EquipSlot) -> Unit,
    onUse: (itemId: Int) -> Unit,
    onDrop: (itemId: Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameBackground.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INVENTORY",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                ActionButton(text = "CLOSE", color = TextSecondary, onClick = onClose)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Equipment Section
            Text(
                text = "EQUIPPED",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonPurple80
            )
            Spacer(modifier = Modifier.height(6.dp))

            EquipmentSlotRow("Weapon", state.player.equipment.weapon, EquipSlot.Weapon, onUnequip)
            EquipmentSlotRow("Armor", state.player.equipment.armor, EquipSlot.Armor, onUnequip)
            EquipmentSlotRow("Accessory", state.player.equipment.accessory, EquipSlot.Accessory, onUnequip)

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Summary
            Text(
                text = "STATS",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonPurple80
            )
            Spacer(modifier = Modifier.height(4.dp))

            val p = state.player
            val eq = p.equipment
            val buffAtk = p.activeBuffs.sumOf { it.atkBonus }
            val buffDef = p.activeBuffs.sumOf { it.defBonus }
            val buffMag = p.activeBuffs.sumOf { it.magBonus }
            val buffSpd = p.activeBuffs.sumOf { it.spdBonus }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(HudBackground)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatText("ATK", p.atk, eq.totalAtk, buffAtk)
                StatText("DEF", p.def, eq.totalDef, buffDef)
                StatText("MAG", p.mag, eq.totalMag, buffMag)
                StatText("SPD", p.spd, eq.totalSpd, buffSpd)
                StatText("HP", p.maxHp, eq.totalHpBonus, 0)
            }

            // Active Buffs
            if (p.activeBuffs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BUFFS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DungeonPurple80
                )
                Spacer(modifier = Modifier.height(4.dp))
                for (buff in p.activeBuffs) {
                    Text(
                        text = "${buff.name} (${buff.turnsRemaining} turns)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Inventory Items
            Text(
                text = "ITEMS (${p.inventory.items.size}/${p.inventory.capacity})",
                style = MaterialTheme.typography.labelLarge,
                color = DungeonPurple80
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (p.inventory.items.isEmpty()) {
                Text(
                    text = "No items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                for (item in p.inventory.items) {
                    InventoryItemRow(item, state, onEquip, onUse, onDrop)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EquipmentSlotRow(
    label: String,
    item: Item?,
    slot: EquipSlot,
    onUnequip: (EquipSlot) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(HudBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            if (item != null) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = rarityColor(item.rarity)
                )
                Text(
                    text = itemStatSummary(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = "--- empty ---",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.5f)
                )
            }
        }
        if (item != null) {
            ActionButton(text = "Remove", color = TextSecondary, onClick = { onUnequip(slot) })
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun InventoryItemRow(
    item: Item,
    state: GameState,
    onEquip: (Int) -> Unit,
    onUse: (Int) -> Unit,
    onDrop: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(HudBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = rarityColor(item.rarity)
            )
            Text(
                text = if (item.category == ItemCategory.Consumable) {
                    item.description
                } else {
                    itemStatSummary(item)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Stat comparison vs equipped item
            if (item.equipSlot != null) {
                val equipped = state.player.equipment.getSlot(item.equipSlot)
                if (equipped != null) {
                    val comparison = buildStatComparison(item, equipped)
                    if (comparison.isNotEmpty()) {
                        Text(
                            text = "vs equipped: $comparison",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        Row {
            if (item.equipSlot != null) {
                ActionButton(text = "Equip", color = DungeonPurple80, onClick = { onEquip(item.id) })
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (item.category == ItemCategory.Consumable) {
                ActionButton(text = "Use", color = DungeonPurple80, onClick = { onUse(item.id) })
                Spacer(modifier = Modifier.width(4.dp))
            }
            ActionButton(text = "Drop", color = TextSecondary, onClick = { onDrop(item.id) })
        }
    }
}

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun StatText(label: String, base: Int, equipBonus: Int, buffBonus: Int) {
    val total = equipBonus + buffBonus
    val bonusText = if (total > 0) "(+$total)" else if (total < 0) "($total)" else ""
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = "$base$bonusText",
            style = MaterialTheme.typography.bodySmall,
            color = if (total > 0) DungeonPurple80 else TextPrimary
        )
    }
}

private fun itemStatSummary(item: Item): String {
    val parts = mutableListOf<String>()
    if (item.atk != 0) parts.add("${if (item.atk > 0) "+" else ""}${item.atk} ATK")
    if (item.def != 0) parts.add("${if (item.def > 0) "+" else ""}${item.def} DEF")
    if (item.mag != 0) parts.add("${if (item.mag > 0) "+" else ""}${item.mag} MAG")
    if (item.spd != 0) parts.add("${if (item.spd > 0) "+" else ""}${item.spd} SPD")
    if (item.hpBonus != 0) parts.add("${if (item.hpBonus > 0) "+" else ""}${item.hpBonus} HP")
    return if (parts.isEmpty()) item.description else parts.joinToString(" ")
}

private fun buildStatComparison(item: Item, equipped: Item): String {
    val diffs = mutableListOf<String>()
    val dAtk = item.atk - equipped.atk
    val dDef = item.def - equipped.def
    val dMag = item.mag - equipped.mag
    val dSpd = item.spd - equipped.spd
    val dHp = item.hpBonus - equipped.hpBonus
    if (dAtk != 0) diffs.add("${if (dAtk > 0) "+" else ""}$dAtk ATK")
    if (dDef != 0) diffs.add("${if (dDef > 0) "+" else ""}$dDef DEF")
    if (dMag != 0) diffs.add("${if (dMag > 0) "+" else ""}$dMag MAG")
    if (dSpd != 0) diffs.add("${if (dSpd > 0) "+" else ""}$dSpd SPD")
    if (dHp != 0) diffs.add("${if (dHp > 0) "+" else ""}$dHp HP")
    return diffs.joinToString(" ")
}

fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.Common -> RarityCommon
    Rarity.Uncommon -> RarityUncommon
    Rarity.Rare -> RarityRare
    Rarity.Epic -> RarityEpic
    Rarity.Legendary -> RarityLegendary
}
