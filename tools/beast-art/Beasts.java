import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The beasts themselves for BeastArt: each race's colourings, its head and tail, their animations; Ox-Head's and Horse-Face's heads.
 * Units are pixels about the neck (y up, the face toward -z): the human head spans x -4..4, y 0..8, z -4..4; the body hangs below the
 * neck to y -12, z -2..2 (its back at +z), the feet at y -24.
 */
public class Beasts {
	/** A colouring: fur or scales, the darker (a tiger's stripes, a fox's socks), the lighter, the belly, eyes, nose, inside the ears or mouth, horn, mane, and the clothes on a race skin. */
	record Palette(String id, int base, int dark, int light, int belly, int eye, int nose, int inner, int horn, int mane, int cloth, int sash) {}

	static int c(int hex) { return BeastArt.rgb(hex); }

	static final Palette[] TIGERS = {
			new Palette("golden", c(0xE08A2C), c(0x1C1410), c(0xF6EAD2), c(0xF6EAD2), c(0xE8C030), c(0xD87878), c(0xF0B8B0), c(0xF0E8D8), c(0x1C1410), c(0x2A2430), c(0x8A2A1E)),
			new Palette("white", c(0xEAE6DC), c(0x2A2A32), c(0xFFFFFF), c(0xFFFFFF), c(0x6AB0E0), c(0xD88A8A), c(0xF0C8C0), c(0xF0E8D8), c(0x2A2A32), c(0x2A2430), c(0x3A5A8A)),
			new Palette("black", c(0x2A2420), c(0xD89030), c(0x8A7A6A), c(0x6A5A4A), c(0xE8C030), c(0x3A2A2A), c(0x6A4A4A), c(0xF0E8D8), c(0xD89030), c(0x1E1A22), c(0xC08830)),
	};
	static final Palette[] LIZARDS = {
			new Palette("jade", c(0x3E8A4A), c(0x1E4A28), c(0x8ACB80), c(0xD8E0A0), c(0xE8D030), c(0xC82828), c(0xC82828), c(0xD8E0A0), c(0x1E4A28), c(0x2A2430), c(0xD8A830)),
			new Palette("black", c(0x26262E), c(0x0E0E14), c(0x5A4A7A), c(0x7A7A8A), c(0xD84040), c(0x9A2A6A), c(0x9A2A6A), c(0x7A7A8A), c(0x0E0E14), c(0x1E1A22), c(0x6A4A9A)),
			new Palette("crimson", c(0xA82424), c(0x4A0C10), c(0xE06A4A), c(0xE8C890), c(0xF0D040), c(0x2A1A1A), c(0x5A1A1A), c(0xE8C890), c(0x4A0C10), c(0x2A2430), c(0xE8C890)),
	};
	static final Palette[] FOXES = {
			new Palette("red", c(0xD8642A), c(0x2A1E18), c(0xF6F0E8), c(0xF6F0E8), c(0xE0A030), c(0x1A1414), c(0xF0D0C8), c(0xF6F0E8), c(0xF6F0E8), c(0x2A2430), c(0x2A5A8A)),
			new Palette("snow", c(0xEEEEF2), c(0x8A909A), c(0xFFFFFF), c(0xFFFFFF), c(0x60A0E0), c(0x2A2A30), c(0xF0D8D8), c(0xFFFFFF), c(0xFFFFFF), c(0x2A3A5A), c(0x9ABAD8)),
			new Palette("silver", c(0x5A5A64), c(0x1A1A1E), c(0xD8D8E0), c(0xC8C8D0), c(0xE8C040), c(0x101014), c(0x9A8A8A), c(0xD8D8E0), c(0xD8D8E0), c(0x1E1A22), c(0x8A2A3A)),
	};
	static final Palette[] DRAGONS = {
			new Palette("azure", c(0x2E8AB8), c(0x145070), c(0x6AC0E0), c(0xE8C860), c(0xF0D040), c(0x0E2A40), c(0xC83A3A), c(0xE0D0A8), c(0xF0F0F8), c(0x1E2A3A), c(0xD8A830)),
			new Palette("gold", c(0xD8A830), c(0x8A6410), c(0xF4DA78), c(0xF4E8C0), c(0xD83A1A), c(0x5A3A0A), c(0xC83A3A), c(0xF4ECDC), c(0xC82828), c(0x3A1E1A), c(0x8A2020)),
			new Palette("crimson", c(0xB42828), c(0x5A0E10), c(0xE05A4A), c(0xE8C060), c(0xF0D040), c(0x3A0808), c(0xE8C060), c(0xE8D8B8), c(0xF0C040), c(0x2A1A1E), c(0xD8A830)),
	};

