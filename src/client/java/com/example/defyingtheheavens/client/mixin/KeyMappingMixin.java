package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ModKeybinds;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Minecraft gives each key to one binding only (whichever its map holds last), so a mod key sharing its key with another (Cast
 * Spell's C and Use Selected Ability's X are also vanilla's creative hotbar activators) might never hear it. The mod's own
 * bindings on the pressed key are given the press, and its held state, as well.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	@Shadow @Final private static Map<InputConstants.Key, KeyMapping> MAP;

	@Inject(method = "click", at = @At("TAIL"))
	private static void dth$clickSharers(InputConstants.Key key, CallbackInfo ci) {
		KeyMapping owner = MAP.get(key);
		for (KeyMapping mine : ModKeybinds.all()) {
			if (mine != owner && KeyBindingHelper.getBoundKeyOf(mine).equals(key)) {
				KeyMappingAccessor sharer = (KeyMappingAccessor) mine;
				sharer.dth$setClickCount(sharer.dth$getClickCount() + 1);
			}
		}
	}

	@Inject(method = "set", at = @At("TAIL"))
	private static void dth$holdSharers(InputConstants.Key key, boolean down, CallbackInfo ci) {
		KeyMapping owner = MAP.get(key);
		for (KeyMapping mine : ModKeybinds.all()) {
			if (mine != owner && KeyBindingHelper.getBoundKeyOf(mine).equals(key)) mine.setDown(down);
		}
	}
}
