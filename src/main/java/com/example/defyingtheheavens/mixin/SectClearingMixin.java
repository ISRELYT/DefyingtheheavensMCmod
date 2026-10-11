package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectStructure;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.BambooFeature;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.VinesFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A sect keeps its grounds clear: worldgen grows no tree, bamboo, huge mushroom, mossy boulder or jungle vine on a sect's
 * grounds or within a few blocks of them (they are decorated after the sect is built, and would come up through the houses
 * and the ways, or hang vines down the halls' walls inside and out). The sect plants its own trees and bamboo (SectPiece).
 * Only worldgen is stopped: a sapling grown there later grows, and vines spread as they will.
 */
@Mixin({TreeFeature.class, AbstractHugeMushroomFeature.class, BambooFeature.class, BlockBlobFeature.class, VinesFeature.class})
public abstract class SectClearingMixin {
	/** How far beyond the grounds nothing is grown: a tree's crown reaches about this far from its trunk. */
	private static final int MARGIN = 4;

	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notOnSectGrounds(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> cir) {
		if (context.level() instanceof WorldGenRegion && SectStructure.onGrounds(context.level(), context.origin(), MARGIN)) cir.setReturnValue(false);
	}
}
