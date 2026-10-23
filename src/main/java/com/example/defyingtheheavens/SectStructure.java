package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.LongSet;
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
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A cultivation sect: an open village of halls and houses on the grass round a paved Formation plaza, with no wall (its barrier
 * is its wall), built from {@link SectPiece}s (not jigsaw templates: the layout is planned here and in {@link SectLayout}, in
 * code, so it fits its population and every building adapts its height to the ground).
 * <pre>
 *   back                         [garden]
 *        [garden] [main hall: the master's dais] [pagoda]
 *        ==torii===( Formation plaza: core, array )===torii==
 *      elders' houses round the plaza, trees and bamboo
 *              [treasury]  lanterns  [meditation hall]
 *   front                       [ gate ]
 * </pre>
 * A paved way runs from the gate up the middle to the plaza and on to the main hall's steps, and out to either side through
 * a torii; footpaths lead from it to every door. The buildings stand within the size's reach of the plaza, so the barrier
 * (sized to them) encloses them, while the gates stand {@link #GATE_BEYOND} further out, outside it, with posts for the
 * gate guards on the road beyond each. The elders' houses scatter over the lawns, with cherry trees and bamboo on the open
 * grass between (SectLayout). The pagoda and the second garden swap sides from sect to sect, as do the
 * treasury and the meditation hall.
 * <p>
 * Three sizes, for 10-12, 13-16 and 17-20 members. The Sect Master sits in the main hall, each elder has a house, and the
 * disciples live in the meditation hall, a mat each. The Formation Core stands at the middle of the grounds; its barrier is
 * sized to enclose all of them. Sects appear only on fairly flat, dry land (and, in the Upper Realm, only on an island wide
 * enough to hold them: there a sect looks up to 48 blocks round its site for one, and takes a smaller size if the island
 * is too small for the one rolled). Members are not spawned here: the core carries the sect's blueprint (its posts,
 * alignment and capacity) and founds the sect when it first ticks (SectManager#found).
 */
public class SectStructure extends Structure {
	/**
	 * How uneven the ground under the grounds may be (highest sample less lowest). The grounds are cleared 22 blocks above the
	 * level of the plaza and filled up to 12 below, so this is about what they can level without leaving a hill through the
	 * halls.
	 * Surveyed 2026-10-10 over 441 candidate sites: in the Overworld about 1 in 8 attempts finds a site at 20, against 1 in
	 * 17 at the old 12; the Upper Realm's islands are rugged and only a few are wide enough, so it takes up to 24 there.
	 */
	private static final int MAX_RELIEF = 20;
	private static final int MAX_RELIEF_UPPER = 24;
	/**
	 * How far the land just beyond the gates and at the far corners may rise above the grounds: the grounds are cut level out
	 * to there, so higher land would stand as a cliff across the way out of a gate.
	 */
	private static final int MAX_EDGE_RISE = 8;
	/** How many of the 13 ground samples may be water (a pond or a stream across the grounds is filled in). */
	private static final int MAX_WET = 3;

	enum Size {
		//    reach min max  treasury(w,d) meditation(w,d) main(w) pagoda tiers garden(w,d) trees bamboo
		// Reach spread out to the barrier's limit (Formations.MAX_RADIUS 64 = the largest reach + 2) on 2026-10-12.
		// The treasury grew on 2026-10-13 (it was 13x11 / 15x13 / 17x15, squat under its roof): never larger than the meditation
		// hall facing it across the way, so it fits the same ground.
		SMALL( 52, 10, 12, 17, 13, 19, 13, 23, 9, 3, 11, 15, 40, 18),
		MEDIUM(58, 13, 16, 17, 15, 19, 15, 27, 11, 5, 12, 17, 52, 22),
		LARGE( 62, 17, 20, 19, 15, 19, 15, 31, 13, 7, 13, 19, 62, 26);

		/**
		 * How far from the plaza the buildings may reach (the barrier closes just outside them, and is never wider than
		 * Formations.MAX_RADIUS); the gates stand {@link #GATE_BEYOND} further out, outside the barrier, and the grounds reach a
		 * few blocks past the gates.
		 */
		final int reach;
		/** Half the grounds' width and depth: the plaza is at (a, b) of a (2a + 1) by (2b + 1) frame. */
		final int a, b, w, d;
		final int minMembers, maxMembers, treasuryW, treasuryD, meditationW, meditationD, mainW, pagoda, tiers, gardenW, gardenD, trees, bamboo;

		Size(int reach, int minMembers, int maxMembers, int treasuryW, int treasuryD, int meditationW, int meditationD, int mainW, int pagoda,
				int tiers, int gardenW, int gardenD, int trees, int bamboo) {
			this.reach = reach;
			this.a = reach + GATE_BEYOND + 6;
			this.b = reach + GATE_BEYOND + 6;
			this.w = 2 * a + 1;
			this.d = 2 * b + 1;
			this.minMembers = minMembers;
			this.maxMembers = maxMembers;
			this.treasuryW = treasuryW;
			this.treasuryD = treasuryD;
			this.meditationW = meditationW;
			this.meditationD = meditationD;
			this.mainW = mainW;
			this.pagoda = pagoda;
			this.tiers = tiers;
			this.gardenW = gardenW;
			this.gardenD = gardenD;
			this.trees = trees;
			this.bamboo = bamboo;
		}

		/** The next size down, or null below the smallest. */
		Size smaller() {
			return ordinal() == 0 ? null : values()[ordinal() - 1];
		}
	}

	static final int MAIN_DEPTH = 21;
	/** How far out a garden may reach (SectLayout#outline): it is low and fenced, so it may come nearer the edge than a house. */
	private static final double GARDEN_LIMIT = 0.97;
	/** How far beyond the buildings' reach the gates stand: well outside the barrier, which closes 2 past the buildings. */
	static final int GATE_BEYOND = 8;
	/** The lawn between the way up the middle and the halls of the front court, and between the main hall and its neighbours. */
	static final int SET_BACK = 8;

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
		Size rolled = roll < 0.35f ? Size.SMALL : roll < 0.75f ? Size.MEDIUM : Size.LARGE;
		Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		if (!isUpperRealm(context, new BlockPos(x, 64, z))) {
			Frame frame = new Frame(facing, x, z, rolled.w, rolled.d);
			int platformY = groundLevel(context, rolled, frame, false);
			return platformY == NO_SITE ? Optional.empty() : Optional.of(stub(random, rolled, frame, platformY, false));
		}
		// The Upper Realm is mostly open sky, and few islands are wide enough: the sect stands wherever near the middle of its
		// cell an island will hold it (see ISLAND_SEARCH), and ground too small for the size rolled takes a smaller sect. A
		// demon clan's cell has none (DenStructure#clanCell): the den stands there.
		if (DenStructure.clanCell(context.seed(), chunk)) return Optional.empty();
		for (Size size = rolled; size != null; size = size.smaller()) {
			for (Frame frame : islandSpots(context, size, facing, x, z)) {
				int platformY = groundLevel(context, size, frame, true);
				if (platformY != NO_SITE) return Optional.of(stub(random, size, frame, platformY, true));
			}
		}
		return Optional.empty();
	}

	/** The sect's start: at its plaza (where its core will stand), which SectSites wakes to found it. */
	private static GenerationStub stub(WorldgenRandom random, Size size, Frame frame, int platformY, boolean upper) {
		return new GenerationStub(new BlockPos(frame.cx(), platformY, frame.cz()), builder -> plan(builder, random, size, frame, platformY, upper));
	}

	static final int NO_SITE = Integer.MIN_VALUE;
	/**
	 * How far from its start chunk's middle an Upper Realm sect may stand, in steps of {@link #ISLAND_STEP}. The Upper Realm's
	 * sects are spread one to a 256-block cell round each start chunk (structure set sects_upper_realm: spacing 16, separation
	 * 15, so a start chunk every 16 chunks exactly): 48 plus the largest grounds' half-width (76) is 124, so a sect's grounds
	 * never leave its cell and two never overlap, and all of it stays within the 8 chunks of its start that worldgen places
	 * structure pieces in. Surveyed 2026-10-12 over 195 cells within 2,048 blocks of the rift on two seeds: 21 and 15 sects,
	 * against 1 and 6 when a sect could only stand at its start chunk's middle; about 85 ms a site on average (on the
	 * worldgen threads), against ~12.
	 */
	static final int ISLAND_SEARCH = 48;
	private static final int ISLAND_STEP = 16;
	/** Below this island mask (ModNoiseSettings#islandMask) a sample point is surely over the open sky. */
	private static final double ISLAND_MIN = -0.15;
	/** How many of the most promising spots, per size, the generator is asked about (3 or 4 found barely any more). */
	private static final int ISLAND_TRIES = 2;

	/**
	 * Where a sect of {@code size} might stand round x, z: the spots within {@link #ISLAND_SEARCH} where every point the
	 * ground is sampled at (see groundLevel) is over an island by the island masks, deepest inside first; at most
	 * {@link #ISLAND_TRIES}. The masks cost next to nothing, while the generator takes ~10 ms to find each point's surface
	 * (mostly sky), so most sites, which are open sky, are passed over at once.
	 */
	static List<Frame> islandSpots(GenerationContext context, Size size, Direction facing, int x, int z) {
		List<Frame> spots = new ArrayList<>();
		List<Double> depths = new ArrayList<>();
		for (int dx = -ISLAND_SEARCH; dx <= ISLAND_SEARCH; dx += ISLAND_STEP) {
			for (int dz = -ISLAND_SEARCH; dz <= ISLAND_SEARCH; dz += ISLAND_STEP) {
				Frame frame = new Frame(facing, x + dx, z + dz, size.w, size.d);
				double depth = Double.MAX_VALUE;
				for (BlockPos at : samplePoints(size, frame)) {
					depth = Math.min(depth, ModNoiseSettings.islandMask(context.randomState(), at.getX(), at.getZ()));
					if (depth < ISLAND_MIN) break;
				}
				if (depth < ISLAND_MIN) continue;
				int i = 0;
				while (i < depths.size() && depths.get(i) >= depth) i++;
				if (i >= ISLAND_TRIES) continue;
				spots.add(i, frame);
				depths.add(i, depth);
				if (spots.size() > ISLAND_TRIES) {
					spots.remove(ISLAND_TRIES);
					depths.remove(ISLAND_TRIES);
				}
			}
		}
		return spots;
	}

	/**
	 * The columns the ground is sampled at for a sect of {@code size} laid out as {@code frame}: 25 where the buildings will
	 * stand (a grid reaching three quarters of their reach from the plaza), then 8 at the gates (front and sides), the back
	 * and the four far corners, just beyond the gates' reach.
	 */
	private static BlockPos[] samplePoints(Size size, Frame frame) {
		BlockPos[] points = new BlockPos[33];
		int n = 0;
		for (int i = 0; i <= 4; i++) {
			for (int j = 0; j <= 4; j++) {
				points[n++] = frame.world(size.a + Math.round(size.reach * 0.375f * (i - 2)), size.b + Math.round(size.reach * 0.375f * (j - 2)));
			}
		}
		int outer = size.reach + GATE_BEYOND + 4;
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k, out = k % 2 == 0 ? outer : outer * 0.75;
			points[n++] = frame.world(size.a + (int) Math.round(Math.cos(angle) * out), size.b + (int) Math.round(Math.sin(angle) * out));
		}
		return points;
	}

	/**
	 * The level of the grounds of a sect of {@code size} laid out as {@code frame}: where its buildings stand, if the land
	 * will take it, else {@link #NO_SITE}. The ground is sampled where the buildings will stand, and at the gates and the far
	 * edges of the grounds (samplePoints): those are levelled too, so a hill there would be cut into a cliff across the way
	 * out of a gate. Too uneven, too wet or (in the Upper Realm) over the edge of an island, and there's no sect here. The
	 * platform's level is the buildings' ground's.
	 */
	static int groundLevel(GenerationContext context, Size size, Frame frame, boolean upper) {
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		RandomState state = context.randomState();
		BlockPos[] points = samplePoints(size, frame);
		int[] samples = new int[points.length];
		int water = 0;
		for (int n = 0; n < 25; n++) {
			BlockPos at = points[n];
			int y = generator.getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, state);
			if (y <= heights.getMinBuildHeight() + 2) return NO_SITE;
			samples[n] = y;
			if (n % 2 == 0) { // (i + j) even on the 5 x 5 grid
				NoiseColumn column = generator.getBaseColumn(at.getX(), at.getZ(), heights, state);
				BlockState top = column.getBlock(y);
				if (!top.getFluidState().isEmpty()) water++;
			}
		}
		int[] inner = Arrays.copyOf(samples, 25);
		Arrays.sort(inner);
		int platformY = inner[12];
		for (int n = 25; n < points.length; n++) {
			BlockPos at = points[n];
			int y = generator.getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, state);
			if (y <= heights.getMinBuildHeight() + 2 || y > platformY + MAX_EDGE_RISE) return NO_SITE;
			samples[n] = y;
		}
		int[] sorted = samples.clone();
		Arrays.sort(sorted);
		if (sorted[sorted.length - 1] - sorted[0] > (upper ? MAX_RELIEF_UPPER : MAX_RELIEF) || water > MAX_WET) return NO_SITE;
		if (platformY + 70 > heights.getMaxBuildHeight()) return NO_SITE;
		return platformY;
	}

	/** The largest grounds' half-width, from the plaza to the edge (for SectGameTests). */
	static int largestHalfWidth() {
		return Size.LARGE.a;
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
	 * Whether worldgen working within {@code box} would cut into one of the mod's buildings: a sect's halls, houses, gates and the
	 * rest (not its open grounds, under which ore lies as anywhere), or a ghost palace (see OreFeatureMixin). Only starts this
	 * generating region can reach are seen (a feature's region reaches eight chunks round it, past any of these buildings).
	 */
	public static boolean cutsIntoBuildings(WorldGenLevel level, BoundingBox box) {
		for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
			for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
				if (chunk == null) continue;
				for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
					Structure structure = entry.getKey();
					if (!(structure instanceof SectStructure) && !(structure instanceof GhostPalaceStructure)) continue;
					for (long start : entry.getValue()) {
						int sx = ChunkPos.getX(start), sz = ChunkPos.getZ(start);
						if (!level.hasChunk(sx, sz)) continue;
						ChunkAccess origin = level.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS, false);
						StructureStart found = origin == null ? null : origin.getStartForStructure(structure);
						if (found == null || !found.isValid() || !found.getBoundingBox().intersects(box)) continue;
						for (StructurePiece piece : found.getPieces()) {
							if (piece instanceof SectPiece sect && sect.kind() == SectPiece.Kind.TERRACE) continue;
							if (piece.getBoundingBox().intersects(box)) return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Whether {@code pos} lies on a sect's open grounds or within {@code margin} of them, where worldgen grows no trees, bamboo,
	 * huge mushrooms or boulders (see SectClearingMixin): a sect keeps its grounds clear and plants its own. Only sects whose
	 * start this generating region can reach are seen; a sect whose start lies beyond it counts as near, to be safe.
	 */
	public static boolean onGrounds(WorldGenLevel level, BlockPos pos, int margin) {
		for (int cx = pos.getX() - margin >> 4; cx <= pos.getX() + margin >> 4; cx++) {
			for (int cz = pos.getZ() - margin >> 4; cz <= pos.getZ() + margin >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
				if (chunk == null) continue;
				for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
					if (!(entry.getKey() instanceof SectStructure)) continue;
					for (long start : entry.getValue()) {
						int sx = ChunkPos.getX(start), sz = ChunkPos.getZ(start);
						if (!level.hasChunk(sx, sz)) return true;
						ChunkAccess origin = level.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS, false);
						StructureStart sect = origin == null ? null : origin.getStartForStructure(entry.getKey());
						if (sect == null || !sect.isValid()) continue;
						for (StructurePiece piece : sect.getPieces()) {
							if (piece instanceof SectPiece grounds && grounds.onGrounds(pos, margin)) return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * An Upper Realm biome (tagged {@link ModTags#IS_UPPER_REALM}): the sect is laid out the Upper Realm's way, on an island. The
	 * Netherworld's biomes are the mod's too, but no sect stands there (its biomes have no has_structure/sect tag).
	 */
	static boolean isUpperRealm(GenerationContext context, BlockPos pos) {
		Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()),
				QuartPos.fromBlock(pos.getZ()), context.randomState().sampler());
		return isUpperRealmBiome(biome);
	}

	/** Whether a sect in this biome is laid out the Upper Realm's way. */
	public static boolean isUpperRealmBiome(Holder<Biome> biome) {
		return biome.is(ModTags.IS_UPPER_REALM);
	}

	/**
	 * The compound's frame: u from left to right across its front, v from the gate inward, as in SectPiece, laid in the world
	 * by {@code facing} (the way it extends from its gate).
	 */
	record Frame(Direction facing, int cx, int cz, int w, int d) {
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

		/** The world direction of a building that extends {@code side} from its door. */
		Direction toWorld(SectLayout.Side side) {
			return switch (side) {
				case BACK -> facing;
				case FRONT -> facing.getOpposite();
				case RIGHT -> right();
				case LEFT -> right().getOpposite();
			};
		}

		/** The world box of compound rectangle u0..u1, v0..v1, from y0 up {@code height}. */
		BoundingBox box(int u0, int v0, int u1, int v1, int y0, int height) {
			BlockPos a = world(u0, v0), b = world(u1, v1);
			return new BoundingBox(Math.min(a.getX(), b.getX()), y0, Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()), y0 + height - 1,
					Math.max(a.getZ(), b.getZ()));
		}
	}

	private static SectPiece piece(SectPiece.Kind kind, SectLayout.Lot lot, Frame frame, int y, int height, long seed, Alignment.Faction faction,
			boolean upper, CompoundTag extra) {
		return new SectPiece(kind, frame.box(lot.u0(), lot.v0(), lot.u1(), lot.v1(), y, height), frame.toWorld(lot.side()), seed, faction, upper, extra);
	}

	/** A plot {@code width} across and {@code depth} deep beside the main hall, {@code beside} out from the middle on {@code side} (+1 right). */
	private static SectLayout.Lot besideHall(int cu, int side, int beside, int width, int v0, int depth, SectLayout.Side facing) {
		int u0 = side > 0 ? cu + beside : cu - beside - width + 1;
		return new SectLayout.Lot(u0, v0, u0 + width - 1, v0 + depth - 1, facing);
	}

	/**
	 * A hall {@code width} across its front and {@code depth} deep, facing the way up the middle from {@code side} of it across
	 * a lawn of {@link #SET_BACK}, before the plaza.
	 */
	private static SectLayout.Lot beforePlaza(int cu, int cv, int side, int width, int depth) {
		int v1 = cv - SectPiece.PLAZA_RADIUS - 3 - SET_BACK, v0 = v1 - width + 1;
		int near = 4 + SET_BACK;
		return side > 0 ? new SectLayout.Lot(cu + near, v0, cu + near - 1 + depth, v1, SectLayout.Side.RIGHT)
				: new SectLayout.Lot(cu - near + 1 - depth, v0, cu - near, v1, SectLayout.Side.LEFT);
	}

	/** Lays out a sect of size {@code size} (0 small, 1 medium, 2 large) at the origin as worldgen would, for the game tests. */
	public static List<StructurePiece> layOut(long seed, int size, Direction facing, boolean upper) {
		Size s = Size.values()[size];
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		plan(builder, new WorldgenRandom(new LegacyRandomSource(seed)), s, new Frame(facing, 0, 0, s.w, s.d), 64, upper);
		return builder.build().pieces();
	}

	/** The most members a sect of size {@code size} may have (see {@link #layOut}). */
	public static int maxMembers(int size) {
		return Size.values()[size].maxMembers;
	}

	private static void plan(StructurePiecesBuilder builder, WorldgenRandom random, Size size, Frame frame, int y, boolean upper) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random);
		Alignment.Faction faction = Alignment.factionOf(alignment);
		int w = size.w, d = size.d, cu = size.a, cv = size.b, r = SectPiece.PLAZA_RADIUS;
		Direction back = frame.facing();
		int capacity = size.minMembers + random.nextInt(size.maxMembers - size.minMembers + 1);
		SectLayout layout = new SectLayout(size.a, size.b, size.reach, seed);
		int gateDistance = size.reach + GATE_BEYOND;
		List<SectPiece> pieces = new ArrayList<>();

		// The ways: up the middle from the gate to the plaza and on, wider, to the main hall's steps; and out to either side.
		int mainV0 = cv + r + 2;
		layout.road(cu, 0, cu, cv, 1);
		layout.road(cu, cv, cu, mainV0 + 2, 3);
		layout.road(0, cv, w - 1, cv, 1);
		layout.plaza(r);

		// The main hall behind the plaza; the pagoda beside it, a garden behind the pagoda and another on the hall's far side.
		int side = random.nextBoolean() ? 1 : -1;
		SectLayout.Lot mainLot = new SectLayout.Lot(cu - size.mainW / 2, mainV0, cu - size.mainW / 2 + size.mainW - 1, mainV0 + MAIN_DEPTH - 1,
				SectLayout.Side.BACK);
		layout.claim(mainLot, 2);
		SectPiece main = piece(SectPiece.Kind.MAIN_HALL, mainLot, frame, y, 34, seed + 30, faction, upper, null);
		pieces.add(main);
		int beside = size.mainW / 2 + 3 + SET_BACK;
		SectLayout.Lot pagodaLot = besideHall(cu, side, beside, size.pagoda, mainV0 + 1, size.pagoda, SectLayout.Side.BACK);
		layout.claim(pagodaLot, 2);
		layout.apron(pagodaLot, 2, 1);
		layout.footpath(pagodaLot.doorstep()[0], pagodaLot.doorstep()[1]);
		CompoundTag pagodaData = new CompoundTag();
		pagodaData.putInt("Tiers", size.tiers);
		SectPiece pagoda = piece(SectPiece.Kind.PAGODA, pagodaLot, frame, y, 6 * size.tiers + 14, seed + 31, faction, upper, pagodaData);
		pieces.add(pagoda);
		List<SectPiece> gardens = new ArrayList<>();
		for (int depth = size.gardenD; depth >= 9; depth--) {
			SectLayout.Lot lot = besideHall(cu, -side, beside, size.gardenW, mainV0 + 1, depth, SectLayout.Side.BACK);
			if (!layout.within(lot, GARDEN_LIMIT)) continue;
			layout.claim(lot, 2);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			gardens.add(piece(SectPiece.Kind.GARDEN, lot, frame, y, 10, seed + 40, faction, upper, null));
			break;
		}
		// Behind the pagoda the second garden opens toward the main hall, its plots running outward.
		for (int across = 10; across >= 8; across--) {
			int v0 = pagodaLot.v1() + 5;
			SectLayout.Lot lot = new SectLayout.Lot(pagodaLot.u0(), v0, pagodaLot.u1(), v0 + across - 1, side > 0 ? SectLayout.Side.RIGHT : SectLayout.Side.LEFT);
			if (!layout.within(lot, GARDEN_LIMIT)) continue;
			layout.claim(lot, 2);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			gardens.add(piece(SectPiece.Kind.GARDEN, lot, frame, y, 10, seed + 41, faction, upper, null));
			break;
		}
		pieces.addAll(gardens);

		// The two halls of the front court face the way up the middle, the treasury on one side and the meditation hall on the
		// other.
		int halls = random.nextBoolean() ? 1 : -1;
		SectLayout.Lot treasuryLot = beforePlaza(cu, cv, halls, size.treasuryW, size.treasuryD);
		SectLayout.Lot meditationLot = beforePlaza(cu, cv, -halls, size.meditationW, size.meditationD);
		for (SectLayout.Lot lot : new SectLayout.Lot[] {treasuryLot, meditationLot}) {
			layout.claim(lot, 2);
			layout.apron(lot, 1, 1);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
		}
		SectPiece treasury = piece(SectPiece.Kind.TREASURY, treasuryLot, frame, y, 18, seed + 20, faction, upper, null);
		SectPiece meditation = piece(SectPiece.Kind.MEDITATION_HALL, meditationLot, frame, y, 18, seed + 21, faction, upper, null);
		pieces.add(treasury);
		pieces.add(meditation);

		// The gate across the way up the middle, and a torii across each side way, all outside the barrier (it closes round the
		// buildings, inside gateDistance), where the gate guards stand and visitors are met.
		SectLayout.Lot gateLot = new SectLayout.Lot(cu - 9, cv - gateDistance - 1, cu + 9, cv - gateDistance + 1, SectLayout.Side.BACK);
		layout.keepClear(gateLot.u0(), gateLot.v0() - 1, gateLot.u1(), gateLot.v1() + 1);
		SectPiece gate = piece(SectPiece.Kind.GATE, gateLot, frame, y, 14, seed + 1, faction, upper, null);
		pieces.add(gate);
		List<SectPiece> gates = new ArrayList<>(List.of(gate));
		for (int k = -1; k <= 1; k += 2) {
			int u = cu + k * gateDistance;
			SectLayout.Lot lot = new SectLayout.Lot(u - 1, cv - 5, u + 1, cv + 5, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.keepClear(lot.u0() - 1, lot.v0(), lot.u1() + 1, lot.v1());
			SectPiece torii = piece(SectPiece.Kind.TORII, lot, frame, y, 10, seed + 2 + k, faction, upper, null);
			pieces.add(torii);
			gates.add(torii);
		}

		// The Altar of Heaven, out on the lawns (placed once the gates have their ground kept clear): round terraces (one, two or three, by the sect's size), where members at Qi
		// Refining Mid's gate perform the Ritual to the Heavens (see NpcLife).
		// (With no room for its full size, a terrace fewer.)
		int tiers = size.ordinal() + 1;
		SectLayout.Lot altarLot = null;
		for (; tiers >= 1 && altarLot == null; tiers--) altarLot = layout.place(random, SectPiece.altarSize(tiers), SectPiece.altarSize(tiers), size.reach / 2 + 6, 3);
		tiers++;
		SectPiece altar = null;
		if (altarLot != null) {
			CompoundTag altarData = new CompoundTag();
			altarData.putInt("Tiers", tiers);
			altar = piece(SectPiece.Kind.ALTAR, altarLot, frame, y, 2 * tiers + 7, seed + 60, faction, upper, altarData);
			pieces.add(altar);
		}

		// The training ground, out on the lawns too: a sparring ring and dummies, where the disciples practise (NpcLife).
		SectLayout.Lot trainingLot = layout.place(random, SectPiece.TRAINING_W, SectPiece.TRAINING_D, size.reach / 2 + 2, 3);
		SectPiece training = null;
		if (trainingLot != null) {
			training = piece(SectPiece.Kind.TRAINING, trainingLot, frame, y, 9, seed + 70, faction, upper, null);
			pieces.add(training);
		}
		// The elders' houses, nearest the plaza (the disciples live in the meditation hall); then lanterns along the ways and
		// trees and bamboo on the grass between.
		EnumMap<NpcTitle, Integer> titles = Sect.titleCounts(size.maxMembers);
		int elders = titles.get(NpcTitle.GRAND_ELDER) + titles.get(NpcTitle.ELDER);
		List<SectLayout.Lot> lots = layout.houses(random, elders, elders);
		List<SectPiece> dwellings = new ArrayList<>();
		for (int i = 0; i < lots.size(); i++) {
			dwellings.add(piece(SectPiece.Kind.ELDER_HOUSE, lots.get(i), frame, y, 18, seed + 100 + i, faction, upper, null));
		}
		pieces.addAll(dwellings);
		for (int v = cv - gateDistance + 4; v < cv - r; v += 5) {
			layout.lantern(cu - 3, v);
			layout.lantern(cu + 3, v);
		}
		for (int u = cu - gateDistance + 4; u < cu - r; u += 5) {
			layout.lantern(u, cv - 3);
			layout.lantern(u, cv + 3);
			layout.lantern(w - 1 - u, cv - 3);
			layout.lantern(w - 1 - u, cv + 3);
		}
		layout.plant(random, size.trees, size.bamboo);

		// The grounds go first, under everything.
		CompoundTag groundsData = new CompoundTag();
		groundsData.putInt("A", size.a);
		groundsData.putInt("B", size.b);
		groundsData.putByteArray("Map", layout.map);
		SectPiece terrace = new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, w - 1, d - 1, y, 24), back, seed, faction, upper, groundsData);
		pieces.add(0, terrace);

		// The plaza last, so its blueprint can name every post of the grounds.
		BoundingBox plazaBox = frame.box(cu - r, cv - r, cu + r, cv + r, y, 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, faction, upper, plazaData);
		BlockPos core = plaza.toWorld(r, 2, r);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = main.masterSeat();
		posts.get(Sect.Post.MASTER_SEAT).add(main.toWorld(seat[0], seat[1], seat[2]));
		for (int[] mat : meditation.meditationMats()) posts.get(Sect.Post.MEDITATION).add(meditation.toWorld(mat[0], 2, mat[1]));
		for (int[] guard : treasury.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(treasury.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Homes: the elders' houses, nearest the plaza first, then the mats of the meditation hall, where the disciples live.
		// The strongest members are founded first (SectManager#found), so the elders get the houses.
		dwellings.sort((x, z) -> Double.compare(x.getBoundingBox().getCenter().distSqr(core), z.getBoundingBox().getCenter().distSqr(core)));
		for (SectPiece house : dwellings) {
			int[] mat = house.homeMat();
			posts.get(Sect.Post.DWELLING).add(house.toWorld(mat[0], mat[1], mat[2]));
		}
		for (int[] mat : meditation.meditationMats()) posts.get(Sect.Post.DWELLING).add(meditation.toWorld(mat[0], 2, mat[1]));
		// Where the inner disciples stroll: along the ways inside the barrier, round the plaza, by the gardens, the pagoda's top.
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		int along = (r + size.reach) / 2;
		courtyard.add(terrace.toWorld(cu, 1, cv - along));
		courtyard.add(terrace.toWorld(cu, 1, cv - size.reach + 4));
		courtyard.add(terrace.toWorld(cu - along, 1, cv));
		courtyard.add(terrace.toWorld(cu + along, 1, cv));
		for (int k = 0; k < 4; k++) {
			double angle = Math.PI / 2 * k;
			courtyard.add(plaza.toWorld(r + (int) Math.round(Math.cos(angle) * 10.5), 1, r + (int) Math.round(Math.sin(angle) * 10.5)));
		}
		for (SectPiece garden : gardens) courtyard.add(garden.toWorld(garden.width() / 2, 1, 1));
		int[] top = pagoda.pagodaSeat();
		courtyard.add(pagoda.toWorld(top[0], top[1], top[2]));
		int[] in = gate.gateInside();
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));
		if (altar != null) {
			for (int[] post : altar.altarPosts()) posts.get(Sect.Post.ALTAR).add(altar.toWorld(post[0], post[1], post[2]));
		}
		if (training != null) {
			for (int[] post : training.trainingPosts()) posts.get(Sect.Post.TRAINING).add(training.toWorld(post[0], post[1], post[2]));
		}
		// Points along the paved ways every ten blocks, out to the gates, for walkers that can't go straight (Sect#waypoints).
		List<BlockPos> ways = posts.get(Sect.Post.WAY);
		for (int v = cv - gateDistance + 2; v <= mainV0; v += 10) if (Math.abs(v - cv) > r) ways.add(terrace.toWorld(cu, 1, v));
		for (int u = cu - gateDistance + 2; u <= cu + gateDistance - 2; u += 10) if (Math.abs(u - cu) > r) ways.add(terrace.toWorld(u, 1, cv));
		// The gate guards, on the way just outside each gate: two at the gate, then one at each torii, then a second at each
		// (Sect#guardPost hands them to the outer disciples in this order).
		List<BlockPos> guards = posts.get(Sect.Post.GATE_GUARD);
		int out = gateDistance + 2;
		guards.add(terrace.toWorld(cu - 2, 1, cv - out));
		guards.add(terrace.toWorld(cu + 2, 1, cv - out));
		guards.add(terrace.toWorld(cu - out, 1, cv - 2));
		guards.add(terrace.toWorld(cu + out, 1, cv - 2));
		guards.add(terrace.toWorld(cu - out, 1, cv + 2));
		guards.add(terrace.toWorld(cu + out, 1, cv + 2));

		// The barrier: from the core out round every building, and a little more; the gates stay outside it.
		int radius = 0;
		for (SectPiece piece : pieces) {
			BoundingBox box = piece.getBoundingBox();
			if (piece == terrace || gates.contains(piece)) continue;
			for (int[] corner : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
				double dx = corner[0] - core.getX(), dz = corner[1] - core.getZ();
				radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
			}
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
