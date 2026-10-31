package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The great mirror of the Jade Lake Mirage's pavilion (MirageLakeWorks): touched, it calls the toucher's reflection out of it to be bested
 * (MirrorReflection); to one who has bested theirs it is a door, walked through to the hall behind it (as a trial's barrier is:
 * TrialBarrierBlock#passes). Nothing breaks it.
 */
public class JadeMirrorBlock extends Block {
	public JadeMirrorBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entity && TrialBarrierBlock.passes(entity.getEntity())) return Shapes.empty();
		return Shapes.block();
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) MirageLakeWorks.mirrorTouched(server, pos, sp);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** One sheet of glass across its frame: no faces drawn between two of it. */
	@Override
	@SuppressWarnings("deprecation")
	public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
		return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
	}

	/** Now and then a glint crosses it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(12) != 0) return;
		level.addParticle(ParticleTypes.END_ROD, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.005, 0);
	}
}
