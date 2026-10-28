import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The legendary weapons' art, phase 3c (2026-10-28): Pangu's Axe (a crescent of primordial chaos on a bronze haft, its veins swirling) and the
 * Celestial Court's five Venerables' weapons: the Azure Dragon Glaive (a guandao of azure steel, light running up it), the White Tiger Saber
 * (a broad white dao striped with gold), the Vermilion Bird Fan (a war fan of red feathers on gold ribs, its rim burning), the Black Tortoise
 * Mace (a round head of shell plates on a black haft, a serpent wound about it, water glinting over it) and the Yellow Dragon Seal (a block of
 * yellow jade on a gold rod, a dragon carved round it, its stamp's characters on its face). Each laid from the lower left (the hand) to the
 * upper right as a held sword is (LegendArt#held), each on its own living texture ({@link LegendArt#SIZE}, {@link LegendArt#FRAMES}).
 * <p>
 * A flat shape (a blade, the fan's leaf) is built a row of pixels at a time ({@link #raster}), each row showing its own slice of the shape's
 * picture, so the picture reads whole from either side.
 */
final class LegendArt3c {
	static final int SIZE = LegendArt.SIZE, FRAMES = LegendArt.FRAMES;

	/** A picture's colour at the model's (x, y) (its pixels, upright about the hand) in {@code frame}; 0 for none (see through). */
	interface Pixel {
		int at(double x, double y, int frame);
	}

	static double[] v(double... values) { return values; }

	static double lerp(double a, double b, double t) { return a + (b - a) * t; }

	/**
	 * Boxes for a flat shape seen front on: one a row, {@code rows} {y, x from, x to} (a pixel tall), {@code z0}..{@code z1} deep; each
	 * row's front and back its own slice of the shape's picture ({@code pic}: u1 v1 u2 v2 standing for the model's {@code shape}, x0 y0 x1
	 * y1, its top the shape's highest row), the back mirrored as the shape is seen from behind; its edges and ends {@code edge}.
	 */
	static void raster(List<LegendArt.Box> boxes, double[][] rows, double z0, double z1, double[] shape, double[] pic, double[] edge) {
		for (double[] row : rows) {
			double y = row[0], xa = row[1], xb = row[2];
			if (xb - xa < 0.05) continue;
			double ua = lerp(pic[0], pic[2], (xa - shape[0]) / (shape[2] - shape[0])), ub = lerp(pic[0], pic[2], (xb - shape[0]) / (shape[2] - shape[0]));
			double vt = lerp(pic[1], pic[3], (shape[3] - (y + 1)) / (shape[3] - shape[1])), vb = lerp(pic[1], pic[3], (shape[3] - y) / (shape[3] - shape[1]));
			boxes.add(new LegendArt.Box(v(xa, y, z0), v(xb, y + 1, z1), v(ua, vt, ub, vb), edge, edge, v(ub, vt, ua, vb)));
		}
	}

	/** {@code pixel}'s picture painted over {@code pic} (sixteenths) of the frame at {@code top}, standing for the model's {@code shape}. */
	static void picture(BufferedImage img, int top, int frame, double[] pic, double[] shape, Pixel pixel) {
		int u0 = (int) Math.round(pic[0] * 2), v0 = (int) Math.round(pic[1] * 2), u1 = (int) Math.round(pic[2] * 2), v1 = (int) Math.round(pic[3] * 2);
		for (int pv = v0; pv < v1; pv++) {
			for (int pu = u0; pu < u1; pu++) {
				double x = lerp(shape[0], shape[2], (pu - u0 + 0.5) / (u1 - u0)), y = lerp(shape[3], shape[1], (pv - v0 + 0.5) / (v1 - v0));
				int c = pixel.at(x, y, frame);
				if (c != 0) img.setRGB(pu, top + pv, c);
			}
		}
	}

	/** A patch's colour at its own pixel (u, v), counted from its corner, in {@code frame}. */
	interface Texel {
		int at(int u, int v, int frame);
	}

	/** A patch (sixteenths) filled by {@code texel}. */
	static void patch(BufferedImage img, int top, int frame, double[] at, Texel texel) {
		int u0 = (int) Math.round(at[0] * 2), v0 = (int) Math.round(at[1] * 2), u1 = (int) Math.round(at[2] * 2), v1 = (int) Math.round(at[3] * 2);
		for (int pv = v0; pv < v1; pv++) for (int pu = u0; pu < u1; pu++) img.setRGB(pu, top + pv, texel.at(pu - u0, pv - v0, frame));
	}

	static int rgb(int hex) { return BeastArt.rgb(hex); }

	static int mix(int a, int b, double t) { return BeastArt.mix(a, b, t); }

	// =================================================== Pangu's Axe ===================================================

	static final double[] AXE_SHAPE = v(-2.5, 11, 7, 22), AXE_PIC = v(0, 0, 10, 12), AXE_EDGE = v(10, 0, 11, 1), AXE_HAFT = v(11, 0, 12, 16),
			AXE_CAP = v(12, 0, 13, 1), AXE_BAND = v(12, 1, 14, 3), AXE_SPIKE = v(10, 1, 11, 2);

	/** How far the axe's blade reaches out from its haft at height {@code y}: its edge bellying out at its middle. */
	static double axeReach(double y) {
		return 2 + 6.5 * Math.sin(Math.PI * Math.max(0, Math.min(1, (y - 11) / 11)));
	}

	static List<LegendArt.Box> axe() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7, -6, 7), v(9, 21, 9), AXE_HAFT, AXE_HAFT, AXE_CAP));
		for (double[] band : new double[][] {{-6.5, -4.5}, {9, 10}, {19.5, 21.5}}) {
			boxes.add(new LegendArt.Box(v(6.6, band[0], 6.6), v(9.4, band[1], 9.4), AXE_BAND, AXE_BAND, AXE_BAND));
		}
		double[][] rows = new double[11][];
		for (int i = 0; i < 11; i++) rows[i] = v(11 + i, 7 - axeReach(11 + i + 0.5), 7);
		raster(boxes, rows, 7.35, 8.65, AXE_SHAPE, AXE_PIC, AXE_EDGE);
		// Its beard, a spike out the back; its crown, a point on top.
		for (int i = 0; i < 4; i++) {
			double out = 2.2 - Math.abs(i - 1.5) * 0.7;
			boxes.add(new LegendArt.Box(v(9, 14 + i, 7.6), v(9 + out, 15 + i, 8.4), AXE_SPIKE, AXE_SPIKE, AXE_SPIKE));
		}
		boxes.add(new LegendArt.Box(v(7.5, 21.5, 7.5), v(8.5, 23.5, 8.5), AXE_EDGE, AXE_EDGE, AXE_EDGE));
		return boxes;
	}

	static void paintAxe(BufferedImage img, int frame, int top) {
		double t = frame / (double) FRAMES * Math.PI * 2;
		picture(img, top, frame, AXE_PIC, AXE_SHAPE, (x, y, f) -> {
			double edge = 7 - axeReach(y);
			if (x < edge - 0.2 || x > 7.05 || y < 11 || y > 22) return 0;
			// Primordial chaos: near black, violet veins swirling through it, its edge keen and pale, its back bronze where it meets the haft.
			double swirl = Math.sin(x * 0.9 + y * 0.7 + t) + Math.sin(y * 1.3 - x * 0.4 - t * 2);
			int c = mix(rgb(0x15101E), rgb(0x2E1C46), 0.5 + 0.5 * Math.sin(x * 0.5 + y * 0.3));
			if (Math.abs(swirl) < 0.35) c = mix(rgb(0x9A6AFF), rgb(0xEADCFF), 1 - Math.abs(swirl) / 0.35);
			if (x - edge < 1.1) c = mix(rgb(0xF0E8FF), rgb(0xB8A8E8), (x - edge) / 1.1);
			if (x > 6.2) c = mix(c, rgb(0x5A3A1E), 0.7);
			return c;
		});
		patch(img, top, frame, AXE_EDGE, (u, v, f) -> mix(rgb(0xF0E8FF), rgb(0xA890E0), (u + v) / 4.0));
		patch(img, top, frame, AXE_SPIKE, (u, v, f) -> mix(rgb(0x2E1C46), rgb(0x9A6AFF), 0.3 + 0.2 * Math.sin(t + u)));
		patch(img, top, frame, AXE_HAFT, (u, v, f) -> {
			// Dark bronze, its grain, and old characters glowing in it a moment as a pulse runs up the haft.
			int c = mix(rgb(0x3A2618), rgb(0x5A3E26), 0.3 + 0.4 * BeastArt.hash(u, v / 3, 7));
			if (u == 0 || u == 3) c = BeastArt.shade(c, -0.25);
			double pulse = Math.pow(Math.max(0, Math.cos((v + f * 2.0) * Math.PI * 2 / SIZE)), 6);
			if ((v % 5 == 2) && u > 0 && u < 3) c = mix(c, rgb(0xC8A0FF), 0.3 + 0.6 * pulse);
			return c;
		});
		patch(img, top, frame, AXE_CAP, (u, v, f) -> rgb(0x2A1A10));
		patch(img, top, frame, AXE_BAND, (u, v, f) -> v == 0 ? rgb(0xFFE89A) : v == 3 ? rgb(0x9A6A14) : rgb(0xE8B840));
	}

	// ============================================ The Azure Dragon Glaive (wood) ============================================

	static final double[] GLAIVE_SHAPE = v(3, 15, 10, 27), GLAIVE_PIC = v(0, 0, 8, 14), GLAIVE_EDGE = v(8, 0, 9, 1), GLAIVE_POLE = v(9, 0, 10, 16),
			GLAIVE_CAP = v(10, 0, 11, 1), GLAIVE_GUARD = v(10, 1, 12, 3), GLAIVE_RING = v(12, 0, 14, 2), GLAIVE_TASSEL = v(12, 2, 13, 5);

	/** The glaive's blade at height {@code y}: from its edge to its spine, widening and then curving back to its point. */
	static double[] glaiveRow(double y) {
		double t = Math.max(0, Math.min(1, (y - 15) / 12));
		double w = 3.2 + 2.4 * Math.sin(Math.PI * t * 0.95), back = t > 0.7 ? (t - 0.7) * 10 : 0;
		double xb = 9 + back * 0.25, xa = Math.min(xb - 0.7, 9 - w + back);
		return v(xa, xb);
	}

	static List<LegendArt.Box> glaive() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7.3, -9, 7.3), v(8.7, 13, 8.7), GLAIVE_POLE, GLAIVE_POLE, GLAIVE_CAP));
		for (double[] ring : new double[][] {{-9.5, -8}, {3, 4}}) boxes.add(new LegendArt.Box(v(7, ring[0], 7), v(9, ring[1], 9), GLAIVE_RING, GLAIVE_RING, GLAIVE_RING));
		// The dragon's head the blade comes out of, and a red tassel hanging from it.
		boxes.add(new LegendArt.Box(v(6.5, 13, 6.6), v(9.6, 15, 9.4), GLAIVE_GUARD, GLAIVE_GUARD, GLAIVE_GUARD));
		boxes.add(new LegendArt.Box(v(9.6, 10.5, 7.7), v(10.3, 13.5, 8.3), GLAIVE_TASSEL, GLAIVE_TASSEL, GLAIVE_TASSEL));
		double[][] rows = new double[12][];
		for (int i = 0; i < 12; i++) {
			double[] row = glaiveRow(15 + i + 0.5);
			rows[i] = v(15 + i, row[0], row[1]);
		}
		raster(boxes, rows, 7.5, 8.5, GLAIVE_SHAPE, GLAIVE_PIC, GLAIVE_EDGE);
		return boxes;
	}

	static void paintGlaive(BufferedImage img, int frame, int top) {
		picture(img, top, frame, GLAIVE_PIC, GLAIVE_SHAPE, (x, y, f) -> {
			double[] row = glaiveRow(y);
			if (x < row[0] - 0.2 || x > row[1] + 0.2 || y < 15 || y > 27) return 0;
			double fromEdge = x - row[0], fromSpine = row[1] - x;
			int c = mix(rgb(0x2A78B8), rgb(0x184A78), 0.3 + 0.4 * (y - 15) / 12);
			// The dragon's scales along its spine; its edge bright; light running up it toward the point.
			if (fromSpine < 1.4 && ((int) (y * 2) + (int) (x * 2)) % 3 == 0) c = rgb(0x3ABF8A);
			if (fromEdge < 0.9) c = mix(rgb(0xE8F8FF), rgb(0x8ACCF0), fromEdge / 0.9);
			double run = Math.pow(Math.max(0, Math.cos((y - f * 0.75) * Math.PI / 6)), 8);
			return mix(c, rgb(0xC8F4FF), run * 0.6 * (fromEdge > 0.9 ? 1 : 0.4));
		});
		patch(img, top, frame, GLAIVE_EDGE, (u, v, f) -> rgb(0xD8F0FF));
		patch(img, top, frame, GLAIVE_POLE, (u, v, f) -> {
			int c = mix(rgb(0x1E2A28), rgb(0x2E4038), 0.3 + 0.4 * BeastArt.hash(u, v / 4, 13));
			return v % 8 == 0 ? rgb(0x3ABF8A) : u == 0 || u == 3 ? BeastArt.shade(c, -0.3) : c;
		});
		patch(img, top, frame, GLAIVE_CAP, (u, v, f) -> rgb(0x1A2420));
		patch(img, top, frame, GLAIVE_GUARD, (u, v, f) -> (u + v) % 3 == 0 ? rgb(0x3ABF8A) : v == 0 ? rgb(0xFFE08A) : rgb(0xC89A2A));
		patch(img, top, frame, GLAIVE_RING, (u, v, f) -> v == 0 ? rgb(0x8AF0C0) : rgb(0x2E9A6A));
		patch(img, top, frame, GLAIVE_TASSEL, (u, v, f) -> (u + v + f / 4) % 2 == 0 ? rgb(0xE83A28) : rgb(0x8A1010));
	}

	// ============================================ The White Tiger Saber (metal) ============================================

	static final double[] SABER_SHAPE = v(4.5, 2, 9.5, 21), SABER_PIC = v(0, 0, 6, 16), SABER_EDGE = v(6, 0, 7, 1), SABER_GRIP = v(7, 0, 8, 4),
			SABER_GUARD = v(8, 0, 10, 2), SABER_POMMEL = v(8, 2, 10, 4), SABER_CAP = v(7, 4, 8, 5);

	/** The saber's blade at height {@code y}: its spine straight, its edge widening toward the point, the point sweeping up to the spine. */
	static double[] saberRow(double y) {
		double t = Math.max(0, Math.min(1, (y - 2) / 19));
		double w = 2.6 + 1.7 * t;
		if (t > 0.82) w *= Math.max(0.15, (1 - t) / 0.18);
		return v(9 - w, 9);
	}

	static List<LegendArt.Box> saber() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7.3, -4, 7.3), v(8.7, 1, 8.7), SABER_GRIP, SABER_GRIP, SABER_CAP));
		boxes.add(new LegendArt.Box(v(7, -5.2, 7.4), v(9, -4, 8.6), SABER_POMMEL, SABER_POMMEL, SABER_POMMEL));
		boxes.add(new LegendArt.Box(v(5.6, 1, 5.6), v(10.4, 2, 10.4), SABER_GUARD, SABER_GUARD, SABER_GUARD));
		double[][] rows = new double[19][];
		for (int i = 0; i < 19; i++) {
			double[] row = saberRow(2 + i + 0.5);
			rows[i] = v(2 + i, row[0], row[1]);
		}
		raster(boxes, rows, 7.55, 8.45, SABER_SHAPE, SABER_PIC, SABER_EDGE);
		return boxes;
	}

	static void paintSaber(BufferedImage img, int frame, int top) {
		picture(img, top, frame, SABER_PIC, SABER_SHAPE, (x, y, f) -> {
			double[] row = saberRow(y);
			if (x < row[0] - 0.2 || x > 9.05 || y < 2 || y > 21) return 0;
			double fromEdge = x - row[0], fromSpine = 9 - x;
			int c = mix(rgb(0xE8EEF2), rgb(0xA8B4C0), 0.25 + 0.35 * (y - 2) / 19);
			// The tiger's stripes, black and gold, slanting across its back; its edge white; a glint running up it.
			double stripe = (y * 1.1 + x * 0.6) % 3.2;
			if (fromSpine < 1.8 && stripe < 0.8) c = stripe < 0.4 ? rgb(0x2A2620) : rgb(0xE8B840);
			if (fromEdge < 0.8) c = mix(rgb(0xFFFFFF), rgb(0xD8E2EA), fromEdge / 0.8);
			double run = Math.pow(Math.max(0, Math.cos((y - f * 1.2) * Math.PI / 6)), 10);
			return mix(c, rgb(0xFFFFFF), run * 0.5);
		});
		patch(img, top, frame, SABER_EDGE, (u, v, f) -> rgb(0xF4F8FF));
		patch(img, top, frame, SABER_GRIP, (u, v, f) -> (u + v) % 3 == 0 ? rgb(0x2A2620) : rgb(0xF0EEE8));
		patch(img, top, frame, SABER_CAP, (u, v, f) -> rgb(0xC8A040));
		patch(img, top, frame, SABER_GUARD, (u, v, f) -> v == 0 ? rgb(0xFFE89A) : (u + v) % 3 == 0 ? rgb(0x9A6A14) : rgb(0xE8B840));
		patch(img, top, frame, SABER_POMMEL, (u, v, f) -> v == 0 ? rgb(0xFFE89A) : rgb(0xC89A2A));
	}

	// ============================================ The Vermilion Bird Fan (fire) ============================================

	static final double[] FAN_SHAPE = v(-2, 3, 18, 13.5), FAN_PIC = v(0, 0, 14, 8), FAN_EDGE = v(14, 0, 15, 1), FAN_HANDLE = v(15, 0, 16, 5),
			FAN_PIVOT = v(14, 1, 15, 2), FAN_HANDLE_CAP = v(14, 2, 15, 3);
	/** The fan's leaf: a half disc this far round about its pivot, a hole of this round at its heart. */
	static final double FAN_R = 10, FAN_HOLE = 3.2;

	static List<LegendArt.Box> fan() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7.4, -5, 7.4), v(8.6, 3, 8.6), FAN_HANDLE, FAN_HANDLE, FAN_HANDLE_CAP));
		boxes.add(new LegendArt.Box(v(7, 3, 7.3), v(9, 4.2, 8.7), FAN_PIVOT, FAN_PIVOT, FAN_PIVOT));
		double[][] rows = new double[10][];
		for (int i = 0; i < 10; i++) {
			double dy = i + 1, half = Math.sqrt(Math.max(0, FAN_R * FAN_R - dy * dy));
			rows[i] = v(3 + i + 0.5, 8 - half, 8 + half);
		}
		raster(boxes, rows, 7.8, 8.2, FAN_SHAPE, FAN_PIC, FAN_EDGE);
		return boxes;
	}

	static void paintFan(BufferedImage img, int frame, int top) {
		double t = frame / (double) FRAMES * Math.PI * 2;
		picture(img, top, frame, FAN_PIC, FAN_SHAPE, (x, y, f) -> {
			double dx = x - 8, dy = y - 3.5, r = Math.hypot(dx, dy);
			if (dy < 0 || r > FAN_R + 0.3) return 0;
			if (r < FAN_HOLE) return 0;
			double angle = Math.atan2(dy, dx);
			// Gold ribs fanning out from the pivot, red feathers between, their rims burning.
			double rib = Math.abs(((angle / Math.PI * 9) % 1 + 1) % 1 - 0.5);
			int c = mix(rgb(0xC8241E), rgb(0xFF6A20), 0.5 + 0.5 * Math.sin(angle * 9));
			if (rib > 0.42) c = rgb(0xE8B840);
			double fire = 0.5 + 0.5 * Math.sin(angle * 23 + t * 2) * Math.sin(r * 2 - t);
			if (r > FAN_R - 1.6) c = mix(rgb(0xFFD060), rgb(0xFFFFE0), fire);
			return c;
		});
		patch(img, top, frame, FAN_EDGE, (u, v, f) -> rgb(0xFF9A30));
		patch(img, top, frame, FAN_HANDLE, (u, v, f) -> v % 4 == 0 ? rgb(0xE8B840) : u == 0 ? rgb(0x7A1010) : rgb(0xB81E18));
		patch(img, top, frame, FAN_PIVOT, (u, v, f) -> rgb(0xFFE08A));
		patch(img, top, frame, FAN_HANDLE_CAP, (u, v, f) -> rgb(0xE8B840));
	}

	// ============================================ The Black Tortoise Mace (water) ============================================

	static final double[] MACE_HEAD = v(0, 0, 4, 4), MACE_HEAD_CAP = v(4, 0, 6, 2), MACE_HAFT = v(6, 0, 7, 16), MACE_BAND = v(7, 0, 9, 2),
			MACE_SERPENT = v(9, 0, 12, 1), MACE_KNOB = v(7, 2, 8, 3);

	/** How far out the mace's head stands at height {@code y}: round, its widest at its middle. */
	static double maceRound(double y) {
		return 0.9 + 3.1 * Math.sin(Math.PI * Math.max(0, Math.min(1, (y - 12) / 9)));
	}

	static List<LegendArt.Box> mace() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7.3, -6, 7.3), v(8.7, 12, 8.7), MACE_HAFT, MACE_HAFT, MACE_HAFT));
		for (double[] band : new double[][] {{-6.5, -5}, {4, 5}, {11, 12}}) boxes.add(new LegendArt.Box(v(7, band[0], 7), v(9, band[1], 9), MACE_BAND, MACE_BAND, MACE_BAND));
		// The head, round every way, a row at a time; the serpent wound about its middle; a knob atop.
		for (int i = 0; i < 9; i++) {
			double r = maceRound(12 + i + 0.5);
			boxes.add(new LegendArt.Box(v(8 - r, 12 + i, 8 - r), v(8 + r, 13 + i, 8 + r), MACE_HEAD, MACE_HEAD, MACE_HEAD_CAP));
		}
		double belt = maceRound(16) + 0.3;
		boxes.add(new LegendArt.Box(v(8 - belt, 15.6, 8 - belt), v(8 + belt, 16.6, 8 + belt), MACE_SERPENT, MACE_SERPENT, MACE_SERPENT));
		boxes.add(new LegendArt.Box(v(7.4, 21, 7.4), v(8.6, 22.2, 8.6), MACE_KNOB, MACE_KNOB, MACE_KNOB));
		return boxes;
	}

	static void paintMace(BufferedImage img, int frame, int top) {
		double t = frame / (double) FRAMES * Math.PI * 2;
		patch(img, top, frame, MACE_HEAD, (u, v, f) -> {
			// Shell plates, dark teal-black, their seams paler, water glinting across them.
			boolean seam = u == 0 || v == 0 || (u + v) % 4 == 0;
			int c = seam ? rgb(0x3A6A6A) : mix(rgb(0x14262A), rgb(0x1E3A3E), BeastArt.hash(u, v, 31));
			double glint = Math.pow(Math.max(0, Math.cos((u + v) * 0.8 - t)), 6);
			return mix(c, rgb(0x9ADAF0), glint * 0.6);
		});
		patch(img, top, frame, MACE_HEAD_CAP, (u, v, f) -> (u + v) % 2 == 0 ? rgb(0x1E3A3E) : rgb(0x3A6A6A));
		patch(img, top, frame, MACE_HAFT, (u, v, f) -> v % 9 == 0 ? rgb(0xB8C8D0) : u == 0 ? rgb(0x0A0E12) : rgb(0x1A2028));
		patch(img, top, frame, MACE_BAND, (u, v, f) -> v == 0 ? rgb(0xE8F0F4) : rgb(0x8A9AA4));
		patch(img, top, frame, MACE_SERPENT, (u, v, f) -> (u + f / 2) % 3 == 0 ? rgb(0x8AF0B0) : rgb(0x2E8A5A));
		patch(img, top, frame, MACE_KNOB, (u, v, f) -> rgb(0x3A6A6A));
	}

	// ============================================ The Yellow Dragon Seal (earth) ============================================

	static final double[] SEAL_SIDE = v(0, 0, 7, 7), SEAL_FACE = v(7, 0, 14, 7), SEAL_ROD = v(14, 0, 15, 16), SEAL_COLLAR = v(15, 0, 16, 2);

	static List<LegendArt.Box> seal() {
		List<LegendArt.Box> boxes = new ArrayList<>();
		boxes.add(new LegendArt.Box(v(7.3, -5, 7.3), v(8.7, 11, 8.7), SEAL_ROD, SEAL_ROD, SEAL_ROD));
		boxes.add(new LegendArt.Box(v(6.8, 10, 6.8), v(9.2, 11, 9.2), SEAL_COLLAR, SEAL_COLLAR, SEAL_COLLAR));
		boxes.add(new LegendArt.Box(v(6.8, -6, 6.8), v(9.2, -5, 9.2), SEAL_COLLAR, SEAL_COLLAR, SEAL_COLLAR));
		// The seal: a block of yellow jade, its stamp's face on top.
		boxes.add(new LegendArt.Box(v(4.5, 11, 4.5), v(11.5, 18, 11.5), SEAL_SIDE, SEAL_SIDE, SEAL_FACE));
		return boxes;
	}

	static void paintSeal(BufferedImage img, int frame, int top) {
		double t = frame / (double) FRAMES * Math.PI * 2;
		patch(img, top, frame, SEAL_SIDE, (u, v, f) -> {
			// Yellow jade, the Yellow Dragon carved winding round it (darker where it is cut), a glint running round.
			double body = Math.sin(u * 0.55 + v * 0.35) * 4 + 7 - v;
			boolean dragon = Math.abs(body) < 1.4 && u > 0 && u < 13;
			int c = mix(rgb(0xF0D060), rgb(0xD8A830), BeastArt.hash(u, v, 41) * 0.5);
			if (dragon) c = (u + v) % 2 == 0 ? rgb(0xB07A18) : rgb(0xC88A20);
			if (u == 0 || v == 0 || u == 13 || v == 13) c = rgb(0x9A6A14);
			double glint = Math.pow(Math.max(0, Math.cos(u * 0.45 - t)), 10);
			return mix(c, rgb(0xFFF4C0), glint * 0.5);
		});
		patch(img, top, frame, SEAL_FACE, (u, v, f) -> {
			// The stamp's face: a border, and old seal characters cut into it, glowing faintly.
			boolean border = u <= 1 || v <= 1 || u >= 12 || v >= 12;
			boolean stroke = !border && (u % 4 == 2 && v > 2 && v < 11 || v % 4 == 3 && u > 2 && u < 11 && (u + v) % 3 != 0);
			int c = border ? rgb(0xB07A18) : rgb(0xF0D060);
			return stroke ? mix(rgb(0x8A1A10), rgb(0xFF6A3A), 0.3 + 0.3 * Math.sin(t)) : c;
		});
		patch(img, top, frame, SEAL_ROD, (u, v, f) -> v % 3 == 0 ? rgb(0xB07A18) : u == 0 ? rgb(0x8A5A10) : rgb(0xE8B840));
		patch(img, top, frame, SEAL_COLLAR, (u, v, f) -> v == 0 ? rgb(0xFFE89A) : rgb(0x9A6A14));
	}

	// ============================================ Writing ============================================

	record Piece(String id, List<LegendArt.Box> boxes, LegendArt.Painter painter, double gui) {}

	static List<Piece> pieces() {
		return List.of(new Piece("pangu_axe", axe(), LegendArt3c::paintAxe, 0.8), new Piece("azure_dragon_glaive", glaive(), LegendArt3c::paintGlaive, 0.72),
				new Piece("white_tiger_saber", saber(), LegendArt3c::paintSaber, 0.9), new Piece("vermilion_bird_fan", fan(), LegendArt3c::paintFan, 0.9),
				new Piece("black_tortoise_mace", mace(), LegendArt3c::paintMace, 0.85), new Piece("yellow_dragon_seal", seal(), LegendArt3c::paintSeal, 0.85));
	}

	static void writeAll(Path assets) throws IOException {
		for (Piece piece : pieces()) {
			BeastArt.write(assets.resolve("textures/item/" + piece.id() + ".png"), LegendArt.frames(piece.painter()));
			BeastArt.write(assets.resolve("textures/item/" + piece.id() + ".png.mcmeta"), LegendArt.FLOWING);
			BeastArt.write(assets.resolve("models/item/" + piece.id() + ".json"), LegendArt.model(piece.id(), piece.id(), piece.boxes(), LegendArt.held(piece.gui()), ""));
		}
	}

	private LegendArt3c() {}
}
