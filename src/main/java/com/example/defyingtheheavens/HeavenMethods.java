package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.LightLayer;

/**
 * The heaven path's scriptures (see HeavenMethod): who may take them up, how far they are understood, what they bring, and the heaven
 * path's gathering by Qi Circulation.
 * <p>
 * <b>Method and Art</b> (the user's overhaul, 2026-10-31: "you need a method to cultivate at all"; a method is HOW one cultivates, and
 * each of its volumes lasts for one realm). The heaven path's orthodox method, <b>Qi Circulation</b>, comes in volumes from its own Method
 * Manuals (since 2026-11-16, the user's ask: before, every scripture carried them): without its volume for one's realm, meditation gathers
 * nothing on the heaven path ({@link #hasVolume}, {@link #canGain}); one may still break through into the next realm (only the Laws, past
 * True Immortal, hold a breakthrough back), and gather nothing there until its volume is found. Four Axis has no volume of any method: its
 * axes are everyone's method there (FourAxis#fillBars). Each scripture (HeavenMethod) is arts alone, read from its Art Manual: its
 * element's spells and passive.
 * <p>
 * A scripture known is understood as far as one's cultivation has come ({@link #understoodTo}, HeavenState#layer), at once and always:
 * no layers settle since 2026-11-16 (the user's ask: "get rid of the whole layer system besides laws"). It goes no further than its own
 * realm.
 * <p>
 * <b>The rate.</b> Circulation's gathering is multiplied by the scripture one practises ({@link #practised}, {@link #cultivationFactor}):
 * the element's share of the qi where one sits ({@link #elementFactor}: x{@link #SHARE_FLOOR} where there is none of it, x1 at an even
 * fifth, up to x{@link #FACTOR_CAP} where it runs thick) x {@link #STELE_BONUS} beside a stele of the method practised x a Qi Gathering
 * Pill's and cultivator clothing's boost to gathering; with none practised, the plain pace. The dead can't take them up (their meridians are
 * the ghost path's now), nor a mortal (closed meridians).
 * <p>
 * <b>Learning.</b> An Art Manual (TechniqueManualItem), read, at once; a cultivator who is Warm to you may teach theirs (TalkSystem). The
 * steles teach methods, not scriptures (SteleTeachings).
 * <p>
 * <b>What they bring.</b> The element's spells of every realm it spans (Spell#taughtBy): three in Qi Refining and in Foundation Building
 * (at its Early, Mid and Late), two in each realm deeper (its Mid and Late), each wielded once the realm reaches it; and the element's
 * passive (HeavenPassives), growing with the realm through the scripture's span. Scriptures of one element don't stack; of different
 * elements they do.
 */
public final class HeavenMethods {
	/** The element's share of the qi: x{@code SHARE_FLOOR + SHARE_GAIN x share}, no more than {@code FACTOR_CAP}; an even fifth is x1. */
	public static final double SHARE_FLOOR = 0.75, SHARE_GAIN = 1.25, FACTOR_CAP = 1.5;
	/** A stele of the method practised near (SteleTeachings, which teach methods since 2026-11-16). */
	public static final double STELE_BONUS = SteleTeachings.BONUS;
	public static final double STELE_LEARN_RADIUS = SteleTeachings.LEARN_RADIUS, STELE_BONUS_RADIUS = SteleTeachings.BONUS_RADIUS;
	public static final double STELE_LEARN_SECONDS = SteleTeachings.LEARN_SECONDS;
	/** A cultivator's guidance on a method already known: this many seconds of the learner's plain meditation (TalkSystem). */
	public static final double TEACHING_SECONDS = 10 * 60;

	// --- Who, and how far ---

	/** Whether {@code c} can take up a heaven method: the living, their meridians open. */
	public static boolean canLearn(PlayerCultivation c) {
		return !c.isMortal() && !c.netherworld().ghost();
	}

	/** Why {@code c} can't take up or cultivate a heaven method now, or null if they can: a mortal's closed meridians, or death. */
	public static Component refusal(PlayerCultivation c) {
		if (c.hasShatteredMeridians()) return Component.translatable(ModLang.MSG_PHYSIQUE_SHATTERED);
		if (c.isMortal()) return Component.translatable(ModLang.MSG_HEAVEN_METHOD_MERIDIANS);
		if (c.netherworld().ghost()) return Component.translatable(ModLang.MSG_HEAVEN_METHOD_DEAD);
		return null;
	}

