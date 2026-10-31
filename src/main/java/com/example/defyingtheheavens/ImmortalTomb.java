package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Tomb of a Fallen Immortal's land (SecretRealm#FALLEN_IMMORTAL_TOMB), worked out column by column from the world's seed alone
 * (ImmortalTombLand lays it, ImmortalTombWorks builds on it): a fallen immortal's palace grounds on a floating base over a sea of golden
 * cloud, every world's its own.
 * <ul>
 *   <li><b>The base</b>: a broad rounded square of white stone, level at {@link #BASE_Y} (its court, its halls and sanctum the works'), its
 *   underside a great dome of rock hung over the void.</li>
 *   <li><b>The islets</b>: rocks broken from it, floating about it at other heights.</li>
 * </ul>
 */
public final class ImmortalTomb {
	/** The base's land, laid block by block (ImmortalTombLand). */
	public static final RealmLand LAND = new ImmortalTombLand();
	/** The base's top (and the sky's hold over the void measured from it: SecretRealm#holdFloor). */
	public static final int BASE_Y = 100;
	/** The base's reach (its rounded square's), the arrivals' distance out (on its corners). */
	public static final int BASE_R = 96, ARRIVAL_R = 92;

	/** What a column is. */
	public enum Kind { BASE, EDGE, ISLET, NONE }

	/** One column: its ground's top, its underside's lowest block, its kind. */
	public record Column(int ground, int bottom, Kind kind) {}

	/** One islet: its middle (from the realm's), its reach, its top. */
	public record Islet(int x, int z, int r, int top) {}

	/** A world's tomb: its noises, the main way's bearing (the sanctum lies that way from the court), its islets. */
	public static final class Shape {
		final SimplexNoise edge, top, under;
		public final double mainTurn;
		public final List<Islet> islets;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x7031B5L);
			edge = new SimplexNoise(random);
			top = new SimplexNoise(random);
			under = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
			List<Islet> islets = new ArrayList<>();
			for (int tries = 0; islets.size() < 7 && tries < 200; tries++) {
				double a = random.nextDouble() * Math.PI * 2, out = BASE_R + 26 + random.nextDouble() * 34;
				Islet islet = new Islet((int) Math.round(Math.cos(a) * out), (int) Math.round(Math.sin(a) * out), 5 + random.nextInt(5),
						BASE_Y - 24 + random.nextInt(40));
				boolean room = true;
				for (Islet other : islets) if (Math.hypot(islet.x - other.x, islet.z - other.z) < islet.r + other.r + 10) room = false;
				if (room) islets.add(islet);
			}
			this.islets = List.copyOf(islets);
		}
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** How far (lx, lz) lies out on the base's rounded square (its edge at {@link #BASE_R}, winding a little). */
	static double squareOut(Shape s, double lx, double lz) {
		double r = Math.max(1.0e-6, Math.hypot(lx, lz));
		double out = Math.pow(Math.pow(Math.abs(lx), 4) + Math.pow(Math.abs(lz), 4), 0.25);
		return out - 3 * s.edge.getValue(lx / r * 2.0, lz / r * 2.0);
	}

	/** The column at (lx, lz) from the realm's middle (SecretRealm#centre: the court's). */
	public static Column column(Shape s, int lx, int lz) {
		if (Math.hypot(lx, lz) > SecretRealm.REACH) return new Column(-1, -1, Kind.NONE);
		double out = squareOut(s, lx, lz);
		if (out < BASE_R) {
			double in = Mth.clamp((BASE_R - out) / BASE_R, 0, 1);
			int ground = BASE_Y + (out > BASE_R - 4 ? (int) Math.round(0.6 * s.top.getValue(lx * 0.1, lz * 0.1)) : 0);
			double depth = 6 + 62 * Math.pow(in, 0.55) + 6 * Math.abs(s.under.getValue(lx * 0.06, lz * 0.06));
			return new Column(ground, ground - (int) Math.round(depth), out > BASE_R - 3 ? Kind.EDGE : Kind.BASE);
		}
		for (Islet islet : s.islets) {
			double d = Math.hypot(lx - islet.x(), lz - islet.z());
			double edge = islet.r() + 1.5 * s.edge.getValue(lx * 0.15, lz * 0.15);
			if (d >= edge) continue;
			double in = Mth.clamp(1 - d / edge, 0, 1);
			int ground = islet.top() + (int) Math.round(in * 1.5);
			return new Column(ground, ground - (int) Math.round(2 + islet.r() * 1.4 * Math.pow(in, 0.6)), Kind.ISLET);
		}
		return new Column(-1, -1, Kind.NONE);
	}

	/** The arrivals: on the base's four corners, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (int k = 0; k < 4; k++) {
			double a = s.mainTurn + Math.PI / 4 + k * Math.PI / 2, d = ARRIVAL_R * 0.98;
			int x = (int) Math.round(Math.cos(a) * d * 0.75), z = (int) Math.round(Math.sin(a) * d * 0.75);
			at.add(new BlockPos(x, column(s, x, z).ground(), z));
		}
		return at;
	}

	private ImmortalTomb() {}
}
