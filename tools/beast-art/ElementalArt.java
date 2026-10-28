import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The weapons made of their element (phase 2 of the Artifact Forge, 2026-10-28; ElementalWeaponItem), each the element itself and each
 * alive: an animated strip of {@link #FRAMES} frames down (textures/item/<grade>_<kind>.png, with its .png.mcmeta), Spirit to Immortal
 * grade, the grade in its fittings and in how fiercely its element burns, flows or shines:
 * <ul>
 *   <li>the Flame Blade, a sword of living flame, white-hot at its heart, its tongues licking up off it and embers rising;</li>
 *   <li>the Tide Spear, a spear of water, see-through, light running up its shaft to a head of foaming water, a drop falling;</li>
 *   <li>the Vine Whip, a living vine swaying from a wooden grip, leaves on it, a mote of spirit qi running along it;</li>
 *   <li>the Mountain Hammer, a head of mountain stone on a long haft, the cracks in it glowing and dimming like breath;</li>
 *   <li>the Golden Light Sword, a blade of golden light, a gleam running up it to the point, sparks of light about it.</li>
 * </ul>
 * Written by ArtifactArt#writeAll; laid out by the same diagonal measures (ArtifactArt: {@code a} along, {@code b} across).
 */
public class ElementalArt {
	static final int FRAMES = 8;
	static final String[] KINDS = {"flame_blade", "tide_spear", "vine_whip", "mountain_hammer", "golden_light_sword"};
	/** Ticks a frame shows, by kind; and whether the frames melt into each other (water's). */
	static final int[] FRAME_TIME = {2, 3, 3, 3, 2};
	static final boolean[] BLEND = {false, true, false, false, false};

	static void writeAll(Path assets) throws IOException {
		for (int g = 1; g < ArtifactArt.GRADES.length; g++) {
			for (int k = 0; k < KINDS.length; k++) {
				Path file = assets.resolve("textures/item/" + ArtifactArt.GRADES[g] + "_" + KINDS[k] + ".png");
				BeastArt.write(file, strip(k, g));
				String meta = "{\n  \"animation\": {\n    \"frametime\": " + FRAME_TIME[k] + (BLEND[k] ? ",\n    \"interpolate\": true" : "") + "\n  }\n}\n";
				Files.writeString(file.resolveSibling(file.getFileName() + ".mcmeta"), meta, StandardCharsets.UTF_8);
			}
		}
	}

	/** Kind {@code k}'s whole strip at grade {@code g} (1 Spirit .. 4 Immortal). */
	static BufferedImage strip(int k, int g) {
		BufferedImage out = new BufferedImage(16, 16 * FRAMES, BufferedImage.TYPE_INT_ARGB);
		for (int f = 0; f < FRAMES; f++) {
			BufferedImage frame = frame(k, g, f);
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out.setRGB(x, f * 16 + y, frame.getRGB(x, y));
		}
		return out;
	}

	static BufferedImage frame(int k, int g, int f) {
		ArtifactArt.Look look = ArtifactArt.LOOKS[g];
		return switch (k) {
			case 0 -> flameBlade(look, g, f);
			case 1 -> tideSpear(look, g, f);
			case 2 -> vineWhip(look, g, f);
			case 3 -> mountainHammer(look, g, f);
			default -> goldenSword(look, g, f);
		};
	}

	// --- Helpers ---

	static int argb(int rgb, int alpha) {
		return (alpha & 255) << 24 | rgb & 0xFFFFFF;
	}

	static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** A steady random in [0, 1) at a lattice point, repeating every 8 across and 4 down (16 pixels at 4 a cell). */
	static double lattice(int x, int y, int seed) {
		return BeastArt.hash(Math.floorMod(x, 8), Math.floorMod(y, 4), seed);
	}

	/** Smooth noise in [0, 1), 4 pixels a cell, repeating every 16 pixels down: a strip scrolled 16 pixels over its frames loops. */
	static double noise(double x, double y, int seed) {
		double gx = x / 4, gy = y / 4;
		int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
		double fx = gx - x0, fy = gy - y0;
		double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
		return lerp(lerp(lattice(x0, y0, seed), lattice(x0 + 1, y0, seed), sx), lerp(lattice(x0, y0 + 1, seed), lattice(x0 + 1, y0 + 1, seed), sx), sy);
	}

	static int xOf(int a, int b) { return (a + b + 15) / 2; }

	static int yOf(int a, int b) { return (b - a + 15) / 2; }

	/** A sword's fittings, as the flying sword's: a guard across, a wrapped grip, a pommel, a tassel in the grade's colour. */
	static void hilt(ArtifactArt.Canvas c, ArtifactArt.Look look) {
		int[] fit = look.fitting();
		for (int a = -4; a <= -3; a++) {
			for (int b = -3; b <= 2; b++) c.at(a, b, b == -3 || b == 2 ? fit[1] : fit[0], fit[2]);
		}
		ArtifactArt.grip(c, look, -9, -5);
		for (int a = -11; a <= -10; a++) for (int b = -1; b <= 0; b++) c.at(a, b, b == -1 ? fit[0] : fit[1], fit[2]);
		ArtifactArt.tassel(c, look, 2, 13);
	}

	/** Paints {@code rgba} at (x, y) over whatever is there unless it would cover the fittings ({@code keep}). */
	static void over(ArtifactArt.Canvas c, int x, int y, int rgba, boolean[][] keep) {
		if (x < 0 || y < 0 || x > 15 || y > 15 || keep != null && keep[x][y]) return;
		c.set(x, y, rgba, 0);
	}

	/** What the fittings cover, so the element never paints over them. */
	static boolean[][] painted(ArtifactArt.Canvas c) {
		boolean[][] keep = new boolean[16][16];
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) keep[x][y] = c.colour[x][y] != 0;
		return keep;
	}

	// --- The Flame Blade ---

	/**
	 * A sword of living flame: its core white-hot (an Immortal one's heart blue-white, the hottest), yellow and orange out to its red
	 * edges, tongues of flame licking up off its upper edge (more of them the higher its grade), embers rising; its fittings below.
	 */
	static BufferedImage flameBlade(ArtifactArt.Look look, int g, int f) {
		ArtifactArt.Canvas c = new ArtifactArt.Canvas();
		hilt(c, look);
		boolean[][] keep = painted(c);
		int hot = g == 4 ? 0xE8F4FF : g == 3 ? 0xFFFBE8 : 0xFFF6C0, warm = 0xFFD040, orange = 0xFF8C24, red = 0xD83A12, deep = 0x9A1C08;
		double fierce = 0.08 * (g - 1);
		// The blade: a widening flame from the guard to a flickering point.
		for (int a = -2; a <= 13; a++) {
			int lo = a <= 0 ? -1 : a >= 11 ? -1 : -2, hi = a <= 0 ? 0 : a >= 11 ? 0 : 1;
			if (a >= 13) lo = hi = 0;
			for (int b = lo; b <= hi; b++) {
				if (((a + b) & 1) == 0) continue;
				int x = xOf(a, b), y = yOf(a, b);
				double h = noise(x, y + 2 * f, 11);
				int col = b == -1 || b == 0 ? BeastArt.mix(hot, warm, Math.max(0, h - 0.25) * 1.2) : BeastArt.mix(orange, red, h);
				if ((b == -1 || b == 0) && a <= 8 && h < 0.35) col = hot;
				over(c, x, y, BeastArt.rgb(col), keep);
			}
		}
		// Tongues licking up off its upper edge (and a few off its lower), drawn as the heat rises.
		for (int a = -1; a <= 13; a++) {
			for (int step = 1; step <= 3; step++) {
				int b = (a <= 0 || a >= 11 ? -1 : -2) - step;
				if (((a + b) & 1) == 0) b--;
				int x = xOf(a, b), y = yOf(a, b);
				if (x < 0 || y < 0 || x > 15 || y > 15) continue;
				double h = noise(x * 1.3, y + 2 * f, 23);
				double need = 0.36 + 0.13 * step - fierce;
				if (h < need) continue;
				int col = step == 1 ? orange : step == 2 ? red : deep;
				over(c, x, y, argb(col, step == 3 ? 170 : 230), keep);
			}
			int b = (a <= 0 || a >= 11 ? 0 : 1) + 1;
			if (((a + b) & 1) == 0) b++;
			int x = xOf(a, b), y = yOf(a, b);
			if (x >= 0 && y >= 0 && x <= 15 && y <= 15 && noise(x * 1.3, y + 2 * f, 31) > 0.62 - fierce) over(c, x, y, argb(red, 210), keep);
		}
		// Embers rising off it, round again every loop.
		int[][] embers = {{9, 6}, {12, 3}, {6, 9}, {14, 6}};
		for (int i = 0; i < embers.length && i < 2 + g / 2; i++) {
			int x = embers[i][0] - (i % 2), y = Math.floorMod(embers[i][1] - 2 * f, 16);
			if (y > 12) continue;
			over(c, x, y, argb(i % 2 == 0 ? 0xFFD060 : 0xFF9A30, 220), keep);
		}
		return c.done();
	}

	// --- The Golden Light Sword ---

	/**
	 * A blade of golden light, pale gold at its heart, its halo soft about it (an Immortal one's wider), a gleam running up it to the point
	 * once a loop and sparks of light winking about it; its fittings below.
	 */
	static BufferedImage goldenSword(ArtifactArt.Look look, int g, int f) {
		ArtifactArt.Canvas c = new ArtifactArt.Canvas();
		hilt(c, look);
		boolean[][] keep = painted(c);
		int pale = g == 4 ? 0xFFFFF4 : 0xFFF2B8, gold = g == 4 ? 0xFFE27A : 0xFFD04A, halo = 0xFFD866;
		int gleam = -4 + 2 * f;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int a = x - y, b = x + y - 15;
				if (a < -2 || !(b == -1 && a <= 10 || b == 0 && a <= 13)) continue;
				boolean lit = a >= gleam - 1 && a <= gleam + 1;
				over(c, x, y, BeastArt.rgb(b == -1 ? (lit ? 0xFFFFFF : pale) : lit ? 0xFFF8D8 : a >= 11 ? pale : gold), keep);
			}
		}
		// Its halo: soft light either side (where nothing else is), brighter where the gleam runs.
		boolean[][] blade = painted(c);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				if (blade[x][y]) continue;
				int a = x - y, b = x + y - 15;
				if (a < -2 || a > 14) continue;
				int edge = b < -1 ? -1 - b : b > 0 ? b : 0;
				int reach = g == 4 ? 2 : 1;
				if (edge < 1 || edge > reach + (a >= gleam - 1 && a <= gleam + 1 ? 1 : 0)) continue;
				boolean lit = a >= gleam - 2 && a <= gleam + 2;
				int alpha = (lit ? 200 : 110) / edge;
				over(c, x, y, argb(lit ? 0xFFF4C8 : halo, alpha), null);
			}
		}
		// Sparks of light winking about it: a cross of light at a few spots, each on its own frames.
		int[][] sparks = {{11, 1}, {6, 5}, {13, 6}, {4, 3}, {9, 9}};
		for (int i = 0; i < sparks.length; i++) {
			if (Math.floorMod(f - i * 3, FRAMES) > 1 + (g >= 3 ? 1 : 0)) continue;
			int x = sparks[i][0], y = sparks[i][1];
			if (blade[x][y]) continue;
			over(c, x, y, argb(0xFFFFFF, 230), keep);
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = x + d[0], ny = y + d[1];
				if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && !blade[nx][ny]) over(c, nx, ny, argb(0xFFE89A, 120), keep);
			}
		}
		return c.done();
	}

	// --- The Tide Spear ---

	/**
	 * A spear of water, see-through: its shaft deep blue under, light above, light running up it to its head of brighter water with
	 * foam winking on its edges, a drop falling from it; a collar and a butt cap of the grade's fittings.
	 */
	static BufferedImage tideSpear(ArtifactArt.Look look, int g, int f) {
		ArtifactArt.Canvas c = new ArtifactArt.Canvas();
		int[] fit = look.fitting();
		int light = 0x5CB4F4, deep = 0x1A5CC4, foam = 0xE8F8FF, head = 0x8AD4FF, headDeep = 0x2E88DA, rim = 0x0E3C8C;
		for (int a = -15; a <= 3; a++) {
			for (int b = -1; b <= 0; b++) {
				if (((a + b) & 1) == 0) continue;
				boolean flow = Math.floorMod(a - 2 * f, 16) <= 1 || Math.floorMod(a - 2 * f - 8, 16) <= 0 && g >= 3;
				int col = flow ? foam : b == -1 ? light : deep;
				c.at(a, b, argb(col, flow ? 235 : 205), 0);
			}
		}
		for (int a = -15; a <= -14; a++) for (int b = -1; b <= 0; b++) c.at(a, b, b == -1 ? fit[0] : fit[1], fit[2]);
		for (int a = 3; a <= 5; a++) for (int b = -2; b <= 1; b++) c.at(a, b, b <= -1 ? fit[0] : fit[1], fit[2]);
		// The head: a leaf of brighter water, rimmed in deep blue, its edges foaming.
		for (int a = 6; a <= 14; a++) {
			int half = a <= 7 ? 1 : a <= 11 ? 2 : 1;
			int lo = -half, hi = half - 1;
			if (a == 14) lo = hi = -1;
			for (int b = lo; b <= hi; b++) {
				if (((a + b) & 1) == 0) continue;
				int x = xOf(a, b), y = yOf(a, b);
				boolean edge = b == lo || b == hi;
				int col = b == -1 ? head : b < -1 ? BeastArt.mix(head, foam, 0.4) : headDeep;
				if (edge && noise(x * 2, y + 2 * f, 41) > 0.6) col = foam;
				c.at(a, b, argb(col, 240), argb(rim, 230));
			}
		}
		// A drop falling from beneath the head, once a loop.
		int dropY = 7 + f;
		if (dropY <= 14 && f < 6) c.set(12, dropY, argb(light, 220), 0);
		return c.done();
	}

	// --- The Vine Whip ---

	/**
	 * A living vine from a wooden grip, swaying as a wave runs along it (its sway the greater toward its tip), its leaves on alternate
	 * sides, a bud at its tip, a mote of spirit qi running along it; the grip banded in the grade's fittings.
	 */
	static BufferedImage vineWhip(ArtifactArt.Look look, int g, int f) {
		ArtifactArt.Canvas c = new ArtifactArt.Canvas();
		int[] fit = look.fitting(), wood = look.wood();
		int green = 0x4E9E34, light = 0x86D45A, dark = 0x2A5A1C, leaf = 0x6CCB3E, leafDark = 0x3E8A26, bud = 0xD8F08A, mote = 0xE8FFB0;
		for (int a = -15; a <= -10; a++) for (int b = -1; b <= 0; b++) c.at(a, b, b == -1 ? wood[0] : wood[1], fit[2]);
		for (int a = -9; a <= -8; a++) for (int b = -2; b <= 1; b++) c.at(a, b, b <= -1 ? fit[0] : fit[1], fit[2]);
		c.at(-15, 0, fit[1], fit[2]);
		// The vine: from the grip's top toward the upper right, swaying.
		double x0 = 5.0, y0 = 10.0, x1 = 14.6, y1 = 1.4, nx = Math.sqrt(0.5), ny = Math.sqrt(0.5);
		double phase = 2 * Math.PI * f / FRAMES;
		double[][] path = new double[41][];
		for (int i = 0; i <= 40; i++) {
			double t = i / 40.0, sway = (0.3 + 1.6 * Math.pow(t, 1.3)) * Math.sin(2 * Math.PI * 1.1 * t - phase);
			path[i] = new double[] {lerp(x0, x1, t) + nx * sway, lerp(y0, y1, t) + ny * sway, t};
		}
		for (double[] p : path) {
			double r = lerp(0.62, 0.4, p[2]);
			for (int y = (int) Math.floor(p[1] - r); y <= (int) Math.ceil(p[1] + r); y++) {
				for (int x = (int) Math.floor(p[0] - r); x <= (int) Math.ceil(p[0] + r); x++) {
					double dx = x + 0.5 - p[0], dy = y + 0.5 - p[1];
					if (dx * dx + dy * dy > r * r + 0.15 || x < 0 || y < 0 || x > 15 || y > 15) continue;
					if (c.colour[x][y] != 0 && c.colour[x][y] != BeastArt.rgb(green) && c.colour[x][y] != BeastArt.rgb(light)) continue;
					c.set(x, y, BeastArt.rgb(dx + dy < -0.2 ? light : green), BeastArt.rgb(dark));
				}
			}
		}
		// Leaves on alternate sides, swaying with it: each a little pointed leaf, lit on top.
		double[] at = {0.2, 0.4, 0.6, 0.8};
		for (int i = 0; i < at.length; i++) {
			double[] p = path[(int) Math.round(at[i] * 40)];
			int side = i % 2 == 0 ? -1 : 1;
			int lx = (int) Math.floor(p[0] - side * 1.2), ly = (int) Math.floor(p[1] - side * 1.2);
			int[][] shape = side < 0 ? new int[][] {{0, 0}, {-1, -1}, {0, -1}} : new int[][] {{0, 0}, {1, 1}, {1, 0}};
			for (int[] d : shape) {
				int x = lx + d[0], y = ly + d[1];
				if (x < 0 || y < 0 || x > 15 || y > 15 || c.colour[x][y] != 0) continue;
				c.set(x, y, BeastArt.rgb(d[1] < 0 || d[0] == 0 && d[1] == 0 ? leaf : leafDark), BeastArt.rgb(dark));
			}
		}
		double[] tip = path[40];
		int tx = Math.min(15, (int) Math.floor(tip[0])), ty = Math.max(0, (int) Math.floor(tip[1]));
		c.set(tx, ty, BeastArt.rgb(bud), BeastArt.rgb(dark));
		// A mote of spirit qi running along it (two, from Heaven grade).
		for (int m = 0; m < (g >= 3 ? 2 : 1); m++) {
			double[] p = path[Math.floorMod(f * 5 + m * 20, 40)];
			int x = (int) Math.floor(p[0]), y = (int) Math.floor(p[1]);
			if (x >= 0 && y >= 0 && x < 16 && y < 16) c.set(x, y, BeastArt.rgb(mote), BeastArt.rgb(dark));
		}
		return c.done();
	}

	// --- The Mountain Hammer ---

	/**
	 * A head of mountain stone across a long haft, lit from the upper left, banded in the grade's fittings where they meet; the cracks
	 * through the stone glowing and dimming like breath (amber, an Earth one golden, a Heaven one violet, an Immortal one's in white jade
	 * gold), a grain of dust falling from it.
	 */
	static BufferedImage mountainHammer(ArtifactArt.Look look, int g, int f) {
		ArtifactArt.Canvas c = new ArtifactArt.Canvas();
		int[] fit = look.fitting(), wood = look.wood(), wrap = look.wrap();
		int[][] stones = {{}, {0xB0ACA2, 0x847F74, 0x58534A, 0x2C2824}, {0xC8A872, 0x9A7C4C, 0x6A5232, 0x34261A},
				{0x6E6680, 0x4C4560, 0x30293E, 0x16111E}, {0xF4F2E8, 0xD6D2C0, 0xA8A28A, 0x5A5440}};
		int[] glows = {0, 0xFFA838, 0xFFC850, 0xD27CFF, 0xFFE070};
		int[] stone = stones[g];
		double breath = 0.5 + 0.5 * Math.sin(2 * Math.PI * f / FRAMES);
		for (int a = -15; a <= 6; a++) {
			for (int b = -1; b <= 0; b++) {
				int col = a <= -10 ? (((a + 20) / 2 + b) % 2 == 0 ? wrap[0] : wrap[1]) : b == -1 ? wood[0] : wood[1];
				c.at(a, b, col, a <= -10 ? fit[2] : 0);
			}
		}
		c.at(-15, 0, fit[1], fit[2]);
		for (int a = 5; a <= 6; a++) for (int b = -2; b <= 1; b++) c.at(a, b, b <= -1 ? fit[0] : fit[1], fit[2]);
		// The head: a long block of stone across the haft's end, its top face lit.
		for (int a = 7; a <= 10; a++) {
			for (int b = -6; b <= 5; b++) {
				if (((a + b) & 1) == 0) continue;
				boolean lit = a >= 10 || b <= -5, shade = a <= 7 || b >= 4;
				int col = lit && !shade ? stone[0] : shade && !lit ? stone[2] : stone[1];
				if (BeastArt.hash(xOf(a, b), yOf(a, b), 61) < 0.18) col = BeastArt.shade(col, -0.08);
				c.at(a, b, BeastArt.rgb(col), BeastArt.rgb(stone[3]));
			}
		}
		// Its crack, a jagged seam across it, glowing as it breathes.
		int glow = BeastArt.mix(BeastArt.shade(glows[g], -0.45), glows[g], breath);
		int[][] cracks = {{8, -5}, {9, -4}, {9, -2}, {8, -1}, {9, 0}, {8, 1}, {9, 2}, {8, 3}};
		for (int[] crack : cracks) {
			c.at(crack[0], crack[1], BeastArt.rgb(BeastArt.mix(glow, 0xFFFFFF, g == 4 ? 0.25 * breath : 0)), BeastArt.rgb(stone[3]));
		}
		// A grain of dust falling from under the head.
		int dy = 7 + f;
		if (f < 6 && dy <= 13) c.set(12, dy, argb(stone[1], 200), 0);
		return c.done();
	}

	// --- A sheet to look over ---

	/** Every weapon, a row a kind, its four grades side by side, four frames each, five times over. */
	static void writeSheet(Path sheet) throws IOException {
		int scale = 5, cell = 16 * scale + 6;
		BufferedImage out = new BufferedImage(cell * 16 + 24, cell * KINDS.length, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, BeastArt.rgb((x / 8 + y / 8) % 2 == 0 ? 0x8B8B8B : 0x7A7A7A));
		for (int k = 0; k < KINDS.length; k++) {
			for (int g = 1; g <= 4; g++) {
				for (int i = 0; i < 4; i++) ArtifactArt.blit(out, frame(k, g, i * 2), ((g - 1) * 4 + i) * cell + (g - 1) * 8, k * cell, scale);
			}
		}
		BeastArt.write(sheet, out);
	}
}
