package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * Stat points (the user's ask, 2026-10-17): every breakthrough of a stage gives {@link #STAGE_POINTS} points to put into the
 * body's six stats, a breakthrough of a realm {@link #REALM_POINTS}; they are counted from the cultivation reached (the higher
 * path's, TwoPaths), so a cultivator who was already far along has theirs at once. Each point is a share more of what its stat
 * governs. Should cultivation fall back below what was spent (a golden core detonated, the Chief Judge's gaze), every point is
 * given back to spend again. The Stats tab's Points page spends them and draws their spread as a hexagon.
 */
public final class StatPoints {
	public static final int STAGE_POINTS = 2, REALM_POINTS = 5;

	/** The six stats, each with the share a point adds (as a fraction) to what it governs. Saved and synced by place: new ones go at the end. */
	public enum Stat {
		/** Melee harm. */
		STRENGTH("strength", 0.01),
		/** Maximum health. */
		CONSTITUTION("constitution", 0.01),
		/** Walking speed and attack speed. */
		AGILITY("agility", 0.005),
		/** Armour and its toughness. */
		ENDURANCE("endurance", 0.01),
		/** The qi pool. */
		SPIRIT("spirit", 0.01),
		/** Cultivation gathered meditating, and qi gathered (half as much). */
		INSIGHT("insight", 0.01);

		private final String id;
		public final double perPoint;

		Stat(String id, double perPoint) {
			this.id = id;
			this.perPoint = perPoint;
		}

		public String getId() { return id; }

		public Component getDisplayName() { return Component.translatable(ModLang.STAT_POINT_PREFIX + id); }

		/** What {@code points} in it give, said ("+12% melee harm"). */
		public Component describe(int points) {
			return Component.translatable(ModLang.STAT_POINT_PREFIX + id + ".effect", Math.round(points * perPoint * 1000) / 10.0);
		}

		public static Stat byIndex(int i) {
			Stat[] all = values();
			return i >= 0 && i < all.length ? all[i] : null;
		}
	}

	private static final UUID STRENGTH_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b01");
	private static final UUID CONSTITUTION_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b02");
	private static final UUID AGILITY_SPEED_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b03");
	private static final UUID AGILITY_ATTACK_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b04");
	private static final UUID ENDURANCE_ARMOR_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b05");
	private static final UUID ENDURANCE_TOUGHNESS_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b06");

	/** Points earned by reaching {@code rank} (PlayerCultivation#rank): {@link #STAGE_POINTS} a stage, {@link #REALM_POINTS} a realm's first stage. */
	public static int earned(int rank) {
		int points = 0;
		int stages = Stage.values().length;
		for (int r = 1; r <= rank; r++) points += r % stages == 0 ? REALM_POINTS : STAGE_POINTS;
		return points;
	}

	/** Points {@code c} has earned (from the higher path's true rank; a mortal on no path, none). */
	public static int earned(PlayerCultivation c) {
		TwoPaths.Body body = TwoPaths.body(c, false);
		if (c.isMortal() && !body.ghostPath()) return 0;
		return earned(body.rank());
	}

	/** Points {@code c} has left to spend. */
	public static int unspent(PlayerCultivation c) {
		return Math.max(0, earned(c) - c.statPointsSpent());
	}

	/** {@code player} puts {@code count} points into {@code stat} (as many as they have, at most). */
	public static void spend(ServerPlayer player, Stat stat, int count) {
		if (stat == null || count <= 0) return;
		PlayerCultivation c = CultivationManager.get(player);
		int spend = Math.min(count, unspent(c));
		if (spend <= 0) return;
		c.setStatPoints(stat, c.getStatPoints(stat) + spend);
		CultivationManager.refresh(player);
	}

	/**
	 * Cultivation fallen below what was spent: every point is given back to spend again, and they are told. Returns whether it was.
	 * (Called whenever the body is refreshed.)
	 */
	public static boolean settle(ServerPlayer player, PlayerCultivation c) {
		if (c.statPointsSpent() <= earned(c)) return false;
		c.clearStatPoints();
		player.displayClientMessage(Component.translatable(ModLang.MSG_STAT_POINTS_RETURNED).withStyle(ChatFormatting.GOLD), false);
		return true;
	}

	/** The share more that {@code c}'s points in {@code stat} give (0.12: twelve percent). */
	public static double bonus(PlayerCultivation c, Stat stat) {
		return c.getStatPoints(stat) * stat.perPoint;
	}

	/** (Re)applies the attribute modifiers of {@code c}'s points (CultivationStats#apply; the qi pool and gathering read them directly). */
	public static void apply(ServerPlayer player, PlayerCultivation c) {
		settle(player, c);
		set(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID, "Stat points: strength", bonus(c, Stat.STRENGTH));
		set(player, Attributes.MAX_HEALTH, CONSTITUTION_ID, "Stat points: constitution", bonus(c, Stat.CONSTITUTION));
		set(player, Attributes.MOVEMENT_SPEED, AGILITY_SPEED_ID, "Stat points: agility", bonus(c, Stat.AGILITY));
		set(player, Attributes.ATTACK_SPEED, AGILITY_ATTACK_ID, "Stat points: agility", bonus(c, Stat.AGILITY));
		set(player, Attributes.ARMOR, ENDURANCE_ARMOR_ID, "Stat points: endurance", bonus(c, Stat.ENDURANCE));
		set(player, Attributes.ARMOR_TOUGHNESS, ENDURANCE_TOUGHNESS_ID, "Stat points: endurance", bonus(c, Stat.ENDURANCE));
		if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
	}

	private static void set(LivingEntity entity, Attribute attribute, UUID id, String name, double share) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		if (share != 0) instance.addPermanentModifier(new AttributeModifier(id, name, share, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	private StatPoints() {}
}
