package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How the heavens weigh a Ritual to the Heavens (see {@link HeavenRitual}): the offering, the altar, and how high it stands.
 * <ul>
 *   <li><b>The offering</b> is everything laid on the Heaven Altar, valued as cultivators value things (TalkSystem#worth, in
 *   Low spirit stones): under a Mid stone's worth it is {@link Offering#UNWORTHY} and counts as nothing at all; from there
 *   Proper (the heavens' leave), Generous, Lavish and Heavenly (their leave and Heaven's Favour as well).</li>
 *   <li><b>The altar</b> is judged freely, by what is built round the Heaven Altar block (see {@link #judge}): the platform it
 *   stands on, terraces stepping down from it, noble stone, pillars, incense and lights, treasures on pedestals. Each counts
 *   only so far, so only an altar done well in several ways is Magnificent. The heavens must be able to see it.</li>
 *   <li><b>Height</b>: a point for every ten blocks above sea level, up to {@link #MAX_HEIGHT_BONUS}.</li>
 * </ul>
 * The chance is the offering's base plus the altar's and the height's percentages, at most {@link #MAX_CHANCE}; with no
 * offering worth the name it is a sliver ({@link #UNWORTHY_CHANCE} to {@link #UNWORTHY_MAX_CHANCE}).
 */
public final class HeavenAltar {
	/** How far round the altar block the altar is judged. */
	public static final int REACH = 6;
	/** Height bonus: a percent for every this many blocks above sea level, at most {@link #MAX_HEIGHT_BONUS}. */
	public static final int BLOCKS_PER_HEIGHT_POINT = 10;
	public static final int MAX_HEIGHT_BONUS = 15;
	public static final double MAX_CHANCE = 0.95;
	/** With no offering worth the name: this chance, rising to {@link #UNWORTHY_MAX_CHANCE} at the grandest altar and height. */
	public static final double UNWORTHY_CHANCE = 0.005;
	public static final double UNWORTHY_MAX_CHANCE = 0.02;

	/** What an offering comes to: the worth it starts from (Low stones), its chance before altar and height, the Favour it earns. */
	public enum Offering {
		UNWORTHY("unworthy", 0, UNWORTHY_CHANCE, 0),
		PROPER("proper", 9, 0.40, 0),
		GENEROUS("generous", 81, 0.55, 1),
		LAVISH("lavish", 729, 0.70, 2),
		HEAVENLY("heavenly", 6561, 0.80, 3);

		public final String id;
		public final double minWorth;
		public final double baseChance;
		/** Steps of Heaven's Favour a successful ritual with this offering grants (see PlayerCultivation#favourFactor). */
		public final int favour;

		Offering(String id, double minWorth, double baseChance, int favour) {
			this.id = id;
			this.minWorth = minWorth;
			this.baseChance = baseChance;
			this.favour = favour;
		}

		public static Offering of(double worth) {
			Offering best = UNWORTHY;
			for (Offering o : values()) if (worth >= o.minWorth) best = o;
			return best;
		}

		public Component displayName() { return Component.translatable(ModLang.ritualOffering(id)); }
	}

	/** The altar's standing, by its points: Humble, Modest, Grand, Magnificent, each adding 10% more. */
	public enum Grandeur {
		HUMBLE("humble", 0, 0),
		MODEST("modest", 10, 10),
		GRAND("grand", 20, 20),
		MAGNIFICENT("magnificent", 30, 30);

		public final String id;
		public final int minPoints;
		public final int percent;

		Grandeur(String id, int minPoints, int percent) {
			this.id = id;
			this.minPoints = minPoints;
			this.percent = percent;
		}

		public static Grandeur of(int points) {
			Grandeur best = HUMBLE;
			for (Grandeur g : values()) if (points >= g.minPoints) best = g;
			return best;
		}

		public Component displayName() { return Component.translatable(ModLang.ritualGrandeur(id)); }
	}

	/** An altar as judged: its points from each thing counted, and whether the heavens can see it. */
	public record Judgement(boolean sky, int platform, int terraces, int noble, int pillars, int lights, int treasures) {
		public int points() { return platform + terraces + noble + pillars + lights + treasures; }

		public Grandeur grandeur() { return Grandeur.of(points()); }
	}

	// --- Offerings ---

	/** What a heap of offerings is worth to the heavens: each item as cultivators value it, times how many. */
	public static double worth(Container offerings) {
		double total = 0;
		for (int i = 0; i < offerings.getContainerSize(); i++) total += worth(offerings.getItem(i));
		return total;
	}

	public static double worth(ItemStack stack) {
		if (stack.isEmpty()) return 0;
		return TalkSystem.worth(stack.copyWithCount(1)) * stack.getCount();
	}

	// --- Height and chance ---

	/** The height bonus (percent) of an altar at {@code pos}. */
	public static int heightBonus(Level level, BlockPos pos) {
		int above = pos.getY() - level.getSeaLevel();
		return Math.max(0, Math.min(MAX_HEIGHT_BONUS, above / BLOCKS_PER_HEIGHT_POINT));
	}

	/** The chance the heavens grant their leave, for an offering at an altar of {@code grandeur} with {@code heightBonus}. */
	public static double chance(Offering offering, Grandeur grandeur, int heightBonus) {
		int bonus = grandeur.percent + heightBonus;
		if (offering == Offering.UNWORTHY) {
			double best = Grandeur.MAGNIFICENT.percent + MAX_HEIGHT_BONUS;
			return UNWORTHY_CHANCE + (UNWORTHY_MAX_CHANCE - UNWORTHY_CHANCE) * bonus / best;
		}
		return Math.min(MAX_CHANCE, offering.baseChance + bonus / 100.0);
	}

	/** "40%", or "0.5%" for the slivers. */
	public static String percent(double chance) {
		double p = chance * 100;
		return p < 10 ? String.format(java.util.Locale.ROOT, "%.1f%%", p) : Math.round(p) + "%";
	}

	// --- Judging the altar ---

	/**
	 * Judges the altar round a Heaven Altar block at {@code pos}:
	 * <ul>
	 *   <li><b>Platform</b>: the widest square (3x3 to 9x9) of solid ground the altar stands in the middle of, raised above
	 *   what is round it (a lawn is no platform): 2 / 5 / 8 / 10.</li>
	 *   <li><b>Terraces</b>: up to three wider rings stepping down from it, each a block to three lower: 3 each.</li>
	 *   <li><b>Noble stone</b>: polished, carved, tiled and bricked stone, quartz, jade, calcite, precious blocks, as a share
	 *   of the platform: up to 6.</li>
	 *   <li><b>Pillars</b> three blocks tall on the platform: 1 each, up to 4.</li>
	 *   <li><b>Incense and lights</b>: lit candles, lit fires, lanterns: 1 each, up to 5.</li>
	 *   <li><b>Treasures</b> displayed on Spirit Pedestals: 2 each, up to 4.</li>
	 * </ul>
	 */
	public static Judgement judge(Level level, BlockPos pos) {
		boolean sky = openSky(level, pos);
		int floor = pos.getY() - 1;
		int half = platformHalf(level, pos, floor);
		int platform = switch (half) {
			case 1 -> 2;
			case 2 -> 5;
			case 3 -> 8;
			case 4 -> 10;
			default -> 0;
		};
		int terraces = half > 0 ? 3 * terraces(level, pos, floor) : 0;
		int noble = 0;
		if (half > 0) {
			int count = 0, of = 0;
			for (int dx = -half; dx <= half; dx++) {
				for (int dz = -half; dz <= half; dz++) {
					of++;
					if (isNoble(level.getBlockState(pos.offset(dx, -1, dz)))) count++;
				}
			}
			noble = (int) Math.round(6.0 * count / of);
		}
		int pillars = 0, lights = 0, treasures = 0;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dx = -REACH; dx <= REACH; dx++) {
			for (int dz = -REACH; dz <= REACH; dz++) {
				if (dx == 0 && dz == 0) continue;
				// A pillar: three blocks of something solid standing on the platform's level.
				if (half > 0 && Math.max(Math.abs(dx), Math.abs(dz)) <= half + 1) {
					boolean pillar = true;
					for (int dy = 0; dy < 3 && pillar; dy++) {
						BlockState s = level.getBlockState(at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
						pillar = s.blocksMotion();
					}
					if (pillar) pillars++;
				}
				for (int dy = -REACH; dy <= 4; dy++) {
					at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					BlockState s = level.getBlockState(at);
					if (isLight(s)) lights++;
					else if (s.is(ModBlocks.SPIRIT_PEDESTAL) && level.getBlockEntity(at) instanceof SpiritPedestalBlockEntity pedestal
							&& pedestal.hasFruit()) treasures++;
				}
			}
		}
		return new Judgement(sky, platform, terraces, noble, Math.min(4, pillars), Math.min(5, lights), Math.min(4, 2 * treasures));
	}

	/** Solid to stand on at (x, y, z), with nothing solid on top of it. */
	private static boolean standable(Level level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		if (!s.isFaceSturdy(level, pos, Direction.UP)) return false;
		BlockState above = level.getBlockState(pos.above());
		return !above.isCollisionShapeFullBlock(level, pos.above());
	}

	/**
	 * Open sky over the altar: nothing solid anywhere above it, glass and leaves included. A formation's barrier doesn't count
	 * (it is no roof, and a sect's altar stands under one); nor do things that don't stop movement, like a banner or a vine.
	 * Read from the blocks, not the sky light, so a roof counts the moment it's built.
	 */
	public static boolean openSky(Level level, BlockPos pos) {
		int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
		if (pos.getY() + 1 >= top) return true;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int y = pos.getY() + 1; y < level.getMaxBuildHeight(); y++) {
			BlockState state = level.getBlockState(at.set(pos.getX(), y, pos.getZ()));
			if (state.isAir() || state.is(ModBlocks.SECT_BARRIER)) continue;
			if (state.blocksMotion() || !state.getFluidState().isEmpty()) return false;
		}
		return true;
	}

	/** Solid on top at {@code pos} (whatever stands on it: a pillar, a lantern, the altar). */
	private static boolean solid(Level level, BlockPos pos) {
		return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP);
	}

	/**
	 * Half the width of the widest raised square platform under {@code pos} (0: none): solid all over at the altar's floor,
	 * with the ring just outside it, or the one beyond (a round platform's edge falls between them), mostly no floor at that
	 * level.
	 */
	private static int platformHalf(Level level, BlockPos pos, int floor) {
		for (int half = 4; half >= 1; half--) {
			boolean whole = true;
			for (int dx = -half; dx <= half && whole; dx++) {
				for (int dz = -half; dz <= half && whole; dz++) {
					whole = solid(level, new BlockPos(pos.getX() + dx, floor, pos.getZ() + dz));
				}
			}
			if (!whole) continue;
			for (int ring = half + 1; ring <= half + 2; ring++) {
				int open = 0, of = 0;
				for (int[] p : ringPositions(ring)) {
					of++;
					if (!solid(level, new BlockPos(pos.getX() + p[0], floor, pos.getZ() + p[1]))) open++;
				}
				if (open * 10 >= of * 6) return half;
			}
		}
		return 0;
	}

	/** How far out terraces are looked for. */
	private static final int TERRACE_REACH = 16;

	/**
	 * Terraces stepping down from the platform: walking out from the altar in each of the eight directions, the steps down (a
	 * block to three) that come after a stretch of two blocks or more on one level; the last step reaches the ground, so the
	 * terraces are one fewer. Square or round, the terraces count if all but one of the eight ways agree (a stair, say).
	 */
	private static int terraces(Level level, BlockPos pos, int floor) {
		int[][] ways = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
		int[] counts = new int[ways.length];
		for (int i = 0; i < ways.length; i++) {
			int y = floor, run = 2, drops = 0; // the platform itself counts as a stretch
			for (int d = 1; d <= TERRACE_REACH; d++) {
				int x = pos.getX() + ways[i][0] * d, z = pos.getZ() + ways[i][1] * d;
				int surface = Integer.MIN_VALUE;
				for (int dy = 0; dy <= 3; dy++) {
					if (standable(level, new BlockPos(x, y - dy, z))) {
						surface = y - dy;
						break;
					}
				}
				if (surface == Integer.MIN_VALUE) break; // a wall, a cliff: the walk ends here
				if (surface == y) {
					run++;
				} else {
					if (run < 2) break; // a slope or a stair, not a terrace
					drops++;
					y = surface;
					run = 1;
				}
			}
			counts[i] = Math.max(0, Math.min(3, drops - 1));
		}
		java.util.Arrays.sort(counts);
		return counts[1];
	}

	/** The (dx, dz) at Chebyshev distance {@code r}. */
	private static int[][] ringPositions(int r) {
		int[][] out = new int[8 * r][];
		int i = 0;
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) == r) out[i++] = new int[] {dx, dz};
			}
		}
		return out;
	}

	/** Stone worked to honour the heavens: polished, carved, tiled, bricked, quartz, jade, calcite, the precious blocks. */
	public static boolean isNoble(BlockState state) {
		if (state.is(ModBlocks.JADE_STONE) || state.is(Blocks.CALCITE) || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.DIAMOND_BLOCK)
				|| state.is(Blocks.EMERALD_BLOCK) || state.is(Blocks.NETHERITE_BLOCK) || state.is(Blocks.LAPIS_BLOCK)
				|| state.is(Blocks.AMETHYST_BLOCK)) return true;
		String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		if (path.contains("planks") || path.contains("wool") || path.contains("glass")) return false;
		return path.contains("polished") || path.contains("bricks") || path.contains("tiles") || path.startsWith("smooth_")
				|| path.startsWith("chiseled") || path.contains("quartz") || path.contains("prismarine") || path.startsWith("cut_")
				|| path.contains("purpur");
	}

	/** Incense and light for the heavens: lit candles, lit fires, lanterns. */
	public static boolean isLight(BlockState state) {
		if (state.getBlock() instanceof CandleBlock) return state.getValue(CandleBlock.LIT);
		if (state.getBlock() instanceof CampfireBlock) return state.getValue(CampfireBlock.LIT);
		return state.getBlock() instanceof LanternBlock;
	}

	private HeavenAltar() {}
}
