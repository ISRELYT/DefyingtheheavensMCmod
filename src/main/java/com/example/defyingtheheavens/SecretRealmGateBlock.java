package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A secret realm's gate (SecretRealms), filling the opening of the stone gate in the middle of its Seal: carved stone while the realm
 * sleeps ({@link Phase#SEALED}), cracks of light running through it as it stirs or while the realm stands open behind a gate shut again
 * ({@link Phase#STIRRING}), and while its gate stands open a sheet of light one walks into ({@link Phase#OPEN}: SecretRealms#atGate, who
 * may go in carried in, anyone else turned back). Right-clicked, the Seal tells how it stands (SecretRealms#tellAt). Nothing breaks it.
 */
public class SecretRealmGateBlock extends Block {
	public enum Phase implements StringRepresentable {
		SEALED("sealed"), STIRRING("stirring"), OPEN("open");

		private final String name;

		Phase(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() { return name; }
	}

	public static final EnumProperty<Phase> PHASE = EnumProperty.create("phase", Phase.class);

	public SecretRealmGateBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(PHASE, Phase.SEALED));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PHASE);
	}

	/** Its light: none sealed, a glow stirring, bright open. */
	public static int light(BlockState state) {
		return switch (state.getValue(PHASE)) {
			case SEALED -> 0;
			case STIRRING -> 7;
			case OPEN -> 13;
		};
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(PHASE) == Phase.OPEN ? Shapes.empty() : Shapes.block();
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (!level.isClientSide() && state.getValue(PHASE) == Phase.OPEN && entity instanceof ServerPlayer player) SecretRealms.atGate(player, pos);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
		if (!level.isClientSide() && player instanceof ServerPlayer sp) SecretRealms.tellAt(sp, pos);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		Phase phase = state.getValue(PHASE);
		if (phase == Phase.SEALED) return;
		if (phase == Phase.STIRRING && random.nextInt(4) != 0) return;
		double x = pos.getX() + random.nextDouble(), y = pos.getY() + random.nextDouble(), z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2;
		level.addParticle(phase == Phase.OPEN ? ParticleTypes.END_ROD : ParticleTypes.WAX_OFF, x, y, z, 0, 0.02, (random.nextDouble() - 0.5) * 0.04);
		if (phase == Phase.OPEN && random.nextInt(3) == 0) level.addParticle(ParticleTypes.ENCHANT, x, y + 0.5, z, 0, -0.4, 0);
	}
}
