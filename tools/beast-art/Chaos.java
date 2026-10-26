import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The Chaos Frontier's for BeastArt (phase 3c, 2026-10-26): the Chaos Fiend (geo/demon_beast/chaos_fiend.geo.json) and the Primordial
 * Chaos (primordial_chaos), each its texture, glow mask and "idle" animation as Bodies writes the wild beasts'; and the Frontier's items and
 * blocks (16 x 16): Chaos Origin Ore, the Chaos Origin Stone, the Dao Fragment (its shard, and its light on a layer of its own that the game
 * tints its Law's colour) and the First Flame (a strip of frames, its tongues white-gold, animated).
 * <p>
 * The bones the client moves (BeastModels): the fiend's "head", "arm_l"/"_r", "smoke_0".."smoke_2", "orbit" (the shards circling it); the
 * Primordial Chaos's "body" (its breath keyed here), "wing_l"/"_r" and "wing_l2"/"_r2", "leg_0".."leg_5", "maw". Neither has lids: the
 * fiend's eyes are fire that never shuts, and the Primordial Chaos has no face. Their "idle" is their breath alone (no lids keyed: they have
 * none).
 * <p>
 * Run: {@code java BeastArt.java <assets> chaos}.
 */
public class Chaos {
	static int c(int hex) { return Bodies.c(hex); }

	static int glow(int hex) { return Bodies.glow(hex); }

	// --- Painters ---

	/** Smoke: dark grey-violet, uneven, lighter wisps curling across it. */
	static BeastArt.Painter smoke(Bodies.Coat k, int seed) {
		return (f, x, y, w, h, d) -> {
			double n = BeastArt.hash(x / 2 + f.ordinal() * 13, y / 2, seed) * 0.6 + BeastArt.hash(x, y, seed + 1) * 0.4;
			double wave = Math.sin((x + f.ordinal() * 5) * 0.8 + y * 0.5 + seed) * 0.5 + 0.5;
			int col = BeastArt.mix(k.dark(), k.base(), 0.35 + n * 0.65);
			if (wave > 0.84) col = BeastArt.mix(col, k.light(), 0.5);
			return col;
		};
	}

	/** Broken stone, cracked, the chaos glowing violet through some of its cracks. */
	static BeastArt.Painter shardStone(int stone, int vein, int seed) {
		BeastArt.Painter rock = Bodies.rock(stone, BeastArt.shade(stone, -0.4), 0, seed);
		return (f, x, y, w, h, d) -> {
			int px = x + f.ordinal() * 29;
			boolean crack = BeastArt.hash(px, y, seed + 7) < 0.12 && BeastArt.hash(px / 2, y / 2 + 1, seed + 9) < 0.6;
			return crack ? glow(vein) : rock.paint(f, x, y, w, h, d);
		};
	}

	/** Just its breath: the body swelling a little and back (no lids to blink). */
	static List<BeastArt.Anim> breath(double length, double by) {
		BeastArt.Anim idle = new BeastArt.Anim("idle", length, true);
		for (int i = 0; i <= 4; i++) idle.key("body", "scale", i * length / 4, i % 2 == 1 ? 1 + by * 0.4 : 1, i % 2 == 1 ? 1 + by : 1, i % 2 == 1 ? 1 + by * 0.4 : 1);
		List<BeastArt.Anim> anims = new ArrayList<>();
		anims.add(idle);
		return anims;
	}

	// --- The Chaos Fiend ---

	static final Bodies.Coat FIEND = new Bodies.Coat(c(0x4A3E5A), c(0x231B2C), c(0x8A7A9C), c(0x3A3046), c(0xF6F2FF), c(0xB07CFF), c(0xE6DED0));

