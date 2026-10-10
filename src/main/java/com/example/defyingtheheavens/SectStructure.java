package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
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
 * A cultivation sect: a walled compound laid out along one axis in the manner of a Chinese temple, built from
 * {@link SectPiece}s (not jigsaw templates: the layout is planned here, in code, so it can keep its symmetry and fit its
 * population, and every building adapts its height to the ground).
 * <pre>
 *   back                             [pagoda]
 *           [garden] [main hall: the master's dais] [garden]
 *  dwellings        ( Formation plaza: core, array )          dwellings
 *  down each [treasury]                      [meditation hall] down each
 *  side             front court, lanterns, trees                 side
 *   front   [tower]           [ gate ]                [tower]
 * </pre>
 * Three sizes, for 10-12, 13-16 and 17-20 members, with a dwelling for each (the Sect Master sits in the main hall). The
 * Formation Core stands near the middle of the grounds; its barrier is sized to enclose every corner of the wall. Sects
 * appear only on fairly flat, dry land (and, in the Upper Realm, only on an island wide enough to hold them). Members are
 * not spawned here: the core carries the sect's blueprint (its posts, alignment and capacity) and founds the sect when it
 * first ticks (SectManager#found).
 */
public class SectStructure extends Structure {
	/** Ground within the walls may differ by at most this much (the rest is levelled by the terrain's beard). */
	/**
	 * How uneven the ground under the compound may be (highest sample less lowest). The terrace clears 22 blocks above its
	 * paving and founds itself up to 12 below, so this is about what it can level without leaving a hill through the halls.
	 * Surveyed 2026-10-10 over 441 candidate sites: in the Overworld about 1 in 8 attempts finds a site at 20, against 1 in
	 * 17 at the old 12; the Upper Realm's islands are rugged and only a few are wide enough, so it takes up to 24 there.
	 */
	private static final int MAX_RELIEF = 20;
	private static final int MAX_RELIEF_UPPER = 24;
	/** How many of the 13 ground samples may be water (a pond or a stream across the grounds is filled in). */
	private static final int MAX_WET = 3;

	private enum Size {
		//     W   D  min max  plaza front court  treasury(w,d) meditation(w,d) main(w)  pagoda tiers onAxis
		SMALL( 63, 79, 10, 12, 33, 14, 13, 11, 15, 11, 23, 9, 4, true),
		MEDIUM(73, 84, 13, 16, 37, 18, 15, 13, 17, 13, 27, 11, 5, true),
		LARGE( 81, 97, 17, 20, 48, 29, 17, 15, 19, 15, 31, 13, 7, true);

		final int w, d, minMembers, maxMembers, plazaV, courtDepth, treasuryW, treasuryD, meditationW, meditationD, mainW, pagoda, tiers;
		final boolean pagodaOnAxis;

		Size(int w, int d, int minMembers, int maxMembers, int plazaV, int courtDepth, int treasuryW, int treasuryD, int meditationW, int meditationD,
				int mainW, int pagoda, int tiers, boolean pagodaOnAxis) {
			this.w = w;
			this.d = d;
			this.minMembers = minMembers;
			this.maxMembers = maxMembers;
			this.plazaV = plazaV;
			this.courtDepth = courtDepth;
			this.treasuryW = treasuryW;
			this.treasuryD = treasuryD;
			this.meditationW = meditationW;
			this.meditationD = meditationD;
			this.mainW = mainW;
			this.pagoda = pagoda;
			this.tiers = tiers;
			this.pagodaOnAxis = pagodaOnAxis;
		}
	}

	private static final int MAIN_DEPTH = 21;

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
		Frame frame = new Frame(facing, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(), size.w, size.d);

		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		RandomState state = context.randomState();
		BlockPos centre = new BlockPos(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ());
		boolean upper = isUpperRealm(context, centre);

		// The ground, sampled over the whole compound: too uneven, too wet or (in the Upper Realm) over the edge of an island,
		// and there's no sect here.
		int[] samples = new int[25];
		int water = 0;
		int n = 0;
		for (int i = 0; i <= 4; i++) {
			for (int j = 0; j <= 4; j++) {
				BlockPos at = frame.world(size.w * i / 4, size.d * j / 4);
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
				builder -> plan(builder, random, size, frame, platformY, upper)));
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

	/** Upper Realm biomes are the mod's own (the Spatial Gap has no surface for a sect). */
	private static boolean isUpperRealm(GenerationContext context, BlockPos pos) {
		Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()),
				QuartPos.fromBlock(pos.getZ()), context.randomState().sampler());
		return biome.unwrapKey().map(key -> key.location().getNamespace().equals(DefyingTheHeavens.MOD_ID)).orElse(false);
	}

	/**
	 * The compound's frame: u from left to right across its front, v from the gate inward, as in SectPiece, laid in the world
	 * by {@code facing} (the way it extends from its gate).
	 */
	private record Frame(Direction facing, int cx, int cz, int w, int d) {
		/** The world column of compound point (u, v). */
		BlockPos world(int u, int v) {
			return switch (facing) {
				case NORTH -> new BlockPos(cx - w / 2 + u, 0, cz + d / 2 - v);
				case SOUTH -> new BlockPos(cx - w / 2 + u, 0, cz - d / 2 + v);
				case WEST -> new BlockPos(cx + d / 2 - v, 0, cz - w / 2 + u);
				default -> new BlockPos(cx - d / 2 + v, 0, cz - w / 2 + u);
			};
		}

		/** Compound +u in the world. */
		Direction right() {
			return facing.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
		}

		/** The world box of compound rectangle u0..u1, v0..v1, from y0 up {@code height}. */
		BoundingBox box(int u0, int v0, int u1, int v1, int y0, int height) {
			BlockPos a = world(u0, v0), b = world(u1, v1);
			return new BoundingBox(Math.min(a.getX(), b.getX()), y0, Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), y0 + height - 1,
					Math.max(a.getZ(), b.getZ()));
		}
	}

	private void plan(StructurePiecesBuilder builder, WorldgenRandom random, Size size, Frame frame, int y, boolean upper) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random);
		Alignment.Faction faction = Alignment.factionOf(alignment);
		int w = size.w, d = size.d, cu = w / 2, cv = size.plazaV;
		Direction back = frame.facing(), right = frame.right(), left = right.getOpposite();
		int capacity = size.minMembers + random.nextInt(size.maxMembers - size.minMembers + 1);
		int frontCourt = 7 + size.courtDepth - 4;

		List<SectPiece> pieces = new ArrayList<>();
		CompoundTag terraceData = new CompoundTag();
		terraceData.putInt("PlazaV", cv);
		terraceData.putInt("PlazaR", SectPiece.PLAZA_RADIUS);
		terraceData.putInt("FrontCourt", frontCourt);
		SectPiece terrace = new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, w - 1, d - 1, y, 24), back, seed, faction, upper, terraceData);
		pieces.add(terrace);
		SectPiece gate = new SectPiece(SectPiece.Kind.GATE, frame.box(cu - 10, 0, cu + 10, 6, y, 20), back, seed + 1, faction, upper, null);
		pieces.add(gate);
		int[][] corners = {{0, 0}, {w - 7, 0}, {0, d - 7}, {w - 7, d - 7}};
		for (int i = 0; i < corners.length; i++) {
			pieces.add(new SectPiece(SectPiece.Kind.TOWER, frame.box(corners[i][0], corners[i][1], corners[i][0] + 6, corners[i][1] + 6, y, 16), back,
					seed + 10 + i, faction, upper, null));
		}

		// The front court's two halls face the axis: the treasury on the left, the meditation hall on the right.
		int courtMid = 7 + size.courtDepth / 2;
		int tu0 = 9 + (w > 70 ? 2 : 0);
		SectPiece treasury = new SectPiece(SectPiece.Kind.TREASURY, frame.box(tu0, courtMid - size.treasuryW / 2, tu0 + size.treasuryD - 1,
				courtMid - size.treasuryW / 2 + size.treasuryW - 1, y, 18), left, seed + 20, faction, upper, null);
		pieces.add(treasury);
		int mu1 = w - 1 - tu0 - (w <= 63 ? 1 : 0);
		SectPiece meditation = new SectPiece(SectPiece.Kind.MEDITATION_HALL, frame.box(mu1 - size.meditationD + 1, courtMid - size.meditationW / 2, mu1,
				courtMid - size.meditationW / 2 + size.meditationW - 1, y, 18), right, seed + 21, faction, upper, null);
		pieces.add(meditation);

		// The main hall behind the plaza, and the pagoda behind it.
		int mainV0 = cv + SectPiece.PLAZA_RADIUS + 2;
		SectPiece main = new SectPiece(SectPiece.Kind.MAIN_HALL, frame.box(cu - size.mainW / 2, mainV0, cu - size.mainW / 2 + size.mainW - 1,
				mainV0 + MAIN_DEPTH - 1, y, 34), back, seed + 30, faction, upper, null);
		pieces.add(main);
		CompoundTag pagodaData = new CompoundTag();
		pagodaData.putInt("Tiers", size.tiers);
		int pv0 = mainV0 + MAIN_DEPTH;
		int pu0 = cu - size.pagoda / 2;
		SectPiece pagoda = new SectPiece(SectPiece.Kind.PAGODA, frame.box(pu0, pv0, pu0 + size.pagoda - 1, pv0 + size.pagoda - 1, y, 6 * size.tiers + 14),
				back, seed + 31, faction, upper, pagodaData);
		pieces.add(pagoda);

		// Herb gardens either side of the main hall.
		int gardenW = Math.max(10, cu - size.mainW / 2 - 2 - (tu0 + 0));
		int gv1 = d - 5;
		SectPiece leftGarden = new SectPiece(SectPiece.Kind.GARDEN, frame.box(tu0, mainV0, tu0 + gardenW - 1, gv1, y, 10), back, seed + 40, faction, upper, null);
		SectPiece rightGarden = new SectPiece(SectPiece.Kind.GARDEN, frame.box(w - tu0 - gardenW, mainV0, w - 1 - tu0, gv1, y, 10), back, seed + 41, faction,
				upper, null);
		pieces.add(leftGarden);
		pieces.add(rightGarden);

		// Dwellings down both sides, facing in; the rearmost of each side are elders' houses.
		List<SectPiece> dwellings = new ArrayList<>();
		List<int[]> slots = new ArrayList<>();
		for (int v0 = 9; v0 + 6 <= d - 9; v0 += 8) slots.add(new int[] {v0});
		for (int side = 0; side < 2; side++) {
			for (int i = 0; i < slots.size(); i++) {
				int v0 = slots.get(i)[0];
				boolean elder = i >= slots.size() - 2;
				int u0 = side == 0 ? 1 : w - 8;
				BoundingBox box = frame.box(u0, v0, u0 + 6, v0 + 6, y, 12);
				SectPiece house = new SectPiece(elder ? SectPiece.Kind.ELDER_HOUSE : SectPiece.Kind.DWELLING, box, side == 0 ? left : right,
						seed + 100 + side * 50 + i, faction, upper, null);
				dwellings.add(house);
			}
		}
		pieces.addAll(dwellings);

		// The plaza last, so its blueprint can name every post of the grounds.
		BoundingBox plazaBox = frame.box(cu - SectPiece.PLAZA_RADIUS, cv - SectPiece.PLAZA_RADIUS, cu + SectPiece.PLAZA_RADIUS, cv + SectPiece.PLAZA_RADIUS, y, 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, faction, upper, plazaData);
		BlockPos core = plaza.toWorld(SectPiece.PLAZA_RADIUS, 2, SectPiece.PLAZA_RADIUS);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = main.masterSeat();
		posts.get(Sect.Post.MASTER_SEAT).add(main.toWorld(seat[0], seat[1], seat[2]));
		for (int[] mat : meditation.meditationMats()) posts.get(Sect.Post.MEDITATION).add(meditation.toWorld(mat[0], 2, mat[1]));
		for (int[] guard : treasury.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(treasury.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Dwellings nearest the plaza first, so the strongest members (founded first) live closest to the heart.
		dwellings.sort((a, b) -> Double.compare(a.getBoundingBox().getCenter().distSqr(core), b.getBoundingBox().getCenter().distSqr(core)));
		for (SectPiece house : dwellings) {
			int[] mat = house.homeMat();
			posts.get(Sect.Post.DWELLING).add(house.toWorld(mat[0], mat[1], mat[2]));
		}
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		courtyard.add(terrace.toWorld(cu - 6, 1, courtMid));
		courtyard.add(terrace.toWorld(cu + 6, 1, courtMid));
		courtyard.add(terrace.toWorld(cu, 1, 9));
		for (int k = 0; k < 4; k++) {
			double angle = Math.PI / 2 * k;
			courtyard.add(plaza.toWorld(SectPiece.PLAZA_RADIUS + (int) Math.round(Math.cos(angle) * 10.5), 1,
					SectPiece.PLAZA_RADIUS + (int) Math.round(Math.sin(angle) * 10.5)));
		}
		courtyard.add(leftGarden.toWorld(leftGarden.width() / 2, 1, 1));
		courtyard.add(rightGarden.toWorld(rightGarden.width() / 2, 1, 1));
		int[] top = pagoda.pagodaSeat();
		courtyard.add(pagoda.toWorld(top[0], top[1], top[2]));
		int[] in = gate.gateInside();
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));

		// The barrier: from the core out to the furthest corner of the wall, and a little more.
		int radius = 0;
		for (int[] corner : new int[][] {{0, 0}, {w - 1, 0}, {0, d - 1}, {w - 1, d - 1}}) {
			BlockPos c = frame.world(corner[0], corner[1]);
			double dx = c.getX() - core.getX(), dz = c.getZ() - core.getZ();
			radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
		}
		radius = Math.min(Formations.MAX_RADIUS, radius);

		CompoundTag blueprint = new CompoundTag();
		blueprint.putLong("Seed", seed);
		blueprint.putInt("Alignment", alignment);
		blueprint.putInt("Capacity", Math.min(capacity, posts.get(Sect.Post.DWELLING).size() + 1));
		blueprint.putInt("Radius", radius);
		BoundingBox bounds = terrace.getBoundingBox();
		blueprint.putIntArray("Bounds", new int[] {bounds.minX(), bounds.minY() - 16, bounds.minZ(), bounds.maxX(), bounds.maxY() + 48, bounds.maxZ()});
		BoundingBox vault = treasury.getBoundingBox().inflatedBy(1);
		blueprint.putIntArray("Treasury", new int[] {vault.minX(), vault.minY(), vault.minZ(), vault.maxX(), vault.maxY(), vault.maxZ()});
		CompoundTag postTag = new CompoundTag();
		for (Map.Entry<Sect.Post, List<BlockPos>> entry : posts.entrySet()) {
			postTag.put(entry.getKey().name(), new LongArrayTag(entry.getValue().stream().mapToLong(BlockPos::asLong).toArray()));
		}
		blueprint.put("Posts", postTag);
		plazaData.put("Blueprint", blueprint);
		pieces.add(plaza);

		for (SectPiece piece : pieces) builder.addPiece(piece);
	}
}
