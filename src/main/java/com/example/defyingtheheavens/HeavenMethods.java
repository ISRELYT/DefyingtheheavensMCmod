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
 * Cultivating the heaven methods (see HeavenMethod): who may, how fast, how far, and what the layers bring.
 * <p>
 * <b>The realm waits on the method.</b> The heaven methods are the living's techniques, cultivated on the five elements' qi: a
 * method of a realm spans Qi Refining up to it, nine layers a realm (MethodLayers), and one's ordinary cultivation enters no stage
 * until a method has cultivated every layer standing for the stages before it ({@link #reach}, PlayerCultivation#isHeldByMethod).
 * The method itself waits on nothing: it grows for as long as one meditates, past the realm if it will, to its last layer. A method
 * goes no further than its own realm: past its last layer, one needs a deeper method. The deepest of any element counts. Without
 * one, cultivation stops at the end of Qi Refining Early. The dead can't cultivate them (their meridians are the ghost path's
 * now), nor a mortal (closed meridians).
 * <p>
 * <b>The cost and the rate.</b> A layer costs cultivation, the stage's own requirement shared out among the layers standing for it
 * ({@link #layerCost}), and meditating, the method practised takes in what meditation gathers (every boost to it included: Rings of
 * Power, mats, Qi Harmony, the Upper Realm's dense qi, spirit stones, Heaven's Favour, the Inner Realm; pills' qi as it is refined)
 * x {@link #methodFactor}: the element's share of the qi where one sits ({@link #elementFactor}: x{@link #SHARE_FLOOR} where there
 * is none of it, x1 at an even fifth, up to x3 where it is all of it) x {@link #STELE_BONUS} beside a Dao Stele of its element x a
 * Qi Gathering Pill's and cultivator clothing's boost to gathering. At an even fifth, then, the layers keep pace with the bar; and
 * a cultivator flies through a realm long passed, its layers costing what its stages do.
 * <p>
 * <b>Learning.</b> A Dao Stele (DaoSteleBlock) teaches its method to one who meditates beside it for {@link #STELE_LEARN_SECONDS};
 * a Jade Slip, read, at once; a cultivator who is Warm to you may teach theirs (TalkSystem). A method is learned at the depth the
 * element's methods already stand ({@link #startLayer}): an element's methods lay the realms out alike, so the deepest of them is
 * the way on, and a lesser one outgrown is known whole.
 * <p>
 * <b>What the layers bring.</b> Two spells each, at the third and seventh layers of its own realm (HeavenMethod#ownLayer, Spell);
 * and every layer of the element's deepest method strengthens the element's passive (HeavenPassives), by the realm the layer
 * stands in. Methods of one element don't stack; of different elements they do.
 */
public final class HeavenMethods {
	/** The element's share of the qi: x{@code SHARE_FLOOR + SHARE_GAIN x share}; an even fifth of the five is x1. */
	public static final double SHARE_FLOOR = 0.5, SHARE_GAIN = 2.5;
	public static final double STELE_BONUS = 1.25;
	public static final double STELE_LEARN_RADIUS = 4, STELE_BONUS_RADIUS = 8;
	public static final double STELE_LEARN_SECONDS = 15;
	/** A cultivator's guidance on a method already known: this many seconds of the learner's plain meditation (TalkSystem). */
	public static final double TEACHING_SECONDS = 10 * 60;

	// --- Who, and how far ---

	/** Whether {@code c} can take up a heaven method: the living, their meridians open. */
	public static boolean canLearn(PlayerCultivation c) {
		return !c.isMortal() && !c.netherworld().ghost();
	}

	/** Why {@code c} can't take up or cultivate a heaven method now, or null if they can: a mortal's closed meridians, or death. */
	public static Component refusal(PlayerCultivation c) {
		if (c.isMortal()) return Component.translatable(ModLang.MSG_HEAVEN_METHOD_MERIDIANS);
		if (c.netherworld().ghost()) return Component.translatable(ModLang.MSG_HEAVEN_METHOD_DEAD);
		return null;
	}

	/** Whether {@code method} is cultivated to the end of its last layer: nothing more to take in. */
	public static boolean perfected(HeavenState s, HeavenMethod method) {
		return s.layer(method) >= method.layers() && s.progress(method) >= layerCost(method.layers());
	}

	/**
	 * Cultivation through layer {@code layer} (PlayerCultivation's own units): the layers standing for a stage share out its
	 * requirement (PlayerCultivation#required) by their places' minutes (MethodLayers#MINUTES), so a stage's layers together cost
	 * what the stage does: Qi Refining Early's two 43 and 57 of its 100, Grand Perfection's one all of it.
	 */
	public static double layerCost(int layer) {
		int at = Math.max(1, layer);
		int within = MethodLayers.within(at);
		Stage stage = MethodLayers.stageAt(within);
		double minutes = 0;
		for (int w = 1; w <= MethodLayers.PER_REALM; w++) if (MethodLayers.stageAt(w) == stage) minutes += MethodLayers.MINUTES[w - 1];
		return PlayerCultivation.required(MethodLayers.realmOf(at), stage) * MethodLayers.MINUTES[within - 1] / minutes;
	}

	// --- How far the methods carry cultivation ---

	/** The layers of {@code method} cultivated whole: those before its current one, and that one too once its bar is full. */
	public static int completed(HeavenState s, HeavenMethod method) {
		int layer = s.layer(method);
		if (layer <= 0) return 0;
		return s.progress(method) >= layerCost(layer) ? layer : layer - 1;
	}

	/**
	 * The furthest rank (PlayerCultivation#rank) {@code c}'s heaven methods carry cultivation to: of any method known, the stage its
	 * next layer stands for once every layer before it is cultivated, and no further than its last layer stands for (a Qi Refining
	 * method, perfected, carries one to Grand Perfection, not into Foundation Building). The deepest of any element counts. Without
	 * one, Qi Refining Early: its bar fills, but Mid waits for a method's first two layers.
	 */
	public static int reach(PlayerCultivation c) {
		int reach = PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY);
		HeavenState s = c.heaven();
		for (HeavenMethod method : s.known()) reach = Math.max(reach, MethodLayers.floor(Math.min(completed(s, method) + 1, method.layers())));
		return reach;
	}

	/** Whether {@code c}'s heaven methods let their cultivation stand at {@code rank}. */
	public static boolean carries(PlayerCultivation c, int rank) {
		return rank <= reach(c);
	}

	/**
	 * Why {@code c}'s cultivation waits on the heaven methods to step from its stage into the next (PlayerCultivation#isHeldByMethod):
	 * the layer a method must finish (the last standing for the stage), or, with no method known that goes on past it, that a
	 * deeper one must be found.
	 */
	public static Component gateLine(PlayerCultivation c) {
		int rank = PlayerCultivation.rank(c.getRealm(), c.getStage());
		int layer = MethodLayers.deepestAt(rank);
		Component next = GhostMethods.rankName(rank + 1);
		for (HeavenMethod method : c.heaven().known()) {
			if (method.layers() > layer) return Component.translatable(ModLang.HEAVEN_GATE_LAYER, layer, next);
		}
		return Component.translatable(ModLang.HEAVEN_GATE_NONE, next);
	}

	// --- An element's methods ---

	/** The method of {@code element} {@code s} has cultivated deepest (of two as deep, the one that goes further), or null. */
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

	/**
	 * The layer {@code method} is learned at and its progress: where its element's deepest method known stands, progress and all,
	 * no further than its own last layer (then perfected); else its first layer.
	 */
	public static double[] startLayer(HeavenState s, HeavenMethod method) {
		HeavenMethod deepest = deepestOf(s, method.element());
		if (deepest == null) return new double[]{1, 0};
		int depth = s.layer(deepest);
		if (depth > method.layers()) return new double[]{method.layers(), layerCost(method.layers())};
		return new double[]{depth, Math.min(s.progress(deepest), layerCost(depth))};
	}

	// --- The rate ---

	/** The element's factor on the rate where {@code share} of the qi is of it. */
	public static double elementFactor(double share) {
		return SHARE_FLOOR + SHARE_GAIN * Math.max(0, Math.min(1, share));
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
	 * What a heaven method takes in of the cultivation meditation gathers, where {@code share} of the qi is its element's: the
	 * element's factor, {@link #STELE_BONUS} by its Dao Stele, and a Qi Gathering Pill's and cultivator clothing's boosts to gathering.
	 */
	public static double methodFactor(PlayerCultivation c, double share, boolean byStele) {
		return elementFactor(share) * (byStele ? STELE_BONUS : 1.0) * (1 + c.getQiBoost()) * (1 + c.getGearQiBonus());
	}

	/** Progress a cultivator's guidance gives {@code c}: {@link #TEACHING_SECONDS} of their plain meditation (their stage's own rate). */
	public static double teachingProgress(PlayerCultivation c) {
		return c.cultivationPerSecond() * TEACHING_SECONDS;
	}

	// --- Cultivating ---

	/**
	 * Adds {@code amount} of progress to {@code method}, layer by layer, whatever the heaven realm (the method waits on nothing),
	 * stopping only at the end of its last layer. Returns how many layers were gained.
	 */
	public static int cultivate(HeavenState s, HeavenMethod method, double amount) {
		int layer = s.layer(method);
		if (layer <= 0 || amount <= 0) return 0;
		double progress = s.progress(method) + amount;
		int gained = 0;
		int last = method.layers();
		while (progress >= layerCost(layer)) {
			if (layer >= last) {
				progress = layerCost(last); // perfected
				break;
			}
			progress -= layerCost(layer);
			layer++;
			gained++;
		}
		s.setMethod(method, layer, progress);
		return gained;
	}

	/** Teaches {@code player} {@code method} if they can take it up (nothing if they know it; told why if they can't). */
	public static boolean learn(ServerPlayer player, HeavenMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		if (c.heaven().knows(method)) return false;
		Component refusal = refusal(c);
		if (refusal != null) {
			player.displayClientMessage(refusal, true);
			return false;
		}
		give(player, method);
		return true;
	}

	/**
	 * {@code method}, whatever stands in the way (an op's command), at the layer {@link #startLayer} gives: the first learned is
	 * practised, and one going further than the practised method of its element takes over.
	 */
	static void give(ServerPlayer player, HeavenMethod method) {
		PlayerCultivation c = CultivationManager.get(player);
		HeavenState s = c.heaven();
		if (s.knows(method)) return;
		double[] start = startLayer(s, method);
		int layer = (int) start[0];
		s.setMethod(method, layer, start[1]);
		HeavenMethod active = s.activeMethod();
		if (active == null || active.element() == method.element() && method.tier() > active.tier()) s.setActiveMethod(method);
		player.sendSystemMessage((layer > 1 ? Component.translatable(ModLang.MSG_HEAVEN_LEARNED_AT, method.getDisplayName(), layer)
				: Component.translatable(ModLang.MSG_HEAVEN_LEARNED, method.getDisplayName())).withStyle(ChatFormatting.GOLD));
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
	 * layer and the realm and stage that layer stands for (Spell#isKnown), so the realm coming up to a layer cultivated long before
	 * brings it too, not only a layer gained ({@link #afterLayer}). The first look after joining notes what is known, adding nothing,
	 * so a spell taken off the wheel stays off.
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

	/** Makes {@code method} the heaven method meditation cultivates. */
	public static void practise(ServerPlayer p, HeavenMethod method) {
		HeavenState s = CultivationManager.get(p).heaven();
		if (method == null || !s.knows(method)) return;
		s.setActiveMethod(method);
		CultivationManager.markDirty(p.server);
		p.displayClientMessage(Component.translatable(ModLang.MSG_HEAVEN_PRACTISED, method.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		Spells.sync(p);
	}

	private HeavenMethods() {}
}
