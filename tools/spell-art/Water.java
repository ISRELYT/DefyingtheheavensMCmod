/**
 * The water element's Dao Integration spells for SpellArt: the Black Tortoise of the north, its shell a dome of warding light round its
 * caster and its serpent coiled about it (Black Tortoise Bastion); and Hundred Rivers Converge, a maelstrom drawing in rivers from the sky.
 */
public class Water {
	// --- The water element's colours: the abyss, the black of the north, navy, the deep, teal, cyan light, ice, foam ---
	static final int ABYSS = 0x060A14, BLACK_SHELL = 0x0E1828, NAVY = 0x172C48, DEEP = 0x1C426C, TEAL = 0x1F7A8C, CYAN = 0x58DCEC, ICE = 0xD8F8FF, FOAM = 0xF2FCFF;
	static final int SERPENT_DARK = 0x0A1818, SERPENT = 0x1C4842, SERPENT_LIGHT = 0x3A8676, SERPENT_BELLY = 0x9CD8C6;

	// --- The Black Tortoise: painters ---

	/**
	 * The shell's light (a dome of panels): the seams between its scutes (cells round the seeds given, on the dome) glowing cyan, each scute's
	 * rings of growth fainter within it, its rim a band of light; between them nothing (the caster sees out, and nothing behind is hidden).
	 */
	static SpellArt.Painter shellLight(double[][] seeds, double rimY) {
		return t -> {
			double d1 = 1e9, d2 = 1e9;
			for (double[] s : seeds) {
				double d = Math.sqrt((t.px - s[0]) * (t.px - s[0]) + (t.py - s[1]) * (t.py - s[1]) + (t.pz - s[2]) * (t.pz - s[2]));
				if (d < d1) {
					d2 = d1;
					d1 = d;
				} else if (d < d2) d2 = d;
			}
			if (t.py < rimY + 1.4) return SpellArt.glow(SpellArt.mix(CYAN, ICE, 0.3), 0.92);
			if (d2 - d1 < 0.95) return SpellArt.glow(SpellArt.mix(CYAN, ICE, 0.45), 0.9);
			double ring = d1 / 3.1;
			if (d1 > 2.2 && ring - Math.floor(ring) < 0.14) return SpellArt.glow(SpellArt.mix(TEAL, CYAN, 0.5), 0.5);
			return SpellArt.CLEAR;
		};
	}

