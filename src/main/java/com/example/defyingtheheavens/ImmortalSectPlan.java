package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An immortal sect (ImmortalSects; phase 2 of the realm plan, 2026-10-25), the tier above the Holy Lands and laid out as none below it is:
 * every building of it on floating islands over the sea of clouds, joined by bridges of white jade (the user's words, 2026-10-25: "the
 * whole sects should be on smaller floating islands like the higher bits with the paths joining them", "the floating sect should be over
 * the cloud sea not above a giant island", "the ground court buildings should still exist just on floating islands"). Mirrored about the
 * one axis from the Mountain Gate to the Sect Lord's hall, each row of islands a little higher than the one before it:
 * <pre>
 *   back     [west elders' island]          [ the Sect Lord's island: the main hall ]          [east elders' island]      (+10 .. +14)
 *                     ||                                       ||                                        ||
 *   [herb garden] = [library island: the pagoda] = [ the arena's island ] = [alchemy island: its hall] = [herb garden]    (+4 .. +6)
 *                                                              ||
 *            [Altar of Heaven] ====== [ the core's island: the plaza, the bell and drum towers ] ====== [treasury]        (0 .. +2)
 *                                                              ||
 *   [disciples' court: two halls] ===== [ the Mountain Gate's island: the Spirit Way, its lions ] ===== [disciples' court] (-6 .. -4)
 *   front                                   (its forecourt, where those who fly in alight)
 * </pre>
 * No way up to it but through the air, as befits immortals (the realm's least are of Entering Nirvana and fly, its people too:
 * CultivatorNpc). The Sect Lord sits in the main hall, the six elders live in the houses on the elders' islands (the nearest the core go to
 * the strongest, SectManager#found), the disciples in the four halls of the courts; the library is the pagoda's (SectPiece#LIBRARY_LOOT, the
 * Immortal Realm's own True Immortal slips). Its posts are a sect's, so its people live as any sect's do (NpcLife, Sect); the bridges are
 * walked like any way (the WAY posts along them).
 */
final class ImmortalSectPlan {
	/**
	 * The grounds' half-width and half-depth (261 by 265, laid about their start chunk's middle so that every piece lies within the eight
	 * chunks of it that worldgen places pieces in: SectStructure), the core at {@link #PU}, {@link #PV}.
	 */
	static final int A = 130, B = 132, W = 2 * A + 1, D = 2 * B + 1;
	static final int PU = A, PV = 110;
	/** How far a bridge lands onto each of its islands. */
	static final int LANDING = 4;
	/** The paved round court about the core (the plaza piece paves the inner {@link SectPiece#PLAZA_RADIUS} of it). */
	static final int COURT_PAVED = 16;
	/** No land may stand under the islands higher than this under the clouds' top (a pinnacle's head, a massif's rim or roots). */
	static final int SKY_CLEAR = 64;

	/** A floating island: its middle in the frame, its half-widths, how high its top stands over the core island's, how deep its rock hangs. */
	record Isle(int u, int v, int ru, int rv, int rise, int depth) {
		/** Its twin across the axis. */
		Isle mirrored() {
			return new Isle(2 * PU - u, v, ru, rv, rise, depth);
		}

		/** Where its rim (its ellipse's: the rim wavers only outward) crosses row {@code row} on its {@code side} (-1 its left, +1 its right). */
		int rimU(int row, int side) {
			double t = (row - v) / (double) rv;
			return u + side * (int) Math.floor(ru * Math.sqrt(Math.max(0, 1 - t * t)));
		}

		/** Where its rim crosses column {@code column} on its {@code side} (-1 its front, +1 its back). */
		int rimV(int column, int side) {
			double t = (column - u) / (double) ru;
			return v + side * (int) Math.floor(rv * Math.sqrt(Math.max(0, 1 - t * t)));
		}
	}

	static final Isle GATE = new Isle(PU, 34, 26, 32, -6, 14);
	static final Isle CORE = new Isle(PU, PV, 44, 28, 0, 18);
	static final Isle ARENA = new Isle(PU, 176, 22, 18, 4, 12);
	static final Isle LORD = new Isle(PU, 234, 40, 28, 14, 20);
	static final Isle COURT_W = new Isle(64, 34, 26, 32, -4, 14), COURT_E = COURT_W.mirrored();
	static final Isle ALTAR = new Isle(52, 118, 22, 22, 2, 14), TREASURY = ALTAR.mirrored();
	static final Isle LIBRARY = new Isle(72, 176, 22, 22, 6, 14), ALCHEMY = LIBRARY.mirrored();
	static final Isle GARDEN_W = new Isle(22, 176, 14, 14, 4, 10), GARDEN_E = GARDEN_W.mirrored();
	static final Isle ELDERS_W = new Isle(56, 234, 20, 20, 10, 14), ELDERS_E = ELDERS_W.mirrored();
	static final Isle[] ISLES = {GATE, CORE, ARENA, LORD, COURT_W, COURT_E, ALTAR, TREASURY, LIBRARY, ALCHEMY, GARDEN_W, GARDEN_E, ELDERS_W, ELDERS_E};
	/** The way across the core's island out to the altar and the treasury: a little behind the plaza's middle, clear of the towers. */
	static final int CROSS_V = PV + 8;
	/** Where the bridges back from the library and the alchemy hall to the elders' islands run, clear of both buildings (the east one mirrored). */
	static final int ELDERS_WAY_U = 59;

	// --- Its site ---

	/**
	 * The level of an immortal sect's core island's top laid out as {@code frame} at {@code site}, or SectStructure#NO_SITE: open sky under
	 * every island (no land at the middle or the rims of any higher than {@link #SKY_CLEAR} under the clouds' top: its site keeps the massifs
	 * off and its clearing the pinnacles, ModNoiseSettings#sectSite), and room for the Sect Lord's hall under the sky's top.
	 */
	static int skyLevel(Structure.GenerationContext context, SectStructure.Frame frame, ModNoiseSettings.CitySite site) {
		RandomState state = context.randomState();
		ChunkGenerator generator = context.chunkGenerator();
		LevelHeightAccessor heights = context.heightAccessor();
		for (Isle isle : ISLES) {
			for (int[] at : new int[][] {{0, 0}, {isle.ru(), 0}, {-isle.ru(), 0}, {0, isle.rv()}, {0, -isle.rv()}}) {
				BlockPos column = frame.world(isle.u() + at[0], isle.v() + at[1]);
				int top = generator.getFirstOccupiedHeight(column.getX(), column.getZ(), Heightmap.Types.WORLD_SURFACE_WG, heights, state);
				if (top > ModDimensions.IMMORTAL_CLOUD_Y - SKY_CLEAR) return SectStructure.NO_SITE;
			}
		}
		int y = (int) Math.round(site.level());
		return y + LORD.rise() + 48 > heights.getMaxBuildHeight() ? SectStructure.NO_SITE : y;
	}

	// --- Its plan ---

	/** What is laid on an island's top as the plan goes, in its own frame (ImmortalSectPieces#island): what it keeps clear, its paving, its stone lanterns. */
	private static final class Ground {
		final List<Integer> keep = new ArrayList<>(), paved = new ArrayList<>(), disc = new ArrayList<>(), lanterns = new ArrayList<>();
	}

	/** An island's lot in the frame: its ellipse and a few blocks round it (its rim wavers outward). */
	private static SectLayout.Lot isleLot(Isle isle) {
		return new SectLayout.Lot(isle.u() - isle.ru() - 3, isle.v() - isle.rv() - 3, isle.u() + isle.ru() + 3, isle.v() + isle.rv() + 3, SectLayout.Side.BACK);
	}

	/** Frame column {@code u} in an island's own frame. */
	private static int x(Isle isle, int u) {
		return u - (isle.u() - isle.ru() - 3);
	}

	/** Frame row {@code v} in an island's own frame. */
	private static int z(Isle isle, int v) {
		return v - (isle.v() - isle.rv() - 3);
	}

	private static void keep(Map<Isle, Ground> grounds, Isle isle, int u0, int v0, int u1, int v1) {
		Collections.addAll(grounds.get(isle).keep, x(isle, u0), z(isle, v0), x(isle, u1), z(isle, v1));
	}

	/** Paves a rectangle of the frame on an island ({@code how}: ImmortalSectPieces#PAVING, PATH or EDGE), its corners given either way round. */
	private static void pave(Map<Isle, Ground> grounds, Isle isle, int u0, int v0, int u1, int v1, int how) {
		Collections.addAll(grounds.get(isle).paved, x(isle, Math.min(u0, u1)), z(isle, Math.min(v0, v1)), x(isle, Math.max(u0, u1)),
				z(isle, Math.max(v0, v1)), how);
	}

	/** A building's paved terrace on its island: its lot and {@code margin} round it (no further than the island reaches). */
	private static void terrace(Map<Isle, Ground> grounds, Isle isle, SectLayout.Lot lot, int margin) {
		pave(grounds, isle, lot.u0() - margin, lot.v0() - margin, lot.u1() + margin, lot.v1() + margin, ImmortalSectPieces.PAVING);
	}

	private static void lantern(Map<Isle, Ground> grounds, Isle isle, int u, int v) {
		Collections.addAll(grounds.get(isle).lanterns, x(isle, u), z(isle, v));
	}

	private static int[] ints(List<Integer> values) {
		return values.stream().mapToInt(Integer::intValue).toArray();
	}

	private static SectLayout.Lot lot(int u, int v, int width, int depth, SectLayout.Side side) {
		return SectLayout.Lot.centred(u, v, width, depth, side);
	}

	/** A building on island {@code isle}, at its top, its lot kept clear of the island's flowers and trees. */
	private static SectPiece building(Map<Isle, Ground> grounds, SectStructure.Frame frame, int y, Isle isle, SectPiece.Kind kind, SectLayout.Lot lot,
			int height, long seed, Alignment.Faction faction, CompoundTag extra) {
		keep(grounds, isle, lot.u0(), lot.v0(), lot.u1(), lot.v1());
		return SectStructure.piece(kind, lot, frame, y + isle.rise(), height, seed, faction, true, extra);
	}

	/** The spot one stands on at frame column (u, v) of island {@code isle}: on its top. */
	private static BlockPos at(SectStructure.Frame frame, int y, Isle isle, int u, int v) {
		BlockPos column = frame.world(u, v);
		return new BlockPos(column.getX(), y + isle.rise() + 1, column.getZ());
	}

	/**
	 * Lays an immortal sect out in {@code frame} (SectStructure: always of {@link ImmortalSects#MEMBERS}), its core island's top at {@code y}
	 * and every other island at its rise over that.
	 */
	static void plan(StructurePiecesBuilder builder, WorldgenRandom random, SectStructure.Frame frame, int y) {
		long seed = random.nextLong();
		int alignment = Alignment.randomSect(random);
		Alignment.Faction faction = Alignment.factionOf(alignment);
		int cu = PU, r = SectPiece.PLAZA_RADIUS;
		Direction back = frame.facing();
		Map<Isle, Ground> grounds = new LinkedHashMap<>();
		for (Isle isle : ISLES) grounds.put(isle, new Ground());
		List<SectPiece> buildings = new ArrayList<>();

		// The Mountain Gate's island: the gate across its front half, its forecourt before it (where those who fly in alight), the Spirit Way
		// on from it to the bridge up to the core's island, paved seven wide between edging, golden lions either side and stone lanterns.
		SectLayout.Lot gateLot = lot(cu, GATE.v() - 13, 35, 9, SectLayout.Side.BACK);
		SectPiece gate = building(grounds, frame, y, GATE, SectPiece.Kind.HOLY_GATE, gateLot, 26, seed + 1, faction, null);
		buildings.add(gate);
		pave(grounds, GATE, cu - 9, gateLot.v0() - 9, cu + 9, gateLot.v0() - 1, ImmortalSectPieces.PAVING);
		int wayEnd = landing(GATE, true, cu, 1) - 1;
		pave(grounds, GATE, cu - 3, gateLot.v1() + 1, cu + 3, wayEnd, ImmortalSectPieces.PATH);
		pave(grounds, GATE, cu - 4, gateLot.v1() + 1, cu - 4, wayEnd, ImmortalSectPieces.EDGE);
		pave(grounds, GATE, cu + 4, gateLot.v1() + 1, cu + 4, wayEnd, ImmortalSectPieces.EDGE);
		for (int v : new int[] {GATE.v() + 8, GATE.v() + 20}) {
			for (int k = -1; k <= 1; k += 2) {
				SectLayout.Lot lion = lot(cu + k * 10, v, 7, 7, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
				buildings.add(building(grounds, frame, y, GATE, SectPiece.Kind.HOLY_STATUE, lion, 11, seed + 200 + v * 2L + k, faction, null));
			}
		}
		for (int v : new int[] {gateLot.v1() + 3, GATE.v() + 14, wayEnd}) {
			lantern(grounds, GATE, cu - 5, v);
			lantern(grounds, GATE, cu + 5, v);
		}

		// The core's island: the plaza round the core (the plaza piece last), a paved round court about it ringed with stone lanterns, the bell
		// tower on its left and the drum tower on its right, their doors on the court.
		Collections.addAll(grounds.get(CORE).disc, x(CORE, cu), z(CORE, PV), COURT_PAVED, ImmortalSectPieces.PAVING);
		for (int k = 0; k < 12; k++) {
			double angle = Math.PI / 6 * k + Math.PI / 12;
			lantern(grounds, CORE, cu + (int) Math.round(Math.cos(angle) * (COURT_PAVED + 1)), PV + (int) Math.round(Math.sin(angle) * (COURT_PAVED + 1)));
		}
		for (int k = -1; k <= 1; k += 2) {
			CompoundTag towerData = new CompoundTag();
			towerData.putBoolean("Drum", k > 0);
			SectLayout.Lot tower = lot(cu + k * 26, PV - 6, 13, 15, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			buildings.add(building(grounds, frame, y, CORE, SectPiece.Kind.HOLY_TOWER, tower, 28, seed + 10 + k, faction, towerData));
			// From the foot of its steps to the court, as wide as the steps (between the lanterns).
			int[] step = tower.doorstep();
			pave(grounds, CORE, step[0], step[1] - 1, cu + k * (COURT_PAVED - 2), step[1] + 1, ImmortalSectPieces.PAVING);
		}

		// The disciples' courts either side of the gate's island: on each two meditation halls facing each other across a paved court, where
		// the bridge from the Spirit Way lands.
		List<SectPiece> halls = new ArrayList<>();
		for (Isle court : new Isle[] {COURT_W, COURT_E}) {
			for (int k = -1; k <= 1; k += 2) {
				SectLayout.Lot lot = lot(court.u(), court.v() + k * 13, 31, 21, k < 0 ? SectLayout.Side.FRONT : SectLayout.Side.BACK);
				halls.add(building(grounds, frame, y, court, SectPiece.Kind.MEDITATION_HALL, lot, 20, seed + 300 + court.u() * 2L + k, faction, null));
			}
			pave(grounds, court, court.u() - 22, court.v() - 2, court.u() + 22, court.v() + 2, ImmortalSectPieces.PAVING);
		}
		buildings.addAll(halls);

		// The Altar of Heaven and the treasury out at either end of the way across the core's island, each on its paved terrace.
		CompoundTag altarData = new CompoundTag();
		altarData.putInt("Tiers", 3);
		int altarSize = SectPiece.altarSize(3);
		SectLayout.Lot altarLot = lot(ALTAR.u(), ALTAR.v(), altarSize, altarSize, SectLayout.Side.LEFT);
		SectPiece altar = building(grounds, frame, y, ALTAR, SectPiece.Kind.ALTAR, altarLot, 2 * 3 + 7, seed + 60, faction, altarData);
		terrace(grounds, ALTAR, altarLot, 3);
		SectLayout.Lot treasuryLot = lot(TREASURY.u(), TREASURY.v(), 25, 21, SectLayout.Side.RIGHT);
		SectPiece treasury = building(grounds, frame, y, TREASURY, SectPiece.Kind.TREASURY, treasuryLot, 20, seed + 20, faction, null);
		terrace(grounds, TREASURY, treasuryLot, 3);
		buildings.add(altar);
		buildings.add(treasury);

		// The arena's island: the training ground in its middle.
		SectLayout.Lot arenaLot = lot(cu, ARENA.v(), SectPiece.TRAINING_W, SectPiece.TRAINING_D, SectLayout.Side.BACK);
		SectPiece training = building(grounds, frame, y, ARENA, SectPiece.Kind.TRAINING, arenaLot, 9, seed + 70, faction, null);
		buildings.add(training);

		// The library's pagoda (its storeys the sect's library) and the alchemy hall either side of it, each on its paved terrace, their doors
		// toward it; the herb gardens out beyond them.
		CompoundTag libraryData = new CompoundTag();
		libraryData.putInt("Tiers", 5);
		SectLayout.Lot libraryLot = lot(LIBRARY.u(), LIBRARY.v(), 17, 17, SectLayout.Side.LEFT);
		SectPiece library = building(grounds, frame, y, LIBRARY, SectPiece.Kind.PAGODA, libraryLot, 6 * 5 + 14, seed + 33, faction, libraryData);
		terrace(grounds, LIBRARY, libraryLot, 3);
		SectLayout.Lot alchemyLot = lot(ALCHEMY.u(), ALCHEMY.v(), 25, 21, SectLayout.Side.RIGHT);
		buildings.add(library);
		buildings.add(building(grounds, frame, y, ALCHEMY, SectPiece.Kind.HOLY_ALCHEMY, alchemyLot, 24, seed + 34, faction, null));
		terrace(grounds, ALCHEMY, alchemyLot, 3);
		List<SectPiece> gardens = new ArrayList<>();
		List<SectLayout.Lot> gardenLots = new ArrayList<>();
		for (Isle isle : new Isle[] {GARDEN_W, GARDEN_E}) {
			SectLayout.Lot lot = lot(isle.u(), isle.v(), 19, 15, isle.u() < cu ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			gardenLots.add(lot);
			gardens.add(building(grounds, frame, y, isle, SectPiece.Kind.GARDEN, lot, 10, seed + 40 + isle.u(), faction, null));
		}
		buildings.addAll(gardens);

		// The elders' islands behind those: on each three houses round a paved court, their doors on it.
		List<SectPiece> dwellings = new ArrayList<>();
		for (Isle isle : new Isle[] {ELDERS_W, ELDERS_E}) {
			int s = isle.u() < cu ? -1 : 1;
			SectLayout.Lot[] houses = {lot(isle.u() + s * 11, isle.v() - 4, 11, 11, s < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT),
					lot(isle.u(), isle.v() + 11, 11, 11, SectLayout.Side.BACK),
					lot(isle.u() - s * 11, isle.v() - 4, 11, 11, s < 0 ? SectLayout.Side.RIGHT : SectLayout.Side.LEFT)};
			for (int i = 0; i < houses.length; i++) {
				dwellings.add(building(grounds, frame, y, isle, SectPiece.Kind.ELDER_HOUSE, houses[i], 18, seed + 100 + isle.u() * 3L + i, faction, null));
			}
			pave(grounds, isle, isle.u() - 5, isle.v() - 14, isle.u() + 5, isle.v() + 5, ImmortalSectPieces.PAVING);
		}
		buildings.addAll(dwellings);

		// The Sect Lord's island at the back, the highest: the main hall on its paved terrace.
		SectLayout.Lot hallLot = lot(cu, LORD.v(), 51, 37, SectLayout.Side.BACK);
		SectPiece lordHall = building(grounds, frame, y, LORD, SectPiece.Kind.MAIN_HALL, hallLot, 40, seed + 50, faction, null);
		terrace(grounds, LORD, hallLot, 3);
		buildings.add(lordHall);

		// The bridges: up the axis from the gate's island to the core's, the arena's and the Sect Lord's; out from the gate's to the disciples'
		// courts, from the core's to the altar and the treasury, from the arena's to the library and the alchemy hall, and on from those out
		// to the herb gardens and back to the elders' islands. Paved ways from their landings to the courts, the terraces and the doors.
		List<SectPiece> ways = new ArrayList<>();
		ways.add(bridge(grounds, frame, seed + 81, faction, y, GATE, CORE, true, cu));
		ways.add(bridge(grounds, frame, seed + 82, faction, y, CORE, ARENA, true, cu));
		ways.add(bridge(grounds, frame, seed + 83, faction, y, ARENA, LORD, true, cu));
		pave(grounds, CORE, cu - 2, landing(CORE, true, cu, -1), cu + 2, PV - COURT_PAVED, ImmortalSectPieces.PAVING);
		pave(grounds, CORE, cu - 2, PV + COURT_PAVED, cu + 2, landing(CORE, true, cu, 1), ImmortalSectPieces.PAVING);
		pave(grounds, ARENA, cu - 2, landing(ARENA, true, cu, -1), cu + 2, arenaLot.v0() - 1, ImmortalSectPieces.PAVING);
		pave(grounds, ARENA, cu - 2, arenaLot.v1() + 1, cu + 2, landing(ARENA, true, cu, 1), ImmortalSectPieces.PAVING);
		pave(grounds, LORD, cu - 2, landing(LORD, true, cu, -1), cu + 2, hallLot.v0() - 3, ImmortalSectPieces.PAVING);
		List<BlockPos> spots = new ArrayList<>();
		for (int s = -1; s <= 1; s += 2) {
			Isle court = s < 0 ? COURT_W : COURT_E, store = s < 0 ? ALTAR : TREASURY, hall = s < 0 ? LIBRARY : ALCHEMY, garden = s < 0 ? GARDEN_W : GARDEN_E,
					elders = s < 0 ? ELDERS_W : ELDERS_E;
			SectLayout.Lot storeLot = s < 0 ? altarLot : treasuryLot, ownLot = s < 0 ? libraryLot : alchemyLot, gardenLot = gardenLots.get(s < 0 ? 0 : 1);
			int eldersWay = s < 0 ? ELDERS_WAY_U : 2 * cu - ELDERS_WAY_U;
			long n = s < 0 ? 0 : 10;
			ways.add(bridge(grounds, frame, seed + 84 + n, faction, y, GATE, court, false, GATE.v()));
			ways.add(bridge(grounds, frame, seed + 85 + n, faction, y, CORE, store, false, CROSS_V));
			ways.add(bridge(grounds, frame, seed + 86 + n, faction, y, ARENA, hall, false, ARENA.v()));
			ways.add(bridge(grounds, frame, seed + 87 + n, faction, y, hall, garden, false, hall.v()));
			ways.add(bridge(grounds, frame, seed + 88 + n, faction, y, hall, elders, true, eldersWay));
			// In from the gate's landing to the Spirit Way, from the core's out to its court, from the arena's to its yard.
			pave(grounds, GATE, landing(GATE, false, GATE.v(), s), GATE.v() - 2, cu + s * 5, GATE.v() + 2, ImmortalSectPieces.PAVING);
			pave(grounds, CORE, landing(CORE, false, CROSS_V, s), CROSS_V - 2, cu + s * (COURT_PAVED - 3), CROSS_V + 2, ImmortalSectPieces.PAVING);
			pave(grounds, ARENA, landing(ARENA, false, ARENA.v(), s), ARENA.v() - 2, s < 0 ? arenaLot.u0() - 1 : arenaLot.u1() + 1, ARENA.v() + 2,
					ImmortalSectPieces.PAVING);
			// On to the altar's or the treasury's terrace; to the library's or the alchemy hall's from both its landings across, and from its
			// landing back down the way to the elders'; to the garden's gate; from the elders' landing up to their court.
			pave(grounds, store, landing(store, false, CROSS_V, -s), CROSS_V - 2, s < 0 ? storeLot.u1() + 3 : storeLot.u0() - 3, CROSS_V + 2,
					ImmortalSectPieces.PAVING);
			pave(grounds, hall, landing(hall, false, hall.v(), -s), hall.v() - 2, s < 0 ? ownLot.u1() + 3 : ownLot.u0() - 3, hall.v() + 2,
					ImmortalSectPieces.PAVING);
			pave(grounds, hall, landing(hall, false, hall.v(), s), hall.v() - 2, s < 0 ? ownLot.u0() - 3 : ownLot.u1() + 3, hall.v() + 2,
					ImmortalSectPieces.PAVING);
			pave(grounds, hall, eldersWay - 2, hall.v() - 2, eldersWay + 2, landing(hall, true, eldersWay, 1), ImmortalSectPieces.PAVING);
			pave(grounds, garden, landing(garden, false, garden.v(), -s), garden.v() - 2, s < 0 ? gardenLot.u1() + 1 : gardenLot.u0() - 1, garden.v() + 2,
					ImmortalSectPieces.PAVING);
			pave(grounds, elders, eldersWay - 2, landing(elders, true, eldersWay, -1), eldersWay + 2, elders.v() - 14, ImmortalSectPieces.PAVING);
			// A spot to stand on every one of these islands: the court's middle, before the altar or the treasury, before the library's or the
			// alchemy hall's door and on the way back to the elders', before the garden's gate, in the elders' court.
			spots.add(at(frame, y, court, court.u(), court.v()));
			spots.add(at(frame, y, store, s < 0 ? storeLot.u1() + 2 : storeLot.u0() - 2, CROSS_V));
			spots.add(at(frame, y, hall, s < 0 ? ownLot.u1() + 3 : ownLot.u0() - 3, hall.v()));
			spots.add(at(frame, y, hall, eldersWay, hall.v() + 10));
			spots.add(at(frame, y, garden, s < 0 ? gardenLot.u1() + 1 : gardenLot.u0() - 1, garden.v()));
			spots.add(at(frame, y, elders, elders.u(), elders.v() - 6));
		}
		spots.add(at(frame, y, GATE, cu, GATE.v()));
		spots.add(at(frame, y, CORE, cu, PV - COURT_PAVED + 1));
		spots.add(at(frame, y, ARENA, cu, arenaLot.v0() - 2));
		spots.add(at(frame, y, LORD, cu, hallLot.v0() - 2));

		// The islands themselves, each with what is laid on its top; in order the islands, what stands on them, then the bridges over their rims.
		List<SectPiece> pieces = new ArrayList<>();
		for (Isle isle : ISLES) {
			Ground ground = grounds.get(isle);
			CompoundTag data = new CompoundTag();
			data.putInt("RU", isle.ru());
			data.putInt("RV", isle.rv());
			data.putInt("Depth", isle.depth());
			data.putIntArray("Keep", ints(ground.keep));
			data.putIntArray("Paved", ints(ground.paved));
			data.putIntArray("Disc", ints(ground.disc));
			data.putIntArray("Lanterns", ints(ground.lanterns));
			pieces.add(SectStructure.piece(SectPiece.Kind.PEAK_ISLAND, isleLot(isle), frame, y + isle.rise(), 12, seed + 500 + isle.u() + isle.v() * 7L, faction, true,
					data));
		}
		pieces.addAll(buildings);
		pieces.addAll(ways);

		// The plaza last, so its blueprint can name every post: on the core's island, at its top.
		BoundingBox plazaBox = frame.box(cu - r, PV - r, cu + r, PV + r, y + CORE.rise(), 8);
		CompoundTag plazaData = new CompoundTag();
		SectPiece plaza = new SectPiece(SectPiece.Kind.PLAZA, plazaBox, back, seed + 50, faction, true, plazaData);
		BlockPos core = plaza.toWorld(r, 2, r);

		Map<Sect.Post, List<BlockPos>> posts = new EnumMap<>(Sect.Post.class);
		for (Sect.Post post : Sect.Post.values()) posts.put(post, new ArrayList<>());
		int[] seat = lordHall.masterSeat();
		posts.get(Sect.Post.MASTER_SEAT).add(lordHall.toWorld(seat[0], seat[1], seat[2]));
		for (SectPiece hall : halls) {
			for (int[] mat : hall.meditationMats()) posts.get(Sect.Post.MEDITATION).add(hall.toWorld(mat[0], 2, mat[1]));
		}
		for (int[] guard : treasury.treasuryGuards()) posts.get(Sect.Post.TREASURY).add(treasury.toWorld(guard[0], guard[1], guard[2]));
		for (int[] guard : SectPiece.corePosts()) posts.get(Sect.Post.CORE).add(plaza.toWorld(guard[0], 1, guard[1]));
		// Homes: the elders' houses, nearest the core first (the strongest are founded first), then the disciples' mats.
		dwellings.sort((a, b) -> Double.compare(a.getBoundingBox().getCenter().distSqr(core), b.getBoundingBox().getCenter().distSqr(core)));
		for (SectPiece house : dwellings) {
			int[] mat = house.homeMat();
			posts.get(Sect.Post.DWELLING).add(house.toWorld(mat[0], mat[1], mat[2]));
		}
		for (SectPiece hall : halls) {
			for (int[] mat : hall.meditationMats()) posts.get(Sect.Post.DWELLING).add(hall.toWorld(mat[0], 2, mat[1]));
		}
		// Where the inner disciples stroll: up the Spirit Way, round the plaza, in the gardens, a spot on every island, the library's top.
		List<BlockPos> courtyard = posts.get(Sect.Post.COURTYARD);
		for (int v : new int[] {GATE.v() - 4, GATE.v() + 8, GATE.v() + 20}) courtyard.add(at(frame, y, GATE, cu, v));
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 4 * k;
			courtyard.add(plaza.toWorld(r + (int) Math.round(Math.cos(angle) * 15), 1, r + (int) Math.round(Math.sin(angle) * 15)));
		}
		for (SectPiece garden : gardens) courtyard.add(garden.toWorld(garden.width() / 2, 1, 1));
		courtyard.addAll(spots);
		int[] top = library.pagodaSeat();
		courtyard.add(library.toWorld(top[0], top[1], top[2]));
		int[] in = gate.gateInside();
		posts.get(Sect.Post.GATE).add(gate.toWorld(in[0], in[1], in[2]));
		for (int[] post : altar.altarPosts()) posts.get(Sect.Post.ALTAR).add(altar.toWorld(post[0], post[1], post[2]));
		for (int[] post : training.trainingPosts()) posts.get(Sect.Post.TRAINING).add(training.toWorld(post[0], post[1], post[2]));
		// Points along the ways for walkers that can't go straight (Sect#waypoints): along every bridge every eight blocks, a spot on every island.
		List<BlockPos> wayPosts = posts.get(Sect.Post.WAY);
		for (SectPiece bridge : ways) {
			for (int z = 2; z < bridge.depth(); z += 8) wayPosts.add(bridge.toWorld(2, ImmortalSectPieces.deck(bridge, z) + 1, z));
		}
		wayPosts.addAll(spots);
		// The gate guards, on the forecourt before the Mountain Gate.
		List<BlockPos> guards = posts.get(Sect.Post.GATE_GUARD);
		guards.add(at(frame, y, GATE, cu - 3, gateLot.v0() - 2));
		guards.add(at(frame, y, GATE, cu + 3, gateLot.v0() - 2));

		// The barrier: from the core round every building and island, their tops counted (it is a sphere), and a little more.
		int radius = 0;
		for (SectPiece piece : pieces) {
			BoundingBox box = piece.getBoundingBox();
			for (int[] corner : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
				double dx = corner[0] - core.getX(), dz = corner[1] - core.getZ(), dy = Math.max(0, box.maxY() - core.getY());
				// An island's ellipse reaches its box's middles, not its corners.
				if (piece.kind() == SectPiece.Kind.PEAK_ISLAND) {
					dx *= 0.75;
					dz *= 0.75;
				}
				radius = Math.max(radius, (int) Math.ceil(Math.sqrt(dx * dx + dz * dz + dy * dy)) + 2);
			}
		}
		radius = Math.min(Formations.IMMORTAL_MAX_RADIUS, radius);

		CompoundTag blueprint = new CompoundTag();
		blueprint.putLong("Seed", seed);
		blueprint.putInt("Alignment", alignment);
		blueprint.putInt("Capacity", Math.min(ImmortalSects.MEMBERS, posts.get(Sect.Post.DWELLING).size() + 1));
		blueprint.putInt("Radius", radius);
		blueprint.putIntArray("Bounds", bounds(pieces));
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

	/** The sect's bounds for its blueprint ("Bounds"): round all its pieces, from well under its islands' rock to 64 over its highest roof. */
	private static int[] bounds(List<SectPiece> pieces) {
		int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
		for (SectPiece piece : pieces) {
			BoundingBox box = piece.getBoundingBox();
			x0 = Math.min(x0, box.minX());
			y0 = Math.min(y0, box.minY());
			z0 = Math.min(z0, box.minZ());
			x1 = Math.max(x1, box.maxX());
			y1 = Math.max(y1, box.maxY());
			z1 = Math.max(z1, box.maxZ());
		}
		return new int[] {x0, y0 - 40, z0, x1, y1 + 64, z1};
	}

	/** Where island {@code isle}'s rim lies on its {@code side} for a bridge five wide along the frame's v ({@code alongV}) or u at {@code across}: of its five columns' rims, the nearest its middle. */
	private static int rim(Isle isle, boolean alongV, int across, int side) {
		int a = alongV ? isle.rimV(across - 2, side) : isle.rimU(across - 2, side), b = alongV ? isle.rimV(across + 2, side) : isle.rimU(across + 2, side);
		return side > 0 ? Math.min(a, b) : Math.max(a, b);
	}

	/** Where a bridge leaving (or reaching) island {@code isle} on its {@code side} ends on it: {@link #LANDING} in from its rim. */
	static int landing(Isle isle, boolean alongV, int across, int side) {
		return rim(isle, alongV, across, side) - side * LANDING;
	}

	/**
	 * A bridge from island {@code from} to island {@code to} along the frame's v ({@code alongV}) or u at {@code across}, its deck from one's
	 * top to the other's, landing {@link #LANDING} onto each; its landings kept clear of the islands' flowers and trees.
	 */
	private static SectPiece bridge(Map<Isle, Ground> grounds, SectStructure.Frame frame, long seed, Alignment.Faction faction, int y, Isle from, Isle to,
			boolean alongV, int across) {
		int dir = alongV ? Integer.signum(to.v() - from.v()) : Integer.signum(to.u() - from.u());
		int start = landing(from, alongV, across, dir), end = landing(to, alongV, across, -dir);
		int lo = Math.min(start, end), hi = Math.max(start, end);
		SectLayout.Lot lot = alongV ? new SectLayout.Lot(across - 2, lo, across + 2, hi, dir > 0 ? SectLayout.Side.BACK : SectLayout.Side.FRONT)
				: new SectLayout.Lot(lo, across - 2, hi, across + 2, dir > 0 ? SectLayout.Side.RIGHT : SectLayout.Side.LEFT);
		int base = y + Math.min(from.rise(), to.rise()) - 4;
		CompoundTag data = new CompoundTag();
		data.putInt("From", y + from.rise() - base);
		data.putInt("To", y + to.rise() - base);
		data.putInt("In0", LANDING);
		data.putInt("In1", LANDING);
		for (Isle isle : new Isle[] {from, to}) keep(grounds, isle, lot.u0() - 1, lot.v0() - 1, lot.u1() + 1, lot.v1() + 1);
		return SectStructure.piece(SectPiece.Kind.PEAK_BRIDGE, lot, frame, base, Math.abs(from.rise() - to.rise()) + 10, seed, faction, true, data);
	}

	private ImmortalSectPlan() {}
}
