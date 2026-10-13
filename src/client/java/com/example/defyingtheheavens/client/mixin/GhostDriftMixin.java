package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientGhosts;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The local player as a ghost: holding jump, the fall slows to a gentle drift ({@link ClientGhosts#drift}). */
@Mixin(Player.class)
public abstract class GhostDriftMixin {
	@Inject(method = "travel", at = @At("TAIL"))
	private void dth$theDeadDriftDown(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player) ClientGhosts.drift(player);
	}
}
