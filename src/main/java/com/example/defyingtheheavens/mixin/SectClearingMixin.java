package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.ImmortalCityStructure;
import com.example.defyingtheheavens.SectStructure;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.levelgen.feature.AbstractHugeMushroomFeature;
import net.minecraft.world.level.levelgen.feature.BambooFeature;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.VinesFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A sect keeps its grounds clear: worldgen grows no tree, bamboo, huge mushroom, mossy boulder or jungle vine on a sect's
 * grounds or within a few blocks of them (they are decorated after the sect is built, and would come up through the houses
 * and the ways, or hang vines down the halls' walls inside and out), nor opens a spring there. The sect plants its own trees and bamboo
 * (SectPiece). Only worldgen is stopped: a sapling grown there later grows, and vines spread as they will. An immortal city's grounds are
 * kept so too (ImmortalCityStructure), and an immortal sect's floating islands (SectPiece#onGrounds: tour 12, the jade peaks' bamboo stood
 * up out of them, the podzol round it browning their lawns, and springs ran out of their undersides in threads down into the clouds).
 * Nor is a lake dug there, of water or of lava (dug before the sect is built, it stood as water through the grounds, the ways laid over it
 * as causeways: the Void Steppes' and the Dao Field of Water's basins, the Dao Field of Fire's lava; the user, 2026-10-27, "stray water").
 */
@Mixin({TreeFeature.class, AbstractHugeMushroomFeature.class, BambooFeature.class, BlockBlobFeature.class, VinesFeature.class, SpringFeature.class,
		LakeFeature.class})
public abstract class SectClearingMixin {
	/** How far beyond the grounds nothing is grown: a tree's crown reaches about this far from its trunk. */
	private static final int MARGIN = 4;
	/** A lake's: it is dug up to eight blocks from where it is placed. */
	private static final int LAKE_MARGIN = MARGIN + 8;

	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notOnSectGrounds(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> cir) {
		int margin = (Object) this instanceof LakeFeature ? LAKE_MARGIN : MARGIN;
		if (context.level() instanceof WorldGenRegion && (SectStructure.onGrounds(context.level(), context.origin(), margin)
				|| ImmortalCityStructure.onGrounds(context.level(), context.origin(), margin))) cir.setReturnValue(false);
	}
}
