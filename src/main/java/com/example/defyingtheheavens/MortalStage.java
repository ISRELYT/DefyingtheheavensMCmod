package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * A mortal's body tempering, before the meridians open: Low, Mid, High, Peak. Tempering comes from fighting, mining,
 * sprinting and eating (see {@link Tempering}); only at Peak is the body strong enough for a Marrow Cleansing Elixir.
 */
public enum MortalStage {
	LOW("low", 120),
	MID("mid", 200),
	HIGH("high", 300),
	PEAK("peak", 0);

	private final String id;
	private final double temperingToNext;

	MortalStage(String id, double temperingToNext) {
		this.id = id;
		this.temperingToNext = temperingToNext;
	}

	public String getId() { return id; }
	/** Tempering needed to reach the next stage (0 at Peak). */
	public double getTemperingToNext() { return temperingToNext; }
	/** Share of Qi Refining Early's body bonuses the tempered body has: none at Low, all of them at Peak. */
	public double bodyShare() { return ordinal() / (double) PEAK.ordinal(); }

	public String getTranslationKey() { return ModLang.mortalStageKey(id); }
	public Component getDisplayName() { return Component.translatable(getTranslationKey()); }

	public boolean isLast() { return this == PEAK; }
	public MortalStage next() { return isLast() ? this : values()[ordinal() + 1]; }

	public static MortalStage byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
