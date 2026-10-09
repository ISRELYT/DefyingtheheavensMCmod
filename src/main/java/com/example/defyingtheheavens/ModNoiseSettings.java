package com.example.defyingtheheavens;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.List;

/**
 * Terrain of the Upper Realm ("upper_realm_floating_islands") and the empty Spatial Gap ("spatial_gap_empty").
 * <p>
 * Three tiers of large floating islands (centered on Y 128, 256 and 390). Each tier is a lens shape: a smooth 2D mask
 * M(x, z), positive over an island, plus a vertical "tent" that is 0 at the tier's center height and falls off upward
 * and downward. Solid where the sum is positive, so the island's top rises and its underside hangs deeper toward its
 * middle. The masks are dominated by one broad octave, so islands are big and solid; a weak edge noise only roughens
 * their rims.
 * <p>
 * A slow regional "style" noise per tier blends three shapes of island, so every tier has all of them side by side:
 * flat-topped tablelands, rolling hills and tall mountain islands. Small boulder islets float around the islands' edges.
 * Then gentle 3D detail roughens the surfaces, the tallest peaks are tapered above Y 460 so they round off below the sky
 * ceiling, caverns are hollowed out only deep inside island cores (they never break through to the surface or the
 * underside), and a vertical slide over the whole -64..512 range keeps the abyss floor and the sky ceiling empty.
 */
public final class ModNoiseSettings {
	public static final ResourceKey<NoiseGeneratorSettings> UPPER_REALM = ResourceKey.create(Registries.NOISE_SETTINGS, DefyingTheHeavens.id("upper_realm_floating_islands"));
	public static final ResourceKey<NoiseGeneratorSettings> SPATIAL_GAP = ResourceKey.create(Registries.NOISE_SETTINGS, DefyingTheHeavens.id("spatial_gap_empty"));

	public static final ResourceKey<NormalNoise.NoiseParameters> ISLAND_LOW = noiseKey("island_low");
	public static final ResourceKey<NormalNoise.NoiseParameters> ISLAND_MID = noiseKey("island_mid");
	public static final ResourceKey<NormalNoise.NoiseParameters> ISLAND_HIGH = noiseKey("island_high");
	public static final ResourceKey<NormalNoise.NoiseParameters> STYLE_LOW = noiseKey("style_low");
	public static final ResourceKey<NormalNoise.NoiseParameters> STYLE_MID = noiseKey("style_mid");
	public static final ResourceKey<NormalNoise.NoiseParameters> STYLE_HIGH = noiseKey("style_high");
	public static final ResourceKey<NormalNoise.NoiseParameters> EDGE = noiseKey("edge");
	public static final ResourceKey<NormalNoise.NoiseParameters> PLATEAU_ROLL = noiseKey("plateau_roll");
	public static final ResourceKey<NormalNoise.NoiseParameters> BOULDER = noiseKey("boulder");
	public static final ResourceKey<NormalNoise.NoiseParameters> DETAIL = noiseKey("detail");
	public static final ResourceKey<NormalNoise.NoiseParameters> CAVERN = noiseKey("cavern");
	public static final ResourceKey<NormalNoise.NoiseParameters> TEMPERATURE = noiseKey("temperature");
	public static final ResourceKey<NormalNoise.NoiseParameters> HUMIDITY = noiseKey("humidity");

	private static final int MIN_Y = ModDimensions.UPPER_REALM_MIN_Y;
	private static final int MAX_Y = MIN_Y + ModDimensions.UPPER_REALM_HEIGHT; // 512
	/** How solid (in density) the outer layer of an island stays that caverns may not cut into. */
	private static final double CAVERN_SHELL = 0.5;
	/**
	 * Above this height the tallest mountain peaks are pulled down, quadratically (up to {@link #PEAK_TAPER} density at the
	 * top of the world): peaks that would reach ~Y 500-530 round off around Y 480-486 and even the rarest stay below ~500,
	 * instead of running into the top slide and being cut flat just under the build limit. Peaks below ~470 barely change.
	 */
	private static final int PEAK_TAPER_Y = 460;
	private static final double PEAK_TAPER = 2.5;

