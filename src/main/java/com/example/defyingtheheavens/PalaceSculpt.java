package com.example.defyingtheheavens;

/**
 * The Demon Palaces' great beasts in stone (DemonPalacePlan; the user's design, 2026-10-25: "giant statues of the race's beast"):
 * <ul>
 *   <li>the colossus behind each Sovereign's hall, its head over the hall's roof ({@link #colossus}): a white tiger crouching, its
 *       forepaws together before it; a nine-tailed fox sitting up, its tails fanned like a halo; a lizard king lying long across,
 *       a mountain on its back; an azure dragon coiled, no legs;</li>
 *   <li>the pairs along the Way of Beasts ({@link #statue}): the same beasts, smaller, on plinths.</li>
 * </ul>
 * (A gate of the beast's head, its jaws open over the way, was tried and not liked: the user, 2026-10-25. The palaces' gates are
 * torii: DemonPalacePieces#gate.)
 * Sculpted as voxels in the statue's own frame (x across its front, z from its front back, y up; it looks out to the front, -z), each
 * painted with what it is ({@link Paint}), which the race's palette turns into blocks (DemonPalacePieces). Plain Java (no game
 * classes), so it can be looked at off the game too.
 */
final class PalaceSculpt {
	/** What a voxel of a statue is. */
	enum Paint { BODY, MARK, BELLY, EYE, FANG, CLAW, CROWN, MANE, TIP, GLOW, STONE }

	/** What a statue's surface is at a point (units, the statue's frame), or null to leave it be. */
	interface Painter {
		Paint at(double x, double y, double z);
	}

	/** A statue's voxels: {@link #w} across, {@link #h} high, {@link #d} deep; the beast's middle line at x = w / 2. */
	static final class Grid {
		final int w, h, d;
		private final byte[] cells;

		Grid(int w, int h, int d) {
			this.w = w;
			this.h = h;
			this.d = d;
			this.cells = new byte[w * h * d];
		}

		boolean inside(int x, int y, int z) { return x >= 0 && y >= 0 && z >= 0 && x < w && y < h && z < d; }

		/** What is at (x, y, z), or null (air, or outside). */
		Paint get(int x, int y, int z) {
			if (!inside(x, y, z)) return null;
			int c = cells[x + w * (z + d * y)];
			return c == 0 ? null : Paint.values()[c - 1];
		}

		void set(int x, int y, int z, Paint paint) {
			if (inside(x, y, z)) cells[x + w * (z + d * y)] = (byte) (paint == null ? 0 : paint.ordinal() + 1);
		}
	}

	/**
	 * Draws into a grid at {@code scale} blocks a unit, the frame's x = 0 at the grid's middle, y = 0 and z = 0 at its floor and front
	 * (offset by {@link #lift} and {@link #push}).
	 */
	static final class Sculptor {
		final Grid grid;
		final double scale;
		double lift, push;
		/**
		 * A tube's least radius in voxels. At a small statue's scale a thinner one (a whisker, a tine) broke into loose bits along its
		 * slope; from √3/2 on, every point of its line is that near the middle of the voxel it lies in, so the voxels join up.
		 */
		double tubeMin = 0.55;
		/** An ellipsoid's least radius in voxels (a small statue's eyes, three quarters of a block across, came to no voxel at all). */
		double blobMin = 0.55;

		Sculptor(Grid grid, double scale) {
			this.grid = grid;
			this.scale = scale;
		}

		private double ux(int x) { return (x + 0.5 - grid.w / 2.0) / scale; }
		private double uy(int y) { return (y + 0.5) / scale - lift; }
		private double uz(int z) { return (z + 0.5) / scale - push; }
		private int vx(double x) { return (int) Math.floor(x * scale + grid.w / 2.0); }
		private int vy(double y) { return (int) Math.floor((y + lift) * scale); }
		private int vz(double z) { return (int) Math.floor((z + push) * scale); }

