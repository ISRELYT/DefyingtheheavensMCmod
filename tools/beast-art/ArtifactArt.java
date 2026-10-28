import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The Artifact Forge's art (2026-10-28): the artifacts' sprites, every kind at every grade (textures/item/<grade>_<kind>.png, 16 x 16), an
 * element banner's cloth a layer of its own (textures/item/element_banner_cloth.png: pale greys, to take its element's tint), and the
 * forge's faces (textures/block/artifact_forge_*.png: a bronze furnace, its mouth dark, or glowing while it works). A grade shows in its
 * metal (iron, gold, diamond, netherite, Immortal Jade: what it is forged of), its fittings and its tassel (its name's colour).
 * {@code java BeastArt.java <assets> artifacts} writes these alone; a path after that, a sheet of them all eight times over besides, to
 * look them over.
 * <p>
 * The long things lie as a vanilla sword does, from the lower left (the hand) to the upper right (the point), painted along {@code a}
 * (x - y: -15 at the lower left corner, 15 at the upper right) and across {@code b} (x + y - 15: 0 on the diagonal, less above it).
 */
public class ArtifactArt {
	static final String[] GRADES = {"mortal", "spirit", "earth", "heaven", "immortal"};

	/**
	 * One grade's colours, each light to dark: its metal (white, light, mid, dark, outline), its fittings (light, dark, outline), a grip's
	 * wrap (light, dark), its tassel (light, dark), a shaft's wood (light, dark), a gourd's body and a jade's stone (light, mid, dark,
	 * outline).
	 */
	record Look(int[] metal, int[] fitting, int[] wrap, int[] tassel, int[] wood, int[] gourd, int[] jade) {}

	static int[] c(int... hex) {
		int[] out = new int[hex.length];
		for (int i = 0; i < hex.length; i++) out[i] = BeastArt.rgb(hex[i]);
		return out;
	}

	static final Look[] LOOKS = {
			// Mortal: iron, plain bronze fittings, a leather grip, a dull red tassel, plain wood, a calabash, pale jade
			new Look(c(0xF2F4F6, 0xD0D5DA, 0xA2A9B0, 0x6E757C, 0x2E3236), c(0xB08A5A, 0x7A5A34, 0x3A2812), c(0x8E5A42, 0x5A3A26),
					c(0xC04040, 0x7A2020), c(0x9A6A48, 0x5E3E26), c(0xF0D890, 0xD0A458, 0x946A2C, 0x3E2A10),
					c(0xE8F4EC, 0xB8D8C4, 0x86B49A, 0x3E5E4A)),
			// Spirit: gold, jade fittings, a dark green grip, a green tassel, red lacquer, a jade-green gourd, green jade
			new Look(c(0xFFF8D0, 0xF5DA6A, 0xD9AE3E, 0xA0721E, 0x4A300C), c(0x72D19A, 0x2F8F5A, 0x0F3D27), c(0x4A6A3A, 0x26402A),
					c(0x6FE07A, 0x2A9A40), c(0xA0522D, 0x5E2A14), c(0xA8EBC4, 0x49B477, 0x2F8F5A, 0x0F3D27),
					c(0xA8EBC4, 0x49B477, 0x2F8F5A, 0x0F3D27)),
			// Earth: diamond, gold fittings, a navy grip, an aqua tassel, black lacquer, a crystal gourd, azure jade
			new Look(c(0xF0FFFF, 0xA8F4F0, 0x4FD8D2, 0x2A9E9C, 0x104A4C), c(0xF5CF55, 0xB8862A, 0x5A3E0C), c(0x3E5A8A, 0x1E2A4A),
					c(0x6AE4F0, 0x2A8AA8), c(0x4A4A5A, 0x24242E), c(0xD8FCFA, 0x7ADED8, 0x2E9EA0, 0x0E4648),
					c(0xC8F4FA, 0x6CCCE4, 0x2E8AAE, 0x0E3A50)),
			// Heaven: netherite with a violet gleam, old gold fittings, a purple grip, a violet tassel, purple-black lacquer, a violet gourd,
			// violet jade
			new Look(c(0xE8D8FF, 0x9A88AE, 0x64546E, 0x3E3248, 0x1A1220), c(0xF0D878, 0xA87A20, 0x4A3008), c(0x5A2E78, 0x2A1238),
					c(0xEE88FF, 0x9A30B0), c(0x5A3470, 0x2A1838), c(0xC8A8E8, 0x8A62B8, 0x52347E, 0x1E1030),
					c(0xE4D0FA, 0xAE8CDC, 0x7050AA, 0x2A1848)),
			// Immortal: Immortal Jade, bright gold fittings, a crimson grip, a gold tassel, ivory, a white-gold gourd, radiant white jade
			new Look(c(0xFFFFFF, 0xDDF9EA, 0x9AE0BE, 0x52B486, 0x1E5A40), c(0xFFE89A, 0xD9A22E, 0x6E4A0C), c(0xC0202A, 0x7A1018),
					c(0xFFD84A, 0xC08A10), c(0xF4ECD4, 0xBFA878), c(0xFFFDF0, 0xF4E4AE, 0xD4B464, 0x6A5418),
					c(0xFFFFFF, 0xF0F4DA, 0xCFC48A, 0x6A5A2A))};

	static void writeAll(Path assets) throws IOException {
		for (int g = 0; g < GRADES.length; g++) {
			Look look = LOOKS[g];
			Path items = assets.resolve("textures/item");
			BeastArt.write(items.resolve(GRADES[g] + "_flying_sword.png"), jian(look));
			BeastArt.write(items.resolve(GRADES[g] + "_saber.png"), dao(look));
			BeastArt.write(items.resolve(GRADES[g] + "_spear.png"), spear(look));
			BeastArt.write(items.resolve(GRADES[g] + "_glaive.png"), glaive(look));
			BeastArt.write(items.resolve(GRADES[g] + "_element_banner.png"), bannerPole(look));
			BeastArt.write(items.resolve(GRADES[g] + "_qi_gourd.png"), gourd(look, g == 3));
			BeastArt.write(items.resolve(GRADES[g] + "_protective_jade.png"), jade(look, g == 4));
		}
		BeastArt.write(assets.resolve("textures/item/element_banner_cloth.png"), bannerCloth());
		Path blocks = assets.resolve("textures/block");
		BeastArt.write(blocks.resolve("artifact_forge_side.png"), forgeSide(false));
		BeastArt.write(blocks.resolve("artifact_forge_front.png"), forgeSide(true, false));
		BeastArt.write(blocks.resolve("artifact_forge_front_lit.png"), forgeSide(true, true));
		BeastArt.write(blocks.resolve("artifact_forge_top.png"), forgeTop());
		BeastArt.write(blocks.resolve("artifact_forge_bottom.png"), forgeBottom());
		ElementalArt.writeAll(assets); // the weapons made of their element (phase 2), animated
	}

	// --- Painting ---

	/** A sprite being painted: each pixel's colour (0: clear), and the outline its part wants round it (0: none). */
	static final class Canvas {
		final int[][] colour = new int[16][16], outline = new int[16][16];

		void set(int x, int y, int c, int edge) {
			if (x < 0 || y < 0 || x > 15 || y > 15) return;
			colour[x][y] = c;
			outline[x][y] = edge;
		}

		/** By the diagonal's measures: the pixel {@code a} along it, {@code b} across (a + b odd, or there is none). */
		void at(int a, int b, int c, int edge) {
			if (((a + b) & 1) == 0) return;
			set((a + b + 15) / 2, (b - a + 15) / 2, c, edge);
		}

		/** Every clear pixel beside a painted one takes that one's outline: the dark edge round each part, as vanilla's items have. */
		BufferedImage done() {
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int c = colour[x][y];
					if (c == 0) {
						for (int[] d : new int[][] {{0, 1}, {-1, 0}, {1, 0}, {0, -1}}) {
							int nx = x + d[0], ny = y + d[1];
							if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && colour[nx][ny] != 0 && outline[nx][ny] != 0) {
								c = outline[nx][ny];
								break;
							}
						}
					}
					img.setRGB(x, y, c);
				}
			}
			return img;
		}
	}

	/** A grip from {@code a0} to {@code a1}, its wrap crossing over and under. */
	static void grip(Canvas k, Look look, int a0, int a1) {
		for (int a = a0; a <= a1; a++) {
			for (int b = -1; b <= 0; b++) k.at(a, b, ((a + 20) / 2 + b) % 2 == 0 ? look.wrap[0] : look.wrap[1], look.fitting[2]);
		}
	}

	/** A tassel hung from the pixel ({@code x}, {@code y}): its cord, then its threads fanning down. */
	static void tassel(Canvas k, Look look, int x, int y) {
		int light = look.tassel[0], dark = look.tassel[1];
		k.set(x, y, dark, 0);
		k.set(x - 1, y + 1, light, 0);
		k.set(x, y + 1, dark, 0);
		k.set(x - 1, y + 2, dark, 0);
		k.set(x, y + 2, light, 0);
	}

	/** The flying sword (jian): a slender straight blade, its ridge catching the light, a crossguard, a wrapped grip, a tassel. */
	static BufferedImage jian(Look look) {
		Canvas k = new Canvas();
		int[] m = look.metal, f = look.fitting;
		for (int a = -2; a <= 13; a++) {
			if (a <= 10) k.at(a, -1, a >= 6 ? m[0] : m[1], m[4]);
			k.at(a, 0, a >= 9 ? m[1] : m[2], m[4]);
		}
		for (int a = -4; a <= -3; a++) {
			for (int b = -3; b <= 2; b++) k.at(a, b, b == -3 || b == 2 ? f[1] : f[0], f[2]);
		}
		grip(k, look, -9, -5);
		for (int a = -11; a <= -10; a++) for (int b = -1; b <= 0; b++) k.at(a, b, b == -1 ? f[0] : f[1], f[2]);
		tassel(k, look, 2, 13);
		return k.done();
	}

	/** The saber (dao): a broad single-edged blade curving back toward its clipped point, its edge gleaming, a disc guard, a ring pommel. */
	static BufferedImage dao(Look look) {
		Canvas k = new Canvas();
		int[] m = look.metal, f = look.fitting;
		for (int a = -2; a <= 12; a++) {
			int back = a <= 5 ? -1 : -2;
			int edge = a <= 0 ? 1 : a <= 8 ? 2 : a == 9 ? 1 : a == 10 ? 0 : a == 11 ? -1 : -2;
			for (int b = back; b <= edge; b++) {
				int c = b == back ? m[1] : b == edge ? m[0] : b == back + 1 && a >= -1 && a <= 7 ? m[3] : m[2];
				k.at(a, b, c, m[4]);
			}
		}
		for (int a = -4; a <= -3; a++) {
			for (int b = -3; b <= 3; b++) k.at(a, b, Math.abs(b) == 3 ? f[1] : f[0], f[2]);
		}
		grip(k, look, -9, -5);
		for (int a = -12; a <= -10; a++) {
			for (int b = -2; b <= 1; b++) if (!(a == -11 && b == 0)) k.at(a, b, b <= -1 ? f[0] : f[1], f[2]);
		}
		tassel(k, look, 1, 14);
		return k.done();
	}

	/** The spear (qiang): a long shaft, a red tuft of horsehair below a leaf-shaped head. */
	static BufferedImage spear(Look look) {
		Canvas k = new Canvas();
		int[] m = look.metal, f = look.fitting, w = look.wood, t = look.tassel;
		for (int a = -15; a <= 3; a++) {
			k.at(a, -1, w[0], 0);
			k.at(a, 0, w[1], 0);
		}
		for (int a = -15; a <= -14; a++) for (int b = -1; b <= 0; b++) k.at(a, b, f[1], 0); // the butt's cap
		// The tuft: hanging down and back from the collar.
		for (int a = 1; a <= 5; a++) {
			for (int b = 1; b <= 4; b++) if (b - 1 <= (a - 1) / 2 + 1) k.at(a, b, b == 1 || (a + b) % 4 == 1 ? t[0] : t[1], 0);
		}
		for (int a = 3; a <= 5; a++) for (int b = -2; b <= 1; b++) k.at(a, b, b <= -1 ? f[0] : f[1], f[2]);
		for (int a = 6; a <= 14; a++) {
			int half = a <= 7 ? 1 : a <= 11 ? 2 : 1;
			int lo = -half, hi = half - 1;
			if (a == 14) lo = hi = -1;
			for (int b = lo; b <= hi; b++) {
				int c = b == -1 ? m[0] : b < -1 ? m[1] : b == 0 ? m[2] : m[3];
				k.at(a, b, c, m[4]);
			}
		}
		return k.done();
	}

	/** The glaive (guandao): a long shaft, a dragon-mouth collar, a great crescent blade, a barb on its back, a tassel. */
	static BufferedImage glaive(Look look) {
		Canvas k = new Canvas();
		int[] m = look.metal, f = look.fitting, w = look.wood, t = look.tassel;
		for (int a = -15; a <= 2; a++) {
			k.at(a, -1, w[0], 0);
			k.at(a, 0, w[1], 0);
		}
		for (int a = -15; a <= -13; a++) for (int b = -1; b <= 0; b++) k.at(a, b, f[1], 0);
		for (int a = -1; a <= 3; a++) {
			for (int b = 1; b <= 4; b++) if (b - 1 <= (a + 1) / 2 + 1) k.at(a, b, b == 1 || (a + b) % 4 == 1 ? t[0] : t[1], 0);
		}
		for (int a = 2; a <= 4; a++) for (int b = -2; b <= 1; b++) k.at(a, b, b <= -1 ? f[0] : f[1], f[2]);
		for (int a = 5; a <= 15; a++) {
			int edge = -1 - (int) Math.round(4.2 * Math.sin(Math.PI * (a - 4) / 12.0));
			int spine = a >= 13 ? 0 : 1;
			if (a == 15) edge = spine = 0;
			for (int b = edge; b <= spine; b++) {
				int c = b == edge ? m[0] : b == edge + 1 ? m[1] : b == spine ? m[3] : m[2];
				k.at(a, b, c, m[4]);
			}
		}
		for (int a = 6; a <= 8; a++) for (int b = 2; b <= 3; b++) if (!(a == 8 && b == 3)) k.at(a, b, b == 2 ? m[2] : m[3], m[4]);
		return k.done();
	}

	/** An element banner's pole (its first layer): a lacquered pole, a pointed finial and a crossbar, a tassel off its end. */
	static BufferedImage bannerPole(Look look) {
		Canvas k = new Canvas();
		int[] f = look.fitting, w = look.wood, t = look.tassel;
		for (int y = 2; y <= 15; y++) {
			k.set(2, y, w[0], 0);
			k.set(3, y, w[1], 0);
		}
		k.set(2, 0, f[0], f[2]);
		k.set(3, 0, f[1], f[2]);
		for (int x = 1; x <= 4; x++) k.set(x, 1, x <= 2 ? f[0] : f[1], f[2]);
		for (int x = 4; x <= 13; x++) k.set(x, 2, w[0], 0);
		k.set(14, 2, f[0], f[2]);
		k.set(14, 3, t[1], 0);
		k.set(14, 4, t[0], 0);
		k.set(13, 5, t[1], 0);
		k.set(14, 5, t[0], 0);
		k.set(15, 5, t[1], 0);
		return k.done();
	}

	/**
	 * An element banner's cloth (its second layer, the same for every grade): pale greys, so its element's tint (ElementBannerItem#colour)
	 * makes it that colour, a dark seal on it; hung from the crossbar, cut into two tails below.
	 */
	static BufferedImage bannerCloth() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int base = BeastArt.rgb(0xF6F6F6), fold = BeastArt.rgb(0xDCDCDC), shade = BeastArt.rgb(0xC4C4C4), edge = BeastArt.rgb(0x8C8C8C),
				seal = BeastArt.rgb(0x5A5A5A);
		boolean[][] in = new boolean[16][16];
		for (int y = 3; y <= 13; y++) {
			for (int x = 4; x <= 13; x++) in[x][y] = !(y >= 11 && Math.abs(x + 0.5 - 9) <= y - 10);
		}
		for (int y = 3; y <= 13; y++) {
			for (int x = 4; x <= 13; x++) {
				if (!in[x][y]) continue;
				boolean border = x == 13 || y == 13 || !in[x][y + 1] || (x > 4 && !in[x - 1][y]) || !in[x + 1 > 15 ? 15 : x + 1][y];
				double dx = x + 0.5 - 9, dy = y + 0.5 - 7.2, r = Math.sqrt(dx * dx + dy * dy);
				int c = x == 7 || x == 10 ? fold : x >= 11 ? shade : base;
				if (r >= 1.9 && r <= 2.9 || r < 0.8) c = seal;
				if (border) c = edge;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/**
	 * A qi gourd: a calabash of two bulbs, lit from the upper left, its stopper of wood, a cord round its waist in the grade's colour and
	 * a tassel off it; a Heaven gourd banded in gold besides.
	 */
	static BufferedImage gourd(Look look, boolean banded) {
		Canvas k = new Canvas();
		int[] g = look.gourd, t = look.tassel, w = look.wood, f = look.fitting;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double px = x + 0.5, py = y + 0.5;
				double[][] bulbs = {{8, 10.6, 4.3}, {8, 4.6, 2.4}};
				for (double[] bulb : bulbs) {
					double nx = (px - bulb[0]) / bulb[2], ny = (py - bulb[1]) / bulb[2];
					if (nx * nx + ny * ny > 1) continue;
					double lit = -0.6 * nx - 0.8 * ny;
					int c = lit > 0.55 ? g[0] : lit > -0.2 ? g[1] : g[2];
					if (banded && (y == 12 || y == 4)) c = lit > 0 ? f[0] : f[1];
					k.set(x, y, c, g[3]);
					break;
				}
			}
		}
		k.set(6, 9, BeastArt.mix(g[0], BeastArt.rgb(0xFFFFFF), 0.6), g[3]); // the glints
		k.set(7, 3, BeastArt.mix(g[0], BeastArt.rgb(0xFFFFFF), 0.6), g[3]);
		for (int x = 7; x <= 8; x++) {
			k.set(x, 0, w[0], w[1]);
			k.set(x, 1, x == 7 ? w[0] : w[1], w[1]);
		}
		for (int x = 6; x <= 9; x++) k.set(x, 6, x <= 7 ? t[0] : t[1], 0);
		k.set(10, 6, t[1], 0);
		k.set(10, 7, t[0], 0);
		k.set(11, 8, t[1], 0);
		k.set(10, 8, t[0], 0);
		k.set(11, 9, t[0], 0);
		k.set(10, 9, t[1], 0);
		return k.done();
	}

	/**
	 * A protective jade: a carved disc (a bi) with a hole through it, lit from the upper left, a ring of cloud-carving round it (an
	 * Immortal one's inlaid with gold), hung from a loop of cord in the grade's colour.
	 */
	static BufferedImage jade(Look look, boolean gilded) {
		Canvas k = new Canvas();
		int[] j = look.jade, t = look.tassel;
		double cx = 8, cy = 9.4;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x + 0.5 - cx, dy = y + 0.5 - cy, r = Math.sqrt(dx * dx + dy * dy);
				if (r > 5.5 || r <= 2.3) continue;
				double lit = (-0.6 * dx - 0.8 * dy) / r;
				int c = lit > 0.35 ? j[0] : lit > -0.45 ? j[1] : j[2];
				if (r <= 3.0) c = lit > 0.35 ? j[2] : j[1]; // the hole's rim, lit the other way
				double angle = Math.atan2(dy, dx);
				if (r >= 3.6 && r <= 4.4 && ((int) Math.floor((angle + Math.PI) / (Math.PI / 5))) % 2 == 0) c = gilded ? look.fitting[1] : j[2]; // the carving
				k.set(x, y, c, j[3]);
			}
		}
		k.set(7, 0, t[0], 0);
		k.set(8, 0, t[1], 0);
		k.set(6, 1, t[0], 0);
		k.set(9, 1, t[1], 0);
		k.set(7, 2, t[1], 0);
		k.set(8, 2, t[0], 0);
		k.set(7, 3, t[0], 0);
		k.set(8, 3, t[1], 0);
		BufferedImage img = k.done();
		// The hole: see-through, its outline left round it.
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x + 0.5 - cx, dy = y + 0.5 - cy;
				if (dx * dx + dy * dy <= 1.7 * 1.7) img.setRGB(x, y, 0);
			}
		}
		return img;
	}

	// --- The forge ---

	static final int BRONZE_HI = BeastArt.rgb(0xE6B874), BRONZE_LIGHT = BeastArt.rgb(0xC8954A), BRONZE = BeastArt.rgb(0xA87434),
			BRONZE_DARK = BeastArt.rgb(0x7A5020), BRONZE_DEEP = BeastArt.rgb(0x4E3214), PATINA = BeastArt.rgb(0x5E9E86),
			PATINA_DARK = BeastArt.rgb(0x3E7462), SOOT = BeastArt.rgb(0x1A0E08), EMBER = BeastArt.rgb(0x7A2208);

	static BufferedImage forgeSide(boolean front) {
		return forgeSide(front, false);
	}

	/**
	 * A face of the forge, in bands as its model takes them (models/block/artifact_forge): rows 0-1 its chimney, 2-3 its lip, 4-13 its
	 * belly (a raised band of thunder pattern, rivets, patina gathering low), 14-15 its feet. Its front has a mouth in its belly, an arch
	 * framed in bright bronze, dark with embers, or ({@code lit}) full of fire, brightest at its floor.
	 */
	static BufferedImage forgeSide(boolean front, boolean lit) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c;
				if (y <= 1) {
					c = y == 0 ? BRONZE_LIGHT : BRONZE;
				} else if (y <= 3) {
					c = y == 2 ? BRONZE_HI : BRONZE_DEEP;
				} else if (y <= 13) {
					c = BeastArt.mix(BRONZE_LIGHT, BRONZE_DARK, (y - 4) / 9.0);
					if (y == 5 || y == 8) c = BRONZE_HI; // the band's raised rims
					if (y == 6 || y == 7) { // the thunder pattern: a square spiral every four
						int i = x % 4;
						boolean groove = y == 6 ? i != 3 : i == 0 || i == 2;
						c = groove ? BRONZE_DARK : BRONZE;
					}
					if (y == 13) c = BRONZE_DEEP;
					if (x == 2 || x == 13) c = BeastArt.shade(c, -0.18); // its belly rounding away
					if ((x == 3 || x == 12) && y == 10) c = BRONZE_HI; // rivets
					if ((x == 3 || x == 12) && y == 11) c = BRONZE_DEEP;
					if (y >= 9 && y <= 12 && BeastArt.hash(x, y, 31) < 0.06 + (y - 9) * 0.04) c = BeastArt.hash(x, y, 32) < 0.5 ? PATINA : PATINA_DARK;
				} else {
					c = y == 14 ? BRONZE_DARK : BRONZE_DEEP;
				}
				img.setRGB(x, y, c);
			}
		}
		if (front) {
			// The mouth's frame: a bright arch, a sill beneath.
			for (int x = 6; x <= 9; x++) img.setRGB(x, 7, BRONZE_HI);
			img.setRGB(5, 8, BRONZE_HI);
			img.setRGB(10, 8, BRONZE_HI);
			for (int y = 9; y <= 12; y++) {
				img.setRGB(4, y, BRONZE_HI);
				img.setRGB(11, y, BRONZE_LIGHT);
			}
			for (int x = 4; x <= 11; x++) img.setRGB(x, 13, BRONZE_LIGHT);
			// The mouth itself.
			for (int y = 8; y <= 12; y++) {
				for (int x = 5; x <= 10; x++) {
					if (y == 8 && (x == 5 || x == 10)) continue;
					int c;
					if (lit) {
						double heat = (y - 8) / 4.0 + (BeastArt.hash(x, y, 41) - 0.5) * 0.3;
						c = heat > 0.85 ? BeastArt.rgb(0xFFF0B0) : heat > 0.6 ? BeastArt.rgb(0xFFC040) : heat > 0.35 ? BeastArt.rgb(0xF07818)
								: heat > 0.12 ? BeastArt.rgb(0xC8420A) : BeastArt.rgb(0x7A1C06);
					} else {
						c = y == 12 && BeastArt.hash(x, y, 42) < 0.5 ? EMBER : SOOT;
					}
					img.setRGB(x, y, c);
				}
			}
		}
		return img;
	}

	/** The forge's top: its lip's rim bright, a groove round, and (5-10) its chimney's mouth, dark and ringed, an ember deep in it. */
	static BufferedImage forgeTop() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = BeastArt.mix(BRONZE_LIGHT, BRONZE, (x + y) / 30.0);
				if (x <= 1 || y <= 1 || x >= 14 || y >= 14) c = BRONZE_HI;
				if ((x == 3 || x == 12) && y >= 3 && y <= 12 || (y == 3 || y == 12) && x >= 3 && x <= 12) c = BRONZE_DARK;
				if (x >= 5 && x <= 10 && y >= 5 && y <= 10) {
					c = x == 5 || y == 5 ? BRONZE_HI : x == 10 || y == 10 ? BRONZE_DARK : SOOT;
					if (x >= 7 && x <= 8 && y >= 7 && y <= 8) c = EMBER;
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	static BufferedImage forgeBottom() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = BeastArt.mix(BRONZE_DARK, BRONZE_DEEP, BeastArt.hash(x, y, 51) * 0.6);
				if (BeastArt.hash(x, y, 52) < 0.08) c = SOOT;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	// --- A sheet to look over ---

	/** Every sprite eight times over on a grey ground (a row a grade; the banners tinted each element), then the forge's faces. */
	static void writeSheet(Path assets, Path sheet) throws IOException {
		int scale = 8, cell = 16 * scale + 8;
		int[] elements = {0x3CB45A, 0x3C78E6, 0xE64A20, 0xC88A2B, 0xE8D070};
		BufferedImage out = new BufferedImage(cell * 11, cell * 6, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, BeastArt.rgb((x / 8 + y / 8) % 2 == 0 ? 0x8B8B8B : 0x7A7A7A));
		BufferedImage cloth = bannerCloth();
		for (int g = 0; g < GRADES.length; g++) {
			Look look = LOOKS[g];
			BufferedImage[] row = {jian(look), dao(look), spear(look), glaive(look), gourd(look, g == 3), jade(look, g == 4)};
			for (int i = 0; i < row.length; i++) blit(out, row[i], i * cell, g * cell, scale);
			for (int e = 0; e < elements.length; e++) {
				BufferedImage banner = bannerPole(look);
				for (int y = 0; y < 16; y++) {
					for (int x = 0; x < 16; x++) {
						int cl = cloth.getRGB(x, y);
						if (cl >>> 24 == 0) continue;
						int tint = elements[e];
						int r = (cl >> 16 & 255) * (tint >> 16 & 255) / 255, gg = (cl >> 8 & 255) * (tint >> 8 & 255) / 255, b = (cl & 255) * (tint & 255) / 255;
						banner.setRGB(x, y, 0xFF000000 | r << 16 | gg << 8 | b);
					}
				}
				blit(out, banner, (6 + e) * cell, g * cell, scale);
			}
		}
		BufferedImage[] forge = {forgeSide(false), forgeSide(true, false), forgeSide(true, true), forgeTop(), forgeBottom()};
		for (int i = 0; i < forge.length; i++) blit(out, forge[i], i * cell, 5 * cell, scale);
		BeastArt.write(sheet, out);
	}

	static void blit(BufferedImage out, BufferedImage img, int ox, int oy, int scale) {
		for (int y = 0; y < img.getHeight() * scale; y++) {
			for (int x = 0; x < img.getWidth() * scale; x++) {
				int c = img.getRGB(x / scale, y / scale);
				if (c >>> 24 != 0) out.setRGB(ox + x, oy + y, c);
			}
		}
	}
}
