package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class ModBlockLootProvider extends FabricBlockLootTableProvider {
	public ModBlockLootProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generate() {
		dropSelf(ModBlocks.JADE_STONE);
		dropSelf(ModBlocks.MEDITATION_MAT);
		dropSelf(ModBlocks.RED_MEDITATION_MAT);
		dropSelf(ModBlocks.BLUE_SPIRIT_LOG);
		dropSelf(ModBlocks.BLUE_SPIRIT_WOOD);
		dropSelf(ModBlocks.BLUE_SPIRIT_PLANKS);
		dropSelf(ModBlocks.ALCHEMY_CAULDRON);
		add(ModBlocks.JADE_ORE, createOreDrop(ModBlocks.JADE_ORE, ModItems.JADE));
		add(ModBlocks.DEEPSLATE_JADE_ORE, createOreDrop(ModBlocks.DEEPSLATE_JADE_ORE, ModItems.JADE));
		add(ModBlocks.WHITE_BLOSSOM_LEAVES, createShearsOnlyDrop(ModBlocks.WHITE_BLOSSOM_LEAVES));
		add(ModBlocks.SPIRIT_PEACH_LEAVES, createShearsOnlyDrop(ModBlocks.SPIRIT_PEACH_LEAVES));
	}
}
