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
 *   <li>Devilish: black stone, crimson pillars, dark nether-brick tiles with red trim, soul-fire lanterns.</li>
 * </ul>
 * The trim (the roofs' ridges, eaves and upturned corners, the gates' lintels) is bronze for the righteous, prismarine for the
 * neutral and red nether brick for the devilish.
 * In the Upper Realm the accents turn to jade and the trees bloom white.
 * <p>
 * Nothing here may be a block springs can open in (vanilla's spring_water and spring_lava take stone, granite, diorite,
 * andesite, deepslate, tuff, calcite and dirt): springs are placed after structures, and would pour water or lava out of
 * the halls' walls. That is why the plaster is smooth quartz, not calcite.
 */
public final class SectPalette {
	public final Alignment.Faction faction;
	public final boolean upper;
	public final Block foundation, paving, path, wallBase, wall, pillar, beam, floor, roof, roofTrim, accent, window, carpet, carpetAlt;
	public final StairBlock roofStairs, baseStairs, trimStairs;
	public final SlabBlock roofSlab, baseSlab, trimSlab;
	public final Block baseWall;
	public final Block lantern, candle, banner, wallBanner, leaves, trunk;
	public final boolean soulFire;
	/** The people it builds for: men (a sect, in its path's colours), or a demon race (a clan's den: {@link #ofRace}). */
	public final Bloodline race;

