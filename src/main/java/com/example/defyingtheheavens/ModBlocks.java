package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
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

	/** Wild ginseng, aging in the ground (see GinsengBlock). No block item: it is dug up as the ginseng item instead. */
	public static final Block GINSENG = register("ginseng", new GinsengBlock(() -> ModItems.GINSENG, ginsengProperties(MapColor.PLANT)));
	/** The rarer Spirit Ginseng: pale jade leaves, golden berries. */
	public static final Block SPIRIT_GINSENG = register("spirit_ginseng", new GinsengBlock(() -> ModItems.SPIRIT_GINSENG,
			ginsengProperties(MapColor.COLOR_LIGHT_GREEN)));

	/** Spirit Peach Tree (see SpiritPeachTree): the sapling a peach pit grows into, its leaves, and the heart at its base. */
	public static final Block SPIRIT_PEACH_SAPLING = register("spirit_peach_sapling",
			new SpiritPeachSaplingBlock(BlockBehaviour.Properties.copy(Blocks.CHERRY_SAPLING)));
	public static final Block SPIRIT_PEACH_LEAVES = registerWithItem("spirit_peach_leaves",
			new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.FLOWERING_AZALEA_LEAVES)));
	public static final Block SPIRIT_PEACH_HEART = register("spirit_peach_heart",
			// Its own properties, not a copy of the cherry log's: a log's map colour reads its axis, which the heart doesn't have.
			new SpiritPeachHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).instrument(NoteBlockInstrument.BASS)
					.strength(2.0f).sound(SoundType.CHERRY_WOOD).ignitedByLava()));

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
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
			entries.accept(MEDITATION_MAT);
			entries.accept(RED_MEDITATION_MAT);
			entries.accept(SPIRIT_PEDESTAL);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(JADE_STONE);
			entries.accept(WHITE_BLOSSOM_LEAVES);
			entries.accept(SPIRIT_PEACH_LEAVES);
		});
	}

	private ModBlocks() {}
}
