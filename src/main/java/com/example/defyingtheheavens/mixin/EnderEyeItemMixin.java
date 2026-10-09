package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.PortalRestrictionHandler;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** End portal frames won't accept Eyes of Ender in the high realms, so an End portal can never be activated there. */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void dth$noFrameActivation(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (PortalRestrictionHandler.isRestricted(context.getLevel())
				&& context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.END_PORTAL_FRAME)) {
			if (!context.getLevel().isClientSide()) {
				PortalRestrictionHandler.notifyBlocked(context.getPlayer());
			}
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
