package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Immortal Realm's other cities (phase 2; the user's word, 2026-10-25: "other immortal cities (without the court) around the realm"):
 * walled market towns on the realm's massifs, away from the main island, laid on the land as it lies as the Jade Capital is (CityPlanner:
 * each building at its own ground's level, the land shaped round it by vanilla's Beardifier, this structure being {@code beard_thin}, the
 * ways laid on the ground where it is). Each is 145 across:
 * <pre>
 *   back           [ the city lord's hall ]
 *       [pagoda]                |                [alchemy hall]
 *   left gate === the cross street ===( the market square )=== right gate
 *     [treasury]         shops  |  shops          [arena]
 *   front          [   the front gate   ]
 * </pre>
 * One to a cell of the realm where the land holds one (ModNoiseSettings#citySite: well inside a massif, land all round it), its ground rolled
 * gently for it by the terrain (no pinnacle or mountain in it), on the Jade Peaks and the Immortal Gardens ({@code #has_structure/immortal_city});
 * its structure set ({@code worldgen/structure_set/immortal_cities.json}) tries every other chunk, so each site's is tried.
 */
public class ImmortalCityStructure extends Structure {
	/** The city's half-width and half-depth (145 by 145), its walls' thickness. */
	static final int A = 72, B = 72, W = 2 * A + 1, D = 2 * B + 1, WALL = 5;

	public ImmortalCityStructure(StructureSettings settings) {
		super(settings);
	}

	/**
	 * A city stands at its cell's site (ModNoiseSettings#citySite: the terrain rolled its ground gently for it), from the even chunk the site
	 * lies in (the structure set tries every other chunk, so every site's has its try; none other builds it).
	 */
	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		RandomState state = context.randomState();
		int cx = Math.floorDiv(chunk.getMiddleBlockX(), ModNoiseSettings.CITY_CELL), cz = Math.floorDiv(chunk.getMiddleBlockZ(), ModNoiseSettings.CITY_CELL);
		ModNoiseSettings.CitySite site = ModNoiseSettings.citySite(state, cx, cz);
		if (site == null || (site.x() >> 4 & ~1) != chunk.x || (site.z() >> 4 & ~1) != chunk.z) return Optional.empty();
		CityPlanner.Ground ground = CityPlanner.ground(context.chunkGenerator(), context.heightAccessor(), state);
		int y = ground.at(site.x(), site.z());
		if (y <= ModDimensions.IMMORTAL_CLOUD_Y) return Optional.empty();
		WorldgenRandom random = context.random();
		Direction facing = Direction.from2DDataValue(Math.floorMod(cx * 31 + cz * 17, 4));
		SectStructure.Frame frame = new SectStructure.Frame(facing, site.x(), site.z(), W, D);
		return Optional.of(new GenerationStub(new BlockPos(site.x(), y, site.z()), builder -> plan(builder, random, frame, ground)));
	}

	/**
	 * Whether {@code pos} lies within an immortal city's walls or within {@code margin} of them, where worldgen grows no trees, bamboo or the
	 * like (SectClearingMixin, as on a sect's grounds): the city plants its own. Only cities whose start this generating region can reach are
	 * seen.
	 */
	public static boolean onGrounds(WorldGenLevel level, BlockPos pos, int margin) {
		for (int cx = pos.getX() - margin >> 4; cx <= pos.getX() + margin >> 4; cx++) {
			for (int cz = pos.getZ() - margin >> 4; cz <= pos.getZ() + margin >> 4; cz++) {
				if (!level.hasChunk(cx, cz)) continue;
				ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
				if (chunk == null) continue;
				for (Map.Entry<Structure, LongSet> entry : chunk.getAllReferences().entrySet()) {
					if (!(entry.getKey() instanceof ImmortalCityStructure)) continue;
					for (long start : entry.getValue()) {
						int sx = ChunkPos.getX(start), sz = ChunkPos.getZ(start);
						if (!level.hasChunk(sx, sz)) return true;
						ChunkAccess origin = level.getChunk(sx, sz, ChunkStatus.STRUCTURE_STARTS, false);
						StructureStart city = origin == null ? null : origin.getStartForStructure(entry.getKey());
						if (city == null || !city.isValid()) continue;
						BoundingBox box = city.getBoundingBox();
						if (pos.getX() >= box.minX() - margin && pos.getX() <= box.maxX() + margin && pos.getZ() >= box.minZ() - margin
								&& pos.getZ() <= box.maxZ() + margin) return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * The level of a city's market square about its middle (x, z): the median of the land's height at nine columns eight apart round it
	 * (the same whichever way the city faces), the land evened to it. Its markets' merchants are found by it (CityMarkets).
	 */
	static int squareY(CityPlanner.Ground ground, int x, int z) {
		int[] heights = new int[9];
		int n = 0;
		for (int dx = -8; dx <= 8; dx += 8) {
			for (int dz = -8; dz <= 8; dz += 8) heights[n++] = ground.at(x + dx, z + dz);
		}
		Arrays.sort(heights);
		return heights[4];
	}

	/** Lays a city out in {@code frame}, each of its buildings on its own ground. */
	static void plan(StructurePiecesBuilder builder, WorldgenRandom random, SectStructure.Frame frame, CityPlanner.Ground ground) {
		long seed = random.nextLong();
		SectLayout layout = new SectLayout(A, B, A + 40, seed);
		Arrays.fill(layout.map, SectLayout.GRASS);
		CityPlanner city = new CityPlanner(frame, layout, ground, seed, Alignment.Faction.NEUTRAL, null);
		int cu = A, cv = B;

		// The walls: corner bastions, gates at the front and either side (the back wall whole, the city lord's hall before it).
		int tower = 13, gateW = 25, gateD = 11;
		for (int[] c : new int[][] {{0, 0}, {W - tower, 0}, {0, D - tower}, {W - tower, D - tower}}) {
			SectLayout.Lot lot = new SectLayout.Lot(c[0], c[1], c[0] + tower - 1, c[1] + tower - 1, SectLayout.Side.BACK);
			layout.claim(lot, 1);
			city.add(SectPiece.Kind.CITY_TOWER, lot, CityPieces.WALL_HEIGHT + 16, c[0] * 7L + c[1], null);
		}
		SectLayout.Lot front = new SectLayout.Lot(cu - gateW / 2, 0, cu + gateW / 2, gateD - 1, SectLayout.Side.BACK);
		SectLayout.Lot left = new SectLayout.Lot(0, cv - gateW / 2, gateD - 1, cv + gateW / 2, SectLayout.Side.RIGHT);
		SectLayout.Lot right = new SectLayout.Lot(W - gateD, cv - gateW / 2, W - 1, cv + gateW / 2, SectLayout.Side.LEFT);
		for (SectLayout.Lot gate : List.of(front, left, right)) {
			layout.claim(gate, 0);
			city.add(SectPiece.Kind.CITY_GATE, gate, CityPieces.WALL_HEIGHT + 26, gate.u0() * 31L + gate.v0(), null);
		}
		int wallHeight = CityPieces.WALL_HEIGHT + 8;
		city.wall(new SectLayout.Lot(tower, 0, front.u0() - 1, WALL - 1, SectLayout.Side.BACK), wallHeight);
		city.wall(new SectLayout.Lot(front.u1() + 1, 0, W - tower - 1, WALL - 1, SectLayout.Side.BACK), wallHeight);
		city.wall(new SectLayout.Lot(tower, D - WALL, W - tower - 1, D - 1, SectLayout.Side.FRONT), wallHeight);
		city.wall(new SectLayout.Lot(0, tower, WALL - 1, left.v0() - 1, SectLayout.Side.RIGHT), wallHeight);
		city.wall(new SectLayout.Lot(0, left.v1() + 1, WALL - 1, D - tower - 1, SectLayout.Side.RIGHT), wallHeight);
		city.wall(new SectLayout.Lot(W - WALL, tower, W - 1, right.v0() - 1, SectLayout.Side.LEFT), wallHeight);
		city.wall(new SectLayout.Lot(W - WALL, right.v1() + 1, W - 1, D - tower - 1, SectLayout.Side.LEFT), wallHeight);

		// The ways: the avenue from the front gate to the city lord's hall, the cross street from gate to gate, the market square where they
		// meet, a way round inside the walls.
		int hallV0 = cv + 32;
		layout.road(cu, 0, cu, hallV0 - 2, 5);
		layout.road(0, cv, W - 1, cv, 4);
		layout.plaza(16);
		layout.road(WALL + 2, WALL + 6, W - WALL - 3, WALL + 6, 2);
		layout.road(WALL + 2, D - WALL - 7, W - WALL - 3, D - WALL - 7, 2);
		layout.road(WALL + 6, WALL + 2, WALL + 6, D - WALL - 3, 2);
		layout.road(W - WALL - 7, WALL + 2, W - WALL - 7, D - WALL - 3, 2);

		// The market square, the land evened under it, its stalls in a ring facing in.
		int squareY = squareY(ground, frame.cx(), frame.cz());
		city.beards.add(frame.box(cu - 17, cv - 17, cu + 17, cv + 17, squareY, 4));
		for (int k = 0; k < 8; k++) {
			double angle = Math.toRadians(22.5 + 45 * k);
			int u = cu + (int) Math.round(Math.cos(angle) * 11), v = cv + (int) Math.round(Math.sin(angle) * 11);
			int du = u - cu, dv = v - cv;
			SectLayout.Side side = Math.abs(du) >= Math.abs(dv) ? (du < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT)
					: (dv < 0 ? SectLayout.Side.FRONT : SectLayout.Side.BACK);
			SectLayout.Lot lot = CityPlanner.lot(u, v, 5, 5, side);
			layout.claim(lot, 0);
			city.pieces.add(SectStructure.piece(SectPiece.Kind.CITY_STALL, lot, frame, squareY, 7, seed + 500 + k, Alignment.Faction.NEUTRAL, true, null));
		}

		// The city lord's hall at the avenue's head, facing the square.
		SectLayout.Lot hall = new SectLayout.Lot(cu - 15, hallV0, cu + 15, hallV0 + 20, SectLayout.Side.BACK);
		layout.claim(hall, 2);
		city.add(SectPiece.Kind.MEDITATION_HALL, hall, 20, 50, null);

		// Shops along the avenue and the cross street, their fronts on the way, a stone lantern between each and the next.
		for (int v = 21; v + 5 < cv - 18; v += 13) {
			for (int k = -1; k <= 1; k += 2) {
				city.shop(cu + k * 11, v, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.lantern(cu + k * 7, v + 7);
			}
		}
		for (int u = 21; u < W - 18; u += 13) {
			if (Math.abs(u - cu) <= 22) continue;
			for (int k = -1; k <= 1; k += 2) {
				city.shop(u, cv + k * 10, k < 0 ? SectLayout.Side.FRONT : SectLayout.Side.BACK);
				layout.lantern(u + 7, cv + k * 6);
			}
		}

		// Its great buildings: the treasury and the arena in the front quarters, a seven-storey pagoda and the alchemy hall behind.
		city.place(SectPiece.Kind.TREASURY, 30, 36, 25, 21, SectLayout.Side.LEFT, 20, null, 3);
		city.place(SectPiece.Kind.TRAINING, W - 1 - 30, 36, SectPiece.TRAINING_W, SectPiece.TRAINING_D, SectLayout.Side.RIGHT, 9, null, 4);
		city.place(SectPiece.Kind.PAGODA, 34, 104, 17, 17, SectLayout.Side.LEFT, 6 * 7 + 14, CityPlanner.tiers(7), 5);
		city.place(SectPiece.Kind.HOLY_ALCHEMY, W - 1 - 34, 106, 25, 21, SectLayout.Side.RIGHT, 24, null, 6);
		city.houses(random, 36, 8);
		layout.plant(random, 50, 12);

		// The grounds first (laid on the ground as it lies, CityPieces#grounds): their box at the world's floor, so the land is shaped round
		// the buildings only, never levelled under all of it.
		CompoundTag data = new CompoundTag();
		data.putByteArray("Map", layout.map);
		builder.addPiece(new SectPiece(SectPiece.Kind.CITY_GROUNDS, frame.box(0, 0, W - 1, D - 1, ModDimensions.IMMORTAL_REALM_MIN_Y, 2), frame.facing(), seed,
				Alignment.Faction.NEUTRAL, true, data));
		for (SectPiece piece : city.pieces) builder.addPiece(piece);
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.IMMORTAL_CITY_TYPE;
	}
}
