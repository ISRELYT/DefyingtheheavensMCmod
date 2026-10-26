/**
 * The looks of the ghost foundations' great spells (GhostSpells, 2026-10-24): shapes of ghost fire and the dead's mist, as the spirit beasts
 * are of their elements (the user's rule: made of what they are cast from, not lifelike). The Procession of Ten Thousand Ghosts' column of
 * the dead, and the Gate of Ten Thousand Ghosts.
 */
public class Ghost {
	// --- The dead's colours: their mist (pale, cold teal in its shadow), ghost fire from deep teal to white, bone, lacquer, the dark beyond ---
	static final int MIST = 0xD2EEF0, MIST_SHADE = 0x80B2BA, MIST_DEEP = 0x34606C, FIRE_DEEP = 0x0E5560, FIRE_TEAL = 0x18A6A8, FIRE_CYAN = 0x56EEE2,
			FIRE_PALE = 0xC4FFF6, FIRE_WHITE = 0xF2FFFD, BONE = 0xE6DEC6, BONE_SHADE = 0xA49C86, BONE_DARK = 0x5E5848, VOID = 0x04070B, LACQUER = 0x121A24,
			LACQUER_LIGHT = 0x2A3848, SLATE = 0x26303A, SLATE_LIGHT = 0x46525E, CINNABAR = 0x5A0E14;
	/** Ghost fire's heat, coolest to hottest. */
	static final int[] GHOST_FIRE = {FIRE_DEEP, FIRE_TEAL, FIRE_CYAN, FIRE_PALE, FIRE_WHITE};

	// --- Painters ---

