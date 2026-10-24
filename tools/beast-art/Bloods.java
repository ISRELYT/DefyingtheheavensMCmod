import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A demon race's blood in its phial (DemonBloodItem, 2026-10-24), one look for each realm's worth (Qi Refining, Foundation Building, Core
 * Formation, Nascent Soul, Heavenly Being and above): textures/item/demon_blood_<look>.png, the phial (glass, stopper, its fittings), and
 * _blood.png, the blood in it, pale, which the game tints its race's hue (its second layer); the item models choosing among them by the
 * "defying-the-heavens:realm" property. The higher the realm, the finer the phial: a corked vial; a cord tied at its neck; a gold cap and
 * band; a faceted crystal one, its blood glowing out of it in rays; a jade-and-gold one in a ring of its light. On its own (one file):
 * {@code java Bloods.java <assets>}.
 */
public class Bloods {
	static final int LOOKS = 5;

	static int argb(int a, int rgb) {
		return a << 24 | rgb & 0xFFFFFF;
	}

	static int mix(int a, int b, double t) {
		t = Math.max(0, Math.min(1, t));
		int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	/** Whether pixel x, y is the phial's own: its bulb, neck, lip or stopper (rx, ry its bulb's half-width and half-height about cx, cy). */
	static boolean inPhial(int x, int y, double cx, double cy, double rx, double ry) {
		if (x < 0 || y < 0 || x > 15 || y > 15) return false;
		double dx = (x - cx) / rx, dy = (y - cy) / ry;
		return dx * dx + dy * dy <= 1.0 || x >= 6 && x <= 9 && y >= 1 && y <= 6 || x >= 5 && x <= 10 && y == 4;
	}

	static BufferedImage[] phial(int look) {
		BufferedImage glass = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), blood = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		// The bulb (rounder and wider with the realm), its neck, the lip and the stopper; a dark outline round it all.
		double cx = 7.5, cy = 10.2, rx = 4.2 + look * 0.22, ry = 4.4;
		double level = 7.8 - look * 0.4; // the blood's surface: fuller the higher
		int outline = 0x161A22, rimGlass = 0x9DBED2, pale = 0xCFE6F2, shine = 0xFFFFFF;
		int cork = 0x9A6638, corkDark = 0x5A3418, gold = 0xF0C040, goldDark = 0xA07018, cord = 0xC8202A, jade = 0x3FB078, jadeDark = 0x1E6A46;
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				boolean in = inPhial(x, y, cx, cy, rx, ry);
				boolean edge = in && (!inPhial(x - 1, y, cx, cy, rx, ry) || !inPhial(x + 1, y, cx, cy, rx, ry) || !inPhial(x, y - 1, cx, cy, rx, ry)
						|| !inPhial(x, y + 1, cx, cy, rx, ry));
				if (!in) {
					boolean beside = inPhial(x - 1, y, cx, cy, rx, ry) || inPhial(x + 1, y, cx, cy, rx, ry) || inPhial(x, y - 1, cx, cy, rx, ry) || inPhial(x, y + 1, cx, cy, rx, ry);
					if (beside) {
						glass.setRGB(x, y, 0xFF000000 | outline);
						continue;
					}
				}
				boolean stopper = x >= 6 && x <= 9 && y >= 1 && y <= 3;
				boolean neck = x >= 6 && x <= 9 && y >= 4 && y <= 6;
				if (in && stopper) {
					// Cork at the first two, a gold cap from Core Formation, jade capped with gold at the last.
					int c = look >= 4 ? (y == 1 ? gold : x == 6 ? jadeDark : jade) : look >= 2 ? (y == 1 || x == 6 ? goldDark : gold) : (x == 6 || y == 3 ? corkDark : cork);
					if (look >= 2 && x == 8 && y == 2) c = shine;
					glass.setRGB(x, y, 0xFF000000 | c);
					continue;
				}
				if (in && (neck || y == 4)) {
					int c = edge ? rimGlass : pale;
					int a = edge ? 255 : 150;
					if (look == 1 && y == 5) { c = cord; a = 255; } // a cord tied at its neck
					if (look >= 2 && y == 6) { c = x == 7 || x == 8 ? gold : goldDark; a = 255; } // a band of gold
					if (look >= 4 && y == 6 && x == 7) c = jade;
					glass.setRGB(x, y, argb(a, c));
					continue;
				}
				if (in) {
					double angle = Math.atan2(y - cy, x - cx);
					if (edge) {
						// Its rim of glass, the light catching it up to the left; a crystal's facets by turns; set in gold below at the last.
						int c = x < cx && y < cy ? pale : rimGlass;
						if (look >= 3 && ((int) Math.floor((angle + Math.PI) / (2 * Math.PI) * (6 + look * 2))) % 2 == 0) c = mix(c, shine, 0.5);
						if (look >= 4 && y > cy + ry * 0.5) c = (x + y) % 2 == 0 ? gold : goldDark;
						glass.setRGB(x, y, 0xFF000000 | c);
						continue;
					}
					if (y < level) glass.setRGB(x, y, argb(130, pale)); // empty glass over the blood
					// The glass's shine, up to the left.
					if (Math.abs(x - (cx - rx * 0.5)) < 0.6 && y > cy - ry * 0.6 && y < cy) glass.setRGB(x, y, argb(230, shine));
					// The blood: lighter toward its surface and the light, a bright line where it meets the glass above it.
					if (y >= level) {
						double lit = 0.6 + 0.3 * (1 - (y - level) / (cy + ry - level)) + 0.15 * (-(x - cx) / rx);
						int b = mix(0x585858, 0xFFFFFF, lit);
						if (y == (int) Math.ceil(level)) b = 0xFFFFFFFF;
						blood.setRGB(x, y, b);
					}
				} else if (look >= 3) {
					// Beyond the glass: its blood's light in rays (Nascent Soul), and a ring of it about the whole (Heavenly Being and above).
					double ddx = x - cx, ddy = y - cy, dist = Math.hypot(ddx / rx, ddy / ry), angle = Math.atan2(ddy, ddx);
					double glow = 0;
					if (Math.abs(Math.sin(angle * 3 + 0.4)) < 0.25 && dist < 1.6 && y > 5) glow = 0.95 * (1 - (dist - 1) / 0.6);
					if (look >= 4 && Math.abs(dist - 1.32) < 0.14 && y > 5) glow = Math.max(glow, 0.75);
					if (glow > 0) blood.setRGB(x, y, argb((int) Math.round(255 * Math.min(1, glow)), 0xFFFFFF));
				}
			}
		}
		return new BufferedImage[] {glass, blood};
	}

	static String model(int look) {
		StringBuilder s = new StringBuilder("{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\"layer0\": \"defying-the-heavens:item/demon_blood_")
				.append(look).append("\", \"layer1\": \"defying-the-heavens:item/demon_blood_").append(look).append("_blood\"}");
		if (look == 0) {
			s.append(",\n  \"overrides\": [\n");
			for (int l = 1; l < LOOKS; l++) {
				double at = l / (double) (LOOKS - 1);
				s.append("    {\"predicate\": {\"defying-the-heavens:realm\": ").append(at == Math.floor(at) ? String.valueOf((int) at) : String.valueOf(at))
						.append("}, \"model\": \"defying-the-heavens:item/demon_blood_").append(l).append("\"}").append(l + 1 < LOOKS ? ",\n" : "\n");
			}
			s.append("  ]");
		}
		return s.append("\n}\n").toString();
	}

	public static void main(String[] args) throws Exception {
		Path assets = Path.of(args[0]);
		for (int look = 0; look < LOOKS; look++) {
			BufferedImage[] phial = phial(look);
			Path tex = assets.resolve("textures/item");
			Files.createDirectories(tex);
			ImageIO.write(phial[0], "png", tex.resolve("demon_blood_" + look + ".png").toFile());
			ImageIO.write(phial[1], "png", tex.resolve("demon_blood_" + look + "_blood.png").toFile());
			Path models = assets.resolve("models/item");
			Files.writeString(models.resolve((look == 0 ? "demon_blood" : "demon_blood_" + look) + ".json"), model(look), StandardCharsets.UTF_8);
		}
		System.out.println("wrote " + LOOKS + " phials");
	}
}
