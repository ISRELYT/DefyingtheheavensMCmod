package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * While a framed screen draws (HallFrame), the graphics' size is the frame's (tooltips keep to it), and a scissor's rectangle, given in
 * the frame's pixels, is turned into the window's by the frame's scale, not the GUI's.
 */
@Mixin(GuiGraphics.class)
public abstract class HallFrameGraphicsMixin {
	@Redirect(method = "guiWidth", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I"))
	private int dth$frameWidth(Window window) {
		return HallFrame.rendering ? HallFrame.width(window) : window.getGuiScaledWidth();
	}

	@Redirect(method = "guiHeight", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I"))
	private int dth$frameHeight(Window window) {
		return HallFrame.rendering ? HallFrame.height(window) : window.getGuiScaledHeight();
	}

	@Redirect(method = "applyScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()D"))
	private double dth$frameScale(Window window) {
		return HallFrame.rendering ? HallFrame.scale(window) : window.getGuiScale();
	}
}
