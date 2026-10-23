package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.MeteorFlightClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The local player in Meteor Flight with forward held flies where they look ({@link MeteorFlightClient#travel}), not as vanilla flies;
 * and with the boost held, touching the ground doesn't end the flight ({@link MeteorFlightClient#keepAloft}, also at the end of
 * vanilla's flight, as LocalPlayer#aiStep lands one on the ground right after it).
 */
@Mixin(Player.class)
public abstract class MeteorFlightMixin {
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	private void dth$meteorFlight(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player && MeteorFlightClient.travel(player)) ci.cancel();
	}

	@Inject(method = "travel", at = @At("RETURN"))
	private void dth$keepAloft(Vec3 input, CallbackInfo ci) {
		if ((Object) this instanceof LocalPlayer player) MeteorFlightClient.keepAloft(player);
	}
}
