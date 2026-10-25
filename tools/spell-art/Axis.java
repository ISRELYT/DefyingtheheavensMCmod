/**
 * The looks of the Four Axis spells for SpellArt, made over as the great spells' are (2026-10-25: the user, of the spells of Four Axis and below,
 * "they were done before geckolib and some look not great"): fire's falling suns (Nine Suns Descend). At the render scale (4) and its realm's
 * greatness (1) a model pixel is a quarter of a block.
 */
public class Axis {
	// --- Shared shapes ---

	/** {@code n} ways out from a heart, spread evenly over a sphere (a Fibonacci spiral), each a unit vector. */
	static double[][] sphereWays(int n, double turn) {
		double[][] ways = new double[n][];
		for (int k = 0; k < n; k++) {
			double y = 1 - 2 * (k + 0.5) / n, rr = Math.sqrt(Math.max(0, 1 - y * y)), phi = k * 2.39996 + turn;
			ways[k] = SpellArt.v3(Math.cos(phi) * rr, y, Math.sin(phi) * rr);
		}
		return ways;
	}

	static double[] unit(double[] v) { return SpellArt.times(v, 1 / SpellArt.len(v)); }

	/**
	 * A tongue of {@code p} standing out from {@code base} along {@code way}, {@code length} long and {@code width} across, crossed (a standing blade
	 * and a lying one along the same way, so it keeps its width seen from any side), each painted the first time its {@code name} is asked for.
	 */
	static void crossedTongue(SpellArt.Model m, SpellArt.Bone bone, String name, double[] base, double[] way, double length, double width, SpellArt.Painter p,
			double tpp, boolean flip) {
		double[] up = SpellArt.aimY(way[0], way[1], way[2]), along = SpellArt.aimZ(way[0], way[1], way[2]);
		SpellArt.shared(m, bone, name, false, base, length, width, up[0], up[1], 0, p, tpp, flip);
		SpellArt.shared(m, bone, name + "_flat", true, base, length, width, along[0], along[1], 0, p, tpp, flip);
	}

