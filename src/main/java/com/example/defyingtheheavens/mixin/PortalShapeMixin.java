package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.PortalRestrictionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Every Nether portal ignition path asks PortalShape for a frame; in the high realms there never is one. */
@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {
	@Inject(method = "findEmptyPortalShape", at = @At("HEAD"), cancellable = true)
	private static void dth$noPortalFramesInHighRealms(LevelAccessor level, BlockPos pos, Direction.Axis axis,
			CallbackInfoReturnable<Optional<PortalShape>> cir) {
		if (PortalRestrictionHandler.isRestricted(level)) {
			cir.setReturnValue(Optional.empty());
		}
	}
}
