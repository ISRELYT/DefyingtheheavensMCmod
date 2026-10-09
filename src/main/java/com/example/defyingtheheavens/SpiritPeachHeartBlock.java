package com.example.defyingtheheavens;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The base of a Spirit Peach Tree's trunk: looks like cherry wood, holds the tree's age and grows its fruit (see
 * SpiritPeachHeartBlockEntity). A cultivator with a Golden Core (Core Formation or higher) can sneak and right-click it
 * with an empty hand to pour their qi into the tree, ageing it and all its fruit. Breaking it gives an ordinary cherry log.
 */
public class SpiritPeachHeartBlock extends BaseEntityBlock {
    public SpiritPeachHeartBlock(Properties properties) { super(properties); }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpiritPeachHeartBlockEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SPIRIT_PEACH_HEART, SpiritPeachHeartBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof SpiritPeachHeartBlockEntity heart) feed(sp, (ServerLevel) level, pos, heart);
        return InteractionResult.CONSUME;
    }

    private static final QiFeeding.Messages MESSAGES = new QiFeeding.Messages(ModLang.PEACH_NEED_CORE, ModLang.PEACH_NOT_ENOUGH, ModLang.PEACH_MAX);

    /** Pours as much of the player's qi into the tree as buys whole years. */
    private static void feed(ServerPlayer player, ServerLevel level, BlockPos pos, SpiritPeachHeartBlockEntity heart) {
        QiFeeding.Fed fed = QiFeeding.pour(player, heart.age(), MESSAGES);
        if (fed == null) return;
        int years = fed.years();
        heart.addYears(years);
        for (CultivationFruitBlockEntity fruit : heart.fruit()) fruit.setAge(fruit.age() + years);

        // Qi flows up the trunk into the canopy.
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2f, 0.7f);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5f, 1.6f);
        for (int i = 0; i < 5; i++) {
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5 + i * 0.8, pos.getZ() + 0.5, 4, 0.3, 0.2, 0.3, 0.02);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 4.5, pos.getZ() + 0.5, 30, 1.6, 1.0, 1.6, 0.0);
        player.displayClientMessage(Component.translatable(ModLang.PEACH_FED, (int) Math.round(fed.qi()), years, heart.age()), true);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.CHERRY_LOG));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CHERRY_LOG);
    }
}
