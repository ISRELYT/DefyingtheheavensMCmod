package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.SectStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No ore blob where it would reach into one of the mod's buildings (SectStructure#cutsIntoBuildings). Ore is placed after
 * structures, and where it may take their stone it came up through their walls: with Terralith, which counts blackstone as the
 * Overworld's stone, vanilla's gravel, dirt and ores appeared in a demonic sect's blackstone walls (the user's, 2026-10-18), and
 * the Upper Realm's quartz and the Netherworld's yin jade take blackstone themselves. The blob's reach is vanilla's own
 * (OreFeature#place writes nothing outside it): ceil(size / 8) + i blocks sideways and 2 + i up and down, i = ceil((size / 8 + 1) / 2).
 */
@Mixin(OreFeature.class)
public abstract class OreFeatureMixin {
	@Inject(method = "place", at = @At("HEAD"), cancellable = true)
	private void dth$notIntoBuildings(FeaturePlaceContext<OreConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
		if (!(context.level() instanceof WorldGenRegion)) return;
		int size = context.config().size;
		int i = Mth.ceil((size / 16.0F * 2.0F + 1.0F) / 2.0F), side = Mth.ceil(size / 8.0F) + i;
		BlockPos o = context.origin();
		BoundingBox blob = new BoundingBox(o.getX() - side, o.getY() - 2 - i, o.getZ() - side, o.getX() + side, o.getY() + 2 + i, o.getZ() + side);
		if (SectStructure.cutsIntoBuildings(context.level(), blob)) cir.setReturnValue(false);
	}
}
