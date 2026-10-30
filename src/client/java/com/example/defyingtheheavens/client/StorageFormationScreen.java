package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CraftMastery;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModPackets;
import com.example.defyingtheheavens.PlayerCultivation;
import com.example.defyingtheheavens.StorageFormationMenu;
import com.example.defyingtheheavens.StorageFormations;
import com.example.defyingtheheavens.TwoPaths;
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

import static com.example.defyingtheheavens.StorageFormationMenu.COLUMNS;
import static com.example.defyingtheheavens.StorageFormationMenu.GRID_X;
import static com.example.defyingtheheavens.StorageFormationMenu.GRID_Y;
import static com.example.defyingtheheavens.StorageFormationMenu.ROWS;

/**
 * A storage formation's screen, in the mod's lacquer and gold (TalkUi): its name over a gold rule, a search line and how full the space is,
 * then the space itself, a grid of what it holds (one well a kind, how many beneath it, a k or an M once the figures grow), scrolled by
 * the wheel or the bar beside it; the pack below. Clicking the grid takes a stack (right: half, shift: straight into the pack) or, with
 * something carried, puts it in (right: one); shift-clicking the pack puts a stack in.
 * <p>
 * Its owner has two buttons beside the search line: Refine, raising it a tier (its cost, realm and mastery on the tooltip), and Trust, which
 * turns the grid's place into the list of those it opens to (a name box to add one, a cross beside each to strike them off).
 */
public class StorageFormationScreen extends AbstractContainerScreen<StorageFormationMenu> implements HallFrame.Framed {
	private static final int W = StorageFormationMenu.WIDTH, H = StorageFormationMenu.HEIGHT;
	private static final int SLOT_BG = 0xFF0B0E12;
	private static final int JADE = 0x7FE0A0;
	/** The scroll bar, beside the grid. */
	private static final int BAR_X = GRID_X + COLUMNS * 18 + 4, BAR_W = 6;
	/** The search line's well, under the title. */
	private static final int SEARCH_Y = 23, SEARCH_W = 88;
	/**
	 * The owner's buttons, in the search line's row at its right (the title above has the width to itself: "Low-Grade Stor..." beside
	 * them before): Trust, then Refine, their right edge the scroll bar's.
	 */
	private static final int BUTTON_W = 38, BUTTON_H = 12, BUTTON_Y = SEARCH_Y;
	private static final int REFINE_X = BAR_X + BAR_W - BUTTON_W, TRUST_X = REFINE_X - 4 - BUTTON_W;
	/** How full the space is: centred on the rule between the grid and the pack. */
	private static final int FILL_Y = GRID_Y + ROWS * 18 + 3;

	private EditBox search;
	private EditBox name;
	/** The trusted list in the grid's place. */
	private boolean trustPage;
	/** The first row of the grid in view. */
	private int scroll;
	private boolean draggingBar;
	/** The space as filtered by the search line, and what it was filtered from. */
	private List<ClientStorageSpace.Entry> shown = List.of();
	private int shownVersion = -1;
	private String shownFilter = null;

	public StorageFormationScreen(StorageFormationMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = W;
		imageHeight = H;
	}

	@Override
	protected void init() {
		super.init();
		search = new EditBox(font, leftPos + GRID_X + 3, topPos + SEARCH_Y + 2, SEARCH_W - 6, 9, Component.translatable(ModLang.STORAGE_SEARCH));
		search.setBordered(false);
		search.setMaxLength(40);
		search.setTextColor(TalkUi.CREAM);
		search.setHint(Component.translatable(ModLang.STORAGE_SEARCH).withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(search);
		name = new EditBox(font, leftPos + GRID_X + 3, topPos + GRID_Y + 3, COLUMNS * 18 - 48, 9, Component.translatable(ModLang.STORAGE_TRUST_NAME));
		name.setBordered(false);
		name.setMaxLength(16);
		name.setTextColor(TalkUi.CREAM);
		name.setHint(Component.translatable(ModLang.STORAGE_TRUST_NAME).withStyle(ChatFormatting.DARK_GRAY));
		name.visible = false;
		addRenderableWidget(name);
	}

	@Override
	protected void containerTick() {
		search.tick();
		name.tick();
		name.visible = trustPage;
		search.visible = !trustPage;
		if (trustPage && !ClientStorageSpace.owns()) trustPage = false;
	}

	// --- What is shown ---

	private List<ClientStorageSpace.Entry> shown() {
		String filter = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
		if (shownVersion != ClientStorageSpace.version() || !filter.equals(shownFilter)) {
			List<ClientStorageSpace.Entry> list = new ArrayList<>();
			for (ClientStorageSpace.Entry entry : ClientStorageSpace.entries(menu.containerId)) {
				if (filter.isEmpty() || entry.kind().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)) list.add(entry);
			}
			shown = list;
			shownVersion = ClientStorageSpace.version();
			shownFilter = filter;
		}
		return shown;
	}

