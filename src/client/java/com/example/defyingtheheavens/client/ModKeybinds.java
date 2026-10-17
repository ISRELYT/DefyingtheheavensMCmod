package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.PlayerCultivation;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeybinds {
	public static KeyMapping OPEN_MENU;
	public static KeyMapping MEDITATE;
	public static KeyMapping CIRCULATE;
	/** Held down to show the ability wheel ({@link AbilityWheelScreen}); letting go closes it. */
	public static KeyMapping ABILITY_WHEEL;
	/** Held down to show the spell wheel ({@link SpellWheelScreen}); letting go closes it. */
	public static KeyMapping SPELL_WHEEL;
	/** Casts the spell readied on the spell wheel. */
	public static KeyMapping CAST_SPELL;
	/** Opens the Storage Ring worn (or else the first one carried): StorageRingScreen. */
	public static KeyMapping STORAGE_RING;
	/** Switches the ability in the ability wheel's top slot on or off, without opening the wheel ({@link QuickAbilities}). */
	public static KeyMapping QUICK_ABILITY;
	/** One for each wheel slot, unbound until the player binds them: each switches its slot's ability ({@link QuickAbilities}). */
	public static final KeyMapping[] ABILITY_SLOTS = new KeyMapping[PlayerCultivation.WHEEL_SLOTS];

	public static void register() {
		OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_MENU, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, ModLang.KEY_CATEGORY));
		MEDITATE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_MEDITATE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, ModLang.KEY_CATEGORY));
		CIRCULATE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_CIRCULATE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, ModLang.KEY_CATEGORY));
		ABILITY_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_ABILITY_WHEEL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, ModLang.KEY_CATEGORY));
		SPELL_WHEEL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_SPELL_WHEEL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, ModLang.KEY_CATEGORY));
		CAST_SPELL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_CAST_SPELL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, ModLang.KEY_CATEGORY));
		STORAGE_RING = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_STORAGE_RING, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, ModLang.KEY_CATEGORY));
		QUICK_ABILITY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_QUICK_ABILITY, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, ModLang.KEY_CATEGORY));
		for (int slot = 0; slot < ABILITY_SLOTS.length; slot++) {
			ABILITY_SLOTS[slot] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
					ModLang.KEY_ABILITY_SLOT_PREFIX + (slot + 1), InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), ModLang.KEY_CATEGORY));
		}
	}

	private static final java.util.List<KeyMapping> ALL = new java.util.ArrayList<>();

	/** Every binding of the mod's, once registered (KeyMappingMixin gives them presses on keys they share with others). */
	public static java.util.List<KeyMapping> all() {
		if (ALL.isEmpty() && OPEN_MENU != null) {
			ALL.addAll(java.util.List.of(OPEN_MENU, MEDITATE, CIRCULATE, ABILITY_WHEEL, SPELL_WHEEL, CAST_SPELL, STORAGE_RING, QUICK_ABILITY));
			ALL.addAll(java.util.Arrays.asList(ABILITY_SLOTS));
		}
		return ALL;
	}

	private ModKeybinds() {}
}
