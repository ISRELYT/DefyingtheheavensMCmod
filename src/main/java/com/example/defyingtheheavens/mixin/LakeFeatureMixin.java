package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No lake (vanilla's lava lakes, the Upper Realm's spring basins) where it would cut into a sect's grounds. A lake reaches
 * up to a chunk past the one it starts in, so one decorated after its neighbour has had its sect built there would hollow
 * out the paving, flood it and line it with stone.
 */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notIntoASect(FeaturePlaceContext<LakeFeature.Configuration> context, CallbackInfoReturnable<Boolean> cir) {
		BlockPos origin = context.origin(); // the lake fills origin.x .. origin.x + 15, the same in z
		if (SectStructure.touchesSect(context.level(), origin.getX(), origin.getZ(), origin.getX() + 15, origin.getZ() + 15)) cir.setReturnValue(false);
	}
}
