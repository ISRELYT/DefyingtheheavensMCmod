import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The sect mission board's art (2026-10-28): the board's faces (textures/block/mission_board_*.png: red-lacquered posts, a roof of glazed
 * tiles, and the board itself, its frame lacquered and its missions pinned up on paper slips), its item's picture (textures/item/
 * mission_board.png), the Mission Slip (a paper slip, inked and red-sealed) and the Sealed Letter (folded, a wax seal on it). The board's
 * model is models/block/mission_board.json: two posts, the board between them on a crossbar, the roof over it all.
 * {@code java BeastArt.java <assets> missions} writes these alone.
 */
public class MissionArt {
	static final int LACQUER = BeastArt.rgb(0x7A2418), LACQUER_DARK = BeastArt.rgb(0x4E140C), LACQUER_LIGHT = BeastArt.rgb(0xA03C28);
	static final int PANEL = BeastArt.rgb(0xC9A46A), PANEL_DARK = BeastArt.rgb(0x9E7A44);
	static final int PAPER = BeastArt.rgb(0xF1E6C8), PAPER_SHADE = BeastArt.rgb(0xD6C49A), PAPER_EDGE = BeastArt.rgb(0x8A7650);
	static final int INK = BeastArt.rgb(0x2A2420), SEAL = BeastArt.rgb(0xC8281E), SEAL_LIGHT = BeastArt.rgb(0xF0584A), GOLD = BeastArt.rgb(0xE0B44A);
	static final int TILE = BeastArt.rgb(0x2E4A44), TILE_LIGHT = BeastArt.rgb(0x5A8476), TILE_DARK = BeastArt.rgb(0x16261F);

	/** The slips pinned to the board: x, y, width, height (in the face's 16 x 16, inside its frame of x 2-13). */
	static final int[][] SLIPS = {{3, 2, 3, 6}, {7, 1, 3, 5}, {10, 2, 3, 6}, {4, 9, 3, 5}, {8, 8, 4, 6}};

	static void writeAll(Path assets) throws IOException {
		Path blocks = assets.resolve("textures/block"), items = assets.resolve("textures/item");
		BeastArt.write(blocks.resolve("mission_board_wood.png"), wood());
		BeastArt.write(blocks.resolve("mission_board_roof.png"), roof());
		BeastArt.write(blocks.resolve("mission_board_face.png"), face());
		BeastArt.write(items.resolve("mission_board.png"), icon());
		BeastArt.write(items.resolve("mission_slip.png"), slip());
		BeastArt.write(items.resolve("sealed_letter.png"), letter());
	}

	private static BufferedImage image() {
		return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
	}

