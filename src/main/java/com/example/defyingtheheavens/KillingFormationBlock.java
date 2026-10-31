package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The eye of a killing formation in a secret realm (HerbValleyWorks lays and renews them): a mossy array stone with its script worn faint,
 * working on everyone who comes within its reach while the realm stands open ({@link Array}, KillingFormationBlockEntity), its guardians
 * aside. Faint to the eye (a shimmer now and then); plain under Qi Sense, its ring drawn on the land (the client's, KillingFormationRenderer).
 * Broken (a pickaxe, a blow of any kind), the formation is undone, and the realm sets it again before it next opens (RealmLedger).
 */
public class KillingFormationBlock extends BaseEntityBlock {
	/** What the formation does to those within it, and how far it reaches. */
	public enum Array implements StringRepresentable {
		/** Qi blades cut at everyone within it, a share of their body each time. */
		BLADES("blades", 6),
		/** Fire springs up under everyone within it. */
		FIRE("fire", 5),
		/** It weighs everyone down: slow, no jumping, no flying. */
		GRAVITY("gravity", 8),
		/** A fog that blinds, and turns those lost in it about. */
		FOG("fog", 10);

		private final String name;
		public final int reach;

		Array(String name, int reach) {
			this.name = name;
			this.reach = reach;
		}

		@Override
		public String getSerializedName() { return name; }
	}

	public static final EnumProperty<Array> ARRAY = EnumProperty.create("array", Array.class);

	public KillingFormationBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ARRAY, Array.BLADES));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ARRAY);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new KillingFormationBlockEntity(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.KILLING_FORMATION, KillingFormationBlockEntity::serverTick);
	}

	/** Broken: the formation is undone, those near hear it shatter. */
	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && !RealmLedger.isQuiet()) {
			server.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.4f, 0.6f);
			server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 40, 1.5, 0.4, 1.5, 0.08);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	/** Now and then, a shimmer over it: faint to the eye. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(6) != 0) return;
		int reach = state.getValue(ARRAY).reach;
		double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * reach;
		level.addParticle(ParticleTypes.WAX_OFF, pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 1.1, pos.getZ() + 0.5 + Math.sin(a) * r, 0, 0.01, 0);
	}
}
