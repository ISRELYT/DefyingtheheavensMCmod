/**
 * The looks of Void Shattering's great spells for SpellArt (2026-10-24): what comes through space torn open, each made of what it is cast
 * from (the user's rule: the spells' shapes made of their elements, not lifelike). The tears themselves are not here: the client draws them
 * by hand (RiftRenderer). Each spell's shapes, by its element: wood's storm cloud of spring thunder and the void tree's hanging roots; fire's
 * Golden Crow and the sun dragged through the void; earth's broken pillar of Mount Buzhou and the dome of the earth's veins; water's fall of
 * the Weak Water and the Kun; metal's severing sword light and the prison of ten swords; the ghost foundation's whirl of the Nine Nethers
 * and the Naihe Bridge; both paths' fist of heaven and demon.
 */
public class Void {
	// --- Spring thunder: the storm's slate and teal, jade light ---
	static final int STORM_DARK = 0x13292A, STORM = 0x264A48, STORM_LIGHT = 0x507E76, STORM_PALE = 0x9CC4B4, JADE = 0x52E89A, JADE_PALE = 0xBFFFDC,
			JADE_WHITE = 0xF2FFF7;
	// --- The void tree: its black wood with a violet sheen, the life it drinks ---
	static final int VOID_BLACK = 0x07040C, VOID_BARK = 0x1E1229, VOID_VIOLET = 0x46285E, VOID_SHEEN = 0x8662B4, LIFE = 0x86FFAE, LIFE_WHITE = 0xEAFFF2;
	// --- The sun's fire: hotter than the Vermilion Bird's, gold to white ---
	static final int SUN_EMBER = 0x7A2606, SUN_DEEP = 0xC2400C, SUN_ORANGE = 0xF07418, SUN_GOLD = 0xFFAE30, SUN_PALE = 0xFFE184, SUN_WHITE = 0xFFFAE8;
	static final int[] SUN_HEAT = {SUN_EMBER, SUN_DEEP, SUN_ORANGE, SUN_GOLD, SUN_PALE, SUN_WHITE};
	/** The sun's face behind the crow: the red sun of the old paintings, its heart gold. */
	static final int[] SUN_FACE = {SUN_EMBER, 0xA81E08, SUN_DEEP, SUN_ORANGE, SUN_GOLD};

	// --- Shared painters ---

