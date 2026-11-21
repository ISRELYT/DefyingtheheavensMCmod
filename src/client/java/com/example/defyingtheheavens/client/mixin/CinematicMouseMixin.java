package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.cinematic.CinematicClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player held in their breakthrough cinematic neither turns nor scrolls (their keys: CinematicClient#swallowKeys). The mouse's movement
 * is let go of as it comes, so nothing is saved up to turn them all at once when the film hands back, and its wheel moves no hotbar slot.
 * Screens (chat, the pause menu) keep the mouse as ever.
 */
@Mixin(MouseHandler.class)
public abstract class CinematicMouseMixin {
	@Shadow
	@Final
	private Minecraft minecraft;
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void dth$heldDoesNotTurn(CallbackInfo ci) {
		if (!CinematicClient.holdsLocal()) return;
		accumulatedDX = 0;
		accumulatedDY = 0;
		ci.cancel();
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void dth$heldDoesNotScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		if (minecraft.screen == null && CinematicClient.holdsLocal()) ci.cancel();
	}
}
