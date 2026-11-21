import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * The cinematics' effect textures (textures/fx, the VFX plan's shared toolkit), every one made here from its own seed, the same every
 * run, nothing taken from anywhere. Run from the repository's root:
 * {@code java tools/fx-tex/FxTex.java src/main/resources/assets/defying-the-heavens/textures/fx}. Each gets a {@code .mcmeta} asking for
 * smooth (blurred) filtering; nothing is clamped, so the strips and the clouds repeat.
 * <p>
 * Most are masks: white, their alpha the light's strength (the {@code fx} shader colours them: a ribbon's middle runs white-hot and
 * its edges take the element's colour). A few carry their own colours (the array's red centre, the clouds' shading).
 * <ul>
 * <li>{@code glow}: a soft round glow, a bright middle and a long tail.</li>
 * <li>{@code spark}: a short soft capsule (a spark, drawn stretched along its flight).</li>
 * <li>{@code streak}: a line of light bright at its head (u = 1), its tail fading (u = 0).</li>
 * <li>{@code ribbon}: across (u) a sharp core in a soft halo; along (v, repeating) the light varies in long streaks.</li>
 * <li>{@code flow}: turbulent streaming flame, repeating both ways (a pillar's body, scrolled upward).</li>
 * <li>{@code flame_band}: a band of swirling flame, its leading edge (v = 0) a bright line, repeating along u (a ring of fire racing out).</li>
 * <li>{@code cloud}: the underside of a storm cloud, repeating both ways: grey billows, mostly covering.</li>
 * <li>{@code puff}: one billow, round, its edge broken (dust, smoke, the fire of a mushroom).</li>
 * <li>{@code ring}: a thin ring of light at nine tenths of the way out.</li>
 * <li>{@code rays}: a burst of thin rays (where light meets in a point).</li>
 * <li>{@code array_rings}, {@code array_circles}, {@code array_star}, {@code array_flame}: the runic array's layers, each turned at its own
 * speed (rings with a band of runes and ticks; small circles with a rune in each; an eight-pointed star; flame swirling between the rings).
 * The runes are made up here, strokes and hooks, no writing of any people.</li>
 * <li>{@code array_core}: the array's red heart, a rune cut dark in it.</li>
 * <li>{@code soft}: a broad, even glow with no bright middle (light cast on the ground or a cloud base).</li>
 * </ul>
 */
public final class FxTex {
	public static void main(String[] args) throws IOException {
		File out = new File(args.length > 0 ? args[0] : "src/main/resources/assets/defying-the-heavens/textures/fx");
		if (!out.isDirectory() && !out.mkdirs()) throw new IOException("can't make " + out);
		write(out, "glow", glow(128));
		write(out, "spark", spark(64, 32));
		write(out, "streak", streak(128, 32));
		write(out, "ribbon", ribbon(64, 256, 7101));
		write(out, "flow", flow(128, 256, 7103));
		write(out, "flame_band", flameBand(512, 128, 7105));
		write(out, "cloud", cloud(512, 7107));
		write(out, "puff", puff(128, 7109));
		write(out, "ring", ring(256));
		write(out, "rays", rays(256));
		write(out, "array_rings", arrayRings(1024, 7111));
		write(out, "array_circles", arrayCircles(1024, 7113));
		write(out, "array_star", arrayStar(1024));
		write(out, "array_flame", arrayFlame(1024, 7115));
		write(out, "array_core", arrayCore(256, 7117));
		write(out, "soft", soft(128));
	}

	static void write(File dir, String name, BufferedImage img) throws IOException {
		ImageIO.write(img, "png", new File(dir, name + ".png"));
		try (FileWriter w = new FileWriter(new File(dir, name + ".png.mcmeta"))) {
			w.write("{\n  \"texture\": {\n    \"blur\": true\n  }\n}\n");
		}
		System.out.println("wrote " + name + " (" + img.getWidth() + "x" + img.getHeight() + ")");
	}

	// --- Masks ---

	static BufferedImage glow(int size) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double r = Math.hypot(x - c, y - c) / (size / 2.0);
				double a = 0.6 * Math.exp(-r * r / 0.016) + 0.4 * Math.exp(-r * r / 0.13);
				mask(img, x, y, a * (1 - smooth(0.82, 1.0, r)));
			}
		}
		return img;
	}

	/** A broad, even glow with no bright middle (light cast on the ground or the cloud base: a pool of light, not a lamp). */
	static BufferedImage soft(int size) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double r = Math.hypot(x - c, y - c) / (size / 2.0);
				mask(img, x, y, Math.exp(-r * r / 0.18) * (1 - smooth(0.7, 1.0, r)));
			}
		}
		return img;
	}

	static BufferedImage spark(int w, int h) {
		BufferedImage img = image(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double u = (x + 0.5) / w * 2 - 1, v = (y + 0.5) / h * 2 - 1;
				double along = Math.pow(Math.max(0, 1 - u * u), 1.4);
				mask(img, x, y, along * (0.75 * Math.exp(-v * v / 0.05) + 0.25 * Math.exp(-v * v / 0.3)));
			}
		}
		return img;
	}

	static BufferedImage streak(int w, int h) {
		BufferedImage img = image(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double u = (x + 0.5) / w, v = (y + 0.5) / h * 2 - 1;
				double along = Math.pow(u, 2.0) * (1 - smooth(0.9, 1.0, u));
				mask(img, x, y, along * (0.8 * Math.exp(-v * v / 0.04) + 0.2 * Math.exp(-v * v / 0.3)));
			}
		}
		return img;
	}

	static BufferedImage ribbon(int w, int h, int seed) {
		BufferedImage img = image(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double u = (x + 0.5) / w * 2 - 1, v = (double) y / h;
				double profile = 0.85 * Math.exp(-u * u / 0.022) + 0.32 * Math.exp(-u * u / 0.22);
				// Along it, long streaks: noise drawn out along v (period 8 along, 2 across, so it repeats along v).
				double n = fbm(u * 1.5 + 4, v * 8, 0, 8, seed, 4);
				mask(img, x, y, profile * (0.62 + 0.55 * (n - 0.5) + 0.2));
			}
		}
		return img;
	}

	static BufferedImage flow(int w, int h, int seed) {
		BufferedImage img = image(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double u = (double) x / w, v = (double) y / h;
				// Warped, and drawn out upward: tongues of flame.
				double q = fbm(u * 4, v * 2, 4, 2, seed, 3);
				double n = fbm(u * 6 + q * 1.4, v * 2 + q * 0.6, 6, 2, seed + 1, 5);
				double a = Math.pow(clamp01((n - 0.28) * 1.9), 1.35);
				mask(img, x, y, 0.15 + 0.85 * a);
			}
		}
		return img;
	}

	static BufferedImage flameBand(int w, int h, int seed) {
		BufferedImage img = image(w, h);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double u = (double) x / w, v = (double) y / h;
				double q = fbm(u * 10, v * 2.5, 10, 0, seed, 3);
				double n = fbm(u * 10 + q * 1.6 + v * 1.2, v * 3.5 + q * 0.8, 10, 0, seed + 1, 5);
				double edge = Math.exp(-Math.pow((v - 0.09) / 0.035, 2)) * (0.75 + 0.25 * n);
				double body = Math.pow(clamp01((n - 0.25) * 1.8), 1.3) * Math.pow(1 - v, 1.3) * smooth(0.03, 0.14, v);
				mask(img, x, y, Math.min(1, edge * 0.95 + body * 0.95));
			}
		}
		return img;
	}

	static BufferedImage puff(int size, int seed) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = (x - c) / (size / 2.0), dy = (y - c) / (size / 2.0);
				double r = Math.hypot(dx, dy), ang = Math.atan2(dy, dx) / (Math.PI * 2) + 0.5;
				double rough = fbm(ang * 6, r * 2, 6, 0, seed, 3); // its broken edge, round the circle
				double rr = r * (0.82 + 0.4 * (rough - 0.5));
				// Clear well inside the square whatever the broken edge does (by the true radius): a billow never shows its quad's straight edge.
				double body = (1 - smooth(0.45, 1.0, rr)) * (1 - smooth(0.74, 0.95, r));
				double inner = fbm(dx * 3 + 7, dy * 3 + 3, 0, 0, seed + 1, 4);
				double a = body * (0.55 + 0.6 * (inner - 0.3));
				// Shaded as a billow lit from above (its upper side paler): light carried in its colour, its cover in alpha.
				double shade = clamp01(0.72 + 0.28 * (-dy) + 0.2 * (inner - 0.5));
				put(img, x, y, shade, shade, shade, clamp01(a));
			}
		}
		return img;
	}

	static BufferedImage ring(int size) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double r = Math.hypot(x - c, y - c) / (size / 2.0);
				double a = Math.exp(-Math.pow((r - 0.9) / 0.022, 2)) + 0.35 * Math.exp(-Math.pow((r - 0.88) / 0.07, 2));
				mask(img, x, y, Math.min(1, a) * (1 - smooth(0.97, 1.0, r)));
			}
		}
		return img;
	}

	static BufferedImage rays(int size) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = (x - c) / (size / 2.0), dy = (y - c) / (size / 2.0), r = Math.hypot(dx, dy);
				double a = 0.9 * Math.exp(-r * r / 0.004) + 0.3 * Math.exp(-r * r / 0.04);
				for (int k = 0; k < 4; k++) { // four long rays, four shorter between them
					double ang = k * Math.PI / 4, ax = Math.cos(ang), ay = Math.sin(ang);
					double perp = Math.abs(dx * ay - dy * ax);
					double len = k % 2 == 0 ? 1.0 : 0.55;
					a += (k % 2 == 0 ? 0.8 : 0.45) * Math.exp(-perp * perp / (0.00012 + 0.0009 * r)) * Math.pow(Math.max(0, 1 - r / len), 2.2);
				}
				mask(img, x, y, Math.min(1, a) * (1 - smooth(0.95, 1.0, r)));
			}
		}
		return img;
	}

	static BufferedImage cloud(int size, int seed) {
		BufferedImage img = image(size, size);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double u = (double) x / size, v = (double) y / size;
				double q = fbm(u * 4, v * 4, 4, 4, seed, 3);
				double n = fbm(u * 5 + q * 1.5, v * 5 - q * 1.2, 5, 5, seed + 1, 6);
				double billow = Math.pow(1 - Math.abs(2 * fbm(u * 8 + q, v * 8, 8, 8, seed + 2, 4) - 1), 2.5); // the billows' rims
				double density = smooth(0.22, 0.7, n);
				// Shaded billows (their rims paler), nearly whole cover: a storm's underside hides the sky behind it.
				double shade = clamp01(0.34 + 0.62 * (n - 0.4) + 0.42 * billow);
				put(img, x, y, shade, shade, shade * 1.05, clamp01(0.9 + 0.1 * density));
			}
		}
		return img;
	}

	// --- The runic array ---

	/** Concentric rings: a double ring at the rim with ticks inside it, a band of runes, two rings further in. */
	static BufferedImage arrayRings(int size, int seed) {
		Canvas cv = new Canvas(size);
		double R = size / 2.0 * 0.98;
		cv.width(R * 0.006);
		cv.circle(R * 0.985);
		cv.width(R * 0.004);
		cv.circle(R * 0.955);
		// Ticks between the rim's rings, long and short by turns.
		for (int k = 0; k < 120; k++) {
			double a = k * Math.PI * 2 / 120, r0 = R * (k % 5 == 0 ? 0.915 : 0.932);
			cv.line(Math.cos(a) * r0, Math.sin(a) * r0, Math.cos(a) * R * 0.952, Math.sin(a) * R * 0.952);
		}
		cv.width(R * 0.005);
		cv.circle(R * 0.90);
		cv.circle(R * 0.775);
		// The band of runes between them, each standing outward.
		Random rnd = new Random(seed);
		int n = 30;
		cv.width(R * 0.0065);
		for (int k = 0; k < n; k++) {
			double a = k * Math.PI * 2 / n;
			cv.rune(a, R * 0.838, R * 0.085, rnd);
		}
		cv.width(R * 0.004);
		cv.circle(R * 0.50);
		cv.width(R * 0.003);
		cv.circle(R * 0.47);
		cv.width(R * 0.0045);
		cv.circle(R * 0.27);
		return cv.glowing(size / 128.0);
	}

	/** Eight small circles round the array, a rune in each, threaded on a ring. */
	static BufferedImage arrayCircles(int size, int seed) {
		Canvas cv = new Canvas(size);
		double R = size / 2.0 * 0.98, at = R * 0.64, small = R * 0.095;
		Random rnd = new Random(seed);
		cv.width(R * 0.005);
		for (int k = 0; k < 8; k++) { // the ring between them, broken where each sits (circle k at k/8 of the way round, plus a sixteenth)
			double a0 = k * Math.PI / 4 + Math.PI / 8, gap = Math.asin(small * 1.1 / at);
			cv.arc(at, a0 + gap, Math.PI / 4 - 2 * gap);
		}
		for (int k = 0; k < 8; k++) {
			double a = k * Math.PI / 4 + Math.PI / 8;
			double cx = Math.cos(a) * at, cy = Math.sin(a) * at;
			cv.width(R * 0.007);
			cv.circleAt(cx, cy, small);
			cv.width(R * 0.004);
			cv.circleAt(cx, cy, small * 0.78);
			cv.width(R * 0.0065);
			cv.runeAt(cx, cy, a, small * 0.62, rnd);
		}
		return cv.glowing(size / 128.0);
	}

	/** An eight-pointed star of two squares, with the lines joining its points. */
	static BufferedImage arrayStar(int size) {
		Canvas cv = new Canvas(size);
		double R = size / 2.0 * 0.98, r = R * 0.5;
		cv.width(R * 0.006);
		for (int s = 0; s < 2; s++) {
			Path2D path = new Path2D.Double();
			for (int k = 0; k <= 4; k++) {
				double a = s * Math.PI / 4 + k * Math.PI / 2;
				if (k == 0) path.moveTo(Math.cos(a) * r, Math.sin(a) * r);
				else path.lineTo(Math.cos(a) * r, Math.sin(a) * r);
			}
			cv.path(path);
		}
		cv.width(R * 0.0035);
		Path2D star = new Path2D.Double(); // the points joined three apart: a star drawn in one line
		for (int k = 0; k <= 8; k++) {
			double a = (k * 3 % 8) * Math.PI / 4;
			if (k == 0) star.moveTo(Math.cos(a) * r, Math.sin(a) * r);
			else star.lineTo(Math.cos(a) * r, Math.sin(a) * r);
		}
		cv.path(star);
		return cv.glowing(size / 128.0);
	}

	/** Flame swirling in the band between the rings (r from 0.28 to 0.76 of the way out), brighter near the inner edge. */
	static BufferedImage arrayFlame(int size, int seed) {
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0, R = size / 2.0 * 0.98;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double dx = x - c, dy = y - c, r = Math.hypot(dx, dy) / R;
				if (r < 0.2 || r > 0.84) continue;
				double v = (r - 0.28) / 0.48, ang = Math.atan2(dy, dx) / (Math.PI * 2) + 0.5;
				double u = ang + v * 0.3; // swirled
				// Fewer, bigger tongues with dark between them (seen from 8 blocks up, finer ones blurred into an even fill).
				double q = fbm(u * 7, v * 2, 7, 0, seed, 3);
				double n = fbm(u * 7 + q * 1.5, v * 2.5 + q, 7, 0, seed + 1, 4);
				double band = smooth(0.0, 0.12, v) * (1 - smooth(0.82, 1.0, v));
				double tongues = Math.pow(clamp01((n - 0.42) * 2.6), 1.3);
				mask(img, x, y, band * (0.04 + 0.9 * tongues) * (1.0 - 0.3 * v));
			}
		}
		return img;
	}

	/** The red heart: a red glow, a bright ring round it, and a rune cut dark through the middle. Its own colours. */
	static BufferedImage arrayCore(int size, int seed) {
		Canvas cut = new Canvas(size); // the rune, as a mask
		cut.width(size * 0.045);
		cut.runeAt(0, 0, Math.PI / 2, size * 0.42, new Random(seed));
		BufferedImage rune = cut.img;
		BufferedImage img = image(size, size);
		double c = (size - 1) / 2.0;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double r = Math.hypot(x - c, y - c) / (size / 2.0);
				double glow = 0.85 * Math.exp(-r * r / 0.18) * (1 - smooth(0.85, 1.0, r));
				double rim = Math.exp(-Math.pow((r - 0.78) / 0.035, 2));
				double dark = (rune.getRGB(x, y) >>> 24) / 255.0;
				double red = glow * (1 - 0.9 * dark);
				// Deep red to orange at the rim; the ring itself pale gold.
				double cr = red * 0.95 + rim * 1.0, cg = red * 0.22 + rim * 0.75, cb = red * 0.07 + rim * 0.4;
				double a = clamp01(Math.max(red, rim));
				double k = a > 0 ? 1 / a : 0;
				put(img, x, y, clamp01(cr * k), clamp01(cg * k), clamp01(cb * k), a);
			}
		}
		return img;
	}

	/** Strokes drawn smooth on a square, centred, then given a glow: their own soft halo. */
	static final class Canvas {
		final BufferedImage img;
		final Graphics2D g;
		final double c;

		Canvas(int size) {
			img = image(size, size);
			g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setColor(Color.WHITE);
			c = size / 2.0;
			g.translate(c, c);
		}

		void width(double w) {
			g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		}

		void circle(double r) {
			g.draw(new Ellipse2D.Double(-r, -r, r * 2, r * 2));
		}

		void circleAt(double x, double y, double r) {
			g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
		}

		void arc(double r, double from, double extent) {
			g.draw(new Arc2D.Double(-r, -r, r * 2, r * 2, -Math.toDegrees(from), -Math.toDegrees(extent), Arc2D.OPEN));
		}

		void line(double x0, double y0, double x1, double y1) {
			g.draw(new Line2D.Double(x0, y0, x1, y1));
		}

		void path(Path2D p) {
			g.draw(p);
		}

		/** A rune at angle {@code a}, {@code r} out from the middle, standing outward, {@code s} tall. */
		void rune(double a, double r, double s, Random rnd) {
			runeAt(Math.cos(a) * r, Math.sin(a) * r, a, s, rnd);
		}

		/** A rune centred on (x, y), its top toward angle {@code a}, {@code s} tall: a stem, branches off it, sometimes a hook or a dot. */
		void runeAt(double x, double y, double a, double s, Random rnd) {
			AffineTransform keep = g.getTransform();
			g.translate(x, y);
			g.rotate(a + Math.PI / 2);
			double h = s / 2;
			int form = rnd.nextInt(4);
			if (form == 3) { // two stems
				line(-h * 0.35, -h, -h * 0.35, h);
				line(h * 0.35, -h, h * 0.35, h);
			} else {
				line(0, -h, 0, h);
			}
			int branches = 1 + rnd.nextInt(3);
			for (int b = 0; b < branches; b++) {
				double y0 = (rnd.nextDouble() * 1.6 - 0.8) * h, side = rnd.nextBoolean() ? 1 : -1;
				double len = s * (0.28 + 0.22 * rnd.nextDouble()), tilt = (rnd.nextDouble() * 1.1 - 0.55);
				line(0, y0, side * len * Math.cos(tilt), y0 - len * Math.sin(tilt));
			}
			if (rnd.nextInt(3) == 0) { // a hook at its top
				double hr = s * 0.18;
				g.draw(new Arc2D.Double(-hr, -h - hr, hr * 2, hr * 2, 0, rnd.nextBoolean() ? 180 : -180, Arc2D.OPEN));
			}
			if (rnd.nextInt(3) == 0) { // a dot beside it
				double d = s * 0.07, side = rnd.nextBoolean() ? 1 : -1;
				g.fill(new Ellipse2D.Double(side * s * 0.32 - d, h * 0.55 - d, d * 2, d * 2));
			}
			g.setTransform(keep);
		}

		/** The strokes as a mask with a halo: the line's own alpha, and a soft glow round it (a wide blur, at a third). */
		BufferedImage glowing(double blur) {
			g.dispose();
			int n = img.getWidth();
			float[] a = new float[n * n];
			for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) a[y * n + x] = (img.getRGB(x, y) >>> 24) / 255f;
			float[] soft = boxBlur(boxBlur(boxBlur(a, n, (int) Math.max(1, blur)), n, (int) Math.max(1, blur)), n, (int) Math.max(1, blur));
			float[] near = boxBlur(a, n, 1);
			BufferedImage out = image(n, n);
			for (int i = 0; i < n * n; i++) mask(out, i % n, i / n, Math.min(1, Math.max(near[i], a[i]) * 0.92 + soft[i] * 0.55));
			return out;
		}
	}

	static float[] boxBlur(float[] in, int n, int r) {
		float[] tmp = new float[n * n], out = new float[n * n];
		for (int y = 0; y < n; y++) {
			float sum = 0;
			for (int x = -r; x <= r; x++) sum += in[y * n + Math.max(0, Math.min(n - 1, x))];
			for (int x = 0; x < n; x++) {
				tmp[y * n + x] = sum / (2 * r + 1);
				sum += in[y * n + Math.min(n - 1, x + r + 1)] - in[y * n + Math.max(0, x - r)];
			}
		}
		for (int x = 0; x < n; x++) {
			float sum = 0;
			for (int y = -r; y <= r; y++) sum += tmp[Math.max(0, Math.min(n - 1, y)) * n + x];
			for (int y = 0; y < n; y++) {
				out[y * n + x] = sum / (2 * r + 1);
				sum += tmp[Math.min(n - 1, y + r + 1) * n + x] - tmp[Math.max(0, y - r) * n + x];
			}
		}
		return out;
	}

	// --- Noise: gradient noise that repeats with the given periods (0 = no repeat), and its octaves ---

	static double fbm(double x, double y, int px, int py, int seed, int octaves) {
		double sum = 0, amp = 0.5, norm = 0;
		for (int o = 0; o < octaves; o++) {
			sum += amp * noise(x, y, px, py, seed + o * 131);
			norm += amp;
			x *= 2;
			y *= 2;
			px *= 2;
			py *= 2;
			amp *= 0.5;
		}
		return clamp01(sum / norm * 0.5 + 0.5);
	}

	/** Gradient noise in -1..1. */
	static double noise(double x, double y, int px, int py, int seed) {
		int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
		double fx = x - x0, fy = y - y0;
		double u = fx * fx * fx * (fx * (fx * 6 - 15) + 10), v = fy * fy * fy * (fy * (fy * 6 - 15) + 10);
		double n00 = grad(x0, y0, px, py, seed, fx, fy), n10 = grad(x0 + 1, y0, px, py, seed, fx - 1, fy);
		double n01 = grad(x0, y0 + 1, px, py, seed, fx, fy - 1), n11 = grad(x0 + 1, y0 + 1, px, py, seed, fx - 1, fy - 1);
		return 1.4 * lerp(v, lerp(u, n00, n10), lerp(u, n01, n11));
	}

	static double grad(int ix, int iy, int px, int py, int seed, double dx, double dy) {
		if (px > 0) ix = Math.floorMod(ix, px);
		if (py > 0) iy = Math.floorMod(iy, py);
		long h = seed * 0x9E3779B97F4A7C15L + ix * 0xBF58476D1CE4E5B9L + iy * 0x94D049BB133111EBL;
		h ^= h >>> 31;
		h *= 0x7FB5D329728EA185L;
		h ^= h >>> 27;
		double a = (h >>> 11) / (double) (1L << 53) * Math.PI * 2;
		return Math.cos(a) * dx + Math.sin(a) * dy;
	}

	// --- Helpers ---

	static BufferedImage image(int w, int h) {
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	}

	/** White, at strength {@code a}. */
	static void mask(BufferedImage img, int x, int y, double a) {
		put(img, x, y, 1, 1, 1, clamp01(a));
	}

	static void put(BufferedImage img, int x, int y, double r, double g, double b, double a) {
		int ai = (int) Math.round(clamp01(a) * 255), ri = (int) Math.round(clamp01(r) * 255), gi = (int) Math.round(clamp01(g) * 255),
				bi = (int) Math.round(clamp01(b) * 255);
		img.setRGB(x, y, ai << 24 | ri << 16 | gi << 8 | bi);
	}

	static double smooth(double e0, double e1, double x) {
		double t = clamp01((x - e0) / (e1 - e0));
		return t * t * (3 - 2 * t);
	}

	static double lerp(double t, double a, double b) {
		return a + (b - a) * t;
	}

	static double clamp01(double v) {
		return v < 0 ? 0 : v > 1 ? 1 : v;
	}

	private FxTex() {}
}
