package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.Concealments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A concealment hides; it doesn't block (the user, 2026-10-28): a player it hides something from meets, in the server's reckoning of their
 * movement, the shape of what they are shown there (Concealments#illusoryShape), so they walk through hidden work and over the ground they
 * see, and nothing hidden from them touches them (Concealments#untouchedBy). Where no veil stands, one field's check.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseVeilMixin {
	@Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
			at = @At("HEAD"), cancellable = true)
	private void dth$asTheyAreShown(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
		if (!Concealments.any || !(context instanceof EntityCollisionContext entityContext) || !(entityContext.getEntity() instanceof ServerPlayer player)) return;
		VoxelShape shape = Concealments.illusoryShape(player, pos, (BlockState) (Object) this);
		if (shape != null) cir.setReturnValue(shape);
	}

	@Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
	private void dth$untouchedByTheHidden(Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (Concealments.any && Concealments.untouchedBy(entity, pos, (BlockState) (Object) this)) ci.cancel();
	}
}
