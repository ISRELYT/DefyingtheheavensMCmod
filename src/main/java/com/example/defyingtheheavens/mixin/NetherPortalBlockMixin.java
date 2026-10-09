package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.PortalRestrictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Portal blocks that exist anyway (commands, structures) don't start the portal timer in the high realms. */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void dth$blockPortalTravel(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (PortalRestrictionHandler.isRestricted(level)) {
			PortalRestrictionHandler.notifyBlocked(entity);
			ci.cancel();
		}
	}
}
