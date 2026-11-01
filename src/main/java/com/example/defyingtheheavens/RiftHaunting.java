package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rift Haunting (CultivationMethod#RIFT_HAUNTING; the methods overhaul's phase 4): the ghost path's way at Void Shattering. Near a rift in space
 * (a spatial rift, the Yellow Springs' rift, a Spatial Tear standing open: within {@link #NEAR}), the souls lost between the worlds slip out to
 * one who practises it, one every {@link #EVERY} ticks, drifting away from the rift (glowing to it alone, gone after {@link #LIFE}); it seizes
 * the nearest within SoulRiverFeast#REACH with the Circulate key (ModPackets#SEIZE_SOUL), as the Soul River Feast's souls are seized: their
 * realms rolled as the river's, their struggle the same (SoulRiverFeast#take), each worth {@link #WORTH} of what the river's would be (a rift
 * is there for as long as one cares to sit by it; a journey on the river ends).
 */
public final class RiftHaunting {
	public static final int EVERY = 400, LIFE = 200, NEAR = 8;
	public static final double WORTH = 0.5, DRIFT = 0.025;

	/** A soul slipped out of a rift: where, which way it drifts (a tick), of what realm (ordinal), since when (game time). */
	record Drift(Vec3 from, Vec3 way, int realm, long born) {
		Vec3 at(long now) { return from.add(way.scale(now - born)); }
	}

	/** Each haunter's rift (where its souls come from), its souls, and the next one's time. */
	private static final class Haunt {
		Vec3 rift;
		long next;
		final List<Drift> souls = new ArrayList<>();
	}

	private static final Map<UUID, Haunt> HAUNTS = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();

	/** Whether {@code p} haunts the rifts: its ghost path begun, practising Rift Haunting, holding its volume. */
	public static boolean haunts(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.RIFT_HAUNTING
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** The middle of a rift within {@link #NEAR} of {@code p}, or null: a rift block's, or a Spatial Tear's. */
	static Vec3 riftNear(ServerPlayer p) {
		ServerLevel level = p.serverLevel();
		BlockPos at = p.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-NEAR, -NEAR, -NEAR), at.offset(NEAR, NEAR, NEAR))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.SPATIAL_RIFT) || state.is(ModBlocks.YELLOW_SPRINGS_RIFT)) return Vec3.atCenterOf(pos).add(0, 0.7, 0);
		}
		List<SpatialTearEntity> tears = level.getEntitiesOfClass(SpatialTearEntity.class, new AABB(at).inflate(NEAR), tear -> tear.isAlive());
		return tears.isEmpty() ? null : tears.get(0).position().add(0, 1.2, 0);
	}

	/** A second of {@code p}'s (NetherworldManager#second): whether it haunts a rift now, and which. */
	static void second(ServerPlayer p) {
		Vec3 rift = haunts(p) ? riftNear(p) : null;
		if (rift == null) {
			HAUNTS.remove(p.getUUID());
			return;
		}
		Haunt haunt = HAUNTS.computeIfAbsent(p.getUUID(), id -> {
			Haunt h = new Haunt();
			h.next = p.serverLevel().getGameTime() + EVERY / 4;
			return h;
		});
		haunt.rift = rift;
	}

	/** Each tick: each haunter's souls slip out when their time comes, drift, glow to it alone, and fade. */
	public static void tick(MinecraftServer server) {
		if (HAUNTS.isEmpty()) return;
		for (Map.Entry<UUID, Haunt> entry : new ArrayList<>(HAUNTS.entrySet())) {
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				HAUNTS.remove(entry.getKey());
				continue;
			}
			Haunt haunt = entry.getValue();
			ServerLevel level = p.serverLevel();
			long now = level.getGameTime();
			haunt.souls.removeIf(soul -> now - soul.born() > LIFE);
			if (now >= haunt.next && haunt.rift != null) {
				haunt.next = now + EVERY;
				double a = RANDOM.nextDouble() * Math.PI * 2;
				Vec3 way = new Vec3(Math.cos(a), 0.15 + RANDOM.nextDouble() * 0.2, Math.sin(a)).normalize().scale(DRIFT);
				haunt.souls.add(new Drift(haunt.rift, way, SoulRiverFeast.roll(pathRealm(p)), now));
				level.sendParticles(p, ParticleTypes.REVERSE_PORTAL, true, haunt.rift.x, haunt.rift.y, haunt.rift.z, 12, 0.2, 0.3, 0.2, 0.05);
			}
			if (now % 3 != 0) continue;
			int path = pathRealm(p);
			for (Drift soul : haunt.souls) {
				Vec3 at = soul.at(now);
				level.sendParticles(p, SoulRiverFeast.fightsBack(soul.realm(), path) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, true,
						at.x, at.y, at.z, 2, 0.12, 0.15, 0.12, 0.01);
			}
		}
	}

	private static int pathRealm(ServerPlayer p) {
		Realm realm = GhostMethods.ghostRealm(CultivationManager.get(p).netherworld());
		return realm == null ? 0 : realm.ordinal();
	}

	/** {@code p} seizes the nearest soul slipped out of its rift within reach (C2S seize_soul, off the Soul River); told if none is near. */
	public static void seize(ServerPlayer p) {
		Haunt haunt = HAUNTS.get(p.getUUID());
		if (haunt == null || !haunts(p)) return;
		long now = p.serverLevel().getGameTime();
		Vec3 eyes = p.getEyePosition();
		Drift nearest = null;
		for (Drift soul : haunt.souls) {
			double d = soul.at(now).distanceTo(eyes);
			if (d <= SoulRiverFeast.REACH && (nearest == null || d < nearest.at(now).distanceTo(eyes))) nearest = soul;
		}
		if (nearest == null) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_RIFT_SOUL_NONE).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		haunt.souls.remove(nearest);
		SoulRiverFeast.take(p, nearest.realm(), nearest.at(now), CultivationMethod.RIFT_HAUNTING, WORTH);
	}

	/** A player left. */
	public static void forget(UUID id) {
		HAUNTS.remove(id);
	}

	/** The server stopped. */
	public static void clear() {
		HAUNTS.clear();
	}

	private RiftHaunting() {}
}
