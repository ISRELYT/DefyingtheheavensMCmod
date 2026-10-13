package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where the Soul River runs, in its own passage dimension (see {@link SoulRiver}): one river, the same for everyone, flowing
 * along +X from {@code x = 0} (the Nether's end) to {@code x = }{@link #LENGTH} (the Netherworld's) at about Y {@link #BASE_Y}.
 * The way up rides it from 0 to LENGTH; the way down is the same river flowing back. Server and client both read it from here,
 * so the server's checks and the client's drawing and steering agree.
 * <p>
 * The current carries a traveller along X at {@link #FLOW} whatever they do; the river sways gently to either side of that line
 * and a little up and down, so they must steer to stay in it. Each sway is a whole number of waves of (1 - cos), so the river
 * leaves and reaches both ends dead straight and on the line. It narrows from {@link #RADIUS_START} to {@link #RADIUS_END} over
 * the journey, and the crosscurrents grow from {@link #TURBULENCE_START} to {@link #TURBULENCE_END}.
 * <p>
 * Steering has room to spare: the steepest sway asks for (SWAY_A x 2 pi x SWAY_A_WAVES + SWAY_B x 2 pi x SWAY_B_WAVES) / LENGTH
 * x FLOW = (8 x 2 pi x 2 + 3 x 2 pi x 5) / 480 x 0.4 = (0.21 + 0.20) x 0.4 = 0.16 blocks a tick sideways, and the strongest
 * crosscurrent 0.06 more: 0.22 against {@link #STEER} = 0.3.
 */
public final class SoulRiverCourse {
	/** The journey, either way: 60 seconds. */
	public static final int TICKS = 60 * 20;
	/** Blocks a tick the current carries a traveller along the river. */
	public static final double FLOW = 0.4;
	/** The river's length: TICKS x FLOW = 1200 x 0.4 = 480 blocks. */
	public static final double LENGTH = TICKS * FLOW;
	public static final double BASE_Y = 192;
	/** The river's reach from its centre line, narrowing as the journey goes on. */
	public static final double RADIUS_START = 4.0, RADIUS_END = 3.0;
	/** Blocks a tick a traveller can push themselves across the current (never along it). */
	public static final double STEER = 0.3;
	/** The crosscurrents' strength, blocks a tick, from the start of the journey to its end. */
	public static final double TURBULENCE_START = 0.02, TURBULENCE_END = 0.06;
	/** The sways: two side to side, in opposite directions (half-height in blocks, waves over the river), and one up and down. */
	private static final double SWAY_A = 8, SWAY_B = 3, RISE = 3;
	private static final int SWAY_A_WAVES = 2, SWAY_B_WAVES = 5, RISE_WAVES = 3;

	/** The centre of the river at {@code x}; past either end it runs on straight. */
	public static Vec3 centre(double x) {
		double t = Mth.TWO_PI * Mth.clamp(x, 0, LENGTH) / LENGTH;
		double z = SWAY_A * (1 - Math.cos(t * SWAY_A_WAVES)) - SWAY_B * (1 - Math.cos(t * SWAY_B_WAVES));
		double y = BASE_Y + RISE * (1 - Math.cos(t * RISE_WAVES));
		return new Vec3(x, y, z);
	}

	/** How far along the journey a traveller at {@code x} is, 0 to 1, going up ({@code ascending}) or down. */
	public static double progress(double x, boolean ascending) {
		double p = Mth.clamp(x / LENGTH, 0, 1);
		return ascending ? p : 1 - p;
	}

	/** Where a journey sets out: X 0 going up, X {@link #LENGTH} going down. */
	public static double startX(boolean ascending) {
		return ascending ? 0 : LENGTH;
	}

	/** +1 going up (along +X), -1 going down. */
	public static int direction(boolean ascending) {
		return ascending ? 1 : -1;
	}

	/** The river's reach at that point of the journey. */
	public static double radius(double progress) {
		return Mth.lerp(Mth.clamp(progress, 0, 1), RADIUS_START, RADIUS_END);
	}

	/** How far a point lies from the river's centre, across it (the Y-Z plane at its X; the sways are gentle enough). */
	public static double distanceFromCentre(Vec3 point) {
		Vec3 centre = centre(point.x);
		double dy = point.y - centre.y, dz = point.z - centre.z;
		return Math.sqrt(dy * dy + dz * dz);
	}

	/**
	 * The crosscurrent pushing a traveller {@code tick}s into their journey: a slow sway across the river and a weaker one up
	 * and down, never along it. Every traveller meets the same currents at the same moment of their journey.
	 */
	public static Vec3 crosscurrent(int tick) {
		double strength = Mth.lerp(Mth.clamp(tick / (double) TICKS, 0, 1), TURBULENCE_START, TURBULENCE_END);
		double side = (Math.sin(tick * 0.07 + 1.3) + 0.5 * Math.sin(tick * 0.19 + 0.4)) / 1.5;
		double up = (Math.sin(tick * 0.11 + 2.6) + 0.5 * Math.sin(tick * 0.23)) / 1.5;
		return new Vec3(0, up * strength * 0.5, side * strength);
	}

	private SoulRiverCourse() {}
}
