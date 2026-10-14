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
 *   <li>Wood, Evergreen Vitality: health regained each second (a Four Axis method's 54th layer, 8 a second).</li>
 *   <li>Water, Ceaseless Flow: qi gathered each second, on top of the realm's (PlayerCultivation#qiGatherPerSecond; up to 42.57).</li>
 *   <li>Fire, Fury of the Fanned Flame: attack speed, in per cent (up to 21.6%).</li>
 *   <li>Earth, Unshaken Mountain Body: less harm taken, in per cent (up to 18%), never against what passes through
 *       invulnerability, the mod's soul harms, or a heavenly tribulation's lightning (those stay as tuned).</li>
 *   <li>Metal, Whetted Edge: melee harm (up to 35.1).</li>
 * </ul>
 */
public final class HeavenPassives {
	/** Each element's amount a layer adds, by the realm the layer stands in (Qi Refining to Four Axis), in the five's order. */
	static final double[][] PER_LAYER = {
			{0.02, 0.04, 0.08, 0.15, 0.25, 0.35},
			{0.02, 0.06, 0.15, 0.4, 1.1, 3},
			{0.25, 0.3, 0.35, 0.4, 0.5, 0.6},
			{0.2, 0.25, 0.3, 0.35, 0.4, 0.5},
			{0.05, 0.1, 0.25, 0.5, 1, 2},
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
	 * (spatial pressure, a severed or seized soul, the Soul River's scattering, the Judge's gaze), which stay as tuned.
	 */
	public static boolean untouchedByWards(DamageSource source) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(ModDamageTypes.SPATIAL_PRESSURE) || source.is(ModDamageTypes.SOUL_SEVERED)
				|| source.is(ModDamageTypes.SOUL_SEIZED) || source.is(ModDamageTypes.SOUL_SCATTER) || source.is(ModDamageTypes.JUDGE_GAZE);
	}

	/** A player left. */
	public static void forget(UUID id) {
		EARTH.remove(id);
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