	private int maxScroll() {
		int rows = (shown().size() + COLUMNS - 1) / COLUMNS;
		return Math.max(0, rows - ROWS);
	}

	/** The entry under the mouse, or null. */
	private ClientStorageSpace.Entry entryAt(double mouseX, double mouseY) {
		int col = (int) Math.floor((mouseX - leftPos - GRID_X) / 18), row = (int) Math.floor((mouseY - topPos - GRID_Y) / 18);
		if (trustPage || !inGrid(mouseX, mouseY) || col < 0 || col >= COLUMNS || row < 0 || row >= ROWS) return null;
		int index = (scroll + row) * COLUMNS + col;
		List<ClientStorageSpace.Entry> list = shown();
		return index < list.size() ? list.get(index) : null;
	}

	private boolean inGrid(double mouseX, double mouseY) {
		double x = mouseX - leftPos - GRID_X, y = mouseY - topPos - GRID_Y;
		return x >= 0 && x < COLUMNS * 18 && y >= 0 && y < ROWS * 18;
	}

	private boolean onBar(double mouseX, double mouseY) {
		double x = mouseX - leftPos - BAR_X, y = mouseY - topPos - GRID_Y;
		return !trustPage && x >= -1 && x < BAR_W + 1 && y >= 0 && y < ROWS * 18;
	}

	private boolean onButton(double mouseX, double mouseY, int bx) {
		double x = mouseX - leftPos - bx, y = mouseY - topPos - BUTTON_Y;
		return ClientStorageSpace.owns() && x >= 0 && x < BUTTON_W && y >= 0 && y < BUTTON_H;
	}

	/** The Add button beside the name box, and a trusted name's cross (its row), on the trust page. */
	private boolean onAdd(double mouseX, double mouseY) {
		double x = mouseX - leftPos - (GRID_X + COLUMNS * 18 - 40), y = mouseY - topPos - (GRID_Y + 1);
		return trustPage && x >= 0 && x < 40 && y >= 0 && y < 12;
	}

	private int crossAt(double mouseX, double mouseY) {
		if (!trustPage) return -1;
		List<ClientStorageSpace.Trusted> trusted = ClientStorageSpace.trusted();
		for (int i = 0; i < trusted.size(); i++) {
			int[] at = trustedAt(i);
			if (mouseX >= leftPos + at[0] && mouseX < leftPos + at[0] + 8 && mouseY >= topPos + at[1] && mouseY < topPos + at[1] + 9) return i;
		}
		return -1;
	}

	/** Where the {@code i}th trusted name's cross stands: two columns of eight under the name box. */
	private static int[] trustedAt(int i) {
		return new int[] {GRID_X + 2 + (i / 8) * (COLUMNS * 9), GRID_Y + 18 + (i % 8) * 9};
	}