	// --- Painters ---

	/** Fur or scales of {@code base}, with the pattern of {@code kind}: "stripes" (a tiger's), "scales", "plain". */
	static BeastArt.Painter coat(Palette p, String kind, int seed) {
		return (f, x, y, w, h, d) -> pattern(p, kind, f, x, y, w, h, seed);
	}

	static int pattern(Palette p, String kind, BeastArt.Face f, int x, int y, int w, int h, int seed) {
		int px = x + f.ordinal() * 23, py = y;
		switch (kind) {
			case "stripes": {
				// Bands across the fur, wavering: along a side face's length (x) and the top's (y).
				int along = f == BeastArt.Face.UP || f == BeastArt.Face.DOWN ? y : x;
				int across = f == BeastArt.Face.UP || f == BeastArt.Face.DOWN ? x : y;
				if (f == BeastArt.Face.FRONT || f == BeastArt.Face.BACK) { along = y; across = x; }
				double wobble = Math.sin(across * 1.3 + seed) * 0.8;
				if (Math.floorMod((int) Math.floor(along + wobble + seed), 3) == 0 && BeastArt.hash(px, py, seed) < 0.85) return p.dark;
				return BeastArt.fur(p.base, px, py, seed);
			}
			case "scales": {
				boolean edge = Math.floorMod(x + (y % 2 == 0 ? 0 : 1), 2) == 0 && y % 2 == 0;
				int c = edge ? BeastArt.mix(p.base, p.dark, 0.55) : p.base;
				if (BeastArt.hash(px, py, seed) < 0.12) c = BeastArt.mix(c, p.light, 0.5);
				return c;
			}
			default:
				return BeastArt.fur(p.base, px, py, seed);
		}
	}

