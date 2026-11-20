package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.ArtifactKind;
import com.example.defyingtheheavens.CircuitBoard;
import com.example.defyingtheheavens.CircuitDiagramItem;
import com.example.defyingtheheavens.ModItems;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.OverseerCoreBlockEntity;
import com.example.defyingtheheavens.OverseerMenu;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.example.defyingtheheavens.Puppetry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Overseer Core's screen (puppet phase 3; OverseerMenu), in the Hall's ink and gold: its nine diagrams on the left; on the right the
 * board of the one pointed at (or last pointed at), its lines in azure, and its wear in its keeper's puppets' fights over it
 * (PuppetBoardScreen#wearMap: cut cells in red, the deeper the more, hot ones ringed in orange), with how often in all.
 */
public class OverseerScreen extends AbstractContainerScreen<OverseerMenu> implements HallFrame.Framed {
	private static final int W = 300, H = 222, PANEL_X0 = 82, PANEL_X1 = 292, PANEL_Y0 = 22, PANEL_Y1 = 132, MAP = 96;
	private static final int SLOT_BG = 0xFF0B0E12, LINE = 0xFF5FD8FF, LINE_THICK = 0xFFA8F0FF, RING = 0xFFE8D27A;
	/** The diagram whose board is shown: the last pointed at (-1: the first there is). */
	private int shown = -1;

	public OverseerScreen(OverseerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = W;
		imageHeight = H;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		HallHud.panel(g, x + PANEL_X0, y + PANEL_Y0, x + PANEL_X1, y + PANEL_Y1, HallHud.FRAME, HallHud.CARD);
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			boolean own = i < OverseerCoreBlockEntity.SLOTS;
			boolean lit = own && i == chosen();
			HallHud.panel(g, x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, own && lit ? 0xFFFFD54F : own ? HallHud.FRAME_LIT : HallHud.FRAME, SLOT_BG);
		}
		int iy = y + OverseerMenu.INVENTORY_Y + 3 * 18 + 1;
		g.fill(x + OverseerMenu.INVENTORY_X, iy, x + OverseerMenu.INVENTORY_X + 9 * 18 - 2, iy + 1, TalkUi.GOLD_DIM);
		map(g, x, y);
	}

	/** The diagram shown: the one pointed at, else the last pointed at, else the first there is (-1: none). */
	private int chosen() {
		if (hoveredSlot != null && hoveredSlot.index < OverseerCoreBlockEntity.SLOTS && hoveredSlot.getItem().getItem() instanceof CircuitDiagramItem) {
			shown = hoveredSlot.index;
		}
		if (shown >= 0 && menu.slots.get(shown).getItem().getItem() instanceof CircuitDiagramItem) return shown;
		for (int i = 0; i < OverseerCoreBlockEntity.SLOTS; i++) if (menu.slots.get(i).getItem().getItem() instanceof CircuitDiagramItem) return i;
		return -1;
	}

	/** The chosen diagram's board, its lines and its wear, in the panel's left. */
	private void map(GuiGraphics g, int x, int y) {
		int i = chosen();
		if (i < 0) return;
		ItemStack diagram = menu.slots.get(i).getItem();
		PuppetBlueprint blueprint = CircuitDiagramItem.blueprint(diagram);
		if (blueprint == null) return;
		CircuitBoard board = new CircuitBoard(blueprint, CircuitDiagramItem.grade(diagram).ordinal(), 0, 0);
		Puppetry.lay(board, CircuitDiagramItem.strokes(diagram));
		int n = board.size, c = Math.max(3, MAP / n), bx = x + PANEL_X0 + 7, by = y + PANEL_Y0 + 7;
		g.fill(bx - 1, by - 1, bx + c * n + 1, by + c * n + 1, 0xFF3A2414);
		for (int cy = 0; cy < n; cy++) {
			for (int cx = 0; cx < n; cx++) {
				int x0 = bx + cx * c, y0 = by + cy * c;
				int ground = board.isSocket(cx, cy) ? 0xFFE0A030 : board.nodeAt(cx, cy) >= 0 ? 0xFFC8B89A : board.isJoint(cx, cy) ? 0xFF4A3A2A
						: (cx + cy) % 2 == 0 ? 0xFF1C1610 : 0xFF19140E;
				g.fill(x0, y0, x0 + c, y0 + c, ground);
				CircuitBoard.Stroke s = board.stroke(cx, cy);
				if (s == CircuitBoard.Stroke.NONE) continue;
				int colour = s == CircuitBoard.Stroke.RING || s == CircuitBoard.Stroke.LINK ? RING : s == CircuitBoard.Stroke.THICK ? LINE_THICK : LINE;
				int inset = s == CircuitBoard.Stroke.THICK ? 0 : 1;
				g.fill(x0 + inset, y0 + inset, x0 + c - inset, y0 + c - inset, colour);
			}
		}
		Puppetry.Wear wear = CircuitDiagramItem.wear(diagram);
		if (wear != null) PuppetBoardScreen.wearMap(g, wear.cuts, wear.heat, bx, by, c, n);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		HallHud.heading(g, font, title, 10, 7, W - 20);
		int i = chosen();
		int x0 = PANEL_X0 + 7 + MAP + 8, x1 = PANEL_X1 - 6, y = PANEL_Y0 + 7;
		if (i < 0) {
			for (FormattedCharSequence line : font.split(Component.translatable(ModLang.OVERSEER_EMPTY), PANEL_X1 - PANEL_X0 - 14)) {
				g.drawString(font, line, PANEL_X0 + 7, y, TalkUi.MUTED, false);
				y += 10;
			}
			return;
		}
		ItemStack diagram = menu.slots.get(i).getItem();
		PuppetBlueprint blueprint = CircuitDiagramItem.blueprint(diagram);
		ArtifactGrade grade = CircuitDiagramItem.grade(diagram);
		java.util.List<Component> lines = new java.util.ArrayList<>();
		if (blueprint != null) lines.add(ModItems.artifact(ArtifactKind.frameOf(blueprint), grade).getDescription().copy().withStyle(s -> s.withColor(TalkUi.GOLD_TEXT)));
		Puppetry.Wear wear = CircuitDiagramItem.wear(diagram);
		if (wear == null || wear.cutsTotal == 0 && wear.overheats == 0) {
			lines.add(Component.translatable(ModLang.OVERSEER_NO_WEAR).withStyle(s -> s.withColor(TalkUi.MUTED)));
		} else {
			lines.add(Component.translatable(ModLang.OVERSEER_CUTS, wear.cutsTotal, wear.mostCuts()).withStyle(s -> s.withColor(0xFFE05050)));
			lines.add(Component.translatable(ModLang.OVERSEER_HEAT, wear.overheats).withStyle(s -> s.withColor(0xFFFF9A3A)));
			lines.add(Component.translatable(ModLang.OVERSEER_KEY).withStyle(s -> s.withColor(TalkUi.MUTED)));
		}
		for (Component line : lines) {
			for (FormattedCharSequence part : font.split(line, x1 - x0)) {
				if (y + 9 > PANEL_Y1 - 4) break;
				g.drawString(font, part, x0, y, TalkUi.CREAM, false);
				y += 10;
			}
			y += 2;
		}
	}
}
