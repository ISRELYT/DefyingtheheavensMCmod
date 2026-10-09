package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CultivationFruitBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(4, 6.5, 4, 12, 16, 12); // the peach (Models/CultivationPeach) and its stem
    public CultivationFruitBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CultivationFruitBlockEntity(pos, state); }

    /** Client only: the age aura's particles and sounds (see FruitAura). The server needs no ticking; age is a clock. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.CULTIVATION_FRUIT, FruitAura::clientTick) : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return level.getBlockState(pos.above()).is(BlockTags.LEAVES); }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CultivationFruitBlockEntity fruit)) return InteractionResult.PASS;
        ItemStack harvested = CultivationFruitItem.create(fruit.age());
        if (!player.getInventory().add(harvested)) {
            player.displayClientMessage(Component.translatable("item.defying-the-heavens.cultivation_fruit.full"), true);
            return InteractionResult.CONSUME;
        }
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(CultivationFruitItem.create(entity instanceof CultivationFruitBlockEntity fruit ? fruit.age() : 1));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return CultivationFruitItem.create(level.getBlockEntity(pos) instanceof CultivationFruitBlockEntity fruit ? fruit.age() : 1);
    }
}
