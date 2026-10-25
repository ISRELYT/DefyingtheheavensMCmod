/**
 * The looks of the ghost path's spells below Dao Integration (Spells), made over as the great spells' are (2026-10-25: the user, of the spells
 * of Four Axis and below, "they were done before geckolib and some look not great"): shapes of ghost fire, the dead's mist, their bones and the
 * Yellow Springs' water, in the dead's colours (Ghost), as the spirit beasts are of their elements. At the render scale and greatness 1 (all
 * of them are of the realms that keep it) a model pixel is the scale over sixteen blocks.
 */
public class Underworld {
	// --- The Yellow Springs' water, gold, and the six paths' colours ---

	/** The Yellow Springs' water: murky yellow with green in it (a tan read as wood in game, 2026-10-25), its foam. */
	static final int SPRINGS_DEEP = 0x40421A, SPRINGS = 0x8E8A2E, SPRINGS_LIGHT = 0xC8C252, SPRINGS_PALE = 0xEAE6A4, FOAM = 0xF6F6E2;
	/** The six paths of rebirth, as the Wheel of Samsara shows them: heaven, the asuras, the living, beasts, hungry ghosts, hell. */
	static final int[] PATHS = {0xFFF1B8, 0xD8303C, 0x5FC88A, 0xD0902E, 0x9480C8, 0x6A1018};

	// --- Painters ---

