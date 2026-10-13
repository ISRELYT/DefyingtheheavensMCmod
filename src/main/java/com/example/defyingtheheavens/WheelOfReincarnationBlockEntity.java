package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries no saved data: it exists so the client can draw the turning Wheel of Reincarnation (WheelOfReincarnationRenderer),
 * and turn it the faster when a ghost is returned to life (the block event {@link WheelOfReincarnationBlock#EVENT_TURN}).
 */
public class WheelOfReincarnationBlockEntity extends BlockEntity {
	/** A ghost's turn sweeps the wheel round this far, on top of its slow drift. */
	private static final float TURN_DEGREES = 720.0f;

	private long turnStart = -1;
	private int turnTicks = 1;
	/** Every turn so far, so the wheel stays where the last one left it. */
	private float settled;

	public WheelOfReincarnationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WHEEL_OF_REINCARNATION, pos, state);
	}

	@Override
	public boolean triggerEvent(int id, int param) {
		if (id != WheelOfReincarnationBlock.EVENT_TURN) return super.triggerEvent(id, param);
		if (level != null) {
			settled = turnAngle(level.getGameTime());
			turnStart = level.getGameTime();
			turnTicks = Math.max(1, param);
		}
		return true;
	}

	/** Degrees the wheel's turns have added, at {@code time}: easing in and out of each turn. */
	public float turnAngle(float time) {
		if (turnStart < 0) return settled;
		float t = (time - turnStart) / turnTicks;
		if (t >= 1) {
			settled += TURN_DEGREES;
			turnStart = -1;
			return settled;
		}
		t = Math.max(0, t);
		return settled + TURN_DEGREES * t * t * (3 - 2 * t);
	}
}
