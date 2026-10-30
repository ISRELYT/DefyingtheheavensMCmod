package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.SectMissions;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A sect's mission board (SectMissions; S2C mission_board), in the Codex's lacquer and gold: its heading, the disciple's title and
 * Contribution, and two halves. The missions: those they hold, then the day's postings, each a row with its thing and its grade and pay; the
 * one chosen in full beside them, with what may be done with it now (take it; hand it in, a new slip, abandon it). The exchange (SectExchange):
 * its wares, each with its price and what is left of it today (those their title doesn't open, dimmed); the one chosen beside them, with its
 * own description. What is done is sent (C2S mission_act), and the page comes back with what came of it at its foot.
 */
public class MissionBoardScreen extends Screen implements HallFrame.Framed {
	private static final int W = 400, H = 230, HEADER = 20, TABS_Y = 25, BODY_TOP = 42, BODY_BOTTOM = H - 22, LIST_X = 8, LIST_W = 170,
			PANE_X = 186, PANE_W = W - PANE_X - 8, ROW_H = 20, LABEL_H = 12, LINE_H = 10, BUTTON_H = 14, ROLLER_W = 4, SCROLL_STEP = 12;
	private static final int EASY = 0x8FD88F, HARD = 0xE0806A;

	/** One mission on the page: its id, whether they hold it, its kind and grade, its pay, its heading and lines, its thing, what may be done with it. */
	record Posting(int id, boolean taken, int kind, int grade, int reward, Component title, List<Component> lines, ItemStack icon, int actions) {}

	/** One ware: its place in the exchange's list, the thing, its price, how many left today of how many, whether it is open to them (else why). */
	record Ware(int index, ItemStack item, int price, int left, int perDay, boolean open, Component locked) {}

	/** The page as the server sent it: for one of no standing at the board, only why ({@code note}). */
	record Page(Component heading, boolean disciple, Component note, int contribution, int earned, Component title, int held, int maxHeld,
			List<Posting> missions, List<Ware> wares, @Nullable Component said) {}

	public static Page read(FriendlyByteBuf buf) {
		Component heading = buf.readComponent();
		if (!buf.readBoolean()) return new Page(heading, false, buf.readComponent(), 0, 0, Component.empty(), 0, 0, List.of(), List.of(), null);
		int contribution = buf.readVarInt(), earned = buf.readVarInt();
		Component title = buf.readComponent();
		int held = buf.readVarInt(), maxHeld = buf.readVarInt();
		List<Posting> missions = new ArrayList<>();
		for (int i = buf.readVarInt(); i > 0; i--) {
			int id = buf.readVarInt();
			boolean taken = buf.readBoolean();
			int kind = buf.readVarInt(), grade = buf.readVarInt(), reward = buf.readVarInt();
			Component heading2 = buf.readComponent();
			List<Component> lines = new ArrayList<>();
			for (int n = buf.readVarInt(); n > 0; n--) lines.add(buf.readComponent());
			ItemStack icon = buf.readItem();
			missions.add(new Posting(id, taken, kind, grade, reward, heading2, lines, icon, buf.readVarInt()));
		}
		List<Ware> wares = new ArrayList<>();
		for (int i = buf.readVarInt(); i > 0; i--) {
			int index = buf.readVarInt();
			ItemStack item = buf.readItem();
			int price = buf.readVarInt(), left = buf.readVarInt(), perDay = buf.readVarInt();
			boolean open = buf.readBoolean();
			wares.add(new Ware(index, item, price, left, perDay, open, buf.readComponent()));
		}
		Component said = buf.readBoolean() ? buf.readComponent() : null;
		return new Page(heading, true, Component.empty(), contribution, earned, title, held, maxHeld, missions, wares, said);
	}

	/** The page come (S2C mission_board): onto the board's screen if it is open, else in a new one. */
	public static void show(Page page) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen instanceof MissionBoardScreen open) open.page(page);
		else mc.setScreen(new MissionBoardScreen(page));
	}

	private Page page;
	private boolean exchange;
	private int chosenMission = -1, chosenWare = -1;
	private int left, top, listScroll, listMax, paneScroll, paneMax;

	private MissionBoardScreen(Page page) {
		super(page.heading());
		this.page = page;
		choose();
	}

	private void page(Page page) {
		this.page = page;
		choose();
	}

	/** Keeps what was chosen if it is still there, else the first of the list. */
	private void choose() {
		if (page.missions().stream().noneMatch(m -> m.id() == chosenMission)) {
			chosenMission = page.missions().isEmpty() ? -1 : page.missions().get(0).id();
			paneScroll = 0;
		}
		if (page.wares().stream().noneMatch(w -> w.index() == chosenWare)) chosenWare = page.wares().isEmpty() ? -1 : page.wares().get(0).index();
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// --- What the list holds ---

	/** A row of the list: a label (no mission or ware), a mission, or a ware; and where it starts down the list. */
	private record Row(@Nullable Component label, @Nullable Posting mission, @Nullable Ware ware, int y, int height) {}

	private List<Row> rows() {
		List<Row> rows = new ArrayList<>();
		int y = 0;
		if (exchange) {
			for (Ware ware : page.wares()) {
				rows.add(new Row(null, null, ware, y, ROW_H));
				y += ROW_H;
			}
			return rows;
		}
		boolean anyTaken = page.missions().stream().anyMatch(Posting::taken);
		if (anyTaken) {
			rows.add(new Row(Component.translatable(ModLang.MISSION_HELD, page.held(), page.maxHeld()), null, null, y, LABEL_H));
			y += LABEL_H;
			for (Posting m : page.missions()) {
				if (!m.taken()) continue;
				rows.add(new Row(null, m, null, y, ROW_H));
				y += ROW_H;
			}
		}
		rows.add(new Row(Component.translatable(ModLang.MISSION_POSTED), null, null, y, LABEL_H));
		y += LABEL_H;
		boolean any = false;
		for (Posting m : page.missions()) {
			if (m.taken()) continue;
			rows.add(new Row(null, m, null, y, ROW_H));
			y += ROW_H;
			any = true;
		}
		if (!any) rows.add(new Row(Component.translatable(ModLang.MISSION_NOTHING).withStyle(s -> s.withColor(TalkUi.MUTED)), null, null, y, LABEL_H * 2));
		return rows;
	}

	private @Nullable Posting chosenMission() {
		for (Posting m : page.missions()) if (m.id() == chosenMission) return m;
		return null;
	}

	private @Nullable Ware chosenWare() {
		for (Ware w : page.wares()) if (w.index() == chosenWare) return w;
		return null;
	}

	// --- Its buttons ---

	/** A button in the pane: the act it sends (and on what), its word, whether it may be pressed, where it stands. */
	private record Button(int act, int id, Component label, boolean enabled, int x, int y, int w) {}

	private List<Button> buttons() {
		List<Button> buttons = new ArrayList<>();
		int y = top + BODY_BOTTOM - BUTTON_H;
		int x = left + PANE_X;
		if (exchange) {
			Ware ware = chosenWare();
			if (ware == null) return buttons;
			Component label = Component.translatable(ModLang.EXCHANGE_BUY);
			buttons.add(new Button(SectMissions.BUY, ware.index(), label, ware.open() && ware.left() > 0 && page.contribution() >= ware.price(), x, y,
					font.width(label) + 12));
			return buttons;
		}
		Posting m = chosenMission();
		if (m == null) return buttons;
		List<Object[]> wanted = new ArrayList<>();
		if (!m.taken()) wanted.add(new Object[] {SectMissions.TAKE, ModLang.MISSION_TAKE, (m.actions() & SectMissions.CAN_TAKE) != 0});
		else {
			if (m.kind() == SectMissions.Kind.GATHER.ordinal() || m.kind() == SectMissions.Kind.CRAFT.ordinal()) {
				wanted.add(new Object[] {SectMissions.HAND_IN, ModLang.MISSION_HAND_IN, (m.actions() & SectMissions.CAN_HAND_IN) != 0});
			}
			if ((m.actions() & SectMissions.CAN_SLIP) != 0) wanted.add(new Object[] {SectMissions.SLIP, ModLang.MISSION_NEW_SLIP, true});
			wanted.add(new Object[] {SectMissions.ABANDON, ModLang.MISSION_ABANDON, true});
		}
		for (Object[] want : wanted) {
			Component label = Component.translatable((String) want[1]);
			int w = font.width(label) + 12;
			buttons.add(new Button((int) want[0], m.id(), label, (boolean) want[2], x, y, w));
			x += w + 4;
		}
		return buttons;
	}

	private void send(int act, int id) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(act);
		buf.writeVarInt(id);
		ClientPlayNetworking.send(ModPackets.MISSION_ACT, buf);
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	// --- Input ---

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0 || !page.disciple()) return super.mouseClicked(mouseX, mouseY, button);
		for (int tab = 0; tab < 2; tab++) {
			if (in(mouseX, mouseY, tabX(tab), top + TABS_Y, tabW(tab), 13)) {
				boolean pick = tab == 1;
				if (pick != exchange) {
					exchange = pick;
					listScroll = 0;
					paneScroll = 0;
					if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0f));
				}
				return true;
			}
		}
		for (Button b : buttons()) {
			if (in(mouseX, mouseY, b.x(), b.y(), b.w(), BUTTON_H)) {
				if (b.enabled()) send(b.act(), b.id());
				return true;
			}
		}
		int listTop = top + BODY_TOP, listBottom = top + BODY_BOTTOM;
		if (in(mouseX, mouseY, left + LIST_X, listTop, LIST_W - ROLLER_W - 2, listBottom - listTop)) {
			for (Row row : rows()) {
				int y = listTop + row.y() - listScroll;
				if (mouseY < y || mouseY >= y + row.height()) continue;
				if (row.mission() != null && row.mission().id() != chosenMission) {
					chosenMission = row.mission().id();
					paneScroll = 0;
				} else if (row.ware() != null) {
					chosenWare = row.ware().index();
					paneScroll = 0;
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (mouseX < left + PANE_X - 4) listScroll = Mth.clamp(listScroll - (int) Math.signum(delta) * SCROLL_STEP, 0, listMax);
		else paneScroll = Mth.clamp(paneScroll - (int) Math.signum(delta) * SCROLL_STEP, 0, paneMax);
		return true;
	}

	// --- Drawing ---

	private int tabX(int tab) {
		return tab == 0 ? left + LIST_X : left + LIST_X + tabW(0) + 4;
	}

	private int tabW(int tab) {
		return font.width(Component.translatable(tab == 0 ? ModLang.MISSION_TAB_MISSIONS : ModLang.MISSION_TAB_EXCHANGE)) + 12;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		TalkUi.panel(g, left, top, left + W, top + H, TalkUi.GOLD);
		// The heading (the Hall's: a gold diamond, the words, the rule under them).
		int cx = left + W / 2;
		HallHud.heading(g, font, title, left + 8, top + 8, W - 16);
		if (!page.disciple()) {
			List<FormattedCharSequence> note = font.split(page.note(), W - 60);
			int y = top + H / 2 - note.size() * LINE_H / 2;
			for (FormattedCharSequence line : note) {
				g.drawString(font, line, cx - font.width(line) / 2, y, TalkUi.CREAM, false);
				y += LINE_H;
			}
			super.render(g, mouseX, mouseY, partialTick);
			return;
		}
		// Their Contribution at the right of the tabs' row (all they have earned, pointed at), their title before it: the heading has its
		// row to itself (a long sect's name ran into them there).
		Component contribution = Component.translatable(ModLang.MISSION_CONTRIBUTION, page.contribution());
		int contributionX = left + W - 10 - font.width(contribution);
		g.drawString(font, contribution, contributionX, top + TABS_Y + 3, TalkUi.GOLD_TEXT, false);
		g.drawString(font, page.title(), contributionX - 10 - font.width(page.title()), top + TABS_Y + 3, TalkUi.MUTED, false);
		boolean overContribution = in(mouseX, mouseY, contributionX, top + TABS_Y + 1, font.width(contribution), 11);
		// The tabs.
		for (int tab = 0; tab < 2; tab++) {
			boolean on = (tab == 1) == exchange;
			Component label = Component.translatable(tab == 0 ? ModLang.MISSION_TAB_MISSIONS : ModLang.MISSION_TAB_EXCHANGE);
			int x = tabX(tab), y = top + TABS_Y;
			boolean hover = in(mouseX, mouseY, x, y, tabW(tab), 13);
			// The tab open in jade, as the Hall's.
			HallHud.panel(g, x, y, x + tabW(tab), y + 13, on ? HallHud.FRAME_LIT : hover ? HallHud.FRAME_LIT : HallHud.FRAME,
					on ? HallHud.ROW_OPEN : hover ? HallHud.ROW_LIT : HallHud.CARD);
			g.drawString(font, label, x + 6, y + 3, on ? TalkUi.GOLD_TEXT : TalkUi.CREAM, false);
		}
		g.fill(left + PANE_X - 5, top + BODY_TOP, left + PANE_X - 4, top + BODY_BOTTOM, TalkUi.GOLD_DIM);
		ItemStack hoverStack = renderList(g, mouseX, mouseY);
		ItemStack paneStack = renderPane(g, mouseX, mouseY);
		// What came of the last thing done, at the foot.
		g.fill(left + 6, top + H - 19, left + W - 6, top + H - 18, TalkUi.GOLD_DIM);
		if (page.said() != null) {
			Component said = page.said();
			String text = said.getString();
			if (font.width(said) > W - 20) text = font.plainSubstrByWidth(text, W - 20 - font.width("...")) + "...";
			g.drawString(font, Component.literal(text).withStyle(said.getStyle()), left + 10, top + H - 14, TalkUi.CREAM, false);
		}
		super.render(g, mouseX, mouseY, partialTick);
		if (hoverStack != null) g.renderTooltip(font, hoverStack, mouseX, mouseY);
		else if (paneStack != null) g.renderTooltip(font, paneStack, mouseX, mouseY);
		else if (overContribution) g.renderTooltip(font, Component.translatable(ModLang.MISSION_EARNED, page.earned()), mouseX, mouseY);
	}

	/** The list: its labels and rows, the chosen row lit; @return the thing pointed at (a ware's), for its tooltip */
	private @Nullable ItemStack renderList(GuiGraphics g, int mouseX, int mouseY) {
		int x = left + LIST_X, viewTop = top + BODY_TOP, viewBottom = top + BODY_BOTTOM, rowW = LIST_W - ROLLER_W - 3;
		List<Row> rows = rows();
		int content = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y() + rows.get(rows.size() - 1).height();
		listMax = Math.max(0, content - (viewBottom - viewTop));
		listScroll = Mth.clamp(listScroll, 0, listMax);
		ItemStack hovered = null;
		g.enableScissor(x, viewTop, x + rowW, viewBottom);
		for (Row row : rows) {
			int y = viewTop + row.y() - listScroll;
			if (y + row.height() <= viewTop || y >= viewBottom) continue;
			boolean hover = in(mouseX, mouseY, x, Math.max(y, viewTop), rowW, row.height()) && mouseY < viewBottom;
			if (row.label() != null) {
				List<FormattedCharSequence> lines = font.split(row.label(), rowW - 4);
				for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 2, y + 2 + i * LINE_H, TalkUi.GOLD_TEXT, false);
				continue;
			}
			boolean chosen = row.mission() != null ? row.mission().id() == chosenMission : row.ware() != null && row.ware().index() == chosenWare;
			// The chosen row in jade with a gold edge, as the Codex's icons; a row pointed at, lit.
			if (chosen) {
				g.fill(x, y, x + rowW, y + row.height() - 1, HallHud.ROW_OPEN);
				g.fill(x, y, x + 1, y + row.height() - 1, HallHud.GOLD);
			} else if (hover) {
				g.fill(x, y, x + rowW, y + row.height() - 1, HallHud.ROW_LIT);
			}
			if (row.mission() != null) {
				Posting m = row.mission();
				g.renderItem(m.icon(), x + 2, y + 2);
				g.drawString(font, trimmed(m.title(), rowW - 24), x + 21, y + 2, chosen || hover ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM, false);
				Component grade = Component.translatable(ModLang.missionGrade(SectMissions.Grade.byIndex(m.grade()).getId()));
				int gradeColour = m.grade() == 0 ? EASY : m.grade() == 2 ? HARD : TalkUi.GOLD_TEXT;
				g.drawString(font, grade, x + 21, y + 11, gradeColour, false);
				g.drawString(font, Component.translatable(ModLang.EXCHANGE_PRICE, m.reward()), x + 25 + font.width(grade), y + 11, TalkUi.MUTED, false);
			} else if (row.ware() != null) {
				Ware ware = row.ware();
				g.renderItem(ware.item(), x + 2, y + 2);
				g.renderItemDecorations(font, ware.item(), x + 2, y + 2);
				boolean able = ware.open() && ware.left() > 0;
				g.drawString(font, trimmed(ware.item().getHoverName(), rowW - 24), x + 21, y + 2, able ? (chosen || hover ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM) : TalkUi.DISABLED, false);
				Component under = !ware.open() ? ware.locked() : Component.translatable(ModLang.EXCHANGE_PRICE, ware.price());
				int colour = !ware.open() ? TalkUi.WARNING : page.contribution() >= ware.price() && ware.left() > 0 ? TalkUi.GOLD_TEXT : TalkUi.DISABLED;
				g.drawString(font, trimmed(under, rowW - 24), x + 21, y + 11, colour, false);
				if (in(mouseX, mouseY, x + 2, y + 2, 16, 16) && mouseY >= viewTop && mouseY < viewBottom) hovered = ware.item();
			}
		}
		g.disableScissor();
		CodexScreen.roller(g, x + LIST_W - ROLLER_W, viewTop, viewBottom, content, listScroll, listMax);
		return hovered;
	}

	/** The pane: the chosen mission or ware in full, and its buttons; @return the thing pointed at (the chosen one's), for its tooltip */
	private @Nullable ItemStack renderPane(GuiGraphics g, int mouseX, int mouseY) {
		int x = left + PANE_X, y = top + BODY_TOP;
		ItemStack icon;
		Component name;
		List<Component> lines = new ArrayList<>();
		if (exchange) {
			Ware ware = chosenWare();
			if (ware == null) return null;
			icon = ware.item();
			name = ware.item().getHoverName();
			lines.add(Component.translatable(ModLang.EXCHANGE_PRICE, ware.price()).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
			lines.add(Component.translatable(ModLang.EXCHANGE_LEFT, ware.left(), ware.perDay()));
			if (!ware.open()) lines.add(ware.locked().copy().withStyle(s -> s.withColor(TalkUi.WARNING)));
			List<Component> tip = ware.item().getTooltipLines(minecraft == null ? null : minecraft.player, TooltipFlag.Default.NORMAL);
			for (int i = 1; i < tip.size(); i++) lines.add(tip.get(i));
		} else {
			Posting m = chosenMission();
			if (m == null) return null;
			icon = m.icon();
			name = m.title();
			if (m.taken()) lines.add(Component.translatable(ModLang.MISSION_TAKEN_TAG).withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
			lines.addAll(m.lines());
		}
		g.renderItem(icon, x, y);
		g.drawString(font, trimmed(name, PANE_W - 22), x + 20, y + 4, TalkUi.GOLD_TEXT, true);
		g.fill(x, y + 19, x + PANE_W, y + 20, TalkUi.GOLD_DIM);
		int viewTop = y + 23, viewBottom = top + BODY_BOTTOM - BUTTON_H - 4, textW = PANE_W - ROLLER_W - 6;
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		List<Integer> gaps = new ArrayList<>();
		for (Component line : lines) {
			List<FormattedCharSequence> split = font.split(line, textW);
			wrapped.addAll(split);
			for (int i = 0; i < split.size(); i++) gaps.add(i == split.size() - 1 ? 3 : 0);
		}
		int content = 0;
		for (int gap : gaps) content += LINE_H + gap;
		paneMax = Math.max(0, content - (viewBottom - viewTop));
		paneScroll = Mth.clamp(paneScroll, 0, paneMax);
		g.enableScissor(x, viewTop, x + textW + 2, viewBottom);
		int ly = viewTop - paneScroll;
		for (int i = 0; i < wrapped.size(); i++) {
			if (ly + LINE_H > viewTop && ly < viewBottom) g.drawString(font, wrapped.get(i), x, ly, TalkUi.CREAM, false);
			ly += LINE_H + gaps.get(i);
		}
		g.disableScissor();
		CodexScreen.roller(g, x + PANE_W - ROLLER_W, viewTop, viewBottom, content, paneScroll, paneMax);
		for (Button b : buttons()) {
			boolean hover = b.enabled() && in(mouseX, mouseY, b.x(), b.y(), b.w(), BUTTON_H);
			HallHud.button(g, font, b.x(), b.y(), b.w(), BUTTON_H, b.label(), b.enabled(), hover);
		}
		return in(mouseX, mouseY, x, y, 16, 16) && exchange ? icon : null;
	}

	private Component trimmed(Component text, int width) {
		if (font.width(text) <= width) return text;
		return Component.literal(font.plainSubstrByWidth(text.getString(), width - font.width("...")) + "...").withStyle(text.getStyle());
	}
}
