package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/**
 * How well a pill came out of the cauldron. The grade is rolled when the pill is brewed; older ingredients make the higher
 * grades likelier. A pill's strength (its {@link #potency}) is its grade's multiplier times a bonus for the age of the
 * ingredients that went into it, so a Low-Grade pill of thousand-year ginseng still beats one of fresh roots.
 */
public enum PillGrade {
	LOW("low", ChatFormatting.WHITE, 1.0),
	MID("mid", ChatFormatting.GREEN, 1.5),
	HIGH("high", ChatFormatting.AQUA, 2.0),
	SUPREME("supreme", ChatFormatting.LIGHT_PURPLE, 3.0),
	IMMORTAL("immortal", ChatFormatting.GOLD, 5.0);

	/**
	 * Chance (in %) of each grade, Low to Immortal, for ingredients averaging at least {@link #AGE_FROM} years. Each row adds
	 * up to 100. Immortal needs ingredients of a thousand years or more, and is still a lucky roll.
	 */
	private static final int[][] ODDS = {
			{85, 15, 0, 0, 0},   // under 10 years
			{60, 32, 8, 0, 0},   // 10+
			{30, 45, 22, 3, 0},  // 100+
			{15, 40, 35, 10, 0}, // 500+
			{5, 25, 45, 24, 1},  // 1,000+
			{0, 12, 43, 40, 5},  // 5,000+
			{0, 5, 35, 45, 15},  // 10,000 (the oldest anything grows)
	};
	private static final int[] AGE_FROM = {1, 10, 100, 500, 1000, 5000, FruitAge.MAX_YEARS};

	private final String id;
	private final ChatFormatting color;
	private final double multiplier;

	PillGrade(String id, ChatFormatting color, double multiplier) {
		this.id = id;
		this.color = color;
		this.multiplier = multiplier;
	}

	public String getId() { return id; }
	public ChatFormatting getColor() { return color; }
	public double getMultiplier() { return multiplier; }

	/** "Mid-Grade Foundation Pill", in the grade's colour. */
	public MutableComponent name(Component base) {
		return Component.translatable(ModLang.pillGradeKey(id), base).withStyle(color);
	}

	/** Grows with the ingredients' age: x1 for fresh ones, x2 at 10,000 years (x1.25 per tenfold). */
	public static double ageFactor(int years) {
		return 1 + Math.log10(FruitAge.clamp(years)) / 4;
	}

	/** How strong a pill of this grade is, brewed from ingredients averaging {@code years}: from 1 (Low, fresh) to 10. */
	public double potency(int years) {
		return multiplier * ageFactor(years);
	}

	/** The chance (in %) of each grade for ingredients averaging {@code years}. */
	public static int[] odds(int years) {
		int row = 0;
		for (int i = 0; i < AGE_FROM.length; i++) {
			if (years >= AGE_FROM[i]) row = i;
		}
		return ODDS[row];
	}

	/** Rolls the grade a brew comes out at. */
	public static PillGrade roll(int years, RandomSource random) {
		int[] odds = odds(years);
		int pick = random.nextInt(100);
		for (int i = 0; i < odds.length; i++) {
			pick -= odds[i];
			if (pick < 0) return values()[i];
		}
		return LOW;
	}

	public static PillGrade byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : LOW;
	}
}