		/** The ellipsoid at (cx, cy, cz) of radii (rx, ry, rz), painted by {@code paint}. Never thinner than a voxel. */
		void blob(double cx, double cy, double cz, double rx, double ry, double rz, Painter paint) {
			double ax = Math.max(rx, blobMin / scale), ay = Math.max(ry, blobMin / scale), az = Math.max(rz, blobMin / scale);
			for (int x = vx(cx - ax) - 1; x <= vx(cx + ax) + 1; x++) {
				for (int y = vy(cy - ay) - 1; y <= vy(cy + ay) + 1; y++) {
					for (int z = vz(cz - az) - 1; z <= vz(cz + az) + 1; z++) {
						if (!grid.inside(x, y, z)) continue;
						double dx = (ux(x) - cx) / ax, dy = (uy(y) - cy) / ay, dz = (uz(z) - cz) / az;
						if (dx * dx + dy * dy + dz * dz > 1) continue;
						Paint p = paint.at(ux(x), uy(y), uz(z));
						if (p != null) grid.set(x, y, z, p);
					}
				}
			}
		}

		void blob(double cx, double cy, double cz, double rx, double ry, double rz, Paint paint) {
			blob(cx, cy, cz, rx, ry, rz, (x, y, z) -> paint);
		}

		/** A tube from a to b, its radius r0 at a narrowing (or swelling) to r1 at b, its ends round. Never thinner than a voxel. */
		void tube(double[] a, double[] b, double r0, double r1, Painter paint) {
			double min = tubeMin / scale;
			double ra = Math.max(r0, min), rb = Math.max(r1, min), reach = Math.max(ra, rb);
			double ex = b[0] - a[0], ey = b[1] - a[1], ez = b[2] - a[2], len2 = ex * ex + ey * ey + ez * ez;
			for (int x = vx(Math.min(a[0], b[0]) - reach) - 1; x <= vx(Math.max(a[0], b[0]) + reach) + 1; x++) {
				for (int y = vy(Math.min(a[1], b[1]) - reach) - 1; y <= vy(Math.max(a[1], b[1]) + reach) + 1; y++) {
					for (int z = vz(Math.min(a[2], b[2]) - reach) - 1; z <= vz(Math.max(a[2], b[2]) + reach) + 1; z++) {
						if (!grid.inside(x, y, z)) continue;
						double px = ux(x) - a[0], py = uy(y) - a[1], pz = uz(z) - a[2];
						double t = len2 < 1.0e-9 ? 0 : Math.max(0, Math.min(1, (px * ex + py * ey + pz * ez) / len2));
						double qx = px - ex * t, qy = py - ey * t, qz = pz - ez * t;
						double r = ra + (rb - ra) * t;
						if (qx * qx + qy * qy + qz * qz > r * r) continue;
						Paint p = paint.at(ux(x), uy(y), uz(z));
						if (p != null) grid.set(x, y, z, p);
					}
				}
			}
		}

		void tube(double[] a, double[] b, double r0, double r1, Paint paint) {
			tube(a, b, r0, r1, (x, y, z) -> paint);
		}

		/** Tubes through {@code points} in turn, the radius going from r0 at the first to r1 at the last by length along them. */
		void chain(double[][] points, double r0, double r1, Painter paint) {
			double total = 0;
			for (int i = 1; i < points.length; i++) total += dist(points[i - 1], points[i]);
			double along = 0;
			for (int i = 1; i < points.length; i++) {
				double step = dist(points[i - 1], points[i]);
				double ra = r0 + (r1 - r0) * along / total, rb = r0 + (r1 - r0) * (along + step) / total;
				tube(points[i - 1], points[i], ra, rb, paint);
				along += step;
			}
		}

		void chain(double[][] points, double r0, double r1, Paint paint) {
			chain(points, r0, r1, (x, y, z) -> paint);
		}

		/** Tubes through {@code points} in turn, each point's own radius {@code radii} (a tail that swells and then tapers). */
		void chain(double[][] points, double[] radii, Painter paint) {
			for (int i = 1; i < points.length; i++) tube(points[i - 1], points[i], radii[i - 1], radii[i], paint);
		}

		/** Empties the box from (x0, y0, z0) to (x1, y1, z1). */
		void carve(double x0, double y0, double z0, double x1, double y1, double z1) {
			for (int x = vx(Math.min(x0, x1)); x <= vx(Math.max(x0, x1)); x++) {
				for (int y = vy(Math.min(y0, y1)); y <= vy(Math.max(y0, y1)); y++) {
					for (int z = vz(Math.min(z0, z1)); z <= vz(Math.max(z0, z1)); z++) {
						double ux = ux(x), uy = uy(y), uz = uz(z);
						if (ux < Math.min(x0, x1) || ux > Math.max(x0, x1) || uy < Math.min(y0, y1) || uy > Math.max(y0, y1) || uz < Math.min(z0, z1)
								|| uz > Math.max(z0, z1)) continue;
						grid.set(x, y, z, null);
					}
				}
			}
		}

