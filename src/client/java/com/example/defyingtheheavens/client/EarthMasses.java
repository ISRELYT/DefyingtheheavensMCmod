package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.EarthSpells;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static com.example.defyingtheheavens.client.BlockMasses.noise;
import static com.example.defyingtheheavens.client.BlockMasses.smooth;

/**
 * The earth's great spells made of the world's own blocks (SpellConstruct Form BLOCKS, laid out, meshed and drawn as BlockMasses lays out,
 * meshes and draws them; the user's ask, 2026-10-25: "rework falling mountain, to look more like falling continent and do that for most earth
 * skill the GeckoLib stuff still just looks a bit weird"). Each is laid out here with its foot (or its heart) at the construct's place, its
 * front toward +z (the way its yaw looks), and moved here as its spell calls its phases:
 * <ul>
 * <li>{@code PEAK_MASS}: Falling Mountain's peak, torn up out of a range: snow on its top, bare rock on its steeps, shrubs on its lower slopes,
 * the rock it was torn from hanging jagged under it, roots and earth clinging to its rim; driven down into what it smashes.</li>
 * <li>{@code PILLAR_MASS}: the broken pillar of Mount Buzhou, that held up heaven: a fluted column of pale stone bound with bands of gold,
 * cracked, broken off jagged at its top, the amber light of the earth glowing in its break.</li>
 * <li>{@code SEAL_MASS}: the Seal of Ten Thousand Mountains, an emperor's seal of jade edged with gold, mountains of jade capped with snow
 * carved on its top, the character 山 (mountain) in red on its face below.</li>
 * <li>{@code GRAVESTONE_MASS}: a colossal stele of black stone, the names of the dead in gold in columns down its face, borne on a stone
 * tortoise; sinking into the land.</li>
 * <li>{@code CAULDRON_MASS}: one of Yu the Great's nine bronze cauldrons (ding): three legs, its round belly banded, two ears over its rim,
 * embers in it; green with the age of its bronze, each its own.</li>
 * <li>{@code MENHIR_MASS}: a standing stone of the old world, rough and leaning, moss at its foot, a spiral carved on its face; heaving up out
 * of the land and toppling in on its ring's heart.</li>
 * <li>{@code CLOD_MASS}: Xirang's clod of the soil that grows of itself: dark living earth, moss, roots and gold, shoots on its top.</li>
 * <li>{@code VEIN_DOME_MASS}: the earth's veins, ribs of dark rock run through with gold and fire, bending in from the land to a keystone
 * overhead, the earth trigram burning under it; heaving up out of the land, sinking back.</li>
 * <li>{@code HAND_MASS}: the Hand of Houtu, the mother of the earth, of yellow jade veined with gold: its forearm rising out of the land, its
 * hand open, closing into a fist (its fingers curling joint by joint), brought down forward onto the land.</li>
 * </ul>
 */
final class EarthMasses {
	private EarthMasses() {}

	// --- Laying out ---

	static void layout(SpellConstruct.Kind kind, BlockMasses.Layout layout, float size, float value, long seed) {
		switch (kind) {
			case PEAK_MASS -> peak(layout, Math.max(6, size), seed);
			case PILLAR_MASS -> pillar(layout, Math.max(16, size), seed);
			case SEAL_MASS -> seal(layout, Math.max(5, size), seed);
			case GRAVESTONE_MASS -> gravestone(layout, Math.max(10, size), seed);
			case CAULDRON_MASS -> cauldron(layout, Math.max(5, size), seed);
			case MENHIR_MASS -> menhir(layout, Math.max(6, size), seed);
			case CLOD_MASS -> clod(layout, Math.max(1.5f, size), seed);
			case VEIN_DOME_MASS -> dome(layout, Math.max(8, size), seed);
			case HAND_MASS -> hand(layout, Math.max(6, size), seed);
			case BURIAL_MASS -> burial(layout, Math.max(1, size), Math.max(1, value), seed);
			default -> SpellMasses.layout(kind, layout, size, value, seed);
		}
	}

	private static BlockState of(net.minecraft.world.level.block.Block block) {
		return block.defaultBlockState();
	}

