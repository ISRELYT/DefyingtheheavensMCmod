package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;

import java.util.Set;
import java.util.UUID;

/** Converts a realm + stage into attribute bonuses (a player's from the higher of their two paths, TwoPaths), and runs the Qi Sustenance ability. */
public final class CultivationStats {
	private static final UUID HEALTH_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a01");
	private static final UUID DAMAGE_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a02");
	private static final UUID SPEED_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a03");
	private static final UUID ARMOR_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a04");
	private static final UUID TOUGHNESS_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a05");
	private static final UUID KNOCKBACK_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4a06");
	private static final Set<UUID> MODIFIER_IDS = Set.of(HEALTH_ID, DAMAGE_ID, SPEED_ID, ARMOR_ID, TOUGHNESS_ID, KNOCKBACK_ID);
	private static final int MAX_FOOD = 20;
	/** The health a player's body has before any cultivation (vanilla's ten hearts). */
	public static final double BASE_HEALTH = 20.0;

	/** True for the attribute modifiers that carry the cultivation bonuses (lets the client recompute stats). */
	public static boolean isCultivationModifier(UUID id) {
		return MODIFIER_IDS.contains(id);
	}

	/** How each bonus is applied: speed multiplies the base walking speed, everything else is added. */
	public static AttributeModifier.Operation operation(Attribute attribute) {
		return attribute == Attributes.MOVEMENT_SPEED ? AttributeModifier.Operation.MULTIPLY_BASE : AttributeModifier.Operation.ADDITION;
	}

	public static double maxHealth(Realm r, Stage s) { return r.getMaxHealth() * s.getPowerMultiplier(); }

	/** The health {@code body} gives (before stat points): the whole of it, the hearts a body starts with and the cultivation's, at its share. */
	public static double bodyHealth(TwoPaths.Body body) {
		return (BASE_HEALTH + maxHealth(body.realm(), body.stage()) * body.scale()) * body.share();
	}
	public static double attackDamage(Realm r, Stage s) { return r.getAttackDamage() * s.getPowerMultiplier(); }
	/** An Ancient Holy Body's health and strength (Physique) are this many times another's. */
	public static final double HOLY_BODY = 1.3;