		/** Repaints whatever is there within the ellipsoid (leaving air air). */
		void paint(double cx, double cy, double cz, double rx, double ry, double rz, Painter paint) {
			for (int x = vx(cx - rx) - 1; x <= vx(cx + rx) + 1; x++) {
				for (int y = vy(cy - ry) - 1; y <= vy(cy + ry) + 1; y++) {
					for (int z = vz(cz - rz) - 1; z <= vz(cz + rz) + 1; z++) {
						if (grid.get(x, y, z) == null) continue;
						double dx = (ux(x) - cx) / rx, dy = (uy(y) - cy) / ry, dz = (uz(z) - cz) / rz;
						if (dx * dx + dy * dy + dz * dz > 1) continue;
						Paint p = paint.at(ux(x), uy(y), uz(z));
						if (p != null) grid.set(x, y, z, p);
					}
				}
			}
		}
	}

	private static double dist(double[] a, double[] b) {
		double x = a[0] - b[0], y = a[1] - b[1], z = a[2] - b[2];
		return Math.sqrt(x * x + y * y + z * z);
	}

	private static double[] p(double x, double y, double z) { return new double[] {x, y, z}; }

	/** A value at (x, y, z): 0 to 1, smooth over a few units (for marks that wander). */
	private static double wave(double x, double y, double z) {
		return 0.5 + 0.25 * Math.sin(x * 0.37 + z * 0.21) + 0.25 * Math.sin(y * 0.29 - z * 0.33 + x * 0.11);
	}

	/** A number from 0 to 1 for the cell (i, j, k), the same every time. */
	private static double hash(int i, int j, int k) {
		long h = i * 73856093L ^ j * 19349663L ^ k * 83492791L;
		h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
		h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
		return ((h ^ (h >>> 33)) >>> 11) / (double) (1L << 53);
	}

	/**
	 * A hide broken into plates about {@code size} across (cells round jittered points): how near (units) the point is to the seam
	 * between its plate and the next, and which plate it is on (a number from 0 to 1), as {seam, plate}.
	 */
	private static double[] plates(double x, double y, double z, double size) {
		int cx = (int) Math.floor(x / size), cy = (int) Math.floor(y / size), cz = (int) Math.floor(z / size);
		double best = Double.MAX_VALUE, second = Double.MAX_VALUE, plate = 0;
		for (int i = cx - 1; i <= cx + 1; i++) {
			for (int j = cy - 1; j <= cy + 1; j++) {
				for (int k = cz - 1; k <= cz + 1; k++) {
					double px = (i + hash(i, j, k)) * size, py = (j + hash(j, k, i)) * size, pz = (k + hash(k, i, j)) * size;
					double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
					if (d < best) {
						second = best;
						best = d;
						plate = hash(i + 7, j - 3, k + 11);
					} else if (d < second) {
						second = d;
					}
				}
			}
		}
		return new double[] {(second - best) / 2, plate};
	}

	// =====================================================================================================================
	// The colossus
	// =====================================================================================================================

	/**
	 * A colossus's grid (blocks): wide, high and deep enough for the greatest of them. It stands right behind the Sovereign's hall, its
	 * front against the hall's back (its head over the hall's roof): nothing of it reaches in front of it (the user, 2026-10-25: the
	 * tiger's forepaws spread wide round the hall were "too far apart").
	 */
	static final int COLOSSUS_W = 84, COLOSSUS_H = 66, COLOSSUS_D = 80;

	/** The colossus of {@code race} at {@code scale} (blocks a unit). */
	static Grid colossus(Bloodline race, double scale) {
		Grid grid = sized(scale);
		beast(new Sculptor(grid, scale), race, true);
		return grid;
	}

	static Grid colossus(Bloodline race) {
		return colossus(race, 1.0);
	}

