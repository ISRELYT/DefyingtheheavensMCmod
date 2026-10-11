package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Gesture;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestures being made in sight (S2C gesture, see TalkSystem's Greet): which entity bows or cups its fist, and until when
 * (client game time). HumanoidModelMixin poses them; a gesture just begun or about to end eases in and out.
 */
public final class ClientGestures {
	private record Made(Gesture gesture, long until) {}

	private static final Map<Integer, Made> MADE = new HashMap<>();

	public static void start(int entityId, Gesture gesture) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || gesture == null) return;
		MADE.put(entityId, new Made(gesture, mc.level.getGameTime() + Gesture.TICKS));
	}

	/** The gesture {@code entity} is making now, or null. */
	public static Gesture of(Entity entity) {
		Made made = MADE.get(entity.getId());
		if (made == null) return null;
		if (entity.level().getGameTime() >= made.until()) {
			MADE.remove(entity.getId());
			return null;
		}
		return made.gesture();
	}

	/** 0-1: how far into its pose the gesture is (up in the first quarter, down in the last). */
	public static float weight(Entity entity, float partialTick) {
		Made made = MADE.get(entity.getId());
		if (made == null) return 0;
		float left = made.until() - entity.level().getGameTime() - partialTick;
		float done = Gesture.TICKS - left;
		float ease = Gesture.TICKS / 4f;
		return Math.max(0, Math.min(1, Math.min(done / ease, left / ease)));
	}

	public static void clear() {
		MADE.clear();
	}

	private ClientGestures() {}
}
