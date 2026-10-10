package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;

/**
 * One player's cultivation state. Used on the server (authoritative) and as a mirror on the client.
 * <p>
 * realm/stage/cultivation are the player's TRUE cultivation and are never lowered by Realm Suppression. While the player
 * is in a lower realm (Overworld, Nether, End) the EFFECTIVE stage, which drives stats, is capped at
 * {@link #LOWER_REALM_CAP_REALM} - {@link #LOWER_REALM_CAP_STAGE}, and nothing may advance past that cap.
 * Breakthroughs past the cap can only happen in the Upper Realm itself, not in the Spatial Gap either. In the Upper Realm,
 * meditation gathers {@link #UPPER_REALM_QI_MULTIPLIER} times as much cultivation as in the lower realms, and the realms
 * only it can sustain (Four Axis and above) need that many times more cultivation, so lower cultivators who reach it race
 * ahead while those realms still take real time.
 * <p>
 * Qi is a separate, spendable pool for spells, flight and other techniques. It refills by itself at the qi gather rate,
 * {@link #UPPER_REALM_QI_MULTIPLIER} times faster in the Upper Realm. Its maximum and gather rate follow the EFFECTIVE
 * stage, so suppression shrinks the pool too.
 * <p>
 * Separately, a stronger cultivator's Realm Suppress ability ({@link RealmSuppressSystem}) can put this one under
 * PRESSURE: the effective stage drops by {@link #getPressureStages()} more, and the qi pool and its gathering shrink by
 * {@link #getPressurePenalty()}. Pressure is never saved; qi above the shrunken pool is held back, not lost, and returns
 * when the pressure lifts.
 * <p>
 * Every player starts as a MORTAL: no cultivation, no qi, no realm bonuses and no abilities, until a Marrow Cleansing Elixir
 * opens their meridians ({@link #awaken}) and they begin at Qi Refining Early. Saves from before mortals existed load as
 * cultivators. Breaking into Foundation Building and Core Formation also takes a pill ({@link #requiresPill}), eaten at Grand
 * Perfection to prepare the breakthrough ({@link #prepareBreakthrough}).
 */
public class PlayerCultivation {
	/** The most the lower realms can sustain. */
	public static final Realm LOWER_REALM_CAP_REALM = Realm.HEAVENLY_BEING;
	public static final Stage LOWER_REALM_CAP_STAGE = Stage.GRAND_PERFECTION;
	private static final int CAP_RANK = rank(LOWER_REALM_CAP_REALM, LOWER_REALM_CAP_STAGE);
	/**
	 * The Upper Realm's dense qi: everything meditation gathers there (Ring of Power bonus included) and the qi pool's
	 * refill are multiplied by this. The cultivation requirements of upper-realm-only realms are multiplied by it too (see
	 * {@link #cultivationRequired()}).
	 */
	public static final double UPPER_REALM_QI_MULTIPLIER = 10.0;
	/** Each Cultivation Pill raises pill resistance by this much; the next one works that much less. */
	public static final double PILL_RESISTANCE_PER_PILL = 0.25;
	/** Pill resistance never passes this, so a pill always does something. */
	public static final double MAX_PILL_RESISTANCE = 0.9;
	/** The body clears one pill's worth of resistance every ten minutes. */
	public static final double PILL_RESISTANCE_DECAY_PER_SECOND = PILL_RESISTANCE_PER_PILL / 600.0;

	/** No cultivation at all until a Marrow Cleansing Elixir opens the meridians (see {@link #awaken}). */
	private boolean mortal = true;
	private Realm realm = Realm.QI_REFINING;
	private Stage stage = Stage.EARLY;
	/** Progress toward the next stage. */
	private double cultivation;
	/**
	 * The spendable qi pool; may briefly exceed {@link #maxQi()} after the maximum drops, until {@link #gatherQi} trims it
	 * (never while under pressure: see {@link #heldQi()}).
	 */
	private double qi;
	private boolean lowerRealmBound;
	/** Abilities switched off on the Abilities tab (or never switched on, for those that start off); everything else is on. */
	private final EnumSet<Ability> disabledAbilities = Ability.offByDefault();
	/** Synced but not saved: re-evaluated from the player's dimension on every join. */
	private boolean inUpperRealm;
	/** Realm Suppress from a stronger cultivator (see {@link RealmSuppressSystem}): synced but never saved. */
	private int pressureStages;
	private double pressurePenalty;

