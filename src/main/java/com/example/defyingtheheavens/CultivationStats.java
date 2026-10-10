package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;

import java.util.Set;
import java.util.UUID;

/** Converts a realm + stage into attribute bonuses, and runs the Qi Sustenance ability. */
public final class CultivationStats {
	private static final UUID HEALTH_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a01");
	private static final UUID DAMAGE_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a02");
	private static final UUID SPEED_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a03");
	private static final UUID ARMOR_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a04");
	private static final UUID TOUGHNESS_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a05");
	private static final UUID KNOCKBACK_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a06");
	private static final Set<UUID> MODIFIER_IDS = Set.of(HEALTH_ID, DAMAGE_ID, SPEED_ID, ARMOR_ID, TOUGHNESS_ID, KNOCKBACK_ID);
	private static final int MAX_FOOD = 20;

	/** True for the attribute modifiers that carry the cultivation bonuses (lets the client recompute stats). */
	public static boolean isCultivationModifier(UUID id) {
		return MODIFIER_IDS.contains(id);
	}

	/** How each bonus is applied: speed multiplies the base walking speed, everything else is added. */
	public static AttributeModifier.Operation operation(Attribute attribute) {
		return attribute == Attributes.MOVEMENT_SPEED ? AttributeModifier.Operation.MULTIPLY_BASE : AttributeModifier.Operation.ADDITION;
	}

	public static double maxHealth(Realm r, Stage s) { return r.getMaxHealth() * s.getPowerMultiplier(); }
	public static double attackDamage(Realm r, Stage s) { return r.getAttackDamage() * s.getPowerMultiplier(); }
	/** Fraction added to base walking speed (0.25 = +25%). */
	public static double moveSpeed(Realm r, Stage s) { return r.getMoveSpeed() * s.getPowerMultiplier(); }
	public static double armor(Realm r, Stage s) { return r.getArmor() * s.getPowerMultiplier(); }
	public static double toughness(Realm r, Stage s) { return r.getToughness() * s.getPowerMultiplier(); }
	/** Capped at 1.0 (full immunity), the attribute's own maximum; Four Axis reaches it at Grand Perfection. */
	public static double knockbackResistance(Realm r, Stage s) { return Math.min(1.0, r.getKnockbackResistance() * s.getPowerMultiplier()); }
	/** Size of the qi pool. */
	public static double maxQi(Realm r, Stage s) { return r.getMaxQi() * s.getPowerMultiplier(); }
	/** Qi gathered per second, before the Upper Realm's multiplier. */
	public static double qiGather(Realm r, Stage s) { return r.getQiGather() * s.getPowerMultiplier(); }

	/**
	 * (Re)applies all cultivation modifiers from the EFFECTIVE stage (capped by Realm Suppression in the lower realms).
	 * Permanent modifiers are saved with the player, so health survives relogs.
	 */
	public static void apply(ServerPlayer player, PlayerCultivation c) {
		Realm r = c.getEffectiveRealm();
		Stage s = c.getEffectiveStage();
		double scale = c.isMortal() ? 0 : 1; // a mortal's body is just a body
		set(player, Attributes.MAX_HEALTH, HEALTH_ID, "Cultivation max health", maxHealth(r, s) * scale);
		set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Cultivation attack damage", attackDamage(r, s) * scale);
		set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "Cultivation speed", moveSpeed(r, s) * scale);
		set(player, Attributes.ARMOR, ARMOR_ID, "Cultivation armor", armor(r, s) * scale);
		set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "Cultivation toughness", toughness(r, s) * scale);
		set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Cultivation knockback resistance", knockbackResistance(r, s) * scale);
	}

	/**
	 * Every tick, tops up food and saturation for players with Qi Sustenance active (awakened and switched on). Full food plus
	 * saturation keeps vanilla's fast natural regen (1 health per half second) running, as if always freshly fed.
	 */
	public static void tickHunger(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || !CultivationManager.get(player).isAbilityActive(Ability.QI_SUSTENANCE)) continue;
			FoodData food = player.getFoodData();
			food.setFoodLevel(MAX_FOOD);
			food.setSaturation(MAX_FOOD); // saturation can't exceed the food level, so 20 is the cap
		}
	}

	private static void set(ServerPlayer player, Attribute attribute, UUID id, String name, double amount) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		if (amount != 0) {
			instance.addPermanentModifier(new AttributeModifier(id, name, amount, operation(attribute)));
		}
	}

	private CultivationStats() {}
}
