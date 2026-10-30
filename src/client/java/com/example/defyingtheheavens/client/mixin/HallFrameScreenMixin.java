package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HallFonts;
import com.example.defyingtheheavens.client.HallFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A framed screen's words (HallFrame) are in Pixelify Sans, the mockups' font (HallFonts#screenFont), in place of Minecraft's own. */
@Mixin(Screen.class)
public abstract class HallFrameScreenMixin {
	@Shadow protected Font font;

	@Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At(value = "FIELD",
			target = "Lnet/minecraft/client/gui/screens/Screen;font:Lnet/minecraft/client/gui/Font;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
	private void dth$hallFont(Minecraft minecraft, int width, int height, CallbackInfo ci) {
		if ((Object) this instanceof HallFrame.Framed) font = HallFonts.screenFont();
	}
}
