import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Puppetry's art (2026-11-17): the frames' sprites, a humanoid's and a bee's at every grade (textures/item/<grade>_humanoid_frame.png,
 * <grade>_bee_frame.png, 16 x 16), in the grade's matter as the puppets are (the user's pick: Mortal wood, Spirit bronze, Earth dark iron
 * with jade inlay, Heaven silver-white, Immortal gold; tools/spell-art Puppets has the same colours); the Command Seal
 * (textures/item/command_seal.png: a seal of green jade with a coiled knob and a red cord); and the Puppet Workbench's faces
 * (textures/block/puppet_workbench_top, _side, _bottom: dark wood, a lacquered board inlaid in its top in a grid of jade lines).
 * {@code java BeastArt.java <assets> puppets [sheet.png]}.
 */
public class PuppetArt {
	static final String[] GRADES = {"mortal", "spirit", "earth", "heaven", "immortal"};

	/** A grade's matter as the sprites take it: body light, mid, dark; joints light, dark; plates; outline; inlay (0 none); wings. */
	record Look(int light, int mid, int dark, int joint, int jointDark, int plate, int outline, int inlay, int wing) {}

	static final Look[] LOOKS = {
			new Look(rgb(0xD8A870), rgb(0xB8875A), rgb(0x7E5634), rgb(0x6E4426), rgb(0x3A2414), rgb(0x8A5530), rgb(0x2A180C), 0, rgb(0xE8D8A8)),
			new Look(rgb(0xF0C878), rgb(0xB98A44), rgb(0x7A5424), rgb(0x6A4A22), rgb(0x3A2810), rgb(0xE0B060), rgb(0x2A1A08), rgb(0x5FB88A), rgb(0xD8E6B0)),
			new Look(rgb(0x7A8088), rgb(0x464A52), rgb(0x26292E), rgb(0x34373C), rgb(0x141618), rgb(0x8EE6BC), rgb(0x0A0B0D), rgb(0x5FD8A0), rgb(0xA8F0D0)),
			new Look(rgb(0xFFFFFF), rgb(0xE2E6EE), rgb(0xAEB6C2), rgb(0xF0D890), rgb(0xB89A48), rgb(0xF6F8EE), rgb(0x4A5260), 0, rgb(0xE8F2FF)),
			new Look(rgb(0xFFEEB0), rgb(0xE8BE4A), rgb(0xA87C1E), rgb(0xF0B090), rgb(0xA86448), rgb(0xFFF6DC), rgb(0x4A3208), rgb(0x9AE0BE), rgb(0xFFF0B8))};

	static int rgb(int hex) { return BeastArt.rgb(hex); }

	static final int QI = rgb(0x8FF0FF), QI_CORE = rgb(0xF0FFFF);

	static void writeAll(Path assets) throws IOException {
		Path items = assets.resolve("textures/item");
		for (int g = 0; g < GRADES.length; g++) {
			BeastArt.write(items.resolve(GRADES[g] + "_humanoid_frame.png"), humanoid(LOOKS[g]));
			BeastArt.write(items.resolve(GRADES[g] + "_bee_frame.png"), bee(LOOKS[g]));
		}
		BeastArt.write(items.resolve("command_seal.png"), seal());
		BeastArt.write(items.resolve("sub_seal.png"), subSeal());
		BeastArt.write(items.resolve("circuit_freeze_talisman.png"), freezeTalisman());
		BeastArt.write(items.resolve("circuit_diagram.png"), diagram(false));
		BeastArt.write(items.resolve("circuit_fragment.png"), diagram(true));
		Path blocks = assets.resolve("textures/block");
		BeastArt.write(blocks.resolve("puppet_workbench_top.png"), benchTop());
		BeastArt.write(blocks.resolve("puppet_workbench_side.png"), benchSide());
		BeastArt.write(blocks.resolve("puppet_workbench_bottom.png"), benchBottom());
		// Puppet phase 3 (the linked army): the Puppet Factory's faces and the Overseer Core's.
		BeastArt.write(blocks.resolve("puppet_factory_front.png"), factoryFront());
		BeastArt.write(blocks.resolve("puppet_factory_side.png"), factorySide());
		BeastArt.write(blocks.resolve("puppet_factory_top.png"), factoryTop());
		BeastArt.write(blocks.resolve("puppet_factory_bottom.png"), factoryBottom());
		BeastArt.write(blocks.resolve("overseer_core_side.png"), overseerSide());
		BeastArt.write(blocks.resolve("overseer_core_top.png"), overseerTop());
		BeastArt.write(blocks.resolve("overseer_core_bottom.png"), overseerBottom());
	}

