package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * As TreeFeatureMixin, for huge red and brown mushrooms (dark forests and mushroom fields scatter them like trees): none
 * generates in a sect's grounds. Mushrooms grown by players with bone meal are in the live world and still grow.
 */
@Mixin(AbstractHugeMushroomFeature.class)
public abstract class HugeMushroomFeatureMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notInASect(FeaturePlaceContext<HugeMushroomFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (!(context.level() instanceof WorldGenRegion)) return;
		BlockPos origin = context.origin();
		if (SectStructure.inSectGrounds(context.level(), origin.getX(), origin.getZ())) cir.setReturnValue(false);
	}
}
