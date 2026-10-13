package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientSoulRiver;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The local player on the Soul River is carried by its current and steers across it ({@link ClientSoulRiver#carry}), never walks or falls. */
@Mixin(Player.class)
public abstract class SoulRiverMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void dth$carriedByTheRiver(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && ClientSoulRiver.isOn() && !player.isPassenger() && !player.isSpectator()) {
			ClientSoulRiver.carry(player);
			ci.cancel();
		}
	}
}
