package com.example.defyingtheheavens;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/**
 * The mod's own tags (block tags filled by datagen, see client ModBlockTagProvider; item and biome tags hand-written under
 * data/defying-the-heavens/tags).
 */
public final class ModTags {
	/** Both Qi Vein ores, as items: what clothing upgrades take. */
	public static final TagKey<Item> QI_VEINS = TagKey.create(Registries.ITEM, DefyingTheHeavens.id("qi_veins"));
	/** Both Qi Vein ores, as blocks: what a formation draws its qi from. */
	public static final TagKey<Block> QI_VEIN_BLOCKS = TagKey.create(Registries.BLOCK, DefyingTheHeavens.id("qi_veins"));
	/**
	 * What a ghost may still use, the Netherworld being solid to the dead (see Incorporeal): the Wheel of Reincarnation, Method
	 * Steles, Meng Po's pot and the Terrace for Gazing Homeward.
	 */
	public static final TagKey<Block> GHOST_USABLE = TagKey.create(Registries.BLOCK, DefyingTheHeavens.id("ghost_usable"));
	/** The Upper Realm's island biomes: where its herbs, ores and fruit grow (ModFeatures) and its sects are laid out (SectStructure). */
	public static final TagKey<Biome> IS_UPPER_REALM = TagKey.create(Registries.BIOME, DefyingTheHeavens.id("is_upper_realm"));
	/** The Netherworld's biomes (see GhostQi, the realm's own features). */
	public static final TagKey<Biome> IS_NETHERWORLD = TagKey.create(Registries.BIOME, DefyingTheHeavens.id("is_netherworld"));

	private ModTags() {}
}
