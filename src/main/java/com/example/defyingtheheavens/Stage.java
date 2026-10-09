package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/** Minor stages inside every realm. */
public enum Stage {
	EARLY("early", 1.0),
	MID("mid", 1.5),
	LATE("late", 2.0),
	GRAND_PERFECTION("grand_perfection", 3.0);

	private final String id;
	private final double cultivationMultiplier;

	Stage(String id, double cultivationMultiplier) {
		this.id = id;
		this.cultivationMultiplier = cultivationMultiplier;
	}

	public String getId() { return id; }
	/** Multiplies the realm's base cultivation requirement. */
	public double getCultivationMultiplier() { return cultivationMultiplier; }
	/** Multiplies the realm's stat bonuses, cultivation gain, max qi and qi gathering: 1.0 / 1.1 / 1.2 / 1.3. */
	public double getPowerMultiplier() { return 1.0 + 0.1 * ordinal(); }

	public String getTranslationKey() { return ModLang.stageKey(id); }
	public Component getDisplayName() { return Component.translatable(getTranslationKey()); }

	public boolean isLast() { return ordinal() == values().length - 1; }
	public Stage next() { return isLast() ? this : values()[ordinal() + 1]; }
	public Stage previous() { return ordinal() == 0 ? this : values()[ordinal() - 1]; }

	public static Stage byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
