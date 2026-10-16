package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivatorNpc;
import com.example.defyingtheheavens.MeditationManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players are currently meditating, and which of them float as they do (Core Formation and above, MeditationManager#floats).
 * Entries expire if the server's 1-second heartbeat stops (player left tracking range, disconnected, etc.), so stale poses can't stick.
 */
public final class ClientMeditationTracker {
	private static final long EXPIRY_MS = 2500;
	/** How high a floating meditator sits over the ground, as the dead sit (SoulRenderer), and how far it bobs. */
	public static final float HOVER = 0.3f, BOB = 0.06f;
	/** It rises to that over this long (ms) after sitting down. */
	private static final long RISE_MS = 1500;
	private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();
	/** The floating ones: when they sat down (or were first seen sitting). */
	private static final Map<UUID, Long> FLOATING = new ConcurrentHashMap<>();

	public static void set(UUID id, boolean meditating, boolean floats) {
		if (meditating) {
			ACTIVE.put(id, System.currentTimeMillis());
			if (floats) FLOATING.putIfAbsent(id, System.currentTimeMillis());
			else FLOATING.remove(id);
		} else {
			ACTIVE.remove(id);
			FLOATING.remove(id);
		}
	}

	public static boolean isMeditating(UUID id) {
		Long last = ACTIVE.get(id);
		if (last == null) return false;
		if (System.currentTimeMillis() - last > EXPIRY_MS) {
			ACTIVE.remove(id);
			FLOATING.remove(id);
			return false;
		}
		return true;
	}

	/**
	 * How high {@code entity} floats over the ground right now: a meditating player at Core Formation or above, or an NPC cultivator
	 * of that realm in meditation, rising gently once seated and bobbing; 0 for everyone else.
	 */
	public static float hover(LivingEntity entity, float partialTick) {
		Long since;
		if (entity instanceof Player) {
			if (!LotusPose.meditating(entity)) return 0;
			since = FLOATING.get(entity.getUUID());
		} else if (entity instanceof CultivatorNpc npc && npc.isMeditating() && !npc.isMortal() && !npc.isGhost()
				&& CultivatorNpc.realmOf(npc.getRank()).ordinal() >= MeditationManager.FLOATS_FROM.ordinal()) {
			since = FLOATING.computeIfAbsent(npc.getUUID(), u -> System.currentTimeMillis());
		} else {
			if (entity instanceof CultivatorNpc) FLOATING.remove(entity.getUUID());
			return 0;
		}
		if (since == null) return 0;
		float rise = Mth.clamp((System.currentTimeMillis() - since) / (float) RISE_MS, 0, 1);
		rise = rise * rise * (3 - 2 * rise);
		float age = entity.tickCount + partialTick;
		return rise * (HOVER + Mth.sin(age * 0.08f) * BOB);
	}

	/** Disconnect: nobody is seen meditating any more. */
	public static void clear() {
		ACTIVE.clear();
		FLOATING.clear();
	}

	private ClientMeditationTracker() {}
}
