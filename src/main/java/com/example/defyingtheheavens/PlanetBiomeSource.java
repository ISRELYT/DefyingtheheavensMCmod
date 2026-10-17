package com.example.defyingtheheavens;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The biomes of the planets' lands (ModDimensions#ASTRAL_SURFACES): at each point, its planet's {@link PlanetTerrain} zone's
 * biome; between the planets, the void's. Registered as "defying-the-heavens:planets" (DefyingTheHeavens); dimension
 * astral_surfaces.json names it.
 */
public class PlanetBiomeSource extends BiomeSource {
	public static final Codec<PlanetBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(RegistryOps.retrieveGetter(Registries.BIOME))
			.apply(instance, instance.stable(PlanetBiomeSource::new)));

	private final Map<PlanetTerrain.Zone, Holder<Biome>> biomes = new EnumMap<>(PlanetTerrain.Zone.class);
	private final Holder<Biome> space;
	/** The last zone worked out for each quart column, by thread (a chunk asks the same 16 columns at every height). */
	private final ThreadLocal<long[]> cacheKeys = ThreadLocal.withInitial(() -> {
		long[] keys = new long[256];
		java.util.Arrays.fill(keys, Long.MIN_VALUE);
		return keys;
	});
	private final ThreadLocal<Holder<Biome>[]> cacheValues = ThreadLocal.withInitial(PlanetBiomeSource::newCache);

	@SuppressWarnings("unchecked")
	private static Holder<Biome>[] newCache() {
		return (Holder<Biome>[]) new Holder[256];
	}

	public PlanetBiomeSource(HolderGetter<Biome> lookup) {
		for (PlanetTerrain.Zone zone : PlanetTerrain.Zone.values()) biomes.put(zone, lookup.getOrThrow(zone.biome));
		space = lookup.getOrThrow(ModBiomes.ASTRAL_VOID);
	}

	@Override
	protected Codec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		Set<Holder<Biome>> all = new LinkedHashSet<>(biomes.values());
		all.add(space);
		return all.stream();
	}

	/** By quart (4 blocks): the zone at the quart column's middle, the same at every height. */
	@Override
	public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
		long key = ((long) qx << 32) | (qz & 0xFFFFFFFFL);
		int slot = (qx & 15) | (qz & 15) << 4;
		long[] keys = cacheKeys.get();
		Holder<Biome>[] values = cacheValues.get();
		if (keys[slot] == key) return values[slot];
		Holder<Biome> biome = biomeAt((qx << 2) + 2, (qz << 2) + 2);
		keys[slot] = key;
		values[slot] = biome;
		return biome;
	}

	private Holder<Biome> biomeAt(int x, int z) {
		AstralPlanets planets = AstralPlanets.get();
		AstralPlanets.Planet planet = planets == null ? null : planets.planetAt(x, z);
		if (planet == null) return space;
		PlanetTerrain.Column column = PlanetTerrain.of(planet).column(x - planet.landX(), z - planet.landZ(), false);
		return biomes.get(column.zone());
	}
}
