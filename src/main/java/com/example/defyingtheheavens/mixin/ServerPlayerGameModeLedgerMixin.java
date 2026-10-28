package com.example.defyingtheheavens.mixin;

import com.example.defyingtheheavens.LandLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player's hands at work, for the land ledger (LandLedger): what changes while they break a block, use an item on one (placing, a
 * bucket, a hoe, a door) or use one in the air is their doing.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeLedgerMixin {
	@Inject(method = "destroyBlock", at = @At("HEAD"))
	private void dth$breakBegins(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		LandLedger.begin();
	}

	@Inject(method = "destroyBlock", at = @At("RETURN"))
	private void dth$breakEnds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		LandLedger.end();
	}

	@Inject(method = "useItemOn", at = @At("HEAD"))
	private void dth$useOnBegins(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		LandLedger.begin();
	}

	@Inject(method = "useItemOn", at = @At("RETURN"))
	private void dth$useOnEnds(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		LandLedger.end();
	}

	@Inject(method = "useItem", at = @At("HEAD"))
	private void dth$useBegins(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		LandLedger.begin();
	}

	@Inject(method = "useItem", at = @At("RETURN"))
	private void dth$useEnds(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		LandLedger.end();
	}
}
