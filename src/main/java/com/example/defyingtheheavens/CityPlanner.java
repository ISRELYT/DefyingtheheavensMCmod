package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntBinaryOperator;

/**
 * Lays out an immortal city on the land as it lies (the Jade Capital, ImmortalCapital, and the realm's other cities, ImmortalCityStructure;
 * the user's ask, 2026-10-25: the cities "dont need to be on perfectly flat ground", the way Minecraft's villages and Terralith's are not):
 * every building, gate, tower and stretch of wall at the level of the ground under its own lot ({@link #level}), each box kept
 * ({@link #beards}) for the land to be shaped round as it is generated (vanilla's Beardifier, as round a structure's {@code beard_thin}
 * pieces), the ways left to its ground plan to be laid on the ground where it is (CityPieces#lay).
 */
final class CityPlanner {
	/** A house or a shop is left out where the ground under its lot rises and falls more than this; the great buildings stand anyway. */
	static final int MAX_RELIEF = 10;
	/** The city wall is laid in lengths of this, each at its own level, stepping with the ground. */
	static final int SEGMENT = 16;

	/** The ground's top block in a column, as the generator makes it before any building's land is shaped (vanilla sets its pieces by the same). */
	@FunctionalInterface
	interface Ground {
		int at(int x, int z);
	}