	private SectPalette(Alignment.Faction faction, boolean upper) {
		this.faction = faction;
		this.upper = upper;
		this.race = Bloodline.HUMAN;
		switch (faction) {
			case DEVILISH -> {
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
				trimStairs = (StairBlock) Blocks.RED_NETHER_BRICK_STAIRS;
				trimSlab = (SlabBlock) Blocks.RED_NETHER_BRICK_SLAB;
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
				trimStairs = (StairBlock) Blocks.PRISMARINE_BRICK_STAIRS;
				trimSlab = (SlabBlock) Blocks.PRISMARINE_BRICK_SLAB;
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
				trimStairs = (StairBlock) Blocks.WAXED_CUT_COPPER_STAIRS;
				trimSlab = (SlabBlock) Blocks.WAXED_CUT_COPPER_SLAB;
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

	/**
	 * A demon clan's den (DenStructure, DenPieces), by its race: for the grounds, the plaza round the core, the lights along the trails,
	 * the trees, the gate and the chests. Its ways are trails, not paving:
	 * <ul>
	 *   <li>Tiger: coarse dirt and podzol trails edged with mossy cobble, spruce, lanterns on mossy cobble posts.</li>
	 *   <li>Lizard: packed mud and rooted dirt edged with mud brick, mangroves, lanterns on mud brick posts.</li>
	 *   <li>Fox: gravel edged with stone brick, cherry trees, stone lanterns, vermilion torii.</li>
	 *   <li>Dragon: worked stone and tuff edged with deepslate tiles, firs, lanterns on deepslate posts; jade at the core.</li>
	 * </ul>
	 * Nothing springs open in (see the class's note): no plain stone, dirt, deepslate or tuff in what stands, only underfoot.
	 */
	private SectPalette(Bloodline race, boolean upper) {
		this.faction = Alignment.Faction.NEUTRAL;
		this.upper = upper;
		this.race = race;
		switch (race) {
			case LIZARD -> {
				foundation = Blocks.MUD_BRICKS;
				paving = Blocks.ROOTED_DIRT;
				path = Blocks.PACKED_MUD;
				wallBase = Blocks.MUD_BRICKS;
				wall = Blocks.PACKED_MUD;
				pillar = Blocks.MANGROVE_LOG;
				beam = Blocks.STRIPPED_MANGROVE_LOG;
				floor = Blocks.PACKED_MUD;
				roof = Blocks.MUD_BRICKS;
				roofStairs = (StairBlock) Blocks.MUD_BRICK_STAIRS;
				roofSlab = (SlabBlock) Blocks.MUD_BRICK_SLAB;
				roofTrim = Blocks.MOSS_BLOCK;
				trimStairs = (StairBlock) Blocks.MANGROVE_STAIRS;
				trimSlab = (SlabBlock) Blocks.MANGROVE_SLAB;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.MOSS_BLOCK;
				window = Blocks.MANGROVE_FENCE;
				carpet = Blocks.MOSS_CARPET;
				carpetAlt = Blocks.GREEN_CARPET;
				baseStairs = (StairBlock) Blocks.MUD_BRICK_STAIRS;
				baseSlab = (SlabBlock) Blocks.MUD_BRICK_SLAB;
				baseWall = Blocks.MUD_BRICK_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.LIME_CANDLE;
				banner = Blocks.GREEN_BANNER;
				wallBanner = Blocks.GREEN_WALL_BANNER;
				leaves = Blocks.MANGROVE_LEAVES;
				trunk = Blocks.MANGROVE_LOG;
				soulFire = false;
			}
			case FOX -> {
				foundation = Blocks.STONE_BRICKS;
				paving = Blocks.GRAVEL;
				path = Blocks.GRAVEL;
				wallBase = Blocks.STONE_BRICKS;
				wall = Blocks.SMOOTH_QUARTZ;
				pillar = Blocks.STRIPPED_MANGROVE_LOG; // vermilion
				beam = Blocks.STRIPPED_DARK_OAK_LOG;
				floor = Blocks.CHERRY_PLANKS;
				roof = Blocks.DEEPSLATE_TILES;
				roofStairs = (StairBlock) Blocks.DEEPSLATE_TILE_STAIRS;
				roofSlab = (SlabBlock) Blocks.DEEPSLATE_TILE_SLAB;
				roofTrim = Blocks.RED_NETHER_BRICKS;
				trimStairs = (StairBlock) Blocks.RED_NETHER_BRICK_STAIRS;
				trimSlab = (SlabBlock) Blocks.RED_NETHER_BRICK_SLAB;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.RED_NETHER_BRICKS;
				window = Blocks.CHERRY_FENCE;
				carpet = Blocks.RED_CARPET;
				carpetAlt = Blocks.WHITE_CARPET;
				baseStairs = (StairBlock) Blocks.STONE_BRICK_STAIRS;
				baseSlab = (SlabBlock) Blocks.STONE_BRICK_SLAB;
				baseWall = Blocks.STONE_BRICK_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.RED_CANDLE;
				banner = Blocks.RED_BANNER;
				wallBanner = Blocks.RED_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.CHERRY_LEAVES;
				trunk = Blocks.CHERRY_LOG;
				soulFire = false;
			}
			case DRAGON -> {
				foundation = Blocks.DEEPSLATE_TILES;
				paving = Blocks.TUFF;
				path = Blocks.COBBLED_DEEPSLATE;
				wallBase = Blocks.DEEPSLATE_TILES;
				wall = Blocks.DEEPSLATE_BRICKS;
				pillar = Blocks.POLISHED_BASALT;
				beam = Blocks.STRIPPED_DARK_OAK_LOG;
				floor = Blocks.POLISHED_DEEPSLATE;
				roof = Blocks.DEEPSLATE_TILES;
				roofStairs = (StairBlock) Blocks.DEEPSLATE_TILE_STAIRS;
				roofSlab = (SlabBlock) Blocks.DEEPSLATE_TILE_SLAB;
				roofTrim = Blocks.GOLD_BLOCK;
				trimStairs = (StairBlock) Blocks.POLISHED_BLACKSTONE_STAIRS;
				trimSlab = (SlabBlock) Blocks.POLISHED_BLACKSTONE_SLAB;
				accent = ModBlocks.JADE_STONE;
				window = Blocks.DARK_OAK_FENCE;
				carpet = Blocks.CYAN_CARPET;
				carpetAlt = Blocks.YELLOW_CARPET;
				baseStairs = (StairBlock) Blocks.DEEPSLATE_TILE_STAIRS;
				baseSlab = (SlabBlock) Blocks.DEEPSLATE_TILE_SLAB;
				baseWall = Blocks.DEEPSLATE_TILE_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.YELLOW_CANDLE;
				banner = Blocks.CYAN_BANNER;
				wallBanner = Blocks.CYAN_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.SPRUCE_LEAVES;
				trunk = Blocks.SPRUCE_LOG;
				soulFire = false;
			}
			default -> { // TIGER (and a man's, which never builds a den)
				foundation = Blocks.MOSSY_COBBLESTONE;
				paving = Blocks.PODZOL;
				path = Blocks.COARSE_DIRT;
				wallBase = Blocks.MOSSY_COBBLESTONE;
				wall = Blocks.COBBLESTONE;
				pillar = Blocks.STRIPPED_SPRUCE_LOG;
				beam = Blocks.SPRUCE_LOG;
				floor = Blocks.COARSE_DIRT;
				roof = Blocks.MOSSY_COBBLESTONE;
				roofStairs = (StairBlock) Blocks.MOSSY_COBBLESTONE_STAIRS;
				roofSlab = (SlabBlock) Blocks.MOSSY_COBBLESTONE_SLAB;
				roofTrim = Blocks.MOSSY_STONE_BRICKS;
				trimStairs = (StairBlock) Blocks.MOSSY_STONE_BRICK_STAIRS;
				trimSlab = (SlabBlock) Blocks.MOSSY_STONE_BRICK_SLAB;
				accent = upper ? ModBlocks.JADE_STONE : Blocks.MOSSY_STONE_BRICKS;
				window = Blocks.SPRUCE_FENCE;
				carpet = Blocks.BROWN_CARPET;
				carpetAlt = Blocks.ORANGE_CARPET;
				baseStairs = (StairBlock) Blocks.MOSSY_COBBLESTONE_STAIRS;
				baseSlab = (SlabBlock) Blocks.MOSSY_COBBLESTONE_SLAB;
				baseWall = Blocks.MOSSY_COBBLESTONE_WALL;
				lantern = Blocks.LANTERN;
				candle = Blocks.ORANGE_CANDLE;
				banner = Blocks.ORANGE_BANNER;
				wallBanner = Blocks.ORANGE_WALL_BANNER;
				leaves = upper ? ModBlocks.WHITE_BLOSSOM_LEAVES : Blocks.SPRUCE_LEAVES;
				trunk = Blocks.SPRUCE_LOG;
				soulFire = false;
			}
		}
	}

	private static final SectPalette[][] RACE_CACHE = new SectPalette[Bloodline.values().length][2];

	/** A demon clan's den's palette (see the race constructor), cached. */
	public static SectPalette ofRace(Bloodline race, boolean upper) {
		SectPalette palette = RACE_CACHE[race.ordinal()][upper ? 1 : 0];
		if (palette == null) {
			palette = new SectPalette(race, upper);
			RACE_CACHE[race.ordinal()][upper ? 1 : 0] = palette;
		}
		return palette;
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

	/** A stair of the trim (a red eave, a ridge's end, a gate's upswept lintel), its tall side facing {@code toward}. */
	public BlockState trimStair(Direction toward, boolean upsideDown) {
		return trimStairs.defaultBlockState().setValue(StairBlock.FACING, toward).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
	}

	public BlockState trimSlab(boolean top) {
		return trimSlab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public BlockState baseStair(Direction toward) {
		return baseStairs.defaultBlockState().setValue(StairBlock.FACING, toward);
	}

	public BlockState baseSlab(boolean top) {
		return baseSlab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}
}
