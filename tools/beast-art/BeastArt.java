import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes the beasts' looks for Defying the Heavens (the demon races of 2026-10-23, and the Ghost Gate's Ox-Head and Horse-Face):
 * GeckoLib models (geo/beast/*.geo.json, Bedrock geometry: pixels, y up, the face toward -z, about the neck or, for a tail, the
 * top of the body), their animations (animations/beast/*.animation.json), the textures they're painted in (textures/entity/
 * bloodline/<race>_<colouring>_beast.png, textures/entity/beast_head/ox.png and horse.png), and each race's whole player skin in
 * each colouring (textures/entity/bloodline/<race>_<colouring>.png, a broad-armed 64 x 64 skin).
 * <p>
 * Every cube is painted by a painter of its own over the box its UVs take (the same layout as vanilla's and Blockbench's box UV),
 * boxes packed onto the texture as they come; a cube may share another's box (a fox's nine tails, alike). The models open in
 * Blockbench (File, Open, a .geo.json as a GeckoLib model) to be tuned there.
 * <p>
 * Run: {@code java BeastArt.java <the mod's src/main/resources/assets/defying-the-heavens>}; with {@code bodies} after it, the wild demon
 * beasts' whole bodies and the demon cores instead (Bodies, Cores); with {@code primordials}, the Immortal Realm's primordial beasts alone
 * (Primordials).
 */
public class BeastArt {
	// --- Colours ---

	static int rgb(int hex) { return 0xFF000000 | hex; }

	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
		int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
		return (int) Math.round(aa + (ba - aa) * t) << 24 | (int) Math.round(ar + (br - ar) * t) << 16
				| (int) Math.round(ag + (bg - ag) * t) << 8 | (int) Math.round(ab + (bb - ab) * t);
	}

	static int shade(int c, double by) {
		return by >= 0 ? mix(c, 0xFFFFFFFF, by) : mix(c, 0xFF000000, -by);
	}

	/** A steady pseudo-random number in [0, 1) for a pixel. */
	static double hash(int x, int y, int seed) {
		long h = x * 374761393L + y * 668265263L + seed * 2246822519L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (double) 0x1000000;
	}

	/** Fur: the colour a little lighter or darker pixel by pixel. */
	static int fur(int c, int x, int y, int seed) {
		return shade(c, (hash(x, y, seed) - 0.5) * 0.16);
	}

	static final int CLEAR = 0x00000000;

	// --- Painting a box ---

	enum Face { UP, DOWN, SIDE_A, FRONT, SIDE_B, BACK }

	/**
	 * One pixel of a cube's face: {@code x} across it (0 at its left as seen from outside, so on a side face 0 or {@code w - 1} is its
	 * front edge: {@link #frontness}), {@code y} down it (0 at its top; on the top face 0 its back edge, on the bottom its front).
	 */
	interface Painter {
		int paint(Face face, int x, int y, int w, int h, int d);
	}

	/** How near the front of the cube a pixel on a side or top face is, 0 (back) to 1 (front); 1 on the front, 0 on the back. */
	static double frontness(Face face, int x, int y, int w, int h) {
		return switch (face) {
			case FRONT -> 1;
			case BACK -> 0;
			case SIDE_A -> w <= 1 ? 1 : x / (double) (w - 1);
			case SIDE_B -> w <= 1 ? 1 : 1 - x / (double) (w - 1);
			case UP -> h <= 1 ? 1 : y / (double) (h - 1);
			case DOWN -> h <= 1 ? 1 : 1 - y / (double) (h - 1);
		};
	}

	static Painter solid(int c) { return (f, x, y, w, h, d) -> c; }

	static Painter furred(int c, int seed) { return (f, x, y, w, h, d) -> fur(c, x + f.ordinal() * 17, y, seed); }

	// --- Models ---

	/** A cube: where (Bedrock: its least corner), how big, its box on the texture (or another's, {@code shares}), how painted. */
	static final class Cube {
		final double[] origin, size;
		final Painter painter;
		double inflate;
		Cube shares;
		int u = -1, v = -1;

		Cube(double[] origin, double[] size, Painter painter) {
			this.origin = origin;
			this.size = size;
			this.painter = painter;
		}

		int bw() { return box(size[0]); }
		int bh() { return box(size[1]); }
		int bd() { return box(size[2]); }

		/**
		 * A box's side on the texture. GeckoLib maps a box UV face over the floor of the cube's size (BakedModelFactory), so the demon
		 * beasts' bodies are packed that way ({@link #floorBoxes}); the races' heads and tails were packed rounding up, and are left so.
		 */
		static int box(double s) { return Math.max(1, floorBoxes ? (int) Math.floor(s + 1e-6) : (int) Math.ceil(s - 1e-6)); }
	}

	/** Pack and paint boxes by the floor of their cubes' sizes, as GeckoLib maps them (Bodies). */
	static boolean floorBoxes;

	static final class Bone {
		final String name, parent;
		final double[] pivot, rotation;
		final List<Cube> cubes = new ArrayList<>();

		Bone(String name, String parent, double[] pivot, double[] rotation) {
			this.name = name;
			this.parent = parent;
			this.pivot = pivot;
			this.rotation = rotation;
		}

		Cube cube(double x, double y, double z, double w, double h, double d, Painter painter) {
			Cube c = new Cube(new double[] {x, y, z}, new double[] {w, h, d}, painter);
			cubes.add(c);
			return c;
		}

		/** A cube painted as {@code like} and sharing its box. */
		Cube cubeLike(Cube like, double x, double y, double z) {
			Cube c = new Cube(new double[] {x, y, z}, like.size, like.painter);
			c.shares = like;
			cubes.add(c);
			return c;
		}
	}

	static final class Model {
		final String id;
		final List<Bone> bones = new ArrayList<>();

		Model(String id) { this.id = id; }

		Bone bone(String name, String parent, double px, double py, double pz, double rx, double ry, double rz) {
			Bone b = new Bone(name, parent, new double[] {px, py, pz}, new double[] {rx, ry, rz});
			bones.add(b);
			return b;
		}

		Bone bone(String name, String parent, double px, double py, double pz) {
			return bone(name, parent, px, py, pz, 0, 0, 0);
		}
	}

	/** Several models painted onto one texture: their cubes' boxes packed in rows. */
	static final class Atlas {
		final int width, height;
		final List<Model> models = new ArrayList<>();

		Atlas(int width, int height) {
			this.width = width;
			this.height = height;
		}

		void pack() {
			int x = 0, y = 0, row = 0;
			for (Model m : models) {
				for (Bone b : m.bones) {
					for (Cube c : b.cubes) {
						if (c.shares != null) continue;
						int bw = 2 * (c.bd() + c.bw()), bh = c.bd() + c.bh();
						if (x + bw > width) {
							x = 0;
							y += row;
							row = 0;
						}
						if (y + bh > height) throw new IllegalStateException(m.id + ": the texture is full (" + width + " x " + height + ")");
						c.u = x;
						c.v = y;
						x += bw;
						row = Math.max(row, bh);
					}
				}
			}
			for (Model m : models) for (Bone b : m.bones) for (Cube c : b.cubes) if (c.shares != null) {
				c.u = c.shares.u;
				c.v = c.shares.v;
			}
		}

		BufferedImage paint() {
			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (Model m : models) for (Bone b : m.bones) for (Cube c : b.cubes) if (c.shares == null) paintBox(img, c.u, c.v, c.bw(), c.bh(), c.bd(), c.painter);
			return img;
		}
	}

	/** Paints a box's six faces where box UV puts them, at (u, v). */
	static void paintBox(BufferedImage img, int u, int v, int w, int h, int d, Painter p) {
		face(img, u + d, v, w, d, Face.UP, w, h, d, p);
		face(img, u + d + w, v, w, d, Face.DOWN, w, h, d, p);
		face(img, u, v + d, d, h, Face.SIDE_A, w, h, d, p);
		face(img, u + d, v + d, w, h, Face.FRONT, w, h, d, p);
		face(img, u + d + w, v + d, d, h, Face.SIDE_B, w, h, d, p);
		face(img, u + 2 * d + w, v + d, w, h, Face.BACK, w, h, d, p);
	}

	static void face(BufferedImage img, int x0, int y0, int fw, int fh, Face face, int w, int h, int d, Painter p) {
		for (int y = 0; y < fh; y++) {
			for (int x = 0; x < fw; x++) {
				int px = x0 + x, py = y0 + y;
				if (px < 0 || py < 0 || px >= img.getWidth() || py >= img.getHeight()) continue;
				img.setRGB(px, py, p.paint(face, x, y, fw, fh, d));
			}
		}
	}

	// --- Writing ---

	static String num(double v) {
		if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
		return String.format(Locale.ROOT, "%.3f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	static String vec(double[] v) { return "[" + num(v[0]) + ", " + num(v[1]) + ", " + num(v[2]) + "]"; }

	static String geo(Model m, Atlas atlas) {
		StringBuilder s = new StringBuilder();
		s.append("{\n\t\"format_version\": \"1.12.0\",\n\t\"minecraft:geometry\": [\n\t\t{\n");
		s.append("\t\t\t\"description\": {\n\t\t\t\t\"identifier\": \"geometry.").append(m.id).append("\",\n");
		s.append("\t\t\t\t\"texture_width\": ").append(atlas.width).append(",\n\t\t\t\t\"texture_height\": ").append(atlas.height).append(",\n");
		s.append("\t\t\t\t\"visible_bounds_width\": 4,\n\t\t\t\t\"visible_bounds_height\": 4,\n\t\t\t\t\"visible_bounds_offset\": [0, 0, 0]\n\t\t\t},\n");
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
					s.append("\t\t\t\t\t\t{\"origin\": ").append(vec(c.origin)).append(", \"size\": ").append(vec(c.size))
							.append(", \"uv\": ").append(faceUv(c));
					if (c.inflate != 0) s.append(", \"inflate\": ").append(num(c.inflate));
					s.append("}").append(j + 1 < b.cubes.size() ? ",\n" : "\n");
				}
				s.append("\t\t\t\t\t]");
			}
			s.append("\n\t\t\t\t}").append(i + 1 < m.bones.size() ? ",\n" : "\n");
		}
		s.append("\t\t\t]\n\t\t}\n\t]\n}\n");
		return s.toString();
	}

	/**
	 * A cube's UVs, face by face: each face where {@link #paintBox} painted it, its box the cube's size rounded up ({@link Cube#bw}).
	 * Not box UV ({@code "uv": [u, v]}): GeckoLib lays a box out by the size rounded <i>down</i>, so on a cube a fraction of a pixel
	 * in any way (a fox's snout 2.5 high, its lids a fifth deep) every face read the wrong pixels: the snout's face its top's, with a
	 * hole through it, the lids half see-through. Face by face, each face reads just what was painted for it (stretched a little over
	 * a fraction of a pixel), and the cube opens the same in Blockbench (per-face UV).
	 */
	static String faceUv(Cube c) {
		int u = c.u, v = c.v, w = c.bw(), h = c.bh(), d = c.bd();
		return "{" + face("north", u + d, v + d, w, h) + ", " + face("east", u, v + d, d, h) + ", " + face("south", u + 2 * d + w, v + d, w, h)
				+ ", " + face("west", u + d + w, v + d, d, h) + ", " + face("up", u + d, v, w, d) + ", " + face("down", u + d + w, v + d, w, -d) + "}";
	}

	static String face(String name, int u, int v, int w, int h) {
		return "\"" + name + "\": {\"uv\": [" + u + ", " + v + "], \"uv_size\": [" + w + ", " + h + "]}";
	}

	/** An animation's keyframes for a bone: channel ("rotation", "position", "scale") to time to value. */
	static final class Anim {
		final String name;
		final double length;
		final boolean loop;
		final Map<String, Map<String, Map<Double, double[]>>> bones = new LinkedHashMap<>();

		Anim(String name, double length, boolean loop) {
			this.name = name;
			this.length = length;
			this.loop = loop;
		}

		Anim key(String bone, String channel, double time, double x, double y, double z) {
			bones.computeIfAbsent(bone, k -> new LinkedHashMap<>()).computeIfAbsent(channel, k -> new java.util.TreeMap<>()).put(time, new double[] {x, y, z});
			return this;
		}
	}

	static String animations(List<Anim> anims) {
		StringBuilder s = new StringBuilder("{\n\t\"format_version\": \"1.8.0\",\n\t\"animations\": {\n");
		for (int i = 0; i < anims.size(); i++) {
			Anim a = anims.get(i);
			s.append("\t\t\"").append(a.name).append("\": {\n");
			if (a.loop) s.append("\t\t\t\"loop\": true,\n");
			s.append("\t\t\t\"animation_length\": ").append(num(a.length)).append(",\n\t\t\t\"bones\": {\n");
			int bi = 0;
			for (Map.Entry<String, Map<String, Map<Double, double[]>>> bone : a.bones.entrySet()) {
				s.append("\t\t\t\t\"").append(bone.getKey()).append("\": {\n");
				int ci = 0;
				for (Map.Entry<String, Map<Double, double[]>> channel : bone.getValue().entrySet()) {
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

	/**
	 * {@code java BeastArt.java <assets>} writes the races' and wardens' heads; {@code ... <assets> bodies}, the wild demon beasts and their
	 * cores; {@code ... <assets> icons}, the ability icons alone (Icons); {@code ... <assets> domains}, the Law domains' icons and the greater
	 * Laws' seals (Icons#writeDomains); {@code ... <assets> chaos}, the Chaos Frontier's fiend, its Primordial Chaos, its items and blocks
	 * (Chaos); {@code ... <assets> worlds}, the small worlds' World Seed and Small World icon (Worlds); {@code ... <assets> techniques}, the
	 * techniques' icons (Techniques: Universe in a Sleeve); {@code ... <assets> artifacts [sheet.png]}, the Artifact Forge's artifacts and
	 * faces (ArtifactArt), and a sheet of them to look over; {@code ... <assets> legends [sheet.png]}, Ganjiang's and Moye's models and their
	 * living textures (LegendArt); {@code ... <assets> missions}, the sect mission board's faces, its slips and letters (MissionArt).
	 */
	public static void main(String[] args) throws Exception {
		if (args.length >= 2 && args[1].equals("legends") && args.length <= 3) {
			LegendArt.writeAll(Path.of(args[0]));
			if (args.length == 3) LegendArt.writeSheet(Path.of(args[2]));
			System.out.println("written");
			return;
		}
		if (args.length == 2 && args[1].equals("missions")) {
			MissionArt.writeAll(Path.of(args[0]));
			System.out.println("written");
			return;
		}
		if (args.length == 2 && args[1].equals("realms")) {
			RealmArt.writeAll(Path.of(args[0]));
			System.out.println("written");
			return;
		}
		if (args.length >= 2 && args[1].equals("artifacts") && args.length <= 3) {
			ArtifactArt.writeAll(Path.of(args[0]));
			if (args.length == 3) {
				ArtifactArt.writeSheet(Path.of(args[0]), Path.of(args[2]));
				String sheet = args[2];
				ElementalArt.writeSheet(Path.of(sheet.substring(0, sheet.length() - 4) + "-elemental.png"));
			}
			System.out.println("written");
			return;
		}
		if (args.length < 1 || args.length > 2 || args.length == 2 && !args[1].equals("bodies") && !args[1].equals("icons") && !args[1].equals("primordials")
				&& !args[1].equals("domains") && !args[1].equals("chaos") && !args[1].equals("worlds") && !args[1].equals("techniques")) {
			System.err.println("java BeastArt.java <the mod's src/main/resources/assets/defying-the-heavens> [bodies|icons|primordials|domains|chaos|worlds|techniques|artifacts [sheet.png]]");
			System.exit(1);
		}
		if (args.length == 2 && args[1].equals("techniques")) {
			Techniques.writeAll(Path.of(args[0]));
		} else if (args.length == 2 && args[1].equals("worlds")) {
			Worlds.writeAll(Path.of(args[0]));
		} else if (args.length == 2 && args[1].equals("chaos")) {
			floorBoxes = true; // packed as the wild beasts' bodies are (Bodies)
			Chaos.writeAll(Path.of(args[0]));
		} else if (args.length == 2 && args[1].equals("icons")) {
			Icons.writeAll(Path.of(args[0]));
		} else if (args.length == 2 && args[1].equals("domains")) {
			Icons.writeDomains(Path.of(args[0]));
		} else if (args.length == 2 && args[1].equals("primordials")) {
			floorBoxes = true; // packed as the wild beasts' bodies are (Bodies)
			Primordials.writeAll(Path.of(args[0]));
		} else if (args.length == 2) {
			floorBoxes = true;
			Bodies.writeAll(Path.of(args[0]));
		} else {
			Beasts.writeAll(Path.of(args[0]));
		}
		System.out.println("written");
	}
}
