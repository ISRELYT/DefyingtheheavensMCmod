/**
 * The True Dragon for SpellArt (the Immortal Realm's primordial beast, PrimordialBeast; 2026-10-26, the user: "overhaul the dragon model"):
 * the dragon of dragons, made as the Azure Dragon's true beast was (Wood#azureDragonBeast) and written where a beast's model goes
 * (geo/demon_beast/true_dragon.geo.json, its texture, glow and animations; {@code SpellArt --primordial}). The game draws it some eighty
 * blocks long (BeastKind#modelScale); BeastModels raises it to the middle of its body and plays its "swim", the head turned to its foe on
 * top, and PrimordialBeast its "roar" as it calls the lightning down.
 */
public class TrueDragon {
	// --- Its colours: gold scales, the belly's pale gold, the storm's light, its hair's silver-white, ivory ---
	static final int GOLD_DARK = 0x6E4810, GOLD_DEEP = 0xB07A1C, GOLD = 0xE8B840, GOLD_LIGHT = 0xFFE69A, BELLY = 0xF8ECC4, BELLY_SEAM = 0xB09458;
	static final int STORM_DEEP = 0x3A6AC8, STORM = 0x9CD4FF, STORM_CORE = 0xF2FAFF, HAIR = 0xF8FAFC, HAIR_SHADE = 0xB8C4D8, IVORY = 0xF4EEDC;
	static final int MOUTH = 0x5A1420, DARK = 0x2A1A08;

	static final int SEGMENTS = 28;
	static final double SEG = 7;
	/** Its head is this much the Azure Dragon's (Wood#head): a great head on a long body. */
	static final double H = 1.25;
	/** How far round its swimming wave runs from each piece of its body to the next (radians): near two waves along it. */
	static final double WAVE = 0.42;

	static double thick(int i) {
		double[] w = {9.6, 10.8, 12.0, 12.9, 13.5, 13.8, 13.8, 13.6, 13.3, 13.0, 12.7, 12.4, 12.0, 11.6, 11.2, 10.8, 10.3, 9.8, 9.2, 8.6, 7.9, 7.2, 6.5, 5.8, 5.0,
				4.2, 3.4, 2.7};
		return w[i];
	}

	/** A point of the head, the Azure Dragon's grown by {@link #H}. */
	static double[] h3(double x, double y, double z) { return SpellArt.v3(x * H, y * H, z * H); }

	/** A box of the head, the Azure Dragon's grown by {@link #H}. */
	static SpellArt.Cube hbox(SpellArt.Bone b, double cx, double cy, double cz, double w, double h, double d, SpellArt.Painter p) {
		return b.box(cx * H, cy * H, cz * H, w * H, h * H, d * H, p);
	}

	// --- Painters ---

