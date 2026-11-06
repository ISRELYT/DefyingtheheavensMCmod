package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LandLedger;
import com.example.defyingtheheavens.LedgerHolder;
import com.example.defyingtheheavens.RealmLedger;
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
 * counter's check and a field's. In the secret realms' dimension every change is noted too, for the realm to mend (RealmLedger): elsewhere
 * a field's check and the dimension's. A stranger's work under a raised veil stops here: it goes no further than strangers' eyes
 * (Concealments#wreck), the block as it was and the setter told it went (Level#setBlock answers true, so what they mined yields what it would).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkLedgerMixin {
	@Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
	private void dth$noteForTheLedger(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
		LevelChunk chunk = (LevelChunk) (Object) this;
		if ((LandLedger.recording() || LandLedger.showing() || ((LedgerHolder) (Object) this).dth$ledger() != null) && LandLedger.setting(chunk, pos, state)) {
			cir.setReturnValue(chunk.getBlockState(pos));
			return;
		}
		// And in the secret realms, whatever changes, to be mended as the realm next stirs (RealmLedger).
		RealmLedger.setting(chunk, pos, state);
	}
}
