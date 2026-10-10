package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

/** Item tooltips wrap at a narrow width, so a long line becomes a short block instead of running far off the item. */
public final class Tooltips {
	/** Characters per tooltip line. */
	public static final int WIDTH = 32;

	/** Adds {@code text}, word-wrapped to {@link #WIDTH}, keeping its style on every line. */
	public static void add(List<Component> lines, MutableComponent text) {
		String whole = text.getString();
		if (whole.length() <= WIDTH) {
			lines.add(text);
			return;
		}
		Style style = text.getStyle();
		StringBuilder line = new StringBuilder();
		for (String word : whole.split(" ")) {
			if (line.length() > 0 && line.length() + 1 + word.length() > WIDTH) {
				lines.add(Component.literal(line.toString()).withStyle(style));
				line.setLength(0);
			}
			if (line.length() > 0) line.append(' ');
			line.append(word);
		}
		if (line.length() > 0) lines.add(Component.literal(line.toString()).withStyle(style));
	}

	private Tooltips() {}
}
