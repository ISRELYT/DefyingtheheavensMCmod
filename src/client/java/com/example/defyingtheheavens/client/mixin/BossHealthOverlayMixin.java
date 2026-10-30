package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.HudCorner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

/**
 * Vanilla's boss bars kept clear of the top-left stack (the user, 2026-10-26: "the qi bar covers the boss health bar"): at a large GUI
 * scale the centred bars run under the stack's qi bar, which is drawn over them. Moved whole, bars and names, as HudCorner#bossShift
 * says: right, beside the stack, or down, under it where the screen is too narrow.
 */
@Mixin(BossHealthOverlay.class)
public abstract class BossHealthOverlayMixin {
	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final Map<UUID, LerpingBossEvent> events;

	@Inject(method = "render", at = @At("HEAD"))
	private void dth$keepClearOfTheStack(GuiGraphics graphics, CallbackInfo ci) {
		HudCorner.nextFrame();
		int widest = 0;
		for (LerpingBossEvent event : events.values()) widest = Math.max(widest, minecraft.font.width(event.getName()));
		int[] shift = HudCorner.bossShift(graphics.guiWidth(), graphics.guiHeight(), events.size(), widest);
		graphics.pose().pushPose();
		graphics.pose().translate(shift[0], shift[1], 0);
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void dth$keptClear(GuiGraphics graphics, CallbackInfo ci) {
		graphics.pose().popPose();
	}
}