	/** The posts and crossbar: red lacquer, the grain running up it (it is stretched up the posts, so the grain hides that). */
	static BufferedImage wood() {
		BufferedImage img = image();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.shade(LACQUER, (BeastArt.hash(x, y / 5, 11) - 0.5) * 0.18);
				if (x % 4 == 0) c = BeastArt.shade(c, -0.2);
				else if (x % 4 == 2 && BeastArt.hash(x, y, 3) > 0.65) c = BeastArt.mix(c, LACQUER_LIGHT, 0.6);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The roof: rows of glazed tiles, each row's lip dark, the tiles staggered row by row, a gleam on each. */
	static BufferedImage roof() {
		BufferedImage img = image();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int row = y / 4, across = (x + (row % 2) * 2) % 4;
				int c = TILE;
				if (y % 4 == 3) c = TILE_DARK;
				else if (across == 0) c = BeastArt.shade(TILE, -0.3);
				else if (y % 4 == 0 && across == 1) c = TILE_LIGHT;
				img.setRGB(x, y, BeastArt.shade(c, (BeastArt.hash(x, y, 5) - 0.5) * 0.08));
			}
		}
		return img;
	}

	/** The board's face: a pale wooden board in a dark lacquered frame (x 2-13, as the model takes it), the missions pinned up on it. */
	static BufferedImage face() {
		BufferedImage img = image();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c;
				if (x < 2 || x > 13) c = LACQUER;
				else if (x == 2 || x == 13 || y == 0 || y == 15) c = LACQUER_DARK;
				else {
					c = BeastArt.shade(PANEL, (BeastArt.hash(x, y / 3, 9) - 0.5) * 0.14);
					if (y % 3 == 2 && BeastArt.hash(x, y, 4) > 0.55) c = PANEL_DARK;
				}
				img.setRGB(x, y, c);
			}
		}
		for (int[] slip : SLIPS) paperSlip(img, slip[0], slip[1], slip[2], slip[3], slip[0] * 31 + slip[1]);
		return img;
	}

	/** One slip pinned up: paper, shaded at its right and foot, a few lines of ink, a red seal at its foot, a gold pin at its head. */
	private static void paperSlip(BufferedImage img, int x0, int y0, int w, int h, int seed) {
		for (int x = x0; x < x0 + w; x++) {
			for (int y = y0; y < y0 + h; y++) img.setRGB(x, y, x == x0 + w - 1 || y == y0 + h - 1 ? PAPER_SHADE : PAPER);
		}
		for (int y = y0 + 1; y < y0 + h - 2; y += 2) {
			for (int x = x0; x < x0 + w - 1; x++) if (BeastArt.hash(x, y, seed) > 0.3) img.setRGB(x, y, INK);
		}
		img.setRGB(x0 + w / 2, y0 + h - 2, SEAL);
		img.setRGB(x0 + w / 2, y0, GOLD);
	}

	/** The board's item: the whole of it seen from the front, roof, posts, crossbar and board, its slips on it. */
	static BufferedImage icon() {
		BufferedImage img = image();
		for (int x = 0; x < 16; x++) {
			img.setRGB(x, 1, x == 0 || x == 15 ? TILE_DARK : TILE_LIGHT);
			img.setRGB(x, 2, BeastArt.shade(TILE, (x % 3 == 0) ? -0.3 : 0));
			img.setRGB(x, 3, TILE_DARK);
		}
		for (int y = 4; y < 16; y++) {
			for (int x : new int[] {1, 2, 13, 14}) img.setRGB(x, y, x == 1 || x == 13 ? LACQUER : LACQUER_DARK);
		}
		for (int x = 3; x <= 12; x++) img.setRGB(x, 13, LACQUER);
		for (int x = 3; x <= 12; x++) {
			for (int y = 5; y <= 12; y++) img.setRGB(x, y, x == 3 || x == 12 || y == 5 || y == 12 ? LACQUER_DARK : PANEL);
		}
		paperSlip(img, 4, 6, 2, 4, 1);
		paperSlip(img, 7, 6, 2, 5, 2);
		paperSlip(img, 10, 7, 1, 4, 3);
		return img;
	}

	/** A Mission Slip: a narrow slip of paper, edged, three lines of ink down it and the sect's red seal at its foot. */
	static BufferedImage slip() {
		BufferedImage img = image();
		for (int x = 5; x <= 10; x++) {
			for (int y = 1; y <= 14; y++) {
				boolean edge = x == 5 || x == 10 || y == 1 || y == 14;
				img.setRGB(x, y, edge ? PAPER_EDGE : x == 9 || y == 13 ? PAPER_SHADE : PAPER);
			}
		}
		for (int y = 3; y <= 9; y += 2) {
			for (int x = 6; x <= 8; x++) if (BeastArt.hash(x, y, 21) > 0.25) img.setRGB(x, y, INK);
		}
		for (int x = 7; x <= 8; x++) {
			for (int y = 11; y <= 12; y++) img.setRGB(x, y, x == 7 && y == 11 ? SEAL_LIGHT : SEAL);
		}
		img.setRGB(7, 1, GOLD);
		img.setRGB(8, 1, GOLD);
		return img;
	}

	/** A Sealed Letter: paper folded into an envelope, its flap's folds showing, a red wax seal where they meet. */
	static BufferedImage letter() {
		BufferedImage img = image();
		for (int x = 1; x <= 14; x++) {
			for (int y = 4; y <= 12; y++) {
				boolean edge = x == 1 || x == 14 || y == 4 || y == 12;
				img.setRGB(x, y, edge ? PAPER_EDGE : y >= 10 ? PAPER_SHADE : PAPER);
			}
		}
		// The flap's folds, from the top corners down to the middle.
		for (int i = 0; i <= 4; i++) {
			img.setRGB(2 + i, 5 + i, PAPER_EDGE);
			img.setRGB(13 - i, 5 + i, PAPER_EDGE);
		}
		for (int x = 6; x <= 9; x++) {
			for (int y = 8; y <= 10; y++) {
				boolean corner = (x == 6 || x == 9) && (y == 8 || y == 10);
				if (!corner) img.setRGB(x, y, x == 7 && y == 9 ? SEAL_LIGHT : SEAL);
			}
		}
		return img;
	}
}
