import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Sacred Vessels' item icons (2026-10-25, SacredVesselItem: textures/item/<id>.png, 16 x 16, in the game's own item manner: lit from
 * the upper left, a dark outline round them). Each vessel is laid row by row as half-widths about the middle, in its materials, then
 * shaded and outlined:
 * <ul>
 *   <li>the Jianmu Bough Vase: a jade-green bronze meiping, gold at its rim, shoulder and foot, a sprig of the world tree budding from it;</li>
 *   <li>the Heaven Furnace Censer: a round bronze censer on three legs, its domed lid, its ears, fire leaping from its knob;</li>
 *   <li>the Nine Provinces Ding: a square cauldron of earthen bronze, patina on it, its two upright handles and its legs;</li>
 *   <li>the Immortal-Forging Bell: a silver bell banded in gold, its studs, the loop it hangs from;</li>
 *   <li>the Sealed Ocean Gourd: a deep blue calabash, a red cord at its waist, a gold stopper.</li>
 * </ul>
 * {@code java Relics.java <the mod's src/main/resources/assets/defying-the-heavens>}
 */
public class Relics {
	static int rgb(int hex) { return 0xFF000000 | hex; }

	/** {@code c} lightened (f > 1) or darkened (f < 1). */
	static int shade(int c, double f) {
		int r = (int) Math.min(255, Math.round((c >> 16 & 255) * f)), g = (int) Math.min(255, Math.round((c >> 8 & 255) * f)),
				b = (int) Math.min(255, Math.round((c & 255) * f));
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	static final class Canvas {
		final int[][] px = new int[16][16];
		/** Pixels laid as details (not shaded with the body's light). */
		final boolean[][] flat = new boolean[16][16];

		void set(int x, int y, int c) {
			if (x >= 0 && x < 16 && y >= 0 && y < 16) px[y][x] = c;
		}

		void detail(int x, int y, int c) {
			if (x < 0 || x >= 16 || y < 0 || y >= 16) return;
			px[y][x] = c;
			flat[y][x] = true;
		}

		/** Row {@code y} filled {@code hw} either side of the middle (x 8 - hw .. 7 + hw). */
		void row(int y, int hw, int c) {
			for (int x = 8 - hw; x <= 7 + hw; x++) set(x, y, c);
		}

		boolean drawn(int x, int y) { return x >= 0 && x < 16 && y >= 0 && y < 16 && px[y][x] != 0; }

		/** Light from the upper left: each row's leftmost pixels lighter, its rightmost darker, its lowest rows darker still. */
		void light() {
			int[][] lit = new int[16][16];
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int c = px[y][x];
					if (c == 0 || flat[y][x]) {
						lit[y][x] = c;
						continue;
					}
					double f = 1;
					if (!drawn(x - 1, y) || !drawn(x - 1, y - 1)) f = 1.3;
					else if (!drawn(x - 2, y)) f = 1.12;
					if (!drawn(x + 1, y) || !drawn(x + 1, y + 1)) f = 0.68;
					else if (!drawn(x + 2, y)) f = 0.84;
					if (!drawn(x, y + 1)) f *= 0.8;
					lit[y][x] = shade(c, f);
				}
			}
			for (int y = 0; y < 16; y++) px[y] = lit[y];
		}

		/** A dark outline: every empty pixel beside a drawn one takes that one's colour, deeply darkened. */
		void outline() {
			int[][] out = new int[16][16];
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					out[y][x] = px[y][x];
					if (px[y][x] != 0) continue;
					for (int[] n : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
						if (drawn(x + n[0], y + n[1])) {
							out[y][x] = shade(px[y + n[1]][x + n[0]], 0.32);
							break;
						}
					}
				}
			}
			for (int y = 0; y < 16; y++) px[y] = out[y];
		}

		BufferedImage image() {
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, px[y][x]);
			return img;
		}
	}

	static final int GOLD = rgb(0xD9B13F), GOLD_LIGHT = rgb(0xF6DC7A);

	static Canvas vase() {
		Canvas c = new Canvas();
		int jade = rgb(0x3E8F62);
		int[][] rows = {{3, 2}, {4, 1}, {5, 2}, {6, 4}, {7, 5}, {8, 5}, {9, 5}, {10, 5}, {11, 4}, {12, 3}, {13, 2}, {14, 3}};
		for (int[] r : rows) c.row(r[0], r[1], r[0] == 3 || r[0] == 7 || r[0] >= 13 ? GOLD : jade);
		c.light();
		// The sprig of the world tree: a stem from the mouth, a leaf either side, a bud.
		int stem = rgb(0x6B4A2C), leaf = rgb(0x7ED45A), leafDark = rgb(0x4E9A38);
		c.detail(8, 2, stem);
		c.detail(8, 1, stem);
		c.detail(9, 0, leaf);
		c.detail(6, 1, leafDark);
		c.detail(7, 1, leaf);
		c.detail(9, 1, leaf);
		c.detail(10, 1, leafDark);
		// Its pattern: a cloud scroll of lighter jade on its belly.
		for (int[] p : new int[][] {{6, 9}, {7, 10}, {8, 10}, {9, 9}}) c.detail(p[0], p[1], rgb(0x7CC89A));
		c.outline();
		return c;
	}

	static Canvas censer() {
		Canvas c = new Canvas();
		int bronze = rgb(0xA8692F), lid = rgb(0x8C5426);
		c.row(3, 1, GOLD); // the knob
		c.row(4, 2, lid);
		c.row(5, 3, lid);
		c.row(6, 4, lid);
		c.row(7, 6, GOLD); // the rim
		int[] body = {6, 6, 6, 5, 4};
		for (int i = 0; i < body.length; i++) c.row(8 + i, body[i], bronze);
		for (int x : new int[] {4, 7, 8, 11}) {
			c.set(x, 13, bronze);
			c.set(x, 14, bronze);
		}
		c.set(1, 8, GOLD); // its ears
		c.set(1, 9, GOLD);
		c.set(14, 8, GOLD);
		c.set(14, 9, GOLD);
		c.light();
		// Fire from the knob.
		int flame = rgb(0xFF8A1E), flameLight = rgb(0xFFE15E);
		c.detail(7, 2, flame);
		c.detail(8, 2, flameLight);
		c.detail(8, 1, flame);
		c.detail(7, 0, flameLight);
		c.detail(9, 1, flameLight);
		// The vents in its lid.
		c.detail(6, 5, rgb(0xFFB040));
		c.detail(9, 5, rgb(0xFFB040));
		c.outline();
		return c;
	}

	static Canvas ding() {
		Canvas c = new Canvas();
		int bronze = rgb(0x6F5733);
		for (int x : new int[] {3, 4, 11, 12}) c.set(x, 2, GOLD); // the handles
		for (int y = 3; y <= 4; y++) {
			c.set(3, y, GOLD);
			c.set(12, y, GOLD);
		}
		c.row(5, 7, GOLD); // the rim
		for (int y = 6; y <= 10; y++) c.row(y, 6, bronze);
		c.row(11, 5, bronze);
		for (int x : new int[] {4, 5, 10, 11}) c.set(x, 12, bronze);
		for (int x : new int[] {4, 11}) {
			c.set(x, 13, bronze);
			c.set(x, 14, bronze);
		}
		c.light();
		// Patina, and the taotie mask on its belly in gold.
		int patina = rgb(0x5C8E6C);
		for (int[] p : new int[][] {{3, 7}, {12, 9}, {5, 10}, {10, 6}}) c.detail(p[0], p[1], patina);
		for (int[] p : new int[][] {{6, 7}, {9, 7}, {7, 8}, {8, 8}, {6, 9}, {9, 9}}) c.detail(p[0], p[1], GOLD_LIGHT);
		c.outline();
		return c;
	}

	static Canvas bell() {
		Canvas c = new Canvas();
		int silver = rgb(0xC9CEDA);
		for (int x : new int[] {7, 8}) c.set(x, 0, GOLD); // the loop
		c.set(6, 1, GOLD);
		c.set(9, 1, GOLD);
		c.row(2, 2, GOLD);
		c.row(3, 3, silver);
		int[] body = {4, 4, 4, 5, 5, 5, 5, 6, 6};
		for (int i = 0; i < body.length; i++) c.row(4 + i, body[i], i == 4 ? GOLD : silver);
		c.row(13, 6, GOLD); // its mouth
		c.light();
		// Its studs of gold, in rows either side.
		for (int y : new int[] {5, 7}) {
			for (int x : new int[] {5, 6, 9, 10}) c.detail(x, y, GOLD);
		}
		c.outline();
		return c;
	}

	static Canvas gourd() {
		Canvas c = new Canvas();
		int blue = rgb(0x2F63B2);
		c.row(0, 1, GOLD); // the stopper
		c.row(1, 1, GOLD);
		int[] top = {1, 2, 3, 3, 2};
		for (int i = 0; i < top.length; i++) c.row(2 + i, top[i], blue);
		c.row(7, 1, rgb(0xC33232)); // the cord
		int[] bottom = {3, 4, 5, 5, 5, 4, 3};
		for (int i = 0; i < bottom.length; i++) c.row(8 + i, bottom[i], blue);
		c.light();
		// The cord's tassel, and a wave on its belly.
		c.detail(9, 8, rgb(0xC33232));
		c.detail(10, 9, rgb(0xE25050));
		for (int[] p : new int[][] {{5, 11}, {6, 10}, {7, 11}, {8, 12}, {9, 11}}) c.detail(p[0], p[1], rgb(0x7FB6F0));
		c.outline();
		return c;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("java Relics.java <the mod's src/main/resources/assets/defying-the-heavens>");
			System.exit(1);
		}
		Path items = Path.of(args[0], "textures", "item");
		Files.createDirectories(items);
		ImageIO.write(vase().image(), "png", items.resolve("jianmu_bough_vase.png").toFile());
		ImageIO.write(censer().image(), "png", items.resolve("heaven_furnace_censer.png").toFile());
		ImageIO.write(ding().image(), "png", items.resolve("nine_provinces_ding.png").toFile());
		ImageIO.write(bell().image(), "png", items.resolve("immortal_forging_bell.png").toFile());
		ImageIO.write(gourd().image(), "png", items.resolve("sealed_ocean_gourd.png").toFile());
		System.out.println("written");
	}
}