	private static ResourceKey<NormalNoise.NoiseParameters> noiseKey(String name) {
		return ResourceKey.create(Registries.NOISE, DefyingTheHeavens.id("upper_realm/" + name));
	}

	public static void bootstrapNoises(BootstapContext<NormalNoise.NoiseParameters> context) {
		// firstOctave -8 = features about 256 blocks across; each extra amplitude adds a finer octave. The island masks keep
		// their finer octaves weak, so an island's inside never dips below the rim threshold (no holes).
		context.register(ISLAND_LOW, new NormalNoise.NoiseParameters(-8, 1.0, 0.3, 0.1));
		context.register(ISLAND_MID, new NormalNoise.NoiseParameters(-8, 1.0, 0.3, 0.1));
		context.register(ISLAND_HIGH, new NormalNoise.NoiseParameters(-8, 1.0, 0.3, 0.1));
		context.register(STYLE_LOW, new NormalNoise.NoiseParameters(-9, 1.0, 0.5));
		context.register(STYLE_MID, new NormalNoise.NoiseParameters(-9, 1.0, 0.5));
		context.register(STYLE_HIGH, new NormalNoise.NoiseParameters(-9, 1.0, 0.5));
		context.register(EDGE, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(PLATEAU_ROLL, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
		context.register(BOULDER, new NormalNoise.NoiseParameters(-4, 1.0));
		context.register(DETAIL, new NormalNoise.NoiseParameters(-5, 1.0, 0.5, 0.25));
		context.register(CAVERN, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
		context.register(TEMPERATURE, new NormalNoise.NoiseParameters(-9, 1.0, 1.0, 0.5));
		context.register(HUMIDITY, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5));
	}

	public static void bootstrap(BootstapContext<NoiseGeneratorSettings> context) {
		HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
		context.register(UPPER_REALM, upperRealm(noises));
		context.register(SPATIAL_GAP, spatialGap());
	}

	// --- Upper Realm ---

	private static NoiseGeneratorSettings upperRealm(HolderGetter<NormalNoise.NoiseParameters> noises) {
		DensityFunction terrain = terrain(noises);
		DensityFunction zero = DensityFunctions.zero();
		NoiseRouter router = new NoiseRouter(
				zero, zero, zero, zero,                                    // no aquifers, barriers or lava sheets
				flat(noises, TEMPERATURE, 1.0),                            // biome temperature
				flat(noises, HUMIDITY, 1.0),                               // biome humidity
				zero, zero,                                                // continentalness, erosion
				DensityFunctions.yClampedGradient(MIN_Y, MAX_Y, 1.0, -1.0), // "depth": picks the peak biomes above ~Y 325
				zero,                                                      // weirdness
				terrain,                                                   // initial density (no separate jaggedness pass)
				DensityFunctions.interpolated(terrain),                    // final density, interpolated per 4x8x4 cell
				zero, zero, zero);                                         // no ore veins

		return new NoiseGeneratorSettings(
				NoiseSettings.create(MIN_Y, ModDimensions.UPPER_REALM_HEIGHT, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				upperRealmSurface(),
				List.of(),
				MIN_Y,      // "sea level" at the abyss floor: no oceans
				false,      // mob generation on
				false,      // aquifers off
				false,      // ore veins off (ores come from placed features)
				false);
	}

	private static DensityFunction terrain(HolderGetter<NormalNoise.NoiseParameters> noises) {
		// Island masks. The edge noise is too weak to open holes inside an island; it only makes the rims ragged.
		DensityFunction edge = scale(flat(noises, EDGE, 1.0), 0.06);
		DensityFunction low = add(mask(noises, ISLAND_LOW, 1.6, -0.33), edge);
		DensityFunction mid = add(mask(noises, ISLAND_MID, 1.6, -0.36), edge);
		DensityFunction high = add(mask(noises, ISLAND_HIGH, 1.6, -0.42), edge);

		DensityFunction lowTier = tier(noises, low, STYLE_LOW, 128);
		DensityFunction midTier = tier(noises, mid, STYLE_MID, 256);
		DensityFunction highTier = tier(noises, high, STYLE_HIGH, 390);

		// Boulder clusters: small islets, only just outside/around each tier's landmasses.
		DensityFunction boulderSpots = add(scale(flat(noises, BOULDER, 1.0), 2.0), DensityFunctions.constant(-1.0));
		DensityFunction boulders = DensityFunctions.max(
				DensityFunctions.max(
						add(DensityFunctions.min(boulderSpots, near(low)), tent(134, 6, 10)),
						add(DensityFunctions.min(boulderSpots, near(mid)), tent(262, 6, 10))),
				add(DensityFunctions.min(boulderSpots, near(high)), tent(396, 6, 10)));

		DensityFunction islands = DensityFunctions.max(DensityFunctions.max(lowTier, midTier), DensityFunctions.max(highTier, boulders));

		// Gentle 3D roughness on every surface, and the peak taper. Both come before the caverns, so the cavern shell is
		// measured from the final surface.
		DensityFunction detail = scale(DensityFunctions.noise(noises.getOrThrow(DETAIL), 1.0, 1.0), 0.12);
		DensityFunction taper = scale(DensityFunctions.yClampedGradient(PEAK_TAPER_Y, MAX_Y, 0.0, 1.0).square(), -PEAK_TAPER);
		DensityFunction rough = add(add(islands, detail), taper);

		// Caverns (negative where the cavern noise is high), allowed only where the island is solid by more than
		// CAVERN_SHELL: the outer shell always stays intact, so caverns are sealed pockets, never holes through an island.
		DensityFunction caverns = scale(add(DensityFunctions.constant(0.5), scale(DensityFunctions.noise(noises.getOrThrow(CAVERN), 1.0, 0.75), -1.0)), 3.0);
		DensityFunction shell = add(DensityFunctions.constant(CAVERN_SHELL), scale(rough, -1.0));
		DensityFunction carved = DensityFunctions.min(rough, DensityFunctions.max(caverns, shell));

		// Vertical slide over the full -64..512 range: empty abyss floor, empty sky ceiling.
		DensityFunction bottomSlide = DensityFunctions.yClampedGradient(MIN_Y, 24, -1.0, 10.0);
		DensityFunction topSlide = DensityFunctions.yClampedGradient(480, MAX_Y, 10.0, -1.0);
		return DensityFunctions.min(DensityFunctions.min(carved, bottomSlide), topSlide);
	}

	/**
	 * One tier of islands around {@code center}. A slow regional style noise blends three island shapes, so each tier has
	 * all of them: flat-topped tablelands (style 0), rolling hills (0.5) and tall mountain islands (1). Tall islands also
	 * hang deeper underneath.
	 */
	private static DensityFunction tier(HolderGetter<NormalNoise.NoiseParameters> noises, DensityFunction mask,
			ResourceKey<NormalNoise.NoiseParameters> styleKey, int center) {
		DensityFunction style = add(scale(flat(noises, styleKey, 1.0), 1.6), DensityFunctions.constant(0.5)).clamp(0.0, 1.0);

		// Tablelands: the top follows the mask up to a gently rolling cap ~16 blocks above the center, then stays flat.
		DensityFunction plateauCap = add(DensityFunctions.constant(0.32), scale(flat(noises, PLATEAU_ROLL, 1.0), 0.08));
		DensityFunction flatTop = add(DensityFunctions.min(mask, plateauCap), tentTop(center, 50));
		// Mountains: signed square of the mask - broad gentle foothills and a steep peak ~50-80 blocks up in the middle.
		DensityFunction tallTop = add(scale(DensityFunctions.mul(mask, mask.abs()), 3.0), tentTop(center, 75));
		DensityFunction top = DensityFunctions.lerp(style, flatTop, tallTop);

		DensityFunction bottom = DensityFunctions.lerp(style, add(mask, tentBottom(center, 85)), add(mask, tentBottom(center, 110)));
		return DensityFunctions.min(top, bottom);
	}

	/** Cached 2D noise. */
	private static DensityFunction flat(HolderGetter<NormalNoise.NoiseParameters> noises, ResourceKey<NormalNoise.NoiseParameters> key, double xzScale) {
		return DensityFunctions.flatCache(DensityFunctions.noise(noises.getOrThrow(key), xzScale, 0.0));
	}

	/** Island mask: positive where islands are, 0 at their rim. */
	private static DensityFunction mask(HolderGetter<NormalNoise.NoiseParameters> noises, ResourceKey<NormalNoise.NoiseParameters> key, double strength, double offset) {
		return add(scale(flat(noises, key, 1.0), strength), DensityFunctions.constant(offset));
	}

	/** Positive in a band just around (and inside) a tier's islands: where its stepping-stone boulders may appear. */
	private static DensityFunction near(DensityFunction tierMask) {
		return scale(add(tierMask, DensityFunctions.constant(0.4)), 2.5);
	}

	/** Vertical lens profile: 0 at {@code center}, -1 per {@code top} blocks above and per {@code bottom} blocks below. */
	private static DensityFunction tent(int center, int top, int bottom) {
		return DensityFunctions.min(tentTop(center, top), tentBottom(center, bottom));
	}

	private static DensityFunction tentTop(int center, int top) {
		return DensityFunctions.yClampedGradient(center - 2 * top, center + 2 * top, 2.0, -2.0);
	}

	private static DensityFunction tentBottom(int center, int bottom) {
		return DensityFunctions.yClampedGradient(center - 2 * bottom, center + 2 * bottom, -2.0, 2.0);
	}

	private static DensityFunction add(DensityFunction a, DensityFunction b) {
		return DensityFunctions.add(a, b);
	}

	private static DensityFunction scale(DensityFunction f, double factor) {
		return DensityFunctions.mul(f, DensityFunctions.constant(factor));
	}

	private static SurfaceRules.RuleSource upperRealmSurface() {
		SurfaceRules.RuleSource grass = block(Blocks.GRASS_BLOCK);
		SurfaceRules.RuleSource dirt = block(Blocks.DIRT);

		SurfaceRules.RuleSource thunderPeaks = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25), block(Blocks.OBSIDIAN)),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.05, 0.05), block(Blocks.BLACKSTONE)),
				block(Blocks.BASALT));

		SurfaceRules.RuleSource denseQiPeaks = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.15), block(Blocks.MOSS_BLOCK))),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.0125, 0.0125), block(Blocks.CALCITE)),
				block(ModBlocks.JADE_STONE));

		SurfaceRules.RuleSource spiritForest = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.3), block(Blocks.PODZOL)),
						grass)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt));

		SurfaceRules.RuleSource lush = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grass),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt));

		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.THUNDER_PEAKS), thunderPeaks),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DENSE_QI_PEAKS), denseQiPeaks),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SPIRIT_FOREST), spiritForest),
				lush); // Peach Blossom Sanctuary and anything else
	}

	private static SurfaceRules.RuleSource block(Block block) {
		return SurfaceRules.state(block.defaultBlockState());
	}

	// --- Spatial Gap ---

	private static NoiseGeneratorSettings spatialGap() {
		DensityFunction zero = DensityFunctions.zero();
		NoiseRouter router = new NoiseRouter(zero, zero, zero, zero, zero, zero, zero, zero, zero, zero,
				DensityFunctions.constant(-1.0), DensityFunctions.constant(-1.0), zero, zero, zero);
		return new NoiseGeneratorSettings(
				NoiseSettings.create(ModDimensions.SPATIAL_GAP_MIN_Y, ModDimensions.SPATIAL_GAP_HEIGHT, 1, 2),
				Blocks.AIR.defaultBlockState(),
				Blocks.AIR.defaultBlockState(),
				router,
				SurfaceRules.state(Blocks.AIR.defaultBlockState()),
				List.of(),
				ModDimensions.SPATIAL_GAP_MIN_Y,
				true,       // nothing spawns in the void
				false,
				false,
				false);
	}

	private ModNoiseSettings() {}
}
