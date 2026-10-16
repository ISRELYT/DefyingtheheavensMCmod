package com.example.defyingtheheavens.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.List;

/**
 * A message over the hotbar too wide for the screen (a stele telling what it holds, at a large GUI scale) is wrapped onto more
 * lines, each centred, going up from where vanilla's one line sits, instead of running off both edges. Not required: should
 * another mod have changed how that line is drawn, the message is left as it draws it rather than the game failing to start.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	/** Left clear at each side of a wrapped message. */
	private static final int DTH$MARGIN = 8;

	@Shadow
	private int screenWidth;

	@WrapOperation(method = "render", require = 0,
			at =@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)I"),
			slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=overlayMessage"), to = @At(value = "CONSTANT", args = "stringValue=titleAndSubtitle")))
	private int dth$wrapOverlayMessage(GuiGraphics graphics, Font font, Component text, int x, int y, int color, Operation<Integer> original) {
		int room = screenWidth - 2 * DTH$MARGIN;
		if (room <= 0 || font.width(text) <= room) return original.call(graphics, font, text, x, y, color);
		List<FormattedCharSequence> lines = font.split(text, room);
		int right = 0;
		for (int i = 0; i < lines.size(); i++) {
			FormattedCharSequence line = lines.get(i);
			int width = font.width(line);
			right = Math.max(right, graphics.drawString(font, line, -width / 2, y - (lines.size() - 1 - i) * font.lineHeight, color));
		}
		return right;
	}
}