	/** What {@code c}'s health (the whole of it) and its strength's bonus are multiplied by: {@link #HOLY_BODY} for an Ancient Holy Body. */
	public static double holyFactor(PlayerCultivation c) {
		return c.hasPhysique(Physique.ANCIENT_HOLY_BODY) ? HOLY_BODY : 1.0;
	}
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
	 * (Re)applies all cultivation modifiers from the body (TwoPaths#body: the higher of the heaven path and the ghost path, at
	 * the EFFECTIVE stage, capped by Realm Suppression in the lower realms), raised by Heaven's Favour and the two paths'
	 * harmony; a mortal on neither path, its tempered body growing toward Qi Refining Early's bonuses. Permanent modifiers are
	 * saved with the player, so health survives relogs.
	 */
	public static void apply(ServerPlayer player, PlayerCultivation c) {
		TwoPaths.Body body = TwoPaths.body(c, true);
		Realm r = body.realm();
		Stage s = body.stage();
		// A path the body doesn't belong to gives it half its strength (TwoPaths#pathShare: the living's ghost path, the dead's heaven
		// path): half of every bonus, and half of the whole of its health, the hearts it started with too.
		double share = body.share();
		double scale = body.scale() * share;
		double holy = holyFactor(c); // an Ancient Holy Body's health (the whole of it) and strength
		// A clone (Clones) has half its maker's strength: its health (the whole of it), blows, armour, toughness and footing; not its speed.
		double clone = Clones.strength(c);
		set(player, Attributes.MAX_HEALTH, HEALTH_ID, "Cultivation max health", bodyHealth(body) * holy * clone - BASE_HEALTH);
		set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Cultivation attack damage", attackDamage(r, s) * scale * holy * clone);
		set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "Cultivation speed", moveSpeed(r, s) * scale);
		set(player, Attributes.ARMOR, ARMOR_ID, "Cultivation armor", armor(r, s) * scale * clone);
		set(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "Cultivation toughness", toughness(r, s) * scale * clone);
		set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Cultivation knockback resistance", knockbackResistance(r, s) * scale * clone);
		StatPoints.apply(player, c); // and what the player has put into their stats on top
	}

	/**
	 * The same bonuses for an NPC cultivator (see CultivatorNpc), with one difference: a mob's walking speed counts twice in
	 * its movement (it is both the push and the scale), so its share is the square root of a player's, to move as much faster.
	 */
	public static void applyNpc(LivingEntity npc, Realm r, Stage s, boolean mortal) {
		double scale = mortal ? 0 : 1;
		set(npc, Attributes.MAX_HEALTH, HEALTH_ID, "Cultivation max health", maxHealth(r, s) * scale);
		set(npc, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Cultivation attack damage", attackDamage(r, s) * scale);
		set(npc, Attributes.MOVEMENT_SPEED, SPEED_ID, "Cultivation speed", (Math.sqrt(1 + moveSpeed(r, s)) - 1) * scale);
		set(npc, Attributes.ARMOR, ARMOR_ID, "Cultivation armor", armor(r, s) * scale);
		set(npc, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "Cultivation toughness", toughness(r, s) * scale);
		set(npc, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "Cultivation knockback resistance", knockbackResistance(r, s) * scale);
	}

	/**
	 * A body mends from nothing to whole in about this long, whatever its realm (the user's ask, 2026-10-22: the great bodies took
	 * hours at vanilla's two a second), fed as Qi Sustenance keeps it; the small bodies of the first realms keep vanilla's own pace,
	 * which is quicker. In tribulations and the gap too (the user's word, the same day: "its okay if they become easier").
	 */
	public static final double MEND_SECONDS = 30;
	/** What vanilla's quick regeneration (2 health a second) mends in {@link #MEND_SECONDS}: a greater body mends the faster. */
	private static final double VANILLA_MENDS = 2 * MEND_SECONDS;

	/**
	 * How many times vanilla's natural regeneration {@code p}'s body heals at (FoodDataMixin; the dead's in the Netherworld too,
	 * GhostSystem): its realm's body ({@link #bodyHealth}: the higher path, as wielded) over {@link #VANILLA_MENDS}, never less than
	 * once. Four Axis Grand Perfection about 4, Void Shattering's some 30, a True Immortal's over 1,000.
	 */
	public static double mendFactor(ServerPlayer p) {
		return Math.max(1, bodyHealth(TwoPaths.body(CultivationManager.get(p), true)) / VANILLA_MENDS);
	}

	/** Health a second a body of {@code maxHealth} mends at rest (an NPC cultivator's out of combat): two, or the whole of it in {@link #MEND_SECONDS}. */
	public static float mendPerSecond(float maxHealth) {
		return (float) Math.max(2.0, maxHealth / MEND_SECONDS);
	}

	/**
	 * Every tick, tops up food and saturation for players with Qi Sustenance active (awakened and switched on). Full food plus
	 * saturation keeps vanilla's fast natural regen (1 health per half second, times the realm's: {@link #mendFactor}) running, as
	 * if always freshly fed.
	 */
	public static void tickHunger(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!player.isAlive() || !CultivationManager.get(player).isAbilityActive(Ability.QI_SUSTENANCE)) continue;
			FoodData food = player.getFoodData();
			food.setFoodLevel(MAX_FOOD);
			food.setSaturation(MAX_FOOD); // saturation can't exceed the food level, so 20 is the cap
		}
	}

	private static void set(LivingEntity entity, Attribute attribute, UUID id, String name, double amount) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		if (amount != 0) {
			instance.addPermanentModifier(new AttributeModifier(id, name, amount, operation(attribute)));
		}
	}

	private CultivationStats() {}
}
