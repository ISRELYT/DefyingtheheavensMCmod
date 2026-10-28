package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.object.builder.v1.advancement.CriterionRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * The milestones the mod's advancements ask for (the user's ask, 2026-10-27: "advancements like normal Minecraft but for the mod"; their
 * picks: five themed tabs, no rewards, what is already done granted on joining, both copies). Each is a name fired through
 * {@link MilestoneTrigger}: where it happens (a tribulation survived, a spell cast, a spar won: the {@code on...} calls below, from the
 * code that does it), and for a standing state (a realm reached, a sect's title held, a Ghost King's crown, a title taken, a small world
 * refined) by a sweep of everyone online every {@link #SWEEP_EVERY} ticks, so a world from before the advancements catches up as its
 * players join. Firing one already met costs nothing: a met criterion stops listening. The advancements themselves are data
 * (ModAdvancements, written by datagen); the worlds, treasures and beasts they ask for by vanilla's own triggers.
 */
public final class Milestones {
	public static final MilestoneTrigger TRIGGER = CriterionRegistry.register(new MilestoneTrigger());
	/** The standing states are looked at this often (ticks). */
	static final int SWEEP_EVERY = 40;

	// --- Their names: the advancements' data asks for these (MilestoneTrigger.TriggerInstance#reached) ---

	/** The highest of one's paths at {@code realm} or past it (heaven, ghost or Demon Path alike). */
	public static String realm(Realm realm) {
		return "realm/" + realm.name().toLowerCase(Locale.ROOT);
	}

	/** A heaven method's spell of {@code element} cast. */
	public static String spell(QiElement element) {
		return "spell/" + element.name().toLowerCase(Locale.ROOT);
	}

	public static final String TRIBULATION = "tribulation/survived", IMMORTAL_TRIBULATION = "tribulation/immortal";
	public static final String HIGH_PHYSIQUE = "physique/high", FOUR_AXES = "four_axes", LAW = "law/first", DECAYS = "decays/weathered",
			RITUAL = "ritual/granted";
	public static final String HEAVEN_METHOD = "method/heaven", SPELL = "spell/any", SPELL_HEAVEN_DEMON = "spell/heaven_demon", SPELL_GHOST = "spell/ghost";
	public static final String QI_SENSE = "ability/qi_sense", QI_FLIGHT = "flight/qi", SWORD_FLIGHT = "flight/sword", METEOR_FLIGHT = "flight/meteor";
	public static final String SOUL_PROJECTION = "soul/projection", POSSESSION = "soul/possession", CLONE = "technique/clone",
			CORE_DETONATION = "technique/core_detonation";
	public static final String STORAGE_RING = "treasure/storage_ring", THOUSAND_YEAR_FRUIT = "treasure/fruit_1000", SMALL_WORLD = "world/small";
	public static final String GHOST = "ghost/died", GHOST_PATH = "ghost/path", GHOST_KING = "ghost/king", SOUP = "ghost/soup", REBORN = "ghost/reborn",
			BOTH_PATHS = "paths/both";
	public static final String TALK = "talk/first", GIFT = "talk/gift", SWORN = "talk/sworn", SPAR_WON = "spar/won", KNEEL = "npc/kneel";
	public static final String SECT_JOINED = "sect/joined", SECT_ELDER = "sect/elder", SECT_MASTER = "sect/master", COURT = "court/joined",
			TITLE = "title/taken", BEAST_BLOOD = "bloodline/beast";
	/** A legend bonded with (any: LegendaryWeaponItem#bond). */
	public static final String LEGEND_BONDED = "legend/bonded";
	/** A Heavenly Venerable beaten in a Dao Duel, its weapon won (Legends#handOver). */
	public static final String DUEL_WON = "legend/duel_won";
	/** A sect mission seen through; a pillar of one's sect (SectMissions#PILLAR earned all told in it); one of every kind seen through (SectMissions#complete). */
	public static final String MISSION_DONE = "mission/done", MISSION_PILLAR = "mission/pillar", MISSION_EVERY_KIND = "mission/every_kind";

	/** {@code legend} heard of (its rumour told: TalkSystem), or found (in hand: Legends#seen). */
	public static String legendHeard(Legend legend) {
		return "legend/heard_" + legend.getId();
	}

	/** {@code p} has reached {@code milestone}. */
	public static void award(ServerPlayer p, String milestone) {
		if (p != null && !p.isRemoved()) TRIGGER.trigger(p, milestone);
	}

	// --- Where they happen ---

	/** A tribulation survived into {@code into} (any path's). */
	public static void onTribulation(ServerPlayer p, Realm into) {
		award(p, TRIBULATION);
		if (into.ordinal() >= Realm.TRUE_IMMORTAL.ordinal()) award(p, IMMORTAL_TRIBULATION);
	}

	/** A spell cast by {@code p} (Spells#cast): any, and which. */
	public static void onSpell(ServerPlayer p, Spell spell) {
		award(p, SPELL);
		if (spell.isDual()) award(p, SPELL_HEAVEN_DEMON);
		else if (spell.isHeaven()) award(p, spell(spell.element()));
		else award(p, SPELL_GHOST);
	}

	// --- The standing states, swept ---

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % SWEEP_EVERY != 11) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			try {
				sweep(p);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Milestones sweep failed for {}", p.getScoreboardName(), e);
			}
		}
	}

	/** What {@code p} stands at now, granted (their own record: not a body they have taken). Package-private for the game tests. */
	static void sweep(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.own(p);
		NetherworldState s = c.netherworld();
		int rank = c.isMortal() ? -1 : TwoPaths.higherRank(c, false);
		for (Realm realm : Realm.values()) {
			if (rank < PlayerCultivation.rank(realm, Stage.EARLY)) break;
			award(p, realm(realm));
		}
		if (c.physique().grade().ordinal() >= Physique.Grade.HIGH.ordinal()) award(p, HIGH_PHYSIQUE);
		boolean axes = true;
		for (FourAxis.Axis axis : FourAxis.Axis.values()) axes &= FourAxis.complete(c, axis);
		if (axes) award(p, FOUR_AXES);
		if (c.laws().total() > 0) award(p, LAW);
		if (!c.heaven().known().isEmpty()) award(p, HEAVEN_METHOD);
		if (Ability.QI_SENSE.isUnlocked(c) && c.isAbilityEnabled(Ability.QI_SENSE)) award(p, QI_SENSE);
		if (p.getAbilities().flying && !p.isCreative() && !p.isSpectator() && !s.ghost() && !c.isMortal()) award(p, QI_FLIGHT);
		if (SwordFlight.isRiding(p)) award(p, SWORD_FLIGHT);
		if (MeteorFlight.isStreaking(p)) award(p, METEOR_FLIGHT);
		if (SoulProjection.isSoul(p)) award(p, SOUL_PROJECTION);
		if (Possession.inhabits(p.getUUID())) award(p, POSSESSION);
		if (Clones.hasClone(p) || Clones.inClone(p)) award(p, CLONE);
		if (SmallWorlds.Data.get(p.server).of(p.getUUID()) != null) award(p, SMALL_WORLD);
		if (s.ghost()) award(p, GHOST);
		if (GhostMethods.ghostRank(s) >= 0) award(p, GHOST_PATH);
		if (GhostSystem.isKing(p)) award(p, GHOST_KING);
		if (!s.ghost() && TwoPaths.heavenRank(c, false) >= 0 && GhostMethods.ghostRank(s) >= 0) award(p, BOTH_PATHS);
		Sect sect = SectMembership.sectOf(p.server, p.getUUID());
		if (sect != null && !sect.extinct) {
			award(p, SECT_JOINED);
			NpcTitle title = SectMembership.titleOf(sect, p);
			if (title.isElderOrAbove()) award(p, SECT_ELDER);
			if (title == NpcTitle.SECT_MASTER) award(p, SECT_MASTER);
		}
		if (CelestialCourt.isMember(p.server, p.getUUID())) award(p, COURT);
		if (!PlayerTitles.shown(p).isEmpty()) award(p, TITLE);
		if (DemonPath.isDemon(c) || DemonPath.isHalfBlood(c)) award(p, BEAST_BLOOD);
	}

	/** Touching this class registers the trigger (DefyingTheHeavens#onInitialize). */
	public static void register() {}

	private Milestones() {}
}
