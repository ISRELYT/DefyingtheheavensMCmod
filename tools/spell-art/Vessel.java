/**
 * The looks of Sacred Vessel's great spells for SpellArt (2026-10-24): the sacred treasures called down and wielded, each wrought of what it is
 * cast from. Wood's Heaven-Swallowing Gourd and Seven-Treasure Bodhi Branch; fire's Wind-Fire Wheel and Eight Trigrams Furnace; earth's clod of
 * Xirang and the cauldrons of Yu; water's sea-fixing pearl and the ocean-sealing vase; metal's ring of binding rope (its ropes the client draws by
 * hand: TendrilRenderer) and the golden dragon scissors; the ghost foundation's mirror of retribution and soul-locking pagoda; both paths' bell of
 * heaven and demon. At the render scale (4) and its realm's greatness (2.13) a model pixel is some half a block.
 */
public class Vessel {
	// --- Shared shapes ---

	/** An octagon's side, of its width across its flats. */
	static final double OCTAGON_SIDE = 2 * Math.tan(Math.PI / 8);

	/**
	 * A round of {@code r} (pixels) {@code h} tall about (cx, cy, cz): a true octagon, four slabs as long as it is wide and as deep as its side,
	 * turned 45 degrees apart (their union is the octagon: no notches), its top and bottom left off unless {@code capped}.
	 */
	static void round(SpellArt.Bone bone, double cx, double cy, double cz, double r, double h, SpellArt.Painter p, boolean capped) {
		for (int k = 0; k < 4; k++) {
			SpellArt.Cube q = bone.box(cx, cy, cz, 2 * r, h, r * OCTAGON_SIDE, p);
			if (k > 0) q.rot(0, 45 * k, 0);
			if (!capped) q.skip(SpellArt.Face.UP, SpellArt.Face.DOWN);
		}
	}

	/**
	 * A lathe-turned body: rounds a {@code step} tall from {@code y0} to {@code y1}, each as wide as {@code radius} says at its middle; their sides
	 * only, and a thin cap (painted coarse: only its rim shows) where the body steps in going up or out going down, and at its two ends.
	 */
	static void lathe(SpellArt.Bone bone, double y0, double y1, double step, java.util.function.DoubleUnaryOperator radius, SpellArt.Painter p) {
		for (double y = y0; y < y1 - 1e-6; y += step) {
			double h = Math.min(step, y1 - y), r = radius.applyAsDouble(y + h / 2);
			if (r < 0.3) continue;
			round(bone, 0, y + h / 2, 0, r, h + 0.02, p, false);
			double above = y + h < y1 - 1e-6 ? radius.applyAsDouble(y + h + Math.min(step, y1 - y - h) / 2) : 0, below = y > y0 + 1e-6 ? radius.applyAsDouble(y - step / 2) : 0;
			double tpp = Math.min(1, 6 / r);
			if (above < r - 0.25) cap(bone, y + h, r, p, tpp, SpellArt.Face.UP);
			if (below < r - 0.25) cap(bone, y, r, p, tpp, SpellArt.Face.DOWN);
		}
	}

	/** A round's cap at {@code y}, facing {@code face} (up or down): a thin round, its one broad face painted at {@code tpp}. */
	static void cap(SpellArt.Bone bone, double y, double r, SpellArt.Painter p, double tpp, SpellArt.Face face) {
		for (int k = 0; k < 4; k++) {
			SpellArt.Cube q = bone.box(0, y, 0, 2 * r, 0.1, r * OCTAGON_SIDE, p).only(face).density(tpp);
			if (k > 0) q.rot(0, 45 * k, 0);
		}
	}

	/** A ring of {@code n} boxes {@code thick} round in the level plane, {@code r} out from (cx, cy, cz), each turned to lie along the ring. */
	static void ring(SpellArt.Bone bone, double cx, double cy, double cz, double r, double thick, int n, SpellArt.Painter p) {
		double chord = 2 * r * Math.sin(Math.PI / n) * 1.08;
		for (int k = 0; k < n; k++) {
			double th = 360.0 * k / n;
			double x = cx + Math.sin(Math.toRadians(th)) * r, z = cz + Math.cos(Math.toRadians(th)) * r;
			bone.box(x, cy, z, chord, thick, thick, p).rot(0, th, 0);
		}
	}

	/** A ring of {@code n} boxes standing upright, in the y-z plane (a wheel's rim), {@code r} out from (cx, cy, cz). */
	static void wheelRing(SpellArt.Bone bone, double cx, double cy, double cz, double r, double thick, double wide, int n, SpellArt.Painter p) {
		double chord = 2 * r * Math.sin(Math.PI / n) * 1.08;
		for (int k = 0; k < n; k++) {
			double th = 360.0 * k / n;
			double y = cy + Math.cos(Math.toRadians(th)) * r, z = cz + Math.sin(Math.toRadians(th)) * r;
			bone.box(cx, y, z, wide, thick, chord, p).rot(-th, 0, 0);
		}
	}

	// --- Heaven-Swallowing Gourd ---

	static final int GOURD_DARK = 0x0E3A2A, GOURD = 0x1F6B48, GOURD_LIGHT = 0x48A872, GOURD_PALE = 0xA6E8B8, GOURD_GLOW = 0x8CFFC0, CORD = 0xC2281E, CORD_DARK = 0x6A0E0A;