	// Alchemy (see PillItem): saved and synced.
	/** The realm a breakthrough pill has prepared the way into (null: none), and the share of its first stage it grants. */
	private Realm preparedRealm;
	private double preparedBonus;
	/** Extra qi gathering from a Qi Gathering Pill (0.5 = +50%), for as long as its effect lasts (see QiManager). */
	private double qiBoost;
	/** How much less the next Cultivation Pill does (0 to {@link #MAX_PILL_RESISTANCE}); it wears off over time. */
	private double pillResistance;

	// Server-only bookkeeping for the Spatial Gap (not synced).
	private SpatialTrialHandler.Route pendingTrial = SpatialTrialHandler.Route.NONE;
	private boolean fallProtected;

	public Realm getRealm() { return realm; }
	public Stage getStage() { return stage; }
	public double getCultivation() { return cultivation; }

	/** Sets the realm and stage outright (commands, sync, tests); whoever has one is a cultivator, no longer a mortal. */
	public void setState(Realm realm, Stage stage, double cultivation) {
		this.mortal = false;
		this.realm = realm;
		this.stage = stage;
		this.cultivation = Math.max(0, cultivation);
	}

	// --- The mortal path ---

	public boolean isMortal() { return mortal; }

	public void setMortal(boolean mortal) { this.mortal = mortal; }

	/**
	 * A Marrow Cleansing Elixir opens a mortal's meridians: they become a Qi Refining Early cultivator, starting with
	 * {@code startFraction} of that stage's cultivation already gathered.
	 */
	public void awaken(double startFraction) {
		setState(Realm.QI_REFINING, Stage.EARLY, 0);
		cultivation = cultivationRequired() * Math.max(0, Math.min(1, startFraction));
		qi = 0;
	}

	// --- Breakthrough pills ---

	/** Breaking into these realms takes a pill on top of the tribulation: Foundation Building and Core Formation. */
	public static boolean requiresPill(Realm target) {
		return target == Realm.FOUNDATION_BUILDING || target == Realm.CORE_FORMATION;
	}

	/** At the Grand Perfection before {@code target}: the moment a breakthrough pill for it can be taken. */
	public boolean canPrepare(Realm target) {
		return !mortal && stage.isLast() && !realm.isLast() && realm.next() == target && preparedRealm != target;
	}

	public Realm getPreparedRealm() { return preparedRealm; }

	public double getPreparedBonus() { return preparedBonus; }

	/** A breakthrough pill has been taken: the breakthrough into {@code target} may go ahead and starts {@code bonus} of the way in. */
	public void prepareBreakthrough(Realm target, double bonus) {
		preparedRealm = target;
		preparedBonus = Math.max(0, Math.min(1, bonus));
	}

	/** The next breakthrough leads into a realm that needs a pill, and none has been taken for it yet. */
	public boolean isMissingBreakthroughPill() {
		return !mortal && stage.isLast() && !realm.isLast() && requiresPill(realm.next()) && preparedRealm != realm.next();
	}

	// --- Qi Gathering Pills and pill resistance ---

	public double getQiBoost() { return qiBoost; }

	public void setQiBoost(double boost) { qiBoost = Math.max(0, boost); }

	public double getPillResistance() { return pillResistance; }

	public void setPillResistance(double resistance) { pillResistance = Math.max(0, Math.min(MAX_PILL_RESISTANCE, resistance)); }

	/** A Cultivation Pill was taken: the next one works {@link #PILL_RESISTANCE_PER_PILL} less. */
	public void addPillResistance() { setPillResistance(pillResistance + PILL_RESISTANCE_PER_PILL); }