	/** The tortoise's hide: rough small scales of the black of the north, seams of cyan light between some of them. */
	static SpellArt.Painter tortoiseHide(int seed) {
		return t -> {
			double cx = (t.px + t.pz * 0.7) / 1.6, cy = (t.py + t.pz * 0.4) / 1.4;
			int ix = (int) Math.floor(cx), iy = (int) Math.floor(cy);
			double d1 = 9, d2 = 9;
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					double d = Math.hypot(cx - (ix + dx + SpellArt.hash(ix + dx, iy + dy, seed)), cy - (iy + dy + SpellArt.hash(ix + dx, iy + dy, seed + 1)));
					if (d < d1) {
						d2 = d1;
						d1 = d;
					} else if (d < d2) d2 = d;
				}
			}
			int c = SpellArt.mix(BLACK_SHELL, NAVY, 0.3 + 0.5 * SpellArt.hash(ix, iy, seed + 2) + (t.ny > 0.5 ? 0.2 : 0));
			if (d2 - d1 < 0.1) return SpellArt.hash(ix, iy, seed + 3) > 0.6 ? SpellArt.glow(TEAL, 0.9) : SpellArt.col(ABYSS);
			return SpellArt.col(SpellArt.shade(c, 0.18 * (1 - d1)));
		};
	}

	/** A claw or a beak's edge: dark horn, glowing cyan toward its point (the top of its cube). */
	static SpellArt.Painter horn() {
		return t -> t.up() > 0.7 ? SpellArt.glow(SpellArt.mix(CYAN, ICE, (t.up() - 0.7) * 3)) : SpellArt.col(SpellArt.mix(ABYSS, NAVY, t.up()));
	}

	/** An eye of the north: ice-white light ringed cyan, on the outer side of its cube; dark round it. */
	static SpellArt.Painter eye() {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return SpellArt.col(ABYSS);
			double x = t.back() - 0.5, y = t.up() - 0.5, r = Math.hypot(x, y * 1.15);
			if (r > 0.46) return SpellArt.col(ABYSS);
			if (Math.abs(y) < 0.06 && Math.abs(x) < 0.3) return SpellArt.col(0x020408);
			return SpellArt.glow(SpellArt.mix(ICE, CYAN, r * 2.1));
		};
	}

	/**
	 * The serpent's scales round a limb's own axis: diamonds of dark teal, their rims lit, a seam of cyan light along its back; broad pale
	 * scutes along its belly (where a face looks down).
	 */
	static SpellArt.Painter serpentScales(int seed) {
		return t -> {
			if (t.ny < -0.45) {
				double band = t.ly / 1.3 - Math.floor(t.ly / 1.3);
				return SpellArt.col(band < 0.2 ? SpellArt.shade(SERPENT_BELLY, -0.4) : SpellArt.shade(SERPENT_BELLY, -0.12 * band));
			}
			if (t.ny > 0.7 && Math.abs(t.across() - 0.5) < 0.1) return SpellArt.glow(SpellArt.mix(CYAN, ICE, 0.2), 0.9);
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2) / (2 * Math.PI) * 10, along = t.ly / 1.1;
			double a = ang + along, b = ang - along;
			double fa = a - Math.floor(a), fb = b - Math.floor(b);
			int c = SpellArt.mix(SERPENT_DARK, SERPENT, 0.4 + 0.5 * SpellArt.hash((int) Math.floor(a), (int) Math.floor(b), seed));
			if (fa < 0.12 || fb < 0.12) return SpellArt.col(SpellArt.mix(c, SERPENT_LIGHT, 0.6));
			return SpellArt.col(c);
		};
	}

	// --- The Black Tortoise ---

	static final double DOME_XZ = 32, DOME_Y = 22, DOME_FOOT = 6;
	/** The dome's bands: from and to elevation (degrees), and how many panels round. */
	static final double[][] BANDS = {{0, 22, 16}, {22, 44, 14}, {44, 64, 11}, {64, 80, 7}};

	/** A point on the dome, at a turn {@code th} round it from its front-back line (degrees, 0 toward +z) and an elevation {@code ph}. */
	static double[] onDome(double th, double ph) {
		double t = Math.toRadians(th), p = Math.toRadians(ph);
		return SpellArt.v3(DOME_XZ * Math.cos(p) * Math.sin(t), DOME_FOOT + DOME_Y * Math.sin(p), DOME_XZ * Math.cos(p) * Math.cos(t));
	}

	/** The scutes' seeds: five down the middle, four costals each side, twenty-two marginals round the rim. */
	static double[][] scutes() {
		java.util.List<double[]> seeds = new java.util.ArrayList<>();
		seeds.add(onDome(180, 46));
		seeds.add(onDome(180, 70));
		seeds.add(onDome(0, 90));
		seeds.add(onDome(0, 70));
		seeds.add(onDome(0, 46));
		for (double th : new double[] {32, 72, 108, 148}) {
			seeds.add(onDome(th, 34));
			seeds.add(onDome(-th, 34));
		}
		for (int k = 0; k < 22; k++) seeds.add(onDome(k * 360.0 / 22 + 8, 10));
		return seeds.toArray(new double[0][]);
	}

	// --- The Black Tortoise, made of water ---

	/**
	 * Black water (a solid, painted where it lies in the model): the deep of the north, near black, moving in slow currents (streaks drawn
	 * out along {@code flow}: 0 along x, 1 along y, 2 along z), lit by cyan where they run fast and capped with foam along their crests;
	 * seen through, its light glowing.
	 */
	static SpellArt.Painter blackWater(int flow, int seed) {
		return t -> {
			double sx = flow == 0 ? 0.035 : 0.14, sy = flow == 1 ? 0.035 : 0.14, sz = flow == 2 ? 0.035 : 0.14;
			double n = SpellArt.fbm3(t.px * sx, t.py * sy, t.pz * sz, seed, 2);
			double current = 1 - Math.abs(2 * SpellArt.fbm3(t.px * sx * 1.8 + n, t.py * sy * 1.8, t.pz * sz * 1.8 + n, seed + 7, 2) - 1);
			if (current > 0.93) return SpellArt.glow(SpellArt.mix(CYAN, FOAM, (current - 0.93) * 12), 0.86);
			if (current > 0.85) return SpellArt.glow(SpellArt.mix(TEAL, CYAN, (current - 0.85) * 10), 0.62);
			int c = SpellArt.ramp(SpellArt.clamp01(n * 0.85 + (t.ny > 0 ? 0.18 * t.ny : 0) - 0.05), ABYSS, BLACK_SHELL, NAVY, DEEP, TEAL);
			return SpellArt.col(c, 0.76);
		};
	}

	/**
	 * A stream (on a limb, flowing along its cube's length): dark water running along it, a few long threads of cyan light where it runs
	 * fast, foam breaking white along its rim (where a face meets the next) here and there; seen through.
	 */
	static SpellArt.Painter stream(int seed) {
		return t -> {
			double across = t.fx, rim = Math.min(across, 1 - across);
			double n = SpellArt.fbm(across * 1.6 + seed, t.ly * 0.06, seed, 2), run = 1 - Math.abs(2 * SpellArt.fbm(across * 2.4 + n * 1.5 + seed, t.ly * 0.1, seed + 3, 2) - 1);
			if (rim < 0.07 && SpellArt.noise(t.ly * 0.25, across * 2, seed + 5) > 0.62) return SpellArt.glow(SpellArt.mix(ICE, FOAM, 0.5), 0.82);
			if (run > 0.93) return SpellArt.glow(SpellArt.mix(CYAN, ICE, (run - 0.93) * 12), 0.86);
			if (run > 0.84) return SpellArt.glow(SpellArt.mix(TEAL, CYAN, (run - 0.84) * 10), 0.62);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(n * 0.85 + 0.1 * Math.max(0, t.ny)), ABYSS, BLACK_SHELL, NAVY, DEEP, TEAL), 0.76);
		};
	}

	/** Ice (a beak's edge, a claw, a fang): clear, its point (the top of its cube) and edges glowing white-cyan; seen through. */
	static SpellArt.Painter ice() {
		return t -> {
			double edge = Math.min(Math.min(t.fx, 1 - t.fx), Math.min(t.fy, 1 - t.fy));
			if (t.up() > 0.7 || edge < 0.12) return SpellArt.glow(SpellArt.mix(ICE, FOAM, t.up() * 0.6), 0.9);
			return SpellArt.glow(SpellArt.mix(CYAN, ICE, 0.4 + 0.4 * t.up()), 0.6);
		};
	}

	/** An eye of the north on the outer side of its cube: a point of ice-white light in the water ({@code around}), nothing else. */
	static SpellArt.Painter iceEye(SpellArt.Painter around) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return around.paint(t);
			double r = Math.hypot(t.back() - 0.5, (t.up() - 0.5) * 1.2);
			if (r > 0.42) return around.paint(t);
			return SpellArt.glow(SpellArt.mix(FOAM, CYAN, r * 2.2));
		};
	}

	/**
	 * The Black Tortoise, beast of the north, made of water (the user, 2026-10-24: the great spells' beasts made of their elements, not
	 * looking like animals): its shell the dome of warding light it was ("shell", its scutes' seams and rings of light, clear between, so its
	 * caster stands within and sees out); beneath its rim the tortoise of black water, its head ("head", its "neck") with points of ice light
	 * for eyes and a beak of ice, its legs ("leg_fl".."leg_br") clawed with ice, its tail; its serpent a stream of the same water, coiled round
	 * its foot ("coil") and rising over it ("serpent_0".."serpent_7"), its head ("serpent_head", the jaw "serpent_jaw", its tongue "tongue")
	 * poised over the tortoise's, fanged with ice. Its bones the beast's ({@link #blackTortoiseBeast}), so it rises, guards and strikes as
	 * that did. It faces -z; the ground at y 0.
	 */
	static void blackTortoise(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone shell = m.bone("shell", "root", 0, DOME_FOOT, 0);
		SpellArt.Painter light = shellLight(scutes(), DOME_FOOT);
		for (double[] band : BANDS) {
			double ph = (band[0] + band[1]) / 2, dph = Math.toRadians(band[1] - band[0]), dth = Math.toRadians(360 / band[2]);
			double p = Math.toRadians(ph), normal = Math.toDegrees(Math.atan2(Math.sin(p) / DOME_Y, Math.cos(p) / DOME_XZ));
			double w = DOME_XZ * Math.cos(p) * dth * 1.07, d = dph * Math.hypot(DOME_XZ * Math.sin(p), DOME_Y * Math.cos(p)) * 1.07;
			for (int k = 0; k < band[2]; k++) {
				double th = 360 * k / band[2] + (band[0] / 22) * 9;
				double[] at = onDome(th, ph);
				SpellArt.sheet(shell, at[0], at[1], at[2], w, d, normal - 90, th, 0, light).density(1.6);
			}
		}
		double[] cap = onDome(0, 90);
		SpellArt.sheet(shell, cap[0], cap[1] - 0.2, cap[2], DOME_XZ * 0.62, DOME_XZ * 0.62, 0, 0, 0, light).density(1.6);
		// The tortoise of black water beneath: its neck and head out at the front, its legs at the four quarters, its tail behind.
		SpellArt.Bone neck = m.bone("neck", "root", 0, 7, -27);
		SpellArt.limb(neck, SpellArt.v3(0, 6, -24), SpellArt.v3(0, 9, -38), 8.4, 7.4, stream(7));
		SpellArt.Bone head = m.bone("head", "neck", 0, 9.4, -38);
		SpellArt.Painter water = blackWater(2, 8);
		head.box(0, 10, -42.6, 9.4, 7.4, 9.4, water);
		head.box(0, 9.4, -47.8, 7.6, 5.6, 3.6, water);
		SpellArt.limb(head, SpellArt.v3(0, 8.6, -49.4), SpellArt.v3(0, 6.6, -50.2), 4.4, 3, ice()).density(3);
		head.box(0, 7.2, -46.6, 7.2, 1.4, 6.6, water);
		for (int s = -1; s <= 1; s += 2) head.box(s * 4.6, 11.4, -45, 0.7, 2.4, 3, iceEye(water)).density(4);
		String[] legs = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[] quarters = {-132, 132, -48, 48};
		for (int k = 0; k < 4; k++) {
			double th = Math.toRadians(quarters[k]);
			double sx = Math.sin(th), sz = Math.cos(th);
			double[] hip = SpellArt.v3(sx * 24, 9, sz * 24), knee = SpellArt.v3(sx * 34, 7, sz * 34), foot = SpellArt.v3(sx * 37, 1.2, sz * 37);
			SpellArt.Bone leg = m.bone(legs[k], "root", hip[0], hip[1], hip[2]);
			SpellArt.limb(leg, hip, knee, 8, 7, stream(20 + k));
			SpellArt.limb(leg, knee, foot, 6.6, 6.6, stream(30 + k));
			leg.box(foot[0], 1.2, foot[2], 7.6, 2.4, 7.6, blackWater(1, 40 + k)).rot(0, quarters[k], 0);
			for (int j = -1; j <= 1; j++) {
				double side = Math.toRadians(quarters[k] + j * 22);
				double[] base = SpellArt.v3(foot[0] + Math.sin(side) * 3.6, 1.4, foot[2] + Math.cos(side) * 3.6);
				SpellArt.limb(leg, base, SpellArt.add(base, SpellArt.v3(Math.sin(side) * 2, -1.2, Math.cos(side) * 2)), 1, 1, ice()).density(4);
			}
		}
		SpellArt.Bone tail = m.bone("tail", "root", 0, 7, 28);
		SpellArt.limb(tail, SpellArt.v3(0, 7, 27), SpellArt.v3(0, 3.6, 38), 4.4, 3.6, stream(50));
		serpentOfWater(m);
		rise(m);
		guard(m);
		strike(m);
	}

	static void serpentOfWater(SpellArt.Model m) {
		SpellArt.Bone coil = m.bone("coil", "root", 0, 0, 0);
		int pieces = 30;
		for (int i = 0; i < pieces; i++) {
			double s0 = i / (double) pieces, s1 = (i + 1) / (double) pieces, thick = 1.8 + 3.4 * SpellArt.smooth(0, 0.35, s0);
			SpellArt.limb(coil, coilAt(s0), SpellArt.add(coilAt(s1), SpellArt.times(SpellArt.sub(coilAt(s1), coilAt(s0)), 0.12)), thick, thick, stream(60 + i % 3));
		}
		// The neck: from the coil's end, up over the shell's back and side to stand over the tortoise's head (a bezier, cut in eight bones).
		double[] p0 = coilAt(1), c1 = SpellArt.v3(p0[0] * 0.9, 34, p0[2] * 0.6), c2 = SpellArt.v3(0, 48, 14), p3 = SpellArt.v3(0, 40, -16);
		String up = "root";
		double[] at = p0;
		for (int i = 0; i < SERPENT_BONES; i++) {
			double s = (i + 1) / (double) SERPENT_BONES, u = 1 - s;
			double[] next = SpellArt.add(SpellArt.add(SpellArt.times(p0, u * u * u), SpellArt.times(c1, 3 * u * u * s)), SpellArt.add(SpellArt.times(c2, 3 * u * s * s), SpellArt.times(p3, s * s * s)));
			SpellArt.Bone b = m.bone("serpent_" + i, up, at[0], at[1], at[2]);
			double thick = 5.2 - i * 0.18;
			SpellArt.limb(b, at, SpellArt.add(next, SpellArt.times(SpellArt.sub(next, at), 0.1)), thick, thick, stream(70 + i % 3));
			up = b.name;
			at = next;
		}
		// The head, a wedge of the same water: its jaw, its fangs of ice, its forked tongue of light, its eyes of ice light.
		SpellArt.Bone head = m.bone("serpent_head", up, at[0], at[1], at[2]);
		SpellArt.Painter water = blackWater(2, 80);
		double hx = at[0], hy = at[1], hz = at[2];
		head.box(hx, hy + 0.6, hz - 3.6, 5.8, 3.4, 8.2, water);
		head.box(hx, hy + 0.4, hz - 8.6, 4.2, 2.6, 2.6, water);
		for (int s = -1; s <= 1; s += 2) {
			head.box(hx + s * 2.95, hy + 1.4, hz - 4.6, 0.5, 1.4, 2, iceEye(water)).density(5);
			SpellArt.limb(head, SpellArt.v3(hx + s * 1.5, hy - 0.9, hz - 8.4), SpellArt.v3(hx + s * 1.5, hy - 3.2, hz - 8.2), 0.6, 0.6, ice()).density(5);
		}
		SpellArt.Bone jaw = m.bone("serpent_jaw", "serpent_head", hx, hy - 0.8, hz - 1);
		jaw.box(hx, hy - 1.6, hz - 4.6, 5, 1.4, 8, water);
		SpellArt.Bone tongue = m.bone("tongue", "serpent_jaw", hx, hy - 1, hz - 8);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(tongue, SpellArt.v3(hx, hy - 1, hz - 8), SpellArt.v3(hx + s * 0.7, hy - 1.2, hz - 12.4), 0.35, 0.35, t -> SpellArt.glow(SpellArt.mix(CYAN, ICE, t.up()), 0.95)).density(6);
		}
	}

	/**
	 * The Black Tortoise, beast of the north: its shell a dome of warding light eight blocks round ("shell", its scutes' seams and growth
	 * rings only, so its caster stands within and sees out), the dark tortoise's head ("head", its "neck"), legs ("leg_fl".."leg_br") and
	 * tail beneath its rim; and its serpent, coiled round its foot ("coil") and rising over it ("serpent_0".."serpent_7"), its head
	 * ("serpent_head", the jaw "serpent_jaw", its tongue "tongue") poised over the tortoise's. It faces -z; the ground at y 0. Stored
	 * (2026-10-24): the spell's tortoise is now made of water, {@link #blackTortoise}; this, the true beast, is kept for later (SpellArt --stored).
	 */
	static void blackTortoiseBeast(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone shell = m.bone("shell", "root", 0, DOME_FOOT, 0);
		SpellArt.Painter light = shellLight(scutes(), DOME_FOOT);
		for (double[] band : BANDS) {
			double ph = (band[0] + band[1]) / 2, dph = Math.toRadians(band[1] - band[0]), dth = Math.toRadians(360 / band[2]);
			double p = Math.toRadians(ph), normal = Math.toDegrees(Math.atan2(Math.sin(p) / DOME_Y, Math.cos(p) / DOME_XZ));
			double w = DOME_XZ * Math.cos(p) * dth * 1.07, d = dph * Math.hypot(DOME_XZ * Math.sin(p), DOME_Y * Math.cos(p)) * 1.07;
			for (int k = 0; k < band[2]; k++) {
				double th = 360 * k / band[2] + (band[0] / 22) * 9;
				double[] at = onDome(th, ph);
				SpellArt.sheet(shell, at[0], at[1], at[2], w, d, normal - 90, th, 0, light).density(1.6);
			}
		}
		double[] cap = onDome(0, 90);
		SpellArt.sheet(shell, cap[0], cap[1] - 0.2, cap[2], DOME_XZ * 0.62, DOME_XZ * 0.62, 0, 0, 0, light).density(1.6);
		// The tortoise beneath: its neck and head out at the front, its legs at the four quarters, its tail behind.
		SpellArt.Painter hide = tortoiseHide(7);
		SpellArt.Bone neck = m.bone("neck", "root", 0, 7, -27);
		SpellArt.limb(neck, SpellArt.v3(0, 6, -24), SpellArt.v3(0, 9, -38), 8.4, 7.4, hide);
		SpellArt.Bone head = m.bone("head", "neck", 0, 9.4, -38);
		head.box(0, 10, -42.6, 9.4, 7.4, 9.4, hide);
		head.box(0, 9.4, -47.8, 7.6, 5.6, 3.6, hide);
		SpellArt.limb(head, SpellArt.v3(0, 8.6, -49.4), SpellArt.v3(0, 6.6, -50.2), 4.4, 3, horn()).density(3);
		head.box(0, 7.2, -46.6, 7.2, 1.4, 6.6, hide);
		for (int s = -1; s <= 1; s += 2) head.box(s * 4.6, 11.4, -45, 0.7, 2.4, 3, eye()).density(4);
		String[] legs = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[] quarters = {-132, 132, -48, 48};
		for (int k = 0; k < 4; k++) {
			double th = Math.toRadians(quarters[k]);
			double sx = Math.sin(th), sz = Math.cos(th);
			double[] hip = SpellArt.v3(sx * 24, 9, sz * 24), knee = SpellArt.v3(sx * 34, 7, sz * 34), foot = SpellArt.v3(sx * 37, 1.2, sz * 37);
			SpellArt.Bone leg = m.bone(legs[k], "root", hip[0], hip[1], hip[2]);
			SpellArt.limb(leg, hip, knee, 8, 7, hide);
			SpellArt.limb(leg, knee, foot, 6.6, 6.6, hide);
			leg.box(foot[0], 1.2, foot[2], 7.6, 2.4, 7.6, hide).rot(0, quarters[k], 0);
			for (int j = -1; j <= 1; j++) {
				double side = Math.toRadians(quarters[k] + j * 22);
				double[] base = SpellArt.v3(foot[0] + Math.sin(side) * 3.6, 1.4, foot[2] + Math.cos(side) * 3.6);
				SpellArt.limb(leg, base, SpellArt.add(base, SpellArt.v3(Math.sin(side) * 2, -1.2, Math.cos(side) * 2)), 1, 1, horn()).density(4);
			}
		}
		SpellArt.Bone tail = m.bone("tail", "root", 0, 7, 28);
		SpellArt.limb(tail, SpellArt.v3(0, 7, 27), SpellArt.v3(0, 3.6, 38), 4.4, 3.6, hide);
		serpent(m);
		rise(m);
		guard(m);
		strike(m);
	}

	static final int SERPENT_BONES = 8;

	/** The serpent's path round the tortoise's foot ({@code s} in [0, 1], tail to neck): a turn and a quarter, rising. */
	static double[] coilAt(double s) {
		double th = Math.toRadians(200 + s * 450), r = 35 - s * 2;
		return SpellArt.v3(Math.sin(th) * r, 2.6 + s * 9, Math.cos(th) * r);
	}

	static void serpent(SpellArt.Model m) {
		SpellArt.Bone coil = m.bone("coil", "root", 0, 0, 0);
		int pieces = 30;
		for (int i = 0; i < pieces; i++) {
			double s0 = i / (double) pieces, s1 = (i + 1) / (double) pieces, thick = 1.8 + 3.4 * SpellArt.smooth(0, 0.35, s0);
			SpellArt.limb(coil, coilAt(s0), SpellArt.add(coilAt(s1), SpellArt.times(SpellArt.sub(coilAt(s1), coilAt(s0)), 0.12)), thick, thick, serpentScales(30 + i % 3));
		}
		// The neck: from the coil's end, up over the shell's back and side to stand over the tortoise's head (a bezier, cut in eight bones).
		double[] p0 = coilAt(1), c1 = SpellArt.v3(p0[0] * 0.9, 34, p0[2] * 0.6), c2 = SpellArt.v3(0, 48, 14), p3 = SpellArt.v3(0, 40, -16);
		String up = "root";
		double[] at = p0;
		for (int i = 0; i < SERPENT_BONES; i++) {
			double s = (i + 1) / (double) SERPENT_BONES, u = 1 - s;
			double[] next = SpellArt.add(SpellArt.add(SpellArt.times(p0, u * u * u), SpellArt.times(c1, 3 * u * u * s)), SpellArt.add(SpellArt.times(c2, 3 * u * s * s), SpellArt.times(p3, s * s * s)));
			SpellArt.Bone b = m.bone("serpent_" + i, up, at[0], at[1], at[2]);
			double thick = 5.2 - i * 0.18;
			SpellArt.limb(b, at, SpellArt.add(next, SpellArt.times(SpellArt.sub(next, at), 0.1)), thick, thick, serpentScales(40 + i % 3));
			up = b.name;
			at = next;
		}
		// The head, a wedge: its jaw, its fangs, its forked tongue, its eyes.
		SpellArt.Bone head = m.bone("serpent_head", up, at[0], at[1], at[2]);
		SpellArt.Painter scales = serpentScales(50);
		double hx = at[0], hy = at[1], hz = at[2];
		head.box(hx, hy + 0.6, hz - 3.6, 5.8, 3.4, 8.2, scales);
		head.box(hx, hy + 0.4, hz - 8.6, 4.2, 2.6, 2.6, scales);
		for (int s = -1; s <= 1; s += 2) {
			head.box(hx + s * 2.95, hy + 1.4, hz - 4.6, 0.5, 1.4, 2, eye()).density(5);
			SpellArt.limb(head, SpellArt.v3(hx + s * 1.5, hy - 0.9, hz - 8.4), SpellArt.v3(hx + s * 1.5, hy - 3.2, hz - 8.2), 0.6, 0.6, Wood.claw(-1)).density(5);
		}
		SpellArt.Bone jaw = m.bone("serpent_jaw", "serpent_head", hx, hy - 0.8, hz - 1);
		jaw.box(hx, hy - 1.6, hz - 4.6, 5, 1.4, 8, scales);
		SpellArt.Bone tongue = m.bone("tongue", "serpent_jaw", hx, hy - 1, hz - 8);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(tongue, SpellArt.v3(hx, hy - 1, hz - 8), SpellArt.v3(hx + s * 0.7, hy - 1.2, hz - 12.4), 0.35, 0.35, t -> SpellArt.glow(SpellArt.mix(CYAN, ICE, t.up()), 0.95)).density(6);
		}
	}

	/** The rising: the shell swelling up out of the ground, the serpent's neck lifting out of its coil. */
	static void rise(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("rise", 1.2, null);
		double[] t = {0, 0.2, 0.45, 0.7, 0.9, 1.2};
		for (double tt : t) {
			double e = SpellArt.easeOut(tt / 0.8), over = tt > 0.45 && tt < 1.2 ? 1 + 0.05 * Math.sin((tt - 0.45) / 0.75 * Math.PI) : 1;
			a.scale("shell", tt, Math.max(0.05, 0.4 + 0.6 * e) * over, Math.max(0.02, e) * over, Math.max(0.05, 0.4 + 0.6 * e) * over);
			a.pos("root", tt, 0, -6 * (1 - e), 0);
			double lift = SpellArt.easeOut((tt - 0.3) / 0.8);
			for (int i = 0; i < SERPENT_BONES; i++) a.rot("serpent_" + i, tt, (i < 3 ? 26 : -10) * (1 - lift), 0, 0);
		}
		a.scale("tongue", 0, 0.001);
		a.scale("tongue", 1.2, 0.001);
	}

	/** The guard (looped): the shell breathing, the serpent swaying over it, looking about, its tongue flicking now and then; the tortoise's head nodding. */
	static void guard(SpellArt.Model m) {
		double T = 3.2;
		SpellArt.Anim a = m.anim("idle", T, "true");
		int steps = 16;
		a.sample("shell", "scale", 0, T, steps, t -> {
			double s = 1 + 0.012 * Math.sin(2 * Math.PI * t / T);
			return new double[] {s, 1 + 0.02 * Math.sin(2 * Math.PI * t / T), s};
		});
		for (int i = 0; i < SERPENT_BONES; i++) {
			int ii = i;
			a.sample("serpent_" + i, "rotation", 0, T, steps, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T - ii * 0.5), 6 * Math.sin(2 * Math.PI * t / T - ii * 0.45), 0});
		}
		a.sample("serpent_head", "rotation", 0, T, steps, t -> new double[] {0, 14 * Math.sin(4 * Math.PI * t / T), 0});
		a.sample("serpent_jaw", "rotation", 0, T, 8, t -> new double[] {4 + 3 * Math.sin(2 * Math.PI * t / T), 0, 0});
		double[] flick = {0, 0.4, 0.45, 0.55, 0.6, 1.6, 1.65, 1.75, 1.8, T};
		double[] out = {0.001, 0.001, 1, 1, 0.001, 0.001, 1, 1, 0.001, 0.001};
		for (int i = 0; i < flick.length; i++) a.scale("tongue", flick[i], out[i]);
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T + 1), 6 * Math.sin(2 * Math.PI * t / T), 0});
	}

	/** The strike: the serpent draws back, then lunges out ahead, its jaws wide, and coils back to its guard. */
	static void strike(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("strike", 0.9, null);
		double[] t = {0, 0.18, 0.3, 0.42, 0.6, 0.9};
		double[] draw = {0, 1, 0, 0, 0, 0}, lunge = {0, 0, 1, 1, 0.4, 0};
		for (int i = 0; i < t.length; i++) {
			// The bend adds up down the chain: a little at each bone.
			for (int b = 0; b < SERPENT_BONES; b++) {
				double rx = -3 * draw[i] + (b < 4 ? 7 : 3) * lunge[i];
				a.rot("serpent_" + b, t[i], rx, 0, 0);
			}
			a.rot("serpent_head", t[i], -6 * lunge[i] + 6 * draw[i], 0, 0);
			a.rot("serpent_jaw", t[i], 4 + 46 * lunge[i] + 10 * draw[i], 0, 0);
			a.scale("tongue", t[i], 0.001);
		}
	}

	// --- Hundred Rivers Converge: painters ---

	/**
	 * Flowing water on a blade (its length the way it flows): deep blue at its heart through teal to cyan, streaked along its flow, foam at its
	 * edges (whitest on its outer edge, {@code u} > 0.5); seen through, its foam and glints glowing.
	 */
	static SpellArt.Painter river(int seed) {
		return t -> {
			double v = t.v(), u = t.u(), len = t.cube.flat ? t.sz() : t.sy();
			double edge = Math.min(u, 1 - u);
			double streak = SpellArt.fbm(u * 6 + seed, v * len * 0.06, seed, 3);
			if (edge < 0.08 + 0.06 * SpellArt.noise(v * len * 0.2, u * 3, seed + 1)) return SpellArt.glow(SpellArt.mix(ICE, FOAM, streak), 0.85);
			if (u > 0.78 && streak > 0.55) return SpellArt.glow(SpellArt.mix(CYAN, FOAM, (streak - 0.55) * 2), 0.8);
			int c = SpellArt.ramp(streak * 0.7 + Math.abs(u - 0.5) * 0.8, NAVY, DEEP, TEAL, CYAN);
			if (Math.abs(streak - 0.5) < 0.03) return SpellArt.glow(SpellArt.mix(c, ICE, 0.6), 0.8);
			return SpellArt.col(c, 0.74);
		};
	}

	/**
	 * The maelstrom's face (a sheet): water swirling in, banded along spirals ({@code arms} of them), deep blue darkening to the eye at its
	 * heart, streaks of foam along the spirals; seen through, fading out at its rim. Alike arm by arm (its noise comes round with each), so
	 * turned one arm round it is as it was: its spin loops without a seam.
	 */
	static SpellArt.Painter swirlFace(int arms, int seed) {
		return t -> {
			double x = t.across() - 0.5, z = t.back() - 0.5, r = Math.hypot(x, z) * 2;
			if (r > 1) return SpellArt.CLEAR;
			double ang = Math.atan2(x, z) / (2 * Math.PI), round = 2 * Math.PI * ang * arms;
			double s = ang * arms - Math.log(Math.max(0.02, r)) * 2.6 + SpellArt.noise(r * 9 + Math.cos(round) * 0.18, Math.sin(round) * 0.18, seed) * 0.25;
			double f = s - Math.floor(s);
			double fade = Math.min(1, (1 - r) * 5), eye = SpellArt.smooth(0.02, 0.2, r);
			if (f < 0.1 && r > 0.12) return SpellArt.glow(SpellArt.mix(CYAN, FOAM, 0.5), 0.6 * fade);
			int c = SpellArt.mix(ABYSS, SpellArt.ramp(f * 0.6 + r * 0.4, NAVY, DEEP, TEAL), eye);
			return SpellArt.col(c, 0.62 * fade);
		};
	}

	/**
	 * A ring of foam on a sheet ({@code at}: its radius as a fraction of the sheet's half), broken in places, alike in each of its {@code fold}
	 * parts round (so turned one part round it is as it was); nothing else.
	 */
	static SpellArt.Painter foamRing(double at, double width, int fold, int seed) {
		return t -> {
			double x = t.across() - 0.5, z = t.back() - 0.5, r = Math.hypot(x, z) * 2, round = Math.atan2(z, x) * fold;
			double off = Math.abs(r - at) / width, n = SpellArt.noise(Math.cos(round) * 0.75 + seed, Math.sin(round) * 0.75 + r * 8, seed);
			if (off > 1 || n < 0.32) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(CYAN, FOAM, 1 - off), 0.5 + 0.4 * (1 - off) * n);
		};
	}

	// --- Hundred Rivers Converge ---

	static final int ARMS = 8, ARM_PIECES = 22, SPOUT_TIERS = 6, SPOUT_BLADES = 10, ARCS = 6, ARC_PIECES = 12;

	/** Where along an arm ({@code s} in [0, 1], from the rim in): a spiral winding in from 96 pixels to 10, rising a little toward the heart. */
	static double[] armAt(double th0, double s) {
		double r = 96 * Math.pow(10 / 96.0, s), th = Math.toRadians(th0 + s * 300);
		return SpellArt.v3(Math.sin(th) * r, 2.4 + 5 * s * s, Math.cos(th) * r);
	}

	/** Where along an arc of river falling in from the sky ({@code s} in [0, 1]): from beyond the rim up through the air and down into the heart. */
	static double[] arcAt(double th0, double s) {
		double r = 112 * (1 - s) + 10 * s, th = Math.toRadians(th0 + s * 70);
		return SpellArt.v3(Math.sin(th) * r, 3 + 46 * Math.sin(Math.PI * Math.pow(s, 0.8)) + 18 * s, Math.cos(th) * r);
	}

	/**
	 * A flat blade of {@code p} from {@code a} to {@code b} (lying flat along the way, tilted up or down it), {@code width} across; and, if
	 * {@code crossed}, a standing one along it too (so it keeps its width seen from any side, as a stream does).
	 */
	static void ribbon(SpellArt.Model m, SpellArt.Bone bone, String name, double[] a, double[] b, double width, SpellArt.Painter p, double tpp, boolean crossed) {
		double[] d = SpellArt.sub(b, a);
		double len = SpellArt.len(d), heading = Math.toDegrees(Math.atan2(d[0], d[2])), pitch = Math.toDegrees(Math.asin(SpellArt.clamp(d[1] / len, -1, 1)));
		SpellArt.shared(m, bone, name, true, a, len * 1.08, width, pitch, heading, 0, p, tpp, false);
		if (crossed) SpellArt.shared(m, bone, name + "_up", false, a, len * 1.08, width * 0.8, pitch - 90, heading, 0, p, tpp, false);
	}

	/**
	 * Hundred Rivers Converge: a maelstrom twenty-four blocks round ("vortex": eight arms of river spiralling in, rings of foam), a spout of
	 * water twisting up from its heart ("spout_0".."spout_5"), and rivers arcing in from the sky to pour into it ("arcs"). Its foot at y 0.
	 */
	static void hundredRivers(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone vortex = m.bone("vortex", "root", 0, 0, 0);
		SpellArt.sheet(vortex, 0, 1, 0, 200, 200, 0, 0, 0, swirlFace(ARMS, 5)).density(0.6);
		for (int k = 0; k < ARMS; k++) {
			double th0 = 360.0 * k / ARMS;
			for (int i = 0; i < ARM_PIECES; i++) {
				double s0 = i / (double) ARM_PIECES, s1 = (i + 1) / (double) ARM_PIECES;
				ribbon(m, vortex, "arm_" + i, armAt(th0, s0), armAt(th0, s1), 16 * (1 - 0.65 * s0), river(10 + i), 1.25, false);
			}
		}
		double[][] rings = {{0.95, 0.03}, {0.62, 0.04}, {0.32, 0.06}};
		for (int k = 0; k < rings.length; k++) SpellArt.sheet(vortex, 0, 1.4 + k * 1.2, 0, 200, 200, 0, k * 40, 0, foamRing(rings[k][0], rings[k][1], ARMS, 20 + k)).density(0.6);
		// The spout: rings of water twisting up from the heart, narrowing to a waist and flaring above it.
		for (int i = 0; i < SPOUT_TIERS; i++) {
			double y = 4 + i * 12, r = 7 + 5 * Math.abs(i - 2.2) / 2.2;
			SpellArt.Bone tier = m.bone("spout_" + i, "root", 0, y, 0);
			for (int k = 0; k < SPOUT_BLADES; k++) {
				double th = 360.0 * (k + 0.5 * i) / SPOUT_BLADES;
				SpellArt.shared(m, tier, "spout", false, SpellArt.v3(Math.sin(Math.toRadians(th)) * r, y, Math.cos(Math.toRadians(th)) * r), 18, 8, 22, th - 90, 0, river(40), 1.6, false);
			}
		}
		// The rivers from the sky.
		SpellArt.Bone arcs = m.bone("arcs", "root", 0, 0, 0);
		for (int k = 0; k < ARCS; k++) {
			double th0 = 360.0 * k / ARCS + 20;
			for (int i = 0; i < ARC_PIECES; i++) {
				double s0 = i / (double) ARC_PIECES, s1 = (i + 1) / (double) ARC_PIECES;
				ribbon(m, arcs, "arc_" + i, arcAt(th0, s0), arcAt(th0, s1), 12 * (1 - 0.4 * s0), river(60 + i), 1.25, true);
			}
		}
		swirl(m);
		burst(m);
	}

	/** The swirl, looped without a seam: the vortex wheels one arm round, the spout's tiers each a blade round (by turns either way), the arcs one arc round. */
	static void swirl(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("spin", T, "true");
		a.sample("vortex", "rotation", 0, T, 2, t -> new double[] {0, -360.0 / ARMS * t / T, 0});
		for (int i = 0; i < SPOUT_TIERS; i++) {
			double turn = (i % 2 == 0 ? -1 : 1) * 360.0 / SPOUT_BLADES;
			a.sample("spout_" + i, "rotation", 0, T, 2, t -> new double[] {0, turn * t / T, 0});
		}
		a.sample("arcs", "rotation", 0, T, 2, t -> new double[] {0, -360.0 / ARCS * t / T, 0});
	}

	/** The burst: the spout bursts up and is gone, the vortex flung out and gone, the arcs fallen. */
	static void burst(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("burst", 0.8, "hold_on_last_frame");
		double[] t = {0, 0.12, 0.25, 0.45, 0.65, 0.8};
		double[] spout = {1, 1.4, 2.2, 2.6, 0.6, 0.001}, out = {1, 1.12, 1.3, 1.5, 0.8, 0.001}, arc = {1, 0.6, 0.001, 0.001, 0.001, 0.001};
		for (int i = 0; i < t.length; i++) {
			for (int k = 0; k < SPOUT_TIERS; k++) {
				a.scale("spout_" + k, t[i], Math.max(0.001, spout[i] * 0.7), spout[i], Math.max(0.001, spout[i] * 0.7));
				a.pos("spout_" + k, t[i], 0, (spout[i] - 1) * k * 10, 0);
			}
			a.scale("vortex", t[i], out[i], 1, out[i]);
			a.scale("arcs", t[i], arc[i]);
		}
	}
}
