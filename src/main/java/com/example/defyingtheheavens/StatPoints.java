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
 * governs. A point spent is spent for good; should cultivation fall back below what was spent (a golden core detonated, the
 * Chief Judge's gaze), only the points the lost stages gave are taken back, the last put in first ({@link #settle}). The Stats
 * tab's Points page spends them and draws their spread as a hexagon.
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
		/** Every path's pool: the qi pool, the ghostly qi pool ({@link #poolFactor}). */
		SPIRIT("spirit", 0.01),
		/** Every path's cultivation gathered meditating ({@link #cultivationFactor}), and its qi gathered, half as much ({@link #gatherFactor}). */
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

	/**
	 * Points {@code c} has earned (from the higher path's true rank, alive or dead: not the body's, which goes by its path's share,
	 * TwoPaths#pathShare; a mortal on no path, none); a Hundred Refinements Body one more for every breakthrough (Physique); and one for each
	 * Ancient Essence taken (a secret realm's: AncientEssenceItem), kept whatever the cultivation does.
	 */
	public static int earned(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, false);
		return earned(rank) + (c.hasPhysique(Physique.HUNDRED_REFINEMENTS_BODY) ? Math.max(0, rank) : 0) + c.essencePoints();
	}

	/** Points {@code c} has left to spend. */
	public static int unspent(PlayerCultivation c) {
		return Math.max(0, earned(c) - c.statPointsSpent());
	}

	/** {@code player} puts {@code count} points into {@code stat} (as many as they have, at most). */
	public static void spend(ServerPlayer player, Stat stat, int count) {
		if (stat == null || count <= 0) return;
		// Not a body's points by the soul in it (Possession): they are its owner's to place, and placed for good (a soul spent them
		// before the sweep of 2026-10-23).
		if (Possession.inhabits(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BUSY), true);
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		int spend = Math.min(count, unspent(c));
		if (spend <= 0) return;
		c.setStatPoints(stat, c.getStatPoints(stat) + spend);
		CultivationManager.refresh(player);
	}

	/**
	 * Cultivation fallen below what was spent: the points it no longer earns are taken back, the last put in first, and the rest
	 * stay where they were put (the user's rule, 2026-10-19: points are locked in; a fall loses only what the stages lost gave).
	 * They are told how many. Climbing back earns them again to spend. Returns whether any were taken. (Called whenever the body is
	 * refreshed.)
	 */
	public static boolean settle(ServerPlayer player, PlayerCultivation c) {
		int over = c.statPointsSpent() - earned(c);
		if (over <= 0) return false;
		for (int i = 0; i < over; i++) c.takeBackLastStatPoint();
		player.displayClientMessage(Component.translatable(ModLang.MSG_STAT_POINTS_RETURNED, over).withStyle(ChatFormatting.GOLD), false);
		return true;
	}

	/** The share more that {@code c}'s points in {@code stat} give (0.12: twelve percent). */
	public static double bonus(PlayerCultivation c, Stat stat) {
		return c.getStatPoints(stat) * stat.perPoint;
	}

	// The cultivation stats are the cultivator's, not one path's (the user's rule, 2026-10-19: "i want the stats buffs to share
	// between the two paths (and for other paths that we add later)"). Every path's pool, gathering and cultivation goes through
	// these: the heaven path's (PlayerCultivation), the ghost path's (GhostQi, GhostMethods), and any path to come.

	/** What every path's pool is multiplied by: Spirit. */
	public static double poolFactor(PlayerCultivation c) {
		return 1 + bonus(c, Stat.SPIRIT);
	}

	/**
	 * What every path's cultivation gathered meditating is multiplied by: Insight (its methods' layers settle the quicker with it),
	 * and an obsession with Enlightenment (Obsession#cultivationFactor), the cultivator's as the stats are.
	 */
	public static double cultivationFactor(PlayerCultivation c) {
		return (1 + bonus(c, Stat.INSIGHT)) * Obsession.cultivationFactor(c);
	}

	/** What every path's qi gathered (qi, ghostly qi) is multiplied by: half Insight's share. */
	public static double gatherFactor(PlayerCultivation c) {
		return 1 + bonus(c, Stat.INSIGHT) / 2;
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