	/**
	 * A Chaos Fiend: a hunched wraith of grey-violet smoke about a core of broken stone, plates of stone on its shoulders and breast (the
	 * chaos glowing through their cracks); a hood of smoke, dark within, two eyes of white fire in it; long arms of smoke, bracers of stone,
	 * three pale claws on each, their points glowing; its smoke trailing down beneath it to nothing; three shards of stone circling it.
	 */
	static void chaosFiend(BeastArt.Model m) {
		Bodies.Coat k = FIEND;
		BeastArt.Painter smoke = smoke(k, 900);
		BeastArt.Painter stone = shardStone(c(0x5A5462), k.glowing(), 901);
		BeastArt.Bone body = m.bone("body", null, 0, 22, 0, 8, 0, 0);
		Bodies.cc(body, 0, 16, 0, 9, 12, 6, smoke);
		Bodies.cc(body, 0, 20.5, -3.4, 5, 5, 1.2, stone); // a plate of stone on its breast
		Bodies.cc(body, 4.8, 24.5, 0, 3, 3.5, 5, stone);
		Bodies.cc(body, -4.8, 24.5, 0, 3, 3.5, 5, stone);
		// Its head: the dark within its hood, two eyes of white fire (their fire rising off them over a row of violet), a jag of stone above.
		BeastArt.Bone head = m.bone("head", null, 0, 28, -1);
		BeastArt.Painter face = (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.FRONT) {
				boolean eyeCol = x == 1 || x == 2 || x == w - 2 || x == w - 3;
				if (eyeCol && y == 3) return glow(x == 2 || x == w - 3 ? k.eye() : BeastArt.mix(k.eye(), k.glowing(), 0.3));
				if (eyeCol && y == 2) return glow(BeastArt.mix(k.eye(), k.glowing(), 0.65));
				if (x >= 1 && x <= w - 2 && y >= 1 && y <= h - 2) return BeastArt.mix(Bodies.BLACK, k.dark(), 0.3);
			}
			return smoke.paint(f, x, y, w, h, d);
		};
		Bodies.cc(head, 0, 28, -1.5, 7, 7, 7, face);
		Bodies.cc(head, 0, 34, 0.5, 8, 2.5, 7, smoke); // the hood's crown, hanging over its brow
		Bodies.cc(head, 0, 35.5, 1.5, 2, 3, 2, stone);
		// Its long arms, a bracer of stone on each, three claws, pale bone, their points glowing.
		BeastArt.Painter claw = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN || y >= h - 1 ? glow(k.glowing()) : Bodies.noise(k.horn(), f, x, y, 905, 0.1);
		for (int side : new int[] {1, -1}) {
			String n = side > 0 ? "arm_l" : "arm_r";
			BeastArt.Bone arm = m.bone(n, null, side * 5.5, 27, -0.5);
			arm.cube(side > 0 ? 4.4 : -6.6, 14, -1.6, 2.2, 13, 2.2, smoke);
			arm.cube(side > 0 ? 4.1 : -6.9, 16.5, -1.9, 2.8, 3, 2.8, stone);
			for (int i = 0; i < 3; i++) arm.cube((side > 0 ? 4.4 : -6.6) + i * 0.75, 10, i == 1 ? -2.6 : -2.2, 0.7, 4, 0.7, claw);
		}
		// Its smoke trailing down beneath it, thinning to nothing.
		BeastArt.Bone s0 = m.bone("smoke_0", null, 0, 16, 0.5);
		Bodies.cc(s0, 0, 9, 0.5, 6, 7, 5, smoke);
		BeastArt.Bone s1 = m.bone("smoke_1", "smoke_0", 0, 9, 1);
		Bodies.cc(s1, 0, 3.5, 1, 4, 5.5, 3.5, smoke);
		BeastArt.Bone s2 = m.bone("smoke_2", "smoke_1", 0, 3.5, 1.5);
		Bodies.cc(s2, 0, 0, 1.5, 2.5, 3.5, 2.5, smoke);
		// The shards circling it, each tumbled its own way.
		m.bone("orbit", null, 0, 22, 0);
		double[][] at = {{0, 20, 9}, {120, 26, 8.5}, {240, 23, 9.5}};
		for (int i = 0; i < at.length; i++) {
			double a = Math.toRadians(at[i][0]), r = at[i][2], x = Math.sin(a) * r, z = Math.cos(a) * r;
			BeastArt.Bone shard = m.bone("shard_" + i, "orbit", x, at[i][1], z, 20 + i * 15, i * 50, 25 - i * 10);
			Bodies.cc(shard, x, at[i][1] - 1.5, z, 2, 3, 2, stone);
		}
	}

	// --- The Primordial Chaos ---

	static final int AZURE = c(0x2E78C8), CRIMSON = c(0xC8302A), YELLOW = c(0xE0B030), PALE = c(0xE8E2D8), INK = c(0x1C1822), GREY = c(0x6E6676),
			VIOLET = c(0xB07CFF);
	static final int[] FIVE = {AZURE, CRIMSON, YELLOW, PALE, INK};
	/** Its sack from this height to this (model pixels), and its middle (the widest of it). */
	static final double SACK_TOP = 80, SACK_BOTTOM = 8, SACK_MIDDLE = 44;

	/**
	 * Its hide on a slab of its sack from {@code y0} to {@code y1}: the five colours in bands round it top to bottom (azure, crimson, yellow,
	 * white, black), their edges churned and swirled, gone half to grey and the more the lower they lie, mottled grey through; the seam between
	 * each two glowing violet. (The first drawn were bright clean stripes round a box: tour 3c-1.)
	 */
	static BeastArt.Painter hide(double y0, double y1, int seed) {
		return (f, x, y, w, h, d) -> {
			double worldY = f == BeastArt.Face.UP ? y1 : f == BeastArt.Face.DOWN ? y0 : y1 - (y + 0.5) * (y1 - y0) / Math.max(1, h);
			double v = (SACK_TOP - worldY) / (SACK_TOP - SACK_BOTTOM);
			int px = x + f.ordinal() * 53;
			double swirl = Math.sin(px * 0.19 + v * 11 + seed) * 0.09 + Math.sin(px * 0.06 - v * 5 + seed * 2) * 0.07
					+ (BeastArt.hash(px / 4, (int) (v * 40), seed + 8) - 0.5) * 0.05;
			double t = Math.max(0, Math.min(0.999, v + swirl));
			int band = (int) (t * FIVE.length);
			double within = t * FIVE.length - band;
			if (band > 0 && within < 0.05) return glow(VIOLET);
			int col = BeastArt.mix(FIVE[band], GREY, Math.min(0.85, 0.35 + t * 0.5));
			double churn = BeastArt.hash(px / 3, y / 3, seed + 4);
			if (churn < 0.3) col = BeastArt.mix(col, churn < 0.12 ? INK : GREY, 0.45); // churned through with grey and dark
			return Bodies.noise(col, f, x, y, seed, 0.2);
		};
	}

	/**
	 * A wing: a broad membrane banded along its chord in the five colours, leading edge to trailing (yellow, crimson, azure, white, black),
	 * gone half to grey, ribbed by darker veins, its trailing edge glowing violet.
	 */
	static BeastArt.Painter wing(int seed) {
		int[] chord = {YELLOW, CRIMSON, AZURE, PALE, INK};
		return (f, x, y, w, h, d) -> {
			if (f == BeastArt.Face.BACK || (f == BeastArt.Face.UP || f == BeastArt.Face.DOWN) && y == 0) return glow(VIOLET);
			if (f != BeastArt.Face.UP && f != BeastArt.Face.DOWN) return Bodies.noise(BeastArt.mix(GREY, INK, 0.4), f, x, y, seed, 0.1);
			double t = 1 - y / (double) Math.max(1, h - 1); // 0 at the leading edge (the front), 1 at the trailing
			// Gone mostly to grey-violet, the bands only a tinge (the first drawn read as rainbow planks: tour 3c-2), veined, mottled.
			int col = BeastArt.mix(chord[Math.min(chord.length - 1, (int) (t * chord.length))], BeastArt.mix(GREY, c(0x4A3E5A), 0.4), 0.7);
			if (Math.floorMod(x, 6) == 0) col = BeastArt.shade(col, -0.35);
			if (BeastArt.hash(x / 2, y / 2, seed + 3) < 0.2) col = BeastArt.shade(col, -0.15);
			return Bodies.noise(col, f, x, y, seed, 0.16);
		};
	}

	/** The sack's slabs, bottom to top (each eight pixels high from {@link #SACK_BOTTOM}): their widths and depths, rounding it as a ball. */
	static final int[][] SLABS = {{36, 32}, {60, 54}, {72, 64}, {78, 70}, {80, 72}, {78, 70}, {72, 64}, {60, 54}, {36, 32}};

	/**
	 * The Primordial Chaos, Hundun's remnant: a vast faceless sack of churning chaos, round (in slabs, each its own width), banded in the
	 * five colours gone half to grey, the seams between them glowing violet; a slit across its front where its maw tears open; four broad
	 * wings, the fore pair high and swept up, the hind pair lower; six legs hanging beneath, three each side, thick at the haunch, their ends
	 * dark. Some forty blocks across its wings as the game draws it (BeastKind#modelScale). (The first drawn was a striped box with fins for
	 * wings: tour 3c-1.)
	 */
	static void primordialChaos(BeastArt.Model m) {
		BeastArt.Bone body = m.bone("body", null, 0, SACK_MIDDLE, 0);
		for (int i = 0; i < SLABS.length; i++) {
			double y0 = SACK_BOTTOM + i * 8;
			Bodies.cc(body, 0, y0, 0, SLABS[i][0], 8, SLABS[i][1], hide(y0, y0 + 8, 950 + i));
		}
		// Its maw: a slit across the front of its widest, black within, its lips glowing violet; torn open as it devours (BeastModels scales it).
		double front = -SLABS[4][1] / 2.0;
		BeastArt.Bone maw = m.bone("maw", "body", 0, SACK_MIDDLE, front - 0.2);
		Bodies.cc(maw, 0, SACK_MIDDLE - 1.5, front, 34, 3, 1.2, (f, x, y, w, h, d) -> f == BeastArt.Face.FRONT && (y == 0 || y == h - 1)
				|| f == BeastArt.Face.UP || f == BeastArt.Face.DOWN ? glow(VIOLET) : c(0x08060A));
		// Four wings: the fore pair high on its flanks, swept up; the hind pair lower and further back.
		BeastArt.Painter fore = wing(960), hind = wing(961);
		BeastArt.Bone wl = m.bone("wing_l", null, 34, 62, -8, 0, 0, 16);
		BeastArt.Cube foreWing = wl.cube(34, 61, -28, 48, 2, 40, fore);
		m.bone("wing_r", null, -34, 62, -8, 0, 0, -16).cubeLike(foreWing, -82, 61, -28);
		BeastArt.Bone wl2 = m.bone("wing_l2", null, 38, 46, 14, 0, 0, 6);
		BeastArt.Cube hindWing = wl2.cube(38, 45.25, 2, 40, 1.5, 30, hind);
		m.bone("wing_r2", null, -38, 46, 14, 0, 0, -6).cubeLike(hindWing, -78, 45.25, 2);
		// Six legs hanging beneath it, three each side, grey-violet, thick at the haunch, their ends dark.
		BeastArt.Painter haunch = (f, x, y, w, h, d) -> Bodies.noise(BeastArt.mix(GREY, c(0x4A3E5A), 0.4), f, x, y, 971, 0.16);
		BeastArt.Painter shin = (f, x, y, w, h, d) -> f == BeastArt.Face.DOWN || f != BeastArt.Face.UP && y >= h - 5 ? Bodies.noise(c(0x2A2430), f, x, y, 970, 0.1)
				: Bodies.noise(BeastArt.mix(GREY, c(0x3A3046), 0.5), f, x, y, 972, 0.16);
		BeastArt.Cube firstHaunch = null, firstShin = null;
		for (int i = 0; i < 6; i++) {
			int side = i < 3 ? 1 : -1;
			double x = side * 22, z = (i % 3 - 1) * 20;
			BeastArt.Bone leg = m.bone("leg_" + i, null, x, 20, z);
			if (firstHaunch == null) {
				firstHaunch = Bodies.cc(leg, x, 6, z, 12, 16, 12, haunch);
				firstShin = Bodies.cc(leg, x, -22, z, 8, 29, 8, shin);
			} else {
				leg.cubeLike(firstHaunch, x - 6, 6, z - 6);
				leg.cubeLike(firstShin, x - 4, -22, z - 4);
			}
		}
	}

	// --- Writing the two ---

	interface Build {
		void build(BeastArt.Model m);
	}

	static final Object[][] KINDS = {
			{"chaos_fiend", (Build) Chaos::chaosFiend, 0.03}, {"primordial_chaos", (Build) Chaos::primordialChaos, 0.02},
	};

	/** The two and the Frontier's items and blocks, under {@code assets}. */
	static void writeAll(Path assets) throws Exception {
		for (Object[] kind : KINDS) {
			String id = (String) kind[0];
			BeastArt.Model m = new BeastArt.Model("demon_beast_" + id);
			((Build) kind[1]).build(m);
			BeastArt.Atlas atlas = null;
			for (int size = 64; size <= 1024; size *= 2) {
				BeastArt.Atlas tryAtlas = new BeastArt.Atlas(size, size);
				tryAtlas.models.add(m);
				try {
					tryAtlas.pack();
					atlas = tryAtlas;
					break;
				} catch (IllegalStateException full) {
					// next size up
				}
			}
			if (atlas == null) throw new IllegalStateException(id + " fits no texture");
			BufferedImage[] painted = Bodies.split(atlas.paint());
			BeastArt.write(assets.resolve("textures/entity/demon_beast/" + id + ".png"), painted[0]);
			Path mask = assets.resolve("textures/entity/demon_beast/" + id + "_glowmask.png");
			if (painted[1] != null) BeastArt.write(mask, painted[1]);
			else java.nio.file.Files.deleteIfExists(mask);
			BeastArt.write(assets.resolve("geo/demon_beast/" + id + ".geo.json"), BeastArt.geo(m, atlas));
			BeastArt.write(assets.resolve("animations/demon_beast/" + id + ".animation.json"), BeastArt.animations(breath(5, (double) kind[2])));
			System.out.println(id + ": " + atlas.width + " x " + atlas.height + ", " + m.bones.size() + " bones");
		}
		BeastArt.write(assets.resolve("textures/block/chaos_origin_ore.png"), ore());
		BeastArt.write(assets.resolve("textures/item/chaos_origin_stone.png"), originStone());
		BufferedImage[] fragment = fragment();
		BeastArt.write(assets.resolve("textures/item/dao_fragment.png"), fragment[0]);
		BeastArt.write(assets.resolve("textures/item/dao_fragment_glow.png"), fragment[1]);
		BeastArt.write(assets.resolve("textures/block/first_flame.png"), firstFlame());
		BeastArt.write(assets.resolve("textures/block/first_flame.png.mcmeta"), "{\n  \"animation\": {\n    \"frametime\": 2\n  }\n}\n");
	}

	// --- The Frontier's items and blocks (16 x 16) ---

	/** A pixel's distance from a point, stretched {@code sy} times up and down. */
	static double dist(double x, double y, double cx, double cy, double sy) {
		return Math.hypot(x - cx, (y - cy) * sy);
	}

	/**
	 * Chaos Origin Ore: the Frontier's tuff-and-deepslate grey-violet, cracked, with four knots of chaos set hard in it, violet-black, their
	 * hearts bright violet with a fleck of white-gold.
	 */
	static BufferedImage ore() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		int stone = c(0x46424E), dark = c(0x2C2A32);
		double[][] knots = {{4, 4, 1.9}, {11.5, 5.5, 1.6}, {5.5, 11.5, 1.7}, {12, 12, 1.5}};
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				int col = BeastArt.shade(stone, (BeastArt.hash(x, y, 1201) - 0.5) * 0.3 + (BeastArt.hash(x / 2, y / 2, 1202) - 0.5) * 0.2);
				if (BeastArt.hash(x, y, 1203) < 0.07) col = dark;
				for (double[] k : knots) {
					double r = dist(x, y, k[0], k[1], 1);
					if (r <= k[2] + 0.35) {
						col = r < k[2] * 0.45 ? (BeastArt.hash(x, y, 1204) < 0.35 ? c(0xFFF0C8) : c(0xC896FF))
								: r < k[2] * 0.8 ? c(0x8A52D8) : c(0x2A1A40);
					}
				}
				img.setRGB(x, y, col);
			}
		}
		return img;
	}

	/** A Chaos Origin Stone: a rough-faceted stone of grey-violet chaos swirled through, a pale heart in it, glints of violet; dark-edged. */
	static BufferedImage originStone() {
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				double r = dist(x, y, 7.5, 8, 1.15) + (BeastArt.hash(x / 2, y / 2, 1210) - 0.5) * 1.4;
				if (r > 6.4) continue;
				int col;
				if (r > 5.5) col = c(0x231C2C);
				else {
					double swirl = Math.sin(x * 0.9 + y * 0.55) * 0.5 + 0.5;
					col = BeastArt.mix(c(0x5A4C6E), c(0x8C7AA8), swirl);
					if (x + y < 12) col = BeastArt.shade(col, 0.18); // lit from the upper left
					if (r < 2.0) col = BeastArt.mix(c(0xF4ECFF), c(0xC8B0F0), r / 2.0);
					if (BeastArt.hash(x, y, 1211) < 0.08) col = c(0xB07CFF);
				}
				img.setRGB(x, y, col);
			}
		}
		return img;
	}

	/**
	 * A Dao Fragment: a long jagged shard standing aslant, its rim silvered, a dark edge about it; its light (the second image) the inside
	 * of it, white at its heart to pale grey, which the game tints its Law's colour.
	 */
	static BufferedImage[] fragment() {
		BufferedImage shard = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), light = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) {
				// Along the shard (from its foot at the lower left to its point at the upper right) and across it.
				double along = (x + (15 - y)) / 30.0, across = (x - (15 - y)) / 2.0;
				double half = 2.6 * Math.sin(Math.min(1, along) * Math.PI) + (BeastArt.hash((int) (along * 12), 0, 1220) - 0.5) * 0.8;
				double a = Math.abs(across);
				if (along < 0.04 || along > 0.98 || a > half + 0.9) continue;
				if (a > half) shard.setRGB(x, y, c(0x2A2434)); // its dark edge
				else if (a > half - 1) shard.setRGB(x, y, c(0xD8D4E0)); // its silvered rim
				else {
					shard.setRGB(x, y, c(0x9A96A8));
					double heart = Math.abs(along - 0.5) * 2 + a / Math.max(1, half);
					light.setRGB(x, y, BeastArt.mix(c(0xFFFFFF), c(0x9A9A9A), Math.min(1, heart * 0.6)));
				}
			}
		}
		return new BufferedImage[] {shard, light};
	}

	static final int FLAME_FRAMES = 16;

	/**
	 * The First Flame: {@link #FLAME_FRAMES} frames of its tongues, one over another, three of them leaping each in its own time: white at
	 * their hearts, gold out to their edges, pale gold at their tips, nothing about them.
	 */
	static BufferedImage firstFlame() {
		BufferedImage img = new BufferedImage(16, 16 * FLAME_FRAMES, BufferedImage.TYPE_INT_ARGB);
		double[][] tongues = {{4.5, 0.0, 0.85}, {8.0, 2.1, 1.0}, {11.5, 4.2, 0.8}};
		for (int frame = 0; frame < FLAME_FRAMES; frame++) {
			double phase = frame / (double) FLAME_FRAMES * Math.PI * 2;
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					double up = (15 - y) / 15.0, heat = 0;
					for (double[] t : tongues) {
						double tall = t[2] * (0.78 + 0.22 * Math.sin(phase + t[1]));
						if (up > tall) continue;
						double sway = Math.sin(phase * 2 + t[1] + up * 4) * 0.9 * up;
						double width = 3.6 * (1 - Math.pow(up / tall, 1.4)) + 0.4;
						double off = Math.abs(x + 0.5 - (t[0] + sway));
						heat = Math.max(heat, (1 - off / width) * (1 - up / tall * 0.55));
					}
					// The flicker: the hash of the pixel rising with the frames.
					heat -= BeastArt.hash(x, y + frame * 3, 1230) * 0.22;
					if (heat <= 0.05) continue;
					int col = heat > 0.62 ? c(0xFFFFF4) : heat > 0.4 ? c(0xFFF0B0) : heat > 0.22 ? c(0xFFD870) : c(0xF0B848);
					img.setRGB(x, frame * 16 + y, col);
				}
			}
		}
		return img;
	}
}
