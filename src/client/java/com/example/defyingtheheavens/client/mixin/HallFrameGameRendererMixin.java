package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A framed screen (HallFrame) is drawn scaled from the GUI's pixels to the frame's, the mouse given to it in the frame's. */
@Mixin(GameRenderer.class)
public abstract class HallFrameGameRendererMixin {
	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltip(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void dth$renderInFrame(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (screen instanceof HallFrame.Framed) HallFrame.render(screen, graphics, partialTick);
		else screen.renderWithTooltip(graphics, mouseX, mouseY, partialTick);
	}
}
