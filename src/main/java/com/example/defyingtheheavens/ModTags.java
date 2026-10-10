package com.example.defyingtheheavens;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** The mod's own tags (filled by datagen, see client ModBlockTagProvider, ModItemTagProvider and ModBiomeTagProvider). */
public final class ModTags {
	/** Both Qi Vein ores, as items: what clothing upgrades take. */
	public static final TagKey<Item> QI_VEINS = TagKey.create(Registries.ITEM, DefyingTheHeavens.id("qi_veins"));
	/** Both Qi Vein ores, as blocks: what a formation draws its qi from. */
	public static final TagKey<Block> QI_VEIN_BLOCKS = TagKey.create(Registries.BLOCK, DefyingTheHeavens.id("qi_veins"));

	private ModTags() {}
}
