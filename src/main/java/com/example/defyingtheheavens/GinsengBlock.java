package com.example.defyingtheheavens;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

/**
 * Ginseng growing wild on the ground (Ginseng, and the rarer Spirit Ginseng). It ages like a Cultivation Fruit on its
 * tree, shows the same age aura, and is dug up with a right-click into an item that keeps its age. Breaking it, or the
 * ground under it, drops it the same way. A bush drawn on crossed planes, like vanilla's sweet berry bush.
 */
public class GinsengBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);
    private final Supplier<Item> harvest;

    /** @param harvest the item this plant is dug up as */
    public GinsengBlock(Supplier<Item> harvest, Properties properties) {
        super(properties);
        this.harvest = harvest;
    }

    public ItemStack harvested(int years) {
        return GinsengItem.create(harvest.get(), years);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GinsengBlockEntity(pos, state); }

    /** Client only: the age aura's particles and sounds (see FruitAura). The server needs no ticking; age is a clock. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.GINSENG, FruitAura::herbClientTick) : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    /** It grows in soil: grass, dirt, podzol, moss and the like. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Right-click digs it up into your inventory; a full inventory leaves it in the ground. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng)) return InteractionResult.PASS;
        if (!player.getInventory().add(harvested(ginseng.age()))) {
            player.displayClientMessage(Component.translatable(ModLang.GINSENG_FULL), true);
            return InteractionResult.CONSUME;
        }
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.9f);
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(harvested(entity instanceof GinsengBlockEntity ginseng ? ginseng.age() : 1));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return harvested(level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng ? ginseng.age() : 1);
    }
}
