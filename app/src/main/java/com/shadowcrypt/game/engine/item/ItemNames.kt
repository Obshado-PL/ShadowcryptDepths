package com.shadowcrypt.game.engine.item

import com.shadowcrypt.game.engine.model.ItemType
import com.shadowcrypt.game.engine.model.Rarity
import kotlin.random.Random

object ItemNames {

    fun randomName(type: ItemType, rarity: Rarity, random: Random): String {
        val names = namePool(type, rarity)
        return names[random.nextInt(names.size)]
    }

    private fun namePool(type: ItemType, rarity: Rarity): List<String> = when (type) {
        ItemType.WEAPON -> weaponNames(rarity)
        ItemType.ARMOR -> armorNames(rarity)
        ItemType.ACCESSORY -> accessoryNames(rarity)
        ItemType.POTION -> potionNames(rarity)
        ItemType.SCROLL -> scrollNames(rarity)
    }

    private fun weaponNames(rarity: Rarity): List<String> = when (rarity) {
        Rarity.COMMON -> listOf("Rusted Blade", "Chipped Dagger", "Cracked Staff", "Worn Axe", "Bent Spear")
        Rarity.UNCOMMON -> listOf("Iron Sword", "Shadow Dagger", "Ashwood Staff", "Serrated Axe", "Bone Spear")
        Rarity.RARE -> listOf("Wraithblade", "Venom Fang", "Doomstaff", "Bloodaxe", "Soulspear")
        Rarity.EPIC -> listOf("Nightfall Edge", "Phantom Claw", "Voidstaff", "Hellfire Cleaver", "Abyssal Lance")
        Rarity.LEGENDARY -> listOf("Shadowreaper", "Cryptfang", "Doomhowl", "Netheraxe", "Soulpiercer")
    }

    private fun armorNames(rarity: Rarity): List<String> = when (rarity) {
        Rarity.COMMON -> listOf("Tattered Robe", "Worn Leather", "Rusty Chainmail", "Cracked Plate")
        Rarity.UNCOMMON -> listOf("Leather Vest", "Iron Chainmail", "Reinforced Robe", "Bone Armor")
        Rarity.RARE -> listOf("Shadowweave", "Wraithmail", "Doomplate", "Cryptguard")
        Rarity.EPIC -> listOf("Nethershroud", "Voidmail", "Hellforged Plate", "Phantomweave")
        Rarity.LEGENDARY -> listOf("Abyssal Shroud", "Shadowplate", "Doomguard Eternal", "Lichking Mantle")
    }

    private fun accessoryNames(rarity: Rarity): List<String> = when (rarity) {
        Rarity.COMMON -> listOf("Cracked Ring", "Dull Amulet", "Tarnished Brooch")
        Rarity.UNCOMMON -> listOf("Iron Ring", "Bone Amulet", "Shadow Brooch")
        Rarity.RARE -> listOf("Ring of Warding", "Cryptkeeper Amulet", "Wraithstone Brooch")
        Rarity.EPIC -> listOf("Nethering", "Void Amulet", "Doomstone Pendant")
        Rarity.LEGENDARY -> listOf("Shadowheart Ring", "Abyssal Amulet", "Soulstone")
    }

    private fun potionNames(rarity: Rarity): List<String> = when (rarity) {
        Rarity.COMMON -> listOf("Minor Health Potion")
        Rarity.UNCOMMON -> listOf("Health Potion")
        Rarity.RARE -> listOf("Greater Health Potion")
        Rarity.EPIC -> listOf("Supreme Health Potion")
        Rarity.LEGENDARY -> listOf("Elixir of Life")
    }

    private fun scrollNames(rarity: Rarity): List<String> = when (rarity) {
        Rarity.COMMON -> listOf("Scroll of Sparks")
        Rarity.UNCOMMON -> listOf("Scroll of Flame")
        Rarity.RARE -> listOf("Scroll of Frost")
        Rarity.EPIC -> listOf("Scroll of Lightning")
        Rarity.LEGENDARY -> listOf("Scroll of Annihilation")
    }
}