	/** The body clears pill resistance over {@code seconds}. @return true if it changed */
	public boolean decayPillResistance(double seconds) {
		if (pillResistance <= 0) return false;
		pillResistance = Math.max(0, pillResistance - PILL_RESISTANCE_DECAY_PER_SECOND * seconds);
		return true;
	}

	/** Single ordering over every realm/stage pair; higher is stronger. */
	public static int rank(Realm realm, Stage stage) {
		return realm.ordinal() * Stage.values().length + stage.ordinal();
	}

	// --- Realm Suppression ---

	public boolean isLowerRealmBound() { return lowerRealmBound; }

	/** @return true if the flag changed */
	public boolean setLowerRealmBound(boolean bound) {
		if (lowerRealmBound == bound) return false;
		lowerRealmBound = bound;
		return true;
	}

	/** Realms that start beyond the cap (Four Axis and above): only the Upper Realm can sustain cultivating them. */
	public static boolean isUpperRealmOnly(Realm realm) { return rank(realm, Stage.EARLY) > CAP_RANK; }

	/** True cultivation exceeds what the current (lower) realm can sustain. */
	public boolean isSuppressed() { return lowerRealmBound && rank(realm, stage) > CAP_RANK; }

	/**
	 * The rank the realm the player stands in lets them wield: the true rank, capped by Realm Suppression in the lower
	 * realms. Another cultivator's pressure doesn't lower it, so Realm Suppress compares cultivators by this (no chains of
	 * suppressed suppressors).
	 */
	public int sustainedRank() {
		int rank = rank(realm, stage);
		return lowerRealmBound ? Math.min(rank, CAP_RANK) : rank;
	}

	/** {@link #sustainedRank()} less the stages another cultivator's pressure takes off; drives stats, qi and the domain. */
	private int effectiveRank() { return Math.max(0, sustainedRank() - pressureStages); }

	public Realm getEffectiveRealm() { return Realm.byIndex(effectiveRank() / Stage.values().length); }
	public Stage getEffectiveStage() { return Stage.byIndex(effectiveRank() % Stage.values().length); }

	// --- Realm Suppress pressure (another cultivator's ability) ---

	public boolean isUnderPressure() { return pressureStages > 0 || pressurePenalty > 0; }
	/** Stages the pressure takes off the effective stage (a whole realm's worth when the suppressor is a realm above). */
	public int getPressureStages() { return pressureStages; }
	/** Fraction (0-1) the pressure takes off the qi pool and its gathering; attributes lose the same through modifiers. */
	public double getPressurePenalty() { return pressurePenalty; }

	/** @return true if anything changed */
	public boolean setPressure(int stages, double penalty) {
		stages = Math.max(0, stages);
		penalty = Math.max(0, Math.min(1, penalty));
		if (pressureStages == stages && pressurePenalty == penalty) return false;
		pressureStages = stages;
		pressurePenalty = penalty;
		return true;
	}

	public boolean isInUpperRealm() { return inUpperRealm; }

	/** @return true if the flag changed */
	public boolean setInUpperRealm(boolean value) {
		if (inUpperRealm == value) return false;
		inUpperRealm = value;
		return true;
	}

	/** Outside the Upper Realm (lower realms and the Spatial Gap alike), no breakthrough may lead past the cap. */
	public boolean isBreakthroughLocked() {
		return !inUpperRealm && !isMaxed() && rank(breakthroughRealm(), breakthroughStage()) > CAP_RANK;
	}

	// --- Spatial Gap bookkeeping ---

	public SpatialTrialHandler.Route getPendingTrial() { return pendingTrial; }
	public void setPendingTrial(SpatialTrialHandler.Route route) { pendingTrial = route == null ? SpatialTrialHandler.Route.NONE : route; }
	public boolean isFallProtected() { return fallProtected; }
	public void setFallProtected(boolean value) { fallProtected = value; }

	// --- Abilities ---

