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
 * Cultivating the ghost methods (see GhostMethod): how fast, how far, and what each layer brings.
 * <p>
 * <b>The ghost path.</b> The ghost methods are a path of their own beside ordinary cultivation, and neither builds on the other:
 * the foundations carry the <b>ghost path</b> through the realms ({@link #ghostRank}), never one's ordinary cultivation, and
 * one's ordinary realm neither opens a ghost method nor speeds it. (A cultivator strong on both paths is meant, one day, to be
 * stronger than one strong on either.)
 * <p>
 * <b>The rate.</b> Meditating wherever ghostly qi flows (the Nether as well as the Netherworld: every layer, anywhere it flows),
 * the active method gains progress at {@link #BASE_RATE} (a second a second, for a ghost path of Nascent Soul, living, in a Soul
 * Sand Valley) x the density / {@link GhostQi#SOUL_SAND_VALLEY} (so x10 in the Netherworld, x12 on the Nine Nether Peaks, x0.25
 * in the rest of the Nether) x the ghost path's factor ({@link #realmFactor}: -10% a realm below Nascent Soul, +10% above) x Qi
 * Harmony x a Yin Gathering Pill x {@link #GHOST_FACTOR} for the dead (made of yin) x {@link #STELE_BONUS} by a stele of that
 * method. The Netherworld is where the dead's methods are best cultivated, not the only place they can be.
 * <p>
 * <b>The layers.</b> Progress is counted in seconds of that baseline meditation, so a layer's cost is its minutes x 60
 * ({@link #layerCost}). The fire and the steps: {@link #MINUTES} = 5, 8, 12, 20, 30, 45, 60, 90, 120 minutes for layers one to
 * nine, about six and a half hours in all. A foundation spans Qi Refining up to its own realm, nine layers to each
 * (MethodLayers): a realm's nine cost 3, 4, 5, 6, 8, 10, 12, 15, 20 minutes (83) times the realm's factor (MethodLayers
 * #REALM_FACTOR, x1 to x16), whatever the foundation. So the Breathing Art's 9 layers take 83 minutes, the Yellow Springs Marrow
 * Art's 18 those and 2.8 hours more, and so on to the Six Paths Reincarnation Canon's 54, about 57 hours in all, in a Soul Sand
 * Valley; a tenth of that in the Netherworld (8 minutes, 5.7 hours), and a ghost goes 1.5 times as fast again.
 * <p>
 * <b>Learning.</b> Meditating within {@link #STELE_LEARN_RADIUS} of a Method Stele for {@link #STELE_LEARN_SECONDS} teaches
 * its method; a Ghost Jade Slip, read, teaches it at once. Not to a mortal, alive or dead: its meridians are closed, for the ghost
 * path as for the heaven path, until a Marrow Cleansing Elixir opens them ({@link #refusal}); any method a mortal knows lies
 * dormant, uncultivated, its spells and abilities out of reach. Any other can take up any foundation, the rarest too, from Qi
 * Refining on: a foundation is learned at the depth the ghost path already stands ({@link #startLayer}), or its first layer.
 * <p>
 * <b>The ghost path's realms.</b> A foundation carries the ghost path from Qi Refining up to its own realm: within each realm's
 * nine, the first layer to its Early stage, the third to Mid, the sixth to Late, the ninth to Grand Perfection ({@link #floor}).
 * The deepest layer of any foundation known sets it ({@link #ghostRank}). So the Breathing Art ends at Qi Refining's Grand
 * Perfection, and only a deeper foundation, found, carries the ghost path further. A ghost whose ghost path reaches Core Formation
 * is a Ghost King, as one who died there is (GhostSystem#isKing).
 * <p>
 * <b>The ghost path's gate.</b> The fire and the steps go only as far as the ghost path carries them ({@link #layerCap}): three
 * layers in its Qi Refining, six in its Foundation Building, all nine from its Core Formation, the living and the dead alike.
 */
public final class GhostMethods {
	/** The fire's and the steps' layers (a foundation's: GhostMethod#layers). */
	public static final int LAYERS = 9;
	/** The fire's and the steps' layers, in minutes of baseline meditation. */
	public static final int[] MINUTES = {5, 8, 12, 20, 30, 45, 60, 90, 120};
	public static final double BASE_RATE = 1.0;
	public static final double GHOST_FACTOR = 1.5;
	public static final double REALM_STEP = 0.10;
	public static final double STELE_BONUS = 1.25;
	public static final double STELE_LEARN_RADIUS = 4, STELE_BONUS_RADIUS = 8;
	public static final double STELE_LEARN_SECONDS = 15;
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

	/** Seconds of baseline meditation to cultivate {@code method} through layer {@code layer} (1 to its last). */
	public static double layerCost(GhostMethod method, int layer) {
		int at = Math.max(1, Math.min(method.layers(), layer));
		if (!method.isFoundation()) return MINUTES[at - 1] * 60.0;
		return MethodLayers.cost(at);
	}

	// --- The ghost path ---

	/** The ghost path (a rank, see PlayerCultivation#rank) layer {@code layer} of the foundation {@code method} carries one to; -1 for none. */
	public static int floor(GhostMethod method, int layer) {
		if (!method.isFoundation() || layer <= 0) return -1;
		return MethodLayers.floor(Math.min(method.layers(), layer));
	}

	/**
	 * The foundation {@code s} has cultivated deepest, the one that carries its ghost path (of two as deep, the one that goes
	 * further); or null if it knows none.
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

	/** Where the ghost path stands (a rank: the deepest foundation's floor), or -1 if no foundation is known (it isn't begun). */
	public static int ghostRank(NetherworldState s) {
		GhostMethod deepest = deepestFoundation(s);
		return deepest == null ? -1 : floor(deepest, s.layer(deepest));
	}

	/**
	 * The ghost path falls to {@code rank} (its nascent soul lost, its core detonated: the realm's abilities it awakened cost what
	 * the heaven path's would, TwoPaths#awakens): every method held no deeper than that rank's last layer (MethodLayers#deepestAt),
	 * as the heaven path's methods are held to its realm (HeavenMethods#clampTo). A path already at or below it is untouched.
	 */
	public static void fallTo(NetherworldState s, int rank) {
		int cap = Math.max(1, MethodLayers.deepestAt(rank));
		for (GhostMethod method : new java.util.ArrayList<>(s.knownMethods())) {
			if (s.layer(method) > cap) s.setMethod(method, cap, 0);
		}
	}

	/** The ghost path's realm, or null if it isn't begun. */
	public static Realm ghostRealm(NetherworldState s) {
		int rank = ghostRank(s);
		return rank < 0 ? null : Realm.byIndex(rank / Stage.values().length);
	}

	/**
	 * How far the ghost path has come through the stage it stands at, toward the next, 0 to 1: the deepest foundation's layers
	 * of that stage behind it and its progress through the one it is on, against all of the stage's layers (the Cultivation
	 * tab's ghost path bar). 0 if it isn't begun.
	 */
	public static double stageProgress(NetherworldState s) {
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
	 * The layer {@code method} is learned at, and its progress through it: a foundation where the ghost path already stands (the
	 * deepest layer of the foundations known, progress and all), no further than its own last layer (then perfected); else, and
	 * the fire and the steps always, its first layer. A foundation's layers cost the same whichever it is (MethodLayers), so the
	 * progress carries over as it is.
	 */
	public static double[] startLayer(NetherworldState s, GhostMethod method) {
		GhostMethod deepest = method.isFoundation() ? deepestFoundation(s) : null;
		if (deepest == null) return new double[]{1, 0};
		int depth = s.layer(deepest);
		if (depth > method.layers()) return new double[]{method.layers(), layerCost(method, method.layers())};
		return new double[]{depth, Math.min(s.progress(deepest), layerCost(method, depth))};
	}

	// --- The ghost path's gate ---

	/**
	 * The furthest layer {@code c} can cultivate {@code method} to: a foundation all its own; the fire and the steps as far as the
	 * ghost path carries them, three a realm (not begun or Qi Refining: 3; Foundation Building: 6; Core Formation and on: 9).
	 */
	public static int layerCap(PlayerCultivation c, GhostMethod method) {
		if (method.isFoundation()) return method.layers();
		Realm realm = ghostRealm(c.netherworld());
		int index = realm == null ? 0 : Math.min(realm.ordinal(), Realm.CORE_FORMATION.ordinal());
		return Math.min(LAYERS, LAYERS_PER_REALM * (index + 1));
	}

	/** The ghost path's realm needed to cultivate {@code method} past {@code cap}. */
	public static Realm realmToPass(GhostMethod method, int cap) {
		if (method.isFoundation()) return method.realm();
		return Realm.byIndex(Math.min(Realm.CORE_FORMATION.ordinal(), cap / LAYERS_PER_REALM));
	}

	/** Whether {@code method} is held at the ghost path's gate right now: the layer's progress full, the next beyond the path. */
	public static boolean heldByRealm(PlayerCultivation c, GhostMethod method) {
		NetherworldState s = c.netherworld();
		int layer = s.layer(method);
		return layer > 0 && layer < method.layers() && layer >= layerCap(c, method) && s.progress(method) >= layerCost(method, layer);
	}

	/** The ghost path's factor on the rate: -10% a realm below Nascent Soul, +10% a realm above (not begun: as Qi Refining). */
	public static double realmFactor(PlayerCultivation c) {
		Realm realm = ghostRealm(c.netherworld());
		int index = realm == null ? Realm.QI_REFINING.ordinal() : realm.ordinal();
		return 1 + REALM_STEP * (index - Realm.NASCENT_SOUL.ordinal());
	}

	/**
	 * Progress a second at {@code density} (a Soul Sand Valley's the baseline, the Netherworld's ten times it), every factor but
	 * harmony, pill and stele (which the caller multiplies in).
	 */
	public static double ratePerSecond(PlayerCultivation c, double density) {
		return BASE_RATE * density / GhostQi.SOUL_SAND_VALLEY * realmFactor(c) * (c.netherworld().ghost() ? GHOST_FACTOR : 1.0)
				* c.physique().ghostFactor(); // a Ghost Devil Body's, a Fallen True Immortal's (Physique)
	}

	/** {@link #cultivate(NetherworldState, GhostMethod, double, int)} with no gate (commands). */
	public static int cultivate(NetherworldState s, GhostMethod method, double amount) {
		return cultivate(s, method, amount, method.layers());
	}

	/**
	 * Adds {@code amount} of progress to {@code method}, layer by layer; stops at the end of layer {@code layerCap} (the ghost
	 * path's gate, {@link #layerCap}) and at the end of its last layer. Returns how many layers were gained, or, with nothing
	 * gained, -2 if it was held at the ghost path's gate.
	 */
	public static int cultivate(NetherworldState s, GhostMethod method, double amount, int layerCap) {
		int layer = s.layer(method);
		if (layer <= 0 || amount <= 0) return 0;
		double progress = s.progress(method) + amount;
		int gained = 0;
		int held = 0;
		int last = method.layers();
		while (progress >= layerCost(method, layer)) {
			if (layer >= last) {
				progress = layerCost(method, last); // perfected
				break;
			}
			if (layer >= layerCap) {
				progress = layerCost(method, layer); // held where the ghost path can carry it no further
				held = -2;
				break;
			}
			progress -= layerCost(method, layer);
			layer++;
			gained++;
		}
		s.setMethod(method, layer, progress);
		return gained > 0 ? gained : held;
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
	 * {@code method}, whatever stands in the way (an op's command), at the layer {@link #startLayer} gives: the first method learned
	 * becomes the active one, and its spells go on the wheel as they come.
	 */
	static void give(ServerPlayer player, GhostMethod method) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		if (s.knows(method)) return;
		int before = ghostRank(s);
		double[] start = startLayer(s, method);
		int layer = (int) start[0];
		// A Ghost Devil Body (Physique) finding a foundation of Core Formation or deeper leaps to its realm's first layer.
		if (method.isFoundation() && CultivationManager.get(player).physique() == Physique.GHOST_DEVIL_BODY
				&& method.realm().ordinal() >= Realm.CORE_FORMATION.ordinal()) {
			int first = method.realm().ordinal() * MethodLayers.PER_REALM + 1;
			if (layer < first) start = new double[]{layer = first, 0};
		}
		s.setMethod(method, layer, start[1]);
		// The first learned is practised; and a foundation going further than the foundation practised takes over (the way on).
		GhostMethod active = s.activeMethod();
		if (active == null || method.isFoundation() && active.isFoundation() && method.tier() > active.tier()) s.setActiveMethod(method);
		player.sendSystemMessage((layer > 1 ? Component.translatable(ModLang.MSG_METHOD_LEARNED_AT, method.getDisplayName(), layer)
				: Component.translatable(ModLang.MSG_METHOD_LEARNED, method.getDisplayName())).withStyle(ChatFormatting.AQUA));
		player.playNotifySound(SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 0.7f);
		afterLayer(player, s, before);
		Journal.note(player, "method:" + method.getId(), ModLang.JOURNAL_METHOD, Component.translatable(ModLang.JOURNAL_METHOD_TEXT,
				method.getDisplayName(), method.getGains()));
		CultivationManager.markDirty(player.server);
		Spells.sync(player);
		NetherworldManager.refresh(player);
	}

	/** After layers change: spells newly in reach go on the wheel. */
	static void afterLayer(ServerPlayer player, NetherworldState s) {
		afterLayer(player, s, ghostRank(s));
	}

	/**
	 * After layers change, the ghost path having stood at {@code ghostRankBefore}: spells newly in reach go on the wheel, and if
	 * the ghost path has risen, its cultivator is told (and a ghost risen to Core Formation is crowned: GhostSystem#watchKing).
	 */
	static void afterLayer(ServerPlayer player, NetherworldState s, int ghostRankBefore) {
		PlayerCultivation c = CultivationManager.get(player);
		for (Spell spell : Spell.values()) {
			if (spell.isKnown(c)) s.addToWheel(spell);
		}
		int now = ghostRank(s);
		if (now > ghostRankBefore) {
			GhostMethod foundation = deepestFoundation(s);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_METHOD_FLOOR, foundation.getDisplayName(), rankName(now)).withStyle(ChatFormatting.AQUA));
			player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
		}
		// The body follows the higher path, and the harmony the two (TwoPaths): either may have changed.
		if (now != ghostRankBefore) CultivationManager.refresh(player);
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
