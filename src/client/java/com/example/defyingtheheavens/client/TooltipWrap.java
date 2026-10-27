package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;

/**
 * The mod's items' tooltips kept on the screen (the user, 2026-10-27: the Court Merit Token's ran off its edge; vanilla never wraps a
 * tooltip's lines): any line past the name wider than {@link #WIDTH} is broken into lines that fit, each keeping its colours.
 */
final class TooltipWrap {
	/** The widest a line may run, in GUI pixels (a third of the smallest usual GUI's width, and some). */
	static final int WIDTH = 220;

	static void register() {
		ItemTooltipCallback.EVENT.register((stack, flag, lines) -> {
			if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(DefyingTheHeavens.MOD_ID)) return;
			Font font = Minecraft.getInstance().font;
			if (font == null) return;
			for (int i = 1; i < lines.size(); i++) {
				Component line = lines.get(i);
				if (font.width(line) <= WIDTH) continue;
				List<FormattedText> parts = font.getSplitter().splitLines(line, WIDTH, Style.EMPTY);
				if (parts.size() <= 1) continue;
				lines.remove(i);
				for (int k = 0; k < parts.size(); k++) lines.add(i + k, component(parts.get(k)));
				i += parts.size() - 1;
			}
		});
	}

	/** A wrapped piece of a line, as a line of its own with the styles it had. */
	private static Component component(FormattedText text) {
		MutableComponent out = Component.empty();
		text.visit((style, piece) -> {
			out.append(Component.literal(piece).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}

	private TooltipWrap() {}
}
