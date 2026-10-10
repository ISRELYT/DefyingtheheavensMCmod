package com.example.defyingtheheavens;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The materials a sect builds with, by its path (see {@link Alignment}):
 * <ul>
 *   <li>Righteous: white plaster walls on grey stone, red-lacquered pillars, dark slate tiles with bronze ridges.</li>
 *   <li>Neutral: white plaster, pillars of Blue Spirit Wood, teal prismarine tiles.</li>
 *   <li>Demonic: black stone, crimson pillars, dark nether-brick tiles with red trim, soul-fire lanterns.</li>
 * </ul>
 * In the Upper Realm the trim turns to jade and the courtyards bloom white.
 * <p>
 * Nothing here may be a block springs can open in (vanilla's spring_water and spring_lava take stone, granite, diorite,
 * andesite, deepslate, tuff, calcite and dirt): springs are placed after structures, and would pour water or lava out of
 * the halls' walls. That is why the plaster is smooth quartz, not calcite.
 */
public final class SectPalette {
	public final Alignment.Faction faction;
	public final boolean upper;
	public final Block foundation, paving, path, wallBase, wall, pillar, beam, floor, roof, roofTrim, accent, window, carpet, carpetAlt;
	public final StairBlock roofStairs, baseStairs;
	public final SlabBlock roofSlab, baseSlab;
	public final Block baseWall;
	public final Block lantern, candle, banner, wallBanner, leaves, trunk;
	public final boolean soulFire;

	private SectPalette(Alignment.Faction faction, boolean upper) {
		this.faction = faction;
		this.upper = upper;
		switch (faction) {
			case DEMONIC -> {
				foundation = Blocks.POLISHED_BLACKSTONE_BRICKS;
				paving = Blocks.POLISHED_BLACKSTONE;
				path = Blocks.CHISELED_POLISHED_BLACKSTONE;
				wallBase = Blocks.POLISHED_BLACKSTONE_BRICKS;
				wall = Blocks.BLACKSTONE;
				pillar = Blocks.STRIPPED_CRIMSON_STEM;
				beam = Blocks.CRIMSON_PLANKS;
				floor = Blocks.CRIMSON_PLANKS;
				roof = Blocks.NETHER_BRICKS;
				roofStairs = (StairBlock) Blocks.NETHER_BRICK_STAIRS;
				roofSlab = (SlabBlock) Blocks.NETHER_BRICK_SLAB;
				roofTrim = Blocks.RED_NETHER_BRICKS;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.GILDED_BLACKSTONE;
				window = Blocks.CRIMSON_FENCE;
				carpet = Blocks.RED_CARPET;
				carpetAlt = Blocks.BLACK_CARPET;
				baseStairs = (StairBlock) Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS;
				baseSlab = (SlabBlock) Blocks.POLISHED_BLACKSTONE_BRICK_SLAB;
				baseWall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL;
				lantern = Blocks.SOUL_LANTERN;
				candle = Blocks.RED_CANDLE;
				banner = Blocks.BLACK_BANNER;
				wallBanner = Blocks.RED_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.CHERRY_LEAVES;
				trunk = Blocks.DARK_OAK_LOG;
				soulFire = true;
			}
			case NEUTRAL -> {
				foundation = Blocks.STONE_BRICKS;
				paving = Blocks.POLISHED_DIORITE;
				path = Blocks.SMOOTH_STONE;
				wallBase = Blocks.STONE_BRICKS;
				wall = Blocks.SMOOTH_QUARTZ;
				pillar = ModBlocks.BLUE_SPIRIT_LOG;
				beam = Blocks.STRIPPED_SPRUCE_LOG;
				floor = Blocks.SPRUCE_PLANKS;
				roof = Blocks.DARK_PRISMARINE;
				roofStairs = (StairBlock) Blocks.DARK_PRISMARINE_STAIRS;
				roofSlab = (SlabBlock) Blocks.DARK_PRISMARINE_SLAB;
				roofTrim = Blocks.PRISMARINE_BRICKS;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.PRISMARINE_BRICKS;
				window = Blocks.SPRUCE_FENCE;
				carpet = Blocks.BLUE_CARPET;
				carpetAlt = Blocks.LIGHT_BLUE_CARPET;
				baseStairs = (StairBlock) Blocks.STONE_BRICK_STAIRS;
				baseSlab = (SlabBlock) Blocks.STONE_BRICK_SLAB;
				baseWall = Blocks.STONE_BRICK_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.LIGHT_BLUE_CANDLE;
				banner = Blocks.BLUE_BANNER;
				wallBanner = Blocks.BLUE_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.SPRUCE_LEAVES;
				trunk = Blocks.SPRUCE_LOG;
				soulFire = false;
			}
			default -> {
				foundation = Blocks.STONE_BRICKS;
				paving = Blocks.POLISHED_ANDESITE;
				path = Blocks.SMOOTH_STONE;
				wallBase = Blocks.STONE_BRICKS;
				wall = Blocks.SMOOTH_QUARTZ;
				pillar = Blocks.STRIPPED_MANGROVE_LOG;
				beam = Blocks.STRIPPED_DARK_OAK_LOG;
				floor = Blocks.DARK_OAK_PLANKS;
				roof = Blocks.DEEPSLATE_TILES;
				roofStairs = (StairBlock) Blocks.DEEPSLATE_TILE_STAIRS;
				roofSlab = (SlabBlock) Blocks.DEEPSLATE_TILE_SLAB;
				roofTrim = Blocks.WAXED_CUT_COPPER;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.WAXED_CUT_COPPER;
				window = Blocks.DARK_OAK_FENCE;
				carpet = Blocks.RED_CARPET;
				carpetAlt = Blocks.WHITE_CARPET;
				baseStairs = (StairBlock) Blocks.STONE_BRICK_STAIRS;
				baseSlab = (SlabBlock) Blocks.STONE_BRICK_SLAB;
				baseWall = Blocks.STONE_BRICK_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.WHITE_CANDLE;
				banner = Blocks.RED_BANNER;
				wallBanner = Blocks.RED_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.CHERRY_LEAVES;
				trunk = Blocks.CHERRY_LOG;
				soulFire = false;
			}
		}
	}

	private static final SectPalette[][] CACHE = new SectPalette[Alignment.Faction.values().length][2];

	public static SectPalette of(Alignment.Faction faction, boolean upper) {
		SectPalette palette = CACHE[faction.ordinal()][upper ? 1 : 0];
		if (palette == null) {
			palette = new SectPalette(faction, upper);
			CACHE[faction.ordinal()][upper ? 1 : 0] = palette;
		}
		return palette;
	}

	public BlockState pillar(Direction.Axis axis) {
		BlockState state = pillar.defaultBlockState();
		return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
	}

	public BlockState beam(Direction.Axis axis) {
		BlockState state = beam.defaultBlockState();
		return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
	}

	/** A roof stair whose tall side faces {@code toward} (into the roof). */
	public BlockState roofStair(Direction toward, boolean upsideDown) {
		return roofStairs.defaultBlockState().setValue(StairBlock.FACING, toward).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
	}

	public BlockState roofSlab(boolean top) {
		return roofSlab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public BlockState baseStair(Direction toward) {
		return baseStairs.defaultBlockState().setValue(StairBlock.FACING, toward);
	}

	public BlockState baseSlab(boolean top) {
		return baseSlab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}
}