	// --- Puppet phase 3: the Puppet Factory and the Overseer Core (2026-11-18) ---

	static final int IRON = rgb(0x3A3E45), IRON_DARK = rgb(0x24272C), IRON_LIGHT = rgb(0x575C64), RIVET = rgb(0xC8A858), RIVET_DARK = rgb(0x8A7038),
			BRASS = rgb(0xB98A44), JADE = rgb(0x49B477), JADE_DARK = rgb(0x2F8F5A), JADE_LIGHT = rgb(0xA8EBC4), DEEP = rgb(0x111316), GOLD = rgb(0xF5CF55);

	/** Dark iron plates, a seam across the middle, brass rivets at the plates' corners, a brass band top and foot. */
	static BufferedImage factorySide() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = (x * 3 + y * 5) % 11 == 0 ? IRON_LIGHT : (x + y * 2) % 7 == 0 ? IRON_DARK : IRON;
				if (y == 7 || y == 8 && x % 2 == 0) c = IRON_DARK;
				if (x == 0 || x == 15) c = IRON_DARK;
				if (y == 0 || y == 15) c = BRASS;
				if (y == 1 || y == 14) c = RIVET_DARK;
				img.setRGB(x, y, c);
			}
		}
		for (int[] r : new int[][] {{2, 3}, {13, 3}, {2, 11}, {13, 11}}) {
			img.setRGB(r[0], r[1], RIVET);
			img.setRGB(r[0] + 1, r[1] + 1, RIVET_DARK);
		}
		return img;
	}

	/** Its front: the side's plates round a mould let into it, a frame of jade, a humanoid's figure in bronze on a lattice of azure lines. */
	static BufferedImage factoryFront() {
		BufferedImage img = factorySide();
		for (int y = 3; y <= 13; y++) for (int x = 3; x <= 12; x++) img.setRGB(x, y, x == 3 || x == 12 || y == 3 || y == 13 ? JADE_DARK : DEEP);
		for (int x = 4; x <= 11; x++) img.setRGB(x, 4, JADE);
		// The lattice the circuits are drawn by, behind the figure.
		for (int y = 5; y <= 12; y++) for (int x = 4; x <= 11; x++) if ((x - 4) % 3 == 0 || (y - 5) % 3 == 0) img.setRGB(x, y, rgb(0x1F4A5A));
		// The figure: head, its lit eyes, a body with its core, arms, legs.
		int b = BRASS, d = rgb(0x7A5424);
		img.setRGB(7, 5, b);
		img.setRGB(8, 5, b);
		img.setRGB(7, 6, QI);
		img.setRGB(8, 6, QI);
		for (int y = 7; y <= 9; y++) for (int x = 6; x <= 9; x++) img.setRGB(x, y, b);
		img.setRGB(7, 8, QI_CORE);
		img.setRGB(8, 8, QI);
		img.setRGB(5, 7, d);
		img.setRGB(5, 8, d);
		img.setRGB(10, 7, d);
		img.setRGB(10, 8, d);
		for (int y = 10; y <= 12; y++) {
			img.setRGB(6, y, d);
			img.setRGB(9, y, d);
		}
		return img;
	}

	/** Its top: an iron plate, a gold-rimmed round mould in it with an azure ring of qi, rivets at its corners. */
	static BufferedImage factoryTop() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				int c = (x + y) % 5 == 0 ? IRON_DARK : IRON;
				if (x == 0 || y == 0 || x == 15 || y == 15) c = BRASS;
				if (d < 6.2) c = d > 5.2 ? GOLD : d > 4.2 ? RIVET_DARK : d > 2.9 && d < 3.9 ? QI : d <= 1.4 ? QI_CORE : DEEP;
				img.setRGB(x, y, c);
			}
		}
		for (int[] r : new int[][] {{1, 1}, {14, 1}, {1, 14}, {14, 14}}) img.setRGB(r[0], r[1], RIVET);
		return img;
	}

	static BufferedImage factoryBottom() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, (x * 5 + y * 3) % 7 == 0 ? IRON_DARK : IRON);
		return img;
	}

	/** The Overseer Core's sides: green jade stone, a gold band at its foot and its head, an azure eye watching from its middle. */
	static BufferedImage overseerSide() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c = (x * 7 + y * 3) % 9 == 0 ? JADE_LIGHT : (x + y * 3) % 5 == 0 ? JADE_DARK : JADE;
				if (y <= 1 || y >= 14) c = y == 1 || y == 14 ? RIVET_DARK : GOLD;
				img.setRGB(x, y, c);
			}
		}
		// The eye: its lids in gold, the white of it pale jade, an azure iris round a bright pupil; lines of qi running down from it.
		for (int x = 3; x <= 12; x++) {
			int half = x <= 7 ? x - 3 : 12 - x;
			int h = Math.min(3, 1 + half / 1);
			for (int y = 8 - h; y <= 7 + h; y++) img.setRGB(x, y, y == 8 - h || y == 7 + h ? GOLD : JADE_LIGHT);
		}
		for (int y = 5; y <= 10; y++) for (int x = 6; x <= 9; x++) if (Math.hypot(x - 7.5, y - 7.5) < 2.4) img.setRGB(x, y, QI);
		img.setRGB(7, 7, QI_CORE);
		img.setRGB(8, 8, DEEP);
		img.setRGB(7, 8, DEEP);
		img.setRGB(8, 7, QI_CORE);
		for (int y = 11; y <= 13; y++) {
			img.setRGB(4, y, rgb(0x5FD8FF));
			img.setRGB(11, y, rgb(0x5FD8FF));
		}
		return img;
	}

	/** Its top: a disc of pale jade framed in gold, a circuit's lines across it to a core at its heart. */
	static BufferedImage overseerTop() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				int c = (x + y) % 4 == 0 ? JADE_DARK : JADE;
				if (x == 0 || y == 0 || x == 15 || y == 15) c = GOLD;
				if (d < 6.5) c = d > 5.6 ? GOLD : JADE_LIGHT;
				if (d < 5.6 && (x == 7 || x == 8 || y == 7 || y == 8)) c = rgb(0x2A8ACF);
				if (d < 1.6) c = QI_CORE;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	static BufferedImage overseerBottom() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) img.setRGB(x, y, (x * 3 + y * 7) % 6 == 0 ? JADE_DARK : JADE);
		return img;
	}

	/** A plain sprite canvas: each pixel its colour (0 clear), outlined after in the grade's outline where clear meets painted. */
	static final class Canvas {
		final int[][] c = new int[16][16];

		void set(int x, int y, int colour) {
			if (x >= 0 && y >= 0 && x < 16 && y < 16) c[x][y] = colour;
		}

		void rect(int x0, int y0, int x1, int y1, int colour) {
			for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) set(x, y, colour);
		}

		BufferedImage done(int outline) {
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int colour = c[x][y];
					if (colour == 0 && outline != 0) {
						for (int[] d : new int[][] {{0, 1}, {-1, 0}, {1, 0}, {0, -1}}) {
							int nx = x + d[0], ny = y + d[1];
							if (nx >= 0 && ny >= 0 && nx < 16 && ny < 16 && c[nx][ny] != 0 && c[nx][ny] != 1) {
								colour = outline;
								break;
							}
						}
					}
					img.setRGB(x, y, colour == 1 ? 0 : colour);
				}
			}
			return img;
		}
	}

	/** A humanoid frame: a jointed figure standing, ball joints darker, its chest's core and its eyes' slit lit azure. */
	static BufferedImage humanoid(Look k) {
		Canvas p = new Canvas();
		// Head and eyes.
		p.rect(6, 1, 9, 4, k.mid());
		p.rect(6, 1, 9, 1, k.light());
		p.rect(6, 2, 9, 2, QI);
		p.set(7, 5, k.jointDark());
		p.set(8, 5, k.jointDark());
		// Torso, its plate and core.
		p.rect(5, 6, 10, 10, k.mid());
		p.rect(5, 6, 10, 6, k.light());
		p.rect(6, 7, 9, 9, k.plate());
		p.set(7, 8, QI_CORE);
		p.set(8, 8, QI);
		if (k.inlay() != 0) {
			p.set(5, 9, k.inlay());
			p.set(10, 9, k.inlay());
		}
		p.rect(6, 11, 9, 11, k.jointDark());
		// Arms: shoulder balls, upper arms, elbows, forearms.
		for (int s = 0; s < 2; s++) {
			int x = s == 0 ? 3 : 12;
			p.set(s == 0 ? 4 : 11, 6, k.joint());
			p.rect(x, 7, x, 8, k.mid());
			p.set(x, 9, k.joint());
			p.rect(x, 10, x, 11, k.dark());
			p.set(x, 12, k.jointDark());
		}
		// Legs: hips, thighs, knees, shins, feet.
		for (int s = 0; s < 2; s++) {
			int x = s == 0 ? 6 : 9;
			p.set(x, 12, k.joint());
			p.set(x, 13, k.mid());
			p.set(x, 14, k.joint());
			p.set(x, 15, k.dark());
			p.set(s == 0 ? x - 1 : x + 1, 15, k.dark());
		}
		return p.done(k.outline());
	}

	/** A bee frame, seen from above: head with lit eyes, thorax, wings out to either side, a banded abdomen, the stinger's light. */
	static BufferedImage bee(Look k) {
		Canvas p = new Canvas();
		// Wings first (pale, veined), so the body lies over them.
		for (int s = 0; s < 2; s++) {
			int dir = s == 0 ? -1 : 1;
			for (int y = 3; y <= 8; y++) {
				int reach = y <= 4 ? 4 + (y - 3) : y <= 6 ? 6 : 6 - (y - 6) * 2;
				for (int i = 1; i <= reach; i++) {
					int x = (s == 0 ? 6 : 9) + dir * i;
					p.set(x, y, (i + y) % 4 == 0 ? k.joint() : k.wing());
				}
			}
		}
		// Head.
		p.rect(6, 1, 9, 3, k.mid());
		p.set(6, 2, QI);
		p.set(9, 2, QI);
		p.set(6, 0, k.jointDark());
		p.set(9, 0, k.jointDark());
		// Thorax.
		p.rect(6, 4, 9, 7, k.mid());
		p.rect(7, 4, 8, 5, k.light());
		// Abdomen, banded.
		p.rect(5, 8, 10, 12, k.mid());
		p.rect(6, 13, 9, 13, k.mid());
		for (int y = 8; y <= 13; y += 2) for (int x = 5; x <= 10; x++) if (p.c[x][y] != 0) p.set(x, y, k.plate());
		if (k.inlay() != 0) p.set(7, 10, k.inlay());
		p.set(7, 14, k.jointDark());
		p.set(8, 14, k.jointDark());
		p.set(7, 15, QI);
		p.set(8, 15, QI_CORE);
		return p.done(k.outline());
	}

	/** The Command Seal: a square seal of green jade, a knob coiled on its top, a red cord hanging, its face's character glinting. */
	static BufferedImage seal() {
		Canvas p = new Canvas();
		int jade = rgb(0x49B477), jadeLight = rgb(0xA8EBC4), jadeDark = rgb(0x2F8F5A), gold = rgb(0xF5CF55), red = rgb(0xC04040), redDark = rgb(0x7A2020);
		// The block of the seal.
		p.rect(3, 8, 12, 14, jade);
		p.rect(3, 8, 12, 8, jadeLight);
		p.rect(3, 14, 12, 14, jadeDark);
		p.rect(12, 9, 12, 14, jadeDark);
		// Its face's character (a gold mark).
		p.rect(5, 10, 10, 10, gold);
		p.rect(7, 10, 8, 13, gold);
		p.rect(5, 12, 10, 12, gold);
		// The knob: a coiled beast.
		p.rect(5, 4, 10, 7, jade);
		p.rect(6, 3, 9, 3, jadeLight);
		p.rect(6, 5, 7, 6, jadeLight);
		p.set(9, 5, rgb(0x0F3D27));
		// Its cord, hanging off the knob.
		p.set(11, 5, red);
		p.set(12, 6, red);
		p.set(13, 7, redDark);
		p.set(13, 8, red);
		p.set(14, 9, redDark);
		p.set(13, 10, red);
		return p.done(rgb(0x0F2A1C));
	}

	/**
	 * A Sub-Seal (puppet phase 2c): a slip of pale paper, a little curled at its foot, the Command Seal's mark stamped on it in red, a line of
	 * azure qi from the mark down the slip (the puppet it is bound to), a green jade cord through a hole at its head.
	 */
	static BufferedImage subSeal() {
		Canvas p = new Canvas();
		int paper = rgb(0xF1E6C9), shade = rgb(0xD5C49D), red = rgb(0xC0281C), redDark = rgb(0x8A1A12), qi = rgb(0x2A8ACF), jade = rgb(0x49B477),
				jadeDark = rgb(0x2F8F5A);
		// The slip.
		p.rect(5, 2, 10, 14, paper);
		for (int y = 2; y <= 14; y++) p.set(10, y, shade);
		p.rect(5, 14, 10, 14, shade);
		p.set(5, 14, 0);
		// Its cord through a hole at the head.
		p.set(7, 3, shade);
		p.set(8, 1, jade);
		p.set(9, 0, jadeDark);
		p.set(7, 1, jadeDark);
		p.set(6, 0, jade);
		// The seal's mark in red: a square with the character in it.
		p.rect(6, 5, 9, 8, red);
		p.rect(7, 6, 8, 6, paper);
		p.set(7, 7, redDark);
		p.set(8, 7, paper);
		// A line of qi from the mark down the slip.
		for (int y = 9; y <= 12; y++) p.set(7, y, qi);
		p.set(8, 12, qi);
		p.set(8, 13, qi);
		return p.done(rgb(0x5A4A2A));
	}

	/**
	 * The Circuit-Freeze Talisman, in the other talismans' frame (talisman-art's talismans.ps1: an 8-wide strip of yellow paper, a dark border, a
	 * darker band at its foot, a tassel): its glyph a knot of azure circuit lines under a red seal, a red bar across its foot.
	 */
	static BufferedImage freezeTalisman() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int border = rgb(0x8A6420), paper = rgb(0xE9C65C), band = rgb(0xC89A36), tassel = rgb(0x7A140E), ink = rgb(0xC0281C), accent = rgb(0x2A8ACF);
		for (int x = 4; x <= 11; x++) {
			for (int y = 1; y <= 14; y++) {
				boolean edge = x == 4 || x == 11 || y == 1 || y == 14;
				img.setRGB(x, y, edge ? border : y == 13 ? band : paper);
			}
		}
		img.setRGB(7, 15, tassel);
		img.setRGB(8, 15, tassel);
		String[] glyph = {"..rr..", "......", "a.aa.a", "aaa.aa", "..a...", ".aaaa.", ".a..a.", ".aaaa.", "...a..", "......", ".rrrr."};
		for (int row = 0; row < glyph.length; row++) {
			for (int col = 0; col < 6; col++) {
				char ch = glyph[row].charAt(col);
				if (ch == 'r') img.setRGB(5 + col, 2 + row, ink);
				else if (ch == 'a') img.setRGB(5 + col, 2 + row, accent);
			}
		}
		return img;
	}

	/**
	 * A Circuit Diagram: a sheet of pale paper, its corners curled, azure lines of a circuit drawn on it round a gold core; a fragment is its top
	 * left quarter torn off, the tear ragged.
	 */
	static BufferedImage diagram(boolean torn) {
		Canvas p = new Canvas();
		int paper = rgb(0xEFE4C8), shade = rgb(0xD2C29C), line = rgb(0x2A8ACF), core = rgb(0xE0A030), node = rgb(0xC04040);
		int x1 = torn ? 9 : 13, y1 = torn ? 9 : 13;
		p.rect(2, 2, x1, y1, paper);
		for (int x = 2; x <= x1; x++) p.set(x, y1, shade);
		for (int y = 2; y <= y1; y++) p.set(x1, y, shade);
		if (!torn) {
			p.set(2, 2, 0);
			p.set(13, 13, 0);
			p.set(12, 13, shade);
		}
		// The circuit: a trunk from the core up, branches to the sides and down.
		int[][] lines = {{7, 4, 7, 11}, {4, 6, 11, 6}, {4, 6, 4, 10}, {11, 6, 11, 10}, {5, 11, 10, 11}};
		for (int[] l : lines) {
			for (int x = l[0]; x <= l[2]; x++) for (int y = l[1]; y <= l[3]; y++) if (x <= x1 - 1 && y <= y1 - 1) p.set(x, y, line);
		}
		if (7 < x1 && 8 < y1) {
			p.set(7, 8, core);
			p.set(8, 8, core);
		}
		int[][] nodes = {{7, 3}, {3, 6}, {12, 6}, {4, 11}, {11, 11}};
		for (int[] n : nodes) if (n[0] < x1 && n[1] < y1) p.set(n[0], n[1], node);
		if (torn) {
			// The ragged tear along its right and foot.
			for (int k = 2; k <= 9; k += 2) {
				p.set(x1, k, 0);
				p.set(k, y1, 0);
			}
		}
		return p.done(rgb(0x5A4A30));
	}

	/** The bench's top: a frame of dark wood round a lacquered board, a grid of fine jade lines laid in it. */
	static BufferedImage benchTop() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int c;
				boolean edge = x == 0 || y == 0 || x == 15 || y == 15;
				if (edge) c = rgb(0x3A2414);
				else if (x == 1 || y == 1 || x == 14 || y == 14) c = (x + y) % 3 == 0 ? rgb(0x5A3A22) : rgb(0x4A2E1A);
				else {
					c = rgb(0x1E1410);
					if ((x - 2) % 3 == 0 || (y - 2) % 3 == 0) c = rgb(0x2E5A48);
					if (x == 8 && y == 8 || x == 7 && y == 7 || x == 8 && y == 7 || x == 7 && y == 8) c = rgb(0x4FB884);
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** Its sides: dark wood, the grain running across, a brass band along the top's edge. */
	static BufferedImage benchSide() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double grain = Math.sin(y * 1.7 + Math.sin(x * 0.6) * 1.3);
				int c = grain > 0.4 ? rgb(0x5A3A22) : grain < -0.5 ? rgb(0x3A2414) : rgb(0x4A2E1A);
				if (y == 2) c = rgb(0xC8A858);
				if (y == 3) c = rgb(0x8A7038);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	static BufferedImage benchBottom() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) img.setRGB(x, y, (x * 7 + y * 3) % 5 == 0 ? rgb(0x2E1C10) : rgb(0x3A2414));
		}
		return img;
	}

	/** Every sprite eight times over on one sheet, to look over. */
	static void writeSheet(Path sheet) throws IOException {
		int scale = 8;
		BufferedImage out = new BufferedImage(16 * scale * 6, 16 * scale * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImage[] row0 = new BufferedImage[6], row1 = new BufferedImage[6];
		for (int g = 0; g < 5; g++) {
			row0[g] = humanoid(LOOKS[g]);
			row1[g] = bee(LOOKS[g]);
		}
		row0[5] = seal();
		row1[5] = benchTop();
		for (int r = 0; r < 2; r++) {
			BufferedImage[] row = r == 0 ? row0 : row1;
			for (int i = 0; i < 6; i++) {
				for (int y = 0; y < 16 * scale; y++) {
					for (int x = 0; x < 16 * scale; x++) {
						int c = row[i].getRGB(x / scale, y / scale);
						out.setRGB(i * 16 * scale + x, r * 16 * scale + y, (c >>> 24) == 0 ? 0xFF202830 : c);
					}
				}
			}
		}
		BeastArt.write(sheet, out);
	}
}
