package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A cultivation sect: an open village, no wall round it, gathered about the Formation Core at its heart. Built from
 * {@link SectPiece}s (not jigsaw templates: the layout is planned here, in code, so it can keep its order and fit its
 * population, and every building adapts its base to the ground).
 * <pre>
 *            [pagoda] :  (garden) [ MAIN HALL ] (garden)  : [pavilion ~pond~]
 *                     :.......... lane round the back .........:
 *   [side gate]==homes=:=elders==( plaza: the core )==elders==:=homes==[side gate]
 *              homes   :  [treasury]       ||       [meditation hall] :   homes
 *              homes   :.......... lane across the front .............:   homes
 *                                       [ main gate ]
 * </pre>
 * The plaza (the core on its dais, the array round it) is the middle of everything: the Sect Master's hall stands at its
 * back edge, the roads run from it to three paifang gates (the main gate in front, one either side), the treasury and the
 * meditation hall face each other across the gate road, and the elders' houses stand on the cross road by the plaza. Two
 * lanes run front to back either side, joined across the front and round the back of the hall (whose back steps come down
 * to it); out past them are the disciples' houses and the outer disciples' dormitories, on the cross road and facing the
 * lanes, and the pagoda, and a lotus pond with its pavilion; herb gardens flank the hall. Trees, bamboo and mossy rocks fill the open ground.
 * <p>
 * Three sizes, for 10-12, 13-16 and 17-20 members, with a mat to sleep on for each (the Sect Master's is in the main hall).
 * The Formation Core's barrier is sized to enclose every building. Sects appear only on fairly flat, dry land (and, in the
 * Upper Realm, only on an island wide enough to hold them); within the village's round the ground is levelled and no
 * vanilla tree grows (see TreeFeatureMixin). Members are not spawned here: the core carries the sect's blueprint (its
 * posts, alignment and capacity) and founds the sect when it first ticks (SectManager#found).
 */
public class SectStructure extends Structure {
	/**
	 * How uneven the ground in the middle of the village may be (highest sample less lowest). The grounds are cleared 24 blocks
	 * above the plaza and filled up to 12 below it, so this is about what they can level without leaving a hill through the halls.
	 * The Upper Realm's islands are rugged and only a few are wide enough, so it takes more there.
	 */
	private static final int MAX_RELIEF = 20;
	private static final int MAX_RELIEF_UPPER = 24;
	/** How many of the 13 ground samples may be water (a pond or a stream across the grounds is filled in). */
	private static final int MAX_WET = 3;
	/** Within this of a sect's core no vanilla tree grows: it would come up through a roof or across a road. */
	public static final int TREELESS_RADIUS = Formations.MAX_RADIUS;

	private enum Size {
		//      members  elders houses dorms tiers pagoda main  medW medD treW treD pondW pondD gateRoad sideRoad trees
		SMALL( 10, 12,   2,     1,     2,    3,    9,     23,   15,  11,  13,  11,  11,   13,   26,      55,      12),
		MEDIUM(13, 16,   3,     2,     2,    5,    11,    27,   17,  13,  15,  13,  13,   15,   32,      55,      18),
		LARGE( 17, 20,   4,     2,     3,    5,    13,    31,   19,  15,  17,  15,  15,   17,   32,      55,      24);

		final int minMembers, maxMembers, elders, houses, dorms, tiers, pagoda, mainW, medW, medD, treW, treD, pondW, pondD, gateRoad, sideRoad, trees;

		Size(int minMembers, int maxMembers, int elders, int houses, int dorms, int tiers, int pagoda, int mainW, int medW, int medD, int treW,
				int treD, int pondW, int pondD, int gateRoad, int sideRoad, int trees) {
			this.minMembers = minMembers;
			this.maxMembers = maxMembers;
			this.elders = elders;
			this.houses = houses;
			this.dorms = dorms;
			this.tiers = tiers;
			this.pagoda = pagoda;
			this.mainW = mainW;
			this.medW = medW;
			this.medD = medD;
			this.treW = treW;
			this.treD = treD;
			this.pondW = pondW;
			this.pondD = pondD;
			this.gateRoad = gateRoad;
			this.sideRoad = sideRoad;
			this.trees = trees;
		}

		/** About how far the village reaches from its core (for surveying the ground). */
		int reach() {
			return sideRoad + 2;
		}
	}

	private static final int MAIN_DEPTH = 25;
	private static final int P = SectPiece.PLAZA_RADIUS;
	/** The lanes run front to back this far either side of the axis, joined across in front and round the back of the main hall. */
	private static final int LANE_U = 33;

	public SectStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.SECT_TYPE;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		WorldgenRandom random = context.random();
		float roll = random.nextFloat();
		Size size = roll < 0.35f ? Size.SMALL : roll < 0.75f ? Size.MEDIUM : Size.LARGE;
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		ChunkPos chunk = context.chunkPos();
		Frame frame = new Frame(facing, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());

		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		RandomState state = context.randomState();
		BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ());
		boolean upper = isUpperRealm(context, centre);

		// The ground, sampled over the heart of the village: too uneven, too wet or (in the Upper Realm) over the edge of an
		// island, and there's no sect here.
		int reach = size.reach() * 4 / 5;
		int[] samples = new int[25];
		int water = 0;
		int n = 0;
		for (int i = 0; i <= 4; i++) {
			for (int j = 0; j <= 4; j++) {
				BlockPos at = frame.world(-reach + reach * i / 2, -reach + reach * j / 2);
				int y = generator.getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, state);
				if (y <= heights.getMinBuildHeight() + 2) return Optional.empty();
				samples[n++] = y;
				if ((i + j) % 2 == 0) {
					NoiseColumn column = generator.getBaseColumn(at.getX(), at.getZ(), heights, state);
					BlockState top = column.getBlock(y);
					if (!top.getFluidState().isEmpty()) water++;
				}
			}
		}
		int[] sorted = samples.clone();
		Arrays.sort(sorted);
		if (sorted[24] - sorted[0] > (upper ? MAX_RELIEF_UPPER : MAX_RELIEF) || water > MAX_WET) return Optional.empty();
		int platformY = sorted[12];
		if (platformY + 70 > heights.getMaxBuildHeight()) return Optional.empty();
		return Optional.of(new GenerationStub(new BlockPos(chunk.getMiddleBlockX(), platformY, chunk.getMiddleBlockZ()),
				builder -> new Plan(builder, random, size, frame, platformY, upper).build()));
	}

	/**
	 * Whether any chunk over blocks x0..x1, z0..z1 holds part of a sect (it carries a reference to one), for worldgen that
	 * mustn't cut into one: see LakeFeatureMixin. Chunks the generating region can't reach are skipped.
	 */
	public static boolean touchesSect(WorldGenLevel level, int x0, int z0, int x1, int z1) {
		for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
			for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
				if (chunk == null) continue;
				for (Structure structure : chunk.getAllReferences().keySet()) {
					if (structure instanceof SectStructure) return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether (x, z) lies within {@link #TREELESS_RADIUS} of a sect's core, for trees that mustn't grow there (see
	 * TreeFeatureMixin). A sect's core is the middle of the chunk it started in, which every chunk it reaches names.
	 */
	public static boolean inSectGrounds(WorldGenLevel level, int x, int z) {
		if (!level.hasChunk(x >> 4, z >> 4)) return false;
		ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.STRUCTURE_REFERENCES, false);
		if (chunk == null) return false;
		for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry : chunk.getAllReferences().entrySet()) {
			if (!(entry.getKey() instanceof SectStructure)) continue;
			for (long start : entry.getValue()) {
				ChunkPos at = new ChunkPos(start);
				double dx = x - at.getMiddleBlockX(), dz = z - at.getMiddleBlockZ();
				if (dx * dx + dz * dz <= (double) TREELESS_RADIUS * TREELESS_RADIUS) return true;
			}
		}
		return false;
	}

	/** Upper Realm biomes are the mod's own (the Spatial Gap has no surface for a sect). */
	private static boolean isUpperRealm(GenerationContext context, BlockPos pos) {
		Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()),
				QuartPos.fromBlock(pos.getZ()), context.randomState().sampler());
		return biome.unwrapKey().map(key -> key.location().getNamespace().equals(DefyingTheHeavens.MOD_ID)).orElse(false);
	}

	/**
	 * The village's frame: u across it (to the right, looking in from the main gate), v from the core toward its back (the
	 * main gate is at negative v), centred on the core; laid in the world by {@code facing} (the way in from the main gate).
	 */
	private record Frame(Direction facing, int cx, int cz) {
		BlockPos world(int u, int v) {
			return switch (facing) {
				case SOUTH -> new BlockPos(cx + u, 0, cz + v);
				case NORTH -> new BlockPos(cx + u, 0, cz - v);
				case EAST -> new BlockPos(cx + v, 0, cz + u);
				default -> new BlockPos(cx - v, 0, cz + u);
			};
		}

		/** Village +u in the world. */
		Direction right() {
			return facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
		}

		/** The world box of village rectangle u0..u1, v0..v1, from y0 up {@code height}. */
		BoundingBox box(int u0, int v0, int u1, int v1, int y0, int height) {
			BlockPos a = world(u0, v0), b = world(u1, v1);
			return new BoundingBox(Math.min(a.getX(), b.getX()), y0, Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), y0 + height - 1,
					Math.max(a.getZ(), b.getZ()));
		}
	}

	/** Which way a building's door faces, in the village's frame. */
	private enum Door { FRONT, BACK, LEFT, RIGHT }

	/** One village being laid out. */
	private static final class Plan {
		private final StructurePiecesBuilder builder;
		private final Size size;
		private final Frame frame;
		private final int y;
		private final boolean upper;
		private final long seed;
		private final int alignment;
		private final Alignment.Faction faction;
		private final List<SectPiece> pieces = new ArrayList<>();
		/** Village rectangles {u0, v0, u1, v1}: what the buildings stand on. */
		private final List<int[]> footprints = new ArrayList<>();
		/** Village rectangles {u0, v0, u1, v1, kind}. */
		private final List<int[]> paths = new ArrayList<>();
		private final List<int[]> lanterns = new ArrayList<>();
		private final List<int[]> trees = new ArrayList<>();
		private int next;

		Plan(StructurePiecesBuilder builder, WorldgenRandom random, Size size, Frame frame, int y, boolean upper) {
			this.builder = builder;
			this.size = size;
			this.frame = frame;
			this.y = y;
			this.upper = upper;
			this.seed = random.nextLong();
			this.alignment = Alignment.randomSect(random);
			this.faction = Alignment.factionOf(alignment);
		}

		private Direction orientation(Door door) {
			Direction back = frame.facing(), right = frame.right();
			return switch (door) {
				case FRONT -> back;                 // the door looks to the front: the building extends toward the back
				case BACK -> back.getOpposite();
				case RIGHT -> right.getOpposite();
				case LEFT -> right;
			};
		}

		private SectPiece add(SectPiece.Kind kind, int u0, int v0, int u1, int v1, Door door, int height, CompoundTag extra) {
			int a = Math.min(u0, u1), b = Math.min(v0, v1), c = Math.max(u0, u1), d = Math.max(v0, v1);
			SectPiece piece = new SectPiece(kind, frame.box(a, b, c, d, y, height), orientation(door), seed + 1 + next++, faction, upper, extra);
			pieces.add(piece);
			if (kind != SectPiece.Kind.GROUNDS) footprints.add(new int[] {a, b, c, d});
			return piece;
		}

		private void path(int u0, int v0, int u1, int v1, int kind) {
			paths.add(new int[] {Math.min(u0, u1), Math.min(v0, v1), Math.max(u0, u1), Math.max(v0, v1), kind});
		}

		/** A lane from the middle of a building's door side straight out to the line {@code to} (a v, or a u for a side door). */
		private void spur(int[] r, Door door, int to, int width) {
			int cu = (r[0] + r[2]) / 2, cv = (r[1] + r[3]) / 2, h = width / 2;
			switch (door) {
				case FRONT -> path(cu - h, r[1], cu + h, to, SectPiece.PATH_LANE);
				case BACK -> path(cu - h, r[3], cu + h, to, SectPiece.PATH_LANE);
				case LEFT -> path(r[0], cv - h, to, cv + h, SectPiece.PATH_LANE);
				case RIGHT -> path(r[2], cv - h, to, cv + h, SectPiece.PATH_LANE);
			}
		}

		/**
		 * A home and its lane to the road, in slot {@code slot} on side {@code side} (+1 right, -1 left): 'A' in front of the
		 * cross road and 'B' behind it, both by the plaza; 'C' behind it out past the lanes; 'D' and 'E' out past the lanes in
		 * front of it, facing the lane (D nearer the cross road).
		 */
		private SectPiece home(SectPiece.Kind kind, int slot, int side) {
			int w = kind == SectPiece.Kind.ELDER_HOUSE ? 15 : kind == SectPiece.Kind.DWELLING ? 13 : 17;
			int d = kind == SectPiece.Kind.ELDER_HOUSE ? 12 : 11;
			CompoundTag extra = kind == SectPiece.Kind.ELDER_HOUSE ? null : tag("Mats", kind == SectPiece.Kind.DORMITORY ? 4 : 2);
			if (slot == 'A' || slot == 'B' || slot == 'C') {
				int u0 = slot == 'C' ? 36 : 16;
				int a = side > 0 ? u0 : -u0 - w + 1, b = side > 0 ? u0 + w - 1 : -u0;
				int[] r = slot == 'A' ? new int[] {a, -3 - d + 1, b, -3} : new int[] {a, 3, b, 3 + d - 1};
				Door door = slot == 'A' ? Door.BACK : Door.FRONT;
				spur(r, door, slot == 'A' ? -1 : 1, 1);
				return add(kind, r[0], r[1], r[2], r[3], door, 16, extra);
			}
			// Turned to face the lane: the house's width runs along v, its depth along u.
			int a = side > 0 ? 36 : -36 - d + 1, b = side > 0 ? 36 + d - 1 : -36;
			int v1 = slot == 'D' ? -6 : -26;
			int[] r = {a, v1 - w + 1, b, v1};
			Door door = side > 0 ? Door.LEFT : Door.RIGHT;
			spur(r, door, side * (LANE_U + 1), 1);
			return add(kind, r[0], r[1], r[2], r[3], door, 16, extra);
		}

		private static CompoundTag tag(String key, int value) {
			CompoundTag tag = new CompoundTag();
			tag.putInt(key, value);
			return tag;
		}

		void build() {
			Size s = size;
			// The heart: the plaza, its blueprint filled in last.
			CompoundTag plazaData = new CompoundTag();
			SectPiece plaza = add(SectPiece.Kind.PLAZA, -P, -P, P, P, Door.FRONT, 8, plazaData);
			// The Sect Master's hall, its steps on the plaza.
			int mw = s.mainW;
			SectPiece main = add(SectPiece.Kind.MAIN_HALL, -(mw / 2), P + 2, mw / 2, P + 1 + MAIN_DEPTH, Door.FRONT, 34, null);
			path(-3, P, 3, P + 2, SectPiece.PATH_ROAD);
			// The gate road and the main gate.
			int gateV = -(P + s.gateRoad);
			path(-2, gateV + 2, 2, -P, SectPiece.PATH_ROAD);
			CompoundTag bigGate = new CompoundTag();
			bigGate.putBoolean("Big", true);
			SectPiece gate = add(SectPiece.Kind.GATE, -9, gateV - 1, 9, gateV + 1, Door.FRONT, 14, bigGate);
			// The treasury (left) and the meditation hall (right), facing each other across the gate road.
			int mid = -(P + 6) - Math.max(s.treW, s.medW) / 2;
			int[] tr = {-(s.treD + 9), mid - s.treW / 2, -10, mid - s.treW / 2 + s.treW - 1};
			SectPiece treasury = add(SectPiece.Kind.TREASURY, tr[0], tr[1], tr[2], tr[3], Door.RIGHT, 18, null);
			spur(tr, Door.RIGHT, -2, 3);
			int[] md = {10, mid - s.medW / 2, 10 + s.medD - 1, mid - s.medW / 2 + s.medW - 1};
			SectPiece meditation = add(SectPiece.Kind.MEDITATION_HALL, md[0], md[1], md[2], md[3], Door.LEFT, 18, null);
			spur(md, Door.LEFT, 2, 3);
			// The cross road and its side gates.
			int side = s.sideRoad;
			path(-side + 1, -1, -P, 1, SectPiece.PATH_ROAD);
			path(P, -1, side - 1, 1, SectPiece.PATH_ROAD);
			add(SectPiece.Kind.GATE, side, -4, side + 2, 4, Door.LEFT, 12, null);
			add(SectPiece.Kind.GATE, -side - 2, -4, -side, 4, Door.RIGHT, 12, null);
			// The lanes: either side, from the front of the village to behind the main hall, joined across front and back, and up
			// to the hall's back steps.
			int frontV = gateV + 3, backV = P + 1 + MAIN_DEPTH + 6;
			for (int sgn : new int[] {-1, 1}) path(sgn * LANE_U - 1, frontV, sgn * LANE_U + 1, backV, SectPiece.PATH_LANE);
			path(-LANE_U, backV, LANE_U, backV + 1, SectPiece.PATH_LANE);
			path(-LANE_U, frontV, LANE_U, frontV + 1, SectPiece.PATH_LANE);
			path(-1, P + 1 + MAIN_DEPTH, 1, backV, SectPiece.PATH_LANE);
			// Homes, nearest the core first: the elders' houses either side of the plaza on the cross road; the disciples' houses
			// and dormitories out past the lanes, on the cross road behind it and facing the lanes in front of it.
			List<SectPiece> homes = new ArrayList<>();
			int[][] elderSlots = {{'A', 1}, {'B', -1}, {'B', 1}, {'A', -1}};
			int[][] outerSlots = {{'C', 1}, {'C', -1}, {'D', 1}, {'D', -1}, {'E', 1}, {'E', -1}};
			List<SectPiece.Kind> kinds = new ArrayList<>();
			for (int i = 0; i < s.houses; i++) kinds.add(SectPiece.Kind.DWELLING);
			for (int i = 0; i < s.dorms; i++) kinds.add(SectPiece.Kind.DORMITORY);
			for (int i = 0; i < s.elders; i++) homes.add(home(SectPiece.Kind.ELDER_HOUSE, elderSlots[i][0], elderSlots[i][1]));
			for (int i = 0; i < kinds.size(); i++) homes.add(home(kinds.get(i), outerSlots[i][0], outerSlots[i][1]));
			// The pagoda (left), herb gardens either side of the hall, the pond and its pavilion (right).
			int pw = s.pagoda;
			int[] pg = {-LANE_U - 3 - pw, P + 6, -LANE_U - 3, P + 6 + pw - 1};
			SectPiece pagoda = add(SectPiece.Kind.PAGODA, pg[0], pg[1], pg[2], pg[3], Door.RIGHT, 6 * s.tiers + 16, tag("Tiers", s.tiers));
			spur(pg, Door.RIGHT, -LANE_U + 1, 3);
			List<SectPiece> gardens = new ArrayList<>();
			for (int sgn : new int[] {-1, 1}) {
				int a = sgn * (mw / 2 + 3), b = sgn * (LANE_U - 2);
				gardens.add(add(SectPiece.Kind.GARDEN, Math.min(a, b), P + 5, Math.max(a, b), P + 5 + 17, sgn > 0 ? Door.LEFT : Door.RIGHT, 10, null));
			}
			int pondU0 = LANE_U + 6;
			add(SectPiece.Kind.POND, pondU0, P + 3, pondU0 + s.pondW - 1, P + 3 + s.pondD - 1, Door.LEFT, 8, null);
			int[] pv = {LANE_U + 2, P + 7, LANE_U + 10, P + 15};
			SectPiece pavilion = add(SectPiece.Kind.PAVILION, pv[0], pv[1], pv[2], pv[3], Door.LEFT, 12, null);
			spur(pv, Door.LEFT, LANE_U + 1, 1);
			// Stone lanterns down the roads, clear of the lanes to the doors.
			for (int v = gateV + 5; v <= -P - 2; v += 6) {
				lanterns.add(new int[] {-4, v});
				lanterns.add(new int[] {4, v});
			}
			for (int u = P + 4; u < side - 2; u += 8) {
				for (int[] at : new int[][] {{u, -2}, {-u, 2}}) {
					int lu = at[0];
					while (onPath(lu, at[1])) lu += lu > 0 ? 1 : -1;
					lanterns.add(new int[] {lu, at[1]});
				}
			}
			int radius = 0;
			for (int[] f : footprints) {
				for (int u : new int[] {f[0], f[2]}) {
					for (int v : new int[] {f[1], f[3]}) radius = Math.max(radius, (int) Math.ceil(Math.sqrt(u * u + v * v)));
				}
			}
			radius += 2;
			// Trees, bamboo and rocks on the open ground.
			RandomSource random = RandomSource.create(seed);
			int[] kindsOfTree = faction == Alignment.Faction.NEUTRAL
					? new int[] {SectPiece.TREE_PINE, SectPiece.TREE_PINE, SectPiece.TREE_BLOSSOM, SectPiece.TREE_BAMBOO, SectPiece.TREE_ROCK}
					: new int[] {SectPiece.TREE_BLOSSOM, SectPiece.TREE_BLOSSOM, SectPiece.TREE_PINE, SectPiece.TREE_BAMBOO, SectPiece.TREE_ROCK};
			for (int attempt = 0; attempt < 4000 && trees.size() < s.trees; attempt++) {
				double a = random.nextDouble() * Math.PI * 2, r = 16 + random.nextDouble() * (radius - 20);
				int u = (int) Math.round(Math.cos(a) * r), v = (int) Math.round(Math.sin(a) * r);
				if (blocked(u, v, 3)) continue;
				boolean crowded = false;
				for (int[] t : trees) crowded |= Math.abs(u - t[0]) < 5 && Math.abs(v - t[1]) < 5;
				if (crowded) continue;
				trees.add(new int[] {u, v, kindsOfTree[random.nextInt(kindsOfTree.length)]});
			}

			// The grounds, drawn first, so every building stands on them.
			BoundingBox groundBox = frame.box(-radius, -radius, radius, radius, y, 24);
			CompoundTag groundData = new CompoundTag();
			groundData.putInt("CentreX", frame.cx());
			groundData.putInt("CentreZ", frame.cz());
			groundData.putInt("Radius", radius);
			ListTag pathTags = new ListTag();
			for (int[] path : paths) pathTags.add(new IntArrayTag(worldRect(path[0], path[1], path[2], path[3], path[4])));
			pathTags.add(new IntArrayTag(worldRect(-P - 2, -P - 2, P + 2, P + 2, SectPiece.PATH_RING)));
			groundData.put("Paths", pathTags);
			ListTag lanternTags = new ListTag();
			for (int[] at : lanterns) {
				BlockPos pos = frame.world(at[0], at[1]);
				lanternTags.add(new IntArrayTag(new int[] {pos.getX(), pos.getZ()}));
			}
			groundData.put("Lanterns", lanternTags);
			ListTag treeTags = new ListTag();
			for (int[] t : trees) {
				BlockPos pos = frame.world(t[0], t[1]);
				treeTags.add(new IntArrayTag(new int[] {pos.getX(), pos.getZ(), t[2]}));
			}
			groundData.put("Trees", treeTags);
			SectPiece grounds = new SectPiece(SectPiece.Kind.GROUNDS, groundBox, frame.facing(), seed, faction, upper, groundData);

			// The posts of the grounds, for the blueprint.
			BlockPos core = plaza.toWorld(P, 2, P);
			Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
			for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
			int[] seat = main.masterSeat();
			posts.get(Sect.Post.MASTER_SEAT).add(main.toWorld(seat[0], seat[1], seat[2]));
			for (int[] mat : meditation.meditationMats()) posts.get(Sect.Post.MEDITATION).add(meditation.toWorld(mat[0], 2, mat[1]));
			for (int[] guard : treasury.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(treasury.toWorld(guard[0], guard[1], guard[2]));
			for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
			// Every mat in every home, nearest the core first, so the strongest members (founded first) live closest to the heart.
			List<BlockPos> mats = new ArrayList<>();
			for (SectPiece home : homes) {
				for (int[] mat : home.homeMats()) mats.add(home.toWorld(mat[0], mat[1], mat[2]));
			}
			mats.sort((a, b) -> Double.compare(a.distSqr(core), b.distSqr(core)));
			posts.get(Sect.Post.DWELLING).addAll(mats);
			List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
			for (int i = 0; i < 4; i++) {
				double angle = Math.PI / 2 * i + Math.PI / 4;
				courtyard.add(plaza.toWorld(P + (int) Math.round(Math.cos(angle) * 10.5), 1, P + (int) Math.round(Math.sin(angle) * 10.5)));
			}
			for (int[] at : new int[][] {{0, gateV / 2}, {P + 6, 0}, {-P - 6, 0}, {0, P + 1 + MAIN_DEPTH + 6}}) {
				BlockPos pos = frame.world(at[0], at[1]);
				courtyard.add(new BlockPos(pos.getX(), y + 1, pos.getZ()));
			}
			for (SectPiece garden : gardens) courtyard.add(garden.toWorld(garden.width() / 2, 1, 1));
			int[] sit = pavilion.pavilionSeat();
			courtyard.add(pavilion.toWorld(sit[0], sit[1], sit[2]));
			int[] top = pagoda.pagodaSeat();
			courtyard.add(pagoda.toWorld(top[0], top[1], top[2]));
			int[] in = gate.gateInside();
			posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));

			// The barrier: from the core out past the furthest corner of every building.
			int barrier = 0;
			for (SectPiece piece : pieces) {
				BoundingBox box = piece.getBoundingBox();
				for (int x : new int[] {box.minX(), box.maxX()}) {
					for (int z : new int[] {box.minZ(), box.maxZ()}) {
						double dx = x - core.getX(), dz = z - core.getZ();
						barrier = Math.max(barrier, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
					}
				}
			}
			barrier = Math.min(Formations.MAX_RADIUS, barrier);

			int capacity = s.minMembers + new java.util.Random(seed).nextInt(s.maxMembers - s.minMembers + 1);
			CompoundTag blueprint = new CompoundTag();
			blueprint.putLong("Seed", seed);
			blueprint.putInt("Alignment", alignment);
			blueprint.putInt("Capacity", Math.min(capacity, posts.get(Sect.Post.DWELLING).size() + 1));
			blueprint.putInt("Radius", barrier);
			blueprint.putIntArray("Bounds", new int[] {groundBox.minX(), groundBox.minY() - 16, groundBox.minZ(), groundBox.maxX(), groundBox.maxY() + 48,
					groundBox.maxZ()});
			BoundingBox vault = treasury.getBoundingBox().inflatedBy(1);
			blueprint.putIntArray("Treasury", new int[] {vault.minX(), vault.minY(), vault.minZ(), vault.maxX(), vault.maxY(), vault.maxZ()});
			CompoundTag postTag = new CompoundTag();
			for (Map.Entry<Sect.Post, List<BlockPos>> entry : posts.entrySet()) {
				postTag.put(entry.getKey().name(), new LongArrayTag(entry.getValue().stream().mapToLong(BlockPos::asLong).toArray()));
			}
			blueprint.put("Posts", postTag);
			plazaData.put("Blueprint", blueprint);

			builder.addPiece(grounds);
			for (SectPiece piece : pieces) builder.addPiece(piece);
		}

		private int[] worldRect(int u0, int v0, int u1, int v1, int kind) {
			BlockPos a = frame.world(u0, v0), b = frame.world(u1, v1);
			return new int[] {Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()), kind};
		}

		private boolean onPath(int u, int v) {
			for (int[] r : paths) {
				if (u >= r[0] && u <= r[2] && v >= r[1] && v <= r[3]) return true;
			}
			return false;
		}

		/** Within {@code margin} of a building or a path, or close by the plaza. */
		private boolean blocked(int u, int v, int margin) {
			for (int[] r : footprints) {
				if (u >= r[0] - margin && u <= r[2] + margin && v >= r[1] - margin && v <= r[3] + margin) return true;
			}
			for (int[] r : paths) {
				if (u >= r[0] - margin && u <= r[2] + margin && v >= r[1] - margin && v <= r[3] + margin) return true;
			}
			return u * u + v * v < (P + 4) * (P + 4);
		}
	}
}
