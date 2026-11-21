package com.example.defyingtheheavens;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The breakthrough cinematics' times that matter outside the client (the VFX plan, step B1, 2026-11-21): each realm's full and short
 * length, its blast tick and the tick its white-out begins, and its acts. The server holds the player for the length and throws mobs
 * back at the blast; the capture tool cuts its sheets by the acts and lines the reference's white-out up on ours. The client checks
 * its sequences (assets/defying-the-heavens/breakthroughs/) against this table as it loads them.
 *
 * Only realms with a cinematic are here; a breakthrough into any other keeps the old burst (TribulationManager#succeed).
 */
public final class CinematicTimes {
	public record Act(String name, int from, int to) {}

	/** One version of a cinematic: its length in ticks, the blast, the white-out's first tick, its acts end to end. */
	public record Times(int length, int blast, int whiteOut, List<Act> acts) {}

	public record Entry(Times full, Times shortVersion) {
		public Times of(boolean full) {
			return full ? this.full : shortVersion;
		}
	}

	private static final Map<Realm, Entry> TABLE = new EnumMap<>(Realm.class);

	static {
		// The hero: Nascent Soul Grand Perfection into Heavenly Being (the breakthrough brief §5; the plan's beat sheet). The blast at 528
		// and the white-out's rise from 532 are the reference's own timing (f2480 to f2524).
		TABLE.put(Realm.HEAVENLY_BEING, new Entry(
				new Times(790, 528, 532, List.of(
						new Act("Stillness", 0, 70),
						new Act("Gathering", 70, 210),
						new Act("The soul stirs, the pillar", 210, 330),
						new Act("The soul sea", 330, 450),
						new Act("Ignition", 450, 590),
						new Act("Awakening", 590, 650),
						new Act("Return and renewal", 650, 790))),
				new Times(240, 140, 142, List.of(
						new Act("The blink", 0, 20),
						new Act("Threads and absorption", 20, 80),
						new Act("The pillar", 80, 110),
						new Act("The soul turns gold", 110, 140),
						new Act("Blast and white-out", 140, 171),
						new Act("The eye opens", 171, 190),
						new Act("Merge, renewal and title", 190, 240)))));
	}

	/** The cinematic into {@code realm}, or null where there is none yet. */
	public static Entry of(Realm realm) {
		return realm == null ? null : TABLE.get(realm);
	}

	private CinematicTimes() {}
}
