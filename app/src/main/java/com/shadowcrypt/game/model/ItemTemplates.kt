package com.shadowcrypt.game.model

object ItemTemplates {

    // ===== WEAPONS =====

    val IronSword = ItemTemplate(
        id = "iron_sword", displayName = "Iron Sword",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 3,
        minFloor = 1, maxFloor = 4, dropWeight = 15,
        description = "A sturdy iron blade."
    )
    val ShadowDagger = ItemTemplate(
        id = "shadow_dagger", displayName = "Shadow Dagger",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 2, baseSpd = 2,
        minFloor = 1, maxFloor = 4, dropWeight = 12,
        description = "A quick dagger that improves speed."
    )
    val OakStaff = ItemTemplate(
        id = "oak_staff", displayName = "Oak Staff",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 1, baseMag = 3,
        minFloor = 1, maxFloor = 4, dropWeight = 10,
        description = "A staff that channels magical energy."
    )
    val WarMace = ItemTemplate(
        id = "war_mace", displayName = "War Mace",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 4, baseSpd = -1,
        minFloor = 1, maxFloor = 5, dropWeight = 10,
        description = "Heavy but hits hard."
    )
    val SteelLongsword = ItemTemplate(
        id = "steel_longsword", displayName = "Steel Longsword",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 5,
        minFloor = 4, maxFloor = 7, dropWeight = 12,
        description = "A well-forged steel blade."
    )
    val VenomFang = ItemTemplate(
        id = "venom_fang", displayName = "Venom Fang",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 4, baseSpd = 2, baseMag = 1,
        minFloor = 4, maxFloor = 7, dropWeight = 8,
        description = "A poisoned blade."
    )
    val RuneStaff = ItemTemplate(
        id = "rune_staff", displayName = "Rune Staff",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 1, baseMag = 6,
        minFloor = 4, maxFloor = 8, dropWeight = 8,
        description = "Glowing with arcane runes."
    )
    val CrystalMace = ItemTemplate(
        id = "crystal_mace", displayName = "Crystal Mace",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 5, baseMag = 2, baseSpd = -1,
        minFloor = 5, maxFloor = 8, dropWeight = 8,
        description = "Infused with crystal shards."
    )
    val InfernoBlade = ItemTemplate(
        id = "inferno_blade", displayName = "Inferno Blade",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 7, baseMag = 2,
        minFloor = 7, maxFloor = 10, dropWeight = 8,
        description = "A sword wreathed in hellfire."
    )
    val VoidScythe = ItemTemplate(
        id = "void_scythe", displayName = "Void Scythe",
        category = ItemCategory.Weapon, equipSlot = EquipSlot.Weapon,
        baseAtk = 8, baseSpd = 1,
        minFloor = 9, maxFloor = 10, dropWeight = 5,
        description = "Cuts through reality itself."
    )

    // ===== ARMOR =====

    val LeatherArmor = ItemTemplate(
        id = "leather_armor", displayName = "Leather Armor",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 2, baseHpBonus = 5,
        minFloor = 1, maxFloor = 4, dropWeight = 15,
        description = "Basic leather protection."
    )
    val ChainMail = ItemTemplate(
        id = "chain_mail", displayName = "Chain Mail",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 3, baseHpBonus = 8, baseSpd = -1,
        minFloor = 2, maxFloor = 5, dropWeight = 10,
        description = "Interlocking metal rings."
    )
    val MageRobes = ItemTemplate(
        id = "mage_robes", displayName = "Mage Robes",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 1, baseMag = 2, baseHpBonus = 3,
        minFloor = 1, maxFloor = 5, dropWeight = 10,
        description = "Robes woven with arcane thread."
    )
    val PlateArmor = ItemTemplate(
        id = "plate_armor", displayName = "Plate Armor",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 5, baseHpBonus = 12, baseSpd = -2,
        minFloor = 4, maxFloor = 7, dropWeight = 8,
        description = "Heavy plate offering strong defense."
    )
    val ShadowCloak = ItemTemplate(
        id = "shadow_cloak", displayName = "Shadow Cloak",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 2, baseSpd = 3, baseHpBonus = 5,
        minFloor = 5, maxFloor = 8, dropWeight = 6,
        description = "A cloak that bends light."
    )
    val InfernalPlate = ItemTemplate(
        id = "infernal_plate", displayName = "Infernal Plate",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 7, baseHpBonus = 15, baseSpd = -1,
        minFloor = 7, maxFloor = 10, dropWeight = 5,
        description = "Forged in hellfire."
    )
    val VoidArmor = ItemTemplate(
        id = "void_armor", displayName = "Void Armor",
        category = ItemCategory.Armor, equipSlot = EquipSlot.Armor,
        baseDef = 6, baseMag = 3, baseHpBonus = 10,
        minFloor = 9, maxFloor = 10, dropWeight = 4,
        description = "Woven from void essence."
    )

    // ===== ACCESSORIES =====

