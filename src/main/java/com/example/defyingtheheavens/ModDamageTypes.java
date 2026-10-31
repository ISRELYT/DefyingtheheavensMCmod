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
	/** A golden core detonated (CoreDetonation): its own cultivator's death, tagged as {@link #SOUL_SEVERED}, so nothing holds it off. */
	public static final ResourceKey<DamageType> CORE_DETONATION = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("core_detonation"));
	/** A lifespan run out (Lifespan): tagged as {@link #CORE_DETONATION}, so nothing holds it off. */
	public static final ResourceKey<DamageType> OLD_AGE = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("old_age"));
	/**
	 * The Netherworld's tribulation (TribulationManager, the ghost path's): its yin lightning, which armour turns as it turns the
	 * heavens' bolts, and the yin in it that goes through armour as the heavens' magic does (tagged bypasses_armor). Both reach the
	 * dead (GhostSystem#reaches), the tribulation's own (the heavens' lightning passes through them).
	 */
	public static final ResourceKey<DamageType> YIN_LIGHTNING = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("yin_lightning"));
	public static final ResourceKey<DamageType> YIN_TRIBULATION = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("yin_tribulation"));
	/**
	 * The heaven methods' spells (HeavenSpells): the five elements' qi, one harm each, "qi_wood" to "qi_metal". Tagged to pass
	 * armour (qi goes through steel), but Protection and Resistance still turn some of it. It reaches the dead as a living blow
	 * does: only from one who sees souls (GhostSystem#reaches).
	 */
	public static final ResourceKey<DamageType> QI_WOOD = qiKey(QiElement.WOOD), QI_WATER = qiKey(QiElement.WATER), QI_FIRE = qiKey(QiElement.FIRE),
			QI_EARTH = qiKey(QiElement.EARTH), QI_METAL = qiKey(QiElement.METAL);
	/**
	 * The spells of both paths (HeavenDemonSpells): heaven's light and a demon's qi as one. Tagged as the five elements' qi (through armour;
	 * Protection and Resistance still turn some of it), no element's ward reading it; the dead it reaches as a living blow does.
	 */
	public static final ResourceKey<DamageType> HEAVEN_DEMON = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("heaven_demon"));
	/**
	 * The Chaos Frontier's storm (ChaosFrontier, phase 3c): chaos from before heaven and earth, unmaking whatever stands unsheltered in it.
	 * Tagged as {@link #SPATIAL_PRESSURE}: armour, enchantments and effects don't turn it, nor any ward (HeavenPassives#untouchedByWards), it
	 * throws nothing about, and it reaches the dead as the living (GhostSystem#reaches).
	 */
	public static final ResourceKey<DamageType> CHAOS_STORM = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("chaos_storm"));
	/** The Sunken Sword Tomb's lingering sword intent, and its Sword Path's pressure (SwordTombWorks): the cuts of swords long fallen. */
	public static final ResourceKey<DamageType> SWORD_INTENT = ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("sword_intent"));

	private static ResourceKey<DamageType> qiKey(QiElement element) {
		return ResourceKey.create(Registries.DAMAGE_TYPE, DefyingTheHeavens.id("qi_" + element.name().toLowerCase(java.util.Locale.ROOT)));
	}

	/** The harm of {@code element}'s qi (one of the five). */
	public static ResourceKey<DamageType> qiType(QiElement element) {
		return switch (element) {
			case WOOD -> QI_WOOD;
			case WATER -> QI_WATER;
			case FIRE -> QI_FIRE;
			case EARTH -> QI_EARTH;
			default -> QI_METAL;
		};
	}

	public static void bootstrap(BootstapContext<DamageType> context) {
		context.register(SPATIAL_PRESSURE, new DamageType(DefyingTheHeavens.MOD_ID + ".spatial_pressure", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SCATTER, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_scatter", DamageScaling.NEVER, 0.0f));
		context.register(JUDGE_GAZE, new DamageType(DefyingTheHeavens.MOD_ID + ".judge_gaze", DamageScaling.NEVER, 0.0f));
		context.register(GHOSTLY, new DamageType(DefyingTheHeavens.MOD_ID + ".ghostly", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEVERED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_severed", DamageScaling.NEVER, 0.0f));
		context.register(SOUL_SEIZED, new DamageType(DefyingTheHeavens.MOD_ID + ".soul_seized", DamageScaling.NEVER, 0.0f));
		context.register(CORE_DETONATION, new DamageType(DefyingTheHeavens.MOD_ID + ".core_detonation", DamageScaling.NEVER, 0.0f));
		context.register(OLD_AGE, new DamageType(DefyingTheHeavens.MOD_ID + ".old_age", DamageScaling.NEVER, 0.0f));
		context.register(YIN_LIGHTNING, new DamageType(DefyingTheHeavens.MOD_ID + ".yin_lightning", DamageScaling.NEVER, 0.1f));
		context.register(YIN_TRIBULATION, new DamageType(DefyingTheHeavens.MOD_ID + ".yin_tribulation", DamageScaling.NEVER, 0.0f));
		for (QiElement element : QiElement.five()) {
			ResourceKey<DamageType> key = qiType(element);
			context.register(key, new DamageType(DefyingTheHeavens.MOD_ID + "." + key.location().getPath(), DamageScaling.NEVER, 0.1f));
		}
		context.register(HEAVEN_DEMON, new DamageType(DefyingTheHeavens.MOD_ID + ".heaven_demon", DamageScaling.NEVER, 0.1f));
		context.register(CHAOS_STORM, new DamageType(DefyingTheHeavens.MOD_ID + ".chaos_storm", DamageScaling.NEVER, 0.0f));
		context.register(SWORD_INTENT, new DamageType(DefyingTheHeavens.MOD_ID + ".sword_intent", DamageScaling.NEVER, 0.1f));
	}

	/** The Sunken Sword Tomb's lingering sword intent (no one's hand). */
	public static DamageSource swordIntent(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SWORD_INTENT));
	}

	/** Lingering sword intent left by {@code attacker}'s blow (WarArts: Sword Intent's second cut; null for none). */
	public static DamageSource swordIntent(Level level, net.minecraft.world.entity.Entity attacker) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SWORD_INTENT), attacker);
	}

	/** The Chaos Frontier's storm. */
	public static DamageSource chaosStorm(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CHAOS_STORM));
	}

	/** A spell of both paths' harm (HeavenDemonSpells), cast by {@code attacker} (null for none: a seal whose caster is gone). */
	public static DamageSource heavenDemon(Level level, net.minecraft.world.entity.Entity attacker) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HEAVEN_DEMON), attacker);
	}

	/** {@code element}'s qi, cast by {@code attacker} (null for none: a zone left burning, say). */
	public static DamageSource qi(Level level, QiElement element, net.minecraft.world.entity.Entity attacker) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(qiType(element)), attacker);
	}

	/**
	 * {@code element}'s qi from a spell of {@code realm} (QiDamageSource: a ward that overcomes the element reads it), cast by
	 * {@code attacker} (null for none), through {@code direct} (a thrown spell; null: the attacker's own hand).
	 */
	public static QiDamageSource qi(Level level, QiElement element, Realm realm, net.minecraft.world.entity.Entity direct,
			net.minecraft.world.entity.Entity attacker) {
		return new QiDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(qiType(element)),
				direct == null ? attacker : direct, attacker, element, realm);
	}

	public static DamageSource spatialPressure(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SPATIAL_PRESSURE));
	}

	/** The dead's harm, by {@code attacker} (null for none: ghost fire left burning, say). */
	public static DamageSource ghostly(Level level, net.minecraft.world.entity.Entity attacker) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GHOSTLY), attacker);
	}

	/** The dead's harm through a thing ({@code direct}: a thrown Ghost-Quelling Talisman) by its {@code owner}. */
	public static DamageSource ghostly(Level level, net.minecraft.world.entity.Entity direct, net.minecraft.world.entity.Entity owner) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(GHOSTLY), direct, owner);
	}

	public static DamageSource judgeGaze(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(JUDGE_GAZE));
	}

	public static DamageSource soulScatter(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SCATTER));
	}

	/** The Netherworld's tribulation's bolt (armour turns it). */
	public static DamageSource yinLightning(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(YIN_LIGHTNING));
	}

	/** The yin in the Netherworld's tribulation that goes through armour. */
	public static DamageSource yinTribulation(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(YIN_TRIBULATION));
	}

	/** One's own golden core, detonated. */
	public static DamageSource coreDetonation(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(CORE_DETONATION));
	}

	/** A lifespan run out. */
	public static DamageSource oldAge(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(OLD_AGE));
	}

	public static DamageSource soulSevered(Level level) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SEVERED));
	}

	/** Seized by {@code soul}'s nascent soul. */
	public static DamageSource soulSeized(Level level, net.minecraft.world.entity.Entity soul) {
		return new ModDamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SOUL_SEIZED), soul);
	}

	private ModDamageTypes() {}
}