	/**
	 * The gourd's living jade wood (round a body standing up its y): deep green lacquered smooth, its grain running up it in faint waving lines,
	 * lit pale where it faces up and the light; veins of green light rising through it here and there.
	 */
	static SpellArt.Painter gourdWood(int seed) {
		return t -> {
			double ang = Math.atan2(t.qx(), t.qz());
			double grain = Math.sin(ang * 26 + Math.sin(t.qy() * 0.18 + seed) * 2.2 + SpellArt.noise(t.qy() * 0.2, ang * 3, seed) * 3);
			double n = SpellArt.fbm3(t.px * 0.12, t.py * 0.12, t.pz * 0.12, seed, 3);
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.05, t.py * 0.12, t.pz * 0.05, seed + 3, 3) - 1), 14);
			if (vein > 0.8 && SpellArt.noise3(t.px * 0.08, t.py * 0.08, t.pz * 0.08, seed + 7) > 0.45) return SpellArt.glow(SpellArt.mix(GOURD_GLOW, 0xF0FFF4, (vein - 0.8) * 4), 0.95);
			double light = SpellArt.clamp01(0.42 + (n - 0.5) * 0.5 + 0.06 * grain + 0.28 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny) + 0.12 * Math.max(0, -t.nx));
			return SpellArt.col(SpellArt.ramp(light, GOURD_DARK, GOURD, GOURD_LIGHT, GOURD_PALE));
		};
	}

	/** A band of gold round the gourd, charms written round it in red cinnabar, its edges bright. */
	static SpellArt.Painter charmBand(int seed) {
		return t -> {
			if (t.flat()) return SpellArt.col(Earth.GOLD_PALE);
			double u = t.up();
			if (u < 0.2 || u > 0.8) return SpellArt.col(SpellArt.mix(Earth.GOLD_SCALE, Earth.GOLD_PALE, 0.5));
			double cell = t.fx * 2;
			int ci = (int) Math.floor(cell);
			if (Fire.glyph(ci + t.f.ordinal() * 2 + seed, seed, cell - ci, (u - 0.2) / 0.6)) return SpellArt.glow(0xFF4A2A, 0.96);
			return SpellArt.col(SpellArt.mix(Earth.BRONZE, Earth.GOLD_SCALE, 0.7));
		};
	}

	/** Its mouth's inside (on a sheet facing down): a whirl of green light drawn in round a black heart. */
	static SpellArt.Painter gourdMouth(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.98) return SpellArt.CLEAR;
			double a = Math.atan2(z, x), arm = Math.sin(a * 3 + r * 9 + seed) * 0.5 + 0.5;
			double light = SpellArt.smooth(0.1, 0.9, r) * (0.4 + 0.6 * arm);
			return SpellArt.glow(SpellArt.mix(0x020806, GOURD_GLOW, light), 0.98);
		};
	}

	/** A gourd's leaf (on a blade, its stalk at v 0): broad, five-lobed, jade with paler veins; a little seen through at its edge. */
	static SpellArt.Painter gourdLeaf(int seed) {
		return t -> {
			double v = t.v(), u = (t.u() - 0.5) * 2;
			double lobe = 0.75 + 0.25 * Math.cos(Math.atan2(u, v - 0.45) * 5);
			double r = Math.hypot(u * 0.95, (v - 0.45) * 2);
			if (r > lobe || v < 0.04) return SpellArt.CLEAR;
			double vein = Math.min(Math.abs(u), Math.abs(Math.abs(u) - (v - 0.45) * 0.9));
			if (vein < 0.05) return SpellArt.col(SpellArt.mix(Wood.LEAF_LIGHT, GOURD_PALE, 0.4));
			return SpellArt.col(SpellArt.mix(Wood.LEAF_DARK, Wood.LEAF, 0.4 + 0.5 * SpellArt.noise(u * 4 + seed, v * 5, seed)), r > lobe - 0.08 ? 0.75 : 1);
		};
	}

	/** A cord of red silk (on a limb), twisted. */
	static SpellArt.Painter cord() {
		return t -> {
			double twist = Math.sin((t.ly * 1.6 + t.fx * 6) * Math.PI);
			return SpellArt.col(twist > 0.2 ? CORD : SpellArt.mix(CORD, CORD_DARK, 0.6));
		};
	}

	/** The vine (on a limb): green, ribbed along it. */
	static SpellArt.Painter vine(int seed) {
		return t -> SpellArt.col(SpellArt.mix(Wood.LEAF_DARK, Wood.LEAF, 0.35 + 0.4 * Math.abs(Math.sin(t.fx * Math.PI * 2 + seed)) + 0.2 * Math.max(0, t.ny)));
	}

	/** The gourd's radius {@code y} pixels up from its mouth: its neck, its little bulb, its waist, its great bulb, its rounded top. */
	static double gourdRadius(double y) {
		if (y < 5) return 3.6 + 0.3 * y;
		if (y < 21) return 5.1 + 6.2 * Math.sin(Math.PI * (y - 5) / 16);
		if (y < 24) return 5.4 - 0.3 * (y - 21);
		double t = (y - 24) / 26.0;
		// Swelling out of the waist to its greatest a little past halfway, then rounded over to its crown.
		if (t < 0.5) return 5.2 + (15.5 - 5.2) * Math.pow(Math.sin(Math.PI * t), 0.65);
		return 15.5 * Math.sqrt(Math.max(0, 1 - Math.pow((t - 0.5) / 0.5, 2)));
	}

	/**
	 * Heaven-Swallowing Gourd, hung mouth down, its mouth at the origin, some fifty pixels tall (a gourd as tall as a tower): its two bulbs of
	 * living jade wood ("gourd", "bulb_small", "bulb_great", round its waist two gold bands of red charms and a cord of red silk tied in a knot,
	 * its tails hanging), its mouth's rim of gold and the green whirl inside it ("mouth"), the stopper it loses as it comes ("stopper"), its vine
	 * climbing away up into the sky out of its top, leaves along it ("vine_0".."vine_4"), and leaves round its waist ("leaves").
	 */
	static void swallowingGourd(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone gourd = m.bone("gourd", "root", 0, 0, 0);
		SpellArt.Bone small = m.bone("bulb_small", "gourd", 0, 5, 0), great = m.bone("bulb_great", "gourd", 0, 37, 0);
		SpellArt.Painter wood = gourdWood(401);
		lathe(small, 0, 23, 2, Vessel::gourdRadius, wood);
		lathe(great, 23, 50, 2, Vessel::gourdRadius, wood);
		// Its waist's bands of charms and its cord.
		round(gourd, 0, 22.5, 0, 6.1, 2.2, charmBand(403), true);
		round(gourd, 0, 6.2, 0, 4.7, 1.8, charmBand(405), true);
		ring(gourd, 0, 24.6, 0, 5.8, 1.1, 14, cord());
		SpellArt.limb(gourd, SpellArt.v3(5.6, 24.6, 0), SpellArt.v3(8.5, 17, 1), 1, 1, cord());
		SpellArt.limb(gourd, SpellArt.v3(5.6, 24.6, 0), SpellArt.v3(9.4, 18.5, -1.5), 1, 1, cord());
		gourd.box(6.2, 24.4, 0, 2.2, 2.2, 2.2, cord()).rot(0, 0, 45);
		// Its mouth: a rim of gold, and inside it the whirl of green light (drawn last: see through).
		SpellArt.Bone mouth = m.bone("mouth", "gourd", 0, 0, 0);
		round(mouth, 0, 0.6, 0, 4.0, 1.4, charmBand(407), true);
		SpellArt.Bone whirl = m.bone("whirl", "mouth", 0, 0, 0);
		SpellArt.sheet(whirl, 0, -0.15, 0, 7, 7, 0, 0, 0, gourdMouth(409)).density(4);
		// The stopper it loses as it comes.
		SpellArt.Bone stopper = m.bone("stopper", "root", 0, -2, 0);
		round(stopper, 0, -2, 0, 3, 3.5, charmBand(411), true);
		stopper.box(0, -4.5, 0, 2, 2, 2, cord());
		// Leaves round its waist.
		SpellArt.Bone leaves = m.bone("leaves", "gourd", 0, 25, 0);
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k + 12;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 5.5, 25.5, Math.cos(Math.toRadians(th)) * 5.5);
			SpellArt.shared(m, leaves, "gourd_leaf", false, base, 9, 8, 52 + (k % 2) * 14, th + 180, 0, gourdLeaf(413), 3, k % 2 == 1);
		}
		// Its vine, climbing away into the sky out of its top in five lengths, leaves along it and a curling tendril.
		String up = "gourd";
		double[] at = SpellArt.v3(0, 49, 0);
		for (int k = 0; k < 5; k++) {
			SpellArt.Bone b = m.bone("vine_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, SpellArt.v3(2.2 * Math.sin(k * 1.7), 10, 2.2 * Math.cos(k * 1.3)));
			SpellArt.limb(b, at, next, 1.8 - k * 0.2, 1.8 - k * 0.2, vine(415 + k));
			double th = k * 97;
			SpellArt.shared(m, b, "vine_leaf", false, SpellArt.add(at, SpellArt.v3(0, 5, 0)), 7, 6, 60, th, 0, gourdLeaf(431), 3, k % 2 == 0);
			if (k % 2 == 1) SpellArt.limb(b, SpellArt.add(at, SpellArt.v3(0, 3, 0)), SpellArt.add(at, SpellArt.v3(4 * Math.cos(th), 6, 4 * Math.sin(th))), 0.6, 0.6, vine(441));
			up = b.name;
			at = next;
		}
		gourdDescend(m);
		gourdDraw(m);
		gourdSpit(m);
	}

	/** Its coming (played once): down out of the sky, swaying to rest; its stopper pops off and falls away, spinning. */
	static void gourdDescend(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("descend", L, null);
		a.sample("gourd", "position", 0, 0.7, 7, t -> new double[] {0, 34 * Math.pow(1 - SpellArt.easeOut(t / 0.7), 1), 0});
		a.sample("gourd", "rotation", 0, L, 10, t -> new double[] {6 * Math.sin(t / L * Math.PI * 2) * (1 - t / L), 0, 4 * Math.sin(t / L * Math.PI * 3) * (1 - t / L)});
		a.sample("stopper", "position", 0, 0.7, 7, t -> new double[] {0, 34 * (1 - SpellArt.easeOut(t / 0.7)), 0});
		a.sample("stopper", "position", 0.7, 0.93, 4, t -> new double[] {0, -14 * SpellArt.easeIn((t - 0.7) / 0.23), 0});
		a.rot("stopper", 0.7, 0, 0, 0);
		a.sample("stopper", "rotation", 0.7, 0.93, 4, t -> new double[] {0, 0, 160 * (t - 0.7) / 0.23});
		a.scale("stopper", 0, 1);
		a.scale("stopper", 0.8, 1);
		a.scale("stopper", 0.93, 0.02);
		a.scale("stopper", L, 0.02);
		// Back where it was, unseen (the loop keeps it so).
		a.pos("stopper", L, 0, 0, 0);
		a.rot("stopper", L, 0, 0, 0);
	}

	/**
	 * Its drawing, looped without a seam: its bulbs swelling as it breathes in, the whirl in its mouth turning, its leaves and vine stirred by the
	 * wind it draws; the stopper gone.
	 */
	static void gourdDraw(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("draw", T, "true");
		a.sample("bulb_great", "scale", 0, T, 8, t -> {
			double s = 1 + 0.05 * Math.sin(2 * Math.PI * t / T * 2);
			return new double[] {s, 1 + 0.03 * Math.sin(2 * Math.PI * t / T * 2), s};
		});
		a.sample("bulb_small", "scale", 0, T, 8, t -> {
			double s = 1 + 0.06 * Math.sin(2 * Math.PI * t / T * 2 - 0.8);
			return new double[] {s, 1, s};
		});
		a.sample("whirl", "rotation", 0, T, 8, t -> new double[] {0, -720 * t / T, 0});
		a.sample("leaves", "rotation", 0, T, 8, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T * 2), 0, 4 * Math.cos(2 * Math.PI * t / T * 2)});
		for (int k = 0; k < 5; k++) {
			int kk = k;
			a.sample("vine_" + k, "rotation", 0, T, 8, t -> new double[] {5 * Math.sin(2 * Math.PI * t / T - kk * 0.8), 0, 4 * Math.cos(2 * Math.PI * t / T - kk * 0.7)});
		}
		a.scale("stopper", 0, 0.02);
		a.scale("stopper", T, 0.02);
	}

	/** Its spitting (played once): it heaves back and its bulbs clench, then it lurches as it spits. */
	static void gourdSpit(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("spit", 0.7, null);
		a.pos("gourd", 0, 0, 0, 0);
		a.pos("gourd", 0.2, 0, 4, 0);
		a.pos("gourd", 0.32, 0, -2, 0);
		a.pos("gourd", 0.5, 0, 1, 0);
		a.pos("gourd", 0.7, 0, 0, 0);
		a.scale("bulb_great", 0, 1);
		a.scale("bulb_great", 0.2, 1.1, 0.95, 1.1);
		a.scale("bulb_great", 0.32, 0.86, 1.08, 0.86);
		a.scale("bulb_small", 0, 1);
		a.scale("bulb_small", 0.25, 1.12, 1, 1.12);
		a.scale("bulb_small", 0.35, 0.8, 1.1, 0.8);
		a.scale("stopper", 0, 0.02);
		a.scale("stopper", 0.7, 0.02);
	}

	// --- Seven-Treasure Bodhi Branch ---

	static final int BODHI_BARK_DARK = 0x3A2412, BODHI_BARK = 0x6E4A26, BODHI_BARK_LIGHT = 0xA67C46, BODHI_GOLD = 0xE8C25A, BODHI_LEAF_DARK = 0x4E7A20,
			BODHI_LEAF = 0x8CB83A, BODHI_LEAF_GOLD = 0xE6D25E;
	/** The seven treasures (WoodSpells#TREASURES): gold, silver, lapis, crystal, pearl, agate, coral. */
	static final int[] TREASURES = {0xFFD54A, 0xE6EEF4, 0x2F5BD9, 0xBFF4FF, 0xFFF6E8, 0xD8502E, 0xFF8A9A};

	/** The bodhi's bark (on a limb): warm brown, ridged along it, gold light caught in its ridges; lit on top. */
	static SpellArt.Painter bodhiBark(int seed) {
		return t -> {
			double ridge = Math.abs(Math.sin(t.fx * Math.PI * 3 + SpellArt.noise(t.ly * 0.25, t.fx * 4, seed) * 2.5));
			double n = SpellArt.fbm(t.fx * 5 + seed, t.ly * 0.3, seed, 2);
			if (ridge < 0.08 && n > 0.55) return SpellArt.glow(BODHI_GOLD, 0.9);
			double light = SpellArt.clamp01(0.35 + 0.4 * ridge * n + 0.25 * Math.max(0, t.ny) - 0.2 * Math.max(0, -t.ny));
			return SpellArt.col(SpellArt.ramp(light, BODHI_BARK_DARK, BODHI_BARK, BODHI_BARK_LIGHT));
		};
	}

	/** A bodhi leaf (on a blade, its stalk at v 0): heart-shaped, drawn out to its long dripping tip; green turning to gold at its edge, veins of light. */
	static SpellArt.Painter bodhiLeaf(int seed) {
		return t -> {
			double v = t.v(), u = (t.u() - 0.5) * 2;
			// A heart: two lobes at its foot, its sides drawn in to a long thin tip.
			double half = v < 0.5 ? 0.6 + 0.4 * Math.sin(Math.PI * v / 0.5 * 0.75) : (1 - v) / 0.5 * 0.9 * Math.pow((1 - v) / 0.5, 0.6);
			if (v < 0.03 || Math.abs(u) > half) return SpellArt.CLEAR;
			double edge = 1 - Math.abs(u) / half;
			double vein = Math.min(Math.abs(u), Math.abs(Math.abs(u) - (v - 0.1) * 0.6));
			if (vein < 0.045) return SpellArt.glow(BODHI_LEAF_GOLD, 0.85);
			int c = SpellArt.mix(BODHI_LEAF_DARK, BODHI_LEAF, 0.4 + 0.4 * SpellArt.noise(u * 5 + seed, v * 6, seed));
			return SpellArt.col(SpellArt.mix(BODHI_LEAF_GOLD, c, SpellArt.smooth(0, 0.3, edge)));
		};
	}

	/** One of the treasures (on a cube): a cut gem of {@code rgb}, its facets lit and shadowed by which way they face, a star of light in it; glowing. */
	static SpellArt.Painter treasure(int rgb) {
		return t -> {
			double facet = 0.5 + 0.3 * t.ny + 0.2 * t.nx - 0.15 * t.nz;
			double star = Math.max(0, 1 - Math.hypot(t.fx - 0.5, t.fy - 0.5) * 3.2);
			return SpellArt.glow(SpellArt.mix(SpellArt.shade(rgb, facet - 0.6), 0xFFFFFF, star * 0.6), 0.9 + 0.08 * star);
		};
	}

	/** A thread of gold (on a limb). */
	static SpellArt.Painter thread() {
		return t -> SpellArt.glow(BODHI_GOLD, 0.9);
	}

	/** A bell of gold (on a cube), its mouth dark. */
	static SpellArt.Painter bellGold() {
		return t -> t.f == SpellArt.Face.DOWN ? SpellArt.col(0x3A2A10) : SpellArt.col(SpellArt.mix(Earth.GOLD_SCALE, Earth.GOLD_PALE, 0.3 + 0.5 * Math.max(0, t.ny)));
	}

	/** A ribbon of jade silk (on a blade): pale green, a gold border, a sheen down it. */
	static SpellArt.Painter ribbon(int seed) {
		return t -> {
			double u = t.u();
			if (u < 0.12 || u > 0.88) return SpellArt.col(BODHI_GOLD);
			return SpellArt.col(SpellArt.mix(Wood.JADE, Wood.PALE, 0.3 + 0.4 * Math.abs(Math.sin(t.v() * 9 + seed))), 0.9);
		};
	}

	/** The bough's line, {@code s} of the way out (0 at its foot, 1 at its tip), and how thick it is there. */
	static double[] boughAt(double s) {
		return SpellArt.v3(5 * Math.sin(s * 5.1) * s, 4 * Math.sin(s * 3.3 + 0.6) * s + 6 * s, -160 * s);
	}

	static double boughThick(double s) {
		return 9 - 7 * s;
	}

	/**
	 * Seven-Treasure Bodhi Branch, its foot at the origin (where its caster holds it) and its tip 160 pixels out along -z (85 blocks): its bough of
	 * the bodhi in four lengths ("bough_0".."bough_3", each turning at its foot, so it bends as it sweeps), twigs off it, heart-shaped leaves of
	 * green and gold all along it, the seven treasures hung from its twigs on threads of gold ("treasure_0".."treasure_6", swinging), gold bells,
	 * and a ribbon of jade silk tied to it trailing in the wind ("ribbon").
	 */
	static void bodhiBranch(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		String up = "root";
		int treasure = 0;
		for (int k = 0; k < 4; k++) {
			double s0 = k / 4.0, s1 = (k + 1) / 4.0;
			double[] a0 = boughAt(s0);
			SpellArt.Bone b = m.bone("bough_" + k, up, a0[0], a0[1], a0[2]);
			for (int i = 0; i < 4; i++) {
				double p0 = s0 + (s1 - s0) * i / 4, p1 = s0 + (s1 - s0) * (i + 1) / 4;
				double w = boughThick((p0 + p1) / 2);
				SpellArt.limb(b, boughAt(p0), boughAt(p1), w, w * 0.9, bodhiBark(451 + k * 4 + i));
			}
			// Twigs off this length, each leafed, some hung with a treasure or a bell.
			for (int j = 0; j < 3; j++) {
				double s = s0 + (s1 - s0) * (0.25 + 0.3 * j);
				double[] foot = boughAt(s);
				double side = (j + k) % 2 == 0 ? 1 : -1, th = side * (55 + 15 * j);
				double reach = 22 - 10 * s;
				double[] tip = SpellArt.add(foot, SpellArt.v3(side * reach * 0.8, 3 + 4 * j % 3, reach * -0.45));
				SpellArt.limb(b, foot, tip, 2.4 - 1.4 * s, 2.2 - 1.3 * s, bodhiBark(481 + k * 3 + j));
				for (int q = 0; q < 4; q++) {
					double f = 0.3 + 0.2 * q;
					double[] at = SpellArt.add(foot, SpellArt.times(SpellArt.sub(tip, foot), f));
					SpellArt.shared(m, b, "bodhi_leaf", false, at, 9 - 3 * s, 7 - 2 * s, 35 + 20 * q, th + 90 * q + 30, 25 * side, bodhiLeaf(501), 3, q % 2 == 1);
				}
				if (treasure < 7 && (k * 3 + j) % 2 == 0 || k == 3 && j == 2 && treasure < 7) {
					// A treasure hung on a thread from the twig's tip.
					SpellArt.Bone gem = m.bone("treasure_" + treasure, b.name, tip[0], tip[1], tip[2]);
					SpellArt.limb(gem, tip, SpellArt.add(tip, SpellArt.v3(0, -7, 0)), 0.35, 0.35, thread());
					double size = 4.2 - 0.25 * treasure;
					gem.box(tip[0], tip[1] - 7 - size * 0.6, tip[2], size, size * 1.3, size, treasure(TREASURES[treasure])).rot(0, 45, 0);
					gem.box(tip[0], tip[1] - 7 - size * 0.6, tip[2], size * 0.72, size * 1.6, size * 0.72, treasure(TREASURES[treasure]));
					treasure++;
				} else {
					b.box(tip[0], tip[1] - 2.5, tip[2], 2.2, 2.6, 2.2, bellGold());
				}
			}
			// Leaves along the bough itself.
			for (int q = 0; q < 5; q++) {
				double s = s0 + (s1 - s0) * (0.1 + 0.18 * q);
				double[] at = boughAt(s);
				SpellArt.shared(m, b, "bodhi_leaf", false, SpellArt.add(at, SpellArt.v3(0, boughThick(s) * 0.4, 0)), 9, 7, 30 + 15 * (q % 3), 70 * q + k * 40, 0, bodhiLeaf(501), 3, q % 2 == 0);
			}
			up = b.name;
		}
		// A ribbon of jade silk tied near its foot, trailing.
		SpellArt.Bone ribbon = m.bone("ribbon", "bough_0", 0, 3, -12);
		for (int i = 0; i < 4; i++) {
			SpellArt.shared(m, ribbon, "ribbon_" + i, false, SpellArt.v3(0.5 * i, 3 - 6.5 * i, -12 + 2.5 * i), 7, 2.6, 12 + 8 * i, 90, 10 * i, ribbon(521 + i), 3, false);
		}
		branchAppear(m);
		branchHold(m);
		branchSweep(m);
	}

	/** Its forming (played once): it grows out from its foot to its tip, twisting as it comes, its treasures last. */
	static void branchAppear(SpellArt.Model m) {
		double L = 1.1;
		SpellArt.Anim a = m.anim("appear", L, null);
		a.sample("root", "scale", 0, 0.8, 8, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 0.8));
			return new double[] {s, s, s};
		});
		a.sample("root", "rotation", 0, 0.8, 8, t -> new double[] {0, 0, -30 * (1 - SpellArt.easeOut(t / 0.8))});
		for (int i = 0; i < 7; i++) {
			a.scale("treasure_" + i, 0, 0.02);
			a.scale("treasure_" + i, 0.6 + 0.05 * i, 0.02);
			a.scale("treasure_" + i, 0.8 + 0.04 * i, 1.25);
			a.scale("treasure_" + i, L, 1);
		}
	}

	/** Held, looped without a seam: its leaves stirring, the bough breathing a little, its treasures swinging on their threads, the ribbon streaming. */
	static void branchHold(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("hold", T, "true");
		for (int k = 0; k < 4; k++) {
			int kk = k;
			a.sample("bough_" + k, "rotation", 0, T, 8, t -> new double[] {1.2 * Math.sin(2 * Math.PI * t / T - kk * 0.6), 0.8 * Math.sin(2 * Math.PI * t / T - kk * 0.5 + 1), 0});
		}
		for (int i = 0; i < 7; i++) {
			int ii = i;
			a.sample("treasure_" + i, "rotation", 0, T, 8, t -> new double[] {9 * Math.sin(2 * Math.PI * t / T + ii), 0, 7 * Math.cos(2 * Math.PI * t / T + ii * 1.3)});
		}
		a.sample("ribbon", "rotation", 0, T, 8, t -> new double[] {12 * Math.sin(2 * Math.PI * t / T * 2), 8 * Math.sin(2 * Math.PI * t / T), 0});
	}

	/**
	 * Its sweep (played once, its spell turning it across its caster's front): drawn back at first, its lengths lagging behind the stroke one after
	 * another, then whipping through, its treasures and ribbon flung out, and coming to rest.
	 */
	static void branchSweep(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("sweep", L, null);
		for (int k = 0; k < 4; k++) {
			double lag = 0.05 * k;
			a.rot("bough_" + k, 0, 0, 0, 0);
			a.rot("bough_" + k, 0.15 + lag, 0, 10 + 3 * k, 0);
			a.rot("bough_" + k, 0.55 + lag, 0, -8 - 2 * k, 0);
			a.rot("bough_" + k, 0.85, 0, 2, 0);
		}
		for (int i = 0; i < 7; i++) {
			a.rot("treasure_" + i, 0, 0, 0, 0);
			a.rot("treasure_" + i, 0.3, 0, 0, 40);
			a.rot("treasure_" + i, 0.6, 0, 0, -30);
		}
		a.rot("ribbon", 0, 0, 0, 0);
		a.rot("ribbon", 0.4, 30, 40, 0);
	}

	// --- Wind-Fire Wheels ---

	static final int[] WHEEL_FIRE = {0x6A1206, 0xC2300C, Fire.VERMILION, Fire.FLAME, Fire.AMBER, 0xFFE9B0};
	static final int WIND = 0xCFF2FF, WIND_DEEP = 0x7EC8E6;

	/** The wheel's rim and spokes (fire made solid, Void#blazeLimbOf in the wheel's own colours). */
	static SpellArt.Painter wheelFire(int seed) {
		return Void.blazeLimbOf(seed, WHEEL_FIRE);
	}

	/** A streamer of whirling wind (on a blade): pale blue, streaked along it, thinning to nothing at its end; seen through. */
	static SpellArt.Painter windStreamer(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double half = 0.45 * (1 - v) * (0.8 + 0.2 * Math.sin(v * 11 + seed));
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double streak = Math.abs(Math.sin((u * 7 + v * 3) * Math.PI + seed));
			return SpellArt.glow(SpellArt.mix(WIND_DEEP, WIND, 0.5 + 0.5 * streak), (0.18 + 0.3 * (1 - v)) * (1 - Math.abs(u) / Math.max(1e-3, half) * 0.6));
		};
	}

	/** Its hub (on a cube): a coal of gold fire, the heart of it white; a ring of script round its faces. */
	static SpellArt.Painter wheelHub(int seed) {
		return t -> {
			double r = Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2;
			if (r > 0.75 && r < 0.88) return SpellArt.glow(0xFFF4D0, 0.98);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(1 - r * 0.8), WHEEL_FIRE), 0.96);
		};
	}

	/**
	 * A Wind-Fire Wheel, standing upright, rolling along -z, its hub at the origin and sixteen pixels round: its rim of fire made solid, doubled
	 * ("wheel", that rolls about x), eight spokes, a hub of white-gold fire, flames streaming back off its rim and streamers of whirling wind
	 * round it ("wind", turning the other way), flames trailing behind it ("trail").
	 */
	static void windFireWheel(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone wheel = m.bone("wheel", "root", 0, 0, 0);
		wheelRing(wheel, 0, 0, 0, 15, 2.6, 5, 20, wheelFire(601));
		wheelRing(wheel, 0, 0, 0, 11.5, 1.6, 3, 16, wheelFire(603));
		for (int k = 0; k < 8; k++) {
			double th = Math.toRadians(45 * k);
			SpellArt.limb(wheel, SpellArt.v3(0, Math.cos(th) * 2.5, Math.sin(th) * 2.5), SpellArt.v3(0, Math.cos(th) * 14, Math.sin(th) * 14), 1.6, 1.6, wheelFire(605 + k));
		}
		wheel.box(0, 0, 0, 4.2, 5.4, 5.4, wheelHub(615));
		wheel.box(0, 0, 0, 4.2, 5.4, 5.4, wheelHub(616)).rot(45, 0, 0);
		// Flames off its rim: tongues standing out of it all round.
		for (int k = 0; k < 16; k++) {
			double th = 360.0 * k / 16;
			double[] base = SpellArt.v3(0, Math.cos(Math.toRadians(th)) * 16.5, Math.sin(Math.toRadians(th)) * 16.5);
			SpellArt.shared(m, wheel, "rim_flame_" + k % 2, false, base, 7 + (k % 3), 5, -th, 0, 0, Void.featherOf(3, 621 + k % 2, WHEEL_FIRE), 3, k % 4 > 1);
		}
		// Whirling wind round it.
		SpellArt.Bone wind = m.bone("wind", "root", 0, 0, 0);
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k;
			double[] base = SpellArt.v3((k % 2 == 0 ? 1 : -1) * 3.2, Math.cos(Math.toRadians(th)) * 18, Math.sin(Math.toRadians(th)) * 18);
			SpellArt.shared(m, wind, "wind_streamer", false, base, 14, 5, -th - 70, 0, 0, windStreamer(641), 2, k % 2 == 0);
		}
		// Flames trailing behind it as it rolls.
		SpellArt.Bone trail = m.bone("trail", "root", 0, 0, 0);
		for (int k = 0; k < 5; k++) {
			double[] base = SpellArt.v3((k - 2) * 1.2, -6 + k * 3, 6);
			SpellArt.shared(m, trail, "trail_flame", false, base, 14 - Math.abs(k - 2) * 2, 6, -60 - k * 6, 0, 0, Void.featherOf(4, 651, WHEEL_FIRE), 2, k % 2 == 1);
		}
		wheelIgnite(m);
		wheelRoll(m);
		wheelBurst(m);
	}

	/** It flares up (played once): from a spark to its whole size, the wind gathering round it. */
	static void wheelIgnite(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("ignite", L, null);
		a.sample("root", "scale", 0, L, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / L) * (1 + 0.15 * Math.sin(Math.PI * t / L)));
			return new double[] {s, s, s};
		});
	}

	/** Its rolling, looped without a seam: the wheel turning (forward over its top), its wind whirling the other way, its trail licking. */
	static void wheelRoll(SpellArt.Model m) {
		double T = 0.5;
		SpellArt.Anim a = m.anim("roll", T, "true");
		a.sample("wheel", "rotation", 0, T, 8, t -> new double[] {360 * t / T, 0, 0});
		a.sample("wind", "rotation", 0, T, 8, t -> new double[] {-180 * t / T, 0, 0});
		a.sample("trail", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.18 * Math.sin(2 * Math.PI * t / T * 2), 1 + 0.12 * Math.cos(2 * Math.PI * t / T * 2)});
	}

	/** It bursts (played once, held): it flares out and is gone. */
	static void wheelBurst(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("burst", 0.4, "hold_on_last_frame");
		a.scale("root", 0, 1);
		a.scale("root", 0.15, 1.6);
		a.scale("root", 0.4, 0.02);
	}

	// --- Eight Trigrams Furnace ---

	static final int FURNACE_DARK = 0x2A1A0C, FURNACE = 0x5E3E1C, FURNACE_LIGHT = 0x9A6A30, FURNACE_GILT = 0xE2B455, PATINA = 0x3E7A62;
	static final int[] SAMADHI = {0x6A1A06, 0xC24A10, 0xF09028, 0xFFC860, 0xFFE8A0, 0xFFFCEE};
	/** The eight trigrams in the Later Heaven order round the furnace (Li, Kun, Dui, Qian, Kan, Gen, Zhen, Xun): their lines, foot to head. */
	static final boolean[][] TRIGRAMS = {{true, false, true}, {false, false, false}, {true, true, false}, {true, true, true}, {false, true, false}, {false, false, true},
			{true, false, false}, {false, true, true}};

	/** The furnace's bronze (on a solid): dark, cast in cloud scrolls, gilt on its raised edges, green with age in its hollows; lit by the fire within. */
	static SpellArt.Painter furnaceBronze(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.18, t.py * 0.18, t.pz * 0.18, seed, 3);
			double scroll = Math.sin(t.px * 0.55 + 2.4 * Math.sin(t.py * 0.45 + seed)) * Math.cos(t.py * 0.55 + t.pz * 0.45 + 1.6 * Math.sin(t.px * 0.3));
			if (Math.abs(scroll) > 0.92) return SpellArt.col(SpellArt.mix(FURNACE_GILT, Earth.GOLD_PALE, 0.3));
			if (n < 0.3) return SpellArt.col(SpellArt.mix(FURNACE_DARK, PATINA, 0.55));
			double light = SpellArt.clamp01(0.4 + (n - 0.5) * 0.7 + 0.2 * Math.max(0, t.ny) - 0.25 * Math.max(0, -t.ny));
			return SpellArt.col(SpellArt.ramp(light, FURNACE_DARK, FURNACE, FURNACE_LIGHT));
		};
	}

	/** One of its eight panels (on a box's outer face, -z): a trigram burning in Samadhi fire in a gilt frame; bronze elsewhere. */
	static SpellArt.Painter trigramPanel(boolean[] lines, int seed) {
		SpellArt.Painter bronze = furnaceBronze(seed);
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return bronze.paint(t);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			if (Math.abs(x) > 0.84 || Math.abs(y) > 0.84) return SpellArt.col(FURNACE_GILT);
			if (Math.abs(x) > 0.74 || Math.abs(y) > 0.74) return SpellArt.col(FURNACE_DARK);
			for (int i = 0; i < 3; i++) {
				double ly = -0.42 + i * 0.42;
				if (Math.abs(y - ly) > 0.12 || Math.abs(x) > 0.55) continue;
				if (!lines[i] && Math.abs(x) < 0.11) continue;
				return SpellArt.glow(SpellArt.ramp(0.75 + 0.2 * Math.abs(x), SAMADHI), 0.98);
			}
			return SpellArt.col(SpellArt.mix(FURNACE_DARK, FURNACE, 0.4));
		};
	}

	/** A window of the furnace (on a sheet): the Samadhi fire within, seen through a lattice; glowing. */
	static SpellArt.Painter furnaceWindow(int seed) {
		return t -> {
			double x = t.across(), y = t.back();
			boolean lattice = Math.abs(Math.sin(x * Math.PI * 4)) < 0.15 || Math.abs(Math.sin(y * Math.PI * 4)) < 0.15;
			if (lattice) return SpellArt.col(FURNACE_DARK);
			double flick = SpellArt.fbm(x * 4 + seed, y * 6, seed, 3);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.4 + flick * 0.7), SAMADHI), 0.98);
		};
	}

	/**
	 * The Eight Trigrams Furnace, its feet on the land at the origin, its belly sixteen pixels round (its spell draws foes into it): three legs cast
	 * as clouds ("legs"), its eight-sided belly of bronze, a trigram burning on each of its eight sides between windows of the Samadhi fire within
	 * ("belly"), its rim and two great handles, its domed lid with a gourd-shaped knob ("lid"), flames licking out round the lid and out of its
	 * windows ("flames"), and a jet waiting behind each window ("jet_0".."jet_7", thrown out when it jets).
	 */
	static void trigramFurnace(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone legs = m.bone("legs", "root", 0, 0, 0);
		SpellArt.Painter bronze = furnaceBronze(701);
		for (int k = 0; k < 3; k++) {
			double th = Math.toRadians(120 * k + 60);
			double[] foot = SpellArt.v3(Math.sin(th) * 13, 0.5, Math.cos(th) * 13), knee = SpellArt.v3(Math.sin(th) * 14.5, 5, Math.cos(th) * 14.5),
					hip = SpellArt.v3(Math.sin(th) * 11, 10, Math.cos(th) * 11);
			SpellArt.limb(legs, knee, hip, 4.5, 4.5, bronze);
			SpellArt.limb(legs, foot, knee, 5, 5, bronze);
			legs.box(foot[0], 1, foot[2], 7, 2, 7, bronze).rot(0, Math.toDegrees(th), 0);
		}
		SpellArt.Bone belly = m.bone("belly", "root", 0, 10, 0);
		// The eight sides: each a panel with its trigram, the corners between them bronze.
		double apothem = 15.5, side = 2 * apothem * Math.tan(Math.PI / 8);
		for (int k = 0; k < 8; k++) {
			double th = 22.5 + 45 * k;
			double x = Math.sin(Math.toRadians(th)) * apothem, z = Math.cos(Math.toRadians(th)) * apothem;
			// Its outer face (-z, the panel's front) turned outward: heading th + 180.
			belly.box(x, 21, z, side + 0.4, 18, 2, trigramPanel(TRIGRAMS[k], 711 + k)).rot(0, th + 180, 0).density(2);
			// A window over each panel, in its frieze.
			SpellArt.Cube w = SpellArt.sheet(belly, Math.sin(Math.toRadians(th)) * (apothem + 1.1), 33.5, Math.cos(Math.toRadians(th)) * (apothem + 1.1), side * 0.7, 3.6, 90, th, 0, furnaceWindow(721 + k));
			w.density(3);
		}
		lathe(belly, 10, 12, 2, y -> apothem - 1.5, bronze);
		lathe(belly, 30, 36, 2, y -> apothem + 0.6, bronze);
		round(belly, 0, 36.6, 0, apothem + 2.2, 1.6, furnaceBronze(731), true);
		// Its two great handles, upright on its rim.
		for (int s = -1; s <= 1; s += 2) {
			belly.box(s * 13, 41, 0, 2.4, 9, 3, bronze);
			belly.box(s * 13, 45.5, -4, 2.4, 2.4, 9, bronze);
			belly.box(s * 13, 41, -8, 2.4, 9, 3, bronze);
		}
		// Its lid: a dome, a ring of vents round it, a knob shaped as a gourd.
		SpellArt.Bone lid = m.bone("lid", "belly", 0, 37, 0);
		lathe(lid, 37, 46, 1.5, y -> 15 * Math.sqrt(Math.max(0, 1 - Math.pow((y - 37) / 10, 2))), furnaceBronze(741));
		lathe(lid, 46, 55, 1.5, y -> 3.2 + 2.4 * Math.sin(Math.PI * (y - 46) / 9) * (y < 50 ? 1 : 0.7), SpellArt.solid(SpellArt.col(FURNACE_GILT)));
		// Flames licking out of the gap under the lid and out of its windows.
		SpellArt.Bone flames = m.bone("flames", "belly", 0, 37, 0);
		for (int k = 0; k < 12; k++) {
			double th = 30.0 * k + 15;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 15, 37, Math.cos(Math.toRadians(th)) * 15);
			SpellArt.shared(m, flames, "lid_flame_" + k % 3, false, base, 8 + (k % 3) * 2, 5, -20, th, 0, Void.featherOf(3, 751 + k % 3, SAMADHI), 2, k % 2 == 0);
		}
		// A jet behind each window: a long tongue of Samadhi fire, small till it jets.
		for (int k = 0; k < 8; k++) {
			double th = 22.5 + 45 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * (apothem + 1), 33.5, Math.cos(Math.toRadians(th)) * (apothem + 1));
			SpellArt.Bone jet = m.bone("jet_" + k, "belly", base[0], base[1], base[2]);
			// Laid out level from its window, outward (a blade swept 90 off upright, headed out).
			SpellArt.shared(m, jet, "jet_flame", false, base, 30, 7, -90, th, 0, Void.featherOf(4, 761, SAMADHI), 2, false);
		}
		furnaceBurn(m);
		furnaceJet(m);
		furnaceBurst(m);
	}

	/** Its burning, looped without a seam: the lid rattling, the flames under it licking, the jets waiting unseen. */
	static void furnaceBurn(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("burn", T, "true");
		a.sample("lid", "position", 0, T, 16, t -> new double[] {0, 0.6 * Math.max(0, Math.sin(2 * Math.PI * t / T * 4)), 0});
		a.sample("lid", "rotation", 0, T, 16, t -> new double[] {1.5 * Math.sin(2 * Math.PI * t / T * 4), 0, 1.5 * Math.cos(2 * Math.PI * t / T * 4)});
		a.sample("flames", "scale", 0, T, 16, t -> new double[] {1, 1 + 0.2 * Math.sin(2 * Math.PI * t / T * 4), 1});
		for (int k = 0; k < 8; k++) {
			a.scale("jet_" + k, 0, 0.02);
			a.scale("jet_" + k, T, 0.02);
		}
	}

	/** It jets (played once): fire thrown out of all eight windows, out and back. */
	static void furnaceJet(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("jet", 0.7, null);
		for (int k = 0; k < 8; k++) {
			a.scale("jet_" + k, 0, 0.02);
			a.scale("jet_" + k, 0.12, 1.3, 1.6, 1.3);
			a.scale("jet_" + k, 0.4, 1.0, 1.2, 1.0);
			a.scale("jet_" + k, 0.7, 0.02);
		}
		a.pos("lid", 0, 0, 0, 0);
		a.pos("lid", 0.1, 0, 2.5, 0);
		a.pos("lid", 0.35, 0, 0, 0);
	}

	/** It bursts (played once, held): the lid blown off, spinning away, and the whole of it flaring out. */
	static void furnaceBurst(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("burst", 0.7, "hold_on_last_frame");
		a.pos("lid", 0, 0, 0, 0);
		a.pos("lid", 0.5, 0, 46, 0);
		a.rot("lid", 0, 0, 0, 0);
		a.rot("lid", 0.5, 70, 200, 30);
		a.scale("flames", 0, 1);
		a.scale("flames", 0.2, 3.2, 4, 3.2);
		a.scale("flames", 0.7, 2.6, 3, 2.6);
		for (int k = 0; k < 8; k++) {
			a.scale("jet_" + k, 0, 0.02);
			a.scale("jet_" + k, 0.15, 1.6, 2, 1.6);
			a.scale("jet_" + k, 0.7, 1.3, 1.5, 1.3);
		}
	}

	// --- Xirang, the Self-Growing Soil ---

	static final int SOIL_DARK = 0x1C140C, SOIL = 0x3E2C1A, SOIL_LIGHT = 0x6A4E2E, SOIL_GOLD = 0xFFC452;

	/** Xirang (on a solid): dark rich soil in clods, crumbs of it lighter, veins of gold root-light running through it; glowing in its veins. */
	static SpellArt.Painter xirangSoil(int seed) {
		return t -> {
			double clod = SpellArt.fbm3(t.px * 0.45, t.py * 0.45, t.pz * 0.45, seed, 3);
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.16, t.py * 0.2, t.pz * 0.16, seed + 3, 3) - 1), 12);
			if (vein > 0.78) return SpellArt.glow(SpellArt.mix(SOIL_GOLD, 0xFFF4C8, (vein - 0.78) * 4), 0.96);
			double light = SpellArt.clamp01(0.3 + (clod - 0.5) * 0.9 + 0.2 * Math.max(0, t.ny) - 0.2 * Math.max(0, -t.ny));
			return SpellArt.col(SpellArt.ramp(light, SOIL_DARK, SOIL, SOIL_LIGHT));
		};
	}

	/** A root of gold light (on a limb). */
	static SpellArt.Painter rootLight() {
		return t -> SpellArt.glow(SpellArt.mix(SOIL_GOLD, 0xFFF4C8, 0.3 + 0.3 * Math.sin(t.ly * 2)), 0.92);
	}

	/** A shoot (on a blade): a young leaf, bright green, its vein pale. */
	static SpellArt.Painter shoot(int seed) {
		return t -> {
			double v = t.v(), u = (t.u() - 0.5) * 2, half = 0.9 * Math.sin(Math.PI * Math.min(1, v * 1.1)) * (1 - 0.3 * v);
			if (Math.abs(u) > half || v < 0.05) return SpellArt.CLEAR;
			if (Math.abs(u) < 0.08) return SpellArt.col(Wood.LEAF_LIGHT);
			return SpellArt.col(SpellArt.mix(Wood.LEAF, Wood.LEAF_LIGHT, 0.3 + 0.4 * SpellArt.noise(u * 3 + seed, v * 4, seed)));
		};
	}

	/**
	 * A clod of Xirang, the soil that grows of itself, its heart at the origin, eleven pixels across: lumps of dark living earth heaped together
	 * ("clod"), veins of gold root-light through it, young shoots sprouting from it ("shoots"), and roots of gold light hanging from it that reach
	 * out and down as it swells ("roots").
	 */
	static void xirang(SpellArt.Model m) {
		m.tpp = 2.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone clod = m.bone("clod", "root", 0, 0, 0);
		SpellArt.Painter soil = xirangSoil(801);
		double[][] lumps = {{0, 0, 0, 9, 7, 8, 0, 20, 0}, {-3, 2.5, 1, 6, 5, 6, 15, 40, 10}, {3, 2, -2, 5.5, 5, 5, -10, 10, 20}, {1, -2.5, 2, 6, 4, 5, 20, -30, 0},
				{-2, -1.5, -3, 5, 5, 5, -15, 60, -10}, {2.5, 3.5, 2.5, 4, 3.5, 4, 30, 15, 25}, {-1, 4, -1, 4.5, 3, 4, 10, -20, 30}};
		for (double[] l : lumps) clod.box(l[0], l[1], l[2], l[3], l[4], l[5], soil).rot(l[6], l[7], l[8]);
		SpellArt.Bone shoots = m.bone("shoots", "clod", 0, 4, 0);
		for (int k = 0; k < 5; k++) {
			double th = 72 * k + 10;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 2.2, 4.5 + (k % 2), Math.cos(Math.toRadians(th)) * 2.2);
			SpellArt.shared(m, shoots, "shoot", false, base, 5, 3, 25 + (k % 2) * 15, th, 0, shoot(811), 4, k % 2 == 0);
		}
		SpellArt.Bone roots = m.bone("roots", "clod", 0, -2, 0);
		for (int k = 0; k < 7; k++) {
			double th = Math.toRadians(360.0 * k / 7 + 20);
			double[] a = SpellArt.v3(Math.sin(th) * 2.5, -2.5, Math.cos(th) * 2.5), b = SpellArt.v3(Math.sin(th) * 6, -7 - (k % 3), Math.cos(th) * 6),
					c = SpellArt.v3(Math.sin(th) * 8.5, -12 - (k % 2) * 2, Math.cos(th) * 8.5);
			SpellArt.limb(roots, a, b, 0.8, 0.8, rootLight());
			SpellArt.limb(roots, b, c, 0.5, 0.5, rootLight());
		}
		xirangPulse(m);
		xirangSwell(m);
		xirangSettle(m);
	}

	/** Its life, looped without a seam: it breathes, its shoots stir, its roots grope. */
	static void xirangPulse(SpellArt.Model m) {
		double T = 1.2;
		SpellArt.Anim a = m.anim("pulse", T, "true");
		a.sample("clod", "scale", 0, T, 8, t -> {
			double s = 1 + 0.07 * Math.sin(2 * Math.PI * t / T);
			return new double[] {s, s, s};
		});
		a.sample("shoots", "rotation", 0, T, 8, t -> new double[] {6 * Math.sin(2 * Math.PI * t / T), 0, 6 * Math.cos(2 * Math.PI * t / T)});
		a.sample("roots", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.15 * Math.sin(2 * Math.PI * t / T + 1), 1});
	}

	/** It swells (played once, held): it heaves up great as the mound grows under it, its roots reaching out and down into the land, its shoots grown. */
	static void xirangSwell(SpellArt.Model m) {
		double L = 1.5;
		SpellArt.Anim a = m.anim("swell", L, "hold_on_last_frame");
		a.sample("clod", "scale", 0, L, 10, t -> {
			double s = 1 + 2.4 * SpellArt.easeOut(t / L) + 0.15 * Math.sin(t / L * Math.PI * 5) * (1 - t / L);
			return new double[] {s, s * 0.8, s};
		});
		// Its roots grow down into the mound with it (they are the clod's: its own swelling widens them already, so they reach only deeper).
		a.sample("roots", "scale", 0, L, 6, t -> {
			double s = 1 + 0.6 * SpellArt.easeOut(t / L);
			return new double[] {0.9, s, 0.9};
		});
		a.sample("shoots", "scale", 0, L, 6, t -> {
			double s = 1 + 1.4 * SpellArt.easeOut(t / L);
			return new double[] {s, s, s};
		});
	}

	/** It settles (played once, held): sinking down into the mound it raised, its heave flattening. */
	static void xirangSettle(SpellArt.Model m) {
		double L = 0.8;
		SpellArt.Anim a = m.anim("settle", L, "hold_on_last_frame");
		a.scale("clod", 0, 3.4, 2.72, 3.4);
		a.scale("clod", 0.3, 3.8, 2.0, 3.8);
		a.scale("clod", L, 3.2, 1.6, 3.2);
		a.pos("clod", 0, 0, 0, 0);
		a.pos("clod", L, 0, -6, 0);
		a.scale("roots", 0, 0.9, 1.6, 0.9);
		a.scale("roots", L, 0.9, 1.6, 0.9);
		a.scale("shoots", 0, 2.4);
		a.scale("shoots", L, 2.4);
	}

	// --- Nine Cauldrons Suppress the Land ---

	/** The nine provinces' names, one on each cauldron (written in Fire#glyph's script; which by the cauldron's place is left to the glyphs). */
	static final int CAULDRON_DARK = 0x1E2418, CAULDRON = 0x4A5236, CAULDRON_LIGHT = 0x7E8656, CAULDRON_GILT = 0xC8A04E, CAULDRON_GLOW = 0xFFB838;

	/**
	 * The cauldron's ancient bronze (on a solid): dark green-grey, cast in a ground of thunder-scrolls (a fine square meander), the bold mask of a
	 * taotie in relief on its faces (geometric, its eyes two bosses), gilt on its raised lines, glowing with the earth's gold in the deepest cuts.
	 */
	static SpellArt.Painter cauldronBronze(int seed, boolean mask) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.2, t.py * 0.2, t.pz * 0.2, seed, 3);
			// The thunder-scroll ground: a square meander, its lines cut fine.
			double gx = t.fx * 12, gy = t.fy * 10;
			double mx = gx - Math.floor(gx), my = gy - Math.floor(gy);
			boolean meander = (Math.floor(gx) + Math.floor(gy)) % 2 == 0 ? Math.abs(mx - 0.5) < 0.12 || my < 0.12 : Math.abs(my - 0.5) < 0.12 || mx < 0.12;
			if (mask && !t.flat() && t.px * t.nx + t.pz * t.nz > 0) {
				double x = (t.fx - 0.5) * 2, y = (t.fy - 0.5) * 2;
				// The mask's two eyes (bosses), its brow and its jaws: raised and gilt.
				double eye = Math.min(Math.hypot(x - 0.38, y + 0.05), Math.hypot(x + 0.38, y + 0.05));
				if (eye < 0.12) return SpellArt.glow(CAULDRON_GLOW, 0.96);
				if (eye < 0.2 || Math.abs(y + 0.42) < 0.06 && Math.abs(x) < 0.7 || Math.abs(y - 0.4) < 0.06 && Math.abs(x) < 0.55
						|| Math.abs(x) < 0.05 && y > -0.42 && y < 0.4) {
					return SpellArt.col(SpellArt.mix(CAULDRON_GILT, Earth.GOLD_PALE, 0.25));
				}
			}
			if (meander) return SpellArt.col(SpellArt.mix(CAULDRON_DARK, CAULDRON, 0.3));
			double light = SpellArt.clamp01(0.4 + (n - 0.5) * 0.7 + 0.2 * Math.max(0, t.ny) - 0.25 * Math.max(0, -t.ny));
			return SpellArt.col(SpellArt.ramp(light, CAULDRON_DARK, CAULDRON, CAULDRON_LIGHT));
		};
	}

	/** The province's glyph on its belly (on its front face, -z): one great glyph of the earth's gold in a gilt cartouche. */
	static SpellArt.Painter provinceGlyph(int seed) {
		SpellArt.Painter bronze = cauldronBronze(seed, false);
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return bronze.paint(t);
			double x = t.across(), y = t.up();
			if (x < 0.06 || x > 0.94 || y < 0.06 || y > 0.94) return SpellArt.col(CAULDRON_GILT);
			if (Fire.glyph(seed, seed, (x - 0.1) / 0.8, (y - 0.1) / 0.8)) return SpellArt.glow(SpellArt.mix(CAULDRON_GLOW, 0xFFF0C0, 0.3), 0.98);
			return SpellArt.col(SpellArt.mix(CAULDRON_DARK, CAULDRON, 0.5));
		};
	}

	/** Smoke of incense rising out of it (on a blade): pale grey, curling, thinning to nothing; seen through. */
	static SpellArt.Painter incense(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5 - 0.18 * Math.sin(v * 7 + seed) * v;
			double half = 0.18 + 0.25 * v;
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(0xA8A498, 0xF2F0E6, v), 0.4 * (1 - v) * (1 - Math.abs(u) / half));
		};
	}

	/**
	 * One of the nine bronze cauldrons of Yu, its feet on the land at the origin, some twenty-two pixels square: four legs, their heads cast as
	 * clouds ("legs"), its square body cast with thunder-scrolls and a taotie mask on each side, its province's glyph burning on its front
	 * ("body"), its rim and two upright handles; incense smoke curling up out of it ("smoke").
	 */
	static void nineCauldron(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 0, 0);
		SpellArt.Painter bronze = cauldronBronze(851, false), masked = cauldronBronze(853, true);
		for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
			body.box(sx * 8.5, 5, sz * 8.5, 3.6, 10, 3.6, bronze);
			body.box(sx * 8.5, 9.5, sz * 8.5, 5, 2.4, 5, bronze).rot(0, 45, 0);
		}
		// The body: its four sides (the front with its glyph, the rest with their masks), its floor and its lip.
		body.box(0, 18, -10.5, 22, 15, 1.6, provinceGlyph(855)).density(2.5);
		body.box(0, 18, 10.5, 22, 15, 1.6, masked).density(2);
		body.box(-10.5, 18, 0, 1.6, 15, 19.4, masked).density(2);
		body.box(10.5, 18, 0, 1.6, 15, 19.4, masked).density(2);
		body.box(0, 11, 0, 21, 1.6, 21, bronze);
		ring(body, 0, 25.8, 0, 11.6, 1.6, 4, bronze);
		for (int s = -1; s <= 1; s += 2) {
			body.box(s * 7.5, 30, -11, 2, 8, 1.6, bronze);
			body.box(s * 4.5, 30, -11, 2, 8, 1.6, bronze);
			body.box(s * 6, 33.6, -11, 5, 1.6, 1.6, bronze);
		}
		for (int s = -1; s <= 1; s += 2) {
			body.box(s * 7.5, 30, 11, 2, 8, 1.6, bronze);
			body.box(s * 4.5, 30, 11, 2, 8, 1.6, bronze);
			body.box(s * 6, 33.6, 11, 5, 1.6, 1.6, bronze);
		}
		// Glow of the earth's gold within it.
		SpellArt.sheet(body, 0, 24, 0, 18, 18, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			return r > 1 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(CAULDRON_GLOW, 0xFFF0C0, 1 - r), 0.6 * (1 - r) + 0.2);
		}).density(2);
		SpellArt.Bone smoke = m.bone("smoke", "body", 0, 25, 0);
		for (int k = 0; k < 4; k++) {
			double th = 90 * k + 30;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 4, 25, Math.cos(Math.toRadians(th)) * 4);
			SpellArt.shared(m, smoke, "incense", false, base, 22, 6, 10, th, 0, incense(861), 1.5, k % 2 == 0);
		}
		cauldronStand(m);
		cauldronLand(m);
		cauldronRise(m);
	}

	/** Standing, looped without a seam: its smoke curling up, the gold in it breathing. */
	static void cauldronStand(SpellArt.Model m) {
		double T = 3;
		SpellArt.Anim a = m.anim("stand", T, "true");
		a.sample("smoke", "rotation", 0, T, 8, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T), 360 * t / T, 4 * Math.cos(2 * Math.PI * t / T)});
		a.sample("smoke", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 2), 1});
	}

	/** It lands (played once): it squats under its own weight and settles. */
	static void cauldronLand(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("land", 0.5, null);
		a.scale("body", 0, 1);
		a.scale("body", 0.08, 1.08, 0.82, 1.08);
		a.scale("body", 0.2, 0.97, 1.05, 0.97);
		a.scale("body", 0.35, 1.01, 0.99, 1.01);
		a.scale("body", 0.5, 1);
	}

	/** It rises (played once, held): lifted off the land, its smoke drawn in, shrinking a little as it goes. */
	static void cauldronRise(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("rise", 0.8, "hold_on_last_frame");
		a.scale("body", 0, 1);
		a.scale("body", 0.8, 0.85);
		a.scale("smoke", 0, 1);
		a.scale("smoke", 0.5, 0.02);
	}

	// --- Twenty-Four Sea-Fixing Pearls ---

	static final int PEARL = 0xF4F2EA, PEARL_SHADE = 0xB8C6D0, PEARL_SEA = 0x5CC8E6;

	/** A pearl of the sea (on a solid): nacre white, a rainbow sheen over it, the sea's blue in its shadow; lit by its own light. */
	static SpellArt.Painter seaPearl(int seed) {
		return t -> {
			double sheen = 0.5 + 0.5 * Math.sin(t.px * 2.2 + t.py * 1.7 + t.pz * 2.6 + seed);
			int c = SpellArt.mix(PEARL_SHADE, PEARL, SpellArt.clamp01(0.5 + 0.4 * t.ny + 0.2 * t.nx));
			c = SpellArt.mix(c, SpellArt.mix(0xFFD6F0, 0xC8F4FF, sheen), 0.22);
			if (t.ny < -0.4) c = SpellArt.mix(c, PEARL_SEA, 0.3);
			return SpellArt.glow(c, 0.97);
		};
	}

	/** The sea turning in it (on a sheet through its heart): a whirl of blue water, its foam white. */
	static SpellArt.Painter seaWhirl(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.98) return SpellArt.CLEAR;
			double arm = Math.sin(Math.atan2(z, x) * 2 + r * 8 + seed);
			return SpellArt.glow(arm > 0.6 ? Water.FOAM : SpellArt.mix(Water.DEEP, PEARL_SEA, 0.5 + 0.5 * arm), 0.5 * (1 - r) + 0.3);
		};
	}

	/**
	 * A sea-fixing pearl, its heart at the origin, four pixels round: the pearl ("pearl"), the sea whirling in a ring round it ("halo"), and
	 * drops of the sea circling it ("drops").
	 */
	static void seaPearl(SpellArt.Model m) {
		m.tpp = 4;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone pearl = m.bone("pearl", "root", 0, 0, 0);
		lathe(pearl, -3.6, 3.6, 0.8, y -> Math.sqrt(Math.max(0, 3.6 * 3.6 - y * y)), seaPearl(901));
		SpellArt.Bone halo = m.bone("halo", "root", 0, 0, 0);
		SpellArt.sheet(halo, 0, 0, 0, 12, 12, 0, 0, 0, t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r < 0.62 || r > 0.98) return SpellArt.CLEAR;
			double arm = Math.sin(Math.atan2(z, x) * 3 + r * 10);
			return SpellArt.glow(arm > 0.4 ? Water.FOAM : PEARL_SEA, 0.55 * (1 - Math.abs(r - 0.8) / 0.18) + 0.1);
		}).density(4);
		SpellArt.sheet(halo, 0, 0, 0, 9, 9, 90, 0, 0, seaWhirl(903)).density(4);
		SpellArt.Bone drops = m.bone("drops", "root", 0, 0, 0);
		for (int k = 0; k < 5; k++) {
			double th = Math.toRadians(72 * k);
			drops.box(Math.cos(th) * 5.5, Math.sin(k * 1.3) * 1.5, Math.sin(th) * 5.5, 1, 1, 1, t -> SpellArt.glow(SpellArt.mix(PEARL_SEA, Water.FOAM, 0.5), 0.9)).rot(45, 45, 0);
		}
		SpellArt.Anim a = m.anim("spin", 1.0, "true");
		a.sample("halo", "rotation", 0, 1.0, 8, t -> new double[] {0, 360 * t, 0});
		a.sample("drops", "rotation", 0, 1.0, 8, t -> new double[] {0, -360 * t, 20 * Math.sin(2 * Math.PI * t)});
		a.sample("pearl", "rotation", 0, 1.0, 8, t -> new double[] {0, 360 * t, 0});
	}

	// --- Ocean-Sealing Vase ---

	static final int VASE_WHITE = 0xF4F6F2, VASE_SHADE = 0xC4CCC8, VASE_BLUE = 0x2A58A8, VASE_BLUE_DEEP = 0x18306A;

	/**
	 * The vase's white jade (round a body standing up its y), painted round with the sea in blue: waves breaking in bands (crests curling, foam
	 * dots), a band of lotus petals at its shoulder; the glaze's gloss lit where it faces up.
	 */
	static SpellArt.Painter vaseJade(int seed) {
		return t -> {
			double ang = Math.atan2(t.qx(), t.qz()), y = t.qy();
			double shade = SpellArt.clamp01(0.55 + 0.3 * Math.max(0, -t.ny) + 0.15 * Math.max(0, t.nx));
			int base = SpellArt.mix(VASE_WHITE, VASE_SHADE, 1 - shade);
			// Bands of waves round its body, each crest curling over.
			double band = (y - 8) / 7.0, bf = band - Math.floor(band);
			if (y > 8 && y < 36) {
				double crest = 0.5 + 0.35 * Math.sin(ang * 9 + Math.floor(band) * 1.7 + seed);
				if (Math.abs(bf - crest) < 0.06) return SpellArt.col(VASE_BLUE_DEEP);
				if (bf < crest && bf > crest - 0.3 && Math.sin(ang * 18 + bf * 9) > 0.3) return SpellArt.col(VASE_BLUE);
				if (Math.abs(bf - crest + 0.12) < 0.03 && Math.sin(ang * 40) > 0.6) return SpellArt.col(VASE_BLUE);
			}
			// A band of lotus petals at its shoulder.
			if (y > 36 && y < 41) {
				double petal = Math.abs(Math.sin(ang * 6)), rise = (y - 36) / 5;
				if (Math.abs(rise - petal * 0.85) < 0.12) return SpellArt.col(VASE_BLUE);
			}
			return SpellArt.col(base);
		};
	}

	/** A stream of water (on a flat limb, its breadth across x): blue-green, streaked along it, white at its edges; seen through. */
	static SpellArt.Painter stream(int seed) {
		return t -> {
			double u = t.across(), v = t.up();
			double streak = Math.abs(Math.sin((u * 6 + v * 4) * Math.PI + seed));
			if (u < 0.1 || u > 0.9) return SpellArt.glow(Water.FOAM, 0.6);
			return SpellArt.glow(SpellArt.mix(Water.TEAL, Water.CYAN, streak), 0.55);
		};
	}

	/** Stream {@code k}'s spiral, {@code i} eighths of the way from the mouth down: out and round as it goes. */
	static double[] streamAt(int k, int i) {
		double th = Math.toRadians(60 * k + 26 * i), r = 2.5 + 3.2 * i, y = -0.5 - 5.6 * i;
		return SpellArt.v3(Math.sin(th) * r, y, Math.cos(th) * r);
	}

	/** The vase's radius {@code y} pixels up from its mouth: its lip, its narrow neck, its broad shoulder, tapering to its foot. */
	static double vaseRadius(double y) {
		if (y < 2) return 4.2;
		if (y < 8) return 3.0;
		if (y < 13) return 3 + 8 * SpellArt.smooth(8, 13, y);
		if (y < 40) return 11 - 4.2 * Math.pow((y - 13) / 27.0, 1.4);
		return y < 42 ? 7.6 : 0;
	}

	/**
	 * The Ocean-Sealing Vase, hung mouth down, its mouth at the origin, some forty pixels tall: its body of white jade painted with the sea
	 * ("vase"), its mouth's inner dark and the stopper that seals it ("stopper", unseen till it seals), and six streams of the sea spiralling up into
	 * its mouth out of the land below ("streams", turning; "stream_0".."stream_5", each in four lengths).
	 */
	static void oceanVase(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone vase = m.bone("vase", "root", 0, 0, 0);
		lathe(vase, 0, 42, 1.5, Vessel::vaseRadius, vaseJade(951));
		SpellArt.sheet(vase, 0, -0.1, 0, 6.5, 6.5, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			return r > 1 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(Water.ABYSS, Water.CYAN, SpellArt.smooth(0.3, 1, r)), 0.98);
		}).density(4);
		SpellArt.Bone stopper = m.bone("stopper", "vase", 0, 0, 0);
		round(stopper, 0, -1.2, 0, 3.6, 3, t -> SpellArt.col(SpellArt.mix(Earth.GOLD_SCALE, Earth.GOLD_PALE, 0.4)), true);
		stopper.box(0, -3.4, 0, 2, 2, 2, cord());
		SpellArt.Bone streams = m.bone("streams", "root", 0, 0, 0);
		for (int k = 0; k < 6; k++) {
			SpellArt.Bone s = m.bone("stream_" + k, "streams", 0, 0, 0);
			for (int i = 0; i < 8; i++) {
				// A length of the spiral from one point of it to the next: out and down from the mouth, winding round.
				double[] a = streamAt(k, i), b = streamAt(k, i + 1);
				SpellArt.limb(s, a, b, 2.6 + i * 0.35, 0.6, stream(961 + i % 4)).density(1.5);
			}
		}
		vaseCome(m);
		vaseDrink(m);
		vaseSeal(m);
		vaseSlam(m);
	}

	/** Its coming (played once): it tips over into place, mouth down, from upright, its streams unseen till it drinks. */
	static void vaseCome(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("come", L, null);
		a.sample("vase", "rotation", 0, L, 8, t -> new double[] {0, 0, 180 * (1 - SpellArt.ease(t / L))});
		a.sample("vase", "position", 0, L, 8, t -> new double[] {0, 42 * (1 - SpellArt.ease(t / L)), 0});
		a.scale("streams", 0, 0.02);
		a.scale("streams", 0.75, 0.02);
		a.scale("streams", L, 1);
		a.scale("stopper", 0, 0.02);
		a.scale("stopper", L, 0.02);
	}

	/** Its drinking, looped without a seam: its streams winding up into it, swelling as they run. */
	static void vaseDrink(SpellArt.Model m) {
		double T = 1.5;
		SpellArt.Anim a = m.anim("drink", T, "true");
		a.sample("streams", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		for (int k = 0; k < 6; k++) {
			int kk = k;
			a.sample("stream_" + k, "scale", 0, T, 8, t -> {
				double s = 1 + 0.1 * Math.sin(2 * Math.PI * t / T * 2 + kk);
				return new double[] {s, 1, s};
			});
		}
		a.scale("stopper", 0, 0.02);
		a.scale("stopper", T, 0.02);
	}

	/** It seals (played once, held): its streams drawn up into it, its stopper thrust home. */
	static void vaseSeal(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("seal", L, "hold_on_last_frame");
		a.scale("streams", 0, 1);
		a.scale("streams", 0.4, 0.02, 0.02, 0.02);
		a.scale("streams", L, 0.02);
		a.scale("stopper", 0, 0.02);
		a.scale("stopper", 0.3, 0.02);
		a.scale("stopper", 0.45, 1.2);
		a.scale("stopper", L, 1);
	}

	/** It slams down (played once, held): it squats as it strikes, sealed. */
	static void vaseSlam(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("slam", 0.5, "hold_on_last_frame");
		a.scale("vase", 0, 1);
		a.scale("vase", 0.1, 1.15, 0.8, 1.15);
		a.scale("vase", 0.3, 0.97, 1.05, 0.97);
		a.scale("vase", 0.5, 1);
		a.scale("streams", 0, 0.02);
		a.scale("streams", 0.5, 0.02);
		a.scale("stopper", 0, 1);
		a.scale("stopper", 0.5, 1);
	}

	// --- Immortal-Binding Golden Ropes: the ring ---

	/** A coil of braided golden rope (on a cube): strands of gold twisting round it, each lit along its crest. */
	static SpellArt.Painter goldenRope(int seed) {
		return t -> {
			double twist = Math.sin((t.fx * 2 + t.fy * 3 + t.lz * 0.8 + t.lx * 0.8) * Math.PI * 2 + seed);
			int c = twist > 0.3 ? SpellArt.mix(Earth.GOLD_SCALE, Earth.GOLD_PALE, (twist - 0.3) * 1.4) : SpellArt.mix(Earth.BRONZE, Earth.GOLD_SCALE, 0.5 + twist);
			return twist > 0.85 ? SpellArt.glow(c, 0.95) : SpellArt.col(c);
		};
	}

	/** A talisman of yellow paper (on a blade, hung from v 0): red cinnabar script down it, a seal at its foot. */
	static SpellArt.Painter talisman(int seed) {
		return t -> {
			double v = t.v(), u = t.u();
			if (u < 0.06 || u > 0.94 || v < 0.02) return SpellArt.col(0xC8A040);
			if (u > 0.3 && u < 0.7 && v > 0.08 && v < 0.85) {
				double cell = v * 5;
				if (Fire.glyph((int) cell + seed, seed, (u - 0.3) / 0.4, cell - Math.floor(cell))) return SpellArt.glow(0xE0281A, 0.9);
			}
			if (v > 0.88 && Math.abs(u - 0.5) < 0.2) return SpellArt.col(0xC0201A);
			return SpellArt.col(SpellArt.mix(0xF2D884, 0xE6C460, SpellArt.noise(u * 4 + seed, v * 9, seed)));
		};
	}

	/**
	 * The ring of Immortal-Binding Golden Ropes, its heart at the origin, fourteen pixels round: its coil of braided golden rope, three turns of it
	 * ("coil", turning), eight talismans hung from it ("tags") and eight little gold bells between them.
	 */
	static void bindingRing(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone coil = m.bone("coil", "root", 0, 0, 0);
		for (int i = 0; i < 3; i++) ring(coil, 0, -1.6 + 1.6 * i, 0, 13.5 + 0.6 * Math.sin(i * 2), 2.2, 24, goldenRope(1001 + i));
		SpellArt.Bone tags = m.bone("tags", "coil", 0, -2.5, 0);
		for (int k = 0; k < 8; k++) {
			double th = 45 * k + 22.5;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 13.5, -2.5, Math.cos(Math.toRadians(th)) * 13.5);
			// Hung straight down from the coil (a blade swept 180: its length down).
			SpellArt.shared(m, tags, "talisman", false, base, 9, 3.6, 180, th + 90, 0, talisman(1011), 3, k % 2 == 0);
			double tb = Math.toRadians(45 * k);
			tags.box(Math.sin(tb) * 13.5, -4, Math.cos(tb) * 13.5, 1.8, 2.2, 1.8, bellGold());
		}
		SpellArt.Anim form = m.anim("form", 0.6, null);
		form.sample("root", "scale", 0, 0.6, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 0.6) * (1 + 0.1 * Math.sin(Math.PI * t / 0.6)));
			return new double[] {s, s, s};
		});
		SpellArt.Anim turn = m.anim("turn", 3, "true");
		turn.sample("coil", "rotation", 0, 3, 12, t -> new double[] {0, 120 * t, 0});
		turn.sample("tags", "rotation", 0, 3, 12, t -> new double[] {5 * Math.sin(2 * Math.PI * t / 3 * 2), 0, 5 * Math.cos(2 * Math.PI * t / 3 * 2)});
		SpellArt.Anim cinch = m.anim("cinch", 0.6, null);
		cinch.scale("root", 0, 1);
		cinch.scale("root", 0.15, 0.7, 1.2, 0.7);
		cinch.scale("root", 0.35, 1.08, 0.95, 1.08);
		cinch.scale("root", 0.6, 1);
	}

	// --- Golden Dragon Scissors ---

	static final int SCISSOR_GOLD_DEEP = 0x7A5414, SCISSOR_GOLD = 0xD8A63C, SCISSOR_GOLD_LIGHT = 0xFFE08A, SCISSOR_EDGE = 0xFFFBEA;

	/**
	 * A blade of the scissors (on a box lying along z): heavy gold, its back engraved with a dragon's scales in rows along it (the blade is the
	 * dragon's body), its edge (the side toward {@code edgeX}) ground to a white line of light.
	 */
	static SpellArt.Painter scissorBlade(double edgeX, int seed) {
		return t -> {
			double toEdge = edgeX > 0 ? t.across() : 1 - t.across();
			if (t.flat() && toEdge > 0.82) return SpellArt.glow(SCISSOR_EDGE, 0.98);
			if (t.side() && (edgeX > 0) == (t.f == SpellArt.Face.SIDE_B)) return SpellArt.glow(SCISSOR_EDGE, 0.98);
			double row = t.qz() / 2.2, along = row - Math.floor(row), col = t.qx() / 2.0 + Math.floor(row) * 0.5, across = col - Math.floor(col);
			double scale = Math.hypot(across - 0.5, along * 0.8);
			double light = SpellArt.clamp01(0.45 + 0.3 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny) + (scale < 0.42 ? 0.15 : -0.15));
			if (toEdge > 0.62 && toEdge <= 0.82) return SpellArt.col(SpellArt.mix(SCISSOR_GOLD_LIGHT, SCISSOR_EDGE, (toEdge - 0.62) * 4));
			return SpellArt.col(SpellArt.ramp(light, SCISSOR_GOLD_DEEP, SCISSOR_GOLD, SCISSOR_GOLD_LIGHT));
		};
	}

	/** A coil of the handle's dragon (on a cube): gold scales, a ridge of spines along its back glowing. */
	static SpellArt.Painter dragonCoil(int seed) {
		return t -> {
			double row = t.qy() / 1.6 + t.qz() / 1.6, f = row - Math.floor(row);
			if (t.ny > 0.6 && Math.abs(t.fx - 0.5) < 0.15) return SpellArt.glow(SCISSOR_GOLD_LIGHT, 0.9);
			return SpellArt.col(SpellArt.mix(SCISSOR_GOLD_DEEP, SCISSOR_GOLD, f < 0.2 ? 0.2 : 0.5 + 0.4 * Math.max(0, t.ny)));
		};
	}

	/**
	 * The Golden Dragon Scissors, lying level, their pivot at the origin and their points 120 pixels out along -z (64 blocks): two blades of gold,
	 * each a dragon's body drawn out long, its scales engraved down it and its edge ground to light, its horned head the blade's heel at the pivot
	 * ("blade_a", "blade_b", each turning about the pivot), their handles behind as the dragons' tails coiled into rings, and the pivot a rivet of jade.
	 */
	static void dragonScissors(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int s = -1; s <= 1; s += 2) {
			String name = s < 0 ? "blade_a" : "blade_b";
			SpellArt.Bone b = m.bone(name, "root", 0, 0, 0);
			// The blade: its back thick by the pivot, tapering to its point; its edge toward the other blade.
			double y = s * 1.3;
			for (int i = 0; i < 6; i++) {
				double z0 = -6 - i * 19, z1 = z0 - 19, wide = 9 - i * 1.3, thick = 2.4 - i * 0.25;
				b.box(-s * wide * 0.25, y, (z0 + z1) / 2, wide, thick, 19.4, scissorBlade(s, 1101 + i)).density(1.2);
			}
			b.box(-s * 0.6, y, -122, 2.4, 1.2, 6, scissorBlade(s, 1109)).rot(0, s * 12, 0);
			// Its heel at the pivot: the dragon's horned head, its jaws gripping the rivet.
			b.box(-s * 1.5, y, -2, 10, 3.4, 9, dragonCoil(1111));
			for (int h = -1; h <= 1; h += 2) b.box(-s * 1.5 + h * 3, y + 2.6, 2.5, 1.2, 1.2, 7, dragonCoil(1113)).rot(-30, h * 15, 0);
			// The handle behind: the dragon's tail coiled into a ring.
			double cz = 18, r = 9;
			for (int k = 0; k < 14; k++) {
				double th = 360.0 * k / 14;
				double x = -s * 4 + Math.sin(Math.toRadians(th)) * r, z = cz + Math.cos(Math.toRadians(th)) * r;
				b.box(x, y, z, 4.6, 3, 4.6, dragonCoil(1121 + k % 3)).rot(0, th, 0);
			}
			SpellArt.limb(b, SpellArt.v3(-s * 1.5, y, 3), SpellArt.v3(-s * 4, y, cz - r + 1), 4, 3, dragonCoil(1131));
		}
		SpellArt.Bone rivet = m.bone("rivet", "root", 0, 0, 0);
		round(rivet, 0, 0, 0, 3.4, 6, t -> SpellArt.glow(SpellArt.mix(Wood.JADE, Wood.PALE, 0.3 + 0.4 * Math.max(0, t.ny)), 0.95), true);
		SpellArt.Anim open = m.anim("open", 1.0, null);
		for (int s = -1; s <= 1; s += 2) {
			String name = s < 0 ? "blade_a" : "blade_b";
			int ss = s;
			open.sample(name, "rotation", 0, 1.0, 8, t -> new double[] {0, ss * 35 * SpellArt.ease(t), 0});
		}
		open.sample("root", "scale", 0, 0.5, 5, t -> {
			double sc = Math.max(0.02, SpellArt.easeOut(t / 0.5));
			return new double[] {sc, sc, sc};
		});
		SpellArt.Anim hover = m.anim("hover", 2, "true");
		for (int s = -1; s <= 1; s += 2) {
			String name = s < 0 ? "blade_a" : "blade_b";
			int ss = s;
			hover.sample(name, "rotation", 0, 2, 8, t -> new double[] {0, ss * (35 + 1.5 * Math.sin(2 * Math.PI * t / 2)), 0});
		}
		hover.sample("root", "position", 0, 2, 8, t -> new double[] {0, 1.2 * Math.sin(2 * Math.PI * t / 2), 0});
		SpellArt.Anim snap = m.anim("snap", 0.3, "hold_on_last_frame");
		for (int s = -1; s <= 1; s += 2) {
			String name = s < 0 ? "blade_a" : "blade_b";
			snap.rot(name, 0, 0, s * 35, 0);
			snap.rot(name, 0.22, 0, -s * 1.5, 0);
			snap.rot(name, 0.3, 0, 0, 0);
		}
		snap.pos("root", 0, 0, 0, 0);
		snap.pos("root", 0.22, 0, -2, 0);
		snap.pos("root", 0.3, 0, 0, 0);
	}

	// --- Mirror of Retribution ---

	static final int MIRROR_BRONZE_DARK = 0x2A1C10, MIRROR_BRONZE = 0x6A4A26, MIRROR_GILT = 0xC08A3C;
	static final int[] HELL_FIRE = {0x3A0604, 0x8A1408, 0xC8361E, 0xF0702C, 0xFFB060, 0xFFE4B8};

	/** The mirror's face (on a sheet facing -z): polished bronze gone to dark water, souls drifting in it in the dead's teal light, swirling to its heart. */
	static SpellArt.Painter karmaFace(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98) return SpellArt.CLEAR;
			double swirl = SpellArt.fbm(Math.atan2(y, x) * 1.5 + r * 4 + seed, r * 6, seed, 3);
			double soul = SpellArt.smooth(0.62, 0.8, SpellArt.fbm(x * 5 + seed, y * 5 + swirl * 2, seed + 3, 3));
			int c = SpellArt.mix(SpellArt.mix(0x04120F, Ghost.FIRE_DEEP, swirl), Ghost.FIRE_CYAN, soul * 0.8);
			if (r > 0.9) c = SpellArt.mix(c, Ghost.MIST, (r - 0.9) * 8);
			return SpellArt.glow(c, 0.97);
		};
	}

	/** The mirror's bronze (its frame, its back): cast with hell's flames in relief, gilt on their edges. */
	static SpellArt.Painter mirrorBronze(int seed) {
		return t -> {
			double flame = Math.sin(t.px * 0.9 + Math.sin(t.py * 0.7 + seed) * 2) * Math.cos(t.py * 0.8 + t.pz * 0.6);
			if (Math.abs(flame) > 0.9) return SpellArt.col(MIRROR_GILT);
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 2);
			return SpellArt.col(SpellArt.mix(MIRROR_BRONZE_DARK, MIRROR_BRONZE, 0.3 + 0.5 * n + 0.2 * Math.max(0, t.ny)));
		};
	}

	static final int VERDIGRIS = 0x3E7A64;

	/**
	 * The mirror's back (on a disc facing +z, painted where it lies: its heart on the z axis): the cast bronze of an old mirror, its rings in
	 * relief catching the light in gilt, between the outer two a band of hell's flames in gilt relief, between the inner two eight bosses; its
	 * hollows gone to verdigris; plain within, round its knob.
	 */
	static SpellArt.Painter mirrorBack(double radius, int seed) {
		return t -> {
			double r = Math.hypot(t.px, t.py) / radius, a = Math.atan2(t.py, t.px);
			double patina = SpellArt.fbm(t.px * 0.35 + seed, t.py * 0.35, seed, 3);
			int ground = SpellArt.mix(MIRROR_BRONZE_DARK, MIRROR_BRONZE, 0.35 + 0.5 * patina);
			if (patina > 0.62) ground = SpellArt.mix(ground, VERDIGRIS, (patina - 0.62) * 2.2);
			if (t.f != SpellArt.Face.FRONT && t.f != SpellArt.Face.BACK) return SpellArt.col(SpellArt.mix(MIRROR_BRONZE, MIRROR_GILT, 0.45));
			if (r > 0.9) return SpellArt.col(SpellArt.mix(MIRROR_GILT, MIRROR_BRONZE, 0.25 + 0.4 * (r - 0.9) * 10));
			for (double ring : new double[] {0.86, 0.55, 0.3}) if (Math.abs(r - ring) < 0.028) return SpellArt.col(MIRROR_GILT);
			if (r > 0.55) {
				// Flames in relief: tongues standing out from the inner ring, swaying, their tips licking at the outer.
				double f = a / (2 * Math.PI) * 14 + 0.25 * Math.sin((r - 0.55) * 18 + seed), frac = f - Math.floor(f), up = (r - 0.55) / 0.31;
				double half = 0.42 * Math.pow(Math.max(0, 1 - up), 0.7);
				if (Math.abs(frac - 0.5) < half) return SpellArt.col(Math.abs(frac - 0.5) > half - 0.08 ? MIRROR_GILT : SpellArt.mix(MIRROR_BRONZE, MIRROR_GILT, 0.55));
				return SpellArt.col(SpellArt.shade(ground, -0.18));
			}
			if (r > 0.3) {
				for (int k = 0; k < 8; k++) {
					double ba = Math.PI * 2 * k / 8, bx = Math.cos(ba) * 0.425, by = Math.sin(ba) * 0.425, d = Math.hypot(t.px / radius - bx, t.py / radius - by);
					if (d < 0.06) return SpellArt.col(SpellArt.mix(MIRROR_GILT, 0xFFE0A0, 1 - d / 0.06));
					if (d < 0.08) return SpellArt.col(MIRROR_BRONZE_DARK);
				}
			}
			return SpellArt.col(ground);
		};
	}

	/** A disc of {@code r} (pixels) {@code h} thick about (0, 0, z), facing ±z: sixteen-sided, eight slabs turned 22.5 degrees apart. */
	static void disc(SpellArt.Bone bone, double z, double r, double h, SpellArt.Painter p, double tpp) {
		double side = 2 * r * Math.tan(Math.PI / 16);
		for (int k = 0; k < 8; k++) bone.box(0, 0, z, 2 * r, side, h, p).rot(0, 0, 22.5 * k).density(tpp);
	}

	/**
	 * The Mirror of Retribution, its face's heart at the origin, facing -z, eleven pixels round: its face of dark water where souls show their sins
	 * ("face", turning), its frame of bronze cast with hell's flames, its back an old mirror's (rings in relief, a band of flames, eight bosses, a
	 * knob with a cord of red silk), a ring of hell fire and ghost fire round it ("fire"), and its stand of bone beneath, its foot a skull-less lotus
	 * of bone ("stand").
	 */
	static void karmaMirror(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone mirror = m.bone("mirror", "root", 0, 0, 0);
		SpellArt.Bone face = m.bone("face", "mirror", 0, 0, 0);
		SpellArt.sheet(face, 0, 0, -0.8, 21, 21, 90, 0, 0, karmaFace(1201)).density(3);
		// Its frame: a round of bronze boxes in the upright plane facing -z.
		double r = 11.2;
		for (int k = 0; k < 20; k++) {
			double th = 360.0 * k / 20;
			double x = Math.sin(Math.toRadians(th)) * r, y = Math.cos(Math.toRadians(th)) * r;
			mirror.box(x, y, 0, 2 * r * Math.sin(Math.PI / 20) * 1.1, 2.4, 2.4, mirrorBronze(1203)).rot(0, 0, th);
		}
		// Its back, and the knob in its heart, a cord of red silk tied through it.
		disc(mirror, 1, 10.6, 1.6, mirrorBack(10.6, 1205), 2);
		disc(mirror, 2.2, 2.4, 1.2, t -> SpellArt.col(SpellArt.mix(MIRROR_GILT, MIRROR_BRONZE, 0.3 + 0.3 * SpellArt.noise(t.px * 2, t.py * 2, 1207))), 3);
		disc(mirror, 3.2, 1.5, 1.2, t -> SpellArt.col(SpellArt.mix(MIRROR_GILT, 0xFFE0A0, 0.4)), 3);
		SpellArt.limb(mirror, SpellArt.v3(0, -1.2, 3.6), SpellArt.v3(0.8, -5.5, 4.2), 0.7, 0.7, cord());
		SpellArt.limb(mirror, SpellArt.v3(0, -1.2, 3.6), SpellArt.v3(-0.9, -5, 4.0), 0.7, 0.7, cord());
		SpellArt.Bone fire = m.bone("fire", "mirror", 0, 0, 0);
		for (int k = 0; k < 14; k++) {
			double th = 360.0 * k / 14;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * (r + 1), Math.cos(Math.toRadians(th)) * (r + 1), 0.2);
			boolean hell = k % 2 == 0;
			SpellArt.shared(m, fire, hell ? "hell_flame" : "ghost_flame", false, base, 7, 4, 0, 90, th, hell ? Fire.flame(3, 1211, HELL_FIRE) : Ghost.ghostFire(3, 1213), 3, k % 4 < 2);
		}
		SpellArt.Bone stand = m.bone("stand", "mirror", 0, -r, 0);
		SpellArt.Painter bone = Ghost.boneStone(1221, false);
		stand.box(0, -r - 5, 0.5, 3, 9, 3, bone);
		stand.box(0, -r - 10.5, 0.5, 8, 2.4, 6, bone);
		for (int k = 0; k < 6; k++) {
			double th = 60 * k;
			stand.box(Math.sin(Math.toRadians(th)) * 3.5, -r - 12, 0.5 + Math.cos(Math.toRadians(th)) * 3.5, 3.5, 1.6, 2, bone).rot(-20, th, 0);
		}
		SpellArt.Anim rise = m.anim("rise", 0.5, null);
		rise.sample("root", "scale", 0, 0.5, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 0.5));
			return new double[] {s, s, s};
		});
		SpellArt.Anim shine = m.anim("shine", 2, "true");
		shine.sample("face", "rotation", 0, 2, 8, t -> new double[] {0, 0, 180 * t});
		shine.sample("fire", "scale", 0, 2, 8, t -> new double[] {1 + 0.08 * Math.sin(2 * Math.PI * t / 2 * 3), 1 + 0.08 * Math.sin(2 * Math.PI * t / 2 * 3), 1});
		SpellArt.Anim flash = m.anim("flash", 0.4, null);
		flash.scale("mirror", 0, 1);
		flash.scale("mirror", 0.1, 1.25);
		flash.scale("mirror", 0.4, 1);
	}

	// --- Soul-Locking Pagoda ---

	static final int IRON_DARK = 0x14161A, IRON = 0x2C3038, IRON_LIGHT = 0x50586A;

	/** The pagoda's black iron (on a solid): dark, studded in rows, rust-red in its seams, the dead's glyphs cut in it here and there lit with ghost fire. */
	static SpellArt.Painter hellIron(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3);
			double sx = t.fx * 6, sy = t.fy * 4;
			if (Math.hypot(sx - Math.floor(sx) - 0.5, sy - Math.floor(sy) - 0.5) < 0.16) return SpellArt.col(IRON_LIGHT);
			if (n > 0.7) {
				double cell = t.fx * 3;
				if (Fire.glyph((int) cell + seed, seed, cell - Math.floor(cell), t.fy)) return SpellArt.glow(Ghost.FIRE_CYAN, 0.92);
			}
			if (n < 0.25) return SpellArt.col(SpellArt.mix(IRON_DARK, 0x5A1E10, 0.5));
			return SpellArt.col(SpellArt.mix(IRON_DARK, IRON, 0.4 + 0.5 * n + 0.15 * Math.max(0, t.ny)));
		};
	}

	/** A tier's width at {@code k} (0 the lowest), in pixels. */
	static double tierWide(int k) {
		return 26 - 2.2 * k;
	}

	/**
	 * The Soul-Locking Pagoda, its foot on the land at the origin, fifty-six pixels tall, seven tiers: each tier's body of black iron and bone
	 * ("tier_0".."tier_6"), its eaves of slate tiles sweeping up at their corners, a soul lantern hung at each corner ("lanterns_<k>", swinging) and
	 * ghost fire licking off them ("fire"), its spire of rings and a pearl of ghost fire ("spire"), and chains from the spire's foot to the top eaves.
	 */
	static void soulPagoda(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone base = m.bone("base", "root", 0, 0, 0);
		SpellArt.Painter bone = Ghost.boneStone(1301, true), iron = hellIron(1303), tiles = Ghost.tiles(1305);
		base.box(0, 1.5, 0, 30, 3, 30, bone);
		SpellArt.Bone fire = m.bone("fire", "root", 0, 0, 0);
		double y = 3;
		for (int k = 0; k < 7; k++) {
			double w = tierWide(k), h = 5.6 - 0.25 * k;
			SpellArt.Bone tier = m.bone("tier_" + k, "root", 0, y, 0);
			tier.box(0, y + h / 2, 0, w * 0.78, h, w * 0.78, iron).density(1.25);
			for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) tier.box(sx * w * 0.39, y + h / 2, sz * w * 0.39, 1.6, h, 1.6, bone);
			// Its eaves: a roof sweeping out over it, tiled, its four corners swept up.
			double ey = y + h + 0.6;
			tier.box(0, ey, 0, w, 1.2, w, tiles).density(1);
			tier.box(0, ey + 1.1, 0, w * 0.82, 1.1, w * 0.82, tiles).density(1);
			for (int c = 0; c < 4; c++) {
				double th = 45 + 90 * c;
				double[] corner = SpellArt.v3(Math.sin(Math.toRadians(th)) * w * 0.66, ey, Math.cos(Math.toRadians(th)) * w * 0.66);
				SpellArt.limb(tier, corner, SpellArt.add(corner, SpellArt.v3(Math.sin(Math.toRadians(th)) * 3, 2.2, Math.cos(Math.toRadians(th)) * 3)), 1.2, 1.2, iron);
				SpellArt.Bone lantern = m.bone("lantern_" + k + "_" + c, "tier_" + k, corner[0], corner[1], corner[2]);
				SpellArt.limb(lantern, corner, SpellArt.add(corner, SpellArt.v3(0, -2.2, 0)), 0.3, 0.3, Ghost.pole());
				lantern.box(corner[0], corner[1] - 3.6, corner[2], 1.8, 2.4, 1.8, Ghost.lantern(1311 + k));
				SpellArt.shared(m, fire, "pagoda_fire", false, SpellArt.add(corner, SpellArt.v3(0, 1.5, 0)), 4 + (k % 2), 2.6, 10, th, 0, Ghost.ghostFire(3, 1321), 3, c % 2 == 0);
			}
			y = ey + 2;
		}
		SpellArt.Bone spire = m.bone("spire", "root", 0, y, 0);
		for (int i = 0; i < 5; i++) round(spire, 0, y + 1 + i * 2.2, 0, 2.6 - i * 0.25, 1.2, t -> SpellArt.col(IRON_LIGHT), true);
		round(spire, 0, y + 12.5, 0, 2.2, 3.4, t -> SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, 0xF0FFFC, 0.4), 0.95), true);
		SpellArt.shared(m, spire, "spire_fire", false, SpellArt.v3(0, y + 13.5, 0), 6, 3.5, 0, 0, 0, Ghost.ghostFire(4, 1331), 3, false);
		// Chains from the spire's foot to the top tier's eaves.
		double top = tierWide(6);
		for (int c = 0; c < 4; c++) {
			double th = 45 + 90 * c;
			double[] a = SpellArt.v3(0, y + 2, 0), b = SpellArt.v3(Math.sin(Math.toRadians(th)) * top * 0.6, y - 1.5, Math.cos(Math.toRadians(th)) * top * 0.6);
			for (int i = 0; i < 4; i++) {
				double[] p0 = SpellArt.add(a, SpellArt.times(SpellArt.sub(b, a), i / 4.0)), p1 = SpellArt.add(a, SpellArt.times(SpellArt.sub(b, a), (i + 1) / 4.0));
				SpellArt.limb(spire, p0, SpellArt.add(p1, SpellArt.v3(0, -0.3 * Math.sin(Math.PI * (i + 1) / 4), 0)), 0.5, 0.5, Void.link());
			}
		}
		SpellArt.Anim glow = m.anim("glow", 2.4, "true");
		glow.sample("fire", "scale", 0, 2.4, 12, t -> new double[] {1, 1 + 0.25 * Math.sin(2 * Math.PI * t / 2.4 * 3), 1});
		for (int k = 0; k < 7; k++) for (int c = 0; c < 4; c++) {
			int kk = k, cc = c;
			glow.sample("lantern_" + k + "_" + c, "rotation", 0, 2.4, 8, t -> new double[] {8 * Math.sin(2 * Math.PI * t / 2.4 + kk + cc), 0, 6 * Math.cos(2 * Math.PI * t / 2.4 + kk * 1.3 + cc)});
		}
		SpellArt.Anim land = m.anim("land", 0.5, null);
		for (int k = 0; k < 7; k++) {
			land.pos("tier_" + k, 0, 0, 0, 0);
			land.pos("tier_" + k, 0.1, 0, -0.6 * (k + 1), 0);
			land.pos("tier_" + k, 0.3, 0, 0.2 * (k + 1), 0);
			land.pos("tier_" + k, 0.5, 0, 0, 0);
		}
		SpellArt.Anim seal = m.anim("seal", 0.7, "hold_on_last_frame");
		seal.scale("fire", 0, 1);
		seal.scale("fire", 0.2, 1.8, 2.6, 1.8);
		seal.scale("fire", 0.7, 1.2, 1.6, 1.2);
		seal.scale("spire", 0, 1);
		seal.scale("spire", 0.25, 1.3);
		seal.scale("spire", 0.7, 1.1);
	}

	// --- Heaven-Demon Bell ---

	/** The bell's height, to its crown (pixels). */
	static final double BELL_TALL = 92;

	/** The bell's radius {@code y} pixels up from its rim: a lip flared at its mouth, its body drawn in up its height, its shoulder rounded over. */
	static double bellRadius(double y) {
		if (y < 5) return 36 - 0.7 * y;
		if (y < 78) return 32.5 - 7 * Math.pow((y - 5) / 73.0, 1.6);
		if (y < BELL_TALL) return 25.5 * Math.sqrt(Math.max(0, 1 - Math.pow((y - 78) / (BELL_TALL - 78 + 0.6), 2)));
		return 0;
	}

	/**
	 * Heaven's half of the bell (on a solid): bright cast gold, smooth, darker in its hollows, lit where it faces up, a cloud scroll here and
	 * there in low relief catching the light; glowing a little.
	 */
	static SpellArt.Painter bellHeaven(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed, 3);
			double scroll = Math.sin(t.py * 0.4 + 2 * Math.sin(Math.atan2(t.px, t.pz) * 6 + seed)) * Math.cos(Math.atan2(t.px, t.pz) * 9 + t.py * 0.2);
			double light = SpellArt.clamp01(0.5 + (n - 0.5) * 0.4 + 0.25 * Math.max(0, t.ny) - 0.2 * Math.max(0, -t.ny) - 0.12 * Math.max(0, t.nx) + (Math.abs(scroll) > 0.94 ? 0.2 : 0));
			return SpellArt.glow(SpellArt.ramp(light, HeavenDemon.GOLD_DEEP, HeavenDemon.GOLD, HeavenDemon.GOLD_BRIGHT, HeavenDemon.GOLD_PALE), 0.97);
		};
	}

	/**
	 * The demon's half (on a solid): crimson bronze, dark and smooth, darker in its hollows, lit where it faces up; a thin vein of burning scarlet
	 * now and then (they glow).
	 */
	static SpellArt.Painter bellDemon(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed, 3);
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.06, t.py * 0.08, t.pz * 0.06, seed + 3, 3) - 1), 16);
			if (vein > 0.9 && SpellArt.noise3(t.px * 0.04, t.py * 0.04, t.pz * 0.04, seed + 9) > 0.55) return SpellArt.glow(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.EMBER_PALE, (vein - 0.9) * 8), 0.96);
			double light = SpellArt.clamp01(0.45 + (n - 0.5) * 0.4 + 0.25 * Math.max(0, t.ny) - 0.2 * Math.max(0, -t.ny) + 0.1 * Math.max(0, t.nx));
			return SpellArt.col(SpellArt.ramp(light, HeavenDemon.BLOOD_DARK, HeavenDemon.BLOOD, HeavenDemon.CRIMSON, HeavenDemon.SCARLET));
		};
	}

	/**
	 * The Heaven-Demon Bell, its rim on the land at the origin, thirty-six pixels round (its spell's mouth), ninety-two tall (a temple bell's
	 * height, half again its breadth): its body heaven's gold to -x and the demon's crimson bronze to +x (HeavenDemon#halves, a seam of white light
	 * down between), girdled with raised bands and studded with bosses in four panels ("bell"), its striking boss a sun on heaven's side and a
	 * fanged moon on the demon's, its hanging loop of two twined ribbons of gold and crimson arched over its crown ("loop"); its mouth's dark hollow
	 * ringed with light.
	 */
	static void heavenDemonBell(SpellArt.Model m) {
		m.tpp = 0.75;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone bell = m.bone("bell", "root", 0, BELL_TALL, 0);
		SpellArt.Painter body = HeavenDemon.halves(bellHeaven(1401), bellDemon(1403));
		lathe(bell, 0, BELL_TALL, 2.5, Vessel::bellRadius, body);
		// Its bands: raised rings round it.
		for (double y : new double[] {6, 26, 50, 66, 77}) round(bell, 0, y, 0, bellRadius(y) + 0.9, y == 6 ? 3 : 1.8, body, true);
		// The bosses: four panels of studs between its upper bands, three rows of four in each.
		for (int panel = 0; panel < 4; panel++) {
			double th0 = 90 * panel + 45;
			for (int row = 0; row < 3; row++) for (int col = 0; col < 4; col++) {
				double y = 54 + row * 4, th = Math.toRadians(th0 + (col - 1.5) * 7.5), r = bellRadius(y) + 0.8;
				bell.box(Math.sin(th) * r, y, Math.cos(th) * r, 2.6, 2.6, 2.6, body).rot(45, Math.toDegrees(th), 0);
			}
		}
		// Its striking bosses: heaven's sun and the demon's moon, low on its waist, each a lotus of eight petals round a boss.
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Painter p = s < 0 ? bellHeaven(1405) : bellDemon(1407);
			double r = bellRadius(16) + 1;
			bell.box(s * r, 16, 0, 1.8, 9, 9, p);
			bell.box(s * r, 16, 0, 1.8, 9, 9, p).rot(45, 0, 0);
			bell.box(s * (r + 0.9), 16, 0, 1.8, 5, 5, p).rot(22.5, 0, 0);
		}
		// Its mouth's dark hollow, its rim's light.
		SpellArt.sheet(bell, 0, 0.1, 0, 70, 70, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 1) return SpellArt.CLEAR;
			return r > 0.9 ? SpellArt.glow(SpellArt.mix(HeavenDemon.GOLD_PALE, HeavenDemon.EMBER_PALE, t.across()), 0.9) : SpellArt.col(SpellArt.mix(0x0A0406, 0x2A1008, r), 0.96);
		}).density(0.6);
		// Its hanging loop: two ribbons twined, gold and crimson, arching over its crown.
		SpellArt.Bone loop = m.bone("loop", "bell", 0, BELL_TALL, 0);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Painter p = s < 0 ? bellHeaven(1411) : bellDemon(1413);
			for (int k = 0; k <= 12; k++) {
				double a = Math.PI * k / 12, x = 14 * Math.cos(a), yy = BELL_TALL - 1 + 18 * Math.sin(a);
				loop.box(x, yy, s * 2.2 * Math.sin(a * 4), 4.6, 4.6, 4.6, p).rot(0, 0, Math.toDegrees(a) - 90);
			}
		}
		SpellArt.Anim hang = m.anim("hang", 4, "true");
		hang.sample("bell", "rotation", 0, 4, 16, t -> new double[] {1.2 * Math.sin(2 * Math.PI * t / 4), 0, 1.2 * Math.cos(2 * Math.PI * t / 4)});
		SpellArt.Anim toll = m.anim("toll", 0.9, null);
		double[] times = {0, 0.06, 0.16, 0.3, 0.5, 0.7, 0.9};
		double[] ring = {1, 1.06, 0.96, 1.03, 0.99, 1.01, 1};
		for (int i = 0; i < times.length; i++) toll.scale("bell", times[i], ring[i], 1 + (1 - ring[i]) * 0.5, ring[i]);
		toll.rot("bell", 0, 0, 0, 0);
		toll.rot("bell", 0.1, 3, 0, 0);
		toll.rot("bell", 0.35, -2, 0, 0);
		toll.rot("bell", 0.6, 1, 0, 0);
	}
}