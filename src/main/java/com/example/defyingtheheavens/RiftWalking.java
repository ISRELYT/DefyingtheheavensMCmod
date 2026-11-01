package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerPlayer;

/**
 * Rift Walking (CultivationMethod#RIFT_WALKING; the methods overhaul's phase 4): the heaven path's way at Void Shattering, walked through
 * space's tears: a Void Breach climbed through into the Great Void, {@link #CLIMB} of the stage (VoidBreach#arrive); and each bolt of a Spatial
 * Gap storm borne, {@link #BOLT} (SpatialStorms: as hard as a tribulation's strike of one's own rank). Never a way down, which costs nothing.
 */
public final class RiftWalking {
	public static final double CLIMB = 0.1, BOLT = 0.05;

	/** Whether {@code p} walks the rifts: one of the living, its heaven path practising Rift Walking. */
	public static boolean walks(ServerPlayer p) {
		return !GhostSystem.isGhost(p) && CultivationMethods.practised(CultivationManager.get(p), TwoPaths.Path.HEAVEN) == CultivationMethod.RIFT_WALKING;
	}

	/** {@code p} came out of a Void Breach's climb into the Great Void. */
	static void climbed(ServerPlayer p) {
		if (walks(p)) CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.RIFT_WALKING, CLIMB);
	}

	/** A gap storm's bolt struck {@code p}: borne, if it lives. */
	static void struck(ServerPlayer p) {
		if (p.isAlive() && walks(p)) CultivationMethods.grant(p, TwoPaths.Path.HEAVEN, CultivationMethod.RIFT_WALKING, BOLT);
	}

	private RiftWalking() {}
}
