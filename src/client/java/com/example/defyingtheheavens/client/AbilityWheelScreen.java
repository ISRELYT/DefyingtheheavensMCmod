package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The ability wheel: open for as long as its key ({@link ModKeybinds#ABILITY_WHEEL}) is held. Pointing toward a slot picks
 * it; a click selects its ability (ringed in cyan) for the Use Selected Ability key ({@link QuickAbilities}), and a second click on
 * it, or a right-click, switches it on or off now, the same as the switch on the menu's Abilities tab; so does pressing the slot's
 * number (1 the top, then clockwise); several can be switched before letting go. The slots are arranged on the Abilities tab
 * ("Arrange Wheel").
 * <p>
 * The player keeps moving while it's open: walking, jumping (and so Qi Flight), sneaking and sprinting pass through
 * ({@link #walk}). The mouse points at the wheel, so the view can't turn until it closes. It only opens once the player
 * has awakened an ability ({@link #canOpen}).
 */
public class AbilityWheelScreen extends Screen {
	/** How far the slots sit from the centre, their radius, and their icons' size (the 32x32 art pixel for pixel). */
	private static final int RING = 70;
	private static final int SLOT_RADIUS = 18;
	private static final int ICON = 32;
	/** Inside this, the pointer picks no slot, so the wheel can open without one already chosen. */
	private static final int DEAD_ZONE = 22;
	/** The selected ability's halo: the spell wheel's readied cyan. */
	private static final int SELECTED_HALO = 0xC060E0F0;

	/** The movement keys passed through to the player while the wheel is open, and whether each was down last tick. */
	private final KeyMapping[] movement;
	private final boolean[] wasDown;

	public AbilityWheelScreen() {
		super(Component.translatable(ModLang.WHEEL_TITLE));
		Options options = Minecraft.getInstance().options;
		movement = new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft, options.keyRight, options.keyJump,
				options.keyShift, options.keySprint};
		// As they are now, so a toggled sneak or sprint isn't flipped just because its key was down when the wheel opened.
		wasDown = new boolean[movement.length];
		for (int i = 0; i < movement.length; i++) wasDown[i] = Boolean.TRUE.equals(physicallyDown(movement[i]));
	}

	/** The wheel needs something to switch: no awakened ability (a mortal), no wheel. */
	public static boolean canOpen(PlayerCultivation c) {
		for (Ability ability : Ability.values()) {
			if (ability.isUnlocked(c)) return true;
		}
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void tick() {
		// Runs before the player's own tick, so the movement keys set here move the player this same tick.
		if (!Boolean.FALSE.equals(physicallyDown(ModKeybinds.ABILITY_WHEEL)) && canOpen(ClientCultivationData.get())) {
			walk();
		} else {
			onClose();
		}
	}

	/**
	 * Any open screen holds the movement keys released, so they're set here from the keyboard itself. Plain keys follow it
	 * every tick; toggled ones (the Sneak and Sprint "Toggle" options) only on a press, as they would without the wheel.
	 */
	private void walk() {
		for (int i = 0; i < movement.length; i++) {
			Boolean down = physicallyDown(movement[i]);
			if (down == null) continue;
			if (!(movement[i] instanceof ToggleKeyMapping) || down != wasDown[i]) movement[i].setDown(down);
			wasDown[i] = down;
		}
	}

	/**
	 * Whether a mapping's key is down on the keyboard (or mouse) right now, since an open screen clears every mapping's own
	 * state. Null for a key bound by scan code, which can't be asked: the wheel's own key then closes on
	 * {@link #keyReleased} instead.
	 */
	static Boolean physicallyDown(KeyMapping mapping) {
		InputConstants.Key key = KeyBindingHelper.getBoundKeyOf(mapping);
		long window = Minecraft.getInstance().getWindow().getWindow();
		return switch (key.getType()) {
			case KEYSYM -> key.getValue() != InputConstants.UNKNOWN.getValue() && InputConstants.isKeyDown(window, key.getValue());
			case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
			case SCANCODE -> null;
		};
	}

	/** Once more as it closes, so a key let go in the same moment as the wheel's isn't left held down. */
	@Override
	public void removed() {
		walk();
	}

	/**
	 * The number keys switch their slot straight off (1 the top, then on clockwise), no pointing needed; so do the Quick Ability
	 * and Ability Wheel Slot keys, which an open screen would otherwise swallow.
	 */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		KeyMapping[] hotbar = Minecraft.getInstance().options.keyHotbarSlots;
		for (int slot = 0; slot < PlayerCultivation.WHEEL_SLOTS; slot++) {
			if (slot < hotbar.length && hotbar[slot].matches(keyCode, scanCode) || ModKeybinds.ABILITY_SLOTS[slot].matches(keyCode, scanCode)) {
				QuickAbilities.toggle(slot);
				return true;
			}
		}
		if (ModKeybinds.QUICK_ABILITY.matches(keyCode, scanCode)) {
			QuickAbilities.useSelected();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (ModKeybinds.ABILITY_WHEEL.matches(keyCode, scanCode)) {
			onClose();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (ModKeybinds.ABILITY_WHEEL.matchesMouse(button)) {
			onClose();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, width, height, 0x50000000); // a light dim: the world stays in view
		PlayerCultivation c = ClientCultivationData.get();
		int cx = width / 2;
		int cy = height / 2;
		int hovered = slotToward(mouseX, mouseY);

		AbilityWheel.drawBackdrop(g, cx, cy, RING + SLOT_RADIUS + 8);
		AbilityWheel.disc(g, cx, cy, RING - SLOT_RADIUS - 4, 0xC0101018); // nearly solid behind the text in the middle
		Ability selected = QuickAbilities.selected(c);
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			Ability ability = AbilityWheel.shown(c, i);
			// The selected one ringed in cyan within the gold, as the spell wheel rings its readied spell.
			if (ability != null && ability == selected) {
				AbilityWheel.ring(g, AbilityWheel.slotX(cx, i, RING), AbilityWheel.slotY(cy, i, RING), SLOT_RADIUS + 4, SLOT_RADIUS + 1, SELECTED_HALO);
			}
			AbilityWheel.drawSlot(g, AbilityWheel.slotX(cx, i, RING), AbilityWheel.slotY(cy, i, RING), SLOT_RADIUS, ability,
					ability != null && c.isAbilityEnabled(ability), i == hovered, ICON);
			// Its number key, just outside it.
			int number = RING + SLOT_RADIUS + 3;
			g.drawCenteredString(font, String.valueOf(i + 1), AbilityWheel.slotX(cx, i, number), AbilityWheel.slotY(cy, i, number) - 4,
					ability == null ? 0x505050 : 0xB8A060);
		}

		// The middle names what the pointer is on, or says what the wheel is for.
		int textWidth = 2 * (RING - SLOT_RADIUS) - 10;
		Ability pointed = hovered < 0 ? null : AbilityWheel.shown(c, hovered);
		if (pointed != null) {
			boolean on = c.isAbilityEnabled(pointed);
			List<FormattedCharSequence> name = font.split(pointed.getDisplayName(), textWidth);
			int y = cy - (name.size() * 10 + 10) / 2;
			for (FormattedCharSequence line : name) {
				g.drawCenteredString(font, line, cx, y, on ? 0xFFD700 : 0x9E9E9E);
				y += 10;
			}
			g.drawCenteredString(font, Component.translatable(on ? ModLang.ABILITY_ON : ModLang.ABILITY_OFF), cx, y + 1, on ? 0x69F0AE : 0x9E9E9E);
			if (pointed == selected) g.drawCenteredString(font, Component.translatable(ModLang.WHEEL_SELECTED), cx, y + 12, 0x8FEAE4);
		} else if (hovered >= 0) {
			g.drawCenteredString(font, Component.translatable(ModLang.WHEEL_EMPTY_SLOT), cx, cy - 4, 0x707070);
		} else {
			boolean any = AbilityWheel.anyShown(c);
			centred(g, Component.translatable(ModLang.WHEEL_TITLE), any ? Component.translatable(ModLang.WHEEL_HINT,
					ModKeybinds.QUICK_ABILITY.getTranslatedKeyMessage().copy().withStyle(style -> style.withColor(0xFFD700)))
					: Component.translatable(ModLang.WHEEL_EMPTY), cx, cy, textWidth);
		}
	}

	/** A gold title over grey lines, centred on (cx, cy) as a block. */
	private void centred(GuiGraphics g, Component title, Component text, int cx, int cy, int width) {
		List<FormattedCharSequence> lines = font.split(text, width);
		int y = cy - (12 + lines.size() * 10) / 2;
		g.drawCenteredString(font, title, cx, y, 0xFFD700);
		y += 12;
		for (FormattedCharSequence line : lines) {
			g.drawCenteredString(font, line, cx, y, 0x9E9E9E);
			y += 10;
		}
	}

	/** The slot the pointer is toward, or -1 inside the dead zone. */
	private int slotToward(double x, double y) {
		double dx = x - width / 2, dy = y - height / 2;
		return dx * dx + dy * dy > DEAD_ZONE * DEAD_ZONE ? AbilityWheel.slotInDirection(x, y, width / 2, height / 2) : -1;
	}

	/**
	 * A click selects the slot's ability for the Use Selected Ability key, and a click on the one already selected switches it (or
	 * carries it out); a right-click switches it now either way.
	 */
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int slot = slotToward(mouseX, mouseY);
		if ((button == 0 || button == 1) && slot >= 0) {
			Ability ability = AbilityWheel.shown(ClientCultivationData.get(), slot);
			if (ability != null) {
				if (button == 0 && ability != QuickAbilities.selected(ClientCultivationData.get())) {
					QuickAbilities.select(ability);
					if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				} else {
					// The server flips it and syncs back; the slot shows the synced state.
					QuickAbilities.switchNow(ability);
				}
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
