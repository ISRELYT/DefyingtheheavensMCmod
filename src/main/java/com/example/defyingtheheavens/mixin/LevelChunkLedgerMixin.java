package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LandLedger;
import com.example.defyingtheheavens.LedgerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every block a player's act changes, noted in its chunk's land ledger before it changes (LandLedger: the state that stood there first),
 * and what the world itself changes at or beside such a block. In a chunk no player has changed, outside a player's act, this is one
 * counter's check and a field's.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkLedgerMixin {
	@Inject(method = "setBlockState", at = @At("HEAD"))
	private void dth$noteForTheLedger(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
		if (LandLedger.recording() || ((LedgerHolder) (Object) this).dth$ledger() != null) LandLedger.setting((LevelChunk) (Object) this, pos, state);
	}
}
