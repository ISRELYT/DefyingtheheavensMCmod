/**
 * The looks of the spell of both paths of Dao Integration (HeavenDemonSpells, 2026-10-24): the Heaven-Demon Avatar, heaven's gold light and
 * a demon's crimson qi made into one colossus, half and half; and its two hands that come down out of the sky, heaven's palm and the
 * demon's claw. Made of the two qi, as the spirit beasts are of their elements (the user's rule).
 */
public class HeavenDemon {
	// --- Heaven's light, gold from its deep to white; a demon's qi, crimson from its dark blood to embers; horn ---
	static final int GOLD_DEEP = 0x7E5210, GOLD = 0xE2A82C, GOLD_BRIGHT = 0xFFD457, GOLD_PALE = 0xFFF0B6, WHITE_LIGHT = 0xFFFDF2;
	static final int BLOOD_DARK = 0x2E0306, BLOOD = 0x620912, CRIMSON = 0xA8122A, SCARLET = 0xE42C3A, EMBER = 0xFF6A48, EMBER_PALE = 0xFFC8A0;
	static final int HORN = 0x1A0A0E, HORN_LIGHT = 0x3E1C22;
	static final int[] HEAVEN = {GOLD_DEEP, GOLD, GOLD_BRIGHT, GOLD_PALE, WHITE_LIGHT};
	static final int[] DEMON = {BLOOD_DARK, BLOOD, CRIMSON, SCARLET, EMBER, EMBER_PALE};

	// --- Painters ---

