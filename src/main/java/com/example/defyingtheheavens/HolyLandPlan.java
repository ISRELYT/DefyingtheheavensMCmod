package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A Holy Land's grounds and buildings (HolyLands; the user's ask, 2026-10-25: "larger and more grand than the previous sects, they should
 * have larger buildings and cover more ground", not the sects over again): grounds 201 across and 261 deep, near three times a great
 * sect's, round the Formation plaza at their middle, laid along one great axis from the Mountain Gate to the Patriarch's Palace (its
 * own buildings drawn by HolyLandPieces, the rest the sects' own made larger):
 * <pre>
 *   back                 [ the Patriarch's Palace on its three terraces ]
 *     [court] [garden]                                                [garden] [court]
 *     [court]  [scripture tower]  [ the Hall of the Great Dao ]  [alchemy hall]  [court]
 *     [court]                                                                    [court]
 *   [altar]    [pagoda] =====( the Great Dao Plaza, its core )===== [pagoda]    [treasury]
 *               [bell tower]                                  [drum tower]
 *     ~~~~~~~~~~~~~~~~~~~~~~~~~~~ the lotus moat ~~~~[bridge]~~~~~~~~~~~~~~~~~~~~~~~
 *     [garden] [disciples' hall]    lion | the Spirit Way | lion    [disciples' hall] [garden]
 *     [arena]  [disciples' hall]    lion |                | lion    [disciples' hall] [arena]
 *   front                          [ the Mountain Gate ]
 * </pre>
 * The Spirit Way runs seven wide from the gate over the moat's bridge, through the plaza (paved twenty-six round) to the Hall of the
 * Great Dao and on to the palace's stairs, the one way in; a way crosses the plaza to the pagodas' doors, another runs before the hall
 * out to the elders' courts, and one before the palace. The Patriarch sits in the palace; the Grand Elders and Elders live in the six courts (the
 * dwellings nearest the plaza go to the strongest, SectManager#found); the disciples in the four halls of the front court. Its posts
 * are a sect's, so its people live as any sect's do (NpcLife, Sect).
 */
final class HolyLandPlan {
	/** The grounds' half-width and half-depth (201 by 261, the plaza at the middle). */
	static final int A = 100, B = 130, W = 2 * A + 1, D = 2 * B + 1;
	/** How far from the plaza any building reaches (the barrier closes 2 past them: Formations#HOLY_MAX_RADIUS). */
	static final int REACH = 128;
	/**
	 * How high over the grounds the land was cut away when they were levelled (a sect's 22), and how uneven the ground under them may be
	 * (since 2026-10-25 each building stands on its own ground and the ways on the land as it lies, so rolling land takes a Holy Land too).
	 */
	static final int CLEAR = 30, MAX_RELIEF = 36;
	/** How many of the samples under the buildings may be water. */
	private static final int MAX_WET = 4;
	/** Below this continent mask (ModNoiseSettings#continentMask, 0 at a rim) a sample point is too near a continent's cliff. */
	private static final double CONTINENT_MIN = 0.05;

	// --- Its site ---

	/**
	 * The columns grounds of half-width {@code a} and half-depth {@code b} are sampled at, in the frame: 7 by 9 across where the buildings
	 * stand, then twelve round its rim.
	 */
	private static List<BlockPos> samplePoints(SectStructure.Frame frame, int a, int b) {
		List<BlockPos> points = new ArrayList<>();
		for (int i = 0; i <= 6; i++) {
			for (int j = 0; j <= 8; j++) points.add(frame.world(a + Math.round(a * 0.82f * (i - 3) / 3f), b + Math.round(b * 0.86f * (j - 4) / 4f)));
		}
		for (int k = 0; k < 12; k++) {
			double angle = Math.PI * 2 * k / 12;
			points.add(frame.world(a + (int) Math.round(Math.cos(angle) * a * 0.92), b + (int) Math.round(Math.sin(angle) * b * 0.92)));
		}
		return points;
	}

	/** Of {@link #samplePoints}, how many are the buildings' (the rest the rim's). */
	private static final int INNER = 63;

	/**
	 * The level of a Holy Land's grounds laid out as {@code frame}, or SectStructure#NO_SITE: every sample well inside a continent by its
	 * mask (asked first, nothing of the generator over the void), the ground no more uneven than {@link #MAX_RELIEF}, hardly any of it
	 * water; the grounds' level the median of the buildings' samples.
	 */
	static int groundLevel(Structure.GenerationContext context, SectStructure.Frame frame) {
		return groundLevel(context, frame, A, B);
	}

	/** As {@link #groundLevel(Structure.GenerationContext, SectStructure.Frame)}, for grounds of half-width {@code a}, half-depth {@code b} (a Demon Palace's too). */
	static int groundLevel(Structure.GenerationContext context, SectStructure.Frame frame, int a, int b) {
		RandomState state = context.randomState();
		List<BlockPos> points = samplePoints(frame, a, b);
		for (BlockPos at : points) {
			if (ModNoiseSettings.continentMask(state, at.getX(), at.getZ()) < CONTINENT_MIN) return SectStructure.NO_SITE;
		}
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		int[] samples = new int[points.size()];
		int water = 0;
		for (int n = 0; n < points.size(); n++) {
			BlockPos at = points.get(n);
			int y = generator.getFirstOccupiedHeight(at.getX(), at.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, state);
			if (y <= heights.getMinBuildHeight() + 2) return SectStructure.NO_SITE;
			samples[n] = y;
			if (n < INNER && n % 3 == 0) {
				NoiseColumn column = generator.getBaseColumn(at.getX(), at.getZ(), heights, state);
				BlockState top = column.getBlock(y);
				if (!top.getFluidState().isEmpty()) water++;
			}
		}
		int[] inner = Arrays.copyOf(samples, INNER);
		Arrays.sort(inner);
		int platformY = inner[INNER / 2];
		int[] sorted = samples.clone();
		Arrays.sort(sorted);
		if (sorted[sorted.length - 1] - sorted[0] > MAX_RELIEF || water > MAX_WET) return SectStructure.NO_SITE;
		if (platformY + 80 > heights.getMaxBuildHeight()) return SectStructure.NO_SITE;
		return platformY;
	}

	// --- Its plan ---

	private static SectLayout.Lot lot(int u, int v, int width, int depth, SectLayout.Side side) {
		return SectLayout.Lot.centred(u, v, width, depth, side);
	}

	/**
	 * Lays a Holy Land out in {@code frame} (SectStructure: always of {@link HolyLands#MEMBERS}), each building on its own ground
	 * ({@code ground}: CityPlanner#level, the land shaped round it as the structure is beard_thin; the user's ask, 2026-10-25, that structures
	 * sit on the land as Minecraft's and Terralith's do), its grounds laid on the land as it lies (their box at {@code floor}).
	 */
	static void plan(StructurePiecesBuilder builder, WorldgenRandom random, SectStructure.Frame frame, int y, CityPlanner.Ground ground, int floor) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random);
		Alignment.Faction faction = Alignment.factionOf(alignment);
		int cu = A, cv = B, r = SectPiece.PLAZA_RADIUS;
		SectLayout layout = new SectLayout(A, B, REACH, seed);
		CityPlanner fit = new CityPlanner(frame, layout, ground, seed, faction, null);
		List<SectPiece> pieces = new ArrayList<>();
		Direction back = frame.facing();

		// The ways: the Spirit Way, seven wide, from the gate to the palace's stairs; a way across the plaza to the pagodas' doors, one
		// before the Hall of the Great Dao out to the elders' courts, one before the palace; the paved plaza round the core.
		int gateV = 4, hallV0 = cv + 26, hallDepth = 37, palaceV0 = cv + 68, palaceDepth = 57;
		layout.road(cu, 0, cu, palaceV0 + 2, 3);
		layout.road(cu - 33, cv, cu + 33, cv, 2);
		layout.road(cu - 76, hallV0 - 3, cu + 76, hallV0 - 3, 2);
		layout.road(cu - 52, palaceV0 - 3, cu + 52, palaceV0 - 3, 2);
		layout.plaza(26);
		for (int k = 0; k < 16; k++) {
			double angle = Math.PI / 8 * k + Math.PI / 16;
			layout.lantern(cu + (int) Math.round(Math.cos(angle) * 28), cv + (int) Math.round(Math.sin(angle) * 28));
		}

		// The Mountain Gate at the front: the one way in (a Holy Land has no side gates).
		SectLayout.Lot gateLot = new SectLayout.Lot(cu - 17, gateV, cu + 17, gateV + 8, SectLayout.Side.BACK);
		layout.keepClear(gateLot.u0(), gateLot.v0() - 1, gateLot.u1(), gateLot.v1() + 1);
		SectPiece gate = SectStructure.piece(SectPiece.Kind.HOLY_GATE, gateLot, frame, fit.level(gateLot), 26, seed + 1, faction, true, null);
		pieces.add(gate);

		// The Spirit Way's guardians: lions of gold in pairs facing across it, and lanterns between them.
		for (int v = gateV + 18; v <= cv - 62; v += 13) {
			for (int k = -1; k <= 1; k += 2) {
				SectLayout.Lot lot = lot(cu + k * 10, v, 7, 7, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 0);
				pieces.add(SectStructure.piece(SectPiece.Kind.HOLY_STATUE, lot, frame, fit.level(lot), 11, seed + 200 + v * 2 + k, faction, true, null));
			}
		}
		for (int v = gateV + 12; v <= cv - 50; v += 6) {
			layout.lantern(cu - 5, v);
			layout.lantern(cu + 5, v);
		}

		// The disciples' four halls in the front court, their doors on the Spirit Way.
		List<SectPiece> halls = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			for (int v : new int[] {cv - 100, cv - 66}) {
				SectLayout.Lot lot = lot(cu + k * 30, v, 31, 21, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 2);
				layout.apron(lot, 1, 2);
				layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
				SectPiece hall = SectStructure.piece(SectPiece.Kind.MEDITATION_HALL, lot, frame, fit.level(lot), 20, seed + 300 + v + k, faction, true, null);
				halls.add(hall);
			}
		}
		pieces.addAll(halls);
		// An arena and a garden beyond each pair of halls.
		List<SectPiece> gardens = new ArrayList<>();
		SectPiece training = null;
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot arena = lot(cu + k * 62, cv - 86, SectPiece.TRAINING_W, SectPiece.TRAINING_D, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.claim(arena, 2);
			layout.footpath(arena.doorstep()[0], arena.doorstep()[1]);
			SectPiece yard = SectStructure.piece(SectPiece.Kind.TRAINING, arena, frame, fit.level(arena), 9, seed + 70 + k, faction, true, null);
			pieces.add(yard);
			if (training == null) training = yard;
			SectLayout.Lot garden = lot(cu + k * 62, cv - 60, 19, 15, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.claim(garden, 2);
			layout.footpath(garden.doorstep()[0], garden.doorstep()[1]);
			gardens.add(SectStructure.piece(SectPiece.Kind.GARDEN, garden, frame, fit.level(garden), 10, seed + 40 + k, faction, true, null));
		}

		// The lotus moat across the grounds, the Spirit Way crossing it by the bridge.
		int moatV0 = cv - 46;
		layout.claim(new SectLayout.Lot(0, moatV0, W - 1, moatV0 + 10, SectLayout.Side.BACK), 0);
		CompoundTag moatData = new CompoundTag();
		moatData.putInt("A", A);
		moatData.putInt("B", B);
		moatData.putInt("V0", moatV0);
		moatData.putLong("Seed", seed);
		// Its frame's 0 is its banks' level, as every building's (the water is laid below it; a box begun lower sank it all three blocks): one
		// level the whole way across (still water), the ground's along its middle, the land shaped to it either side.
		List<Integer> along = new ArrayList<>();
		for (int u = 12; u < W - 12; u += 12) {
			BlockPos at = frame.world(u, moatV0 + 5);
			along.add(ground.at(at.getX(), at.getZ()));
		}
		java.util.Collections.sort(along);
		int moatY = along.get(along.size() / 2);
		pieces.add(new SectPiece(SectPiece.Kind.HOLY_MOAT, frame.box(0, moatV0, W - 1, moatV0 + 10, moatY, 8), back, seed + 5, faction, true, moatData));

		// The bell tower and the drum tower before the plaza, facing each other across the Spirit Way.
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot lot = lot(cu + k * 36, cv - 24, 13, 15, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.claim(lot, 2);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			CompoundTag towerData = new CompoundTag();
			towerData.putBoolean("Drum", k > 0);
			pieces.add(SectStructure.piece(SectPiece.Kind.HOLY_TOWER, lot, frame, fit.level(lot), 28, seed + 10 + k, faction, true, towerData));
		}

		// The twin pagodas, nine storeys, either side of the plaza; beyond them the Altar of Heaven and the treasury.
		List<SectPiece> pagodas = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot lot = lot(cu + k * 42, cv, 17, 17, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			layout.claim(lot, 2);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			CompoundTag pagodaData = new CompoundTag();
			pagodaData.putInt("Tiers", 9);
			pagodas.add(SectStructure.piece(SectPiece.Kind.PAGODA, lot, frame, fit.level(lot), 6 * 9 + 14, seed + 31 + k, faction, true, pagodaData));
		}
		pieces.addAll(pagodas);
		int altarSize = SectPiece.altarSize(3);
		SectLayout.Lot altarLot = lot(cu - 76, cv, altarSize, altarSize, SectLayout.Side.LEFT);
		layout.claim(altarLot, 2);
		layout.footpath(altarLot.doorstep()[0], altarLot.doorstep()[1]);
		CompoundTag altarData = new CompoundTag();
		altarData.putInt("Tiers", 3);
		SectPiece altar = SectStructure.piece(SectPiece.Kind.ALTAR, altarLot, frame, fit.level(altarLot), 2 * 3 + 7, seed + 60, faction, true, altarData);
		pieces.add(altar);
		SectLayout.Lot treasuryLot = lot(cu + 74, cv, 25, 21, SectLayout.Side.RIGHT);
		layout.claim(treasuryLot, 2);
		layout.apron(treasuryLot, 1, 1);
		layout.footpath(treasuryLot.doorstep()[0], treasuryLot.doorstep()[1]);
		SectPiece treasury = SectStructure.piece(SectPiece.Kind.TREASURY, treasuryLot, frame, fit.level(treasuryLot), 20, seed + 20, faction, true, null);
		pieces.add(treasury);

		// The Hall of the Great Dao behind the plaza; the scripture tower and the alchemy hall either side of it.
		SectLayout.Lot hallLot = new SectLayout.Lot(cu - 25, hallV0, cu + 25, hallV0 + hallDepth - 1, SectLayout.Side.BACK);
		layout.claim(hallLot, 2);
		SectPiece daoHall = SectStructure.piece(SectPiece.Kind.MAIN_HALL, hallLot, frame, fit.level(hallLot), 40, seed + 30, faction, true, null);
		pieces.add(daoHall);
		SectLayout.Lot towerLot = lot(cu - 42, hallV0 + 16, 21, 21, SectLayout.Side.BACK);
		layout.claim(towerLot, 2);
		layout.footpath(towerLot.doorstep()[0], towerLot.doorstep()[1]);
		CompoundTag towerData = new CompoundTag();
		towerData.putInt("Tiers", 3);
		SectPiece scriptures = SectStructure.piece(SectPiece.Kind.PAGODA, towerLot, frame, fit.level(towerLot), 6 * 3 + 14, seed + 33, faction, true, towerData);
		pieces.add(scriptures);
		SectLayout.Lot alchemyLot = lot(cu + 42, hallV0 + 16, 25, 21, SectLayout.Side.BACK);
		layout.claim(alchemyLot, 2);
		layout.footpath(alchemyLot.doorstep()[0], alchemyLot.doorstep()[1]);
		pieces.add(SectStructure.piece(SectPiece.Kind.HOLY_ALCHEMY, alchemyLot, frame, fit.level(alchemyLot), 24, seed + 34, faction, true, null));

		// The six elders' courts down the sides, each with its elder's house in its middle.
		List<SectPiece> dwellings = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			for (int v : new int[] {cv + 38, cv + 64, cv + 90}) {
				int u = cu + k * 68;
				SectLayout.Lot court = lot(u, v, 23, 23, SectLayout.Side.BACK);
				// The court and its house on the court's ground.
				int courtY = fit.level(court);
				// Its walls only (its house within is claimed apart, and a footpath finds the gate from the house's door); its front wall
				// stands a row in (HolyLandPieces#court).
				int wallV = court.v0() + 1;
				layout.claim(new SectLayout.Lot(court.u0(), wallV, court.u0(), court.v1(), SectLayout.Side.BACK), 0);
				layout.claim(new SectLayout.Lot(court.u1(), wallV, court.u1(), court.v1(), SectLayout.Side.BACK), 0);
				layout.claim(new SectLayout.Lot(court.u0(), court.v1(), court.u1(), court.v1(), SectLayout.Side.BACK), 0);
				layout.claim(new SectLayout.Lot(court.u0(), wallV, u - 2, wallV, SectLayout.Side.BACK), 0);
				layout.claim(new SectLayout.Lot(u + 2, wallV, court.u1(), wallV, SectLayout.Side.BACK), 0);
				pieces.add(SectStructure.piece(SectPiece.Kind.HOLY_COURT, court, frame, courtY, 8, seed + 400 + v * 2 + k, faction, true, null));
				SectLayout.Lot house = lot(u, v + 2, 11, 11, SectLayout.Side.BACK);
				layout.claim(house, 0);
				layout.footpath(house.doorstep()[0], house.doorstep()[1]);
				layout.footpath(u, court.v0());
				dwellings.add(SectStructure.piece(SectPiece.Kind.ELDER_HOUSE, house, frame, courtY, 18, seed + 100 + v + k, faction, true, null));
			}
		}
		pieces.addAll(dwellings);
		// Two more gardens behind the courts' row, either side of the palace.
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot garden = lot(cu + k * 54, cv + 110, 17, 13, SectLayout.Side.BACK);
			if (!layout.within(garden, 0.97)) continue;
			layout.claim(garden, 2);
			layout.footpath(garden.doorstep()[0], garden.doorstep()[1]);
			gardens.add(SectStructure.piece(SectPiece.Kind.GARDEN, garden, frame, fit.level(garden), 10, seed + 42 + k, faction, true, null));
		}
		pieces.addAll(gardens);

		// The Patriarch's Palace at the head of the way.
		SectLayout.Lot palaceLot = new SectLayout.Lot(cu - 34, palaceV0, cu + 34, palaceV0 + palaceDepth - 1, SectLayout.Side.BACK);
		layout.claim(palaceLot, 2);
		SectPiece palace = SectStructure.piece(SectPiece.Kind.HOLY_PALACE, palaceLot, frame, fit.level(palaceLot), 64, seed + 50, faction, true, null);
		pieces.add(palace);

		// Lanterns along the way before the hall, trees and bamboo on the lawns between.
		for (int u = cu - 72; u <= cu + 72; u += 8) {
			if (Math.abs(u - cu) <= 28) continue;
			layout.lantern(u, hallV0 - 7);
		}
		layout.plant(random, 150, 40);

		// The grounds go first, under everything: laid on the land as it lies, their box at the world's floor (SectPiece#followGround).
		CompoundTag groundsData = new CompoundTag();
		groundsData.putInt("A", A);
		groundsData.putInt("B", B);
		groundsData.putInt("Clear", CLEAR);
		groundsData.putByteArray("Map", layout.map);
		groundsData.putBoolean("Follow", true);
		SectPiece terrace = new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, W - 1, D - 1, floor, 2), back, seed, faction, true, groundsData);
		pieces.add(0, terrace);

		// The plaza last, so its blueprint can name every post of the grounds: on its own ground, evened under it.
		BoundingBox plazaBox = frame.box(cu - r, cv - r, cu + r, cv + r, fit.level(new SectLayout.Lot(cu - r, cv - r, cu + r, cv + r, SectLayout.Side.BACK)), 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, faction, true, plazaData);
		BlockPos core = plaza.toWorld(r, 2, r);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = HolyLandPieces.throne(palace);
		posts.get(Sect.Post.MASTER_SEAT).add(palace.toWorld(seat[0], seat[1], seat[2]));
		for (SectPiece hall : halls) {
			for (int[] mat : hall.meditationMats()) posts.get(Sect.Post.MEDITATION).add(hall.toWorld(mat[0], 2, mat[1]));
		}
		for (int[] guard : treasury.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(treasury.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Homes: the elders' houses in their courts, nearest the plaza first (the strongest are founded first, so the Grand Elders
		// get the nearest), then the disciples' mats.
		dwellings.sort((x, z) -> Double.compare(x.getBoundingBox().getCenter().distSqr(core), z.getBoundingBox().getCenter().distSqr(core)));
		for (SectPiece house : dwellings) {
			int[] mat = house.homeMat();
			posts.get(Sect.Post.DWELLING).add(house.toWorld(mat[0], mat[1], mat[2]));
		}
		for (SectPiece hall : halls) {
			for (int[] mat : hall.meditationMats()) posts.get(Sect.Post.DWELLING).add(hall.toWorld(mat[0], 2, mat[1]));
		}
		// Where the inner disciples stroll: the Spirit Way, round the plaza, the bridge, the gardens, the pagodas' tops, before the hall
		// and the palace.
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		for (int v : new int[] {cv - 80, cv - 56, cv - 30, hallV0 - 3, palaceV0 - 3}) courtyard.add(fit.onGround(cu, v));
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k;
			courtyard.add(plaza.toWorld(r + (int) Math.round(Math.cos(angle) * 19), 1, r + (int) Math.round(Math.sin(angle) * 19)));
		}
		for (SectPiece garden : gardens) courtyard.add(garden.toWorld(garden.width() / 2, 1, 1));
		for (SectPiece pagoda : pagodas) {
			int[] top = pagoda.pagodaSeat();
			courtyard.add(pagoda.toWorld(top[0], top[1], top[2]));
		}
		int[] in = gate.gateInside();
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));
		for (int[] post : altar.altarPosts()) posts.get(Sect.Post.ALTAR).add(altar.toWorld(post[0], post[1], post[2]));
		for (int[] post : training.trainingPosts()) posts.get(Sect.Post.TRAINING).add(training.toWorld(post[0], post[1], post[2]));
		// Points along the ways every ten blocks, for walkers that can't go straight (Sect#waypoints).
		List<BlockPos> ways = posts.get(Sect.Post.WAY);
		for (int v = gateV + 10; v <= palaceV0; v += 10) if (Math.abs(v - cv) > r && (v < moatV0 - 1 || v > moatV0 + 11)) ways.add(fit.onGround(cu, v));
		for (int u = cu - 30; u <= cu + 30; u += 10) if (Math.abs(u - cu) > r) ways.add(fit.onGround(u, cv));
		for (int u = cu - 70; u <= cu + 70; u += 10) {
			ways.add(fit.onGround(u, hallV0 - 3));
			if (Math.abs(u - cu) <= 50) ways.add(fit.onGround(u, palaceV0 - 3));
		}
		// The gate guards, before the Mountain Gate.
		List<BlockPos> guards = posts.get(Sect.Post.GATE_GUARD);
		guards.add(fit.onGround(cu - 3, gateV - 2));
		guards.add(fit.onGround(cu + 3, gateV - 2));

		// The barrier: from the core round every building, and a little more; the gate stays outside it.
		int radius = 0;
		for (SectPiece piece : pieces) {
			BoundingBox box = piece.getBoundingBox();
			if (piece == terrace || piece.kind() == SectPiece.Kind.HOLY_MOAT || piece == gate) continue;
			for (int[] corner : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
				double dx = corner[0] - core.getX(), dz = corner[1] - core.getZ();
				radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
			}
		}
		radius = Math.min(Formations.HOLY_MAX_RADIUS, radius);

		CompoundTag blueprint = new CompoundTag();
		blueprint.putLong("Seed", seed);
		blueprint.putInt("Alignment", alignment);
		blueprint.putInt("Capacity", Math.min(HolyLands.MEMBERS, posts.get(Sect.Post.DWELLING).size() + 1));
		blueprint.putInt("Radius", radius);
		blueprint.putIntArray("Bounds", SectStructure.bounds(terrace, pieces, plaza, 64));
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

	private HolyLandPlan() {}
}