	public boolean isAbilityEnabled(Ability ability) { return !disabledAbilities.contains(ability); }

	public void setAbilityEnabled(Ability ability, boolean enabled) {
		if (enabled) {
			disabledAbilities.remove(ability);
		} else {
			disabledAbilities.add(ability);
		}
	}

	/** Awakened by the realm and switched on: the ability takes effect. */
	public boolean isAbilityActive(Ability ability) { return ability.isUnlocked(this) && isAbilityEnabled(ability); }

	/** Switched-off abilities as a bit mask over their ordinals, for the sync packet. */
	public int getDisabledAbilityMask() {
		int mask = 0;
		for (Ability ability : disabledAbilities) mask |= 1 << ability.ordinal();
		return mask;
	}

	public void setDisabledAbilityMask(int mask) {
		disabledAbilities.clear();
		for (Ability ability : Ability.values()) {
			if ((mask & (1 << ability.ordinal())) != 0) disabledAbilities.add(ability);
		}
	}

	// --- Qi (the spendable pool) ---

	/** Qi in the pool, never more than {@link #maxQi()}. */
	public double getQi() { return Math.min(qi, maxQi()); }

	public void setQi(double qi) { this.qi = Math.max(0, qi); }

	/**
	 * Most qi the pool holds. Follows the effective stage, so a suppressed cultivator holds less, and pressure shrinks it. A
	 * mortal has none.
	 */
	public double maxQi() {
		if (mortal) return 0;
		return CultivationStats.maxQi(getEffectiveRealm(), getEffectiveStage()) * (1 - pressurePenalty);
	}

	/**
	 * Qi gathered per second where the player is now: {@link #UPPER_REALM_QI_MULTIPLIER} times faster in the Upper Realm,
	 * raised by a Qi Gathering Pill, slowed by pressure. A mortal gathers none.
	 */
	public double qiGatherPerSecond() {
		if (mortal) return 0;
		return CultivationStats.qiGather(getEffectiveRealm(), getEffectiveStage()) * (inUpperRealm ? UPPER_REALM_QI_MULTIPLIER : 1.0)
				* (1 + qiBoost) * (1 - pressurePenalty);
	}

	/** Qi above the shrunken pool that pressure holds back; it is back in the pool as soon as the pressure lifts. */
	private double heldQi() { return isUnderPressure() ? Math.max(0, qi - maxQi()) : 0; }

	/**
	 * Gathers {@code seconds} worth of qi while spending {@code spendPerSecond} on ongoing techniques (Qi Flight, Realm
	 * Suppress), never past the maximum or below zero. Qi above the maximum (left over after it dropped, e.g. when
	 * suppression set in) is trimmed, except what pressure holds back.
	 *
	 * @return true if the pool changed
	 */
	public boolean gatherQi(double seconds, double spendPerSecond) {
		double max = maxQi();
		double next = heldQi() + Math.max(0, Math.min(max, getQi() + (qiGatherPerSecond() - spendPerSecond) * seconds));
		if (next == qi) return false;
		qi = next;
		return true;
	}

	/**
	 * Qi a second of flight costs. Follows the effective realm, and doesn't grow with the stage, so later stages fly longer on
	 * their larger pools.
	 */
	public double qiFlightCostPerSecond() { return getEffectiveRealm().getQiFlightCost(); }

	/** Gathering here keeps up with Qi Flight, so it can go on forever (always in the Upper Realm, everywhere from Heavenly Being). */
	public boolean isQiFlightSustained() { return qiGatherPerSecond() >= qiFlightCostPerSecond(); }

	/** Spends qi for a spell or technique. @return false, spending nothing, if the pool holds less than {@code amount} */
	public boolean consumeQi(double amount) {
		if (amount < 0 || getQi() < amount) return false;
		qi = heldQi() + getQi() - amount;
		return true;
	}

	// --- Progression ---

