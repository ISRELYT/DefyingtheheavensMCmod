package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The great spells' wounds in the land, as a walking disaster leaves them (the user's ask, 2026-10-17): craters where meteors,
 * mountains and suns come down, the ground sinking under a seal's weight, fissures running out from a quake, a canyon where a
 * slash has passed, the land scorched under a sea of flames. Every block goes by HeavenSpells#carvable (nothing kept in it, nothing
 * harder than obsidian, nothing a barrier shelters, nothing its caster couldn't dig) and is gone for good, dropping nothing; a
 * few are flung off as debris. A big wound is taken a share at a time ({@link #BUDGET} blocks a tick for all of them together),
 * nearest its heart first, so no tick has to take it all.
 */
public final class Devastation {
	/** Blocks taken away at most in one tick, every wound together. */
	static final int BUDGET = 2500;
	/** Pieces flung off a wound, at most. */
	private static final int DEBRIS = 36;

	/** What the floor of a wound becomes once it is cut: left as it is, burnt (magma, blackstone, fires), broken (rubble), or a lake in it. */
	public enum Floor { NONE, SCORCHED, RUBBLE, LAKE }

	/** A wound being cut: its cells, nearest its heart first, how far through, and what follows. */
	private static final class Job {
		final ServerLevel level;
		final ServerPlayer caster;
		final List<BlockPos> cells;
		final Vec3 heart;
		final Runnable after;
		int next, flung;

		Job(ServerLevel level, ServerPlayer caster, List<BlockPos> cells, Vec3 heart, Runnable after) {
			this.level = level;
			this.caster = caster;
			this.cells = cells;
			this.heart = heart;
			this.after = after;
		}
	}

	private static final List<Job> JOBS = new ArrayList<>();

	// --- The wounds ---

	/**
	 * A crater {@code radius} round and {@code depth} deep at {@code at} (a bowl, ragged at its edge; what stood on the ground there
	 * blown away up to six tenths of the radius above it), its floor then made {@code floor}.
	 */
	public static void crater(ServerPlayer p, ServerLevel level, Vec3 at, double radius, double depth, Floor floor) {
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius), d = (int) Math.ceil(depth), up = (int) Math.ceil(radius * 0.6);
		List<BlockPos> cells = new ArrayList<>();
		Map<Long, Integer> lowest = new HashMap<>();
		long seed = centre.asLong();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double flat = (dx * dx + dz * dz) / (radius * radius);
				double rag = 1 + 0.18 * (noise(seed, dx, dz) - 0.5);
				if (flat > rag) continue;
				for (int dy = -d; dy <= up; dy++) {
					double v = dy < 0 ? (dy / depth) * (dy / depth) : (dy / (radius * 0.6)) * (dy / (radius * 0.6));
					if (flat + v > rag) continue;
					BlockPos cell = centre.offset(dx, dy, dz);
					cells.add(cell);
					lowest.merge(BlockPos.asLong(cell.getX(), 0, cell.getZ()), cell.getY(), Math::min);
				}
			}
		}
		queue(level, p, cells, at, () -> lay(p, level, lowest, floor, centre.getY()));
	}

	/**
	 * The ground {@code radius} round {@code at} sinking under a weight: up to {@code drop} blocks at the heart, less toward the
	 * edge; the new ground cracked (stone to cobblestone, earth to coarse dirt). Trees keep their footing, and so does whoever
	 * stands within {@code spare} of the heart (the caster).
	 */
	public static void subside(ServerPlayer p, ServerLevel level, Vec3 at, double radius, int drop, double spare) {
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		List<BlockPos> cells = new ArrayList<>();
		Map<Long, Integer> lowest = new HashMap<>();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz) / radius;
				if (dist > 1 || dist * radius < spare) continue;
				int n = (int) Math.round(drop * Math.sqrt(1 - dist) * (0.75 + 0.5 * noise(centre.asLong(), dx, dz)));
				if (n <= 0) continue;
				BlockPos top = groundTop(level, centre.getX() + dx, centre.getZ() + dz);
				if (top == null || level.getBlockState(top.above()).is(BlockTags.LOGS)) continue; // a tree keeps its footing
				for (int i = 0; i < n; i++) cells.add(top.below(i));
				lowest.put(BlockPos.asLong(top.getX(), 0, top.getZ()), top.getY() - n + 1);
			}
		}
		queue(level, p, cells, at, () -> lay(p, level, lowest, Floor.RUBBLE, centre.getY()));
	}

	/**
	 * {@code count} fissures running out from {@code from} blocks off {@code at} to {@code radius}, jagged, a block or two wide and
	 * {@code depth} deep (shallower toward their ends).
	 */
	public static void fissures(ServerPlayer p, ServerLevel level, Vec3 at, double radius, int count, int depth, double from) {
		BlockPos centre = BlockPos.containing(at);
		List<BlockPos> cells = new ArrayList<>();
		java.util.Set<Long> columns = new java.util.HashSet<>();
		for (int k = 0; k < count; k++) {
			double angle = Math.PI * 2 * k / count + (noise(centre.asLong(), k, 7) - 0.5) * 0.6;
			double wander = 0;
			for (double d = from; d <= radius; d += 0.7) {
				wander += (noise(centre.asLong() + k * 31L, (int) (d * 4), k) - 0.5) * 0.25;
				double a = angle + wander;
				int x = centre.getX() + (int) Math.round(Math.cos(a) * d), z = centre.getZ() + (int) Math.round(Math.sin(a) * d);
				int width = noise(centre.asLong(), x, z) < 0.35 ? 2 : 1;
				for (int w = 0; w < width; w++) {
					int cx = x + (w == 1 ? (int) Math.round(-Math.sin(a)) : 0), cz = z + (w == 1 ? (int) Math.round(Math.cos(a)) : 0);
					if (!columns.add(BlockPos.asLong(cx, 0, cz))) continue;
					BlockPos top = groundTop(level, cx, cz);
					if (top == null) continue;
					int deep = (int) Math.ceil(depth * (1 - 0.6 * d / radius));
					for (int i = 0; i < deep; i++) cells.add(top.below(i));
				}
			}
		}
		queue(level, p, cells, at, null);
	}

	/** A tunnel {@code radius} round bored from {@code from} to {@code to} through whatever stands between, nearest {@code from} first. */
	public static void bore(ServerPlayer p, ServerLevel level, Vec3 from, Vec3 to, double radius) {
		Vec3 line = to.subtract(from);
		double length = line.length();
		if (length < 1.0e-3) return;
		Vec3 unit = line.scale(1 / length);
		java.util.Set<BlockPos> cells = new java.util.LinkedHashSet<>();
		int r = (int) Math.ceil(radius);
		for (double t = 0; t <= length; t += 0.5) {
			BlockPos centre = BlockPos.containing(from.add(unit.scale(t)));
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						BlockPos cell = centre.offset(dx, dy, dz);
						Vec3 rel = Vec3.atCenterOf(cell).subtract(from);
						double along = rel.dot(unit);
						if (along < 0 || along > length || rel.subtract(unit.scale(along)).lengthSqr() > radius * radius) continue;
						cells.add(cell);
					}
				}
			}
		}
		queue(level, p, new ArrayList<>(cells), from, null);
	}

	/**
	 * The cross-section of a slash at column {@code x, z}, from {@code yMin} to {@code yMax}: cut away at once (the slash itself
	 * paces it, a column at a time). Returns how many blocks went.
	 */
	public static int cutColumn(ServerPlayer p, ServerLevel level, int x, int z, int yMin, int yMax, boolean debris) {
		int cut = 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int y = yMin; y <= yMax; y++) {
			pos.set(x, y, z);
			if (!level.isLoaded(pos)) return cut;
			BlockState state = level.getBlockState(pos);
			if (state.isAir() || !HeavenSpells.carvable(level, pos, p)) continue;
			level.removeBlock(pos, false);
			cut++;
			if (debris && level.random.nextInt(60) == 0) {
				SpellBlock.show(level, HeavenSpells.asEarth(state), Vec3.atBottomCenterOf(pos), new Vec3(level.random.nextGaussian() * 3, 2 + level.random.nextDouble() * 4,
						level.random.nextGaussian() * 3), 0, 16, SpellBlock.Motion.FLY, 0);
			}
		}
		return cut;
	}

	/**
	 * The land {@code radius} round {@code at} set ablaze: grass and earth scorched to coarse dirt, now and then a patch of magma or
	 * netherrack, and fires on {@code share} of it (on whatever stands highest there: a tree's crown catches too).
	 */
	public static void burn(ServerPlayer p, ServerLevel level, Vec3 at, double radius, double share) {
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				BlockPos ground = groundTop(level, x, z);
				if (ground != null && HeavenSpells.carvable(level, ground, p)) {
					BlockState was = level.getBlockState(ground);
					double roll = level.random.nextDouble();
					if (roll < 0.05) level.setBlock(ground, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
					else if (roll < 0.12) level.setBlock(ground, Blocks.NETHERRACK.defaultBlockState(), 3);
					else if (was.is(Blocks.GRASS_BLOCK) || was.is(Blocks.DIRT) || was.is(Blocks.PODZOL) || was.is(Blocks.MYCELIUM)) {
						level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), 3);
					}
				}
				if (level.random.nextDouble() < share) {
					BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z));
					ignite(p, level, top);
				}
			}
		}
	}

	// --- Underway ---

	private static void queue(ServerLevel level, ServerPlayer p, List<BlockPos> cells, Vec3 heart, Runnable after) {
		BlockPos centre = BlockPos.containing(heart);
		cells.sort(java.util.Comparator.comparingDouble(c -> c.distSqr(centre)));
		JOBS.add(new Job(level, p, cells, heart, after));
	}

	/** Every tick (HeavenSpells#tick): the wounds underway cut a share further, {@link #BUDGET} blocks in all. */
	public static void tick(MinecraftServer server) {
		if (JOBS.isEmpty()) return;
		int budget = BUDGET;
		for (Iterator<Job> it = JOBS.iterator(); it.hasNext() && budget > 0; ) {
			Job job = it.next();
			while (job.next < job.cells.size() && budget > 0) {
				BlockPos pos = job.cells.get(job.next++);
				budget--;
				if (!job.level.isLoaded(pos)) continue;
				BlockState state = job.level.getBlockState(pos);
				if (state.isAir() || !HeavenSpells.carvable(job.level, pos, job.caster)) continue;
				job.level.removeBlock(pos, false);
				if (job.flung < DEBRIS && job.level.random.nextInt(Math.max(8, job.cells.size() / (DEBRIS * 2))) == 0) {
					job.flung++;
					Vec3 from = Vec3.atBottomCenterOf(pos);
					Vec3 out = from.subtract(job.heart).multiply(1, 0, 1);
					out = out.lengthSqr() < 1.0e-4 ? new Vec3(job.level.random.nextGaussian(), 0, job.level.random.nextGaussian()) : out.normalize();
					double far = 4 + job.level.random.nextDouble() * 8;
					SpellBlock.show(job.level, HeavenSpells.asEarth(state), from.add(0, 0.5, 0), out.scale(far).add(0, 1 + job.level.random.nextDouble() * 3, 0), 0,
							14 + job.level.random.nextInt(10), SpellBlock.Motion.FLY, 0);
				}
			}
			if (job.next >= job.cells.size()) {
				it.remove();
				if (job.after != null) job.after.run();
			}
		}
	}

	/** Server stopping: whatever is underway stops where it is. */
	public static void clear() {
		JOBS.clear();
	}

	/** Blocks still to be cut, all wounds together (tests). */
	public static int pending() {
		int left = 0;
		for (Job job : JOBS) left += job.cells.size() - job.next;
		return left;
	}

	// --- Helpers ---

	/**
	 * A wound's floor laid: the block under each column's deepest cut made {@code floor}'s (and fires on a burnt one); a lake's
	 * water set in the cut itself, up to two below the ground's level ({@code surfaceY}) so it stays in its basin.
	 */
	private static void lay(ServerPlayer p, ServerLevel level, Map<Long, Integer> lowest, Floor floor, int surfaceY) {
		if (floor == Floor.NONE) return;
		for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
			BlockPos below = new BlockPos(BlockPos.getX(e.getKey()), e.getValue() - 1, BlockPos.getZ(e.getKey()));
			if (!level.isLoaded(below) || !level.getBlockState(below.above()).isAir()) continue;
			if (floor == Floor.LAKE) {
				for (BlockPos water = below.above(); water.getY() <= surfaceY - 2 && water.getY() <= below.getY() + 3; water = water.above()) {
					if (!level.getBlockState(water).isAir() || !mayChange(p, level, water)) break;
					level.setBlock(water, Blocks.WATER.defaultBlockState(), 3);
				}
				continue;
			}
			if (!HeavenSpells.carvable(level, below, p)) continue;
			double roll = level.random.nextDouble();
			if (floor == Floor.SCORCHED) {
				if (roll < 0.22) level.setBlock(below, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
				else if (roll < 0.45) level.setBlock(below, Blocks.BLACKSTONE.defaultBlockState(), 3);
				else if (roll < 0.6) level.setBlock(below, Blocks.NETHERRACK.defaultBlockState(), 3);
				if (level.random.nextDouble() < 0.18) ignite(p, level, below.above());
			} else {
				if (roll < 0.3) level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
				else if (roll < 0.45) level.setBlock(below, Blocks.GRAVEL.defaultBlockState(), 3);
				else if (roll < 0.6) level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
			}
		}
	}

	/** A fire set at {@code pos} if one can burn there and {@code p} may change it. */
	static void ignite(ServerPlayer p, ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) return;
		if (!mayChange(p, level, pos)) return;
		level.setBlock(pos, BaseFireBlock.getState(level, pos), 11);
	}

	/** Whether a spell of {@code p}'s may set something at {@code pos}: not under a barrier's shelter, ground they may change. */
	private static boolean mayChange(ServerPlayer p, ServerLevel level, BlockPos pos) {
		return !Formations.shelters(level, pos) && (p == null || !Formations.protectsGround(p, pos) && p.mayBuild() && level.mayInteract(p, pos));
	}

	/** The topmost ground block of column {@code x, z} (below plants, trunks and leaves), or null if its land isn't loaded. */
	static BlockPos groundTop(ServerLevel level, int x, int z) {
		BlockPos probe = new BlockPos(x, 0, z);
		if (!level.isLoaded(probe)) return null;
		BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe).below();
		for (int i = 0; i < 40 && pos.getY() > level.getMinBuildHeight(); i++) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES) && !state.canBeReplaced()) return pos;
			pos = pos.below();
		}
		return null;
	}

	/** A number from 0 to 1 the same every time for the same seed and place (ragged edges, wandering cracks). */
	private static double noise(long seed, int a, int b) {
		long h = seed * 0x9E3779B97F4A7C15L + a * 0xBF58476D1CE4E5B9L + b * 0x94D049BB133111EBL;
		h ^= h >>> 31;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 29;
		return (h & 0xFFFFFF) / (double) 0xFFFFFF;
	}

	private Devastation() {}
}
