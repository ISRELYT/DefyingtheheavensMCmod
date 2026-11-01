package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gathering formation's fuel (GatheringFormations): the ticks of burning its spirit stones have bought. Each second it burns it tells the
 * formations' index it is lit, and goes dark (its block's LIT) when they run out.
 */
public class GatheringFormationBlockEntity extends BlockEntity {
	/** Ticks of burning left. */
	private long fuel;

	public GatheringFormationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GATHERING_FORMATION, pos, state);
	}

	public long fuel() { return fuel; }

	/** {@code ticks} more of burning; lit at once. */
	void addFuel(long ticks) {
		fuel = Math.max(0, fuel + ticks);
		setChanged();
		if (level != null && fuel > 0) light(true);
	}

	static void serverTick(Level level, BlockPos pos, BlockState state, GatheringFormationBlockEntity formation) {
		if (formation.fuel <= 0) {
			if (state.getValue(GatheringFormationBlock.LIT)) formation.light(false);
			return;
		}
		formation.fuel--;
		if (level.getGameTime() % 20 != 0 && formation.fuel > 0) return;
		formation.setChanged();
		formation.light(formation.fuel > 0);
	}

	/** Its block lit or dark, and the index told. */
	private void light(boolean lit) {
		if (level == null || !(getBlockState().getBlock() instanceof GatheringFormationBlock block)) return;
		GatheringFormations.lit(level, worldPosition, block.kind(), block.tier(), lit);
		if (getBlockState().getValue(GatheringFormationBlock.LIT) != lit) {
			level.setBlock(worldPosition, getBlockState().setValue(GatheringFormationBlock.LIT, lit), Block.UPDATE_ALL);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (level != null && getBlockState().getBlock() instanceof GatheringFormationBlock block) {
			GatheringFormations.lit(level, worldPosition, block.kind(), block.tier(), false);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (fuel > 0) tag.putLong("Fuel", fuel);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		fuel = tag.getLong("Fuel");
	}
}
