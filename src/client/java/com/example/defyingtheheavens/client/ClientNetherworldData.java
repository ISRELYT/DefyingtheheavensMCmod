package com.example.defyingtheheavens.client;

/**
 * The local player's Netherworld state, as the server last sent it (ModPackets#NETHERWORLD_SYNC): the ghostly qi pool, the density
 * of ghostly qi where they are, and yin corruption. Forgotten on leaving a world.
 */
public final class ClientNetherworldData {
	private static double ghostQi;
	private static double ghostQiMax;
	private static double density;
	/** 0 to 1, or -1 where it doesn't apply (nothing to show). */
	private static float corruption = -1;
	/** 1 rising, -1 ebbing, 0 held still. */
	private static int trend;
	/** Seconds a Yang Preservation Pill still holds it still. */
	private static int heldSeconds;

	static void set(double ghostQi, double ghostQiMax, double density, float corruption, int trend, int heldSeconds) {
		ClientNetherworldData.ghostQi = ghostQi;
		ClientNetherworldData.ghostQiMax = ghostQiMax;
		ClientNetherworldData.density = density;
		ClientNetherworldData.corruption = corruption;
		ClientNetherworldData.trend = trend;
		ClientNetherworldData.heldSeconds = heldSeconds;
	}

	static void reset() {
		set(0, 0, 0, -1, 0, 0);
	}

	public static double ghostQi() { return ghostQi; }
	public static double ghostQiMax() { return ghostQiMax; }
	public static double density() { return density; }
	public static float corruption() { return corruption; }
	public static int trend() { return trend; }
	public static int heldSeconds() { return heldSeconds; }

	private ClientNetherworldData() {}
}
