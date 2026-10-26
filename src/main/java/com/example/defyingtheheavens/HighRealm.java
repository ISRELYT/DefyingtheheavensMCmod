package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The high realms, each above a lower realm, as the code knows them: what it sits above, which qi it is rich in, how one comes
 * and goes, and whether it lets a cultivator break through past the lower realms' cap.
 * <ul>
 *   <li>Both lift the lower-realm cap (Realm Suppression is a lower realm's), refuse portals, are reached by a rift trial at
 *   0, 0 of the realm below and left by a descent, and draw NPC cultivators up from below.</li>
 *   <li>The <b>Upper Realm</b>, above the Overworld, is rich in ordinary qi (x{@link PlayerCultivation#UPPER_REALM_QI_MULTIPLIER})
 *   and is the one place a cultivator breaks through into Four Axis. Its trial is the Spatial Gap.</li>
 *   <li>The <b>Netherworld</b>, the realm of the dead above the Nether, is rich in ghostly qi ({@link GhostQi}) and holds
 *   ordinary qi as thinly as anywhere; breakthroughs past the cap stay the Upper Realm's. Its trial is the Soul River
 *   ({@link SoulRiver}).</li>
 * </ul>
 * Keep {@link ModDimensions#isLowerRealm} for the lower realms: the Nether stays one.
 */
public enum HighRealm {
	UPPER_REALM(ModDimensions.UPPER_REALM, Level.OVERWORLD, ModDimensions.UPPER_REALM_MIN_Y, true),
	NETHERWORLD(ModDimensions.NETHERWORLD, Level.NETHER, ModDimensions.NETHERWORLD_MIN_Y, false),
	/**
	 * The Great Void Realm, above the Jade Firmament (2026-10-25): no lower realm under it, but the Upper Realm; reached by a Void
	 * Breach torn in that one's sky (VoidBreach), not a rift at 0, 0, and left by falling off a continent into the Jade Firmament's sky
	 * (VoidFallHandler). Breakthroughs go on there as in the Upper Realm.
	 */
	GREAT_VOID(ModDimensions.GREAT_VOID, ModDimensions.UPPER_REALM, ModDimensions.GREAT_VOID_MIN_Y, true),
	/**
	 * The Immortal Realm, above the Great Void (phase 2, 2026-10-25): reached through the Immortal Gate over the Great Void's void sea
	 * when it opens (ImmortalGate), left by falling off its peaks into the Great Void's sky (ImmortalRealm#fallOut) or by a Spatial Tear.
	 * The one place True Immortal is cultivated at its pace, and its methods taught.
	 */
	IMMORTAL_REALM(ModDimensions.IMMORTAL_REALM, ModDimensions.GREAT_VOID, ModDimensions.IMMORTAL_REALM_MIN_Y, true);

	private final ResourceKey<Level> dimension;
	private final ResourceKey<Level> below;
	private final int floorY;
	private final boolean opensFourAxis;

	HighRealm(ResourceKey<Level> dimension, ResourceKey<Level> below, int floorY, boolean opensFourAxis) {
		this.dimension = dimension;
		this.below = below;
		this.floorY = floorY;
		this.opensFourAxis = opensFourAxis;
	}

	/** The high realm's own dimension. */
	public ResourceKey<Level> dimension() { return dimension; }

	/** The lower realm it sits above, where its rift stands and where a descent from it lands. */
	public ResourceKey<Level> below() { return below; }

	/** Falling below this Y in the realm starts the way down (a descent for the living). */
	public int floorY() { return floorY; }

	/** Breakthroughs past the lower realms' cap (into and within Four Axis) can happen here. */
	public boolean opensFourAxis() { return opensFourAxis; }

	/** The high realm whose dimension this is, or null. */
	public static HighRealm of(ResourceKey<Level> dimension) {
		for (HighRealm realm : values()) {
			if (realm.dimension == dimension) return realm;
		}
		return null;
	}

	/** The high realm above this realm, or null (the End has none): the Great Void above the Upper Realm. */
	public static HighRealm above(ResourceKey<Level> lower) {
		for (HighRealm realm : values()) {
			if (realm.below == lower) return realm;
		}
		return null;
	}
}
