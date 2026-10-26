package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * What an NPC cultivator is called on their nameplate. Sect titles follow cultivation within the sect (see
 * {@link Sect#recalculateTitles}): the strongest member is always the Sect Master, then come the Grand Elders, Elders, Inner
 * and Outer Disciples. Cultivators belonging to no sect are Rogue Cultivators, or Wanderers if they never opened their
 * meridians. The Netherworld's ghost cultivators are Wandering Ghosts, Ghost Elders from Foundation Building, and Ghost Kings
 * from Core Formation, ruling their palaces with their Ghost Retainers; Ox-Head and Horse-Face are its Gate Wardens, Meng Po
 * the Keeper of the Naihe Bridge.
 */
public enum NpcTitle {
	SECT_MASTER("sect_master", 0xFFD54F, true),
	GRAND_ELDER("grand_elder", 0xE1A95F, true),
	ELDER("elder", 0xCE93D8, true),
	INNER_DISCIPLE("inner_disciple", 0x80DEEA, true),
	OUTER_DISCIPLE("outer_disciple", 0xB0BEC5, true),
	ROGUE("rogue", 0xBCAAA4, false),
	WANDERER("wanderer", 0x9E9E9E, false),
	// The Netherworld's dead (GhostCultivatorNpc): appended, as titles are saved by ordinal.
	WANDERING_GHOST("wandering_ghost", 0x8AF4F0, false),
	GHOST_ELDER("ghost_elder", 0x4FC3D9, false),
	GATE_WARDEN("gate_warden", 0xE57373, false),
	BRIDGE_KEEPER("bridge_keeper", 0xB39DDB, false),
	// The Netherworld's Ghost Kings (Core Formation and above) and the retainers of their courts: appended.
	GHOST_KING("ghost_king", 0xB388FF, false),
	GHOST_RETAINER("ghost_retainer", 0x80CBC4, false),
	// The Great Void's (2026-10-25): one who keeps a Sacred Vessel's altar (SacredRelics). Appended.
	RELIC_KEEPER("relic_keeper", 0xE8C860, false),
	// The Immortal Realm's (phase 2): the Celestial Court's officers, who guard the Southern Heavenly Gate (ImmortalGate). Appended.
	IMMORTAL_OFFICER("immortal_officer", 0xF5D47A, false),
	// The Immortal Realm's: one who keeps a stall in its cities' markets (CityMarkets). Appended.
	IMMORTAL_MERCHANT("immortal_merchant", 0x9CF0C8, false),
	// The Celestial Court's (CelestialCourt): the Heavenly King on his throne, the five Heavenly Venerables in their halls, the Immortal
	// Generals at the Court's gate, its Quartermaster. Appended.
	HEAVENLY_KING("heavenly_king", 0xFFE57F, false),
	HEAVENLY_VENERABLE("heavenly_venerable", 0xFFC46B, false),
	IMMORTAL_GENERAL("immortal_general", 0xFF8A65, false),
	COURT_QUARTERMASTER("court_quartermaster", 0xA5D6A7, false);

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

	/** One of the Celestial Court's own, who keep their posts at the Court (CelestialCourt). */
	public boolean isCourt() { return this == HEAVENLY_KING || this == HEAVENLY_VENERABLE || this == IMMORTAL_GENERAL || this == COURT_QUARTERMASTER; }

	/** "Sect Master"; the nameplate puts it in brackets. */
	public Component getDisplayName() { return Component.translatable(ModLang.titleKey(id)); }

	/** As {@link #getDisplayName}, a demon clan's master its "Clan Head" (the user's design, 2026-10-23); the other titles a sect's. */
	public Component getDisplayName(boolean clan) {
		return clan && this == SECT_MASTER ? Component.translatable(ModLang.titleKey("clan_head")) : getDisplayName();
	}

	/**
	 * As {@link #getDisplayName(boolean)}, and in the Great Void ({@code holy}) a Holy Land's master (HolyLands) its "Patriarch", a Demon
	 * Palace's (a clan there, DemonPalaces) its "Demon Sovereign".
	 */
	public Component getDisplayName(boolean clan, boolean holy) {
		if (holy && this == SECT_MASTER) return Component.translatable(ModLang.titleKey(clan ? "demon_sovereign" : "patriarch"));
		return getDisplayName(clan);
	}

	/** As {@link #getDisplayName(boolean, boolean)}, and in the Immortal Realm ({@code immortal}) an immortal sect's master its "Sect Lord". */
	public Component getDisplayName(boolean clan, boolean holy, boolean immortal) {
		if (immortal && this == SECT_MASTER && !clan) return Component.translatable(ModLang.titleKey("sect_lord"));
		return getDisplayName(clan, holy);
	}

	public static NpcTitle byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : ROGUE;
	}
}
