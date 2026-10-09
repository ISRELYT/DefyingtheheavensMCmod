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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
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
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(entries -> {
			entries.accept(JADE_STONE);
			entries.accept(WHITE_BLOSSOM_LEAVES);
		});
	}

	private ModBlocks() {}
}
