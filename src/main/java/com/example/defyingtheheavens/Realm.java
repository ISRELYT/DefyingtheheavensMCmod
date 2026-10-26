package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Major realms. All balance numbers live here, so tuning is a one-file job.
 * Bonus values are for EARLY stage; later stages multiply them by {@link Stage#getPowerMultiplier()}.
 * <p>
 * Cultivation (cultBase, cult/sec) is the progress toward the next stage. Qi (maxQi, qi/sec) is the spendable energy pool
 * for spells and techniques: it refills on its own, from ~100 s for an empty Qi Refining pool down to ~40 s from Four Axis on.
 * Flying on qi (fly/sec, from Core Formation) costs a flat amount per second while the pool keeps gathering, so flight only
 * ends where gathering falls short of the cost:
 * <ul>
 *   <li>Core Formation and Nascent Soul in the lower realms: the cost is above even Grand Perfection's qi/sec, so a full
 *   Early pool lasts ~30 s (CF) or ~50 s (NS), and later stages longer.</li>
 *   <li>Never in the Upper Realm: x10 gathering covers every realm's cost.</li>
 *   <li>Never from Heavenly Being on, anywhere: the cost is below the Early stage's qi/sec (and below HB Grand
 *   Perfection's, which a suppressed cultivator keeps).</li>
 * </ul>
 * Keep those three in mind when retuning qi/sec or fly/sec.
 * <p>
 * Riding a sword (sword/sec, from Foundation Building; see {@link SwordFlight}) costs less than flying on qi and is a
 * little slower. In the lower realms it stays finite through Core Formation even with the full Immortal clothing set
 * (+60% gathering: CF Grand Perfection then gathers ~16.6/s against 18), and through Nascent Soul up to its Mid stage
 * with it (Late and Grand Perfection in the full set ride forever). Like Qi Flight it never ends in the Upper Realm or from
 * Heavenly Being on. An Early pool lasts ~60 s at Foundation Building and Core Formation.
 */
public enum Realm {
	//            id                  cultBase  cult/sec  maxQi  qi/sec  fly/sec  sword/sec  health  damage  speed  armor  tough  knockback
	QI_REFINING(        "qi_refining",        100,      1,     100,     1,      0,        0,      2,    0.5,  0.03,    0,     0,   0.00),
	FOUNDATION_BUILDING("foundation_building", 800,      5,     250,     3,      0,        7,      8,    2.0,  0.08,    2,     0,   0.00),
	CORE_FORMATION(     "core_formation",     6400,     25,     600,     8,     28,       18,     20,    5.0,  0.15,    5,     2,   0.10),
	NASCENT_SOUL(       "nascent_soul",      51200,    125,    1500,    25,     55,       45,     45,   11.0,  0.25,    9,     4,   0.30),
	HEAVENLY_BEING(     "heavenly_being",   409600,    625,    3500,    70,     60,       48,     90,   24.0,  0.40,   15,     8,   0.60),
	// First realm beyond what the lower realms can sustain: only reachable through breakthroughs in the Upper Realm.
	// Its cultivation requirement is 10x this base (PlayerCultivation.cultivationRequired) to offset the Upper Realm's dense qi.
	FOUR_AXIS(          "four_axis",       3276800,   3125,    8000,   200,    150,      110,    180,   50.0,  0.55,   22,    14,   0.80),
	// The realms after Four Axis (the user's order, 2026-10-22), Upper Realm only as it is: Dao Integration twice Four Axis's might,
	// Void Shattering a larger leap (four times), Sacred Vessel and Entering Nirvana twice again, which ends the middle boundary;
	// True Immortal, the first of the major boundary, an immense leap (ten times). Armour, toughness and knockback stay at Four
	// Axis's: vanilla's own caps hold them there (80% of a blow turned, full immunity to knockback at Grand Perfection).
	DAO_INTEGRATION(    "dao_integration",  26214400,  15625,   18000,   450,    300,      220,    360,  100.0,  0.62,   22,    14,   0.80),
	VOID_SHATTERING(    "void_shattering", 209715200,  78125,   60000,  1500,   1000,      700,   1440,  400.0,  0.72,   22,    14,   0.80),
	SACRED_VESSEL(      "sacred_vessel",  1677721600, 390625,  130000,  3250,   2000,     1500,   2880,  800.0,  0.78,   22,    14,   0.80),
	ENTERING_NIRVANA(   "entering_nirvana", 13421772800.0, 1953125, 280000, 7000, 4500,   3300,   5760, 1600.0,  0.84,   22,    14,   0.80),
	TRUE_IMMORTAL(      "true_immortal", 107374182400.0, 9765625, 2500000, 62500, 40000, 30000, 57600, 16000.0,  0.95,   22,    14,   0.80),
	// The ladder past True Immortal (phase 3, 2026-10-26): each realm twice the last (the user's rule, "Each new realm should be 2X stronger
	// than the last"), its cultivation eight times and its gain five times the last's as before (so every stage still takes longer than
	// the one before: Stage#timeWeight), and each entered only with enough Laws comprehended besides (Laws#needed). Move speed stays at
	// True Immortal's.
	EARTH_IMMORTAL(     "earth_immortal", 858993459200.0, 48828125, 5000000, 125000, 80000, 60000, 115200, 32000.0,  0.95,   22,    14,   0.80),
	HEAVEN_IMMORTAL(    "heaven_immortal", 6871947673600.0, 244140625, 10000000, 250000, 160000, 120000, 230400, 64000.0,  0.95,   22,    14,   0.80),
	GOLDEN_IMMORTAL(    "golden_immortal", 54975581388800.0, 1220703125, 20000000, 500000, 320000, 240000, 460800, 128000.0,  0.95,   22,    14,   0.80),
	TAIYI_GOLDEN_IMMORTAL("taiyi_golden_immortal", 439804651110400.0, 6103515625.0, 40000000, 1000000, 640000, 480000, 921600, 256000.0, 0.95, 22, 14, 0.80),
	// The last two renamed by the user (2026-10-26): "change the name of Daluo Golden to Profound Sovereign, and Dao Ancestor to Heavenly Lord".
	PROFOUND_SOVEREIGN( "profound_sovereign", 3518437208883200.0, 30517578125.0, 80000000, 2000000, 1280000, 960000, 1843200, 512000.0, 0.95, 22, 14, 0.80),
	HEAVENLY_LORD(      "heavenly_lord", 28147497671065600.0, 152587890625.0, 160000000, 4000000, 2560000, 1920000, 3686400, 1024000.0, 0.95, 22, 14, 0.80);

	private final String id;
	private final double cultivationBase;
	private final double cultivationPerSecond;
	private final double maxQi;
	private final double qiGather;
	private final double qiFlightCost;
	private final double swordFlightCost;
	private final double maxHealth;
	private final double attackDamage;
	private final double moveSpeed;
	private final double armor;
	private final double toughness;
	private final double knockbackResistance;

	Realm(String id, double cultivationBase, double cultivationPerSecond, double maxQi, double qiGather, double qiFlightCost,
		  double swordFlightCost, double maxHealth, double attackDamage, double moveSpeed, double armor, double toughness,
		  double knockbackResistance) {
		this.id = id;
		this.cultivationBase = cultivationBase;
		this.cultivationPerSecond = cultivationPerSecond;
		this.maxQi = maxQi;
		this.qiGather = qiGather;
		this.qiFlightCost = qiFlightCost;
		this.swordFlightCost = swordFlightCost;
		this.maxHealth = maxHealth;
		this.attackDamage = attackDamage;
		this.moveSpeed = moveSpeed;
		this.armor = armor;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
	}

	public String getId() { return id; }

	/**
	 * Whether this realm's methods are to be had in the worlds there are (the lower realms, the Upper Realm, the Netherworld, the stars): a
	 * heaven method's slip or stele, a ghost method's, a demon race's blood. Entering Nirvana's at the deepest (the user's rule, 2026-10-25:
	 * "the highest you should be able to get is entering nirvana"): True Immortal's are kept for the Immortal Realm.
	 */
	public boolean taughtInTheWorld() { return ordinal() <= ENTERING_NIRVANA.ordinal(); }

	/** This realm, or the deepest realm the worlds teach if it is beyond it ({@link #taughtInTheWorld}). */
	public Realm atMostTaught() { return taughtInTheWorld() ? this : ENTERING_NIRVANA; }

	/** True Immortal or past it: an immortal, its life without end (the Five Decays come for it instead: FiveDecays). */
	public boolean isImmortal() { return ordinal() >= TRUE_IMMORTAL.ordinal(); }

	/** One of the ladder past True Immortal (Earth Immortal to Heavenly Lord): no methods teach it, Laws open its way (Laws#needed). */
	public boolean isPastTrueImmortal() { return ordinal() > TRUE_IMMORTAL.ordinal(); }

	/** How many realms past True Immortal this is (0 at True Immortal and below). */
	public int pastTrueImmortal() { return Math.max(0, ordinal() - TRUE_IMMORTAL.ordinal()); }

	/** The deepest realm a method of the heaven path, the ghost path or a demon's blood belongs to: True Immortal's (the ladder has none). */
	public static final Realm LAST_TAUGHT = TRUE_IMMORTAL;

	public double getCultivationBase() { return cultivationBase; }
	public double getCultivationPerSecond() { return cultivationPerSecond; }
	public double getMaxQi() { return maxQi; }
	/** Qi gathered per second, always (not only while meditating). */
	public double getQiGather() { return qiGather; }
	/** Qi spent per second of flight (only realms that {@link #canFlyOnQi()}). */
	public double getQiFlightCost() { return qiFlightCost; }
	/** Qi spent per second riding a sword (only realms that {@link #canRideSword()}). */
	public double getSwordFlightCost() { return swordFlightCost; }
	public double getMaxHealth() { return maxHealth; }
	public double getAttackDamage() { return attackDamage; }
	public double getMoveSpeed() { return moveSpeed; }
	public double getArmor() { return armor; }
	public double getToughness() { return toughness; }
	public double getKnockbackResistance() { return knockbackResistance; }

	public String getTranslationKey() { return ModLang.realmKey(id); }
	public Component getDisplayName() { return Component.translatable(getTranslationKey()); }

	public boolean isLast() { return ordinal() == values().length - 1; }
	public Realm next() { return isLast() ? this : values()[ordinal() + 1]; }
	public Realm previous() { return ordinal() == 0 ? this : values()[ordinal() - 1]; }

	/** From Foundation Building on, qi sustains the body: hunger and saturation stay full. */
	public boolean isSustainedByQi() { return ordinal() >= FOUNDATION_BUILDING.ordinal(); }

	/** From Core Formation (the golden core) on, the cultivator can fly on their qi (see {@link QiFlight}). */
	public boolean canFlyOnQi() { return ordinal() >= CORE_FORMATION.ordinal(); }

	/** From Foundation Building on, the cultivator can ride a sword through the air (see {@link SwordFlight}). */
	public boolean canRideSword() { return ordinal() >= FOUNDATION_BUILDING.ordinal(); }

	/** From Core Formation on, the cultivator can see the plane of Qi (see {@link QiSense}). */
	public boolean canSenseQi() { return ordinal() >= CORE_FORMATION.ordinal(); }

	/** From Nascent Soul on, every minor stage is a bottleneck that needs its own Heavenly Tribulation. */
	public boolean hasStageTribulations() { return ordinal() >= NASCENT_SOUL.ordinal(); }

	/** From Nascent Soul on, the cultivator can send their nascent soul out of the body (see {@link SoulProjection}). */
	public boolean canProjectSoul() { return ordinal() >= NASCENT_SOUL.ordinal(); }

	public static Realm byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
