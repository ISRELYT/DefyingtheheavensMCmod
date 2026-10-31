import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * The secret realms' art (2026-10-31, SecretRealms): a Seal's gate as it sleeps, stirs and stands open (textures/block/secret_realm_gate_*),
 * its keystone carved with the realm's seal (secret_realm_seal), a killing formation's worn array stone (killing_formation), a trial's barrier
 * of golden light (trial_barrier), the Pill Sage's Cauldron (the alchemy cauldron's own faces, gilded: pill_sage_cauldron_*); the items: the
 * Departure Jade, Ancient Essence, the Spring of the Ancients, the Pill Sage's Notes and a page of them; and the Spring's mark among one's
 * effects (textures/mob_effect/ancient_spring). {@code java BeastArt.java <assets> realms} writes these alone.
 */
public class RealmArt {
	static final int STONE = BeastArt.rgb(0x2E3036), STONE_LIGHT = BeastArt.rgb(0x4A4D55), STONE_DARK = BeastArt.rgb(0x1A1B1F);
	static final int GLYPH = BeastArt.rgb(0x6C7078), JADE = BeastArt.rgb(0x6EF0BE), JADE_CORE = BeastArt.rgb(0xDCFFF0), JADE_DEEP = BeastArt.rgb(0x2A9A78);
	static final int GOLD = BeastArt.rgb(0xF2C14E), GOLD_LIGHT = BeastArt.rgb(0xFFE9A8), GOLD_DARK = BeastArt.rgb(0xA0741E);
	static final int MOSS = BeastArt.rgb(0x5E7A3A), MOSS_DARK = BeastArt.rgb(0x3E5226), GREY = BeastArt.rgb(0x7E7E78), GREY_DARK = BeastArt.rgb(0x5A5A54);
	static final int CORD = BeastArt.rgb(0xC41E1E), PAPER = BeastArt.rgb(0xEDE0BE), PAPER_SHADE = BeastArt.rgb(0xCDB98C), INK = BeastArt.rgb(0x2A2420);
	static final int COVER = BeastArt.rgb(0x2E5E52), COVER_DARK = BeastArt.rgb(0x1A3A32);

	static void writeAll(Path assets) throws IOException {
		Path blocks = assets.resolve("textures/block"), items = assets.resolve("textures/item");
		BeastArt.write(blocks.resolve("secret_realm_gate_sealed.png"), gate(false));
		BeastArt.write(blocks.resolve("secret_realm_gate_stirring.png"), gate(true));
		BeastArt.write(blocks.resolve("secret_realm_gate_open.png"), gateOpen());
		BeastArt.write(blocks.resolve("secret_realm_seal.png"), keystone());
		BeastArt.write(blocks.resolve("killing_formation.png"), arrayStone());
		BeastArt.write(blocks.resolve("trial_barrier.png"), barrier());
		for (String face : new String[] {"side", "top", "bottom", "inner"}) {
			BufferedImage plain = ImageIO.read(blocks.resolve("alchemy_cauldron_" + face + ".png").toFile());
			BeastArt.write(blocks.resolve("pill_sage_cauldron_" + face + ".png"), gild(plain));
		}
		BeastArt.write(items.resolve("departure_jade.png"), jade());
		BeastArt.write(items.resolve("ancient_essence.png"), essence());
		BeastArt.write(items.resolve("spring_of_the_ancients.png"), spring());
		BeastArt.write(items.resolve("pill_sage_notes.png"), notes());
		BeastArt.write(items.resolve("pill_sage_page.png"), page());
		BeastArt.write(assets.resolve("textures/mob_effect/ancient_spring.png"), springMark());
		// The second phase (2026-10-31): the Sunken Sword Tomb's and the Ancient Battlefield's.
		BeastArt.write(blocks.resolve("buried_sword.png"), buriedSword());
		BeastArt.write(items.resolve("buried_sword.png"), buriedSwordItem());
		BeastArt.write(items.resolve("sleeping_sword.png"), sleepingSword());
		BeastArt.write(items.resolve("war_generals_banner.png"), warBanner());
		// The third phase (2026-10-31): the Cave Abode's five element pillars, the Graveyard's essence, the Abode's fruit.
		String[] elements = {"wood", "fire", "earth", "metal", "water"};
		int[] colours = {0x4FB848, 0xE8502A, 0xD8A030, 0xEDE6D2, 0x3C8CE8};
		int[] glows = {0x4FB848, 0xE8502A, 0xD8A030, 0xE0B040, 0x3C8CE8}; // lit, metal burns gold (its qi's glow)
		for (int i = 0; i < elements.length; i++) {
			BeastArt.write(blocks.resolve("element_pillar_" + elements[i] + ".png"), elementPillar(BeastArt.rgb(colours[i]), i, false));
			BeastArt.write(blocks.resolve("element_pillar_" + elements[i] + "_lit.png"), elementPillar(BeastArt.rgb(glows[i]), i, true));
		}
		BeastArt.write(blocks.resolve("element_pillar_top.png"), elementPillarTop());
		BeastArt.write(items.resolve("primeval_blood_essence.png"), bloodEssence());
		BeastArt.write(items.resolve("marrow_washing_fruit.png"), marrowFruit());
		BeastArt.write(assets.resolve("textures/mob_effect/masters_lecture.png"), lectureMark());
		// The fourth phase (2026-10-31): the Mirage's great mirror and its Mirror, the Ruins' formation eye, the Tomb's Law seal; their marks.
		BeastArt.write(blocks.resolve("jade_mirror.png"), mirrorGlass());
		BeastArt.write(blocks.resolve("formation_eye.png"), formationEye());
		BeastArt.write(blocks.resolve("law_seal.png"), lawSeal());
		BeastArt.write(items.resolve("jade_lake_mirror.png"), handMirror());
		BeastArt.write(assets.resolve("textures/mob_effect/mirror_raised.png"), mirrorMark());
		BeastArt.write(assets.resolve("textures/mob_effect/decaying.png"), decayMark());
	}

