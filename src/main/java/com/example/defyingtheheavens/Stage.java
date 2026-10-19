package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Minor stages inside every realm. The cultivation multipliers are set so every stage takes longer than the one before, on both
 * paths and across realms (the user's ask, 2026-10-19): with the gain's 1.0 / 1.1 / 1.2 / 1.3 a stage's time goes x1.3 / x1.5 /
 * x1.67 / x1.88 of its realm's unit ({@link #timeWeight}), and the next realm's unit is 1.6 times this one's (eight times the
 * cultivation, five times the gain: Realm), so its Early (x2.08) is longer than this Grand Perfection. Before, 1.0 / 1.5 / 2.0 /
 * 3.0, and a new realm's Early was quicker than the Grand Perfection before it. The realms take as long in all as they did.
 */
public enum Stage {
	EARLY("early", 1.3),
	MID("mid", 1.65),
	LATE("late", 2.0),
	GRAND_PERFECTION("grand_perfection", 2.45);

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

	/**
	 * How long the stage takes against its realm's unit: the cultivation it needs over the gain it is cultivated at (1.3 / 1.5 /
	 * 1.67 / 1.88). The ghost path's stages go by it too (GhostMethods#required), so both paths step up alike.
	 */
	public double timeWeight() { return cultivationMultiplier / getPowerMultiplier(); }

	public String getTranslationKey() { return ModLang.stageKey(id); }
	public Component getDisplayName() { return Component.translatable(getTranslationKey()); }

	public boolean isLast() { return ordinal() == values().length - 1; }
	public Stage next() { return isLast() ? this : values()[ordinal() + 1]; }
	public Stage previous() { return ordinal() == 0 ? this : values()[ordinal() - 1]; }

	public static Stage byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