	/** A statue of the Way of Beasts: the beast at {@code scale} (a third or so), without a colossus's crag or rock. */
	static Grid statue(Bloodline race, double scale) {
		Grid grid = sized(scale);
		Sculptor s = new Sculptor(grid, scale);
		s.tubeMin = 0.87;
		s.blobMin = 0.8;
		beast(s, race, false);
		return grid;
	}

	/** A grid for a beast at {@code scale}: the colossus's, scaled, a voxel to spare each way. */
	static Grid sized(double scale) {
		return new Grid((int) Math.ceil(COLOSSUS_W * scale) + 2, (int) Math.ceil(COLOSSUS_H * scale) + 2, (int) Math.ceil(COLOSSUS_D * scale) + 2);
	}

	private static void beast(Sculptor s, Bloodline race, boolean colossus) {
		switch (race) {
			case FOX -> fox(s, colossus);
			case LIZARD -> lizard(s, colossus);
			case DRAGON -> dragon(s, colossus);
			default -> tiger(s, colossus);
		}
	}

	// --- The white tiger, crouching ---

	/**
	 * Tiger stripes: bands across its length, wavering, none on its belly; {@code coarse} times as wide and as far apart (a small
	 * statue's: at a third of a block a unit they broke into spots).
	 */
	private static Painter tigerCoat(double belly, double coarse) {
		double period = 7.5 * coarse;
		return (x, y, z) -> {
			if (y < belly) return Paint.BELLY;
			double band = (z + 2.2 * coarse * Math.sin(y * 0.33 / coarse + x * 0.12) + 1.4 * Math.sin(Math.abs(x) * 0.28)) % period;
			if (band < 0) band += period;
			double width = (1.2 + 0.9 * wave(x, y, z)) * coarse;
			return band < width && Math.abs(x) > 1.2 ? Paint.MARK : Paint.BODY;
		};
	}

	private static void tiger(Sculptor s, boolean colossus) {
		Painter coat = tigerCoat(6, Math.max(1, 0.7 / s.scale));
		s.push = 9; // its forepaws reach out in front of its chest: room for them
		// Its haunches and body, low behind, rising to the chest.
		s.blob(0, 11, 52, 17, 11, 13, coat);
		s.blob(0, 13, 39, 15.5, 12, 16, coat);
		s.blob(0, 16, 27, 15, 14.5, 11, coat);
		// Its hind legs folded under it, their paws forward at its sides.
		for (int k = -1; k <= 1; k += 2) {
			s.blob(k * 15, 9, 50, 7, 9, 10, coat);
			s.blob(k * 17.5, 3, 41, 5.5, 3.2, 7.5, Paint.BODY);
		}
		// Its forelegs reaching out together before its chest, paws flat on the ground side by side.
		for (int k = -1; k <= 1; k += 2) {
			s.chain(new double[][] {p(k * 8.5, 16, 26), p(k * 9, 9, 15), p(k * 8.5, 4, 5)}, 6.5, 5.2, coat);
			s.blob(k * 8.5, 3, 0, 6, 3.6, 7.5, Paint.BODY);
			for (int c = -1; c <= 1; c++) s.tube(p(k * 8.5 + c * 2.8, 2.2, -5.2), p(k * 8.5 + c * 3, 0.6, -8), 1.0, 0.35, Paint.CLAW);
		}
		// Its neck and great head raised over the hall's roof, looking out.
		s.tube(p(0, 22, 26), p(0, 31, 19), 11, 10, coat);
		Painter face = (x, y, z) -> {
			// The 王 on its brow, and a stripe or two over the eyes and down the cheeks.
			if (z < 9 && Math.abs(x) <= 3.6 && y > 38.5 && y < 46) {
				double bar = (y - 38.5) % 3.3;
				if (bar < 1.05 || Math.abs(x) < 0.6) return Paint.MARK;
			}
			if (Math.abs(x) > 6 && Math.abs(x) < 10.5 && (y + Math.abs(x) * 0.6) % 4.2 < 1.1 && z < 16) return Paint.MARK;
			return y < 30 ? Paint.BELLY : Paint.BODY;
		};
		s.blob(0, 36, 17, 11.5, 10.5, 10.5, face);
		// The ruff of its cheeks.
		for (int k = -1; k <= 1; k += 2) s.blob(k * 9.5, 31.5, 15, 4.5, 5.5, 6, face);
		// Its muzzle and nose, its jaw, its fangs.
		s.blob(0, 31.5, 7.5, 7, 5, 5.5, (x, y, z) -> y < 30.5 ? Paint.BELLY : Paint.BODY);
		s.blob(0, 34.6, 2.6, 2.6, 1.8, 1.8, Paint.MARK);
		s.blob(0, 27.6, 9, 5.2, 2.6, 4.6, Paint.BELLY);
		for (int k = -1; k <= 1; k += 2) s.tube(p(k * 2.6, 29.5, 3.6), p(k * 2.5, 25.6, 4.2), 1.05, 0.3, Paint.FANG);
		// Its eyes, burning, under a dark brow.
		for (int k = -1; k <= 1; k += 2) {
			s.tube(p(k * 2.2, 40.4, 7.4), p(k * 7.5, 39.6, 9.4), 0.9, 0.9, Paint.MARK);
			s.blob(k * 4.8, 38.2, 7.6, 1.9, 1.3, 1.1, Paint.EYE);
		}
		// Its ears, round, dark behind.
		for (int k = -1; k <= 1; k += 2) {
			s.blob(k * 7.5, 45.6, 18.5, 3.4, 3.6, 2.2, (x, y, z) -> z > 18.8 ? Paint.MARK : Paint.BODY);
		}
		// Its tail curling round its right flank, ringed, its tip dark.
		s.chain(new double[][] {p(0, 14, 64), p(8, 8, 67), p(18, 4, 63), p(24, 3, 53), p(25.5, 4, 42), p(24, 6, 33)}, 3.6, 2.2,
				(x, y, z) -> (z * 0.7 + x * 0.7) % 6 < 1.6 || z < 36 ? Paint.MARK : Paint.BODY);
	}

