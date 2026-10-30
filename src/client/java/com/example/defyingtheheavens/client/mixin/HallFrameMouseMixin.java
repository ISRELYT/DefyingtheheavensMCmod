package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Clicks, drags, moves and the wheel reach a framed screen (HallFrame) in the frame's pixels: its size in place of the GUI's. */
@Mixin(MouseHandler.class)
public abstract class HallFrameMouseMixin {
	@Shadow @Final private Minecraft minecraft;

	@Redirect(method = {"onPress", "onScroll", "onMove"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
	private int dth$frameWidth(Window window) {
		return minecraft.screen instanceof HallFrame.Framed ? HallFrame.width(window) : window.getGuiScaledWidth();
	}

	@Redirect(method = {"onPress", "onScroll", "onMove"}, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
	private int dth$frameHeight(Window window) {
		return minecraft.screen instanceof HallFrame.Framed ? HallFrame.height(window) : window.getGuiScaledHeight();
	}
}
