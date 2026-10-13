package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
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
	}

	private ModKeybinds() {}
}