	/** Ghost fire's heart (on a solid): white at its middle through pale to cyan, boiling a little; glowing. */
	static SpellArt.Painter ghostCore(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(0.3 + 0.6 * n + 0.2 * Math.max(0, t.ny)), Ghost.FIRE_CYAN, Ghost.FIRE_PALE, Ghost.FIRE_WHITE));
		};
	}

	/**
	 * A ring of ghost fire on the ground (on a sheet lying level): a band from {@code inner} to {@code outer} of its half, licking in and out,
	 * hottest along its middle; clear elsewhere.
	 */
	static SpellArt.Painter fireRing(double inner, double outer, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			double lick = (outer - inner) * 0.35 * (SpellArt.noise(a * 7 + seed, 1, seed) - 0.3);
			double lo = inner - lick * 0.5, hi = outer + lick;
			if (r < lo || r > hi || r > 0.995) return SpellArt.CLEAR;
			double mid = 1 - Math.abs(r - (lo + hi) / 2) / ((hi - lo) / 2);
			return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01(mid * 1.1), Ghost.GHOST_FIRE), 0.3 + 0.65 * mid);
		};
	}

	/**
	 * The Yellow Springs' water (on a blade, its foot at v 0): murky yellow-green, darker low down and seen through more there, streaks of
	 * light running up its face as a wave's do, a bright band where it curls; its top breaking into foam, broken and ragged, the last of it lit
	 * with ghost light.
	 */
	static SpellArt.Painter springsWater(int seed) {
		return t -> {
			double v = t.v(), u = t.u(), n = SpellArt.fbm(u * 5 + seed, v * 4, seed, 3);
			if (v > 0.95 - 0.12 * SpellArt.noise(u * 13, seed, seed + 1)) return SpellArt.CLEAR;
			double streak = SpellArt.fbm(u * 16 + seed, v * 1.6 + 0.3 * n, seed + 5, 2), curl = Math.exp(-Math.pow((v - 0.66) / 0.08, 2));
			double foam = SpellArt.smooth(0.74, 0.9, v + 0.2 * (n - 0.5)) * SpellArt.smooth(0.3, 0.6, SpellArt.fbm(u * 9 + seed, v * 7, seed + 3, 2) + 0.3 * v);
			if (foam > 0.55) return v > 0.86 ? SpellArt.glow(SpellArt.mix(FOAM, Ghost.FIRE_PALE, 0.5), 0.85) : SpellArt.col(FOAM, 0.9);
			double light = SpellArt.clamp01(0.15 + 0.5 * v + 0.35 * (n - 0.5) + 0.5 * Math.max(0, streak - 0.55) + 0.25 * curl);
			int water = SpellArt.ramp(light, SPRINGS_DEEP, SPRINGS, SPRINGS_LIGHT, SPRINGS_PALE);
			return SpellArt.col(SpellArt.mix(water, FOAM, foam), 0.62 + 0.25 * v);
		};
	}

	/** Spray off the springs' water (on a blade, its foot at v 0): a plume of foam narrowing to nothing, thinning as it goes. */
	static SpellArt.Painter spray(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.46 * Math.pow(1 - v, 0.7) * (0.8 + 0.2 * SpellArt.noise(v * 9, seed, seed));
			if (Math.abs(u) > half || v > 0.95) return SpellArt.CLEAR;
			double n = SpellArt.fbm(u * 8 + seed, v * 6, seed + 1, 2);
			return SpellArt.col(SpellArt.mix(SPRINGS_PALE, FOAM, n), (0.9 - 0.6 * v) * (0.7 + 0.3 * n));
		};
	}

	/**
	 * The flood the tide spreads behind it (on a sheet lying level ahead of its caster, {@code w} across and {@code d} deep, its caster at the
	 * middle of its back edge): the springs' water in a fan sixty degrees either way out to {@code reach}, its edges ragged, thin and seen
	 * through toward its caster; foam in broken patches on it, glints of light (not bands: rings of foam read as the grain of wood).
	 */
	static SpellArt.Painter springsFlood(double w, double d, double reach, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * w, z = -d * (1 - t.back()), r = Math.hypot(x, z), ang = Math.toDegrees(Math.atan2(x, -z));
			double rag = SpellArt.noise(ang * 0.15 + seed, r * 0.2, seed);
			if (r > reach * (0.9 + 0.1 * rag) || Math.abs(ang) > 56 + 8 * rag || r < 1.5) return SpellArt.CLEAR;
			double n = SpellArt.fbm(x * 0.12 + seed, z * 0.12, seed + 2, 3), out = r / reach;
			double froth = SpellArt.fbm(x * 0.35 + seed, z * 0.35, seed + 4, 3) + 0.25 * out;
			if (froth > 0.8) return SpellArt.col(SpellArt.mix(SPRINGS_PALE, FOAM, froth - 0.8), 0.45 + 0.35 * out);
			double glint = SpellArt.noise(x * 0.9 + seed, z * 0.9, seed + 6);
			int water = SpellArt.ramp(SpellArt.clamp01(0.25 + 0.5 * n + (glint > 0.82 ? 0.35 : 0)), SPRINGS_DEEP, SPRINGS, SPRINGS_LIGHT, SPRINGS_PALE);
			return SpellArt.col(water, 0.22 + 0.45 * out);
		};
	}

	/** Bone of the dead steeped in the Yellow Springs (on a solid): ivory, lit on top and shaded under, a little mottled; seen through a little. */
	static SpellArt.Painter marrow(int seed) {
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.5, t.py * 0.5, t.pz * 0.5, seed, 2);
			double light = SpellArt.clamp01(0.52 + 0.3 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny) + (n - 0.5) * 0.5);
			return SpellArt.col(SpellArt.ramp(light, Ghost.BONE_DARK, Ghost.BONE_SHADE, Ghost.BONE, 0xFBF6E8), 0.92);
		};
	}

	/** Ghost fire in a joint of bone (on a solid): cyan to pale, glowing. */
	static SpellArt.Painter jointFire(int seed) {
		return t -> SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, Ghost.FIRE_PALE, SpellArt.noise(t.px * 2, t.py * 2 + t.pz, seed)), 0.95);
	}

	// --- Ghost Fire: the cold fire kindled in the hands ---

	/**
	 * Ghost Fire kindled in its caster's hands (drawn at their size; the hands' middle at the origin, a little before them): a knot of ghost fire
	 * in each ("fire_l", "fire_r"), three tongues and a heart.
	 */
	static void ghostKindle(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone hand = m.bone(s < 0 ? "fire_r" : "fire_l", "root", s * 3.2, 0, 0);
			hand.box(s * 3.2, 0.6, 0, 1.6, 1.6, 1.6, ghostCore(5101)).density(4);
			for (int k = 0; k < 3; k++) {
				double th = Math.toRadians(120 * k + (s > 0 ? 30 : 0));
				double[] way = Axis.unit(SpellArt.v3(Math.sin(th) * 0.35, 1, Math.cos(th) * 0.35));
				Axis.crossedTongue(m, hand, "kindle_" + k % 2, SpellArt.v3(s * 3.2, 0, 0), way, 6.5 - k, 3.2, Ghost.ghostFire(2, 5103 + k % 2), 4, k == 2);
			}
		}
		SpellArt.Anim kindle = m.anim("kindle", 0.3, null);
		for (String b : new String[] {"fire_l", "fire_r"}) {
			kindle.scale(b, 0, 0.1);
			kindle.scale(b, 0.12, 1.35);
			kindle.scale(b, 0.3, 1);
		}
		double T = 0.6;
		SpellArt.Anim burn = m.anim("burn", T, "true");
		burn.sample("fire_l", "scale", 0, T, 6, t -> new double[] {1, 1 + 0.15 * Axis.wave(t, T, 2, 0), 1});
		burn.sample("fire_r", "scale", 0, T, 6, t -> new double[] {1, 1 + 0.15 * Axis.wave(t, T, 2, 1.7), 1});
	}

	// --- Ghost Fire Bolt ---

	/**
	 * Ghost Fire Bolt's bolt (drawn at half the render scale, 2), its heart at the origin, flying toward -z: a small heart of white ghost fire
	 * ("core") wrapped in tongues of it, leaning back off its brow and streaming back from its sides ("flames"), a long tail of its fire behind
	 * ("tail").
	 */
	static void ghostBolt(SpellArt.Model m) {
		m.tpp = 2.5;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 0, 0);
		Immortal.ball(core, 0, 0, 0, 2.1, 1.05, ghostCore(5111), 4);
		SpellArt.Bone flames = m.bone("flames", "root", 0, 0, 0);
		// Off its brow, bent back over it; then round its sides, streaming back.
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k);
			double[] base = SpellArt.v3(Math.sin(th) * 1.2, Math.cos(th) * 1.2, -1.6), way = Axis.unit(SpellArt.v3(Math.sin(th) * 0.9, Math.cos(th) * 0.9, 0.6));
			Axis.crossedTongue(m, flames, "bolt_brow", base, way, 5.5, 3.6, Ghost.ghostFire(2, 5112), 3, k % 2 == 1);
		}
		for (int k = 0; k < 10; k++) {
			double th = Math.toRadians(45 * k + 10), out = k < 8 ? 0.62 : 0.15;
			double[] way = Axis.unit(SpellArt.v3(Math.sin(th) * out, Math.cos(th) * out, 1));
			Axis.crossedTongue(m, flames, "bolt_fire_" + k % 2, SpellArt.times(way, 1.2), way, k < 8 ? 9 + 2 * (k % 3) : 14, 4.2, Ghost.ghostFire(2, 5113 + k % 2), 2.5, k % 2 == 1);
		}
		SpellArt.Bone tail = m.bone("tail", "root", 0, 0, 2);
		for (int k = 0; k < 3; k++) {
			SpellArt.flatBlade(tail, SpellArt.v3(0, (k - 1) * 0.8, 2), 22 - 4 * Math.abs(k - 1), 4.4, (k - 1) * 6, (k - 1) * 12, (k - 1) * 30, Ghost.ghostFire(1.4, 5117 + k)).density(2);
		}
		double T = 0.4;
		SpellArt.Anim fly = m.anim("fly", T, "true");
		fly.sample("flames", "scale", 0, T, 6, t -> new double[] {1 + 0.1 * Axis.wave(t, T, 2, 0), 1 + 0.1 * Axis.wave(t, T, 2, 1), 1 + 0.18 * Axis.wave(t, T, 2, 0.5)});
		fly.sample("tail", "rotation", 0, T, 6, t -> new double[] {4 * Axis.wave(t, T, 1, 0), 9 * Axis.wave(t, T, 1, 1.2), 0});
		fly.sample("core", "scale", 0, T, 6, t -> Axis.same(1 + 0.08 * Axis.wave(t, T, 2, 0.3)));
		// It bursts where it strikes: its heart flaring and gone, its fire thrown wide.
		SpellArt.Anim burst = m.anim("burst", 0.3, "hold_on_last_frame");
		burst.sample("core", "scale", 0, 0.3, 4, t -> Axis.same(Math.max(0.02, 1 + 1.6 * Math.sin(Math.PI * Math.min(1, t / 0.24)) - 1.0 * SpellArt.smooth(0.12, 0.3, t))));
		burst.sample("flames", "scale", 0, 0.3, 4, t -> Axis.same(Math.max(0.02, (1 + 1.4 * SpellArt.easeOut(t / 0.3)) * (1 - SpellArt.smooth(0.15, 0.3, t)))));
		burst.sample("tail", "scale", 0, 0.3, 3, t -> Axis.same(Math.max(0.02, 1 - SpellArt.easeOut(t / 0.2))));
	}

	// --- Soul Dash: the wraith left behind ---

	/**
	 * Soul Dash's wraith, left where its caster stood (drawn at their size, two blocks tall; it faces -z, the way they rushed): a hood of mist
	 * and the hollow under it, a robe narrowing to a hem torn into wisps, sleeves streaming back ("body", leaning after them), a long wisp of a
	 * tail ("tail").
	 */
	static void soulWraith(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 4, 0, 16, 0, 0);
		SpellArt.Painter robe = Ghost.robe(5121);
		body.box(0, 26, -0.4, 6, 6.4, 5.6, Ghost.hood(5121)).density(4);
		body.box(0, 25.6, 2.4, 5.2, 6, 2.6, robe);
		body.box(0, 20, 0.2, 8.4, 4.6, 4.6, robe);
		body.box(0, 15.6, 0.4, 7, 4.8, 4.2, robe);
		body.box(0, 11.4, 0.6, 6, 4.2, 3.8, robe);
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(30 + k * 60), hx = Math.sin(th) * 2.8, hz = Math.cos(th) * 1.9;
			SpellArt.blade(body, SpellArt.v3(hx, 9.6, 0.6 + hz), 8, 3.6, 166 + (hz > 0 ? -14 : 0), Math.toDegrees(th) * 0.3, -Math.signum(hx) * 9, Ghost.wisp(5130 + k)).density(2.5);
		}
		for (int s = -1; s <= 1; s += 2) {
			double[] shoulder = SpellArt.v3(s * 4.6, 21, 0.4), wrist = SpellArt.v3(s * 4.2, 15.5, 6.5);
			SpellArt.limb(body, shoulder, wrist, 2.4, 2.6, robe);
			SpellArt.flatBlade(body, wrist, 7, 3.2, -14, s * 10, s * 20, Ghost.wisp(5136 + s)).density(2.5);
		}
		SpellArt.Bone tail = m.bone("tail", "body", 0, 10, 2);
		SpellArt.flatBlade(tail, SpellArt.v3(0, 10, 2), 20, 6, -18, 0, 0, Ghost.wisp(5140)).density(2);
		SpellArt.flatBlade(tail, SpellArt.v3(0, 11, 2), 15, 4.4, -10, 14, 60, Ghost.wisp(5141)).density(2);
		SpellArt.flatBlade(tail, SpellArt.v3(0, 11, 2), 15, 4.4, -10, -14, -60, Ghost.wisp(5142)).density(2);
		// Left behind (its enter): lunging after its caster, drawn out toward them, thinning.
		double L = 0.5;
		SpellArt.Anim left = m.anim("left", L, null);
		left.sample("body", "position", 0, L, 5, t -> new double[] {0, 0, -7 * SpellArt.easeOut(t / L)});
		left.sample("body", "scale", 0, L, 5, t -> new double[] {1 - 0.3 * SpellArt.easeOut(t / L), 1 - 0.2 * SpellArt.easeOut(t / L), 1 + 0.7 * SpellArt.easeOut(t / L)});
		left.sample("tail", "rotation", 0, L, 5, t -> new double[] {-10 * SpellArt.easeOut(t / L), 12 * Math.sin(Math.PI * t / L), 0});
		double T = 1;
		SpellArt.Anim drift = m.anim("drift", T, "true");
		drift.sample("body", "position", 0, T, 8, t -> new double[] {0, 0.5 * Axis.wave(t, T, 1, 0), -7});
		drift.sample("body", "scale", 0, T, 2, t -> new double[] {0.7, 0.8, 1.7});
		drift.sample("tail", "rotation", 0, T, 8, t -> new double[] {-10, 8 * Axis.wave(t, T, 1, 0.6), 0});
	}

	// --- Ghostly Wail ---

	static final int CRIES = 16;
	/** How far out its cries fly (the wail's reach, eight blocks, at a quarter block a pixel). */
	static final double CRY_REACH = 30;

	/**
	 * Ghostly Wail round its caster (their feet at the origin; it faces the way they look): a great face of the dead looming over them, wailing
	 * (a hood of mist and the hollow under it, its mouth open; wisps hanging from it: "face"); the dead's cries flying out from them every way
	 * ("cry_0"..: each a wisp, its head leading), a ripple of mist running out over the ground ("ripple").
	 */
	static void ghostWail(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone face = m.bone("face", "root", 0, 9, 0);
		face.box(0, 13.4, -0.4, 8, 9, 7.4, Ghost.hood(5151)).density(3);
		face.box(0, 12.8, 3.6, 7, 8, 2.6, Ghost.robe(5151));
		face.box(0, 18.4, 1, 5.4, 1.8, 5.4, Ghost.robe(5152));
		for (int k = 0; k < 8; k++) {
			double th = Math.toRadians(22.5 + k * 45), hx = Math.sin(th) * 3.4, hz = Math.cos(th) * 3;
			SpellArt.blade(face, SpellArt.v3(hx, 9.2, 0.4 + hz), 9 - (k % 2) * 2, 3.4, 172 + (hz > 0 ? -10 : 6), Math.toDegrees(th) * 0.25, -Math.signum(hx) * 10, Ghost.wisp(5160 + k)).density(2);
		}
		SpellArt.Painter cry = Ghost.wisp(5170);
		for (int k = 0; k < CRIES; k++) {
			double th = 2 * Math.PI * k / CRIES;
			double[] at = SpellArt.v3(Math.sin(th) * 3, 5 + (k % 3) * 1.2, -Math.cos(th) * 3), in = SpellArt.v3(-Math.sin(th), -0.05, Math.cos(th));
			SpellArt.Bone b = m.bone("cry_" + k, "root", at[0], at[1], at[2]);
			Axis.crossedTongue(m, b, "cry", at, in, 8, 3.6, cry, 2, k % 2 == 1);
		}
		SpellArt.Bone ripple = m.bone("ripple", "root", 0, 0, 0);
		SpellArt.sheet(ripple, 0, 0.3, 0, CRY_REACH * 2.1, CRY_REACH * 2.1, 0, 0, 0, mistRing(5180)).density(0.8);
		wail(m);
		echo(m);
	}

	/** A ring of the dead's mist on the ground (on a sheet lying level): a band at its edge, soft and broken, faintly lit; clear within. */
	static SpellArt.Painter mistRing(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			double band = 1 - Math.abs(r - 0.86) / 0.12;
			if (band <= 0 || r > 0.99) return SpellArt.CLEAR;
			double n = SpellArt.fbm(a * 3 + seed, r * 6, seed, 3);
			double alpha = SpellArt.clamp01(band * 1.4) * SpellArt.clamp01((n - 0.25) * 2.2);
			return alpha < 0.05 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(Ghost.MIST_SHADE, Ghost.FIRE_PALE, n), 0.45 * alpha);
		};
	}

	/** It wails (its enter): the face rising up over its caster and swelling as it cries out; the cries flying out to the wail's reach; the ripple running out. */
	static void wail(SpellArt.Model m) {
		double L = 0.7;
		SpellArt.Anim a = m.anim("wail", L, null);
		a.sample("face", "position", 0, 0.25, 4, t -> new double[] {0, -8 * (1 - SpellArt.easeOut(t / 0.25)), 0});
		a.scale("face", 0, 0.4);
		a.scale("face", 0.25, 1.15);
		a.scale("face", 0.4, 1);
		a.scale("face", 0.55, 1.06);
		a.scale("face", L, 1);
		for (int k = 0; k < CRIES; k++) {
			double th = 2 * Math.PI * k / CRIES, t0 = 0.12 + 0.01 * (k % 4);
			String b = "cry_" + k;
			a.key(b, "position", 0, 0, 0, 0);
			a.sample(b, "position", t0, L, 6, t -> {
				double f = SpellArt.easeOut((t - t0) / (L - t0)) * (CRY_REACH - 3);
				return new double[] {Math.sin(th) * f, 2 * f / CRY_REACH, -Math.cos(th) * f};
			});
			a.scale(b, 0, 0.02);
			a.scale(b, t0, 0.3);
			a.scale(b, t0 + 0.1, 1.1);
			a.scale(b, L - 0.12, 0.9);
			a.scale(b, L, 0.02);
		}
		a.sample("ripple", "scale", 0, L, 6, t -> new double[] {Math.max(0.05, SpellArt.easeOut(SpellArt.clamp01((t - 0.1) / (L - 0.1)))), 1, Math.max(0.05, SpellArt.easeOut(SpellArt.clamp01((t - 0.1) / (L - 0.1))))});
	}

	/** It echoes on (looped without a seam): the face swaying over its caster, wailing still; its cries gone out. */
	static void echo(SpellArt.Model m) {
		double T = 1.2;
		SpellArt.Anim a = m.anim("echo", T, "true");
		a.sample("face", "position", 0, T, 8, t -> new double[] {0, 0.5 * Axis.wave(t, T, 1, 0), 0});
		a.sample("face", "rotation", 0, T, 8, t -> new double[] {3 * Axis.wave(t, T, 1, 1), 0, 2.5 * Axis.wave(t, T, 1, 2.2)});
		for (int k = 0; k < CRIES; k++) {
			double th = 2 * Math.PI * k / CRIES, f = CRY_REACH - 3;
			for (double t : new double[] {0, T}) {
				a.key("cry_" + k, "position", t, Math.sin(th) * f, 2, -Math.cos(th) * f);
				a.scale("cry_" + k, t, 0.02);
			}
		}
		a.scale("ripple", 0, 1);
		a.scale("ripple", T, 1);
	}

	// --- Yellow Springs Tide ---

	static final int CRESTS = 7;
	/** The tide's reach (seven blocks at a quarter block a pixel), and where its crests rise from. */
	static final double TIDE_REACH = 28, TIDE_FROM = 4;

	/**
	 * Yellow Springs Tide out ahead of its caster (their feet at the origin; it runs toward -z, the way they look, sixty degrees either way): seven
	 * crests of the springs' murky yellow water ("crest_0".., each turned to its way, its wave "wave_0".. riding out along it), their tops
	 * breaking in pale foam, spray off them; the flood spread behind them ("flood"); hands of the dead reaching up out of it ("hand_0"..).
	 */
	static void springsTide(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone flood = m.bone("flood", "root", 0, 0, 0);
		double w = TIDE_REACH * 1.8, d = TIDE_REACH + 1;
		SpellArt.sheet(flood, 0, 0.25, -d / 2, w, d, 0, 0, 0, springsFlood(w, d, TIDE_REACH, 5201)).density(2);
		SpellArt.Painter water = springsWater(5203), spray = spray(5205);
		for (int k = 0; k < CRESTS; k++) {
			double th = -54 + 18 * k;
			m.bone("crest_" + k, "root", 0, 0, 0, 0, -th, 0);
			SpellArt.Bone wave = m.bone("wave_" + k, "crest_" + k, 0, 0, -TIDE_FROM);
			// Its face, leaning out the way it runs; its lip curling over and down; spray off the lip.
			double[] foot = SpellArt.v3(0, 0, -TIDE_FROM), top = SpellArt.add(foot, SpellArt.v3(0, 7 * Math.sin(Math.toRadians(108)), 7 * Math.cos(Math.toRadians(108))));
			SpellArt.shared(m, wave, "tide_face", true, foot, 7, 9, 108, 0, 0, water, 2, k % 2 == 1);
			SpellArt.shared(m, wave, "tide_lip", true, top, 4.2, 9, 205, 0, 0, water, 2, k % 2 == 0);
			for (int j = -1; j <= 1; j += 2) {
				double[] at = SpellArt.add(top, SpellArt.v3(j * 2.6, 0, -0.6));
				Axis.crossedTongue(m, wave, "tide_spray", at, Axis.unit(SpellArt.v3(j * 0.3, 1, -0.7)), 4.4, 2.4, spray, 2, j > 0);
			}
		}
		double[][] hands = {{-9, -17}, {7, -20}, {1, -13}};
		for (int i = 0; i < hands.length; i++) {
			double[] h = hands[i];
			SpellArt.Bone hand = m.bone("hand_" + i, "root", h[0], 0, h[1]);
			double[] wrist = SpellArt.v3(h[0] + 0.6, 3.2, h[1] - 0.8);
			SpellArt.limb(hand, SpellArt.v3(h[0], -1, h[1]), wrist, 1.2, 1.2, Ghost.robe(5210 + i));
			for (int f = 0; f < 4; f++) {
				double spread = (f - 1.5) * 0.55;
				SpellArt.limb(hand, wrist, SpellArt.add(wrist, SpellArt.v3(spread, 2.2 - Math.abs(spread) * 0.4, -0.5 - 0.3 * Math.abs(spread))), 0.42, 0.42, Ghost.deadHand(5214 + i)).density(5);
			}
		}
		tideSurge(m);
		tideEbb(m);
	}

	static final double SURGE = 0.5, SURGE_OUT = 0.42;

	/**
	 * It surges (its enter): each crest rising as it rides out to the tide's reach, widening as the arc it is part of does, and at its reach
	 * crashing over; the flood spreading behind them; the hands of the dead rising out of it.
	 */
	static void tideSurge(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("surge", SURGE, null);
		for (int k = 0; k < CRESTS; k++) {
			String b = "wave_" + k;
			a.sample(b, "position", 0, SURGE_OUT, 6, t -> new double[] {0, 0, -(TIDE_REACH - TIDE_FROM) * SpellArt.easeOut(t / SURGE_OUT)});
			a.sample(b, "scale", 0, SURGE_OUT, 6, t -> {
				double f = SpellArt.easeOut(t / SURGE_OUT);
				return new double[] {0.3 + 0.85 * f, 0.4 + 0.8 * Math.min(1, t / 0.3), 1};
			});
			a.sample(b, "scale", SURGE_OUT, SURGE, 2, t -> new double[] {1.15, 1.2 - 0.85 * SpellArt.easeIn((t - SURGE_OUT) / (SURGE - SURGE_OUT)), 1});
			a.rot(b, 0, 0, 0, 0);
			a.rot(b, SURGE_OUT, 0, 0, 0);
			a.sample(b, "rotation", SURGE_OUT, SURGE, 2, t -> new double[] {38 * SpellArt.easeIn((t - SURGE_OUT) / (SURGE - SURGE_OUT)), 0, 0});
		}
		a.sample("flood", "scale", 0, SURGE_OUT, 6, t -> {
			double f = Math.max(0.12, SpellArt.easeOut(t / SURGE_OUT));
			return new double[] {f, 1, f};
		});
		for (int i = 0; i < 3; i++) {
			double t0 = 0.22 + 0.06 * i;
			String b = "hand_" + i;
			a.scale(b, 0, 0.02);
			a.scale(b, t0, 0.02);
			a.sample(b, "scale", t0, SURGE, 3, t -> Axis.same(Math.max(0.02, SpellArt.easeOut((t - t0) / (SURGE - t0)))));
		}
	}

	/** It ebbs (looped without a seam): the crests broken low at the tide's reach, rocking; the hands clutching. */
	static void tideEbb(SpellArt.Model m) {
		double T = 1;
		SpellArt.Anim a = m.anim("ebb", T, "true");
		for (int k = 0; k < CRESTS; k++) {
			double ph = k * 0.9;
			String b = "wave_" + k;
			a.sample(b, "position", 0, T, 4, t -> new double[] {0, 0, -(TIDE_REACH - TIDE_FROM)});
			a.sample(b, "scale", 0, T, 8, t -> new double[] {1.15, 0.35 + 0.05 * Axis.wave(t, T, 1, ph), 1});
			a.sample(b, "rotation", 0, T, 8, t -> new double[] {38 + 4 * Axis.wave(t, T, 1, ph + 1), 0, 0});
		}
		a.scale("flood", 0, 1);
		a.scale("flood", T, 1);
		for (int i = 0; i < 3; i++) {
			double ph = i * 2.1;
			a.sample("hand_" + i, "rotation", 0, T, 8, t -> new double[] {10 * Axis.wave(t, T, 1, ph), 0, 6 * Axis.wave(t, T, 1, ph + 1.5)});
		}
	}

	// --- Marrow Ward ---

	static final int RIBS = 5;

	/**
	 * Marrow Ward round its caster (their feet at the origin; drawn at their size, two blocks tall; it faces -z, the way they face): a spine of
	 * the dead's bone down their back ("spine"), ribs curving round their chest from it, sloping down to a breastbone ("rib_0".., the breastbone
	 * "sternum"), shoulder plates of bone spiked at their tops over their arms ("shoulders"), ghost fire burning in its joints.
	 */
	static void marrowWard(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter bone = marrow(5301), fire = jointFire(5303);
		SpellArt.Bone spine = m.bone("spine", "root", 0, 11, 3.7);
		for (int k = 0; k < 8; k++) {
			double y = 12 + 1.8 * k;
			spine.box(0, y, 3.7, 2.4, 1.3, 2, bone);
			spine.box(0, y, 5.1, 0.8, 1, 1.4, bone);
			spine.box(0, y + 0.8, 3.7, 1.3, 0.4, 1.3, fire).density(4);
		}
		for (int j = 0; j < RIBS; j++) {
			double y = 23.6 - 2.2 * j, width = 5.0 + 0.15 * j, drop = 3.4;
			SpellArt.Bone rib = m.bone("rib_" + j, "root", 0, y, 3.4);
			for (int s = -1; s <= 1; s += 2) {
				double[] prev = null;
				for (int i = 0; i <= 6; i++) {
					double phi = Math.toRadians(16 + 149 * i / 6.0);
					double[] p = SpellArt.v3(s * width * Math.sin(phi), y - drop * Math.pow(i / 6.0, 1.5), 3.25 * Math.cos(phi));
					if (prev != null) SpellArt.limb(rib, prev, p, 0.85 - 0.04 * i, 1.05 - 0.05 * i, bone).density(4);
					prev = p;
				}
				rib.box(s * 1.5, y, 3.3, 0.9, 0.9, 0.9, fire).density(4);
			}
		}
		SpellArt.Bone sternum = m.bone("sternum", "root", 0, 24, -3.5);
		sternum.box(0, 18.6, -3.5, 1.5, 9.6, 0.9, bone);
		sternum.box(0, 23.6, -3.5, 2.4, 1.1, 1, bone);
		SpellArt.Bone shoulders = m.bone("shoulders", "root", 0, 25, 0);
		for (int s = -1; s <= 1; s += 2) {
			shoulders.box(s * 6, 25.3, 0, 5, 1.2, 5.2, bone).rot(0, 0, -s * 18);
			shoulders.box(s * 8.2, 23.6, 0, 1.2, 3, 4.6, bone).rot(0, 0, -s * 12);
			SpellArt.limb(shoulders, SpellArt.v3(s * 6.4, 25.8, 0.4), SpellArt.v3(s * 8, 29, 1.2), 0.9, 0.9, bone).density(4);
			SpellArt.limb(shoulders, SpellArt.v3(s * 5, 25.6, 0.4), SpellArt.v3(s * 5.6, 27.6, 0.9), 0.7, 0.7, bone).density(4);
			shoulders.box(s * 4.2, 26.1, 0, 0.9, 0.9, 0.9, fire).density(4);
		}
		wardForm(m);
		wardHold(m);
		wardCrumble(m);
	}

	/** It forms (its enter): the spine growing up their back, the ribs closing round them one by one from the top, the breastbone and shoulders after. */
	static void wardForm(SpellArt.Model m) {
		double L = 0.55;
		SpellArt.Anim a = m.anim("form", L, null);
		a.sample("spine", "scale", 0, 0.22, 3, t -> new double[] {1, Math.max(0.05, SpellArt.easeOut(t / 0.22)), 1});
		a.scale("spine", L, 1);
		for (int j = 0; j < RIBS; j++) {
			double t0 = 0.12 + 0.05 * j;
			String b = "rib_" + j;
			a.scale(b, 0, 0.02);
			a.scale(b, t0, 0.02);
			a.scale(b, t0 + 0.1, 1.08, 1, 1.08);
			a.scale(b, Math.min(L, t0 + 0.16), 1);
			a.scale(b, L, 1);
		}
		a.scale("sternum", 0, 0.02);
		a.scale("sternum", 0.38, 0.02);
		a.scale("sternum", 0.48, 1);
		a.scale("sternum", L, 1);
		a.scale("shoulders", 0, 0.02);
		a.scale("shoulders", 0.3, 0.02);
		a.scale("shoulders", 0.42, 1.12);
		a.scale("shoulders", L, 1);
	}

	/** It holds (looped without a seam): the ribs swelling and settling a little, as breath. */
	static void wardHold(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("hold", T, "true");
		for (int j = 0; j < RIBS; j++) {
			double ph = -0.25 * j;
			a.sample("rib_" + j, "scale", 0, T, 8, t -> new double[] {1 + 0.03 * Axis.wave(t, T, 1, ph), 1, 1 + 0.03 * Axis.wave(t, T, 1, ph)});
		}
		a.sample("shoulders", "position", 0, T, 8, t -> new double[] {0, 0.2 * Axis.wave(t, T, 1, 0.5), 0});
	}

	/** It crumbles (played once, held): the bones falling away from them and sinking. */
	static void wardCrumble(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("crumble", L, "hold_on_last_frame");
		for (int j = 0; j < RIBS; j++) {
			double t0 = 0.03 * j;
			a.sample("rib_" + j, "position", 0, L, 4, t -> new double[] {0, -9 * SpellArt.easeIn(SpellArt.clamp01((t - t0) / (L - t0))), 0});
			a.sample("rib_" + j, "scale", 0, L, 4, t -> new double[] {1 + 0.3 * SpellArt.clamp01(t / L), 1, 1 + 0.3 * SpellArt.clamp01(t / L)});
		}
		a.sample("shoulders", "position", 0, L, 4, t -> new double[] {0, -12 * SpellArt.easeIn(t / L), 0});
		a.sample("sternum", "position", 0, L, 4, t -> new double[] {0, -10 * SpellArt.easeIn(t / L), 0});
		a.sample("spine", "scale", 0, L, 4, t -> new double[] {1, Math.max(0.05, 1 - SpellArt.easeIn(t / L)), 1});
	}

	// --- Ghost Core Burst ---

	static final int BURST_TONGUES = 24;

	/**
	 * Ghost Core Burst round its caster (their feet at the origin): their core's ghost fire flaring out of their breast, a heart of white fire
	 * ("core"), tongues of it thrown out every way ("flames"), a ring of it running out over the ground to the burst's reach ("ring").
	 */
	static void coreBurst(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 4.4, 0);
		Immortal.ball(core, 0, 4.4, 0, 3, 1.5, ghostCore(5401), 3);
		SpellArt.Bone flames = m.bone("flames", "root", 0, 4.4, 0);
		double[][] ways = Axis.sphereWays(BURST_TONGUES, 0.4);
		for (int k = 0; k < BURST_TONGUES; k++) {
			double[] way = ways[k];
			if (way[1] < -0.55) way = Axis.unit(SpellArt.v3(way[0], -0.55, way[2]));
			Axis.crossedTongue(m, flames, "burst_fire_" + k % 3, SpellArt.add(SpellArt.v3(0, 4.4, 0), SpellArt.times(way, 2.4)), way, 10 + 3 * (k % 3), 4.6,
					Ghost.ghostFire(2, 5403 + k % 3), 2, k % 2 == 1);
		}
		SpellArt.Bone ring = m.bone("ring", "root", 0, 0, 0);
		SpellArt.sheet(ring, 0, 0.3, 0, 44, 44, 0, 0, 0, fireRing(0.78, 0.94, 5407)).density(1.2);
		double L = 0.4;
		SpellArt.Anim burst = m.anim("burst", L, null);
		burst.scale("core", 0, 0.2);
		burst.scale("core", 0.08, 1.7);
		burst.scale("core", L, 0.7);
		burst.sample("flames", "scale", 0, L, 5, t -> Axis.same(Math.max(0.1, 1.35 * SpellArt.easeOut(Math.min(1, t / 0.18)) - 0.3 * SpellArt.smooth(0.18, L, t))));
		burst.sample("ring", "scale", 0, L, 5, t -> {
			double f = Math.max(0.08, SpellArt.easeOut(SpellArt.clamp01((t - 0.04) / 0.3)));
			return new double[] {f, 1, f};
		});
		double T = 0.6;
		SpellArt.Anim smoulder = m.anim("smoulder", T, "true");
		smoulder.sample("flames", "scale", 0, T, 6, t -> new double[] {1.05 + 0.06 * Axis.wave(t, T, 2, 0), 1.05 + 0.08 * Axis.wave(t, T, 2, 1), 1.05 + 0.06 * Axis.wave(t, T, 2, 2)});
		smoulder.sample("core", "scale", 0, T, 6, t -> Axis.same(0.7 + 0.06 * Axis.wave(t, T, 2, 0.5)));
		smoulder.scale("ring", 0, 1);
		smoulder.scale("ring", T, 1);
	}

	// --- King's Decree ---

	/**
	 * A Ghost King's decree (on a sheet standing, its face toward -z): bone-white silk framed in black lacquer edged with ghost-fire thread,
	 * three columns of the dead's glyphs in cinnabar down it, the king's square seal stamped in its foot by the last of them.
	 */
	static SpellArt.Painter decreeSilk(int seed) {
		return t -> {
			double u = t.across(), v = 1 - t.back(), edge = Math.min(Math.min(u, 1 - u) * 1.8, Math.min(v, 1 - v));
			if (edge < 0.035) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, Ghost.FIRE_PALE, 0.3), 0.95);
			if (edge < 0.07) return SpellArt.col(Ghost.LACQUER);
			// The seal: a square of cinnabar low in its last column, its glyph cut in it (the silk showing through).
			double su = (u - 0.12) / 0.16, sv = (v - 0.66) / 0.24;
			if (su >= 0 && su <= 1 && sv >= 0 && sv <= 1) {
				boolean cut = su > 0.12 && su < 0.88 && sv > 0.12 && sv < 0.88 && Fire.glyph(seed + 7, seed, (su - 0.12) / 0.76, (sv - 0.12) / 0.76);
				return cut ? SpellArt.col(Ghost.BONE) : SpellArt.glow(SpellArt.mix(Ghost.CINNABAR, 0xB8202A, 0.6), 0.97);
			}
			// Three columns, right to left as a decree is written, four glyphs down each.
			for (int c = 0; c < 3; c++) {
				double cu = (u - (0.74 - 0.27 * c)) / 0.17;
				if (cu < 0 || cu > 1 || v < 0.12 || v > (c == 2 ? 0.6 : 0.88)) continue;
				double cell = (v - 0.12) / 0.19;
				int ci = (int) Math.floor(cell);
				if (Fire.glyph(seed + c * 5 + ci, seed, cu, cell - ci)) return SpellArt.col(SpellArt.mix(Ghost.CINNABAR, 0x8A141C, 0.5));
			}
			return SpellArt.col(SpellArt.mix(Ghost.BONE, 0xF8F2E0, 0.3 + 0.4 * SpellArt.noise(u * 9, v * 5, seed)));
		};
	}

	/** A rod of a scroll (on a box standing): black lacquer, a sheen down its front; its two ends (caps) bone. */
	static SpellArt.Painter scrollRod() {
		return t -> {
			double up = t.up();
			if (up < 0.09 || up > 0.91) return SpellArt.col(SpellArt.mix(Ghost.BONE_SHADE, Ghost.BONE, 0.5 + 0.4 * Math.max(0, t.ny)));
			return SpellArt.col(SpellArt.mix(Ghost.LACQUER, Ghost.LACQUER_LIGHT, t.f == SpellArt.Face.FRONT ? 0.7 : 0.25));
		};
	}

	/** 王, king: its three bars and the stroke joining them (Fire#STROKES). */
	static final int[] KING = {0, 1, 2, 3, 4, 5, 8, 9};

	/**
	 * A Ghost King's seal on the ground (on a sheet lying level): a square frame of cinnabar round a ring of the dead's glyphs in ghost fire,
	 * 王 at its heart; clear elsewhere.
	 */
	static SpellArt.Painter kingSeal(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x), sq = Math.max(Math.abs(x), Math.abs(z));
			if (sq > 0.97) return SpellArt.CLEAR;
			if (sq > 0.88) return SpellArt.glow(SpellArt.mix(Ghost.CINNABAR, 0xC02028, 0.5), 0.9);
			if (Math.abs(r - 0.78) < 0.02 || Math.abs(r - 0.5) < 0.02) return SpellArt.glow(Ghost.FIRE_CYAN, 0.85);
			if (r > 0.52 && r < 0.76) {
				double cells = 12, f = (a / (2 * Math.PI) + 1) % 1 * cells;
				int ci = (int) Math.floor(f);
				if (Fire.glyph(ci + seed, seed, f - ci, (r - 0.52) / 0.24)) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_TEAL, Ghost.FIRE_PALE, 0.5), 0.92);
				return SpellArt.CLEAR;
			}
			if (r < 0.34) {
				double gx = (x + 0.34) / 0.68, gz = (z + 0.34) / 0.68;
				if (Fire.strokes(KING, gx, gz)) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, Ghost.FIRE_WHITE, 0.5), 0.95);
			}
			return SpellArt.CLEAR;
		};
	}

	/**
	 * King's Decree over its caster (their feet at the origin; drawn at half the render scale, 2; it faces the way they look): a Ghost King's
	 * decree unrolled in the air over them, its silk ("silk") between its two rods ("rod_l", "rod_r"), a faint light behind it ("glow"); the
	 * king's seal stamped on the ground under them ("seal").
	 */
	static void kingsDecree(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		double y = 27, z = -5, w = 30, h = 17;
		SpellArt.Bone scroll = m.bone("scroll", "root", 0, y, z);
		SpellArt.Bone silk = m.bone("silk", "scroll", 0, y, z);
		SpellArt.sheet(silk, 0, y, z, w, h, 90, 0, 0, decreeSilk(5501)).density(3);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.Bone rod = m.bone(s < 0 ? "rod_r" : "rod_l", "scroll", s * (w / 2 + 0.6), y, z);
			rod.box(s * (w / 2 + 0.6), y, z, 1.6, h + 3.4, 1.6, scrollRod()).density(4);
		}
		SpellArt.Bone glow = m.bone("glow", "scroll", 0, y, z + 0.6);
		SpellArt.sheet(glow, 0, y, z + 0.6, w + 10, h + 10, 90, 0, 0, Axis.haze(Ghost.FIRE_PALE, Ghost.FIRE_TEAL, 0.18)).density(1);
		SpellArt.Bone seal = m.bone("seal", "root", 0, 0, 0);
		SpellArt.sheet(seal, 0, 0.3, 0, 48, 48, 0, 0, 0, kingSeal(5505)).density(1.5);
		// It unrolls (its enter): the rods drawn apart from its middle, the silk opening between them; the seal stamped.
		double L = 0.5;
		SpellArt.Anim unroll = m.anim("unroll", L, null);
		unroll.sample("scroll", "position", 0, 0.2, 3, t -> new double[] {0, -6 * (1 - SpellArt.easeOut(t / 0.2)), 0});
		unroll.sample("silk", "scale", 0, L, 5, t -> new double[] {Math.max(0.04, SpellArt.ease(SpellArt.clamp01((t - 0.1) / 0.35))), 1, 1});
		for (int s = -1; s <= 1; s += 2) {
			double sign = s;
			unroll.sample(s < 0 ? "rod_r" : "rod_l", "position", 0, L, 5, t -> new double[] {-sign * (w / 2 + 0.6) * (1 - SpellArt.ease(SpellArt.clamp01((t - 0.1) / 0.35))), 0, 0});
		}
		unroll.sample("glow", "scale", 0, L, 4, t -> new double[] {Math.max(0.04, SpellArt.ease(SpellArt.clamp01((t - 0.15) / 0.35))), 1, 1});
		unroll.scale("seal", 0, 0.1);
		unroll.scale("seal", 0.12, 1.15);
		unroll.scale("seal", 0.25, 1);
		unroll.scale("seal", L, 1);
		double T = 1.4;
		SpellArt.Anim hold = m.anim("hold", T, "true");
		hold.sample("scroll", "position", 0, T, 8, t -> new double[] {0, 0.6 * Axis.wave(t, T, 1, 0), 0});
		hold.sample("scroll", "rotation", 0, T, 8, t -> new double[] {1.5 * Axis.wave(t, T, 1, 1.2), 0, 0});
		hold.sample("glow", "scale", 0, T, 8, t -> Axis.same(1 + 0.05 * Axis.wave(t, T, 2, 0)));
	}

	// --- Nine Nether Step ---

	/**
	 * A whirl of ghost fire turning in the ground (on a sheet lying level; Ghost#vortex is for one standing): {@code arms} arms wound in round a
	 * dark heart, clear outside its round.
	 */
	static SpellArt.Painter whirl(int arms, double twist, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.99) return SpellArt.CLEAR;
			double ang = Math.atan2(y, x) / (2 * Math.PI), s = ang * arms + Math.log(Math.max(0.06, r)) * twist + 0.15 * SpellArt.noise(r * 6, ang * 8, seed);
			double arm = Math.pow(Math.abs(Math.sin((s - Math.floor(s)) * Math.PI)), 3);
			double heat = SpellArt.clamp01(arm * (1.2 - 0.5 * r) + 0.15 * (1 - r));
			if (heat < 0.14) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.ramp(heat, Ghost.GHOST_FIRE), (0.35 + 0.6 * heat) * SpellArt.clamp01((0.99 - r) / 0.2));
		};
	}

	/** The dark under a way into the Nine Nethers (on a sheet lying level): black, softening at its rim to nothing. */
	static SpellArt.Painter netherDark() {
		return t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 0.97) return SpellArt.CLEAR;
			return SpellArt.col(Ghost.VOID, 0.92 * SpellArt.clamp01((0.97 - r) / 0.25));
		};
	}

	/**
	 * Nine Nether Step's way through the dark, where its caster steps in and where they step out (their feet at the origin; drawn at their
	 * size): a pool of the Nine Nethers' dark opened in the ground, a whirl of ghost fire turning in it ("pool"), ghost fire licking up round
	 * its rim ("wisps"), hands of the dead reaching up out of it ("hand_0"..).
	 */
	static void netherPool(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone pool = m.bone("pool", "root", 0, 0, 0);
		SpellArt.sheet(pool, 0, 0.2, 0, 40, 40, 0, 0, 0, netherDark()).density(1);
		SpellArt.sheet(pool, 0, 0.35, 0, 46, 46, 0, 0, 0, whirl(5, 1.6, 5601)).density(1.6);
		SpellArt.Bone wisps = m.bone("wisps", "root", 0, 0, 0);
		for (int k = 0; k < 12; k++) {
			double th = 2 * Math.PI * k / 12 + 0.2;
			double[] base = SpellArt.v3(Math.sin(th) * 18, 0.3, Math.cos(th) * 18), way = Axis.unit(SpellArt.v3(-Math.sin(th) * 0.2, 1, -Math.cos(th) * 0.2));
			Axis.crossedTongue(m, wisps, "pool_fire_" + k % 2, base, way, 8 + 3 * (k % 3), 4.4, Ghost.ghostFire(2, 5603 + k % 2), 2, k % 3 == 1);
		}
		double[][] hands = {{-6, 3}, {5, -5}, {2, 7}};
		for (int i = 0; i < hands.length; i++) {
			double[] h = hands[i];
			SpellArt.Bone hand = m.bone("hand_" + i, "root", h[0], 0, h[1]);
			double[] wrist = SpellArt.v3(h[0] * 0.8, 9, h[1] * 0.8);
			SpellArt.limb(hand, SpellArt.v3(h[0], -2, h[1]), wrist, 1.8, 1.8, Ghost.robe(5610 + i));
			for (int f = 0; f < 4; f++) {
				double spread = (f - 1.5) * 0.8;
				double[] dir = Axis.unit(SpellArt.v3(-h[0], 0, -h[1]));
				double[] tip = SpellArt.add(wrist, SpellArt.add(SpellArt.v3(dir[2] * spread, 3.4 - Math.abs(spread) * 0.5, -dir[0] * spread), SpellArt.times(dir, 1.2)));
				SpellArt.limb(hand, wrist, tip, 0.6, 0.6, Ghost.deadHand(5614 + i)).density(5);
			}
		}
		// It opens (its enter): the pool spreading, the wisps rising off it, the hands reaching up.
		double L = 0.2;
		SpellArt.Anim open = m.anim("open", L, null);
		open.sample("pool", "scale", 0, L, 4, t -> new double[] {Math.max(0.05, SpellArt.easeOut(t / L)), 1, Math.max(0.05, SpellArt.easeOut(t / L))});
		open.sample("wisps", "scale", 0, L, 4, t -> new double[] {1, Math.max(0.05, SpellArt.easeOut(t / L)), 1});
		for (int i = 0; i < hands.length; i++) {
			open.scale("hand_" + i, 0, 0.02);
			open.scale("hand_" + i, 0.08, 0.02);
			open.scale("hand_" + i, L, 1);
		}
		double T = 1;
		SpellArt.Anim swirl = m.anim("swirl", T, "true");
		swirl.sample("pool", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		swirl.sample("wisps", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.12 * Axis.wave(t, T, 2, 0), 1});
		for (int i = 0; i < hands.length; i++) {
			double ph = i * 2.1;
			swirl.sample("hand_" + i, "rotation", 0, T, 8, t -> new double[] {9 * Axis.wave(t, T, 1, ph), 0, 7 * Axis.wave(t, T, 1, ph + 1.3)});
		}
		// It closes (played once, held): the hands sinking back, the pool and its wisps drawn in after them.
		double C = 0.25;
		SpellArt.Anim close = m.anim("close", C, "hold_on_last_frame");
		close.sample("pool", "scale", 0, C, 4, t -> new double[] {Math.max(0.05, 1 - SpellArt.easeIn(t / C)), 1, Math.max(0.05, 1 - SpellArt.easeIn(t / C))});
		close.sample("wisps", "scale", 0, C, 4, t -> Axis.same(Math.max(0.05, 1 - SpellArt.easeIn(t / C))));
		for (int i = 0; i < hands.length; i++) close.sample("hand_" + i, "position", 0, C, 3, t -> new double[] {0, -12 * SpellArt.easeIn(t / C), 0});
	}

	// --- Yama's Verdict ---

	/**
	 * Yama's tablet (on its body, its broad faces front and back): black lacquer framed in gold, one great glyph of judgement down its middle in
	 * cinnabar burning; gold on its edges.
	 */
	static SpellArt.Painter tablet(int seed) {
		return t -> {
			boolean face = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK;
			if (!face) return SpellArt.col(SpellArt.mix(HeavenDemon.GOLD_DEEP, HeavenDemon.GOLD, 0.45 + 0.4 * SpellArt.noise(t.px * 0.8, t.py * 0.8, seed)));
			double u = t.across(), v = t.up(), edge = Math.min(Math.min(u, 1 - u) * t.sx(), Math.min(v, 1 - v) * t.sy());
			if (edge < 0.7) return SpellArt.col(SpellArt.mix(HeavenDemon.GOLD, HeavenDemon.GOLD_BRIGHT, 0.4));
			if (edge < 1.1) return SpellArt.col(Ghost.LACQUER);
			if (edge < 1.4) return SpellArt.col(HeavenDemon.GOLD_DEEP);
			double gu = (u - 0.22) / 0.56, gv = (v - 0.26) / 0.58;
			if (t.sy() > 8 && gu >= 0 && gu <= 1 && gv >= 0 && gv <= 1) {
				int cell = gv < 0.5 ? 1 : 0;
				double cv = gv < 0.5 ? gv * 2 : gv * 2 - 1;
				if (Fire.glyph(seed + cell, seed, gu, cv)) return SpellArt.glow(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.EMBER, 0.35 + 0.3 * cv), 0.97);
			}
			return SpellArt.col(SpellArt.mix(Ghost.LACQUER, Ghost.LACQUER_LIGHT, 0.15 + 0.35 * SpellArt.noise(u * 3 + seed, v * 24, seed)));
		};
	}

	/** Cracks in the ground (on a sheet lying level): dark rifts running out from its middle, ghost fire glowing in their depths; clear between. */
	static SpellArt.Painter netherCracks(int seed, int rifts) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.97) return SpellArt.CLEAR;
			double best = 9;
			for (int k = 0; k < rifts; k++) {
				double ak = 2 * Math.PI * (k + 0.4 * SpellArt.hash(k, 1, seed)) / rifts + 0.5 * (SpellArt.noise(r * 4 + k * 3, k, seed) - 0.5) * r;
				double reach = 0.55 + 0.45 * SpellArt.hash(k, 2, seed);
				if (r > reach) continue;
				best = Math.min(best, Math.abs(Math.atan2(Math.sin(a - ak), Math.cos(a - ak))) * r);
			}
			double w = 0.04 * (1 - r) + 0.01;
			if (r < 0.14) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, Ghost.FIRE_DEEP, r / 0.14), 0.7);
			if (best < w * 0.55) return SpellArt.glow(SpellArt.ramp(1 - best / (w * 0.55), Ghost.FIRE_DEEP, Ghost.FIRE_TEAL, Ghost.FIRE_CYAN), 0.95);
			if (best < w) return SpellArt.col(Ghost.VOID, 0.9);
			return SpellArt.CLEAR;
		};
	}

	/**
	 * Yama's Verdict on one being (its place at the origin; drawn at half the render scale, 2; it faces the caster): Yama's tablet of judgement
	 * cast down from on high and driven into the ground behind it, leaning back ("tablet"), the ground cracked round its foot, ghost fire in the
	 * cracks ("cracks").
	 */
	static void yamaTablet(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone tablet = m.bone("tablet", "root", 0, 0, 0, -8, 0, 0);
		SpellArt.Painter p = tablet(5701);
		tablet.box(0, 8, 0, 12, 24, 2.4, p);
		tablet.box(0, 20.9, 0, 10, 1.8, 2.4, p);
		tablet.box(0, 22.3, 0, 7, 1.2, 2.4, p);
		SpellArt.Bone cracks = m.bone("cracks", "root", 0, 0, 0);
		SpellArt.sheet(cracks, 0, 0.3, 0, 40, 40, 0, 0, 0, netherCracks(5703, 9)).density(1.5);
		// It is cast down (its enter): falling out of the sky and driven in; the ground cracking round it.
		double L = 0.3, LAND = 0.16;
		SpellArt.Anim slam = m.anim("slam", L, null);
		slam.sample("tablet", "position", 0, LAND, 4, t -> new double[] {0, 56 * (1 - SpellArt.easeIn(t / LAND)), 0});
		slam.pos("tablet", 0.2, 0, -1.2, 0);
		slam.pos("tablet", 0.25, 0, 0, 0);
		slam.pos("tablet", L, 0, 0, 0);
		slam.sample("tablet", "rotation", 0, LAND, 4, t -> new double[] {-18 * (1 - t / LAND), 0, 6 * (1 - t / LAND)});
		slam.rot("tablet", L, 0, 0, 0);
		slam.scale("cracks", 0, 0.02);
		slam.scale("cracks", LAND, 0.02);
		slam.sample("cracks", "scale", LAND, L, 3, t -> {
			double f = Math.max(0.02, SpellArt.easeOut((t - LAND) / (L - LAND)));
			return new double[] {f, 1, f};
		});
		double T = 1;
		SpellArt.Anim stand = m.anim("stand", T, "true");
		stand.pos("tablet", 0, 0, 0, 0);
		stand.pos("tablet", T, 0, 0, 0);
		stand.scale("cracks", 0, 1);
		stand.scale("cracks", T, 1);
		// It sinks (played once, held): drawn down into the ground it was driven into.
		double S = 0.5;
		SpellArt.Anim sink = m.anim("sink", S, "hold_on_last_frame");
		sink.sample("tablet", "position", 0, S, 4, t -> new double[] {0, -26 * SpellArt.easeIn(t / S), 0});
		sink.sample("cracks", "scale", 0, S, 3, t -> {
			double f = Math.max(0.3, 1 - 0.7 * SpellArt.clamp01(t / S));
			return new double[] {f, 1, f};
		});
	}

	// --- Chains of Hell: the way to hell they come out of ---

	/**
	 * A seal on the ground over a way down to hell (on a sheet lying level): a ragged rift of the dark at its heart, ghost fire licking up its
	 * edges; round it a ring of cinnabar glyphs between two threads of ghost fire; clear elsewhere.
	 */
	static SpellArt.Painter hellSeal(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.96) return SpellArt.CLEAR;
			if (Math.abs(r - 0.93) < 0.025 || Math.abs(r - 0.66) < 0.02) return SpellArt.glow(SpellArt.mix(Ghost.FIRE_TEAL, Ghost.FIRE_CYAN, 0.5), 0.9);
			if (r > 0.69 && r < 0.9) {
				double cells = 10, f = (a / (2 * Math.PI) + 1) % 1 * cells;
				int ci = (int) Math.floor(f);
				if (Fire.glyph(ci + seed, seed, f - ci, (r - 0.69) / 0.21)) return SpellArt.glow(SpellArt.mix(Ghost.CINNABAR, 0xC8242E, 0.6), 0.95);
				return SpellArt.CLEAR;
			}
			double rift = 0.38 + 0.12 * SpellArt.noise(a * 3 + seed, 1, seed) + 0.06 * SpellArt.noise(a * 9, 2, seed);
			if (r < rift - 0.05) return SpellArt.col(Ghost.VOID, 0.95);
			if (r < rift + 0.03) return SpellArt.glow(SpellArt.ramp(SpellArt.clamp01((rift + 0.03 - r) / 0.08), Ghost.FIRE_DEEP, Ghost.FIRE_TEAL, Ghost.FIRE_CYAN, Ghost.FIRE_PALE), 0.92);
			return SpellArt.CLEAR;
		};
	}

	/**
	 * Chains of Hell's seal at its caster's feet (their feet at the origin; drawn at half the render scale, 2): a way down to hell torn open in the
	 * ground under a ring of its glyphs ("seal"), the chains lashing up out of it (SpellConstruct HELL_CHAIN, drawn by hand); tongues of ghost
	 * fire licking up round its rift ("flames").
	 */
	static void hellGate(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone seal = m.bone("seal", "root", 0, 0, 0);
		SpellArt.sheet(seal, 0, 0.3, 0, 40, 40, 0, 0, 0, hellSeal(5801)).density(2);
		SpellArt.Bone flames = m.bone("flames", "root", 0, 0, 0);
		for (int k = 0; k < 8; k++) {
			double th = 2 * Math.PI * k / 8 + 0.3;
			double[] base = SpellArt.v3(Math.sin(th) * 7.5, 0.3, Math.cos(th) * 7.5), way = Axis.unit(SpellArt.v3(Math.sin(th) * 0.2, 1, Math.cos(th) * 0.2));
			Axis.crossedTongue(m, flames, "gate_fire", base, way, 7 + 2 * (k % 3), 4, Ghost.ghostFire(2, 5803), 2, k % 2 == 1);
		}
		SpellArt.Anim open = m.anim("open", 0.3, null);
		open.sample("seal", "scale", 0, 0.3, 4, t -> {
			double f = Math.max(0.05, SpellArt.easeOut(t / 0.3));
			return new double[] {f, 1, f};
		});
		open.sample("flames", "scale", 0, 0.3, 4, t -> new double[] {1, Math.max(0.05, SpellArt.easeOut(SpellArt.clamp01((t - 0.1) / 0.2))), 1});
		double T = 2;
		SpellArt.Anim burn = m.anim("burn", T, "true");
		burn.sample("seal", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		burn.sample("flames", "scale", 0, T, 12, t -> new double[] {1, 1 + 0.18 * Axis.wave(t, T, 5, 0), 1});
	}

	// --- Wheel of Samsara ---

	/** A path's medallion on the wheel (on a sheet standing): round, a rim of gold, the path's colour within, a glyph of it in white light. */
	static SpellArt.Painter pathDisc(int path, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98) return SpellArt.CLEAR;
			if (r > 0.82) return SpellArt.glow(SpellArt.mix(HeavenDemon.GOLD, HeavenDemon.GOLD_BRIGHT, 0.5), 0.97);
			if (r < 0.6 && Fire.glyph(seed + path, seed, (x + 0.6) / 1.2, (y + 0.6) / 1.2)) return SpellArt.glow(path == 0 ? HeavenDemon.GOLD_DEEP : HeavenDemon.WHITE_LIGHT);
			int c = PATHS[path];
			return path == 5 && r > 0.7 ? SpellArt.glow(Ghost.FIRE_CYAN, 0.9) : SpellArt.glow(SpellArt.mix(SpellArt.shade(c, -0.25), c, 1 - r));
		};
	}

	/** The wheel's hub (on a sheet standing): a round of white-gold light, the wheel's own glyph dark at its heart, a rim of gold. */
	static SpellArt.Painter wheelHub(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, y = (t.back() - 0.5) * 2, r = Math.hypot(x, y);
			if (r > 0.98) return SpellArt.CLEAR;
			if (r > 0.84) return SpellArt.glow(HeavenDemon.GOLD, 0.97);
			if (r < 0.62 && Fire.glyph(seed, seed, (x + 0.62) / 1.24, (y + 0.62) / 1.24)) return SpellArt.glow(HeavenDemon.GOLD_DEEP);
			return SpellArt.glow(SpellArt.mix(HeavenDemon.WHITE_LIGHT, HeavenDemon.GOLD_PALE, r));
		};
	}

	static final double WHEEL_Y = 18, WHEEL_Z = 7, WHEEL_R = 17;

	/**
	 * The Wheel of Samsara behind its caster (their feet at the origin; drawn at half the render scale, 2; it faces the way they face): a great
	 * wheel standing at their back, a rim of gold ("rim"), six spokes of bone out from a hub of white-gold light, each ending in a path's
	 * medallion ("wheel": it turns), a faint light behind it ("halo").
	 */
	static void samsaraWheel(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone wheel = m.bone("wheel", "root", 0, WHEEL_Y, WHEEL_Z);
		SpellArt.Bone rim = m.bone("rim", "wheel", 0, WHEEL_Y, WHEEL_Z, 90, 0, 0);
		Vessel.ring(rim, 0, WHEEL_Y, WHEEL_Z, WHEEL_R, 1.6, 28, HeavenDemon.heavenly(5901));
		Vessel.ring(rim, 0, WHEEL_Y, WHEEL_Z, WHEEL_R - 3, 0.8, 24, HeavenDemon.heavenly(5902));
		SpellArt.Painter bone = marrow(5903);
		for (int k = 0; k < 6; k++) {
			double a = Math.toRadians(60 * k), sx = Math.sin(a), sy = Math.cos(a);
			double[] from = SpellArt.v3(sx * 3, WHEEL_Y + sy * 3, WHEEL_Z), to = SpellArt.v3(sx * (WHEEL_R - 5.4), WHEEL_Y + sy * (WHEEL_R - 5.4), WHEEL_Z);
			SpellArt.limb(wheel, from, to, 1.2, 1.2, bone).density(4);
			SpellArt.sheet(wheel, sx * (WHEEL_R - 6.2), WHEEL_Y + sy * (WHEEL_R - 6.2), WHEEL_Z - 0.8, 6.4, 6.4, 90, 0, 0, pathDisc(k, 5910)).density(4);
		}
		SpellArt.sheet(wheel, 0, WHEEL_Y, WHEEL_Z - 0.9, 7.4, 7.4, 90, 0, 0, wheelHub(5920)).density(4);
		SpellArt.Bone halo = m.bone("halo", "root", 0, WHEEL_Y, WHEEL_Z + 1);
		SpellArt.sheet(halo, 0, WHEEL_Y, WHEEL_Z + 1, WHEEL_R * 2.8, WHEEL_R * 2.8, 90, 0, 0, Axis.haze(HeavenDemon.GOLD_PALE, Ghost.FIRE_PALE, 0.18)).density(0.8);
		SpellArt.Anim appear = m.anim("appear", 0.45, null);
		appear.scale("wheel", 0, 0.1);
		appear.scale("wheel", 0.3, 1.12);
		appear.scale("wheel", 0.45, 1);
		appear.sample("halo", "scale", 0, 0.45, 4, t -> Axis.same(Math.max(0.05, SpellArt.easeOut(t / 0.45))));
		double T = 4;
		SpellArt.Anim turn = m.anim("turn", T, "true");
		turn.sample("wheel", "rotation", 0, T, 8, t -> new double[] {0, 0, 360 * t / T});
		turn.sample("halo", "scale", 0, T, 8, t -> Axis.same(1 + 0.04 * Axis.wave(t, T, 2, 0)));
	}

	// --- Six Paths Annihilation ---

	static final double SIX_R = 28.8, SIX_H = 40;

	/**
	 * The ground under Six Paths Annihilation (on a sheet lying level, its six pillars' feet on a circle at {@code ring} of its half): a ring of
	 * ghost fire through the pillars' feet, a hexagon of it joining each to the next and spokes in to the middle, a small circle round each
	 * foot and one at the middle; clear elsewhere.
	 */
	static SpellArt.Painter sixPathSeal(double ring, int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 0.99) return SpellArt.CLEAR;
			double line = Math.min(Math.abs(r - ring), Math.abs(r - ring * 0.2));
			for (int k = 0; k < 6; k++) {
				double a0 = Math.toRadians(60 * k), a1 = Math.toRadians(60 * k + 60);
				double ax = Math.sin(a0) * ring, az = Math.cos(a0) * ring, bx = Math.sin(a1) * ring, bz = Math.cos(a1) * ring;
				double dx = bx - ax, dz = bz - az, f = SpellArt.clamp01(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz));
				line = Math.min(line, Math.hypot(x - ax - dx * f, z - az - dz * f));
				// Its spoke, from the middle's circle out to the foot.
				double g = SpellArt.clamp01((x * ax + z * az) / (ring * ring));
				if (g > 0.2) line = Math.min(line, Math.hypot(x - ax * g, z - az * g));
				line = Math.min(line, Math.abs(Math.hypot(x - ax, z - az) - 0.09));
			}
			if (line > 0.018) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(Ghost.FIRE_CYAN, Ghost.FIRE_PALE, 1 - line / 0.018), 0.92);
		};
	}

	/**
	 * Six Paths Annihilation round its caster (their feet at the origin): six pillars of ghost fire ten blocks tall standing round them, one for
	 * each path of rebirth ("pillar_0".., each a white core wrapped in climbing tongues of fire), the six-pointed figure of the six paths burning
	 * on the ground between them ("seal").
	 */
	static void sixPaths(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int k = 0; k < 6; k++) {
			double a = Math.toRadians(60 * k), px = Math.sin(a) * SIX_R, pz = Math.cos(a) * SIX_R;
			SpellArt.Bone pillar = m.bone("pillar_" + k, "root", px, 0, pz);
			pillar.box(px, SIX_H / 2, pz, 2.2, SIX_H, 2.2, ghostCore(6001)).density(2);
			for (int j = 0; j < 5; j++) {
				double y = j * 8;
				for (int q = 0; q < 2; q++) {
					double th = Math.toRadians(90 * q + 45 * j + 60 * k);
					double[] base = SpellArt.v3(px + Math.sin(th) * 0.6, y, pz + Math.cos(th) * 0.6), way = Axis.unit(SpellArt.v3(Math.sin(th) * 0.12, 1, Math.cos(th) * 0.12));
					Axis.crossedTongue(m, pillar, "six_fire_" + (j + q) % 2, base, way, 13 - j * 0.6, 8.6 - j * 0.7, Ghost.ghostFire(2.4, 6003 + (j + q) % 2), 1.5, q == 1);
				}
			}
			for (int q = 0; q < 5; q++) {
				double th = Math.toRadians(72 * q + 30 * k);
				double[] base = SpellArt.v3(px + Math.sin(th) * 2.2, 0.2, pz + Math.cos(th) * 2.2), way = Axis.unit(SpellArt.v3(Math.sin(th) * 0.5, 1, Math.cos(th) * 0.5));
				Axis.crossedTongue(m, pillar, "six_foot", base, way, 6, 3.4, Ghost.ghostFire(2, 6005), 1.5, q % 2 == 1);
			}
		}
		SpellArt.Bone seal = m.bone("seal", "root", 0, 0, 0);
		double size = SIX_R * 2 + 10;
		SpellArt.sheet(seal, 0, 0.3, 0, size, size, 0, 0, 0, sixPathSeal(SIX_R * 2 / size, 6007)).density(1.2);
		sixErupt(m);
		sixBurn(m);
		sixCollapse(m);
	}

	/** They erupt (its enter): the figure burning out over the ground, then each pillar in turn bursting up out of it, overshooting and settling. */
	static void sixErupt(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("erupt", L, null);
		a.sample("seal", "scale", 0, 0.25, 4, t -> {
			double f = Math.max(0.05, SpellArt.easeOut(t / 0.25));
			return new double[] {f, 1, f};
		});
		a.scale("seal", L, 1);
		for (int k = 0; k < 6; k++) {
			double t0 = 0.12 + 0.035 * k;
			String b = "pillar_" + k;
			a.scale(b, 0, 1, 0.02, 1);
			a.scale(b, t0, 1, 0.02, 1);
			a.scale(b, t0 + 0.12, 1.1, 1.25, 1.1);
			a.scale(b, Math.min(L, t0 + 0.24), 1);
			a.scale(b, L, 1);
		}
	}

	/** They burn (looped without a seam): each pillar's fire swelling and flickering on its own beat. */
	static void sixBurn(SpellArt.Model m) {
		double T = 0.8;
		SpellArt.Anim a = m.anim("burn", T, "true");
		for (int k = 0; k < 6; k++) {
			double ph = k * 1.05;
			a.sample("pillar_" + k, "scale", 0, T, 8, t -> new double[] {1 + 0.07 * Axis.wave(t, T, 2, ph), 1 + 0.04 * Axis.wave(t, T, 1, ph + 1), 1 + 0.07 * Axis.wave(t, T, 2, ph)});
		}
		a.scale("seal", 0, 1);
		a.scale("seal", T, 1);
	}

	/** They fall in (played once, held): every pillar leaning in over its caster and guttering out. */
	static void sixCollapse(SpellArt.Model m) {
		double L = 0.45;
		SpellArt.Anim a = m.anim("collapse", L, "hold_on_last_frame");
		for (int k = 0; k < 6; k++) {
			double ang = Math.toRadians(60 * k), ca = Math.cos(ang), sa = Math.sin(ang);
			String b = "pillar_" + k;
			a.sample(b, "rotation", 0, L, 4, t -> {
				double lean = 28 * SpellArt.easeIn(t / L);
				return new double[] {lean * ca, 0, -lean * sa};
			});
			a.sample(b, "scale", 0, L, 4, t -> new double[] {Math.max(0.05, 1 - 0.6 * SpellArt.easeIn(t / L)), Math.max(0.05, 1 - SpellArt.easeIn(t / L)), Math.max(0.05, 1 - 0.6 * SpellArt.easeIn(t / L))});
		}
		a.sample("seal", "scale", 0, L, 3, t -> {
			double f = Math.max(0.05, 1 - SpellArt.easeIn(t / L));
			return new double[] {f, 1, f};
		});
	}
}