	/** Falling Mountain's peak, {@code radius} round at its foot (its foot at 0), the rock it was torn from hanging under it. */
	private static void peak(BlockMasses.Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius) + 1;
		float high = radius * 1.9f, snowLine = high * 0.58f;
		// A shoulder: a lesser peak to one side of it.
		double sa = Math.PI * 2 * noise(seed, 3, 1, 4);
		float sx = (float) Math.cos(sa) * radius * 0.42f, sz = (float) Math.sin(sa) * radius * 0.42f;
		int[][] tops = new int[r * 2 + 1][r * 2 + 1];
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				tops[x + r][z + r] = Integer.MIN_VALUE;
				double angle = Math.atan2(z, x);
				float rim = radius * (0.84f + 0.16f * smooth(seed + 11, (float) Math.cos(angle) * 6, (float) Math.sin(angle) * 6, 2.5f));
				float d = Mth.sqrt(x * x + z * z);
				if (d > rim) continue;
				float e = d / rim;
				float h = high * (float) Math.pow(1 - e, 1.7) * (0.82f + 0.3f * smooth(seed + 21, x, z, 4.5f));
				float sd = Mth.sqrt((x - sx) * (x - sx) + (z - sz) * (z - sz));
				h = Math.max(h, high * 0.55f * (float) Math.pow(Math.max(0, 1 - sd / (radius * 0.5f)), 1.3) * (0.85f + 0.25f * smooth(seed + 22, x, z, 3)));
				// Ridges running down it; its summit a crown of rock, not a needle.
				h += high * 0.08f * (1 - e) * (smooth(seed + 23, (float) Math.cos(angle) * 9, (float) Math.sin(angle) * 9, 1.5f) - 0.5f);
				h = Math.min(h, high * (0.8f + 0.12f * smooth(seed + 24, x, z, 2)));
				int top = Math.max(0, Mth.floor(h));
				float teeth = smooth(seed + 31, x, z, 2.5f), tooth = noise(seed, x >> 1, 5, z >> 1);
				int bottom = -Mth.floor(2 + radius * 0.75f * (float) Math.pow(1 - e, 0.75) * (0.55f + 0.45f * teeth) + (tooth < 0.08f ? radius * 0.3f * (1 - e) : 0));
				tops[x + r][z + r] = top;
				for (int y = bottom; y <= top; y++) layout.set(x, y, z, BlockMasses.rock(seed, x, y, z, bottom, top, high), 0);
				// Roots and earth clinging to its rim, hanging from under it.
				if (e > 0.62f && noise(seed, x, 77, z) < 0.25f) {
					int drop = 1 + (int) (noise(seed, x, 78, z) * 3);
					for (int k = 1; k <= drop; k++) layout.set(x, bottom - k, z, of(Blocks.HANGING_ROOTS), 0);
				}
			}
		}
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				int top = tops[x + r][z + r];
				if (top == Integer.MIN_VALUE) continue;
				int steep = 0;
				for (int[] n : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					int nx = x + n[0] + r, nz = z + n[1] + r;
					int other = nx < 0 || nz < 0 || nx > r * 2 || nz > r * 2 ? Integer.MIN_VALUE : tops[nx][nz];
					steep = Math.max(steep, other == Integer.MIN_VALUE ? 3 : Math.abs(other - top));
				}
				float n = noise(seed, x, 3, z);
				if (top > snowLine - 2 * smooth(seed + 41, x, z, 3)) {
					layout.set(x, top, z, steep > 2 && n < 0.5f ? of(Blocks.STONE) : of(Blocks.SNOW_BLOCK), 0);
					if (steep <= 1 && n < 0.55f) layout.set(x, top + 1, z, of(Blocks.SNOW), 0);
				} else if (steep > 1 || top > snowLine * 0.55f) {
					layout.set(x, top, z, n < 0.25f ? of(Blocks.ANDESITE) : n < 0.4f ? of(Blocks.GRAVEL) : n < 0.5f ? of(Blocks.COBBLESTONE) : of(Blocks.STONE), 0);
				} else {
					layout.set(x, top, z, n < 0.3f ? of(Blocks.PODZOL) : n < 0.42f ? of(Blocks.COARSE_DIRT) : of(Blocks.GRASS_BLOCK), 0);
					for (int y = top - 1; y >= top - 2; y--) if (layout.has(x, y, z)) layout.set(x, y, z, of(Blocks.DIRT), 0);
					float plant = noise(seed, x, 6, z);
					if (plant < 0.14f) layout.set(x, top + 1, z, of(plant < 0.06f ? Blocks.FERN : Blocks.GRASS), 0);
					else if (plant > 0.95f) layout.set(x, top + 1, z, of(plant > 0.98f ? Blocks.AZALEA : Blocks.SWEET_BERRY_BUSH), 0); // shrubs: a tree would dwarf it
				}
			}
		}
	}

	/** Buzhou's broken pillar, {@code length} long, its foot at 0: fluted pale stone bound in gold, cracked, broken off jagged at its top. */
	private static void pillar(BlockMasses.Layout layout, float length, long seed) {
		float radius = Math.max(3, length * 0.085f);
		int r = Mth.ceil(radius) + 2, len = Mth.ceil(length);
		float breakFrom = length - radius * 1.8f;
		int bands = 6;
		for (int y = 0; y <= len; y++) {
			boolean band = (y + 2) % Math.max(5, len / bands) < 2 && y < breakFrom - 2;
			boolean plinth = y < 3;
			for (int x = -r; x <= r; x++) {
				for (int z = -r; z <= r; z++) {
					double angle = Math.atan2(z + 0.5, x + 0.5);
					float d = Mth.sqrt((x + 0.5f) * (x + 0.5f) + (z + 0.5f) * (z + 0.5f));
					float flute = (float) Math.cos(angle * 12);
					float rr = radius * (1 + 0.07f * flute) + (plinth ? 1.6f : band ? 0.6f : 0);
					if (d > rr) continue;
					// Broken off jagged at its top: each column ending at its own height.
					float jag = breakFrom + radius * 1.8f * smooth(seed + 7, x, z, 1.6f);
					if (y > jag) continue;
					BlockState stuff;
					float n = noise(seed, x, y, z);
					if (y > jag - 1.5f) stuff = n < 0.25f ? of(Blocks.OCHRE_FROGLIGHT) : n < 0.6f ? of(Blocks.TUFF) : of(Blocks.COBBLESTONE);
					else if (plinth) stuff = y == 2 ? of(Blocks.GOLD_BLOCK) : of(Blocks.POLISHED_BLACKSTONE_BRICKS);
					else if (band) stuff = flute > 0.5f ? of(Blocks.GOLD_BLOCK) : of(Blocks.CHISELED_STONE_BRICKS);
					else if (d < rr - 1.2f) stuff = of(Blocks.STONE);
					else if (n < 0.04f) stuff = of(Blocks.OCHRE_FROGLIGHT); // the earth's light in a crack
					else if (n < 0.16f) stuff = of(Blocks.CRACKED_STONE_BRICKS);
					else if (flute < -0.6f) stuff = of(Blocks.POLISHED_ANDESITE);
					else stuff = n < 0.55f ? of(Blocks.SMOOTH_STONE) : of(Blocks.STONE_BRICKS);
					layout.set(x, y, z, stuff, 0);
				}
			}
		}
	}

	/** The Seal of Ten Thousand Mountains, {@code half} the half of its width, its face (the stamp) at 0: jade edged with gold, mountains carved over it. */
	private static void seal(BlockMasses.Layout layout, float half, long seed) {
		int w = Mth.ceil(half), tall = Mth.ceil(half * 0.85f);
		for (int x = -w; x <= w; x++) {
			for (int z = -w; z <= w; z++) {
				if (Math.abs(x) + Math.abs(z) > w * 1.8f) continue; // its corners bevelled
				boolean edge = Math.abs(x) == w || Math.abs(z) == w || Math.abs(x) + Math.abs(z) > w * 1.8f - 1.5f;
				for (int y = 0; y <= tall; y++) {
					BlockState stuff;
					float n = noise(seed, x, y, z);
					if (y == 0) stuff = stamp(x / (float) w, z / (float) w) ? of(Blocks.RED_CONCRETE) : jade(n);
					else if ((y == 1 || y == tall) && edge) stuff = of(Blocks.GOLD_BLOCK);
					else stuff = jade(n);
					layout.set(x, y, z, stuff, 0);
				}
			}
		}
		// Its handle: a range of jade mountains, snow on their tops.
		float[][] peaks = {{0, 0, half * 0.95f, half * 1.6f}, {-half * 0.6f, half * 0.3f, half * 0.6f, half * 1.0f}, {half * 0.6f, -half * 0.3f, half * 0.6f, half * 0.95f},
				{half * 0.2f, half * 0.65f, half * 0.45f, half * 0.7f}, {-half * 0.3f, -half * 0.65f, half * 0.45f, half * 0.65f}};
		for (int x = -w; x <= w; x++) {
			for (int z = -w; z <= w; z++) {
				float h = 0;
				for (float[] p : peaks) {
					float d = Mth.sqrt((x - p[0]) * (x - p[0]) + (z - p[1]) * (z - p[1]));
					h = Math.max(h, p[3] * (float) Math.pow(Math.max(0, 1 - d / p[2]), 1.25) * (0.85f + 0.3f * smooth(seed + 9, x, z, 2.5f)));
				}
				int top = Mth.floor(h);
				for (int y = 1; y <= top; y++) {
					BlockState stuff = y > top - 1 && h > half * 0.6f ? of(Blocks.SNOW_BLOCK) : y > half * 1.05f ? of(Blocks.CALCITE) : jade(noise(seed, x, y + 40, z));
					layout.set(x, tall + y, z, stuff, 0);
				}
			}
		}
	}

	/** Jade: the even green of oxidized copper, flecked a little with prismarine. */
	private static BlockState jade(float n) {
		if (n < 0.86f) return of(Blocks.OXIDIZED_COPPER);
		if (n < 0.95f) return of(Blocks.PRISMARINE_BRICKS);
		return of(Blocks.OXIDIZED_CUT_COPPER);
	}

	/** Whether the seal's face is red at {@code (u, v)} (each -1 to 1 across it): its frame, and the character 山 within it. */
	private static boolean stamp(float u, float v) {
		float au = Math.abs(u), av = Math.abs(v);
		if (Math.max(au, av) > 0.86f && Math.max(au, av) < 0.97f) return true; // its frame
		// 山: three strokes standing up from a stroke along its foot, the middle one tallest (its foot toward +v).
		boolean foot = v > 0.42f && v < 0.6f && au < 0.62f;
		boolean middle = au < 0.09f && v > -0.68f && v < 0.6f;
		boolean sides = Math.abs(au - 0.53f) < 0.09f && v > -0.25f && v < 0.6f;
		return foot || middle || sides;
	}

	/** Gravestone Falls' stele, {@code height} tall, on its stone tortoise (its foot at 0, its head toward +z): its face's names toward +z. */
	private static void gravestone(BlockMasses.Layout layout, float height, long seed) {
		float sw = height * 0.36f, sl = height * 0.5f, sh = height * 0.22f;
		int rw = Mth.ceil(sw) + 1, rl = Mth.ceil(sl) + 3, rh = Mth.ceil(sh) + 1;
		// The tortoise: its shell (plates of stone brick), its head stretched out before it, its feet.
		for (int x = -rw; x <= rw; x++) for (int z = -rl; z <= rl; z++) for (int y = 0; y <= rh; y++) {
			float ex = x / sw, ez = z / sl, ey = (y - 1) / sh;
			if (ex * ex + ez * ez + ey * ey > 1 || y < 1) continue;
			boolean seam = (Math.floorMod(x + (y % 2) * 2, 4) == 0 || Math.floorMod(z, 4) == 0) && ex * ex + ez * ez + ey * ey > 0.7f;
			float n = noise(seed, x, y, z);
			layout.set(x, y, z, seam ? of(Blocks.CHISELED_STONE_BRICKS) : n < 0.3f ? of(Blocks.MOSSY_STONE_BRICKS) : n < 0.45f ? of(Blocks.CRACKED_STONE_BRICKS)
					: of(Blocks.STONE_BRICKS), 0);
		}
		BlockMasses.sweep(layout, 0, sh * 0.5, sl * 0.85, 0, sh * 0.8, sl * 1.3, sh * 0.32, sh * 0.42, (x, y, z) -> of(Blocks.STONE), 0);
		for (int k = 0; k < 4; k++) {
			double fx = (k % 2 == 0 ? 1 : -1) * sw * 0.8, fz = (k < 2 ? 1 : -1) * sl * 0.55;
			BlockMasses.sweep(layout, fx * 0.8, sh * 0.5, fz * 0.8, fx * 1.1, 0.5, fz * 1.05, sh * 0.4, sh * 0.45, (x, y, z) -> of(Blocks.MOSSY_COBBLESTONE), 0);
		}
		// The stele, standing on its back: black stone, its arch carved, the names in gold down its face.
		float hw = height * 0.17f, th = Math.max(1.5f, height * 0.06f);
		int base = Mth.floor(sh * 1.2f), top = Mth.ceil(height * 1.15f);
		int iw = Mth.ceil(hw), it = Mth.ceil(th);
		for (int y = base; y <= top; y++) {
			for (int x = -iw; x <= iw; x++) {
				// Its top an arch.
				float archFrom = top - hw;
				if (y > archFrom) {
					float ax = x / hw, ay = (y - archFrom) / hw;
					if (ax * ax + ay * ay > 1) continue;
				}
				for (int z = -it; z <= it; z++) {
					boolean face = z == it, border = Math.abs(x) >= iw - 1 || y <= base + 1 || y >= archFrom - 1;
					BlockState stuff;
					if (y > archFrom) stuff = noise(seed, x, y, z) < 0.5f ? of(Blocks.CHISELED_POLISHED_BLACKSTONE) : of(Blocks.POLISHED_BLACKSTONE);
					else if (face && !border && Math.floorMod(x, 2) == 0 && noise(seed, x, y, 5) < 0.62f) stuff = of(Blocks.GOLD_BLOCK); // the names, in columns
					else if (border) stuff = of(Blocks.POLISHED_BLACKSTONE);
					else stuff = noise(seed, x, y, z) < 0.1f ? of(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) : of(Blocks.POLISHED_BLACKSTONE_BRICKS);
					layout.set(x, y, z, stuff, 0);
				}
			}
		}
	}

	/** One of the Nine Cauldrons, {@code height} tall (its feet at 0): three legs, a round banded belly, two ears over its rim, embers in it. */
	private static void cauldron(BlockMasses.Layout layout, float height, long seed) {
		float belly = height * 0.5f, legTop = height * 0.24f, rim = height * 0.74f;
		BlockState[] bronze = patina(seed);
		int rb = Mth.ceil(belly) + 2;
		for (int y = Mth.floor(legTop); y <= Mth.ceil(rim); y++) {
			float f = (y - legTop) / (rim - legTop);
			float r = belly * (0.78f + 0.26f * Mth.sin(f * (float) Math.PI * 0.9f + 0.25f));
			boolean band = Math.abs(f - 0.55f) < 0.08f;
			for (int x = -rb; x <= rb; x++) for (int z = -rb; z <= rb; z++) {
				float d = Mth.sqrt((x + 0.5f) * (x + 0.5f) + (z + 0.5f) * (z + 0.5f));
				if (d > r) continue;
				boolean wall = d > r - 1.5f || y <= legTop + 1;
				if (!wall) {
					// Within: embers at its floor, open above.
					if (y <= legTop + 2) layout.set(x, y, z, noise(seed, x, y, z) < 0.5f ? of(Blocks.MAGMA_BLOCK) : of(Blocks.BLACKSTONE), 0);
					continue;
				}
				double angle = Math.atan2(z + 0.5, x + 0.5);
				BlockState stuff = band ? (Math.cos(angle * 8) > 0.3 ? of(Blocks.GOLD_BLOCK) : bronze[2]) : bronze[noise(seed, x, y, z) < 0.88f ? 0 : 1];
				layout.set(x, y, z, stuff, 0);
			}
		}
		// Its lip.
		int ry = Mth.ceil(rim) + 1;
		for (int x = -rb; x <= rb; x++) for (int z = -rb; z <= rb; z++) {
			float d = Mth.sqrt((x + 0.5f) * (x + 0.5f) + (z + 0.5f) * (z + 0.5f));
			if (d <= belly * 1.04f && d > belly * 1.04f - 1.6f) layout.set(x, ry, z, bronze[2], 0);
		}
		// Its ears, standing over its rim either side.
		for (int s = -1; s <= 1; s += 2) {
			float ex = s * belly * 0.82f, high = height * 0.22f, wide = belly * 0.22f;
			BlockMasses.sweep(layout, ex, ry, -wide, ex, ry + high, -wide, 0.6, 0.6, (x, y, z) -> bronze[2], 0);
			BlockMasses.sweep(layout, ex, ry + high, -wide, ex, ry + high, wide, 0.6, 0.6, (x, y, z) -> bronze[2], 0);
			BlockMasses.sweep(layout, ex, ry, wide, ex, ry + high, wide, 0.6, 0.6, (x, y, z) -> bronze[2], 0);
		}
		// Its three legs.
		for (int k = 0; k < 3; k++) {
			double a = Math.PI * 2 * k / 3 + Math.PI / 6;
			double lx = Math.cos(a) * belly * 0.6, lz = Math.sin(a) * belly * 0.6;
			BlockMasses.sweep(layout, lx, legTop + 1, lz, lx * 1.2, 0.5, lz * 1.2, height * 0.09, height * 0.11, (x, y, z) -> bronze[1], 0);
		}
	}

	/** A bronze's patina, its own for its seed: its main, its streaks, its bands and ears. */
	private static BlockState[] patina(long seed) {
		float age = noise(seed, 1, 2, 3);
		if (age < 0.33f) return new BlockState[] {of(Blocks.WEATHERED_COPPER), of(Blocks.WEATHERED_CUT_COPPER), of(Blocks.OXIDIZED_CUT_COPPER)};
		if (age < 0.66f) return new BlockState[] {of(Blocks.OXIDIZED_COPPER), of(Blocks.OXIDIZED_CUT_COPPER), of(Blocks.WEATHERED_CUT_COPPER)};
		return new BlockState[] {of(Blocks.EXPOSED_COPPER), of(Blocks.EXPOSED_CUT_COPPER), of(Blocks.WEATHERED_CUT_COPPER)};
	}

	/** A standing stone, {@code height} tall (its foot at 0, its carved face toward +z): rough, leaning a little, mossy at its foot, a spiral on its face. */
	private static void menhir(BlockMasses.Layout layout, float height, long seed) {
		float lean = (noise(seed, 2, 2, 2) - 0.5f) * 0.12f;
		int h = Mth.ceil(height);
		for (int y = -3; y <= h; y++) {
			float f = Mth.clamp(y / height, 0, 1);
			float w = height * (0.2f - 0.07f * f) * (0.9f + 0.2f * smooth(seed + 3, y, 0, 3)), t = height * (0.12f - 0.03f * f);
			// Its top rounded.
			if (f > 0.85f) {
				float k = (f - 0.85f) / 0.15f;
				w *= Mth.sqrt(Math.max(0, 1 - k * k));
				t *= Mth.sqrt(Math.max(0, 1 - k * k));
			}
			float cx = lean * y;
			int iw = Mth.ceil(w) + 1, it = Mth.ceil(t) + 1;
			for (int x = -iw; x <= iw; x++) for (int z = -it; z <= it; z++) {
				float ex = (x + 0.5f - cx) / w, ez = (z + 0.5f) / t;
				if (ex * ex * ex * ex + ez * ez * ez * ez > 1 + 0.25f * (noise(seed, x, y, z) - 0.5f)) continue;
				float n = noise(seed, x, y, z);
				BlockState stuff;
				boolean face = z >= it - 1 && ez > 0.6f;
				float sx = (x + 0.5f - cx) / w, sy = (y - height * 0.55f) / (height * 0.25f);
				float rr = Mth.sqrt(sx * sx + sy * sy), aa = (float) Math.atan2(sy, sx);
				boolean spiral = face && rr < 0.95f && Math.abs(((rr * 3.2f - aa / (float) Math.PI) % 1 + 1) % 1 - 0.5f) < 0.18f;
				if (spiral) stuff = of(Blocks.CHISELED_DEEPSLATE);
				else if (y < 3 && n < 0.5f) stuff = of(Blocks.MOSSY_COBBLESTONE);
				else if (n < 0.2f) stuff = of(Blocks.ANDESITE);
				else if (n < 0.3f) stuff = of(Blocks.TUFF);
				else if (n < 0.36f) stuff = of(Blocks.COBBLESTONE);
				else stuff = of(Blocks.STONE);
				layout.set(x, y, z, stuff, 0);
			}
		}
	}

	/** Xirang's clod, {@code radius} round about its heart at 0: living earth, moss and roots, gold in it, shoots on its top. */
	private static void clod(BlockMasses.Layout layout, float radius, long seed) {
		int r = Mth.ceil(radius) + 1;
		for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
			float d = Mth.sqrt(x * x + y * y + z * z);
			if (d > radius * (0.85f + 0.25f * noise(seed, x, y, z))) continue;
			float n = noise(seed, x + 9, y, z);
			BlockState stuff = n < 0.08f ? of(Blocks.RAW_GOLD_BLOCK) : n < 0.3f ? of(Blocks.ROOTED_DIRT) : n < 0.5f ? of(Blocks.MUD) : n < 0.62f ? of(Blocks.COARSE_DIRT)
					: of(Blocks.DIRT);
			if (y >= r - 2 && n > 0.4f) stuff = of(Blocks.MOSS_BLOCK);
			layout.set(x, y, z, stuff, 0);
		}
		for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
			int top = Integer.MIN_VALUE, bottom = Integer.MAX_VALUE;
			for (int y = -r; y <= r; y++) if (layout.has(x, y, z)) {
				top = Math.max(top, y);
				bottom = Math.min(bottom, y);
			}
			if (top == Integer.MIN_VALUE) continue;
			float n = noise(seed, x, 5, z);
			if (n < 0.35f) layout.set(x, top + 1, z, of(n < 0.15f ? Blocks.FERN : Blocks.GRASS), 0);
			if (noise(seed, x, 6, z) < 0.3f) layout.set(x, bottom - 1, z, of(Blocks.HANGING_ROOTS), 0);
		}
	}

	/**
	 * Xirang's burial: the living earth heaved up round a being, {@code radius} round at its foot and {@code height} high (its foot at 0): a rough
	 * mound of earth, rooted earth and moss, flecked with gold, a little ragged at its top where the being's head stands out of it, grass and
	 * ferns springing up over it, roots clutching at its foot.
	 */
	private static void burial(BlockMasses.Layout layout, float radius, float height, long seed) {
		int r = Mth.ceil(radius) + 1;
		for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
			float d = Mth.sqrt(x * x + z * z) / (radius + 0.5f);
			if (d > 1.15f) continue;
			int top = Mth.floor(height * (1.08f - 0.55f * d * d) * (0.85f + 0.3f * noise(seed, x, 1, z)));
			for (int y = -1; y <= top; y++) {
				float n = noise(seed, x, y, z);
				BlockState stuff = n < 0.06f ? of(Blocks.RAW_GOLD_BLOCK) : n < 0.3f ? of(Blocks.ROOTED_DIRT) : n < 0.45f ? of(Blocks.COARSE_DIRT) : of(Blocks.DIRT);
				if (y == top) stuff = n < 0.55f ? of(Blocks.GRASS_BLOCK) : n < 0.8f ? of(Blocks.MOSS_BLOCK) : of(Blocks.ROOTED_DIRT);
				layout.set(x, y, z, stuff, 0);
			}
			float plant = noise(seed, x, 7, z);
			if (plant < 0.3f) layout.set(x, top + 1, z, of(plant < 0.12f ? Blocks.FERN : Blocks.GRASS), 0);
		}
	}

	/** The dome of the earth's veins, {@code radius} round (its foot at 0): eight ribs bending in to a keystone, two rings binding them, the trigram under it. */
	private static void dome(BlockMasses.Layout layout, float radius, long seed) {
		float high = radius * 0.82f;
		BlockMasses.Stuff vein = (x, y, z) -> {
			float n = noise(seed, x, y, z);
			if (n < 0.06f) return of(Blocks.MAGMA_BLOCK);
			if (n < 0.13f) return of(Blocks.GILDED_BLACKSTONE);
			if (n < 0.17f) return of(Blocks.RAW_GOLD_BLOCK);
			if (n < 0.45f) return of(Blocks.COBBLED_DEEPSLATE);
			if (n < 0.7f) return of(Blocks.DEEPSLATE);
			return of(Blocks.BLACKSTONE);
		};
		int ribs = 8;
		for (int k = 0; k < ribs; k++) {
			double a = Math.PI * 2 * k / ribs + noise(seed, k, 1, 1) * 0.12;
			int steps = 18;
			for (int i = 0; i < steps; i++) {
				double t0 = i / (double) steps, t1 = (i + 1) / (double) steps;
				double p0 = t0 * Math.PI / 2, p1 = t1 * Math.PI / 2;
				// Knotted as it climbs: a little in and out along its way.
				double w0 = 1 + 0.06 * Math.sin(t0 * 17 + k), w1 = 1 + 0.06 * Math.sin(t1 * 17 + k);
				double r0 = Math.max(1.2, 3.0 - 1.7 * t0), r1 = Math.max(1.2, 3.0 - 1.7 * t1);
				BlockMasses.sweep(layout, Math.cos(a) * radius * Math.cos(p0) * w0, high * Math.sin(p0), Math.sin(a) * radius * Math.cos(p0) * w0,
						Math.cos(a) * radius * Math.cos(p1) * w1, high * Math.sin(p1), Math.sin(a) * radius * Math.cos(p1) * w1, r0, r1, vein, 0);
			}
			// Its root, bursting out of the land.
			BlockMasses.sweep(layout, Math.cos(a) * radius, 0, Math.sin(a) * radius, Math.cos(a) * radius * 1.12, -2, Math.sin(a) * radius * 1.12, 2.6, 1.6, vein, 0);
		}
		// Two rings binding the ribs.
		for (double ring : new double[] {0.3, 0.62}) {
			double p = ring * Math.PI / 2, rr = radius * Math.cos(p), y = high * Math.sin(p);
			int n = Mth.ceil(rr * 2);
			for (int i = 0; i < n; i++) {
				double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
				BlockMasses.sweep(layout, Math.cos(a0) * rr, y, Math.sin(a0) * rr, Math.cos(a1) * rr, y, Math.sin(a1) * rr, 0.8, 0.8, vein, 0);
			}
		}
		// The keystone, ringed in gold; under it the earth trigram ☷, its three broken bars burning.
		BlockMasses.sweep(layout, 0, high - 1, 0, 0, high + 1.5, 0, 3.2, 2.4, (x, y, z) -> Math.abs(Math.sqrt(x * x + z * z) - 2.6) < 0.7 ? of(Blocks.GOLD_BLOCK)
				: of(Blocks.CHISELED_POLISHED_BLACKSTONE), 0);
		int ty = Mth.floor(high) - 3;
		for (int bar = -1; bar <= 1; bar++) {
			for (int x = -3; x <= 3; x++) {
				if (x == 0) continue; // broken in the middle
				layout.set(x, ty, bar * 2, of(Blocks.OCHRE_FROGLIGHT), 0);
			}
		}
	}

	// --- The Hand of Houtu ---

	/** The hand's measure: a tenth of its size (its fist's reach from its wrist's foot). */
	private static float unit(float size) {
		return size / 10f;
	}

	/** Finger {@code f}'s joints (its knuckle and the two above it, and its tip) at rest, open and upright: {x, y, z} each. */
	private static float[][] finger(int f, float size) {
		float u = unit(size);
		float[] xs = {-2.2f, -0.75f, 0.75f, 2.2f};
		float[][] lengths = {{2.5f, 1.8f, 1.4f}, {3.0f, 2.2f, 1.7f}, {2.9f, 2.1f, 1.6f}, {2.2f, 1.6f, 1.3f}};
		float[][] joints = new float[4][3];
		float y = 10.6f * u - (f == 0 || f == 3 ? 0.4f * u : 0);
		joints[0] = new float[] {xs[f] * u, y, 0};
		for (int s = 0; s < 3; s++) joints[s + 1] = new float[] {xs[f] * u, joints[s][1] + lengths[f][s] * u, 0};
		return joints;
	}

	/** The thumb's joints at rest (its root, its knuckle, its tip): out to the side, up and a little forward. */
	private static float[][] thumb(float size) {
		float u = unit(size);
		float[] root = {2.9f * u, 6.2f * u, -0.4f * u};
		float[] dir = {0.6f, 0.74f, -0.3f};
		float[][] joints = new float[3][];
		joints[0] = root;
		joints[1] = new float[] {root[0] + dir[0] * 2.4f * u, root[1] + dir[1] * 2.4f * u, root[2] + dir[2] * 2.4f * u};
		joints[2] = new float[] {joints[1][0] + dir[0] * 1.9f * u, joints[1][1] + dir[1] * 1.9f * u, joints[1][2] + dir[2] * 1.9f * u};
		return joints;
	}

	/** Its part for finger {@code f}'s segment {@code s} (its forearm and palm are part 0; the thumb's two, 13 and 14). */
	private static int part(int f, int s) {
		return 1 + f * 3 + s;
	}

	/** The Hand of Houtu, {@code size} its fist's reach (its wrist's foot at 0, its palm toward -z, its back toward +z, where it slams). */
	private static void hand(BlockMasses.Layout layout, float size, long seed) {
		float u = unit(size);
		BlockMasses.Stuff jade = (x, y, z) -> {
			float n = noise(seed, x, y, z), m = smooth(seed + 5, x + y * 0.6f, z - y * 0.4f, 2.2f);
			if (Math.abs(m - 0.5f) < 0.03f) return of(Blocks.GOLD_BLOCK); // its veins of gold
			if (n < 0.015f) return of(Blocks.OCHRE_FROGLIGHT);
			if (n < 0.82f) return of(Blocks.SMOOTH_SANDSTONE);
			if (n < 0.95f) return of(Blocks.CUT_SANDSTONE);
			return of(Blocks.SANDSTONE);
		};
		// Its forearm, rising out of the land, and its wrist.
		BlockMasses.sweep(layout, 0, -6 * u, 0.2 * u, 0, 5.8 * u, 0, 2.1 * u, 1.75 * u, jade, 0);
		// Its palm: broad and thick, rounded at its edges.
		int px = Mth.ceil(3.0f * u), py0 = Mth.floor(5.2f * u), py1 = Mth.ceil(10.9f * u), pz = Mth.ceil(1.2f * u);
		for (int x = -px; x <= px; x++) for (int y = py0; y <= py1; y++) for (int z = -pz; z <= pz; z++) {
			float ex = x / (3.0f * u), ey = (y - 8.1f * u) / (2.85f * u), ez = z / (1.2f * u);
			if (ex * ex * ex * ex + ey * ey * ey * ey + ez * ez > 1.05f) continue;
			layout.set(x, y, z, jade.at(x, y, z), 0);
		}
		// Its fingers, joint by joint, each segment its own part (it curls them), knuckles thicker.
		for (int f = 0; f < 4; f++) {
			float[][] j = finger(f, size);
			for (int s = 0; s < 3; s++) {
				float r0 = (0.72f - 0.08f * s) * u, r1 = (0.64f - 0.08f * s) * u;
				BlockMasses.sweep(layout, j[s][0], j[s][1], j[s][2], j[s + 1][0], j[s + 1][1], j[s + 1][2], r0, r1, jade, part(f, s));
			}
			// Its nail, pale.
			float[] tip = j[3];
			layout.set(Mth.floor(tip[0]), Mth.floor(tip[1]) - 1, Mth.floor(tip[2] + 0.35f * u), of(Blocks.SMOOTH_QUARTZ), part(f, 2));
		}
		float[][] t = thumb(size);
		BlockMasses.sweep(layout, t[0][0], t[0][1], t[0][2], t[1][0], t[1][1], t[1][2], 0.85 * u, 0.72 * u, jade, 13);
		BlockMasses.sweep(layout, t[1][0], t[1][1], t[1][2], t[2][0], t[2][1], t[2][2], 0.72 * u, 0.6 * u, jade, 14);
	}

	// --- Drawing ---

	static void draw(SpellConstruct c, BlockMasses.Mass mass, float partial, float age, PoseStack pose, MultiBufferSource buffers) {
		float size = c.size();
		String phase = c.phase();
		float since = c.phaseAge(partial);
		float left = c.life() - age;
		pose.pushPose();
		switch (c.kind()) {
			case PEAK_MASS -> {
				boolean struck = "impact".equals(phase);
				if (struck) pose.translate(0, -Math.min(since * 0.45f + since * since * 0.012f, size * 1.1f), 0);
				float wobble = struck ? Math.max(0, 1 - since / 6) : 1;
				pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.06f) * 5 * wobble));
				pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 0.05f + 1.1f) * 4 * wobble));
				BlockMasses.draw(mass, 0, pose);
			}
			case PILLAR_MASS -> {
				shudder(pose, "impact".equals(phase) ? since : -1, 2.5f);
				sinkAway(pose, left, size * 0.6f);
				BlockMasses.draw(mass, 0, pose);
			}
			case SEAL_MASS -> {
				if ("stamp".equals(phase)) {
					float t = Mth.clamp(since / 10, 0, 1);
					pose.translate(0, Mth.sin(t * (float) Math.PI) * (1 - t) * size * 0.08f, 0);
				}
				sinkAway(pose, left, size * 2);
				BlockMasses.draw(mass, 0, pose);
			}
			case GRAVESTONE_MASS -> {
				if ("land".equals(phase)) shudder(pose, since, 2);
				if ("sink".equals(phase)) pose.translate(0, -size * 1.25f * SpellRender.ease(since / EarthSpells.GRAVE_SINKS), 0);
				BlockMasses.draw(mass, 0, pose);
			}
			case CAULDRON_MASS -> {
				if ("land".equals(phase)) {
					float t = Mth.clamp(since / 8, 0, 1);
					pose.translate(0, Mth.sin(t * (float) Math.PI) * (1 - t) * size * 0.06f, 0);
				}
				BlockMasses.draw(mass, 0, pose);
			}
			case MENHIR_MASS -> {
				// Heaving up out of the land, trembling as it comes; toppling in on the ring's heart (toward +z) about its foot's front.
				float rise = SpellRender.easeOut(age / EarthSpells.BURIED_RISES);
				pose.translate(0, -(1 - rise) * (size + 3), 0);
				if (rise < 1) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 1.7f) * 1.2f * (1 - rise)));
				if ("topple".equals(phase)) {
					float t = Mth.clamp(since / EarthSpells.BURIED_FALLS, 0, 1);
					float edge = size * 0.12f;
					pose.translate(0, 0, edge);
					pose.mulPose(Axis.XP.rotationDegrees(84 * t * t));
					pose.translate(0, 0, -edge);
				}
				sinkAway(pose, left, size);
				BlockMasses.draw(mass, 0, pose);
			}
			case CLOD_MASS -> {
				// Landed, it swells, throbbing, and sinks down into the land, its soil poured out into it (the land grows of itself from there).
				float grow = 1;
				if ("swell".equals(phase)) {
					float t = Mth.clamp(since / EarthSpells.XIRANG_SWELLS, 0, 1);
					grow = (1 + 0.5f * SpellRender.easeOut(Math.min(1, t * 3))) * (1 + 0.08f * Mth.sin(since * 0.9f)) * (1 - 0.35f * t);
					pose.translate(0, -size * 1.6f * SpellRender.ease(t), 0);
				}
				if ("settle".equals(phase)) grow = 0;
				if (grow <= 0.02f) break;
				pose.mulPose(Axis.YP.rotationDegrees(age * 3));
				pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(age * 0.2f) * 12));
				pose.scale(grow, grow, grow);
				BlockMasses.draw(mass, 0, pose);
			}
			case BURIAL_MASS -> {
				// Heaving up out of the land round the being it buries, trembling as it comes; crumbling back down into it as it lets go.
				float rise = SpellRender.easeOut(Math.min(1, age / 10));
				float crumble = "crumble".equals(phase) ? SpellRender.ease(since / 12) : 0;
				pose.translate(0, -(1 - rise + crumble) * (c.value() + 1.5f), 0);
				if (rise < 1 || crumble > 0) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 1.8f) * 2.5f * Math.max(1 - rise, crumble)));
				BlockMasses.draw(mass, 0, pose);
			}
			case VEIN_DOME_MASS -> {
				float rise = SpellRender.easeOut(age / EarthSpells.LOCK_RISES);
				float sink = "release".equals(phase) ? SpellRender.ease(since / EarthSpells.LOCK_RELEASE) : 0;
				pose.translate(0, -(1 - rise + sink) * size * 0.9f, 0);
				if (rise < 1) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(age * 1.9f) * 0.8f * (1 - rise)));
				BlockMasses.draw(mass, 0, pose);
			}
			case HAND_MASS -> hand(c, mass, partial, age, size, phase, since, left, pose);
			default -> SpellMasses.draw(c, mass, partial, age, pose, buffers);
		}
		pose.popPose();
	}

	/** A shudder through it as it lands, {@code since} ticks ago (less than nought: none), fading over {@code ticks}. */
	private static void shudder(PoseStack pose, float since, float degrees) {
		if (since < 0 || since > 12) return;
		float f = 1 - since / 12;
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(since * 2.1f) * degrees * f));
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(since * 1.7f + 1) * degrees * f));
	}

	/** Sunk back into the land over its last second ({@code left} ticks of its life left), {@code depth} down by its end. */
	private static void sinkAway(PoseStack pose, float left, float depth) {
		if (left >= 20) return;
		float f = 1 - Math.max(0, left) / 20;
		pose.translate(0, -depth * f * f, 0);
	}

	/**
	 * The Hand of Houtu: rising out of the land, its fingers open and stirring ({@link EarthSpells#HOUTU_RISES}); closing into a fist
	 * ({@code grasp}: each finger joint by joint, the thumb over them); brought down forward onto the land about its wrist's foot
	 * ({@code slam}, {@link EarthSpells#HOUTU_SLAMS}), and lying there till it sinks away.
	 */
	private static void hand(SpellConstruct c, BlockMasses.Mass mass, float partial, float age, float size, String phase, float since, float left, PoseStack pose) {
		float u = unit(size);
		float rise = SpellRender.easeOut(age / EarthSpells.HOUTU_RISES);
		pose.translate(0, -(1 - rise) * 13 * u, 0);
		boolean grasping = "grasp".equals(phase) || "slam".equals(phase);
		float closed = "grasp".equals(phase) ? SpellRender.ease(since / EarthSpells.HOUTU_GRASPS) : "slam".equals(phase) ? 1 : 0;
		if ("slam".equals(phase)) {
			float t = Mth.clamp(since / EarthSpells.HOUTU_SLAMS, 0, 1);
			pose.mulPose(Axis.XP.rotationDegrees(95 * t * t));
		}
		sinkAway(pose, left, 6 * u);
		BlockMasses.draw(mass, 0, pose);
		// Open, its fingers stir; closing, they curl joint by joint into a fist.
		float[] curl = {-84, -96, -82};
		for (int f = 0; f < 4; f++) {
			float[][] j = finger(f, size);
			float stir = grasping ? 0 : Mth.sin(age * 0.12f + f * 0.9f) * 4;
			pose.pushPose();
			for (int s = 0; s < 3; s++) {
				float open = -8 - 4 * s + stir;
				float angle = Mth.lerp(SpellRender.ease(Mth.clamp(closed * 1.25f - s * 0.12f, 0, 1)), open, curl[s]);
				pose.translate(j[s][0], j[s][1], j[s][2]);
				pose.mulPose(Axis.XP.rotationDegrees(angle));
				pose.translate(-j[s][0], -j[s][1], -j[s][2]);
				BlockMasses.draw(mass, part(f, s), pose);
			}
			pose.popPose();
		}
		float[][] t = thumb(size);
		pose.pushPose();
		float[] thumbCurl = {52, 44};
		for (int s = 0; s < 2; s++) {
			float angle = Mth.lerp(SpellRender.ease(Mth.clamp(closed * 1.2f - 0.15f - s * 0.1f, 0, 1)), 4, thumbCurl[s]);
			pose.translate(t[s][0], t[s][1], t[s][2]);
			pose.mulPose(Axis.ZP.rotationDegrees(angle));
			pose.mulPose(Axis.XP.rotationDegrees(-angle * 0.35f));
			pose.translate(-t[s][0], -t[s][1], -t[s][2]);
			BlockMasses.draw(mass, 13 + s, pose);
		}
		pose.popPose();
	}
}
