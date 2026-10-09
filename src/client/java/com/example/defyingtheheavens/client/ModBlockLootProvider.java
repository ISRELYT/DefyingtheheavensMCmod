package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;

public class ModBlockLootProvider extends FabricBlockLootTableProvider {
	public ModBlockLootProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generate() {
		dropSelf(ModBlocks.JADE_STONE);
		add(ModBlocks.WHITE_BLOSSOM_LEAVES, createShearsOnlyDrop(ModBlocks.WHITE_BLOSSOM_LEAVES));
	}
}
