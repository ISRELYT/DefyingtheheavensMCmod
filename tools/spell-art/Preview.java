import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Draws a spell's written GeckoLib model the way GeckoLib 4.8.4 would, to look at it without the game: the .geo.json baked as
 * BakedModelFactory bakes it (x mirrored, per-face or box UV, GeoQuad's corners), posed as the AnimationProcessor poses it at a time of one of
 * its animations (rotations added to the rest, x and y negated), each bone turned as RenderUtils turns it (Z * Y * X about its pivot,
 * parent to child), the entity's own 180 degree turn, then the base texture drawn see-through with the entity shader's two lights, and
 * the glow mask's pixels taken out of it and drawn again full bright (AutoGlowingTexture). Orthographic, on a night sky, with a grid of
 * 4-block squares (at the suggested render scale) under it.
 * <p>
 * {@code java Preview.java <assets> <out dir> <id> [views=front,face,side,top,back,below,low] [anim=<name>:<t>,<t>...] [animview=front]
 * [at=<name>:<t>] [focus=<bone>,...] [scale=4] [w=480] [h=360] [cols=2] [tag=<name>]} writes {@code <out dir>/<id>[_<tag>].png}: the views
 * asked for (posed at {@code at}, else at rest), then the animation's frames (seen from {@code animview}); {@code focus} fits each view to
 * those bones (a name ending in * : and all that hangs from it). Views: front, face, side, left, frontl, back, top, high, below, low.
 * <p>
 * {@code gif=<name>:<seconds>:<fps>} writes {@code <out dir>/<id>_<name>.gif} instead: that animation played from its start for that long,
 * looping, seen from {@code animview}, every frame fitted alike (to show a move or a loop as it plays in game).
 */
public class Preview {
	// --- A little JSON ---

	static final class Json {
		final String s;
		int i;

		Json(String s) { this.s = s; }

		void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

		Object value() {
			ws();
			char c = s.charAt(i);
			if (c == '{') {
				Map<String, Object> m = new LinkedHashMap<>();
				i++;
				ws();
				if (s.charAt(i) == '}') { i++; return m; }
				while (true) {
					ws();
					String k = (String) value();
					ws();
					i++; // :
					m.put(k, value());
					ws();
					if (s.charAt(i++) == '}') return m;
				}
			}
			if (c == '[') {
				List<Object> l = new ArrayList<>();
				i++;
				ws();
				if (s.charAt(i) == ']') { i++; return l; }
				while (true) {
					l.add(value());
					ws();
					if (s.charAt(i++) == ']') return l;
				}
			}
			if (c == '"') {
				StringBuilder b = new StringBuilder();
				i++;
				while (s.charAt(i) != '"') {
					if (s.charAt(i) == '\\') i++;
					b.append(s.charAt(i++));
				}
				i++;
				return b.toString();
			}
			if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
			if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
			if (s.startsWith("null", i)) { i += 4; return null; }
			int j = i;
			while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
			return Double.parseDouble(s.substring(j, i));
		}
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> obj(Object o) { return (Map<String, Object>) o; }

	@SuppressWarnings("unchecked")
	static List<Object> arr(Object o) { return (List<Object>) o; }

	static double[] vec(Object o, double[] fallback) {
		if (o == null) return fallback;
		List<Object> l = arr(o);
		double[] v = new double[l.size()];
		for (int k = 0; k < v.length; k++) v[k] = ((Number) l.get(k)).doubleValue();
		return v;
	}

	// --- The model, baked as GeckoLib bakes it: GeckoLib's space (x mirrored), in blocks ---

	static final class Quad {
		final double[][] v = new double[4][], uv = new double[4][];
		double[] n;
	}

	static final class GCube {
		double[] pivot, rot;
		final List<Quad> quads = new ArrayList<>();
	}

	static final class GBone {
		String name;
		GBone parent;
		double[] pivot, rot;
		final List<GCube> cubes = new ArrayList<>();
		final List<GBone> children = new ArrayList<>();
	}

	static int texW, texH;

	static List<GBone> load(Path geo) throws Exception {
		Map<String, Object> root = obj(new Json(Files.readString(geo, StandardCharsets.UTF_8)).value());
		Map<String, Object> g = obj(arr(root.get("minecraft:geometry")).get(0));
		Map<String, Object> desc = obj(g.get("description"));
		texW = ((Number) desc.get("texture_width")).intValue();
		texH = ((Number) desc.get("texture_height")).intValue();
		Map<String, GBone> byName = new LinkedHashMap<>();
		List<GBone> tops = new ArrayList<>();
		for (Object bo : arr(g.get("bones"))) {
			Map<String, Object> b = obj(bo);
			GBone bone = new GBone();
			bone.name = (String) b.get("name");
			double[] p = vec(b.get("pivot"), new double[3]), r = vec(b.get("rotation"), new double[3]);
			bone.pivot = new double[] {-p[0], p[1], p[2]};
			bone.rot = new double[] {Math.toRadians(-r[0]), Math.toRadians(-r[1]), Math.toRadians(r[2])};
			if (b.get("cubes") != null) for (Object co : arr(b.get("cubes"))) bone.cubes.add(cube(obj(co)));
			byName.put(bone.name, bone);
			Object parent = b.get("parent");
			if (parent != null) {
				bone.parent = byName.get(parent);
				bone.parent.children.add(bone);
			} else {
				tops.add(bone);
			}
		}
		return tops;
	}

	static final String[] DIRS = {"west", "east", "north", "south", "up", "down"};
	static final double[][] NORMALS = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, -1, 0}};

	static GCube cube(Map<String, Object> c) {
		double[] o = vec(c.get("origin"), null), s = vec(c.get("size"), null), p = vec(c.get("pivot"), new double[3]), r = vec(c.get("rotation"), new double[3]);
		double inf = c.get("inflate") == null ? 0 : ((Number) c.get("inflate")).doubleValue() / 16;
		GCube g = new GCube();
		g.pivot = new double[] {-p[0], p[1], p[2]};
		g.rot = new double[] {Math.toRadians(-r[0]), Math.toRadians(-r[1]), Math.toRadians(r[2])};
		double ox = -(o[0] + s[0]) / 16, oy = o[1] / 16, oz = o[2] / 16, vx = s[0] / 16, vy = s[1] / 16, vz = s[2] / 16;
		double x0 = ox - inf, x1 = ox + vx + inf, y0 = oy - inf, y1 = oy + vy + inf, z0 = oz - inf, z1 = oz + vz + inf;
		double[] blb = {x0, y0, z0}, brb = {x0, y0, z1}, tlb = {x0, y1, z0}, trb = {x0, y1, z1}, tlf = {x1, y1, z0}, trf = {x1, y1, z1}, blf = {x1, y0, z0}, brf = {x1, y0, z1};
		double[][][] verts = {
				{trb, tlb, blb, brb}, // west
				{tlf, trf, brf, blf}, // east
				{tlb, tlf, blf, blb}, // north
				{trf, trb, brb, brf}, // south
				{trb, trf, tlf, tlb}, // up
				{blb, blf, brf, brb}, // down
		};
		Object uvo = c.get("uv");
		for (int d = 0; d < 6; d++) {
			double[] uv, size;
			if (uvo instanceof Map) {
				Object face = obj(uvo).get(DIRS[d]);
				if (face == null) continue;
				uv = vec(obj(face).get("uv"), null);
				size = vec(obj(face).get("uv_size"), null);
			} else {
				double[] b = vec(uvo, new double[2]);
				double fx = Math.floor(s[0]), fy = Math.floor(s[1]), fz = Math.floor(s[2]);
				switch (d) {
					case 0 -> { uv = new double[] {b[0] + fz + fx, b[1] + fz}; size = new double[] {fz, fy}; }
					case 1 -> { uv = new double[] {b[0], b[1] + fz}; size = new double[] {fz, fy}; }
					case 2 -> { uv = new double[] {b[0] + fz, b[1] + fz}; size = new double[] {fx, fy}; }
					case 3 -> { uv = new double[] {b[0] + fz + fx + fz, b[1] + fz}; size = new double[] {fx, fy}; }
					case 4 -> { uv = new double[] {b[0] + fz, b[1]}; size = new double[] {fx, fz}; }
					default -> { uv = new double[] {b[0] + fz + fx, b[1] + fz}; size = new double[] {fx, -fz}; }
				}
			}
			double u = uv[0] / texW, v = uv[1] / texH, uw = (uv[0] + size[0]) / texW, vh = (uv[1] + size[1]) / texH;
			double t = uw; // not mirrored: GeoQuad swaps the u's
			uw = u;
			u = t;
			double[][] uvs = {{u, v}, {uw, v}, {uw, vh}, {u, vh}};
			Quad q = new Quad();
			for (int k = 0; k < 4; k++) {
				q.v[k] = verts[d][k];
				q.uv[k] = uvs[k];
			}
			q.n = NORMALS[d];
			g.quads.add(q);
		}
		return g;
	}

	// --- Animations, sampled as GeckoLib samples them (linear keys; before the first key its value, after the last key its value) ---

	static final class Channel {
		final TreeMap<Double, double[]> keys = new TreeMap<>();

		double[] at(double t) {
			Map.Entry<Double, double[]> lo = keys.floorEntry(t), hi = keys.ceilingEntry(t);
			if (lo == null) return hi.getValue();
			if (hi == null || hi.getKey().equals(lo.getKey())) return lo.getValue();
			double f = (t - lo.getKey()) / (hi.getKey() - lo.getKey());
			double[] a = lo.getValue(), b = hi.getValue();
			return new double[] {a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
		}
	}

	static final class Anim {
		double length;
		boolean loop;
		final Map<String, Map<String, Channel>> bones = new LinkedHashMap<>();
	}

	static Map<String, Anim> loadAnims(Path file) throws Exception {
		Map<String, Anim> out = new LinkedHashMap<>();
		if (!Files.exists(file)) return out;
		Map<String, Object> root = obj(new Json(Files.readString(file, StandardCharsets.UTF_8)).value());
		for (Map.Entry<String, Object> e : obj(root.get("animations")).entrySet()) {
			Map<String, Object> a = obj(e.getValue());
			Anim anim = new Anim();
			anim.length = a.get("animation_length") == null ? 0 : ((Number) a.get("animation_length")).doubleValue();
			anim.loop = Boolean.TRUE.equals(a.get("loop"));
			if (a.get("bones") != null) for (Map.Entry<String, Object> b : obj(a.get("bones")).entrySet()) {
				Map<String, Channel> chans = new LinkedHashMap<>();
				for (Map.Entry<String, Object> ch : obj(b.getValue()).entrySet()) {
					Channel c = new Channel();
					Object v = ch.getValue();
					if (v instanceof List) c.keys.put(0.0, vec(v, null));
					else for (Map.Entry<String, Object> k : obj(v).entrySet()) {
						Object kv = k.getValue();
						if (kv instanceof Map) {
							Map<String, Object> km = obj(kv);
							kv = km.containsKey("vector") ? km.get("vector") : km.containsKey("post") ? km.get("post") : km.get("pre");
						}
						c.keys.put(Double.parseDouble(k.getKey()), vec(kv, null));
					}
					chans.put(ch.getKey(), c);
				}
				anim.bones.put(b.getKey(), chans);
			}
			out.put(e.getKey(), anim);
		}
		return out;
	}

	// --- Matrices (row-major 4 x 4) ---

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

	static double[] tr(double x, double y, double z) { return new double[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1}; }

	static double[] rx(double a) { double c = Math.cos(a), s = Math.sin(a); return new double[] {1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1}; }
	static double[] ry(double a) { double c = Math.cos(a), s = Math.sin(a); return new double[] {c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1}; }
	static double[] rz(double a) { double c = Math.cos(a), s = Math.sin(a); return new double[] {c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }
	static double[] sc(double x, double y, double z) { return new double[] {x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1}; }

	static double[] ap(double[] m, double[] p) {
		return new double[] {m[0] * p[0] + m[1] * p[1] + m[2] * p[2] + m[3], m[4] * p[0] + m[5] * p[1] + m[6] * p[2] + m[7], m[8] * p[0] + m[9] * p[1] + m[10] * p[2] + m[11]};
	}

	static double[] dir(double[] m, double[] p) {
		double[] r = {m[0] * p[0] + m[1] * p[1] + m[2] * p[2], m[4] * p[0] + m[5] * p[1] + m[6] * p[2], m[8] * p[0] + m[9] * p[1] + m[10] * p[2]};
		double l = Math.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]);
		return l == 0 ? r : new double[] {r[0] / l, r[1] / l, r[2] / l};
	}

	/** A posed quad in the world (entity at yaw 0), in blocks. */
	static final class WQuad {
		final double[][] p = new double[4][];
		double[][] uv;
		double[] n;
		/** The bones it hangs from, its own first. */
		List<String> bones;
	}

	static void pose(GBone b, double[] parent, Anim anim, double t, List<WQuad> out) {
		double[] rot = b.rot.clone(), pos = {0, 0, 0}, scl = {1, 1, 1};
		Map<String, Channel> chans = anim == null ? null : anim.bones.get(b.name);
		if (chans != null) {
			if (chans.get("rotation") != null) {
				double[] r = chans.get("rotation").at(t);
				rot[0] += Math.toRadians(-r[0]);
				rot[1] += Math.toRadians(-r[1]);
				rot[2] += Math.toRadians(r[2]);
			}
			if (chans.get("position") != null) pos = chans.get("position").at(t);
			if (chans.get("scale") != null) scl = chans.get("scale").at(t);
		}
		double[] m = mul(parent, tr(-pos[0] / 16, pos[1] / 16, pos[2] / 16));
		m = mul(m, tr(b.pivot[0] / 16, b.pivot[1] / 16, b.pivot[2] / 16));
		m = mul(m, mul(rz(rot[2]), mul(ry(rot[1]), rx(rot[0]))));
		m = mul(m, sc(scl[0], scl[1], scl[2]));
		m = mul(m, tr(-b.pivot[0] / 16, -b.pivot[1] / 16, -b.pivot[2] / 16));
		for (GCube c : b.cubes) {
			double[] cm = mul(m, tr(c.pivot[0] / 16, c.pivot[1] / 16, c.pivot[2] / 16));
			cm = mul(cm, mul(rz(c.rot[2]), mul(ry(c.rot[1]), rx(c.rot[0]))));
			cm = mul(cm, tr(-c.pivot[0] / 16, -c.pivot[1] / 16, -c.pivot[2] / 16));
			List<String> chain = new ArrayList<>();
			for (GBone up = b; up != null; up = up.parent) chain.add(up.name);
			for (Quad q : c.quads) {
				WQuad w = new WQuad();
				for (int k = 0; k < 4; k++) w.p[k] = ap(cm, q.v[k]);
				w.uv = q.uv;
				w.n = dir(cm, q.n);
				w.bones = chain;
				out.add(w);
			}
		}
		for (GBone child : b.children) pose(child, m, anim, t, out);
	}

	// --- Drawing ---

	static final double[] L0 = norm(0.2, 1.0, -0.7), L1 = norm(-0.2, 1.0, 0.7);

	static double[] norm(double x, double y, double z) {
		double l = Math.sqrt(x * x + y * y + z * z);
		return new double[] {x / l, y / l, z / l};
	}

	static int[] base, glowTex;

	static final class View {
		final String name;
		final double yaw, pitch;

		View(String name, double yaw, double pitch) {
			this.name = name;
			this.yaw = Math.toRadians(yaw);
			this.pitch = Math.toRadians(pitch);
		}

		/** World to view: x right, y up, z toward the camera. */
		double[] see(double[] p) {
			double x1 = p[0] * Math.cos(yaw) - p[2] * Math.sin(yaw), z1 = p[0] * Math.sin(yaw) + p[2] * Math.cos(yaw);
			double y2 = p[1] * Math.cos(pitch) - z1 * Math.sin(pitch), z2 = p[1] * Math.sin(pitch) + z1 * Math.cos(pitch);
			return new double[] {x1, y2, z2};
		}
	}

	static View view(String name) {
		return switch (name) {
			case "front" -> new View("front 3/4", 35, 18);
			case "face" -> new View("front", 0, 5);
			case "side" -> new View("side", 90, 4);
			case "back" -> new View("back 3/4", 215, 22);
			case "top" -> new View("top", 15, 75);
			case "below" -> new View("below", 25, -60);
			case "low" -> new View("low 3/4", -35, -12);
			case "left" -> new View("left side", -90, 4);
			case "frontl" -> new View("front-left 3/4", -35, 18);
			case "high" -> new View("high 3/4", 30, 42);
			default -> throw new IllegalArgumentException(name);
		};
	}

	static final class Frame {
		final View view;
		final List<WQuad> quads;
		final String label;
		final boolean animated;

		Frame(View view, List<WQuad> quads, String label, boolean animated) {
			this.view = view;
			this.quads = quads;
			this.label = label;
			this.animated = animated;
		}
	}

	static BufferedImage draw(Frame f, double[] fit, int w, int h, double cell, double groundY) {
		int ss = 2, W = w * ss, H = h * ss;
		float[] z = new float[W * H];
		double[] r = new double[W * H], g = new double[W * H], b = new double[W * H];
		for (int y = 0; y < H; y++) {
			double t = y / (double) H;
			for (int x = 0; x < W; x++) {
				int i = y * W + x;
				z[i] = Float.NEGATIVE_INFINITY;
				r[i] = 0.10 - 0.06 * t;
				g[i] = 0.13 - 0.08 * t;
				b[i] = 0.24 - 0.13 * t;
			}
		}
		double scale = Math.min(W / (fit[2] - fit[0]), H / (fit[3] - fit[1])) * 0.9;
		double cx = (fit[0] + fit[2]) / 2, cy = (fit[1] + fit[3]) / 2;
		// Project each quad; base first, back to front, see-through; then the glow, back faces culled.
		List<double[][]> screen = new ArrayList<>();
		List<Double> depth = new ArrayList<>();
		for (WQuad q : f.quads) {
			double[][] s = new double[4][];
			double d = 0;
			for (int k = 0; k < 4; k++) {
				double[] v = f.view.see(q.p[k]);
				s[k] = new double[] {W / 2.0 + (v[0] - cx) * scale, H / 2.0 - (v[1] - cy) * scale, v[2]};
				d += v[2];
			}
			screen.add(s);
			depth.add(d);
		}
		Integer[] order = new Integer[f.quads.size()];
		for (int i = 0; i < order.length; i++) order[i] = i;
		java.util.Arrays.sort(order, (a, c) -> Double.compare(depth.get(a), depth.get(c)));
		for (int pass = 0; pass < 2; pass++) {
			int[] tex = pass == 0 ? base : glowTex;
			if (tex == null) continue;
			for (int qi : order) {
				WQuad q = f.quads.get(qi);
				double[] nv = viewDir(f.view, q.n);
				if (pass == 1 && nv[2] <= 0) continue; // the glow layer culls back faces
				double light = pass == 1 ? 1 : Math.min(1, 0.4 + 0.6 * (Math.max(0, dot(q.n, L0)) + Math.max(0, dot(q.n, L1))));
				double[][] s = screen.get(qi);
				tri(s[0], s[1], s[2], q.uv[0], q.uv[1], q.uv[2], q, tex, light, pass, z, r, g, b, W, H);
				tri(s[0], s[2], s[3], q.uv[0], q.uv[2], q.uv[3], q, tex, light, pass, z, r, g, b, W, H);
			}
		}
		// The ground's grid, where nothing nearer hides it.
		if (cell > 0) {
			for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
				double vx = (x - W / 2.0) / scale + cx, vy = -(y - H / 2.0) / scale + cy;
				// View point (vx, vy, vz) on the plane world y = groundY: solve for vz.
				double cp = Math.cos(f.view.pitch), sp = Math.sin(f.view.pitch);
				if (Math.abs(sp) < 1e-3) continue;
				// world y = vy * cos(p) + vz * sin(p)
				double vz = (groundY - vy * cp) / sp;
				double z1 = -vy * sp + vz * cp; // back to yawed space
				double cyw = Math.cos(f.view.yaw), syw = Math.sin(f.view.yaw);
				double wx = vx * cyw + z1 * syw, wz = -vx * syw + z1 * cyw;
				int i = y * W + x;
				if (vz < z[i]) continue;
				double gx = Math.abs(wx / cell - Math.rint(wx / cell)) * cell * scale, gz = Math.abs(wz / cell - Math.rint(wz / cell)) * cell * scale;
				double far = Math.sqrt(wx * wx + wz * wz) / cell;
				if (far > 7) continue;
				if (gx < 1.2 || gz < 1.2) {
					double a = 0.22 * (1 - far / 7);
					r[i] += (0.45 - r[i]) * a;
					g[i] += (0.55 - g[i]) * a;
					b[i] += (0.75 - b[i]) * a;
				}
			}
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			double rr = 0, gg = 0, bb = 0;
			for (int sy = 0; sy < ss; sy++) for (int sx = 0; sx < ss; sx++) {
				int i = (y * ss + sy) * W + x * ss + sx;
				rr += r[i];
				gg += g[i];
				bb += b[i];
			}
			int n = ss * ss;
			img.setRGB(x, y, c255(rr / n) << 16 | c255(gg / n) << 8 | c255(bb / n));
		}
		Graphics2D gr = img.createGraphics();
		gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		gr.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
		gr.setColor(new Color(200, 210, 235));
		gr.drawString(f.label, 6, 15);
		gr.dispose();
		return img;
	}

	static int c255(double v) { return (int) Math.max(0, Math.min(255, Math.round(v * 255))); }

	static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

	static double[] viewDir(View v, double[] n) {
		double[] o = v.see(new double[] {0, 0, 0}), p = v.see(n);
		return new double[] {p[0] - o[0], p[1] - o[1], p[2] - o[2]};
	}

	static void tri(double[] a, double[] b, double[] c, double[] ua, double[] ub, double[] uc, WQuad q, int[] tex, double light, int pass,
			float[] z, double[] r, double[] g, double[] bl, int W, int H) {
		double area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
		if (Math.abs(area) < 1e-9) return;
		int x0 = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0])))), x1 = (int) Math.min(W - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
		int y0 = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1])))), y1 = (int) Math.min(H - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
		// The quad's texels, to keep the sampling within them.
		double umin = 1e9, umax = -1e9, vmin = 1e9, vmax = -1e9;
		for (double[] uv : q.uv) {
			umin = Math.min(umin, uv[0]);
			umax = Math.max(umax, uv[0]);
			vmin = Math.min(vmin, uv[1]);
			vmax = Math.max(vmax, uv[1]);
		}
		int tu0 = (int) Math.floor(umin * texW + 1e-6), tu1 = (int) Math.ceil(umax * texW - 1e-6) - 1, tv0 = (int) Math.floor(vmin * texH + 1e-6), tv1 = (int) Math.ceil(vmax * texH - 1e-6) - 1;
		if (tu1 < tu0) tu1 = tu0;
		if (tv1 < tv0) tv1 = tv0;
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				double px = x + 0.5, py = y + 0.5;
				double w0 = ((b[0] - px) * (c[1] - py) - (b[1] - py) * (c[0] - px)) / area;
				double w1 = ((c[0] - px) * (a[1] - py) - (c[1] - py) * (a[0] - px)) / area;
				double w2 = 1 - w0 - w1;
				if (w0 < -1e-9 || w1 < -1e-9 || w2 < -1e-9) continue;
				double d = w0 * a[2] + w1 * b[2] + w2 * c[2];
				int i = y * W + x;
				if (pass == 0 ? d < z[i] : d < z[i] - 1e-4) continue;
				double u = w0 * ua[0] + w1 * ub[0] + w2 * uc[0], v = w0 * ua[1] + w1 * ub[1] + w2 * uc[1];
				int tx = Math.max(tu0, Math.min(tu1, (int) Math.floor(u * texW))), ty = Math.max(tv0, Math.min(tv1, (int) Math.floor(v * texH)));
				if (tx < 0 || ty < 0 || tx >= texW || ty >= texH) continue;
				int p = tex[ty * texW + tx];
				double al = (p >>> 24) / 255.0;
				if (al < 0.1) continue;
				double sr = (p >> 16 & 255) / 255.0 * light, sg = (p >> 8 & 255) / 255.0 * light, sb = (p & 255) / 255.0 * light;
				r[i] = sr * al + r[i] * (1 - al);
				g[i] = sg * al + g[i] * (1 - al);
				bl[i] = sb * al + bl[i] * (1 - al);
				z[i] = (float) Math.max(z[i], d);
			}
		}
	}

	static List<String> focus = new ArrayList<>();

	static double[] bounds(View v, List<WQuad> quads, double[] into) {
		for (WQuad q : quads) for (double[] p : q.p) {
			if (!focus.isEmpty() && focus.stream().noneMatch(f -> f.endsWith("*") ? q.bones.contains(f.substring(0, f.length() - 1)) : q.bones.get(0).equals(f))) continue;
			double[] s = v.see(p);
			into[0] = Math.min(into[0], s[0]);
			into[1] = Math.min(into[1], s[1]);
			into[2] = Math.max(into[2], s[0]);
			into[3] = Math.max(into[3], s[1]);
		}
		return into;
	}

	static List<WQuad> posed(List<GBone> tops, Anim anim, double t) {
		List<WQuad> out = new ArrayList<>();
		double[] entity = ry(Math.PI); // GeoEntityRenderer turns the model by 180 - yaw about y
		for (GBone b : tops) pose(b, entity, anim, t, out);
		return out;
	}

	/** {@code name} played as a looping GIF (see the class's note), its frames {@code 1/fps} apart. */
	static void gif(Path assets, Path out, String id, List<GBone> tops, Map<String, Anim> anims, String name, double seconds, double fps, String animView,
			double renderScale, int w, int h) throws Exception {
		Anim a = anims.get(name);
		if (a == null) throw new IllegalArgumentException(id + " has no animation " + name + " (has " + anims.keySet() + ")");
		List<Frame> frames = new ArrayList<>();
		int count = Math.max(1, (int) Math.round(seconds * fps));
		for (int k = 0; k < count; k++) {
			double t = k / fps;
			frames.add(new Frame(view(animView), posed(tops, a, t), id + "  " + name + " @ " + String.format(Locale.ROOT, "%.2f", t) + " s", true));
		}
		double groundY = 1e9;
		for (WQuad q : posed(tops, null, 0)) for (double[] p : q.p) groundY = Math.min(groundY, p[1]);
		double cell = 4 / renderScale;
		double[] fit = {1e9, 1e9, -1e9, -1e9};
		for (Frame f : frames) bounds(f.view, f.quads, fit);
		Files.createDirectories(out);
		Path file = out.resolve(id + "_" + name + ".gif");
		javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
		try (javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(file.toFile())) {
			writer.setOutput(stream);
			writer.prepareWriteSequence(null);
			int delay = (int) Math.max(2, Math.round(100 / fps));
			for (int k = 0; k < frames.size(); k++) {
				BufferedImage img = draw(frames.get(k), fit, w, h, cell, groundY);
				javax.imageio.ImageTypeSpecifier type = javax.imageio.ImageTypeSpecifier.createFromRenderedImage(img);
				javax.imageio.metadata.IIOMetadata meta = writer.getDefaultImageMetadata(type, null);
				String format = meta.getNativeMetadataFormatName();
				javax.imageio.metadata.IIOMetadataNode root = (javax.imageio.metadata.IIOMetadataNode) meta.getAsTree(format);
				javax.imageio.metadata.IIOMetadataNode control = node(root, "GraphicControlExtension");
				control.setAttribute("disposalMethod", "none");
				control.setAttribute("userInputFlag", "FALSE");
				control.setAttribute("transparentColorFlag", "FALSE");
				control.setAttribute("delayTime", Integer.toString(delay));
				control.setAttribute("transparentColorIndex", "0");
				if (k == 0) {
					javax.imageio.metadata.IIOMetadataNode apps = node(root, "ApplicationExtensions");
					javax.imageio.metadata.IIOMetadataNode app = new javax.imageio.metadata.IIOMetadataNode("ApplicationExtension");
					app.setAttribute("applicationID", "NETSCAPE");
					app.setAttribute("authenticationCode", "2.0");
					app.setUserObject(new byte[] {1, 0, 0}); // loop for ever
					apps.appendChild(app);
				}
				meta.setFromTree(format, root);
				writer.writeToSequence(new javax.imageio.IIOImage(img, null, meta), null);
			}
			writer.endWriteSequence();
		}
		writer.dispose();
		System.out.println(file);
	}

	static javax.imageio.metadata.IIOMetadataNode node(javax.imageio.metadata.IIOMetadataNode root, String name) {
		for (int i = 0; i < root.getLength(); i++) if (root.item(i).getNodeName().equalsIgnoreCase(name)) return (javax.imageio.metadata.IIOMetadataNode) root.item(i);
		javax.imageio.metadata.IIOMetadataNode node = new javax.imageio.metadata.IIOMetadataNode(name);
		root.appendChild(node);
		return node;
	}

	public static void main(String[] args) throws Exception {
		Path assets = Path.of(args[0]), out = Path.of(args[1]);
		String id = args[2];
		List<String> views = new ArrayList<>(List.of("front", "side", "top", "back"));
		String animName = null, atName = null, tag = null, animView = "front", gifName = null, folder = "spell";
		double gifSeconds = 0, gifFps = 20;
		List<Double> times = new ArrayList<>();
		double atTime = 0, renderScale = 4;
		int w = 480, h = 360, cols = 2;
		for (int i = 3; i < args.length; i++) {
			String a = args[i];
			if (a.startsWith("views=")) views = a.length() > 6 ? List.of(a.substring(6).split(",")) : List.of();
			else if (a.startsWith("anim=")) {
				String[] p = a.substring(5).split(":");
				animName = p[0];
				for (String s : p[1].split(",")) times.add(Double.parseDouble(s));
			} else if (a.startsWith("at=")) {
				String[] p = a.substring(3).split(":");
				atName = p[0];
				atTime = Double.parseDouble(p[1]);
			} else if (a.startsWith("scale=")) renderScale = Double.parseDouble(a.substring(6));
			else if (a.startsWith("w=")) w = Integer.parseInt(a.substring(2));
			else if (a.startsWith("h=")) h = Integer.parseInt(a.substring(2));
			else if (a.startsWith("cols=")) cols = Integer.parseInt(a.substring(5));
			else if (a.startsWith("tag=")) tag = a.substring(4);
			else if (a.startsWith("animview=")) animView = a.substring(9);
			else if (a.startsWith("focus=")) focus = List.of(a.substring(6).split(","));
			else if (a.startsWith("folder=")) folder = a.substring(7); // "realm" for the secret realms' guardians
			else if (a.startsWith("gif=")) {
				String[] p = a.substring(4).split(":");
				gifName = p[0];
				gifSeconds = Double.parseDouble(p[1]);
				if (p.length > 2) gifFps = Double.parseDouble(p[2]);
			}
		}
		List<GBone> tops = load(assets.resolve("geo/" + folder + "/" + id + ".geo.json"));
		Map<String, Anim> anims = loadAnims(assets.resolve("animations/" + folder + "/" + id + ".animation.json"));
		BufferedImage bt = ImageIO.read(assets.resolve("textures/entity/" + folder + "/" + id + ".png").toFile());
		base = bt.getRGB(0, 0, texW, texH, null, 0, texW);
		Path maskFile = assets.resolve("textures/entity/" + folder + "/" + id + "_glowmask.png");
		glowTex = null;
		if (Files.exists(maskFile)) {
			BufferedImage mt = ImageIO.read(maskFile.toFile());
			int[] mask = mt.getRGB(0, 0, texW, texH, null, 0, texW);
			glowTex = new int[mask.length];
			for (int i = 0; i < mask.length; i++) {
				if (mask[i] == 0) continue;
				int ma = mask[i] >>> 24, ba = base[i] >>> 24;
				glowTex[i] = (ma > 0 ? ma : ba) << 24 | base[i] & 0xFFFFFF;
				base[i] = 0;
			}
		}
		if (gifName != null) {
			gif(assets, out, id, tops, anims, gifName, gifSeconds, gifFps, animView, renderScale, w, h);
			return;
		}
		Anim at = atName == null ? null : anims.get(atName);
		if (atName != null && at == null) throw new IllegalArgumentException(id + " has no animation " + atName + " (has " + anims.keySet() + ")");
		List<Frame> frames = new ArrayList<>();
		for (String v : views) frames.add(new Frame(view(v), posed(tops, at, atTime), view(v).name + (at == null ? "" : "  (" + atName + " @ " + atTime + " s)"), false));
		if (animName != null) {
			Anim a = anims.get(animName);
			if (a == null) throw new IllegalArgumentException(id + " has no animation " + animName + " (has " + anims.keySet() + ")");
			String av = animView;
			for (double t : times) frames.add(new Frame(view(av), posed(tops, a, t), animName + " @ " + String.format(Locale.ROOT, "%.2f", t) + " s", true));
		}
		// The ground: the model's foot at rest.
		double groundY = 1e9;
		for (WQuad q : posed(tops, null, 0)) for (double[] p : q.p) groundY = Math.min(groundY, p[1]);
		double cell = 4 / renderScale; // 4 blocks in game, in model blocks
		int rows = (frames.size() + cols - 1) / cols;
		BufferedImage sheet = new BufferedImage(w * Math.min(cols, frames.size()), h * rows, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = sheet.createGraphics();
		// Frames of one animation share one fit, so its motion shows; the views fit each alone.
		double[] animFit = {1e9, 1e9, -1e9, -1e9};
		for (Frame f : frames) if (f.animated) bounds(f.view, f.quads, animFit);
		for (int i = 0; i < frames.size(); i++) {
			Frame f = frames.get(i);
			double[] fit = f.animated ? animFit : bounds(f.view, f.quads, new double[] {1e9, 1e9, -1e9, -1e9});
			g.drawImage(draw(f, fit, w, h, cell, groundY), (i % cols) * w, (i / cols) * h, null);
		}
		g.dispose();
		Files.createDirectories(out);
		Path file = out.resolve(id + (tag == null ? "" : "_" + tag) + ".png");
		ImageIO.write(sheet, "png", file.toFile());
		System.out.println(file);
	}
}
