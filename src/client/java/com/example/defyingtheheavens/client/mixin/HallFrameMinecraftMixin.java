package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A framed screen (HallFrame) is laid out at the frame's size, when it opens and when the window or the GUI scale changes. */
@Mixin(Minecraft.class)
public abstract class HallFrameMinecraftMixin {
	@Redirect(method = "setScreen", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;init(Lnet/minecraft/client/Minecraft;II)V"))
	private void dth$initInFrame(Screen screen, Minecraft minecraft, int width, int height) {
		if (screen instanceof HallFrame.Framed) screen.init(minecraft, HallFrame.width(minecraft.getWindow()), HallFrame.height(minecraft.getWindow()));
		else screen.init(minecraft, width, height);
	}

	@Redirect(method = "resizeDisplay", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/Screen;resize(Lnet/minecraft/client/Minecraft;II)V"))
	private void dth$resizeInFrame(Screen screen, Minecraft minecraft, int width, int height) {
		if (screen instanceof HallFrame.Framed) screen.resize(minecraft, HallFrame.width(minecraft.getWindow()), HallFrame.height(minecraft.getWindow()));
		else screen.resize(minecraft, width, height);
	}
}
