import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * The Immortal Realm's primordial beasts for BeastArt (PrimordialBeast, 2026-10-26): the Peng the Kun becomes (geo/demon_beast/
 * kun_peng_peng.geo.json) and the True Phoenix (true_phoenix), each its texture, glow mask and "idle" animation as Bodies writes the wild
 * beasts'. Drawn by the game five and a half to six and a half times their size (BeastKind#modelScale), so a pixel here is a third of a
 * block there or more: bold shapes, every eye with its lid (beast-art rules: per-face UV, lids exactly over the painted eyes, faces that
 * stand out from what is round them). The Kun is Void Shattering's own (tools/spell-art's Void#kun, the user, 2026-10-26: "Make the ku's
 * first form look like the one from the spell that summons it"), and the True Dragon tools/spell-art's TrueDragon ("overhaul the dragon
 * model"); both were first made here.
 * <p>
 * The bones the client moves (BeastModels): the Peng's and the Phoenix's as a bird's ("body", "head", "jaw", "lids", "wing_l"/"_r" and
 * their "_tip"s, "leg_l"/"_r", the Peng's "tail_0", the Phoenix's streamers "fan_0".."fan_4" and their "_tip"s).
 * <p>
 * Run: {@code java BeastArt.java <assets> primordials}.
 */
public class Primordials {
	static int c(int hex) { return Bodies.c(hex); }

	static int glow(int hex) { return Bodies.glow(hex); }

	// --- Painters of their own ---