	// --- The nine-tailed fox, sitting up ---

	private static void fox(Sculptor s, boolean colossus) {
		Painter coat = (x, y, z) -> Paint.BODY;
		Painter chest = (x, y, z) -> z < 26 && Math.abs(x) < 7 ? Paint.BELLY : Paint.BODY;
		// Its nine tails first (its body over their roots): fanned out behind it like a halo, each white, its end afire. Each swells from
		// its root and tapers to a point that curls a little outward (not a row of fingers: the user, 2026-10-25), the middle ones a
		// little further back, so the fan cups round it.
		for (int i = 0; i < 9; i++) {
			double a = Math.toRadians(-76 + i * 19);
			double sx = Math.sin(a), cy = Math.cos(a), out = Math.signum(sx), cup = 3 * cy * cy;
			double[][] tail = {p(sx * 3, 9, 50), p(sx * 10, 12 + cy * 6, 57 + cup * 0.4), p(sx * 21, 19 + cy * 17, 60 + cup),
					p(sx * 29 + out * 1.5, 27 + cy * 26, 58 + cup), p(sx * 31 + out * 3.5, 32 + cy * 31, 55 + cup * 0.6)};
			double reach = 31 + 31 * Math.abs(cy);
			s.chain(tail, new double[] {3.0, 4.6, 5.2, 4.2, 1.6}, (x, y, z) -> Math.hypot(x - sx * 3, y - 9) > reach * 0.72 ? Paint.TIP : Paint.BODY);
			double[] end = tail[tail.length - 1];
			s.blob(end[0], end[1], end[2], 1.2, 1.2, 1.2, Paint.GLOW);
		}
		// Its haunches and body sitting up, its white chest out.
		s.blob(0, 10, 47, 14, 10, 12, coat);
		s.blob(0, 21, 39, 11.5, 16, 10, coat);
		s.blob(0, 25, 31, 10, 13, 8, chest);
		// Its hind paws forward at its sides, its forelegs straight down before its chest.
		for (int k = -1; k <= 1; k += 2) {
			s.blob(k * 13, 3, 41, 4.8, 3, 8, Paint.BODY);
			s.chain(new double[][] {p(k * 6.5, 22, 30), p(k * 7.5, 11, 27), p(k * 8, 3, 25)}, 3.6, 3, (x, y, z) -> y < 7 ? Paint.MARK : Paint.BODY);
			s.blob(k * 8, 2, 22.5, 3.8, 2.2, 5, Paint.MARK);
		}
		// Its neck and head high over the hall's roof, its long muzzle out.
		s.tube(p(0, 33, 33), p(0, 41, 29), 7.5, 6.5, chest);
		s.blob(0, 44, 27, 8, 7.2, 7.6, (x, y, z) -> y < 42 && z < 26 ? Paint.BELLY : Paint.BODY);
		for (int k = -1; k <= 1; k += 2) s.blob(k * 6.5, 41.5, 26.5, 3.2, 3.8, 4.2, Paint.BELLY); // its cheek ruffs
		s.tube(p(0, 42, 21), p(0, 40.6, 12), 4.2, 1.9, (x, y, z) -> y < 40.4 ? Paint.BELLY : Paint.BODY);
		s.blob(0, 40.8, 11.4, 1.5, 1.3, 1.3, Paint.MARK);
		// Its eyes, slanted gold, and the red marks over them.
		for (int k = -1; k <= 1; k += 2) {
			s.tube(p(k * 2.4, 45.4, 20.5), p(k * 5.6, 46.6, 22), 0.8, 0.6, Paint.EYE);
			s.tube(p(k * 1.8, 47.6, 20.6), p(k * 6, 48.8, 23.4), 0.7, 0.5, Paint.TIP);
		}
		// Its tall ears, red within.
		for (int k = -1; k <= 1; k += 2) {
			s.tube(p(k * 4.6, 49, 28), p(k * 7.2, 59, 29.5), 3.4, 0.5, (x, y, z) -> z < 27.8 && y < 57 ? Paint.TIP : Paint.BODY);
		}
		// A jewel of gold on its brow.
		s.blob(0, 48.6, 20.6, 1.2, 1.4, 0.8, Paint.CROWN);
	}

