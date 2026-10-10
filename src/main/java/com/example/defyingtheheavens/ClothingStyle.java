package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * The colours cultivator clothing is dyed in, one per path (see {@link Alignment}): the white of the righteous, the blue of
 * the neutral, the red and black of the demonic. Each piece's cloth is drawn in {@link #getCloth()} with its lapels, sash
 * and cuffs in {@link #getAccent()}; the embroidery takes its grade's colour ({@link ClothingTier#getTrimColor()}).
 */
public enum ClothingStyle {
	RIGHTEOUS("righteous", 0xF4F1EA, 0x9FB4C8),
	NEUTRAL("neutral", 0x3D6FC0, 0xE6EEF8),
	DEMONIC("demonic", 0x23191C, 0xB0202A);

	private final String id;
	private final int cloth;
	private final int accent;

	ClothingStyle(String id, int cloth, int accent) {
		this.id = id;
		this.cloth = cloth;
		this.accent = accent;
	}

	public String getId() { return id; }
	public int getCloth() { return cloth; }
	public int getAccent() { return accent; }
	public Component getDisplayName() { return Component.translatable(ModLang.clothingStyleKey(id)); }

	/** The colours of a cultivator's path. */
	public static ClothingStyle of(Alignment.Faction faction) {
		return switch (faction) {
			case RIGHTEOUS -> RIGHTEOUS;
			case NEUTRAL -> NEUTRAL;
			case DEMONIC -> DEMONIC;
		};
	}

	public static ClothingStyle byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : NEUTRAL;
	}
}
