package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The legendary weapons (phase 3 of the Artifact Forge, the user's picks, 2026-10-28: "some famous weapons ... unique immortal weapons that
 * some named cultivators have like tamed tribulation lightning"; the design they approved, every weapon in it). None is forged: each is one
 * of a kind in the world (Legends), rests somewhere or is carried by a named immortal, and grows with whoever holds it: its powers wake in
 * three stages as its wielder's realm rises ({@link #stage}), from the lowest realm that can wield it at all ({@link #lowest}).
 * <p>
 * Phase 3a: the paired swords Ganjiang (yang) and Moye (yin), and Heaven's Tribulation Spear. Phase 3b: the Xuanyuan Sword, the
 * Sun-Shooting Bow and the Sea-Fixing Needle. Phase 3c: Pangu's Axe, and the Celestial Court's five Venerables' weapons (the Four Symbols
 * and the Yellow Dragon), won in Dao Duels. Append-only: a legend's id names its item and its record.
 */
public enum Legend {
	//                  id                    lowest realm                 blow  swings  the world it rests in
	GANJIANG(          "ganjiang",          Realm.CORE_FORMATION,          11,   1.6,    Level.OVERWORLD),
	MOYE(              "moye",              Realm.CORE_FORMATION,          11,   1.6,    ModDimensions.NETHERWORLD),
	TRIBULATION_SPEAR( "tribulation_spear", Realm.TRUE_IMMORTAL,           14,   1.2,    ModDimensions.IMMORTAL_REALM),
	XUANYUAN(          "xuanyuan_sword",    Realm.NASCENT_SOUL,            12,   1.6,    ModDimensions.UPPER_REALM),
	SUN_BOW(           "sun_shooting_bow",  Realm.HEAVENLY_BEING,          6,    1.0,    ModDimensions.ASTRAL_SURFACES),
	SEA_NEEDLE(        "sea_fixing_needle", Realm.ENTERING_NIRVANA,        13,   1.0,    ModDimensions.IMMORTAL_REALM),
	// Phase 3c: Pangu's Axe, in the heart of the Primordial Chaos; the Celestial Court's five Venerables' weapons, one of each element.
	PANGU_AXE(         "pangu_axe",         Realm.EARTH_IMMORTAL,          18,   0.9,    ModDimensions.IMMORTAL_REALM),
	AZURE_GLAIVE(      "azure_dragon_glaive", Realm.TRUE_IMMORTAL,         15,   1.1,    ModDimensions.IMMORTAL_REALM),
	WHITE_TIGER_SABER( "white_tiger_saber", Realm.TRUE_IMMORTAL,           15,   1.5,    ModDimensions.IMMORTAL_REALM),
	VERMILION_FAN(     "vermilion_bird_fan", Realm.TRUE_IMMORTAL,          12,   1.6,    ModDimensions.IMMORTAL_REALM),
	TORTOISE_MACE(     "black_tortoise_mace", Realm.TRUE_IMMORTAL,         17,   0.9,    ModDimensions.IMMORTAL_REALM),
	YELLOW_DRAGON_SEAL("yellow_dragon_seal", Realm.TRUE_IMMORTAL,          16,   1.0,    ModDimensions.IMMORTAL_REALM);

	private final String id;
	private final Realm lowest;
	private final int blow;
	private final double speed;
	private final ResourceKey<Level> home;

	Legend(String id, Realm lowest, int blow, double speed, ResourceKey<Level> home) {
		this.id = id;
		this.lowest = lowest;
		this.blow = blow;
		this.speed = speed;
		this.home = home;
	}

	public String getId() { return id; }
	/** The lowest realm that can wield it: below it, it is too heavy to hold. */
	public Realm lowest() { return lowest; }
	/** Its blow at its first awakening, as its tooltip shows it; two more at each awakening after. */
	public int blow() { return blow; }
	/** Swings a second. */
	public double speed() { return speed; }
	/** The world its resting place is in. */
	public ResourceKey<Level> home() { return home; }

	public Component getDisplayName() { return Component.translatable(ModLang.legendName(id)); }

	/** Its other half (Ganjiang's Moye, Moye's Ganjiang), or null. */
	public Legend pair() {
		return this == GANJIANG ? MOYE : this == MOYE ? GANJIANG : null;
	}

	/** Whether it rests in a legend's stone (LegendStoneBlock) when it is at home: Ganjiang, Moye, the Xuanyuan Sword, the Sun-Shooting Bow. */
	public boolean restsInStone() {
		return this == GANJIANG || this == MOYE || this == XUANYUAN || this == SUN_BOW;
	}

	/** The element of a Venerable's weapon (one of the Celestial Court's five: its Venerable the one of that element), or null. */
	public QiElement venerable() {
		return switch (this) {
			case AZURE_GLAIVE -> QiElement.WOOD;
			case WHITE_TIGER_SABER -> QiElement.METAL;
			case VERMILION_FAN -> QiElement.FIRE;
			case TORTOISE_MACE -> QiElement.WATER;
			case YELLOW_DRAGON_SEAL -> QiElement.EARTH;
			default -> null;
		};
	}

	/** The Venerable's weapon of {@code element}, or null. */
	public static Legend ofVenerable(QiElement element) {
		for (Legend legend : values()) if (legend.venerable() == element && element != null) return legend;
		return null;
	}

	/** Its art, for a Venerable's weapon: its symbol's own Dao Integration spell cast through it (LegendaryWeaponItem#symbolArt), or null. */
	public Spell symbolSpell() {
		return switch (this) {
			case AZURE_GLAIVE -> Spell.AZURE_DRAGON;
			case WHITE_TIGER_SABER -> Spell.WHITE_TIGER_POUNCE;
			case VERMILION_FAN -> Spell.VERMILION_BIRD_DESCENT;
			case TORTOISE_MACE -> Spell.BLACK_TORTOISE_BASTION;
			case YELLOW_DRAGON_SEAL -> Spell.SEAL_OF_TEN_THOUSAND_MOUNTAINS;
			default -> null;
		};
	}

	/**
	 * How far it has woken in the hands of one at {@code realm}: none below its lowest realm; the first stage there and a realm on, the
	 * second two realms on, the third from four realms on (the design's "three stages as your realm rises").
	 */
	public int stage(Realm realm) {
		int on = realm.ordinal() - lowest.ordinal();
		return on < 0 ? 0 : on < 2 ? 1 : on < 4 ? 2 : 3;
	}

	public static Legend byId(String id) {
		for (Legend legend : values()) if (legend.id.equals(id)) return legend;
		return null;
	}
}
