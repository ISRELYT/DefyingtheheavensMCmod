package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Balance knobs shared by attached fruit and harvested items. Time is server game time, not wall time. */
public final class FruitAge {
    public static final int MAX_YEARS = 10_000;
    public static final long TICKS_PER_YEAR = 2_400;
    public static final int CULTIVATION_PER_YEAR = 10;

    /** Age tiers: each gives a fruit still on the tree a stronger aura (see FruitAura; 10,000 = MAX_YEARS has its own). */
    public static final int HUNDRED_YEAR_TIER = 100;
    public static final int FIVE_HUNDRED_YEAR_TIER = 500;
    public static final int THOUSAND_YEAR_TIER = 1_000;
    public static final int HEAVENLY_TIER = 5_000;

    /** Chance a wild fruit is born into each tier. The rest are a few decades old, centred on ~40 years. */
    public static final double NATURAL_HUNDRED_YEAR_CHANCE = 0.15;
    public static final double NATURAL_THOUSAND_YEAR_CHANCE = 0.026;
    public static final double NATURAL_HEAVENLY_CHANCE = 0.004;
    private static final int NATURAL_COMMON_MEAN = 40;
    private static final int NATURAL_COMMON_SPREAD = 15;

    public static int clamp(int years) { return Math.max(1, Math.min(MAX_YEARS, years)); }

    public static int at(int initialYears, long plantedAt, long now) {
        long elapsed = now > plantedAt && plantedAt >= 0 ? now - plantedAt : 0;
        return (int) Math.min(MAX_YEARS, clamp(initialYears) + elapsed / TICKS_PER_YEAR);
    }

    public static int cultivation(int years) { return clamp(years) * CULTIVATION_PER_YEAR; }

    /**
     * Age of a fruit found growing in the wild. Most are a few decades old; each older tier is far rarer than the one
     * before, and within a tier the younger end is more common, so a 10,000-year fruit is the rarest of all.
     */
    public static int natural(RandomSource random) {
        double roll = random.nextDouble();
        if (roll < NATURAL_HEAVENLY_CHANCE) return skewedYoung(random, HEAVENLY_TIER, MAX_YEARS);
        roll -= NATURAL_HEAVENLY_CHANCE;
        if (roll < NATURAL_THOUSAND_YEAR_CHANCE) return skewedYoung(random, THOUSAND_YEAR_TIER, HEAVENLY_TIER - 1);
        roll -= NATURAL_THOUSAND_YEAR_CHANCE;
        if (roll < NATURAL_HUNDRED_YEAR_CHANCE) return skewedYoung(random, HUNDRED_YEAR_TIER, THOUSAND_YEAR_TIER - 1);
        int common = (int) Math.round(NATURAL_COMMON_MEAN + random.nextGaussian() * NATURAL_COMMON_SPREAD);
        return Mth.clamp(common, 10, HUNDRED_YEAR_TIER - 1);
    }

    /** A value in [min, max], weighted toward min. */
    private static int skewedYoung(RandomSource random, int min, int max) {
        float t = random.nextFloat();
        return min + Math.round((max - min) * t * t);
    }

    private FruitAge() {}
}
