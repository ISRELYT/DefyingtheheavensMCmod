package com.example.defyingtheheavens.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players are currently meditating. Entries expire if the server's 1-second heartbeat stops
 * (player left tracking range, disconnected, etc.), so stale poses can't stick.
 */
public final class ClientMeditationTracker {
	private static final long EXPIRY_MS = 2500;
	private static final Map<UUID, Long> ACTIVE = new ConcurrentHashMap<>();

	public static void set(UUID id, boolean meditating) {
		if (meditating) {
			ACTIVE.put(id, System.currentTimeMillis());
		} else {
			ACTIVE.remove(id);
		}
	}

	public static boolean isMeditating(UUID id) {
		Long last = ACTIVE.get(id);
		if (last == null) return false;
		if (System.currentTimeMillis() - last > EXPIRY_MS) {
			ACTIVE.remove(id);
			return false;
		}
		return true;
	}

	private ClientMeditationTracker() {}
}
