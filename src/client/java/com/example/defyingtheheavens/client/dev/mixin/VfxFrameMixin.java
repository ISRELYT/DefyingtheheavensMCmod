package com.example.defyingtheheavens.client.dev.mixin;

import com.example.defyingtheheavens.client.VfxRecorder;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The capture tool's frame grab (VfxRecorder), at the very end of the frame: after GameRenderer#render has drawn the world, the post
 * effects, a shader pack's passes, the HUD, toasts and any screen into the main render target, just before it is put on the screen. (A grab
 * at WorldRenderEvents.END came before the end-of-frame passes and missed every glow: the vault's lessons, 2026-11-18.) Dev only
 * (DevMixinPlugin): it is a mixin of its own, not a line in MinecraftMixin, so that a player's game, without the dev flag, never has it
 * applied at all (the mixin configs for Iris and JEI are switched the same way). Sodium's own hooks on runTick (HEAD) and on the blit
 * itself (its body) leave this call where it is.
 */
@Mixin(Minecraft.class)
public abstract class VfxFrameMixin {
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
	private void dth$vfxFrameEnd(boolean renderLevel, CallbackInfo ci) {
		VfxRecorder.frameEnd((Minecraft) (Object) this);
	}
}
