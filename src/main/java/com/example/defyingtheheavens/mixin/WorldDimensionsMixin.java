package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.ModDimensions;
import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla 1.20.1 flags any world with a non-vanilla dimension as "experimental", which puts a "Worlds using Experimental
 * Settings are not supported" warning in front of every world creation and load. This marks the Upper Realm and Spatial
 * Gap as stable. Other mods' custom dimensions still go through vanilla's per-dimension check, so they keep the warning.
 */
@Mixin(WorldDimensions.class)
public abstract class WorldDimensionsMixin {
	@Inject(method = "checkStability", at = @At("HEAD"), cancellable = true)
	private static void dth$ourDimensionsAreStable(ResourceKey<LevelStem> key, LevelStem stem, CallbackInfoReturnable<Lifecycle> cir) {
		if (ModDimensions.UPPER_REALM_STEM.equals(key) || ModDimensions.SPATIAL_GAP_STEM.equals(key) || ModDimensions.INNER_REALM_STEM.equals(key)) {
			cir.setReturnValue(Lifecycle.stable());
		}
	}

	/**
	 * bake() also starts the dimension registry as experimental whenever there are more than the three vanilla
	 * dimensions. Start it stable instead; every entry still contributes its own (per-dimension) lifecycle on top.
	 */
	@Redirect(method = "bake", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Lifecycle;experimental()Lcom/mojang/serialization/Lifecycle;"))
	private Lifecycle dth$extraDimensionsAreJudgedIndividually() {
		return Lifecycle.stable();
	}
}
