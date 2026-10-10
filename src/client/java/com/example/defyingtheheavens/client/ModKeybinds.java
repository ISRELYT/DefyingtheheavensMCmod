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

	public static void register() {
		OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_MENU, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, ModLang.KEY_CATEGORY));
		MEDITATE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_MEDITATE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, ModLang.KEY_CATEGORY));
		CIRCULATE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				ModLang.KEY_CIRCULATE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, ModLang.KEY_CATEGORY));
	}

	private ModKeybinds() {}
}