	/** Why {@code c} can't take up {@code method}, or null if they can: {@link #refusal(PlayerCultivation)}, or roots of other elements (Physique#allows). */
	public static Component refusal(PlayerCultivation c, HeavenMethod method) {
		Component refusal = refusal(c);
		if (refusal != null || c.rootsAllow(method.element())) return refusal;
		return Component.translatable(ModLang.MSG_PHYSIQUE_WRONG_ELEMENT, method.element().getDisplayName(), Physiques.rootsLine(c.getRootElements()));
	}

	/** Whether {@code method} is understood to its last layer. */
	public static boolean perfected(HeavenState s, HeavenMethod method) {
		return s.layer(method) >= method.layers();
	}

	// --- How far the scriptures are understood ---

	/**
	 * The layer {@code method} is understood to: the deepest standing for the stage {@code c}'s cultivation stands at, no further than its
	 * last (HeavenState#layer of a cultivator's: since 2026-11-16 nothing settles, the realm alone decides).
	 */
	public static int understoodTo(PlayerCultivation c, HeavenMethod method) {
		return Math.min(method.layers(), MethodLayers.deepestAt(PlayerCultivation.rank(c.getRealm(), c.getStage())));
	}

	/** Whether {@code method}'s layers are still settling in: never, since 2026-11-16 (the user's ask: no layers but the Laws'). */
	public static boolean settling(PlayerCultivation c, HeavenMethod method) {
		return false;
	}

	/** How many arts {@code method} teaches: its element's of every realm it spans (Spell#taughtBy), each wielded as the realm reaches it. */
	public static int artsOf(HeavenMethod method) {
		int count = 0;
		for (Spell spell : Spell.values()) if (spell.taughtBy(method)) count++;
		return count;
	}

	/**
	 * Every method known understood no deeper than the stage {@code rank} stands for (a fall): nothing to do since 2026-11-16, an art's
	 * layers being only how far the realm stands (HeavenState#layer).
	 */
	public static void clampTo(HeavenState s, int rank) {}

	/** Every method known understood exactly as far as the stage {@code rank} stands for: so they always are since 2026-11-16 (HeavenState#layer). */
	public static void alignTo(HeavenState s, int rank) {}

	// --- Qi Circulation's volumes ---

	/**
	 * Whether {@code c} has Qi Circulation's volume for {@code realm}: its manual read (or a stele sat by, a teacher heard) as far as it or
	 * further (PlayerCultivation#methodReach; since 2026-11-16, the user's ask, no longer what the scriptures carry: they are arts now). Four
	 * Axis has none (its axes are the method there); past True Immortal the Laws carry it, so there is nothing to have.
	 */
	public static boolean hasVolume(PlayerCultivation c, Realm realm) {
		if (realm == Realm.FOUR_AXIS) return false;
		if (realm.isPastTrueImmortal()) return true;
		return c.methodReach(CultivationMethod.QI_CIRCULATION) >= realm.ordinal();
	}

	/**
	 * Whether {@code c}'s heaven path can gather where it stands: one of the living, the method it practises holding its volume of the realm
	 * (CultivationMethods#holdsVolume: Qi Circulation's by its manual). How it gathers is the method's own (CultivationMethods#whyNoGain).
	 */
	public static boolean canGain(PlayerCultivation c) {
		return canLearn(c) && CultivationMethods.holdsVolume(c, TwoPaths.Path.HEAVEN);
	}

	// --- How far the scriptures reach ---

	/** The deepest realm {@code s}'s heaven methods reach (of any element), or null with none known. */
	public static Realm deepestRealm(HeavenState s) {
		Realm deepest = null;
		for (HeavenMethod method : s.known()) {
			if (deepest == null || method.realm().ordinal() > deepest.ordinal()) deepest = method.realm();
		}
		return deepest;
	}

	/**
	 * Whether the heaven path's way into {@code realm} is open: up to True Immortal, always (since the overhaul of 2026-10-31 no scripture
	 * holds a breakthrough back: without one, the new realm only gathers nothing, {@link #hasVolume}); the ladder past it, which no
	 * scripture carries, wants the Laws (Laws#opensWay, phase 3): the one breakthrough still held (PlayerCultivation#isHeldByMethod).
	 */
	public static boolean opensWay(PlayerCultivation c, Realm realm) {
		return !realm.isPastTrueImmortal() || Laws.opensWay(c, realm);
	}

