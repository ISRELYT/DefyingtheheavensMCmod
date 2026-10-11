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
