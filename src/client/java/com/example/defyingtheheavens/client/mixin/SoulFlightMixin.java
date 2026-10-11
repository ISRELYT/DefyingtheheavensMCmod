package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.ClientSoul;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The local player's nascent soul, out of its body, flies its own way ({@link ClientSoul#fly}) instead of walking or falling. */
@Mixin(Player.class)
public abstract class SoulFlightMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void dth$soulFlies(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && ClientSoul.isOut() && !player.isPassenger()) {
			ClientSoul.fly(player);
			ci.cancel();
		}
	}
}