	/** The generator's ground, each column asked once. */
	static Ground ground(ChunkGenerator generator, LevelHeightAccessor heights, RandomState state) {
		Long2IntOpenHashMap seen = new Long2IntOpenHashMap();
		return (x, z) -> seen.computeIfAbsent(ChunkPos.asLong(x, z),
				key -> generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, state));
	}

	final SectStructure.Frame frame;
	final SectLayout layout;
	final Ground ground;
	final long seed;
	final Alignment.Faction faction;
	/** What is drawn, in order (its grounds go first, by whoever draws them), and the boxes the land is shaped round. */
	final List<SectPiece> pieces = new ArrayList<>();
	final List<BoundingBox> beards = new ArrayList<>();
	/** Where the frame's walls stand off the ground (the Court's pinnacle at the capital): a column (x, z) there is left out of a wall's level. */
	private final IntBinaryOperator offWall;

	CityPlanner(SectStructure.Frame frame, SectLayout layout, Ground ground, long seed, Alignment.Faction faction, IntBinaryOperator offWall) {
		this.frame = frame;
		this.layout = layout;
		this.ground = ground;
		this.seed = seed;
		this.faction = faction;
		this.offWall = offWall;
	}

	// --- Levels ---

	/** The median of the ground's top at nine columns {@code step} apart about frame point (u, v). */
	int median(int u, int v, int step) {
		List<Integer> heights = new ArrayList<>();
		for (int du = -step; du <= step; du += step) {
			for (int dv = -step; dv <= step; dv += step) {
				BlockPos at = frame.world(u + du, v + dv);
				heights.add(ground.at(at.getX(), at.getZ()));
			}
		}
		Collections.sort(heights);
		return heights.get(heights.size() / 2);
	}

	/** The ground's top under a lot, lowest first: at its corners, the middles of its sides and its middle. */
	int[] heights(SectLayout.Lot lot) {
		int[] us = {lot.u0(), (lot.u0() + lot.u1()) / 2, lot.u1()}, vs = {lot.v0(), (lot.v0() + lot.v1()) / 2, lot.v1()};
		int[] heights = new int[9];
		int n = 0;
		for (int u : us) {
			for (int v : vs) {
				BlockPos at = frame.world(u, v);
				heights[n++] = ground.at(at.getX(), at.getZ());
			}
		}
		Arrays.sort(heights);
		return heights;
	}

	/** Of a lot's heights, those this far off their median are taken for a gap or a crag (an island's edge, a spire), not its ground. */
	private static final int OUTLIER = 16;

	/**
	 * The level a building is laid at over the ground's {@code sorted} heights under it: near the highest, so little of it is sunk into a
	 * slope; its foundation and the land drawn up under it make up the rest (a hall stands up on its plinth on the downhill side). Heights
	 * far off the rest ({@link #OUTLIER}: a column over an island's edge, a crag) are passed over.
	 */
	static int level(int[] sorted) {
		int median = sorted[sorted.length / 2], lo = median, hi = median;
		for (int h : sorted) {
			if (Math.abs(h - median) > OUTLIER) continue;
			lo = Math.min(lo, h);
			hi = Math.max(hi, h);
		}
		return hi - (hi - lo) / 4;
	}

	int level(SectLayout.Lot lot) {
		return level(heights(lot));
	}

	static boolean steep(int[] sorted) {
		return sorted[sorted.length - 1] - sorted[0] > MAX_RELIEF;
	}

	/** A post on the grounds at frame point (u, v): on the ground there, one over its top block. */
	BlockPos onGround(int u, int v) {
		BlockPos at = frame.world(u, v);
		return new BlockPos(at.getX(), ground.at(at.getX(), at.getZ()) + 1, at.getZ());
	}

	/** Ground that is level everywhere at {@code y} (the game tests lay sects out with no land to read). */
	static Ground flat(int y) {
		return (x, z) -> y;
	}

	// --- Laying out ---

	static SectLayout.Lot lot(int u, int v, int width, int depth, SectLayout.Side side) {
		return SectLayout.Lot.centred(u, v, width, depth, side);
	}

	/** A building on {@code lot} at level {@code y}, the land shaped round its box. */
	SectPiece add(SectPiece.Kind kind, SectLayout.Lot lot, int y, int height, long salt, CompoundTag extra) {
		SectPiece piece = SectStructure.piece(kind, lot, frame, y, height, seed + salt, faction, true, extra);
		pieces.add(piece);
		beards.add(piece.getBoundingBox());
		return piece;
	}

	/** A building on {@code lot} at its ground's level. */
	SectPiece add(SectPiece.Kind kind, SectLayout.Lot lot, int height, long salt, CompoundTag extra) {
		return add(kind, lot, level(lot), height, salt, extra);
	}

	/** Whether every column of {@code lot} lies on the grounds and isn't taken. */
	boolean free(SectLayout.Lot lot) {
		for (int u = lot.u0(); u <= lot.u1(); u++) {
			for (int v = lot.v0(); v <= lot.v1(); v++) {
				if (!layout.isOpen(u, v)) return false;
			}
		}
		return true;
	}

	static CompoundTag tiers(int tiers) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Tiers", tiers);
		return tag;
	}

	/**
	 * A stretch of the city wall on {@code stretch}, in lengths of {@link #SEGMENT} each at the ground's level along it (eased toward its
	 * neighbours', so the wall steps gently), none where it stands wholly off the ground ({@link #offWall}).
	 */
	void wall(SectLayout.Lot stretch, int height) {
		if (stretch.u1() < stretch.u0() || stretch.v1() < stretch.v0()) return;
		layout.claim(stretch, 1);
		boolean alongU = stretch.side() == SectLayout.Side.BACK || stretch.side() == SectLayout.Side.FRONT;
		int length = alongU ? stretch.u1() - stretch.u0() + 1 : stretch.v1() - stretch.v0() + 1;
		int count = (length + SEGMENT - 1) / SEGMENT;
		SectLayout.Lot[] lots = new SectLayout.Lot[count];
		int[] raw = new int[count];
		for (int i = 0; i < count; i++) {
			int a = i * SEGMENT, b = Math.min(length - 1, a + SEGMENT - 1);
			lots[i] = alongU ? new SectLayout.Lot(stretch.u0() + a, stretch.v0(), stretch.u0() + b, stretch.v1(), stretch.side())
					: new SectLayout.Lot(stretch.u0(), stretch.v0() + a, stretch.u1(), stretch.v0() + b, stretch.side());
			raw[i] = wallLevel(lots[i], alongU);
		}
		for (int i = 0; i < count; i++) {
			if (raw[i] == Integer.MIN_VALUE) continue;
			int sum = 2 * raw[i], n = 2;
			if (i > 0 && raw[i - 1] != Integer.MIN_VALUE) {
				sum += raw[i - 1];
				n++;
			}
			if (i < count - 1 && raw[i + 1] != Integer.MIN_VALUE) {
				sum += raw[i + 1];
				n++;
			}
			add(SectPiece.Kind.CITY_WALL, lots[i], Math.round((float) sum / n), height, lots[i].u0() * 13L + lots[i].v0(), null);
		}
	}

	/** The level a length of wall is laid at, from the ground along its middle off {@link #offWall} (Integer.MIN_VALUE if all of it is). */
	private int wallLevel(SectLayout.Lot lot, boolean alongU) {
		int mu = (lot.u0() + lot.u1()) / 2, mv = (lot.v0() + lot.v1()) / 2;
		int[][] points = alongU ? new int[][] {{lot.u0(), mv}, {mu, mv}, {lot.u1(), mv}} : new int[][] {{mu, lot.v0()}, {mu, mv}, {mu, lot.v1()}};
		List<Integer> heights = new ArrayList<>();
		for (int[] point : points) {
			BlockPos at = frame.world(point[0], point[1]);
			if (offWall == null || offWall.applyAsInt(at.getX(), at.getZ()) == 0) heights.add(ground.at(at.getX(), at.getZ()));
		}
		if (heights.isEmpty()) return Integer.MIN_VALUE;
		return level(heights.stream().mapToInt(Integer::intValue).sorted().toArray());
	}

	/** A shop at (u, v), its front toward {@code side}'s door: if its lot is free and its ground not too steep. */
	void shop(int u, int v, SectLayout.Side side) {
		SectLayout.Lot lot = lot(u, v, 11, 9, side);
		if (!free(lot)) return;
		int[] heights = heights(lot);
		if (steep(heights)) return;
		layout.claim(lot, 0);
		add(SectPiece.Kind.CITY_SHOP, lot, level(heights), 16, 2000 + u * 401L + v, null);
	}

	/** A building on its own lot about (u, v) extending {@code side} from its door, if the lot is free, a footpath from its door to the ways. */
	SectPiece place(SectPiece.Kind kind, int u, int v, int width, int depth, SectLayout.Side side, int height, CompoundTag extra, int salt) {
		SectLayout.Lot lot = lot(u, v, width, depth, side);
		if (!free(lot)) return null;
		layout.claim(lot, 2);
		layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
		return add(kind, lot, height, salt * 977L, extra);
	}

	/** A building and its mirror across the frame's middle (u), {@code u} in from its left side, both facing the middle. */
	void pair(SectPiece.Kind kind, int u, int v, int width, int depth, int height, CompoundTag extra, int salt) {
		for (int k = -1; k <= 1; k += 2) {
			SectLayout.Lot lot = lot(k < 0 ? u : layout.w - 1 - u, v, width, depth, k < 0 ? SectLayout.Side.LEFT : SectLayout.Side.RIGHT);
			if (!free(lot)) continue;
			layout.claim(lot, 2);
			layout.footpath(lot.doorstep()[0], lot.doorstep()[1]);
			add(kind, lot, height, salt * 977L + k, extra == null ? null : extra.copy());
		}
	}

	/** A great family's courtyard house at (u, v): its walled court, its house in the middle facing the court's gate, both at the court's level. */
	void courtyard(int u, int v) {
		SectLayout.Lot court = lot(u, v, 23, 23, SectLayout.Side.BACK);
		if (!free(court)) return;
		int y = level(court);
		// Its walls only (its house within is claimed apart, and a footpath finds the gate from the house's door); its front wall stands a
		// row in (HolyLandPieces#court).
		int wallV = court.v0() + 1;
		layout.claim(new SectLayout.Lot(court.u0(), wallV, court.u0(), court.v1(), SectLayout.Side.BACK), 0);
		layout.claim(new SectLayout.Lot(court.u1(), wallV, court.u1(), court.v1(), SectLayout.Side.BACK), 0);
		layout.claim(new SectLayout.Lot(court.u0(), court.v1(), court.u1(), court.v1(), SectLayout.Side.BACK), 0);
		layout.claim(new SectLayout.Lot(court.u0(), wallV, u - 2, wallV, SectLayout.Side.BACK), 0);
		layout.claim(new SectLayout.Lot(u + 2, wallV, court.u1(), wallV, SectLayout.Side.BACK), 0);
		add(SectPiece.Kind.HOLY_COURT, court, y, 8, 400 + u * 31L + v, null);
		SectLayout.Lot house = lot(u, v + 2, 11, 11, SectLayout.Side.BACK);
		layout.claim(house, 0);
		layout.footpath(house.doorstep()[0], house.doorstep()[1]);
		layout.footpath(u, court.v0());
		add(SectPiece.Kind.ELDER_HOUSE, house, y, 18, 100 + u * 31L + v, null);
	}

	/** The many houses of the city between its buildings, each on its own ground (none on a slope too steep for it). */
	void houses(RandomSource random, int count, int large) {
		for (SectLayout.Lot house : layout.houses(random, count, large)) {
			int[] heights = heights(house);
			if (steep(heights)) continue;
			add(house.u1() - house.u0() >= 8 ? SectPiece.Kind.ELDER_HOUSE : SectPiece.Kind.DWELLING, house, level(heights), 14,
					1000 + house.u0() * 401L + house.v0(), null);
		}
	}
}
