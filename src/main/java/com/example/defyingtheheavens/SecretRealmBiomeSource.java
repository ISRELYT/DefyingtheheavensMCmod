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
 * The secret realms' biomes (ModDimensions#SECRET_REALM, 2026-10-31): each realm's own all over its slot (SecretRealm#biome: its sky, mist,
 * grass and motes), the first realm's between them. Registered as "defying-the-heavens:secret_realms" (SecretRealms#register); dimension
 * secret_realm.json names it.
 */
public class SecretRealmBiomeSource extends BiomeSource {
	public static final Codec<SecretRealmBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(RegistryOps.retrieveGetter(Registries.BIOME))
			.apply(instance, instance.stable(SecretRealmBiomeSource::new)));

	private final Map<SecretRealm, Holder<Biome>> biomes = new EnumMap<>(SecretRealm.class);
	private final Holder<Biome> between;

	public SecretRealmBiomeSource(HolderGetter<Biome> lookup) {
		for (SecretRealm realm : SecretRealm.values()) biomes.put(realm, lookup.getOrThrow(realm.biome()));
		between = biomes.get(SecretRealm.values()[0]);
	}

	@Override
	protected Codec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		Set<Holder<Biome>> all = new LinkedHashSet<>(biomes.values());
		return all.stream();
	}

	/** By quart (4 blocks): the realm whose slot the quart column lies in, the same at every height. */
	@Override
	public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
		SecretRealm realm = SecretRealm.at((qx << 2) + 2, (qz << 2) + 2);
		return realm == null ? between : biomes.get(realm);
	}
}