	/**
	 * A feather of flame in the colours of {@code heat} (on a blade, its length along it): Fire#flameFeather's tongue, widest a third of the
	 * way out, drawn to a wavering point split into {@code licks}; hottest down its middle, cooling to its edges and tip; glowing.
	 */
	static SpellArt.Painter featherOf(double licks, int seed, int[] heat) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.05 * Math.sin(v * 9 + seed) * v - 0.05 * (SpellArt.noise(v * 5 + seed, 2, seed) - 0.5);
			double half = 0.48 * Math.pow(Math.sin(Math.PI * Math.min(1, Math.pow(v, 0.55)) * 0.999), 0.8) * (0.84 + 0.16 * SpellArt.noise(v * 11, seed, seed + 1));
			if (v < 0.015 || Math.abs(uu) > half) return SpellArt.CLEAR;
			double across = (uu / half + 1) / 2;
			double tongue = Math.pow(Math.abs(Math.sin(across * Math.PI * licks + seed * 0.4)), 0.6);
			double top = 0.7 + 0.3 * tongue * (0.8 + 0.2 * SpellArt.hash(seed, 1, 7));
			if (v > top) return SpellArt.CLEAR;
			double core = 1 - Math.abs(uu) / half, out = v / top;
			double streak = 1 - Math.abs(2 * SpellArt.fbm(across * 3.5 + seed, v * 2.6, seed + 2, 2) - 1);
			double hot = SpellArt.clamp01(core * (1.15 - 0.8 * out) + 0.3 * Math.pow(streak, 3) * (1 - out) - 0.08);
			return SpellArt.glow(SpellArt.ramp(hot, heat), 0.22 + 0.76 * Math.pow(hot, 0.6));
		};
	}

	/**
	 * Fire made into a body, in the colours of {@code heat} (on a solid): Fire#blaze's flames streaming back along it (+z), hottest along its
	 * top and toward its heart (the line x = 0, y = {@code axisY}), cooler beneath; all glowing.
	 */
	static SpellArt.Painter blazeOf(double axisY, int seed, int[] heat) {
		return t -> {
			double r = Math.hypot(t.qx(), t.qy() - axisY);
			double flow = SpellArt.fbm3(t.px * 0.22, t.py * 0.3, t.pz * 0.06, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.5, t.py * 0.5, t.pz * 0.11 + flow, seed + 5, 2) - 1);
			double heat2 = SpellArt.clamp01(0.52 - r * 0.03 + (flow - 0.5) * 0.75 + 0.32 * Math.max(0, t.ny) - 0.38 * Math.max(0, -t.ny) + 0.42 * Math.pow(streak, 3));
			return SpellArt.glow(SpellArt.ramp(heat2, heat), 0.7 + 0.28 * heat2);
		};
	}

	/** {@link #blazeOf} on a limb (its own axis along its cube): hotter toward each face's middle and on top, streaked along it. */
	static SpellArt.Painter blazeLimbOf(int seed, int[] heat) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.25, t.py * 0.3, t.pz * 0.1, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm(t.fx * 3 + seed, t.ly * 0.35 + flow * 2, seed + 5, 2) - 1);
			double middle = 1 - Math.abs(t.fx - 0.5) * 2;
			double h = SpellArt.clamp01(0.36 + (flow - 0.5) * 0.8 + 0.22 * Math.max(0, t.ny) + 0.3 * middle + 0.35 * Math.pow(streak, 3));
			return SpellArt.glow(SpellArt.ramp(h, heat), 0.66 + 0.32 * h);
		};
	}

	/**
	 * A ring of script on a sheet, in {@code line}'s and {@code glyph}'s light: two borders round a band of glyphs ({@code cells}, each its own;
	 * Fire#glyph), nothing else ({@code inner}, {@code outer}: the band's edges, fractions of the sheet's half).
	 */
	static SpellArt.Painter script(double inner, double outer, int cells, int seed, int line, int glyph) {
		return t -> {
			double x = t.across() - 0.5, z = t.back() - 0.5, r = Math.hypot(x, z) * 2;
			if (Math.abs(r - inner) < 0.016 || Math.abs(r - outer) < 0.016) return SpellArt.glow(line, 0.96);
			if (r < inner || r > outer) return SpellArt.CLEAR;
			double ang = (Math.atan2(z, x) / (2 * Math.PI) + 0.5) * cells;
			int cell = Math.floorMod((int) Math.floor(ang), cells);
			if (Fire.glyph(cell, seed, ang - Math.floor(ang), (r - inner) / (outer - inner))) return SpellArt.glow(glyph, 0.97);
			return SpellArt.CLEAR;
		};
	}

	/**
	 * One of the eight trigrams (Bagua) on a sheet, its three lines across it ({@code lines}: from its foot, at +z, to its head; true a whole line,
	 * yang, false a broken one, yin) in a round of script; in {@code line}'s and {@code glyph}'s light. Thunder's is whole, broken, broken (☳);
	 * earth's all broken (☷).
	 */
	static SpellArt.Painter trigram(boolean[] lines, int seed, int line, int glyph) {
		SpellArt.Painter ring = script(0.66, 0.9, 16, seed, line, glyph);
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.62) return ring.paint(t);
			if (Math.abs(r - 0.6) < 0.018) return SpellArt.glow(line, 0.95);
			for (int i = 0; i < 3; i++) {
				double lz = 0.26 - i * 0.26;
				if (Math.abs(z - lz) > 0.075 || Math.abs(x) > 0.36) continue;
				if (!lines[i] && Math.abs(x) < 0.07) continue;
				return SpellArt.glow(SpellArt.mix(glyph, 0xFFFFFF, 0.3), 0.98);
			}
			return SpellArt.CLEAR;
		};
	}

	// --- Spring Thunder of the Azure Emperor: the cloud ---

	/**
	 * A billow of storm cloud (on a cube): slate and teal, dark beneath and lit pale on its tops, veins of jade lightning glowing in it;
	 * soft at its edges, seen through.
	 */
	static SpellArt.Painter stormCloud(int seed) {
		return t -> {
			double billow = SpellArt.fbm3(t.px * 0.09, t.py * 0.15, t.pz * 0.09, seed, 4);
			double light = SpellArt.clamp01(0.16 + billow * 0.5 + 0.44 * Math.max(0, t.ny) - 0.26 * Math.max(0, -t.ny));
			int c = SpellArt.ramp(light, STORM_DARK, STORM, STORM_LIGHT, STORM_PALE);
			// The storm's jade light, glowing through it from within, mostly from beneath.
			double within = SpellArt.smooth(0.6, 0.78, SpellArt.fbm3(t.px * 0.045, t.py * 0.05, t.pz * 0.045, seed + 3, 2)) * (t.ny > 0.5 ? 0.25 : 1);
			c = SpellArt.mix(c, SpellArt.mix(JADE, JADE_PALE, billow), within * 0.6);
			double edge = Math.min(Math.min(t.fx, 1 - t.fx), Math.min(t.fy, 1 - t.fy)) * 2;
			double a = (0.5 + 0.32 * light) * (0.25 + 0.75 * SpellArt.smooth(0, 0.75, edge + (billow - 0.5) * 0.9));
			return within > 0.35 ? SpellArt.glow(c, Math.max(0.05, a)) : SpellArt.col(c, a);
		};
	}

	/** The cloud's underside (on a great sheet): its dark belly in soft swells, lit jade toward its heart; thinning to nothing at its rim. */
	static SpellArt.Painter cloudBase(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			double swell = SpellArt.fbm(t.across() * 6 + seed, t.back() * 6, seed, 4);
			double a = SpellArt.clamp01((0.98 - r) * 3) * (0.5 + 0.35 * swell);
			if (a < 0.04) return SpellArt.CLEAR;
			int c = SpellArt.ramp(SpellArt.clamp01(swell * 0.8 - 0.05), STORM_DARK, STORM, STORM_LIGHT);
			double lit = SpellArt.smooth(0.55, 0.15, r) * SpellArt.smooth(0.45, 0.7, swell);
			c = SpellArt.mix(c, JADE, lit * 0.5);
			return lit > 0.3 ? SpellArt.glow(c, a) : SpellArt.col(c, a);
		};
	}

	/** The storm's eye beneath it (on a sheet): a whirl of jade light round a dark heart, its arms spiralling in; clear outside its round. */
	static SpellArt.Painter stormEye(int arms, double twist, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.99) return SpellArt.CLEAR;
			double ang = Math.atan2(z, x) / (2 * Math.PI), s = ang * arms + Math.log(Math.max(0.05, r)) * twist + 0.15 * SpellArt.noise(r * 6, ang * 8, seed);
			double f = s - Math.floor(s), arm = Math.pow(Math.abs(Math.sin(f * Math.PI)), 3);
			double heat = SpellArt.clamp01(arm * (1.15 - 0.6 * r) + 0.2 * (1 - r));
			if (heat < 0.14) return r < 0.3 ? SpellArt.col(STORM_DARK, 0.9) : SpellArt.col(STORM, 0.5 * (1 - r));
			return SpellArt.glow(SpellArt.ramp(heat, STORM_LIGHT, JADE, JADE_PALE, JADE_WHITE), 0.3 + 0.62 * heat);
		};
	}

	/**
	 * Lightning crawling over the cloud's underside (on a great sheet): seven forked bolts out from its heart, white-hot threads in jade glow,
	 * wandering; clear between them.
	 */
	static SpellArt.Painter lightning(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r < 0.12 || r > 0.97) return SpellArt.CLEAR;
			double ang = Math.atan2(z, x);
			double best = 9;
			for (int k = 0; k < 7; k++) {
				double base = k * 2 * Math.PI / 7 + SpellArt.hash(k, seed, 1) * 0.6;
				double a = base + 0.32 * (SpellArt.fbm(r * 5 + k * 3.1, k, seed, 2) - 0.5) * 2;
				double d = Math.abs(Math.atan2(Math.sin(ang - a), Math.cos(ang - a))) * r;
				// A fork off it from a third of the way out.
				if (r > 0.38) {
					double fa = a + (k % 2 == 0 ? 1 : -1) * (r - 0.38) * 0.9;
					d = Math.min(d, Math.abs(Math.atan2(Math.sin(ang - fa), Math.cos(ang - fa))) * r + (r > 0.8 ? 1 : 0));
				}
				best = Math.min(best, d / (1.2 - r));
			}
			if (best < 0.006) return SpellArt.glow(JADE_WHITE, 0.98);
			if (best < 0.022) return SpellArt.glow(SpellArt.mix(JADE_PALE, JADE, (best - 0.006) / 0.016), 0.85 - 18 * (best - 0.006));
			return SpellArt.CLEAR;
		};
	}

	/** The cloud's rings of billows: how many, how far out, how high, and each billow's width, height and depth. */
	static final double[][] STORM_RINGS = {{10, 10, 1, 21, 13, 19}, {14, 24, 0, 23, 12, 20}, {18, 38, -1, 23, 11, 20}, {22, 52, -2, 21, 9, 18}, {8, 21, 9, 25, 13, 22}};
	/** Thunder's trigram, ☳: a whole line at its foot, two broken above it. */
	static final boolean[] THUNDER = {true, false, false};

	/**
	 * Spring Thunder of the Azure Emperor's cloud, some fifty blocks across, at its middle: rings of storm billows ("ring_0".."ring_4", turning
	 * each its own way), jade lightning veined in them; beneath it the storm's eye ("eye", a whirl of jade light), the trigram of thunder burning
	 * in a round of script under that ("sigil"), and the lightning that crawls over its underside as it calls a volley down ("veins", unseen
	 * between its flashes).
	 */
	static void azureThunderCloud(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int r = 0; r < STORM_RINGS.length; r++) {
			double[] g = STORM_RINGS[r];
			SpellArt.Bone ring = m.bone("ring_" + r, "root", 0, g[2], 0);
			SpellArt.Cube[][] variants = new SpellArt.Cube[4][];
			for (int k = 0; k < g[0]; k++) {
				double th = 360.0 * k / g[0] + r * 13, x = Math.sin(Math.toRadians(th)) * g[1], z = Math.cos(Math.toRadians(th)) * g[1];
				double lift = (k % 3 == 0 ? 1.2 : k % 3 == 1 ? -0.8 : 0.3);
				int v = k % 4;
				if (variants[v] == null) {
					variants[v] = new SpellArt.Cube[] {ring.box(x, g[2] + lift, z, g[3], g[4], g[5], stormCloud(400 + r * 10 + v)).rot(0, th, 0),
							ring.box(x, g[2] + lift + g[4] * 0.2, z, g[3] * 0.72, g[4] * 1.2, g[5] * 0.72, stormCloud(450 + r * 10 + v)).rot(0, th + 45, 0)};
				} else {
					ring.like(variants[v][0], x, g[2] + lift, z, k % 2 == 0).rot(0, th, 0);
					ring.like(variants[v][1], x, g[2] + lift + g[4] * 0.2, z, k % 2 == 1).rot(0, th + 45, 0);
				}
			}
		}
		// Beneath it: its dark belly, its eye, the trigram of thunder in its round of script, and the lightning (each a sheet, drawn after the billows).
		SpellArt.Bone base = m.bone("base", "root", 0, -5.4, 0);
		SpellArt.sheet(base, 0, -5.4, 0, 118, 118, 0, 0, 0, cloudBase(37)).density(1);
		SpellArt.Bone eye = m.bone("eye", "root", 0, -6.6, 0);
		SpellArt.sheet(eye, 0, -6.6, 0, 46, 46, 0, 0, 0, stormEye(5, 1.6, 31)).density(1.5);
		SpellArt.Bone sigil = m.bone("sigil", "root", 0, -7.8, 0);
		SpellArt.sheet(sigil, 0, -7.8, 0, 30, 30, 0, 0, 0, trigram(THUNDER, 33, JADE_PALE, JADE_WHITE)).density(3);
		SpellArt.Bone veins = m.bone("veins", "root", 0, -7.2, 0);
		SpellArt.sheet(veins, 0, -7.2, 0, 116, 116, 0, 0, 0, lightning(35)).density(1.25);
		gather(m);
		churn(m);
		flash(m);
	}

	/**
	 * Its gathering: the rings swell in out of nothing, the inner first, swirling as they come (their wheeling is their spin's, already turning:
	 * SpellArt#splitSpins); the eye opens; the trigram kindles last.
	 */
	static void gather(SpellArt.Model m) {
		double L = 1.5;
		SpellArt.Anim a = m.anim("gather", L, null);
		double[] from = {0, 0.15, 0.3, 0.45, 0.2};
		for (int r = 0; r < STORM_RINGS.length; r++) {
			String n = "ring_" + r;
			double t0 = from[r];
			a.scale(n, 0, 0.08);
			if (t0 > 0) a.scale(n, t0, 0.08);
			a.sample(n, "scale", t0, Math.min(L, t0 + 0.7), 5, t -> {
				double e = SpellArt.easeOut((t - t0) / 0.7);
				return new double[] {0.08 + 0.92 * e, 0.08 + 0.92 * e, 0.08 + 0.92 * e};
			});
			a.scale(n, L, 1);
		}
		a.scale("base", 0, 0.08);
		a.scale("base", 0.3, 0.25);
		a.scale("base", 1.15, 1);
		a.scale("base", L, 1);
		a.scale("eye", 0, 0.02);
		a.scale("eye", 0.4, 0.02);
		a.scale("eye", 1.1, 1.05);
		a.scale("eye", L, 1);
		a.scale("sigil", 0, 0.02);
		a.scale("sigil", 0.9, 0.02);
		a.scale("sigil", 1.25, 1.15);
		a.scale("sigil", L, 1);
	}

	/**
	 * Its churning, looped without a seam: the rings wheeling, alternately, the eye spinning, the trigram turning slowly; now and then a flicker
	 * of lightning in it between the volleys.
	 */
	static void churn(SpellArt.Model m) {
		double T = 16;
		SpellArt.Anim a = m.anim("churn", T, "true");
		double[] turns = {720, -360, 360, -360, 360};
		for (int r = 0; r < STORM_RINGS.length; r++) {
			double turn = turns[r];
			int rr = r;
			a.sample("ring_" + r, "rotation", 0, T, 16, t -> new double[] {0, turn * t / T, 0});
			a.sample("ring_" + r, "position", 0, T, 32, t -> new double[] {0, 0.7 * Math.sin(2 * Math.PI * t / T * 4 + rr), 0});
		}
		a.sample("base", "rotation", 0, T, 16, t -> new double[] {0, -360 * t / T, 0});
		a.sample("eye", "rotation", 0, T, 16, t -> new double[] {0, -1440 * t / T, 0});
		a.sample("sigil", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
		// The veins unseen, but for two flickers.
		double[][] flickers = {{3.1, 0.6}, {9.5, 0.75}};
		a.scale("veins", 0, 0.01);
		for (double[] f : flickers) {
			a.scale("veins", f[0], 0.01);
			a.scale("veins", f[0] + 0.05, f[1]);
			a.scale("veins", f[0] + 0.12, 0.01);
		}
		a.scale("veins", T, 0.01);
	}

	/** A volley: the lightning flashes over its underside, the trigram and the eye flaring. */
	static void flash(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("flash", 0.5, null);
		a.scale("veins", 0, 0.01);
		a.scale("veins", 0.04, 1.0);
		a.scale("veins", 0.14, 0.92);
		a.scale("veins", 0.2, 1.04);
		a.scale("veins", 0.34, 0.01);
		a.scale("sigil", 0, 1);
		a.scale("sigil", 0.06, 1.22);
		a.scale("sigil", 0.3, 1);
		a.scale("eye", 0, 1);
		a.scale("eye", 0.06, 1.12);
		a.scale("eye", 0.35, 1);
	}

	// --- Void Withering: the void tree's roots ---

	/**
	 * The void tree's wood (on a limb, its grain along its length): black with a violet sheen, ridged, lit a little on top; veins of the life it
	 * drinks glowing green-white up it.
	 */
	static SpellArt.Painter voidBark(int seed) {
		return t -> {
			if (t.flat()) return SpellArt.col(VOID_BARK);
			// A thread of the life it drinks winding up each face, unbroken, brighter in swells along it.
			int face = t.f.ordinal();
			double at = 0.5 + 0.26 * Math.sin(t.qy() * 0.11 + t.qx() * 0.07 + seed + face * 1.9);
			double d = Math.abs(t.fx - at), swell = 0.55 + 0.45 * Math.sin(t.qy() * 0.35 + face + seed);
			if (d < 0.05 + 0.03 * swell) return SpellArt.glow(SpellArt.mix(LIFE, LIFE_WHITE, swell * (1 - d * 12)), 0.95);
			if (d < 0.14) return SpellArt.glow(SpellArt.mix(VOID_VIOLET, LIFE, 0.35 * swell), 0.6);
			double grain = SpellArt.noise(t.fx * 6 + seed, t.qy() * 0.3, seed);
			double ridge = Math.abs(Math.sin(t.fx * Math.PI * 4 + grain * 2.5));
			int c = SpellArt.ramp(SpellArt.clamp01(0.18 + grain * 0.35 + 0.22 * ridge + 0.25 * Math.max(0, t.ny)), VOID_BLACK, VOID_BARK, VOID_VIOLET, VOID_SHEEN);
			return SpellArt.col(c);
		};
	}

	/** A root's tip, where it drinks (a blob at its end): the life it draws glowing white-green, dark round it. */
	static SpellArt.Painter rootTip() {
		return t -> {
			double r = Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2;
			return r < 0.7 ? SpellArt.glow(SpellArt.mix(LIFE_WHITE, LIFE, r * 1.3), 0.92) : SpellArt.col(VOID_BARK);
		};
	}

	/**
	 * The roots that reach the land (none since 2026-10-24: the user found them waving about doing nothing, and the roots that reach out now
	 * seize what they reach, drawn by hand: TendrilRenderer), and those that hang, spilling out of the rift.
	 */
	static final int ROOTS = 0, HANGING = 14;
	/** How far below the rift the land is (40 by its realm: the rift's height over it, WoodSpells#WITHER_HIGH); the hanging roots a share of it. */
	static final double ROOT_DROP = 72;
	/** Each root's pieces, as fractions of the way down it; its bones begin at every second. */
	static final double[] ROOT_STEPS = {0, 0.12, 0.27, 0.44, 0.62, 0.81, 1.0};

	/**
	 * A root's line {@code s} of the way down it (0 at the knot, 1 at its tip): one that reaches the land ({@code k} below {@link #ROOTS}) out from
	 * the knot and then plunging into it; one that only hangs, shorter, out and down, dangling.
	 */
	static double[] rootAt(int k, double s) {
		boolean reaches = k < ROOTS;
		int i = reaches ? k : k - ROOTS;
		double th = Math.toRadians(reaches ? 360.0 * i / ROOTS + 14 * Math.sin(i * 2.3) : 360.0 * (i + 0.5) / HANGING + 20 * Math.sin(i * 1.7));
		// The hanging ones half to nine tenths of the way down to the land (a quarter to half before 2026-10-24: the user's ask for longer roots).
		double reach = reaches ? 34 + 26 * SpellArt.hash(i, 3, 91) : 12 + 22 * SpellArt.hash(i, 5, 91);
		double depth = reaches ? ROOT_DROP + 2 : ROOT_DROP * (0.5 + 0.4 * SpellArt.hash(i, 7, 91));
		double out = reach * Math.pow(Math.sin(s * Math.PI / 2), reaches ? 1.4 : 1.1), down = depth * Math.pow(s, reaches ? 1.3 : 1.15);
		double wobble = (reaches ? 3.5 : 2.5) * Math.sin(s * 6 + k * 1.3);
		return SpellArt.v3(Math.sin(th) * out + Math.cos(th) * wobble, -down, Math.cos(th) * out - Math.sin(th) * wobble);
	}

	/** A root's bone name: "root_<k>" for one that reaches the land, "hang_<k>" for one that only hangs, "_1" and "_2" down it. */
	static String rootBone(int k, int j) {
		String n = k < ROOTS ? "root_" + k : "hang_" + (k - ROOTS);
		return j == 0 ? n : n + "_" + j;
	}

	/**
	 * Void Withering's roots, spilling out of the rift they come through (just over the origin): a gnarled knot of the void tree's black wood
	 * ("knot") and fourteen roots ("hang_<k>", each of three bones "", "_1", "_2" down it) hanging from it, dangling, half to nine tenths of
	 * the way down to the land; threads of green-white life winding up them all. (Great roots reaching the land, "root_<k>", if {@link #ROOTS} had any:
	 * since 2026-10-24 the roots that reach out are the spell's grasping ones, drawn by hand.)
	 */
	static void voidRoots(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone knot = m.bone("knot", "root", 0, 0, 0);
		for (int k = 0; k < 9; k++) {
			double a = k * 40;
			knot.box(Math.sin(Math.toRadians(a)) * 4, -2 + (k % 3) * 1.6, Math.cos(Math.toRadians(a)) * 4, 9 - k % 3, 8 + k % 2 * 2, 9 - k % 2, voidBark(500 + k))
					.rot(k * 23, a, k * 17).density(1.25);
		}
		for (int k = 0; k < ROOTS + HANGING; k++) {
			boolean reaches = k < ROOTS;
			String up = "knot";
			for (int j = 0; j < 3; j++) {
				double[] at = rootAt(k, ROOT_STEPS[j * 2]);
				SpellArt.Bone seg = m.bone(rootBone(k, j), up, at[0], at[1], at[2]);
				for (int p = 0; p < 2; p++) {
					int i = j * 2 + p;
					double[] a = rootAt(k, ROOT_STEPS[i]), b = rootAt(k, ROOT_STEPS[i + 1]);
					// A little past each end, so the pieces meet at their bends.
					double[] d = SpellArt.sub(b, a), ext = SpellArt.times(d, 0.6 / Math.max(1e-6, SpellArt.len(d)));
					double thick = (reaches ? 7.4 : 4.2) * (1 - 0.8 * ROOT_STEPS[i] * (reaches ? 0.95 : 1)) + 0.6;
					SpellArt.limb(seg, SpellArt.sub(a, ext), SpellArt.add(b, ext), thick, thick * 0.9, voidBark(600 + k * 7 + i)).density(thick > 4 ? 1.5 : 2.2);
					// A rootlet off its joint, out and down.
					if (reaches && i > 0 && i % 2 == 1) {
						double side = (k + i) % 2 == 0 ? 1 : -1;
						double[] off = SpellArt.add(a, SpellArt.v3(side * (4 + i * 0.6), -7 - i, side * (2 - i * 0.5)));
						SpellArt.limb(seg, a, off, 1.6, 1.4, voidBark(700 + k * 7 + i)).density(3);
					}
				}
				if (j == 2) {
					double[] tip = rootAt(k, 1);
					if (reaches) seg.box(tip[0], tip[1] + 0.8, tip[2], 3.4, 3.4, 3.4, rootTip()).density(3);
					else SpellArt.limb(seg, tip, SpellArt.add(tip, SpellArt.v3(0, -4, 0)), 1.2, 1.2, voidBark(800 + k)).density(3);
				}
				up = seg.name;
			}
		}
		descend(m);
		drink(m);
		withdraw(m);
	}

	/** Its coming: the knot swells out of the rift, and the roots grow down out of it, each a little after the last. */
	static void descend(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("descend", L, null);
		a.scale("knot", 0, 0.3);
		a.scale("knot", 0.4, 1.05);
		a.scale("knot", L, 1);
		for (int k = 0; k < ROOTS + HANGING; k++) {
			String n = rootBone(k, 0);
			double t0 = 0.05 + 0.02 * k;
			a.scale(n, 0, 0.02);
			a.scale(n, t0, 0.02);
			a.sample(n, "scale", t0, Math.min(L, t0 + 0.6), 5, t -> {
				double e = SpellArt.easeOut((t - t0) / 0.6);
				return new double[] {Math.max(0.02, e), Math.max(0.02, e), Math.max(0.02, e)};
			});
			a.scale(n, L, 1);
		}
	}

	/** Its drinking, looped without a seam: the roots swaying (the hanging ones swinging free), a swell running up each as it swallows; the knot pulsing. */
	static void drink(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("drink", T, "true");
		for (int k = 0; k < ROOTS + HANGING; k++) {
			double sway = k < ROOTS ? 1.8 : 6;
			for (int j = 0; j < 3; j++) {
				String n = rootBone(k, j);
				int kk = k, jj = j;
				a.sample(n, "rotation", 0, T, 8, t -> new double[] {sway * Math.sin(2 * Math.PI * t / T + kk + jj * 0.8), 0, sway * Math.sin(2 * Math.PI * t / T + kk * 1.7 + jj * 0.8)});
				if (j > 0) a.sample(n, "scale", 0, T, 8, t -> {
					double s = 1 + 0.06 * Math.sin(4 * Math.PI * t / T + jj * 1.4 + kk);
					return new double[] {s, 1, s};
				});
			}
		}
		a.sample("knot", "scale", 0, T, 8, t -> {
			double s = 1 + 0.05 * Math.sin(4 * Math.PI * t / T);
			return new double[] {s, s, s};
		});
	}

	/** Its drawing back (held at its end): the roots pulled up into the knot, the knot back into the rift. */
	static void withdraw(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("withdraw", L, "hold_on_last_frame");
		for (int k = 0; k < ROOTS + HANGING; k++) {
			String n = rootBone(k, 0);
			a.sample(n, "scale", 0, L * 0.85, 5, t -> {
				double e = 1 - SpellArt.easeIn(t / (L * 0.85));
				return new double[] {Math.max(0.02, e), Math.max(0.02, e), Math.max(0.02, e)};
			});
			a.scale(n, L, 0.02);
		}
		a.scale("knot", 0, 1);
		a.scale("knot", 0.6, 1);
		a.scale("knot", L, 0.15);
	}

	// --- Golden Crow Descends: the crow ---

	/** The crow's eye on the outer side of its cube: an almond of white fire in its blaze ({@code around}). */
	static SpellArt.Painter crowEye(SpellArt.Painter around) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return around.paint(t);
			double x = (t.back() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = Math.abs(y) - (1 - x * x) * 0.65;
			if (shape > 0.05) return around.paint(t);
			if (Math.hypot(x + 0.2, y) < 0.18) return SpellArt.col(SUN_EMBER);
			return SpellArt.glow(SpellArt.mix(SUN_WHITE, SUN_PALE, SpellArt.clamp01(shape + 0.5)));
		};
	}

	/** The crow's beak: white fire from its root at z {@code rootZ} to its point at {@code tipZ}, its gape a line of ember. */
	static SpellArt.Painter crowBeak(double rootZ, double tipZ) {
		return t -> {
			double along = SpellArt.clamp01((t.qz() - rootZ) / (tipZ - rootZ));
			if (t.side() && Math.abs(t.up() - 0.3) < 0.06) return SpellArt.glow(SUN_DEEP, 0.95);
			double flicker = SpellArt.noise(t.qx() * 1.5 + t.qy(), t.qz() * 1.2, 5);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.62 + 0.38 * along + (flicker - 0.5) * 0.2), SUN_HEAT), 0.95);
		};
	}

	/** A talon: white fire at its point (the top of its cube), gold and ember down it. */
	static SpellArt.Painter crowTalon() {
		return t -> SpellArt.glow(SpellArt.ramp(0.35 + 0.65 * t.up(), SUN_HEAT), 0.95);
	}

	/**
	 * The sun's face behind the crow (on a sheet): a round of red fire, its rim burning gold, its heart deep red cooling to ember at its edge,
	 * swirls of flame in it; flames licking out round its rim.
	 */
	static SpellArt.Painter sunFace(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), ang = Math.atan2(z, x);
			double lick = 0.84 + 0.12 * SpellArt.fbm(ang * 3 + seed, 0.5, seed, 2);
			if (r > lick) return SpellArt.CLEAR;
			if (r > 0.8) {
				double heat = (lick - r) / (lick - 0.8);
				return SpellArt.glow(SpellArt.ramp(heat, SUN_DEEP, SUN_ORANGE, SUN_GOLD), 0.3 + 0.6 * heat);
			}
			if (r > 0.74) return SpellArt.glow(SpellArt.mix(SUN_GOLD, SUN_PALE, (r - 0.74) * 16), 0.95);
			double swirl = SpellArt.fbm(x * 3 + Math.sin(r * 9 + seed), z * 3 + Math.cos(r * 7), seed, 3);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.55 - r * 0.35 + (swirl - 0.5) * 0.6), SUN_FACE), 0.86);
		};
	}

	static final double[] CROW_TAIL = {22, 24, 25.5, 26, 25.5, 24, 22};

	/**
	 * The Golden Crow (金乌), the three-legged crow that carries the sun across the sky, made of the sun's own fire, some forty blocks from
	 * wingtip to wingtip, facing -z: its body ("body") a blaze of gold and white; its thick neck ("neck") and its head ("head") with a heavy beak
	 * of white fire ("beak_low" its lower half) and eyes of white fire; two broad wings ("wing_l", "wing_r", each with its "_mid" and "_tip"),
	 * their ends splayed into the long fingers of a crow's wing; a fan of a tail ("tail"); its three legs ("leg_0".."leg_2", the third between the
	 * other two), talons of white fire; and the sun it carries burning at its back ("sun": a red sun's face and a ring of flames round it,
	 * "sun_flames").
	 */
	static void goldenCrow(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 0, 0);
		SpellArt.Painter fire = blazeOf(0.5, 61, SUN_HEAT);
		body.box(0, 0, -6, 13, 11, 14, fire);
		body.box(0, 0.5, -6, 10, 13, 12.6, fire);
		body.box(0, -3.8, -7, 8.6, 4, 10, fire);
		body.box(0, 0.6, 4, 11, 9.6, 11, fire);
		body.box(0, 1.2, 12.4, 7.6, 7, 8, fire);
		// Flames streaming back off its back and flanks, where its feathers would lie.
		double[] rowZ = {-11, -5, 1, 7, 12}, rowR = {7.6, 7.4, 6.8, 6.2, 5};
		for (int row = 0; row < rowZ.length; row++) {
			for (int k = -2; k <= 2; k++) {
				double phi = k * 34;
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * rowR[row], 0.5 + Math.cos(Math.toRadians(phi)) * rowR[row], rowZ[row]);
				int variant = (row + Math.abs(k)) % 3;
				SpellArt.shared(m, body, "back_flame_" + variant, true, base, 11 + variant, 6, 20, 0, phi, featherOf(2, 70 + variant, SUN_HEAT), 2.5, k < 0);
			}
		}
		crowHead(m);
		crowWing(m, 1);
		crowWing(m, -1);
		// The tail: a fan of broad feathers of flame.
		SpellArt.Bone tail = m.bone("tail", "body", 0, 1.6, 15);
		for (int k = 0; k < CROW_TAIL.length; k++) {
			int off = Math.abs(k - 3);
			SpellArt.shared(m, tail, "tail_" + off, true, SpellArt.v3((k - 3) * 0.9, 1.8 + (3 - off) * 0.1, 14.5), CROW_TAIL[k], 6.4, -3, (k - 3) * 10, 0,
					featherOf(2, 90 + off, SUN_HEAT), 2, k < 3);
		}
		crowLegs(m);
		// The sun it carries, at its back: its face (a sheet under the body, seen behind it from above) and a ring of flames round it.
		SpellArt.Bone sun = m.bone("sun", "root", 0, -1.5, 2);
		SpellArt.Bone flames = m.bone("sun_flames", "sun", 0, -1.5, 2);
		for (int k = 0; k < 16; k++) {
			double th = 360.0 * k / 16 + 11, x = Math.sin(Math.toRadians(th)) * 21, z = 2 + Math.cos(Math.toRadians(th)) * 21;
			SpellArt.shared(m, flames, "sun_flame_" + k % 2, true, SpellArt.v3(x, -1.5, z), 13 + (k % 2) * 3, 8, -6, th, 0, Fire.flame(2.4, 120 + k % 2, SUN_HEAT), 2, false);
		}
		SpellArt.sheet(sun, 0, -1.5, 2, 44, 44, 0, 0, 0, sunFace(131)).density(1.5);
		emerge(m);
		crowDive(m);
		crowStrike(m);
	}

	static void crowHead(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 2.5, -11);
		SpellArt.Painter fire = blazeLimbOf(62, SUN_HEAT);
		double[] from = SpellArt.v3(0, 2, -10), to = SpellArt.v3(0, 5.4, -18.6);
		SpellArt.limb(neck, from, to, 8, 7.4, fire);
		SpellArt.limb(neck, from, to, 6.4, 9, fire);
		// Its hackles: a ruff of flames bristling round the throat.
		for (int k = -4; k <= 4; k++) {
			double phi = 180 + k * 26;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 4.4, 3.8 + Math.cos(Math.toRadians(phi)) * 4.2, -15.5 + Math.abs(k) * 0.4);
			SpellArt.shared(m, neck, "hackle_" + Math.abs(k) % 2, true, base, 8, 3.6, 30, 0, phi, featherOf(2, 140 + Math.abs(k) % 2, SUN_HEAT), 3, k < 0);
		}
		SpellArt.Bone head = m.bone("head", "neck", 0, 6, -18);
		SpellArt.Painter face = blazeOf(7, 63, SUN_HEAT);
		head.box(0, 7.2, -22.5, 8, 7.6, 9, face);
		head.box(0, 7.6, -22, 6.4, 9, 8, face);
		for (int s = -1; s <= 1; s += 2) head.box(s * 4.05, 8.4, -24, 0.6, 2.6, 3.4, crowEye(face)).density(4);
		// The beak: heavy and straight, a crow's, drawn to a point; its lower half its own bone.
		SpellArt.Painter beak = crowBeak(-26.5, -36);
		head.box(0, 7.8, -29.4, 4.6, 3.6, 6.4, beak);
		head.box(0, 7.4, -33.6, 3.4, 2.6, 3.4, beak);
		SpellArt.limb(head, SpellArt.v3(0, 7.6, -35), SpellArt.v3(0, 6.6, -37.6), 1.8, 1.6, beak).density(3);
		SpellArt.Bone low = m.bone("beak_low", "head", 0, 5.8, -26.6);
		low.box(0, 5.4, -29.8, 4, 1.6, 6.6, beak);
		low.box(0, 5.3, -33.6, 2.6, 1.2, 2.4, beak);
		// Flames streaming back from its crown and from behind its eyes.
		for (int k = 0; k < 3; k++) {
			SpellArt.blade(head, SpellArt.v3((k - 1) * 1.4, 11.6, -22 + Math.abs(k - 1)), 11 - Math.abs(k - 1) * 2, 4.4, -60 - Math.abs(k - 1) * 8, (k - 1) * 14, 0,
					Fire.flame(2, 150 + k, SUN_HEAT)).density(2.5);
		}
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.blade(head, SpellArt.v3(s * 4.2, 9, -21.6), 9, 3.6, -78, s * 20, s * 12, Fire.flame(2, 160 + s, SUN_HEAT)).density(2.5);
		}
	}

	static void crowWing(SpellArt.Model m, int s) {
		String w = "wing_" + (s > 0 ? "l" : "r");
		boolean flip = s < 0;
		double[][] edge = {SpellArt.v3(s * 6, 3.6, -7), SpellArt.v3(s * 19, 5, -4), SpellArt.v3(s * 32, 5.8, -6), SpellArt.v3(s * 40, 6, -5)};
		SpellArt.Bone[] bones = {m.bone(w, "body", edge[0][0], edge[0][1], edge[0][2], 0, 0, -s * 6), m.bone(w + "_mid", w, edge[1][0], edge[1][1], edge[1][2]),
				m.bone(w + "_tip", w + "_mid", edge[2][0], edge[2][1], edge[2][2])};
		SpellArt.Painter arm = blazeLimbOf(64, SUN_HEAT);
		bones[0].box(edge[0][0] + s * 1.4, edge[0][1], edge[0][2] + 1.5, 6.6, 5.4, 8, blazeOf(0.5, 61, SUN_HEAT));
		SpellArt.limb(bones[0], edge[0], edge[1], 5, 3.8, arm);
		SpellArt.limb(bones[1], edge[1], edge[2], 4.2, 3, arm);
		SpellArt.limb(bones[2], edge[2], edge[3], 3.2, 2.4, arm);
		double l0 = SpellArt.len(SpellArt.sub(edge[1], edge[0])), l1 = SpellArt.len(SpellArt.sub(edge[2], edge[1])), l2 = SpellArt.len(SpellArt.sub(edge[3], edge[2]));
		double wrist = (l0 + l1) / (l0 + l1 + l2);
		// The secondaries: broad flames trailing back along the arm.
		for (int k = 0; k < 10; k++) {
			double[] at = Fire.along(edge, 0.04 + (wrist - 0.04) * k / 9.0);
			SpellArt.shared(m, bones[(int) at[3]], "secondary_" + k % 5, true, SpellArt.v3(at[0], at[1] - 0.6, at[2] + 1.4), 19 + 0.4 * k, 7.6, -3, s * (k * 0.5 - 2), 0,
					featherOf(2, 170 + k % 5, SUN_HEAT), 2, flip);
		}
		// The crow's fingers: six long primaries splayed apart at the wing's end, gaps between them.
		double[] lengths = {24, 27, 29, 30, 29, 26};
		for (int k = 0; k < 6; k++) {
			double[] at = Fire.along(edge, wrist + (1 - wrist) * k / 5.0);
			SpellArt.shared(m, bones[2], "finger_" + k, true, SpellArt.v3(at[0], at[1] - 0.9, at[2] + 0.8), lengths[k], 5.2, -2 + k * 0.6, s * (18 + k * 11), 0,
					featherOf(k > 3 ? 3 : 2, 180 + k, SUN_HEAT), 2, flip);
		}
		// Coverts over the feathers' roots along the leading edge.
		for (int k = 0; k < 12; k++) {
			double f = 0.04 + 0.92 * k / 11.0;
			double[] at = Fire.along(edge, f);
			double heading = f > wrist ? s * (10 + (f - wrist) / (1 - wrist) * 36) : s * (k * 0.5 - 1);
			SpellArt.shared(m, bones[(int) at[3]], "covert_" + k % 3, true, SpellArt.v3(at[0], at[1] + 0.6, at[2] + 1), 10.5, 5.4, -4, heading, 0, featherOf(2, 190 + k % 3, SUN_HEAT), 2, flip);
		}
	}

	/** Its three legs: two either side and the third between them, tucked back under it, each a thigh of flame, a shank and four talons of white fire. */
	static void crowLegs(SpellArt.Model m) {
		double[][] hips = {SpellArt.v3(3.4, -4, 2), SpellArt.v3(-3.4, -4, 2), SpellArt.v3(0, -4.6, 6)};
		for (int i = 0; i < hips.length; i++) {
			double[] hip = hips[i];
			SpellArt.Bone leg = m.bone("leg_" + i, "body", hip[0], hip[1], hip[2]);
			double[] knee = SpellArt.add(hip, SpellArt.v3(0, -3.4, 2.2)), ankle = SpellArt.add(knee, SpellArt.v3(0, -5.4, 2.6));
			SpellArt.limb(leg, hip, knee, 3, 3.4, blazeLimbOf(200 + i, SUN_HEAT));
			SpellArt.limb(leg, knee, ankle, 1.6, 1.6, crowTalon()).density(3);
			for (int toe = 0; toe < 4; toe++) {
				double heading = toe == 3 ? 180 : (toe - 1) * 22;
				double[] dir = SpellArt.turn(SpellArt.v3(0, 0, -1), -30, heading, 0);
				double[] tip = SpellArt.add(ankle, SpellArt.times(dir, toe == 3 ? 2.6 : 3.6));
				SpellArt.limb(leg, ankle, tip, 0.9, 0.9, crowTalon()).density(4);
			}
		}
	}

	/** Its bursting out of the rift: its wings folded tight unfold into its stoop, its legs swing back, the sun kindles behind it. */
	static void emerge(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("emerge", 0.6, null);
		double[] t = {0, 0.2, 0.45, 0.6};
		double[] open = {0, 0.3, 1.08, 1};
		for (int i = 0; i < t.length; i++) {
			double o = open[i], fold = 1 - o;
			for (int s = -1; s <= 1; s += 2) {
				String w = "wing_" + (s > 0 ? "l" : "r");
				a.rot(w, t[i], 0, -s * (DIVE_SWEEP + 40 * fold), s * (DIVE_DIP + 50 * fold));
				a.rot(w + "_mid", t[i], 0, -s * (DIVE_MID + 60 * fold), 0);
				a.rot(w + "_tip", t[i], 0, -s * (DIVE_TIP + 40 * fold), 0);
			}
			a.scale("sun", t[i], Math.max(0.02, o));
		}
	}

	/** The stoop's pose: the wings swept back (degrees about y at the shoulder, the elbow, the hand) and held a little low. */
	static final double DIVE_SWEEP = 20, DIVE_MID = 12, DIVE_TIP = 9, DIVE_DIP = -5;

	/** Its stoop, looped without a seam: wings swept back and trembling in the rush of its fall, the fingers fluttering, the tail and legs streaming, the sun turning. */
	static void crowDive(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dive", T, "true");
		int steps = 40;
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			// Half closed, still beating in the rush, the hand whipping after the arm (the user's ask, 2026-10-25: the beasts too rigid).
			a.sample(w, "rotation", 0, T, steps, t -> new double[] {2.5 * Math.sin(2 * Math.PI * t / T * 5), -ss * (DIVE_SWEEP + 6 * Math.sin(2 * Math.PI * t / T * 5 + 0.5)), ss * (DIVE_DIP + 14 * Math.sin(2 * Math.PI * t / T * 5))});
			a.sample(w + "_mid", "rotation", 0, T, steps, t -> new double[] {0, -ss * DIVE_MID, ss * 11 * Math.sin(2 * Math.PI * t / T * 5 - 0.9)});
			a.sample(w + "_tip", "rotation", 0, T, steps, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T * 10 + 1), -ss * DIVE_TIP, ss * (13 * Math.sin(2 * Math.PI * t / T * 5 - 1.7) + 3 * Math.sin(2 * Math.PI * t / T * 10 - 0.9))});
		}
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {6 * Math.sin(2 * Math.PI * t / T * 5 + 2), 4 * Math.sin(2 * Math.PI * t / T * 5), 0});
		a.sample("neck", "rotation", 0, T, 8, t -> new double[] {4 + 1.5 * Math.sin(2 * Math.PI * t / T * 2), 0, 0});
		a.sample("head", "rotation", 0, T, 8, t -> new double[] {3 + 1.5 * Math.sin(2 * Math.PI * t / T * 2 + 1), 0, 0});
		a.sample("beak_low", "rotation", 0, T, 8, t -> new double[] {4 + 3 * Math.sin(2 * Math.PI * t / T * 2), 0, 0});
		for (int i = 0; i < 3; i++) {
			int ii = i;
			a.sample("leg_" + i, "rotation", 0, T, 16, t -> new double[] {38 + 4 * Math.sin(2 * Math.PI * t / T * 5 + ii), 0, 3 * Math.sin(2 * Math.PI * t / T * 5 + ii * 2)});
		}
		a.sample("sun_flames", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
		a.sample("sun", "scale", 0, T, 16, t -> {
			double p = 1 + 0.04 * Math.sin(2 * Math.PI * t / T * 4);
			return new double[] {p, 1, p};
		});
	}

	/** Its strike (held): its wings thrown forward and up to brake, its three legs thrust out ahead, talons spread, the sun at its back flaring wide. */
	static void crowStrike(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("strike", 1.0, "hold_on_last_frame");
		double[] t = {0, 0.12, 0.3, 0.6, 1.0};
		double[] flare = {0, 0.7, 1, 1, 1};
		double[] sun = {1, 1.5, 1.9, 1.75, 1.7};
		for (int i = 0; i < t.length; i++) {
			double f = flare[i];
			for (int s = -1; s <= 1; s += 2) {
				String w = "wing_" + (s > 0 ? "l" : "r");
				a.rot(w, t[i], -10 * f, -s * DIVE_SWEEP * (1 - f) + s * 30 * f, s * DIVE_DIP * (1 - f) - s * 36 * f);
				a.rot(w + "_mid", t[i], 0, -s * DIVE_MID * (1 - f) + s * 10 * f, -s * 8 * f);
				a.rot(w + "_tip", t[i], 0, -s * DIVE_TIP * (1 - f) + s * 8 * f, -s * 10 * f);
			}
			for (int leg = 0; leg < 3; leg++) a.rot("leg_" + leg, t[i], 38 * (1 - f) - 70 * f, 0, 0);
			a.rot("neck", t[i], 4 - 22 * f, 0, 0);
			a.rot("head", t[i], 3 - 10 * f, 0, 0);
			a.rot("beak_low", t[i], 4 + 26 * f, 0, 0);
			a.scale("sun", t[i], sun[i]);
		}
	}

	// --- Sun Pulled Through the Void: the sun ---

	/**
	 * The sun's face (on a solid): its burning surface in cells of rising fire, white-gold, ringed in darker lanes; a few dark spots; hotter
	 * where it faces up; glowing.
	 */
	static SpellArt.Painter sunSurface(int seed) {
		return t -> {
			double x = t.px / 7, y = t.py / 7, z = t.pz / 7;
			int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
			double d1 = 9, d2 = 9;
			for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
				int cx = ix + dx, cy = iy + dy, cz = iz + dz;
				double px = cx + SpellArt.hash3(cx, cy, cz, seed), py = cy + SpellArt.hash3(cx, cy, cz, seed + 1), pz = cz + SpellArt.hash3(cx, cy, cz, seed + 2);
				double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
				if (d < d1) {
					d2 = d1;
					d1 = d;
				} else if (d < d2) d2 = d;
			}
			double lane = d2 - d1, swirl = SpellArt.fbm3(t.px * 0.06, t.py * 0.06, t.pz * 0.06, seed + 3, 3);
			if (SpellArt.fbm3(t.px * 0.04, t.py * 0.04, t.pz * 0.04, seed + 7, 2) > 0.76) return SpellArt.glow(SpellArt.mix(SUN_DEEP, SUN_ORANGE, SpellArt.smooth(0, 0.4, lane)), 0.97);
			double heat = SpellArt.clamp01(0.7 + 0.18 * SpellArt.smooth(0.0, 0.35, lane) - 0.12 * d1 + (swirl - 0.5) * 0.5);
			return SpellArt.glow(SpellArt.ramp(heat, SUN_HEAT), 0.98);
		};
	}

	/** The sun's turbulence (on a shell over it): a few broad streaks of hotter plasma flowing round it, clear between them; glowing. */
	static SpellArt.Painter plasma(int seed) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.06, t.py * 0.04, t.pz * 0.06, seed, 3);
			double streak = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.09 + flow, t.py * 0.05, t.pz * 0.09, seed + 3, 2) - 1);
			if (streak < 0.8) return SpellArt.CLEAR;
			double heat = (streak - 0.8) / 0.2;
			return SpellArt.glow(SpellArt.ramp(0.6 + 0.4 * heat, SUN_HEAT), 0.3 + 0.55 * heat);
		};
	}

	/** A prominence (on a limb): a loop of fire arching off the sun, hot along its middle, its edges ragged and cooler; glowing. */
	static SpellArt.Painter prominence(int seed) {
		return t -> {
			double across = Math.abs(t.fx - 0.5) * 2, n = SpellArt.noise(t.ly * 0.8 + seed, t.fx * 4, seed);
			if (across > 0.8 + 0.2 * n) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(1 - across * 0.9 + (n - 0.5) * 0.3), SUN_HEAT), 0.55 + 0.4 * (1 - across));
		};
	}

	static final double SUN_R = 18;

	/** The shape of a ball of fire {@code r} round, for {@code b}: three boxes along its axes and three cubes turned, as near round as boxes go. */
	static void ball(SpellArt.Bone b, double r, SpellArt.Painter p, double tpp) {
		double c = 1.42 * r;
		b.box(0, 0, 0, 2 * r, c, c, p).density(tpp);
		b.box(0, 0, 0, c, 2 * r, c, p).density(tpp);
		b.box(0, 0, 0, c, c, 2 * r, p).density(tpp);
		double d = 1.62 * r;
		b.box(0, 0, 0, d, d, d, p).rot(0, 45, 0).density(tpp);
		b.box(0, 0, 0, d, d, d, p).rot(45, 0, 0).density(tpp);
		b.box(0, 0, 0, d, d, d, p).rot(0, 0, 45).density(tpp);
		double e = 1.5 * r;
		b.box(0, 0, 0, e, e, e, p).rot(35.26, 45, 0).density(tpp);
		b.box(0, 0, 0, e, e, e, p).rot(-35.26, 45, 0).density(tpp);
	}

	/**
	 * The sun dragged through the void, some sixteen blocks across, at its middle: its burning face ("core"), two shells of plasma turning over
	 * it each its own way ("surface_0", "surface_1"), flames standing off it all round ("flares"), four loops of fire arching off it
	 * ("prominence_0".."prominence_3"), and its corona, longer streamers of flame between the flares ("glow").
	 */
	static void voidSun(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 0, 0);
		ball(core, SUN_R, sunSurface(211), 1.25);
		SpellArt.Bone tilt = m.bone("surface_tilt", "root", 0, 0, 0, 30, 0, 20);
		SpellArt.Bone s0 = m.bone("surface_0", "root", 0, 0, 0);
		SpellArt.Bone s1 = m.bone("surface_1", "surface_tilt", 0, 0, 0);
		ball(s0, SUN_R * 1.07, plasma(221), 1);
		ball(s1, SUN_R * 1.13, plasma(231), 1);
		// Flames standing off it all round (on a sphere's points, each aimed out from its heart).
		SpellArt.Bone flares = m.bone("flares", "root", 0, 0, 0);
		int n = 30;
		for (int k = 0; k < n; k++) {
			double y = 1 - 2 * (k + 0.5) / n, rr = Math.sqrt(1 - y * y), phi = k * 2.39996;
			double[] dir = SpellArt.v3(Math.cos(phi) * rr, y, Math.sin(phi) * rr);
			double[] aim = SpellArt.aimY(dir[0], dir[1], dir[2]);
			SpellArt.shared(m, flares, "flare_" + k % 3, false, SpellArt.times(dir, SUN_R * 0.82), 12 + (k % 3) * 2.5, 9, aim[0], aim[1], 0,
					Fire.flame(2.6, 240 + k % 3, SUN_HEAT), 1.5, false);
		}
		// Four loops of fire arching off it.
		double[][] arcs = {{20, 30, 0}, {140, -20, 60}, {250, 40, -40}, {320, -50, 20}};
		for (int k = 0; k < arcs.length; k++) {
			SpellArt.Bone arc = m.bone("prominence_" + k, "root", 0, 0, 0, arcs[k][1], arcs[k][0], arcs[k][2]);
			int seg = 7;
			double[] prev = null;
			for (int i = 0; i <= seg; i++) {
				double a = Math.PI * i / seg, lift = SUN_R * (0.9 + 0.55 * Math.sin(a));
				double[] p = SpellArt.v3(Math.cos(a) * 9, lift * 0.96, Math.sin(a) * 2.5);
				if (prev != null) SpellArt.limb(arc, prev, p, 3.2, 3.2, prominence(250 + k * 10 + i)).density(1.5);
				prev = p;
			}
		}
		// Its corona: long streamers of flame standing out further, between the flares (they burst out wide as it collapses).
		SpellArt.Bone glow = m.bone("glow", "root", 0, 0, 0);
		int streamers = 18;
		for (int k = 0; k < streamers; k++) {
			double y = 1 - 2 * (k + 0.5) / streamers, rr = Math.sqrt(1 - y * y), phi = k * 2.39996 + 1.2;
			double[] dir = SpellArt.v3(Math.cos(phi) * rr, y, Math.sin(phi) * rr);
			double[] aim = SpellArt.aimY(dir[0], dir[1], dir[2]);
			SpellArt.shared(m, glow, "streamer_" + k % 2, false, SpellArt.times(dir, SUN_R * 0.9), 26 + (k % 2) * 5, 7, aim[0], aim[1], 0,
					Fire.flame(1.6, 270 + k % 2, SUN_HEAT), 1.2, false);
		}
		sunEmerge(m);
		blaze(m);
		collapse(m);
	}

	/** Its coming through: swelling out of the rift, its flames and loops unfurling, its glow kindling. */
	static void sunEmerge(SpellArt.Model m) {
		double L = 1.2;
		SpellArt.Anim a = m.anim("emerge", L, null);
		a.sample("root", "scale", 0, L, 8, t -> {
			double s = 0.12 + 0.88 * SpellArt.easeOut(t / L);
			return new double[] {s, s, s};
		});
		a.scale("flares", 0, 0.3);
		a.scale("flares", 0.8, 1.12);
		a.scale("flares", L, 1);
		a.scale("glow", 0, 0.02);
		a.scale("glow", 0.5, 0.02);
		a.scale("glow", L, 1);
		for (int k = 0; k < 4; k++) {
			a.scale("prominence_" + k, 0, 0.1);
			a.scale("prominence_" + k, 0.6 + k * 0.1, 0.1);
			a.scale("prominence_" + k, L, 1);
		}
	}

	/** Its burning, looped without a seam: its face turning, its plasma swirling over it each shell its own way, its flames wheeling, its loops pulsing, its glow breathing. */
	static void blaze(SpellArt.Model m) {
		double T = 6;
		SpellArt.Anim a = m.anim("blaze", T, "true");
		a.sample("core", "rotation", 0, T, 12, t -> new double[] {0, 360 * t / T, 0});
		a.sample("surface_0", "rotation", 0, T, 12, t -> new double[] {0, -360 * t / T, 0});
		a.sample("surface_1", "rotation", 0, T, 12, t -> new double[] {0, 720 * t / T, 0});
		a.sample("flares", "rotation", 0, T, 12, t -> new double[] {0, 360 * t / T, 0});
		a.sample("flares", "scale", 0, T, 24, t -> {
			double p = 1 + 0.06 * Math.sin(2 * Math.PI * t / T * 6);
			return new double[] {p, p, p};
		});
		for (int k = 0; k < 4; k++) {
			int kk = k;
			a.sample("prominence_" + k, "scale", 0, T, 12, t -> {
				double p = 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 2 + kk * 1.6);
				return new double[] {p, p, p};
			});
		}
		a.sample("glow", "scale", 0, T, 12, t -> {
			double p = 1 + 0.05 * Math.sin(2 * Math.PI * t / T * 3);
			return new double[] {p, p, p};
		});
	}

	/** Its collapse (held): it shrinks in on itself, its flames sucked in after it; then it bursts, its glow flaring out wide. */
	static void collapse(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("collapse", 1.0, "hold_on_last_frame");
		a.sample("root", "scale", 0, 0.55, 6, t -> {
			double s = 1 - 0.7 * SpellArt.easeIn(t / 0.55);
			return new double[] {s, s, s};
		});
		a.scale("root", 0.7, 0.62);
		a.scale("root", 1.0, 0.7);
		a.scale("flares", 0, 1);
		a.scale("flares", 0.5, 0.15);
		a.scale("flares", 0.62, 2.2);
		a.scale("flares", 1.0, 2.4);
		a.scale("glow", 0, 1);
		a.scale("glow", 0.55, 0.5);
		a.scale("glow", 0.72, 2.6);
		a.scale("glow", 1.0, 2.9);
	}

	// --- Buzhou Falls: the broken pillar ---

	/** The pillar's ancient stone: blue-grey, and the earth's molten gold. */
	static final int PILLAR_DARK = 0x2C3036, PILLAR = 0x58606A, PILLAR_LIGHT = 0x8E96A0, PILLAR_PALE = 0xC8CCD0, BRONZE = 0x6E4E22, VERDIGRIS = 0x4E8C78,
			CLOUD_LIT = 0xFFF6DE, CLOUD_SHADE = 0xBCB8B0;

	/**
	 * The pillar's stone (round a shaft standing up its y): ancient blue-grey stone carved in relief, a dragon's scaled coils winding up it (a
	 * band of scales round it, climbing), cloud scrolls between its turns; worn, cracked, cracks of molten gold through it where it broke.
	 */
	static SpellArt.Painter pillarStone(int seed) {
		return t -> {
			double ang = Math.atan2(t.qx(), t.qz()) / (2 * Math.PI) + 0.5, y = t.qy();
			// The dragon: a band winding up, three turns over the shaft, its scales in rows across it.
			double turn = (y / 34 - ang) % 1.0, band = turn < 0 ? turn + 1 : turn;
			boolean coil = band < 0.32;
			// Cracks of molten gold: thin, and only here and there (thicker toward its broken top).
			double crack = 1 - Math.abs(2 * SpellArt.noise3(t.px * 0.22, t.py * 0.09, t.pz * 0.22, seed + 3) - 1);
			double where = SpellArt.noise3(t.px * 0.05, t.py * 0.03, t.pz * 0.05, seed + 5) + 0.25 * SpellArt.smooth(70, 104, y);
			if (where > 0.62 && crack > 0.955) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, (crack - 0.955) * 20), 0.95);
			double n = SpellArt.fbm3(t.px * 0.2, t.py * 0.2, t.pz * 0.2, seed, 3);
			if (coil) {
				double row = band / 0.32 * 3, along = ang * 40 + Math.floor(row) * 0.5, f = along - Math.floor(along), r = row - Math.floor(row);
				double rim = Math.min(Math.abs(f - 0.5) * 2, 1 - r);
				int c = SpellArt.mix(PILLAR, PILLAR_LIGHT, 0.4 + 0.4 * n);
				if (band < 0.03 || band > 0.29) return SpellArt.col(SpellArt.mix(PILLAR_DARK, PILLAR, 0.4));
				return SpellArt.col(rim < 0.14 ? SpellArt.shade(c, -0.35) : SpellArt.shade(c, 0.12 * (1 - rim)));
			}
			// Cloud scrolls between the coils: spirals cut into it.
			double cx = ang * 12, cy = (band - 0.32) / 0.68 * 2;
			double ux = cx - Math.floor(cx) - 0.5, uy = cy - Math.floor(cy) - 0.5, r = Math.hypot(ux, uy);
			int c = SpellArt.ramp(SpellArt.clamp01(0.35 + (n - 0.5) * 0.7 + 0.15 * Math.max(0, t.ny)), PILLAR_DARK, PILLAR, PILLAR_LIGHT, PILLAR_PALE);
			if (r < 0.42) {
				double th = Math.atan2(uy, ux) / (2 * Math.PI) + 0.5, s = r / 0.1 - th;
				if (s - Math.floor(s) < 0.25) return SpellArt.col(SpellArt.shade(c, -0.32));
			}
			return SpellArt.col(c);
		};
	}

	/** A band of bronze round the pillar: green with age, its rim worn bright, a row of glyphs round it burning with the earth's gold. */
	static SpellArt.Painter bronzeBand(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 2);
			if (t.flat()) return SpellArt.col(SpellArt.mix(BRONZE, VERDIGRIS, n));
			double u = t.up();
			if (u < 0.16 || u > 0.84) return SpellArt.col(SpellArt.mix(BRONZE, Earth.GOLD_PALE, 0.35));
			double cell = t.fx * 3;
			int ci = (int) Math.floor(cell);
			if (Fire.glyph(ci + t.f.ordinal() * 3, seed, cell - ci, (u - 0.16) / 0.68)) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, 0.3), 0.97);
			return SpellArt.col(SpellArt.mix(BRONZE, VERDIGRIS, 0.3 + 0.6 * n));
		};
	}

	/** The stone where it broke (its broken top): fresh, paler, splintered, cracks of molten gold through it. */
	static SpellArt.Painter brokenStone(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.35, t.py * 0.35, t.pz * 0.35, seed, 3);
			double crack = 1 - Math.abs(2 * SpellArt.noise3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed + 3) - 1);
			if (crack > 0.94) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, (crack - 0.94) * 16), 0.95);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.45 + (n - 0.5) * 0.8 + 0.2 * Math.max(0, t.ny)), PILLAR, PILLAR_LIGHT, PILLAR_PALE));
		};
	}

	/** A billow of heaven's cloud that wreathes the pillar (on a cube): white lit gold on its top, grey beneath, soft at its edges; seen through. */
	static SpellArt.Painter heavenCloud(int seed) {
		return t -> {
			double billow = SpellArt.fbm3(t.px * 0.15, t.py * 0.2, t.pz * 0.15, seed, 3);
			double light = SpellArt.clamp01(0.4 + billow * 0.5 + 0.3 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny));
			int c = SpellArt.mix(CLOUD_SHADE, CLOUD_LIT, light);
			double edge = Math.min(Math.min(t.fx, 1 - t.fx), Math.min(t.fy, 1 - t.fy)) * 2;
			double a = (0.5 + 0.3 * light) * (0.2 + 0.8 * SpellArt.smooth(0, 0.8, edge + (billow - 0.5) * 0.8));
			return SpellArt.col(c, a);
		};
	}

	/** A chain's link of bronze, worn bright at its edges. */
	static SpellArt.Painter link() {
		return t -> SpellArt.col(SpellArt.mix(BRONZE, Earth.GOLD_PALE, Math.min(t.fx, 1 - t.fx) < 0.2 ? 0.5 : 0.15));
	}

	static final double PILLAR_R = 13, PILLAR_TOP = 104;

	/**
	 * The broken pillar of Mount Buzhou (不周山), that held up heaven until Gonggong broke it, fifty-nine blocks long, its foot at the origin:
	 * its plinth ("pillar": its carved foot, its eight-sided shaft carved with a dragon coiling up it among clouds, three bands of bronze written
	 * round with glyphs of the earth's gold), its top broken off in jagged stone, molten gold in its cracks; heaven's clouds still wreathing it
	 * ("clouds"); the two chains that bound it, broken, swinging from its upper band ("chain_l_0".."_3", "chain_r_0".."_3").
	 */
	static void buzhouPillar(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone pillar = m.bone("pillar", "root", 0, 0, 0);
		SpellArt.Painter carved = pillarStone(301), rock = brokenStone(303);
		// The plinth: a square foot, stepped, its lotus moulding.
		pillar.box(0, 3, 0, 36, 6, 36, rock);
		pillar.box(0, 8, 0, 31, 4, 31, rock).rot(0, 45, 0);
		pillar.box(0, 11, 0, 30, 3, 30, bronzeBand(305));
		// The shaft: eight-sided.
		double tall = PILLAR_TOP - 12;
		pillar.box(0, 12 + tall / 2, 0, 2 * PILLAR_R, tall, 2 * PILLAR_R * 0.83, carved).skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		pillar.box(0, 12 + tall / 2, 0, 2 * PILLAR_R, tall, 2 * PILLAR_R * 0.83, carved).rot(0, 45, 0).skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		pillar.box(0, 12 + tall / 2, 0, 2 * PILLAR_R * 0.83, tall, 2 * PILLAR_R, carved).skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		for (double y : new double[] {34, 64, 94}) {
			pillar.box(0, y, 0, 2 * PILLAR_R + 3, 5, 2 * PILLAR_R + 3, bronzeBand((int) y)).density(1.5);
			pillar.box(0, y, 0, 2 * PILLAR_R + 3, 5, 2 * PILLAR_R + 3, bronzeBand((int) y + 1)).rot(0, 45, 0).density(1.5);
		}
		// Its broken top: jagged stone, fallen in, molten gold in its cracks.
		SpellArt.Painter broken = brokenStone(307);
		double[][] chunks = {{0, 2, 0, 24, 6, 22, 0, 10, 0}, {-5, 7, 3, 13, 9, 12, 8, -20, 14}, {6, 9, -4, 11, 12, 10, -10, 30, -8}, {-2, 14, -6, 8, 10, 8, 14, 10, 20},
				{7, 15, 5, 7, 8, 7, -18, -15, -12}, {-8, 11, -7, 7, 9, 6, 20, 40, 10}, {2, 19, 1, 5, 7, 5, 10, 25, -14}};
		for (double[] c : chunks) pillar.box(c[0], PILLAR_TOP + c[1], c[2], c[3], c[4], c[5], broken).rot(c[6], c[7], c[8]);
		// Heaven's clouds still wreathing it.
		SpellArt.Bone clouds = m.bone("clouds", "pillar", 0, 78, 0);
		for (int k = 0; k < 10; k++) {
			double th = k * 36, r = 19 + (k % 3) * 2, y = 76 + (k % 4) * 3;
			clouds.box(Math.sin(Math.toRadians(th)) * r, y, Math.cos(Math.toRadians(th)) * r, 14, 7, 10, heavenCloud(320 + k)).rot(0, th, 0).density(1.25);
		}
		// The chains that bound it, broken off, hanging from its upper band.
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r";
			double[] at = SpellArt.v3(s * (PILLAR_R + 2.5), 93, 0);
			String up = "pillar";
			for (int k = 0; k < 4; k++) {
				SpellArt.Bone b = m.bone("chain_" + side + "_" + k, up, at[0], at[1], at[2]);
				double[] next = SpellArt.add(at, SpellArt.v3(s * 0.6, -5.5, 0));
				b.box(at[0], at[1] - 2.75, at[2], k % 2 == 0 ? 1.4 : 4, 6, k % 2 == 0 ? 4 : 1.4, link()).density(3);
				up = b.name;
				at = next;
			}
		}
		pillarFall(m);
		pillarImpact(m);
	}

	/** Its fall, looped without a seam: the chains swinging in the rush of air, the clouds about it wheeling, the pillar shuddering as it falls. */
	static void pillarFall(SpellArt.Model m) {
		double T = 8;
		SpellArt.Anim a = m.anim("fall", T, "true");
		a.sample("clouds", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
		a.sample("pillar", "rotation", 0, T, 32, t -> new double[] {1.2 * Math.sin(2 * Math.PI * t / T * 4), 0, 1.2 * Math.sin(2 * Math.PI * t / T * 4 + 1.3)});
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r";
			for (int k = 0; k < 4; k++) {
				int kk = k, ss = s;
				a.sample("chain_" + side + "_" + k, "rotation", 0, T, 32, t -> new double[] {10 * Math.sin(2 * Math.PI * t / T * 4 - kk * 0.7), 0, ss * (8 + 6 * Math.sin(2 * Math.PI * t / T * 4 - kk * 0.6 + ss))});
			}
		}
	}

	/** Its landing (held): it slams in and shudders, settling a little askew; the chains whip down; the clouds are flung out and sink away. */
	static void pillarImpact(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("impact", 1.2, "hold_on_last_frame");
		double[] t = {0, 0.05, 0.12, 0.2, 0.3, 0.45, 0.7, 1.2};
		double[] drop = {0, -3, 1.2, -0.8, 0.4, -0.2, 0, 0};
		double[] tilt = {0, 1.5, 3.5, 4.8, 5.4, 5.2, 5, 5};
		for (int i = 0; i < t.length; i++) {
			a.pos("pillar", t[i], 0, drop[i], 0);
			a.rot("pillar", t[i], tilt[i], 0, tilt[i] * 0.4);
		}
		a.scale("clouds", 0, 1);
		a.scale("clouds", 0.25, 1.7);
		a.scale("clouds", 1.2, 2.2);
		a.pos("clouds", 0, 0, 0, 0);
		a.pos("clouds", 1.2, 0, -34, 0);
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r";
			for (int k = 0; k < 4; k++) {
				a.rot("chain_" + side + "_" + k, 0, 0, 0, s * 8);
				a.rot("chain_" + side + "_" + k, 0.1, 30 - k * 8, 0, s * 30);
				a.rot("chain_" + side + "_" + k, 0.4, -12, 0, -s * 6);
				a.rot("chain_" + side + "_" + k, 1.2, 2, 0, s * 2);
			}
		}
	}

	// --- Earth Veins Lock the Void: the dome of the earth ---

	/** Its ribs of rock, and the boulders drifting round inside them. */
	static final int RIBS = 9, BOULDERS = 12;
	static final double DOME_R = 54, DOME_H = 38;

	/**
	 * The earth's own rock (a solid, painted where it lies in the model), as Earth#rock is but heavier and plainer: broad plates of grey-brown
	 * granite, umber and dark stone, now and then tan, each chiselled (lit toward its middle, shadowed to its edges) and gritty, dark cracks
	 * between them, and in some of the cracks the earth's molten gold showing through; darker beneath. {@code size} is a plate's breadth, in
	 * pixels.
	 */
	static SpellArt.Painter earthRock(double size, int seed) {
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
			if (gap < 0.07) {
				// A crack: dark, or running with the earth's molten gold where its fire shows through.
				if (SpellArt.noise3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed + 3) > 0.66) return SpellArt.glow(SpellArt.mix(Earth.EMBER, Earth.MOLTEN_CORE, 0.35 + gap * 6), 0.95);
				return SpellArt.col(Earth.CRACK);
			}
			double h = SpellArt.hash(cell, 1, seed);
			int c = h < 0.34 ? Earth.STONE : h < 0.58 ? SpellArt.mix(Earth.STONE_DARK, Earth.STONE, 0.45) : h < 0.78 ? SpellArt.mix(Earth.UMBER, Earth.STONE_DARK, 0.3)
					: h < 0.92 ? SpellArt.mix(Earth.STONE, Earth.STONE_LIGHT, 0.55) : SpellArt.mix(Earth.SANDSTONE, Earth.STONE, 0.4);
			double grit = SpellArt.noise3(t.px * 1.1, t.py * 1.1, t.pz * 1.1, seed + 4);
			c = SpellArt.shade(c, -0.24 + 0.36 * SpellArt.smooth(0.05, 0.35, gap) - 0.1 * d1 + (grit - 0.5) * 0.16);
			if (t.ny < -0.5) c = SpellArt.shade(c, -0.2);
			return SpellArt.col(c);
		};
	}

	/**
	 * A point on rib {@code k}, {@code s} of the way up it (0 at its foot on the rim, 1 where it meets the keystone): no true arc, but the earth
	 * heaved up, each rib its own way round (its feet unevenly spaced), its own height, wandering a little as it goes.
	 */
	static double[] ribAt(int k, double s) {
		double th = Math.toRadians(360.0 * k / RIBS + 11 * (SpellArt.hash(k, 3, 451) - 0.5) + 4 * Math.sin(s * 5.2 + k * 1.9));
		double a = s * Math.PI / 2, rim = DOME_R * (0.94 + 0.1 * SpellArt.hash(k, 5, 451));
		double r = rim * Math.cos(a) + 6 * Math.sin(a) + 2.2 * Math.sin(s * 7.3 + k * 2.6);
		double y = 1 + (DOME_H - 1) * Math.pow(Math.sin(a), 0.92) * (0.93 + 0.07 * SpellArt.hash(k, 7, 451)) + 1.6 * Math.sin(s * 9.1 + k);
		return SpellArt.v3(Math.sin(th) * r, y, Math.cos(th) * r);
	}

	/**
	 * The lock's ground (on a sheet over the land): the earth cracked open, cracks of molten amber running in from each rib's foot toward its heart,
	 * wandering and forking, the earth heaved up dark at its rim; clear elsewhere.
	 */
	static SpellArt.Painter crackedGround(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), ang = Math.atan2(x, z);
			if (r > 0.99) return SpellArt.CLEAR;
			// The earth heaved up at its rim: dark soil in ragged clods.
			double clod = SpellArt.fbm(x * 9 + seed, z * 9, seed, 2);
			if (r > 0.86 + 0.06 * clod && clod > 0.42) return SpellArt.col(SpellArt.mix(Earth.HOOF, Earth.UMBER, clod));
			for (int k = 0; k < RIBS; k++) {
				double a = Math.toRadians(360.0 * k / RIBS + 11 * (SpellArt.hash(k, 3, 451) - 0.5)) + 0.35 * (SpellArt.fbm(r * 3.5 + k, k, seed, 2) - 0.5) * 2;
				double d = Math.abs(Math.atan2(Math.sin(ang - a), Math.cos(ang - a))) * r;
				double forkA = a + (k % 2 == 0 ? 0.42 : -0.42) * Math.max(0, 0.7 - r), fork = r > 0.7 || r < 0.2 ? 9 : Math.abs(Math.atan2(Math.sin(ang - forkA), Math.cos(ang - forkA))) * r;
				double w = (0.008 + 0.018 * r) * (0.7 + 0.6 * SpellArt.noise(r * 14 + k, k, seed));
				double near = Math.min(d, fork * 1.4);
				if (near < w && r > 0.08) return SpellArt.glow(SpellArt.mix(Earth.EMBER, Earth.MOLTEN_CORE, 1 - near / w), 0.9);
				if (near < w * 2.4 && r > 0.08) return SpellArt.col(Earth.CRACK);
			}
			if (r < 0.07) return SpellArt.glow(Earth.MOLTEN, 0.7 * (1 - r / 0.07));
			return SpellArt.CLEAR;
		};
	}

	/** The keystone's rock ({@link #earthRock}), the earth's trigram ☷ (three broken lines) burning in molten gold on its underside. */
	static SpellArt.Painter keystone(int seed) {
		SpellArt.Painter stone = earthRock(5, seed), mark = trigram(new boolean[] {false, false, false}, seed, Earth.MOLTEN, Earth.MOLTEN_CORE);
		return t -> {
			if (t.ny < -0.5) {
				int p = mark.paint(t);
				if (p != SpellArt.CLEAR) return p;
			}
			return stone.paint(t);
		};
	}

	/**
	 * Earth Veins Lock the Void's dome, twenty-six blocks round, its foot at the origin, made of the earth itself (the user, 2026-10-24: the
	 * first, of glowing veins in arches and rings, "looks mechanical rather than earth-like"): nine ribs of rough rock heaved up out of the land
	 * round its rim and bent in to meet over it ("rib_0".."rib_8", each pivoting at its foot), craggy, thick at their feet and thinning, knuckled,
	 * cracked through with the earth's molten gold, amber crystals breaking out of them; rubble heaved up round each foot, shards of stone
	 * standing out of it ("rubble"); the keystone where they meet, a crag with the earth's trigram ☷ burning on its underside ("keystone");
	 * boulders torn from the land drifting round inside it ("boulders", each its own "boulder_<k>"); and the land under it cracked open, the
	 * earth's fire showing in the cracks ("ground", drawn last: see through).
	 */
	static void earthVeinLock(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone rubble = m.bone("rubble", "root", 0, 0, 0);
		for (int k = 0; k < RIBS; k++) {
			double[] foot = ribAt(k, 0);
			double th = Math.atan2(foot[0], foot[2]);
			for (int b = 0; b < 3; b++) {
				double s = 6 + 4 * SpellArt.hash(k, b, 461), out = (b - 1) * 5.5, back = (SpellArt.hash(k, b + 3, 461) - 0.3) * 7;
				double x = foot[0] + Math.cos(th) * out + Math.sin(th) * back, z = foot[2] - Math.sin(th) * out + Math.cos(th) * back;
				rubble.box(x, s * 0.35, z, s, s * 0.8, s * 0.9, earthRock(5.5, 470 + k * 3 + b)).rot(SpellArt.hash(k, b, 462) * 40 - 20, SpellArt.hash(k, b, 463) * 90,
						SpellArt.hash(k, b, 464) * 40 - 20).density(1.5);
			}
			// Shards of stone standing out of the rubble, leaning out from the dome.
			for (int b = 0; b < 2; b++) {
				double side = b == 0 ? 1 : -1;
				double[] at = SpellArt.v3(foot[0] + Math.cos(th) * side * 7, 0.5, foot[2] - Math.sin(th) * side * 7);
				SpellArt.blade(rubble, at, 9 + 4 * SpellArt.hash(k, b, 465), 4.2, -(18 + 14 * SpellArt.hash(k, b, 466)), Math.toDegrees(th) + side * 25, 0, Earth.shard(480 + k * 2 + b))
						.density(3);
			}
		}
		for (int k = 0; k < RIBS; k++) {
			double[] foot = ribAt(k, 0);
			SpellArt.Bone rib = m.bone("rib_" + k, "root", foot[0], foot[1], foot[2]);
			int pieces = 9;
			for (int i = 0; i < pieces; i++) {
				double s0 = i / (double) pieces, s1 = (i + 1) / (double) pieces;
				double[] a = ribAt(k, s0), b = ribAt(k, s1);
				double[] d = SpellArt.sub(b, a), ext = SpellArt.times(d, 1.1 / SpellArt.len(d));
				double girth = (10.5 - 6.2 * s0) * (0.85 + 0.3 * SpellArt.hash(k, i, 471));
				SpellArt.limb(rib, SpellArt.sub(a, ext), SpellArt.add(b, ext), girth, girth * (0.78 + 0.2 * SpellArt.hash(k, i, 472)), earthRock(6.5, 500 + k * 11 + i))
						.density(girth > 7 ? 1.25 : 1.6);
				// Knuckles of rock where its pieces meet, standing off it each its own way.
				if (i > 0 && SpellArt.hash(k, i, 473) < 0.6) {
					double knuckle = girth * (0.8 + 0.35 * SpellArt.hash(k, i, 474));
					rib.box(a[0], a[1], a[2], knuckle, knuckle * 0.85, knuckle * 0.9, earthRock(5, 560 + k * 11 + i)).rot(SpellArt.hash(k, i, 475) * 50 - 25,
							SpellArt.hash(k, i, 476) * 90, SpellArt.hash(k, i, 477) * 50 - 25).density(1.5);
				}
				// Amber crystals breaking out of it here and there.
				if (i > 0 && i < pieces - 1 && SpellArt.hash(k, i, 478) < 0.34) {
					// Up and out from the rib (away from the dome's heart), each a little its own way.
					double[] aim = SpellArt.aimY(a[0] * 0.012 + (SpellArt.hash(k, i, 479) - 0.5) * 0.6, 1, a[2] * 0.012 + (SpellArt.hash(k, i, 480) - 0.5) * 0.6);
					SpellArt.shared(m, rib, "crystal_" + (i % 2), false, a, 5 + 3 * (i % 2), 2.6, aim[0], aim[1], 0, Earth.amber(true, 590 + i % 2), 3, false);
				}
			}
		}
		SpellArt.Bone keystone = m.bone("keystone", "root", 0, DOME_H + 2, 0);
		double[][] crag = {{0, DOME_H + 2.5, 0, 14, 9, 13, 8, 20, -6}, {3.5, DOME_H + 4.8, -2, 10, 7, 9, -14, 65, 10}, {-4, DOME_H + 1, 3, 9, 6, 10, 18, -30, 4}};
		for (int c = 0; c < crag.length; c++) {
			double[] g = crag[c];
			keystone.box(g[0], g[1], g[2], g[3], g[4], g[5], c == 0 ? keystone(531) : earthRock(5, 532 + c)).rot(g[6], g[7], g[8]).density(c == 0 ? 2 : 1.5);
		}
		// The boulders torn from the land, drifting round inside it at its middle's height.
		SpellArt.Bone boulders = m.bone("boulders", "root", 0, DOME_H * 0.42, 0);
		for (int k = 0; k < BOULDERS; k++) {
			double th = Math.toRadians(360.0 * k / BOULDERS + 13 * (SpellArt.hash(k, 1, 491) - 0.5));
			double r = DOME_R * (0.62 + 0.16 * SpellArt.hash(k, 2, 491)), y = DOME_H * (0.3 + 0.28 * SpellArt.hash(k, 3, 491)), s = 3 + 4.5 * SpellArt.hash(k, 4, 491);
			double[] at = SpellArt.v3(Math.sin(th) * r, y, Math.cos(th) * r);
			SpellArt.Bone b = m.bone("boulder_" + k, "boulders", at[0], at[1], at[2]);
			b.box(at[0], at[1], at[2], s, s * 0.85, s * 0.95, earthRock(3.6, 600 + k)).rot(SpellArt.hash(k, 5, 491) * 60, SpellArt.hash(k, 6, 491) * 90, SpellArt.hash(k, 7, 491) * 60)
					.density(s > 5 ? 1.5 : 2);
		}
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0.3, 0);
		SpellArt.sheet(ground, 0, 0.3, 0, 2 * DOME_R + 10, 2 * DOME_R + 10, 0, 0, 0, crackedGround(401)).density(1.25);
		veinRise(m);
		veinHold(m);
		veinRelease(m);
	}

	/**
	 * Its rising: the land cracks open and the rubble heaves up out of it, the ribs burst up out of it one after another and bend in, the keystone
	 * crashing down onto them last; the boulders torn up after them.
	 */
	static void veinRise(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("rise", L, null);
		a.scale("ground", 0, 0.2);
		a.scale("ground", 0.3, 1);
		a.pos("rubble", 0, 0, -8, 0);
		a.pos("rubble", 0.22, 0, 1.2, 0);
		a.pos("rubble", 0.34, 0, 0, 0);
		a.scale("rubble", 0, 0.4);
		a.scale("rubble", 0.22, 1.06);
		a.scale("rubble", 0.34, 1);
		for (int k = 0; k < RIBS; k++) {
			double t0 = 0.06 + 0.04 * k;
			a.scale("rib_" + k, 0, 0.02);
			a.scale("rib_" + k, t0, 0.02);
			a.sample("rib_" + k, "scale", t0, t0 + 0.32, 4, t -> {
				double e = SpellArt.easeOut((t - t0) / 0.32) * 1.04;
				return new double[] {Math.max(0.02, e), Math.max(0.02, e), Math.max(0.02, e)};
			});
			a.scale("rib_" + k, Math.min(L, t0 + 0.42), 1);
			a.scale("rib_" + k, L, 1);
		}
		a.scale("keystone", 0, 0.02);
		a.scale("keystone", 0.58, 0.02);
		a.pos("keystone", 0, 0, 14, 0);
		a.pos("keystone", 0.58, 0, 14, 0);
		a.pos("keystone", 0.74, 0, -1, 0);
		a.pos("keystone", L, 0, 0, 0);
		a.scale("keystone", 0.64, 1.15);
		a.scale("keystone", L, 1);
		for (int k = 0; k < BOULDERS; k++) {
			double t0 = 0.3 + 0.025 * k;
			a.pos("boulder_" + k, 0, 0, -16, 0);
			a.pos("boulder_" + k, t0, 0, -16, 0);
			a.pos("boulder_" + k, Math.min(L, t0 + 0.28), 0, 0.8, 0);
			a.pos("boulder_" + k, L, 0, 0, 0);
			a.scale("boulder_" + k, 0, 0.02);
			a.scale("boulder_" + k, t0, 0.02);
			a.scale("boulder_" + k, Math.min(L, t0 + 0.2), 1);
			a.scale("boulder_" + k, L, 1);
		}
	}

	/**
	 * Its holding, looped without a seam: the ribs shuddering with the earth's strain, the boulders drifting round inside it, each bobbing and
	 * tumbling its own way, the keystone's trigram throbbing.
	 */
	static void veinHold(SpellArt.Model m) {
		double T = 8;
		SpellArt.Anim a = m.anim("hold", T, "true");
		for (int k = 0; k < RIBS; k++) {
			int kk = k;
			a.sample("rib_" + k, "rotation", 0, T, 48, t -> new double[] {0.5 * Math.sin(2 * Math.PI * t / T * 12 + kk * 1.3), 0, 0.4 * Math.sin(2 * Math.PI * t / T * 9 + kk * 2.1)});
		}
		a.sample("boulders", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
		for (int k = 0; k < BOULDERS; k++) {
			int kk = k, bobs = 2 + k % 2;
			double spin = (k % 2 == 0 ? 1 : -1) * 360;
			a.sample("boulder_" + k, "position", 0, T, 16, t -> new double[] {0, 1.4 * Math.sin(2 * Math.PI * t / T * bobs + kk * 1.7), 0});
			a.sample("boulder_" + k, "rotation", 0, T, 16, t -> new double[] {0, spin * t / T, 0});
		}
		a.sample("keystone", "scale", 0, T, 16, t -> {
			double p = 1 + 0.025 * Math.sin(2 * Math.PI * t / T * 4);
			return new double[] {p, p, p};
		});
	}

	/** Its release (held): the boulders fall, the keystone crumbles away, the ribs sink back into the land one after another, the rubble after them. */
	static void veinRelease(SpellArt.Model m) {
		double L = 0.8;
		SpellArt.Anim a = m.anim("release", L, "hold_on_last_frame");
		for (int k = 0; k < BOULDERS; k++) {
			a.pos("boulder_" + k, 0, 0, 0, 0);
			a.pos("boulder_" + k, 0.4, 0, -14, 0);
			a.pos("boulder_" + k, L, 0, -18, 0);
			a.scale("boulder_" + k, 0, 1);
			a.scale("boulder_" + k, 0.3, 1);
			a.scale("boulder_" + k, 0.45, 0.02);
			a.scale("boulder_" + k, L, 0.02);
		}
		a.scale("keystone", 0, 1);
		a.scale("keystone", 0.3, 0.02);
		a.scale("keystone", L, 0.02);
		for (int k = 0; k < RIBS; k++) {
			double t0 = 0.12 + 0.03 * k;
			a.pos("rib_" + k, 0, 0, 0, 0);
			a.pos("rib_" + k, t0, 0, 0, 0);
			a.pos("rib_" + k, Math.min(L, t0 + 0.4), 0, -12, 0);
			a.pos("rib_" + k, L, 0, -12, 0);
			a.scale("rib_" + k, 0, 1);
			a.scale("rib_" + k, t0, 1);
			a.scale("rib_" + k, Math.min(L, t0 + 0.4), 0.02);
			a.scale("rib_" + k, L, 0.02);
		}
		a.pos("rubble", 0, 0, 0, 0);
		a.pos("rubble", 0.5, 0, 0, 0);
		a.pos("rubble", L, 0, -8, 0);
		a.scale("ground", 0, 1);
		a.scale("ground", 0.6, 1);
		a.scale("ground", L, 0.02);
	}

	// --- Weak Water from the Void: its fall ---

	/** The Weak Water's colours: pale, ghostly blue, its light near white. */
	static final int WEAK_DEEP = 0x2E5A80, WEAK = 0x6AA6D6, WEAK_PALE = 0xB4DAFA, WEAK_WHITE = 0xEEF8FF;

	/**
	 * A sheet of the Weak Water falling (on a blade, its length up it): pale and thin, seen through, streaks running down it on a slant (so,
	 * the fall turning, they seem to pour down), its edges glowing.
	 */
	static SpellArt.Painter weakWater(int seed) {
		return t -> {
			double v = t.v(), u = t.u();
			// Long streaks down it, a little aslant, wavering; brighter threads among them.
			double s = u * 7 + v * 2.2 + 0.25 * Math.sin(v * 9 + seed);
			double streak = Math.pow(1 - Math.abs(2 * SpellArt.noise(s, v * 0.9 + seed, seed) - 1), 3);
			double thread = Math.pow(1 - Math.abs(2 * SpellArt.noise(s * 1.7 + 3, v * 0.5, seed + 1) - 1), 14);
			double fadeEdge = SpellArt.smooth(0, 0.18, Math.min(u, 1 - u));
			if (thread > 0.6) return SpellArt.glow(SpellArt.mix(WEAK_PALE, WEAK_WHITE, thread), 0.55 * fadeEdge + 0.15);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.25 + streak * 0.65), WEAK_DEEP, WEAK, WEAK_PALE), (0.18 + 0.32 * streak) * (0.4 + 0.6 * fadeEdge));
		};
	}

	/** Its spray where it strikes the land (on a blade, rising from its foot): fine drops thrown up and out, thinning to nothing. */
	static SpellArt.Painter weakSpray(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.45 * (1 - v * 0.6);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double drops = SpellArt.hash((int) Math.floor(t.fx * 14), (int) Math.floor(t.ly * 1.4), seed);
			if (drops > 0.5 + 0.45 * v) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(WEAK_WHITE, WEAK_PALE, v), (1 - v) * 0.7);
		};
	}

	/** The pool it spreads in (on a sheet): rings of pale ripples and foam, thinning out to its edge. */
	static SpellArt.Painter weakPool(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.98) return SpellArt.CLEAR;
			double ripple = Math.sin(r * 38 + SpellArt.noise(x * 3, z * 3, seed) * 3);
			double foam = SpellArt.fbm(t.across() * 9 + seed, t.back() * 9, seed, 3);
			double a = (1 - r) * (0.35 + 0.35 * Math.max(0, ripple)) + (r < 0.3 ? 0.25 * (1 - r / 0.3) : 0);
			if (foam > 0.66) return SpellArt.glow(WEAK_WHITE, Math.min(0.85, a + 0.3));
			if (a < 0.04) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(WEAK, WEAK_PALE, 0.5 + 0.5 * ripple), a);
		};
	}

	/** A feather sinking in it (nothing floats in the Weak Water): white, its shaft a thread of light. */
	static SpellArt.Painter featherWhite(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.45 * Math.sin(Math.PI * Math.min(1, v * 1.05));
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			if (Math.abs(u) < 0.06) return SpellArt.glow(WEAK_WHITE, 0.9);
			double barb = (v * 9 - Math.abs(u) * 5 + seed) % 1;
			return SpellArt.col(barb < 0.3 ? 0xDCE6F0 : 0xF8FBFF, 0.9);
		};
	}

	static final double FALL_TOP = 88, FALL_R = 11;
	static final int FALL_SHEETS = 12, SPLASHES = 14, FEATHERS = 6;

	/**
	 * Weak Water from the Void's fall, from the rift forty blocks up down to the land at the origin: a column of pale water pouring ("column",
	 * its pivot at the top; twelve falling sheets in a ring and a brighter core of six, "core"), its spray where it strikes ("splash"), the pool it
	 * spreads in ("pool") and the mist over it ("mist"), and white feathers sinking in it ("feather_0".."feather_5").
	 */
	static void weakWaterFall(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone column = m.bone("column", "root", 0, FALL_TOP, 0);
		SpellArt.Bone sheets = m.bone("sheets", "column", 0, FALL_TOP, 0);
		for (int k = 0; k < FALL_SHEETS; k++) {
			double th = 360.0 * k / FALL_SHEETS;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * FALL_R, 0, Math.cos(Math.toRadians(th)) * FALL_R);
			SpellArt.shared(m, sheets, "fall_sheet", false, base, FALL_TOP, 7, 0, th + 90, 0, weakWater(501), 1, false);
		}
		SpellArt.Bone core = m.bone("core", "column", 0, FALL_TOP, 0);
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k + 15;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 4.5, 0, Math.cos(Math.toRadians(th)) * 4.5);
			SpellArt.shared(m, core, "fall_core", false, base, FALL_TOP, 6, 0, th + 90, 0, weakWater(503), 1, false);
		}
		SpellArt.Bone splash = m.bone("splash", "root", 0, 0, 0);
		for (int k = 0; k < SPLASHES; k++) {
			double th = 360.0 * k / SPLASHES;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * (FALL_R + 1), 0.4, Math.cos(Math.toRadians(th)) * (FALL_R + 1));
			// Leaning out from the fall: a blade's sweep tips it toward its -z, turned (th + 180) so that is outward. Every second one alike in all
			// (its shape, height and lean): the spray's wheeling, two of them a loop, comes round seamlessly.
			SpellArt.shared(m, splash, "splash_" + k % 2, false, base, 14 + (k % 2) * 4, 8, 42 + (k % 2) * 12, th + 180, 0, weakSpray(510 + k % 2), 2, false);
		}
		SpellArt.Bone pool = m.bone("pool", "root", 0, 0.5, 0);
		SpellArt.sheet(pool, 0, 0.5, 0, 88, 88, 0, 0, 0, weakPool(521)).density(1);
		for (int k = 0; k < FEATHERS; k++) {
			double th = 60.0 * k + 30, r = 3 + (k % 3) * 2.5;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * r, 4, Math.cos(Math.toRadians(th)) * r);
			SpellArt.Bone f = m.bone("feather_" + k, "root", at[0], at[1], at[2]);
			SpellArt.blade(f, at, 5, 2.2, 20 + k * 12, th, 30, featherWhite(530 + k)).density(4);
		}
		// The mist over its pool, drawn last.
		SpellArt.Bone mist = m.bone("mist", "root", 0, 3, 0);
		SpellArt.sheet(mist, 0, 3, 0, 80, 80, 0, 0, 0, Ghost.mistBank(541)).density(1);
		SpellArt.sheet(mist, 0, 7, 0, 64, 64, 0, 30, 0, Ghost.mistBank(542)).density(1);
		pour(m);
		weakFall(m);
		dry(m);
	}

	/** Its pouring out: the column runs down out of the rift to the land, and the pool, the spray and the mist spread where it strikes. */
	static void pour(SpellArt.Model m) {
		double L = 0.7;
		SpellArt.Anim a = m.anim("pour", L, null);
		a.sample("column", "scale", 0, 0.5, 5, t -> new double[] {1, Math.max(0.02, SpellArt.easeIn(t / 0.5)), 1});
		a.scale("column", L, 1);
		for (String n : new String[] {"splash", "pool", "mist"}) {
			a.scale(n, 0, 0.02);
			a.scale(n, 0.45, 0.02);
			a.scale(n, 0.62, 1.1);
			a.scale(n, L, 1);
		}
		for (int k = 0; k < FEATHERS; k++) {
			a.scale("feather_" + k, 0, 0.02);
			a.scale("feather_" + k, L, 0.02);
		}
	}

	/**
	 * Its falling, looped without a seam: the sheets turning (a slant of streaks on them, they seem to pour down), the core the other way, the
	 * spray wheeling, the pool breathing, the mist drifting; and the feathers sinking down the column one after another (each gone at its foot,
	 * and back at its top unseen).
	 */
	static void weakFall(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("fall", T, "true");
		a.sample("sheets", "rotation", 0, T, 8, t -> new double[] {0, 60 * t / T, 0});
		a.sample("core", "rotation", 0, T, 8, t -> new double[] {0, -120 * t / T, 0});
		a.sample("splash", "rotation", 0, T, 8, t -> new double[] {0, 360.0 / SPLASHES * 2 * t / T, 0});
		a.sample("splash", "scale", 0, T, 16, t -> new double[] {1, 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 4), 1});
		a.sample("pool", "scale", 0, T, 8, t -> {
			double p = 1 + 0.04 * Math.sin(2 * Math.PI * t / T);
			return new double[] {p, 1, p};
		});
		a.sample("mist", "rotation", 0, T, 8, t -> new double[] {0, 6 * Math.sin(2 * Math.PI * t / T), 0});
		for (int k = 0; k < FEATHERS; k++) {
			double offset = k / (double) FEATHERS;
			String n = "feather_" + k;
			for (double t = 0; t <= T + 1e-9; t += 0.1) {
				double phase = (t / T + offset) % 1;
				a.pos(n, t, 0, (FALL_TOP - 10) * (1 - phase), 0);
				a.scale(n, t, Math.max(0.02, Math.min(1, Math.min(phase, 1 - phase) * 10)));
				a.rot(n, t, 0, 360 * phase * 2, 25 * Math.sin(phase * 12));
			}
			double wrap = (1 - offset) * T;
			if (wrap > 0 && wrap < T) {
				a.pos(n, wrap - 0.001, 0, 0, 0);
				a.scale(n, wrap - 0.001, 0.02);
				a.rot(n, wrap - 0.001, 0, 720, 0);
				a.pos(n, wrap, 0, FALL_TOP - 10, 0);
				a.scale(n, wrap, 0.02);
				a.rot(n, wrap, 0, 0, 0);
			}
		}
	}

	/** Its running dry (held): the column thins to a thread and is gone, the spray and pool sink, the mist thins. */
	static void dry(SpellArt.Model m) {
		double L = 0.8;
		SpellArt.Anim a = m.anim("dry", L, "hold_on_last_frame");
		a.scale("column", 0, 1);
		a.scale("column", 0.6, 0.06, 1, 0.06);
		a.scale("column", L, 0.02, 1, 0.02);
		a.scale("splash", 0, 1);
		a.scale("splash", L, 0.1);
		a.scale("pool", 0, 1);
		a.scale("pool", L, 0.4);
		a.scale("mist", 0, 1);
		a.scale("mist", L, 0.5);
	}

	// --- Kun Rises from the Northern Abyss: the Kun ---

	/** The abyss's black water and the Kun's markings: wave patterns of the old paintings, glowing pale. */
	static final int KUN_BACK = 0x05080F, KUN_DEEP = 0x0B1830, KUN_FLANK = 0x15305A, KUN_BELLY = 0x4E7298, KUN_GLOW = 0x7CE6FF, KUN_WHITE = 0xE2FBFF;

	/**
	 * The Kun's hide of black water (round a body along z, its axis at x = 0, y = 0): near black along its back, deep blue on its flanks, pale
	 * beneath; on its flanks rows of the old paintings' sea waves (nested arcs) in glowing lines, a seam of light along its side; its currents
	 * moving in it (Water#blackWater); seen through a little.
	 */
	static SpellArt.Painter kunHide(int seed) {
		return t -> {
			double qy = t.qy(), qx = t.qx(), qz = t.qz();
			double side = Math.atan2(qy, Math.abs(qx) + 0.01) / (Math.PI / 2); // 1 along its back, -1 beneath
			if (t.flat()) side = t.ny > 0 ? 1 : -1;
			// Its currents: the abyss's water moving in it, lit where it runs fast.
			double n = SpellArt.fbm3(t.px * 0.07, t.py * 0.09, t.pz * 0.03, seed, 3);
			double current = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.12 + n, t.py * 0.12, t.pz * 0.05, seed + 7, 2) - 1);
			// The lateral line: a seam of light down each flank, wavering.
			double line = Math.abs(side - 0.08 - 0.06 * Math.sin(qz * 0.12));
			if (line < 0.045 && !t.flat()) return SpellArt.glow(SpellArt.mix(KUN_GLOW, KUN_WHITE, 0.4), 0.95);
			// The old paintings' cloud-water scrolls along its flanks: great hooked spirals of light, one after another.
			if (side > -0.45 && side < 0.6 && !t.flat()) {
				double cx = qz / 13 + (side > 0.1 ? 0.5 : 0), cy = (side - 0.08) * 2.2;
				double ux = cx - Math.floor(cx) - 0.5, r = Math.hypot(ux * 1.3, cy * 0.9);
				if (r < 0.48) {
					double th = Math.atan2(cy, ux) / (2 * Math.PI) + 0.5, s = r / 0.12 - th;
					if (s - Math.floor(s) < 0.2 && r > 0.06) return SpellArt.glow(SpellArt.mix(KUN_GLOW, KUN_WHITE, 0.25 + 0.5 * (0.48 - r)), 0.88);
				}
			}
			if (current > 0.9) return SpellArt.glow(SpellArt.mix(KUN_FLANK, KUN_GLOW, (current - 0.9) * 10), 0.7);
			int c = side < -0.4 ? SpellArt.mix(KUN_FLANK, KUN_BELLY, SpellArt.clamp01((-side - 0.4) * 1.8)) : SpellArt.mix(KUN_BACK, KUN_FLANK, SpellArt.clamp01(0.75 - side));
			return SpellArt.col(SpellArt.shade(c, (n - 0.5) * 0.35), 0.9);
		};
	}

	/** One of the Kun's fins (on a blade): the abyss's water, seen through, rays of light running out along it to its ragged edge, which glows. */
	static SpellArt.Painter kunFin(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double edge = 0.48 * (0.5 + 0.5 * Math.sin(Math.PI * Math.min(1, v * 0.9 + 0.12))) - 0.05 * SpellArt.noise(v * 10, seed, seed);
			if (Math.abs(u) > edge || v > 0.985) return SpellArt.CLEAR;
			double rays = Math.abs(Math.sin((u / Math.max(0.05, edge)) * 5 + v * 1.5));
			if (edge - Math.abs(u) < 0.05 || v > 0.94) return SpellArt.glow(SpellArt.mix(KUN_GLOW, KUN_WHITE, 0.45), 0.9);
			if (rays < 0.14) return SpellArt.glow(SpellArt.mix(KUN_FLANK, KUN_GLOW, 0.8), 0.75);
			double film = SpellArt.noise(u * 6 + seed, v * 8, seed + 2);
			return SpellArt.glow(SpellArt.mix(KUN_FLANK, KUN_GLOW, 0.22 + 0.25 * v + 0.15 * film), 0.42 + 0.12 * film);
		};
	}

	/** The Kun's body along z: the points its width and height are given at (half each), and those half-widths and half-heights. */
	static final double[] KUN_Z = {-66, -58, -46, -30, -10, 10, 28, 42, 54}, KUN_W = {6, 11, 14, 15.5, 14.5, 11.5, 7.5, 4.6, 3.4},
			KUN_H = {5.5, 10, 12.5, 13.5, 12.5, 10, 7, 4.6, 3.4};

	static double kunProfile(double[] at, double z) {
		if (z <= KUN_Z[0]) return at[0];
		for (int i = 0; i < KUN_Z.length - 1; i++) {
			if (z <= KUN_Z[i + 1]) return SpellArt.lerp(at[i], at[i + 1], SpellArt.fade((z - KUN_Z[i]) / (KUN_Z[i + 1] - KUN_Z[i])));
		}
		return at[at.length - 1];
	}

	/** The Kun's eye on the outer side of its cube: a great round of pale cyan light, dark at its heart. */
	static SpellArt.Painter kunEye(SpellArt.Painter around) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return around.paint(t);
			double x = (t.back() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.95) return around.paint(t);
			if (r < 0.32) return SpellArt.col(KUN_BACK);
			return SpellArt.glow(SpellArt.mix(KUN_WHITE, KUN_GLOW, r), 0.97);
		};
	}

	/**
	 * Kun (鲲), the fish of the northern abyss that becomes the Peng, made of the abyss's black water, some sixty blocks long, facing -z, its middle
	 * at the origin: its body in eight-sided slices swelling from its blunt head and tapering to its tail, in pieces that bend ("front", "back_0"..
	 * "back_2"), cloud-water scrolls of light along its flanks and a seam of light down each; its head ("head", its jaw "jaw") with great eyes of
	 * pale light and its mouth's line glowing, two long barbels trailing from it ("barbel_l_0".."_3", "barbel_r_..."); a sail of a fin along its
	 * back; its fins great as wings, the Peng's in it ("fin_l", "fin_r"); and its tail forked, up and back and down and back ("tail").
	 */
	static void kun(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter hide = kunHide(601);
		m.bone("front", "root", 0, 0, 0);
		SpellArt.Bone head = m.bone("head", "front", 0, 0, -42);
		m.bone("back_0", "root", 0, 0, 0);
		m.bone("back_1", "back_0", 0, 0, 20);
		m.bone("back_2", "back_1", 0, 0, 36);
		// Its body, slice by slice: each eight-sided, a little past the next so they run together.
		double step = 6.5;
		for (double z = -63.5; z <= 51; z += step) {
			double w = kunProfile(KUN_W, z), h = kunProfile(KUN_H, z), len = step + 0.8;
			SpellArt.Bone b = m.get(z < -42 ? "head" : z < 0 ? "front" : z < 20 ? "back_0" : z < 36 ? "back_1" : "back_2");
			b.box(0, 0, z, 2 * w, 2 * h * 0.66, len, hide).density(1);
			b.box(0, 0, z, 2 * w * 0.66, 2 * h, len, hide).density(1);
			b.box(0, 0, z, 2 * w * 0.88, 2 * h * 0.88, len, hide).density(1);
		}
		// Its eyes, great and pale, and the long line of its mouth glowing.
		double ez = -50, ew = kunProfile(KUN_W, ez);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * (ew - 0.1), 3.2, ez, 0.8, 5.4, 5.4, kunEye(hide)).density(3);
			head.box(s * (kunProfile(KUN_W, -58) - 0.3), -1.6, -58, 0.7, 0.7, 14, t -> SpellArt.glow(SpellArt.mix(KUN_GLOW, KUN_WHITE, 0.3), 0.95)).rot(0, -s * 10, 0).density(3);
		}
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, -4, -48);
		jaw.box(0, -6.6, -57, 2 * kunProfile(KUN_W, -58) * 0.86, 4, 16, hide).density(1.25);
		// Its barbels, long, trailing back from the corners of its mouth.
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r";
			double[] at = SpellArt.v3(s * (kunProfile(KUN_W, -62) - 0.5), -2.2, -62);
			String up = "head";
			for (int k = 0; k < 4; k++) {
				SpellArt.Bone b = m.bone("barbel_" + side + "_" + k, up, at[0], at[1], at[2]);
				double[] next = SpellArt.add(at, SpellArt.v3(s * (3.5 - k * 0.6), -2.5 - k * 0.8, 8));
				double thick = 1.6 - k * 0.3;
				SpellArt.limb(b, at, next, thick, thick, t -> SpellArt.glow(SpellArt.mix(KUN_FLANK, KUN_GLOW, 0.5 + 0.5 * t.up()), 0.88)).density(3);
				up = b.name;
				at = next;
			}
		}
		// The sail along its back, swept back.
		for (int k = 0; k < 8; k++) {
			double z = -36 + k * 8.5;
			SpellArt.Bone bone = m.get(z < 0 ? "front" : z < 20 ? "back_0" : "back_1");
			SpellArt.blade(bone, SpellArt.v3(0, kunProfile(KUN_H, z) - 1.2, z), 17 - k * 1.2, 10, -28, 0, 0, kunFin(610 + k % 4)).density(1.25);
		}
		// Its fins, great as wings (the Peng's): four long vanes each side, swept back and drooping a little.
		double fz = -30, fw = kunProfile(KUN_W, fz);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone fin = m.bone("fin_" + (s > 0 ? "l" : "r"), "front", s * (fw - 2), -5, fz);
			double[] lengths = {44, 38, 31, 23}, widths = {13, 11, 9, 7};
			for (int k = 0; k < 4; k++) {
				SpellArt.flatBlade(fin, SpellArt.v3(s * (fw - 2), -5 - k * 0.5, fz + k * 3), lengths[k], widths[k], -4 - k * 3, s * (52 + k * 13), s * (6 + k * 4),
						kunFin(620 + k + (s > 0 ? 0 : 5))).density(1);
			}
		}
		// Two small fins beneath it.
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.flatBlade(m.get("back_0"), SpellArt.v3(s * 5, -kunProfile(KUN_H, 8) + 1, 8), 15, 6, -24, s * 40, s * 30, kunFin(640 + (s > 0 ? 0 : 1))).density(1.5);
		}
		// Its tail: forked, its lobes swept up and back and down and back.
		SpellArt.Bone tail = m.bone("tail", "back_2", 0, 0, 50);
		SpellArt.blade(tail, SpellArt.v3(0, 1, 50), 33, 15, -48, 0, 0, kunFin(631)).density(1);
		SpellArt.blade(tail, SpellArt.v3(0, -1, 50), 29, 14, -132, 0, 0, kunFin(632)).density(1);
		SpellArt.blade(tail, SpellArt.v3(0, 0.5, 49), 20, 10, -66, 0, 0, kunFin(633)).density(1.25);
		SpellArt.blade(tail, SpellArt.v3(0, -0.5, 49), 18, 9, -114, 0, 0, kunFin(634)).density(1.25);
		kunSwim(m);
	}

	/** Its swimming, looped without a seam: a wave running down its body to its tail, its fins beating like wings, its barbels trailing, its jaw working. */
	static void kunSwim(SpellArt.Model m) {
		double T = 2.0;
		SpellArt.Anim a = m.anim("swim", T, "true");
		double w = 2 * Math.PI / T;
		// A great fish's thrash (the user's ask, 2026-10-25: the beasts too rigid): its tail sweeping wide, its body flexing up and down a little too.
		a.sample("front", "rotation", 0, T, 24, t -> new double[] {2 * Math.sin(w * t + 1.4), -6 * Math.sin(w * t + 0.6), 0});
		a.sample("head", "rotation", 0, T, 24, t -> new double[] {1.5 * Math.sin(w * t + 2), -4 * Math.sin(w * t + 1.2), 0});
		String[] back = {"back_0", "back_1", "back_2", "tail"};
		double[] amp = {9, 15, 22, 30};
		for (int i = 0; i < back.length; i++) {
			int ii = i;
			a.sample(back[i], "rotation", 0, T, 24, t -> new double[] {amp[ii] * 0.25 * Math.sin(w * t - ii * 0.8 + 1.2), amp[ii] * Math.sin(w * t - ii * 0.85), 0});
		}
		a.sample("jaw", "rotation", 0, T, 8, t -> new double[] {6 + 4 * Math.sin(w * t), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("fin_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 24, t -> new double[] {6 * Math.sin(w * t + 1.4), 0, ss * 30 * Math.sin(w * t + 0.4)});
			for (int k = 0; k < 4; k++) {
				int kk = k;
				a.sample("barbel_" + (s > 0 ? "l" : "r") + "_" + k, "rotation", 0, T, 12, t -> new double[] {8 * Math.sin(w * t - kk * 0.9), ss * 10 * Math.sin(w * t - kk * 0.7 + 1), 0});
			}
		}
	}

	// --- Void-Severing Sword Light: the sword of light ---

	/** Sword light: cold white, blue-white, the violet of the void at its edges. */
	static final int LIGHT_WHITE = 0xFAFCFF, LIGHT_PALE = 0xD6E8FF, LIGHT_BLUE = 0x9CC4FF, LIGHT_VIOLET = 0xA680FF;

	/**
	 * A blade of sword light (on a flat blade, its point at its base, v 0, its hilt at its far end): drawn to its point; a white-hot thread down
	 * its middle, blue-white either side, its edges burning the void's violet, glyphs down its fuller; fading a little toward its hilt.
	 */
	static SpellArt.Painter swordLight(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double half = 0.5 * Math.pow(Math.min(1, v / 0.16), 0.75);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double edge = (half - Math.abs(u)) / Math.max(0.02, half);
			if (edge < 0.18) return SpellArt.glow(SpellArt.mix(LIGHT_VIOLET, LIGHT_PALE, edge / 0.18), 0.92);
			if (Math.abs(u) < 0.05) return SpellArt.glow(LIGHT_WHITE, 0.98);
			if (v > 0.3 && v < 0.8 && Math.abs(u) < 0.2) {
				double cell = (v - 0.3) / 0.5 * 7;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci, seed, (u + 0.2) / 0.4, cell - ci)) return SpellArt.glow(LIGHT_VIOLET, 0.95);
			}
			return SpellArt.glow(SpellArt.mix(LIGHT_PALE, LIGHT_BLUE, Math.abs(u) * 2), 0.82 - 0.2 * v);
		};
	}

	/** A streamer of light trailing a stroke (on a flat blade, v 0 nearest the sword): white, thinning and fading behind it. */
	static SpellArt.Painter lightTrail(int seed) {
		return t -> {
			double v = t.v(), u = Math.abs(t.u() - 0.5) * 2;
			double half = (1 - v) * (0.8 + 0.2 * Math.sin(v * 9 + seed));
			if (u > half) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(LIGHT_WHITE, LIGHT_VIOLET, v), (0.75 - 0.7 * v) * (1 - u / Math.max(0.05, half)) + 0.05);
		};
	}

	/** The ring of torn air at the stroke's point (on a sheet): a thin ring of white light, a violet glow round it; clear elsewhere. */
	static SpellArt.Painter shockRing() {
		return t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			double d = Math.abs(r - 0.84);
			if (d < 0.03) return SpellArt.glow(LIGHT_WHITE, 0.95);
			if (d < 0.12) return SpellArt.glow(SpellArt.mix(LIGHT_PALE, LIGHT_VIOLET, d / 0.12), 0.55 * (1 - d / 0.12));
			return SpellArt.CLEAR;
		};
	}

	static final double SEVER_BLADE = 62;

	/**
	 * Void-Severing Sword Light's sword of light, its point at the origin and its blade back along +z, some thirty-five blocks of it: its blade
	 * ("blade", two crossed planes of light), wings of light for a guard, a thread of a hilt; streamers of light trailing it ("trail"), and rings
	 * of torn air running back from its point ("ring_0".."ring_2").
	 */
	static void severingLight(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone blade = m.bone("blade", "root", 0, 0, 0);
		SpellArt.flatBlade(blade, SpellArt.v3(0, 0, 0), SEVER_BLADE, 7, 0, 0, 0, swordLight(701)).density(2);
		SpellArt.flatBlade(blade, SpellArt.v3(0, 0, 0), SEVER_BLADE, 7, 0, 0, 90, swordLight(702)).density(2);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.flatBlade(blade, SpellArt.v3(s * 1.5, 0, SEVER_BLADE), 10, 3, 0, s * 62, 0, lightTrail(703)).density(3);
		}
		SpellArt.limb(blade, SpellArt.v3(0, 0, SEVER_BLADE), SpellArt.v3(0, 0, SEVER_BLADE + 12), 1.6, 1.6, t -> SpellArt.glow(LIGHT_PALE, 0.9)).density(3);
		SpellArt.Bone trail = m.bone("trail", "root", 0, 0, SEVER_BLADE + 8);
		double[][] streams = {{0, 0, 44, 6, 0, 0}, {2, 1, 36, 4, 6, 4}, {-2, -1, 36, 4, -6, -4}};
		for (int k = 0; k < streams.length; k++) {
			double[] s = streams[k];
			SpellArt.flatBlade(trail, SpellArt.v3(s[0], s[1], SEVER_BLADE + 8), s[2], s[3], s[5], s[4], k == 0 ? 90 : 0, lightTrail(710 + k)).density(1.5);
		}
		for (int k = 0; k < 3; k++) {
			SpellArt.Bone ring = m.bone("ring_" + k, "root", 0, 0, 2);
			SpellArt.sheet(ring, 0, 0, 2, 18, 18, 90, 0, 0, shockRing()).density(2.5);
		}
		thrust(m);
	}

	/** Its thrust, looped without a seam: rings of torn air running back from its point one after another (each gone as it comes round), the blade shimmering, the streamers rippling. */
	static void thrust(SpellArt.Model m) {
		double T = 0.6;
		SpellArt.Anim a = m.anim("thrust", T, "true");
		for (int k = 0; k < 3; k++) {
			double offset = k / 3.0;
			String n = "ring_" + k;
			for (double t = 0; t <= T + 1e-9; t += 0.05) {
				double phase = (t / T + offset) % 1;
				a.pos(n, t, 0, 0, 22 * phase);
				a.scale(n, t, Math.max(0.02, (0.5 + 1.5 * phase) * Math.min(1, Math.min(phase, 1 - phase) * 8)));
			}
			double wrap = (1 - offset) * T;
			if (wrap > 0 && wrap < T) {
				a.pos(n, wrap - 0.001, 0, 0, 22);
				a.scale(n, wrap - 0.001, 0.02);
				a.pos(n, wrap, 0, 0, 0);
				a.scale(n, wrap, 0.02);
			}
		}
		a.sample("blade", "scale", 0, T, 12, t -> new double[] {1 + 0.06 * Math.sin(2 * Math.PI * t / T * 3), 1 + 0.06 * Math.sin(2 * Math.PI * t / T * 3 + 1.5), 1});
		a.sample("trail", "rotation", 0, T, 12, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T), 4 * Math.sin(2 * Math.PI * t / T + 1), 0});
	}

	// --- Ten-Direction Sword Prison ---

	/**
	 * A great sword's blade standing point down (its point at the foot of its cube; its broad faces front and back): steel, its edges blazing with
	 * sword light, a ridge of silver down its middle, glyphs of gold down its upper half; drawn to its point.
	 */
	static SpellArt.Painter prisonBlade(int seed) {
		return t -> {
			double up = t.up(), u = t.across() - 0.5, half = 0.5 * Math.pow(Math.min(1, up / 0.16), 0.75);
			if (t.side()) return up > 0.16 ? SpellArt.glow(SpellArt.mix(Metal.GLEAM, Metal.WHITE, 0.6), 0.96) : SpellArt.CLEAR;
			if (t.flat()) return SpellArt.col(Metal.SILVER);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double edge = (half - Math.abs(u)) * 4.4;
			if (edge < 0.55) return SpellArt.glow(SpellArt.mix(Metal.GLEAM, Metal.WHITE, 1 - edge / 0.55), 0.97);
			if (up > 0.5 && up < 0.92 && Math.abs(u) < 0.2) {
				double cell = (up - 0.5) / 0.42 * 5;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci, seed, (u + 0.2) / 0.4, cell - ci)) return SpellArt.glow(SpellArt.mix(Metal.GOLD, 0xFFF0C0, 0.35), 0.97);
			}
			if (Math.abs(u) < 0.05) return SpellArt.col(SpellArt.mix(Metal.SILVER, Metal.GLEAM, 0.4));
			return SpellArt.col(SpellArt.mix(Metal.STEEL, Metal.SILVER, 0.35 + 0.45 * SpellArt.noise(up * 9 + seed, u * 4, seed)));
		};
	}

	/**
	 * A wall of sword light between two of the prison's swords (on a sheet standing up): bars of white light like a cage's, a faint sheen between
	 * them, a band of glyphs across its middle, its foot and head brighter.
	 */
	static SpellArt.Painter swordWall(int seed) {
		return t -> {
			double x = t.across(), y = t.back();
			if (y < 0.04 || y > 0.96) return SpellArt.glow(Metal.GLEAM, 0.7);
			double bar = Math.abs(x * 6 - Math.rint(x * 6));
			if (bar < 0.05) return SpellArt.glow(SpellArt.mix(Metal.GLEAM, Metal.WHITE, 0.5), 0.85);
			if (Math.abs(y - 0.5) < 0.1) {
				double cell = x * 8;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci + seed, seed, cell - ci, (y - 0.4) / 0.2)) return SpellArt.glow(Metal.GLEAM, 0.85);
			}
			return SpellArt.glow(SpellArt.mix(Metal.ICE_EYE, Metal.GLEAM, y), 0.1 + 0.06 * Math.sin(y * 20 + x * 3));
		};
	}

	/** The prison's floor (on a sheet): a ring of glyphs round it, and the eight lines that join its swords, of sword light; clear elsewhere. */
	static SpellArt.Painter prisonFloor(int seed) {
		SpellArt.Painter ring = script(0.86, 0.98, 32, seed, Metal.GLEAM, Metal.WHITE);
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.84) return ring.paint(t);
			double ang = Math.atan2(x, z), sector = Math.PI / 4;
			// The octagon through the swords (its corners at 0.8 of the sheet's half), and a smaller one within it.
			double fromEdge = ang - sector * (Math.floor(ang / sector) + 0.5);
			for (double size : new double[] {0.8, 0.5}) {
				double oct = size * Math.cos(sector / 2) / Math.cos(fromEdge);
				if (Math.abs(r - oct) < 0.012) return SpellArt.glow(size > 0.6 ? Metal.GLEAM : Metal.ICE_EYE, 0.9);
			}
			// The lines from its heart out to each sword.
			double fromCorner = ang - sector * Math.rint(ang / sector);
			if (Math.abs(fromCorner) * r < 0.01 && r > 0.08 && r < 0.8) return SpellArt.glow(Metal.ICE_EYE, 0.75);
			if (r < 0.06) return SpellArt.glow(Metal.WHITE, 0.9);
			return SpellArt.CLEAR;
		};
	}

	/**
	 * A great sword standing point down in {@code bone}, its point at {@code tip}, its blade {@code length} long: the blade and its ridge, a guard
	 * of gold with its ends swept up, its grip and its pommel.
	 */
	static void greatSword(SpellArt.Bone bone, double[] tip, double length, int seed) {
		double x = tip[0], y = tip[1], z = tip[2], wide = length * 0.13;
		bone.box(x, y + length / 2, z, wide, length, 0.9, prisonBlade(seed)).only(SpellArt.Face.FRONT, SpellArt.Face.BACK, SpellArt.Face.SIDE_A, SpellArt.Face.SIDE_B).density(2);
		bone.box(x, y + length * 0.58, z, wide * 0.18, length * 0.84, 1.3, t -> SpellArt.col(SpellArt.mix(Metal.SILVER, Metal.GLEAM, 0.3 + 0.3 * t.up())))
				.skip(SpellArt.Face.SIDE_A, SpellArt.Face.SIDE_B).density(2);
		SpellArt.Painter gilt = Metal.gold(seed + 1);
		bone.box(x, y + length + 1.1, z, wide * 2.4, 2.2, 2.6, gilt).density(2);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(bone, SpellArt.v3(x + s * wide * 1.1, y + length + 1.1, z), SpellArt.v3(x + s * wide * 1.45, y + length + 3.6, z), 1.1, 1.6, gilt).density(3);
		}
		bone.box(x, y + length + 2.2 + length * 0.12, z, 1.7, length * 0.24, 1.7, Metal.grip()).density(3);
		bone.box(x, y + length + 2.2 + length * 0.24 + 1.2, z, 2.8, 2.8, 2.8, gilt).rot(0, 45, 0).density(3);
	}

	static final int PRISON_SWORDS = 8;
	static final double PRISON_R = 28, PRISON_FALL = 96, PRISON_TOP = 108, PRISON_SINK = 8;

	/**
	 * Ten-Direction Sword Prison (十方: the eight quarters, above and below), its heart at the origin on the land: eight great swords standing
	 * point down in a ring thirteen blocks round ("sword_0".."sword_7", driven in where they fell), walls of sword light between them
	 * ("walls"), the ring of glyphs and lines of light on the land joining them ("floor"); the sword from above hanging in its rift ("top"),
	 * and the sword from below waiting under the land ("bottom"), that meet in its heart.
	 */
	static void swordPrison(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone floor = m.bone("floor", "root", 0, 0.3, 0);
		SpellArt.sheet(floor, 0, 0.3, 0, 2 * PRISON_R / 0.8, 2 * PRISON_R / 0.8, 0, 0, 0, prisonFloor(801)).density(1.25);
		for (int k = 0; k < PRISON_SWORDS; k++) {
			double th = Math.toRadians(360.0 * k / PRISON_SWORDS);
			double[] tip = SpellArt.v3(Math.sin(th) * PRISON_R, -PRISON_SINK, Math.cos(th) * PRISON_R);
			SpellArt.Bone sword = m.bone("sword_" + k, "root", tip[0], tip[1], tip[2], 0, 360.0 * k / PRISON_SWORDS, 0);
			greatSword(sword, tip, 34, 810 + k);
		}
		// The walls between them, standing up along the ring (drawn after the swords: they are seen through).
		SpellArt.Bone walls = m.bone("walls", "root", 0, 0, 0);
		double chord = 2 * PRISON_R * Math.sin(Math.PI / PRISON_SWORDS);
		for (int k = 0; k < PRISON_SWORDS; k++) {
			double mid = 360.0 * (k + 0.5) / PRISON_SWORDS, r = PRISON_R * Math.cos(Math.PI / PRISON_SWORDS);
			SpellArt.sheet(walls, Math.sin(Math.toRadians(mid)) * r, 11, Math.cos(Math.toRadians(mid)) * r, chord - 2, 22, 90, mid, 0, swordWall(830 + k)).density(1.25);
		}
		SpellArt.Bone top = m.bone("top", "root", 0, PRISON_TOP, 0);
		greatSword(top, SpellArt.v3(0, PRISON_TOP, 0), 44, 850);
		SpellArt.Bone bottom = m.bone("bottom", "root", 0, -52, 0, 0, 0, 180);
		greatSword(bottom, SpellArt.v3(0, -52, 0), 40, 860);
		prisonDescend(m);
		prisonHold(m);
		converge(m);
	}

	/**
	 * Its forming: the eight swords come out of their rifts high over the ring, hang a moment, and plunge into the land; the walls spring up
	 * between them and the floor's light kindles as they strike; the sword above comes into its rift and waits there.
	 */
	static void prisonDescend(SpellArt.Model m) {
		double L = 1.2;
		SpellArt.Anim a = m.anim("descend", L, null);
		for (int k = 0; k < PRISON_SWORDS; k++) {
			String n = "sword_" + k;
			a.pos(n, 0, 0, PRISON_FALL + PRISON_SINK, 0);
			a.scale(n, 0, 0.15);
			a.scale(n, 0.5, 1);
			a.pos(n, 0.55 + k * 0.01, 0, PRISON_FALL + PRISON_SINK, 0);
			a.sample(n, "position", 0.55 + k * 0.01, 1.12, 5, t -> new double[] {0, (PRISON_FALL + PRISON_SINK) * (1 - SpellArt.easeIn((t - 0.55) / 0.57)), 0});
			a.pos(n, L, 0, 0, 0);
			a.scale(n, L, 1);
		}
		a.scale("walls", 0, 1, 0.02, 1);
		a.scale("walls", 1.1, 1, 0.02, 1);
		a.scale("walls", L, 1, 1, 1);
		a.scale("floor", 0, 0.02);
		a.scale("floor", 1.1, 0.02);
		a.scale("floor", L, 1);
		a.scale("top", 0, 0.1);
		a.scale("top", 0.6, 1);
		a.scale("top", L, 1);
		a.scale("bottom", 0, 0.02);
		a.scale("bottom", L, 0.02);
	}

	/** Its holding, looped without a seam: the swords thrumming, the walls shimmering, the sword above turning slowly in its rift. */
	static void prisonHold(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("hold", T, "true");
		for (int k = 0; k < PRISON_SWORDS; k++) {
			int kk = k;
			a.sample("sword_" + k, "position", 0, T, 16, t -> new double[] {0, 0.3 * Math.sin(2 * Math.PI * t / T * 8 + kk), 0});
		}
		a.sample("walls", "scale", 0, T, 8, t -> {
			double p = 1 + 0.012 * Math.sin(2 * Math.PI * t / T * 2);
			return new double[] {p, 1, p};
		});
		a.sample("top", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		a.scale("bottom", 0, 0.02);
		a.scale("bottom", T, 0.02);
	}

	/**
	 * The last two meeting (held at its end: its spell ends first): the sword above plunging down out of its rift and the one below thrusting up
	 * out of the land, point to point in its heart.
	 */
	static void converge(SpellArt.Model m) {
		double L = 2.4, meet = 0.7, meetY = 13;
		SpellArt.Anim a = m.anim("converge", L, "hold_on_last_frame");
		a.sample("top", "position", 0, meet, 6, t -> new double[] {0, -(PRISON_TOP - meetY) * SpellArt.easeIn(t / meet), 0});
		a.pos("top", L, 0, -(PRISON_TOP - meetY), 0);
		a.scale("bottom", 0, 0.02);
		a.scale("bottom", 0.03, 1);
		a.scale("bottom", L, 1);
		a.pos("bottom", 0, 0, 0, 0);
		a.pos("bottom", 0.1, 0, 0, 0);
		a.sample("bottom", "position", 0.1, meet, 5, t -> new double[] {0, (52 + meetY) * SpellArt.easeOut((t - 0.1) / (meet - 0.1)), 0});
		a.pos("bottom", L, 0, 52 + meetY, 0);
		a.scale("walls", 0, 1);
		a.scale("walls", meet, 1.16);
		a.scale("walls", meet + 0.3, 1);
		a.scale("walls", L, 1);
	}

	// --- Rift to the Nine Nethers: its whirl ---

	/** A paper talisman (on a blade hanging down: v 0 at its top): yellow paper, glyphs of cinnabar down it, ghost fire along its edges. */
	static SpellArt.Painter talisman(int seed) {
		return t -> {
			double u = t.u(), v = t.v();
			if (v > 0.96) return SpellArt.CLEAR;
			double edge = Math.min(Math.min(u, 1 - u), 0.96 - v);
			if (edge < 0.05) return SpellArt.glow(Ghost.FIRE_CYAN, 0.8);
			if (Math.abs(u - 0.5) < 0.3 && v > 0.08 && v < 0.88) {
				double cell = (v - 0.08) / 0.8 * 4;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci + seed * 4, seed, (u - 0.2) / 0.6, cell - ci)) return SpellArt.col(Ghost.CINNABAR);
			}
			return SpellArt.col(SpellArt.mix(0xD8B850, 0xF0DC8C, SpellArt.noise(u * 6 + seed, v * 9, seed)), 0.95);
		};
	}

	/** One of the Nine Nethers' rings (on a limb going round it): a band of ghost fire, the same all along (so, turning, its seams never show). */
	static SpellArt.Painter netherBand() {
		return t -> SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.15 + 0.6 * (1 - Math.abs(t.fx - 0.5) * 2)), Ghost.GHOST_FIRE), 0.7);
	}

	static final int NETHER_RINGS = 9;

	/** Ring {@code k} of the whirl (0 the lowest and narrowest, at the rift's mouth): its radius, its height, its flames. */
	static double netherRadius(int k) { return 7 + 4.6 * k; }
	static double netherY(int k) { return 0.4 + 2.5 * k; }
	/** Its flames, tall and short by turns: always an even number, so a turn of two of them a loop comes round seamlessly (8, 12, 14 ... 32). */
	static int netherFlames(int k) { return 2 * ((9 + 3 * k) / 2); }

	/**
	 * Rift to the Nine Nethers' whirl, over the rift in the land at the origin: nine rings of ghost fire one over another ("ring_0".."ring_8"),
	 * widening as they rise, each turning its own way (the lowest the swiftest), souls of the dead dragged round on them ("soul_0".."soul_7"),
	 * and nine talismans hanging round its rim ("talisman_0".."talisman_8").
	 */
	static void nineNethers(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int k = 0; k < NETHER_RINGS; k++) {
			double r = netherRadius(k), y = netherY(k);
			int n = netherFlames(k);
			SpellArt.Bone ring = m.bone("ring_" + k, "root", 0, y, 0);
			for (int j = 0; j < n; j++) {
				double a0 = 2 * Math.PI * j / n, a1 = 2 * Math.PI * (j + 1) / n;
				double[] p0 = SpellArt.v3(Math.sin(a0) * r, y, Math.cos(a0) * r), p1 = SpellArt.v3(Math.sin(a1) * r, y, Math.cos(a1) * r);
				double[] d = SpellArt.sub(p1, p0), ext = SpellArt.times(d, 0.3 / SpellArt.len(d));
				SpellArt.limb(ring, SpellArt.sub(p0, ext), SpellArt.add(p1, ext), 0.7, 0.6, netherBand()).density(2);
				double th = 360.0 * j / n;
				// Its flames leaning with its turning, toward the next.
				SpellArt.shared(m, ring, "nether_flame_" + k + "_" + (j % 2), false, p0, 7.5 + 0.6 * k + (j % 2) * 2, 4.6, (k % 2 == 0 ? 1 : -1) * 14, th + 90, 0,
						Ghost.ghostFire(1.8, 900 + k * 2 + j % 2), 2, false);
			}
		}
		// Souls dragged round: a hood and its wisps, on every second ring.
		for (int k = 0; k < 8; k++) {
			int ringIndex = 1 + k;
			double r = netherRadius(ringIndex), y = netherY(ringIndex) + 2.6, th = Math.toRadians(k * 137.5);
			double[] at = SpellArt.v3(Math.sin(th) * r, y, Math.cos(th) * r);
			SpellArt.Bone soul = m.bone("soul_" + k, "root", 0, y, 0);
			soul.box(at[0], at[1], at[2], 3.6, 3.8, 3.2, Ghost.hood(920 + k)).rot(0, Math.toDegrees(th) + 90, 0).density(3);
			soul.box(at[0], at[1] - 2.8, at[2], 3.2, 2.4, 2.6, Ghost.robe(925 + k)).rot(0, Math.toDegrees(th) + 90, 0).density(3);
			SpellArt.blade(soul, SpellArt.add(at, SpellArt.v3(0, -3.6, 0)), 8, 3.2, 150, Math.toDegrees(th) + 90, 0, Ghost.wisp(930 + k)).density(2.5);
		}
		// The talismans round its rim.
		double rim = netherRadius(NETHER_RINGS - 1) + 3.5, top = netherY(NETHER_RINGS - 1) + 8;
		for (int k = 0; k < 9; k++) {
			double th = Math.toRadians(k * 40 + 20);
			double[] at = SpellArt.v3(Math.sin(th) * rim, top, Math.cos(th) * rim);
			SpellArt.Bone tal = m.bone("talisman_" + k, "root", at[0], at[1], at[2]);
			SpellArt.blade(tal, at, 7, 2.8, 180, Math.toDegrees(th), 0, talisman(940 + k)).density(4);
		}
		netherOpen(m);
		netherTurn(m);
		netherSeal(m);
	}

	/** Its opening: the rings spread out of the rift one over another from the lowest, the souls and talismans after. */
	static void netherOpen(SpellArt.Model m) {
		double L = 0.8;
		SpellArt.Anim a = m.anim("open", L, null);
		for (int k = 0; k < NETHER_RINGS; k++) {
			double t0 = 0.04 * k;
			a.scale("ring_" + k, 0, 0.02);
			if (t0 > 0) a.scale("ring_" + k, t0, 0.02);
			a.scale("ring_" + k, t0 + 0.35, 1.06);
			a.scale("ring_" + k, L, 1);
		}
		for (int k = 0; k < 8; k++) {
			a.scale("soul_" + k, 0, 0.02);
			a.scale("soul_" + k, 0.5, 0.02);
			a.scale("soul_" + k, L, 1);
		}
		for (int k = 0; k < 9; k++) {
			a.scale("talisman_" + k, 0, 0.02);
			a.scale("talisman_" + k, 0.55, 0.02);
			a.scale("talisman_" + k, L, 1);
		}
	}

	/** Its turning, looped without a seam: each ring wheeling its own way, the lowest swiftest; the souls dragged round; the talismans fluttering. */
	static void netherTurn(SpellArt.Model m) {
		double T = 4;
		SpellArt.Anim a = m.anim("turn", T, "true");
		for (int k = 0; k < NETHER_RINGS; k++) {
			int n = netherFlames(k), times = Math.max(1, 4 - k / 3);
			double turn = (k % 2 == 0 ? 1 : -1) * 720.0 / n * times;
			a.sample("ring_" + k, "rotation", 0, T, 8, t -> new double[] {0, turn * t / T, 0});
		}
		for (int k = 0; k < 8; k++) {
			double turn = ((1 + k) % 2 == 0 ? 1 : -1) * 360;
			a.sample("soul_" + k, "rotation", 0, T, 16, t -> new double[] {0, turn * t / T, 0});
		}
		for (int k = 0; k < 9; k++) {
			int kk = k;
			a.sample("talisman_" + k, "rotation", 0, T, 16, t -> new double[] {12 * Math.sin(2 * Math.PI * t / T * 3 + kk), 0, 10 * Math.sin(2 * Math.PI * t / T * 2 + kk * 1.7)});
		}
	}

	/** Its sealing (held): the rings drawn in and down into the rift from the outermost, the souls swallowed, the talismans burnt away. */
	static void netherSeal(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("seal", L, "hold_on_last_frame");
		for (int k = 0; k < NETHER_RINGS; k++) {
			double t0 = 0.03 * (NETHER_RINGS - 1 - k);
			a.scale("ring_" + k, 0, 1);
			a.scale("ring_" + k, t0, 1);
			a.scale("ring_" + k, Math.min(L, t0 + 0.3), 0.02);
			a.scale("ring_" + k, L, 0.02);
		}
		for (int k = 0; k < 8; k++) {
			a.scale("soul_" + k, 0, 1);
			a.scale("soul_" + k, 0.2, 0.02);
			a.scale("soul_" + k, L, 0.02);
		}
		for (int k = 0; k < 9; k++) {
			a.scale("talisman_" + k, 0, 1);
			a.scale("talisman_" + k, 0.3, 0.02);
			a.scale("talisman_" + k, L, 0.02);
		}
	}

	// --- Bridge of No Return: the Naihe Bridge ---

	/** The River of Forgetting under the bridge (on a sheet, flowing along x): dark water, ghost lights in it, red petals of the other shore's lilies drifting. */
	static SpellArt.Painter forgetting(int seed) {
		return t -> {
			double x = t.across(), z = t.back(), edge = Math.min(z, 1 - z);
			if (edge < 0.04) return SpellArt.CLEAR;
			double flow = SpellArt.fbm(x * 3 + seed, z * 6, seed, 3);
			double petal = SpellArt.hash((int) Math.floor(x * 46), (int) Math.floor(z * 18), seed);
			if (petal > 0.985) return SpellArt.glow(0xE0303A, 0.9);
			if (flow > 0.68) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_TEAL, Ghost.FIRE_PALE, (flow - 0.68) * 3), 0.6);
			return SpellArt.col(SpellArt.mix(Ghost.VOID, Ghost.MIST_DEEP, flow), 0.78 * SpellArt.smooth(0.04, 0.2, edge));
		};
	}

	/** A red spider lily of the other shore (on a blade, its petal up it): crimson curling to its tip, which glows. */
	static SpellArt.Painter lily(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, curl = 0.18 * Math.sin(v * 5 + seed) * v, half = 0.32 * (1 - v * 0.8);
			if (Math.abs(u - curl) > half) return SpellArt.CLEAR;
			if (v > 0.8) return SpellArt.glow(SpellArt.mix(0xFF5060, 0xFFC0C0, (v - 0.8) * 5), 0.92);
			return SpellArt.glow(SpellArt.mix(0x7A0812, 0xE0202E, v), 0.9);
		};
	}

	static final double SPAN_HALF = 26, DECK_W = 6, DECK_RISE = 8, DECK_FOOT = 3;

	/** The deck's height {@code z} along the span (its middle at 0): low at its ends, its arch rising to the middle. */
	static double deckY(double z) {
		double f = z / SPAN_HALF;
		return DECK_FOOT + DECK_RISE * (1 - f * f);
	}

	/**
	 * One span of the Naihe Bridge (奈何桥), that the dead cross over the River of Forgetting, twenty-four blocks long along z, its middle at the
	 * origin on the land: its deck of bone-white stone arching up and down again, the stone of its arch beneath it ("span"); its posts and rails
	 * either side; lanterns of ghost fire hanging from its ends and its middle ("lantern_0".."lantern_5"); under it the dark river with its ghost
	 * lights and drifting petals ("river") and mist over that ("mist"); and red spider lilies of the other shore by its feet ("lilies").
	 */
	static void naiheBridge(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone span = m.bone("span", "root", 0, 0, 0);
		SpellArt.Painter stone = Ghost.boneStone(1001, false), carved = Ghost.boneStone(1003, true);
		int pieces = 8;
		double step = 2 * SPAN_HALF / pieces;
		for (int i = 0; i < pieces; i++) {
			double z0 = -SPAN_HALF + i * step, z1 = z0 + step, zm = (z0 + z1) / 2, y0 = deckY(z0), y1 = deckY(z1);
			double slope = Math.toDegrees(Math.atan2(y1 - y0, step));
			span.box(0, (y0 + y1) / 2 - 0.8, zm, 2 * DECK_W, 1.6, step + 0.5, stone).rot(slope, 0, 0).density(1.5);
			// Beneath the deck: stone down to the arch's curve (an opening under its middle), or to the land at its ends.
			double archTop = Math.abs(zm) < 20 ? 8 * (1 - (zm / 20) * (zm / 20)) : 0, under = (y0 + y1) / 2 - 1.6 - archTop;
			if (under > 0.6) span.box(0, archTop + under / 2, zm, 2 * DECK_W - 1.4, under, step + 0.3, carved).density(1.25);
		}
		// Posts and rails either side.
		for (int s = -1; s <= 1; s += 2) {
			double x = s * (DECK_W - 0.8);
			double[] prevTop = null, prevLow = null;
			for (int i = 0; i <= pieces; i++) {
				double z = -SPAN_HALF + i * step, y = deckY(z);
				span.box(x, y + 2.2, z, 1.6, 4.4, 1.6, stone).density(2);
				span.box(x, y + 4.8, z, 1.4, 0.9, 1.4, stone).rot(0, 45, 0).density(3);
				double[] top = SpellArt.v3(x, y + 3.7, z), low = SpellArt.v3(x, y + 1.7, z);
				if (prevTop != null) {
					SpellArt.limb(span, prevTop, top, 0.9, 0.9, stone).density(2);
					SpellArt.limb(span, prevLow, low, 0.7, 0.7, stone).density(2);
				}
				prevTop = top;
				prevLow = low;
			}
		}
		// Lanterns of ghost fire hanging from arms at its ends and its middle.
		double[] lanternZ = {-SPAN_HALF, 0, SPAN_HALF};
		int lantern = 0;
		for (int s = -1; s <= 1; s += 2) {
			for (double z : lanternZ) {
				double y = deckY(z) + 4.4, x = s * (DECK_W - 0.8);
				double[] arm = SpellArt.v3(x + s * 2.6, y + 0.6, z);
				SpellArt.limb(span, SpellArt.v3(x, y, z), arm, 0.6, 0.6, Ghost.pole()).density(3);
				SpellArt.Bone b = m.bone("lantern_" + lantern++, "span", arm[0], arm[1], arm[2]);
				b.box(arm[0], arm[1] - 2.6, arm[2], 2.6, 3.2, 2.6, Ghost.lantern(1010 + lantern)).density(4);
				SpellArt.shared(m, b, "lantern_flame", false, SpellArt.v3(arm[0], arm[1] - 1.0, arm[2]), 2.6, 1.8, 0, 0, 0, Ghost.ghostFire(1.4, 1021), 4, false);
				SpellArt.shared(m, b, "lantern_flame", false, SpellArt.v3(arm[0], arm[1] - 1.0, arm[2]), 2.6, 1.8, 0, 90, 0, Ghost.ghostFire(1.4, 1021), 4, false);
			}
		}
		// The lilies of the other shore by its feet.
		SpellArt.Bone lilies = m.bone("lilies", "root", 0, 0, 0);
		for (int end = -1; end <= 1; end += 2) {
			for (int c = 0; c < 4; c++) {
				double[] at = SpellArt.v3((c - 1.5) * 4.6 + (c % 2) * 1.2, 0, end * (SPAN_HALF - 2 + (c % 2) * 2.4));
				for (int p = 0; p < 5; p++) {
					SpellArt.shared(m, lilies, "lily_" + p % 2, false, SpellArt.add(at, SpellArt.v3(0, 0.6, 0)), 3.6 + (p % 2), 1.6, 36, p * 72 + c * 20, 0, lily(1030 + p % 2), 4, false);
				}
				SpellArt.limb(lilies, at, SpellArt.add(at, SpellArt.v3(0, 1.2, 0)), 0.4, 0.4, t -> SpellArt.col(0x2A5030)).density(4);
			}
		}
		// The river under it, and the mist over the river (drawn last: seen through).
		SpellArt.Bone river = m.bone("river", "root", 0, 0.2, 0);
		SpellArt.sheet(river, 0, 0.2, 0, 76, 30, 0, 0, 0, forgetting(1041)).density(1);
		SpellArt.Bone mist = m.bone("mist", "root", 0, 2, 0);
		SpellArt.sheet(mist, 0, 2, 0, 64, 58, 0, 0, 0, Ghost.mistBank(1051)).density(1);
		SpellArt.sheet(mist, 0, 5, 0, 48, 40, 0, 20, 0, Ghost.mistBank(1052)).density(1);
		bridgeForm(m);
		bridgeStand(m);
	}

	/** Its rising: the span lifts up out of the mist, the lanterns kindling. */
	static void bridgeForm(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("form", L, null);
		a.sample("span", "position", 0, L, 5, t -> new double[] {0, -16 * (1 - SpellArt.easeOut(t / L)), 0});
		a.sample("span", "scale", 0, L, 5, t -> {
			double s = 0.7 + 0.3 * SpellArt.easeOut(t / L);
			return new double[] {s, s, s};
		});
		a.scale("lilies", 0, 0.02);
		a.scale("lilies", 0.3, 0.02);
		a.scale("lilies", L, 1);
		a.scale("river", 0, 0.4, 1, 1);
		a.scale("river", L, 1);
	}

	/** Its standing, looped without a seam: the lanterns swaying and their flames flickering, the river drifting under it, the mist rolling, the lilies nodding. */
	static void bridgeStand(SpellArt.Model m) {
		double T = 3;
		SpellArt.Anim a = m.anim("stand", T, "true");
		for (int k = 0; k < 6; k++) {
			int kk = k;
			a.sample("lantern_" + k, "rotation", 0, T, 12, t -> new double[] {8 * Math.sin(2 * Math.PI * t / T + kk), 0, 6 * Math.sin(2 * Math.PI * t / T + kk * 1.9)});
		}
		a.sample("river", "position", 0, T, 12, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T), 0, 0});
		a.sample("mist", "position", 0, T, 12, t -> new double[] {2.4 * Math.sin(2 * Math.PI * t / T + 1), 0.4 * Math.sin(4 * Math.PI * t / T), 0});
		a.sample("lilies", "rotation", 0, T, 12, t -> new double[] {1.5 * Math.sin(2 * Math.PI * t / T), 0, 1.5 * Math.sin(2 * Math.PI * t / T + 2)});
	}

	// --- Heaven-Demon Void Shatter: the fist ---

	/** The sun on the fist's back (on the top face of a plate): white light in a ring, the ring gold on heaven's side and fire on the demon's. */
	static SpellArt.Painter fistEmblem() {
		return t -> {
			if (t.f != SpellArt.Face.UP) return SpellArt.glow(HeavenDemon.GOLD, 0.94);
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.98) return SpellArt.col(HeavenDemon.BLOOD_DARK);
			if (r > 0.72) return SpellArt.glow(x < 0 ? HeavenDemon.GOLD_BRIGHT : HeavenDemon.SCARLET, 0.97);
			double ray = Math.pow(Math.abs(Math.cos(Math.atan2(z, x) * 4)), 6);
			return SpellArt.glow(SpellArt.mix(HeavenDemon.WHITE_LIGHT, HeavenDemon.GOLD_PALE, r * (1 - ray)), 0.98);
		};
	}

	/** A ring of the two round the fist's arm (on a limb going round it): gold light or crimson, the same all along (so, spinning, it shows no seam). */
	static SpellArt.Painter armRing(boolean heaven) {
		return t -> {
			double middle = 1 - Math.abs(t.fx - 0.5) * 2;
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.45 + 0.55 * middle), heaven ? HeavenDemon.HEAVEN : HeavenDemon.DEMON), 0.94);
		};
	}

	/**
	 * Heaven-Demon Void Shatter's fist, some fifteen blocks across, punching toward -z: its hand clenched ("fist"), heaven's half (-x) armoured in
	 * plates of gold light, the demon's (+x) crimson sinew with spikes of horn off its knuckles, a seam of white light between; heaven's sun on its
	 * back; its cuff and forearm behind it; rings of the two turning round the arm ("ring_0", "ring_1"); and gold light and crimson qi streaming
	 * back off it ("aura").
	 */
	static void heavenDemonFist(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone fist = m.bone("fist", "root", 0, 0, 0);
		SpellArt.Painter skin = HeavenDemon.halves(HeavenDemon.heavenly(1101), HeavenDemon.demonic(1102));
		// The hand: the back of it, the knuckles in a row at its front, the fingers curled under, their tips tucked in.
		fist.box(0, 0, -4, 30, 18, 20, skin);
		for (int k = 0; k < 4; k++) {
			double x = -11.4 + k * 7.6;
			fist.box(x, 3, -16.5, 6.6, 7.4, 6.4, skin).density(1.5);
			fist.box(x, -5, -15.5, 6.4, 6.4, 7.4, skin).density(1.5);
			fist.box(x, -9.6, -9, 6.2, 5, 6.4, skin).density(1.5);
			if (x < 0) fist.box(x, 3.4, -19.9, 5.6, 6, 0.8, HeavenDemon.heavenly(1110 + k)).density(3); // heaven's knuckle-plates
			else SpellArt.limb(fist, SpellArt.v3(x, 5.5, -18), SpellArt.v3(x + 1.2, 9.5, -25), 2, 2, HeavenDemon.horn(1120 + k)).density(3); // the demon's spikes
		}
		// The thumb, wrapped across the front of the fingers from heaven's side.
		SpellArt.limb(fist, SpellArt.v3(-15, -3, -3), SpellArt.v3(-14, -5.5, -14), 6, 6.4, skin).density(1.5);
		SpellArt.limb(fist, SpellArt.v3(-14, -5.5, -14), SpellArt.v3(-6, -7, -20), 5.4, 5.4, skin).density(1.5);
		fist.box(0, 9.3, -5, 11, 0.8, 11, fistEmblem()).density(3);
		// The cuff and the forearm, tapering behind it.
		fist.box(0, 0, 10, 26, 17, 10, skin);
		fist.box(0, 0, 10, 27, 3, 11, t -> t.px < 0 ? SpellArt.glow(HeavenDemon.GOLD_BRIGHT, 0.95) : SpellArt.glow(HeavenDemon.SCARLET, 0.95)).density(1.5);
		fist.box(0, 0, 20, 21, 14, 10, skin);
		fist.box(0, 0, 29, 17, 11, 9, skin);
		// The rings round the arm.
		double[][] rings = {{19, 15}, {29, 12}};
		for (int r = 0; r < rings.length; r++) {
			SpellArt.Bone ring = m.bone("ring_" + r, "root", 0, 0, rings[r][0]);
			int n = 14;
			for (int j = 0; j < n; j++) {
				double a0 = 2 * Math.PI * j / n, a1 = 2 * Math.PI * (j + 1) / n, rad = rings[r][1];
				double[] p0 = SpellArt.v3(Math.cos(a0) * rad, Math.sin(a0) * rad, rings[r][0]), p1 = SpellArt.v3(Math.cos(a1) * rad, Math.sin(a1) * rad, rings[r][0]);
				double[] d = SpellArt.sub(p1, p0), ext = SpellArt.times(d, 0.3 / SpellArt.len(d));
				SpellArt.limb(ring, SpellArt.sub(p0, ext), SpellArt.add(p1, ext), 1.4, 1.4, armRing(r == 0)).density(2);
			}
		}
		// Gold light and crimson qi streaming back off it.
		SpellArt.Bone aura = m.bone("aura", "root", 0, 0, 14);
		for (int k = 0; k < 14; k++) {
			double a = 2 * Math.PI * k / 14, x = Math.cos(a) * 11, y = Math.sin(a) * 8;
			boolean heaven = x < 0;
			SpellArt.shared(m, aura, "aura_" + k, true, SpellArt.v3(x, y, 12 + (k % 3) * 4), 26 + (k % 3) * 5, 9,
					-Math.sin(a) * 10, Math.cos(a) * 12, 0, heaven ? HeavenDemon.heavenFlame(2.2, 1130 + k % 2) : HeavenDemon.demonFlame(2.2, 1140 + k % 2), 1.5, false);
		}
		fistFly(m);
		fistImpact(m);
	}

	/** Its flight, looped without a seam: the rings spinning round the arm each its own way, the light streaming off it flickering, the fist shaking with its force. */
	static void fistFly(SpellArt.Model m) {
		double T = 0.8;
		SpellArt.Anim a = m.anim("fly", T, "true");
		a.sample("ring_0", "rotation", 0, T, 8, t -> new double[] {0, 0, 360 * t / T});
		a.sample("ring_1", "rotation", 0, T, 8, t -> new double[] {0, 0, -360 * t / T});
		a.sample("aura", "scale", 0, T, 8, t -> new double[] {1 + 0.06 * Math.sin(4 * Math.PI * t / T), 1 + 0.06 * Math.sin(4 * Math.PI * t / T + 1), 1 + 0.1 * Math.sin(4 * Math.PI * t / T + 2)});
		a.sample("fist", "position", 0, T, 16, t -> new double[] {0.25 * Math.sin(2 * Math.PI * t / T * 4), 0.25 * Math.sin(2 * Math.PI * t / T * 4 + 1.3), 0});
	}

	/** Its impact (held): the fist driven in and rebounding, the light streaming off it bursting out wide, the rings flung out. */
	static void fistImpact(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("impact", 0.8, "hold_on_last_frame");
		a.scale("fist", 0, 1);
		a.scale("fist", 0.08, 1.05, 1.05, 0.82);
		a.scale("fist", 0.2, 0.98, 0.98, 1.06);
		a.scale("fist", 0.4, 1);
		a.scale("fist", 0.8, 1);
		a.scale("aura", 0, 1);
		a.scale("aura", 0.3, 2.6);
		a.scale("aura", 0.8, 3);
		for (int r = 0; r < 2; r++) {
			a.scale("ring_" + r, 0, 1);
			a.scale("ring_" + r, 0.4, 3.4);
			a.scale("ring_" + r, 0.8, 3.8);
		}
	}
}
