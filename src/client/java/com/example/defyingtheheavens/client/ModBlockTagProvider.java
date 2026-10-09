package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
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
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.JADE_STONE).add(ModBlocks.SPIRIT_PEDESTAL);
		// Lets vanilla-style ore configurations (and Upper Realm ores) replace jade like stone.
		getOrCreateTagBuilder(BlockTags.STONE_ORE_REPLACEABLES).add(ModBlocks.JADE_STONE);
		getOrCreateTagBuilder(BlockTags.BASE_STONE_OVERWORLD).add(ModBlocks.JADE_STONE);

		getOrCreateTagBuilder(BlockTags.LEAVES).add(ModBlocks.WHITE_BLOSSOM_LEAVES);
		getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_HOE).add(ModBlocks.WHITE_BLOSSOM_LEAVES);

		// The rift must survive dragons, withers and feature placement.
		getOrCreateTagBuilder(BlockTags.DRAGON_IMMUNE).add(ModBlocks.SPATIAL_RIFT);
		getOrCreateTagBuilder(BlockTags.WITHER_IMMUNE).add(ModBlocks.SPATIAL_RIFT);
		getOrCreateTagBuilder(BlockTags.FEATURES_CANNOT_REPLACE).add(ModBlocks.SPATIAL_RIFT);
	}
}
