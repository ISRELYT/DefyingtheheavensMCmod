package com.example.defyingtheheavens;

/**
 * What an element spell leaves on a being for a while (SpellAuras): a ward round the caster, a buff on their hands or weapon, a
 * fire or frost on a foe. Each is drawn on the being by every client that sees it (the client's SpellAuraRenderer).
 * <p>
 * A ward is of an element and a realm, and stands in the cycle of the five (QiElement#overcomes): it takes {@link #reduction} off
 * all harm, and {@link #overcomeReduction} off the harm of the element it overcomes in place of that (half as much if the harm is a
 * spell of a deeper realm than the ward's); harm of the element that overcomes it strikes {@link SpellAuras#PIERCE} times as hard.
 * Synced by ordinal: add new ones at the end.
 */
public enum SpellAura {
	FLAME_FIST,
	/** Fire and lava pass harmlessly; lava bears the body. A fire ward. */
	CRIMSON_TEMPERING(QiElement.FIRE, Realm.QI_REFINING, 0, 0.2),
	EMERALD_HEALING,
	HARDENED_BODY(QiElement.EARTH, Realm.QI_REFINING, 0.07, 0.14),
	/** Fire's harm and fire spells stop at it (lava boils it off). A water ward. */
	AZURE_COATING(QiElement.WATER, Realm.QI_REFINING, 0, 1.0),
	AZURE_BREATHING,
	BLADE_POLISH,
	STEEL_HURRICANE,
	/** -10% harm, -20% metal's (spells, and blows of metal weapons). A fire ward. */
	FLAME_ARMOUR(QiElement.FIRE, Realm.FOUNDATION_BUILDING, 0.10, 0.20),
	ANCIENT_BARK_SKIN(QiElement.WOOD, Realm.CORE_FORMATION, 0.20, 0.40),
	MOUNTAIN_STANCE(QiElement.EARTH, Realm.CORE_FORMATION, 0.25, 0.50),
	/** Takes the harm itself until it has taken its fill (SpellAuras#bell); wood's twice over. A metal ward. */
	GOLDEN_BELL(QiElement.METAL, Realm.CORE_FORMATION, 0, 0),
	VAJRA_BODY(QiElement.METAL, Realm.NASCENT_SOUL, 0.35, 0.70),
	/** On a foe: Karmic Fire burning away its sins. */
	KARMIC_FIRE,
	/** On a foe: frozen solid (Frozen Field, Absolute Zero). */
	FROZEN,
	/** On a foe: pressed to the ground (Heavy Earth Seal). */
	PRESSED,
	/** The phoenix's flame waiting to rise from a death (Phoenix Nirvana). */
	PHOENIX_NIRVANA,
	/** Life being drawn out of it (Life Force Drain) or poured into it (Life Force Gain). */
	LIFE_FLOW,
	/** Under the Black Tortoise's shell (Black Tortoise Bastion): -40% harm, fire's stopped outright. A water ward (its shape is the tortoise). */
	BLACK_TORTOISE(QiElement.WATER, Realm.DAO_INTEGRATION, 0.40, 1.0),
	/**
	 * Ghost Fire kindled in the hands (Spells#ghostFire), while its blows last: drawn as cold fire wreathing both hands (SpellAuraLayer, and
	 * in first person), the user's ask, 2026-10-25: "make ghost fire go around my hands and not just two random balls".
	 */
	GHOST_FIRE;

	private final QiElement ward;
	private final Realm wardRealm;
	private final double reduction;
	private final double overcomeReduction;

	SpellAura() {
		this(null, null, 0, 0);
	}

	SpellAura(QiElement ward, Realm wardRealm, double reduction, double overcomeReduction) {
		this.ward = ward;
		this.wardRealm = wardRealm;
		this.reduction = reduction;
		this.overcomeReduction = overcomeReduction;
	}

	/** The element it wards with, or null if it is no ward. */
	public QiElement ward() { return ward; }

	/** A ward's realm: against a spell of a deeper realm, what it takes off the element it overcomes is halved. */
	public Realm wardRealm() { return wardRealm; }

	/** The share it takes off all harm (0 for none). */
	public double reduction() { return reduction; }

	/** The share it takes off the harm of the element it overcomes, in place of {@link #reduction} (1: stopped outright). */
	public double overcomeReduction() { return overcomeReduction; }

	public static SpellAura byIndex(int index) {
		return index >= 0 && index < values().length ? values()[index] : null;
	}
}
