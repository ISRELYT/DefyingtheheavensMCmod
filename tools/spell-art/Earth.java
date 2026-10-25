/**
 * The earth element's Dao Integration spells for SpellArt: the Qilin of the centre, golden-scaled, molten gold in its veins, its hooves
 * wreathed in earth-fire (Qilin Tramples the Land); and the Seal of Ten Thousand Mountains, a great seal of yellow jade crowned with
 * mountains, its face pressed in vermilion and carved with four characters of seal script.
 */
public class Earth {
	// --- The earth element's colours: umber, bronze, ochre and gold scales, cream; the molten gold in the earth's veins; hooves ---
	static final int RUST = 0x9A4218, BRONZE = 0x8C5220, OCHRE = 0xD47C24, GOLD_SCALE = 0xF4B434, GOLD_PALE = 0xFFDA7C, CREAM = 0xF8E6BC, CREAM_SEAM = 0xC49C64;
	static final int MOLTEN = 0xFFB838, MOLTEN_CORE = 0xFFF0B8, EMBER = 0xE07A1E, HOOF = 0x2E2418, HOOF_LIGHT = 0x5A4A30;
	// --- Yellow jade, stone, pine and mist; the seal's paste and its cord ---
	static final int JADE_Y = 0xD6B262, JADE_Y_LIGHT = 0xF0D894, JADE_Y_DARK = 0x957030, STONE = 0x746A5C, STONE_LIGHT = 0xA49886, STONE_DARK = 0x3E372E;
	static final int MOSS = 0x5E7A3A, PINE = 0x2E5A34, PINE_LIGHT = 0x4E8A4C, MIST = 0xF4F2EA, PASTE = 0xB0200F, PASTE_DARK = 0x7A1208, CORD = 0xC41E1E, CORD_DARK = 0x701010;

	/** The earth's fire: from bronze at its cool edges through ember to molten gold and its white-gold heart. */
	static final int[] EARTH_FIRE = {BRONZE, EMBER, MOLTEN, GOLD_PALE, MOLTEN_CORE};

	/** {@code c} lit ({@code by} > 0, toward pale gold) or shadowed (toward a warm umber, not black: gold shaded toward black turns olive). */
	static int warm(int c, double by) { return by >= 0 ? SpellArt.mix(c, 0xFFF4D0, by) : SpellArt.mix(c, 0x3A1606, -by); }

	// --- The Qilin's painters ---

