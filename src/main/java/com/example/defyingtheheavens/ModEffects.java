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
