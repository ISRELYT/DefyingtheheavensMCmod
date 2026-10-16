package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.StorageRingMenu;
import com.example.defyingtheheavens.StorageRings;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.example.defyingtheheavens.StorageRingMenu.COLUMNS;
import static com.example.defyingtheheavens.StorageRingMenu.GRID_X;
import static com.example.defyingtheheavens.StorageRingMenu.GRID_Y;
import static com.example.defyingtheheavens.StorageRingMenu.ROWS;

/**
 * A Storage Ring's screen, in the mod's lacquer and gold (TalkUi): the ring's name and grade over a gold rule, a search line and
 * how full the space is, then the space itself, a grid of what it holds (one well a kind, how many beneath it, a k or an M once
 * the figures grow), scrolled by the wheel or the bar beside it; the pack below. Clicking the grid takes a stack (right: half,
 * shift: straight into the pack) or, with something carried, puts it in (right: one); shift-clicking the pack puts a stack in.
 * Refine, at the top right, raises the ring a grade, its cost and the realm it wants on the button's tooltip.
 */
public class StorageRingScreen extends AbstractContainerScreen<StorageRingMenu> {
	private static final int W = StorageRingMenu.WIDTH, H = StorageRingMenu.HEIGHT;
	private static final int SLOT_BG = 0xFF120D0B;
	private static final int JADE = 0x7FE0A0;
	/** The scroll bar, beside the grid. */
	private static final int BAR_X = GRID_X + COLUMNS * 18 + 4, BAR_W = 6;
	/** The Refine button, top right. */
	private static final int REFINE_W = 48, REFINE_H = 13, REFINE_Y = 4;

	private EditBox search;
	/** The first row of the grid in view. */
	private int scroll;
	private boolean draggingBar;
	/** The space as filtered by the search line, and what it was filtered from. */
	private List<ClientStorageRing.Entry> shown = List.of();
	private int shownVersion = -1;
	private String shownFilter = null;

	public StorageRingScreen(StorageRingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = W;
		imageHeight = H;
	}

