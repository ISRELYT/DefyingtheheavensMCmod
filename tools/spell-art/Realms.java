/**
 * The secret realms' guardians (2026-10-31, SecretRealms: "guardians made of the realm's own material, the way your spell beasts are made of
 * their element"), written as the spells' shapes are (SpellArt: GeckoLib models, animations, textures) into geo/realm, animations/realm and
 * textures/entity/realm by {@code java SpellArt.java --realm <assets>}. Each has four animations the game asks for by name (RealmGuardian):
 * "dormant" (asleep at its post), "idle", "walk" and "strike"; its head bone "head" turned by the renderer to its look.
 */
final class Realms {
	// --- Colours ---

	static final int BRONZE_DARK = 0x4A2E14, BRONZE = 0x8C5A28, BRONZE_LIGHT = 0xC8904C, VERDIGRIS = 0x4E9A84, VERDIGRIS_LIGHT = 0x86C8B0;
	static final int JADE_GLOW = 0x7CFFC8, JADE_CORE = 0xE4FFF2;

	static double wave(double t, double T, double k, double phase) { return Math.sin(2 * Math.PI * k * t / T + phase); }

	// --- The Vine Keeper ---

	/**
	 * The Verdant Herb Valley's Vine Keeper, of its old wood and living vines (Wood's painters: its bark plates, its vines' jade nodes, its
	 * leaves): some two and a quarter blocks tall, stooped; a trunk for a body, leafy shoulders and a back grown over with moss and leaves,
	 * vines hanging from them; a head of knotted wood with two eyes of jade light and branches for horns; long arms of wood ending in claws of
	 * root, vines trailing from them; legs of root spreading into root feet.
	 */
	static void vineKeeper(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter bark = Wood.woodLimb(7101), head = Wood.woodHead(7103), vine = Wood.vine(7105), leaves = Wood.foliage(7107);
		SpellArt.Painter eye = t -> SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, 0.5));
		SpellArt.Bone body = m.bone("body", "root", 0, 14, 0);
		body.box(0, 20.5, 0.5, 10, 13, 8.5, bark);
		body.box(0, 28.5, 1, 13, 5, 9.5, bark);
		body.box(0, 16, 0.5, 9, 4, 7.5, bark);
		// Its back and shoulders grown over: leaves and moss.
		body.box(0, 31.8, 2.4, 14, 3.4, 8.6, leaves).density(2);
		body.box(-6.6, 30.2, 0.8, 4.4, 4.4, 6.4, leaves).density(2);
		body.box(6.6, 30.2, 0.8, 4.4, 4.4, 6.4, leaves).density(2);
		body.box(0, 26, 5.4, 9, 7, 2.2, leaves).density(2);
		// Vines hanging from its shoulders.
		double[][] hanging = {{-5.4, 25.6, -4.6, 9}, {-2.2, 26.4, -4.9, 6}, {3, 25.2, -4.8, 10}, {5.8, 26.6, -4.2, 7}, {0, 22, 5.6, 9}};
		for (double[] h : hanging) body.box(h[0], h[1] - h[3] / 2 + 3, h[2], 0.9, h[3], 0.9, vine).density(4);
		SpellArt.Bone headBone = m.bone("head", "body", 0, 31, -2.5);
		headBone.box(0, 34.6, -4.6, 7.6, 6.6, 6.8, head);
		headBone.box(0, 32.2, -8.2, 5, 2.2, 1.2, head);
		for (int s = -1; s <= 1; s += 2) {
			headBone.box(s * 1.8, 35.2, -8.1, 1.6, 1.6, 0.6, eye).density(4);
			// Branches for horns, angled up and out.
			SpellArt.limb(headBone, SpellArt.v3(s * 2.4, 37.6, -4), SpellArt.v3(s * 5.2, 42.5, -2.2), 1.3, 1.3, bark).density(4);
			SpellArt.limb(headBone, SpellArt.v3(s * 4.2, 40.4, -2.8), SpellArt.v3(s * 6.8, 42, -4.2), 0.9, 0.9, bark).density(4);
			String w = "arm_" + (s > 0 ? "l" : "r");
			SpellArt.Bone arm = m.bone(w, "body", s * 7, 28.5, -0.5);
			SpellArt.limb(arm, SpellArt.v3(s * 7.4, 28.5, -0.5), SpellArt.v3(s * 9.8, 19.4, -1.8), 3.6, 3.6, bark);
			arm.box(s * 9.2, 26, -0.4, 0.8, 8, 0.8, vine).density(4);
			SpellArt.Bone low = m.bone(w + "_low", w, s * 9.8, 19.4, -1.8);
			SpellArt.limb(low, SpellArt.v3(s * 9.8, 19.4, -1.8), SpellArt.v3(s * 10.4, 9.6, -4.2), 2.8, 2.8, bark);
			for (int f = -1; f <= 1; f++) {
				SpellArt.limb(low, SpellArt.v3(s * 10.4 + f * 0.9, 9.8, -4.2), SpellArt.v3(s * 10.6 + f * 1.6, 5.4, -5.6 - Math.abs(f) * 0.4), 0.9, 0.9,
						Wood.rootClaw(-1)).density(4);
			}
			low.box(s * 11.2, 13, -2.8, 0.8, 7, 0.8, vine).density(4);
			String l = "leg_" + (s > 0 ? "l" : "r");
			SpellArt.Bone leg = m.bone(l, "root", s * 3.4, 14, 0.5);
			SpellArt.limb(leg, SpellArt.v3(s * 3.4, 14, 0.5), SpellArt.v3(s * 4.6, 2.2, 0.6), 4.4, 4.4, bark);
			leg.box(s * 4.8, 1.2, -0.8, 5, 2.4, 6.4, bark);
			for (int f = -1; f <= 1; f++) {
				SpellArt.limb(leg, SpellArt.v3(s * 4.8 + f * 1.6, 1, -3.6), SpellArt.v3(s * 4.8 + f * 2.6, 0.4, -6.2), 1, 1, Wood.rootClaw(1)).density(4);
			}
		}
		keeperDormant(m);
		keeperIdle(m);
		keeperWalk(m);
		keeperStrike(m);
	}

	/** Asleep by its bed: sunk into its roots, bowed over, arms folded in, head down, a slow breath; like a stump grown over. */
	static void keeperDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("root", "position", 0, T, 8, t -> new double[] {0, -5, 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {38 + 1.6 * wave(t, T, 1, 0), 0, 0});
		a.sample("head", "rotation", 0, T, 8, t -> new double[] {28, 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 8, t -> new double[] {18, 0, -ss * 30});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 8, t -> new double[] {-55, 0, 0});
			a.sample("leg_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 8, t -> new double[] {-24, 0, ss * 8});
		}
	}

	/** Awake, standing: a slow sway, its arms hanging and swinging a little, its vines with them. */
	static void keeperIdle(SpellArt.Model m) {
		double T = 3.0;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {9 + 2 * wave(t, T, 1, 0), 0, 1.5 * wave(t, T, 1, 1.2)});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {-6 + 2 * wave(t, T, 1, 0.6), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {-6 + 4 * wave(t, T, 1, ss), 0, -ss * 6});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 16, t -> new double[] {-14 + 3 * wave(t, T, 1, ss + 0.5), 0, 0});
		}
	}

	/** Walking: its root legs striding, its arms swinging against them, its body rocking. */
	static void keeperWalk(SpellArt.Model m) {
		double T = 1.2;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.6 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {12, 4 * wave(t, T, 1, 0), 2 * wave(t, T, 1, 0)});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("leg_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {24 * ss * wave(t, T, 1, 0), 0, 0});
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {-18 * ss * wave(t, T, 1, 0) - 6, 0, -ss * 6});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 16, t -> new double[] {-18, 0, 0});
		}
	}

	/** Its blow: both arms of wood drawn back and up, then lashed down and forward together, the body leaning into it. */
	static void keeperStrike(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "rotation", 0, L, 12, t -> new double[] {12 + 18 * strike(t / L), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, L, 12, t -> new double[] {-150 * windUp(t / L) + 40 * strike(t / L), 0, -ss * (8 + 10 * windUp(t / L))});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, L, 12, t -> new double[] {-30 * windUp(t / L) - 10 * strike(t / L), 0, 0});
		}
	}

	/** A blow's drawing back (0 to 1 by 40%, away again by 60%) and its fall (0 to 1 from 40% to 60%, away by its end). */
	static double windUp(double u) {
		return u < 0.4 ? SpellArt.ease(u / 0.4) : u < 0.6 ? 1 - SpellArt.ease((u - 0.4) / 0.2) : 0;
	}

	static double strike(double u) {
		return u < 0.4 ? 0 : u < 0.6 ? SpellArt.easeOut((u - 0.4) / 0.2) : 1 - SpellArt.ease((u - 0.6) / 0.4);
	}

	// --- The Bronze Puppet ---

	/** Old bronze: dark and burnished by turns, its edges worn bright, rivets in rows, green rust (verdigris) creeping over it in patches. */
	static SpellArt.Painter bronze(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.32 + seed, t.py * 0.32 + t.pz * 0.27, seed, 3);
			double patina = SpellArt.smooth(0.5, 0.72, SpellArt.fbm(t.px * 0.15 + 7, t.py * 0.15 - t.pz * 0.17, seed + 5, 3));
			int c = SpellArt.mix(BRONZE_DARK, BRONZE, 0.25 + 0.75 * n);
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.07) c = SpellArt.mix(c, BRONZE_LIGHT, 0.55);
			// Rivets along its plates' edges.
			if (Math.min(ex, ey) < 0.16 && Math.min(ex, ey) > 0.09 && (int) Math.floor((t.fx + t.fy) * 9) % 3 == 0) c = SpellArt.mix(c, BRONZE_LIGHT, 0.7);
			return SpellArt.col(SpellArt.mix(c, SpellArt.mix(VERDIGRIS, VERDIGRIS_LIGHT, n), patina * 0.85));
		};
	}

	/** Jade light (its core, its visor): glowing, brighter at its middle. */
	static SpellArt.Painter jadeLight() {
		return t -> {
			double d = Math.hypot(t.fx - 0.5, t.fy - 0.5);
			return SpellArt.glow(SpellArt.mix(JADE_CORE, JADE_GLOW, Math.min(1, d * 2.2)));
		};
	}

	/**
	 * An alchemists' guard puppet of bronze (the Verdant Herb Valley's ruins; the Gatekeeper before the Pill Sage's hall): some two and a half
	 * blocks tall, broad; a barrel chest with a disc of jade light set in it (its core), great pauldrons, a helmet-head with a slit of jade light
	 * for its eyes and a crest; heavy forearms ending in gauntlets; thick legs on broad feet. Green rust over all.
	 */
	static void bronzePuppet(SpellArt.Model m) {
		puppet(m, bronze(7201), bronze(7203));
	}

	static final int JADE_WHITE = 0xEEF0E4, JADE_WHITE_SHADE = 0xC4D2BE, JADE_CLOUD = 0x96C6A2, GILT = 0xE0B448, GILT_LIGHT = 0xFFE6A0, GILT_DARK = 0x94701E;

	/** White jade: milky and cool, a faint green cloud drifting through it, its edges catching the light. */
	static SpellArt.Painter whiteJade(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.28 + seed, t.py * 0.28 + t.pz * 0.23, seed, 3);
			double cloud = SpellArt.smooth(0.45, 0.75, SpellArt.fbm(t.px * 0.12 + 3, t.py * 0.12 - t.pz * 0.14, seed + 7, 3));
			int c = SpellArt.mix(SpellArt.mix(JADE_WHITE_SHADE, JADE_WHITE, 0.35 + 0.65 * n), JADE_CLOUD, cloud * 0.4);
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.07) c = SpellArt.mix(c, 0xFFFFFF, 0.45);
			return SpellArt.col(c);
		};
	}

	/** Gold leaf over its plates: bright and dark by turns, worn bright at the edges, a line of chasing within them. */
	static SpellArt.Painter gilt(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.3 + seed, t.py * 0.3 + t.pz * 0.25, seed, 3);
			int c = SpellArt.mix(GILT_DARK, GILT, 0.3 + 0.7 * n);
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.08) c = SpellArt.mix(c, GILT_LIGHT, 0.6);
			else if (Math.min(ex, ey) < 0.17 && Math.min(ex, ey) > 0.12) c = SpellArt.mix(c, GILT_DARK, 0.6);
			return SpellArt.col(c);
		};
	}

	/**
	 * The Cave Abode master's own puppet (before his chamber, its last trial): the bronze guards' make carved in white jade, its plates gilded,
	 * its core and visor the same jade light.
	 */
	static void masterPuppet(SpellArt.Model m) {
		puppet(m, whiteJade(7301), gilt(7303));
	}

	/** A guard puppet's make (bronzePuppet's), of {@code metal} with {@code plate} over it, and its animations. */
	static void puppet(SpellArt.Model m, SpellArt.Painter metal, SpellArt.Painter plate) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter light = jadeLight();
		SpellArt.Bone waist = m.bone("waist", "root", 0, 17, 0);
		waist.box(0, 17.4, 0, 9, 4, 6.4, plate);
		waist.box(0, 14.6, -3.4, 6, 4, 0.8, plate); // its tasset, front
		SpellArt.Bone body = m.bone("body", "waist", 0, 19, 0);
		body.box(0, 25.4, 0, 12, 11, 8, metal);
		body.box(0, 26, -4.3, 9, 7, 0.8, plate);
		body.box(0, 26, -4.9, 3.4, 3.4, 0.6, light).density(4);
		body.box(0, 31.4, 0, 8, 1.8, 6.4, plate); // its collar
		for (int s = -1; s <= 1; s += 2) body.box(s * 7.6, 30, 0, 5, 4, 7.4, plate).rot(0, 0, -s * 14);
		SpellArt.Bone head = m.bone("head", "body", 0, 32, 0);
		head.box(0, 35.6, 0, 7, 7, 7, metal);
		head.box(0, 35.8, -3.65, 5, 1.2, 0.5, light).density(4);
		head.box(0, 39.8, 0.6, 1.2, 2.6, 5.4, plate); // its crest
		head.box(0, 33, -3.7, 4.6, 1.4, 0.6, plate); // its jaw guard
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			SpellArt.Bone arm = m.bone(w, "body", s * 7.6, 29.5, 0);
			SpellArt.limb(arm, SpellArt.v3(s * 8, 29.5, 0), SpellArt.v3(s * 9, 21.2, 0), 3.8, 3.8, metal);
			SpellArt.Bone low = m.bone(w + "_low", w, s * 9, 21.2, 0);
			SpellArt.limb(low, SpellArt.v3(s * 9, 21.2, 0), SpellArt.v3(s * 9.4, 13.4, -1), 4.4, 4.4, plate);
			low.box(s * 9.4, 11.6, -1.2, 4.8, 4, 4.8, metal);
			String l = "leg_" + (s > 0 ? "l" : "r");
			SpellArt.Bone leg = m.bone(l, "root", s * 2.8, 16, 0);
			SpellArt.limb(leg, SpellArt.v3(s * 2.8, 16, 0), SpellArt.v3(s * 3.2, 8.6, 0), 4.2, 4.6, plate);
			SpellArt.Bone shin = m.bone(l + "_low", l, s * 3.2, 8.6, 0);
			SpellArt.limb(shin, SpellArt.v3(s * 3.2, 8.6, 0), SpellArt.v3(s * 3.4, 2, 0), 4.4, 4.6, metal);
			shin.box(s * 3.4, 1, -1, 4.8, 2, 6.8, plate);
		}
		puppetDormant(m);
		puppetIdle(m);
		puppetWalk(m);
		puppetStrike(m);
	}

	/** Still as a statue at its post: its head bowed, its arms at its sides, its fists closed on nothing. */
	static void puppetDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {22, 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 4, t -> new double[] {0, 0, -ss * 4});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 4, t -> new double[] {-6, 0, 0});
		}
	}

	/** Awake, standing guard: a slow mechanical breath, its head turning a little. */
	static void puppetIdle(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "rotation", 0, T, 12, t -> new double[] {1.5 * wave(t, T, 1, 0), 2 * wave(t, T, 1, 1.4), 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 12, t -> new double[] {-4 + 2 * wave(t, T, 1, ss), 0, -ss * 6});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 12, t -> new double[] {-20, 0, 0});
		}
	}

	/** Its stiff stride: legs swinging from the hip and bending at the knee, arms against them, a heavy bob. */
	static void puppetWalk(SpellArt.Model m) {
		double T = 1.4;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.5 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {4, 5 * wave(t, T, 1, 0), 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("leg_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {20 * ss * wave(t, T, 1, 0), 0, 0});
			a.sample("leg_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 16, t -> new double[] {Math.max(0, 22 * ss * wave(t, T, 1, -1.2)), 0, 0});
			a.sample("arm_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {-14 * ss * wave(t, T, 1, 0), 0, -ss * 6});
			a.sample("arm_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 16, t -> new double[] {-22, 0, 0});
		}
	}

	/** Its blow: its right fist raised high over its head, the body twisting back, then brought down hard. */
	static void puppetStrike(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "rotation", 0, L, 12, t -> new double[] {10 * strike(t / L), -16 * windUp(t / L) + 10 * strike(t / L), 0});
		a.sample("arm_r", "rotation", 0, L, 12, t -> new double[] {-165 * windUp(t / L) + 25 * strike(t / L), 0, 10 * windUp(t / L)});
		a.sample("arm_r_low", "rotation", 0, L, 12, t -> new double[] {-35 * windUp(t / L), 0, 0});
		a.sample("arm_l", "rotation", 0, L, 12, t -> new double[] {-20 * windUp(t / L), 0, 8});
	}

	// --- The Sword Wraith (the Sunken Sword Tomb's) ---

	static final int SPIRIT_LIGHT = 0xBFE6FF, SPIRIT_CORE = 0xF4FBFF, TASSEL = 0xB4232A, TASSEL_DARK = 0x6E1218;

	/** Spirit-light (a sword wraith's wisps, its eye): pale blue-white, brightest at its middle. */
	static SpellArt.Painter spiritLight() {
		return t -> SpellArt.glow(SpellArt.mix(SPIRIT_CORE, SPIRIT_LIGHT, Math.min(1, Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2.2)));
	}

	/** A silk tassel: deep red, its threads hanging in lines, darker in their folds. */
	static SpellArt.Painter tassel(int seed) {
		return t -> {
			double thread = 0.5 + 0.5 * Math.sin(t.fx * 22 + SpellArt.noise(t.fy * 3, seed, seed) * 2);
			return SpellArt.col(SpellArt.mix(TASSEL_DARK, TASSEL, 0.35 + 0.55 * thread));
		};
	}

	/**
	 * The Sunken Sword Tomb's Sword Wraith (the sword spirit of one of its fallen swords; the game's own "sword_spirit" is a spell's): an
	 * ancient jian of the tomb hanging point-down in the air, some two blocks long, a red silk tassel from its pommel, its spirit about it as
	 * wisps of pale light wound round the blade and a bright eye where the blade meets the guard. Its blade, guard, grip and pommel are Metal's
	 * (Myriad Swords Return's jian), larger. Asleep it is a blade driven into the ground, its spirit gone in (its "spirit" bone shrunk to nothing).
	 */
	static void swordWraith(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		// The sword built along +z (its tip there, as Metal paints a jian), its middle at the body's pivot, then stood point-down.
		SpellArt.Bone body = m.bone("body", "root", 0, 16, 0);
		double y = 16;
		SpellArt.Cube[] sword = {
				body.box(0, y, 4, 3.2, 0.7, 24, Metal.steel(31)).density(2),
				body.box(0, y, -8.6, 7.6, 1.6, 1.6, Metal.gold(32)).density(2),
				body.box(0, y, -12.2, 1.3, 1.3, 5.6, Metal.grip()).density(2),
				body.box(0, y, -15.6, 2, 2, 1.6, Metal.gold(33)).density(2)};
		for (SpellArt.Cube cube : sword) cube.rot(-90, 0, 0, 0, y, 0);
		// The tassel hanging from the pommel beside the grip.
		SpellArt.Bone tassel = m.bone("tassel", "body", 0, 32, 0);
		tassel.box(0.9, 29.6, 0, 0.4, 4.6, 0.4, tassel(35)).density(4);
		tassel.box(1, 26, 0, 1.4, 3.2, 1.4, tassel(36)).density(4);
		// Its spirit: an eye of light at the guard, wisps wound round the blade.
		SpellArt.Bone spirit = m.bone("spirit", "body", 0, 16, 0);
		spirit.box(0, 23.2, -0.9, 1.6, 1.6, 0.4, spiritLight()).density(4).rot(0, 0, 45);
		for (int k = 0; k < 3; k++) {
			double a = k * 120;
			SpellArt.limb(spirit, SpellArt.v3(Math.sin(Math.toRadians(a)) * 2.4, 22, Math.cos(Math.toRadians(a)) * 2.4),
					SpellArt.v3(Math.sin(Math.toRadians(a + 150)) * 1.6, 6, Math.cos(Math.toRadians(a + 150)) * 1.6), 0.4, 0.4, spiritLight()).density(4);
		}
		spiritDormant(m);
		spiritIdle(m);
		spiritWalk(m);
		spiritStrike(m);
	}

	/** Asleep: a blade driven into the ground at a lean, its spirit gone in. */
	static void spiritDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("body", "position", 0, T, 4, t -> new double[] {0, -9, 0});
		a.sample("body", "rotation", 0, T, 4, t -> new double[] {6, 0, 9});
		a.sample("spirit", "scale", 0, T, 4, t -> new double[] {0, 0, 0});
		a.sample("tassel", "rotation", 0, T, 8, t -> new double[] {0, 0, 4 + 3 * wave(t, T, 1, 0)});
	}

	/** Awake: hanging in the air, rising and falling a little, turning slowly; its wisps winding round it, its tassel swaying. */
	static void spiritIdle(SpellArt.Model m) {
		double T = 3.0;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "position", 0, T, 16, t -> new double[] {0, 2 + 0.9 * wave(t, T, 1, 0), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {3 * wave(t, T, 1, 0.8), 18 * wave(t, T, 1, 0), 3 * wave(t, T, 1, 2)});
		a.sample("spirit", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
		a.sample("spirit", "scale", 0, T, 16, t -> {
			double s = 1 + 0.08 * wave(t, T, 2, 0);
			return new double[] {s, s, s};
		});
		a.sample("tassel", "rotation", 0, T, 16, t -> new double[] {6 * wave(t, T, 1, 1), 0, 8 * wave(t, T, 1, 0)});
	}

	/** Gliding after a foe: leaned forward, point trailing, its tassel streaming back. */
	static void spiritWalk(SpellArt.Model m) {
		double T = 1.0;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("body", "position", 0, T, 16, t -> new double[] {0, 3 + 0.6 * wave(t, T, 1, 0), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {-32, 0, 4 * wave(t, T, 1, 0)});
		a.sample("spirit", "rotation", 0, T, 16, t -> new double[] {0, 720 * t / T, 0});
		a.sample("tassel", "rotation", 0, T, 16, t -> new double[] {38 + 6 * wave(t, T, 2, 0), 0, 0});
	}

	/** Its blow: drawn back, point swung up behind it, then swept through in a cut and on round, a flash along its edge. */
	static void spiritStrike(SpellArt.Model m) {
		double L = 0.8;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "position", 0, L, 12, t -> new double[] {0, 3 + 2 * windUp(t / L), -3 * strike(t / L)});
		a.sample("body", "rotation", 0, L, 12, t -> new double[] {70 * windUp(t / L) - 120 * strike(t / L), 0, 0});
		a.sample("spirit", "scale", 0, L, 12, t -> {
			double s = 1 + 0.4 * strike(t / L);
			return new double[] {s, s, s};
		});
	}

	// --- The Dragon's Echo (the Primeval Beast Graveyard's) ---

	static final int ECHO = 0xCDEFB4, ECHO_DARK = 0x4E8A58, ECHO_CORE = 0xF6FFE8, ECHO_EYE = 0xFFE27A;

	/** The echo's stuff: the dead dragon's light, scaled in rows, seen through (more where it is thinner), brighter where it is denser. */
	static SpellArt.Painter echoScales(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.45 + seed, t.py * 0.45 + t.pz * 0.45, seed, 3);
			double row = 0.5 + 0.5 * Math.sin(t.fx * 9 + Math.floor(t.fy * 6) * 1.7);
			return SpellArt.glow(SpellArt.mix(ECHO_DARK, ECHO, 0.3 + 0.45 * n + 0.25 * row), 0.4 + 0.3 * n);
		};
	}

	/** Its horns, claws and ridge: the light at its brightest. */
	static SpellArt.Painter echoBright() {
		return t -> SpellArt.glow(SpellArt.mix(ECHO, ECHO_CORE, t.up()));
	}

	/**
	 * The Primeval Beast Graveyard's Dragon's Echo (the trial's guardian, matched to its challenger): the dead primeval dragon risen as a shade
	 * of jade-gold light over its own bones, some five blocks from snout to tail, low on four clawed legs: a long horned head with whiskers and
	 * a jaw that opens, a ridge of spines down its back, its body in five lengths so it can wind as it goes (dormant, idle, walk, strike).
	 */
	static void dragonEcho(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter scales = echoScales(8101), bright = echoBright();
		SpellArt.Bone chest = m.bone("body0", "root", 0, 12, -8);
		chest.box(0, 12, -8, 9, 8, 12, scales);
		SpellArt.Bone neck = m.bone("neck", "body0", 0, 14, -14);
		neck.box(0, 17, -18, 6, 6, 8, scales).rot(-25, 0, 0, 0, 14, -14);
		SpellArt.Bone head = m.bone("head", "neck", 0, 19, -22);
		head.box(0, 20, -25.5, 7, 6, 8, scales);
		head.box(0, 18.8, -32, 5, 3.6, 6, scales); // its snout
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 3.6, 21.4, -27.5, 0.4, 1, 1.6, t -> SpellArt.glow(ECHO_EYE)).density(4); // its eyes
			SpellArt.limb(head, SpellArt.v3(s * 2.4, 23, -23), SpellArt.v3(s * 3.6, 27.6, -14.5), 1.1, 1.1, bright).density(2); // its horns
			SpellArt.limb(head, SpellArt.v3(s * 2.4, 18.4, -34), SpellArt.v3(s * 7.5, 16.2, -30.5), 0.4, 0.4, bright).density(4); // its whiskers
		}
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 17.4, -22.5);
		jaw.box(0, 16.4, -29.5, 4.6, 1.6, 9, scales);
		SpellArt.Bone b1 = m.bone("body1", "body0", 0, 12, -2);
		b1.box(0, 12, 4, 8, 7, 12, scales);
		SpellArt.Bone b2 = m.bone("body2", "body1", 0, 12, 10);
		b2.box(0, 12, 16, 7, 6, 12, scales);
		SpellArt.Bone t0 = m.bone("tail0", "body2", 0, 12, 22);
		t0.box(0, 12, 28, 5, 5, 12, scales);
		SpellArt.Bone t1 = m.bone("tail1", "tail0", 0, 12, 34);
		t1.box(0, 12, 39, 3.6, 3.6, 10, scales);
		SpellArt.Bone t2 = m.bone("tail2", "tail1", 0, 12, 44);
		t2.box(0, 12, 49, 2.4, 2.4, 10, scales);
		t2.box(0, 13.2, 54.5, 0.6, 5, 4, bright).density(4); // its tail's tuft
		// The ridge of spines down its back, each length its own.
		Object[][] ridge = {{chest, -12.0, 16.6}, {chest, -6.0, 16.6}, {b1, 0.0, 16.0}, {b1, 6.0, 16.0}, {b2, 12.0, 15.4}, {b2, 18.0, 15.4}, {t0, 25.0, 14.8},
				{t0, 31.0, 14.8}};
		for (Object[] spine : ridge) ((SpellArt.Bone) spine[0]).box(0, (double) spine[2], (double) spine[1], 0.6, 3, 2, bright).density(4).rot(-20, 0, 0);
		// Its legs: two under its chest, two under its hips, each ending in a three-toed claw.
		Object[][] legs = {{"leg_fl", "body0", 1, -9.0}, {"leg_fr", "body0", -1, -9.0}, {"leg_bl", "body2", 1, 16.0}, {"leg_br", "body2", -1, 16.0}};
		for (Object[] l : legs) {
			int s = (int) l[2];
			double z = (double) l[3];
			SpellArt.Bone leg = m.bone((String) l[0], (String) l[1], s * 4.5, 10, z);
			SpellArt.limb(leg, SpellArt.v3(s * 4.5, 10, z), SpellArt.v3(s * 6, 2, z - 1), 2.2, 2.2, scales);
			leg.box(s * 6, 0.9, z - 2, 2.8, 1.8, 3.4, scales);
			for (int toe = -1; toe <= 1; toe++) leg.box(s * 6 + toe * 0.9, 0.5, z - 4.2, 0.6, 1, 1.4, bright).density(4);
		}
		echoDormant(m);
		echoIdle(m);
		echoWalk(m);
		echoStrike(m);
	}

	/** Asleep in its bones: low on folded legs, its head laid down, its body curled to one side, the tip of its tail stirring. */
	static void echoDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("root", "position", 0, T, 4, t -> new double[] {0, -5, 0});
		a.sample("neck", "rotation", 0, T, 4, t -> new double[] {22, -12, 0});
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {20, -10, 0});
		a.sample("body1", "rotation", 0, T, 4, t -> new double[] {0, 22, 0});
		a.sample("body2", "rotation", 0, T, 4, t -> new double[] {0, 26, 0});
		a.sample("tail0", "rotation", 0, T, 4, t -> new double[] {0, 30, 0});
		a.sample("tail1", "rotation", 0, T, 8, t -> new double[] {0, 34, 0});
		a.sample("tail2", "rotation", 0, T, 8, t -> new double[] {0, 30 + 8 * wave(t, T, 1, 0), 0});
		for (String leg : new String[] {"leg_fl", "leg_fr", "leg_bl", "leg_br"}) a.sample(leg, "rotation", 0, T, 4, t -> new double[] {-70, 0, 0});
	}

	/** Awake: standing over its bones, its body winding a little, its head turning, its tail swaying. */
	static void echoIdle(SpellArt.Model m) {
		double T = 3.0;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.5 * wave(t, T, 1, 0), 0});
		a.sample("neck", "rotation", 0, T, 16, t -> new double[] {-4 + 3 * wave(t, T, 1, 0.6), 6 * wave(t, T, 1, 0), 0});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {2 * wave(t, T, 1, 1.4), 6 * wave(t, T, 1, 0.4), 0});
		a.sample("jaw", "rotation", 0, T, 16, t -> new double[] {4 + 3 * wave(t, T, 2, 0), 0, 0});
		String[] body = {"body1", "body2", "tail0", "tail1", "tail2"};
		for (int i = 0; i < body.length; i++) {
			double phase = -0.8 * i, amp = 4 + 2.5 * i;
			a.sample(body[i], "rotation", 0, T, 16, t -> new double[] {0, amp * wave(t, T, 1, phase), 0});
		}
	}

	/** Going after a foe: its body winding from side to side as a serpent's, its legs striding against it, its head held level and forward. */
	static void echoWalk(SpellArt.Model m) {
		double T = 1.2;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.6 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("neck", "rotation", 0, T, 16, t -> new double[] {-8, -8 * wave(t, T, 1, 0.8), 0});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {6, -6 * wave(t, T, 1, 1.4), 0});
		String[] body = {"body1", "body2", "tail0", "tail1", "tail2"};
		for (int i = 0; i < body.length; i++) {
			double phase = -1.0 * (i + 1), amp = 10 + 3 * i;
			a.sample(body[i], "rotation", 0, T, 16, t -> new double[] {0, amp * wave(t, T, 1, phase), 0});
		}
		a.sample("body0", "rotation", 0, T, 16, t -> new double[] {0, 7 * wave(t, T, 1, 0), 0});
		a.sample("leg_fl", "rotation", 0, T, 16, t -> new double[] {32 * wave(t, T, 1, 0), 0, 0});
		a.sample("leg_fr", "rotation", 0, T, 16, t -> new double[] {-32 * wave(t, T, 1, 0), 0, 0});
		a.sample("leg_bl", "rotation", 0, T, 16, t -> new double[] {-32 * wave(t, T, 1, 0), 0, 0});
		a.sample("leg_br", "rotation", 0, T, 16, t -> new double[] {32 * wave(t, T, 1, 0), 0, 0});
	}

	/** Its blow: its head drawn back and up on its neck, its jaws opening, then thrown forward to bite, its body lunging after. */
	static void echoStrike(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("root", "position", 0, L, 12, t -> new double[] {0, 0, -5 * strike(t / L)});
		a.sample("neck", "rotation", 0, L, 12, t -> new double[] {-32 * windUp(t / L) + 38 * strike(t / L), 0, 0});
		a.sample("head", "rotation", 0, L, 12, t -> new double[] {-18 * windUp(t / L) + 22 * strike(t / L), 0, 0});
		a.sample("jaw", "rotation", 0, L, 12, t -> new double[] {40 * windUp(t / L) - 20 * strike(t / L), 0, 0});
		a.sample("body1", "rotation", 0, L, 12, t -> new double[] {0, 0, 0});
	}

	// --- The Bone Soldier (the Ancient Battlefield's), the Battle Spirit, the General's Spirit ---

	static final int BONE = 0xDDD5BA, BONE_DARK = 0x9C9278, IRON = 0x3C3B40, RUST = 0x8E4A22, RUST_LIGHT = 0xB86A30, CLOTH = 0x8A2520, CLOTH_DARK = 0x4E1614;
	static final int GHOST = 0x9EE8D2, GHOST_DARK = 0x3E8A7A, GOLD = 0xD4A640, GOLD_DARK = 0x7A5A1C;
	/** The light of the old army the War General's Banner calls up: warm gold, the banner's side (never the field's pale blue-green). */
	static final int ARMY = 0xF6D890, ARMY_DARK = 0xA07A30, ARMY_CORE = 0xFFF6D8;

	/** The Yellow Springs' drowned: their murky gold-green light, its deep shade, the cold yellow of their eyes. */
	static final int DROWNED = 0xB8B45A, DROWNED_DARK = 0x3A4A26, DROWNED_EYE = 0xF4EE9A;
	/** The Jade Lake Mirage's shades: glassy silver light, its blue-grey shade, the pale cyan of their eyes. */
	static final int MIRROR = 0xE6F4FF, MIRROR_DARK = 0x7A8EA6, MIRROR_EYE = 0xC8FFFF;
	/** The fallen holy land's remnants: plates of white jade, its grey-green shade, its white cloth, the gold of their eyes. */
	static final int HOLY_JADE = 0xEEF2EC, HOLY_JADE_DARK = 0xB2C0B8, HOLY_CLOTH = 0xF2EEE2, HOLY_EYE = 0xFFE08A;
	/** The fallen immortal's guards: gold gone dull and brown, the green of old bronze on it, faded violet cloth, rotted bone, sickly green eyes. */
	static final int ROT_GOLD = 0x9A7A38, ROT_GOLD_DARK = 0x4E3A18, ROT_CLOTH = 0x6A5070, ROT_CLOTH_DARK = 0x2E2232, ROT_BONE = 0x8A8268,
			ROT_EYE = 0xB8FF7A;

	/**
	 * How a soldier of the old war is painted: the bones, its armour and cloth, as the dead lie (bone, rust), their spirits (ghost) or their
	 * general's (gold); the banner's army (army: spirits in gold light); the Yellow Springs Hollow's drowned (drowned: spirits in the springs'
	 * murky light); the Jade Lake Mirage's shades (mirror: spirits in glassy silver); the fallen holy land's remnants (holy: white jade and
	 * gold); the fallen immortal's guards and his own spirit (decayed, decayed lord: gold rotted, a cape on the lord).
	 */
	enum Soldier { BONE, GHOST, GENERAL, ARMY, DROWNED, MIRROR, HOLY, DECAYED, DECAYED_LORD }

	/** Whether it is a spirit drawn wholly in light: the field's own, the banner's army, the drowned, the mirror's shades. */
	static boolean spirit(Soldier kind) {
		return kind == Soldier.GHOST || kind == Soldier.ARMY || kind == Soldier.DROWNED || kind == Soldier.MIRROR;
	}

	/** Whether it is of the fallen immortal's, rotted with him. */
	static boolean decayed(Soldier kind) {
		return kind == Soldier.DECAYED || kind == Soldier.DECAYED_LORD;
	}

	/** Old bone: off-white yellowed with age, darker in its hollows, cracked here and there (the holy land's paler, the immortal's rotted). */
	static SpellArt.Painter bone(int seed, Soldier kind) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.5 + seed, t.py * 0.5 + t.pz * 0.4, seed, 3);
			int c;
			if (kind == Soldier.BONE) c = SpellArt.mix(BONE_DARK, BONE, 0.35 + 0.65 * n);
			else if (kind == Soldier.HOLY) c = SpellArt.mix(0xB4AC98, 0xEDE6D2, 0.35 + 0.65 * n);
			else if (decayed(kind)) c = SpellArt.mix(0x3E3628, ROT_BONE, 0.3 + 0.7 * n);
			else return ghost(kind, n, 0.5);
			if (Math.abs(SpellArt.noise(t.px * 1.3 + seed, t.py * 1.3, seed + 1) - 0.5) < 0.025) c = SpellArt.shade(c, -0.4); // a crack
			return SpellArt.col(c);
		};
	}

	/** Ribs: bars of bone across the chest, dark between them. */
	static SpellArt.Painter ribs(int seed, Soldier kind) {
		SpellArt.Painter bone = bone(seed, kind);
		boolean solid = kind == Soldier.BONE || kind == Soldier.HOLY || decayed(kind);
		return t -> {
			double r = t.up() * 5 - Math.floor(t.up() * 5);
			if (r < 0.35 && !t.flat()) return solid ? SpellArt.col(0x1C1812) : ghost(kind, 0.1, 0.2);
			return bone.paint(t);
		};
	}

	/**
	 * Armour of the old war: iron lamellae laced in rows, rusted through in patches (a spirit's: its light; the general's: gilded; the holy
	 * land's: white jade laced with gold; the fallen immortal's: gold gone dull, green with old bronze in patches).
	 */
	static SpellArt.Painter armour(int seed, Soldier kind) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.6 + seed, t.py * 0.6 - t.pz * 0.5, seed, 3);
			double row = t.up() * 6 - Math.floor(t.up() * 6), col = t.across() * 5 - Math.floor(t.across() * 5);
			boolean lace = row < 0.12 || t.side() && col < 0.1 || !t.side() && (t.f != null && col < 0.1);
			if (spirit(kind)) return ghost(kind, n, lace ? 0.25 : 0.6);
			if (kind == Soldier.GENERAL) {
				int c = SpellArt.mix(GOLD_DARK, GOLD, 0.4 + 0.6 * n);
				return lace ? SpellArt.col(SpellArt.shade(c, -0.45)) : SpellArt.col(c);
			}
			if (kind == Soldier.HOLY) return lace ? SpellArt.col(SpellArt.mix(GOLD_DARK, GOLD, 0.5 + 0.5 * n)) : SpellArt.col(SpellArt.mix(HOLY_JADE_DARK, HOLY_JADE, 0.4 + 0.6 * n));
			if (decayed(kind)) {
				double rot = SpellArt.smooth(0.45, 0.7, SpellArt.fbm(t.px * 0.25 + 5, t.py * 0.25 + t.pz * 0.2, seed + 11, 3));
				int c = SpellArt.mix(SpellArt.mix(ROT_GOLD_DARK, ROT_GOLD, 0.35 + 0.65 * n), VERDIGRIS, rot * 0.8);
				return SpellArt.col(lace ? SpellArt.shade(c, -0.45) : c);
			}
			double rust = SpellArt.smooth(0.45, 0.7, SpellArt.fbm(t.px * 0.25 + 3, t.py * 0.25 + t.pz * 0.2, seed + 9, 3));
			int c = SpellArt.mix(SpellArt.mix(IRON, 0x5A5A62, n), SpellArt.mix(RUST, RUST_LIGHT, n), rust);
			return SpellArt.col(lace ? SpellArt.shade(c, -0.5) : c);
		};
	}

	/** War cloth, faded red and torn: ragged toward its hem (holes, a frayed edge). */
	static SpellArt.Painter cloth(int seed, Soldier kind) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.7 + seed, t.py * 0.7 + t.pz * 0.6, seed, 3);
			double hem = 1 - t.up();
			if (!t.flat() && hem > 0.55 && SpellArt.hash((int) Math.floor(t.fx * 9), (int) Math.floor(t.fy * 9), seed) < (hem - 0.55) * 1.6) return SpellArt.CLEAR;
			if (spirit(kind)) return ghost(kind, n, 0.45);
			// The holy land's: white, a band of gold at its hem; the fallen immortal's: faded violet.
			if (kind == Soldier.HOLY) return SpellArt.col(hem > 0.82 ? SpellArt.mix(GOLD_DARK, GOLD, 0.5 + 0.5 * n) : SpellArt.mix(0xC8C0B0, HOLY_CLOTH, 0.3 + 0.7 * n));
			if (decayed(kind)) return SpellArt.col(SpellArt.mix(ROT_CLOTH_DARK, ROT_CLOTH, 0.3 + 0.7 * n));
			return SpellArt.col(SpellArt.mix(CLOTH_DARK, kind == Soldier.GENERAL ? 0xA8282A : CLOTH, 0.3 + 0.7 * n));
		};
	}

	/** Its spear's head: an iron leaf, keen-edged, rusted at its socket (a spirit's of light). */
	static SpellArt.Painter spearHead(int seed, Soldier kind) {
		return t -> {
			double half = 0.5 * Math.sin(Math.PI * Math.min(1, t.up() * 1.15));
			if (!t.flat() && Math.abs(t.across() - 0.5) > half && Math.abs(t.back() - 0.5) > half) return SpellArt.CLEAR;
			if (kind == Soldier.GHOST) return SpellArt.glow(SpellArt.mix(GHOST, SPIRIT_CORE, t.up()));
			if (kind == Soldier.ARMY) return SpellArt.glow(SpellArt.mix(ARMY, ARMY_CORE, t.up()));
			if (kind == Soldier.DROWNED) return SpellArt.glow(SpellArt.mix(DROWNED, DROWNED_EYE, t.up()));
			if (kind == Soldier.MIRROR) return SpellArt.glow(SpellArt.mix(MIRROR, MIRROR_EYE, t.up()));
			if (kind == Soldier.HOLY) return SpellArt.col(SpellArt.mix(GOLD_DARK, GOLD, 0.4 + 0.6 * t.up()));
			if (decayed(kind)) return SpellArt.col(SpellArt.mix(SpellArt.mix(ROT_GOLD_DARK, ROT_GOLD, t.up()), VERDIGRIS, 0.35 * (1 - t.up())));
			int c = SpellArt.mix(0x6A6C72, 0xB4B8C0, 0.4 + 0.6 * t.up());
			if (t.up() < 0.18 && kind == Soldier.BONE) c = SpellArt.mix(c, RUST, 0.6);
			return SpellArt.col(c);
		};
	}

	/** The dead's eyes: a cold light deep in the sockets. */
	static SpellArt.Painter socketLight(Soldier kind) {
		int c = kind == Soldier.GENERAL ? 0xFFB040 : kind == Soldier.GHOST ? 0xE8FFF8 : kind == Soldier.ARMY ? 0xFFF0C0 : kind == Soldier.DROWNED ? DROWNED_EYE
				: kind == Soldier.MIRROR ? MIRROR_EYE : kind == Soldier.HOLY ? HOLY_EYE : decayed(kind) ? ROT_EYE : 0x8FD8FF;
		return t -> SpellArt.glow(c);
	}

	/**
	 * A spirit's stuff: its light, seen through (more where it is thinner), brighter where it is denser; the banner's army's gold; the
	 * drowned's murk; the mirror's glassy silver.
	 */
	static int ghost(Soldier kind, double n, double body) {
		int dark = kind == Soldier.ARMY ? ARMY_DARK : kind == Soldier.DROWNED ? DROWNED_DARK : kind == Soldier.MIRROR ? MIRROR_DARK : GHOST_DARK;
		int light = kind == Soldier.ARMY ? ARMY : kind == Soldier.DROWNED ? DROWNED : kind == Soldier.MIRROR ? MIRROR : GHOST;
		return SpellArt.glow(SpellArt.mix(dark, light, 0.4 + 0.6 * n), 0.35 + 0.35 * body);
	}

	static void boneSoldier(SpellArt.Model m) { soldier(m, Soldier.BONE); }

	static void battleSpirit(SpellArt.Model m) { soldier(m, Soldier.GHOST); }

	static void generalSpirit(SpellArt.Model m) { soldier(m, Soldier.GENERAL); }

	/** A soldier of the old army called up by the War General's Banner (the mod's ArmySpirit): a Battle Spirit in the banner's gold. */
	static void armySpirit(SpellArt.Model m) { soldier(m, Soldier.ARMY); }

	/** A Drowned Shade of the Yellow Springs Hollow: a soldier of the drowned city's old garrison, risen in the springs' murky light. */
	static void drownedShade(SpellArt.Model m) { soldier(m, Soldier.DROWNED); }

	/** A Mirror Shade of the Jade Lake Mirage: a soldier of the lake's old sect, risen in the mirror's glassy silver light. */
	static void mirrorShade(SpellArt.Model m) { soldier(m, Soldier.MIRROR); }

	/** A remnant guard of the Ruins of a Fallen Holy Land: one of its disciples' last watch, in armour of white jade laced with gold. */
	static void holyRemnant(SpellArt.Model m) { soldier(m, Soldier.HOLY); }

	/** A decayed celestial guard of the Tomb of a Fallen Immortal: its gold armour gone dull and green with its lord's decay. */
	static void decayedGuard(SpellArt.Model m) { soldier(m, Soldier.DECAYED); }

	/** The fallen immortal's own decayed spirit, before his sanctum: as his guards, but caped as a lord (drawn larger: the mod's renderer). */
	static void decayedImmortal(SpellArt.Model m) { soldier(m, Soldier.DECAYED_LORD); }

	// --- The Ferryman (the Yellow Springs Hollow's) ---

	static final int ROBE = 0x413C44, ROBE_DARK = 0x1C1A20, STRAW = 0xB49C60, STRAW_DARK = 0x5E4E2C, HAT = 0xAE9258, HAT_DARK = 0x5A4624;
	static final int POLE = 0x3E2C1C, POLE_LIGHT = 0x6E5034, FACE = 0x0A090E, HAND = 0x8E887A;

	/** His robe: grey-black hemp, darker down its folds, its hem ragged. */
	static SpellArt.Painter robe(int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.5 + seed, t.py * 0.5 + t.pz * 0.45, seed, 3);
			double fold = 0.5 + 0.5 * Math.sin(t.across() * 13 + n * 3);
			double hem = 1 - t.up();
			if (!t.flat() && hem > 0.88 && SpellArt.hash((int) Math.floor(t.fx * 10), (int) Math.floor(t.fy * 10), seed) < (hem - 0.88) * 4) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(ROBE_DARK, ROBE, 0.3 + 0.45 * n + 0.25 * fold));
		};
	}

	/** His straw cape: strands hanging in rows, pale and dark by turns, their ends ragged below. */
	static SpellArt.Painter straw(int seed) {
		return t -> {
			int strand = (int) Math.floor(t.across() * 22);
			if (!t.flat() && t.up() < 0.15 && SpellArt.hash(strand, 7, seed) < 0.45) return SpellArt.CLEAR;
			return SpellArt.col(SpellArt.mix(STRAW_DARK, STRAW, 0.3 + 0.7 * SpellArt.hash(strand, (int) Math.floor(t.up() * 3), seed)));
		};
	}

	/** His hat of woven bamboo: a check of strips, light and dark, weathered. */
	static SpellArt.Painter bambooHat(int seed) {
		return t -> {
			boolean light = ((int) Math.floor(t.fx * 10) + (int) Math.floor(t.fy * 10)) % 2 == 0;
			double n = SpellArt.fbm(t.px * 0.6 + seed, t.pz * 0.6 + t.py * 0.3, seed, 2);
			return SpellArt.col(SpellArt.mix(HAT_DARK, HAT, (light ? 0.75 : 0.45) * (0.75 + 0.25 * n)));
		};
	}

	/** His pole: old dark wood, its grain running its length. */
	static SpellArt.Painter poleWood(int seed) {
		return t -> SpellArt.col(SpellArt.mix(POLE, POLE_LIGHT, 0.25 + 0.5 * SpellArt.fbm(t.py * 0.9 + seed, t.px * 0.3, seed, 2)));
	}

	/** His lantern: a frame of dark wood about panes of the springs' pale yellow light, brightest at its heart. */
	static SpellArt.Painter springLantern() {
		return t -> {
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.14) return SpellArt.col(POLE);
			return SpellArt.glow(SpellArt.mix(DROWNED_EYE, 0xFFFDE8, Math.max(0, 1 - Math.hypot(t.fx - 0.5, t.fy - 0.5) * 2)));
		};
	}

	/**
	 * The Ferryman of the Yellow Springs (the Hollow's trial, at its landing): some two blocks and a bit tall to his hat's crown, robed to the
	 * ground in grey-black hemp, a straw cape over his shoulders, a wide hat of woven bamboo hiding his face (only two pale eyes in the dark
	 * under it); his long punting pole upright in both hands, a lantern of the springs' light hung from a peg at its top.
	 */
	static void ferryman(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter robe = robe(7401), cape = straw(7403), hat = bambooHat(7405), pole = poleWood(7407);
		SpellArt.Painter face = t -> SpellArt.col(FACE), eye = t -> SpellArt.glow(DROWNED_EYE), hand = t -> SpellArt.col(HAND);
		SpellArt.Bone body = m.bone("body", "root", 0, 12, 0);
		body.box(0, 6.5, 0, 10, 13, 8, robe); // the robe's skirt, to the ground
		body.box(0, 18.5, 0, 8.6, 11, 6, robe);
		body.box(0, 24.8, 0.2, 12.4, 4.6, 8.6, cape); // the straw cape over his shoulders
		body.box(0, 19.4, 3.6, 10.6, 9, 1.4, cape); // and down his back
		SpellArt.Bone head = m.bone("head", "body", 0, 26, 0);
		head.box(0, 28.8, 0, 4.6, 4.8, 4.6, face);
		for (int s = -1; s <= 1; s += 2) head.box(s * 1.0, 29.0, -2.35, 0.9, 0.7, 0.3, eye).density(4);
		head.box(0, 31.6, 0, 15, 0.8, 15, hat); // the hat's brim
		head.box(0, 32.6, 0, 9, 1.4, 9, hat);
		head.box(0, 33.8, 0, 4.4, 1.2, 4.4, hat);
		// His arms in their wide sleeves, both hands on the pole before him.
		SpellArt.Bone armR = m.bone("arm_r", "body", -5, 24, 0);
		SpellArt.limb(armR, SpellArt.v3(-5.2, 24, 0), SpellArt.v3(-5, 18.6, -1.6), 3.2, 3.2, robe);
		SpellArt.Bone lowR = m.bone("arm_r_low", "arm_r", -5, 18.6, -1.6);
		SpellArt.limb(lowR, SpellArt.v3(-5, 18.6, -1.6), SpellArt.v3(-2.6, 15.6, -5.2), 2.8, 2.8, robe);
		lowR.box(-2.2, 15.2, -5.8, 1.6, 1.6, 1.6, hand);
		SpellArt.Bone armL = m.bone("arm_l", "body", 5, 24, 0);
		SpellArt.limb(armL, SpellArt.v3(5.2, 24, 0), SpellArt.v3(4, 20, -2.6), 3.2, 3.2, robe);
		SpellArt.Bone lowL = m.bone("arm_l_low", "arm_l", 4, 20, -2.6);
		SpellArt.limb(lowL, SpellArt.v3(4, 20, -2.6), SpellArt.v3(-0.8, 21.2, -5.4), 2.8, 2.8, robe);
		lowL.box(-1.6, 21.2, -5.8, 1.6, 1.6, 1.6, hand);
		// The pole, upright in his hands (its right hand's), the lantern hung from a peg at its top.
		SpellArt.Bone staff = m.bone("pole", "arm_r_low", -2.2, 15.2, -5.8);
		staff.box(-2.2, 23.5, -5.8, 0.8, 45, 0.8, pole).density(2);
		staff.box(-3.6, 44.6, -5.8, 2.8, 0.5, 0.5, pole);
		staff.box(-4.8, 43.6, -5.8, 0.3, 1.6, 0.3, pole);
		staff.box(-4.8, 41.6, -5.8, 2.2, 2.6, 2.2, springLantern()).density(4);
		ferrymanDormant(m);
		ferrymanIdle(m);
		ferrymanWalk(m);
		ferrymanStrike(m);
	}

	/** Still at his post: leaning on his pole, his hat bowed low. */
	static void ferrymanDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("body", "rotation", 0, T, 4, t -> new double[] {5, 0, 0});
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {22, 0, 0});
	}

	/** Awake: a slow sway, his hat lifting a little to look about, the pole rocking in his hands. */
	static void ferrymanIdle(SpellArt.Model m) {
		double T = 3.6;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {1.5 * wave(t, T, 1, 0), 0, 1.2 * wave(t, T, 1, 1.3)});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {4 + 3 * wave(t, T, 1, 0.6), 4 * wave(t, T, 1, 2), 0});
		a.sample("pole", "rotation", 0, T, 16, t -> new double[] {2 * wave(t, T, 1, 0.3), 0, 0});
	}

	/** Gliding on: no feet show under the robe; it sways as he goes, the pole planted and pushed with each stride. */
	static void ferrymanWalk(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.4 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {6, 4 * wave(t, T, 1, 0), 2 * wave(t, T, 1, 0)});
		a.sample("arm_r", "rotation", 0, T, 16, t -> new double[] {-10 + 12 * wave(t, T, 1, 0), 0, 0});
		a.sample("pole", "rotation", 0, T, 16, t -> new double[] {-10 * wave(t, T, 1, 0), 0, 0});
	}

	/** His blow: the pole swept up and back with his right arm, then brought down before him, his body turning into it. */
	static void ferrymanStrike(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "rotation", 0, L, 12, t -> new double[] {12 * strike(t / L), -18 * windUp(t / L) + 14 * strike(t / L), 0});
		a.sample("arm_r", "rotation", 0, L, 12, t -> new double[] {-120 * windUp(t / L) + 30 * strike(t / L), 0, 0});
		a.sample("pole", "rotation", 0, L, 12, t -> new double[] {-40 * windUp(t / L) + 70 * strike(t / L), 0, 0});
		a.sample("arm_l", "rotation", 0, L, 12, t -> new double[] {-60 * windUp(t / L) + 20 * strike(t / L), 0, 0});
	}

	/**
	 * A soldier of the old war (the Ancient Battlefield): some two blocks tall, a skeleton in the armour it died in; a helmet of iron with a red
	 * plume over its skull, lamellar armour over its ribs, a red sash, greaves on its shins, a long spear in its right hand. Its spirit (a Battle
	 * Spirit) is the same soldier in light, seen through; its general's (the General's Spirit) gilded, with a cape (drawn larger by its
	 * renderer).
	 */
	static void soldier(SpellArt.Model m, Soldier kind) {
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter bone = bone(7301, kind), ribs = ribs(7302, kind), plate = armour(7303, kind), cloth = cloth(7304, kind);
		SpellArt.Painter wood = spirit(kind) ? t -> ghost(kind, 0.5, 0.5) : Axis.spearWood(7305);
		SpellArt.Bone waist = m.bone("waist", "root", 0, 14, 0);
		waist.box(0, 14.2, 0, 5.4, 2, 3, bone);
		waist.box(0, 11.6, -1.7, 4.6, 5, 0.4, cloth); // its war skirt, front
		waist.box(0, 11.6, 1.7, 4.6, 5, 0.4, cloth); // and back
		SpellArt.Bone body = m.bone("body", "waist", 0, 15, 0);
		body.box(0, 17.8, 0, 1.2, 4, 1.2, bone); // its spine
		body.box(0, 21.4, 0, 6.4, 5.4, 3.6, ribs);
		body.box(0, 21.6, -2.1, 7, 5.6, 0.6, plate); // its armour, front
		body.box(0, 21.6, 2.1, 7, 5.6, 0.6, plate); // and back
		body.box(0, 20.6, -2.5, 7.6, 1.2, 0.4, cloth).rot(0, 0, -28); // its sash
		for (int s = -1; s <= 1; s += 2) body.box(s * 4.4, 24.4, 0, 2.8, 1.2, 4.2, plate).rot(0, 0, -s * 18); // its shoulder plates
		if (kind == Soldier.GENERAL || kind == Soldier.DECAYED_LORD) body.box(0, 18.6, 2.8, 7.4, 10, 0.4, cloth).rot(8, 0, 0); // its cape
		SpellArt.Bone head = m.bone("head", "body", 0, 25, 0);
		head.box(0, 27.4, 0, 4.4, 4.4, 4.4, bone);
		for (int s = -1; s <= 1; s += 2) head.box(s * 1.05, 27.8, -2.25, 1.1, 1.1, 0.3, socketLight(kind)).density(4);
		head.box(0, 30.1, 0, 5, 1.6, 5, plate); // its helmet
		head.box(0, 29.2, 2.3, 5, 2.4, 0.5, plate); // its neck guard
		for (int s = -1; s <= 1; s += 2) head.box(s * 2.45, 28.4, 0.3, 0.4, 2.6, 3.8, plate); // its cheek guards
		head.box(0, 32.2, 0, 0.6, 2.6, 0.6, plate); // its spike
		head.box(0, 31.6, 0.9, 1.4, 1.8, 2.2, cloth).rot(-35, 0, 0); // its plume
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 25.6, -0.4);
		jaw.box(0, 25.4, -0.6, 3.6, 1, 3.4, bone);
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			SpellArt.Bone arm = m.bone(w, "body", s * 4.2, 23.6, 0);
			SpellArt.limb(arm, SpellArt.v3(s * 4.4, 23.6, 0), SpellArt.v3(s * 4.8, 18.2, 0), 1.2, 1.2, bone);
			SpellArt.Bone low = m.bone(w + "_low", w, s * 4.8, 18.2, 0);
			SpellArt.limb(low, SpellArt.v3(s * 4.8, 18.2, 0), SpellArt.v3(s * 4.9, 12.8, -0.8), 1.1, 1.1, bone);
			low.box(s * 4.9, 12, -0.9, 1.7, 1.7, 1.7, bone); // its hand
			low.box(s * 4.85, 15.2, -0.4, 1.8, 2.8, 1.8, plate); // its bracer
			String l = "leg_" + (s > 0 ? "l" : "r");
			SpellArt.Bone leg = m.bone(l, "root", s * 1.7, 13.4, 0);
			SpellArt.limb(leg, SpellArt.v3(s * 1.7, 13.4, 0), SpellArt.v3(s * 1.9, 7, 0), 1.3, 1.3, bone);
			SpellArt.Bone shin = m.bone(l + "_low", l, s * 1.9, 7, 0);
			SpellArt.limb(shin, SpellArt.v3(s * 1.9, 7, 0), SpellArt.v3(s * 2, 1.2, 0), 1.2, 1.2, bone);
			shin.box(s * 2, 4.2, -0.2, 2, 3.8, 2, plate); // its greave
			shin.box(s * 2, 0.6, -0.8, 1.9, 1.2, 3.2, bone); // its foot
		}
		// Its spear in its right hand: a long shaft upright, the head at its top, a red tassel under it.
		SpellArt.Bone spear = m.bone("spear", "arm_r_low", -4.9, 12, -0.9);
		spear.box(-4.9, 16, -0.9, 0.7, 34, 0.7, wood).density(2);
		spear.box(-4.9, 35.6, -0.9, 1.6, 5.2, 0.4, spearHead(7306, kind)).density(4);
		spear.box(-4.9, 32.4, -0.9, 1.4, 1.4, 1.4, plate);
		spear.box(-4.9, 31, -0.9, 1.6, 2.6, 1.6, cloth);
		soldierDormant(m);
		soldierIdle(m);
		soldierWalk(m);
		soldierStrike(m);
	}

	/** Fallen where it stood: down on one knee, bowed over its planted spear, its head hanging. */
	static void soldierDormant(SpellArt.Model m) {
		double T = 4.0;
		SpellArt.Anim a = m.anim("dormant", T, "true");
		a.sample("root", "position", 0, T, 4, t -> new double[] {0, -4.4, 0});
		a.sample("body", "rotation", 0, T, 4, t -> new double[] {28, 0, 0});
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {32, 0, 6});
		a.sample("jaw", "rotation", 0, T, 4, t -> new double[] {14, 0, 0});
		a.sample("leg_r", "rotation", 0, T, 4, t -> new double[] {-88, 0, 0});
		a.sample("leg_r_low", "rotation", 0, T, 4, t -> new double[] {88, 0, 0});
		a.sample("leg_l", "rotation", 0, T, 4, t -> new double[] {-10, 0, 0});
		a.sample("leg_l_low", "rotation", 0, T, 4, t -> new double[] {94, 0, 0});
		a.sample("arm_r", "rotation", 0, T, 4, t -> new double[] {-30, 0, 0});
		a.sample("arm_r_low", "rotation", 0, T, 4, t -> new double[] {-20, 0, 0});
		// (The spear's tip leans as the sum of its arm's turns and its own: forward, over its planted foot, at +15.)
		a.sample("spear", "rotation", 0, T, 4, t -> new double[] {65, 0, 0});
		a.sample("arm_l", "rotation", 0, T, 4, t -> new double[] {-10, 0, 10});
	}

	/** Standing guard: spear upright at its side, a slow sway; now and then its jaw works, as if it spoke. */
	static void soldierIdle(SpellArt.Model m) {
		double T = 3.0;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {2 * wave(t, T, 1, 0), 3 * wave(t, T, 1, 1.1), 0});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {-3 + 3 * wave(t, T, 1, 0.5), 0, 0});
		a.sample("jaw", "rotation", 0, T, 16, t -> new double[] {Math.max(0, 14 * wave(t, T, 3, 0)), 0, 0});
		a.sample("arm_r", "rotation", 0, T, 16, t -> new double[] {-14, 0, 4});
		a.sample("arm_r_low", "rotation", 0, T, 16, t -> new double[] {-40, 0, 0});
		a.sample("spear", "rotation", 0, T, 16, t -> new double[] {50, 0, 0});
		a.sample("arm_l", "rotation", 0, T, 16, t -> new double[] {2 * wave(t, T, 1, 0), 0, 5});
	}

	/** Marching: legs swinging and bending at the knee, the free arm against them, the spear carried slanted forward. */
	static void soldierWalk(SpellArt.Model m) {
		double T = 1.1;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.5 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {8, 4 * wave(t, T, 1, 0), 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("leg_" + (s > 0 ? "l" : "r"), "rotation", 0, T, 16, t -> new double[] {26 * ss * wave(t, T, 1, 0), 0, 0});
			a.sample("leg_" + (s > 0 ? "l" : "r") + "_low", "rotation", 0, T, 16, t -> new double[] {Math.max(0, 30 * ss * wave(t, T, 1, -1.2)), 0, 0});
		}
		a.sample("arm_l", "rotation", 0, T, 16, t -> new double[] {-22 * wave(t, T, 1, 0), 0, 5});
		a.sample("arm_r", "rotation", 0, T, 16, t -> new double[] {-30, 0, 4});
		a.sample("arm_r_low", "rotation", 0, T, 16, t -> new double[] {-45, 0, 0});
		a.sample("spear", "rotation", 0, T, 16, t -> new double[] {115, 0, 0}); // its tip forward at +40 (the sum with its arm's)
	}

	/**
	 * Its thrust: the spear drawn back beside it, then lowered level and driven at its foe, the arm straight out, the body lunging behind it.
	 * (The spear's tip leans by the sum of its arm's turns and its own: upright at 0, level forward at +90.)
	 */
	static void soldierStrike(SpellArt.Model m) {
		double L = 0.9;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "rotation", 0, L, 12, t -> new double[] {8 + 14 * strike(t / L), -20 * windUp(t / L) + 14 * strike(t / L), 0});
		a.sample("arm_r", "rotation", 0, L, 12, t -> new double[] {-14 + 10 * windUp(t / L) - 61 * strike(t / L), 0, 6});
		a.sample("arm_r_low", "rotation", 0, L, 12, t -> new double[] {-40 - 5 * windUp(t / L) + 30 * strike(t / L), 0, 0});
		a.sample("spear", "rotation", 0, L, 12, t -> new double[] {50 + 30 * windUp(t / L) + 125 * strike(t / L), 0, 0});
		a.sample("spear", "position", 0, L, 12, t -> new double[] {0, 0, 2.5 * windUp(t / L) - 4 * strike(t / L)});
		a.sample("arm_l", "rotation", 0, L, 12, t -> new double[] {-20 * windUp(t / L) + 20 * strike(t / L), 0, 5});
		a.sample("leg_l", "rotation", 0, L, 12, t -> new double[] {-24 * strike(t / L), 0, 0});
	}

	private Realms() {}
}
