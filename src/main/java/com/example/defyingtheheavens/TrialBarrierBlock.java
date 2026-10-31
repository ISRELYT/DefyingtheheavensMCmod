package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

/**
 * A trial's barrier (HerbValleyWorks: the Pill Sage's hall's doorway): a sheet of golden formation light, solid to everyone but those who
 * have passed the trial before it (HerbValleyWorks#hasPassed); to them it is nothing. The client is told whether its own player has passed
 * ({@link #CLIENT}: SecretRealmClient), so it walks through as the server lets it. Nothing breaks it.
 */
public class TrialBarrierBlock extends Block {
	/** The client's: whether {@code entity} (its own player) has passed. Set by SecretRealmClient. */
	public static volatile Predicate<Entity> CLIENT = entity -> false;

	public TrialBarrierBlock(Properties properties) {
		super(properties);
	}

	/** Whether {@code entity} passes through. */
	public static boolean passes(Entity entity) {
		if (entity == null) return false;
		return entity.level().isClientSide ? CLIENT.test(entity) : RealmWorks.hasPassed(entity);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entity && passes(entity.getEntity())) return Shapes.empty();
		return Shapes.block();
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(3) != 0) return;
		level.addParticle(ParticleTypes.WAX_ON, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.02, 0);
	}
}
