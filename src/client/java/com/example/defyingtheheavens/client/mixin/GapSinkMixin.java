package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SpatialTrialHandler;
import com.example.defyingtheheavens.client.ClientCultivationData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The local player flying the Spatial Gap by their own tear: in its lower layers the gap is thick, and they sink no faster than
 * {@link SpatialTrialHandler#LOWER_SINK}, falling or flying down alike (before vanilla's travel, so this tick's step is held to it).
 */
@Mixin(Player.class)
public abstract class GapSinkMixin {
	@Inject(method = "travel", at = @At("HEAD"))
	private void dth$theGapIsThickBelow(Vec3 input, CallbackInfo ci) {
		if (!((Object) this instanceof LocalPlayer player) || player.isPassenger() || player.isSpectator() || player.getAbilities().instabuild) return;
		if (!ModDimensions.isSpatialGap(player.level().dimension()) || !SpatialTrialHandler.inLowerLayers(player.getY())) return;
		if (!ClientCultivationData.get().isAbilityEnabled(Ability.SPATIAL_TEAR)) return; // a trial's current is its own
		Vec3 motion = player.getDeltaMovement();
		if (motion.y < -SpatialTrialHandler.LOWER_SINK) player.setDeltaMovement(motion.x, -SpatialTrialHandler.LOWER_SINK, motion.z);
	}
}
