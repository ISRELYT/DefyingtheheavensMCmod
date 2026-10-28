package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LedgerHolder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Every chunk can hold a land ledger (LandLedger), made the first time a player changes something in it. */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessLedgerMixin implements LedgerHolder {
	@Unique
	private Long2ObjectOpenHashMap<BlockState> dth$ledger;

	@Override
	public Long2ObjectOpenHashMap<BlockState> dth$ledger() {
		return dth$ledger;
	}

	@Override
	public void dth$setLedger(Long2ObjectOpenHashMap<BlockState> ledger) {
		dth$ledger = ledger;
	}
}
