/**
 * The looks of True Immortal's great spells for SpellArt (2026-10-24): the elements as they were at the beginning, before heaven and earth were
 * parted, each wrought of what it is cast from. Wood's cyclone of the primal wind and the first seed of creation under its dome of life; fire's
 * three braided pillars of Samadhi's true fire and the flower of chaos-fire that burned before heaven; earth's hand of Houtu and the continent
 * that falls; water's glacier of the first age and the dome of the primordial sea; metal's heaven-slaying sword rising out of the land and
 * Pangu's axe; the ghost foundation's walls of Fengdu and the helix of ten thousand souls; both paths' sphere of primordial chaos. At the render
 * scale (4) and its realm's greatness (3.41) a model pixel is some eighty-five hundredths of a block.
 */
public class Immortal {
	// --- Shared shapes ---

	/** A hemisphere (its rim on y = 0, its crown at y = r) of rounds {@code step} tall, their sides only (a dome to stand within). */
	static void dome(SpellArt.Bone bone, double r, double step, SpellArt.Painter p, double tpp) {
		for (double y = 0; y < r - 1e-6; y += step) {
			double h = Math.min(step, r - y), mid = y + h / 2, rr = Math.sqrt(Math.max(0, r * r - mid * mid));
			if (rr < 0.5) continue;
			for (int k = 0; k < 4; k++) {
				SpellArt.Cube q = bone.box(0, mid, 0, 2 * rr, h + 0.02, rr * Vessel.OCTAGON_SIDE, p).skip(SpellArt.Face.UP, SpellArt.Face.DOWN).density(tpp);
				if (k > 0) q.rot(0, 45 * k, 0);
			}
		}
	}

	/** A ball of {@code r} about (cx, cy, cz): rounds {@code step} tall, capped where they step in. */
	static void ball(SpellArt.Bone bone, double cx, double cy, double cz, double r, double step, SpellArt.Painter p, double tpp) {
		for (double y = -r; y < r - 1e-6; y += step) {
			double h = Math.min(step, r - y), mid = y + h / 2, rr = Math.sqrt(Math.max(0, r * r - mid * mid));
			if (rr < 0.4) continue;
			for (int k = 0; k < 4; k++) {
				SpellArt.Cube q = bone.box(cx, cy + mid, cz, 2 * rr, h + 0.02, rr * Vessel.OCTAGON_SIDE, p).density(tpp);
				if (k > 0) q.rot(0, 45 * k, 0);
			}
		}
	}

	// --- Primal Wind of Creation ---

	static final int PRIMAL_GREEN = 0x7CF0B0, PRIMAL_GOLD = 0xFFE89A, PRIMAL_DEEP = 0x2E8A6A;

