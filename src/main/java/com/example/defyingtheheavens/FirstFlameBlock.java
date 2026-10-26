package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The First Flame (phase 3c, FirstFlame): the eternal white-gold flame of the Chaos Frontier's spring, burning before heaven and earth were
 * parted. Nothing feeds it and nothing puts it out; it harms none who walk into it, gives the brightest light, can be taken by no one
 * (only the creative may place or break it), and its embers rise white and gold. What its light does is the spring's (FirstFlame), not the
 * block's: one set down elsewhere is only its look.
 */
public class FirstFlameBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 14, 14);

	public FirstFlameBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		double x = pos.getX() + 0.5, y = pos.getY() + 0.3, z = pos.getZ() + 0.5;
		if (random.nextInt(3) == 0) {
			level.addParticle(FirstFlame.GOLD, x + (random.nextDouble() - 0.5) * 0.7, y + random.nextDouble() * 0.9, z + (random.nextDouble() - 0.5) * 0.7,
					0, 0.05, 0);
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.END_ROD, x + (random.nextDouble() - 0.5) * 0.5, y + 0.6 + random.nextDouble() * 0.6,
					z + (random.nextDouble() - 0.5) * 0.5, (random.nextDouble() - 0.5) * 0.02, 0.06 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.02);
		}
		if (random.nextInt(40) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6f + random.nextFloat() * 0.3f, 0.6f + random.nextFloat() * 0.2f, false);
		}
	}
}
