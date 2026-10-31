package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Seal's keystone (SecretRealms): the stone over its gate carved with its realm's seal, which knows the realm it holds shut
 * (SecretRealmSealBlockEntity). While the realm stirs or stands open a pillar of light rises from it into the sky (the client's,
 * SecretRealmSealRenderer: seen from afar, the way there). Right-clicked, it tells how the realm stands. Nothing breaks it.
 */
public class SecretRealmSealBlock extends BaseEntityBlock {
	public SecretRealmSealBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SecretRealmSealBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer sp) SecretRealms.tellAt(sp, pos);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}
}
