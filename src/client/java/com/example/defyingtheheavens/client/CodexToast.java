package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.ModLang;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * "New in the Codex": the toast for pages of the Dao Codex as they open (CodexState), in its lacquer and gold, with the key that opens
 * it. One page is named; several at once are counted. Pages opening while it shows join it, and it shows on afresh.
 */
final class CodexToast implements Toast {
	private static final long SHOWN_MS = 5000L;

	private final List<Codex.Entry> entries = new ArrayList<>();
	private boolean changed = true;
	private long since;

	static void show(ToastComponent toasts, List<Codex.Entry> news) {
		CodexToast toast = toasts.getToast(CodexToast.class, NO_TOKEN);
		if (toast == null) {
			toast = new CodexToast();
			toast.entries.addAll(news);
			toasts.addToast(toast);
		} else {
			toast.entries.addAll(news);
			toast.changed = true;
		}
	}

	@Override
	public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
		if (changed) {
			since = time;
			changed = false;
		}
		if (entries.isEmpty()) return Visibility.HIDE;
		Font font = toasts.getMinecraft().font;
		int w = width();
		int h = height();
		g.fill(0, 0, w, h, TalkUi.GOLD);
		g.fill(1, 1, w - 1, h - 1, TalkUi.INNER);
		g.fill(2, 2, w - 2, h - 2, TalkUi.PANEL);

		boolean one = entries.size() == 1;
		g.renderFakeItem(CodexScreen.iconOf(one ? entries.get(0).icon() : Items.BOOK), 8, 8);
		Component top = Component.translatable(ModLang.CODEX_TOAST_NEW);
		g.drawString(font, top, 30, 7, TalkUi.GOLD_TEXT, false);
		// The key that opens it, at the end of the first line (when it fits there).
		Component key = Component.literal("[" + ModKeybinds.CODEX.getTranslatedKeyMessage().getString() + "]");
		if (30 + font.width(top) + 6 + font.width(key) <= w - 7) g.drawString(font, key, w - 7 - font.width(key), 7, TalkUi.MUTED, false);
		Component bottom = one ? Component.translatable(entries.get(0).titleKey()) : Component.translatable(ModLang.CODEX_TOAST_MANY, entries.size());
		g.drawString(font, font.plainSubstrByWidth(bottom.getString(), w - 36), 30, 18, TalkUi.CREAM, false);
		return time - since >= SHOWN_MS * toasts.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
	}
}