	/**
	 * Heaven's light made solid, as a general's lamellar harness: gold in overlapping plates (each lit along its upper edge, shadowed under
	 * the seam above it), brightest where it faces up, cloud scrolls engraved in it catching the light; glowing.
	 */
	static SpellArt.Painter heavenly(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.06, t.py * 0.06, t.pz * 0.06, seed, 4);
			double band = t.py / 9 + 0.3 * Math.sin(t.px * 0.15 + seed) + 0.2 * Math.sin(t.pz * 0.2), bf = band - Math.floor(band);
			if (bf < 0.035) return SpellArt.col(SpellArt.mix(GOLD_DEEP, BLOOD_DARK, 0.4));
			double scroll = Math.sin(t.px * 0.5 + 2.2 * Math.sin(t.py * 0.4 + seed)) * Math.cos(t.py * 0.5 + t.pz * 0.3 + 1.5 * Math.sin(t.px * 0.25));
			double up = 0.3 * Math.max(0, t.ny) - 0.35 * Math.max(0, -t.ny), rim = bf > 0.9 ? 0.22 : bf < 0.12 ? -0.14 : 0;
			double light = SpellArt.clamp01(0.42 + (n - 0.5) * 0.9 + up + rim + (Math.abs(scroll) > 0.93 ? 0.35 : 0));
			return SpellArt.glow(SpellArt.ramp(light, HEAVEN), 0.95);
		};
	}

	/** A demon's qi made into sinew: dark crimson, mottled, lit above and darker beneath, veins of burning scarlet through it (they glow). */
	static SpellArt.Painter demonic(int seed) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.1, t.py * 0.08, t.pz * 0.1, seed, 3);
			double vein = Math.pow(1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.14, t.py * 0.1, t.pz * 0.14, seed + 3, 3) - 1), 9);
			if (vein > 0.55) return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01((vein - 0.55) * 2.6), SCARLET, EMBER, EMBER_PALE), 0.96);
			double up = 0.22 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny);
			double light = SpellArt.clamp01(0.42 + (flow - 0.5) * 0.8 + up);
			return SpellArt.col(SpellArt.ramp(light, BLOOD_DARK, BLOOD, CRIMSON, SCARLET), 0.95);
		};
	}

	/** The avatar's two halves: heaven's to -x, the demon's to +x, a seam of white light wavering down between them. */
	static SpellArt.Painter halves(SpellArt.Painter heaven, SpellArt.Painter demon) {
		return t -> {
			double x = t.px + 0.6 * Math.sin(t.py * 0.3) + 0.4 * Math.sin(t.pz * 0.5);
			if (Math.abs(x) < 0.55) return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_PALE, Math.abs(x) / 0.55), 0.98);
			return x < 0 ? heaven.paint(t) : demon.paint(t);
		};
	}

	/** Heaven's eye (on the front of its cube): an almond of white light under a low lid, calm; gold round it. */
	static SpellArt.Painter heavenEye() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(GOLD_DEEP);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = y + 0.15 - (1 - x * x) * 0.75;
			if (shape > 0 || y < -(1 - x * x) * 0.45) return SpellArt.glow(GOLD_BRIGHT, 0.95);
			return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_PALE, Math.abs(x)));
		};
	}

	/** The demon's eye: a slanting slit of fire, gold at its heart through ember to scarlet; dark round it. */
	static SpellArt.Painter demonEye() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(BLOOD_DARK);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2 - x * 0.35, shape = Math.abs(y) - (1 - x * x) * 0.7;
			if (shape > 0) return SpellArt.col(BLOOD_DARK);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(1 - Math.abs(y) * 1.8 - Math.abs(x) * 0.3), SCARLET, EMBER, GOLD_BRIGHT, WHITE_LIGHT));
		};
	}

	/** The third eye on the brow, on the seam: upright, white light ringed in gold above and crimson below. */
	static SpellArt.Painter thirdEye() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(BLOOD_DARK);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, shape = Math.abs(x) - (1 - y * y) * 0.85;
			if (shape > 0.12) return SpellArt.col(BLOOD_DARK);
			if (shape > -0.18) return SpellArt.glow(y > 0 ? GOLD_BRIGHT : SCARLET, 0.97);
			return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_PALE, Math.abs(y)));
		};
	}

	/** Its mouth (on the front of its cube): on heaven's half a calm line of gold; on the demon's a snarl, dark, burning within. */
	static SpellArt.Painter mouth() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.col(BLOOD_DARK);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2;
			if (x < 0) return Math.abs(y) < 0.4 ? SpellArt.glow(GOLD_PALE, 0.95) : SpellArt.glow(GOLD, 0.94);
			return Math.abs(y + 0.2 * x) < 0.55 ? SpellArt.glow(SpellArt.mix(EMBER, SCARLET, x), 0.95) : SpellArt.col(BLOOD_DARK);
		};
	}

	/** A demon's horn, spike or talon (a limb along it): black horn, ridged, a burning edge of scarlet, its point (the top of its cube) ember-bright. */
	static SpellArt.Painter horn(int seed) {
		return t -> {
			double up = t.up();
			if (up > 0.86) return SpellArt.glow(SpellArt.mix(SCARLET, EMBER_PALE, (up - 0.86) / 0.14), 0.96);
			double ridge = t.ly / 1.3 - Math.floor(t.ly / 1.3);
			if (ridge < 0.14) return SpellArt.col(HORN);
			if (t.f == SpellArt.Face.FRONT && (t.across() < 0.15 || t.across() > 0.85)) return SpellArt.glow(SCARLET, 0.85);
			return SpellArt.col(SpellArt.mix(HORN, HORN_LIGHT, 0.3 + 0.5 * SpellArt.noise(t.fx * 3 + seed, t.ly * 0.5, seed)));
		};
	}

	static SpellArt.Painter heavenFlame(double licks, int seed) {
		return Fire.flame(licks, seed, HEAVEN);
	}

	static SpellArt.Painter demonFlame(double licks, int seed) {
		return Fire.flame(licks, seed, DEMON);
	}

	/**
	 * The cloud of the two it rises out of (on a cube): billows of gold light on heaven's side (-x), of crimson qi on the demon's (+x), swirled
	 * together where they meet; bright on their tops, fading beneath; seen through, the brightest glowing.
	 */
	static SpellArt.Painter cloud(int seed) {
		return t -> {
			double swirl = SpellArt.fbm3(t.px * 0.05, t.py * 0.1, t.pz * 0.05, seed, 3);
			double side = SpellArt.clamp01(0.5 + t.px * 0.03 + (swirl - 0.5) * 1.6);
			double billow = SpellArt.fbm3(t.px * 0.14, t.py * 0.2, t.pz * 0.14, seed + 5, 3);
			double light = SpellArt.clamp01(0.3 + billow * 0.6 + 0.3 * Math.max(0, t.ny) - 0.35 * Math.max(0, -t.ny));
			int c = SpellArt.mix(SpellArt.ramp(light, HEAVEN), SpellArt.ramp(light, DEMON), SpellArt.smooth(0.35, 0.65, side));
			// Soft at its edges (a billow, not a block), thin beneath.
			double edge = Math.min(Math.min(t.fx, 1 - t.fx), Math.min(t.fy, 1 - t.fy)) * 2;
			double a = (0.5 + 0.4 * light - (t.ny < -0.5 ? 0.3 : 0)) * (0.35 + 0.65 * SpellArt.smooth(0, 0.6, edge + (billow - 0.5) * 0.6));
			return light > 0.74 ? SpellArt.glow(c, a) : SpellArt.col(c, a);
		};
	}

	/**
	 * The halo (on a sheet standing behind the head, facing -z): a ring of light, gold with rays standing off it on heaven's side (-x), crimson
	 * with fire licking out of it on the demon's; clear elsewhere.
	 */
	static SpellArt.Painter halo(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y), ang = Math.atan2(y, x);
			boolean heaven = x < 0;
			if (Math.abs(r - 0.76) < 0.035) return SpellArt.glow(heaven ? GOLD_PALE : EMBER, 0.97);
			if (Math.abs(r - 0.68) < 0.012) return SpellArt.glow(heaven ? GOLD : SCARLET, 0.9);
			if (heaven) {
				double ray = Math.pow(Math.abs(Math.cos(ang * 12)), 8);
				if (r > 0.79 && r < 0.8 + 0.18 * ray) return SpellArt.glow(SpellArt.mix(GOLD_BRIGHT, WHITE_LIGHT, ray), 0.5 + 0.45 * ray);
				return SpellArt.CLEAR;
			}
			double lick = SpellArt.fbm(ang * 5 + seed, r * 3, seed, 2), outer = 0.79 + 0.2 * lick;
			if (r > 0.79 && r < outer) {
				double heat = 1 - (r - 0.79) / (outer - 0.79);
				return SpellArt.glow(SpellArt.ramp(heat, DEMON), 0.35 + 0.55 * heat);
			}
			return SpellArt.CLEAR;
		};
	}

	/** Rays of light behind the halo (on a sheet): twelve, thin, fading out from the head; the same all round (so, wheeling, it never jumps). */
	static SpellArt.Painter rays() {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y), ang = Math.atan2(y, x);
			if (r < 0.35 || r > 0.99) return SpellArt.CLEAR;
			double ray = Math.pow(Math.abs(Math.cos(ang * 6)), 30);
			if (ray < 0.25) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(GOLD_PALE, WHITE_LIGHT, ray), (1 - r) * 0.8 * ray);
		};
	}

	/** The emblem on its breast, on the seam: a sun of white light in a ring, the ring gold on heaven's side and fire on the demon's. */
	static SpellArt.Painter emblem() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.glow(GOLD, 0.94);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98) return SpellArt.col(BLOOD_DARK);
			if (r > 0.72) return SpellArt.glow(x < 0 ? GOLD_BRIGHT : SCARLET, 0.97);
			double ray = Math.pow(Math.abs(Math.cos(Math.atan2(y, x) * 4)), 6);
			return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_PALE, r * (1 - ray)), 0.98);
		};
	}

	/** A sash round the waist: gold on heaven's side, crimson on the demon's, a band of light along its middle. */
	static SpellArt.Painter sash() {
		return t -> {
			if (Math.abs(t.up() - 0.5) < 0.14) return SpellArt.glow(t.px < 0 ? GOLD_PALE : EMBER, 0.95);
			return t.px < 0 ? SpellArt.glow(GOLD, 0.94) : SpellArt.col(CRIMSON);
		};
	}

	/**
	 * Light poured down from the sky (a limb standing up from a hand): the colours of {@code heat}, brightest down its middle, fading out as it
	 * rises (its foot at the hand, its top lost in the sky).
	 */
	static SpellArt.Painter beam(int[] heat) {
		return t -> {
			double up = t.up(), u = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK ? t.across() : t.back(), core = 1 - Math.abs(u - 0.5) * 2;
			if (t.flat()) return SpellArt.CLEAR;
			double a = (1 - up) * (1 - up) * 0.8 * (0.45 + 0.55 * core);
			if (a < 0.03) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.45 + 0.55 * core - 0.3 * up), heat), a);
		};
	}

	/**
	 * Heaven's seal on the ground under the palm (on a sheet): a ring of gold, a band of the glyphs of heaven round it (Fire#scriptRing), and at
	 * its heart the character for heaven in white light; clear elsewhere.
	 */
	static SpellArt.Painter palmSeal(int seed) {
		SpellArt.Painter script = Fire.scriptRing(0.62, 0.86, 16, seed);
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.97) return SpellArt.CLEAR;
			if (Math.abs(r - 0.94) < 0.018 || Math.abs(r - 0.56) < 0.014) return SpellArt.glow(GOLD_PALE, 0.95);
			if (r > 0.58) return script.paint(t);
			double u = x / 0.44, v = z / 0.44;
			boolean stroke = Fire.segment(u, v, -0.6, -0.6, 0.6, -0.6) < 0.12 || Fire.segment(u, v, -0.85, -0.12, 0.85, -0.12) < 0.13
					|| Fire.segment(u, v, 0, -0.6, 0, -0.12) < 0.12 || Fire.segment(u, v, 0, -0.12, -0.72, 0.82) < 0.13 || Fire.segment(u, v, 0, -0.12, 0.72, 0.82) < 0.13;
			return stroke ? SpellArt.glow(WHITE_LIGHT, 0.97) : SpellArt.CLEAR;
		};
	}

	/** The claw's gouges in the ground (on a sheet, its length along z): four furrows burning crimson to ember, tapered at both ends. */
	static SpellArt.Painter gouges() {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = t.back();
			for (int k = 0; k < 4; k++) {
				double cx = (k - 1.5) * 0.42 + 0.05 * Math.sin(z * 6 + k), w = 0.08 * Math.pow(Math.sin(Math.PI * z), 0.6), d = Math.abs(x - cx);
				if (d < w) return SpellArt.glow(SpellArt.ramp(1 - d / w, DEMON), 0.92);
				if (d < w + 0.035) return SpellArt.col(BLOOD_DARK, 0.86);
			}
			return SpellArt.CLEAR;
		};
	}

	// --- The Heaven-Demon Avatar ---

	/**
	 * The Heaven-Demon Avatar, some forty blocks of it, facing -z, rising out of a cloud of the two at the origin: its body from the waist up
	 * ("body"), heaven's half (-x) a harness of gold light, the demon's (+x) crimson sinew veined with fire, a seam of white light between; heaven's
	 * shoulder a pauldron of gold, the demon's a ridge of horn spikes; its head ("head") split as its body, heaven's eye calm, the demon's a slit of
	 * fire, a third eye on the seam, a fang, a crown of gold rays on heaven's side and a mane of crimson fire on the demon's, a great horn
	 * ("horn_0".."horn_2"); a halo behind its head ("halo", its rays "halo_rays" wheeling), gold and fire; heaven's arm ("arm_h", "arm_h_low",
	 * "hand_h") a palm held up open, the demon's ("arm_d", "arm_d_low", "hand_d") clawed with talons of horn; the cloud ("cloud", its rings
	 * "cloud_0".."cloud_2" and the flames licking out of it, "cloud_flames").
	 */
	static void avatar(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		cloud(m);
		SpellArt.Bone body = m.bone("body", "root", 0, 30, 0);
		SpellArt.Painter skin = halves(heavenly(11), demonic(12));
		// Its trunk, broadening from the waist to the shoulders.
		body.box(0, 36, 1, 30, 12, 19, skin);
		body.box(0, 47.5, 0, 38, 11, 22, skin);
		body.box(0, 60, -0.5, 46, 14, 24, skin);
		body.box(0, 71, -1, 52, 9, 25, skin);
		body.box(0, 77, 0, 42, 5, 22, skin);
		body.box(0, 81.5, 0, 13, 5, 13, skin);
		// Heaven's side armoured: a breastplate with its round protector-mirror, plates over the belly. The demon's bare: breast and belly of
		// sinew, ribs ridged.
		body.box(-11.5, 66, -13.6, 19, 12, 1.6, heavenly(13)).density(1.6);
		body.box(-11, 66.5, -14.6, 8, 8, 0.8, mirror()).density(4);
		for (int k = 0; k < 3; k++) body.box(-9.5, 52 - k * 4.4, -11.6 - k * 0.3, 16 - k, 4.6, 1.6, heavenly(18 + k)).density(1.6).rot(-6, 0, 0);
		body.box(10, 66, -13.2, 16, 9, 2.4, demonic(14)).density(1.6);
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 2; c++) body.box(5 + c * 5.4, 53 - r * 4.6, -11.6, 4.6, 3.8, 1.6, demonic(30 + r * 2 + c)).density(2);
		}
		for (int k = 0; k < 3; k++) body.box(19.6, 61 - k * 4, -9 - k, 2.2, 1.6, 8, demonic(36 + k)).density(2);
		body.box(0, 64.5, -14.4, 8, 8, 1, emblem()).density(4);
		body.box(0, 41.5, 1, 32, 3.2, 21, sash()).density(1.6);
		// Spikes of horn down the demon's half of its back.
		for (int k = 0; k < 4; k++) {
			SpellArt.limb(body, SpellArt.v3(6 + (k % 2) * 3, 72 - k * 7, 11), SpellArt.v3(8 + (k % 2) * 4, 76 - k * 7, 19 - k), 2.4, 2.4, horn(60 + k)).density(2);
		}
		// Shoulders: heaven's a pauldron of gold in three plates, a cloud scroll curled on it; the demon's a ridge of horn spikes.
		body.box(-26, 73, 0, 16, 8, 20, heavenly(15));
		body.box(-27.5, 69, 0, 15, 4, 22, heavenly(16));
		body.box(-28.5, 65.8, 0, 13, 3, 21, heavenly(17));
		SpellArt.limb(body, SpellArt.v3(-22, 77, -6), SpellArt.v3(-30, 79.5, -2), 2.4, 2.4, heavenly(23));
		SpellArt.limb(body, SpellArt.v3(-30, 79.5, -2), SpellArt.v3(-30.5, 77, 4), 2.2, 2.2, heavenly(24));
		body.box(26, 71.5, 0, 14, 9, 18, demonic(25));
		for (int k = 0; k < 4; k++) {
			SpellArt.limb(body, SpellArt.v3(22 + k * 3, 75.5, -5 + k * 3.2), SpellArt.v3(25 + k * 4.4, 86 - k * 1.2, -6 + k * 3.6), 2.6 - k * 0.3, 2.6 - k * 0.3, horn(40 + k)).density(2);
		}
		head(m);
		arms(m);
		rise(m);
		loom(m);
		strikePalm(m);
		strikeClaw(m);
	}

	/** The cloud it rises out of: three rings of billows, flames of the two licking up out of them. */
	static void cloud(SpellArt.Model m) {
		m.bone("cloud", "root", 0, 0, 0);
		// count, radius, height, then each billow's width, height, depth: each billow two boxes, the second turned to round it off.
		double[][] rings = {{10, 24, 7, 15, 9, 12}, {12, 32, 13, 13, 8, 11}, {8, 18, 22, 13, 7, 10}};
		for (int r = 0; r < rings.length; r++) {
			double[] g = rings[r];
			SpellArt.Bone ring = m.bone("cloud_" + r, "cloud", 0, g[2], 0);
			for (int k = 0; k < g[0]; k++) {
				double th = 360.0 * k / g[0] + r * 17, x = Math.sin(Math.toRadians(th)) * g[1], z = Math.cos(Math.toRadians(th)) * g[1];
				ring.box(x, g[2], z, g[3], g[4], g[5], cloud(300 + r * 20 + k)).rot(0, th, 0).density(1);
				ring.box(x, g[2] + g[4] * 0.15, z, g[3] * 0.72, g[4] * 1.2, g[5] * 0.72, cloud(310 + r * 20 + k)).rot(0, th + 45, 0).density(1);
			}
		}
		SpellArt.Bone flames = m.bone("cloud_flames", "cloud", 0, 18, 0);
		for (int k = 0; k < 10; k++) {
			double th = 360.0 * k / 10 + 9, x = Math.sin(Math.toRadians(th)) * 22, z = Math.cos(Math.toRadians(th)) * 22;
			boolean heaven = x < 0;
			SpellArt.shared(m, flames, heaven ? "cloud_flame_h" : "cloud_flame_d", false, SpellArt.v3(x, 16, z), 24, 13, 0, th + 90, 0,
					heaven ? heavenFlame(2.4, 310) : demonFlame(2.4, 320), 1.4, false);
		}
	}

	static void head(SpellArt.Model m) {
		SpellArt.Bone head = m.bone("head", "body", 0, 83, 0);
		SpellArt.Painter face = halves(heavenly(21), demonic(22));
		head.box(0, 94.5, -1, 19, 19, 19, face);
		head.box(0, 86, -2.5, 14, 5, 14, face);
		head.box(0, 85.4, -9.4, 6, 3, 2, face).density(2);
		head.box(0, 99.4, -10.5, 18, 2, 2.4, face).density(2);
		for (int s = -1; s <= 1; s += 2) head.box(s * 6.4, 92.6, -10, 5, 2.4, 2.6, face).density(2);
		head.box(0, 91.6, -11.2, 2.6, 6, 2.2, face).density(2);
		head.box(0, 104.6, -1, 15, 1.6, 15, face);
		// Heaven's long earlobe; the demon's ear drawn to a point.
		head.box(-10, 91.5, 0.5, 1.4, 8, 3, heavenly(26)).density(2);
		SpellArt.limb(head, SpellArt.v3(9.8, 95, 0.5), SpellArt.v3(13.5, 100.5, 3), 1.6, 3, demonic(27)).density(2);
		// Two small horns over the demon's eye.
		for (int k = 0; k < 2; k++) SpellArt.limb(head, SpellArt.v3(3 + k * 3.2, 100.6, -9.8), SpellArt.v3(3.6 + k * 4, 103.2, -10.6), 1, 1, horn(28 + k)).density(4);
		head.box(-4.8, 96, -11.2, 6, 2, 0.6, heavenEye()).density(6);
		head.box(4.8, 96, -11.2, 6, 2.6, 0.6, demonEye()).density(6);
		head.box(0, 101, -11.2, 1.8, 4.2, 0.6, thirdEye()).density(6);
		head.box(0, 87.6, -11.2, 9, 1.2, 0.5, mouth()).density(6);
		SpellArt.limb(head, SpellArt.v3(3.4, 88.4, -11.5), SpellArt.v3(3.8, 84.6, -11.9), 1.2, 1.0, horn(50)).density(4);
		// Heaven's crown: rays of gold light fanning up off heaven's half of the brow.
		SpellArt.Bone crown = m.bone("crown", "head", -5, 104, -2);
		for (int k = 0; k < 5; k++) {
			SpellArt.blade(crown, SpellArt.v3(-1.5 - k * 2, 104.5, -5 + k * 1.4), 11 - k, 4, -6, 90, 8 + k * 10, heavenFlame(1.6, 60 + k)).density(2.5);
		}
		// The demon's mane: crimson fire streaming back off its half of the head.
		SpellArt.Bone mane = m.bone("mane", "head", 5, 102, 4);
		for (int k = 0; k < 6; k++) {
			SpellArt.blade(mane, SpellArt.v3(3 + (k % 3) * 2.6, 104 - (k / 3) * 5, -3 + k * 2.2), 15, 7, -58 - k * 5, 0, -8 - k * 5, demonFlame(2.4, 70 + k)).density(2);
		}
		// Its horn, rising from the demon's brow and sweeping back.
		String up = "head";
		double[] at = SpellArt.v3(6.5, 104, -3);
		double[][] dirs = {{2, 7, 1}, {2.4, 6, 3.4}, {1.4, 4, 5}};
		for (int k = 0; k < dirs.length; k++) {
			SpellArt.Bone b = m.bone("horn_" + k, up, at[0], at[1], at[2]);
			double[] next = SpellArt.add(at, dirs[k]);
			SpellArt.limb(b, at, SpellArt.add(next, SpellArt.times(dirs[k], 0.06)), 4.2 - k * 1.2, 4.2 - k * 1.2, horn(55 + k)).density(2.5);
			up = b.name;
			at = next;
		}
		SpellArt.Bone halo = m.bone("halo", "head", 0, 98, 13);
		halo.box(0, 98, 13, 66, 66, 0.1, halo(80)).only(SpellArt.Face.FRONT, SpellArt.Face.BACK).density(1.6);
		SpellArt.Bone rays = m.bone("halo_rays", "halo", 0, 98, 13.6);
		rays.box(0, 98, 13.6, 84, 84, 0.1, rays()).only(SpellArt.Face.FRONT, SpellArt.Face.BACK).density(1.2);
	}

	static final double[] SHOULDER_H = {-28, 70, 0}, ELBOW_H = {-37, 50, -10}, WRIST_H = {-26, 52, -34};

	static void arms(SpellArt.Model m) {
		// Heaven's arm: gold, its palm held up open ahead (-z), fingers up, a sun of light in its hollow.
		SpellArt.Bone arm = m.bone("arm_h", "body", SHOULDER_H[0], SHOULDER_H[1], SHOULDER_H[2]);
		SpellArt.limb(arm, SHOULDER_H, ELBOW_H, 13, 13, heavenly(30));
		SpellArt.Bone low = m.bone("arm_h_low", "arm_h", ELBOW_H[0], ELBOW_H[1], ELBOW_H[2]);
		SpellArt.limb(low, ELBOW_H, WRIST_H, 11, 11, heavenly(31));
		SpellArt.limb(low, SpellArt.add(WRIST_H, SpellArt.v3(-2, -0.4, 6)), SpellArt.add(WRIST_H, SpellArt.v3(0, 0, 1)), 13, 13, t -> SpellArt.glow(t.py > 52 ? GOLD_PALE : GOLD_BRIGHT, 0.95));
		SpellArt.Bone hand = m.bone("hand_h", "arm_h_low", WRIST_H[0], WRIST_H[1], WRIST_H[2]);
		hand.box(-26, 57.5, -36, 12, 12, 4, palm()).density(2.5);
		for (int k = 0; k < 4; k++) {
			double x = -30.6 + k * 3.05, len = 7.5 - Math.abs(k - 1.6) * 1.2;
			SpellArt.limb(hand, SpellArt.v3(x, 63, -36), SpellArt.v3(x - (k - 1.5) * 0.5, 63 + len, -36.8), 2.7, 2.7, heavenly(33 + k)).density(2.5);
		}
		SpellArt.limb(hand, SpellArt.v3(-20.6, 55, -36), SpellArt.v3(-17.2, 60.4, -37.4), 2.9, 2.9, heavenly(37)).density(2.5);
		// The demon's arm: crimson sinew, its hand clawed, talons of horn curled ahead.
		double[] shoulder = SpellArt.v3(28, 70, 0), elbow = SpellArt.v3(37, 50, -10), wrist = SpellArt.v3(26, 52, -34);
		SpellArt.Bone darm = m.bone("arm_d", "body", shoulder[0], shoulder[1], shoulder[2]);
		SpellArt.limb(darm, shoulder, elbow, 13, 13, demonic(40));
		for (int k = 0; k < 3; k++) SpellArt.limb(darm, SpellArt.v3(34 + k, 63 - k * 4.6, -3 - k * 2), SpellArt.v3(40 + k * 1.5, 64 - k * 4.6, -2 - k * 2), 1.6, 1.6, horn(44 + k)).density(3);
		SpellArt.Bone dlow = m.bone("arm_d_low", "arm_d", elbow[0], elbow[1], elbow[2]);
		SpellArt.limb(dlow, elbow, wrist, 11, 11, demonic(41));
		SpellArt.Bone dhand = m.bone("hand_d", "arm_d_low", wrist[0], wrist[1], wrist[2]);
		dhand.box(26, 55.5, -36.5, 12, 10, 5, demonic(42)).density(2);
		for (int k = 0; k < 4; k++) {
			double x = 21.4 + k * 3.05;
			double[] knuckle = SpellArt.v3(x, 59, -38.5), joint = SpellArt.v3(x + (k - 1.5) * 0.4, 61.5, -44), tip = SpellArt.v3(x + (k - 1.5) * 0.7, 57.5, -51 + Math.abs(k - 1.5));
			SpellArt.limb(dhand, knuckle, joint, 2.6, 2.6, demonic(45 + k)).density(2.5);
			SpellArt.limb(dhand, joint, tip, 1.5, 1.2, horn(48 + k)).density(4);
		}
		SpellArt.limb(dhand, SpellArt.v3(31.5, 54, -36.5), SpellArt.v3(35, 57, -42), 2.6, 2.6, demonic(49)).density(2.5);
		SpellArt.limb(dhand, SpellArt.v3(35, 57, -42), SpellArt.v3(35.5, 54, -47.5), 1.4, 1.2, horn(52)).density(4);
	}

	/** Heaven's protector-mirror on the breastplate (its face ahead): polished white gold, a ring at its rim, light shining off its middle. */
	static SpellArt.Painter mirror() {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return SpellArt.glow(GOLD, 0.95);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98) return SpellArt.glow(GOLD_DEEP, 0.95);
			if (r > 0.8) return SpellArt.glow(GOLD_BRIGHT, 0.97);
			return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_PALE, Math.hypot(x + 0.3, y - 0.3)), 0.98);
		};
	}

	/** Heaven's palm (its broad faces ahead and behind): gold, its hollow (on the front) a sun of white light. */
	static SpellArt.Painter palm() {
		SpellArt.Painter gold = heavenly(32);
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return gold.paint(t);
			double x = (t.across() - 0.5) * 2, y = (t.up() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.7) return gold.paint(t);
			double ray = Math.pow(Math.abs(Math.cos(Math.atan2(y, x) * 4)), 4);
			return SpellArt.glow(SpellArt.mix(WHITE_LIGHT, GOLD_BRIGHT, r / 0.7 * (1 - 0.6 * ray)), 0.98);
		};
	}

	/** The rising: it lifts up out of its cloud, bowed, its arms coming up; its halo blazes out behind it. */
	static void rise(SpellArt.Model m) {
		double L = 1.5;
		SpellArt.Anim a = m.anim("rise", L, null);
		a.sample("body", "position", 0, 1.1, 8, t -> new double[] {0, -80 * (1 - SpellArt.easeOut(t / 1.1)), 0});
		a.pos("body", L, 0, 0, 0);
		a.sample("body", "rotation", 0, 1.2, 6, t -> new double[] {18 * (1 - SpellArt.ease(t / 1.2)), 0, 0});
		a.rot("body", L, 0, 0, 0);
		a.sample("head", "rotation", 0, 1.3, 6, t -> new double[] {22 * (1 - SpellArt.ease(t / 1.3)), 0, 0});
		a.rot("head", L, 0, 0, 0);
		a.sample("cloud", "scale", 0, 0.7, 5, t -> new double[] {0.3 + 0.7 * SpellArt.easeOut(t / 0.7), 0.5 + 0.5 * SpellArt.easeOut(t / 0.7), 0.3 + 0.7 * SpellArt.easeOut(t / 0.7)});
		a.scale("cloud", L, 1);
		a.scale("halo", 0, 0.01).scale("halo", 0.8, 0.01).scale("halo", 1.2, 1.12).scale("halo", 1.4, 1).scale("halo", L, 1);
		for (String arm : new String[] {"arm_h", "arm_d"}) {
			double s = arm.equals("arm_h") ? 1 : -1;
			a.sample(arm, "rotation", 0, 1.4, 6, t -> new double[] {30 * (1 - SpellArt.ease(t / 1.4)), 0, s * 25 * (1 - SpellArt.ease(t / 1.4))});
			a.rot(arm, L, 0, 0, 0);
		}
	}

	/**
	 * Its looming, looped without a seam: breathing, swaying a little, its head turning; the halo's rays wheeling one ray round, the cloud's
	 * rings each one billow round, their flames and its mane flickering; its arms stirring.
	 */
	static void loom(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("loom", T, "true");
		a.sample("body", "position", 0, T, 12, t -> new double[] {0, 1.2 * Math.sin(2 * Math.PI * t / T), 0});
		a.sample("body", "rotation", 0, T, 12, t -> new double[] {1.2 * Math.sin(2 * Math.PI * t / T + 1), 1.6 * Math.sin(2 * Math.PI * t / T), 0});
		a.sample("head", "rotation", 0, T, 12, t -> new double[] {2 * Math.sin(2 * Math.PI * t / T + 2), 4 * Math.sin(2 * Math.PI * t / T + 0.5), 1.5 * Math.sin(2 * Math.PI * t / T)});
		a.sample("halo", "scale", 0, T, 12, t -> new double[] {1 + 0.025 * Math.sin(4 * Math.PI * t / T), 1 + 0.025 * Math.sin(4 * Math.PI * t / T), 1});
		a.sample("halo_rays", "rotation", 0, T, 2, t -> new double[] {0, 0, 30 * t / T});
		double[] billows = {10, 12, 8};
		for (int r = 0; r < 3; r++) {
			double turn = (r % 2 == 0 ? 1 : -1) * 360.0 / billows[r];
			a.sample("cloud_" + r, "rotation", 0, T, 2, t -> new double[] {0, turn * t / T, 0});
		}
		a.sample("cloud_flames", "scale", 0, T, 12, t -> new double[] {1 + 0.04 * Math.sin(4 * Math.PI * t / T), 0.88 + 0.16 * Math.sin(6 * Math.PI * t / T), 1 + 0.04 * Math.sin(4 * Math.PI * t / T + 1)});
		a.sample("mane", "scale", 0, T, 12, t -> new double[] {1, 0.9 + 0.14 * Math.sin(6 * Math.PI * t / T + 1), 1 + 0.06 * Math.sin(4 * Math.PI * t / T)});
		a.sample("crown", "scale", 0, T, 12, t -> new double[] {1, 0.92 + 0.1 * Math.sin(4 * Math.PI * t / T + 2), 1});
		a.sample("arm_h", "rotation", 0, T, 12, t -> new double[] {2.5 * Math.sin(2 * Math.PI * t / T + 0.3), 0, 1.5 * Math.sin(2 * Math.PI * t / T + 1.2)});
		a.sample("arm_d", "rotation", 0, T, 12, t -> new double[] {2.5 * Math.sin(2 * Math.PI * t / T + 1.9), 0, -1.5 * Math.sin(2 * Math.PI * t / T + 2.6)});
		a.sample("hand_d", "rotation", 0, T, 12, t -> new double[] {4 * Math.sin(2 * Math.PI * t / T + 1), 0, 0});
	}

	/** Heaven's palm: it rears back, raising its palm, then thrusts it out and down, pressing (its palm comes down out of the sky as it does). */
	static void strikePalm(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("strike_palm", 1.4, null);
		// (A rotation of -x raises the arm forward and up, of +x lowers it.)
		a.rot("arm_h", 0, 0, 0, 0).rot("arm_h", 0.35, -105, 8, -14).rot("arm_h", 0.7, -58, 0, 4).rot("arm_h", 1.0, -55, 0, 4);
		// (The elbow and wrist, which its loom leaves be, brought home by its end themselves.)
		a.rot("arm_h_low", 0, 0, 0, 0).rot("arm_h_low", 0.35, -25, 0, 0).rot("arm_h_low", 0.7, 42, 0, 0).rot("arm_h_low", 1.0, 40, 0, 0).rot("arm_h_low", 1.4, 0, 0, 0);
		a.rot("hand_h", 0, 0, 0, 0).rot("hand_h", 0.35, -15, 0, 0).rot("hand_h", 0.7, 48, 0, 0).rot("hand_h", 1.0, 45, 0, 0).rot("hand_h", 1.4, 0, 0, 0);
		a.rot("body", 0, 0, 0, 0).rot("body", 0.35, -6, -8, 0).rot("body", 0.7, 9, 6, 0).rot("body", 1.0, 8, 5, 0);
		a.rot("head", 0, 0, 0, 0).rot("head", 0.35, -6, -4, 0).rot("head", 0.7, 8, 4, 0).rot("head", 1.0, 7, 3, 0);
	}

	/** The demon's claw: it rears up, its claw raised high, roaring, then rakes it down and across. */
	static void strikeClaw(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("strike_claw", 1.4, null);
		a.rot("arm_d", 0, 0, 0, 0).rot("arm_d", 0.35, -95, -10, 24).rot("arm_d", 0.7, 26, 18, -8).rot("arm_d", 1.0, 24, 16, -8);
		a.rot("arm_d_low", 0, 0, 0, 0).rot("arm_d_low", 0.35, -25, 0, 0).rot("arm_d_low", 0.7, 18, 0, 0).rot("arm_d_low", 1.0, 16, 0, 0).rot("arm_d_low", 1.4, 0, 0, 0);
		a.rot("hand_d", 0, 0, 0, 0).rot("hand_d", 0.35, -20, 0, 0).rot("hand_d", 0.7, 40, 0, 0).rot("hand_d", 1.0, 38, 0, 0);
		a.rot("body", 0, 0, 0, 0).rot("body", 0.35, -8, 10, 0).rot("body", 0.7, 11, -10, 0).rot("body", 1.0, 10, -9, 0);
		a.rot("head", 0, 0, 0, 0).rot("head", 0.35, -14, 0, 0).rot("head", 0.7, 10, -6, 0).rot("head", 1.0, 9, -5, 0);
	}

	// --- Its hands come down ---

	/** How high above the ground a hand begins its fall (model pixels). */
	static final double DROP = 150;

	/**
	 * Heaven's palm coming down out of the sky, some twenty blocks of it, lying palm down on the ground at the origin, its fingers ahead (-z):
	 * the hand ("hand", fingers "finger_0".."finger_3", thumb "thumb") of gold light in plates, flames of light rising off its back, the arm of
	 * light it hangs from rising behind it into the sky ("light"); and under it heaven's seal on the ground ("seal").
	 */
	static void avatarPalm(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone hand = m.bone("hand", "root", 0, 4, 10);
		SpellArt.Painter gold = heavenly(90);
		hand.box(0, 4, 4, 40, 7, 30, gold);
		hand.box(0, 6, -10.5, 38, 5, 5, gold);
		hand.box(0, 6, 24, 26, 11, 12, gold);
		hand.box(0, 6, 24, 28, 12, 4, t -> SpellArt.glow(Math.abs(t.up() - 0.5) < 0.18 ? WHITE_LIGHT : GOLD_BRIGHT, 0.96)).density(2);
		double[] xs = {-15, -5, 5, 15}, lens = {20, 26, 27, 23};
		for (int k = 0; k < 4; k++) {
			double[] knuckle = SpellArt.v3(xs[k], 4.4, -11), mid = SpellArt.v3(xs[k] * 1.08, 4, -11 - lens[k] * 0.55), tip = SpellArt.v3(xs[k] * 1.14, 3.4, -11 - lens[k]);
			SpellArt.Bone f = m.bone("finger_" + k, "hand", knuckle[0], knuckle[1], knuckle[2]);
			SpellArt.limb(f, knuckle, mid, 8, 6.6, heavenly(91 + k));
			SpellArt.limb(f, mid, tip, 7, 5.8, heavenly(95 + k));
		}
		SpellArt.Bone thumb = m.bone("thumb", "hand", 20, 4.4, 8);
		SpellArt.limb(thumb, SpellArt.v3(20, 4.4, 8), SpellArt.v3(30, 3.8, -4), 8.6, 7, heavenly(99));
		SpellArt.limb(thumb, SpellArt.v3(30, 3.8, -4), SpellArt.v3(34, 3.2, -14), 7.4, 6, heavenly(100));
		// Flames of light rising off its back.
		for (int k = 0; k < 6; k++) {
			double x = -14 + k * 5.6, z = 14 - (k % 2) * 9;
			SpellArt.shared(m, hand, "palm_flame", false, SpellArt.v3(x, 7.4, z), 15, 8, -22, 0, 0, heavenFlame(2, 101), 1.6, false);
		}
		SpellArt.Bone light = m.bone("light", "hand", 0, 8, 28);
		SpellArt.limb(light, SpellArt.v3(0, 8, 28), SpellArt.v3(0, 140, 88), 22, 18, beam(HEAVEN)).density(1);
		SpellArt.Bone seal = m.bone("seal", "root", 0, 0.3, 0);
		SpellArt.sheet(seal, 0, 0.3, 0, 84, 84, 0, 0, 0, palmSeal(103)).density(1.5);
		handFall(m, true);
		handPress(m, true);
	}

	/**
	 * The demon's claw coming down out of the sky and raking: lying on the ground at the origin, its talons dug in ahead (-z): the hand ("hand",
	 * fingers "finger_0".."finger_3", thumb "thumb") of crimson sinew, its talons horn, flames of crimson qi rising off its back, the arm of qi it
	 * hangs from rising behind it into the sky ("light"); and the gouges it has torn in the ground ("gouges", growing from its back end).
	 */
	static void avatarClaw(SpellArt.Model m) {
		m.tpp = 1.25;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone hand = m.bone("hand", "root", 0, 6, 10);
		SpellArt.Painter sinew = demonic(110);
		hand.box(0, 7, 4, 38, 9, 30, sinew);
		hand.box(0, 6, 24, 24, 12, 12, sinew);
		// Knuckles spiked with horn.
		double[] xs = {-14.5, -4.8, 4.8, 14.5};
		for (int k = 0; k < 4; k++) {
			hand.box(xs[k], 11, -9, 7, 3, 6, sinew);
			SpellArt.limb(hand, SpellArt.v3(xs[k], 12, -8), SpellArt.v3(xs[k] * 1.04, 16.5, -5), 2.2, 2.2, horn(111 + k)).density(2.5);
		}
		for (int k = 0; k < 4; k++) {
			double[] knuckle = SpellArt.v3(xs[k], 7, -11), joint = SpellArt.v3(xs[k] * 1.06, 6.4, -24), tip = SpellArt.v3(xs[k] * 1.1, -2.5, -38 - Math.abs(k - 1.5) * 1.5);
			SpellArt.Bone f = m.bone("finger_" + k, "hand", knuckle[0], knuckle[1], knuckle[2]);
			SpellArt.limb(f, knuckle, joint, 7.4, 6.4, demonic(115 + k));
			SpellArt.limb(f, joint, tip, 4.2, 3.4, horn(119 + k)).density(2);
		}
		SpellArt.Bone thumb = m.bone("thumb", "hand", 19, 6, 10);
		SpellArt.limb(thumb, SpellArt.v3(19, 6, 10), SpellArt.v3(28, 5, -2), 8, 6.8, demonic(123));
		SpellArt.limb(thumb, SpellArt.v3(28, 5, -2), SpellArt.v3(31, -2, -14), 4, 3.2, horn(124)).density(2);
		for (int k = 0; k < 6; k++) {
			double x = -14 + k * 5.6, z = 14 - (k % 2) * 9;
			SpellArt.shared(m, hand, "claw_flame", false, SpellArt.v3(x, 11.4, z), 17, 9, -26, 0, 0, demonFlame(2.4, 125), 1.6, false);
		}
		SpellArt.Bone light = m.bone("light", "hand", 0, 8, 28);
		SpellArt.limb(light, SpellArt.v3(0, 8, 28), SpellArt.v3(0, 140, 88), 22, 18, beam(DEMON)).density(1);
		SpellArt.Bone gouges = m.bone("gouges", "root", 0, 0.3, -74);
		SpellArt.sheet(gouges, 0, 0.3, -57, 44, 34, 0, 0, 0, gouges()).density(1.5);
		handFall(m, false);
		handPress(m, false);
	}

	/**
	 * A hand's fall out of the sky ({@code palm}: heaven's, straight down, its fingers spreading as it lands, its seal flaring; else the demon's,
	 * coming down beyond its mark and raking back along the ground toward its caster, its talons tearing the gouges open ahead of where they
	 * stop). Lands at 0.65 s (0.5 s for the claw, which rakes until 0.68 s).
	 */
	static void handFall(SpellArt.Model m, boolean palm) {
		double L = 0.7, land = palm ? 0.65 : 0.5;
		SpellArt.Anim a = m.anim("fall", L, null);
		a.sample("root", "position", 0, land, 8, t -> {
			double f = t / land;
			return new double[] {0, DROP * (1 - f * f), palm ? 0 : -30 - 30 * (1 - f * f)};
		});
		if (palm) {
			a.pos("root", L, 0, 0, 0);
			a.sample("root", "rotation", 0, land, 5, t -> new double[] {-18 * (1 - SpellArt.ease(t / land)), 0, 0});
			a.rot("root", L, 0, 0, 0);
			a.scale("hand", 0, 1).scale("hand", land, 1).scale("hand", 0.68, 1.05, 0.88, 1.05).scale("hand", L, 1);
			for (int k = 0; k < 4; k++) a.rot("finger_" + k, 0, 6, 0, 0).rot("finger_" + k, land - 0.05, 6, 0, 0).rot("finger_" + k, land, 0, (k - 1.5) * 4, 0).rot("finger_" + k, L, 0, 0, 0);
			a.scale("seal", 0, 0.01).scale("seal", land - 0.01, 0.01).scale("seal", 0.68, 1.15).scale("seal", L, 1);
		} else {
			a.sample("root", "position", land, 0.68, 4, t -> new double[] {0, 0, -30 * (1 - SpellArt.easeOut((t - land) / 0.18))});
			a.pos("root", L, 0, 0, 0);
			a.sample("root", "rotation", 0, land, 5, t -> new double[] {-24 * (1 - SpellArt.ease(t / land)), 0, 0});
			a.rot("root", L, 0, 0, 0);
			for (int k = 0; k < 4; k++) a.rot("finger_" + k, 0, -20, 0, 0).rot("finger_" + k, land, 6, 0, 0).rot("finger_" + k, 0.68, 0, 0, 0).rot("finger_" + k, L, 0, 0, 0);
			a.sample("gouges", "scale", 0, L, 7, t -> new double[] {1, 1, Math.max(0.01, t < land ? 0 : SpellArt.easeOut((t - land) / 0.18))});
		}
	}

	/** A fallen hand pressing on, looped without a seam: the hand pulsing as it presses, its flames flickering, the light above it wavering; its seal or its gouges throbbing. */
	static void handPress(SpellArt.Model m, boolean palm) {
		double T = 1.3;
		SpellArt.Anim a = m.anim("press", T, "true");
		a.sample("hand", "scale", 0, T, 8, t -> new double[] {1 + 0.015 * Math.sin(2 * Math.PI * t / T), 1 - 0.02 * Math.sin(2 * Math.PI * t / T), 1 + 0.015 * Math.sin(2 * Math.PI * t / T)});
		a.sample("light", "scale", 0, T, 8, t -> new double[] {1 + 0.05 * Math.sin(4 * Math.PI * t / T), 1, 1 + 0.05 * Math.sin(4 * Math.PI * t / T + 1)});
		a.sample(palm ? "seal" : "gouges", "scale", 0, T, 8, t -> new double[] {1 + 0.03 * Math.sin(2 * Math.PI * t / T), 1, 1 + 0.03 * Math.sin(2 * Math.PI * t / T)});
	}
}
