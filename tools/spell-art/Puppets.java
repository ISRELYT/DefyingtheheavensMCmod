/**
 * The puppets' looks (Puppetry, 2026-11-17; the user's picks: "Humanoid + bee first", "Material by grade": Mortal wood, Spirit bronze,
 * Earth dark iron with jade inlay, Heaven silver-white, Immortal gold), written by {@code java SpellArt.java --puppet <assets>} as
 * geo/puppet/{shape}_{grade}.geo.json, animations/puppet/... and textures/entity/puppet/... (the same make at every grade, its matter
 * painted as the grade's).
 * <ul>
 *   <li>The humanoid: a jointed figure two blocks tall, ball joints at shoulder, elbow, hip and knee, a chest plate with its core's light
 *   behind it, a slit of the same light for its eyes, a jaw that drops for its art; a smooth panel on its back where its circuits glow for
 *   one with Qi Sense (the client's PuppetRenderer). Idle, walking, its blow ("strike"), its art ("beam"), slumped ("inert").</li>
 *   <li>The bee: a thorax and a striped abdomen of the grade's matter, a head with eyes of light, four wings of thin membrane, six legs and
 *   a stinger; a panel on its abdomen's back for its circuits. Flying (always: it never lands while it works), its sting, its art from the
 *   stinger, and at rest ("inert": wings folded, sunk down).</li>
 * </ul>
 */
public class Puppets {
	static final String[] GRADES = {"mortal", "spirit", "earth", "heaven", "immortal"};

	/** A grade's matter: its body (light, mid, dark), its joints (light, dark), its plates (light, dark), its inlay (0: none), its wings' membrane. */
	record Look(int light, int mid, int dark, int jointLight, int jointDark, int plateLight, int plateDark, int inlay, int membrane, boolean grain) {}

	static final Look[] LOOKS = {
			// Mortal: pale oak, dark walnut joints, darker lacquered plates; amber wings.
			new Look(0xD8A870, 0xB8875A, 0x7E5634, 0x6E4426, 0x3A2414, 0x8A5530, 0x52301A, 0, 0xE8C890, true),
			// Spirit: bronze with a green bloom, dark bronze joints, polished plates; pale green-gold wings.
			new Look(0xE6BC6E, 0xB98A44, 0x7A5424, 0x6A4A22, 0x3A2810, 0xF0C870, 0x9A6A28, 0, 0xD8E6B0, false),
			// Earth: dark iron inlaid with jade, black iron joints, jade plates; jade-green wings.
			new Look(0x6E747C, 0x464A52, 0x26292E, 0x34373C, 0x141618, 0x8EE6BC, 0x3E9E74, 0x5FD8A0, 0xA8F0D0, false),
			// Heaven: silver-white, pale gold joints, white jade plates; pale blue-white wings.
			new Look(0xFFFFFF, 0xE2E6EE, 0xAEB6C2, 0xF0D890, 0xB89A48, 0xF6F8EE, 0xC8D2C4, 0, 0xE8F2FF, false),
			// Immortal: gold, rose-gold joints, white-gold plates with jade; warm gold wings.
			new Look(0xFFE8A0, 0xE8BE4A, 0xA87C1E, 0xF0B090, 0xA86448, 0xFFF6DC, 0xD8C890, 0x9AE0BE, 0xFFF0B8, false)};

	// --- Painters ---

