package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LightLayer;

/**
 * Turtle Breath's deep seclusion (CultivationMethod#TURTLE_BREATH; the methods overhaul's phase 2): it gathers only in the Inner Realm, or
 * shut away from the world, where no light of the sky reaches one's eyes and stone (any solid block) closes round within {@link #WALLS}
 * blocks on every side and above (a sealed cave, a shut room). Half the pace, a third of the years (CultivationMethods#TURTLE_PACE), and that
 * third its own: nothing outside it saves more years (a mat's, a pedestal's, a stone's, the Inner Realm's: MeditationManager), only denser
 * qi. So the talented without the aids find their years stretched, and a low talent with every aid is no further for it (the talent retune
 * of phase 2: x3 on top of the aids took a Low talent with everything to Void Shattering).
 */
public final class TurtleBreath {
	/** How near the walls and roof must close round one. */
	public static final int WALLS = 6;

	/** Whether {@code p} meditates in deep seclusion. */
	public static boolean secluded(ServerPlayer p) {
		if (InnerRealm.isInside(p)) return true;
		ServerLevel level = p.serverLevel();
		BlockPos eyes = BlockPos.containing(p.getEyePosition());
		if (level.dimensionType().hasSkyLight() && level.getBrightness(LightLayer.SKY, eyes) > 0) return false;
		for (Direction side : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			if (!closed(level, eyes, side)) return false;
		}
		return true;
	}

	/** Whether a solid block stands within {@link #WALLS} blocks of {@code from} toward {@code side}. */
	private static boolean closed(ServerLevel level, BlockPos from, Direction side) {
		BlockPos.MutableBlockPos at = from.mutable();
		for (int i = 1; i <= WALLS; i++) {
			at.move(side);
			if (level.getBlockState(at).isSolidRender(level, at)) return true;
		}
		return false;
	}

	private TurtleBreath() {}
}
