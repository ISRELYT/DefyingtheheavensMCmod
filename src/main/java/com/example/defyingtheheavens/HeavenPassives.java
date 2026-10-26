package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The five elements' passives (see HeavenMethods): every layer of an element's most deeply cultivated method strengthens its
 * element's passive by the amount of the realm the layer stands in ({@link #PER_LAYER}), once one's cultivation has reached the
 * stage that layer stands for (a method run ahead of the realm adds nothing for its layers above it until the realm catches up);
 * an element's methods don't stack, the five elements do. Only the living's, their meridians open. Applied once a second
 * (NetherworldManager's tick) and whenever the layers change.
 * <ul>
 *   <li>Wood, Evergreen Vitality: health regained each second (a Four Axis method's 54th layer, 8 a second; a True Immortal one's
 *       99th, some 1,200).</li>
 *   <li>Water, Ceaseless Flow: qi gathered each second, on top of the realm's (PlayerCultivation#qiGatherPerSecond; 42.57 at Four
 *       Axis's last layer, some 10,150 at True Immortal's).</li>
 *   <li>Fire, Fury of the Fanned Flame: attack speed, in per cent (21.6% at Four Axis's last, 63% at True Immortal's).</li>
 *   <li>Earth, Unshaken Mountain Body: less harm taken, in per cent (18% at Four Axis's last, 47.7% at True Immortal's), never
 *       against what passes through invulnerability, the mod's soul harms, or a heavenly tribulation's lightning (those stay as tuned).</li>
 *   <li>Metal, Whetted Edge: melee harm (35.1 at Four Axis's last, some 6,840 at True Immortal's).</li>
 * </ul>
 */
public final class HeavenPassives {
	/**
	 * Each element's amount a layer adds, by the realm the layer stands in (Qi Refining to True Immortal), in the five's order. Past
	 * Four Axis the healing, the qi and the edge grow as the realms' own health, gathering and strength do (Void Shattering's and True
	 * Immortal's leaps with them); the fury and the ward a little a realm.
	 */
	static final double[][] PER_LAYER = {
			{0.02, 0.04, 0.08, 0.15, 0.25, 0.35, 0.7, 2.8, 5.6, 11.2, 112},
			{0.02, 0.06, 0.15, 0.4, 1.1, 3, 7, 22, 49, 105, 940},
			{0.25, 0.3, 0.35, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.2},
			{0.2, 0.25, 0.3, 0.35, 0.4, 0.5, 0.55, 0.6, 0.65, 0.7, 0.8},
			{0.05, 0.1, 0.25, 0.5, 1, 2, 4, 16, 32, 64, 640},
	};

	private static final UUID FIRE_ID = UUID.fromString("5b1f7c3e-8a24-4d61-9e0b-2c7a4f18d630");
	private static final UUID METAL_ID = UUID.fromString("c4e8a913-6f2d-4b07-a5c1-93d0e27b4f58");
	/** Each living cultivator's Unshaken Mountain Body (per cent), as last applied: read on every blow, so kept rather than counted. */
	private static final Map<UUID, Double> EARTH = new HashMap<>();

	/**
	 * The passive {@code element}'s layers in {@code s} come to, no further than layer {@code upTo}: each layer of its deepest
	 * method by the realm it stands in.
	 */
	public static double amount(HeavenState s, QiElement element, int upTo) {
		int depth = Math.min(HeavenMethods.depth(s, element), upTo);
		double total = 0;
		for (int layer = 1; layer <= depth; layer++) total += PER_LAYER[element.ordinal()][MethodLayers.realmOf(layer).ordinal()];
		return total;
	}

	/**
	 * What {@code c}'s passive of {@code element} comes to now: its layers as far as its cultivation has come (the deepest layer
	 * standing for its realm and stage); nothing for a mortal or one of the dead.
	 */
	public static double amount(PlayerCultivation c, QiElement element) {
		if (c.isMortal() || c.netherworld().ghost() || !c.heaven().knowsAny()) return 0;
		return amount(c.heaven(), element, MethodLayers.deepestAt(PlayerCultivation.rank(c.getRealm(), c.getStage())));
	}