	// --- The mountain-bearing lizard king, propped high ---

	/**
	 * Its hide: plates of the land's dark green, a few darker, broken by seams where the earth's fire glows through (cells round
	 * jittered points, as the elder's: a speckle of sines read as spots).
	 */
	private static Paint lizardHide(double x, double y, double z) {
		// Great plates (a seam half a voxel wide joins up across a surface; on plates of 6 or 7 it took two fifths of the hide).
		double[] plate = plates(x, y, z, 13);
		if (plate[0] < 0.45) return plate[1] < 0.6 ? Paint.GLOW : Paint.MARK;
		return plate[1] < 0.2 ? Paint.MARK : Paint.BODY;
	}

	/**
	 * The lizard king lies long and low across the back of the hall, seen side on from the way up (propped up and faced square on it
	 * read as a frog: the user, 2026-10-25): its head raised on the left and turned to look down the way, its tail sweeping round on the
	 * right and curling forward, the mountain it bears on its back over the hall's roof.
	 */
	private static void lizard(Sculptor s, boolean colossus) {
		Painter hide = PalaceSculpt::lizardHide;
		Painter under = (x, y, z) -> y < 3.5 ? Paint.BELLY : lizardHide(x, y, z);
		double back = 34;
		// Its long body across, its belly on the ground, from its shoulders on the left to its hips on the right.
		s.chain(new double[][] {p(-22, 9, back), p(-8, 10.5, back + 1), p(6, 10.5, back + 1), p(18, 9, back)}, 9.5, 9, under);
		// Its legs sprawled out to either side of it, elbows and knees high, the forelegs reaching forward, the hind legs back.
		for (int k = -1; k <= 1; k += 2) {
			s.chain(new double[][] {p(-17, 9, back + k * 6), p(-22, 12.5, back + k * 13), p(-26, 2.2, back + k * 16)}, 4.6, 3.6, hide);
			s.blob(-27, 1.6, back + k * 17, 5, 1.8, 4.2, Paint.BODY);
			for (int c = -1; c <= 1; c++) s.tube(p(-30, 1.2, back + k * 17 + c * 2.4), p(-33, 0.6, back + k * 17.5 + c * 3), 0.85, 0.3, Paint.CLAW);
			s.chain(new double[][] {p(13, 9, back + k * 6), p(18, 12.5, back + k * 13), p(23, 2.2, back + k * 15)}, 5, 3.8, hide);
			s.blob(25, 1.6, back + k * 15.5, 5, 1.8, 4.4, Paint.BODY);
			for (int c = -1; c <= 1; c++) s.tube(p(28, 1.2, back + k * 15.5 + c * 2.4), p(31, 0.6, back + k * 16 + c * 3), 0.85, 0.3, Paint.CLAW);
		}
		// Its neck raised on the left, curving forward, its long head looking down the way.
		s.chain(new double[][] {p(-22, 11, back), p(-30, 16, back - 4), p(-34, 21, back - 11)}, 8, 6.5, hide);
		s.blob(-35, 23.5, back - 16, 6.2, 5, 7.5, under);
		s.blob(-35, 22.6, back - 25, 4.4, 3.4, 6.5, under); // its snout
		s.blob(-35, 19.8, back - 20, 4.8, 1.9, 8, Paint.BELLY); // its jaw
		for (int z = -27; z <= -14; z += 3) {
			for (int k = -1; k <= 1; k += 2) s.tube(p(-35 + k * 4.0, 21.2, back + z), p(-35 + k * 3.8, 19.8, back + z), 0.5, 0.25, Paint.FANG);
		}
		for (int k = -1; k <= 1; k += 2) {
			s.tube(p(-35 + k * 3.4, 27.4, back - 21), p(-35 + k * 5.6, 26.6, back - 13), 1.0, 1.2, Paint.MARK); // its brow ridge
			s.blob(-35 + k * 5.6, 25.4, back - 17, 1.4, 1.5, 1.8, Paint.EYE);
			s.chain(new double[][] {p(-35 + k * 3.4, 27.5, back - 12), p(-35 + k * 5.5, 31, back - 7), p(-35 + k * 6.5, 33, back - 2)}, 1.8, 0.5, Paint.CROWN);
			s.blob(-35 + k * 1.8, 24.4, back - 31, 0.8, 0.7, 0.7, Paint.MARK); // a nostril
		}
		// Jade crystals along its spine, from its neck to the root of its tail.
		for (int i = 0; i < 9; i++) {
			double x = -26 + i * 5.6, y = i == 0 ? 22 : 18.5;
			s.tube(p(x, y - 3, back), p(x + 0.8, y + 3.6 - Math.abs(i - 4) * 0.3, back + 0.6), 2.0, 0.5, Paint.CROWN);
		}
		// Its tail sweeping on round to the right and curling forward, thinning.
		s.chain(new double[][] {p(18, 9, back), p(28, 7.5, back + 2), p(34, 5, back - 4), p(35, 3.5, back - 14), p(31, 2.5, back - 23), p(24, 2, back - 28)},
				8.5, 1.8, under);
		// The mountain it bears on its back, over the hall's roof: a crag of three peaks, jade at its top, fire in its seams.
		if (colossus) {
			Painter rock = (x, y, z) -> {
				double[] crack = plates(x, y, z, 11);
				return crack[0] < 0.4 && crack[1] < 0.5 ? Paint.GLOW : y > 45 ? Paint.CROWN : Paint.STONE;
			};
			s.chain(new double[][] {p(-2, 14, back + 1), p(-1, 27, back + 2), p(-2, 39, back + 2), p(-2, 48, back + 2)}, 12, 3, rock);
			s.chain(new double[][] {p(-12, 14, back), p(-13, 24, back + 1), p(-14, 32, back + 1)}, 7, 2, rock);
			s.chain(new double[][] {p(9, 14, back), p(10, 22, back), p(11, 29, back + 1)}, 6.5, 2, rock);
		}
	}

