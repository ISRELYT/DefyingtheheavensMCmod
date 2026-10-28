package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Sea-Fixing Needle standing as a pillar over the Immortal Realm's sea of clouds (Legends, LegendSites#pillar): black iron, banded with
 * gold at its ends. A hand laid on it pulls it free, if its puller is strong enough (Legends#pull). Unbreakable, no item.
 */
public class SeaPillarBlock extends Block {
	/** A band of gold about it (its ends), or its black iron. */
	public static final BooleanProperty BANDED = BooleanProperty.create("banded");

	public SeaPillarBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(BANDED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BANDED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (level instanceof ServerLevel server) Legends.pull(server, player, pos);
		return InteractionResult.CONSUME;
	}
}
