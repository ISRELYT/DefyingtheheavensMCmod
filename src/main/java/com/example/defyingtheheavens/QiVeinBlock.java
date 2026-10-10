package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Qi Vein: stone threaded with condensed qi, as rare as diamond and found in veins of at most four (see QiVeinFeature).
 * It takes a diamond pickaxe and drops itself, so it can be carried off and set into a formation's array: every vein
 * connected to a Formation Core by seals feeds it {@link #QI_PER_SECOND} (see FormationCoreBlockEntity). Clothing upgrades
 * take it too.
 * <p>
 * It gathers qi from the land around it on its own, which only Qi Sense shows: motes drift into its exposed faces
 * ({@link #clientEffects}, set by the client).
 */
public class QiVeinBlock extends Block {
	/** What one vein block feeds the formation it is connected to, per second. */
	public static final double QI_PER_SECOND = 8.0;

	/** Client hook (QiSenseClientHandler): draws the vein's gathering under Qi Sense. Does nothing on a dedicated server. */
	public static ClientEffects clientEffects = (level, pos, random) -> {};

	@FunctionalInterface
	public interface ClientEffects {
		void animate(Level level, BlockPos pos, RandomSource random);
	}

	public QiVeinBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		clientEffects.animate(level, pos, random);
	}
}