	/**
	 * Cultivation needed to fill the current stage. Upper-realm-only realms need {@link #UPPER_REALM_QI_MULTIPLIER} times
	 * the realm's base, offsetting the dense qi they can only be cultivated in.
	 */
	public double cultivationRequired() {
		double required = realm.getCultivationBase() * stage.getCultivationMultiplier();
		return isUpperRealmOnly(realm) ? required * UPPER_REALM_QI_MULTIPLIER : required;
	}

	public double cultivationPerSecond() { return realm.getCultivationPerSecond() * stage.getPowerMultiplier(); }

	/**
	 * Cultivation meditation actually gathers per second where the player is now: the stage's rate plus a flat bonus
	 * (Rings of Power), times {@link #UPPER_REALM_QI_MULTIPLIER} in the Upper Realm. Nothing while suppressed.
	 */
	public double meditationCultivationPerSecond(double bonus) {
		if (mortal || isSuppressed()) return 0;
		return (cultivationPerSecond() + bonus) * (inUpperRealm ? UPPER_REALM_QI_MULTIPLIER : 1.0);
	}

	/** Leaving this stage takes a tribulation: Grand Perfection always, and every stage from Nascent Soul on. */
	private boolean needsTribulation() { return stage.isLast() || realm.hasStageTribulations(); }

	/** Full cultivation bar at a stage that can only be left through a tribulation. */
	public boolean isAtBottleneck() { return !mortal && needsTribulation() && cultivation >= cultivationRequired(); }

	/** Ready for the tribulation: at the bottleneck, not sealed by the lower realm, and any pill the next realm needs taken. */
	public boolean canBreakthrough() {
		return isAtBottleneck() && !isMaxed() && !isBreakthroughLocked() && !isMissingBreakthroughPill();
	}

	/** Grand Perfection of the highest realm (currently Four Axis). */
	public boolean isMaxed() { return realm.isLast() && stage.isLast(); }

	/** Realm a breakthrough from the current stage leads to. */
	public Realm breakthroughRealm() { return stage.isLast() ? realm.next() : realm; }

	/** Stage a breakthrough from the current stage leads to. */
	public Stage breakthroughStage() { return stage.isLast() ? Stage.EARLY : stage.next(); }

	/**
	 * Adds cultivation. Minor stages advance automatically until a bottleneck, where cultivation is capped at the
	 * stage's requirement until the player breaks through. A suppressed cultivator gains nothing.
	 *
	 * @return true if at least one minor stage was gained
	 */
	public boolean addCultivation(double amount) {
		if (mortal) return false; // sealed meridians hold nothing
		if (isSuppressed()) return false; // the lower realm can't sustain any further growth
		boolean advanced = false;
		cultivation += amount;
		while (cultivation >= cultivationRequired()) {
			boolean pastCap = lowerRealmBound && rank(realm, stage.next()) > CAP_RANK;
			if (needsTribulation() || pastCap) {
				cultivation = cultivationRequired();
				break;
			}
			cultivation -= cultivationRequired();
			stage = stage.next();
			advanced = true;
		}
		return advanced;
	}

	/**
	 * Moves to the breakthrough target (next stage, or next realm's Early stage) and empties cultivation; a pill taken for
	 * this realm is used up, starting the new stage part of the way in.
	 */
	public boolean breakthrough() {
		if (!canBreakthrough()) return false;
		Realm nextRealm = breakthroughRealm();
		Stage nextStage = breakthroughStage();
		realm = nextRealm;
		stage = nextStage;
		cultivation = 0;
		if (preparedRealm == realm) {
			cultivation = cultivationRequired() * preparedBonus;
			preparedRealm = null;
			preparedBonus = 0;
		}
		return true;
	}

	/**
	 * Tribulation failure: drop one minor stage (Early falls to the previous realm's Grand Perfection) and lose all
	 * cultivation. A breakthrough pill's power is spent with it.
	 */
	public void fallOneStage() {
		preparedRealm = null;
		preparedBonus = 0;
		if (stage.ordinal() > 0) {
			stage = stage.previous();
		} else if (realm.ordinal() > 0) {
			realm = realm.previous();
			stage = Stage.GRAND_PERFECTION;
		}
		cultivation = 0;
	}

