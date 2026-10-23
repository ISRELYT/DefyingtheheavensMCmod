package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Bloodline;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * "Choose Your Bloodline" (Bloodlines), the first time one joins, in the mod's lacquer and gold (TalkUi): the five races down the
 * right, the one picked told of under them and shown on oneself at the left, turning to follow the mouse (ClientBloodlines#preview:
 * the race's whole look, or one's own skin for a human), with its colourings to step through. Begin (or Enter) chooses it, for good.
 * It can't be put aside: until one is chosen it comes up again each time one joins.
 */
public class BloodlineScreen extends Screen {
	private static final int W = 340, H = 230;
	/** The figure's box at the left, and the races' rows at the right. */
	private static final int FIGURE_W = 110, ROW_H = 16, ROWS_Y = 49;
	private static final Bloodline[] CHOICES = Bloodline.values();

	private Bloodline chosen = Bloodline.HUMAN;
	private int variant;
	private Button begin, previous, next;
	private int left, top;

	public BloodlineScreen() {
		super(Component.translatable(ModLang.BLOODLINE_TITLE));
	}

	/** S2C bloodline_ask: brought up, over whatever else is open. */
	public static void ask() {
		Minecraft.getInstance().setScreen(new BloodlineScreen());
	}

	@Override
	protected void init() {
		left = (width - W) / 2;
		top = (height - H) / 2;
		int right = left + FIGURE_W + 14, wide = W - FIGURE_W - 28;
		previous = addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1)).bounds(right, top + H - 60, 20, 20).build());
		next = addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1)).bounds(right + wide - 20, top + H - 60, 20, 20).build());
		begin = addRenderableWidget(Button.builder(Component.translatable(ModLang.BLOODLINE_CONFIRM), b -> confirm())
				.bounds(right, top + H - 26, wide, 20).build());
		show();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void removed() {
		ClientBloodlines.preview = null;
	}

	private int rowY(int i) { return top + ROWS_Y + i * ROW_H; }

	private int rowAt(double mouseX, double mouseY) {
		int x = left + FIGURE_W + 14, wide = W - FIGURE_W - 28;
		for (int i = 0; i < CHOICES.length; i++) {
			int y = rowY(i);
			if (mouseX >= x && mouseX < x + wide && mouseY >= y && mouseY < y + ROW_H - 2) return i;
		}
		return -1;
	}

	private void pick(int i) {
		if (i < 0 || i >= CHOICES.length || CHOICES[i] == chosen) return;
		chosen = CHOICES[i];
		variant = 0;
		show();
		click();
	}

	private void step(int by) {
		if (chosen.variants() == 0) return;
		variant = Math.floorMod(variant + by, chosen.variants());
		show();
		click();
	}

	/** The race picked, worn by oneself while the screen is open (a fox spirit with the tails its realm gives); its colourings' arrows. */
	private void show() {
		int tails = Bloodline.tails(ClientCultivationData.get().getRealm());
		ClientBloodlines.preview = new ClientBloodlines.Look(chosen, variant, tails);
		previous.visible = next.visible = chosen.variants() > 1;
	}

	private void click() {
		if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	/** Chosen: told to the server, which keeps it and tells everyone. */
	private void confirm() {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(chosen.ordinal());
		buf.writeVarInt(variant);
		ClientPlayNetworking.send(ModPackets.BLOODLINE_CHOOSE, buf);
		onClose();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int i = rowAt(mouseX, mouseY);
		if (button == 0 && i >= 0) {
			pick(i);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int key, int scan, int modifiers) {
		if (key >= GLFW.GLFW_KEY_1 && key < GLFW.GLFW_KEY_1 + CHOICES.length) {
			pick(key - GLFW.GLFW_KEY_1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
			step(key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
			return true;
		}
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			confirm();
			return true;
		}
		return super.keyPressed(key, scan, modifiers);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		int x = left, y = top;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		// The title, a gold rule under it with a scroll roller's knob at each end (as the Heaven Altar's).
		g.drawString(font, title, x + (W - font.width(title)) / 2, y + 7, TalkUi.GOLD_TEXT, false);
		g.fill(x + 14, y + 19, x + W - 14, y + 20, TalkUi.GOLD);
		g.fill(x + 14, y + 20, x + W - 14, y + 21, TalkUi.GOLD_DIM);
		for (int kx : new int[] {x + 10, x + W - 14}) {
			g.fill(kx, y + 17, kx + 4, y + 23, TalkUi.GOLD);
			g.fill(kx + 1, y + 18, kx + 3, y + 22, TalkUi.GOLD_DIM);
		}
		List<FormattedCharSequence> intro = font.split(Component.translatable(ModLang.BLOODLINE_INTRO), W - 28);
		int iy = y + 26;
		for (FormattedCharSequence line : intro) {
			g.drawString(font, line, x + (W - font.width(line)) / 2, iy, TalkUi.CREAM, false);
			iy += 10;
		}

		// Oneself, wearing the race picked, turning to the mouse.
		int fx = x + 12, fy = y + ROWS_Y - 4, fh = H - ROWS_Y - 10;
		TalkUi.panel(g, fx, fy, fx + FIGURE_W, fy + fh, TalkUi.GOLD_DIM);
		if (minecraft != null && minecraft.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + FIGURE_W / 2, fy + fh - 14, 52, fx + FIGURE_W / 2 - mouseX,
					fy + fh / 2 - mouseY, minecraft.player);
		}

		// The races, the one picked framed in gold.
		int rx = x + FIGURE_W + 14, wide = W - FIGURE_W - 28;
		int hovered = rowAt(mouseX, mouseY);
		for (int i = 0; i < CHOICES.length; i++) {
			Bloodline b = CHOICES[i];
			int ry = rowY(i);
			boolean picked = b == chosen;
			TalkUi.panel(g, rx, ry, rx + wide, ry + ROW_H - 2, picked ? TalkUi.GOLD : TalkUi.GOLD_DIM);
			if (picked || i == hovered) g.fill(rx + 3, ry + 3, rx + wide - 3, ry + ROW_H - 5, TalkUi.PANEL_LIT);
			String number = (i + 1) + ".";
			g.drawString(font, number, rx + 6, ry + 3, TalkUi.MUTED, false);
			g.drawString(font, b.getDisplayName(), rx + 6 + font.width(number) + 4, ry + 3, picked ? TalkUi.CREAM_BRIGHT : 0xFF000000 | b.getColour(), false);
			Component kind = Component.translatable(b.isBeast() ? ModLang.BLOODLINE_CORE : ModLang.BLOODLINE_OWN_SKIN);
			g.drawString(font, kind, rx + wide - 6 - font.width(kind), ry + 3, TalkUi.MUTED, false);
		}

		// What the race picked is, and its colouring.
		int ly = rowY(CHOICES.length) + 2;
		for (FormattedCharSequence line : font.split(Component.translatable(ModLang.bloodlineLoreKey(chosen.getId())), wide)) {
			if (ly > top + H - 69) break;
			g.drawString(font, line, rx, ly, TalkUi.CREAM, false);
			ly += 10;
		}
		Component colouring = chosen.isBeast() ? chosen.variantName(variant) : Component.translatable(ModLang.BLOODLINE_OWN_SKIN);
		g.drawString(font, colouring, rx + (wide - font.width(colouring)) / 2, top + H - 54, TalkUi.GOLD_TEXT, false);
		Component last = Component.translatable(ModLang.BLOODLINE_FINAL);
		g.drawString(font, last, rx + (wide - font.width(last)) / 2, top + H - 37, TalkUi.MUTED, false);
		super.render(g, mouseX, mouseY, partialTick);
	}
}
