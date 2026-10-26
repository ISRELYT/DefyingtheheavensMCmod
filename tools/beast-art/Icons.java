import java.awt.image.BufferedImage;

/**
 * The races' acts' icons for the ability wheel and the Abilities tab (textures/gui/ability/<id>.png, 32 x 32, soft-edged like the
 * others): Tiger's Roar (a striped tiger's head, its jaws wide, the roar going out from it), Illusion (a fox's mask, pale, fading);
 * and Immortal Essence's (a pearl of immortal gold, its light raying out). {@code java BeastArt.java <assets> icons} writes these alone.
 */
public class Icons {
	static void writeAll(java.nio.file.Path assets) throws java.io.IOException {
		BeastArt.write(assets.resolve("textures/gui/ability/tigers_roar.png"), roar());
		BeastArt.write(assets.resolve("textures/gui/ability/illusion.png"), illusion());
		BeastArt.write(assets.resolve("textures/gui/ability/immortal_essence.png"), essence());
		BeastArt.write(assets.resolve("textures/item/immortal_jade.png"), jade());
		BeastArt.write(assets.resolve("textures/item/court_merit_token.png"), token());
	}

	/**
	 * A Court Merit Token's sprite (16 x 16): a command token of gold, an upright plaque arched at its top, lit from the upper left, its rim
	 * the deeper gold; set in its face a panel of jade carved with the Court's mark (heaven's, as 天: a short stroke over a long one, and
	 * the figure under them); a red cord through its top, a tassel hanging below.
	 */
	static BufferedImage token() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int pale = BeastArt.rgb(0xFFF3B0), light = BeastArt.rgb(0xF5CF55), gold = BeastArt.rgb(0xD9A22E), deep = BeastArt.rgb(0x8A5A12),
				jade = BeastArt.rgb(0x6FD3A4), jadeDeep = BeastArt.rgb(0x2C7A55), red = BeastArt.rgb(0xC4302B), redDeep = BeastArt.rgb(0x7A1A16);
		for (int y = 1; y <= 12; y++) {
			for (int x = 4; x <= 11; x++) {
				// The arch: its top two rows cut in at the corners.
				if (y == 1 && (x < 6 || x > 9) || y == 2 && (x < 5 || x > 10)) continue;
				boolean rim = x == 4 || x == 11 || y == 12 || y == 1 || y == 2 && (x == 5 || x == 10) || y == 3 && (x == 4 || x == 11);
				double lit = 1 - ((x - 4) / 7.0 + (y - 1) / 11.0) / 2;
				int c = lit > 0.6 ? BeastArt.mix(light, pale, (lit - 0.6) / 0.4) : BeastArt.mix(gold, light, lit / 0.6);
				if (rim) c = BeastArt.mix(c, deep, 0.55);
				img.setRGB(x, y, c);
			}
		}
		// The jade panel, lit along its top and left edges, its mark carved deep.
		for (int y = 4; y <= 10; y++) {
			for (int x = 6; x <= 9; x++) img.setRGB(x, y, x == 6 || y == 4 ? jade : BeastArt.mix(jade, jadeDeep, 0.35));
		}
		for (int[] m : new int[][] {{7, 5}, {8, 5}, {6, 6}, {7, 6}, {8, 6}, {9, 6}, {7, 7}, {8, 7}, {6, 8}, {9, 8}, {6, 9}, {9, 9}}) img.setRGB(m[0], m[1], jadeDeep);
		// The cord through its top, and the tassel below, splaying at its end.
		img.setRGB(7, 0, red);
		img.setRGB(8, 0, red);
		for (int y = 13; y <= 15; y++) {
			img.setRGB(7, y, y == 15 ? redDeep : red);
			img.setRGB(8, y, y == 15 ? redDeep : red);
		}
		img.setRGB(6, 15, redDeep);
		img.setRGB(9, 15, redDeep);
		return img;
	}

	/**
	 * Immortal Jade's sprite (16 x 16, a flat item's: the game makes it a disc a pixel thick): a bi, a disc of jade with a hole in its
	 * middle, lit from the upper left, a gloss on its shoulder, its face carved with a ring of raised grains, its rims cut.
	 */
	static BufferedImage jade() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int pale = BeastArt.rgb(0xE2FCEE), light = BeastArt.rgb(0xA6EAC6), mid = BeastArt.rgb(0x66C798), deep = BeastArt.rgb(0x2C8558),
				rim = BeastArt.rgb(0x1E6644);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - 7.5, dy = y - 7.5, r = Math.sqrt(dx * dx + dy * dy);
				if (r > 7.25 || r < 2.4) continue; // beyond it, and the hole in its middle (square, as a coin's)
				double lit = Math.max(0, Math.min(1, 0.55 - (dx + dy) / 18.0));
				int c = lit > 0.5 ? BeastArt.mix(mid, light, (lit - 0.5) / 0.5) : BeastArt.mix(deep, mid, lit / 0.5);
				boolean gloss = r > 4.2 && r < 6.3 && dx + dy < -5.2;
				if (gloss) c = BeastArt.mix(c, pale, 0.6);
				else if (r > 4.3 && r < 5.8 && (x + y) % 2 == 0) c = BeastArt.mix(c, pale, 0.3); // the grains
				if (r > 6.8 || r < 3.0) c = BeastArt.mix(c, rim, 0.45); // the rims, cut
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** Immortal Essence: a pearl of immortal gold lit from the upper left, a soft halo about it, eight rays of its light going out. */
	static BufferedImage essence() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int white = BeastArt.rgb(0xFFFBEA), pale = BeastArt.rgb(0xFFE27A), gold = BeastArt.rgb(0xF0B43A), deep = BeastArt.rgb(0x8E5A0C);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 15.5, dy = y - 15.5;
				double r = Math.sqrt(dx * dx + dy * dy);
				double angle = Math.atan2(dy, dx);
				int c = 0;
				if (r >= 7.5) {
					// The rays, every 45 degrees, the four straight ones the longer, tapering and fading as they go.
					boolean straight = Math.abs(Math.sin(2 * angle)) < 0.5;
					double reach = straight ? 15.8 : 12.5;
					double along = (r - 7.5) / (reach - 7.5);
					double half = 1.15 * (1 - along) + 0.3;
					int alpha = 0;
					if (along < 1 && r * Math.abs(Math.sin(4 * angle)) / 4 < half) alpha = (int) Math.round(255 * Math.pow(1 - along, 1.1));
					// The halo about the pearl.
					if (r < 10.5) alpha = Math.max(alpha, (int) Math.round(170 * (1 - (r - 7.5) / 3.0)));
					if (alpha > 0) c = alpha << 24 | BeastArt.mix(pale, gold, along) & 0xFFFFFF;
				}
				if (r < 8) {
					// The pearl, lit from the upper left, its rim the deeper gold.
					double lx = dx + 3, ly = dy + 3;
					double lit = 1 - Math.min(1, Math.sqrt(lx * lx + ly * ly) / 12.5);
					c = lit > 0.78 ? BeastArt.mix(pale, white, (lit - 0.78) / 0.22) : lit > 0.42 ? BeastArt.mix(gold, pale, (lit - 0.42) / 0.36)
							: BeastArt.mix(deep, gold, lit / 0.42);
					// The essence turning in it: a faint swirl of paler light toward the middle.
					double swirl = Math.sin(angle * 2 + r * 0.7);
					if (r < 6.5 && swirl > 0.55) c = BeastArt.mix(c, white, 0.28 * (swirl - 0.55) / 0.45 * (1 - r / 6.5));
					if (r > 7) c = BeastArt.mix(c, deep, 0.45);
				}
				img.setRGB(x, y, c);
			}
		}
		// Sparkles of it in the air about.
		for (int[] s : new int[][] {{5, 6}, {27, 9}, {7, 26}}) {
			img.setRGB(s[0], s[1], white);
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) img.setRGB(s[0] + d[0], s[1] + d[1], 0x90 << 24 | 0xFFF2B0);
		}
		return img;
	}

	static BufferedImage roar() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int orange = BeastArt.rgb(0xE88A2A), dark = BeastArt.rgb(0x1C1410), white = BeastArt.rgb(0xF6EAD2), mouth = BeastArt.rgb(0x8A1A14);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				double dx = x - 11.5, dy = y - 16.5;
				double r = Math.sqrt(dx * dx / 1.0 + dy * dy / 1.2);
				int c = 0;
				if (r < 9.5) {
					c = BeastArt.fur(orange, x, y, 1);
					if ((x + y / 2) % 4 == 0 && Math.abs(dx) > 2.5 && dy < 3) c = dark; // stripes
					if (dy > 1 && Math.abs(dx) < 5) c = BeastArt.fur(white, x, y, 2); // muzzle
					if (dy > 3.5 && dy < 7.5 && Math.abs(dx) < 3.5) c = mouth; // the jaws wide
					if (dy > 3.5 && dy < 4.5 && (x == 9 || x == 14)) c = white; // fangs
					if (dy > -3.5 && dy < -2 && (x == 8 || x == 15)) c = BeastArt.rgb(0xF2C030); // eyes
				}
				// Ears.
				if ((Math.abs(x - 5) <= 1 || Math.abs(x - 18) <= 1) && y >= 5 && y <= 8) c = y == 5 ? dark : orange;
				// The roar going out: three arcs to the right.
				double ax = x - 14, ay = y - 20;
				double ar = Math.sqrt(ax * ax + ay * ay);
				if (ax > 4 && Math.abs(ay) < ax * 0.9) {
					for (double ring : new double[] {9, 13, 17}) {
						if (Math.abs(ar - ring) < 0.8) c = BeastArt.mix(BeastArt.rgb(0xFFFFFF), 0x00FFFFFF, (ring - 9) / 14.0);
					}
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	static BufferedImage illusion() {
		BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		int pale = BeastArt.rgb(0xE8E0FF), violet = BeastArt.rgb(0x9A6AE8), dark = BeastArt.rgb(0x2A1E3A);
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 32; x++) {
				int c = 0;
				double dx = x - 15.5, dy = y - 17;
				// A fox's mask: a broad top narrowing to the snout, two tall ears.
				double half = dy < -2 ? 10 : 10 - (dy + 2) * 1.0;
				if (dy > -6 && dy < 11 && Math.abs(dx) < half) c = BeastArt.mix(pale, violet, Math.min(1, (dy + 6) / 20.0));
				for (int side : new int[] {-1, 1}) {
					double ex = dx - side * 6;
					if (dy <= -6 && dy > -14 && Math.abs(ex) < (dy + 14) * 0.45) c = BeastArt.mix(pale, violet, 0.3);
				}
				if (c != 0 && Math.abs(dy + 1) < 1 && Math.abs(Math.abs(dx) - 4.5) < 1.6) c = dark; // the eye holes
				if (c != 0 && dy > 9) c = dark; // the nose
				// Fading to nothing toward the right, as an illusion goes.
				if (c != 0) {
					double fade = Math.max(0, Math.min(1, (x - 14) / 18.0));
					int alpha = (int) Math.round(255 * (1 - fade * 0.8));
					c = alpha << 24 | (c & 0xFFFFFF);
				}
				if (c == 0 && BeastArt.hash(x, y, 9) < 0.03 && Math.abs(dx) < 15) c = BeastArt.rgb(0xFFFFFF); // sparkles
				img.setRGB(x, y, c);
			}
		}
		return img;
	}
}