	/**
	 * What waits on what, at the heaven path's Grand Perfection: past True Immortal, the Laws a breakthrough wants
	 * (PlayerCultivation#isHeldByMethod); below it nothing holds the breakthrough, and this says the realm ahead will gather nothing
	 * until a scripture of it is found (Four Axis: its axes fill it).
	 */
	public static Component gateLine(PlayerCultivation c) {
		Realm ahead = c.getRealm().next();
		if (ahead.isPastTrueImmortal()) return Laws.gateLine(c, ahead);
		if (ahead == Realm.FOUR_AXIS) return Component.translatable(ModLang.METHOD_GATE_AXES);
		return Component.translatable(ModLang.HEAVEN_GATE_NONE, ahead.getDisplayName());
	}

	// --- The rate ---

	/**
	 * The method meditation cultivates by: the one practised, else (none chosen) the one that goes furthest; null with none known.
	 */
	public static HeavenMethod practised(HeavenState s) {
		HeavenMethod active = s.activeMethod();
		if (active != null) return active;
		HeavenMethod best = null;
		for (HeavenMethod method : s.known()) {
			if (best == null || method.layers() > best.layers() || method.layers() == best.layers() && s.layer(method) > s.layer(best)) best = method;
		}
		return best;
	}

	/** The element's factor on the rate where {@code share} of the qi is of it. */
	public static double elementFactor(double share) {
		return Math.min(FACTOR_CAP, SHARE_FLOOR + SHARE_GAIN * Math.max(0, Math.min(1, share)));
	}

