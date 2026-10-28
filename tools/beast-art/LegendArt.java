import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The legendary weapons' art (phase 3a of the Artifact Forge, 2026-10-28): Ganjiang and Moye as whole swords in three dimensions
 * (models/item/ganjiang.json, moye.json: a straight jian built of boxes, its blade, stepped point, guard and horns, gem, wrapped grip,
 * pommel and tassel), laid from the lower left (the hand) to the upper right (the point) as a vanilla sword's sprite is, so a held sword's
 * display serves; each painted on a texture of its own (textures/item/ganjiang.png, moye.png: {@link #SIZE} square, {@link #FRAMES}
 * frames) whose blade is alive: Ganjiang's veins of fire flowing up toward the point (yang rises), Moye's veins of cold soul-light sinking
 * toward the hand (yin falls). The Tribulation Spear has no texture: it is a bolt of tribulation, drawn by the client
 * (TribulationSpearRenderer).
 * <p>
 * {@code java BeastArt.java <assets> legends [sheet.png]} writes these; with a sheet, every frame of both big besides, to look them over.
 */
public class LegendArt {
	/** A frame's size (pixels: two to a model pixel, about), and how many frames its blade's flow takes to come round. */
	static final int SIZE = 32, FRAMES = 16;

	/**
	 * A sword's colours: its blade's steel (light, mid, dark), its edges (bright, mid), its fuller (the groove down its middle), its veins
	 * (core, glow); its guard (light, mid, dark), gem (light, dark), grip's wrap (light, dark), pommel (light, dark), tassel (light, dark);
	 * whether its veins flow up toward the point, and how high its guard's horns sit (up for Ganjiang's yang, down for Moye's yin, level for
	 * the Xuanyuan Sword).
	 */
	record Look(String id, int[] steel, int[] edge, int fuller, int[] vein, int[] guard, int[] gem, int[] wrap, int[] pommel, int[] tassel,
			boolean rising, double hornLow) {}

	static int[] c(int... hex) {
		int[] out = new int[hex.length];
		for (int i = 0; i < hex.length; i++) out[i] = BeastArt.rgb(hex[i]);
		return out;
	}

	static final Look GANJIANG = new Look("ganjiang",
			c(0x7A3A26, 0x52201A, 0x2A0E0A), c(0xFFE6A8, 0xD8964A), BeastArt.rgb(0x1E0806), c(0xFFF6C8, 0xFF6A12),
			c(0xF0C060, 0xB07A28, 0x5A3810), c(0xFF8A70, 0xC81810), c(0x8A2018, 0x40080A), c(0xFFE08A, 0xB8862A), c(0xE83A28, 0x8A1010), true, 2.25);
	static final Look MOYE = new Look("moye",
			c(0x4A6280, 0x2E405A, 0x161E2E), c(0xE8FAFF, 0xA8C8DA), BeastArt.rgb(0x0A0E18), c(0xE8FFFF, 0x4AD8EE),
			c(0xE4EAF0, 0xA0AAB6, 0x505A68), c(0x9AB8FF, 0x2440A8), c(0x2A3A68, 0x0E1630), c(0xF0F4F8, 0x8A96A4), c(0x3A4A8A, 0x12142A), false, 0.25);
	/** The Xuanyuan Sword (phase 3b): a golden blade, light flowing up it toward the point, its guard of gold set with jade, its horns level. */
	static final Look XUANYUAN = new Look("xuanyuan_sword",
			c(0xE2C068, 0xB8923A, 0x6A4E16), c(0xFFFCEA, 0xF2DA94), BeastArt.rgb(0x3A2A08), c(0xFFFFF4, 0xFFD44C),
			c(0xFAE8A8, 0xD0A844, 0x6E5218), c(0x9AF0C0, 0x1E7A4A), c(0xD8A830, 0x6A4A10), c(0xFFEAA8, 0xB48C32), c(0xF2C442, 0x9A6A12), true, 1.25);

	// --- The model ---

	/**
	 * One box of the sword, upright about the hand before it is laid over (pixels), its faces' places on the texture (u1 v1 u2 v2, in
	 * sixteenths): its front (the south face, seen in a slot), its back (the north face: the front's own by default), its sides and caps.
	 */
	record Box(double[] from, double[] to, double[] front, double[] side, double[] cap, double[] back) {
		Box(double[] from, double[] to, double[] front, double[] side, double[] cap) {
			this(from, to, front, side, cap, front);
		}
	}

	static double[] v(double... values) { return values; }

	/** Ganjiang's guard's horns turn up (yang); Moye's down (yin). */
	static List<Box> boxes(Look look) {
		List<Box> boxes = new ArrayList<>();
		// The blade, and its point in two steps.
		boxes.add(new Box(v(7, 2.25, 7.75), v(9, 17.75, 8.25), v(0, 0, 3, 16), v(3, 0, 3.5, 16), v(3, 0, 3.5, 0.5)));
		boxes.add(new Box(v(7.5, 17.75, 7.8), v(8.5, 18.75, 8.2), v(1, 0, 2, 0.5), v(3, 0, 3.5, 0.5), v(3, 0, 3.5, 0.5)));
		boxes.add(new Box(v(7.75, 18.75, 7.85), v(8.25, 19.25, 8.15), v(1.25, 0, 1.75, 0.25), v(3, 0, 3.5, 0.25), v(3, 0, 3.5, 0.25)));
		// The guard, its horns, the gem set in it.
		boxes.add(new Box(v(5, 1.25, 7), v(11, 2.25, 9), v(4, 0, 10, 1.5), v(10, 0, 12, 1.5), v(4, 1.5, 10, 3.5)));
		double hornLow = look.hornLow();
		boxes.add(new Box(v(4.5, hornLow, 7.25), v(5.5, hornLow + 1, 8.75), v(12, 0, 13, 1), v(12, 0, 13, 1), v(12, 0, 13, 1)));
		boxes.add(new Box(v(10.5, hornLow, 7.25), v(11.5, hornLow + 1, 8.75), v(12, 0, 13, 1), v(12, 0, 13, 1), v(12, 0, 13, 1)));
		boxes.add(new Box(v(7.5, 1.35, 6.8), v(8.5, 2.15, 9.2), v(13, 0, 14, 1), v(13, 0, 14, 1), v(13, 0, 14, 1)));
		// The grip, the pommel, the tassel.
		boxes.add(new Box(v(7.35, -2.25, 7.35), v(8.65, 1.25, 8.65), v(4, 4, 5.5, 8), v(4, 4, 5.5, 8), v(5.5, 4, 7, 5.5)));
		boxes.add(new Box(v(7, -3.25, 7), v(9, -2.25, 9), v(7, 4, 9, 5), v(7, 4, 9, 5), v(7, 5, 9, 7)));
		boxes.add(new Box(v(7.5, -5.25, 7.75), v(8.5, -3.25, 8.25), v(9, 4, 10, 8), v(9, 4, 9.5, 8), v(9, 7.5, 10, 8)));
		return boxes;
	}

	/**
	 * {@code look}'s item model: its boxes laid over from the lower left to the upper right, held as a sword's sprite is. Its display is a held
	 * sword's (vanilla's item/handheld and item/generated), written out here: a model of boxes can't take item/handheld for its parent, whose
	 * root (item/generated) makes the model from a sprite and drops the boxes (in the first tour both swords were drawn as nothing at all).
	 */
	static String model(Look look) {
		return model(look.id(), look.id(), boxes(look), held(0.92), "");
	}

	/** A held sword's display (vanilla's item/handheld and item/generated, written out), its icon scaled by {@code gui}. */
	static String held(double gui) {
		String g = num(v(gui, gui, gui));
		return "  \"display\": {\n"
				+ "    \"thirdperson_righthand\": {\"rotation\": [0, -90, 55], \"translation\": [0, 4, 0.5], \"scale\": [0.85, 0.85, 0.85]},\n"
				+ "    \"thirdperson_lefthand\": {\"rotation\": [0, 90, -55], \"translation\": [0, 4, 0.5], \"scale\": [0.85, 0.85, 0.85]},\n"
				+ "    \"firstperson_righthand\": {\"rotation\": [0, -90, 25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]},\n"
				+ "    \"firstperson_lefthand\": {\"rotation\": [0, 90, -25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]},\n"
				+ "    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 2, 0], \"scale\": [0.5, 0.5, 0.5]},\n"
				+ "    \"head\": {\"rotation\": [0, 180, 0], \"translation\": [0, 13, 7], \"scale\": [1, 1, 1]},\n"
				+ "    \"fixed\": {\"rotation\": [0, 180, 0], \"translation\": [0, 0, 0], \"scale\": [1, 1, 1]},\n"
				+ "    \"gui\": {\"rotation\": [0, 0, 0], \"translation\": [0, 0, 0], \"scale\": " + g + "}\n"
				+ "  },\n";
	}

	/**
	 * A held bow's display (vanilla's item/bow over item/generated, written out): its sprite's limbs lie on the diagonal from the lower left to
	 * the upper right, its arrow pointing to the upper left, as the Sun-Shooting Bow's boxes are laid.
	 */
	static String bowHeld() {
		return "  \"display\": {\n"
				+ "    \"thirdperson_righthand\": {\"rotation\": [-80, 260, -40], \"translation\": [-1, -2, 2.5], \"scale\": [0.9, 0.9, 0.9]},\n"
				+ "    \"thirdperson_lefthand\": {\"rotation\": [-80, -280, 40], \"translation\": [-1, -2, 2.5], \"scale\": [0.9, 0.9, 0.9]},\n"
				+ "    \"firstperson_righthand\": {\"rotation\": [0, -90, 25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]},\n"
				+ "    \"firstperson_lefthand\": {\"rotation\": [0, 90, -25], \"translation\": [1.13, 3.2, 1.13], \"scale\": [0.68, 0.68, 0.68]},\n"
				+ "    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 2, 0], \"scale\": [0.5, 0.5, 0.5]},\n"
				+ "    \"head\": {\"rotation\": [0, 180, 0], \"translation\": [0, 13, 7], \"scale\": [1, 1, 1]},\n"
				+ "    \"fixed\": {\"rotation\": [0, 180, 0], \"translation\": [0, 0, 0], \"scale\": [1, 1, 1]},\n"
				+ "    \"gui\": {\"rotation\": [0, 0, 0], \"translation\": [0, 0, 0], \"scale\": [0.85, 0.85, 0.85]}\n"
				+ "  },\n";
	}

	/**
	 * An item model of {@code boxes}, laid over from the lower left to the upper right, painted with {@code texture}, shown as {@code display}
	 * says; {@code extra} (its overrides, say) after its elements.
	 */
	static String model(String id, String texture, List<Box> boxes, String display, String extra) {
		StringBuilder out = new StringBuilder();
		out.append("{\n  \"gui_light\": \"front\",\n");
		out.append(display);
		out.append("  \"textures\": {\"blade\": \"defying-the-heavens:item/").append(texture).append("\", \"particle\": \"defying-the-heavens:item/")
				.append(texture).append("\"},\n  \"elements\": [\n");
		for (int i = 0; i < boxes.size(); i++) {
			Box box = boxes.get(i);
			out.append("    {\"from\": ").append(num(box.from())).append(", \"to\": ").append(num(box.to()))
					.append(", \"rotation\": {\"angle\": -45, \"axis\": \"z\", \"origin\": [8, 8, 8]},\n      \"faces\": {");
			String[][] faces = {{"north", "back"}, {"south", "front"}, {"east", "side"}, {"west", "side"}, {"up", "cap"}, {"down", "cap"}};
			for (int f = 0; f < faces.length; f++) {
				double[] uv = switch (faces[f][1]) {
					case "front" -> box.front();
					case "back" -> box.back();
					case "side" -> box.side();
					default -> box.cap();
				};
				out.append(f == 0 ? "" : ", ").append("\"").append(faces[f][0]).append("\": {\"uv\": ").append(num(uv)).append(", \"texture\": \"#blade\"}");
			}
			out.append("}}").append(i < boxes.size() - 1 ? ",\n" : "\n");
		}
		out.append("  ]").append(extra).append("\n}\n");
		return out.toString();
	}

	static String num(double[] values) {
		StringBuilder out = new StringBuilder("[");
		for (int i = 0; i < values.length; i++) {
			double value = values[i];
			out.append(i == 0 ? "" : ", ").append(value == Math.rint(value) ? String.valueOf((long) value) : String.format(Locale.ROOT, "%s", value));
		}
		return out.append("]").toString();
	}

	// --- The texture ---

	/** {@code look}'s texture: every frame one under another (the blade's flow moving on), each {@link #SIZE} square. */
	static BufferedImage texture(Look look) {
		BufferedImage img = new BufferedImage(SIZE, SIZE * FRAMES, BufferedImage.TYPE_INT_ARGB);
		for (int frame = 0; frame < FRAMES; frame++) paint(img, look, frame, frame * SIZE);
		return img;
	}

	/** One frame at {@code top}: the blade's faces and edges flowing, the rest the same in every frame. */
	static void paint(BufferedImage img, Look look, int frame, int top) {
		// The blade's face (6 wide, 32 long: its point at the top), and its edge (1 wide).
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < 6; x++) img.setRGB(x, top + y, bladeFace(look, x, y, frame));
			img.setRGB(6, top + y, bladeEdge(look, y, frame));
		}
		// The guard's front (12 x 3), its top and bottom (12 x 4), its ends (4 x 3).
		for (int x = 0; x < 12; x++) {
			for (int y = 0; y < 3; y++) img.setRGB(8 + x, top + y, guardFront(look, x, y));
			for (int y = 0; y < 4; y++) img.setRGB(8 + x, top + 3 + y, metal(look.guard(), x, y, 12, 4));
		}
		for (int x = 0; x < 4; x++) for (int y = 0; y < 3; y++) img.setRGB(20 + x, top + y, metal(look.guard(), x, y, 4, 3));
		// The horns (2 x 2) and the gem (2 x 2).
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 2; y++) {
				img.setRGB(24 + x, top + y, x + y == 0 ? look.guard()[0] : x + y == 2 ? look.guard()[2] : look.guard()[1]);
				img.setRGB(26 + x, top + y, x == 0 && y == 0 ? BeastArt.mix(look.gem()[0], BeastArt.rgb(0xFFFFFF), 0.5) : x + y == 2 ? look.gem()[1] : look.gem()[0]);
			}
		}
		// The grip's wrap (3 x 8: crossed bands), its ends (3 x 3).
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 8; y++) img.setRGB(8 + x, top + 8 + y, (x + y) % 3 == 0 ? look.wrap()[1] : look.wrap()[0]);
			for (int y = 0; y < 3; y++) img.setRGB(11 + x, top + 8 + y, look.wrap()[1]);
		}
		// The pommel's sides (4 x 2) and ends (4 x 4).
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 2; y++) img.setRGB(14 + x, top + 8 + y, y == 0 ? look.pommel()[0] : look.pommel()[1]);
			for (int y = 0; y < 4; y++) img.setRGB(14 + x, top + 10 + y, (x == 0 || x == 3 || y == 0 || y == 3) ? look.pommel()[1] : look.pommel()[0]);
		}
		// The tassel (2 x 8): its knot, then its threads.
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 8; y++) {
				int c = y < 2 ? look.pommel()[1] : (x + y) % 2 == 0 ? look.tassel()[0] : look.tassel()[1];
				img.setRGB(18 + x, top + 8 + y, y == 7 && x == 1 ? look.tassel()[1] : c);
			}
		}
	}

	/**
	 * The blade's face at column {@code x} (0 and 5 its edges, 1 and 4 their bevels, 2 and 3 its body about the fuller), row {@code y} (0 at
	 * the point): its steel darkening toward the point, a vein of its qi winding along it and flowing with the frames.
	 */
	static int bladeFace(Look look, int x, int y, int frame) {
		double along = y / (double) (SIZE - 1);
		if (x == 0 || x == 5) return BeastArt.mix(look.edge()[0], look.edge()[1], 0.3 + 0.4 * Math.abs(Math.sin(y * 0.7 + frame * 0.4)));
		int steel = BeastArt.mix(look.steel()[0], look.steel()[1], 0.35 + 0.3 * along);
		if (x == 1 || x == 4) return BeastArt.mix(steel, look.edge()[1], 0.25);
		int body = BeastArt.mix(steel, look.steel()[2], 0.35);
		// The fuller down its middle, faint at the point.
		if (y > 3) body = BeastArt.mix(body, look.fuller(), 0.45);
		// The vein: where its winding line passes this column, how bright it burns there.
		double glow = vein(look, x - 2, y, frame);
		return BeastArt.mix(body, glow > 0.6 ? look.vein()[0] : look.vein()[1], Math.min(1, glow * 1.25));
	}

	/**
	 * How brightly the vein shows at column {@code x} (0 or 1 of the fuller's two) and row {@code y}: a line winding across the fuller, its
	 * phase moving {@link #FRAMES} times round a pass along the blade; up toward the point for Ganjiang, down toward the hand for Moye. The
	 * flow's pulses (brighter stretches) move along with it.
	 */
	static double vein(Look look, int x, int y, int frame) {
		double shift = (look.rising() ? 1 : -1) * frame * 2.0; // two pixels a frame: once round the 32 in 16 frames
		double phase = (y + shift) * Math.PI * 2 / SIZE;
		double centre = 0.5 + 0.55 * Math.sin(phase * 3) + 0.25 * Math.sin(phase * 5 + 1.3);
		double near = Math.exp(-Math.pow(x - centre, 2) / 0.35);
		double pulse = 0.55 + 0.45 * Math.sin(phase * 2 + (look.rising() ? 0 : 2));
		return near * pulse;
	}

	/** The blade's edge (seen side on): bright steel, a glint running along it with the flow. */
	static int bladeEdge(Look look, int y, int frame) {
		double shift = (look.rising() ? 1 : -1) * frame * 2.0;
		double glint = Math.max(0, Math.cos((y + shift) * Math.PI * 2 / SIZE * 2));
		return BeastArt.mix(look.edge()[1], look.edge()[0], Math.pow(glint, 4));
	}

	/** The guard's front: its metal shaded, a raised rim, a cloud scroll at each end. */
	static int guardFront(Look look, int x, int y) {
		int base = metal(look.guard(), x, y, 12, 3);
		boolean scroll = (x <= 1 || x >= 10) && y == 1;
		return scroll ? look.guard()[2] : base;
	}

	/** Shaded metal over a {@code w} by {@code h} face: light along its top, dark along its foot and ends. */
	static int metal(int[] shades, int x, int y, int w, int h) {
		if (y == 0) return shades[0];
		if (y == h - 1 || x == 0 || x == w - 1) return shades[2];
		return shades[1];
	}

	// --- The Sea-Fixing Needle (phase 3b) ---

	/** Its iron (light, mid, dark, and the red in its grain), its gold (light, mid, dark), and the glow of the old characters cut down it. */
	static final int[] IRON = c(0x4A3A36, 0x2A1E1C, 0x120C0C, 0x6A1A12), GOLD = c(0xFFF0A8, 0xE8B83A, 0x8A6214), GLYPH = c(0xFFF4C0, 0xFFB43A);

	/** The Needle's boxes, upright about the hand: its iron staff, and a band of gold at each end. */
	static List<Box> needleBoxes() {
		List<Box> boxes = new ArrayList<>();
		boxes.add(new Box(v(7, -5.5, 7), v(9, 21.5, 9), v(0, 0, 4, 16), v(0, 0, 4, 16), v(4, 0, 6, 2)));
		boxes.add(new Box(v(6.5, -7, 6.5), v(9.5, -3.5, 9.5), v(6, 0, 10, 4), v(6, 0, 10, 4), v(10, 0, 14, 4)));
		boxes.add(new Box(v(6.5, 19.5, 6.5), v(9.5, 23, 9.5), v(6, 0, 10, 4), v(6, 0, 10, 4), v(10, 0, 14, 4)));
		return boxes;
	}

	/**
	 * One frame of the Needle's texture at {@code top}: the staff's face (8 by 32: black iron, red in its grain, a column of old characters cut
	 * down its middle, their glow running along it with the frames), its end (4 by 4), the bands' faces (8 by 8: gold, a groove at each edge,
	 * a cloud scroll between, a glint passing) and their ends (8 by 8: rings of gold).
	 */
	static void paintNeedle(BufferedImage img, int frame, int top) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < 8; x++) {
				double grain = BeastArt.hash(x, y / 3, 41);
				int iron = x == 0 || x == 7 ? IRON[2] : x == 1 || x == 6 ? IRON[1] : BeastArt.mix(IRON[1], IRON[0], 0.4 + 0.3 * Math.sin(y * 0.4));
				iron = BeastArt.mix(iron, IRON[3], grain < 0.25 ? 0.45 : 0.12);
				// The characters: short strokes down the middle, a glow passing down them.
				boolean stroke = x >= 3 && x <= 4 && y % 4 != 3 && BeastArt.hash(x, y, 77) < 0.7 || x == 2 && y % 4 == 1 || x == 5 && y % 4 == 2;
				if (stroke && y > 2 && y < SIZE - 3) {
					double glow = Math.pow(Math.max(0, Math.cos((y - frame * 2.0) * Math.PI * 2 / SIZE)), 3);
					iron = BeastArt.mix(BeastArt.mix(IRON[2], GLYPH[1], 0.35), GLYPH[0], glow);
				}
				img.setRGB(x, top + y, iron);
			}
		}
		for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) img.setRGB(8 + x, top + y, x == 0 || y == 0 || x == 3 || y == 3 ? IRON[2] : IRON[1]);
		double glint = (frame % FRAMES) / (double) FRAMES * 12 - 2;
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				int gold = y == 0 || y == 7 ? GOLD[2] : y == 1 || y == 6 ? GOLD[0] : (x + y) % 4 == 0 ? GOLD[2] : GOLD[1];
				if (Math.abs(x + y * 0.5 - glint) < 1) gold = BeastArt.mix(gold, BeastArt.rgb(0xFFFFFF), 0.6);
				img.setRGB(12 + x, top + y, gold);
				double r = Math.hypot(x - 3.5, y - 3.5);
				img.setRGB(20 + x, top + y, r > 3.2 ? GOLD[2] : r > 2.2 ? GOLD[0] : r > 1.2 ? GOLD[1] : GOLD[2]);
			}
		}
	}

	// --- The Sun-Shooting Bow (phase 3b) ---

	/**
	 * Its texture's parts (sixteenths): its limbs' red lacquer, darker sides, gold inlay and tips (dark gold), the grip's wrap, the sun on it,
	 * the string, and the arrow of sun qi (its shaft, head and fletching of fire).
	 */
	static final double[] RED = v(0, 0, 2, 2), RED_SIDE = v(2, 0, 4, 2), INLAY = v(4, 0, 6, 2), INLAY_DARK = v(6, 0, 8, 2), WRAP = v(8, 0, 10, 4),
			SUN = v(10, 0, 13, 3), STRING = v(0, 4, 1, 8), HEAD = v(4, 4, 6, 6), FLETCH = v(6, 4, 8, 6), SHAFT = v(8, 4, 16, 5);
	/** How far the limbs bend back from the grip to their tips (the arc's x = 5 + BEND (y - 8)^2), the tips' height either way, and each draw's pull. */
	static final double BEND = 0.0413, TIP = 10.5;
	static final double[] PULLS = {2.5, 4.5, 6.5};

	/**
	 * The bow's boxes, the arrow's way along -x (the upper left, once laid over), its limbs up and down y: its arc of red lacquer bending back to
	 * gold tips, gold inlay every few pixels, the grip wrapped with a sun on each side, and its string, straight at rest ({@code pull} -1), or
	 * drawn back to {@link #PULLS}[pull] with an arrow of sun qi nocked.
	 */
	static List<Box> bowBoxes(int pull) {
		List<Box> boxes = new ArrayList<>();
		for (int i = -3; i <= 18; i++) {
			double mid = i + 0.5, x = arc(mid);
			boolean tip = i <= -2 || i >= 17;
			double[] face = tip ? INLAY : Math.floorMod(i, 4) == 0 ? INLAY : RED;
			boxes.add(new Box(v(x - 0.75, i, 7.25), v(x + 0.75, i + 1, 8.75), face, tip ? INLAY_DARK : RED_SIDE, tip ? INLAY_DARK : RED_SIDE));
		}
		// The tips curling back toward the string.
		double tipX = arc(-2.5);
		boxes.add(new Box(v(tipX, -3.5, 7.4), v(tipX + 1.6, -2.5, 8.6), INLAY, INLAY_DARK, INLAY_DARK));
		boxes.add(new Box(v(tipX, 18.5, 7.4), v(tipX + 1.6, 19.5, 8.6), INLAY, INLAY_DARK, INLAY_DARK));
		// The grip, wrapped, and a sun on each side of it.
		boxes.add(new Box(v(3.5, 6, 7), v(6.5, 10, 9), WRAP, WRAP, WRAP));
		boxes.add(new Box(v(3.75, 6.75, 6.7), v(6.25, 9.25, 7), SUN, SUN, SUN));
		boxes.add(new Box(v(3.75, 6.75, 9), v(6.25, 9.25, 9.3), SUN, SUN, SUN));
		double stringX = tipX + 1.4;
		if (pull < 0) {
			boxes.add(new Box(v(stringX - 0.2, -2.5, 7.8), v(stringX + 0.2, 18.5, 8.2), STRING, STRING, STRING));
			return boxes;
		}
		double drawn = PULLS[pull];
		for (int i = -3; i <= 17; i++) {
			double mid = i + 0.5, x = stringX + drawn * (1 - Math.abs(mid - 8) / TIP);
			boxes.add(new Box(v(x - 0.2, i + 0.5, 7.8), v(x + 0.2, i + 1.5, 8.2), STRING, STRING, STRING));
		}
		// The arrow, nocked on the string, beside the grip.
		double nock = stringX + drawn;
		boxes.add(new Box(v(nock - 15, 7.75, 8.9), v(nock, 8.25, 9.4), SHAFT, SHAFT, STRING));
		boxes.add(new Box(v(nock - 17, 7.25, 8.65), v(nock - 15, 8.75, 9.65), HEAD, HEAD, HEAD));
		boxes.add(new Box(v(nock - 2.5, 7, 9.05), v(nock - 0.5, 9, 9.25), FLETCH, FLETCH, FLETCH));
		return boxes;
	}

	/** The limbs' arc: how far toward the string (x) it bends back at height {@code y}. */
	static double arc(double y) {
		return 5 + BEND * (y - 8) * (y - 8);
	}

	/** The bow's own model: at rest, drawn by the vanilla bow's three pulls' models as it is drawn ("pulling", "pull": LegendsClient). */
	static String bowModel() {
		String overrides = ",\n  \"overrides\": [\n"
				+ "    {\"predicate\": {\"pulling\": 1}, \"model\": \"defying-the-heavens:item/sun_shooting_bow_pulling_0\"},\n"
				+ "    {\"predicate\": {\"pulling\": 1, \"pull\": 0.65}, \"model\": \"defying-the-heavens:item/sun_shooting_bow_pulling_1\"},\n"
				+ "    {\"predicate\": {\"pulling\": 1, \"pull\": 0.9}, \"model\": \"defying-the-heavens:item/sun_shooting_bow_pulling_2\"}\n"
				+ "  ]";
		return model("sun_shooting_bow", "sun_shooting_bow", bowBoxes(-1), bowHeld(), overrides);
	}

	/** Lacquer red (light, mid, dark), the bow's gold (light, mid, dark), the grip's silk (light, dark), sunfire (white, gold, orange, red). */
	static final int[] LACQUER = c(0xD8402A, 0xA8201A, 0x5A0E0C), BOW_GOLD = c(0xFFF2A8, 0xF0BE40, 0x9A6A14), SILK = c(0x5A1414, 0x2A0808),
			SUNFIRE = c(0xFFFFF0, 0xFFE070, 0xFF9A20, 0xD8401A);

	/** One frame of the bow's texture at {@code top}: the lacquer and gold steady but for a glint, the sun, string and arrow burning with the frames. */
	static void paintBow(BufferedImage img, int frame, int top) {
		double t = frame / (double) FRAMES;
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				int red = BeastArt.mix(LACQUER[0], LACQUER[1], 0.3 + 0.5 * BeastArt.hash(x, y, 13));
				img.setRGB(x, top + y, y == 0 ? BeastArt.mix(red, LACQUER[0], 0.5) : red);
				img.setRGB(4 + x, top + y, BeastArt.mix(LACQUER[1], LACQUER[2], 0.4 + 0.4 * BeastArt.hash(x, y, 17)));
				double glint = Math.max(0, Math.cos((x + y - t * 16) * Math.PI / 4));
				img.setRGB(8 + x, top + y, BeastArt.mix(y == 0 ? BOW_GOLD[0] : BOW_GOLD[1], BeastArt.rgb(0xFFFFFF), Math.pow(glint, 6) * 0.6));
				img.setRGB(12 + x, top + y, BeastArt.mix(BOW_GOLD[1], BOW_GOLD[2], 0.5 + 0.3 * BeastArt.hash(x, y, 19)));
			}
		}
		// The grip: dark red silk wound about it, a thread of gold crossing it.
		for (int x = 0; x < 4; x++) for (int y = 0; y < 8; y++) img.setRGB(16 + x, top + y, (x + y) % 4 == 0 ? BOW_GOLD[1] : (x + y) % 2 == 0 ? SILK[0] : SILK[1]);
		// The sun: a disc of gold, its rays pulsing.
		double pulse = 0.5 + 0.5 * Math.sin(t * Math.PI * 2);
		for (int x = 0; x < 6; x++) {
			for (int y = 0; y < 6; y++) {
				double r = Math.hypot(x - 2.5, y - 2.5);
				int c = r < 1.2 ? SUNFIRE[0] : r < 2.1 ? BeastArt.mix(SUNFIRE[1], SUNFIRE[0], pulse * 0.5) : (x + y) % 2 == 0 ? BeastArt.mix(SUNFIRE[2], SUNFIRE[1], pulse) : BOW_GOLD[2];
				img.setRGB(20 + x, top + y, c);
			}
		}
		// The string: sunfire, its light running along it.
		for (int x = 0; x < 2; x++) {
			for (int y = 0; y < 8; y++) {
				double run = 0.5 + 0.5 * Math.cos((y - t * 8) * Math.PI / 4);
				img.setRGB(x, top + 8 + y, BeastArt.mix(SUNFIRE[2], SUNFIRE[0], run * (x == 0 ? 1 : 0.7)));
			}
		}
		// The arrow's head (a white flame), its fletching (red flame, flickering), its shaft (gold light, a pulse running to the head).
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				double r = Math.hypot(x - 1.5, y - 1.5), flicker = BeastArt.hash(x, y, frame);
				img.setRGB(8 + x, top + 8 + y, r < 1 ? SUNFIRE[0] : BeastArt.mix(SUNFIRE[1], SUNFIRE[0], flicker * 0.6));
				img.setRGB(12 + x, top + 8 + y, BeastArt.mix(SUNFIRE[3], SUNFIRE[2], 0.3 + 0.7 * BeastArt.hash(x, y, frame + 99)));
			}
		}
		for (int x = 0; x < 16; x++) {
			double run = Math.pow(Math.max(0, Math.cos((x + t * 16) * Math.PI / 8)), 4);
			for (int y = 0; y < 2; y++) img.setRGB(16 + x, top + 8 + y, BeastArt.mix(SUNFIRE[1], SUNFIRE[0], 0.3 + 0.7 * run));
		}
	}

	/** {@code painter}'s frames, one under another, each {@link #SIZE} square. */
	static BufferedImage frames(Painter painter) {
		BufferedImage img = new BufferedImage(SIZE, SIZE * FRAMES, BufferedImage.TYPE_INT_ARGB);
		for (int frame = 0; frame < FRAMES; frame++) painter.paint(img, frame, frame * SIZE);
		return img;
	}

	interface Painter {
		void paint(BufferedImage img, int frame, int top);
	}

	// --- The Sea-Fixing Needle's pillar (phase 3b): blocks ---

	/** The pillar's black iron's side (16 square): red in its grain, a seam every eight rows. */
	static BufferedImage pillarIron(boolean end) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.mix(IRON[1], IRON[0], 0.3 + 0.4 * BeastArt.hash(x, end ? y : y / 4, 53));
				c = BeastArt.mix(c, IRON[3], BeastArt.hash(x / 2, y, 61) < 0.2 ? 0.4 : 0.1);
				if (end) {
					double r = Math.hypot(x - 7.5, y - 7.5);
					if (Math.abs(r - 5.5) < 0.6 || Math.abs(r - 2.5) < 0.6) c = IRON[2];
				} else if (y % 8 == 7) {
					c = IRON[2];
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The pillar's gold band's side (16 square): grooves at its edges, a cloud scroll between; its end, rings of gold. */
	static BufferedImage pillarBand(boolean end) {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c;
				if (end) {
					double r = Math.hypot(x - 7.5, y - 7.5);
					c = Math.abs(r - 6) < 0.7 || Math.abs(r - 3) < 0.7 ? GOLD[2] : r < 1.5 ? GOLD[0] : GOLD[1];
				} else {
					boolean groove = y == 1 || y == 14;
					boolean scroll = y >= 5 && y <= 10 && Math.abs(Math.sin((x + (y - 5) * 0.8) * Math.PI / 4) * 2.5 - (y - 7.5)) < 0.8;
					c = groove || scroll ? GOLD[2] : y == 0 || y == 15 ? GOLD[0] : BeastArt.mix(GOLD[1], GOLD[0], 0.2 * BeastArt.hash(x, y, 67));
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	// --- Writing ---

	static final String FLOWING = "{\n  \"animation\": {\"frametime\": 2, \"interpolate\": true}\n}\n";

	static void writeAll(Path assets) throws IOException {
		for (Look look : new Look[] {GANJIANG, MOYE, XUANYUAN}) {
			BeastArt.write(assets.resolve("textures/item/" + look.id() + ".png"), texture(look));
			BeastArt.write(assets.resolve("textures/item/" + look.id() + ".png.mcmeta"), FLOWING);
			BeastArt.write(assets.resolve("models/item/" + look.id() + ".json"), model(look));
		}
		// The Sea-Fixing Needle: held as a sword is, a little smaller in its slot (it is long).
		BeastArt.write(assets.resolve("textures/item/sea_fixing_needle.png"), frames(LegendArt::paintNeedle));
		BeastArt.write(assets.resolve("textures/item/sea_fixing_needle.png.mcmeta"), FLOWING);
		BeastArt.write(assets.resolve("models/item/sea_fixing_needle.json"), model("sea_fixing_needle", "sea_fixing_needle", needleBoxes(), held(0.8), ""));
		// The Sun-Shooting Bow: at rest, and its three pulls.
		BeastArt.write(assets.resolve("textures/item/sun_shooting_bow.png"), frames(LegendArt::paintBow));
		BeastArt.write(assets.resolve("textures/item/sun_shooting_bow.png.mcmeta"), FLOWING);
		BeastArt.write(assets.resolve("models/item/sun_shooting_bow.json"), bowModel());
		for (int pull = 0; pull < PULLS.length; pull++) {
			BeastArt.write(assets.resolve("models/item/sun_shooting_bow_pulling_" + pull + ".json"),
					model("sun_shooting_bow_pulling_" + pull, "sun_shooting_bow", bowBoxes(pull), bowHeld(), ""));
		}
		// The pillar's blocks: black iron, and its bands of gold.
		BeastArt.write(assets.resolve("textures/block/sea_pillar.png"), pillarIron(false));
		BeastArt.write(assets.resolve("textures/block/sea_pillar_top.png"), pillarIron(true));
		BeastArt.write(assets.resolve("textures/block/sea_pillar_band.png"), pillarBand(false));
		BeastArt.write(assets.resolve("textures/block/sea_pillar_band_top.png"), pillarBand(true));
		for (String block : new String[] {"sea_pillar", "sea_pillar_band"}) {
			BeastArt.write(assets.resolve("models/block/" + block + ".json"), "{\n  \"parent\": \"minecraft:block/cube_column\",\n  \"textures\": {\n"
					+ "    \"end\": \"defying-the-heavens:block/" + block + "_top\",\n    \"side\": \"defying-the-heavens:block/" + block + "\"\n  }\n}\n");
		}
		BeastArt.write(assets.resolve("blockstates/sea_pillar.json"), "{\n  \"variants\": {\n"
				+ "    \"banded=false\": {\"model\": \"defying-the-heavens:block/sea_pillar\"},\n"
				+ "    \"banded=true\": {\"model\": \"defying-the-heavens:block/sea_pillar_band\"}\n  }\n}\n");
		LegendArt3c.writeAll(assets); // phase 3c's: Pangu's Axe, the Venerables' five weapons
	}

	/** Every legend's texture, four of its frames, eight times over, side by side: to look them over. */
	static void writeSheet(Path sheet) throws IOException {
		int scale = 8;
		List<BufferedImage> all = new ArrayList<>(List.of(texture(GANJIANG), texture(MOYE), texture(XUANYUAN), frames(LegendArt::paintNeedle), frames(LegendArt::paintBow)));
		for (LegendArt3c.Piece piece : LegendArt3c.pieces()) all.add(frames(piece.painter()));
		BufferedImage[] textures = all.toArray(new BufferedImage[0]);
		BufferedImage out = new BufferedImage((SIZE * scale + 8) * textures.length, SIZE * scale * 4 + 8, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, BeastArt.rgb((x / 8 + y / 8) % 2 == 0 ? 0x8B8B8B : 0x7A7A7A));
		for (int l = 0; l < textures.length; l++) {
			BufferedImage tex = textures[l];
			for (int f = 0; f < 4; f++) {
				int frame = f * 4;
				for (int y = 0; y < SIZE; y++) {
					for (int x = 0; x < SIZE; x++) {
						int c = tex.getRGB(x, frame * SIZE + y);
						if (c >>> 24 == 0) continue;
						for (int dy = 0; dy < scale; dy++) for (int dx = 0; dx < scale; dx++) out.setRGB(l * (SIZE * scale + 8) + x * scale + dx, f * SIZE * scale + y * scale + dy, c);
					}
				}
			}
		}
		BeastArt.write(sheet, out);
	}
}
