import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A sect's guest pass (SectPassItem, 2026-10-24): textures/item/sect_pass.png, a tablet of pale jade with a rounded top, a frame cut
 * round its face and the sect's seal carved in it (a square within a ring), hung from a red cord with a tassel; and its item model.
 * On its own (one file): {@code java Tokens.java <assets>}.
 */
public class Tokens {
	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	/** Whether pixel x, y is the tablet's: 9 wide, 11 tall from y 4, its top corners rounded. */
	static boolean tablet(int x, int y) {
		if (x < 4 || x > 12 || y < 4 || y > 14) return false;
		return !((y == 4 || y == 14) && (x == 4 || x == 12));
	}

	public static void main(String[] args) throws Exception {
		Path assets = Path.of(args[0]);
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int outline = 0x123A2C, jade = 0x7FD6B0, jadeLight = 0xC8F2DE, jadeDark = 0x3E9C78, carve = 0x2A7458, cord = 0xC0202A, cordDark = 0x7A1018, gold = 0xE8B840;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				boolean in = tablet(x, y);
				if (!in) {
					if (tablet(x - 1, y) || tablet(x + 1, y) || tablet(x, y - 1) || tablet(x, y + 1)) img.setRGB(x, y, 0xFF000000 | outline);
					continue;
				}
				// Jade, lit from the upper left, clouded a little.
				double lit = 0.55 + 0.25 * (8 - x) / 8.0 + 0.2 * (9 - y) / 9.0 + 0.08 * Math.sin(x * 1.7 + y * 2.3);
				int c = mix(jadeDark, jadeLight, lit);
				// A frame cut round its face, and the sect's seal: a square within a ring.
				boolean frame = (x == 5 || x == 11) && y >= 5 && y <= 13 || (y == 5 || y == 13) && x >= 5 && x <= 11;
				double dx = x - 8, dy = y - 9.5, ring = Math.sqrt(dx * dx + dy * dy);
				boolean seal = Math.abs(ring - 2.6) < 0.5 || Math.max(Math.abs(dx), Math.abs(dy)) == 1;
				if (frame) c = mix(c, carve, 0.55);
				if (seal) c = carve;
				if (x == 6 && y >= 6 && y <= 8) c = 0xFFFFFFFF & 0xFFF2FFF8; // its shine
				img.setRGB(x, y, 0xFF000000 | (c & 0xFFFFFF));
			}
		}
		// The cord up to a loop over the tablet, a gold bead, and the tassel hanging at its side.
		int[][] cordPx = {{8, 3}, {8, 2}, {7, 1}, {9, 1}, {7, 0}, {9, 0}, {8, 0}};
		for (int[] p : cordPx) img.setRGB(p[0], p[1], 0xFF000000 | cord);
		img.setRGB(8, 3, 0xFF000000 | gold);
		for (int y = 9; y <= 15; y++) img.setRGB(14, y, 0xFF000000 | (y % 2 == 0 ? cord : cordDark));
		img.setRGB(13, 9, 0xFF000000 | cord);
		img.setRGB(15, 13, 0xFF000000 | cordDark);
		img.setRGB(13, 14, 0xFF000000 | cordDark);
		Path tex = assets.resolve("textures/item");
		Files.createDirectories(tex);
		ImageIO.write(img, "png", tex.resolve("sect_pass.png").toFile());
		Files.writeString(assets.resolve("models/item/sect_pass.json"),
				"{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\"layer0\": \"defying-the-heavens:item/sect_pass\"}\n}\n", StandardCharsets.UTF_8);
		System.out.println("wrote the sect pass");
	}
}
