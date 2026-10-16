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
 * Cultivating the heaven methods (see HeavenMethod): who may, how they speed cultivation, how far they are understood, and what the
 * layers bring.
 * <p>
 * <b>Cultivation is its own; the methods ride on it.</b> The heaven methods are the living's techniques, cultivated on the five
 * elements' qi: a method of a realm spans Qi Refining up to it, nine layers a realm (MethodLayers). One's cultivation is the bar
 * (PlayerCultivation#addCultivation): meditation fills it and the stages step on by themselves, a method or none. A method known is
 * understood as far as one's cultivation has come ({@link #understoodTo}): learned, at once; and each time the realm reaches a new
 * stage, the layers standing for it settle in over meditation ({@link #settle}: half the stage's own cultivation, gathered by the
 * method's element), each told as it does. A method goes no further than its own realm.
 * <p>
 * <b>The gate.</b> The methods hold back no minor stage; only a major breakthrough waits on one: breaking into a realm wants a heaven
 * method of that realm or deeper, of any element ({@link #opensWay}). Qi Refining wants none.
 * <p>
 * <b>The rate.</b> Meditation's cultivation is multiplied by the method practised ({@link #cultivationFactor}): the element's share of
 * the qi where one sits ({@link #elementFactor}: x{@link #SHARE_FLOOR} where there is none of it, x1 at an even fifth, up to
 * x{@link #FACTOR_CAP} where it runs thick) x {@link #STELE_BONUS} beside a Dao Stele of its element x a Qi Gathering Pill's and
 * cultivator clothing's boost to gathering. With no method practised, x{@link #NO_METHOD_FACTOR}: a cultivator without a technique.
 * The dead can't cultivate them (their meridians are the ghost path's now), nor a mortal (closed meridians).
 * <p>
 * <b>Learning.</b> A Dao Stele (DaoSteleBlock) teaches its method to one who meditates beside it for {@link #STELE_LEARN_SECONDS};
 * a Jade Slip, read, at once; a cultivator who is Warm to you may teach theirs (TalkSystem).
 * <p>
 * <b>What the layers bring.</b> The element's spells of every realm it spans (Spell#taughtBy): three in Qi Refining and in
 * Foundation Building (at the second, fifth and eighth layers of the realm), two in each realm deeper (the third and seventh); and
 * every layer of the element's deepest method strengthens the element's passive (HeavenPassives), by the realm the layer stands
 * in. Methods of one element don't stack; of different elements they do.
 */
public final class HeavenMethods {
	/** The element's share of the qi: x{@code SHARE_FLOOR + SHARE_GAIN x share}, no more than {@code FACTOR_CAP}; an even fifth is x1. */
	public static final double SHARE_FLOOR = 0.75, SHARE_GAIN = 1.25, FACTOR_CAP = 1.5;
	/** Meditation with no heaven method practised. */
	public static final double NO_METHOD_FACTOR = 0.75;
	public static final double STELE_BONUS = 1.25;
	public static final double STELE_LEARN_RADIUS = 4, STELE_BONUS_RADIUS = 8;
	public static final double STELE_LEARN_SECONDS = 15;
	/** A new stage's layers settle in over this share of the stage's own cultivation, gathered by the method's element. */
	public static final double SETTLE_SHARE = 0.5;
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

	/**
	 * Cultivation through layer {@code layer} (PlayerCultivation's own units): the layers standing for a stage share out its
	 * requirement (PlayerCultivation#required) by their places' minutes (MethodLayers#MINUTES): Qi Refining Early's two 43 and 57 of
	 * its 100, Grand Perfection's one all of it.
	 */
	public static double layerCost(int layer) {
		int at = Math.max(1, layer);
		int within = MethodLayers.within(at);
		Stage stage = MethodLayers.stageAt(within);
		double minutes = 0;
		for (int w = 1; w <= MethodLayers.PER_REALM; w++) if (MethodLayers.stageAt(w) == stage) minutes += MethodLayers.MINUTES[w - 1];
		return PlayerCultivation.required(MethodLayers.realmOf(at), stage) * MethodLayers.MINUTES[within - 1] / minutes;
	}

	/** What layer {@code layer} takes to settle in (meditation's cultivation by its element: {@link #settle}): {@link #SETTLE_SHARE} of its cost. */
	public static double settleCost(int layer) {
		return SETTLE_SHARE * layerCost(layer);
	}

	// --- How far the methods are understood ---

	/**
	 * The layer {@code method} is understood to once settled: the deepest standing for the stage {@code c}'s cultivation stands at,
	 * no further than its last.
	 */
	public static int understoodTo(PlayerCultivation c, HeavenMethod method) {
		return Math.min(method.layers(), MethodLayers.deepestAt(PlayerCultivation.rank(c.getRealm(), c.getStage())));
	}

	/** Whether {@code method}'s layers are still settling in, behind {@code c}'s cultivation. */
	public static boolean settling(PlayerCultivation c, HeavenMethod method) {
		HeavenState s = c.heaven();
		return s.knows(method) && s.layer(method) < understoodTo(c, method);
	}

	/**
	 * Adds {@code amount} of settling to {@code method} (meditation's cultivation, by its element), layer by layer, up to layer
	 * {@code upTo}; with nothing left to settle there, none is kept. Never lowers a layer. Returns how many layers settled.
	 */
	public static int settle(HeavenState s, HeavenMethod method, double amount, int upTo) {
		int layer = s.layer(method);
		if (layer <= 0) return 0;
		if (layer >= upTo) {
			if (s.progress(method) > 0) s.setMethod(method, layer, 0);
			return 0;
		}
		if (amount <= 0) return 0;
		double progress = s.progress(method) + amount;
		int gained = 0;
		while (layer < upTo && progress >= settleCost(layer + 1)) {
			progress -= settleCost(layer + 1);
			layer++;
			gained++;
		}
		s.setMethod(method, layer, layer >= upTo ? 0 : progress);
		return gained;
	}

	/** Every method known understood no deeper than the stage {@code rank} stands for (a fall: a tribulation failed, the nascent soul lost). */
	public static void clampTo(HeavenState s, int rank) {
		int deepest = Math.max(1, MethodLayers.deepestAt(rank));
		for (HeavenMethod method : java.util.List.copyOf(s.known())) {
			if (s.layer(method) > deepest) s.setMethod(method, deepest, 0);
		}
	}

	/**
	 * Every method known understood exactly as far as the stage {@code rank} stands for, nothing settling (an op setting the realm, an
	 * old save): those deeper set back, those behind brought up.
	 */
	public static void alignTo(HeavenState s, int rank) {
		int deepest = Math.max(1, MethodLayers.deepestAt(rank));
		for (HeavenMethod method : java.util.List.copyOf(s.known())) s.setMethod(method, Math.min(method.layers(), deepest), 0);
	}

	// --- The gate: major breakthroughs ---

	/** The deepest realm {@code s}'s heaven methods reach (of any element), or null with none known. */
	public static Realm deepestRealm(HeavenState s) {
		Realm deepest = null;
		for (HeavenMethod method : s.known()) {
			if (deepest == null || method.realm().ordinal() > deepest.ordinal()) deepest = method.realm();
		}
		return deepest;
	}

	/** Whether {@code c}'s heaven methods open the way into {@code realm}: one of it or deeper known. Qi Refining wants none. */
	public static boolean opensWay(PlayerCultivation c, Realm realm) {
		if (realm == Realm.QI_REFINING) return true;
		Realm deepest = deepestRealm(c.heaven());
		return deepest != null && deepest.ordinal() >= realm.ordinal();
	}

	/** Why {@code c}'s breakthrough waits on a heaven method (PlayerCultivation#isHeldByMethod): one of the realm ahead is wanted. */
	public static Component gateLine(PlayerCultivation c) {
		return Component.translatable(ModLang.HEAVEN_GATE_NONE, c.getRealm().isLast() ? c.getRealm().getDisplayName() : c.getRealm().next().getDisplayName());
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
	 * What a heaven method of an element with {@code share} of the qi here makes of meditation's cultivation (and how fast its layers
	 * settle): the element's factor, {@link #STELE_BONUS} by its Dao Stele, and a Qi Gathering Pill's and cultivator clothing's boosts
	 * to gathering.
	 */
	public static double methodFactor(PlayerCultivation c, double share, boolean byStele) {
		return elementFactor(share) * (byStele ? STELE_BONUS : 1.0) * boosts(c);
	}

	/**
	 * What meditation's cultivation is multiplied by with {@code method} practised ({@link #practised}), where {@code share} of the qi
	 * is its element's: {@link #methodFactor}; with none, {@link #NO_METHOD_FACTOR} and the boosts to gathering.
	 */
	public static double cultivationFactor(PlayerCultivation c, HeavenMethod method, double share, boolean byStele) {
		return method == null ? NO_METHOD_FACTOR * boosts(c) : methodFactor(c, share, byStele);
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

	/**
	 * Half a second of meditation for {@code player}'s heaven methods (MeditationManager): every method known whose layers are behind
	 * the realm settles by {@code gained} (meditation's cultivation, before any method's factor) x its own element's factor here
	 * ({@link #methodFactor}, by its stele too: {@code byStele} names the elements with a Dao Stele near), plus {@code refined} pill
	 * and fruit qi; each layer that settles is told, with any spell it brings. Returns whether any did.
	 */
	static boolean meditate(ServerPlayer player, double gained, double refined, java.util.Set<QiElement> byStele) {
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		boolean any = false;
		for (HeavenMethod method : java.util.List.copyOf(s.known())) {
			int upTo = understoodTo(c, method);
			if (s.layer(method) >= upTo) {
				if (s.progress(method) > 0) s.setMethod(method, s.layer(method), 0);
				continue;
			}
			int before = s.layer(method);
			double amount = c.physique() == Physique.FALLEN_TRUE_IMMORTAL ? Double.POSITIVE_INFINITY // understood at once, no settling
					: gained * methodFactor(c, shareAt(player, method.element()), byStele.contains(method.element())) + refined;
			if (settle(s, method, amount, upTo) <= 0) continue;
			any = true;
			for (int layer = before + 1; layer <= s.layer(method); layer++) settled(player, method, layer);
		}
		if (any) {
			player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
			afterLayer(player);
		}
		return any;
	}

	/** Layer {@code layer} of {@code method} has settled in: told, with the spells it brings. */
	private static void settled(ServerPlayer player, HeavenMethod method, int layer) {
		Component spells = null;
		for (Spell spell : Spell.values()) {
			if (!spell.taughtBy(method) || spell.layer() != layer) continue;
			spells = spells == null ? spell.getDisplayName().copy() : spells.copy().append(", ").append(spell.getDisplayName());
		}
		player.sendSystemMessage((spells == null ? Component.translatable(ModLang.MSG_HEAVEN_LAYER, method.getDisplayName(), layer)
				: Component.translatable(ModLang.MSG_HEAVEN_LAYER_SPELL, method.getDisplayName(), layer, spells)).withStyle(ChatFormatting.GOLD));
	}

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
		int layer = Math.max(1, understoodTo(c, method));
		s.setMethod(method, layer, 0);
		HeavenMethod active = s.activeMethod();
		if (active == null || active.element() == method.element() && method.tier() > active.tier()) s.setActiveMethod(method);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_HEAVEN_LEARNED, method.getDisplayName(), layer, method.layers()).withStyle(ChatFormatting.GOLD));
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
		for (Spell spell : Spell.values()) if (spell.isHeaven() && spell.isKnown(c)) now.add(spell);
		java.util.EnumSet<Spell> before = KNOWN_SPELLS.put(p.getUUID(), now);
		if (before == null) return;
		boolean added = false;
		for (Spell spell : now) {
			if (before.contains(spell)) continue;
			c.netherworld().addToWheel(spell);
			added = true;
		}
		if (!added) return;
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
