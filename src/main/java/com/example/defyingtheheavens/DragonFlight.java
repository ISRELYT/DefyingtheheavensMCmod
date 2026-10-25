package com.example.defyingtheheavens;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Azure Dragon's flight (WoodSpells.Dragon; the user's ask, 2026-10-25: "don't make azure dragon like look up and roar, just make it fly
 * off, also make it smoother, still just looks retarded when it rotates around looks like its spinning instead of flying"): one path, worked
 * out alike on both sides from the way it sets off and its size, so the server can carry its head along it and every watcher can lay its
 * body along the same path behind the head (SpellConstructRenderer's dragon pose), a serpent's body following where its head has been
 * rather than one stiff shape turned about. It bursts up out of the land almost straight up, then bends over into a long rising flight
 * away the way it set off, swinging gently from side to side, quicker as it goes, until it is gone into the sky.
 * <p>
 * The path is in blocks from where its head first stands (a little under the land where it rises), ticks from its start: {@link Path#at}.
 * Before its start (where the rest of its body still is as it bursts out) the path runs straight back down the way it first came.
 */
public final class DragonFlight {
	/** How long it flies, ticks: then it is gone into the sky. */
	public static final int TICKS = 110;
	/** Its head starts this far under the land where it rises (its body, all of it, under the land behind it). */
	public static final double UNDER = 2.5;
	/** The path's samples are this many ticks apart (the rest between them eased). */
	private static final double STEP = 0.25;
	/** Its speed, blocks a tick for each of its size: at its start, and the most it comes to (by {@link #QUICKENS} ticks). */
	private static final double SPEED_FROM = 0.75, SPEED_TO = 1.9, QUICKENS = 60;
	/** Its pitch (degrees, less than nought up): bursting out, and once it has bent over into its flight (by {@link #BENDS} ticks). */
	private static final double PITCH_FROM = -82, PITCH_TO = -16, BENDS = 48;
	/** How far it swings from side to side (degrees), and how long a swing there and back takes (ticks). */
	private static final double SWAY = 24, SWAY_PERIOD = 64;

	private static final Map<Long, Path> MADE = new LinkedHashMap<>(16, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, Path> eldest) {
			return size() > 16;
		}
	};

	/** The path of one setting off {@code yaw} (degrees, as an entity's), its size {@code size} (its spell's greatness). */
	public static synchronized Path of(float yaw, float size) {
		long key = (long) Float.floatToIntBits(yaw) << 32 | (Float.floatToIntBits(size) & 0xFFFFFFFFL);
		return MADE.computeIfAbsent(key, k -> new Path(yaw, size));
	}

	/** Its yaw and pitch (degrees) {@code t} ticks in, as its course turns. */
	private static double yaw(double yaw0, double t) {
		double settle = Mth.clamp(t / 22, 0, 1);
		return yaw0 + SWAY * Math.sin(2 * Math.PI * (t - 14) / SWAY_PERIOD) * settle * settle;
	}

	private static double pitch(double t) {
		double f = Mth.clamp(t / BENDS, 0, 1);
		double eased = f * f * (3 - 2 * f);
		return PITCH_FROM + (PITCH_TO - PITCH_FROM) * eased + 5 * Math.sin(2 * Math.PI * t / 38) * eased;
	}

	private static double speed(double t, float size) {
		double f = Mth.clamp(t / QUICKENS, 0, 1);
		return size * (SPEED_FROM + (SPEED_TO - SPEED_FROM) * f * f * (3 - 2 * f));
	}

	/** The way (a unit) of yaw and pitch, as Minecraft's look. */
	private static Vec3 way(double yaw, double pitch) {
		return Vec3.directionFromRotation((float) pitch, (float) yaw);
	}

	/** One flight's path, sampled. */
	public static final class Path {
		private final double[] x, y, z, arc;
		private final Vec3 first;
		private final double firstSpeed;

		Path(float yaw0, float size) {
			int n = (int) Math.ceil((TICKS + 20) / STEP) + 1;
			x = new double[n];
			y = new double[n];
			z = new double[n];
			arc = new double[n];
			Vec3 at = Vec3.ZERO;
			for (int i = 0; i < n; i++) {
				x[i] = at.x;
				y[i] = at.y;
				z[i] = at.z;
				double t = i * STEP;
				Vec3 step = way(yaw(yaw0, t + STEP / 2), pitch(t + STEP / 2)).scale(speed(t + STEP / 2, size) * STEP);
				if (i > 0) arc[i] = arc[i - 1] + Math.sqrt((x[i] - x[i - 1]) * (x[i] - x[i - 1]) + (y[i] - y[i - 1]) * (y[i] - y[i - 1]) + (z[i] - z[i - 1]) * (z[i] - z[i - 1]));
				at = at.add(step);
			}
			first = way(yaw(yaw0, 0), pitch(0));
			firstSpeed = speed(0, size);
		}

		/** Where its head is {@code t} ticks in (blocks from where it started): before its start, back down the way it first came. */
		public Vec3 at(double t) {
			if (t <= 0) return first.scale(firstSpeed * t);
			double f = t / STEP;
			int i = Math.min(x.length - 2, (int) f);
			double u = Math.min(1, f - i);
			return new Vec3(Mth.lerp(u, x[i], x[i + 1]), Mth.lerp(u, y[i], y[i + 1]), Mth.lerp(u, z[i], z[i + 1]));
		}

		/** How far along it its head has come {@code t} ticks in (blocks; less than nought before its start). */
		public double arc(double t) {
			if (t <= 0) return firstSpeed * t;
			double f = t / STEP;
			int i = Math.min(arc.length - 2, (int) f);
			return Mth.lerp(Math.min(1, f - i), arc[i], arc[i + 1]);
		}

		/** When its head was {@code s} blocks along it ({@link #arc}'s undoing). */
		public double timeAt(double s) {
			if (s <= 0) return s / firstSpeed;
			int lo = 0, hi = arc.length - 1;
			if (s >= arc[hi]) return hi * STEP;
			while (hi - lo > 1) {
				int mid = (lo + hi) >>> 1;
				if (arc[mid] <= s) lo = mid;
				else hi = mid;
			}
			double span = arc[hi] - arc[lo];
			return (lo + (span <= 1.0e-9 ? 0 : (s - arc[lo]) / span)) * STEP;
		}

		/** Where along it the body is {@code behind} blocks back from its head, {@code t} ticks in. */
		public Vec3 behind(double t, double behind) {
			return at(timeAt(arc(t) - behind));
		}

		/** The way its head goes {@code t} ticks in (a unit). */
		public Vec3 heading(double t) {
			Vec3 d = at(t + 0.5).subtract(at(t - 0.5));
			return d.lengthSqr() < 1.0e-9 ? first : d.normalize();
		}
	}

	private DragonFlight() {}
}
