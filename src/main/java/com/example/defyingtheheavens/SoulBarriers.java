package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A nascent soul passes through stone and wood as if they weren't there, but not through a formation's barrier, which is made of
 * qi as the soul is. One the barrier doesn't let through and that is weaker than the formation ({@link Formations#canBreak})
 * meets it like a wall: the client stops it at the shell (see ClientSoul), and here, should it get across anyway, it is set back
 * on its own side and loses {@link #BUMP_QI} of its pool. One strong enough to break the barrier may pass, but the formation
 * drains {@link #INSIDE_DRAIN_PER_SECOND} of its pool a second while it is inside.
 */
final class SoulBarriers {
	/** Within this of the shell, a soul that can't pass is set back. */
	static final double SHELL_BAND = 0.8;
	private static final double BUMP_QI = 0.05;
	private static final double INSIDE_DRAIN_PER_SECOND = 0.02;
	/** Ticks between two bumps' messages and costs. */
	private static final int BUMP_COOLDOWN = 10;

	/** Which side of each barrier each soul was last clearly on (true: inside). */
	private static final Map<UUID, Map<UUID, Boolean>> SIDES = new HashMap<>();
	private static final Map<UUID, Integer> LAST_BUMP = new HashMap<>();

	/** Every other tick, for each soul out of its body (and not inside a host: then the barrier deals with the host). */
	static void check(ServerPlayer soul, boolean possessing) {
		if (possessing) return;
		PlayerCultivation c = CultivationManager.get(soul);
		Vec3 at = soul.position().add(0, 1, 0);
		Map<UUID, Boolean> sides = SIDES.computeIfAbsent(soul.getUUID(), id -> new HashMap<>());
		for (Formation formation : FormationData.get(soul.serverLevel()).all()) {
			if (!formation.isRaised() || formation.allows(soul)) continue;
			Vec3 center = Vec3.atCenterOf(formation.center);
			double distance = at.distanceTo(center);
			double radius = formation.getRadius() + 0.5;
			if (distance > radius + 4) {
				sides.remove(formation.id);
				continue;
			}
			if (Formations.canBreak(soul, formation)) {
				if (distance < radius && c.getQi() > 0) {
					c.setQi(Math.max(0, c.getQi() - c.maxQi() * INSIDE_DRAIN_PER_SECOND / 10));
				}
				continue;
			}
			if (Math.abs(distance - radius) > SHELL_BAND) {
				sides.put(formation.id, distance < radius);
				continue;
			}
			// On the shell: back to the side it came from.
			boolean inside = sides.getOrDefault(formation.id, false);
			Vec3 out = at.subtract(center);
			out = out.lengthSqr() < 1.0e-4 ? new Vec3(0, 1, 0) : out.normalize();
			Vec3 to = center.add(out.scale(inside ? radius - SHELL_BAND - 1 : radius + SHELL_BAND + 1)).subtract(0, 1, 0);
			soul.connection.teleport(to.x, to.y, to.z, soul.getYRot(), soul.getXRot());
			int now = soul.tickCount;
			Integer last = LAST_BUMP.get(soul.getUUID());
			if (last == null || now - last > BUMP_COOLDOWN) {
				LAST_BUMP.put(soul.getUUID(), now);
				c.setQi(Math.max(0, c.getQi() - c.maxQi() * BUMP_QI));
				soul.displayClientMessage(Component.translatable(ModLang.MSG_SOUL_BARRIER), true);
				soul.serverLevel().playSound(null, soul.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.5f);
			}
		}
		CultivationManager.markDirty(soul.server);
	}

	static void forget(UUID soul) {
		SIDES.remove(soul);
		LAST_BUMP.remove(soul);
	}

	private SoulBarriers() {}
}
