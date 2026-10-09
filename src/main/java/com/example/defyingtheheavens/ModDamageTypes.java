package com.example.defyingtheheavens;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
	/** Raw spatial pressure in the Spatial Gap: tagged (by datagen) to bypass armor, enchantments and effects. */
	public static final ResourceKey<DamageType> SPATIAL_PRESSURE = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("spatial_pressure"));

	public static void bootstrap(BootstapContext<DamageType> context) {
		context.register(SPATIAL_PRESSURE, new DamageType(DefyingTheHeavens.MOD_ID + ".spatial_pressure", DamageScaling.NEVER, 0.0f));
	}

	public static DamageSource spatialPressure(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPATIAL_PRESSURE));
	}

	private ModDamageTypes() {}
}
