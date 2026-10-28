package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.AstralRealm;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla loads no chunks round a spectator while the gamerule says so ({@code skipPlayer}); nor round one flying space faster than a
 * chunk a tick (AstralRealm#zooming: from Void Shattering on), whose empty chunks the server can't make as fast and whose making,
 * sending and saving ate its ticks. They load round it again a moment after it slows (vanilla's {@code move} sees the change).
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	@Inject(method = "skipPlayer", at = @At("RETURN"), cancellable = true)
	private void dth$theSwiftLoadNoSpace(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && AstralRealm.zooming(player)) cir.setReturnValue(true);
	}

	/** A chunk just sent to a player: what a concealment hides in it from them put right at once after it (Concealments#chunkSent). */
	@Inject(method = "playerLoadedChunk", at = @At("TAIL"))
	private void dth$veiledChunk(ServerPlayer player, org.apache.commons.lang3.mutable.MutableObject<?> packet,
			net.minecraft.world.level.chunk.LevelChunk chunk, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		com.example.defyingtheheavens.Concealments.chunkSent(player, chunk);
	}
}