	/**
	 * The dead's mist made into a robe, a body, a sleeve: pale, flowing in folds down it, teal in its shadow and beneath, seen through a
	 * little; here and there ghost fire seeping out along a seam.
	 */
	static SpellArt.Painter robe(int seed) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.3, t.py * 0.12, t.pz * 0.3, seed, 3);
			// Folds falling down it, wavering.
			double fold = 0.5 + 0.5 * Math.sin(t.px * 1.2 + t.pz * 0.9 + flow * 5 + t.py * 0.15);
			double light = SpellArt.clamp01(0.38 + 0.32 * fold + 0.25 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny) + (flow - 0.5) * 0.4 + 0.12 * t.up());
			return SpellArt.col(SpellArt.ramp(light, MIST_DEEP, MIST_SHADE, MIST, 0xF4FFFF), 0.5 + 0.18 * light + 0.08 * t.up());
		};
	}

	/**
	 * A hood of mist; on the front of its cube the hollow under it: dark, rimmed with ghost fire, two eyes of ghost fire slanting, a mouth
	 * open in a wail.
	 */
	static SpellArt.Painter hood(int seed) {
		SpellArt.Painter cloth = robe(seed);
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return cloth.paint(t);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			double hollow = x * x / 0.6 + (y + 0.1) * (y + 0.1) / 0.8;
			if (hollow > 1.18) return cloth.paint(t);
			if (hollow > 1) return SpellArt.glow(SpellArt.mix(FIRE_TEAL, FIRE_CYAN, (1.18 - hollow) / 0.18), 0.8);
			for (int s = -1; s <= 1; s += 2) {
				double ex = x - s * 0.33, ey = y - 0.16 - s * ex * 0.4, e = ex * ex / 0.034 + ey * ey / 0.011;
				if (e < 1) return SpellArt.glow(SpellArt.mix(FIRE_WHITE, FIRE_CYAN, e), 0.98);
			}
			double my = y + 0.4, mouth = x * x / 0.045 + my * my / 0.085;
			if (mouth < 1) return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_DEEP, mouth), 0.6 + 0.35 * (1 - mouth));
			return SpellArt.col(SpellArt.mix(VOID, MIST_DEEP, hollow * hollow * 0.7));
		};
	}

	/** A wisp of the dead trailing off (on a blade, its root at v 0): mist drawn out to a curling point, fading to nothing; ghost fire along its edges. */
	static SpellArt.Painter wisp(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.14 * Math.sin(v * 5 + seed) * v;
			double half = 0.48 * Math.pow(Math.max(0, 1 - v), 0.75) * (0.84 + 0.16 * SpellArt.noise(v * 8, seed, seed));
			if (Math.abs(uu) > half || v > 0.97) return SpellArt.CLEAR;
			double edge = (half - Math.abs(uu)) / Math.max(0.02, half), flow = SpellArt.fbm(uu * 6 + seed, v * 4, seed + 1, 2);
			if (edge < 0.16) return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_PALE, flow), 0.25 + 0.6 * (1 - v));
			double light = SpellArt.clamp01(0.4 + 0.45 * flow + 0.15 * edge);
			return SpellArt.col(SpellArt.ramp(light, MIST_DEEP, MIST_SHADE, MIST), Math.max(0.04, (0.86 - 0.8 * v) * (0.75 + 0.25 * flow)));
		};
	}

	/** Ghost fire: a tongue of flame in the dead's colours, cold teal at its edges to white at its heart (Fire#flame). */
	static SpellArt.Painter ghostFire(double licks, int seed) {
		return Fire.flame(licks, seed, GHOST_FIRE);
	}

	/** A paper lantern lit with ghost fire: ribs of bone round it, the paper glowing, a glyph of the dead on its front and back; lacquer caps. */
	static SpellArt.Painter lantern(int seed) {
		return t -> {
			if (t.flat()) return SpellArt.col(LACQUER_LIGHT);
			double up = t.up(), rib = up * 6, f = rib - Math.floor(rib);
			if (f < 0.1) return SpellArt.col(BONE_SHADE);
			boolean face = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (face && up > 0.2 && up < 0.8 && Fire.glyph(seed, seed, t.across(), (up - 0.2) / 0.6)) return SpellArt.col(CINNABAR);
			double heart = 1 - Math.abs(up - 0.5) * 1.3 - Math.abs(t.across() - 0.5) * 0.5;
			return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_WHITE, SpellArt.clamp01(heart * 0.9 + 0.1 * SpellArt.noise(t.fx * 4, t.fy * 4, seed))), 0.92);
		};
	}

	/**
	 * A soul-banner hanging from its bar (on a blade hanging down: v 0 at the bar): bone-white silk torn ragged at its foot, a column of the
	 * dead's glyphs down it in cinnabar, a glow of ghost fire along its edges.
	 */
	static SpellArt.Painter banner(int seed) {
		return t -> {
			double u = t.u(), v = t.v();
			double torn = 0.8 + 0.16 * SpellArt.noise(u * 10 + seed, 1, seed);
			if (v > torn) return SpellArt.CLEAR;
			if (Math.abs(u - 0.5) < 0.3 && v > 0.1 && v < 0.74) {
				double cell = (v - 0.1) / 0.64 * 3;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci + seed, seed, (u - 0.2) / 0.6, cell - ci)) return SpellArt.col(CINNABAR);
			}
			double edge = Math.min(Math.min(u, 1 - u), torn - v);
			if (edge < 0.05) return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_PALE, 0.5), 0.6);
			return SpellArt.col(SpellArt.mix(BONE, BONE_SHADE, SpellArt.noise(u * 5 + seed, v * 7, seed + 2) * 0.6), 0.9);
		};
	}

	/** A pole of old bone-white wood. */
	static SpellArt.Painter pole() {
		return t -> SpellArt.col(SpellArt.mix(BONE_SHADE, BONE, 0.3 + 0.5 * SpellArt.noise(t.fx * 3, t.ly * 0.8, 61)));
	}

	/** A bank of the dead's mist (on a sheet): soft clouds thinning to nothing at its edges, seen through. */
	static SpellArt.Painter mistBank(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.sqrt(x * x + z * z * 0.6);
			double cloud = SpellArt.fbm(t.across() * 5 + seed, t.back() * 9, seed, 4);
			double a = SpellArt.clamp01((cloud - 0.4) * 2.4) * SpellArt.clamp01((1 - r) * 2.5);
			if (a < 0.05) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(MIST_SHADE, MIST, cloud), a * 0.46);
		};
	}

	// --- Procession of Ten Thousand Ghosts ---

	/** The procession's dead, from its head back: x, y, z (its middle), size. The first holds up a lantern; two carry soul-banners. */
	static final double[][] GHOSTS = {
			{0, 0.6, 4, 1.25}, {-4, 5.2, 8, 0.7}, {-7, -1, 11, 1.0}, {7, 0, 12, 1.0}, {5, 6.4, 15, 0.7},
			{0, 1.4, 18, 1.05}, {-10.5, 0.4, 20, 0.9}, {10.5, -0.6, 21, 0.95}, {-8, 6, 24, 0.7}, {-5, -1.4, 27, 1.0},
			{5, 1, 28, 1.0}, {8, 5.6, 31, 0.7}, {-10.5, 1.4, 34, 0.9}, {0, -0.6, 35, 1.05}, {10.5, 0, 36, 0.9},
			{-3, 6.8, 39, 0.7}, {-6, 0.6, 42, 0.95}, {6, -1, 43, 0.95}, {4, 6, 46, 0.7}, {0, 1, 49, 0.9},
			{-10.5, -1, 50, 0.85}, {10.5, 1, 51, 0.85},
	};
	/** Which of them carry banners. */
	static final int[] BANNERS = {5, 13};
	/** Will-o'-wisps of ghost fire drifting along the column: x, y, z. */
	static final double[][] WISPS = {{-13, 4, 8}, {12, 5, 16}, {-3, 7, 24}, {14, 3, 30}, {-14, 6, 38}, {4, 6.5, 45}, {-8, 5, 54}, {9, 7, 58}};

	/**
	 * One of the dead ({@code name}, its trailing tail "_tail"): a peaked hood with the hollow beneath it, draped behind; a robe of mist, its
	 * hem torn into wisps streaming back; its wide sleeves reaching ahead (-z), ragged at their ends; and a long wisp of a tail trailing back
	 * and down; leaning into its going.
	 */
	static void ghost(SpellArt.Model m, String name, double x, double y, double z, double s, int seed) {
		SpellArt.Bone b = m.bone(name, "column", x, y, z, 14, 0, 0);
		SpellArt.Painter robe = robe(seed);
		// The hood: the hollow's cube in front, a drape behind it, its peak.
		b.box(x, y + 3.3 * s, z - 0.3 * s, 3.2 * s, 3.4 * s, 3.0 * s, hood(seed)).density(4);
		b.box(x, y + 3.1 * s, z + 1.3 * s, 2.8 * s, 3.2 * s, 1.4 * s, robe);
		b.box(x, y + 5.15 * s, z + 0.3 * s, 2.2 * s, 0.9 * s, 2.4 * s, robe);
		SpellArt.limb(b, SpellArt.v3(x, y + 5.3 * s, z + 0.9 * s), SpellArt.v3(x, y + 6.0 * s, z + 2.4 * s), 1.1 * s, 1.1 * s, robe);
		// Its shoulders and breast, narrowing to its waist.
		b.box(x, y + 1.95 * s, z + 0.1 * s, 4.4 * s, 1.6 * s, 2.6 * s, robe);
		b.box(x, y + 0.5 * s, z + 0.2 * s, 3.6 * s, 1.6 * s, 2.4 * s, robe);
		// The hem: torn into wisps, streaming down and back about it.
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(30 + k * 60), hx = Math.sin(th) * 1.5 * s, hz = Math.cos(th) * 1.1 * s;
			SpellArt.blade(b, SpellArt.v3(x + hx, y - 0.1 * s, z + 0.2 * s + hz), 4.4 * s, 2.2 * s, 162 + (hz > 0 ? -12 : 0), Math.toDegrees(th) * 0.3, -Math.signum(hx) * 8,
					wisp(seed + 10 + k)).density(2.5);
		}
		for (int side = -1; side <= 1; side += 2) {
			double[] shoulder = SpellArt.v3(x + side * 2.1 * s, y + 1.9 * s, z), hand = SpellArt.v3(x + side * 1.5 * s, y + 0.7 * s, z - 4.4 * s);
			SpellArt.limb(b, shoulder, hand, 1.3 * s, 1.4 * s, robe);
			// The sleeve's wide mouth, and its ragged end: wisps hanging from it.
			double[] dir = SpellArt.sub(hand, shoulder), r = SpellArt.aimY(dir[0], dir[1], dir[2]);
			b.box(hand[0], hand[1], hand[2], 2.2 * s, 1.2 * s, 2.0 * s, robe).rot(r[0], r[1], r[2]);
			SpellArt.blade(b, hand, 2.8 * s, 2.0 * s, 160, side * 10, side * 6, wisp(seed + side)).density(3);
		}
		SpellArt.Bone tail = m.bone(name + "_tail", name, x, y - 1.6 * s, z + 0.8 * s);
		SpellArt.flatBlade(tail, SpellArt.v3(x, y - 1.4 * s, z + 0.6 * s), 10 * s, 3.4 * s, -22, 0, 0, wisp(seed + 3)).density(2.5);
		SpellArt.flatBlade(tail, SpellArt.v3(x, y - 1.0 * s, z + 0.6 * s), 8 * s, 2.6 * s, -14, 8, 70, wisp(seed + 4)).density(2.5);
		SpellArt.flatBlade(tail, SpellArt.v3(x, y - 1.0 * s, z + 0.6 * s), 8 * s, 2.6 * s, -14, -8, -70, wisp(seed + 5)).density(2.5);
	}

	/**
	 * The Procession of Ten Thousand Ghosts: a column of the dead flying along the look, its head at the origin, the rest trailing back (+z):
	 * sixteen of them in ragged rows ("ghost_0".."ghost_15", each with its tail), the first holding up a lantern of ghost fire on a pole
	 * ("lantern"), two carrying soul-banners ("banner_3", "banner_9"); will-o'-wisps of ghost fire drifting with them ("wisp_0".."wisp_7"); and
	 * banks of mist they wade through ("mist", drawn last: a texel seen through hides what is drawn after it).
	 */
	static void procession(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		m.bone("column", "root", 0, 0, 0);
		for (int i = 0; i < GHOSTS.length; i++) ghost(m, "ghost_" + i, GHOSTS[i][0], GHOSTS[i][1], GHOSTS[i][2], GHOSTS[i][3], 100 + i * 7);
		// The lead's lantern, held up ahead on a pole, swinging from its end.
		double[] g = GHOSTS[0];
		double s = g[3];
		double[] hand = SpellArt.v3(g[0] + 1.5 * s, g[1] + 0.7 * s, g[2] - 4.4 * s), top = SpellArt.v3(g[0] + 1.2 * s, g[1] + 7.5 * s, g[2] - 7.5 * s);
		SpellArt.limb(m.get("ghost_0"), hand, top, 0.5, 0.5, pole()).density(4);
		SpellArt.Bone lantern = m.bone("lantern", "ghost_0", top[0], top[1], top[2]);
		SpellArt.limb(lantern, top, SpellArt.add(top, SpellArt.v3(0, -1.2, 0)), 0.2, 0.2, t -> SpellArt.col(BONE_DARK)).density(6);
		lantern.box(top[0], top[1] - 3.2, top[2], 2.8, 3.6, 2.8, lantern(5)).density(5);
		lantern.box(top[0], top[1] - 1.25, top[2], 1.8, 0.4, 1.8, lantern(5)).density(5);
		lantern.box(top[0], top[1] - 5.15, top[2], 1.8, 0.4, 1.8, lantern(5)).density(5);
		SpellArt.blade(lantern, SpellArt.v3(top[0], top[1] - 5.4, top[2]), 1.8, 1, 180, 0, 0, t -> SpellArt.col(CINNABAR)).density(5);
		// The soul-banners: a pole up from the bearer's hand, a bar across its top, the banner hanging from it.
		for (int i : BANNERS) {
			double[] b = GHOSTS[i];
			double bs = b[3];
			double[] grip = SpellArt.v3(b[0] - 1.5 * bs, b[1] + 0.7 * bs, b[2] - 4.4 * bs), head = SpellArt.v3(b[0] - 1.5 * bs, b[1] + 11 * bs, b[2] - 3.4 * bs);
			SpellArt.limb(m.get("ghost_" + i), SpellArt.add(grip, SpellArt.v3(0, -1.5, 0.2)), head, 0.5, 0.5, pole()).density(4);
			SpellArt.Bone banner = m.bone("banner_" + i, "ghost_" + i, head[0], head[1], head[2]);
			SpellArt.limb(banner, SpellArt.add(head, SpellArt.v3(-2.4, -0.6, 0)), SpellArt.add(head, SpellArt.v3(2.4, -0.6, 0)), 0.4, 0.4, pole()).density(4);
			SpellArt.blade(banner, SpellArt.add(head, SpellArt.v3(0, -0.6, 0)), 8.5, 4.4, 180, 90, 0, banner(i)).density(3);
		}
		// Will-o'-wisps: tongues of ghost fire, two crossed in each.
		for (int i = 0; i < WISPS.length; i++) {
			double[] w = WISPS[i];
			SpellArt.Bone b = m.bone("wisp_" + i, "column", w[0], w[1], w[2]);
			SpellArt.shared(m, b, "wisp", false, w, 3.4, 2.2, 0, 0, 0, ghostFire(1.6, 211), 4, false);
			SpellArt.shared(m, b, "wisp", false, w, 3.4, 2.2, 0, 90, 0, ghostFire(1.6, 211), 4, false);
		}
		// The mist they wade through: a low bank, and a thinner one about their middles.
		SpellArt.Bone mist = m.bone("mist", "root", 0, 0, 0);
		SpellArt.sheet(mist, 0, -4.6, 30, 34, 66, 0, 0, 0, mistBank(301)).density(1);
		SpellArt.sheet(mist, 0, -0.4, 32, 30, 62, 0, 0, 0, mistBank(302)).density(1);
		march(m);
		pour(m);
	}

	/** The march, looped without a seam: each of the dead bobbing and swaying on its own beat, its tail streaming; the lantern and banners swinging; the wisps flickering. */
	static void march(SpellArt.Model m) {
		double T = 1.0;
		SpellArt.Anim a = m.anim("march", T, "true");
		for (int i = 0; i < GHOSTS.length; i++) {
			double ph = i * 1.37;
			a.sample("ghost_" + i, "position", 0, T, 8, t -> new double[] {0.25 * Math.sin(2 * Math.PI * t / T + ph + 2), 0.55 * Math.sin(2 * Math.PI * t / T + ph), 0});
			a.sample("ghost_" + i, "rotation", 0, T, 8, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T + ph + 0.8), 0, 3.5 * Math.sin(2 * Math.PI * t / T + ph + 2.1)});
			a.sample("ghost_" + i + "_tail", "rotation", 0, T, 8, t -> new double[] {7 * Math.sin(2 * Math.PI * t / T + ph + 1.4), 14 * Math.sin(2 * Math.PI * t / T + ph + 0.4), 0});
		}
		a.sample("lantern", "rotation", 0, T, 8, t -> new double[] {9 * Math.sin(2 * Math.PI * t / T + 0.5), 0, 6 * Math.sin(2 * Math.PI * t / T + 2)});
		for (int i : BANNERS) {
			double ph = i * 0.7;
			a.sample("banner_" + i, "rotation", 0, T, 8, t -> new double[] {7 * Math.sin(2 * Math.PI * t / T + ph), 0, 4 * Math.sin(2 * Math.PI * t / T + ph + 1.3)});
		}
		for (int i = 0; i < WISPS.length; i++) {
			double ph = i * 2.1;
			a.sample("wisp_" + i, "position", 0, T, 8, t -> new double[] {0, 1.1 * Math.sin(2 * Math.PI * t / T + ph), 0});
			a.sample("wisp_" + i, "scale", 0, T, 8, t -> new double[] {1 + 0.08 * Math.sin(4 * Math.PI * t / T + ph), 0.85 + 0.2 * Math.sin(4 * Math.PI * t / T + ph + 1), 1});
		}
		a.sample("mist", "scale", 0, T, 8, t -> new double[] {1 + 0.04 * Math.sin(2 * Math.PI * t / T), 1, 1});
	}

	/** The pouring out: the dead come out of nothing from the head of the column back, the mist unrolling behind them. */
	static void pour(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("pour", L, null);
		for (int i = 0; i < GHOSTS.length; i++) {
			double t0 = 0.3 * GHOSTS[i][2] / 51;
			String n = "ghost_" + i;
			a.scale(n, 0, 0.02);
			if (t0 > 0.001) a.scale(n, t0, 0.02);
			a.scale(n, Math.min(L, t0 + 0.09), 1.1);
			a.scale(n, Math.min(L, t0 + 0.15), 1);
			a.scale(n, L, 1);
		}
		for (int i = 0; i < WISPS.length; i++) {
			double t0 = 0.3 * WISPS[i][2] / 58;
			a.scale("wisp_" + i, 0, 0.02);
			if (t0 > 0.001) a.scale("wisp_" + i, t0, 0.02);
			a.scale("wisp_" + i, Math.min(L, t0 + 0.12), 1);
			a.scale("wisp_" + i, L, 1);
		}
		a.sample("mist", "scale", 0, L, 5, t -> new double[] {0.6 + 0.4 * SpellArt.easeOut(t / L), 1, Math.max(0.02, SpellArt.easeOut(t / 0.45))});
	}

	// --- Gate of Ten Thousand Ghosts ---

	/**
	 * Bone-white stone, weathered: pale, mottled, darker in its cracks and beneath; on the front and back of a pillar ({@code glyphs}) a column
	 * of the dead's glyphs carved in it, lit with ghost fire.
	 */
	static SpellArt.Painter boneStone(int seed, boolean glyphs) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3), crack = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.3, t.py * 0.22, t.pz * 0.3, seed + 3, 2) - 1);
			boolean face = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (glyphs && face && Math.abs(t.across() - 0.5) < 0.32) {
				double cell = t.ly / t.sx() / 0.9;
				int ci = (int) Math.floor(cell);
				if (t.up() > 0.18 && t.up() < 0.92 && Fire.glyph(ci, seed, (t.across() - 0.18) / 0.64, cell - ci)) return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_PALE, 0.4), 0.97);
			}
			if (Math.pow(crack, 16) > 0.62) return SpellArt.col(SpellArt.mix(BONE_DARK, BONE_SHADE, 0.3));
			double light = SpellArt.clamp01(0.55 + (n - 0.5) * 0.7 + 0.18 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny));
			return SpellArt.col(SpellArt.ramp(light, BONE_DARK, BONE_SHADE, BONE, 0xF6F0E0));
		};
	}

	/** Roof tiles: dark slate in rows of rolled tiles, a little paler along each roll's crest. */
	static SpellArt.Painter tiles(int seed) {
		return t -> {
			double roll = t.px / 2.2, f = roll - Math.floor(roll), crest = Math.sin(f * Math.PI);
			double row = t.pz / 3.0, rf = row - Math.floor(row);
			int c = SpellArt.mix(SLATE, SLATE_LIGHT, crest * 0.7 + 0.2 * SpellArt.noise(roll, row, seed));
			if (rf < 0.08) c = SpellArt.shade(c, -0.4);
			return SpellArt.col(c);
		};
	}

	/**
	 * A door leaf of the gate (a slab: its broad faces front and back): black lacquer, rows of bone studs, a ring of bone for a knocker by
	 * its inner edge ({@code inner}: its -x or +x side), a thread of ghost light down that edge.
	 */
	static SpellArt.Painter doorLeaf(boolean innerAtPlusX) {
		return t -> {
			if (t.flat() || t.side()) {
				boolean innerEdge = t.side() && (t.f == SpellArt.Face.SIDE_B) == innerAtPlusX;
				return innerEdge ? SpellArt.glow(FIRE_CYAN, 0.9) : SpellArt.col(LACQUER);
			}
			double a = t.f == SpellArt.Face.FRONT ? t.across() : 1 - t.across(), u = innerAtPlusX ? a : 1 - a, v = t.up();
			// Studs: five across, seven up.
			double sx = u * 5, sy = v * 7.5, fx = sx - Math.floor(sx) - 0.5, fy = sy - Math.floor(sy) - 0.5;
			if (fx * fx + fy * fy < 0.07 && u > 0.04 && u < 0.88) return SpellArt.col(fx + fy < -0.1 ? 0xF6F0E0 : BONE_SHADE);
			// The knocker: a ring of bone near the inner edge, at a third of the height.
			double kx = (u - 0.86) * 7, ky = (v - 0.45) * 7.5 * 1.0, r = Math.hypot(kx, ky);
			if (Math.abs(r - 0.55) < 0.12) return SpellArt.col(BONE);
			double grain = SpellArt.noise(u * 2, v * 30, 17);
			return SpellArt.col(SpellArt.mix(LACQUER, LACQUER_LIGHT, 0.2 + 0.4 * grain + (v > 0.97 || v < 0.03 ? 0.3 : 0)));
		};
	}

	/**
	 * The way beyond the gate (on a sheet standing in its doorway, facing -z): a whirl of ghost fire with {@code arms} arms round a dark heart,
	 * clear outside its round (so, turning, no corner shows).
	 */
	static SpellArt.Painter vortex(int arms, double twist, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.99) return SpellArt.CLEAR;
			double ang = Math.atan2(y, x) / (2 * Math.PI), s = ang * arms + Math.log(Math.max(0.06, r)) * twist + 0.15 * SpellArt.noise(r * 6, ang * 8, seed);
			double f = s - Math.floor(s), arm = Math.pow(Math.abs(Math.sin(f * Math.PI)), 3);
			double heat = SpellArt.clamp01(arm * (1.2 - 0.5 * r) + 0.15 * (1 - r));
			if (heat < 0.12) return r < 0.25 ? SpellArt.col(VOID) : SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.ramp(heat, GHOST_FIRE), 0.35 + 0.6 * heat);
		};
	}

	/** A hand of the dead reaching out (a limb along it): mist, the fingertips lit with ghost fire. */
	static SpellArt.Painter deadHand(int seed) {
		SpellArt.Painter mist = robe(seed);
		return t -> t.up() > 0.82 ? SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_PALE, t.up()), 0.9) : mist.paint(t);
	}

	/** The plaque over the doorway: dark lacquer framed in bone, three glyphs of the dead lit in ghost fire. */
	static SpellArt.Painter plaque() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT && t.f != SpellArt.Face.BACK) return SpellArt.col(BONE_SHADE);
			double u = t.across(), v = t.up();
			if (u < 0.06 || u > 0.94 || v < 0.12 || v > 0.88) return SpellArt.col(BONE);
			double cell = (u - 0.06) / 0.88 * 3;
			int ci = (int) Math.floor(cell);
			if (Fire.glyph(ci, 31, cell - ci, (v - 0.12) / 0.76)) return SpellArt.glow(SpellArt.mix(FIRE_CYAN, FIRE_WHITE, 0.4), 0.97);
			return SpellArt.col(LACQUER);
		};
	}

	static final double PILLAR_X = 15, PILLAR_W = 4.6, PILLAR_H = 36, DOOR_H = 27, HINGE = 12.8;
	/** The hands reaching out of the way beyond: where each comes from in the doorway (x, y), where it reaches to (x, y, z). */
	static final double[][] HANDS = {{-6, 16, -9, 20, -11}, {5, 18, 10, 22, -12}, {-3, 8, -7, 6, -12}, {6, 9, 9, 5, -10}, {0, 21, 1, 25, -9}};

	/**
	 * The Gate of Ten Thousand Ghosts, fourteen blocks of it, facing -z, its foot at the origin: two pillars of bone-white stone carved with
	 * glowing glyphs ("gate"), a lintel and its plaque, a tiled roof with upswept eaves; ghost fire burning in stone lanterns either side of it
	 * ("flame_l", "flame_r"); paper lanterns hanging under the lintel ("lantern_l", "lantern_r"); its two doors of black lacquer, studded with bone
	 * ("door_l", "door_r", hinged at the pillars, opening inward); in its doorway the way beyond, a whirl of ghost fire round a dark heart
	 * ("vortex", "vortex_in", a dark wall behind them), hands of the dead reaching out of it ("hand_0".."hand_4"); mist about its foot ("mist").
	 */
	static void ghostGate(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone gate = m.bone("gate", "root", 0, 0, 0);
		for (int s = -1; s <= 1; s += 2) {
			double x = s * PILLAR_X;
			gate.box(x, 2, 0, 6.6, 4, 6.6, boneStone(40, false));
			gate.box(x, 4 + PILLAR_H / 2 - 2, 0, PILLAR_W, PILLAR_H - 4, PILLAR_W, boneStone(41 + (s > 0 ? 1 : 0), true)).density(2.5);
			gate.box(x, PILLAR_H + 0.4, 0, 5.8, 1.4, 5.8, boneStone(43, false));
		}
		// The lintel and the frieze under it, closed from pillar to pillar down to the doors' tops, the plaque on it. (A beam over the doors
		// once stood there alone, the sky showing between it and the lintel either side of the plaque: the user's report, 2026-10-26, "fix
		// the gap in the gate of ten thousand ghosts".)
		gate.box(0, 33.2, 0, 40, 3.4, 4, boneStone(44, false));
		gate.box(0, 29.3, 0.45, 2 * HINGE + 0.4, 4.8, 3.9, boneStone(45, false));
		gate.box(0, 30.8, -2.1, 11, 4.4, 0.8, plaque()).density(4);
		// The roof: two slopes of tiles meeting along a ridge, the eaves swept up at the corners, horns of bone at the ridge's ends.
		SpellArt.Painter roof = tiles(46);
		gate.box(0, 37.6, -2.8, 46, 1.4, 6.4, roof).rot(16, 0, 0);
		gate.box(0, 37.6, 2.8, 46, 1.4, 6.4, roof).rot(-16, 0, 0);
		gate.box(0, 39.4, 0, 42, 1.6, 1.6, boneStone(47, false));
		for (int s = -1; s <= 1; s += 2) {
			for (int f = -1; f <= 1; f += 2) {
				SpellArt.limb(gate, SpellArt.v3(s * 21.5, 36.2, f * 5.2), SpellArt.v3(s * 25.5, 38.6, f * 6.4), 1.4, 1.4, roof);
			}
			SpellArt.limb(gate, SpellArt.v3(s * 20.5, 40, 0), SpellArt.v3(s * 22.4, 43, 0), 1.2, 1.2, boneStone(48, false));
			SpellArt.limb(gate, SpellArt.v3(s * 22.4, 43, 0), SpellArt.v3(s * 21.2, 44.6, 0), 0.8, 0.8, boneStone(48, false));
		}
		// The dark beyond the doorway, and the way into it: the dark set within the frame's depth, its edges tucked into the pillars and up into the
		// frieze (it stood a little behind them, and from below the sky showed through the slit between it and the frieze: the user's report,
		// 2026-10-26, "its because that black wall is a little bit behind the actual gate thing"); the frieze runs back over its top edge.
		gate.box(0, (DOOR_H + 1) / 2, 2.2, 2 * HINGE + 0.8, DOOR_H + 1, 0.2, t -> SpellArt.col(VOID)).only(SpellArt.Face.FRONT, SpellArt.Face.BACK).density(0.5);
		SpellArt.Bone vortex = m.bone("vortex", "gate", 0, DOOR_H / 2, 1.95);
		vortex.box(0, DOOR_H / 2, 1.95, 25, 25, 0.1, vortex(4, 1.6, 50)).only(SpellArt.Face.FRONT).density(2);
		SpellArt.Bone in = m.bone("vortex_in", "gate", 0, DOOR_H / 2, 1.75);
		in.box(0, DOOR_H / 2, 1.75, 15, 15, 0.1, vortex(3, -2.2, 51)).only(SpellArt.Face.FRONT).density(3);
		// The doors, hinged at the pillars, opening inward (their rest: shut).
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone door = m.bone(s < 0 ? "door_l" : "door_r", "gate", s * HINGE, 0, 0);
			door.box(s * HINGE / 2, DOOR_H / 2, 0, HINGE - 0.15, DOOR_H, 1.2, doorLeaf(s < 0)).density(2.5);
		}
		// Hands of the dead reaching out of the way beyond.
		for (int i = 0; i < HANDS.length; i++) {
			double[] h = HANDS[i];
			double[] from = SpellArt.v3(h[0], h[1], 1.4), to = SpellArt.v3(h[2], h[3], h[4]);
			SpellArt.Bone hand = m.bone("hand_" + i, "gate", from[0], from[1], from[2]);
			double[] dir = SpellArt.sub(to, from), unit = SpellArt.times(dir, 1 / SpellArt.len(dir));
			// Square to the reach: across it (level) and down from it.
			double[] side = SpellArt.v3(-unit[2], 0, unit[0]);
			side = SpellArt.times(side, 1 / Math.max(1.0e-6, SpellArt.len(side)));
			double[] down = SpellArt.v3(side[1] * unit[2] - side[2] * unit[1], side[2] * unit[0] - side[0] * unit[2], side[0] * unit[1] - side[1] * unit[0]);
			if (down[1] > 0) down = SpellArt.times(down, -1);
			double[] elbow = SpellArt.add(from, SpellArt.times(dir, 0.5)), r = SpellArt.aimY(unit[0], unit[1], unit[2]);
			SpellArt.Painter sleeve = robe(60 + i);
			SpellArt.limb(hand, from, elbow, 2.2, 2.0, sleeve);
			SpellArt.limb(hand, elbow, to, 1.4, 1.3, sleeve);
			SpellArt.blade(hand, elbow, 3.4, 2.0, 150, 0, 0, wisp(80 + i)).density(3);
			// The hand: a palm square to the reach, fingers splayed and crooked like claws, a thumb out to the side.
			double[] palm = SpellArt.add(to, SpellArt.times(unit, 0.8));
			hand.box(palm[0], palm[1], palm[2], 2.0, 1.8, 0.7, deadHand(70 + i)).rot(r[0], r[1], r[2]).density(4);
			for (int k = 0; k < 4; k++) {
				double spread = (k - 1.5) * 0.55;
				double[] base = SpellArt.add(SpellArt.add(palm, SpellArt.times(unit, 0.8)), SpellArt.times(side, spread));
				double[] tip = SpellArt.add(SpellArt.add(SpellArt.add(base, SpellArt.times(unit, 2.1)), SpellArt.times(side, spread * 0.7)), SpellArt.times(down, 0.8));
				SpellArt.limb(hand, base, tip, 0.45, 0.45, deadHand(74 + k)).density(5);
			}
			double[] thumb = SpellArt.add(palm, SpellArt.times(side, 1.1));
			SpellArt.limb(hand, thumb, SpellArt.add(SpellArt.add(thumb, SpellArt.times(unit, 1.4)), SpellArt.times(side, 0.9)), 0.5, 0.5, deadHand(79)).density(5);
		}
		// Lanterns hanging under the lintel.
		for (int s = -1; s <= 1; s += 2) {
			double[] top = SpellArt.v3(s * 11.5, 27.6, -1.6);
			SpellArt.Bone lantern = m.bone(s < 0 ? "lantern_l" : "lantern_r", "gate", top[0], top[1], top[2]);
			SpellArt.limb(lantern, top, SpellArt.add(top, SpellArt.v3(0, -1.6, 0)), 0.25, 0.25, t -> SpellArt.col(BONE_DARK)).density(6);
			lantern.box(top[0], top[1] - 3.8, top[2], 3, 4.2, 3, lantern(9 + s)).density(4);
			SpellArt.blade(lantern, SpellArt.v3(top[0], top[1] - 6, top[2]), 2.2, 1.2, 180, 0, 0, t -> SpellArt.col(CINNABAR)).density(4);
		}
		// Stone lanterns flanking it, ghost fire burning in them.
		for (int s = -1; s <= 1; s += 2) {
			double x = s * 24.5, z = -3;
			gate.box(x, 2.6, z, 3.2, 5.2, 3.2, boneStone(52, false));
			gate.box(x, 5.6, z, 5, 0.9, 5, boneStone(53, false));
			for (int cx = -1; cx <= 1; cx += 2) {
				for (int cz = -1; cz <= 1; cz += 2) gate.box(x + cx * 1.9, 7.6, z + cz * 1.9, 0.9, 3.2, 0.9, boneStone(54, false));
			}
			gate.box(x, 9.8, z, 6.4, 1.2, 6.4, tiles(55)).rot(0, 45, 0);
			gate.box(x, 10.9, z, 1.4, 1.4, 1.4, boneStone(56, false)).rot(0, 45, 0);
			double[] at = SpellArt.v3(x, 6.05, z);
			SpellArt.Bone flame = m.bone(s < 0 ? "flame_l" : "flame_r", "gate", at[0], at[1], at[2]);
			SpellArt.shared(m, flame, "brazier", false, at, 6.4, 4.2, 0, 0, 0, ghostFire(2.2, 230), 4, false);
			SpellArt.shared(m, flame, "brazier", false, at, 6.4, 4.2, 0, 90, 0, ghostFire(2.2, 230), 4, false);
		}
		// Mist about its foot, last (a texel seen through hides what is drawn after it).
		SpellArt.Bone mist = m.bone("mist", "root", 0, 0, 0);
		SpellArt.sheet(mist, 0, 0.4, -4, 64, 40, 0, 0, 0, mistBank(310)).density(1);
		openGate(m);
		draw(m);
		slam(m);
	}

	static final double DOOR_OPEN = 100;

	/** The opening: the gate rises out of the mist, its fires catch, its doors swing in, and the way beyond whirls up and its dead reach out. */
	static void openGate(SpellArt.Model m) {
		double L = 1.5;
		SpellArt.Anim a = m.anim("open", L, null);
		a.sample("gate", "position", 0, 0.8, 6, t -> new double[] {0, -46 * (1 - SpellArt.easeOut(t / 0.8)), 0});
		a.pos("gate", L, 0, 0, 0);
		a.sample("mist", "scale", 0, 0.6, 4, t -> new double[] {0.2 + 0.8 * SpellArt.easeOut(t / 0.6), 1, 0.2 + 0.8 * SpellArt.easeOut(t / 0.6)});
		for (int s = -1; s <= 1; s += 2) {
			String door = s < 0 ? "door_l" : "door_r";
			double open = s < 0 ? -DOOR_OPEN : DOOR_OPEN;
			a.rot(door, 0, 0, 0, 0);
			a.rot(door, 0.8, 0, 0, 0);
			a.rot(door, 1.25, 0, open * 1.04, 0);
			a.rot(door, 1.4, 0, open, 0);
			a.rot(door, L, 0, open, 0);
			String flame = s < 0 ? "flame_l" : "flame_r";
			a.scale(flame, 0, 0.02);
			a.scale(flame, 0.5, 0.02);
			a.scale(flame, 0.75, 1.15);
			a.scale(flame, 0.9, 1);
		}
		for (String v : new String[] {"vortex", "vortex_in"}) {
			a.scale(v, 0, 0.01);
			a.scale(v, 0.8, 0.01);
			a.scale(v, 1.3, 1);
			a.scale(v, L, 1);
		}
		for (int i = 0; i < HANDS.length; i++) {
			double t0 = 1.0 + 0.06 * i;
			a.scale("hand_" + i, 0, 0.01);
			a.scale("hand_" + i, t0, 0.01);
			a.scale("hand_" + i, Math.min(L, t0 + 0.25), 1);
			a.scale("hand_" + i, L, 1);
		}
	}

	/**
	 * Drawing in, looped without a seam: the way beyond whirling (each whirl one arm round, the inner the other way), the hands of the dead
	 * clutching, the lanterns swinging, the fires flickering; the doors held open.
	 */
	static void draw(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("draw", T, "true");
		a.sample("vortex", "rotation", 0, T, 2, t -> new double[] {0, 0, 90 * t / T});
		a.sample("vortex_in", "rotation", 0, T, 2, t -> new double[] {0, 0, -120 * t / T});
		a.sample("vortex", "scale", 0, T, 8, t -> new double[] {1 + 0.03 * Math.sin(2 * Math.PI * t / T), 1 + 0.03 * Math.sin(2 * Math.PI * t / T), 1});
		a.rot("door_l", 0, 0, -DOOR_OPEN, 0).rot("door_l", T, 0, -DOOR_OPEN, 0);
		a.rot("door_r", 0, 0, DOOR_OPEN, 0).rot("door_r", T, 0, DOOR_OPEN, 0);
		for (int i = 0; i < HANDS.length; i++) {
			double ph = i * 1.9;
			a.sample("hand_" + i, "rotation", 0, T, 8, t -> new double[] {10 * Math.sin(2 * Math.PI * t / T + ph), 12 * Math.sin(2 * Math.PI * t / T + ph + 1.1), 6 * Math.sin(4 * Math.PI * t / T + ph)});
			a.sample("hand_" + i, "position", 0, T, 8, t -> new double[] {0, 0, -1.4 * Math.sin(2 * Math.PI * t / T + ph + 0.4)});
		}
		for (int s = -1; s <= 1; s += 2) {
			double ph = s * 0.9;
			a.sample(s < 0 ? "lantern_l" : "lantern_r", "rotation", 0, T, 8, t -> new double[] {6 * Math.sin(2 * Math.PI * t / T + ph), 0, 4 * Math.sin(2 * Math.PI * t / T + ph + 1.4)});
			a.sample(s < 0 ? "flame_l" : "flame_r", "scale", 0, T, 8, t -> new double[] {1 + 0.05 * Math.sin(4 * Math.PI * t / T + ph), 0.88 + 0.16 * Math.sin(4 * Math.PI * t / T + ph + 1), 1 + 0.05 * Math.sin(4 * Math.PI * t / T + ph)});
		}
		a.sample("mist", "scale", 0, T, 8, t -> new double[] {1 + 0.03 * Math.sin(2 * Math.PI * t / T), 1, 1 + 0.03 * Math.sin(2 * Math.PI * t / T + 1)});
	}

	/**
	 * The slam: its doors swing shut (meeting at 0.35 s), the hands torn back into the dark and the whirl gone with them, the gate shuddering
	 * at the blow; then it sinks back into the mist. Held at its end.
	 */
	static void slam(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("slam", L, "hold_on_last_frame");
		for (int s = -1; s <= 1; s += 2) {
			String door = s < 0 ? "door_l" : "door_r";
			double open = s < 0 ? -DOOR_OPEN : DOOR_OPEN;
			a.sample(door, "rotation", 0, 0.35, 5, t -> new double[] {0, open * (1 - SpellArt.easeIn(t / 0.35)), 0});
			a.rot(door, 0.42, 0, -open * 0.03, 0);
			a.rot(door, 0.5, 0, 0, 0);
			a.rot(door, L, 0, 0, 0);
		}
		for (String v : new String[] {"vortex", "vortex_in"}) {
			a.scale(v, 0, 1);
			a.scale(v, 0.3, 0.01);
			a.scale(v, L, 0.01);
		}
		for (int i = 0; i < HANDS.length; i++) {
			a.scale("hand_" + i, 0, 1);
			a.scale("hand_" + i, 0.22, 0.01);
			a.scale("hand_" + i, L, 0.01);
		}
		double[] t = {0, 0.35, 0.39, 0.43, 0.47, 0.51, 0.55};
		double[] dx = {0, 0, 0.7, -0.6, 0.45, -0.3, 0};
		for (int k = 0; k < t.length; k++) a.pos("gate", t[k], dx[k], 0, 0);
		// Sinking, eased out as well as in: it comes to rest as it is held (sunk at its fastest, it stopped dead: seen in the bone log).
		a.sample("gate", "position", 0.55, L, 6, tt -> new double[] {0, -16 * SpellArt.ease((tt - 0.55) / 0.35), 0});
	}
}
