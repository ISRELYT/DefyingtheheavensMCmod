import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The wild demon beasts' whole bodies for BeastArt (BeastKind, 2026-10-23): each kind's GeckoLib model (geo/demon_beast/<id>.geo.json),
 * its texture (textures/entity/demon_beast/<id>.png) and what of it glows in the dark (<id>_glowmask.png: GeckoLib's own glow layer),
 * and its "idle" animation (animations/demon_beast/<id>.animation.json: its breath and its blink). Units are pixels, the ground at y 0,
 * the face toward -z, sized to the kind's least rank (the game grows it with its rank).
 * <p>
 * The bones the client moves (BeastModels): "body"; "head" (after a serpent's "neck_*"), its "jaw" and "lids"; a beast's legs "leg_fl",
 * "leg_fr", "leg_bl", "leg_br", an ape's "arm_l", "arm_r", "leg_l", "leg_r", a bird's "leg_l", "leg_r"; a bird's "wing_l", "wing_r" and
 * their "_tip"s; a tail's "tail_0".. in a chain; a serpent's "seg_0".. in a chain; a fox's tails "fan_0".."fan_2"; a turtle's "shell".
 * A painter marks a pixel as glowing by {@link #glow} (alpha 254), which {@link #split} lifts onto the glow mask.
 */
public class Bodies {
	static int c(int hex) { return BeastArt.rgb(hex); }

	/** A glowing pixel of colour {@code hex}. */
	static int glow(int hex) { return 0xFE000000 | (hex & 0xFFFFFF); }

	static final int BLACK = c(0x0A0A0A), WHITE = c(0xF4F0E4);

	/** A coat: its fur or scales, darker (stripes, socks, the back), lighter, the belly, the eyes, what glows on it, horn or claw. */
	record Coat(int base, int dark, int light, int belly, int eye, int glowing, int horn) {}

	// --- Painters ---

	static int noise(int c, BeastArt.Face f, int x, int y, int seed, double by) {
		return BeastArt.shade(c, (BeastArt.hash(x + f.ordinal() * 31, y + f.ordinal() * 7, seed) - 0.5) * by);
	}

	/** Fur: the back a little darker, the belly underneath, the lower flanks toward it. */
	static BeastArt.Painter fur(Coat k, int seed) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return noise(k.belly, f, x, y, seed, 0.14);
			if (f == BeastArt.Face.UP) return noise(BeastArt.mix(k.base, k.dark, 0.25), f, x, y, seed, 0.16);
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && h > 3 && y >= h - Math.max(1, h / 4)) return noise(BeastArt.mix(k.base, k.belly, 0.6), f, x, y, seed, 0.14);
			return noise(k.base, f, x, y, seed, 0.16);
		};
	}

	/** Plain colour, a little uneven. */
	static BeastArt.Painter plain(int colour, int seed) {
		return (f, x, y, w, h, d) -> noise(colour, f, x, y, seed, 0.12);
	}

	/** A tiger's stripes, wavering bands across the length of the fur, over {@code under}. */
	static BeastArt.Painter stripes(Coat k, BeastArt.Painter under, int every, int seed) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return under.paint(f, x, y, w, h, d);
			int along = f == BeastArt.Face.UP ? y : f == BeastArt.Face.FRONT || f == BeastArt.Face.BACK ? y : x;
			int across = f == BeastArt.Face.UP ? x : f == BeastArt.Face.FRONT || f == BeastArt.Face.BACK ? x : y;
			double wobble = Math.sin(across * 1.1 + seed) * 0.9;
			boolean belly = (f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y >= h - Math.max(1, h / 4);
			if (!belly && Math.floorMod((int) Math.floor(along + wobble + seed), every) == 0 && BeastArt.hash(x + f.ordinal() * 9, y, seed) < 0.88) return k.dark;
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** Scales: a lattice of darker edges, now and then a lighter one; the belly in broad plates. */
	static BeastArt.Painter scales(Coat k, int seed) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return (y % 2 == 0) ? BeastArt.mix(k.belly, k.dark, 0.25) : noise(k.belly, f, x, y, seed, 0.08);
			boolean edge = Math.floorMod(x + (Math.floorMod(y, 4) < 2 ? 0 : 1), 2) == 0 && y % 2 == 0;
			int col = edge ? BeastArt.mix(k.base, k.dark, 0.5) : k.base;
			if (BeastArt.hash(x + f.ordinal() * 13, y, seed) < 0.1) col = BeastArt.mix(col, k.light, 0.5);
			return col;
		};
	}

	/** A serpent's back: diamonds of the darker colour down its spine, edged in the lighter, over scales. */
	static BeastArt.Painter diamonds(Coat k, int seed) {
		BeastArt.Painter scale = scales(k, seed);
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.UP) {
				double cx = (w - 1) / 2.0, period = Math.max(4, w);
				double py = Math.floorMod(y + seed, (int) period) - period / 2.0;
				double r = Math.abs(x - cx) / Math.max(1, w / 2.0) + Math.abs(py) / (period / 2.0);
				if (r < 0.55) return k.dark;
				if (r < 0.75) return k.light;
			}
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 0 && Math.floorMod(x + seed, 4) < 2) return k.dark;
			return scale.paint(f, x, y, w, h, d);
		};
	}

	/** Plates (iron hide, a rhino's armour, a crocodile's scutes): broad plates in {@code plate}, darker seams, lit at their top edge. */
	static BeastArt.Painter plates(int plate, int seam, int w0, int h0, BeastArt.Painter under, int seed) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return under.paint(f, x, y, w, h, d);
			int ox = Math.floorMod(y / h0, 2) * (w0 / 2);
			int px = Math.floorMod(x + ox, w0), py = Math.floorMod(y, h0);
			if (px == 0 || py == 0) return seam;
			if (py == 1) return BeastArt.shade(plate, 0.18);
			return noise(plate, f, x, y, seed, 0.1);
		};
	}

	/** Thick hide in folds (a rhino's): wavering lines of {@code line} across it every {@code every} rows, over {@code under}. */
	static BeastArt.Painter folds(int base, int line, int every, BeastArt.Painter under, int seed) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return under.paint(f, x, y, w, h, d);
			int along = f == BeastArt.Face.UP ? y : x, across = f == BeastArt.Face.UP ? x : y;
			int wave = (int) Math.round(Math.sin(along * 0.45 + seed) * 1.3 + Math.sin(along * 0.17 + seed * 2) * 0.8);
			if (Math.floorMod(across + wave, every) == 0 && BeastArt.hash(x + f.ordinal() * 7, y, seed) < 0.9) return BeastArt.mix(base, line, 0.55);
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** Stone: uneven grey, cracked, mossed in patches if {@code moss} is not 0. */
	static BeastArt.Painter rock(int stone, int crack, int moss, int seed) {
		return (f, x, y, w, h, d) -> {
			int px = x + f.ordinal() * 37, py = y;
			double low = BeastArt.hash(px / 3, py / 3, seed + 5);
			if (moss != 0 && f == BeastArt.Face.UP && low < 0.35) return noise(moss, f, x, y, seed, 0.2);
			if (moss != 0 && f != BeastArt.Face.DOWN && py <= 1 && low < 0.45) return noise(moss, f, x, y, seed, 0.2);
			if (BeastArt.hash(px, py, seed) < 0.07 || BeastArt.hash(px + 1, py / 2, seed + 3) < 0.04) return crack;
			return noise(stone, f, x, y, seed, 0.28);
		};
	}

	/** Feathers in rows, each row's lower edge darker, the rows set off from one another. */
	static BeastArt.Painter feathers(int base, int edge, int seed) {
		return (f, x, y, w, h, d) -> {
			int row = y / 2;
			int col = Math.floorMod(x + (row % 2) * 2, 4);
			if (y % 2 == 1 && col != 0) return BeastArt.mix(base, edge, 0.45);
			return noise(base, f, x, y, seed, 0.12);
		};
	}

	/** Lightning across {@code under}: zig-zag streaks along the sides, glowing. */
	static BeastArt.Painter lightning(int spark, BeastArt.Painter under, int seed) {
		return (f, x, y, w, h, d) -> {
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B || f == BeastArt.Face.UP) && w > 4) {
				int zig = (int) Math.round(h * 0.4 + (Math.floorMod(x + seed, 4) < 2 ? Math.floorMod(x + seed, 4) : 4 - Math.floorMod(x + seed, 4)) - 1);
				if (y == zig && Math.floorMod(x + seed * 3, 11) < 7) return glow(spark);
			}
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** Toward the end of a part ({@code fromEnd} pixels of it, at its back face's end), fire: deepening to glowing gold at the tip. */
	static BeastArt.Painter flameTip(int flame, int core, double share, BeastArt.Painter under) {
		return (f, x, y, w, h, d) -> {
			double t = 1 - BeastArt.frontness(f, x, y, w, h); // 1 at the back (a tail's tip)
			if (f == BeastArt.Face.BACK || t > 1 - share) {
				double into = f == BeastArt.Face.BACK ? 1 : (t - (1 - share)) / share;
				return glow(BeastArt.mix(flame, core, into));
			}
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** Over {@code under}, eyes on its front face at {@code row}, {@code inset} from either edge, {@code width} across (the inner pupil black). */
	static BeastArt.Painter eyes(Coat k, BeastArt.Painter under, int row, int inset, int width, boolean glowing) {
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.FRONT && y == row) {
				int l = inset, r = w - 1 - inset;
				if (x >= l && x < l + width || x <= r && x > r - width) {
					if (width > 1 && (x == l + width - 1 || x == r - width + 1)) return BLACK;
					return glowing ? glow(k.eye) : k.eye;
				}
			}
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** Over {@code under}, an eye on each side face at {@code row}, at the front edge ({@code width} along it). */
	static BeastArt.Painter sideEyes(Coat k, BeastArt.Painter under, int row, int width, boolean glowing) {
		return (f, x, y, w, h, d) -> {
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == row) {
				int front = f == BeastArt.Face.SIDE_A ? w - 1 - x : x; // 0 at the front edge
				if (front >= 1 && front <= width) return front == width && width > 1 ? BLACK : glowing ? glow(k.eye) : k.eye;
			}
			return under.paint(f, x, y, w, h, d);
		};
	}

	/** A hoof's or a paw's colour on a leg's lowest {@code rows}. */
	static BeastArt.Painter shod(int hoof, int rows, BeastArt.Painter under) {
		return (f, x, y, w, h, d) -> f != BeastArt.Face.UP && (f == BeastArt.Face.DOWN || y >= h - rows) ? hoof : under.paint(f, x, y, w, h, d);
	}

	/** A face's front only: {@code front} there, {@code under} elsewhere. */
	static BeastArt.Painter fronted(BeastArt.Painter front, BeastArt.Painter under) {
		return (f, x, y, w, h, d) -> (f == BeastArt.Face.FRONT ? front : under).paint(f, x, y, w, h, d);
	}

	// --- Pieces ---

	/** A cube centred on {@code cx}, {@code cz}, its bottom at {@code y}. */
	static BeastArt.Cube cc(BeastArt.Bone b, double cx, double y, double cz, double w, double h, double d, BeastArt.Painter p) {
		return b.cube(cx - w / 2, y, cz - d / 2, w, h, d, p);
	}

	/** How tall a row of a face's texture stands on a cube {@code size} tall (GeckoLib maps the floor of its size over it). */
	static double rowOf(double size) {
		return size / Math.max(1, Math.floor(size + 1e-6));
	}

	/**
	 * Lids before the eyes {@link #eyes} paints on a head cube's front face (its top at {@code top}, {@code h} tall, its front at {@code z}):
	 * a strip over the face's row {@code row}, {@code w} across, shut only as it blinks (the "idle" animation scales it from nothing).
	 */
	static void lids(BeastArt.Model m, String head, double top, double h, int row, double z, double w, int colour) {
		double px = rowOf(h), y = top - (row + 1) * px;
		BeastArt.Bone lids = m.bone("lids", head, 0, y + px, z - 0.1);
		lids.cube(-w / 2, y, z - 0.25, w, px, 0.2, BeastArt.solid(colour));
	}

	/**
	 * Lids over the eyes {@link #sideEyes} paints on a head cube's sides (its top at {@code top}, {@code h} tall, its front at {@code z},
	 * {@code d} deep, {@code halfWidth} out from the middle): over the row {@code row}, {@code width} pixels from the front edge's second.
	 */
	static void sideLids(BeastArt.Model m, String head, double top, double h, int row, double z, double d, double halfWidth, int width, int colour) {
		double py = rowOf(h), pz = rowOf(d), y = top - (row + 1) * py;
		BeastArt.Bone lids = m.bone("lids", head, 0, y + py, z);
		double from = z + pz - 0.05, length = width * pz + 0.1;
		lids.cube(halfWidth, y, from, 0.2, py, length, BeastArt.solid(colour));
		lids.cube(-halfWidth - 0.2, y, from, 0.2, py, length, BeastArt.solid(colour));
	}

	/** Four legs, each {@code w} square and {@code h} tall, under a body {@code bw} wide and {@code bl} long, a little inside its corners. */
	static void legs(BeastArt.Model m, double bw, double bl, double w, double h, double inX, double inZ, BeastArt.Painter p) {
		double x = bw / 2 - w / 2 - inX, zf = -bl / 2 + w / 2 + inZ, zb = bl / 2 - w / 2 - inZ;
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[][] at = {{x, zf}, {-x, zf}, {x, zb}, {-x, zb}};
		BeastArt.Cube first = null;
		for (int i = 0; i < 4; i++) {
			BeastArt.Bone leg = m.bone(names[i], null, at[i][0], h, at[i][1]);
			if (first == null) first = cc(leg, at[i][0], 0, at[i][1], w, h + 1, w, p);
			else leg.cubeLike(first, at[i][0] - w / 2, 0, at[i][1] - w / 2);
		}
	}

	/**
	 * A tail in pieces from ({@code y}, {@code z}) going back (+z), each turned {@code bend[i]} degrees about x from the one before (Bedrock's:
	 * positive lifts its far end), {@code thick[i]} square and {@code length[i]} long, painted by {@code paint(i)}.
	 */
	static void tail(BeastArt.Model m, String prefix, String parent, double y, double z, double[] bend, double[] thick, double[] length,
			java.util.function.IntFunction<BeastArt.Painter> paint) {
		String up = parent;
		for (int i = 0; i < bend.length; i++) {
			BeastArt.Bone piece = m.bone(prefix + i, up, 0, y, z, bend[i], 0, 0);
			double t = thick[i];
			piece.cube(-t / 2, y - t / 2, z - 0.5, t, t, length[i] + 0.5, paint.apply(i));
			up = piece.name;
			z += length[i];
		}
	}

	/**
	 * A serpent's body lying on the ground behind {@code z}: {@code thick.length} pieces in a chain ("seg_0" first, behind the head), each
	 * as wide as it is thick, a little flatter, and {@code length} long.
	 */
	static void segments(BeastArt.Model m, double z, double[] thick, double length, java.util.function.IntFunction<BeastArt.Painter> paint) {
		String up = null;
		for (int i = 0; i < thick.length; i++) {
			double t = thick[i], h = Math.max(1, t * 0.8);
			BeastArt.Bone seg = m.bone("seg_" + i, up, 0, h / 2, z);
			seg.cube(-t / 2, 0, z - 0.5, t, h, length + 0.5, paint.apply(i));
			up = seg.name;
			z += length;
		}
	}

	// --- The common beasts ---

	static final Coat BOAR = new Coat(c(0x5A4636), c(0x2E2219), c(0x8A7660), c(0x6A5444), c(0xE83A2A), c(0xE83A2A), c(0xF0E8D0));

	static void ironHideBoar(BeastArt.Model m) {
		Coat k = BOAR;
		int iron = c(0x8A9098);
		BeastArt.Bone body = m.bone("body", null, 0, 9.5, 0);
		// Iron plates grown over its back and upper flanks, bristly hide below them.
		BeastArt.Painter plated = plates(iron, c(0x62666C), 7, 4, fur(k, 1), 2), hide = (f, x, y, w, h, d) ->
				f == BeastArt.Face.UP || (f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y < h * 0.55 || f == BeastArt.Face.BACK && y < h * 0.4
						? plated.paint(f, x, y, w, h, d) : fur(k, 1).paint(f, x, y, w, h, d);
		cc(body, 0, 5, 0, 12, 9, 18, hide);
		// The bristles down its back, dark and stiff.
		cc(body, 0, 14, -1, 3, 2, 14, (f, x, y, w, h, d) -> noise(k.dark, f, x * 2, y, 3, 0.3));
		cc(body, 0, 13, -6.5, 10, 3, 6, plain(BeastArt.mix(k.base, k.dark, 0.3), 4)); // the shoulders' hump
		legs(m, 12, 18, 3, 5, 0.5, 1, shod(c(0x1E1814), 1, fur(k, 5)));
		BeastArt.Bone head = m.bone("head", null, 0, 11, -9);
		cc(head, 0, 6, -12.5, 9, 8, 7, eyes(k, fur(k, 6), 2, 1, 1, true));
		// The snout's disc, pink, its nostrils; tusks curving up from its sides.
		cc(head, 0, 6.5, -17, 6, 5, 3, fronted((f, x, y, w, h, d) -> (y == 2 && (x == 1 || x == 4)) ? c(0x3A1E1E) : c(0xC88A7A), fur(k, 7)));
		BeastArt.Painter tusk = (f, x, y, w, h, d) -> BeastArt.mix(k.horn, c(0xA89870), y / 3.0);
		head.cube(-4, 8, -18.6, 1, 3, 1, tusk);
		head.cube(3, 8, -18.6, 1, 3, 1, tusk);
		head.cube(-4, 10.5, -18.2, 1, 1, 1, BeastArt.solid(k.horn));
		head.cube(3, 10.5, -18.2, 1, 1, 1, BeastArt.solid(k.horn));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 6.5, -12);
		cc(jaw, 0, 5.2, -15, 6, 1.3, 5, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0x8A3A3A) : noise(k.belly, f, x, y, 8, 0.1));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 3.5, 14, -11, 20, 0, side * -25);
			cc(ear, side * 3.5, 14, -11, 2.5, 2, 1, plain(k.dark, 9));
		}
		lids(m, "head", 14, 8, 2, -16, 7, k.base);
		tail(m, "tail_", null, 12.5, 9, new double[] {-50, 30, 40}, new double[] {1, 1, 1}, new double[] {2, 1.5, 1.5}, i -> plain(k.dark, 10 + i));
	}

	static final Coat WOLF = new Coat(c(0x26222E), c(0x121016), c(0x4A4256), c(0x3A3444), c(0xB07AF8), c(0x8A5AD8), c(0xE8E0F0));

	static void shadowWolf(BeastArt.Model m) {
		Coat k = WOLF;
		// Shadow wisps down its back and flanks, faintly glowing violet.
		BeastArt.Painter coat = (f, x, y, w, h, d) -> {
			if ((f == BeastArt.Face.UP || f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && w > 4
					&& Math.floorMod(x * 2 + y * 3 + (int) (BeastArt.hash(x / 3, y / 2, 21) * 5), 9) == 0) return glow(BeastArt.mix(k.glowing, k.base, 0.35));
			return fur(k, 20).paint(f, x, y, w, h, d);
		};
		BeastArt.Bone body = m.bone("body", null, 0, 10.5, 0);
		cc(body, 0, 7, 0, 7, 7, 15, coat);
		cc(body, 0, 6.5, -5, 8.5, 8.5, 5, plain(BeastArt.mix(k.base, k.light, 0.25), 22)); // the ruff about its neck
		legs(m, 7, 15, 2.5, 7, 0.2, 0.8, shod(k.dark, 2, fur(k, 23)));
		BeastArt.Bone head = m.bone("head", null, 0, 13, -7.5);
		cc(head, 0, 11, -10, 6, 6, 5, eyes(k, fur(k, 24), 2, 0, 2, true));
		cc(head, 0, 11, -14, 3, 3, 3.5, fronted((f, x, y, w, h, d) -> y == 0 && x == 1 ? BLACK : noise(k.light, f, x, y, 25, 0.1), fur(k, 26)));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 11.5, -12.5);
		cc(jaw, 0, 10.2, -14, 2.6, 1, 3.2, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0x6A2A4A) : noise(k.light, f, x, y, 27, 0.1));
		head.cube(-1.2, 10.4, -15.6, 0.6, 1, 0.6, BeastArt.solid(WHITE));
		head.cube(0.6, 10.4, -15.6, 0.6, 1, 0.6, BeastArt.solid(WHITE));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 2, 17, -9, 0, 0, side * -10);
			cc(ear, side * 2, 17, -9, 2, 3, 1, (f, x, y, w, h, d) -> f == BeastArt.Face.FRONT ? c(0x4A2A5A) : noise(k.dark, f, x, y, 28, 0.1));
		}
		lids(m, "head", 17, 6, 2, -12.5, 6, k.base);
		tail(m, "tail_", null, 13, 7.5, new double[] {-35, -10, 10}, new double[] {2.2, 3, 2.4}, new double[] {3, 4, 3},
				i -> i == 2 ? flameTip(k.glowing, c(0xE0C8FF), 0.4, fur(k, 30)) : fur(k, 29 + i));
	}

	static final Coat VIPER = new Coat(c(0x2E8A4A), c(0x14502A), c(0x8ACB80), c(0xD8E06A), c(0xF0D030), c(0xF0D030), WHITE);

	static void jadeViper(BeastArt.Model m) {
		Coat k = VIPER;
		segments(m, 0, new double[] {3.2, 3.4, 3.2, 3, 2.6, 2.2, 1.8, 1.3}, 3.5, i -> diamonds(k, 40 + i));
		// The neck rising from the body, the head level on it: wide at the back, narrowing to the snout (a viper's).
		BeastArt.Bone neck = m.bone("neck_0", null, 0, 1.5, 0.5, -35, 0, 0);
		neck.cube(-1.4, 0, -3.5, 2.8, 2.6, 4, diamonds(k, 48));
		BeastArt.Bone head = m.bone("head", "neck_0", 0, 1.5, -3.5, 35, 0, 0);
		head.cube(-2.5, 0, -7, 5, 2.4, 4, sideEyes(k, diamonds(k, 49), 0, 1, true));
		head.cube(-1.5, 0.2, -9, 3, 1.8, 2.2, scales(k, 50));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 0.4, -6.5);
		jaw.cube(-1.4, -0.4, -8.8, 2.8, 0.6, 3.2, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0xC8506A) : noise(k.belly, f, x, y, 51, 0.1));
		head.cube(-1.1, -0.6, -8.4, 0.4, 0.8, 0.4, BeastArt.solid(WHITE)); // fangs
		head.cube(0.7, -0.6, -8.4, 0.4, 0.8, 0.4, BeastArt.solid(WHITE));
		BeastArt.Bone tongue = m.bone("tongue", "head", 0, 0.6, -9);
		tongue.cube(-0.3, 0.6, -11, 0.6, 0, 2, BeastArt.solid(c(0xD8384A)));
		sideLids(m, "head", 2.4, 2.4, 0, -7, 4, 2.5, 1, k.base);
	}

	static final Coat TURTLE = new Coat(c(0x6A6A62), c(0x3A3A36), c(0x9A9A8E), c(0xC8C090), BLACK, c(0x7AD8A8), c(0x2E2A22));

	static void rockShellTurtle(BeastArt.Model m) {
		Coat k = TURTLE;
		int skin = c(0x7A8A5A), skinDark = c(0x4A5A34), moss = c(0x5A8A3A);
		BeastArt.Painter shell = rock(k.base, k.dark, moss, 60);
		BeastArt.Bone body = m.bone("shell", null, 0, 6, 0);
		cc(body, 0, 2, 0, 13, 2, 15, plain(k.belly, 61)); // the plastron beneath
		cc(body, 0, 3.5, 0, 15, 4, 17, shell);
		cc(body, 0, 7.5, 0, 12, 2, 13, shell);
		cc(body, 0, 9.5, 0, 8, 1.5, 9, shell);
		// Moss and a few water-worn stones on top; one stone faintly glowing with the water's qi.
		cc(body, 2.5, 11, 1, 2, 1, 3, rock(k.light, k.dark, 0, 62));
		cc(body, -2, 11, -1.5, 2, 1, 2, (f, x, y, w, h, d) -> glow(BeastArt.mix(k.glowing, WHITE, BeastArt.hash(x, y, 63) * 0.4)));
		BeastArt.Bone head = m.bone("head", null, 0, 5, -8);
		BeastArt.Painter skinP = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN ? noise(k.belly, f, x, y, 64, 0.1)
				: Math.floorMod(x * 3 + y * 5, 7) == 0 ? skinDark : noise(skin, f, x, y, 64, 0.14);
		cc(head, 0, 3.5, -10, 3.5, 3, 4, skinP); // the neck
		cc(head, 0, 4, -13.5, 4.5, 4, 4, sideEyes(k, skinP, 1, 1, false));
		head.cube(-1.5, 3.8, -16, 3, 1.2, 0.6, BeastArt.solid(c(0x2E2A22))); // the beak's edge
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 4.2, -12);
		cc(jaw, 0, 3.4, -13.8, 3.8, 0.8, 3.4, skinP);
		sideLids(m, "head", 8, 4, 1, -15.5, 4, 2.25, 1, skin);
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[][] at = {{5.5, -5.5}, {-5.5, -5.5}, {5.5, 6}, {-5.5, 6}};
		BeastArt.Cube first = null;
		for (int i = 0; i < 4; i++) {
			BeastArt.Bone leg = m.bone(names[i], null, at[i][0], 3, at[i][1]);
			BeastArt.Painter clawed = shod(k.horn, 1, skinP);
			if (first == null) first = cc(leg, at[i][0], 0, at[i][1], 4, 3.5, 4, clawed);
			else leg.cubeLike(first, at[i][0] - 2, 0, at[i][1] - 2);
		}
		tail(m, "tail_", null, 3, 8, new double[] {-15}, new double[] {1.6}, new double[] {3}, i -> skinP);
	}

	static final Coat HAWK = new Coat(c(0x5A6A82), c(0x2A3242), c(0xE8E8F0), c(0xE8E8F0), c(0xF0D030), c(0xBFE8FF), c(0xE8C040));

	/**
	 * A bird of prey: body, head with a hooked beak, wings spread level ({@code span} and {@code tipSpan} across, {@code chord} deep) that
	 * the client folds on the ground and beats in the air, a fan of a tail, legs and talons. {@code s} scales every measure.
	 */
	static void bird(BeastArt.Model m, Coat k, double s, int seed, boolean crest) {
		BeastArt.Painter plumage = feathers(k.base, k.dark, seed);
		BeastArt.Painter breast = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN || f == BeastArt.Face.FRONT ? noise(k.belly, f, x, y, seed + 1, 0.12)
				: plumage.paint(f, x, y, w, h, d);
		double legH = 3 * s, bw = 5 * s, bh = 5 * s, bl = 8 * s;
		BeastArt.Bone body = m.bone("body", null, 0, legH + bh / 2, 0);
		cc(body, 0, legH, 0, bw, bh, bl, breast);
		BeastArt.Bone head = m.bone("head", null, 0, legH + bh, -bl / 2 + 1 * s);
		double hs = 4 * s;
		int eyeRow = (int) Math.round(Math.floor(hs) * 0.35), eyeWidth = (int) Math.max(1, Math.round(s));
		cc(head, 0, legH + bh - 1 * s, -bl / 2 - hs / 2 + 1.5 * s, hs, hs, hs, sideEyes(k, plain(k.light, seed + 2), eyeRow, eyeWidth, true));
		// The beak: hooked, gold darkening to its tip.
		BeastArt.Painter beak = (f, x, y, w, h, d) -> BeastArt.mix(k.horn, c(0x3A2A10), BeastArt.frontness(f, x, y, w, h) * 0.6);
		double beakZ = -bl / 2 - hs + 1.5 * s;
		cc(head, 0, legH + bh + 0.6 * s, beakZ - 1 * s, 1.8 * s, 1.6 * s, 2 * s, beak);
		cc(head, 0, legH + bh - 0.2 * s, beakZ - 1.8 * s, 1.2 * s, 1.2 * s, 0.8 * s, beak);
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, legH + bh, beakZ);
		cc(jaw, 0, legH + bh - 0.2 * s, beakZ - 0.8 * s, 1.4 * s, 0.6 * s, 1.4 * s, beak);
		if (crest) {
			BeastArt.Bone plume = m.bone("crest", "head", 0, legH + bh + hs - 1 * s, -bl / 2 + 1.5 * s, -30, 0, 0);
			cc(plume, 0, legH + bh + hs - 1.2 * s, -bl / 2 + 2.5 * s, 1 * s, 2.5 * s, 4 * s, (f, x, y, w, h, d) -> glow(BeastArt.mix(k.glowing, k.base, y / (double) Math.max(1, h))));
		}
		sideLids(m, "head", legH + bh - 1 * s + hs, hs, eyeRow, -bl / 2 - hs + 1.5 * s, hs, hs / 2, eyeWidth, k.base);
		// The wings.
		double span = 6 * s, tip = 6 * s, chord = 6 * s, tipChord = 4.5 * s, wy = legH + bh - 1 * s, wz = -bl / 2 + 1 * s;
		BeastArt.Painter wing = (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.DOWN) return noise(BeastArt.mix(k.light, k.base, 0.4), f, x, y, seed + 3, 0.1);
			return Math.floorMod(y + x / 3, 3) == 0 ? BeastArt.mix(k.base, k.dark, 0.5) : noise(k.base, f, x, y, seed + 4, 0.12);
		};
		BeastArt.Painter wingTip = (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.UP && Math.floorMod(x, 3) == 0) return k.dark; // the flight feathers' edges
			if (f == BeastArt.Face.UP && y >= h - 1) return glow(BeastArt.mix(k.glowing, k.base, 0.3));
			return wing.paint(f, x, y, w, h, d);
		};
		for (int side : new int[] {1, -1}) {
			String n = side > 0 ? "wing_l" : "wing_r";
			BeastArt.Bone inner = m.bone(n, null, side * bw / 2, wy + 0.5 * s, wz);
			inner.cube(side > 0 ? bw / 2 : -bw / 2 - span, wy, wz, span, 1 * s, chord, wing);
			BeastArt.Bone outer = m.bone(n + "_tip", n, side * (bw / 2 + span), wy + 0.5 * s, wz);
			outer.cube(side > 0 ? bw / 2 + span : -bw / 2 - span - tip, wy + 0.1 * s, wz, tip, 0.8 * s, tipChord, wingTip);
		}
		BeastArt.Bone tail = m.bone("tail_0", null, 0, legH + bh * 0.7, bl / 2, -10, 0, 0);
		cc(tail, 0, legH + bh * 0.7 - 0.5 * s, bl / 2 + 2.5 * s, 4 * s, 1 * s, 5.5 * s, (f, x, y, w, h, d) -> Math.floorMod(y, 3) == 0 ? k.dark : noise(k.base, f, x, y, seed + 5, 0.12));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone leg = m.bone(side > 0 ? "leg_l" : "leg_r", null, side * 1.4 * s, legH, 0.5 * s);
			cc(leg, side * 1.4 * s, 0, 0.5 * s, 1 * s, legH + 0.5, 1 * s, BeastArt.solid(k.horn));
			cc(leg, side * 1.4 * s, 0, -0.3 * s, 1.6 * s, 0.6 * s, 2.2 * s, BeastArt.solid(c(0x2A2218))); // the talons
		}
	}

	static void windHawk(BeastArt.Model m) { bird(m, HAWK, 1, 70, false); }

	static final Coat MONKEY = new Coat(c(0xC8481E), c(0x7A2410), c(0xF0C850), c(0xE88A50), c(0xFFE070), c(0xFFB020), c(0x3A1A10));

	static void fireMonkey(BeastArt.Model m) {
		Coat k = MONKEY;
		BeastArt.Painter fur = fur(k, 80);
		int face = c(0xF0B080);
		BeastArt.Bone body = m.bone("body", null, 0, 5, 0);
		cc(body, 0, 5, 0.5, 5, 3, 4, fur); // the hips
		cc(body, 0, 8, -0.3, 6, 5, 4.4, fur); // the chest
		BeastArt.Bone head = m.bone("head", "body", 0, 13, -0.5);
		cc(head, 0, 12.5, -0.8, 5, 5, 5, fur);
		// The face: pale and hairless, ringed in gold fur; its eyes burning.
		cc(head, 0, 12.8, -3.5, 4, 3, 0.6, (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.FRONT && y == 1 && (x == 0 || x == 3)) return glow(k.eye);
			if (f == BeastArt.Face.FRONT && y == 2 && x >= 1 && x <= 2) return c(0x5A2A1A);
			return noise(face, f, x, y, 81, 0.08);
		});
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 12.8, -3);
		cc(jaw, 0, 12.2, -3.4, 2.4, 0.8, 1, plain(face, 82));
		cc(head, 0, 16.5, -0.8, 3, 1.5, 3, (f, x, y, w, h, d) -> glow(BeastArt.mix(k.glowing, c(0xFFF0A0), BeastArt.hash(x, y, 83) * 0.6))); // a flame on its crown
		for (int side : new int[] {1, -1}) cc(head, side * 3, 14, -0.5, 1, 1.8, 1.5, plain(face, 84)); // the ears
		lids(m, "head", 15.8, 3, 1, -3.8, 4, face);
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone arm = m.bone(side > 0 ? "arm_l" : "arm_r", "body", side * 4, 12.5, -0.3);
			cc(arm, side * 4, 3, -0.3, 2, 10, 2, (f, x, y, w, h, d) -> y >= h - 2 ? glow(BeastArt.mix(k.glowing, k.base, 0.3)) : fur.paint(f, x, y, w, h, d));
			BeastArt.Bone leg = m.bone(side > 0 ? "leg_l" : "leg_r", null, side * 1.5, 5.5, 0.5);
			cc(leg, side * 1.5, 0, 0.5, 2, 6, 2.2, shod(k.dark, 1, fur));
		}
		// The long tail curling up behind, its end aflame.
		tail(m, "tail_", "body", 6, 2.5, new double[] {-20, 30, 35, 40, 35}, new double[] {1.2, 1.2, 1.1, 1, 1}, new double[] {3, 3, 3, 2.5, 2.5},
				i -> i == 4 ? flameTip(k.glowing, c(0xFFF0A0), 1, fur) : plain(k.base, 85 + i));
	}

	// --- The uncommon beasts ---

	static final Coat TIGER = new Coat(c(0xB8321E), c(0x1A1010), c(0xE8D0B0), c(0xE8D0B0), c(0xFFD040), c(0xFF4030), c(0xF4ECDC));

	static void bloodfangTiger(BeastArt.Model m) {
		Coat k = TIGER;
		BeastArt.Painter coat = stripes(k, fur(k, 90), 4, 91);
		BeastArt.Bone body = m.bone("body", null, 0, 14, 0);
		cc(body, 0, 9, 0, 11, 10, 24, coat);
		legs(m, 11, 24, 4, 9, 0, 1, shod(c(0x2A1E1A), 1, stripes(k, fur(k, 92), 3, 93)));
		BeastArt.Bone head = m.bone("head", null, 0, 16, -12);
		cc(head, 0, 12.5, -16, 10, 9, 9, eyes(k, stripes(k, fronted(fur(new Coat(k.base, k.dark, k.light, k.light, k.eye, k.glowing, k.horn), 94), fur(k, 94)), 3, 94),
				3, 1, 2, true));
		cc(head, 0, 13, -21.5, 6, 4, 3, fronted((f, x, y, w, h, d) -> y == 0 && x >= 2 && x <= 3 ? c(0x5A2A2A) : noise(k.light, f, x, y, 95, 0.1), plain(k.light, 96)));
		for (int side : new int[] {1, -1}) cc(head, side * 5.6, 12.5, -16.5, 1.4, 4.5, 5, plain(k.light, 97)); // the cheek ruffs
		// The blood fangs: long, ivory, reddening to their points.
		BeastArt.Painter fang = (f, x, y, w, h, d) -> y >= h - 1 ? glow(k.glowing) : BeastArt.mix(k.horn, c(0xC83A2A), y / (double) Math.max(1, h));
		head.cube(-2.5, 9.5, -22.6, 1, 4, 1, fang);
		head.cube(1.5, 9.5, -22.6, 1, 4, 1, fang);
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 13.2, -16);
		cc(jaw, 0, 11.8, -20.5, 5, 1.5, 4, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0x8A2020) : noise(k.light, f, x, y, 98, 0.1));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 3.5, 21.5, -14, 0, 0, side * -12);
			cc(ear, side * 3.5, 21.5, -14, 2.5, 2.2, 1, (f, x, y, w, h, d) -> f == BeastArt.Face.BACK ? (y == 1 ? k.light : k.dark) : plain(k.base, 99).paint(f, x, y, w, h, d));
		}
		lids(m, "head", 21.5, 9, 3, -20.5, 8, k.base);
		tail(m, "tail_", null, 18, 12, new double[] {-40, -20, 20, 25}, new double[] {2.6, 2.4, 2.2, 2.2}, new double[] {6, 6, 5, 4},
				i -> i == 3 ? plain(k.dark, 100) : stripes(k, fur(k, 101 + i), 3, 102 + i));
	}

	static final Coat FROST = new Coat(c(0xE8F0F4), c(0x8AA8C0), c(0xFFFFFF), c(0xC8D8E4), c(0x6AD0FF), c(0x9AE0FF), c(0x3A4A5A));

	static void frostApe(BeastArt.Model m) {
		Coat k = FROST;
		BeastArt.Painter fur = fur(k, 110);
		int skin = c(0x5A7A9A);
		BeastArt.Painter ice = (f, x, y, w, h, d) -> glow(BeastArt.mix(k.glowing, c(0xFFFFFF), BeastArt.hash(x + f.ordinal() * 5, y, 111) * 0.5));
		BeastArt.Bone body = m.bone("body", null, 0, 8, 0);
		cc(body, 0, 8, 1, 12, 6, 8, fur); // the hips
		cc(body, 0, 14, -1, 16, 9, 11, fur); // the great chest and shoulders
		cc(body, 0, 15, -6.6, 10, 6, 0.8, plain(BeastArt.mix(skin, k.light, 0.35), 112)); // the bare chest
		// Ice grown on its shoulders and back.
		cc(body, 6, 23, 0, 3, 3, 3, ice);
		cc(body, -6, 23, -1, 2, 4, 2, ice);
		cc(body, 0, 23, 2, 2, 2.5, 2, ice);
		cc(body, 3, 23, 3.5, 1.5, 2, 1.5, ice);
		BeastArt.Bone head = m.bone("head", "body", 0, 21, -6);
		cc(head, 0, 18.5, -9.5, 9, 9, 8, fur);
		cc(head, 0, 19, -13.8, 7, 7, 1, (f, x, y, w, h, d) -> { // the face: blue-grey, the heavy brow over glowing eyes
			if (f == BeastArt.Face.FRONT && y == 2 && (x == 1 || x == 5)) return glow(k.eye);
			if (f == BeastArt.Face.FRONT && y == 2 && (x == 2 || x == 4)) return BLACK;
			if (f == BeastArt.Face.FRONT && y == 4 && (x == 2 || x == 4)) return c(0x2A3A4A);
			return noise(skin, f, x, y, 113, 0.1);
		});
		cc(head, 0, 24.2, -14.5, 8, 1.4, 1.6, plain(k.dark, 114)); // the brow
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 19.5, -12);
		cc(jaw, 0, 18, -13.6, 6, 1.5, 2.5, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0x2A3A5A) : noise(skin, f, x, y, 115, 0.1));
		head.cube(-2.2, 18.4, -15.3, 0.8, 1.4, 0.8, BeastArt.solid(WHITE));
		head.cube(1.4, 18.4, -15.3, 0.8, 1.4, 0.8, BeastArt.solid(WHITE));
		lids(m, "head", 26, 7, 2, -14.3, 6, skin);
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone arm = m.bone(side > 0 ? "arm_l" : "arm_r", "body", side * 10.5, 21.5, -2);
			cc(arm, side * 10.5, 0.5, -2, 6, 22, 6, (f, x, y, w, h, d) -> y >= h - 3 ? noise(skin, f, x, y, 116, 0.1) : fur.paint(f, x, y, w, h, d));
			cc(arm, side * 10.5, 0, -3.5, 6.5, 2, 3, plain(skin, 117)); // the knuckles
			BeastArt.Bone leg = m.bone(side > 0 ? "leg_l" : "leg_r", null, side * 4, 8.5, 2);
			cc(leg, side * 4, 0, 2, 5, 9, 6, shod(skin, 1, fur));
		}
	}

	static final Coat BEAR = new Coat(c(0x5A3A22), c(0x2E1E12), c(0x8A6A4A), c(0x6A4A30), c(0xE8A030), c(0xF0A030), c(0x2A2018));

	static void earthquakeBear(BeastArt.Model m) {
		Coat k = BEAR;
		BeastArt.Painter fur = fur(k, 120);
		// Stone slabs grown on its back, cracked with the earth's glowing qi.
		BeastArt.Painter stone = (f, x, y, w, h, d) -> {
			if (f != BeastArt.Face.DOWN && BeastArt.hash(x + f.ordinal() * 11, y, 121) < 0.06) return glow(k.glowing);
			return rock(c(0x8A8A82), c(0x4A4A44), 0, 122).paint(f, x, y, w, h, d);
		};
		BeastArt.Bone body = m.bone("body", null, 0, 16, 0);
		cc(body, 0, 9, 1, 18, 14, 28, fur);
		cc(body, 0, 21, -7, 15, 5, 11, fur); // the shoulders' hump
		cc(body, 0, 25.5, -7, 9, 2.5, 7, stone);
		cc(body, 3, 23, 4, 7, 2.5, 8, stone);
		cc(body, -3.5, 23, 9, 6, 2, 6, stone);
		cc(body, -4, 25, -3, 4, 3, 4, stone);
		legs(m, 18, 28, 6, 9, 0, 1.5, shod(c(0x1E140C), 1, fur));
		BeastArt.Bone head = m.bone("head", null, 0, 18, -13);
		cc(head, 0, 13.5, -17.5, 11, 10, 9, eyes(k, fur, 4, 2, 1, true));
		cc(head, 0, 14, -23.5, 6, 5, 4, fronted((f, x, y, w, h, d) -> y <= 1 && x >= 2 && x <= 3 ? BLACK : noise(k.light, f, x, y, 123, 0.1), plain(k.light, 124)));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 14.5, -17);
		cc(jaw, 0, 12.8, -22.8, 5.4, 1.6, 4.4, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0x6A2A2A) : noise(k.light, f, x, y, 125, 0.1));
		for (int side : new int[] {1, -1}) cc(head, side * 4, 23.5, -16, 3, 3, 1.5, plain(k.dark, 126)); // round ears
		lids(m, "head", 23.5, 10, 4, -22, 7, k.base);
		tail(m, "tail_", null, 21, 15, new double[] {-30}, new double[] {3}, new double[] {2.5}, i -> fur);
	}

	static final Coat CROC = new Coat(c(0x2E3E26), c(0x18220F), c(0x5A6A3A), c(0xA8A85A), c(0xE0C030), c(0xE0C030), c(0x8A9098));

	static void ironJawCrocodile(BeastArt.Model m) {
		Coat k = CROC;
		BeastArt.Painter back = plates(BeastArt.mix(k.base, k.dark, 0.3), k.dark, 3, 3, scales(k, 130), 131);
		BeastArt.Bone body = m.bone("body", null, 0, 6, 0);
		cc(body, 0, 2.5, 0, 12, 6, 22, scales(k, 132));
		cc(body, 0, 8.5, 0, 8, 1, 20, back); // the scutes down its back
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		double[][] at = {{6.5, -7}, {-6.5, -7}, {6.5, 7}, {-6.5, 7}};
		BeastArt.Cube first = null;
		for (int i = 0; i < 4; i++) {
			BeastArt.Bone leg = m.bone(names[i], null, at[i][0], 4, at[i][1]);
			if (first == null) first = cc(leg, at[i][0], 0, at[i][1], 3.5, 4.5, 4, shod(k.dark, 1, scales(k, 133)));
			else leg.cubeLike(first, at[i][0] - 1.75, 0, at[i][1] - 2);
		}
		BeastArt.Bone head = m.bone("head", null, 0, 6, -11);
		cc(head, 0, 3, -14.5, 9, 4.5, 7, sideEyes(k, scales(k, 134), 0, 1, true));
		cc(head, -2.7, 7, -14, 1.6, 1.2, 2, scales(k, 135)); // the eyes' ridges
		cc(head, 2.7, 7, -14, 1.6, 1.2, 2, scales(k, 135));
		// The long snout, its edge of iron teeth; the jaw beneath it, iron-grey, opens wide.
		BeastArt.Painter teethEdge = (f, x, y, w, h, d) -> (f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == h - 1 && x % 2 == 0 ? k.horn
				: f == BeastArt.Face.FRONT && y == 0 && (x == 1 || x == w - 2) ? BLACK : scales(k, 136).paint(f, x, y, w, h, d);
		cc(head, 0, 4, -23, 6, 3, 10, teethEdge);
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 4, -12);
		cc(jaw, 0, 2.4, -19.5, 6.4, 1.6, 17, (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.UP) return x == 0 || x == w - 1 ? k.horn : c(0xC87A6A);
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 0 && x % 2 == 1) return WHITE;
			return noise(BeastArt.mix(k.horn, k.base, 0.4), f, x, y, 137, 0.15);
		});
		sideLids(m, "head", 7.5, 4.5, 0, -18, 7, 4.5, 1, k.base);
		tail(m, "tail_", null, 6, 11, new double[] {-6, 0, 0, 0}, new double[] {5, 4, 3, 2}, new double[] {7, 7, 6, 6}, i -> {
			BeastArt.Painter s = scales(k, 138 + i);
			return (f, x, y, w, h, d) -> f == BeastArt.Face.UP && (x == 0 || x == w - 1) && y % 2 == 0 ? k.dark : s.paint(f, x, y, w, h, d);
		});
	}

	static final Coat ANTELOPE = new Coat(c(0xC8A06A), c(0x7A5A30), c(0xF0E0C0), c(0xF6F0E4), c(0x1A1410), c(0x6AC8FF), c(0x3A3A48));

	static void lightningAntelope(BeastArt.Model m) {
		Coat k = ANTELOPE;
		BeastArt.Painter coat = lightning(k.glowing, fur(k, 140), 141);
		BeastArt.Bone body = m.bone("body", null, 0, 15.5, 0);
		cc(body, 0, 12, 0, 7, 7, 16, coat);
		legs(m, 7, 16, 2, 12, 0.4, 1, shod(BLACK, 1, (f, x, y, w, h, d) -> y > h * 0.6 ? noise(k.light, f, x, y, 142, 0.1) : fur(k, 143).paint(f, x, y, w, h, d)));
		// The neck rising forward from the shoulders, the head level on top of it.
		BeastArt.Bone neck = m.bone("neck_0", null, 0, 17.5, -6.5, 25, 0, 0);
		cc(neck, 0, 17, -6.5, 3, 8, 3.5, fur(k, 144));
		BeastArt.Bone head = m.bone("head", "neck_0", 0, 25, -6.5, -25, 0, 0);
		cc(head, 0, 24, -9, 4, 4, 6, sideEyes(k, fur(k, 145), 1, 1, false));
		cc(head, 0, 23.8, -12.8, 3, 2.6, 2.4, fronted((f, x, y, w, h, d) -> y == 0 ? BLACK : noise(k.light, f, x, y, 146, 0.1), plain(k.light, 147)));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 24, -10);
		cc(jaw, 0, 23.2, -12.4, 2.4, 0.8, 2.8, plain(k.light, 148));
		sideLids(m, "head", 28, 4, 1, -12, 6, 2, 1, k.base);
		// The horns: long, ringed, swept back, crackling at their tips.
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone horn = m.bone(side > 0 ? "horn_l" : "horn_r", "head", side * 1.2, 28, -8.5, -30, 0, side * -8);
			cc(horn, side * 1.2, 28, -8.5, 1, 10, 1, (f, x, y, w, h, d) -> y <= 1 ? glow(k.glowing) : y % 2 == 0 ? k.horn : BeastArt.shade(k.horn, 0.25));
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 2, 27, -7.5, 0, 0, side * 50);
			cc(ear, side * 2.6, 27, -7.5, 1, 2.5, 1, plain(k.base, 149));
		}
		tail(m, "tail_", null, 18, 8, new double[] {-50}, new double[] {1.6}, new double[] {2.5}, i -> plain(k.light, 150));
	}

	static final Coat EMBER_FOX = new Coat(c(0xD8501E), c(0x5A1E10), c(0xFFE0B0), c(0xFFF0D8), c(0xFFD040), c(0xFFB020), c(0x2A1410));

	static void emberFox(BeastArt.Model m) {
		Coat k = EMBER_FOX;
		BeastArt.Painter coat = (f, x, y, w, h, d) -> f != BeastArt.Face.DOWN && BeastArt.hash(x + f.ordinal() * 3, y, 160) < 0.05 ? glow(k.glowing) : fur(k, 161).paint(f, x, y, w, h, d);
		BeastArt.Bone body = m.bone("body", null, 0, 9, 0);
		cc(body, 0, 6, 0, 7, 6, 14, coat);
		cc(body, 0, 5.8, -4.5, 7.4, 6.4, 3.5, plain(k.light, 162)); // the white chest
		legs(m, 7, 14, 2, 6, 0.4, 0.8, shod(k.dark, 3, fur(k, 163)));
		BeastArt.Bone head = m.bone("head", null, 0, 11, -7);
		cc(head, 0, 10, -9.5, 7, 6, 5.5, eyes(k, (f, x, y, w, h, d) -> f == BeastArt.Face.FRONT && y >= 3 || (f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y >= 3
				? noise(k.light, f, x, y, 164, 0.1) : fur(k, 165).paint(f, x, y, w, h, d), 2, 1, 2, true));
		cc(head, 0, 10.2, -13.6, 3, 2.4, 3.6, fronted((f, x, y, w, h, d) -> y == 0 && x == 1 ? BLACK : noise(k.light, f, x, y, 166, 0.1), plain(k.light, 167)));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 10.4, -12);
		cc(jaw, 0, 9.7, -13.5, 2.4, 0.8, 3, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? glow(c(0xFF8020)) : noise(k.light, f, x, y, 168, 0.1));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 2.2, 16, -9, 0, 0, side * -8);
			cc(ear, side * 2.2, 16, -9, 2.2, 3.5, 1, (f, x, y, w, h, d) -> y == 0 ? glow(k.glowing) : f == BeastArt.Face.FRONT ? k.dark : plain(k.base, 169).paint(f, x, y, w, h, d));
		}
		lids(m, "head", 16, 6, 2, -12.25, 5, k.base);
		// Three great tails fanned up behind it, their ends aflame.
		double[] spread = {-28, 0, 28};
		for (int i = 0; i < 3; i++) {
			BeastArt.Bone fan = m.bone("fan_" + i, null, 0, 11, 6.5, 40, spread[i], 0);
			cc(fan, 0, 9.5, 9, 3, 3, 4, fur(k, 170 + i));
			cc(fan, 0, 9, 14, 4, 4, 6, flameTip(k.glowing, c(0xFFF4B0), 0.5, fur(k, 173 + i)));
		}
	}

	// --- The rare beasts ---

	static final Coat ROC = new Coat(c(0xC89A2A), c(0x6A4A22), c(0xF8E090), c(0xF4E4B0), c(0xFF6020), c(0xFFE070), c(0x3A2A10));

	static void goldenWingedRoc(BeastArt.Model m) { bird(m, ROC, 2.6, 180, true); }

	static final Coat PYTHON = new Coat(c(0x2E7A4A), c(0x184A2A), c(0x7AC890), c(0xE0C04A), c(0xFFD040), c(0xFFD040), WHITE);

	static void jadeScaledPython(BeastArt.Model m) {
		Coat k = PYTHON;
		// Gold saddles down its back, edged in jade.
		Coat saddle = new Coat(k.base, k.belly, k.light, k.belly, k.eye, k.glowing, k.horn);
		segments(m, 0, new double[] {7, 7.5, 7.5, 7, 6.5, 6, 5.5, 5, 4.2, 3.5, 2.8, 2}, 6, i -> diamonds(saddle, 190 + i));
		BeastArt.Bone neck = m.bone("neck_0", null, 0, 3, 0.5, -20, 0, 0);
		neck.cube(-3, 0, -6, 6, 5.4, 6.5, diamonds(saddle, 202));
		BeastArt.Bone head = m.bone("head", "neck_0", 0, 3, -6, 20, 0, 0);
		head.cube(-4, 0.5, -14, 8, 5, 8.5, sideEyes(k, diamonds(saddle, 203), 1, 2, true));
		head.cube(-2.8, 0.7, -17.5, 5.6, 3.6, 3.8, scales(k, 204));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 1, -12);
		jaw.cube(-3, -0.4, -17.3, 6, 1.2, 6.5, (f, x, y, w, h, d) -> f == BeastArt.Face.UP ? c(0xC86A7A) : noise(k.belly, f, x, y, 205, 0.1));
		head.cube(-2, -0.2, -17, 0.6, 1, 0.6, BeastArt.solid(WHITE));
		head.cube(1.4, -0.2, -17, 0.6, 1, 0.6, BeastArt.solid(WHITE));
		BeastArt.Bone tongue = m.bone("tongue", "head", 0, 1.8, -17.5);
		tongue.cube(-0.5, 1.8, -21.5, 1, 0, 4, BeastArt.solid(c(0xD8384A)));
		sideLids(m, "head", 5.5, 5, 1, -14, 8.5, 4, 2, k.base);
	}

	static final Coat RHINO = new Coat(c(0x6A6A72), c(0x3A3A44), c(0x9A9AA6), c(0x8A8A92), c(0xFFE070), c(0x6AC8FF), c(0xE8E0D0));

	static void hornedThunderRhino(BeastArt.Model m) {
		Coat k = RHINO;
		// Thick hide in wavering folds (plates in a staggered grid read as stone brick), lightning in its creases.
		BeastArt.Painter armour = lightning(k.glowing, folds(k.base, k.dark, 5, fur(k, 210), 211), 212);
		BeastArt.Bone body = m.bone("body", null, 0, 19, 0);
		cc(body, 0, 10, 0, 22, 17, 34, armour);
		cc(body, 0, 27, -9, 16, 3, 12, folds(k.light, k.dark, 3, fur(new Coat(k.light, k.dark, k.light, k.belly, k.eye, k.glowing, k.horn), 213), 214)); // the shoulder's hump
		legs(m, 22, 34, 7, 10, 0.5, 2, shod(c(0x2A2A30), 2, folds(k.base, k.dark, 3, fur(k, 215), 216)));
		BeastArt.Bone head = m.bone("head", null, 0, 20, -17);
		cc(head, 0, 11, -22.5, 12, 11, 12, sideEyes(k, folds(k.base, k.dark, 4, fur(k, 217), 218), 4, 1, true));
		cc(head, 0, 11.5, -30, 9, 7, 4, plain(k.light, 219)); // the snout
		// The great horn on its snout, and a lesser behind it, both ringed with thunder at their points.
		BeastArt.Painter horn = (f, x, y, w, h, d) -> y <= Math.max(1, h / 5) ? glow(k.glowing) : BeastArt.mix(k.horn, c(0x8A8070), y / (double) h);
		BeastArt.Bone great = m.bone("horn_0", "head", 0, 18.5, -29.5, -20, 0, 0);
		cc(great, 0, 18.5, -29.5, 4, 12, 4, horn);
		BeastArt.Bone lesser = m.bone("horn_1", "head", 0, 22, -24, -10, 0, 0);
		cc(lesser, 0, 22, -24, 3, 6, 3, horn);
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 12, -24);
		cc(jaw, 0, 10.5, -29, 8, 2, 5, plain(k.light, 220));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = m.bone(side > 0 ? "ear_l" : "ear_r", "head", side * 5, 22, -19, 0, 0, side * -20);
			cc(ear, side * 5, 22, -19, 2.5, 3.5, 1.5, plain(k.dark, 221));
		}
		sideLids(m, "head", 22, 11, 4, -28.5, 12, 6, 1, k.base);
		tail(m, "tail_", null, 22, 17, new double[] {-60}, new double[] {2}, new double[] {4}, i -> plain(k.dark, 222));
	}

	static final Coat FLOOD = new Coat(c(0x1E6A8A), c(0x0E3A50), c(0x5AB0D0), c(0xD8F0F0), c(0xF0F8FF), c(0x7AE8F0), c(0xE8F0F0));

	static void floodDragon(BeastArt.Model m) {
		Coat k = FLOOD;
		double[] thick = {9, 9.5, 9.5, 9, 8.5, 8, 7, 6, 5, 4};
		segments(m, 0, thick, 8, i -> scales(k, 230 + i));
		// The fin crest down its back, glowing faintly as the deep's qi does; the tail's fin at its end.
		for (int i = 0; i < thick.length; i++) {
			BeastArt.Bone seg = boneNamed(m, "seg_" + i);
			double h = Math.max(1, thick[i] * 0.8);
			cc(seg, 0, h, i * 8 + 4, 1, 2.5 - i * 0.12, 6, (f, x, y, w, hh, d) -> glow(BeastArt.mix(k.glowing, k.base, y / (double) Math.max(1, hh) * 0.7)));
		}
		BeastArt.Bone last = boneNamed(m, "seg_" + (thick.length - 1));
		cc(last, 0, 0.5, thick.length * 8 + 2.5, 9, 1, 6, (f, x, y, w, hh, d) -> glow(BeastArt.mix(k.glowing, k.light, 0.4)));
		// Four short legs with webbed claws, two under its first segments and two further back (they paddle as it swims).
		String[] names = {"leg_fl", "leg_fr", "leg_bl", "leg_br"};
		String[] on = {"seg_1", "seg_1", "seg_5", "seg_5"};
		double[][] at = {{5, 12}, {-5, 12}, {4.5, 44}, {-4.5, 44}};
		BeastArt.Cube first = null;
		for (int i = 0; i < 4; i++) {
			BeastArt.Bone leg = m.bone(names[i], on[i], at[i][0], 3, at[i][1]);
			if (first == null) first = cc(leg, at[i][0], 0, at[i][1], 2.5, 3.5, 3, shod(k.horn, 1, scales(k, 240)));
			else leg.cubeLike(first, at[i][0] - 1.25, 0, at[i][1] - 1.5);
		}
		BeastArt.Bone head = m.bone("head", null, 0, 4, 0);
		head.cube(-5, 0.5, -10, 10, 7, 10.5, eyes(k, scales(k, 241), 1, 1, 2, true));
		head.cube(-3.5, 1, -16, 7, 4.5, 6.5, fronted((f, x, y, w, h, d) -> y == 1 && (x == 1 || x == w - 2) ? BLACK : scales(k, 242).paint(f, x, y, w, h, d), scales(k, 243)));
		BeastArt.Bone jaw = m.bone("jaw", "head", 0, 1.5, -9);
		jaw.cube(-3.3, 0, -15.5, 6.6, 1.5, 7, (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.UP) return c(0x8A3A5A);
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 0 && x % 2 == 0) return WHITE;
			return noise(k.belly, f, x, y, 244, 0.1);
		});
		// A single horn, swept back; whiskers trailing from the snout.
		BeastArt.Bone horn = m.bone("horn_0", "head", 0, 7.5, -6, -35, 0, 0);
		cc(horn, 0, 7.5, -6, 1.5, 6, 1.5, (f, x, y, w, h, d) -> y == 0 ? glow(k.glowing) : BeastArt.mix(k.horn, k.light, y / (double) h));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone whisker = m.bone(side > 0 ? "whisker_l" : "whisker_r", "head", side * 3.5, 2.5, -15, 10, side * -35, 0);
			whisker.cube(side > 0 ? 3.5 : -4.1, 2.2, -15, 0.6, 0.6, 9, plain(k.light, 245));
			BeastArt.Bone fin = m.bone(side > 0 ? "frill_l" : "frill_r", "head", side * 5, 5, -2, 0, side * 20, side * -10);
			fin.cube(side > 0 ? 5 : -5.5, 3, -2, 0.5, 4.5, 5, (f, x, y, w, h, d) -> glow(BeastArt.mix(k.glowing, k.base, 0.3)));
		}
		lids(m, "head", 7.5, 7, 1, -10, 8, k.base);
	}

	static BeastArt.Bone boneNamed(BeastArt.Model m, String name) {
		for (BeastArt.Bone b : m.bones) if (b.name.equals(name)) return b;
		throw new IllegalArgumentException(m.id + " has no bone " + name);
	}

	// --- Writing ---

	/** The base texture and the glow mask (null where nothing glows): a painted pixel of alpha 254 glows. */
	static BufferedImage[] split(BufferedImage painted) {
		BufferedImage base = new BufferedImage(painted.getWidth(), painted.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage mask = new BufferedImage(painted.getWidth(), painted.getHeight(), BufferedImage.TYPE_INT_ARGB);
		boolean any = false;
		for (int y = 0; y < painted.getHeight(); y++) {
			for (int x = 0; x < painted.getWidth(); x++) {
				int p = painted.getRGB(x, y);
				if (p >>> 24 == 0xFE) {
					base.setRGB(x, y, 0xFF000000 | p);
					mask.setRGB(x, y, 0xFF000000 | p);
					any = true;
				} else {
					base.setRGB(x, y, p);
				}
			}
		}
		return new BufferedImage[] {base, any ? mask : null};
	}

	/** The "idle" loop: the body's slow breath, and a blink now and then (the lids scaled from nothing). */
	static List<BeastArt.Anim> animations(BeastArt.Model m) {
		BeastArt.Anim idle = new BeastArt.Anim("idle", 6, true);
		boolean body = m.bones.stream().anyMatch(b -> b.name.equals("body"));
		if (body) for (int i = 0; i <= 4; i++) idle.key("body", "scale", i * 1.5, 1, i % 2 == 1 ? 1.025 : 1, i % 2 == 1 ? 1.01 : 1);
		for (double t : new double[] {0, 1.9, 2.0, 2.1, 2.2, 4.7, 4.8, 4.9, 5.0, 6}) {
			boolean shut = t == 2.0 || t == 2.1 || t == 4.8 || t == 4.9;
			idle.key("lids", "scale", t, 1, shut ? 1 : 0, 1);
		}
		if (m.bones.stream().anyMatch(b -> b.name.equals("tongue"))) {
			for (double t : new double[] {0, 0.9, 1.0, 1.15, 1.3, 1.45, 1.6, 3.9, 4.0, 4.15, 4.3, 6}) {
				boolean out = t == 1.0 || t == 1.3 || t == 4.0;
				boolean half = t == 1.15 || t == 1.45 || t == 4.15;
				idle.key("tongue", "scale", t, 1, 1, out ? 1 : half ? 0.4 : 0);
			}
		}
		List<BeastArt.Anim> anims = new ArrayList<>();
		anims.add(idle);
		return anims;
	}

	interface Build {
		void build(BeastArt.Model m);
	}

	static final Object[][] KINDS = {
			{"iron_hide_boar", (Build) Bodies::ironHideBoar}, {"shadow_wolf", (Build) Bodies::shadowWolf}, {"jade_viper", (Build) Bodies::jadeViper},
			{"rock_shell_turtle", (Build) Bodies::rockShellTurtle}, {"wind_hawk", (Build) Bodies::windHawk}, {"fire_monkey", (Build) Bodies::fireMonkey},
			{"bloodfang_tiger", (Build) Bodies::bloodfangTiger}, {"frost_ape", (Build) Bodies::frostApe}, {"earthquake_bear", (Build) Bodies::earthquakeBear},
			{"iron_jaw_crocodile", (Build) Bodies::ironJawCrocodile}, {"lightning_antelope", (Build) Bodies::lightningAntelope}, {"ember_fox", (Build) Bodies::emberFox},
			{"golden_winged_roc", (Build) Bodies::goldenWingedRoc}, {"jade_scaled_python", (Build) Bodies::jadeScaledPython},
			{"horned_thunder_rhino", (Build) Bodies::hornedThunderRhino}, {"flood_dragon", (Build) Bodies::floodDragon},
	};

	/** Every beast, under {@code assets} (the mod's assets folder): the smallest texture each one's boxes fit. */
	static void writeAll(Path assets) throws Exception {
		for (Object[] kind : KINDS) {
			String id = (String) kind[0];
			BeastArt.Model m = new BeastArt.Model("demon_beast_" + id);
			((Build) kind[1]).build(m);
			BeastArt.Atlas atlas = null;
			for (int size = 64; size <= 512; size *= 2) {
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
			BufferedImage[] painted = split(atlas.paint());
			BeastArt.write(assets.resolve("textures/entity/demon_beast/" + id + ".png"), painted[0]);
			java.nio.file.Path mask = assets.resolve("textures/entity/demon_beast/" + id + "_glowmask.png");
			if (painted[1] != null) BeastArt.write(mask, painted[1]);
			else java.nio.file.Files.deleteIfExists(mask);
			BeastArt.write(assets.resolve("geo/demon_beast/" + id + ".geo.json"), BeastArt.geo(m, atlas));
			BeastArt.write(assets.resolve("animations/demon_beast/" + id + ".animation.json"), BeastArt.animations(animations(m)));
		}
		Cores.writeAll(assets);
	}
}
