package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
	public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.JADE_STONE).add(ModBlocks.SPIRIT_PEDESTAL)
				.add(ModBlocks.ALCHEMY_CAULDRON).add(ModBlocks.JADE_ORE).add(ModBlocks.DEEPSLATE_JADE_ORE);
		getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.JADE_ORE).add(ModBlocks.DEEPSLATE_JADE_ORE);
		// Lets vanilla-style ore configurations (and Upper Realm ores) replace jade like stone.
		getOrCreateTagBuilder(BlockTags.STONE_ORE_REPLACEABLES).add(ModBlocks.JADE_STONE);
		getOrCreateTagBuilder(BlockTags.BASE_STONE_OVERWORLD).add(ModBlocks.JADE_STONE);

		getOrCreateTagBuilder(BlockTags.LEAVES).add(ModBlocks.WHITE_BLOSSOM_LEAVES).add(ModBlocks.SPIRIT_PEACH_LEAVES);
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.WHITE_BLOSSOM_LEAVES).add(ModBlocks.SPIRIT_PEACH_LEAVES);
		// The Spirit Peach Tree's heart counts as a log: its leaves stay alive around it, and an axe cuts it.
		getOrCreateTagBuilder(BlockTags.LOGS).add(ModBlocks.SPIRIT_PEACH_HEART);
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE).add(ModBlocks.SPIRIT_PEACH_HEART);

		// Blue Spirit Wood behaves like any vanilla wood.
		getOrCreateTagBuilder(BlockTags.LOGS).add(ModBlocks.BLUE_SPIRIT_LOG).add(ModBlocks.BLUE_SPIRIT_WOOD);
		getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN).add(ModBlocks.BLUE_SPIRIT_LOG).add(ModBlocks.BLUE_SPIRIT_WOOD);
		getOrCreateTagBuilder(BlockTags.PLANKS).add(ModBlocks.BLUE_SPIRIT_PLANKS);
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE)
				.add(ModBlocks.BLUE_SPIRIT_LOG).add(ModBlocks.BLUE_SPIRIT_WOOD).add(ModBlocks.BLUE_SPIRIT_PLANKS);

		// The rift must survive dragons, withers and feature placement.
		getOrCreateTagBuilder(BlockTags.DRAGON_IMMUNE).add(ModBlocks.SPATIAL_RIFT);
		getOrCreateTagBuilder(BlockTags.WITHER_IMMUNE).add(ModBlocks.SPATIAL_RIFT);
		getOrCreateTagBuilder(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.SPATIAL_RIFT);

		// Qi Veins: diamond-hard ore, what formations draw on. The Formation Core takes an iron pickaxe.
		getOrCreateTagBuilder(ModTags.QI_VEIN_BLOCKS).add(ModBlocks.QI_VEIN).add(ModBlocks.DEEPSLATE_QI_VEIN);
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.QI_VEIN).add(ModBlocks.DEEPSLATE_QI_VEIN).add(ModBlocks.FORMATION_CORE);
		getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.QI_VEIN).add(ModBlocks.DEEPSLATE_QI_VEIN);
		getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.FORMATION_CORE);
		// Barriers: no dragon or wither breaks them, and snow never settles on the dome (it would show the invisible sphere).
		getOrCreateTagBuilder(BlockTags.DRAGON_IMMUNE).add(ModBlocks.SECT_BARRIER);
		getOrCreateTagBuilder(BlockTags.WITHER_IMMUNE).add(ModBlocks.SECT_BARRIER);
		getOrCreateTagBuilder(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON).add(ModBlocks.SECT_BARRIER);
		getOrCreateTagBuilder(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.SECT_BARRIER).add(ModBlocks.FORMATION_CORE);
	}
}