	/** A haze of light (on a sheet through its heart): {@code heart} fading out through {@code rim} to nothing, round; glowing, seen through. */
	static SpellArt.Painter haze(int heart, int rim, double strength) {
		return t -> {
			double r = Math.hypot(t.across() - 0.5, t.back() - 0.5) * 2;
			if (r > 0.98) return SpellArt.CLEAR;
			double a = (1 - r) * (1 - r);
			return a < 0.03 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(rim, heart, Math.pow(1 - r, 1.5)), strength * a);
		};
	}

	/** Three sheets of {@code p} crossed through (0, 0, 0), {@code size} across: a glow seen alike from any side. */
	static void hazeBall(SpellArt.Bone bone, double size, SpellArt.Painter p, double tpp) {
		hazeBall(bone, SpellArt.v3(0, 0, 0), size, p, tpp);
	}

	/** Three sheets of {@code p} crossed through {@code at}, {@code size} across. */
	static void hazeBall(SpellArt.Bone bone, double[] at, double size, SpellArt.Painter p, double tpp) {
		SpellArt.sheet(bone, at[0], at[1], at[2], size, size, 0, 0, 0, p).density(tpp);
		SpellArt.sheet(bone, at[0], at[1], at[2], size, size, 90, 0, 0, p).density(tpp);
		SpellArt.sheet(bone, at[0], at[1], at[2], size, size, 90, 90, 0, p).density(tpp);
	}

	/** {@code k} turns of a sine over a loop {@code T} long, {@code t} seconds in (so it comes back where it began). */
	static double wave(double t, double T, double k, double phase) { return Math.sin(2 * Math.PI * k * t / T + phase); }

	static double[] same(double s) { return new double[] {s, s, s}; }

	// --- Nine Suns Descend: a falling sun ---

	/** A falling sun's heat from its coolest to its hottest. */
	static final int[] SUNFIRE = {Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.AMBER, Fire.GOLD_LIGHT, Fire.WHITE_FIRE};
	/** A tail of fire's heat: saturated, flame to gold, no white (a pale tail of light read as smoke against the day's sky). */
	static final int[] TAIL_FIRE = {Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.AMBER, Fire.GOLD};
	static final double SUN_R = 13;

	/**
	 * A falling sun's face (on a solid): its fire boiling in small cells, white-gold at their hearts, darker amber in the lanes between them,
	 * streaked hotter where it rushes back from its brow; glowing.
	 */
	static SpellArt.Painter sunfire(int seed) {
		return t -> {
			double x = t.px / 2.8, y = t.py / 2.8, z = t.pz / 2.8;
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
			double lane = d2 - d1, swirl = SpellArt.fbm3(t.px * 0.12, t.py * 0.12, t.pz * 0.05, seed + 3, 3);
			// Its brow (-z, the way it falls) hottest; streaks of hotter fire running back over it.
			double brow = SpellArt.clamp01(-t.pz / SUN_R), streak = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.08 + swirl, seed + 5, 2) - 1);
			double heat = SpellArt.clamp01(0.5 + 0.28 * SpellArt.smooth(0.0, 0.3, lane) - 0.12 * d1 + (swirl - 0.5) * 0.35 + 0.18 * brow + 0.2 * Math.pow(streak, 4));
			return SpellArt.glow(SpellArt.ramp(heat, SUNFIRE));
		};
	}

	/**
	 * A comet's tail of fire (on a blade, its length along it: {@code v0}..{@code v1} of a tail {@code total} pixels long): widest and white-hot at
	 * its root, wavering, licking out in tongues along its edges, cooling through flame to crimson and thinning away to nothing at its end;
	 * glowing, seen through more toward its end.
	 */
	static SpellArt.Painter cometTail(double total, double v0, double v1, int seed) {
		return t -> {
			double V = v0 + (v1 - v0) * t.v(), along = V * total;
			double uu = t.u() - 0.5 - 0.07 * Math.sin(along * 0.17 + seed) * SpellArt.smooth(0, 0.4, V);
			double licks = 0.82 + 0.18 * Math.sin(along * 0.6 + seed) * Math.sin(along * 0.23 + seed * 2);
			double half = 0.49 * Math.pow(1 - V, 0.45) * licks;
			if (Math.abs(uu) > half || V > 0.99) return SpellArt.CLEAR;
			double core = 1 - Math.abs(uu) / half, streak = 1 - Math.abs(2 * SpellArt.fbm(uu * 9 + seed, along * 0.08, seed + 1, 2) - 1);
			// Saturated flame, and nearly solid but at its edges and end: its glow is drawn over what is behind it, not added to it (GeckoLib's),
			// so by day a thin pale tail read as smoke, beige against the sky.
			double hot = SpellArt.clamp01(0.3 + Math.pow(core, 0.9) * (1.0 - 0.6 * V) + 0.3 * Math.pow(streak, 3) * core * (1 - V));
			return SpellArt.glow(SpellArt.ramp(hot, TAIL_FIRE), (0.35 + 0.65 * Math.pow(core, 0.4)) * (1 - 0.5 * Math.pow(V, 1.5)));
		};
	}

	static final int TAILS = 5;
	static final double[] TAIL_LENGTH = {80, 66, 70, 62, 68}, TAIL_HEADING = {0, 8, -7, 3, -2}, TAIL_PITCH = {0, 5, 5, -8, 9};

	/**
	 * One of the Nine Suns as it falls, its heart at the origin and its brow toward -z (the way it falls), some five blocks across: its boiling
	 * face ("core"), a mantle of hotter fire streaming over it ("mantle"), a corona of flames standing off it all round and swept back by its
	 * fall ("corona"), a haze of its light ("halo"), and its tail: {@link #TAILS} streamers of fire rushing back from it, twenty blocks long
	 * ("tail_0".."tail_6", each of two bones "_0" and "_1").
	 */
	static void fallingSun(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone core = m.bone("core", "root", 0, 0, 0);
		Immortal.ball(core, 0, 0, 0, SUN_R, 2, sunfire(3101), 1.5);
		SpellArt.Bone mantle = m.bone("mantle", "root", 0, 0, 0);
		Immortal.ball(mantle, 0, 0, 0, SUN_R * 1.1, 2.75, Void.plasma(3111), 1.2);
		// Its corona: flames standing off it all round, swept back the more the further back they stand.
		SpellArt.Bone corona = m.bone("corona", "root", 0, 0, 0);
		double[][] ways = sphereWays(34, 0.4);
		for (int k = 0; k < ways.length; k++) {
			double[] out = ways[k], way = unit(SpellArt.add(out, SpellArt.v3(0, 0, 0.55 + 0.45 * Math.max(0, out[2]))));
			double length = 9 + 4 * SpellArt.hash(k, 7, 3101) + 3 * Math.max(0, out[2]);
			crossedTongue(m, corona, "flame_" + k % 4, SpellArt.times(out, SUN_R * 0.8), way, length, 7, Fire.flame(2.4, 3121 + k % 4, SUNFIRE), 1.5, k % 2 == 1);
		}
		// Its haze: faint (it is drawn over the sun, not added to its light: strong, it dulled the sun's face to tan by day).
		SpellArt.Bone halo = m.bone("halo", "root", 0, 0, 0);
		hazeBall(halo, 64, haze(Fire.GOLD_LIGHT, Fire.FLAME, 0.16), 1);
		// Its tail: broad tongues of fire rushing back off it where the tail begins, then streamers of fire, each in two lengths (so it waves).
		SpellArt.Bone tail = m.bone("tail", "root", 0, 0, SUN_R * 0.4);
		for (int k = 0; k < 6; k++) {
			double th = 60.0 * k + 30, off = 0.45;
			double[] out = unit(SpellArt.v3(Math.sin(Math.toRadians(th)) * off, Math.cos(Math.toRadians(th)) * off, 1));
			crossedTongue(m, tail, "plume_" + k % 2, SpellArt.times(out, SUN_R * 0.7), unit(SpellArt.add(out, SpellArt.v3(0, 0, 1.6))), 26 + (k % 2) * 5, 16,
					Fire.flame(2, 3141 + k % 2, SUNFIRE), 1.2, k % 2 == 1);
		}
		for (int k = 0; k < TAILS; k++) {
			double total = TAIL_LENGTH[k], heading = TAIL_HEADING[k], pitch = TAIL_PITCH[k];
			double[] dir = SpellArt.turn(SpellArt.v3(0, 0, 1), -pitch, heading, 0);
			double[] start = SpellArt.add(SpellArt.v3(0, 0, SUN_R * 0.4), SpellArt.times(unit(SpellArt.v3(dir[0] * 4, dir[1] * 4, 0.5)), SUN_R * 0.45));
			double[] mid = SpellArt.add(start, SpellArt.times(dir, total * 0.45)), end = SpellArt.add(start, SpellArt.times(dir, total));
			double width = 28 - k * 1.6;
			SpellArt.Bone a = m.bone("tail_" + k + "_0", "tail", start[0], start[1], start[2]);
			Water.ribbon(m, a, "tail_" + k + "_0", start, mid, width, cometTail(total, 0, 0.45, 3131 + k), 1.5, true);
			SpellArt.Bone b = m.bone("tail_" + k + "_1", "tail_" + k + "_0", mid[0], mid[1], mid[2]);
			Water.ribbon(m, b, "tail_" + k + "_1", mid, end, width * 0.6, cometTail(total, 0.45, 1, 3131 + k), 1.2, true);
		}
		sunKindle(m);
		sunBurn(m);
		sunImpact(m);
	}

	/** It kindles as it is let fall (played once): swelling out of a spark, its corona and tail unfurling after it. */
	static void sunKindle(SpellArt.Model m) {
		double L = 0.45;
		SpellArt.Anim a = m.anim("kindle", L, null);
		a.sample("root", "scale", 0, L, 6, t -> same(0.2 + 0.8 * SpellArt.easeOut(t / L)));
		a.scale("corona", 0, 0.3);
		a.scale("corona", L, 1);
		a.scale("tail", 0, 0.05);
		a.scale("tail", L, 1);
	}

	/**
	 * Its fall (looped without a seam): its face and mantle turning over each its own way, its corona wheeling round the way it falls and
	 * flickering, its glow breathing, each streamer of its tail lashing.
	 */
	static void sunBurn(SpellArt.Model m) {
		double T = 4;
		SpellArt.Anim a = m.anim("burn", T, "true");
		a.sample("core", "rotation", 0, T, 16, t -> new double[] {0, 0, 360 * t / T});
		a.sample("mantle", "rotation", 0, T, 16, t -> new double[] {0, 0, -720 * t / T});
		a.sample("corona", "rotation", 0, T, 16, t -> new double[] {0, 0, 360 * t / T});
		a.sample("corona", "scale", 0, T, 32, t -> same(1 + 0.07 * wave(t, T, 8, 0)));
		a.sample("halo", "scale", 0, T, 16, t -> same(1 + 0.06 * wave(t, T, 4, 1)));
		for (int k = 0; k < TAILS; k++) {
			double ph = k * 1.7;
			a.sample("tail_" + k + "_0", "rotation", 0, T, 32, t -> new double[] {5 * wave(t, T, 6, ph), 6 * wave(t, T, 4, ph + 1), 0});
			a.sample("tail_" + k + "_1", "rotation", 0, T, 32, t -> new double[] {9 * wave(t, T, 6, ph - 1.2), 11 * wave(t, T, 4, ph - 0.4), 0});
			a.sample("tail_" + k + "_0", "scale", 0, T, 32, t -> new double[] {1, 1, 1 + 0.06 * wave(t, T, 8, ph)});
		}
	}

	/** It strikes the land (played once, held: it is gone a moment after): its face bursting outward, its corona and glow flung wide, its tail snuffed. */
	static void sunImpact(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("impact", 0.5, "hold_on_last_frame");
		a.sample("core", "scale", 0, 0.5, 5, t -> new double[] {1 + 0.9 * SpellArt.easeOut(t / 0.5), 1 - 0.45 * SpellArt.easeOut(t / 0.5), 1 + 0.9 * SpellArt.easeOut(t / 0.5)});
		a.sample("mantle", "scale", 0, 0.5, 5, t -> same(1 + 1.4 * SpellArt.easeOut(t / 0.5)));
		a.sample("corona", "scale", 0, 0.5, 5, t -> same(1 + 2.2 * SpellArt.easeOut(t / 0.5)));
		a.sample("halo", "scale", 0, 0.5, 5, t -> same(1 + 2.8 * SpellArt.easeOut(t / 0.5)));
		a.sample("tail", "scale", 0, 0.3, 4, t -> same(Math.max(0.02, 1 - SpellArt.easeIn(t / 0.3))));
		a.scale("tail", 0.5, 0.02);
	}

	// --- Phoenix Nirvana: the phoenix of nirvana ---

	/** The phoenix's fire from its coolest to its hottest: hotter and golder than the Vermilion Bird's, for the fire it is reborn from. */
	static final int[] NIRVANA_HEAT = {Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.AMBER, Fire.GOLD, Fire.GOLD_LIGHT, Fire.WHITE_FIRE};

	/**
	 * Fire made into the phoenix's body (on a solid; Fire#blaze in its heat): flames streaming back along it, white-gold along its back and toward
	 * its heart (the line x = 0, y = {@code axisY}), cooling through gold and flame to crimson toward its edges and beneath; glowing.
	 */
	static SpellArt.Painter nirvanaBlaze(double axisY, int seed) { return blazeOf(axisY, seed, NIRVANA_HEAT); }

	/** {@link #nirvanaBlaze} in another fire's {@code heat}. */
	static SpellArt.Painter blazeOf(double axisY, int seed, int[] fire) {
		return t -> {
			double r = Math.hypot(t.qx(), t.qy() - axisY);
			double flow = SpellArt.fbm3(t.px * 0.3, t.py * 0.35, t.pz * 0.08, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.6, t.py * 0.6, t.pz * 0.14 + flow, seed + 5, 2) - 1);
			double heat = SpellArt.clamp01(0.62 - r * 0.06 + (flow - 0.5) * 0.8 + 0.22 * Math.max(0, t.ny) - 0.3 * Math.max(0, -t.ny) + 0.45 * Math.pow(streak, 3));
			return SpellArt.glow(SpellArt.ramp(heat, fire), 0.6 + 0.36 * heat);
		};
	}

	/** {@link #nirvanaBlaze} on a limb (its own axis along its cube): hotter toward each face's middle and on top, streaked along it. */
	static SpellArt.Painter nirvanaLimb(int seed) { return limbOf(seed, NIRVANA_HEAT); }

	/** {@link #nirvanaLimb} in another fire's {@code heat}. */
	static SpellArt.Painter limbOf(int seed, int[] fire) {
		return t -> {
			double flow = SpellArt.fbm3(t.px * 0.3, t.py * 0.35, t.pz * 0.12, seed, 3), streak = 1 - Math.abs(2 * SpellArt.fbm(t.fx * 3 + seed, t.ly * 0.4 + flow * 2, seed + 5, 2) - 1);
			double middle = 1 - Math.abs(t.fx - 0.5) * 2;
			double heat = SpellArt.clamp01(0.22 + (flow - 0.5) * 0.9 + 0.16 * Math.max(0, t.ny) + 0.34 * middle + 0.4 * Math.pow(streak, 3));
			return SpellArt.glow(SpellArt.ramp(heat, fire), 0.5 + 0.44 * heat);
		};
	}

	/**
	 * A section of a phoenix's streamer of fire (Fire#streamer made of flame: on a blade {@code width} pixels across, {@code v0}..{@code v1} of the
	 * whole, {@code total} pixels long, proportioned to an eye {@code eyeWidth} across): a narrow rippling vane of flame round a thread of white
	 * fire; at its end, an eye of fire ringed gold, crimson and gold round a white heart, flames licking from its rim; all glowing.
	 */
	static SpellArt.Painter flameStreamer(double width, double eyeWidth, double total, double v0, double v1, int seed) {
		return flameStreamer(width, eyeWidth, total, v0, v1, seed, NIRVANA_HEAT);
	}

	/** {@link #flameStreamer} in another fire's {@code heat}. */
	static SpellArt.Painter flameStreamer(double width, double eyeWidth, double total, double v0, double v1, int seed, int[] fire) {
		SpellArt.Painter eye = Fire.streamer(width, eyeWidth, total, v0, v1, seed);
		return t -> {
			double V = v0 + (v1 - v0) * t.v(), x = (t.u() - 0.5) * width, ax = Math.abs(x);
			double along = (V - 0.86) * total, re = Math.hypot(along / (0.12 * total), ax / (0.4 * eyeWidth));
			if (re < 1.22 || V > 0.86) return eye.paint(t);
			double half = eyeWidth * (0.1 + 0.07 * SpellArt.smooth(0.45, 0.75, V) + 0.027 * Math.sin(V * total * 0.5 + seed));
			if (ax > half) return SpellArt.CLEAR;
			if (ax < 0.34) return SpellArt.glow(SpellArt.mix(Fire.GOLD, Fire.WHITE_FIRE, V), 0.95);
			double edge = ax / half, barb = (V * total - ax * 1.2) / 1.4;
			double hot = SpellArt.clamp01(1 - edge * 0.8 - (barb - Math.floor(barb) < 0.3 ? 0.12 : 0));
			return SpellArt.glow(SpellArt.ramp(hot, fire), 0.45 + 0.45 * hot);
		};
	}

	/** A feather of flame (Fire#flameFeather) in {@code heat}: a long tongue widest a third of the way out, drawn to a wavering point split into {@code licks}. */
	static SpellArt.Painter featherOf(double licks, int seed, int[] heat) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5;
			double uu = u - 0.05 * Math.sin(v * 9 + seed) * v - 0.05 * (SpellArt.noise(v * 5 + seed, 2, seed) - 0.5);
			double half = 0.48 * Math.pow(Math.sin(Math.PI * Math.min(1, Math.pow(v, 0.55)) * 0.999), 0.8) * (0.84 + 0.16 * SpellArt.noise(v * 11, seed, seed + 1));
			if (v < 0.015 || Math.abs(uu) > half) return SpellArt.CLEAR;
			double across = (uu / half + 1) / 2;
			double tongue = Math.pow(Math.abs(Math.sin(across * Math.PI * licks + seed * 0.4)), 0.6);
			double top = 0.7 + 0.3 * tongue * (0.8 + 0.2 * SpellArt.hash(seed, 1, 7));
			if (v > top) return SpellArt.CLEAR;
			double core = 1 - Math.abs(uu) / half, out = v / top;
			double streak = 1 - Math.abs(2 * SpellArt.fbm(across * 3.5 + seed, v * 2.6, seed + 2, 2) - 1);
			double hot = SpellArt.clamp01(core * (1.2 - 0.8 * out) + 0.3 * Math.pow(streak, 3) * (1 - out));
			return SpellArt.glow(SpellArt.ramp(hot, heat), 0.24 + 0.74 * Math.pow(hot, 0.6));
		};
	}

	/** A phoenix's look: its fire, and how it holds itself at rest (its body's pitch, its neck's and head's, how high its wings are raised; degrees). */
	record Phoenix(int[] heat, double body, double neck, double neckUp, double head, double wingRaise) {}

	/** The phoenix of nirvana's: golden fire, rising, its wings raised high. */
	static final Phoenix NIRVANA = new Phoenix(NIRVANA_HEAT, -26, 14, 8, 4, 22);

	/** A section of the phoenix's streamers: its shaft and narrow vane, ending in an eye of fire (Fire#streamer, its widths at the phoenix's size). */
	static final double[][] PHOENIX_SECTIONS = {{0, 0.34, 3.4}, {0.34, 0.66, 3.4}, {0.66, 1, 8.4}};
	static final double[] PHOENIX_PLUMES = {46, 55, 64, 55, 46};
	static final double PHOENIX_UP = 22;

	/**
	 * The phoenix of nirvana, made of fire (the great spells' beasts made of their elements: the user, 2026-10-24), its body's heart at the origin
	 * and facing -z, some twenty blocks from wingtip to wingtip: its body a blaze, golder and hotter than the Vermilion Bird's ("body", held
	 * rising), flames streaming off it; its long neck ("neck", "neck_up") ruffed in flame; its head ("head", the lower beak "beak_low") a flame
	 * with a beak and eyes of white fire, a crest ("crest") of three plumes each ending in an eye of fire; its wings ("wing_l", "wing_r", each with
	 * its "_mid" and "_tip") raised, all tongues of flame; five long streamers trailing from its tail ("tail", "plume_0".."plume_4", each of three
	 * bones "_0".."_2"), each ending in an eye, as a phoenix's do.
	 */
	static void nirvanaPhoenix(SpellArt.Model m) {
		phoenixShape(m, NIRVANA);
		phoenixHover(m);
		phoenixEmerge(m);
		phoenixEnfold(m);
		phoenixReborn(m);
	}

	/** A phoenix's shape in its look {@code p} (its bones {@link #nirvanaPhoenix}'s), at the render scale 4. */
	static void phoenixShape(SpellArt.Model m, Phoenix p) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone body = m.bone("body", "root", 0, 0, 0, p.body(), 0, 0);
		SpellArt.Painter fire = blazeOf(0.5, 3201, p.heat());
		body.box(0, 0, -3.5, 8, 7, 9, fire);
		body.box(0, 0.4, -3.5, 6, 8.6, 8.4, fire);
		body.box(0, -0.2, -8.6, 5.6, 5.4, 2.6, fire);
		body.box(0, 0.3, 3, 7, 6.2, 7, fire);
		body.box(0, 0.6, 3, 5.2, 7.4, 6.6, fire);
		body.box(0, 1, 8.4, 4.4, 4.4, 4.8, fire);
		// Flames streaming back off its back and flanks, lifting away from it; and off its breast.
		double[] rowZ = {-7, -3, 1, 5, 9}, rowR = {4.7, 4.9, 4.6, 4.0, 2.8};
		for (int row = 0; row < rowZ.length; row++) {
			for (int k = -2; k <= 2; k++) {
				double phi = k * 32;
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * rowR[row], 0.5 + Math.cos(Math.toRadians(phi)) * rowR[row], rowZ[row]);
				int variant = (row + Math.abs(k)) % 3;
				SpellArt.shared(m, body, "back_flame_" + variant, true, base, 9 + variant, 5, 22, 0, phi, featherOf(2, 3210 + variant, p.heat()), 2.5, k < 0);
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int k = -1; k <= 1; k++) {
				double phi = 180 + k * 36;
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 4.2, 0.5 + Math.cos(Math.toRadians(phi)) * (4.3 - row * 0.3), -7.5 + row * 4.5);
				SpellArt.shared(m, body, "breast_flame_" + (row + Math.abs(k)) % 2, true, base, 9, 5.4, 16, 0, phi, featherOf(2, 3216 + (row + Math.abs(k)) % 2, p.heat()), 2.5, k < 0);
			}
		}
		phoenixNeckAndHead(m, p);
		phoenixWing(m, 1, p);
		phoenixWing(m, -1, p);
		phoenixTail(m, p);
	}

	static void phoenixNeckAndHead(SpellArt.Model m, Phoenix p) {
		SpellArt.Bone neck = m.bone("neck", "body", 0, 1.6, -8, p.neck(), 0, 0);
		SpellArt.Painter limb = limbOf(3230, p.heat());
		SpellArt.limb(neck, SpellArt.v3(0, 1.2, -7.4), SpellArt.v3(0, 6.2, -11.6), 4.4, 4, limb);
		SpellArt.limb(neck, SpellArt.v3(0, 1.2, -7.4), SpellArt.v3(0, 6.2, -11.6), 3.4, 5, limb);
		// The ruff: flames flaring back round the neck's foot.
		for (int k = -4; k <= 4; k++) {
			double phi = k * 34;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(phi)) * 3.4, 2.6 + Math.cos(Math.toRadians(phi)) * 3.2, -9 + Math.abs(k) * 0.25);
			SpellArt.shared(m, neck, "ruff_" + Math.abs(k) % 3, true, base, 8, 3.6, 26, 0, phi, featherOf(2, 3232 + Math.abs(k) % 3, p.heat()), 2.5, k < 0);
		}
		SpellArt.Bone up = m.bone("neck_up", "neck", 0, 6.2, -11.6, p.neckUp(), 0, 0);
		SpellArt.limb(up, SpellArt.v3(0, 6.2, -11.6), SpellArt.v3(0, 10.4, -12.8), 3.4, 3.2, limb);
		SpellArt.limb(up, SpellArt.v3(0, 6.2, -11.6), SpellArt.v3(0, 10.4, -12.8), 2.6, 4, limb);
		SpellArt.Bone head = m.bone("head", "neck_up", 0, 10.4, -12.8, p.head(), 0, 0);
		SpellArt.Painter face = blazeOf(11.4, 3241, p.heat()), point = Fire.whiteFire(-16, -20.4);
		head.box(0, 11.4, -14.4, 4, 3.6, 5, face);
		head.box(0, 11.6, -14.4, 3, 4.6, 4.6, face);
		head.box(0, 11, -17.3, 2.2, 1.8, 2.6, point);
		head.box(0, 10.9, -19, 1.5, 1.3, 1.6, point);
		SpellArt.limb(head, SpellArt.v3(0, 11.1, -19.6), SpellArt.v3(0, 10.1, -20.5), 0.9, 0.9, point).density(4);
		for (int s = -1; s <= 1; s += 2) {
			head.box(s * 2.05, 12.1, -15.2, 0.4, 1.4, 1.8, Fire.fireEye(face)).density(5);
			// Flames streaming back from behind each eye.
			SpellArt.blade(head, SpellArt.v3(s * 2.2, 12.2, -13.4), 7, 2.8, -80, s * 18, s * 14, Fire.flame(2, 3243 + s, p.heat())).density(3);
		}
		SpellArt.Bone low = m.bone("beak_low", "head", 0, 10.4, -16.4);
		low.box(0, 10.2, -17.8, 1.8, 0.8, 3, point);
		// A wattle of flame under the throat.
		SpellArt.blade(head, SpellArt.v3(0, 9.6, -15.6), 6, 2.6, -150, 0, 0, Fire.flame(1.6, 3246, p.heat())).density(3);
		// The crest: three plumes sweeping back from the crown, each ending in an eye of fire; flames at their foot.
		SpellArt.Bone crest = m.bone("crest", "head", 0, 13.6, -14.4);
		for (int k = 0; k < 3; k++) {
			double len = 17 - Math.abs(k - 1) * 3.5;
			SpellArt.blade(crest, SpellArt.v3((k - 1) * 0.7, 13.5, -14.6 + Math.abs(k - 1) * 0.5), len, 3.2, -58 - Math.abs(k - 1) * 12, (k - 1) * 18, (k - 1) * 10,
					flameStreamer(3.2, 3.2, len, 0, 1, 3250 + k, p.heat())).density(3);
		}
		for (int k = 0; k < 2; k++) {
			double side = k == 0 ? 1 : -1;
			SpellArt.blade(crest, SpellArt.v3(side * 0.9, 13.4, -15.4), 7.5, 3.2, -36, side * 26, side * 16, Fire.flame(2.2, 3254 + k, p.heat())).density(3);
		}
	}

	static void phoenixWing(SpellArt.Model m, int s, Phoenix p) {
		String w = "wing_" + (s > 0 ? "l" : "r");
		boolean flip = s < 0;
		// The leading edge: shoulder, elbow, wrist, the hand's end; raised, as a phoenix rising holds them.
		double[][] edge = {SpellArt.v3(s * 3.6, 2.6, -4), SpellArt.v3(s * 12, 4.4, -3), SpellArt.v3(s * 20, 6.2, -4.6), SpellArt.v3(s * 26, 6.8, -3.2)};
		SpellArt.Bone[] bones = {m.bone(w, "body", edge[0][0], edge[0][1], edge[0][2], 0, 0, -s * p.wingRaise()), m.bone(w + "_mid", w, edge[1][0], edge[1][1], edge[1][2], 0, 0, -s * 6),
				m.bone(w + "_tip", w + "_mid", edge[2][0], edge[2][1], edge[2][2], 0, 0, -s * 4)};
		SpellArt.Painter arm = limbOf(3260, p.heat());
		bones[0].box(edge[0][0] + s * 1, edge[0][1], edge[0][2] + 1, 4, 3.6, 5, blazeOf(0.5, 3201, p.heat()));
		SpellArt.limb(bones[0], edge[0], edge[1], 3.2, 2.4, arm);
		SpellArt.limb(bones[1], edge[1], edge[2], 2.6, 2, arm);
		SpellArt.limb(bones[2], edge[2], edge[3], 2, 1.6, arm);
		double l0 = SpellArt.len(SpellArt.sub(edge[1], edge[0])), l1 = SpellArt.len(SpellArt.sub(edge[2], edge[1])), l2 = SpellArt.len(SpellArt.sub(edge[3], edge[2]));
		double wrist = (l0 + l1) / (l0 + l1 + l2);
		// Where the secondaries were: tongues of flame trailing back along the arm, longer toward the wrist.
		for (int k = 0; k < 9; k++) {
			double[] at = Fire.along(edge, 0.05 + (wrist - 0.05) * k / 8.0);
			SpellArt.shared(m, bones[(int) at[3]], "secondary_" + k, true, SpellArt.v3(at[0], at[1] - 0.4, at[2] + 0.9), 13 + 4 * k / 8.0, 5, -3, s * (k * 0.6 - 2), 0,
					featherOf(2, 3270 + k, p.heat()), 2, flip);
		}
		// Where the primaries were: long tongues fanning out from the hand.
		double[] lengths = {19, 20, 21, 22, 22.5, 22, 20.5, 18};
		for (int k = 0; k < 8; k++) {
			double[] at = Fire.along(edge, wrist + (1 - wrist) * Math.min(1, k / 6.0));
			SpellArt.shared(m, bones[2], "primary_" + k, true, SpellArt.v3(at[0], at[1] - 0.6 - k * 0.04, at[2] + 0.7), lengths[k], 5 - k * 0.12, -2 + k * 0.4, s * (12 + k * 8.5), 0,
					featherOf(k > 4 ? 3 : 2, 3280 + k, p.heat()), 2, flip);
		}
		for (int k = 0; k < 2; k++) {
			SpellArt.shared(m, bones[0], "tertial_" + k, true, SpellArt.v3(s * (3.4 + k * 1.6), 3, -0.6 + k * 0.6), 12 + k * 1.5, 4.4, -2, -s * (10 - k * 5), 0,
					featherOf(2, 3290 + k, p.heat()), 2, flip);
		}
		// Shorter flames over the tongues' roots along the leading edge.
		for (int k = 0; k < 11; k++) {
			double f = 0.04 + 0.94 * k / 10.0;
			double[] at = Fire.along(edge, f);
			double heading = f > wrist ? s * (8 + (f - wrist) / (1 - wrist) * 40) : s * (k * 0.5 - 1);
			SpellArt.shared(m, bones[(int) at[3]], "greater_" + k % 3, true, SpellArt.v3(at[0], at[1] + 0.4, at[2] + 0.8), 8.5, 3.8, -4, heading, 0,
					featherOf(2, 3294 + k % 3, p.heat()), 2, flip);
		}
		for (int k = 0; k < 12; k++) {
			double[] at = Fire.along(edge, 0.03 + 0.95 * k / 11.0);
			SpellArt.shared(m, bones[(int) at[3]], "lesser_" + k % 2, true, SpellArt.v3(at[0], at[1] + 1.3, at[2] - 0.5), 5.2, 3.2, -10, s * 6, 0,
					featherOf(2, 3298 + k % 2, p.heat()), 2.5, flip);
		}
	}

	static void phoenixTail(SpellArt.Model m, Phoenix p) {
		SpellArt.Bone tail = m.bone("tail", "body", 0, 1.4, 10.6);
		// The fan over the streamers' roots: broad tongues.
		for (int k = 0; k < 7; k++) {
			int off = Math.abs(k - 3);
			SpellArt.shared(m, tail, "tail_flame_" + off, true, SpellArt.v3((k - 3) * 0.6, 1.8 + (3 - off) * 0.1, 10.2), 15 + (3 - off) * 1.2, 4.6, -6, (k - 3) * 11, 0,
					featherOf(2, 3300 + off, p.heat()), 2, k < 3);
		}
		// The streamers: five, each of three sections (its bones a chain), drooping and twisting a little as they go, each ending in an eye.
		for (int k = 0; k < 5; k++) {
			double heading = (k - 2) * 12, pitch = -16 - Math.abs(k - 2) * 4, total = PHOENIX_PLUMES[k];
			double[] at = SpellArt.v3((k - 2) * 0.8, 1.4, 12);
			String up = "tail";
			for (int j = 0; j < PHOENIX_SECTIONS.length; j++) {
				double[] sec = PHOENIX_SECTIONS[j];
				double length = total * (sec[1] - sec[0]), bank = (k - 2) * 12 + (k < 2 ? -1 : 1) * j * 14;
				SpellArt.Bone b = m.bone("plume_" + k + "_" + j, up, at[0], at[1], at[2]);
				SpellArt.flatBlade(b, at, length + 0.5, sec[2], pitch, heading, bank, flameStreamer(sec[2], PHOENIX_SECTIONS[2][2], total, sec[0], sec[1], 3310 + k, p.heat())).density(2);
				at = SpellArt.add(at, SpellArt.times(SpellArt.turn(SpellArt.v3(0, 0, 1), pitch, heading, bank), length));
				up = b.name;
			}
		}
	}

	/** Its hover over its caster (looped): slow deep wingbeats, the hand lagging the arm; the body riding them; the crest and streamers rippling. */
	static void phoenixHover(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("hover", T, "true");
		int steps = 16;
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, T, steps, t -> new double[] {0, -ss * 6 * wave(t, T, 1, 0.5), ss * 30 * wave(t, T, 1, 0)});
			a.sample(w + "_mid", "rotation", 0, T, steps, t -> new double[] {0, 0, ss * 12 * wave(t, T, 1, -0.7)});
			a.sample(w + "_tip", "rotation", 0, T, steps, t -> new double[] {0, -ss * 6 * Math.max(0, -wave(t, T, 1, -1.2)), ss * 16 * wave(t, T, 1, -1.3)});
		}
		a.sample("root", "position", 0, T, steps, t -> new double[] {0, PHOENIX_UP + 1.6 * wave(t, T, 1, -0.5), 0});
		a.sample("body", "rotation", 0, T, steps, t -> new double[] {2.5 * wave(t, T, 1, -0.3), 0, 0});
		a.sample("neck", "rotation", 0, T, steps, t -> new double[] {-2 * wave(t, T, 1, -0.5), 0, 0});
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {-2.5 * wave(t, T, 1, -0.2), 2 * wave(t, T, 1, 1), 0});
		a.sample("beak_low", "rotation", 0, T, 8, t -> new double[] {3 + 2 * wave(t, T, 1, 0), 0, 0});
		a.sample("crest", "rotation", 0, T, steps, t -> new double[] {5 * wave(t, T, 2, -1), 0, 0});
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {4 * wave(t, T, 1, -1.5), 2.5 * wave(t, T, 1, -2), 0});
		for (int k = 0; k < 5; k++) {
			for (int j = 0; j < PHOENIX_SECTIONS.length; j++) {
				int kk = k, jj = j;
				a.sample("plume_" + k + "_" + j, "rotation", 0, T, steps, t -> new double[] {(4 + 4 * jj) * wave(t, T, 1, -1.8 - 0.8 * jj - 0.3 * kk), (3 + 3 * jj) * wave(t, T, 1, -0.9 * jj + 1.1 * kk), 0});
			}
		}
	}

	/** Its rising out of its caster (its enter): up from their heart, swelling, its wings unfolding from along its flanks, crying as they open. */
	static void phoenixEmerge(SpellArt.Model m) {
		double L = 1.2;
		SpellArt.Anim a = m.anim("emerge", L, null);
		a.sample("root", "position", 0, L, 8, t -> new double[] {0, PHOENIX_UP * SpellArt.easeOut(t / L), 0});
		a.sample("root", "scale", 0, L, 8, t -> same(0.15 + 0.85 * SpellArt.easeOut(t / L)));
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, L, 8, t -> {
				double fold = 1 - SpellArt.ease(t / (L * 0.85));
				return new double[] {0, -ss * 70 * fold, ss * 26 * fold};
			});
			a.sample(w + "_mid", "rotation", 0, L, 8, t -> new double[] {0, -ss * 50 * (1 - SpellArt.ease(t / (L * 0.85))), 0});
			a.sample(w + "_tip", "rotation", 0, L, 8, t -> new double[] {0, -ss * 40 * (1 - SpellArt.ease(t / (L * 0.85))), 0});
		}
		a.sample("tail", "scale", 0, L, 6, t -> same(0.25 + 0.75 * SpellArt.easeOut(t / L)));
		double[] cry = {0, 0.4, 0.75, 0.95, L};
		double[] open = {0, 0, 1, 1, 0};
		for (int i = 0; i < cry.length; i++) {
			a.rot("beak_low", cry[i], 3 + 30 * open[i], 0, 0);
			a.rot("head", cry[i], -14 * open[i], 0, 0);
		}
	}

	/**
	 * It enfolds its caster (played once, held: it is gone a moment after): its wings swept forward round them as it stoops into their heart
	 * (it faces them, so toward its -z), dwindling into them.
	 */
	static void phoenixEnfold(SpellArt.Model m) {
		double L = 1.1;
		SpellArt.Anim a = m.anim("enfold", L, "hold_on_last_frame");
		a.sample("root", "position", 0, L, 8, t -> {
			double f = SpellArt.ease(t / L);
			return new double[] {0, PHOENIX_UP * (1 - f) + 4 * f, -10 * f};
		});
		a.sample("root", "scale", 0, L, 8, t -> same(1 - 0.85 * SpellArt.easeIn(t / L)));
		a.sample("body", "rotation", 0, L, 8, t -> new double[] {30 * SpellArt.ease(t / L), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, L, 8, t -> {
				double f = SpellArt.ease(t / (L * 0.7));
				return new double[] {0, ss * 58 * f, ss * 14 * f};
			});
			a.sample(w + "_mid", "rotation", 0, L, 8, t -> new double[] {0, ss * 30 * SpellArt.ease(t / (L * 0.7)), 0});
			a.sample(w + "_tip", "rotation", 0, L, 8, t -> new double[] {0, ss * 26 * SpellArt.ease(t / (L * 0.7)), 0});
		}
		a.sample("neck", "rotation", 0, L, 6, t -> new double[] {16 * SpellArt.ease(t / L), 0, 0});
	}

	/**
	 * Its rebirth (the enter of the phoenix that bursts out of a caster its nirvana has saved): it bursts up out of their heart, thrown up past
	 * where it hovers, its wings flung up and wide, its streamers fanned, crying; then it settles into its hover.
	 */
	static void phoenixReborn(SpellArt.Model m) {
		double L = 1.4;
		SpellArt.Anim a = m.anim("reborn", L, null);
		double[] t = {0, 0.25, 0.6, 1.0, L};
		double[] up = {-2, 20, 32, 25, PHOENIX_UP + 1.6 * Math.sin(-0.5)};
		double[] size = {0.3, 1.15, 1.25, 1.05, 1};
		double[] fling = {0, 1, 1, 0.4, 0};
		double[] cry = {0, 1, 1, 0.3, 0};
		for (int i = 0; i < t.length; i++) {
			a.pos("root", t[i], 0, up[i], 0);
			a.scale("root", t[i], size[i]);
			double f = fling[i];
			for (int s = -1; s <= 1; s += 2) {
				String w = "wing_" + (s > 0 ? "l" : "r");
				a.rot(w, t[i], 0, s * 8 * f, -s * 34 * f);
				a.rot(w + "_mid", t[i], 0, 0, -s * 10 * f);
				a.rot(w + "_tip", t[i], 0, 0, -s * 8 * f);
			}
			a.rot("beak_low", t[i], 3 + 32 * cry[i], 0, 0);
			a.rot("head", t[i], -18 * cry[i], 0, 0);
			a.rot("neck", t[i], -10 * cry[i], 0, 0);
			for (int k = 0; k < 5; k++) a.rot("plume_" + k + "_0", t[i], -10 * f, (k - 2) * 18 * f, 0);
		}
	}

	// --- Phoenix Descent: the phoenix that stoops ---

	/** The stooping phoenix's fire: the Vermilion Bird's own heat, from embers to white. */
	static final int[] DESCENT_HEAT = Fire.HEAT;
	/** The stooping phoenix's look: vermilion fire, its body level as it flies, its head thrust out, its wings swept a little up. */
	static final Phoenix DESCENT = new Phoenix(DESCENT_HEAT, 0, 4, 0, -4, 8);

	/**
	 * Phoenix Descent's phoenix, the phoenix of nirvana's shape (its bones {@link #nirvanaPhoenix}'s) in vermilion fire and flying level, drawn
	 * at half its size (its kind's render scale 2): it gathers beside its caster, then stoops on its mark, a missile flying as it
	 * (SpellProjectile#body), and bursts as it strikes.
	 */
	static void descentPhoenix(SpellArt.Model m) {
		phoenixShape(m, DESCENT);
		m.scale = 2;
		descentForm(m);
		descentFly(m);
		descentStrike(m);
	}

	/** It gathers itself beside its caster (its enter, while its missile forms: half a second): swelling out of a spark, its wings opening. */
	static void descentForm(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("form", L, null);
		a.sample("root", "scale", 0, L, 6, t -> same(0.1 + 0.9 * SpellArt.easeOut(t / L)));
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, L, 6, t -> new double[] {0, -ss * 60 * (1 - SpellArt.ease(t / L)), ss * 20 * (1 - SpellArt.ease(t / L))});
		}
		a.sample("tail", "scale", 0, L, 5, t -> same(0.2 + 0.8 * SpellArt.easeOut(t / L)));
	}

	/** Its stoop (looped without a seam): fast deep wingbeats, the hand lagging the arm, the body riding them, the streamers lashing behind it. */
	static void descentFly(SpellArt.Model m) {
		double T = 0.7;
		SpellArt.Anim a = m.anim("fly", T, "true");
		int steps = 14;
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, T, steps, t -> new double[] {0, -ss * (14 + 8 * wave(t, T, 1, 0.6)), ss * 34 * wave(t, T, 1, 0)});
			a.sample(w + "_mid", "rotation", 0, T, steps, t -> new double[] {0, -ss * 10, ss * 14 * wave(t, T, 1, -0.8)});
			a.sample(w + "_tip", "rotation", 0, T, steps, t -> new double[] {0, -ss * (8 + 6 * Math.max(0, -wave(t, T, 1, -1.3))), ss * 18 * wave(t, T, 1, -1.4)});
		}
		a.sample("root", "position", 0, T, steps, t -> new double[] {0, 1.4 * wave(t, T, 1, -0.6), 0});
		a.sample("body", "rotation", 0, T, steps, t -> new double[] {3 * wave(t, T, 1, -0.4), 0, 0});
		a.sample("head", "rotation", 0, T, steps, t -> new double[] {-2.5 * wave(t, T, 1, -0.3), 0, 0});
		a.sample("crest", "rotation", 0, T, steps, t -> new double[] {-8 + 4 * wave(t, T, 2, -1), 0, 0});
		a.sample("tail", "rotation", 0, T, steps, t -> new double[] {5 * wave(t, T, 1, -1.6), 0, 0});
		for (int k = 0; k < 5; k++) {
			for (int j = 0; j < PHOENIX_SECTIONS.length; j++) {
				int kk = k, jj = j;
				a.sample("plume_" + k + "_" + j, "rotation", 0, T, steps, t -> new double[] {12 + (5 + 5 * jj) * wave(t, T, 1, -1.9 - 0.9 * jj - 0.3 * kk), (4 + 4 * jj) * wave(t, T, 1, -jj + 1.1 * kk), 0});
			}
		}
	}

	/** It strikes (played once, held: it bursts a moment after): its wings flung forward round its mark, its head thrust down, crying, its fire blazing up. */
	static void descentStrike(SpellArt.Model m) {
		double L = 0.4;
		SpellArt.Anim a = m.anim("strike", L, "hold_on_last_frame");
		a.sample("root", "scale", 0, L, 5, t -> same(1 + 0.6 * SpellArt.easeOut(t / L)));
		for (int s = -1; s <= 1; s += 2) {
			String w = "wing_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, L, 5, t -> new double[] {0, ss * 45 * SpellArt.easeOut(t / L), -ss * 30 * SpellArt.easeOut(t / L)});
		}
		a.sample("beak_low", "rotation", 0, L, 5, t -> new double[] {3 + 30 * SpellArt.easeOut(t / L), 0, 0});
		a.sample("head", "rotation", 0, L, 5, t -> new double[] {14 * SpellArt.easeOut(t / L), 0, 0});
	}

	// --- Blazing Meteor: the meteor ---

	/**
	 * The meteor's rock (painted where it lies): black and red-brown stone, fused glassy in places, split by cracks running with molten fire, its
	 * brow (-z, the way it falls) white-hot; its fire showing through.
	 */
	static SpellArt.Painter meteorRock(int seed) {
		SpellArt.Painter stone = golemStone(2.6, 0.75, seed);
		return t -> {
			double brow = SpellArt.clamp01(-t.pz / 9.0 * 0.9 + 0.1 * (t.ny > 0 ? 1 : 0));
			int c = stone.paint(t);
			if (SpellArt.glowing(c)) return SpellArt.glow(SpellArt.mix(c & 0xFFFFFF, Fire.WHITE_FIRE, brow * 0.6));
			double heat = brow * brow * (0.6 + 0.4 * SpellArt.noise3(t.px * 0.4, t.py * 0.4, t.pz * 0.4, seed + 9));
			if (heat > 0.32) return SpellArt.glow(SpellArt.ramp(heat, Fire.CRIMSON, Fire.VERMILION, Fire.FLAME, Fire.GOLD_LIGHT));
			return SpellArt.col(SpellArt.mix(SpellArt.mix(c & 0xFFFFFF, 0x2A1410, 0.45), Fire.EMBER, heat * 1.6));
		};
	}

	/** A tail of smoke (on a blade, its length along it): grey-brown, billowing wider and thinner toward its end; seen through. */
	static SpellArt.Painter smokeTail(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, half = 0.2 + 0.28 * Math.sqrt(v);
			double billow = SpellArt.fbm(u * 3 + seed, v * 5, seed, 3);
			if (Math.abs(u) > half * (0.7 + 0.5 * billow) || v < 0.05) return SpellArt.CLEAR;
			double a = (1 - Math.abs(u) / half) * (0.6 - 0.5 * v) * (0.5 + 0.6 * billow);
			return a < 0.04 ? SpellArt.CLEAR : SpellArt.col(SpellArt.mix(0x3A3230, 0x8A7E76, billow), SpellArt.clamp01(a));
		};
	}

	static final double METEOR_R = 9;

	/**
	 * Blazing Meteor's meteor, its heart at the origin and its brow toward -z (the way it falls), some five blocks across: a jagged rock ("rock",
	 * tumbling) black and red with heat, cracked through with molten fire; a mantle of flame streaming back off its brow ("flames"); a haze of its
	 * heat ("halo"); its tail of fire and, over it, of smoke ("tail", "smoke").
	 */
	static void blazingMeteor(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone rock = m.bone("rock", "root", 0, 0, 0);
		SpellArt.Painter stone = meteorRock(3901);
		// A boulder of rock, lumps of it standing out every way, no two alike, so it is jagged.
		Immortal.ball(rock, 0, 0, 0, METEOR_R * 0.85, 2.4, stone, 1.8);
		double[][] lumps = {{3.6, 2.4, -1, 7, 6, 7, 30, 20, 15}, {-3.8, -1.4, 1.6, 6, 7, 6, -25, 50, 35}, {1, -4, -2.4, 7, 5, 6, 50, -30, -20},
				{-1.6, 4, 0.6, 6, 5, 6, 15, 70, -45}, {2.6, -1.8, 4, 5, 5, 5, 40, 10, 60}, {-2.4, 1.6, -4.4, 5, 6, 5, -40, -20, 25}};
		for (double[] l : lumps) rock.box(l[0], l[1], l[2], l[3], l[4], l[5], stone).rot(l[6], l[7], l[8]).density(1.8);
		// Its mantle of flame: tongues off all of it but its back, streaming back as it falls, long off its brow.
		SpellArt.Bone flames = m.bone("flames", "root", 0, 0, 0);
		double[][] ways = sphereWays(40, 1.1);
		for (int k = 0; k < ways.length; k++) {
			double[] out = ways[k];
			if (out[2] > 0.6) continue; // its back: the tail's
			double[] way = unit(SpellArt.add(out, SpellArt.v3(0, 0, 1.2)));
			crossedTongue(m, flames, "flame_" + k % 3, SpellArt.times(out, METEOR_R * 0.7), way, 14 + 6 * SpellArt.hash(k, 3, 3901) + 6 * Math.max(0, -out[2]), 8,
					Fire.flame(2.2, 3911 + k % 3), 1.5, k % 2 == 1);
		}
		SpellArt.Bone halo = m.bone("halo", "root", 0, 0, 0);
		hazeBall(halo, 46, haze(Fire.AMBER, Fire.VERMILION, 0.16), 1);
		SpellArt.Bone tail = m.bone("tail", "root", 0, 0, METEOR_R * 0.5);
		for (int k = 0; k < 5; k++) {
			double heading = (k - 2) * 6, pitch = (k % 2 == 0 ? 1 : -1) * 4 * (k > 0 ? 1 : 0), total = 52 + 8 * SpellArt.hash(k, 4, 3901);
			double[] dir = SpellArt.turn(SpellArt.v3(0, 0, 1), -pitch, heading, 0), start = SpellArt.v3(dir[0] * 3, dir[1] * 3, METEOR_R * 0.5);
			double[] mid = SpellArt.add(start, SpellArt.times(dir, total * 0.45)), end = SpellArt.add(start, SpellArt.times(dir, total));
			SpellArt.Bone a = m.bone("tail_" + k + "_0", "tail", start[0], start[1], start[2]);
			Water.ribbon(m, a, "tail_" + k + "_0", start, mid, 18 - k, cometTail(total, 0, 0.45, 3921 + k), 1.5, true);
			SpellArt.Bone b = m.bone("tail_" + k + "_1", "tail_" + k + "_0", mid[0], mid[1], mid[2]);
			Water.ribbon(m, b, "tail_" + k + "_1", mid, end, (18 - k) * 0.7, cometTail(total, 0.45, 1, 3921 + k), 1.2, true);
		}
		SpellArt.Bone smoke = m.bone("smoke", "root", 0, 0, METEOR_R);
		for (int k = 0; k < 3; k++) {
			double[] start = SpellArt.v3((k - 1) * 3, 2 + k, METEOR_R + 4), end = SpellArt.add(start, SpellArt.v3((k - 1) * 6, 6 + 3 * k, 70));
			Water.ribbon(m, smoke, "smoke_" + k, start, end, 26, smokeTail(3931 + k), 0.8, true);
		}
		meteorFall(m);
		meteorImpact(m);
	}

	/** Its fall (looped without a seam): the rock tumbling, its flames flickering, its glow breathing, its tail lashing, its smoke rolling. */
	static void meteorFall(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("fall", T, "true");
		a.sample("rock", "rotation", 0, T, 16, t -> new double[] {360 * t / T, 0, 180 * t / T});
		a.sample("flames", "scale", 0, T, 20, t -> same(1 + 0.08 * wave(t, T, 8, 0)));
		a.sample("halo", "scale", 0, T, 10, t -> same(1 + 0.06 * wave(t, T, 3, 1)));
		for (int k = 0; k < 5; k++) {
			double ph = k * 1.4;
			a.sample("tail_" + k + "_0", "rotation", 0, T, 20, t -> new double[] {5 * wave(t, T, 4, ph), 6 * wave(t, T, 3, ph + 1), 0});
			a.sample("tail_" + k + "_1", "rotation", 0, T, 20, t -> new double[] {9 * wave(t, T, 4, ph - 1.2), 10 * wave(t, T, 3, ph - 0.4), 0});
		}
		a.sample("smoke", "rotation", 0, T, 10, t -> new double[] {0, 0, 360 * t / T});
	}

	/** It strikes (played once, held: it is gone a moment after): its fire bursting outward, its tail and smoke snuffed. */
	static void meteorImpact(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("impact", 0.5, "hold_on_last_frame");
		a.sample("flames", "scale", 0, 0.5, 5, t -> same(1 + 2 * SpellArt.easeOut(t / 0.5)));
		a.sample("halo", "scale", 0, 0.5, 5, t -> same(1 + 2.6 * SpellArt.easeOut(t / 0.5)));
		a.sample("rock", "scale", 0, 0.5, 5, t -> same(Math.max(0.05, 1 - 0.7 * SpellArt.easeIn(t / 0.5))));
		a.sample("tail", "scale", 0, 0.3, 4, t -> same(Math.max(0.02, 1 - SpellArt.easeIn(t / 0.3))));
		a.scale("tail", 0.5, 0.02);
		a.sample("smoke", "scale", 0, 0.3, 4, t -> same(Math.max(0.02, 1 - SpellArt.easeIn(t / 0.3))));
		a.scale("smoke", 0.5, 0.02);
	}

	// --- Falling Mountain: the mountain that falls ---

	/** The mountain's peaks: x, z (pixels from its middle), height, breadth at the foot. */
	static final double[][] PEAK = {{0, 0, 40, 22}, {-12, 7, 26, 15}, {11, -8, 30, 16}, {9, 11, 20, 12}, {-10, -10, 18, 11}};
	static final double[] PEAK_TIERS = {0.24, 0.2, 0.18, 0.15, 0.13, 0.1};
	/** Where its torn foot is, in pixels over its middle (the missile's: its underside hangs down to the missile's own foot). */
	static final double PEAK_BASE = 2;

	/** The mountain's stone (Earth#rock), and snow on whatever faces up high on it. */
	static SpellArt.Painter snowRock(int seed, double snowFrom) {
		SpellArt.Painter rock = Earth.rock(seed);
		return t -> {
			double snow = SpellArt.smooth(snowFrom, snowFrom + 8, t.py) * (t.ny > 0.35 ? 1 : 0.35 * SpellArt.smooth(0.6, 1, t.up()));
			if (snow > 0.5 - 0.3 * SpellArt.noise(t.px * 0.5, t.pz * 0.5, seed)) return SpellArt.col(SpellArt.mix(0xD8DDE2, 0xFFFFFF, SpellArt.noise(t.px, t.pz, seed + 1)));
			return rock.paint(t);
		};
	}

	/** The torn earth beneath it: dark soil and broken rock, roots hanging out of it. */
	static SpellArt.Painter tornEarth(int seed) {
		SpellArt.Painter stone = golemStone(2.4, 0.05, seed);
		return t -> {
			double n = SpellArt.fbm3(t.px * 0.3, t.py * 0.3, t.pz * 0.3, seed, 3);
			if (n > 0.55) return stone.paint(t);
			return SpellArt.col(SpellArt.mix(0x241A12, 0x4E3A28, n * 1.6));
		};
	}

	/**
	 * Falling Mountain's mountain, upright (its missile carries it so), its torn foot {@link #PEAK_BASE} pixels over its middle, some ten blocks
	 * across and twelve tall: a summit and four lesser peaks of crags stacked in tiers ("peak", tumbling a little as it falls), snow on their
	 * heights, pines clinging to them, the earth it was torn from hanging under it ("under", roots and rocks), stones falling round it ("debris"),
	 * mist clinging round its shoulders ("mist").
	 */
	static void fallingPeak(SpellArt.Model m) {
		m.tpp = 1.2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone peak = m.bone("peak", "root", 0, PEAK_BASE, 0);
		for (int p = 0; p < PEAK.length; p++) {
			double[] pk = PEAK[p];
			double y = PEAK_BASE - 0.5, x = pk[0], z = pk[1];
			for (int i = 0; i < PEAK_TIERS.length; i++) {
				double hh = pk[2] * PEAK_TIERS[i], w = pk[3] * (1 - i * 0.14);
				double h1 = SpellArt.hash(p, i, 13) - 0.5, h2 = SpellArt.hash(p, i, 14) - 0.5, h3 = SpellArt.hash(p, i, 15);
				peak.box(x, y + hh / 2, z, w, hh + 1, w * (0.8 + 0.3 * h3), snowRock(4001 + p, PEAK_BASE + 24)).rot(h1 * 7, h3 * 90, h2 * 7).skip(SpellArt.Face.DOWN);
				y += hh;
				x += h1 * w * 0.12;
				z += h2 * w * 0.12;
			}
			// Its point: two last narrow crags, turned against each other.
			double cap = pk[3] * 0.34;
			peak.box(x, y + cap * 0.6, z, cap, cap * 1.4, cap, snowRock(4001 + p, PEAK_BASE + 24)).rot(0, 45 + p * 17, 6).skip(SpellArt.Face.DOWN);
			peak.box(x, y + cap * 1.4, z, cap * 0.5, cap * 1.1, cap * 0.5, snowRock(4001 + p, PEAK_BASE + 24)).rot(4, p * 17, -6).skip(SpellArt.Face.DOWN);
			if (pk[2] >= 24) {
				double side = SpellArt.hash(p, 9, 16) < 0.5 ? 1 : -1, py = PEAK_BASE + pk[2] * 0.45;
				double[] foot = SpellArt.v3(pk[0] + side * pk[3] * 0.34, py, pk[1]), crown = SpellArt.add(foot, SpellArt.v3(side * 3.6, 3, -0.6));
				SpellArt.limb(peak, foot, crown, 1, 1, Wood.bark(4011 + p, 0)).density(2);
				peak.box(crown[0], crown[1] + 0.5, crown[2], 6, 1.4, 4.6, Earth.needles(4021 + p)).rot(0, 20 * side, -8 * side).density(2);
			}
		}
		// The earth it was torn up with, hanging under it: a broken cone of soil and rock, roots trailing out of it.
		SpellArt.Bone under = m.bone("under", "root", 0, PEAK_BASE, 0);
		double[] rings = {22, 18, 13, 8, 4};
		for (int i = 0; i < rings.length; i++) {
			double y = PEAK_BASE - 1.4 - i * 2.8, w = rings[i];
			under.box(SpellArt.hash(i, 1, 17) * 2 - 1, y, SpellArt.hash(i, 2, 17) * 2 - 1, w, 3, w * 0.9, tornEarth(4031 + i)).rot(0, i * 33, 0).skip(SpellArt.Face.UP);
		}
		for (int k = 0; k < 8; k++) {
			double th = Math.toRadians(45 * k + 10), r = 6 + 5 * SpellArt.hash(k, 3, 17);
			double[] a = SpellArt.v3(Math.sin(th) * r, PEAK_BASE - 2, Math.cos(th) * r), b = SpellArt.add(a, SpellArt.v3(Math.sin(th) * 2, -5 - 4 * SpellArt.hash(k, 4, 17), Math.cos(th) * 2));
			SpellArt.limb(under, a, b, 0.8, 0.8, Wood.bark(4041 + k, 0)).density(3);
		}
		SpellArt.Bone debris = m.bone("debris", "root", 0, PEAK_BASE, 0);
		for (int k = 0; k < 7; k++) {
			double th = Math.toRadians(51 * k), r = 20 + 4 * SpellArt.hash(k, 5, 17), c = 2 + 2 * SpellArt.hash(k, 6, 17);
			debris.box(Math.sin(th) * r, PEAK_BASE - 4 + 12 * SpellArt.hash(k, 7, 17), Math.cos(th) * r, c, c * 0.8, c, golemStone(1.4, 0, 4051 + k)).rot(30 * k, 50 * k, 20 * k).density(2);
		}
		SpellArt.Bone mist = m.bone("mist", "root", 0, PEAK_BASE + 14, 0);
		SpellArt.sheet(mist, 0, PEAK_BASE + 14, 0, 58, 58, 0, 20, 0, Earth.mist(4061)).density(0.8);
		peakFall(m);
		peakImpact(m);
	}

	/** Its fall (looped without a seam): the mountain swaying a little as it falls, its stones turning round it, its mist drifting. */
	static void peakFall(SpellArt.Model m) {
		double T = 3;
		SpellArt.Anim a = m.anim("fall", T, "true");
		a.sample("peak", "rotation", 0, T, 12, t -> new double[] {3 * wave(t, T, 1, 0), 0, 3 * wave(t, T, 1, 1.6)});
		a.sample("under", "rotation", 0, T, 12, t -> new double[] {3 * wave(t, T, 1, 0), 0, 3 * wave(t, T, 1, 1.6)});
		a.sample("debris", "rotation", 0, T, 12, t -> new double[] {0, 360 * t / T, 0});
		a.sample("mist", "rotation", 0, T, 12, t -> new double[] {0, -360 * t / T, 0});
	}

	/** It lands (played once, held: it is gone a moment after): driven down into the land, settling as it breaks, its stones flung out. */
	static void peakImpact(SpellArt.Model m) {
		double L = 0.5;
		SpellArt.Anim a = m.anim("impact", L, "hold_on_last_frame");
		a.sample("peak", "position", 0, L, 5, t -> new double[] {0, -10 * SpellArt.easeOut(t / L), 0});
		a.sample("peak", "scale", 0, L, 5, t -> new double[] {1 + 0.12 * SpellArt.easeOut(t / L), 1 - 0.25 * SpellArt.easeOut(t / L), 1 + 0.12 * SpellArt.easeOut(t / L)});
		a.sample("under", "position", 0, L, 5, t -> new double[] {0, -10 * SpellArt.easeOut(t / L), 0});
		a.sample("debris", "scale", 0, L, 5, t -> same(1 + 1.4 * SpellArt.easeOut(t / L)));
		a.sample("mist", "scale", 0, L, 5, t -> same(1 + 1.2 * SpellArt.easeOut(t / L)));
	}

	// --- Sword Domain: the domain of swords ---

	/**
	 * A jian of the domain, lying along +z from {@code at} (its middle), turned by {@code rx}, {@code ry} about it: its steel blade (Metal#steel),
	 * gold guard, silk grip, pommel and a trail of sword-light behind it; painted once (by the first, {@code like}, if not null) and shared.
	 */
	static SpellArt.Cube[] domainSword(SpellArt.Bone bone, double[] at, double rx, double ry, SpellArt.Cube[] like) {
		double x = at[0], y = at[1], z = at[2];
		double[][] parts = {{0, 0, 4.6}, {0, 0, -3.2}, {0, 0, -5.6}, {0, 0, -8}, {0, 0.05, -16.4}};
		SpellArt.Cube[] c = new SpellArt.Cube[5];
		if (like == null) {
			c[0] = bone.box(x, y, z + 4.6, 2, 0.4, 15, Metal.steel(4101)).density(3);
			c[1] = bone.box(x, y, z - 3.2, 4.4, 0.9, 0.9, Metal.gold(4103)).density(3);
			c[2] = bone.box(x, y, z - 5.6, 0.8, 0.8, 3.8, Metal.grip()).density(3);
			c[3] = bone.box(x, y, z - 8, 1.3, 1.3, 1.3, Metal.gold(4105)).density(3);
			c[4] = bone.box(x, y + 0.05, z - 16.4, 2.4, 0.1, 15, Metal.trail()).density(2);
		} else {
			for (int i = 0; i < 5; i++) c[i] = bone.like(like[i], x + parts[i][0], y + parts[i][1], z + parts[i][2], false);
		}
		for (SpellArt.Cube cube : c) cube.rot(rx, ry, 0, x, y, z);
		return c;
	}

	/** The domain's formation on the ground (on a sheet): rings of gold light, swords' shapes laid round it pointing out, seal script between; glowing, seen through. */
	static SpellArt.Painter swordArray(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 1) return SpellArt.CLEAR;
			if (Math.abs(r - 0.97) < 0.012 || Math.abs(r - 0.6) < 0.01 || Math.abs(r - 0.25) < 0.012) return SpellArt.glow(SpellArt.mix(Metal.GOLD, Metal.GLEAM, 0.4), 0.85);
			// Eight swords laid from the inner ring out, points out.
			double sector = Math.PI / 4, off = (a % sector + sector) % sector - sector / 2, along = (r - 0.27) / 0.66, wide = 0.05 * (1 - Math.pow(Math.max(0, along - 0.75) / 0.25, 1));
			if (along > 0 && along < 1 && Math.abs(off) * r < Math.max(0.004, wide) * (along < 0.12 ? 2.2 : 1)) return SpellArt.glow(SpellArt.mix(Metal.GLEAM, 0xFFFFFF, along), 0.8);
			if (r > 0.64 && r < 0.93) {
				double cell = (a / (2 * Math.PI) + 0.5) * 32;
				int ci = (int) Math.floor(cell);
				if (Earth.character(seed + ci, (cell - ci - 0.12) / 0.76, ((r - 0.64) / 0.29 - 0.12) / 0.76)) return SpellArt.glow(SpellArt.mix(Metal.GOLD, Metal.GLEAM, 0.2), 0.7);
			}
			return SpellArt.CLEAR;
		};
	}

	/** A blade of the domain's wall (on a blade standing up): a thin thread of sword-light, fading toward its top and foot. */
	static SpellArt.Painter wallLight() {
		return t -> {
			double v = t.v(), u = Math.abs(t.u() - 0.5) * 2, a = (1 - u) * Math.sin(Math.PI * v);
			return a < 0.05 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(Metal.GLEAM, 0xFFFFFF, 1 - u), 0.45 * a);
		};
	}

	static final int DOMAIN_LOW = 12, DOMAIN_HIGH = 8, DOMAIN_WALL = 24;
	static final double DOMAIN_R = 40;

	/**
	 * Sword Domain round its caster (their feet at the origin), ten blocks round: twelve swords wheeling low round its edge ("ring_low", turning on
	 * "ring_low_spin"), eight wheeling the other way higher in, pointing down and out at the domain's edge ("ring_high", "ring_high_spin"); the
	 * formation laid on the ground under them ("array"); threads of sword-light standing round its bounds ("wall").
	 */
	static void swordDomain(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone array = m.bone("array", "root", 0, 0, 0);
		SpellArt.sheet(array, 0, 0.3, 0, DOMAIN_R * 2.1, DOMAIN_R * 2.1, 0, 0, 0, swordArray(4111)).density(0.9);
		SpellArt.Cube[] first = null;
		m.bone("ring_low", "root", 0, 5, 0);
		SpellArt.Bone low = m.bone("ring_low_spin", "ring_low", 0, 5, 0);
		for (int k = 0; k < DOMAIN_LOW; k++) {
			double th = 360.0 * k / DOMAIN_LOW;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * DOMAIN_R * 0.75, 5, Math.cos(Math.toRadians(th)) * DOMAIN_R * 0.75);
			SpellArt.Cube[] made = domainSword(low, at, 0, th + 90, first);
			if (first == null) first = made;
		}
		m.bone("ring_high", "root", 0, 16, 0);
		SpellArt.Bone high = m.bone("ring_high_spin", "ring_high", 0, 16, 0);
		for (int k = 0; k < DOMAIN_HIGH; k++) {
			double th = 360.0 * k / DOMAIN_HIGH + 22.5;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * DOMAIN_R * 0.45, 16, Math.cos(Math.toRadians(th)) * DOMAIN_R * 0.45);
			domainSword(high, at, -28, th, first);
		}
		SpellArt.Bone wall = m.bone("wall", "root", 0, 0, 0);
		for (int k = 0; k < DOMAIN_WALL; k++) {
			double th = 360.0 * k / DOMAIN_WALL;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * DOMAIN_R, 0, Math.cos(Math.toRadians(th)) * DOMAIN_R);
			SpellArt.shared(m, wall, "wall", false, base, 14, 1.4, 0, th, 0, wallLight(), 2, false);
		}
		domainUnfold(m);
		domainWheel(m);
		domainStrike(m);
	}

	/** It unfolds (its enter): the swords flung out from its caster to their rings, the formation spreading, the wall rising. */
	static void domainUnfold(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("unfold", L, null);
		for (String ring : new String[] {"ring_low", "ring_high"}) a.sample(ring, "scale", 0, L, 6, t -> new double[] {0.05 + 0.95 * SpellArt.easeOut(t / L), 1, 0.05 + 0.95 * SpellArt.easeOut(t / L)});
		a.sample("array", "scale", 0, L, 6, t -> new double[] {0.1 + 0.9 * SpellArt.easeOut(t / L), 1, 0.1 + 0.9 * SpellArt.easeOut(t / L)});
		a.sample("wall", "scale", 0, L, 6, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut((t - 0.25) / (L - 0.25))), 1});
	}

	/** It holds (looped without a seam): each ring wheeling two swords' spacing round, the other way to the other; the wall's light turning slowly. */
	static void domainWheel(SpellArt.Model m) {
		double T = 2;
		SpellArt.Anim a = m.anim("wheel", T, "true");
		a.sample("ring_low_spin", "rotation", 0, T, 4, t -> new double[] {0, 2 * 360.0 / DOMAIN_LOW * t / T, 0});
		a.sample("ring_high_spin", "rotation", 0, T, 4, t -> new double[] {0, -2 * 360.0 / DOMAIN_HIGH * t / T, 0});
		a.sample("wall", "rotation", 0, T, 4, t -> new double[] {0, 360.0 / DOMAIN_WALL * t / T, 0});
		a.sample("ring_high", "position", 0, T, 8, t -> new double[] {0, 1.2 * wave(t, T, 1, 0), 0});
	}

	/** It cuts (played once): both rings darting in on what stands inside, and back out. */
	static void domainStrike(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("strike", 0.4, null);
		double[] t = {0, 0.1, 0.4};
		double[] in = {1, 0.72, 1};
		for (int i = 0; i < t.length; i++) {
			a.key("ring_low", "scale", t[i], in[i], 1, in[i]);
			a.key("ring_high", "scale", t[i], 0.5 + 0.5 * in[i], 1, 0.5 + 0.5 * in[i]);
		}
	}

	// --- Heaven-Piercing Bamboo: the bamboo that pierces ---

	/** Spirit bamboo (on a culm, its length up its cube): jade green, faintly striped along it, ringed at each node, a band of jade light under each ring. */
	static SpellArt.Painter spiritBamboo(double node, int seed) {
		return t -> {
			double y = t.qy(), f = y / node - Math.floor(y / node);
			if (f < 0.05) return SpellArt.col(SpellArt.mix(0x2E5A2A, 0x467838, f / 0.05));
			if (f < 0.12) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, 0.3), 0.85);
			double stripe = Math.abs(Math.sin((t.lx + t.lz) * 2.4 + seed)), n = SpellArt.noise(t.lx + t.lz + seed, y * 0.2, seed);
			int c = SpellArt.mix(SpellArt.mix(0x4E9A44, 0x8ACA6A, 0.3 + 0.4 * n), 0x3A7A38, 0.25 * stripe);
			if (t.ny > 0.5) c = SpellArt.mix(c, 0xB8E098, 0.3);
			return SpellArt.col(Earth.warm(c & 0xFFFFFF, -0.12 + 0.16 * (1 - Math.abs(t.fx - 0.5) * 2)));
		};
	}

	/** A culm's cut point (on a blade, its length along it): green narrowing to a keen slanted tip, its cut face pale, its tip glowing jade. */
	static SpellArt.Painter bambooPoint() {
		return t -> {
			double v = t.v(), u = t.u(), half = 1 - v;
			if (u > half + 0.02) return SpellArt.CLEAR;
			if (v > 0.75) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, v), 0.92);
			if (half - u < 0.12) return SpellArt.col(0xD8E8B0);
			return SpellArt.col(SpellArt.mix(0x4E9A44, 0x8ACA6A, 0.5 * v));
		};
	}

	/** Each culm: where it bursts up (x, z), its height, its girth, its lean (degrees) and the way it leans. */
	static final double[][] CULMS = {{0, 0, 72, 3.6, 3, 0}, {3.4, 1.6, 60, 3.2, 9, 30}, {-3, 2.2, 64, 3.2, 8, 150}, {1.2, -3.6, 54, 2.8, 11, 260}, {-2.8, -2.4, 48, 2.6, 13, 210},
			{4.2, -2, 42, 2.4, 15, 320}};

	/**
	 * Heaven-Piercing Bamboo's bamboo under one being it pierces (its foot at the origin): six culms of spirit bamboo bursting up out of the broken
	 * ground, the tallest eighteen blocks ("culm_0".."culm_5", each its own bone so it shoots up on its own), cut to keen points, leaves at their upper
	 * nodes; the ground they broke ("ground").
	 */
	static void piercingBamboo(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0, 0);
		SpellArt.sheet(ground, 0, 0.2, 0, 26, 26, 0, 0, 0, crackedEarth(4201)).density(2);
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k + 20);
			ground.box(Math.sin(th) * 8, 0.6, Math.cos(th) * 8, 3, 1.6, 2.4, golemStone(1.2, 0, 4203 + k)).rot(10 * k, 47 * k, 15).density(3);
		}
		SpellArt.Painter leaf = Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.0, 4211);
		for (int k = 0; k < CULMS.length; k++) {
			double[] c = CULMS[k];
			double[] foot = SpellArt.v3(c[0], 0, c[1]), way = SpellArt.turn(SpellArt.v3(0, 1, 0), 0, c[5], c[4]);
			double[] top = SpellArt.add(foot, SpellArt.times(way, c[2]));
			SpellArt.Bone culm = m.bone("culm_" + k, "root", foot[0], foot[1], foot[2]);
			double body = c[2] - 6;
			SpellArt.limb(culm, foot, SpellArt.add(foot, SpellArt.times(way, body)), c[3], c[3], spiritBamboo(8, 4221 + k)).density(2.5);
			crossedTongue(m, culm, "point", SpellArt.add(foot, SpellArt.times(way, body - 0.4)), way, 6.4, c[3], bambooPoint(), 3, k % 2 == 1);
			// Leaves at the upper nodes, a spray of them on short twigs.
			for (int j = 0; j < 3; j++) {
				double[] at = SpellArt.add(foot, SpellArt.times(way, body * (0.55 + 0.14 * j)));
				double heading = c[5] + 90 + j * 120 + k * 33;
				SpellArt.shared(m, culm, "bamboo_leaf", false, at, 7, 1.8, -64, heading, 12, leaf, 4, j % 2 == 1);
				SpellArt.shared(m, culm, "bamboo_leaf", false, at, 7, 1.8, -50, heading + 40, -8, leaf, 4, j % 2 == 0);
			}
		}
		bambooBurst(m);
		bambooSway(m);
	}

	/** It bursts up (its enter, as fast as a spear is thrust): the ground breaking, every culm shooting up out of it, one on another. */
	static void bambooBurst(SpellArt.Model m) {
		double L = 0.35;
		SpellArt.Anim a = m.anim("burst", L, null);
		a.sample("ground", "scale", 0, 0.12, 3, t -> new double[] {0.2 + 0.8 * SpellArt.easeOut(t / 0.12), 1, 0.2 + 0.8 * SpellArt.easeOut(t / 0.12)});
		for (int k = 0; k < CULMS.length; k++) {
			double t0 = 0.02 * k, t1 = t0 + 0.14;
			String b = "culm_" + k;
			a.scale(b, 0, 1, 0.02, 1);
			if (t0 > 0) a.key(b, "scale", t0, 1, 0.02, 1);
			a.key(b, "scale", t1, 1, 1.06, 1);
			a.scale(b, L, 1);
		}
	}

	/** It stands (looped without a seam): the culms swaying a little, each its own way. */
	static void bambooSway(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("sway", T, "true");
		for (int k = 0; k < CULMS.length; k++) {
			double ph = k * 1.3;
			a.sample("culm_" + k, "rotation", 0, T, 8, t -> new double[] {1.6 * wave(t, T, 1, ph), 0, 1.6 * wave(t, T, 1, ph + 1.4)});
		}
	}

	// --- Lotus of Rebirth: the lotus ---

	/** A lotus petal (on a flat blade, its length along it): broad and pointed, white at its foot flushed to rose at its tip, its veins glowing; seen through a little at its rim. */
	static SpellArt.Painter lotusPetal(int seed) {
		return t -> {
			double v = t.v(), u = t.u() - 0.5, au = Math.abs(u);
			double half = 0.5 * Math.pow(Math.sin(Math.PI * Math.min(1, Math.pow(v, 0.62))), 0.7) * (v > 0.8 ? (1 - v) / 0.2 * 0.6 + 0.4 : 1);
			if (au > half || v > 0.99) return SpellArt.CLEAR;
			double vein = Math.abs(Math.sin((u / Math.max(0.05, half)) * 3.2 * Math.PI + v * 2));
			int c = SpellArt.ramp(v, 0xFFF6F0, 0xFFE0E8, 0xFFB0C8, 0xF07CA8);
			if (vein < 0.12 && v > 0.08) return SpellArt.glow(SpellArt.mix(c, 0xFFE8A8, 0.5), 0.9);
			if (half - au < 0.04) return SpellArt.glow(SpellArt.mix(c, 0xFFFFFF, 0.4), 0.8);
			return SpellArt.glow(SpellArt.shade(c, -0.08 * au / Math.max(0.05, half)), 0.9);
		};
	}

	/** A lotus pad (on a sheet lying level): round and green, notched to its middle, veins running out from its middle, a bead of light on it here and there. */
	static SpellArt.Painter lotusPad(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.98 - 0.04 * Math.abs(Math.sin(a * 9 + seed)) || (Math.abs(a - 0.6) < 0.08 && r > 0.1)) return SpellArt.CLEAR;
			if (Math.abs(Math.sin(a * 11 + seed)) < 0.06 && r > 0.08) return SpellArt.col(0x8CC870);
			if (SpellArt.hash((int) Math.floor(x * 9), (int) Math.floor(z * 9), seed) > 0.985) return SpellArt.glow(0xE8FFF4, 0.9);
			return SpellArt.col(SpellArt.mix(0x2E6A30, 0x5AA448, 0.3 + 0.5 * SpellArt.noise(x * 4 + seed, z * 4, seed) + 0.2 * (1 - r)));
		};
	}

	/** The pod at the lotus's heart (on a round): gold, its top pitted with seeds that glow. */
	static SpellArt.Painter lotusPod(int seed) {
		return t -> {
			if (t.f == SpellArt.Face.UP) {
				double x = t.px / 1.6, z = t.pz / 1.6;
				double d = Math.hypot(x - Math.rint(x), z - Math.rint(z));
				if (d < 0.22) return SpellArt.glow(0xFFF4C0);
			}
			return SpellArt.col(SpellArt.mix(0xC89A3A, 0xF0D070, 0.4 + 0.4 * SpellArt.noise(t.px * 0.8, t.py * 0.8 + t.pz * 0.8, seed)));
		};
	}

	/** The lotus's rings of petals: how many, the radius of their feet, their length, breadth, and how far they lean out from upright (degrees). */
	static final double[][] LOTUS_RINGS = {{10, 7, 26, 14, 62}, {8, 5, 20, 12, 40}, {6, 3, 14, 9, 18}};

	/**
	 * Lotus of Rebirth's great lotus at its caster's feet (the origin), opening under them: three rings of petals ("petal_0_0".., each its own bone,
	 * so it opens), the pod at its heart and the stamens round it ("heart"), lotus pads spread over the ground round it out to its reach ("pads"), a
	 * glow of its light rising ("light").
	 */
	static void rebirthLotus(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone pads = m.bone("pads", "root", 0, 0, 0);
		for (int k = 0; k < 11; k++) {
			double th = Math.toRadians(360.0 * k / 11 + 13 * SpellArt.hash(k, 1, 4301)), r = 24 + 14 * SpellArt.hash(k, 2, 4301), size = 12 + 7 * SpellArt.hash(k, 3, 4301);
			SpellArt.sheet(pads, Math.sin(th) * r, 0.3 + 0.1 * k, Math.cos(th) * r, size, size, 0, 40 * k, 0, lotusPad(4303 + k % 3)).density(2);
		}
		SpellArt.Bone flower = m.bone("flower", "root", 0, 0, 0);
		for (int ring = 0; ring < LOTUS_RINGS.length; ring++) {
			double[] r = LOTUS_RINGS[ring];
			for (int k = 0; k < r[0]; k++) {
				double th = 360.0 * (k + 0.5 * ring) / r[0];
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * r[1], 1 + ring, Math.cos(Math.toRadians(th)) * r[1]);
				SpellArt.Bone petal = m.bone("petal_" + ring + "_" + k, "flower", base[0], base[1], base[2], 0, th, 0);
				// Built pointing +z from its foot (its bone turns it out), lifted from the level by how far it leans in from upright.
				SpellArt.shared(m, petal, "petal_" + ring, true, base, r[2], r[3], 90 - r[4], 0, 0, lotusPetal(4311 + ring), 2, k % 2 == 1);
			}
		}
		SpellArt.Bone heart = m.bone("heart", "root", 0, 0, 0);
		Vessel.round(heart, 0, 1.6, 0, 4.4, 3.2, lotusPod(4321), true);
		for (int k = 0; k < 12; k++) {
			double th = 30 * k;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 4.8, 1.2, Math.cos(Math.toRadians(th)) * 4.8);
			SpellArt.shared(m, heart, "stamen", false, base, 5, 0.8, -20, th + 180, 0, t -> SpellArt.glow(SpellArt.mix(0xF0C040, 0xFFF4C0, t.v()), 0.95), 4, false);
		}
		SpellArt.Bone light = m.bone("light", "root", 0, 0, 0);
		SpellArt.sheet(light, 0, 18, 0, 36, 36, 90, 0, 0, haze(0xFFF0E0, 0xFFB0C8, 0.2)).density(1.5);
		SpellArt.sheet(light, 0, 18, 0, 36, 36, 90, 90, 0, haze(0xFFF0E0, 0xFFB0C8, 0.2)).density(1.5);
		lotusBloom(m);
		lotusGlow(m);
	}

	/** It opens (its enter): its pads spreading out over the ground, then its petals unfolding from a closed bud, ring by ring, the outer first; its light rising. */
	static void lotusBloom(SpellArt.Model m) {
		double L = 1.2;
		SpellArt.Anim a = m.anim("bloom", L, null);
		a.sample("pads", "scale", 0, 0.6, 5, t -> new double[] {Math.max(0.05, SpellArt.easeOut(t / 0.6)), 1, Math.max(0.05, SpellArt.easeOut(t / 0.6))});
		for (int ring = 0; ring < LOTUS_RINGS.length; ring++) {
			double[] r = LOTUS_RINGS[ring];
			double t0 = 0.2 + 0.15 * ring;
			for (int k = 0; k < r[0]; k++) {
				String b = "petal_" + ring + "_" + k;
				// Closed: lifted up to upright and a little in, over the heart.
				a.rot(b, 0, r[4] + 12, 0, 0);
				a.rot(b, t0, r[4] + 12, 0, 0);
				a.sample(b, "rotation", t0, L, 5, t -> new double[] {(r[4] + 12) * (1 - SpellArt.ease((t - t0) / (L - t0))), 0, 0});
			}
		}
		a.sample("light", "scale", 0, L, 5, t -> new double[] {1, Math.max(0.05, SpellArt.easeOut(t / L)), 1});
	}

	/** It glows (looped without a seam): its petals stirring, its pads rocking on the ground, its light breathing. */
	static void lotusGlow(SpellArt.Model m) {
		double T = 4;
		SpellArt.Anim a = m.anim("glow", T, "true");
		for (int ring = 0; ring < LOTUS_RINGS.length; ring++) {
			for (int k = 0; k < LOTUS_RINGS[ring][0]; k++) {
				double ph = k * 0.9 + ring;
				a.sample("petal_" + ring + "_" + k, "rotation", 0, T, 8, t -> new double[] {3 * wave(t, T, 1, ph), 0, 0});
			}
		}
		a.sample("light", "scale", 0, T, 8, t -> new double[] {1, 1 + 0.12 * wave(t, T, 2, 0), 1});
		a.sample("pads", "rotation", 0, T, 8, t -> new double[] {0.6 * wave(t, T, 1, 0), 0, 0.6 * wave(t, T, 1, 1.5)});
	}

	// --- Thorn Spear Forest: the spears ---

	/** A spear of living wood (on a limb): dark bark ridged along it, its upper part glowing faintly with the wood's qi. */
	static SpellArt.Painter spearWood(int seed) {
		SpellArt.Painter wood = bindingVine(seed);
		return t -> {
			int c = wood.paint(t);
			return SpellArt.glowing(c) ? c : SpellArt.col(Earth.warm(c & 0xFFFFFF, -0.15));
		};
	}

	static final int SPEARS = 18;

	/**
	 * Thorn Spear Forest's spears (their ground's middle at the origin, six blocks round): {@link #SPEARS} spears of living wood thrust up out of the
	 * cracked ground, two to four blocks tall, leaning every way ("spear_0".., each its own bone, so each shoots up on its own), thorns along them;
	 * the cracked ground ("ground").
	 */
	static void thornForest(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0, 0);
		SpellArt.sheet(ground, 0, 0.2, 0, 54, 54, 0, 0, 0, crackedEarth(4401)).density(1.2);
		SpellArt.Painter thorn = thorn();
		for (int k = 0; k < SPEARS; k++) {
			double th = 2 * Math.PI * k * 0.618034, r = 22 * Math.sqrt((k + 0.5) / SPEARS);
			double[] foot = SpellArt.v3(Math.sin(th) * r, 0, Math.cos(th) * r);
			// Three and a half to six blocks tall, the tallest at its middle, leaning out.
			double lean = 8 + 14 * SpellArt.hash(k, 1, 4401), dir = Math.toDegrees(th) + (SpellArt.hash(k, 2, 4401) - 0.5) * 60;
			double len = (14 + 8 * SpellArt.hash(k, 3, 4401)) * (1.15 - 0.3 * r / 22);
			double[] way = SpellArt.turn(SpellArt.v3(0, 1, 0), 0, dir, lean);
			SpellArt.Bone spear = m.bone("spear_" + k, "root", foot[0], foot[1], foot[2]);
			double[] mid = SpellArt.add(foot, SpellArt.times(way, len * 0.55)), end = SpellArt.add(foot, SpellArt.times(way, len * 0.85));
			SpellArt.limb(spear, SpellArt.add(foot, SpellArt.v3(0, -1, 0)), mid, 3.2, 3.2, spearWood(4411 + k % 3)).density(3);
			SpellArt.limb(spear, mid, end, 2.1, 2.1, spearWood(4411 + k % 3)).density(3);
			crossedTongue(m, spear, "spear_tip", SpellArt.sub(end, SpellArt.times(way, 0.3)), way, len * 0.2 + 1, 2.1, thorn, 5, k % 2 == 1);
			for (int j = 0; j < 2; j++) {
				double[] at = SpellArt.add(foot, SpellArt.times(way, len * (0.3 + 0.3 * j)));
				double[] out = unit(SpellArt.add(SpellArt.turn(SpellArt.v3(1, 0, 0), 0, dir + 140 * j + 30, 0), SpellArt.times(way, 0.9)));
				crossedTongue(m, spear, "side_thorn", at, out, 2, 1.1, thorn, 6, j == 1);
			}
		}
		spearsErupt(m);
		spearsStand(m);
	}

	/** They erupt (its enter): the ground cracking open and stirring, then every spear thrust up out of it at once, a moment after (its blow). */
	static void spearsErupt(SpellArt.Model m) {
		double L = 0.45;
		SpellArt.Anim a = m.anim("erupt", L, null);
		a.sample("ground", "scale", 0, 0.25, 4, t -> new double[] {0.2 + 0.8 * SpellArt.easeOut(t / 0.25), 1, 0.2 + 0.8 * SpellArt.easeOut(t / 0.25)});
		for (int k = 0; k < SPEARS; k++) {
			String b = "spear_" + k;
			double t0 = 0.27 + 0.004 * k;
			a.scale(b, 0, 0.12, 0.12, 0.12);
			a.key(b, "scale", t0, 0.15, 0.15, 0.15);
			a.key(b, "scale", t0 + 0.06, 1.05, 1.15, 1.05);
			a.scale(b, L, 1);
		}
	}

	/** They stand (looped without a seam): each spear quivering a little. */
	static void spearsStand(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("stand", T, "true");
		for (int k = 0; k < SPEARS; k++) {
			double ph = k * 0.77;
			a.sample("spear_" + k, "rotation", 0, T, 8, t -> new double[] {1.2 * wave(t, T, 1, ph), 0, 1.2 * wave(t, T, 1, ph + 2)});
		}
	}

	// --- Sword Rain: the ring of swords and the swords that fall ---

	static final int RAIN_SWORDS = 12;
	static final double RAIN_R = 32;

	/**
	 * Sword Rain's ring of swords over its mark, its middle at the origin (drawn at half the render scale, 2): twelve jian points down
	 * ("sword_0".., each its own bone: they come one by one), wheeling ("ring", turning on "ring_spin"); the formation over them ("array").
	 */
	static void swordRing(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone array = m.bone("array", "root", 0, 4, 0);
		SpellArt.sheet(array, 0, 4, 0, RAIN_R * 2.6, RAIN_R * 2.6, 0, 0, 0, swordArray(4501)).density(0.9);
		m.bone("ring", "root", 0, 0, 0);
		m.bone("ring_spin", "ring", 0, 0, 0);
		SpellArt.Cube[] first = null;
		for (int k = 0; k < RAIN_SWORDS; k++) {
			double th = 360.0 * k / RAIN_SWORDS;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * RAIN_R, 0, Math.cos(Math.toRadians(th)) * RAIN_R);
			SpellArt.Bone sword = m.bone("sword_" + k, "ring_spin", at[0], at[1], at[2]);
			// Point down (its blade along +z, turned down about its middle), its flat to the ring's middle.
			SpellArt.Cube[] made = domainSword(sword, at, 90, th, first);
			if (first == null) first = made;
		}
		ringGather(m);
		ringTurn(m);
	}

	/** It gathers (its enter, while the swords form: three quarters of a second): the formation spreading, the swords coming one by one round the ring. */
	static void ringGather(SpellArt.Model m) {
		double L = 0.75;
		SpellArt.Anim a = m.anim("gather", L, null);
		a.sample("array", "scale", 0, 0.3, 4, t -> new double[] {0.1 + 0.9 * SpellArt.easeOut(t / 0.3), 1, 0.1 + 0.9 * SpellArt.easeOut(t / 0.3)});
		for (int k = 0; k < RAIN_SWORDS; k++) {
			double t0 = 0.05 + 0.045 * k;
			String b = "sword_" + k;
			a.scale(b, 0, 0.02);
			a.scale(b, t0, 0.02);
			a.scale(b, t0 + 0.1, 1.15);
			a.scale(b, Math.min(L, t0 + 0.16), 1);
			a.scale(b, L, 1);
		}
	}

	/**
	 * It turns (looped without a seam): the ring wheeling a sword's spacing round in the time the swords take to form (MetalSpells RAIN_FORM),
	 * so each sword hangs where its blade is loosed from as they plunge; the swords bobbing.
	 */
	static void ringTurn(SpellArt.Model m) {
		double T = 0.75;
		SpellArt.Anim a = m.anim("turn", T, "true");
		a.sample("ring_spin", "rotation", 0, T, 4, t -> new double[] {0, 360.0 / RAIN_SWORDS * t / T, 0});
		a.sample("ring", "position", 0, T, 8, t -> new double[] {0, 0.8 * wave(t, T, 1, 0), 0});
	}

	/** One of Sword Rain's swords as it plunges (drawn at half the render scale, 2), its point toward -z (the way it flies), its trail of sword-light behind it. */
	static void rainSword(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone sword = m.bone("sword", "root", 0, 0, 0);
		domainSword(sword, SpellArt.v3(0, 0, 0), 0, 180, null);
		SpellArt.Bone glow = m.bone("glow", "root", 0, 0, 0);
		SpellArt.sheet(glow, 0, 0, -6, 10, 30, 90, 0, 0, haze(Metal.GLEAM, Metal.SILVER, 0.2)).density(1.5);
		SpellArt.Anim fall = m.anim("fall", 0.6, "true");
		fall.sample("sword", "rotation", 0, 0.6, 6, t -> new double[] {0, 0, 360 * t / 0.6});
		fall.sample("glow", "scale", 0, 0.6, 6, t -> same(1 + 0.1 * wave(t, 0.6, 2, 0)));
	}

	// --- Earth Dragon Surge: the dragon in the earth ---

	static final double[] DRAGON_COIL_Z = {14, 29, 43, 56}, DRAGON_COIL_R = {6.5, 6, 5.2, 4.2}, DRAGON_COIL_W = {7, 6.2, 5.2, 4.2};

	/**
	 * Earth Dragon Surge's dragon swimming through the land at the front of its ridge (the origin, on the ground; it faces -z, the way the ridge
	 * runs): its head of rock breaking out of the ground ("head", its jaw "jaw"), molten gold in its mouth and eyes, horns of amber swept back, a
	 * mane of shards; behind it the coils of its back arching up out of the ground and down again ("coil_0".."coil_3"), shards along their crests.
	 */
	static void earthDragon(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter stone = golemStone(3, 0.14, 4601), face = golemStone(2.4, 0.16, 4603);
		SpellArt.Bone head = m.bone("head", "root", 0, 2, 2, -24, 0, 0);
		head.box(0, 5, -1, 9, 8, 10, face);
		head.box(0, 5.4, -1, 7, 9.4, 9, face);
		head.box(0, 3.6, -9, 7.4, 5.6, 8, face);
		head.box(0, 4, -14, 6.2, 4.4, 3, face);
		head.box(0, 6.8, -8.4, 4, 1.2, 8, face);
		head.box(0, 4.6, 6, 8, 7, 6, stone);
		for (int s = -1; s <= 1; s += 2) {
			SpellArt.limb(head, SpellArt.v3(s * 1.8, 8.2, -6), SpellArt.v3(s * 4.4, 9.4, -1.6), 2.2, 1.8, face);
			head.box(s * 4.55, 6.6, -3.6, 0.7, 2.2, 3, Earth.moltenEye(face)).density(4);
			// Horns of amber, swept back over the neck.
			double[] h0 = SpellArt.v3(s * 3, 9.6, 1), h1 = SpellArt.add(h0, SpellArt.v3(s * 1.4, 3.2, 4)), h2 = SpellArt.add(h1, SpellArt.v3(s * 0.8, 1.4, 5));
			SpellArt.limb(head, h0, h1, 2, 2, Earth.amber(false, 4605)).density(3);
			SpellArt.limb(head, h1, h2, 1.3, 1.3, Earth.amber(true, 4606)).density(3);
			for (int k = 0; k < 3; k++) {
				SpellArt.blade(head, SpellArt.v3(s * 4.2, 5 - k * 1.4, 1 + k * 1.4), 8 - k * 1.2, 3.4, -80 - k * 12, s * (30 + k * 10), s * (26 + k * 12), Earth.shard(4610 + k + (s > 0 ? 0 : 3))).density(2.5);
			}
		}
		SpellArt.Bone jaw = m.bone("jaw", "head", 0, 2.6, -3, 14, 0, 0);
		jaw.box(0, 1.6, -8.4, 6.2, 2.2, 10, face);
		jaw.box(0, 2.85, -8.4, 4, 0.3, 8.6, t -> SpellArt.glow(SpellArt.mix(Earth.EMBER, Earth.MOLTEN, SpellArt.hash(t.x, t.y, 3) * 0.5), 0.9));
		head.box(0, 3.05, -8.4, 4, 0.3, 8.6, t -> SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, SpellArt.hash(t.x, t.y, 4) * 0.4), 0.9));
		for (int k = -1; k <= 1; k += 2) SpellArt.blade(jaw, SpellArt.v3(k * 2.2, 2.4, -12.6), 1.6, 1, -10, 0, 0, Earth.amber(true, 4620)).density(4);
		// Its coils: arcs of rock rising out of the land and down into it again, behind its head.
		for (int c = 0; c < DRAGON_COIL_Z.length; c++) {
			double z0 = DRAGON_COIL_Z[c], r = DRAGON_COIL_R[c], w = DRAGON_COIL_W[c];
			SpellArt.Bone coil = m.bone("coil_" + c, "root", 0, 0, z0);
			double[] prev = null;
			for (int i = 0; i <= 6; i++) {
				double a = Math.PI * i / 6;
				double[] p = SpellArt.v3(0, Math.sin(a) * r - 1.5, z0 - Math.cos(a) * r * 1.2);
				if (prev != null) SpellArt.limb(coil, prev, SpellArt.add(p, SpellArt.times(unit(SpellArt.sub(p, prev)), w * 0.3)), w, w, stone);
				if (i > 0 && i < 6) SpellArt.blade(coil, SpellArt.add(p, SpellArt.v3(0, w * 0.4, 0)), 5 - c * 0.6, 3, -40 + i * 13, 0, 0, Earth.shard(4640 + i)).density(2.5);
				prev = p;
			}
			SpellArt.sheet(coil, 0, 0.2, z0 - r * 1.2, w * 2.2, w * 2.2, 0, 0, 0, crackedEarth(4650 + c)).density(1.5);
			SpellArt.sheet(coil, 0, 0.2, z0 + r * 1.2, w * 2.2, w * 2.2, 0, 0, 0, crackedEarth(4655 + c)).density(1.5);
		}
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0, 0);
		SpellArt.sheet(ground, 0, 0.2, 0, 22, 22, 0, 0, 0, crackedEarth(4660)).density(1.5);
		dragonBreach(m);
		dragonSurge(m);
		dragonDive(m);
	}

	/**
	 * It breaks out of the land (its enter): its head heaving up out of the ground, then each coil arching up out of it in turn, as the head
	 * has swum on as far as the coil is behind it (a quarter block a pixel at a block and a half a tick, EarthSpells SURGE_PACE), so they come
	 * up where it broke out and never behind its caster.
	 */
	static void dragonBreach(SpellArt.Model m) {
		double L = 0.62, RISE = 0.15;
		SpellArt.Anim a = m.anim("breach", L, null);
		a.sample("head", "position", 0, 0.35, 4, t -> new double[] {0, -14 * (1 - SpellArt.easeOut(t / 0.35)), 0});
		for (int c = 0; c < DRAGON_COIL_Z.length; c++) {
			double t0 = DRAGON_COIL_Z[c] / 120;
			a.key("coil_" + c, "position", 0, 0, -12, 0);
			a.sample("coil_" + c, "position", t0, t0 + RISE, 4, t -> new double[] {0, -12 * (1 - SpellArt.easeOut((t - t0) / RISE)), 0});
		}
	}

	/** It swims on through the land (looped without a seam): its head tossing, its jaw snapping, a wave running back along its coils. */
	static void dragonSurge(SpellArt.Model m) {
		double T = 0.6;
		SpellArt.Anim a = m.anim("surge", T, "true");
		a.sample("head", "rotation", 0, T, 8, t -> new double[] {6 * wave(t, T, 1, 0), 3 * wave(t, T, 1, 1), 0});
		a.sample("head", "position", 0, T, 8, t -> new double[] {0, 1.2 * wave(t, T, 1, 0.4), 0});
		a.sample("jaw", "rotation", 0, T, 8, t -> new double[] {8 + 8 * wave(t, T, 1, 0.8), 0, 0});
		for (int c = 0; c < DRAGON_COIL_Z.length; c++) {
			double ph = -1.1 * (c + 1);
			a.sample("coil_" + c, "position", 0, T, 8, t -> new double[] {0, 1.8 * wave(t, T, 1, ph), 0});
			a.sample("coil_" + c, "rotation", 0, T, 8, t -> new double[] {0, 0, 4 * wave(t, T, 1, ph + 1)});
		}
	}

	/** It dives back into the land at the ridge's end (played once, held): its head plunging down, its coils sinking after it. */
	static void dragonDive(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("dive", L, "hold_on_last_frame");
		a.sample("head", "rotation", 0, L, 5, t -> new double[] {40 * SpellArt.ease(t / L), 0, 0});
		a.sample("head", "position", 0, L, 5, t -> new double[] {0, -16 * SpellArt.easeIn(t / L), 0});
		for (int c = 0; c < DRAGON_COIL_Z.length; c++) {
			double t0 = 0.08 * c;
			a.sample("coil_" + c, "position", 0, L, 5, t -> new double[] {0, -13 * SpellArt.easeIn(SpellArt.clamp01((t - t0) / (L - t0))), 0});
		}
	}

	// --- The spells of both paths below Dao Integration (heaven's gold light, a demon's crimson qi) ---

	/**
	 * Heaven-Demon Palm's palm (drawn at half the render scale, 2), its heart at the origin, thrust toward -z: a hand of heaven's gold light
	 * ("palm", its fingers up), heaven's seal glowing on its palm, a demon's crimson qi burning off its back and wrist ("fire").
	 */
	static void hdPalm(SpellArt.Model m) {
		m.tpp = 2.5;
		m.scale = 2;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone palm = m.bone("palm", "root", 0, 0, 0);
		SpellArt.Painter gold = HeavenDemon.heavenly(4701);
		palm.box(0, 0, 0, 11, 11, 2.6, gold);
		palm.box(0, -6.6, 0.4, 8, 3.6, 2.4, gold);
		double[] fingerX = {-3.9, -1.3, 1.3, 3.9}, fingerLen = {7, 8.4, 8, 6.4};
		for (int k = 0; k < 4; k++) {
			double[] a = SpellArt.v3(fingerX[k], 5.2, 0), b = SpellArt.add(a, SpellArt.v3((k - 1.5) * 0.4, fingerLen[k] * 0.55, 0.3)), c = SpellArt.add(b, SpellArt.v3((k - 1.5) * 0.3, fingerLen[k] * 0.45, 0.6));
			SpellArt.limb(palm, a, b, 2.3, 2.2, gold);
			SpellArt.limb(palm, b, c, 2, 1.9, gold);
		}
		SpellArt.limb(palm, SpellArt.v3(5.4, -1.6, 0.2), SpellArt.v3(8.6, 2.6, -0.6), 2.4, 2.2, gold);
		SpellArt.limb(palm, SpellArt.v3(8.6, 2.6, -0.6), SpellArt.v3(9.8, 6.2, -1.2), 2.1, 2, gold);
		// Heaven's seal on its palm, facing out.
		SpellArt.sheet(palm, 0, 0, -1.4, 10, 10, 90, 0, 0, HeavenDemon.palmSeal(4703)).density(4);
		SpellArt.Bone fire = m.bone("fire", "root", 0, 0, 0);
		for (int k = 0; k < 8; k++) {
			double th = Math.toRadians(45 * k);
			double[] base = SpellArt.v3(Math.sin(th) * 5.4, Math.cos(th) * 5.4 - 1, 1.4), way = unit(SpellArt.v3(Math.sin(th) * 0.8, Math.cos(th) * 0.8, 1.4));
			crossedTongue(m, fire, "demon_fire_" + k % 2, base, way, 9 + 3 * (k % 2), 4, HeavenDemon.demonFlame(2, 4705 + k % 2), 2.5, k % 2 == 1);
		}
		SpellArt.Anim thrust = m.anim("thrust", 0.2, null);
		thrust.sample("root", "scale", 0, 0.2, 4, t -> same(0.3 + 0.85 * SpellArt.easeOut(t / 0.14) - 0.15 * SpellArt.smooth(0.14, 0.2, t)));
		thrust.sample("root", "position", 0, 0.2, 4, t -> new double[] {0, 0, 6 * (1 - SpellArt.easeOut(t / 0.2))});
		SpellArt.Anim glow = m.anim("glow", 0.8, "true");
		glow.sample("fire", "scale", 0, 0.8, 8, t -> same(1 + 0.12 * wave(t, 0.8, 2, 0)));
		SpellArt.Anim strike = m.anim("strike", 0.3, "hold_on_last_frame");
		strike.sample("root", "scale", 0, 0.3, 4, t -> same(1 + 0.6 * SpellArt.easeOut(t / 0.3)));
		strike.sample("fire", "scale", 0, 0.3, 4, t -> same(1 + 0.8 * SpellArt.easeOut(t / 0.3)));
	}

	/**
	 * Heaven-Demon Mantle round its caster (their feet at the origin; drawn at their size, two blocks tall at 1): a ring of heaven's gold light
	 * about their chest hung with talismans ("heaven_ring"), a ring of a demon's crimson scales turning the other way below it ("demon_ring"),
	 * tongues of the two rising round them ("flames").
	 */
	static void hdMantle(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone heaven = m.bone("heaven_ring", "root", 0, 20, 0);
		Vessel.ring(heaven, 0, 20, 0, 20, 1.4, 24, HeavenDemon.heavenly(4711));
		for (int k = 0; k < 8; k++) {
			double th = 45 * k;
			double[] at = SpellArt.v3(Math.sin(Math.toRadians(th)) * 20, 19.4, Math.cos(Math.toRadians(th)) * 20);
			SpellArt.shared(m, heaven, "tag", false, at, 6, 2.6, 180, th + 90, 0, talisman(4713), 4, false);
		}
		SpellArt.Bone demon = m.bone("demon_ring", "root", 0, 10, 0);
		for (int k = 0; k < 14; k++) {
			double th = 360.0 * k / 14;
			double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 17, 8, Math.cos(Math.toRadians(th)) * 17);
			SpellArt.shared(m, demon, "scale", false, base, 6, 5, -18, th + 90, 0, demonScale(4715), 4, k % 2 == 1);
		}
		SpellArt.Bone flames = m.bone("flames", "root", 0, 0, 0);
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k + 30);
			double[] base = SpellArt.v3(Math.sin(th) * 13, 2, Math.cos(th) * 13);
			crossedTongue(m, flames, "mantle_fire_" + k % 2, base, SpellArt.v3(0, 1, 0), 16, 5, k % 2 == 0 ? HeavenDemon.heavenFlame(2, 4717) : HeavenDemon.demonFlame(2, 4718), 2.5, false);
		}
		SpellArt.Anim wrap = m.anim("wrap", 0.4, null);
		for (String b : new String[] {"heaven_ring", "demon_ring", "flames"}) wrap.sample(b, "scale", 0, 0.4, 4, t -> same(Math.max(0.05, SpellArt.easeOut(t / 0.4))));
		double T = 3;
		SpellArt.Anim turn = m.anim("turn", T, "true");
		turn.sample("heaven_ring", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		turn.sample("demon_ring", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		turn.sample("flames", "scale", 0, T, 12, t -> new double[] {1, 1 + 0.15 * wave(t, T, 4, 0), 1});
	}

	/** A demon's scale (on a blade): a pointed plate of dark crimson horn, a burning scarlet rim, a ridge down its middle; glowing at its rim. */
	static SpellArt.Painter demonScale(int seed) {
		return t -> {
			double v = t.v(), u = Math.abs(t.u() - 0.5) * 2, half = 1 - Math.pow(v, 2.2) * 0.9;
			if (u > half) return SpellArt.CLEAR;
			if (half - u < 0.14) return SpellArt.glow(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.EMBER, v), 0.92);
			if (u < 0.08) return SpellArt.col(HeavenDemon.HORN_LIGHT);
			return SpellArt.col(SpellArt.mix(HeavenDemon.BLOOD, HeavenDemon.CRIMSON, 0.3 + 0.5 * v + 0.2 * SpellArt.noise(u * 4 + seed, v * 4, seed)));
		};
	}

	/**
	 * Devouring Seal over one being it seals (its feet at the origin; drawn at that being's size, two blocks tall at 1): heaven's seal turning over
	 * its head ("seal"); a demon's maw round its middle, its upper and lower jaws of horn fangs ("jaw_up", "jaw_down"), biting.
	 */
	static void devouringSeal(SpellArt.Model m) {
		m.tpp = 3;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone seal = m.bone("seal", "root", 0, 38, 0);
		SpellArt.sheet(seal, 0, 38, 0, 30, 30, 0, 0, 0, HeavenDemon.palmSeal(4721)).density(3);
		SpellArt.Painter horn = HeavenDemon.horn(4723), gum = HeavenDemon.demonic(4725);
		for (int j = 0; j < 2; j++) {
			boolean up = j == 0;
			String name = up ? "jaw_up" : "jaw_down";
			double y = up ? 22 : 10;
			SpellArt.Bone jaw = m.bone(name, "root", 0, y, 0);
			// The jaw's gum: a ring of crimson sinew round the being; its fangs pointing in and toward the other jaw.
			Vessel.ring(jaw, 0, y, 0, 14, 2.4, 20, gum);
			for (int k = 0; k < 16; k++) {
				double th = 360.0 * k / 16 + (up ? 0 : 11.25);
				double[] base = SpellArt.v3(Math.sin(Math.toRadians(th)) * 13.4, y, Math.cos(Math.toRadians(th)) * 13.4);
				double[] tip = SpellArt.add(SpellArt.times(base, 0.82), SpellArt.v3(0, up ? -4.6 : 4.6, 0));
				tip[1] = y + (up ? -4.6 : 4.6);
				SpellArt.limb(jaw, base, tip, 1.4, 1.4, horn).density(4);
			}
		}
		double T = 2;
		SpellArt.Anim hold = m.anim("hold", T, "true");
		hold.sample("seal", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
		hold.sample("jaw_up", "position", 0, T, 8, t -> new double[] {0, 0.6 * wave(t, T, 1, 0), 0});
		hold.sample("jaw_down", "position", 0, T, 8, t -> new double[] {0, -0.6 * wave(t, T, 1, 0), 0});
		SpellArt.Anim bite = m.anim("bite", 0.5, null);
		double[] bt = {0, 0.14, 0.5};
		double[] close = {0, 1, 0};
		for (int i = 0; i < bt.length; i++) {
			bite.pos("jaw_up", bt[i], 0, -4.4 * close[i], 0);
			bite.pos("jaw_down", bt[i], 0, 4.4 * close[i], 0);
		}
	}

	/**
	 * A great talon of a demon (on a limb along it; {@code part} 0 at its root to 5 at its point): at its root still a demon's qi, burning and
	 * seen through, as it comes out of the sky; below black horn going to dark blood, ridged across, its leading edge (its back: the inside of
	 * its curve) burning scarlet, a seam of heaven's gold down its front; its point cut to a point on every side and burning ember.
	 */
	static SpellArt.Painter talon(int seed, int part) {
		return t -> {
			double n = SpellArt.noise(t.fx * 2 + seed, t.ly * 0.35, seed), up = t.up();
			if (part == 5) {
				double across = t.f == SpellArt.Face.FRONT || t.f == SpellArt.Face.BACK ? t.across() : t.back();
				if (t.f == SpellArt.Face.UP || Math.abs(across - 0.5) > 0.5 * (1 - up)) return SpellArt.CLEAR;
				return SpellArt.glow(SpellArt.mix(HeavenDemon.SCARLET, HeavenDemon.EMBER_PALE, up), 0.97);
			}
			// Its root (its cube's foot is up, in the sky): fading out into it, no end to be seen.
			if (part == 0) return t.f == SpellArt.Face.DOWN || up < 0.12 ? SpellArt.CLEAR
					: SpellArt.glow(SpellArt.mix(HeavenDemon.CRIMSON, HeavenDemon.SCARLET, 0.3 + 0.5 * n), 0.1 + 0.85 * Math.pow((up - 0.12) / 0.88, 1.4));
			if (t.f == SpellArt.Face.BACK) return SpellArt.glow(SpellArt.mix(HeavenDemon.CRIMSON, HeavenDemon.EMBER, 0.25 + 0.5 * n + 0.25 * up), 0.95);
			if (t.f == SpellArt.Face.FRONT && Math.abs(t.across() - 0.5) < 0.2) return SpellArt.glow(SpellArt.mix(HeavenDemon.GOLD, HeavenDemon.GOLD_BRIGHT, n), 0.95);
			double ridge = t.ly / 2.4 - Math.floor(t.ly / 2.4);
			int c = SpellArt.mix(HeavenDemon.HORN, HeavenDemon.BLOOD, 0.15 + 0.5 * n);
			return SpellArt.col(ridge < 0.12 ? SpellArt.shade(c, -0.4) : c);
		};
	}

	static final double CLAW_UP = 58;
	/** Each talon's length, its root to its point: the middle two the longest. */
	static final double[] TALON_LENGTH = {54, 57, 56, 53};

	/**
	 * Heaven-Demon Claw's rake out of the sky, the point it rakes at the origin (it faces -z, the way its caster looks): four great talons of a
	 * demon ("talon_0".."talon_3"), hung from on high out of a demon's qi, black horn curving forward to burning points, their leading edges
	 * scarlet and a seam of heaven's gold down their backs; swept from over their caster's side down through the place and on, then withdrawn
	 * into the sky; the gouges they leave burning in the ground ("gouges").
	 */
	static void hdClaw(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		for (int k = 0; k < 4; k++) {
			double x = (k - 1.5) * 9, spread = (k - 1.5) * 1.6, len = TALON_LENGTH[k];
			SpellArt.Bone talon = m.bone("talon_" + k, "root", x, CLAW_UP, 0);
			// Hung from on high, curving forward (the way it rakes) to its point, narrowing: six lengths.
			double[] prev = null;
			for (int i = 0; i <= 6; i++) {
				double s = i / 6.0;
				double[] p = SpellArt.v3(x + spread * s, CLAW_UP - len * s, -20 * Math.pow(s, 2.2) * len / 56);
				if (prev != null) {
					double thick = 6.4 * (1 - 0.8 * (i - 0.5) / 6);
					SpellArt.limb(talon, prev, p, thick * 0.72, thick, talon(4731 + k * 7 + i, i - 1)).density(i == 6 ? 4 : 2);
				}
				prev = p;
			}
		}
		SpellArt.Bone gouges = m.bone("gouges", "root", 0, 0, 0);
		SpellArt.sheet(gouges, 0, 0.3, 4, 44, 52, 0, 0, 0, HeavenDemon.gouges()).density(1.5);
		clawRake(m);
		clawLinger(m);
	}

	static final double RAKE = 0.55, SWEEP = 0.28, RAKE_FROM = 80, RAKE_TO = -50, RAKE_ON = -64;

	/**
	 * It rakes (its enter): each talon in turn, a moment after the one before, swept from over its caster's side (tipped back toward +z) down
	 * through the place, its point gouging the ground beneath its root, and on up the far side; then on a little more as it is drawn back
	 * into the sky (shrinking to its root). The gouges open behind them.
	 */
	static void clawRake(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("rake", RAKE, null);
		for (int k = 0; k < 4; k++) {
			double t0 = 0.03 * k, t1 = t0 + SWEEP;
			String b = "talon_" + k;
			a.sample(b, "rotation", 0, t1, 8, t -> new double[] {RAKE_FROM + (RAKE_TO - RAKE_FROM) * SpellArt.ease(SpellArt.clamp01((t - t0) / SWEEP)), 0, 0});
			a.sample(b, "rotation", t1, RAKE, 3, t -> new double[] {RAKE_TO + (RAKE_ON - RAKE_TO) * SpellArt.easeOut((t - t1) / (RAKE - t1)), 0, 0});
			a.scale(b, 0, 1);
			a.sample(b, "scale", t1, RAKE, 3, t -> same(Math.max(0.02, 1 - SpellArt.easeIn((t - t1) / (RAKE - t1)))));
		}
		a.sample("gouges", "scale", 0, 0.4, 4, t -> new double[] {1, 1, Math.max(0.02, SpellArt.clamp01((t - 0.12) / 0.2))});
	}

	/** Its gouges burn on (looped without a seam): the talons gone into the sky. */
	static void clawLinger(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("linger", 1, "true");
		for (int k = 0; k < 4; k++) {
			for (double t : new double[] {0, 1}) {
				a.rot("talon_" + k, t, RAKE_ON, 0, 0);
				a.scale("talon_" + k, t, 0.02);
			}
		}
	}

	/**
	 * Heaven and the Demon made one, round its caster (their feet at the origin), sixteen blocks round: a vortex of the two on the ground, an arm of
	 * heaven's gold and an arm of a demon's crimson wound in to its heart ("vortex", turning), and over its heart a pillar of the two twined
	 * together ("pillar").
	 */
	static void hdAsOne(SpellArt.Model m) {
		m.tpp = 1;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone vortex = m.bone("vortex", "root", 0, 0, 0);
		SpellArt.sheet(vortex, 0, 0.4, 0, 128, 128, 0, 0, 0, taiji(4751)).density(0.7);
		SpellArt.Bone pillar = m.bone("pillar", "root", 0, 0, 0);
		for (int arm = 0; arm < 2; arm++) {
			double[] prev = null;
			for (int i = 0; i <= 14; i++) {
				double y = i * 3.2, a = arm * Math.PI + y * 0.18, r = 4 - y * 0.04;
				double[] p = SpellArt.v3(Math.sin(a) * r, y, Math.cos(a) * r);
				if (prev != null) SpellArt.limb(pillar, prev, p, 2.4 - i * 0.08, 2.4 - i * 0.08, arm == 0 ? HeavenDemon.heavenly(4753) : HeavenDemon.demonic(4755));
				prev = p;
			}
		}
		SpellArt.Anim converge = m.anim("converge", 0.6, null);
		converge.sample("vortex", "scale", 0, 0.6, 5, t -> new double[] {1.35 - 0.35 * SpellArt.easeOut(t / 0.6), 1, 1.35 - 0.35 * SpellArt.easeOut(t / 0.6)});
		converge.sample("pillar", "scale", 0, 0.6, 5, t -> new double[] {1, Math.max(0.02, SpellArt.easeOut(t / 0.6)), 1});
		double T = 1.2;
		SpellArt.Anim spin = m.anim("spin", T, "true");
		spin.sample("vortex", "rotation", 0, T, 8, t -> new double[] {0, -360 * t / T, 0});
		spin.sample("pillar", "rotation", 0, T, 8, t -> new double[] {0, 360 * t / T, 0});
	}

	/**
	 * The vortex of the two (on a sheet lying level): an arm of heaven's gold light and an arm of a demon's crimson qi winding in to the heart,
	 * brightest there, thinning as they go out; seen through, glowing.
	 */
	static SpellArt.Painter taiji(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.99) return SpellArt.CLEAR;
			// The arms: the angle each lies at r out, wound in.
			double wind = a - 5.2 * Math.sqrt(r), ph = ((wind / (2 * Math.PI)) % 1 + 1) % 1;
			double d = Math.min(Math.abs(ph - 0.25), Math.abs(ph - 0.75)), width = 0.09 + 0.06 * (1 - r);
			if (d > width) return Math.abs(r - 0.97) < 0.01 ? SpellArt.glow(HeavenDemon.GOLD_PALE, 0.7) : SpellArt.CLEAR;
			boolean heaven = Math.abs(ph - 0.25) < Math.abs(ph - 0.75);
			double core = 1 - d / width, alpha = (0.35 + 0.6 * core) * (1 - 0.6 * r);
			return SpellArt.glow(SpellArt.ramp(core, heaven ? HeavenDemon.HEAVEN : HeavenDemon.DEMON), alpha);
		};
	}

	// --- Myriad Vine Binding: the binding vines ---

	static final int VINES = 4, VINE_SEGS = 10;
	static final double[] VINE_HIGH = {30, 27, 31, 28};

	/**
	 * A binding vine (on a limb): dark green living wood, ridged along its length, mossed lighter on top and darker beneath, two veins of jade light
	 * winding slowly round it in its grooves.
	 */
	static SpellArt.Painter bindingVine(int seed) {
		return t -> {
			double ang = Math.atan2(t.lx - t.sx() / 2, t.lz - t.sz() / 2) / (2 * Math.PI), vein = ang * 2 + t.ly * 0.06 + seed * 0.37;
			double d = Math.abs(vein - Math.rint(vein));
			if (d < 0.045) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, 0.3), 0.95);
			double ridge = Math.abs(Math.sin(ang * 2 * Math.PI * 5 + 1.5 * SpellArt.noise(t.ly * 0.4, ang * 6, seed)));
			double n = SpellArt.fbm(t.lx * 1.5 + t.lz * 1.5 + seed, t.ly * 0.35, seed, 2);
			// Living wood, brown going olive where it is mossed (green alone was lost on green beings, and on the grass).
			int c = SpellArt.ramp(SpellArt.clamp01(0.3 + 0.35 * n + 0.22 * ridge + 0.18 * Math.max(0, t.ny) - 0.2 * Math.max(0, -t.ny)), 0x2A1C10, 0x4E3820, 0x6A6230, 0x94A24C);
			if (d < 0.09) c = SpellArt.shade(c, -0.3);
			return SpellArt.col(c);
		};
	}

	/** A thorn (on a blade, its length along it): a hooked spike of dark wood narrowing to its point, the point glowing jade. */
	static SpellArt.Painter thorn() {
		return t -> {
			double v = t.v(), u = t.u() - 0.5 + 0.12 * v * v, half = 0.5 * (1 - v);
			if (Math.abs(u) > half) return SpellArt.CLEAR;
			if (v > 0.62) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.VEIN_CORE, (v - 0.62) * 2.5), 0.95);
			return SpellArt.col(SpellArt.mix(Wood.WOOD_DARK, Wood.WOOD_LIGHT, 0.2 + 0.6 * v));
		};
	}

	/** The ground the vines burst from (on a sheet): earth cracked open round its heart, the cracks lit jade, fading out; seen through at its rim. */
	static SpellArt.Painter crackedEarth(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.98) return SpellArt.CLEAR;
			double crack = Math.abs(Math.sin(a * 4.5 + 2.2 * SpellArt.noise(r * 3 + seed, a, seed)));
			double ring = Math.abs(r - 0.42 - 0.08 * SpellArt.noise(a * 2, seed, seed + 1));
			if ((crack < 0.07 && r > 0.12) || ring < 0.025) return SpellArt.glow(SpellArt.mix(Wood.VEIN, Wood.JADE, r), 0.9 * (1 - r * 0.7));
			double soil = SpellArt.fbm(x * 4 + seed, z * 4, seed + 2, 3);
			return SpellArt.col(SpellArt.mix(0x2A1E14, 0x5A4430, soil), 0.9 * (1 - SpellArt.smooth(0.6, 0.98, r)));
		};
	}

	/**
	 * Where vine {@code k} is, {@code s} of the way along it (0 its foot, 1 its tip), in pixels from the held being's feet: out of the ground a
	 * little way off, closing in on them, then coiling up round them, the vines turning alternately one way and the other so they cross.
	 */
	static double[] vineAt(int k, double s) {
		double turn = (k % 2 == 0 ? 1 : -1) * 0.95, th = 2 * Math.PI * (k / (double) VINES + turn * s);
		double r = 7.2 + 4.6 * Math.pow(1 - s, 5) - 1.6 * s * s, y = VINE_HIGH[k] * Math.pow(s, 1.08) + 0.6;
		// The tip curls in over the shoulders.
		if (s > 0.86) r -= (s - 0.86) / 0.14 * 1.4;
		return SpellArt.v3(Math.sin(th) * r, y, Math.cos(th) * r);
	}

	/**
	 * Myriad Vine Binding's vines round one being held (its feet at the origin), two blocks tall at the size its kind draws it (its spell sizes it
	 * to the being): {@link #VINES} thorned vines of living wood burst from the cracked ground ("ground", root knuckles round it) and coil up round
	 * it, crossing, their tips curled over its shoulders ("vine_0_0".."vine_4_6", each vine a chain of {@link #VINE_SEGS} bones), leaves on
	 * them; veins of jade light in their grooves.
	 */
	static void bindingVines(SpellArt.Model m) {
		m.tpp = 4;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone ground = m.bone("ground", "root", 0, 0, 0);
		SpellArt.sheet(ground, 0, 0.15, 0, 30, 30, 0, 0, 0, crackedEarth(3401)).density(2);
		SpellArt.Painter wood = Wood.woodLimb(3403);
		for (int k = 0; k < 8; k++) {
			double th = Math.toRadians(45 * k + 20), r0 = 13 + 2 * SpellArt.hash(k, 1, 3401), r1 = r0 - 4.5;
			double[] a = SpellArt.v3(Math.sin(th) * r0, -0.6, Math.cos(th) * r0), top = SpellArt.v3(Math.sin(th) * (r0 - 2.2), 1.6 + SpellArt.hash(k, 2, 3401), Math.cos(th) * (r0 - 2.2)),
					b = SpellArt.v3(Math.sin(th) * r1, -0.6, Math.cos(th) * r1);
			SpellArt.limb(ground, a, top, 1.6, 1.4, wood);
			SpellArt.limb(ground, top, b, 1.4, 1.2, wood);
		}
		SpellArt.Painter thorn = thorn(), leaf = Wood.leaf(Wood.LEAF_DARK, Wood.LEAF_LIGHT, 1.4, 3405);
		for (int k = 0; k < VINES; k++) {
			String up = "root";
			for (int i = 0; i < VINE_SEGS; i++) {
				double s0 = i / (double) VINE_SEGS, s1 = (i + 1) / (double) VINE_SEGS;
				double[] a = vineAt(k, s0), b = vineAt(k, s1);
				SpellArt.Bone seg = m.bone("vine_" + k + "_" + i, up, a[0], a[1], a[2]);
				double thick = 2.1 - 1.0 * s0;
				// Each length a little longer than its span, so the joints overlap as it bends.
				double[] along = unit(SpellArt.sub(b, a)), end = SpellArt.add(b, SpellArt.times(along, thick * 0.35));
				SpellArt.limb(seg, a, end, thick, thick, bindingVine(3410 + k));
				// A thorn standing out from it, away from the being it holds; and a leaf here and there.
				double[] at = SpellArt.add(a, SpellArt.times(SpellArt.sub(b, a), 0.5));
				double[] out = unit(SpellArt.add(SpellArt.v3(at[0], i % 2 == 0 ? 0 : 3, at[2]), SpellArt.times(along, -1.2)));
				crossedTongue(m, seg, "thorn", at, out, 2.2, 1.3, thorn, 6, i % 2 == 1);
				if (i == 3 || i == 7) {
					double heading = Math.toDegrees(Math.atan2(at[0], at[2])) + (k % 2 == 0 ? 70 : -70);
					SpellArt.shared(m, seg, "leaf_" + (i == 3 ? 0 : 1), false, at, i == 3 ? 6 : 5, 3.4, -60, heading, 0, leaf, 5, k % 2 == 1);
				}
				up = seg.name;
			}
		}
		vinesSeize(m);
		vinesSqueeze(m);
		vinesCrush(m);
	}

	/** It seizes (its enter): the ground cracking, then each vine bursting up out of it and coiling up its being, length after length. */
	static void vinesSeize(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("seize", L, null);
		a.sample("ground", "scale", 0, 0.2, 4, t -> new double[] {0.3 + 0.7 * SpellArt.easeOut(t / 0.2), 1, 0.3 + 0.7 * SpellArt.easeOut(t / 0.2)});
		for (int k = 0; k < VINES; k++) {
			double from = 0.05 + 0.03 * k;
			for (int i = 0; i < VINE_SEGS; i++) {
				double t0 = from + i * 0.04, t1 = t0 + 0.06;
				a.scale("vine_" + k + "_" + i, 0, 0.02);
				if (t0 > 0) a.scale("vine_" + k + "_" + i, t0, 0.02);
				a.scale("vine_" + k + "_" + i, Math.min(L, t1), 1.08);
				a.scale("vine_" + k + "_" + i, L, 1);
			}
		}
	}

	/**
	 * It holds (looped without a seam): the coils tightening and easing round its being (the whole drawn in a little, about its middle: a vine's
	 * lengths are a chain, each drawn in about its own foot would draw the next in again, so its top sank into its being), the vines' tips stirring.
	 */
	static void vinesSqueeze(SpellArt.Model m) {
		double T = 1.6;
		SpellArt.Anim a = m.anim("squeeze", T, "true");
		a.sample("root", "scale", 0, T, 8, t -> new double[] {1 - 0.03 * (1 + wave(t, T, 1, 0)), 1, 1 - 0.03 * (1 + wave(t, T, 1, 0))});
		for (int k = 0; k < VINES; k++) {
			int kk = k;
			for (int i = VINE_SEGS - 2; i < VINE_SEGS; i++) {
				int ii = i;
				a.sample("vine_" + k + "_" + i, "rotation", 0, T, 8, t -> new double[] {6 * wave(t, T, 1, kk * 1.3 + ii), 0, 5 * wave(t, T, 1, kk * 0.7 + ii + 1)});
			}
		}
	}

	/** It crushes (played once): every coil drawn hard in round its being, and let a little go. */
	static void vinesCrush(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("crush", 0.45, null);
		a.scale("root", 0, 1);
		a.key("root", "scale", 0.12, 0.84, 1.03, 0.84);
		a.scale("root", 0.45, 1);
	}

	// --- Summon Golem and the Earthen Titan: the earth golem ---

	static final int EARTH_DARK = 0x2C2118, EARTH = 0x5C4632, EARTH_LIGHT = 0x8A6E4E;

	/**
	 * Packed earth and fitted stone (a solid, painted where it lies in the model; Earth#rock in the golem's earth): stones fitted together, each
	 * its own umber, brown or grey, chiselled (lit toward its middle, shadowed to its edges), dark cracks between them, more of them running with
	 * molten amber the higher {@code molten} (0 to 1); darker beneath. {@code size} is a stone's breadth, in pixels.
	 */
	static SpellArt.Painter golemStone(double size, double molten, int seed) {
		return t -> {
			double x = t.px / size, y = t.py / size, z = t.pz / size;
			int ix = (int) Math.floor(x), iy = (int) Math.floor(y), iz = (int) Math.floor(z);
			double d1 = 9, d2 = 9;
			int cell = 0;
			for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
				int cx = ix + dx, cy = iy + dy, cz = iz + dz;
				double px = cx + SpellArt.hash3(cx, cy, cz, seed), py = cy + SpellArt.hash3(cx, cy, cz, seed + 1), pz = cz + SpellArt.hash3(cx, cy, cz, seed + 2);
				double d = Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
				if (d < d1) {
					d2 = d1;
					d1 = d;
					cell = cx * 73856093 ^ cy * 19349663 ^ cz * 83492791;
				} else if (d < d2) d2 = d;
			}
			double gap = d2 - d1;
			if (gap < 0.11) {
				if (SpellArt.noise3(t.px * 0.16, t.py * 0.16, t.pz * 0.16, seed + 3) > 0.74 - 0.32 * molten) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, 0.25 + gap * 4), 0.95);
				return SpellArt.col(Earth.CRACK);
			}
			double h = SpellArt.hash(cell, 1, seed);
			int c = h < 0.32 ? EARTH : h < 0.55 ? Earth.UMBER : h < 0.78 ? SpellArt.mix(Earth.STONE, Earth.STONE_LIGHT, 0.3) : h < 0.92 ? SpellArt.mix(EARTH, EARTH_LIGHT, 0.6) : Earth.STONE_DARK;
			double grit = SpellArt.noise3(t.px * 1.1, t.py * 1.1, t.pz * 1.1, seed + 4);
			c = Earth.warm(c, -0.24 + 0.36 * SpellArt.smooth(0.08, 0.4, gap) - 0.12 * d1 + (grit - 0.5) * 0.16 + 0.08 * Math.max(0, t.ny));
			if (t.ny < -0.5) c = Earth.warm(c, -0.25);
			return SpellArt.col(c);
		};
	}

	/** The golem's face (its head's front): two slits of molten light under its brow, a seam of it for a mouth; the stone round them. */
	static SpellArt.Painter golemFace(SpellArt.Painter stone) {
		return t -> {
			if (t.f != SpellArt.Face.FRONT) return stone.paint(t);
			double x = t.across() - 0.5, y = t.up() - 0.5;
			double eye = Math.abs(y - 0.12) - 0.07 * (1 - Math.pow((Math.abs(x) - 0.22) / 0.14, 2));
			if (Math.abs(Math.abs(x) - 0.22) < 0.14 && eye < 0) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN_CORE, Earth.MOLTEN, Math.abs(Math.abs(x) - 0.22) * 6));
			if (Math.abs(y + 0.26) < 0.035 && Math.abs(x) < 0.2) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.EMBER, Math.abs(x) * 4), 0.9);
			return stone.paint(t);
		};
	}

	/**
	 * Summon Golem's earth golem (the Earthen Titan is the same, three times as large: its renderer's), made of its element: a hulk of packed earth
	 * and fitted stone with no legs, floating, molten amber in its cracks, its feet's place at the origin and some two and a half blocks tall
	 * at its own size; its body ("body") a great chest and shoulders, slabs of rock and crystals of amber jutting from its back; its small head
	 * ("head") sunk between them, two slits of molten light for eyes; long heavy arms ("arm_l", "arm_r", each with its "_low") ending in fists
	 * like boulders; a tail of broken stones under it where legs would be ("tail_0".."tail_2"); stones wheeling round its waist ("orbit").
	 */
	static void earthGolem(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Painter stone = golemStone(3.4, 0.18, 3501), fine = golemStone(2.2, 0.24, 3503), amber = Earth.amber(true, 3505);
		SpellArt.Bone body = m.bone("body", "root", 0, 20, 0, 6, 0, 0);
		body.box(0, 20.5, 0.5, 13, 8, 10, stone);
		body.box(0, 20.5, 0.5, 11, 9, 12, stone);
		body.box(0, 30.5, -0.5, 20, 13, 13, stone);
		body.box(0, 30.5, -0.5, 17, 14.6, 11, stone);
		body.box(0, 31.5, -6.4, 15, 8, 2, fine);
		// Slabs of rock on its back and shoulders, and amber crystals jutting from among them.
		body.box(-5, 36.6, 5.4, 8, 6, 4, stone).rot(12, 0, -12);
		body.box(5.2, 36, 5.2, 8, 5, 4, stone).rot(10, 0, 10);
		body.box(0, 33, 6.8, 10, 7, 2.5, fine).rot(6, 0, 0);
		double[][] crystals = {{-8, 38.5, 3.5, -28, 0, 30}, {-6, 39.5, 5.5, -40, 0, 14}, {8.5, 38, 3, -26, 0, -32}, {6.5, 39.2, 5.6, -44, 0, -12}, {0, 37.5, 7.4, -58, 0, 0}};
		for (double[] c : crystals) body.box(c[0], c[1] + 2.4, c[2], 2.2, 5.4, 2.2, amber).rot(c[3], c[4], c[5], c[0], c[1], c[2]).density(4);
		SpellArt.Bone head = m.bone("head", "body", 0, 37, -3);
		head.box(0, 39.4, -4.6, 8, 7, 8, golemFace(fine));
		head.box(0, 42.4, -8.1, 9, 2, 2.4, fine);
		head.box(0, 36.6, -7.6, 6, 2.2, 2.6, fine);
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			SpellArt.Bone arm = m.bone(w, "body", s * 11, 35, 0);
			arm.box(s * 12.6, 34.2, 0, 8, 7, 9, stone);
			SpellArt.limb(arm, SpellArt.v3(s * 12.6, 33, 0), SpellArt.v3(s * 13.2, 24.6, 0.6), 6.4, 7, stone);
			arm.box(s * 14.6, 37.4, 1.4, 2, 4.6, 2, amber).rot(-12, 0, -s * 30).density(4);
			SpellArt.Bone low = m.bone(w + "_low", w, s * 13.2, 25.2, 0.6);
			SpellArt.limb(low, SpellArt.v3(s * 13.2, 25.6, 0.6), SpellArt.v3(s * 13.6, 13, 0), 8, 8.4, stone);
			low.box(s * 13.6, 9.4, -0.4, 10, 9, 10, fine);
			low.box(s * 13.6, 8.8, -5.2, 9, 4.2, 2.2, fine);
			low.box(s * 17.2, 18.8, 0, 1.8, 4, 1.8, amber).rot(0, 0, -s * 40).density(4);
		}
		String up = "body";
		double[][] tail = {{0, 16, 1, 9, 6, 8, 8, 15, 0}, {0, 10.2, 1.6, 6.6, 4.6, 6, -10, -20, 5}, {0, 5.8, 2, 4.6, 3.6, 4.2, 15, 30, 0}};
		for (int i = 0; i < tail.length; i++) {
			double[] p = tail[i];
			SpellArt.Bone b = m.bone("tail_" + i, up, p[0], p[1], p[2]);
			b.box(p[0], p[1] - p[4] / 2, p[2], p[3], p[4], p[5], i == 0 ? stone : fine).rot(p[6], p[7], p[8]);
			up = b.name;
		}
		SpellArt.Bone orbit = m.bone("orbit", "root", 0, 21, 0);
		for (int k = 0; k < 6; k++) {
			double th = Math.toRadians(60 * k + 15), r = 15 + 1.5 * SpellArt.hash(k, 3, 3501), c = 2.4 + 1.4 * SpellArt.hash(k, 4, 3501);
			orbit.box(Math.sin(th) * r, 21 + 2.4 * Math.sin(k * 2.1), Math.cos(th) * r, c, c * 0.85, c * 1.1, fine).rot(25 * k, 40 * k, 15 * k).density(3);
		}
		golemRise(m);
		golemFloat(m);
		golemPound(m);
	}

	/** It rises out of the land (its enter): heaving itself up out of the ground, arms reaching up and hauling down, its back straightening. */
	static void golemRise(SpellArt.Model m) {
		double L = 1.0;
		SpellArt.Anim a = m.anim("rise", L, null);
		a.sample("root", "position", 0, L, 8, t -> new double[] {0, -44 * (1 - SpellArt.easeOut(t / L)), 0});
		a.sample("body", "rotation", 0, L, 6, t -> new double[] {22 * (1 - SpellArt.ease(t / L)), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, L, 8, t -> new double[] {-150 * (1 - SpellArt.ease(t / (L * 0.85))), 0, -ss * 12 * (1 - SpellArt.ease(t / L))});
			a.sample(w + "_low", "rotation", 0, L, 6, t -> new double[] {-30 * (1 - SpellArt.ease(t / (L * 0.85))), 0, 0});
		}
		a.sample("orbit", "scale", 0, L, 6, t -> same(Math.max(0.02, SpellArt.clamp01((t - 0.4) / 0.6))));
	}

	/** It floats (looped without a seam): bobbing a little over the ground, its arms swinging slowly, its tail of stones swaying, its stones wheeling round it. */
	static void golemFloat(SpellArt.Model m) {
		double T = 3.2;
		SpellArt.Anim a = m.anim("float", T, "true");
		a.sample("root", "position", 0, T, 16, t -> new double[] {0, 4 + 1.4 * wave(t, T, 1, 0), 0});
		a.sample("body", "rotation", 0, T, 16, t -> new double[] {1.6 * wave(t, T, 1, -0.6), 0, 1.2 * wave(t, T, 1, 1.1)});
		a.sample("head", "rotation", 0, T, 16, t -> new double[] {2 * wave(t, T, 1, 0.4), 0, 0});
		for (int s = -1; s <= 1; s += 2) {
			String w = "arm_" + (s > 0 ? "l" : "r");
			int ss = s;
			a.sample(w, "rotation", 0, T, 16, t -> new double[] {-4 + 6 * ss * wave(t, T, 1, 0.3), 0, -ss * 3});
			a.sample(w + "_low", "rotation", 0, T, 16, t -> new double[] {-8 + 3 * wave(t, T, 1, -0.3 + ss), 0, 0});
		}
		for (int i = 0; i < 3; i++) {
			int ii = i;
			a.sample("tail_" + i, "rotation", 0, T, 16, t -> new double[] {4 * wave(t, T, 1, -0.8 * ii), 0, (4 + 3 * ii) * wave(t, T, 1, -0.8 * ii + 1.2)});
		}
		a.sample("orbit", "rotation", 0, T, 16, t -> new double[] {0, 360 * t / T, 0});
	}

	// --- Mountain Seal: the talisman sealing the mountain ---

	/** The talisman's middle over the mountain's foot (twenty blocks: over its summit, its chains clearing its flanks), its size, its stakes' ring. */
	static final double TALISMAN_UP = 80, TALISMAN_W = 10, TALISMAN_H = 28, PEG_R = 30;

	/** Whether ({@code x}, {@code y}) in [0, 1] (y up) lies on a stroke of 山, the character for mountain: three strokes rising off a base, the middle tallest. */
	static boolean shan(double x, double y) { return shanWithin(x, y, 0.075); }

	/** Whether ({@code x}, {@code y}) lies just round a stroke of {@link #shan} (its cut edge). */
	static boolean shanEdge(double x, double y) { return shanWithin(x, y, 0.12); }

	static boolean shanWithin(double x, double y, double w) {
		return Fire.segment(x, y, 0.1, 0.12, 0.9, 0.12) < w || Fire.segment(x, y, 0.5, 0.12, 0.5, 0.92) < w || Fire.segment(x, y, 0.14, 0.12, 0.14, 0.62) < w
				|| Fire.segment(x, y, 0.86, 0.12, 0.86, 0.62) < w;
	}

	/**
	 * The talisman (on a slab standing upright, its broad faces front and back): yellow jade within a border of gold; on each face the character
	 * for mountain in molten gold, and under it a talisman's script brushed in glowing cinnabar, a winding stroke crossed with strokes, and a
	 * column of small characters of seal script; its edges gold.
	 */
	static SpellArt.Painter talisman(int seed) {
		return t -> {
			if (t.f != SpellArt.Face.FRONT && t.f != SpellArt.Face.BACK) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.GOLD_PALE, 0.4), 0.95);
			double x = t.across(), y = t.up(), px = Math.min(x, 1 - x) * t.sx(), py = Math.min(y, 1 - y) * t.sy();
			if (px < 0.45 || py < 0.45) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.GOLD_PALE, 0.5), 0.96);
			if (px < 0.8 || py < 0.8) return SpellArt.col(Earth.JADE_Y_DARK);
			// The character at its head, square; under it the script, a winding stroke crossed with short ones; then a column of small characters.
			double yy = y * t.sy(), glyph = t.sx() * 0.76, foot = t.sy() - 1.4 - glyph, chars = t.sy() * 0.25;
			double gx = (x - 0.12) / 0.76, gy = (yy - foot) / glyph;
			if (shan(gx, gy)) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN_CORE, Earth.MOLTEN, 0.3));
			// Its strokes cut into the jade, dark round them (by day the gold was lost on the pale jade).
			if (shanEdge(gx, gy)) return SpellArt.col(Earth.warm(Earth.JADE_Y_DARK, -0.45));
			if (yy > chars + 0.4 && yy < foot - 0.8) {
				double wind = 0.5 + 0.14 * Math.sin(yy * 1.5 + seed) * SpellArt.smooth(chars, foot, yy);
				if (Math.abs(x - wind) < 0.05 || Math.abs(yy - Math.rint(yy / 1.7) * 1.7) < 0.17 && Math.abs(x - wind) < 0.24) return SpellArt.glow(SpellArt.mix(Earth.PASTE, 0xFF5A2A, 0.4), 0.95);
			}
			if (yy > 1.2 && yy < chars) {
				int cell = (int) Math.floor((yy - 1.2) / 2.2);
				double cy = ((yy - 1.2) / 2.2 - cell), cx = (x - 0.3) / 0.4;
				if (Earth.character(seed + cell * 5, (cx - 0.08) / 0.84, (cy - 0.08) / 0.84)) return SpellArt.glow(SpellArt.mix(Earth.PASTE, 0xFF5A2A, 0.3), 0.92);
			}
			double n = SpellArt.fbm(t.px * 0.3 + seed, t.py * 0.3 + t.pz * 0.3, seed, 3);
			return SpellArt.col(SpellArt.mix(Earth.JADE_Y_DARK, Earth.JADE_Y, 0.35 + 0.55 * n));
		};
	}

	/** A link of a chain of earth's light (on a box): its rim molten gold, hollow within (a ring seen face-on); its thin sides gold. */
	static SpellArt.Painter chainLink() {
		return t -> {
			double[] s = t.cube.faceSize(t.f);
			if (Math.min(s[0], s[1]) < 0.8) return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.GOLD_PALE, 0.3), 0.96);
			double ex = Math.min(t.fx, 1 - t.fx) * s[0], ey = Math.min(t.fy, 1 - t.fy) * s[1];
			if (Math.min(ex, ey) > 0.38) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, 0.2 + 0.5 * Math.min(1, Math.min(ex, ey) / 0.38)), 0.97);
		};
	}

	/** A stake of stone (on a box standing up): grey rock, a band of seal script round it glowing gold, its foot darker where it is driven in. */
	static SpellArt.Painter stake(int seed) {
		SpellArt.Painter stone = golemStone(1.6, 0.1, seed);
		return t -> {
			if (!t.flat() && Math.abs(t.up() - 0.62) < 0.12) {
				double a = (t.f.ordinal() + (t.side() ? t.back() : t.across())) * 0.5;
				return Earth.character(seed + (int) Math.floor(a * 4), (a * 4 - Math.floor(a * 4)), (t.up() - 0.5) / 0.24) ? SpellArt.glow(Earth.MOLTEN, 0.95)
						: SpellArt.col(Earth.warm(Earth.STONE_DARK, -0.1));
			}
			int c = stone.paint(t);
			return t.up() < 0.25 && !t.flat() ? SpellArt.col(Earth.warm(c & 0xFFFFFF, -0.3)) : c;
		};
	}

	/** The seal's ring round the mountain's foot (on a sheet): two rings of molten gold and between them characters of seal script; seen through, glowing. */
	static SpellArt.Painter sealRing(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z);
			if (r > 1 || r < 0.74) return SpellArt.CLEAR;
			if (Math.abs(r - 0.77) < 0.012 || Math.abs(r - 0.97) < 0.012) return SpellArt.glow(SpellArt.mix(Earth.EMBER, Earth.MOLTEN, 0.5), 0.88);
			if (r < 0.8 || r > 0.94) return SpellArt.CLEAR;
			double a = (Math.atan2(z, x) / (2 * Math.PI) + 0.5) * 24;
			int cell = (int) Math.floor(a);
			if (!Earth.character(seed + cell * 3, (a - cell - 0.12) / 0.76, ((r - 0.8) / 0.14 - 0.1) / 0.8)) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(Earth.MOLTEN, Earth.MOLTEN_CORE, 0.3), 0.9);
		};
	}

	/**
	 * Mountain Seal's talisman over the mountain it raises (its foot's middle at the origin; at the render scale a pixel is a quarter of a block):
	 * a plaque of yellow jade hung over the summit ("talisman", turning slowly), the character for mountain on it in molten gold and a talisman's
	 * script in glowing cinnabar, a haze of its light round it ("glow"); four chains of the earth's light ("chains") running from under it down
	 * over the mountain's flanks to stakes of stone driven into the ground round its foot; a ring of seal script turning round the foot ("ring").
	 */
	static void mountainTalisman(SpellArt.Model m) {
		m.tpp = 2;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone talisman = m.bone("talisman", "root", 0, TALISMAN_UP, 0);
		talisman.box(0, TALISMAN_UP, 0, TALISMAN_W, TALISMAN_H, 0.8, talisman(3601)).density(4);
		// A knot of red cord at its head, a tassel hanging from its foot.
		talisman.box(0, TALISMAN_UP + TALISMAN_H / 2 + 0.7, 0, 1.4, 1.4, 1.4, Earth.cord(false)).density(4);
		SpellArt.blade(talisman, SpellArt.v3(0, TALISMAN_UP - TALISMAN_H / 2, 0), 5, 2.4, 180, 90, 0, Earth.cord(true)).density(4);
		SpellArt.Bone glow = m.bone("glow", "root", 0, TALISMAN_UP, 0);
		// Its glow on two standing sheets only (a level one cut a line across it, seen from beside it).
		SpellArt.sheet(glow, 0, TALISMAN_UP, 0, 44, 44, 90, 0, 0, haze(Earth.GOLD_PALE, Earth.MOLTEN, 0.2)).density(2);
		SpellArt.sheet(glow, 0, TALISMAN_UP, 0, 44, 44, 90, 90, 0, haze(Earth.GOLD_PALE, Earth.MOLTEN, 0.2)).density(2);
		SpellArt.Bone chains = m.bone("chains", "root", 0, TALISMAN_UP - TALISMAN_H / 2 - 1, 0);
		SpellArt.Painter link = chainLink(), stake = stake(3611);
		double[] from = SpellArt.v3(0, TALISMAN_UP - TALISMAN_H / 2 - 1, 0);
		for (int k = 0; k < 4; k++) {
			double th = Math.toRadians(45 + 90 * k);
			double[] to = SpellArt.v3(Math.sin(th) * PEG_R, 6.5, Math.cos(th) * PEG_R);
			int links = 26;
			for (int i = 0; i < links; i++) {
				double s0 = i / (double) links, s1 = (i + 1) / (double) links;
				double[] a = chainAt(from, to, s0), b = chainAt(from, to, s1), dir = SpellArt.sub(b, a), mid = SpellArt.times(SpellArt.add(a, b), 0.5), r = SpellArt.aimY(dir[0], dir[1], dir[2]);
				double len = SpellArt.len(dir) * 1.25;
				if (i % 2 == 0) chains.box(mid[0], mid[1], mid[2], 1.6, len, 0.5, link).rot(r[0], r[1], 0).density(5);
				else chains.box(mid[0], mid[1], mid[2], 0.5, len, 1.6, link).rot(r[0], r[1], 0).density(5);
			}
			chains.box(to[0], to[1] - 3.5, to[2], 3, 9, 3, stake).density(3);
		}
		SpellArt.Bone ring = m.bone("ring", "root", 0, 0, 0);
		SpellArt.sheet(ring, 0, 0.4, 0, 2 * (PEG_R + 6), 2 * (PEG_R + 6), 0, 0, 0, sealRing(3621)).density(1.6);
		talismanDescend(m);
		talismanHold(m);
		talismanBreak(m);
	}

	/** A chain's place {@code s} of the way from {@code a} to {@code b}, drawn taut (sagging a little at its middle). */
	static double[] chainAt(double[] a, double[] b, double s) {
		double[] p = SpellArt.add(a, SpellArt.times(SpellArt.sub(b, a), s));
		p[1] -= 2 * Math.sin(Math.PI * s);
		return p;
	}

	/** It comes down on the mountain (its enter): the talisman falling out of the sky onto it; then its chains shoot down to stake it, its ring spreading. */
	static void talismanDescend(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("descend", L, null);
		a.sample("talisman", "position", 0, 0.4, 5, t -> new double[] {0, 48 * (1 - SpellArt.easeIn(t / 0.4)), 0});
		a.sample("talisman", "scale", 0, 0.4, 5, t -> same(0.5 + 0.5 * SpellArt.easeOut(t / 0.4)));
		a.sample("glow", "position", 0, 0.4, 5, t -> new double[] {0, 48 * (1 - SpellArt.easeIn(t / 0.4)), 0});
		a.scale("chains", 0, 0.02);
		a.scale("chains", 0.36, 0.02);
		a.sample("chains", "scale", 0.36, L, 4, t -> same(Math.max(0.02, SpellArt.easeOut((t - 0.36) / (L - 0.36)))));
		a.scale("ring", 0, 0.05);
		a.scale("ring", 0.4, 0.05);
		a.scale("ring", L, 1);
	}

	/** It holds the mountain sealed (looped without a seam): the talisman turning slowly over it and hanging still in the air, its glow breathing, its ring turning round its foot. */
	static void talismanHold(SpellArt.Model m) {
		double T = 6;
		SpellArt.Anim a = m.anim("hold", T, "true");
		a.sample("talisman", "rotation", 0, T, 12, t -> new double[] {0, 360 * t / T, 0});
		a.sample("talisman", "position", 0, T, 12, t -> new double[] {0, 1.2 * wave(t, T, 2, 0), 0});
		a.sample("glow", "scale", 0, T, 12, t -> same(1 + 0.08 * wave(t, T, 3, 0)));
		a.sample("ring", "rotation", 0, T, 12, t -> new double[] {0, -360 * t / T, 0});
	}

	/** It lets go (played once, held: it fades a moment after): its chains drawn up out of the land into it, the talisman lifting away, its ring closing. */
	static void talismanBreak(SpellArt.Model m) {
		double L = 0.6;
		SpellArt.Anim a = m.anim("break", L, "hold_on_last_frame");
		a.sample("chains", "scale", 0, 0.35, 4, t -> same(Math.max(0.02, 1 - SpellArt.easeIn(t / 0.35))));
		a.scale("chains", L, 0.02);
		a.sample("talisman", "position", 0, L, 5, t -> new double[] {0, 18 * SpellArt.easeIn(t / L), 0});
		a.sample("glow", "position", 0, L, 5, t -> new double[] {0, 18 * SpellArt.easeIn(t / L), 0});
		a.sample("ring", "scale", 0, L, 5, t -> same(Math.max(0.05, 1 - 0.8 * SpellArt.ease(t / L))));
	}

	// --- Absolute Zero: the coffin of ice ---

	static final int ICE_DEEP = 0x5AAEE0, ICE_CLEAR = 0xA8E6FF, ICE_PALE = 0xE4FAFF;

	/**
	 * Clear ice (on a crystal's prism, its length up its cube): seen through so what it holds shows, pale blue, cracked through with fine white
	 * fractures; its edges and its upper end glowing white-cyan; rimed white near its foot.
	 */
	static SpellArt.Painter clearIce(int seed) {
		return t -> {
			double[] s = t.cube.faceSize(t.f);
			double ex = Math.min(t.fx, 1 - t.fx) * s[0], ey = Math.min(t.fy, 1 - t.fy) * s[1], up = t.flat() ? 1 : t.up();
			if (Math.min(ex, t.flat() ? ey : 99) < 0.32 || ex < 0.32) return SpellArt.glow(SpellArt.mix(ICE_CLEAR, ICE_PALE, 0.4 + 0.5 * up), 0.85);
			double crack = 1 - Math.abs(2 * SpellArt.fbm3(t.px * 0.5, t.py * 0.25, t.pz * 0.5, seed, 2) - 1);
			if (crack > 0.93) return SpellArt.glow(ICE_PALE, 0.55);
			if (up < 0.14) return SpellArt.glow(SpellArt.mix(ICE_PALE, 0xFFFFFF, 0.5), 0.7 - up * 2);
			double n = SpellArt.fbm(t.px * 0.3 + seed, t.py * 0.2 + t.pz * 0.3, seed + 1, 2);
			if (up > 0.82) return SpellArt.glow(SpellArt.mix(ICE_CLEAR, ICE_PALE, (up - 0.82) * 5), 0.7);
			return SpellArt.col(SpellArt.mix(ICE_DEEP, ICE_CLEAR, 0.45 + 0.4 * n + 0.15 * up), 0.34 + 0.12 * n);
		};
	}

	/** A crystal's point (on a blade, its length along it): a spike of clear ice narrowing to its tip, its edges and tip glowing white-cyan. */
	static SpellArt.Painter icePoint() {
		return t -> {
			double v = t.v(), u = Math.abs(t.u() - 0.5), half = 0.5 * (1 - v);
			if (u > half) return SpellArt.CLEAR;
			if (half - u < 0.08 || v > 0.7) return SpellArt.glow(SpellArt.mix(ICE_CLEAR, ICE_PALE, v), 0.88);
			return SpellArt.col(SpellArt.mix(ICE_DEEP, ICE_CLEAR, 0.5 + 0.4 * v), 0.4);
		};
	}

	/** Rime on the ground (on a sheet): frost spread out from the heart in feathered rays, white, thinning to nothing at the sheet's rim; glowing faintly. */
	static SpellArt.Painter rime(int seed) {
		return t -> {
			double x = (t.across() - 0.5) * 2, z = (t.back() - 0.5) * 2, r = Math.hypot(x, z), a = Math.atan2(z, x);
			if (r > 0.98) return SpellArt.CLEAR;
			double ray = Math.abs(Math.sin(a * 6 + seed)), feather = Math.abs(Math.sin(a * 40 + r * 18 + seed));
			double frost = (1 - r) * (0.35 + 0.45 * Math.pow(ray, 6) + 0.25 * Math.pow(feather, 12) * (1 - r));
			if (frost < 0.06) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(ICE_CLEAR, ICE_PALE, frost), SpellArt.clamp01(frost * 1.3));
		};
	}

	/** One crystal of the coffin: its foot, the way it leans (a unit), its length and breadth. */
	record Crystal(double[] foot, double[] way, double length, double width) {}

	static final Crystal[] COFFIN = coffin();

	/** The coffin's crystals: tall ones round the being, leaning in over it; a few low across its front and back; small spikes thrust out round its foot. */
	static Crystal[] coffin() {
		java.util.List<Crystal> all = new java.util.ArrayList<>();
		int tall = 10;
		for (int k = 0; k < tall; k++) {
			double th = 2 * Math.PI * (k + 0.45 * SpellArt.hash(k, 1, 3701)) / tall, r = 5.8 + 1.8 * SpellArt.hash(k, 2, 3701);
			double[] foot = SpellArt.v3(Math.sin(th) * r, -0.5, Math.cos(th) * r);
			// Some leaning in over the being, some standing out from it; and turned a little round it, so no two stand alike.
			double length = 21 + 17 * SpellArt.hash(k, 3, 3701), lean = -1.5 + 6 * SpellArt.hash(k, 4, 3701), twist = (SpellArt.hash(k, 11, 3701) - 0.5) * 0.9;
			double[] top = SpellArt.v3(Math.sin(th + twist) * lean, length * 0.95, Math.cos(th + twist) * lean);
			all.add(new Crystal(foot, unit(SpellArt.sub(top, foot)), length, 4.2 + 3.6 * SpellArt.hash(k, 5, 3701)));
		}
		for (int k = 0; k < 3; k++) {
			double th = 2 * Math.PI * k / 3 + 0.5;
			double[] foot = SpellArt.v3(Math.sin(th) * 5, 4 + 6 * k, Math.cos(th) * 5);
			double[] way = unit(SpellArt.v3(-Math.sin(th + 1.6) * 0.8, 0.9, -Math.cos(th + 1.6) * 0.8));
			all.add(new Crystal(foot, way, 14 + 3 * k, 4));
		}
		for (int k = 0; k < 9; k++) {
			double th = 2 * Math.PI * (k + 0.5) / 9 + 0.2;
			double[] foot = SpellArt.v3(Math.sin(th) * 8.5, -0.5, Math.cos(th) * 8.5);
			all.add(new Crystal(foot, unit(SpellArt.v3(Math.sin(th), 1.1 + 0.5 * SpellArt.hash(k, 6, 3701), Math.cos(th))), 6 + 5 * SpellArt.hash(k, 7, 3701), 2.6));
		}
		return all.toArray(new Crystal[0]);
	}

	/**
	 * Absolute Zero's coffin of ice round one being frozen in it (its feet at the origin), two blocks tall at the size its kind draws it (its spell
	 * sizes it to the being): clear crystals of ice grown up round it and leaning in over it, a few low across it, spikes thrust out round its foot
	 * ("crystal_0".., each its own bone: they fly apart as it shatters); rime spread over the ground ("rime"); a haze of cold ("cold").
	 */
	static void iceCoffin(SpellArt.Model m) {
		m.tpp = 4;
		m.scale = 1;
		m.bone("root", null, 0, 0, 0);
		SpellArt.Bone rime = m.bone("rime", "root", 0, 0, 0);
		SpellArt.sheet(rime, 0, 0.2, 0, 34, 34, 0, 0, 0, rime(3703)).density(2);
		SpellArt.Bone cold = m.bone("cold", "root", 0, 14, 0);
		hazeBall(cold, SpellArt.v3(0, 14, 0), 34, haze(ICE_PALE, ICE_CLEAR, 0.22), 1.5);
		SpellArt.Painter ice = clearIce(3705), point = icePoint();
		for (int k = 0; k < COFFIN.length; k++) {
			Crystal c = COFFIN[k];
			SpellArt.Bone b = m.bone("crystal_" + k, "root", c.foot()[0], c.foot()[1], c.foot()[2]);
			double body = c.length() * 0.78;
			double[] end = SpellArt.add(c.foot(), SpellArt.times(c.way(), body));
			SpellArt.limb(b, c.foot(), end, c.width(), c.width() * 0.8, ice);
			crossedTongue(m, b, "point_" + (c.width() > 4.5 ? 0 : c.width() > 3 ? 1 : 2), SpellArt.sub(end, SpellArt.times(c.way(), 0.4)), c.way(), c.length() - body + 0.4,
					c.width(), point, 3, k % 2 == 1);
		}
		coffinFreeze(m);
		coffinHold(m);
		coffinShatter(m);
	}

	/** It freezes (its enter): the rime racing out over the ground, every crystal shooting up out of it, one hard on another. */
	static void coffinFreeze(SpellArt.Model m) {
		double L = 0.45;
		SpellArt.Anim a = m.anim("freeze", L, null);
		a.sample("rime", "scale", 0, 0.2, 4, t -> new double[] {0.1 + 0.9 * SpellArt.easeOut(t / 0.2), 1, 0.1 + 0.9 * SpellArt.easeOut(t / 0.2)});
		a.sample("cold", "scale", 0, L, 4, t -> same(Math.max(0.05, SpellArt.easeOut(t / L))));
		for (int k = 0; k < COFFIN.length; k++) {
			double t0 = 0.03 + 0.012 * k, t1 = t0 + 0.12;
			String b = "crystal_" + k;
			a.scale(b, 0, 0.02);
			a.scale(b, t0, 0.02);
			a.scale(b, t1, 1.1);
			a.scale(b, Math.min(L, t1 + 0.08), 1);
			a.scale(b, L, 1);
		}
	}

	/** It holds its being frozen (looped without a seam): all still but the cold breathing round it and the rime's glitter. */
	static void coffinHold(SpellArt.Model m) {
		double T = 2.4;
		SpellArt.Anim a = m.anim("hold", T, "true");
		a.sample("cold", "scale", 0, T, 8, t -> same(1 + 0.06 * wave(t, T, 1, 0)));
		a.sample("rime", "scale", 0, T, 8, t -> new double[] {1 + 0.02 * wave(t, T, 2, 1), 1, 1 + 0.02 * wave(t, T, 2, 1)});
	}

	/** It shatters (played once, held: it is gone a moment after): every crystal flung out from it, tumbling and falling as it breaks. */
	static void coffinShatter(SpellArt.Model m) {
		double L = 0.45;
		SpellArt.Anim a = m.anim("shatter", L, "hold_on_last_frame");
		for (int k = 0; k < COFFIN.length; k++) {
			Crystal c = COFFIN[k];
			double[] out = unit(SpellArt.v3(c.foot()[0] + 0.01, 0, c.foot()[2]));
			double far = 10 + 8 * SpellArt.hash(k, 8, 3701), up = 4 + 6 * SpellArt.hash(k, 9, 3701), spin = (120 + 200 * SpellArt.hash(k, 10, 3701)) * (k % 2 == 0 ? 1 : -1);
			String b = "crystal_" + k;
			a.sample(b, "position", 0, L, 5, t -> {
				double f = SpellArt.easeOut(t / L);
				return new double[] {out[0] * far * f, up * f - 14 * (t / L) * (t / L), out[2] * far * f};
			});
			a.sample(b, "rotation", 0, L, 5, t -> new double[] {spin * t / L, 0, spin * 0.6 * t / L});
			a.sample(b, "scale", 0, L, 5, t -> same(1 - 0.5 * t / L));
		}
		a.sample("cold", "scale", 0, L, 4, t -> same(1 + 1.5 * SpellArt.easeOut(t / L)));
	}

	// --- Heaven-Severing Sword: the sword of light ---

	static final int SEVER_LIGHT = 0xFFF4D0, SEVER_WARM = 0xFFE29A;
	/** The sword's length (SEVER_SWORD, 34 blocks), its stroke's start and end (the bone's turn about y: behind on the right, round to the left). */
	static final double SEVER_LEN = 136, SEVER_FROM = 125, SEVER_TO = -105;
	/** The stroke's times (seconds): the sword forms in the hand (MetalSpells SEVER_FORM, 4 ticks), is swung (SEVER_SWING, 5 ticks). */
	static final double SEVER_FORMED = 0.2, SEVER_SWUNG = 0.45;

	/**
	 * The sword of light's blade (on a box lying flat, its length along z): white-gold light, a fuller of white fire down its middle carved with
	 * seal script, its edges keen white; glowing throughout, a little seen through away from its fuller.
	 */
	static SpellArt.Painter lightBlade(int seed) {
		return t -> {
			if (!t.flat()) return SpellArt.glow(SpellArt.mix(SEVER_LIGHT, 0xFFFFFF, 0.6), 0.96);
			double u = Math.abs(t.across() - 0.5) * 2, z = t.qz();
			if (u > 0.82) return SpellArt.glow(0xFFFFFF, 0.97);
			if (u < 0.22) {
				double cell = -z / 4.6;
				int ci = (int) Math.floor(cell);
				if (u < 0.17 && Earth.character(seed + ci, (cell - ci - 0.12) / 0.76, t.across() < 0.5 ? (0.5 - t.across()) / 0.17 * 0.5 + 0.5 : (t.across() - 0.5) / 0.17 * 0.5 + 0.5))
					return SpellArt.glow(SEVER_WARM, 0.97);
				return SpellArt.glow(0xFFFFFF, 0.95);
			}
			double streak = SpellArt.noise(z * 0.15 + seed, u * 3, seed);
			return SpellArt.glow(SpellArt.mix(SEVER_WARM, SEVER_LIGHT, 0.4 + 0.5 * streak + 0.2 * u), 0.8 + 0.12 * u);
		};
	}

	/** The point of the sword of light (on a blade, its length along it): narrowing to its tip, keen white edges, white fire at its tip. */
	static SpellArt.Painter lightPoint() {
		return t -> {
			double v = t.v(), u = Math.abs(t.u() - 0.5), half = 0.5 * (1 - v);
			if (u > half) return SpellArt.CLEAR;
			if (half - u < 0.09 || v > 0.75) return SpellArt.glow(0xFFFFFF, 0.97);
			return SpellArt.glow(SpellArt.mix(SEVER_WARM, SEVER_LIGHT, 0.5 + 0.5 * v), 0.85);
		};
	}

	/** The sword's light round its blade (on a blade along it): a soft white-gold glow, strongest along its middle, thinning out across and toward its point. */
	static SpellArt.Painter bladeAura() {
		return t -> {
			double u = Math.abs(t.u() - 0.5) * 2, v = t.v(), a = (1 - u) * (1 - u) * (1 - 0.6 * v);
			return a < 0.04 ? SpellArt.CLEAR : SpellArt.glow(SpellArt.mix(SEVER_WARM, SEVER_LIGHT, 1 - u), 0.4 * a);
		};
	}

	/**
	 * The light its stroke leaves (on a sheet lying level, its bone's quarter from the blade, -z, round toward -x, where the blade has been): a
	 * fan seventy degrees wide out from the hilt, from a sixth of the blade's length to just past its point, streaked along the stroke, white at
	 * the blade and fading back from it.
	 */
	static SpellArt.Painter strokeFan() {
		return t -> {
			double x = -SEVER_LEN * 1.06 * (1 - t.across()), z = -SEVER_LEN * 1.06 * (1 - t.back()), r = Math.hypot(x, z), phi = Math.toDegrees(Math.atan2(-x, -z));
			if (phi < -1 || phi > 70 || r < SEVER_LEN * 0.16 || r > SEVER_LEN * 1.04) return SpellArt.CLEAR;
			double behind = Math.max(0, phi) / 70, fade = (1 - behind) * (1 - behind), out = r / SEVER_LEN;
			double streak = 0.82 + 0.18 * Math.sin(r * 0.55) * Math.sin(r * 0.17 + 1);
			double a = fade * streak * (0.35 + 0.65 * out) * SpellArt.smooth(0.16, 0.3, out) * (1 - SpellArt.smooth(0.98, 1.04, out));
			if (a < 0.03) return SpellArt.CLEAR;
			return SpellArt.glow(SpellArt.mix(SEVER_WARM, 0xFFFFFF, 1 - behind), SpellArt.clamp01(a * 0.95));
		};
	}

	/**
	 * The Heaven-Severing Sword's sword of light at its caster's chest, the hilt in their right hand (-x) just before them, the whole tilted a
	 * little (high on the right, low on the left: "tilt"): a jian thirty-four blocks long ("sword", on "swing", which turns about the hilt), its
	 * blade of white-gold light with a fuller of white fire carved with seal script, a guard of gold, a grip of dark silk, a pommel and a red
	 * tassel, its light round it; the fan of light its stroke leaves behind its edge ("trail", turning with it).
	 */
	static void severingSword(SpellArt.Model m) {
		m.tpp = 1.5;
		m.scale = 4;
		m.bone("root", null, 0, 0, 0);
		m.bone("tilt", "root", 0, 0, 0, 0, 0, 8);
		double[] hilt = SpellArt.v3(-2.4, 0, -1.2);
		m.bone("swing", "tilt", hilt[0], hilt[1], hilt[2]);
		SpellArt.Bone sword = m.bone("sword", "swing", hilt[0], hilt[1], hilt[2]);
		double x = hilt[0], y = hilt[1], z = hilt[2];
		SpellArt.Painter blade = lightBlade(3801), gold = Metal.gold(3803);
		sword.box(x, y, z - 21, 4.8, 1, 40, blade).density(2);
		sword.box(x, y, z - 61, 4.3, 0.9, 40.4, blade).density(2);
		sword.box(x, y, z - 96, 3.7, 0.8, 30, blade).density(2);
		crossedTongue(m, sword, "point", SpellArt.v3(x, y, z - 110.6), SpellArt.v3(0, 0, -1), SEVER_LEN - 110.6, 3.7, lightPoint(), 2.5, false);
		// The guard, its ends swept forward; the grip, the pommel, the tassel.
		sword.box(x, y, z + 0.4, 10, 1.8, 2, gold).density(4);
		sword.box(x - 5.6, y, z - 0.4, 2.4, 1.6, 2, gold).rot(0, -30, 0).density(4);
		sword.box(x + 5.6, y, z - 0.4, 2.4, 1.6, 2, gold).rot(0, 30, 0).density(4);
		sword.box(x, y, z + 4.4, 1.6, 1.6, 6, Metal.grip()).density(4);
		sword.box(x, y, z + 8, 2.6, 2.6, 1.4, gold).density(4);
		SpellArt.blade(sword, SpellArt.v3(x, y - 0.6, z + 8.6), 6, 1.6, -150, 0, 0, Earth.cord(true)).density(4);
		// Its light round it: a glow along the blade, standing and lying.
		SpellArt.shared(m, sword, "aura", true, SpellArt.v3(x, y, z - 2), SEVER_LEN - 2, 16, 0, 180, 0, bladeAura(), 0.5, false);
		SpellArt.shared(m, sword, "aura_up", false, SpellArt.v3(x, y, z - 2), SEVER_LEN - 2, 10, -90, 180, 0, bladeAura(), 0.5, false);
		SpellArt.Bone trail = m.bone("trail", "swing", hilt[0], hilt[1], hilt[2]);
		double w = SEVER_LEN * 1.06;
		SpellArt.sheet(trail, x - w / 2, y, z - w / 2, w, w, 0, 0, 0, strokeFan()).density(0.9);
		severStroke(m);
		severGone(m);
	}

	/** Its blade's turn {@code t} seconds into its stroke: held behind on the right as it forms, then swung round, fast out of the wind-up and settling at its end. */
	static double severAngle(double t) {
		double u = SpellArt.clamp01((t - SEVER_FORMED) / (SEVER_SWUNG - SEVER_FORMED));
		return SEVER_FROM + (SEVER_TO - SEVER_FROM) * (1 - Math.pow(1 - u, 3));
	}

	/**
	 * Its stroke (its enter): the sword forming in the hand out of light, swung level across its caster's front, the fan of light opening after
	 * its edge and closing once it is still; then it goes back into light from its point down to the hilt.
	 */
	static void severStroke(SpellArt.Model m) {
		double L = 1.1;
		SpellArt.Anim a = m.anim("stroke", L, null);
		a.sample("swing", "rotation", 0, SEVER_SWUNG + 0.05, 20, t -> new double[] {0, severAngle(t), 0});
		a.rot("swing", L, 0, SEVER_TO, 0);
		a.sample("sword", "scale", 0, SEVER_FORMED, 4, t -> same(0.05 + 0.95 * SpellArt.easeOut(t / SEVER_FORMED)));
		a.scale("sword", 0.62, 1);
		a.sample("sword", "scale", 0.62, 1.0, 5, t -> new double[] {1, 1, Math.max(0.02, 1 - SpellArt.easeIn((t - 0.62) / 0.38))});
		a.scale("sword", L, 0.02);
		a.key("trail", "scale", 0, 0.02, 1, 1);
		a.key("trail", "scale", SEVER_FORMED, 0.02, 1, 1);
		a.key("trail", "scale", SEVER_FORMED + 0.1, 1, 1, 1);
		a.key("trail", "scale", SEVER_SWUNG, 1, 1, 1);
		a.key("trail", "scale", SEVER_SWUNG + 0.18, 0.02, 1, 1);
		a.key("trail", "scale", L, 0.02, 1, 1);
	}

	/** After its stroke (looped): gone back into light, the sword and its fan drawn in to nothing at the hilt, where its stroke ended. */
	static void severGone(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("gone", 1, "true");
		a.rot("swing", 0, 0, SEVER_TO, 0);
		a.rot("swing", 1, 0, SEVER_TO, 0);
		a.scale("sword", 0, 0.02);
		a.scale("sword", 1, 0.02);
		a.key("trail", "scale", 0, 0.02, 1, 1);
		a.key("trail", "scale", 1, 0.02, 1, 1);
	}

	/** Its blow (played once): both fists heaved up overhead, then brought down together on its mark, its body thrown into it. */
	static void golemPound(SpellArt.Model m) {
		SpellArt.Anim a = m.anim("pound", 0.6, null);
		double[] t = {0, 0.16, 0.3, 0.42, 0.6};
		double[] raise = {0, 1, 0, 0, 0}, slam = {0, 0, 1, 0.6, 0};
		for (int i = 0; i < t.length; i++) {
			for (int s = -1; s <= 1; s += 2) {
				String w = "arm_" + (s > 0 ? "l" : "r");
				a.rot(w, t[i], -150 * raise[i] - 28 * slam[i], 0, -s * 3);
				a.rot(w + "_low", t[i], -34 * raise[i] - 6 * slam[i], 0, 0);
			}
			a.rot("body", t[i], -8 * raise[i] + 20 * slam[i], 0, 0);
			a.rot("head", t[i], -6 * raise[i] + 8 * slam[i], 0, 0);
		}
	}
}
