package com.example.defyingtheheavens;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Consciousness Domain: a cultivator's consciousness reaches a sphere around them that grows with realm and stage
 * ({@link #radius}), from 10 blocks at Qi Refining Early to 160 (10 chunks) at Heavenly Being Grand Perfection, on an
 * exponential curve like the realms' own growth. Four Axis keeps the 10 chunks. Realm Suppress uses the same radius whether
 * or not this ability is switched on.
 * <p>
 * The ability itself costs no qi. While it is on, the client draws the domain, outlines every living thing inside it
 * through walls and labels cultivators with their realm. This server half tells each user, twice a second, the realm and
 * stage of the cultivators in their domain (a client never learns other players' cultivation otherwise) and which other
 * active domains touch theirs, and alerts both cultivators when two domains first touch. Another's open domain is seen by
 * anyone near it ({@link #SEEN_MARGIN}), their own domain open or not: the sphere only, nothing of what it senses.
 */
public final class ConsciousnessDomainHandler {
	public static final double BASE_RADIUS = 10;
	public static final double MAX_RADIUS = 160;
	/** The domain stops growing here (Heavenly Being Grand Perfection); it also keeps the 160-block scans affordable. */
	private static final int MAX_RANK = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION);
	private static final int SCAN_INTERVAL = 10;
	/**
	 * Two domains that touched only count as apart again once this many blocks open between them, so cultivators hovering
	 * at the edge don't set off the alert over and over.
	 */
	private static final double SEPARATION_MARGIN = 4;
	/**
	 * Another's open domain is seen (whether one's own is open or not) from inside it and from up to this far outside its shell:
	 * a domain is felt, not only met.
	 */
	public static final double SEEN_MARGIN = 48;

	/** A cultivator in someone's domain, as that someone senses them: the effective (displayed) realm and stage. */
	public record SensedCultivator(int entityId, Realm realm, Stage stage, boolean pressed) {}

	/** Another active domain touching the user's; the position is for clients that don't have its owner loaded. */
	public record TouchingDomain(int entityId, double x, double y, double z, float radius) {}

	/** Domain users whose domains touch, both directions stored. */
	private static Map<UUID, Set<UUID>> touching = new HashMap<>();

	/** Domain radius of a cultivator at {@code realm} - {@code stage}: 10 * 16^(rank / 19), capped at 160. */
	public static double radius(Realm realm, Stage stage) {
		int rank = Math.min(PlayerCultivation.rank(realm, stage), MAX_RANK);
		return BASE_RADIUS * Math.pow(MAX_RADIUS / BASE_RADIUS, rank / (double) MAX_RANK);
	}

	/**
	 * A player's domain, from where their body stands as wielded (either path: TwoPaths#wieldedRank; the heaven path's alone, before
	 * the 2026-10-20 sweep, left a ghost path's Heavenly Being a Qi Refining domain): a stronger cultivator's pressure narrows it.
	 */
	public static double radius(PlayerCultivation c) {
		if (c.hasPhysique(Physique.INNATE_CONSCIOUSNESS_BODY)) return MAX_RADIUS; // a Four Axis cultivator's from the first day (Physique)
		int rank = Math.max(0, TwoPaths.wieldedRank(c)), stages = Stage.values().length;
		return radius(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages));
	}

	/** Where a player is sensed to stand (by a domain, as the alert names it): their body as wielded, either path (TwoPaths#wieldedRank). */
	public static Stage sensedStage(PlayerCultivation c) {
		return Stage.byIndex(Math.max(0, TwoPaths.wieldedRank(c)) % Stage.values().length);
	}

	/** Where a domain is centred: the middle of its owner's body. */
	public static Vec3 center(Entity entity) {
		return entity.position().add(0, entity.getBbHeight() * 0.5, 0);
	}

	/**
	 * Entities of {@code type} matching {@code filter} whose middle lies within {@code radius} of {@code owner}'s, the owner
	 * excluded. One box query over the level's entity sections (only the sections the box covers are visited), then an
	 * exact sphere check.
	 */
	public static <T extends Entity> List<T> entitiesIn(Entity owner, double radius, Class<T> type, Predicate<? super T> filter) {
		Vec3 center = center(owner);
		double radiusSqr = radius * radius;
		List<T> found = new ArrayList<>();
		for (Entity entity : owner.level().getEntities(owner, new AABB(center, center).inflate(radius), type::isInstance)) {
			T candidate = type.cast(entity);
			if (entity.getBoundingBox().getCenter().distanceToSqr(center) <= radiusSqr && filter.test(candidate)) found.add(candidate);
		}
		return found;
	}

	/** The domain is open: switched on, alive, and not a spectator (who has no body for a consciousness to dwell in). */
	public static boolean isActive(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && CultivationManager.get(player).isAbilityActive(Ability.CONSCIOUSNESS_DOMAIN);
	}

	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % SCAN_INTERVAL != 0) return;
		Map<UUID, Set<UUID>> next = new HashMap<>();
		for (ServerLevel level : server.getAllLevels()) {
			List<ServerPlayer> users = new ArrayList<>();
			for (ServerPlayer player : level.players()) {
				if (isActive(player)) users.add(player);
			}
			if (users.isEmpty()) continue;
			// A nascent soul out of its body still sees through its domain, but touches no one's: it is only sensed as near (see
			// SoulProjection#senseDomains), never met.
			List<ServerPlayer> embodied = new ArrayList<>(users);
			embodied.removeIf(SoulProjection::isSoul);
			List<CultivatorNpc> npcs = CultivatorNpc.openDomains(level);
			updateContacts(embodied, next);
			updateNpcContacts(embodied, npcs, next);
			for (ServerPlayer viewer : users) sendDomain(viewer, embodied, npcs, next);
			// One whose own domain is shut still sees the open domains of others near (only the spheres: what is in them is the
			// domain's owner's to sense).
			for (ServerPlayer viewer : level.players()) {
				if (users.contains(viewer)) continue;
				List<TouchingDomain> near = new ArrayList<>();
				addNear(viewer, embodied, near);
				if (!near.isEmpty()) ModPackets.sendConsciousness(viewer, List.of(), near);
			}
		}
		touching = next; // pairs that split up across dimensions or went inactive are simply not carried over
	}

	/**
	 * NPC cultivators' open domains (rogues always, sect members in a fight) touch players' like any other: the player is
	 * alerted when one first touches theirs. A secluded cultivator's Concealment Barrier hides its domain entirely.
	 */
	private static void updateNpcContacts(List<ServerPlayer> users, List<CultivatorNpc> npcs, Map<UUID, Set<UUID>> next) {
		if (npcs.isEmpty()) return;
		for (ServerPlayer player : users) {
			double radius = radius(CultivationManager.get(player));
			for (CultivatorNpc npc : npcs) {
				double reach = radius + npc.domainRadius();
				double distance = center(player).distanceTo(center(npc));
				boolean was = touching.getOrDefault(player.getUUID(), Set.of()).contains(npc.getUUID());
				if (distance > reach && (!was || distance > reach + SEPARATION_MARGIN)) continue;
				next.computeIfAbsent(player.getUUID(), id -> new HashSet<>()).add(npc.getUUID());
				if (!was) ModPackets.sendConsciousnessAlert(player, npc.getId(), npc.describe().getString(), npc.getDisplayedRealm(), npc.getDisplayedStage());
			}
		}
	}

	/** Which pairs of domains in one level touch now; alerts both sides of each pair that has just met. */
	private static void updateContacts(List<ServerPlayer> users, Map<UUID, Set<UUID>> next) {
		for (int i = 0; i < users.size(); i++) {
			ServerPlayer a = users.get(i);
			double ra = radius(CultivationManager.get(a));
			for (int j = i + 1; j < users.size(); j++) {
				ServerPlayer b = users.get(j);
				double reach = ra + radius(CultivationManager.get(b));
				double distance = center(a).distanceTo(center(b));
				boolean was = touching.getOrDefault(a.getUUID(), Set.of()).contains(b.getUUID());
				if (distance > reach && (!was || distance > reach + SEPARATION_MARGIN)) continue;
				next.computeIfAbsent(a.getUUID(), id -> new HashSet<>()).add(b.getUUID());
				next.computeIfAbsent(b.getUUID(), id -> new HashSet<>()).add(a.getUUID());
				if (!was) {
					ModPackets.sendConsciousnessAlert(a, b, CultivationManager.get(b));
					ModPackets.sendConsciousnessAlert(b, a, CultivationManager.get(a));
				}
			}
		}
	}

	/** The cultivators inside {@code viewer}'s domain, and the domains touching it. */
	private static void sendDomain(ServerPlayer viewer, List<ServerPlayer> users, List<CultivatorNpc> npcs, Map<UUID, Set<UUID>> contacts) {
		double radius = radius(CultivationManager.get(viewer));
		List<SensedCultivator> cultivators = new ArrayList<>();
		for (Entity entity : entitiesIn(viewer, radius, Entity.class, e -> CultivatorEntity.isCultivator(e) && e.isAlive() && !e.isSpectator())) {
			if (entity instanceof ServerPlayer player) {
				if (SoulProjection.isSoul(player)) continue; // no outline, no realm: a soul is only sensed as near
				PlayerCultivation c = CultivationManager.get(player);
				if (c.isMortal()) continue; // shows as a mortal: an outline, no realm
				cultivators.add(new SensedCultivator(player.getId(), TwoPaths.wieldedRealm(c), sensedStage(c), c.isUnderPressure()));
			} else if (entity instanceof CultivatorEntity npc) {
				if (npc.isConcealed()) continue; // a Concealment Barrier masks it
				if (npc instanceof CultivatorNpc set && set.isVacant()) continue; // set aside, unseen, while a soul uses its body (Possession)
				cultivators.add(new SensedCultivator(entity.getId(), npc.getDisplayedRealm(), npc.getDisplayedStage(),
						RealmSuppressSystem.isPressed(entity)));
			}
		}
		List<TouchingDomain> domains = new ArrayList<>();
		Set<UUID> met = contacts.getOrDefault(viewer.getUUID(), Set.of());
		for (ServerPlayer other : users) {
			if (other == viewer || !met.contains(other.getUUID())) continue;
			Vec3 c = center(other);
			domains.add(new TouchingDomain(other.getId(), c.x, c.y, c.z, (float) radius(CultivationManager.get(other))));
		}
		for (CultivatorNpc npc : npcs) {
			if (!met.contains(npc.getUUID())) continue;
			Vec3 c = center(npc);
			domains.add(new TouchingDomain(npc.getId(), c.x, c.y, c.z, (float) npc.domainRadius()));
		}
		addNear(viewer, users, domains); // and those near that don't reach this one
		ModPackets.sendConsciousness(viewer, cultivators, domains); // the client works its own radius out from its synced stage
	}

	/**
	 * Adds to {@code domains} the open domains of {@code users} that {@code viewer} is inside or within {@link #SEEN_MARGIN} of,
	 * and not already there: only of those whose owner the viewer's client is sent at all (a soul, or one of the dead, is only
	 * for those who perceive them: SoulProjection#broadcastsTo).
	 */
	private static void addNear(ServerPlayer viewer, List<ServerPlayer> users, List<TouchingDomain> domains) {
		Vec3 at = center(viewer);
		for (ServerPlayer other : users) {
			if (other == viewer || !SoulProjection.broadcastsTo(other, viewer)) continue;
			boolean listed = false;
			for (TouchingDomain domain : domains) listed |= domain.entityId() == other.getId();
			if (listed) continue;
			double radius = radius(CultivationManager.get(other));
			Vec3 c = center(other);
			if (c.distanceTo(at) > radius + SEEN_MARGIN) continue;
			domains.add(new TouchingDomain(other.getId(), c.x, c.y, c.z, (float) radius));
		}
	}

	public static void forget(UUID id) {
		touching.remove(id);
		for (Set<UUID> others : touching.values()) others.remove(id);
	}

	private ConsciousnessDomainHandler() {}
}
