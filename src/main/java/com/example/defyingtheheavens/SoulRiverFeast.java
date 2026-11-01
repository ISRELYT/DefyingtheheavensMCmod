package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Soul River Feast (CultivationMethod#SOUL_RIVER_FEAST; the methods overhaul's phase 3): on the Soul River (SoulRiver), one whose ghost path
 * practises it sees the souls the river carries drift past, one every {@link #EVERY} ticks a little ahead (glowing to it alone, gone after
 * {@link #LIFE}), and seizes the nearest within {@link #REACH} with the Circulate key (ModPackets#SEIZE_SOUL). The river carries the common
 * dead mostly: a soul stands from three realms below the seizer's ghost path to one above ({@link #roll}), and gives {@link #SHARE} of the
 * path's stage, twice as much for each realm above the path, half for each below, at most {@link #MOST} (CultivationMethods#grant). One above
 * the path fights back ({@link #fightsBack}): it tears at the seizer ({@link #HARM} of its health, ghostly harm, which reaches the dead), half
 * the time it breaks free, and one time in {@link #MEMORY_ODDS} the river takes a memory in the struggle: the top layer of one of the
 * seizer's ghost foundations, to settle in again ({@link #forgetMemory}).
 */
public final class SoulRiverFeast {
	public static final int EVERY = 120, LIFE = 100, MEMORY_ODDS = 4;
	public static final double REACH = 4.5, SHARE = 0.05, MOST = 0.5;
	public static final float HARM = 0.2f;

	/** A soul drifting past: where, of what realm (ordinal), since when (game time). */
	record Soul(Vec3 at, int realm, long born) {}

	private static final Map<UUID, List<Soul>> SOULS = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();

	/** Whether {@code p} feasts on the river: its ghost path begun, practising the Feast, holding its volume. */
	public static boolean feasts(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.SOUL_RIVER_FEAST
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	private static int pathRealm(ServerPlayer p) {
		Realm realm = GhostMethods.ghostRealm(CultivationManager.get(p).netherworld());
		return realm == null ? 0 : realm.ordinal();
	}

	/** A tick of {@code p}'s journey on the river ({@code tick} its count: SoulRiver#tickPlayer): a soul drifts by when its time comes; the souls glow. */
	static void travelling(ServerPlayer p, int tick) {
		if (!feasts(p)) {
			SOULS.remove(p.getUUID());
			return;
		}
		ServerLevel level = p.serverLevel();
		long now = level.getGameTime();
		List<Soul> souls = SOULS.computeIfAbsent(p.getUUID(), id -> new ArrayList<>());
		souls.removeIf(soul -> now - soul.born() > LIFE);
		int path = pathRealm(p);
		if (tick % EVERY == EVERY / 2) {
			// Well ahead, near the traveller's line, so it comes drifting past within reach (as the river carries the traveller on).
			Vec3 ahead = p.getLookAngle().multiply(1, 0, 1).normalize().scale(6 + RANDOM.nextDouble() * 3);
			souls.add(new Soul(p.position().add(ahead).add((RANDOM.nextDouble() - 0.5) * 2, 0.6 + RANDOM.nextDouble() * 0.8, (RANDOM.nextDouble() - 0.5) * 2),
					roll(path), now));
		}
		if (tick % 3 != 0) return;
		for (Soul soul : souls) {
			level.sendParticles(p, fightsBack(soul.realm(), path) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SOUL, true,
					soul.at().x, soul.at().y, soul.at().z, 2, 0.15, 0.2, 0.15, 0.01);
		}
	}

	/** The realm (ordinal) of a soul the river carries past one whose ghost path stands in {@code path}: the common dead, mostly below it. */
	static int roll(int path) {
		double r = RANDOM.nextDouble();
		int below = r < 0.30 ? 3 : r < 0.60 ? 2 : r < 0.85 ? 1 : r < 0.95 ? 0 : -1;
		return Math.max(0, path - below);
	}

	/** Whether a soul of {@code soul}'s realm fights back against one whose path stands in {@code path}: one above it does. */
	public static boolean fightsBack(int soul, int path) {
		return soul > path;
	}

	/** The share of its path's stage a soul of {@code soul}'s realm gives one whose path stands in {@code path}. */
	public static double share(int soul, int path) {
		return Math.min(MOST, SHARE * Math.pow(2, soul - path));
	}

	/** {@code p} seizes the nearest drifting soul within reach (C2S seize_soul); told if none is near. */
	public static void seize(ServerPlayer p) {
		List<Soul> souls = SOULS.get(p.getUUID());
		if (!SoulRiver.isTravelling(p.getUUID()) || !feasts(p)) return;
		Vec3 eyes = p.getEyePosition();
		Soul nearest = null;
		if (souls != null) {
			for (Soul soul : souls) {
				if (soul.at().distanceTo(eyes) <= REACH && (nearest == null || soul.at().distanceToSqr(eyes) < nearest.at().distanceToSqr(eyes))) nearest = soul;
			}
		}
		if (nearest == null) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_FEAST_NONE).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		souls.remove(nearest);
		take(p, nearest.realm(), nearest.at());
	}

	/** {@code p} takes a soul of {@code realm} at {@code at}: fed on, unless it fights back and breaks free. @return whether it was fed on */
	static boolean take(ServerPlayer p, int realm, Vec3 at) {
		return take(p, realm, at, CultivationMethod.SOUL_RIVER_FEAST, 1);
	}

	/** {@link #take}, the soul fed on by {@code method} (Rift Haunting's too: RiftHaunting), its worth {@code worth} times the Feast's. */
	static boolean take(ServerPlayer p, int realm, Vec3 at, CultivationMethod method, double worth) {
		ServerLevel level = p.serverLevel();
		int path = pathRealm(p);
		Vec3 to = p.getEyePosition();
		for (int i = 0; i <= 8; i++) {
			Vec3 step = at.lerp(to, i / 8.0);
			level.sendParticles(ParticleTypes.SOUL, step.x, step.y, step.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
		if (fightsBack(realm, path)) {
			p.hurt(ModDamageTypes.ghostly(level, null), p.getMaxHealth() * HARM);
			if (RANDOM.nextInt(MEMORY_ODDS) == 0) forgetMemory(p);
			if (RANDOM.nextBoolean()) {
				level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 1.4f);
				p.displayClientMessage(Component.translatable(ModLang.MSG_FEAST_ESCAPED).withStyle(ChatFormatting.RED), true);
				return false;
			}
		}
		level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f);
		CultivationMethods.grant(p, TwoPaths.Path.GHOST, method, share(realm, path) * worth);
		return true;
	}

	/** The river takes a memory: the top layer of one of {@code p}'s ghost foundations (a layer past its first), to settle in again. */
	static void forgetMemory(ServerPlayer p) {
		forgetMemory(p, ModLang.MSG_FEAST_MEMORY);
	}

	/** A memory taken ({@link #forgetMemory}), told by {@code line} (the Yellow Springs' forgetting: YellowSpringsImmersion). */
	static void forgetMemory(ServerPlayer p, String line) {
		NetherworldState s = CultivationManager.get(p).netherworld();
		List<GhostMethod> deep = new ArrayList<>();
		for (GhostMethod method : s.knownMethods()) if (s.layer(method) > 1) deep.add(method);
		if (deep.isEmpty()) return;
		GhostMethod method = deep.get(RANDOM.nextInt(deep.size()));
		s.setMethod(method, s.layer(method) - 1, 0);
		p.sendSystemMessage(Component.translatable(line, method.getDisplayName()).withStyle(ChatFormatting.DARK_RED));
		CultivationManager.markDirty(p.server);
		Spells.sync(p);
	}

	/** A player left, or its journey ended. */
	public static void forget(UUID id) {
		SOULS.remove(id);
	}

	private SoulRiverFeast() {}
}
