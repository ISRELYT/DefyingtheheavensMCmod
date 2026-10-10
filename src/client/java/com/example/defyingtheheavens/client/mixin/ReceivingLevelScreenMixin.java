package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.InnerRealmFade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While the eyes are closed (InnerRealmFade), the "Loading terrain" screen of the dimension change stays black. */
@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin extends Screen {
	protected ReceivingLevelScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void dth$blackWhileEyesClosed(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!InnerRealmFade.isDark()) return;
		graphics.fill(0, 0, width, height, 0xFF000000);
		ci.cancel();
	}
}
