/**
 * The fire element's Dao Integration spells for SpellArt: the Vermilion Bird of the south (Vermilion Bird Descends), and One Flame Burns
 * the Heavens: its seed (a lotus bud of flame round a pearl of white fire, ringed by talisman script) and the pillar the seed blooms into.
 */
public class Fire {
	// --- The fire element's colours: embers, crimson, vermilion, flame, amber, gold, the white heart of the fire; horn and talon ---
	static final int EMBER = 0x621109, CRIMSON = 0xA6190F, VERMILION = 0xE23C1B, FLAME = 0xFF7627, AMBER = 0xFFA234, GOLD = 0xFFC93F,
			GOLD_LIGHT = 0xFFE78F, WHITE_FIRE = 0xFFF9E5;
	/** Below, a warm amber (not gold: gold shaded darker beneath turns olive). */
	static final int RUDDY = 0xC02A14, BELLY = 0xFFB04A, HORN = 0xECB54C, HORN_DARK = 0x8F4D15, TALON = 0x3B1A0C, SOCKET = 0x2A0806;

	// --- Painters ---

	/**
	 * A feather on a blade (standing or flat): a rounded vane, lighter along its shaft (a thread of light), fine barbs slanting out toward
	 * its tip, {@code root} at its root through {@code mid} to {@code tip}, paler beneath (a flat blade's underside); if it {@code burn}s, its
	 * end catches fire and flames lick off its tip (that much of its length). {@code fat} widens it; {@code asym} narrows its outer vane (a
	 * primary's, toward its +x).
	 */
	static SpellArt.Painter feather(int root, int mid, int tip, double fat, double burn, double asym, int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double len = t.cube.flat ? t.sz() : t.sy(), wide = t.cube.flat ? t.sx() : t.sz();
			// The feather itself, and past it its flames.
			double body = 1 - 0.55 * burn, vv = v / body;
			double half = 0.5 * Math.min(1, fat) * (0.35 + 0.65 * Math.min(1, vv * 5));
			if (vv > 0.68) half *= Math.sqrt(Math.max(0, 1 - Math.pow((vv - 0.68) / 0.32, 2)));
			if (u > 0) half *= 1 - asym;
			half *= 1 - 0.05 * SpellArt.noise(v * len * 0.45 + seed, u > 0 ? 1 : 5, seed);
			if (au <= half && vv <= 1) {
				if (au * wide < 0.3 + 0.25 * (1 - vv) && vv > 0.05) return SpellArt.glow(SpellArt.mix(GOLD, WHITE_FIRE, vv), 0.92);
				double n = SpellArt.fbm(u * 5 + seed, v * 7, seed, 2);
				int c = SpellArt.ramp(vv + (n - 0.5) * 0.12, root, mid, tip);
				c = SpellArt.shade(c, 0.08 - 0.22 * au / Math.max(0.05, half));
				double barb = (vv * len * body - au * wide * 1.1) / 1.5 + seed * 0.37;
				if (barb - Math.floor(barb) < 0.34) c = SpellArt.mix(c, root, 0.2);
				if (t.flat() && t.ny < -0.3) c = SpellArt.mix(c, FLAME, 0.3);
				double fire = burn <= 0 ? 0 : SpellArt.smooth(1 - burn * 1.2, 1, vv);
				if (fire > 0.03) return SpellArt.glow(SpellArt.mix(c, SpellArt.mix(FLAME, GOLD_LIGHT, fire), fire), 0.97 - 0.25 * fire);
				if ((half - au) * wide < 0.4) return SpellArt.col(SpellArt.mix(c, root, 0.35));
				return SpellArt.col(c);
			}
			if (burn <= 0 || v < body * 0.88) return SpellArt.CLEAR;
			// Flames licking off its tip: two or three tongues, wavering, thinning to their points.
			int tongues = wide > 5.5 ? 3 : 2;
			for (int i = 0; i < tongues; i++) {
				double from = body * 0.88, reach = 1 - 0.14 * SpellArt.hash(i, seed, 3), up = (v - from) / (reach - from);
				if (up > 1) continue;
				double cx = ((i + 0.5) / tongues - 0.5) * 0.6 * Math.min(1, fat) + 0.06 * Math.sin(up * 5 + i + seed);
				double w = (tongues == 2 ? 0.2 : 0.16) * Math.min(1, fat) * Math.pow(1 - up, 0.7);
				if (Math.abs(u - cx) < w) {
					double heat = (1 - up) * (1 - Math.abs(u - cx) / w);
					return SpellArt.glow(SpellArt.ramp(heat, VERMILION, FLAME, GOLD, GOLD_LIGHT), 0.35 + 0.55 * heat);
				}
			}
			return SpellArt.CLEAR;
		};
	}

	/**
	 * Rows of rounded feathers overlapping toward increasing {@code row} (their tips that way): {@code a} 0 on the back to 1 beneath, from
	 * ruddy vermilion down the back through flame on the flanks to amber and gold under the breast; each tip lit, gold fire on most.
	 */
	static int feathered(double a, double col, double row, int seed) {
		int r = (int) Math.floor(row);
		double v = row - r, colF = col + Math.floorMod(r, 2) * 0.5;
		int ci = (int) Math.floor(colF);
		double u = colF - ci, rim = 0.4 + 0.6 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
		double h = SpellArt.hash(ci, r, seed) - 0.5;
		int base = SpellArt.ramp(a + h * 0.08, RUDDY, VERMILION, VERMILION, FLAME, AMBER, BELLY);
		if (v > rim) return SpellArt.col(SpellArt.shade(base, -0.38));
		if (rim - v < 0.15) return a > 0.18 && h > -0.2 ? SpellArt.glow(SpellArt.mix(base, GOLD_LIGHT, 0.5), 0.86) : SpellArt.col(SpellArt.shade(base, 0.22));
		if (Math.abs(u - 0.5) < 0.07 && v > 0.3) return SpellArt.col(SpellArt.shade(base, 0.14));
		return SpellArt.col(SpellArt.shade(base, -0.2 + 0.32 * v / rim + h * 0.12));
	}

	/** The body's plumage round an axis along z (x = 0, y = {@code axisY}): {@link #feathered}, the tips toward the tail. */
	static SpellArt.Painter plumage(double axisY, double row, int columns, int seed) {
		return t -> {
			double th = Math.atan2(t.qx(), t.qy() - axisY), a = Math.abs(th) / Math.PI;
			if (t.flat() && t.ny < -0.6) a = 1;
			if (t.flat() && t.ny > 0.6) a = Math.min(a, 0.15);
			return feathered(a, th / (2 * Math.PI) * columns, t.qz() / row, seed);
		};
	}

	/**
	 * {@link #feathered} round a limb's own axis (its cube's y, through its middle), coloured by which way a face looks; the tips toward the
	 * limb's far end, or its foot if {@code back}.
	 */
	static SpellArt.Painter plumageLimb(double row, int columns, boolean back, int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2);
			return feathered(SpellArt.clamp01(0.5 - t.ny * 0.55), ang / (2 * Math.PI) * columns, (back ? t.sy() - t.ly : t.ly) / row, seed);
		};
	}

	/** The head's small feathers ({@link #feathered}, fine), a streak of gold and crimson fire back from each eye. */
	static SpellArt.Painter headPlumage(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - 10, qz = t.qz();
			if (Math.abs(qx) > 3.4 && Math.abs(qy - 0.9 + (qz + 25) * 0.18) < 0.7 && qz > -25.2 && qz < -19.8)
				return SpellArt.glow(SpellArt.mix(GOLD, CRIMSON, (qz + 25.2) / 5.4), 0.95);
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.flat() && t.ny < -0.6) a = 1;
			return feathered(a * 0.9, th / (2 * Math.PI) * 15, qz / 1.15, seed);
		};
	}

	/** The beak's horn, from its root at z {@code rootZ} to its tip at {@code tipZ}: gold darkening to the root, the tip white fire. */
	static SpellArt.Painter beak(double rootZ, double tipZ) {
		return t -> {
			double along = SpellArt.clamp01((t.qz() - rootZ) / (tipZ - rootZ));
			if (along > 0.86) return SpellArt.glow(SpellArt.mix(HORN, WHITE_FIRE, (along - 0.86) * 6), 0.97);
			int c = SpellArt.mix(HORN_DARK, HORN, 0.3 + 0.75 * along);
			if (t.side() && t.up() < 0.16) c = SpellArt.shade(c, -0.4);
			return SpellArt.col(SpellArt.shade(c, (SpellArt.noise(t.qx() * 2, t.qz() * 1.5, 3) - 0.5) * 0.15));
		};
	}

	/** The bird's eye on the outer side of its cube: white fire ringed amber, a slit of black, a glint; dark round it. */
	static SpellArt.Painter eye() {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return SpellArt.col(SOCKET);
			double x = t.back() - 0.5, y = t.up() - 0.5, r = Math.hypot(x, y * 1.2);
			if (r > 0.46) return SpellArt.col(SOCKET);
			if (Math.abs(x) < 0.06 && Math.abs(y) < 0.3) return SpellArt.col(0x140402);
			if (Math.hypot(x + 0.18, y - 0.18) < 0.08) return SpellArt.glow(0xFFFFFF);
			return SpellArt.glow(SpellArt.mix(WHITE_FIRE, AMBER, r * 2.1));
		};
	}

	/** A shank's scales: rings of gold horn. */
	static SpellArt.Painter shank() {
		return t -> {
			double ring = t.ly / 0.7;
			int c = SpellArt.mix(HORN_DARK, HORN, 0.45 + 0.4 * SpellArt.hash((int) Math.floor(ring), (int) Math.floor(t.lx + t.lz), 9));
			if (ring - Math.floor(ring) < 0.22) c = SpellArt.shade(c, -0.35);
			return SpellArt.col(c);
		};
	}

	/** A talon: dark horn, its point (the top of its cube) glowing gold. */
	static SpellArt.Painter talon() {
		return t -> {
			double toward = t.up();
			if (toward > 0.7) return SpellArt.glow(SpellArt.mix(GOLD, WHITE_FIRE, (toward - 0.7) * 3));
			return SpellArt.col(SpellArt.mix(TALON, SpellArt.shade(HORN_DARK, -0.2), toward));
		};
	}

	/**
	 * A section of a streamer (a blade {@code width} pixels across) from {@code v0} to {@code v1} of the whole ({@code total} pixels long),
	 * proportioned to an eye {@code eyeWidth} across: a narrow rippling vane of crimson and vermilion round a shaft of light; at its end, an
	 * eye of fire (a phoenix's), ringed gold, crimson and gold round a white heart, flames licking from its rim.
	 */
	static SpellArt.Painter streamer(double width, double eyeWidth, double total, double v0, double v1, int seed) {
		return t -> {
			double V = v0 + (v1 - v0) * t.v(), x = (t.u() - 0.5) * width, ax = Math.abs(x);
			double along = (V - 0.86) * total, re = Math.hypot(along / (0.12 * total), ax / (0.4 * eyeWidth));
			if (re < 1) {
				if (re > 0.84) return SpellArt.glow(SpellArt.mix(GOLD, GOLD_LIGHT, (re - 0.84) * 5), 0.95);
				if (re > 0.62) return SpellArt.col(SpellArt.mix(CRIMSON, EMBER, (re - 0.62) * 3));
				if (re > 0.48) return SpellArt.glow(SpellArt.mix(AMBER, GOLD, 0.5), 0.96);
				if (re > 0.25) return SpellArt.glow(SpellArt.mix(FLAME, VERMILION, (re - 0.25) * 4), 0.95);
				return SpellArt.glow(SpellArt.mix(WHITE_FIRE, GOLD_LIGHT, re * 3), 0.98);
			}
			if (re < 1.22) {
				// Flames licking out from the eye's rim.
				double lick = Math.abs(Math.sin(Math.atan2(ax, along) * 4 + seed));
				if (re - 1 < 0.22 * lick) return SpellArt.glow(SpellArt.mix(FLAME, GOLD, lick), 0.25 + 0.55 * lick);
			}
			if (V > 0.86) return SpellArt.CLEAR;
			// The vane: narrow, rippling, widening a little toward the eye.
			double half = eyeWidth * (0.1 + 0.07 * SpellArt.smooth(0.45, 0.75, V) + 0.027 * Math.sin(V * total * 0.5 + seed));
			if (ax > half) return SpellArt.CLEAR;
			if (ax < 0.34) return SpellArt.glow(SpellArt.mix(GOLD, WHITE_FIRE, V), 0.95);
			int c = SpellArt.ramp(V, CRIMSON, VERMILION, FLAME);
			double barb = (V * total - ax * 1.2) / 1.4;
			if (barb - Math.floor(barb) < 0.32) c = SpellArt.shade(c, -0.22);
			if (half - ax < 0.4) return SpellArt.glow(SpellArt.mix(c, GOLD, 0.5), 0.72);
			return SpellArt.col(c);
		};
	}

	/**
	 * A tongue of flame on a blade (its length up it): wide low down, wavering, its top split into {@code licks} tongues; white and gold at
	 * its hot heart, through amber and flame to crimson at its edges, seen through more toward its tips and faded in at its foot (so tongues
	 * stacked on one another run together); all of it glowing.
	 */
	static SpellArt.Painter flame(double licks, int seed) {
		return flame(licks, seed, CRIMSON, VERMILION, FLAME, AMBER, GOLD_LIGHT, WHITE_FIRE);
	}

	/** {@link #flame(double, int)} in another fire's colours, {@code heat} from its cool edges to its hot heart. */
	static SpellArt.Painter flame(double licks, int seed, int... heat) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.07 * Math.sin(v * 7 + seed) * v - 0.06 * (SpellArt.noise(v * 4 + seed, 3, seed) - 0.5);
			double half = 0.47 * Math.pow(Math.max(0, 1 - v * 0.75), 0.6) * (0.85 + 0.15 * SpellArt.noise(v * 9, seed, seed + 1));
			if (Math.abs(uu) > half) return SpellArt.CLEAR;
			double across = (uu / half + 1) / 2;
			double tongue = Math.pow(Math.abs(Math.sin(across * Math.PI * licks + seed * 0.3)), 0.7);
			double top = 0.42 + 0.58 * tongue * (0.75 + 0.25 * SpellArt.noise(across * 6 + seed, 1, seed + 2));
			if (v > top) return SpellArt.CLEAR;
			double foot = SpellArt.smooth(0, 0.24, v);
			double hot = SpellArt.clamp01((1 - v / top) * (1 - Math.abs(uu) / half) * 1.5) * (0.45 + 0.55 * foot);
			return SpellArt.glow(SpellArt.ramp(hot, heat), (0.3 + 0.62 * Math.sqrt(hot)) * (0.25 + 0.75 * foot));
		};
	}

	// --- The Vermilion Bird ---

	/** The point a fraction {@code f} of the way along a polyline, and the index of the piece it lies on (its fourth value). */
	static double[] along(double[][] line, double f) {
		double[] lens = new double[line.length - 1];
		double total = 0;
		for (int i = 0; i < lens.length; i++) total += lens[i] = SpellArt.len(SpellArt.sub(line[i + 1], line[i]));
		double d = SpellArt.clamp01(f) * total;
		for (int i = 0; i < lens.length; i++) {
			if (d <= lens[i] || i == lens.length - 1) {
				double[] p = SpellArt.add(line[i], SpellArt.times(SpellArt.sub(line[i + 1], line[i]), SpellArt.clamp01(d / lens[i])));
				return new double[] {p[0], p[1], p[2], i};
			}
			d -= lens[i];
		}
		throw new IllegalStateException();
	}

	static final double[] PLUME_LENGTHS = {72, 84, 94, 84, 72};
	/** A streamer's three sections: from and to (fractions of its length), and their width. */
	static final double[][] PLUME_SECTIONS = {{0, 0.32, 5.2}, {0.32, 0.64, 5.2}, {0.64, 1, 13}};

	// --- The Vermilion Bird, made of fire ---

	/** The fire's heat from its coolest to its hottest: embers, crimson, vermilion, flame, amber, pale gold, white. */
	static final int[] HEAT = {EMBER, CRIMSON, VERMILION, FLAME, AMBER, GOLD_LIGHT, WHITE_FIRE};

	/**
	 * Fire made into a body (on a solid): flames streaming back along it (+z, as a flying blaze trails) and licking up off it, hottest
	 * along its top and toward its heart (the line x = 0, y = {@code axisY}), white and gold there, cooling through flame and vermilion to
	 * crimson toward its edges and beneath; all glowing, seen through a little where it is coolest.
	 */
	static SpellArt.Painter blaze(double axisY, int seed) {
		return t -> {
			double r = Math.hypot(t.qx(), t.qy() - axisY);
			// Streaks running back along it: the sharp ridges of a noise stretched along z, wavering.
			double flow = SpellArt.fbm3(t.px * 0.22, t.py * 0.3, t.pz * 0.06, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.5, t.py * 0.5, t.pz * 0.11 + flow, seed + 5, 2) - 1);
			double heat = 0.62 - r * 0.04 + (flow - 0.5) * 0.7 + 0.25 * Math.max(0, t.ny) - 0.35 * Math.max(0, -t.ny) + 0.42 * Math.pow(streak, 3);
			heat = SpellArt.clamp01(heat);
			return SpellArt.glow(SpellArt.ramp(heat, HEAT), 0.66 + 0.32 * heat);
		};
	}

	/** {@link #blaze} on a limb (its own axis along its cube): hotter toward each face's middle and on top, streaked along it. */
	static SpellArt.Painter blazeLimb(int seed) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.25, t.py * 0.3, t.pz * 0.1, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm(t.fx * 3 + seed, t.ly * 0.35 + flow * 2, seed + 5, 2) - 1);
			double middle = 1 - Math.abs(t.fx - 0.5) * 2;
			double heat = SpellArt.clamp01(0.3 + (flow - 0.5) * 0.8 + 0.22 * Math.max(0, t.ny) + 0.3 * middle + 0.35 * Math.pow(streak, 3));
			return SpellArt.glow(SpellArt.ramp(heat, HEAT), 0.64 + 0.34 * heat);
		};
	}

	/**
	 * A feather of flame (on a blade, its length along it): a long tongue widest a third of the way out, drawn to a wavering point split
	 * into {@code licks}; white-gold down its middle, cooling through flame to crimson at its edges, flickering; glowing, seen through
	 * toward its edges and point.
	 */
	static SpellArt.Painter flameFeather(double licks, int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.05 * Math.sin(v * 9 + seed) * v - 0.05 * (SpellArt.noise(v * 5 + seed, 2, seed) - 0.5);
			double half = 0.48 * Math.pow(Math.sin(Math.PI * Math.min(1, Math.pow(v, 0.55)) * 0.999), 0.8) * (0.84 + 0.16 * SpellArt.noise(v * 11, seed, seed + 1));
			if (v < 0.015 || Math.abs(uu) > half) return SpellArt.CLEAR;
			double across = (uu / half + 1) / 2;
			double tongue = Math.pow(Math.abs(Math.sin(across * Math.PI * licks + seed * 0.4)), 0.6);
			double top = 0.7 + 0.3 * tongue * (0.8 + 0.2 * SpellArt.hash(seed, 1, 7));
			if (v > top) return SpellArt.CLEAR;
			// White-hot down its middle near its root, cooling out to its edges and on to its tips; streaks of hotter fire running out along it.
			double core = 1 - Math.abs(uu) / half, out = v / top;
			double streak = 1 - Math.abs(2 * SpellArt.fbm(across * 3.5 + seed, v * 2.6, seed + 2, 2) - 1);
			double hot = SpellArt.clamp01(core * (1.15 - 0.8 * out) + 0.3 * Math.pow(streak, 3) * (1 - out) - 0.08);
			return SpellArt.glow(SpellArt.ramp(hot, HEAT), 0.22 + 0.76 * Math.pow(hot, 0.6));
		};
	}

	/**
	 * A ribbon of fire (on a blade, its length along it: {@code v0}..{@code v1} of a streamer {@code total} pixels long): a wavering thread
	 * of white fire down its middle, flame either side of it licking out in tongues, wider toward its end, where it flares into a last
	 * burst; glowing, seen through toward its edges.
	 */
	static SpellArt.Painter flameRibbon(double total, double v0, double v1, int seed) {
		return t -> {
			double V = v0 + (v1 - v0) * t.v(), along = V * total, uu = t.u() - 0.5 - 0.07 * Math.sin(along * 0.16 + seed) * SpellArt.smooth(0, 0.3, V);
			double licks = 0.75 + 0.25 * Math.sin(along * 0.55 + seed) * Math.sin(along * 0.21 + seed * 2);
			double flare = V > 0.8 ? Math.sin((V - 0.8) / 0.2 * Math.PI) * 0.3 : 0;
			double half = Math.min(0.5, (0.12 + 0.2 * SpellArt.smooth(0, 0.75, V)) * licks + flare);
			if (Math.abs(uu) > half || V > 0.995) return SpellArt.CLEAR;
			double core = 1 - Math.abs(uu) / half, streak = 1 - Math.abs(2 * SpellArt.fbm(uu * 9 + seed, along * 0.09, seed + 1, 2) - 1);
			double hot = SpellArt.clamp01(Math.pow(core, 1.3) * (1.05 - 0.35 * V) + 0.25 * Math.pow(streak, 3) * core);
			return SpellArt.glow(SpellArt.ramp(hot, HEAT), 0.2 + 0.78 * Math.pow(hot, 0.6));
		};
	}

	/** White fire from {@code rootZ} to {@code tipZ} (a beak of it): pale gold at the root, white at the point; glowing. */
	static SpellArt.Painter whiteFire(double rootZ, double tipZ) {
		return t -> {
			double along = SpellArt.clamp01((t.qz() - rootZ) / (tipZ - rootZ)), flicker = SpellArt.noise(t.qx() * 1.5 + t.qy(), t.qz() * 1.2, 5);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.55 + 0.45 * along + (flicker - 0.5) * 0.2), HEAT), 0.92);
		};
	}

	/** An eye of the fire on the outer side of its cube: an almond of white fire in the blaze ({@code around}), and nothing more. */
	static SpellArt.Painter fireEye(SpellArt.Painter around) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return around.paint(t);
			double x = (t.back() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = Math.abs(y) - (1 - x * x) * 0.7;
			if (shape > 0.08) return around.paint(t);
			return SpellArt.glow(SpellArt.mix(WHITE_FIRE, GOLD_LIGHT, SpellArt.clamp01(shape + 0.45)));
		};
	}

	/**
	 * The Vermilion Bird of the south, made of fire (the user, 2026-10-24: the great spells' beasts made of their elements, not looking like
	 * animals): its body a blaze ("body"), white-hot along its back, flames streaming off it where feathers were; its neck ("neck") ruffed in
	 * flame; its head ("head", the lower beak "beak_low") a flame with a beak of white fire and eyes of white fire, a crest ("crest") of three
	 * ribbons of fire; its wings ("wing_l", "wing_r", each with its "_mid" and "_tip") and the fan over its tail ("tail") all tongues of
	 * flame, five ribbons of fire for streamers ("plume_0".."plume_4", each of three bones "_0".."_2"), flaring at their ends; fire trailing
	 * where its legs would be ("leg_l", "leg_r"). Its bones the beast's ({@link #vermilionBirdBeast}), so it glides and dives as that did.
	 */
	static void vermilionBird(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 0, 0);
		SpellArt.Painter fire = blaze(0.5, 7);
		// The blaze of its body: the breast deep, tapering to the rump.
		body.box(0, 0, -7, 15, 11, 14, fire);
		body.box(0, 0.6, -7, 11, 14.2, 13, fire);
		body.box(0, -4.6, -9, 9.4, 4, 9.4, fire);
		body.box(0, 0, -14.6, 11.4, 9.4, 2.4, fire);
		body.box(0, 0.2, -16.2, 7.6, 6.6, 1.6, fire);
		body.box(0, 0.6, 4, 13, 10, 12, fire);
		body.box(0, 1, 4, 9.6, 12.4, 11.4, fire);
		body.box(0, 1.6, 13.5, 9, 7, 9, fire);
		body.box(0, 1.9, 13.5, 6.6, 8.6, 8.4, fire);
		// Flames streaming back off its back and flanks, lifting away from it; and flaring off its breast.
		double[] rowZ = {-12, -6, 0, 6, 12}, rowR = {8.5, 8.3, 7.9, 7.3, 6.3};
		for (int row = 0; row < rowZ.length; row++) {
			for (int k = -3; k <= 3; k++) {
				double phi = k * 26, r = rowR[row] * (Math.abs(k) == 3 ? 0.97 : 1);
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * r, 0.5 + Math.cos(Math.toRadians(phi)) * r, rowZ[row]);
				int variant = (row + Math.abs(k)) % 3;
				SpellArt.shared(m, body, "back_flame_" + variant, true, base, 13 + variant, 7, 22, 0, phi, flameFeather(2, 20 + variant), 2.5, k < 0);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int k = -1; k <= 1; k++) {
				double phi = 180 + k * 34;
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 7.2, 0.5 + Math.cos(Math.toRadians(phi)) * (7.4 - row * 0.4), -15.5 + row * 7);
				int variant = (row + Math.abs(k)) % 2;
				SpellArt.shared(m, body, "breast_flame_" + variant, true, base, 13, 8, 16, 0, phi, flameFeather(2, 30 + variant), 2.5, k < 0);
			}
		}
		// Flames licking up off its back from its nape to its rump, streaming back; and off its flanks, up and back.
		for (int k = 0; k < 8; k++) {
			double side = k % 2 == 0 ? 1 : -1;
			SpellArt.blade(body, SpellArt.v3(side * (k % 3) * 0.6, 8.2 - Math.abs(k - 3) * 0.3, -14 + k * 4), 10.5 - Math.abs(k - 3) * 0.6, 5.6, -62 - k * 3, side * 10, side * 8, flame(2.2, 40 + k)).density(2.5);
		}
		for (int s = -1; s <= 1; s += 2) {
			for (int k = 0; k < 4; k++) {
				SpellArt.blade(body, SpellArt.v3(s * 6.8, 2.6 - k * 0.4, -10 + k * 7), 8.5 - k * 0.6, 4.6, -70, s * 18, s * (40 + k * 6), flame(2, 50 + k + (s > 0 ? 0 : 7))).density(2.5);
			}
		}
		neckAndHeadOfFire(m);
		wingOfFire(m, 1);
		wingOfFire(m, -1);
		tailOfFire(m);
		legsOfFire(m);
		glide(m);
		dive(m);
	}

	static void neckAndHeadOfFire(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 3, -12);
		SpellArt.Painter fire = blazeLimb(50);
		double[] from = SpellArt.v3(0, 2.4, -10.5), to = SpellArt.v3(0, 8.8, -21.2);
		SpellArt.limb(neck, from, to, 7.6, 6.6, fire);
		SpellArt.limb(neck, from, to, 5.6, 8.8, fire);
		// The ruff: flames flaring back round the neck's foot.
		for (int k = -5; k <= 5; k++) {
			double phi = k * 28;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 5.4, 3.6 + Math.cos(Math.toRadians(phi)) * 5.0, -13.5 + Math.abs(k) * 0.3);
			int variant = Math.abs(k) % 3;
			SpellArt.shared(m, neck, "ruff_" + variant, true, base, 11.5, 4.8, 24, 0, phi, flameFeather(2, 60 + variant), 2.5, k < 0);
		}
		SpellArt.Bone head = m.bone("head", "neck", 0, 8.6, -20);
		SpellArt.Painter face = blaze(10, 41), point = whiteFire(-29.5, -38);
		head.box(0, 10, -24.4, 7, 6.6, 9, face);
		head.box(0, 10.3, -24.4, 5.2, 8.2, 8.2, face);
		head.box(0, 9.1, -28.6, 5.4, 5, 3.2, face);
		// Flames lying back along the sides of the head.
		for (int s = -1; s <= 1; s += 2) {
			for (int k = 0; k < 2; k++) {
				SpellArt.shared(m, head, "cheek_" + k, true, SpellArt.v3(s * 3.6, 9.4 - k * 1.8, -27.2 + k * 0.6), 8 - k, 4, 8, s * 6, s * 90, flameFeather(2, 66 + k), 3, s < 0);
			}
		}
		// The beak: a wedge of white fire, the upper drawn to a hooked point; the lower its own bone (it cries).
		head.box(0, 9.7, -31.6, 3.8, 2.8, 4.4, point);
		head.box(0, 9.5, -35, 2.8, 2.2, 2.8, point);
		SpellArt.limb(head, SpellArt.v3(0, 9.9, -36.2), SpellArt.v3(0, 7.8, -37.8), 1.5, 1.5, point).density(3);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 3.55, 10.9, -26.2, 0.6, 2.4, 3, fireEye(face)).density(4);
			// Flames streaming back from behind each eye.
			for (int k = 0; k < 2; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 3.9, 11.2 - k * 1.6, -23.4 + k), 10 - k * 2, 4.2, -76 - k * 10, s * (18 + k * 10), s * (14 + k * 6), flame(2, 70 + k * 2 + s)).density(2.5);
			}
		}
		SpellArt.Bone low = m.bone("beak_low", "head", 0, 8.4, -29.2);
		low.box(0, 7.9, -31.8, 3.2, 1.4, 5, point);
		low.box(0, 7.8, -34.8, 2.2, 1.0, 1.8, point);
		// A wattle of flame under the throat, streaming back.
		SpellArt.blade(head, SpellArt.v3(0, 6.6, -27.6), 9, 3.8, -150, 0, 0, flame(1.6, 76)).density(2.5);
		// The crest: three long ribbons of fire sweeping back from the crown, flames fanned at their foot.
		SpellArt.Bone crest = m.bone("crest", "head", 0, 14, -25);
		for (int k = 0; k < 3; k++) {
			double len = 26 - Math.abs(k - 1) * 5;
			SpellArt.blade(crest, SpellArt.v3((k - 1) * 1.2, 13.6, -25.4 + Math.abs(k - 1) * 0.8), len, 4.8, -58 - Math.abs(k - 1) * 10, (k - 1) * 16, (k - 1) * 10,
					flameRibbon(len, 0, 1, 80 + k)).density(3);
		}
		for (int k = 0; k < 4; k++) {
			double side = k % 2 == 0 ? 1 : -1;
			SpellArt.blade(crest, SpellArt.v3(side * 1.6, 13.4, -27 + k * 0.9), 11 - k * 1.2, 5, -34 - k * 8, side * (22 + k * 6), side * 16, flame(2.2, 84 + k)).density(2.5);
		}
	}

	static void wingOfFire(SpellArt.Model m, int s) {
		String w = "wing_" + (s > 0 ? "l" : "r");
		boolean flip = s < 0;
		// The leading edge: shoulder, elbow, wrist, the hand's end (as the beast's).
		double[][] edge = {SpellArt.v3(s * 6.5, 4.2, -6), SpellArt.v3(s * 22, 5.4, -4), SpellArt.v3(s * 38, 6.4, -6.5), SpellArt.v3(s * 50, 6.8, -5)};
		SpellArt.Bone[] bones = {m.bone(w, "body", edge[0][0], edge[0][1], edge[0][2], 0, 0, -s * 6), m.bone(w + "_mid", w, edge[1][0], edge[1][1], edge[1][2]),
				m.bone(w + "_tip", w + "_mid", edge[2][0], edge[2][1], edge[2][2])};
		SpellArt.Painter arm = blazeLimb(90);
		bones[0].box(edge[0][0] + s * 1.5, edge[0][1], edge[0][2] + 1.5, 7, 5.6, 8, blaze(0.5, 7));
		SpellArt.limb(bones[0], edge[0], edge[1], 5.2, 3.8, arm);
		SpellArt.limb(bones[1], edge[1], edge[2], 4.2, 3.0, arm);
		SpellArt.limb(bones[2], edge[2], edge[3], 3.2, 2.4, arm);
		double l0 = SpellArt.len(SpellArt.sub(edge[1], edge[0])), l1 = SpellArt.len(SpellArt.sub(edge[2], edge[1])), l2 = SpellArt.len(SpellArt.sub(edge[3], edge[2]));
		double wrist = (l0 + l1) / (l0 + l1 + l2);
		// Where the secondaries were: tongues of flame trailing back along the arm, longer toward the wrist.
		for (int k = 0; k < 11; k++) {
			double[] at = along(edge, 0.04 + (wrist - 0.04) * k / 10.0);
			SpellArt.shared(m, bones[(int) at[3]], "secondary_" + k, true, SpellArt.v3(at[0], at[1] - 0.6, at[2] + 1.4), 21 + 6 * k / 10.0, 7, -3, s * (k * 0.6 - 2), 0,
					flameFeather(2, 100 + k), 2, flip);
		}
		// Where the primaries were: long tongues fanning out from the hand.
		double[] lengths = {30, 32, 34, 36, 37.5, 38.5, 38, 36, 33, 29};
		for (int k = 0; k < 10; k++) {
			double[] at = along(edge, wrist + (1 - wrist) * Math.min(1, k / 7.0));
			SpellArt.shared(m, bones[2], "primary_" + k, true, SpellArt.v3(at[0], at[1] - 0.9 - k * 0.05, at[2] + 1), lengths[k], 7.2 - k * 0.16, -2 + k * 0.4, s * (12 + k * 7.2), 0,
					flameFeather(k > 5 ? 3 : 2, 200 + k), 2, flip);
		}
		// Over the gap at the body, and over the tongues' roots along the leading edge: shorter flames.
		for (int k = 0; k < 3; k++) {
			SpellArt.shared(m, bones[0], "tertial_" + k, true, SpellArt.v3(s * (5 + k * 2.2), 4.6, -1 + k * 0.8), 18 + k * 2, 6, -2, -s * (10 - k * 4), 0, flameFeather(2, 500 + k), 2, flip);
		}
		for (int k = 0; k < 14; k++) {
			double f = 0.04 + 0.94 * k / 13.0;
			double[] at = along(edge, f);
			double heading = f > wrist ? s * (8 + (f - wrist) / (1 - wrist) * 40) : s * (k * 0.5 - 1);
			SpellArt.shared(m, bones[(int) at[3]], "greater_" + k % 4, true, SpellArt.v3(at[0], at[1] + 0.5, at[2] + 1.2), 12.5, 5.4, -4, heading, 0, flameFeather(2, 300 + k % 4), 2, flip);
		}
		for (int k = 0; k < 16; k++) {
			double[] at = along(edge, 0.03 + 0.95 * k / 15.0);
			SpellArt.shared(m, bones[(int) at[3]], "lesser_" + k % 3, true, SpellArt.v3(at[0], at[1] + 2.0, at[2] - 0.8), 7.5, 4.6, -10, s * 6, 0, flameFeather(2, 400 + k % 3), 2.5, flip);
		}
	}

	static void tailOfFire(SpellArt.Model m) {
		SpellArt.Bone tail = m.bone("tail", "body", 0, 2.4, 17);
		// The fan over the streamers' roots: broad tongues.
		for (int k = 0; k < 9; k++) {
			int off = Math.abs(k - 4);
			SpellArt.shared(m, tail, "tail_flame_" + off, true, SpellArt.v3((k - 4) * 0.8, 2.8 + (4 - off) * 0.12, 16.5), 24 + (4 - off) * 1.6, 6.4, -4, (k - 4) * 9.5, 0,
					flameFeather(2, 600 + off), 2, k < 4);
		}
		// The streamers: five ribbons of fire, each of three sections (its bones a chain), twisting a little as they go, flaring at their ends.
		for (int k = 0; k < 5; k++) {
			double heading = (k - 2) * 11, pitch = -6 - Math.abs(k - 2) * 2, total = PLUME_LENGTHS[k];
			double[] at = SpellArt.v3((k - 2) * 1.2, 2.4, 19);
			String up = "tail";
			for (int j = 0; j < PLUME_SECTIONS.length; j++) {
				double[] sec = PLUME_SECTIONS[j];
				double length = total * (sec[1] - sec[0]), bank = (k - 2) * 10 + (k < 2 ? -1 : 1) * j * 12;
				SpellArt.Bone b = m.bone("plume_" + k + "_" + j, up, at[0], at[1], at[2]);
				SpellArt.flatBlade(b, at, length + 0.6, sec[2], pitch, heading, bank, flameRibbon(total, sec[0], sec[1], 700 + k)).density(2);
				at = SpellArt.add(at, SpellArt.times(SpellArt.turn(SpellArt.v3(0, 0, 1), pitch, heading, bank), length));
				up = b.name;
			}
		}
	}

	/** Where its legs would be (their bones kept, as the beast's), flames trailing back under it. */
	static void legsOfFire(SpellArt.Model m) {
		for (int s = -1; s <= 1; s += 2) {
			double[] hip = SpellArt.v3(s * 3.4, -4.4, 8);
			SpellArt.Bone leg = m.bone("leg_" + (s > 0 ? "l" : "r"), "body", hip[0], hip[1], hip[2]);
			for (int j = 0; j < 3; j++) {
				SpellArt.flatBlade(leg, SpellArt.add(hip, SpellArt.v3(s * j * 0.6, -0.4 - j * 0.5, 0)), 13 - j * 2.5, 4 - j * 0.6, -10 - j * 7, s * (4 + j * 7), 0,
						flameFeather(2, 110 + j + (s > 0 ? 0 : 5))).density(2.5);
			}
		}
	}

	/**
	 * The Vermilion Bird of the south, a phoenix of fire: its body ("body") deep-breasted and feathered vermilion, gold beneath, flames rising
	 * off its back; its neck ("neck") ruffed in flame-tipped feathers; its head ("head", the lower beak "beak_low") with a crest ("crest") of
	 * three plumes each ending in an eye of fire; two great wings ("wing_l", "wing_r", each with its "_mid" and "_tip"), every flight feather
	 * burning at its tip; a fan of tail coverts ("tail") over five long streamers ("plume_0".."plume_4", each of three bones "_0".."_2"),
	 * each ending in a phoenix's eye; its legs ("leg_l", "leg_r") tucked back in flight. It faces -z, its wings spread along x. Stored (2026-10-24):
	 * the spell's bird is now made of fire, {@link #vermilionBird}; this, the true beast, is kept for later (SpellArt --stored).
	 */
	static void vermilionBirdBeast(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 0, 0);
		SpellArt.Painter down = plumage(0.5, 2.0, 16, 7);
		// The body: the breast deep and round, tapering to the rump; each piece a wide box and a tall one, so it rounds.
		body.box(0, 0, -7, 15, 11, 14, down);
		body.box(0, 0.6, -7, 11, 14.2, 13, down);
		body.box(0, -4.6, -9, 9.4, 4, 9.4, down);
		body.box(0, 0, -14.6, 11.4, 9.4, 2.4, down);
		body.box(0, 0.2, -16.2, 7.6, 6.6, 1.6, down);
		body.box(0, 0.6, 4, 13, 10, 12, down);
		body.box(0, 1, 4, 9.6, 12.4, 11.4, down);
		body.box(0, 1.6, 13.5, 9, 7, 9, down);
		body.box(0, 1.9, 13.5, 6.6, 8.6, 8.4, down);
		// Contour feathers lying over the back and down the flanks, each banked round the body; and over the breast.
		double[] rowZ = {-12, -6, 0, 6, 12}, rowR = {8.5, 8.3, 7.9, 7.3, 6.3};
		for (int row = 0; row < rowZ.length; row++) {
			for (int k = -3; k <= 3; k++) {
				double phi = k * 26, r = rowR[row] * (Math.abs(k) == 3 ? 0.97 : 1);
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * r, 0.5 + Math.cos(Math.toRadians(phi)) * r, rowZ[row]);
				int variant = (row + Math.abs(k)) % 3;
				SpellArt.shared(m, body, "contour_" + variant, true, base, 10, 7, 3, 0, phi, feather(RUDDY, VERMILION, FLAME, 1.2, 0, 0, 20 + variant), 2.5, k < 0);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int k = -1; k <= 1; k++) {
				double phi = 180 + k * 34;
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 7.2, 0.5 + Math.cos(Math.toRadians(phi)) * (7.4 - row * 0.4), -15.5 + row * 7);
				int variant = (row + Math.abs(k)) % 2;
				SpellArt.shared(m, body, "breast_" + variant, true, base, 12, 8, 2, 0, phi, feather(FLAME, AMBER, BELLY, 1.2, 0, 0, 30 + variant), 2.5, k < 0);
			}
		}
		// Flames rising off the back between the wings, streaming back.
		for (int k = 0; k < 3; k++) {
			double side = k % 2 == 0 ? 1 : -1;
			SpellArt.blade(body, SpellArt.v3(side * k * 0.6, 8.4 - k * 0.5, -6 + k * 7), 9.5 - k, 5.6, -68 - k * 5, side * 10, side * 8, flame(2.2, 40 + k)).density(2.5);
		}
		neckAndHead(m);
		wing(m, 1);
		wing(m, -1);
		tail(m);
		legs(m);
		glide(m);
		dive(m);
	}

	static void neckAndHead(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 3, -12);
		SpellArt.Painter nape = plumageLimb(1.5, 12, true, 50);
		double[] from = SpellArt.v3(0, 2.4, -10.5), to = SpellArt.v3(0, 8.8, -21.2);
		SpellArt.limb(neck, from, to, 7.6, 6.6, nape);
		SpellArt.limb(neck, from, to, 5.6, 8.8, nape);
		// The ruff: flame-tipped feathers flaring back round the neck's foot.
		for (int k = -5; k <= 5; k++) {
			double phi = k * 28;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 5.4, 3.6 + Math.cos(Math.toRadians(phi)) * 5.0, -13.5 + Math.abs(k) * 0.3);
			int variant = Math.abs(k) % 3;
			SpellArt.shared(m, neck, "ruff_" + variant, true, base, 11.5, 4.8, 24, 0, phi, feather(VERMILION, FLAME, GOLD_LIGHT, 1.1, 0.38, 0, 60 + variant), 2.5, k < 0);
		}
		SpellArt.Bone head = m.bone("head", "neck", 0, 8.6, -20);
		SpellArt.Painter face = headPlumage(41), horn = beak(-29.5, -38);
		head.box(0, 10, -24.4, 7, 6.6, 9, face);
		head.box(0, 10.3, -24.4, 5.2, 8.2, 8.2, face);
		head.box(0, 9.1, -28.6, 5.4, 5, 3.2, face);
		// Cheek plumes lying back along the sides of the head.
		for (int s = -1; s <= 1; s += 2) {
			for (int k = 0; k < 2; k++) {
				SpellArt.shared(m, head, "cheek_" + k, true, SpellArt.v3(s * 3.6, 9.4 - k * 1.8, -27.2 + k * 0.6), 8 - k, 4, 8, s * 6, s * 90,
						feather(VERMILION, FLAME, GOLD, 1.2, 0.3, 0, 66 + k), 3, s < 0);
			}
		}
		// The beak: the upper, hooked; the lower its own bone (it cries).
		head.box(0, 9.7, -31.6, 3.8, 2.8, 4.4, horn);
		head.box(0, 9.5, -35, 2.8, 2.2, 2.8, horn);
		SpellArt.limb(head, SpellArt.v3(0, 9.9, -36.2), SpellArt.v3(0, 7.8, -37.8), 1.5, 1.5, horn).density(3);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 3.55, 10.9, -26.2, 0.6, 2.4, 3, eye()).density(4);
			// Flames streaming back from behind each eye.
			for (int k = 0; k < 2; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 3.9, 11.2 - k * 1.6, -23.4 + k), 10 - k * 2, 4.2, -76 - k * 10, s * (18 + k * 10), s * (14 + k * 6), flame(2, 70 + k * 2 + s)).density(2.5);
			}
		}
		SpellArt.Bone low = m.bone("beak_low", "head", 0, 8.4, -29.2);
		low.box(0, 7.9, -31.8, 3.2, 1.4, 5, horn);
		low.box(0, 7.8, -34.8, 2.2, 1.0, 1.8, horn);
		// A wattle of flame under the throat, streaming back.
		SpellArt.blade(head, SpellArt.v3(0, 6.6, -27.6), 9, 3.8, -150, 0, 0, flame(1.6, 76)).density(2.5);
		// The crest: three long plumes sweeping back from the crown, each ending in an eye of fire, flames fanned at their foot.
		SpellArt.Bone crest = m.bone("crest", "head", 0, 14, -25);
		for (int k = 0; k < 3; k++) {
			double len = 26 - Math.abs(k - 1) * 5;
			SpellArt.blade(crest, SpellArt.v3((k - 1) * 1.2, 13.6, -25.4 + Math.abs(k - 1) * 0.8), len, 4.8, -58 - Math.abs(k - 1) * 10, (k - 1) * 16, (k - 1) * 10,
					streamer(4.8, 4.8, len, 0, 1, 80 + k)).density(3);
		}
		for (int k = 0; k < 4; k++) {
			double side = k % 2 == 0 ? 1 : -1;
			SpellArt.blade(crest, SpellArt.v3(side * 1.6, 13.4, -27 + k * 0.9), 11 - k * 1.2, 5, -34 - k * 8, side * (22 + k * 6), side * 16, flame(2.2, 84 + k)).density(2.5);
		}
	}

	static void wing(SpellArt.Model m, int s) {
		String w = "wing_" + (s > 0 ? "l" : "r");
		boolean flip = s < 0;
		// The leading edge: shoulder, elbow, wrist, the hand's end.
		double[][] edge = {SpellArt.v3(s * 6.5, 4.2, -6), SpellArt.v3(s * 22, 5.4, -4), SpellArt.v3(s * 38, 6.4, -6.5), SpellArt.v3(s * 50, 6.8, -5)};
		SpellArt.Bone[] bones = {m.bone(w, "body", edge[0][0], edge[0][1], edge[0][2], 0, 0, -s * 6), m.bone(w + "_mid", w, edge[1][0], edge[1][1], edge[1][2]),
				m.bone(w + "_tip", w + "_mid", edge[2][0], edge[2][1], edge[2][2])};
		SpellArt.Painter arm = plumageLimb(1.6, 8, false, 90);
		bones[0].box(edge[0][0] + s * 1.5, edge[0][1], edge[0][2] + 1.5, 7, 5.6, 8, plumage(0.5, 2.0, 16, 7));
		SpellArt.limb(bones[0], edge[0], edge[1], 5.2, 3.8, arm);
		SpellArt.limb(bones[1], edge[1], edge[2], 4.2, 3.0, arm);
		SpellArt.limb(bones[2], edge[2], edge[3], 3.2, 2.4, arm);
		double l0 = SpellArt.len(SpellArt.sub(edge[1], edge[0])), l1 = SpellArt.len(SpellArt.sub(edge[2], edge[1])), l2 = SpellArt.len(SpellArt.sub(edge[3], edge[2]));
		double wrist = (l0 + l1) / (l0 + l1 + l2);
		// The secondaries along the arm and forearm, trailing back, longer toward the wrist.
		for (int k = 0; k < 11; k++) {
			double[] at = along(edge, 0.04 + (wrist - 0.04) * k / 10.0);
			SpellArt.shared(m, bones[(int) at[3]], "secondary_" + k, true, SpellArt.v3(at[0], at[1] - 0.6, at[2] + 1.4), 21 + 6 * k / 10.0, 7, -3, s * (k * 0.6 - 2), 0,
					feather(CRIMSON, VERMILION, FLAME, 1, 0.26, 0.08, 100 + k), 2, flip);
		}
		// The primaries from the hand, fanning out to the fingers at its end.
		double[] lengths = {30, 32, 34, 36, 37.5, 38.5, 38, 36, 33, 29};
		for (int k = 0; k < 10; k++) {
			double[] at = along(edge, wrist + (1 - wrist) * Math.min(1, k / 7.0));
			SpellArt.shared(m, bones[2], "primary_" + k, true, SpellArt.v3(at[0], at[1] - 0.9 - k * 0.05, at[2] + 1), lengths[k], 7.2 - k * 0.16, -2 + k * 0.4, s * (12 + k * 7.2), 0,
					feather(CRIMSON, VERMILION, GOLD, 1, 0.34, k > 5 ? 0.3 : 0.12, 200 + k), 2, flip);
		}
		// The tertials over the gap at the body.
		for (int k = 0; k < 3; k++) {
			SpellArt.shared(m, bones[0], "tertial_" + k, true, SpellArt.v3(s * (5 + k * 2.2), 4.6, -1 + k * 0.8), 18 + k * 2, 6, -2, -s * (10 - k * 4), 0,
					feather(RUDDY, VERMILION, FLAME, 1, 0.22, 0, 500 + k), 2, flip);
		}
		// The greater coverts over the flight feathers' roots, and the lesser in rows over the leading edge.
		for (int k = 0; k < 14; k++) {
			double f = 0.04 + 0.94 * k / 13.0;
			double[] at = along(edge, f);
			double heading = f > wrist ? s * (8 + (f - wrist) / (1 - wrist) * 40) : s * (k * 0.5 - 1);
			SpellArt.shared(m, bones[(int) at[3]], "greater_" + k % 4, true, SpellArt.v3(at[0], at[1] + 0.5, at[2] + 1.2), 12.5, 5.4, -4, heading, 0,
					feather(EMBER, CRIMSON, VERMILION, 1.1, 0, 0.05, 300 + k % 4), 2, flip);
		}
		for (int k = 0; k < 16; k++) {
			double[] at = along(edge, 0.03 + 0.95 * k / 15.0);
			SpellArt.shared(m, bones[(int) at[3]], "lesser_" + k % 3, true, SpellArt.v3(at[0], at[1] + 2.0, at[2] - 0.8), 7.5, 4.6, -10, s * 6, 0,
					feather(RUDDY, VERMILION, FLAME, 1.2, 0, 0, 400 + k % 3), 2.5, flip);
		}
	}

	static void tail(SpellArt.Model m) {
		SpellArt.Bone tail = m.bone("tail", "body", 0, 2.4, 17);
		// The coverts: a fan of broad feathers over the streamers' roots.
		for (int k = 0; k < 9; k++) {
			int off = Math.abs(k - 4);
			SpellArt.shared(m, tail, "tail_covert_" + off, true, SpellArt.v3((k - 4) * 0.8, 2.8 + (4 - off) * 0.12, 16.5), 24 + (4 - off) * 1.6, 6.4, -4, (k - 4) * 9.5, 0,
					feather(CRIMSON, VERMILION, GOLD, 1, 0.3, 0, 600 + off), 2, k < 4);
		}
		// The streamers: five, each of three sections (its bones a chain), twisting a little as they go, each ending in an eye.
		for (int k = 0; k < 5; k++) {
			double heading = (k - 2) * 11, pitch = -6 - Math.abs(k - 2) * 2, total = PLUME_LENGTHS[k];
			double[] at = SpellArt.v3((k - 2) * 1.2, 2.4, 19);
			String up = "tail";
			for (int j = 0; j < PLUME_SECTIONS.length; j++) {
				double[] sec = PLUME_SECTIONS[j];
				double length = total * (sec[1] - sec[0]), bank = (k - 2) * 10 + (k < 2 ? -1 : 1) * j * 12;
				SpellArt.Bone b = m.bone("plume_" + k + "_" + j, up, at[0], at[1], at[2]);
				SpellArt.flatBlade(b, at, length + 0.6, sec[2], pitch, heading, bank, streamer(sec[2], PLUME_SECTIONS[2][2], total, sec[0], sec[1], 700 + k)).density(2);
				at = SpellArt.add(at, SpellArt.times(SpellArt.turn(SpellArt.v3(0, 0, 1), pitch, heading, bank), length));
				up = b.name;
			}
		}
	}

	static void legs(SpellArt.Model m) {
		for (int s = -1; s <= 1; s += 2) {
			double[] hip = SpellArt.v3(s * 3.4, -4.4, 8), knee = SpellArt.v3(s * 3.9, -7.2, 12.6), ankle = SpellArt.v3(s * 3.6, -8.4, 19.4);
			SpellArt.Bone leg = m.bone("leg_" + (s > 0 ? "l" : "r"), "body", hip[0], hip[1], hip[2]);
			SpellArt.limb(leg, hip, knee, 3.4, 3.4, plumageLimb(1.2, 6, false, 110));
			SpellArt.limb(leg, knee, ankle, 1.5, 1.5, shank()).density(3);
			// The foot tucked back under the tail: three toes and the hind one, their talons curled.
			for (int j = -1; j <= 1; j++) {
				double[] knuckle = SpellArt.add(ankle, SpellArt.v3(s * j * 0.6, -0.2, 0.4)), toe = SpellArt.add(knuckle, SpellArt.v3(s * j * 0.8, -0.6, 2.6)),
						point = SpellArt.add(toe, SpellArt.v3(0, -1.4, 0.4));
				SpellArt.limb(leg, knuckle, toe, 0.8, 0.8, shank()).density(3);
				SpellArt.limb(leg, toe, point, 0.6, 0.6, talon()).density(4);
			}
			double[] heel = SpellArt.add(ankle, SpellArt.v3(0, 0.2, -0.6)), spur = SpellArt.add(heel, SpellArt.v3(0, -1.3, -1.2));
			SpellArt.limb(leg, heel, spur, 0.6, 0.6, talon()).density(4);
		}
	}

	/**
	 * The glide: deep, strong wingbeats (the user's ask, 2026-10-25: the beasts "are very rigid their animations need to be more fluid"), each
	 * part of the wing whipping after the one before it, swept forward on the downstroke and back on the up (a figure of eight); the body rising
	 * on each downstroke and rolling a little, the neck and head riding it to hold the gaze steady; the streamers lashing out behind in waves.
	 */
	static void glide(SpellArt.Model m) {
		double T = 2.0;
		SpellArt.Anim a = m.anim("glide", T, "true");
		int steps = 24;
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, T, steps, t -> {
				double ph = 2 * Math.PI * t / T;
				return new double[] {2 * Math.sin(ph + 1.2), -ss * 9 * Math.sin(ph + 0.6), ss * (6 + 34 * Math.sin(ph))};
			});
			a.sample(w + "_mid", "rotation", 0, T, steps, t -> {
				double ph = 2 * Math.PI * t / T;
				return new double[] {0, -ss * 6 * Math.max(0, -Math.sin(ph - 0.9)), ss * 24 * Math.sin(ph - 0.9)};
			});
			a.sample(w + "_tip", "rotation", 0, T, steps, t -> {
				double ph = 2 * Math.PI * t / T;
				return new double[] {0, -ss * 12 * Math.max(0, -Math.sin(ph - 1.6)), ss * 28 * Math.sin(ph - 1.6)};
			});
		}
		a.sample("root", "position", 0, T, steps, t -> new double[] {0, 4.5 * Math.sin(2 * Math.PI * t / T - 0.5), 0});
		a.sample("body", "rotation", 0, T, steps, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T - 0.3), 0, 2.5 * Math.sin(2 * Math.PI * t / T + 1.0)});
		a.sample("neck", "rotation", 0, T, steps, t -> new double[] {-4 * Math.sin(2 * Math.PI * t / T - 0.5), 0, 0});
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {-3.5 * Math.sin(2 * Math.PI * t / T - 0.2), 3 * Math.sin(2 * Math.PI * t / T + 0.6), -2.5 * Math.sin(2 * Math.PI * t / T + 1.0)});
		a.sample("beak_low", "rotation", 0, T, 8, t -> new double[] {3 + 3 * Math.sin(2 * Math.PI * t / T), 0, 0});
		a.sample("crest", "rotation", 0, T, steps, t -> new double[] {7 * Math.sin(4 * Math.PI * t / T - 1), 0, 0});
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {6 * Math.sin(2 * Math.PI * t / T - 1.5), 4 * Math.sin(2 * Math.PI * t / T - 2), 0});
		for (int k = 0; k < 5; k++) {
			for (int j = 0; j < PLUME_SECTIONS.length; j++) {
				int kk = k, jj = j;
				a.sample("plume_" + k + "_" + j, "rotation", 0, T, steps, t -> {
					double ph = 2 * Math.PI * t / T;
					return new double[] {(5 + 5 * jj) * Math.sin(ph - 1.8 - 1.0 * jj - 0.3 * kk), (4 + 4 * jj) * Math.sin(ph - 1.1 * jj + 1.1 * kk), 0};
				});
			}
		}
	}

	/**
	 * The dive: the wings swept back and up into a dart, the head thrust out, the crest laid flat; then, at the strike, the wings flare
	 * open, the streamers fan and it cries; then it settles back into its glide.
	 */
	static void dive(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("dive", 1.4, null);
		double[] t = {0, 0.25, 0.55, 0.8, 0.95, 1.1, 1.25, 1.4};
		double[] fold = {0, 1, 1, 1, 0.2, -0.28, -0.1, 0};
		double[] cry = {0, 0.1, 0.2, 0.5, 1, 1, 0.5, 0};
		for (int i = 0; i < t.length; i++) {
			double f = fold[i], in = Math.max(0, f), flare = Math.max(0, -f);
			for (int s = -1; s <= 1; s += 2) {
				String w = "wing_" + (s > 0 ? "l" : "r");
				// Folded: swept back along its flanks (a stoop), held a little low; flared: thrown forward and down.
				a.rot(w, t[i], 0, -s * (f > 0 ? 58 : 42) * f, s * (f > 0 ? 6 : 18) * f);
				a.rot(w + "_mid", t[i], 0, -s * 44 * f, s * 2 * f);
				a.rot(w + "_tip", t[i], 0, -s * 36 * f, -s * 4 * f);
			}
			a.rot("neck", t[i], 10 * in - 8 * cry[i], 0, 0);
			a.rot("head", t[i], 6 * in - 10 * cry[i], 0, 0);
			a.rot("beak_low", t[i], 3 + 32 * cry[i], 0, 0);
			a.rot("crest", t[i], -16 * in + 10 * cry[i], 0, 0);
			a.rot("tail", t[i], -4 * f, 0, 0);
			for (int k = 0; k < 5; k++) a.rot("plume_" + k + "_0", t[i], 0, (k - 2) * 26 * flare, 0);
		}
	}

	// --- One Flame Burns the Heavens: the seed ---

	/** The pearl of fire at the seed's heart: white-hot, swirling gold. */
	static SpellArt.Painter pearl(int seed) {
		return t -> {
			double s = SpellArt.fbm3(t.px * 0.6 + seed, t.py * 0.6, t.pz * 0.6, seed, 3);
			double sw = Math.sin((t.px + t.pz) * 0.9 + t.py * 1.3 + s * 6);
			int c = SpellArt.mix(WHITE_FIRE, GOLD_LIGHT, 0.5 + 0.5 * sw);
			if (s < 0.4) c = SpellArt.mix(c, AMBER, (0.4 - s) * 2);
			return SpellArt.glow(c, 0.97);
		};
	}

	/** The pearl's halo: a haze of gold light, brightest toward each face's middle. */
	static SpellArt.Painter halo() {
		return t -> {
			double r = Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2;
			return SpellArt.glow(SpellArt.mix(GOLD_LIGHT, AMBER, r), 0.34 * (1 - 0.6 * r));
		};
	}

	/**
	 * A lotus petal of flame (its section from {@code v0} to {@code v1} of the whole, on a blade): broad and pointed, crimson at its foot
	 * through vermilion to gold at its point, veins of light running up it, its rim burning gold; seen through.
	 */
	static SpellArt.Painter petal(double v0, double v1, int seed) {
		return t -> {
			double V = v0 + (v1 - v0) * t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(V, 0.8))), 0.6) * (V < 0.15 ? 0.55 + 3 * V : 1);
			if (au > half) return SpellArt.CLEAR;
			if (half - au < 0.05) return SpellArt.glow(SpellArt.mix(GOLD, GOLD_LIGHT, V), 0.9);
			if (au < 0.03) return SpellArt.glow(SpellArt.mix(GOLD_LIGHT, WHITE_FIRE, V), 0.92);
			double vein = au / Math.max(0.05, half) * 3.5 - V * 0.8 + seed * 0.1;
			if (Math.abs(vein - Math.rint(vein)) < 0.09) return SpellArt.glow(SpellArt.mix(AMBER, GOLD_LIGHT, V), 0.8);
			int c = SpellArt.ramp(V + (SpellArt.noise(u * 6 + seed, V * 5, seed) - 0.5) * 0.2, EMBER, CRIMSON, VERMILION, FLAME, GOLD);
			return SpellArt.glow(c, 0.74 + 0.2 * V);
		};
	}

	/**
	 * A ring of fire script on a sheet: two borders of gold flame round a band of seal-script glyphs ({@code cells} of them, each its own);
	 * nothing else ({@code inner}, {@code outer}: the band's edges, fractions of the sheet's half). No haze between the glyphs: in game a texel
	 * seen through (but not clear) still hides what is drawn after it, so a faint band would cut the other rings where it crosses them.
	 */
	static SpellArt.Painter scriptRing(double inner, double outer, int cells, int seed) {
		return t -> {
			double x = t.across() - 0.5, z = t.back() - 0.5, r = Math.hypot(x, z) * 2;
			if (Math.abs(r - inner) < 0.016 || Math.abs(r - outer) < 0.016) return SpellArt.glow(GOLD_LIGHT, 0.96);
			if (r < inner || r > outer) return SpellArt.CLEAR;
			double ang = (Math.atan2(z, x) / (2 * Math.PI) + 0.5) * cells;
			int cell = Math.floorMod((int) Math.floor(ang), cells);
			if (glyph(cell, seed, ang - Math.floor(ang), (r - inner) / (outer - inner))) return SpellArt.glow(SpellArt.mix(GOLD, WHITE_FIRE, 0.45), 0.97);
			return SpellArt.CLEAR;
		};
	}

	/** Glyph strokes on a 3 x 3 lattice: rows, columns, two slants. */
	static final double[][] STROKES = {{0, 0, 1, 0}, {1, 0, 2, 0}, {0, 1, 1, 1}, {1, 1, 2, 1}, {0, 2, 1, 2}, {1, 2, 2, 2}, {0, 0, 0, 1}, {0, 1, 0, 2}, {1, 0, 1, 1},
			{1, 1, 1, 2}, {2, 0, 2, 1}, {2, 1, 2, 2}, {0, 0, 1, 1}, {2, 0, 1, 1}};

	/**
	 * Whether ({@code a}, {@code b}) in [0, 1] lies on a stroke of glyph {@code cell}: its spine down the middle and some of the rest. A cell
	 * whose strokes would make a swastika (2026-10-24, one came up on the Gate of Ten Thousand Ghosts' pillar) is drawn as another.
	 */
	static boolean glyph(int cell, int seed, double a, double b) {
		if (a < 0.16 || a > 0.84 || b < 0.16 || b > 0.84) return false;
		while (swastika(cell, seed)) cell += 7919;
		double x = (a - 0.16) / 0.68 * 2, y = (b - 0.16) / 0.68 * 2;
		for (int i = 0; i < STROKES.length; i++) {
			if (!stroke(cell, seed, i)) continue;
			double[] s = STROKES[i];
			if (segment(x, y, s[0], s[1], s[2], s[3]) < 0.22) return true;
		}
		return false;
	}

	/** Whether glyph {@code cell} has stroke {@code i} ({@link #STROKES}): its spine always, the rest by chance. */
	static boolean stroke(int cell, int seed, int i) {
		return i == 8 || i == 9 || SpellArt.hash(cell * 31 + i, seed, 77) >= 0.56;
	}

	/**
	 * Whether glyph {@code cell}'s strokes come near a swastika: both bars of the cross and three or more of either turning's hooks; or two
	 * opposite arms of the cross turning the same way, an arm turning only where it is hooked one way and not both (2026-10-25: a cross with
	 * its top-left and bottom-right corners boxed in, the hooks of both turnings, still read as one at the heart of King's Decree's seal; a
	 * bar across an arm's end, as the three of 王, turns neither way).
	 */
	static boolean swastika(int cell, int seed) {
		if (!stroke(cell, seed, 2) || !stroke(cell, seed, 3)) return false;
		int[][] hooks = {{1, 11, 4, 6}, {0, 7, 5, 10}};
		for (int[] turning : hooks) {
			int n = 0;
			for (int i : turning) if (stroke(cell, seed, i)) n++;
			if (n >= 3) return true;
		}
		// Each arm's two hooks, top, right, bottom, left: the one turning as the first turning's, and the other.
		int[][] arms = {{1, 0}, {11, 10}, {4, 5}, {6, 7}};
		int[] turns = new int[4];
		for (int k = 0; k < 4; k++) {
			boolean one = stroke(cell, seed, arms[k][0]), other = stroke(cell, seed, arms[k][1]);
			turns[k] = one == other ? 0 : one ? 1 : -1;
		}
		return turns[0] != 0 && turns[0] == turns[2] || turns[1] != 0 && turns[1] == turns[3];
	}

	/** Whether ({@code a}, {@code b}) in [0, 1] lies on one of {@code strokes} ({@link #STROKES}), drawn as a glyph's are: a glyph made to order. */
	static boolean strokes(int[] strokes, double a, double b) {
		if (a < 0.16 || a > 0.84 || b < 0.16 || b > 0.84) return false;
		double x = (a - 0.16) / 0.68 * 2, y = (b - 0.16) / 0.68 * 2;
		for (int i : strokes) {
			double[] s = STROKES[i];
			if (segment(x, y, s[0], s[1], s[2], s[3]) < 0.22) return true;
		}
		return false;
	}

	/** How far ({@code x}, {@code y}) lies from the segment ({@code x0}, {@code y0}) to ({@code x1}, {@code y1}). */
	static double segment(double x, double y, double x0, double y0, double x1, double y1) {
		double dx = x1 - x0, dy = y1 - y0, l = dx * dx + dy * dy;
		double k = l == 0 ? 0 : SpellArt.clamp01(((x - x0) * dx + (y - y0) * dy) / l);
		return Math.hypot(x - x0 - k * dx, y - y0 - k * dy);
	}

	static final double PETAL_FOOT = -4.6;
	/** The bud's two rings of petals: how many, how far out their feet, their two sections' lengths, their width, how far each leans (out, in). */
	static final double[][] PETAL_RINGS = {{5, 0.6, 5.4, 5.6, 5.2, 18, -20}, {8, 1.8, 7, 7.4, 6.8, 28, -24}};
	/** The rings of script: radius, tilt, turn. */
	static final double[][] RINGS = {{11, 0, 0}, {9.6, 62, 30}, {9.6, 62, 150}};

	/**
	 * The seed of One Flame Burns the Heavens: a pearl of white fire ("pearl", in its gold halo) in a lotus bud of flame ("bud": two rings of
	 * petals, "petal_<n>", each turned to its place, opening on "petal_<n>_open" at its foot and "petal_<n>_tip" at its middle), ringed by three
	 * rings of talisman script ("ring_0".."ring_2", each tilted, spinning on its "_spin").
	 */
	static void oneFlameSeed(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone heart = m.bone("pearl", "root", 0, 0, 0);
		SpellArt.Painter white = pearl(5);
		heart.box(0, 0, 0, 5.4, 5.4, 5.4, white);
		heart.box(0, 0, 0, 5.4, 5.4, 5.4, white).rot(45, 45, 0);
		heart.box(0, 0, 0, 5.4, 5.4, 5.4, white).rot(0, 45, 45);
		heart.box(0, 0, 0, 8.2, 8.2, 8.2, halo()).rot(0, 45, 0);
		m.bone("bud", "root", 0, 0, 0);
		int n = 0;
		for (int ring = 0; ring < PETAL_RINGS.length; ring++) {
			double[] p = PETAL_RINGS[ring];
			double share = p[2] / (p[2] + p[3]);
			for (int k = 0; k < p[0]; k++, n++) {
				String name = "petal_" + n;
				m.bone(name, "bud", 0, PETAL_FOOT, 0, 0, 360.0 * k / p[0] + ring * 22.5, 0);
				// Built at +x, its face outward: the foot leaning out, the tip leaning back in over the pearl.
				double[] foot = SpellArt.v3(p[1], PETAL_FOOT, 0), knee = SpellArt.add(foot, SpellArt.v3(Math.sin(Math.toRadians(p[5])) * p[2], Math.cos(Math.toRadians(p[5])) * p[2], 0));
				SpellArt.Bone open = m.bone(name + "_open", name, foot[0], foot[1], foot[2]);
				SpellArt.shared(m, open, "petal_" + ring + "_foot", false, foot, p[2] + 0.3, p[4], 0, 0, p[5], petal(0, share, 120 + ring), 3, false);
				SpellArt.Bone tip = m.bone(name + "_tip", name + "_open", knee[0], knee[1], knee[2]);
				SpellArt.shared(m, tip, "petal_" + ring + "_tip", false, knee, p[3], p[4], 0, 0, p[6], petal(share, 1, 120 + ring), 3, false);
			}
		}
		for (int k = 0; k < RINGS.length; k++) {
			double[] r = RINGS[k];
			m.bone("ring_" + k, "root", 0, 0, 0, r[1], r[2], 0);
			SpellArt.Bone spin = m.bone("ring_" + k + "_spin", "ring_" + k, 0, 0, 0);
			SpellArt.sheet(spin, 0, 0, 0, r[0] * 2, r[0] * 2, 0, 0, 0, scriptRing(0.79, 0.95, 16 - k * 2, 130 + k)).density(6);
		}
		seedSpin(m, n);
		bloom(m, n);
	}

	/** The spin: the bud turning one way and the pearl the other, pulsing; the rings wheeling each its own way; the petals breathing. */
	static void seedSpin(SpellArt.Model m, int petals) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("spin", T, "true");
		for (int i = 0; i <= 8; i++) {
			double tt = T * i / 8, f = i / 8.0;
			a.rot("bud", tt, 0, -360 * f, 0);
			a.rot("pearl", tt, 0, 360 * f, 0);
			a.scale("pearl", tt, 1 + 0.1 * Math.sin(4 * Math.PI * f));
			a.rot("ring_0_spin", tt, 0, 360 * f, 0);
			a.rot("ring_1_spin", tt, 0, -360 * f, 0);
			a.rot("ring_2_spin", tt, 0, 720 * f, 0);
			a.pos("root", tt, 0, 0.8 * Math.sin(2 * Math.PI * f), 0);
		}
		for (int k = 0; k < petals; k++) {
			int kk = k;
			a.sample("petal_" + k + "_open", "rotation", 0, T, 8, t -> new double[] {0, 0, 4 + 3 * Math.sin(2 * Math.PI * t / T + kk)});
		}
	}

	/** The bloom: the petals fling open, the pearl flares, the rings swell out and are gone. Held at its end. */
	static void bloom(SpellArt.Model m, int petals) {
		SpellArt.Anim a = m.anim("bloom", 0.9, "hold_on_last_frame");
		double[] t = {0, 0.15, 0.3, 0.45, 0.6, 0.75, 0.9};
		int outerFrom = (int) PETAL_RINGS[0][0];
		for (double tt : t) {
			double e = SpellArt.easeOut(tt / 0.6), over = tt > 0.3 && tt < 0.9 ? 1 + 0.1 * Math.sin((tt - 0.3) / 0.6 * Math.PI) : 1;
			for (int k = 0; k < petals; k++) {
				boolean outer = k >= outerFrom;
				a.rot("petal_" + k + "_open", tt, 0, 0, (outer ? 40 : 28) * e * over);
				a.rot("petal_" + k + "_tip", tt, 0, 0, (outer ? 30 : 22) * e * over);
			}
			a.scale("pearl", tt, 1 + 0.8 * e * over);
			double ring = tt >= 0.9 ? 0.001 : tt >= 0.75 ? 2.9 : 1 + 1.7 * e;
			for (int k = 0; k < RINGS.length; k++) a.scale("ring_" + k, tt, ring);
		}
	}

	// --- One Flame Burns the Heavens: the pillar ---

	/** The pillar's heart: white fire streaming up it, streaked gold, seen through a little. */
	static SpellArt.Painter coreFire(int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2) / (2 * Math.PI);
			double s = SpellArt.fbm(ang * 10 + seed, t.ly * 0.04, seed, 3);
			int c = SpellArt.mix(GOLD_LIGHT, WHITE_FIRE, s * 1.4 - 0.2);
			if (s < 0.38) c = SpellArt.mix(c, AMBER, (0.38 - s) * 3);
			return SpellArt.glow(c, 0.62 + 0.3 * s);
		};
	}

	/** The ground beneath the pillar: glass melting, its crust cracked by veins of fire, white-hot at the heart, cooling and fading outward. */
	static SpellArt.Painter molten(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 1) return SpellArt.CLEAR;
			// Cells of crust (round the nearest of jittered points), their seams the cracks.
			double cx = x * 7, cz = z * 7, d1 = 9, d2 = 9;
			int ix = (int) Math.floor(cx), iz = (int) Math.floor(cz);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					double d = Math.hypot(cx - (ix + dx + SpellArt.hash(ix + dx, iz + dz, seed)), cz - (iz + dz + SpellArt.hash(ix + dx, iz + dz, seed + 1)));
					if (d < d1) {
						d2 = d1;
						d1 = d;
					} else if (d < d2) d2 = d;
				}
			}
			double heat = 1 - r;
			if (d2 - d1 < 0.12 + 0.18 * heat) return SpellArt.glow(SpellArt.ramp(heat, VERMILION, FLAME, GOLD, WHITE_FIRE), 0.95 - 0.5 * r * r);
			if (heat > 0.75) return SpellArt.glow(SpellArt.mix(AMBER, GOLD_LIGHT, (heat - 0.75) * 4), 0.9);
			return SpellArt.col(SpellArt.mix(0x1A0A08, 0x4A1A10, SpellArt.noise(cx * 2, cz * 2, seed + 3) * 0.6 + heat * 0.5), Math.min(1, 1.6 * (1 - r)));
		};
	}

	static final double PILLAR_HEIGHT = 360;
	/**
	 * The pillar's walls of flame, inside out: each a ring of tongues ({@code count, radius, step between tiers, tongue height, width, tiers,
	 * licks}) stacked in tiers up the pillar, each tier set half a tongue round from the one beneath.
	 */
	static final double[][] WALLS = {{8, 11, 24, 40, 13, 15, 2.2}, {12, 20, 30, 48, 15, 12, 2.6}, {16, 30, 40, 46, 13, 9, 3.0}};
	static final int CROWN = 12, SWIRL = 14;

	/**
	 * The pillar of One Flame Burns the Heavens, ninety blocks of fire: a core of white fire ("core"); three walls of tongues of flame round it
	 * (tiers "wall_<w>_<i>", rising and wheeling as they burn), the innermost hottest; the molten glass beneath ("ground") and a swirl of flame
	 * over it ("swirl", its tongues "swirl_<n>" leaning out); and the crown of fire it spreads into at the top ("crown", "crown_<n>").
	 */
	static void oneFlamePillar(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 0, 0);
		core.box(0, PILLAR_HEIGHT / 2, 0, 13, PILLAR_HEIGHT, 13, coreFire(3)).skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		core.box(0, PILLAR_HEIGHT / 2, 0, 11, PILLAR_HEIGHT, 11, coreFire(4)).rot(0, 45, 0).skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0, 0);
		SpellArt.sheet(ground, 0, 0.3, 0, 84, 84, 0, 0, 0, molten(9)).density(1.5);
		for (int w = 0; w < WALLS.length; w++) {
			double[] wall = WALLS[w];
			int count = (int) wall[0];
			// Its tiers, and a foot that stays (sunk a little into the ground), so the lowest tier rising never leaves the ground bare.
			for (int i = -1; i < wall[5]; i++) {
				double y = i < 0 ? -wall[3] * 0.25 : i * wall[2];
				SpellArt.Bone tier = m.bone("wall_" + w + "_" + (i < 0 ? "foot" : i), "root", 0, y, 0);
				for (int k = 0; k < count; k++) {
					double th = 360.0 * (k + 0.5 * i) / count;
					double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * wall[1], y, Math.cos(Math.toRadians(th)) * wall[1]);
					// Each wall's tongues lean along it, the walls by turns either way (so, wheeling, they spiral).
					SpellArt.shared(m, tier, "wall_" + w, false, base, wall[3], wall[4], w % 2 == 0 ? 10 : -10, th - 90, 0, flame(wall[6], 150 + w), 2, false);
				}
			}
		}
		// The swirl over the molten ground: tongues leaning out and round.
		m.bone("swirl", "root", 0, 0, 0);
		for (int k = 0; k < SWIRL; k++) {
			SpellArt.Bone b = m.bone("swirl_" + k, "swirl", 0, 0, 0, 0, 360.0 * k / SWIRL, 0);
			SpellArt.shared(m, b, "swirl", false, SpellArt.v3(24, 0.5, 0), 30, 14, -24, 0, 38, flame(2.4, 170), 2, false);
		}
		// The crown at the top: great tongues flung out and down like a flower of fire.
		m.bone("crown", "root", 0, PILLAR_HEIGHT - 24, 0);
		for (int k = 0; k < CROWN; k++) {
			SpellArt.Bone b = m.bone("crown_" + k, "crown", 0, PILLAR_HEIGHT - 24, 0, 0, 360.0 * k / CROWN + 15, 0);
			SpellArt.shared(m, b, "crown", false, SpellArt.v3(10, PILLAR_HEIGHT - 24, 0), 58, 24, 0, 0, 52, flame(3, 180), 1.6, false);
			SpellArt.shared(m, b, "crown_low", false, SpellArt.v3(16, PILLAR_HEIGHT - 40, 0), 40, 18, 0, 0, 74, flame(2.6, 186), 1.6, false);
		}
		erupt(m);
		burn(m);
	}

	/** The eruption: the pillar bursting up out of the ground, its crown flung open as it reaches the sky. */
	static void erupt(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("erupt", 0.6, null);
		double[] t = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6};
		for (double tt : t) {
			double e = SpellArt.easeOut(tt / 0.45), over = tt > 0.3 ? 1 + 0.06 * Math.sin((tt - 0.3) / 0.3 * Math.PI) : 1;
			a.scale("root", tt, Math.max(0.05, 0.55 + 0.45 * e) * over, Math.max(0.02, e) * over, Math.max(0.05, 0.55 + 0.45 * e) * over);
			a.scale("crown", tt, Math.max(0.001, SpellArt.easeOut((tt - 0.3) / 0.3)));
		}
	}

	/**
	 * The burning, looped without a seam: each tier of each wall rises one step and turns half a tongue round in the loop (so at its end each
	 * stands where the next began; the lowest grows out of nothing, the highest dwindles away), its width breathing by its height; the core
	 * flickering; the swirl and the crown wheeling one tongue round.
	 */
	static void burn(SpellArt.Model m) {
		double T = 0.6;
		SpellArt.Anim a = m.anim("burn", T, "true");
		int steps = 6;
		for (int w = 0; w < WALLS.length; w++) {
			double[] wall = WALLS[w];
			int tiers = (int) wall[5];
			double turn = (w % 2 == 0 ? 1 : -1) * 180.0 / wall[0];
			for (int i = 0; i < tiers; i++) {
				int ii = i;
				String tier = "wall_" + w + "_" + i;
				a.sample(tier, "position", 0, T, 1, t -> new double[] {0, wall[2] * t / T, 0});
				a.sample(tier, "rotation", 0, T, 1, t -> new double[] {0, turn * t / T, 0});
				a.sample(tier, "scale", 0, T, steps, t -> {
					double f = t / T, breath = 1 + 0.06 * Math.sin(2 * Math.PI * ((ii + f) * 0.31 + f));
					// The lowest grows quickly, inside the foot's flames; the highest dwindles late, under the crown.
					double grow = Math.max(0.001, ii == 0 ? SpellArt.easeOut(f / 0.35) : ii == tiers - 1 ? 1 - SpellArt.easeIn((f - 0.6) / 0.4) : 1);
					return new double[] {breath * grow, grow, breath * grow};
				});
			}
			a.sample("wall_" + w + "_foot", "scale", 0, T, steps, t -> {
				double breath = 1 + 0.06 * Math.sin(2 * Math.PI * t / T), lick = 1 + 0.12 * Math.sin(4 * Math.PI * t / T + 1);
				return new double[] {breath, lick, breath};
			});
		}
		a.sample("core", "scale", 0, T, steps, t -> {
			double s = 1 + 0.07 * Math.sin(4 * Math.PI * t / T);
			return new double[] {s, 1, s};
		});
		a.sample("swirl", "rotation", 0, T, 1, t -> new double[] {0, -360.0 / SWIRL * t / T, 0});
		a.sample("crown", "rotation", 0, T, 1, t -> new double[] {0, 360.0 / CROWN * t / T, 0});
		a.sample("crown", "scale", 0, T, steps, t -> {
			double s = 1 + 0.05 * Math.sin(2 * Math.PI * t / T);
			return new double[] {s, 1, s};
		});
	}
}
