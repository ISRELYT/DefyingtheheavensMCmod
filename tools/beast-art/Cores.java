import java.awt.image.BufferedImage;
import java.nio.file.Path;

/**
 * The demon core's looks for BeastArt (DemonCoreItem, 2026-10-23), one for each realm's worth (Qi Refining, Foundation Building, Core
 * Formation, Nascent Soul, Heavenly Being and above): textures/item/demon_core_<look>.png, its dark crystal shell, and _glow.png, the
 * light within it, white, which the game tints the core's element (its second layer); the item models choosing among them by the
 * "defying-the-heavens:realm" property; and the wild beasts' spawn eggs' models. The higher the realm, the larger the core, the more
 * its facets and the brighter its light: veins of it running out from the heart, then rays, then a ring of it about the whole.
 */
public class Cores {
	static final int LOOKS = 5;

	static BufferedImage[] core(int look) {
		BufferedImage shell = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), light = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		double r = 3.6 + look * 0.75, cx = 7.5, cy = 8.0;
		double heart = r * (0.5 + look * 0.05);
		int facets = 5 + look * 2;
		int dark = Bodies.c(0x140E18), mid = Bodies.c(0x4A3652), edge = Bodies.c(0x0A060C), shine = Bodies.c(0xD8C8E0);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double dx = x - cx, dy = (y - cy) * 1.08, d = Math.sqrt(dx * dx + dy * dy);
				double angle = Math.atan2(dy, dx);
				if (d <= r) {
					// The shell: lit from the upper left, in facets that catch the light by turns, darker at the rim.
					double lit = (-dx - dy) / (r * 1.6) * 0.5 + 0.5;
					int facet = (int) Math.floor((angle + Math.PI) / (2 * Math.PI) * facets);
					if (look > 0) lit += (facet % 2 == 0 ? 0.12 : -0.08);
					int c = BeastArt.mix(dark, mid, lit);
					if (d > r - 0.9) c = BeastArt.mix(c, edge, 0.6);
					if (Math.hypot(dx + r * 0.42, dy + r * 0.42) < 0.9 + look * 0.15) c = shine;
					shell.setRGB(x, y, c);
					// The light within: a faint glow through the whole (the element's hue in it), its heart, fading out; veins of it running
					// toward the rim.
					double glow = 0.3;
					if (d < heart) glow = 1 - d / heart * 0.5;
					if (look >= 1 && d >= heart && d < r - 0.5) {
						double toVein = Math.abs(Math.sin((angle + look * 0.4) * (look + 2) / 2.0));
						if (toVein < 0.3) glow = Math.max(glow, 0.85 * (1 - (d - heart) / (r - heart)) + 0.2);
					}
					if (glow > 0) light.setRGB(x, y, alpha(glow));
				} else if (look >= 3) {
					// Beyond the shell: rays (Nascent Soul), and a ring about it all (Heavenly Being and above).
					double ray = Math.abs(Math.sin(angle * 4 + 0.6));
					double glow = 0;
					if (ray < 0.18 && d < r + 2.2) glow = 0.55 * (1 - (d - r) / 2.2);
					if (look >= 4 && Math.abs(d - (r + 1.3)) < 0.5) glow = Math.max(glow, 0.5);
					if (glow > 0) light.setRGB(x, y, alpha(glow));
				}
			}
		}
		return new BufferedImage[] {shell, light};
	}

	/** White light, as strong as {@code glow} (0 to 1). */
	static int alpha(double glow) {
		int a = (int) Math.round(255 * Math.max(0, Math.min(1, glow)));
		return a << 24 | 0xFFFFFF;
	}

	static String model(int look) {
		return "{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": {\"layer0\": \"defying-the-heavens:item/demon_core_" + look
				+ "\", \"layer1\": \"defying-the-heavens:item/demon_core_" + look + "_glow\"}" + (look == 0 ? overrides() : "") + "\n}\n";
	}

	static String overrides() {
		StringBuilder s = new StringBuilder(",\n  \"overrides\": [\n");
		for (int look = 1; look < LOOKS; look++) {
			s.append("    {\"predicate\": {\"defying-the-heavens:realm\": ").append(BeastArt.num(look / (double) (LOOKS - 1)))
					.append("}, \"model\": \"defying-the-heavens:item/demon_core_").append(look).append("\"}").append(look + 1 < LOOKS ? ",\n" : "\n");
		}
		return s.append("  ]").toString();
	}

	static void writeAll(Path assets) throws Exception {
		for (int look = 0; look < LOOKS; look++) {
			BufferedImage[] core = core(look);
			BeastArt.write(assets.resolve("textures/item/demon_core_" + look + ".png"), core[0]);
			BeastArt.write(assets.resolve("textures/item/demon_core_" + look + "_glow.png"), core[1]);
			BeastArt.write(assets.resolve("models/item/" + (look == 0 ? "demon_core" : "demon_core_" + look) + ".json"), model(look));
		}
		for (Object[] kind : Bodies.KINDS) {
			BeastArt.write(assets.resolve("models/item/" + kind[0] + "_spawn_egg.json"), "{\n  \"parent\": \"minecraft:item/template_spawn_egg\"\n}\n");
		}
	}
}
