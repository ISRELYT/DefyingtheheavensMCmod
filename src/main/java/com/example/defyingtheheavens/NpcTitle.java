package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * What an NPC cultivator is called on their nameplate. Sect titles follow cultivation within the sect (see
 * {@link Sect#recalculateTitles}): the strongest member is always the Sect Master, then come the Grand Elders, Elders, Inner
 * and Outer Disciples. Cultivators belonging to no sect are Rogue Cultivators, or Wanderers if they never opened their
 * meridians.
 */
public enum NpcTitle {
	SECT_MASTER("sect_master", 0xFFD54F, true),
	GRAND_ELDER("grand_elder", 0xE1A95F, true),
	ELDER("elder", 0xCE93D8, true),
	INNER_DISCIPLE("inner_disciple", 0x80DEEA, true),
	OUTER_DISCIPLE("outer_disciple", 0xB0BEC5, true),
	ROGUE("rogue", 0xBCAAA4, false),
	WANDERER("wanderer", 0x9E9E9E, false);

	private final String id;
	private final int color;
	private final boolean sect;

	NpcTitle(String id, int color, boolean sect) {
		this.id = id;
		this.color = color;
		this.sect = sect;
	}

	public String getId() { return id; }
	public int getColor() { return color; }
	public boolean isSectTitle() { return sect; }

	/** Elders and above: they guard the sect's treasures and only fight when the disciples need them. */
	public boolean isElderOrAbove() { return this == SECT_MASTER || this == GRAND_ELDER || this == ELDER; }

	/** "Sect Master"; the nameplate puts it in brackets. */
	public Component getDisplayName() { return Component.translatable(ModLang.titleKey(id)); }

	public static NpcTitle byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : ROGUE;
	}
}
