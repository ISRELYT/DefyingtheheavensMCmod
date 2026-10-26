package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A Demon Palace's grounds and buildings (DemonPalaces; the user's yes, 2026-10-25: "the demon counterpart of the Holy Lands", not a
 * Holy Land re-skinned nor a bigger den): grounds 181 across and 261 deep, as a Holy Land's, round the Formation plaza at their middle,
 * laid along one way from the great torii up to the Sovereign's hall and the colossus of the race's beast behind it (its own pieces
 * drawn by DemonPalacePieces, the rest the sects' own in the palace's dark stone, SectPalette#palace):
 * <pre>
 *   back                  [ the colossus of the race's beast, its head over the hall's roof ]
 *       [elder]                [ the Sovereign's hall on its dais of three steps ]             [elder]
 *       [elder]   =========================================================================   [elder]
 *       [elder]                                                                               [elder]
 *   [vault] =======(( the blood ring round the plaza, the core at its heart; four bridges ))======= [arena]
 *      [disciples' hall]   [statue]   | the Way of Beasts |   [statue]   [disciples' hall]
 *      [disciples' hall]   [statue]   |                   |   [statue]   [disciples' hall]
 *   front                        [torii] [torii] [ the great torii ]
 * </pre>
 * The way runs seven wide from the great torii through two lesser ones, between the pairs of statues of the Way of Beasts, over the
 * blood ring's bridges and across the plaza to the dais's stairs; a way crosses the plaza to the vault and the sparring ground, one runs
 * before the hall out to the elders' ways. The Sovereign sits on the throne in the hall; the six elders live in their houses down the
 * sides (the nearest the plaza to the strongest, SectManager#found); the disciples in the four halls of the front court.
 */
final class DemonPalacePlan {
	/** The grounds' half-width and half-depth (a Holy Land's: 181 by 261, the plaza at the middle). */
	static final int A = 90, B = 130, W = 2 * A + 1, D = 2 * B + 1;
	/** How far from the plaza any building reaches (the colossus's back corners: the barrier closes at Formations#HOLY_MAX_RADIUS). */
	static final int REACH = 130;
	/** How high over the grounds the land is cut away (a Holy Land's). */
	static final int CLEAR = 30;
	/** The colossus's blocks a unit of PalaceSculpt's beasts (it stands some fifty high), and a statue's of the Way of Beasts. */
	static final double COLOSSUS_SCALE = 0.85, STATUE_SCALE = 0.36;
	/** Half the blood ring's square (DemonPalacePieces#pool: its outer rim at 24.5). */
	static final int RING_HALF = 26;

	/** The level of a palace's grounds laid out as {@code frame}, or SectStructure#NO_SITE: a Holy Land's measure (HolyLandPlan#groundLevel). */
	static int groundLevel(Structure.GenerationContext context, SectStructure.Frame frame) {
		return HolyLandPlan.groundLevel(context, frame, A, B);
	}

	private static SectLayout.Lot lot(int u, int v, int width, int depth, SectLayout.Side side) {
		return SectLayout.Lot.centred(u, v, width, depth, side);
	}

	private static SectPiece piece(SectPiece.Kind kind, SectLayout.Lot lot, SectStructure.Frame frame, int y, int height, long seed, Bloodline race,
			CompoundTag extra) {
		return new SectPiece(kind, frame.box(lot.u0(), lot.v0(), lot.u1(), lot.v1(), y, height), frame.toWorld(lot.side()), seed,
				Alignment.Faction.NEUTRAL, true, extra, race);
	}

	/** Lays out a palace of {@code race} at the origin as worldgen would (its plaza at (0, 64, 0), on level ground), for the game tests. */
	static List<net.minecraft.world.level.levelgen.structure.StructurePiece> layOut(long seed, Direction facing, Bloodline race) {
		StructurePiecesBuilder builder = new StructurePiecesBuilder();
		plan(builder, new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(seed)), new SectStructure.Frame(facing, 0, 0, W, D), 64, race,
				CityPlanner.flat(64), 0);
		return builder.build().pieces();
	}

	/**
	 * Lays a palace of {@code race} out in {@code frame} (DenStructure: always of DemonPalaces#MEMBERS), each building on its own ground
	 * ({@code ground}: CityPlanner#level, the land shaped round it as the structure is beard_thin; the user's ask, 2026-10-25, that structures
	 * sit on the land as Minecraft's and Terralith's do), its grounds laid on the land as it lies (their box at {@code floor}).
	 */
	static void plan(StructurePiecesBuilder builder, WorldgenRandom random, SectStructure.Frame frame, int y, Bloodline race, CityPlanner.Ground ground,
			int floor) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random); // a clan's path, as a den's (DenStructure)
		int cu = A, cv = B, r = SectPiece.PLAZA_RADIUS;
		SectLayout layout = new SectLayout(A, B, REACH, seed);
		CityPlanner fit = new CityPlanner(frame, layout, ground, seed, Alignment.Faction.NEUTRAL, null);
		List<SectPiece> pieces = new ArrayList<>();
		Direction back = frame.facing();
		DemonPalacePieces.Sculpture colossus = DemonPalacePieces.colossus(race), statue = DemonPalacePieces.statue(race);
		int gateV = 2, hallV0 = cv + RING_HALF + 3, hallV1 = hallV0 + DemonPalacePieces.HALL_DEPTH - 1, colossusV0 = hallV1 + 1;

		// The ways: up the middle, seven wide, from the great torii to the dais's stairs; across the plaza to the vault and the sparring
		// ground; before the hall out to the elders' ways, and those up the sides; the plaza at the heart.
		layout.road(cu, 0, cu, hallV0 + 2, 3);
		layout.road(cu - 52, cv, cu + 52, cv, 2);
		layout.road(cu - 46, hallV0 - 3, cu + 46, hallV0 - 3, 2);
		for (int k = -1; k <= 1; k += 2) layout.road(cu + k * 46, hallV0 - 3, cu + k * 46, hallV0 + 76, 1);
		layout.plaza(r);

		// The great torii at the front, the one way in, and the two lesser behind it.
		SectLayout.Lot gateLot = new SectLayout.Lot(cu - DemonPalacePieces.GATE_WIDTH / 2, gateV, cu + DemonPalacePieces.GATE_WIDTH / 2,
				gateV + DemonPalacePieces.GATE_DEPTH - 1, SectLayout.Side.BACK);
		layout.keepClear(gateLot.u0(), gateLot.v0() - 1, gateLot.u1(), gateLot.v1() + 1);
		SectPiece gate = piece(SectPiece.Kind.PALACE_GATE, gateLot, frame, fit.level(gateLot), DemonPalacePieces.GATE_HEIGHT, seed + 1, race, null);
		pieces.add(gate);
		for (int i = 1; i <= 2; i++) {
			int v = gateV + DemonPalacePieces.GATE_DEPTH + 6 + (i - 1) * 11;
			SectLayout.Lot lot = new SectLayout.Lot(cu - DemonPalacePieces.LESSER_WIDTH / 2, v, cu + DemonPalacePieces.LESSER_WIDTH / 2,
					v + DemonPalacePieces.LESSER_DEPTH - 1, SectLayout.Side.BACK);
			layout.keepClear(lot.u0(), lot.v0(), lot.u1(), lot.v1());
			CompoundTag lesser = new CompoundTag();
			lesser.putBoolean("Lesser", true);
			pieces.add(piece(SectPiece.Kind.PALACE_GATE, lot, frame, fit.level(lot), DemonPalacePieces.LESSER_HEIGHT, seed + 1 + i, race, lesser));
		}

		// The Way of Beasts: two pairs of the race's beasts on their plinths, facing across it, braziers' lanterns between.
		int statueW = statue.grid().w + 2 * DemonPalacePieces.PLINTH_REACH, statueD = statue.grid().d + 2 * DemonPalacePieces.PLINTH_REACH;
		int statueH = statue.grid().h + DemonPalacePieces.PLINTH_HIGH + 2;
		for (int v : new int[] {cv - 81, cv - 44}) {
			for (int k = -1; k <= 1; k += 2) {
				SectLayout.Lot lot = lot(cu + k * (7 + statueD / 2), v, statueW, statueD, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 1);
				pieces.add(piece(SectPiece.Kind.PALACE_STATUE, lot, frame, fit.level(lot), statueH, seed + 200 + v * 2 + k, race, null));
			}
		}
		for (int v = gateV + 34; v <= cv - RING_HALF - 6; v += 6) {
			layout.lantern(cu - 5, v);
			layout.lantern(cu + 5, v);
		}

		// The disciples' four halls in the front court, their doors on the way.
		List<SectPiece> halls = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			for (int v : new int[] {cv - 78, cv - 44}) {
				SectLayout.Lot lot = lot(cu + k * 54, v, 31, 21, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(lot, 2);
				layout.apron(lot, 1, 2);
				layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
				halls.add(piece(SectPiece.Kind.MEDITATION_HALL, lot, frame, fit.level(lot), 20, seed + 300 + v + k, race, null));
			}
		}
		pieces.addAll(halls);

		// The blood ring round the plaza (its bridges carry the ways over it).
		SectLayout.Lot ringLot = new SectLayout.Lot(cu - RING_HALF, cv - RING_HALF, cu + RING_HALF, cv + RING_HALF, SectLayout.Side.BACK);
		layout.claim(ringLot, 0);
		// The ring and the plaza within it on one level, their ground's.
		int ringY = fit.level(ringLot);
		pieces.add(piece(SectPiece.Kind.PALACE_POOL, ringLot, frame, ringY, 8, seed + 5, race, null));
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k + Math.PI / 8;
			layout.lantern(cu + (int) Math.round(Math.cos(angle) * (RING_HALF + 3)), cv + (int) Math.round(Math.sin(angle) * (RING_HALF + 3)));
		}

		// The vault on one side of the plaza, the sparring ground on the other.
		SectLayout.Lot vaultLot = lot(cu - 64, cv, 25, 21, SectLayout.Side.LEFT);
		layout.claim(vaultLot, 2);
		layout.apron(vaultLot, 1, 1);
		layout.footpath(vaultLot.doorstep()[0], vaultLot.doorstep()[1]);
		SectPiece vault = piece(SectPiece.Kind.TREASURY, vaultLot, frame, fit.level(vaultLot), 20, seed + 20, race, null);
		pieces.add(vault);
		SectLayout.Lot arenaLot = lot(cu + 64, cv, SectPiece.TRAINING_W, SectPiece.TRAINING_D, SectLayout.Side.RIGHT);
		layout.claim(arenaLot, 2);
		layout.footpath(arenaLot.doorstep()[0], arenaLot.doorstep()[1]);
		SectPiece arena = piece(SectPiece.Kind.TRAINING, arenaLot, frame, fit.level(arenaLot), 9, seed + 70, race, null);
		pieces.add(arena);

		// The colossus of the race's beast, and the Sovereign's hall right before it.
		int cw = colossus.grid().w, cd = colossus.grid().d;
		SectLayout.Lot colossusLot = new SectLayout.Lot(cu - cw / 2, colossusV0, cu - cw / 2 + cw - 1, colossusV0 + cd - 1, SectLayout.Side.BACK);
		layout.claim(colossusLot, 2);
		pieces.add(piece(SectPiece.Kind.PALACE_COLOSSUS, colossusLot, frame, fit.level(colossusLot), colossus.grid().h + 2, seed + 90, race, null));
		SectLayout.Lot hallLot = new SectLayout.Lot(cu - DemonPalacePieces.HALL_WIDTH / 2, hallV0, cu + DemonPalacePieces.HALL_WIDTH / 2, hallV1,
				SectLayout.Side.BACK);
		layout.claim(hallLot, 1);
		SectPiece hall = piece(SectPiece.Kind.PALACE_HALL, hallLot, frame, fit.level(hallLot), DemonPalacePieces.HALL_HEIGHT, seed + 50, race, null);
		pieces.add(hall);

		// The six elders' houses down the sides, beyond the colossus, doors to their ways.
		List<SectPiece> dwellings = new ArrayList<>();
		for (int k = -1; k <= 1; k += 2) {
			for (int v : new int[] {hallV0 + 12, hallV0 + 40, hallV0 + 68}) {
				SectLayout.Lot house = lot(cu + k * 56, v, 11, 11, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				layout.claim(house, 2);
				layout.footpath(house.doorstep()[0], house.doorstep()[1]);
				dwellings.add(piece(SectPiece.Kind.ELDER_HOUSE, house, frame, fit.level(house), 18, seed + 100 + v + k, race, null));
			}
		}
		pieces.addAll(dwellings);

		// Lanterns along the way before the hall and up the elders' ways; the race's trees and undergrowth between.
		for (int u = cu - 44; u <= cu + 44; u += 8) if (Math.abs(u - cu) > 22) layout.lantern(u, hallV0 - 7);
		for (int k = -1; k <= 1; k += 2) {
			for (int v = hallV0 + 2; v <= hallV0 + 74; v += 9) layout.lantern(cu + k * 50, v);
		}
		layout.plant(random, 110, 40);

		// The grounds go first, under everything: laid on the land as it lies, their box at the world's floor (SectPiece#followGround).
		CompoundTag groundsData = new CompoundTag();
		groundsData.putInt("A", A);
		groundsData.putInt("B", B);
		groundsData.putInt("Clear", CLEAR);
		groundsData.putByteArray("Map", layout.map);
		groundsData.putBoolean("Follow", true);
		SectPiece terrace = new SectPiece(SectPiece.Kind.TERRACE, frame.box(0, 0, W - 1, D - 1, floor, 2), back, seed, Alignment.Faction.NEUTRAL, true,
				groundsData, race);
		pieces.add(0, terrace);

		// The plaza last, so its blueprint can name every post of the grounds: within its ring, on the ring's level.
		BoundingBox plazaBox = frame.box(cu - r, cv - r, cu + r, cv + r, ringY, 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, Alignment.Faction.NEUTRAL, true, plazaData, race);
		BlockPos core = plaza.toWorld(r, 2, r);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = DemonPalacePieces.throne(hall);
		posts.get(Sect.Post.MASTER_SEAT).add(hall.toWorld(seat[0], seat[1], seat[2]));
		for (SectPiece h : halls) {
			for (int[] mat : h.meditationMats()) posts.get(Sect.Post.MEDITATION).add(h.toWorld(mat[0], 2, mat[1]));
		}
		for (int[] guard : vault.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(vault.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Homes: the elders' houses nearest the plaza first (the strongest are founded first, SectManager#found), then the disciples' mats.
		dwellings.sort((x, z) -> Double.compare(x.getBoundingBox().getCenter().distSqr(core), z.getBoundingBox().getCenter().distSqr(core)));
		for (SectPiece house : dwellings) {
			int[] mat = house.homeMat();
			posts.get(Sect.Post.DWELLING).add(house.toWorld(mat[0], mat[1], mat[2]));
		}
		for (SectPiece h : halls) {
			for (int[] mat : h.meditationMats()) posts.get(Sect.Post.DWELLING).add(h.toWorld(mat[0], 2, mat[1]));
		}
		// Where the inner disciples stroll: up the way between the beasts, round the blood ring, on its bridges, before the hall.
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		for (int v : new int[] {cv - 88, cv - 56, cv - RING_HALF - 4, hallV0 - 3}) courtyard.add(fit.onGround(cu, v));
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k;
			courtyard.add(plaza.toWorld(r + (int) Math.round(Math.cos(angle) * (RING_HALF + 2)), 1, r + (int) Math.round(Math.sin(angle) * (RING_HALF + 2))));
		}
		for (int k = -1; k <= 1; k += 2) courtyard.add(plaza.toWorld(r + k * 19, 1, r));
		int[] in = {gate.width() / 2, 1, gate.depth() + 2};
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));
		for (int[] post : arena.trainingPosts()) posts.get(Sect.Post.TRAINING).add(arena.toWorld(post[0], post[1], post[2]));
		// Points along the ways every ten blocks, for walkers that can't go straight (Sect#waypoints).
		List<BlockPos> ways = posts.get(Sect.Post.WAY);
		for (int v = gateV + 10; v <= hallV0 - 3; v += 10) if (Math.abs(v - cv) > r) ways.add(fit.onGround(cu, v));
		for (int u = cu - 50; u <= cu + 50; u += 10) if (Math.abs(u - cu) > r) ways.add(fit.onGround(u, cv));
		for (int u = cu - 40; u <= cu + 40; u += 10) ways.add(fit.onGround(u, hallV0 - 3));
		for (int k = -1; k <= 1; k += 2) {
			for (int v = hallV0 + 7; v <= hallV0 + 75; v += 10) ways.add(fit.onGround(cu + k * 46, v));
		}
		// The gate guards, before the great torii.
		List<BlockPos> guards = posts.get(Sect.Post.GATE_GUARD);
		guards.add(fit.onGround(cu - 3, gateV - 2));
		guards.add(fit.onGround(cu + 3, gateV - 2));

		// The barrier: from the core round every building and the colossus; the torii stay outside it.
		int radius = 0;
		for (SectPiece p : pieces) {
			if (p == terrace || p.kind() == SectPiece.Kind.PALACE_GATE) continue;
			BoundingBox box = p.getBoundingBox();
			for (int[] corner : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
				double dx = corner[0] - core.getX(), dz = corner[1] - core.getZ();
				radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz)) + 2);
			}
		}
		radius = Math.min(Formations.HOLY_MAX_RADIUS, radius);

		CompoundTag blueprint = new CompoundTag();
		blueprint.putLong("Seed", seed);
		blueprint.putInt("Alignment", alignment);
		blueprint.putString("Race", race.getId());
		blueprint.putInt("Capacity", Math.min(DemonPalaces.MEMBERS, posts.get(Sect.Post.DWELLING).size() + 1));
		blueprint.putInt("Radius", radius);
		blueprint.putIntArray("Bounds", SectStructure.bounds(terrace, pieces, plaza, 64));
		BoundingBox treasury = vault.getBoundingBox().inflatedBy(1);
		blueprint.putIntArray("Treasury", new int[] {treasury.minX(), treasury.minY(), treasury.minZ(), treasury.maxX(), treasury.maxY(), treasury.maxZ()});
		CompoundTag postTag = new CompoundTag();
		for (Map.Entry<Sect.Post, List<BlockPos>> entry : posts.entrySet()) {
			postTag.put(entry.getKey().name(), new LongArrayTag(entry.getValue().stream().mapToLong(BlockPos::asLong).toArray()));
		}
		blueprint.put("Posts", postTag);
		plazaData.put("Blueprint", blueprint);
		pieces.add(plaza);

		for (SectPiece p : pieces) builder.addPiece(p);
	}

	private DemonPalacePlan() {}
}
