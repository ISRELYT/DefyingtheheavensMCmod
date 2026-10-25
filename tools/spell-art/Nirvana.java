/**
 * The looks of Entering Nirvana's great spells for SpellArt (2026-10-24): death and rebirth, each wrought of what it is cast from. Wood's seed of
 * withering rebirth and wheel of the seasons; fire's undying ember and pyre of the heavens; earth's gravestone on its stone tortoise and the
 * standing stones of a buried world; water's frozen moon in the well and the moon of ice that falls; metal's tomb of broken swords and the sword
 * reborn in white flame; the ghost foundation's two Impermanences and the Book of Life and Death; both paths' eclipse of heaven's sun by the
 * demon's moon. At the render scale (4) and its realm's greatness (2.41) a model pixel is some six tenths of a block.
 */
public class Nirvana {
	// --- Shared shapes ---

	/**
	 * A petal or a leaf that opens: its turn ("NAME_turn": turned {@code theta} degrees round the y axis, so its outward way is that way) and
	 * the petal itself ("NAME": its x turn opens it, tipping its top out), its base at {@code base}; a blade standing in the x-y plane (it opens
	 * out of its own plane). @return the petal's bone
	 */
	static SpellArt.Bone petal(SpellArt.Model m, String parent, String name, double[] base, double theta, double length, double width, String shared,
			SpellArt.Painter p, double tpp, boolean flip) {
		m.bone(name + "_turn", parent, base[0], base[1], base[2], 0, theta + 180, 0);
		SpellArt.Bone b = m.bone(name, name + "_turn", base[0], base[1], base[2]);
		SpellArt.shared(m, b, shared, false, base, length, width, 0, 90, 0, p, tpp, flip);
		return b;
	}

	// --- Seed of Withering Rebirth ---

	static final int HUSK_DARK = 0x231811, HUSK = 0x4E3B2B, HUSK_LIGHT = 0x84705A, HUSK_PALE = 0xB8A88C;
	static final int PETAL_ROOT = 0xE4FFEE, PETAL = 0xF8ECF2, PETAL_BLUSH = 0xF2A6C8, PETAL_GOLD = 0xFFE8A0;

