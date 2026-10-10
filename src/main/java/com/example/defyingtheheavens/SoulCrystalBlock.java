package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Inner Realm's floor: frosted, see-through crystal tiles (the leylines show beneath), tinted by the client to the
 * viewer's realm. A tile lights up under a soul's feet and fades again; the Leyline sequence lights tiles gold (CUE).
 * Unbreakable and never dropped: it exists only in the Inner Realm.
 */
public class SoulCrystalBlock extends HalfTransparentBlock {
	public static final int GLOW_NONE = 0, GLOW_STEP = 1, GLOW_CUE = 2;
	public static final IntegerProperty GLOW = IntegerProperty.create("glow", 0, 2);
	/** A stepped-on tile stays lit this long after the soul leaves it (ticks). */
	private static final int STEP_GLOW = 12;
	/** The tile each soul last stood on, so standing still doesn't chime over and over. */
	private static final Map<UUID, BlockPos> LAST_STEP = new HashMap<>();

	public SoulCrystalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(GLOW, GLOW_NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GLOW);
	}

	/** Light level: a faint inner glow, brighter underfoot, brightest as a cue. */
	public static int light(BlockState state) {
		return switch (state.getValue(GLOW)) {
			case GLOW_STEP -> 11;
			case GLOW_CUE -> 15;
			default -> 5;
		};
	}

	@Override
	public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
		super.stepOn(level, pos, state, entity);
		if (level.isClientSide || !(entity instanceof ServerPlayer player)) return;
		boolean fresh = !pos.equals(LAST_STEP.put(player.getUUID(), pos.immutable()));
		if (fresh) QiSurges.onStep(player, pos);
		BlockState now = level.getBlockState(pos);
		if (!now.is(this) || now.getValue(GLOW) == GLOW_CUE) return; // a cue keeps its gold
		if (now.getValue(GLOW) == GLOW_NONE) level.setBlock(pos, now.setValue(GLOW, GLOW_STEP), Block.UPDATE_CLIENTS);
		level.scheduleTick(pos, this, STEP_GLOW);
		if (fresh && level instanceof ServerLevel server) {
			server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.25f,
					0.8f + server.random.nextFloat() * 0.6f);
			server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 3, 0.25, 0.0, 0.25, 0.01);
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(GLOW) == GLOW_NONE) return;
		// Still under someone's feet: stay lit a little longer.
		if (state.getValue(GLOW) == GLOW_STEP && !level.getEntitiesOfClass(ServerPlayer.class,
				new net.minecraft.world.phys.AABB(pos.above()).inflate(0.2, 0.0, 0.2)).isEmpty()) {
			level.scheduleTick(pos, this, STEP_GLOW);
			return;
		}
		level.setBlock(pos, state.setValue(GLOW, GLOW_NONE), Block.UPDATE_CLIENTS);
	}

	/** Lights a tile (or puts it out with {@code glow} 0) for {@code ticks}. */
	public static void setGlow(ServerLevel level, BlockPos pos, int glow, int ticks) {
		BlockState state = level.getBlockState(pos);
		if (!state.is(ModBlocks.SOUL_CRYSTAL)) return;
		level.setBlock(pos, state.setValue(GLOW, glow), Block.UPDATE_CLIENTS);
		if (glow != GLOW_NONE && ticks > 0) level.scheduleTick(pos, state.getBlock(), ticks);
	}

	public static void forget(UUID player) {
		LAST_STEP.remove(player);
	}
}
