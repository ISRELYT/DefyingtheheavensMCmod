package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No tree generates in a sect's grounds (within SectStructure#TREELESS_RADIUS of its core): trees are decorated after the
 * sect is built, and one would come up through a roof or across a road. The sect plants its own. Only worldgen is stopped:
 * saplings grown by players go through the same feature, in the live world, and still grow.
 */
@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notInASect(FeaturePlaceContext<TreeConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (!(context.level() instanceof WorldGenRegion)) return;
		BlockPos origin = context.origin();
		if (SectStructure.inSectGrounds(context.level(), origin.getX(), origin.getZ())) cir.setReturnValue(false);
	}
}
