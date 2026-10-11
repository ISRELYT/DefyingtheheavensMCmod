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
	/**
	 * A nascent soul cut off from its body (see SoulProjection): nothing stands between it and death, not armor, not effects, not
	 * a totem (tagged, like spatial pressure, plus bypasses_invulnerability and bypasses_resistance).
	 */
	public static final ResourceKey<DamageType> SOUL_SEVERED = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("soul_severed"));
	/** A body taken by another cultivator's nascent soul (see SoulProjection#seize): tagged as {@link #SOUL_SEVERED}. */
	public static final ResourceKey<DamageType> SOUL_SEIZED = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("soul_seized"));

	public static void bootstrap(BootstapContext<DamageType> context) {
		context.register(SPATIAL_PRESSURE, new DamageType(DefyingTheHeavens.MOD_ID + ".spatial_pressure", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEVERED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_severed", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEIZED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_seized", DamageScaling.NEVER, 0.0f));
	}

	public static DamageSource spatialPressure(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPATIAL_PRESSURE));
	}

	public static DamageSource soulSevered(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SEVERED));
	}

	/** Seized by {@code soul}'s nascent soul. */
	public static DamageSource soulSeized(Level level, net.minecraft.world.entity.Entity soul) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SEIZED), soul);
	}

	private ModDamageTypes() {}
}
