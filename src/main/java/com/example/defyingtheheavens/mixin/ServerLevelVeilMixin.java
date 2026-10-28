package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Concealments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particles and the cracks of a breaking block from within a concealment reach only those it doesn't hide from (Concealments). */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVeilMixin {
	@Inject(method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z", at = @At("HEAD"),
			cancellable = true)
	private void dth$veiledParticles(ServerPlayer player, boolean force, double x, double y, double z, Packet<?> packet, CallbackInfoReturnable<Boolean> cir) {
		if (Concealments.hiding((ServerLevel) (Object) this, player, x, y, z) != null) cir.setReturnValue(false);
	}

	@Inject(method = "destroyBlockProgress", at = @At("HEAD"), cancellable = true)
	private void dth$veiledCracks(int breaker, BlockPos pos, int progress, CallbackInfo ci) {
		if (Concealments.cracks((ServerLevel) (Object) this, breaker, pos, progress)) ci.cancel();
	}
}
