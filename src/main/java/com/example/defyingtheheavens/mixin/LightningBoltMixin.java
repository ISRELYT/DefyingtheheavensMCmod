package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.TribulationLightning;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A tribulation's dim bolt (TribulationLightning#isDim: one of strikes in quick succession, a great tribulation's many or a lifespan
 * tribulation's near its full power) is drawn, and thunders, without vanilla's flash of the whole sky: a sky lit several times a
 * second would strobe. The sky flashes at most once a second for a storm (TribulationManager#FLASH_EVERY).
 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setSkyFlashTime(I)V"))
	private void dth$noStrobe(Level level, int time) {
		if ((Object) this instanceof TribulationLightning bolt && bolt.isDim()) return;
		level.setSkyFlashTime(time);
	}
}
