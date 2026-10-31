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
 * The Ruins of a Fallen Holy Land's land (SecretRealm#FALLEN_HOLY_LAND), worked out column by column from the world's seed alone
 * (FallenHolyLandLand lays it, FallenHolyLandWorks builds on it): the shards of a holy land broken in some war of the ancients, floating
 * in the void about {@link #LEVEL_Y}, every world's its own.
 * <ul>
 *   <li><b>The heart shard</b>: the greatest, the ancestral hall on it under its formation (the works').</li>
 *   <li><b>The eye shards</b>: four about it, a formation eye on each (the works'), each guarded.</li>
 *   <li><b>The ruin shards</b>: the holy land's broken halls on them; and one at each arrival.</li>
 * </ul>
 * Each shard's top is level grass and stone, its underside a jagged cone of rock torn from the land below; between them nothing but the
 * void and its currents (the works').
 */
public final class FallenHolyLand {
	/** The shards' land, laid block by block (FallenHolyLandLand). */
	public static final RealmLand LAND = new FallenHolyLandLand();
	/** The shards' level: their tops about it (the heart's above it), and the sky's hold over the void measured from it (SecretRealm#holdFloor). */
	public static final int LEVEL_Y = 100;
	/** The heart shard's reach and top; the eye shards' and the arrivals' distance out. */
	public static final int HEART_R = 30, HEART_TOP = LEVEL_Y + 4, EYES_OUT = 92, ARRIVAL_R = 134;

	/** What a column is. */
	public enum Kind { HEART, EYE, ARRIVAL, RUIN, NONE }

	/** One column: its ground's top, its underside's lowest block, its kind, its shard (-1: none). */
	public record Column(int ground, int bottom, Kind kind, int shard) {}

	/** One shard: its middle (from the realm's), its reach, its top, and what it is ("heart", "eye", "arrival", "ruin"). */
	public record Shard(int x, int z, int r, int top, String role) {
		public Kind kind() {
			return switch (role) {
				case "heart" -> Kind.HEART;
				case "eye" -> Kind.EYE;
				case "arrival" -> Kind.ARRIVAL;
				default -> Kind.RUIN;
			};
		}
	}

	/** A world's shards: their noises, the main way's bearing (the hall faces along it), the shards. */
	public static final class Shape {
		final SimplexNoise edge, top, under;
		public final double mainTurn;
		public final List<Shard> shards;
		final long seed;

		Shape(long seed) {
			this.seed = seed;
			RandomSource random = RandomSource.create(seed ^ 0x401E1A2DL);
			edge = new SimplexNoise(random);
			top = new SimplexNoise(random);
			under = new SimplexNoise(random);
			mainTurn = random.nextInt(4) * Math.PI / 2;
			List<Shard> shards = new ArrayList<>();
			shards.add(new Shard(0, 0, HEART_R, HEART_TOP, "heart"));
			for (int k = 0; k < 4; k++) shards.add(at(mainTurn + Math.PI / 4 + k * Math.PI / 2, EYES_OUT, 11, LEVEL_Y - 2 + random.nextInt(7), "eye"));
			for (int k = 0; k < 4; k++) shards.add(at(mainTurn + k * Math.PI / 2, ARRIVAL_R, 10, LEVEL_Y - 2 + random.nextInt(5), "arrival"));
			// The broken land's own, between: wherever there is room, the void between each and the next.
			for (int tries = 0; shards.size() < 19 && tries < 400; tries++) {
				Shard shard = at(random.nextDouble() * Math.PI * 2, HEART_R + 26 + random.nextDouble() * (ARRIVAL_R - HEART_R - 14), 7 + random.nextInt(9),
						LEVEL_Y - 6 + random.nextInt(13), "ruin");
				boolean room = true;
				for (Shard other : shards) if (Math.hypot(shard.x - other.x, shard.z - other.z) < shard.r + other.r + 9) room = false;
				if (room) shards.add(shard);
			}
			this.shards = List.copyOf(shards);
		}

		private static Shard at(double bearing, double out, int r, int top, String role) {
			return new Shard((int) Math.round(Math.cos(bearing) * out), (int) Math.round(Math.sin(bearing) * out), r, top, role);
		}

		public Shard heart() { return shards.get(0); }

		/** The four eye shards, in order. */
		public List<Shard> eyes() { return shards.subList(1, 5); }
	}

	private static final Map<Long, Shape> SHAPES = Collections.synchronizedMap(new HashMap<>());

	public static Shape shape(long seed) {
		return SHAPES.computeIfAbsent(seed, Shape::new);
	}

	/** {@code shard}'s edge at (lx, lz)'s bearing from its middle (winding: by the bearing's cosine and sine, no seam). */
	static double edgeOf(Shape s, Shard shard, double lx, double lz) {
		double dx = lx - shard.x(), dz = lz - shard.z(), d = Math.max(1.0e-6, Math.hypot(dx, dz));
		return shard.r() + Math.max(2.0, shard.r() * 0.18) * s.edge.getValue(dx / d * 1.7 + shard.x() * 0.01, dz / d * 1.7 + shard.z() * 0.01);
	}

	/** The column at (lx, lz) from the realm's middle (SecretRealm#centre: the heart shard's). */
	public static Column column(Shape s, int lx, int lz) {
		if (Math.hypot(lx, lz) > SecretRealm.REACH) return new Column(-1, -1, Kind.NONE, -1);
		for (int i = 0; i < s.shards.size(); i++) {
			Shard shard = s.shards.get(i);
			double d = Math.hypot(lx - shard.x(), lz - shard.z());
			if (d > shard.r() * 1.5 + 4) continue;
			double edge = edgeOf(s, shard, lx, lz);
			if (d >= edge) continue;
			double in = Mth.clamp(1 - d / edge, 0, 1);
			int ground = shard.top() + (int) Math.round(0.7 * s.top.getValue(lx * 0.08, lz * 0.08) + (shard.role().equals("heart") ? 0 : 1.2 * in));
			// The underside: a jagged cone of torn rock, deepest under the middle.
			double depth = 3 + shard.r() * 1.5 * Math.pow(in, 0.65) + 4 * Math.abs(s.under.getValue(lx * 0.12, lz * 0.12));
			return new Column(ground, ground - (int) Math.round(depth), shard.kind(), i);
		}
		return new Column(-1, -1, Kind.NONE, -1);
	}

	/** The arrivals: the arrival shards' middles, on the ground. */
	public static List<BlockPos> arrivals(Shape s) {
		List<BlockPos> at = new ArrayList<>();
		for (Shard shard : s.shards) if (shard.role().equals("arrival")) at.add(new BlockPos(shard.x(), column(s, shard.x(), shard.z()).ground(), shard.z()));
		return at;
	}

	private FallenHolyLand() {}
}
