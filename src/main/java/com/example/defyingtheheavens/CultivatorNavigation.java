package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A cultivator's way on foot. Vanilla's search gives up on any walk longer than the follow range (32 blocks), so from the back
 * of the meditation hall, its gate behind the hall, every path it found ended in the corner nearest the gate: the way out of
 * the door and round the hall is longer than that. {@link #farPath} is the longer search, for when the usual one falls short
 * (CultivatorGoals#walkToward). And a walker that gets nowhere beside the next point of its path, unable to reach its very
 * middle (someone sits there) or to get down onto it (a guard hung on the corner of the meditation hall's plinth, rocking
 * back and forth, where its path stepped down across the corner), steps down onto it and goes on to the next.
 */
public class CultivatorNavigation extends GroundPathNavigation {
	/** How long a walk the far search will follow, and how many more steps than the usual search it may try. */
	public static final float FAR_WALK = 96;
	private static final float FAR_NODES = 16;
	/** A far search that failed isn't tried again for the same spot this soon (a spot nobody can walk to costs the most). */
	private static final long FAR_RETRY = 200;
	/** Getting nowhere this long beside the next point of its path, it goes on to the one after. */
	private static final int STALL_TICKS = 20;

	private boolean far;
	/** Spots the far search failed to reach lately, and when. */
	private final Map<BlockPos, Long> failed = new HashMap<>();
	/** Where it was when it last got anywhere, and how many ticks since. */
	private Vec3 stallFrom;
	private int stalled;

	public CultivatorNavigation(Mob mob, Level level) {
		super(mob, level);
	}

	/** A path to {@code target} by a walk of up to {@link #FAR_WALK} blocks (the end nearest it, if it can't be reached), or null. */
	public Path farPath(BlockPos target, int accuracy) {
		return far(target, () -> createPath(target, accuracy));
	}

	/**
	 * A path by the far search to whichever of {@code targets} it reaches first (from inside a building, the nearest by the
	 * walk: out of the door), or null. {@code key} stands for them all in the record of failed searches.
	 */
	public Path farPath(Set<BlockPos> targets, BlockPos key, int accuracy) {
		return far(key, () -> createPath(targets, accuracy));
	}

	private Path far(BlockPos key, Supplier<Path> search) {
		// In the air (a step down, a jump) nothing can be planned: that is no failure to remember.
		if (!canUpdatePath()) return null;
		long now = level.getGameTime();
		Long lastFailed = failed.get(key);
		if (lastFailed != null && now - lastFailed < FAR_RETRY) return null;
		if (failed.size() > 16) failed.values().removeIf(time -> now - time >= FAR_RETRY);
		// Set aside the path it is on: vanilla hands that back for the same spot (a short search's dead end, say) unsearched.
		Path current = path;
		path = null;
		far = true;
		setMaxVisitedNodesMultiplier(FAR_NODES);
		Path found;
		try {
			found = search.get();
		} finally {
			far = false;
			resetMaxVisitedNodesMultiplier();
			path = current;
		}
		if (found == null || !found.canReach()) failed.put(key.immutable(), now);
		else failed.remove(key);
		return found;
	}

	@Override
	protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy) {
		return far ? createPath(targets, regionOffset, offsetUpward, accuracy, FAR_WALK) : super.createPath(targets, regionOffset, offsetUpward, accuracy);
	}

	@Override
	protected void followThePath() {
		super.followThePath();
		if (path == null || path.isDone()) {
			stalled = 0;
			return;
		}
		// Getting nowhere: still within half a block of where it was a second ago (pressing on, or rocking back and forth).
		Vec3 at = mob.position();
		if (stallFrom == null || at.distanceToSqr(stallFrom) > 0.25) {
			stallFrom = at;
			stalled = 0;
			return;
		}
		if (++stalled < STALL_TICKS) return;
		stallFrom = null;
		// Vanilla takes a point as reached only within 0.45 of its middle and less than a block above it.
		BlockPos next = path.getNextNodePos();
		double dx = Math.abs(at.x - (next.getX() + 0.5)), dz = Math.abs(at.z - (next.getZ() + 0.5)), dy = at.y - next.getY();
		if (dx >= 1.5 || dz >= 1.5 || dy <= -0.5 || dy >= 1.5) return;
		// Caught on the edge above it (a step down taken across a corner): down onto it.
		double floor = WalkNodeEvaluator.getFloorLevel(level, next);
		if (dy >= 0.5 && level.noCollision(mob, mob.getType().getDimensions().makeBoundingBox(next.getX() + 0.5, floor, next.getZ() + 0.5))) {
			mob.moveTo(next.getX() + 0.5, floor, next.getZ() + 0.5, mob.getYRot(), mob.getXRot());
		}
		path.advance();
	}
}