	/** A streamer of the primal wind (on a blade): green-gold light, streaked along it, thinning to nothing at its end; seen through. */
	static SpellArt.Painter primalWind(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double half = 0.46 * (1 - v * 0.85) * (0.8 + 0.2 * Math.sin(v * 9 + seed));
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double streak = Math.abs(Math.sin((u * 6 + v * 4) * Math.PI + seed));
			int c = SpellArt.mix(SpellArt.mix(PRIMAL_DEEP, PRIMAL_GREEN, 0.4 + 0.6 * streak), PRIMAL_GOLD, Math.pow(1 - Math.abs(u) / Math.max(1e-3, half), 3) * 0.6);
			return SpellArt.glow(c, (0.22 + 0.4 * (1 - v)) * (1 - Math.abs(u) / Math.max(1e-3, half) * 0.5));
		};
	}

	static final int CYCLONE_RINGS = 12;
	static final double CYCLONE_HIGH = 64;

	/** The cyclone's radius {@code y} pixels up: a funnel, narrow at its foot, opening wide at its top. */
	static double cycloneRadius(double y) {
		return 2.5 + 16 * Math.pow(y / CYCLONE_HIGH, 1.4);
	}

	/**
	 * The Primal Wind of Creation, its foot at the origin, {@link #CYCLONE_HIGH} pixels tall: a cyclone of the primal wind, a funnel of
	 * {@link #CYCLONE_RINGS} rings of green-gold streamers ("ring_0"..), each turning on its own, leaves and seeds of creation whirled up in it
	 * ("seeds"), a haze of green light round its foot ("foot").
	 */
	static void primordialCyclone(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int k = 0; k < CYCLONE_RINGS; k++) {
			double y = CYCLONE_HIGH * k / CYCLONE_RINGS, r = cycloneRadius(y + 2);
			SpellArt.Bone ring = m.bone("ring_" + k, "root", 0, y, 0);
			int n = 8 + k / 2;
			for (int i = 0; i < n; i++) {
				double th = 360.0 * i / n + k * 17, rad = Math.toRadians(th);
				double[] base = SpellArt.v3(Math.sin(rad) * r, y, Math.cos(rad) * r);
				// Each streamer lies round the funnel, swept up its side.
				SpellArt.shared(m, ring, "wind_" + (k % 3), false, base, 10 + r * 0.6, 4 + r * 0.25, 70, th + 90, 0, primalWind(2701 + k % 3), 1.2, i % 2 == 0);
			}
		}
		SpellArt.Bone seeds = m.bone("seeds", "root", 0, 0, 0);
		for (int i = 0; i < 18; i++) {
			double y = 6 + i * 3.2, r = cycloneRadius(y) * 0.85, th = i * 67;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * r, y, Math.cos(Math.toRadians(th)) * r);
			if (i % 3 == 0) seeds.box(base[0], base[1], base[2], 1.4, 1.4, 1.4, t -> SpellArt.glow(PRIMAL_GOLD, 0.96)).rot(45, 45, 0);
			else SpellArt.shared(m, seeds, "cyclone_leaf", false, base, 3.5, 2.6, 40 + i * 13, th, 30, Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.5, 2711), 4, i % 2 == 0);
		}
		SpellArt.Bone foot = m.bone("foot", "root", 0, 0, 0);
		SpellArt.sheet(foot, 0, 0.3, 0, 30, 30, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 0.98) return SpellArt.CLEAR;
			double arm = Math.sin(Math.atan2(t.back() - 0.5, t.across() - 0.5) * 3 + r * 9);
			return SpellArt.glow(SpellArt.mix(PRIMAL_GREEN, PRIMAL_GOLD, 0.5 + 0.5 * arm), 0.35 * (1 - r));
		}).density(2);
		// It forms (played once): rising ring by ring out of the land.
		SpellArt.Anim form = m.anim("form", 1.0, null);
		for (int k = 0; k < CYCLONE_RINGS; k++) {
			int kk = k;
			form.sample("ring_" + k, "scale", 0, 1.0, 5, t -> {
				double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - kk * 0.05) / 0.4)));
				return new double[] {s, s, s};
			});
		}
		form.sample("seeds", "scale", 0, 1.0, 4, t -> {
			double s = Math.max(0.02, SpellArt.clamp01((t - 0.4) / 0.6));
			return new double[] {s, s, s};
		});
		// It whirls (looped): every ring turning, the faster the higher, the seeds wheeling up round it.
		double T = 2;
		SpellArt.Anim spin = m.anim("spin", T, "true");
		for (int k = 0; k < CYCLONE_RINGS; k++) {
			int turns = 1 + k / 4;
			spin.sample("ring_" + k, "rotation", 0, T, 8, t -> new double[] {0, 360.0 * turns * t / T, 0});
		}
		spin.sample("seeds", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		spin.sample("foot", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		// It dies away (played once, held).
		SpellArt.Anim fade = m.anim("fade", 0.8, "hold_on_last_frame");
		fade.sample("root", "scale", 0, 0.8, 5, t -> {
			double s = Math.max(0.02, 1 - SpellArt.easeIn(t / 0.8));
			return new double[] {s, 1 + 0.2 * t, s};
		});
	}

	// --- First Seed of Creation ---

	static final int LIFE_DOME = 0x8CFFB8, LIFE_DOME_DEEP = 0x1E8A5A;

	/** The seed of creation (on a solid): gold and jade, veins of white light through it; glowing. */
	static SpellArt.Painter creationSeed(int seed) {
		return t -> {
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.4, t.py * 0.3, t.pz * 0.4, seed, 3) - 1), 9);
			if (vein > 0.6) return SpellArt.glow(0xFFFFFF, 0.98);
			double n = SpellArt.fbm3(t.px * 0.2, t.py * 0.2, t.pz * 0.2, seed + 2, 2);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.3 + 0.5 * n + 0.2 * Math.max(0, t.ny)), Wood.JADE_WOOD, Wood.JADE, PRIMAL_GOLD, 0xFFF8E0), 0.97);
		};
	}

	/** A vine of the dome of life (on a limb): living green twisting along it, lighter on top, veins of creation's gold light through it. */
	static SpellArt.Painter lifeVine(int seed) {
		return t -> {
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.12, t.py * 0.12, t.pz * 0.12, seed, 2) - 1), 12);
			if (vein > 0.8) return SpellArt.glow(SpellArt.mix(PRIMAL_GOLD, 0xFFFFFF, (vein - 0.8) * 3), 0.96);
			double twist = Math.sin(t.ly * 0.6 + t.fx * 4 + t.f.ordinal() * 1.7);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.42 + 0.22 * twist + 0.22 * Math.max(0, t.ny) - 0.12 * Math.max(0, -t.ny)), 0x0E3020, 0x24683E, 0x4CAE68, 0x9CE6AE));
		};
	}

	/** The dome's footprint (on a sheet): a ring of green-gold light round its foot, faint spokes in to its heart; seen through. */
	static SpellArt.Painter lifeRing(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 1) return SpellArt.CLEAR;
			double ring = Math.exp(-Math.pow((r - 0.93) / 0.035, 2)), inner = Math.exp(-Math.pow((r - 0.55) / 0.02, 2)) * 0.6;
			double spoke = Math.pow(Math.max(0, Math.cos(a * 8)), 40) * (0.2 + 0.6 * r) * 0.7;
			double glow = Math.max(ring, Math.max(inner, spoke)) + 0.08 * (1 - r);
			if (glow < 0.05) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(LIFE_DOME, PRIMAL_GOLD, 0.4 + 0.6 * ring), SpellArt.clamp01(glow * 0.8));
		};
	}

	static final double LIFE_R = 56;
	static final int LIFE_ARCHES = 8, ARCH_STEPS = 9;

	/**
	 * The First Seed of Creation: the seed hanging at its heart, twelve pixels up ("seed"); the dome of life over it all, {@link #LIFE_R} pixels
	 * round, {@link #LIFE_ARCHES} great vines arching up out of the land to meet over it ("arch_0".., each growing from its foot, in "dome"),
	 * leaves along them, a knot of light where they meet ("crown") and a ring of light round their feet ("foot"); and the tree of creation that
	 * will grow out of the seed, unseen till it blooms ("tree": its trunk, its boughs, its crown).
	 */
	static void creationSeed(SpellArt.Model m) {
		m.tpp = 0.8;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone seed = m.bone("seed", "root", 0, 12, 0);
		Vessel.lathe(seed, 6, 18, 1, y -> 4.5 * Math.pow(Math.max(0, 1 - Math.pow((y - 12) / 6.2, 2)), 0.55), creationSeed(2801));
		for (SpellArt.Cube c : seed.cubes) c.density(3);
		m.bone("dome", "root", 0, 0, 0);
		SpellArt.Painter vine = lifeVine(2803), leaf = Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.5, 2804);
		for (int k = 0; k < LIFE_ARCHES; k++) {
			double th = Math.toRadians(360.0 * k / LIFE_ARCHES + 22.5);
			double[] foot = SpellArt.v3(Math.sin(th) * LIFE_R, 0, Math.cos(th) * LIFE_R), last = foot;
			SpellArt.Bone arch = m.bone("arch_" + k, "dome", foot[0], 0, foot[2]);
			for (int i = 1; i <= ARCH_STEPS; i++) {
				double ph = Math.toRadians(90.0 * i / ARCH_STEPS), r = LIFE_R * Math.cos(ph);
				double[] at = SpellArt.v3(Math.sin(th) * r, LIFE_R * Math.sin(ph), Math.cos(th) * r), dir = SpellArt.sub(at, last);
				double[] way = SpellArt.times(dir, 0.5 / SpellArt.len(dir));
				double w = 4.4 - 2.2 * i / ARCH_STEPS;
				// A little past each joint, so the bends close.
				SpellArt.limb(arch, SpellArt.sub(last, way), SpellArt.add(at, way), w, w, vine).density(1);
				if (i < ARCH_STEPS) for (int s = -1; s <= 1; s += 2) {
					SpellArt.shared(m, arch, "dome_leaf", false, at, 9, 5.5, 65, Math.toDegrees(th) - 90 * s + 15 * (i % 3 - 1), 10 * s, leaf, 1.5, s > 0);
				}
				last = at;
			}
		}
		SpellArt.Bone crown = m.bone("crown", "dome", 0, LIFE_R, 0);
		ball(crown, 0, LIFE_R, 0, 4.5, 1.5, creationSeed(2809), 2);
		SpellArt.Bone ring = m.bone("foot", "root", 0, 0, 0);
		SpellArt.sheet(ring, 0, 0.3, 0, 2 * LIFE_R + 8, 2 * LIFE_R + 8, 0, 0, 0, lifeRing(2811)).density(0.6);
		SpellArt.Bone tree = m.bone("tree", "root", 0, 12, 0);
		// Its trunk down through the seed into the land, its roots spread over it; its boughs and a full crown of leaves above.
		SpellArt.limb(tree, SpellArt.v3(0, 0, 0), SpellArt.v3(0.6, 66, 0.4), 7, 7, Wood.jadeBark(0.4, 6, 2805)).density(1.5);
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k + 10);
			SpellArt.limb(tree, SpellArt.v3(0, 3, 0), SpellArt.v3(Math.sin(th) * 15, 0.4, Math.cos(th) * 15), 3.2, 2.4, Wood.jadeBark(0.4, 4, 2821 + k)).density(1.5);
		}
		SpellArt.Painter foliage = Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.6, 2815);
		for (int k = 0; k < 7; k++) {
			double th = Math.toRadians(360.0 / 7 * k + 20);
			double[] a = SpellArt.v3(0.5, 46 + (k % 3) * 5, 0.3), b = SpellArt.v3(Math.sin(th) * 26, 64 + (k % 2) * 6, Math.cos(th) * 26);
			SpellArt.limb(tree, a, b, 2.8, 2.8, Wood.jadeBark(0.4, 4, 2807 + k)).density(1.5);
			for (int j = 0; j < 7; j++) {
				double[] at = SpellArt.add(b, SpellArt.v3(Math.sin(j * 2.1) * 7, 1 + j * 1.4, Math.cos(j * 1.7) * 7));
				SpellArt.shared(m, tree, "creation_foliage", false, at, 15, 12, 30 + j * 17, Math.toDegrees(th) + j * 53, 0, foliage, 1.2, j % 2 == 0);
			}
		}
		for (int j = 0; j < 8; j++) {
			double[] at = SpellArt.v3(Math.sin(j * 0.785) * 8, 70 + (j % 2) * 3, Math.cos(j * 0.785) * 8);
			SpellArt.shared(m, tree, "creation_foliage", false, at, 15, 12, 20 + j * 9, j * 45, 0, foliage, 1.2, j % 2 == 0);
		}
		// It grows (played once): the vines reaching up out of the land one after another to meet over it all, the knot of light where they
		// meet, the seed coming down out of the light.
		SpellArt.Anim grow = m.anim("grow", 1.4, null);
		for (int k = 0; k < LIFE_ARCHES; k++) {
			int kk = k;
			grow.sample("arch_" + k, "scale", 0, 1.4, 7, t -> {
				double s = Math.max(0.02, SpellArt.easeOut(SpellArt.clamp01((t - kk * 0.05) / 0.9)));
				return new double[] {s, s, s};
			});
		}
		grow.sample("crown", "scale", 0, 1.4, 6, t -> {
			double s = Math.max(0.02, SpellArt.clamp01((t - 1.0) / 0.4));
			return new double[] {s, s, s};
		});
		grow.sample("foot", "scale", 0, 1.4, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(Math.min(1, t / 0.6)));
			return new double[] {s, 1, s};
		});
		grow.sample("seed", "position", 0, 1.4, 6, t -> new double[] {0, 40 * (1 - SpellArt.easeOut(t / 1.4)), 0});
		grow.scale("tree", 0, 0.02);
		grow.scale("tree", 1.4, 0.02);
		// It breathes (looped): the seed turning and glowing, the dome breathing, the ring at its foot wheeling.
		double T = 3;
		SpellArt.Anim pulse = m.anim("pulse", T, "true");
		pulse.sample("seed", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		pulse.sample("seed", "scale", 0, T, 8, t -> {
			double s = 1 + 0.08 * Math.sin(2 * Math.PI * t / T * 2);
			return new double[] {s, s, s};
		});
		pulse.sample("dome", "scale", 0, T, 8, t -> {
			double s = 1 + 0.015 * Math.sin(2 * Math.PI * t / T);
			return new double[] {s, s, s};
		});
		pulse.sample("foot", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		pulse.scale("tree", 0, 0.02);
		pulse.scale("tree", T, 0.02);
		// It blooms (played once, held): the tree of creation thrown up out of the seed through the dome, the vines flaring and drawn back down
		// into the land.
		SpellArt.Anim bloom = m.anim("bloom", 1.2, "hold_on_last_frame");
		bloom.sample("tree", "scale", 0, 1.2, 6, t -> {
			double s = Math.max(0.02, Math.min(1.08, SpellArt.easeOut(t / 0.7) * 1.08) - Math.max(0, t - 0.7) * 0.16);
			return new double[] {s, s, s};
		});
		for (int k = 0; k < LIFE_ARCHES; k++) {
			bloom.scale("arch_" + k, 0, 1);
			bloom.scale("arch_" + k, 0.3, 1.06);
			bloom.scale("arch_" + k, 1.2, 0.02);
		}
		bloom.scale("crown", 0, 1);
		bloom.scale("crown", 0.3, 0.02);
		bloom.scale("seed", 0, 1);
		bloom.scale("seed", 1.2, 0.3);
	}

	// --- Samadhi True Fire ---

	/** The three fires of Samadhi: the heart's crimson, the spirit's gold, the essence's white. */
	static final int[][] SAMADHI_FIRES = {{0x3A0604, 0x8A1408, 0xD8361E, 0xFF7A3C, 0xFFD0A0}, {0x7A2A06, 0xE07A1E, 0xFFB838, 0xFFE89A, 0xFFFBEA}, Nirvana.WHITE_FLAME};

	static final double PILLAR_HIGH = 88;

	/**
	 * The three pillars of Samadhi True Fire, braided about one another round the origin, {@link #PILLAR_HIGH} pixels tall: each a helix of
	 * flame in its own fire ("pillar_0".."pillar_2", turning), a ring of molten light at their foot ("foot").
	 */
	static void samadhiPillars(SpellArt.Model m) {
		m.tpp = 1.4;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone braid = m.bone("braid", "root", 0, 0, 0);
		for (int k = 0; k < 3; k++) {
			SpellArt.Bone pillar = m.bone("pillar_" + k, "braid", 0, 0, 0);
			for (int i = 0; i < 22; i++) {
				double y = i * 4, th = 120 * k + i * 24, r = 8.5, rad = Math.toRadians(th);
				double[] base = SpellArt.v3(Math.sin(rad) * r, y, Math.cos(rad) * r);
				SpellArt.shared(m, pillar, "samadhi_" + k, false, base, 15, 11, 0, th, 0, Fire.flame(3, 2901 + k, SAMADHI_FIRES[k]), 1.2, i % 2 == 0);
				if (i % 2 == 0) SpellArt.shared(m, pillar, "samadhi_" + k, false, base, 15, 11, 0, th + 90, 0, null, 1.2, i % 4 == 0);
			}
		}
		SpellArt.Bone foot = m.bone("foot", "root", 0, 0, 0);
		SpellArt.sheet(foot, 0, 0.3, 0, 40, 40, 0, 0, 0, t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 0.98) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(1 - r), Fire.CRIMSON, Fire.FLAME, Fire.GOLD, Fire.WHITE_FIRE), 0.7 * (1 - r * 0.7));
		}).density(2);
		// They erupt (played once): up out of the land, the braid winding up.
		SpellArt.Anim erupt = m.anim("erupt", 0.8, null);
		erupt.sample("braid", "scale", 0, 0.8, 6, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut(t / 0.8)), 1});
		erupt.sample("foot", "scale", 0, 0.8, 4, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 0.5));
			return new double[] {s, 1, s};
		});
		// They burn (looped): the braid turning, each pillar's flames leaping.
		double T = 1.2;
		SpellArt.Anim burn = m.anim("burn", T, "true");
		burn.sample("braid", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		for (int k = 0; k < 3; k++) {
			int kk = k;
			burn.sample("pillar_" + k, "scale", 0, T, 8, t -> new double[] {1 + 0.1 * Math.sin(2 * Math.PI * t / T * 2 + kk * 2), 1, 1 + 0.1 * Math.sin(2 * Math.PI * t / T * 2 + kk * 2)});
		}
		// They meet (played once, held): the three drawn into one, flaring white.
		SpellArt.Anim converge = m.anim("converge", 0.6, "hold_on_last_frame");
		for (int k = 0; k < 3; k++) {
			converge.scale("pillar_" + k, 0, 1);
			converge.scale("pillar_" + k, 0.4, 0.3, 1.1, 0.3);
			converge.scale("pillar_" + k, 0.6, 0.02);
		}
		converge.scale("foot", 0, 1);
		converge.scale("foot", 0.3, 1.6, 1, 1.6);
		converge.scale("foot", 0.6, 0.02);
	}

	// --- First Flame Before Heaven ---

	static final int[] BLACK_FLAME = {0x050208, 0x1A0A2A, 0x3E1E5A, 0x7A5AA8, 0xE6E0FF};
	/** How far its petals open from upright (degrees): a lotus's cup, not a flat star. */
	static final double FLOWER_OPEN = 56;

	/**
	 * The flower of chaos-fire that burned before heaven, its heart at the origin, lying open: eight great petals of flame, black and white by turns
	 * ("petal_0".., opening out of the "flower"), its heart a white star in a ring of black fire ("heart").
	 */
	static void chaosFlower(SpellArt.Model m) {
		m.tpp = 1.4;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone flower = m.bone("flower", "root", 0, 0, 0);
		for (int k = 0; k < 8; k++) {
			boolean white = k % 2 == 0;
			Nirvana.petal(m, "flower", "petal_" + k, SpellArt.v3(0, 2, 0), 45.0 * k, 40, 22, white ? "petal_white" : "petal_black",
					Fire.flame(4, white ? 3001 : 3003, white ? Nirvana.WHITE_FLAME : BLACK_FLAME), 1.2, k % 4 < 2);
		}
		SpellArt.Bone heart = m.bone("heart", "flower", 0, 6, 0);
		ball(heart, 0, 6, 0, 5, 1.2, t -> SpellArt.glow(SpellArt.mix(0xFFFFFF, 0xE6E0FF, Math.hypot(t.px, t.py - 6) / 6), 0.98), 3);
		for (int k = 0; k < 10; k++) {
			double th = 36.0 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 6, 4, Math.cos(Math.toRadians(th)) * 6);
			SpellArt.shared(m, heart, "heart_black_flame", false, base, 10, 6, 15, th + 180, 0, Fire.flame(3, 3005, BLACK_FLAME), 2, k % 2 == 0);
		}
		// It opens (played once): the bud of chaos-fire thrown open into its flower.
		SpellArt.Anim open = m.anim("open", 1.4, null);
		for (double tt : new double[] {0, 0.35, 0.7, 1.05, 1.4}) for (int k = 0; k < 8; k++) open.rot("petal_" + k, tt, 6 + (FLOWER_OPEN - 6) * SpellArt.easeOut(tt / 1.4), 0, 0);
		open.sample("root", "scale", 0, 1.4, 6, t -> {
			double s = Math.max(0.02, 0.3 + 0.7 * SpellArt.easeOut(t / 1.4));
			return new double[] {s, s, s};
		});
		// It burns (looped): its petals swaying, its heart throbbing, the flower turning slowly.
		double T = 4;
		SpellArt.Anim burn = m.anim("burn", T, "true");
		for (int k = 0; k < 8; k++) {
			int kk = k;
			burn.sample("petal_" + k, "rotation", 0, T, 8, t -> new double[] {FLOWER_OPEN + 4 * Math.sin(2 * Math.PI * t / T * 2 + kk), 0, 0});
		}
		burn.sample("flower", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		burn.sample("heart", "scale", 0, T, 8, t -> {
			double s = 1 + 0.12 * Math.sin(2 * Math.PI * t / T * 4);
			return new double[] {s, s, s};
		});
		// It implodes (played once, held): every petal slammed shut in on its heart, the heart blazing, then all of it gone to a point.
		SpellArt.Anim implode = m.anim("implode", 0.6, "hold_on_last_frame");
		for (int k = 0; k < 8; k++) {
			implode.rot("petal_" + k, 0, FLOWER_OPEN, 0, 0);
			implode.rot("petal_" + k, 0.3, -10, 0, 0);
		}
		implode.scale("heart", 0, 1);
		implode.scale("heart", 0.3, 2.2);
		implode.scale("heart", 0.6, 0.02);
		implode.scale("root", 0, 1);
		implode.scale("root", 0.35, 0.8);
		implode.scale("root", 0.6, 0.02);
	}

	// --- Hand of Houtu ---

	/** The hand's stone (painted where it lies): the earth's yellow jade, veined with gold, darker in its folds. */
	static SpellArt.Painter houtuStone(int seed) {
		return t -> {
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed, 2) - 1), 18);
			if (vein > 0.74) return SpellArt.glow(SpellArt.mix(Metal.GOLD, 0xFFF0B0, (vein - 0.74) * 3), 0.96);
			double n = SpellArt.fbm3(t.px * 0.12, t.py * 0.12, t.pz * 0.12, seed + 2, 3), fine = SpellArt.noise(t.px * 0.7 + t.pz * 0.7, t.py * 0.7, seed + 4);
			return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.42 + (n - 0.5) * 0.8 + 0.08 * fine + 0.2 * Math.max(0, t.ny) - 0.25 * Math.max(0, -t.ny)), 0x4A3A1C, Earth.JADE_Y_DARK, Earth.JADE_Y, Earth.JADE_Y_LIGHT));
		};
	}

	/**
	 * The Hand of Houtu, its wrist on the ground at the origin, reaching up palm open: the forearm rising out of the land ("arm"), banded with
	 * gold; the palm ("hand"), four fingers of three joints ("finger_0".., "finger_0_1"..) and the thumb ("thumb", "thumb_1"), that close.
	 */
	static void houtuHand(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter stone = houtuStone(3101);
		SpellArt.Bone arm = m.bone("arm", "root", 0, 0, 0);
		// The forearm, broad below and narrowing to the wrist, a bracelet of gold at the wrist.
		arm.box(0, -12, 0, 15, 14, 13, stone).density(1.2);
		arm.box(0, 0.5, 0, 13, 11, 11, stone).density(1.2);
		Vessel.ring(arm, 0, 3, 0, 7.6, 2.2, 16, Metal.gold(3103));
		SpellArt.Bone hand = m.bone("hand", "arm", 0, 6, 0);
		// The palm, the heel of the thumb swelling at its side, the ridge of the knuckles across its top.
		hand.box(0, 9.5, 0, 18, 9, 9, stone);
		hand.box(5.6, 8.2, 0.6, 7, 7.5, 9.8, stone);
		hand.box(0, 14, 0, 18.6, 1.8, 9.6, stone);
		for (int f = 0; f < 4; f++) {
			double x = -6.6 + f * 4.4, len = f == 1 || f == 2 ? 7 : 6;
			String up = "hand";
			double y = 14;
			for (int j = 0; j < 3; j++) {
				String name = j == 0 ? "finger_" + f : "finger_" + f + "_" + j;
				SpellArt.Bone b = m.bone(name, up, x, y, 0);
				double w = 3.9 - j * 0.35;
				b.box(x, y + len / 2, 0, w, len, w + 0.4, stone);
				b.box(x, y + 0.5, 0, w + 0.6, 1.4, w + 0.9, stone);
				if (j == 2) b.box(x, y + len + 0.45, 0, w - 0.9, 0.9, w - 0.5, stone);
				up = name;
				y += len;
				len *= 0.8;
			}
		}
		SpellArt.Bone thumb = m.bone("thumb", "hand", 9, 8, -1);
		thumb.box(11.5, 10.5, -1, 4, 6, 4.4, stone).rot(0, 0, -30);
		SpellArt.Bone thumb1 = m.bone("thumb_1", "thumb", 13.5, 13, -1);
		thumb1.box(14.5, 16, -1, 3.6, 5, 4, stone).rot(0, 0, -15);
		// It rises (played once): up out of the land, its fingers opening.
		SpellArt.Anim rise = m.anim("rise", 1.0, null);
		rise.sample("root", "position", 0, 1.0, 6, t -> new double[] {0, -46 * (1 - SpellArt.easeOut(t / 1.0)), 0});
		// It waits (looped): its fingers stirring.
		double T = 1.5;
		SpellArt.Anim open = m.anim("open", T, "true");
		for (int f = 0; f < 4; f++) {
			int ff = f;
			open.sample("finger_" + f, "rotation", 0, T, 6, t -> new double[] {-8 + 4 * Math.sin(2 * Math.PI * t / T + ff), 0, 0});
		}
		// It grasps (played once, held): every finger curled over what it holds, the thumb closed across them.
		SpellArt.Anim grasp = m.anim("grasp", 0.5, "hold_on_last_frame");
		for (int f = 0; f < 4; f++) {
			grasp.rot("finger_" + f, 0, -8, 0, 0);
			grasp.rot("finger_" + f, 0.5, 70, 0, 0);
			for (int j = 1; j < 3; j++) {
				grasp.rot("finger_" + f + "_" + j, 0, 0, 0, 0);
				grasp.rot("finger_" + f + "_" + j, 0.5, 60, 0, 0);
			}
		}
		grasp.rot("thumb", 0, 0, 0, 0);
		grasp.rot("thumb", 0.5, 0, 40, 40);
		// It slams (played once, held): the whole fist brought down forward onto the land.
		SpellArt.Anim slam = m.anim("slam", 0.6, "hold_on_last_frame");
		for (int f = 0; f < 4; f++) {
			slam.rot("finger_" + f, 0, 70, 0, 0);
			slam.rot("finger_" + f, 0.6, 70, 0, 0);
			for (int j = 1; j < 3; j++) {
				slam.rot("finger_" + f + "_" + j, 0, 60, 0, 0);
				slam.rot("finger_" + f + "_" + j, 0.6, 60, 0, 0);
			}
		}
		slam.rot("thumb", 0, 0, 40, 40);
		slam.rot("thumb", 0.6, 0, 40, 40);
		slam.rot("arm", 0, 0, 0, 0);
		slam.rot("arm", 0.35, 95, 0, 0);
		slam.rot("arm", 0.6, 88, 0, 0);
	}

	// --- Falling Continent ---

	/** Torn-up stone (painted where it lies): greys and browns in strata, softly mottled, lighter where it faces up, {@code dark} the darker. */
	static SpellArt.Painter landRock(int seed, double dark) {
		return t -> {
			double strata = Math.sin(t.py * 1.1 + 4 * SpellArt.fbm(t.px * 0.05, t.pz * 0.05, seed, 2));
			double n = SpellArt.fbm3(t.px * 0.14, t.py * 0.14, t.pz * 0.14, seed + 1, 3);
			double light = SpellArt.clamp01(0.52 + 0.14 * strata + (n - 0.5) * 0.45 + 0.15 * t.ny - dark);
			return SpellArt.col(SpellArt.ramp(light, 0x24201C, 0x4A443C, 0x6E685A, 0x948E7C));
		};
	}

	/** A plate of the land (painted where it lies): meadow on top, a fringe of grass and a band of soil down its sides, its stone beneath. */
	static SpellArt.Painter landPlate(int seed) {
		SpellArt.Painter rock = landRock(seed + 3, 0.05);
		return t -> {
			if (t.ny > 0.5) {
				double n = SpellArt.fbm(t.px * 0.06, t.pz * 0.06, seed, 3), fine = SpellArt.noise(t.px * 0.6, t.pz * 0.6, seed + 1);
				return SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.35 + 0.5 * n + 0.15 * fine), 0x2A5A24, 0x447E34, 0x6AA046, 0x8CB85A));
			}
			if (t.ny < -0.5) return rock.paint(t);
			double fromTop = t.sy() - t.ly;
			if (fromTop < 0.9) return SpellArt.col(0x4A7A34);
			if (fromTop < 3.4) return SpellArt.col(SpellArt.mix(0x4E3420, 0x76563A, 0.4 + 0.4 * SpellArt.noise(t.px * 0.5, t.py * 0.8 + t.pz * 0.5, seed + 2)));
			return rock.paint(t);
		};
	}

	/**
	 * The Falling Continent, its heart at the origin, some eighty pixels across: a land torn up whole, its top green ("land": meadows, forests,
	 * grey mountains capped with snow, a lake), its underside the bedrock it was torn from, a jagged mass narrowing down, spurs of rock hanging
	 * off it ("roots").
	 */
	static void fallingContinent(SpellArt.Model m) {
		m.tpp = 0.8;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone land = m.bone("land", "root", 0, 0, 0);
		double[][] plates = {{0, 0, 0, 70, 8, 56}, {-14, -1, 18, 46, 8, 30}, {20, -1, -16, 40, 8, 34}, {-24, -2, -14, 30, 8, 26}, {22, -2, 20, 30, 8, 24}};
		for (int i = 0; i < plates.length; i++) {
			double[] p = plates[i];
			land.box(p[0], p[1], p[2], p[3], p[4], p[5], landPlate(3201 + i * 7)).density(0.8);
		}
		// Its mountains of grey stone, snow on their peaks and their high slopes.
		double[][] peaks = {{-12, -8, 18}, {8, 10, 22}, {-26, 14, 13}, {18, -16, 15}};
		for (int k = 0; k < peaks.length; k++) {
			double[] pk = peaks[k];
			double h = pk[2], w = h * 1.25;
			SpellArt.Painter stone = landRock(3205 + k, -0.12);
			for (int i = 0; i < 5; i++) {
				double y = 4 + i * h / 5, ww = w * (1 - i / 5.0);
				boolean snowy = i >= 3;
				land.box(pk[0], y + h / 10, pk[1], ww, h / 5 + 0.2, ww * 0.9, snowy ? t -> t.ny > 0.5 || t.ly > t.sy() * 0.4 ? SpellArt.col(SpellArt.mix(0xDCE6EC, 0xFFFFFF, SpellArt.noise(t.px, t.pz, 3209)))
						: stone.paint(t) : stone).rot(0, 23 * i + k * 11, 0).density(0.8);
			}
		}
		// Its forests: dark crowns standing on it.
		for (int i = 0; i < 26; i++) {
			double x = -30 + (i * 37 % 60), z = -24 + (i * 23 % 48);
			if (Math.abs(x + 12) < 9 && Math.abs(z + 8) < 9 || Math.abs(x - 8) < 10 && Math.abs(z - 10) < 10 || Math.abs(x - 26) < 8 && Math.abs(z - 4) < 6) continue;
			int ii = i;
			land.box(x, 6.5, z, 3.4, 5, 3.4, t -> SpellArt.col(SpellArt.ramp(SpellArt.clamp01(0.3 + 0.5 * t.up() + 0.2 * SpellArt.noise(t.px * 0.8, t.py * 0.8, 3221 + ii)),
					0x10281A, 0x1E4A2A, 0x2E6A36))).rot(0, i * 30, 0).density(1);
		}
		SpellArt.sheet(land, 26, 4.3, 4, 14, 10, 0, 20, 0, t -> SpellArt.glow(SpellArt.mix(Water.DEEP, Water.TEAL, 0.3 + 0.5 * SpellArt.noise(t.across() * 4, t.back() * 4, 3223)), 0.85)).density(1);
		// Beneath: the bedrock it was torn up with, a jagged mass narrowing down, and spurs of it hanging.
		SpellArt.Bone roots = m.bone("roots", "root", 0, 0, 0);
		double[][] under = {{64, 50, -6}, {52, 41, -11}, {41, 32, -16}, {30, 24, -21}, {20, 16, -26}, {11, 9, -31}};
		for (int i = 0; i < under.length; i++) {
			roots.box(i % 2 * 3 - 1.5, under[i][2], i % 3 - 1, under[i][0], 5.6, under[i][1], landRock(3211 + i, 0.12 + i * 0.03)).rot(0, i * 17, 0).density(0.8);
		}
		for (int i = 0; i < 10; i++) {
			double a = Math.toRadians(i * 36 + 10), r = 14 + (i % 3) * 7, x = Math.sin(a) * r, z = Math.cos(a) * r * 0.8, len = 8 + (i % 4) * 4;
			double[] top = SpellArt.v3(x, -7 - (r - 14) * 0.6, z), mid = SpellArt.add(top, SpellArt.v3(-x * 0.08, -len * 0.55, -z * 0.08)),
					tip = SpellArt.add(top, SpellArt.v3(-x * 0.14, -len, -z * 0.14));
			SpellArt.limb(roots, top, mid, 6, 6, landRock(3217, 0.18)).density(0.8);
			SpellArt.limb(roots, mid, tip, 3.2, 3.2, landRock(3219, 0.22)).density(0.8);
		}
		// It falls (looped): tumbling a little, slowly.
		double T = 3;
		SpellArt.Anim fall = m.anim("fall", T, "true");
		fall.sample("root", "rotation", 0, T, 8, t -> new double[] {5 * Math.sin(2 * Math.PI * t / T), 0, 4 * Math.cos(2 * Math.PI * t / T)});
		// It strikes (played once, held): crushed flat into the land, sinking into what it smashed.
		SpellArt.Anim impact = m.anim("impact", 1.2, "hold_on_last_frame");
		impact.rot("root", 0, 0, 0, 0);
		impact.rot("root", 1.2, 0, 0, 0);
		impact.scale("root", 0, 1);
		impact.scale("root", 0.15, 1.12, 0.6, 1.12);
		impact.scale("root", 1.2, 1.1, 0.4, 1.1);
		impact.pos("root", 0, 0, 0, 0);
		impact.pos("root", 1.2, 0, -14, 0);
	}

	// --- Glacier of the First Age ---

	/**
	 * The glacier's ice (painted where it lies): blue-white, deeper blue low in it and in its shadowed faces, the faint bands of its ages, a few
	 * thin crevasses deep blue down it, snow on its tops; glowing faintly.
	 */
	static SpellArt.Painter glacierIce(int seed) {
		return t -> {
			if (t.ny > 0.6) return SpellArt.col(SpellArt.mix(0xDDEBF3, 0xFFFFFF, 0.5 + 0.5 * SpellArt.noise(t.px * 0.3, t.pz * 0.3, seed)));
			double band = Math.sin(t.py * 0.8 + 5 * SpellArt.fbm(t.px * 0.06, t.pz * 0.06, seed, 2));
			double crevasse = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.16, t.py * 0.015, t.pz * 0.16, seed + 2, 2) - 1);
			if (crevasse > 0.975) return SpellArt.glow(SpellArt.mix(0x123A78, 0x2A6AB0, (crevasse - 0.975) * 30), 0.96);
			double low = SpellArt.clamp01(1 - t.py / 44), shade = Math.abs(t.nx) * 0.12;
			int c = SpellArt.mix(SpellArt.mix(0xD6F2FF, 0x86C2E8, low * 0.75 + shade), 0xEFFAFF, 0.12 + 0.08 * band);
			return SpellArt.glow(c, 0.92);
		};
	}

	/** Frost-mist (on a sheet): white-blue, thick at its middle, thinning to nothing at its edges, broken into drifts. */
	static SpellArt.Painter frostMist(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.max(Math.abs(x), Math.abs(z) * 1.1);
			double drift = SpellArt.fbm(t.across() * 6, t.back() * 4, seed, 3), a = (1 - r) * (0.2 + 0.5 * drift);
			if (a < 0.04) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(0xBFE0F2, 0xFFFFFF, drift), SpellArt.clamp01(a));
		};
	}

	static final int SERACS = 13;

	/**
	 * The Glacier of the First Age, grinding forward along -z, its foot at the origin: a wall of ancient ice some fifty pixels across, its body
	 * ranks of seracs, broken towers of ice taller toward its back ("wall"), the great slabs of its face that calve ("face_0".."face_3"), a
	 * mist of frost rolling at its foot ("mist").
	 */
	static void primalGlacier(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone wall = m.bone("wall", "root", 0, 0, 0);
		SpellArt.Painter ice = glacierIce(3301);
		for (int rank = 0; rank < 2; rank++) {
			for (int i = 0; i < SERACS; i++) {
				double hash = SpellArt.hash(i, rank, 3305), hash2 = SpellArt.hash(i, rank, 3307);
				double x = -25 + i * 50.0 / (SERACS - 1) + (hash2 - 0.5) * 1.5, h = (rank == 0 ? 24 : 32) + 9 * hash - 5 * Math.abs(i - 6) / 6.0;
				double z = rank == 0 ? 9 : 22, d = rank == 0 ? 16 : 14;
				wall.box(x, h / 2, z + (hash - 0.5) * 3, 5.6, h, d, ice).rot((hash2 - 0.5) * 8, (hash - 0.5) * 14, (hash2 - 0.5) * 7).density(1);
			}
		}
		for (int i = 0; i < 4; i++) {
			double x = -18 + i * 12, h = 22 + (i % 2) * 7;
			SpellArt.Bone face = m.bone("face_" + i, "wall", x, 0, 0);
			face.box(x, h / 2, -1, 11.5, h, 6, glacierIce(3303 + i)).rot(-4 * (i % 2 == 0 ? 1 : -1), (i - 1.5) * 4, 0).density(1);
		}
		SpellArt.Bone mist = m.bone("mist", "root", 0, 0, 0);
		SpellArt.sheet(mist, 0, 1, -10, 76, 30, 0, 0, 0, frostMist(3311)).density(0.8);
		SpellArt.sheet(mist, 0, 4, -6, 64, 16, 0, 0, 0, frostMist(3313)).density(0.8);
		// It rises (played once): heaved up out of the land, its mist spreading.
		SpellArt.Anim rise = m.anim("rise", 1.0, null);
		rise.sample("wall", "position", 0, 1.0, 6, t -> new double[] {0, -44 * (1 - SpellArt.easeOut(t / 1.0)), 0});
		rise.sample("mist", "scale", 0, 1.0, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.0));
			return new double[] {s, 1, s};
		});
		// It grinds on (looped): shuddering as it goes, its mist rolling.
		double T = 1;
		SpellArt.Anim advance = m.anim("advance", T, "true");
		advance.sample("wall", "rotation", 0, T, 8, t -> new double[] {0.6 * Math.sin(2 * Math.PI * t / T * 3), 0, 0.4 * Math.sin(2 * Math.PI * t / T * 2)});
		advance.sample("mist", "scale", 0, T, 8, t -> new double[] {1 + 0.06 * Math.sin(2 * Math.PI * t / T), 1, 1 + 0.06 * Math.cos(2 * Math.PI * t / T)});
		// It calves (played once, held): the slabs of its face toppling forward off it and breaking, gone.
		SpellArt.Anim calve = m.anim("calve", 1.0, "hold_on_last_frame");
		for (int i = 0; i < 4; i++) {
			int ii = i;
			calve.sample("face_" + i, "rotation", 0, 1.0, 6, t -> new double[] {85 * SpellArt.easeIn(SpellArt.clamp01((t - ii * 0.08) / 0.6)), 0, 0});
			calve.sample("face_" + i, "scale", 0, 1.0, 6, t -> {
				double s = Math.max(0.02, 1 - SpellArt.easeIn(SpellArt.clamp01((t - 0.5) / 0.5)));
				return new double[] {s, s, s};
			});
		}
	}

	// --- The Primordial Sea ---

	/**
	 * A wave of the primordial sea (on a blade standing up): the abyss at its foot through deep blue and teal to the light at its crest, currents
	 * of foam through it, its crest foaming white and ragged; seen through a little.
	 */
	static SpellArt.Painter seaWave(int seed) {
		return t -> {
			double v = t.up(), u = t.back();
			double crest = 0.84 + 0.05 * Math.sin(u * Math.PI * 2 + seed) + 0.05 * SpellArt.noise(u * 12, 0.5, seed);
			if (v > crest + 0.1) return SpellArt.CLEAR;
			if (v > crest) return SpellArt.glow(SpellArt.mix(Water.FOAM, 0xFFFFFF, (v - crest) / 0.1), 0.94);
			double current = Math.pow(1 - Math.abs(2 * SpellArt.fbm(u * 3 + v * 2.5, v * 5, seed + 1, 3) - 1), 9);
			int c = SpellArt.ramp(SpellArt.clamp01(v / crest), Water.ABYSS, Water.DEEP, Water.TEAL, Water.CYAN);
			if (current > 0.72) c = SpellArt.mix(c, Water.FOAM, Math.min(1, (current - 0.72) * 2.5));
			return SpellArt.glow(c, 0.72 + 0.2 * v);
		};
	}

	/**
	 * The sea within (on a sheet over the land): the deep, dark, wheeling in a whirlpool, arms of foam and teal light spiralling in to the black
	 * at its heart, brighter toward its walls; seen through.
	 */
	static SpellArt.Painter seaFloor(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 1) return SpellArt.CLEAR;
			double arm = 0.5 + 0.5 * Math.sin(a * 4 + r * 11 + 2 * SpellArt.fbm(x * 2, z * 2, seed, 2));
			int c = SpellArt.mix(SpellArt.mix(0x02060C, Water.DEEP, SpellArt.clamp01(r * 1.4)), Water.TEAL, arm * 0.35 * r);
			if (arm > 0.93 && r > 0.15) c = SpellArt.mix(c, Water.FOAM, (arm - 0.93) * 10 * r);
			return SpellArt.glow(c, 0.66);
		};
	}

	static final double SEA_R = 46, SEA_HIGH = 40;
	static final int SEA_WAVES = 24;

	/**
	 * The Primordial Sea, its heart at the origin on the land: the waters before heaven and earth risen in a ring of {@link #SEA_WAVES} towering
	 * waves {@link #SEA_R} pixels round and {@link #SEA_HIGH} tall, facing in ("wave_0".., each turned on a bone of its own, to lean in and crash
	 * over the heart), the sea within it wheeling in a whirlpool ("water"), and breakers round its foot ("breakers").
	 */
	static void primordialSea(SpellArt.Model m) {
		m.tpp = 0.8;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		m.bone("ring", "root", 0, 0, 0);
		double width = 2 * Math.PI * SEA_R / SEA_WAVES * 1.3;
		for (int k = 0; k < SEA_WAVES; k++) {
			double th = 360.0 * k / SEA_WAVES, rad = Math.toRadians(th);
			Nirvana.petal(m, "ring", "wave_" + k, SpellArt.v3(Math.sin(rad) * SEA_R, 0, Math.cos(rad) * SEA_R), th, SEA_HIGH, width, "sea_wave", seaWave(3401), 0.8, k % 2 == 1);
		}
		SpellArt.Bone water = m.bone("water", "root", 0, 0, 0);
		SpellArt.sheet(water, 0, 1.2, 0, 2 * SEA_R, 2 * SEA_R, 0, 0, 0, seaFloor(3402)).density(0.7);
		SpellArt.Bone breakers = m.bone("breakers", "root", 0, 0, 0);
		SpellArt.sheet(breakers, 0, 0.6, 0, 2 * SEA_R + 24, 2 * SEA_R + 24, 0, 0, 0, Water.foamRing(0.86, 0.1, 9, 3405)).density(0.5);
		// It rises (played once): the sea welling up out of the land into its ring of waves, the waters within spreading.
		SpellArt.Anim rise = m.anim("rise", 1.4, null);
		rise.sample("ring", "scale", 0, 1.4, 6, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut(t / 1.4)), 1});
		rise.sample("water", "scale", 0, 1.4, 5, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(Math.min(1, t / 0.7)));
			return new double[] {s, 1, s};
		});
		// It churns (looped): its waves heaving, leaning in and out by turns round the ring, its currents wheeling, each its own way.
		double T = 3;
		SpellArt.Anim churn = m.anim("churn", T, "true");
		for (int k = 0; k < SEA_WAVES; k++) {
			int kk = k;
			churn.sample("wave_" + k, "rotation", 0, T, 8, t -> new double[] {-6 + 5 * Math.sin(2 * Math.PI * t / T + kk * 0.9), 0, 0});
		}
		churn.sample("water", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		churn.sample("breakers", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		// It closes (played once, held): every wave leaning in and crashing down over the heart.
		SpellArt.Anim close = m.anim("close", 1.0, "hold_on_last_frame");
		for (int k = 0; k < SEA_WAVES; k++) {
			double lag = (k % 3) * 0.05;
			close.sample("wave_" + k, "rotation", 0, 1.0, 7, t -> new double[] {-6 - 80 * SpellArt.easeIn(SpellArt.clamp01((t - lag) / 0.8)), 0, 0});
		}
	}

	// --- Heaven-Slaying Sword Rises ---

	static final double SLAYER_LENGTH = 110;

	/** The heaven-slaying sword's blade's half-width at {@code y} up it: {@link #SLAYER_WIDE}'s half up its length, drawn in to its point over its last 22 pixels. */
	static double slayerHalf(double y) {
		return SLAYER_WIDE / 2 * Math.min(1, (SLAYER_LENGTH - y) / 22 + 0.02);
	}

	static final double SLAYER_WIDE = 14;

	/**
	 * The heaven-slaying sword's blade (standing point up, its broad faces forward and back, solid to its outline: {@link #slayerHalf}):
	 * sword-light white and gold, a fuller of glyphs; its edges white, the faces at its outline (its sides, its point's steps) all edge.
	 */
	static SpellArt.Painter slayerBlade(int seed) {
		return t -> {
			double y = t.qy(), u = t.qx() / SLAYER_WIDE, half = slayerHalf(y) / SLAYER_WIDE;
			if (t.side() || t.flat()) return SpellArt.glow(0xFFFFFF, 0.97);
			double edge = Math.max(0, half - Math.abs(u));
			if (edge < 0.1) return SpellArt.glow(SpellArt.mix(0xFFFFFF, Metal.GLEAM, edge * 10), 0.98);
			if (Math.abs(u) < 0.1 && y > 10 && y < SLAYER_LENGTH - 26) {
				double cell = y / 10;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci, seed, (u + 0.1) / 0.2, 1 - (cell - ci))) return SpellArt.glow(0xFFE89A, 0.98);
				return SpellArt.glow(SpellArt.mix(Metal.SILVER, 0xFFF4D0, 0.3), 0.92);
			}
			return SpellArt.glow(SpellArt.mix(Metal.SILVER, Metal.GLEAM, 0.4 + 0.4 * SpellArt.noise(y * 0.2 + seed, u * 5, seed)), 0.92);
		};
	}

	/** The halo round the heaven-slaying sword's point (on a sheet): a ring of gold light, rays of sword-light out from it, white at its heart. */
	static SpellArt.Painter slayerHalo() {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 1) return SpellArt.CLEAR;
			double ring = Math.exp(-Math.pow((r - 0.6) / 0.06, 2));
			double rays = Math.pow(Math.max(0, Math.cos(a * 12)), 8) * SpellArt.clamp01((r - 0.15) / 0.3) * (1 - r);
			double glow = Math.max(ring, rays * 0.9) + 0.3 * Math.pow(1 - r, 3);
			if (glow < 0.06) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(0xFFE08A, 0xFFFFFF, Math.max(ring, 1 - r)), SpellArt.clamp01(glow));
		};
	}

	/** A wing of the sword's guard (on a blade): a gold feather, broad at its root, curving to its point, its shaft bright down its middle. */
	static SpellArt.Painter guardWing(int seed) {
		return t -> {
			double v = t.up(), u = t.back() - 0.5, half = 0.5 * Math.pow(Math.max(0, 1 - v), 0.7) * Math.min(1, v * 5 + 0.25);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			if (Math.abs(u) < 0.06) return SpellArt.glow(0xFFF2C0, 0.97);
			double vane = Math.abs(Math.sin((v * 14 - Math.abs(u) * 6) * Math.PI));
			return SpellArt.col(SpellArt.mix(Metal.GOLD_DARK, Metal.GOLD, 0.45 + 0.4 * vane + 0.15 * SpellArt.noise(v * 8, u * 8, seed)));
		};
	}

	/**
	 * The Heaven-Slaying Sword, rising point up out of the land, its guard at the origin: its blade of sword-light ({@link #SLAYER_LENGTH} pixels,
	 * "blade"), its guard of gold swept up like spread wings, its grip and pommel below (in the land as it rises), a halo of light round its
	 * point ("halo").
	 */
	static void heavenSlayingSword(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone blade = m.bone("blade", "root", 0, 0, 0);
		// Its blade, solid to its outline: up its length in pieces, then its point drawn in slice by slice (a box painted to its point and clear
		// round it was a sheet seen edge-on: the user, 2026-10-25, "2D basically"); thick, a ridge of sword-light down its middle.
		SpellArt.Painter light = slayerBlade(3501);
		double taper = SLAYER_LENGTH - 22;
		for (int i = 0; i < 5; i++) {
			double y0 = 2 + i * 18, y1 = Math.min(taper, y0 + 18);
			if (y1 > y0) blade.box(0, (y0 + y1) / 2, 0, SLAYER_WIDE, y1 - y0, 3.4, light).density(1);
		}
		for (SpellArt.Cube slice : SpellArt.extrude(blade, SpellArt.Plane.XY, 0, taper, SLAYER_LENGTH, 1, 3.4, y -> new double[] {-slayerHalf(y), slayerHalf(y)}, light)) slice.density(1);
		blade.box(0, (2 + SLAYER_LENGTH - 8) / 2, 0, 3, SLAYER_LENGTH - 10, 4.8, light).density(1); // the ridge, its fuller of glyphs on it
		SpellArt.Painter gold = Metal.gold(3503);
		blade.box(0, 0, 0, 30, 3, 4, gold);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(blade, SpellArt.v3(s * 14, 0, 0), SpellArt.v3(s * 20, 6, 0), 3, 3.4, gold);
			for (int j = 0; j < 3; j++) {
				SpellArt.shared(m, blade, "slayer_wing", false, SpellArt.v3(s * (8 + j * 4), 1.2, 0.3), 16 - j * 3, 5.5, 0, 90, s * (40 + j * 14), guardWing(3509), 1.5, s > 0);
			}
		}
		blade.box(0, -8, 0, 4, 14, 4, Metal.grip());
		blade.box(0, -16, 0, 6, 6, 6, gold).rot(45, 45, 0);
		SpellArt.Bone halo = m.bone("halo", "root", 0, SLAYER_LENGTH - 6, 0);
		SpellArt.sheet(halo, 0, SLAYER_LENGTH - 6, 0, 34, 34, 90, 0, 0, slayerHalo()).density(1.5);
		// It rises (played once): up out of the land, point first.
		SpellArt.Anim rise = m.anim("rise", 1.0, null);
		rise.sample("root", "position", 0, 1.0, 6, t -> new double[] {0, -SLAYER_LENGTH * (1 - SpellArt.easeOut(t / 1.0)), 0});
		// It stands (looped): its halo wheeling, its light breathing.
		double T = 2;
		SpellArt.Anim stand = m.anim("stand", T, "true");
		stand.sample("halo", "rotation", 0, T, 8, t -> new double[] {0, 0, 360 * t / T});
		stand.sample("blade", "scale", 0, T, 8, t -> new double[] {1 + 0.04 * Math.sin(2 * Math.PI * t / T * 2), 1, 1});
		// It flares (played once): blazing up as its cuts run out from it.
		SpellArt.Anim flare = m.anim("flare", 0.5, null);
		flare.scale("root", 0, 1);
		flare.scale("root", 0.15, 1.12, 1.04, 1.12);
		flare.scale("root", 0.5, 1);
	}

	// --- Primal Axe Splits Chaos ---

	/** The axe's bronze (painted where it lies): dark bronze of the beginning, chaos swirling in it in grey and gold. */
	static SpellArt.Painter axeHead(double edgeZ, int seed) {
		return t -> {
			if (t.qz() < edgeZ + 2.2) return SpellArt.glow(SpellArt.mix(0xFFFFFF, Metal.GLEAM, (t.qz() - edgeZ) / 2.2), 0.97);
			double swirl = Math.sin(Math.atan2(t.py, t.pz) * 3 + Math.hypot(t.py, t.pz) * 0.4 + seed);
			if (swirl > 0.85) return SpellArt.glow(SpellArt.mix(Metal.GOLD, 0xFFE89A, swirl - 0.85), 0.9);
			double n = SpellArt.fbm3(t.px * 0.2, t.py * 0.2, t.pz * 0.2, seed + 2, 3);
			return SpellArt.col(SpellArt.mix(0x2A2420, 0x6A5A44, 0.3 + 0.5 * n + 0.15 * swirl));
		};
	}

	/**
	 * The axe's crescent across (along z) at {@code y} over its middle: from its edge, an arc bulging forward (-z), back to where its cheeks
	 * narrow to the haft (the higher or lower, the shorter); null above and below it.
	 */
	static double[] axeSpan(double y) {
		double ay = Math.abs(y), edgeZ = 16 - Math.sqrt(Math.max(0, 50 * 50 - y * y)), back = 5 - ay * 0.25;
		if (ay > 5) back = Math.min(back, -32 * Math.pow((ay - 5) / 15, 1.25));
		return back - edgeZ >= 0.5 ? new double[] {edgeZ, back} : null;
	}

	/**
	 * The axe's blade (its crescent, {@link #axeSpan}, extruded row by row, its broad faces its sides, {@code yc} its middle's height): its
	 * edge white with light (on the edge's own faces too, the crescent solid), its rims dark bronze, its cheeks' bronze swirled with chaos.
	 */
	static SpellArt.Painter axeBlade(double yc, int seed) {
		return t -> {
			double y = t.qy() - yc, z = t.qz();
			double edgeZ = 16 - Math.sqrt(Math.max(0, 50 * 50 - y * y)), toEdge = z - edgeZ;
			if (toEdge < 2.8) return SpellArt.glow(SpellArt.mix(0xFFFFFF, Metal.GLEAM, Math.max(0, toEdge) / 2.8), 0.97);
			if (!t.side()) return SpellArt.col(0x3A3028);
			if (toEdge < 3.6) return SpellArt.col(Metal.GOLD_DARK);
			double swirl = Math.sin(Math.atan2(y, z + 12) * 3 + Math.hypot(y, z + 12) * 0.35 + seed);
			if (swirl > 0.88) return SpellArt.glow(SpellArt.mix(Metal.GOLD, 0xFFE89A, (swirl - 0.88) * 6), 0.92);
			double n = SpellArt.fbm3(t.px * 0.15, t.py * 0.15, t.pz * 0.15, seed + 2, 3);
			return SpellArt.col(SpellArt.mix(0x241E1A, 0x6A5A44, 0.3 + 0.5 * n + 0.12 * swirl));
		};
	}

	/** The axe's haft (on a limb): ancient wood, bound in bronze bands, written with runes of chaos. */
	static SpellArt.Painter axeHaft(int seed) {
		return t -> {
			if (t.ly % 14 < 1.4) return SpellArt.col(Metal.GOLD_DARK);
			if (t.fx > 0.35 && t.fx < 0.65 && Fire.glyph((int) (t.ly / 7), seed, (t.fx - 0.35) / 0.3, (t.ly % 7) / 7)) return SpellArt.glow(Metal.GOLD, 0.9);
			return SpellArt.col(SpellArt.mix(Wood.WOOD_DARK, Wood.WOOD, 0.3 + 0.5 * SpellArt.noise(t.fx * 3 + seed, t.ly * 0.2, seed)));
		};
	}

	/** How long its haft is; how far back it is raised, and how far forward it chops (degrees from upright: past level, into the land). */
	static final double HAFT = 100, RAISED = 45, CHOPPED = 95;

	/**
	 * Pangu's Primal Axe, held by its haft's end at the origin and reaching up {@link #HAFT} pixels, its head at the top, its edge toward -z
	 * ("axe": raised back over, it chops down forward), and the chaos it trails ("chaos", sheets of grey and gold behind its edge).
	 */
	static void panguAxe(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone axe = m.bone("axe", "root", 0, 0, 0);
		SpellArt.limb(axe, SpellArt.v3(0, 0, 0), SpellArt.v3(0, HAFT, 0), 5, 5, axeHaft(3601)).density(1.2);
		double edge = -30, yc = HAFT - 20;
		// The crescent blade, solid: extruded row by row to its outline (it was a box painted to it and clear round it, two sheets with nothing
		// between, flat to see: the user, 2026-10-25); the socket that holds it on the haft; a spike behind.
		for (SpellArt.Cube row : SpellArt.extrude(axe, SpellArt.Plane.YZ, 0, yc - 21, yc + 21, 1, 3.6, y -> axeSpan(y - yc), axeBlade(yc, 3603))) row.density(1);
		axe.box(0, yc, 0.5, 7.5, 16, 8, axeHead(-99, 3605)).density(1);
		SpellArt.limb(axe, SpellArt.v3(0, yc, 4), SpellArt.v3(0, yc + 1, 13), 4, 3, axeHead(-99, 3606));
		SpellArt.Bone chaos = m.bone("chaos", "axe", 0, HAFT - 18, 0);
		for (int i = 0; i < 4; i++) {
			SpellArt.shared(m, chaos, "chaos_trail", false, SpellArt.v3(0, HAFT - 30 + i * 7, edge + 2), 30, 6, -100, 0, 0, t -> {
				double v = t.v(), u = Math.abs(t.u() - 0.5) * 2;
				if (u > 1 - v) return SpellArt.CLEAR;
				return SpellArt.glow(SpellArt.mix(0xB8B0A0, Metal.GOLD, SpellArt.noise(v * 4, u * 3, 3607)), 0.4 * (1 - v));
			}, 1, false);
		}
		// It is raised (played once, as it comes; its waiting holds it there): swung back up over its wielder.
		SpellArt.Anim raise = m.anim("raise", 0.8, null);
		raise.sample("axe", "rotation", 0, 0.8, 6, t -> new double[] {-RAISED * SpellArt.ease(t / 0.8), 0, 0});
		raise.scale("chaos", 0, 0.02);
		raise.scale("chaos", 0.8, 0.02);
		// It waits (looped), raised, trembling with what it holds.
		double T = 0.6;
		SpellArt.Anim hold = m.anim("hold", T, "true");
		hold.sample("axe", "rotation", 0, T, 6, t -> new double[] {-RAISED + 1.5 * Math.sin(2 * Math.PI * t / T), 0, 0});
		hold.scale("chaos", 0, 0.02);
		hold.scale("chaos", T, 0.02);
		// It chops (played once, held): down forward through all of it, its head driven into the land before it, chaos streaming off its edge.
		SpellArt.Anim chop = m.anim("chop", 0.4, "hold_on_last_frame");
		chop.sample("axe", "rotation", 0, 0.4, 6, t -> new double[] {-RAISED + (RAISED + CHOPPED) * SpellArt.easeIn(t / 0.4), 0, 0});
		chop.scale("chaos", 0, 0.02);
		chop.scale("chaos", 0.15, 1);
		chop.scale("chaos", 0.4, 1.2);
	}

	// --- Fengdu's Walls Rise ---

	/** The ghost city's wall (painted where it lies): blocks of black stone, its joints pale, glyphs of the dead in teal on its face here and there. */
	static SpellArt.Painter fengduStone(int seed) {
		return t -> {
			double bx = (t.px + (Math.floorMod((int) Math.floor(t.py / 2.5), 2) == 0 ? 0 : 2)) / 4, by = t.py / 2.5;
			double fx = bx - Math.floor(bx), fy = by - Math.floor(by);
			if (Math.min(fx, fy) < 0.07) return SpellArt.col(Ghost.SLATE_LIGHT);
			if (t.f == SpellArt.Face.FRONT && SpellArt.hash((int) Math.floor(bx), (int) Math.floor(by), seed) > 0.85 && Fire.glyph((int) Math.floor(bx) * 7 + (int) Math.floor(by), seed, fx, fy))
				return SpellArt.glow(Ghost.FIRE_CYAN, 0.9);
			double n = SpellArt.noise(t.px * 0.5, t.py * 0.5 + t.pz, seed);
			return SpellArt.col(SpellArt.mix(Ghost.LACQUER, Ghost.SLATE, 0.3 + 0.5 * n));
		};
	}

	/**
	 * A length of the walls of Fengdu, the city of the dead, its foot at the origin, forty pixels long along x and facing -z (in, toward the city's
	 * heart): black stone, crenellated, a gate-tower at its middle with eaves of black tile ("wall"), a soul lantern hanging at each end
	 * ("lantern_l", "lantern_r"), banners of the dead down its face.
	 */
	static void fengduWall(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone wall = m.bone("wall", "root", 0, 0, 0);
		SpellArt.Painter stone = fengduStone(3701);
		wall.box(0, 9, 0, 40, 18, 6, stone);
		for (int i = 0; i < 9; i++) wall.box(-18 + i * 4.5, 19.2, 0, 2.6, 2.4, 6, stone);
		wall.box(0, 23, 0, 12, 10, 8, stone);
		for (int s = -1; s <= 1; s += 2) wall.box(0, 29, s * 0.01, 18, 1.4, 12, Ghost.tiles(3703)).rot(s * 20, 0, 0);
		wall.box(0, 6, -3.1, 6, 10, 0.4, t -> SpellArt.col(Ghost.VOID));
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone lantern = m.bone(s < 0 ? "lantern_l" : "lantern_r", "wall", s * 18, 17, -4);
			lantern.box(s * 18, 15, -4.5, 2.6, 3.6, 2.6, Ghost.lantern(3705));
			SpellArt.blade(wall, SpellArt.v3(s * 10, 16, -3.3), 12, 5, 180, 90, 0, Ghost.banner(3707)).density(2);
		}
		// It rises (played once): up out of the land.
		SpellArt.Anim rise = m.anim("rise", 1.0, null);
		rise.sample("root", "position", 0, 1.0, 6, t -> new double[] {0, -32 * (1 - SpellArt.easeOut(t / 1.0)), 0});
		// It stands (looped): its lanterns swinging.
		double T = 2;
		SpellArt.Anim stand = m.anim("stand", T, "true");
		stand.sample("lantern_l", "rotation", 0, T, 8, t -> new double[] {8 * Math.sin(2 * Math.PI * t / T), 0, 0});
		stand.sample("lantern_r", "rotation", 0, T, 8, t -> new double[] {8 * Math.sin(2 * Math.PI * t / T + 1.5), 0, 0});
		// It falls (played once, held): toppling in on the city's heart.
		SpellArt.Anim fall = m.anim("fall", 1.0, "hold_on_last_frame");
		fall.sample("wall", "rotation", 0, 1.0, 6, t -> new double[] {85 * SpellArt.easeIn(t / 1.0), 0, 0});
	}

	// --- Ten Thousand Souls Ascend ---

	/** A soul's face among the helix's wisps (on its front; pale mist elsewhere): pale and hollow-cheeked, its eyes ghost fire, its mouth open. */
	static SpellArt.Painter soulFace(int seed) {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.glow(SpellArt.mix(0x8AA8A8, 0xDCEEEC, t.up()), 0.5);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			for (int s = -1; s <= 1; s += 2) {
				double e = Math.hypot((x - s * 0.4) / 0.24, (y - 0.18) / 0.2);
				if (e < 0.6) return SpellArt.glow(Ghost.FIRE_CYAN, 0.98);
				if (e < 1) return SpellArt.col(0x0A1416);
			}
			if (Math.abs(x) < 0.2 && y < -0.25 && y > -0.75) return SpellArt.col(0x081012);
			return SpellArt.glow(SpellArt.mix(0xB4CCCA, 0xF0FAF8, 0.5 + 0.4 * y), 0.88);
		};
	}

	/**
	 * The helix of ten thousand souls, its foot at the origin, ninety pixels tall: two strands of the dead winding up about one another, wisps of
	 * mist and ghost fire with pale faces among them ("strand_0", "strand_1", in "helix", that turns), ghost fire at its foot ("foot").
	 */
	static void soulHelix(SpellArt.Model m) {
		m.tpp = 1.4;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone helix = m.bone("helix", "root", 0, 0, 0);
		for (int k = 0; k < 2; k++) {
			SpellArt.Bone strand = m.bone("strand_" + k, "helix", 0, 0, 0);
			for (int i = 0; i < 38; i++) {
				double y = i * 2.4, th = 180 * k + i * 18, r = 11 + 3 * Math.sin(i * 0.32), rad = Math.toRadians(th);
				double[] base = SpellArt.v3(Math.sin(rad) * r, y, Math.cos(rad) * r);
				SpellArt.shared(m, strand, "soul_wisp", false, base, 13, 7.5, 150, th + 90, 0, Ghost.wisp(3801), 1.2, i % 2 == 0);
				if (i % 4 == 2) strand.box(base[0], base[1] + 1, base[2], 2.8, 3.2, 2.4, soulFace(3803)).rot(0, th + 180, 0).density(4);
			}
		}
		SpellArt.Bone foot = m.bone("foot", "root", 0, 0, 0);
		for (int k = 0; k < 10; k++) {
			double th = 36.0 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 11, 0, Math.cos(Math.toRadians(th)) * 11);
			SpellArt.shared(m, foot, "foot_fire", false, base, 14, 7, 10, th + 180, 0, Ghost.ghostFire(3, 3805), 1.4, k % 2 == 0);
		}
		// It rises (played once): winding up out of the dark.
		SpellArt.Anim rise = m.anim("rise", 1.2, null);
		rise.sample("helix", "scale", 0, 1.2, 6, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut(t / 1.2)), 1});
		// It spirals (looped): the souls winding up round and round.
		double T = 2;
		SpellArt.Anim spiral = m.anim("spiral", T, "true");
		spiral.sample("helix", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		spiral.sample("foot", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.15 * Math.sin(2 * Math.PI * t / T * 3), 1});
		// It goes down (played once, held): drawn back down into the dark.
		SpellArt.Anim descend = m.anim("descend", 1.0, "hold_on_last_frame");
		descend.sample("helix", "scale", 0, 1.0, 6, t -> new double[] {1, Math.max(0.02, 1 - SpellArt.easeIn(t / 1.0)), 1});
		descend.sample("foot", "scale", 0, 1.0, 4, t -> {
			double s = Math.max(0.02, 1 - t);
			return new double[] {s, s, s};
		});
	}

	// --- Primordial Chaos ---

	/** Chaos (on a solid, painted where it lies): heaven's gold and the demon's crimson swirled together, neither yet parted from the other. */
	static SpellArt.Painter chaos(int seed) {
		return t -> {
			double a = Math.atan2(t.pz, t.px), r = Math.hypot(t.px, t.pz);
			double swirl = Math.sin(a * 2 + r * 0.25 + t.py * 0.18 + 3 * SpellArt.fbm3(t.px * 0.05, t.py * 0.05, t.pz * 0.05, seed, 3));
			if (Math.abs(swirl) < 0.08) return SpellArt.glow(0xFFFFFF, 0.98);
			int c = swirl > 0 ? SpellArt.mix(HeavenDemon.GOLD, HeavenDemon.GOLD_PALE, swirl) : SpellArt.mix(HeavenDemon.CRIMSON, HeavenDemon.BLOOD_DARK, -swirl);
			return SpellArt.glow(c, 0.96);
		};
	}

	/** An orbit of chaos (on a sheet): a ring of light, gold or crimson, broken into arcs. */
	static SpellArt.Painter orbit(boolean heaven) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (Math.abs(r - 0.9) > 0.05 || Math.sin(a * 5) < -0.3) return SpellArt.CLEAR;
			return SpellArt.glow(heaven ? HeavenDemon.GOLD_BRIGHT : HeavenDemon.SCARLET, 0.9);
		};
	}

	/**
	 * Primordial Chaos, its heart at the origin: a sphere of the chaos before heaven and earth were parted ("sphere", turning), and two orbits
	 * of light round it, heaven's gold and the demon's crimson, wheeling against each other ("orbit_heaven", "orbit_demon").
	 */
	static void chaosSphere(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone sphere = m.bone("sphere", "root", 0, 0, 0);
		ball(sphere, 0, 0, 0, 22, 2, chaos(3901), 1.2);
		// Each orbit's tilt on a bone of its own, so its wheeling turns it about its own tilted axis.
		m.bone("orbit_heaven_turn", "root", 0, 0, 0, 20, 0, 0);
		SpellArt.Bone heaven = m.bone("orbit_heaven", "orbit_heaven_turn", 0, 0, 0);
		SpellArt.sheet(heaven, 0, 0, 0, 64, 64, 0, 0, 0, orbit(true)).density(1.4);
		m.bone("orbit_demon_turn", "root", 0, 0, 0, -25, 0, 30);
		SpellArt.Bone demon = m.bone("orbit_demon", "orbit_demon_turn", 0, 0, 0);
		SpellArt.sheet(demon, 0, 0, 0, 60, 60, 0, 0, 0, orbit(false)).density(1.4);
		// It swells (played once): out of a point.
		SpellArt.Anim swell = m.anim("swell", 1.5, null);
		swell.sample("root", "scale", 0, 1.5, 6, t -> {
			double s = Math.max(0.02, SpellArt.easeOut(t / 1.5));
			return new double[] {s, s, s};
		});
		// It churns (looped): the sphere turning, its orbits wheeling against each other.
		double T = 2;
		SpellArt.Anim churn = m.anim("churn", T, "true");
		churn.sample("sphere", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		churn.sample("orbit_heaven", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		churn.sample("orbit_demon", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		// It bursts (played once, held): swelling and gone.
		SpellArt.Anim burst = m.anim("burst", 0.6, "hold_on_last_frame");
		burst.scale("root", 0, 1);
		burst.scale("root", 0.25, 1.6);
		burst.scale("root", 0.6, 0.02);
	}
}
