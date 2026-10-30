package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AlchemyRecipes;
import com.example.defyingtheheavens.Codex;
import com.example.defyingtheheavens.ModLang;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * The Dao Codex (Codex; the user's ask, 2026-10-27), as its mockup in the cultivation menu's Hall draws it (the user's ask, 2026-10-30:
 * "EXACTLY the same as the artefact at all GUI scales"; drawn in the artifact's pixels, HallArt, in the Hall's frame, HallFrame): the
 * whole screen in ink with far hills along its foot; down a column at its left the Codex's name, how much of it is open, and its chapters
 * by name, the one open in jade with a gold diamond; beside them the chosen chapter's pages; and the page itself in a framed panel on the
 * right, its icon and title over a rule, its words in Pixelify Sans. A sealed page shows ??? and its hint, a chapter with every page sealed
 * ???; a page open and not yet read wears a gold square, and so does its chapter (CodexState). Opened by its key (J) wherever one is in
 * the world, or from the cultivation menu (the Hall's column of tabs, or the {@link Book} beside the narrow menu), which it goes back to;
 * it opens on the newest page not yet read, else where it was left.
 * <p>
 * A page's pictures (the user's pick, 2026-10-28) stand between its words: rows of items in gold-rimmed slots, the recipes read from the
 * game itself (the cauldron's, AlchemyRecipes; a crafting recipe, the client's recipe manager), and diagrams (CodexDiagrams, drawn in the
 * frame's pixels). Pointed at, an item shows its tooltip, and a diagram what it says of the part pointed at.
 */
public class CodexScreen extends Screen implements HallFrame.Framed {
	/** The mockup's places, in the artifact's pixels: the column of chapters (its rows from 106, 44 apart, 40 tall). */
	private static final int NAV = 264, CHAPTERS_TOP = 106, CHAPTER_ROW = 44, CHAPTER_H = 40;
	/** The list of pages (from 280, 320 across; its rows from 70, 40 apart, 36 tall) and the page's panel (from 616, 644 across). */
	private static final int LIST_X = 280, LIST_W = 320, LIST_TOP = 70, ROW = 40, ROW_H = 36, LIST_VIEW_TOP = 66;
	private static final int PAGE_X = 616, PAGE_Y = 20, PAGE_W = 644;
	/** The page's words: 24 in from its panel, 580 across (a point's 20 in), at 19 on lines of 25, 9 between paragraphs, from 106 down. */
	private static final int TEXT_IN = 24, TEXT_W = 580, TEXT_TOP = 106, BODY = 19, LINE = 25, PARAGRAPH = 9, INDENT = 20;
	/** The rule under the title, 596 across, the middle a picture's rows stand on; a picture 10 below the words before it and 16 above the next. */
	private static final int RULE_W = 596, PICTURE_ABOVE = 1, PICTURE_BELOW = 16;
	/** A picture's item slot (66 with a border of 3, its item 48), a "+" or an arrow's room, and the gap between them and their rows. */
	private static final int SLOT = 66, GLYPH_W = 27, CELL_GAP = 6;
	/** How far a notch of the mouse wheel scrolls, in the artifact's pixels. */
	private static final int SCROLL_STEP = 36;
	/** The mockup's colours: the panels' frame and ink, the jade row, a row pointed at, a slot's well. */
	private static final int FRAME = 0xFF6E5214, INK = 0xFF11161C, JADE = 0xFF1E3A32, LIT = 0xFF19202A, WELL = 0xFF0B0E12;
	/** Its words: a page's, gold, a chapter's name and a page's, what is muted, what is still sealed; a point's diamond; the scrollbar. */
	private static final int WORDS = 0xE6D3A3, GOLD_WORDS = 0xFFD54F, CHAPTER = 0xB9B2A0, NAME = 0xC9C2B0, MUTED = 0x8C7A55, SEALED = 0x6C665A;
	private static final int POINT = 0xFFB8860B, THUMB = 0xFF8C6A1E;
	/** A dot's gold, kept for the Book (beside the narrow menu) and the Hall's Codex tab (CultivationScreen). */
	private static final int DOT = 0xFFF2C94C;
	private static final int DOT_RIM = 0xFF8A6A1E;

	/** Where the Codex was left, kept while the game runs. */
	private static String chapterShown = "start";
	private static String entryShown = "codex";

	private final Screen parent;
	/** How far right of the mockup's places the list and the page stand in a frame wider than its 1280; the chapters' rows' step. */
	private float ox;
	private float chapterRow = CHAPTER_ROW;
	private boolean placed;
	private int listScroll;
	private int listMax;
	private int pageScroll;
	private int pageMax;
	/** The page shown, laid out (in the artifact's pixels; and which page, open or sealed, at which size): its words and its pictures. */
	private List<Line> lines = List.of();
	private List<Picture> pictures = List.of();
	private int linesHeight;
	private String laidOut;
	/** What the mouse points at on the page, for its tooltip: an item of a picture, or a part of a diagram. */
	private ItemStack hoverStack;
	private List<FormattedCharSequence> hoverTip;

	private record Line(FormattedCharSequence text, int x, int y, boolean bullet) {}

	/** A picture between the words (Codex: "@items", "@brew", "@craft", "@diagram"): its cells in rows, or a diagram (CodexDiagrams). */
	private record Picture(int y, int height, List<Cell> cells, String diagram) {}

	/**
	 * A cell of a picture, where it sits in it: an item (several in turn where a recipe takes any of them, as any wool), or a glyph
	 * between them ('+', or '>' for the arrow).
	 */
	private record Cell(int x, int y, List<ItemStack> stacks, char glyph) {
		int width() {
			return glyph == 0 ? SLOT : GLYPH_W;
		}
	}

	public CodexScreen(Screen parent) {
		super(Component.translatable(ModLang.CODEX_TITLE));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (minecraft != null) CodexState.lookNow(minecraft);
		// The mockup's 1280 x 720 in the frame (three of its pixels to one of the frame's): the list and the page kept together in the
		// middle of a wider one, the chapters' rows closer where a frame is too short for them all.
		ox = Math.max(0, (width * 3f - HallArt.W) / 2);
		chapterRow = Math.min(CHAPTER_ROW, (height * 3f - CHAPTERS_TOP - 6) / Math.max(1, Codex.chapters().size()));
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
		float mx = mouseX * 3f, my = mouseY * 3f; // the mouse in the artifact's pixels
		hoverStack = null;
		hoverTip = null;
		List<Component> tip;
		HallArt.beginFramed(g);
		try {
			float w = width * 3f, h = height * 3f;
			HallArt.rect(g, 0, 0, w, h, 0xFF0C0F13);
			HallArt.ridge(g, CultivationScreen.FAR_HILLS, w / HallArt.W, h - 220, h, 0xFF11161C);
			HallArt.ridge(g, CultivationScreen.NEAR_HILLS, w / HallArt.W, h - 220, h, 0xFF0E1318);
			tip = renderChapters(g, mx, my);
			renderList(g, mx, my);
			renderPage(g, mx, my, mouseX, mouseY);
		} finally {
			HallArt.end(g);
		}
		super.render(g, mouseX, mouseY, partialTick);
		if (tip != null) g.renderComponentTooltip(font, tip, mouseX, mouseY);
		else if (hoverStack != null) g.renderTooltip(font, hoverStack, mouseX, mouseY);
		else if (hoverTip != null) g.renderTooltip(font, hoverTip, mouseX, mouseY);
	}

	/**
	 * The column down the left: the Codex's name and how much of it is open over a rule, then each chapter by name, the one open in jade
	 * with a gold diamond, one sealed ???, one with a page unread the gold square. Returns the tooltip of what is pointed at, if anything.
	 */
	private List<Component> renderChapters(GuiGraphics g, float mx, float my) {
		float h = height * 3f;
		HallArt.rect(g, 0, 0, NAV - 3, h, INK);
		HallArt.rect(g, NAV - 3, 0, NAV, h, FRAME);
		HallArt.text(g, title.getString(), 30, 26, 26, 26, GOLD_WORDS, false);
		HallArt.text(g, Component.translatable(ModLang.CODEX_COUNT, CodexState.openCount(), Codex.entries().size()).getString(), 30, 60, 17, 22, MUTED, false);
		HallArt.rect(g, 24, 94, 240, 97, FRAME);
		List<Component> tip = null;
		List<Codex.Chapter> chapters = Codex.chapters();
		for (int i = 0; i < chapters.size(); i++) {
			Codex.Chapter chapter = chapters.get(i);
			float top = CHAPTERS_TOP + i * chapterRow;
			boolean selected = chapter.id().equals(chapterShown), hover = in(mx, my, 0, top, NAV - 3, CHAPTER_H);
			boolean sealed = !CodexState.anyOpen(chapter), unread = CodexState.anyUnread(chapter);
			if (selected) HallArt.rect(g, 0, top, NAV - 3, top + CHAPTER_H, JADE);
			else if (hover) HallArt.rect(g, 0, top, NAV - 3, top + CHAPTER_H, LIT);
			if (selected) HallArt.diamond(g, 22, top + 14, 12, 0xFF000000 | GOLD_WORDS);
			// A long name a little smaller (down to 15) before it is cut short, clear of the gold square.
			String name = sealed ? Component.translatable(ModLang.CODEX_LOCKED).getString() : Component.translatable(chapter.titleKey()).getString();
			float room = (unread ? 232 : 250) - 48, size = HallArt.fitSize(name, room, 20, 15);
			HallArt.text(g, HallArt.fit(name, room, size), 48, top + 9, size, 22, sealed ? SEALED : selected ? GOLD_WORDS : hover ? WORDS : CHAPTER, false);
			if (unread) HallArt.rect(g, 236, top + 15, 245, top + 24, 0xFF000000 | GOLD_WORDS);
			if (hover) {
				tip = sealed
						? List.of(Component.translatable(ModLang.CODEX_LOCKED), Component.translatable(ModLang.codexHint(chapter.hint().id())).withStyle(s -> s.withColor(TalkUi.MUTED)))
						: List.of(Component.translatable(chapter.titleKey()).withStyle(s -> s.withColor(GOLD_WORDS)),
								Component.translatable(ModLang.CODEX_COUNT, opened(chapter), chapter.entries().size()).withStyle(s -> s.withColor(TalkUi.MUTED)));
			}
		}
		return tip;
	}

	/**
	 * The chapter's pages beside the column: its name over a rule, then its pages, the one open in jade with a gold diamond; under them,
	 * while they all fit, what opens a page still sealed (the one pointed at, else the one shown, else the first).
	 */
	private void renderList(GuiGraphics g, float mx, float my) {
		Codex.Chapter chapter = chapter();
		boolean sealed = !CodexState.anyOpen(chapter);
		float x = LIST_X + ox, bottom = listBottom();
		String name = Component.translatable(sealed ? ModLang.CODEX_LOCKED : chapter.titleKey()).getString();
		HallArt.text(g, HallArt.fit(name, 300, 20), x + 8, 26, 20, 22, MUTED, false);
		HallArt.rect(g, x + 8, 56, x + 312, 59, FRAME);

		List<Codex.Entry> entries = chapter.entries();
		int content = entries.size() * ROW;
		listMax = Math.max(0, Math.round(content - (bottom - LIST_TOP)));
		listScroll = Mth.clamp(listScroll, 0, listMax);
		float rowW = listRowW();
		boolean inView = my >= LIST_VIEW_TOP && my < bottom;
		Codex.Entry firstSealed = null, hoverSealed = null;
		scissor(g, x, LIST_VIEW_TOP, x + LIST_W, bottom);
		for (int i = 0; i < entries.size(); i++) {
			Codex.Entry entry = entries.get(i);
			float top = LIST_TOP + i * ROW - listScroll;
			boolean selected = entry.id().equals(entryShown), hover = inView && in(mx, my, x, top, rowW, ROW_H);
			boolean open = CodexState.isOpen(entry);
			if (!open && firstSealed == null) firstSealed = entry;
			if (!open && hover) hoverSealed = entry;
			if (top + ROW_H <= LIST_VIEW_TOP || top >= bottom) continue;
			if (selected) HallArt.rect(g, x, top, x + rowW, top + ROW_H, JADE);
			else if (hover) HallArt.rect(g, x, top, x + rowW, top + ROW_H, LIT);
			if (selected) HallArt.diamond(g, x + 6, top + 14, 8, 0xFF000000 | GOLD_WORDS);
			else if (CodexState.isUnread(entry)) HallArt.rect(g, x + 6, top + 14, x + 14, top + 22, 0xFF000000 | GOLD_WORDS);
			String title = Component.translatable(open ? entry.titleKey() : ModLang.CODEX_LOCKED).getString();
			float size = HallArt.fitSize(title, rowW - 32, 19, 16);
			HallArt.text(g, HallArt.fit(title, rowW - 32, size), x + 24, top + 7, size, 22, !open ? SEALED : selected ? GOLD_WORDS : hover ? WORDS : NAME, false);
		}
		g.disableScissor();
		if (listMax > 0) {
			scrollbar(g, x + LIST_W - 6, LIST_VIEW_TOP, bottom, content, listScroll, listMax);
			return;
		}
		Codex.Entry shown = entry();
		Codex.Entry note = hoverSealed != null ? hoverSealed : shown != null && !CodexState.isOpen(shown) ? shown : firstSealed;
		if (note == null) return;
		Component words = Component.translatable(ModLang.CODEX_SEALED_NOTE, Component.translatable(ModLang.codexHint(note.hint().id())));
		float y = LIST_TOP + content - (ROW - ROW_H) + 10;
		for (FormattedCharSequence line : HallArt.wrap(words, 16, 300)) {
			if (y + 20 > bottom) break;
			HallArt.line(g, line, x + 8, y, 16, 20, SEALED);
			y += 20;
		}
	}

	/** The foot of the pages' list, and how wide its rows are: the whole column, short of the scrollbar while there is one. */
	private float listBottom() {
		return height * 3f - 8;
	}

	private float listRowW() {
		return LIST_W - (listMax > 0 ? 12 : 0);
	}

	/**
	 * The page in its panel: its icon and its title in 30 over a rule; then its words in 19 and its pictures, rolled by the wheel under a
	 * thin gold scrollbar; a sealed page ??? and its hint. Diagrams are drawn in the frame's pixels (CodexDiagrams), the mouse given there.
	 */
	private void renderPage(GuiGraphics g, float mx, float my, int frameMouseX, int frameMouseY) {
		float px = PAGE_X + ox, h = height * 3f, textX = px + TEXT_IN;
		HallArt.box(g, px, PAGE_Y, PAGE_W, h - 2 * PAGE_Y, 3, FRAME, INK);
		Codex.Entry entry = entry();
		boolean open = entry != null && CodexState.isOpen(entry);
		if (entry != null) {
			float tx = textX;
			if (open) {
				HallArt.item(g, iconOf(entry.icon()), textX, 38, 42, false);
				tx = px + 78;
			}
			String name = Component.translatable(open ? entry.titleKey() : ModLang.CODEX_LOCKED).getString();
			HallArt.text(g, HallArt.fit(name, px + PAGE_W - TEXT_IN - tx, 30), tx, 44, 30, 30, open ? GOLD_WORDS : SEALED, false);
		}
		HallArt.rect(g, textX, 92, px + PAGE_W - TEXT_IN, 95, FRAME);

		layOut(entry, open);
		float top = TEXT_TOP, bottom = h - 34;
		pageMax = Math.max(0, Math.round(linesHeight - (bottom - top)));
		pageScroll = Mth.clamp(pageScroll, 0, pageMax);
		// Only what shows can be pointed at: the mouse outside the page's view points at nothing on it.
		boolean inView = in(mx, my, px + 3, top, PAGE_W - 30, bottom - top);
		float pmx = inView ? mx : Float.MIN_VALUE / 2, pmy = inView ? my : Float.MIN_VALUE / 2;
		scissor(g, px + 3, top, px + PAGE_W - 22, bottom);
		for (Line line : lines) {
			float ly = top + line.y() - pageScroll;
			if (ly + LINE <= top || ly >= bottom) continue;
			if (line.bullet()) HallArt.diamond(g, textX, ly + 9, 8, POINT);
			HallArt.line(g, line.text(), textX + line.x(), ly, BODY, LINE, WORDS);
		}
		for (Picture picture : pictures) {
			float py = top + picture.y() - pageScroll;
			if (py + picture.height() <= top || py >= bottom) continue;
			if (picture.diagram() != null) {
				// Diagrams draw in the frame's pixels (CodexDiagrams), as they always have.
				HallArt.end(g);
				try {
					List<FormattedCharSequence> tip = CodexDiagrams.draw(g, font, picture.diagram(), Math.round(textX / 3), Math.round(py / 3), TEXT_W / 3,
							inView ? frameMouseX : Integer.MIN_VALUE / 2, inView ? frameMouseY : Integer.MIN_VALUE / 2);
					if (tip != null) hoverTip = tip;
				} finally {
					HallArt.beginFramed(g);
				}
			}
			for (Cell cell : picture.cells()) {
				float cx = textX + cell.x(), cy = py + cell.y();
				if (cell.glyph() != 0) {
					glyph(g, cell.glyph(), cx, cy);
					continue;
				}
				ItemStack stack = cell.stacks().get((int) (Util.getMillis() / 1000 % cell.stacks().size()));
				boolean hover = in(pmx, pmy, cx, cy, SLOT, SLOT);
				HallArt.box(g, cx, cy, SLOT, SLOT, 3, hover ? 0xFF000000 | GOLD_WORDS : FRAME, hover ? LIT : WELL);
				HallArt.item(g, stack, cx + 9, cy + 9, 48, true);
				if (hover) hoverStack = stack;
			}
		}
		g.disableScissor();
		if (pageMax > 0) scrollbar(g, px + PAGE_W - 24, top, bottom, linesHeight, pageScroll, pageMax);
	}

	/** A scissor round (x0, y0) to (x1, y1), given in the artifact's pixels (the frame's three to one: HallFrame scales it on). */
	private static void scissor(GuiGraphics g, float x0, float y0, float x1, float y1) {
		g.enableScissor(Math.round(x0 / 3), Math.round(y0 / 3), Math.round(x1 / 3), Math.round(y1 / 3));
	}

	/** The mockup's scrollbar: a dark trough 6 across, a gold thumb. */
	private static void scrollbar(GuiGraphics g, float x, float top, float bottom, int content, int scroll, int max) {
		float lane = bottom - top, thumb = Math.max(36, lane * lane / Math.max(1, content)), y = top + (lane - thumb) * scroll / Math.max(1, max);
		HallArt.rect(g, x, top, x + 6, bottom, WELL);
		HallArt.rect(g, x, y, x + 6, y + thumb, THUMB);
	}

	/** A glyph between a picture's items: a gold "+", or a gold arrow pointing to what they make. */
	private void glyph(GuiGraphics g, char glyph, float x, float y) {
		if (glyph == '+') {
			HallArt.textCentred(g, "+", x + GLYPH_W / 2f, y + 21, 24, 24, GOLD_WORDS, false);
			return;
		}
		int gold = 0xFF000000 | GOLD_WORDS;
		HallArt.rect(g, x + 3, y + 30, x + 18, y + 36, gold);
		HallArt.rect(g, x + 15, y + 24, x + 18, y + 42, gold);
		HallArt.rect(g, x + 18, y + 27, x + 21, y + 39, gold);
		HallArt.rect(g, x + 21, y + 30, x + 24, y + 36, gold);
	}

	/**
	 * The page's text wrapped to its width (in the artifact's pixels, at 19): paragraphs, points of a list (indented, a gold diamond
	 * before), headings in gold; and its pictures between them, where their lines stand.
	 */
	private void layOut(Codex.Entry entry, boolean open) {
		String key = (entry == null ? "" : entry.id() + (open ? "" : "?")) + "@" + Math.round(HallArt.unit() * 1000);
		if (key.equals(laidOut)) return;
		laidOut = key;
		List<Line> out = new ArrayList<>();
		List<Picture> drawn = new ArrayList<>();
		int y = 0;
		if (entry != null) {
			Language language = Language.getInstance();
			String text = open ? language.getOrDefault(entry.textKey()) : language.getOrDefault(ModLang.codexHint(entry.hint().id()));
			for (String paragraph : text.split("\n")) {
				if (paragraph.isBlank()) {
					y += PARAGRAPH;
					continue;
				}
				if (Codex.pictureKind(paragraph) != null) {
					Picture picture = open ? picture(paragraph, y + PICTURE_ABOVE) : null;
					if (picture != null) {
						drawn.add(picture);
						y += PICTURE_ABOVE + picture.height() + PICTURE_BELOW;
					}
					continue;
				}
				boolean bullet = paragraph.startsWith("- ");
				boolean heading = paragraph.startsWith("# ");
				String body = bullet || heading ? paragraph.substring(2) : paragraph;
				int indent = bullet ? INDENT : 0;
				int color = heading ? GOLD_WORDS : open ? WORDS : NAME;
				List<FormattedCharSequence> wrapped = HallArt.wrap(styled(body, color), BODY, TEXT_W - indent);
				for (int i = 0; i < wrapped.size(); i++) {
					out.add(new Line(wrapped.get(i), indent, y, bullet && i == 0));
					y += LINE;
				}
				y += PARAGRAPH;
			}
		}
		lines = out;
		pictures = drawn;
		linesHeight = y;
	}

	/**
	 * A picture's line laid out at {@code y}: a diagram, or its cells in rows as wide as the words, each row centred; null if it has
	 * nothing to show (a recipe this game doesn't know). A diagram's height is the frame's three to one.
	 */
	private Picture picture(String line, int y) {
		String kind = Codex.pictureKind(line);
		List<String> parts = Codex.pictureParts(line);
		if (kind.equals("diagram")) {
			return parts.size() == 1 && Codex.DIAGRAMS.contains(parts.get(0)) ? new Picture(y, 3 * CodexDiagrams.height(parts.get(0)), List.of(), parts.get(0)) : null;
		}
		if (kind.equals("beast")) {
			// The creature in its frame, and what it leaves in a row under it.
			ResourceLocation id = parts.size() == 1 ? Codex.pictureId(parts.get(0)) : null;
			if (id == null) return null;
			String diagram = CodexDiagrams.BEAST + id;
			int height = 3 * CodexDiagrams.beastHeight(font, id.toString(), TEXT_W / 3);
			List<Cell> drops = new ArrayList<>();
			for (List<ItemStack> stacks : CodexDiagrams.drops(id.toString())) drops.add(item(stacks));
			if (drops.isEmpty()) return new Picture(y, height - 12, List.of(), diagram);
			Picture row = rows(drops, y, height);
			return new Picture(y, row.height(), row.cells(), diagram);
		}
		List<Cell> flow = switch (kind) {
			case "items" -> named(parts);
			case "brew" -> parts.size() == 1 ? brewed(Codex.pictureItem(parts.get(0))) : List.of();
			case "craft" -> parts.size() == 1 ? crafted(Codex.pictureId(parts.get(0))) : List.of();
			default -> List.of();
		};
		if (flow.isEmpty()) return null;
		return rows(flow, y, 0);
	}

	/** {@code flow}'s cells in rows as wide as the words, each row centred, the first {@code top} below the picture's top. */
	private static Picture rows(List<Cell> flow, int y, int top) {
		// Into lines, a new one wherever the next cell wouldn't fit; then each line centred.
		List<List<Cell>> lines = new ArrayList<>();
		List<Cell> line = new ArrayList<>();
		int lineWidth = 0;
		for (Cell cell : flow) {
			int needs = (line.isEmpty() ? 0 : CELL_GAP) + cell.width();
			if (!line.isEmpty() && lineWidth + needs > TEXT_W) {
				lines.add(line);
				line = new ArrayList<>();
				lineWidth = 0;
				needs = cell.width();
			}
			line.add(cell);
			lineWidth += needs;
		}
		lines.add(line);
		List<Cell> cells = new ArrayList<>();
		for (int r = 0; r < lines.size(); r++) {
			List<Cell> those = lines.get(r);
			int width = -CELL_GAP;
			for (Cell cell : those) width += cell.width() + CELL_GAP;
			int cx = (RULE_W - width) / 2;
			for (Cell cell : those) {
				cells.add(new Cell(cx, top + r * (SLOT + CELL_GAP), cell.stacks(), cell.glyph()));
				cx += cell.width() + CELL_GAP;
			}
		}
		return new Picture(y, top + lines.size() * (SLOT + CELL_GAP) - CELL_GAP, cells, null);
	}

	/** "@items": each item named, how many after "*", with its "+" and arrows. */
	private static List<Cell> named(List<String> parts) {
		List<Cell> flow = new ArrayList<>();
		for (String part : parts) {
			if (part.equals("+") || part.equals(">")) flow.add(new Cell(0, 0, List.of(), part.charAt(0)));
			else {
				Item item = Codex.pictureItem(part);
				if (item != null) flow.add(item(List.of(new ItemStack(item, Math.max(1, Codex.pictureCount(part))))));
			}
		}
		return flow;
	}

	/** "@brew": the cauldron's recipe for {@code output}, its ingredients with "+" between, an arrow, and the pill. */
	private static List<Cell> brewed(Item output) {
		AlchemyRecipes.Recipe recipe = output == null ? null : AlchemyRecipes.forOutput(output);
		if (recipe == null) return List.of();
		List<Cell> flow = new ArrayList<>();
		for (Map.Entry<Item, Integer> ingredient : recipe.ingredients().entrySet()) {
			if (!flow.isEmpty()) flow.add(new Cell(0, 0, List.of(), '+'));
			flow.add(item(List.of(new ItemStack(ingredient.getKey(), ingredient.getValue()))));
		}
		flow.add(new Cell(0, 0, List.of(), '>'));
		flow.add(item(List.of(new ItemStack(recipe.output()))));
		return flow;
	}

	/** "@craft": a crafting recipe as this game has it, alike ingredients counted together in the order they come, an arrow, the result. */
	private static List<Cell> crafted(ResourceLocation id) {
		Minecraft mc = Minecraft.getInstance();
		Recipe<?> recipe = id == null || mc.level == null ? null : mc.level.getRecipeManager().byKey(id).orElse(null);
		if (recipe == null) return List.of();
		Map<String, List<ItemStack>> kinds = new LinkedHashMap<>();
		Map<String, Integer> counts = new HashMap<>();
		for (Ingredient ingredient : recipe.getIngredients()) {
			ItemStack[] options = ingredient.getItems();
			if (options.length == 0) continue;
			StringBuilder key = new StringBuilder();
			for (ItemStack option : options) key.append(BuiltInRegistries.ITEM.getKey(option.getItem())).append(',');
			kinds.putIfAbsent(key.toString(), List.of(options));
			counts.merge(key.toString(), 1, Integer::sum);
		}
		ItemStack result = recipe.getResultItem(mc.level.registryAccess());
		if (kinds.isEmpty() || result.isEmpty()) return List.of();
		List<Cell> flow = new ArrayList<>();
		for (Map.Entry<String, List<ItemStack>> kind : kinds.entrySet()) {
			if (!flow.isEmpty()) flow.add(new Cell(0, 0, List.of(), '+'));
			List<ItemStack> stacks = new ArrayList<>();
			for (ItemStack option : kind.getValue()) stacks.add(option.copyWithCount(counts.get(kind.getKey())));
			flow.add(item(stacks));
		}
		flow.add(new Cell(0, 0, List.of(), '>'));
		flow.add(item(List.of(result.copy())));
		return flow;
	}

	private static Cell item(List<ItemStack> stacks) {
		return new Cell(0, 0, stacks, (char) 0);
	}

	/** A line of a page's text, its {key}s written as the keys they are bound to now, in gold. */
	private static Component styled(String text, int color) {
		MutableComponent out = Component.empty();
		Matcher matcher = Codex.KEY.matcher(text);
		int at = 0;
		while (matcher.find()) {
			if (matcher.start() > at) out.append(Component.literal(text.substring(at, matcher.start())).withStyle(s -> s.withColor(color)));
			out.append(Component.literal(keyName(matcher.group(1))).withStyle(s -> s.withColor(GOLD_WORDS)));
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

	/**
	 * A thin roller at a column's right, only while there is more than fits: a lacquer trough between gold caps, and a gold thumb. The mission
	 * board's screen (MissionBoardScreen) rolls its columns with it.
	 */
	static void roller(GuiGraphics g, int x, int viewTop, int viewBottom, int content, int scroll, int max) {
		if (max <= 0) return;
		int laneTop = viewTop + 2;
		int laneBottom = viewBottom - 2;
		int lane = laneBottom - laneTop;
		int thumb = Mth.clamp(lane * (viewBottom - viewTop) / Math.max(1, content), 10, lane);
		int thumbY = laneTop + (lane - thumb) * scroll / max;
		g.fill(x, laneTop, x + 4, laneBottom, 0xFF6E5214);
		g.fill(x + 1, laneTop, x + 3, laneBottom, 0xFF0C0C14);
		for (int capY : new int[] {viewTop, laneBottom}) {
			g.fill(x - 1, capY, x + 5, capY + 1, TalkUi.GOLD);
			g.fill(x - 1, capY + 1, x + 5, capY + 2, 0xFF6E5214);
		}
		g.fill(x, thumbY, x + 4, thumbY + thumb, 0xFFB8860B);
		g.fill(x, thumbY, x + 4, thumbY + 1, 0xFFF0C850);
		g.fill(x + 3, thumbY + 1, x + 4, thumbY + thumb, 0xFF6E5214);
	}

	/** The gold dot of something not yet read: three pixels, rimmed (the Book beside the narrow menu wears it). */
	static void dot(GuiGraphics g, int x, int y) {
		g.fill(x - 1, y - 1, x + 4, y + 4, DOT_RIM);
		g.fill(x, y, x + 3, y + 3, DOT);
	}

	/** An icon to draw (the book, should a block have no item). */
	static ItemStack iconOf(ItemLike like) {
		ItemStack stack = new ItemStack(like);
		return stack.isEmpty() ? new ItemStack(Items.BOOK) : stack;
	}

	private static boolean in(double mouseX, double mouseY, double x, double y, double w, double h) {
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
			float view = listBottom() - LIST_TOP;
			if (row >= 0) listScroll = Mth.clamp(Math.round(row * ROW - view / 2), 0, Math.max(0, Math.round(chapter().entries().size() * ROW - view)));
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
		double mx = mouseX * 3, my = mouseY * 3; // the artifact's pixels
		if (button == 0) {
			List<Codex.Chapter> chapters = Codex.chapters();
			for (int i = 0; i < chapters.size(); i++) {
				if (in(mx, my, 0, CHAPTERS_TOP + i * chapterRow, NAV - 3, CHAPTER_H)) {
					if (!chapters.get(i).id().equals(chapterShown)) {
						selectChapter(chapters.get(i));
						turn();
					}
					return true;
				}
			}
			float x = LIST_X + ox;
			if (in(mx, my, x, LIST_VIEW_TOP, listRowW(), listBottom() - LIST_VIEW_TOP)) {
				int row = (int) Math.floor((my - LIST_TOP + listScroll) / ROW);
				List<Codex.Entry> entries = chapter().entries();
				if (row >= 0 && row < entries.size() && my - (LIST_TOP + row * ROW - listScroll) < ROW_H) {
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
		double mx = mouseX * 3;
		int step = (int) Math.round(-delta * SCROLL_STEP);
		if (mx < NAV) {
			step(Codex.chapters(), delta < 0 ? 1 : -1);
		} else if (mx < PAGE_X + ox) {
			listScroll = Mth.clamp(listScroll + step, 0, listMax);
		} else {
			pageScroll = Mth.clamp(pageScroll + step, 0, pageMax);
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
				int page = Math.round(height * 3f - 34 - TEXT_TOP - LINE);
				pageScroll = Mth.clamp(pageScroll + (keyCode == GLFW.GLFW_KEY_PAGE_UP ? -page : page), 0, pageMax);
				return true;
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
	}

	/**
	 * The book beside the narrow cultivation menu (CultivationScreen) that opens the Codex, going back to the menu when it closes: lacquer
	 * in a gold rim, wearing the gold dot while anything open is unread.
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