	// --- Drawing ---

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		scroll = Mth.clamp(scroll, 0, maxScroll());
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		if (menu.getCarried().isEmpty()) {
			ClientStorageSpace.Entry hovered = entryAt(mouseX, mouseY);
			if (hovered != null) {
				List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hovered.kind()));
				lines.add(Component.translatable(ModLang.STORAGE_COUNT, String.format("%,d", hovered.count())).withStyle(ChatFormatting.GOLD));
				g.renderTooltip(font, lines, hovered.kind().getTooltipImage(), mouseX, mouseY);
			} else if (onButton(mouseX, mouseY, REFINE_X)) {
				g.renderComponentTooltip(font, refineTooltip(), mouseX, mouseY);
			} else if (onButton(mouseX, mouseY, TRUST_X)) {
				g.renderComponentTooltip(font, List.of(Component.translatable(ModLang.STORAGE_TRUST_TIP)), mouseX, mouseY);
			}
		}
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		if (!trustPage) {
			// The search line's well.
			g.fill(x + GRID_X, y + SEARCH_Y, x + GRID_X + SEARCH_W, y + SEARCH_Y + 12, TalkUi.GOLD_DIM);
			g.fill(x + GRID_X + 1, y + SEARCH_Y + 1, x + GRID_X + SEARCH_W - 1, y + SEARCH_Y + 11, SLOT_BG);
		}
		if (ClientStorageSpace.owns()) {
			button(g, REFINE_X, Component.translatable(ModLang.STORAGE_REFINE), ClientStorageSpace.tier().next() != null, onButton(mouseX, mouseY, REFINE_X), false);
			button(g, TRUST_X, Component.translatable(ModLang.STORAGE_TRUST), true, onButton(mouseX, mouseY, TRUST_X), trustPage);
		}
		if (trustPage) trusted(g, mouseX, mouseY);
		else {
			grid(g, mouseX, mouseY);
			bar(g);
		}
		// The pack's wells, a gold rule between it and the hotbar.
		for (Slot slot : menu.slots) {
			g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, TalkUi.GOLD_DIM);
			g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT_BG);
		}
		int iy = y + StorageFormationMenu.INVENTORY_Y + 3 * 18 + 1;
		g.fill(x + StorageFormationMenu.INVENTORY_X, iy, x + StorageFormationMenu.INVENTORY_X + 9 * 18 - 2, iy + 1, TalkUi.GOLD_DIM);
		// Between the space and the pack, a thin rule: how full the space is at its middle (renderLabels), or a notch on the trust page.
		int ry = y + FILL_Y + 3;
		if (trustPage) {
			g.fill(x + 14, ry, x + W - 14, ry + 1, TalkUi.GOLD_DIM);
			g.fill(x + W / 2 - 3, ry - 1, x + W / 2 + 3, ry + 2, TalkUi.GOLD);
		} else {
			int half = font.width(fillText()) / 2 + 5;
			g.fill(x + 14, ry, x + W / 2 - half, ry + 1, TalkUi.GOLD_DIM);
			g.fill(x + W / 2 + half, ry, x + W - 14, ry + 1, TalkUi.GOLD_DIM);
		}
	}

	private void button(GuiGraphics g, int bx, Component label, boolean live, boolean hover, boolean on) {
		int x = leftPos + bx, y = topPos + BUTTON_Y;
		// The Hall's buttons; the page open (the trust page's) in jade.
		HallHud.panel(g, x, y, x + BUTTON_W, y + BUTTON_H, !live ? HallHud.FRAME_OFF : hover || on ? HallHud.FRAME_LIT : HallHud.FRAME,
				on ? HallHud.ROW_OPEN : hover && live ? HallHud.ROW_LIT : HallHud.CARD);
		g.drawString(font, label, x + (BUTTON_W - font.width(label)) / 2, y + 2, live ? TalkUi.GOLD_TEXT : TalkUi.DISABLED, false);
	}

	private void grid(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = leftPos + GRID_X, y0 = topPos + GRID_Y;
		HallHud.panel(g, x0 - 2, y0 - 2, x0 + COLUMNS * 18 + 2, y0 + ROWS * 18 + 2, HallHud.FRAME_LIT, TalkUi.INNER);
		List<ClientStorageSpace.Entry> list = shown();
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < COLUMNS; col++) {
				int sx = x0 + col * 18, sy = y0 + row * 18;
				g.fill(sx, sy, sx + 18, sy + 18, TalkUi.GOLD_DIM);
				g.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT_BG);
				int index = (scroll + row) * COLUMNS + col;
				if (index >= list.size()) continue;
				ClientStorageSpace.Entry entry = list.get(index);
				if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x40FFF4C9);
				g.renderItem(entry.kind(), sx + 1, sy + 1);
				g.renderItemDecorations(font, entry.kind(), sx + 1, sy + 1, null);
				count(g, entry.count(), sx + 1, sy + 1);
			}
		}
		if (list.isEmpty()) {
			// Nothing at all, or nothing the search line lets through.
			Component empty = Component.translatable(ClientStorageSpace.entries(menu.containerId).isEmpty() ? ModLang.STORAGE_EMPTY : ModLang.STORAGE_NO_MATCH);
			g.drawString(font, empty, x0 + (COLUMNS * 18 - font.width(empty)) / 2, y0 + ROWS * 9 - 4, TalkUi.DISABLED, false);
		}
	}

	/** The trust page: the name box with its Add, then the names, a cross before each. */
	private void trusted(GuiGraphics g, int mouseX, int mouseY) {
		int x0 = leftPos + GRID_X, y0 = topPos + GRID_Y;
		HallHud.panel(g, x0 - 2, y0 - 2, x0 + COLUMNS * 18 + 2, y0 + ROWS * 18 + 2, HallHud.FRAME_LIT, TalkUi.INNER);
		HallHud.panel(g, x0, y0, x0 + COLUMNS * 18 - 44, y0 + 13, HallHud.FRAME, SLOT_BG);
		int ax = x0 + COLUMNS * 18 - 40, ay = y0 + 1;
		HallHud.button(g, font, ax, ay, 40, 12, Component.translatable(ModLang.STORAGE_TRUST_ADD), true, onAdd(mouseX, mouseY));
		List<ClientStorageSpace.Trusted> trusted = ClientStorageSpace.trusted();
		if (trusted.isEmpty()) {
			Component none = Component.translatable(ModLang.STORAGE_TRUST_NONE);
			g.drawString(font, none, x0 + (COLUMNS * 18 - font.width(none)) / 2, y0 + ROWS * 9 + 2, TalkUi.DISABLED, false);
		}
		int cross = crossAt(mouseX, mouseY);
		for (int i = 0; i < trusted.size(); i++) {
			int[] at = trustedAt(i);
			g.drawString(font, "x", leftPos + at[0] + 1, topPos + at[1], i == cross ? TalkUi.WARNING : TalkUi.MUTED, false);
			g.drawString(font, trusted.get(i).name(), leftPos + at[0] + 9, topPos + at[1], TalkUi.CREAM, false);
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

	/** The Refine button's tooltip: the next tier, the realm and mastery it wants, and each part of its cost against what the pack holds. */
	private List<Component> refineTooltip() {
		List<Component> lines = new ArrayList<>();
		StorageFormations.Tier next = ClientStorageSpace.tier().next();
		if (next == null) {
			lines.add(Component.translatable(ModLang.STORAGE_REFINE_LAST).withStyle(ChatFormatting.GOLD));
			return lines;
		}
		Component size = next.boundless() ? Component.translatable(ModLang.STORAGE_REFINE_ENDLESS)
				: Component.translatable(ModLang.STORAGE_REFINE_STACKS, next.stacks());
		lines.add(Component.translatable(ModLang.STORAGE_REFINE_TO, next.getDisplayName(), size).withStyle(ChatFormatting.GOLD));
		PlayerCultivation c = ClientCultivationData.get();
		boolean realm = !c.isMortal() && TwoPaths.awakens(c, next.seal(), false) != null; // on any path, as the server weighs it
		lines.add(Component.translatable(ModLang.STORAGE_REFINE_REALM, next.seal().getDisplayName())
				.withStyle(realm ? ChatFormatting.GREEN : ChatFormatting.RED));
		if (next.mastery() != CraftMastery.Rank.APPRENTICE) {
			boolean mastered = CraftMastery.rank(c, CraftMastery.Craft.FORMATIONS).ordinal() >= next.mastery().ordinal();
			lines.add(Component.translatable(ModLang.STORAGE_REFINE_MASTERY, next.mastery().getDisplayName())
					.withStyle(mastered ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		for (ItemStack cost : next.cost()) {
			int have = minecraft.player.getInventory().countItem(cost.getItem());
			lines.add(Component.literal(cost.getCount() + " x ").append(cost.getHoverName()).append(" (" + have + ")")
					.withStyle(have >= cost.getCount() || minecraft.player.getAbilities().instabuild ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		return lines;
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		HallHud.heading(g, font, fitted(this.title, W - 33), 10, 7, W - 20);
		if (trustPage) return;
		StorageFormations.Tier tier = ClientStorageSpace.tier();
		boolean full = !tier.boundless() && ClientStorageSpace.stacksUsed(menu.containerId) >= tier.stacks();
		Component fill = fillText();
		g.drawString(font, fill, (W - font.width(fill)) / 2, FILL_Y, full ? TalkUi.WARNING : tier.boundless() ? JADE : TalkUi.MUTED, false);
	}

	/** How full the space is: stacks used of the tier's; a boundless one, how many kinds. */
	private Component fillText() {
		StorageFormations.Tier tier = ClientStorageSpace.tier();
		return tier.boundless() ? Component.translatable(ModLang.STORAGE_USED_BOUNDLESS, ClientStorageSpace.entries(menu.containerId).size())
				: Component.translatable(ModLang.STORAGE_USED, ClientStorageSpace.stacksUsed(menu.containerId), tier.stacks());
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
		ClientPlayNetworking.send(ModPackets.STORAGE_ACTION, buf);
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && onButton(mouseX, mouseY, REFINE_X)) {
			if (ClientStorageSpace.tier().next() != null) {
				click();
				act(StorageFormations.REFINE, ItemStack.EMPTY);
			}
			return true;
		}
		if (button == 0 && onButton(mouseX, mouseY, TRUST_X)) {
			click();
			trustPage = !trustPage;
			name.setFocused(trustPage);
			search.setFocused(false);
			return true;
		}
		if (trustPage) {
			if (button == 0 && onAdd(mouseX, mouseY)) {
				addTrusted();
				return true;
			}
			int cross = crossAt(mouseX, mouseY);
			if (button == 0 && cross >= 0) {
				click();
				FriendlyByteBuf buf = PacketByteBufs.create();
				buf.writeBoolean(false);
				buf.writeUUID(ClientStorageSpace.trusted().get(cross).id());
				ClientPlayNetworking.send(ModPackets.STORAGE_TRUST, buf);
				return true;
			}
			if (inGrid(mouseX, mouseY)) {
				name.setFocused(true);
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, button);
		}
		if (onBar(mouseX, mouseY)) {
			draggingBar = true;
			scrollTo(mouseY);
			return true;
		}
		if (inGrid(mouseX, mouseY) && (button == 0 || button == 1)) {
			search.setFocused(false);
			if (!menu.getCarried().isEmpty()) {
				act(button == 1 ? StorageFormations.PUT_ONE : StorageFormations.PUT_ALL, ItemStack.EMPTY);
			} else {
				ClientStorageSpace.Entry entry = entryAt(mouseX, mouseY);
				if (entry != null) {
					act(hasShiftDown() ? StorageFormations.TAKE_TO_PACK : button == 1 ? StorageFormations.TAKE_HALF : StorageFormations.TAKE_STACK, entry.kind());
				}
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	/** The name in the box, sent to be trusted (the server looks them up, and says if it can't). */
	private void addTrusted() {
		String typed = name.getValue().trim();
		if (typed.isEmpty()) return;
		click();
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(true);
		buf.writeUtf(typed, 16);
		ClientPlayNetworking.send(ModPackets.STORAGE_TRUST, buf);
		name.setValue("");
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
		if (!trustPage && (inGrid(mouseX, mouseY) || onBar(mouseX, mouseY))) {
			scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Typing in the search line or the name box: no key closes the screen or swaps a slot but Escape; Enter adds the name.
		if (name.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
			addTrusted();
			return true;
		}
		if ((search.isFocused() || name.isFocused()) && keyCode != GLFW.GLFW_KEY_ESCAPE) {
			(search.isFocused() ? search : name).keyPressed(keyCode, scanCode, modifiers);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
		return super.hasClickedOutside(mouseX, mouseY, left, top, button) && !inGrid(mouseX, mouseY);
	}
}
