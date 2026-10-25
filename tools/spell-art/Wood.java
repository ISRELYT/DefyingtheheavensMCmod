/**
 * The wood element's Dao Integration spells for SpellArt: the Azure Dragon (living wood and jade leaves) and Myriad Woods Return to the
 * Dao's spirit tree.
 */
public class Wood {
	// --- The wood element's colours: azure and jade scales, bark, celadon, the veins' light ---
	static final int DEEP = 0x145266, AZURE = 0x2A8EAA, JADE = 0x3ABD94, JADE_LIGHT = 0x8CEBC4, PALE = 0xD2F6E2, BELLY = 0xE4ECBA, BELLY_SEAM = 0x9AA670;
	static final int BARK = 0x46321F, BARK_LIGHT = 0x7A5A38, MOSS = 0x4E8A3C, VEIN = 0xB0FFE2, VEIN_CORE = 0xE8FFF4, IVORY = 0xEAF0D6;
	static final int LEAF_DARK = 0x1C7A4E, LEAF = 0x3CB070, LEAF_LIGHT = 0x9CEB9A;

	// --- Painters ---

	/**
	 * A jade leaf on a blade (SpellArt.blade): pointed, a little toothed, its midrib and veins glowing, seen through toward its rim.
	 * {@code fat} widens it (1 a willow's, 1.6 a broad leaf's).
	 */
	static SpellArt.Painter leaf(int dark, int light, double fat, int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(v, 0.8))), 0.9) * Math.min(1, fat * (0.62 + 0.38 * (1 - v)));
			half *= 1 - 0.07 * Math.abs(Math.sin(v * Math.PI * 9 + seed));
			if (au > half || v > 0.985) return SpellArt.CLEAR;
			double edge = half - au;
			if (au < 0.035 + 0.02 * (1 - v)) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, 1 - v), 0.95);
			double w = (v - au * 0.95) * 8;
			if (Math.abs(w - Math.rint(w)) < 0.1 && au > 0.06 && edge > 0.03) return SpellArt.glow(SpellArt.mix(VEIN, light, 0.4), 0.75);
			if (edge < 0.05) return SpellArt.col(SpellArt.shade(light, 0.25), 0.62);
			double n = SpellArt.fbm(t.lz * 1.7 + seed, t.ly * 1.7, seed, 2);
			int c = SpellArt.mix(dark, light, 0.25 + 0.6 * v + (n - 0.5) * 0.3);
			return SpellArt.col(SpellArt.shade(c, -0.12 * (1 - edge / Math.max(0.05, half)) + 0.04), 0.86);
		};
	}

	/** The dragon's hide, round a body whose axis is the line x = 0, y = {@code axisY}: azure scales above, jade on the flanks, a vein of light down each, belly plates. */
	static SpellArt.Painter hide(double axisY, double rowLength, int columns, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.ny < -0.6 && t.flat()) a = 1;
			// The belly's plates.
			if (a > 0.8) {
				double band = qz / 1.9 - Math.floor(qz / 1.9);
				int c = SpellArt.mix(BELLY, PALE, SpellArt.noise(qx * 0.6, qz * 0.4, seed) * 0.4);
				if (band < 0.16) return SpellArt.col(SpellArt.mix(c, BELLY_SEAM, 0.75));
				return SpellArt.col(SpellArt.shade(c, -0.1 * band + (a > 0.82 ? 0 : -0.08)));
			}
			// The spine: a ridge of living wood, its grain along the body.
			if (a < 0.13) {
				double grain = SpellArt.fbm(qx * 3.1, qz * 0.35, seed + 7, 3);
				int c = SpellArt.mix(SpellArt.mix(DEEP, BARK, 0.55), BARK_LIGHT, grain * 0.9 - 0.15);
				if (Math.abs(grain - 0.5) < 0.03) c = SpellArt.shade(c, -0.3);
				return SpellArt.col(c);
			}
			// Scales: rows down the body, each scale's free rim an arc toward the tail.
			double arc = th / (2 * Math.PI) * columns;
			double rowF = qz / rowLength;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = arc + Math.floorMod(row, 2) * 0.5;
			int colI = (int) Math.floor(colF);
			double u = colF - colI, rim = 0.32 + 0.68 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			int base = SpellArt.ramp((a - 0.13) / 0.67, DEEP, AZURE, JADE, JADE_LIGHT);
			double h = SpellArt.hash(colI, row, seed) - 0.5;
			int c;
			if (v > rim) c = SpellArt.shade(base, -0.32);
			else if (rim - v < 0.13) c = SpellArt.shade(base, -0.42);
			else if (rim - v < 0.27 && a > 0.2 && a < 0.62) return SpellArt.glow(SpellArt.mix(base, JADE_LIGHT, 0.55 + h * 0.4), 0.55); // the rims catch the light
			else c = SpellArt.shade(base, -0.16 + 0.34 * (v / rim) + h * 0.12);
			// The veins of light down the flanks, branching up and back like a leaf's.
			double side = Math.abs(th), vein = 1.62 + 0.2 * Math.sin(qz * 0.27 + seed) + 0.06 * Math.sin(qz * 0.9), r = Math.hypot(qx, qy);
			double off = (side - vein) * Math.max(2, r);
			if (Math.abs(off) < 0.27) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, 0.5), 0.92);
			double along = Math.floorMod((int) Math.floor((qz + 2.5 * Math.signum(th)) * 10), 55) / 10.0;
			if (off < 0 && off > -2.2 && along < 2.4 && Math.abs(-off - along * 0.85 - 0.12 * along * along) < 0.2) return SpellArt.glow(VEIN, 0.7);
			if (Math.abs(off) < 0.75) c = SpellArt.mix(c, JADE_LIGHT, 0.35);
			return SpellArt.col(c);
		};
	}

	/** Bark: brown, grained along the cube's length (y), mossed in patches, a bud of jade light now and then. */
	static SpellArt.Painter bark(int seed, double glowBuds) {
		return t -> {
			double n = SpellArt.fbm(t.lx * 2.2 + t.lz * 2.2 + seed, t.ly * 0.45, seed, 3);
			int c = SpellArt.mix(BARK, BARK_LIGHT, n * 1.2 - 0.2);
			if (Math.abs(n - 0.5) < 0.035) c = SpellArt.shade(c, -0.35);
			double moss = SpellArt.noise(t.px * 0.45 + 3, t.pz * 0.45 + t.py * 0.3, seed + 9);
			if (moss > 0.66) c = SpellArt.mix(c, MOSS, (moss - 0.66) * 2.5);
			if (glowBuds > 0 && SpellArt.hash3((int) Math.floor(t.px * 2), (int) Math.floor(t.py * 2), (int) Math.floor(t.pz * 2), seed) < glowBuds) return SpellArt.glow(VEIN);
			return SpellArt.col(c);
		};
	}

	/** Ivory claws and teeth, glowing jade at their points (the end toward {@code tipUp}: +1 the top of the cube, -1 its foot). */
	static SpellArt.Painter claw(int tipUp) {
		return t -> {
			double toward = tipUp > 0 ? t.up() : 1 - t.up();
			if (toward > 0.72) return SpellArt.glow(SpellArt.mix(IVORY, VEIN, (toward - 0.72) * 3));
			return SpellArt.col(SpellArt.mix(SpellArt.shade(IVORY, -0.25), IVORY, toward));
		};
	}

	// --- The Azure Dragon ---

	static final int SEGMENTS = 20;
	static final double SEG = 7;

	static double thick(int i) {
		double[] w = {7.4, 8.4, 9.8, 10.8, 11.2, 11.2, 11, 10.7, 10.4, 10, 9.5, 8.9, 8.3, 7.6, 6.8, 6, 5.1, 4.2, 3.3, 2.5};
		return w[i];
	}

	// --- The Azure Dragon, made of wood ---

	/** The dragon's wood: its bark's furrows, the bark, its light ridges, azure lichen on it, young jade growth; the pale wood beneath. */
	static final int WOOD_DARK = 0x2A2018, WOOD = 0x5E4A34, WOOD_LIGHT = 0x8E7552, LICHEN = 0x4A8C94, WOOD_YOUNG = 0x58A070, SAPWOOD = 0xD6C9A0, SAPWOOD_DARK = 0xA2936C;

	/**
	 * A plate of bark, or the furrow round it, at {@code g} (round the grain: a column a unit wide) and {@code z} (along it, pixels): plates
	 * long along the grain, set off from each other column by column, each its own shade of brown or grey-azure with lichen, or jade with
	 * young growth; the furrows dark, and some furrows (whole lines of them, down the grain) a vein of jade light.
	 */
	static int barkPlate(double g, double z, double plateLength, int seed) {
		int gi = (int) Math.floor(g);
		double gf = g - gi, zz = z / plateLength + SpellArt.hash(gi, 7, seed) * 0.8;
		int zi = (int) Math.floor(zz);
		double zf = zz - zi, edge = Math.min(Math.min(gf, 1 - gf) * 2.4, Math.min(zf, 1 - zf) * 2.4 * plateLength / 4);
		if (Math.min(gf, 1 - gf) < 0.09) {
			// The long furrows down the grain: some of them veins of light, unbroken.
			int furrow = gf < 0.5 ? gi : gi + 1;
			if (SpellArt.hash(furrow, 3, seed) > 0.72) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, 0.35), 0.93);
			return SpellArt.col(WOOD_DARK);
		}
		if (edge < 0.16) return SpellArt.col(SpellArt.mix(WOOD_DARK, WOOD, 0.3));
		double h = SpellArt.hash(gi, zi, seed), n = SpellArt.noise(g * 3 + seed, z * 0.35, seed + 1);
		int c = h < 0.55 ? SpellArt.mix(WOOD, WOOD_LIGHT, 0.2 + 0.6 * n) : h < 0.85 ? SpellArt.mix(SpellArt.mix(WOOD, LICHEN, 0.55), WOOD_LIGHT, 0.4 * n) : SpellArt.mix(WOOD_YOUNG, WOOD_LIGHT, 0.3 * n);
		// Each plate rounded: lit along its middle, shadowed toward its edges; fine cracks across it.
		c = SpellArt.shade(c, -0.18 + 0.3 * Math.min(1, edge));
		if (Math.abs(Math.sin(z * 1.7 + gi * 3.1)) < 0.06 && edge > 0.35) c = SpellArt.shade(c, -0.25);
		return SpellArt.col(c);
	}

	/**
	 * Living wood round a body whose axis runs along z (x = 0, y = {@code axisY}): plates of bark (see {@link #barkPlate}) grained along it
	 * and twisting round it as it goes, so its veins of light spiral down it; paler wood beneath, ringed; a face looking along it shows the
	 * rings of its cut.
	 */
	static SpellArt.Painter livingWood(double axisY, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.ny < -0.6 && t.flat()) a = 1;
			boolean across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (across) {
				double ring = Math.hypot(qx, qy) * 0.8 + SpellArt.noise(qx * 0.5, qy * 0.5, seed + 4) * 0.5;
				return SpellArt.col(Math.abs(ring - Math.rint(ring)) < 0.15 ? SAPWOOD_DARK : SAPWOOD);
			}
			if (a > 0.82) {
				double band = qz / 2.4 + SpellArt.noise(qx * 0.6, qz * 0.2, seed) * 0.4;
				return SpellArt.col(Math.abs(band - Math.rint(band)) < 0.1 ? SAPWOOD_DARK : SpellArt.mix(SAPWOOD, SAPWOOD_DARK, 0.2 + 0.3 * SpellArt.noise(qx, qz * 0.3, seed + 2)));
			}
			double g = th / (2 * Math.PI) * 11 + qz * 0.045 + 0.35 * SpellArt.noise(qz * 0.06, th * 2, seed);
			return barkPlate(g, qz, 6.5, seed);
		};
	}

	/** {@link #barkPlate} on a limb or a bough (its own axis along its cube): plates along it, twisting a little round it. */
	static SpellArt.Painter woodLimb(int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2);
			if (t.ny < -0.5) return SpellArt.col(SpellArt.mix(SAPWOOD_DARK, WOOD_LIGHT, 0.4));
			return barkPlate(ang / (2 * Math.PI) * 6 + t.ly * 0.04, t.ly, 3.6, seed);
		};
	}

	/** The head's wood: plates of bark grained back from the snout (along z), small, knotted; paler wood beneath, along the jaw. */
	static SpellArt.Painter woodHead(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			if (t.ny < -0.55) return SpellArt.col(SpellArt.mix(SAPWOOD, SAPWOOD_DARK, 0.3 + 0.4 * SpellArt.noise(qx * 0.8, qz * 0.3, seed)));
			if (t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK) return barkPlate(qx * 0.7 + 0.4 * SpellArt.noise(qy * 0.3, qx * 0.2, seed), qy * 1.3, 3, seed);
			return barkPlate((t.ny > 0.55 ? qx : qy) * 0.6 + 0.5 * SpellArt.noise(qz * 0.15, qx * 0.3 + qy * 0.3, seed), qz, 3.4, seed);
		};
	}

	/** A knothole of light for an eye, on the outer side of its cube: jade-gold light in a ring of dark wood; the wood round it. */
	static SpellArt.Painter knotEye(SpellArt.Painter wood) {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return wood.paint(t);
			double x = t.back() - 0.5, y = t.up() - 0.5, r = Math.hypot(x, y * 1.2);
			if (r > 0.5) return wood.paint(t);
			if (r > 0.34) return SpellArt.col(SpellArt.shade(WOOD_DARK, -0.45));
			return SpellArt.glow(SpellArt.mix(VEIN_CORE, 0xD8F0A0, r * 2.4));
		};
	}

	/** A row of thorns along a strip's sides (where teeth were), pointing down ({@code upper}) or up: pale wood, tipped with jade light. */
	static SpellArt.Painter thorns(boolean upper) {
		return t -> {
			if (!t.side()) return SpellArt.CLEAR;
			double z = t.qz() / 1.6, f = Math.abs(z - Math.floor(z) - 0.5) * 2;
			double from = upper ? t.up() : 1 - t.up();
			if (from < f * 0.95) return SpellArt.CLEAR;
			if (from < 0.3) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, 0.3), 0.9);
			return SpellArt.col(SpellArt.mix(SAPWOOD_DARK, SAPWOOD, from));
		};
	}

	/** A claw of root (or a thorn of a fang): dark wood, its point (the end toward {@code tipUp}) glowing jade. */
	static SpellArt.Painter rootClaw(int tipUp) {
		return t -> {
			double toward = tipUp > 0 ? t.up() : 1 - t.up();
			if (toward > 0.74) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, (toward - 0.74) * 3));
			return SpellArt.col(SpellArt.mix(WOOD_DARK, WOOD_LIGHT, toward * 0.8));
		};
	}

	/** A vine (a whisker of one): green, a node of jade light every so often along it. */
	static SpellArt.Painter vine(int seed) {
		return t -> {
			double along = t.ly / 1.6 + seed * 0.3;
			if (Math.abs(along - Math.rint(along)) < 0.1) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, 0.3), 0.95);
			return SpellArt.col(SpellArt.mix(LEAF_DARK, LEAF, 0.4 + 0.4 * SpellArt.noise(t.ly * 0.5, t.lx + t.lz, seed)));
		};
	}

	/** The nose's front: knotted wood, two knotholes where nostrils were, dark, a faint light deep in each. */
	static SpellArt.Painter knotNose(SpellArt.Painter wood) {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return wood.paint(t);
			double x = t.across() - 0.5, y = t.up() - 0.5;
			double d = Math.hypot(Math.abs(x) - 0.27, y * 1.3);
			if (d < 0.08) return SpellArt.glow(SpellArt.mix(VEIN, WOOD_DARK, 0.5), 0.9);
			if (d < 0.16) return SpellArt.col(SpellArt.shade(WOOD_DARK, -0.45));
			return wood.paint(t);
		};
	}

	/**
	 * The Azure Dragon of the east, made of wood (the user, 2026-10-24: the great spells' beasts made of their elements, not looking like
	 * animals): its body twenty bones of living wood ("seg_0" behind the head to "seg_19", each {@link #SEG} pixels, a chain), azure-grey
	 * bark twisting along it, veins of jade light in its furrows, leaves down its spine; its head ("head", the jaw "jaw") a carving of the same
	 * wood with knotholes of light for eyes and thorns for teeth, bough antlers ("antler_l", "antler_r"), vines for whiskers ("whisker_l_0"..,
	 * "whisker_r_0"..), a mane of leaves ("mane", "mane_l", "mane_r") and a leaf beard; four legs of wood ("leg_fl".."leg_br", each with its
	 * "_low"), clawed with roots; its tail's leafy plume ("tail"). Its bones the beast's ({@link #azureDragonBeast}), so it swims and roars as
	 * that did. The whole hangs from "root" at the back of the head, the body trailing back (+z).
	 */
	static void azureDragon(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		headOfWood(m);
		// The body.
		String up = "root";
		for (int i = 0; i < SEGMENTS; i++) {
			double z = i * SEG, w = thick(i), h = w * 0.9;
			double rx = 5.4 * Math.sin(i * 0.5 + 0.2), ry = 6.0 * Math.sin(i * 0.34 + 1.4);
			SpellArt.Bone seg = m.bone("seg_" + i, up, 0, 0, z, i == 0 ? 0 : rx, i == 0 ? 0 : ry, 0);
			SpellArt.Painter wood = livingWood(0, 11);
			SpellArt.Cube core = seg.box(0, 0, z + SEG / 2, w, h * 0.74, SEG + 1.4, wood);
			SpellArt.Cube tall = seg.box(0, 0, z + SEG / 2, w * 0.7, h, SEG + 1.4, wood);
			if (i < SEGMENTS - 1) {
				core.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
				tall.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			} else {
				core.skip(SpellArt.Face.FRONT);
				tall.skip(SpellArt.Face.FRONT);
			}
			// The spine's ridge of bark, and its leaves sweeping back from it.
			seg.box(0, h / 2 + 0.3, z + SEG / 2, Math.max(0.8, w * 0.14), 0.8, SEG + 0.4, bark(20 + i, 0.04)).skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			if (i < SEGMENTS - 1) {
				double fh = 4.2 + w * 0.62, fw = 3.6 + w * 0.2, lean = (i % 2 == 0 ? 1 : -1);
				SpellArt.blade(seg, SpellArt.v3(0, h / 2 + 0.4, z + 1.4), fh, fw, -64, lean * 5, lean * 8, leaf(LEAF_DARK, LEAF_LIGHT, 1.2, i * 7));
				SpellArt.blade(seg, SpellArt.v3(lean * 0.4, h / 2 + 0.3, z + 4.6), fh * 0.62, fw * 0.75, -72, -lean * 14, -lean * 22, leaf(LEAF_DARK, LEAF, 1.25, i * 7 + 3));
			}
			// Sprays of leaves low on the flanks, every third segment; and a twig budding from the wood between.
			if (i % 3 == 1 && i < 17) {
				for (int s = -1; s <= 1; s += 2) {
					SpellArt.blade(seg, SpellArt.v3(s * w * 0.42, -h * 0.18, z + 3), 2.6 + w * 0.3, 3.2, -118, s * 18, s * 32, leaf(LEAF_DARK, LEAF, 1.1, i * 13 + s));
				}
			}
			if (i % 3 == 2 && i < 18) {
				int s = i % 2 == 0 ? 1 : -1;
				double[] from = SpellArt.v3(s * w * 0.4, h * 0.22, z + 2.5), to = SpellArt.add(from, SpellArt.v3(s * 2.6, 2.2, 3.4));
				SpellArt.limb(seg, from, to, 0.9, 0.9, woodLimb(40 + i)).density(3);
				SpellArt.blade(seg, to, 2.8, 1.8, -30, s * 40, s * 30, leaf(LEAF, LEAF_LIGHT, 1.4, 50 + i)).density(4);
			}
			up = seg.name;
		}
		legsOfWood(m);
		// The tail's plume: a fan of fronds.
		SpellArt.Bone tail = m.bone("tail", "seg_" + (SEGMENTS - 1), 0, 0, SEGMENTS * SEG);
		double[][] fronds = {{-90, 0, 0, 17, 6.5}, {-70, 16, 40, 15, 5.8}, {-70, -16, -40, 15, 5.8}, {-104, 22, 70, 13, 5.2}, {-104, -22, -70, 13, 5.2},
				{-60, 0, 0, 12, 5}, {-122, 0, 0, 12, 5}, {-86, 34, 88, 11, 4.4}, {-86, -34, -88, 11, 4.4}};
		for (int k = 0; k < fronds.length; k++) {
			double[] f = fronds[k];
			SpellArt.blade(tail, SpellArt.v3(0, 0, SEGMENTS * SEG - 1), f[3], f[4], f[0], f[1], f[2], leaf(LEAF_DARK, LEAF_LIGHT, 1.35, 90 + k)).density(2.5);
		}
		swim(m);
		roar(m);
	}

	static void headOfWood(SpellArt.Model m) {
		SpellArt.Bone head = m.bone("head", "root", 0, 0, 0);
		SpellArt.Painter wood = woodHead(31), roof = mouthRoof(wood);
		// The skull, its crown, the nape where it meets the neck; the great jowls (as the beast's, carved of wood).
		head.box(0, 2.0, -6.6, 10.4, 8, 11, wood);
		head.box(0, 6.5, -6.4, 7.8, 1.3, 9, wood);
		head.box(0, 1.2, -1.4, 9.2, 7.8, 3, wood).skip(SpellArt.Face.BACK);
		head.box(0, -0.2, -9.6, 12.6, 4.8, 6.2, wood);
		head.box(0, -1.7, -10.6, 10.2, 1.6, 5.4, wood);
		// The snout, its bridge, the broad nose (knotholes for nostrils), the lip beneath it.
		head.box(0, 2.0, -18, 8, 4.6, 12.4, roof);
		head.box(0, 4.6, -17.0, 4.6, 1.0, 8.6, wood);
		head.box(0, 4.2, -24.4, 8.6, 4.8, 4.2, knotNose(wood));
		head.box(0, 6.9, -23.9, 6.2, 1.0, 3.4, wood);
		head.box(0, 0.7, -24.6, 7.2, 2.2, 3.6, roof);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 4.3, 4.6, -25.0, 1.4, 2.8, 2.6, wood);
			// The brows: heavy ridges of wood over the eyes, flaring back into tufts of leaves.
			SpellArt.limb(head, SpellArt.v3(s * 2.0, 6.6, -14.0), SpellArt.v3(s * 5.4, 7.6, -8.4), 2.2, 1.9, wood);
			double[] brow = SpellArt.v3(s * 5.2, 7.9, -8.6);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, brow, 6.2 - k * 1.2, 2.6, -48 - k * 22, s * (18 + k * 12), s * (24 + k * 14), leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 40 + k * 2 + s)).density(3);
			}
			head.box(s * 5.35, 4.4, -11.2, 1.0, 2.2, 3.0, knotEye(wood)).density(4);
			// Thorns where its fangs were (no teeth: a wooden mouth, the light within it).
			SpellArt.limb(head, SpellArt.v3(s * 3.1, -0.3, -24.0), SpellArt.v3(s * 3.0, -3.2, -23.4), 1.0, 1.0, rootClaw(-1)).density(4);
			SpellArt.limb(head, SpellArt.v3(s * 3.7, -0.3, -15.4), SpellArt.v3(s * 3.7, -2.3, -15.2), 0.7, 0.7, rootClaw(-1)).density(4);
			// Cheek fins: leaves fanned back from the jowls.
			for (int k = 0; k < 4; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 6.0, 1.6 - k * 1.3, -8.4 + k * 0.9), 7.4 - k * 1.1, 3.4, -72 - k * 14, s * (34 + k * 6), s * (30 + k * 12),
						leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 50 + k * 2 + s)).density(2.5);
			}
		}
		// The jaw, hinged under the ear, a little open: wood, a thorn either side of its front, the light within its mouth.
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, -1.0, -9.6, 12, 0, 0);
		jaw.box(0, -2.6, -16.6, 7.4, 2.4, 14, jawSkin(wood));
		jaw.box(0, -3.8, -22.6, 6.2, 1.6, 3, wood);
		jaw.box(0, -1.25, -16.4, 3.2, 0.5, 11, t -> SpellArt.glow(SpellArt.mix(0x3CE0B0, VEIN_CORE, t.back() * 0.3 + SpellArt.hash(t.x, t.y, 3) * 0.2), 0.9));
		for (int s = -1; s <= 1; s += 2) SpellArt.limb(jaw, SpellArt.v3(s * 2.7, -1.6, -22.6), SpellArt.v3(s * 2.8, 0.7, -22.9), 0.85, 0.85, rootClaw(1)).density(4);
		// The beard: leaves hanging from the chin and streaming back.
		for (int k = -2; k <= 2; k++) {
			SpellArt.blade(jaw, SpellArt.v3(k * 1.2, -4.4, -21.6 + Math.abs(k) * 1.6), 8.5 - Math.abs(k) * 1.4, 3.4, -140 + Math.abs(k) * 6, k * 9, k * 8,
					leaf(LEAF_DARK, LEAF_LIGHT, 1.1, 60 + k)).density(2.5);
		}
		// Whiskers: vines from the nose, curving back past the head.
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r", up = "head";
			double z = -25.2, x = s * 4.9, y = 3.8;
			double[][] rest = {{-20, s * 32, 0}, {-8, -s * 6, 0}, {12, -s * 16, 0}, {22, -s * 16, 0}};
			for (int k = 0; k < rest.length; k++) {
				SpellArt.Bone wb = m.bone("whisker_" + side + "_" + k, up, x, y, z, rest[k][0], rest[k][1], rest[k][2]);
				double thin = 0.8 - k * 0.1;
				wb.box(x, y, z + 3.3, thin, thin, 6.8, vine(k + (s > 0 ? 0 : 4))).density(3);
				if (k == 1 || k == 3) SpellArt.blade(wb, SpellArt.v3(x, y, z + 5), 2.4, 1.5, -60, s * 30, s * 20, leaf(LEAF, LEAF_LIGHT, 1.4, 120 + k + (s > 0 ? 0 : 4))).density(4);
				up = wb.name;
				z += 6.6;
			}
		}
		// Antlers: boughs, branching, budding jade.
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone a = m.bone(s > 0 ? "antler_l" : "antler_r", "head", s * 2.8, 7.0, -5.4);
			double[] p0 = SpellArt.v3(s * 2.8, 6.6, -5.4), p1 = SpellArt.add(p0, SpellArt.v3(s * 1.0, 4.4, 1.6)), p2 = SpellArt.add(p1, SpellArt.v3(s * 1.4, 4.0, 3.0)),
					p3 = SpellArt.add(p2, SpellArt.v3(s * 1.2, 3.2, 4.0)), p4 = SpellArt.add(p3, SpellArt.v3(s * 0.4, 1.6, 3.6));
			SpellArt.Painter bough = woodLimb(70 + s);
			a.box(p0[0], p0[1] + 0.4, p0[2], 2.6, 1.0, 2.6, bough); // the burr
			SpellArt.limb(a, p0, p1, 1.9, 1.9, bough);
			SpellArt.limb(a, p1, p2, 1.6, 1.6, bough);
			SpellArt.limb(a, p2, p3, 1.25, 1.25, bough);
			SpellArt.limb(a, p3, p4, 0.9, 0.9, bough);
			double[] t1 = SpellArt.add(p1, SpellArt.v3(s * 0.4, 3.8, -2.2)), t2 = SpellArt.add(p2, SpellArt.v3(s * 2.4, 3.0, -0.8)), t3 = SpellArt.add(p2, SpellArt.v3(-s * 0.8, 3.2, 1.0)),
					t4 = SpellArt.add(p3, SpellArt.v3(s * 1.6, 2.6, 0.2));
			SpellArt.limb(a, SpellArt.add(p1, SpellArt.v3(0, 0.3, 0)), t1, 1.05, 1.05, bough);
			SpellArt.limb(a, SpellArt.add(p2, SpellArt.v3(0, 0.2, 0)), t2, 0.95, 0.95, bough);
			SpellArt.limb(a, SpellArt.add(p2, SpellArt.v3(0, 0.2, 0)), t3, 0.85, 0.85, bough);
			SpellArt.limb(a, SpellArt.add(p3, SpellArt.v3(0, 0.1, 0)), t4, 0.75, 0.75, bough);
			double[][] tips = {t1, t2, t3, t4, p4};
			for (int k = 0; k < tips.length; k++) {
				SpellArt.blade(a, tips[k], 2.6, 1.7, -20 - k * 10, s * (30 + k * 20), s * 30, leaf(LEAF, LEAF_LIGHT, 1.4, 80 + k)).density(4);
				SpellArt.blade(a, tips[k], 2.2, 1.5, 40, s * (-40 + k * 15), -s * 25, leaf(LEAF, LEAF_LIGHT, 1.4, 86 + k)).density(4);
			}
		}
		// The mane: leaves and vines streaming back from the head over the neck.
		SpellArt.Bone mane = m.bone("mane", "head", 0, 6.8, -4);
		for (int k = 0; k < 5; k++) {
			SpellArt.blade(mane, SpellArt.v3(0, 7.0, -9 + k * 2.2), 10.5 - k * 0.5, 4.4, -60 - k * 5, 0, 0, leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 100 + k));
		}
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone side = m.bone(s > 0 ? "mane_l" : "mane_r", "head", s * 4, 4.5, -3);
			for (int k = 0; k < 5; k++) {
				SpellArt.blade(side, SpellArt.v3(s * (3.6 + k * 0.5), 6.2 - k * 1.6, -6.6 + k * 1.4), 12.5 - k * 1.3, 4.8, -70 - k * 7, s * (16 + k * 6), s * (26 + k * 11),
						leaf(LEAF_DARK, LEAF_LIGHT, 1.15, 110 + k * 3 + s));
			}
			for (int k = 0; k < 2; k++) {
				SpellArt.limb(side, SpellArt.v3(s * (3 + k), 5.2 - k * 2, -2), SpellArt.v3(s * (7.5 + k * 1.5), 8.5 - k * 3.5, 11 + k * 2), 0.6, 0.6, vine(10 + k)).density(3);
			}
		}
	}

	static void legsOfWood(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			boolean front = k < 2;
			int segI = front ? 2 : 10;
			double z0 = segI * SEG + 3.5, w = thick(segI), big = front ? 1 : 1.12;
			SpellArt.Painter wood = livingWood(0, 200 + k), limb = woodLimb(200 + k);
			double[] shoulder = SpellArt.v3(s * w * 0.4, -w * 0.22, z0), elbow = SpellArt.add(shoulder, SpellArt.v3(s * 2.6 * big, -5.2 * big, front ? 1.8 : 2.6)),
					wrist = SpellArt.add(elbow, SpellArt.v3(s * 0.4, -4.4 * big, front ? -2.6 : -1.6));
			SpellArt.Bone leg = m.bone(names[k], "seg_" + segI, shoulder[0], shoulder[1], shoulder[2], front ? -36 : 48, 0, -s * 10);
			leg.box(shoulder[0] * 1.05, shoulder[1] + 0.4, shoulder[2], 3.6 * big, 4.2 * big, 4.6 * big, wood);
			SpellArt.limb(leg, shoulder, elbow, 2.9 * big, 3.1 * big, limb);
			// Leaves sprouting back from the elbow.
			for (int j = 0; j < 3; j++) {
				SpellArt.blade(leg, elbow, 5.2 - j * 0.9, 2.6, -64 - j * 18, s * (12 + j * 10), s * (20 + j * 12), leaf(LEAF_DARK, LEAF_LIGHT, 1.1, 300 + k * 5 + j)).density(2.5);
			}
			SpellArt.Bone low = m.bone(names[k] + "_low", names[k], elbow[0], elbow[1], elbow[2], front ? 30 : 24, 0, 0);
			SpellArt.limb(low, elbow, wrist, 2.3 * big, 2.4 * big, limb);
			// The foot: a burl of wood and four roots for claws, three forward, one behind.
			double[] palm = SpellArt.add(wrist, SpellArt.v3(0, -0.4, -0.6));
			low.box(palm[0], palm[1], palm[2], 3.2 * big, 1.3, 3.2 * big, limb);
			for (int j = -1; j <= 1; j++) {
				double[] knuckle = SpellArt.add(palm, SpellArt.v3(s * j * 1.05 * big, -0.2, -1.4 * big)), tip = SpellArt.add(knuckle, SpellArt.v3(s * j * 0.7, -0.9, -2.0)),
						point = SpellArt.add(tip, SpellArt.v3(s * j * 0.2, -1.6, 0.2));
				SpellArt.limb(low, knuckle, tip, 0.8, 0.8, limb).density(3);
				SpellArt.limb(low, tip, point, 0.6, 0.6, rootClaw(1)).density(4);
			}
			double[] heel = SpellArt.add(palm, SpellArt.v3(0, -0.2, 1.4)), spur = SpellArt.add(heel, SpellArt.v3(0, -1.6, 1.0));
			SpellArt.limb(low, heel, spur, 0.6, 0.6, rootClaw(1)).density(4);
		}
	}

	/**
	 * The Azure Dragon of the east, living wood and jade: a serpent's body in twenty bones ("seg_0" behind the head to "seg_19", each
	 * {@link #SEG} pixels, a chain), azure-scaled with veins of light, leaf fins down its spine; its head ("head", the jaw "jaw") with
	 * bough antlers ("antler_l", "antler_r"), whiskers ("whisker_l_0".."_2", "whisker_r_0".."_2"), a mane of leaves ("mane", "mane_l",
	 * "mane_r") and a leaf beard; four clawed legs ("leg_fl", "leg_fr", "leg_bl", "leg_br", each with its "_low"); its tail's leafy plume
	 * ("tail"). The whole hangs from "root" at the back of the head, the body trailing back (+z). Stored (2026-10-24): the spell's dragon
	 * is now made of wood, {@link #azureDragon}; this, the true beast, is kept for later (SpellArt --stored).
	 */
	static void azureDragonBeast(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		head(m);
		// The body.
		String up = "root";
		for (int i = 0; i < SEGMENTS; i++) {
			double z = i * SEG, w = thick(i), h = w * 0.9;
			double rx = 5.4 * Math.sin(i * 0.5 + 0.2), ry = 6.0 * Math.sin(i * 0.34 + 1.4);
			SpellArt.Bone seg = m.bone("seg_" + i, up, 0, 0, z, i == 0 ? 0 : rx, i == 0 ? 0 : ry, 0);
			SpellArt.Painter skin = hide(0, 2.1, 18, 11);
			SpellArt.Cube core = seg.box(0, 0, z + SEG / 2, w, h * 0.74, SEG + 1.4, skin);
			SpellArt.Cube tall = seg.box(0, 0, z + SEG / 2, w * 0.7, h, SEG + 1.4, skin);
			if (i < SEGMENTS - 1) {
				core.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
				tall.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			} else {
				core.skip(SpellArt.Face.FRONT);
				tall.skip(SpellArt.Face.FRONT);
			}
			// The spine's ridge and its leaf fin, sweeping back.
			seg.box(0, h / 2 + 0.3, z + SEG / 2, Math.max(0.8, w * 0.14), 0.8, SEG + 0.4, bark(20 + i, 0.04)).skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			if (i < SEGMENTS - 1) {
				// The crest: long leaves swept far back, each over the next, leaning a little to either side by turns, a lesser one beneath.
				double fh = 4.2 + w * 0.62, fw = 3.6 + w * 0.2, lean = (i % 2 == 0 ? 1 : -1);
				SpellArt.blade(seg, SpellArt.v3(0, h / 2 + 0.4, z + 1.4), fh, fw, -64, lean * 5, lean * 8, leaf(LEAF_DARK, LEAF_LIGHT, 1.2, i * 7));
				SpellArt.blade(seg, SpellArt.v3(lean * 0.4, h / 2 + 0.3, z + 4.6), fh * 0.62, fw * 0.75, -72, -lean * 14, -lean * 22, leaf(LEAF_DARK, LEAF, 1.25, i * 7 + 3));
			}
			// Tufts of leaves low on the flanks, every third segment.
			if (i % 3 == 1 && i < 17) {
				for (int s = -1; s <= 1; s += 2) {
					SpellArt.blade(seg, SpellArt.v3(s * w * 0.42, -h * 0.18, z + 3), 2.6 + w * 0.3, 3.2, -118, s * 18, s * 32, leaf(LEAF_DARK, LEAF, 1.1, i * 13 + s));
				}
			}
			up = seg.name;
		}
		legs(m);
		// The tail's plume: a fan of fronds.
		SpellArt.Bone tail = m.bone("tail", "seg_" + (SEGMENTS - 1), 0, 0, SEGMENTS * SEG);
		double[][] fronds = {{-90, 0, 0, 17, 6.5}, {-70, 16, 40, 15, 5.8}, {-70, -16, -40, 15, 5.8}, {-104, 22, 70, 13, 5.2}, {-104, -22, -70, 13, 5.2},
				{-60, 0, 0, 12, 5}, {-122, 0, 0, 12, 5}, {-86, 34, 88, 11, 4.4}, {-86, -34, -88, 11, 4.4}};
		for (int k = 0; k < fronds.length; k++) {
			double[] f = fronds[k];
			SpellArt.blade(tail, SpellArt.v3(0, 0, SEGMENTS * SEG - 1), f[3], f[4], f[0], f[1], f[2], leaf(LEAF_DARK, LEAF_LIGHT, 1.35, 90 + k)).density(2.5);
		}
		swim(m);
		roar(m);
	}

	/**
	 * The head's hide, by which way a face looks: azure scales on top darkening along the brow, jade down the sides lightening toward the
	 * lips, pale celadon beneath; fine scales everywhere, a lip line of light along the mouth's edge.
	 */
	static SpellArt.Painter headHide(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			if (t.ny < -0.55) {
				double band = qz / 1.3 - Math.floor(qz / 1.3);
				return SpellArt.col(band < 0.18 ? SpellArt.mix(BELLY, BELLY_SEAM, 0.6) : SpellArt.shade(BELLY, -0.06 * band));
			}
			double height = SpellArt.clamp01((qy + 2.5) / 9.5);
			int base = t.ny > 0.55 ? SpellArt.mix(DEEP, AZURE, 0.55 + 0.3 * SpellArt.noise(qx * 0.5, qz * 0.5, seed)) : SpellArt.ramp(1 - height, AZURE, JADE, JADE_LIGHT);
			// Fine scales, rows along the head, the rows set off from each other.
			double a = t.ny > 0.55 ? qx : qy, b = qz;
			double rowF = b / 1.15;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = a / 1.1 + Math.floorMod(row, 2) * 0.5;
			int colI = (int) Math.floor(colF);
			double u = colF - colI, rim = 0.3 + 0.7 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			double h = SpellArt.hash(colI, row, seed) - 0.5;
			int c = v > rim ? SpellArt.shade(base, -0.3) : rim - v < 0.15 ? SpellArt.shade(base, -0.4) : SpellArt.shade(base, -0.12 + 0.3 * v / rim + h * 0.1);
			return SpellArt.col(c);
		};
	}

	static void head(SpellArt.Model m) {
		SpellArt.Bone head = m.bone("head", "root", 0, 0, 0);
		SpellArt.Painter skin = headHide(31), roof = mouthRoof(skin);
		// The skull, its crown, the nape where it meets the neck; the great jowls.
		head.box(0, 2.0, -6.6, 10.4, 8, 11, skin);
		head.box(0, 6.5, -6.4, 7.8, 1.3, 9, skin);
		head.box(0, 1.2, -1.4, 9.2, 7.8, 3, skin).skip(SpellArt.Face.BACK);
		head.box(0, -0.2, -9.6, 12.6, 4.8, 6.2, skin);
		head.box(0, -1.7, -10.6, 10.2, 1.6, 5.4, skin);
		// The snout, its bridge, the broad upturned nose and its flared nostrils, the lip beneath it.
		head.box(0, 2.0, -18, 8, 4.6, 12.4, roof);
		head.box(0, 4.6, -17.0, 4.6, 1.0, 8.6, skin);
		head.box(0, 4.2, -24.4, 8.6, 4.8, 4.2, nose(skin));
		head.box(0, 6.9, -23.9, 6.2, 1.0, 3.4, skin);
		head.box(0, 0.7, -24.6, 7.2, 2.2, 3.6, roof);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 4.3, 4.6, -25.0, 1.4, 2.8, 2.6, nostril(s, skin));
			// The brows: heavy ridges over the eyes, flaring back into tufts of leaves like a flame's.
			SpellArt.limb(head, SpellArt.v3(s * 2.0, 6.6, -14.0), SpellArt.v3(s * 5.4, 7.6, -8.4), 2.2, 1.9, skin);
			double[] brow = SpellArt.v3(s * 5.2, 7.9, -8.6);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, brow, 6.2 - k * 1.2, 2.6, -48 - k * 22, s * (18 + k * 12), s * (24 + k * 14), leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 40 + k * 2 + s)).density(3);
			}
			head.box(s * 5.35, 4.4, -11.2, 1.0, 2.2, 3.0, eye()).density(4);
			// Teeth, upper; the fangs.
			head.box(s * 3.75, -0.95, -18.4, 0.4, 1.4, 11.6, teeth(true)).density(4);
			SpellArt.limb(head, SpellArt.v3(s * 3.1, -0.3, -24.0), SpellArt.v3(s * 3.0, -3.2, -23.4), 1.0, 1.0, claw(-1)).density(4);
			SpellArt.limb(head, SpellArt.v3(s * 3.7, -0.3, -15.4), SpellArt.v3(s * 3.7, -2.3, -15.2), 0.7, 0.7, claw(-1)).density(4);
			// Cheek fins: leaves fanned back from the jowls, like a flame's tongues.
			for (int k = 0; k < 4; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 6.0, 1.6 - k * 1.3, -8.4 + k * 0.9), 7.4 - k * 1.1, 3.4, -72 - k * 14, s * (34 + k * 6), s * (30 + k * 12),
						leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 50 + k * 2 + s)).density(2.5);
			}
		}
		// The jaw, hinged under the ear, a little open.
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, -1.0, -9.6, 12, 0, 0);
		jaw.box(0, -2.6, -16.6, 7.4, 2.4, 14, jawSkin(skin));
		jaw.box(0, -3.8, -22.6, 6.2, 1.6, 3, skin);
		jaw.box(0, -1.25, -16.4, 3.2, 0.5, 11, t -> SpellArt.glow(SpellArt.mix(0x3CE0B0, VEIN_CORE, t.back() * 0.3 + SpellArt.hash(t.x, t.y, 3) * 0.2), 0.9));
		for (int s = -1; s <= 1; s += 2) {
			jaw.box(s * 3.35, -0.95, -17.0, 0.4, 1.1, 11.6, teeth(false)).density(4);
			SpellArt.limb(jaw, SpellArt.v3(s * 2.7, -1.6, -22.6), SpellArt.v3(s * 2.8, 0.7, -22.9), 0.85, 0.85, claw(1)).density(4);
		}
		// The beard: leaves hanging from the chin and streaming back.
		for (int k = -2; k <= 2; k++) {
			SpellArt.blade(jaw, SpellArt.v3(k * 1.2, -4.4, -21.6 + Math.abs(k) * 1.6), 8.5 - Math.abs(k) * 1.4, 3.4, -140 + Math.abs(k) * 6, k * 9, k * 8,
					leaf(LEAF_DARK, LEAF_LIGHT, 1.1, 60 + k)).density(2.5);
		}
		// Whiskers: long barbels from the nostrils, curving back past the head.
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r", up = "head";
			double z = -25.2, x = s * 4.9, y = 3.8;
			double[][] rest = {{-20, s * 32, 0}, {-8, -s * 6, 0}, {12, -s * 16, 0}, {22, -s * 16, 0}};
			for (int k = 0; k < rest.length; k++) {
				SpellArt.Bone wb = m.bone("whisker_" + side + "_" + k, up, x, y, z, rest[k][0], rest[k][1], rest[k][2]);
				double thin = 0.72 - k * 0.1;
				int kk = k;
				wb.box(x, y, z + 3.3, thin, thin, 6.8, t -> SpellArt.glow(SpellArt.mix(PALE, VEIN, (kk + t.back()) / 4), 0.97 - (kk + t.back()) * 0.12)).density(3);
				up = wb.name;
				z += 6.6;
			}
		}
		// Antlers: boughs, branching, budding jade.
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone a = m.bone(s > 0 ? "antler_l" : "antler_r", "head", s * 2.8, 7.0, -5.4);
			double[] p0 = SpellArt.v3(s * 2.8, 6.6, -5.4), p1 = SpellArt.add(p0, SpellArt.v3(s * 1.0, 4.4, 1.6)), p2 = SpellArt.add(p1, SpellArt.v3(s * 1.4, 4.0, 3.0)),
					p3 = SpellArt.add(p2, SpellArt.v3(s * 1.2, 3.2, 4.0)), p4 = SpellArt.add(p3, SpellArt.v3(s * 0.4, 1.6, 3.6));
			SpellArt.Painter wood = bark(70 + s, 0.05);
			a.box(p0[0], p0[1] + 0.4, p0[2], 2.6, 1.0, 2.6, wood); // the burr
			SpellArt.limb(a, p0, p1, 1.9, 1.9, wood);
			SpellArt.limb(a, p1, p2, 1.6, 1.6, wood);
			SpellArt.limb(a, p2, p3, 1.25, 1.25, wood);
			SpellArt.limb(a, p3, p4, 0.9, 0.9, wood);
			double[] t1 = SpellArt.add(p1, SpellArt.v3(s * 0.4, 3.8, -2.2)), t2 = SpellArt.add(p2, SpellArt.v3(s * 2.4, 3.0, -0.8)), t3 = SpellArt.add(p2, SpellArt.v3(-s * 0.8, 3.2, 1.0)),
					t4 = SpellArt.add(p3, SpellArt.v3(s * 1.6, 2.6, 0.2));
			SpellArt.limb(a, SpellArt.add(p1, SpellArt.v3(0, 0.3, 0)), t1, 1.05, 1.05, wood);
			SpellArt.limb(a, SpellArt.add(p2, SpellArt.v3(0, 0.2, 0)), t2, 0.95, 0.95, wood);
			SpellArt.limb(a, SpellArt.add(p2, SpellArt.v3(0, 0.2, 0)), t3, 0.85, 0.85, wood);
			SpellArt.limb(a, SpellArt.add(p3, SpellArt.v3(0, 0.1, 0)), t4, 0.75, 0.75, wood);
			double[][] tips = {t1, t2, t3, t4, p4};
			for (int k = 0; k < tips.length; k++) {
				SpellArt.blade(a, tips[k], 2.6, 1.7, -20 - k * 10, s * (30 + k * 20), s * 30, leaf(LEAF, LEAF_LIGHT, 1.4, 80 + k)).density(4);
				SpellArt.blade(a, tips[k], 2.2, 1.5, 40, s * (-40 + k * 15), -s * 25, leaf(LEAF, LEAF_LIGHT, 1.4, 86 + k)).density(4);
			}
		}
		// The mane: leaves and vines streaming back from the head over the neck.
		SpellArt.Bone mane = m.bone("mane", "head", 0, 6.8, -4);
		for (int k = 0; k < 5; k++) {
			SpellArt.blade(mane, SpellArt.v3(0, 7.0, -9 + k * 2.2), 10.5 - k * 0.5, 4.4, -60 - k * 5, 0, 0, leaf(LEAF_DARK, LEAF_LIGHT, 1.2, 100 + k));
		}
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone side = m.bone(s > 0 ? "mane_l" : "mane_r", "head", s * 4, 4.5, -3);
			for (int k = 0; k < 5; k++) {
				SpellArt.blade(side, SpellArt.v3(s * (3.6 + k * 0.5), 6.2 - k * 1.6, -6.6 + k * 1.4), 12.5 - k * 1.3, 4.8, -70 - k * 7, s * (16 + k * 6), s * (26 + k * 11),
						leaf(LEAF_DARK, LEAF_LIGHT, 1.15, 110 + k * 3 + s));
			}
			// Vines trailing back, beaded with light.
			for (int k = 0; k < 2; k++) {
				SpellArt.limb(side, SpellArt.v3(s * (3 + k), 5.2 - k * 2, -2), SpellArt.v3(s * (7.5 + k * 1.5), 8.5 - k * 3.5, 11 + k * 2), 0.6, 0.6, t -> {
					double v = t.up();
					if (Math.abs(v * 8 - Math.rint(v * 8)) < 0.12) return SpellArt.glow(VEIN);
					return SpellArt.col(SpellArt.mix(LEAF_DARK, MOSS, v));
				}).density(3);
			}
		}
	}

	static SpellArt.Painter mouthRoof(SpellArt.Painter skin) {
		return t -> t.f == SpellArt.Face.DOWN ? SpellArt.glow(SpellArt.mix(0x0E5A4C, 0x2EC79A, SpellArt.noise(t.lx * 2, t.lz * 2, 5) * 0.6), 0.9) : skin.paint(t);
	}

	static SpellArt.Painter jawSkin(SpellArt.Painter skin) {
		return t -> t.f == SpellArt.Face.UP ? SpellArt.glow(SpellArt.mix(0x0E5A4C, 0x2EC79A, SpellArt.noise(t.lx * 2, t.lz * 2, 6) * 0.6), 0.9) : skin.paint(t);
	}

	/** The nose's front: a dark cleft down its middle, ridged; its top: the scales. */
	static SpellArt.Painter nose(SpellArt.Painter skin) {
		return t -> {
			if (t.f == SpellArt.Face.FRONT) {
				double x = t.across() - 0.5, y = t.up();
				if (Math.abs(x) < 0.04 && y > 0.25) return SpellArt.col(0x0C2A2C);
				if (y > 0.8) return SpellArt.col(SpellArt.mix(AZURE, JADE, 0.3));
				return SpellArt.col(SpellArt.shade(JADE, -0.1 + 0.25 * y + (Math.abs(y * 5 - Math.rint(y * 5)) < 0.12 ? -0.25 : 0)));
			}
			return skin.paint(t);
		};
	}

	/** A nostril's flare, its opening dark on the outer side's front. */
	static SpellArt.Painter nostril(int s, SpellArt.Painter skin) {
		return t -> {
			if (t.f == SpellArt.Face.FRONT && Math.hypot((t.across() - (s > 0 ? 0.6 : 0.4)) * 1.3, t.up() - 0.5) < 0.32) return SpellArt.col(0x061614);
			return skin.paint(t);
		};
	}

	/** A dragon's eye on the outer side of its cube: gold, burning, slit, ringed dark. */
	static SpellArt.Painter eye() {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return SpellArt.col(0x0A2228);
			double x = t.back() - 0.5, y = t.up() - 0.5, r = Math.hypot(x, y * 1.25);
			if (r > 0.48) return SpellArt.col(0x0A2228);
			if (Math.abs(x) < 0.07 && Math.abs(y) < 0.36) return SpellArt.col(0x120804);
			if (Math.hypot(x + 0.2, y - 0.2) < 0.08) return SpellArt.glow(0xFFFFF0);
			return SpellArt.glow(SpellArt.mix(0xFFF2A0, 0xF0A020, r * 2));
		};
	}

	/** A row of teeth along a strip's sides, pointing down ({@code upper}) or up, ivory on nothing. */
	static SpellArt.Painter teeth(boolean upper) {
		return t -> {
			if (!t.side()) return SpellArt.CLEAR;
			double z = t.qz() / 1.25, f = Math.abs(z - Math.floor(z) - 0.5) * 2; // 0 at a tooth's middle, 1 between teeth
			double from = upper ? t.up() : 1 - t.up(); // 1 at the gum
			if (from < f * 0.9) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(IVORY, SpellArt.shade(IVORY, -0.3), from));
		};
	}

	static void legs(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			boolean front = k < 2;
			int segI = front ? 2 : 10;
			double z0 = segI * SEG + 3.5, w = thick(segI), big = front ? 1 : 1.12;
			SpellArt.Painter skin = hide(0, 1.6, 14, 200 + k), scaled = hideLimb(200 + k);
			double[] shoulder = SpellArt.v3(s * w * 0.4, -w * 0.22, z0), elbow = SpellArt.add(shoulder, SpellArt.v3(s * 2.6 * big, -5.2 * big, front ? 1.8 : 2.6)),
					wrist = SpellArt.add(elbow, SpellArt.v3(s * 0.4, -4.4 * big, front ? -2.6 : -1.6));
			// The forelegs reach forward, the hind legs trail back, as a dragon holds them in flight.
			SpellArt.Bone leg = m.bone(names[k], "seg_" + segI, shoulder[0], shoulder[1], shoulder[2], front ? -36 : 48, 0, -s * 10);
			leg.box(shoulder[0] * 1.05, shoulder[1] + 0.4, shoulder[2], 3.6 * big, 4.2 * big, 4.6 * big, skin);
			SpellArt.limb(leg, shoulder, elbow, 2.9 * big, 3.1 * big, scaled);
			// Elbow tufts: leaves streaming back like a flame's tongues.
			for (int j = 0; j < 3; j++) {
				SpellArt.blade(leg, elbow, 5.2 - j * 0.9, 2.6, -64 - j * 18, s * (12 + j * 10), s * (20 + j * 12), leaf(LEAF_DARK, LEAF_LIGHT, 1.1, 300 + k * 5 + j)).density(2.5);
			}
			SpellArt.Bone low = m.bone(names[k] + "_low", names[k], elbow[0], elbow[1], elbow[2], front ? 30 : 24, 0, 0);
			SpellArt.limb(low, elbow, wrist, 2.3 * big, 2.4 * big, scaled);
			// The foot: a palm and four claws, three forward, one behind.
			double[] palm = SpellArt.add(wrist, SpellArt.v3(0, -0.4, -0.6));
			low.box(palm[0], palm[1], palm[2], 3.2 * big, 1.3, 3.2 * big, scaled);
			for (int j = -1; j <= 1; j++) {
				double[] knuckle = SpellArt.add(palm, SpellArt.v3(s * j * 1.05 * big, -0.2, -1.4 * big)), tip = SpellArt.add(knuckle, SpellArt.v3(s * j * 0.7, -0.9, -2.0)),
						point = SpellArt.add(tip, SpellArt.v3(s * j * 0.2, -1.6, 0.2));
				SpellArt.limb(low, knuckle, tip, 0.8, 0.8, scaled).density(3);
				SpellArt.limb(low, tip, point, 0.6, 0.6, claw(1)).density(4);
			}
			double[] heel = SpellArt.add(palm, SpellArt.v3(0, -0.2, 1.4)), spur = SpellArt.add(heel, SpellArt.v3(0, -1.6, 1.0));
			SpellArt.limb(low, heel, spur, 0.6, 0.6, claw(1)).density(4);
		}
	}

	/** A limb's scales: small, azure, its underside paler. */
	static SpellArt.Painter hideLimb(int seed) {
		return t -> {
			int row = (int) Math.floor(t.ly / 1.1), col = (int) Math.floor((t.lx + t.lz) / 1.2);
			int c = SpellArt.mix(AZURE, JADE, 0.4 + SpellArt.hash(row, col, seed) * 0.4);
			double v = t.ly / 1.1 - row;
			c = v < 0.18 ? SpellArt.shade(c, -0.35) : SpellArt.shade(c, 0.1 * v + ((row + col) % 2 == 0 ? -0.05 : 0.03));
			return SpellArt.col(c);
		};
	}

	/** The swim loop: waves running down the body from the head, legs paddling, the mane and whiskers streaming, the plume swaying. */
	static void swim(SpellArt.Model m) {
		double T = 3.2;
		SpellArt.Anim a = m.anim("swim", T, "true");
		int steps = 16;
		for (int i = 0; i < SEGMENTS; i++) {
			int ii = i;
			double env = i == 0 ? 0.35 : i < 3 ? 0.75 : 1 + i * 0.015;
			// A serpent's S-curves (the user's ask, 2026-10-25: the beasts too rigid): side to side as much as up and down, each a little behind.
			a.sample("seg_" + i, "rotation", 0, T, steps, t -> {
				double ph = 2 * Math.PI * t / T - ii * 0.5;
				return new double[] {8 * env * Math.sin(ph), 9 * env * Math.sin(ph + 1.3), 2.5 * env * Math.sin(ph + 2.2)};
			});
		}
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {4.5 * Math.sin(2 * Math.PI * t / T + 0.6), 6 * Math.sin(2 * Math.PI * t / T + 1.9), 3 * Math.sin(2 * Math.PI * t / T + 1.1)});
		a.sample("jaw", "rotation", 0, T, 8, t -> new double[] {4 + 3 * Math.sin(2 * Math.PI * t / T), 0, 0});
		for (String side : new String[] {"l", "r"}) {
			int s = side.equals("l") ? 1 : -1;
			for (int k = 0; k < 4; k++) {
				int kk = k;
				a.sample("whisker_" + side + "_" + k, "rotation", 0, T, steps, t -> {
					double ph = 2 * Math.PI * t / T - kk * 0.9;
					return new double[] {(8 + kk * 4) * Math.sin(ph), s * (7 + kk * 4) * Math.sin(ph + 0.7), 0};
				});
			}
			a.sample("mane_" + side, "rotation", 0, T, steps, t -> new double[] {-5 * Math.sin(2 * Math.PI * t / T - 0.8), 0, s * 4 * Math.sin(2 * Math.PI * t / T - 0.4)});
		}
		a.sample("mane", "rotation", 0, T, steps, t -> new double[] {-6 * Math.sin(2 * Math.PI * t / T - 0.6), 0, 0});
		String[] legs = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[] phase = {0, Math.PI, Math.PI, 0};
		for (int k = 0; k < 4; k++) {
			double p = phase[k];
			a.sample(legs[k], "rotation", 0, T, steps, t -> new double[] {26 * Math.sin(2 * Math.PI * t / T + p), 0, 0});
			a.sample(legs[k] + "_low", "rotation", 0, T, steps, t -> new double[] {-22 * Math.sin(2 * Math.PI * t / T + p - 1.0), 0, 0});
		}
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {12 * Math.sin(2 * Math.PI * t / T - SEGMENTS * 0.5 - 0.5), 20 * Math.sin(2 * Math.PI * t / T - SEGMENTS * 0.5 + 0.8), 0});
	}

	/** The roar: the head rears and the jaw gapes, the neck arching under it, whiskers and mane flung back; then it settles. */
	static void roar(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("roar", 1.6, null);
		double[] t = {0, 0.35, 0.5, 0.65, 0.8, 0.95, 1.1, 1.6};
		double[] rear = {0, 1, 1, 1, 1, 1, 1, 0};
		double[] shake = {0, 0, 4, -4, 4, -4, 0, 0};
		for (int k = 0; k < t.length; k++) {
			double r = rear[k];
			a.rot("head", t[k], -30 * r, shake[k], shake[k] * 0.5);
			a.rot("jaw", t[k], (k == 0 || k == t.length - 1 ? 0 : 40) * (k == 1 ? 0.85 : 1), 0, 0);
			a.rot("seg_0", t[k], -14 * r, 0, 0);
			a.rot("seg_1", t[k], -10 * r, 0, 0);
			a.rot("seg_2", t[k], -6 * r, 0, 0);
			a.rot("seg_3", t[k], 4 * r, 0, 0);
			a.rot("seg_4", t[k], 8 * r, 0, 0);
			a.rot("seg_5", t[k], 6 * r, 0, 0);
			a.rot("mane", t[k], 22 * r, 0, 0);
			a.rot("mane_l", t[k], 18 * r, 0, -12 * r);
			a.rot("mane_r", t[k], 18 * r, 0, 12 * r);
			for (String side : new String[] {"l", "r"}) {
				int s = side.equals("l") ? 1 : -1;
				a.rot("whisker_" + side + "_0", t[k], 18 * r, s * 12 * r, 0);
				a.rot("whisker_" + side + "_1", t[k], 10 * r + shake[k], 0, 0);
				a.rot("whisker_" + side + "_2", t[k], 12 * r - shake[k], 0, 0);
				a.rot("whisker_" + side + "_3", t[k], 10 * r + shake[k] * 1.5, 0, 0);
			}
			a.rot("leg_fl", t[k], -34 * r, 0, -10 * r);
			a.rot("leg_fr", t[k], -34 * r, 0, 10 * r);
			a.rot("leg_fl_low", t[k], 30 * r, 0, 0);
			a.rot("leg_fr_low", t[k], 30 * r, 0, 0);
		}
	}

	// --- Myriad Woods Return to the Dao ---

	static final int JADE_WOOD = 0x1C6656, JADE_WOOD_LIGHT = 0x58B894, JADE_WOOD_DARK = 0x0C3A34, GROOVE = 0x9CFFD8;

	/**
	 * Jade wood: bark of carved green jade round an axis (the line x = 0, z = 0 of the cube's own frame), ridged in plates between grooves
	 * that spiral up it ({@code twist} turns a pixel) and glow with the tree's light; gold-brown toward the roots.
	 */
	static SpellArt.Painter jadeBark(double twist, int grooves, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			double th = Math.atan2(qx, qz) / (2 * Math.PI);
			double s = th * grooves + qy * twist + 0.35 * SpellArt.fbm(qy * 0.08, th * 4, seed, 2);
			double f = s - Math.floor(s);
			double n = SpellArt.fbm(th * 22 + seed, qy * 0.22, seed + 3, 3);
			int c = SpellArt.mix(JADE_WOOD, JADE_WOOD_LIGHT, n * 1.1 - 0.15);
			double foot = SpellArt.clamp01(1 - qy / 22);
			c = SpellArt.mix(c, SpellArt.mix(BARK_LIGHT, 0x8A7A40, n), foot * 0.55);
			if (f < 0.1) return SpellArt.glow(SpellArt.mix(GROOVE, VEIN_CORE, 0.5 - Math.abs(f - 0.05) * 6), 0.92);
			if (f < 0.16 || f > 0.95) return SpellArt.col(SpellArt.mix(JADE_WOOD_DARK, 0x0A2A24, 0.5));
			// The plates between the grooves: lit along their crest, grained across.
			double crest = 1 - Math.abs(f - 0.55) / 0.45;
			c = SpellArt.shade(c, -0.18 + 0.3 * crest);
			if (Math.abs(n - 0.5) < 0.025) c = SpellArt.shade(c, -0.3);
			return SpellArt.col(c);
		};
	}

	/** A bough's bark: jade wood grained along it (its y), a groove of light now and then. */
	static SpellArt.Painter boughBark(int seed) {
		return t -> {
			// Round the limb's own axis (its cube's y through its middle), so the grain runs on unbroken from face to face.
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2) / (2 * Math.PI);
			double n = SpellArt.fbm(ang * 9 + seed, t.ly * 0.3, seed, 3);
			int c = SpellArt.mix(JADE_WOOD, JADE_WOOD_LIGHT, n * 1.1 - 0.12);
			double s = ang * 3 + t.ly * 0.04 + 0.2 * SpellArt.noise(t.ly * 0.2, ang * 3, seed), f = s - Math.floor(s);
			if (f < 0.07) return SpellArt.glow(SpellArt.mix(GROOVE, JADE_WOOD_LIGHT, 0.3), 0.85);
			if (f < 0.13) c = SpellArt.shade(c, -0.32);
			if (Math.abs(n - 0.5) < 0.03) c = SpellArt.shade(c, -0.25);
			return SpellArt.col(c);
		};
	}

	/**
	 * A pad of foliage (the layered "clouds" of an old pine): dense small leaves, bright and glowing on top, darker down the sides, the
	 * light shining through beneath; their rims see-through.
	 */
	static SpellArt.Painter foliage(int seed) {
		return t -> {
			double px = t.px, py = t.py, pz = t.pz;
			// Leaf cells across the surface; each cell a little leaf turned its own way.
			double a = t.flat() ? px : px + pz, b = t.flat() ? pz : py;
			double cs = 1.1;
			int cx = (int) Math.floor(a / cs), cy = (int) Math.floor(b / cs);
			double u = a / cs - cx - 0.5, v = b / cs - cy - 0.5, ang = SpellArt.hash(cx, cy, seed) * Math.PI;
			double lu = u * Math.cos(ang) + v * Math.sin(ang), lv = -u * Math.sin(ang) + v * Math.cos(ang);
			boolean inLeaf = lu * lu / 0.2 + lv * lv / 0.06 < 1;
			double n = SpellArt.fbm(px * 0.21, pz * 0.21 + py * 0.15, seed, 3);
			double hi = SpellArt.hash(cx, cy, seed + 1);
			if (t.ny > 0.5) {
				int c = SpellArt.mix(LEAF, LEAF_LIGHT, n * 0.9 + (inLeaf ? 0.25 : -0.2));
				if (inLeaf && hi > 0.5) return SpellArt.glow(SpellArt.mix(c, VEIN_CORE, (hi - 0.5) * 0.7), 0.9);
				if (inLeaf && Math.abs(lv) < 0.06) return SpellArt.glow(SpellArt.mix(c, VEIN, 0.6), 0.85);
				return SpellArt.col(c, inLeaf ? 0.94 : 0.8);
			}
			if (t.ny < -0.5) {
				int c = SpellArt.mix(0x0E4A3A, LEAF_DARK, n);
				if (hi > 0.86 && inLeaf) return SpellArt.glow(SpellArt.mix(GROOVE, VEIN_CORE, hi - 0.86), 0.8);
				return SpellArt.col(c, 0.88);
			}
			int c = SpellArt.mix(LEAF_DARK, LEAF, n * 0.9 + (inLeaf ? 0.2 : -0.2) + t.up() * 0.25);
			if (inLeaf && hi > 0.9) return SpellArt.glow(LEAF_LIGHT, 0.85);
			return SpellArt.col(c, inLeaf ? 0.9 : 0.72);
		};
	}

	/**
	 * A spray of leaves on a blade: a frond lobed along its edges (a leaflet to each lobe), a stem of light up its middle, leaflets slanting
	 * out from it, brighter toward the tip, see-through at the rims.
	 */
	static SpellArt.Painter spray(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double lobe = Math.abs(Math.sin(v * Math.PI * 4.5 + seed * 0.7));
			double half = 0.5 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(v, 0.65))), 0.7) * (0.62 + 0.38 * lobe);
			if (au > half || v > 0.98) return SpellArt.CLEAR;
			if (au < 0.03 + 0.03 * (1 - v)) return SpellArt.glow(SpellArt.mix(VEIN, VEIN_CORE, v), 0.92);
			// Leaflets: bands slanting out and up from the stem, each a little lighter at its middle.
			double band = (v * 6.5 - au * 2.2) + seed * 0.13, f = band - Math.floor(band);
			int c = SpellArt.mix(LEAF_DARK, LEAF_LIGHT, 0.3 + 0.55 * v + 0.25 * Math.sin(f * Math.PI) - 0.15);
			if (f < 0.12) c = SpellArt.shade(c, -0.3);
			double edge = half - au;
			if (edge < 0.06) return SpellArt.col(SpellArt.shade(c, 0.2), 0.6);
			if (f > 0.45 && f < 0.55 && SpellArt.hash((int) Math.floor(band), u > 0 ? 1 : 0, seed) > 0.55) return SpellArt.glow(SpellArt.mix(c, VEIN, 0.5), 0.85);
			return SpellArt.col(c, 0.9);
		};
	}

	/** A hanging tendril of light: a wavering vine down a narrow blade, beaded, with tiny leaves. */
	static SpellArt.Painter tendril(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double wave = 0.16 * Math.sin(v * 9 + seed) * v;
			double d = Math.abs(u - wave);
			if (d < 0.07) return SpellArt.glow(SpellArt.mix(VEIN, LEAF, v * 0.5), 0.9 - (1 - v) * 0.3);
			double bead = Math.abs(v * 7 - Math.rint(v * 7));
			if (bead < 0.08 && d < 0.2) return SpellArt.glow(VEIN_CORE, 0.95);
			if (d < 0.32 && Math.floorMod((int) Math.floor(v * 14 + seed), 3) == 0 && (u - wave) * Math.sin(v * 40) > 0) return SpellArt.col(SpellArt.mix(LEAF, LEAF_LIGHT, v), 0.8);
			return SpellArt.CLEAR;
		};
	}

	/** A glowing fruit of the Dao: a peach of jade light, gold at its heart. */
	static SpellArt.Painter fruit() {
		return t -> {
			double x = t.fx - 0.5, y = t.fy - 0.5, r = Math.hypot(x, y);
			return SpellArt.glow(SpellArt.mix(0xFFF6C0, SpellArt.mix(0xB8FFD0, 0x5CE0A0, r * 2), SpellArt.smooth(0.05, 0.5, r)), 0.95);
		};
	}

	/** A bough of the tree: its {@code [heading, height, length, rise, thick, droop]}, built along +z and turned to its heading. */
	static final double[][] BOUGHS = {
			// heading, height, length, rise, thickness, droop at the tip
			{28, 60, 56, 10, 9.0, -6}, {208, 69, 52, 12, 8.4, -5}, {118, 80, 46, 12, 7.6, -4}, {298, 91, 42, 13, 7.0, -3},
			{62, 101, 36, 13, 6.2, -2}, {168, 110, 32, 14, 5.6, -1}, {248, 117, 28, 15, 5.0, 0},
	};

	static final double TRUNK_PIECE = 20.5;

	/**
	 * The spirit tree of Myriad Woods Return to the Dao: jade wood and the light of the Dao, a twisted trunk in six bones ("trunk_0" at the
	 * foot to "trunk_5"), its roots ("roots") spread over the ground, seven boughs ("bough_0".."bough_6", each with its "_tip"), and the
	 * crown and boughs' layered pads of glowing leaves ("pad_0".., hung with tendrils and fruits of light). The ground at y 0.
	 */
	static void daoTree(SpellArt.Model m) {
		m.tpp = 1.6;
		m.scale = 4;
		SHARED.clear();
		m.bone("root", null, 0, 0, 0);
		// The roots: nine, spreading and plunging, some arching over the ground.
		SpellArt.Bone roots = m.bone("roots", "root", 0, 0, 0);
		for (int k = 0; k < 9; k++) {
			// Each root swells from the trunk's foot, arches out over the ground and curls a little aside as it plunges into it.
			double ang = Math.toRadians(k * 40 + 18 + (SpellArt.hash(k, 3, 7) - 0.5) * 26), len = 32 + SpellArt.hash(k, 5, 7) * 14, arch = k % 3 == 0 ? 6 : 2.5;
			double cx = Math.sin(ang), cz = Math.cos(ang), curl = (SpellArt.hash(k, 9, 7) - 0.5) * 0.5;
			double[] a = SpellArt.v3(cx * 6, 15, cz * 6), b = SpellArt.v3(cx * len * 0.32, 8 + arch, cz * len * 0.32),
					c = SpellArt.v3((cx + cz * curl * 0.5) * len * 0.6, 3.5 + arch * 0.7, (cz - cx * curl * 0.5) * len * 0.6),
					d = SpellArt.v3((cx + cz * curl) * len * 0.84, 0.5, (cz - cx * curl) * len * 0.84), e = SpellArt.v3((cx + cz * curl * 1.4) * len, -4, (cz - cx * curl * 1.4) * len);
			SpellArt.limb(roots, a, b, 10, 9, jadeBark(0.02, 5, 400 + k));
			SpellArt.limb(roots, b, c, 7, 6.4, boughBark(410 + k));
			SpellArt.limb(roots, c, d, 5, 4.6, boughBark(420 + k));
			SpellArt.limb(roots, d, e, 3.2, 3, boughBark(430 + k));
		}
		// The trunk: six pieces, each twisted on the last and leaning by turns, octagonal (a square and a diamond).
		double[] widths = {24, 20, 17, 14.2, 11.6, 9.2};
		double[][] lean = {{0, 0, 0}, {7, 16, -7}, {-8, 15, 8}, {8, 16, -8}, {-7, 15, 7}, {6, 14, -6}};
		String up = "root";
		double y = 0;
		for (int i = 0; i < 6; i++) {
			double h = TRUNK_PIECE, w = widths[i];
			SpellArt.Bone tr = m.bone("trunk_" + i, up, 0, y, 0, lean[i][0], lean[i][1], lean[i][2]);
			SpellArt.Painter bark = jadeBark(0.018, 6, 430);
			SpellArt.Cube sq = tr.box(0, y + h / 2, 0, w, h + 2, w, bark), di = tr.box(0, y + h / 2, 0, w * 0.86, h + 2, w * 0.86, bark).rot(0, 45, 0);
			if (i > 0) {
				sq.skip(SpellArt.Face.DOWN);
				di.skip(SpellArt.Face.DOWN);
			}
			if (i < 5) {
				sq.skip(SpellArt.Face.UP);
				di.skip(SpellArt.Face.UP);
			}
			if (i == 0) {
				tr.box(0, 5, 0, w * 1.4, 10, w * 1.4, bark).skip(SpellArt.Face.DOWN).rot(0, 22, 0); // the flared foot
				tr.box(0, 2.5, 0, w * 1.75, 5, w * 1.75, bark).skip(SpellArt.Face.DOWN).rot(0, 4, 0);
			}
			up = tr.name;
			y += h;
		}
		// The boughs, from the trunk piece at their height; each built along +z, turned to its heading.
		int pad = 0;
		for (int k = 0; k < BOUGHS.length; k++) {
			double[] b = BOUGHS[k];
			int on = Math.min(5, (int) (b[1] / TRUNK_PIECE));
			SpellArt.Bone bough = m.bone("bough_" + k, "trunk_" + on, 0, b[1], 0, 0, b[0], 0);
			double L = b[2], t0 = b[4];
			double[] p0 = SpellArt.v3(0, b[1], 2), p1 = SpellArt.v3(0, b[1] + b[3] * 0.65, L * 0.45), p2 = SpellArt.v3(0, b[1] + b[3], L * 0.78);
			SpellArt.limb(bough, p0, p1, t0, t0, boughBark(500 + k));
			SpellArt.Bone tip = m.bone("bough_" + k + "_tip", "bough_" + k, p1[0], p1[1], p1[2]);
			SpellArt.limb(tip, p1, p2, t0 * 0.7, t0 * 0.7, boughBark(510 + k));
			double[] p3 = SpellArt.v3(0, p2[1] + b[5], L);
			SpellArt.limb(tip, p2, p3, t0 * 0.45, t0 * 0.45, boughBark(520 + k));
			// A side branch from the middle of the tip, with a lesser pad of its own.
			double side = k % 2 == 0 ? 1 : -1;
			double[] s0 = SpellArt.add(p1, SpellArt.times(SpellArt.sub(p2, p1), 0.4)), s1 = SpellArt.add(s0, SpellArt.v3(side * L * 0.32, b[3] * 0.5, L * 0.12));
			SpellArt.limb(tip, s0, s1, t0 * 0.4, t0 * 0.4, boughBark(530 + k));
			pad = foliagePad(m, "bough_" + k + "_tip", pad, p3, 34 - k * 1.8, 26 - k * 1.3, k * 31);
			pad = foliagePad(m, "bough_" + k + "_tip", pad, s1, 20 - k * 0.9, 16 - k * 0.7, k * 31 + 7);
		}
		// The crown.
		pad = foliagePad(m, "trunk_5", pad, SpellArt.v3(0, 6 * TRUNK_PIECE + 2, 0), 38, 34, 300);
		pad = foliagePad(m, "trunk_5", pad, SpellArt.v3(-2, 6 * TRUNK_PIECE + 10, 1), 24, 22, 310);
		grow(m, pad);
		sway(m, pad);
		wither(m, pad);
	}

	/** The blades painted once and shared since (by name), for the model being built. */
	static final java.util.Map<String, SpellArt.Cube> SHARED = new java.util.HashMap<>();

	/** A blade (standing, or {@code flat}), painted the first time a name is asked for and sharing that painting after (the same size). */
	static SpellArt.Cube sharedBlade(SpellArt.Bone bone, String name, boolean flat, double[] base, double length, double width, double sweep, double heading, double bank,
			SpellArt.Painter p, double tpp, boolean flip) {
		SpellArt.Cube like = SHARED.get(name);
		if (like == null) {
			SpellArt.Cube c = (flat ? SpellArt.flatBlade(bone, base, length, width, sweep, heading, bank, p) : SpellArt.blade(bone, base, length, width, sweep, heading, bank, p)).density(tpp);
			SHARED.put(name, c);
			return c;
		}
		return SpellArt.bladeLike(bone, like, base, sweep, heading, bank, flip);
	}

	/** A pad of foliage ("pad_<n>") at {@code at}, {@code d} long and {@code e} across, fringed with sprays, hung with tendrils and a fruit. */
	static int foliagePad(SpellArt.Model m, String parent, int n, double[] at, double d, double e, int seed) {
		SpellArt.Bone pad = m.bone("pad_" + n, parent, at[0], at[1], at[2]);
		double cx = at[0], cy = at[1] + 1.5, cz = at[2] + d * 0.12;
		SpellArt.Painter leaves = foliage(600 + seed);
		// A cloud of leaves: a broad bed, and lumps heaped on it, each its own size and turn, a little tilted.
		pad.box(cx, cy, cz, e * 0.92, 3.0, d * 0.92, leaves).rot(0, 12, 0);
		for (int k = 0; k < 6; k++) {
			double h1 = SpellArt.hash(k, seed, 1), h2 = SpellArt.hash(k, seed, 2), h3 = SpellArt.hash(k, seed, 3);
			double ang = 2 * Math.PI * (k / 6.0 + h1 * 0.12), r = k == 0 ? 0 : 0.27 + 0.1 * h2;
			double lw = e * (k == 0 ? 0.6 : 0.38 + 0.12 * h3), ld = d * (k == 0 ? 0.58 : 0.34 + 0.12 * h2), lh = k == 0 ? 5.6 : 2.8 + 1.8 * h1;
			pad.box(cx + Math.sin(ang) * e * r, cy + 1.2 + lh / 2, cz + Math.cos(ang) * d * r, lw, lh, ld, leaves).rot((h2 - 0.5) * 12, h3 * 90, (h1 - 0.5) * 12).skip(SpellArt.Face.DOWN);
		}
		// Sprays of leaves lying out from the rim, drooping a little, two rings of them; more rising from the top (a few paintings shared by every pad).
		for (int ring = 0; ring < 2; ring++) {
			int sprays = ring == 0 ? 14 : 9;
			for (int k = 0; k < sprays; k++) {
				double ang = 2 * Math.PI * (k + ring * 0.5) / sprays + seed * 0.1, reach = ring == 0 ? 0.42 : 0.3;
				double rx = Math.sin(ang) * e * reach, rz = Math.cos(ang) * d * reach;
				int variant = (k + seed + ring) % 3;
				sharedBlade(pad, "spray" + variant, true, SpellArt.v3(cx + rx, cy - 0.6 + ring * 3.2, cz + rz), 8 + variant * 1.6, 6.4, -14 + ring * 10 - (k % 3) * 6,
						Math.toDegrees(Math.atan2(rx, rz)), 0, spray(variant * 17), 2.2, k % 2 == 1);
			}
		}
		for (int k = 0; k < 5; k++) {
			double ang = 2 * Math.PI * k / 5 + seed;
			sharedBlade(pad, "spray_top" + k % 2, false, SpellArt.v3(cx + Math.sin(ang) * e * 0.2, cy + 5.6, cz + Math.cos(ang) * d * 0.2), 7, 5.4, -32, Math.toDegrees(ang), 0,
					spray(60 + k % 2), 2.2, k % 3 == 0);
		}
		// Tendrils of light hanging beneath, and a fruit of the Dao.
		for (int k = 0; k < 3; k++) {
			double ang = 2 * Math.PI * k / 3 + seed * 0.7;
			int variant = (seed + k) % 3;
			double[] base = SpellArt.v3(cx + Math.sin(ang) * e * 0.3, cy - 1.6, cz + Math.cos(ang) * d * 0.3);
			sharedBlade(pad, "tendril" + variant, false, base, 10 + variant * 3.5, 2.2, 180, Math.toDegrees(ang) + 90, 0, tendril(variant * 5), 3, false);
		}
		pad.box(cx + e * 0.12, cy - 3.4, cz - d * 0.1, 1.8, 2.2, 1.8, fruit()).density(4);
		return n + 1;
	}

	/** The growing: the tree rises from nothing, its trunk untwisting, its boughs unfurling from the trunk, its pads swelling out last. */
	static void grow(SpellArt.Model m, int pads) {
		SpellArt.Anim a = m.anim("grow", 2, null);
		double[] t = {0, 0.2, 0.4, 0.6, 0.8, 1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
		for (double tt : t) {
			double g = SpellArt.easeOut(tt / 1.3);
			double over = tt > 1.2 && tt < 1.7 ? 1 + 0.04 * Math.sin((tt - 1.2) / 0.5 * Math.PI) : 1;
			a.scale("root", tt, Math.max(0.05, 0.25 + 0.75 * g) * over, Math.max(0.01, g) * over, Math.max(0.05, 0.25 + 0.75 * g) * over);
			for (int i = 1; i < 6; i++) a.rot("trunk_" + i, tt, 0, -24 * (1 - g), 0);
			for (int k = 0; k < BOUGHS.length; k++) {
				double start = 0.3 + k * 0.12, u = SpellArt.easeOut((tt - start) / 0.9);
				a.rot("bough_" + k, tt, 72 * (1 - u), 0, 0);
				a.rot("bough_" + k + "_tip", tt, 40 * (1 - u), 0, 0);
			}
		}
		for (int p = 0; p < pads; p++) {
			double start = 0.75 + (p % 7) * 0.09;
			for (double tt : new double[] {0, start, start + 0.35, start + 0.55, 2.0}) {
				double s = tt <= start ? 0 : tt >= 2.0 || tt == start + 0.55 ? 1 : 1.12;
				a.scale("pad_" + p, Math.min(2, tt), Math.max(0.001, s));
			}
		}
	}

	/** The sway: the trunk breathing in a slow wind, boughs and pads nodding after it, the tendrils swinging. */
	static void sway(SpellArt.Model m, int pads) {
		double T = 6;
		SpellArt.Anim a = m.anim("sway", T, "true");
		for (int i = 1; i < 6; i++) {
			int ii = i;
			a.sample("trunk_" + i, "rotation", 0, T, 12, t -> new double[] {0.8 * Math.sin(2 * Math.PI * t / T - ii * 0.4), 0, 1.1 * Math.sin(2 * Math.PI * t / T - ii * 0.4 + 1.2)});
		}
		for (int k = 0; k < BOUGHS.length; k++) {
			int kk = k;
			a.sample("bough_" + k, "rotation", 0, T, 12, t -> new double[] {2.4 * Math.sin(2 * Math.PI * t / T - kk * 0.8 - 0.6), 0, 0});
			a.sample("bough_" + k + "_tip", "rotation", 0, T, 12, t -> new double[] {3.2 * Math.sin(2 * Math.PI * t / T - kk * 0.8 - 1.4), 0, 0});
		}
		for (int p = 0; p < pads; p++) {
			int pp = p;
			a.sample("pad_" + p, "rotation", 0, T, 12, t -> new double[] {2 * Math.sin(2 * Math.PI * t / T - pp * 0.7 - 2), 0, 1.6 * Math.sin(2 * Math.PI * t / T - pp * 0.5 - 1)});
		}
	}

	/** The withering: the pads shrink away, the boughs droop and the tips hang, the trunk bows, and all of it sinks into nothing. */
	static void wither(SpellArt.Model m, int pads) {
		SpellArt.Anim a = m.anim("wither", 1.5, "hold_on_last_frame");
		double[] t = {0, 0.3, 0.6, 0.9, 1.2, 1.5};
		for (double tt : t) {
			double d = SpellArt.ease(tt / 1.1);
			for (int k = 0; k < BOUGHS.length; k++) {
				a.rot("bough_" + k, tt, -24 * d, 0, 0);
				a.rot("bough_" + k + "_tip", tt, -26 * d, 0, 0);
			}
			a.rot("trunk_2", tt, 5 * d, 0, -3 * d);
			a.rot("trunk_4", tt, 6 * d, 0, 4 * d);
			double sink = SpellArt.ease((tt - 1.0) / 0.5);
			a.scale("root", tt, Math.max(0.001, 1 - 0.7 * sink), Math.max(0.001, 1 - sink), Math.max(0.001, 1 - 0.7 * sink));
		}
		for (int p = 0; p < pads; p++) {
			double start = 0.05 + (p % 5) * 0.08;
			a.scale("pad_" + p, 0, 1);
			a.scale("pad_" + p, start, 1);
			a.scale("pad_" + p, start + 0.45, 0.55);
			a.scale("pad_" + p, Math.min(1.5, start + 0.9), 0.001);
			a.scale("pad_" + p, 1.5, 0.001);
		}
	}
}