	/** Eyes on the side faces of a head cube, two rows ({@code row}, {@code row + 1}) from the front edge's second pixel {@code width} back; the pupil at the back of the lower row. */
	static BeastArt.Painter sideEyes2(Bodies.Coat k, BeastArt.Painter under, int row, int width) {
		return (f, x, y, w, h, d) -> {
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && (y == row || y == row + 1)) {
				int front = f == BeastArt.Face.SIDE_A ? w - 1 - x : x; // 0 at the front edge
				if (front >= 1 && front <= width) {
					if (y == row + 1 && front == width) return Bodies.BLACK;
					return glow(y == row ? k.eye() : BeastArt.mix(k.eye(), c(0xFFFFFF), 0.35));
				}
				if (y == row - 1 && front >= 1 && front <= width + 1) return k.dark(); // a heavy brow over it
			}
			return under.paint(f, x, y, w, h, d);
		};
	}

	/**
	 * Lids over {@link #sideEyes2}' eyes on a head cube (its top at {@code top}, {@code h} tall, its front at {@code z}, {@code d} deep,
	 * {@code halfWidth} out from the middle): two rows from {@code row}, {@code width} pixels from the front edge's second, a fifth of a
	 * pixel proud of each side; shut only as it blinks (the "idle" animation scales "lids" from nothing).
	 */
	static void sideLids2(BeastArt.Model m, String head, double top, double h, int row, double z, double d, double halfWidth, int width, int colour) {
		double py = Bodies.rowOf(h), pz = Bodies.rowOf(d), y = top - (row + 2) * py;
		BeastArt.Bone lids = m.bone("lids", head, 0, y + 2 * py, z);
		double from = z + pz - 0.05, length = width * pz + 0.1;
		lids.cube(halfWidth, y, from, 0.2, 2 * py, length, BeastArt.solid(colour));
		lids.cube(-halfWidth - 0.2, y, from, 0.2, 2 * py, length, BeastArt.solid(colour));
	}

	/** Pinions: feathers in rows across the wing, each row's edge in {@code edge}; the wing's trailing edge (its back) glowing {@code burn}. */
	static BeastArt.Painter pinions(int base, int edge, int burn, int seed) {
		BeastArt.Painter rows = Bodies.feathers(base, edge, seed);
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.BACK || (f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) && y >= h - 1) return glow(burn);
			if ((f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) && Math.floorMod(x, 4) == 0) return edge;
			return rows.paint(f, x, y, w, h, d);
		};
	}

	/** A beak of gold (or a talon), darkening only toward its point (the first drawn were dull brown boxes: tour 14). */
	static BeastArt.Painter goldBeak(int gold, int tip, int seed) {
		return (f, x, y, w, h, d) -> {
			double t = BeastArt.frontness(f, x, y, w, h);
			return Bodies.noise(BeastArt.mix(gold, tip, Math.max(0, t - 0.65) / 0.35 * 0.75), f, x, y, seed, 0.06);
		};
	}

	// --- The Peng ---

	static final Bodies.Coat PENG = new Bodies.Coat(c(0xECE8E0), c(0x2A5C9A), c(0xFFFFFF), c(0xF4F0E8), c(0xFFC030), c(0x8FD8FF), c(0xE8C060));

	/**
	 * The Peng, the Kun become a bird: white as the clouds it broke from, its flight feathers and the bars of its tail the sky's azure, their
	 * edges glowing; a crest of three plumes swept back; a hooked beak and talons of gold; fierce golden eyes. Its wings span some sixty
	 * blocks as the game draws it.
	 */
	static void peng(BeastArt.Model m) {
		Bodies.Coat k = PENG;
		BeastArt.Painter plumage = Bodies.feathers(k.base(), BeastArt.mix(k.base(), k.dark(), 0.35), 650);
		BeastArt.Painter breast = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN || f == BeastArt.Face.FRONT ? Bodies.noise(k.belly(), f, x, y, 651, 0.08)
				: plumage.paint(f, x, y, w, h, d);
		BeastArt.Bone body = m.bone("body", null, 0, 10.5, 0);
		Bodies.cc(body, 0, 4, 0, 16, 13, 28, breast);
		// Its head on a ruffed neck; side eyes under dark brows; its beak, gold and hooked, darkening only at its point; its crest.
		BeastArt.Bone head = m.bone("head", null, 0, 17, -13);
		Bodies.cc(head, 0, 13, -15, 11, 10, 6, plumage); // its ruffed neck
		Bodies.cc(head, 0, 16, -21, 10, 10, 10, sideEyes2(k, Bodies.plain(k.light(), 652), 3, 2));
		sideLids2(m, "head", 26, 10, 3, -26, 10, 5, 2, k.base());
		BeastArt.Painter beak = goldBeak(k.horn(), c(0x3A2A10), 656);
		Bodies.cc(head, 0, 18.5, -29, 4.4, 4, 6, beak);
		Bodies.cc(head, 0, 16, -32.2, 2.8, 3.6, 2, beak); // its hook, turned down
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 18.5, -26);
		Bodies.cc(jaw, 0, 17.3, -28.6, 3.4, 1.4, 5.2, beak);
		for (int i = 0; i < 3; i++) {
			BeastArt.Bone plume = m.bone("crest_" + i, "head", 0, 26, -19, 18 + i * 10, (i - 1) * 14, 0);
			plume.cube(-0.9, 25.5, -19, 1.8, 1.8, 12, (f, x, y, w, h, d) -> BeastArt.frontness(f, x, y, w, h) < 0.35 ? glow(k.glowing()) : k.dark());
		}
		// The wings: the inner white with azure coverts, the outer the azure pinions, their trailing edges glowing.
		BeastArt.Painter inner = pinions(k.base(), k.dark(), BeastArt.mix(k.glowing(), c(0xFFFFFF), 0.3), 653);
		BeastArt.Painter outer = pinions(BeastArt.mix(k.dark(), k.base(), 0.25), c(0x173860), k.glowing(), 654);
		for (int side : new int[] {1, -1}) {
			String n = side > 0 ? "wing_l" : "wing_r";
			BeastArt.Bone wing = m.bone(n, null, side * 8, 16.5, -6);
			wing.cube(side > 0 ? 8 : -40, 15.5, -8, 32, 2, 20, inner);
			BeastArt.Bone tip = m.bone(n + "_tip", n, side * 40, 16.5, -6);
			tip.cube(side > 0 ? 40 : -76, 15.75, -8, 36, 1.5, 14, outer);
		}
		// Its tail: a broad fan, barred azure, its edge glowing.
		BeastArt.Bone tail = m.bone("tail_0", null, 0, 11, 14, -8, 0, 0);
		Bodies.cc(tail, 0, 10.25, 27, 18, 1.5, 26, (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.BACK || (f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) && y >= h - 1) return glow(k.glowing());
			return Math.floorMod(f == BeastArt.Face.UP || f == BeastArt.Face.DOWN ? y : x, 4) == 0 ? k.dark() : Bodies.noise(k.base(), f, x, y, 655, 0.08);
		});
		// Its legs, gold, its talons dark.
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone leg = m.bone(side > 0 ? "leg_l" : "leg_r", null, side * 4, 5, 3);
			Bodies.cc(leg, side * 4, 0, 3, 2.5, 5.5, 2.5, BeastArt.solid(k.horn()));
			Bodies.cc(leg, side * 4, 0, 1.4, 3.6, 1.2, 4.6, BeastArt.solid(c(0x2A2218)));
		}
	}

	// --- The True Phoenix ---

	static final Bodies.Coat PHOENIX = new Bodies.Coat(c(0xC8241E), c(0x6A0E0A), c(0xFF8A3A), c(0xFFC040), c(0xFFF0A0), c(0xFFB020), c(0xFFD060));
	/** Its five streamers' colours: crimson, gold, jade, azure, violet (the five colours the phoenix wears). */
	static final int[] STREAMERS = {c(0xE8201A), c(0xFFC020), c(0x2FC878), c(0x3A8AF0), c(0x9A40E0)};

	/**
	 * The True Phoenix, fire's: crimson, its breast gold, its feathers edged in gold; a long neck, a gold beak, bright eyes under dark brows;
	 * a crest of three plumes of fire; wings crimson to gold, their trailing edges burning; five long streamers behind it, one of each of the
	 * five colours, an eye of light near the end of each, burning out at their tips.
	 */
	static void truePhoenix(BeastArt.Model m) {
		Bodies.Coat k = PHOENIX;
		int gold = k.belly();
		BeastArt.Painter plumage = Bodies.feathers(k.base(), gold, 680);
		BeastArt.Painter breast = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN || f == BeastArt.Face.FRONT ? Bodies.noise(gold, f, x, y, 681, 0.08)
				: plumage.paint(f, x, y, w, h, d);
		BeastArt.Bone body = m.bone("body", null, 0, 9.5, 0);
		Bodies.cc(body, 0, 4, 0, 12, 11, 22, breast);
		// Its long neck and its head (its eyes bright, ringed with gold); its gold beak, hooked; its crest of fire.
		BeastArt.Bone head = m.bone("head", null, 0, 15, -10);
		Bodies.cc(head, 0, 11, -13, 6, 11, 6, plumage); // its neck, rising from its breast
		Bodies.cc(head, 0, 19, -19.5, 8, 8, 9, sideEyes2(k, Bodies.plain(BeastArt.mix(k.base(), k.light(), 0.2), 682), 2, 3));
		sideLids2(m, "head", 27, 8, 2, -24, 9, 4, 3, k.base());
		BeastArt.Painter beak = goldBeak(k.horn(), c(0x6A3A10), 684);
		Bodies.cc(head, 0, 21.5, -26.5, 3, 2.8, 5, beak);
		Bodies.cc(head, 0, 20, -29.5, 2, 2.6, 1.6, beak); // its hook, turned down
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 21.5, -24);
		Bodies.cc(jaw, 0, 20.6, -26, 2.4, 1, 4, beak);
		for (int i = 0; i < 3; i++) {
			BeastArt.Bone plume = m.bone("crest_" + i, "head", 0, 27, -18, 52 - i * 12, (i - 1) * 16, 0);
			plume.cube(-0.7, 26.5, -18, 1.4, 1.4, 10, Bodies.flameTip(k.glowing(), c(0xFFF0A0), 0.6, BeastArt.solid(k.base())));
			plume.cube(-1.1, 26.1, -9.5, 2.2, 2.2, 2.2, Bodies.aglow(c(0xFFF4C0), k.glowing(), 683 + i));
		}
		// The wings: crimson barred with gold within, gold out to the pinions, the trailing edge burning.
		BeastArt.Painter inner = pinions(k.base(), gold, k.glowing(), 686);
		BeastArt.Painter outer = pinions(BeastArt.mix(k.base(), gold, 0.45), k.dark(), c(0xFFE070), 687);
		for (int side : new int[] {1, -1}) {
			String n = side > 0 ? "wing_l" : "wing_r";
			BeastArt.Bone wing = m.bone(n, null, side * 6, 14.5, -4);
			wing.cube(side > 0 ? 6 : -30, 13.5, -7, 24, 2, 18, inner);
			BeastArt.Bone tip = m.bone(n + "_tip", n, side * 30, 14.5, -4);
			tip.cube(side > 0 ? 30 : -58, 13.75, -7, 28, 1.5, 13, outer);
		}
		// Five streamers fanned behind it, broad ribbons each its own colour, an eye of light near its end, its tip burning out (the end
		// its "_tip"). Drawn as thin rods at first (tour 14).
		for (int i = 0; i < STREAMERS.length; i++) {
			int colour = STREAMERS[i];
			BeastArt.Bone fan = m.bone("fan_" + i, null, 0, 10, 11, -8 - Math.abs(i - 2) * 3, (i - 2) * 13, 0);
			fan.cube(-2.5, 9.25, 11, 5, 1.5, 26, (f, x, y, w, h, d) -> Bodies.noise(BeastArt.mix(colour, gold, 0.15), f, x, y, 690, 0.1));
			BeastArt.Bone tip = m.bone("fan_" + i + "_tip", "fan_" + i, 0, 10, 37, 6, 0, 0);
			tip.cube(-3, 9.25, 37, 6, 1.5, 30, (f, x, y, w, h, d) -> {
				double t = 1 - BeastArt.frontness(f, x, y, w, h); // 0 where it joins its root, 1 at its end
				if (f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) {
					double along = t * (h - 1), r = Math.hypot((x - (w - 1) / 2.0) * 1.3, (along - (h - 1) * 0.68) * 0.7);
					if (r < 1.8) return glow(c(0xFFF8E0)); // the eye of light
					if (r < 3.4) return glow(BeastArt.mix(colour, c(0xFFFFFF), 0.45));
				}
				if (t > 0.82 || f == BeastArt.Face.BACK) return glow(BeastArt.mix(colour, c(0xFFE070), 0.5));
				return Bodies.noise(colour, f, x, y, 691, 0.1);
			});
		}
		// Its legs, gold, its talons dark.
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone leg = m.bone(side > 0 ? "leg_l" : "leg_r", null, side * 3, 5, 3);
			Bodies.cc(leg, side * 3, 0, 3, 2, 5.5, 2, BeastArt.solid(k.horn()));
			Bodies.cc(leg, side * 3, 0, 1.6, 3, 1, 4, BeastArt.solid(c(0x3A1A0A)));
		}
	}

	// --- Writing ---

	interface Build {
		void build(BeastArt.Model m);
	}

	static final Object[][] KINDS = {
			{"kun_peng_peng", (Build) Primordials::peng}, {"true_phoenix", (Build) Primordials::truePhoenix},
	};

	/** The two, under {@code assets}: the smallest square texture each one's boxes fit, its glow mask, its geometry, its "idle" animation. */
	static void writeAll(Path assets) throws Exception {
		for (Object[] kind : KINDS) {
			String id = (String) kind[0];
			BeastArt.Model m = new BeastArt.Model("demon_beast_" + id);
			((Build) kind[1]).build(m);
			BeastArt.Atlas atlas = null;
			for (int size = 64; size <= 1024; size *= 2) {
				BeastArt.Atlas tryAtlas = new BeastArt.Atlas(size, size);
				tryAtlas.models.add(m);
				try {
					tryAtlas.pack();
					atlas = tryAtlas;
					break;
				} catch (IllegalStateException full) {
					// next size up
				}
			}
			if (atlas == null) throw new IllegalStateException(id + " fits no texture");
			BufferedImage[] painted = Bodies.split(atlas.paint());
			BeastArt.write(assets.resolve("textures/entity/demon_beast/" + id + ".png"), painted[0]);
			Path mask = assets.resolve("textures/entity/demon_beast/" + id + "_glowmask.png");
			if (painted[1] != null) BeastArt.write(mask, painted[1]);
			else java.nio.file.Files.deleteIfExists(mask);
			BeastArt.write(assets.resolve("geo/demon_beast/" + id + ".geo.json"), BeastArt.geo(m, atlas));
			List<BeastArt.Anim> anims = Bodies.animations(m);
			BeastArt.write(assets.resolve("animations/demon_beast/" + id + ".animation.json"), BeastArt.animations(anims));
			System.out.println(id + ": " + atlas.width + " x " + atlas.height + ", " + m.bones.size() + " bones");
		}
	}
}
