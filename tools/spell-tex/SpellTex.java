import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The spells' hand-drawn textures that are painted rather than modelled (textures/spell): each made from its own seed, the same every
 * run. Run from the repository's root: {@code java tools/spell-tex/SpellTex.java src/main/resources/assets/defying-the-heavens/textures/spell}.
 * <ul>
 * <li>{@code demon_moon}: the demon's moon of the Heaven-Demon Eclipse, a black disc veined with glowing blood, its rim crimson; clear
 * round it.</li>
 * <li>{@code corona}: the eclipse's corona, the sun's white-gold light streaming out past the moon's rim in long rays; clear within.</li>
 * </ul>
 */
public final class SpellTex {
	public static void main(String[] args) throws IOException {
		File out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/defying-the-heavens/textures/spell");
		write(new File(out, "demon_moon.png"), demonMoon(128, 2611));
		write(new File(out, "corona.png"), corona(256, 2617));
	}

	static void write(File file, BufferedImage img) throws IOException {
		ImageIO.write(img, "png", file);
		System.out.println("wrote " + file);
	}

	// --- The pictures ---

	/** The demon's moon, {@code size} across: black with a little of the blood in it, veins of glowing blood across it, a crimson rim. */
	static BufferedImage demonMoon(int size, int seed) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double c = (size - 1) / 2.0, radius = size * 0.47;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = x - c, dy = y - c, r = Math.hypot(dx, dy) / radius;
				double edge = clamp01((1 - r) * radius * 0.7); // a pixel or so of softened edge
				if (edge <= 0) continue;
				double u = dx / radius, v = dy / radius;
				double shade = 0.55 + 0.45 * fbm(u * 3 + 11, v * 3 + 7, seed, 4);
				double[] col = mix(new double[] {0.02, 0.005, 0.01}, new double[] {0.16, 0.02, 0.04}, shade * 0.45);
				// Veins of blood: the ridges of a noise, glowing where they run sharpest.
				double ridge = Math.pow(1 - Math.abs(2 * fbm(u * 1.8 + 3, v * 1.8 - 5, seed + 1, 4) - 1), 13);
				double fine = Math.pow(1 - Math.abs(2 * fbm(u * 4 - 2, v * 4 + 9, seed + 2, 3) - 1), 20) * 0.45;
				double vein = clamp01(Math.max(ridge, fine) * (0.4 + 0.6 * r));
				col = mix(col, new double[] {0.85, 0.06, 0.05}, vein * 0.7);
				// The rim: crimson brightening to ember at its very edge.
				if (r > 0.86) col = mix(col, mix(new double[] {0.6, 0.02, 0.04}, new double[] {1.0, 0.35, 0.18}, (r - 0.86) / 0.14), clamp01((r - 0.86) / 0.1));
				put(img, x, y, col, edge);
			}
		}
		return img;
	}

	/**
	 * The corona, {@code size} across: clear within the moon's rim (its inner edge at 0.42 of the way out), a thin ring of white light hugging
	 * the rim, and rays of white-gold streaming out from it, long and short by turns, fading as they go.
	 */
	static BufferedImage corona(int size, int seed) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double c = (size - 1) / 2.0, outer = size * 0.5;
		double inner = 0.42;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = x - c, dy = y - c, r = Math.hypot(dx, dy) / outer, a = Math.atan2(dy, dx);
				if (r < inner - 0.02 || r >= 1) continue;
				double out = (r - inner) / (1 - inner);
				// Rays: long and short by turns round it, each wavering.
				double rays = 0.25 + 0.75 * Math.pow(Math.abs(Math.cos(a * 6 + 0.6 * Math.sin(a * 3))), 3) * (0.6 + 0.4 * noise(Math.cos(a) * 4 + 20, Math.sin(a) * 4, seed));
				double streak = 0.6 + 0.4 * fbm(Math.cos(a) * 18, Math.sin(a) * 18 + r * 2, seed + 3, 3);
				double reach = 0.18 + 0.82 * rays;
				double light = clamp01(1 - out / reach);
				light = light * light * streak;
				// The ring of light hugging the rim.
				double ring = Math.exp(-Math.pow((r - inner) / 0.025, 2)) * 1.2;
				double glow = clamp01(light + ring);
				if (glow <= 0.002) continue;
				double[] col = mix(new double[] {1.0, 0.97, 0.88}, new double[] {1.0, 0.72, 0.28}, clamp01(out * 1.3));
				double soft = clamp01((r - (inner - 0.02)) / 0.02);
				put(img, x, y, col, glow * soft);
			}
		}
		return img;
	}

	// --- Paint ---

	static void put(BufferedImage img, int x, int y, double[] rgb, double alpha) {
		int a = (int) Math.round(clamp01(alpha) * 255), r = (int) Math.round(clamp01(rgb[0]) * 255), g = (int) Math.round(clamp01(rgb[1]) * 255),
				b = (int) Math.round(clamp01(rgb[2]) * 255);
		img.setRGB(x, y, a << 24 | r << 16 | g << 8 | b);
	}

	static double[] mix(double[] a, double[] b, double t) {
		t = clamp01(t);
		return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
	}

	static double clamp01(double v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	// --- Noise ---

	static double hash(int x, int y, int seed) {
		long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
		h = (h ^ (h >>> 13)) * 1274126177L;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / (double) 0xFFFFFF;
	}

	/** Smooth value noise, 0 to 1. */
	static double noise(double x, double y, int seed) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double fx = x - x0, fy = y - y0;
		double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
		double a = hash(x0, y0, seed), b = hash(x0 + 1, y0, seed), c = hash(x0, y0 + 1, seed), d = hash(x0 + 1, y0 + 1, seed);
		return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy;
	}

	/** {@code octaves} of {@link #noise}, each twice as fine and half as strong, 0 to 1. */
	static double fbm(double x, double y, int seed, int octaves) {
		double sum = 0, weight = 0, w = 1;
		for (int i = 0; i < octaves; i++) {
			sum += noise(x, y, seed + i * 31) * w;
			weight += w;
			x *= 2.03;
			y *= 2.03;
			w *= 0.5;
		}
		return sum / weight;
	}

	private SpellTex() {}
}
