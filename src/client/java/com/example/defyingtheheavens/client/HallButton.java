package com.example.defyingtheheavens.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

/**
 * A button in the Hall's look (HallHud#button; the user's ask, 2026-10-30: "go through the other menus and make them consistent"), in
 * place of vanilla's stone ones on every screen of the mod. Made as vanilla's are: {@code HallButton.of(message, onPress).bounds(x, y, w,
 * h).build()}. One given its place in the artifact's pixels ({@link #art}) is drawn there as the menu's mockup draws its buttons: a box
 * with a border of 3, the words in Pixelify Sans at their size; "primary" ones (Meditate, a Breakthrough ready) in jade in gold, faded
 * ones while they can't be pressed.
 */
public class HallButton extends Button {
	/** Whether it is the one open of a row of tabs (in jade, a gold diamond before its words, as the Hall's open tab). */
	private final BooleanSupplier open;
	/** Its place in the artifact's pixels (NaN: drawn in the GUI's, HallHud#button), its words' size, and whether it is a primary one. */
	private float artX = Float.NaN, artY, artW, artH, size = 22;
	private BooleanSupplier primary = () -> false;

	protected HallButton(int x, int y, int width, int height, Component message, OnPress onPress, BooleanSupplier open) {
		super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
		this.open = open;
	}

	public static Builder of(Component message, OnPress onPress) {
		return new Builder(message, onPress);
	}

	/**
	 * Places it at (x, y), {@code w} by {@code h} in the artifact's pixels within a framed screen (HallFrame: three of them to one of the
	 * frame's), its words at {@code size}; its bounds for the mouse follow.
	 */
	public HallButton art(float x, float y, float w, float h, float size) {
		artX = x;
		artY = y;
		artW = w;
		artH = h;
		this.size = size;
		setX(Math.round(x / 3));
		setY(Math.round(y / 3));
		setWidth(Math.max(1, Math.round((x + w) / 3) - getX()));
		height = Math.max(1, Math.round((y + h) / 3) - getY());
		return this;
	}

	/** Drawn in jade in gold while {@code primary} holds (a Meditate, a Breakthrough ready). */
	public HallButton primary(BooleanSupplier primary) {
		this.primary = primary;
		return this;
	}

	@Override
	protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		if (!Float.isNaN(artX)) {
			renderArt(g);
			return;
		}
		Font font = HallFonts.current();
		if (!open.getAsBoolean()) {
			HallHud.button(g, font, getX(), getY(), width, height, getMessage(), active, isHoveredOrFocused());
			return;
		}
		HallHud.panel(g, getX(), getY(), getX() + width, getY() + height, HallHud.FRAME_LIT, HallHud.ROW_OPEN);
		int words = font.width(getMessage()), x = getX() + (width - words - 9) / 2;
		HallHud.diamond(g, x, getY() + (height - 5) / 2, 5, HallHud.GOLD);
		g.drawString(font, getMessage(), x + 9, getY() + (height - 7) / 2, HallHud.GOLD, false);
	}

	/** As the menu's mockup draws its buttons, in the artifact's pixels. */
	private void renderArt(GuiGraphics g) {
		boolean lit = isHoveredOrFocused(), main = primary.getAsBoolean();
		int border, ground, words;
		if (!active) {
			border = 0xFF3A3A3A;
			ground = 0xFF1A1D21;
			words = 0x6C6C6C;
		} else if (main) {
			border = lit ? 0xFFFFD54F : 0xFFB8860B;
			ground = 0xFF1E5A49;
			words = 0xFFFFFF;
		} else {
			border = lit ? 0xFFB8860B : 0xFF6E5214;
			ground = lit ? 0xFF1E242C : 0xFF161B22;
			words = lit ? 0xFFD54F : 0xE6D3A3;
		}
		HallArt.beginFramed(g);
		try {
			HallArt.box(g, artX, artY, artW, artH, 3, border, ground);
			String text = HallArt.fit(getMessage().getString(), artW - 16, size);
			HallArt.textCentred(g, text, artX + artW / 2, artY + (artH - size * 1.2f) / 2, size, size * 1.2f, words, false);
		} finally {
			HallArt.end(g);
		}
	}

	/** As vanilla's Button.Builder: where it stands and how large, its tooltip, whether it is the tab open, then the button. */
	public static final class Builder {
		private final Component message;
		private final OnPress onPress;
		private int x, y, width = 150, height = 20;
		private Tooltip tooltip;
		private BooleanSupplier open = () -> false;

		private Builder(Component message, OnPress onPress) {
			this.message = message;
			this.onPress = onPress;
		}

		public Builder bounds(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder tooltip(Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Builder open(BooleanSupplier open) {
			this.open = open;
			return this;
		}

		public HallButton build() {
			HallButton button = new HallButton(x, y, width, height, message, onPress, open);
			button.setTooltip(tooltip);
			return button;
		}
	}
}
