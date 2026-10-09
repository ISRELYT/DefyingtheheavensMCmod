package com.example.defyingtheheavens;

/** Balance knobs shared by attached fruit and harvested items. Time is server game time, not wall time. */
public final class FruitAge {
    public static final int MAX_YEARS = 10_000;
    public static final long TICKS_PER_YEAR = 2_400;
    public static final int CULTIVATION_PER_YEAR = 10;

    public static int clamp(int years) { return Math.max(1, Math.min(MAX_YEARS, years)); }

    public static int at(int initialYears, long plantedAt, long now) {
        long elapsed = now > plantedAt && plantedAt >= 0 ? now - plantedAt : 0;
        return (int) Math.min(MAX_YEARS, clamp(initialYears) + elapsed / TICKS_PER_YEAR);
    }

    public static int cultivation(int years) { return clamp(years) * CULTIVATION_PER_YEAR; }
    private FruitAge() {}
}
