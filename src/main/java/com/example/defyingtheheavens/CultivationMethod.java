package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * The methods of cultivation (the user's overhaul, 2026-10-31: "you need a method to cultivate at all"): HOW a path gathers, apart from
 * the arts its scriptures, foundations and blood teach. A method comes in volumes, one a realm, across its own realms ({@link #first} to
 * {@link #last}, never Four Axis), and a path gathers only in a realm whose volume it holds (CultivationMethods#hasVolume). Each path
 * cultivates by one at a time (CultivationMethods#practised): its orthodox method ({@link #orthodox}), whose volumes are in what the path is
 * taught (the heaven scriptures, the ghost foundations, the blood), or another, learned from a Method Manual (MethodManualItem). Switching
 * costs the path its stages in its realm, never the realm (CultivationMethods#practise). At Four Axis the four axes are everyone's method
 * ({@link #FOUR_AXES}, FourAxis#fillBars).
 * <p>
 * <b>Talent.</b> Talent divides the years a gain costs (Lifespan#seclusionYears), and a method's {@link Weight} says how much of it does:
 * the whole (the orthodox ways and Turtle Breath: the geniuses' road), its square root, or none; and every method costs its own share of
 * those years ({@link #yearsFactor}). The unorthodox methods so carry a low talent a realm or two past where it would stop, each at a
 * price of its own (phase 2: the user's "yes, at a price"). Set by reach tables (scratchpad reach.ps1 of session d057ee0b), the user's lines
 * of 2026-10-27 kept (Lifespan): Qi Circulation's are untouched, and a gathering formation only speeds it, so a Low talent with everything
 * still ends in Core Formation's Grand Perfection and a red mat in the Inner Realm still takes a High talent to Void Shattering, a
 * Supreme to Sacred Vessel, an Immortal to True Immortal. Then, with Longevity Pills' 400 years:
 * <ul>
 *   <li>the rites (a sixteenth, no talent) take a Low or Mid talent into Heavenly Being's Grand Perfection;</li>
 *   <li>the half-weight ways of their own (a twelfth: corpses, kin blood, cores, the five elements' offerings) a Low talent to the end of Nascent Soul, a Mid one into
 *   Dao Integration, a High one into Sacred Vessel, a Supreme into Entering Nirvana (each no further than its own realms);</li>
 *   <li>a territory (a third, at half again the pace, the aids counting) carries a Low demon through Core Formation, and on to Heavenly
 *   Being by the blood's other ways;</li>
 *   <li>Turtle Breath (a third, no aids but denser qi) a Mid talent into Core Formation's Grand Perfection, a High one to Sacred Vessel, a
 *   Supreme into Entering Nirvana, a Low one no further than the aids would;</li>
 *   <li>Thunder Tempering (a twelfth, half weight) takes its Heavenly Thunder Body (an Immortal grade) to True Immortal, as a red mat would,
 *   at twice the pace or more in a storm.</li>
 * </ul>
 * Qi Circulation with everything is still the furthest road for every talent but the lowest. The ways of phase 4, most of one realm alone,
 * cost the years of their kind (a deed of no talent a sixteenth; a half-weight way of its own a twelfth; a place's meditation a quarter;
 * Immortal Essence Refining, the geniuses' way at True Immortal, a half): a realm's way for those whose talent would stop short of it.
 * <p>
 * <b>How it gathers</b> ({@link #meditates}): by meditation (Circulation, Breathing, Purification, Turtle Breath, Territorial Dominion, the
 * places' ways, the Soul Furnace), or in its own way, meditation gathering nothing on its path while it is practised (the rites, lightning,
 * corpses, blood and cores, the five elements' offerings, herbs eaten, a clone's seclusion, a body borrowed, the heart demon, rifts,
 * vessels, the Shore of Nirvana, the springs, the Wheel, ghosts slain; the Body Path's bath, beasts' blood, weights and spars).
 */
public enum CultivationMethod {
	QI_CIRCULATION("qi_circulation", TwoPaths.Path.HEAVEN, Realm.QI_REFINING, Realm.TRUE_IMMORTAL, Weight.FULL, 1.0, true),
	/** At a Heaven Altar, at the right hour, a quarter of the stage a rite (HeavenRites). Every path's leave is the first volume. */
	HEAVEN_RITES("heaven_rites", TwoPaths.Path.HEAVEN, Realm.QI_REFINING, Realm.HEAVENLY_BEING, Weight.NONE, 1 / 16.0, false),
	/** Deep seclusion only, half the pace, a third of the years and no aid's saving (MeditationManager; TurtleBreath). */
	TURTLE_BREATH("turtle_breath", TwoPaths.Path.HEAVEN, Realm.FOUNDATION_BUILDING, Realm.SACRED_VESSEL, Weight.FULL, 1 / 3.0, true),
	/** The Heavenly Thunder Body's alone, known from birth: lightning called down on oneself (ThunderTempering). */
	THUNDER_TEMPERING("thunder_tempering", TwoPaths.Path.HEAVEN, Realm.CORE_FORMATION, Realm.TRUE_IMMORTAL, Weight.HALF, 1 / 12.0, false),
	NETHERWORLD_BREATHING("netherworld_breathing", TwoPaths.Path.GHOST, Realm.QI_REFINING, Realm.TRUE_IMMORTAL, Weight.FULL, 1.0, true),
	/** A corpse's yin drawn out, once a corpse (CorpseYin). */
	CORPSE_YIN("corpse_yin", TwoPaths.Path.GHOST, Realm.QI_REFINING, Realm.HEAVENLY_BEING, Weight.HALF, 1 / 12.0, false),
	BLOODLINE_PURIFICATION("bloodline_purification", TwoPaths.Path.DEMON, Realm.QI_REFINING, Realm.TRUE_IMMORTAL, Weight.FULL, 1.0, true),
	/** One's own race's blood drunk, blood held already too (DemonBlood#drink). */
	KIN_BLOOD("kin_blood", TwoPaths.Path.DEMON, Realm.QI_REFINING, Realm.SACRED_VESSEL, Weight.HALF, 1 / 12.0, false),
	/** Any beast's core eaten (DemonCoreItem); a foreign element's taints the blood. */
	CORE_DEVOURING("core_devouring", TwoPaths.Path.DEMON, Realm.FOUNDATION_BUILDING, Realm.ENTERING_NIRVANA, Weight.HALF, 1 / 12.0, false),
	/** A territory marked with one's blood, meditated in (Territories). */
	TERRITORIAL_DOMINION("territorial_dominion", TwoPaths.Path.DEMON, Realm.QI_REFINING, Realm.CORE_FORMATION, Weight.HALF, 1 / 3.0, true),
	// --- Phase 3 (the shrines and the methods of others) ---
	/** The five elements taken in as offerings, in the order each gives rise to the next, every cycle closed a gain (FiveElementsCycling). */
	FIVE_ELEMENTS("five_elements", TwoPaths.Path.HEAVEN, Realm.QI_REFINING, Realm.ENTERING_NIRVANA, Weight.HALF, 1 / 12.0, false),
	/** Shrines of oneself, prayed at by villagers, sect members and players (Shrines: a statue). */
	INCENSE_FAITH("incense_faith", TwoPaths.Path.HEAVEN, Realm.FOUNDATION_BUILDING, Realm.TRUE_IMMORTAL, Weight.NONE, 1 / 16.0, false),
	/** A wild demon beast subdued and bound, grown with on cores and in battle (SpiritBonds). */
	SPIRIT_BEAST_BOND("spirit_beast_bond", TwoPaths.Path.HEAVEN, Realm.CORE_FORMATION, Realm.TRUE_IMMORTAL, Weight.HALF, 1 / 12.0, false),
	/** The shrines as the dead's ancestral tablets, the living's incense burned to them (Shrines: a tablet). */
	ANCESTRAL_INCENSE("ancestral_incense", TwoPaths.Path.GHOST, Realm.QI_REFINING, Realm.TRUE_IMMORTAL, Weight.NONE, 1 / 16.0, false),
	/** One of the dead haunting the living's villages by night, fed on their fear (Haunting). */
	HAUNTING("haunting", TwoPaths.Path.GHOST, Realm.QI_REFINING, Realm.NASCENT_SOUL, Weight.NONE, 1 / 16.0, false),
	/** Souls seized as they drift past on the Soul River (SoulRiverFeast). */
	SOUL_RIVER_FEAST("soul_river_feast", TwoPaths.Path.GHOST, Realm.FOUNDATION_BUILDING, Realm.SACRED_VESSEL, Weight.NONE, 1 / 16.0, false),
	/** A Ghost King's palace held, its court's tribute taken (CourtOfTheDead). */
	COURT_OF_THE_DEAD("court_of_the_dead", TwoPaths.Path.GHOST, Realm.CORE_FORMATION, Realm.TRUE_IMMORTAL, Weight.HALF, 1 / 12.0, false),
	/** Mortals' worship of a demon as their mountain god (Shrines: an idol). */
	BEAST_DEITY("beast_deity", TwoPaths.Path.DEMON, Realm.CORE_FORMATION, Realm.TRUE_IMMORTAL, Weight.NONE, 1 / 16.0, false),
	// --- Phase 4 (the ways of one realm, and of a few) ---
	/** The heart demon called up again and again in the Inner Realm, each one bested a gain, each defeat a loss (HeartDemonTempering). */
	HEART_DEMON_TEMPERING("heart_demon_tempering", TwoPaths.Path.HEAVEN, Realm.NASCENT_SOUL, Realm.NASCENT_SOUL, Weight.NONE, 1 / 16.0, false),
	/** Meditation in the Great Void's Dao field of the stage alone, twice as fast there (MethodGrounds). */
	DAO_FIELD_ATTUNEMENT("dao_field_attunement", TwoPaths.Path.HEAVEN, Realm.DAO_INTEGRATION, Realm.DAO_INTEGRATION, Weight.HALF, 1 / 4.0, true),
	/** Space walked through: a Void Breach climbed, a gap storm's bolt borne (RiftWalking). */
	RIFT_WALKING("rift_walking", TwoPaths.Path.HEAVEN, Realm.VOID_SHATTERING, Realm.VOID_SHATTERING, Weight.HALF, 1 / 12.0, false),
	/** Qi poured into a Sacred Vessel and drawn back refined (VesselNurturing). */
	VESSEL_NURTURING("vessel_nurturing", TwoPaths.Path.HEAVEN, Realm.SACRED_VESSEL, Realm.SACRED_VESSEL, Weight.HALF, 1 / 12.0, false),
	/** Struck down on the Shore of Nirvana, risen again from the ashes, or not (NirvanaRebirth). */
	NIRVANA_REBIRTH("nirvana_rebirth", TwoPaths.Path.HEAVEN, Realm.ENTERING_NIRVANA, Realm.ENTERING_NIRVANA, Weight.NONE, 1 / 16.0, false),
	/** Meditation in the Immortal Realm burning immortal essence, twice as fast while it lasts (MethodGrounds). */
	IMMORTAL_ESSENCE_REFINING("immortal_essence_refining", TwoPaths.Path.HEAVEN, Realm.TRUE_IMMORTAL, Realm.TRUE_IMMORTAL, Weight.FULL, 0.5, true),
	/** Soaking in the Yellow Springs' waters (YellowSpringsImmersion). */
	YELLOW_SPRINGS_IMMERSION("yellow_springs_immersion", TwoPaths.Path.GHOST, Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, Weight.HALF, 1 / 4.0, false),
	/** Wandering souls escorted to the Wheel of Reincarnation (JudgementHallService). */
	JUDGEMENT_HALL_SERVICE("judgement_hall_service", TwoPaths.Path.GHOST, Realm.HEAVENLY_BEING, Realm.DAO_INTEGRATION, Weight.HALF, 1 / 12.0, false),
	/** Souls seized as they slip out through space's rifts (RiftHaunting). */
	RIFT_HAUNTING("rift_haunting", TwoPaths.Path.GHOST, Realm.VOID_SHATTERING, Realm.VOID_SHATTERING, Weight.NONE, 1 / 16.0, false),
	/** Meditation in one's own ghost fire, the gain growing as the burning does (SoulFurnace). */
	SOUL_FURNACE("soul_furnace", TwoPaths.Path.GHOST, Realm.ENTERING_NIRVANA, Realm.ENTERING_NIRVANA, Weight.HALF, 1 / 12.0, true),
	/** Ghosts slain by one's own hand taken in (TenThousandGhosts). */
	TEN_THOUSAND_GHOSTS("ten_thousand_ghosts", TwoPaths.Path.GHOST, Realm.TRUE_IMMORTAL, Realm.TRUE_IMMORTAL, Weight.HALF, 1 / 12.0, false),
	/** Meditation among the primeval dragon's bones, three times as fast there (MethodGrounds). */
	ANCESTRAL_COMMUNION("ancestral_communion", TwoPaths.Path.DEMON, Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, Weight.HALF, 1 / 4.0, true),
	/** One's own race's blood of Entering Nirvana or above drunk to remake the blood, or burn it (BloodlineNirvana). */
	BLOODLINE_NIRVANA("bloodline_nirvana", TwoPaths.Path.DEMON, Realm.ENTERING_NIRVANA, Realm.ENTERING_NIRVANA, Weight.NONE, 1 / 16.0, false),
	/** Meditation near a living primordial beast, twice as fast there (MethodGrounds). */
	PRIMORDIAL_RETURN("primordial_return", TwoPaths.Path.DEMON, Realm.TRUE_IMMORTAL, Realm.TRUE_IMMORTAL, Weight.HALF, 1 / 4.0, true),
	// --- 2026-11-01 (the user's picks of the second brainstorm: the herbs, the clone, a life borrowed) ---
	/** Spirit herbs eaten for their essence, by their age (SpiritHerbRefining). */
	SPIRIT_HERB_REFINING("spirit_herb_refining", TwoPaths.Path.HEAVEN, Realm.QI_REFINING, Realm.DAO_INTEGRATION, Weight.HALF, 1 / 12.0, false),
	/** One's clone left meditating, what it gathers banked and drawn back in with it (CloneSeclusion). */
	CLONE_SECLUSION("clone_seclusion", TwoPaths.Path.HEAVEN, Realm.NASCENT_SOUL, Realm.ENTERING_NIRVANA, Weight.HALF, 1 / 4.0, false),
	/** A living cultivator's body possessed and its life drawn off (BorrowedLife). */
	BORROWED_LIFE("borrowed_life", TwoPaths.Path.GHOST, Realm.NASCENT_SOUL, Realm.VOID_SHATTERING, Weight.HALF, 1 / 12.0, false),
	/**
	 * The Body Path's own (BodyPath, 2026-11-01): the blood circulated in meditation, and the body's deeds while it is drawn in (BodyPath#deed).
	 * The body is its own volume, every realm of it; past the Indestructible Body the Laws carry it on up the ladder, as every orthodox way
	 * (#spans).
	 */
	BODY_TEMPERING("body_tempering", TwoPaths.Path.BODY, Realm.QI_REFINING, Realm.TRUE_IMMORTAL, Weight.FULL, 1.0, true),
	// --- The Body Path's other ways (the user's "Go, all four", 2026-11-01), each tempering it only while drawn in (BodyPath#temperBy) ---
	/** Soaking in a water cauldron steeped with spirit herbs (MedicinalBath). */
	MEDICINAL_BATH("medicinal_bath", TwoPaths.Path.BODY, Realm.QI_REFINING, Realm.HEAVENLY_BEING, Weight.HALF, 1 / 12.0, false),
	/** Standing in the blood a slain demon beast spills (BeastBlood). */
	BEAST_BLOOD_BATHING("beast_blood_bathing", TwoPaths.Path.BODY, Realm.QI_REFINING, Realm.NASCENT_SOUL, Weight.HALF, 1 / 12.0, false),
	/** Every block covered on foot under heavy armour (WeightedTraining). */
	WEIGHTED_TRAINING("weighted_training", TwoPaths.Path.BODY, Realm.QI_REFINING, Realm.NASCENT_SOUL, Weight.HALF, 1 / 12.0, false),
	/** Blows traded in a spar with a cultivator, and a spar won (HundredBattles). */
	HUNDRED_BATTLES("hundred_battles", TwoPaths.Path.BODY, Realm.FOUNDATION_BUILDING, Realm.HEAVENLY_BEING, Weight.HALF, 1 / 12.0, false),
	/** Four Axis's, every path's: its axes of virtue fill its stages (FourAxis#fillBars). No one chooses it: Four Axis does. */
	FOUR_AXES("four_axes", null, Realm.FOUR_AXIS, Realm.FOUR_AXIS, Weight.NONE, 0, false);

	/** How much of one's talent divides the years a method's gains cost (Lifespan#seclusionYears). */
	public enum Weight {
		FULL, HALF, NONE;

		/** What {@code talent} (Physique.Grade#talent: x1 to x64) divides the years by. */
		public double divisor(double talent) {
			return switch (this) {
				case FULL -> talent;
				case HALF -> Math.sqrt(talent);
				case NONE -> 1;
			};
		}
	}

	private final String id;
	private final TwoPaths.Path path;
	private final Realm first, last;
	private final Weight weight;
	private final double yearsFactor;
	private final boolean meditates;

	CultivationMethod(String id, TwoPaths.Path path, Realm first, Realm last, Weight weight, double yearsFactor, boolean meditates) {
		this.id = id;
		this.path = path;
		this.first = first;
		this.last = last;
		this.weight = weight;
		this.yearsFactor = yearsFactor;
		this.meditates = meditates;
	}

	public String getId() { return id; }

	/** The path it cultivates; null for the four axes, every path's at Four Axis. */
	public TwoPaths.Path path() { return path; }

	/** The first realm it has a volume of, and the last. */
	public Realm first() { return first; }
	public Realm last() { return last; }

	public Weight weight() { return weight; }

	/** The share of a gain's years it costs (its talent's division aside): the orthodox ways the whole, the unorthodox less. */
	public double yearsFactor() { return yearsFactor; }

	/** What {@code talent} divides its gains' years by ({@link Weight#divisor}). */
	public double talentDivisor(double talent) { return weight.divisor(talent); }

	/** Whether meditation is what it gathers by; if not, meditation gathers nothing on its path while it is practised. */
	public boolean meditates() { return meditates; }

	/** Whether its volumes span {@code realm} at all (Four Axis never, but the axes' own). */
	public boolean spans(Realm realm) {
		if (realm == null) return false;
		if (this == FOUR_AXES) return realm == Realm.FOUR_AXIS;
		if (realm == Realm.FOUR_AXIS) return false;
		if (realm.isPastTrueImmortal()) return last == Realm.TRUE_IMMORTAL && isOrthodox(); // past True Immortal the Laws carry the orthodox ways
		return realm.ordinal() >= first.ordinal() && realm.ordinal() <= last.ordinal();
	}

	/** One of the four orthodox methods, whose volumes are in what its path is taught (the Body Path's in the body itself). */
	public boolean isOrthodox() {
		return this == QI_CIRCULATION || this == NETHERWORLD_BREATHING || this == BLOODLINE_PURIFICATION || this == BODY_TEMPERING;
	}

	/** Learned from a Method Manual (not orthodox, not the axes, not the Heavenly Thunder Body's own). */
	public boolean fromManual() {
		return !isOrthodox() && this != FOUR_AXES && this != THUNDER_TEMPERING;
	}

	public Component getDisplayName() { return Component.translatable(ModLang.cultivationMethodKey(id)); }

	/** What it is and how it gathers. */
	public Component getDescription() { return Component.translatable(ModLang.cultivationMethodKey(id) + ".description"); }

	/** The orthodox method of {@code path}: every cultivator of it has its volumes in what the path teaches. */
	public static CultivationMethod orthodox(TwoPaths.Path path) {
		return switch (path) {
			case HEAVEN -> QI_CIRCULATION;
			case GHOST -> NETHERWORLD_BREATHING;
			case DEMON -> BLOODLINE_PURIFICATION;
			case BODY -> BODY_TEMPERING;
		};
	}

	public static CultivationMethod byId(String id) {
		for (CultivationMethod method : values()) if (method.id.equals(id)) return method;
		return null;
	}
}
