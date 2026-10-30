package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.HeavenAltar;
import com.example.defyingtheheavens.HeavenAltarBlockEntity;
import com.example.defyingtheheavens.HeavenAltarMenu;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * The Heaven Altar's screen, in the Hall's ink and gold (TalkUi, HallHud, AbilityWheel): the nine offering slots in a gold ring
 * round a dial that shows the chance (a gold arc filling round it as it rises), and beside it what makes the chance: the
 * offering's tier and worth (and what the next tier needs), the altar's grandeur and its points by part, the height bonus,
 * the reward a ritual would bring, and where the player stands with the heavens. The offering is weighed as items are laid
 * down; the rest was judged by the server when the screen opened (HeavenAltarMenu#judge).
 */
public class HeavenAltarScreen extends AbstractContainerScreen<HeavenAltarMenu> implements HallFrame.Framed {
	private static final int W = 300, H = 224;
	/** The panel of figures beside the ring. */
	private static final int PANEL_X0 = 121, PANEL_X1 = 292, PANEL_Y0 = 23, PANEL_Y1 = 128;
	private static final int JADE = 0x7FE0A0;
	private static final int SLOT_BG = 0xFF0B0E12;
	private static final String[] NUMERALS = {"", "I", "II", "III"};

	public HeavenAltarScreen(HeavenAltarMenu menu, Inventory inventory, Component title) {
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

	// --- What the figures come to ---

	private HeavenAltar.Offering offering() { return HeavenAltar.Offering.of(menu.offeringWorth()); }

	private HeavenAltar.Grandeur grandeur() { return HeavenAltar.Grandeur.of(menu.get(HeavenAltarMenu.POINTS)); }

	private double chance() { return HeavenAltar.chance(offering(), grandeur(), menu.get(HeavenAltarMenu.HEIGHT)); }

	/** Whether a ritual could begin here now: open sky, and the heavens' leave still to be asked. */
	private boolean ready() { return menu.get(HeavenAltarMenu.SKY) == 1 && menu.get(HeavenAltarMenu.STATUS) == HeavenAltarMenu.READY; }

	// --- Drawing ---

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = leftPos, y = topPos;
		TalkUi.panel(g, x, y, x + W, y + H, TalkUi.GOLD);
		ring(g, x + HeavenAltarMenu.RING_X, y + HeavenAltarMenu.RING_Y);
		HallHud.panel(g, x + PANEL_X0, y + PANEL_Y0, x + PANEL_X1, y + PANEL_Y1, HallHud.FRAME, HallHud.CARD);
		// The slots: ink wells in a dark-gold frame (the offering ring's in brighter gold).
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			boolean offering = i < HeavenAltarBlockEntity.SLOTS;
			HallHud.panel(g, x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, offering ? HallHud.FRAME_LIT : HallHud.FRAME, SLOT_BG);
		}
		// A gold rule between the backpack and the hotbar.
		int iy = y + HeavenAltarMenu.INVENTORY_Y + 3 * 18 + 1;
		g.fill(x + HeavenAltarMenu.INVENTORY_X, iy, x + HeavenAltarMenu.INVENTORY_X + 9 * 18 - 2, iy + 1, TalkUi.GOLD_DIM);
	}

	/** The offering ring: a gold band, faint spokes to each slot, the dial at its heart with the chance's arc round it. */
	private void ring(GuiGraphics g, int cx, int cy) {
		int r = HeavenAltarMenu.RING_RADIUS;
		AbilityWheel.ring(g, cx, cy, r + 11, r + 10, HallHud.FRAME_LIT);
		AbilityWheel.ring(g, cx, cy, r + 1, r - 1, TalkUi.GOLD_DIM);
		// The dial: lacquer, a gold rim, and the chance as a gold arc from the top, clockwise.
		int dial = 22;
		AbilityWheel.disc(g, cx, cy, dial + 3, TalkUi.GOLD_DIM);
		double share = ready() ? chance() : 0;
		int arc = ready() ? 0xFF000000 | colour(chance()) : TalkUi.GOLD_DIM;
		for (int dy = -dial - 3; dy <= dial + 3; dy++) {
			for (int dx = -dial - 3; dx <= dial + 3; dx++) {
				double d = Math.sqrt((dx + 0.5) * (dx + 0.5) + (dy + 0.5) * (dy + 0.5));
				if (d < dial || d > dial + 3) continue;
				double angle = Math.atan2(dx + 0.5, -(dy + 0.5)); // 0 at the top, clockwise
				if (angle < 0) angle += 2 * Math.PI;
				if (angle / (2 * Math.PI) <= share) g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, arc);
			}
		}
		AbilityWheel.disc(g, cx, cy, dial, TalkUi.INNER);
		AbilityWheel.disc(g, cx, cy, dial - 1, TalkUi.PANEL);
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		HallHud.heading(g, font, title, 10, 7, W - 20);
		dial(g, HeavenAltarMenu.RING_X, HeavenAltarMenu.RING_Y);
		figures(g);
		// Where the player stands with the heavens, under the ring and the figures.
		List<FormattedCharSequence> lines = font.split(state(), W - 16);
		int y = 132 - 5 * (lines.size() - 1);
		for (FormattedCharSequence line : lines) {
			g.drawString(font, line, (W - font.width(line)) / 2, y, ready() ? TalkUi.CREAM : TalkUi.WARNING, false);
			y += 10;
		}
	}

	/** In the dial: "Chance" and the figure, large (the offering's tier is in the panel beside). */
	private void dial(GuiGraphics g, int cx, int cy) {
		Component label = Component.translatable(ModLang.ALTAR_CHANCE);
		g.drawString(font, label, cx - font.width(label) / 2, cy - 12, TalkUi.MUTED, false);
		String figure = HeavenAltar.percent(chance());
		int colour = ready() ? colour(chance()) : TalkUi.DISABLED;
		g.pose().pushPose();
		g.pose().translate(cx - font.width(figure), cy - 1, 0);
		g.pose().scale(2, 2, 1);
		g.drawString(font, figure, 0, 0, colour, false);
		g.pose().popPose();
	}

	/** The panel: offering, altar (and its parts), height, reward. */
	private void figures(GuiGraphics g) {
		int x0 = PANEL_X0 + 7, x1 = PANEL_X1 - 7, y = PANEL_Y0 + 6;
		HeavenAltar.Offering offering = offering();
		double worth = menu.offeringWorth();
		row(g, Component.translatable(ModLang.ALTAR_OFFERING), offering.displayName(),
				offering == HeavenAltar.Offering.UNWORTHY ? TalkUi.WARNING : TalkUi.GOLD_TEXT, x0, x1, y);
		y += 10;
		HeavenAltar.Offering next = offering.ordinal() + 1 < HeavenAltar.Offering.values().length ? HeavenAltar.Offering.values()[offering.ordinal() + 1] : null;
		Component worthLine = next == null ? Component.translatable(ModLang.ALTAR_WORTH, number(worth))
				: Component.translatable(ModLang.ALTAR_WORTH_NEXT, number(worth), next.displayName(), number(next.minWorth));
		if (font.width(worthLine) > x1 - x0) worthLine = Component.translatable(ModLang.ALTAR_WORTH, number(worth));
		g.drawString(font, worthLine, x0, y, TalkUi.MUTED, false);
		y += 13;

		HeavenAltar.Grandeur grandeur = grandeur();
		row(g, Component.translatable(ModLang.ALTAR_ALTAR), Component.literal("").append(grandeur.displayName()).append(" +" + grandeur.percent + "%"),
				TalkUi.GOLD_TEXT, x0, x1, y);
		y += 11;
		int[][] parts = {
				{HeavenAltarMenu.PLATFORM, HeavenAltarMenu.TERRACES}, {HeavenAltarMenu.NOBLE, HeavenAltarMenu.PILLARS},
				{HeavenAltarMenu.LIGHTS, HeavenAltarMenu.TREASURES}};
		String[][] names = {{"platform", "terraces"}, {"noble", "pillars"}, {"lights", "treasures"}};
		int mid = (x0 + x1) / 2 + 4;
		for (int r = 0; r < parts.length; r++) {
			part(g, names[r][0], menu.get(parts[r][0]), x0, mid - 6, y);
			part(g, names[r][1], menu.get(parts[r][1]), mid, x1, y);
			y += 10;
		}
		y += 2;

		int height = menu.get(HeavenAltarMenu.HEIGHT);
		row(g, Component.translatable(ModLang.ALTAR_HEIGHT), Component.literal("+" + height + "%"), TalkUi.GOLD_TEXT, x0, x1, y);
		Component level = Component.translatable(ModLang.ALTAR_HEIGHT_Y, menu.get(HeavenAltarMenu.ALTAR_Y));
		g.drawString(font, level, x0 + font.width(Component.translatable(ModLang.ALTAR_HEIGHT)) + 5, y, TalkUi.MUTED, false);
		y += 12;

		g.fill(x0, y - 4, x1, y - 3, TalkUi.GOLD_DIM);
		Component reward = Component.translatable(ModLang.ALTAR_REWARD);
		Component permission = Component.translatable(ModLang.ALTAR_REWARD_LEAVE);
		// The label only where it leaves room for what is granted (a longer translation keeps just the grant).
		if (font.width(reward) + 4 + font.width(permission) <= x1 - x0) g.drawString(font, reward, x0, y, TalkUi.MUTED, false);
		g.drawString(font, permission, x1 - font.width(permission), y, TalkUi.CREAM, false);
		y += 10;
		if (offering.favour > 0) {
			int have = menu.get(HeavenAltarMenu.FAVOUR);
			Component favour = have >= offering.favour
					? Component.translatable(ModLang.ALTAR_REWARD_HAVE, NUMERALS[have])
					: Component.translatable(ModLang.ALTAR_REWARD_FAVOUR, NUMERALS[offering.favour],
							Math.round(offering.favour * PlayerCultivation.FAVOUR_PER_STEP * 100));
			g.drawString(font, favour, x1 - font.width(favour), y, have >= offering.favour ? TalkUi.MUTED : JADE, false);
		}
	}

	/** "Label ........ value": the label left, the value right. */
	private void row(GuiGraphics g, Component label, Component value, int colour, int x0, int x1, int y) {
		g.drawString(font, label, x0, y, TalkUi.MUTED, false);
		g.drawString(font, value, x1 - font.width(value), y, colour, false);
	}

	/** One of the altar's parts and its points, bright if it scores. */
	private void part(GuiGraphics g, String id, int points, int x0, int x1, int y) {
		Component name = Component.translatable(ModLang.altarPart(id));
		String value = String.valueOf(points);
		g.drawString(font, name, x0, y, points > 0 ? TalkUi.CREAM : TalkUi.DISABLED, false);
		g.drawString(font, value, x1 - font.width(value), y, points > 0 ? TalkUi.GOLD_TEXT : TalkUi.DISABLED, false);
	}

	/** Where the player stands: ready (how to begin), or why not. */
	private Component state() {
		if (menu.get(HeavenAltarMenu.SKY) != 1) return Component.translatable(ModLang.altarState("no_sky"));
		return Component.translatable(ModLang.altarState(switch (menu.get(HeavenAltarMenu.STATUS)) {
			case HeavenAltarMenu.GRANTED -> "granted";
			case HeavenAltarMenu.NOT_YET -> "not_yet";
			case HeavenAltarMenu.UNTIL_DAWN -> "dawn";
			case HeavenAltarMenu.BUSY -> "busy";
			default -> "ready";
		}));
	}

	/** Jade for a good chance, gold for a fair one, red for a slim one. */
	private static int colour(double chance) {
		return chance >= 0.7 ? JADE : chance >= 0.35 ? TalkUi.GOLD_TEXT : TalkUi.WARNING;
	}

	private static String number(double v) {
		return v == Math.floor(v) ? String.format("%,d", (long) v) : String.format("%,.1f", v);
	}
}
