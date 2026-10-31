package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One of the four eyes of the Ruins of a Fallen Holy Land's ancestral formation (FallenHolyLandWorks): a stone of white jade set with the
 * formation's light, on a dais on its shard. While its guard stands near it nothing touches it (no hand, blast or spell: the realm's
 * {@code holds}); its guard down, a hand breaks it. All four broken, the formation over the ancestral hall falls for this opening; the
 * realm sets them whole before it next opens (RealmLedger).
 */
public class FormationEyeBlock extends Block {
	public FormationEyeBlock(Properties properties) {
		super(properties);
	}

	/** Its guard near: no hand gets anywhere with it. */
	@Override
	@SuppressWarnings("deprecation")
	public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level instanceof Level l && FallenHolyLandWorks.eyeGuarded(l, pos)) return 0.0f;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		super.playerWillDestroy(level, pos, state, player);
		if (level instanceof ServerLevel server && !RealmLedger.isQuiet()) FallenHolyLandWorks.eyeBroken(server, pos, player);
	}

	/** The formation's light rising from it. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(2) != 0) return;
		level.addParticle(ParticleTypes.ENCHANT, pos.getX() + 0.5 + (random.nextDouble() - 0.5), pos.getY() + 1.2, pos.getZ() + 0.5 + (random.nextDouble() - 0.5),
				0, 0.3, 0);
		if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, 0.05, 0);
	}
}
