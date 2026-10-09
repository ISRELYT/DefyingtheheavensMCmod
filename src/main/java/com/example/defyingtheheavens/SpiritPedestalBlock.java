package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A carved stone pedestal with jade inlays, taller than a block, that displays one Cultivation Fruit floating just above
 * its top. Right-click with a fruit to set it down, right-click again to take it back; breaking the pedestal gives back
 * both. A fruit on display
 * shows its age aura (as on the tree) and feeds anyone meditating nearby (see {@link CultivationBoost}).
 */
public class SpiritPedestalBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            box(1, 0, 1, 15, 3, 15),              // base
            box(3, 3, 3, 13, 18, 13),             // column, pilasters and capital
            box(2, 18, 2, 14, 20, 14)); // top; like a fence it stands taller than a block

    public SpiritPedestalBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpiritPedestalBlockEntity(pos, state); }

    /** Client only: the displayed fruit's aura particles and sounds. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.SPIRIT_PEDESTAL, FruitAura::pedestalClientTick) : null;
    }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SpiritPedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (!pedestal.hasFruit()) {
            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ModItems.CULTIVATION_FRUIT)) return InteractionResult.PASS; // lets the other hand try
            if (!level.isClientSide) {
                ItemStack one = held.copy();
                one.setCount(1);
                pedestal.setFruit(one);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 0.9f);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            ItemStack fruit = pedestal.getFruit();
            pedestal.setFruit(ItemStack.EMPTY);
            if (!player.getInventory().add(fruit)) player.drop(fruit, false);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.9f);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Breaking it gives back the pedestal and the fruit on it (survival breaking and explosions). */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(this));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SpiritPedestalBlockEntity pedestal && pedestal.hasFruit()) {
            drops.add(pedestal.getFruit().copy());
        }
        return drops;
    }

    /** Creative breaking drops nothing, so a pedestal holding a fruit pops both out instead (like a filled shulker box). */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof SpiritPedestalBlockEntity pedestal
                && pedestal.hasFruit()) {
            popResource(level, pos, new ItemStack(this));
            popResource(level, pos, pedestal.getFruit().copy());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(ModLang.PEDESTAL_TOOLTIP).withStyle(ChatFormatting.GRAY));
    }
}
