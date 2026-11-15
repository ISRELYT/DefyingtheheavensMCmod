package com.example.defyingtheheavens.client.mixin;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SpatialStorms;
import com.example.defyingtheheavens.SpatialTrialHandler;
import com.example.defyingtheheavens.client.ClientCultivationData;
import com.example.defyingtheheavens.client.ClientSpatialStorms;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The local player in the Spatial Gap (before vanilla's travel, so this tick's step is held to it): in its lower layers the gap is
 * thick, and they sink no faster than {@link SpatialTrialHandler#LOWER_SINK}, falling or flying down alike; and under or inside one of
 * its storms the winds carry them along with its drift ({@link SpatialStorms}, 2026-11-13: "storms that matter"; the server halves their flight
 * meanwhile, QiFlight#dragged).
 */
@Mixin(Player.class)
public abstract class GapSinkMixin {
	/**
	 * What the air takes off a mover each tick is 0.09 of its motion (vanilla's 0.91 kept): pushed this share of a drift every tick, one
	 * comes to go with it at its own pace, on top of whatever else one does.
	 */
	private static final double WIND_GRIP = 1 - 0.91;

	@Inject(method = "travel", at = @At("HEAD"))
	private void dth$theGapIsThickBelow(Vec3 input, CallbackInfo ci) {
		if (!((Object) this instanceof LocalPlayer player) || player.isPassenger() || player.isSpectator() || player.getAbilities().instabuild) return;
		if (!ModDimensions.isSpatialGap(player.level().dimension())) return;
		Vec3 wind = ClientSpatialStorms.windAt(player.getX(), player.getY(), player.getZ());
		if (wind != null) player.setDeltaMovement(player.getDeltaMovement().add(wind.x * WIND_GRIP, 0, wind.z * WIND_GRIP));
		if (!SpatialTrialHandler.inLowerLayers(player.getY())) return;
		if (!ClientCultivationData.get().isAbilityEnabled(Ability.SPATIAL_TEAR)) return; // a trial's current is its own
		Vec3 motion = player.getDeltaMovement();
		if (motion.y < -SpatialTrialHandler.LOWER_SINK) player.setDeltaMovement(motion.x, -SpatialTrialHandler.LOWER_SINK, motion.z);
	}
}
