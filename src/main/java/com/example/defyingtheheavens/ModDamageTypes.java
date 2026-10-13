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
	/**
	 * A soul scattering on the Soul River (see SoulRiver#scatter): tagged as {@link #SPATIAL_PRESSURE}. It never takes the last of
	 * a body's health, so its death message is only there for completeness.
	 */
	public static final ResourceKey<DamageType> SOUL_SCATTER = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("soul_scatter"));
	/**
	 * The dead's harm (see GhostSystem): a ghost's blow, ghost fire, a Hungry Ghost's claws. Tagged as {@link #SPATIAL_PRESSURE}:
	 * armour, enchantments and effects don't turn it, and it throws nothing back.
	 */
	public static final ResourceKey<DamageType> GHOSTLY = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("ghostly"));
	/** The Chief Judge of the Netherworld's gaze (see JudgeEye): tagged as {@link #SOUL_SEVERED}, so nothing holds it off. */
	public static final ResourceKey<DamageType> JUDGE_GAZE = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("judge_gaze"));

	public static void bootstrap(BootstapContext<DamageType> context) {
		context.register(SPATIAL_PRESSURE, new DamageType(DefyingTheHeavens.MOD_ID + ".spatial_pressure", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SCATTER, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_scatter", DamageScaling.NEVER, 0.0f));
		context.register(JUDGE_GAZE, new DamageType(DefyingTheHeavens.MOD_ID + ".judge_gaze", DamageScaling.NEVER, 0.0f));
		context.register(GHOSTLY, new DamageType(DefyingTheHeavens.MOD_ID + ".ghostly", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEVERED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_severed", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEIZED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_seized", DamageScaling.NEVER, 0.0f));
	}

	public static DamageSource spatialPressure(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPATIAL_PRESSURE));
	}

	/** The dead's harm, by {@code attacker} (null for none: ghost fire left burning, say). */
	public static DamageSource ghostly(Level level, net.minecraft.world.entity.Entity attacker) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GHOSTLY), attacker);
	}

	/** The dead's harm through a thing ({@code direct}: a thrown Ghost-Quelling Talisman) by its {@code owner}. */
	public static DamageSource ghostly(Level level, net.minecraft.world.entity.Entity direct, net.minecraft.world.entity.Entity owner) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GHOSTLY), direct, owner);
	}

	public static DamageSource judgeGaze(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(JUDGE_GAZE));
	}

	public static DamageSource soulScatter(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SCATTER));
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
