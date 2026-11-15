package com.example.defyingtheheavens.client.compat.jei.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * JEI reads the mouse itself (for a key pressed over its list: R, U, A, and its bookmarks' dragging) in the GUI's pixels; over a framed
 * screen (HallFrame: the Heaven Altar's, a storage formation's, a seized body's) its list is laid out in the frame's, so it is given the
 * frame's mouse there, as everything else over that screen is. Only with JEI (JeiMixinPlugin); if a later JEI moves this, it is left alone.
 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.input.MouseUtil", remap = false)
public abstract class JeiMouseUtilMixin {
	@Inject(method = "getX", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
	private static void dth$frameX(CallbackInfoReturnable<Double> cir) {
		double x = HallFrame.mouseX();
		if (!Double.isNaN(x)) cir.setReturnValue(x);
	}

	@Inject(method = "getY", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
	private static void dth$frameY(CallbackInfoReturnable<Double> cir) {
		double y = HallFrame.mouseY();
		if (!Double.isNaN(y)) cir.setReturnValue(y);
	}
}
