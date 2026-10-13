package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
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

	/** The Netherworld's terrain ("netherworld_plateaus", see {@link #netherworld}) and its own noises. */
	public static final ResourceKey<NoiseGeneratorSettings> NETHERWORLD = ResourceKey.create(Registries.NOISE_SETTINGS, DefyingTheHeavens.id("netherworld_plateaus"));
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_LAND = netherworldNoise("land");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_RELIEF = netherworldNoise("relief");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_PEAK = netherworldNoise("peak");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_LOW = netherworldNoise("low");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_BONE = netherworldNoise("bone");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_EDGE = netherworldNoise("edge");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_UNDER = netherworldNoise("under");
	public static final ResourceKey<NormalNoise.NoiseParameters> NW_DETAIL = netherworldNoise("detail");

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

	private static ResourceKey<NormalNoise.NoiseParameters> netherworldNoise(String name) {
		return ResourceKey.create(Registries.NOISE, DefyingTheHeavens.id("netherworld/" + name));
	}

	/** Registers the mod's own density function type ({@link OriginPlateau}); called once at start-up, before any world loads. */
	public static void registerTypes() {
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DefyingTheHeavens.id("origin_plateau"), OriginPlateau.CODEC.codec());
	}

	/**
	 * 1 within {@code inner} blocks of the world's 0, 0, falling evenly to 0 at {@code outer}: the Netherworld adds this to its
	 * land and flattens its relief by it, so the Ghost Gate at 0, 0 always stands on broad, level ground.
	 */
	public record OriginPlateau(double inner, double outer) implements DensityFunction.SimpleFunction {
		public static final KeyDispatchDataCodec<OriginPlateau> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.DOUBLE.fieldOf("inner").forGetter(OriginPlateau::inner),
				Codec.DOUBLE.fieldOf("outer").forGetter(OriginPlateau::outer)).apply(instance, OriginPlateau::new)));

		@Override
		public double compute(FunctionContext context) {
			double distance = Math.sqrt((double) context.blockX() * context.blockX() + (double) context.blockZ() * context.blockZ());
			return Mth.clamp((outer - distance) / (outer - inner), 0.0, 1.0);
		}

		@Override
		public double minValue() { return 0.0; }

		@Override
		public double maxValue() { return 1.0; }

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
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

		// The Netherworld: broad land with chasms (land, ~250 blocks), rolling plateaus (relief), the Nine Nether Peaks' uplift,
		// the Yellow Springs' lowlands and the Bone Wastes (peak, low, bone: also the biomes), ragged rims and undersides.
		context.register(NW_LAND, new NormalNoise.NoiseParameters(-8, 1.0, 0.4, 0.15));
		context.register(NW_RELIEF, new NormalNoise.NoiseParameters(-7, 1.0, 0.5));
		context.register(NW_PEAK, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
		context.register(NW_LOW, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
		context.register(NW_BONE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
		context.register(NW_EDGE, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(NW_UNDER, new NormalNoise.NoiseParameters(-4, 1.0, 0.5));
		context.register(NW_DETAIL, new NormalNoise.NoiseParameters(-5, 1.0, 0.5, 0.25));
	}

	public static void bootstrap(BootstapContext<NoiseGeneratorSettings> context) {
		HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
		context.register(UPPER_REALM, upperRealm(noises));
		context.register(SPATIAL_GAP, spatialGap());
		context.register(NETHERWORLD, netherworld(noises));
	}

	// --- The Netherworld ---

	private static final int NW_MIN_Y = ModDimensions.NETHERWORLD_MIN_Y;
	private static final int NW_MAX_Y = NW_MIN_Y + ModDimensions.NETHERWORLD_HEIGHT;
	/**
	 * The plateaus' top: around Y {@link #NW_BASE_Y}, rolling up to {@link #NW_RELIEF_BLOCKS} either way, raised up to
	 * {@link #NW_PEAK_BLOCKS} on the Nine Nether Peaks, with crags up to {@link #NW_CRAG_BLOCKS} more on top (~Y 170-210), and
	 * lowered up to {@link #NW_LOW_BLOCKS} in the Yellow Springs (~Y 65-80, flooded below YellowSpringsPoolFeature#POOL_LEVEL).
	 * The land is about {@link #NW_THICKNESS} blocks thick under its top (more under the peaks), its underside ragged.
	 */
	private static final int NW_BASE_Y = 100, NW_RELIEF_BLOCKS = 30, NW_PEAK_BLOCKS = 70, NW_CRAG_BLOCKS = 35, NW_LOW_BLOCKS = 35, NW_THICKNESS = 50;
	/**
	 * The biomes' edges on the noises that pick them (dimension/netherworld.json must agree): the Nine Nether Peaks where the peak
	 * noise passes {@link #NW_PEAK_EDGE}, the Yellow Springs where the low noise falls below -{@link #NW_LOW_EDGE}. Uplift and
	 * lowering start just inside the edge ({@link #NW_RAMP_START}) and are full {@link #NW_RAMP_WIDTH} further on.
	 */
	private static final double NW_PEAK_EDGE = 0.25, NW_LOW_EDGE = 0.25, NW_RAMP_START = 0.2, NW_RAMP_WIDTH = 0.3;
	/**
	 * How much of the sky is land: land where {@code land * 1.5 + offset > 0}, about three columns in four; the rest are chasms
	 * open to the bottomless abyss. Within {@link #NW_ORIGIN_INNER} of 0, 0 it is always land, and level (see OriginPlateau).
	 */
	private static final double NW_LAND_OFFSET = 0.35;
	private static final double NW_ORIGIN_INNER = 96, NW_ORIGIN_OUTER = 180;

	/**
	 * Broad misty plateaus over a bottomless abyss. One land, not tiers: a 2D land mask (positive over land) cut by chasms where
	 * it dips below 0, with near-vertical walls roughened by the edge noise; a top surface built from the relief, peak and low
	 * noises (the same noises pick the biomes, so the Nine Nether Peaks stand tall and the Yellow Springs lie low); and a ragged
	 * underside some 50 blocks below it, eaten away by 3D noise. Cheap: everything but the detail and the underside is 2D.
	 */
	private static NoiseGeneratorSettings netherworld(HolderGetter<NormalNoise.NoiseParameters> noises) {
		DensityFunction zero = DensityFunctions.zero();
		DensityFunction origin = DensityFunctions.flatCache(new OriginPlateau(NW_ORIGIN_INNER, NW_ORIGIN_OUTER));
		DensityFunction land = add(add(scale(flat(noises, NW_LAND, 1.0), 1.5), DensityFunctions.constant(NW_LAND_OFFSET)),
				add(scale(origin, 1.5), scale(flat(noises, NW_EDGE, 1.0), 0.08)));
		// 0 outside the Nine Nether Peaks / Yellow Springs, rising to 1 a little way in.
		DensityFunction peak = add(scale(flat(noises, NW_PEAK, 1.0), 1.0 / NW_RAMP_WIDTH),
				DensityFunctions.constant(-NW_RAMP_START / NW_RAMP_WIDTH)).clamp(0.0, 1.0);
		DensityFunction low = add(scale(flat(noises, NW_LOW, 1.0), -1.0 / NW_RAMP_WIDTH),
				DensityFunctions.constant(-NW_RAMP_START / NW_RAMP_WIDTH)).clamp(0.0, 1.0);
		// The peaks' crags: sharp ridges where the edge noise crosses zero, standing taller the further in.
		DensityFunction crags = DensityFunctions.mul(peak, add(DensityFunctions.constant(1.0), scale(flat(noises, NW_EDGE, 1.0).abs(), -3.0)).clamp(0.0, 1.0));
		DensityFunction variation = add(add(scale(flat(noises, NW_RELIEF, 1.0), NW_RELIEF_BLOCKS), scale(crags, NW_CRAG_BLOCKS)),
				add(scale(peak, NW_PEAK_BLOCKS), scale(low, -NW_LOW_BLOCKS)));
		// Near the gate the land lies almost level.
		DensityFunction top = DensityFunctions.flatCache(add(DensityFunctions.constant(NW_BASE_Y),
				DensityFunctions.mul(variation, add(DensityFunctions.constant(1.0), scale(origin, -0.85)))));
		DensityFunction y = DensityFunctions.yClampedGradient(NW_MIN_Y, NW_MAX_Y, NW_MIN_Y, NW_MAX_Y);

		DensityFunction detail = scale(DensityFunctions.noise(noises.getOrThrow(NW_DETAIL), 1.0, 1.0), 0.15);
		DensityFunction topDensity = add(scale(add(top, scale(y, -1.0)), 1.0 / 12.0), detail);
		DensityFunction underside = scale(DensityFunctions.noise(noises.getOrThrow(NW_UNDER), 1.0, 1.5), 14.0);
		DensityFunction bottom = add(top, add(DensityFunctions.constant(-NW_THICKNESS), add(scale(peak, -25.0), underside)));
		DensityFunction bottomDensity = scale(add(y, scale(bottom, -1.0)), 1.0 / 8.0);
		DensityFunction terrain = DensityFunctions.min(DensityFunctions.min(topDensity, bottomDensity), scale(land, 6.0));

		DensityFunction bottomSlide = DensityFunctions.yClampedGradient(NW_MIN_Y, NW_MIN_Y + 16, -1.0, 10.0);
		DensityFunction topSlide = DensityFunctions.yClampedGradient(NW_MAX_Y - 20, NW_MAX_Y, 10.0, -1.0);
		terrain = DensityFunctions.min(DensityFunctions.min(terrain, bottomSlide), topSlide);

		NoiseRouter router = new NoiseRouter(
				zero, zero, zero, zero,                                    // no aquifers or lava sheets
				flat(noises, NW_PEAK, 1.0),                                // "temperature": the Nine Nether Peaks where high
				flat(noises, NW_LOW, 1.0),                                 // "humidity": the Yellow Springs where low
				zero, zero, zero,                                          // continentalness, erosion, depth
				flat(noises, NW_BONE, 1.0),                                // "weirdness": the Bone Wastes where high
				terrain,
				DensityFunctions.interpolated(terrain),
				zero, zero, zero);
		return new NoiseGeneratorSettings(
				NoiseSettings.create(NW_MIN_Y, ModDimensions.NETHERWORLD_HEIGHT, 1, 2),
				Blocks.DEEPSLATE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				netherworldSurface(),
				List.of(),
				NW_MIN_Y,   // "sea level" at the bottom of the world: no seas, the chasms stay open to the abyss
				true,       // no mobs at generation
				false,      // no aquifers
				false,      // no ore veins
				false);
	}

	/**
	 * The Netherworld's ground: the Fields of the Other Shore soul soil (patched with soul sand), the Bone Wastes soul sand
	 * with bone and basalt breaking through, the Yellow Springs mud and soul soil banks, the Nine Nether Peaks bare blackstone
	 * and basalt; deepslate beneath them all.
	 */
	private static SurfaceRules.RuleSource netherworldSurface() {
		SurfaceRules.RuleSource soulSoil = block(Blocks.SOUL_SOIL);
		SurfaceRules.RuleSource soulSand = block(Blocks.SOUL_SAND);
		SurfaceRules.RuleSource fields = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35), soulSand),
						soulSoil)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, soulSoil));
		SurfaceRules.RuleSource bones = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.4), block(Blocks.BONE_BLOCK)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.025, 0.025), block(Blocks.BASALT)),
						soulSand)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, soulSand));
		SurfaceRules.RuleSource springs = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.0), block(Blocks.MUD)),
						soulSoil)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.MUD)));
		SurfaceRules.RuleSource peaks = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.04, 0.04), block(Blocks.BASALT)),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, block(Blocks.BLACKSTONE)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.BLACKSTONE)));
		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.NINE_NETHER_PEAKS), peaks),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.BONE_WASTES), bones),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.YELLOW_SPRINGS), springs),
				fields); // the Fields of the Other Shore, and anything else
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

	/** The island masks' strength, each tier's offset (the higher, the more of the sky its islands cover), the edge noise's. */
	private static final double MASK_STRENGTH = 1.6;
	private static final double LOW_OFFSET = -0.33, MID_OFFSET = -0.36, HIGH_OFFSET = -0.42;
	private static final double EDGE_STRENGTH = 0.06;

	/**
	 * The strongest of the three tiers' island masks at x, z in {@code state}'s world: positive over an island, 0 at its rim,
	 * negative over open sky. An island (as against a stray boulder islet) only stands where this is above about -0.15 (the
	 * 3D detail noise reaches about 0.15). Four 2D noise samples: far cheaper than having the generator find a column's
	 * surface (~10 ms, most of it spent on the empty sky), so SectStructure looks here first for ground for a sect.
	 */
	public static double islandMask(RandomState state, int x, int z) {
		double edge = state.getOrCreateNoise(EDGE).getValue(x, 0, z) * EDGE_STRENGTH;
		double low = state.getOrCreateNoise(ISLAND_LOW).getValue(x, 0, z) * MASK_STRENGTH + LOW_OFFSET;
		double mid = state.getOrCreateNoise(ISLAND_MID).getValue(x, 0, z) * MASK_STRENGTH + MID_OFFSET;
		double high = state.getOrCreateNoise(ISLAND_HIGH).getValue(x, 0, z) * MASK_STRENGTH + HIGH_OFFSET;
		return Math.max(low, Math.max(mid, high)) + edge;
	}

	private static DensityFunction terrain(HolderGetter<NormalNoise.NoiseParameters> noises) {
		// Island masks. The edge noise is too weak to open holes inside an island; it only makes the rims ragged.
		DensityFunction edge = scale(flat(noises, EDGE, 1.0), EDGE_STRENGTH);
		DensityFunction low = add(mask(noises, ISLAND_LOW, MASK_STRENGTH, LOW_OFFSET), edge);
		DensityFunction mid = add(mask(noises, ISLAND_MID, MASK_STRENGTH, MID_OFFSET), edge);
		DensityFunction high = add(mask(noises, ISLAND_HIGH, MASK_STRENGTH, HIGH_OFFSET), edge);

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
				// Mostly plain stone, with broad patches of jade stone (and more in veins, see ModFeatures#register).
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.4, -0.1), block(ModBlocks.JADE_STONE)),
				block(Blocks.STONE));

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
