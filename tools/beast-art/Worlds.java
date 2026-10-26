import java.awt.image.BufferedImage;

/**
 * The small worlds' art (phase 3d, SmallWorlds): the World Seed's sprite (textures/item/world_seed.png, 16 x 16) and the Small World
 * ability's icon (textures/gui/ability/small_world.png, 32 x 32, in the Law domains' gold trim). {@code java BeastArt.java <assets> worlds}
 * writes these alone.
 */
public class Worlds {
	static void writeAll(java.nio.file.Path assets) throws java.io.IOException {
		BeastArt.write(assets.resolve("textures/item/world_seed.png"), seed());
		BeastArt.write(assets.resolve("textures/gui/ability/small_world.png"), icon());
	}

	/**
	 * The World Seed: a seed of chaos set hard, an almond of grey-violet stone lying from lower left to upper right, its tip the finer, lit
	 * from the upper left and edged dark, bound round with two bands of jade, a spark of starlight in its heart.
	 */
	static BufferedImage seed() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int pale = BeastArt.rgb(0xC4B8D6), light = BeastArt.rgb(0x9284AA), body = BeastArt.rgb(0x5E5270), deep = BeastArt.rgb(0x2E263A),
				rim = BeastArt.rgb(0x181220), jade = BeastArt.rgb(0x7EE0B0), jadeDeep = BeastArt.rgb(0x2C8558), star = BeastArt.rgb(0xFFF8DC),
				glow = BeastArt.rgb(0xFFE08A);
		double cx = 7.5, cy = 7.5, axis = Math.sqrt(0.5);
		boolean[][] in = new boolean[16][16];
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - cx, dy = y - cy;
				double u = (dx - dy) * axis, v = (dx + dy) * axis; // u along it (its tip at +u, to the upper right), v across
				double t = u / 6.6, half = 3.9 * Math.sqrt(Math.max(0, 1 - t * t)) * (1 - 0.3 * Math.max(0, t));
				in[x][y] = Math.abs(t) <= 1 && Math.abs(v) <= half;
			}
		}
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				if (!in[x][y]) continue;
				boolean edge = x == 0 || y == 0 || x == 15 || y == 15 || !in[x - 1][y] || !in[x + 1][y] || !in[x][y - 1] || !in[x][y + 1];
				double dx = x - cx, dy = y - cy, u = (dx - dy) * axis;
				double lit = Math.max(0, Math.min(1, 0.55 - (dx + dy) / 12.0));
				int c = lit > 0.62 ? BeastArt.mix(light, pale, (lit - 0.62) / 0.38) : lit > 0.3 ? BeastArt.mix(body, light, (lit - 0.3) / 0.32)
						: BeastArt.mix(deep, body, lit / 0.3);
				// The two bands of jade round it, across its length.
				if (Math.abs(u + 2.4) < 0.62 || Math.abs(u - 2.0) < 0.62) c = BeastArt.mix(jadeDeep, jade, lit + 0.2);
				if (edge) c = rim;
				img.setRGB(x, y, c);
			}
		}
		// The spark in its heart: a point of starlight, its four rays.
		img.setRGB(7, 8, star);
		for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) img.setRGB(7 + d[0], 8 + d[1], BeastArt.mix(glow, body, 0.25));
		return img;
	}

	/**
	 * The Small World: in the domains' gold-trimmed disc, a night sky all stars, black above and indigo below, and in it a floating
	 * island, its grass lit from above, its rock hanging beneath it to a point, a cherry tree and a little gate of starlight on it.
	 */
	static BufferedImage icon() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int skyTop = BeastArt.rgb(0x05061A), skyLow = BeastArt.rgb(0x26306E), white = BeastArt.rgb(0xF4F2FF);
		int gold = BeastArt.rgb(0xD9A22E), goldLight = BeastArt.rgb(0xF5CF55), goldDeep = BeastArt.rgb(0x6E5214);
		int grass = BeastArt.rgb(0x6FCB78), grassLight = BeastArt.rgb(0xA6EA9A), earth = BeastArt.rgb(0x6B4A2E), rock = BeastArt.rgb(0x6E6A78),
				rockDeep = BeastArt.rgb(0x3A3746), petal = BeastArt.rgb(0xF2A6C8), petalDeep = BeastArt.rgb(0xC46A98), trunk = BeastArt.rgb(0x4A2A22),
				jade = BeastArt.rgb(0x6FD3A4), gateLight = BeastArt.rgb(0xC9B8FF);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 15.5, dy = y - 15.5, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 12.6) {
					c = BeastArt.mix(skyTop, skyLow, (y - 3) / 24.0);
					if (BeastArt.hash(x, y, 77) < 0.06) c = BeastArt.mix(c, white, 0.5 + 0.5 * BeastArt.hash(x, y, 78)); // stars
				} else if (r < 13.5) {
					c = BeastArt.mix(skyLow, white, 0.25); // the rim of light
				} else if (r < 14.3) {
					c = dy < 0 ? goldLight : gold;
				} else if (r < 15.2) {
					c = dy < 0 ? gold : goldDeep;
				} else if (r < 15.8) {
					c = 0xA0 << 24 | goldDeep & 0xFFFFFF;
				}
				img.setRGB(x, y, c);
			}
		}
		// The island: its grass a flat ellipse, its rock hanging beneath it to a point.
		for (int y = 14; y <= 27; y++) {
			for (int x = 6; x <= 25; x++) {
				double dx = (x - 15.5) / 9.2, top = (y - 16.5) / 1.6;
				if (dx * dx + top * top <= 1 && y <= 17) {
					img.setRGB(x, y, y <= 15 ? grassLight : grass);
					continue;
				}
				if (y > 17) {
					double half = 9.2 * Math.max(0, 1 - (y - 17) / 9.5) * (1 + 0.15 * Math.sin(x * 1.7));
					if (Math.abs(x - 15.5) <= half) img.setRGB(x, y, y == 18 ? earth : BeastArt.mix(rock, rockDeep, (y - 18) / 9.0 + (x > 15.5 ? 0.15 : 0)));
				}
			}
		}
		// A cherry tree on its left.
		for (int y = 11; y <= 15; y++) img.setRGB(10, y, trunk);
		for (int y = 6; y <= 12; y++) {
			for (int x = 6; x <= 14; x++) {
				double dx = (x - 10) / 3.8, dy = (y - 9) / 3.0;
				if (dx * dx + dy * dy <= 1) img.setRGB(x, y, dy < -0.2 || dx < -0.3 ? petal : petalDeep);
			}
		}
		// A little gate of starlight on its right: jade posts, a lintel, starlight between.
		for (int y = 10; y <= 15; y++) {
			img.setRGB(18, y, jade);
			img.setRGB(22, y, jade);
			for (int x = 19; x <= 21; x++) img.setRGB(x, y, y == 10 ? jade : BeastArt.mix(gateLight, white, BeastArt.hash(x, y, 5) * 0.6));
		}
		for (int x = 17; x <= 23; x++) img.setRGB(x, 9, goldLight);
		return img;
	}
}
