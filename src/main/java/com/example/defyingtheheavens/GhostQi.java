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
 *   <tr><td>The Great Void's ancient battlefields</td><td>{@link #ANCIENT_BATTLEFIELD} (x100)</td></tr>
 *   <tr><td>The secret realm of the Ancient Battlefield</td><td>{@link #SECRET_BATTLEFIELD} (x30)</td></tr>
 *   <tr><td>The Netherworld</td><td>{@link #NETHERWORLD} (x10)</td></tr>
 *   <tr><td>The Nine Nether Peaks</td><td>{@link #NETHERWORLD} x {@link #PEAKS_FACTOR} (x12)</td></tr>
 *   <tr><td>A Soul Sand Valley</td><td>{@link #SOUL_SAND_VALLEY} (x1)</td></tr>
 *   <tr><td>The rest of the Nether</td><td>{@link #NETHER} (x0.5)</td></tr>
 *   <tr><td>The Overworld</td><td>{@link #OVERWORLD} (x0.25)</td></tr>
 *   <tr><td>Everywhere else</td><td>0</td></tr>
 * </table>
 * The ghostly qi pool fills on its own as the qi pool does, {@link #OWN_FILL} a second at Qi Refining Early wherever one is (since
 * 2026-10-31), and the place's ghostly qi adds density x {@link #FILL_PER_DENSITY} to that: 20 a second more on the battlefields, 2 in the
 * Netherworld (2.4 on its peaks), 0.2 in a Soul Sand Valley, 0.1 in the rest of the Nether, 0.05 in the Overworld, nothing elsewhere; the
 * ghost path's realm multiplies both as it grows the pool (since 2026-10-27: {@link #realmPool}, {@link #realmFill}), so from Four Axis on an
 * empty pool fills in about 40 s anywhere, 13 s in the Netherworld, 2 s on a battlefield. Spirit stones and Spirit Dew don't fill it.
 */
public final class GhostQi {
	/**
	 * The Great Void's ancient battlefields (ModBiomes#ANCIENT_BATTLEFIELD), where the armies of Heaven and the demons fell: ten times the
	 * Netherworld's (the user's ask, 2026-10-27: "make it so that ghostly qi is 100X in ancient battlefields in the great void. Then
	 * ghostly cultivators have a place to go for high density not just the netherworld"). The rest of the Great Void has none.
	 */
	public static final double ANCIENT_BATTLEFIELD = 100.0;
	/**
	 * The secret realm of the Ancient Battlefield (SecretRealm#ANCIENT_BATTLEFIELD, 2026-10-31): the dead of an old war lie thick there, three
	 * times the Netherworld's (the design's "yin thick enough for the ghost path"). Any other secret realm: its world's, times its qi.
	 */
	public static final double SECRET_BATTLEFIELD = 30.0;
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
	/** The place's ghostly qi fills the pool at the density times this, a second. */
	public static final double FILL_PER_DENSITY = 0.2;
	/**
	 * What the pool fills a second on its own at Qi Refining Early, wherever one is: the qi pool's own gathering at that rank
	 * (CultivationStats#qiGather), as the qi pool refills (the user's pick, 2026-10-31, "On its own, like qi": "ghostly qi doesnt regen
	 * anywhere really once you reach high enough levels of it", where it had filled only from the place, never in the worlds without yin).
	 */
	public static final double OWN_FILL = CultivationStats.qiGather(Realm.QI_REFINING, Stage.EARLY);
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
		if (ModDimensions.isGreatVoid(dimension)) return biome != null && biome.is(ModBiomes.ANCIENT_BATTLEFIELD) ? ANCIENT_BATTLEFIELD : 0.0;
		return 0.0;
	}

	/**
	 * The density at a position in a level: the place's, and the yin the bodies of the dead lying round it give off (Corpses#yinAt, since
	 * 2026-10-31), never past the Netherworld's where the place's own is thinner.
	 */
	public static double densityAt(ServerLevel level, BlockPos pos) {
		double place = placeDensity(level, pos), yin = Corpses.yinAt(level, pos);
		return yin <= 0 ? place : Math.max(place, Math.min(NETHERWORLD, place + yin));
	}

	/** The place's own density at a position in a level. */
	private static double placeDensity(ServerLevel level, BlockPos pos) {
		ResourceKey<Level> dimension = level.dimension();
		if (dimension == Level.OVERWORLD) return OVERWORLD; // the same everywhere in it: no biome to look up
		if (ModDimensions.isSecretRealm(dimension)) {
			SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
			if (realm == null) return 0.0;
			return realm == SecretRealm.ANCIENT_BATTLEFIELD ? SECRET_BATTLEFIELD : density(realm.home(), null) * SecretRealms.QI;
		}
		if (!ModDimensions.isNetherworld(dimension) && dimension != Level.NETHER && !ModDimensions.isGreatVoid(dimension)) return 0.0;
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
	 * This cultivator's pool size. Every ghost, everyone who knows a ghost method, and everyone whose ghost path has begun (one born
	 * again who began it among the dead, with no method) has one; a mortal's layers add nothing to it, its methods dormant
	 * (GhostMethods#refusal). On top of the methods' layers, the ghost path's realm and stage give the pool the heaven path's qi pool
	 * gets at that rank ({@link #realmPool}).
	 */
	public static double poolMax(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		boolean has = s.ghost() || s.knowsAnyMethod() || s.pathRank() >= 0;
		if (!has) return 0;
		return (poolMax(true, GhostMethods.layersKnown(c), GhostMethods.foundationLayers(c)) + realmPool(c))
				* StatPoints.poolFactor(c) // Spirit, every path's pool alike
				* (1 + TwoPaths.harmony(c)); // and the paths' harmony, as the body and the qi pool have it (2026-10-24)
	}

	/**
	 * The pool the ghost path's realm gives, as the heaven path's realm gives the qi pool (CultivationStats#maxQi, the same table: 100 at Qi
	 * Refining Early up to 160 million at Heavenly Lord; the user's ask, 2026-10-27: "ghost qi pool should increase with realm in ghost path
	 * similarly to how it does with the others"). As Realm Suppression and pressure leave the path (TwoPaths#ghostRank wielded); none
	 * before the path begins, nor for a mortal's dormant one.
	 */
	public static double realmPool(PlayerCultivation c) {
		int rank = TwoPaths.ghostRank(c, true);
		return rank < 0 ? 0 : CultivationStats.maxQi(realm(rank), stage(rank));
	}

	/**
	 * How much faster the ghost path's realm fills the pool than Qi Refining Early: the heaven path's qi gathering at that rank over Qi
	 * Refining Early's (CultivationStats#qiGather), so a pool grown with the realm refills with it (2,500,000 at True Immortal Early fills
	 * 62,500 times as fast), its own filling and the place's alike. 1 before the path begins.
	 */
	public static double realmFill(PlayerCultivation c) {
		int rank = TwoPaths.ghostRank(c, true);
		if (rank < 0) return 1;
		return CultivationStats.qiGather(realm(rank), stage(rank)) / CultivationStats.qiGather(Realm.QI_REFINING, Stage.EARLY);
	}

	private static Realm realm(int rank) {
		return Realm.byIndex(Math.min(Realm.values().length - 1, rank / Stage.values().length));
	}

	private static Stage stage(int rank) {
		return Stage.byIndex(rank % Stage.values().length);
	}

	/**
	 * Ghostly qi filled a second at {@code density} for {@code c}: the pool's own {@link #OWN_FILL} and the place's, x half its Insight, as
	 * every path's qi is gathered (StatPoints#gatherFactor), and x its ghost path's realm ({@link #realmFill}).
	 */
	public static double fillPerSecond(PlayerCultivation c, double density) {
		return (OWN_FILL + fillPerSecond(density)) * StatPoints.gatherFactor(c) * realmFill(c);
	}

	/** Ghostly qi the place itself fills a second at {@code density} (on top of the pool's own, {@link #OWN_FILL}). */
	public static double fillPerSecond(double density) {
		return density * FILL_PER_DENSITY;
	}

	/**
	 * The density's word on the cultivation menu: teeming (the Great Void's battlefields), dense (the Netherworld), thin (a Soul Sand Valley),
	 * faint (the Overworld, the Nether), none.
	 */
	public static String word(double density) {
		if (density >= ANCIENT_BATTLEFIELD) return "teeming";
		if (density >= NETHERWORLD) return "dense";
		if (density >= SOUL_SAND_VALLEY) return "thin";
		if (density > 0) return "faint";
		return "none";
	}

	private GhostQi() {}
}
