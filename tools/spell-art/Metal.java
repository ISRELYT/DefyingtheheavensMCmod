/**
 * The metal element's Dao Integration spells for SpellArt: the White Tiger of the west, white-furred, its stripes of dark steel edged with
 * sword-light, the character for king on its brow (White Tiger Pounce); and Myriad Swords Return: the rings of flying swords and the
 * colossal sword they merge into.
 */
public class Metal {
	// --- The metal element's colours: white fur and its shade, the stripes' dark steel, steel and silver, the sword-light; gold ---
	static final int FUR = 0xF2F4F8, FUR_SHADE = 0xC6CCD6, STRIPE = 0x23272F, STRIPE_LIGHT = 0x454D5A, STEEL_DARK = 0x3A424E, STEEL = 0x7E8998, SILVER = 0xC2CCD8;
	static final int GLEAM = 0xE6F2FF, WHITE = 0xFFFFFF, GOLD = 0xE8C060, GOLD_DARK = 0x9A7430, NOSE = 0xA4787E, PAD = 0x3A3438, ICE_EYE = 0xBFEFFF;

	// --- The White Tiger: painters ---

	/**
	 * The tiger's coat round a body whose axis runs along z at x = 0, y = {@code axisY}: white, a sheen of silver along the back; stripes of
	 * dark steel round it, wavering, forking, thinning down the flanks and gone under the belly, each edged on one side with a line of
	 * sword-light; on a face looking forward or back, the stripes run down it.
	 */
	static SpellArt.Painter fur(double axisY, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.flat() && t.ny < -0.6) a = 1;
			boolean across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			double along = across ? -t.qy() : qz;
			double hair = SpellArt.noise(th * 30 + seed, along * 0.7, seed);
			int c = SpellArt.mix(SpellArt.mix(FUR_SHADE, FUR, 0.6 + 0.3 * hair), SILVER, 0.18 * (1 - a));
			if (a > 0.82) c = SpellArt.mix(c, FUR, 0.6);
			double s = along / 4.6 + 0.42 * Math.sin(th * 1.7 + along * 0.13 + seed) + 0.3 * (SpellArt.noise(along * 0.25, th * 2, seed + 1) - 0.5);
			double width = 0.32 * (1 - SpellArt.smooth(0.42, 0.8, a)) * (0.6 + 0.4 * SpellArt.noise(along * 0.3, th * 3, seed + 2));
			double f = s - Math.floor(s);
			if (f < width) {
				// A fork: some stripes split down their middle.
				if (Math.abs(f - width / 2) < width * 0.16 && SpellArt.noise(Math.floor(s) * 3.1, th * 4, seed + 3) > 0.62) return SpellArt.col(c);
				return SpellArt.col(SpellArt.mix(STRIPE, STRIPE_LIGHT, hair * 0.5));
			}
			if (f < width + 0.035 && width > 0.08) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, hair), 0.85);
			return SpellArt.col(c);
		};
	}

	/** A limb's fur: white, rings of dark steel stripes round it (thinning toward the paw), paler on its inner side and beneath. */
	static SpellArt.Painter limbFur(int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2);
			double hair = SpellArt.noise(ang * 8 + seed, t.ly * 0.8, seed);
			int c = SpellArt.mix(FUR_SHADE, FUR, 0.6 + 0.35 * hair);
			double s = t.ly / 3.4 + 0.25 * Math.sin(ang * 2 + seed), f = s - Math.floor(s), width = 0.26 * (1 - t.up() * 0.6);
			if (t.ny < -0.5 || Math.abs(t.lx - t.sx() / 2) < 0.6 && t.nx != 0) return SpellArt.col(SpellArt.mix(c, FUR, 0.5));
			if (f < width && SpellArt.noise(Math.floor(s), ang * 2, seed + 1) > 0.25) return SpellArt.col(STRIPE);
			return SpellArt.col(c);
		};
	}

	/**
	 * The head's fur: white, stripes sweeping back over the cheeks and crown; if {@code brow}, on the front of its cube above the eyes the
	 * character for king (three bars and a stroke down them) in burning gold, a stripe either side of it.
	 */
	static SpellArt.Painter headFur(int seed, boolean brow) {
		return t -> {
			double hair = SpellArt.noise(t.px * 2 + seed, t.pz * 2 + t.py, seed);
			int c = SpellArt.mix(SpellArt.mix(FUR_SHADE, FUR, 0.65 + 0.3 * hair), SILVER, t.ny > 0.5 ? 0.15 : 0);
			if (brow && t.f == SpellArt.Face.FRONT) {
				double x = t.across() - 0.5, y = t.up();
				boolean bar = Math.abs(x) < 0.17 && (Math.abs(y - 0.7) < 0.03 || Math.abs(y - 0.81) < 0.028 && Math.abs(x) < 0.13 || Math.abs(y - 0.92) < 0.03);
				if (bar || Math.abs(x) < 0.03 && y > 0.7 && y < 0.92) return SpellArt.glow(SpellArt.mix(GOLD, 0xFFF0C0, 0.4), 0.97);
				if (Math.abs(Math.abs(x) - 0.3) < 0.035 && y > 0.66 && y < 0.97) return SpellArt.col(STRIPE);
				return SpellArt.col(c);
			}
			// Stripes on the sides and crown, sweeping back.
			double s = (t.pz + Math.abs(t.px) * 0.4 + t.py * 0.5) / 3.2 + 0.3 * Math.sin(t.py * 0.6 + seed), f = s - Math.floor(s);
			if ((t.side() || t.ny > 0.5) && f < 0.2 && SpellArt.noise(Math.floor(s) * 2, t.py * 0.3, seed + 1) > 0.3) return SpellArt.col(STRIPE);
			return SpellArt.col(c);
		};
	}

	/** An ear: its back (looking back) dark with a white spot, its front white fur, its rim dark. */
	static SpellArt.Painter ear() {
		return t -> {
			double x = t.across() - 0.5, y = t.up() - 0.5;
			if (t.f == SpellArt.Face.BACK) return Math.hypot(x, y) < 0.2 ? SpellArt.col(FUR) : SpellArt.col(STRIPE);
			if (Math.abs(x) > 0.38 || y > 0.38) return SpellArt.col(STRIPE_LIGHT);
			return SpellArt.col(SpellArt.mix(FUR_SHADE, FUR, 0.5 - y));
		};
	}

	/** The nose's leather, rose-grey, its nostrils dark, on its front; fur elsewhere. */
	static SpellArt.Painter nose(SpellArt.Painter fur) {
		return t -> {
			if (t.f != SpellArt.Face.FRONT && !t.flat()) return fur.paint(t);
			double x = t.across() - 0.5, y = t.up();
			if (t.f == SpellArt.Face.FRONT && Math.abs(Math.abs(x) - 0.22) < 0.12 && y < 0.45) return SpellArt.col(0x2A1A1E);
			return SpellArt.col(SpellArt.mix(NOSE, 0xC89CA4, y * 0.5));
		};
	}

	/** An eye on the front of its cube: almond-shaped, ice-blue light ringed gold, a round dark pupil, a glint; outlined dark. */
	static SpellArt.Painter eye() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(STRIPE);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			double shape = Math.abs(y) - (1 - x * x) * 0.85;
			if (shape > 0) return SpellArt.col(STRIPE);
			double r = Math.hypot(x * 0.7, y);
			if (r < 0.28) return SpellArt.col(0x05080C);
			if (Math.hypot(x * 0.7 + 0.18, y - 0.3) < 0.14) return SpellArt.glow(WHITE);
			return SpellArt.glow(SpellArt.mix(ICE_EYE, GOLD, r * 0.9));
		};
	}

	/** A claw or a fang, a blade of silver steel: darker at its root, its point (the top of its cube) blazing with sword-light. */
	static SpellArt.Painter blade() {
		return t -> {
			double toward = t.up();
			if (toward > 0.6) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, (toward - 0.6) * 2.5));
			return SpellArt.col(SpellArt.mix(STEEL_DARK, SILVER, 0.3 + toward));
		};
	}

	/** A paw: white fur over, dark pads beneath. */
	static SpellArt.Painter paw(SpellArt.Painter fur) {
		return t -> t.ny < -0.5 ? SpellArt.col(SpellArt.hash(t.x / 2, t.y / 2, 7) > 0.3 ? PAD : SpellArt.shade(PAD, -0.3)) : fur.paint(t);
	}

	/** A tuft of fur on a blade: white, in strands, a dark streak or two. */
	static SpellArt.Painter tuft(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.46 * Math.pow(1 - v, 0.6);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			double strand = Math.abs(Math.sin(u / Math.max(0.04, half) * 5 + v * 2 + seed));
			if (SpellArt.noise(u * 9 + seed, v * 3, seed) > 0.75) return SpellArt.col(STRIPE_LIGHT);
			return SpellArt.col(SpellArt.mix(FUR_SHADE, FUR, 0.4 + 0.6 * strand));
		};
	}

	// --- The White Tiger, made of metal ---

	/**
	 * The tiger's stripe at {@code along} (pixels down it) and {@code th} (round it), its width shrinking with {@code a} (0 on the back to 1
	 * beneath): 0 off it, 1 on it, 2 on the line of sword-light along its edge.
	 */
	static int stripe(double along, double th, double a, int seed) {
		double s = along / 4.6 + 0.42 * Math.sin(th * 1.7 + along * 0.13 + seed) + 0.3 * (SpellArt.noise(along * 0.25, th * 2, seed + 1) - 0.5);
		double width = 0.18 * (1 - SpellArt.smooth(0.42, 0.8, a)) * (0.6 + 0.4 * SpellArt.noise(along * 0.3, th * 3, seed + 2)), f = s - Math.floor(s);
		if (f < width) return 1;
		return f < width + 0.045 && width > 0.05 ? 2 : 0;
	}

	/**
	 * Polished steel's light on a face (as chrome takes the sky and the ground): bright where it looks up, and on a face looking out, bright
	 * above a dark band (the horizon) a little below its middle, dim beneath it; dark where it looks down. {@code by} lightens (+) or darkens.
	 */
	static int chrome(SpellArt.Texel t, int base, double brush, double by) {
		double shine;
		if (t.ny > 0.45) shine = 0.42;
		else if (t.ny < -0.45) shine = -0.45;
		else {
			double h = t.up();
			shine = h > 0.56 ? 0.28 + 0.2 * (h - 0.56) : h > 0.38 ? -0.42 + 0.25 * (h - 0.38) / 0.18 : -0.08 + 0.1 * h;
		}
		return SpellArt.shade(base, SpellArt.clamp(shine + by + (brush - 0.5) * 0.12, -0.8, 0.8));
	}

	/**
	 * Polished steel in plates round a body whose axis runs along z (x = 0, y = {@code axisY}): silver, brushed along it, lit as chrome
	 * ({@link #chrome}), a seam round it every so far (riveted) and one down its spine; the tiger's stripes engraved in it, dark grooves each
	 * edged on one side with a line of sword-light; on a face looking forward or back, the stripes run down it.
	 */
	static SpellArt.Painter steelHide(double axisY, int seed) {
		return t -> {
			double qx = t.qx(), qy = t.qy() - axisY, qz = t.qz();
			double th = Math.atan2(qx, qy), a = Math.abs(th) / Math.PI;
			if (t.flat() && t.ny < -0.6) a = 1;
			boolean across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			double along = across ? -t.qy() : qz;
			double brush = SpellArt.noise(th * 40 + seed, along * 0.12, seed) * 0.7 + SpellArt.noise(th * 90 + seed, along * 0.05, seed + 3) * 0.3;
			double ring = along / 6.5 + 0.1 * Math.sin(th * 2 + seed), rf = ring - Math.rint(ring);
			if (Math.abs(rf) < 0.03 || !across && Math.abs(th) < 0.03) return SpellArt.col(SpellArt.shade(STEEL_DARK, -0.35));
			// Rivets along each seam.
			double rivet = th / (2 * Math.PI) * 18;
			if (Math.abs(rf) < 0.09 && Math.abs(rivet - Math.rint(rivet)) < 0.12 && !across) return SpellArt.col(rf > 0 ? WHITE : STEEL_DARK);
			int band = stripe(along, th, a, seed);
			if (band == 1) return SpellArt.col(SpellArt.shade(STRIPE, -0.2));
			if (band == 2) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, brush), 0.92);
			return SpellArt.col(chrome(t, SpellArt.mix(STEEL, SILVER, 0.55 + 0.35 * brush), brush, a > 0.8 ? -0.25 : 0));
		};
	}

	/** Polished steel on a limb: brushed along it, lit as chrome, a seam round it every so far, grooves engraved round it, darker beneath. */
	static SpellArt.Painter steelLimb(int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2);
			double brush = SpellArt.noise(ang * 14 + seed, t.ly * 0.2, seed);
			if (Math.abs(t.ly / 4.2 - Math.rint(t.ly / 4.2)) < 0.04) return SpellArt.col(SpellArt.shade(STEEL_DARK, -0.35));
			double s = t.ly / 3.4 + 0.25 * Math.sin(ang * 2 + seed), f = s - Math.floor(s), width = 0.16 * (1 - t.up() * 0.6);
			if (f < width && SpellArt.noise(Math.floor(s), ang * 2, seed + 1) > 0.25) return SpellArt.col(SpellArt.shade(STRIPE, -0.2));
			if (f < width + 0.04 && f >= width && SpellArt.noise(Math.floor(s), ang * 2, seed + 1) > 0.25) return SpellArt.glow(GLEAM, 0.88);
			return SpellArt.col(chrome(t, SpellArt.mix(STEEL, SILVER, 0.55 + 0.35 * brush), brush, 0));
		};
	}

	/**
	 * The head's steel: polished white plates, seams across them, stripes engraved back over the cheeks and crown; if {@code brow}, on the
	 * front of its cube above the eyes the character for king inlaid in gold, a stripe either side of it.
	 */
	static SpellArt.Painter steelHead(int seed, boolean brow) {
		return t -> {
			double brush = SpellArt.noise(t.px * 1.4 + seed, t.pz * 1.4 + t.py, seed);
			int c = chrome(t, SpellArt.mix(STEEL, SILVER, 0.6 + 0.3 * brush), brush, 0.05);
			if (brow && t.f == SpellArt.Face.FRONT) {
				double x = t.across() - 0.5, y = t.up();
				boolean bar = Math.abs(x) < 0.17 && (Math.abs(y - 0.7) < 0.03 || Math.abs(y - 0.81) < 0.028 && Math.abs(x) < 0.13 || Math.abs(y - 0.92) < 0.03);
				if (bar || Math.abs(x) < 0.03 && y > 0.7 && y < 0.92) return SpellArt.glow(SpellArt.mix(GOLD, 0xFFF0C0, 0.4), 0.97);
				if (Math.abs(Math.abs(x) - 0.3) < 0.035 && y > 0.66 && y < 0.97) return SpellArt.col(STRIPE);
				return SpellArt.col(c);
			}
			if (Math.abs(t.pz / 3.6 - Math.rint(t.pz / 3.6)) < 0.04 && t.side()) return SpellArt.col(STEEL_DARK);
			double s = (t.pz + Math.abs(t.px) * 0.4 + t.py * 0.5) / 3.2 + 0.3 * Math.sin(t.py * 0.6 + seed), f = s - Math.floor(s);
			if ((t.side() || t.ny > 0.5) && f < 0.2 && SpellArt.noise(Math.floor(s) * 2, t.py * 0.3, seed + 1) > 0.3) return SpellArt.col(STRIPE);
			if ((t.side() || t.ny > 0.5) && f < 0.24 && f >= 0.2) return SpellArt.glow(GLEAM, 0.85);
			return SpellArt.col(c);
		};
	}

	/** An eye on the front of its cube: a slit of sword-light, ice-white, keen at its corners; dark steel round it. */
	static SpellArt.Painter steelEye() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(STEEL_DARK);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = Math.abs(y) - (1 - x * x) * 0.55;
			if (shape > 0) return SpellArt.col(STRIPE);
			return SpellArt.glow(SpellArt.mix(WHITE, ICE_EYE, Math.abs(x) * 0.8));
		};
	}

	/**
	 * A blade on a blade (a lame of the ruff, an ear, the fringe beneath it, its length along it): straight-edged, drawn to a point, silver
	 * steel with a darker ridge down its middle, its edges and point blazing with sword-light.
	 */
	static SpellArt.Painter lame(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.46 * Math.min(1, (1 - v) * 1.5) * (v < 0.06 ? 0.7 : 1);
			if (Math.abs(u) > half || v > 0.99) return SpellArt.CLEAR;
			double edge = half - Math.abs(u);
			if (edge < 0.06 || v > 0.86) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, v), 0.92);
			if (Math.abs(u) < 0.05) return SpellArt.col(SpellArt.mix(STEEL_DARK, STEEL, v));
			return SpellArt.col(SpellArt.mix(STEEL, SILVER, 0.4 + 0.5 * SpellArt.noise(u * 6 + seed, v * 3, seed)));
		};
	}

	/** A paw of steel: plated as the limb ({@code skin}), dark steel beneath. */
	static SpellArt.Painter steelPaw(SpellArt.Painter skin) {
		return t -> t.ny < -0.5 ? SpellArt.col(SpellArt.shade(STEEL_DARK, -0.25)) : skin.paint(t);
	}

	/**
	 * The White Tiger, beast of the west, made of metal (the user, 2026-10-24: the great spells' beasts made of their elements, not looking
	 * like animals): its body ("body", pivoting at the hips) of white steel in plates, its stripes inlaid dark steel edged with sword-light,
	 * a fringe of blades beneath; its neck ("neck") and head ("head", the jaw "jaw") polished steel, the character for king inlaid in gold on
	 * its brow, slits of sword-light for eyes, ears and a ruff of blades, its fangs blades; its legs ("leg_fl".."leg_br", each with its
	 * "_low" and "_paw") plated, clawed with blades; its tail ("tail_0".."tail_5") of plates ending in a sword's point. Its bones the beast's
	 * ({@link #whiteTigerBeast}), so it prowls, pounces and claws as that did. It faces -z, its paws on the ground at y 0.
	 */
	static void whiteTiger(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 21, 14);
		SpellArt.Painter plates = steelHide(21, 3);
		body.box(0, 21, -12, 14, 14, 16, plates);
		body.box(0, 21.6, -12, 10, 16.4, 15, plates);
		body.box(0, 20.2, 2, 12, 12, 14, plates);
		body.box(0, 20.8, 2, 9, 13.8, 13, plates);
		body.box(0, 21.2, 14, 13, 13, 12, plates);
		body.box(0, 21.8, 14, 9.6, 14.8, 11, plates);
		body.box(0, 20.5, -20.8, 9, 11, 2, plates);
		body.box(0, 22, 21, 9, 10, 2, plates);
		for (int s = -1; s <= 1; s += 2) body.box(s * 4.2, 28.4, -14.5, 4.6, 2.6, 9, plates);
		// A fringe of blades hanging under the belly and the breast.
		for (int k = 0; k < 6; k++) {
			for (int s = -1; s <= 1; s += 2) {
				SpellArt.blade(body, SpellArt.v3(s * 3.2, 14.4 + (k == 0 ? 1.4 : 0), -16 + k * 5.4), 4.6, 3.2, -160, s * 8, s * 14, lame(20 + k % 3)).density(2.5);
			}
		}
		// Blades raised along its spine, from the shoulders to the hips, swept back.
		for (int k = 0; k < 7; k++) {
			SpellArt.blade(body, SpellArt.v3(0, k < 2 ? 29.4 : 28 - Math.abs(k - 4) * 0.2, -16 + k * 5.4), 4.4 - Math.abs(k - 3) * 0.3, 2.8, -58, 0, 0, lame(30 + k % 3)).density(3);
		}
		neckAndHeadOfMetal(m);
		legsOfMetal(m);
		tailOfMetal(m);
		prowl(m);
		pounce(m);
		claw(m);
	}

	static void neckAndHeadOfMetal(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 25, -19);
		neck.box(0, 25.6, -22, 10.6, 11.4, 7, steelHide(25, 5));
		SpellArt.Bone head = m.bone("head", "neck", 0, 28, -24);
		SpellArt.Painter face = steelHead(9, false);
		head.box(0, 30.5, -28.4, 12.4, 11, 9.6, steelHead(9, true)).density(4);
		head.box(0, 30.6, -28.4, 10, 11.4, 9, face);
		head.box(0, 27.2, -35.2, 8.4, 5.6, 5.4, face);
		head.box(0, 31.2, -34.8, 4, 1.6, 6, face);
		head.box(0, 29.3, -37.9, 3.6, 2.2, 1.2, face).density(4);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 5.8, 27.8, -27.6, 3.4, 7, 8, face);
			head.box(s * 2.2, 26.4, -36.2, 3.6, 3, 3.8, face);
			head.box(s * 3.1, 31.6, -33.3, 2.8, 1.7, 0.6, steelEye()).density(5);
			SpellArt.limb(head, SpellArt.v3(s * 1.4, 33.6, -33.4), SpellArt.v3(s * 4.8, 33.8, -32.6), 1.6, 1.2, face);
			// An ear: a blade, raised.
			SpellArt.blade(head, SpellArt.v3(s * 4.6, 34.4, -26.6), 5.6, 3.6, -8, s * 15, -s * 18, lame(50 + (s > 0 ? 0 : 1))).density(3);
			// The ruff: blades flaring back from the cheeks.
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 6.8, 29.6 - k * 2.2, -27 + k * 0.6), 8 - k, 3.8, -84 - k * 10, s * (26 + k * 10), s * (30 + k * 10), lame(40 + k)).density(2.5);
			}
			// Whiskers of light.
			for (int k = 0; k < 3; k++) {
				double[] from = SpellArt.v3(s * 3.8, 26.8 - k * 0.7, -36.6), to = SpellArt.v3(s * 11.5, 26.4 + k * 0.9 - 0.9, -33.4 + k * 0.8);
				SpellArt.limb(head, from, to, 0.3, 0.3, t -> SpellArt.glow(SpellArt.mix(WHITE, GLEAM, t.up()), 0.95 - t.up() * 0.4)).density(6);
			}
			SpellArt.limb(head, SpellArt.v3(s * 2.8, 25.9, -37.4), SpellArt.v3(s * 2.8, 23.2, -37.2), 1, 1, blade()).density(5);
		}
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 25.6, -31, 4, 0, 0);
		jaw.box(0, 24.6, -34.4, 7.4, 2.6, 7.4, face);
		for (int s = -1; s <= 1; s += 2) SpellArt.limb(jaw, SpellArt.v3(s * 2.4, 25.4, -37.4), SpellArt.v3(s * 2.4, 27.2, -37.6), 0.8, 0.8, blade()).density(5);
	}

	static void legsOfMetal(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			String n = names[k];
			SpellArt.Painter plated = steelLimb(60 + k), big = steelHide(21, 3);
			double[] top, knee, low, pawAt;
			SpellArt.Bone leg, lower, foot;
			if (k < 2) {
				top = SpellArt.v3(s * 5, 20, -14);
				knee = SpellArt.v3(s * 5.4, 11, -12.6);
				low = SpellArt.v3(s * 5.2, 3.6, -13.8);
				leg = m.bone(n, "body", top[0], top[1], top[2]);
				leg.box(s * 5, 17.6, -13.6, 6, 8, 8, big);
				SpellArt.limb(leg, top, knee, 5.6, 6.6, plated);
				lower = m.bone(n + "_low", n, knee[0], knee[1], knee[2]);
				SpellArt.limb(lower, knee, low, 4.2, 4.6, plated);
				foot = m.bone(n + "_paw", n + "_low", low[0], low[1], low[2]);
				pawAt = SpellArt.v3(s * 5.2, 1.7, -15.2);
			} else {
				top = SpellArt.v3(s * 5.2, 21, 15);
				knee = SpellArt.v3(s * 5.8, 12, 10.4);
				low = SpellArt.v3(s * 5.6, 6.4, 18.4);
				leg = m.bone(n, "body", top[0], top[1], top[2]);
				leg.box(s * 5.2, 17.4, 13.6, 6.6, 10, 10, big);
				SpellArt.limb(leg, top, knee, 6.4, 8.4, plated);
				lower = m.bone(n + "_low", n, knee[0], knee[1], knee[2]);
				SpellArt.limb(lower, knee, low, 4.6, 5.4, plated);
				foot = m.bone(n + "_paw", n + "_low", low[0], low[1], low[2]);
				SpellArt.limb(foot, low, SpellArt.v3(s * 5.4, 2.8, 16.6), 3.6, 4, plated);
				pawAt = SpellArt.v3(s * 5.4, 1.7, 15.4);
			}
			// The paw: broad, plated; its toes, its claws (blades of steel, half drawn).
			foot.box(pawAt[0], pawAt[1], pawAt[2], 5.8, 3.4, 6.4, steelPaw(plated));
			for (int j = 0; j < 4; j++) {
				double x = pawAt[0] + (j - 1.5) * 1.45;
				foot.box(x, 1.2, pawAt[2] - 3.4, 1.4, 2.4, 1.8, steelPaw(plated)).density(3);
				SpellArt.limb(foot, SpellArt.v3(x, 1.8, pawAt[2] - 4.2), SpellArt.v3(x, 0.4, pawAt[2] - 6.2), 0.5, 0.8, blade()).density(5);
			}
		}
	}

	static void tailOfMetal(SpellArt.Model m) {
		String up = "body";
		double[] at = SpellArt.v3(0, 26, 21);
		double[][] dirs = {{0, 1.2, 6}, {0, 0.4, 6}, {0, -1.6, 5.6}, {0, -3, 5}, {0, -3.6, 4.6}, {0, -3.4, 4}};
		for (int k = 0; k < dirs.length; k++) {
			SpellArt.Bone b = m.bone("tail_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, dirs[k]);
			double thick = 3.4 - k * 0.24;
			int kk = k;
			if (kk == dirs.length - 1) {
				// Its end: a sword's point.
				SpellArt.limb(b, at, next, thick, thick, steelLimb(70 + k));
				SpellArt.limb(b, next, SpellArt.add(next, SpellArt.times(dirs[k], 1.1)), thick * 0.7, thick * 0.4, blade()).density(4);
			} else {
				SpellArt.limb(b, at, SpellArt.add(next, SpellArt.times(SpellArt.sub(next, at), 0.08)), thick, thick, t -> {
					double f = (t.ly + kk * 6) / 3.3 - Math.floor((t.ly + kk * 6) / 3.3);
					if (f < 0.06) return SpellArt.col(STEEL_DARK);
					return SpellArt.col(f < 0.32 ? STRIPE : SpellArt.mix(SpellArt.mix(STEEL, SILVER, 0.7), FUR, 0.4));
				});
			}
			up = b.name;
			at = next;
		}
	}

	/**
	 * The White Tiger, beast of the west: its body ("body", pivoting at the hips) white, striped in dark steel edged with sword-light; its
	 * neck ("neck") and head ("head", the jaw "jaw"), the character for king on its brow in gold, its eyes ice-blue, its fangs blades; its
	 * legs ("leg_fl".."leg_br", each with its "_low" and "_paw"), its claws blades of silver steel; its tail ("tail_0".."tail_5"), black at
	 * its tip. It faces -z, its paws on the ground at y 0. Stored (2026-10-24): the spell's tiger is now made of metal, {@link #whiteTiger};
	 * this, the true beast, is kept for later (SpellArt --stored).
	 */
	static void whiteTigerBeast(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 21, 14);
		SpellArt.Painter coat = fur(21, 3);
		body.box(0, 21, -12, 14, 14, 16, coat);
		body.box(0, 21.6, -12, 10, 16.4, 15, coat);
		body.box(0, 20.2, 2, 12, 12, 14, coat);
		body.box(0, 20.8, 2, 9, 13.8, 13, coat);
		body.box(0, 21.2, 14, 13, 13, 12, coat);
		body.box(0, 21.8, 14, 9.6, 14.8, 11, coat);
		body.box(0, 20.5, -20.8, 9, 11, 2, coat);
		body.box(0, 22, 21, 9, 10, 2, coat);
		for (int s = -1; s <= 1; s += 2) body.box(s * 4.2, 28.4, -14.5, 4.6, 2.6, 9, coat);
		// Fur hanging under the belly and the breast.
		for (int k = 0; k < 6; k++) {
			for (int s = -1; s <= 1; s += 2) {
				SpellArt.blade(body, SpellArt.v3(s * 3.2, 14.4 + (k == 0 ? 1.4 : 0), -16 + k * 5.4), 4.6, 4.2, -160, s * 8, s * 14, tuft(20 + k % 3)).density(2.5);
			}
		}
		neckAndHead(m);
		legs(m);
		tail(m);
		prowl(m);
		pounce(m);
		claw(m);
	}

	static void neckAndHead(SpellArt.Model m) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 25, -19);
		neck.box(0, 25.6, -22, 10.6, 11.4, 7, fur(25, 5));
		SpellArt.Bone head = m.bone("head", "neck", 0, 28, -24);
		SpellArt.Painter face = headFur(9, false);
		head.box(0, 30.5, -28.4, 12.4, 11, 9.6, headFur(9, true)).density(4);
		head.box(0, 30.6, -28.4, 10, 11.4, 9, face);
		head.box(0, 27.2, -35.2, 8.4, 5.6, 5.4, face);
		head.box(0, 31.2, -34.8, 4, 1.6, 6, face);
		head.box(0, 29.3, -37.9, 3.6, 2.2, 1.2, nose(face)).density(4);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 5.8, 27.8, -27.6, 3.4, 7, 8, face);
			head.box(s * 2.2, 26.4, -36.2, 3.6, 3, 3.8, face);
			head.box(s * 3.1, 31.6, -33.3, 2.8, 1.7, 0.6, eye()).density(5);
			SpellArt.limb(head, SpellArt.v3(s * 1.4, 33.6, -33.4), SpellArt.v3(s * 4.8, 33.8, -32.6), 1.6, 1.2, face);
			head.box(s * 4.6, 36.2, -26.6, 3.6, 3.8, 1.2, ear()).rot(0, s * 15, -s * 10).density(3);
			// The ruff: fur flaring back from the cheeks.
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 6.8, 29.6 - k * 2.2, -27 + k * 0.6), 8 - k, 4.4, -84 - k * 10, s * (26 + k * 10), s * (30 + k * 10), tuft(40 + k)).density(2.5);
			}
			// Whiskers of light.
			for (int k = 0; k < 3; k++) {
				double[] from = SpellArt.v3(s * 3.8, 26.8 - k * 0.7, -36.6), to = SpellArt.v3(s * 11.5, 26.4 + k * 0.9 - 0.9, -33.4 + k * 0.8);
				SpellArt.limb(head, from, to, 0.3, 0.3, t -> SpellArt.glow(SpellArt.mix(WHITE, GLEAM, t.up()), 0.95 - t.up() * 0.4)).density(6);
			}
			SpellArt.limb(head, SpellArt.v3(s * 2.8, 25.9, -37.4), SpellArt.v3(s * 2.8, 23.2, -37.2), 1, 1, blade()).density(5);
		}
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 25.6, -31, 4, 0, 0);
		jaw.box(0, 24.6, -34.4, 7.4, 2.6, 7.4, face);
		for (int s = -1; s <= 1; s += 2) SpellArt.limb(jaw, SpellArt.v3(s * 2.4, 25.4, -37.4), SpellArt.v3(s * 2.4, 27.2, -37.6), 0.8, 0.8, blade()).density(5);
	}

	static void legs(SpellArt.Model m) {
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		for (int k = 0; k < 4; k++) {
			int s = k % 2 == 0 ? 1 : -1;
			String n = names[k];
			SpellArt.Painter coat = limbFur(60 + k), big = fur(21, 3);
			double[] top, knee, low, pawAt;
			SpellArt.Bone leg, lower, foot;
			if (k < 2) {
				top = SpellArt.v3(s * 5, 20, -14);
				knee = SpellArt.v3(s * 5.4, 11, -12.6);
				low = SpellArt.v3(s * 5.2, 3.6, -13.8);
				leg = m.bone(n, "body", top[0], top[1], top[2]);
				leg.box(s * 5, 17.6, -13.6, 6, 8, 8, big);
				SpellArt.limb(leg, top, knee, 5.6, 6.6, coat);
				lower = m.bone(n + "_low", n, knee[0], knee[1], knee[2]);
				SpellArt.limb(lower, knee, low, 4.2, 4.6, coat);
				foot = m.bone(n + "_paw", n + "_low", low[0], low[1], low[2]);
				pawAt = SpellArt.v3(s * 5.2, 1.7, -15.2);
			} else {
				top = SpellArt.v3(s * 5.2, 21, 15);
				knee = SpellArt.v3(s * 5.8, 12, 10.4);
				low = SpellArt.v3(s * 5.6, 6.4, 18.4);
				leg = m.bone(n, "body", top[0], top[1], top[2]);
				leg.box(s * 5.2, 17.4, 13.6, 6.6, 10, 10, big);
				SpellArt.limb(leg, top, knee, 6.4, 8.4, coat);
				lower = m.bone(n + "_low", n, knee[0], knee[1], knee[2]);
				SpellArt.limb(lower, knee, low, 4.6, 5.4, coat);
				foot = m.bone(n + "_paw", n + "_low", low[0], low[1], low[2]);
				SpellArt.limb(foot, low, SpellArt.v3(s * 5.4, 2.8, 16.6), 3.6, 4, coat);
				pawAt = SpellArt.v3(s * 5.4, 1.7, 15.4);
			}
			// The paw: broad, its toes, its claws (blades of steel, half drawn).
			foot.box(pawAt[0], pawAt[1], pawAt[2], 5.8, 3.4, 6.4, paw(coat));
			for (int j = 0; j < 4; j++) {
				double x = pawAt[0] + (j - 1.5) * 1.45;
				foot.box(x, 1.2, pawAt[2] - 3.4, 1.4, 2.4, 1.8, paw(coat)).density(3);
				SpellArt.limb(foot, SpellArt.v3(x, 1.8, pawAt[2] - 4.2), SpellArt.v3(x, 0.4, pawAt[2] - 6.2), 0.5, 0.8, blade()).density(5);
			}
		}
	}

	static void tail(SpellArt.Model m) {
		String up = "body";
		double[] at = SpellArt.v3(0, 26, 21);
		double[][] dirs = {{0, 1.2, 6}, {0, 0.4, 6}, {0, -1.6, 5.6}, {0, -3, 5}, {0, -3.6, 4.6}, {0, -3.4, 4}};
		for (int k = 0; k < dirs.length; k++) {
			SpellArt.Bone b = m.bone("tail_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, dirs[k]);
			double thick = 3.4 - k * 0.24;
			int kk = k;
			SpellArt.limb(b, at, SpellArt.add(next, SpellArt.times(SpellArt.sub(next, at), 0.08)), thick, thick, kk == dirs.length - 1 ? t -> SpellArt.col(t.up() > 0.35 ? STRIPE : FUR)
					: t -> {
						double f = (t.ly + kk * 6) / 3.3 - Math.floor((t.ly + kk * 6) / 3.3);
						return SpellArt.col(f < 0.32 ? STRIPE : SpellArt.mix(FUR_SHADE, FUR, 0.7));
					});
			up = b.name;
			at = next;
		}
	}

	/**
	 * The prowl (the user's ask, 2026-10-25: "look at how tigers actually move"): a great cat's stalking walk. Its feet fall in the lateral
	 * order, the left hind, the left fore, the right hind, the right fore, a quarter of the stride apart, each on the ground three fifths of it,
	 * so two or three always bear it. A leg on the ground sweeps back under the body as it goes over it, the shoulder blade rising over the spine
	 * as the fore takes its weight; one in the air lifts low and swings forward, the fore's wrist folding so its paw turns up behind it, then
	 * reaching out ahead, the paw laid down softly, flat; the hind's hock flexing under it. The body slung low between its legs, its spine
	 * swaying side to side with each pair of steps; the head held low and level, still while the body moves under it; the tail hanging low in a
	 * long curve, its tip curled up, swinging slowly behind.
	 */
	static void prowl(SpellArt.Model m) {
		double T = 1.8;
		SpellArt.Anim a = m.anim("prowl", T, "true");
		int steps = 36;
		String[] legs = {"leg_bl", "leg_fl", "leg_br", "leg_fr"};
		double[] down = {0.0, 0.25, 0.5, 0.75};
		double duty = 0.62;
		// Each joint round its own stride (SpellArt#keyed: from its paw set down, through its stance to duty, through its swing): the upper leg
		// (less than nought: forward), the elbow or hock, the wrist or the hind paw.
		double[][] foreUpper = {{0, -22}, {duty * 0.5, -2}, {duty, 18}, {duty + 0.1, 22}, {duty + 0.24, 2}, {duty + 0.34, -24}, {0.97, -23}};
		double[][] foreElbow = {{0, 4}, {duty * 0.5, 12}, {duty, 8}, {duty + 0.08, 40}, {duty + 0.18, 56}, {duty + 0.3, 14}, {duty + 0.35, -2}};
		double[][] forePaw = {{0, 0}, {duty * 0.6, 4}, {duty, -12}, {duty + 0.09, -46}, {duty + 0.2, -38}, {duty + 0.32, 4}, {duty + 0.36, 1}};
		double[][] hindUpper = {{0, -16}, {duty * 0.5, 6}, {duty, 26}, {duty + 0.1, 24}, {duty + 0.26, -4}, {duty + 0.35, -18}};
		double[][] hindHock = {{0, -4}, {duty * 0.5, -14}, {duty, 6}, {duty + 0.1, -48}, {duty + 0.22, -60}, {duty + 0.33, -12}, {duty + 0.36, -5}};
		double[][] hindPaw = {{0, 0}, {duty * 0.6, -6}, {duty, 20}, {duty + 0.1, 50}, {duty + 0.24, 32}, {duty + 0.34, -2}, {duty + 0.37, 0}};
		for (int k = 0; k < 4; k++) {
			double at = down[k];
			boolean front = k % 2 == 1;
			double[][] upper = front ? foreUpper : hindUpper, mid = front ? foreElbow : hindHock, paw = front ? forePaw : hindPaw;
			a.sample(legs[k], "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), upper), 0, 0});
			a.sample(legs[k] + "_low", "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), mid), 0, 0});
			a.sample(legs[k] + "_paw", "rotation", 0, T, steps, t -> new double[] {SpellArt.keyed(SpellArt.stride(t / T, at), paw), 0, 0});
			// The shoulder blade (and, less, the hip) rising over the spine as the leg takes the weight.
			a.sample(legs[k], "position", 0, T, steps, t -> {
				double s = SpellArt.stride(t / T, at);
				return new double[] {0, s < duty ? (front ? 1.1 : 0.5) * Math.sin(Math.PI * s / duty) : 0, 0};
			});
		}
		// Slung low, bobbing a little at each step; the spine swaying side to side, the shoulders rolling with the steps.
		a.sample("root", "position", 0, T, steps, t -> new double[] {0, -0.5 + 0.45 * Math.cos(4 * Math.PI * (t / T - 0.1)), 0});
		a.sample("body", "rotation", 0, T, steps, t -> new double[] {0.8 * Math.cos(4 * Math.PI * t / T), 4 * Math.sin(2 * Math.PI * (t / T - 0.12)),
				2.6 * Math.sin(2 * Math.PI * (t / T - 0.3))});
		// The head low and level, held still as the body sways and rolls under it.
		a.sample("neck", "rotation", 0, T, steps, t -> new double[] {9 - 0.6 * Math.cos(4 * Math.PI * t / T), -3 * Math.sin(2 * Math.PI * (t / T - 0.12)), 0});
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {2 - 0.8 * Math.cos(4 * Math.PI * (t / T - 0.1)), -1.4 * Math.sin(2 * Math.PI * (t / T - 0.12)),
				-2.2 * Math.sin(2 * Math.PI * (t / T - 0.3))});
		// The tail hanging low in a long curve, its tip curled up, swinging slowly, each part a little behind the one before it.
		double[] curve = {-24, -8, 0, 12, 24, 32}; // (more than nought: up) drooping from its root, the tip curled up
		for (int k = 0; k < 6; k++) {
			int kk = k;
			a.sample("tail_" + k, "rotation", 0, T, steps, t -> new double[] {curve[kk] + 2 * Math.sin(2 * Math.PI * (t / T - 0.1 * kk)),
					(4 + 2.4 * kk) * Math.sin(2 * Math.PI * (t / T - 0.25 - 0.11 * kk)), 0});
		}
	}

	/** The pounce: it crouches, springs (forepaws flung ahead, claws out, hind legs thrown back), lands on its prey, and gathers itself. Held: it stands. */
	static void pounce(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("pounce", 1.1, "hold_on_last_frame");
		double[] t = {0, 0.25, 0.45, 0.7, 1.1};
		double[] y = {0, -3.5, 6, 0, 0}, body = {0, 4, -14, 6, 0}, fore = {0, 30, -70, -20, 0}, foreLow = {0, -30, 20, 10, 0};
		double[] hind = {0, -30, 50, 10, 0}, hindLow = {0, 40, -20, 0, 0}, head = {0, 8, -8, 6, 0}, jaw = {4, 4, 32, 22, 4}, tailUp = {0, -10, 20, 10, 0};
		for (int i = 0; i < t.length; i++) {
			a.pos("root", t[i], 0, y[i], 0);
			a.rot("body", t[i], body[i], 0, 0);
			for (String s : new String[] {"l", "r"}) {
				a.rot("leg_f" + s, t[i], fore[i], 0, 0);
				a.rot("leg_f" + s + "_low", t[i], foreLow[i], 0, 0);
				a.rot("leg_b" + s, t[i], hind[i], 0, 0);
				a.rot("leg_b" + s + "_low", t[i], hindLow[i], 0, 0);
			}
			a.rot("head", t[i], head[i], 0, 0);
			a.rot("jaw", t[i], jaw[i], 0, 0);
			a.rot("tail_0", t[i], tailUp[i] - 10, 0, 0);
		}
	}

	/** The claw: it rears a little and rakes its right forepaw down and across, its jaws wide; then sets it down. Held: it stands over its work. */
	static void claw(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("claw", 0.7, "hold_on_last_frame");
		double[] t = {0, 0.2, 0.35, 0.5, 0.7};
		double[] body = {0, -10, -6, 0, 0}, up = {0, -80, 10, 0, 0}, across = {0, -20, 25, 0, 0}, low = {0, 30, -10, 0, 0}, turn = {0, -8, 8, 0, 0}, jaw = {4, 20, 34, 10, 4};
		for (int i = 0; i < t.length; i++) {
			a.rot("body", t[i], body[i], 0, 0);
			a.rot("leg_fr", t[i], up[i], across[i], 0);
			a.rot("leg_fr_low", t[i], low[i], 0, 0);
			a.rot("leg_fl", t[i], -body[i], 0, 0);
			a.rot("head", t[i], 0, turn[i], 0);
			a.rot("jaw", t[i], jaw[i], 0, 0);
		}
	}

	// --- Myriad Swords Return: painters ---

	/** A blade lying flat along z (its tip at +z): steel along a raised ridge, its edges keen, glowing with sword-light, brighter toward the tip. */
	static SpellArt.Painter steel(int seed) {
		return t -> {
			if (!t.flat()) return SpellArt.glow(GLEAM, 0.95);
			double u = t.across() - 0.5, v = t.back(), half = 0.5 * (v > 0.84 ? (1 - v) / 0.16 : 1);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			if (half - Math.abs(u) < 0.17) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, v), 0.96);
			if (Math.abs(u) < 0.08) return SpellArt.col(SpellArt.mix(SILVER, GLEAM, 0.5));
			return SpellArt.col(SpellArt.mix(STEEL, SILVER, 0.5 + 0.4 * v + (SpellArt.noise(v * 20, u * 3, seed) - 0.5) * 0.2));
		};
	}

	/** Gold, engraved: lines cut darker, brightest where it faces up. */
	static SpellArt.Painter gold(int seed) {
		return t -> {
			double n = SpellArt.noise(t.px * 1.5 + seed, t.py * 1.5 + t.pz, seed);
			int c = SpellArt.mix(GOLD_DARK, GOLD, 0.5 + 0.4 * n + (t.ny > 0.5 ? 0.2 : 0));
			if (Math.abs(n - 0.5) < 0.04) c = SpellArt.shade(c, -0.35);
			return SpellArt.col(c);
		};
	}

	/** A hilt's grip: dark silk wound on it in diamonds. */
	static SpellArt.Painter grip() {
		return t -> {
			double a = (t.ly + t.lx + t.lz) / 1.2, b = (t.ly - t.lx - t.lz) / 1.2;
			boolean seam = a - Math.floor(a) < 0.18 || b - Math.floor(b) < 0.18;
			return SpellArt.col(seam ? 0x101418 : SpellArt.mix(0x2A3442, 0x3E4C60, SpellArt.hash((int) Math.floor(a), (int) Math.floor(b), 3)));
		};
	}

	/** A flying sword's trail of sword-light (a flat blade along z, nearest the sword at its far end): white, thinning and fading behind it. */
	static SpellArt.Painter trail() {
		return t -> {
			double v = t.back(), u = Math.abs(t.across() - 0.5) * 2;
			if (u > v) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, v), 0.15 + 0.6 * v * (1 - u));
		};
	}

	// --- Myriad Swords Return ---

	/** Each ring of swords: how many, radius, height, tilt (degrees), which way they fly (1: with a turn of +y). */
	static final double[][] SWORD_RINGS = {{12, 18, 14, 0, 1}, {16, 28, 22, 0, -1}, {20, 38, 10, 16, 1}, {10, 11, 34, -50, 1}};

	/**
	 * One flying sword: a jian lying along +z from {@code at} (its middle), turned by {@code rx}, {@code ry} about that middle; its blade,
	 * guard, grip, pommel and trail painted once (by the first, {@code like}, if not null) and shared by every other.
	 */
	static SpellArt.Cube[] sword(SpellArt.Bone bone, double[] at, double rx, double ry, SpellArt.Cube[] like) {
		double x = at[0], y = at[1], z = at[2];
		SpellArt.Cube[] c = new SpellArt.Cube[5];
		if (like == null) {
			c[0] = bone.box(x, y, z + 3.2, 1.3, 0.3, 10, steel(3)).density(4);
			c[1] = bone.box(x, y, z - 2.1, 3, 0.7, 0.7, gold(4)).density(4);
			c[2] = bone.box(x, y, z - 3.9, 0.55, 0.55, 2.9, grip()).density(4);
			c[3] = bone.box(x, y, z - 5.7, 0.9, 0.9, 0.9, gold(5)).density(4);
			c[4] = bone.box(x, y + 0.05, z - 12.6, 1.6, 0.1, 12, trail()).density(3);
		} else {
			double[][] centres = {{0, 0, 3.2}, {0, 0, -2.1}, {0, 0, -3.9}, {0, 0, -5.7}, {0, 0.05, -12.6}};
			for (int i = 0; i < 5; i++) c[i] = bone.like(like[i], x + centres[i][0], y + centres[i][1], z + centres[i][2], false);
		}
		for (SpellArt.Cube cube : c) cube.rot(rx, ry, 0, x, y, z);
		return c;
	}

	/**
	 * Myriad Swords Return: four rings of flying swords ("ring_0".."ring_3", fifty-eight jian, each with its trail of sword-light) wheeling
	 * round their caster, the last a crown of swords above them pointing down and in; centred on its caster's feet.
	 */
	static void myriadSwords(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Cube[] first = null;
		for (int r = 0; r < SWORD_RINGS.length; r++) {
			double[] ring = SWORD_RINGS[r];
			// The ring tilted, its swords wheeling on its own "_spin" (a turn on a tilted bone would rock it, not wheel it).
			m.bone("ring_" + r, "root", 0, ring[2], 0, ring[3] > 0 ? ring[3] : 0, 0, 0);
			SpellArt.Bone bone = m.bone("ring_" + r + "_spin", "ring_" + r, 0, ring[2], 0);
			for (int k = 0; k < ring[0]; k++) {
				double th = 360.0 * k / ring[0];
				double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * ring[1], ring[2], Math.cos(Math.toRadians(th)) * ring[1]);
				// Along the ring (the way it wheels); the crown's point down and in.
				double ry = ring[3] < 0 ? th + 180 : th + ring[4] * 90, rx = ring[3] < 0 ? ring[3] : 0;
				SpellArt.Cube[] made = sword(bone, at, rx, ry, first);
				if (first == null) first = made;
			}
		}
		orbit(m);
		merge(m);
	}

	/** The orbit, looped without a seam: each ring wheels one sword round, the way its swords fly. */
	static void orbit(SpellArt.Model m) {
		double T = 1.2;
		SpellArt.Anim a = m.anim("orbit", T, "true");
		for (int r = 0; r < SWORD_RINGS.length; r++) {
			double turn = SWORD_RINGS[r][4] * 360.0 / SWORD_RINGS[r][0];
			a.sample("ring_" + r + "_spin", "rotation", 0, T, 2, t -> new double[] {0, turn * t / T, 0});
		}
	}

	/** The merging: the rings close in and rise together into a point above (where the colossal sword is born), wheeling faster as they go. */
	static void merge(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("merge", 1.2, "hold_on_last_frame");
		double[] t = {0, 0.3, 0.6, 0.9, 1.2};
		for (int r = 0; r < SWORD_RINGS.length; r++) {
			double turn = SWORD_RINGS[r][4] * 360.0;
			for (double tt : t) {
				double e = SpellArt.easeIn(tt / 1.2);
				a.scale("ring_" + r, tt, Math.max(0.02, 1 - e), Math.max(0.02, 1 - e * 0.8), Math.max(0.02, 1 - e));
				a.pos("ring_" + r, tt, 0, 46 * e, 0);
				a.rot("ring_" + r + "_spin", tt, 0, turn * e, 0);
			}
		}
	}

	// --- The colossal sword ---

	static final double BLADE = 122, BLADE_WIDE = 12;

	/** The colossal sword's blade's half-width at {@code y} up it from its point: drawn to its point over its lowest 24 pixels. */
	static double bigHalf(double y) {
		return BLADE_WIDE / 2 * Math.pow(Math.max(0, Math.min(1, y / 24)), 0.75);
	}

	/**
	 * The colossal sword's blade (standing, its point at the foot, its broad faces looking forward and back, solid to its outline:
	 * {@link #bigHalf}): steel, its edges blazing with sword-light (the faces at its outline, its sides and its point's steps, all edge), a
	 * ridge of silver down its middle; down its upper part an inscription of glyphs in gold.
	 */
	static SpellArt.Painter bigBlade(int seed) {
		return t -> {
			double y = t.qy(), u = t.qx() / BLADE_WIDE, half = bigHalf(y) / BLADE_WIDE;
			if (t.side() || t.flat()) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, 0.6), 0.97);
			double edge = Math.max(0, half - Math.abs(u)) * BLADE_WIDE;
			if (edge < 1) return SpellArt.glow(SpellArt.mix(GLEAM, WHITE, 0.5 + 0.5 * (1 - edge)), 0.97);
			double up = y / BLADE;
			if (up > 0.56 && up < 0.94 && Math.abs(u) < 0.22) {
				double cell = (up - 0.56) / 0.38 * 6;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(ci, seed, (u + 0.22) / 0.44, cell - ci)) return SpellArt.glow(SpellArt.mix(GOLD, 0xFFF0C0, 0.35), 0.97);
				return SpellArt.col(SpellArt.mix(STEEL_DARK, STEEL, 0.4));
			}
			if (Math.abs(u) < 0.05) return SpellArt.col(SpellArt.mix(SILVER, GLEAM, 0.4));
			double n = SpellArt.noise(y * 0.3 + seed, u * 4, seed);
			return SpellArt.col(SpellArt.mix(STEEL, SILVER, 0.35 + 0.4 * n + 0.2 * Math.abs(u) * 2));
		};
	}

	/**
	 * The colossal sword of Myriad Swords Return, forty blocks of it, point down: its blade ("sword"), a guard of gold with a jewel of
	 * sword-light, its grip wound in dark silk, its pommel; and a red tassel from the pommel ("tassel_0".."tassel_2"). Its point at y 0.
	 */
	static void colossalSword(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone sword = m.bone("sword", "root", 0, 0, 0);
		// Its blade, solid to its outline: its point drawn in slice by slice, the rest of it whole (a box painted to its point and clear round
		// it was a sheet seen edge-on: the user, 2026-10-25, "2D basically"); thick, its ridge of silver standing out down its middle.
		SpellArt.Painter steel = bigBlade(11);
		for (SpellArt.Cube slice : SpellArt.extrude(sword, SpellArt.Plane.XY, 0, 0, 24, 1, 3, y -> new double[] {-bigHalf(y), bigHalf(y)}, steel)) slice.density(3);
		sword.box(0, 24 + (BLADE - 24) / 2, 0, BLADE_WIDE, BLADE - 24, 3, steel).only(SpellArt.Face.FRONT, SpellArt.Face.BACK, SpellArt.Face.SIDE_A, SpellArt.Face.SIDE_B).density(3);
		sword.box(0, 14 + (BLADE - 14) / 2, 0, 2.2, BLADE - 14, 4.2, t -> t.flat() ? SpellArt.col(SILVER) : SpellArt.col(SpellArt.mix(SILVER, GLEAM, 0.35 + 0.2 * t.up())));
		// The guard: a bar of gold, its ends sweeping up, a jewel of sword-light at its heart.
		SpellArt.Painter gilt = gold(21);
		sword.box(0, BLADE + 3, 0, 26, 5, 6.4, gilt);
		sword.box(0, BLADE + 6.2, 0, 14, 2.4, 5, gilt);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(sword, SpellArt.v3(s * 12, BLADE + 3, 0), SpellArt.v3(s * 15.6, BLADE + 9, 0), 3, 4.6, gilt);
			SpellArt.limb(sword, SpellArt.v3(s * 15.6, BLADE + 9, 0), SpellArt.v3(s * 15, BLADE + 12.4, 0), 2, 3.4, gilt);
		}
		sword.box(0, BLADE + 3, 0, 4, 4, 7, t -> SpellArt.glow(SpellArt.mix(GLEAM, ICE_EYE, Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2), 0.97)).rot(0, 0, 45).density(3);
		// The grip and the pommel.
		sword.box(0, BLADE + 8 + 15, 0, 4.6, 30, 4.6, grip()).density(2);
		sword.box(0, BLADE + 39.4, 0, 8.6, 6, 8.6, gilt).rot(0, 45, 0);
		sword.box(0, BLADE + 43.4, 0, 4, 3, 4, gilt).rot(0, 45, 0);
		// The tassel, from a ring at the pommel, hanging down beside the grip.
		String up = "sword";
		double[] at = SpellArt.v3(4.6, BLADE + 38.6, 0);
		double[][] steps = {{1.6, -9, 0}, {0.8, -9, 0}, {0.4, -8, 0}};
		for (int k = 0; k < steps.length; k++) {
			SpellArt.Bone b = m.bone("tassel_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, steps[k]);
			SpellArt.limb(b, at, next, 1.1, 1.1, Earth.cord(false)).density(3);
			if (k == steps.length - 1) {
				b.box(next[0], next[1] - 0.6, next[2], 2.4, 2.4, 2.4, Earth.cord(false)).density(3);
				for (int j = 0; j < 4; j++) SpellArt.blade(b, SpellArt.add(next, SpellArt.v3(0, -1.2, 0)), 11, 4, 180, j * 45, 0, Earth.cord(true)).density(3);
			}
			up = b.name;
			at = next;
		}
		hum(m);
	}

	/** The hum (looped): the sword thrumming, the tassel swinging. */
	static void hum(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("sword", "position", 0, T, 16, t -> new double[] {0.25 * Math.sin(2 * Math.PI * t / T * 8), 0.6 * Math.sin(2 * Math.PI * t / T), 0});
		for (int k = 0; k < 3; k++) {
			int kk = k;
			a.sample("tassel_" + k, "rotation", 0, T, 12, t -> new double[] {(3 + kk * 2) * Math.sin(2 * Math.PI * t / T - kk * 0.8), 0, (4 + kk * 2) * Math.sin(2 * Math.PI * t / T - kk * 0.6)});
		}
	}
}