	/** What share of the qi where {@code p} sits is {@code element} (QiElement#share); in the Inner Realm, the body's place. */
	public static double shareAt(ServerPlayer p, QiElement element) {
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p));
		if (level == null) level = p.serverLevel();
		BlockPos pos = InnerRealm.bodyPos(p);
		return QiElement.share(level.getBiome(pos), underground(level, pos), element);
	}

	/** No sky above, in a realm that has one (as Qi Sense reads it): the qi leans to earth and metal. */
	static boolean underground(ServerLevel level, BlockPos pos) {
		return level.dimensionType().hasSkyLight() && !level.dimensionType().hasCeiling() && pos.getY() < level.getSeaLevel()
				&& level.getBrightness(LightLayer.SKY, pos) < level.getMaxLightLevel();
	}

	/**
	 * What a heaven scripture of an element with {@code share} of the qi here makes of meditation's cultivation: the element's factor,
	 * {@link #STELE_BONUS} beside a stele of the method practised, and a Qi Gathering Pill's and cultivator clothing's boosts to
	 * gathering.
	 */
	public static double methodFactor(PlayerCultivation c, double share, boolean byStele) {
		return elementFactor(share) * (byStele ? STELE_BONUS : 1.0) * boosts(c);
	}

	/**
	 * What Qi Circulation makes of meditation's cultivation, attuned to {@code method}'s Art ({@link #practised}), where {@code share} of
	 * the qi is its element's: {@link #methodFactor}; with no art to follow, the plain pace (x1) with a Qi Gathering Pill's and clothing's
	 * boosts, and a stele's of the method (whether it gathers at all is Qi Circulation's volume's: MeditationManager, CultivationMethods#whyNoGain).
	 */
	public static double cultivationFactor(PlayerCultivation c, HeavenMethod method, double share, boolean byStele) {
		return method == null ? (byStele ? STELE_BONUS : 1.0) * boosts(c) : methodFactor(c, share, byStele);
	}

	/** A Qi Gathering Pill's and cultivator clothing's boosts to gathering. */
	private static double boosts(PlayerCultivation c) {
		return (1 + c.getQiBoost()) * (1 + c.getGearQiBonus());
	}

	/** Cultivation a cultivator's guidance gives {@code c}: {@link #TEACHING_SECONDS} of their plain meditation (their stage's own rate). */
	public static double teachingProgress(PlayerCultivation c) {
		return c.cultivationPerSecond() * TEACHING_SECONDS;
	}

	// --- Settling in ---


	// --- An element's methods ---

	/** The method of {@code element} {@code s} has understood deepest (of two as deep, the one that goes further), or null. */
	public static HeavenMethod deepestOf(HeavenState s, QiElement element) {
		HeavenMethod deepest = null;
		for (HeavenMethod method : s.known()) {
			if (method.element() != element) continue;
			if (deepest == null || s.layer(method) > s.layer(deepest) || s.layer(method) == s.layer(deepest) && method.tier() > deepest.tier()) {
				deepest = method;
			}
		}
		return deepest;
	}

	/** The deepest layer of {@code element}'s methods {@code s} knows: 0 for none. */
	public static int depth(HeavenState s, QiElement element) {
		HeavenMethod deepest = deepestOf(s, element);
		return deepest == null ? 0 : s.layer(deepest);
	}

	// --- Learning ---

	/** Teaches {@code player} {@code method} if they can take it up (nothing if they know it; told why if they can't). */
	public static boolean learn(ServerPlayer player, HeavenMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.heaven().knows(method)) return false;
		Component refusal = refusal(c, method);
		if (refusal != null) {
			player.displayClientMessage(refusal, true);
			return false;
		}
		give(player, method);
		return true;
	}

	/**
	 * {@code method}, whatever stands in the way (an op's command), understood at once as far as the player's cultivation has come
	 * ({@link #understoodTo}): the first learned is practised, and one going further than the practised method of its element takes
	 * over.
	 */
	static void give(ServerPlayer player, HeavenMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		if (s.knows(method)) return;
		s.setMethod(method, 1, 0); // its layers are as far as the realm stands, at once (HeavenState#layer): no layers to settle since 2026-11-16
		HeavenMethod active = s.activeMethod();
		if (active == null || active.element() == method.element() && method.tier() > active.tier()) s.setActiveMethod(method);
		// Its arts only, every one of them at once, each wielded as the realm reaches it (Spell#isKnown): Qi Circulation's volumes are in its own
		// manual since 2026-11-16, so an art carries none.
		player.sendSystemMessage(Component.translatable(ModLang.MSG_ART_LEARNED, method.getDisplayName(), artsOf(method)).withStyle(ChatFormatting.GOLD));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.1f);
		Journal.note(player, "heaven_method:" + method.getId(), ModLang.JOURNAL_HEAVEN_METHOD, Component.translatable(ModLang.JOURNAL_HEAVEN_METHOD_TEXT,
				method.getDisplayName(), method.element().getDisplayName(), method.getGains()));
		afterLayer(player);
	}

	/** After heaven layers change: spells newly in reach go on the wheel, the passives are refreshed, the client told. */
	static void afterLayer(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		for (Spell spell : Spell.values()) {
			if (spell.isHeaven() && spell.isKnown(c)) s.addToWheel(spell);
		}
		HeavenPassives.apply(player);
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
	}

	/** The heaven spells each player knew at the last look (not saved). */
	private static final java.util.Map<java.util.UUID, java.util.EnumSet<Spell>> KNOWN_SPELLS = new java.util.HashMap<>();

	/**
	 * Once a second (NetherworldManager): heaven spells that have just come into reach go on the spell wheel, once. A spell wants its
	 * layer understood and the realm and stage that layer stands for (Spell#isKnown). The first look after joining notes what is
	 * known, adding nothing, so a spell taken off the wheel stays off.
	 */
	static void watchSpells(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		java.util.EnumSet<Spell> now = java.util.EnumSet.noneOf(Spell.class);
		// The ghost path's arts too (since 2026-11-16 an art's spells open with the path's stages, no layers settling to tell of them).
		for (Spell spell : Spell.values()) if (!spell.isDual() && spell.isKnown(c)) now.add(spell);
		java.util.EnumSet<Spell> before = KNOWN_SPELLS.put(p.getUUID(), now);
		if (before == null) return;
		Component opened = null;
		for (Spell spell : now) {
			if (before.contains(spell)) continue;
			c.netherworld().addToWheel(spell);
			opened = opened == null ? spell.getDisplayName().copy() : opened.copy().append(", ").append(spell.getDisplayName());
		}
		if (opened == null) return;
		// Told in the chat, as a layer's spells were: the realm reached opens the arts known that wait on it.
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ARTS_OPENED, opened).withStyle(ChatFormatting.GOLD));
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
	}

	/** A player left. */
	public static void forget(java.util.UUID id) {
		KNOWN_SPELLS.remove(id);
	}

	/** Makes {@code method} the heaven method meditation cultivates by. */
	public static void practise(ServerPlayer p, HeavenMethod method) {
		HeavenState s = CultivationManager.get(p).heaven();
		if (method == null || !s.knows(method)) return;
		s.setActiveMethod(method);
		CultivationManager.markDirty(p.server);
		CultivationManager.sync(p); // the Cultivation tab's bar takes the method's colour
		p.displayClientMessage(Component.translatable(ModLang.MSG_HEAVEN_PRACTISED, method.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		Spells.sync(p);
	}

	private HeavenMethods() {}
}
