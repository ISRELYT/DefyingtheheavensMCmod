package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.TalkIntent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The dialogue box (see TalkSystem): across the foot of the screen, what the cultivator says on the left with your regard
 * under it, the replies on the right, numbered. Above it, their portrait and name plate, and the talk wheel folded into a
 * row of tabs, so you can turn to a gift or an insult without leaving. Esc goes back to the wheel.
 */
public class DialogueScreen extends Screen implements HallFrame.Framed {
	private static final int BOX_H = 80;
	private static final int ROW_H = 11;
	private static final int TAB_W = 52;
	private static final int TAB_H = 15;

	final TalkSession session;
	private TalkSession.Dialogue dialogue;
	private int boxX0, boxY0, boxX1, boxY1, repliesX;
	/**
	 * How many of what they say is scrolled past (lines), and how many more there are than the box shows: a long answer (a Heavenly
	 * Venerable's on the four primordial beasts) ran past the box's foot on a small screen and was cut off (2026-11-10). The wheel over it,
	 * or the arrow keys, scroll it; arrows at its side tell there is more.
	 */
	private int textScroll, textMaxScroll;

	DialogueScreen(TalkSession session, TalkSession.Dialogue dialogue) {
		super(Component.literal(session.name));
		this.session = session;
		this.dialogue = dialogue;
	}

	/** They said something more. */
	void show(TalkSession.Dialogue next) {
		dialogue = next;
		textScroll = 0;
		if (minecraft != null) fit();
	}

	@Override
	protected void init() {
		boxX0 = 12;
		boxX1 = width - 12;
		boxY1 = height - 8;
		int repliesW = Math.min(190, (boxX1 - boxX0) / 2);
		repliesX = boxX1 - 10 - repliesW;
		fit();
	}

	/**
	 * The box's height: {@link #BOX_H}, or taller to hold every reply and all they say, as far as the screen allows (the
	 * portrait and tabs keep their room above). Rows that didn't fit were left off: an elder's "Ask to join", after its
	 * teachings, was never seen.
	 */
	private void fit() {
		boxY0 = boxY1 - BOX_H;
		List<Row> rows = rows(-1);
		int forReplies = rows.get(rows.size() - 1).bottom() + 4 - boxY0;
		int forLine = font.split(dialogue.line(), repliesX - (boxX0 + 10) - 14).size() * 10 + 23;
		int room = boxY1 - 60;
		// For what they say, the portrait and its plates (52 above the box) kept below the HUD's top-left stack: on a small screen a long
		// answer grew the box till they sat over its name and bars, words on words (2026-11-10); what doesn't fit scrolls. The replies may
		// still take all the room (none of them is ever left off).
		int hud = HudCorner.foot();
		int clear = hud > 0 ? Math.min(room, boxY1 - (hud + 4) - 52) : room;
		boxY0 = boxY1 - Math.max(BOX_H, Math.max(Math.min(room, forReplies), Math.min(clear, forLine)));
	}

	/** One row of the replies, wrapped to the column: where it starts and its lines (the number goes before the first). */
	private record Row(int y, List<FormattedCharSequence> lines) {
		int bottom() { return y + Math.max(1, lines.size()) * 10 + (ROW_H - 10); }
	}

	/** The replies laid out down their column, each wrapped to fit it, then "Esc  Back to the wheel". */
	private List<Row> rows(int hoveredRow) {
		List<Row> rows = new ArrayList<>();
		List<TalkSession.Reply> replies = dialogue.replies();
		int numberW = font.width("9  ");
		int width = boxX1 - 10 - repliesX - numberW;
		int y = boxY0 + 9;
		for (int i = 0; i < replies.size(); i++) {
			TalkSession.Reply reply = replies.get(i);
			int color = !reply.enabled() ? TalkUi.DISABLED : i == hoveredRow ? TalkUi.CREAM_BRIGHT : TalkUi.CREAM;
			MutableComponent text = reply.label().copy().withStyle(style -> style.withColor(color));
			if (reply.note() != null) text.append(Component.literal(" ").append(reply.note()).withStyle(style -> style.withColor(TalkUi.WARNING)));
			Row row = new Row(y, font.split(text, width));
			rows.add(row);
			y = row.bottom();
		}
		rows.add(new Row(y, font.split(Component.translatable(ModLang.TALK_BACK).withStyle(style -> style.withColor(TalkUi.MUTED)), width + numberW)));
		return rows;
	}

