package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.CircuitDiagramItem;
import com.example.defyingtheheavens.CraftMastery;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.example.defyingtheheavens.PuppetFactoryBlockEntity;
import com.example.defyingtheheavens.PuppetFactoryMenu;
import com.example.defyingtheheavens.Puppetry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Puppet Factory's screen (puppet phase 3; PuppetFactoryMenu), in the Hall's ink and gold: its diagram's slot, its matter's nine and its
 * cores' four on the left; on the right what it is doing or why it waits, its copy's progress, what each copy takes (each thing's count in
 * its matter against what is wanted, from the diagram laid in it), and the mode button: building copies, or repair only.
 */
public class PuppetFactoryScreen extends AbstractContainerScreen<PuppetFactoryMenu> implements HallFrame.Framed {
	private static final int W = 300, H = 222, PANEL_X0 = 124, PANEL_X1 = 292, PANEL_Y0 = 22, PANEL_Y1 = 126, BUTTON_H = 14;
	private static final int SLOT_BG = 0xFF0B0E12, JADE = 0x7FE0A0;

	public PuppetFactoryScreen(PuppetFactoryMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		imageWidth = W;
		imageHeight = H;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g);
		super.render(g, mouseX, mouseY, partialTick);
		renderTooltip(g, mouseX, mouseY);
		if (in(mouseX, mouseY, leftPos + PANEL_X0 + 6, topPos + PANEL_Y1 - BUTTON_H - 6, PANEL_X1 - PANEL_X0 - 12, BUTTON_H)) {
			g.renderTooltip(font, font.split(Component.translatable(ModLang.FACTORY_MODE_TIP, (int) Puppetry.FACTORY_REACH), 200), mouseX, mouseY);
		}
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		HallHud.panel(g, x + PANEL_X0, y + PANEL_Y0, x + PANEL_X1, y + PANEL_Y1, HallHud.FRAME, HallHud.CARD);
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			boolean own = i < PuppetFactoryBlockEntity.SLOTS;
			HallHud.panel(g, x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, own ? HallHud.FRAME_LIT : HallHud.FRAME, SLOT_BG);
		}
		int iy = y + PuppetFactoryMenu.INVENTORY_Y + 3 * 18 + 1;
		g.fill(x + PuppetFactoryMenu.INVENTORY_X, iy, x + PuppetFactoryMenu.INVENTORY_X + 9 * 18 - 2, iy + 1, TalkUi.GOLD_DIM);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		HallHud.heading(g, font, title, 10, 7, W - 20);
		// How many it has built, at the heading's right.
		Component built = Component.translatable(ModLang.FACTORY_BUILT_COUNT, menu.get(PuppetFactoryBlockEntity.BUILT));
		g.drawString(font, built, W - 12 - font.width(built), 8, TalkUi.MUTED, false);
		Component diagram = Component.translatable(ModLang.FACTORY_DIAGRAM);
		g.drawString(font, diagram, PuppetFactoryMenu.DIAGRAM_X + 8 - font.width(diagram) / 2, PuppetFactoryMenu.DIAGRAM_Y - 11, TalkUi.MUTED, false);
		g.drawString(font, Component.translatable(ModLang.FACTORY_MATTER), PuppetFactoryMenu.MATTER_X, PuppetFactoryMenu.MATTER_Y - 10, TalkUi.MUTED, false);
		g.drawString(font, Component.translatable(ModLang.FACTORY_CORES), PuppetFactoryMenu.CORES_X, PuppetFactoryMenu.CORES_Y - 10, TalkUi.MUTED, false);
		panel(g, mouseX - leftPos, mouseY - topPos);
	}

	/** The right panel: its state, its progress, a copy's needs, the mode button. */
	private void panel(GuiGraphics g, int mx, int my) {
		int x0 = PANEL_X0 + 6, x1 = PANEL_X1 - 6, y = PANEL_Y0 + 6;
		int state = menu.get(PuppetFactoryBlockEntity.STATE);
		ItemStack diagram = menu.slots.get(PuppetFactoryBlockEntity.DIAGRAM).getItem();
		boolean has = diagram.getItem() instanceof CircuitDiagramItem && CircuitDiagramItem.blueprint(diagram) != null;
		for (FormattedCharSequence line : font.split(stateLine(state, diagram), x1 - x0)) {
			g.drawString(font, line, x0, y, state == PuppetFactoryBlockEntity.READY ? JADE : state == PuppetFactoryBlockEntity.REPAIRING ? TalkUi.GOLD_TEXT
					: TalkUi.WARNING, false);
			y += 10;
		}
		// Its copy's progress, as a gold bar.
		if (state == PuppetFactoryBlockEntity.READY) {
			int filled = (x1 - x0) * menu.get(PuppetFactoryBlockEntity.PROGRESS) / 1000;
			g.fill(x0, y + 1, x1, y + 4, 0xFF2A1F14);
			g.fill(x0, y + 1, x0 + filled, y + 4, 0xFFFFD54F);
			y += 7;
		}
		// What a copy takes, each thing's count in its matter against what is wanted.
		if (has) {
			PuppetBlueprint blueprint = CircuitDiagramItem.blueprint(diagram);
			ArtifactGrade grade = CircuitDiagramItem.grade(diagram);
			List<ItemStack> matter = new ArrayList<>();
			for (int i = PuppetFactoryBlockEntity.MATTER; i < PuppetFactoryBlockEntity.MATTER + PuppetFactoryBlockEntity.MATTER_SLOTS; i++) {
				matter.add(menu.slots.get(i).getItem());
			}
			g.drawString(font, Component.translatable(ModLang.FACTORY_NEEDS, grade.getDisplayName()), x0, y + 1, TalkUi.MUTED, false);
			y += 11;
			int col = 0;
			for (Map.Entry<Item, Integer> need : PuppetFactoryBlockEntity.needs(blueprint, grade).entrySet()) {
				int have = PuppetFactoryBlockEntity.count(matter, need.getKey());
				int cx = x0 + col * 54;
				g.renderItem(new ItemStack(need.getKey()), cx, y);
				String count = Math.min(have, need.getValue()) + "/" + need.getValue();
				g.drawString(font, count, cx + 18, y + 5, have >= need.getValue() ? JADE : TalkUi.WARNING, false);
				if (in(mx, my, cx, y, 16, 16)) {
					g.renderTooltip(font, List.of(new ItemStack(need.getKey()).getHoverName().getVisualOrderText()), mx, my);
				}
				if (++col == 3) {
					col = 0;
					y += 17;
				}
			}
		}
		// The mode button, at the panel's foot.
		int by = PANEL_Y1 - BUTTON_H - 6;
		Component mode = Component.translatable(menu.get(PuppetFactoryBlockEntity.MODE) == PuppetFactoryBlockEntity.REPAIR_ONLY ? ModLang.FACTORY_MODE_REPAIR
				: ModLang.FACTORY_MODE_BUILD);
		HallHud.button(g, font, x0, by, x1 - x0, BUTTON_H, mode, true, in(mx, my, x0, by, x1 - x0, BUTTON_H));
	}

	/** What it is doing, or why it waits. */
	private Component stateLine(int state, ItemStack diagram) {
		return switch (state) {
			case PuppetFactoryBlockEntity.READY -> Component.translatable(ModLang.FACTORY_BUILDING, menu.get(PuppetFactoryBlockEntity.PROGRESS) / 10);
			case PuppetFactoryBlockEntity.NO_RANK -> Component.translatable(ModLang.FACTORY_NO_RANK,
					CraftMastery.Rank.values()[Math.min(CraftMastery.Rank.values().length - 1, CircuitDiagramItem.rankWanted(diagram))].getDisplayName());
			case PuppetFactoryBlockEntity.NO_SLOTS -> Component.translatable(ModLang.FACTORY_NO_SLOTS);
			case PuppetFactoryBlockEntity.NO_MATTER -> Component.translatable(ModLang.FACTORY_NO_MATTER);
			case PuppetFactoryBlockEntity.NO_CORE -> Component.translatable(ModLang.FACTORY_NO_CORE,
					Puppetry.lowest(CircuitDiagramItem.grade(diagram)).getDisplayName());
			case PuppetFactoryBlockEntity.WONT_SEAL -> Component.translatable(ModLang.FACTORY_WONT_SEAL);
			case PuppetFactoryBlockEntity.OWNER_AWAY -> Component.translatable(ModLang.FACTORY_AWAY);
			case PuppetFactoryBlockEntity.REPAIRING -> Component.translatable(ModLang.FACTORY_REPAIRING, (int) Puppetry.FACTORY_REACH);
			case PuppetFactoryBlockEntity.NO_OWNER -> Component.translatable(ModLang.FACTORY_NO_OWNER);
			default -> Component.translatable(ModLang.FACTORY_NO_DIAGRAM);
		};
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		int x0 = leftPos + PANEL_X0 + 6, x1 = leftPos + PANEL_X1 - 6, by = topPos + PANEL_Y1 - BUTTON_H - 6;
		if (button == 0 && in(mx, my, x0, by, x1 - x0, BUTTON_H) && minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}
}
