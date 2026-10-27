package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.ModLang;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The Dao Codex (Codex; the user's ask, 2026-10-27): the chapters down its left side as icons, the chosen chapter's pages listed beside
 * them, and the page itself on the right, in the talk wheel's lacquer and gold (TalkUi). A sealed page shows ??? and its hint, a chapter
 * with every page sealed a darkened icon and ???; a page open and not yet read wears a gold dot, and so does its chapter (CodexState).
 * Opened by its key (J) wherever one is in the world, or by the book beside the cultivation menu ({@link Book}), which it goes back to;
 * it opens on the newest page not yet read, else where it was left.
 */
public class CodexScreen extends Screen {
	private static final int WIDTH = 400; // the smallest usual GUI is 427 wide
	private static final int HEIGHT = 230; // and 240 high, as the cultivation menu is fitted
	private static final int HEADER = 20;
	private static final int BODY_TOP = 24;
	private static final int BODY_BOTTOM = HEIGHT - 8;
	/** The chapters' column: a cell for each, its icon in it. */
	private static final int CHAPTER_X = 7;
	private static final int CHAPTER_W = 22;
	private static final int CHAPTER_H = 18;
	/** The pages' list, under its chapter's name. */
	private static final int LIST_X = 35;
	private static final int LIST_W = 132;
	private static final int LIST_TOP = BODY_TOP + 17;
	private static final int ROW_H = 12;
	/** The page: its icon and title, then its text. */
	private static final int PAGE_X = 173;
	private static final int PAGE_W = WIDTH - PAGE_X - 8;
	private static final int TEXT_TOP = BODY_TOP + 23;
	private static final int LINE_H = 10;
	private static final int PARAGRAPH_GAP = 4;
	private static final int BULLET_INDENT = 8;
	/** The thin scroll rollers at a column's right, and pixels a mouse-wheel notch moves. */
	private static final int ROLLER_W = 4;
	private static final int SCROLL_STEP = 12;
	/** A dot's gold, and the shade laid over a sealed chapter's icon. */
	private static final int DOT = 0xFFF2C94C;
	private static final int DOT_RIM = 0xFF8A6A1E;
	private static final int SEALED_SHADE = 0xC0140F0C;

	/** Where the Codex was left, kept while the game runs. */
	private static String chapterShown = "start";
	private static String entryShown = "codex";

	private final Screen parent;
	private int left;
	private int top;
	private boolean placed;
	private int listScroll;
	private int listMax;
	private int pageScroll;
	private int pageMax;
	/** The page shown, laid out for its width (and which page, open or sealed, it was laid out for). */
	private List<Line> lines = List.of();
	private int linesHeight;
	private String laidOut;

	private record Line(FormattedCharSequence text, int x, int y, boolean bullet) {}

	public CodexScreen(Screen parent) {
		super(Component.translatable(ModLang.CODEX_TITLE));
		this.parent = parent;
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		if (minecraft != null) CodexState.lookNow(minecraft);
		laidOut = null;
		if (!placed) {
			placed = true;
			Codex.Entry newest = CodexState.newest();
			Codex.Entry shown = newest != null ? newest : Codex.entry(entryShown);
			select(shown != null ? shown : Codex.entries().get(0), newest != null);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false; // as the cultivation menu: meditation keeps running while one reads
	}

	@Override
	public void onClose() {
		if (minecraft != null) minecraft.setScreen(parent);
	}

	// --- Drawing ---

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + WIDTH, top + HEIGHT, TalkUi.GOLD);

		// The title between two gold rules, and how much of the Codex is open.
		int cx = left + WIDTH / 2;
		int tw = font.width(title);
		g.drawString(font, title, cx - tw / 2, top + 8, TalkUi.GOLD_TEXT, true);
		for (int side : new int[] {-1, 1}) {
			int from = side < 0 ? cx - tw / 2 - 40 : cx + tw / 2 + 8;
			g.fill(from, top + 12, from + 32, top + 13, TalkUi.GOLD_DIM);
			int nub = side < 0 ? from + 32 : from - 1;
			g.fill(nub - 1, top + 11, nub + 2, top + 14, TalkUi.GOLD);
		}
		Component count = Component.translatable(ModLang.CODEX_COUNT, CodexState.openCount(), Codex.entries().size());
		g.drawString(font, count, left + WIDTH - 10 - font.width(count), top + 8, TalkUi.MUTED, false);
		g.fill(left + 6, top + HEADER, left + WIDTH - 6, top + HEADER + 1, TalkUi.GOLD_DIM);
		// The rules between the columns.
		g.fill(left + LIST_X - 4, top + BODY_TOP, left + LIST_X - 3, top + BODY_BOTTOM, TalkUi.GOLD_DIM);
		g.fill(left + PAGE_X - 4, top + BODY_TOP, left + PAGE_X - 3, top + BODY_BOTTOM, TalkUi.GOLD_DIM);

		List<Component> tip = renderChapters(g, mouseX, mouseY);
		renderList(g, mouseX, mouseY);
		renderPage(g);
		super.render(g, mouseX, mouseY, partialTick);
		if (tip != null) g.renderComponentTooltip(font, tip, mouseX, mouseY);
	}

	/** The chapters' column; returns the tooltip of the one pointed at, if any. */
	private List<Component> renderChapters(GuiGraphics g, int mouseX, int mouseY) {
		List<Component> tip = null;
		List<Codex.Chapter> chapters = Codex.chapters();
		for (int i = 0; i < chapters.size(); i++) {
			Codex.Chapter chapter = chapters.get(i);
			int x = left + CHAPTER_X;
			int y = top + BODY_TOP + i * CHAPTER_H;
			boolean selected = chapter.id().equals(chapterShown);
			boolean hover = in(mouseX, mouseY, x, y, CHAPTER_W, CHAPTER_H - 1);
			boolean sealed = !CodexState.anyOpen(chapter);
			if (selected) {
				g.fill(x, y, x + CHAPTER_W, y + CHAPTER_H - 1, TalkUi.GOLD);
				g.fill(x + 1, y + 1, x + CHAPTER_W - 1, y + CHAPTER_H - 2, TalkUi.PANEL_LIT);
			} else if (hover) {
				g.fill(x, y, x + CHAPTER_W, y + CHAPTER_H - 1, TalkUi.PANEL_LIT);
			}
			g.renderItem(iconOf(chapter.icon()), x + 3, y);
			g.pose().pushPose();
			g.pose().translate(0, 0, 200); // over the icon
			if (sealed) g.fill(x + 3, y, x + 19, y + 16, SEALED_SHADE);
			if (CodexState.anyUnread(chapter)) dot(g, x + CHAPTER_W - 5, y + 1);
			g.pose().popPose();
			if (hover) {
				tip = sealed
						? List.of(Component.translatable(ModLang.CODEX_LOCKED), Component.translatable(ModLang.codexHint(chapter.hint().id())).withStyle(s -> s.withColor(TalkUi.MUTED)))
						: List.of(Component.translatable(chapter.titleKey()).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)),
								Component.translatable(ModLang.CODEX_COUNT, opened(chapter), chapter.entries().size()).withStyle(s -> s.withColor(TalkUi.MUTED)));
			}
		}
		return tip;
	}

	private void renderList(GuiGraphics g, int mouseX, int mouseY) {
		Codex.Chapter chapter = chapter();
		boolean sealed = !CodexState.anyOpen(chapter);
		int x = left + LIST_X;
		int y = top + BODY_TOP;
		Component name = Component.translatable(sealed ? ModLang.CODEX_LOCKED : chapter.titleKey());
		g.drawString(font, trimmed(name, LIST_W - 4), x + 2, y + 3, TalkUi.GOLD_TEXT, true);
		g.fill(x, y + 14, x + LIST_W, y + 15, TalkUi.GOLD_DIM);

		int viewTop = top + LIST_TOP;
		int viewBottom = top + BODY_BOTTOM;
		List<Codex.Entry> entries = chapter.entries();
		int content = entries.size() * ROW_H;
		listMax = Math.max(0, content - (viewBottom - viewTop));
		listScroll = Mth.clamp(listScroll, 0, listMax);
		int rowW = LIST_W - ROLLER_W - 3;
		boolean inView = mouseY >= viewTop && mouseY < viewBottom;
		g.enableScissor(x, viewTop, x + rowW, viewBottom);
		for (int i = 0; i < entries.size(); i++) {
			Codex.Entry entry = entries.get(i);
			int ry = viewTop + i * ROW_H - listScroll;
			if (ry + ROW_H <= viewTop || ry >= viewBottom) continue;
			boolean selected = entry.id().equals(entryShown);
			boolean hover = inView && in(mouseX, mouseY, x, ry, rowW, ROW_H);
			if (selected) {
				g.fill(x, ry, x + rowW, ry + ROW_H, TalkUi.PANEL_LIT);
				g.fill(x, ry, x + 1, ry + ROW_H, TalkUi.GOLD);
			} else if (hover) {
				g.fill(x, ry, x + rowW, ry + ROW_H, 0x80302418);
			}
			boolean open = CodexState.isOpen(entry);
			if (CodexState.isUnread(entry)) dot(g, x + 3, ry + 4);
			Component title = Component.translatable(open ? entry.titleKey() : ModLang.CODEX_LOCKED);
			int color = !open ? TalkUi.DISABLED : selected || hover ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM;
			g.drawString(font, trimmed(title, rowW - 11), x + 9, ry + 2, color, false);
		}
		g.disableScissor();
		roller(g, x + LIST_W - ROLLER_W, viewTop, viewBottom, content, listScroll, listMax);
	}

	private void renderPage(GuiGraphics g) {
		Codex.Entry entry = entry();
		boolean open = entry != null && CodexState.isOpen(entry);
		int x = left + PAGE_X;
		int y = top + BODY_TOP;
		if (entry != null) {
			if (open) g.renderItem(iconOf(entry.icon()), x, y);
			Component name = Component.translatable(open ? entry.titleKey() : ModLang.CODEX_LOCKED);
			g.drawString(font, trimmed(name, PAGE_W - 22), x + 20, y + 4, open ? TalkUi.GOLD_TEXT : TalkUi.DISABLED, true);
		}
		g.fill(x, y + 19, x + PAGE_W, y + 20, TalkUi.GOLD_DIM);

		layOut(entry, open);
		int viewTop = top + TEXT_TOP;
		int viewBottom = top + BODY_BOTTOM;
		pageMax = Math.max(0, linesHeight - (viewBottom - viewTop));
		pageScroll = Mth.clamp(pageScroll, 0, pageMax);
		g.enableScissor(x, viewTop, x + PAGE_W - ROLLER_W - 2, viewBottom);
		for (Line line : lines) {
			int ly = viewTop + line.y() - pageScroll;
			if (ly + LINE_H <= viewTop || ly >= viewBottom) continue;
			if (line.bullet()) g.fill(x + 2, ly + 3, x + 4, ly + 5, TalkUi.GOLD);
			g.drawString(font, line.text(), x + line.x(), ly, TalkUi.CREAM, false);
		}
		g.disableScissor();
		roller(g, x + PAGE_W - ROLLER_W, viewTop, viewBottom, linesHeight, pageScroll, pageMax);
	}

	/** The page's text wrapped to its width: paragraphs, points of a list (indented, a gold point before), headings in gold. */
	private void layOut(Codex.Entry entry, boolean open) {
		String key = entry == null ? "" : entry.id() + (open ? "" : "?");
		if (key.equals(laidOut)) return;
		laidOut = key;
		List<Line> out = new ArrayList<>();
		int y = 0;
		if (entry != null) {
			Language language = Language.getInstance();
			String text = open ? language.getOrDefault(entry.textKey()) : language.getOrDefault(ModLang.codexHint(entry.hint().id()));
			int width = PAGE_W - ROLLER_W - 6;
			for (String paragraph : text.split("\n")) {
				if (paragraph.isBlank()) {
					y += PARAGRAPH_GAP;
					continue;
				}
				boolean bullet = paragraph.startsWith("- ");
				boolean heading = paragraph.startsWith("# ");
				String body = bullet || heading ? paragraph.substring(2) : paragraph;
				int indent = bullet ? BULLET_INDENT : 0;
				int color = heading ? TalkUi.GOLD_TEXT : open ? TalkUi.CREAM : TalkUi.MUTED;
				List<FormattedCharSequence> wrapped = font.split(styled(body, color), width - indent);
				for (int i = 0; i < wrapped.size(); i++) {
					out.add(new Line(wrapped.get(i), indent, y, bullet && i == 0));
					y += LINE_H;
				}
				y += bullet ? 2 : PARAGRAPH_GAP;
			}
		}
		lines = out;
		linesHeight = y;
	}

	/** A line of a page's text, its {key}s written as the keys they are bound to now, in gold. */
	private static Component styled(String text, int color) {
		MutableComponent out = Component.empty();
		Matcher matcher = Codex.KEY.matcher(text);
		int at = 0;
		while (matcher.find()) {
			if (matcher.start() > at) out.append(Component.literal(text.substring(at, matcher.start())).withStyle(s -> s.withColor(color)));
			out.append(Component.literal(keyName(matcher.group(1))).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
			at = matcher.end();
		}
		if (at < text.length()) out.append(Component.literal(text.substring(at)).withStyle(s -> s.withColor(color)));
		return out;
	}

	/** One of Codex.KEYS, as the key it is bound to now ("G", "Left Shift"). */
	static String keyName(String name) {
		Options options = Minecraft.getInstance().options;
		KeyMapping key = switch (name) {
			case "menu" -> ModKeybinds.OPEN_MENU;
			case "meditate" -> ModKeybinds.MEDITATE;
			case "circulate" -> ModKeybinds.CIRCULATE;
			case "abilities" -> ModKeybinds.ABILITY_WHEEL;
			case "quick" -> ModKeybinds.QUICK_ABILITY;
			case "spells" -> ModKeybinds.SPELL_WHEEL;
			case "cast" -> ModKeybinds.CAST_SPELL;
			case "ring" -> ModKeybinds.STORAGE_RING;
			case "codex" -> ModKeybinds.CODEX;
			case "jump" -> options.keyJump;
			case "sneak" -> options.keyShift;
			case "sprint" -> options.keySprint;
			case "forward" -> options.keyUp;
			case "use" -> options.keyUse;
			default -> null;
		};
		return key == null ? name : key.getTranslatedKeyMessage().getString();
	}

	/** A thin roller at a column's right, only while there is more than fits: a lacquer trough between gold caps, and a gold thumb. */
	private static void roller(GuiGraphics g, int x, int viewTop, int viewBottom, int content, int scroll, int max) {
		if (max <= 0) return;
		int laneTop = viewTop + 2;
		int laneBottom = viewBottom - 2;
		int lane = laneBottom - laneTop;
		int thumb = Mth.clamp(lane * (viewBottom - viewTop) / Math.max(1, content), 10, lane);
		int thumbY = laneTop + (lane - thumb) * scroll / max;
		g.fill(x, laneTop, x + ROLLER_W, laneBottom, 0xFF6E5214);
		g.fill(x + 1, laneTop, x + ROLLER_W - 1, laneBottom, 0xFF0C0C14);
		for (int capY : new int[] {viewTop, laneBottom}) {
			g.fill(x - 1, capY, x + ROLLER_W + 1, capY + 1, TalkUi.GOLD);
			g.fill(x - 1, capY + 1, x + ROLLER_W + 1, capY + 2, 0xFF6E5214);
		}
		g.fill(x, thumbY, x + ROLLER_W, thumbY + thumb, 0xFFB8860B);
		g.fill(x, thumbY, x + ROLLER_W, thumbY + 1, 0xFFF0C850);
		g.fill(x + ROLLER_W - 1, thumbY + 1, x + ROLLER_W, thumbY + thumb, 0xFF6E5214);
	}

	/** The gold dot of something not yet read: three pixels, rimmed. */
	private static void dot(GuiGraphics g, int x, int y) {
		g.fill(x - 1, y - 1, x + 4, y + 4, DOT_RIM);
		g.fill(x, y, x + 3, y + 3, DOT);
	}

	private Component trimmed(Component text, int width) {
		if (font.width(text) <= width) return text;
		return Component.literal(font.plainSubstrByWidth(text.getString(), width - font.width("...")) + "...").withStyle(text.getStyle());
	}

	/** An icon to draw (the book, should a block have no item). */
	static ItemStack iconOf(ItemLike like) {
		ItemStack stack = new ItemStack(like);
		return stack.isEmpty() ? new ItemStack(Items.BOOK) : stack;
	}

	private static boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	// --- Choosing ---

	private Codex.Chapter chapter() {
		for (Codex.Chapter chapter : Codex.chapters()) if (chapter.id().equals(chapterShown)) return chapter;
		return Codex.chapters().get(0);
	}

	private Codex.Entry entry() {
		Codex.Entry entry = Codex.entry(entryShown);
		return entry != null && entry.chapter().equals(chapter().id()) ? entry : null;
	}

	private static int opened(Codex.Chapter chapter) {
		int open = 0;
		for (Codex.Entry entry : chapter.entries()) if (CodexState.isOpen(entry)) open++;
		return open;
	}

	/** Shows {@code entry} (read, if it is open), its chapter's list brought round to it if {@code reveal}. */
	private void select(Codex.Entry entry, boolean reveal) {
		boolean other = !entry.chapter().equals(chapterShown);
		chapterShown = entry.chapter();
		entryShown = entry.id();
		pageScroll = 0;
		laidOut = null;
		if (other) listScroll = 0;
		if (reveal || other) {
			int row = chapter().entries().indexOf(entry);
			int view = BODY_BOTTOM - LIST_TOP;
			if (row >= 0) listScroll = Mth.clamp(row * ROW_H - view / 2, 0, Math.max(0, chapter().entries().size() * ROW_H - view));
		}
		CodexState.markRead(entry);
	}

	/** A chapter, at its first page not yet read (else its first open one, else its first). */
	private void selectChapter(Codex.Chapter chapter) {
		Codex.Entry pick = null;
		for (Codex.Entry entry : chapter.entries()) if (CodexState.isUnread(entry)) {
			pick = entry;
			break;
		}
		if (pick == null) for (Codex.Entry entry : chapter.entries()) if (CodexState.isOpen(entry)) {
			pick = entry;
			break;
		}
		if (pick == null) pick = chapter.entries().get(0);
		chapterShown = chapter.id();
		listScroll = 0;
		select(pick, true);
	}

	private void turn() {
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			List<Codex.Chapter> chapters = Codex.chapters();
			for (int i = 0; i < chapters.size(); i++) {
				if (in(mouseX, mouseY, left + CHAPTER_X, top + BODY_TOP + i * CHAPTER_H, CHAPTER_W, CHAPTER_H - 1)) {
					if (!chapters.get(i).id().equals(chapterShown)) {
						selectChapter(chapters.get(i));
						turn();
					}
					return true;
				}
			}
			int viewTop = top + LIST_TOP;
			if (in(mouseX, mouseY, left + LIST_X, viewTop, LIST_W - ROLLER_W - 3, top + BODY_BOTTOM - viewTop)) {
				int row = (int) Math.floor((mouseY - viewTop + listScroll) / ROW_H);
				List<Codex.Entry> entries = chapter().entries();
				if (row >= 0 && row < entries.size()) {
					if (!entries.get(row).id().equals(entryShown)) {
						select(entries.get(row), false);
						turn();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int step = (int) Math.round(-delta * SCROLL_STEP);
		if (mouseX >= left + LIST_X && mouseX < left + PAGE_X - 4) {
			listScroll = Mth.clamp(listScroll + step, 0, listMax);
		} else if (mouseX >= left + PAGE_X - 4 && mouseX < left + WIDTH) {
			pageScroll = Mth.clamp(pageScroll + step, 0, pageMax);
		} else if (mouseX >= left && mouseX < left + LIST_X - 4) {
			step(Codex.chapters(), delta < 0 ? 1 : -1);
		}
		return true;
	}

	/** The chapter beside the one shown ({@code by} -1 or 1), for the mouse wheel over the chapters and the left and right keys. */
	private void step(List<Codex.Chapter> chapters, int by) {
		int at = chapters.indexOf(chapter());
		int next = Mth.clamp(at + by, 0, chapters.size() - 1);
		if (next != at) {
			selectChapter(chapters.get(next));
			turn();
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (ModKeybinds.CODEX.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		List<Codex.Entry> entries = chapter().entries();
		int at = entries.indexOf(entry());
		switch (keyCode) {
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
				int next = Mth.clamp(at + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1), 0, entries.size() - 1);
				if (next != at) {
					select(entries.get(next), true);
					turn();
				}
				return true;
			}
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> {
				step(Codex.chapters(), keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1);
				return true;
			}
			case GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN -> {
				int page = BODY_BOTTOM - TEXT_TOP - LINE_H;
				pageScroll = Mth.clamp(pageScroll + (keyCode == GLFW.GLFW_KEY_PAGE_UP ? -page : page), 0, pageMax);
				return true;
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	/**
	 * The book beside the cultivation menu (CultivationScreen) that opens the Codex, going back to the menu when it closes: lacquer in a
	 * gold rim, wearing the gold dot while anything open is unread.
	 */
	public static final class Book extends AbstractButton {
		public static final int SIZE = 20;
		private final Screen from;

		public Book(int x, int y, Screen from) {
			super(x, y, SIZE, SIZE, Component.translatable(ModLang.CODEX_TITLE));
			this.from = from;
			setTooltip(Tooltip.create(Component.translatable(ModLang.CODEX_BUTTON, ModKeybinds.CODEX.getTranslatedKeyMessage())));
		}

		@Override
		public void onPress() {
			Minecraft.getInstance().setScreen(new CodexScreen(from));
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
			int x = getX();
			int y = getY();
			boolean lit = isHoveredOrFocused();
			g.fill(x, y, x + width, y + height, lit ? 0xFFE6C35C : TalkUi.GOLD);
			g.fill(x + 1, y + 1, x + width - 1, y + height - 1, TalkUi.INNER);
			g.fill(x + 2, y + 2, x + width - 2, y + height - 2, lit ? TalkUi.PANEL_LIT : TalkUi.PANEL);
			g.renderItem(new ItemStack(Items.BOOK), x + 2, y + 2);
			if (CodexState.anyUnread()) {
				g.pose().pushPose();
				g.pose().translate(0, 0, 200);
				dot(g, x + width - 6, y + 3);
				g.pose().popPose();
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			defaultButtonNarrationText(output);
		}
	}
}