	/** The reply row under the pointer: an index into the replies, the back row (size), or -1. */
	private int rowAt(double mouseX, double mouseY) {
		if (mouseX < repliesX - 4 || mouseX >= boxX1 - 6) return -1;
		List<Row> rows = rows(-1);
		for (int i = 0; i < rows.size(); i++) {
			if (mouseY >= rows.get(i).y() - 2 && mouseY < rows.get(i).bottom() - 2) return i;
		}
		return -1;
	}

	/** Tabs narrow when there are many (the portrait keeps its corner). */
	private int tabW() {
		return Math.max(36, Math.min(TAB_W, (boxX1 - boxX0 - 60) / Math.max(1, session.intents.size()) - 3));
	}

	private int tabX(int i) {
		return boxX1 - (session.intents.size() - i) * (tabW() + 3);
	}

	private int tabAt(double mouseX, double mouseY) {
		if (mouseY < boxY0 - TAB_H || mouseY >= boxY0) return -1;
		for (int i = 0; i < session.intents.size(); i++) {
			if (mouseX >= tabX(i) && mouseX < tabX(i) + tabW()) return i;
		}
		return -1;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x500C0F13);
		// Portrait and name plate.
		int frameX = boxX0, frameY = boxY0 - 52, frame = 44;
		HallHud.panel(g, frameX, frameY, frameX + frame, frameY + frame, HallHud.FRAME_LIT, TalkUi.INNER);
		Entity entity = session.entity();
		if (entity instanceof LivingEntity living) {
			g.enableScissor(frameX + 2, frameY + 2, frameX + frame - 2, frameY + frame - 2);
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, frameX + frame / 2, frameY + 82, 38, frameX + frame / 2 - mouseX,
					frameY + 14 - mouseY, living);
			g.disableScissor();
		}
		TalkUi.namePlate(g, font, session, frameX + frame + 8, frameY + 10);

		// The wheel, folded into tabs on the box.
		int hoveredTab = tabAt(mouseX, mouseY);
		for (int i = 0; i < session.intents.size(); i++) {
			TalkIntent intent = session.intents.get(i);
			boolean current = intent == dialogue.intent();
			int x = tabX(i), y = boxY0 - TAB_H + (current ? 0 : 2), w = tabW();
			// The one being talked of in jade, as the Hall's open tab.
			int border = current || i == hoveredTab ? HallHud.FRAME_LIT : intent == TalkIntent.PROVOKE ? TalkUi.RED_DIM : HallHud.FRAME;
			HallHud.panel(g, x, y, x + w, boxY0 - 1, border, current ? HallHud.ROW_OPEN : i == hoveredTab ? TalkUi.PANEL_LIT : TalkUi.PANEL);
			int color = current ? TalkUi.GOLD_TEXT : i == hoveredTab ? TalkUi.CREAM : TalkUi.MUTED;
			g.drawCenteredString(font, Component.translatable(ModLang.talkIntent(intent.getId())), x + w / 2, y + 4, color);
		}

		// The box.
		TalkUi.panel(g, boxX0, boxY0, boxX1, boxY1, TalkUi.GOLD);
		int textX = boxX0 + 10, textW = repliesX - textX - 14;
		int top = boxY0 + 9;
		List<FormattedCharSequence> said = font.split(dialogue.line(), textW);
		int shown = Math.max(1, (boxY1 - 24 - top) / 10 + 1);
		textMaxScroll = Math.max(0, said.size() - shown);
		textScroll = Math.max(0, Math.min(textScroll, textMaxScroll));
		int y = top;
		for (int i = textScroll; i < said.size() && i < textScroll + shown; i++) {
			g.drawString(font, said.get(i), textX, y, TalkUi.CREAM, false);
			y += 10;
		}
		// More above or below: a small gold arrow in the gap between the words and the replies' rule (the wheel over it, or the arrow keys, scroll it).
		int arrowX = repliesX - 13;
		if (textScroll > 0) arrow(g, arrowX, top + 1, true);
		if (textScroll < textMaxScroll) arrow(g, arrowX, top + (shown - 1) * 10 + 2, false);
		TalkUi.regard(g, font, session.regard, textX, boxY1 - 15);
		g.fill(repliesX - 8, boxY0 + 8, repliesX - 7, boxY1 - 8, HallHud.RULE);

		// The replies.
		// Each reply wrapped to the column: a long one, or one with a note, takes two lines rather than running off the box.
		int hoveredRow = rowAt(mouseX, mouseY);
		List<TalkSession.Reply> replies = dialogue.replies();
		List<Row> rows = rows(hoveredRow);
		int numberW = font.width("9  ");
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			if (row.bottom() > boxY1 - 4) break;
			boolean back = i == replies.size();
			boolean enabled = back || replies.get(i).enabled();
			if (i == hoveredRow && enabled) g.fill(repliesX - 4, row.y() - 2, boxX1 - 6, row.bottom() - 2, HallHud.ROW_LIT);
			if (!back) g.drawString(font, (i + 1) + "  ", repliesX, row.y(), TalkUi.MUTED, false);
			int ly = row.y();
			for (FormattedCharSequence line : row.lines()) {
				g.drawString(font, line, back ? repliesX : repliesX + numberW, ly, TalkUi.CREAM, false);
				ly += 10;
			}
		}
		super.render(g, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			int tab = tabAt(mouseX, mouseY);
			if (tab >= 0) {
				choose(session.intents.get(tab));
				return true;
			}
			int row = rowAt(mouseX, mouseY);
			if (row >= 0) {
				pick(row);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** A five-pixel gold arrow, pointing {@code up} or down, its top left at ({@code x}, {@code y}). */
	private static void arrow(GuiGraphics g, int x, int y, boolean up) {
		// (Opaque: fill takes the colour's alpha as given, and the text colour has none of its own.)
		int gold = 0xFF000000 | TalkUi.GOLD_TEXT;
		for (int row = 0; row < 3; row++) {
			int inset = up ? 2 - row : row;
			g.fill(x + inset, y + row, x + 5 - inset, y + row + 1, gold);
		}
	}

	/** The wheel over what they say scrolls it: two lines a notch. */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (textMaxScroll > 0 && mouseX < repliesX - 8 && mouseY >= boxY0 && mouseY < boxY1) {
			int lines = 2 * Math.max(1, (int) Math.round(Math.abs(delta)));
			textScroll = Math.max(0, Math.min(textMaxScroll, textScroll - (int) Math.signum(delta) * lines));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		// The arrow keys scroll what they say, when it is longer than the box.
		if (textMaxScroll > 0 && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
			textScroll = Math.max(0, Math.min(textMaxScroll, textScroll + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
			return true;
		}
		int index = key - GLFW.GLFW_KEY_1;
		if (index >= 0 && index < dialogue.replies().size()) {
			pick(index);
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	private void pick(int row) {
		if (row == dialogue.replies().size()) {
			onClose();
			return;
		}
		TalkSession.Reply reply = dialogue.replies().get(row);
		if (reply.enabled()) session.sendTopic(reply.id());
	}

	private void choose(TalkIntent intent) {
		if (intent == dialogue.intent()) return;
		session.sendIntent(intent);
		if (intent.isQuick()) Minecraft.getInstance().setScreen(null); // answered over the hotbar; the talk is over
	}

	@Override
	public void tick() {
		if (session.entity() == null) {
			session.sendEnd();
			Minecraft.getInstance().setScreen(null);
		}
	}

	/** Esc: back to the wheel. */
	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(new TalkWheelScreen(session));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
