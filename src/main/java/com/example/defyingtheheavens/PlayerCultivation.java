package com.example.defyingtheheavens;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
 * <p>
 * The heaven methods (HeavenMethods) ride on the cultivation bar rather than make it: one cultivates with none (more slowly), a
 * method known is understood as far as the realm has come, and only a major breakthrough waits on one, of the realm ahead or deeper
 * ({@link #isHeldByMethod}). A fall (a failed tribulation, the nascent soul lost, the Chief Judge's gaze) takes the methods' layers
 * past the stage fallen to with it ({@link #setBackTo}). Cultivator NPCs advance by their own reckoning (CultivatorNpc#advance).
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
	/** Meditation refines unrefined qi at this many times its own cultivation rate, on top of that rate. */
	public static final double REFINE_RATE = 2.0;
	/** Slots on the ability wheel (held open by a key to switch abilities without the menu). */
	public static final int WHEEL_SLOTS = 8;

	/** No cultivation at all until a Marrow Cleansing Elixir opens the meridians (see {@link #awaken}). */
	private boolean mortal = true;
	/** A mortal's body tempering (see {@link Tempering}): the stage, and progress toward the next one. */
	private MortalStage mortalStage = MortalStage.LOW;
	private double tempering;
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
	/**
	 * The ability wheel's slots, clockwise from the top (null: empty); each ability sits in one slot at most. Saved and synced.
	 * It starts with the abilities in their menu order, so the wheel works before the player has arranged it.
	 */
	private final Ability[] wheel = defaultWheel();
	/** Rumours told to this player (see Journal). Saved, sent on its own packet. */
	private final List<Journal.Entry> journal = new ArrayList<>();
	/** The Netherworld's hold on them: ghostly qi, yin corruption, the Soul River. Saved only when not at its defaults. */
	private final NetherworldState netherworld = new NetherworldState();
	/** The heaven methods known (see HeavenMethods). Saved only when one is. */
	private final HeavenState heaven = new HeavenState();
	/** Synced but not saved: re-evaluated from the player's dimension on every join. */
	private boolean inUpperRealm;
	/**
	 * How dense ordinary qi is where they stand, against the lower realms' (1): space's thin qi, a planet's own (AstralRealm sets it
	 * as they move; synced, not saved). The Upper Realm's is {@link #UPPER_REALM_QI_MULTIPLIER} regardless.
	 */
	private double placeQi = 1.0;
	/** Realm Suppress from a stronger cultivator (see {@link RealmSuppressSystem}): synced but never saved. */
	private int pressureStages;
	private double pressurePenalty;

	// Alchemy (see PillItem): saved and synced.
	/** The realm a breakthrough pill has prepared the way into (null: none), and the share of its first stage it grants. */
	private Realm preparedRealm;
	private double preparedBonus;
	/** Extra qi gathering from a Qi Gathering Pill (0.5 = +50%), for as long as its effect lasts (see QiManager). */
	private double qiBoost;
	/**
	 * Medicinal toxicity (stored as pill resistance): how much less the next Cultivation Pill or Cultivation Fruit does (0 to
	 * {@link #MAX_PILL_RESISTANCE}); it wears off over time.
	 */
	private double pillResistance;
	/**
	 * Unrefined qi from pills and fruit: it only becomes cultivation while meditating, at {@link #REFINE_RATE} times the
	 * meditation rate (see MeditationManager).
	 */
	private double medicinalQi;
	/** While the soul is in the Inner Realm: where the body is (see {@link InnerRealm}); null otherwise. Saved. */
	private net.minecraft.nbt.CompoundTag innerReturn;
	/**
	 * While the nascent soul is out of the body (see {@link SoulProjection}): where the body sits and how it was. Saved, so a crash
	 * or a stop with the soul out still ends with the player back in their body; null otherwise.
	 */
	private net.minecraft.nbt.CompoundTag soulReturn;
	/**
	 * The harm the nascent soul carries and hasn't mended yet (health below its whole, SoulProjection#mendSouls): kept from one going
	 * out to the next, mended only while the soul rests in a body (its own, or one it possesses). Saved and synced; 0 when whole.
	 */
	private float soulWounds;
	/**
	 * A possession under way ({@link Possession}): for a soul in another's body, whose body and its own things put by ("Tenant",
	 * "Body", "Npc", "Stash"); for a player whose body is held, by whom and their game mode before ("Held", "By", "Mode", and
	 * "Returned": their body's things waiting for them after a crash). Saved, so a crash mid-way is put right as each comes back;
	 * null otherwise.
	 */
	private net.minecraft.nbt.CompoundTag possession;
	/** The nascent soul's state ({@link SoulProjection.State} ordinal): synced, never saved. */
	private int soulState;

	/**
	 * The heavens' leave to cultivate past Qi Refining Mid, granted by the Ritual to the Heavens ({@link HeavenRitual}); until
	 * then cultivation stops full at Qi Refining Mid. Saved and synced; anyone already past Qi Refining Mid has it.
	 */
	private boolean heavensLeave;
	/** Heaven's Favour from a generous offering (0 none, up to 3): every cultivation bonus and cultivation speed, raised. Saved and synced. */
	private int favour;
	/** Server-only, saved: the game day a Ritual to the Heavens last failed on; no new one until the next dawn. -1: none. */
	private long ritualFailedDay = -1;

	/** The cultivator's path, {@link Alignment#MIN} (devil) to {@link Alignment#MAX} (saint); saved and synced. */
	private int alignment;

	/** The body they were born with (see Physique): null until rolled, on their first join. Saved and synced. */
	private Physique physique;
	/** Three or True Element Roots: the elements whose heaven methods they may take up. Saved and synced. */
	private final EnumSet<QiElement> rootElements = EnumSet.noneOf(QiElement.class);
	/**
	 * Shattered Meridians: how far past a mortal's Peak the body has been refined (BodyRefining's stage, 0 none yet); the progress
	 * toward the next is {@link #tempering}. Saved and synced.
	 */
	private int bodyRefining;
	/** Extra qi gathering from cultivator clothing worn (see ClothingItem#qiGatherBonus): synced, not saved. */
	private double gearQiBonus;

	// Server-only bookkeeping for the Spatial Gap (not synced).
	private SpatialTrialHandler.Route pendingTrial = SpatialTrialHandler.Route.NONE;
	private boolean fallProtected;
	/** Torn into the Spatial Gap from the Upper Realm (SpatialTrialHandler#tear): tearing out leads back there, not to the Overworld. */
	private boolean gapFromUpper;
	/**
	 * Server-only, saved: a Heavenly Tribulation is under way. Set when one starts and cleared when it ends, so a player who
	 * leaves mid-trial (logging out, quitting, the game closing) still carries it on their return, where it counts as failed
	 * (see TribulationManager#onJoin).
	 */
	private boolean inTribulation;

	public Realm getRealm() { return realm; }
	public Stage getStage() { return stage; }
	public double getCultivation() { return cultivation; }

	/** Sets the realm and stage outright (commands, sync, tests); whoever has one is a cultivator, no longer a mortal. */
	public void setState(Realm realm, Stage stage, double cultivation) {
		this.mortal = false;
		this.realm = realm;
		this.stage = stage;
		this.cultivation = Math.max(0, cultivation);
		if (rank(realm, stage) > RITUAL_RANK) heavensLeave = true; // whoever is already past the ritual had the heavens' leave
	}

	// --- The Ritual to the Heavens (see HeavenRitual) ---

	/** The stage the heavens' leave is needed to leave: Qi Refining Mid. */
	public static final int RITUAL_RANK = rank(Realm.QI_REFINING, Stage.MID);
	/** What each step of Heaven's Favour adds to every cultivation bonus and to cultivation speed. */
	public static final double FAVOUR_PER_STEP = 0.05;

	public boolean hasHeavensLeave() { return heavensLeave; }

	public void setHeavensLeave(boolean leave) { heavensLeave = leave; }

	public int getFavour() { return favour; }

	public void setFavour(int favour) { this.favour = Math.max(0, Math.min(3, favour)); }

	/** What Heaven's Favour multiplies every cultivation bonus, the qi pool, its gathering and cultivation speed by. */
	public double favourFactor() { return 1 + FAVOUR_PER_STEP * favour; }

	public long getRitualFailedDay() { return ritualFailedDay; }

	public void setRitualFailedDay(long day) { ritualFailedDay = day; }

	/** As if the ritual had never been asked (an op setting a cultivator back to or below Qi Refining Mid): no leave, no Favour. */
	public void resetRitual() {
		heavensLeave = false;
		favour = 0;
		ritualFailedDay = -1;
	}

	/** At the stage only the heavens' leave lets one leave, without it: Qi Refining Mid. A Heaven Sacred Body has it from birth. */
	public boolean needsHeavensLeave() { return !mortal && !heavensLeave && physique != Physique.HEAVEN_SACRED_BODY && rank(realm, stage) == RITUAL_RANK; }

	// --- The body they were born with (see Physique) ---

	/** Their physique, or null until it is rolled (their first join). */
	public Physique getPhysique() { return physique; }

	/** Their physique, or a plain mortal body until one is rolled. */
	public Physique physique() { return physique == null ? Physique.MORTAL_BODY : physique; }

	public void setPhysique(Physique physique) { this.physique = physique; }

	/** Three or True Element Roots: their roots' elements (empty for any other physique). */
	public java.util.Set<QiElement> getRootElements() { return rootElements; }

	public void setRootElements(java.util.Collection<QiElement> elements) {
		rootElements.clear();
		rootElements.addAll(elements);
	}

	/** Whether their roots let them take up a heaven method of {@code element}. */
	public boolean rootsAllow(QiElement element) { return physique().allows(element, rootElements); }

	/** Shattered Meridians: the stage of body refining past a mortal's Peak (0 none yet). */
	public int getBodyRefining() { return bodyRefining; }

	/** Points put into each stat (StatPoints), by the stat's place. */
	private final int[] statPoints = new int[StatPoints.Stat.values().length];

	public int getStatPoints(StatPoints.Stat stat) { return statPoints[stat.ordinal()]; }

	public void setStatPoints(StatPoints.Stat stat, int points) { statPoints[stat.ordinal()] = Math.max(0, points); }

	/** Every point spent, all stats together. */
	public int statPointsSpent() {
		int spent = 0;
		for (int points : statPoints) spent += points;
		return spent;
	}

	public void clearStatPoints() { java.util.Arrays.fill(statPoints, 0); }

	/** The ability selected on the ability wheel, which the Use Selected Ability key switches (null: none yet). */
	private Ability selectedAbility;

	public Ability getSelectedAbility() { return selectedAbility; }

	public void setSelectedAbility(Ability ability) { selectedAbility = ability; }

	public void setBodyRefining(int stage) { bodyRefining = Math.max(0, Math.min(BodyRefining.LAST, stage)); }

	/** Born with shattered meridians: they can't cultivate, and temper the body instead. */
	public boolean hasShatteredMeridians() { return physique == Physique.SHATTERED_MERIDIANS; }

	/** {@link #needsHeavensLeave()} with the stage's cultivation full: the stage goes no further until the ritual is done. */
	public boolean isAtRitualGate() { return needsHeavensLeave() && cultivation >= cultivationRequired(); }

	/** No more cultivation fits: at a tribulation's bottleneck or at the Ritual to the Heavens' gate. */
	public boolean isStalled() { return isAtBottleneck() || isAtRitualGate(); }

	// --- The heaven method's gate (see HeavenMethods#opensWay) ---

	/**
	 * At Grand Perfection's bottleneck with no heaven method known of the realm ahead or deeper: the breakthrough waits on finding one
	 * (minor stages never do).
	 */
	public boolean isHeldByMethod() {
		return isAtBottleneck() && stage.isLast() && !isMaxed() && !HeavenMethods.opensWay(this, realm.next());
	}

	// --- The mortal path ---

	public boolean isMortal() { return mortal; }

	public void setMortal(boolean mortal) { this.mortal = mortal; }

	public MortalStage getMortalStage() { return mortalStage; }

	public double getTempering() { return tempering; }

	/** Tempering the current mortal stage needs to reach the next (0 at Peak; past it, Shattered Meridians' body refining). */
	public double temperingRequired() {
		if (mortalStage.isLast() && hasShatteredMeridians()) return BodyRefining.toNext(bodyRefining);
		return mortalStage.getTemperingToNext();
	}

	/** Whether tempering can still take this body further: a mortal below Peak, or Shattered Meridians below the last refining. */
	public boolean canTemper() {
		return mortal && (!mortalStage.isLast() || hasShatteredMeridians() && bodyRefining < BodyRefining.LAST);
	}

	public void setMortalState(MortalStage stage, double tempering) {
		this.mortalStage = stage;
		this.tempering = Math.max(0, tempering);
		if (!canTemper()) this.tempering = 0;
	}

	/** Mortal Peak: the body can take a Marrow Cleansing Elixir. */
	public boolean isMortalPeak() { return mortal && mortalStage.isLast(); }

	/**
	 * Adds tempering to a mortal's body, moving up through the stages; nothing past Peak.
	 *
	 * @return true if at least one stage was gained
	 */
	public boolean addTempering(double amount) {
		if (!canTemper() || amount <= 0) return false;
		boolean advanced = false;
		tempering += amount;
		while (canTemper() && tempering >= temperingRequired()) {
			tempering -= temperingRequired();
			if (!mortalStage.isLast()) mortalStage = mortalStage.next();
			else bodyRefining++;
			advanced = true;
		}
		if (!canTemper()) tempering = 0;
		return advanced;
	}

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

	public double getMedicinalQi() { return medicinalQi; }

	public net.minecraft.nbt.CompoundTag getInnerReturn() { return innerReturn; }

	public void setInnerReturn(net.minecraft.nbt.CompoundTag back) { innerReturn = back; }

	public net.minecraft.nbt.CompoundTag getSoulReturn() { return soulReturn; }

	public void setSoulReturn(net.minecraft.nbt.CompoundTag back) { soulReturn = back; }

	public float getSoulWounds() { return soulWounds; }

	public void setSoulWounds(float wounds) { soulWounds = Math.max(0, wounds); }

	public net.minecraft.nbt.CompoundTag getPossession() { return possession; }

	public void setPossession(net.minecraft.nbt.CompoundTag tag) { possession = tag; }

	public SoulProjection.State getSoulState() { return SoulProjection.State.byIndex(soulState); }

	public void setSoulState(SoulProjection.State state) { soulState = state.ordinal(); }

	/** The nascent soul is out of the body (flying free, possessing, in another's body, or on its way back), not merely gathering to leave. */
	public boolean isSoulOut() {
		SoulProjection.State state = getSoulState();
		return state == SoulProjection.State.PROJECTED || state == SoulProjection.State.RECALLING || state == SoulProjection.State.INHABITING;
	}

	/**
	 * Qi Sense is open: switched on. A soul sets out with it open (it is how a nascent soul sees: SoulProjection#emerge), but may
	 * shut it while out like anyone else.
	 */
	public boolean isQiSenseOn() {
		return Ability.QI_SENSE.isUnlocked(this) && isAbilityEnabled(Ability.QI_SENSE);
	}

	// --- Soul Projection's numbers (see SoulProjection) ---

	/**
	 * The path the nascent soul is born of (TwoPaths#awakens): the heaven path once it stands at Nascent Soul, else the ghost path.
	 * The soul's strength is that path's, it draws on that path's pool, and losing it is that path's fall (SoulProjection).
	 */
	public TwoPaths.Path soulPath() {
		TwoPaths.Path path = TwoPaths.awakens(this, Realm.NASCENT_SOUL, true);
		return path == null ? TwoPaths.Path.HEAVEN : path;
	}

	/** The rank Soul Projection reckons from: the soul's path's, as wielded, and never below Nascent Soul Early. */
	private int soulRank() {
		return Math.max(rank(Realm.NASCENT_SOUL, Stage.EARLY), TwoPaths.rank(this, soulPath(), true));
	}

	/** Hearts the projected soul holds: 1 at Nascent Soul Early, 3 more each stage (10 at Grand Perfection, 34 at Four Axis's). */
	public int soulHearts() {
		return 1 + 3 * (soulRank() - rank(Realm.NASCENT_SOUL, Stage.EARLY));
	}

	/** How far (blocks) the tether lets the soul stray from its body. */
	public int soulTetherRadius() {
		return SoulProjection.TETHER_RADII[Math.min(SoulProjection.TETHER_RADII.length - 1, soulRank() - rank(Realm.NASCENT_SOUL, Stage.EARLY))];
	}

	/**
	 * Qi a second the soul costs at {@code reach} (0 at the body, 1 at the tether's end): half the realm's own gathering at the
	 * body, twice it at the end of the tether, and that much again on top while possessing. Reckoned from the realm's plain
	 * gathering, so whatever adds to gathering (clothing, pills, the Upper Realm's dense qi) helps keep the soul out. A soul born of
	 * the ghost path ({@link #soulPath}) costs ghost qi, reckoned the same way from what that pool gathers in the Netherworld
	 * (SoulProjection#GHOST_SOUL_BASE): it fills by the place's ghostly density, not by the realm.
	 */
	public double soulCostPerSecond(double reach, boolean possessing) {
		double base = soulPath() == TwoPaths.Path.GHOST ? SoulProjection.GHOST_SOUL_BASE : CultivationStats.qiGather(getEffectiveRealm(), getEffectiveStage());
		return base * (0.5 + 1.5 * Math.max(0, Math.min(1, reach))) + (possessing ? base : 0);
	}

	public void setMedicinalQi(double amount) { medicinalQi = Math.max(0, amount); }

	/**
	 * A Cultivation Pill or Fruit: {@code amount} weakened by the current toxicity goes into the unrefined pool, and the
	 * toxicity rises. Returns what actually went in.
	 */
	public double takeMedicine(double amount) {
		double taken = amount * (1 - pillResistance);
		medicinalQi += taken;
		addPillResistance();
		// A Jade Poison Body refines it the moment it is taken (what doesn't fit before the next bottleneck waits, refined as soon as it does).
		if (physique == Physique.JADE_POISON_BODY) refineAtOnce();
		return taken;
	}

	/** A Jade Poison Body's unrefined qi, refined into cultivation at once as far as it fits. @return whether any was */
	public boolean refineAtOnce() {
		if (physique != Physique.JADE_POISON_BODY || medicinalQi <= 0) return false;
		double refined = refine(medicinalQi);
		if (refined <= 0) return false;
		addCultivation(refined);
		return true;
	}

	/**
	 * Meditation refines up to {@code max} unrefined qi into cultivation (nothing at a bottleneck, so none is lost there).
	 * Returns how much was refined.
	 */
	public double refine(double max) {
		if (mortal || medicinalQi <= 0 || isStalled() || isSuppressed()) return 0;
		double refined = Math.min(Math.min(medicinalQi, Math.max(0, max)), roomToBottleneck());
		medicinalQi -= refined;
		return refined;
	}

	/** Cultivation that still fits before the next bottleneck (the lower realm's cap, or the ritual's gate), across minor stages. */
	private double roomToBottleneck() {
		double room = cultivationRequired() - cultivation;
		Realm r = realm;
		Stage s = stage;
		while (!(s.isLast() || r.hasStageTribulations()) && !(rank(r, s) == RITUAL_RANK && !heavensLeave)) {
			s = s.next();
			if (lowerRealmBound && rank(r, s) > CAP_RANK) break;
			room += required(r, s);
		}
		return Math.max(0, room);
	}

	public void setPillResistance(double resistance) { pillResistance = Math.max(0, Math.min(MAX_PILL_RESISTANCE, resistance)); }

	/** A Cultivation Pill was taken: the next one works {@link #PILL_RESISTANCE_PER_PILL} less (a Jade Poison Body builds up no toxicity). */
	public void addPillResistance() {
		if (physique != Physique.JADE_POISON_BODY) setPillResistance(pillResistance + PILL_RESISTANCE_PER_PILL);
	}

	// --- Alignment and clothing ---

	public int getAlignment() { return alignment; }

	public void setAlignment(int alignment) { this.alignment = Alignment.clamp(alignment); }

	public double getGearQiBonus() { return gearQiBonus; }

	/** @return true if it changed */
	public boolean setGearQiBonus(double bonus) {
		bonus = Math.max(0, bonus);
		if (Math.abs(bonus - gearQiBonus) < 1.0e-9) return false;
		gearQiBonus = bonus;
		return true;
	}

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
	public int effectiveRank() { return Math.max(0, sustainedRank() - pressureStages); }

	/**
	 * Any rank as the realm the player stands in and another's pressure let them wield it, as {@link #effectiveRank} is the true
	 * rank's: the ghost path's, say (TwoPaths). -1 (none) stays -1.
	 */
	public int wielded(int rank) {
		if (rank < 0) return rank;
		return Math.max(0, (lowerRealmBound ? Math.min(rank, CAP_RANK) : rank) - pressureStages);
	}

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

	public double getPlaceQi() { return placeQi; }

	/** @return true if it changed */
	public boolean setPlaceQi(double value) {
		if (Math.abs(placeQi - value) < 1.0e-9) return false;
		placeQi = value;
		return true;
	}

	/** How many times the lower realms' qi this place gives: the Upper Realm's {@link #UPPER_REALM_QI_MULTIPLIER}, else the place's own density. */
	public double placeFactor() { return inUpperRealm ? UPPER_REALM_QI_MULTIPLIER : placeQi; }

	/** Outside the Upper Realm (lower realms and the Spatial Gap alike), no breakthrough may lead past the cap. */
	public boolean isBreakthroughLocked() {
		return !inUpperRealm && !isMaxed() && rank(breakthroughRealm(), breakthroughStage()) > CAP_RANK;
	}

	// --- Spatial Gap bookkeeping ---

	public SpatialTrialHandler.Route getPendingTrial() { return pendingTrial; }
	public void setPendingTrial(SpatialTrialHandler.Route route) { pendingTrial = route == null ? SpatialTrialHandler.Route.NONE : route; }
	public boolean isFallProtected() { return fallProtected; }
	public void setFallProtected(boolean value) { fallProtected = value; }
	public boolean isGapFromUpper() { return gapFromUpper; }
	public void setGapFromUpper(boolean value) { gapFromUpper = value; }

	// --- Heavenly Tribulation bookkeeping ---

	public boolean isInTribulation() { return inTribulation; }
	public void setInTribulation(boolean value) { inTribulation = value; }

	// --- Abilities ---

	public boolean isAbilityEnabled(Ability ability) { return !disabledAbilities.contains(ability); }

	/** Switches an ability on or off; switching on one of a pair that excludes each other switches the other off. */
	public void setAbilityEnabled(Ability ability, boolean enabled) {
		if (enabled) {
			disabledAbilities.remove(ability);
			if (ability.exclusiveWith() != null) disabledAbilities.add(ability.exclusiveWith());
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

	private static Ability[] defaultWheel() {
		Ability[] wheel = new Ability[WHEEL_SLOTS];
		for (int i = 0; i < WHEEL_SLOTS && i < Ability.values().length; i++) wheel[i] = Ability.values()[i];
		return wheel;
	}

	/** The ability in a wheel slot, or null if the slot is empty. */
	public Ability getWheelSlot(int slot) { return wheel[slot]; }

	/**
	 * Puts an ability in a wheel slot (null empties it). An ability already elsewhere on the wheel trades places with the
	 * slot's old occupant, so dragging one ability onto another swaps them and no ability is ever on the wheel twice.
	 */
	public void setWheelSlot(int slot, Ability ability) {
		if (slot < 0 || slot >= WHEEL_SLOTS) return;
		if (ability != null) {
			for (int i = 0; i < WHEEL_SLOTS; i++) {
				if (i != slot && wheel[i] == ability) wheel[i] = wheel[slot];
			}
		}
		wheel[slot] = ability;
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
		return CultivationStats.maxQi(getEffectiveRealm(), getEffectiveStage()) * favourFactor() * (1 - pressurePenalty) * physique().qiFactor()
				* (1 + StatPoints.bonus(this, StatPoints.Stat.SPIRIT));
	}

	/**
	 * Qi gathered per second where the player is now: {@link #UPPER_REALM_QI_MULTIPLIER} times faster in the Upper Realm, by the
	 * place's own density in the Astral Realm ({@link #placeFactor}), raised by a Qi Gathering Pill and by cultivator clothing,
	 * slowed by pressure. A mortal gathers none.
	 */
	public double qiGatherPerSecond() {
		if (mortal) return 0;
		return CultivationStats.qiGather(getEffectiveRealm(), getEffectiveStage()) * placeFactor()
				* (1 + qiBoost) * (1 + gearQiBonus) * favourFactor() * (1 - pressurePenalty) * physique().qiFactor()
				* (1 + StatPoints.bonus(this, StatPoints.Stat.INSIGHT) / 2)
				+ HeavenPassives.amount(this, QiElement.WATER); // Ceaseless Flow, the water methods' passive
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

	/** Qi a second of riding a sword costs ({@link SwordFlight}): less than Qi Flight, by the effective realm, not the stage. */
	public double swordFlightCostPerSecond() { return getEffectiveRealm().getSwordFlightCost(); }

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
		return required(realm, stage);
	}

	/** {@link #cultivationRequired()} for any realm and stage (the heaven methods' layers share it out: HeavenMethods#layerCost). */
	public static double required(Realm realm, Stage stage) {
		double required = realm.getCultivationBase() * stage.getCultivationMultiplier();
		return isUpperRealmOnly(realm) ? required * UPPER_REALM_QI_MULTIPLIER : required;
	}

	public double cultivationPerSecond() { return realm.getCultivationPerSecond() * stage.getPowerMultiplier(); }

	/**
	 * Cultivation meditation actually gathers per second where the player is now: the stage's rate plus a flat bonus
	 * (Rings of Power), times {@link #UPPER_REALM_QI_MULTIPLIER} in the Upper Realm or the place's own density in the Astral Realm
	 * ({@link #placeFactor}), raised by Heaven's Favour. Nothing while suppressed.
	 */
	public double meditationCultivationPerSecond(double bonus) {
		if (mortal || isSuppressed()) return 0;
		return (cultivationPerSecond() + bonus) * placeFactor() * favourFactor() * physique().cultivationFactor()
				* (1 + StatPoints.bonus(this, StatPoints.Stat.INSIGHT));
	}

	/** Leaving this stage takes a tribulation: Grand Perfection always, and every stage from Nascent Soul on. */
	private boolean needsTribulation() { return stage.isLast() || realm.hasStageTribulations(); }

	/** Full cultivation bar at a stage that can only be left through a tribulation. */
	public boolean isAtBottleneck() { return !mortal && needsTribulation() && cultivation >= cultivationRequired(); }

	/**
	 * Ready for the tribulation: at the bottleneck, not sealed by the lower realm, any pill the next realm needs taken, and into a new
	 * realm, a heaven method of it or deeper known ({@link #isHeldByMethod}).
	 */
	public boolean canBreakthrough() {
		return isAtBottleneck() && !isMaxed() && !isBreakthroughLocked() && !isMissingBreakthroughPill() && !isHeldByMethod();
	}

	/** Grand Perfection of the highest realm (currently Four Axis). */
	public boolean isMaxed() { return realm.isLast() && stage.isLast(); }

	/** Realm a breakthrough from the current stage leads to. */
	public Realm breakthroughRealm() { return stage.isLast() ? realm.next() : realm; }

	/** Stage a breakthrough from the current stage leads to. */
	public Stage breakthroughStage() { return stage.isLast() ? Stage.EARLY : stage.next(); }

	/**
	 * Adds cultivation. Minor stages advance automatically until a bottleneck, where cultivation is capped at the
	 * stage's requirement until the player breaks through (or, at Qi Refining Mid, until the heavens grant their leave). A
	 * suppressed cultivator gains nothing. The heaven methods hold no minor stage back (their layers settle in after it:
	 * HeavenMethods#settle).
	 *
	 * @return true if at least one minor stage was gained
	 */
	public boolean addCultivation(double amount) {
		if (mortal) return false; // sealed meridians hold nothing
		if (isSuppressed()) return false; // the lower realm can't sustain any further growth
		boolean advanced = false;
		cultivation += Math.max(0, amount);
		while (cultivation >= cultivationRequired()) {
			boolean pastCap = lowerRealmBound && rank(realm, stage.next()) > CAP_RANK;
			if (needsTribulation() || pastCap || needsHeavensLeave()) {
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
	 * this realm is used up, starting the new stage part of the way in. The new stage's heaven layers settle in with meditation.
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
	 * Tribulation failure: drop one minor stage (Early falls to the previous realm's Grand Perfection) and lose all cultivation,
	 * the heaven methods' layers past it with it ({@link #setBackTo}). A breakthrough pill's power is spent with it.
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
		setBackTo(realm, stage);
	}

	/**
	 * Set back to {@code realm} {@code stage} with none of it cultivated (a tribulation failed, the nascent soul lost, the Chief
	 * Judge's gaze): the heaven methods' layers past the stage are lost with it (HeavenMethods#clampTo), to settle in again when the
	 * realm climbs back.
	 */
	public void setBackTo(Realm realm, Stage stage) {
		setState(realm, stage, 0);
		HeavenMethods.clampTo(heaven, rank(realm, stage));
	}

	/**
	 * The nascent soul is lost (its tether cut, its body slain, the soul itself destroyed): whatever was built on it goes with it,
	 * down to Core Formation Grand Perfection with no cultivation (the methods' layers past it too), so the soul has to be formed
	 * again (the Nascent Soul tribulation). A breakthrough pill's power is spent too.
	 */
	public void loseNascentSoul() {
		if (mortal || realm.ordinal() < Realm.NASCENT_SOUL.ordinal()) return;
		setBackTo(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);
		preparedRealm = null;
		preparedBonus = 0;
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
		tag.putInt("MortalStage", mortalStage.ordinal());
		tag.putDouble("Tempering", tempering);
		tag.putInt("PreparedRealm", preparedRealm == null ? -1 : preparedRealm.ordinal());
		tag.putDouble("PreparedBonus", preparedBonus);
		tag.putDouble("QiBoost", qiBoost);
		tag.putDouble("PillResistance", pillResistance);
		tag.putDouble("MedicinalQi", medicinalQi);
		if (innerReturn != null) tag.put("InnerReturn", innerReturn.copy());
		if (soulReturn != null) tag.put("SoulReturn", soulReturn.copy());
		if (soulWounds > 0) tag.putFloat("SoulWounds", soulWounds);
		if (possession != null) tag.put("Possession", possession.copy());
		tag.putInt("Realm", realm.ordinal());
		tag.putInt("Stage", stage.ordinal());
		tag.putDouble("Cultivation", cultivation);
		tag.putDouble("Qi", qi);
		tag.putInt("EffectiveRealm", getEffectiveRealm().ordinal());
		tag.putInt("EffectiveStage", getEffectiveStage().ordinal());
		tag.putBoolean("LowerRealmBound", lowerRealmBound);
		tag.putString("SpatialTrial", pendingTrial.name());
		tag.putBoolean("FallProtected", fallProtected);
		if (gapFromUpper) tag.putBoolean("GapFromUpper", true);
		tag.putBoolean("InTribulation", inTribulation);
		tag.putInt("Alignment", alignment);
		if (physique != null) tag.putString("Physique", physique.getId());
		if (!rootElements.isEmpty()) {
			ListTag roots = new ListTag();
			for (QiElement element : rootElements) roots.add(StringTag.valueOf(element.name()));
			tag.put("RootElements", roots);
		}
		if (bodyRefining > 0) tag.putInt("BodyRefining", bodyRefining);
		if (statPointsSpent() > 0) tag.putIntArray("StatPoints", statPoints.clone());
		if (selectedAbility != null) tag.putString("SelectedAbility", selectedAbility.getId());
		tag.putBoolean("HeavensLeave", heavensLeave);
		tag.putInt("Favour", favour);
		tag.putLong("RitualFailedDay", ritualFailedDay);
		// Both lists, so an ability missing from either (one added since the save) takes its default.
		ListTag disabled = new ListTag();
		ListTag enabled = new ListTag();
		for (Ability ability : Ability.values()) {
			(disabledAbilities.contains(ability) ? disabled : enabled).add(StringTag.valueOf(ability.getId()));
		}
		tag.put("DisabledAbilities", disabled);
		tag.put("EnabledAbilities", enabled);
		ListTag wheelTag = new ListTag();
		for (Ability ability : wheel) wheelTag.add(StringTag.valueOf(ability == null ? "" : ability.getId()));
		tag.put("AbilityWheel", wheelTag);
		if (!journal.isEmpty()) tag.put("Journal", Journal.save(journal));
		CompoundTag nether = netherworld.save();
		if (!nether.isEmpty()) tag.put("Netherworld", nether);
		CompoundTag methods = heaven.save();
		if (!methods.isEmpty()) tag.put("HeavenMethods", methods);
		tag.putInt("PathModel", PATH_MODEL);
		return tag;
	}

	/**
	 * How the heaven path is saved: 3 since the methods are understood as far as the realm (HeavenMethods#understoodTo). Before it
	 * (2: the realm followed the methods' layers; below: the methods grew apart from it) the cultivation saved stands as it was.
	 */
	private static final int PATH_MODEL = 3;

	public static PlayerCultivation load(CompoundTag tag) {
		PlayerCultivation c = new PlayerCultivation();
		// Saves from before the qi pool kept cultivation progress under "Qi"; their pool starts empty and fills up.
		boolean legacy = !tag.contains("Cultivation");
		c.setState(Realm.byIndex(tag.getInt("Realm")), Stage.byIndex(tag.getInt("Stage")), tag.getDouble(legacy ? "Qi" : "Cultivation"));
		c.setQi(legacy ? 0 : tag.getDouble("Qi"));
		// Saves from before the mortal path were already cultivating, so a missing flag means "not mortal".
		c.mortal = tag.contains("Mortal") && tag.getBoolean("Mortal");
		// The body first: shattered meridians temper on past a mortal's Peak (saves from before physiques roll one on joining).
		c.physique = tag.contains("Physique") ? Physique.byId(tag.getString("Physique")) : null;
		ListTag roots = tag.getList("RootElements", Tag.TAG_STRING);
		for (int i = 0; i < roots.size(); i++) {
			try {
				c.rootElements.add(QiElement.valueOf(roots.getString(i)));
			} catch (IllegalArgumentException ignored) {
				// an element no longer known: dropped
			}
		}
		c.setBodyRefining(tag.getInt("BodyRefining"));
		int[] points = tag.getIntArray("StatPoints"); // none spent (or a save from before them): all zero
		for (int i = 0; i < points.length && i < c.statPoints.length; i++) c.statPoints[i] = Math.max(0, points[i]);
		c.selectedAbility = Ability.byId(tag.getString("SelectedAbility")); // none yet: null
		c.setMortalState(MortalStage.byIndex(tag.getInt("MortalStage")), tag.getDouble("Tempering"));
		int prepared = tag.contains("PreparedRealm") ? tag.getInt("PreparedRealm") : -1;
		c.preparedRealm = prepared >= 0 && prepared < Realm.values().length ? Realm.byIndex(prepared) : null;
		c.preparedBonus = tag.getDouble("PreparedBonus");
		c.qiBoost = tag.getDouble("QiBoost");
		c.setPillResistance(tag.getDouble("PillResistance"));
		c.medicinalQi = Math.max(0, tag.getDouble("MedicinalQi"));
		c.innerReturn = tag.contains("InnerReturn") ? tag.getCompound("InnerReturn").copy() : null;
		c.soulReturn = tag.contains("SoulReturn") ? tag.getCompound("SoulReturn").copy() : null;
		c.soulWounds = Math.max(0, tag.getFloat("SoulWounds"));
		c.possession = tag.contains("Possession") ? tag.getCompound("Possession").copy() : null;
		c.lowerRealmBound = tag.getBoolean("LowerRealmBound");
		c.pendingTrial = SpatialTrialHandler.Route.byName(tag.getString("SpatialTrial"));
		c.fallProtected = tag.getBoolean("FallProtected");
		c.gapFromUpper = tag.getBoolean("GapFromUpper");
		c.inTribulation = tag.getBoolean("InTribulation");
		c.setAlignment(tag.getInt("Alignment")); // saves from before alignment start neutral (0)
		// Saves from before the ritual: past Qi Refining Mid already counts as leave (setState); at or below it, they need it.
		if (tag.getBoolean("HeavensLeave")) c.heavensLeave = true;
		c.setFavour(tag.getInt("Favour"));
		c.ritualFailedDay = tag.contains("RitualFailedDay") ? tag.getLong("RitualFailedDay") : -1;
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
		// Saves from before the wheel keep the default one.
		if (tag.contains("AbilityWheel", Tag.TAG_LIST)) {
			ListTag wheelTag = tag.getList("AbilityWheel", Tag.TAG_STRING);
			for (int i = 0; i < WHEEL_SLOTS; i++) c.wheel[i] = null;
			for (int i = 0; i < WHEEL_SLOTS && i < wheelTag.size(); i++) c.setWheelSlot(i, Ability.byId(wheelTag.getString(i)));
		}
		Journal.load(tag.getList("Journal", Tag.TAG_COMPOUND), c.journal);
		c.netherworld.load(tag.getCompound("Netherworld")); // an empty compound when missing: every default
		c.heaven.load(tag.getCompound("HeavenMethods"));
		// Saves from before: every method known understood as far as the realm, none behind it or past it.
		if (tag.getInt("PathModel") < PATH_MODEL && !c.mortal) HeavenMethods.alignTo(c.heaven, rank(c.realm, c.stage));
		return c;
	}

	/** The Netherworld's hold on this cultivator (see NetherworldState). */
	public NetherworldState netherworld() {
		return netherworld;
	}

	/** The heaven methods this cultivator knows (see HeavenMethods). */
	public HeavenState heaven() {
		return heaven;
	}

	/** The rumours this player has been told (see Journal), newest first. Not part of the sync packet: sent on its own. */
	public List<Journal.Entry> journal() {
		return journal;
	}
}
