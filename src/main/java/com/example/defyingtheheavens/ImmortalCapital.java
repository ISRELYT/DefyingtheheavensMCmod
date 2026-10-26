package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The Jade Capital and the Celestial Court on the Immortal Realm's main island (phase 2, 2026-10-25; the user's word: "the immortal gate
 * should lead to a main island where the biggest immortal city along with the immortal court resides"). North of the Southern Heavenly
 * Gate (ImmortalGate), the city's walls round the island's rolling ground (ModNoiseSettings#cityGround), the Court's pinnacle its north
 * wall's middle; the Court on the pinnacle's top.
 * <pre>
 *   north           [   the Celestial Court on its pinnacle   ]
 *                    flight ========== landing ========== flight   (the Court's stair, up its south face)
 *       [pagoda] [garden]   [alchemy hall] [pagoda] | [pagoda] [alchemy hall]   [garden] [pagoda]
 *   west gate ===== the cross avenue ===( the market square )=== east gate
 *     [city lord's hall] [meditation hall]  [bell] | [drum]  [meditation hall] [city lord's hall]
 *     [arena] [court]       [treasury]   shops | the Heavenly Avenue | shops   [treasury]       [court] [arena]
 *   south            [  the south gate tower  ]
 *                    (the Southern Heavenly Gate's plaza beyond)
 * </pre>
 * Laid on the land as it lies, the way Minecraft's villages and Terralith's are (the user's ask, 2026-10-25: the cities "dont need to be on
 * perfectly flat ground"; CityPlanner): every building, gate, tower and stretch of wall at the level of the ground under its own lot, the
 * land drawn up under each and cut away over it by vanilla's Beardifier as it is generated ({@link #beard}, as a structure's
 * {@code beard_thin} pieces are), and the ways, the market square, the footpaths, lanterns and trees laid on the ground where it is
 * ({@link #dress}, as a village's {@code terrain_matching} roads are), the square and the Gate's plaza each evened under it.
 * <p>
 * Too large for a structure (a structure's pieces are only laid within some 8 chunks of where it starts), it is laid by a feature
 * ({@link CapitalFeature}) in every chunk of the realm: the plan made once ({@link #plan}), each chunk drawing what of it crosses it.
 * Built in the city's colours (SectPalette#holy, the neutral path's: white jade, blue spirit wood, jade-green tiles, gold).
 */
public final class ImmortalCapital {
	/** The city's frame: its half-width and half-depth about the island's middle (401 by 401), its walls' thickness. */
	static final int A = 200, B = 200, W = 2 * A + 1, D = 2 * B + 1, WALL = 5;
	/** The Court's frame on its pinnacle (149 by 149 about the pinnacle's middle); buildings within {@link #COURT_REACH} of its middle. */
	static final int CA = 74, CB = 74, CW = 2 * CA + 1, CD = 2 * CB + 1, COURT_REACH = 72;
	/** How high over the Court's grounds the rock is cut away (its pinnacle's top is level but for a block or so of roughness). */
	static final int COURT_CLEAR = 24;
	/** The pinnacle's foot is kept clear of the city's buildings and ways this far round its middle (in the frame). */
	static final int PINNACLE_KEEP = (int) ModNoiseSettings.COURT_OUTER + 10;
	/** The Southern Heavenly Gate's plaza (ImmortalGate#plaza): the land is evened under it too. */
	static final int GATE_PLAZA = ImmortalGate.REACH + 12;
	/** The way from the Gate's plaza to the city's south gate: its paving this far either side of its middle. */
	static final int APPROACH_HALF = 6;
	/** All that is laid, in the world (the city, the Court's frame, the Gate's plaza and the way between): no chunk beyond asks for the plan. */
	private static final BoundingBox EXTENT = new BoundingBox(ModNoiseSettings.MAIN_X - A - 2, ModDimensions.IMMORTAL_REALM_MIN_Y,
			ModNoiseSettings.COURT_Z - CB - 2, ModNoiseSettings.MAIN_X + A + 2,
			ModDimensions.IMMORTAL_REALM_MIN_Y + ModDimensions.IMMORTAL_REALM_HEIGHT - 1, ImmortalGate.HEAVEN_Z + GATE_PLAZA + 2);

	private static final Alignment.Faction CITY = Alignment.Faction.NEUTRAL;

	/**
	 * The capital and the Court as planned for a world: what is drawn (the Court's grounds first, then every building), the boxes the land is
	 * shaped round ({@link #beard}), the city's ground plan (its ways, lanterns and trees, laid on the ground as it lies: {@link #dress}),
	 * and the levels of the Gate's plaza, the market square and the Court.
	 */
	record Plan(List<SectPiece> pieces, List<BoundingBox> beards, byte[] map, SectStructure.Frame frame, int gateY, int squareY, int courtY) {}

	private static Plan plan;
	private static long planFor;

	/** The plan for the world {@code state} is of (made the first time any chunk near the capital asks, then kept). */
	static synchronized Plan plan(RandomState state, ChunkGenerator generator, LevelHeightAccessor heights) {
		long key = state.getOrCreateRandomFactory(DefyingTheHeavens.id("immortal_capital")).at(0, 0, 0).nextLong();
		if (plan != null && planFor == key) return plan;
		long start = System.currentTimeMillis();
		Plan made = make(key, CityPlanner.ground(generator, heights, state));
		plan = made;
		planFor = key;
		DefyingTheHeavens.LOGGER.info("The Jade Capital planned: {} pieces, its square at Y {}, the Court at Y {}, in {} ms", made.pieces().size(),
				made.squareY(), made.courtY(), System.currentTimeMillis() - start);
		return made;
	}

	private static Plan make(long key, CityPlanner.Ground ground) {
		List<SectPiece> pieces = new ArrayList<>();
		List<BoundingBox> beards = new ArrayList<>();
		int courtY = median(ground, ModNoiseSettings.COURT_X, ModNoiseSettings.COURT_Z, 16);
		CityPlanner court = court(key, ground, courtY, pieces);
		CityPlanner city = city(key, ground, courtY);
		pieces.addAll(court.pieces);
		pieces.addAll(city.pieces);
		beards.addAll(court.beards);
		beards.addAll(city.beards);
		// The Gate's plaza: the land evened to the ground's level about it, which the Gate is built on (ImmortalGate#floorAt reads it).
		int gateY = median(ground, ImmortalGate.HEAVEN_X, ImmortalGate.HEAVEN_Z, 16);
		beards.add(new BoundingBox(ImmortalGate.HEAVEN_X - GATE_PLAZA, gateY, ImmortalGate.HEAVEN_Z - GATE_PLAZA, ImmortalGate.HEAVEN_X + GATE_PLAZA,
				gateY + 3, ImmortalGate.HEAVEN_Z + GATE_PLAZA));
		return new Plan(Collections.unmodifiableList(pieces), List.copyOf(beards), city.layout.map, city.frame, gateY, city.median(A, B, 16), courtY);
	}

	/** The median of the ground's top at nine columns {@code step} apart about world column (x, z). */
	private static int median(CityPlanner.Ground ground, int x, int z, int step) {
		List<Integer> heights = new ArrayList<>();
		for (int dx = -step; dx <= step; dx += step) {
			for (int dz = -step; dz <= step; dz += step) heights.add(ground.at(x + dx, z + dz));
		}
		Collections.sort(heights);
		return heights.get(heights.size() / 2);
	}

	/** Whether a column lies under the Court's pinnacle (its rock rises there, whatever the ground about it: the city wall's middle). */
	private static boolean underPinnacle(int x, int z) {
		double dx = x - ModNoiseSettings.COURT_X, dz = z - ModNoiseSettings.COURT_Z, r = ModNoiseSettings.COURT_OUTER + 2;
		return dx * dx + dz * dz <= r * r;
	}

	// --- The city ---

	private static CityPlanner city(long key, CityPlanner.Ground ground, int courtY) {
		RandomSource random = RandomSource.create(key ^ 0x7A3E5C1BL);
		long seed = random.nextLong();
		SectStructure.Frame frame = new SectStructure.Frame(Direction.NORTH, ModNoiseSettings.MAIN_X, ModNoiseSettings.MAIN_Z, W, D);
		SectLayout layout = new SectLayout(A, B, 300, seed);
		CityPlanner city = new CityPlanner(frame, layout, ground, seed, CITY, (x, z) -> underPinnacle(x, z) ? 1 : 0);
		// The whole square within the walls is the city's (not a sect's rounded grounds).
		Arrays.fill(layout.map, SectLayout.GRASS);
		int cu = A, cv = B;
		BlockPos corner = frame.world(0, 0);
		int pu = ModNoiseSettings.COURT_X - corner.getX(), pv = corner.getZ() - ModNoiseSettings.COURT_Z;
		// The pinnacle's foot: none of the city there (its rock is the north wall's middle).
		for (int v = Math.max(0, pv - PINNACLE_KEEP); v <= Math.min(D - 1, pv + PINNACLE_KEEP); v++) {
			int half = (int) Math.sqrt((double) PINNACLE_KEEP * PINNACLE_KEEP - (double) (v - pv) * (v - pv));
			int u0 = Math.max(0, pu - half), u1 = Math.min(W - 1, pu + half);
			for (int u = u0; u <= u1; u++) layout.map[u + v * W] = SectLayout.OUT;
			layout.claim(new SectLayout.Lot(u0, v, u1, v, SectLayout.Side.BACK), 0);
		}

		// The walls: four corner bastions, the gate towers (south, west, east; the north is the pinnacle's), the stretches between, each
		// at the ground's level under it.
		int tower = 15, gateW = 31, gateD = 13, wallHeight = CityPieces.WALL_HEIGHT + 8;
		for (int[] c : new int[][] {{0, 0}, {W - tower, 0}, {0, D - tower}, {W - tower, D - tower}}) {
			SectLayout.Lot lot = new SectLayout.Lot(c[0], c[1], c[0] + tower - 1, c[1] + tower - 1, SectLayout.Side.BACK);
			layout.claim(lot, 1);
			city.add(SectPiece.Kind.CITY_TOWER, lot, CityPieces.WALL_HEIGHT + 16, c[0] * 7L + c[1], null);
		}
		SectLayout.Lot south = new SectLayout.Lot(cu - gateW / 2, 0, cu + gateW / 2, gateD - 1, SectLayout.Side.BACK);
		SectLayout.Lot west = new SectLayout.Lot(0, cv - gateW / 2, gateD - 1, cv + gateW / 2, SectLayout.Side.RIGHT);
		SectLayout.Lot east = new SectLayout.Lot(W - gateD, cv - gateW / 2, W - 1, cv + gateW / 2, SectLayout.Side.LEFT);
		for (SectLayout.Lot gate : List.of(south, west, east)) {
			layout.claim(gate, 0);
			city.add(SectPiece.Kind.CITY_GATE, gate, CityPieces.WALL_HEIGHT + 26, gate.u0() * 31L + gate.v0(), null);
		}
		city.wall(new SectLayout.Lot(tower, 0, south.u0() - 1, WALL - 1, SectLayout.Side.BACK), wallHeight);
		city.wall(new SectLayout.Lot(south.u1() + 1, 0, W - tower - 1, WALL - 1, SectLayout.Side.BACK), wallHeight);
		city.wall(new SectLayout.Lot(tower, D - WALL, W - tower - 1, D - 1, SectLayout.Side.FRONT), wallHeight);
		city.wall(new SectLayout.Lot(0, tower, WALL - 1, west.v0() - 1, SectLayout.Side.RIGHT), wallHeight);
		city.wall(new SectLayout.Lot(0, west.v1() + 1, WALL - 1, D - tower - 1, SectLayout.Side.RIGHT), wallHeight);
		city.wall(new SectLayout.Lot(W - WALL, tower, W - 1, east.v0() - 1, SectLayout.Side.LEFT), wallHeight);
		city.wall(new SectLayout.Lot(W - WALL, east.v1() + 1, W - 1, D - tower - 1, SectLayout.Side.LEFT), wallHeight);

		// The ways: the Heavenly Avenue from the south gate to the way along the Court's stair, the cross avenue from gate to gate, the market
		// square where they meet, the wards' streets, a way round inside the walls.
		int stairV = pv - PINNACLE_KEEP - 8, stairRoad = stairV - 10;
		layout.road(cu, 0, cu, stairRoad + 2, 8);
		layout.road(0, cv, W - 1, cv, 6);
		layout.plaza(36);
		for (int u : new int[] {100, 300}) layout.road(u, WALL + 2, u, D - WALL - 3, 3);
		for (int v : new int[] {100, 300}) layout.road(WALL + 2, v, W - WALL - 3, v, 3);
		layout.road(WALL + 2, WALL + 6, W - WALL - 3, WALL + 6, 2);
		layout.road(WALL + 2, D - WALL - 7, W - WALL - 3, D - WALL - 7, 2);
		layout.road(WALL + 6, WALL + 2, WALL + 6, D - WALL - 3, 2);
		layout.road(W - WALL - 7, WALL + 2, W - WALL - 7, D - WALL - 3, 2);
		layout.road(cu - 100, stairRoad, cu + 100, stairRoad, 3);

		// The Court's stair, its flights along the pinnacle's south face, its bridge on to the Court's gate: laid at the ground's level along
		// its foot, rising from there to the Court (only its flights' ground evened, not the cliff its bridge crosses).
		List<Integer> foot = new ArrayList<>();
		for (int u = cu - 90; u <= cu + 90; u += 15) {
			BlockPos at = frame.world(u, stairV);
			foot.add(ground.at(at.getX(), at.getZ()));
		}
		Collections.sort(foot);
		int stairY = foot.get(foot.size() / 2), rise = Math.max(24, courtY - stairY), stairHalf = rise + 10, courtFront = pv - CB;
		SectLayout.Lot stairLot = new SectLayout.Lot(cu - stairHalf, stairV, cu + stairHalf, courtFront + 1, SectLayout.Side.BACK);
		layout.claim(stairLot, 0);
		CompoundTag stairData = new CompoundTag();
		stairData.putInt("Rise", rise);
		city.pieces.add(SectStructure.piece(SectPiece.Kind.COURT_STAIR, stairLot, frame, stairY, rise + 12, seed + 99, CITY, true, stairData));
		city.beards.add(frame.box(stairLot.u0(), stairV, stairLot.u1(), stairV + 16, stairY, 4));

		// The market square, the land evened under it, its stalls in a ring facing in; the bell and drum towers either side of the avenue
		// before it.
		int squareY = city.median(cu, cv, 16);
		city.beards.add(frame.box(cu - 37, cv - 37, cu + 37, cv + 37, squareY, 4));
		for (int k = 0; k < 12; k++) {
			double angle = Math.toRadians(15 + 30 * k);
			int u = cu + (int) Math.round(Math.cos(angle) * 27), v = cv + (int) Math.round(Math.sin(angle) * 27);
			int du = u - cu, dv = v - cv;
			SectLayout.Side side = Math.abs(du) >= Math.abs(dv) ? (du < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT)
					: (dv < 0 ? SectLayout.Side.FRONT : SectLayout.Side.BACK);
			SectLayout.Lot lot = CityPlanner.lot(u, v, 5, 5, side);
			layout.claim(lot, 0);
			city.pieces.add(SectStructure.piece(SectPiece.Kind.CITY_STALL, lot, frame, squareY, 7, seed + 500 + k, CITY, true, null));
		}
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot lot = CityPlanner.lot(cu + k * 17, cv - 52, 13, 15, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.claim(lot, 2);
			CompoundTag towerData = new CompoundTag();
			towerData.putBoolean("Drum", k > 0);
			city.add(SectPiece.Kind.HOLY_TOWER, lot, 28, 10 + k, towerData);
		}
		// Twin nine-storey pagodas flanking the avenue's end, the Court's stair rising behind them.
		for (int k = -1; k <= 1; k += 2) {
			city.place(SectPiece.Kind.PAGODA, cu + k * 22, stairRoad - 14, 17, 17, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT, 6 * 9 + 14,
					CityPlanner.tiers(9), 31 + k);
		}

		// Shops along both avenues, their fronts on the avenue, a stone lantern between each and the next.
		for (int v = 22; v + 5 < stairRoad - 4; v += 13) {
			if (Math.abs(v - cv) <= 44) continue;
			for (int k = -1; k <= 1; k += 2) {
				city.shop(cu + k * 14, v, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.lantern(cu + k * 10, v + 7);
			}
		}
		for (int u = 22; u < W - 22; u += 13) {
			if (Math.abs(u - cu) <= 44) continue;
			for (int k = -1; k <= 1; k += 2) {
				city.shop(u, cv + k * 12, k < 0 ? SectLayout.Side.FRONT : SectLayout.Side.BACK);
				layout.lantern(u + 7, cv + k * 8);
			}
		}

		// The great buildings, in pairs mirrored across the Heavenly Avenue, each on its own ground: the city lord's halls, meditation halls,
		// treasuries, arenas, alchemy halls, a five-storey pagoda and gardens in each ward; the courtyard houses of the great families.
		city.pair(SectPiece.Kind.MAIN_HALL, 55, 144, 51, 37, 40, null, 1);
		city.pair(SectPiece.Kind.MEDITATION_HALL, 143, 144, 31, 21, 20, null, 2);
		city.pair(SectPiece.Kind.TREASURY, 143, 55, 25, 21, 20, null, 3);
		city.pair(SectPiece.Kind.TRAINING, 40, 55, SectPiece.TRAINING_W, SectPiece.TRAINING_D, 9, null, 4);
		city.pair(SectPiece.Kind.HOLY_ALCHEMY, 143, 237, 25, 21, 24, null, 5);
		city.pair(SectPiece.Kind.PAGODA, 55, 256, 17, 17, 6 * 5 + 14, CityPlanner.tiers(5), 6);
		city.pair(SectPiece.Kind.GARDEN, 55, 228, 19, 15, 10, null, 7);
		city.pair(SectPiece.Kind.GARDEN, 55, 365, 19, 15, 10, null, 8);
		for (int[] c : new int[][] {{76, 55}, {40, 330}, {76, 330}}) {
			for (int k = -1; k <= 1; k += 2) city.courtyard(k < 0 ? c[0] : W - 1 - c[0], c[1]);
		}
		// The many houses of the city between, each on its own ground (none where it would stand on a slope too steep for it).
		city.houses(random, 140, 30);
		layout.plant(random, 160, 30);
		return city;
	}

	// --- The Court ---

	/**
	 * The Celestial Court on its pinnacle's level top, at {@code y}: its gate where the stair's bridge comes on, golden lions up the way to the
	 * Heavenly King's palace, the four Heavenly Venerables' halls either side of the court before it and the fifth behind it, ways round the
	 * palace; its grounds levelled (the pinnacle's top is level by its making) and planted, laid first in {@code grounds}.
	 */
	private static CityPlanner court(long key, CityPlanner.Ground ground, int y, List<SectPiece> grounds) {
		RandomSource random = RandomSource.create(key ^ 0x3C0D7EAFL);
		long seed = random.nextLong();
		SectStructure.Frame frame = new SectStructure.Frame(Direction.NORTH, ModNoiseSettings.COURT_X, ModNoiseSettings.COURT_Z, CW, CD);
		SectLayout layout = new SectLayout(CA, CB, COURT_REACH, seed);
		CityPlanner court = new CityPlanner(frame, layout, ground, seed, CITY, null);
		int cu = CA, cv = CB;
		for (int u = 0; u < CW; u++) {
			for (int v = 0; v < CD; v++) {
				if ((u - cu) * (u - cu) + (v - cv) * (v - cv) > (COURT_REACH + 1) * (COURT_REACH + 1)) layout.map[u + v * CW] = SectLayout.OUT;
			}
		}
		// The way from the gate to the palace; a way before the palace, round its sides and behind it to the fifth hall.
		int palaceV0 = 70, palaceDepth = 57, behind = palaceV0 + palaceDepth + 2;
		layout.road(cu, 0, cu, palaceV0 + 2, 3);
		layout.road(cu - 38, palaceV0 - 3, cu + 38, palaceV0 - 3, 1);
		for (int k = -1; k <= 1; k += 2) layout.road(cu + k * 38, palaceV0 - 3, cu + k * 38, behind, 1);
		layout.road(cu - 38, behind, cu + 38, behind, 1);
		// The gate at the pinnacle's south edge, where the stair's bridge comes on.
		SectLayout.Lot gateLot = new SectLayout.Lot(cu - 17, 2, cu + 17, 10, SectLayout.Side.BACK);
		layout.keepClear(gateLot.u0(), gateLot.v0() - 1, gateLot.u1(), gateLot.v1() + 1);
		court.add(SectPiece.Kind.HOLY_GATE, gateLot, y, 26, 1, null);
		// Golden lions up the way.
		for (int v = 18; v <= 42; v += 12) {
			for (int k = -1; k <= 1; k += 2) {
				SectLayout.Lot lot = CityPlanner.lot(cu + k * 9, v, 7, 7, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 0);
				court.add(SectPiece.Kind.HOLY_STATUE, lot, y, 11, 200 + v * 2 + k, null);
			}
		}
		// The Heavenly King's palace at the head of the way.
		SectLayout.Lot palaceLot = new SectLayout.Lot(cu - 34, palaceV0, cu + 34, palaceV0 + palaceDepth - 1, SectLayout.Side.BACK);
		layout.claim(palaceLot, 2);
		court.add(SectPiece.Kind.HOLY_PALACE, palaceLot, y, 64, 50, null);
		// The five Heavenly Venerables' halls: two either side of the court before the palace, facing the way; the fifth behind the palace.
		for (int k = -1; k <= 1; k += 2) {
			for (int v : new int[] {28, 54}) {
				SectLayout.Lot lot = CityPlanner.lot(cu + k * 38, v, 21, 15, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 1);
				layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
				court.add(SectPiece.Kind.MEDITATION_HALL, lot, y, 20, 300 + v + k, null);
			}
		}
		SectLayout.Lot fifth = new SectLayout.Lot(cu - 10, behind + 4, cu + 10, behind + 16, SectLayout.Side.BACK);
		layout.claim(fifth, 1);
		court.add(SectPiece.Kind.MEDITATION_HALL, fifth, y, 20, 380, null);
		for (int k = 0; k < 12; k++) {
			double angle = Math.PI / 6 * k;
			layout.lantern(cu + (int) Math.round(Math.cos(angle) * 66), cv + (int) Math.round(Math.sin(angle) * 66));
		}
		layout.plant(random, 30, 6);
		CompoundTag data = new CompoundTag();
		data.putInt("Clear", COURT_CLEAR);
		data.putByteArray("Map", layout.map);
		grounds.add(new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, CW - 1, CD - 1, y, COURT_CLEAR + 2), Direction.NORTH, seed, CITY, true, data));
		return court;
	}

	// --- The land shaped round it ---

	/**
	 * The Beardifier for a chunk of the Immortal Realm's land as it is generated (NoiseBasedChunkGeneratorMixin): vanilla's ({@code vanilla},
	 * for its structures), and the capital's buildings within reach of the chunk added to it as a structure's {@code beard_thin} pieces
	 * are, so the land is drawn up under each and cut away over it (each box's floor its building's paving, the ground's top block there).
	 */
	public static Beardifier beard(NoiseBasedChunkGenerator generator, ChunkAccess chunk, RandomState state, Beardifier vanilla) {
		if (!generator.generatorSettings().is(ModNoiseSettings.IMMORTAL_REALM)) return vanilla;
		ChunkPos pos = chunk.getPos();
		if (!near(pos, Beardifier.BEARD_KERNEL_RADIUS)) return vanilla;
		Plan plan = plan(state, generator, chunk);
		int reach = Beardifier.BEARD_KERNEL_RADIUS, x0 = pos.getMinBlockX() - reach, z0 = pos.getMinBlockZ() - reach, x1 = pos.getMaxBlockX() + reach,
				z1 = pos.getMaxBlockZ() + reach;
		ObjectList<Beardifier.Rigid> rigids = new ObjectArrayList<>();
		for (BoundingBox box : plan.beards()) {
			if (box.maxX() < x0 || box.minX() > x1 || box.maxZ() < z0 || box.minZ() > z1) continue;
			rigids.add(new Beardifier.Rigid(box, TerrainAdjustment.BEARD_THIN, 1));
		}
		return rigids.isEmpty() ? vanilla : new CapitalBeardifier(vanilla, rigids);
	}

	/** Vanilla's Beardifier for a chunk with the capital's buildings added to it. */
	private static final class CapitalBeardifier extends Beardifier {
		private final Beardifier vanilla;

		CapitalBeardifier(Beardifier vanilla, ObjectList<Beardifier.Rigid> rigids) {
			super(rigids.iterator(), new ObjectArrayList<JigsawJunction>().iterator());
			this.vanilla = vanilla;
		}

		@Override
		public double compute(DensityFunction.FunctionContext context) {
			return vanilla.compute(context) + super.compute(context);
		}
	}

	/** Whether a chunk lies within {@code margin} of all the capital lays ({@link #EXTENT}). */
	private static boolean near(ChunkPos pos, int margin) {
		return pos.getMaxBlockX() + margin >= EXTENT.minX() && pos.getMinBlockX() - margin <= EXTENT.maxX()
				&& pos.getMaxBlockZ() + margin >= EXTENT.minZ() && pos.getMinBlockZ() - margin <= EXTENT.maxZ();
	}

	// --- Drawing ---

	/**
	 * Lays, in the chunk at {@code origin}, whatever of the capital and the Court crosses it (every chunk of the Immortal Realm asks; all but
	 * the island's are let go at once): its ways first, on the ground as the land was shaped, then its buildings.
	 */
	static boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!ModDimensions.isImmortalRealm(level.getLevel().dimension())) return false;
		ChunkPos chunkPos = new ChunkPos(origin);
		if (!near(chunkPos, 0)) return false;
		Plan plan = plan(level.getLevel().getChunkSource().randomState(), generator, level);
		BoundingBox chunk = new BoundingBox(chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(), chunkPos.getMaxBlockX(),
				level.getMaxBuildHeight() - 1, chunkPos.getMaxBlockZ());
		dress(level, plan, chunk, SectPalette.holy(CITY));
		for (SectPiece piece : plan.pieces()) {
			BoundingBox box = piece.getBoundingBox();
			if (box.maxX() < chunk.minX() || box.minX() > chunk.maxX() || box.maxZ() < chunk.minZ() || box.minZ() > chunk.maxZ()) continue;
			piece.postProcess(level, null, generator, random, chunk, chunkPos, origin);
		}
		return true;
	}

	/**
	 * The city's ways, footpaths, lanterns and trees in a chunk, and the way from the Gate's plaza to its south gate, laid on the ground where
	 * it lies (CityPieces#lay); before any building, so each column's top is still the ground's.
	 */
	private static void dress(WorldGenLevel level, Plan plan, BoundingBox chunk, SectPalette p) {
		BlockPos corner = plan.frame().world(0, 0);
		int approachNorth = corner.getZ() + 1, approachSouth = ImmortalGate.HEAVEN_Z - (ImmortalGate.REACH + 10) + 1;
		// (Out to a crown's spread beyond the chunk: a tree just over its edge draws its crown's share in it.)
		for (int x = chunk.minX() - CityPieces.CROWN; x <= chunk.maxX() + CityPieces.CROWN; x++) {
			for (int z = chunk.minZ() - CityPieces.CROWN; z <= chunk.maxZ() + CityPieces.CROWN; z++) {
				int u = x - corner.getX(), v = corner.getZ() - z;
				byte code = u >= 0 && u < W && v >= 0 && v < D ? plan.map()[u + v * W]
						: z >= approachNorth && z <= approachSouth ? approach(x - ImmortalGate.HEAVEN_X, z) : SectLayout.OUT;
				if (code == SectLayout.OUT || code == SectLayout.GRASS || !CityPieces.draws(chunk, code, x, z)) continue;
				int top = CityPieces.groundTop(level, x, z);
				if (top <= ModDimensions.IMMORTAL_CLOUD_Y) continue; // nothing under it to lay on
				CityPieces.lay(level, chunk, p, CITY, code, x, top, z);
			}
		}
	}

	/** What the way from the Gate's plaza to the city lays at {@code dx} from its middle: paving, its edging, lanterns along it. */
	private static byte approach(int dx, int z) {
		int d = Math.abs(dx);
		if (d < APPROACH_HALF) return SectLayout.PATH;
		if (d == APPROACH_HALF) return SectLayout.PATH_EDGE;
		return d == APPROACH_HALF + 2 && Math.floorMod(z, 6) == 0 ? SectLayout.LANTERN : SectLayout.OUT;
	}

	/** The feature that lays the capital (ModFeatures#IMMORTAL_CAPITAL): once in every chunk of the Immortal Realm. */
	public static final class CapitalFeature extends Feature<NoneFeatureConfiguration> {
		public CapitalFeature() {
			super(NoneFeatureConfiguration.CODEC);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			return ImmortalCapital.place(context.level(), context.chunkGenerator(), context.random(), context.origin());
		}
	}

	private ImmortalCapital() {}
}
