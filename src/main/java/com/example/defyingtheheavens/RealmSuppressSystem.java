package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Realm Suppress, switched on from the Abilities tab (off by default): the weight of the cultivator's realm presses down on
 * every weaker being within their consciousness domain's radius, whether or not the Consciousness Domain ability itself is
 * on. Rechecked twice a second:
 * <ul>
 *   <li>Mortals (vanilla mobs and anything else that isn't a {@link CultivatorEntity}) lose half their max health, attack
 *   damage and speed.</li>
 *   <li>Lower cultivators in the same realm lose {@link #STAGE_PENALTY} of those per stage of the gap, and the same fraction
 *   of their qi pool and gathering.</li>
 *   <li>Cultivators a realm or more below lose a whole realm off their effective stage (their displayed realm, qi pool, qi
 *   gathering and realm bonuses all drop with it) plus {@link #REALM_PENALTY} (+{@link #EXTRA_REALM_PENALTY} per further
 *   realm, at most {@link #MAX_REALM_PENALTY}) of max health, attack and speed: about 40-60% in all.</li>
 *   <li>Equal and stronger cultivators are untouched.</li>
 * </ul>
 * Cultivators are compared by their true rank as the land they stand in allows it (a player's by either path, its body's:
 * TwoPaths#weighedRank), so a Four Axis cultivator in the lower realms counts as Heavenly Being Grand Perfection, and pressure on a
 * suppressor doesn't weaken their own.
 * <p>
 * Every target costs the suppressor qi each second, charged by {@link QiManager} after Qi Flight: a mortal
 * {@link #MORTAL_COST_PER_HEALTH} per point of its base max health, a cultivator {@link #CULTIVATOR_COST} times its own qi
 * gathering divided by the stage gap (holding down a near-equal is costly, someone far below is cheap). Running dry releases
 * everyone; the pressure returns once the pool is back to {@link #RESUME_FRACTION}.
 * <p>
 * The penalty is transient attribute modifiers (never saved) and, for players, {@link PlayerCultivation}'s pressure (never
 * saved). Leaving range, the suppressor switching it off, dying, logging out or running dry lifts it at once, and each
 * target keeps the fraction of health it had. Where several suppressors reach one target, the strongest pressure applies.
 */
public final class RealmSuppressSystem {
	private static final UUID HEALTH_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b01");
	private static final UUID DAMAGE_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b02");
	private static final UUID SPEED_ID = UUID.fromString("5f1e2a10-8b3c-4d77-a1e0-0c1d2e3f4b03");
	private static final Set<UUID> MODIFIER_IDS = Set.of(HEALTH_ID, DAMAGE_ID, SPEED_ID);

	public static final double MORTAL_PENALTY = 0.5;
	public static final double STAGE_PENALTY = 0.05;
	public static final double REALM_PENALTY = 0.25;
	public static final double EXTRA_REALM_PENALTY = 0.05;
	public static final double MAX_REALM_PENALTY = 0.40;
	/** Qi per second for each point of a mortal's base max health: a zombie (20) costs 1/s, an iron golem (100) 5/s. */
	public static final double MORTAL_COST_PER_HEALTH = 0.05;
	/** Times the target's own qi gathering, divided by the stage gap. */
	public static final double CULTIVATOR_COST = 2.0;
	/** After running dry, the pressure returns (silently) at this fraction of the pool. */
	public static final double RESUME_FRACTION = 0.05;
	private static final int SCAN_INTERVAL = 10;
	/** Half a scan after the Consciousness Domain's, so the two big scans don't land on the same tick. */
	private static final int SCAN_OFFSET = 5;
	/** Players are told about pressed entities this close to them, for the pressure particles and marker. */
	private static final double VISUAL_RANGE = 64;

	/** What one suppressor does to one target: stages off the effective stage, and the fraction taken off stats and qi. */
	public record Pressure(int stages, double penalty) {
		boolean strongerThan(Pressure other) {
			return stages != other.stages ? stages > other.stages : penalty > other.penalty;
		}
	}

	/** A pressed entity as sent to clients nearby, for drawing the pressure on it. */
	public record PressedVisual(int entityId, float penalty, boolean heavy) {}

	private record Pressed(LivingEntity entity, Pressure pressure) {}

	/** Everything under pressure now, by entity UUID. */
	private static final Map<UUID, Pressed> PRESSED = new HashMap<>();
	/** Qi per second each suppressor's current targets cost. */
	private static final Map<UUID, Double> UPKEEP = new HashMap<>();
	/** Suppressors whose qi ran dry and who haven't gathered back to {@link #RESUME_FRACTION} yet. */
	private static final Set<UUID> EXHAUSTED = new HashSet<>();
	/** Players last sent a non-empty list of pressed entities (they get one empty list when it clears). */
	private static final Set<UUID> SAW_PRESSURE = new HashSet<>();
	/** Something changed that shouldn't wait for the next scan; at most one extra scan a tick, however many changes. */
	private static boolean rescan;
	/** Which players press each entity now (target UUID to suppressor UUIDs), rebuilt every scan: who it kneels to (TalkSystem). */
	private static final Map<UUID, Set<UUID>> PRESSERS = new HashMap<>();
	/** Entities a player has let up off (TalkSystem's Release pressure), each until a game time. */
	private static final Map<UUID, Map<UUID, Long>> RELEASED = new HashMap<>();
	/** How long a released one is left alone by that player's Realm Suppress (five minutes). */
	public static final long RELEASE_TICKS = 6000;

	/** {@code player}'s Realm Suppress is pressing down on {@code target} right now. */
	public static boolean presses(ServerPlayer player, Entity target) {
		Set<UUID> pressers = PRESSERS.get(target.getUUID());
		return pressers != null && pressers.contains(player.getUUID());
	}

	/** {@code player} lets up off {@code target} for {@link #RELEASE_TICKS}: their Realm Suppress passes it over. */
	public static void spare(ServerPlayer player, Entity target) {
		RELEASED.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(target.getUUID(), player.level().getGameTime() + RELEASE_TICKS);
		refreshNow();
	}

	/**
	 * {@code player} switched Realm Suppress (off or on): whoever they let up off is under it again the next time it's on. A
	 * release is a mercy for this pressing, not a lasting pass.
	 */
	public static void forgetReleases(ServerPlayer player) {
		RELEASED.remove(player.getUUID());
	}

	private static boolean released(ServerPlayer suppressor, Entity target) {
		Map<UUID, Long> spared = RELEASED.get(suppressor.getUUID());
		if (spared == null) return false;
		Long until = spared.get(target.getUUID());
		if (until == null) return false;
		if (until > suppressor.level().getGameTime()) return true;
		spared.remove(target.getUUID());
		return false;
	}

	/** True for the attribute modifiers this ability adds (lets the client tell pressure apart from gear and effects). */
	public static boolean isPressureModifier(UUID id) {
		return MODIFIER_IDS.contains(id);
	}

	public static boolean isPressed(Entity entity) {
		return PRESSED.containsKey(entity.getUUID());
	}

	/** The attributes the pressure takes its fraction off. */
	public static boolean lowers(Attribute attribute) {
		return attribute == Attributes.MAX_HEALTH || attribute == Attributes.ATTACK_DAMAGE || attribute == Attributes.MOVEMENT_SPEED;
	}

	public static void tick(MinecraftServer server) {
		boolean scheduled = server.getTickCount() % SCAN_INTERVAL == SCAN_OFFSET;
		if (!scheduled && !rescan) return;
		rescan = false;
		if (scheduled) {
			EXHAUSTED.removeIf(id -> {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player == null) return true;
				PlayerCultivation c = CultivationManager.get(player);
				return c.getQi() >= c.maxQi() * RESUME_FRACTION;
			});
		}
		scan(server, null);
		if (scheduled) sendVisuals(server);
	}

	/**
	 * Re-evaluates every suppressor at the end of this tick rather than at the next scan: after one switches the ability,
	 * dies or runs dry, everyone they held is released straight away. Coalesced, so a flood of switch packets costs one scan
	 * a tick at most.
	 */
	public static void refreshNow() {
		rescan = true;
	}

	/**
	 * Qi per second the player's Realm Suppress costs this tick, called by {@link QiManager} after Qi Flight claimed
	 * {@code alreadySpending}. If the pool can't cover it, the pressure lifts from everyone the player holds.
	 */
	static double upkeep(ServerPlayer player, PlayerCultivation c, double alreadySpending) {
		Double cost = UPKEEP.get(player.getUUID());
		// Switched off since the last scan: nothing is charged while the release waits for this tick's rescan.
		if (cost == null || cost <= 0 || !isSuppressing(player, c)) return 0;
		if (c.getQi() + (c.qiGatherPerSecond() - alreadySpending - cost) / 20.0 >= 0) return cost;
		EXHAUSTED.add(player.getUUID());
		UPKEEP.remove(player.getUUID());
		player.displayClientMessage(Component.translatable(ModLang.MSG_SUPPRESS_EXHAUSTED, (int) (RESUME_FRACTION * 100)), true);
		refreshNow(); // QiManager runs before this system's tick, so the pressure lifts this same tick
		return 0;
	}

	/** Pressing down on others: switched on, alive, in the world, with qi to spare. */
	private static boolean isSuppressing(ServerPlayer player, PlayerCultivation c) {
		return player.isAlive() && !player.isSpectator() && c.isAbilityActive(Ability.REALM_SUPPRESS) && !EXHAUSTED.contains(player.getUUID())
				&& !SoulProjection.isSoul(player) // a soul out of its body has no realm to bear down with
				&& !Incorporeal.isGhost(player); // nor have the dead
	}

	/**
	 * Finds the strongest pressure on every entity in reach of a suppressor, then lifts it from whatever is no longer pressed
	 * and applies new or changed pressure. {@code leaving} (a player logging out) is neither suppressor nor target.
	 */
	private static void scan(MinecraftServer server, UUID leaving) {
		Map<UUID, Pressed> next = new HashMap<>();
		Map<UUID, Set<UUID>> pressers = new HashMap<>();
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer suppressor : level.players()) {
				PlayerCultivation c = CultivationManager.get(suppressor);
				if (suppressor.getUUID().equals(leaving) || !isSuppressing(suppressor, c)) {
					UPKEEP.remove(suppressor.getUUID());
					continue;
				}
				int rank = TwoPaths.weighedRank(c); // either path's (its body's), as it is weighed (rankOf)
				double cost = 0;
				List<LivingEntity> inReach = ConsciousnessDomainHandler.entitiesIn(suppressor, ConsciousnessDomainHandler.radius(c),
						LivingEntity.class, target -> canBePressed(target, suppressor, leaving));
				for (LivingEntity target : inReach) {
					int targetRank = rankOf(target);
					Pressure pressure = pressureOn(rank, targetRank);
					if (pressure == null) continue;
					cost += costOf(rank, targetRank, target);
					pressers.computeIfAbsent(target.getUUID(), id -> new HashSet<>()).add(suppressor.getUUID());
					Pressed current = next.get(target.getUUID());
					if (current == null || pressure.strongerThan(current.pressure())) next.put(target.getUUID(), new Pressed(target, pressure));
				}
				if (cost > 0) {
					UPKEEP.put(suppressor.getUUID(), cost);
				} else {
					UPKEEP.remove(suppressor.getUUID());
				}
			}
			// NPC cultivators press down only in a fight, and only on the foes they're fighting (see CultivatorNpc); they pay
			// the upkeep out of their own pools, reading it back with upkeepOf.
			for (CultivatorNpc suppressor : CultivatorNpc.suppressors(level)) {
				int rank = suppressor.sustainedRank();
				double cost = 0;
				List<LivingEntity> inReach = ConsciousnessDomainHandler.entitiesIn(suppressor, suppressor.domainRadius(), LivingEntity.class,
						target -> suppressor.isHostileTo(target) && canBePressed(target, suppressor, leaving));
				for (LivingEntity target : inReach) {
					int targetRank = rankOf(target);
					Pressure pressure = pressureOn(rank, targetRank);
					if (pressure == null) continue;
					cost += costOf(rank, targetRank, target);
					Pressed current = next.get(target.getUUID());
					if (current == null || pressure.strongerThan(current.pressure())) next.put(target.getUUID(), new Pressed(target, pressure));
				}
				if (cost > 0) {
					UPKEEP.put(suppressor.getUUID(), cost);
				} else {
					UPKEEP.remove(suppressor.getUUID());
				}
			}
		}
		// NPCs that stopped (or left) are no longer charged.
		UPKEEP.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null && !npcSuppressing(server, id));
		PRESSERS.clear();
		PRESSERS.putAll(pressers);

		for (Iterator<Map.Entry<UUID, Pressed>> it = PRESSED.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Pressed> entry = it.next();
			Pressed now = next.get(entry.getKey());
			// Also when the same UUID is a new entity now (a respawned player): the old one is released first.
			if (now == null || now.entity() != entry.getValue().entity()) {
				release(server, entry.getValue().entity());
				it.remove();
			}
		}
		for (Map.Entry<UUID, Pressed> entry : next.entrySet()) {
			Pressed before = PRESSED.get(entry.getKey());
			if (before == null || !before.pressure().equals(entry.getValue().pressure())) {
				apply(entry.getValue().entity(), entry.getValue().pressure());
				PRESSED.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private static boolean canBePressed(LivingEntity target, Entity suppressor, UUID leaving) {
		if (!target.isAlive() || target.isRemoved() || target instanceof ArmorStand || target.getUUID().equals(leaving)) return false;
		if (suppressor instanceof ServerPlayer player && released(player, target)) return false; // let up off (Release pressure)
		if (target instanceof ServerPlayer player) return !player.isSpectator() && !player.isCreative() && !Incorporeal.untouchable(player);
		// A player's body left sitting or standing (a projected soul's, a clone's or the main body's, an Inner Realm meditator's) isn't
		// a mortal's to press: weighed as one, its health was halved, and its owner came back into it so (the sweep of 2026-10-23).
		if (target instanceof SoulBodyEntity || target instanceof CloneBodyEntity || target instanceof InnerBodyEntity) return false;
		// Map makers' invulnerable NPCs stay as they are, and a cultivator's own companions are spared.
		if (target.isInvulnerable()) return false;
		return !(target instanceof OwnableEntity pet) || !suppressor.getUUID().equals(pet.getOwnerUUID());
	}

	/** A cultivator's rank as Realm Suppress weighs it, or -1 for a mortal: a player's by either path (TwoPaths#weighedRank, its body's). */
	private static int rankOf(LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			PlayerCultivation c = CultivationManager.get(player);
			return c.isMortal() ? -1 : TwoPaths.weighedRank(c); // a player who hasn't begun cultivating is weighed as a mortal
		}
		if (entity instanceof CultivatorEntity npc && !npc.isMortal()) return PlayerCultivation.rank(npc.getCultivationRealm(), npc.getCultivationStage());
		return -1;
	}

	/** Qi per second an NPC's Realm Suppress costs it right now (0 when it holds nobody down). */
	public static double upkeepOf(UUID id) {
		Double cost = UPKEEP.get(id);
		return cost == null ? 0 : cost;
	}

	private static boolean npcSuppressing(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(id) instanceof CultivatorNpc npc && npc.isSuppressing()) return true;
		}
		return false;
	}

	/** What a suppressor of {@code rank} does to a target of {@code targetRank} (-1: mortal); null if the target is immune. */
	public static Pressure pressureOn(int rank, int targetRank) {
		if (targetRank < 0) return new Pressure(0, MORTAL_PENALTY);
		int gap = rank - targetRank;
		if (gap <= 0) return null;
		int stages = Stage.values().length;
		int realmGap = rank / stages - targetRank / stages;
		if (realmGap == 0) return new Pressure(0, STAGE_PENALTY * gap);
		return new Pressure(stages, Math.min(MAX_REALM_PENALTY, REALM_PENALTY + EXTRA_REALM_PENALTY * (realmGap - 1)));
	}

	/** Qi per second it costs a suppressor of {@code rank} to hold down {@code target}. */
	private static double costOf(int rank, int targetRank, LivingEntity target) {
		if (targetRank < 0) return MORTAL_COST_PER_HEALTH * target.getAttributeBaseValue(Attributes.MAX_HEALTH);
		int stages = Stage.values().length;
		double gather = CultivationStats.qiGather(Realm.byIndex(targetRank / stages), Stage.byIndex(targetRank % stages));
		return CULTIVATOR_COST * gather / (rank - targetRank);
	}

	private static void apply(LivingEntity entity, Pressure pressure) {
		float oldMax = entity.getMaxHealth();
		float oldHealth = entity.getHealth();
		setModifier(entity, Attributes.MAX_HEALTH, HEALTH_ID, "Realm Suppress max health", pressure.penalty());
		setModifier(entity, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "Realm Suppress attack damage", pressure.penalty());
		setModifier(entity, Attributes.MOVEMENT_SPEED, SPEED_ID, "Realm Suppress speed", pressure.penalty());
		if (entity instanceof ServerPlayer player) {
			// Effective stage, realm bonuses and the qi pool follow the pressure; refresh re-applies stats and syncs.
			if (CultivationManager.get(player).setPressure(pressure.stages(), pressure.penalty())) CultivationManager.refresh(player);
		} else if (entity instanceof CultivatorEntity npc) {
			npc.onRealmPressure(pressure.stages(), pressure.penalty());
		}
		keepHealthFraction(entity, oldMax, oldHealth);
	}

	/** Restores everything the pressure took. An entity that died or left only has its owner's cultivation cleared. */
	private static void release(MinecraftServer server, LivingEntity entity) {
		boolean present = entity.isAlive() && !entity.isRemoved();
		float oldMax = entity.getMaxHealth();
		float oldHealth = entity.getHealth();
		for (Attribute attribute : new Attribute[] {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED}) {
			AttributeInstance instance = entity.getAttribute(attribute);
			if (instance == null) continue;
			for (UUID id : MODIFIER_IDS) instance.removeModifier(id);
		}
		if (entity instanceof ServerPlayer player) {
			// By UUID: after a respawn the player list holds a new entity for the same cultivator. Fake players (other mods'
			// machines) aren't in the list at all; theirs is the entity itself.
			ServerPlayer live = server.getPlayerList().getPlayer(player.getUUID());
			if (live == null && present) live = player;
			if (CultivationManager.get(player).setPressure(0, 0) && live != null) CultivationManager.refresh(live);
		} else if (entity instanceof CultivatorEntity npc) {
			npc.onRealmPressure(0, 0);
		}
		if (present) keepHealthFraction(entity, oldMax, oldHealth);
	}

	private static void setModifier(LivingEntity entity, Attribute attribute, UUID id, String name, double penalty) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, name, -penalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	/** {@code entity}'s max health as it would stand unpressed (the pressure takes its share of it while it lasts). */
	public static float unpressedMaxHealth(LivingEntity entity) {
		AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
		AttributeModifier pressure = health == null ? null : health.getModifier(HEALTH_ID);
		if (pressure == null || pressure.getAmount() <= -1) return entity.getMaxHealth();
		return (float) (entity.getMaxHealth() / (1 + pressure.getAmount()));
	}

	/** Max health moved: keep the same fraction of it, so pressure neither heals nor wounds by itself. */
	private static void keepHealthFraction(LivingEntity entity, float oldMax, float oldHealth) {
		if (entity.isAlive() && oldMax > 0 && entity.getMaxHealth() != oldMax) {
			entity.setHealth(oldHealth * entity.getMaxHealth() / oldMax);
		}
	}

	/** Tells each player about the pressed entities near them; clients draw the pressure with those. */
	private static void sendVisuals(MinecraftServer server) {
		for (ServerLevel level : server.getAllLevels()) {
			for (ServerPlayer player : level.players()) {
				List<PressedVisual> near = new ArrayList<>();
				for (Pressed pressed : PRESSED.values()) {
					LivingEntity entity = pressed.entity();
					if (entity.level() != level || entity.distanceToSqr(player) > VISUAL_RANGE * VISUAL_RANGE) continue;
					near.add(new PressedVisual(entity.getId(), (float) pressed.pressure().penalty(), pressed.pressure().stages() > 0));
				}
				boolean sawBefore = near.isEmpty() ? SAW_PRESSURE.remove(player.getUUID()) : !SAW_PRESSURE.add(player.getUUID());
				if (!near.isEmpty() || sawBefore) ModPackets.sendPressedEntities(player, near);
			}
		}
	}

	/** A player is logging out: lift their pressure (before the save) and everything they held. */
	public static void onDisconnect(ServerPlayer player) {
		scan(player.server, player.getUUID());
		EXHAUSTED.remove(player.getUUID());
		SAW_PRESSURE.remove(player.getUUID());
	}

	/**
	 * A respawned player starts unpressed: the new entity may have copied the old one's transient modifiers (leaving the End
	 * copies attributes whole), and the cultivation's pressure is cleared before stats are applied. The next scan presses
	 * them again if they're still in reach.
	 */
	public static void onRespawn(ServerPlayer player) {
		for (Attribute attribute : new Attribute[] {Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.MOVEMENT_SPEED}) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance == null) continue;
			for (UUID id : MODIFIER_IDS) instance.removeModifier(id);
		}
		CultivationManager.get(player).setPressure(0, 0);
		PRESSED.remove(player.getUUID());
	}

	/** Server stopping: everyone is restored before players and chunks are saved. */
	public static void releaseAll(MinecraftServer server) {
		for (Pressed pressed : PRESSED.values()) release(server, pressed.entity());
		PRESSED.clear();
		UPKEEP.clear();
		EXHAUSTED.clear();
		SAW_PRESSURE.clear();
	}

	private RealmSuppressSystem() {}
}