	/**
	 * The qilin's hide round a body whose axis runs along z at x = 0, y = {@code axisY}: overlapping scales, bronze along the back to gold
	 * on the flanks, each scale's rim lit; plates of cream beneath; and down each flank a seam of molten gold, the earth's vein, branching.
	 */
	static SpellArt.Painter hide(double axisY, double rowLength, int columns, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.ny < -0.6 && t.flat()) a = 1;
			// Rows run along the body, and down a face that looks forward or back (along which z doesn't change).
			boolean across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			double along = across ? -t.qy() : qz;
			if (a > 0.78) {
				double band = along / 2.2 - Math.floor(along / 2.2);
				int c = SpellArt.mix(CREAM, GOLD_PALE, SpellArt.noise(qx * 0.6, qz * 0.4, seed) * 0.5);
				if (band < 0.15) return SpellArt.col(SpellArt.mix(c, CREAM_SEAM, 0.8));
				return SpellArt.col(warm(c, -0.1 * band));
			}
			// The vein: a seam of molten gold down each flank, wandering, with branches up and back.
			double side = Math.abs(th), vein = 1.5 + 0.18 * Math.sin(qz * 0.21 + seed) + 0.07 * Math.sin(qz * 0.7), r = Math.hypot(qx, qy);
			double off = (side - vein) * Math.max(2, r);
			if (Math.abs(off) < 0.35) return SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.4), 0.95);
			double branch = Math.floorMod((int) Math.floor((qz + 3 * Math.signum(th)) * 10), 70) / 10.0;
			if (!across && off < 0 && off > -2.6 && branch < 2.8 && Math.abs(-off - branch * 0.9) < 0.24) return SpellArt.glow(SpellArt.mix(MOLTEN, EMBER, 0.3), 0.9);
			// The scales, their free rims toward the tail (or down).
			double rowF = along / rowLength, arc = across ? qx / 2.2 : th / (2 * Math.PI) * columns;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = arc + Math.floorMod(row, 2) * 0.5;
			int ci = (int) Math.floor(colF);
			double u = colF - ci, rim = 0.32 + 0.68 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			int base = SpellArt.ramp(a / 0.78, RUST, OCHRE, GOLD_SCALE, GOLD_PALE);
			double h = SpellArt.hash(ci, row, seed) - 0.5;
			int c;
			if (v > rim) c = warm(base, -0.26);
			else if (rim - v < 0.13) c = warm(base, -0.3);
			else if (rim - v < 0.27 && a > 0.2 && a < 0.6) return SpellArt.glow(SpellArt.mix(base, GOLD_PALE, 0.5 + h * 0.3), 0.7);
			else c = warm(base, -0.08 + 0.24 * (v / rim) + h * 0.1);
			if (Math.abs(off) < 0.9) c = SpellArt.mix(c, EMBER, 0.35);
			return SpellArt.col(c);
		};
	}

	/** A limb's (or the neck's) scales: small, bronze to gold, cream where it faces down; the rows' rims dark. */
	static SpellArt.Painter limbHide(int seed) {
		return t -> {
			int row = (int) Math.floor(t.ly / 1.2), col = (int) Math.floor((t.lx + t.lz) / 1.3 + Math.floorMod(row, 2) * 0.5);
			double v = t.ly / 1.2 - row;
			int c = SpellArt.mix(BRONZE, GOLD_SCALE, 0.35 + SpellArt.hash(row, col, seed) * 0.35 + (1 - t.up()) * 0.2);
			if (t.ny < -0.5) c = SpellArt.mix(c, CREAM, 0.45);
			return SpellArt.col(v > 0.82 ? warm(c, -0.35) : warm(c, 0.14 * (1 - v) - 0.04));
		};
	}

	/** The head's fine scales: bronze over the crown and down the bridge, gold down the sides, cream beneath. */
	static SpellArt.Painter headHide(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			if (t.ny < -0.55) {
				double band = qz / 1.4 - Math.floor(qz / 1.4);
				return SpellArt.col(band < 0.18 ? SpellArt.mix(CREAM, CREAM_SEAM, 0.6) : warm(CREAM, -0.06 * band));
			}
			int base = t.ny > 0.55 ? SpellArt.mix(BRONZE, OCHRE, 0.5 + 0.35 * SpellArt.noise(qx * 0.5, qz * 0.5, seed)) : SpellArt.ramp(SpellArt.clamp01((qy - 46) / 12), GOLD_PALE, GOLD_SCALE, OCHRE);
			double a = t.ny > 0.55 ? qx : qy, rowF = qz / 1.2;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = a / 1.1 + Math.floorMod(row, 2) * 0.5;
			int ci = (int) Math.floor(colF);
			double u = colF - ci, rim = 0.3 + 0.7 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			double h = SpellArt.hash(ci, row, seed) - 0.5;
			return SpellArt.col(v > rim ? warm(base, -0.3) : rim - v < 0.15 ? warm(base, -0.4) : warm(base, -0.12 + 0.3 * v / rim + h * 0.1));
		};
	}

	/** The nose's front: broad, ridged, two dark nostrils; elsewhere the head's scales. */
	static SpellArt.Painter nose(SpellArt.Painter skin) {
		return t -> {
			if (t.f == SpellArt.Face.FRONT) {
				double x = t.across() - 0.5, y = t.up() - 0.5;
				if (Math.hypot(Math.abs(x) - 0.26, (y + 0.05) * 1.4) < 0.13) return SpellArt.col(0x1A1008);
				return SpellArt.col(warm(GOLD_SCALE, -0.12 + 0.2 * (y + 0.5) + (Math.abs(y * 6 - Math.rint(y * 6)) < 0.12 ? -0.22 : 0)));
			}
			return skin.paint(t);
		};
	}

	/**
	 * A lock of the mane (on a blade, its length along it): hair like flame, ochre at its root to pale gold, in strands; its outer part
	 * glowing earth-fire, wisping thin.
	 */
	static SpellArt.Painter maneLock(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.08 * Math.sin(v * 6 + seed) * v, half = 0.46 * Math.pow(1 - v, 0.55) * (0.9 + 0.1 * Math.sin(v * 14 + seed));
			if (Math.abs(uu) > half) return SpellArt.CLEAR;
			double strand = Math.abs(Math.sin(uu / Math.max(0.04, half) * 4.5 + v * 3 + seed));
			int c = warm(SpellArt.ramp(v, OCHRE, GOLD_SCALE, GOLD_PALE, MOLTEN_CORE), -0.1 + 0.2 * strand);
			if (v > 0.3) return SpellArt.glow(SpellArt.mix(c, MOLTEN, (v - 0.3) * 0.8), 0.97 - (v - 0.3) * 0.7);
			return SpellArt.col(c);
		};
	}

	/** Amber crystal, lit from within, its facets catching the light; if {@code tip}, white-gold toward its point (the top of its cube). */
	static SpellArt.Painter amber(boolean tip, int seed) {
		return t -> {
			double facet = SpellArt.hash((int) Math.floor(t.ly * 0.8), t.f.ordinal(), seed);
			int c = SpellArt.mix(EMBER, MOLTEN, 0.4 + 0.5 * facet);
			if (tip && t.up() > 0.6) c = SpellArt.mix(c, MOLTEN_CORE, (t.up() - 0.6) * 2.5);
			if (Math.abs(t.fx - 0.5) < 0.12 && !t.flat()) c = SpellArt.mix(c, MOLTEN_CORE, 0.4);
			return SpellArt.glow(c, 0.92);
		};
	}

	/** A hoof: dark horn, cloven (a dark split down its front), a band of molten gold round its top. */
	static SpellArt.Painter hoof() {
		return t -> {
			if ((t.f == SpellArt.Face.FRONT || t.flat()) && Math.abs(t.across() - 0.5) < 0.07) return SpellArt.col(0x120C06);
			if (t.up() > 0.8 && !t.flat()) return SpellArt.glow(MOLTEN, 0.95);
			return SpellArt.col(SpellArt.mix(HOOF, HOOF_LIGHT, 0.3 + 0.4 * t.up() + (SpellArt.hash(t.x, t.y, 5) - 0.5) * 0.15));
		};
	}

	/** An ear: a pointed leaf of gold scale, cream inside. */
	static SpellArt.Painter ear() {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.5 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(v, 0.7))), 0.7);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			if (Math.abs(u) < half * 0.5 && v > 0.15 && v < 0.8) return SpellArt.col(SpellArt.mix(CREAM, EMBER, 0.15));
			return SpellArt.col(SpellArt.mix(OCHRE, GOLD_SCALE, v));
		};
	}

	// --- The Qilin, made of earth ---

	/** The qilin's stone: sandstone, ochre and umber plates, grey rock; its cracks; deep beneath. */
	static final int SANDSTONE = 0xC9A46A, SANDSTONE_LIGHT = 0xE2C48C, UMBER = 0x6E4A2A, CRACK = 0x241810;

	/**
	 * Rock in plates (a solid, painted where it lies in the model): stones fitted together, each its own sandstone, ochre, umber or grey,
	 * chiselled (lit toward its middle, shadowed to its edges), dark cracks between them, some cracks running with molten gold; darker
	 * beneath. {@code size} is a plate's breadth, in pixels.
	 */
	static SpellArt.Painter rock(double size, int seed) {
		return t -> {
			double x = t.px / size, y = t.py / size, z = t.pz / size;
			int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
			double d1 = 9, d2 = 9;
			int cell = 0;
			for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
				int cx = ix + dx, cy = iy + dy, cz = iz + dz;
				double px = cx + SpellArt.hash3(cx, cy, cz, seed), py = cy + SpellArt.hash3(cx, cy, cz, seed + 1), pz = cz + SpellArt.hash3(cx, cy, cz, seed + 2);
				double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
				if (d < d1) {
					d2 = d1;
					d1 = d;
					cell = cx * 73856093 ^ cy * 19349663 ^ cz * 83492791;
				} else if (d < d2) d2 = d;
			}
			double gap = d2 - d1;
			if (gap < 0.1) {
				// A crack: dark, or running with molten gold where the earth's fire shows through.
				if (SpellArt.noise3(t.px * 0.06, t.py * 0.06, t.pz * 0.06, seed + 3) > 0.73) return SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.3 + gap * 4), 0.95);
				return SpellArt.col(CRACK);
			}
			double h = SpellArt.hash(cell, 1, seed);
			int c = h < 0.3 ? SANDSTONE : h < 0.55 ? OCHRE : h < 0.75 ? SpellArt.mix(STONE, STONE_LIGHT, 0.4) : h < 0.92 ? UMBER : SpellArt.mix(SANDSTONE_LIGHT, GOLD_PALE, 0.3);
			double grit = SpellArt.noise3(t.px * 0.9, t.py * 0.9, t.pz * 0.9, seed + 4);
			c = warm(c, -0.22 + 0.34 * SpellArt.smooth(0.08, 0.4, gap) - 0.12 * d1 + (grit - 0.5) * 0.14);
			if (t.ny < -0.5) c = warm(c, -0.25);
			return SpellArt.col(c);
		};
	}

	/**
	 * Rock in plates round a body whose axis runs along z (x = 0, y = {@code axisY}): {@link #rock}, and down each flank the earth's vein,
	 * a seam of molten gold wandering along it with branches up and back.
	 */
	static SpellArt.Painter stoneHide(double axisY, int seed) {
		SpellArt.Painter stone = rock(5.4, seed);
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			boolean across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			double th = Math.atan2(qx, qy), side = Math.abs(th), r = Math.hypot(qx, qy);
			double vein = 1.5 + 0.18 * Math.sin(qz * 0.21 + seed) + 0.07 * Math.sin(qz * 0.7), off = (side - vein) * Math.max(2, r);
			if (!across && Math.abs(off) < 0.4) return SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.4), 0.96);
			double branch = Math.floorMod((int) Math.floor((qz + 3 * Math.signum(th)) * 10), 70) / 10.0;
			if (!across && off < 0 && off > -2.6 && branch < 2.8 && Math.abs(-off - branch * 0.9) < 0.26) return SpellArt.glow(SpellArt.mix(MOLTEN, EMBER, 0.3), 0.92);
			return stone.paint(t);
		};
	}

	/** An eye of the earth on the outer side of its cube: a slit of molten gold in the rock ({@code around}), white-hot at its middle. */
	static SpellArt.Painter moltenEye(SpellArt.Painter around) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return around.paint(t);
			double x = (t.back() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = Math.abs(y) - (1 - x * x) * 0.55;
			if (shape > 0.06) return around.paint(t);
			return SpellArt.glow(SpellArt.mix(MOLTEN_CORE, MOLTEN, SpellArt.clamp01(Math.abs(x) * 0.8 + shape + 0.3)));
		};
	}

	/**
	 * A shard of stone on a blade (its length along it): a jagged flake, broad at its foot and broken to an uneven point, sandstone and
	 * umber in layers across it, its broken edges glowing with molten gold; gold light flickering out at its tip.
	 */
	static SpellArt.Painter shard(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			// Its outline: straight breaks, by steps (a flake struck from rock), narrowing to its point.
			double step = Math.floor(v * 5 + SpellArt.hash(seed, 2, 1) * 0.5) / 5;
			double half = 0.48 * (1 - Math.pow(v, 1.4)) * (0.72 + 0.28 * SpellArt.hash((int) Math.floor(v * 5), seed, 3)) + 0.03 * (1 - step);
			double lean = 0.12 * (SpellArt.hash(seed, 4, 5) - 0.5) * v;
			if (Math.abs(u - lean) > half || v > 0.985) return SpellArt.CLEAR;
			double edge = half - Math.abs(u - lean);
			if (edge < 0.05 + 0.03 * v) return SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, v), 0.9);
			if (v > 0.8) return SpellArt.glow(SpellArt.mix(MOLTEN, GOLD_PALE, (v - 0.8) * 4), 0.85);
			double layer = v * 7 + (u - lean) * 2.5 + SpellArt.noise(u * 6 + seed, v * 3, seed) * 0.8;
			int c = Math.floorMod((int) Math.floor(layer), 3) == 0 ? UMBER : Math.floorMod((int) Math.floor(layer), 3) == 1 ? SANDSTONE : OCHRE;
			return SpellArt.col(warm(c, -0.15 + 0.3 * Math.min(1, edge * 6)));
		};
	}

	/** A hoof of the earth: a boulder ({@link #rock}), a band of molten gold round its top. */
	static SpellArt.Painter boulder(int seed) {
		SpellArt.Painter stone = rock(2.2, seed);
		return t -> t.up() > 0.78 && !t.flat() ? SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.25), 0.95) : stone.paint(t);
	}

	/**
	 * The Qilin, the beast of the centre, made of earth (the user, 2026-10-24: the great spells' beasts made of their elements, not looking
	 * like animals): its body ("body", pivoting at the hips, so it rears) of fitted rock, cracks of molten gold through it, the earth's vein
	 * glowing down each flank, ridged with amber crystal; its neck ("neck") and a mane of stone shards ("mane") edged with molten gold; its
	 * head ("head", the jaw "jaw") of rock with slits of molten gold for eyes, its one horn of amber ("horn"), whiskers of molten gold, a beard
	 * of shards; its legs ("leg_fl", "leg_fr", "leg_bl", "leg_br", each with its "_low" and "_hoof") pillars of rock on boulders, wreathed
	 * in earth-fire; its tail ("tail_0".."tail_3") of stones, a tuft of shards. Its bones the beast's ({@link #qilinBeast}), so it gallops
	 * and rears as that did. It faces -z, its hooves on the ground at y 0.
	 */
	static void qilin(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 31, 14);
		SpellArt.Painter stone = stoneHide(31, 11);
		// The torso: the chest deep, the barrel, the hips (as the beast's), of fitted rock.
		body.box(0, 31, -12, 15, 17, 16, stone);
		body.box(0, 31.5, -12, 11, 20.4, 15, stone);
		body.box(0, 30.6, 1, 13.6, 15, 14, stone);
		body.box(0, 31, 1, 10, 17.6, 13, stone);
		body.box(0, 32, 14, 14, 15, 12, stone);
		body.box(0, 32.5, 14, 10.4, 17.2, 11, stone);
		body.box(0, 28.5, -20.6, 10, 12, 2.4, stone);
		body.box(0, 32, 21.2, 10, 12, 2.6, stone);
		// The spine's ridge of amber crystals, from the withers to the tail, leaning back.
		for (int k = 0; k < 13; k++) {
			double z = -18 + k * 3, top = z < -5 ? 41.7 : z < 8 ? 39.8 : 41.1, h = 3.6 - Math.abs(k - 3) * 0.2;
			SpellArt.limb(body, SpellArt.v3(0, top - 0.8, z), SpellArt.v3(0, top + h, z + 1.8), 1.6, 2.2, amber(true, 40 + k)).density(3);
		}
		// Shards jutting from its flanks and shoulders, as rock breaks from a cliff.
		for (int s = -1; s <= 1; s += 2) {
			for (int k = 0; k < 4; k++) {
				SpellArt.blade(body, SpellArt.v3(s * 7.2, 34 - k * 0.8, -14 + k * 9), 5.2 - k * 0.4, 3.4, -55, s * 25, s * 62, shard(150 + k + (s > 0 ? 0 : 5))).density(3);
			}
		}
		neckAndHeadOfEarth(m);
		legsOfEarth(m);
		tailOfEarth(m);
		gallop(m);
		rear(m);
	}

	static void neckAndHeadOfEarth(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 37, -16);
		SpellArt.Painter stone = rock(4, 21);
		double[] from = SpellArt.v3(0, 34.5, -15), to = SpellArt.v3(0, 50, -26.5);
		SpellArt.limb(neck, from, to, 9, 11, stone);
		SpellArt.limb(neck, from, to, 7, 13, stone);
		// The mane: shards of stone streaming back from the crest of the neck, either way, their edges molten.
		SpellArt.Bone mane = m.bone("mane", "neck", 0, 45, -22);
		for (int k = 0; k < 7; k++) {
			double[] at = SpellArt.add(SpellArt.add(from, SpellArt.times(SpellArt.sub(to, from), 1 - k / 6.0 * 0.92)), SpellArt.v3(0, 3.9, 5.2));
			for (int s = -1; s <= 1; s += 2) {
				SpellArt.blade(mane, SpellArt.add(at, SpellArt.v3(s * 1.4, 0, 0)), 13 - k * 0.5 - (k % 2) * 2.2, 5.2, -70 + k * 3 + (k % 2) * 7, s * (10 + (k % 2) * 10), s * (20 + (k % 3) * 9),
						shard(60 + k * 2 + (s > 0 ? 1 : 0))).density(2.5);
			}
		}
		SpellArt.Bone head = m.bone("head", "neck", 0, 50, -27);
		SpellArt.Painter face = rock(3.4, 31);
		head.box(0, 52.6, -31, 9, 8, 10, face);
		head.box(0, 53, -31, 7, 9.6, 9, face);
		head.box(0, 50.8, -39, 7.6, 6.2, 8.4, face);
		head.box(0, 51.6, -44.2, 6.8, 5, 2.4, face);
		head.box(0, 54.3, -38.6, 4.2, 1.2, 8, face);
		for (int s = -1; s <= 1; s += 2) {
			// The brow ridge over each eye, the eye a slit of molten gold, an ear of a stone flake, cheek shards, a whisker from beside the nose.
			SpellArt.limb(head, SpellArt.v3(s * 1.8, 55.8, -36.8), SpellArt.v3(s * 4.6, 57, -31.6), 2.2, 1.8, face);
			head.box(s * 4.55, 53.7, -33.8, 0.7, 2.4, 3, moltenEye(face)).density(4);
			SpellArt.blade(head, SpellArt.v3(s * 3.8, 56.6, -27.6), 6.6, 3.4, -40, s * 40, s * 50, shard(70 + (s > 0 ? 0 : 1))).density(3);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 4.6, 51.6 - k * 1.7, -30.6 + k), 10 - k * 1.5, 4, -80 - k * 12, s * (28 + k * 9), s * (24 + k * 12), shard(80 + k * 2 + (s > 0 ? 1 : 0))).density(2.5);
			}
			String up = "head";
			double[] at = SpellArt.v3(s * 3.6, 50.6, -44.6);
			double[][] rest = {{-8, s * 40, 0}, {6, -s * 10, 0}, {14, -s * 14, 0}};
			for (int k = 0; k < rest.length; k++) {
				SpellArt.Bone w = m.bone("whisker_" + (s > 0 ? "l" : "r") + "_" + k, up, at[0], at[1], at[2], rest[k][0], rest[k][1], rest[k][2]);
				int kk = k;
				w.box(at[0], at[1], at[2] + 2.6, 0.6 - k * 0.12, 0.6 - k * 0.12, 5.4, t -> SpellArt.glow(SpellArt.mix(GOLD_PALE, MOLTEN, (kk + t.back()) / 3), 0.97 - (kk + t.back()) * 0.14)).density(3);
				up = w.name;
				at = SpellArt.add(at, SpellArt.v3(0, 0, 5.2));
			}
		}
		// The horn: one, of amber crystal, rising from the brow and curving back, ringed at its foot.
		SpellArt.Bone horn = m.bone("horn", "head", 0, 57, -33.6);
		double[] h0 = SpellArt.v3(0, 56.4, -34), h1 = SpellArt.add(h0, SpellArt.v3(0, 4.8, 1.2)), h2 = SpellArt.add(h1, SpellArt.v3(0, 4, 3)), h3 = SpellArt.add(h2, SpellArt.v3(0, 2.4, 4));
		horn.box(0, 57.2, -33.8, 3.4, 1.2, 3.4, amber(false, 49)).rot(0, 45, 0);
		SpellArt.limb(horn, h0, h1, 2.6, 2.6, amber(false, 50)).density(3);
		SpellArt.limb(horn, h1, h2, 2, 2, amber(false, 51)).density(3);
		SpellArt.limb(horn, h2, h3, 1.3, 1.3, amber(true, 52)).density(3);
		// The jaw, a little open: rock, molten light within its mouth, a beard of shards.
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 48.6, -33.6, 8, 0, 0);
		jaw.box(0, 47.4, -38.8, 6.6, 2.4, 10.4, face);
		jaw.box(0, 48.75, -38.8, 4.2, 0.3, 9, t -> SpellArt.glow(SpellArt.mix(EMBER, MOLTEN, SpellArt.hash(t.x, t.y, 3) * 0.5), 0.9));
		head.box(0, 48.9, -38.8, 4.2, 0.3, 9, t -> SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, SpellArt.hash(t.x, t.y, 4) * 0.4), 0.9));
		for (int k = -1; k <= 1; k++) {
			SpellArt.blade(jaw, SpellArt.v3(k * 1.4, 46.4, -42 + Math.abs(k)), 9 - Math.abs(k) * 1.5, 3.4, -150 + Math.abs(k) * 8, k * 10, k * 8, shard(90 + k)).density(2.5);
		}
	}

	static void legsOfEarth(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			String n = names[k];
			SpellArt.Painter stone = rock(3, 200 + k);
			if (k < 2) {
				double[] shoulder = SpellArt.v3(s * 5.8, 29, -13), elbow = SpellArt.v3(s * 6, 16.5, -11.4), fetlock = SpellArt.v3(s * 5.8, 5.2, -12.6),
						toe = SpellArt.v3(s * 5.8, 2.4, -14.2);
				SpellArt.Bone leg = m.bone(n, "body", shoulder[0], shoulder[1], shoulder[2]);
				leg.box(s * 5.6, 26, -12.6, 6, 9, 8, stone);
				SpellArt.limb(leg, shoulder, elbow, 5.4, 6.4, stone);
				SpellArt.Bone low = m.bone(n + "_low", n, elbow[0], elbow[1], elbow[2]);
				SpellArt.limb(low, elbow, fetlock, 3.2, 3.6, stone);
				// Earth-fire streaming back from the elbow, and wreathing the fetlock.
				for (int j = 0; j < 3; j++) {
					SpellArt.blade(low, SpellArt.add(elbow, SpellArt.v3(0, -1, 1.6)), 7 - j * 1.2, 3.6, -60 - j * 18, s * (14 + j * 12), s * (18 + j * 10), Fire.flame(2, 300 + k * 5 + j, EARTH_FIRE)).density(2.5);
				}
				SpellArt.Bone hoofBone = m.bone(n + "_hoof", n + "_low", fetlock[0], fetlock[1], fetlock[2]);
				SpellArt.limb(hoofBone, fetlock, toe, 2.6, 2.8, stone);
				hoofBone.box(toe[0], 1.4, toe[2] - 0.4, 4.4, 2.8, 5, boulder(260 + k));
				fetlockFlames(hoofBone, fetlock, s, 320 + k * 7);
			} else {
				double[] hip = SpellArt.v3(s * 5.8, 31, 15), stifle = SpellArt.v3(s * 6.4, 20, 9.4), hock = SpellArt.v3(s * 6.2, 11, 18.6), fetlock = SpellArt.v3(s * 6, 4.6, 17.2),
						toe = SpellArt.v3(s * 6, 2.2, 15.8);
				SpellArt.Bone leg = m.bone(n, "body", hip[0], hip[1], hip[2]);
				leg.box(s * 5.6, 26.4, 13.2, 6.6, 12, 10, stone);
				SpellArt.limb(leg, hip, stifle, 6, 8, stone);
				SpellArt.Bone low = m.bone(n + "_low", n, stifle[0], stifle[1], stifle[2]);
				SpellArt.limb(low, stifle, hock, 4.4, 5, stone);
				for (int j = 0; j < 3; j++) {
					SpellArt.blade(low, SpellArt.add(hock, SpellArt.v3(0, 0.6, 1)), 7.4 - j * 1.2, 3.6, -58 - j * 16, s * (14 + j * 12), s * (18 + j * 10), Fire.flame(2, 340 + k * 5 + j, EARTH_FIRE)).density(2.5);
				}
				SpellArt.Bone hoofBone = m.bone(n + "_hoof", n + "_low", hock[0], hock[1], hock[2]);
				SpellArt.limb(hoofBone, hock, fetlock, 3, 3.4, stone);
				SpellArt.limb(hoofBone, fetlock, toe, 2.6, 2.8, stone);
				hoofBone.box(toe[0], 1.4, toe[2] - 0.4, 4.4, 2.8, 5, boulder(260 + k));
				fetlockFlames(hoofBone, fetlock, s, 360 + k * 7);
			}
		}
	}

	static void tailOfEarth(SpellArt.Model m) {
		String up = "body";
		double[] at = SpellArt.v3(0, 37, 21.6);
		double[][] dirs = {{0, -1.4, 6.6}, {0, -2.6, 6}, {0, -3.4, 5.4}, {0, -3.8, 4.6}};
		SpellArt.Painter stone = rock(2.4, 400);
		for (int k = 0; k < dirs.length; k++) {
			SpellArt.Bone b = m.bone("tail_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, dirs[k]);
			SpellArt.limb(b, at, next, 3.6 - k * 0.6, 3.6 - k * 0.6, stone);
			if (k == dirs.length - 1) {
				// The tuft: a fan of shards.
				for (int j = 0; j < 7; j++) {
					double fan = (j - 3) * 16;
					SpellArt.blade(b, next, 11 - Math.abs(j - 3), 4.6, -120 + Math.abs(j - 3) * 6, fan, fan * 0.6, shard(420 + j)).density(2.5);
				}
			}
			up = b.name;
			at = next;
		}
	}

	/**
	 * The Qilin, the beast of the centre: a deer's form under a dragon's golden scales, a seam of molten gold down each flank; its body
	 * ("body", pivoting at the hips, so it rears) ridged with amber crystal; its neck ("neck") and its mane of flame-hair ("mane"); its
	 * dragon's head ("head", the jaw "jaw") with one horn of amber ("horn"), whiskers and a flame-hair beard; its legs ("leg_fl", "leg_fr",
	 * "leg_bl", "leg_br", each with its "_low" and "_hoof"), cloven hooves wreathed in earth-fire; a lion's tail ("tail_0".."tail_3").
	 * It faces -z, its hooves on the ground at y 0. Stored (2026-10-24): the spell's qilin is now made of earth, {@link #qilin}; this, the
	 * true beast, is kept for later (SpellArt --stored).
	 */
	static void qilinBeast(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 31, 14);
		SpellArt.Painter skin = hide(31, 2.2, 18, 11);
		// The torso: the chest deep, the barrel, the hips; each a wide box and a tall one, so it rounds; the breast and rump bulging.
		body.box(0, 31, -12, 15, 17, 16, skin);
		body.box(0, 31.5, -12, 11, 20.4, 15, skin);
		body.box(0, 30.6, 1, 13.6, 15, 14, skin);
		body.box(0, 31, 1, 10, 17.6, 13, skin);
		body.box(0, 32, 14, 14, 15, 12, skin);
		body.box(0, 32.5, 14, 10.4, 17.2, 11, skin);
		body.box(0, 28.5, -20.6, 10, 12, 2.4, skin);
		body.box(0, 32, 21.2, 10, 12, 2.6, skin);
		// The spine's ridge of amber crystals, from the withers to the tail, leaning back.
		for (int k = 0; k < 13; k++) {
			double z = -18 + k * 3, top = z < -5 ? 41.7 : z < 8 ? 39.8 : 41.1, h = 3.6 - Math.abs(k - 3) * 0.2;
			SpellArt.limb(body, SpellArt.v3(0, top - 0.8, z), SpellArt.v3(0, top + h, z + 1.8), 1.6, 2.2, amber(true, 40 + k)).density(3);
		}
		neckAndHead(m);
		legs(m);
		tail(m);
		gallop(m);
		rear(m);
	}

	static void neckAndHead(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 37, -16);
		SpellArt.Painter scaled = limbHide(21);
		double[] from = SpellArt.v3(0, 34.5, -15), to = SpellArt.v3(0, 50, -26.5);
		SpellArt.limb(neck, from, to, 9, 11, scaled);
		SpellArt.limb(neck, from, to, 7, 13, scaled);
		// The mane: locks of flame-hair streaming back from the crest of the neck, either way.
		SpellArt.Bone mane = m.bone("mane", "neck", 0, 45, -22);
		for (int k = 0; k < 7; k++) {
			double[] at = SpellArt.add(SpellArt.add(from, SpellArt.times(SpellArt.sub(to, from), 1 - k / 6.0 * 0.92)), SpellArt.v3(0, 3.9, 5.2));
			for (int s = -1; s <= 1; s += 2) {
				SpellArt.blade(mane, SpellArt.add(at, SpellArt.v3(s * 1.4, 0, 0)), 15 - k * 0.6 - (k % 2) * 2.5, 5.4, -74 + k * 3 + (k % 2) * 7, s * (10 + (k % 2) * 10), s * (20 + (k % 3) * 9),
						maneLock(60 + k * 2 + (s > 0 ? 1 : 0))).density(2.5);
			}
		}
		SpellArt.Bone head = m.bone("head", "neck", 0, 50, -27);
		SpellArt.Painter face = headHide(31);
		head.box(0, 52.6, -31, 9, 8, 10, face);
		head.box(0, 53, -31, 7, 9.6, 9, face);
		head.box(0, 50.8, -39, 7.6, 6.2, 8.4, face);
		head.box(0, 51.6, -44.2, 6.8, 5, 2.4, nose(face));
		head.box(0, 54.3, -38.6, 4.2, 1.2, 8, face);
		for (int s = -1; s <= 1; s += 2) {
			// The brow ridge over each eye, the eye, the ear, cheek locks of flame-hair, a whisker from beside the nose.
			SpellArt.limb(head, SpellArt.v3(s * 1.8, 55.8, -36.8), SpellArt.v3(s * 4.6, 57, -31.6), 2.2, 1.8, face);
			head.box(s * 4.55, 53.7, -33.8, 0.7, 2.4, 3, Fire.eye()).density(4);
			SpellArt.blade(head, SpellArt.v3(s * 3.8, 56.6, -27.6), 6.6, 3.4, -40, s * 40, s * 50, ear()).density(3);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 4.6, 51.6 - k * 1.7, -30.6 + k), 11 - k * 1.5, 4.2, -80 - k * 12, s * (28 + k * 9), s * (24 + k * 12), maneLock(80 + k * 2 + (s > 0 ? 1 : 0))).density(2.5);
			}
			String up = "head";
			double[] at = SpellArt.v3(s * 3.6, 50.6, -44.6);
			double[][] rest = {{-8, s * 40, 0}, {6, -s * 10, 0}, {14, -s * 14, 0}};
			for (int k = 0; k < rest.length; k++) {
				SpellArt.Bone w = m.bone("whisker_" + (s > 0 ? "l" : "r") + "_" + k, up, at[0], at[1], at[2], rest[k][0], rest[k][1], rest[k][2]);
				int kk = k;
				w.box(at[0], at[1], at[2] + 2.6, 0.6 - k * 0.12, 0.6 - k * 0.12, 5.4, t -> SpellArt.glow(SpellArt.mix(GOLD_PALE, MOLTEN, (kk + t.back()) / 3), 0.97 - (kk + t.back()) * 0.14)).density(3);
				up = w.name;
				at = SpellArt.add(at, SpellArt.v3(0, 0, 5.2));
			}
		}
		// The horn: one, of amber crystal, rising from the brow and curving back, ringed at its foot.
		SpellArt.Bone horn = m.bone("horn", "head", 0, 57, -33.6);
		double[] h0 = SpellArt.v3(0, 56.4, -34), h1 = SpellArt.add(h0, SpellArt.v3(0, 4.8, 1.2)), h2 = SpellArt.add(h1, SpellArt.v3(0, 4, 3)), h3 = SpellArt.add(h2, SpellArt.v3(0, 2.4, 4));
		horn.box(0, 57.2, -33.8, 3.4, 1.2, 3.4, amber(false, 49)).rot(0, 45, 0);
		SpellArt.limb(horn, h0, h1, 2.6, 2.6, amber(false, 50)).density(3);
		SpellArt.limb(horn, h1, h2, 2, 2, amber(false, 51)).density(3);
		SpellArt.limb(horn, h2, h3, 1.3, 1.3, amber(true, 52)).density(3);
		// The jaw, a little open: its teeth, its beard of flame-hair.
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 48.6, -33.6, 8, 0, 0);
		jaw.box(0, 47.4, -38.8, 6.6, 2.4, 10.4, face);
		jaw.box(0, 48.75, -38.8, 4.2, 0.3, 9, t -> SpellArt.glow(SpellArt.mix(EMBER, MOLTEN, SpellArt.hash(t.x, t.y, 3) * 0.5), 0.9));
		for (int s = -1; s <= 1; s += 2) {
			jaw.box(s * 2.9, 49.1, -39.2, 0.4, 1.1, 9.4, Wood.teeth(false)).density(4);
			head.box(s * 3.3, 48.6, -39.4, 0.4, 1.2, 9.6, Wood.teeth(true)).density(4);
		}
		for (int k = -1; k <= 1; k++) {
			SpellArt.blade(jaw, SpellArt.v3(k * 1.4, 46.4, -42 + Math.abs(k)), 10 - Math.abs(k) * 1.6, 3.6, -150 + Math.abs(k) * 8, k * 10, k * 8, maneLock(90 + k)).density(2.5);
		}
	}

	static void legs(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			String n = names[k];
			SpellArt.Painter scaled = limbHide(200 + k);
			if (k < 2) {
				double[] shoulder = SpellArt.v3(s * 5.8, 29, -13), elbow = SpellArt.v3(s * 6, 16.5, -11.4), fetlock = SpellArt.v3(s * 5.8, 5.2, -12.6),
						toe = SpellArt.v3(s * 5.8, 2.4, -14.2);
				SpellArt.Bone leg = m.bone(n, "body", shoulder[0], shoulder[1], shoulder[2]);
				leg.box(s * 5.6, 26, -12.6, 6, 9, 8, scaled);
				SpellArt.limb(leg, shoulder, elbow, 5.4, 6.4, scaled);
				SpellArt.Bone low = m.bone(n + "_low", n, elbow[0], elbow[1], elbow[2]);
				SpellArt.limb(low, elbow, fetlock, 3.2, 3.6, scaled);
				// Earth-fire streaming back from the elbow, and wreathing the fetlock.
				for (int j = 0; j < 3; j++) {
					SpellArt.blade(low, SpellArt.add(elbow, SpellArt.v3(0, -1, 1.6)), 7 - j * 1.2, 3.6, -60 - j * 18, s * (14 + j * 12), s * (18 + j * 10), Fire.flame(2, 300 + k * 5 + j, EARTH_FIRE)).density(2.5);
				}
				SpellArt.Bone hoofBone = m.bone(n + "_hoof", n + "_low", fetlock[0], fetlock[1], fetlock[2]);
				SpellArt.limb(hoofBone, fetlock, toe, 2.6, 2.8, scaled);
				hoofBone.box(toe[0], 1.4, toe[2] - 0.4, 4.4, 2.8, 5, hoof());
				fetlockFlames(hoofBone, fetlock, s, 320 + k * 7);
			} else {
				double[] hip = SpellArt.v3(s * 5.8, 31, 15), stifle = SpellArt.v3(s * 6.4, 20, 9.4), hock = SpellArt.v3(s * 6.2, 11, 18.6), fetlock = SpellArt.v3(s * 6, 4.6, 17.2),
						toe = SpellArt.v3(s * 6, 2.2, 15.8);
				SpellArt.Bone leg = m.bone(n, "body", hip[0], hip[1], hip[2]);
				leg.box(s * 5.6, 26.4, 13.2, 6.6, 12, 10, scaled);
				SpellArt.limb(leg, hip, stifle, 6, 8, scaled);
				SpellArt.Bone low = m.bone(n + "_low", n, stifle[0], stifle[1], stifle[2]);
				SpellArt.limb(low, stifle, hock, 4.4, 5, scaled);
				for (int j = 0; j < 3; j++) {
					SpellArt.blade(low, SpellArt.add(hock, SpellArt.v3(0, 0.6, 1)), 7.4 - j * 1.2, 3.6, -58 - j * 16, s * (14 + j * 12), s * (18 + j * 10), Fire.flame(2, 340 + k * 5 + j, EARTH_FIRE)).density(2.5);
				}
				SpellArt.Bone hoofBone = m.bone(n + "_hoof", n + "_low", hock[0], hock[1], hock[2]);
				SpellArt.limb(hoofBone, hock, fetlock, 3, 3.4, scaled);
				SpellArt.limb(hoofBone, fetlock, toe, 2.6, 2.8, scaled);
				hoofBone.box(toe[0], 1.4, toe[2] - 0.4, 4.4, 2.8, 5, hoof());
				fetlockFlames(hoofBone, fetlock, s, 360 + k * 7);
			}
		}
	}

	/** A wreath of earth-fire round a fetlock, licking up and back. */
	static void fetlockFlames(SpellArt.Bone bone, double[] at, int s, int seed) {
		for (int j = 0; j < 4; j++) {
			double ang = Math.toRadians(j * 90 + 45);
			SpellArt.blade(bone, SpellArt.add(at, SpellArt.v3(Math.sin(ang) * 1.6, -1.2, Math.cos(ang) * 1.6)), 6.4, 3.4, -20 - (j > 1 ? 24 : 0), j * 90 + 45 - 90, s * 12, Fire.flame(2, seed + j, EARTH_FIRE)).density(2.5);
		}
	}

	static void tail(SpellArt.Model m) {
		String up = "body";
		double[] at = SpellArt.v3(0, 37, 21.6);
		double[][] dirs = {{0, -1.4, 6.6}, {0, -2.6, 6}, {0, -3.4, 5.4}, {0, -3.8, 4.6}};
		SpellArt.Painter scaled = limbHide(400);
		for (int k = 0; k < dirs.length; k++) {
			SpellArt.Bone b = m.bone("tail_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, dirs[k]);
			SpellArt.limb(b, at, next, 3.6 - k * 0.6, 3.6 - k * 0.6, scaled);
			if (k == dirs.length - 1) {
				// The tuft: a fan of flame-hair.
				for (int j = 0; j < 7; j++) {
					double fan = (j - 3) * 16;
					SpellArt.blade(b, next, 13 - Math.abs(j - 3), 5, -120 + Math.abs(j - 3) * 6, fan, fan * 0.6, maneLock(420 + j)).density(2.5);
				}
			}
			up = b.name;
			at = next;
		}
	}

	/**
	 * The gallop (the user's ask, 2026-10-25: "look at how ... horses actually gallop"): a horse's transverse gallop, four beats and then the
	 * legs gathered in the air. The left hind lands, then the right hind, then the left fore, then the right fore (the leading fore), each on
	 * the ground a third of the stride; then all four are off it, gathered under the body, before the hind lands again. A leg on the ground
	 * sweeps back under the body as it goes on over it; one in the air swings forward: a fore's knee folding tight, its hoof flicked up under it,
	 * then reaching out straight ahead to land; a hind's hock flexing as it swings under the belly, then reaching to land under the hips. The
	 * body rocks as a rocking horse does, its forehand thrown up as the hinds drive and sinking as the fores take it, highest in the air; the
	 * neck and head nod down once a stride as the fores land, holding the gaze steady; the mane and tail stream back, rippling.
	 */
	static void gallop(SpellArt.Model m) {
		double T = 0.9;
		SpellArt.Anim a = m.anim("gallop", T, "true");
		int steps = 36;
		String[] legs = {"leg_bl", "leg_br", "leg_fl", "leg_fr"};
		// When each hoof lands (of the stride), and how long it is down: the hinds left then right, the fores left then right, then the air.
		double[] down = {0.00, 0.10, 0.30, 0.40};
		double duty = 0.32;
		// Each joint round its own stride (SpellArt#keyed: from its hoof landing, through its stance to duty, through its swing): the upper leg
		// (less than nought: forward), the knee or hock, the fetlock and hoof.
		double[][] foreUpper = {{0, -38}, {duty * 0.5, -4}, {duty, 32}, {duty + 0.12, 40}, {duty + 0.4, 4}, {duty + 0.58, -44}, {0.94, -42}};
		double[][] foreKnee = {{0, 2}, {duty * 0.5, 6}, {duty, 10}, {duty + 0.12, 70}, {duty + 0.28, 96}, {duty + 0.46, 50}, {duty + 0.58, -4}, {0.94, -2}};
		double[][] foreHoof = {{0, 0}, {duty * 0.5, -10}, {duty, 18}, {duty + 0.12, 44}, {duty + 0.3, 40}, {duty + 0.5, 6}, {0.94, 0}};
		double[][] hindUpper = {{0, -28}, {duty * 0.5, 6}, {duty, 40}, {duty + 0.14, 34}, {duty + 0.4, -12}, {duty + 0.56, -32}, {0.94, -30}};
		double[][] hindHock = {{0, -4}, {duty * 0.45, -16}, {duty, 6}, {duty + 0.12, -40}, {duty + 0.28, -66}, {duty + 0.46, -34}, {duty + 0.58, -6}, {0.94, -4}};
		double[][] hindHoof = {{0, 0}, {duty * 0.5, -10}, {duty, 26}, {duty + 0.14, 48}, {duty + 0.34, 30}, {duty + 0.52, 4}, {0.94, 0}};
		for (int k = 0; k < 4; k++) {
			double at = down[k];
			boolean front = k >= 2;
			double[][] upper = front ? foreUpper : hindUpper, knee = front ? foreKnee : hindHock, hoof = front ? foreHoof : hindHoof;
			a.sample(legs[k], "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), upper), 0, 0});
			a.sample(legs[k] + "_low", "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), knee), 0, 0});
			a.sample(legs[k] + "_hoof", "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), hoof), 0, 0});
		}
		// Highest gathered in the air, lowest as the fores take it; the forehand thrown up as the hinds drive, sinking as the fores land.
		a.sample("root", "position", 0, T, steps, t -> new double[] {0, 3.2 * Math.cos(2 * Math.PI * (t / T - 0.86)), 0});
		a.sample("body", "rotation", 0, T, steps, t -> new double[] {6 * Math.sin(2 * Math.PI * (t / T - 0.22)), 0, 1.2 * Math.sin(2 * Math.PI * t / T)});
		// The neck and head nodding down once a stride as the fores land, holding the gaze steady against the body's rocking.
		a.sample("neck", "rotation", 0, T, steps, t -> new double[] {-3 * Math.sin(2 * Math.PI * (t / T - 0.22)) + 8 * Math.sin(2 * Math.PI * (t / T - 0.32)), 0, 0});
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {-4 * Math.sin(2 * Math.PI * (t / T - 0.36)), 1.5 * Math.sin(2 * Math.PI * t / T), 0});
		a.sample("jaw", "rotation", 0, T, 12, t -> new double[] {5 + 4 * Math.sin(2 * Math.PI * (t / T - 0.5)), 0, 0});
		a.sample("mane", "rotation", 0, T, steps, t -> new double[] {-10 - 6 * Math.sin(2 * Math.PI * (t / T - 0.45)), 0, 0});
		for (int k = 0; k < 4; k++) {
			int kk = k;
			a.sample("tail_" + k, "rotation", 0, T, steps, t -> new double[] {(kk == 0 ? 14 : -2) + (5 + 3 * kk) * Math.sin(2 * Math.PI * (t / T - 0.5 - 0.12 * kk)),
					(3 + 2 * kk) * Math.sin(2 * Math.PI * (t / T - 0.2 - 0.1 * kk)), 0});
		}
		for (String side : new String[] {"l", "r"}) {
			for (int k = 0; k < 3; k++) {
				int kk = k;
				a.sample("whisker_" + side + "_" + k, "rotation", 0, T, steps, t -> new double[] {-6 + (5 + kk * 3) * Math.sin(2 * Math.PI * (t / T - 0.4 - kk * 0.14)), 0, 0});
			}
		}
	}

	/** The rearing: it rises on its hind legs, forehooves pawing the air, its head flung up, and slams down (the stamp); then it settles. */
	static void rear(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("rear", 1.3, null);
		double[] t = {0, 0.2, 0.4, 0.55, 0.7, 0.82, 0.92, 1.05, 1.3};
		double[] up = {0, 0.6, 1, 1, 1, 0.5, -0.12, 0.04, 0};
		double[] paw = {0, 0.3, 1, -1, 1, 0.2, 0, 0, 0};
		for (int i = 0; i < t.length; i++) {
			double r = up[i], lift = Math.max(0, r);
			a.rot("body", t[i], -38 * r, 0, 0);
			// The hind legs kept under it; the fore legs pawing.
			a.rot("leg_bl", t[i], 30 * lift, 0, 0);
			a.rot("leg_br", t[i], 30 * lift, 0, 0);
			a.rot("leg_bl_low", t[i], -8 * lift, 0, 0);
			a.rot("leg_br_low", t[i], -8 * lift, 0, 0);
			a.rot("leg_fl", t[i], -30 * lift + 18 * paw[i], 0, 0);
			a.rot("leg_fr", t[i], -30 * lift - 18 * paw[i], 0, 0);
			a.rot("leg_fl_low", t[i], 70 * lift + 20 * paw[i], 0, 0);
			a.rot("leg_fr_low", t[i], 70 * lift - 20 * paw[i], 0, 0);
			a.rot("neck", t[i], 12 * lift, 0, 0);
			a.rot("head", t[i], -20 * lift, 0, 0);
			a.rot("jaw", t[i], 4 + 26 * lift, 0, 0);
			a.rot("mane", t[i], 10 * lift, 0, 0);
			a.rot("tail_0", t[i], -14 + 20 * lift, 0, 0);
		}
	}

	// --- The Seal of Ten Thousand Mountains: painters ---

	/** A mountain ridgeline across [0, 1] (its height in [0, 1]): sharp peaks, ridged noise of two octaves. */
	static double ridge(double x, int seed) {
		double a = 1 - Math.abs(SpellArt.noise(x, 0.5, seed) * 2 - 1), b = 1 - Math.abs(SpellArt.noise(x * 2.3, 1.5, seed + 1) * 2 - 1);
		return a * 0.75 + b * 0.25;
	}

	/**
	 * The seal's sides, yellow jade: a range of mountains carved in relief (two ridges, the far darker, their crests cut dark and lit gold)
	 * over a band of cloud scrolls, between borders of inlaid gold; its top smooth jade, mottled, within a carved border inlaid with gold.
	 */
	static SpellArt.Painter sealBody(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.05 + seed, (t.py + t.pz) * 0.05 + t.px * 0.02, seed, 3);
			int jade = SpellArt.mix(JADE_Y_DARK, JADE_Y_LIGHT, 0.25 + 0.7 * n);
			if (Math.abs(n - 0.52) < 0.012) jade = warm(jade, -0.18);
			if (t.f == SpellArt.Face.UP) {
				double d = Math.max(Math.abs(t.across() - 0.5), Math.abs(t.back() - 0.5));
				if (Math.abs(d - 0.44) < 0.008) return SpellArt.glow(MOLTEN, 0.95);
				if (Math.abs(d - 0.425) < 0.005) return SpellArt.col(warm(jade, -0.35));
				return SpellArt.col(jade);
			}
			if (t.f == SpellArt.Face.DOWN) return SpellArt.col(warm(jade, -0.2));
			double a = t.side() ? t.back() : t.across(), up = t.up();
			if (up > 0.95 || up < 0.04) return SpellArt.col(warm(jade, -0.08));
			if (Math.abs(up - 0.93) < 0.011 || Math.abs(up - 0.065) < 0.011 || Math.abs(up - 0.33) < 0.009) return SpellArt.glow(MOLTEN, 0.95);
			if (up < 0.33) {
				// Cloud scrolls: a row of spirals, their grooves cut dark, a thread of gold at each heart.
				double cx = a * 7, cy = (up - 0.065) / 0.265;
				double ux = cx - Math.floor(cx) - 0.5, uy = cy - 0.5, r = Math.hypot(ux, uy * 0.9);
				if (r < 0.06) return SpellArt.glow(MOLTEN, 0.92);
				if (r < 0.46) {
					double th = Math.atan2(uy, ux) / (2 * Math.PI) + 0.5, s = r / 0.11 - th;
					if (s - Math.floor(s) < 0.22) return SpellArt.col(warm(jade, -0.42));
					return SpellArt.col(warm(jade, 0.12));
				}
				return SpellArt.col(warm(jade, -0.12));
			}
			// The mountains: a near ridge over a far one, carved in relief; the sky above them plain jade.
			double h = (up - 0.33) / 0.6;
			double far = 0.5 + 0.42 * ridge(a * 3.2 + seed, seed), near = 0.2 + 0.42 * ridge(a * 4.7 + seed + 9, seed + 1);
			if (Math.abs(h - near) < 0.018) return SpellArt.glow(SpellArt.mix(MOLTEN, GOLD_PALE, 0.3), 0.9);
			if (Math.abs(h - near) < 0.04 && h > near) return SpellArt.col(warm(jade, -0.4));
			if (h < near) {
				// The near slopes, lit; streaks of their crags down them.
				double streak = Math.abs(Math.sin(a * 90 + h * 14 + seed));
				return SpellArt.col(warm(jade, 0.14 - 0.14 * streak * (1 - h / near)));
			}
			if (Math.abs(h - far) < 0.014) return SpellArt.col(warm(jade, -0.38));
			if (h < far) return SpellArt.col(warm(jade, -0.2));
			return SpellArt.col(jade);
		};
	}

	/** Lattice strokes for a character of seal script: on a five-by-five lattice, the row and column steps between its points. */
	static final int LATTICE = 5;

	/** Whether ({@code x}, {@code y}) in [0, 1] lies on a stroke of character {@code ch}: a frame, then rows, columns and a few slants. */
	static boolean character(int ch, double x, double y) {
		double w = 0.05;
		if (x < 0 || x > 1 || y < 0 || y > 1) return false;
		double step = 1.0 / (LATTICE - 1);
		for (int i = 0; i < LATTICE; i++) {
			for (int j = 0; j < LATTICE; j++) {
				double px = i * step, py = j * step;
				boolean edge = i == 0 || j == 0 || i == LATTICE - 1 || j == LATTICE - 1;
				if (i < LATTICE - 1 && SpellArt.hash(ch * 97 + i * 7 + j, 1, 31) < (edge && j % (LATTICE - 1) == 0 ? 0.75 : 0.42)
						&& Fire.segment(x, y, px, py, px + step, py) < w) return true;
				if (j < LATTICE - 1 && SpellArt.hash(ch * 97 + i * 7 + j, 2, 31) < (edge && i % (LATTICE - 1) == 0 ? 0.75 : 0.42)
						&& Fire.segment(x, y, px, py, px, py + step) < w) return true;
				if (i < LATTICE - 1 && j < LATTICE - 1 && SpellArt.hash(ch * 97 + i * 7 + j, 3, 31) < 0.08
						&& Fire.segment(x, y, px, py, px + step, py + step) < w) return true;
			}
		}
		return false;
	}

	/**
	 * The seal's face, as it lies at ({@code x}, {@code z}) in [0, 1]: 0 nothing, 1 its frame, 2 a stroke of one of its four characters
	 * (a square frame round four great characters, each in its quarter).
	 */
	static int faceMark(double x, double z, int seed) {
		double d = Math.max(Math.abs(x - 0.5), Math.abs(z - 0.5));
		if (d > 0.42 && d < 0.47) return 1;
		if (d >= 0.42) return 0;
		double gx = (x - 0.1) / 0.8 * 2, gz = (z - 0.1) / 0.8 * 2;
		int cx = (int) Math.floor(gx), cz = (int) Math.floor(gz);
		if (cx < 0 || cz < 0 || cx > 1 || cz > 1) return 0;
		return character(seed + cx + cz * 2, (gx - cx - 0.12) / 0.76, (gz - cz - 0.12) / 0.76) ? 2 : 0;
	}

	/** The seal's foot: its face pressed in vermilion paste, its characters standing proud of it, molten gold; its edge a band of gold. */
	static SpellArt.Painter sealFace(int seed) {
		return t -> {
			if (t.f != SpellArt.Face.DOWN) return t.flat() ? SpellArt.col(JADE_Y_DARK) : SpellArt.glow(SpellArt.mix(MOLTEN, GOLD_PALE, t.up() * 0.5), 0.95);
			int mark = faceMark(t.across(), t.back(), seed);
			if (mark > 0) return SpellArt.glow(mark == 1 ? MOLTEN : SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.35), 0.97);
			return SpellArt.col(SpellArt.mix(PASTE_DARK, PASTE, 0.4 + 0.5 * SpellArt.noise(t.across() * 30, t.back() * 30, seed)));
		};
	}

	/**
	 * The seal's imprint spreading over the ground round its foot (a sheet {@code scale} times the seal's width): square rings of molten
	 * gold, and between them a ring of characters of seal script, six to a side; seen through, glowing; nothing else (under the seal).
	 */
	static SpellArt.Painter imprint(int seed, double scale) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.max(Math.abs(x), Math.abs(z)), inner = 1 / scale;
			// Fractions of the way from the seal's edge to the sheet's.
			double f = (r - inner) / (1 - inner);
			if (f < 0.04 || f > 1) return SpellArt.CLEAR;
			if (Math.abs(f - 0.1) < 0.018 || Math.abs(f - 0.9) < 0.018) return SpellArt.glow(SpellArt.mix(EMBER, MOLTEN, 0.5), 0.85);
			if (f < 0.2 || f > 0.8) return SpellArt.CLEAR;
			// Along the ring: which side, and how far along it (the corners' cells shared out).
			double along = Math.abs(x) >= Math.abs(z) ? (z / r + 1) / 2 : (x / r + 1) / 2;
			int sideIndex = Math.abs(x) >= Math.abs(z) ? (x > 0 ? 0 : 1) : (z > 0 ? 2 : 3);
			double cells = 6, cf = along * cells;
			int cell = (int) Math.min(cells - 1, Math.floor(cf));
			double cx = cf - cell, cy = (f - 0.2) / 0.6;
			if (!character(seed * 31 + sideIndex * 7 + cell, (cx - 0.14) / 0.72, (cy - 0.14) / 0.72)) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(MOLTEN, MOLTEN_CORE, 0.3), 0.9);
		};
	}

	/** The mountains' stone: grey-brown cliffs streaked down their faces, mossed on their ledges (where they face up), a thread of gold now and then. */
	static SpellArt.Painter rock(int seed) {
		return t -> {
			double streak = SpellArt.fbm((t.px + t.pz) * 0.35 + seed, t.py * 0.05, seed, 3);
			int c = SpellArt.mix(STONE_DARK, STONE_LIGHT, streak * 1.3 - 0.15);
			if (Math.abs(streak - 0.5) < 0.025) c = SpellArt.shade(c, -0.3);
			if (t.ny > 0.5) c = SpellArt.mix(c, MOSS, 0.6 + 0.3 * SpellArt.noise(t.px * 0.4, t.pz * 0.4, seed + 3));
			else if (t.up() > 0.8 && SpellArt.noise(t.px * 0.3 + t.pz * 0.3, t.py * 0.6, seed + 5) > 0.55) c = SpellArt.mix(c, MOSS, 0.45);
			if (Math.abs(SpellArt.noise((t.px + t.pz) * 0.08, t.py * 0.08, seed + 7) - 0.5) < 0.006) return SpellArt.glow(MOLTEN, 0.92);
			return SpellArt.col(c);
		};
	}

	/** A pine's pad of needles, dark beneath and lit on top. */
	static SpellArt.Painter needles(int seed) {
		return t -> {
			double n = SpellArt.hash((int) Math.floor(t.px * 3), (int) Math.floor(t.pz * 3 + t.py * 3), seed);
			int c = t.ny > 0.5 ? SpellArt.mix(PINE, PINE_LIGHT, 0.4 + n * 0.6) : SpellArt.mix(SpellArt.shade(PINE, -0.3), PINE, n);
			return SpellArt.col(c);
		};
	}

	/** A sheet of mist: soft white clouds, thinning to nothing at their edges and the sheet's. */
	static SpellArt.Painter mist(int seed) {
		return t -> {
			double x = t.across() - 0.5, z = t.back() - 0.5, r = Math.hypot(x, z) * 2;
			double c = SpellArt.fbm(t.across() * 5 + seed, t.back() * 5, seed, 3) - 0.35 - r * 0.35;
			if (c < 0.06) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(0xD8D4C8, MIST, c * 2), Math.min(0.7, c * 2.4));
		};
	}

	/** Red silk cord, twisted; the tassel's threads paler at their ends. */
	static SpellArt.Painter cord(boolean tassel) {
		return t -> {
			if (tassel) {
				double v = t.v(), u = t.u() - 0.5;
				if (Math.abs(u) > 0.42 * (0.6 + 0.4 * v)) return SpellArt.CLEAR;
				double strand = Math.abs(Math.sin(u * 40 + v * 2));
				return SpellArt.col(SpellArt.mix(CORD_DARK, SpellArt.mix(CORD, 0xE85050, v * 0.5), 0.5 + 0.5 * strand));
			}
			double twist = (t.ly * 1.4 + t.lx + t.lz) % 1.0;
			return SpellArt.col(twist < 0.5 ? CORD : CORD_DARK);
		};
	}

	// --- The Seal of Ten Thousand Mountains ---

	static final double SEAL = 112, SEAL_TALL = 40;
	/** The peaks on the seal: x, z, height, width at the foot. */
	static final double[][] PEAKS = {{0, 0, 82, 24}, {-24, -16, 54, 17}, {22, -20, 62, 18}, {-20, 22, 46, 15}, {26, 18, 40, 14}, {4, -34, 34, 12},
			{-36, 4, 30, 11}, {36, -4, 36, 12}, {-2, 32, 30, 11}, {-36, -34, 20, 9}, {34, 36, 18, 8}, {-12, -2, 40, 12}, {12, 8, 46, 13}};
	/** A peak's tiers: how much of its height each is. */
	static final double[] TIERS = {0.22, 0.19, 0.17, 0.15, 0.14, 0.13};

	/**
	 * The Seal of Ten Thousand Mountains: a block of yellow jade twenty-eight blocks square ("seal"), its sides carved with mountains over
	 * clouds and inlaid with gold, its face (beneath) pressed in vermilion and carved with four characters of seal script; on it, mountains
	 * for its knob, eleven peaks of mossed stone veined with gold, pines clinging to them, mist among them; a red cord and tassel hanging
	 * from it ("cord_0".."cord_2"); and its imprint ("imprint"), seen only as it stamps, spreading over the ground. Its face at y 0.
	 */
	static void mountainSeal(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone seal = m.bone("seal", "root", 0, 0, 0);
		SpellArt.Painter jade = sealBody(5);
		seal.box(0, 1.5, 0, SEAL, 3, SEAL, sealFace(7)).density(1.25);
		seal.box(0, 3 + SEAL_TALL / 2, 0, SEAL, SEAL_TALL, SEAL, jade).skip(SpellArt.Face.DOWN);
		seal.box(0, SEAL_TALL + 4, 0, SEAL - 8, 2, SEAL - 8, jade).skip(SpellArt.Face.DOWN);
		seal.box(0, SEAL_TALL + 5.6, 0, SEAL - 16, 1.2, SEAL - 16, jade).skip(SpellArt.Face.DOWN);
		double top = SEAL_TALL + 6.2;
		// The mountains: each peak a pillar of crags stacked in tiers, narrowing, each leaning a little and turned its own way, a point on top.
		for (int p = 0; p < PEAKS.length; p++) {
			double[] pk = PEAKS[p];
			double y = top - 1, x = pk[0], z = pk[1];
			for (int i = 0; i < TIERS.length; i++) {
				double hh = pk[2] * TIERS[i], w = pk[3] * (1 - i * 0.13);
				double h1 = SpellArt.hash(p, i, 3) - 0.5, h2 = SpellArt.hash(p, i, 4) - 0.5, h3 = SpellArt.hash(p, i, 5);
				seal.box(x, y + hh / 2, z, w, hh + 1, w * (0.8 + 0.3 * h3), rock(60 + p)).rot(h1 * 6, h3 * 90, h2 * 6).skip(SpellArt.Face.DOWN);
				y += hh;
				x += h1 * w * 0.1;
				z += h2 * w * 0.1;
			}
			double cap = pk[3] * 0.5;
			seal.box(x, y + cap * 0.2, z, cap, cap, cap, rock(60 + p)).rot(45, 30 + p * 17, 35);
			// A pine clinging near the top of the taller peaks.
			if (pk[2] >= 32) {
				double side = SpellArt.hash(p, 9, 6) < 0.5 ? 1 : -1, py = top + pk[2] * 0.62;
				double[] foot = SpellArt.v3(pk[0] + side * pk[3] * 0.32, py, pk[1]), crown = SpellArt.add(foot, SpellArt.v3(side * 4.6, 3.4, -0.6));
				SpellArt.limb(seal, foot, crown, 1.1, 1.1, Wood.bark(70 + p, 0)).density(2);
				seal.box(crown[0], crown[1] + 0.5, crown[2], 7, 1.6, 5.4, needles(80 + p)).rot(0, 20 * side, -8 * side).density(2);
				seal.box(crown[0] - side * 2.2, crown[1] - 1.6, crown[2] + 0.6, 5.4, 1.4, 4.2, needles(90 + p)).rot(0, -15 * side, 6 * side).density(2);
			}
		}
		// Mist among the peaks.
		SpellArt.sheet(seal, 0, top + 14, 0, SEAL * 0.9, SEAL * 0.9, 0, 30, 0, mist(11)).density(0.75);
		SpellArt.sheet(seal, 6, top + 30, -4, SEAL * 0.6, SEAL * 0.6, 0, -20, 0, mist(12)).density(0.75);
		// The cord, tied through a ring at the front edge, hanging down its face; its tassel.
		seal.box(-40, SEAL_TALL + 3.6, -SEAL / 2 - 0.6, 3.4, 3.4, 1.2, t -> SpellArt.glow(SpellArt.mix(MOLTEN, GOLD_PALE, t.fy * 0.5), 0.95)).density(2);
		String up = "seal";
		double[] at = SpellArt.v3(-40, SEAL_TALL + 2.4, -SEAL / 2 - 1.3);
		for (int k = 0; k < 3; k++) {
			SpellArt.Bone c = m.bone("cord_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, SpellArt.v3(0, -7.5, -0.2));
			SpellArt.limb(c, at, next, 1, 1, cord(false)).density(3);
			if (k == 2) {
				c.box(next[0], next[1] - 0.6, next[2], 2.2, 2.2, 2.2, cord(false)).density(3);
				for (int j = 0; j < 4; j++) {
					SpellArt.blade(c, SpellArt.add(next, SpellArt.v3(0, -1, 0)), 9, 3.6, 180, j * 45, 0, cord(true)).density(3);
				}
			}
			up = c.name;
			at = next;
		}
		// Its imprint on the ground, under its face.
		SpellArt.Bone print = m.bone("imprint", "root", 0, 0, 0);
		SpellArt.sheet(print, 0, 0.35, 0, SEAL * 2.2, SEAL * 2.2, 0, 0, 0, imprint(7, 2.2)).density(0.75);
		sealFall(m);
		stamp(m);
	}

	/** The fall (and its hanging after): the seal swaying a little, its cord swinging; its imprint not yet made. */
	static void sealFall(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("fall", T, "true");
		a.sample("seal", "rotation", 0, T, 12, t -> new double[] {1.2 * Math.sin(2 * Math.PI * t / T), 0, 1.2 * Math.cos(2 * Math.PI * t / T)});
		for (int k = 0; k < 3; k++) {
			int kk = k;
			a.sample("cord_" + k, "rotation", 0, T, 12, t -> new double[] {(3 + kk * 2) * Math.sin(2 * Math.PI * t / T - kk * 0.8), 0, (2 + kk) * Math.cos(2 * Math.PI * t / T - kk * 0.7)});
		}
		a.scale("imprint", 0, 0.001);
		a.scale("imprint", T, 0.001);
	}

	/** The stamp: the seal slams down and squats a little under its own weight; its imprint flares out over the ground and sinks into it. Held: it stands still after. */
	static void stamp(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("stamp", 1.4, "hold_on_last_frame");
		double[] t = {0, 0.06, 0.18, 0.35, 0.6, 1.0, 1.2, 1.4};
		double[] squat = {1, 0.9, 1.04, 1, 1, 1, 1, 1};
		double[] spread = {0.001, 0.55, 0.72, 0.88, 1, 1.12, 1.16, 0.001};
		double[] sink = {0, 0, 0, 0, 0, 0, -2, -3};
		for (int i = 0; i < t.length; i++) {
			a.scale("seal", t[i], 1 + (1 - squat[i]) * 0.3, squat[i], 1 + (1 - squat[i]) * 0.3);
			a.scale("imprint", t[i], spread[i]);
			a.pos("imprint", t[i], 0, sink[i], 0);
			a.rot("cord_0", t[i], i == 1 ? -30 : i == 2 ? 20 : 0, 0, 0);
		}
	}
}
