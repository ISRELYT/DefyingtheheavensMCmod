package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * The ghost path and its methods (see GhostMethod): how fast, how far, and what each layer brings.
 * <p>
 * <b>The ghost path.</b> The ghost methods' path is one of its own beside ordinary cultivation, and neither builds on the other:
 * one's ordinary realm neither opens a ghost method nor speeds it. Since 2026-10-19 (the user's ask) it is cultivated as the
 * heaven path is: <b>its own bar</b> ({@link #ghostRank}, NetherworldState#pathRank), filled by meditating on ghostly qi, its
 * stages stepping on by themselves; the methods ride on it as the heaven methods ride on the heaven path's. It begins (at Qi
 * Refining Early) with the first foundation learned, or for one of the dead with its first meditation on ghostly qi, method or
 * none ({@link #begin}: the user's ask, 2026-10-19), and stays begun when it is born again. A meditation draws in one qi or the
 * other (NetherworldState#cultivatingGhost, the Cultivation tab's switch), so the two paths are never cultivated at once; the
 * dead draw in only ghostly qi.
 * <p>
 * <b>The rate.</b> Meditating wherever ghostly qi flows, the bar fills at {@link #BASE_RATE} (a second a second, for a ghost
 * path of Nascent Soul, living, in a Soul Sand Valley) x the density / {@link GhostQi#SOUL_SAND_VALLEY} (so x10 in the Netherworld,
 * x12 on the Nine Nether Peaks, x0.5 in the rest of the Nether, x0.25 in the Overworld) x the ghost path's factor
 * ({@link #realmFactor}: -10% a realm below Nascent Soul, +10% above) x Qi Harmony x a Yin Gathering Pill x {@link #GHOST_FACTOR}
 * for the dead (made of yin) x {@link #STELE_BONUS} by a stele of the method practised x a possessor's guidance (Possession);
 * with no foundation known, x {@link #NO_METHOD_FACTOR}, as the heaven path without a method; x Insight (StatPoints#cultivationFactor,
 * every path's).
 * <p>
 * <b>The bar.</b> Every stage takes longer than the one before, as on the heaven path, and by the same steps (the user's ask,
 * 2026-10-19): each realm 1.6 times the one before ({@link #REALM_TIME_STEP}, the heaven path's eight times the cultivation for
 * five times the gain), its stages by Stage#timeWeight ({@link #required}); the whole way to Four Axis Grand Perfection about 53
 * hours in a Soul Sand Valley, a tenth of that in the Netherworld (what it was when a stage took its layers' cost). The stages step on
 * by themselves, but for the bottlenecks: the <b>ghost path's tribulation</b> at every Grand Perfection and at every stage from
 * Nascent Soul on (TribulationManager, the Netherworld's own: the user's ask, 2026-10-19), as on the heaven path.
 * <p>
 * <b>The gate.</b> Breaking into a ghost realm wants a foundation of that realm or deeper known ({@link #opensWay}): the Breathing
 * Art carries the path no further than Qi Refining's Grand Perfection, and only a deeper foundation, found, opens the way on.
 * <p>
 * <b>The methods are understood as far as the path has come</b> ({@link #understoodTo}): a foundation to the layer standing for the
 * stage (its realm's nine, as MethodLayers lays them out), the fire and the steps three layers a ghost realm (all nine from Core
 * Formation). Learned, at once; and as the path reaches new stages, their layers settle in over meditation ({@link #settle}: half
 * a layer's own cost, gathered on the path), every method known at once. Their spells, abilities and gains come with their layers.
 * <p>
 * <b>Learning.</b> Meditating within {@link #STELE_LEARN_RADIUS} of a Method Stele for {@link #STELE_LEARN_SECONDS} teaches
 * its method (whichever qi the meditation draws in); a Ghost Jade Slip, read, teaches it at once. Not to a mortal, alive or dead:
 * its meridians are closed, for the ghost path as for the heaven path, until a Marrow Cleansing Elixir opens them ({@link #refusal});
 * any method a mortal knows lies dormant, its spells and abilities out of reach.
 * <p>
 * A ghost whose ghost path reaches Core Formation is a Ghost King, and only so: the heaven path's realm makes no king (GhostSystem#isKing).
 */
public final class GhostMethods {
	/** The fire's and the steps' layers (a foundation's: GhostMethod#layers). */
	public static final int LAYERS = 9;
	/** The fire's and the steps' layers, in minutes of baseline meditation (what each cost to cultivate before the bar; half of it settles one in). */
	public static final int[] MINUTES = {5, 8, 12, 20, 30, 45, 60, 90, 120};
	public static final double BASE_RATE = 1.0;
	public static final double GHOST_FACTOR = 1.5;
	public static final double REALM_STEP = 0.10;
	public static final double STELE_BONUS = 1.25;
	public static final double STELE_LEARN_RADIUS = 4, STELE_BONUS_RADIUS = 8;
	public static final double STELE_LEARN_SECONDS = 15;
	/** The ghost path cultivated with no foundation known (one of the dead, or one born again who began it so): the heaven path's pace without a method. */
	public static final double NO_METHOD_FACTOR = HeavenMethods.NO_METHOD_FACTOR;
	/** A new stage's layers settle in over this share of their own cost, gathered on the ghost path (as the heaven methods'). */
	public static final double SETTLE_SHARE = 0.5;
	/** Each layer of Wandering Soul Steps quickens a ghost this much; each of Ghost Fire Scripture strengthens its blows this much. */
	public static final double STEPS_SPEED_PER_LAYER = 0.03, FIRE_DAMAGE_PER_LAYER = 0.10;
	/**
	 * From this layer of Wandering Soul Steps on, each layer speeds the free flight of the dead and of a Ghost King's form this much
	 * (QiFlight#freeSpeed: the fourth +10%, the ninth +60%; the user's pick, 2026-10-18, in Ghost Flight's place).
	 */
	public static final int STEPS_FLIGHT_FROM = 4;
	public static final float STEPS_FLIGHT_PER_LAYER = 0.10f;
	/** Yin Wind Steps: this much swifter (and no harm from falls). */
	public static final double YIN_WIND_SPEED = 0.20;
	/** The abilities' upkeep from the ghostly qi pool, a second. */
	public static final double GHOST_EYE_COST = 0.5, VEIL_COST = 1.0, YIN_WIND_COST = 1.0, GHOST_FLIGHT_COST = 2.0;

	private static final UUID SPEED_ID = UUID.fromString("0f6d9a52-2c1e-4a37-8f0b-3b2e5d9c7a11");
	private static final UUID DAMAGE_ID = UUID.fromString("7a3c1e58-9d4b-4f62-b0a7-5c8e2d1f6b93");

	/** Layers of the fire or the steps each of the ghost path's realms carries: three in Qi Refining, all nine from Core Formation. */
	public static final int LAYERS_PER_REALM = 3;
	/** The very top of the ghost path, as the heaven path's (the user's ask, 2026-10-22; Four Axis's before): Heavenly Lord Grand Perfection since phase 3. */
	public static final int LAST_RANK = PlayerCultivation.LAST_RANK;

	/** Seconds of baseline meditation through layer {@code layer} of {@code method} (1 to its last): what a layer stands for. */
	public static double layerCost(GhostMethod method, int layer) {
		int at = Math.max(1, Math.min(method.layers(), layer));
		if (!method.isFoundation()) return MINUTES[at - 1] * 60.0;
		return MethodLayers.cost(at);
	}

	/** What layer {@code layer} of {@code method} takes to settle in ({@link #settle}): {@link #SETTLE_SHARE} of its cost. */
	public static double settleCost(GhostMethod method, int layer) {
		return SETTLE_SHARE * layerCost(method, layer);
	}

	// --- The ghost path ---

	/** Seconds of baseline meditation (a Soul Sand Valley, the living, at Nascent Soul's pace) a Qi Refining stage of time weight 1 takes. */
	public static final double STAGE_SECONDS = 1150;
	/** Each realm's stages take this many times the realm before's, as on the heaven path (eight times the cultivation, five times the gain). */
	public static final double REALM_TIME_STEP = 1.6;

	/**
	 * The ghost path's cultivation through the stage {@code rank} stands for: {@link #STAGE_SECONDS} x {@link #REALM_TIME_STEP} a
	 * realm x the stage's time weight (Stage#timeWeight, the heaven path's own), x the realm's pace ({@link #realmFactor}) so the
	 * time is that whatever the realm: every stage longer than the one before, a realm's Early longer than the Grand Perfection before
	 * it (the user's ask, 2026-10-19: before, a stage took its layers' cost, and Grand Perfection was quicker than Mid). 0 for none.
	 */
	public static double required(int rank) {
		if (rank < 0) return 0;
		int stages = Stage.values().length;
		Realm realm = Realm.byIndex(Math.min(Realm.values().length - 1, rank / stages));
		Stage stage = Stage.byIndex(rank % stages);
		// (x PlayerCultivation#STAGE_LENGTH since 2026-10-27, as the heaven path's.)
		return Math.round(STAGE_SECONDS * Math.pow(REALM_TIME_STEP, realm.ordinal()) * stage.timeWeight() * realmFactor(realm) * PlayerCultivation.STAGE_LENGTH);
	}

	/** The ghost path (a rank, see PlayerCultivation#rank) layer {@code layer} of the foundation {@code method} stands for; -1 for none. */
	public static int floor(GhostMethod method, int layer) {
		if (!method.isFoundation() || layer <= 0) return -1;
		return MethodLayers.floor(Math.min(method.layers(), layer));
	}

	/**
	 * The foundation {@code s} has understood deepest (of two as deep, the one that goes further); or null if it knows none. It is the
	 * one that reaches furthest, for the ghost path's gate ({@link #opensWay}) and the dead's hearts.
	 */
	public static GhostMethod deepestFoundation(NetherworldState s) {
		GhostMethod deepest = null;
		for (GhostMethod method : GhostMethod.values()) {
			if (!method.isFoundation() || !s.knows(method)) continue;
			if (deepest == null || s.layer(method) > s.layer(deepest) || s.layer(method) == s.layer(deepest) && method.tier() > deepest.tier()) {
				deepest = method;
			}
		}
		return deepest;
	}

	/** The deepest layer of any foundation {@code s} knows: 0 if it knows none. */
	public static int foundationDepth(NetherworldState s) {
		GhostMethod deepest = deepestFoundation(s);
		return deepest == null ? 0 : s.layer(deepest);
	}

	/** Where the ghost path stands (a rank: its bar's), or -1 if it isn't begun. */
	public static int ghostRank(NetherworldState s) {
		return s.pathRank();
	}

	/** The ghost path's realm, or null if it isn't begun. */
	public static Realm ghostRealm(NetherworldState s) {
		int rank = ghostRank(s);
		return rank < 0 ? null : Realm.byIndex(rank / Stage.values().length);
	}

	/** How far the ghost path has come through its stage, 0 to 1 (the Cultivation tab's bar). 0 if it isn't begun. */
	public static double stageProgress(NetherworldState s) {
		double required = required(ghostRank(s));
		return required <= 0 ? 0 : Math.min(1, s.pathCultivation() / required);
	}

	/**
	 * How far the ghost path stood through its stage by its deepest foundation's layers, as it was reckoned before the bar (0 to 1):
	 * for a world saved then (NetherworldState#load), to start the bar from.
	 */
	static double layeredStageProgress(NetherworldState s) {
		GhostMethod deepest = deepestFoundation(s);
		if (deepest == null) return 0;
		int layer = s.layer(deepest);
		int rank = floor(deepest, layer);
		double done = 0, whole = 0;
		for (int at = MethodLayers.deepestAt(rank - 1) + 1; at <= MethodLayers.deepestAt(rank); at++) {
			double cost = MethodLayers.cost(at);
			whole += cost;
			if (at < layer) done += cost;
			else if (at == layer) done += Math.min(cost, s.progress(deepest));
		}
		return whole <= 0 ? 0 : Math.min(1, done / whole);
	}

	/** The name of a rank (PlayerCultivation#rankName), for the ghost path's. */
	public static Component rankName(int rank) {
		return PlayerCultivation.rankName(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length));
	}

	/** Leaving the stage {@code rank} stands for takes the ghost path's tribulation: Grand Perfection always, every stage from Nascent Soul. */
	public static boolean needsTribulation(int rank) {
		Stage stage = Stage.byIndex(rank % Stage.values().length);
		return stage.isLast() || Realm.byIndex(rank / Stage.values().length).hasStageTribulations();
	}

	/** The ghost path at its very top: True Immortal Grand Perfection ({@link #LAST_RANK}). */
	public static boolean isMaxed(NetherworldState s) {
		return ghostRank(s) >= LAST_RANK;
	}

	/** The ghost path's bar full at a stage only its tribulation leaves (a mortal's lies dormant). */
	public static boolean atBottleneck(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		int rank = ghostRank(s);
		return !c.isMortal() && rank >= 0 && needsTribulation(rank) && s.pathCultivation() >= required(rank);
	}

	/**
	 * Whether {@code c}'s foundations open the ghost path's way into {@code realm}: one of it or deeper known. Qi Refining wants none;
	 * the ladder past True Immortal, which no foundation carries, wants the Laws instead (Laws#opensWay, phase 3).
	 */
	public static boolean opensWay(PlayerCultivation c, Realm realm) {
		if (realm == Realm.QI_REFINING) return true;
		if (realm.isPastTrueImmortal()) return Laws.opensWay(c, realm);
		for (GhostMethod method : c.netherworld().knownMethods()) {
			if (method.isFoundation() && method.realm().ordinal() >= realm.ordinal()) return true;
		}
		return false;
	}

	/** At Grand Perfection's bottleneck with no foundation known of the realm ahead: the breakthrough waits on finding one. */
	public static boolean heldByMethod(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		int rank = ghostRank(s);
		return atBottleneck(c) && Stage.byIndex(rank % Stage.values().length).isLast() && !isMaxed(s)
				&& !opensWay(c, Realm.byIndex(rank / Stage.values().length + 1));
	}

	/** Ready for the ghost path's tribulation: at its bottleneck, not at the top, the way ahead open, in a place that holds it. */
	public static boolean canBreakthrough(PlayerCultivation c) {
		return atBottleneck(c) && !isMaxed(c.netherworld()) && !heldByMethod(c) && !heldByAxis(c) && !heldByPlace(c);
	}

	/**
	 * At its bottleneck with the stage ahead True Immortal's or past it, outside the Immortal Realm: only there is that breakthrough made,
	 * on any path (PlayerCultivation#isImmortalRealmLocked, the user's rule of 2026-10-26). Why: PlayerCultivation#lockLine.
	 */
	public static boolean heldByPlace(PlayerCultivation c) {
		NetherworldState s = c.netherworld();
		return atBottleneck(c) && !isMaxed(s) && c.isImmortalRealmLocked(ghostRank(s) + 1);
	}

	/** At a Four Axis stage's bottleneck with its axis of virtue not yet complete (FourAxis: the cultivator's, both paths'). */
	public static boolean heldByAxis(PlayerCultivation c) {
		return atBottleneck(c) && FourAxis.holds(c, ghostRank(c.netherworld()));
	}

	/** Why the ghost path's breakthrough waits on a foundation ({@link #heldByMethod}): one of the realm ahead is wanted (past True Immortal, the Laws). */
	public static Component gateLine(PlayerCultivation c) {
		int rank = ghostRank(c.netherworld());
		Realm ahead = Realm.byIndex(Math.min(Realm.values().length - 1, Math.max(0, rank) / Stage.values().length + 1));
		if (ahead.isPastTrueImmortal()) return Laws.gateLine(c, ahead);
		return Component.translatable(ModLang.GHOST_GATE_NONE, ahead.getDisplayName());
	}

	/**
	 * Adds {@code amount} to the ghost path's bar. Its stages step on by themselves, up to a bottleneck (a tribulation's), where the bar
	 * stops full. A path not begun gains nothing. @return how many stages were gained
	 */
	public static int addCultivation(NetherworldState s, double amount) {
		int rank = ghostRank(s);
		if (rank < 0 || amount <= 0) return 0;
		double cultivation = s.pathCultivation() + amount;
		int gained = 0;
		while (cultivation >= required(rank)) {
			if (needsTribulation(rank)) {
				cultivation = required(rank);
				break;
			}
			cultivation -= required(rank);
			rank++;
			gained++;
		}
		s.setPath(rank, cultivation);
		return gained;
	}

	/** The ghost path's tribulation survived: on to the next stage (or realm), its bar empty; the new stage's layers settle in. */
	public static boolean breakthrough(NetherworldState s) {
		int rank = ghostRank(s);
		if (rank < 0 || rank >= LAST_RANK) return false;
		s.setPath(rank + 1, 0);
		return true;
	}

	/** The ghost path's tribulation failed: a stage down (Early to the realm before's Grand Perfection), its cultivation lost, the methods' layers past it. */
	public static void fallOneStage(NetherworldState s) {
		int rank = ghostRank(s);
		if (rank < 0) return;
		s.setPath(Math.max(0, rank - 1), 0);
		clampTo(s, ghostRank(s));
	}

	/**
	 * The ghost path falls to {@code rank} (its nascent soul lost, its core detonated), its cultivation there lost, every method
	 * understood no deeper than that rank's last layer ({@link #clampTo}). A path already at or below it is untouched.
	 */
	public static void fallTo(NetherworldState s, int rank) {
		if (ghostRank(s) <= rank) return;
		s.setPath(rank, 0);
		clampTo(s, rank);
	}

	/** Every method known understood no deeper than the ghost path at {@code rank} carries it ({@link #understoodTo}). */
	public static void clampTo(NetherworldState s, int rank) {
		for (GhostMethod method : java.util.List.copyOf(s.knownMethods())) {
			int deepest = Math.max(1, understoodAt(method, rank));
			if (s.layer(method) > deepest) s.setMethod(method, deepest, 0);
		}
	}

	/**
	 * The ghost path set to {@code rank} (-1: not begun), its stage's cultivation none, every method understood exactly as far as it
	 * carries them, nothing settling (an op's command, the game tests).
	 */
	public static void setPath(NetherworldState s, int rank) {
		s.setPath(rank, 0);
		alignTo(s);
	}

	/** Every method known understood exactly as far as the ghost path carries it, nothing settling (those deeper set back, those behind brought up). */
	public static void alignTo(NetherworldState s) {
		for (GhostMethod method : java.util.List.copyOf(s.knownMethods())) s.setMethod(method, Math.max(1, understoodTo(s, method)), 0);
	}

	// --- How far the methods are understood ---

	/**
	 * The layer {@code method} is understood to once settled: a foundation to the deepest standing for the ghost path's stage (no
	 * further than its last); the fire and the steps three a ghost realm (all nine from Core Formation). Not begun: as Qi Refining Early.
	 */
	public static int understoodTo(NetherworldState s, GhostMethod method) {
		return understoodAt(method, Math.max(0, ghostRank(s)));
	}

	private static int understoodAt(GhostMethod method, int rank) {
		if (method.isFoundation()) return Math.min(method.layers(), MethodLayers.deepestAt(Math.max(0, rank)));
		int realm = Math.min(Math.max(0, rank) / Stage.values().length, Realm.CORE_FORMATION.ordinal());
		return Math.min(LAYERS, LAYERS_PER_REALM * (realm + 1));
	}

	/**
	 * The ghost path's rank from which layer {@code layer} of {@code method} is understood ({@link #understoodTo}): a foundation's, the
	 * stage it stands for; the fire's and the steps', their ghost realm's Early (three a realm, all nine from Core Formation).
	 */
	public static int opensAt(GhostMethod method, int layer) {
		if (method.isFoundation()) return MethodLayers.floor(Math.max(1, Math.min(method.layers(), layer)));
		int realm = Math.min(Realm.CORE_FORMATION.ordinal(), (Math.max(1, Math.min(LAYERS, layer)) - 1) / LAYERS_PER_REALM);
		return PlayerCultivation.rank(Realm.byIndex(realm), Stage.EARLY);
	}

	/** Whether {@code method}'s layers are still settling in, behind the ghost path. */
	public static boolean settling(NetherworldState s, GhostMethod method) {
		return s.knows(method) && s.layer(method) < understoodTo(s, method);
	}

	/**
	 * Adds {@code amount} of settling to {@code method}, layer by layer, up to layer {@code upTo}; with nothing left to settle there,
	 * none is kept. Never lowers a layer. Returns how many layers settled.
	 */
	public static int settle(NetherworldState s, GhostMethod method, double amount, int upTo) {
		int layer = s.layer(method);
		if (layer <= 0) return 0;
		if (layer >= upTo) {
			if (s.progress(method) > 0) s.setMethod(method, layer, 0);
			return 0;
		}
		if (amount <= 0) return 0;
		double progress = s.progress(method) + amount;
		int gained = 0;
		while (layer < upTo && progress >= settleCost(method, layer + 1)) {
			progress -= settleCost(method, layer + 1);
			layer++;
			gained++;
		}
		s.setMethod(method, layer, layer >= upTo ? 0 : progress);
		return gained;
	}

	/**
	 * Half a second of meditation on the ghost path for {@code player}'s methods (MeditationManager): every method known whose layers
	 * lag behind the path settles by {@code gained} (what the bar gathered), by its own stele quicker; each layer that settles is told,
	 * with any spell it brings. Returns whether any did.
	 */
	static boolean meditate(ServerPlayer player, double gained, java.util.Set<GhostMethod> byStele) {
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		boolean any = false;
		for (GhostMethod method : java.util.List.copyOf(s.knownMethods())) {
			int upTo = understoodTo(s, method);
			if (s.layer(method) >= upTo) {
				if (s.progress(method) > 0) s.setMethod(method, s.layer(method), 0);
				continue;
			}
			int before = s.layer(method);
			double amount = c.hasPhysique(Physique.FALLEN_TRUE_IMMORTAL) ? Double.POSITIVE_INFINITY // understood at once, no settling
					: gained * (byStele.contains(method) ? STELE_BONUS : 1.0);
			if (settle(s, method, amount, upTo) <= 0) continue;
			any = true;
			for (int layer = before + 1; layer <= s.layer(method); layer++) settled(player, method, layer);
		}
		if (any) {
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.6f);
			afterLayer(player, s);
		}
		return any;
	}

	/** Layer {@code layer} of {@code method} has settled in: told, with the spells it brings. */
	private static void settled(ServerPlayer player, GhostMethod method, int layer) {
		Component spells = null;
		for (Spell spell : Spell.values()) {
			if (spell.method() != method || spell.layer() != layer) continue;
			spells = spells == null ? spell.getDisplayName().copy() : spells.copy().append(", ").append(spell.getDisplayName());
		}
		if (spells == null) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_LAYER, method.getDisplayName(), layer).withStyle(ChatFormatting.AQUA), true);
		} else {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_LAYER_SPELL, method.getDisplayName(), layer, spells).withStyle(ChatFormatting.AQUA));
		}
	}

	/** The ghost path's factor on the rate: -10% a realm below Nascent Soul, +10% a realm above (not begun: as Qi Refining). */
	public static double realmFactor(PlayerCultivation c) {
		Realm realm = ghostRealm(c.netherworld());
		return realmFactor(realm == null ? Realm.QI_REFINING : realm);
	}

	/** {@link #realmFactor(PlayerCultivation)} of a ghost path in {@code realm}. */
	public static double realmFactor(Realm realm) {
		return 1 + REALM_STEP * (realm.ordinal() - Realm.NASCENT_SOUL.ordinal());
	}

	/**
	 * The bar's gain a second at {@code density} (a Soul Sand Valley's the baseline, the Netherworld's ten times it), every factor but
	 * harmony, pill, stele and a possessor's guidance (which the caller multiplies in).
	 */
	public static double ratePerSecond(PlayerCultivation c, double density) {
		return BASE_RATE * density / GhostQi.SOUL_SAND_VALLEY * realmFactor(c) * (c.netherworld().ghost() ? GHOST_FACTOR : 1.0)
				* c.physiqueGhostFactor() // a Ghost Devil Body's, a Fallen True Immortal's, a Yin Soul's, a Nine Yang's (Physique)
				* (deepestFoundation(c.netherworld()) == null ? NO_METHOD_FACTOR : 1.0)
				* StatPoints.cultivationFactor(c); // Insight, every path's alike
	}

	/**
	 * One of the dead, meditating on ghostly qi with no ghost path yet, begins it (at Qi Refining Early) without a method, as the
	 * living may the heaven path: it is told so, and the path stays its own if it is born again (MeditationManager#ghostMethods).
	 */
	static void begin(ServerPlayer player) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		if (ghostRank(s) >= 0) return;
		s.setPath(0, 0);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_PATH_BEGINS, rankName(0)).withStyle(ChatFormatting.AQUA));
		player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.8f);
		CultivationManager.markDirty(player.server);
		CultivationManager.refresh(player);
		Spells.sync(player); // the Cultivation tab's ghost column (ModPackets#sendMethods carries the path)
	}

	// --- Learning ---

	/**
	 * Whether {@code c} can take up {@code method}: any method, the rarest foundation too (each carries the ghost path from Qi
	 * Refining), but never by a mortal, alive or dead (its meridians are closed, as for the heaven path, until a Marrow Cleansing
	 * Elixir opens them). One's ordinary realm has nothing to do with it.
	 */
	public static boolean canLearn(PlayerCultivation c, GhostMethod method) {
		return !c.isMortal();
	}

	/**
	 * Why {@code c} can't take up {@code method}, or go on with it, now; or null if it can: a mortal's meridians are closed (to a
	 * method known as much as to one new: a mortal's methods lie dormant).
	 */
	public static Component refusal(PlayerCultivation c, GhostMethod method) {
		if (c.isMortal()) return Component.translatable(c.netherworld().ghost() ? ModLang.MSG_METHOD_MERIDIANS_DEAD : ModLang.MSG_METHOD_MERIDIANS);
		return null;
	}

	/**
	 * Teaches {@code player} {@code method} if they can take it up ({@link #canLearn}; nothing if they know it, and told why if a
	 * mortal's closed meridians stand in the way).
	 */
	public static boolean learn(ServerPlayer player, GhostMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.netherworld().knows(method)) return false;
		Component refusal = refusal(c, method);
		if (refusal != null) {
			player.displayClientMessage(refusal, true);
			return false;
		}
		give(player, method);
		return true;
	}

	/**
	 * {@code method}, whatever stands in the way (an op's command), understood at once as far as the ghost path has come
	 * ({@link #understoodTo}); the first foundation begins the path (Qi Refining Early). The first method learned is practised, and a
	 * foundation going further than the foundation practised takes over; its spells go on the wheel as they come.
	 */
	static void give(ServerPlayer player, GhostMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		if (s.knows(method)) return;
		int before = ghostRank(s);
		if (method.isFoundation() && before < 0) s.setPath(0, 0); // the ghost path begins
		// A Ghost Devil Body (Physique) finding a foundation of Core Formation or deeper leaps to its realm's Early.
		if (method.isFoundation() && c.hasPhysique(Physique.GHOST_DEVIL_BODY) && method.realm().ordinal() >= Realm.CORE_FORMATION.ordinal()) {
			int leap = PlayerCultivation.rank(method.realm(), Stage.EARLY);
			if (ghostRank(s) < leap) {
				s.setPath(leap, 0);
				alignTo(s); // the methods already known carry it as far at once
			}
		}
		int layer = Math.max(1, understoodTo(s, method));
		s.setMethod(method, layer, 0);
		GhostMethod active = s.activeMethod();
		if (active == null || method.isFoundation() && active.isFoundation() && method.tier() > active.tier()) s.setActiveMethod(method);
		player.sendSystemMessage((layer > 1 ? Component.translatable(ModLang.MSG_METHOD_LEARNED_AT, method.getDisplayName(), layer)
				: Component.translatable(ModLang.MSG_METHOD_LEARNED, method.getDisplayName())).withStyle(ChatFormatting.AQUA));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
		afterLayer(player, s);
		afterRank(player, before);
		Journal.note(player, "method:" + method.getId(), ModLang.JOURNAL_METHOD, Component.translatable(ModLang.JOURNAL_METHOD_TEXT,
				method.getDisplayName(), method.getGains()));
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
		NetherworldManager.refresh(player);
	}

	/** After layers change: spells newly in reach go on the wheel. */
	static void afterLayer(ServerPlayer player, NetherworldState s) {
		PlayerCultivation c = CultivationManager.get(player);
		for (Spell spell : Spell.values()) {
			if (spell.isKnown(c)) s.addToWheel(spell);
		}
	}

	/**
	 * After the ghost path may have moved, having stood at {@code before}: risen, its cultivator is told (and a ghost risen to Core
	 * Formation is crowned: GhostSystem#watchKing); either way, the body follows the higher path and the harmony the two (TwoPaths).
	 */
	static void afterRank(ServerPlayer player, int before) {
		int now = ghostRank(CultivationManager.get(player).netherworld());
		if (now > before && now >= 0) {
			player.sendSystemMessage(Component.translatable(ModLang.MSG_GHOST_PATH_RISES, rankName(now)).withStyle(ChatFormatting.AQUA));
			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
		}
		if (now != before) CultivationManager.refresh(player);
	}

	// --- What the layers bring ---

	/**
	 * The foundations' depth (each layer a heart more for a ghost, a bigger pool, a longer stay for the living): the deepest layer
	 * of any known ({@link #foundationDepth}), since each spans the realms the ones before it do, and two cultivated as deep are
	 * no deeper than one. None of a mortal's, whose methods lie dormant (its meridians closed, {@link #refusal}).
	 */
	public static int foundationLayers(PlayerCultivation c) {
		if (c.isMortal()) return 0;
		return foundationDepth(c.netherworld());
	}

	/**
	 * Every layer known, for the ghostly qi pool (GhostQi#poolMax): the foundations' depth and the fire's and steps' layers. None of
	 * a mortal's.
	 */
	public static int layersKnown(PlayerCultivation c) {
		if (c.isMortal()) return 0;
		NetherworldState s = c.netherworld();
		int layers = foundationDepth(s);
		for (GhostMethod method : GhostMethod.values()) {
			if (!method.isFoundation()) layers += s.layer(method);
		}
		return layers;
	}

	/**
	 * A ghost's swiftness (Wandering Soul Steps' layers) and blows (Ghost Fire Scripture's), and Yin Wind Steps' speed while it is
	 * on (for the living too): transient modifiers, refreshed every second. Not a mortal's ghost's: its methods lie dormant.
	 */
	static void applyModifiers(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		NetherworldState s = c.netherworld();
		boolean ghost = s.ghost() && !c.isMortal();
		double speed = (ghost ? STEPS_SPEED_PER_LAYER * s.layer(GhostMethod.WANDERING_SOUL_STEPS) : 0)
				+ (c.isAbilityActive(Ability.YIN_WIND_STEPS) ? YIN_WIND_SPEED : 0);
		set(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "Wandering Soul Steps", speed, AttributeModifier.Operation.MULTIPLY_BASE);
		double damage = ghost ? FIRE_DAMAGE_PER_LAYER * s.layer(GhostMethod.GHOST_FIRE_SCRIPTURE) : 0;
		set(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Ghost Fire Scripture", damage, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	private static void set(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID id, String name, double amount,
							AttributeModifier.Operation operation) {
		AttributeInstance instance = p.getAttribute(attribute);
		if (instance == null) return;
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0) {
			if (current != null) instance.removeModifier(id);
			return;
		}
		if (current != null && Math.abs(current.getAmount() - amount) < 1.0e-9) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, amount, operation));
	}

	/** The ghostly qi the switched-on ghost abilities cost a second (see Ability). */
	static double upkeep(PlayerCultivation c) {
		double cost = 0;
		if (c.isAbilityActive(Ability.GHOST_EYE)) cost += GHOST_EYE_COST;
		if (c.isAbilityActive(Ability.NETHERWORLD_VEIL)) cost += VEIL_COST;
		if (c.isAbilityActive(Ability.YIN_WIND_STEPS)) cost += YIN_WIND_COST;
		return cost;
	}

	/** The pool ran dry: every ghost ability switches off. */
	static void exhaust(ServerPlayer p, PlayerCultivation c) {
		boolean any = false;
		for (Ability ability : new Ability[]{Ability.GHOST_EYE, Ability.NETHERWORLD_VEIL, Ability.YIN_WIND_STEPS}) {
			if (c.isAbilityEnabled(ability)) {
				c.setAbilityEnabled(ability, false);
				any = true;
			}
		}
		if (any) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_QI_SPENT), true);
			CultivationManager.markDirty(p.server);
			CultivationManager.sync(p);
		}
	}

	private GhostMethods() {}
}
