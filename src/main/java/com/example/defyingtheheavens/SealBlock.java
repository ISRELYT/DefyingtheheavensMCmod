package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A seal of qi ink painted on the ground or a wall with the Inscription Brush: the conduit a formation draws on. Seals touching
 * each other (sideways, or up and down a step, like redstone dust) form one line, and a line from a Formation Core to Qi Veins
 * carries their qi into it (see FormationCoreBlockEntity#scanNetwork). {@link #FACE} is the side its supporting block is on.
 * <p>
 * Ink of qi can't be seen with mortal eyes: the block draws nothing and can't even be picked out with the cursor unless the
 * player has Qi Sense on, when the client draws the seals as glowing azure lines on the Qi Sense layer. It doesn't block
 * anything, and water washes it away.
 */
public class SealBlock extends BaseEntityBlock {
	/** The side of the seal its supporting block is on: DOWN for the floor, a horizontal side for a wall. */
	public static final DirectionProperty FACE = DirectionProperty.create("face", Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST);

	private static final VoxelShape FLOOR = box(0, 0, 0, 16, 1, 16);
	private static final VoxelShape NORTH = box(0, 0, 0, 16, 16, 1);
	private static final VoxelShape SOUTH = box(0, 0, 15, 16, 16, 16);
	private static final VoxelShape WEST = box(0, 0, 0, 1, 16, 16);
	private static final VoxelShape EAST = box(15, 0, 0, 16, 16, 16);

	public SealBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACE, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACE);
	}

	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SealBlockEntity(pos, state); }
	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

	public static VoxelShape plate(Direction face) {
		return switch (face) {
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> FLOOR;
		};
	}

	/** Only a player seeing with Qi Sense can pick a seal out (to break it); to everyone else there's nothing there. */
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player player
				&& CultivationManager.forPlayer(player).isAbilityActive(Ability.QI_SENSE)) {
			return plate(state.getValue(FACE));
		}
		return Shapes.empty();
	}

	/**
	 * Nothing to walk into, except where the seal lies on a raised barrier's shell (an array line crossing it): the barrier
	 * leaves seals in place so the line still carries qi, and the seal's cell closes in its stead to anyone the barrier
	 * wouldn't let through.
	 */
	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null) {
			Formation formation = Formations.at(entityContext.getEntity().level(), pos);
			if (formation != null && !formation.allows(entityContext.getEntity())) return Shapes.block();
		}
		return Shapes.empty();
	}

	/** Painted on something solid: the floor's top, or a wall's face. */
	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction face = state.getValue(FACE);
		BlockPos support = pos.relative(face);
		return level.getBlockState(support).isFaceSturdy(level, support, face.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return direction == state.getValue(FACE) && !canSurvive(state, level, pos)
				? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	/** Seals join seals within one block in every direction (diagonals too), so a line can climb a step or a wall. */
	public static boolean touches(BlockPos a, BlockPos b) {
		return Math.abs(a.getX() - b.getX()) <= 1 && Math.abs(a.getY() - b.getY()) <= 1 && Math.abs(a.getZ() - b.getZ()) <= 1 && !a.equals(b);
	}
}
