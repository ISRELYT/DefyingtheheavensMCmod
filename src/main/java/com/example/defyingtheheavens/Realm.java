package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;

/**
 * Major realms. All balance numbers live here, so tuning is a one-file job.
 * Bonus values are for EARLY stage; later stages multiply them by {@link Stage#getPowerMultiplier()}.
 * <p>
 * Cultivation (cultBase, cult/sec) is the progress toward the next stage. Qi (maxQi, qi/sec) is the spendable energy pool
 * for spells and techniques: it refills on its own, from ~100 s for an empty Qi Refining pool down to ~40 s at Four Axis.
 */
public enum Realm {
	//            id                  cultBase  cult/sec  maxQi  qi/sec  health  damage  speed  armor  tough  knockback
	QI_REFINING(        "qi_refining",        100,      1,     100,     1,      2,    0.5,  0.03,    0,     0,   0.00),
	FOUNDATION_BUILDING("foundation_building", 800,      5,     250,     3,      8,    2.0,  0.08,    2,     0,   0.00),
	CORE_FORMATION(     "core_formation",     6400,     25,     600,     8,     20,    5.0,  0.15,    5,     2,   0.10),
	NASCENT_SOUL(       "nascent_soul",      51200,    125,    1500,    25,     45,   11.0,  0.25,    9,     4,   0.30),
	HEAVENLY_BEING(     "heavenly_being",   409600,    625,    3500,    70,     90,   24.0,  0.40,   15,     8,   0.60),
	// First realm beyond what the lower realms can sustain: only reachable through breakthroughs in the Upper Realm.
	// Its cultivation requirement is 10x this base (PlayerCultivation.cultivationRequired) to offset the Upper Realm's dense qi.
	FOUR_AXIS(          "four_axis",       3276800,   3125,    8000,   200,    180,   50.0,  0.55,   22,    14,   0.80);

	private final String id;
	private final double cultivationBase;
	private final double cultivationPerSecond;
	private final double maxQi;
	private final double qiGather;
	private final double maxHealth;
	private final double attackDamage;
	private final double moveSpeed;
	private final double armor;
	private final double toughness;
	private final double knockbackResistance;

	Realm(String id, double cultivationBase, double cultivationPerSecond, double maxQi, double qiGather, double maxHealth,
		  double attackDamage, double moveSpeed, double armor, double toughness, double knockbackResistance) {
		this.id = id;
		this.cultivationBase = cultivationBase;
		this.cultivationPerSecond = cultivationPerSecond;
		this.maxQi = maxQi;
		this.qiGather = qiGather;
		this.maxHealth = maxHealth;
		this.attackDamage = attackDamage;
		this.moveSpeed = moveSpeed;
		this.armor = armor;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
	}

	public String getId() { return id; }
	public double getCultivationBase() { return cultivationBase; }
	public double getCultivationPerSecond() { return cultivationPerSecond; }
	public double getMaxQi() { return maxQi; }
	/** Qi gathered per second, always (not only while meditating). */
	public double getQiGather() { return qiGather; }
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

	/** From Nascent Soul on, every minor stage is a bottleneck that needs its own Heavenly Tribulation. */
	public boolean hasStageTribulations() { return ordinal() >= NASCENT_SOUL.ordinal(); }

	public static Realm byIndex(int index) {
		return values()[Math.max(0, Math.min(values().length - 1, index))];
	}
}
