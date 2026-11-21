package com.example.defyingtheheavens.client.cinematic;

import net.minecraft.util.Mth;

/**
 * The cinematics' easing curves (the breakthrough brief §6.1): cubic, exponential and back, each in, out and in-out, plus a snap
 * (no motion until the very end, then all of it) and linear. Every one maps 0..1 onto 0..1 (back overshoots on the way).
 */
public enum Ease {
	LINEAR, CUBIC_IN, CUBIC_OUT, CUBIC_IN_OUT, EXPO_IN, EXPO_OUT, EXPO_IN_OUT, BACK_IN, BACK_OUT, BACK_IN_OUT, SNAP, SMOOTH;

	private static final float BACK = 1.70158f;

	public float apply(float t) {
		t = Mth.clamp(t, 0, 1);
		return switch (this) {
			case LINEAR -> t;
			case CUBIC_IN -> t * t * t;
			case CUBIC_OUT -> 1 - (1 - t) * (1 - t) * (1 - t);
			case CUBIC_IN_OUT -> t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
			case EXPO_IN -> t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
			case EXPO_OUT -> t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
			case EXPO_IN_OUT -> t == 0 ? 0 : t == 1 ? 1 : t < 0.5f ? (float) Math.pow(2, 20 * t - 10) / 2 : (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
			case BACK_IN -> (BACK + 1) * t * t * t - BACK * t * t;
			case BACK_OUT -> 1 + (BACK + 1) * (float) Math.pow(t - 1, 3) + BACK * (float) Math.pow(t - 1, 2);
			case BACK_IN_OUT -> {
				float c = BACK * 1.525f;
				yield t < 0.5f ? (float) (Math.pow(2 * t, 2) * ((c + 1) * 2 * t - c)) / 2
						: (float) (Math.pow(2 * t - 2, 2) * ((c + 1) * (t * 2 - 2) + c) + 2) / 2;
			}
			case SNAP -> t < 1 ? 0 : 1;
			case SMOOTH -> t * t * (3 - 2 * t);
		};
	}

	/** By its name in a sequence ("cubic_in_out"), or SMOOTH for none. */
	public static Ease of(String name) {
		if (name == null || name.isEmpty()) return SMOOTH;
		return valueOf(name.toUpperCase(java.util.Locale.ROOT));
	}

	/** How far through [from, to) tick {@code t} is, eased: 0 before it, 1 after it. */
	public float between(float t, float from, float to) {
		return to <= from ? (t >= to ? 1 : 0) : apply((t - from) / (to - from));
	}
}