	/** "Nascent Soul - Mid" style name. */
	public static Component rankName(Realm realm, Stage stage) {
		return Component.empty().append(realm.getDisplayName()).append(" - ").append(stage.getDisplayName());
	}

	/**
	 * True and effective stages are stored separately. The true stage is the source of truth and is never overwritten by
	 * suppression; the effective stage is written alongside it for inspection and recomputed from the true stage on load.
	 */
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("Mortal", mortal);
		tag.putInt("PreparedRealm", preparedRealm == null ? -1 : preparedRealm.ordinal());
		tag.putDouble("PreparedBonus", preparedBonus);
		tag.putDouble("QiBoost", qiBoost);
		tag.putDouble("PillResistance", pillResistance);
		tag.putInt("Realm", realm.ordinal());
		tag.putInt("Stage", stage.ordinal());
		tag.putDouble("Cultivation", cultivation);
		tag.putDouble("Qi", qi);
		tag.putInt("EffectiveRealm", getEffectiveRealm().ordinal());
		tag.putInt("EffectiveStage", getEffectiveStage().ordinal());
		tag.putBoolean("LowerRealmBound", lowerRealmBound);
		tag.putString("SpatialTrial", pendingTrial.name());
		tag.putBoolean("FallProtected", fallProtected);
		// Both lists, so an ability missing from either (one added since the save) takes its default.
		ListTag disabled = new ListTag();
		ListTag enabled = new ListTag();
		for (Ability ability : Ability.values()) {
			(disabledAbilities.contains(ability) ? disabled : enabled).add(StringTag.valueOf(ability.getId()));
		}
		tag.put("DisabledAbilities", disabled);
		tag.put("EnabledAbilities", enabled);
		return tag;
	}

	public static PlayerCultivation load(CompoundTag tag) {
		PlayerCultivation c = new PlayerCultivation();
		// Saves from before the qi pool kept cultivation progress under "Qi"; their pool starts empty and fills up.
		boolean legacy = !tag.contains("Cultivation");
		c.setState(Realm.byIndex(tag.getInt("Realm")), Stage.byIndex(tag.getInt("Stage")), tag.getDouble(legacy ? "Qi" : "Cultivation"));
		c.setQi(legacy ? 0 : tag.getDouble("Qi"));
		// Saves from before the mortal path were already cultivating, so a missing flag means "not mortal".
		c.mortal = tag.contains("Mortal") && tag.getBoolean("Mortal");
		int prepared = tag.contains("PreparedRealm") ? tag.getInt("PreparedRealm") : -1;
		c.preparedRealm = prepared >= 0 && prepared < Realm.values().length ? Realm.byIndex(prepared) : null;
		c.preparedBonus = tag.getDouble("PreparedBonus");
		c.qiBoost = tag.getDouble("QiBoost");
		c.setPillResistance(tag.getDouble("PillResistance"));
		c.lowerRealmBound = tag.getBoolean("LowerRealmBound");
		c.pendingTrial = SpatialTrialHandler.Route.byName(tag.getString("SpatialTrial"));
		c.fallProtected = tag.getBoolean("FallProtected");
		// Starts from the defaults (the constructor's), so saves from before an ability existed get its default.
		ListTag disabled = tag.getList("DisabledAbilities", Tag.TAG_STRING);
		for (int i = 0; i < disabled.size(); i++) {
			Ability ability = Ability.byId(disabled.getString(i));
			if (ability != null) c.disabledAbilities.add(ability); // unknown ids (a removed ability) are dropped
		}
		ListTag enabled = tag.getList("EnabledAbilities", Tag.TAG_STRING);
		for (int i = 0; i < enabled.size(); i++) {
			Ability ability = Ability.byId(enabled.getString(i));
			if (ability != null) c.disabledAbilities.remove(ability);
		}
		return c;
	}
}
