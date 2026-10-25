package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The great spells' wounds in the land, as a walking disaster leaves them (the user's ask, 2026-10-17): craters where meteors,
 * mountains and suns come down, the ground sinking under a seal's weight, fissures running out from a quake, a canyon where a
 * slash has passed, the land scorched under a sea of flames. Every block goes by HeavenSpells#carvable (nothing kept in it, nothing
 * harder than obsidian, nothing a barrier shelters, nothing its caster couldn't dig; a burst golden core's takes water, lava and
 * what holds things too, {@link #obliterate}) and is gone for good, dropping nothing; a
 * few are flung off as debris. A big wound is taken a share at a time ({@link #BUDGET} blocks a tick for all of them together),
 * nearest its heart first, so no tick has to take it all. Its cells are kept packed ({@link BlockPos#asLong}), and its land kept
 * loaded while it's cut ({@link #KEEP_LOADED}), so even a True Immortal's crater (CoreDetonation, 256 blocks across) is cut whole.
 */
public final class Devastation {
	/** Blocks taken away at most in one tick, every wound together. */
	static final int BUDGET = 2500;
	/** Pieces flung off a wound, at most (a slash's swath is many small wounds, a few each). */
	private static final int DEBRIS = 36, SHEAR_DEBRIS = 4;
	/**
	 * A wound reaching further than {@link #KEEP_FROM} blocks from its heart keeps its land loaded till it's cut (its caster may be
	 * gone: a burst core's is dead and may wake far off), out to {@link #KEEP_MOST} chunks round, the word renewed every
	 * {@link #KEEP_EVERY} ticks and lapsing {@link #KEEP_TICKS} after the last (never saved with the world).
	 */
	private static final double KEEP_FROM = 40;
	private static final int KEEP_MOST = 10, KEEP_EVERY = 20, KEEP_TICKS = 100;
	private static final TicketType<ChunkPos> KEEP_LOADED = TicketType.create("defying_the_heavens_devastation", Comparator.comparingLong(ChunkPos::toLong), KEEP_TICKS);

	/**
	 * A wound's floor: as it was cut, burnt to magma and blackstone, broken rubble, a lake, melted to obsidian glass (a white flame's), or
	 * frozen to ice and snow (a frozen moon's, 2026-10-24).
	 */
	public enum Floor { NONE, SCORCHED, RUBBLE, LAKE, GLASSY, ICE }

	/**
	 * A wound being cut: its cells (packed), nearest its heart first, how far through, what follows, and its land kept loaded. Or, from
	 * Sacred Vessel on (2026-10-24), land laid down rather than taken away ({@link #place}: a mound of Xirang, the soil that grows of itself),
	 * or the water drawn out of it ({@link #drain}: an ocean sealed in a vase).
	 */
	private static final class Job {
		final ServerLevel level;
		final LivingEntity caster;
		final long[] cells;
		final Vec3 heart;
		final Runnable after;
		final int debris;
		/** Its heart's chunk, and how many chunks round its land is kept loaded (0: not). */
		final ChunkPos keepAt;
		final int keep;
		/** Taken as a burst golden core takes the land: water, lava and what holds things too (HeavenSpells#carvable, {@link #obliterate}). */
		final boolean utter;
		/** What each cell is filled with (block state ids, as its cells are ordered), where land is laid down; null where it is cut. */
		final int[] place;
		/** Only the water is taken out of its cells (and out of what stands in water), nothing else. */
		final boolean drain;
		int next, flung, age;
		/**
		 * At most this many of its cells a tick, whatever the budget left (0: no more than the budget), and how many this tick so far. A
		 * lake's water: set all in one tick, thousands of sources at once, it held the game up (Kun Rises from the Northern Abyss: the
		 * user's report, 2026-10-25, "when I use the big water move for the whale fish thing the game lags out").
		 */
		int rate, thisTick;

		Job(ServerLevel level, LivingEntity caster, long[] cells, Vec3 heart, Runnable after, int debris, int keep, boolean utter) {
			this(level, caster, cells, heart, after, debris, keep, utter, null, false);
		}

		Job(ServerLevel level, LivingEntity caster, long[] cells, Vec3 heart, Runnable after, int debris, int keep, boolean utter, int[] place, boolean drain) {
			this.level = level;
			this.caster = caster;
			this.cells = cells;
			this.heart = heart;
			this.after = after;
			this.debris = debris;
			this.keepAt = new ChunkPos(BlockPos.containing(heart));
			this.keep = keep;
			this.utter = utter;
			this.place = place;
			this.drain = drain;
		}
	}

	private static final List<Job> JOBS = new ArrayList<>();

	// --- How great ---

	/**
	 * How great a spell's wounds in the land are by the realm it is of (the user's ask, 2026-10-23: as a golden core's blast grows by
	 * the realm, CoreDetonation#radius, so do the great spells' wounds): its realm's blast against Four Axis's, Core Formation 0.34,
	 * Nascent Soul 0.56, Heavenly Being 0.78, Four Axis 1. Heaven-Severing, Four Axis's, is the yardstick the rest are sized by. Only the
	 * spells meant to lay the land waste go by it; the small carvings of the realms below (a cave, a cleave's slot) stay as they are.
	 */
	public static double scale(Realm realm) {
		return CoreDetonation.radius(realm, Stage.EARLY) / CoreDetonation.radius(Realm.FOUR_AXIS, Stage.EARLY);
	}

	/** A wound {@code atFourAxis} blocks great (round, long, deep) as a spell of Four Axis would cut it, cut by one of {@code realm} ({@link #scale}). */
	public static double sized(Realm realm, double atFourAxis) {
		return atFourAxis * scale(realm);
	}

	// --- The wounds ---

	/**
	 * A crater {@code radius} round and {@code depth} deep at {@code at} (a bowl, ragged at its edge; what stood on the ground there
	 * blown away up to six tenths of the radius above it), its floor then made {@code floor}.
	 */
	public static void crater(LivingEntity p, ServerLevel level, Vec3 at, double radius, double depth, Floor floor) {
		crater(p, level, at, radius, depth, floor, false);
	}

	/**
	 * As {@link #crater(ServerPlayer, ServerLevel, Vec3, double, double, Floor)}, or, {@code utter}, everything in it taken (a burst
	 * golden core's, CoreDetonation): water and lava, lecterns, pedestals and whatever else holds something, besides the ground.
	 */
	public static void crater(LivingEntity p, ServerLevel level, Vec3 at, double radius, double depth, Floor floor, boolean utter) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		// A burnt floor is its caster's own fire (HeavenPassives#ownFire: one who has mastered fire walks it unburnt).
		if (floor == Floor.SCORCHED || floor == Floor.GLASSY) HeavenPassives.ownFire(p, level, at, radius + 2);
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius), d = (int) Math.ceil(depth), up = (int) Math.ceil(radius * 0.6);
		LongArrayList cells = new LongArrayList();
		Map<Long, Integer> lowest = new HashMap<>();
		long seed = centre.asLong();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double flat = (dx * dx + dz * dz) / (radius * radius);
				double rag = 1 + 0.18 * (noise(seed, dx, dz) - 0.5);
				if (flat > rag) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				// Nothing above what stands highest there: only air (a great crater's dome of it, millions of cells, cost its budget).
				int top = up;
				if (level.hasChunk(x >> 4, z >> 4)) top = Math.min(up, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1 - centre.getY());
				for (int dy = -d; dy <= top; dy++) {
					double v = dy < 0 ? (dy / depth) * (dy / depth) : (dy / (radius * 0.6)) * (dy / (radius * 0.6));
					if (flat + v > rag) continue;
					int y = centre.getY() + dy;
					cells.add(BlockPos.asLong(x, y, z));
					lowest.merge(BlockPos.asLong(x, 0, z), y, Math::min);
				}
			}
		}
		queue(level, p, cells.toLongArray(), at, () -> lay(p, level, lowest, floor, centre.getY()), DEBRIS, utter);
	}

	/**
	 * A flood's basin, a ring of it at a time (Weak Water from the Void, Hundred Rivers Converge; the user's ask, 2026-10-24: the water
	 * "should come out during it not after, and should cover the area the spells cover not just the center"): the bowl {@code radius}
	 * round and {@code depth} deep at its heart at {@code at}, its columns between {@code from} and {@code to} out from the heart cut
	 * (from the bowl's floor up to the land's top) and, as soon as they are, filled with water up to a block under the land's level at
	 * the heart: called again and again with the ring moving, the water spreads out over the whole of it (or pours in from its rim). A
	 * rise of the land more than {@link #FLOOD_HILL} over the heart is left standing out of it; a hollow under it is filled no deeper
	 * than the bowl's floor.
	 */
	public static void flood(LivingEntity p, ServerLevel level, Vec3 at, double radius, double depth, double from, double to) {
		if (!HeavenSpells.carvesLand(p) || to <= from) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(Math.min(to, radius * 1.1));
		LongArrayList cells = new LongArrayList();
		Map<Long, Integer> lowest = new HashMap<>();
		long seed = centre.asLong();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist < from || dist >= to) continue;
				double flat = dist * dist / (radius * radius), rag = 1 + 0.12 * (noise(seed, dx, dz) - 0.5);
				if (flat > rag) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				if (!level.hasChunk(x >> 4, z >> 4)) continue;
				BlockPos ground = groundTop(level, x, z);
				if (ground == null || ground.getY() > centre.getY() + FLOOD_HILL) continue; // a hill stands out of the flood
				int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1; // what stands on it swept away with it
				int bowl = centre.getY() - (int) Math.ceil(depth * Math.sqrt(Math.max(0, 1 - flat / rag)));
				for (int y = bowl; y <= Math.max(surface, centre.getY()); y++) cells.add(BlockPos.asLong(x, y, z));
				lowest.put(BlockPos.asLong(x, 0, z), bowl);
			}
		}
		queue(level, p, cells.toLongArray(), at, () -> fill(p, level, lowest, centre.getY() - 1), DEBRIS / 3, false);
	}

	/** How far over a flood's heart the land may rise and still be flooded (Devastation#flood): higher, it stands out of the water. */
	static final int FLOOD_HILL = 3;

	/** Water set in each column's open cells, from its floor ({@code lowest}) up to {@code waterY}. */
	private static void fill(LivingEntity p, ServerLevel level, Map<Long, Integer> lowest, int waterY) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
			int x = BlockPos.getX(e.getKey()), z = BlockPos.getZ(e.getKey());
			for (int y = e.getValue(); y <= waterY; y++) {
				pos.set(x, y, z);
				if (!level.isLoaded(pos)) break;
				BlockState state = level.getBlockState(pos);
				if (!state.isAir() && !state.canBeReplaced() || !state.getFluidState().isEmpty() && state.getFluidState().isSource()) continue;
				if (!mayChange(p, level, pos)) continue;
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
			}
		}
	}

	/**
	 * The ground {@code radius} round {@code at} sinking under a weight: up to {@code drop} blocks at the heart, less toward the
	 * edge; the new ground cracked (stone to cobblestone, earth to coarse dirt). Trees keep their footing, and so does whoever
	 * stands within {@code spare} of the heart (the caster).
	 */
	public static void subside(LivingEntity p, ServerLevel level, Vec3 at, double radius, int drop, double spare) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
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
	public static void fissures(LivingEntity p, ServerLevel level, Vec3 at, double radius, int count, int depth, double from) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
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

	/**
	 * {@code cells} a spell has shaped itself (Void-Severing Sword Light's chasm along its line: MetalSpells.Severing), cut like any wound,
	 * nearest {@code heart} first, a few pieces flung.
	 */
	public static void cut(LivingEntity p, ServerLevel level, List<BlockPos> cells, Vec3 heart) {
		cut(p, level, cells, heart, DEBRIS);
	}

	/**
	 * {@link #cut(LivingEntity, ServerLevel, List, Vec3)}, flinging off at most {@code debris} pieces: for a wound cut a slice a tick (a sweep's,
	 * 2026-10-24: the Bodhi Branch flung 36 every tick of its stroke, seven hundred pieces of land in the air at once).
	 */
	public static void cut(LivingEntity p, ServerLevel level, List<BlockPos> cells, Vec3 heart, int debris) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		queue(level, p, cells, heart, null, debris);
	}

	/**
	 * A gouge raked through the land from {@code from} to {@code to} (a claw's: the Heaven-Demon Avatar's), {@code width} across and
	 * {@code depth} deep, following the land's top: deepest midway along it and down its middle, shallowing to nothing at its ends and edges.
	 */
	public static void gouge(LivingEntity p, ServerLevel level, Vec3 from, Vec3 to, double width, double depth) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		Vec3 line = to.subtract(from).multiply(1, 0, 1);
		double length = line.length();
		if (length < 1.0e-3) return;
		Vec3 along = line.scale(1 / length), across = new Vec3(-along.z, 0, along.x);
		java.util.Map<Long, Integer> deepest = new java.util.LinkedHashMap<>();
		for (double t = 0; t <= length; t += 0.5) {
			double lengthwise = Math.pow(Math.sin(Math.PI * t / length), 0.6);
			for (double off = -width / 2; off <= width / 2; off += 0.5) {
				double crosswise = 1 - Math.pow(off / (width / 2 + 0.25), 2);
				int deep = (int) Math.round(depth * lengthwise * crosswise);
				if (deep <= 0) continue;
				Vec3 at = from.add(along.scale(t)).add(across.scale(off));
				deepest.merge(BlockPos.asLong((int) Math.floor(at.x), 0, (int) Math.floor(at.z)), deep, Math::max);
			}
		}
		List<BlockPos> cells = new ArrayList<>();
		for (Map.Entry<Long, Integer> e : deepest.entrySet()) {
			BlockPos top = groundTop(level, BlockPos.getX(e.getKey()), BlockPos.getZ(e.getKey()));
			if (top == null) continue;
			// What stands on it goes with it (a tree's trunk, a wall), and the ground down to the gouge's floor.
			int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()) - 1;
			for (int y = surface; y > top.getY() - e.getValue(); y--) cells.add(new BlockPos(top.getX(), y, top.getZ()));
		}
		queue(level, p, cells, from.add(to).scale(0.5), null);
	}

	/**
	 * A shaft burnt straight up from {@code base} to the open sky (One Flame Burns the Heavens; the user's ask, 2026-10-24: "using it
	 * underground should create a tunnel to the surface ridden with fire"): {@code radius} round at its foot, wider toward the top, ragged,
	 * through whatever stands over it up to the highest land within it (no more than {@code mostUp}); once it is cut its walls are burnt
	 * ({@link #scorchShaft}): magma, netherrack and blackstone, and fire on every ledge.
	 */
	public static void shaft(LivingEntity p, ServerLevel level, Vec3 base, double radius, int mostUp) {
		if (HeavenSpells.carvesLand(p)) HeavenPassives.ownFire(p, level, base, radius + 3); // its burnt walls its caster's own fire
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		BlockPos centre = BlockPos.containing(base);
		int r = (int) Math.ceil(radius * 1.35);
		int top = centre.getY();
		for (int dx = -r; dx <= r; dx += 2) {
			for (int dz = -r; dz <= r; dz += 2) {
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				if (level.hasChunk(x >> 4, z >> 4)) top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
			}
		}
		top = Math.min(Math.min(top + 2, centre.getY() + mostUp), level.getMaxBuildHeight() - 1);
		LongArrayList cells = new LongArrayList();
		long seed = centre.asLong() * 31;
		for (int y = centre.getY(); y <= top; y++) {
			double flare = shaftWidth(radius, y - centre.getY(), top - centre.getY());
			int fr = (int) Math.ceil(flare * 1.15);
			for (int dx = -fr; dx <= fr; dx++) {
				for (int dz = -fr; dz <= fr; dz++) {
					double rag = 1 + 0.25 * (noise(seed + y / 3, dx, dz) - 0.5);
					if (dx * dx + dz * dz > flare * flare * rag * rag) continue;
					cells.add(BlockPos.asLong(centre.getX() + dx, y, centre.getZ() + dz));
				}
			}
		}
		int height = top - centre.getY();
		queue(level, p, cells.toLongArray(), base, () -> scorchShaft(p, level, centre, radius, height), DEBRIS, false);
	}

	/** How wide a shaft ({@link #shaft}) is {@code up} blocks over its foot, of {@code height}: its {@code radius} there, a third more at its top. */
	private static double shaftWidth(double radius, int up, int height) {
		return radius * (0.9 + 0.35 * up / (double) Math.max(1, height));
	}

	/** A burnt shaft's walls ({@link #shaft}): the rock round it burnt to magma, netherrack and blackstone, fires set on its ledges and its burning ground. */
	private static void scorchShaft(LivingEntity p, ServerLevel level, BlockPos centre, double radius, int height) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int up = -1; up <= height; up++) {
			double flare = shaftWidth(radius, Math.max(0, up), height);
			// The band its ragged edge can lie in (its cut ±1/8 of its width), and the floor's ring under it.
			double inner = up < 0 ? 0 : flare * 0.85, outerEdge = flare * 1.15 + 1.5;
			int outer = (int) Math.ceil(outerEdge);
			for (int dx = -outer; dx <= outer; dx++) {
				for (int dz = -outer; dz <= outer; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d < inner || d > outerEdge) continue;
					pos.set(centre.getX() + dx, centre.getY() + up, centre.getZ() + dz);
					if (!level.isLoaded(pos)) continue;
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || !state.isSolid()) continue;
					// Only the wall's face: a block with the shaft's air beside or above it.
					boolean face = level.getBlockState(pos.above()).isAir() || level.getBlockState(pos.north()).isAir() || level.getBlockState(pos.south()).isAir()
							|| level.getBlockState(pos.east()).isAir() || level.getBlockState(pos.west()).isAir();
					if (!face || !HeavenSpells.carvable(level, pos, p)) continue;
					double roll = level.random.nextDouble();
					BlockState burnt = roll < 0.22 ? Blocks.MAGMA_BLOCK.defaultBlockState() : roll < 0.58 ? Blocks.NETHERRACK.defaultBlockState()
							: roll < 0.8 ? Blocks.BLACKSTONE.defaultBlockState() : null;
					if (burnt != null) level.setBlock(pos, burnt, 3);
					BlockPos above = pos.above();
					if (level.getBlockState(above).isAir() && (burnt != null && burnt.is(Blocks.NETHERRACK) || level.random.nextDouble() < 0.35)) ignite(p, level, above.immutable());
				}
			}
		}
	}

	/** A tunnel {@code radius} round bored from {@code from} to {@code to} through whatever stands between, nearest {@code from} first. */
	public static void bore(LivingEntity p, ServerLevel level, Vec3 from, Vec3 to, double radius) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
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
	public static int cutColumn(LivingEntity p, ServerLevel level, int x, int z, int yMin, int yMax, boolean debris) {
		if (!HeavenSpells.carvesLand(p)) return 0;
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
	 * What a slash's blade passed through (the Heaven-Severing Sword's, a tick of it at a time): each of {@code spans} ({x, z,
	 * lowest y, highest y}) cut from its top (or the top of what stands there, if lower) down to its bottom, like any wound,
	 * nearest {@code heart} first, a few pieces flung.
	 */
	public static void shear(LivingEntity p, ServerLevel level, List<int[]> spans, Vec3 heart) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		List<BlockPos> cells = new ArrayList<>();
		for (int[] span : spans) {
			// Land still loading (the slash asks for it as it goes) is cut once it has come, if it comes in time.
			if (!level.hasChunk(span[0] >> 4, span[1] >> 4)) WAITING.add(new Waiting(level, p, span, heart, level.getGameTime() + WAIT_TICKS));
			else addSpan(level, span, cells);
		}
		if (!cells.isEmpty()) queue(level, p, cells, heart, null, SHEAR_DEBRIS);
	}

	/** One span's cells ({x, z, lowest y, highest y}), from its top (or the top of what stands there, if lower) down. */
	private static void addSpan(ServerLevel level, int[] span, List<BlockPos> cells) {
		int x = span[0], z = span[1];
		int bottom = Math.max(level.getMinBuildHeight() + 1, span[2]);
		int top = Math.min(Math.min(level.getMaxBuildHeight() - 1, span[3]), level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1);
		for (int y = top; y >= bottom; y--) cells.add(new BlockPos(x, y, z));
	}

	/** A span of a slash's blade over land that wasn't loaded yet, waiting for it until {@code until} (game time). */
	private record Waiting(ServerLevel level, LivingEntity caster, int[] span, Vec3 heart, long until) {}

	private static final List<Waiting> WAITING = new ArrayList<>();
	/** How long a span waits for its land (ticks): long enough for land never made before to be made. */
	private static final int WAIT_TICKS = 300;

	/** The spans whose land has come since, cut (one wound a tick for them all); those waited for too long, let go. */
	private static void cutWaiting() {
		List<BlockPos> cells = new ArrayList<>();
		Waiting first = null;
		for (Iterator<Waiting> it = WAITING.iterator(); it.hasNext(); ) {
			Waiting w = it.next();
			if (w.level.hasChunk(w.span[0] >> 4, w.span[1] >> 4)) {
				if (first == null) first = w;
				if (w.level == first.level) {
					addSpan(w.level, w.span, cells);
					it.remove();
				}
			} else if (w.level.getGameTime() > w.until) {
				it.remove();
			}
		}
		if (first != null && !cells.isEmpty()) queue(first.level, first.caster, cells, first.heart, null, SHEAR_DEBRIS);
	}

	/**
	 * The land {@code radius} round {@code at} set ablaze: grass and earth scorched to coarse dirt, now and then a patch of magma or
	 * netherrack, and fires on {@code share} of it (on whatever stands highest there: a tree's crown catches too).
	 */
	public static void burn(LivingEntity p, ServerLevel level, Vec3 at, double radius, double share) {
		// A cultivator's spells leave the land as it was (HeavenSpells#carvesLand); a player's with the spells' griefing off set only tame
		// fires (#ignite), the ground left unscorched.
		boolean scorches = HeavenSpells.carvesLand(p);
		if (!scorches && !(p instanceof ServerPlayer)) return;
		HeavenPassives.ownFire(p, level, at, radius + 2); // its caster's own fire (one who has mastered fire walks it unburnt)
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				BlockPos ground = scorches ? groundTop(level, x, z) : null;
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

	/**
	 * The land between {@code from} and {@code to} round {@code at} withered, its life drunk out of it (Void Withering): grass, moss, podzol and
	 * mycelium gone to coarse dirt, and what grew on them dead (a dead bush here and there, else gone). Nothing is taken away.
	 */
	public static void wither(LivingEntity p, ServerLevel level, Vec3 at, double from, double to) {
		if (!HeavenSpells.carvesLand(p)) return; // a cultivator's spells leave the land as it was (HeavenSpells#carvesLand)
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(to);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double dist = Math.sqrt(dx * dx + dz * dz);
				if (dist < from || dist > to) continue;
				BlockPos ground = groundTop(level, centre.getX() + dx, centre.getZ() + dz);
				if (ground == null || !HeavenSpells.carvable(level, ground, p)) continue;
				BlockState was = level.getBlockState(ground);
				if (was.is(Blocks.GRASS_BLOCK) || was.is(Blocks.PODZOL) || was.is(Blocks.MYCELIUM) || was.is(Blocks.MOSS_BLOCK) || was.is(Blocks.ROOTED_DIRT)) {
					level.setBlock(ground, Blocks.COARSE_DIRT.defaultBlockState(), 3);
				}
				BlockPos above = ground.above();
				BlockState plant = level.getBlockState(above);
				if (!plant.isAir() && !plant.is(BlockTags.LEAVES) && (plant.is(BlockTags.FLOWERS) || plant.is(BlockTags.SAPLINGS) || plant.is(Blocks.GRASS) || plant.is(Blocks.FERN)
						|| plant.is(Blocks.TALL_GRASS) || plant.is(Blocks.LARGE_FERN) || plant.is(Blocks.MOSS_CARPET)) && mayChange(p, level, above)) {
					boolean tall = plant.is(Blocks.TALL_GRASS) || plant.is(Blocks.LARGE_FERN) || plant.is(BlockTags.TALL_FLOWERS);
					if (tall) level.setBlock(above.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
					level.setBlock(above, level.random.nextInt(5) == 0 ? Blocks.DEAD_BUSH.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
	}

	// --- Underway ---

	private static void queue(ServerLevel level, LivingEntity p, List<BlockPos> cells, Vec3 heart, Runnable after) {
		queue(level, p, cells, heart, after, DEBRIS);
	}

	private static void queue(ServerLevel level, LivingEntity p, List<BlockPos> cells, Vec3 heart, Runnable after, int debris) {
		long[] packed = new long[cells.size()];
		for (int i = 0; i < packed.length; i++) packed[i] = cells.get(i).asLong();
		queue(level, p, packed, heart, after, debris, false);
	}

	/**
	 * Blocks one caster's wounds may have waiting to be cut at once, all of them together, and everyone's ({@link #allowance}; the user's call,
	 * 2026-10-25, on the third sweep's finding: a True Immortal recasting its craters could queue hundreds of thousands and keep their land
	 * loaded for minutes). A wound past it is cut short, what lies nearest its heart kept, or not cut at all; a burst golden core's goes whole.
	 */
	static final int MOST_WAITING = 250_000, MOST_WAITING_IN_ALL = 2_000_000;

	/**
	 * How many of a new wound's {@code cells} {@code p} may have queued: what is left of its own share ({@link #MOST_WAITING}) and of everyone's
	 * ({@link #MOST_WAITING_IN_ALL}), a burst golden core's ({@code utter}) all of them, and counted against neither.
	 */
	private static int allowance(LivingEntity p, int cells, boolean utter) {
		if (utter) return cells;
		long mine = 0, all = 0;
		for (Job job : JOBS) {
			if (job.utter) continue;
			long left = job.cells.length - job.next;
			all += left;
			if (p != null && job.caster != null && job.caster.getUUID().equals(p.getUUID())) mine += left;
		}
		long room = Math.min(p == null ? Long.MAX_VALUE : MOST_WAITING - mine, MOST_WAITING_IN_ALL - all);
		return (int) Math.max(0, Math.min(cells, room));
	}

	/** {@code cells} (packed) sorted nearest {@code heart} first, and cut from the next tick, as many as its caster may yet have queued ({@link #allowance}). */
	private static void queue(ServerLevel level, LivingEntity p, long[] cells, Vec3 heart, Runnable after, int debris, boolean utter) {
		if (cells.length == 0) {
			if (after != null) after.run();
			return;
		}
		int allowed = allowance(p, cells.length, utter);
		if (allowed == 0) return;
		BlockPos centre = BlockPos.containing(heart);
		// Sorted by a key of the distance and the place in the list: a sort of plain numbers, not of millions of objects.
		long[] keys = new long[cells.length];
		for (int i = 0; i < cells.length; i++) {
			long dx = BlockPos.getX(cells[i]) - centre.getX(), dy = BlockPos.getY(cells[i]) - centre.getY(), dz = BlockPos.getZ(cells[i]) - centre.getZ();
			keys[i] = (dx * dx + dy * dy + dz * dz) << INDEX_BITS | i;
		}
		Arrays.sort(keys);
		// Cut short to what its caster may yet have queued: the nearest its heart kept.
		long[] sorted = new long[allowed];
		for (int i = 0; i < allowed; i++) sorted[i] = cells[(int) (keys[i] & INDEX_MASK)];
		double furthest = Math.sqrt(keys[allowed - 1] >>> INDEX_BITS);
		int keep = furthest > KEEP_FROM ? Math.min(KEEP_MOST, (int) Math.ceil(furthest / 16) + 1) : 0;
		JOBS.add(new Job(level, p, sorted, heart, after, debris, keep, utter));
	}

	/** A cell's place in its list, in the low bits of its sorting key (a wound of up to 268 million cells). */
	private static final int INDEX_BITS = 28;
	private static final long INDEX_MASK = (1L << INDEX_BITS) - 1;

	/**
	 * Every tick (HeavenSpells#tick): the wounds underway cut a share further, {@link #BUDGET} blocks in all, shared out evenly among
	 * them (what one doesn't need goes round again to the rest), so a small wound isn't kept waiting behind a great one.
	 */
	public static void tick(MinecraftServer server) {
		if (!WAITING.isEmpty()) cutWaiting();
		if (JOBS.isEmpty()) return;
		int budget = BUDGET;
		List<Job> done = new ArrayList<>();
		for (Job job : JOBS) {
			job.thisTick = 0;
			if (job.keep > 0 && job.age++ % KEEP_EVERY == 0) job.level.getChunkSource().addRegionTicket(KEEP_LOADED, job.keepAt, job.keep, job.keepAt);
		}
		while (budget > 0 && !JOBS.isEmpty()) {
			int share = Math.max(1, budget / JOBS.size());
			boolean went = false;
			for (Iterator<Job> it = JOBS.iterator(); it.hasNext() && budget > 0; ) {
				Job job = it.next();
				int most = Math.min(share, budget);
				if (job.rate > 0) most = Math.min(most, job.rate - job.thisTick);
				int cut = most > 0 ? cut(job, most) : 0;
				job.thisTick += cut;
				budget -= cut;
				if (cut > 0) went = true;
				if (job.next >= job.cells.length) {
					it.remove();
					done.add(job);
					went = true;
				}
			}
			if (!went) break; // every job left has had all it may this tick
		}
		for (Job job : done) if (job.after != null) job.after.run();
	}

	/** {@code job} cut up to {@code most} cells further. @return how many it went through */
	private static int cut(Job job, int most) {
		int went = 0;
		while (job.next < job.cells.length && went < most) {
			int index = job.next++;
			BlockPos pos = BlockPos.of(job.cells[index]);
			went++;
			if (!job.level.isLoaded(pos)) continue;
			if (job.place != null) {
				lay(job, pos, Block.stateById(job.place[index]));
				continue;
			}
			if (job.drain) {
				dry(job, pos);
				continue;
			}
			BlockState state = job.level.getBlockState(pos);
			if (state.isAir() || !HeavenSpells.carvable(job.level, pos, job.caster, job.utter)) continue;
			if (job.utter) {
				obliterate(job.level, pos);
				// Water, lava and things that hold something are never flung as pieces: only the ground.
				if (!state.getFluidState().isEmpty() && state.canBeReplaced() || state.hasBlockEntity()) continue;
			} else {
				job.level.removeBlock(pos, false);
			}
			if (job.flung < job.debris && job.level.random.nextInt(Math.max(8, job.cells.length / (job.debris * 2))) == 0) {
				job.flung++;
				Vec3 from = Vec3.atBottomCenterOf(pos);
				Vec3 out = from.subtract(job.heart).multiply(1, 0, 1);
				out = out.lengthSqr() < 1.0e-4 ? new Vec3(job.level.random.nextGaussian(), 0, job.level.random.nextGaussian()) : out.normalize();
				double far = 4 + job.level.random.nextDouble() * 8;
				SpellBlock.show(job.level, HeavenSpells.asEarth(state), from.add(0, 0.5, 0), out.scale(far).add(0, 1 + job.level.random.nextDouble() * 3, 0), 0,
						14 + job.level.random.nextInt(10), SpellBlock.Motion.FLY, 0);
			}
		}
		return went;
	}

	/**
	 * A block taken by a burst golden core: gone to air outright (water and lava too, and no waterlogged block leaves its water
	 * behind). What it holds is thrown out: whatever holds items (a chest, a shulker box, any mod's container) spills them here and is
	 * emptied first, so nothing is spilled twice (a shulker box, which keeps its things only in its own loot, took them with it before
	 * the sweep of 2026-10-23); a lectern's book, the mod's cauldrons and altars spill as they always do when removed; a Spirit Pedestal
	 * (whose fruit only its loot table gives back, never a removal) lets its fruit fall.
	 */
	private static void obliterate(ServerLevel level, BlockPos pos) {
		net.minecraft.world.level.block.entity.BlockEntity held = level.getBlockEntity(pos);
		if (held instanceof SpiritPedestalBlockEntity pedestal && pedestal.hasFruit()) {
			Block.popResource(level, pos, pedestal.getFruit().copy());
		} else if (held instanceof net.minecraft.world.Container container && !container.isEmpty()) {
			net.minecraft.world.Containers.dropContents(level, pos, container);
			container.clearContent();
		}
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/**
	 * A cell of land laid down ({@link #raise}): {@code state} set where only air, water or something that gives way stands (grass, snow, a
	 * flower), and only where its caster may change the land; a few clods of it seen flying in.
	 */
	private static void lay(Job job, BlockPos pos, BlockState state) {
		BlockState here = job.level.getBlockState(pos);
		if (!here.isAir() && !here.canBeReplaced() || HeavenSpells.isTemporary(job.level, pos) || !mayChange(job.caster, job.level, pos)) return;
		job.level.setBlock(pos, state, 3);
		if (job.flung < job.debris && job.level.random.nextInt(Math.max(8, job.cells.length / (job.debris * 2))) == 0) {
			job.flung++;
			Vec3 to = Vec3.atBottomCenterOf(pos);
			Vec3 from = to.add(job.level.random.nextGaussian() * 2, 3 + job.level.random.nextDouble() * 3, job.level.random.nextGaussian() * 2);
			SpellBlock.show(job.level, state, from, to.subtract(from), 0, 10, SpellBlock.Motion.FLY, 0);
		}
	}

	/** A cell's water drawn out of it ({@link #drain}): a water block (and what grows in water) to air, a waterlogged block unwatered. */
	private static void dry(Job job, BlockPos pos) {
		BlockState state = job.level.getBlockState(pos);
		if (!state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) || !mayChange(job.caster, job.level, pos)) return;
		if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
			job.level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, false), 2 | 16);
		} else if (state.canBeReplaced() || state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SEAGRASS)
				|| state.is(Blocks.TALL_SEAGRASS)) {
			job.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
		}
	}

	/**
	 * A trench cut along the land from {@code from} to {@code to} (a wheel's, a jet of fire's, a glacier's), {@code width} across and
	 * {@code depth} deep all along it, following the land's top (and through whatever stands on it), its edges ragged; nearest {@code from}
	 * first. Unlike {@link #gouge} it doesn't shallow toward its ends: a stretch of a longer track, cut a tick at a time.
	 */
	public static void trench(LivingEntity p, ServerLevel level, Vec3 from, Vec3 to, double width, double depth) {
		if (!HeavenSpells.carvesLand(p)) return;
		Vec3 line = to.subtract(from).multiply(1, 0, 1);
		double length = line.length();
		Vec3 along = length < 1.0e-3 ? new Vec3(1, 0, 0) : line.scale(1 / length), across = new Vec3(-along.z, 0, along.x);
		long seed = BlockPos.containing(from).asLong() * 7;
		java.util.Map<Long, Integer> deepest = new java.util.LinkedHashMap<>();
		for (double t = 0; t <= length; t += 0.5) {
			for (double off = -width / 2; off <= width / 2; off += 0.5) {
				Vec3 at = from.add(along.scale(t)).add(across.scale(off));
				int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z);
				double edge = Math.abs(off) / (width / 2 + 0.01);
				int deep = (int) Math.round(depth * (edge > 0.7 ? 0.5 + 0.5 * noise(seed, x, z) : 1));
				if (deep > 0) deepest.merge(BlockPos.asLong(x, 0, z), deep, Math::max);
			}
		}
		List<BlockPos> cells = new ArrayList<>();
		for (Map.Entry<Long, Integer> e : deepest.entrySet()) {
			BlockPos top = groundTop(level, BlockPos.getX(e.getKey()), BlockPos.getZ(e.getKey()));
			if (top == null) continue;
			int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()) - 1;
			for (int y = surface; y > top.getY() - e.getValue(); y--) cells.add(new BlockPos(top.getX(), y, top.getZ()));
		}
		queue(level, p, cells, from, null, SHEAR_DEBRIS);
	}

	// --- Land laid down, water drawn out, the land frozen or greened (Sacred Vessel on, 2026-10-24) ---

	/**
	 * A mound {@code radius} round and {@code height} high at its heart heaved up over the land round {@code at} (Xirang, the soil that grows of
	 * itself): earth laid on the ground of each column (stone at its heart, earth over it, grass or coarse earth on top), higher toward the heart
	 * and ragged at its edge, from the bottom up, nearest the heart first; nothing laid within {@code spare} of its caster's column (it is not
	 * buried by its own spell), nor where its caster may not change the land. A cultivator's spells lay nothing (HeavenSpells#carvesLand).
	 */
	public static void raise(LivingEntity p, ServerLevel level, Vec3 at, double radius, double height, double spare) {
		if (!HeavenSpells.carvesLand(p)) return;
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		double sx = p == null ? Double.NaN : p.getX() - centre.getX(), sz = p == null ? Double.NaN : p.getZ() - centre.getZ();
		LongArrayList cells = new LongArrayList();
		it.unimi.dsi.fastutil.ints.IntArrayList states = new it.unimi.dsi.fastutil.ints.IntArrayList();
		long seed = centre.asLong() * 17;
		int stone = Block.getId(Blocks.STONE.defaultBlockState()), dirt = Block.getId(Blocks.DIRT.defaultBlockState()), rooted = Block.getId(Blocks.ROOTED_DIRT.defaultBlockState()),
				grass = Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()), coarse = Block.getId(Blocks.COARSE_DIRT.defaultBlockState());
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double flat = (dx * dx + dz * dz) / (radius * radius), rag = 1 + 0.2 * (noise(seed, dx, dz) - 0.5);
				if (flat > rag || p != null && Math.hypot(dx + 0.5 - sx, dz + 0.5 - sz) < spare) continue;
				BlockPos ground = groundTop(level, centre.getX() + dx, centre.getZ() + dz);
				if (ground == null) continue;
				// Its height over the land at the heart's level: a column already high stands out of it, a low one is filled up to it.
				int top = centre.getY() - 1 + (int) Math.round(height * Math.sqrt(Math.max(0, 1 - flat / rag)) * (0.85 + 0.3 * noise(seed + 5, dx, dz)));
				for (int y = ground.getY() + 1; y <= top; y++) {
					cells.add(BlockPos.asLong(ground.getX(), y, ground.getZ()));
					int below = top - y;
					states.add(y == top ? (noise(seed + 9, dx, dz) < 0.25 ? coarse : grass) : below < 3 ? (noise(seed + y, dx, dz) < 0.2 ? rooted : dirt) : stone);
				}
			}
		}
		if (cells.isEmpty()) return;
		int allowed = allowance(p, cells.size(), false);
		if (allowed == 0) return;
		// Bottom up (a cell laid on air would float), nearest the heart first on each level; cut short (its top left off) to what its caster
		// may yet have queued.
		long[] packed = cells.toLongArray();
		int[] ids = states.toIntArray();
		long[] keys = new long[packed.length];
		for (int i = 0; i < packed.length; i++) {
			long dx = BlockPos.getX(packed[i]) - centre.getX(), dz = BlockPos.getZ(packed[i]) - centre.getZ();
			long dy = BlockPos.getY(packed[i]) - centre.getY() + 512;
			keys[i] = (dy << 18 | Math.min((1L << 18) - 1, dx * dx + dz * dz)) << INDEX_BITS | i;
		}
		Arrays.sort(keys);
		long[] sorted = new long[allowed];
		int[] sortedIds = new int[allowed];
		for (int i = 0; i < allowed; i++) {
			int from = (int) (keys[i] & INDEX_MASK);
			sorted[i] = packed[from];
			sortedIds[i] = ids[from];
		}
		int keep = radius > KEEP_FROM ? Math.min(KEEP_MOST, (int) Math.ceil(radius / 16) + 1) : 0;
		JOBS.add(new Job(level, p, sorted, at, null, DEBRIS, keep, false, sortedIds, false));
	}

	/**
	 * The water drawn out of the land within {@code radius} of {@code at}, from {@code up} over its level to {@code down} under it (an ocean
	 * sealed in a vase): lakes and rivers left dry, nearest the heart first. A cultivator's spells take nothing.
	 */
	public static void drain(LivingEntity p, ServerLevel level, Vec3 at, double radius, int up, int down) {
		if (!HeavenSpells.carvesLand(p)) return;
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		LongArrayList cells = new LongArrayList();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				if (!level.hasChunk(x >> 4, z >> 4)) continue;
				for (int y = centre.getY() - down; y <= centre.getY() + up; y++) {
					if (level.getBlockState(pos.set(x, y, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) cells.add(pos.asLong());
				}
			}
		}
		if (cells.isEmpty()) return;
		int allowed = allowance(p, cells.size(), false);
		if (allowed == 0) return;
		long[] packed = cells.toLongArray();
		long[] keys = new long[packed.length];
		for (int i = 0; i < packed.length; i++) {
			long dx = BlockPos.getX(packed[i]) - centre.getX(), dy = BlockPos.getY(packed[i]) - centre.getY(), dz = BlockPos.getZ(packed[i]) - centre.getZ();
			keys[i] = (dx * dx + dy * dy + dz * dz) << INDEX_BITS | i;
		}
		Arrays.sort(keys);
		long[] sorted = new long[allowed];
		for (int i = 0; i < allowed; i++) sorted[i] = packed[(int) (keys[i] & INDEX_MASK)];
		JOBS.add(new Job(level, p, sorted, at, null, 0, radius > KEEP_FROM ? Math.min(KEEP_MOST, (int) Math.ceil(radius / 16) + 1) : 0, false, null, true));
	}

	/**
	 * The land within {@code radius} of {@code at} frozen (a frozen moon's light, 2026-10-24): the top of any water to ice (packed ice within
	 * half of it), the ground under snow, its grass and flowers dead under it. Nothing is taken away; a cultivator's spells change nothing.
	 */
	public static void freeze(LivingEntity p, ServerLevel level, Vec3 at, double radius) {
		if (!HeavenSpells.carvesLand(p)) return;
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(radius);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > radius) continue;
				int x = centre.getX() + dx, z = centre.getZ() + dz;
				if (!level.hasChunk(x >> 4, z >> 4)) continue;
				BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
				BlockState state = level.getBlockState(top);
				if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER) && state.getFluidState().isSource() && state.is(Blocks.WATER)) {
					if (mayChange(p, level, top)) level.setBlock(top, (d < radius * 0.5 ? Blocks.PACKED_ICE : Blocks.ICE).defaultBlockState(), 3);
					continue;
				}
				BlockPos ground = groundTop(level, x, z);
				if (ground == null) continue;
				BlockPos above = ground.above();
				BlockState over = level.getBlockState(above);
				if (!over.isAir() && !over.canBeReplaced() || !mayChange(p, level, above)) continue;
				if (!over.isAir() && over.getFluidState().isEmpty()) level.setBlock(above, Blocks.AIR.defaultBlockState(), 2 | 16);
				if (Blocks.SNOW.defaultBlockState().canSurvive(level, above)) {
					int layers = 1 + (int) Math.round(3 * (1 - d / radius) * (0.6 + 0.4 * noise(centre.asLong(), dx, dz)));
					level.setBlock(above, Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, Math.min(8, layers)), 3);
				}
			}
		}
	}

	/**
	 * The land within {@code radius} of {@code at} greened (life given back to it, 2026-10-24): bare earth to grass, and on {@code share} of the
	 * grass a plant (tall grass, ferns, flowers); stone at its top mossed near the heart. Nothing is taken away; a cultivator's spells change
	 * nothing.
	 */
	public static void flourish(LivingEntity p, ServerLevel level, Vec3 at, double from, double to, double share) {
		if (!HeavenSpells.carvesLand(p)) return;
		BlockPos centre = BlockPos.containing(at);
		int r = (int) Math.ceil(to);
		BlockState[] plants = {Blocks.GRASS.defaultBlockState(), Blocks.GRASS.defaultBlockState(), Blocks.FERN.defaultBlockState(), Blocks.POPPY.defaultBlockState(),
				Blocks.DANDELION.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState(), Blocks.OXEYE_DAISY.defaultBlockState(),
				Blocks.LILY_OF_THE_VALLEY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState()};
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d < from || d > to) continue;
				BlockPos ground = groundTop(level, centre.getX() + dx, centre.getZ() + dz);
				if (ground == null || !mayChange(p, level, ground)) continue;
				BlockState was = level.getBlockState(ground);
				if (was.is(Blocks.DIRT) || was.is(Blocks.COARSE_DIRT) || was.is(Blocks.ROOTED_DIRT) || was.is(Blocks.PODZOL) || was.is(Blocks.MYCELIUM)) {
					level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
					was = Blocks.GRASS_BLOCK.defaultBlockState();
				} else if ((was.is(Blocks.STONE) || was.is(Blocks.COBBLESTONE)) && d < to * 0.5 && noise(centre.asLong(), dx, dz) < 0.5) {
					level.setBlock(ground, Blocks.MOSS_BLOCK.defaultBlockState(), 3);
					continue;
				}
				if (!was.is(Blocks.GRASS_BLOCK) || level.random.nextDouble() >= share) continue;
				BlockPos above = ground.above();
				BlockState plant = plants[level.random.nextInt(plants.length)];
				if (level.getBlockState(above).isAir() && plant.canSurvive(level, above) && mayChange(p, level, above)) level.setBlock(above, plant, 3);
			}
		}
	}

	/** Server stopping: whatever is underway stops where it is. */
	public static void clear() {
		JOBS.clear();
		WAITING.clear();
	}

	/** Blocks still to be cut, all wounds together (tests). */
	public static int pending() {
		int left = 0;
		for (Job job : JOBS) left += job.cells.length - job.next;
		return left;
	}

	// --- Helpers ---

	/**
	 * A wound's floor laid: the block under each column's deepest cut made {@code floor}'s (and fires on a burnt one); a lake's
	 * water set in the cut itself, up to two below the ground's level ({@code surfaceY}) so it stays in its basin.
	 */
	private static void lay(LivingEntity p, ServerLevel level, Map<Long, Integer> lowest, Floor floor, int surfaceY) {
		if (floor == Floor.NONE) return;
		LongArrayList lake = new LongArrayList();
		for (Map.Entry<Long, Integer> e : lowest.entrySet()) {
			BlockPos below = new BlockPos(BlockPos.getX(e.getKey()), e.getValue() - 1, BlockPos.getZ(e.getKey()));
			if (!level.isLoaded(below) || !level.getBlockState(below.above()).isAir()) continue;
			if (floor == Floor.LAKE) {
				for (BlockPos water = below.above(); water.getY() <= surfaceY - 2 && water.getY() <= below.getY() + 3; water = water.above()) {
					if (!level.getBlockState(water).isAir() || !mayChange(p, level, water)) break;
					lake.add(water.asLong());
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
			} else if (floor == Floor.GLASSY) {
				if (roll < 0.55) level.setBlock(below, Blocks.OBSIDIAN.defaultBlockState(), 3);
				else if (roll < 0.75) level.setBlock(below, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
				else if (roll < 0.9) level.setBlock(below, Blocks.BLACK_STAINED_GLASS.defaultBlockState(), 3);
			} else if (floor == Floor.ICE) {
				if (roll < 0.4) level.setBlock(below, Blocks.PACKED_ICE.defaultBlockState(), 3);
				else if (roll < 0.65) level.setBlock(below, Blocks.ICE.defaultBlockState(), 3);
				else if (roll < 0.85) level.setBlock(below, Blocks.SNOW_BLOCK.defaultBlockState(), 3);
				else if (roll < 0.93) level.setBlock(below, Blocks.BLUE_ICE.defaultBlockState(), 3);
			} else {
				if (roll < 0.3) level.setBlock(below, Blocks.COBBLESTONE.defaultBlockState(), 3);
				else if (roll < 0.45) level.setBlock(below, Blocks.GRAVEL.defaultBlockState(), 3);
				else if (roll < 0.6) level.setBlock(below, Blocks.COARSE_DIRT.defaultBlockState(), 3);
			}
		}
		if (!lake.isEmpty()) {
			// The lake's water poured in over a while, bottom up (Job#rate), not all its thousands of sources in one tick.
			long[] cells = lake.toLongArray();
			long[] keys = new long[cells.length];
			for (int i = 0; i < cells.length; i++) keys[i] = (long) (BlockPos.getY(cells[i]) + 2048) << INDEX_BITS | i;
			Arrays.sort(keys);
			long[] sorted = new long[cells.length];
			int[] water = new int[cells.length];
			int id = Block.getId(Blocks.WATER.defaultBlockState());
			for (int i = 0; i < cells.length; i++) {
				sorted[i] = cells[(int) (keys[i] & INDEX_MASK)];
				water[i] = id;
			}
			Job job = new Job(level, p, sorted, Vec3.atCenterOf(BlockPos.of(sorted[0])), null, 0, 0, false, water, false);
			job.rate = LAKE_RATE;
			JOBS.add(job);
		}
	}

	/** A lake's water is poured into its basin at most this many blocks a tick (Job#rate). */
	static final int LAKE_RATE = 400;

	/** How long a tame fire burns (a spell's, its griefing off: {@link #ignite}), in ticks: this, and up to as long again. */
	public static final int TAME_FIRE = 70;

	/**
	 * A fire set at {@code pos} if one can burn there and {@code p} may change it. With the spells' griefing off (HeavenSpells#griefs, the
	 * user's ask, 2026-10-25: "fire will not spread and just put itself out after a bit"), a tame one: set down for a moment
	 * (HeavenSpells#placeTemporary, {@link #TAME_FIRE} or so), catching nothing round it and burning nothing away (FireBlockMixin), then out.
	 */
	static void ignite(LivingEntity p, ServerLevel level, BlockPos pos) {
		if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !BaseFireBlock.canBePlacedAt(level, pos, Direction.UP)) return;
		if (p instanceof ServerPlayer && !HeavenSpells.griefs(level)) {
			if (allowed(p, level, pos, false) && HeavenSpells.placeTemporary(level, pos, BaseFireBlock.getState(level, pos), TAME_FIRE + level.random.nextInt(TAME_FIRE))) {
				HeavenPassives.ownFire(p, level, Vec3.atCenterOf(pos), 2.5);
			}
			return;
		}
		if (!mayChange(p, level, pos)) return;
		level.setBlock(pos, BaseFireBlock.getState(level, pos), 11);
		HeavenPassives.ownFire(p, level, Vec3.atCenterOf(pos), 2.5); // its caster's own fire (one who has mastered fire walks it unburnt)
	}

	/**
	 * Whether a spell of {@code p}'s may set something at {@code pos} for good: not under a barrier's shelter, ground they may change, nothing
	 * kept safe (HeavenSpells#guarded), and the spells' griefing on (HeavenSpells#griefs). On a sect's grounds not {@code p}'s own, that is
	 * trespass (SectManager#spoiled).
	 */
	static boolean mayChange(LivingEntity p, ServerLevel level, BlockPos pos) {
		if (p instanceof ServerPlayer && !HeavenSpells.griefs(level)) return false;
		return allowed(p, level, pos, true);
	}

	/**
	 * {@link #mayChange}'s rule, the griefing aside: a tame fire ({@link #ignite}) or water that drains away again goes only where a lasting
	 * change might, but trespasses on nothing ({@code taking} false).
	 */
	static boolean allowed(LivingEntity p, ServerLevel level, BlockPos pos, boolean taking) {
		if (Formations.sheltersFrom(level, pos, p) || HeavenSpells.guarded(level, pos, p)) return false;
		if (p == null) return true;
		if (!(p instanceof ServerPlayer player)) return false; // a cultivator's spells set nothing in the land (HeavenSpells#carvesLand)
		if (Formations.protectsGroundQuietly(player, pos) || !player.mayBuild() || !level.mayInteract(player, pos)) return false;
		if (taking) SectManager.spoiled(level, player, pos);
		return true;
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