    val IronRing = ItemTemplate(
        id = "iron_ring", displayName = "Iron Ring",
        category = ItemCategory.Accessory, equipSlot = EquipSlot.Accessory,
        baseAtk = 1, baseDef = 1,
        minFloor = 1, maxFloor = 5, dropWeight = 12,
        description = "A simple iron band."
    )
    val AmuletOfVitality = ItemTemplate(
        id = "amulet_vitality", displayName = "Amulet of Vitality",
        category = ItemCategory.Accessory, equipSlot = EquipSlot.Accessory,
        baseHpBonus = 10, baseDef = 1,
        minFloor = 2, maxFloor = 6, dropWeight = 8,
        description = "Pulses with life energy."
    )
    val SpeedBoots = ItemTemplate(
        id = "speed_boots", displayName = "Speed Boots",
        category = ItemCategory.Accessory, equipSlot = EquipSlot.Accessory,
        baseSpd = 3,
        minFloor = 3, maxFloor = 7, dropWeight = 8,
        description = "Enchanted for swiftness."
    )
    val ArcaneOrb = ItemTemplate(
        id = "arcane_orb", displayName = "Arcane Orb",
        category = ItemCategory.Accessory, equipSlot = EquipSlot.Accessory,
        baseMag = 4,
        minFloor = 4, maxFloor = 8, dropWeight = 6,
        description = "Swirling with magic."
    )
    val VoidPendant = ItemTemplate(
        id = "void_pendant", displayName = "Void Pendant",
        category = ItemCategory.Accessory, equipSlot = EquipSlot.Accessory,
        baseAtk = 2, baseMag = 2, baseSpd = 1,
        minFloor = 8, maxFloor = 10, dropWeight = 4,
        description = "A pendant from the void."
    )

    // ===== CONSUMABLES =====

    val HealthPotion = ItemTemplate(
        id = "health_potion", displayName = "Health Potion",
        category = ItemCategory.Consumable,
        effect = ItemEffect.Heal(20),
        minFloor = 1, maxFloor = 10, dropWeight = 20,
        description = "Restores 20 HP."
    )
    val GreaterHealthPotion = ItemTemplate(
        id = "greater_health_potion", displayName = "Greater Health Potion",
        category = ItemCategory.Consumable,
        effect = ItemEffect.Heal(40),
        minFloor = 5, maxFloor = 10, dropWeight = 10,
        description = "Restores 40 HP."
    )
    val StrengthPotion = ItemTemplate(
        id = "strength_potion", displayName = "Strength Potion",
        category = ItemCategory.Consumable,
        effect = ItemEffect.BuffAtk(4, 15),
        minFloor = 2, maxFloor = 10, dropWeight = 8,
        description = "+4 ATK for 15 turns."
    )
    val IronSkinPotion = ItemTemplate(
        id = "iron_skin_potion", displayName = "Iron Skin Potion",
        category = ItemCategory.Consumable,
        effect = ItemEffect.BuffDef(4, 15),
        minFloor = 2, maxFloor = 10, dropWeight = 8,
        description = "+4 DEF for 15 turns."
    )
    val SpeedScroll = ItemTemplate(
        id = "speed_scroll", displayName = "Speed Scroll",
        category = ItemCategory.Consumable,
        effect = ItemEffect.BuffSpd(4, 15),
        minFloor = 3, maxFloor = 10, dropWeight = 8,
        description = "+4 SPD for 15 turns."
    )
    val ArcaneTome = ItemTemplate(
        id = "arcane_tome", displayName = "Arcane Tome",
        category = ItemCategory.Consumable,
        effect = ItemEffect.BuffMag(4, 15),
        minFloor = 3, maxFloor = 10, dropWeight = 6,
        description = "+4 MAG for 15 turns."
    )
    val TeleportScroll = ItemTemplate(
        id = "teleport_scroll", displayName = "Teleport Scroll",
        category = ItemCategory.Consumable,
        effect = ItemEffect.Teleport,
        minFloor = 3, maxFloor = 10, dropWeight = 5,
        description = "Teleport to a random safe location."
    )
    val MapScroll = ItemTemplate(
        id = "map_scroll", displayName = "Map Scroll",
        category = ItemCategory.Consumable,
        effect = ItemEffect.RevealMap,
        minFloor = 1, maxFloor = 10, dropWeight = 5,
        description = "Reveals the entire floor."
    )
    val FreezeScroll = ItemTemplate(
        id = "freeze_scroll", displayName = "Freeze Scroll",
        category = ItemCategory.Consumable,
        effect = ItemEffect.FreezeEnemies(3),
        minFloor = 4, maxFloor = 10, dropWeight = 4,
        description = "Stuns all visible enemies for 3 turns."
    )
    val Antidote = ItemTemplate(
        id = "antidote", displayName = "Antidote",
        category = ItemCategory.Consumable,
        effect = ItemEffect.CureStatus,
        minFloor = 2, maxFloor = 10, dropWeight = 8,
        description = "Cures all status effects."
    )

    // ===== Lookups =====

    val allEquipment: List<ItemTemplate> = listOf(
        IronSword, ShadowDagger, OakStaff, WarMace,
        SteelLongsword, VenomFang, RuneStaff, CrystalMace,
        InfernoBlade, VoidScythe,
        LeatherArmor, ChainMail, MageRobes, PlateArmor,
        ShadowCloak, InfernalPlate, VoidArmor,
        IronRing, AmuletOfVitality, SpeedBoots, ArcaneOrb, VoidPendant
    )

    val allConsumables: List<ItemTemplate> = listOf(
        HealthPotion, GreaterHealthPotion,
        StrengthPotion, IronSkinPotion,
        SpeedScroll, ArcaneTome,
        TeleportScroll, MapScroll, FreezeScroll, Antidote
    )

    val all: List<ItemTemplate> = allEquipment + allConsumables

    fun forFloor(floor: Int): List<ItemTemplate> =
        all.filter { floor in it.minFloor..it.maxFloor }

    fun equipmentForFloor(floor: Int): List<ItemTemplate> =
        allEquipment.filter { floor in it.minFloor..it.maxFloor }

    fun consumablesForFloor(floor: Int): List<ItemTemplate> =
        allConsumables.filter { floor in it.minFloor..it.maxFloor }
}
