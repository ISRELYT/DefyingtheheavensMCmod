package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.PlayerCultivation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * What the ability wheel ({@link AbilityWheelScreen}) and its editor on the Abilities tab share: each ability's icon, where
 * the slots sit, and the round shapes they are drawn with, in the menu's lacquer and gold.
 */
final class AbilityWheel {
	static final int GOLD = 0xFFB8860B;
	static final int GOLD_DARK = 0xFF6E5214;
	static final int GOLD_BRIGHT = 0xFFFFD700;
	static final int LACQUER = 0xE0101018;
	static final int SLOT = 0xFF1A1A26;
	static final int SLOT_HOVER = 0xFF2C2C44;
	static final int SLOT_EMPTY_RIM = 0xFF3A3A4A;
	static final int SLOT_OFF_RIM = 0xFF6A6A78;
	static final int ON = 0xFF69F0AE;

	private static final Map<Ability, ResourceLocation> ICONS = new EnumMap<>(Ability.class);

	/** Each ability's 16x16 icon, textures/gui/ability/<id>.png. */
	static ResourceLocation icon(Ability ability) {
		return ICONS.computeIfAbsent(ability, a -> new ResourceLocation(DefyingTheHeavens.MOD_ID, "textures/gui/ability/" + a.getId() + ".png"));
	}

	/** Draws an ability's icon {@code size} pixels square; an ability switched off is drawn dim. */
	static void drawIcon(GuiGraphics g, Ability ability, int x, int y, int size, boolean lit) {
		RenderSystem.enableBlend(); // the icons' soft edges (qi steam) are partly see-through
		if (!lit) g.setColor(0.42F, 0.42F, 0.48F, 1.0F);
		g.blit(icon(ability), x, y, size, size, 0, 0, 16, 16, 16, 16);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	/**
	 * The ability shown in a slot: the one placed there, if the cultivator has awakened it. One placed there that isn't
	 * awakened (a lower realm's suppression, say) shows as an empty slot, so the wheel never names what's still to come.
	 */
	static Ability shown(PlayerCultivation c, int slot) {
		Ability ability = c.getWheelSlot(slot);
		return ability != null && ability.isUnlocked(c) ? ability : null;
	}

	/** True if any slot shows an ability. */
	static boolean anyShown(PlayerCultivation c) {
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			if (shown(c, i) != null) return true;
		}
		return false;
	}

	/** Where a slot's centre is, clockwise from the top, {@code ring} pixels out from the wheel's centre. */
	static int slotX(int cx, int slot, int ring) {
		return cx + (int) Math.round(Math.sin(angle(slot)) * ring);
	}

	static int slotY(int cy, int slot, int ring) {
		return cy - (int) Math.round(Math.cos(angle(slot)) * ring);
	}

	private static double angle(int slot) {
		return Math.PI * 2 * slot / PlayerCultivation.WHEEL_SLOTS;
	}

	/** The slot whose direction from the centre the point lies in (each slot owns an eighth of the circle). */
	static int slotInDirection(double x, double y, int cx, int cy) {
		double angle = Math.atan2(x - cx, cy - y); // clockwise from the top
		if (angle < 0) angle += Math.PI * 2;
		double share = Math.PI * 2 / PlayerCultivation.WHEEL_SLOTS;
		return (int) Math.floor((angle + share / 2) / share) % PlayerCultivation.WHEEL_SLOTS;
	}

	/** The slot whose circle of {@code radius} (plus a pixel of grace) holds the point, or -1. */
	static int slotUnder(double x, double y, int cx, int cy, int ring, int radius) {
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			double dx = x - slotX(cx, i, ring), dy = y - slotY(cy, i, ring);
			if (dx * dx + dy * dy <= (radius + 1) * (radius + 1)) return i;
		}
		return -1;
	}

	/** A filled circle of radius {@code r} around the pixel corner (cx, cy), one row at a time. */
	static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
		ring(g, cx, cy, r, 0, color);
	}

	/** The band between radius {@code inner} and {@code outer} around (cx, cy); inner 0 fills the whole disc. */
	static void ring(GuiGraphics g, int cx, int cy, int outer, int inner, int color) {
		for (int row = -outer; row < outer; row++) {
			double y = row + 0.5;
			int o = (int) Math.round(Math.sqrt(Math.max(0, outer * outer - y * y)));
			if (o == 0) continue;
			int i = Math.abs(y) < inner ? (int) Math.round(Math.sqrt(inner * inner - y * y)) : 0;
			if (i == 0) {
				g.fill(cx - o, cy + row, cx + o, cy + row + 1, color);
			} else {
				g.fill(cx - o, cy + row, cx - i, cy + row + 1, color);
				g.fill(cx + i, cy + row, cx + o, cy + row + 1, color);
			}
		}
	}

	/**
	 * One slot: a dark disc with a rim. The rim is gold for an ability switched on, grey for one switched off, faint for an
	 * empty slot; the hovered slot is lit and haloed. A small green lamp under an ability that is on.
	 */
	static void drawSlot(GuiGraphics g, int x, int y, int radius, Ability ability, boolean on, boolean hovered, int iconSize) {
		if (hovered) ring(g, x, y, radius + 3, radius + 1, 0x80FFD700);
		disc(g, x, y, radius + 1, ability == null ? SLOT_EMPTY_RIM : on ? GOLD : SLOT_OFF_RIM);
		disc(g, x, y, radius, hovered ? SLOT_HOVER : SLOT);
		if (ability == null) return;
		drawIcon(g, ability, x - iconSize / 2, y - iconSize / 2, iconSize, on);
		if (on) {
			g.fill(x - 2, y + radius - 2, x + 2, y + radius + 1, GOLD_DARK);
			g.fill(x - 1, y + radius - 1, x + 1, y + radius, ON);
		}
	}

	/** The wheel's lacquer disc with a gold rim, its shaded inner line like the Qi bar's trim. */
	static void drawBackdrop(GuiGraphics g, int cx, int cy, int radius) {
		disc(g, cx, cy, radius, LACQUER);
		ring(g, cx, cy, radius + 2, radius, GOLD);
		ring(g, cx, cy, radius, radius - 1, GOLD_DARK);
	}

	private AbilityWheel() {}
}
