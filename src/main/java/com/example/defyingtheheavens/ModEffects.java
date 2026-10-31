package com.example.defyingtheheavens;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	/**
	 * A Qi Gathering Pill at work. The effect itself does nothing: it shows the pill's timer, and while it lasts the pill's
	 * boost (kept in PlayerCultivation, since it varies with the pill) raises qi gathering. Milk or death ends both.
	 */
	public static final MobEffect QI_GATHERING = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("qi_gathering"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x4AAEEA));

	/**
	 * Insight: a senior's guidance (TalkSystem, or losing a spar to one who likes you) opens the way for a while, and meditation
	 * gathers {@link #INSIGHT_BONUS} more cultivation (MeditationManager). Once a day from each teacher.
	 */
	public static final MobEffect INSIGHT = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("insight"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF2C94C));
	public static final double INSIGHT_BONUS = 0.25;
	public static final int INSIGHT_TICKS = 10 * 60 * 20;

	/**
	 * The Spring of the Ancients drunk (SpringOfTheAncientsItem, found only in the secret realms): for a day ({@link #SPRING_TICKS}: an in-game
	 * day) meditation gathers {@link #SPRING_BONUS} more cultivation (MeditationManager), on top of Insight.
	 */
	public static final MobEffect ANCIENT_SPRING = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("ancient_spring"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x7FE3C4));
	public static final double SPRING_BONUS = 0.5;
	public static final int SPRING_TICKS = 24_000;

	/**
	 * The master's lecture heard (the Cave Abode's event, CaveAbodeWorks#lecture): kept on whoever is in its lecture hall while the master's
	 * voice lasts; meditation there gathers {@link #LECTURE_BONUS} more cultivation (MeditationManager), five times as much.
	 */
	public static final MobEffect MASTERS_LECTURE = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("masters_lecture"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xE8D49A));
	public static final double LECTURE_BONUS = 4.0;
	/** The Jade Lake Mirror raised (JadeLakeMirrorItem): its amplifier the blows it may still turn back, less one. */
	public static final MobEffect MIRROR_RAISED = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("mirror_raised"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xBFF4E8));
	/**
	 * The Five Decays spreading through the Tomb of a Fallen Immortal (ImmortalTombWorks): one's blows weaker (FiveDecays#outgoing, as their
	 * signs weaken an immortal's) and one's armour too.
	 */
	public static final MobEffect DECAYING = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("decaying"),
			new MarkerEffect(MobEffectCategory.HARMFUL, 0x8A7A4A).addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
					"5f1d0a0e-7031-4dec-a7e0-0c0a1d0e7031", -(1 - FiveDecays.ARMOUR_KEPT), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));

	/** Ghost fire: cold cyan fire from the Ghost Fire Scripture (see GhostFireEffect, Spells). */
	public static final MobEffect GHOST_FIRE = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("ghost_fire"),
			new GhostFireEffect());

	/**
	 * A Yin Gathering Pill at work: meditation cultivates the ghost method faster by the pill's boost (kept in NetherworldState,
	 * like the Qi Gathering Pill's) while it lasts (MeditationManager).
	 */
	public static final MobEffect YIN_GATHERING = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("yin_gathering"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x4FC3D9));
	/** A Yang Preservation Pill at work: the Netherworld's yin is held still in the body while it lasts (YinCorruption). */
	public static final MobEffect YANG_PRESERVATION = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("yang_preservation"),
			new MarkerEffect(MobEffectCategory.BENEFICIAL, 0xF5B642));
	/** One of the dead bound by a Ghost-Quelling Talisman, held where it stands (see QuelledEffect). */
	public static final MobEffect QUELLED = Registry.register(BuiltInRegistries.MOB_EFFECT, DefyingTheHeavens.id("quelled"),
			new QuelledEffect());

	private static final class MarkerEffect extends MobEffect {
		MarkerEffect(MobEffectCategory category, int color) {
			super(category, color);
		}
	}

	/** Touching this class registers the effects. */
	public static void register() {
	}

	private ModEffects() {}
}
