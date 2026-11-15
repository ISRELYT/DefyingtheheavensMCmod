package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A framed screen (HallFrame) is drawn scaled from the GUI's pixels to the frame's, the mouse given to it in the frame's. The mouse the
 * frame begins with is read in its pixels too (the first of the window's GUI sizes the frame's render reads, not the totem's after it), so
 * Fabric's screen events about the screen (other mods' drawing over it: HallFrame#frameEvents) are told the frame's mouse, as it is.
 */
@Mixin(GameRenderer.class)
public abstract class HallFrameGameRendererMixin {
	@Shadow @Final private Minecraft minecraft;

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledWidth()I", ordinal = 0))
	private int dth$frameMouseWidth(Window window) {
		return minecraft.screen instanceof HallFrame.Framed ? HallFrame.width(window) : window.getGuiScaledWidth();
	}

	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScaledHeight()I", ordinal = 0))
	private int dth$frameMouseHeight(Window window) {
		return minecraft.screen instanceof HallFrame.Framed ? HallFrame.height(window) : window.getGuiScaledHeight();
	}

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void dth$renderInFrame(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (screen instanceof HallFrame.Framed) HallFrame.render(screen, graphics, partialTick);
		else screen.renderWithTooltip(graphics, mouseX, mouseY, partialTick);
	}
}
