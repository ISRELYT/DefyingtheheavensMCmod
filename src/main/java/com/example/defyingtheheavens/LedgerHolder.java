package com.example.defyingtheheavens;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chunk's land ledger (LandLedger), kept on the chunk itself (mixin/ChunkAccessLedgerMixin) so it is saved and loaded with it and held
 * in memory only while the chunk is.
 */
public interface LedgerHolder {
	/** Block position (BlockPos#asLong) to the state that stood there before any player changed it; null until one does. */
	Long2ObjectOpenHashMap<BlockState> dth$ledger();

	void dth$setLedger(Long2ObjectOpenHashMap<BlockState> ledger);
}