	// --- The azure dragon, coiled ---

	/** Its scales: rows of the darker blue over the azure; its belly plates gold. */
	private static Painter dragonScales(double bellyBelow) {
		return (x, y, z) -> {
			if (y < bellyBelow) return Paint.BELLY;
			return (Math.floor(x * 0.55) + Math.floor(y * 0.55) + Math.floor(z * 0.55)) % 3 == 0 ? Paint.MARK : Paint.BODY;
		};
	}

	/**
	 * The azure dragon, coiled behind the hall: no legs (the user, 2026-10-25), a thick body wound once and a half round a rock and
	 * rising, its neck reaching forward from the top of its coil to hold its head high over the hall's roof, looking down the way.
	 */
	private static void dragon(Sculptor s, boolean colossus) {
		Painter scales = dragonScales(-100);
		s.push = 4; // its snout and whiskers reach out before it
		// Its body coiled behind the hall: once and a half round a rock, rising, then up and forward to its neck.
		double cx = 0, cz = 46, ring = 18;
		int steps = 30;
		double[][] coil = new double[steps + 1][];
		for (int i = 0; i <= steps; i++) {
			double t = i / (double) steps;
			double a = Math.toRadians(-90 - 540 * t);
			coil[i] = p(cx + Math.cos(a) * ring * (1 - 0.25 * t), 7.5 + 21 * t, cz + Math.sin(a) * ring * (1 - 0.25 * t) * 0.85);
		}
		if (colossus) s.tube(p(0, 0, cz), p(0, 24, cz), 9, 6, (x, y, z) -> Paint.STONE); // the rock it coils round
		s.chain(coil, 7.6, 7.2, scales);
		// Its dorsal fins along the coil.
		for (int i = 1; i < steps; i += 2) {
			double[] c = coil[i];
			s.tube(p(c[0], c[1] + 6, c[2]), p(c[0] * 1.08, c[1] + 11, c[2] + 1), 1.8, 0.4, Paint.MANE);
		}
		// Its tail end trailing out along the ground round the rock, a tuft of fin at its tip.
		s.chain(new double[][] {coil[0], p(-8, 4.5, 30), p(-20, 3.5, 29), p(-29, 3, 37)}, 7.2, 2.6, scales);
		s.blob(-31, 4, 39.5, 3.8, 3.2, 2.8, Paint.MANE);
		// From the top of its coil its neck rises forward over the hall.
		double[] top = coil[steps];
		s.chain(new double[][] {top, p(0, 35, 34), p(0, 41, 25), p(0, 43, 18)}, 7.2, 6.2, scales);
		// Its head: a long skull, a broad snout with its nostrils, jaws parted, fangs, a mane behind.
		s.blob(0, 45, 14, 8, 6.8, 9, scales);
		s.blob(0, 43.2, 5, 6, 4.6, 6, scales);
		s.blob(0, 39.4, 7, 5.4, 2.2, 6.4, Paint.BELLY); // its lower jaw
		for (int k = -1; k <= 1; k += 2) {
			s.blob(k * 2.6, 46.2, -0.2, 1.2, 1.0, 0.9, Paint.MARK); // a nostril
			s.tube(p(k * 3.2, 41.6, 0.8), p(k * 3.2, 38.4, 1.6), 0.85, 0.3, Paint.FANG);
			s.blob(k * 5.6, 47.8, 8.8, 2.0, 1.4, 1.3, Paint.EYE);
			s.tube(p(k * 4.2, 50, 7), p(k * 7.6, 50.4, 11.5), 1.1, 0.7, Paint.MARK); // its brow
			// Its antlers of gold, branching.
			s.chain(new double[][] {p(k * 4.2, 50, 15), p(k * 6.5, 55, 18.5), p(k * 8.5, 60, 23), p(k * 9, 63.5, 27)}, 1.9, 0.8, Paint.CROWN);
			s.tube(p(k * 6.6, 55.5, 19), p(k * 11.5, 58, 17.5), 1.1, 0.45, Paint.CROWN);
			s.tube(p(k * 8.4, 60, 23), p(k * 13, 62, 22), 1.0, 0.4, Paint.CROWN);
			// Its whiskers, long, drooping out from its snout (thick enough to stay whole in blocks: thinner, a colossus's broke into
			// loose bits along their slope).
			s.chain(new double[][] {p(k * 5, 43.5, 1.5), p(k * 11, 43, -1), p(k * 17, 40, -2), p(k * 21, 35, 0)}, 1.3, 1.05, Paint.MANE);
			// The mane flowing back from its jaw.
			s.chain(new double[][] {p(k * 6.5, 44, 18), p(k * 8.5, 40.5, 24), p(k * 7, 36, 29)}, 3, 1.1, Paint.MANE);
		}
		s.chain(new double[][] {p(0, 51.5, 19), p(0, 51, 26), p(0, 46, 32)}, 3.2, 1.3, Paint.MANE);
	}

	private PalaceSculpt() {}
}
