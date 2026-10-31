package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Cave Abode of a Heavenly Being's works (SecretRealm#CAVE_ABODE; its land is CaveAbode's, the machinery every realm shares RealmWorks').
 * <ul>
 *   <li><b>Laid once</b>: the abode hollowed out of its mountain ({@link Room}, in its own frame: {@link #frame}). Its gate at the mountain's
 *   foot on the main way, set into the slope, a porch of white jade pillars roofed in dark tiles, a causeway across the lake to it; within, a
 *   long hall with a library and a hall of spirit springs either side, then the lecture hall and the master's dais; at the heart the garden
 *   court under a shaft of sky cut down through the peak (its pond, its herb beds); off the garden the hall of the five elements, the maze, and
 *   behind a trial's barrier the master's chamber. Pines in the basin; a terrace at each arrival.</li>
 *   <li><b>Renewed each time it stirs</b>: the garden's herbs and the pond's lotuses (older by every year it slept); the library's and the
 *   springs' vaults sealed anew for the ancients; the five pillars dark; the chamber's barrier whole and the master's bones in their place; the
 *   bones of a few earlier visitors.</li>
 *   <li><b>Its guardians</b>: Bronze Puppets at the gate, at the causeway's far end, in the halls and in the maze; a Vine Keeper by the
 *   garden's herbs; before the chamber the master's own puppet, its last trial.</li>
 *   <li><b>Its event</b>: some time into each opening the master's voice lectures in the lecture hall for {@link #LECTURE_TICKS}: whoever is in
 *   the hall gathers five times as much at meditation (ModEffects#MASTERS_LECTURE), and one who sat through enough of it keeps an Insight
 *   after.</li>
 *   <li><b>Its trials</b>: light the five element pillars in the order each gives rise to the next, beginning where one will (out of turn, a
 *   backlash, and all five dark again); then walk the maze to its heart (its walls hold against every hand and spell: {@link #holds}); then
 *   best the master's puppet, matched to oneself; past its barrier, at the master's altar, the Great Inheritance (his own method and the
 *   Marrow-Washing Fruit) once in a world, after it a lesser legacy each opening.</li>
 * </ul>
 */
public final class CaveAbodeWorks extends RealmWorks {
	static final CaveAbodeWorks WORKS = new CaveAbodeWorks();
	/** The abode's floor: its paving's level (the basin's at the mountain's foot). */
	static final int FLOOR = CaveAbode.FLOOR_Y;
	/** The lecture lasts this long (two minutes); one who sat in meditation through this much of it (ticks) keeps an Insight after. */
	static final int LECTURE_TICKS = 2400, HEARD_ENOUGH = 600;
	/** A pillar touched out of turn strikes for this share of one's whole health; all five lit burn this long (ticks) before going dark. */
	static final double BACKLASH = 0.12;
	static final int PILLARS_REST = 60;
	/** The maze: its cells a side, each cell's size (a corridor three wide and its wall), its walls' height; its middle row (the heart's). */
	static final int CELLS = 7, CELL = 4, MAZE_WALL = 4, MAZE_MID = 3;
	/** The maze's near corner in the frame: its first row (its outer wall) along f, along s. */
	static final int MAZE_F = 13, MAZE_S = -14;
	/** The elements hall's middle (s; at f 0), the pillars' ring about it; the garden pond's reach. */
	static final int ELEMENTS_MIDDLE_S = -21;
	static final double PILLAR_RING = 4.5, POND_R = 3.6;

	/** A room hollowed out of the mountain: its inside (frame coordinates, inclusive), its height. */
	record Room(int f0, int f1, int s0, int s1, int h) {
		boolean inside(int f, int s) {
			return f >= f0 && f <= f1 && s >= s0 && s <= s1;
		}
	}

	static final Room TUNNEL = new Room(-56, -48, -2, 2, 5), HALL = new Room(-46, -36, -9, 9, 8), LIBRARY = new Room(-46, -36, -21, -11, 6),
			SPRINGS = new Room(-46, -36, 11, 21, 6), LECTURE = new Room(-33, -15, -10, 10, 11), GARDEN = new Room(-11, 11, -11, 11, 12),
			ELEMENTS = new Room(-7, 7, -27, -15, 9), CHAMBER = new Room(-5, 5, 15, 25, 8);

	/** The doorways cut once every room is hollowed: {f0, f1, s0, s1, height}. */
	static final int[][] DOORS = {
			{-58, -57, -2, 2, 5}, {-47, -47, -2, 2, 5}, // the gate's; the tunnel into the long hall
			{-42, -40, -10, -10, 4}, {-42, -40, 10, 10, 4}, // the library's; the springs'
			{-35, -34, -2, 2, 5}, // into the lecture hall
			{-14, -12, -1, 1, 4}, // behind the master's screen into the garden
			{-1, 1, -14, -12, 4}, // the elements hall's
			{12, 13, -1, 1, 3}, // the maze's
			{-1, 1, 13, 14, 3}}; // the chamber's (its barrier in the garden's wall)

	/** Where in the abode one stands (a doorway counted with the room it opens from), or outside it. */
	enum Zone { OUTSIDE, TUNNEL, HALL, LECTURE, GARDEN, ELEMENTS, MAZE, CHAMBER }

	static Zone zone(int[] at) {
		int f = at[0], s = at[1], y = at[2];
		if (y < -2 || y > 14) return Zone.OUTSIDE;
		if (f >= MAZE_F && f <= MAZE_F + CELLS * CELL && s >= MAZE_S && s <= MAZE_S + CELLS * CELL) return Zone.MAZE;
		if (f >= -6 && f <= 6 && s >= 13 && s <= 26) return Zone.CHAMBER;
		if (f >= -8 && f <= 8 && s >= -28 && s <= -13) return Zone.ELEMENTS;
		if (f >= -13 && f <= 12 && s >= -12 && s <= 12) return Zone.GARDEN;
		if (f >= -34 && f <= -14 && s >= -11 && s <= 11) return Zone.LECTURE;
		if (f >= -47 && f <= -35 && s >= -22 && s <= 22) return Zone.HALL;
		if (f >= -58 && f <= -47 && s >= -3 && s <= 3) return Zone.TUNNEL;
		return Zone.OUTSIDE;
	}

	// --- Where things are ---

	/** The main way out from the mountain (the gate faces it), as a direction. */
	static Direction mainWay(CaveAbode.Shape s) {
		return Direction.fromDelta((int) Math.round(Math.cos(s.mainTurn)), 0, (int) Math.round(Math.sin(s.mainTurn)));
	}

	/** The abode's frame: its origin the mountain's middle at the floor, forward from the gate inward (f), s to the right of that. */
	static Frame frame(CaveAbode.Shape s, BlockPos c) {
		return new Frame(c.atY(FLOOR), mainWay(s).getOpposite());
	}

	/** {@code pos} in {@code fr}'s coordinates: (f, s, y). */
	static int[] local(Frame fr, BlockPos pos) {
		return fr.local(pos);
	}

	/** The master's element (the world's own): his method's. */
	static QiElement masterElement(long seed) {
		return QiElement.five()[Math.floorMod((int) (seed ^ seed >>> 32 ^ 0xCA7E), 5)];
	}

	/** The master's own name (his bones', in his chamber): the world's own, the same every time. */
	public static String masterName(ServerLevel level) {
		return NpcNames.of(new UUID(level.getSeed(), 0xCA7EAB0DL));
	}

	/** Where the master's puppet keeps its post: in the garden before the chamber's barrier. */
	static BlockPos puppetPost(Frame fr) { return fr.at(0, 9, 1); }

	/** The master's low table before his seat, where what he left waits. */
	static BlockPos altar(Frame fr) { return fr.at(0, 21, 1); }

	/** Where his bones lie: in his seat at the chamber's back. */
	static BlockPos masterBones(Frame fr) { return fr.at(0, 24, 1); }

	/** The seat on the lecture hall's dais (the master's voice comes from it). */
	static BlockPos lectureSeat(Frame fr) { return fr.at(-18, 0, 2); }

	/** The chamber's doorway in the garden's east wall: the barrier, three wide and three high. */
	static List<BlockPos> barrier(Frame fr) {
		List<BlockPos> out = new ArrayList<>();
		for (int f = -1; f <= 1; f++) for (int y = 1; y <= 3; y++) out.add(fr.at(f, 12, y));
		return out;
	}

	static BlockPos libraryVault(Frame fr) { return fr.at(-41, -20, 1); }

	static BlockPos springsVault(Frame fr) { return fr.at(-41, 20, 1); }

	/** The garden's herb spots: two in each of its four beds. */
	static List<BlockPos> herbSpots(Frame fr) {
		List<BlockPos> out = new ArrayList<>();
		for (int bf = -7; bf <= 7; bf += 14) for (int bs = -7; bs <= 7; bs += 14) {
			out.add(fr.at(bf - 1, bs - 1, 1));
			out.add(fr.at(bf + 1, bs + 1, 1));
		}
		return out;
	}

	/** The pond's lotus spots, on its water. */
	static List<BlockPos> lotusSpots(Frame fr) {
		return List.of(fr.at(2, 0, 1), fr.at(-1, 2, 1), fr.at(-1, -2, 1));
	}

	/**
	 * The five pillars' places in a ring about the elements hall's middle (each its lower stone), each with its element: the world's own
	 * order round the ring, so their order must be known, not read off it. The ring's point lies away from the door, two pillars either side
	 * of the way in.
	 */
	static Map<ElementPillarBlock.Element, BlockPos> pillars(CaveAbode.Shape s, BlockPos c) {
		Frame fr = frame(s, c);
		List<ElementPillarBlock.Element> order = new ArrayList<>(List.of(ElementPillarBlock.Element.values()));
		Collections.shuffle(order, new java.util.Random(s.seed ^ 0x5E1E7L));
		Map<ElementPillarBlock.Element, BlockPos> at = new EnumMap<>(ElementPillarBlock.Element.class);
		for (int k = 0; k < order.size(); k++) {
			double a = Math.PI / 2 + Math.PI / order.size() + k * Math.PI * 2 / order.size();
			at.put(order.get(k), fr.at((int) Math.round(Math.cos(a) * PILLAR_RING), ELEMENTS_MIDDLE_S + (int) Math.round(Math.sin(a) * PILLAR_RING), 2));
		}
		return at;
	}

	// --- The maze ---

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	/** A world's maze (its own roll): which walls between its cells stand open, and the way from its entrance (its near side's middle) to its heart. */
	static final class Maze {
		/** Each cell's open sides: 1, toward the next cell along f; 2, toward the next along s. */
		final int[][] open;
		final List<int[]> way;

		Maze(int[][] open) {
			this.open = open;
			this.way = path(new int[] {0, MAZE_MID}, new int[] {MAZE_MID, MAZE_MID});
		}

		/** Whether one may go from cell (i, j) on to (i + di, j + dj). */
		boolean passes(int i, int j, int di, int dj) {
			int ni = i + di, nj = j + dj;
			if (ni < 0 || nj < 0 || ni >= CELLS || nj >= CELLS) return false;
			if (di != 0) return (open[Math.min(i, ni)][j] & 1) != 0;
			return (open[i][Math.min(j, nj)] & 2) != 0;
		}

		/** The cells from {@code from} to {@code to}, both with them: the one way between (a maze carved so has but one). */
		List<int[]> path(int[] from, int[] to) {
			int[][] prev = new int[CELLS * CELLS][];
			boolean[] seen = new boolean[CELLS * CELLS];
			ArrayDeque<int[]> queue = new ArrayDeque<>();
			queue.add(from);
			seen[from[0] * CELLS + from[1]] = true;
			while (!queue.isEmpty()) {
				int[] at = queue.poll();
				if (at[0] == to[0] && at[1] == to[1]) break;
				for (int[] d : SIDES) {
					if (!passes(at[0], at[1], d[0], d[1])) continue;
					int[] next = {at[0] + d[0], at[1] + d[1]};
					int k = next[0] * CELLS + next[1];
					if (seen[k]) continue;
					seen[k] = true;
					prev[k] = at;
					queue.add(next);
				}
			}
			LinkedList<int[]> out = new LinkedList<>();
			for (int[] at = to; at != null; at = prev[at[0] * CELLS + at[1]]) {
				out.addFirst(at);
				if (at[0] == from[0] && at[1] == from[1]) break;
			}
			return out;
		}

		/** Whether maze block (mx, mz) (from its near corner, 0 to CELLS * CELL) is wall: the grid's lines, but where two cells open on each other, and its entrance. */
		boolean wall(int mx, int mz) {
			boolean onF = mx % CELL == 0, onS = mz % CELL == 0;
			if (onF && onS) return true;
			if (onF) {
				int i = mx / CELL, j = mz / CELL;
				if (i == 0) return j != MAZE_MID;
				return i >= CELLS || (open[i - 1][j] & 1) == 0;
			}
			if (onS) {
				int i = mx / CELL, j = mz / CELL;
				return j == 0 || j >= CELLS || (open[i][j - 1] & 2) == 0;
			}
			return false;
		}
	}

	private static final Map<Long, Maze> MAZES = Collections.synchronizedMap(new HashMap<>());

	/** The world's maze. */
	static Maze maze(long seed) {
		return MAZES.computeIfAbsent(seed, CaveAbodeWorks::carveMaze);
	}

	/** Carved by a walk that never crosses its own track (each cell reached once); of a few such, the first whose way to the heart is long, else the longest. */
	private static Maze carveMaze(long seed) {
		Maze best = null;
		for (int k = 0; k < 24 && (best == null || best.way.size() < 16); k++) {
			RandomSource random = RandomSource.create(seed ^ 0x3A2E1L ^ k * 0x9E3779B97F4A7C15L);
			int[][] open = new int[CELLS][CELLS];
			boolean[][] seen = new boolean[CELLS][CELLS];
			ArrayDeque<int[]> stack = new ArrayDeque<>();
			stack.push(new int[] {0, MAZE_MID});
			seen[0][MAZE_MID] = true;
			while (!stack.isEmpty()) {
				int[] at = stack.peek();
				List<int[]> next = new ArrayList<>();
				for (int[] d : SIDES) {
					int i = at[0] + d[0], j = at[1] + d[1];
					if (i >= 0 && j >= 0 && i < CELLS && j < CELLS && !seen[i][j]) next.add(new int[] {i, j});
				}
				if (next.isEmpty()) {
					stack.pop();
					continue;
				}
				int[] to = next.get(random.nextInt(next.size()));
				if (to[0] != at[0]) open[Math.min(at[0], to[0])][at[1]] |= 1;
				else open[at[0]][Math.min(at[1], to[1])] |= 2;
				seen[to[0]][to[1]] = true;
				stack.push(to);
			}
			Maze maze = new Maze(open);
			if (best == null || maze.way.size() > best.way.size()) best = maze;
		}
		return best;
	}

	/** The middle of maze cell (i, j), over its floor. */
	static BlockPos cellMiddle(Frame fr, int i, int j) {
		return fr.at(MAZE_F + i * CELL + CELL / 2, MAZE_S + j * CELL + CELL / 2, 1);
	}

	/** The maze cell at local (f, s, y), or null outside the maze. */
	static int[] cellAt(int[] at) {
		int mx = at[0] - MAZE_F, mz = at[1] - MAZE_S;
		if (mx <= 0 || mz <= 0 || mx >= CELLS * CELL || mz >= CELLS * CELL || at[2] < 0 || at[2] > MAZE_WALL + 1) return null;
		return new int[] {Math.min(mx / CELL, CELLS - 1), Math.min(mz / CELL, CELLS - 1)};
	}

	/**
	 * Whether {@code pos} is of the maze (its walls, floor and roof and the rock about them): kept from every hand and spell (CultivationEvents,
	 * HeavenSpells#guarded), or its way would be dug round.
	 */
	public static boolean holds(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel server) || !ModDimensions.isSecretRealm(level.dimension())) return false;
		if (SecretRealm.at(pos.getX(), pos.getZ()) != SecretRealm.CAVE_ABODE) return false;
		int[] at = local(frame(CaveAbode.shape(server.getSeed()), SecretRealm.CAVE_ABODE.centre()), pos);
		return at[0] >= MAZE_F - 1 && at[0] <= MAZE_F + CELLS * CELL + 1 && at[1] >= MAZE_S - 1 && at[1] <= MAZE_S + CELLS * CELL + 1 && at[2] >= -1
				&& at[2] <= MAZE_WALL + 2;
	}

	@Override
	protected String holds(ServerLevel level, SecretRealms.Record r, BlockPos pos) {
		return holds(level, pos) ? ModLang.MSG_ABODE_MAZE_HOLDS : null;
	}

	// --- The readying (RealmWorks#prepare) ---

	@Override
	protected void layOut(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		CaveAbode.Shape s = CaveAbode.shape(seed);
		Frame fr = frame(s, c);
		BlockState andesite = Blocks.POLISHED_ANDESITE.defaultBlockState(), bricks = Blocks.STONE_BRICKS.defaultBlockState(), smooth = Blocks.SMOOTH_STONE.defaultBlockState();
		steps.add(() -> gate(level, s, fr));
		steps.add(() -> {
			hollow(level, fr, TUNNEL, andesite, bricks, smooth);
			hollow(level, fr, HALL, andesite, bricks, smooth);
		});
		steps.add(() -> {
			hollow(level, fr, LIBRARY, Blocks.DARK_OAK_PLANKS.defaultBlockState(), bricks, smooth);
			hollow(level, fr, SPRINGS, Blocks.CALCITE.defaultBlockState(), bricks, smooth);
		});
		steps.add(() -> hollow(level, fr, LECTURE, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), bricks, Blocks.DARK_OAK_PLANKS.defaultBlockState()));
		steps.add(() -> hollow(level, fr, GARDEN, Blocks.GRASS_BLOCK.defaultBlockState(), bricks, smooth));
		steps.add(() -> skyWell(level, fr));
		steps.add(() -> hollow(level, fr, ELEMENTS, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICKS.defaultBlockState(),
				Blocks.POLISHED_DEEPSLATE.defaultBlockState()));
		steps.add(() -> mazeLaid(level, s, fr));
		steps.add(() -> hollow(level, fr, CHAMBER, Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.QUARTZ_BRICKS.defaultBlockState(),
				Blocks.SMOOTH_QUARTZ.defaultBlockState()));
		steps.add(() -> doors(level, fr));
		steps.add(() -> furnishHalls(level, s, fr));
		steps.add(() -> furnishLecture(level, fr));
		steps.add(() -> furnishGarden(level, s, fr));
		steps.add(() -> furnishElements(level, s, c, fr));
		steps.add(() -> furnishChamber(level, fr));
		steps.add(() -> arrivalTerraces(level, s, c));
		for (int part = 0; part < 4; part++) {
			int p = part;
			steps.add(() -> pines(level, s, c, p, RandomSource.create(seed ^ p * 0x91E5L)));
		}
	}

	@Override
	protected void renewal(Deque<Runnable> steps, ServerLevel level, long seed, BlockPos c, SecretRealms.Record r) {
		CaveAbode.Shape s = CaveAbode.shape(seed);
		Frame fr = frame(s, c);
		steps.add(() -> renewGarden(level, s, fr, r));
		steps.add(() -> renewVaults(level, fr));
		steps.add(() -> renewTrials(level, s, c, fr));
	}

	// --- Building ---

	/** The chunks under the box from {@code a} to {@code b} loaded (made, the first time). */
	static void loadChunks(ServerLevel level, BlockPos a, BlockPos b) {
		for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++) {
			for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) level.getChunk(cx, cz);
		}
	}

	/** {@code room} hollowed out of the mountain: its floor, walls and ceiling laid a block about its inside, its inside cleared. */
	static void hollow(ServerLevel level, Frame fr, Room room, BlockState floor, BlockState wall, BlockState ceiling) {
		loadChunks(level, fr.at(room.f0() - 1, room.s0() - 1, 0), fr.at(room.f1() + 1, room.s1() + 1, 0));
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int f = room.f0() - 1; f <= room.f1() + 1; f++) {
			for (int sd = room.s0() - 1; sd <= room.s1() + 1; sd++) {
				boolean in = room.inside(f, sd);
				for (int y = 0; y <= room.h() + 1; y++) put(level, fr.at(f, sd, y), y == 0 ? floor : y == room.h() + 1 ? ceiling : in ? air : wall);
			}
		}
	}

	/** A band of {@code state} about {@code room}'s walls at height {@code y} (not across its doorways). */
	static void band(ServerLevel level, Frame fr, Room room, int y, BlockState state) {
		for (int f = room.f0() - 1; f <= room.f1() + 1; f++) {
			for (int sd = room.s0() - 1; sd <= room.s1() + 1; sd++) {
				if (room.inside(f, sd)) continue;
				BlockPos at = fr.at(f, sd, y);
				if (!level.getBlockState(at).isAir()) put(level, at, state);
			}
		}
	}

	/** {@code room}'s walls weathered: old stone, mossy and cracked here and there. */
	static void weather(ServerLevel level, Frame fr, Room room, RandomSource random) {
		for (int f = room.f0() - 1; f <= room.f1() + 1; f++) {
			for (int sd = room.s0() - 1; sd <= room.s1() + 1; sd++) {
				if (room.inside(f, sd)) continue;
				for (int y = 1; y <= room.h(); y++) {
					BlockPos at = fr.at(f, sd, y);
					if (level.getBlockState(at).is(Blocks.STONE_BRICKS)) put(level, at, oldStone(random));
				}
			}
		}
	}

	/** {@code room}'s walls, fences and panes joined to what stands about them. */
	static void joinRoom(ServerLevel level, Frame fr, Room room) {
		join(level, fr.at(room.f0() - 1, room.s0() - 1, 0), fr.at(room.f1() + 1, room.s1() + 1, room.h() + 1));
	}

	static BlockState soulLantern() {
		return Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
	}

	/**
	 * The gate at the mountain's foot: the causeway across the lake to it and a forecourt before it, paved, railed and lit; its front of old
	 * stone set into the slope (the mountain's rock built up behind it), a doorway five wide; two white jade pillars before it under a porch
	 * roofed in dark tiles, lanterns hung from its lintel.
	 */
	static void gate(ServerLevel level, CaveAbode.Shape s, Frame fr) {
		loadChunks(level, fr.at(-100, -10, 0), fr.at(-48, 10, 0));
		SecretRealm realm = SecretRealm.CAVE_ABODE;
		RandomSource random = RandomSource.create(s.seed ^ 0x6A7E5L);
		BlockState paving = Blocks.POLISHED_ANDESITE.defaultBlockState(), fill = Blocks.STONE_BRICKS.defaultBlockState();
		levelOff(level, realm, fr.at(-98, -3, 0), fr.at(-64, 3, 0), FLOOR, paving, fill, 5);
		levelOff(level, realm, fr.at(-64, -9, 0), fr.at(-58, 9, 0), FLOOR, paving, fill, 16);
		for (int f = -64; f <= -59; f++) for (int sd = -9; sd <= 9; sd++) if (Math.abs(sd) == 9 || f == -64) put(level, fr.at(f, sd, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		// The causeway's rails, a lantern on every sixth post.
		for (int f = -96; f <= -65; f++) {
			for (int sd = -3; sd <= 3; sd += 6) {
				put(level, fr.at(f, sd, 1), Blocks.STONE_BRICK_WALL.defaultBlockState());
				if (Math.floorMod(f, 6) == 0) put(level, fr.at(f, sd, 2), lantern(false));
			}
		}
		// The front, two blocks thick, its edges carved.
		for (int sd = -8; sd <= 8; sd++) {
			for (int y = 0; y <= 11; y++) {
				put(level, fr.at(-57, sd, y), oldStone(random));
				boolean trim = Math.abs(sd) == 8 || y == 11 || y == 0;
				put(level, fr.at(-58, sd, y), trim ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : oldStone(random));
			}
		}
		// The mountain's rock built up behind it, so that it stands set into the slope (the tunnel's own left).
		for (int f = -56; f <= -50; f++) {
			for (int sd = -9; sd <= 9; sd++) {
				for (int y = 1; y <= 11; y++) {
					if (Math.abs(sd) <= 3 && y <= TUNNEL.h() + 1) continue;
					BlockPos at = fr.at(f, sd, y);
					BlockState here = level.getBlockState(at);
					if (here.isAir() || here.canBeReplaced()) put(level, at, CaveAbodeLand.rock(s, at.getX(), at.getY(), at.getZ()));
				}
			}
		}
		// Its porch: two pillars of white jade, a lintel across them, the roof over all; lanterns hung either side of the way in.
		for (int sd = -5; sd <= 5; sd += 10) for (int y = 1; y <= 8; y++) put(level, fr.at(-61, sd, y), log(Blocks.QUARTZ_PILLAR));
		for (int sd = -6; sd <= 6; sd++) put(level, fr.at(-61, sd, 9), Blocks.SMOOTH_QUARTZ.defaultBlockState());
		HerbValleyWorks.hipRoof(level, fr, -64, -58, -9, 9, 10);
		for (int sd = -3; sd <= 3; sd += 6) put(level, fr.at(-61, sd, 8), lantern(true));
		join(level, fr.at(-98, -10, 0), fr.at(-56, 10, 12));
	}

	/** The shaft of sky cut down through the peak to the garden: nine wide, from the garden's roof to the mountain's top. */
	static void skyWell(ServerLevel level, Frame fr) {
		loadChunks(level, fr.at(-5, -5, 0), fr.at(5, 5, 0));
		long seed = level.getSeed();
		for (int f = -4; f <= 4; f++) {
			for (int sd = -4; sd <= 4; sd++) {
				BlockPos base = fr.at(f, sd, 0);
				int top = groundAt(SecretRealm.CAVE_ABODE, seed, base.getX(), base.getZ());
				for (int y = FLOOR + GARDEN.h() + 1; y <= top + 2; y++) put(level, base.atY(y), Blocks.AIR.defaultBlockState());
			}
		}
	}

	/** The maze: its floor of dark tiles, its walls by the world's own roll ({@link #maze}), its roof; its heart a ring of white jade about a light. */
	static void mazeLaid(ServerLevel level, CaveAbode.Shape s, Frame fr) {
		int size = CELLS * CELL;
		loadChunks(level, fr.at(MAZE_F, MAZE_S, 0), fr.at(MAZE_F + size, MAZE_S + size, 0));
		Maze maze = maze(s.seed);
		RandomSource random = RandomSource.create(s.seed ^ 0x3A2E2L);
		for (int mx = 0; mx <= size; mx++) {
			for (int mz = 0; mz <= size; mz++) {
				int f = MAZE_F + mx, sd = MAZE_S + mz;
				put(level, fr.at(f, sd, 0), Blocks.DEEPSLATE_TILES.defaultBlockState());
				boolean wall = maze.wall(mx, mz), corner = mx % CELL == 0 && mz % CELL == 0;
				for (int y = 1; y <= MAZE_WALL; y++) {
					BlockState state = !wall ? Blocks.AIR.defaultBlockState() : corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
							: random.nextFloat() < 0.15f ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
					put(level, fr.at(f, sd, y), state);
				}
				put(level, fr.at(f, sd, MAZE_WALL + 1), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
			}
		}
		BlockPos heart = cellMiddle(fr, MAZE_MID, MAZE_MID).below();
		for (int a = -1; a <= 1; a++) {
			for (int b = -1; b <= 1; b++) {
				put(level, heart.relative(fr.forward(), a).relative(fr.right(), b), (a == 0 && b == 0 ? Blocks.SEA_LANTERN : Blocks.CHISELED_QUARTZ_BLOCK).defaultBlockState());
			}
		}
		// Soul lanterns hung at every other cell: a dim light, blue in the mist.
		for (int i = 0; i < CELLS; i++) {
			for (int j = 0; j < CELLS; j++) if ((i + j) % 2 == 0 && !(i == MAZE_MID && j == MAZE_MID)) put(level, cellMiddle(fr, i, j).above(MAZE_WALL - 1), soulLantern());
		}
	}

	/** The doorways between the rooms (DOORS), and the chamber's barrier. */
	static void doors(ServerLevel level, Frame fr) {
		for (int[] d : DOORS) {
			for (int f = d[0]; f <= d[1]; f++) {
				for (int sd = d[2]; sd <= d[3]; sd++) {
					put(level, fr.at(f, sd, 0), Blocks.POLISHED_ANDESITE.defaultBlockState());
					for (int y = 1; y <= d[4]; y++) put(level, fr.at(f, sd, y), Blocks.AIR.defaultBlockState());
				}
			}
		}
		for (BlockPos p : barrier(fr)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
	}

	/**
	 * The tunnel, lanterns hung down it; the long hall, its floor a check of dark tiles, four white jade pillars, a carved band about its walls;
	 * the library, shelves of books along its walls, lecterns, a reading table; the springs, a pool of spirit water lit from beneath in a rim of
	 * white stone.
	 */
	static void furnishHalls(ServerLevel level, CaveAbode.Shape s, Frame fr) {
		RandomSource random = RandomSource.create(s.seed ^ 0xF0121L);
		for (int f = -54; f <= -50; f += 4) put(level, fr.at(f, 0, TUNNEL.h()), lantern(true));
		for (int f = HALL.f0(); f <= HALL.f1(); f++) {
			for (int sd = HALL.s0(); sd <= HALL.s1(); sd++) {
				put(level, fr.at(f, sd, 0), (Math.floorMod(f + sd, 2) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState());
			}
		}
		for (int f = -43; f <= -39; f += 4) for (int sd = -5; sd <= 5; sd += 10) for (int y = 1; y <= HALL.h(); y++) put(level, fr.at(f, sd, y), log(Blocks.QUARTZ_PILLAR));
		weather(level, fr, HALL, random);
		band(level, fr, HALL, 3, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		for (int f = -44; f <= -38; f += 6) for (int sd = -2; sd <= 2; sd += 4) put(level, fr.at(f, sd, HALL.h()), lantern(true));
		// The library.
		for (int f = LIBRARY.f0(); f <= LIBRARY.f1(); f++) {
			for (int sd = LIBRARY.s0(); sd <= LIBRARY.s1(); sd++) {
				boolean shelf = f == LIBRARY.f0() || f == LIBRARY.f1() || sd == LIBRARY.s0() && f != -41;
				if (!shelf) continue;
				for (int y = 1; y <= 3; y++) put(level, fr.at(f, sd, y), Blocks.BOOKSHELF.defaultBlockState());
			}
		}
		for (int f = -44; f <= -38; f += 6) put(level, fr.at(f, -13, 1), Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, fr.right()));
		for (int f = -42; f <= -40; f++) put(level, fr.at(f, -16, 1), slab(Blocks.DARK_OAK_SLAB, true));
		put(level, fr.at(-41, -16, LIBRARY.h()), lantern(true));
		// The springs: the pool's rim of white stone, its water two deep, sea lanterns under it.
		for (int f = -44; f <= -38; f++) {
			for (int sd = 13; sd <= 19; sd++) {
				boolean rim = f == -44 || f == -38 || sd == 13 || sd == 19;
				if (rim) {
					put(level, fr.at(f, sd, 0), Blocks.SMOOTH_QUARTZ.defaultBlockState());
					continue;
				}
				put(level, fr.at(f, sd, -2), (Math.floorMod(f + sd, 2) == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_STONE).defaultBlockState());
				put(level, fr.at(f, sd, -1), Blocks.WATER.defaultBlockState());
				put(level, fr.at(f, sd, 0), Blocks.WATER.defaultBlockState());
			}
		}
		put(level, fr.at(-41, 16, SPRINGS.h()), lantern(true));
		joinRoom(level, fr, HALL);
	}

	/**
	 * The lecture hall: two rows of red pillars, a ceiling of dark wood on beams, cushions in rows; at its far end the master's dais, his seat
	 * on it facing the hall and a screen of jade behind (the way into the garden behind the screen); lanterns hung in rows.
	 */
	static void furnishLecture(ServerLevel level, Frame fr) {
		Room l = LECTURE;
		for (int f = -30; f <= -20; f += 5) {
			for (int sd = -7; sd <= 7; sd += 14) for (int y = 1; y <= l.h(); y++) put(level, fr.at(f, sd, y), log(Blocks.STRIPPED_MANGROVE_LOG));
			for (int sd = l.s0(); sd <= l.s1(); sd++) {
				put(level, fr.at(f, sd, l.h() + 1), Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, fr.right().getAxis()));
			}
		}
		band(level, fr, l, 4, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
		for (int f = -30; f <= -21; f += 3) for (int sd = -4; sd <= 4; sd += 2) put(level, fr.at(f, sd, 1), Blocks.WHITE_CARPET.defaultBlockState());
		// The dais, its steps; the seat; the screen.
		for (int f = -19; f <= l.f1(); f++) for (int sd = -4; sd <= 4; sd++) put(level, fr.at(f, sd, 1), Blocks.POLISHED_ANDESITE.defaultBlockState());
		for (int sd = -4; sd <= 4; sd++) put(level, fr.at(-20, sd, 1), stairs(Blocks.POLISHED_ANDESITE_STAIRS, fr.forward(), false));
		put(level, lectureSeat(fr), stairs(Blocks.DARK_OAK_STAIRS, fr.forward(), false));
		for (int sd = -3; sd <= 3; sd++) {
			for (int y = 2; y <= 6; y++) put(level, fr.at(-17, sd, y), (Math.abs(sd) == 3 || y == 6 ? Blocks.SMOOTH_QUARTZ : ModBlocks.JADE_STONE).defaultBlockState());
		}
		for (int f = -30; f <= -20; f += 5) for (int sd = -3; sd <= 3; sd += 6) put(level, fr.at(f, sd, l.h()), lantern(true));
		joinRoom(level, fr, l);
	}

	/**
	 * The garden court: grass over earth, paths of trodden earth from its doors about the pond at its middle (open to the sky), four herb beds,
	 * azaleas along its walls, pillars of white jade at the sky well's corners; its walls old and mossy; lanterns hung about.
	 */
	static void furnishGarden(ServerLevel level, CaveAbode.Shape s, Frame fr) {
		Room g = GARDEN;
		RandomSource random = RandomSource.create(s.seed ^ 0x6A4DE7L);
		for (int f = g.f0(); f <= g.f1(); f++) {
			for (int sd = g.s0(); sd <= g.s1(); sd++) {
				double r = Math.hypot(f, sd);
				put(level, fr.at(f, sd, -1), Blocks.DIRT.defaultBlockState());
				if (r < POND_R) {
					put(level, fr.at(f, sd, -2), Blocks.CLAY.defaultBlockState());
					put(level, fr.at(f, sd, -1), Blocks.WATER.defaultBlockState());
					put(level, fr.at(f, sd, 0), Blocks.WATER.defaultBlockState());
				} else if (r < POND_R + 1) {
					put(level, fr.at(f, sd, 0), Blocks.MOSSY_STONE_BRICKS.defaultBlockState());
				} else {
					boolean path = Math.abs(f) <= 1 && Math.abs(sd) > 5 || Math.abs(sd) <= 1 && Math.abs(f) > 5 || r >= 5 && r < 6.3;
					put(level, fr.at(f, sd, 0), (path ? Blocks.DIRT_PATH : Blocks.GRASS_BLOCK).defaultBlockState());
				}
			}
		}
		for (int bf = -7; bf <= 7; bf += 14) for (int bs = -7; bs <= 7; bs += 14) for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
			put(level, fr.at(bf + a, bs + b, 0), Blocks.PODZOL.defaultBlockState());
		}
		for (int f = -5; f <= 5; f += 10) for (int sd = -5; sd <= 5; sd += 10) for (int y = 1; y <= g.h(); y++) put(level, fr.at(f, sd, y), log(Blocks.QUARTZ_PILLAR));
		for (int k = -9; k <= 9; k += 3) {
			if (Math.abs(k) <= 2) continue;
			for (int[] at : new int[][] {{k, -10}, {k, 10}, {-10, k}, {10, k}}) {
				if (level.getBlockState(fr.at(at[0], at[1], 0)).is(Blocks.GRASS_BLOCK)) {
					put(level, fr.at(at[0], at[1], 1), (random.nextBoolean() ? Blocks.FLOWERING_AZALEA : Blocks.AZALEA).defaultBlockState());
				}
			}
		}
		weather(level, fr, g, random);
		for (int f = -8; f <= 8; f += 8) for (int sd = -8; sd <= 8; sd += 8) if (f != 0 || sd != 0) put(level, fr.at(f, sd, g.h()), lantern(true));
		joinRoom(level, fr, g);
	}

	/** The hall of the five elements: a dark floor with rings of carved stone about gold at its middle; the five pillars in their ring; lanterns. */
	static void furnishElements(ServerLevel level, CaveAbode.Shape s, BlockPos c, Frame fr) {
		Room e = ELEMENTS;
		for (int f = e.f0(); f <= e.f1(); f++) {
			for (int sd = e.s0(); sd <= e.s1(); sd++) {
				double r = Math.hypot(f, sd - ELEMENTS_MIDDLE_S);
				BlockState floor = r < 0.5 ? Blocks.GOLD_BLOCK.defaultBlockState() : Math.abs(r - 2.2) < 0.5 || Math.abs(r - 6.6) < 0.5
						? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
				put(level, fr.at(f, sd, 0), floor);
			}
		}
		for (Map.Entry<ElementPillarBlock.Element, BlockPos> p : pillars(s, c).entrySet()) {
			BlockPos at = p.getValue();
			BlockState pillar = ModBlocks.ELEMENT_PILLAR.defaultBlockState().setValue(ElementPillarBlock.ELEMENT, p.getKey());
			put(level, at.below(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
			put(level, at, pillar);
			put(level, at.above(), pillar);
			put(level, at.above(2), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
		}
		for (int f = -5; f <= 5; f += 10) for (int sd = -25; sd <= -17; sd += 8) put(level, fr.at(f, sd, e.h()), lantern(true));
	}

	/**
	 * The master's chamber: white jade walls, a floor of white stone bordered dark; shelves of books along its sides; at its back his bones
	 * where he sat, a screen of jade behind them; his low table before him (the altar), candles either side of it; lanterns hung.
	 */
	static void furnishChamber(ServerLevel level, Frame fr) {
		Room ch = CHAMBER;
		for (int f = ch.f0(); f <= ch.f1(); f++) {
			for (int sd = ch.s0(); sd <= ch.s1(); sd++) {
				boolean edge = f == ch.f0() || f == ch.f1() || sd == ch.s0() || sd == ch.s1();
				put(level, fr.at(f, sd, 0), (edge ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_QUARTZ).defaultBlockState());
			}
		}
		for (int sd = 16; sd <= 23; sd++) for (int f = ch.f0(); f <= ch.f1(); f += ch.f1() - ch.f0()) for (int y = 1; y <= 3; y++) put(level, fr.at(f, sd, y), Blocks.BOOKSHELF.defaultBlockState());
		for (int f = -3; f <= 3; f++) for (int y = 1; y <= 5; y++) put(level, fr.at(f, 25, y), (Math.abs(f) == 3 || y == 5 ? Blocks.SMOOTH_QUARTZ : ModBlocks.JADE_STONE).defaultBlockState());
		put(level, altar(fr), slab(Blocks.DARK_OAK_SLAB, true));
		for (int f = -2; f <= 2; f += 4) {
			put(level, fr.at(f, 21, 1), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
			put(level, fr.at(f, 21, 2), Blocks.WHITE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
		}
		layMaster(level, fr);
		for (int f = -3; f <= 3; f += 6) for (int sd = 17; sd <= 23; sd += 6) put(level, fr.at(f, sd, ch.h()), lantern(true));
		joinRoom(level, fr, ch);
	}

	/** The master's bones where he sat, facing his door. */
	static void layMaster(ServerLevel level, Frame fr) {
		BlockPos at = masterBones(fr);
		put(level, at, ModBlocks.CORPSE.defaultBlockState().setValue(CorpseBlock.STAGE, CorpseBlock.Rot.BONES));
		if (level.getBlockEntity(at) instanceof CorpseBlockEntity body) {
			body.ancient(masterName(level), 0, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.MID), fr.right().getOpposite().toYRot(), level.getGameTime(), List.of());
		}
	}

	/** At each arrival: a terrace of old paving in the meadow, a stone lantern either side. */
	static void arrivalTerraces(ServerLevel level, CaveAbode.Shape s, BlockPos c) {
		RandomSource random = RandomSource.create(s.seed ^ 0xA771AL);
		for (BlockPos at : WORKS.arrivalSpots(s.seed, c)) {
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			for (int dx = -3; dx <= 3; dx++) {
				for (int dz = -3; dz <= 3; dz++) {
					if (dx * dx + dz * dz > 12) continue;
					put(level, at.offset(dx, 0, dz), oldStone(random));
					for (int y = 1; y <= 4; y++) put(level, at.offset(dx, y, dz), Blocks.AIR.defaultBlockState());
				}
			}
			for (int side = -3; side <= 3; side += 6) {
				put(level, at.offset(side, 1, 0), Blocks.STONE_BRICK_WALL.defaultBlockState());
				put(level, at.offset(side, 2, 0), lantern(false));
			}
			join(level, at.offset(-4, 0, -4), at.offset(4, 4, 4));
		}
	}

	/** Pines, part {@code part}: a quarter of the basin round, off the lake, the causeway's way, the arrivals and the mountain; a few great ones. */
	static void pines(ServerLevel level, CaveAbode.Shape s, BlockPos c, int part, RandomSource random) {
		var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
		List<BlockPos> arrivals = WORKS.arrivalSpots(s.seed, c);
		for (int i = 0, tries = 0; i < 34 && tries < 240; tries++) {
			double a = s.mainTurn + (part + random.nextDouble()) * Math.PI / 2, r = CaveAbode.MOUNT_R + 6 + random.nextDouble() * (CaveAbode.BASIN_R - CaveAbode.MOUNT_R - 4);
			int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
			CaveAbode.Column col = CaveAbode.column(s, x, z);
			if (col.kind() != CaveAbode.Kind.FLOOR) continue;
			double[] off = CaveAbode.mainOff(s, x, z);
			if (off[1] > 0 && off[0] < 9 && off[1] < 110) continue; // the way to the gate
			BlockPos at = c.offset(x, 0, z).atY(col.ground() + 1);
			boolean near = false;
			for (BlockPos w : arrivals) if (w.distSqr(at.atY(w.getY())) < 10 * 10) near = true;
			if (near) continue;
			double roll = random.nextDouble();
			ResourceKey<ConfiguredFeature<?, ?>> kind = roll < 0.1 ? TreeFeatures.MEGA_PINE : roll < 0.55 ? TreeFeatures.PINE : TreeFeatures.SPRUCE;
			var tree = features.getHolder(kind);
			if (tree.isEmpty()) continue;
			level.getChunk(at.getX() >> 4, at.getZ() >> 4);
			if (!level.getBlockState(at).isAir() && !level.getBlockState(at).canBeReplaced()) continue;
			if (!level.getBlockState(at).isAir()) put(level, at, Blocks.AIR.defaultBlockState());
			if (tree.get().value().place(level, level.getChunkSource().getGenerator(), random, at)) i++;
		}
	}

	// --- Renewing ---

	/** The garden's herbs and the pond's lotuses: an unpicked one aged by the years shut, one picked grown again. */
	static void renewGarden(ServerLevel level, CaveAbode.Shape s, Frame fr, SecretRealms.Record r) {
		RandomSource random = RandomSource.create(s.seed ^ r.openings * 0x6A4DEL);
		int bonus = r.realm.years() * HerbValleyWorks.YEARS_A_YEAR_SHUT;
		Block[] kinds = {ModBlocks.SPIRIT_GINSENG, ModBlocks.PURPLE_LINGZHI, ModBlocks.OCHRE_HUANGJING};
		int i = 0;
		for (BlockPos at : herbSpots(fr)) HerbValleyWorks.renewHerb(level, at, kinds[i++ % kinds.length], 1000 + random.nextInt(2000), bonus, Blocks.PODZOL);
		for (BlockPos at : lotusSpots(fr)) HerbValleyWorks.renewHerb(level, at, ModBlocks.SPIRIT_LOTUS, 1000 + random.nextInt(2000), bonus, null);
	}

	/** The library's vault (High) and the springs' (Mid): sealed anew for the ancients and filled. */
	static void renewVaults(ServerLevel level, Frame fr) {
		BlockPos[] at = {libraryVault(fr), springsVault(fr)};
		for (int k = 0; k < at.length; k++) {
			level.getChunk(at[k].getX() >> 4, at[k].getZ() >> 4);
			StorageFormations.Tier tier = k == 0 ? StorageFormations.Tier.HIGH : StorageFormations.Tier.MID;
			if (!(level.getBlockState(at[k]).getBlock() instanceof StorageFormationBlock)) put(level, at[k], ModBlocks.storageFormation(tier).defaultBlockState());
			if (level.getBlockEntity(at[k]) instanceof StorageFormationBlockEntity vault) {
				vault.sealForAncients(level, k == 0 ? libraryTreasures(level.getRandom()) : springsTreasures(level.getRandom()));
			}
		}
	}

	/** The library's: manuals of the heaven path's spells, a jade slip of a method now and then, spirit stones. */
	static List<ItemStack> libraryTreasures(RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 3 + random.nextInt(5)));
		for (int i = 0; i < 2 + random.nextInt(2); i++) {
			ItemStack manual = manual(random, Realm.HEAVENLY_BEING);
			if (!manual.isEmpty()) out.add(manual);
		}
		if (random.nextFloat() < 0.5f) out.add(slip(random, Realm.NASCENT_SOUL));
		if (random.nextFloat() < 0.15f) out.add(slip(random, Realm.HEAVENLY_BEING));
		if (random.nextFloat() < 0.4f) out.add(new ItemStack(ModItems.DEPARTURE_JADE));
		return out;
	}

	/** The springs': bottles of the Spring of the Ancients, old pills, spirit stones, now and then an artifact or Ancient Essence. */
	static List<ItemStack> springsTreasures(RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		out.add(new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS, 1 + random.nextInt(2)));
		out.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 2 + random.nextInt(3)));
		for (int i = 0; i < 2; i++) out.add(HerbValleyWorks.pill(random, 1000, 3000));
		if (random.nextFloat() < 0.3f) out.add(Artifacts.any(random, ArtifactGrade.EARTH, null));
		if (random.nextFloat() < 0.1f) out.add(new ItemStack(ModItems.ANCIENT_ESSENCE));
		return out;
	}

	/** A manual of a heaven path's spell up to {@code realm}'s. */
	static ItemStack manual(RandomSource random, Realm realm) {
		List<Spell> spells = new ArrayList<>();
		for (Spell spell : Spell.values()) if (spell.isHeaven() && spell.heavenRealm().ordinal() <= realm.ordinal()) spells.add(spell);
		return spells.isEmpty() ? ItemStack.EMPTY : TechniqueManualItem.of(spells.get(random.nextInt(spells.size())));
	}

	/** A jade slip of a heaven method of {@code realm}, of any of the five. */
	static ItemStack slip(RandomSource random, Realm realm) {
		QiElement[] five = QiElement.five();
		return JadeSlipItem.of(HeavenMethod.of(five[random.nextInt(five.length)], realm));
	}

	/** The five pillars dark; the chamber's barrier whole; the master's bones in their place. */
	static void renewTrials(ServerLevel level, CaveAbode.Shape s, BlockPos c, Frame fr) {
		loadChunks(level, fr.at(-8, -28, 0), fr.at(8, 26, 0));
		for (BlockPos at : pillars(s, c).values()) setLit(level, at, false);
		for (BlockPos p : barrier(fr)) if (!level.getBlockState(p).is(ModBlocks.TRIAL_BARRIER)) put(level, p, ModBlocks.TRIAL_BARRIER.defaultBlockState());
		if (!(level.getBlockState(masterBones(fr)).getBlock() instanceof CorpseBlock)) layMaster(level, fr);
	}

	// --- Arrivals, bones, guardians ---

	@Override
	protected List<BlockPos> arrivalSpots(long seed, BlockPos c) {
		List<BlockPos> at = new ArrayList<>();
		for (BlockPos local : CaveAbode.arrivals(CaveAbode.shape(seed))) at.add(c.offset(local.getX(), 0, local.getZ()).atY(local.getY()));
		return at;
	}

	/** An earlier visitor's bones: in one of the abode's halls, or its garden. */
	@Override
	protected BlockPos remainsSpot(ServerLevel level, long seed, BlockPos c, RandomSource random) {
		Frame fr = frame(CaveAbode.shape(seed), c);
		Room[] rooms = {HALL, LIBRARY, LECTURE, GARDEN, ELEMENTS};
		Room room = rooms[random.nextInt(rooms.length)];
		int f = room.f0() + 1 + random.nextInt(Math.max(1, room.f1() - room.f0() - 1)), sd = room.s0() + 1 + random.nextInt(Math.max(1, room.s1() - room.s0() - 1));
		if (room == GARDEN && Math.hypot(f, sd) < POND_R + 1.5) return null;
		return fr.at(f, sd, 1);
	}

	@Override
	protected List<ItemStack> remainsCarried(RandomSource random) {
		List<ItemStack> carried = new ArrayList<>();
		carried.add(new ItemStack(ModItems.MID_SPIRIT_STONE, 1 + random.nextInt(3)));
		if (random.nextFloat() < 0.4f) carried.add(HerbValleyWorks.pill(random, 600, 2000));
		if (random.nextFloat() < 0.25f) carried.add(new ItemStack(ModItems.DEPARTURE_JADE));
		if (random.nextFloat() < 0.15f) {
			ItemStack manual = manual(random, Realm.NASCENT_SOUL);
			if (!manual.isEmpty()) carried.add(manual);
		}
		return carried;
	}

	@Override
	protected List<Slot> guardianSlots(long seed, BlockPos c, SecretRealm realm) {
		CaveAbode.Shape s = CaveAbode.shape(seed);
		Frame fr = frame(s, c);
		List<Slot> slots = new ArrayList<>();
		for (int k = 0; k < 2; k++) {
			int side = k == 0 ? -1 : 1;
			slots.add(new Slot("gate" + k, ModEntities.BRONZE_PUPPET, fr.at(-62, side * 4, 1), realm.guardianRank(Stage.EARLY), false));
			BlockPos end = fr.at(-100, side * 5, 0);
			slots.add(new Slot("causeway" + k, ModEntities.BRONZE_PUPPET, end.atY(groundAt(realm, seed, end.getX(), end.getZ()) + 1), realm.guardianRank(Stage.EARLY), false));
			slots.add(new Slot("hall" + k, ModEntities.BRONZE_PUPPET, fr.at(-41, side * 7, 1), realm.guardianRank(Stage.MID), false));
			slots.add(new Slot("lecture" + k, ModEntities.BRONZE_PUPPET, fr.at(-25, side * 9, 1), realm.guardianRank(Stage.LATE), false));
		}
		slots.add(new Slot("library", ModEntities.BRONZE_PUPPET, fr.at(-38, -16, 1), realm.guardianRank(Stage.MID), false));
		slots.add(new Slot("springs", ModEntities.BRONZE_PUPPET, fr.at(-37, 16, 1), realm.guardianRank(Stage.MID), false));
		slots.add(new Slot("keeper", ModEntities.VINE_KEEPER, fr.at(9, -7, 1), realm.guardianRank(Stage.MID), false));
		slots.add(new Slot("elements", ModEntities.BRONZE_PUPPET, fr.at(5, -26, 1), realm.guardianRank(Stage.LATE), false));
		Maze maze = maze(seed);
		for (int k = 1; k <= 2; k++) {
			int[] cell = maze.way.get(maze.way.size() * k / 3);
			slots.add(new Slot("maze" + k, ModEntities.BRONZE_PUPPET, cellMiddle(fr, cell[0], cell[1]), realm.guardianRank(Stage.MID), false));
		}
		slots.add(new Slot("puppet", ModEntities.MASTER_PUPPET, puppetPost(fr), realm.guardianRank(Stage.EARLY), true));
		return slots;
	}

	/** The mountain holds: the closing quake splits only the basin. */
	@Override
	protected int calm() { return CaveAbode.MOUNT_R; }

	// --- During an opening ---

	@Override
	protected void begin(SecretRealms.Record r, long now, RandomSource random) {
		r.works.putLong("LectureAt", now + (long) (r.realm.insideTicks() * (0.3 + 0.3 * random.nextDouble())));
		r.works.putInt("Lecture", 0);
	}

	@Override
	protected void everyTick(ServerLevel level, SecretRealms.Data data, SecretRealms.Record r, long now, List<ServerPlayer> inside) {
		CaveAbode.Shape s = CaveAbode.shape(level.getSeed());
		BlockPos c = r.realm.centre();
		Frame fr = frame(s, c);
		lecture(level, fr, r, now, inside, data);
		if (r.works.contains("PillarsRest") && now >= r.works.getLong("PillarsRest")) {
			darken(level, r, pillars(s, c));
			data.setDirty();
		}
		if (now % 10 == 4) for (ServerPlayer p : inside) visit(level, fr, r, p, now, data);
		if (now % 20 == 9) {
			BlockPos altar = altar(fr);
			if (level.isLoaded(altar)) level.sendParticles(ParticleTypes.ENCHANT, altar.getX() + 0.5, altar.getY() + 0.9, altar.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.4);
		}
	}

	/** What {@code p} meets where they stand: the trials told in the garden; the springs' water; the maze's mist and its heart; the master's altar. */
	private static void visit(ServerLevel level, Frame fr, SecretRealms.Record r, ServerPlayer p, long now, SecretRealms.Data data) {
		int[] at = local(fr, p.blockPosition());
		UUID id = p.getUUID();
		switch (zone(at)) {
			case GARDEN -> {
				ListTag told = r.works.getList("Told", Tag.TAG_STRING);
				if (told.contains(StringTag.valueOf(id.toString()))) return;
				told.add(StringTag.valueOf(id.toString()));
				r.works.put("Told", told);
				data.setDirty();
				p.sendSystemMessage(Component.translatable(ModLang.MSG_ABODE_TRIALS, Component.translatable(r.inheritor.isEmpty() ? ModLang.MSG_ABODE_GREAT
						: ModLang.MSG_ABODE_LESSER)).withStyle(ChatFormatting.GOLD));
			}
			case HALL -> {
				if (!SPRINGS.inside(at[0], at[1])) return;
				// The spring's water mends whoever bathes in it; its steam rises.
				if (p.isInWater()) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, true));
				if (now % 20 == 4) {
					BlockPos pool = fr.at(-41, 16, 1);
					level.sendParticles(ParticleTypes.CLOUD, pool.getX() + 0.5, pool.getY() + 0.2, pool.getZ() + 0.5, 6, 2, 0.1, 2, 0.01);
				}
			}
			case MAZE -> {
				level.sendParticles(p, ParticleTypes.WHITE_ASH, true, p.getX(), p.getY() + 1, p.getZ(), 30, 4, 1.5, 4, 0.01);
				int[] cell = cellAt(at);
				if (cell == null) return;
				boolean elements = passed(r, "Elements", id);
				if (cell[0] == MAZE_MID && cell[1] == MAZE_MID) {
					if (!elements) {
						if (now % 40 == 4) p.displayClientMessage(Component.translatable(ModLang.MSG_ABODE_MAZE_CLOSED).withStyle(ChatFormatting.GRAY), true);
					} else if (!passed(r, "Maze", id)) {
						pass(r, "Maze", id);
						data.setDirty();
						p.sendSystemMessage(Component.translatable(ModLang.MSG_ABODE_MAZE_DONE).withStyle(ChatFormatting.GOLD));
						p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
						BlockPos heart = cellMiddle(fr, MAZE_MID, MAZE_MID);
						level.sendParticles(ParticleTypes.END_ROD, heart.getX() + 0.5, heart.getY() + 0.5, heart.getZ() + 0.5, 30, 0.6, 1.0, 0.6, 0.05);
					}
				} else if (cell[0] == 0 && cell[1] == MAZE_MID && !elements && now % 60 == 4) {
					p.displayClientMessage(Component.translatable(ModLang.MSG_ABODE_MAZE_CLOSED).withStyle(ChatFormatting.GRAY), true);
				}
			}
			case CHAMBER -> {
				if (p.blockPosition().distSqr(altar(fr)) < 3 * 3 && passed(r, id) && !passed(r, "Claimed", id)) {
					pass(r, "Claimed", id);
					data.setDirty();
					inherit(level, r, p);
				}
			}
			default -> {
			}
		}
	}

	/**
	 * Its hour come, for {@link #LECTURE_TICKS}: the master's voice from his seat on the dais; whoever is in the lecture hall gathers the more at
	 * meditation (ModEffects#MASTERS_LECTURE); at its end, an Insight for those who sat through enough of it.
	 */
	static void lecture(ServerLevel level, Frame fr, SecretRealms.Record r, long now, List<ServerPlayer> inside, SecretRealms.Data data) {
		int stage = r.works.getInt("Lecture");
		if (stage == 0 && now >= r.works.getLong("LectureAt")) {
			r.works.putInt("Lecture", 1);
			r.works.putLong("LectureEnds", now + LECTURE_TICKS);
			data.setDirty();
			tellInside(level, r.realm, Component.translatable(ModLang.MSG_ABODE_LECTURE, LECTURE_TICKS / 1200).withStyle(ChatFormatting.LIGHT_PURPLE));
			for (ServerPlayer p : inside) p.playNotifySound(SoundEvents.BELL_RESONATE, SoundSource.AMBIENT, 1.0f, 0.6f);
		} else if (stage == 1) {
			CompoundTag heard = r.works.getCompound("Heard");
			if (now >= r.works.getLong("LectureEnds")) {
				r.works.putInt("Lecture", 2);
				data.setDirty();
				tellInside(level, r.realm, Component.translatable(ModLang.MSG_ABODE_LECTURE_ENDS).withStyle(ChatFormatting.GRAY));
				for (ServerPlayer p : inside) {
					if (heard.getInt(p.getStringUUID()) < HEARD_ENOUGH) continue;
					p.addEffect(new MobEffectInstance(ModEffects.INSIGHT, ModEffects.INSIGHT_TICKS, 0, false, true, true));
					p.sendSystemMessage(Component.translatable(ModLang.MSG_ABODE_LECTURE_HEARD).withStyle(ChatFormatting.GOLD));
				}
				return;
			}
			if (now % 20 != 6) return;
			BlockPos seat = lectureSeat(fr);
			if (level.isLoaded(seat)) {
				level.sendParticles(ParticleTypes.ENCHANT, seat.getX() + 0.5, seat.getY() + 1.6, seat.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 1.2);
				level.sendParticles(ParticleTypes.END_ROD, seat.getX() + 0.5, seat.getY() + 1.0, seat.getZ() + 0.5, 3, 0.3, 0.6, 0.3, 0.01);
				if (now % 100 == 6) level.playSound(null, seat, SoundEvents.BEACON_AMBIENT, SoundSource.AMBIENT, 2.0f, 0.6f);
			}
			for (ServerPlayer p : inside) {
				if (zone(local(fr, p.blockPosition())) != Zone.LECTURE) continue;
				p.addEffect(new MobEffectInstance(ModEffects.MASTERS_LECTURE, 40, 0, true, true, true));
				if (MeditationManager.isMeditating(p.getUUID())) heard.putInt(p.getStringUUID(), heard.getInt(p.getStringUUID()) + 20);
			}
			r.works.put("Heard", heard);
		}
	}

	// --- The five pillars ---

	/** A pillar touched by {@code p} (ElementPillarBlock#use): lit if it is the next, else the backlash ({@link #light}); one who has passed is told so. */
	static void pillarTouched(ServerLevel level, BlockPos pos, ServerPlayer p) {
		SecretRealm realm = SecretRealms.realmAt(level, pos.getX(), pos.getZ());
		if (realm != SecretRealm.CAVE_ABODE || p.isSpectator()) return;
		SecretRealms.Data data = SecretRealms.Data.get(level.getServer());
		SecretRealms.Record r = data.of(realm);
		if (r.openedAt < 0) return;
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ElementPillarBlock)) return;
		if (passed(r, "Elements", p.getUUID())) {
			p.displayClientMessage(Component.translatable(ModLang.MSG_ABODE_ELEMENTS_DONE).withStyle(ChatFormatting.GRAY), true);
			return;
		}
		light(level, r, data, state.getValue(ElementPillarBlock.ELEMENT), p);
	}

	/**
	 * {@code e}'s pillar touched by {@code who}: lit if none is yet, or it is the one the last lit gives rise to; all five lit, all who lit them
	 * this round have passed (and the five go dark a moment after, for whoever comes next); out of turn, the backlash: {@code who} struck and
	 * thrown back, all five dark. @return whether it was lit
	 */
	static boolean light(ServerLevel level, SecretRealms.Record r, SecretRealms.Data data, ElementPillarBlock.Element e, LivingEntity who) {
		CaveAbode.Shape s = CaveAbode.shape(level.getSeed());
		Map<ElementPillarBlock.Element, BlockPos> pillars = pillars(s, r.realm.centre());
		BlockPos at = pillars.get(e);
		if (r.works.contains("PillarsRest") || lit(level, at)) return false;
		ElementPillarBlock.Element[] cycle = ElementPillarBlock.Element.values();
		if (r.works.contains("PillarLast") && cycle[(r.works.getInt("PillarLast") + 1) % cycle.length] != e) {
			backlash(level, r, pillars, at, who);
			data.setDirty();
			return false;
		}
		setLit(level, at, true);
		r.works.putInt("PillarLast", e.ordinal());
		ListTag lighting = r.works.getList("Lighting", Tag.TAG_STRING);
		StringTag id = StringTag.valueOf(who.getUUID().toString());
		if (!lighting.contains(id)) lighting.add(id);
		r.works.put("Lighting", lighting);
		level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5f, 0.6f + 0.2f * e.ordinal());
		level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 12, 0.3, 0.8, 0.3, 0.03);
		boolean all = true;
		for (BlockPos p : pillars.values()) all &= lit(level, p);
		if (all) {
			for (Tag t : lighting) {
				UUID u = UUID.fromString(t.getAsString());
				pass(r, "Elements", u);
				ServerPlayer sp = level.getServer().getPlayerList().getPlayer(u);
				if (sp == null) continue;
				sp.sendSystemMessage(Component.translatable(ModLang.MSG_ABODE_ELEMENTS_DONE).withStyle(ChatFormatting.GOLD));
				sp.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
			}
			r.works.putLong("PillarsRest", level.getServer().overworld().getGameTime() + PILLARS_REST);
		} else if (who instanceof ServerPlayer sp) {
			sp.displayClientMessage(Component.translatable(ModLang.MSG_ABODE_PILLAR_LIT, QiElement.valueOf(e.name()).getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		}
		data.setDirty();
		return true;
	}

	/** The backlash of a pillar touched out of turn: all five dark, {@code who} struck by the formation and thrown back from it. */
	private static void backlash(ServerLevel level, SecretRealms.Record r, Map<ElementPillarBlock.Element, BlockPos> pillars, BlockPos at, LivingEntity who) {
		darken(level, r, pillars);
		who.hurt(level.damageSources().magic(), (float) (who.getMaxHealth() * BACKLASH));
		Vec3 toward = Vec3.atCenterOf(at).subtract(who.position());
		who.knockback(1.2, toward.x, toward.z);
		who.hurtMarked = true;
		level.sendParticles(ParticleTypes.EXPLOSION, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
		level.playSound(null, at, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.8f, 1.4f);
		if (who instanceof ServerPlayer sp) sp.sendSystemMessage(Component.translatable(ModLang.MSG_ABODE_PILLARS_WRONG).withStyle(ChatFormatting.RED));
	}

	/** All five dark again, the round forgotten. */
	static void darken(ServerLevel level, SecretRealms.Record r, Map<ElementPillarBlock.Element, BlockPos> pillars) {
		for (BlockPos pos : pillars.values()) setLit(level, pos, false);
		r.works.remove("PillarLast");
		r.works.remove("Lighting");
		r.works.remove("PillarsRest");
	}

	static boolean lit(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof ElementPillarBlock && state.getValue(ElementPillarBlock.LIT);
	}

	/** {@code pos}'s pillar (its two stones) lit or dark. */
	static void setLit(ServerLevel level, BlockPos pos, boolean lit) {
		for (BlockPos p : new BlockPos[] {pos, pos.above()}) {
			BlockState state = level.getBlockState(p);
			if (state.getBlock() instanceof ElementPillarBlock && state.getValue(ElementPillarBlock.LIT) != lit) {
				RealmLedger.quietly(() -> level.setBlock(p, state.setValue(ElementPillarBlock.LIT, lit), Block.UPDATE_ALL));
			}
		}
	}

	// --- Its trials and inheritance ---

	/** The master's puppet stirs only for one who has walked the maze to its heart. */
	@Override
	protected Component mayChallenge(SecretRealms.Record r, LivingEntity p) {
		return passed(r, "Maze", p.getUUID()) ? null : Component.translatable(ModLang.MSG_ABODE_PUPPET_UNREADY);
	}

	@Override
	protected MutableComponent challengeMessage(Component rank) {
		return Component.translatable(ModLang.MSG_ABODE_PUPPET_WAKES, rank);
	}

	@Override
	protected MutableComponent gatePassedMessage() {
		return Component.translatable(ModLang.MSG_ABODE_PUPPET_BOWS);
	}

	/** The Great Inheritance: the master's own method (to the Four Axis, of his element) and the Marrow-Washing Fruit. */
	@Override
	protected List<ItemStack> great(ServerLevel level) {
		return List.of(JadeSlipItem.of(HeavenMethod.of(masterElement(level.getSeed()), Realm.FOUR_AXIS)), new ItemStack(ModItems.MARROW_WASHING_FRUIT));
	}

	/** A lesser legacy: the method he cultivated before it (the Heavenly Being's), spirit stones, a Spring of the Ancients. */
	@Override
	protected List<ItemStack> lesser(ServerLevel level, RandomSource random) {
		return List.of(JadeSlipItem.of(HeavenMethod.of(masterElement(level.getSeed()), Realm.HEAVENLY_BEING)), new ItemStack(ModItems.MID_SPIRIT_STONE, 3 + random.nextInt(3)),
				new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
	}

	@Override
	protected MutableComponent inheritedMessage(Component who, SecretRealm realm) {
		return Component.translatable(ModLang.MSG_ABODE_INHERITED, who, realm.displayName());
	}

	@Override
	protected MutableComponent legacyMessage(String inheritor) {
		return Component.translatable(ModLang.MSG_ABODE_LEGACY, inheritor);
	}

	@Override
	protected MutableComponent silentMessage() {
		return Component.translatable(ModLang.MSG_ABODE_SILENT);
	}

	// --- Those who come for it (SecretRealmRivals) ---

	/**
	 * A herb in the garden a few steps off, as it passes (its way about the abode is its trials': flown at from outside, the mountain would be
	 * in the way, so its vaults are left to players).
	 */
	@Override
	protected BlockPos rivalPickings(ServerLevel level, long seed, BlockPos c, BlockPos near) {
		Frame fr = frame(CaveAbode.shape(seed), c);
		if (zone(local(fr, near)) != Zone.GARDEN) return null;
		BlockPos best = null;
		double d = 8 * 8;
		for (BlockPos at : herbSpots(fr)) {
			double here = at.distSqr(near);
			if (here < d && level.isLoaded(at) && level.getBlockState(at).getBlock() instanceof GinsengBlock) {
				d = here;
				best = at;
			}
		}
		return best;
	}

	@Override
	protected List<ItemStack> rivalTake(ServerLevel level, BlockPos at, CultivatorNpc npc) {
		return HerbValleyWorks.WORKS.rivalTake(level, at, npc);
	}

	/**
	 * Its trials a step at a time, on foot within the mountain (hall to hall toward the garden, cell by cell through the maze): the five pillars
	 * lit; the maze's heart; the master's puppet bested; through its barrier to his altar.
	 */
	@Override
	protected Trial rivalTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc) {
		CaveAbode.Shape s = CaveAbode.shape(level.getSeed());
		Frame fr = frame(s, r.realm.centre());
		UUID id = npc.getUUID();
		if (passed(r, "Claimed", id)) return null;
		int[] at = local(fr, npc.blockPosition());
		Zone zone = zone(at);
		Zone goal = !passed(r, "Elements", id) ? Zone.ELEMENTS : !passed(r, "Maze", id) ? Zone.MAZE : Zone.CHAMBER;
		if (zone == Zone.MAZE) return mazeStep(fr, at, goal == Zone.MAZE, s.seed);
		if (zone == goal) {
			return goal == Zone.ELEMENTS ? new Trial("pillars", fr.at(0, ELEMENTS_MIDDLE_S, 1), 3.5, true) : new Trial("altar", altar(fr), 2.5, true);
		}
		return switch (zone) {
			case GARDEN -> switch (goal) {
				case ELEMENTS -> new Trial("way", fr.at(0, -17, 1), 2.0, true);
				case MAZE -> new Trial("way", cellMiddle(fr, 0, MAZE_MID), 1.6, true);
				default -> passed(r, id) ? new Trial("door", fr.at(0, 11, 1), 2.0, true) : new Trial("duel", puppetPost(fr), 4, true);
			};
			case ELEMENTS -> new Trial("way", fr.at(0, -9, 1), 2.0, true);
			case LECTURE -> new Trial("way", at[0] < -26 ? fr.at(-24, 0, 1) : fr.at(-9, 0, 1), 2.0, true);
			case HALL -> new Trial("way", Math.abs(at[1]) > 9 ? fr.at(-41, 0, 1) : fr.at(-30, 0, 1), 2.0, true);
			case TUNNEL -> new Trial("way", fr.at(-41, 0, 1), 2.0, true);
			case CHAMBER -> null; // shut in before its time: nothing more for it
			// From outside: to the causeway's end before the porch (in the open: a flier over the porch's roof would take that for the
			// ground and hover there), then in through the gate on foot.
			default -> {
				boolean before = at[0] >= -68 && at[0] <= -57 && Math.abs(at[1]) <= 9; // on the forecourt or the causeway's end
				yield before || fr.at(-68, 0, 1).distSqr(npc.blockPosition()) <= 6 * 6 ? new Trial("way", fr.at(-52, 0, 1), 2.0, true)
						: new Trial("way", fr.at(-68, 0, 1), 3.0, false);
			}
		};
	}

	/** A step through the maze two cells on: toward its heart ({@code in}; at it, "heart") or back to its entrance (there, out to the garden). */
	private static Trial mazeStep(Frame fr, int[] at, boolean in, long seed) {
		Maze maze = maze(seed);
		int[] cell = cellAt(at);
		if (cell == null) cell = new int[] {0, MAZE_MID}; // in its doorway
		int[] goal = in ? new int[] {MAZE_MID, MAZE_MID} : new int[] {0, MAZE_MID};
		if (cell[0] == goal[0] && cell[1] == goal[1]) {
			return in ? new Trial("heart", cellMiddle(fr, goal[0], goal[1]), 1.5, true) : new Trial("way", fr.at(9, 0, 1), 2.0, true);
		}
		List<int[]> way = maze.path(cell, goal);
		int[] next = way.get(Math.min(2, way.size() - 1));
		return new Trial("way", cellMiddle(fr, next[0], next[1]), 1.5, true);
	}

	/** The pillars lit in their order (one come for it knows it), one each second; the heart reached; the puppet fought; the barrier passed; the altar. */
	@Override
	protected boolean rivalAtTrial(ServerLevel level, SecretRealms.Record r, CultivatorNpc npc, Trial trial, int working) {
		UUID id = npc.getUUID();
		switch (trial.step()) {
			case "pillars" -> {
				if (passed(r, "Elements", id)) return true;
				if (working % 20 != 19) return false;
				ElementPillarBlock.Element[] cycle = ElementPillarBlock.Element.values();
				ElementPillarBlock.Element next = cycle[r.works.contains("PillarLast") ? (r.works.getInt("PillarLast") + 1) % cycle.length : 0];
				npc.swing(InteractionHand.MAIN_HAND);
				light(level, r, SecretRealms.Data.get(level.getServer()), next, npc);
				return passed(r, "Elements", id) || working > 600;
			}
			case "heart" -> pass(r, "Maze", id);
			case "duel" -> {
				return challengeAt(level, r, npc, trial.mark(), working);
			}
			case "door" -> {
				// The barrier lets one who bested the puppet by: set down on its far side.
				BlockPos in = frame(CaveAbode.shape(level.getSeed()), r.realm.centre()).at(0, 16, 1);
				npc.getNavigation().stop();
				npc.teleportTo(in.getX() + 0.5, in.getY(), in.getZ() + 0.5);
			}
			case "altar" -> {
				pass(r, "Claimed", id);
				rivalInherits(level, r, npc);
			}
			default -> {
			}
		}
		return true;
	}

	private CaveAbodeWorks() {}
}