	/** Applies the five passives to {@code p}: the wood's healing, the fire's and metal's modifiers, the earth's ward kept for blows. */
	public static void apply(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		double wood = amount(c, QiElement.WOOD);
		if (wood > 0 && p.isAlive() && p.getHealth() < p.getMaxHealth()) p.heal((float) wood);
		set(p, Attributes.ATTACK_SPEED, FIRE_ID, "Fury of the Fanned Flame", amount(c, QiElement.FIRE) / 100.0, AttributeModifier.Operation.MULTIPLY_BASE);
		set(p, Attributes.ATTACK_DAMAGE, METAL_ID, "Whetted Edge", amount(c, QiElement.METAL), AttributeModifier.Operation.ADDITION);
		double earth = amount(c, QiElement.EARTH);
		if (earth > 0) EARTH.put(p.getUUID(), earth);
		else EARTH.remove(p.getUUID());
	}

	/** What harm coming to {@code target} comes to after its passives (HeavenSpells#incoming); the same amount where none applies. */
	public static float incoming(LivingEntity target, DamageSource source, float amount) {
		if (!(target instanceof ServerPlayer p) || EARTH.isEmpty()) return amount;
		Double earth = EARTH.get(p.getUUID());
		if (earth == null || earth <= 0 || untouchedByWards(source) || TribulationManager.isActive(p.getUUID())) return amount;
		return (float) (amount * (1 - Math.min(90, earth) / 100.0));
	}