	/** A colour with its alpha. */
	static int alpha(int rgb, int a) {
		return a << 24 | rgb & 0xFFFFFF;
	}

	/** The great mirror's glass: pale jade silvered, lighter to its top left, two streaks of sheen across it, a little see-through. */
	static BufferedImage mirrorGlass() {
		BufferedImage img = image(16);
		int light = BeastArt.rgb(0xF0FCF8), deep = BeastArt.rgb(0x8EC4BA), sheen = BeastArt.rgb(0xFFFFFF);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double t = (x + y) / 30.0;
				int c = BeastArt.mix(light, deep, 0.15 + 0.7 * t + (BeastArt.hash(x, y, 501) - 0.5) * 0.08);
				int d = x - y;
				if (d == 3 || d == 4 || d == -6) c = BeastArt.mix(c, sheen, d == -6 ? 0.45 : 0.7);
				img.setRGB(x, y, alpha(c, 210));
			}
		}
		return img;
	}

	/** A formation eye: a stone of white jade, its edge darker, a ring of gold about an eye of the formation's light. */
	static BufferedImage formationEye() {
		BufferedImage img = image(16);
		int white = BeastArt.rgb(0xEEF2EC), shade = BeastArt.rgb(0xB8C4BC), edge = BeastArt.rgb(0x8A968E);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double d = Math.hypot(x - 7.5, y - 7.5);
				int c = BeastArt.mix(white, shade, BeastArt.hash(x, y, 511) * 0.35);
				if (x == 0 || y == 0 || x == 15 || y == 15) c = edge;
				else if (Math.abs(d - 5.0) < 0.75) c = BeastArt.mix(GOLD, GOLD_LIGHT, BeastArt.hash(x, y, 512) * 0.4);
				else if (d < 2.4) c = BeastArt.mix(JADE_CORE, JADE, d / 2.4);
				else if (d < 3.4) c = JADE_DEEP;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/**
	 * A Law's seal: a sheet of light, near white so its Law's colour tints it (the client's), a frame about it and lines of script across
	 * it brighter, seen through between them.
	 */
	static BufferedImage lawSeal() {
		BufferedImage img = image(16);
		int sheet = BeastArt.rgb(0xE8E8E8), script = BeastArt.rgb(0xFFFFFF);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				boolean frame = x == 0 || y == 0 || x == 15 || y == 15 || (x == 2 || x == 13) && y >= 2 && y <= 13 || (y == 2 || y == 13) && x >= 2 && x <= 13;
				boolean rune = y >= 4 && y <= 11 && x >= 4 && x <= 11 && (y % 3 != 0) && BeastArt.hash(x, y / 3, 521) < 0.55;
				img.setRGB(x, y, frame ? alpha(script, 235) : rune ? alpha(script, 215) : alpha(sheet, 110));
			}
		}
		return img;
	}

	/** The Jade Lake Mirror: a round hand mirror of silvered jade in a rim of gold, its handle wrapped in red cord below. */
	static BufferedImage handMirror() {
		BufferedImage img = image(16);
		int light = BeastArt.rgb(0xF0FCF8), deep = BeastArt.rgb(0x8EC4BA);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double d = Math.hypot(x - 8.5, y - 6.0);
				if (d < 4.6) {
					int c = BeastArt.mix(light, deep, Math.min(1, (x + y - 6) / 14.0));
					if (x - y == 2 || x - y == 3) c = BeastArt.mix(c, BeastArt.rgb(0xFFFFFF), 0.6);
					img.setRGB(x, y, c);
				} else if (d < 5.9) {
					img.setRGB(x, y, BeastArt.mix(GOLD, GOLD_DARK, d > 5.2 ? 0.5 : 0.0));
				}
			}
		}
		// The handle, down and to the left: gold at its neck, red cord along it, a gold knob.
		for (int k = 0; k < 5; k++) {
			int x = 5 - k, y = 11 + k;
			if (y > 15 || x < 0) break;
			img.setRGB(x, y, k == 0 ? GOLD_DARK : k == 4 ? GOLD : CORD);
			if (x + 1 < 16) img.setRGB(x + 1, y, k == 0 ? GOLD : k == 4 ? GOLD_DARK : BeastArt.mix(CORD, BeastArt.rgb(0x6A0E0E), 0.4));
		}
		return img;
	}

	/** The Jade Lake Mirror raised among one's effects: a round mirror, light streaming back from it. */
	static BufferedImage mirrorMark() {
		BufferedImage img = image(18);
		int light = BeastArt.rgb(0xF0FCF8), deep = BeastArt.rgb(0x8EC4BA);
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 18; y++) {
				double d = Math.hypot(x - 8.5, y - 8.5);
				if (d < 4.2) img.setRGB(x, y, BeastArt.mix(light, deep, Math.min(1, (x + y - 4) / 18.0)));
				else if (d < 5.4) img.setRGB(x, y, GOLD);
				else if (d < 8.4 && (x == 8 || x == 9 || y == 8 || y == 9 || x == y || x + y == 17) && BeastArt.hash(x, y, 531) < 0.8) {
					img.setRGB(x, y, alpha(JADE_CORE, 200));
				}
			}
		}
		return img;
	}

	/** The decays spreading among one's effects: a withered leaf, brown and dull gold, flecks of it falling away. */
	static BufferedImage decayMark() {
		BufferedImage img = image(18);
		int leaf = BeastArt.rgb(0x8A7A4A), dark = BeastArt.rgb(0x4E4024), vein = BeastArt.rgb(0xB8A060);
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 18; y++) {
				// A leaf along the diagonal, pointed at both ends, ragged at its edge where it has withered.
				double along = (x + y - 4) / 26.0, across = (x - y) / 2.0;
				double half = 3.4 * Math.sin(Math.PI * Math.max(0, Math.min(1, along)));
				if (along <= 0 || along >= 1 || Math.abs(across) > half) continue;
				if (Math.abs(across) > half - 1 && BeastArt.hash(x, y, 541) < 0.35) continue;
				img.setRGB(x, y, Math.abs(across) < 0.6 ? vein : BeastArt.mix(leaf, dark, BeastArt.hash(x, y, 542) * 0.6));
			}
		}
		for (int[] fleck : new int[][] {{14, 4}, {15, 7}, {12, 2}}) img.setRGB(fleck[0], fleck[1], dark);
		return img;
	}

	/** The master's lecture among one's effects: a scroll unrolled, gold rollers at its ends, columns of script down it. */
	static BufferedImage lectureMark() {
		BufferedImage img = image(18);
		for (int x = 2; x <= 15; x++) {
			for (int y = 4; y <= 13; y++) {
				int c;
				if (x <= 3 || x >= 14) {
					c = y == 4 || y == 13 ? GOLD_DARK : BeastArt.mix(GOLD, GOLD_LIGHT, x == 3 || x == 14 ? 0.5 : 0.1);
				} else {
					c = BeastArt.mix(PAPER, PAPER_SHADE, BeastArt.hash(x, y, 91) * 0.3);
					if (x % 2 == 1 && x >= 5 && x <= 12 && y >= 6 && y <= 11 && BeastArt.hash(x, y, 93) < 0.75) c = INK;
				}
				img.setRGB(x, y, c);
			}
		}
		for (int x : new int[] {2, 3, 14, 15}) {
			img.setRGB(x, 3, GOLD_DARK);
			img.setRGB(x, 14, GOLD_DARK);
		}
		return img;
	}

	/** Whether (x, y) of a 16 face lies on element {@code e}'s mark (wood, fire, earth, metal, water: its own simple glyph in the band). */
	static boolean elementMark(int x, int y, int e) {
		double dx = x - 7.5, dy = y - 7.5;
		return switch (e) {
			case 0 -> Math.hypot(dx, dy + 1.4) < 2.9 || Math.abs(dx) < 0.6 && dy > 0.5 && dy < 3.8; // a tree: its crown, its trunk
			case 1 -> dy > -3.8 && dy < 3.6 && Math.abs(dx) < 3.1 * Math.pow(Math.sin(Math.PI * (dy + 3.8) / 7.6), 0.75); // a flame, pointed above
			case 2 -> Math.abs(dy) < 0.6 && Math.abs(dx) < 3.6 || Math.abs(dy - 2.6) < 0.6 && Math.abs(dx) < 2.6 || Math.abs(dy + 2.6) < 0.6 && Math.abs(dx) < 1.6; // strata
			case 3 -> Math.abs(Math.abs(dx) + Math.abs(dy) - 3.2) < 0.6; // a diamond, edged
			default -> Math.abs(Math.sin(dx * 1.1) * 1.4 - dy + 1.5) < 0.6 && Math.abs(dx) < 3.6 || Math.abs(Math.sin(dx * 1.1) * 1.4 - dy - 1.5) < 0.6 && Math.abs(dx) < 3.6; // waves
		};
	}

	/**
	 * An element pillar's side (the Cave Abode's trial): carved pale stone, fluted, a dark band about its middle with its element's glyph inlaid
	 * in its colour, rims above and below. Lit, its glyph burns white at its heart into its colour, the band about it tinted with its light.
	 */
	static BufferedImage elementPillar(int colour, int element, boolean lit) {
		BufferedImage img = image(16);
		int pale = BeastArt.rgb(0xB8B4AA), paleDark = BeastArt.rgb(0x8C887E);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.shade(pale, (BeastArt.hash(x, y, 71) - 0.5) * 0.12);
				if (x == 2 || x == 13) c = paleDark; // its flutes
				if (y <= 1 || y >= 14) c = BeastArt.mix(pale, BeastArt.rgb(0xDAD6CC), y == 0 || y == 15 ? 0.6 : 0.2);
				boolean band = y >= 3 && y <= 12 && x >= 3 && x <= 12;
				if (band) {
					c = BeastArt.shade(BeastArt.rgb(0x3A3834), (BeastArt.hash(x, y, 73) - 0.5) * 0.15); // its band
					if (lit) c = BeastArt.mix(c, colour, 0.35);
				}
				if (band && elementMark(x, y, element)) {
					c = lit ? BeastArt.mix(BeastArt.rgb(0xFFFFFF), colour, Math.min(1, Math.hypot(x - 7.5, y - 7.5) / 4.5) * 0.7)
							: BeastArt.mix(colour, BeastArt.rgb(0xFFFFFF), BeastArt.hash(x, y, 75) * 0.25);
				}
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** An element pillar's top and bottom: pale stone, a ring cut in it. */
	static BufferedImage elementPillarTop() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double r = Math.hypot(x - 7.5, y - 7.5);
				int c = BeastArt.shade(BeastArt.rgb(0xB8B4AA), (BeastArt.hash(x, y, 77) - 0.5) * 0.12);
				if (Math.abs(r - 5) < 0.7) c = BeastArt.rgb(0x8C887E);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The Primeval Blood Essence: a long crystal phial stoppered in gold, its blood dark red with a gold light in it, glowing at its heart. */
	static BufferedImage bloodEssence() {
		BufferedImage img = image(16);
		int glass = BeastArt.rgb(0xCFE8F0), blood = BeastArt.rgb(0x7A0E14), bloodLight = BeastArt.rgb(0xC8282C);
		for (int y = 3; y <= 14; y++) {
			int half = y < 6 ? 1 : 3;
			for (int x = 8 - half; x <= 7 + half; x++) {
				boolean edge = x == 8 - half || x == 7 + half || y == 14;
				int c = edge ? BeastArt.mix(glass, BeastArt.rgb(0x88A4B0), 0.4) : y < 7 ? glass : BeastArt.mix(blood, bloodLight, BeastArt.hash(x, y, 81) * 0.5);
				if (!edge && y >= 9 && y <= 11 && x >= 7 && x <= 8) c = GOLD_LIGHT; // its light
				img.setRGB(x, y, c);
			}
		}
		for (int x = 6; x <= 9; x++) for (int y = 1; y <= 2; y++) img.setRGB(x, y, y == 1 ? GOLD_LIGHT : GOLD);
		return img;
	}

	/** The Marrow-Washing Fruit: a pale peach, white flushed with gold, a light about it, two jade leaves at its stem. */
	static BufferedImage marrowFruit() {
		BufferedImage img = image(16);
		int white = BeastArt.rgb(0xF6F2E6), flush = BeastArt.rgb(0xF2C870), shadow = BeastArt.rgb(0xC8B48A);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double dx = (x - 7.5) / 5.6, dy = (y - 9) / 5.4, r = Math.sqrt(dx * dx + dy * dy);
				if (r > 1) continue;
				int c = BeastArt.mix(white, flush, Math.max(0, 0.6 - dy * 0.5 + dx * 0.2) * 0.7);
				if (r > 0.8) c = BeastArt.mix(c, shadow, 0.5);
				if (Math.abs(dx + 0.05) < 0.07 && dy < -0.2) c = BeastArt.mix(c, shadow, 0.6); // its seam
				img.setRGB(x, y, c);
			}
		}
		img.setRGB(7, 3, BeastArt.rgb(0x5A3C22));
		img.setRGB(7, 2, BeastArt.rgb(0x5A3C22));
		for (int[] leaf : new int[][] {{8, 2}, {9, 2}, {10, 3}, {5, 3}, {6, 2}}) img.setRGB(leaf[0], leaf[1], BeastArt.mix(JADE_DEEP, JADE, BeastArt.hash(leaf[0], leaf[1], 83)));
		return img;
	}

	static final int STEEL = BeastArt.rgb(0x9AA2AE), STEEL_LIGHT = BeastArt.rgb(0xD8DEE8), STEEL_DARK = BeastArt.rgb(0x5C626C), RUST = BeastArt.rgb(0x8A4A24);
	static final int BRONZE = BeastArt.rgb(0xA8743A), BRONZE_DARK = BeastArt.rgb(0x6A4420), WRAP = BeastArt.rgb(0x2A2E36), WRAP_LIGHT = BeastArt.rgb(0x4A5260);
	static final int SPIRIT = BeastArt.rgb(0xBFE6FF), SPIRIT_CORE = BeastArt.rgb(0xF4FBFF), RED = BeastArt.rgb(0xB4232A), RED_DARK = BeastArt.rgb(0x6E1218);
	static final int POLE = BeastArt.rgb(0x5A3C22), POLE_DARK = BeastArt.rgb(0x3A2614);

	/**
	 * A buried sword's faces, laid out for its models (models/block/buried_sword*): its blade's two faces (columns 0 and 1, rows 0 to 10 from the
	 * guard down to the ground: steel, a bright ridge, rust and dirt toward the ground) and edge (column 2), its broken end (row 12), its guard
	 * (4 to 8, rows 0 and 1, bronze), grip (10, rows 0 to 2, wound dark), pommel (12 and 13).
	 */
	static BufferedImage buriedSword() {
		BufferedImage img = image(16);
		for (int y = 0; y <= 10; y++) {
			double ground = Math.max(0, (y - 6) / 4.0);
			for (int x = 0; x <= 2; x++) {
				int c = x == 0 ? STEEL : x == 1 ? STEEL_LIGHT : BeastArt.mix(STEEL, STEEL_LIGHT, 0.6);
				c = BeastArt.shade(c, (BeastArt.hash(x, y, 51) - 0.5) * 0.12);
				if (BeastArt.hash(x, y, 53) < ground * 0.8) c = BeastArt.mix(c, RUST, 0.5 + 0.3 * BeastArt.hash(x, y, 55));
				img.setRGB(x, y, c);
			}
		}
		img.setRGB(0, 12, STEEL_DARK);
		img.setRGB(1, 12, STEEL_LIGHT);
		for (int x = 4; x <= 8; x++) {
			img.setRGB(x, 0, BeastArt.mix(BRONZE, BRONZE_DARK, x == 4 || x == 8 ? 0.4 : BeastArt.hash(x, 0, 57) * 0.3));
			img.setRGB(x, 1, BeastArt.mix(BRONZE, BRONZE_DARK, 0.25));
		}
		for (int y = 0; y <= 2; y++) img.setRGB(10, y, y == 1 ? WRAP_LIGHT : WRAP);
		for (int x = 12; x <= 13; x++) for (int y = 0; y <= 2; y++) img.setRGB(x, y, BeastArt.mix(BRONZE, BRONZE_DARK, y == 0 ? 0.1 : 0.35));
		return img;
	}

	/** A broken sword driven into a mound of grey earth, as it lies on the tomb's floor (its item). */
	static BufferedImage buriedSwordItem() {
		BufferedImage img = image(16);
		for (int y = 3; y <= 12; y++) {
			img.setRGB(7, y, STEEL);
			img.setRGB(8, y, y < 5 ? STEEL : STEEL_LIGHT);
		}
		img.setRGB(8, 2, STEEL_LIGHT);
		for (int y = 9; y <= 12; y++) if (BeastArt.hash(7, y, 59) < 0.6) img.setRGB(7, y, RUST);
		for (int x = 2; x <= 13; x++) {
			int top = 12 + (int) Math.round(Math.abs(x - 7.5) / 3.0);
			for (int y = top; y <= 15; y++) img.setRGB(x, y, BeastArt.mix(GREY_DARK, GREY, BeastArt.hash(x, y, 61)));
		}
		return img;
	}

	/** The Sleeping Sword: a jian corner to corner, its blade bright with spirit-light along its edges, a gilded guard, a red tassel. */
	static BufferedImage sleepingSword() {
		BufferedImage img = image(16);
		// The blade from (6, 9) up to its point at (14, 1); the guard across at (5, 10); the grip and pommel down to (2, 13); the tassel below.
		for (int i = 0; i <= 8; i++) {
			int x = 6 + i, y = 9 - i;
			img.setRGB(x, y, i == 8 ? SPIRIT_CORE : STEEL_LIGHT);
			if (i < 8) {
				img.setRGB(x + 1, y, SPIRIT);
				img.setRGB(x, y - 1, i < 7 ? STEEL : SPIRIT);
			}
		}
		for (int k = -2; k <= 2; k++) img.setRGB(5 + k, 10 + k, k == 0 ? GOLD_LIGHT : GOLD);
		img.setRGB(4, 11, WRAP_LIGHT);
		img.setRGB(3, 12, WRAP);
		img.setRGB(2, 13, GOLD_DARK);
		img.setRGB(1, 14, RED);
		img.setRGB(2, 14, RED_DARK);
		img.setRGB(1, 15, RED_DARK);
		return img;
	}

	/** The War General's Banner: a war banner of deep red on a dark pole, bordered in gold, a gold sun on it, black tassels below. */
	static BufferedImage warBanner() {
		BufferedImage img = image(16);
		for (int y = 0; y <= 15; y++) img.setRGB(3, y, y == 0 ? GOLD : BeastArt.mix(POLE, POLE_DARK, BeastArt.hash(3, y, 63) * 0.5));
		for (int x = 4; x <= 13; x++) img.setRGB(x, 1, POLE);
		for (int x = 4; x <= 13; x++) {
			int bottom = 11 + (x % 3 == 1 ? 1 : 0);
			for (int y = 2; y <= bottom; y++) {
				boolean edge = x == 4 || x == 13 || y == 2 || y == bottom;
				double dx = x - 8.5, dy = y - 6.5, r = Math.sqrt(dx * dx + dy * dy);
				int c = edge ? GOLD : r < 1.8 ? GOLD_LIGHT : r < 2.6 && (x + y) % 2 == 0 ? GOLD : BeastArt.mix(RED, RED_DARK, BeastArt.hash(x, y, 65) * 0.4);
				img.setRGB(x, y, c);
			}
		}
		for (int x = 5; x <= 12; x += 3) for (int y = 13; y <= 14; y++) img.setRGB(x, y, y == 14 ? STONE_DARK : STONE);
		return img;
	}

	private static BufferedImage image(int size) {
		return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	}

	/** Whether (x, y) of a 16 face lies on its seal: a ring about its middle, a cross of strokes within, four dots at its corners. */
	static boolean seal(int x, int y) {
		double dx = x - 7.5, dy = y - 7.5, r = Math.sqrt(dx * dx + dy * dy);
		if (Math.abs(r - 5.6) < 0.6) return true;
		if (r < 4.4 && (Math.abs(dx) < 0.6 || Math.abs(dy) < 0.6)) return true;
		if (r < 3.4 && Math.abs(Math.abs(dx) - Math.abs(dy)) < 0.5) return true;
		return (x == 2 || x == 13) && (y == 2 || y == 13);
	}

	/** The gate asleep: dark stone, its seal carved faint in it; stirring: the seal and cracks from it lit jade. */
	static BufferedImage gate(boolean stirring) {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.shade(STONE, (BeastArt.hash(x, y, 31) - 0.5) * 0.18);
				if (BeastArt.hash(x / 2, y / 2, 33) > 0.82) c = BeastArt.mix(c, STONE_LIGHT, 0.5);
				boolean mark = seal(x, y);
				boolean crack = stirring && !mark && BeastArt.hash(x + y * 3, (x - y) & 7, 35) > 0.9;
				if (mark) c = stirring ? BeastArt.mix(JADE, JADE_CORE, BeastArt.hash(x, y, 37) * 0.5) : GLYPH;
				else if (crack) c = BeastArt.mix(JADE_DEEP, JADE, 0.5);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The gate open: a sheet of light, gold at its heart, swirling, seen through at its edges. */
	static BufferedImage gateOpen() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double dx = x - 7.5, dy = y - 7.5, r = Math.sqrt(dx * dx + dy * dy) / 7.5;
				double swirl = Math.sin(Math.atan2(dy, dx) * 3 + r * 6) * 0.5 + 0.5;
				int c = BeastArt.mix(GOLD_LIGHT, GOLD, swirl * 0.6 + r * 0.3);
				int a = (int) Math.round(150 + 90 * (1 - Math.min(1, r)) * swirl + 15 * BeastArt.hash(x, y, 41));
				img.setRGB(x, y, Math.min(255, a) << 24 | c & 0xFFFFFF);
			}
		}
		return img;
	}

	/** The keystone: dark polished stone in a frame, the realm's seal on it in gold. */
	static BufferedImage keystone() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.shade(STONE_DARK, (BeastArt.hash(x, y, 43) - 0.5) * 0.14);
				if (x == 0 || y == 0 || x == 15 || y == 15) c = STONE_LIGHT;
				if (seal(x, y)) c = BeastArt.mix(GOLD, GOLD_LIGHT, BeastArt.hash(x, y, 45) * 0.6);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** A killing formation's eye: mossy grey stone, its array's script worn almost away (pale lines, faint). */
	static BufferedImage arrayStone() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				int c = BeastArt.shade(GREY, (BeastArt.hash(x, y, 51) - 0.5) * 0.2);
				if (BeastArt.hash(x / 3, y / 3, 53) > 0.6) c = BeastArt.mix(c, MOSS, 0.6);
				if (BeastArt.hash(x, y, 55) > 0.85) c = MOSS_DARK;
				if (seal(x, y) && BeastArt.hash(x, y, 57) > 0.3) c = BeastArt.mix(c, BeastArt.rgb(0xC8D4CC), 0.35);
				if (x == 0 || y == 0 || x == 15 || y == 15) c = BeastArt.mix(c, GREY_DARK, 0.6);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** A trial's barrier: golden light in a lattice, seen through between its bars. */
	static BufferedImage barrier() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				boolean bar = (x + y) % 8 == 0 || (x - y + 16) % 8 == 0;
				int c = bar ? GOLD_LIGHT : GOLD;
				int a = bar ? 200 : 96 + (int) (40 * BeastArt.hash(x, y, 61));
				img.setRGB(x, y, a << 24 | c & 0xFFFFFF);
			}
		}
		return img;
	}

	/** The alchemy cauldron's face gilded: its bronze turned gold, its light parts brighter. */
	static BufferedImage gild(BufferedImage plain) {
		BufferedImage img = new BufferedImage(plain.getWidth(), plain.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int x = 0; x < plain.getWidth(); x++) {
			for (int y = 0; y < plain.getHeight(); y++) {
				int p = plain.getRGB(x, y), a = p >>> 24;
				if (a == 0) continue;
				double lum = ((p >> 16 & 255) * 0.3 + (p >> 8 & 255) * 0.55 + (p & 255) * 0.15) / 255.0;
				int c = lum < 0.5 ? BeastArt.mix(GOLD_DARK, GOLD, lum * 2) : BeastArt.mix(GOLD, GOLD_LIGHT, (lum - 0.5) * 2);
				img.setRGB(x, y, a << 24 | c & 0xFFFFFF);
			}
		}
		return img;
	}

	/** A Departure Jade: a round pendant of pale jade, a hole through it, a red cord up from it. */
	static BufferedImage jade() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double dx = x - 7.5, dy = y - 9, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 5.6 && r > 1.4) {
					c = BeastArt.mix(BeastArt.rgb(0xB8F0D0), JADE_DEEP, (dx + dy) / 14 + 0.45);
					if (r > 4.9) c = BeastArt.shade(c, -0.25);
					if (Math.abs(r - 3.3) < 0.4) c = BeastArt.shade(c, 0.25);
				}
				if (x == 7 && y <= 3 || x == 8 && y >= 1 && y <= 3) c = CORD;
				if (y == 1 && (x == 6 || x == 9)) c = CORD;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** Ancient Essence: a bead of gold light, brightest at its heart, a faint halo round it. */
	static BufferedImage essence() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double dx = x - 7.5, dy = y - 7.5, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 4.2) c = BeastArt.mix(GOLD_LIGHT, GOLD, r / 4.2);
				else if (r < 6.6 && (x + y) % 2 == 0) c = (int) Math.round(120 * (1 - (r - 4.2) / 2.4)) << 24 | GOLD_LIGHT & 0xFFFFFF;
				if (r < 1.5) c = BeastArt.rgb(0xFFFFF4);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The Spring of the Ancients: a round glass bottle of jade-clear water, a cork in it, a glint. */
	static BufferedImage spring() {
		BufferedImage img = image(16);
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				double dx = x - 7.5, dy = y - 10, r = Math.sqrt(dx * dx + dy * dy);
				int c = 0;
				if (r < 5) c = r > 4.2 ? BeastArt.rgb(0xD8F4F0) : BeastArt.mix(BeastArt.rgb(0x7FF0D8), BeastArt.rgb(0x2A9A90), (dy + 5) / 10);
				if (y >= 2 && y <= 5 && x >= 6 && x <= 9) c = x == 6 || x == 9 ? BeastArt.rgb(0xD8F4F0) : BeastArt.rgb(0x9CF4E4);
				if (y <= 2 && x >= 6 && x <= 9) c = BeastArt.rgb(0x8A6A3E);
				if (x == 5 && (y == 8 || y == 9)) c = BeastArt.rgb(0xFFFFFF);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The Pill Sage's Notes: a bound book, its cover jade-green, a pill and its gold rim on it, its pages' edge pale. */
	static BufferedImage notes() {
		BufferedImage img = image(16);
		for (int x = 2; x <= 13; x++) {
			for (int y = 1; y <= 14; y++) {
				int c = BeastArt.shade(COVER, (BeastArt.hash(x, y, 71) - 0.5) * 0.12);
				if (x == 2 || x == 3) c = COVER_DARK; // its spine
				if (y == 14 || x == 13) c = PAPER_SHADE;
				if (x == 4 && y % 3 == 1) c = GOLD; // its binding's threads
				double dx = x - 8.5, dy = y - 7, r = Math.sqrt(dx * dx + dy * dy);
				if (r < 2.4) c = BeastArt.mix(GOLD_LIGHT, GOLD, r / 2.4);
				else if (r < 3.1) c = GOLD_DARK;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** A page of the Notes: yellowed paper, two columns of ink, a small drawn pill. */
	static BufferedImage page() {
		BufferedImage img = image(16);
		for (int x = 3; x <= 12; x++) {
			for (int y = 1; y <= 14; y++) {
				int c = BeastArt.shade(PAPER, (BeastArt.hash(x, y, 81) - 0.5) * 0.1);
				if (x == 3 || x == 12 || y == 1 || y == 14) c = PAPER_SHADE;
				if ((x == 5 || x == 7) && y >= 3 && y <= 12 && BeastArt.hash(x, y, 83) > 0.3) c = INK;
				double dx = x - 10, dy = y - 5, r = Math.sqrt(dx * dx + dy * dy);
				if (r < 1.5) c = GOLD;
				img.setRGB(x, y, c);
			}
		}
		return img;
	}

	/** The Spring's mark among one's effects (18 x 18): a drop of jade-clear water over rings spreading. */
	static BufferedImage springMark() {
		BufferedImage img = image(18);
		for (int x = 0; x < 18; x++) {
			for (int y = 0; y < 18; y++) {
				int c = 0;
				double dx = x - 8.5, dy = y - 7;
				// The drop: round below, pointed above.
				boolean drop = dy >= 0 ? dx * dx + dy * dy < 16 : Math.abs(dx) < (4 + dy) * 0.9 && dy > -6;
				if (drop) c = BeastArt.mix(BeastArt.rgb(0xB0FFE8), BeastArt.rgb(0x2A9A90), (dy + 6) / 10);
				double rr = Math.sqrt(dx * dx / 4 + (y - 14.5) * (y - 14.5));
				if (!drop && Math.abs(rr - 2.4) < 0.5) c = BeastArt.rgb(0x7FE3C4);
				img.setRGB(x, y, c);
			}
		}
		return img;
	}
}