	@Override
	protected void init() {
		super.init();
		search = new EditBox(font, leftPos + GRID_X + 3, topPos + 25, 92, 9, Component.translatable(ModLang.STORAGE_SEARCH));
		search.setBordered(false);
		search.setMaxLength(40);
		search.setTextColor(TalkUi.CREAM);
		search.setHint(Component.translatable(ModLang.STORAGE_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(search);
	}

	@Override
	protected void containerTick() {
		search.tick();
	}

	// --- What is shown ---

	private List<ClientStorageRing.Entry> shown() {
		String filter = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		if (shownVersion != ClientStorageRing.version() || !filter.equals(shownFilter)) {
			List<ClientStorageRing.Entry> all = ClientStorageRing.entries(menu.containerId);
			List<ClientStorageRing.Entry> list = new ArrayList<>();
			for (ClientStorageRing.Entry entry : all) {
				if (filter.isEmpty() || entry.kind().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)) list.add(entry);
			}
			shown = list;
			shownVersion = ClientStorageRing.version();
			shownFilter = filter;
		}
		return shown;
	}

	private int maxScroll() {
		int rows = (shown().size() + COLUMNS - 1) / COLUMNS;
		return Math.max(0, rows - ROWS);
	}

	/** The entry under the mouse, or null. */
	private ClientStorageRing.Entry entryAt(double mouseX, double mouseY) {
		int col = (int) Math.floor((mouseX - leftPos - GRID_X) / 18), row = (int) Math.floor((mouseY - topPos - GRID_Y) / 18);
		if (!inGrid(mouseX, mouseY) || col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) return null;
		int index = (scroll + row) * COLUMNS + col;
		List<ClientStorageRing.Entry> list = shown();
		return index < list.size() ? list.get(index) : null;
	}

	private boolean inGrid(double mouseX, double mouseY) {
		double x = mouseX - leftPos - GRID_X, y = mouseY - topPos - GRID_Y;
		return x >= 0 && x < COLUMNS * 18 && y >= 0 && y < ROWS * 18;
	}

	private boolean onBar(double mouseX, double mouseY) {
		double x = mouseX - leftPos - BAR_X, y = mouseY - topPos - GRID_Y;
		return x >= -1 && x < BAR_W + 1 && y >= 0 && y < ROWS * 18;
	}

	private boolean onRefine(double mouseX, double mouseY) {
		double x = mouseX - leftPos - (W - 10 - REFINE_W), y = mouseY - topPos - REFINE_Y;
		return x >= 0 && x < REFINE_W && y >= 0 && y < REFINE_H;
	}

	// --- Drawing ---

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		scroll = Mth.clamp(scroll, 0, maxScroll());
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.getCarried().isEmpty()) {
			ClientStorageRing.Entry hovered = entryAt(mouseX, mouseY);
			if (hovered != null) {
				List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hovered.kind()));
				lines.add(Component.translatable(ModLang.STORAGE_COUNT, String.format("%,d", hovered.count())).withStyle(ChatFormatting.GOLD));
				g.renderTooltip(font, lines, hovered.kind().getTooltipImage(), mouseX, mouseY);
			} else if (onRefine(mouseX, mouseY)) {
				g.renderComponentTooltip(font, refineTooltip(), mouseX, mouseY);
			}
		}
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		// A gold rule under the title, a scroll roller's knob at each end.
		g.fill(x + 14, y + 19, x + W - 14, y + 20, TalkUi.GOLD);
		g.fill(x + 14, y + 20, x + W - 14, y + 21, TalkUi.GOLD_DIM);
		for (int kx : new int[] {x + 10, x + W - 14}) {
			g.fill(kx, y + 17, kx + 4, y + 23, TalkUi.GOLD);
			g.fill(kx + 1, y + 18, kx + 3, y + 22, TalkUi.GOLD_DIM);
		}
		// The search line's well.
		g.fill(x + GRID_X, y + 23, x + GRID_X + 98, y + 35, TalkUi.GOLD_DIM);
		g.fill(x + GRID_X + 1, y + 24, x + GRID_X + 97, y + 34, SLOT_BG);
		// The Refine button.
		refineButton(g, mouseX, mouseY);
		// The space: its wells, what each holds, and the bar.
		grid(g, mouseX, mouseY);
		bar(g);
		// The pack's wells, a gold rule between it and the hotbar.
		for (Slot slot : menu.slots) {
			g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, TalkUi.GOLD_DIM);
			g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
		}
		int iy = y + StorageRingMenu.INVENTORY_Y + 3 * 18 + 1;
		g.fill(x + StorageRingMenu.INVENTORY_X, iy, x + StorageRingMenu.INVENTORY_X + 9 * 18 - 2, iy + 1, TalkUi.GOLD_DIM);
		// Between the space and the pack, a thin rule with a notch at its middle.
		int ry = y + GRID_Y + ROWS * 18 + 6;
		g.fill(x + 14, ry, x + W - 14, ry + 1, TalkUi.GOLD_DIM);
		g.fill(x + W / 2 - 3, ry - 1, x + W / 2 + 3, ry + 2, TalkUi.GOLD);
	}

	private void grid(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = leftPos + GRID_X, y0 = topPos + GRID_Y;
		g.fill(x0 - 2, y0 - 2, x0 + COLUMNS * 18 + 2, y0 + ROWS * 18 + 2, TalkUi.GOLD);
		g.fill(x0 - 1, y0 - 1, x0 + COLUMNS * 18 + 1, y0 + ROWS * 18 + 1, TalkUi.INNER);
		List<ClientStorageRing.Entry> list = shown();
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				int sx = x0 + col * 18, sy = y0 + row * 18;
				g.fill(sx, sy, sx + 18, sy + 18, TalkUi.GOLD_DIM);
				g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
				int index = (scroll + row) * COLUMNS + col;
				if (index >= list.size()) continue;
				ClientStorageRing.Entry entry = list.get(index);
				if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x40FFF4C9);
				g.renderItem(entry.kind(), sx + 1, sy + 1);
				g.renderItemDecorations(font, entry.kind(), sx + 1, sy + 1, null);
				count(g, entry.count(), sx + 1, sy + 1);
			}
		}
		if (list.isEmpty()) {
			Component empty = Component.translatable(ModLang.STORAGE_EMPTY);
			g.drawString(font, empty, x0 + (COLUMNS * 18 - font.width(empty)) / 2, y0 + ROWS * 9 - 4, TalkUi.DISABLED, false);
		}
	}

	/** How many, at a well's foot: as is to 999, then shortened (1.2k, 34k, 5.6M) and drawn smaller. */
	private void count(GuiGraphics g, long count, int x, int y) {
		if (count <= 1) return;
		String text = shortCount(count);
		g.pose().pushPose();
		g.pose().translate(0, 0, 200);
		if (text.length() <= 2) {
			g.drawString(font, text, x + 17 - font.width(text), y + 9, 0xFFFFFF, true);
		} else {
			float scale = 0.67f;
			g.pose().translate(x + 17, y + 12, 0);
			g.pose().scale(scale, scale, 1);
			g.drawString(font, text, -font.width(text), 0, 0xFFFFFF, true);
		}
		g.pose().popPose();
	}

	static String shortCount(long count) {
		if (count < 1000) return Long.toString(count);
		String[] units = {"k", "M", "B", "T", "Q"};
		double value = count;
		int unit = -1;
		while (value >= 1000 && unit < units.length - 1) {
			value /= 1000;
			unit++;
		}
		return (value < 10 ? String.format(Locale.ROOT, "%.1f", Math.floor(value * 10) / 10) : Long.toString((long) value)) + units[unit];
	}

	private void bar(GuiGraphics g) {
		int x = leftPos + BAR_X, y0 = topPos + GRID_Y, y1 = y0 + ROWS * 18;
		g.fill(x, y0 - 2, x + BAR_W, y1 + 2, TalkUi.GOLD_DIM);
		g.fill(x + 1, y0 - 1, x + BAR_W - 1, y1 + 1, SLOT_BG);
		int max = maxScroll();
		int track = y1 - y0;
		int thumb = max == 0 ? track : Math.max(10, track * ROWS / (ROWS + max));
		int top = max == 0 ? y0 : y0 + (track - thumb) * scroll / max;
		g.fill(x + 1, top, x + BAR_W - 1, top + thumb, max == 0 ? TalkUi.GOLD_DIM : TalkUi.GOLD);
		g.fill(x + 2, top + 1, x + BAR_W - 2, top + thumb - 1, max == 0 ? TalkUi.INNER : 0xFFF2C94C);
	}

	private void refineButton(GuiGraphics g, int mouseX, int mouseY) {
		int x = leftPos + W - 10 - REFINE_W, y = topPos + REFINE_Y;
		boolean last = ClientStorageRing.grade().next() == null;
		boolean hover = !last && onRefine(mouseX, mouseY);
		g.fill(x, y, x + REFINE_W, y + REFINE_H, last ? TalkUi.GOLD_DIM : TalkUi.GOLD);
		g.fill(x + 1, y + 1, x + REFINE_W - 1, y + REFINE_H - 1, hover ? TalkUi.PANEL_LIT : 0xF2120D0B);
		Component label = Component.translatable(ModLang.STORAGE_REFINE);
		g.drawString(font, label, x + (REFINE_W - font.width(label)) / 2, y + 3, last ? TalkUi.DISABLED : TalkUi.GOLD_TEXT, false);
	}

	/** The Refine button's tooltip: the next grade, the realm it wants, and each part of its cost against what the pack holds. */
	private List<Component> refineTooltip() {
		List<Component> lines = new ArrayList<>();
		StorageRings.Grade next = ClientStorageRing.grade().next();
		if (next == null) {
			lines.add(Component.translatable(ModLang.STORAGE_REFINE_LAST).withStyle(ChatFormatting.GOLD));
			return lines;
		}
		Component size = next.boundless() ? Component.translatable(ModLang.STORAGE_TOOLTIP_BOUNDLESS)
				: Component.translatable(ModLang.STORAGE_TOOLTIP_STACKS, next.stacks());
		lines.add(Component.translatable(ModLang.STORAGE_REFINE_TO, next.getDisplayName(), size).withStyle(ChatFormatting.GOLD));
		PlayerCultivation c = ClientCultivationData.get();
		boolean realm = !c.isMortal() && c.getRealm().ordinal() >= next.realm().ordinal();
		lines.add(Component.translatable(ModLang.STORAGE_REFINE_REALM, next.realm().getDisplayName())
				.withStyle(realm ? ChatFormatting.GREEN : ChatFormatting.RED));
		for (ItemStack cost : next.cost()) {
			int have = minecraft.player.getInventory().countItem(cost.getItem());
			lines.add(Component.literal(cost.getCount() + " x ").append(cost.getHoverName()).append(" (" + have + ")")
					.withStyle(have >= cost.getCount() || minecraft.player.getAbilities().instabuild ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		return lines;
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		Component name = fitted(title, W - 24 - REFINE_W - 8);
		g.drawString(font, name, 12, 7, TalkUi.GOLD_TEXT, false);
		// How full the space is: stacks used of the grade's; a boundless one, how many kinds.
		StorageRings.Grade grade = ClientStorageRing.grade();
		List<ClientStorageRing.Entry> all = ClientStorageRing.entries(menu.containerId);
		Component used = grade.boundless() ? Component.translatable(ModLang.STORAGE_USED_BOUNDLESS, all.size())
				: Component.translatable(ModLang.STORAGE_USED, ClientStorageRing.stacksUsed(menu.containerId), grade.stacks());
		boolean full = !grade.boundless() && ClientStorageRing.stacksUsed(menu.containerId) >= grade.stacks();
		g.drawString(font, used, W - 12 - font.width(used), 26, full ? TalkUi.WARNING : grade.boundless() ? JADE : TalkUi.MUTED, false);
	}

	/** {@code text}, cut with an ellipsis to {@code width}. */
	private Component fitted(Component text, int width) {
		if (font.width(text) <= width) return text;
		String plain = font.plainSubstrByWidth(text.getString(), width - font.width("..."));
		return Component.literal(plain + "...").withStyle(text.getStyle());
	}

	// --- Clicks, the wheel, keys ---

	private void act(int action, ItemStack like) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(action);
		buf.writeItem(like);
		ClientPlayNetworking.send(ModPackets.STORAGE_RING_ACTION, buf);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (onRefine(mouseX, mouseY) && button == 0) {
			if (ClientStorageRing.grade().next() != null) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
				act(StorageRings.REFINE, ItemStack.EMPTY);
			}
			return true;
		}
		if (onBar(mouseX, mouseY)) {
			draggingBar = true;
			scrollTo(mouseY);
			return true;
		}
		if (inGrid(mouseX, mouseY) && (button == 0 || button == 1)) {
			search.setFocused(false);
			if (!menu.getCarried().isEmpty()) {
				act(button == 1 ? StorageRings.PUT_ONE : StorageRings.PUT_ALL, ItemStack.EMPTY);
			} else {
				ClientStorageRing.Entry entry = entryAt(mouseX, mouseY);
				if (entry != null) act(hasShiftDown() ? StorageRings.TAKE_TO_PACK : button == 1 ? StorageRings.TAKE_HALF : StorageRings.TAKE_STACK, entry.kind());
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void scrollTo(double mouseY) {
		int max = maxScroll();
		if (max == 0) return;
		double share = (mouseY - topPos - GRID_Y) / (ROWS * 18.0);
		scroll = Mth.clamp((int) Math.round(share * max), 0, max);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingBar) {
			scrollTo(mouseY);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingBar = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (inGrid(mouseX, mouseY) || onBar(mouseX, mouseY)) {
			scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Typing in the search line: no key closes the screen or swaps a slot but Escape.
		if (search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
			search.keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
		return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !inGrid(mouseX, mouseY);
	}
}