	/** Its body: the grade's matter, the edges catching the light; wood with its grain along the part, iron with lines of jade let in. */
	static SpellArt.Painter body(Look k, int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.3 + seed, t.py * 0.3 + t.pz * 0.27, seed, 3);
			int c = SpellArt.mix(k.dark(), k.mid(), 0.35 + 0.65 * n);
			if (k.grain()) {
				double grain = 0.5 + 0.5 * Math.sin((t.qx() + t.qz()) * 2.2 + SpellArt.noise(t.qy() * 0.6, seed, seed) * 3);
				c = SpellArt.mix(c, k.dark(), 0.28 * grain);
			}
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.08) c = SpellArt.mix(c, k.light(), 0.5);
			if (k.inlay() != 0 && Math.abs(Math.sin(t.qy() * 1.3 + t.qx() * 0.4)) < 0.12 && Math.min(ex, ey) > 0.15) c = SpellArt.mix(c, k.inlay(), 0.85);
			return SpellArt.col(c);
		};
	}

	/** Its joints: balls and pins, darker, a bright rim where the light strikes them. */
	static SpellArt.Painter joint(Look k, int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.4 + seed, t.py * 0.4 + t.pz * 0.33, seed, 2);
			int c = SpellArt.mix(k.jointDark(), k.jointLight(), 0.3 + 0.6 * n);
			double d = Math.hypot(t.fx - 0.35, t.fy - 0.3);
			if (d < 0.18) c = SpellArt.mix(c, 0xFFFFFF, 0.35 * (1 - d / 0.18));
			return SpellArt.col(c);
		};
	}

	/** Its plates: smoother, a chased line within the edge, rivets at the corners. */
	static SpellArt.Painter plate(Look k, int seed) {
		return t -> {
			double n = SpellArt.fbm(t.px * 0.25 + seed, t.py * 0.25 + t.pz * 0.2, seed, 2);
			int c = SpellArt.mix(k.plateDark(), k.plateLight(), 0.45 + 0.5 * n);
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy), e = Math.min(ex, ey);
			if (e < 0.07) c = SpellArt.mix(c, 0xFFFFFF, 0.25);
			else if (e > 0.11 && e < 0.15) c = SpellArt.mix(c, k.plateDark(), 0.55);
			if (ex < 0.16 && ey < 0.16 && Math.hypot(ex - 0.08, ey - 0.08) < 0.05) c = SpellArt.mix(c, k.jointDark(), 0.7);
			if (k.inlay() != 0 && e > 0.2 && Math.abs(t.fx - 0.5) < 0.06) c = SpellArt.mix(c, k.inlay(), 0.6);
			return SpellArt.col(c);
		};
	}

	/** The qi light in it (its core, its eyes, a bee's stinger): glowing azure, white at its middle. */
	static SpellArt.Painter qiLight() {
		return t -> {
			double d = Math.hypot(t.fx - 0.5, t.fy - 0.5);
			return SpellArt.glow(SpellArt.mix(0xF0FFFF, 0x6FD8FF, Math.min(1, d * 2.2)));
		};
	}

	/** A bee's wing: thin membrane seen through, its veins darker, its edge a fine line of its frame's metal. */
	static SpellArt.Painter membrane(Look k, int seed) {
		return t -> {
			double ex = Math.min(t.fx, 1 - t.fx), ey = Math.min(t.fy, 1 - t.fy);
			if (Math.min(ex, ey) < 0.06) return SpellArt.col(k.jointLight());
			double vein = Math.abs(Math.sin(t.fx * 9 + t.fy * 3 + SpellArt.noise(t.fx * 3, t.fy * 3, seed) * 2));
			if (vein < 0.12) return SpellArt.col(SpellArt.mix(k.membrane(), k.jointDark(), 0.55), 0.8);
			return SpellArt.col(k.membrane(), 0.5);
		};
	}

	// --- The humanoid ---

	static void humanoid(SpellArt.Model m, int grade) {
		Look k = LOOKS[grade];
		int seed = 8100 + grade * 10;
		SpellArt.Painter bodyP = body(k, seed), joint = joint(k, seed + 1), plate = plate(k, seed + 2), light = qiLight();
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone waist = m.bone("waist", "root", 0, 13, 0);
		waist.box(0, 13, 0, 7, 3, 4.2, plate);
		SpellArt.Bone body = m.bone("body", "waist", 0, 14.5, 0);
		body.box(0, 19.5, 0, 8, 9, 5, bodyP); // the torso: y 15 to 24
		body.box(0, 20.2, -2.75, 6, 5, 0.5, plate); // its chest plate
		body.box(0, 20.6, -3.15, 2, 2, 0.4, light).density(4); // its core's light
		body.box(0, 24.4, 0, 6, 0.8, 4.2, plate); // its collar
		body.box(0, 15.6, 0, 7.2, 1.2, 4.6, joint); // its waist ring
		body.box(0, 19.6, 2.7, 6.6, 6.6, 0.4, plate); // the panel on its back, where its circuits glow (back face at z 2.9)
		SpellArt.Bone head = m.bone("head", "body", 0, 24.5, 0);
		head.box(0, 25.3, 0, 2, 1.6, 2, joint); // its neck
		head.box(0, 28.4, 0, 6, 6, 6, bodyP); // its head: y 25.4 to 31.4
		head.box(0, 29.2, -3.05, 4.4, 0.9, 0.3, light).density(4); // its eyes' slit
		head.box(0, 31.6, 0.4, 1, 0.8, 4.4, plate); // its crest
		head.box(0, 26.6, -2.92, 3, 0.8, 0.2, light).density(4); // its mouth's light, behind the jaw
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 27, -1);
		jaw.box(0, 26.4, -2.35, 4.2, 1.4, 1.6, plate); // its jaw, dropping for its art
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			SpellArt.Bone arm = m.bone(w, "body", s * 5.3, 23, 0);
			arm.box(s * 5.3, 23, 0, 2.8, 2.8, 2.8, joint); // its shoulder
			arm.box(s * 5.5, 19.6, 0, 2.4, 5.2, 2.4, bodyP);
			arm.box(s * 5.5, 21.7, 0, 3, 1.4, 3, plate);
			SpellArt.Bone low = m.bone(w + "_low", w, s * 5.5, 16.6, 0);
			low.box(s * 5.5, 16.6, 0, 2.2, 2.2, 2.2, joint); // its elbow
			low.box(s * 5.5, 13.4, 0, 2.2, 5, 2.2, bodyP);
			low.box(s * 5.5, 10.1, -0.2, 2.6, 2.2, 2.8, joint); // its fist
			String l = "leg_" + (s > 0 ? "l" : "r");
			SpellArt.Bone leg = m.bone(l, "root", s * 2.1, 12, 0);
			leg.box(s * 2.1, 11.8, 0, 2.8, 2.8, 2.8, joint); // its hip
			leg.box(s * 2.1, 8.8, 0, 2.6, 4.6, 2.6, bodyP);
			SpellArt.Bone shin = m.bone(l + "_low", l, s * 2.1, 6.2, 0);
			shin.box(s * 2.1, 6.2, 0, 2.4, 2.4, 2.4, joint); // its knee
			shin.box(s * 2.1, 3.2, 0, 2.4, 4.6, 2.4, bodyP);
			shin.box(s * 2.1, 0.6, -0.6, 2.8, 1.2, 4, plate); // its foot
		}
		humanoidIdle(m);
		humanoidWalk(m);
		humanoidStrike(m);
		humanoidBeam(m);
		humanoidInert(m);
	}

	static double wave(double t, double T, double k, double phase) { return Math.sin(2 * Math.PI * k * t / T + phase); }

	static String side(int s) { return s > 0 ? "l" : "r"; }

	/** Standing ready: a slow mechanical breath, its arms a little out. */
	static void humanoidIdle(SpellArt.Model m) {
		double T = 3.0;
		SpellArt.Anim a = m.anim("idle", T, "true");
		a.sample("body", "rotation", 0, T, 12, t -> new double[] {1.2 * wave(t, T, 1, 0), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + side(s), "rotation", 0, T, 12, t -> new double[] {-3 + 2 * wave(t, T, 1, ss), 0, -ss * 5});
			a.sample("arm_" + side(s) + "_low", "rotation", 0, T, 12, t -> new double[] {-12, 0, 0});
		}
	}

	/** Its even stride: legs from the hip, knees bending, arms against them, a slight bob. */
	static void humanoidWalk(SpellArt.Model m) {
		double T = 1.0;
		SpellArt.Anim a = m.anim("walk", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 0.4 * Math.abs(wave(t, T, 1, 0)), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {3, 4 * wave(t, T, 1, 0), 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("leg_" + side(s), "rotation", 0, T, 16, t -> new double[] {26 * ss * wave(t, T, 1, 0), 0, 0});
			a.sample("leg_" + side(s) + "_low", "rotation", 0, T, 16, t -> new double[] {Math.max(0, 30 * ss * wave(t, T, 1, -1.2)), 0, 0});
			a.sample("arm_" + side(s), "rotation", 0, T, 16, t -> new double[] {-20 * ss * wave(t, T, 1, 0), 0, -ss * 4});
			a.sample("arm_" + side(s) + "_low", "rotation", 0, T, 16, t -> new double[] {-15, 0, 0});
		}
	}

	static double windUp(double u) {
		return u < 0.35 ? SpellArt.ease(u / 0.35) : u < 0.55 ? 1 - SpellArt.ease((u - 0.35) / 0.2) : 0;
	}

	static double blow(double u) {
		return u < 0.35 ? 0 : u < 0.55 ? SpellArt.easeOut((u - 0.35) / 0.2) : 1 - SpellArt.ease((u - 0.55) / 0.45);
	}

	/** Its blow: the right fist drawn back, the body turning, then driven straight out. */
	static void humanoidStrike(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("strike", L, null);
		a.sample("body", "rotation", 0, L, 10, t -> new double[] {4 * blow(t / L), 16 * windUp(t / L) - 14 * blow(t / L), 0});
		a.sample("arm_r", "rotation", 0, L, 10, t -> new double[] {35 * windUp(t / L) - 92 * blow(t / L), 0, 4});
		a.sample("arm_r_low", "rotation", 0, L, 10, t -> new double[] {-70 * windUp(t / L) - 8 * blow(t / L), 0, 0});
		a.sample("arm_l", "rotation", 0, L, 10, t -> new double[] {-25 * windUp(t / L), 0, -6});
	}

	/** Its art: its head thrown back a little, its jaw dropping, held while the beam runs, then shut. */
	static void humanoidBeam(SpellArt.Model m) {
		double L = 0.7;
		SpellArt.Anim a = m.anim("beam", L, null);
		a.sample("head", "rotation", 0, L, 10, t -> new double[] {-10 * hold(t / L), 0, 0});
		a.sample("jaw", "rotation", 0, L, 10, t -> new double[] {30 * hold(t / L), 0, 0});
		a.sample("body", "rotation", 0, L, 10, t -> new double[] {-4 * hold(t / L), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + side(s), "rotation", 0, L, 10, t -> new double[] {18 * hold(t / L), 0, -ss * 14 * hold(t / L)});
		}
	}

	/** Up to 1 over the first fifth, held, and down over the last quarter. */
	static double hold(double u) {
		return u < 0.2 ? SpellArt.ease(u / 0.2) : u < 0.75 ? 1 : 1 - SpellArt.ease((u - 0.75) / 0.25);
	}

	/** Slumped: no qi in its heart of command (or frozen, or stalled): head down, shoulders forward, knees given. */
	static void humanoidInert(SpellArt.Model m) {
		double T = 2.5;
		SpellArt.Anim a = m.anim("inert", T, "true");
		a.sample("root", "position", 0, T, 4, t -> new double[] {0, -1.2, 0});
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {38, 0, 0});
		a.sample("body", "rotation", 0, T, 8, t -> new double[] {16 + 0.6 * wave(t, T, 1, 0), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			int ss = s;
			a.sample("arm_" + side(s), "rotation", 0, T, 4, t -> new double[] {-14, 0, -ss * 8});
			a.sample("arm_" + side(s) + "_low", "rotation", 0, T, 4, t -> new double[] {-12, 0, 0});
			a.sample("leg_" + side(s), "rotation", 0, T, 4, t -> new double[] {-14, 0, 0});
			a.sample("leg_" + side(s) + "_low", "rotation", 0, T, 4, t -> new double[] {26, 0, 0});
		}
	}

	// --- The bee ---

	static void bee(SpellArt.Model m, int grade) {
		Look k = LOOKS[grade];
		int seed = 8200 + grade * 10;
		SpellArt.Painter bodyP = body(k, seed), joint = joint(k, seed + 1), plate = plate(k, seed + 2), light = qiLight(), wing = membrane(k, seed + 3);
		m.tpp = 2;
		m.scale = 1;
		m.creature = true;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 7, 0);
		body.box(0, 7, -0.6, 4.6, 4.2, 4.6, bodyP); // the thorax: y 4.9 to 9.1, z -2.9 to 1.7
		body.box(0, 9.25, -0.6, 3.2, 0.3, 3.2, plate);
		for (int s = -1; s <= 1; s += 2) {
			for (int p = 0; p < 3; p++) {
				double z = -2.0 + p * 1.4, x = s * 2.4;
				body.box(x + s * 0.9, 3.6, z, 0.6, 3, 0.6, joint).rot(0, 0, -s * 28, x, 5.2, z); // its legs
			}
		}
		SpellArt.Bone head = m.bone("head", "body", 0, 7.4, -2.9);
		head.box(0, 7.4, -4.4, 3.8, 3.6, 3, bodyP);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 1.35, 7.9, -5.95, 1.1, 1.4, 0.3, light).density(4); // its eyes
			head.box(s * 0.9, 10.2, -5, 0.5, 2.6, 0.5, joint).rot(-25, 0, -s * 15, s * 0.9, 9, -5); // its feelers
			head.box(s * 0.7, 5.9, -5.95, 0.8, 0.8, 0.6, plate); // its mandibles
		}
		SpellArt.Bone abdomen = m.bone("abdomen", "body", 0, 7, 1.7);
		abdomen.box(0, 6.6, 4.9, 5.2, 4.8, 6.2, bodyP); // y 4.2 to 9.0, z 1.8 to 8.0
		abdomen.box(0, 6.6, 3.6, 5.5, 5.1, 0.9, plate); // its bands
		abdomen.box(0, 6.6, 5.9, 5.5, 5.1, 0.9, plate);
		abdomen.box(0, 9.2, 4.9, 4.4, 0.3, 5.4, plate); // the panel on its back where its circuits glow (top face at y 9.35)
		SpellArt.Bone stinger = m.bone("stinger", "abdomen", 0, 6.2, 8);
		stinger.box(0, 6.2, 8.9, 1, 1, 1.8, joint);
		stinger.box(0, 6.2, 10.1, 0.5, 0.5, 0.7, light).density(4);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone w = m.bone("wing_" + side(s), "body", s * 1.6, 9.2, -0.8);
			w.cube(s > 0 ? 1.6 : -7.8, 9.15, -2.4, 6.2, 0.1, 4.2, wing); // its forewing
			w.cube(s > 0 ? 1.4 : -5.8, 9.1, 0.6, 4.4, 0.1, 2.8, wing); // its hind wing
		}
		beeFly(m);
		beeSting(m);
		beeBeam(m);
		beeInert(m);
	}

	/** On the wing: its wings beating fast, a little bob, its abdomen swaying. */
	static void beeFly(SpellArt.Model m) {
		double T = 0.6;
		SpellArt.Anim a = m.anim("fly", T, "true");
		a.sample("root", "position", 0, T, 12, t -> new double[] {0, 3 + 0.7 * wave(t, T, 1, 0), 0});
		a.sample("wing_l", "rotation", 0, T, 24, t -> new double[] {0, 0, 38 * wave(t, T, 4, 0)});
		a.sample("wing_r", "rotation", 0, T, 24, t -> new double[] {0, 0, -38 * wave(t, T, 4, 0)});
		a.sample("abdomen", "rotation", 0, T, 12, t -> new double[] {-4 + 3 * wave(t, T, 1, 1), 0, 0});
	}

	/** Its sting: the abdomen curled down and forward, struck, drawn back. */
	static void beeSting(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("sting", L, null);
		a.sample("abdomen", "rotation", 0, L, 10, t -> new double[] {12 * windUp(t / L) - 48 * blow(t / L), 0, 0});
		a.sample("stinger", "rotation", 0, L, 10, t -> new double[] {-20 * blow(t / L), 0, 0});
	}

	/** Its art from the stinger: the abdomen curled under and held, the stinger thrust out. */
	static void beeBeam(SpellArt.Model m) {
		double L = 0.7;
		SpellArt.Anim a = m.anim("beam", L, null);
		a.sample("abdomen", "rotation", 0, L, 10, t -> new double[] {-34 * hold(t / L), 0, 0});
		a.sample("stinger", "scale", 0, L, 10, t -> new double[] {1, 1, 1 + 0.5 * hold(t / L)});
	}

	/** At rest (inert, frozen, stalled): sunk down, its wings folded back along it, its head low. */
	static void beeInert(SpellArt.Model m) {
		double T = 2.5;
		SpellArt.Anim a = m.anim("inert", T, "true");
		a.sample("root", "position", 0, T, 4, t -> new double[] {0, 0, 0});
		a.sample("wing_l", "rotation", 0, T, 4, t -> new double[] {0, 32, -6});
		a.sample("wing_r", "rotation", 0, T, 4, t -> new double[] {0, -32, 6});
		a.sample("head", "rotation", 0, T, 4, t -> new double[] {12, 0, 0});
		a.sample("abdomen", "rotation", 0, T, 4, t -> new double[] {-8, 0, 0});
	}
}
