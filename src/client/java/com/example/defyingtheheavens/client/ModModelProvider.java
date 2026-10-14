package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DaoSteleBlock;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.GhostMethod;
import com.example.defyingtheheavens.QiElement;
import com.example.defyingtheheavens.MethodSteleBlock;
import com.example.defyingtheheavens.ModBlocks;
import com.example.defyingtheheavens.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.data.models.blockstates.PropertyDispatch;
import net.minecraft.data.models.blockstates.Variant;
import net.minecraft.data.models.blockstates.VariantProperties;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.data.models.model.TexturedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ModModelProvider extends FabricModelProvider {
	public ModModelProvider(FabricDataOutput output) {
		super(output);
	}

	@Override
	public void generateBlockStateModels(BlockModelGenerators blocks) {
		blocks.createTrivialCube(ModBlocks.JADE_STONE);
		blocks.createTrivialCube(ModBlocks.JADE_ORE);
		blocks.createTrivialCube(ModBlocks.DEEPSLATE_JADE_ORE);
		blocks.createTrivialCube(ModBlocks.SPIRIT_STONE_ORE);
		blocks.createTrivialCube(ModBlocks.DEEPSLATE_SPIRIT_STONE_ORE);
		blocks.createTrivialCube(ModBlocks.RICH_SPIRIT_STONE_ORE);
		blocks.createTrivialBlock(ModBlocks.WHITE_BLOSSOM_LEAVES, TexturedModel.LEAVES);
		blocks.woodProvider(ModBlocks.BLUE_SPIRIT_LOG).logWithHorizontal(ModBlocks.BLUE_SPIRIT_LOG).wood(ModBlocks.BLUE_SPIRIT_WOOD);
		blocks.createTrivialCube(ModBlocks.BLUE_SPIRIT_PLANKS);
		blocks.createTrivialCube(ModBlocks.QI_VEIN);
		blocks.createTrivialCube(ModBlocks.DEEPSLATE_QI_VEIN);
		// The rift is drawn by its block entity renderer; the model only supplies black particles.
		blocks.createNonTemplateModelBlock(ModBlocks.SPATIAL_RIFT, Blocks.BLACK_CONCRETE);
		// The Netherworld: the Yellow Springs rift and the Wheel's hub are drawn by their renderers too.
		blocks.createNonTemplateModelBlock(ModBlocks.YELLOW_SPRINGS_RIFT, Blocks.BLACK_CONCRETE);
		blocks.createNonTemplateModelBlock(ModBlocks.WHEEL_OF_REINCARNATION, Blocks.POLISHED_BLACKSTONE_BRICKS);
		blocks.createCrossBlockWithDefaultItem(ModBlocks.RED_SPIDER_LILY, BlockModelGenerators.TintState.NOT_TINTED);
		methodStele(blocks);
		daoStele(blocks);
		blocks.createTrivialCube(ModBlocks.YIN_JADE_ORE);
		blocks.createTrivialCube(ModBlocks.YIN_JADE_BLOCK);
		// The Netherworld's herbs have hand-made blockstates and models, as the other herbs do (resources).
	}

	/**
	 * The Method Stele: for each method and part a model on the hand-made shapes (models/block/method_stele_part and _top), the
	 * method's emblem on the middle stone's faces, the carved frame on the others; turned to face its way.
	 */
	private static void methodStele(BlockModelGenerators blocks) {
		ModelTemplate part = new ModelTemplate(
				Optional.of(DefyingTheHeavens.id("block/method_stele_part")), Optional.empty(),
				TextureSlot.FRONT, TextureSlot.SIDE);
		ModelTemplate top = new ModelTemplate(
				Optional.of(DefyingTheHeavens.id("block/method_stele_top")), Optional.empty(),
				TextureSlot.FRONT, TextureSlot.SIDE);
		ResourceLocation side = DefyingTheHeavens.id("block/method_stele_side");
		ResourceLocation frame = DefyingTheHeavens.id("block/method_stele_front");
		Map<String, ResourceLocation> models = new HashMap<>();
		for (GhostMethod method : GhostMethod.values()) {
			for (MethodSteleBlock.Part p : MethodSteleBlock.Part.values()) {
				ResourceLocation front = p == MethodSteleBlock.Part.MIDDLE
						? DefyingTheHeavens.id("block/method_stele_" + method.getId()) : frame;
				ResourceLocation id = DefyingTheHeavens.id(
						"block/method_stele_" + method.getId() + "_" + p.getSerializedName());
				TextureMapping textures = new TextureMapping()
						.put(TextureSlot.FRONT, front).put(TextureSlot.SIDE, side);
				(p == MethodSteleBlock.Part.UPPER ? top : part).create(id, textures, blocks.modelOutput);
				models.put(method.getId() + "_" + p.getSerializedName(), id);
			}
		}
		blocks.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ModBlocks.METHOD_STELE)
				.with(PropertyDispatch.properties(MethodSteleBlock.METHOD,
						MethodSteleBlock.PART, MethodSteleBlock.FACING)
						.generate((method, p, facing) -> Variant.variant()
								.with(VariantProperties.MODEL, models.get(method.getId() + "_" + p.getSerializedName()))
								.with(VariantProperties.Y_ROT, switch (facing) {
									case EAST -> VariantProperties.Rotation.R90;
									case SOUTH -> VariantProperties.Rotation.R180;
									case WEST -> VariantProperties.Rotation.R270;
									default -> VariantProperties.Rotation.R0;
								}))));
	}

	/**
	 * The Dao Stele: the Method Stele's shape (its part and top models) in pale carved stone, its element's emblem on the middle
	 * stone's faces (one model each element: its six methods' steles look alike), the frame on the others.
	 */
	private static void daoStele(BlockModelGenerators blocks) {
		ModelTemplate part = new ModelTemplate(
				Optional.of(DefyingTheHeavens.id("block/method_stele_part")), Optional.empty(),
				TextureSlot.FRONT, TextureSlot.SIDE);
		ModelTemplate top = new ModelTemplate(
				Optional.of(DefyingTheHeavens.id("block/method_stele_top")), Optional.empty(),
				TextureSlot.FRONT, TextureSlot.SIDE);
		ResourceLocation side = DefyingTheHeavens.id("block/dao_stele_side");
		ResourceLocation frame = DefyingTheHeavens.id("block/dao_stele_front");
		Map<String, ResourceLocation> models = new HashMap<>();
		for (QiElement element : QiElement.five()) {
			String name = element.name().toLowerCase(java.util.Locale.ROOT);
			for (MethodSteleBlock.Part p : MethodSteleBlock.Part.values()) {
				ResourceLocation front = p == MethodSteleBlock.Part.MIDDLE ? DefyingTheHeavens.id("block/dao_stele_" + name) : frame;
				ResourceLocation id = DefyingTheHeavens.id("block/dao_stele_" + name + "_" + p.getSerializedName());
				TextureMapping textures = new TextureMapping().put(TextureSlot.FRONT, front).put(TextureSlot.SIDE, side);
				(p == MethodSteleBlock.Part.UPPER ? top : part).create(id, textures, blocks.modelOutput);
				models.put(name + "_" + p.getSerializedName(), id);
			}
		}
		blocks.blockStateOutput.accept(MultiVariantGenerator.multiVariant(ModBlocks.DAO_STELE)
				.with(PropertyDispatch.properties(DaoSteleBlock.METHOD, DaoSteleBlock.PART, DaoSteleBlock.FACING)
						.generate((method, p, facing) -> Variant.variant()
								.with(VariantProperties.MODEL, models.get(method.element().name().toLowerCase(java.util.Locale.ROOT) + "_" + p.getSerializedName()))
								.with(VariantProperties.Y_ROT, switch (facing) {
									case EAST -> VariantProperties.Rotation.R90;
									case SOUTH -> VariantProperties.Rotation.R180;
									case WEST -> VariantProperties.Rotation.R270;
									default -> VariantProperties.Rotation.R0;
								}))));
	}

	@Override
	public void generateItemModels(ItemModelGenerators items) {
		items.generateFlatItem(ModItems.JADE_SLIP, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.RING_OF_POWER, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.RING_OF_TRANSCENDENCE, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.INSCRIPTION_BRUSH, ModelTemplates.FLAT_HANDHELD_ITEM);
		items.generateFlatItem(ModItems.YANG_FIRE_TALISMAN, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.GHOST_QUELLING_TALISMAN, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.GHOST_JADE_SLIP, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.YIN_JADE, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.PEACH_WOOD_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
		items.generateFlatItem(ModItems.OTHER_SHORE_FLOWER, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.OTHER_SHORE_FLOWER_SEEDS, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.NETHERWORLD_LOTUS, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.NETHERWORLD_LOTUS_SEEDS, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.YANG_PRESERVATION_PILL, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.YIN_GATHERING_PILL, ModelTemplates.FLAT_ITEM);
		items.generateFlatItem(ModItems.JADE, ModelTemplates.FLAT_ITEM);
		// Spirit stones have a hand-made oval model (resources: models/item/spirit_stone.json), not a flat sprite.
	}
}