	/**
	 * Its hide, round a body whose axis is the line x = 0, y = 0: gold scales in rows, their free rims arcs toward the tail, dark gold along
	 * its back brightening down its flanks; a thread of lightning down each flank, zigzag; the belly's pale gold plates.
	 */
	static SpellArt.Painter hide(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI; // 0 along its spine, 1 beneath
			if (t.ny < -0.6 && t.flat()) a = 1;
			// The belly's plates.
			if (a > 0.8) {
				double band = qz / 2.1 - Math.floor(qz / 2.1);
				int c = SpellArt.mix(BELLY, GOLD_LIGHT, SpellArt.noise(qx * 0.6, qz * 0.4, seed) * 0.35);
				if (band < 0.15) return SpellArt.col(SpellArt.mix(c, BELLY_SEAM, 0.8));
				return SpellArt.col(SpellArt.shade(c, -0.12 * band + (a > 0.82 ? 0 : -0.08)));
			}
			double arc = th / (2 * Math.PI) * 16;
			double rowF = qz / 2.2;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = arc + Math.floorMod(row, 2) * 0.5;
			int colI = (int) Math.floor(colF);
			double u = colF - colI, rim = 0.32 + 0.68 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			int base = SpellArt.ramp(a / 0.8, GOLD_DEEP, GOLD, GOLD_LIGHT);
			double h = SpellArt.hash(colI, row, seed) - 0.5;
			int c;
			if (v > rim) c = SpellArt.shade(base, -0.26);
			else if (rim - v < 0.13) c = SpellArt.shade(base, -0.36);
			else if (rim - v < 0.27 && a > 0.15 && a < 0.65) c = SpellArt.mix(base, 0xFFF6D0, 0.4 + h * 0.3); // the rims catch the light
			else c = SpellArt.shade(base, -0.1 + 0.3 * (v / rim) + h * 0.12);
			// The storm in it: a thread of lightning down each flank.
			double zig = qz / 3.1 + seed * 0.13;
			zig = Math.abs((zig - Math.floor(zig)) * 2 - 1);
			double seam = 1.55 + 0.16 * (zig - 0.5) + 0.05 * Math.sin(qz * 0.21), off = (Math.abs(th) - seam) * Math.max(2, Math.hypot(qx, qy));
			if (Math.abs(off) < 0.28) return SpellArt.glow(SpellArt.mix(STORM, STORM_CORE, 0.5), 0.92);
			if (Math.abs(off) < 0.8) c = SpellArt.mix(c, STORM, 0.22);
			return SpellArt.col(c);
		};
	}

	/** The plates along its spine: dark gold, a seam between each. */
	static SpellArt.Painter ridge(int seed) {
		return t -> {
			double plate = t.qz() / 1.75 - Math.floor(t.qz() / 1.75);
			int c = SpellArt.mix(GOLD_DARK, GOLD_DEEP, 0.4 + 0.4 * SpellArt.noise(t.qz() * 0.4, t.qx(), seed));
			return SpellArt.col(plate < 0.15 ? SpellArt.shade(c, -0.35) : c);
		};
	}

	/** The head's hide, by which way a face looks: dark gold above, gold down the sides paling toward the lips, pale gold beneath; fine scales. */
	static SpellArt.Painter headHide(int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy(), qz = t.qz();
			if (t.ny < -0.55) {
				double band = qz / 1.5 - Math.floor(qz / 1.5);
				return SpellArt.col(band < 0.18 ? SpellArt.mix(BELLY, BELLY_SEAM, 0.6) : SpellArt.shade(BELLY, -0.06 * band));
			}
			double height = SpellArt.clamp01((qy + 2.5 * H) / (9.5 * H));
			int base = t.ny > 0.55 ? SpellArt.mix(GOLD_DEEP, GOLD, 0.5 + 0.3 * SpellArt.noise(qx * 0.5, qz * 0.5, seed)) : SpellArt.ramp(1 - height, GOLD, GOLD_LIGHT, BELLY);
			double a = t.ny > 0.55 ? qx : qy, b = qz;
			double rowF = b / 1.3;
			int row = (int) Math.floor(rowF);
			double v = rowF - row, colF = a / 1.2 + Math.floorMod(row, 2) * 0.5;
			int colI = (int) Math.floor(colF);
			double u = colF - colI, rim = 0.3 + 0.7 * Math.sqrt(Math.max(0, 1 - Math.pow(2 * u - 1, 2)));
			double h = SpellArt.hash(colI, row, seed) - 0.5;
			int c = v > rim ? SpellArt.shade(base, -0.22) : rim - v < 0.15 ? SpellArt.shade(base, -0.32) : SpellArt.shade(base, -0.06 + 0.28 * v / rim + h * 0.1);
			return SpellArt.col(c);
		};
	}

	/** A limb's scales: small, gold, set in rows. */
	static SpellArt.Painter hideLimb(int seed) {
		return t -> {
			int row = (int) Math.floor(t.ly / 1.2), col = (int) Math.floor((t.lx + t.lz) / 1.3);
			int c = SpellArt.mix(GOLD, GOLD_LIGHT, 0.2 + SpellArt.hash(row, col, seed) * 0.4);
			double v = t.ly / 1.2 - row;
			return SpellArt.col(v < 0.18 ? SpellArt.shade(c, -0.38) : SpellArt.shade(c, 0.08 * v + ((row + col) % 2 == 0 ? -0.06 : 0.03)));
		};
	}

	/**
	 * A lock of its hair (on a blade: its mane, its beard, the tufts at its elbows, its tail's plume): silver-white in strands, gold at its
	 * root, seen through toward its edge; its end the storm's light.
	 */
	static SpellArt.Painter hair(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.max(0, 1 - v), 0.55) * (0.92 + 0.08 * Math.sin(v * 11 + seed));
			if (au > half || v > 0.985) return SpellArt.CLEAR;
			double edge = (half - au) / Math.max(0.02, half);
			if (v > 0.74) return SpellArt.glow(SpellArt.mix(HAIR, STORM, (v - 0.74) * 3.5), 0.5 + 0.4 * edge);
			double strand = Math.abs(Math.sin((u * 9 + 0.6 * Math.sin(v * 7 + seed)) * Math.PI));
			int c = SpellArt.mix(SpellArt.mix(GOLD_LIGHT, HAIR, Math.min(1, v * 3)), HAIR_SHADE, strand < 0.25 ? 0.35 : 0.04);
			return SpellArt.col(c, edge < 0.25 ? 0.7 : 0.94);
		};
	}

	/** A fin of its crest (on a blade): a tongue of the storm's light, broad at its root, flickering at its edge, a thread of lightning up it. */
	static SpellArt.Painter stormFin(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.max(0, 1 - v), 0.8) * (0.85 + 0.15 * Math.sin(v * 17 + seed * 1.3));
			if (au > half || v > 0.98) return SpellArt.CLEAR;
			double edge = (half - au) / Math.max(0.02, half);
			double tri = v * 5 + seed * 0.21;
			tri = Math.abs((tri - Math.floor(tri)) * 2 - 1) - 0.5;
			if (Math.abs(u - tri * 0.22 * (1 - v)) < 0.04) return SpellArt.glow(STORM_CORE, 0.97);
			if (edge < 0.2) return SpellArt.glow(SpellArt.mix(STORM, STORM_CORE, 0.4), 0.8);
			double n = SpellArt.noise(u * 7 + seed, v * 9, seed);
			return SpellArt.glow(SpellArt.mix(STORM_DEEP, STORM, 0.3 + 0.5 * v + 0.2 * n), 0.42 + 0.2 * (1 - v));
		};
	}

	/** Ivory, grained along the cube's length (y). */
	static SpellArt.Painter ivory(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.lx * 2 + t.lz * 2 + seed, t.ly * 0.6, seed, 3);
			int c = SpellArt.mix(SpellArt.shade(IVORY, -0.18), IVORY, n * 1.2 - 0.1);
			return SpellArt.col(Math.abs(n - 0.5) < 0.03 ? SpellArt.shade(c, -0.2) : c);
		};
	}

	/** Ivory claws, fangs and tines, the storm's light at their points (the end toward {@code tipUp}: +1 the top of the cube, -1 its foot). */
	static SpellArt.Painter claw(int tipUp) {
		return t -> {
			double toward = tipUp > 0 ? t.up() : 1 - t.up();
			if (toward > 0.75) return SpellArt.glow(SpellArt.mix(IVORY, STORM, (toward - 0.75) * 3));
			return SpellArt.col(SpellArt.mix(SpellArt.shade(IVORY, -0.28), IVORY, toward));
		};
	}

	/** The roof of its mouth (the snout's underside) and its jaw's floor: dark crimson. */
	static SpellArt.Painter mouthRoof(SpellArt.Painter skin) {
		return t -> t.f == SpellArt.Face.DOWN ? SpellArt.col(SpellArt.shade(MOUTH, -0.2 + 0.3 * SpellArt.noise(t.lx * 2, t.lz * 2, 5))) : skin.paint(t);
	}

	static SpellArt.Painter jawSkin(SpellArt.Painter skin) {
		return t -> t.f == SpellArt.Face.UP ? SpellArt.col(SpellArt.shade(MOUTH, -0.15 + 0.3 * SpellArt.noise(t.lx * 2, t.lz * 2, 6))) : skin.paint(t);
	}

	/** The nose's front: a dark cleft down its middle, ridged gold; its other faces the scales. */
	static SpellArt.Painter nose(SpellArt.Painter skin) {
		return t -> {
			if (t.f == SpellArt.Face.FRONT) {
				double x = t.across() - 0.5, y = t.up();
				if (Math.abs(x) < 0.04 && y > 0.25) return SpellArt.col(DARK);
				if (y > 0.8) return SpellArt.col(SpellArt.mix(GOLD_DEEP, GOLD, 0.4));
				return SpellArt.col(SpellArt.shade(GOLD, -0.1 + 0.25 * y + (Math.abs(y * 5 - Math.rint(y * 5)) < 0.12 ? -0.25 : 0)));
			}
			return skin.paint(t);
		};
	}

	/** Its eye on the outer side of its cube: the storm's white, burning, slit, ringed dark. */
	static SpellArt.Painter eye() {
		return t -> {
			boolean outer = t.side() && (t.f == SpellArt.Face.SIDE_B) == (t.cube.origin[0] + t.cube.size[0] / 2 > 0);
			if (!outer) return SpellArt.col(DARK);
			double x = t.back() - 0.5, y = t.up() - 0.5, r = Math.hypot(x, y * 1.25);
			if (r > 0.48) return SpellArt.col(DARK);
			if (Math.abs(x) < 0.06 && Math.abs(y) < 0.34) return SpellArt.col(0x0A1020);
			if (Math.hypot(x + 0.2, y - 0.2) < 0.08) return SpellArt.glow(0xFFFFFF);
			return SpellArt.glow(SpellArt.mix(STORM_CORE, STORM_DEEP, r * 2));
		};
	}

	// --- The True Dragon ---

	/**
	 * The True Dragon, facing -z: a body of gold scales in twenty-eight bones ("seg_0" behind the head to "seg_27", each {@link #SEG} pixels,
	 * a chain), a thread of lightning down each flank, a crest of the storm's light along its spine; its great head ("head", the jaw "jaw")
	 * with ivory antlers ("antler_l", "antler_r"), whiskers ("whisker_l_0".."_3", "whisker_r_0".."_3"), a mane of silver-white hair ("mane",
	 * "mane_l", "mane_r") and a beard, eyes of the storm's white; four legs, five-clawed ("leg_fl", "leg_fr", "leg_bl", "leg_br", each with its
	 * "_low"); and its tail's plume ("tail"). No pearl (the user, 2026-10-26: "Remove the pearl from the dragon"). The whole hangs from "root"
	 * at the back of the head, the body's line at y = 0, the body trailing back (+z).
	 */
	static void trueDragon(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 5.1;
		m.bone("root", null, 0, 0, 0);
		head(m);
		String up = "root";
		SpellArt.Painter skin = hide(11), plates = ridge(20);
		for (int i = 0; i < SEGMENTS; i++) {
			double z = i * SEG, w = thick(i), h = w * 0.9;
			double rx = 4.0 * Math.sin(i * WAVE + 0.2), ry = 5.0 * Math.sin(i * 0.3 + 1.4);
			SpellArt.Bone seg = m.bone("seg_" + i, up, 0, 0, z, i == 0 ? 0 : rx, i == 0 ? 0 : ry, 0);
			SpellArt.Cube core = seg.box(0, 0, z + SEG / 2, w, h * 0.74, SEG + 1.4, skin);
			SpellArt.Cube tall = seg.box(0, 0, z + SEG / 2, w * 0.7, h, SEG + 1.4, skin);
			if (i < SEGMENTS - 1) {
				core.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
				tall.skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			} else {
				core.skip(SpellArt.Face.FRONT);
				tall.skip(SpellArt.Face.FRONT);
			}
			// The plates along its spine, and its crest: tongues of the storm's light swept back, each over the next, leaning to either side by
			// turns, a lesser one beneath.
			seg.box(0, h / 2 + 0.3, z + SEG / 2, Math.max(0.9, w * 0.16), 0.9, SEG + 0.4, plates).skip(SpellArt.Face.FRONT, SpellArt.Face.BACK);
			if (i < SEGMENTS - 1) {
				double fh = 3.4 + w * 0.6, fw = 3.4 + w * 0.22, lean = i % 2 == 0 ? 1 : -1;
				SpellArt.blade(seg, SpellArt.v3(0, h / 2 + 0.4, z + 1.4), fh, fw, -64, lean * 5, lean * 8, stormFin(i * 7));
				SpellArt.blade(seg, SpellArt.v3(lean * 0.4, h / 2 + 0.3, z + 4.6), fh * 0.62, fw * 0.75, -72, -lean * 14, -lean * 22, stormFin(i * 7 + 3));
			}
			up = seg.name;
		}
		legs(m);
		// The tail's plume: a fan of locks of hair, the storm's light at their ends.
		SpellArt.Bone tail = m.bone("tail", "seg_" + (SEGMENTS - 1), 0, 0, SEGMENTS * SEG);
		double[][] locks = {{-90, 0, 0, 20, 7.5}, {-70, 16, 40, 18, 6.8}, {-70, -16, -40, 18, 6.8}, {-104, 22, 70, 15, 6}, {-104, -22, -70, 15, 6},
				{-60, 0, 0, 14, 5.8}, {-122, 0, 0, 14, 5.8}, {-86, 34, 88, 13, 5}, {-86, -34, -88, 13, 5}};
		for (int k = 0; k < locks.length; k++) {
			double[] f = locks[k];
			SpellArt.blade(tail, SpellArt.v3(0, 0, SEGMENTS * SEG - 1), f[3], f[4], f[0], f[1], f[2], hair(90 + k)).density(2.5);
		}
		swim(m);
		roar(m);
	}

	static void head(SpellArt.Model m) {
		SpellArt.Bone head = m.bone("head", "root", 0, 0, 0);
		SpellArt.Painter skin = headHide(31), roof = mouthRoof(skin);
		// The skull, its crown, the nape where it meets the neck; the great jowls.
		hbox(head, 0, 2.0, -6.6, 10.4, 8, 11, skin);
		hbox(head, 0, 6.5, -6.4, 7.8, 1.3, 9, skin);
		hbox(head, 0, 1.2, -1.4, 9.2, 7.8, 3, skin).skip(SpellArt.Face.BACK);
		hbox(head, 0, -0.2, -9.6, 12.6, 4.8, 6.2, skin);
		hbox(head, 0, -1.7, -10.6, 10.2, 1.6, 5.4, skin);
		// The snout, its bridge, the broad nose and its flared nostrils, the lip beneath it.
		hbox(head, 0, 2.0, -18, 8, 4.6, 12.4, roof);
		hbox(head, 0, 4.6, -17.0, 4.6, 1.0, 8.6, skin);
		hbox(head, 0, 4.2, -24.4, 8.6, 4.8, 4.2, nose(skin));
		hbox(head, 0, 6.9, -23.9, 6.2, 1.0, 3.4, skin);
		hbox(head, 0, 0.7, -24.6, 7.2, 2.2, 3.6, roof);
		for (int s = -1; s <= 1; s += 2) {
			hbox(head, s * 4.3, 4.6, -25.0, 1.4, 2.8, 2.6, Wood.nostril(s, skin));
			// The brows: heavy ridges over the eyes, flaring back into locks of hair.
			SpellArt.limb(head, h3(s * 2.0, 6.6, -14.0), h3(s * 5.4, 7.6, -8.4), 2.2 * H, 1.9 * H, skin);
			double[] brow = h3(s * 5.2, 7.9, -8.6);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, brow, (6.8 - k * 1.2) * H, 2.6 * H, -48 - k * 22, s * (18 + k * 12), s * (24 + k * 14), hair(40 + k * 2 + s)).density(3);
			}
			hbox(head, s * 5.35, 4.4, -11.2, 1.0, 2.2, 3.0, eye()).density(4);
			// Teeth, upper; the fangs.
			hbox(head, s * 3.75, -0.95, -18.4, 0.4, 1.4, 11.6, Wood.teeth(true)).density(4);
			SpellArt.limb(head, h3(s * 3.1, -0.3, -24.0), h3(s * 3.0, -3.2, -23.4), H, H, claw(-1)).density(4);
			SpellArt.limb(head, h3(s * 3.7, -0.3, -15.4), h3(s * 3.7, -2.3, -15.2), 0.7 * H, 0.7 * H, claw(-1)).density(4);
			// Its cheeks: locks of hair fanned back from the jowls.
			for (int k = 0; k < 4; k++) {
				SpellArt.blade(head, h3(s * 6.0, 1.6 - k * 1.3, -8.4 + k * 0.9), (8.4 - k * 1.1) * H, 3.4 * H, -72 - k * 14, s * (34 + k * 6), s * (30 + k * 12),
						hair(50 + k * 2 + s)).density(2.5);
			}
		}
		// The jaw, hinged under the ear, a little open; the storm crackling in its mouth.
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, -1.0 * H, -9.6 * H, 12, 0, 0);
		hbox(jaw, 0, -2.6, -16.6, 7.4, 2.4, 14, jawSkin(skin));
		hbox(jaw, 0, -3.8, -22.6, 6.2, 1.6, 3, skin);
		hbox(jaw, 0, -1.25, -16.4, 3.2, 0.5, 11, t -> SpellArt.glow(SpellArt.mix(STORM_DEEP, STORM, t.back() * 0.5 + SpellArt.hash(t.x, t.y, 3) * 0.3), 0.9));
		for (int s = -1; s <= 1; s += 2) {
			hbox(jaw, s * 3.35, -0.95, -17.0, 0.4, 1.1, 11.6, Wood.teeth(false)).density(4);
			SpellArt.limb(jaw, h3(s * 2.7, -1.6, -22.6), h3(s * 2.8, 0.7, -22.9), 0.85 * H, 0.85 * H, claw(1)).density(4);
		}
		// The beard: hair hanging from the chin and streaming back.
		for (int k = -2; k <= 2; k++) {
			SpellArt.blade(jaw, h3(k * 1.2, -4.4, -21.6 + Math.abs(k) * 1.6), (9.5 - Math.abs(k) * 1.4) * H, 3.4 * H, -140 + Math.abs(k) * 6, k * 9, k * 8, hair(60 + k))
					.density(2.5);
		}
		// Whiskers: long, from the nostrils, curving back past the head; gold, then the storm's light toward their ends.
		for (int s = -1; s <= 1; s += 2) {
			String side = s > 0 ? "l" : "r", up = "head";
			double z = -25.2 * H, x = s * 4.9 * H, y = 3.8 * H, step = 8 * H;
			double[][] rest = {{-20, s * 32, 0}, {-8, -s * 6, 0}, {12, -s * 16, 0}, {22, -s * 16, 0}};
			for (int k = 0; k < rest.length; k++) {
				SpellArt.Bone wb = m.bone("whisker_" + side + "_" + k, up, x, y, z, rest[k][0], rest[k][1], rest[k][2]);
				double thin = (0.8 - k * 0.12) * H;
				int kk = k;
				wb.box(x, y, z + step / 2, thin, thin, step + 0.3, t -> {
					double along = (kk + t.back()) / 4; // 0 at its root, 1 at its end
					if (along < 0.5) return SpellArt.col(SpellArt.mix(GOLD, GOLD_LIGHT, along * 2));
					return SpellArt.glow(SpellArt.mix(GOLD_LIGHT, STORM, (along - 0.5) * 2), 0.97 - (along - 0.5) * 0.6);
				}).density(3);
				up = wb.name;
				z += step;
			}
		}
		// Antlers: ivory, a stag's, branching high; their tines the storm's light at their points.
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone a = m.bone(s > 0 ? "antler_l" : "antler_r", "head", s * 2.8 * H, 7.0 * H, -5.4 * H);
			double[] p0 = h3(s * 2.8, 6.6, -5.4), p1 = branch(p0, s * 1.0, 4.4, 1.6), p2 = branch(p1, s * 1.4, 4.0, 3.0), p3 = branch(p2, s * 1.2, 3.2, 4.0),
					p4 = branch(p3, s * 0.4, 1.6, 3.6);
			SpellArt.Painter bone = ivory(70 + s), tine = claw(1);
			a.box(p0[0], p0[1] + 0.4 * H, p0[2], 2.6 * H, H, 2.6 * H, bone); // the burr
			SpellArt.limb(a, p0, p1, 1.9 * H, 1.9 * H, bone);
			SpellArt.limb(a, p1, p2, 1.6 * H, 1.6 * H, bone);
			SpellArt.limb(a, p2, p3, 1.25 * H, 1.25 * H, bone);
			SpellArt.limb(a, p3, p4, 0.9 * H, 0.9 * H, tine);
			double[] t1 = branch(p1, s * 0.4, 3.8, -2.2), t2 = branch(p2, s * 2.4, 3.0, -0.8), t3 = branch(p2, -s * 0.8, 3.2, 1.0), t4 = branch(p3, s * 1.6, 2.6, 0.2);
			SpellArt.limb(a, SpellArt.add(p1, h3(0, 0.3, 0)), t1, 1.05 * H, 1.05 * H, tine);
			SpellArt.limb(a, SpellArt.add(p2, h3(0, 0.2, 0)), t2, 0.95 * H, 0.95 * H, tine);
			SpellArt.limb(a, SpellArt.add(p2, h3(0, 0.2, 0)), t3, 0.85 * H, 0.85 * H, tine);
			SpellArt.limb(a, SpellArt.add(p3, h3(0, 0.1, 0)), t4, 0.75 * H, 0.75 * H, tine);
		}
		// The mane: silver-white hair streaming back from the head over the neck.
		SpellArt.Bone mane = m.bone("mane", "head", 0, 6.8 * H, -4 * H);
		for (int k = 0; k < 6; k++) {
			SpellArt.blade(mane, h3(0, 7.0, -9 + k * 2.0), (12 - k * 0.4) * H, 4.6 * H, -60 - k * 4, 0, 0, hair(100 + k));
		}
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone side = m.bone(s > 0 ? "mane_l" : "mane_r", "head", s * 4 * H, 4.5 * H, -3 * H);
			for (int k = 0; k < 5; k++) {
				SpellArt.blade(side, h3(s * (3.6 + k * 0.5), 6.2 - k * 1.6, -6.6 + k * 1.4), (14.5 - k * 1.3) * H, 5 * H, -70 - k * 7, s * (16 + k * 6), s * (26 + k * 11),
						hair(110 + k * 3 + s));
			}
		}
	}

	/** An antler's next point from {@code from}: the Azure Dragon's boughs' steps, grown by {@link #H} and a fifth again. */
	static double[] branch(double[] from, double x, double y, double z) { return SpellArt.add(from, SpellArt.times(h3(x, y, z), 1.2)); }

	static void legs(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			boolean front = k < 2;
			int segI = front ? 3 : 15;
			double z0 = segI * SEG + 3.5, w = thick(segI), big = front ? 1.35 : 1.45;
			SpellArt.Painter skin = hide(200 + k), scaled = hideLimb(200 + k);
			double[] shoulder = SpellArt.v3(s * w * 0.4, -w * 0.22, z0), elbow = SpellArt.add(shoulder, SpellArt.v3(s * 2.6 * big, -5.2 * big, front ? 1.8 : 2.6)),
					wrist = SpellArt.add(elbow, SpellArt.v3(s * 0.4, -4.4 * big, front ? -2.6 : -1.6));
			// The forelegs reach forward, the hind legs trail back, as a dragon holds them in flight.
			SpellArt.Bone leg = m.bone(names[k], "seg_" + segI, shoulder[0], shoulder[1], shoulder[2], front ? -36 : 48, 0, -s * 10);
			leg.box(shoulder[0] * 1.05, shoulder[1] + 0.4, shoulder[2], 3.6 * big, 4.2 * big, 4.6 * big, skin);
			SpellArt.limb(leg, shoulder, elbow, 2.9 * big, 3.1 * big, scaled);
			// Elbow tufts: hair streaming back.
			for (int j = 0; j < 3; j++) {
				SpellArt.blade(leg, elbow, (5.2 - j * 0.9) * big, 2.6 * big, -64 - j * 18, s * (12 + j * 10), s * (20 + j * 12), hair(300 + k * 5 + j)).density(2.5);
			}
			SpellArt.Bone low = m.bone(names[k] + "_low", names[k], elbow[0], elbow[1], elbow[2], front ? 30 : 24, 0, 0);
			SpellArt.limb(low, elbow, wrist, 2.3 * big, 2.4 * big, scaled);
			// The foot: a palm and five claws, four spread forward and one behind (the five claws of the dragon of dragons).
			double[] palm = SpellArt.add(wrist, SpellArt.v3(0, -0.4, -0.6));
			low.box(palm[0], palm[1], palm[2], 3.4 * big, 1.3 * big, 3.2 * big, scaled);
			for (int j = 0; j < 4; j++) {
				double spread = (j - 1.5) * 0.8;
				double[] knuckle = SpellArt.add(palm, SpellArt.v3(s * spread * big, -0.2, -1.4 * big)), tip = SpellArt.add(knuckle, SpellArt.v3(s * spread * 0.5, -0.9 * big, -2.0 * big)),
						point = SpellArt.add(tip, SpellArt.v3(s * spread * 0.15, -1.6 * big, 0.2));
				SpellArt.limb(low, knuckle, tip, 0.75 * big, 0.75 * big, scaled).density(3);
				SpellArt.limb(low, tip, point, 0.55 * big, 0.55 * big, claw(1)).density(4);
			}
			double[] heel = SpellArt.add(palm, SpellArt.v3(0, -0.2, 1.4 * big)), spur = SpellArt.add(heel, SpellArt.v3(0, -1.6 * big, 1.0 * big));
			SpellArt.limb(low, heel, spur, 0.55 * big, 0.55 * big, claw(1)).density(4);
		}
	}

	/** The swim loop: waves running down the body from the head, legs paddling, the mane and whiskers streaming, the plume swaying. */
	static void swim(SpellArt.Model m) {
		double T = 4.0, w = 2 * Math.PI / T;
		SpellArt.Anim a = m.anim("swim", T, "true");
		int steps = 16;
		for (int i = 0; i < SEGMENTS; i++) {
			int ii = i;
			double env = i == 0 ? 0.3 : i < 3 ? 0.7 : 1 + i * 0.01;
			a.sample("seg_" + i, "rotation", 0, T, steps, t -> {
				double ph = w * t - ii * WAVE;
				return new double[] {6.0 * env * Math.sin(ph), 3.6 * env * Math.sin(ph + 1.3), 0};
			});
		}
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {2.5 * Math.sin(w * t + 0.6), 2 * Math.sin(w * t + 1.9), 0});
		a.sample("jaw", "rotation", 0, T, 8, t -> new double[] {3 + 2.5 * Math.sin(w * t), 0, 0});
		for (String side : new String[] {"l", "r"}) {
			int s = side.equals("l") ? 1 : -1;
			for (int k = 0; k < 4; k++) {
				int kk = k;
				a.sample("whisker_" + side + "_" + k, "rotation", 0, T, steps, t -> {
					double ph = w * t - kk * 0.9;
					return new double[] {(5 + kk * 2) * Math.sin(ph), s * (4 + kk * 2) * Math.sin(ph + 0.7), 0};
				});
			}
			a.sample("mane_" + side, "rotation", 0, T, steps, t -> new double[] {-5 * Math.sin(w * t - 0.8), 0, s * 4 * Math.sin(w * t - 0.4)});
		}
		a.sample("mane", "rotation", 0, T, steps, t -> new double[] {-6 * Math.sin(w * t - 0.6), 0, 0});
		String[] legs = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[] phase = {0, Math.PI, Math.PI, 0};
		for (int k = 0; k < 4; k++) {
			double p = phase[k];
			a.sample(legs[k], "rotation", 0, T, steps, t -> new double[] {22 * Math.sin(w * t + p), 0, 0});
			a.sample(legs[k] + "_low", "rotation", 0, T, steps, t -> new double[] {-18 * Math.sin(w * t + p - 1.0), 0, 0});
		}
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {8 * Math.sin(w * t - SEGMENTS * WAVE - 0.5), 12 * Math.sin(w * t - SEGMENTS * WAVE + 0.8), 0});
	}

	/** The roar (as it calls the lightning down): the head rears and the jaw gapes, the neck arching under it, whiskers and mane flung back; then it settles. */
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
}
