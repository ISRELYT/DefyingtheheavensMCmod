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
		// A Qi Vein comes out whole (to be set into a formation's array), with no Fortune bonus; the core drops itself.
		dropSelf(ModBlocks.QI_VEIN);
		dropSelf(ModBlocks.DEEPSLATE_QI_VEIN);
		dropSelf(ModBlocks.FORMATION_CORE);
		// The sapling has no item of its own: breaking it gives back the peach pit it grew from.
		dropOther(ModBlocks.SPIRIT_PEACH_SAPLING, ModItems.PEACH_PIT);
		add(ModBlocks.JADE_ORE, createOreDrop(ModBlocks.JADE_ORE, ModItems.JADE));
		add(ModBlocks.DEEPSLATE_JADE_ORE, createOreDrop(ModBlocks.DEEPSLATE_JADE_ORE, ModItems.JADE));
		add(ModBlocks.WHITE_BLOSSOM_LEAVES, createShearsOnlyDrop(ModBlocks.WHITE_BLOSSOM_LEAVES));
		add(ModBlocks.SPIRIT_PEACH_LEAVES, createShearsOnlyDrop(ModBlocks.SPIRIT_PEACH_LEAVES));
	}
}