	/** The coat with a lighter belly underneath (a tail's underside, a jaw's). */
	static BeastArt.Painter coatBelly(Palette p, String kind, int seed) {
		return (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN ? BeastArt.fur(p.belly, x, y, seed) : pattern(p, kind, f, x, y, w, h, seed);
	}

	// --- The tiger ---

	static void tiger(BeastArt.Model head, BeastArt.Model tail, Palette p) {
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		// The skull: striped, its forehead marked, amber eyes under white brows.
		h.cube(-4.5, 0, -4.5, 9, 8, 9, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT) {
				if (y == 3 && (x == 1 || x == 2 || x == 6 || x == 7)) return (x == 2 || x == 6) ? 0xFF0A0A0A : p.eye;
				if (y == 2 && (x >= 1 && x <= 2 || x >= 6 && x <= 7)) return p.light;
				if (y == 0 && x >= 2 && x <= 6 || y == 1 && x == 4) return p.dark; // the forehead's mark
				if (y >= 4) return BeastArt.fur(p.light, x, y, 3);
			}
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y >= 4 && BeastArt.frontness(f, x, y, w, hh) > 0.5) return BeastArt.fur(p.light, x, y, 4);
			return pattern(p, "stripes", f, x, y, w, hh, 1);
		});
		// The muzzle: white, the nose pink on its front, whisker spots.
		h.cube(-2.5, 0.5, -7.5, 5, 3.5, 3, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 0 && x >= 1 && x <= 3) return p.nose;
			if (f == BeastArt.Face.FRONT && y == 1 && x == 2) return p.nose;
			if (f == BeastArt.Face.FRONT && y == 2 && (x == 0 || x == 4)) return p.dark;
			if (f == BeastArt.Face.UP && y < 2) return BeastArt.fur(p.base, x, y, 5);
			return BeastArt.fur(p.light, x, y, 6);
		});
		// Cheek ruffs.
		h.cube(-5.5, 0, -4, 1.5, 4, 4, (f, x, y, w, hh, d) -> BeastArt.fur(p.light, x, y, 7));
		h.cube(4, 0, -4, 1.5, 4, 4, (f, x, y, w, hh, d) -> BeastArt.fur(p.light, x, y, 8));
		// Fangs.
		h.cube(-2, -0.5, -7.4, 1, 1, 1, BeastArt.solid(c(0xF4F0E4)));
		h.cube(1, -0.5, -7.4, 1, 1, 1, BeastArt.solid(c(0xF4F0E4)));
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0.5, -4.5);
		jaw.cube(-2, -0.5, -7.2, 4, 1, 2.7, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.light, x, y, 9));
		ears(head, p, 2, 8, -1.5, 2.5, 2, 1, -2, true);
		lids(head, p, frontLid(-3.5, 4, 2, 1, -4.5), frontLid(1.5, 4, 2, 1, -4.5));

		stripedTail(tail, p, new double[] {-35, -25, 20, 30}, new double[] {2, 1.8, 1.6, 1.6}, new double[] {5, 5, 4.5, 3}, true);
	}

	/** Two ears, from (x, y, z) on the right and mirrored on the left, {@code w x h x d}; tipped out {@code tilt} degrees; a tiger's (round, a white spot behind) or a fox's (tall, dark-tipped). */
	static void ears(BeastArt.Model head, Palette p, double x, double y, double z, double w, double h, double d, double tilt, boolean round) {
		for (int side : new int[] {1, -1}) {
			double ox = side > 0 ? x : -x - w;
			BeastArt.Bone ear = head.bone(side > 0 ? "ear_left" : "ear_right", "head", side * (x + w / 2), y, z + d / 2, 0, 0, side * tilt);
			ear.cube(ox, y, z, w, h, d, (f, px, py, fw, fh, fd) -> {
				if (f == BeastArt.Face.FRONT) return py == 0 && !round ? p.dark : BeastArt.fur(p.inner, px, py, 11);
				if (f == BeastArt.Face.BACK && round) return py >= 1 && px >= 1 && px <= 1 ? p.light : p.dark;
				if (!round && py == 0) return p.dark;
				return BeastArt.fur(p.base, px, py, 12);
			});
		}
	}

	/**
	 * Eyelids, one over each eye and only over the eye, shut only as it blinks (scaled from nothing down from their top edge: the
	 * "idle" animation). Each is {x, y, z, w, h, d}: the eye's own pixels on the face, the lid a fifth of a pixel proud of it, so it
	 * must be placed where the eye is painted (they once sat a row or two above the eyes, blinking on the forehead).
	 */
	static void lids(BeastArt.Model head, Palette p, double[]... eyes) {
		double top = Double.NEGATIVE_INFINITY;
		for (double[] e : eyes) top = Math.max(top, e[1] + e[4]);
		BeastArt.Bone lids = head.bone("lids", "head", 0, top, eyes[0][2]);
		for (double[] e : eyes) lids.cube(e[0], e[1], e[2], e[3], e[4], e[5], BeastArt.furred(p.base, 13));
	}

	/** An eyelid over an eye painted on a face toward -z, at x..x+w, y..y+h, the face at z: a fifth of a pixel before it. */
	static double[] frontLid(double x, double y, double w, double h, double faceZ) {
		return new double[] {x - 0.05, y - 0.05, faceZ - 0.2, w + 0.1, h + 0.1, 0.2};
	}

	/** A tail in pieces from the small of the back, each turned from the one before ({@code bend} degrees about x), ringed if {@code stripes}, its tip dark. */
	static void stripedTail(BeastArt.Model tail, Palette p, double[] bend, double[] thick, double[] length, boolean stripes) {
		double z = 2, y = -10.5;
		String parent = null;
		for (int i = 0; i < bend.length; i++) {
			BeastArt.Bone piece = tail.bone("tail_" + i, parent, 0, y, z, bend[i], 0, 0);
			double t = thick[i];
			boolean tip = i == bend.length - 1;
			int seed = 20 + i;
			piece.cube(-t / 2, y - t / 2, z, t, t, length[i], (f, x, py, w, h, d) -> {
				if (tip && stripes) return BeastArt.fur(p.dark, x, py, 21);
				return stripes ? pattern(p, "stripes", f, x, py, w, h, seed) : pattern(p, "scales", f, x, py, w, h, seed);
			});
			parent = piece.name;
			z += length[i];
		}
	}

	// --- The lizard ---

	static void lizard(BeastArt.Model head, BeastArt.Model tail, Palette p) {
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		h.cube(-4, 0, -5, 8, 5, 9, (f, x, y, w, hh, d) -> {
			// Slit eyes at the front corners, high; the belly's colour under the chin.
			if (f == BeastArt.Face.FRONT && y == 1 && (x == 0 || x == 7)) return p.eye;
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 1 && BeastArt.frontness(f, x, y, w, hh) > 0.8) return p.eye;
			if (f == BeastArt.Face.DOWN) return BeastArt.fur(p.belly, x, y, 30);
			return pattern(p, "scales", f, x, y, w, hh, 31);
		});
		h.cube(-3, 0.5, -9, 6, 3.5, 4, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 0 && (x == 1 || x == 4)) return p.dark; // the nostrils
			if (f == BeastArt.Face.DOWN) return BeastArt.fur(p.belly, x, y, 32);
			return pattern(p, "scales", f, x, y, w, hh, 33);
		});
		// Brow ridges over the eyes.
		h.cube(-4.2, 4.5, -6, 2, 1, 3, (f, x, y, w, hh, d) -> BeastArt.mix(p.base, p.dark, 0.5));
		h.cube(2.2, 4.5, -6, 2, 1, 3, (f, x, y, w, hh, d) -> BeastArt.mix(p.base, p.dark, 0.5));
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0.5, -5);
		jaw.cube(-3, -0.5, -9, 6, 1, 4, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.belly, x, y, 34));
		// The forked tongue, flicking out (scaled from nothing: "idle").
		BeastArt.Bone tongue = head.bone("tongue", "head", 0, 1, -9);
		tongue.cube(-0.5, 1, -12, 1, 0, 3, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) return y == 0 ? BeastArt.CLEAR : p.nose;
			return p.nose;
		});
		// The eyes wrap the front corners (a pixel on the front, two on the side): a lid over each corner.
		lids(head, p, new double[] {-4.1, 2.95, -5.1, 1.15, 1.1, 2.2}, new double[] {2.95, 2.95, -5.1, 1.15, 1.1, 2.2});
		// Long: down from the small of the back to the ground, and along it behind.
		stripedTail(tail, p, new double[] {-60, -15, 60, 15, 0, 0}, new double[] {3, 2.8, 2.5, 2.2, 1.8, 1.2}, new double[] {5, 5, 5, 5, 4, 3}, false);
	}

	// --- The fox spirit ---

	static void fox(BeastArt.Model head, BeastArt.Model tail, Palette p) {
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		h.cube(-4, 0, -4, 8, 7, 8, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT) {
				// The eyes a row above the white of the cheeks, fur between them and the muzzle (they once sat on the cheeks' edge,
				// level with the nose).
				if (y == 2 && (x == 1 || x == 2 || x == 5 || x == 6)) return (x == 2 || x == 5) ? 0xFF0A0A0A : p.eye;
				if (y >= 4) return BeastArt.fur(p.light, x, y, 40);
			}
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y >= 4) return BeastArt.fur(p.light, x, y, 41);
			return BeastArt.fur(p.base, x + f.ordinal() * 13, y, 42);
		});
		// The muzzle: the fur running down its bridge, white below, the black nose at its tip; so it stands out from the white cheeks.
		h.cube(-2, 0.5, -7.5, 4, 2.5, 3.5, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 0 && x >= 1 && x <= 2) return p.nose;
			if (f == BeastArt.Face.UP || y == 0 && f != BeastArt.Face.DOWN) return BeastArt.fur(p.base, x, y, 43);
			return BeastArt.fur(p.light, x, y, 44);
		});
		h.cube(-5, 0, -3, 1, 3, 3, (f, x, y, w, hh, d) -> BeastArt.fur(p.light, x, y, 45));
		h.cube(4, 0, -3, 1, 3, 3, (f, x, y, w, hh, d) -> BeastArt.fur(p.light, x, y, 46));
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0.5, -4);
		jaw.cube(-1.5, -0.3, -7.2, 3, 0.8, 3.2, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.light, x, y, 47));
		ears(head, p, 1.5, 7, -1, 3, 4, 1, -10, false);
		lids(head, p, frontLid(-3, 4, 2, 1, -4), frontLid(1, 4, 2, 1, -4));

		// Nine tails, alike, fanned out in code as many as the realm gives (BeastHeadModels.Model): each from the small of the back,
		// held up and back, bushy in the middle, white-tipped.
		BeastArt.Cube base = null, middle = null, tip = null;
		for (int i = 1; i <= 9; i++) {
			BeastArt.Bone root = tail.bone("tail_" + i, null, 0, -10.5, 2, 35, 0, 0);
			BeastArt.Bone mid = tail.bone("tail_" + i + "_1", root.name, 0, -10.5, 6, 15, 0, 0);
			BeastArt.Bone end = tail.bone("tail_" + i + "_2", mid.name, 0, -10.5, 12, 10, 0, 0);
			if (base == null) {
				base = root.cube(-1.75, -12.25, 2, 3.5, 3.5, 4, coatBelly(p, "plain", 50));
				middle = mid.cube(-2.5, -13, 6, 5, 5, 6, coatBelly(p, "plain", 51));
				tip = end.cube(-2, -12.5, 12, 4, 4, 3, (f, x, y, w, hh, d) -> BeastArt.fur(p.light, x, y, 52));
			} else {
				root.cubeLike(base, -1.75, -12.25, 2);
				mid.cubeLike(middle, -2.5, -13, 6);
				end.cubeLike(tip, -2, -12.5, 12);
			}
		}
	}

	// --- The true dragon ---

	static void dragon(BeastArt.Model head, BeastArt.Model tail, Palette p) {
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		h.cube(-4, 0, -4, 8, 7, 8, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 2 && (x == 1 || x == 2 || x == 5 || x == 6)) return (x == 2 || x == 5) ? 0xFF0A0A0A : p.eye;
			if (f == BeastArt.Face.DOWN) return BeastArt.fur(p.belly, x, y, 60);
			return pattern(p, "scales", f, x, y, w, hh, 61);
		});
		// The long snout, its nostrils at the front; teeth along the jaw beneath.
		h.cube(-2.5, 0.5, -10, 5, 3.5, 6, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 0 && (x == 1 || x == 3)) return p.nose;
			if (f == BeastArt.Face.DOWN) return BeastArt.fur(p.belly, x, y, 62);
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 3) return c(0xF4F0E4);
			return pattern(p, "scales", f, x, y, w, hh, 63);
		});
		h.cube(-4.2, 5.5, -5, 3.2, 1.2, 1.5, (f, x, y, w, hh, d) -> p.dark);
		h.cube(1, 5.5, -5, 3.2, 1.2, 1.5, (f, x, y, w, hh, d) -> p.dark);
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0.5, -4);
		jaw.cube(-2.3, -0.7, -9.5, 4.6, 1.2, 5.5, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.belly, x, y, 64));
		BeastArt.Bone beard = head.bone("beard", "jaw", 0, -0.7, -8);
		beard.cube(-1, -2.2, -8.6, 2, 1.5, 1, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x, y, 65));
		// Antlers, sweeping back, each with a tine forward.
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone antler = head.bone(side > 0 ? "antler_left" : "antler_right", "head", side * 2, 7, 0, -25, 0, side * -20);
			double ax = side > 0 ? 1.5 : -2.5;
			antler.cube(ax, 7, -0.5, 1, 5, 1, (f, x, y, w, hh, d) -> BeastArt.mix(p.horn, 0xFF000000, y * 0.04));
			antler.cube(ax, 9.5, -2.5, 1, 1, 2, BeastArt.solid(p.horn));
			antler.cube(ax, 11, 0.5, 1, 1, 2, BeastArt.solid(p.horn));
			// Whiskers from the snout, drifting back and out.
			BeastArt.Bone whisker = head.bone(side > 0 ? "whisker_left" : "whisker_right", "head", side * 2.5, 1.5, -9, 10, side * -30, 0);
			double wx = side > 0 ? 2.5 : -3.1;
			whisker.cube(wx, 1.2, -9, 0.6, 0.6, 8, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x, y, 66));
		}
		// The mane down the back of the head and the neck.
		BeastArt.Bone mane = head.bone("mane", "head", 0, 4, 3);
		mane.cube(-1.5, 1, 2.5, 3, 7.5, 3, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x * 3, y, 67));
		mane.cube(-1, -4, 3.5, 2, 5, 2, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x * 3, y, 68));
		lids(head, p, frontLid(-3, 4, 2, 1, -4), frontLid(1, 4, 2, 1, -4));

		// Long and scaled, ridged along its back, tufted with the mane at its end.
		double[] bend = {-50, -20, 55, 15, 5, 5}, thick = {3, 2.8, 2.6, 2.3, 2, 1.6}, length = {5, 5, 5, 5, 4, 3};
		double z = 2, y = -10.5;
		String parent = null;
		for (int i = 0; i < bend.length; i++) {
			BeastArt.Bone piece = tail.bone("tail_" + i, parent, 0, y, z, bend[i], 0, 0);
			double t = thick[i];
			piece.cube(-t / 2, y - t / 2, z, t, t, length[i], coatBelly(p, "scales", 70 + i));
			piece.cube(-0.3, y + t / 2, z + 0.5, 0.6, 1, length[i] - 1, BeastArt.solid(p.dark));
			if (i == bend.length - 1) piece.cube(-1.5, y - 1.5, z + length[i] - 1, 3, 3, 3, (f, x, py, w, hh, d) -> BeastArt.fur(p.mane, x, py, 77));
			parent = piece.name;
			z += length[i];
		}
	}

	// --- The Ghost Gate's wardens ---

	static final Palette OX = new Palette("ox", c(0x3A2A22), c(0x1A120E), c(0x6A5040), c(0x6A5040), c(0xE83A2A), c(0x1A1010), c(0x7A4A40),
			c(0xE8DCC0), c(0x1A120E), 0, 0);
	static final Palette HORSE = new Palette("horse", c(0x8A4A2A), c(0x1A1412), c(0xF2EEE6), c(0x6A3A20), c(0x2A1A10), c(0x1A1010), c(0x6A3A30),
			c(0xE8DCC0), c(0x1A1412), 0, 0);

	static void ox(BeastArt.Model head) {
		Palette p = OX;
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		h.cube(-4.5, 0, -4.5, 9, 9, 9, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 3 && (x == 1 || x == 2 || x == 6 || x == 7)) return (x == 1 || x == 7) ? p.eye : 0xFF0A0606;
			return BeastArt.fur(p.base, x + f.ordinal() * 11, y, 80);
		});
		h.cube(-4.5, 6.5, -5.2, 9, 1.5, 1, BeastArt.furred(p.dark, 81));
		h.cube(-2, 9, -3, 4, 1.5, 3, (f, x, y, w, hh, d) -> BeastArt.fur(p.dark, x * 2, y, 82));
		h.cube(-3.5, 0, -8.5, 7, 4.5, 4, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 1 && (x == 1 || x == 5)) return 0xFF0A0606; // nostrils
			return BeastArt.fur(p.light, x, y, 83);
		});
		// The gold ring through its nose.
		int gold = c(0xE0B040);
		h.cube(-1.5, -1.5, -8.9, 3, 1, 0.8, BeastArt.solid(gold));
		h.cube(-1.5, -0.5, -8.9, 1, 1.5, 0.8, BeastArt.solid(gold));
		h.cube(0.5, -0.5, -8.9, 1, 1.5, 0.8, BeastArt.solid(gold));
		for (int side : new int[] {1, -1}) {
			String s = side > 0 ? "left" : "right";
			BeastArt.Bone base = head.bone("horn_" + s, "head", side * 4.5, 7.5, -0.5, 0, 0, side * 20);
			base.cube(side > 0 ? 4.5 : -7.5, 6.5, -1.5, 3, 2.5, 2.5, (f, x, y, w, hh, d) -> BeastArt.fur(p.horn, x, y, 84));
			BeastArt.Bone mid = head.bone("horn_" + s + "_mid", base.name, side * 7.5, 7.5, -0.5, 0, 0, side * 35);
			mid.cube(side > 0 ? 7.5 : -10.5, 6.8, -1.2, 3, 2, 2, (f, x, y, w, hh, d) -> BeastArt.mix(p.horn, 0xFF5A4A3A, 0.2));
			BeastArt.Bone tip = head.bone("horn_" + s + "_tip", mid.name, side * 10.5, 7.5, -0.5, -30, 0, side * 40);
			tip.cube(side > 0 ? 10.5 : -13, 7, -0.8, 2.5, 1.2, 1.2, BeastArt.solid(BeastArt.mix(p.horn, 0xFF2A2018, 0.6)));
			BeastArt.Bone ear = head.bone("ear_" + s, "head", side * 4.5, 5.5, 0.5, 0, side * -15, side * -30);
			ear.cube(side > 0 ? 4.5 : -7.5, 4.8, 0, 3, 1.5, 1, (f, x, y, w, hh, d) -> f == BeastArt.Face.FRONT ? p.inner : BeastArt.fur(p.base, x, y, 85));
		}
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0, -4.5);
		jaw.cube(-3, -1, -8, 6, 1, 3.5, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.light, x, y, 86));
		lids(head, p, frontLid(-3.5, 5, 2, 1, -4.5), frontLid(1.5, 5, 2, 1, -4.5));
	}

	static void horse(BeastArt.Model head) {
		Palette p = HORSE;
		BeastArt.Bone h = head.bone("head", null, 0, 0, 0);
		h.cube(-3.5, 1, -3.5, 7, 8, 7, (f, x, y, w, hh, d) -> {
			if ((f == BeastArt.Face.SIDE_A || f == BeastArt.Face.SIDE_B) && y == 3 && BeastArt.frontness(f, x, y, w, hh) > 0.8) return 0xFF0A0606;
			if (f == BeastArt.Face.FRONT && x >= 2 && x <= 4) return BeastArt.fur(p.light, x, y, 90); // the blaze
			return BeastArt.fur(p.base, x + f.ordinal() * 11, y, 91);
		});
		// The long face, the blaze running down it, the nose wider at its end, the jaw under it.
		h.cube(-2.5, 0, -11, 5, 6, 7.5, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.UP && x >= 1 && x <= 3) return BeastArt.fur(p.light, x, y, 92);
			return BeastArt.fur(p.base, x + f.ordinal() * 7, y, 93);
		});
		h.cube(-3, -0.5, -11.8, 6, 4, 3, (f, x, y, w, hh, d) -> {
			if (f == BeastArt.Face.FRONT && y == 1 && (x == 1 || x == 4)) return 0xFF0A0606;
			return BeastArt.fur(BeastArt.mix(p.base, 0xFF1A1010, 0.45), x, y, 94);
		});
		BeastArt.Bone jaw = head.bone("jaw", "head", 0, 0, -4.5);
		jaw.cube(-2, -1, -10.5, 4, 1, 6, (f, x, y, w, hh, d) -> f == BeastArt.Face.UP ? p.inner : BeastArt.fur(p.base, x, y, 95));
		for (int side : new int[] {1, -1}) {
			BeastArt.Bone ear = head.bone(side > 0 ? "ear_left" : "ear_right", "head", side * 2.2, 9, 0.5, -10, 0, side * -12);
			ear.cube(side > 0 ? 1.2 : -3.2, 9, 0, 2, 4, 1, (f, x, y, w, hh, d) -> f == BeastArt.Face.FRONT ? p.inner : BeastArt.fur(p.base, x, y, 96));
			ear.cube(side > 0 ? 1.7 : -2.7, 13, 0.2, 1, 1, 0.6, BeastArt.furred(p.base, 97));
		}
		// The black mane down the back of the neck and a crest over the poll, the forelock on the brow.
		BeastArt.Bone mane = head.bone("mane", "head", 0, 9, 2);
		mane.cube(-1, -6, 3.5, 2, 15, 2, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x * 3, y, 98));
		mane.cube(-1, 9, -2, 2, 2, 6, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x * 3, y, 99));
		h.cube(-1.5, 6.5, -4.2, 3, 3, 0.8, (f, x, y, w, hh, d) -> BeastArt.fur(p.mane, x * 3, y, 100));
		// Its eyes are on the sides of the head, at the front: a lid on each side.
		lids(head, p, new double[] {3.5, 4.95, -3.55, 0.2, 1.1, 2.1}, new double[] {-3.7, 4.95, -3.55, 0.2, 1.1, 2.1});
	}

	// --- Animations ---

	/**
	 * The head's: "idle" (ears flick now and then, the eyes blink, a lizard's tongue flicks, a dragon's whiskers and mane drift, the
	 * horse's ears turn), and "strike" (the jaw drops as it strikes and closes).
	 */
	static List<BeastArt.Anim> animations(String kind) {
		List<BeastArt.Anim> anims = new ArrayList<>();
		BeastArt.Anim idle = new BeastArt.Anim("idle", 6, true);
		// The blink, twice in six seconds: the lids scaled from nothing to whole and back.
		for (double t : new double[] {0, 1.9, 2.0, 2.1, 2.2, 4.7, 4.8, 4.9, 5.0, 6}) {
			boolean shut = t == 2.0 || t == 2.1 || t == 4.8 || t == 4.9;
			idle.key("lids", "scale", t, 1, shut ? 1 : 0, 1);
		}
		boolean ears = !kind.equals("lizard") && !kind.equals("dragon");
		if (ears) {
			idle.key("ear_left", "rotation", 0, 0, 0, 0).key("ear_left", "rotation", 3.2, 0, 0, 0).key("ear_left", "rotation", 3.35, 0, 0, -18)
					.key("ear_left", "rotation", 3.5, 0, 0, 0).key("ear_left", "rotation", 3.65, 0, 0, -12).key("ear_left", "rotation", 3.8, 0, 0, 0)
					.key("ear_left", "rotation", 6, 0, 0, 0);
			idle.key("ear_right", "rotation", 0, 0, 0, 0).key("ear_right", "rotation", 0.8, 0, 0, 0).key("ear_right", "rotation", 0.95, 0, 0, 18)
					.key("ear_right", "rotation", 1.1, 0, 0, 0).key("ear_right", "rotation", 5.2, 0, 0, 0).key("ear_right", "rotation", 5.35, -15, 0, 0)
					.key("ear_right", "rotation", 5.6, 0, 0, 0).key("ear_right", "rotation", 6, 0, 0, 0);
		}
		if (kind.equals("lizard")) {
			for (double t : new double[] {0, 0.9, 1.0, 1.15, 1.3, 1.45, 1.6, 3.9, 4.0, 4.15, 4.3, 6}) {
				boolean out = t == 1.0 || t == 1.3 || t == 4.0;
				boolean half = t == 1.15 || t == 1.45 || t == 4.15;
				idle.key("tongue", "scale", t, 1, 1, out ? 1 : half ? 0.4 : 0);
			}
		}
		if (kind.equals("dragon")) {
			for (int i = 0; i <= 6; i++) {
				double wave = Math.sin(i * Math.PI / 3);
				idle.key("whisker_left", "rotation", i, wave * 6, wave * 8, wave * 5);
				idle.key("whisker_right", "rotation", i, -wave * 6, -wave * 8, -wave * 5);
				idle.key("mane", "rotation", i, Math.sin(i * Math.PI / 3 + 1) * 4, 0, Math.sin(i * Math.PI / 3) * 3);
			}
		}
		if (kind.equals("ox") || kind.equals("horse")) {
			// The jaw working slowly, chewing.
			for (int i = 0; i <= 12; i++) idle.key("jaw", "rotation", i * 0.5, i % 2 == 0 ? 0 : 6, 0, 0);
		}
		anims.add(idle);
		BeastArt.Anim strike = new BeastArt.Anim("strike", 0.35, false);
		strike.key("jaw", "rotation", 0, 0, 0, 0).key("jaw", "rotation", 0.1, 28, 0, 0).key("jaw", "rotation", 0.35, 0, 0, 0);
		anims.add(strike);
		return anims;
	}

	interface Build {
		void build(BeastArt.Model head, BeastArt.Model tail, Palette p);
	}

	/** Everything, under {@code assets} (the mod's assets folder). */
	static void writeAll(Path assets) throws Exception {
		Object[][] races = {{"tiger", TIGERS, (Build) Beasts::tiger}, {"lizard", LIZARDS, (Build) Beasts::lizard}, {"fox", FOXES, (Build) Beasts::fox},
				{"dragon", DRAGONS, (Build) Beasts::dragon}};
		for (Object[] race : races) {
			String id = (String) race[0];
			Palette[] palettes = (Palette[]) race[1];
			Build build = (Build) race[2];
			for (int i = 0; i < palettes.length; i++) {
				Palette p = palettes[i];
				BeastArt.Model head = new BeastArt.Model(id + "_head"), tail = new BeastArt.Model(id + "_tail");
				build.build(head, tail, p);
				BeastArt.Atlas atlas = new BeastArt.Atlas(id.equals("dragon") ? 128 : 64, id.equals("dragon") ? 128 : 64);
				atlas.models.add(head);
				atlas.models.add(tail);
				atlas.pack();
				BeastArt.write(assets.resolve("textures/entity/bloodline/" + id + "_" + p.id() + "_beast.png"), atlas.paint());
				// The shapes are the same in every colouring: written from the first.
				if (i == 0) {
					BeastArt.write(assets.resolve("geo/beast/" + id + "_head.geo.json"), BeastArt.geo(head, atlas));
					BeastArt.write(assets.resolve("geo/beast/" + id + "_tail.geo.json"), BeastArt.geo(tail, atlas));
					BeastArt.write(assets.resolve("animations/beast/" + id + ".animation.json"), BeastArt.animations(animations(id)));
				}
				BeastArt.write(assets.resolve("textures/entity/bloodline/" + id + "_" + p.id() + ".png"), Skins.skin(id, p));
			}
		}
		BeastArt.write(assets.resolve("textures/gui/ability/tigers_roar.png"), Icons.roar());
		BeastArt.write(assets.resolve("textures/gui/ability/illusion.png"), Icons.illusion());
		for (String id : new String[] {"ox", "horse"}) {
			BeastArt.Model head = new BeastArt.Model(id + "_head");
			if (id.equals("ox")) ox(head);
			else horse(head);
			BeastArt.Atlas atlas = new BeastArt.Atlas(64, 64);
			atlas.models.add(head);
			atlas.pack();
			BeastArt.write(assets.resolve("textures/entity/beast_head/" + id + ".png"), atlas.paint());
			BeastArt.write(assets.resolve("geo/beast/" + id + "_head.geo.json"), BeastArt.geo(head, atlas));
			BeastArt.write(assets.resolve("animations/beast/" + id + ".animation.json"), BeastArt.animations(animations(id)));
		}
	}
}
