import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Writes the looks of the Dao Integration heaven spells for Defying the Heavens (2026-10-24): GeckoLib models (geo/spell/<id>.geo.json,
 * Bedrock geometry: pixels, y up, the front toward -z), their animations (animations/spell/<id>.animation.json) and their textures
 * (textures/entity/spell/<id>.png, and <id>_glowmask.png for GeckoLib's own glow layer, AutoGlowingGeoLayer).
 * <p>
 * Built as BeastArt builds the demon beasts (Model, Bone, Cube, a painter for each cube), with two changes: every face has a UV of its own
 * (Bedrock's per-face UV) instead of box UV, so a face is painted at the model's density ({@link Model#tpp}, texels per model pixel,
 * whatever the cube's size) and a thin plane (a leaf, a feather, a flame) takes texture only for its two broad faces; and a painter is
 * handed where its pixel lies in the model at rest ({@link Texel}), so patterns run on across cubes (a tiger's stripes, a trunk's spiral
 * bark, a shell's hexagons).
 * <p>
 * A painted pixel's alpha says what it is: 255 solid; 0 nothing; an even alpha below 254 seen through (spirit light); 254 glowing
 * ({@link #glow(int)}); an odd alpha below 254 glowing and seen through ({@link #glow(int, double)}). {@link #split} lifts the glowing ones
 * onto the glow mask, at their own alpha (GeckoLib takes a glowing pixel out of the base texture and draws it again, full bright, at the
 * mask's alpha).
 * <p>
 * Run: {@code java SpellArt.java <the mod's src/main/resources/assets/defying-the-heavens> [ids...]} (JDK 22+; every spell, or those named).
 * Then {@code java Preview.java <assets> <out dir> <id>} draws what was written.
 */
public class SpellArt {
	// --- Colours: sums on RGB (0xRRGGBB); a pixel's alpha is set only at the end, by col or glow ---

	static double clamp01(double t) { return t < 0 ? 0 : t > 1 ? 1 : t; }

	static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }

	static double smooth(double e0, double e1, double v) {
		double t = clamp01((v - e0) / (e1 - e0));
		return t * t * (3 - 2 * t);
	}

	static int mix(int a, int b, double t) {
		t = clamp01(t);
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
		int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
		return (int) Math.round(ar + (br - ar) * t) << 16 | (int) Math.round(ag + (bg - ag) * t) << 8 | (int) Math.round(ab + (bb - ab) * t);
	}

	static int shade(int c, double by) { return by >= 0 ? mix(c, 0xFFFFFF, by) : mix(c, 0x000000, -by); }

	/** Colour stops spread evenly over {@code t} in [0, 1]. */
	static int ramp(double t, int... stops) {
		t = clamp01(t) * (stops.length - 1);
		int i = Math.min(stops.length - 2, (int) Math.floor(t));
		return mix(stops[i], stops[i + 1], t - i);
	}

	/** A solid pixel. */
	static int col(int rgb) { return 0xFF000000 | rgb & 0xFFFFFF; }

	/** A pixel seen through, {@code alpha} in [0, 1] (kept even: an odd alpha means glowing). */
	static int col(int rgb, double alpha) {
		int a = (int) Math.round(clamp01(alpha) * 255);
		if (a >= 254) return col(rgb);
		if (a < 2) return CLEAR;
		return (a & ~1) << 24 | rgb & 0xFFFFFF;
	}

	/** A glowing pixel (BeastArt's mark, alpha 254). */
	static int glow(int rgb) { return 0xFE000000 | rgb & 0xFFFFFF; }

	/** A glowing pixel seen through (an odd alpha). */
	static int glow(int rgb, double alpha) {
		int a = (int) Math.round(clamp01(alpha) * 255);
		if (a >= 254) return glow(rgb);
		if (a < 1) return CLEAR;
		return (a | 1) << 24 | rgb & 0xFFFFFF;
	}

	static boolean glowing(int argb) {
		int a = argb >>> 24;
		return a == 254 || a != 255 && (a & 1) == 1;
	}

	static final int CLEAR = 0;

	// --- Noise ---

	static double hash(int x, int y, int seed) {
		long h = x * 374761393L + y * 668265263L + seed * 2246822519L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (double) 0x1000000;
	}

	static double hash3(int x, int y, int z, int seed) {
		long h = x * 374761393L + y * 668265263L + z * 1440662683L + seed * 2246822519L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		h = (h ^ (h >>> 16)) * 2246822519L;
		return ((h ^ (h >>> 15)) & 0xFFFFFF) / (double) 0x1000000;
	}

	static double fade(double t) { return t * t * (3 - 2 * t); }

	/** Smooth value noise in [0, 1]. */
	static double noise(double x, double y, int seed) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double fx = fade(x - x0), fy = fade(y - y0);
		double a = hash(x0, y0, seed), b = hash(x0 + 1, y0, seed), c = hash(x0, y0 + 1, seed), d = hash(x0 + 1, y0 + 1, seed);
		return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
	}

	static double noise3(double x, double y, double z, int seed) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
		double fx = fade(x - x0), fy = fade(y - y0), fz = fade(z - z0);
		double v00 = lerp(hash3(x0, y0, z0, seed), hash3(x0 + 1, y0, z0, seed), fx);
		double v10 = lerp(hash3(x0, y0 + 1, z0, seed), hash3(x0 + 1, y0 + 1, z0, seed), fx);
		double v01 = lerp(hash3(x0, y0, z0 + 1, seed), hash3(x0 + 1, y0, z0 + 1, seed), fx);
		double v11 = lerp(hash3(x0, y0 + 1, z0 + 1, seed), hash3(x0 + 1, y0 + 1, z0 + 1, seed), fx);
		return lerp(lerp(v00, v10, fy), lerp(v01, v11, fy), fz);
	}

	static double lerp(double a, double b, double t) { return a + (b - a) * t; }

	static double fbm(double x, double y, int seed, int octaves) {
		double sum = 0, amp = 1, norm = 0;
		for (int i = 0; i < octaves; i++) {
			sum += noise(x, y, seed + i * 31) * amp;
			norm += amp;
			amp *= 0.5;
			x *= 2.03;
			y *= 2.03;
		}
		return sum / norm;
	}

	static double fbm3(double x, double y, double z, int seed, int octaves) {
		double sum = 0, amp = 1, norm = 0;
		for (int i = 0; i < octaves; i++) {
			sum += noise3(x, y, z, seed + i * 31) * amp;
			norm += amp;
			amp *= 0.5;
			x *= 2.03;
			y *= 2.03;
			z *= 2.03;
		}
		return sum / norm;
	}

	// --- Painting a face ---

	/** A cube's faces: SIDE_A is its -x side, SIDE_B its +x side, FRONT its -z side (the way a model faces), BACK its +z side. */
	enum Face {
		UP("up"), DOWN("down"), SIDE_A("east"), FRONT("north"), SIDE_B("west"), BACK("south");

		/** GeckoLib's (and Blockbench's) name for the face: it mirrors x, so a Bedrock cube's -x side is its "east". */
		final String key;

		Face(String key) { this.key = key; }
	}

	/**
	 * One pixel of a face, as a painter is handed it: {@code x}, {@code y} across and down the face's {@code w} x {@code h} texels (as it is
	 * seen from outside, upright; the top face seen from above with the cube's front at its foot, the bottom seen from below with the front
	 * at its head); {@code fx}, {@code fy} the same in [0, 1]; {@code lx..lz} where on the cube's box (pixels from its least corner); and
	 * {@code px..pz}, {@code nx..nz} where it lies and which way it faces in the model at rest.
	 */
	static final class Texel {
		Face f;
		int x, y, w, h;
		double fx, fy, lx, ly, lz, px, py, pz, nx, ny, nz;
		Cube cube;

		/** Where the pixel lies in the model before any turn (the cube's origin plus lx..lz): a bent chain's parts lie straight here. */
		double qx() { return cube.origin[0] + lx; }
		double qy() { return cube.origin[1] + ly; }
		double qz() { return cube.origin[2] + lz; }

		double sx() { return cube.size[0]; }
		double sy() { return cube.size[1]; }
		double sz() { return cube.size[2]; }
		boolean side() { return f == Face.SIDE_A || f == Face.SIDE_B; }
		boolean flat() { return f == Face.UP || f == Face.DOWN; }
		/** How far up the cube, 0 at its foot to 1 at its top (0.5 on the top and bottom faces' centres). */
		double up() { return cube.size[1] <= 0 ? 0.5 : ly / cube.size[1]; }
		/** How far along the cube from its front (-z, 0) to its back (+z, 1). */
		double back() { return cube.size[2] <= 0 ? 0.5 : lz / cube.size[2]; }
		/** How far across the cube from its -x side (0) to its +x side (1). */
		double across() { return cube.size[0] <= 0 ? 0.5 : lx / cube.size[0]; }
		/** Along a blade from its base (0) to its tip (1): up a standing blade, back along a flat one ({@link #flatBlade}). */
		double v() { return cube.flat ? back() : up(); }
		/** Across a blade, 0 to 1. */
		double u() { return cube.flat ? across() : back(); }
	}

	interface Painter {
		int paint(Texel t);
	}

	static Painter solid(int argb) { return t -> argb; }

	// --- Matrices (row-major 4 x 4), in the model's own Bedrock coordinates ---

	static double[] ident() { return new double[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }

	static double[] mul(double[] a, double[] b) {
		double[] r = new double[16];
		for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) {
			double s = 0;
			for (int k = 0; k < 4; k++) s += a[i * 4 + k] * b[k * 4 + j];
			r[i * 4 + j] = s;
		}
		return r;
	}

	static double[] translate(double x, double y, double z) { return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1}; }

	static double[] rotX(double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new double[] {1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1};
	}

	static double[] rotY(double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new double[] {c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1};
	}

	static double[] rotZ(double a) {
		double c = Math.cos(a), s = Math.sin(a);
		return new double[] {c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
	}

	/**
	 * A Bedrock rotation (degrees) as GeckoLib turns it, seen in the model's own coordinates: about x by -x, then y by +y, then z by -z
	 * (GeckoLib mirrors x, negates x and y, and turns Z * Y * X). So +x lifts the far end of a part reaching back (+z) and lowers one
	 * reaching forward (-z); +y swings a part reaching back toward +x; +z lowers a part reaching out to +x.
	 */
	static double[] bedrock(double rx, double ry, double rz) {
		return mul(rotZ(Math.toRadians(-rz)), mul(rotY(Math.toRadians(ry)), rotX(Math.toRadians(-rx))));
	}

	static double[] about(double[] pivot, double[] rot) {
		if (rot == null || rot[0] == 0 && rot[1] == 0 && rot[2] == 0) return ident();
		return mul(translate(pivot[0], pivot[1], pivot[2]), mul(bedrock(rot[0], rot[1], rot[2]), translate(-pivot[0], -pivot[1], -pivot[2])));
	}

	static double[] apply(double[] m, double x, double y, double z) {
		return new double[] {m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7], m[8] * x + m[9] * y + m[10] * z + m[11]};
	}

	static double[] applyDir(double[] m, double x, double y, double z) {
		double[] r = {m[0] * x + m[1] * y + m[2] * z, m[4] * x + m[5] * y + m[6] * z, m[8] * x + m[9] * y + m[10] * z};
		double l = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
		if (l > 0) for (int i = 0; i < 3; i++) r[i] /= l;
		return r;
	}

	/** {@code v} turned by a Bedrock rotation (degrees). */
	static double[] turn(double[] v, double rx, double ry, double rz) { return apply(bedrock(rx, ry, rz), v[0], v[1], v[2]); }

	/** The Bedrock rotation (degrees, no roll) that turns a cube's +y onto {@code (dx, dy, dz)}. */
	static double[] aimY(double dx, double dy, double dz) {
		double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
		dx /= l;
		dy /= l;
		dz /= l;
		double rx = Math.toDegrees(Math.acos(clamp(dy, -1, 1)));
		double ry = Math.abs(Math.sin(Math.toRadians(rx))) < 1e-6 ? 0 : Math.toDegrees(Math.atan2(-dx, -dz));
		return new double[] {rx, ry, 0};
	}

	/** The Bedrock rotation (degrees, no roll) that turns a cube's +z onto {@code (dx, dy, dz)}. */
	static double[] aimZ(double dx, double dy, double dz) {
		double l = Math.sqrt(dx * dx + dy * dy + dz * dz);
		return new double[] {Math.toDegrees(Math.asin(clamp(dy / l, -1, 1))), Math.toDegrees(Math.atan2(dx / l, dz / l)), 0};
	}

	static double[] add(double[] a, double[] b) { return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
	static double[] sub(double[] a, double[] b) { return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
	static double[] times(double[] a, double k) { return new double[] {a[0] * k, a[1] * k, a[2] * k}; }
	static double len(double[] a) { return Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]); }
	static double[] v3(double x, double y, double z) { return new double[] {x, y, z}; }

	/** A box {@code w} x {@code d} thick reaching from {@code a} to {@code b} (its +y along them), turned about its own centre. */
	static Cube limb(Bone bone, double[] a, double[] b, double w, double d, Painter p) {
		double[] mid = times(add(a, b), 0.5), dir = sub(b, a), r = aimY(dir[0], dir[1], dir[2]);
		return bone.box(mid[0], mid[1], mid[2], w, len(dir), d, p).rot(r[0], r[1], r[2]);
	}

	/**
	 * A flat blade (a leaf, a fin, a flame, a feather): a plane {@code length} long (its +y, from its base at {@code base}) and {@code width}
	 * across (its z), turned about its base: first {@code sweep} about x (within its own plane: + tips it forward, toward -z), then
	 * {@code heading} about y, then {@code bank} about z. Its painter reads {@link Texel#up()} along it and {@link Texel#back()} across it.
	 */
	static Cube blade(Bone bone, double[] base, double length, double width, double sweep, double heading, double bank, Painter p) {
		return bone.cube(base[0] - 0.05, base[1], base[2] - width / 2, 0.1, length, width, p).rot(sweep, heading, bank, base[0], base[1], base[2]);
	}

	/** A blade like {@code like} (its size, its painting: it shares its texture, mirrored if {@code flip}) at another base and turn. */
	static Cube bladeLike(Bone bone, Cube like, double[] base, double sweep, double heading, double bank, boolean flip) {
		Cube c = like.flat ? bone.cube(base[0] - like.size[0] / 2, base[1] - 0.05, base[2], like.size[0], 0.1, like.size[2], like.painter)
				: bone.cube(base[0] - 0.05, base[1], base[2] - like.size[2] / 2, 0.1, like.size[1], like.size[2], like.painter);
		c.shares = like.shares != null ? like.shares : like;
		c.flipped = flip;
		c.flat = like.flat;
		c.tpp = like.tpp;
		System.arraycopy(like.skip, 0, c.skip, 0, 6);
		return c.rot(sweep, heading, bank, base[0], base[1], base[2]);
	}

	/**
	 * A flat blade lying level: {@code length} long back along +z from its base and {@code width} across x, turned about its base by
	 * {@code pitch} about x (+ lifts its tip), then {@code heading} about y, then {@code bank} about z. Its painter reads {@link Texel#v()}
	 * along it and {@link Texel#u()} across, as a standing blade's does.
	 */
	static Cube flatBlade(Bone bone, double[] base, double length, double width, double pitch, double heading, double bank, Painter p) {
		Cube c = bone.cube(base[0] - width / 2, base[1] - 0.05, base[2], width, 0.1, length, p).rot(pitch, heading, bank, base[0], base[1], base[2]);
		c.flat = true;
		return c;
	}

	/**
	 * A blade (standing, or {@code flat}) painted by {@code p} the first time the model asks for {@code name}, and sharing that painting
	 * (the same size, mirrored if {@code flip}) every time after.
	 */
	static Cube shared(Model m, Bone bone, String name, boolean flat, double[] base, double length, double width, double sweep, double heading, double bank,
			Painter p, double tpp, boolean flip) {
		Cube like = m.shared.get(name);
		if (like == null) {
			Cube c = (flat ? flatBlade(bone, base, length, width, sweep, heading, bank, p) : blade(bone, base, length, width, sweep, heading, bank, p)).density(tpp);
			m.shared.put(name, c);
			return c;
		}
		return bladeLike(bone, like, base, sweep, heading, bank, flip);
	}

	/** A flat plane lying level ({@code w} across x, {@code d} along z), turned about its centre. Its painter reads across() and back(). */
	static Cube sheet(Bone bone, double cx, double cy, double cz, double w, double d, double rx, double ry, double rz, Painter p) {
		return bone.box(cx, cy, cz, w, 0.1, d, p).rot(rx, ry, rz);
	}

	/** Which plane a silhouette lies in for {@link #extrude}: how it is sliced, which way it is spanned, which way it is thick. */
	enum Plane {
		/** Upright, broad faces to the sides (an axe's blade): sliced up y, spanned along z, thick along x. */
		YZ,
		/** Upright, broad faces forward and back (a standing sword's blade): sliced up y, spanned along x, thick along z. */
		XY,
		/** Lying level (a sword lying flat): sliced along z, spanned along x, thick along y. */
		XZ
	}

	/**
	 * A solid cut to a silhouette (the user, 2026-10-25, of shapes drawn as an outline painted on a flat box, clear round it: "isn't filled
	 * in properly (they're 2D basically)"; such a box has faces only where it is, not round its outline, so seen edge-on or aslant it is two
	 * painted sheets with nothing between): a stack of boxes {@code thick} through about {@code mid} (x, y or z, as {@code plane} has it),
	 * one to each {@code step} from {@code from} to {@code to}, each spanning what {@code span} gives for its middle (the silhouette's least
	 * and greatest across there; null where there is none of it), so its outline has faces all round it. Painted by {@code p} as one piece:
	 * it should read where a texel lies (Texel#qx..qz), not how far across its own box.
	 */
	static List<Cube> extrude(Bone bone, Plane plane, double mid, double from, double to, double step, double thick,
			java.util.function.DoubleFunction<double[]> span, Painter p) {
		List<Cube> made = new ArrayList<>();
		for (double a = from; a < to - 1.0e-9; a += step) {
			double h = Math.min(step, to - a);
			double[] s = span.apply(a + h / 2);
			if (s == null || s[1] - s[0] < 0.25) continue;
			double w = s[1] - s[0];
			made.add(switch (plane) {
				case YZ -> bone.cube(mid - thick / 2, a, s[0], thick, h, w, p);
				case XY -> bone.cube(s[0], a, mid - thick / 2, w, h, thick, p);
				case XZ -> bone.cube(s[0], mid - thick / 2, a, w, thick, h, p);
			});
		}
		return made;
	}

	// --- Models ---

	static final class Cube {
		final double[] origin, size;
		Painter painter;
		double inflate;
		double[] pivot, rotation;
		/** Texels per pixel for this cube (0: the model's). */
		double tpp;
		final boolean[] skip = new boolean[6];
		Cube shares;
		boolean flipped;
		/** A flat blade: its painter's {@link Texel#v()} runs back along it, not up it. */
		boolean flat;
		final int[][] uv = new int[6][];
		Bone bone;
		double[] rest;

		Cube(double[] origin, double[] size, Painter painter) {
			this.origin = origin;
			this.size = size;
			this.painter = painter;
		}

		/** Turned (Bedrock degrees) about its own centre. */
		Cube rot(double rx, double ry, double rz) {
			return rot(rx, ry, rz, origin[0] + size[0] / 2, origin[1] + size[1] / 2, origin[2] + size[2] / 2);
		}

		Cube rot(double rx, double ry, double rz, double px, double py, double pz) {
			rotation = new double[] {rx, ry, rz};
			pivot = new double[] {px, py, pz};
			return this;
		}

		Cube skip(Face... faces) {
			for (Face f : faces) skip[f.ordinal()] = true;
			return this;
		}

		Cube only(Face... faces) {
			for (int i = 0; i < 6; i++) skip[i] = true;
			for (Face f : faces) skip[f.ordinal()] = false;
			return this;
		}

		Cube density(double tpp) {
			this.tpp = tpp;
			return this;
		}

		Cube inflate(double by) {
			inflate = by;
			return this;
		}

		/** The face's width and height in pixels. */
		double[] faceSize(Face f) {
			return switch (f) {
				case UP, DOWN -> new double[] {size[0], size[2]};
				case SIDE_A, SIDE_B -> new double[] {size[2], size[1]};
				default -> new double[] {size[0], size[1]};
			};
		}

		/** Whether the face is written: wanted, and broad enough to see (a plane's edges are not). */
		boolean has(Face f) {
			if (skip[f.ordinal()]) return false;
			double[] s = faceSize(f);
			return Math.min(s[0], s[1]) >= 0.25;
		}

		int[] texels(Face f, double modelTpp) {
			double d = tpp > 0 ? tpp : modelTpp;
			double[] s = faceSize(f);
			return new int[] {Math.max(1, (int) Math.round(s[0] * d)), Math.max(1, (int) Math.round(s[1] * d))};
		}

		void texel(Texel t, Face f, int x, int y, int w, int h) {
			double fx = (x + 0.5) / w, fy = (y + 0.5) / h, sx = size[0], sy = size[1], sz = size[2];
			double lx, ly, lz, nx = 0, ny = 0, nz = 0;
			switch (f) {
				case FRONT -> { lx = fx * sx; ly = (1 - fy) * sy; lz = 0; nz = -1; }
				case BACK -> { lx = (1 - fx) * sx; ly = (1 - fy) * sy; lz = sz; nz = 1; }
				case SIDE_A -> { lx = 0; ly = (1 - fy) * sy; lz = (1 - fx) * sz; nx = -1; }
				case SIDE_B -> { lx = sx; ly = (1 - fy) * sy; lz = fx * sz; nx = 1; }
				case UP -> { lx = fx * sx; ly = sy; lz = (1 - fy) * sz; ny = 1; }
				default -> { lx = fx * sx; ly = 0; lz = fy * sz; ny = -1; }
			}
			double[] p = apply(rest, origin[0] + lx, origin[1] + ly, origin[2] + lz), n = applyDir(rest, nx, ny, nz);
			t.f = f;
			t.x = x;
			t.y = y;
			t.w = w;
			t.h = h;
			t.fx = fx;
			t.fy = fy;
			t.lx = lx;
			t.ly = ly;
			t.lz = lz;
			t.px = p[0];
			t.py = p[1];
			t.pz = p[2];
			t.nx = n[0];
			t.ny = n[1];
			t.nz = n[2];
			t.cube = this;
		}
	}

	static final class Bone {
		final String name, parent;
		final double[] pivot, rotation;
		final List<Cube> cubes = new ArrayList<>();
		double[] rest;

		Bone(String name, String parent, double[] pivot, double[] rotation) {
			this.name = name;
			this.parent = parent;
			this.pivot = pivot;
			this.rotation = rotation;
		}

		/** A cube from its least corner. */
		Cube cube(double x, double y, double z, double w, double h, double d, Painter p) {
			Cube c = new Cube(new double[] {x, y, z}, new double[] {w, h, d}, p);
			c.bone = this;
			cubes.add(c);
			return c;
		}

		/** A cube about its centre. */
		Cube box(double cx, double cy, double cz, double w, double h, double d, Painter p) {
			return cube(cx - w / 2, cy - h / 2, cz - d / 2, w, h, d, p);
		}

		/** A cube about its centre painted as {@code like} and sharing its texture (the same size), mirrored if {@code flip}. */
		Cube like(Cube like, double cx, double cy, double cz, boolean flip) {
			Cube c = box(cx, cy, cz, like.size[0], like.size[1], like.size[2], like.painter);
			c.shares = like.shares != null ? like.shares : like;
			c.flipped = flip;
			c.tpp = like.tpp;
			System.arraycopy(like.skip, 0, c.skip, 0, 6);
			c.inflate = like.inflate;
			return c;
		}
	}

	static final class Model {
		final String id;
		final List<Bone> bones = new ArrayList<>();
		final List<Anim> anims = new ArrayList<>();
		final Map<String, Cube> shared = new java.util.HashMap<>();
		/** Texels per model pixel. */
		double tpp = 2;
		/** The render scale suggested for it in game. */
		double scale = 4;
		/**
		 * A creature's (Realms: a guardian), not a spell's shape: several loops of its own the game picks between (asleep, standing, walking), so
		 * none is carried into the rest nor its spins split off ({@link #carryTheLoop}, {@link #splitSpins}): what a loop doesn't key eases to rest.
		 */
		boolean creature;
		int texW, texH;

		Model(String id) { this.id = id; }

		Bone bone(String name, String parent, double px, double py, double pz, double rx, double ry, double rz) {
			for (Bone b : bones) if (b.name.equals(name)) throw new IllegalArgumentException(id + ": two bones named " + name);
			if (parent != null && get(parent) == null) throw new IllegalArgumentException(id + ": " + name + "'s parent " + parent + " comes first");
			Bone b = new Bone(name, parent, new double[] {px, py, pz}, new double[] {rx, ry, rz});
			bones.add(b);
			return b;
		}

		Bone bone(String name, String parent, double px, double py, double pz) { return bone(name, parent, px, py, pz, 0, 0, 0); }

		Bone get(String name) {
			for (Bone b : bones) if (b.name.equals(name)) return b;
			return null;
		}

		List<Cube> cubes() {
			List<Cube> all = new ArrayList<>();
			for (Bone b : bones) all.addAll(b.cubes);
			return all;
		}

		Anim anim(String name, double length, String loop) {
			Anim a = new Anim(name, length, loop);
			anims.add(a);
			return a;
		}

		/** Each bone's and cube's place at rest. */
		void settle() {
			for (Bone b : bones) {
				double[] parent = b.parent == null ? ident() : get(b.parent).rest;
				b.rest = mul(parent, about(b.pivot, b.rotation));
				for (Cube c : b.cubes) c.rest = c.rotation == null ? b.rest : mul(b.rest, about(c.pivot, c.rotation));
			}
		}

		/** The least and greatest corner over every cube at rest, in pixels. */
		double[][] extent() {
			double[] lo = {1e9, 1e9, 1e9}, hi = {-1e9, -1e9, -1e9};
			for (Cube c : cubes()) {
				for (int i = 0; i < 8; i++) {
					double x = c.origin[0] + ((i & 1) != 0 ? c.size[0] : 0), y = c.origin[1] + ((i & 2) != 0 ? c.size[1] : 0), z = c.origin[2] + ((i & 4) != 0 ? c.size[2] : 0);
					double[] p = apply(c.rest, x, y, z);
					for (int k = 0; k < 3; k++) {
						lo[k] = Math.min(lo[k], p[k]);
						hi[k] = Math.max(hi[k], p[k]);
					}
				}
			}
			return new double[][] {lo, hi};
		}
	}

	// --- Packing the faces onto the texture ---

	static final int[][] SIZES = {{64, 64}, {128, 64}, {128, 128}, {256, 128}, {256, 256}, {512, 256}, {512, 512}, {1024, 512}, {1024, 1024}};

	/** Shelves of faces, tallest first, on the smallest texture they fit. */
	static void pack(Model m) {
		List<Cube> cubes = m.cubes();
		List<int[]> regions = new ArrayList<>();
		for (int i = 0; i < cubes.size(); i++) {
			Cube c = cubes.get(i);
			if (c.shares != null) continue;
			for (Face f : Face.values()) {
				if (!c.has(f)) continue;
				int[] wh = c.texels(f, m.tpp);
				regions.add(new int[] {wh[0], wh[1], i, f.ordinal(), 0, 0});
			}
		}
		regions.sort((a, b) -> b[1] != a[1] ? b[1] - a[1] : b[0] - a[0]);
		for (int[] size : SIZES) {
			if (shelves(regions, size[0], size[1])) {
				m.texW = size[0];
				m.texH = size[1];
				for (int[] r : regions) cubes.get(r[2]).uv[r[3]] = new int[] {r[4], r[5], r[0], r[1]};
				for (Cube c : cubes) {
					if (c.shares == null) continue;
					for (int f = 0; f < 6; f++) c.uv[f] = c.has(Face.values()[f]) ? c.shares.uv[f] : null;
				}
				return;
			}
		}
		long area = 0;
		for (int[] r : regions) area += (long) r[0] * r[1];
		throw new IllegalStateException(m.id + ": its faces fit no texture up to 1024 x 1024 (" + area + " texels)");
	}

	static boolean shelves(List<int[]> regions, int width, int height) {
		int x = 0, y = 0, row = 0;
		for (int[] r : regions) {
			if (r[0] > width) return false;
			if (x + r[0] > width) {
				x = 0;
				y += row;
				row = 0;
			}
			if (y + r[1] > height) return false;
			r[4] = x;
			r[5] = y;
			x += r[0];
			row = Math.max(row, r[1]);
		}
		return true;
	}

	static BufferedImage paint(Model m) {
		BufferedImage img = new BufferedImage(m.texW, m.texH, BufferedImage.TYPE_INT_ARGB);
		Texel t = new Texel();
		for (Cube c : m.cubes()) {
			if (c.shares != null) continue;
			for (Face f : Face.values()) {
				int[] r = c.uv[f.ordinal()];
				if (r == null) continue;
				for (int y = 0; y < r[3]; y++) {
					for (int x = 0; x < r[2]; x++) {
						c.texel(t, f, x, y, r[2], r[3]);
						img.setRGB(r[0] + x, r[1] + y, c.painter.paint(t));
					}
				}
			}
		}
		return img;
	}

	/** The base texture and the glow mask (null if nothing glows), from the painted pixels' alphas (see the class's note). */
	static BufferedImage[] split(BufferedImage painted) {
		int w = painted.getWidth(), h = painted.getHeight();
		BufferedImage base = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB), mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		boolean any = false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int p = painted.getRGB(x, y), a = p >>> 24;
				if (a == 0) continue;
				if (glowing(p)) {
					int alpha = a == 254 ? 255 : a;
					base.setRGB(x, y, alpha << 24 | p & 0xFFFFFF);
					mask.setRGB(x, y, alpha << 24 | p & 0xFFFFFF);
					any = true;
				} else {
					base.setRGB(x, y, p);
				}
			}
		}
		return new BufferedImage[] {base, any ? mask : null};
	}

	// --- Animations ---

	/** An animation's keyframes: bone, then channel ("rotation", "position", "scale"), then time to value. */
	static final class Anim {
		final String name;
		final double length;
		/** "true" (a loop), "hold_on_last_frame", or null (played once). */
		final String loop;
		final Map<String, Map<String, TreeMap<Double, double[]>>> bones = new LinkedHashMap<>();

		Anim(String name, double length, String loop) {
			this.name = name;
			this.length = length;
			this.loop = loop;
		}

		Anim key(String bone, String channel, double time, double x, double y, double z) {
			time = Math.round(time * 1000) / 1000.0;
			bones.computeIfAbsent(bone, k -> new LinkedHashMap<>()).computeIfAbsent(channel, k -> new TreeMap<>()).put(time, new double[] {x, y, z});
			return this;
		}

		Anim rot(String bone, double time, double x, double y, double z) { return key(bone, "rotation", time, x, y, z); }
		Anim pos(String bone, double time, double x, double y, double z) { return key(bone, "position", time, x, y, z); }
		Anim scale(String bone, double time, double x, double y, double z) { return key(bone, "scale", time, x, y, z); }
		Anim scale(String bone, double time, double s) { return key(bone, "scale", time, s, s, s); }

		/** {@code steps + 1} keys over [t0, t1] of the channel's value {@code f(t)}. */
		Anim sample(String bone, String channel, double t0, double t1, int steps, java.util.function.DoubleFunction<double[]> f) {
			for (int i = 0; i <= steps; i++) {
				double t = t0 + (t1 - t0) * i / steps;
				double[] v = f.apply(t);
				key(bone, channel, t, v[0], v[1], v[2]);
			}
			return this;
		}
	}

	/**
	 * Each of a model's other animations keys every bone its loop moves, and an enter or a move played once ends where the loop begins.
	 * GeckoLib eases a bone no playing animation keys back to rest (over 5 ticks) and holds it there, goes from an enter's last frame
	 * straight to its loop's first, and eases a move's last frame into the loop's first over 3 ticks; so the dragon's tail and hind half
	 * went stiff and straight while it roared, a tree that grew a few degrees off its sway twitched as it began to sway, and a dragon's legs
	 * flicked back into its swim (2026-10-24, found checking the animations in game). What a move or an enter leaves out goes on with the
	 * loop's own motion, timed to come to the loop's first frame as it ends; what it keys itself comes to that frame at its end (unless it
	 * holds its last frame).
	 */
	static void carryTheLoop(Model m) {
		Anim loop = null;
		for (Anim a : m.anims) {
			if (!"true".equals(a.loop) || a.name.endsWith(SPIN)) continue;
			if (loop != null) throw new IllegalStateException(m.id + " has two loops, " + loop.name + " and " + a.name);
			loop = a;
		}
		if (loop == null) return;
		for (Anim a : m.anims) {
			if (a == loop || a.name.endsWith(SPIN)) continue;
			for (Map.Entry<String, Map<String, TreeMap<Double, double[]>>> bone : loop.bones.entrySet()) {
				for (Map.Entry<String, TreeMap<Double, double[]>> channel : bone.getValue().entrySet()) {
					Map<String, TreeMap<Double, double[]>> own = a.bones.get(bone.getKey());
					if (own != null && own.containsKey(channel.getKey())) {
						if (a.loop == null) {
							double[] start = phase(channel.getValue(), 0);
							a.key(bone.getKey(), channel.getKey(), a.length, start[0], start[1], start[2]);
						}
						continue;
					}
					carry(a, bone.getKey(), channel.getKey(), channel.getValue(), loop.length);
				}
			}
		}
	}

	/** The name's ending of a loop's spins, played on a controller of their own ({@link #splitSpins}). */
	static final String SPIN = "_spin";

	/**
	 * A loop's spins (a rotation that turns on round the loop, a wheel's or a vortex's, not a sway that comes back where it began) moved out of
	 * it into an animation of their own, "<loop>_spin", that its shape plays on a second controller from the first (SpellConstruct): so a move
	 * played in the middle of the loop leaves them turning on. Carried into the move ({@link #carryTheLoop}), they were eased back to the loop's
	 * start over 3 ticks, wheels spun backwards whole turns in a moment (a storm cloud's eye three and a half at each lightning flash, found in
	 * game, 2026-10-24). Only a spin no other animation keys of its own; every model has one, empty where nothing spins (GeckoLib wants what it
	 * is asked for to be there).
	 */
	static void splitSpins(Model m) {
		Anim loop = null;
		for (Anim a : m.anims) if ("true".equals(a.loop) && !a.name.endsWith(SPIN)) loop = a;
		if (loop == null) return;
		Anim spin = new Anim(loop.name + SPIN, loop.length, "true");
		for (java.util.Iterator<Map.Entry<String, Map<String, TreeMap<Double, double[]>>>> it = loop.bones.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<String, Map<String, TreeMap<Double, double[]>>> bone = it.next();
			TreeMap<Double, double[]> rotation = bone.getValue().get("rotation");
			if (rotation == null || rotation.isEmpty()) continue;
			double[] first = phase(rotation, 0), last = phase(rotation, loop.length);
			boolean turns = false;
			for (int i = 0; i < 3; i++) if (Math.abs(last[i] - first[i]) >= 15) turns = true;
			if (!turns) continue;
			// Not one whose turning goes with its moving or swelling (a relay's tier stepping into the next one's place, its seam in all of them):
			// on a controller of its own it would come round out of step with them (its loop begins after its enter and afresh after every move).
			boolean coupled = false;
			for (Map.Entry<String, TreeMap<Double, double[]>> channel : bone.getValue().entrySet()) {
				if (channel.getKey().equals("rotation")) continue;
				double[] s = phase(channel.getValue(), 0), e = phase(channel.getValue(), loop.length);
				double tolerance = channel.getKey().equals("scale") ? 0.01 : 0.1;
				for (int i = 0; i < 3; i++) if (Math.abs(e[i] - s[i]) > tolerance) coupled = true;
			}
			if (coupled) continue;
			boolean keyedElsewhere = false;
			for (Anim a : m.anims) {
				if (a != loop && a.bones.containsKey(bone.getKey()) && a.bones.get(bone.getKey()).containsKey("rotation")) keyedElsewhere = true;
			}
			if (keyedElsewhere) continue;
			spin.bones.computeIfAbsent(bone.getKey(), k -> new LinkedHashMap<>()).put("rotation", rotation);
			bone.getValue().remove("rotation");
			if (bone.getValue().isEmpty()) it.remove();
		}
		m.anims.add(spin);
	}

	/** Keys {@code into}'s {@code bone}'s {@code channel} with a loop's ({@code keys}, {@code period} long), shifted to come to its start as {@code into} ends. */
	static void carry(Anim into, String bone, String channel, TreeMap<Double, double[]> keys, double period) {
		double length = into.length;
		TreeMap<Double, double[]> out = new TreeMap<>();
		out.put(0.0, phase(keys, ((-length) % period + period) % period));
		// Each of the loop's keys, wherever in it its time falls (t - length, round the loop).
		for (Map.Entry<Double, double[]> k : keys.entrySet()) {
			for (int n = (int) Math.ceil(k.getKey() / period - 1e-9); length + k.getKey() - n * period >= -1e-9; n++) {
				double t = length + k.getKey() - n * period;
				if (t <= length + 1e-9) out.put(Math.max(0, Math.min(length, t)), k.getValue());
			}
		}
		// Where the loop comes round within it (and at its end, where it is just coming round): a step from its end to its start, not a
		// sweep back (a wheel turned one spoke round steps back to its start unseen).
		double[] end = phase(keys, period), start = phase(keys, 0);
		boolean step = Math.abs(end[0] - start[0]) + Math.abs(end[1] - start[1]) + Math.abs(end[2] - start[2]) > 1e-6;
		for (int n = 0; length - n * period > 1e-9; n++) {
			double t = length - n * period;
			out.put(t, start);
			if (step) out.put(Math.max(0, t - 0.001), end);
		}
		for (Map.Entry<Double, double[]> k : out.entrySet()) into.key(bone, channel, k.getKey(), k.getValue()[0], k.getValue()[1], k.getValue()[2]);
	}

	/** A channel's value {@code t} seconds in (between its keys, straight; before the first and after the last, held). */
	static double[] phase(TreeMap<Double, double[]> keys, double t) {
		Map.Entry<Double, double[]> lo = keys.floorEntry(t), hi = keys.ceilingEntry(t);
		if (lo == null) return hi.getValue();
		if (hi == null || hi.getKey().equals(lo.getKey())) return lo.getValue();
		double f = (t - lo.getKey()) / (hi.getKey() - lo.getKey());
		double[] a = lo.getValue(), b = hi.getValue();
		return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
	}

	/** Eases a fraction in and out. */
	static double ease(double t) {
		t = clamp01(t);
		return t * t * (3 - 2 * t);
	}

	static double easeOut(double t) {
		t = clamp01(t);
		return 1 - (1 - t) * (1 - t);
	}

	static double easeIn(double t) {
		t = clamp01(t);
		return t * t;
	}

	// --- Gaits (the user's ask, 2026-10-25: "look at how tigers actually move and how horses actually gallop") ---

	/**
	 * Where a leg is in its own stride, {@code phase} of the beast's (0 to 1): its foot set down at {@code down} of the stride and on the ground
	 * {@code duty} of it. Its own stride runs from its foot touching down (0) through its stance (to {@code duty}) and its swing (to 1).
	 */
	static double stride(double phase, double down) {
		double s = (phase - down) % 1;
		return s < 0 ? s + 1 : s;
	}

	/**
	 * A joint's angle {@code s} of the way round its own stride ({@link #stride}), through {@code keys} ({s, degrees} in order, from 0 below 1),
	 * on a looping Catmull-Rom curve: no stop at any key, its last joined smoothly to its first round the stride's end.
	 */
	static double keyed(double s, double[][] keys) {
		int n = keys.length;
		s = ((s % 1) + 1) % 1;
		int i = n - 1;
		for (int k = 0; k < n; k++) if (keys[k][0] <= s) i = k;
		// Its neighbours round the loop, their times carried past its ends.
		double t1 = keys[i][0], v1 = keys[i][1];
		double t2 = i + 1 < n ? keys[i + 1][0] : keys[0][0] + 1, v2 = keys[(i + 1) % n][1];
		double t0 = i - 1 >= 0 ? keys[i - 1][0] : keys[n - 1][0] - 1, v0 = keys[(i - 1 + n) % n][1];
		double t3 = i + 2 < n ? keys[i + 2][0] : keys[(i + 2) % n][0] + 1, v3 = keys[(i + 2) % n][1];
		if (s < t1) s += 1;
		double h = t2 - t1, u = (s - t1) / h;
		double m1 = (v2 - v0) / (t2 - t0) * h, m2 = (v3 - v1) / (t3 - t1) * h;
		double u2 = u * u, u3 = u2 * u;
		return (2 * u3 - 3 * u2 + 1) * v1 + (u3 - 2 * u2 + u) * m1 + (-2 * u3 + 3 * u2) * v2 + (u3 - u2) * m2;
	}

	// --- Writing ---

	static String num(double v) {
		if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
		String s = String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
		return s.equals("-0") ? "0" : s;
	}

	static String vec(double[] v) { return "[" + num(v[0]) + ", " + num(v[1]) + ", " + num(v[2]) + "]"; }

	static String geo(Model m) {
		double[][] e = m.extent();
		double width = Math.ceil(2 * Math.max(Math.max(Math.abs(e[0][0]), Math.abs(e[1][0])), Math.max(Math.abs(e[0][2]), Math.abs(e[1][2]))) / 16 + 1);
		double height = Math.ceil((e[1][1] - e[0][1]) / 16 + 1);
		double offset = (e[1][1] + e[0][1]) / 2 / 16;
		StringBuilder s = new StringBuilder();
		s.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
		s.append("\t\t\t\"description\": {\n\t\t\t\t\"identifier\": \"geometry.spell_").append(m.id).append("\",\n");
		s.append("\t\t\t\t\"texture_width\": ").append(m.texW).append(",\n\t\t\t\t\"texture_height\": ").append(m.texH).append(",\n");
		s.append("\t\t\t\t\"visible_bounds_width\": ").append(num(width)).append(",\n\t\t\t\t\"visible_bounds_height\": ").append(num(height)).append(",\n");
		s.append("\t\t\t\t\"visible_bounds_offset\": [0, ").append(num(Math.round(offset * 100) / 100.0)).append(", 0]\n\t\t\t},\n");
		s.append("\t\t\t\"bones\": [\n");
		for (int i = 0; i < m.bones.size(); i++) {
			Bone b = m.bones.get(i);
			s.append("\t\t\t\t{\n\t\t\t\t\t\"name\": \"").append(b.name).append("\",\n");
			if (b.parent != null) s.append("\t\t\t\t\t\"parent\": \"").append(b.parent).append("\",\n");
			s.append("\t\t\t\t\t\"pivot\": ").append(vec(b.pivot));
			if (b.rotation[0] != 0 || b.rotation[1] != 0 || b.rotation[2] != 0) s.append(",\n\t\t\t\t\t\"rotation\": ").append(vec(b.rotation));
			if (!b.cubes.isEmpty()) {
				s.append(",\n\t\t\t\t\t\"cubes\": [\n");
				for (int j = 0; j < b.cubes.size(); j++) {
					Cube c = b.cubes.get(j);
					s.append("\t\t\t\t\t\t{\"origin\": ").append(vec(c.origin)).append(", \"size\": ").append(vec(c.size));
					if (c.inflate != 0) s.append(", \"inflate\": ").append(num(c.inflate));
					if (c.rotation != null) s.append(", \"pivot\": ").append(vec(c.pivot)).append(", \"rotation\": ").append(vec(c.rotation));
					s.append(", \"uv\": {");
					boolean first = true;
					for (Face f : Face.values()) {
						int[] r = c.uv[f.ordinal()];
						if (r == null) continue;
						if (!first) s.append(", ");
						first = false;
						if (c.flipped) s.append("\"").append(f.key).append("\": {\"uv\": [").append(r[0] + r[2]).append(", ").append(r[1]).append("], \"uv_size\": [").append(-r[2]).append(", ").append(r[3]).append("]}");
						else s.append("\"").append(f.key).append("\": {\"uv\": [").append(r[0]).append(", ").append(r[1]).append("], \"uv_size\": [").append(r[2]).append(", ").append(r[3]).append("]}");
					}
					s.append("}}").append(j + 1 < b.cubes.size() ? ",\n" : "\n");
				}
				s.append("\t\t\t\t\t]");
			}
			s.append("\n\t\t\t\t}").append(i + 1 < m.bones.size() ? ",\n" : "\n");
		}
		s.append("\t\t\t]\n\t\t}\n\t]\n}\n");
		return s.toString();
	}

	static String animations(List<Anim> anims) {
		StringBuilder s = new StringBuilder("{\n\t\"format_version\": \"1.8.0\",\n\t\"animations\": {\n");
		for (int i = 0; i < anims.size(); i++) {
			Anim a = anims.get(i);
			s.append("\t\t\"").append(a.name).append("\": {\n");
			if ("true".equals(a.loop)) s.append("\t\t\t\"loop\": true,\n");
			else if (a.loop != null) s.append("\t\t\t\"loop\": \"").append(a.loop).append("\",\n");
			s.append("\t\t\t\"animation_length\": ").append(num(a.length)).append(",\n\t\t\t\"bones\": {\n");
			int bi = 0;
			for (Map.Entry<String, Map<String, TreeMap<Double, double[]>>> bone : a.bones.entrySet()) {
				s.append("\t\t\t\t\"").append(bone.getKey()).append("\": {\n");
				int ci = 0;
				for (Map.Entry<String, TreeMap<Double, double[]>> channel : bone.getValue().entrySet()) {
					s.append("\t\t\t\t\t\"").append(channel.getKey()).append("\": {\n");
					int ki = 0;
					for (Map.Entry<Double, double[]> key : channel.getValue().entrySet()) {
						s.append("\t\t\t\t\t\t\"").append(num(key.getKey())).append("\": ").append(vec(key.getValue()))
								.append(++ki < channel.getValue().size() ? ",\n" : "\n");
					}
					s.append("\t\t\t\t\t}").append(++ci < bone.getValue().size() ? ",\n" : "\n");
				}
				s.append("\t\t\t\t}").append(++bi < a.bones.size() ? ",\n" : "\n");
			}
			s.append("\t\t\t}\n\t\t}").append(i + 1 < anims.size() ? ",\n" : "\n");
		}
		s.append("\t}\n}\n");
		return s.toString();
	}

	static void write(Path file, String text) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, text, StandardCharsets.UTF_8);
	}

	static void write(Path file, BufferedImage img) throws IOException {
		Files.createDirectories(file.getParent());
		ImageIO.write(img, "png", file.toFile());
	}

	/** Builds, packs, paints and writes one spell under {@code assets}. */
	static void write(Path assets, Model m) throws IOException {
		write(assets, m, "spell");
	}

	/** Builds, packs, paints and writes one model under {@code assets}, in its {@code folder} of each kind ("spell"; a primordial beast's "demon_beast"). */
	static void write(Path assets, Model m, String folder) throws IOException {
		m.settle();
		for (Anim a : m.anims) for (String bone : a.bones.keySet()) if (m.get(bone) == null) throw new IllegalStateException(m.id + "'s " + a.name + " moves no bone " + bone);
		if (!m.creature) {
			splitSpins(m);
			carryTheLoop(m);
		}
		pack(m);
		BufferedImage[] painted = split(paint(m));
		write(assets.resolve("textures/entity/" + folder + "/" + m.id + ".png"), painted[0]);
		Path mask = assets.resolve("textures/entity/" + folder + "/" + m.id + "_glowmask.png");
		if (painted[1] != null) write(mask, painted[1]);
		else Files.deleteIfExists(mask);
		write(assets.resolve("geo/" + folder + "/" + m.id + ".geo.json"), geo(m));
		write(assets.resolve("animations/" + folder + "/" + m.id + ".animation.json"), animations(m.anims));
		double[][] e = m.extent();
		int cubes = m.cubes().size();
		System.out.printf(Locale.ROOT, "%-18s %3d bones %4d cubes, texture %4d x %-4d  %.0f x %.0f x %.0f px = %.1f x %.1f x %.1f blocks at x%s%n", m.id, m.bones.size(), cubes, m.texW, m.texH,
				e[1][0] - e[0][0], e[1][1] - e[0][1], e[1][2] - e[0][2], (e[1][0] - e[0][0]) / 16 * m.scale, (e[1][1] - e[0][1]) / 16 * m.scale, (e[1][2] - e[0][2]) / 16 * m.scale, num(m.scale));
	}

	interface Build {
		void build(Model m);
	}

	static final Object[][] SPELLS = {
			{"azure_dragon", (Build) Wood::azureDragon}, {"dao_tree", (Build) Wood::daoTree},
			{"vermilion_bird", (Build) Fire::vermilionBird}, {"one_flame_seed", (Build) Fire::oneFlameSeed}, {"one_flame_pillar", (Build) Fire::oneFlamePillar},
			{"qilin", (Build) Earth::qilin}, {"mountain_seal", (Build) Earth::mountainSeal},
			{"black_tortoise", (Build) Water::blackTortoise}, {"hundred_rivers", (Build) Water::hundredRivers},
			{"white_tiger", (Build) Metal::whiteTiger}, {"myriad_swords", (Build) Metal::myriadSwords}, {"colossal_sword", (Build) Metal::colossalSword},
			{"procession", (Build) Ghost::procession}, {"ghost_gate", (Build) Ghost::ghostGate},
			{"heaven_demon_avatar", (Build) HeavenDemon::avatar}, {"avatar_palm", (Build) HeavenDemon::avatarPalm}, {"avatar_claw", (Build) HeavenDemon::avatarClaw},
			// Void Shattering's (Void).
			{"azure_thunder_cloud", (Build) Void::azureThunderCloud}, {"void_roots", (Build) Void::voidRoots},
			{"golden_crow", (Build) Void::goldenCrow}, {"void_sun", (Build) Void::voidSun},
			{"buzhou_pillar", (Build) Void::buzhouPillar}, {"earth_vein_lock", (Build) Void::earthVeinLock},
			{"weak_water_fall", (Build) Void::weakWaterFall}, {"kun", (Build) Void::kun},
			{"severing_light", (Build) Void::severingLight}, {"sword_prison", (Build) Void::swordPrison},
			{"nine_nethers", (Build) Void::nineNethers}, {"naihe_bridge", (Build) Void::naiheBridge}, {"heaven_demon_fist", (Build) Void::heavenDemonFist},
			// Sacred Vessel's (Vessel).
			{"swallowing_gourd", (Build) Vessel::swallowingGourd}, {"bodhi_branch", (Build) Vessel::bodhiBranch},
			{"wind_fire_wheel", (Build) Vessel::windFireWheel}, {"trigram_furnace", (Build) Vessel::trigramFurnace},
			{"xirang", (Build) Vessel::xirang}, {"nine_cauldron", (Build) Vessel::nineCauldron},
			{"sea_pearl", (Build) Vessel::seaPearl}, {"ocean_vase", (Build) Vessel::oceanVase},
			{"binding_ring", (Build) Vessel::bindingRing}, {"dragon_scissors", (Build) Vessel::dragonScissors},
			{"karma_mirror", (Build) Vessel::karmaMirror}, {"soul_pagoda", (Build) Vessel::soulPagoda}, {"heaven_demon_bell", (Build) Vessel::heavenDemonBell},
			// Entering Nirvana's (Nirvana).
			{"rebirth_seed", (Build) Nirvana::rebirthSeed}, {"season_wheel", (Build) Nirvana::seasonWheel},
			{"undying_ember", (Build) Nirvana::undyingEmber}, {"heaven_pyre", (Build) Nirvana::heavenPyre},
			{"gravestone", (Build) Nirvana::gravestone}, {"standing_stone", (Build) Nirvana::standingStone},
			{"frozen_moon", (Build) Nirvana::frozenMoon}, {"ice_moon", (Build) Nirvana::iceMoon},
			{"sword_tomb", (Build) Nirvana::swordTomb}, {"rebirth_sword", (Build) Nirvana::rebirthSword},
			{"white_impermanence", (Build) Nirvana::whiteImpermanence}, {"black_impermanence", (Build) Nirvana::blackImpermanence},
			{"life_death_book", (Build) Nirvana::lifeDeathBook}, {"heaven_demon_eclipse", (Build) Nirvana::heavenDemonEclipse},
			// True Immortal's (Immortal).
			{"primordial_cyclone", (Build) Immortal::primordialCyclone}, {"creation_seed", (Build) Immortal::creationSeed},
			{"samadhi_pillars", (Build) Immortal::samadhiPillars}, {"chaos_flower", (Build) Immortal::chaosFlower},
			{"houtu_hand", (Build) Immortal::houtuHand}, {"falling_continent", (Build) Immortal::fallingContinent},
			{"primal_glacier", (Build) Immortal::primalGlacier}, {"primordial_sea", (Build) Immortal::primordialSea},
			{"heaven_slaying_sword", (Build) Immortal::heavenSlayingSword}, {"pangu_axe", (Build) Immortal::panguAxe},
			{"fengdu_wall", (Build) Immortal::fengduWall}, {"soul_helix", (Build) Immortal::soulHelix},
			{"chaos_sphere", (Build) Immortal::chaosSphere},
			// Four Axis's, made over as the great spells' are (Axis).
			{"falling_sun", (Build) Axis::fallingSun}, {"nirvana_phoenix", (Build) Axis::nirvanaPhoenix}, {"binding_vines", (Build) Axis::bindingVines},
			{"earth_golem", (Build) Axis::earthGolem}, {"mountain_talisman", (Build) Axis::mountainTalisman},
			{"ice_coffin", (Build) Axis::iceCoffin}, {"severing_sword", (Build) Axis::severingSword},
			// Heavenly Being's and Nascent Soul's, made over as the great spells' are (Axis).
			{"descent_phoenix", (Build) Axis::descentPhoenix}, {"blazing_meteor", (Build) Axis::blazingMeteor}, {"falling_peak", (Build) Axis::fallingPeak},
			{"sword_domain", (Build) Axis::swordDomain}, {"piercing_bamboo", (Build) Axis::piercingBamboo}, {"rebirth_lotus", (Build) Axis::rebirthLotus},
			{"thorn_forest", (Build) Axis::thornForest},
			// Core Formation's, made over (Axis).
			{"sword_ring", (Build) Axis::swordRing}, {"rain_sword", (Build) Axis::rainSword}, {"earth_dragon", (Build) Axis::earthDragon},
			// Both paths' below Dao Integration (Axis).
			{"hd_palm", (Build) Axis::hdPalm}, {"hd_mantle", (Build) Axis::hdMantle}, {"devouring_seal", (Build) Axis::devouringSeal}, {"hd_claw", (Build) Axis::hdClaw},
			{"hd_as_one", (Build) Axis::hdAsOne},
			// The ghost path's below Dao Integration (Underworld).
			{"ghost_kindle", (Build) Underworld::ghostKindle}, {"ghost_bolt", (Build) Underworld::ghostBolt}, {"soul_wraith", (Build) Underworld::soulWraith},
			{"ghost_wail", (Build) Underworld::ghostWail}, {"springs_tide", (Build) Underworld::springsTide}, {"marrow_ward", (Build) Underworld::marrowWard},
			{"core_burst", (Build) Underworld::coreBurst}, {"kings_decree", (Build) Underworld::kingsDecree}, {"nether_pool", (Build) Underworld::netherPool},
			{"yama_tablet", (Build) Underworld::yamaTablet}, {"hell_gate", (Build) Underworld::hellGate}, {"samsara_wheel", (Build) Underworld::samsaraWheel},
			{"six_paths", (Build) Underworld::sixPaths},
	};

	/**
	 * The spirit beasts as they were first made, true beasts (scaled, feathered, furred), before the spells' beasts were made of their
	 * elements (the user, 2026-10-24: "keep the current models in storage though for later"). Written only by {@code --stored}, laid out as
	 * the mod's assets are (geo/spell, animations/spell, textures/entity/spell), so a stored beast goes back by copying its files in.
	 */
	static final Object[][] STORED = {
			{"azure_dragon", (Build) Wood::azureDragonBeast}, {"vermilion_bird", (Build) Fire::vermilionBirdBeast}, {"qilin", (Build) Earth::qilinBeast},
			{"black_tortoise", (Build) Water::blackTortoiseBeast}, {"white_tiger", (Build) Metal::whiteTigerBeast},
	};

	/**
	 * The Immortal Realm's primordial beasts made here (2026-10-26, the user: "overhaul the dragon model"), written as a beast's are
	 * (geo/demon_beast, animations/demon_beast, textures/entity/demon_beast) by {@code --primordial}. The Kun-Peng's Kun is the spell's own
	 * ({@code kun}, Void), drawn from the spell's files.
	 */
	static final Object[][] PRIMORDIAL = {
			{"true_dragon", (Build) TrueDragon::trueDragon},
	};

	/**
	 * The secret realms' guardians (2026-10-31, Realms), written into geo/realm, animations/realm and textures/entity/realm by {@code --realm}.
	 */
	static final Object[][] REALM = {
			{"vine_keeper", (Build) Realms::vineKeeper}, {"bronze_puppet", (Build) Realms::bronzePuppet},
			{"sword_wraith", (Build) Realms::swordWraith}, {"bone_soldier", (Build) Realms::boneSoldier},
			{"battle_spirit", (Build) Realms::battleSpirit}, {"general_spirit", (Build) Realms::generalSpirit},
			{"army_spirit", (Build) Realms::armySpirit}, {"dragon_echo", (Build) Realms::dragonEcho},
			{"master_puppet", (Build) Realms::masterPuppet}, {"ferryman", (Build) Realms::ferryman}, {"drowned_shade", (Build) Realms::drownedShade},
			{"mirror_shade", (Build) Realms::mirrorShade}, {"holy_remnant", (Build) Realms::holyRemnant},
			{"decayed_guard", (Build) Realms::decayedGuard}, {"decayed_immortal", (Build) Realms::decayedImmortal},
	};

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("java SpellArt.java <the mod's src/main/resources/assets/defying-the-heavens> [ids...]");
			System.err.println("java SpellArt.java --stored <dir, e.g. tools/spell-art/stored> [ids...]   (the stored true beasts)");
			System.err.println("java SpellArt.java --primordial <the mod's assets> [ids...]   (the primordial beasts made here)");
			System.err.println("java SpellArt.java --realm <the mod's assets> [ids...]   (the secret realms' guardians)");
			System.exit(1);
		}
		boolean stored = args[0].equals("--stored"), primordial = args[0].equals("--primordial"), realm = args[0].equals("--realm");
		int first = stored || primordial || realm ? 1 : 0;
		Path assets = Path.of(args[first]);
		Set<String> only = new LinkedHashSet<>();
		for (int i = first + 1; i < args.length; i++) only.add(args[i]);
		for (Object[] spell : stored ? STORED : primordial ? PRIMORDIAL : realm ? REALM : SPELLS) {
			String id = (String) spell[0];
			if (!only.isEmpty() && !only.contains(id)) continue;
			Model m = new Model(id);
			((Build) spell[1]).build(m);
			write(assets, m, primordial ? "demon_beast" : realm ? "realm" : "spell");
		}
	}
}
