package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A round meditation mat (a woven straw pu tuan) that lies on the ground like a carpet, drawn one and a half blocks
 * across so it overhangs its block by a quarter block on every side. Right-click it to sit in its
 * centre and start meditating, exactly as the meditation key does (same checks, messages and cultivation gain);
 * right-click again, or move, to stop. Models: Models/MeditationMat and Models/RedMeditationMat.
 */
public class MeditationMatBlock extends Block {
    private final double height;
    private final VoxelShape shape;

    /** @param height top of the mat in pixels (1/16 block) */
    public MeditationMatBlock(double height, Properties properties) {
        super(properties);
        this.height = height;
        // The model is a mat one and a half blocks across, overhanging its block; the block itself is the flat square
        // under its middle (what you click, stand on and break).
        this.shape = box(0, 0, 0, 16, height, 16);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    /** Like a carpet: it needs something beneath it. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (MeditationManager.isMeditating(serverPlayer.getUUID())) {
            MeditationManager.stop(serverPlayer, false);
            return InteractionResult.CONSUME;
        }
        // Can't meditate right now (tribulation, mid-jump, in water...): start() explains why, and nobody gets moved.
        if (!MeditationManager.canStart(serverPlayer)) {
            MeditationManager.start(serverPlayer);
            return InteractionResult.CONSUME;
        }
        // Settle onto the middle of the mat, keeping the way the player faces, then meditate as usual.
        serverPlayer.connection.teleport(pos.getX() + 0.5, pos.getY() + height / 16.0, pos.getZ() + 0.5,
                serverPlayer.getYRot(), serverPlayer.getXRot());
        MeditationManager.start(serverPlayer);
        return InteractionResult.CONSUME;
    }
}
