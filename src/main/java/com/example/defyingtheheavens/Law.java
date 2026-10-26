package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * The Laws (phase 3, 2026-10-26: the user's pick, "Five + 3 greater Laws"): what the realms past True Immortal are built on, as the
 * methods carry those below it. The five elements' Laws, each strengthening its own element's spells, and the three greater Laws, Space,
 * Time and Life and Death, comprehended only from Golden Immortal on (Laws#GREATER_FROM). Each has {@link Laws#MAX_LEVEL} levels
 * (LawState). Add new Laws at the end: their ordinals are synced (saves use their ids).
 */
public enum Law {
	METAL("metal", QiElement.METAL),
	WOOD("wood", QiElement.WOOD),
	WATER("water", QiElement.WATER),
	FIRE("fire", QiElement.FIRE),
	EARTH("earth", QiElement.EARTH),
	SPACE("space", null),
	TIME("time", null),
	LIFE_AND_DEATH("life_and_death", null);

	private final String id;
	private final QiElement element;

	Law(String id, QiElement element) {
		this.id = id;
		this.element = element;
	}

	public String getId() { return id; }

	/** The element whose spells it strengthens, or null for a greater Law. */
	public QiElement element() { return element; }

	/** One of the three greater Laws (Space, Time, Life and Death), not an element's. */
	public boolean isGreater() { return element == null; }

	public Component getDisplayName() { return Component.translatable(ModLang.lawKey(id)); }

	/** What its levels give, in a line (the Methods tab's Laws page). */
	public Component getGains() { return Component.translatable(ModLang.lawKey(id) + ".gains"); }

	/** The colour it is drawn in: its element's, or the greater Laws' own. */
	public int color() {
		if (element != null) return element == QiElement.METAL ? 0xE8C060 : element.getCoreColor();
		return switch (this) {
			case SPACE -> 0xB388FF;
			case TIME -> 0x80DEEA;
			default -> 0xE0E0E0; // Life and Death: the white of bone, the black beside it
		};
	}

	/** The element's own Law (null for yin, or none). */
	public static Law of(QiElement element) {
		if (element == null) return null;
		for (Law law : values()) if (law.element == element) return law;
		return null;
	}

	public static Law byId(String id) {
		for (Law law : values()) if (law.id.equals(id)) return law;
		return null;
	}

	public static Law byIndex(int index) {
		return index < 0 || index >= values().length ? null : values()[index];
	}
}
