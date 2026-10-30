package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.Spell;
import com.example.defyingtheheavens.Spells;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * The spell wheel, the ability wheel's twin for spells: open for as long as its key ({@link ModKeybinds#SPELL_WHEEL}) is held.
 * Pointing toward a slot picks it; a click readies its spell for the cast key ({@link ModKeybinds#CAST_SPELL}), a right click
 * casts it at once. The readied spell's slot is ringed in gold, and a spell cooling down is dimmed with the seconds left on it.
 * Spells go on the wheel as they are learned; the Spells tab rearranges them. The player keeps moving while it's open, as with
 * the ability wheel; it opens once a spell can be cast ({@link ClientMethods#anyKnown}).
 */
public class SpellWheelScreen extends Screen implements HallFrame.Framed {
	private static final int RING = 70;
	private static final int SLOT_RADIUS = 18;
	private static final int ICON = 32;
	private static final int DEAD_ZONE = 22;
	/** The readied spell's rim, and the halo around it: the cold fire's cyan within the gold. */
	private static final int READIED_HALO = 0xC060E0F0;

	private final KeyMapping[] movement;
	private final boolean[] wasDown;

	public SpellWheelScreen() {
		super(Component.translatable(ModLang.SPELL_WHEEL_TITLE));
		Options options = Minecraft.getInstance().options;
		movement = new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump,
				options.keyShift, options.keySprint};
		wasDown = new boolean[movement.length];
		for (int i = 0; i < movement.length; i++) wasDown[i] = Boolean.TRUE.equals(AbilityWheelScreen.physicallyDown(movement[i]));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		if (!Boolean.FALSE.equals(AbilityWheelScreen.physicallyDown(ModKeybinds.SPELL_WHEEL)) && ClientMethods.anyKnown()) {
			walk();
		} else {
			onClose();
		}
	}

	/** The movement keys follow the keyboard while the wheel is open (see AbilityWheelScreen#walk). */
	private void walk() {
		for (int i = 0; i < movement.length; i++) {
			Boolean down = AbilityWheelScreen.physicallyDown(movement[i]);
			if (down == null) continue;
			if (!(movement[i] instanceof ToggleKeyMapping) || down != wasDown[i]) movement[i].setDown(down);
			wasDown[i] = down;
		}
	}

	@Override
	public void removed() {
		walk();
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (ModKeybinds.SPELL_WHEEL.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (ModKeybinds.SPELL_WHEEL.matchesMouse(button)) {
			onClose();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x50000000);
		int cx = width / 2;
		int cy = height / 2;
		int hovered = slotToward(mouseX, mouseY);
		Spell readied = ClientMethods.state().readiedSpell();

		AbilityWheel.drawBackdrop(g, cx, cy, RING + SLOT_RADIUS + 8);
		AbilityWheel.disc(g, cx, cy, RING - SLOT_RADIUS - 4, 0xE00C0F13);
		for (int i = 0; i < Spells.WHEEL_SLOTS; i++) {
			drawSlot(g, AbilityWheel.slotX(cx, i, RING), AbilityWheel.slotY(cy, i, RING), ClientMethods.shown(i), readied, i == hovered);
		}

		int textWidth = 2 * (RING - SLOT_RADIUS) - 10;
		Spell pointed = hovered < 0 ? null : ClientMethods.shown(hovered);
		if (pointed != null) {
			int cooldown = ClientMethods.cooldownLeft(pointed);
			// Each cost on its own line: a heaven method's spell costs qi, a ghost method's ghostly qi, one of both paths (TwoPaths) both.
			boolean usesQi = pointed.qiCost() > 0, usesGhostQi = pointed.cost() > 0;
			boolean ghostAffordable = ClientNetherworldData.ghostQi() >= pointed.cost() || isCreative();
			boolean qiAffordable = ClientCultivationData.get().getQi() >= pointed.qiCost() || isCreative();
			List<FormattedCharSequence> name = font.split(pointed.getDisplayName(), textWidth);
			int y = cy - (name.size() * 10 + (usesQi ? 10 : 0) + (usesGhostQi ? 10 : 0) + 10) / 2;
			for (FormattedCharSequence line : name) {
				g.drawCenteredString(font, line, cx, y, cooldown > 0 ? AbilityWheel.TEXT_MUTED : AbilityWheel.TEXT_GOLD);
				y += 10;
			}
			if (usesQi) {
				g.drawCenteredString(font, Component.translatable(ModLang.SPELLS_QI_COST, Math.round(pointed.qiCost())), cx, y + 1,
						qiAffordable ? 0x64B5F6 : 0xE05050);
				y += 10;
			}
			if (usesGhostQi) {
				g.drawCenteredString(font, Component.translatable(ModLang.SPELLS_COST, Math.round(pointed.cost())), cx, y + 1,
						ghostAffordable ? 0x7FDBE8 : 0xE05050);
				y += 10;
			}
			Component state = cooldown > 0 ? Component.translatable(ModLang.SPELLS_COOLING, (cooldown + 19) / 20)
					: Component.translatable(pointed == readied ? ModLang.SPELLS_READIED : ModLang.SPELLS_READY);
			g.drawCenteredString(font, state, cx, y + 2, cooldown > 0 ? AbilityWheel.TEXT_MUTED : pointed == readied ? 0x69F0AE : AbilityWheel.TEXT_MUTED);
		} else if (hovered >= 0) {
			g.drawCenteredString(font, Component.translatable(ModLang.WHEEL_EMPTY_SLOT), cx, cy - 4, AbilityWheel.TEXT_EMPTY);
		} else {
			boolean any = false;
			for (int i = 0; i < Spells.WHEEL_SLOTS; i++) any |= ClientMethods.shown(i) != null;
			Component hint = any ? Component.translatable(ModLang.SPELL_WHEEL_HINT,
					ModKeybinds.CAST_SPELL.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(AbilityWheel.TEXT_GOLD)))
					: Component.translatable(ModLang.SPELL_WHEEL_EMPTY);
			centred(g, Component.translatable(ModLang.SPELL_WHEEL_TITLE), hint, cx, cy, textWidth);
		}
	}

	private static boolean isCreative() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getAbilities().instabuild;
	}

	/**
	 * One slot, as the ability wheel draws them: the readied spell's ringed in cyan and gold, a cooling one dimmed with its seconds
	 * left over the icon.
	 */
	private void drawSlot(GuiGraphics g, int x, int y, Spell spell, Spell readied, boolean hovered) {
		if (spell != null && spell == readied) AbilityWheel.ring(g, x, y, SLOT_RADIUS + 4, SLOT_RADIUS + 1, READIED_HALO);
		if (hovered) AbilityWheel.ring(g, x, y, SLOT_RADIUS + 3, SLOT_RADIUS + 1, 0x80FFD54F);
		AbilityWheel.disc(g, x, y, SLOT_RADIUS + 1, spell == null ? AbilityWheel.SLOT_EMPTY_RIM : spell == readied ? AbilityWheel.GOLD_BRIGHT
				: AbilityWheel.GOLD);
		AbilityWheel.disc(g, x, y, SLOT_RADIUS, hovered ? AbilityWheel.SLOT_HOVER : AbilityWheel.SLOT);
		if (spell == null) return;
		int cooldown = ClientMethods.cooldownLeft(spell);
		drawIcon(g, spell, x - ICON / 2, y - ICON / 2, ICON, cooldown <= 0);
		if (cooldown > 0) {
			String seconds = String.valueOf((cooldown + 19) / 20);
			g.drawCenteredString(font, seconds, x, y - 4, 0xFFFFFF);
		}
	}

	/** A spell's icon, {@code size} pixels square; dimmed while it cools. */
	static void drawIcon(GuiGraphics g, Spell spell, int x, int y, int size, boolean lit) {
		RenderSystem.enableBlend();
		if (!lit) g.setColor(0.38F, 0.38F, 0.45F, 1.0F);
		g.blit(ClientMethods.icon(spell), x, y, size, size, 0, 0, 16, 16, 16, 16);
		g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableBlend();
	}

	private void centred(GuiGraphics g, Component title, Component text, int cx, int cy, int width) {
		List<FormattedCharSequence> lines = font.split(text, width);
		int y = cy - (12 + lines.size() * 10) / 2;
		g.drawCenteredString(font, title, cx, y, AbilityWheel.TEXT_GOLD);
		y += 12;
		for (FormattedCharSequence line : lines) {
			g.drawCenteredString(font, line, cx, y, AbilityWheel.TEXT_MUTED);
			y += 10;
		}
	}

	private int slotToward(double x, double y) {
		double dx = x - width / 2, dy = y - height / 2;
		return dx * dx + dy * dy > DEAD_ZONE * DEAD_ZONE ? AbilityWheel.slotInDirection(x, y, width / 2, height / 2) : -1;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int slot = slotToward(mouseX, mouseY);
		Spell spell = slot < 0 ? null : ClientMethods.shown(slot);
		if (spell != null && (button == 0 || button == 1)) {
			// The server readies or casts it and syncs back; the wheel shows the synced state.
			if (button == 0) ClientMethods.ready(spell);
			else ClientMethods.cast(spell);
			if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
