package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.world.level.block.Blocks;

public class ModModelProvider extends FabricModelProvider {
	public ModModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blocks) {
		blocks.createTrivialCube(ModBlocks.JADE_STONE);
		blocks.createTrivialBlock(ModBlocks.WHITE_BLOSSOM_LEAVES, TexturedModel.LEAVES);
		blocks.woodProvider(ModBlocks.BLUE_SPIRIT_LOG).logWithHorizontal(ModBlocks.BLUE_SPIRIT_LOG).wood(ModBlocks.BLUE_SPIRIT_WOOD);
		blocks.createTrivialCube(ModBlocks.BLUE_SPIRIT_PLANKS);
		// The rift is drawn by its block entity renderer; the model only supplies black particles.
		blocks.createNonTemplateModelBlock(ModBlocks.SPATIAL_RIFT, Blocks.BLACK_CONCRETE);
	}

	@Override
	public void generateItemModels(ItemModelGenerators items) {
		items.generateFlatItem(ModItems.RING_OF_POWER, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.RING_OF_TRANSCENDENCE, ModelTemplates.FLAT_ITEM);
	}
}
