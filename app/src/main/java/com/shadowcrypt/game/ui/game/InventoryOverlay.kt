package com.shadowcrypt.game.ui.game

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.shadowcrypt.game.engine.model.EquipmentSlot
import com.shadowcrypt.game.engine.model.InventoryData
import com.shadowcrypt.game.engine.model.ItemData
import com.shadowcrypt.game.engine.model.ItemType
import com.shadowcrypt.game.engine.model.Rarity
import com.shadowcrypt.game.ui.theme.DungeonPurple80
import com.shadowcrypt.game.ui.theme.GameBackground
import com.shadowcrypt.game.ui.theme.HudBackground
import com.shadowcrypt.game.ui.theme.RarityCommon
import com.shadowcrypt.game.ui.theme.RarityEpic
import com.shadowcrypt.game.ui.theme.RarityLegendary
import com.shadowcrypt.game.ui.theme.RarityRare
import com.shadowcrypt.game.ui.theme.RarityUncommon
import com.shadowcrypt.game.ui.theme.HealthRed
import com.shadowcrypt.game.ui.theme.TextPrimary
import com.shadowcrypt.game.ui.theme.TextSecondary

@Composable
fun InventoryOverlay(
    inventory: InventoryData,
    onEquip: (Int) -> Unit,
    onUse: (Int) -> Unit,
    onDrop: (Int) -> Unit,
    onUnequip: (EquipmentSlot) -> Unit,
    onClose: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<ItemData?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBackground)
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(12.dp))
                .background(GameBackground)
                .border(1.dp, DungeonPurple80.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable(enabled = false) {} // Block click-through
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INVENTORY",
                    style = MaterialTheme.typography.titleMedium,
                    color = DungeonPurple80
                )
                Text(
                    text = "${inventory.items.size}/${inventory.maxSlots}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Equipment slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EquipmentSlotBox(
                    label = "WPN",
                    item = inventory.equipment.weapon,
                    onClick = {
                        if (inventory.equipment.weapon != null) {
                            onUnequip(EquipmentSlot.WEAPON)
                        }
                    }
                )
                EquipmentSlotBox(
                    label = "ARM",
                    item = inventory.equipment.armor,
                    onClick = {
                        if (inventory.equipment.armor != null) {
                            onUnequip(EquipmentSlot.ARMOR)
                        }
                    }
                )
                EquipmentSlotBox(
                    label = "ACC",
                    item = inventory.equipment.accessory,
                    onClick = {
                        if (inventory.equipment.accessory != null) {
                            onUnequip(EquipmentSlot.ACCESSORY)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Item grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(inventory.items, key = { it.id }) { item ->
                    ItemSlot(
                        item = item,
                        isSelected = selectedItem?.id == item.id,
                        onClick = { selectedItem = if (selectedItem?.id == item.id) null else item }
                    )
                }
            }

            // Item detail panel
            if (selectedItem != null) {
                val item = selectedItem!!
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            HudBackground,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = rarityColor(item.rarity)
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Stat bonuses
                    if (item.attackBonus > 0 || item.defenseBonus > 0 || item.maxHpBonus > 0) {
                        val stats = buildList {
                            if (item.attackBonus > 0) add("+${item.attackBonus} ATK")
                            if (item.defenseBonus > 0) add("+${item.defenseBonus} DEF")
                            if (item.maxHpBonus > 0) add("+${item.maxHpBonus} HP")
                        }.joinToString("  ")
                        Text(
                            text = stats,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Stat comparison with currently equipped item
                    val isEquipment = item.type in listOf(
                        ItemType.WEAPON, ItemType.ARMOR, ItemType.ACCESSORY
                    )
                    if (isEquipment) {
                        val currentEquipped = when (item.type) {
                            ItemType.WEAPON -> inventory.equipment.weapon
                            ItemType.ARMOR -> inventory.equipment.armor
                            ItemType.ACCESSORY -> inventory.equipment.accessory
                            else -> null
                        }
                        val atkDelta = item.attackBonus - (currentEquipped?.attackBonus ?: 0)
                        val defDelta = item.defenseBonus - (currentEquipped?.defenseBonus ?: 0)
                        val hpDelta = item.maxHpBonus - (currentEquipped?.maxHpBonus ?: 0)

                        if (atkDelta != 0 || defDelta != 0 || hpDelta != 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                val vsLabel = if (currentEquipped != null) "vs ${currentEquipped.name.take(10)}" else "vs empty"
                                Text(
                                    text = vsLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                if (atkDelta != 0) {
                                    val sign = if (atkDelta > 0) "+" else ""
                                    Text(
                                        text = "${sign}${atkDelta} ATK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (atkDelta > 0) RarityUncommon else HealthRed
                                    )
                                }
                                if (defDelta != 0) {
                                    val sign = if (defDelta > 0) "+" else ""
                                    Text(
                                        text = "${sign}${defDelta} DEF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (defDelta > 0) RarityUncommon else HealthRed
                                    )
                                }
                                if (hpDelta != 0) {
                                    val sign = if (hpDelta > 0) "+" else ""
                                    Text(
                                        text = "${sign}${hpDelta} HP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hpDelta > 0) RarityUncommon else HealthRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isConsumable = item.type in listOf(
                            ItemType.POTION, ItemType.SCROLL
                        )

                        if (isEquipment) {
                            ActionButton("EQUIP", RarityUncommon) {
                                onEquip(item.id)
                                selectedItem = null
                            }
                        }
                        if (isConsumable) {
                            ActionButton("USE", RarityRare) {
                                onUse(item.id)
                                selectedItem = null
                            }
                        }
                        ActionButton("DROP", RarityCommon) {
                            onDrop(item.id)
                            selectedItem = null
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Close button
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DungeonPurple80.copy(alpha = 0.3f),
                    contentColor = DungeonPurple80
                )
            ) {
                Text("CLOSE", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EquipmentSlotBox(
    label: String,
    item: ItemData?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .border(
                    width = 1.dp,
                    color = if (item != null) rarityColor(item.rarity) else TextSecondary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    if (item != null) rarityColor(item.rarity).copy(alpha = 0.1f)
                    else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (item != null) {
                Text(
                    text = item.name.take(6),
                    style = MaterialTheme.typography.labelSmall,
                    color = rarityColor(item.rarity),
                    maxLines = 2
                )
            } else {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun ItemSlot(
    item: ItemData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) TextPrimary else rarityColor(item.rarity).copy(alpha = 0.5f)
    val bgColor = if (isSelected) rarityColor(item.rarity).copy(alpha = 0.2f)
    else rarityColor(item.rarity).copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .size(56.dp)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(bgColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = typeIcon(item.type),
                style = MaterialTheme.typography.labelLarge,
                color = rarityColor(item.rarity)
            )
            Text(
                text = item.name.split(" ").last().take(5),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.3f),
            contentColor = color
        ),
        modifier = Modifier.width(80.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

private fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> RarityCommon
    Rarity.UNCOMMON -> RarityUncommon
    Rarity.RARE -> RarityRare
    Rarity.EPIC -> RarityEpic
    Rarity.LEGENDARY -> RarityLegendary
}

private fun typeIcon(type: ItemType): String = when (type) {
    ItemType.WEAPON -> "\u2694"
    ItemType.ARMOR -> "\u26E8"
    ItemType.ACCESSORY -> "\u25C6"
    ItemType.POTION -> "\u2661"
    ItemType.SCROLL -> "\u2606"
}
