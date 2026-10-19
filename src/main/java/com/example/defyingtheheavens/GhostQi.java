package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

/**
 * Ghostly qi (阴气): the second kind of qi, which belongs to places rather than to the cultivator who gathers it. It is what ghost
 * methods are cultivated on, as fast as it is dense (GhostMethods#ratePerSecond), and what the ghostly qi pool fills from.
 * <p>
 * Density, a Soul Sand Valley being 1:
 * <table>
 *   <tr><td>The Netherworld</td><td>{@link #NETHERWORLD} (x10)</td></tr>
 *   <tr><td>The Nine Nether Peaks</td><td>{@link #NETHERWORLD} x {@link #PEAKS_FACTOR} (x12)</td></tr>
 *   <tr><td>A Soul Sand Valley</td><td>{@link #SOUL_SAND_VALLEY} (x1)</td></tr>
 *   <tr><td>The rest of the Nether</td><td>{@link #NETHER} (x0.5)</td></tr>
 *   <tr><td>The Overworld</td><td>{@link #OVERWORLD} (x0.25)</td></tr>
 *   <tr><td>Everywhere else</td><td>0</td></tr>
 * </table>
 * The ghostly qi pool fills at density x {@link #FILL_PER_DENSITY} a second: 2 a second in the Netherworld (2.4 on its peaks),
 * 0.2 in a Soul Sand Valley, 0.1 in the rest of the Nether, 0.05 in the Overworld, nothing elsewhere. Spirit stones and Spirit Dew
 * don't fill it.
 */
public final class GhostQi {
	public static final double NETHERWORLD = 10.0;
	public static final double PEAKS_FACTOR = 1.2;
	public static final double SOUL_SAND_VALLEY = 1.0;
	/** The rest of the Nether: half a Soul Sand Valley's (it was the Overworld's until 2026-10-19, when the user swapped the two). */
	public static final double NETHER = 0.5;
	/**
	 * The living world's own faint yin, everywhere in the Overworld (the user's ask, 2026-10-18): a quarter of a Soul Sand Valley's,
	 * the thinnest anywhere there is any (2026-10-19, the user's: the Overworld the worst place for the ghost path).
	 */
	public static final double OVERWORLD = 0.25;
	/** The pool fills at the density times this, a second. */
	public static final double FILL_PER_DENSITY = 0.2;
	/**
	 * The pool's size: {@link #BASE_POOL} for anyone who has one (every ghost, and everyone who knows a ghost method), and
	 * {@link #POOL_PER_LAYER} more for every layer known across all methods (each foundation's layers add their own on top).
	 */
	public static final double BASE_POOL = 30.0;
	public static final double POOL_PER_LAYER = 10.0;
	/** A foundation's own addition to the pool, a layer (the Netherworld Breathing Art's and those after it: GhostMethod#isFoundation). */
	public static final double BREATHING_POOL_PER_LAYER = 20.0;

	/** The ghostly qi density at a place: one pure function, so tests can call it. */
	public static double density(ResourceKey<Level> dimension, Holder<Biome> biome) {
		if (ModDimensions.isNetherworld(dimension)) {
			return biome != null && biome.is(ModBiomes.NINE_NETHER_PEAKS) ? NETHERWORLD * PEAKS_FACTOR : NETHERWORLD;
		}
		if (dimension == Level.NETHER) {
			return biome != null && biome.is(Biomes.SOUL_SAND_VALLEY) ? SOUL_SAND_VALLEY : NETHER;
		}
		if (dimension == Level.OVERWORLD) return OVERWORLD;
		return 0.0;
	}

	/** The density at a position in a level. */
	public static double densityAt(ServerLevel level, BlockPos pos) {
		ResourceKey<Level> dimension = level.dimension();
		if (dimension == Level.OVERWORLD) return OVERWORLD; // the same everywhere in it: no biome to look up
		if (!ModDimensions.isNetherworld(dimension) && dimension != Level.NETHER) return 0.0;
		return density(dimension, level.getBiome(pos));
	}

	/**
	 * The pool's size: nothing without a pool; otherwise {@link #BASE_POOL}, {@link #POOL_PER_LAYER} for every layer known (the
	 * foundations' depth, GhostMethods#layersKnown, with the fire's and the steps'), and {@link #BREATHING_POOL_PER_LAYER} more for
	 * each of the foundations' depth. A ghost who knows all nine layers of the Breathing Art, the fire and the steps holds 30 + 27 x
	 * 10 + 9 x 20 = 480; the Six Paths Reincarnation Canon's 54 with them, 30 + 72 x 10 + 54 x 20 = 1,830.
	 */
	public static double poolMax(boolean hasPool, int layersKnown, int breathingLayers) {
		if (!hasPool) return 0;
		return BASE_POOL + POOL_PER_LAYER * Math.max(0, layersKnown) + BREATHING_POOL_PER_LAYER * Math.max(0, breathingLayers);
	}

	/**
	 * This cultivator's pool size. Every ghost, and everyone who knows a ghost method, has one; a mortal's layers add nothing to it,
	 * its methods dormant (GhostMethods#refusal).
	 */
	public static double poolMax(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		return poolMax(s.ghost() || s.knowsAnyMethod(), GhostMethods.layersKnown(c), GhostMethods.foundationLayers(c));
	}

	/** Ghostly qi filled a second at {@code density}. */
	public static double fillPerSecond(double density) {
		return density * FILL_PER_DENSITY;
	}

	/** The density's word on the cultivation menu: dense (the Netherworld), thin (a Soul Sand Valley), faint (the Overworld, the Nether), none. */
	public static String word(double density) {
		if (density >= NETHERWORLD) return "dense";
		if (density >= SOUL_SAND_VALLEY) return "thin";
		if (density > 0) return "faint";
		return "none";
	}

	private GhostQi() {}
}
