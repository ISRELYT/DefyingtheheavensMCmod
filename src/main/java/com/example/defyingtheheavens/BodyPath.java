package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Body Path (the user's ask of 2026-11-01: "lets begin to expand on body cultivation ... to make it an actually viable method (and you
 * dont need shattered meridians to practice it either)"; their calls: a separate, fourth path; its first phase its realms and the body's
 * strength alone, no techniques yet; its realms their own names): the body tempered beside the qi paths, or alone (Shattered Meridians walk
 * nothing else).
 * <ul>
 *   <li><b>Its realms</b> stand beside the qi realms rank for rank (PlayerCultivation#rank, so the body is weighed against the other paths),
 *   each with its own name ({@link #realmName}: Copper Skin beside Qi Refining up to the Indestructible Body beside True Immortal, and on up
 *   the ladder to the Body of Heaven and Earth beside Heavenly Lord), four stages each; its bar the heaven path's own
 *   (PlayerCultivation#required).</li>
 *   <li><b>Begun</b> ({@link #begin}) by any of the living whose mortal body has reached Peak or gone past it (the Cultivation tab's Begin
 *   button); Shattered Meridians' body refining is the Body Path's now (PlayerCultivation#load), and their Peak begins it by itself.</li>
 *   <li><b>Tempered</b> while it is drawn in (the Cultivation tab's switch: TwoPaths#cultivatesBody) by Body Tempering, its own method
 *   (CultivationMethod#BODY_TEMPERING): meditation circulating the blood at the stage's own pace (MeditationManager), and deeds ({@link #deed}:
 *   charged blows, harm taken, the slain, stone and ore mined, sprinting), each worth some seconds of it.</li>
 *   <li><b>Its gates</b> are the Demon Path's: a tribulation at every Grand Perfection and at every stage from Jade Bone, its Nascent Soul's
 *   (TribulationManager#startBody); the lower realms' cap; the axes of virtue at Golden Blood, its Four Axis (FourAxis#fillBars); past the
 *   Indestructible Body, the Laws ({@link #heldByLaws}) and the Immortal Realm, as on every path (the user's "Go, all three parts",
 *   2026-11-01). An Indestructible Body or past it is an immortal to the Laws and the Five Decays ({@link #immortal}).</li>
 *   <li><b>What it gives</b>: the body, where it is the mightiest of the paths (TwoPaths#body), at {@link #BODY} times its rank's might; no qi,
 *   no abilities, no arts. Its realms give their years (Lifespan#spanRank).</li>
 * </ul>
 */
public final class BodyPath {
	/** The body the Body Path gives, against its rank's (TwoPaths#body): the mightiest of the paths' for its rank, as it has no qi. */
	public static final double BODY = 1.5;
	/** Its last rank: Heavenly Lord's Grand Perfection, as every path's (its ladder past True Immortal: the user's "Go, all three parts", 2026-11-01). */
	public static final int LAST_RANK = PlayerCultivation.LAST_RANK;
	/** Its first immortal rank: the Indestructible Body, beside True Immortal ({@link #immortal}). */
	public static final int IMMORTAL_RANK = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
	/** Its realms' names, by the qi realm each stands beside (the first eight Shattered Meridians' body refining's; the last six the ladder's). */
	private static final String[] NAMES = {"copper_skin", "iron_flesh", "steel_sinew", "jade_bone", "silver_marrow", "golden_blood",
			"diamond_heart", "star_forged_body", "heaven_bone_body", "undying_body", "indestructible_body", "earth_bearing_body", "sky_pillar_body",
			"imperishable_gold_body", "primordial_saint_body", "chaos_tempered_body", "body_of_heaven_and_earth"};

	/** Deeds, each worth these seconds of the stage's own meditation ({@link #deed}). */
	public static final double CHARGED_HIT = 0.5, HOSTILE_KILL = 5, OTHER_KILL = 1, STONE_MINED = 0.2, ORE_MINED = 3, SPRINT_PER_SECOND = 0.1;
	/** Harm taken: this many seconds for the whole of one's health. */
	public static final double WHOLE_HEALTH_TAKEN = 20;
	/** A slain cultivator's or beast's kill is worth twice as much for each realm it stood above the Body Path, half for each below, within this. */
	public static final double MOST_KILL_FACTOR = 8;

	/** Small gains reach the client at most this often (ticks); a new stage syncs at once. */
	private static final int SYNC_INTERVAL = 20;
	private static final Map<UUID, Long> LAST_SYNC = new HashMap<>();

	// --- Where it stands ---

	/** The Body Path's rank (PlayerCultivation#rank), or -1: not begun. */
	public static int rank(PlayerCultivation c) {
		return c.body().rank();
	}

	public static Realm realm(int rank) {
		return Realm.byIndex(Math.min(Realm.values().length - 1, Math.max(0, rank) / Stage.values().length));
	}

	public static Stage stage(int rank) {
		return Stage.byIndex(Math.max(0, rank) % Stage.values().length);
	}

	/** The body realm beside {@code realm}: "Copper Skin" beside Qi Refining, and so on. */
	public static Component realmName(Realm realm) {
		return Component.translatable(ModLang.bodyRefiningKey(NAMES[Math.max(0, Math.min(NAMES.length - 1, realm.ordinal()))]));
	}

	/** "Copper Skin - Early". */
	public static Component rankName(int rank) {
		return Component.empty().append(realmName(realm(rank))).append(" - ").append(stage(rank).getDisplayName());
	}

	/** The bar through the stage {@code rank} stands for: the heaven path's own (PlayerCultivation#required). 0 for none. */
	public static double required(int rank) {
		return rank < 0 ? 0 : PlayerCultivation.required(realm(rank), stage(rank));
	}

	/** How far through its stage, 0 to 1 (the Cultivation tab's bar). */
	public static double stageProgress(PlayerCultivation c) {
		double required = required(rank(c));
		return required <= 0 ? 0 : Math.min(1, c.body().cultivation() / required);
	}

	/** Leaving the stage {@code rank} stands for takes a tribulation: Grand Perfection always, every stage from Jade Bone (Nascent Soul's). */
	public static boolean needsTribulation(int rank) {
		return stage(rank).isLast() || realm(rank).hasStageTribulations();
	}

	public static boolean isMaxed(PlayerCultivation c) {
		return rank(c) >= LAST_RANK;
	}

	/** The bar full at a stage only a tribulation leaves (or at the very last). */
	public static boolean atBottleneck(PlayerCultivation c) {
		int rank = rank(c);
		return rank >= 0 && (needsTribulation(rank) || rank >= LAST_RANK) && c.body().cultivation() >= required(rank);
	}

	/**
	 * Held at the lower realms' cap, as the other paths are (PlayerCultivation#isBreakthroughLocked: the Upper Realm wanted), or into True
	 * Immortal outside the Immortal Realm (PlayerCultivation#isImmortalRealmLocked). Why: PlayerCultivation#lockLine.
	 */
	public static boolean locked(PlayerCultivation c) {
		return !isMaxed(c) && (c.isLowerRealmBound()
				&& rank(c) + 1 > PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)
				|| rank(c) >= 0 && c.isImmortalRealmLocked(rank(c) + 1));
	}

	/** At a Golden Blood (Four Axis) stage's bottleneck with its axis of virtue not complete (FourAxis: every path's). */
	public static boolean heldByAxis(PlayerCultivation c) {
		return atBottleneck(c) && FourAxis.holds(c, rank(c));
	}

	/**
	 * At a realm's Grand Perfection with the realm ahead one of the ladder past the Indestructible Body and too few of the Laws held for it
	 * (Laws#opensWay: 3, 7, 12, 18, 25, 35 levels together), as on every path: no tempering carries the body past True Immortal's realm alone.
	 */
	public static boolean heldByLaws(PlayerCultivation c) {
		int rank = rank(c);
		return atBottleneck(c) && stage(rank).isLast() && !isMaxed(c) && !Laws.opensWay(c, realm(rank + 1));
	}

	/** Why its breakthrough waits on the Laws ({@link #heldByLaws}), the realm ahead named the body's way. */
	public static Component lawGateLine(PlayerCultivation c) {
		Realm ahead = realm(rank(c) + 1);
		return Laws.gateLine(c, ahead, realmName(ahead));
	}

	/** Ready for its tribulation. */
	public static boolean canBreakthrough(PlayerCultivation c) {
		return atBottleneck(c) && !isMaxed(c) && !locked(c) && !heldByAxis(c) && !heldByLaws(c);
	}

	/**
	 * Whether {@code c}'s body is an immortal's (the Indestructible Body or past it, one of the living): the Laws are comprehended by it (Laws#isImmortal)
	 * and the Five Decays come for it (FiveDecays#decays). It has no immortal essence, so no Law domain (Laws#isQiImmortal).
	 */
	public static boolean immortal(PlayerCultivation c) {
		return !c.netherworld().ghost() && rank(c) >= IMMORTAL_RANK;
	}

	/** Whether {@code c}'s Body Path can be tempered where it stands: begun, its method holding its volume (Body Tempering: the body is its own). */
	public static boolean canGain(PlayerCultivation c) {
		return rank(c) >= 0 && CultivationMethods.holdsVolume(c, TwoPaths.Path.BODY);
	}

	/** Whether {@code c} may begin the Body Path: one of the living, its mortal body at Peak or past it (a cultivator's is). */
	public static boolean available(PlayerCultivation c) {
		return !c.netherworld().ghost() && (!c.isMortal() || c.getMortalStage().isLast());
	}

	// --- Tempering it ---

	/**
	 * What a second's tempering gathers before a meditation's own boosts (MeditationManager multiplies those in): its realm's and stage's own
	 * rate, as the heaven path's (PlayerCultivation#meditationCultivationPerSecond), the place's density with it (its realm's qi at home,
	 * PlayerCultivation#placeFactor), an Ancient Holy Body's twice (Tempering#HOLY_TEMPERING). Nothing while the land holds the cultivator
	 * below its realm (PlayerCultivation#sustained).
	 */
	public static double perSecond(PlayerCultivation c, double bonus) {
		int rank = rank(c);
		if (rank < 0 || c.sustained(rank) < rank) return 0;
		double rate = realm(rank).getCultivationPerSecond() * stage(rank).getPowerMultiplier() + bonus;
		return rate * c.placeFactor() * c.favourFactor() * StatPoints.cultivationFactor(c)
				* (c.hasPhysique(Physique.ANCIENT_HOLY_BODY) ? Tempering.HOLY_TEMPERING : 1.0);
	}

	/** Adds {@code amount} to the bar; its stages step on by themselves to a bottleneck, where it stops full. @return the stages gained */
	public static int addCultivation(PlayerCultivation c, double amount) {
		int rank = rank(c);
		if (rank < 0 || amount <= 0) return 0;
		double cultivation = c.body().cultivation() + amount;
		int gained = 0;
		while (cultivation >= required(rank)) {
			if (needsTribulation(rank) || rank >= LAST_RANK) {
				cultivation = required(rank);
				break;
			}
			cultivation -= required(rank);
			rank++;
			gained++;
		}
		c.body().set(rank, cultivation);
		return gained;
	}

	/** Its tribulation survived: on to the next stage, its bar empty. */
	public static boolean breakthrough(PlayerCultivation c) {
		int rank = rank(c);
		if (rank < 0 || rank >= LAST_RANK) return false;
		c.body().set(rank + 1, 0);
		return true;
	}

	/** Its tribulation failed: a stage down, its tempering there lost. */
	public static void fallOneStage(PlayerCultivation c) {
		int rank = rank(c);
		if (rank < 0) return;
		c.body().set(Math.max(0, rank - 1), 0);
	}

	/**
	 * {@code p} begins the Body Path (the Cultivation tab's button, C2S begin_body_path): at Copper Skin Early, drawn in (meditation and deeds
	 * temper it from now). Refused, told, for the dead and for a mortal body short of Peak. @return whether it began
	 */
	public static boolean begin(ServerPlayer p) {
		// Not begun in a body by the soul in it (Possession): the path is its owner's to begin (CultivationMethods#practise).
		if (Possession.inhabits(p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return false;
		}
		PlayerCultivation c = CultivationManager.get(p);
		if (rank(c) >= 0) return false;
		if (!available(c)) {
			p.displayClientMessage(Component.translatable(c.netherworld().ghost() ? ModLang.MSG_GHOST_CANNOT : ModLang.MSG_BODY_NOT_PEAK).withStyle(ChatFormatting.GRAY), true);
			return false;
		}
		c.body().set(0, 0);
		drawIn(c);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_BODY_BEGUN, rankName(0)).withStyle(ChatFormatting.GOLD));
		p.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7f, 0.8f);
		CultivationManager.markDirty(p.server);
		Spells.sync(p); // the Cultivation tab's switch and columns (ModPackets#sendMethods carries it)
		CultivationManager.refresh(p);
		return true;
	}

	/** {@code c}'s Body Path drawn in: meditation and deeds temper it, the other paths rest (the switch's, TwoPaths#switchPath). */
	static void drawIn(PlayerCultivation c) {
		c.netherworld().setCultivatingGhost(false);
		c.demon().setCultivating(false);
		c.body().setCultivating(true);
	}

	/**
	 * One with Shattered Meridians whose mortal body has reached Peak, its Body Path not begun (a world from before it, or Peak just reached):
	 * begun, drawn in, as their body refining went on by itself before. Joining, and at a new mortal stage (Tempering#gain).
	 */
	public static void shatteredBegin(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (c.hasShatteredMeridians() && c.isMortal() && c.getMortalStage().isLast() && rank(c) < 0 && available(c)) begin(p);
	}

	/**
	 * A deed of {@code p}'s worth {@code seconds} of its Body Path's own tempering (charged blows, harm taken, the slain, stone and ore, a
	 * sprint: Tempering), while it is drawn in by Body Tempering (the deeds are its way: another body method practised, they temper nothing)
	 * and can be tempered, not held at a bottleneck: tempered, its years paid as meditation's are (Lifespan#seclude, by Body Tempering's
	 * whole weight), told of any stage won or bottleneck come to.
	 */
	public static void deed(ServerPlayer p, double seconds) {
		if (seconds <= 0 || p.isSpectator()) return;
		PlayerCultivation c = CultivationManager.get(p);
		if (!practises(c, CultivationMethod.BODY_TEMPERING) || !canGain(c) || atBottleneck(c)) return;
		int before = rank(c);
		double was = c.body().cultivation(), gained = perSecond(c, 0) * seconds;
		if (gained <= 0) return;
		int stages = addCultivation(c, gained);
		Lifespan.seclude(p, before, was, rank(c), c.body().cultivation(), BodyPath::required,
				Lifespan.saving(1, 1, Lifespan.placeSaving(c, realm(before)), 1), 1.0, CultivationMethod.BODY_TEMPERING);
		if (stages > 0) {
			p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
			afterRank(p, before);
		} else {
			CultivationManager.markDirty(p.server);
			long now = p.server.getTickCount();
			Long last = LAST_SYNC.get(p.getUUID());
			if (last == null || now - last >= SYNC_INTERVAL) {
				CultivationManager.sync(p);
				LAST_SYNC.put(p.getUUID(), now);
			}
		}
		if (atBottleneck(c)) tellBottleneck(p, c);
	}

	/** {@code fraction} of a stage said in hundredths: to a hundredth under a tenth ("0.04"), a tenth under one ("0.4"), whole above ("3"). */
	public static String percent(double fraction) {
		double percent = fraction * 100;
		return percent < 0.095 ? String.format(java.util.Locale.ROOT, "%.2f", percent)
				: percent < 0.95 ? String.format(java.util.Locale.ROOT, "%.1f", percent) : String.valueOf(Math.round(percent));
	}

	/** Whether {@code c} tempers its Body Path by {@code method} now: drawn in (TwoPaths#cultivatesBody), that method the one practised. */
	public static boolean practises(PlayerCultivation c, CultivationMethod method) {
		return TwoPaths.cultivatesBody(c) && CultivationMethods.practised(c, TwoPaths.Path.BODY) == method;
	}

	/**
	 * {@code seconds} of the Body Path's own pace ({@link #perSecond}) won by {@code method} (the body's other ways: MedicinalBath, BeastBlood,
	 * WeightedTraining, HundredBattles), while it is drawn in by that method and not held at a bottleneck: onto the bar by
	 * CultivationMethods#grant (its years the method's own, its volume needed), untold (each way says its own), told of a bottleneck come to.
	 * @return the share of the stage won
	 */
	public static double temperBy(ServerPlayer p, CultivationMethod method, double seconds) {
		PlayerCultivation c = CultivationManager.get(p);
		if (seconds <= 0 || p.isSpectator() || !practises(c, method) || atBottleneck(c)) return 0;
		double required = required(rank(c));
		double fraction = required <= 0 ? 0 : perSecond(c, 0) * seconds / required;
		if (fraction <= 0) return 0;
		double won = CultivationMethods.grant(p, TwoPaths.Path.BODY, method, fraction, false);
		if (won <= 0) return 0;
		if (atBottleneck(c)) tellBottleneck(p, c);
		return fraction;
	}

	/** A stage or more gained ({@code before} the rank it stood at): told, the body's strength anew. */
	public static void afterRank(ServerPlayer p, int before) {
		PlayerCultivation c = CultivationManager.get(p);
		p.displayClientMessage(Component.translatable(ModLang.MSG_BODY_STAGE_UP, rankName(rank(c))).withStyle(ChatFormatting.GOLD), true);
		CultivationManager.markDirty(p.server);
		CultivationManager.refresh(p);
		LAST_SYNC.put(p.getUUID(), (long) p.server.getTickCount());
	}

	/** Its bar full where only a breakthrough leaves it: why it waits (the lower realms' cap, an axis of virtue, the Laws), or that its tribulation calls. */
	static void tellBottleneck(ServerPlayer p, PlayerCultivation c) {
		if (isMaxed(c)) return;
		p.sendSystemMessage(heldByAxis(c) ? FourAxis.gateLine(c, rank(c)).copy().withStyle(ChatFormatting.GOLD)
				: heldByLaws(c) ? lawGateLine(c).copy().withStyle(ChatFormatting.GOLD)
				: locked(c) ? lockLine(c)
				: Component.translatable(ModLang.MSG_BODY_BOTTLENECK, rankName(rank(c) + 1)).withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Why its next breakthrough can't be made here ({@link #locked}), the body's realms named its way: only the Immortal Realm holds one into
	 * the Indestructible Body or past it; past the lower realms' cap, only the Jade Firmament and the realms above it (PlayerCultivation#lockLine).
	 */
	public static Component lockLine(PlayerCultivation c) {
		int target = rank(c) + 1;
		if (c.isImmortalRealmLocked(target)) return Component.translatable(ModLang.MSG_IMMORTAL_REALM_LOCKED, rankName(target));
		return Component.translatable(ModLang.MSG_REALM_LOCKED,
				rankName(PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE)));
	}

	/** A slain {@code rank}'s worth to a Body Path standing at {@code body} (-1 for neither known): x2 a realm above, /2 below, within the bounds. */
	public static double killFactor(int slainRank, int bodyRank) {
		if (slainRank < 0 || bodyRank < 0) return 1;
		int stages = Stage.values().length;
		double factor = Math.pow(2, slainRank / stages - bodyRank / stages);
		return Math.max(1 / MOST_KILL_FACTOR, Math.min(MOST_KILL_FACTOR, factor));
	}

	public static void forget(UUID id) {
		LAST_SYNC.remove(id);
	}

	private BodyPath() {}
}
