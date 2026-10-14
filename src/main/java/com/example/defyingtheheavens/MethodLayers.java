package com.example.defyingtheheavens;

/**
 * How a realm-carrying method's layers are laid out, for the ghost path's foundations (GhostMethod) and the heaven methods
 * (HeavenMethod) alike. A method of a realm carries cultivation from Qi Refining up to that realm: nine layers for each realm it
 * spans ({@link #PER_REALM}), so the Netherworld Breathing Art (Qi Refining) has 9, a Four Axis method 54. Within each realm's
 * nine, layer 1 stands for its Early stage, 3 for Mid, 6 for Late, 9 for Grand Perfection ({@link #floor}); a layer costs its
 * place's minutes ({@link #MINUTES}) times its realm's factor ({@link #REALM_FACTOR}) in seconds of baseline meditation, so a
 * rarer method's lower realms cost what a commoner one's do, and it simply goes further.
 */
public final class MethodLayers {
	/** Layers to each realm a method spans. */
	public static final int PER_REALM = 9;
	/** Minutes of baseline meditation for each of a realm's nine layers, before the realm's factor. */
	public static final int[] MINUTES = {3, 4, 5, 6, 8, 10, 12, 15, 20};
	/** Each realm's factor on {@link #MINUTES}: Qi Refining's layers x1 up to Four Axis's x16. */
	public static final int[] REALM_FACTOR = {1, 2, 4, 7, 11, 16};

	/** All the layers of a method whose own realm is {@code top} (it spans Qi Refining up to it). */
	public static int layers(Realm top) {
		return PER_REALM * (top.ordinal() + 1);
	}

	/** The realm layer {@code layer} (1 and up) belongs to. */
	public static Realm realmOf(int layer) {
		return Realm.byIndex(Math.min(Realm.values().length - 1, Math.max(0, (layer - 1) / PER_REALM)));
	}

	/** Its place within its realm's nine, 1 to 9. */
	public static int within(int layer) {
		return (Math.max(1, layer) - 1) % PER_REALM + 1;
	}

	/** The stage a place within a realm's nine stands for: Early at 1, Mid at 3, Late at 6, Grand Perfection at 9. */
	public static Stage stageAt(int within) {
		return within >= 9 ? Stage.GRAND_PERFECTION : within >= 6 ? Stage.LATE : within >= 3 ? Stage.MID : Stage.EARLY;
	}

	/** The rank (PlayerCultivation#rank) layer {@code layer} stands for; -1 for none (0 or below). */
	public static int floor(int layer) {
		if (layer <= 0) return -1;
		return PlayerCultivation.rank(realmOf(layer), stageAt(within(layer)));
	}

	/** Seconds of baseline meditation through layer {@code layer}. */
	public static double cost(int layer) {
		int at = Math.max(1, layer);
		return MINUTES[within(at) - 1] * REALM_FACTOR[realmOf(at).ordinal()] * 60.0;
	}

	/** The deepest layer whose rank is at most {@code rank} (its realm's Early stage at least): 0 below Qi Refining Early. */
	public static int deepestAt(int rank) {
		if (rank < 0) return 0;
		int stages = Stage.values().length;
		int realm = Math.min(Realm.values().length - 1, rank / stages);
		int[] lastOfStage = {2, 5, 8, 9}; // Early 1-2, Mid 3-5, Late 6-8, Grand Perfection 9
		return realm * PER_REALM + lastOfStage[Math.min(3, rank % stages)];
	}

	private MethodLayers() {}
}