	/**
	 * The seed's husk (on a solid, its seam the plane x = 0): one half withered (grey-brown, cracked, dry), the other alive (jade, veins of
	 * light through it), the seam between them burning with the light of its rebirth.
	 */
	static SpellArt.Painter seedHusk(int seed) {
		return t -> {
			double x = t.px + 0.35 * Math.sin(t.py * 1.1 + seed);
			if (Math.abs(x) < 0.3) return SpellArt.glow(SpellArt.mix(Wood.VEIN_CORE, Earth.GOLD_PALE, 0.35), 0.98);
			double n = SpellArt.fbm3(t.px * 0.5, t.py * 0.5, t.pz * 0.5, seed, 3);
			double up = 0.22 * Math.max(0, t.ny) - 0.22 * Math.max(0, -t.ny);
			if (x < 0) {
				double crack = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.9, t.py * 0.5, t.pz * 0.9, seed + 3, 2) - 1);
				if (crack > 0.93) return SpellArt.col(HUSK_DARK);
				return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.4 + (n - 0.5) * 0.7 + up), HUSK_DARK, HUSK, HUSK_LIGHT, HUSK_PALE));
			}
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.6, t.py * 0.35, t.pz * 0.6, seed + 5, 3) - 1), 9);
			if (vein > 0.55) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, (vein - 0.55) * 2), 0.96);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.45 + (n - 0.5) * 0.6 + up), Wood.LEAF_DARK, Wood.JADE, Wood.LEAF_LIGHT, Wood.PALE));
		};
	}

	/**
	 * A petal of the flower of rebirth (on a blade): broad and pointed, jade-white at its foot through pearl to a blush at its point, fine veins
	 * of blush up it, its midrib and rim lit gold; glowing, a little seen through.
	 */
	static SpellArt.Painter rebirthPetal(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(v, 0.75))), 0.55) * (v < 0.12 ? 0.5 + 4 * v : 1);
			if (au > half || v > 0.99) return SpellArt.CLEAR;
			if (half - au < 0.045) return SpellArt.glow(SpellArt.mix(PETAL_GOLD, 0xFFFFFF, v), 0.92);
			if (au < 0.025) return SpellArt.glow(SpellArt.mix(Wood.VEIN_CORE, PETAL_GOLD, v), 0.9);
			double vein = au / Math.max(0.05, half) * 3 - v * 0.6 + seed * 0.1;
			if (Math.abs(vein - Math.rint(vein)) < 0.07) return SpellArt.glow(SpellArt.mix(PETAL_BLUSH, PETAL, 0.5), 0.78);
			int c = SpellArt.ramp(SpellArt.clamp01(v + (SpellArt.noise(u * 5 + seed, v * 6, seed) - 0.5) * 0.15), Wood.JADE_LIGHT, PETAL_ROOT, PETAL, PETAL_BLUSH);
			return SpellArt.glow(c, 0.82 + 0.12 * v);
		};
	}

	/** The flower's heart: gold light, white at its middle. */
	static SpellArt.Painter bloomHeart() {
		return t -> SpellArt.glow(SpellArt.mix(0xFFFFFF, PETAL_GOLD, SpellArt.clamp01(Math.hypot(t.px, t.pz) / 2.4 + 0.2 * SpellArt.noise(t.px * 2, t.py * 2, 1407))), 0.97);
	}

	/** The seed's radius {@code y} pixels from its middle: an almond, pointed at both ends. */
	static double seedRadius(double y) {
		return 3.6 * Math.pow(Math.max(0, 1 - (y / 6.2) * (y / 6.2)), 0.55);
	}

	/**
	 * The Seed of Withering Rebirth, its heart at the origin: the seed ("seed", half withered, half alive, its seam burning), the shoot it puts out
	 * ("sprout"), its roots of vine reaching out and down into what it has struck ("vine_0".."vine_5"), and the flower of rebirth that opens out of
	 * it ("bloom": five inner petals and seven outer, "petal_i0".., "petal_o0".., round a heart of gold light), at first a closed bud.
	 */
	static void rebirthSeed(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone seed = m.bone("seed", "root", 0, 0, 0);
		Vessel.lathe(seed, -6.2, 6.2, 1.24, Nirvana::seedRadius, seedHusk(1401));
		SpellArt.Bone sprout = m.bone("sprout", "root", 0, 6, 0);
		SpellArt.limb(sprout, SpellArt.v3(0, 5.5, 0), SpellArt.v3(0.6, 11, 0.4), 0.9, 0.9, Vessel.vine(1403));
		SpellArt.shared(m, sprout, "sprout_leaf", false, SpellArt.v3(0.6, 10.5, 0.4), 6, 4, 50, 40, 0, Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.5, 1405), 4, false);
		SpellArt.shared(m, sprout, "sprout_leaf", false, SpellArt.v3(0.6, 10.5, 0.4), 6, 4, 50, 220, 0, null, 4, true);
		// Its vines: thin, winding out round it as they reach down, each its own length, leaves along them.
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k + 15), wind = (k % 2 == 0 ? 1 : -1) * 0.55, reach = 1 + 0.18 * (k % 3);
			SpellArt.Bone v = m.bone("vine_" + k, "root", 0, -4, 0);
			double[] at = SpellArt.v3(Math.sin(th) * 1.4, -4.2, Math.cos(th) * 1.4);
			for (int j = 1; j <= 5; j++) {
				double f = j / 5.0, a = th + wind * f * 2.2, out = (1.4 + 9 * Math.pow(f, 0.8)) * reach;
				double[] next = SpellArt.v3(Math.sin(a) * out, -4.2 - 16 * f * reach + 2.5 * Math.sin(f * Math.PI * 2 + k), Math.cos(a) * out);
				double w = 0.95 - 0.13 * j;
				SpellArt.limb(v, at, next, w, w, Vessel.vine(1411 + k * 5 + j));
				if (j == 2 || j == 4) SpellArt.shared(m, v, "vine_leaf", false, next, 4, 3, 55, Math.toDegrees(a) + 90 * (j == 2 ? 1 : -1), 0,
						Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.4, 1471), 4, (k + j) % 2 == 0);
				at = next;
			}
		}
		SpellArt.Bone bloom = m.bone("bloom", "root", 0, 4, 0);
		for (int k = 0; k < 7; k++) petal(m, "bloom", "petal_o" + k, SpellArt.v3(0, 4, 0), 360.0 * k / 7, 13, 7, "petal_outer", rebirthPetal(1441), 3, k % 2 == 1);
		for (int k = 0; k < 5; k++) petal(m, "bloom", "petal_i" + k, SpellArt.v3(0, 4.3, 0), 360.0 * k / 5 + 25, 9.5, 5.5, "petal_inner", rebirthPetal(1443), 3, k % 2 == 1);
		SpellArt.Bone heart = m.bone("heart", "bloom", 0, 5, 0);
		Void.ball(heart, 1.5, bloomHeart(), 4);
		// The ball is made about the model's origin: up into the flower's heart with its bone.
		for (SpellArt.Cube c : heart.cubes) {
			c.origin[1] += 5;
			if (c.pivot != null) c.pivot[1] += 5;
		}
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k);
			SpellArt.limb(heart, SpellArt.v3(0, 5, 0), SpellArt.v3(Math.sin(th) * 2.4, 9, Math.cos(th) * 2.4), 0.4, 0.4, t -> SpellArt.glow(PETAL_GOLD, 0.95));
		}
		// Its flight (looped): the seed spinning about its length (on its own controller from its first moment: it drills on into what it
		// strikes), its roots, shoot and flower not yet out of it (theirs the root's bones, so they stand still round it).
		double T = 0.6;
		SpellArt.Anim fly = m.anim("fly", T, "true");
		fly.sample("seed", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		for (String b : new String[] {"sprout", "bloom", "vine_0", "vine_1", "vine_2", "vine_3", "vine_4", "vine_5"}) {
			fly.scale(b, 0, 0.02);
			fly.scale(b, T, 0.02);
		}
		// It roots (played once, held): upright in what it struck, its vines reaching out and down, its shoot out, its bud set.
		SpellArt.Anim root = m.anim("root", 0.8, "hold_on_last_frame");
		root.rot("seed", 0, 0, 0, 0);
		root.rot("seed", 0.8, 0, 0, 0);
		for (int k = 0; k < 6; k++) {
			int kk = k;
			root.sample("vine_" + k, "scale", 0, 0.8, 6, t -> {
				double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - kk * 0.04) / 0.6)));
				return new double[] {s, s, s};
			});
		}
		root.sample("sprout", "scale", 0.2, 0.8, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut((t - 0.2) / 0.6));
			return new double[] {s, s, s};
		});
		root.scale("bloom", 0, 0.02);
		root.scale("bloom", 0.5, 0.02);
		root.scale("bloom", 0.8, 0.6);
		bloomPose(root, 0, 0);
		bloomPose(root, 0.8, 0);
		// It blooms (played once, held): the flower thrown open great out of it, its heart blazing; the seed spent, withering within it.
		SpellArt.Anim open = m.anim("bloom", 1.0, "hold_on_last_frame");
		open.scale("bloom", 0, 0.6);
		open.scale("bloom", 0.45, 1.75);
		open.scale("bloom", 0.7, 1.55);
		open.scale("bloom", 1.0, 1.6);
		for (double tt : new double[] {0, 0.25, 0.5, 0.75, 1.0}) bloomPose(open, tt, Math.min(1, SpellArt.easeOut(tt / 0.6)) * (tt > 0.6 ? 1 - 0.06 * Math.sin((tt - 0.6) / 0.4 * Math.PI) : 1));
		open.scale("heart", 0, 1);
		open.scale("heart", 0.4, 1.6);
		open.scale("heart", 1.0, 1.25);
		open.scale("seed", 0, 1);
		open.scale("seed", 1.0, 0.45);
		for (int k = 0; k < 6; k++) {
			open.scale("vine_" + k, 0, 1);
			open.scale("vine_" + k, 1.0, 1);
		}
		open.scale("sprout", 0, 1);
		open.scale("sprout", 1.0, 1);
		// It withers (played once, held): its petals drooping and falling in, its vines drawn in, all of it sinking.
		SpellArt.Anim wither = m.anim("wither", 1.2, "hold_on_last_frame");
		for (double tt : new double[] {0, 0.4, 0.8, 1.2}) bloomPose(wither, tt, 1 + 0.9 * SpellArt.easeIn(tt / 1.2));
		wither.scale("bloom", 0, 1.6);
		wither.scale("bloom", 1.2, 0.9);
		wither.pos("bloom", 0, 0, 0, 0);
		wither.pos("bloom", 1.2, 0, -5, 0);
		wither.scale("heart", 0, 1.25);
		wither.scale("heart", 1.2, 0.3);
		wither.scale("seed", 0, 0.45);
		wither.scale("seed", 1.2, 0.2);
		for (int k = 0; k < 6; k++) {
			wither.scale("vine_" + k, 0, 1);
			wither.scale("vine_" + k, 1.2, 0.5, 0.35, 0.5);
		}
	}

	/** The flower's petals {@code open} of the way open (0 a bud, 1 open, more: drooping over) at {@code time}. */
	static void bloomPose(SpellArt.Anim a, double time, double open) {
		for (int k = 0; k < 7; k++) a.rot("petal_o" + k, time, 8 + 70 * open, 0, 0);
		for (int k = 0; k < 5; k++) a.rot("petal_i" + k, time, 4 + 44 * open, 0, 0);
	}

	// --- Withering and Flourishing ---

	static final int DRY = 0x5E4A34, DRY_LIGHT = 0x8C7452, DRY_PALE = 0xB49C74, DRY_CRACK = 0x2A1E14, BLOSSOM = 0xF4B8D2, BLOSSOM_WHITE = 0xFFF0F6;

	/** Whether the wheel of the seasons is alive (spring's) at (x, z) of its half-width: a taiji of the two, each with the other's eye. */
	static boolean alive(double x, double z) {
		double h = 0.44, dTop = Math.hypot(x, z - h), dBottom = Math.hypot(x, z + h);
		boolean alive;
		if (dTop <= h) alive = true;
		else if (dBottom <= h) alive = false;
		else alive = x < 0;
		if (dTop < h * 0.26) alive = false;
		if (dBottom < h * 0.26) alive = true;
		return alive;
	}

	/**
	 * The wheel of the seasons (on a sheet): its rim a band of carved wood written round with the seasons' glyphs in gold; within it the taiji of
	 * withering and flourishing: spring's half green, blossoms scattered through it, autumn's dry earth, cracked, fallen leaves on it; each with
	 * the other's eye, and a seam of gold light winding between them; glowing, a little seen through.
	 */
	static SpellArt.Painter seasonDisc(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.99) return SpellArt.CLEAR;
			if (r > 0.88) {
				if (r > 0.97 || r < 0.9) return SpellArt.glow(SpellArt.mix(Wood.BARK_LIGHT, Earth.GOLD_PALE, 0.4), 0.95);
				double ang = (Math.atan2(z, x) / (2 * Math.PI) + 0.5) * 24;
				int cell = Math.floorMod((int) Math.floor(ang), 24);
				if (Fire.glyph(cell, seed, ang - Math.floor(ang), (r - 0.9) / 0.07)) return SpellArt.glow(Earth.GOLD_PALE, 0.97);
				return SpellArt.col(SpellArt.mix(Wood.BARK, Wood.BARK_LIGHT, 0.4 + 0.3 * SpellArt.noise(ang * 3, r * 30, seed)), 0.95);
			}
			double X = x / 0.88, Z = z / 0.88;
			boolean live = alive(X, Z);
			double e = 0.018;
			if (alive(X + e, Z) != live || alive(X - e, Z) != live || alive(X, Z + e) != live || alive(X, Z - e) != live) return SpellArt.glow(SpellArt.mix(Earth.GOLD_PALE, 0xFFFFFF, 0.4), 0.98);
			double n = SpellArt.fbm(x * 9 + seed, z * 9, seed, 3);
			if (live) {
				int cx = (int) Math.floor(x * 30), cz = (int) Math.floor(z * 30);
				double h = SpellArt.hash(cx, cz, seed);
				if (h > 0.9) return SpellArt.glow(h > 0.96 ? BLOSSOM_WHITE : BLOSSOM, 0.95);
				double blade = Math.sin(x * 140 + Math.sin(z * 40) * 2) * Math.sin(z * 90);
				int c = SpellArt.mix(Wood.LEAF_DARK, Wood.LEAF, 0.35 + 0.5 * n + 0.15 * blade);
				return SpellArt.glow(c, 0.8);
			}
			// Dry earth in cracked plates, leaves fallen on it.
			double cx = x * 11, cz = z * 11, d1 = 9, d2 = 9;
			int ix = (int) Math.floor(cx), iz = (int) Math.floor(cz);
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
				double d = Math.hypot(cx - (ix + dx + SpellArt.hash(ix + dx, iz + dz, seed + 3)), cz - (iz + dz + SpellArt.hash(ix + dx, iz + dz, seed + 4)));
				if (d < d1) {
					d2 = d1;
					d1 = d;
				} else if (d < d2) d2 = d;
			}
			if (d2 - d1 < 0.08) return SpellArt.col(DRY_CRACK, 0.9);
			int lx = (int) Math.floor(x * 24), lz = (int) Math.floor(z * 24);
			double h = SpellArt.hash(lx, lz, seed + 7);
			if (h > 0.9) return SpellArt.col(h > 0.95 ? Fire.FLAME : Fire.EMBER, 0.92);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.35 + (n - 0.5) * 0.8 + 0.2 * (d2 - d1)), DRY_CRACK, DRY, DRY_LIGHT, DRY_PALE), 0.85);
		};
	}

	/** A twig of the dead half (on a limb): bare, grey-brown, frost on its upper side. */
	static SpellArt.Painter deadTwig(int seed) {
		return t -> SpellArt.col(t.ny > 0.5 ? 0xD8E4EA : SpellArt.mix(0x3A3028, 0x6E625A, 0.3 + 0.5 * SpellArt.noise(t.ly * 0.6 + seed, t.fx * 3, seed)));
	}

	/** A blossom's petal (on a blade): small, round-tipped, pink to white. */
	static SpellArt.Painter blossom(int seed) {
		return t -> {
			double v = t.v(), u = (t.u() - 0.5) * 2, half = Math.sin(Math.PI * Math.min(1, v * 0.9 + 0.1)) * 0.95;
			if (Math.abs(u) > half || v < 0.03) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(BLOSSOM_WHITE, BLOSSOM, 0.3 + 0.5 * Math.abs(u) + 0.2 * SpellArt.noise(u * 3 + seed, v * 4, seed)), 0.9);
		};
	}

	/** A fallen leaf of autumn (on a blade): pointed, its colour turning from amber to russet, its vein darker. */
	static SpellArt.Painter autumnLeaf(int seed) {
		return t -> {
			double v = t.v(), u = (t.u() - 0.5) * 2, half = 0.85 * Math.pow(Math.max(0, Math.sin(Math.PI * Math.pow(v, 0.8))), 0.8);
			if (Math.abs(u) > half || v > 0.98) return SpellArt.CLEAR;
			if (Math.abs(u) < 0.07) return SpellArt.col(Fire.EMBER);
			return SpellArt.col(SpellArt.mix(Fire.AMBER, Fire.CRIMSON, 0.25 + 0.6 * v + 0.15 * SpellArt.noise(u * 4 + seed, v * 3, seed)));
		};
	}

	/**
	 * The wheel of withering and flourishing, its heart at the origin on the ground, 112 pixels across (lying on the land, as wide as the spell's
	 * reach): the wheel ("disc", turning), the tree at its heart half in blossom and half bare ("tree": its trunk, three boughs of each), and
	 * blossoms and dead leaves drifting round it ("drift").
	 */
	static void seasonWheel(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone disc = m.bone("disc", "root", 0, 0, 0);
		SpellArt.sheet(disc, 0, 0.3, 0, 112, 112, 0, 0, 0, seasonDisc(1501)).density(1.6);
		SpellArt.Bone tree = m.bone("tree", "root", 0, 0, 0);
		SpellArt.limb(tree, SpellArt.v3(0, 0, 0), SpellArt.v3(0.4, 14, 0.2), 3.2, 3.2, Wood.bark(1503, 0.02));
		SpellArt.Painter twig = deadTwig(1505);
		for (int k = 0; k < 6; k++) {
			boolean live = k % 2 == 0;
			double th = Math.toRadians(live ? 120 + k * 22 : -60 + k * 20), out = 7 + (k % 3) * 2;
			double[] a = SpellArt.v3(0.4, 10 + (k % 3) * 1.5, 0.2), b = SpellArt.v3(-Math.abs(Math.cos(th)) * out * (live ? 1 : -1), 16 + (k % 3) * 2, Math.sin(th) * out * 0.6);
			SpellArt.limb(tree, a, b, live ? 1.6 : 1.0, live ? 1.6 : 1.0, live ? Wood.bark(1507 + k, 0.05) : twig);
			if (live) {
				for (int j = 0; j < 4; j++) {
					double[] at = SpellArt.add(b, SpellArt.v3(Math.sin(j * 1.9) * 1.6, 0.6 + j * 0.5, Math.cos(j * 2.3) * 1.6));
					SpellArt.shared(m, tree, "blossom", false, at, 2.8, 2.4, 30 + j * 20, j * 90 + k * 30, 0, blossom(1511), 5, j % 2 == 0);
				}
			} else {
				double[] c = SpellArt.add(b, SpellArt.v3(b[0] * 0.3, 3, b[2] * 0.4));
				SpellArt.limb(tree, b, c, 0.6, 0.6, twig);
			}
		}
		SpellArt.Bone drift = m.bone("drift", "root", 0, 0, 0);
		for (int k = 0; k < 12; k++) {
			double th = 30.0 * k, r = 26 + (k % 3) * 9, y = 5 + (k % 4) * 3;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * r, y, Math.cos(Math.toRadians(th)) * r);
			if (k % 2 == 0) SpellArt.shared(m, drift, "drift_blossom", false, at, 3.2, 2.8, 50, th, 30, blossom(1521), 5, k % 4 == 0);
			else SpellArt.shared(m, drift, "drift_leaf", false, at, 4, 2.6, 70, th, -20, autumnLeaf(1523), 5, k % 4 == 1);
		}
		// It grows out of the land (played once): the wheel spreading, the tree rising.
		SpellArt.Anim grow = m.anim("grow", 1.2, null);
		grow.sample("disc", "scale", 0, 1.2, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.2));
			return new double[] {s, 1, s};
		});
		grow.sample("tree", "scale", 0, 1.2, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - 0.2) / 1.0)));
			return new double[] {s, s, s};
		});
		grow.sample("drift", "scale", 0, 1.2, 4, t -> {
			double s = Math.max(0.02, SpellArt.clamp01((t - 0.5) / 0.7));
			return new double[] {s, s, s};
		});
		// It turns (looped): the wheel round once, its drift the other way, the tree stirring.
		double T = 8;
		SpellArt.Anim turn = m.anim("turn", T, "true");
		turn.sample("disc", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		turn.sample("drift", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		turn.sample("tree", "rotation", 0, T, 8, t -> new double[] {2 * Math.sin(2 * Math.PI * t / T * 2), 0, 2 * Math.cos(2 * Math.PI * t / T * 2)});
		// Its last turn (played once): the wheel spun round twice, swelling, the tree flaring.
		SpellArt.Anim flip = m.anim("flip", 1.0, null);
		flip.scale("disc", 0, 1);
		flip.scale("disc", 0.4, 1.12, 1, 1.12);
		flip.scale("disc", 1.0, 1);
		flip.scale("tree", 0, 1);
		flip.scale("tree", 0.35, 1.2);
		flip.scale("tree", 1.0, 1);
	}

	// --- Undying Ember ---

	/**
	 * The ember (on a solid): a coal crusted in plates, each glowing dull crimson to flame toward its middle, the seams between them white-hot
	 * gold; all of it alight.
	 */
	static SpellArt.Painter emberCore(int seed) {
		return t -> {
			double x = t.px / 1.6, y = t.py / 1.6, z = t.pz / 1.6;
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
			double gap = d2 - d1;
			if (gap < 0.12) return SpellArt.glow(SpellArt.mix(Fire.WHITE_FIRE, Fire.GOLD_LIGHT, gap / 0.12), 0.98);
			double heat = SpellArt.clamp01(0.3 + 0.45 * (1 - d1) + 0.2 * SpellArt.noise3(t.px * 0.8, t.py * 0.8, t.pz * 0.8, seed + 5));
			return SpellArt.glow(SpellArt.ramp(heat, Fire.EMBER, Fire.CRIMSON, Fire.VERMILION, Fire.FLAME), 0.96);
		};
	}

	/** The ember's haze (on a sheet through it): a round glow of its light, gold at its heart fading out through flame to nothing. */
	static SpellArt.Painter emberHaze() {
		return t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 0.98) return SpellArt.CLEAR;
			double a = (1 - r) * (1 - r);
			return a < 0.04 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(Fire.FLAME, Fire.GOLD_LIGHT, 1 - r), 0.55 * a);
		};
	}

	/**
	 * The Undying Ember, its heart at the origin: the ember itself ("core"), a haze of its light round it ("haze"), and the phoenix's feathers of
	 * flame it is wrapped in ("feathers": two rings of them, flaring out), and three long tail feathers streaming back ("tail").
	 */
	static void undyingEmber(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 0, 0);
		Void.ball(core, 3.2, emberCore(1601), 3);
		SpellArt.Bone haze = m.bone("haze", "root", 0, 0, 0);
		SpellArt.sheet(haze, 0, 0, 0, 16, 16, 0, 0, 0, emberHaze()).density(2);
		SpellArt.sheet(haze, 0, 0, 0, 16, 16, 90, 0, 0, emberHaze()).density(2);
		SpellArt.sheet(haze, 0, 0, 0, 16, 16, 90, 90, 0, emberHaze()).density(2);
		SpellArt.Bone feathers = m.bone("feathers", "root", 0, 0, 0);
		for (int k = 0; k < 8; k++) {
			double th = 45.0 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 2.2, 0.8, Math.cos(Math.toRadians(th)) * 2.2);
			SpellArt.shared(m, feathers, "feather_up", false, base, 13, 5, 90 - 28, th + 180, 0, Fire.flameFeather(3, 1603), 3, k % 2 == 1);
		}
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k + 30;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 2.2, -0.8, Math.cos(Math.toRadians(th)) * 2.2);
			SpellArt.shared(m, feathers, "feather_down", false, base, 10, 4.5, 90 + 30, th + 180, 0, Fire.flameFeather(2, 1605), 3, k % 2 == 0);
		}
		SpellArt.Bone tail = m.bone("tail", "root", 0, 0, 2);
		for (int k = -1; k <= 1; k++) {
			SpellArt.shared(m, tail, "tail_feather", false, SpellArt.v3(k * 1.2, 0, 2.5), 22, 5, -95 + k * 3, k * 14, 0, Fire.flameFeather(3, 1607), 2.5, k > 0);
		}
		// Its flight (looped): feathers wheeling round it, the ember breathing, its tail streaming.
		double T = 0.8;
		SpellArt.Anim fly = m.anim("fly", T, "true");
		fly.sample("feathers", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		fly.sample("core", "scale", 0, T, 8, t -> {
			double s = 1 + 0.08 * Math.sin(2 * Math.PI * t / T);
			return new double[] {s, s, s};
		});
		fly.sample("haze", "scale", 0, T, 8, t -> {
			double s = 1 + 0.12 * Math.sin(2 * Math.PI * t / T + 1);
			return new double[] {s, s, s};
		});
		fly.sample("tail", "rotation", 0, T, 8, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T), 6 * Math.sin(2 * Math.PI * t / T * 2), 0});
		// It flares (played once): every feather thrown wide, the ember blazing up, then settling.
		SpellArt.Anim flare = m.anim("flare", 0.7, null);
		flare.scale("feathers", 0, 1);
		flare.scale("feathers", 0.15, 1.8);
		flare.scale("feathers", 0.7, 1);
		flare.scale("core", 0, 1);
		flare.scale("core", 0.12, 1.6);
		flare.scale("core", 0.7, 1);
		flare.scale("haze", 0, 1);
		flare.scale("haze", 0.15, 2.6);
		flare.scale("haze", 0.7, 1);
		// It dims (played once, held): its fire sunk low, its feathers drawn in round it, only the ember glowing (it never quite goes out).
		SpellArt.Anim dim = m.anim("dim", 0.5, "hold_on_last_frame");
		dim.scale("feathers", 0, 1);
		dim.scale("feathers", 0.5, 0.3);
		dim.scale("haze", 0, 1);
		dim.scale("haze", 0.5, 0.4);
		dim.scale("tail", 0, 1);
		dim.scale("tail", 0.5, 0.3);
		dim.scale("core", 0, 1);
		dim.scale("core", 0.5, 0.75);
	}

	// --- Pyre of the Heavens ---

	static final double PYRE_R = 46;
	static final int PYRES = 10;

	/** A log of the pyre (on a solid, its length along x or z): charred bark, cracked, its cracks glowing with the fire in it; burnt brightest beneath. */
	static SpellArt.Painter pyreLog(int seed) {
		return t -> {
			double along = Math.max(t.lx, t.lz), n = SpellArt.fbm(along * 0.7 + seed, (t.ly + t.fx) * 2, seed, 3);
			double crack = 1 - Math.abs(2 * SpellArt.fbm(along * 0.25 + seed, t.fy * 3 + t.fx * 2, seed + 5, 2) - 1);
			if (crack > 0.9) return SpellArt.glow(SpellArt.mix(Fire.FLAME, Fire.GOLD, (crack - 0.9) * 10), 0.96);
			if (t.ny < -0.5) return SpellArt.glow(SpellArt.mix(Fire.CRIMSON, Fire.FLAME, n), 0.9);
			return SpellArt.col(SpellArt.mix(0x1A120E, 0x4A3628, 0.2 + 0.7 * n));
		};
	}

	/**
	 * The Pyre of the Heavens, its heart at the origin on the ground, {@link #PYRE_R} pixels round: a ring of {@link #PYRES} cribs of logs stacked
	 * crosswise and burning ("crib_0".."crib_9", each turned so it falls inward, its flames "fire_0".."fire_9"), and the walls of fire between them
	 * ("walls").
	 */
	static void heavenPyre(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int k = 0; k < PYRES; k++) {
			double th = 360.0 * k / PYRES, x = Math.sin(Math.toRadians(th)) * PYRE_R, z = Math.cos(Math.toRadians(th)) * PYRE_R;
			m.bone("crib_" + k + "_turn", "root", x, 0, z, 0, th, 0);
			SpellArt.Bone crib = m.bone("crib_" + k, "crib_" + k + "_turn", x, 0, z);
			for (int layer = 0; layer < 4; layer++) {
				double y = 1.2 + layer * 2.3;
				for (int s = -1; s <= 1; s += 2) {
					if (layer % 2 == 0) crib.box(x, y, z + s * 3.2, 10, 2.3, 2.3, pyreLog(1701 + layer * 2 + (s > 0 ? 1 : 0)));
					else crib.box(x + s * 3.2, y, z, 2.3, 2.3, 10, pyreLog(1711 + layer * 2 + (s > 0 ? 1 : 0)));
				}
			}
			SpellArt.Bone fire = m.bone("fire_" + k, "crib_" + k, x, 9, z);
			for (int j = 0; j < 3; j++) {
				double[] base = SpellArt.v3(x + (j - 1) * 2.5, 8.5, z + (j - 1) * 1.2);
				SpellArt.shared(m, fire, "pyre_flame", false, base, 18 - Math.abs(j - 1) * 4, 9, 0, th + j * 60, 0, Fire.flame(3, 1721), 1.5, j == 1);
			}
		}
		SpellArt.Bone walls = m.bone("walls", "root", 0, 0, 0);
		for (int k = 0; k < PYRES; k++) {
			double phi = 360.0 * (k + 0.5) / PYRES, chord = 2 * PYRE_R * Math.sin(Math.PI / PYRES);
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * PYRE_R, 0, Math.cos(Math.toRadians(phi)) * PYRE_R);
			SpellArt.shared(m, walls, "pyre_wall", false, base, 22, chord * 1.1, 0, phi - 90, 0, Fire.flame(6, 1731), 1, k % 2 == 1);
		}
		// It rises (played once): each crib heaving up out of the land in turn, the walls of fire leaping up between them.
		SpellArt.Anim rise = m.anim("rise", 1.0, null);
		for (int k = 0; k < PYRES; k++) {
			int kk = k;
			rise.sample("crib_" + k, "position", 0, 1.0, 5, t -> new double[] {0, -14 * (1 - SpellArt.easeOut(SpellArt.clamp01((t - kk * 0.04) / 0.55))), 0});
		}
		rise.sample("walls", "scale", 0, 1.0, 5, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - 0.35) / 0.65))), 1});
		// It burns (looped): the walls of fire heaving, each crib's flames leaping.
		double T = 1.0;
		SpellArt.Anim burn = m.anim("burn", T, "true");
		burn.sample("walls", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.14 * Math.sin(2 * Math.PI * t / T * 2), 1});
		for (int k = 0; k < PYRES; k++) {
			int kk = k;
			burn.sample("fire_" + k, "scale", 0, T, 8, t -> {
				double s = 1 + 0.18 * Math.sin(2 * Math.PI * t / T * 2 + kk * 1.3);
				return new double[] {1 + 0.08 * Math.sin(2 * Math.PI * t / T + kk), s, 1};
			});
		}
		// It collapses (played once, held): the cribs toppling in on the heart, the walls of fire sinking.
		SpellArt.Anim collapse = m.anim("collapse", 1.2, "hold_on_last_frame");
		for (int k = 0; k < PYRES; k++) {
			int kk = k;
			collapse.sample("crib_" + k, "rotation", 0, 1.2, 6, t -> new double[] {78 * SpellArt.easeIn(SpellArt.clamp01((t - kk * 0.03) / 0.9)), 0, 0});
		}
		collapse.sample("walls", "scale", 0, 1.2, 5, t -> new double[] {1, Math.max(0.02, 1 - SpellArt.easeIn(t / 1.2)), 1});
	}

	// --- Gravestone Falls ---

	static final int STELE = 0x8C8A84, STELE_LIGHT = 0xBDBAB0, STELE_DARK = 0x4E4C48, STELE_GLYPH = 0xFFC860;

	/**
	 * The stele's stone (painted where it lies): grey, lichened, weathered; on its broad faces a border carved round it and a column of five great
	 * glyphs down its middle, cut deep and burning with the earth's gold (the names of the dead); cracks through it here and there.
	 */
	static SpellArt.Painter steleStone(double bottom, double top, int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3);
			int c = SpellArt.ramp(SpellArt.clamp01(0.45 + (n - 0.5) * 0.7 + 0.15 * Math.max(0, t.ny)), STELE_DARK, STELE, STELE_LIGHT);
			if (SpellArt.noise3(t.px * 0.5, t.py * 0.5, t.pz * 0.5, seed + 2) > 0.7) c = SpellArt.mix(c, Earth.MOSS, 0.5);
			double crack = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.2, t.py * 0.12, t.pz * 0.2, seed + 4, 2) - 1);
			if (crack > 0.965) return SpellArt.col(Earth.CRACK);
			boolean broad = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (broad) {
				double ax = Math.abs(t.px), y = t.py;
				// The border: a groove a little in from the edge.
				if (Math.abs(ax - 7.2) < 0.45 && y > bottom + 1.5 && y < top - 1) return SpellArt.col(STELE_DARK);
				if (ax < 3.6 && y > bottom + 3 && y < top - 4) {
					double cell = (y - bottom - 3) / (top - 7 - bottom) * 5;
					int ci = (int) Math.floor(cell);
					if (Fire.glyph(4 - ci, seed, (t.px + 3.6) / 7.2, 1 - (cell - ci))) return SpellArt.glow(SpellArt.mix(STELE_GLYPH, Earth.MOLTEN_CORE, 0.25), 0.97);
				}
			}
			return SpellArt.col(c);
		};
	}

	/** The stone tortoise's shell (painted where it lies): plates of six sides, their seams cut in, each domed and lit toward its middle. */
	static SpellArt.Painter shellStone(int seed) {
		return t -> {
			double x = t.px / 3.2, z = t.pz / 3.2 + (Math.floorMod((int) Math.floor(t.px / 3.2), 2) == 0 ? 0 : 0.5);
			double fx = x - Math.floor(x), fz = z - Math.floor(z);
			double edge = Math.min(Math.min(fx, 1 - fx), Math.min(fz, 1 - fz));
			if (edge < 0.08) return SpellArt.col(STELE_DARK);
			double n = SpellArt.noise3(t.px * 0.6, t.py * 0.6, t.pz * 0.6, seed);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.35 + edge * 1.2 + (n - 0.5) * 0.3), STELE_DARK, STELE, STELE_LIGHT));
		};
	}

	/**
	 * The gravestone, its foot at the origin: a stele of grey stone written with the names of the dead in burning gold ("stele", its top rounded),
	 * borne on the back of a stone tortoise ("tortoise": its shell of plates, its head reaching out before it with eyes of the earth's gold, its four
	 * feet), on a plinth of fitted rock.
	 */
	static void gravestone(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone tortoise = m.bone("tortoise", "root", 0, 0, 0);
		tortoise.box(0, 1.5, 0, 30, 3, 32, shellStone(1801));
		Vessel.lathe(tortoise, 3, 10, 1, y -> 12.5 * Math.sqrt(Math.max(0, 1 - Math.pow((y - 3) / 7.5, 2))), shellStone(1803));
		SpellArt.Painter stone = steleStone(0, 0, 1805);
		tortoise.box(0, 6, -14.5, 6, 5.5, 7, Earth.moltenEye(stone));
		tortoise.box(0, 8.6, -17.2, 4.4, 1.6, 2, stone);
		for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) tortoise.box(sx * 10, 4.5, sz * 9, 5, 3, 6, stone).rot(0, sx * sz * 20, 0);
		tortoise.box(0, 4.5, 13.5, 3, 2.5, 6, stone).rot(-15, 0, 0);
		SpellArt.Bone stele = m.bone("stele", "tortoise", 0, 10, 0);
		double bottom = 10, top = 10 + 34;
		SpellArt.Painter face = steleStone(bottom, top + 8, 1807);
		stele.box(0, (bottom + top) / 2, 0, 18, 34, 4.5, face).density(1.5);
		Vessel.disc(stele, 0, 9, 4.5, face, 1.5);
		// The disc stands at the stele's top: its lower half within the slab, its upper its rounded crown.
		for (SpellArt.Cube c : stele.cubes.subList(1, stele.cubes.size())) {
			c.origin[1] += top;
			c.pivot[1] += top;
		}
		stele.box(0, bottom + 1, 0, 20, 2, 6, face);
		// It falls (looped): swaying a little as it drops.
		double T = 1.2;
		SpellArt.Anim fall = m.anim("fall", T, "true");
		fall.sample("root", "rotation", 0, T, 8, t -> new double[] {3 * Math.sin(2 * Math.PI * t / T), 0, 2 * Math.cos(2 * Math.PI * t / T)});
		// It lands (played once, held): it shudders and settles, upright.
		SpellArt.Anim land = m.anim("land", 0.6, "hold_on_last_frame");
		land.scale("root", 0, 1);
		land.scale("root", 0.1, 1.06, 0.92, 1.06);
		land.scale("root", 0.3, 0.98, 1.03, 0.98);
		land.scale("root", 0.6, 1);
		land.rot("root", 0, 0, 0, 0);
		land.rot("root", 0.6, 0, 0, 0);
		// It sinks (played once, held): down into the land it struck, leaning as it goes.
		SpellArt.Anim sink = m.anim("sink", 2.0, "hold_on_last_frame");
		sink.sample("root", "position", 0, 2.0, 8, t -> new double[] {0, -56 * SpellArt.easeIn(t / 2.0), 0});
		sink.sample("root", "rotation", 0, 2.0, 6, t -> new double[] {5 * SpellArt.easeOut(t / 2.0), 0, -3 * SpellArt.easeOut(t / 2.0)});
	}

	// --- Buried World ---

	/**
	 * A standing stone's grey granite (painted where it lies): weathered, lichened, mossed toward its foot; on its face toward the heart (its -z)
	 * a spiral carved and a column of notched runes beside it, glowing faintly with the earth's gold.
	 */
	static SpellArt.Painter menhir(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.35, t.py * 0.25, t.pz * 0.35, seed, 3);
			int c = SpellArt.ramp(SpellArt.clamp01(0.42 + (n - 0.5) * 0.8 + 0.12 * Math.max(0, t.ny)), Earth.STONE_DARK, Earth.STONE, Earth.STONE_LIGHT);
			double lichen = SpellArt.noise3(t.px * 0.7, t.py * 0.7, t.pz * 0.7, seed + 3);
			if (lichen > 0.72) c = SpellArt.mix(c, 0xB8B070, 0.55);
			if (t.py < 6 && SpellArt.noise3(t.px * 0.5, t.py * 0.9, t.pz * 0.5, seed + 4) > 0.45 - t.py * 0.04) c = SpellArt.mix(c, Earth.MOSS, 0.7);
			if (t.f == SpellArt.Face.FRONT) {
				// A spiral cut into it, turning in three times; and down its left edge a line of notched runes.
				double x = t.px, y = t.py - 17, r = Math.hypot(x, y), a = Math.atan2(y, x), pitch = 0.95;
				double turn = (r - pitch * (a + Math.PI) / (2 * Math.PI)) / pitch, spiral = (turn - Math.rint(turn)) * pitch;
				if (r < 3.4 && r > 0.4 && Math.abs(spiral) < 0.24) return SpellArt.glow(SpellArt.mix(Earth.GOLD_PALE, STELE_GLYPH, 0.4), 0.8);
				double ry = t.py - 5, ex = t.px + 2.6;
				if (ry > 0 && ry < 20 && Math.abs(ex) < 0.22) return SpellArt.glow(SpellArt.mix(Earth.GOLD_PALE, STELE_GLYPH, 0.4), 0.7);
				int notch = (int) Math.floor(ry / 1.4);
				if (ry > 0 && ry < 20 && ry - notch * 1.4 < 0.32 && ex > 0 && ex < 0.5 + 1.1 * SpellArt.hash(notch, 3, seed)) return SpellArt.glow(SpellArt.mix(Earth.GOLD_PALE, STELE_GLYPH, 0.4), 0.7);
			}
			return SpellArt.col(c);
		};
	}

	/**
	 * A standing stone, its foot at the origin, 34 pixels tall: rough-hewn, leaning a little, narrowing to a blunt top, its carved face toward its
	 * -z ("stone", tipping about its front foot), rubble round its foot.
	 */
	static void standingStone(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone stone = m.bone("stone", "root", 0, 0, -3);
		SpellArt.Painter p = menhir(1901);
		double[][] blocks = {{0, 3, 0, 9.5, 6, 7, 0, 0, 1}, {0.2, 9, 0.1, 9, 6.5, 6.6, 1, 3, -1}, {-0.2, 15.5, 0, 8.4, 7, 6.2, -1, -2, 1.5},
				{0.3, 22, 0.2, 7.6, 6.5, 5.8, 1.5, 4, -1}, {0, 27.5, 0, 6.6, 5, 5, -1, -3, 2}, {0.4, 31.5, 0.1, 5, 3.5, 4, 2, 6, -2}};
		for (double[] b : blocks) stone.box(b[0], b[1], b[2], b[3], b[4], b[5], p).rot(b[6], b[7], b[8]);
		SpellArt.Bone rubble = m.bone("rubble", "root", 0, 0, 0);
		SpellArt.Painter rock = menhir(1903);
		for (int k = 0; k < 7; k++) {
			double th = Math.toRadians(52 * k + 10), r = 6 + (k % 3);
			rubble.box(Math.sin(th) * r, 0.8, Math.cos(th) * r, 2.4 + (k % 2), 1.6 + (k % 3) * 0.5, 2.2, rock).rot(k * 11, k * 37, k * 7);
		}
		// It rises (played once): heaving up out of the land, shuddering.
		SpellArt.Anim rise = m.anim("rise", 1.2, null);
		rise.sample("stone", "position", 0, 1.2, 10, t -> new double[] {0.4 * Math.sin(t * 60) * (1 - t / 1.2), -36 * (1 - SpellArt.easeOut(t / 1.2)), 0});
		rise.sample("rubble", "scale", 0, 1.2, 4, t -> {
			double s = Math.max(0.02, SpellArt.clamp01((t - 0.3) / 0.6));
			return new double[] {s, s, s};
		});
		// It stands (looped): the land under it trembling.
		double T = 0.5;
		SpellArt.Anim stand = m.anim("stand", T, "true");
		stand.sample("stone", "rotation", 0, T, 8, t -> new double[] {0.5 * Math.sin(2 * Math.PI * t / T * 2), 0, 0.4 * Math.sin(2 * Math.PI * t / T)});
		// It topples (played once, held): over onto its carved face, toward the heart.
		SpellArt.Anim topple = m.anim("topple", 1.0, "hold_on_last_frame");
		topple.sample("stone", "rotation", 0, 1.0, 8, t -> new double[] {88 * SpellArt.easeIn(t / 1.0) + (t > 0.85 ? -3 * Math.sin((t - 0.85) / 0.15 * Math.PI) : 0), 0, 0});
	}

	// --- Frozen Moon in the Well ---

	static final int MOON_ICE = 0xE4F6FF, MOON_ICE_SHADE = 0x9CCCE8, MOON_GLOW = 0xBEEFFF;

	/** The moon's ice (painted where it lies): pale, frost veined white through it, its shadow the cold blue of deep ice; glowing. */
	static SpellArt.Painter moonIce(int seed) {
		return t -> {
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3) - 1), 10);
			if (vein > 0.6) return SpellArt.glow(0xFFFFFF, 0.97);
			double n = SpellArt.fbm3(t.px * 0.15, t.py * 0.15, t.pz * 0.15, seed + 2, 2);
			int c = SpellArt.mix(MOON_ICE_SHADE, MOON_ICE, SpellArt.clamp01(0.45 + 0.4 * n + 0.2 * Math.max(0, t.ny) + 0.15 * Math.max(0, -t.nz)));
			return SpellArt.glow(c, 0.95);
		};
	}

	/**
	 * The well's water under the moon (on a sheet): still and black-blue, the moon's crescent shining back out of its middle, rings of ripple
	 * spreading faintly round it, frost creeping in from its rim in feathers of ice.
	 */
	static SpellArt.Painter moonPool(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.99) return SpellArt.CLEAR;
			// The crescent reflected: the outer disc less the inner, offset.
			double cx = x / 0.36, cz = z / 0.36;
			if (Math.hypot(cx, cz) < 1 && Math.hypot(cx - 0.42, cz) > 0.82) return SpellArt.glow(SpellArt.mix(0xFFFFFF, MOON_GLOW, Math.hypot(cx, cz) * 0.5), 0.95);
			double frost = SpellArt.fbm(Math.atan2(z, x) * 4 + seed, r * 12, seed, 3);
			if (r > 0.78 + 0.18 * frost) return SpellArt.glow(SpellArt.mix(MOON_ICE, MOON_ICE_SHADE, (1 - r) * 3), 0.85);
			double ripple = Math.pow(Math.abs(Math.sin(r * 26 - seed)), 18) * (1 - r);
			int c = SpellArt.mix(SpellArt.mix(Water.ABYSS, Water.NAVY, 0.4 + 0.4 * SpellArt.fbm(x * 3 + seed, z * 3, seed + 1, 2)), MOON_GLOW, ripple * 0.6 + 0.15 * (1 - r));
			return SpellArt.glow(c, 0.8);
		};
	}

	/** A halo of cold light round the moon (on a sheet standing behind it): a soft ring, clear within and without. */
	static SpellArt.Painter moonHalo() {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98 || r < 0.55) return SpellArt.CLEAR;
			double a = Math.pow(1 - Math.abs(r - 0.78) / 0.22, 2);
			return SpellArt.glow(SpellArt.mix(MOON_GLOW, 0xFFFFFF, a), 0.12 + 0.45 * a);
		};
	}

	static final double MOON_HIGH = 44, MOON_R = 16;
	static final int MOON_PIECES = 7;

	/**
	 * The Frozen Moon in the Well: the moon's crescent of ice hanging {@link #MOON_HIGH} pixels over the origin, facing -z, its points to +x, in
	 * {@link #MOON_PIECES} pieces it shatters into ("moon_0".., in "moon"), a halo of cold light behind it ("halo"); and on the ground its reflection
	 * in the well, a pool of still black water ("pool").
	 */
	static void frozenMoon(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone moon = m.bone("moon", "root", 0, MOON_HIGH, 0);
		SpellArt.Painter ice = moonIce(2001);
		int segs = 28;
		for (int i = 0; i < segs; i++) {
			int piece = i * MOON_PIECES / segs;
			double a0 = -140 + 280.0 * i / segs, a1 = -140 + 280.0 * (i + 1) / segs, am = (a0 + a1) / 2;
			double w = 1.2 + 5.2 * Math.pow(Math.cos(Math.toRadians(am * 90 / 140)), 0.8);
			double rm = MOON_R - w / 2, ang = Math.toRadians(am);
			double x = -Math.cos(ang) * rm, y = MOON_HIGH + Math.sin(ang) * rm;
			String name = "moon_" + piece;
			SpellArt.Bone b = m.get(name) != null ? m.get(name) : m.bone(name, "moon", x, y, 0);
			double chord = 2 * Math.PI * MOON_R * (a1 - a0) / 360 * 1.12;
			// Along the arc: the box's length (its y) turned onto the tangent there, (sin, cos) of the angle.
			b.box(x, y, 0, w, chord, 3 + w * 0.3, ice).rot(0, 0, am);
		}
		SpellArt.Bone halo = m.bone("halo", "moon", 0, MOON_HIGH, 0);
		SpellArt.sheet(halo, 0, MOON_HIGH, 2.5, 52, 52, 90, 0, 0, t -> moonHalo().paint(t)).density(1.2);
		SpellArt.Bone pool = m.bone("pool", "root", 0, 0, 0);
		SpellArt.sheet(pool, 0, 0.2, 0, 60, 60, 0, 0, 0, moonPool(2003)).density(2);
		// It rises (played once): the moon coming clear out of the cold, the well spreading under it.
		SpellArt.Anim rise = m.anim("rise", 1.2, null);
		rise.sample("moon", "scale", 0, 1.2, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.2));
			return new double[] {s, s, s};
		});
		rise.sample("moon", "position", 0, 1.2, 6, t -> new double[] {0, -14 * (1 - SpellArt.easeOut(t / 1.2)), 0});
		rise.sample("pool", "scale", 0, 1.2, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.2));
			return new double[] {s, 1, s};
		});
		// It shines (looped): the moon swaying a little, its halo breathing; the pool's ripples turning.
		double T = 3;
		SpellArt.Anim shine = m.anim("shine", T, "true");
		shine.sample("moon", "rotation", 0, T, 8, t -> new double[] {0, 0, 4 * Math.sin(2 * Math.PI * t / T)});
		shine.sample("moon", "position", 0, T, 8, t -> new double[] {0, 1.2 * Math.sin(2 * Math.PI * t / T * 2), 0});
		shine.sample("halo", "scale", 0, T, 8, t -> {
			double s = 1 + 0.08 * Math.sin(2 * Math.PI * t / T * 2);
			return new double[] {s, s, 1};
		});
		shine.sample("pool", "rotation", 0, T, 8, t -> new double[] {0, 120 * t / T, 0});
		// It shatters (played once, held): the crescent broken into its pieces, flung out and falling, gone to nothing; its halo with it.
		SpellArt.Anim shatter = m.anim("shatter", 0.9, "hold_on_last_frame");
		for (int k = 0; k < MOON_PIECES; k++) {
			double am = Math.toRadians(-140 + 280.0 * (k + 0.5) / MOON_PIECES), ox = -Math.cos(am) * 16, oy = Math.sin(am) * 16;
			int kk = k;
			shatter.sample("moon_" + k, "position", 0, 0.9, 6, t -> {
				double f = t / 0.9;
				return new double[] {ox * f, oy * f - 30 * f * f, (kk % 2 == 0 ? 6 : -6) * f};
			});
			shatter.sample("moon_" + k, "rotation", 0, 0.9, 6, t -> new double[] {200 * t * (kk % 2 == 0 ? 1 : -1), 140 * t, 90 * t});
			shatter.sample("moon_" + k, "scale", 0, 0.9, 6, t -> {
				double s = Math.max(0.02, 1 - SpellArt.easeIn(t / 0.9));
				return new double[] {s, s, s};
			});
		}
		shatter.scale("halo", 0, 1);
		shatter.scale("halo", 0.3, 1.6);
		shatter.scale("halo", 0.9, 0.02);
	}

	// --- The Frozen Moon Falls ---

	/**
	 * The moon of ice (painted where it lies): pale ice, its seas darker blue, craters on it rimmed bright, cracks of cyan light through it; glowing.
	 */
	static SpellArt.Painter iceMoonSurface(int seed) {
		return t -> {
			double x = t.px / 5.5, y = t.py / 5.5, z = t.pz / 5.5;
			int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
			double best = 9, bestR = 0;
			for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
				int cx = ix + dx, cy = iy + dy, cz = iz + dz;
				if (SpellArt.hash3(cx, cy, cz, seed + 9) > 0.55) continue;
				double px = cx + SpellArt.hash3(cx, cy, cz, seed), py = cy + SpellArt.hash3(cx, cy, cz, seed + 1), pz = cz + SpellArt.hash3(cx, cy, cz, seed + 2);
				double r = 0.2 + 0.3 * SpellArt.hash3(cx, cy, cz, seed + 3), d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz)) / r;
				if (d < best) {
					best = d;
					bestR = r;
				}
			}
			double crack = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.12, t.py * 0.12, t.pz * 0.12, seed + 5, 3) - 1);
			if (crack > 0.95) return SpellArt.glow(SpellArt.mix(Water.CYAN, 0xFFFFFF, (crack - 0.95) * 12), 0.98);
			double sea = SpellArt.fbm3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed + 7, 3);
			int c = SpellArt.mix(MOON_ICE, MOON_ICE_SHADE, SpellArt.smooth(0.5, 0.7, sea) * 0.7);
			if (best < 1) c = best > 0.8 ? SpellArt.mix(c, 0xFFFFFF, 0.6) : SpellArt.mix(c, Water.TEAL, 0.25 * (1 - best));
			return SpellArt.glow(SpellArt.shade(c, -0.12 * Math.max(0, -t.ny)), 0.96);
		};
	}

	/** A trail of frost-mist (on a blade, its root at v 0): white drawn out to nothing, cyan at its edges. */
	static SpellArt.Painter frostTrail(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.48 * Math.pow(Math.max(0, 1 - v), 0.8) * (0.8 + 0.2 * SpellArt.noise(v * 7, seed, seed));
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double edge = (half - Math.abs(u)) / Math.max(0.02, half);
			if (edge < 0.18) return SpellArt.glow(MOON_GLOW, 0.3 + 0.5 * (1 - v));
			return SpellArt.glow(SpellArt.mix(0xFFFFFF, MOON_ICE, v), Math.max(0.05, 0.75 - 0.7 * v));
		};
	}

	static final double ICE_MOON_R = 20;

	/**
	 * The moon of ice that falls, its heart at the origin, {@link #ICE_MOON_R} pixels round: the moon ("moon", turning as it falls), its trail of
	 * frost streaming up behind it ("trail"), a halo of cold light round it ("halo"), and the shards it breaks into ("shard_0".., unseen till it
	 * shatters).
	 */
	static void iceMoon(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone moon = m.bone("moon", "root", 0, 0, 0);
		Vessel.lathe(moon, -ICE_MOON_R, ICE_MOON_R, 2, y -> Math.sqrt(Math.max(0, ICE_MOON_R * ICE_MOON_R - y * y)), iceMoonSurface(2101));
		SpellArt.Bone trail = m.bone("trail", "root", 0, 0, 0);
		for (int k = 0; k < 10; k++) {
			double th = 36.0 * k, out = ICE_MOON_R * 0.7;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * out, ICE_MOON_R * 0.5, Math.cos(Math.toRadians(th)) * out);
			SpellArt.shared(m, trail, "frost_trail", false, base, 38 + (k % 3) * 8, 12, 8 + (k % 2) * 6, th + 180, 0, frostTrail(2103), 0.8, k % 2 == 0);
		}
		SpellArt.Bone halo = m.bone("halo", "root", 0, 0, 0);
		SpellArt.sheet(halo, 0, 0, 0, 64, 64, 90, 0, 0, moonHalo()).density(0.8);
		SpellArt.sheet(halo, 0, 0, 0, 64, 64, 90, 90, 0, moonHalo()).density(0.8);
		for (int k = 0; k < 10; k++) {
			double th = Math.toRadians(36 * k + 7), el = Math.toRadians((k % 3 - 1) * 35);
			double[] d = SpellArt.v3(Math.cos(el) * Math.sin(th), Math.sin(el), Math.cos(el) * Math.cos(th));
			SpellArt.Bone s = m.bone("shard_" + k, "root", d[0] * 10, d[1] * 10, d[2] * 10);
			SpellArt.limb(s, SpellArt.times(d, 6), SpellArt.times(d, 6 + 10 + (k % 3) * 3), 4 + (k % 2) * 2, 3, Water.ice());
		}
		// It falls (looped): turning over and over, its trail streaming.
		double T = 2;
		SpellArt.Anim fall = m.anim("fall", T, "true");
		fall.sample("moon", "rotation", 0, T, 8, t -> new double[] {12 * Math.sin(2 * Math.PI * t / T), 360 * t / T, 0});
		fall.sample("trail", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 3), 1});
		for (int k = 0; k < 10; k++) {
			fall.scale("shard_" + k, 0, 0.02);
			fall.scale("shard_" + k, T, 0.02);
		}
		// It shatters (played once, held): swelling, bursting into its shards, which fly out and fall away to nothing.
		SpellArt.Anim shatter = m.anim("shatter", 0.8, "hold_on_last_frame");
		shatter.scale("moon", 0, 1);
		shatter.scale("moon", 0.12, 1.25);
		shatter.scale("moon", 0.25, 0.02);
		shatter.scale("moon", 0.8, 0.02);
		shatter.scale("trail", 0, 1);
		shatter.scale("trail", 0.25, 0.02);
		shatter.scale("trail", 0.8, 0.02);
		shatter.scale("halo", 0, 1);
		shatter.scale("halo", 0.2, 2.2);
		shatter.scale("halo", 0.8, 0.02);
		for (int k = 0; k < 10; k++) {
			double th = Math.toRadians(36 * k + 7), el = Math.toRadians((k % 3 - 1) * 35);
			double[] d = SpellArt.v3(Math.cos(el) * Math.sin(th), Math.sin(el), Math.cos(el) * Math.cos(th));
			int kk = k;
			shatter.sample("shard_" + k, "scale", 0, 0.8, 6, t -> {
				double s = t < 0.1 ? 0.02 : Math.max(0.02, 1.2 * (1 - SpellArt.easeIn((t - 0.1) / 0.7)));
				return new double[] {s, s, s};
			});
			shatter.sample("shard_" + k, "position", 0, 0.8, 6, t -> {
				double f = t / 0.8;
				return new double[] {d[0] * 44 * f, d[1] * 44 * f - 26 * f * f, d[2] * 44 * f};
			});
			shatter.sample("shard_" + k, "rotation", 0, 0.8, 6, t -> new double[] {300 * t * (kk % 2 == 0 ? 1 : -1), 200 * t, 0});
		}
	}

	// --- Sword Tomb Awakens ---

	/** The tomb's earth (painted where it lies): dark grave-soil, stones in it, a little grass grown over it. */
	static SpellArt.Painter graveSoil(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.4, t.py * 0.4, t.pz * 0.4, seed, 3);
			if (t.ny > 0.5 && SpellArt.noise3(t.px * 0.8, t.py, t.pz * 0.8, seed + 2) > 0.6) return SpellArt.col(SpellArt.mix(Earth.PINE, Earth.MOSS, n));
			if (SpellArt.hash3((int) Math.floor(t.px * 0.8), (int) Math.floor(t.py * 0.8), (int) Math.floor(t.pz * 0.8), seed + 4) > 0.86)
				return SpellArt.col(SpellArt.mix(Earth.STONE, Earth.STONE_LIGHT, n));
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.3 + (n - 0.5) * 0.8 + 0.2 * Math.max(0, t.ny)), 0x1C140E, 0x3A2C20, 0x5E4A36));
		};
	}

	/**
	 * A blade long buried (a limb, its point down): dark, rusted in patches, nicked; its two edges keen still, burning with a thread of white
	 * sword-light; its top (where it was snapped, for a broken one) jagged.
	 */
	static SpellArt.Painter tombBlade(boolean broken, int seed) {
		return t -> {
			double up = t.up(), u = t.across();
			if (broken && up > 0.9 - 0.08 * SpellArt.noise(u * 8 + seed, 1, seed)) return SpellArt.CLEAR;
			boolean broad = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (!broad || u < 0.1 || u > 0.9) return SpellArt.glow(SpellArt.mix(Metal.GLEAM, 0xFFFFFF, up), 0.95);
			double rust = SpellArt.fbm(u * 3 + seed, t.ly * 0.3, seed, 3);
			if (rust > 0.6) return SpellArt.col(SpellArt.mix(Earth.RUST, 0x5A2A12, (rust - 0.6) * 2.5));
			if (Math.abs(u - 0.5) < 0.06) return SpellArt.col(Metal.STEEL);
			return SpellArt.col(SpellArt.mix(0x1E2228, Metal.STEEL_DARK, 0.3 + 0.6 * SpellArt.noise(u * 4 + seed, t.ly * 0.5, seed + 1)));
		};
	}

	/** Tarnished bronze of a hilt: dark, its edges worn bright. */
	static SpellArt.Painter tarnish() {
		return t -> SpellArt.col(SpellArt.mix(0x3E3320, Metal.GOLD_DARK, Math.min(Math.min(t.fx, 1 - t.fx), Math.min(t.fy, 1 - t.fy)) < 0.15 ? 0.9 : 0.35));
	}

	/** How many swords stand in the grave's ring, and how far round it is (pixels). */
	static final int GRAVE_SWORDS = 24;
	static final double GRAVE_R = 20, GRAVE_RISE = 34;

	/** A sword of the grave, stuck point-down at {@code foot}: its blade (snapped short if {@code broken}), its guard, grip and pommel. */
	static void graveSword(SpellArt.Bone b, double[] foot, double length, boolean broken, int seed) {
		double[] top = SpellArt.add(foot, SpellArt.v3(0, length, 0));
		SpellArt.limb(b, foot, top, 2.6, 0.7, tombBlade(broken, seed));
		double[] guard = SpellArt.add(top, SpellArt.v3(0, 0.6, 0)), grip = SpellArt.add(top, SpellArt.v3(0, 4.8, 0));
		SpellArt.limb(b, SpellArt.add(guard, SpellArt.v3(-3.4, 0, 0)), SpellArt.add(guard, SpellArt.v3(3.4, 0, 0)), 1.3, 1.5, tarnish());
		SpellArt.limb(b, guard, grip, 1.3, 1.3, Metal.grip());
		b.box(grip[0], grip[1] + 0.7, grip[2], 2, 2, 2, tarnish()).rot(45, 45, 0);
	}

	/**
	 * The grave of swords, its heart at the origin on the land: a ring {@link #GRAVE_R} pixels round of {@link #GRAVE_SWORDS} swords of the fallen
	 * stuck point-down in the earth, some of them snapped ("sword_0".., each in its turn "sword_0_turn", so its lean is out from the heart),
	 * grave-earth heaped at their feet ("earth"), and a ring of their cold light on the land within ("light").
	 */
	static void swordTomb(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone earth = m.bone("earth", "root", 0, 0, 0);
		SpellArt.Painter soil = graveSoil(2201);
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			double th = Math.toRadians(360.0 * k / GRAVE_SWORDS + 7);
			earth.box(Math.sin(th) * GRAVE_R, 0.8, Math.cos(th) * GRAVE_R, 4.5, 1.6 + (k % 3) * 0.5, 4, soil).rot(0, Math.toDegrees(th) + (k % 4) * 11, 0);
		}
		SpellArt.Bone light = m.bone("light", "root", 0, 0, 0);
		SpellArt.sheet(light, 0, 0.2, 0, 2 * GRAVE_R + 10, 2 * GRAVE_R + 10, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2, ring = GRAVE_R / (GRAVE_R + 5);
			if (Math.abs(r - ring) < 0.02) return SpellArt.glow(0xFFFFFF, 0.85);
			if (r > ring || r < ring - 0.5) return SpellArt.CLEAR;
			double ang = Math.atan2(t.back() - 0.5, t.across() - 0.5), spoke = Math.pow(Math.abs(Math.cos(ang * GRAVE_SWORDS / 2.0)), 40);
			return SpellArt.glow(SpellArt.mix(TOMB_GLOW, 0xFFFFFF, spoke), 0.12 + 0.4 * spoke * (r - (ring - 0.5)) * 2);
		}).density(2);
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			double th = 360.0 * k / GRAVE_SWORDS, x = Math.sin(Math.toRadians(th)) * GRAVE_R, z = Math.cos(Math.toRadians(th)) * GRAVE_R;
			// Its turn: its -z out from the heart, so tipping its x leans its hilt out and its point in.
			m.bone("sword_" + k + "_turn", "root", x, 0, z, 0, th + 180, 0);
			SpellArt.Bone b = m.bone("sword_" + k, "sword_" + k + "_turn", x, 0, z);
			graveSword(b, SpellArt.v3(x, -6, z), 20 - (k % 3) * 2, k % 4 == 1, 2211 + k % 6);
		}
		// It erupts (played once): the ring of swords bursting up out of the land all round, their light flaring.
		SpellArt.Anim erupt = m.anim("erupt", 0.8, null);
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			int kk = k;
			erupt.sample("sword_" + k, "position", 0, 0.8, 5, t -> new double[] {0, -24 * (1 - SpellArt.easeOut(SpellArt.clamp01((t - (kk % 6) * 0.06) / 0.45))), 0});
		}
		erupt.sample("light", "scale", 0, 0.8, 4, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 0.8));
			return new double[] {s, 1, s};
		});
		// It stands (looped): its swords trembling in the earth, eager.
		double T = 0.6;
		SpellArt.Anim stand = m.anim("stand", T, "true");
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			int kk = k;
			stand.sample("sword_" + k, "rotation", 0, T, 6, t -> new double[] {12 + 1.6 * Math.sin(2 * Math.PI * t / T * 2 + kk), 0, 1.4 * Math.cos(2 * Math.PI * t / T + kk * 1.7)});
		}
		// They rise (played once, held): torn free of the earth, up over the ring, their points turned in on its heart.
		SpellArt.Anim rise = m.anim("rise", 1.0, "hold_on_last_frame");
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			int kk = k;
			rise.sample("sword_" + k, "position", 0, 1.0, 6, t -> new double[] {0, GRAVE_RISE * SpellArt.ease(SpellArt.clamp01((t - (kk % 4) * 0.04) / 0.8)), 0});
			rise.sample("sword_" + k, "rotation", 0, 1.0, 6, t -> new double[] {12 + 28 * SpellArt.ease(SpellArt.clamp01((t - (kk % 4) * 0.04) / 0.8)), 0, 0});
		}
		rise.scale("light", 0, 1);
		rise.scale("light", 1.0, 1);
		// They rain (played once, held): one after another round the ring, each plunging down into its heart, gone as it strikes.
		SpellArt.Anim rain = m.anim("rain", 1.5, "hold_on_last_frame");
		for (int k = 0; k < GRAVE_SWORDS; k++) {
			double t0 = k * 0.05;
			rain.sample("sword_" + k, "position", 0, 1.5, 15, t -> {
				double f = SpellArt.easeIn(SpellArt.clamp01((t - t0) / 0.3));
				return new double[] {0, GRAVE_RISE - (GRAVE_RISE + 4) * f, GRAVE_R * 0.85 * f};
			});
			rain.sample("sword_" + k, "rotation", 0, 1.5, 15, t -> new double[] {40 + 20 * SpellArt.clamp01((t - t0) / 0.3), 0, 0});
			rain.sample("sword_" + k, "scale", 0, 1.5, 15, t -> {
				double s = t < t0 + 0.3 ? 1 : Math.max(0.02, 1 - (t - t0 - 0.3) / 0.06);
				return new double[] {s, s, s};
			});
		}
		rain.sample("light", "scale", 0, 1.5, 4, t -> {
			double s = Math.max(0.02, 1 - SpellArt.easeIn(t / 1.5));
			return new double[] {s, 1, s};
		});
	}

	/** The grave's light: the cold white of a buried blade. */
	static final int TOMB_GLOW = 0xBFD8FF;

	// --- Nine Rebirths of the Sword ---

	/** White flame's heat, coolest to hottest: a cold blue at its edges, white at its heart. */
	static final int[] WHITE_FLAME = {0x4A5A8A, 0x8CA4E8, 0xD4E2FF, 0xFFF6E8, 0xFFFFFF};

	/** The reborn sword's blade (lying flat, its point toward -z): bright steel, its edges white flame, a fuller down its middle written in pale gold. */
	static SpellArt.Painter rebornBlade(double tipZ, int seed) {
		return t -> {
			double u = t.across() - 0.5, z = t.qz(), toTip = SpellArt.clamp01((z - tipZ) / 18), half = 0.5 * Math.min(1, toTip * 1.2 + 0.02);
			// Its edges and ends white, but none where its point narrows inside its box (else the box's outline shows round the point).
			if (!t.flat()) return (t.side() ? half < 0.5 : Math.abs(u) > half) ? SpellArt.CLEAR : SpellArt.glow(0xFFFFFF, 0.97);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double edge = half - Math.abs(u);
			if (edge < 0.12) return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(1 - edge / 0.12), WHITE_FLAME), 0.97);
			if (Math.abs(u) < 0.09) {
				double cell = -z / 9;
				int ci = (int) Math.floor(cell);
				if (z < -10 && z > tipZ + 20 && Fire.glyph(ci, seed, (u + 0.09) / 0.18, cell - ci)) return SpellArt.glow(0xFFF0C0, 0.97);
				return SpellArt.col(SpellArt.mix(Metal.SILVER, Metal.GLEAM, 0.4));
			}
			return SpellArt.col(SpellArt.mix(Metal.SILVER, Metal.GLEAM, 0.35 + 0.4 * SpellArt.noise(z * 0.3 + seed, u * 4, seed)));
		};
	}

	static final double REBORN_LENGTH = 92;

	/**
	 * The sword of the Nine Rebirths, lying level, its hilt at the origin and its point {@link #REBORN_LENGTH} pixels out along -z: its blade of
	 * bright steel ("blade"), white flame streaming back off both its edges ("flames"), its guard of gold swept back like a phoenix's wings, its
	 * grip and pommel.
	 */
	static void rebirthSword(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone blade = m.bone("blade", "root", 0, 0, 0);
		double tip = -REBORN_LENGTH;
		for (int i = 0; i < 6; i++) {
			double z0 = -2 - i * 15, z1 = Math.max(tip, z0 - 15);
			blade.box(0, 0, (z0 + z1) / 2, 9.5, 1.4, z0 - z1, rebornBlade(tip, 2301)).density(1.5);
		}
		SpellArt.Bone flames = m.bone("flames", "blade", 0, 0, 0);
		for (int i = 0; i < 9; i++) {
			double z = -8 - i * 9.5, w = 4.8 * Math.min(1, (z - tip) / 18);
			for (int s = -1; s <= 1; s += 2) {
				SpellArt.shared(m, flames, "white_flame", false, SpellArt.v3(s * w, 0.4, z), 13 - i * 0.5, 6, -55, s * 15, s * 35, Fire.flame(3, 2303, WHITE_FLAME), 2, s > 0);
			}
		}
		SpellArt.Painter gold = Metal.gold(2305);
		blade.box(0, 0, 1, 18, 2.2, 3, gold);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(blade, SpellArt.v3(s * 8, 0, 1), SpellArt.v3(s * 12, 0.5, 6), 2, 1.6, gold);
			SpellArt.shared(m, blade, "guard_feather", false, SpellArt.v3(s * 10, 0.6, 3), 10, 4, -80, s * 30, s * 20, Fire.flameFeather(3, 2307), 2, s > 0);
		}
		blade.box(0, 0, 8.5, 2.6, 2.6, 12, Metal.grip());
		blade.box(0, 0, 15.5, 3.6, 3.6, 3.6, gold).rot(45, 0, 45);
		// It blazes (looped): its white flame streaming and leaping.
		double T = 0.8;
		SpellArt.Anim flare = m.anim("flare", T, "true");
		flare.sample("flames", "scale", 0, T, 8, t -> new double[] {1 + 0.08 * Math.sin(2 * Math.PI * t / T * 2), 1 + 0.2 * Math.sin(2 * Math.PI * t / T * 2 + 1), 1});
		// It is reborn (played once): bursting into white flame and forming again from it, greater.
		SpellArt.Anim reborn = m.anim("reborn", 0.5, null);
		reborn.scale("root", 0, 0.3);
		reborn.scale("root", 0.25, 1.15);
		reborn.scale("root", 0.5, 1);
		reborn.scale("flames", 0, 2.2);
		reborn.scale("flames", 0.5, 1);
	}

	// --- Black and White Impermanence ---

	static final int DARK_ROBE = 0x14181E, DARK_ROBE_LIGHT = 0x323C48, TONGUE = 0xC0303C;

	/** The Black Impermanence's robe: the dark of the underworld made cloth, folds falling down it, ghost fire seeping at its seams. */
	static SpellArt.Painter darkRobe(int seed) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.3, t.py * 0.12, t.pz * 0.3, seed, 3);
			double fold = 0.5 + 0.5 * Math.sin(t.px * 1.2 + t.pz * 0.9 + flow * 5 + t.py * 0.15);
			double seam = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.08, t.py * 0.05, t.pz * 0.08, seed + 7, 2) - 1), 24);
			if (seam > 0.7) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_TEAL, Ghost.FIRE_CYAN, (seam - 0.7) * 3), 0.85);
			double light = SpellArt.clamp01(0.3 + 0.35 * fold + 0.2 * Math.max(0, t.ny) + (flow - 0.5) * 0.4);
			return SpellArt.col(SpellArt.ramp(light, 0x05070A, DARK_ROBE, DARK_ROBE_LIGHT, Ghost.SLATE_LIGHT), 0.82);
		};
	}

	/**
	 * An Impermanence's face (on the front of its head's cube; plain flesh elsewhere): the White's deathly pale, smiling, eyes of ghost fire
	 * slanting up, its long red tongue below (a limb of its own); the Black's dark as lacquer, scowling, its eyes fierce slits of ghost fire.
	 */
	static SpellArt.Painter impermanenceFace(boolean white) {
		int skin = white ? 0xF2F0EA : 0x1E2228, shade = white ? 0xC8C4BA : 0x0A0C10;
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(SpellArt.mix(shade, skin, 0.6 + 0.3 * Math.max(0, t.ny)));
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			for (int s = -1; s <= 1; s += 2) {
				double ex = x - s * 0.42, ey = y - 0.12 - (white ? -s * ex * 0.5 : s * ex * 0.6), e = ex * ex / 0.05 + ey * ey / (white ? 0.016 : 0.008);
				if (e < 1) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_WHITE, Ghost.FIRE_CYAN, e), 0.98);
				if (e < 1.6) return SpellArt.col(white ? 0x101418 : 0x000000);
				// The Black's brows, knotted.
				if (!white && Math.abs(y - 0.42 - s * (x - s * 0.4) * 0.5) < 0.07 && Math.abs(x - s * 0.42) < 0.32) return SpellArt.col(0x000000);
			}
			double mouth = white ? Math.abs(y + 0.42 + 0.3 * x * x) : Math.abs(y + 0.45 - 0.25 * x * x);
			if (mouth < 0.06 && Math.abs(x) < 0.5) return SpellArt.col(white ? 0x501014 : 0x5A0E14);
			return SpellArt.col(SpellArt.mix(shade, skin, 0.75 + 0.2 * SpellArt.noise(x * 3, y * 3, white ? 2401 : 2403)));
		};
	}

	/** A tall hat (round a body standing up its y): the White's white, the Black's black, a column of four glyphs down its front in the other. */
	static SpellArt.Painter impermanenceHat(boolean white, double y0, double y1) {
		int cloth = white ? 0xF4F2EC : 0x101216, ink = white ? 0x141418 : 0xF0EEE6;
		return t -> {
			double front = -t.nz, up = (t.py - y0) / (y1 - y0);
			if (front > 0.6 && Math.abs(t.px) < 1.6 && up > 0.12 && up < 0.92) {
				double cell = (up - 0.12) / 0.8 * 4;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci + (white ? 0 : 7), 2405, (t.px + 1.6) / 3.2, 1 - (cell - ci))) return SpellArt.col(ink);
			}
			return SpellArt.col(SpellArt.mix(cloth, white ? 0xC8C4BC : 0x2A2E36, 0.2 + 0.3 * Math.max(0, -t.ny) + 0.15 * SpellArt.noise(t.px, t.py, 2407)));
		};
	}

	/**
	 * One of the two Impermanences, the soul-catchers of the underworld ({@code white}: the White, who smiles and holds up the soul-summoning
	 * banner; else the Black, who scowls and swings the chain that binds the dead), its foot at the origin, some forty-six pixels tall, facing
	 * -z: no feet, its robe of mist (or of the dark) trailing off into wisps ("body", "hem"), its sleeves ("arm_l", "arm_r", each with its hand),
	 * its head ("head") and tall hat ("hat"), and what it carries ("banner" in the White's right hand, "chain" in the Black's left).
	 */
	static void impermanence(SpellArt.Model m, boolean white) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 4, 0);
		SpellArt.Painter robe = white ? Ghost.robe(2411) : darkRobe(2413);
		// Its robe: two rings of long panels of cloth leaning in to its shoulders and flaring out to its hem (its folds), its shoulders over them.
		for (int ring = 0; ring < 2; ring++) {
			int n = 12;
			for (int k = 0; k < n; k++) {
				double th = 360.0 * (k + ring * 0.5) / n, rad = Math.toRadians(th), mid = 6.2 - ring * 0.6;
				body.box(Math.sin(rad) * mid, 15, Math.cos(rad) * mid, 2 * Math.PI * mid / n * 1.3, 20, 0.9, robe).rot(13 + ring * 2, th, 0);
			}
		}
		Vessel.lathe(body, 22, 28, 1.5, y -> 4.7 - (y - 22) * 0.27, robe);
		SpellArt.Bone hem = m.bone("hem", "body", 0, 4, 0);
		for (int k = 0; k < 8; k++) {
			double th = 45.0 * k + 10;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 7.5, 5, Math.cos(Math.toRadians(th)) * 7.5);
			SpellArt.shared(m, hem, "hem_wisp", false, base, 9, 6, 150 + (k % 2) * 15, th, 0, Ghost.wisp(2415), 2, k % 2 == 0);
		}
		// The sash: a cord round its waist.
		Vessel.ring(body, 0, 16, 0, 6.2, 0.9, 16, white ? t -> SpellArt.col(0xD8CCAA) : Vessel.cord());
		for (int s = -1; s <= 1; s += 2) {
			String name = s < 0 ? "arm_r" : "arm_l";
			SpellArt.Bone arm = m.bone(name, "body", s * 4.6, 25, 0);
			SpellArt.limb(arm, SpellArt.v3(s * 4.6, 25, 0), SpellArt.v3(s * 7.6, 16, -2.5), 3.4, 3.4, robe);
			arm.box(s * 8.2, 13.5, -3, 5.4, 6, 5.4, robe).rot(-15, 0, s * 10);
			arm.box(s * 8.4, 10.4, -3.6, 2.2, 2.6, 2.2, t -> SpellArt.col(white ? 0xEEECE4 : 0x2A2E34));
		}
		SpellArt.Bone head = m.bone("head", "body", 0, 28, 0);
		head.box(0, 31.5, -0.3, 6, 7, 6, impermanenceFace(white));
		if (white) head.box(0, 26.5, -3.6, 1.6, 6, 0.5, t -> SpellArt.col(SpellArt.mix(TONGUE, 0xF07080, 0.3 * t.up())));
		SpellArt.Bone hat = m.bone("hat", "head", 0, 35, 0);
		Vessel.lathe(hat, 35, 49, 1.4, y -> 3.6 - (y - 35) * 0.06, impermanenceHat(white, 35, 49));
		Vessel.ring(hat, 0, 35.2, 0, 4.2, 0.9, 16, impermanenceHat(white, 35, 49));
		// Ghost fire round its shoulders.
		SpellArt.Bone aura = m.bone("aura", "body", 0, 24, 0);
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 5.5, 23, Math.cos(Math.toRadians(th)) * 5.5);
			SpellArt.shared(m, aura, "shoulder_fire", false, base, 8, 4, 20, th + 180, 0, Ghost.ghostFire(3, 2417), 3, k % 2 == 0);
		}
		if (white) {
			// The soul-summoning banner, in its right hand: a pole of old bone-white wood, the banner of the dead hung from its crossbar.
			SpellArt.Bone banner = m.bone("banner", "arm_r", -8.4, 10.4, -3.6);
			SpellArt.limb(banner, SpellArt.v3(-8.4, 4, -3.6), SpellArt.v3(-8.8, 34, -3.6), 1.2, 1.2, Ghost.pole());
			SpellArt.limb(banner, SpellArt.v3(-12.8, 32, -3.6), SpellArt.v3(-4.6, 32, -3.6), 0.9, 0.9, Ghost.pole());
			SpellArt.blade(banner, SpellArt.v3(-8.7, 32, -3.6), 16, 7, 180, 90, 0, Ghost.banner(2419)).density(3);
		} else {
			// The chain that binds the dead, swung from its left hand: links of iron, a shackle at its end.
			SpellArt.Bone chain = m.bone("chain", "arm_l", 8.4, 10.4, -3.6);
			for (int k = 0; k < 9; k++) {
				double y = 9 - k * 1.9, x = 8.4 + Math.sin(k * 0.5) * 1.2;
				chain.box(x, y, -3.6, 1.6, 2.4, 0.6, t -> SpellArt.col(SpellArt.mix(0x2A2E34, 0x8A929E, Math.min(t.fx, 1 - t.fx) < 0.25 ? 0.7 : 0.25))).rot(0, k % 2 == 0 ? 0 : 90, 0);
			}
			Vessel.ring(chain, 8.4 + Math.sin(4.5) * 1.2, -8.5, -3.6, 2, 0.8, 10, t -> SpellArt.col(0x6A727E));
		}
		// It comes (played once): rising out of the mist below, unfolding to its height.
		SpellArt.Anim come = m.anim("come", 1.0, null);
		come.sample("root", "scale", 0, 1.0, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.0));
			return new double[] {s, s, s};
		});
		come.sample("root", "position", 0, 1.0, 6, t -> new double[] {0, -12 * (1 - SpellArt.easeOut(t / 1.0)), 0});
		// It waits (looped): floating, its robe and hem stirring, its sleeves swaying.
		double T = 2;
		SpellArt.Anim idle = m.anim("idle", T, "true");
		idle.sample("body", "position", 0, T, 8, t -> new double[] {0, 1.2 * Math.sin(2 * Math.PI * t / T), 0});
		idle.sample("hem", "scale", 0, T, 8, t -> new double[] {1 + 0.08 * Math.sin(2 * Math.PI * t / T * 2), 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 2 + 1), 1 + 0.08 * Math.sin(2 * Math.PI * t / T * 2)});
		idle.sample("arm_l", "rotation", 0, T, 8, t -> new double[] {5 * Math.sin(2 * Math.PI * t / T), 0, -3 * Math.cos(2 * Math.PI * t / T)});
		idle.sample("arm_r", "rotation", 0, T, 8, t -> new double[] {5 * Math.sin(2 * Math.PI * t / T + 1), 0, 3 * Math.cos(2 * Math.PI * t / T + 1)});
		idle.sample("aura", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.15 * Math.sin(2 * Math.PI * t / T * 3), 1});
		// It strikes (played once): the White sweeps its banner down over its quarry; the Black swings its chain out at it.
		SpellArt.Anim strike = m.anim("strike", 0.8, null);
		String arm = white ? "arm_r" : "arm_l";
		strike.rot(arm, 0, 0, 0, 0);
		strike.rot(arm, 0.25, 50, 0, white ? -20 : 20);
		strike.rot(arm, 0.45, -75, 0, white ? 10 : -10);
		strike.rot(arm, 0.8, 0, 0, 0);
		strike.pos("body", 0, 0, 0, 0);
		strike.pos("body", 0.45, 0, 0, -3);
		strike.pos("body", 0.8, 0, 0, 0);
	}

	static void whiteImpermanence(SpellArt.Model m) { impermanence(m, true); }

	static void blackImpermanence(SpellArt.Model m) { impermanence(m, false); }

	// --- Book of Life and Death ---

	static final int PAPER = 0xEEE2C4, PAPER_SHADE = 0xC8B890, INK = 0x16120E, CINNABAR_INK = 0xC21E1E;

	/** A page of the book (on its top face): columns of the names of the living and the dead in black, read down and right to left, some struck through in red. */
	static SpellArt.Painter pageText(int seed) {
		return t -> {
			if (!t.flat() || t.ny < 0) return SpellArt.col(SpellArt.mix(PAPER_SHADE, PAPER, 0.5));
			double u = t.across(), v = t.back();
			if (u < 0.08 || u > 0.92 || v < 0.06 || v > 0.94) return SpellArt.col(SpellArt.mix(PAPER_SHADE, PAPER, 0.6));
			double col = (u - 0.08) / 0.84 * 6;
			int ci = (int) Math.floor(col);
			double cu = col - ci;
			double cell = (v - 0.06) / 0.88 * 8;
			int ri = (int) Math.floor(cell);
			if (SpellArt.hash(ci, 99, seed) > 0.55 && Math.abs(cu - 0.5) < 0.07 && cell > 1 && cell < 6) return SpellArt.col(CINNABAR_INK);
			if (cu > 0.15 && cu < 0.85 && Fire.glyph(ci * 8 + ri, seed, (cu - 0.15) / 0.7, cell - ri)) return SpellArt.col(INK);
			return SpellArt.col(SpellArt.mix(PAPER, PAPER_SHADE, 0.15 * SpellArt.noise(u * 9 + seed, v * 9, seed)));
		};
	}

	/** The book's cover: black lacquer, its border and corners gold; on the outside of its left board the title in cinnabar on a plaque of gold. */
	static SpellArt.Painter bookCover(boolean titled) {
		return t -> {
			double u = t.across(), v = t.back();
			boolean outside = t.flat() && t.ny < 0;
			if (t.flat() && (u < 0.06 || u > 0.94 || v < 0.05 || v > 0.95)) return SpellArt.col(Metal.GOLD);
			if (titled && outside && Math.abs(u - 0.5) < 0.16 && v > 0.18 && v < 0.72) {
				double cell = (v - 0.18) / 0.54 * 3;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci + 20, 2501, (u - 0.34) / 0.32, cell - ci)) return SpellArt.col(CINNABAR_INK);
				return SpellArt.col(Metal.GOLD);
			}
			return SpellArt.col(SpellArt.mix(Ghost.LACQUER, Ghost.LACQUER_LIGHT, 0.3 + 0.4 * SpellArt.noise(u * 6, v * 6, 2503)));
		};
	}

	/** The judge's brush: its handle black lacquer banded gold. */
	static SpellArt.Painter brushHandle() {
		return t -> {
			double band = t.ly % 5;
			return SpellArt.col(band < 0.6 ? Metal.GOLD : SpellArt.mix(Ghost.LACQUER, Ghost.LACQUER_LIGHT, 0.4));
		};
	}

	/** Its bristles (on a solid, their point at its foot): soaked black with ink, their point red with cinnabar, glowing. */
	static SpellArt.Painter bristles() {
		return t -> t.up() < 0.35 ? SpellArt.glow(SpellArt.mix(0xFF4A3A, CINNABAR_INK, t.up() / 0.35), 0.96) : SpellArt.col(INK);
	}

	/**
	 * The Book of Life and Death, lying open, its spine along z at the origin: its two boards of black lacquer and gold ("cover_l", "cover_r":
	 * the right one folds over onto the left to close it), its pages, three loose leaves turning in the wind of the underworld ("page_0".."page_2"),
	 * a ring of the dead's glyphs in ghost fire under it ("ring"), and over it the judge's brush ("brush"), red with cinnabar.
	 */
	static void lifeDeathBook(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone left = m.bone("cover_l", "root", 0, 0, 0);
		left.box(-8.5, 0, 0, 17, 0.8, 24, bookCover(true));
		left.box(-7.8, 1.3, 0, 15, 1.8, 22, pageText(2511));
		SpellArt.Bone right = m.bone("cover_r", "root", 0, 0, 0);
		right.box(8.5, 0, 0, 17, 0.8, 24, bookCover(false));
		right.box(7.8, 1.3, 0, 15, 1.8, 22, pageText(2513));
		for (int k = 0; k < 3; k++) {
			SpellArt.Bone page = m.bone("page_" + k, "root", 0, 2.3 + k * 0.05, 0);
			SpellArt.sheet(page, 7.6, 2.3 + k * 0.05, 0, 15, 22, 0, 0, 0, pageText(2521 + k)).density(2);
		}
		SpellArt.Bone ring = m.bone("ring", "root", 0, -1, 0);
		SpellArt.sheet(ring, 0, -1, 0, 44, 44, 0, 0, 0, Void.script(0.62, 0.9, 18, 2531, Ghost.FIRE_PALE, Ghost.FIRE_CYAN)).density(1.5);
		SpellArt.Bone brush = m.bone("brush", "root", 5, 9, -4);
		SpellArt.limb(brush, SpellArt.v3(5, 9, -4), SpellArt.v3(9, 26, 2), 1.6, 1.6, brushHandle());
		SpellArt.limb(brush, SpellArt.v3(4.6, 6.2, -4.4), SpellArt.v3(5.1, 9.6, -3.9), 2.4, 2.4, bristles());
		// It opens (played once): rising out of nothing, its right board swinging over from the left to lie open.
		SpellArt.Anim open = m.anim("open", 1.0, null);
		open.sample("root", "scale", 0, 1.0, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01(t / 0.5)));
			return new double[] {s, s, s};
		});
		open.sample("cover_r", "rotation", 0, 1.0, 6, t -> new double[] {0, 0, -180 * (1 - SpellArt.ease(SpellArt.clamp01((t - 0.3) / 0.7)))});
		for (int k = 0; k < 3; k++) open.sample("page_" + k, "rotation", 0, 1.0, 6, t -> new double[] {0, 0, -180 * (1 - SpellArt.ease(SpellArt.clamp01((t - 0.3) / 0.7)))});
		// It is written in (looped): its loose leaves riffling over and back, the brush moving over it.
		double T = 2;
		SpellArt.Anim write = m.anim("write", T, "true");
		for (int k = 0; k < 3; k++) {
			int kk = k;
			write.sample("page_" + k, "rotation", 0, T, 10, t -> new double[] {0, 0, -175 * Math.pow(Math.sin(Math.PI * t / T + kk * 0.35), 2)});
		}
		write.sample("brush", "position", 0, T, 10, t -> new double[] {-6 * Math.sin(2 * Math.PI * t / T), -1.5 * Math.abs(Math.sin(2 * Math.PI * t / T * 3)), 4 * Math.cos(2 * Math.PI * t / T)});
		write.sample("ring", "rotation", 0, T, 8, t -> new double[] {0, 90 * t / T, 0});
		// It judges (played once): the brush sweeping one great stroke across the book, the leaves flung up.
		SpellArt.Anim strike = m.anim("strike", 0.7, null);
		strike.pos("brush", 0, 0, 0, 0);
		strike.pos("brush", 0.15, -10, 4, -6);
		strike.pos("brush", 0.4, 8, -2, 8);
		strike.pos("brush", 0.7, 0, 0, 0);
		strike.rot("brush", 0, 0, 0, 0);
		strike.rot("brush", 0.4, 0, 0, -35);
		strike.rot("brush", 0.7, 0, 0, 0);
		for (int k = 0; k < 3; k++) {
			strike.rot("page_" + k, 0, 0, 0, 0);
			strike.rot("page_" + k, 0.3, 0, 0, -60 - k * 30);
			strike.rot("page_" + k, 0.7, 0, 0, 0);
		}
	}

	// --- Heaven-Demon Eclipse ---

	/** The demon's moon (painted where it lies, its heart at x = {@code cx}): black, veined with blood that glows, its rim crimson. */
	static SpellArt.Painter demonMoon(double cx, double radius, int seed) {
		return t -> {
			double r = Math.hypot(t.px - cx, t.py) / radius;
			if (r > 0.9 && (t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK)) return SpellArt.glow(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.EMBER, (r - 0.9) * 10), 0.97);
			if (t.f != SpellArt.Face.FRONT && t.f != SpellArt.Face.BACK) return SpellArt.glow(HeavenDemon.CRIMSON, 0.95);
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm(t.px * 0.2 + seed, t.py * 0.2, seed, 3) - 1), 10);
			if (vein > 0.7) return SpellArt.glow(SpellArt.mix(HeavenDemon.BLOOD, HeavenDemon.SCARLET, (vein - 0.7) * 3), 0.95);
			return SpellArt.col(SpellArt.mix(0x050103, HeavenDemon.BLOOD_DARK, 0.3 + 0.5 * SpellArt.noise(t.px * 0.4, t.py * 0.4, seed + 1)));
		};
	}

	/** The eclipse's ring (on a sheet facing the caster): the corona round the darkened sun, white-gold, a diamond of light at one point of it. */
	static SpellArt.Painter corona() {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y), a = Math.atan2(y, x);
			if (r < 0.5 || r > 0.99) return SpellArt.CLEAR;
			double streamer = Math.pow(Math.abs(Math.cos(a * 7)), 6) * 0.35;
			double fall = (r - 0.5) / (0.45 + streamer);
			if (fall > 1) return SpellArt.CLEAR;
			double diamond = Math.hypot(x - 0.38, y - 0.36);
			if (diamond < 0.08) return SpellArt.glow(0xFFFFFF, 0.98);
			return SpellArt.glow(SpellArt.mix(HeavenDemon.WHITE_LIGHT, HeavenDemon.GOLD, fall), 0.9 * (1 - fall) * (1 - fall));
		};
	}

	/** The eclipse's beam (on a limb hanging down from it): darkness edged with crimson and gold, thinning and fading as it falls. */
	static SpellArt.Painter eclipseBeam() {
		return t -> {
			if (t.flat()) return SpellArt.CLEAR;
			double down = 1 - t.up(), u = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK ? t.across() : t.back(), edge = Math.abs(u - 0.5) * 2;
			double a = (1 - down * 0.6) * 0.92;
			if (edge > 0.8) return SpellArt.glow(SpellArt.mix(HeavenDemon.GOLD_BRIGHT, HeavenDemon.SCARLET, down), a);
			if (edge > 0.6) return SpellArt.glow(HeavenDemon.CRIMSON, a);
			return SpellArt.col(SpellArt.mix(0x020001, HeavenDemon.BLOOD_DARK, edge), a);
		};
	}

	static final double SUN_R = 14, MOON_DISC_R = 13.5, APART = 24;

	/**
	 * The Heaven-Demon Eclipse, its heart at the origin, facing -z (its caster looks at its +z): heaven's sun ("sun", a disc of the sun's fire
	 * crowned with heaven's flames, "sun_fire") and the demon's moon ("moon", black veined with blood, crimson fire round its rim) {@link #APART}
	 * pixels to either side, that come together, the moon before the sun; the corona round them once they meet ("corona"), and the beam of
	 * darkness it pours down ("beam").
	 */
	static void heavenDemonEclipse(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone sun = m.bone("sun", "root", -APART, 0, 0);
		SpellArt.Bone sunDisc = m.bone("sun_disc", "sun", -APART, 0, 0);
		Vessel.disc(sunDisc, -0.5, SUN_R, 3, Void.sunSurface(2601), 2);
		for (SpellArt.Cube c : sunDisc.cubes) {
			c.origin[0] -= APART;
			c.pivot[0] -= APART;
		}
		SpellArt.Bone sunFire = m.bone("sun_fire", "sun", -APART, 0, 0);
		for (int k = 0; k < 16; k++) {
			double phi = 22.5 * k, pr = Math.toRadians(phi);
			double[] base = SpellArt.v3(-APART + Math.sin(pr) * SUN_R, Math.cos(pr) * SUN_R, -0.5);
			SpellArt.shared(m, sunFire, "sun_flame", false, base, 9 + (k % 2) * 3, 6, 0, 90, phi, HeavenDemon.heavenFlame(3, 2603), 2, k % 2 == 0);
		}
		SpellArt.Bone moon = m.bone("moon", "root", APART, 0, 0);
		SpellArt.Bone moonDisc = m.bone("moon_disc", "moon", APART, 0, 3);
		Vessel.disc(moonDisc, 3, MOON_DISC_R, 3, demonMoon(APART, MOON_DISC_R, 2605), 2);
		for (SpellArt.Cube c : moonDisc.cubes) {
			c.origin[0] += APART;
			c.pivot[0] += APART;
		}
		SpellArt.Bone moonFire = m.bone("moon_fire", "moon", APART, 0, 3);
		for (int k = 0; k < 12; k++) {
			double phi = 30.0 * k + 15, pr = Math.toRadians(phi);
			double[] base = SpellArt.v3(APART + Math.sin(pr) * MOON_DISC_R, Math.cos(pr) * MOON_DISC_R, 3);
			SpellArt.shared(m, moonFire, "moon_flame", false, base, 6, 4.5, 0, 90, phi, HeavenDemon.demonFlame(2, 2607), 2, k % 2 == 1);
		}
		SpellArt.Bone ring = m.bone("corona", "root", 0, 0, 0);
		SpellArt.sheet(ring, 0, 0, -1.5, 62, 62, 90, 0, 0, corona()).density(1.2);
		SpellArt.Bone beam = m.bone("beam", "root", 0, 0, 0);
		SpellArt.limb(beam, SpellArt.v3(0, -2, 1), SpellArt.v3(0, -90, 1), 14, 14, eclipseBeam()).density(0.6);
		// They come together (played once, held at its end by the loop): the sun and the moon closing on the middle, the moon passing before it;
		// the corona flaring as they meet, the beam pouring down.
		double L = 1.6;
		SpellArt.Anim converge = m.anim("converge", L, null);
		converge.sample("sun", "position", 0, L, 8, t -> new double[] {APART * SpellArt.ease(SpellArt.clamp01(t / 1.3)), 0, 0});
		converge.sample("moon", "position", 0, L, 8, t -> new double[] {-APART * SpellArt.ease(SpellArt.clamp01(t / 1.3)), 0, 0});
		converge.sample("corona", "scale", 0, L, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - 1.15) / 0.45)));
			return new double[] {s, s, s};
		});
		converge.sample("beam", "scale", 0, L, 6, t -> new double[] {Math.max(0.02, SpellArt.clamp01((t - 1.3) / 0.3)), Math.max(0.02, SpellArt.clamp01((t - 1.3) / 0.3)), Math.max(0.02, SpellArt.clamp01((t - 1.3) / 0.3))});
		// The eclipse (looped): sun and moon as one, the corona wheeling, the beam throbbing, heaven's flames and the demon's leaping.
		double T = 1.2;
		SpellArt.Anim blaze = m.anim("blaze", T, "true");
		blaze.pos("sun", 0, APART, 0, 0);
		blaze.pos("sun", T, APART, 0, 0);
		blaze.pos("moon", 0, -APART, 0, 0);
		blaze.pos("moon", T, -APART, 0, 0);
		blaze.sample("corona", "rotation", 0, T, 8, t -> new double[] {0, 0, 360 * t / T});
		blaze.sample("beam", "scale", 0, T, 8, t -> new double[] {1 + 0.15 * Math.sin(2 * Math.PI * t / T * 2), 1, 1 + 0.15 * Math.sin(2 * Math.PI * t / T * 2)});
		blaze.sample("sun_fire", "scale", 0, T, 8, t -> {
			double s = 1 + 0.1 * Math.sin(2 * Math.PI * t / T * 3);
			return new double[] {s, s, 1};
		});
		blaze.sample("moon_fire", "scale", 0, T, 8, t -> {
			double s = 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 3 + 1);
			return new double[] {s, s, 1};
		});
		// It pulses (played once): all of it swelling, the beam thrown wide.
		SpellArt.Anim pulse = m.anim("pulse", 0.5, null);
		pulse.pos("sun", 0, APART, 0, 0);
		pulse.pos("sun", 0.5, APART, 0, 0);
		pulse.pos("moon", 0, -APART, 0, 0);
		pulse.pos("moon", 0.5, -APART, 0, 0);
		pulse.scale("root", 0, 1);
		pulse.scale("root", 0.15, 1.15);
		pulse.scale("root", 0.5, 1);
		pulse.scale("beam", 0, 1);
		pulse.scale("beam", 0.15, 2.2, 1, 2.2);
		pulse.scale("beam", 0.5, 1);
	}
}
