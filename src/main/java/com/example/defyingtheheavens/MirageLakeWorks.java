package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * The Jade Lake Mirage's works (SecretRealm#JADE_LAKE_MIRAGE; its land is MirageLake's, the machinery every realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: bridges of jade between the isles and to the heart isle, about a third of them false ({@link #FALSE_SHARE}: their
 *   middle span a mirage, MirageBlock, one walks onto and falls through); on the heart isle the Mirror Pavilion, its great mirror across its
 *   middle (JadeMirrorBlock), a hall behind it with the altar; willows by the water and blossom trees about the isles and the hills; a
 *   terrace at each arrival.</li>
 *   <li><b>Renewed each time it stirs</b>: spirit lotuses by the isles (older by every year it slept), and among them mirage lotuses; two
 *   vaults sealed anew for the ancients, and two mirage caskets in their look; the mirror whole; the bones of a few earlier visitors.</li>
 *   <li><b>Its guardians</b>: Mirror Shades about the isles, on the bridges and before the pavilion.</li>
 *   <li><b>Its event</b>: some time into each opening the mirror wakes for {@link #WAKE_TICKS}: each visitor's reflection rises from the
 *   lake beside them, at half their strength (MirrorReflection).</li>
 *   <li><b>Its trial</b>: the mirror touched, the toucher's reflection steps out of it, a copy of them to be bested; bested, the mirror is a
 *   door to them, and behind it the altar: the Great Inheritance (the Jade Lake Mirror) once in a world, after it a lesser legacy each
 *   opening.</li>
 * </ul>
 */
public final class MirageLakeWorks extends RealmWorks {
	static final MirageLakeWorks WORKS = new MirageLakeWorks();
	/** The mirror wakes this long (a minute). */
	static final int WAKE_TICKS = 1200;
	/** The pavilion's floor, on the heart isle's mound. */
	static final int FLOOR = MirageLake.ISLE_Y + 3;
	/** About this share of the lake's bridges are false (never the landing's to the heart). */
	static final double FALSE_SHARE = 0.34;
	/** The bridges' decks: one over the water. */
	static final int DECK_Y = MirageLake.WATER_Y + 1;

	// --- Where things are ---

	static Direction mainWay(MirageLake.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The pavilion's frame: its middle the heart isle's at its floor, forward toward the landing (f), s to its right. */
	static Frame pavilion(MirageLake.Shape s, BlockPos c) {
		return new Frame(c.atY(FLOOR), mainWay(s));
	}

	/** The great mirror: across the pavilion's middle, five wide and six high, facing the landing. */
	static List<BlockPos> mirror(Frame p) {
		List<BlockPos> out = new ArrayList<>();
		for (int sd = -2; sd <= 2; sd++) for (int y = 1; y <= 6; y++) out.add(p.at(0, sd, y));
		return out;
	}

	/** Where one stands before the mirror; where a reflection steps out of it. */
	static BlockPos beforeMirror(Frame p) { return p.at(4, 0, 1); }

	static BlockPos stepsOut(Frame p) { return p.at(2, 0, 1); }

	/** Before the altar in the hall behind the mirror, where its legacy waits. */
	static BlockPos altar(Frame p) { return p.at(-5, 0, 1); }

	/** Whether local (f, s, y) of the pavilion's frame is of the hall behind the mirror (its walls, roof and floor). */
	static boolean ofInnerHall(int[] at) {
		return at[0] >= -12 && at[0] <= 0 && Math.abs(at[1]) <= 8 && at[2] >= -3 && at[2] <= 16;
	}

	/** Every isle with the heart's first: the bridges' ends. */
	static List<MirageLake.Isle> nodes(MirageLake.Shape s) {
		List<MirageLake.Isle> out = new ArrayList<>();
		out.add(new MirageLake.Isle(0, 0, MirageLake.HEART_R, "heart"));
		out.addAll(s.isles);
		return out;
	}

	/** The bridges: each isle joined to the nearest already joined, from the heart out (the shortest tree of them). */
	static List<int[]> bridges(MirageLake.Shape s) {
		List<MirageLake.Isle> isles = nodes(s);
		boolean[] joined = new boolean[isles.size()];
		joined[0] = true;
		List<int[]> out = new ArrayList<>();
		for (int k = 1; k < isles.size(); k++) {
			int from = -1, to = -1;
			double best = Double.MAX_VALUE;
			for (int a = 0; a < isles.size(); a++) {
				if (!joined[a]) continue;
				for (int b = 0; b < isles.size(); b++) {
					if (joined[b]) continue;
					MirageLake.Isle ia = isles.get(a), ib = isles.get(b);
					double d = Math.hypot(ia.x() - ib.x(), ia.z() - ib.z()) - ia.r() - ib.r();
					if (d < best) {
						best = d;
						from = a;
						to = b;
					}
				}
			}
			if (to < 0) break;
			joined[to] = true;
			out.add(new int[] {from, to});
		}
		return out;
	}

	/** Whether the bridge {@code b} is false: never the landing's (nodes 0 and 1: the heart and the landing). */
	static boolean isFalse(MirageLake.Shape s, int[] b) {
		if (b[0] <= 1 && b[1] <= 1) return false;
		return RealmLand.chance(b[0] * 31 + b[1], b[1] * 17 + b[0], s.seed ^ 0x3A1EFL) < FALSE_SHARE;
	}

	/** Whether a span {@code along} of the way across (0 to 1) is the false bridge's mirage: its middle. */
	static boolean falseSpan(double along) {
		return along > 0.33 && along < 0.67;
	}

	/** An isle's middle, at the isles' height. */
	static BlockPos isleMiddle(BlockPos c, MirageLake.Isle isle) {
		return c.offset(isle.x(), 0, isle.z()).atY(MirageLake.ISLE_Y);
	}

	/** An isle's frame: at its middle, facing the lake's middle. */
	static Frame isleFrame(BlockPos c, MirageLake.Isle isle) {
		double cos = -isle.x(), sin = -isle.z();
		Direction in = Math.abs(cos) > Math.abs(sin) ? (cos > 0 ? Direction.EAST : Direction.WEST) : (sin > 0 ? Direction.SOUTH : Direction.NORTH);
		return new Frame(isleMiddle(c, isle), in);
	}

	/** The lake's own isles, the largest first: the first two keep the vaults, the next two the mirage caskets. */
	static List<MirageLake.Isle> treasureIsles(MirageLake.Shape s) {
		List<MirageLake.Isle> own = new ArrayList<>();
		for (MirageLake.Isle isle : s.isles) if (isle.role().equals("isle")) own.add(isle);
		own.sort((a, b) -> b.r() - a.r());
		return own.subList(0, Math.min(4, own.size()));
	}

	/** Where an isle's vault (or casket) stands: on its ground, a little back from its middle. */
	static BlockPos treasureAt(MirageLake.Shape s, BlockPos c, MirageLake.Isle isle) {
		BlockPos at = isleFrame(c, isle).at(-2, 0, 0);
		return at.atY(MirageLake.column(s, at.getX() - c.getX(), at.getZ() - c.getZ()).ground() + 1);
	}

	/** The spirit lotuses' places: on the water just off each isle's shore, two to an isle. */
	static List<BlockPos> lotusSpots(MirageLake.Shape s, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		RandomSource random = RandomSource.create(s.seed ^ 0x3A107L);
		for (MirageLake.Isle isle : s.isles) {
			for (int k = 0, tries = 0; k < 2 && tries < 24; tries++) {
				double a = random.nextDouble() * Math.PI * 2, d = isle.r() + 0.5 + random.nextDouble() * 2.5;
				int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
				MirageLake.Column col = MirageLake.column(s, x, z);
				if (col.kind() != MirageLake.Kind.LAKE || col.ground() >= MirageLake.WATER_Y) continue;
				out.add(c.offset(x, 0, z).atY(MirageLake.WATER_Y + 1));
				k++;
			}
		}
		return out;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		MirageLake.Shape s = MirageLake.shape(seed);
		List<MirageLake.Isle> nodes = nodes(s);
		for (int[] b : bridges(s)) steps.add(() -> bridge(level, s, c, nodes.get(b[0]), nodes.get(b[1]), isFalse(s, b)));
		steps.add(() -> pavilionBase(level, s, c));
		steps.add(() -> pavilionHalls(level, s, c));
		for (int part = 0; part < 4; part++) {
			int p = part;
			steps.add(() -> trees(level, s, c, p, RandomSource.create(seed ^ p * 0x3A1E7L)));
		}
		steps.add(() -> arrivalTerraces(level, s, c));
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		MirageLake.Shape s = MirageLake.shape(seed);
		steps.add(() -> renewLotuses(level, s, c, r));
		steps.add(() -> renewTreasures(level, s, c, r));
		steps.add(() -> renewMirror(level, s, c));
	}

	// --- Building ---

	static BlockState leaves(Block block) {
		return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
	}

	/**
	 * A bridge of jade from {@code a} to {@code b} over the water: a deck three wide one over the lake, white rails along it, piers down to the
	 * bed every few blocks; {@code fake}, its middle span a mirage of its deck (no piers under it, its rails real).
	 */
	static void bridge(ServerLevel level, MirageLake.Shape s, BlockPos c, MirageLake.Isle a, MirageLake.Isle b, boolean fake) {
		double dx = b.x() - a.x(), dz = b.z() - a.z(), len = Math.hypot(dx, dz);
		if (len < 1) return;
		double ux = dx / len, uz = dz / len;
		BlockState jade = ModBlocks.JADE_STONE.defaultBlockState(), mirage = ModBlocks.MIRAGE_JADE.defaultBlockState();
		BlockState rail = Blocks.DIORITE_WALL.defaultBlockState(), pier = Blocks.CALCITE.defaultBlockState();
		int lastPier = -10;
		for (double t = 0; t <= len; t += 0.5) {
			double along = t / len;
			for (int k = -2; k <= 2; k++) {
				int x = (int) Math.round(a.x() + ux * t - uz * k), z = (int) Math.round(a.z() + uz * t + ux * k);
				MirageLake.Column col = MirageLake.column(s, x, z);
				if (col.kind() != MirageLake.Kind.LAKE) continue;
				BlockPos at = c.offset(x, 0, z).atY(DECK_Y);
				level.getChunk(at.getX() >> 4, at.getZ() >> 4);
				boolean span = fake && falseSpan(along);
				if (Math.abs(k) <= 1) {
					put(level, at, span ? mirage : jade);
					put(level, at.above(), Blocks.AIR.defaultBlockState()); // a rail of a bridge it crosses taken away
				} else if (!level.getBlockState(at).is(ModBlocks.JADE_STONE) && !level.getBlockState(at).is(ModBlocks.MIRAGE_JADE)) {
					put(level, at, span ? mirage : jade);
					put(level, at.above(), rail); // never over another bridge's deck where two meet
				}
				// Piers to the bed under the real deck, every few blocks along its middle.
				if (k == 0 && !span && (int) t - lastPier >= 6) {
					lastPier = (int) t;
					for (int y = DECK_Y - 1; y > col.ground(); y--) put(level, at.atY(y), pier);
				}
			}
		}
		join(level, c.offset(Math.min(a.x(), b.x()) - 3, 0, Math.min(a.z(), b.z()) - 3).atY(MirageLake.BED_Y - 4),
				c.offset(Math.max(a.x(), b.x()) + 3, 0, Math.max(a.z(), b.z()) + 3).atY(DECK_Y + 2));
	}

	/** The pavilion's base: an eight-sided terrace of white stone ringed in jade on the heart isle, steps down its front to the landing's bridge. */
	static void pavilionBase(ServerLevel level, MirageLake.Shape s, BlockPos c) {
		Frame p = pavilion(s, c);
		for (int cx = (c.getX() - 24) >> 4; cx <= (c.getX() + 24) >> 4; cx++) for (int cz = (c.getZ() - 24) >> 4; cz <= (c.getZ() + 24) >> 4; cz++) level.getChunk(cx, cz);
		for (int f = -13; f <= 13; f++) {
			for (int sd = -13; sd <= 13; sd++) {
				int edge = Math.abs(f) + Math.abs(sd);
				if (edge > 21) continue;
				BlockPos at = p.at(f, sd, 0);
				boolean rim = edge >= 20 || Math.abs(f) == 13 || Math.abs(sd) == 13;
				put(level, at, rim ? ModBlocks.JADE_STONE.defaultBlockState() : (f + sd) % 4 == 0 ? Blocks.POLISHED_DIORITE.defaultBlockState()
						: Blocks.SMOOTH_QUARTZ.defaultBlockState());
				int ground = groundAt(SecretRealm.JADE_LAKE_MIRAGE, s.seed, at.getX(), at.getZ());
				for (int y = FLOOR - 1; y > Math.min(ground, FLOOR - 1) - 1 && y >= FLOOR - 6; y--) put(level, at.atY(y), Blocks.CALCITE.defaultBlockState());
				for (int y = 1; y <= 16; y++) put(level, at.above(y), Blocks.AIR.defaultBlockState());
			}
		}
		// Its steps, down the front toward the landing's bridge.
		for (int k = 1; k <= 4; k++) {
			for (int sd = -2; sd <= 2; sd++) {
				BlockPos at = p.at(13 + k, sd, -k);
				put(level, at, stairs(Blocks.QUARTZ_STAIRS, p.forward().getOpposite(), false));
				for (int y = 1; y <= 4; y++) put(level, at.above(y), Blocks.AIR.defaultBlockState());
				for (int y = -1; y >= -3; y--) if (level.getBlockState(at.above(y)).canBeReplaced()) put(level, at.above(y), Blocks.CALCITE.defaultBlockState());
			}
		}
		join(level, p.at(-20, -20, -8), p.at(20, 20, 17));
	}

	/**
	 * The pavilion: its front hall open on pillars of quartz, the great mirror across its middle in a frame of gold, the hall behind it walled
	 * (its altar of jade, candles on it), one roof of jade tiles over all; lanterns hung within.
	 */
	static void pavilionHalls(ServerLevel level, MirageLake.Shape s, BlockPos c) {
		Frame p = pavilion(s, c);
		BlockState pillar = Blocks.QUARTZ_PILLAR.defaultBlockState(), wall = Blocks.QUARTZ_BRICKS.defaultBlockState(), gold = Blocks.GOLD_BLOCK.defaultBlockState();
		// The front hall's pillars and the lintels on them.
		for (int f : new int[] {2, 6, 10}) {
			for (int sd = -9; sd <= 9; sd += 18) for (int y = 1; y <= 6; y++) put(level, p.at(f, sd, y), pillar);
		}
		for (int sd = -5; sd <= 5; sd += 10) for (int y = 1; y <= 6; y++) put(level, p.at(10, sd, y), pillar);
		for (int f = 1; f <= 10; f++) for (int sd = -9; sd <= 9; sd += 18) put(level, p.at(f, sd, 7), wall);
		for (int sd = -9; sd <= 9; sd++) put(level, p.at(10, sd, 7), wall);
		// The mirror's wall, its frame of gold, the mirror.
		for (int sd = -9; sd <= 9; sd++) for (int y = 1; y <= 7; y++) put(level, p.at(0, sd, y), Math.abs(sd) == 3 && y <= 7 || y == 7 && Math.abs(sd) <= 3 ? gold : wall);
		for (BlockPos at : mirror(p)) put(level, at, ModBlocks.JADE_MIRROR.defaultBlockState());
		// The hall behind it: walled, its altar of jade with candles.
		for (int f = -12; f <= -1; f++) {
			for (int sd = -8; sd <= 8; sd++) {
				boolean side = Math.abs(sd) == 8 || f == -12;
				for (int y = 1; y <= 7; y++) put(level, p.at(f, sd, y), side ? wall : Blocks.AIR.defaultBlockState());
			}
		}
		put(level, p.at(-7, 0, 1), ModBlocks.JADE_STONE.defaultBlockState());
		for (int sd = -1; sd <= 1; sd += 2) put(level, p.at(-7, sd, 1), Blocks.QUARTZ_STAIRS.defaultBlockState());
		put(level, p.at(-7, 0, 2), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		// One roof of jade tiles over all, lanterns hung from it.
		HerbValleyWorks.hipRoof(level, p, -13, 11, -10, 10, 8, Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_SLAB, Blocks.PRISMARINE_BRICKS);
		for (int f : new int[] {-9, -4, 4, 8}) put(level, p.at(f, 0, 7), lantern(true));
		join(level, p.at(-14, -11, 0), p.at(13, 11, 18));
	}

	/** A willow by the water: a dark trunk, a crown of leaves, and long strands hanging from it to the water. */
	static void willow(ServerLevel level, BlockPos base, RandomSource random) {
		int height = 5 + random.nextInt(3);
		for (int y = 1; y <= height; y++) put(level, base.above(y), log(Blocks.DARK_OAK_LOG));
		BlockPos top = base.above(height);
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = -1; dy <= 1; dy++) {
					double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 2.5);
					if (d > 3.2 || dx == 0 && dz == 0 && dy < 1) continue;
					BlockPos at = top.offset(dx, dy, dz);
					if (level.getBlockState(at).canBeReplaced()) put(level, at, leaves(Blocks.OAK_LEAVES));
				}
				// Strands down from the crown's rim.
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r > 2.2 && r < 3.3 && random.nextFloat() < 0.6f) {
					int hang = 2 + random.nextInt(3);
					for (int k = 1; k <= hang; k++) {
						BlockPos at = top.offset(dx, -1 - k, dz);
						if (!level.getBlockState(at).canBeReplaced() || !level.getFluidState(at).isEmpty()) break;
						put(level, at, leaves(Blocks.OAK_LEAVES));
					}
				}
			}
		}
	}

	/** A blossom tree: a crooked trunk, a broad crown of blossom, petals fallen about it. */
	static void blossom(ServerLevel level, BlockPos base, RandomSource random) {
		int height = 4 + random.nextInt(2);
		Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		BlockPos at = base;
		for (int y = 1; y <= height; y++) {
			at = at.above();
			if (y == height - 1) at = at.relative(lean);
			put(level, at, log(Blocks.CHERRY_LOG));
		}
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				for (int dy = 0; dy <= 2; dy++) {
					double d = Math.sqrt(dx * dx + dz * dz) + dy * 0.9;
					if (d > 3.3 || random.nextFloat() < 0.08f) continue;
					BlockPos leaf = at.offset(dx, dy, dz);
					if (level.getBlockState(leaf).canBeReplaced()) put(level, leaf, leaves(Blocks.CHERRY_LEAVES));
				}
			}
		}
		for (int k = 0; k < 6; k++) {
			BlockPos petal = base.offset(random.nextInt(7) - 3, 1, random.nextInt(7) - 3);
			if (level.getBlockState(petal).isAir() && level.getBlockState(petal.below()).is(Blocks.GRASS_BLOCK)) {
				put(level, petal, Blocks.PINK_PETALS.defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + random.nextInt(4))
						.setValue(PinkPetalsBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random)));
			}
		}
	}

	/** The trees: a quarter of them each step: willows by the isles' shores, blossoms within them, both on the hills; four blossoms by the pavilion. */
	static void trees(ServerLevel level, MirageLake.Shape s, BlockPos c, int part, RandomSource random) {
		SecretRealm realm = SecretRealm.JADE_LAKE_MIRAGE;
		if (part == 0) {
			Frame p = pavilion(s, c);
			for (int i = 0; i < 4; i++) {
				BlockPos at = p.at(i < 2 ? 16 : -16, i % 2 == 0 ? -15 : 15, 0);
				int g = groundAt(realm, s.seed, at.getX(), at.getZ());
				if (g > MirageLake.WATER_Y) blossom(level, at.atY(g), random);
			}
		}
		for (int i = part; i < s.isles.size(); i += 4) {
			MirageLake.Isle isle = s.isles.get(i);
			for (int k = 0; k < 2; k++) {
				double a = random.nextDouble() * Math.PI * 2, d = k == 0 ? isle.r() - 2 : isle.r() * 0.4;
				int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
				MirageLake.Column col = MirageLake.column(s, x, z);
				if (col.kind() != MirageLake.Kind.ISLE || col.ground() <= MirageLake.WATER_Y + 1) continue;
				BlockPos at = c.offset(x, 0, z).atY(col.ground());
				level.getChunk(at.getX() >> 4, at.getZ() >> 4);
				if (k == 0) willow(level, at, random);
				else if (random.nextBoolean() && !isle.role().equals("arrival")) blossom(level, at, random);
			}
		}
		// The hills: willows at the shore's foot, blossoms higher.
		for (int k = 0; k < 14; k++) {
			double a = random.nextDouble() * Math.PI * 2, d = MirageLake.WALL_R + 6 + random.nextDouble() * 16;
			int x = (int) Math.round(Math.cos(a) * d), z = (int) Math.round(Math.sin(a) * d);
			MirageLake.Column col = MirageLake.column(s, x, z);
			if (col.kind() != MirageLake.Kind.HILL || !level.getBlockState(c.offset(x, 0, z).atY(col.ground())).is(Blocks.GRASS_BLOCK)) continue;
			BlockPos at = c.offset(x, 0, z).atY(col.ground());
			if (col.ground() < MirageLake.WATER_Y + 6) willow(level, at, random);
			else blossom(level, at, random);
		}
	}

	static void arrivalTerraces(ServerLevel level, MirageLake.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0x3A1A7L);
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), (random.nextFloat() < 0.3f ? Blocks.POLISHED_DIORITE : Blocks.SMOOTH_QUARTZ).defaultBlockState());
					for (int y = 1; y <= 4; y++) put(level, at.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
			for (int side = -3; side <= 3; side += 6) {
				put(level, at.offset(side, 1, 0), Blocks.DIORITE_WALL.defaultBlockState());
				put(level, at.offset(side, 2, 0), lantern(false));
			}
			join(level, at.offset(-4, 0, -4), at.offset(4, 4, 4));
		}
	}

	// --- Renewing ---

	/** The lotuses: each spot this opening a real spirit lotus (aged by the years shut) or a mirage of one, by its own roll. */
	static void renewLotuses(ServerLevel level, MirageLake.Shape s, BlockPos c, SecretRealms.Record r) {
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x3A108L);
		int bonus = r.realm.years() * HerbValleyWorks.YEARS_A_YEAR_SHUT;
		for (BlockPos at : lotusSpots(s, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (random.nextFloat() < 0.4f) put(level, at, ModBlocks.MIRAGE_LOTUS.defaultBlockState());
			else HerbValleyWorks.renewHerb(level, at, ModBlocks.SPIRIT_LOTUS, 600 + random.nextInt(2400), bonus, null);
		}
	}

	/** Two vaults sealed anew for the ancients and two mirage caskets in their look, which isle keeps which rolled each opening. */
	static void renewTreasures(ServerLevel level, MirageLake.Shape s, BlockPos c, SecretRealms.Record r) {
		List<MirageLake.Isle> isles = new ArrayList<>(treasureIsles(s));
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x3A109L);
		for (int i = isles.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			MirageLake.Isle t = isles.get(i);
			isles.set(i, isles.get(j));
			isles.set(j, t);
		}
		for (int k = 0; k < isles.size(); k++) {
			BlockPos at = treasureAt(s, c, isles.get(k));
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (k >= 2) {
				put(level, at, ModBlocks.MIRAGE_CASKET.defaultBlockState());
				continue;
			}
			if (!(level.getBlockState(at).getBlock() instanceof StorageFormationBlock)) put(level, at, ModBlocks.storageFormation(StorageFormations.Tier.HIGH).defaultBlockState());
			if (level.getBlockEntity(at) instanceof StorageFormationBlockEntity vault) vault.sealForAncients(level, vaultTreasures(level.getRandom()));
		}
	}

	/** A vault of the lake's sect: spirit stones, now and then an artifact or a Departure Jade. */
	static List<ItemStack> vaultTreasures(RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 3 + random.nextInt(4)));
		if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.SUPREME_SPIRIT_STONE));
		if (random.nextFloat() < 0.3f) out.add(Artifacts.any(random, ArtifactGrade.EARTH, null));
		if (random.nextFloat() < 0.3f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return out;
	}

	/** The mirror whole (one struck out of it by some spell put back). */
	static void renewMirror(ServerLevel level, MirageLake.Shape s, BlockPos c) {
		Frame p = pavilion(s, c);
		level.getChunk(p.origin().getX() >> 4, p.origin().getZ() >> 4);
		for (BlockPos at : mirror(p)) if (!level.getBlockState(at).is(ModBlocks.JADE_MIRROR)) put(level, at, ModBlocks.JADE_MIRROR.defaultBlockState());
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : MirageLake.arrivals(MirageLake.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	/** An earlier visitor's bones: on one of the isles. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		MirageLake.Shape s = MirageLake.shape(seed);
		MirageLake.Isle isle = s.isles.get(random.nextInt(s.isles.size()));
		double a = random.nextDouble() * Math.PI * 2, d = isle.r() * random.nextDouble() * 0.7;
		int x = isle.x() + (int) Math.round(Math.cos(a) * d), z = isle.z() + (int) Math.round(Math.sin(a) * d);
		MirageLake.Column col = MirageLake.column(s, x, z);
		return col.kind() == MirageLake.Kind.ISLE ? c.offset(x, 0, z).atY(col.ground() + 1) : null;
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.HIGH_SPIRIT_STONE, 1 + random.nextInt(3)));
		carried.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 3 + random.nextInt(6)));
		if (random.nextFloat() < 0.2f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.1f) carried.add(Artifacts.any(random, ArtifactGrade.EARTH, null));
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		MirageLake.Shape s = MirageLake.shape(seed);
		List<Slot> slots = new ArrayList<>();
		int k = 0;
		for (MirageLake.Isle isle : s.isles) {
			if (isle.role().equals("arrival")) continue;
			BlockPos at = isleFrame(c, isle).at(2, 1, 0);
			slots.add(new Slot("isle" + k++, ModEntities.MIRROR_SHADE, at.atY(groundAt(realm, seed, at.getX(), at.getZ()) + 1),
					realm.guardianRank(isle.r() >= 11 ? Stage.MID : Stage.EARLY), false));
		}
		// On the real bridges' middles, every other one.
		List<MirageLake.Isle> nodes = nodes(s);
		List<int[]> bridges = bridges(s);
		for (int b = 0; b < bridges.size(); b += 2) {
			if (isFalse(s, bridges.get(b))) continue;
			MirageLake.Isle from = nodes.get(bridges.get(b)[0]), to = nodes.get(bridges.get(b)[1]);
			double dx = to.x() - from.x(), dz = to.z() - from.z(), len = Math.max(1, Math.hypot(dx, dz));
			double t = (from.r() + (len - to.r() - from.r()) / 2) / len;
			int x = (int) Math.round(from.x() + dx * t), z = (int) Math.round(from.z() + dz * t);
			if (MirageLake.column(s, x, z).kind() != MirageLake.Kind.LAKE) continue;
			slots.add(new Slot("bridge" + b, ModEntities.MIRROR_SHADE, c.offset(x, 0, z).atY(DECK_Y + 1), realm.guardianRank(Stage.EARLY), false));
		}
		Frame p = pavilion(s, c);
		for (int side = -1; side <= 1; side += 2) {
			slots.add(new Slot("steps" + (side + 1), ModEntities.MIRROR_SHADE, p.at(11, side * 6, 1), realm.guardianRank(Stage.LATE), false));
			slots.add(new Slot("hall" + (side + 1), ModEntities.MIRROR_SHADE, p.at(4, side * 7, 1), realm.guardianRank(Stage.LATE), false));
		}
		return slots;
	}

	/** The heart isle holds: the closing quake splits only the isles and the hills. */
	@Override
	protected int calm() { return MirageLake.HEART_R + 4; }

	/** The hall behind the mirror holds: it is gone into through the mirror, never round it. */
	@Override
	protected String holds(ServerLevel level, SecretRealms.Record r, BlockPos pos) {
		MirageLake.Shape s = MirageLake.shape(level.getSeed());
		return ofInnerHall(pavilion(s, r.realm.centre()).local(pos)) ? ModLang.MSG_MIRAGE_HALL_HOLDS : null;
	}

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("WakeAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Wake", 0);
	}

	@Override
	protected void end(ServerLevel level, SecretRealms.Record r) {
		for (MirrorReflection m : reflections(level, r.realm)) m.shatter();
	}

	static List<MirrorReflection> reflections(ServerLevel level, SecretRealm realm) {
		return level.getEntitiesOfClass(MirrorReflection.class, new AABB(realm.centre()).inflate(SecretRealm.BOUND, 160, SecretRealm.BOUND));
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		MirageLake.Shape s = MirageLake.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		wake(level, s, c, r, now, inside, data);
		if (now % 20 == 9) {
			// The trial told to whoever comes to the pavilion; the legacy to one who bested their reflection, before the altar.
			Frame p = pavilion(s, c);
			BlockPos altar = altar(p);
			ListTag told = r.works.getList("Told", Tag.TAG_STRING);
			for (ServerPlayer pl : inside) {
				if (pl.blockPosition().distSqr(p.origin()) < 34 * 34 && !told.contains(StringTag.valueOf(pl.getStringUUID()))) {
					told.add(StringTag.valueOf(pl.getStringUUID()));
					pl.sendSystemMessage(Component.translatable(ModLang.MSG_MIRAGE_TRIALS, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_MIRAGE_GREAT
							: ModLang.MSG_MIRAGE_LESSER)).withStyle(ChatFormatting.GOLD));
				}
				if (pl.blockPosition().distSqr(altar) < 3 * 3 && passed(r, pl.getUUID()) && !passed(r, "Claimed", pl.getUUID())) {
					pass(r, "Claimed", pl.getUUID());
					inherit(level, r, pl);
				}
			}
			r.works.put("Told", told);
		}
	}

	/** The mirror wakes: each visitor's reflection rises from the lake beside them, at half their strength, for a minute. */
	private static void wake(ServerLevel level, MirageLake.Shape s, BlockPos c, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int wake = r.works.getInt("Wake");
		if (wake == 0 && now >= r.works.getLong("WakeAt")) {
			r.works.putInt("Wake", 1);
			long ends = now + WAKE_TICKS;
			r.works.putLong("WakeEnds", ends);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_MIRAGE_WAKES).withStyle(ChatFormatting.AQUA));
			for (ServerPlayer p : inside) {
				if (p.isCreative() || p.isSpectator()) continue;
				MirrorReflection.raise(level, p, risingBeside(level, p), p.getYRot() + 180, true, ends);
			}
		} else if (wake == 1 && now >= r.works.getLong("WakeEnds")) {
			r.works.putInt("Wake", 2);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_MIRAGE_WAKE_ENDS).withStyle(ChatFormatting.GRAY));
		}
	}

	/** Where one's reflection rises: out of the nearest water within a few blocks of them, else beside them. */
	static Vec3 risingBeside(ServerLevel level, ServerPlayer p) {
		BlockPos at = p.blockPosition();
		for (int d = 2; d <= 6; d++) {
			for (int k = 0; k < 8; k++) {
				BlockPos w = at.offset((int) Math.round(Math.cos(k * Math.PI / 4) * d), 0, (int) Math.round(Math.sin(k * Math.PI / 4) * d)).atY(MirageLake.WATER_Y);
				if (level.getFluidState(w).isSource() && level.getBlockState(w.above()).isAir()) return Vec3.atBottomCenterOf(w.above());
			}
		}
		Vec3 back = Vec3.directionFromRotation(0, p.getYRot()).scale(-2);
		return p.position().add(back.x, 0, back.z);
	}

	/** {@code p} touched the great mirror: their reflection steps out, unless they've bested it, or it already stands. */
	static void mirrorTouched(ServerLevel level, BlockPos pos, ServerPlayer p) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm != SecretRealm.JADE_LAKE_MIRAGE || p.isSpectator()) return;
		SecretRealms.Record r = SecretRealms.Data.get(level.getServer()).of(realm);
		if (r.openedAt < 0) return;
		if (passed(r, p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_MIRAGE_MIRROR_OPEN).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		if (p.isCreative()) return;
		for (MirrorReflection m : reflections(level, realm)) if (m.isTrial() && p.getUUID().equals(m.getOwnerId())) return;
		Frame f = pavilion(MirageLake.shape(level.getSeed()), realm.centre());
		BlockPos out = stepsOut(f);
		if (MirrorReflection.raise(level, p, Vec3.atBottomCenterOf(out), f.forward().toYRot(), false, -1) != null) {
			p.sendSystemMessage(Component.translatable(ModLang.MSG_MIRAGE_REFLECTION).withStyle(ChatFormatting.AQUA));
		}
	}

	/** {@code owner}'s reflection, the mirror's, bested: the mirror is a door to them now. */
	static void reflectionBested(ServerLevel level, ServerPlayer owner) {
		passedGate(level, SecretRealm.JADE_LAKE_MIRAGE, owner.getUUID(), owner);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_MIRAGE_MIRROR_PASSED);
	}

	// --- Its inheritance ---

	/** The Great Inheritance: the Jade Lake Mirror; supreme spirit stones from the altar. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(new ItemStack(ModItems.JADE_LAKE_MIRROR), new ItemStack(ModItems.SUPREME_SPIRIT_STONE, 3));
	}

	/** A lesser legacy: a Heaven-grade artifact of the lake's sect, spirit stones. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(Artifacts.any(random, ArtifactGrade.HEAVEN, null), new ItemStack(ModItems.HIGH_SPIRIT_STONE, 4 + random.nextInt(4)));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_MIRAGE_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_MIRAGE_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_MIRAGE_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/** On one of the isles, the nearer the heart the later. */
	@Override
	protected BlockPos rivalStart(ServerLevel level, long seed, BlockPos c, double along, RandomSource random) {
		MirageLake.Shape s = MirageLake.shape(seed);
		MirageLake.Isle best = null;
		double want = MirageLake.ARRIVAL_R - along * (MirageLake.ARRIVAL_R - MirageLake.HEART_R - 30), d = Double.MAX_VALUE;
		for (MirageLake.Isle isle : s.isles) {
			double here = Math.abs(Math.hypot(isle.x(), isle.z()) - want) + random.nextDouble() * 12;
			if (here < d) {
				d = here;
				best = isle;
			}
		}
		return c.offset(best.x() + random.nextInt(3) - 1, 0, best.z() + random.nextInt(3) - 1);
	}

	/** The nearest lotus by the isles, real or not: one that goes for a mirage gets nothing but mist. */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		BlockPos best = null;
		double d = 40 * 40;
		for (BlockPos at : lotusSpots(MirageLake.shape(seed), c)) {
			double here = at.distSqr(near);
			if (here < d && level.isLoaded(at) && (level.getBlockState(at).getBlock() instanceof SpiritLotusBlock || level.getBlockState(at).getBlock() instanceof MirageBlock)) {
				d = here;
				best = at;
			}
		}
		return best;
	}

	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		if (level.getBlockState(at).getBlock() instanceof MirageBlock) {
			MirageBlock.dissolve(level, at, null);
			return List.of();
		}
		return HerbValleyWorks.WORKS.rivalTake(level, at, npc);
	}

	@Override
	protected List<BlockPos> vaults(long seed, BlockPos c) {
		List<BlockPos> out = new ArrayList<>();
		MirageLake.Shape s = MirageLake.shape(seed);
		for (MirageLake.Isle isle : treasureIsles(s)) out.add(treasureAt(s, c, isle));
		return out;
	}

	/** Before the mirror (its own reflection faced); through it; the altar. */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		Frame p = pavilion(MirageLake.shape(level.getSeed()), r.realm.centre());
		UUID id = npc.getUUID();
		if (passed(r, "Claimed", id)) return null;
		if (!passed(r, id)) return new Trial("mirror", beforeMirror(p), 2.5, true);
		if (!ofInnerHall(p.local(npc.blockPosition()))) return new Trial("door", beforeMirror(p), 2.5, true);
		return new Trial("altar", altar(p), 2.0, true);
	}

	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		switch (trial.step()) {
			case "mirror" -> {
				// Its own reflection faced in the mirror's light: a while, then bested or not, by its strength against the realm's.
				if (working % 10 == 0) level.sendParticles(ParticleTypes.END_ROD, npc.getX(), npc.getY() + 1.2, npc.getZ(), 6, 0.5, 0.8, 0.5, 0.05);
				if (working == 0) level.playSound(null, npc.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4f, 0.5f);
				if (working < 160) return false;
				double odds = npc.getRank() >= r.realm.guardianRank(Stage.MID) ? 0.75 : 0.45;
				if (npc.getRandom().nextDouble() < odds) {
					level.playSound(null, npc.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
					passedGate(level, r.realm, npc.getUUID(), npc);
				} else {
					giveUp(r, npc.getUUID());
				}
			}
			case "door" -> {
				// The mirror lets it by: set down behind it.
				BlockPos in = pavilion(MirageLake.shape(level.getSeed()), r.realm.centre()).at(-2, 0, 1);
				npc.getNavigation().stop();
				npc.teleportTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
			}
			case "altar" -> {
				pass(r, "Claimed", npc.getUUID());
				rivalInherits(level, r, npc);
			}
			default -> {
			}
		}
		return true;
	}

	private MirageLakeWorks() {}
}
