package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.ModDimensions;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's local player stands frozen until the chunk it is in has loaded. Space has no land to wait for (AstralPlanets: its stars
 * and planets are drawn, never blocks), and the swift outfly any loading of its empty chunks (AstralRealm#spaceSpeed: from Void
 * Shattering on, hundreds of blocks a second): there, the player flies on regardless.
 */
@Mixin(LocalPlayer.class)
public abstract class SpaceLoadingMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;hasChunkAt(II)Z"))
	private boolean dth$theVoidNeedsNoLand(boolean loaded) {
		return loaded || ModDimensions.isSpace(((LocalPlayer) (Object) this).level().dimension());
	}
}
