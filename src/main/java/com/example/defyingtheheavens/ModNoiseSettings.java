package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.CubicSpline;
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

import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

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

	/** The Great Void Realm's terrain ("great_void_continents", see {@link #greatVoid}) and its own noises. */
	public static final ResourceKey<NoiseGeneratorSettings> GREAT_VOID = ResourceKey.create(Registries.NOISE_SETTINGS, DefyingTheHeavens.id("great_void_continents"));
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_CONTINENT = greatVoidNoise("continent");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_EDGE = greatVoidNoise("edge");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_RELIEF = greatVoidNoise("relief");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_MOUNTAIN = greatVoidNoise("mountain");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_RIDGE = greatVoidNoise("ridge");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_UNDER = greatVoidNoise("under");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_ISLE = greatVoidNoise("isle");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_ELEMENT = greatVoidNoise("element");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_DAO = greatVoidNoise("dao");
	public static final ResourceKey<NormalNoise.NoiseParameters> GV_BATTLE = greatVoidNoise("battle");

	/** The Immortal Realm's terrain ("immortal_peaks", see {@link #immortalRealm}) and its own noises. */
	public static final ResourceKey<NoiseGeneratorSettings> IMMORTAL_REALM = ResourceKey.create(Registries.NOISE_SETTINGS, DefyingTheHeavens.id("immortal_peaks"));
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_MASSIF = immortalNoise("massif");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_EDGE = immortalNoise("edge");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_TERRACE = immortalNoise("terrace");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_RELIEF = immortalNoise("relief");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_RANGE = immortalNoise("range");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_RIDGE = immortalNoise("ridge");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_PILLAR = immortalNoise("pillar");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_PILLAR_TOP = immortalNoise("pillar_top");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_UNDER = immortalNoise("under");
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_GARDEN = immortalNoise("garden");
	/** Where in its cell each of the realm's other cities stands (ImmortalCityStructure, {@link #citySite}). */
	public static final ResourceKey<NormalNoise.NoiseParameters> IR_CITY_SITE = immortalNoise("city_site");

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

	private static ResourceKey<NormalNoise.NoiseParameters> greatVoidNoise(String name) {
		return ResourceKey.create(Registries.NOISE, DefyingTheHeavens.id("great_void/" + name));
	}

	private static ResourceKey<NormalNoise.NoiseParameters> immortalNoise(String name) {
		return ResourceKey.create(Registries.NOISE, DefyingTheHeavens.id("immortal_realm/" + name));
	}

	/**
	 * Registers the mod's own density function types ({@link OriginPlateau}, {@link NirvanaShore}, {@link Summit}); called once at
	 * start-up, before any world loads.
	 */
	public static void registerTypes() {
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DefyingTheHeavens.id("origin_plateau"), OriginPlateau.CODEC.codec());
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DefyingTheHeavens.id("nirvana_shore"), NirvanaShore.CODEC.codec());
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DefyingTheHeavens.id("summit"), Summit.CODEC.codec());
		Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, DefyingTheHeavens.id("city_district"), CityDistrict.CODEC.codec());
	}

	// --- The Immortal Realm's other cities' sites (ImmortalCityStructure) ---

	/**
	 * The realm's other cities: a site tried in each cell of {@link #CITY_CELL} blocks, the land rolled gently about it out to
	 * {@link #CITY_DISTRICT_INNER} and back to the land's own by {@link #CITY_DISTRICT_OUTER} (as the capital's island is about the capital),
	 * its own land too (where it stands near a massif's rim, the massif reaches out round it), so a city stands on rolling ground with no
	 * pinnacle or mountain in it (tour 6: tried on the land as the realm makes it, a city came only one in some nineteen thousand blocks, and
	 * that one had a pinnacle in its square and its ground forty-five blocks uneven; tour 7: a site asked land all round its district, and one
	 * cell in a hundred had one).
	 */
	public static final int CITY_CELL = 640, CITY_DISTRICT_INNER = 90, CITY_DISTRICT_OUTER = 140;
	/** How far a site may stand off its cell's middle (two cities' districts never meet), and how many spots in a cell are tried. */
	private static final int CITY_JITTER = 150, CITY_TRIES = 6;
	/** A site lies on a massif by its mask at least this far in (at its very rim, its district reaches out over the clouds as a headland). */
	private static final double CITY_SITE_MASK = 0.0;
	/** No city this near the capital's island's middle. */
	private static final double CITY_CAPITAL_KEEP = ModNoiseSettings.MAIN_OUTER + CITY_DISTRICT_OUTER + 120;
	/** How far either way a city's ground rolls about its site's level by the terrace noise (and the relief's small hills on that). */
	public static final double CITY_SITE_SWELL = 10;

	/** A city's site: its middle, and the level its ground rolls about (by the terrace noise, {@link #CITY_SITE_SWELL}). */
	public record CitySite(int x, int z, double level) {}

	/** The sites found, a cell at a time, for each world (keyed by its site noise): the terrain and the structure ask alike. */
	private static final Map<NormalNoise, Map<Long, Optional<CitySite>>> CITY_SITES = Collections.synchronizedMap(new WeakHashMap<>());

	/** The site in cell (cx, cz) of the world {@code state} is of, or null if the land there holds none. */
	public static CitySite citySite(RandomState state, int cx, int cz) {
		return citySite(state.getOrCreateNoise(IR_CITY_SITE), state.getOrCreateNoise(IR_MASSIF), state.getOrCreateNoise(IR_EDGE),
				state.getOrCreateNoise(IR_TERRACE), cx, cz);
	}

	static CitySite citySite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, int cx, int cz) {
		Map<Long, Optional<CitySite>> cells = CITY_SITES.computeIfAbsent(site, key -> new ConcurrentHashMap<>());
		return cells.computeIfAbsent(ChunkPos.asLong(cx, cz), key -> Optional.ofNullable(findCitySite(site, massif, edge, terrace, cx, cz))).orElse(null);
	}

	/**
	 * Up to {@link #CITY_TRIES} spots off the cell's middle by the site noise, the first on a massif (its district makes the rest of its own
	 * land), clear of the capital's island; its level the massif's terrace there (as the land's own spline makes it) less the terrace noise's
	 * share, so the ground rolls about the terrace's level.
	 */
	private static CitySite findCitySite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, int cx, int cz) {
		for (int k = 0; k < CITY_TRIES; k++) {
			double ox = site.getValue(cx * 13.37 + k * 101.3, 0.0, cz * 13.37), oz = site.getValue(cx * 13.37 + 57.1, 0.0, cz * 13.37 + k * 101.3);
			int x = cx * CITY_CELL + CITY_CELL / 2 + (int) Math.round(Mth.clamp(ox, -1.0, 1.0) * CITY_JITTER);
			int z = cz * CITY_CELL + CITY_CELL / 2 + (int) Math.round(Mth.clamp(oz, -1.0, 1.0) * CITY_JITTER);
			if (Math.hypot(x - MAIN_X, z - MAIN_Z) < CITY_CAPITAL_KEEP || mask(massif, edge, x, z) < CITY_SITE_MASK) continue;
			double t = terrace.getValue(x, 0.0, z);
			return new CitySite(x, z, terraceLevel(t) - CITY_SITE_SWELL * Mth.clamp(t, -1.25, 1.25));
		}
		return null;
	}

	/** The site nearest column (x, z), of its cell's and the eight round it, or null if none. */
	static CitySite nearestCitySite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, double x, double z) {
		int cx = Math.floorDiv((int) Math.floor(x), CITY_CELL), cz = Math.floorDiv((int) Math.floor(z), CITY_CELL);
		CitySite nearest = null;
		double best = Double.MAX_VALUE;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				CitySite found = citySite(site, massif, edge, terrace, cx + dx, cz + dz);
				if (found == null) continue;
				double d = (found.x() - x) * (found.x() - x) + (found.z() - z) * (found.z() - z);
				if (d < best) {
					best = d;
					nearest = found;
				}
			}
		}
		return nearest;
	}

	// --- The Immortal Realm's immortal sects' sites (ImmortalSectPlan) ---

	/**
	 * The realm's immortal sects: a site tried in each cell of {@link #SECT_CELL} blocks, out over the sea of clouds with no land within
	 * {@link #SECT_SEA_REACH} of it (the user's word, 2026-10-25: "the floating sect should be over the cloud sea not above a giant island";
	 * its islands float there, ImmortalSectPlan), clear of the capital's island and of every city's district; no pinnacle stands within
	 * {@link #SECT_CLEARING_INNER} of it, the sea's own back by {@link #SECT_CLEARING_OUTER} (CityDistrict's clearing).
	 */
	public static final int SECT_CELL = 960, SECT_CLEARING_INNER = 190, SECT_CLEARING_OUTER = 250;
	/** How far a site may stand off its cell's middle, and how many spots in a cell are tried. */
	private static final int SECT_JITTER = 240, SECT_TRIES = 8;
	/** No land within this of a site: the massifs' mask at it and on three rings round it out to here no more than {@link #SECT_SEA_MASK}. */
	public static final int SECT_SEA_REACH = 200;
	private static final double SECT_SEA_MASK = -0.15;
	/** A sect's core island's top stands about this high over the clouds, a few blocks either way by the terrace noise. */
	public static final int SECT_LEVEL_Y = ModDimensions.IMMORTAL_CLOUD_Y + 56;
	private static final double SECT_LEVEL_SWELL = 6;
	/** No sect this near the capital's island's middle, nor this near a city's site (whose district makes land). */
	private static final double SECT_CAPITAL_KEEP = ModNoiseSettings.MAIN_OUTER + SECT_SEA_REACH + 160,
			SECT_CITY_KEEP = CITY_DISTRICT_OUTER + SECT_SEA_REACH + 30;

	/** The sects' sites found, a cell at a time, for each world (keyed by its site noise). */
	private static final Map<NormalNoise, Map<Long, Optional<CitySite>>> SECT_SITES = Collections.synchronizedMap(new WeakHashMap<>());

	/** The immortal sect's site in sect cell (cx, cz) of the world {@code state} is of, or null if the land there holds none. */
	public static CitySite sectSite(RandomState state, int cx, int cz) {
		return sectSite(state.getOrCreateNoise(IR_CITY_SITE), state.getOrCreateNoise(IR_MASSIF), state.getOrCreateNoise(IR_EDGE),
				state.getOrCreateNoise(IR_TERRACE), cx, cz);
	}

	static CitySite sectSite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, int cx, int cz) {
		Map<Long, Optional<CitySite>> cells = SECT_SITES.computeIfAbsent(site, key -> new ConcurrentHashMap<>());
		return cells.computeIfAbsent(ChunkPos.asLong(cx, cz), key -> Optional.ofNullable(findSectSite(site, massif, edge, terrace, cx, cz))).orElse(null);
	}

	/**
	 * Up to {@link #SECT_TRIES} spots off the cell's middle by the site noise (other draws of it than the cities'), the first out over the
	 * open sea of clouds ({@link #overSea}), clear of the capital's island and of every city's site by {@link #SECT_CITY_KEEP}; its level
	 * {@link #SECT_LEVEL_Y}, a few blocks either way by the terrace noise.
	 */
	private static CitySite findSectSite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, int cx, int cz) {
		for (int k = 0; k < SECT_TRIES; k++) {
			double ox = site.getValue(cx * 7.77 + 500.5 + k * 91.1, 0.0, cz * 7.77 - 300.3), oz = site.getValue(cx * 7.77 - 211.7, 0.0, cz * 7.77 + 401.9 + k * 91.1);
			int x = cx * SECT_CELL + SECT_CELL / 2 + (int) Math.round(Mth.clamp(ox, -1.0, 1.0) * SECT_JITTER);
			int z = cz * SECT_CELL + SECT_CELL / 2 + (int) Math.round(Mth.clamp(oz, -1.0, 1.0) * SECT_JITTER);
			if (Math.hypot(x - MAIN_X, z - MAIN_Z) < SECT_CAPITAL_KEEP || !overSea(massif, edge, x, z)) continue;
			if (nearCity(site, massif, edge, terrace, x, z, SECT_CITY_KEEP)) continue;
			return new CitySite(x, z, SECT_LEVEL_Y + SECT_LEVEL_SWELL * Mth.clamp(terrace.getValue(x, 0.0, z), -1.0, 1.0));
		}
		return null;
	}

	/** Whether no massif reaches within {@link #SECT_SEA_REACH} of column (x, z): its mask there and on three rings round it under {@link #SECT_SEA_MASK}. */
	private static boolean overSea(NormalNoise massif, NormalNoise edge, int x, int z) {
		if (mask(massif, edge, x, z) > SECT_SEA_MASK) return false;
		for (int ring = 1; ring <= 3; ring++) {
			double reach = SECT_SEA_REACH * ring / 3.0;
			for (int k = 0; k < 8 * ring; k++) {
				double angle = Math.PI * 2 * k / (8 * ring);
				if (mask(massif, edge, x + Math.cos(angle) * reach, z + Math.sin(angle) * reach) > SECT_SEA_MASK) return false;
			}
		}
		return true;
	}

	/** Whether any city's site lies within {@code reach} of column (x, z). */
	private static boolean nearCity(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, int x, int z, double reach) {
		int r = (int) Math.ceil(reach);
		for (int cx = Math.floorDiv(x - r, CITY_CELL); cx <= Math.floorDiv(x + r, CITY_CELL); cx++) {
			for (int cz = Math.floorDiv(z - r, CITY_CELL); cz <= Math.floorDiv(z + r, CITY_CELL); cz++) {
				CitySite city = citySite(site, massif, edge, terrace, cx, cz);
				if (city != null && Math.hypot(city.x() - x, city.z() - z) < reach) return true;
			}
		}
		return false;
	}

	/** The immortal sect's site nearest column (x, z), of its sect cell's and the eight round it, or null if none. */
	static CitySite nearestSectSite(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, double x, double z) {
		int cx = Math.floorDiv((int) Math.floor(x), SECT_CELL), cz = Math.floorDiv((int) Math.floor(z), SECT_CELL);
		CitySite nearest = null;
		double best = Double.MAX_VALUE;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				CitySite found = sectSite(site, massif, edge, terrace, cx + dx, cz + dz);
				if (found == null) continue;
				double d = (found.x() - x) * (found.x() - x) + (found.z() - z) * (found.z() - z);
				if (d < best) {
					best = d;
					nearest = found;
				}
			}
		}
		return nearest;
	}

	/** A sect's clearing's weight this far from its site: 1 within {@link #SECT_CLEARING_INNER}, easing to 0 by {@link #SECT_CLEARING_OUTER}. */
	static double sectClearingWeight(double distance) {
		double t = Mth.clamp((SECT_CLEARING_OUTER - distance) / (SECT_CLEARING_OUTER - SECT_CLEARING_INNER), 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	/** An immortal sect's clearing at column (x, z) from the noises: 1 about its site, 0 beyond (no pinnacle stands in it). */
	static double sectClearing(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, double x, double z) {
		CitySite sect = nearestSectSite(site, massif, edge, terrace, x, z);
		return sect == null ? 0.0 : sectClearingWeight(Math.hypot(x - sect.x(), z - sect.z()));
	}

	/** The cities' districts at column (x, z) from their noises: {weight, level} of the city's whose district it lies in, {0, the gate's level} out of every one. */
	static double[] district(NormalNoise site, NormalNoise massif, NormalNoise edge, NormalNoise terrace, double x, double z) {
		CitySite city = nearestCitySite(site, massif, edge, terrace, x, z);
		if (city == null) return new double[] {0.0, GATE_Y};
		return new double[] {districtWeight(Math.hypot(x - city.x(), z - city.z())), city.level()};
	}

	/** The massifs' mask (as {@link #massifMask}) from its two noises. */
	private static double mask(NormalNoise massif, NormalNoise edge, double x, double z) {
		return massif.getValue(x, 0.0, z) * IR_MASK_STRENGTH + IR_MASK_OFFSET + edge.getValue(x, 0.0, z) * IR_EDGE_STRENGTH
				+ 3.0 * mainIsland().at(x, z) + 3.0 * courtSummit().at(x, z);
	}

	/** The massifs' terrace level for a terrace noise value: the spline of {@link #IR_TERRACES} (its level at each point, eased between). */
	static double terraceLevel(double noise) {
		if (noise <= IR_TERRACES[0][0]) return IR_TERRACES[0][1];
		for (int i = 0; i < IR_TERRACES.length - 1; i++) {
			if (noise <= IR_TERRACES[i + 1][0]) {
				double t = (noise - IR_TERRACES[i][0]) / (IR_TERRACES[i + 1][0] - IR_TERRACES[i][0]);
				return IR_TERRACES[i][1] + (IR_TERRACES[i + 1][1] - IR_TERRACES[i][1]) * t * t * (3 - 2 * t);
			}
		}
		return IR_TERRACES[IR_TERRACES.length - 1][1];
	}

	/**
	 * The realm's cities' districts, as the terrain reads them: 1 within {@link #CITY_DISTRICT_INNER} of a city's site, easing to 0 by
	 * {@link #CITY_DISTRICT_OUTER}; or ({@code level}) the nearest site's level. Its sites from its noises (wired to the world's, so the same
	 * as the structure finds: {@link #citySite}). Or ({@code sects}) the immortal sects' clearings ({@link #sectSite}, out to
	 * {@link #SECT_CLEARING_OUTER}): {@link #sectClearing}.
	 */
	public record CityDistrict(DensityFunction.NoiseHolder site, DensityFunction.NoiseHolder massif, DensityFunction.NoiseHolder edge,
			DensityFunction.NoiseHolder terrace, boolean level, boolean sects) implements DensityFunction {
		public static final KeyDispatchDataCodec<CityDistrict> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
				DensityFunction.NoiseHolder.CODEC.fieldOf("site").forGetter(CityDistrict::site),
				DensityFunction.NoiseHolder.CODEC.fieldOf("massif").forGetter(CityDistrict::massif),
				DensityFunction.NoiseHolder.CODEC.fieldOf("edge").forGetter(CityDistrict::edge),
				DensityFunction.NoiseHolder.CODEC.fieldOf("terrace").forGetter(CityDistrict::terrace),
				Codec.BOOL.fieldOf("level").forGetter(CityDistrict::level),
				Codec.BOOL.optionalFieldOf("sects", false).forGetter(CityDistrict::sects)).apply(instance, CityDistrict::new)));

		@Override
		public double compute(FunctionContext context) {
			if (site.noise() == null || massif.noise() == null || edge.noise() == null || terrace.noise() == null) return level ? GATE_Y : 0.0;
			if (sects) return sectClearing(site.noise(), massif.noise(), edge.noise(), terrace.noise(), context.blockX(), context.blockZ());
			double[] near = district(site.noise(), massif.noise(), edge.noise(), terrace.noise(), context.blockX(), context.blockZ());
			return level ? near[1] : near[0];
		}

		@Override
		public void fillArray(double[] values, ContextProvider provider) {
			provider.fillAllDirectly(values, this);
		}

		@Override
		public DensityFunction mapAll(Visitor visitor) {
			return visitor.apply(new CityDistrict(visitor.visitNoise(site), visitor.visitNoise(massif), visitor.visitNoise(edge), visitor.visitNoise(terrace),
					level, sects));
		}

		@Override
		public double minValue() { return 0.0; }

		@Override
		public double maxValue() { return level ? IR_MAX_Y : 1.0; }

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
	}

	/**
	 * 1 within {@code inner} blocks of ({@code x}, {@code z}), falling evenly to 0 at {@code outer}: the Immortal Realm's Celestial
	 * Court stands on the summit it raises (see {@link #immortalRealm}).
	 */
	public record Summit(double x, double z, double inner, double outer) implements DensityFunction.SimpleFunction {
		public static final KeyDispatchDataCodec<Summit> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.DOUBLE.fieldOf("x").forGetter(Summit::x),
				Codec.DOUBLE.fieldOf("z").forGetter(Summit::z),
				Codec.DOUBLE.fieldOf("inner").forGetter(Summit::inner),
				Codec.DOUBLE.fieldOf("outer").forGetter(Summit::outer)).apply(instance, Summit::new)));

		@Override
		public double compute(FunctionContext context) {
			return at(context.blockX(), context.blockZ());
		}

		/** Its value at a column. */
		public double at(double bx, double bz) {
			double dx = bx - x, dz = bz - z;
			return Mth.clamp((outer - Math.sqrt(dx * dx + dz * dz)) / (outer - inner), 0.0, 1.0);
		}

		@Override
		public double minValue() { return 0.0; }

		@Override
		public double maxValue() { return 1.0; }

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
	}

	/**
	 * The Shore of Nirvana's ground in the Great Void, added to its continents' mask: land made sure of within {@code inner} blocks of
	 * the world's 0, 0 (fading out by {@code outer}), and to its north a sea of open void, a disc of {@code seaRadius} about
	 * (0, -{@code seaCentre}), where no land stands (a coast, then nothing: where the Immortal Gate is to open, over the void).
	 */
	public record NirvanaShore(double inner, double outer, double seaCentre, double seaRadius) implements DensityFunction.SimpleFunction {
		public static final KeyDispatchDataCodec<NirvanaShore> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.DOUBLE.fieldOf("inner").forGetter(NirvanaShore::inner),
				Codec.DOUBLE.fieldOf("outer").forGetter(NirvanaShore::outer),
				Codec.DOUBLE.fieldOf("sea_centre").forGetter(NirvanaShore::seaCentre),
				Codec.DOUBLE.fieldOf("sea_radius").forGetter(NirvanaShore::seaRadius)).apply(instance, NirvanaShore::new)));
		/** How strongly the shore's land and its void sea outweigh the continents' own mask (which stays within about -1.9..1.9). */
		static final double LAND = 3.0, SEA = 4.0;
		/** Over how many blocks the sea's edge fades to the land's own. */
		static final double SEA_EDGE = 40;

		@Override
		public double compute(FunctionContext context) {
			return LAND * plateau(context.blockX(), context.blockZ()) - SEA * sea(context.blockX(), context.blockZ());
		}

		/** 1 within {@link #inner} of 0, 0, falling to 0 at {@link #outer}. */
		double plateau(double x, double z) {
			return Mth.clamp((outer - Math.sqrt(x * x + z * z)) / (outer - inner), 0.0, 1.0);
		}

		/** 1 inside the void sea north of the shore, 0 outside it. */
		double sea(double x, double z) {
			double dz = z + seaCentre;
			return Mth.clamp((seaRadius - Math.sqrt(x * x + dz * dz)) / SEA_EDGE, 0.0, 1.0);
		}

		@Override
		public double minValue() { return -SEA; }

		@Override
		public double maxValue() { return LAND; }

		@Override
		public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
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

		// The Great Void: continents about a thousand blocks across (continent), their ragged rims (edge), rolling relief and
		// mountain ranges with sharp ridges, ragged undersides; sparse high isles; and the noises that pick its lands: which element's
		// Dao runs strong (element), where a Dao field lies (dao), where an old battlefield (battle), each some 500 blocks across.
		context.register(GV_CONTINENT, new NormalNoise.NoiseParameters(-10, 1.0, 0.5, 0.25));
		context.register(GV_EDGE, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
		context.register(GV_RELIEF, new NormalNoise.NoiseParameters(-7, 1.0, 0.5));
		context.register(GV_MOUNTAIN, new NormalNoise.NoiseParameters(-9, 1.0, 0.5));
		context.register(GV_RIDGE, new NormalNoise.NoiseParameters(-7, 1.0));
		context.register(GV_UNDER, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(GV_ISLE, new NormalNoise.NoiseParameters(-8, 1.0, 0.3, 0.1));
		context.register(GV_ELEMENT, new NormalNoise.NoiseParameters(-9, 1.0, 1.0, 0.5));
		context.register(GV_DAO, new NormalNoise.NoiseParameters(-9, 1.0, 1.0, 0.5));
		context.register(GV_BATTLE, new NormalNoise.NoiseParameters(-9, 1.0, 1.0, 0.5));

		// The Immortal Realm: massifs some 500 blocks across (massif) with ragged rims (edge), terraced into levels (terrace, some 250
		// blocks) with small hills on them (relief), mountain ranges with sharp ridges (range, ridge), karst pillars standing alone out of
		// the clouds (pillar, a dozen blocks across, their tops by pillar_top), ragged roots (under), and its gardens (garden: the biome).
		context.register(IR_MASSIF, new NormalNoise.NoiseParameters(-9, 1.0, 0.5, 0.25));
		context.register(IR_EDGE, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
		context.register(IR_TERRACE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
		context.register(IR_RELIEF, new NormalNoise.NoiseParameters(-6, 1.0, 0.5));
		context.register(IR_RANGE, new NormalNoise.NoiseParameters(-8, 1.0, 0.5));
		context.register(IR_RIDGE, new NormalNoise.NoiseParameters(-6, 1.0));
		context.register(IR_PILLAR, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(IR_PILLAR_TOP, new NormalNoise.NoiseParameters(-6, 1.0));
		context.register(IR_UNDER, new NormalNoise.NoiseParameters(-5, 1.0, 0.5));
		context.register(IR_GARDEN, new NormalNoise.NoiseParameters(-8, 1.0, 1.0, 0.5));
		context.register(IR_CITY_SITE, new NormalNoise.NoiseParameters(0, 1.0));
	}

	public static void bootstrap(BootstapContext<NoiseGeneratorSettings> context) {
		HolderGetter<NormalNoise.NoiseParameters> noises = context.lookup(Registries.NOISE);
		context.register(UPPER_REALM, upperRealm(noises));
		context.register(SPATIAL_GAP, spatialGap());
		context.register(NETHERWORLD, netherworld(noises));
		context.register(GREAT_VOID, greatVoid(noises));
		context.register(IMMORTAL_REALM, immortalRealm(noises));
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

	// --- The Great Void ---

	private static final int GV_MIN_Y = ModDimensions.GREAT_VOID_MIN_Y;
	private static final int GV_MAX_Y = GV_MIN_Y + ModDimensions.GREAT_VOID_HEIGHT; // 512
	/**
	 * A continent's ground lies about Y {@link #GV_BASE_Y}, rolling up to {@link #GV_RELIEF_BLOCKS} either way; its mountain ranges
	 * rise {@link #GV_MOUNTAIN_BLOCKS} (and up to {@link #GV_RIDGE_BLOCKS} more along their ridges) a little way in from the rim. The
	 * land is {@link #GV_THICK_RIM} blocks thick at a rim and up to {@link #GV_THICK_INLAND} more inland, hanging another
	 * {@link #GV_HANG} lower under the mountains, so a continent seen from below is an upturned mountain.
	 */
	private static final int GV_BASE_Y = 200, GV_RELIEF_BLOCKS = 16, GV_MOUNTAIN_BLOCKS = 80, GV_RIDGE_BLOCKS = 90;
	private static final int GV_THICK_RIM = 24, GV_THICK_INLAND = 80, GV_HANG = 30;
	/** The continents' mask: the noise times this, plus the offset (land where above 0: about half the sky), and the rims' raggedness. */
	private static final double GV_MASK_STRENGTH = 1.8, GV_MASK_OFFSET = -0.05, GV_EDGE_STRENGTH = 0.12;
	/** A mountain range rises where the mountain noise passes this, and stands full height this much further on. */
	private static final double GV_MOUNTAIN_FROM = 0.15, GV_MOUNTAIN_RAMP = 0.33;
	/**
	 * The Shore of Nirvana about 0, 0 ({@link NirvanaShore}): sure, level land within {@link #SHORE_INNER} blocks, fading out by
	 * {@link #SHORE_OUTER}; north of it the void sea, a disc of {@link #SHORE_SEA_RADIUS} about (0, -{@link #SHORE_SEA_CENTRE}), so the
	 * shore's coast runs some 190 blocks north of 0, 0 and nothing stands beyond it for some 650 blocks.
	 */
	public static final double SHORE_INNER = 150, SHORE_OUTER = 230, SHORE_SEA_CENTRE = 520, SHORE_SEA_RADIUS = 330;
	/** The land lies almost level within {@link #SHORE_CALM_INNER} of 0, 0 (its coast too: no mountain stands at the sea), and is its own by the outer. */
	private static final double SHORE_CALM_INNER = 240, SHORE_CALM_OUTER = 340;
	/** The 3D roughness on the continents' ground (as density: about a block of height a tenth), breaking up the smooth hills' terraces. */
	private static final double GV_DETAIL = 0.28;
	/** The high isles: a sparse tier about Y {@link #GV_ISLE_Y} (the mask's offset keeps them to some one sky in eight). */
	private static final double GV_ISLE_STRENGTH = 1.6, GV_ISLE_OFFSET = -0.62;
	private static final int GV_ISLE_Y = 400;

	/** The Shore of Nirvana's density function, as the terrain adds it ({@link NirvanaShore}). */
	public static NirvanaShore nirvanaShore() {
		return new NirvanaShore(SHORE_INNER, SHORE_OUTER, SHORE_SEA_CENTRE, SHORE_SEA_RADIUS);
	}

	/**
	 * The Great Void's continents' mask at x, z in {@code state}'s world, as the terrain reads it: positive over a continent, 0 at its
	 * rim, negative over the void (the Shore of Nirvana's land and sea counted in). Three 2D noise samples: SpatialArrivals finds a
	 * breach's landing by it, VoidBreach's arrivals too, nothing generated to look.
	 */
	public static double continentMask(RandomState state, int x, int z) {
		NirvanaShore shore = nirvanaShore();
		return state.getOrCreateNoise(GV_CONTINENT).getValue(x, 0, z) * GV_MASK_STRENGTH + GV_MASK_OFFSET
				+ state.getOrCreateNoise(GV_EDGE).getValue(x, 0, z) * GV_EDGE_STRENGTH
				+ NirvanaShore.LAND * shore.plateau(x, z) - NirvanaShore.SEA * shore.sea(x, z);
	}

	/**
	 * Continents far greater than the Jade Firmament's islands, floating under the starlit void. One land, as the Netherworld's (2D
	 * where it can be, so cheap): a mask that says where the continents are, cut off at their rims in sheer cliffs (the mask's own wall,
	 * roughened by the edge noise); a top surface rolling about Y {@link #GV_BASE_Y} with mountain ranges rising inland; an underside
	 * hanging deeper the further in, deepest under the mountains; 3D detail on the top; caverns sealed deep inside. Above them a
	 * sparse tier of high isles ({@link #tier}, as the Jade Firmament's), and shards of land drifting off the continents' rims. At the
	 * world's 0, 0, the Shore of Nirvana ({@link NirvanaShore}): level land, then a coast onto a sea of open void.
	 */
	private static NoiseGeneratorSettings greatVoid(HolderGetter<NormalNoise.NoiseParameters> noises) {
		DensityFunction zero = DensityFunctions.zero();
		DensityFunction plateau = DensityFunctions.flatCache(new OriginPlateau(SHORE_INNER, SHORE_OUTER));
		DensityFunction shore = DensityFunctions.flatCache(nirvanaShore());
		DensityFunction mask = DensityFunctions.flatCache(add(add(scale(flat(noises, GV_CONTINENT, 1.0), GV_MASK_STRENGTH), DensityFunctions.constant(GV_MASK_OFFSET)),
				add(scale(flat(noises, GV_EDGE, 1.0), GV_EDGE_STRENGTH), shore)));
		// 0 at a rim, 1 a little way in: mountains rise and the land thickens inland.
		DensityFunction inland = scale(mask, 3.0).clamp(0.0, 1.0);
		DensityFunction range = add(scale(flat(noises, GV_MOUNTAIN, 1.0), 1.0 / GV_MOUNTAIN_RAMP),
				DensityFunctions.constant(-GV_MOUNTAIN_FROM / GV_MOUNTAIN_RAMP)).clamp(0.0, 1.0);
		// Sharp ridges where the ridge noise crosses zero.
		DensityFunction ridge = add(DensityFunctions.constant(1.0), scale(flat(noises, GV_RIDGE, 1.0).abs(), -2.0)).clamp(0.0, 1.0);
		DensityFunction mountains = DensityFunctions.mul(DensityFunctions.mul(range, inland),
				add(DensityFunctions.constant(GV_MOUNTAIN_BLOCKS), scale(ridge, GV_RIDGE_BLOCKS)));
		// The Shore of Nirvana lies almost level out to its coast (its variation cut by nine tenths); every rim drops a little toward its cliff.
		DensityFunction calm = DensityFunctions.flatCache(new OriginPlateau(SHORE_CALM_INNER, SHORE_CALM_OUTER));
		DensityFunction level = add(DensityFunctions.constant(1.0), scale(calm, -0.92));
		DensityFunction ground = DensityFunctions.flatCache(add(add(DensityFunctions.constant(GV_BASE_Y),
				DensityFunctions.mul(scale(flat(noises, GV_RELIEF, 1.0), GV_RELIEF_BLOCKS), level)),
				scale(add(DensityFunctions.constant(1.0), scale(inland, -1.0)), -10.0)));
		DensityFunction top = DensityFunctions.flatCache(add(ground, DensityFunctions.mul(mountains, level)));
		DensityFunction y = DensityFunctions.yClampedGradient(GV_MIN_Y, GV_MAX_Y, GV_MIN_Y, GV_MAX_Y);

		DensityFunction detail = scale(DensityFunctions.noise(noises.getOrThrow(DETAIL), 1.0, 1.0), GV_DETAIL);
		DensityFunction topDensity = add(scale(add(top, scale(y, -1.0)), 1.0 / 12.0), detail);
		// The underside follows the ground, not the mountains (a mountain is solid down to it), and hangs deeper inland and under them.
		DensityFunction bottom = add(ground, add(add(DensityFunctions.constant(-GV_THICK_RIM), scale(inland, -GV_THICK_INLAND)),
				add(scale(range, -GV_HANG), scale(flat(noises, GV_UNDER, 1.0), 16.0))));
		DensityFunction bottomDensity = scale(add(y, scale(bottom, -1.0)), 1.0 / 8.0);
		DensityFunction continents = DensityFunctions.min(DensityFunctions.min(topDensity, bottomDensity), scale(mask, 8.0));

		// The high isles, and shards of land adrift just off the continents' rims.
		DensityFunction isleMask = add(mask(noises, GV_ISLE, GV_ISLE_STRENGTH, GV_ISLE_OFFSET), scale(flat(noises, GV_EDGE, 1.0), 0.06));
		DensityFunction isles = tier(noises, isleMask, STYLE_HIGH, GV_ISLE_Y);
		DensityFunction boulderSpots = add(scale(flat(noises, BOULDER, 1.0), 2.0), DensityFunctions.constant(-1.0));
		DensityFunction offRim = DensityFunctions.min(scale(add(mask, DensityFunctions.constant(0.4)), 2.5), scale(mask, -5.0));
		DensityFunction shards = add(DensityFunctions.min(boulderSpots, offRim), tent(GV_BASE_Y - 10, 8, 12));
		DensityFunction land = DensityFunctions.max(continents, DensityFunctions.max(isles, shards));

		// The tallest peaks rounded off, then caverns sealed deep inside (as the Jade Firmament's).
		DensityFunction taper = scale(DensityFunctions.yClampedGradient(PEAK_TAPER_Y, GV_MAX_Y, 0.0, 1.0).square(), -PEAK_TAPER);
		DensityFunction rough = add(land, taper);
		DensityFunction caverns = scale(add(DensityFunctions.constant(0.5), scale(DensityFunctions.noise(noises.getOrThrow(CAVERN), 1.0, 0.75), -1.0)), 3.0);
		DensityFunction shell = add(DensityFunctions.constant(CAVERN_SHELL), scale(rough, -1.0));
		DensityFunction carved = DensityFunctions.min(rough, DensityFunctions.max(caverns, shell));
		DensityFunction bottomSlide = DensityFunctions.yClampedGradient(GV_MIN_Y, 24, -1.0, 10.0);
		DensityFunction topSlide = DensityFunctions.yClampedGradient(480, GV_MAX_Y, 10.0, -1.0);
		DensityFunction terrain = DensityFunctions.min(DensityFunctions.min(carved, bottomSlide), topSlide);

		NoiseRouter router = new NoiseRouter(
				zero, zero, zero, zero,                                    // no aquifers or lava sheets
				flat(noises, GV_ELEMENT, 1.0),                             // "temperature": which element's Dao field
				flat(noises, GV_DAO, 1.0),                                 // "humidity": a Dao field where high
				plateau,                                                   // "continentalness": the Shore of Nirvana about 0, 0
				zero, zero,                                                // erosion, depth
				flat(noises, GV_BATTLE, 1.0),                              // "weirdness": an old battlefield where high
				terrain,
				DensityFunctions.interpolated(terrain),
				zero, zero, zero);
		return new NoiseGeneratorSettings(
				NoiseSettings.create(GV_MIN_Y, ModDimensions.GREAT_VOID_HEIGHT, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				greatVoidSurface(),
				List.of(),
				GV_MIN_Y,   // "sea level" at the floor: no seas, the void below stays open
				false,      // mob generation on
				false,      // no aquifers
				false,      // ore veins off (ores come from placed features)
				false);
	}

	/**
	 * The Great Void's ground, each land its own: the void steppes' silvered grass, the Dao fields' (a wood field's moss and grass, a fire
	 * field's red sand and terracotta with magma breaking through, an earth field's banded terracotta, a metal field's steel-grey tuff and andesite,
	 * a water field's grass and clay), an old battlefield's churned earth, gravel and soul soil, and the Shore of Nirvana's pale calcite
	 * with sand and grass; stone beneath them all.
	 */
	private static SurfaceRules.RuleSource greatVoidSurface() {
		SurfaceRules.RuleSource grass = block(Blocks.GRASS_BLOCK);
		SurfaceRules.RuleSource dirt = block(Blocks.DIRT);
		SurfaceRules.RuleSource steppe = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.4), block(Blocks.COARSE_DIRT)),
						grass)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt));
		SurfaceRules.RuleSource wood = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.25), block(Blocks.MOSS_BLOCK)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.5, -0.3), block(Blocks.PODZOL)),
						grass)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt));
		SurfaceRules.RuleSource fire = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.012, 0.012), block(Blocks.BASALT)),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.5), block(Blocks.MAGMA_BLOCK)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.1), block(Blocks.RED_TERRACOTTA)),
						block(Blocks.RED_SAND))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.RED_SANDSTONE)));
		SurfaceRules.RuleSource earth = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.3), block(Blocks.COARSE_DIRT)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.2, 0.0), block(Blocks.PACKED_MUD)),
						block(Blocks.YELLOW_TERRACOTTA))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, 0.0, 0.3), block(Blocks.ORANGE_TERRACOTTA)),
						block(Blocks.TERRACOTTA))));
		// Steel-grey ground: tuff and andesite, smooth basalt in its seams, calcite catching the light now and then.
		SurfaceRules.RuleSource metal = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.012, 0.012), block(Blocks.SMOOTH_BASALT)),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.45), block(Blocks.CALCITE)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.0, 0.45), block(Blocks.POLISHED_ANDESITE)),
						block(Blocks.TUFF))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.TUFF)));
		SurfaceRules.RuleSource water = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35), block(Blocks.CLAY)),
						grass)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt));
		SurfaceRules.RuleSource battlefield = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.03, 0.03), block(Blocks.BLACKSTONE)),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.3), block(Blocks.GRAVEL)),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.5, -0.25), block(Blocks.SOUL_SOIL)),
						block(Blocks.COARSE_DIRT))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.COARSE_DIRT)));
		SurfaceRules.RuleSource shore = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.3), grass),
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.4, -0.15), block(Blocks.SAND)),
						block(Blocks.CALCITE))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.CALCITE)));
		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.SHORE_OF_NIRVANA), shore),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.ANCIENT_BATTLEFIELD), battlefield),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DAO_FIELD_WOOD), wood),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DAO_FIELD_FIRE), fire),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DAO_FIELD_EARTH), earth),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DAO_FIELD_METAL), metal),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.DAO_FIELD_WATER), water),
				steppe); // the Void Steppes, and anything else
	}

	// --- The Immortal Realm ---

	private static final int IR_MIN_Y = ModDimensions.IMMORTAL_REALM_MIN_Y;
	private static final int IR_MAX_Y = IR_MIN_Y + ModDimensions.IMMORTAL_REALM_HEIGHT; // 512
	private static final int CLOUD_Y = ModDimensions.IMMORTAL_CLOUD_Y;
	/**
	 * The massifs' mask: the noise times this, plus the offset (land where above 0: some one sky in six, the rest the sea of clouds and
	 * its pinnacles; tour 1, at a third of it, the clouds were lost behind land to the horizon), and the rims' raggedness.
	 */
	private static final double IR_MASK_STRENGTH = 1.7, IR_MASK_OFFSET = -0.55, IR_EDGE_STRENGTH = 0.1;
	/**
	 * A massif's levels over its terrace noise (a cubic spline, flat at each level, so each lies level with a steep step of some twenty
	 * blocks up to the next): just over the clouds, then each the higher. Small hills of up to {@link #IR_RELIEF_BLOCKS} roll on them.
	 */
	private static final float[][] IR_TERRACES = {{-1.0f, 174}, {-0.6f, 180}, {-0.5f, 200}, {-0.2f, 202}, {-0.1f, 224}, {0.15f, 226}, {0.25f, 250},
			{0.5f, 252}, {0.6f, 280}, {1.0f, 284}};
	private static final int IR_RELIEF_BLOCKS = 4;
	/** A mountain range rises where its noise passes this, full height this much further on: some sixty blocks, and up to ninety more along its ridges. */
	private static final double IR_RANGE_FROM = 0.15, IR_RANGE_RAMP = 0.3;
	private static final int IR_MOUNTAIN_BLOCKS = 60, IR_RIDGE_BLOCKS = 90;
	/**
	 * The jade pinnacles standing out of the sea of clouds: where the pillar noise (some thirty blocks across) passes {@link #IR_PILLAR_FROM}
	 * (their walls sheer by {@link #IR_PILLAR_WALL}), their tops about Y {@link #IR_PILLAR_TOP_Y}, each its own some
	 * {@link #IR_PILLAR_TOP_BLOCKS} either way, their roots {@link #IR_PILLAR_ROOT} below the clouds' top.
	 */
	private static final double IR_PILLAR_FROM = 0.45, IR_PILLAR_WALL = 10;
	private static final int IR_PILLAR_TOP_Y = 240, IR_PILLAR_TOP_BLOCKS = 90, IR_PILLAR_ROOT = 70;
	/** A massif's roots hang {@link #IR_ROOT_RIM} below the clouds' top at a rim, up to {@link #IR_ROOT_INLAND} more inland. */
	private static final int IR_ROOT_RIM = 30, IR_ROOT_INLAND = 70;
	/**
	 * The main island (the user's word, 2026-10-25: "the immortal gate should lead to a main island where the biggest immortal city along
	 * with the immortal court resides"): sure land within {@link #MAIN_INNER} of ({@link #MAIN_X}, {@link #MAIN_Z}), the realm's own by
	 * {@link #MAIN_OUTER}; within {@link #CITY_INNER} gently rolling ground about Y {@link #GATE_Y} (long swells of some
	 * {@link #CITY_SWELL_BLOCKS} either way and small hills of {@link #CITY_RELIEF_BLOCKS}, no steps or mountains: the user's ask, 2026-10-25,
	 * the cities "dont need to be on perfectly flat ground", so the capital is laid on it as it lies, ImmortalCapital), the land's own
	 * terraces and peaks back by {@link #CITY_OUTER}, so its rim rises in hills and peaks about the capital. The Southern Heavenly Gate (the
	 * Immortal Gate's other side, ImmortalGate) at the world's 0, 0 near its south end; at the capital's north end the Celestial Court's
	 * pinnacle about ({@link #COURT_X}, {@link #COURT_Z}): level at Y {@link #COURT_Y} within {@link #COURT_INNER}, its walls sheer down to
	 * the city by {@link #COURT_OUTER}.
	 */
	public static final int MAIN_X = 0, MAIN_Z = -260;
	public static final double MAIN_INNER = 300, MAIN_OUTER = 380, CITY_INNER = 290, CITY_OUTER = 340;
	public static final int GATE_Y = 240, CITY_SWELL_BLOCKS = 16, CITY_RELIEF_BLOCKS = 3;
	public static final int COURT_X = 0, COURT_Z = -440, COURT_Y = 330;
	public static final double COURT_INNER = 74, COURT_OUTER = 90;
	/** The 3D roughness on the ground (as density, about a block and a half of height), and how much of it the capital's ground loses. */
	private static final double IR_DETAIL = 0.15, IR_CITY_SMOOTHING = 0.75;
	/**
	 * How deep under the land's top the caverns keep sealed (as density: 25 blocks at the realm's 10 blocks a unit). The Jade Firmament's
	 * {@link #CAVERN_SHELL} is 5 blocks here, and where the land is cut down round a building (ImmortalCapital's Beardifier, which comes
	 * after) the caverns under it opened as trenches across the capital's streets (tour 4).
	 */
	private static final double IR_CAVERN_SHELL = 2.5;

	/** The main island ({@link #MAIN_INNER}, {@link #MAIN_OUTER}). */
	public static Summit mainIsland() {
		return new Summit(MAIN_X, MAIN_Z, MAIN_INNER, MAIN_OUTER);
	}

	/** The main island's rolling ground, the capital's ({@link #CITY_INNER}, {@link #CITY_OUTER}). */
	public static Summit cityGround() {
		return new Summit(MAIN_X, MAIN_Z, CITY_INNER, CITY_OUTER);
	}

	/** The Celestial Court's summit. */
	public static Summit courtSummit() {
		return new Summit(COURT_X, COURT_Z, COURT_INNER, COURT_OUTER);
	}

	/**
	 * The Immortal Realm's massifs' mask at x, z in {@code state}'s world, as the terrain reads it: positive over a massif, 0 at its rim,
	 * negative over the sea of clouds (the main island and the Court's pinnacle counted in; its lone pillars not). Two 2D noise samples.
	 */
	public static double massifMask(RandomState state, int x, int z) {
		return state.getOrCreateNoise(IR_MASSIF).getValue(x, 0, z) * IR_MASK_STRENGTH + IR_MASK_OFFSET
				+ state.getOrCreateNoise(IR_EDGE).getValue(x, 0, z) * IR_EDGE_STRENGTH
				+ 3.0 * mainIsland().at(x, z) + 3.0 * courtSummit().at(x, z) + 3.0 * cityDistrict(state, x, z);
	}

	/** A city's district at column (x, z) in {@code state}'s world, as the terrain reads it (CityDistrict): 1 about a city, 0 beyond. */
	public static double cityDistrict(RandomState state, double x, double z) {
		return district(state.getOrCreateNoise(IR_CITY_SITE), state.getOrCreateNoise(IR_MASSIF), state.getOrCreateNoise(IR_EDGE),
				state.getOrCreateNoise(IR_TERRACE), x, z)[0];
	}

	/** A district's weight this far from its site: 1 within {@link #CITY_DISTRICT_INNER}, easing to 0 by {@link #CITY_DISTRICT_OUTER}. */
	static double districtWeight(double distance) {
		double t = Mth.clamp((CITY_DISTRICT_OUTER - distance) / (CITY_DISTRICT_OUTER - CITY_DISTRICT_INNER), 0.0, 1.0);
		return t * t * (3 - 2 * t);
	}

	/**
	 * An endless sea of clouds (drawn by the client at {@link ModDimensions#IMMORTAL_CLOUD_Y}: nothing of it is a block) with jade peaks
	 * rising out of it. Massifs (a 2D mask, cut off at their rims in sheer cliffs) terraced into level grounds with steep steps between
	 * ({@link #IR_TERRACES}), mountain ranges with sharp ridges inland, their roots hanging into the clouds; jade pinnacles standing alone
	 * out of the sea between them; and the main island ({@link #MAIN_X}, {@link #MAIN_Z}): level ground for the capital, the Southern
	 * Heavenly Gate at its south end, the Celestial Court's pinnacle at its north end. Waterfalls spill off the cliffs into the clouds (the
	 * springs' feature).
	 */
	private static NoiseGeneratorSettings immortalRealm(HolderGetter<NormalNoise.NoiseParameters> noises) {
		DensityFunction zero = DensityFunctions.zero();
		DensityFunction island = DensityFunctions.flatCache(mainIsland());
		DensityFunction city = DensityFunctions.flatCache(cityGround());
		DensityFunction court = DensityFunctions.flatCache(courtSummit());
		// The realm's other cities' districts (CityDistrict): each its own land (a lobe of its massif where it stands near the rim), rolling
		// gently about its site's level, no pinnacles or mountains in it.
		DensityFunction district = DensityFunctions.flatCache(new CityDistrict(new DensityFunction.NoiseHolder(noises.getOrThrow(IR_CITY_SITE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_MASSIF)), new DensityFunction.NoiseHolder(noises.getOrThrow(IR_EDGE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_TERRACE)), false, false));
		DensityFunction districtLevel = DensityFunctions.flatCache(new CityDistrict(new DensityFunction.NoiseHolder(noises.getOrThrow(IR_CITY_SITE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_MASSIF)), new DensityFunction.NoiseHolder(noises.getOrThrow(IR_EDGE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_TERRACE)), true, false));
		// The immortal sects' clearings (ImmortalSectPlan): open sky about each, its islands floating over the sea of clouds with no pinnacle
		// among them.
		DensityFunction clearing = DensityFunctions.flatCache(new CityDistrict(new DensityFunction.NoiseHolder(noises.getOrThrow(IR_CITY_SITE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_MASSIF)), new DensityFunction.NoiseHolder(noises.getOrThrow(IR_EDGE)),
				new DensityFunction.NoiseHolder(noises.getOrThrow(IR_TERRACE)), false, true));
		DensityFunction mask = DensityFunctions.flatCache(add(add(scale(flat(noises, IR_MASSIF, 1.0), IR_MASK_STRENGTH), DensityFunctions.constant(IR_MASK_OFFSET)),
				add(scale(flat(noises, IR_EDGE, 1.0), IR_EDGE_STRENGTH), add(add(scale(island, 3.0), scale(court, 3.0)), scale(district, 3.0)))));
		// 0 at a rim, 1 a little way in: mountains rise and the roots hang deeper inland.
		DensityFunction inland = scale(mask, 2.5).clamp(0.0, 1.0);
		DensityFunction range = add(scale(flat(noises, IR_RANGE, 1.0), 1.0 / IR_RANGE_RAMP), DensityFunctions.constant(-IR_RANGE_FROM / IR_RANGE_RAMP)).clamp(0.0, 1.0);
		DensityFunction ridge = add(DensityFunctions.constant(1.0), scale(flat(noises, IR_RIDGE, 1.0).abs(), -2.0)).clamp(0.0, 1.0);
		DensityFunction mountains = DensityFunctions.mul(DensityFunctions.mul(range, inland),
				add(DensityFunctions.constant(IR_MOUNTAIN_BLOCKS), scale(ridge, IR_RIDGE_BLOCKS)));
		// The capital's ground rolls gently about GATE_Y (the terrace noise's long swells and the relief's small hills, no steps or mountains);
		// elsewhere the terraces, their hills and the mountains, every rim dropping a little to its cliff.
		DensityFunction terraced = add(add(terraces(flat(noises, IR_TERRACE, 1.0)), scale(flat(noises, IR_RELIEF, 1.0), IR_RELIEF_BLOCKS)),
				add(mountains, scale(add(DensityFunctions.constant(1.0), scale(inland, -1.0)), -8.0)));
		// (The terrace noise clamped: at its rare extremes it reaches some two and a half, tour 4's ground fell to Y 200.)
		DensityFunction rolling = add(DensityFunctions.constant(GATE_Y),
				add(scale(flat(noises, IR_TERRACE, 1.0).clamp(-1.25, 1.25), CITY_SWELL_BLOCKS), scale(flat(noises, IR_RELIEF, 1.0), CITY_RELIEF_BLOCKS)));
		DensityFunction capitalGround = add(DensityFunctions.mul(terraced, add(DensityFunctions.constant(1.0), scale(city, -1.0))), DensityFunctions.mul(city, rolling));
		DensityFunction districtGround = add(districtLevel,
				add(scale(flat(noises, IR_TERRACE, 1.0).clamp(-1.25, 1.25), CITY_SITE_SWELL), scale(flat(noises, IR_RELIEF, 1.0), CITY_RELIEF_BLOCKS)));
		DensityFunction ground = add(DensityFunctions.mul(capitalGround, add(DensityFunctions.constant(1.0), scale(district, -1.0))),
				DensityFunctions.mul(district, districtGround));
		// The Court's pinnacle: level at COURT_Y, its walls sheer and ragged, the rim pushed in and out by the edge noise (tour 2: a smooth
		// cylinder); its top and its foot as they were.
		DensityFunction rim = scale(DensityFunctions.mul(court, add(DensityFunctions.constant(1.0), scale(court, -1.0))), 4.0);
		DensityFunction ragged = add(court, scale(DensityFunctions.mul(rim, flat(noises, IR_EDGE, 1.0)), 0.7)).clamp(0.0, 1.0);
		DensityFunction summit = scale(ragged, COURT_Y);
		DensityFunction top = DensityFunctions.flatCache(DensityFunctions.max(ground, summit));
		DensityFunction y = DensityFunctions.yClampedGradient(IR_MIN_Y, IR_MAX_Y, IR_MIN_Y, IR_MAX_Y);

		DensityFunction detail = scale(DensityFunctions.noise(noises.getOrThrow(DETAIL), 1.0, 1.0), IR_DETAIL);
		// The capital's ground smoother (tour 4: the roughness left lone blocks standing up out of its paved ways, laid on the ground as it lies).
		DensityFunction groundDetail = DensityFunctions.mul(detail, add(DensityFunctions.constant(1.0), scale(add(city, district), -IR_CITY_SMOOTHING)));
		DensityFunction topDensity = add(scale(add(top, scale(y, -1.0)), 1.0 / 10.0), groundDetail);
		DensityFunction roots = add(DensityFunctions.constant(CLOUD_Y - IR_ROOT_RIM), add(scale(inland, -IR_ROOT_INLAND), scale(flat(noises, IR_UNDER, 1.0), 14.0)));
		DensityFunction rootDensity = scale(add(y, scale(roots, -1.0)), 1.0 / 8.0);
		DensityFunction massifs = DensityFunctions.min(DensityFunctions.min(topDensity, rootDensity), scale(mask, 8.0));

		// The pinnacles, none on the main island or the Court's, nor in a city's district or an immortal sect's clearing.
		DensityFunction pillar = add(scale(add(flat(noises, IR_PILLAR, 1.0), DensityFunctions.constant(-IR_PILLAR_FROM)), IR_PILLAR_WALL),
				scale(add(add(island, court), add(district, clearing)), -8.0)).clamp(-1.0, 1.0);
		DensityFunction pillarTop = add(DensityFunctions.constant(IR_PILLAR_TOP_Y), scale(flat(noises, IR_PILLAR_TOP, 1.0), IR_PILLAR_TOP_BLOCKS));
		DensityFunction pillarTopDensity = add(scale(add(pillarTop, scale(y, -1.0)), 1.0 / 4.0), detail);
		DensityFunction pillarRootDensity = scale(add(y, DensityFunctions.constant(-(CLOUD_Y - IR_PILLAR_ROOT))), 1.0 / 8.0);
		DensityFunction pillars = DensityFunctions.min(DensityFunctions.min(scale(pillar, 3.0), pillarTopDensity), pillarRootDensity);
		DensityFunction land = DensityFunctions.max(massifs, pillars);

		// The tallest peaks rounded off, then caverns sealed deep inside (as the Jade Firmament's).
		DensityFunction taper = scale(DensityFunctions.yClampedGradient(PEAK_TAPER_Y, IR_MAX_Y, 0.0, 1.0).square(), -PEAK_TAPER);
		DensityFunction rough = add(land, taper);
		DensityFunction caverns = scale(add(DensityFunctions.constant(0.5), scale(DensityFunctions.noise(noises.getOrThrow(CAVERN), 1.0, 0.75), -1.0)), 3.0);
		DensityFunction shell = add(DensityFunctions.constant(IR_CAVERN_SHELL), scale(rough, -1.0));
		DensityFunction carved = DensityFunctions.min(rough, DensityFunctions.max(caverns, shell));
		DensityFunction bottomSlide = DensityFunctions.yClampedGradient(IR_MIN_Y, IR_MIN_Y + 24, -1.0, 10.0);
		DensityFunction topSlide = DensityFunctions.yClampedGradient(IR_MAX_Y - 32, IR_MAX_Y, 10.0, -1.0);
		DensityFunction terrain = DensityFunctions.min(DensityFunctions.min(carved, bottomSlide), topSlide);

		NoiseRouter router = new NoiseRouter(
				zero, zero, zero, zero,                                    // no aquifers or lava sheets
				flat(noises, IR_GARDEN, 1.0),                              // "temperature": the Immortal Gardens where high
				zero,                                                      // humidity
				city,                                                      // "continentalness": the capital's level ground
				zero,                                                      // erosion
				DensityFunctions.yClampedGradient(IR_MIN_Y, IR_MAX_Y, 1.0, -1.0), // "depth": the Celestial Heights above ~Y 320
				zero,                                                      // weirdness
				terrain,
				DensityFunctions.interpolated(terrain),
				zero, zero, zero);
		return new NoiseGeneratorSettings(
				NoiseSettings.create(IR_MIN_Y, ModDimensions.IMMORTAL_REALM_HEIGHT, 1, 2),
				Blocks.STONE.defaultBlockState(),
				Blocks.WATER.defaultBlockState(),
				router,
				immortalSurface(),
				List.of(),
				IR_MIN_Y,   // "sea level" at the floor: no seas, the clouds are the client's
				false,      // mob generation on
				false,      // no aquifers
				false,      // ore veins off (ores come from placed features)
				false);
	}

	/** The massifs' levels over the terrace noise ({@link #IR_TERRACES}): a cubic spline flat at every point. */
	private static DensityFunction terraces(DensityFunction input) {
		CubicSpline.Builder<DensityFunctions.Spline.Point, DensityFunctions.Spline.Coordinate> spline = CubicSpline.builder(
				new DensityFunctions.Spline.Coordinate(Holder.direct(input)));
		for (float[] point : IR_TERRACES) spline.addPoint(point[0], point[1], 0.0f);
		return DensityFunctions.spline(spline.build());
	}

	/**
	 * The Immortal Realm's ground: the Jade Peaks' grass and moss over dirt, their steps bare stone banded with jade; the Immortal Gardens'
	 * lawns; the Celestial Heights' white jade (calcite) with grass in its hollows. Under every top, down every column, streaks of jade and
	 * of white jade, so the sheer walls of the pinnacles (whose faces no top rule reaches) run green and white down into the clouds (tour
	 * 1: bare grey stone); stone between them.
	 */
	private static SurfaceRules.RuleSource immortalSurface() {
		SurfaceRules.RuleSource grass = block(Blocks.GRASS_BLOCK);
		SurfaceRules.RuleSource dirt = block(Blocks.DIRT);
		SurfaceRules.RuleSource cliff = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.15, 0.25), block(ModBlocks.JADE_STONE)),
				block(Blocks.STONE));
		SurfaceRules.RuleSource streaks = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.12, 0.2), block(ModBlocks.JADE_STONE)),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.CALCITE, -0.04, 0.04), block(Blocks.CALCITE)));
		SurfaceRules.RuleSource peaks = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.steep(), cliff),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.45), block(Blocks.MOSS_BLOCK)),
						grass)),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt),
				streaks);
		SurfaceRules.RuleSource gardens = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.steep(), cliff),
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grass),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt),
				streaks);
		SurfaceRules.RuleSource heights = SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
						SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.35), grass),
						block(Blocks.CALCITE))),
				SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, block(Blocks.CALCITE)),
				SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.3, 0.3), block(Blocks.CALCITE)),
				streaks);
		return SurfaceRules.sequence(
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.CELESTIAL_HEIGHTS), heights),
				SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.IMMORTAL_GARDENS, ModBiomes.JADE_CAPITAL), gardens),
				peaks); // the Jade Peaks, and anything else
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
