package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	public static final Block CULTIVATION_FRUIT = register("cultivation_fruit", new CultivationFruitBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).noCollission().noOcclusion()
					.instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));
	/** The tear at Overworld X=0, Z=0. Unbreakable, walk-through, no item. */
	public static final Block SPATIAL_RIFT = register("spatial_rift", new SpatialRiftBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BLACK)
			.noCollission()
			.strength(-1.0f, 3600000.0f)
			.noLootTable()
			.lightLevel(state -> 12)
			.pushReaction(PushReaction.BLOCK)
			.sound(SoundType.AMETHYST)));

	/** Jade-tinged stone of the Dense Qi Peaks. Counts as ore-replaceable stone, so Upper Realm ores spawn in it. */
	public static final Block JADE_STONE = registerWithItem("jade_stone",
			new Block(BlockBehaviour.Properties.copy(Blocks.STONE).mapColor(MapColor.COLOR_LIGHT_GREEN)));

	/**
	 * Jade Ore: drops Jade (more with Fortune) and a little experience, like emerald ore, and needs an iron pickaxe. Found
	 * in Overworld mountains and all through the Upper Realm's islands (see ModPlacedFeatures), the deepslate kind deep down.
	 */
	public static final Block JADE_ORE = registerWithItem("jade_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.EMERALD_ORE), UniformInt.of(3, 7)));
	public static final Block DEEPSLATE_JADE_ORE = registerWithItem("deepslate_jade_ore",
			new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_EMERALD_ORE), UniformInt.of(3, 7)));

	/** The white canopy of the Peach Blossom Sanctuary's pale trees. */
	public static final Block WHITE_BLOSSOM_LEAVES = registerWithItem("white_blossom_leaves",
			new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_LEAVES).mapColor(MapColor.SNOW)));

	/** A woven straw cushion to sit and meditate on (see MeditationMatBlock). */
	public static final Block MEDITATION_MAT = registerWithItem("meditation_mat", new MeditationMatBlock(1.5,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS)
					.noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
	/** The same cushion with a square of red silk laid across it. */
	public static final Block RED_MEDITATION_MAT = registerWithItem("red_meditation_mat", new MeditationMatBlock(2.0,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.5f).sound(SoundType.WOOL)
					.noOcclusion().ignitedByLava().pushReaction(PushReaction.DESTROY)));
	/** Displays a Cultivation Fruit; fruit on pedestals near a meditator speeds their cultivation (see SpiritPedestalBlock). */
	public static final Block SPIRIT_PEDESTAL = registerWithItem("spirit_pedestal", new SpiritPedestalBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(1.5f, 6.0f).sound(SoundType.POLISHED_DEEPSLATE)
					.noOcclusion()));

	/** Ginseng, aging in the ground (see GinsengBlock). Planted from its seeds; dug up as the ginseng root, keeping its age. */
	public static final Block GINSENG = register("ginseng", new GinsengBlock(() -> ModItems.GINSENG, () -> ModItems.GINSENG_SEEDS,
			ginsengProperties(MapColor.PLANT)));
	/** The rarer Spirit Ginseng: pale jade leaves, golden berries. */
	public static final Block SPIRIT_GINSENG = register("spirit_ginseng", new GinsengBlock(() -> ModItems.SPIRIT_GINSENG,
			() -> ModItems.SPIRIT_GINSENG_SEEDS, ginsengProperties(MapColor.COLOR_LIGHT_GREEN)));

	/**
	 * More aging herbs for alchemy, each with a rarer variant (see GinsengBlock): Lingzhi (a lacquered red shelf fungus) and
	 * Purple Lingzhi; Huangjing (Solomon's seal) and Ochre Huangjing. Like ginseng, dug up as items that keep their age.
	 */
	public static final Block LINGZHI = register("lingzhi", new GinsengBlock(() -> ModItems.LINGZHI, () -> ModItems.LINGZHI_SPORES,
			ginsengProperties(MapColor.COLOR_RED)));
	public static final Block PURPLE_LINGZHI = register("purple_lingzhi", new GinsengBlock(() -> ModItems.PURPLE_LINGZHI,
			() -> ModItems.PURPLE_LINGZHI_SPORES, ginsengProperties(MapColor.COLOR_PURPLE)));
	public static final Block HUANGJING = register("huangjing", new GinsengBlock(() -> ModItems.HUANGJING, () -> ModItems.HUANGJING_SEEDS,
			ginsengProperties(MapColor.PLANT)));
	public static final Block OCHRE_HUANGJING = register("ochre_huangjing", new GinsengBlock(() -> ModItems.OCHRE_HUANGJING,
			() -> ModItems.OCHRE_HUANGJING_SEEDS, ginsengProperties(MapColor.COLOR_ORANGE)));
	/** The Spirit Lotus: an aging herb that floats on still water (see SpiritLotusBlock). */
	public static final Block SPIRIT_LOTUS = register("spirit_lotus", new SpiritLotusBlock(() -> ModItems.SPIRIT_LOTUS,
			() -> ModItems.SPIRIT_LOTUS_SEEDS, ginsengProperties(MapColor.COLOR_YELLOW)));
	/** Grass that gathers Spirit Dew (see SpiritDewGrassBlock). */
	public static final Block SPIRIT_DEW_GRASS = registerWithItem("spirit_dew_grass", new SpiritDewGrassBlock(
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission().instabreak().sound(SoundType.GRASS)
					.pushReaction(PushReaction.DESTROY).randomTicks()));

	/** Spirit Peach Tree (see SpiritPeachTree): the sapling a peach pit grows into, its leaves, and the heart at its base. */
	public static final Block SPIRIT_PEACH_SAPLING = register("spirit_peach_sapling",
			new SpiritPeachSaplingBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_SAPLING)));
	public static final Block SPIRIT_PEACH_LEAVES = registerWithItem("spirit_peach_leaves",
			new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.FLOWERING_AZALEA_LEAVES)));
	public static final Block SPIRIT_PEACH_HEART = register("spirit_peach_heart",
			// Its own properties, not a copy of the cherry log's: a log's map colour reads its axis, which the heart doesn't have.
			new SpiritPeachHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).instrument(NoteBlockInstrument.BASS)
					.strength(2.0f).sound(SoundType.CHERRY_WOOD).ignitedByLava()));

	/** Blue Spirit Wood: spruce-dark timber with blue qi pulsing up its veins (the glow is an animated texture). */
	public static final Block BLUE_SPIRIT_LOG = registerWithItem("blue_spirit_log",
			new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_LOG).mapColor(MapColor.PODZOL)));
	public static final Block BLUE_SPIRIT_WOOD = registerWithItem("blue_spirit_wood",
			new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.SPRUCE_WOOD).mapColor(MapColor.PODZOL)));
	public static final Block BLUE_SPIRIT_PLANKS = registerWithItem("blue_spirit_planks",
			new Block(BlockBehaviour.Properties.copy(Blocks.SPRUCE_PLANKS).mapColor(MapColor.PODZOL)));

	/** Where pills are brewed: water, a fire beneath, then the ingredients (see AlchemyCauldronBlock). */
	public static final Block ALCHEMY_CAULDRON = registerWithItem("alchemy_cauldron",
			new AlchemyCauldronBlock(BlockBehaviour.Properties.copy(Blocks.CAULDRON).mapColor(MapColor.TERRACOTTA_ORANGE)));

	private static BlockBehaviour.Properties ginsengProperties(MapColor colour) {
		return BlockBehaviour.Properties.of().mapColor(colour).noCollission().noOcclusion().instabreak()
				.sound(SoundType.SWEET_BERRY_BUSH).pushReaction(PushReaction.DESTROY).ignitedByLava();
	}

	private static Block register(String id, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, DefyingTheHeavens.id(id), block);
	}

	private static Block registerWithItem(String id, Block block) {
		register(id, block);
		Registry.register(BuiltInRegistries.ITEM, DefyingTheHeavens.id(id), new BlockItem(block, new Item.Properties()));
		return block;
	}

	/** Touching this class registers the blocks; also wires flammability and creative tabs. */
	public static void register() {
		FlammableBlockRegistry.getDefaultInstance().add(WHITE_BLOSSOM_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(SPIRIT_PEACH_LEAVES, 30, 60);
		FlammableBlockRegistry.getDefaultInstance().add(SPIRIT_PEACH_HEART, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(MEDITATION_MAT, 60, 20);
		FlammableBlockRegistry.getDefaultInstance().add(RED_MEDITATION_MAT, 60, 20);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_LOG, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_WOOD, 5, 5);
		FlammableBlockRegistry.getDefaultInstance().add(BLUE_SPIRIT_PLANKS, 5, 20);
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(MEDITATION_MAT);
			entries.accept(RED_MEDITATION_MAT);
			entries.accept(SPIRIT_PEDESTAL);
			entries.accept(ALCHEMY_CAULDRON);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(JADE_STONE);
			entries.accept(JADE_ORE);
			entries.accept(DEEPSLATE_JADE_ORE);
			entries.accept(WHITE_BLOSSOM_LEAVES);
			entries.accept(SPIRIT_PEACH_LEAVES);
			entries.accept(BLUE_SPIRIT_LOG);
			entries.accept(SPIRIT_DEW_GRASS);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(BLUE_SPIRIT_LOG);
			entries.accept(BLUE_SPIRIT_WOOD);
			entries.accept(BLUE_SPIRIT_PLANKS);
		});
	}

	private ModBlocks() {}
}