	/**
	 * Harm no heaven ward or passive lessens: what passes through invulnerability (the void, /kill), and the mod's soul harms
	 * (spatial pressure, a severed or seized soul, the Soul River's scattering, the Judge's gaze), which stay as tuned; and the Chaos
	 * Frontier's storm (only shelter turns it: ChaosFrontier).
	 */
	public static boolean untouchedByWards(DamageSource source) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(ModDamageTypes.SPATIAL_PRESSURE) || source.is(ModDamageTypes.SOUL_SEVERED)
				|| source.is(ModDamageTypes.SOUL_SEIZED) || source.is(ModDamageTypes.SOUL_SCATTER) || source.is(ModDamageTypes.JUDGE_GAZE)
				|| source.is(ModDamageTypes.CHAOS_STORM);
	}

	// --- The masteries (the user's ask, 2026-10-25) ---

	/**
	 * The layer of an element's method (understood as far as one's cultivation has come) from which its mastery shows: the first of Core
	 * Formation. Fire's, Flame-Kin: one's own fire no longer burns one ({@link #ownFireSpares}: the fire one's spells set, the ground they
	 * scorch, and the burning one takes from them). Water's, Water-Treading: water bears one up as the ground does, to walk on as it is (sneak
	 * to sink into it), and a fall onto it does no harm (SpellLivingEntityMixin).
	 */
	public static final int MASTERY_LAYER = 19;

	/** Whether {@code c} has mastered {@code element}: its deepest method understood to {@link #MASTERY_LAYER} or past. The living's only. */
	public static boolean masters(PlayerCultivation c, QiElement element) {
		if (c == null || c.isMortal() || c.netherworld().ghost() || !c.heaven().knowsAny()) return false;
		int understood = Math.min(HeavenMethods.depth(c.heaven(), element), MethodLayers.deepestAt(PlayerCultivation.rank(c.getRealm(), c.getStage())));
		return understood >= MASTERY_LAYER;
	}

	/** The client's word on a mastery: its own player's, as its cultivation was told it (set by the client; nothing elsewhere). */
	public static java.util.function.BiPredicate<LivingEntity, QiElement> clientView = (e, element) -> false;

	/** Whether {@code e}, a player, has mastered {@code element} (on a client, as it was told: its own player only). */
	public static boolean masters(LivingEntity e, QiElement element) {
		if (e.level().isClientSide) return clientView.test(e, element);
		return e instanceof ServerPlayer p && masters(CultivationManager.get(p), element);
	}

	/** Land a player's spells set burning: where, how far round, and until when it is still theirs. */
	private record Blaze(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> level, net.minecraft.world.phys.Vec3 at, double radius, long until) {}

	/** Each player's own fire, its burning land (merged as it spreads), the newest last. */
	private static final Map<UUID, java.util.List<Blaze>> OWN_FIRE = new HashMap<>();
	/** When each player last stood in their own fire (the burning it left on them is theirs too, a while). */
	private static final Map<UUID, Long> IN_OWN_FIRE = new HashMap<>();
	/** How long the land a player's spells set burning stays their own fire (it may spread a while), and how long the burning it leaves on them is. */
	static final int OWN_FIRE_TICKS = 20 * 180, OWN_BURNING_TICKS = 20 * 10;

	/**
	 * {@code caster}'s spells set the land {@code radius} round {@code at} burning (Devastation: a fire set, a scorched or glassy floor, a burnt
	 * shaft): it is their own fire a while ({@link #OWN_FIRE_TICKS}). Fires set close together are taken as one blaze, grown to hold them.
	 */
	public static void ownFire(LivingEntity caster, net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 at, double radius) {
		if (!(caster instanceof ServerPlayer)) return;
		java.util.List<Blaze> mine = OWN_FIRE.computeIfAbsent(caster.getUUID(), k -> new java.util.ArrayList<>());
		long now = level.getGameTime(), until = now + OWN_FIRE_TICKS;
		mine.removeIf(b -> b.until() < now);
		for (int i = 0; i < mine.size(); i++) {
			Blaze b = mine.get(i);
			double apart = b.at().distanceTo(at);
			if (b.level() != level.dimension() || apart > b.radius() + 3) continue;
			mine.set(i, new Blaze(b.level(), b.at(), Math.min(256, Math.max(b.radius(), apart + radius)), until));
			return;
		}
		if (mine.size() >= 32) mine.remove(0);
		mine.add(new Blaze(level.dimension(), at, radius, until));
	}

	/**
	 * Whether {@code source} is {@code target}'s own fire, that one who has mastered fire takes nothing from (Flame-Kin, {@link #MASTERY_LAYER}):
	 * standing in fire or on magma within the land their own spells set burning ({@link #ownFire}), or burning from it a while after. Asked as
	 * the harm comes (HeavenSpells#incoming); the flames on them are put out with it.
	 */
	public static boolean ownFireSpares(LivingEntity target, DamageSource source) {
		if (!(target instanceof ServerPlayer p) || OWN_FIRE.isEmpty()) return false;
		boolean standing = source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR);
		boolean burning = source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE);
		if (!standing && !burning) return false;
		java.util.List<Blaze> mine = OWN_FIRE.get(p.getUUID());
		if (mine == null || !masters(p, QiElement.FIRE)) return false;
		long now = p.level().getGameTime();
		for (Blaze b : mine) {
			if (b.until() >= now && b.level() == p.level().dimension() && b.at().distanceTo(p.position()) <= b.radius() + 1.5) {
				IN_OWN_FIRE.put(p.getUUID(), now);
				return true;
			}
		}
		Long last = IN_OWN_FIRE.get(p.getUUID());
		return burning && last != null && now - last <= OWN_BURNING_TICKS;
	}

	/** A player left. */
	public static void forget(UUID id) {
		EARTH.remove(id);
		OWN_FIRE.remove(id);
		IN_OWN_FIRE.remove(id);
	}

	private static void set(ServerPlayer p, Attribute attribute, UUID id, String name, double amount, AttributeModifier.Operation operation) {
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

	private HeavenPassives() {}
}
