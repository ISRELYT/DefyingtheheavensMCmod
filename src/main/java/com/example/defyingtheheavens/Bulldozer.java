package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Meteor Flight's bulldozer (the user's ask, 2026-10-23: "I go so fast that when I run into shit it will sometimes stop me and I
 * will lose all my speed"): flying fast with the boost held (sprint), the way ahead is cleared before the flier reaches it, so they
 * keep their speed. An illusion that works, done the way fast games do it:
 * <ul>
 * <li><b>A swept volume, not a ray.</b> At up to 10 blocks a tick a ray from the eyes misses what the feet or a shoulder would hit,
 * and checks a tick apart skip what lies between (tunnelling). The body's own box is swept along the way it goes ({@link #sweep}),
 * every half block, so nothing it would touch is missed.</li>
 * <li><b>A reach that grows with the speed</b> ({@link #reach}): this tick's move and {@link #LEAD_TICKS} more, so what's in the way
 * is gone a moment before one gets there, at most {@link #MOST_REACH}.</li>
 * <li><b>Predicted on the flier's client, settled by the server</b> (client-side prediction and server reconciliation): the client
 * clears what it swept at once (MeteorFlightClient#clearPath, through vanilla's own BlockStatePredictionHandler, as it breaks a
 * block one mines) and names it to the server, which takes away what it agrees may go (MeteorFlight#bulldoze) and acknowledges the
 * word; whatever it refused comes back on the client, so nothing is lost to a ghost block. Waiting on the server instead (as the
 * ablaze carve did alone) left a wall standing on the flier's screen for its latency, and it stopped them dead.</li>
 * <li><b>Only obstacles</b>: blocks one would collide with (grass and flowers are flown through), and never the ground under one's
 * feet ({@code floor}): skimming low clears what stands up ahead and slides along the floor rather than ploughing a trench. Diving
 * steeply ({@link #burrowing}) there is no floor: one burrows straight in.</li>
 * <li><b>A budget</b>: at most {@link #MOST_A_TICK} blocks a tick, and the crumbling shown on only a few of them.</li>
 * </ul>
 * What may go is the great spells' rule (HeavenSpells#carvable; {@link #looksCarvable} is the part a client can see), and nothing
 * at all with the gamerule meteorFlightSmashes off.
 */
public final class Bulldozer {
	/** The way is cleared from this speed (blocks a tick: Qi Flight's own sprint, 22 blocks a second) with the boost held. */
	public static final double FROM_SPEED = MeteorFlight.NORMAL_SPRINT;
	/** Cleared this many ticks' flight ahead besides this tick's own, and this much more, and never further than {@link #MOST_REACH}. */
	public static final double LEAD_TICKS = 3, MARGIN = 1.5, MOST_REACH = 48;
	/** The server takes the client's word for blocks this much further from where it last saw the flier (its moves come later). */
	public static final double SLACK = 24;
	/** The body's box, this much wider each side and taller, swept (a hair, against rounding: the hole is the body's own size). */
	private static final double PAD = 0.1;
	/** Swept every this many blocks along the way: under the box's narrowest, so nothing between is missed. */
	private static final double STEP = 0.5;
	/** At most this many blocks a tick. */
	public static final int MOST_A_TICK = 384;
	/** The crumbling (sound and pieces) shown on at most this many of a tick's blocks. */
	public static final int SHOWN_A_TICK = 3;
	/**
	 * Flying down steeper than this (the sine of 35 degrees below level), one burrows: the ground under the feet is in the way like
	 * anything else, not a floor to skim. Shallower, a swoop to the ground slides along it. (The floor was kept however steep the dive,
	 * so looking down to burrow straight into the earth only skimmed off along it: the user's report, 2026-10-24.)
	 */
	public static final double BURROW_SLOPE = 0.574;

	/** Whether flying along {@code dir} (a unit vector) burrows into the ground rather than skimming it ({@link #BURROW_SLOPE}). */
	public static boolean burrowing(Vec3 dir) {
		return dir.y < -BURROW_SLOPE;
	}

	/** The floor to {@link #sweep} by, the feet at {@code feet}: the feet skimming, none {@code burrowing}. */
	public static double floor(boolean burrowing, double feet) {
		return burrowing ? Double.NEGATIVE_INFINITY : feet;
	}

	/** How far ahead the way is cleared, flying {@code speed} blocks a tick. */
	public static double reach(double speed) {
		return Math.min(MOST_REACH, speed * (1 + LEAD_TICKS) + MARGIN);
	}

	/**
	 * What stands in the way of {@code body} swept {@code length} blocks along {@code dir} (a unit vector), nearest first: blocks it
	 * would collide with that {@link #looksCarvable} for {@code p}, whose top stands above {@code floor} (the feet), at most
	 * {@code max}. Land not loaded is left alone.
	 */
	public static List<BlockPos> sweep(Level level, Player p, AABB body, Vec3 dir, double length, double floor, int max) {
		List<BlockPos> found = new ArrayList<>();
		if (length <= 0 || max <= 0) return found;
		AABB start = new AABB(body.minX - PAD, body.minY, body.minZ - PAD, body.maxX + PAD, body.maxY + PAD, body.maxZ + PAD);
		// Decided once each (gone, or not to be): a cell only grazed by an earlier box may still be met by a later one.
		Set<Long> decided = new HashSet<>();
		BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
		int steps = Math.max(1, Mth.ceil(length / STEP));
		for (int i = 0; i <= steps; i++) {
			AABB box = start.move(dir.scale(length * i / steps));
			int x0 = Mth.floor(box.minX), x1 = Mth.floor(box.maxX - 1.0e-7);
			int y0 = Mth.floor(box.minY), y1 = Mth.floor(box.maxY - 1.0e-7);
			int z0 = Mth.floor(box.minZ), z1 = Mth.floor(box.maxZ - 1.0e-7);
			for (int x = x0; x <= x1; x++) {
				for (int y = y0; y <= y1; y++) {
					for (int z = z0; z <= z1; z++) {
						cell.set(x, y, z);
						long key = cell.asLong();
						if (decided.contains(key)) continue;
						if (!level.isLoaded(cell)) continue;
						BlockState state = level.getBlockState(cell);
						if (state.isAir() || !looksCarvable(level, cell, state, p)) {
							decided.add(key);
							continue;
						}
						VoxelShape shape = state.getCollisionShape(level, cell);
						if (shape.isEmpty()) {
							decided.add(key);
							continue;
						}
						AABB solid = shape.bounds().move(cell);
						// The ground one skims over is the floor, not in the way.
						if (solid.maxY <= floor + 1.0e-3) {
							decided.add(key);
							continue;
						}
						if (!solid.intersects(box)) continue;
						decided.add(key);
						found.add(cell.immutable());
						if (found.size() >= max) return found;
					}
				}
			}
		}
		return found;
	}

	/**
	 * Whether the bulldozer may take {@code state} at {@code pos} for {@code p}, as far as either side can see: the parts of
	 * HeavenSpells#carvable that need no server (nothing harder than obsidian, nothing kept in it, no barrier, no fluid of its own,
	 * not in Adventure mode). The server checks the rest (formations, spawn protection, a sect's grounds) and undoes the rest.
	 */
	public static boolean looksCarvable(Level level, BlockPos pos, BlockState state, Player p) {
		if (!level.isInWorldBounds(pos) || state.isAir() || !state.getFluidState().isEmpty() && state.canBeReplaced()) return false;
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness < 0 || hardness >= 50 || state.hasBlockEntity() || state.getBlock() instanceof SectBarrierBlock) return false;
		return p.mayBuild();
	}

	private Bulldozer() {}
}
