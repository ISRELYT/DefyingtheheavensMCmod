package com.example.defyingtheheavens.client;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * The dead this client has been told are bound by a Ghost-Quelling Talisman (ModPackets#QUELLED), and until when (the level's
 * game time): SoulRenderer seals the talisman to their brow while it holds.
 */
public final class ClientQuelled {
	private static final Map<Integer, Long> UNTIL = new HashMap<>();

	static void set(Entity bound, int ticks) {
		UNTIL.put(bound.getId(), bound.level().getGameTime() + ticks);
	}

	/** Whether a talisman binds {@code entity} now. */
	static boolean is(Entity entity) {
		Long until = UNTIL.get(entity.getId());
		if (until == null) return false;
		if (entity.level().getGameTime() < until) return true;
		UNTIL.remove(entity.getId());
		return false;
	}

	static void clear() {
		UNTIL.clear();
	}

	private ClientQuelled() {}
}
