package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {
	/** Heavy dark-red motes that fall around anything under Realm Suppress (drawn by the client's SuppressionParticle). */
	public static final SimpleParticleType SUPPRESSION = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
			DefyingTheHeavens.id("suppression"), FabricParticleTypes.simple());

	/** Touching this class registers the particle types. */
	public static void register() {
	}

	private ModParticles() {}
}
