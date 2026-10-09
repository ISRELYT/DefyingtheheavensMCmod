package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.client.TribulationAtmosphere;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void dth$tribulationSky(Vec3 position, float partialTick, CallbackInfoReturnable<Vec3> cir) {
		cir.setReturnValue(TribulationAtmosphere.tint(cir.getReturnValue(), partialTick));
	}

	@Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
	private void dth$tribulationClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
		cir.setReturnValue(TribulationAtmosphere.tint(cir.getReturnValue(), partialTick));
	}
}
