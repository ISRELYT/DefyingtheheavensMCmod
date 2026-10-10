package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Spirit Dew Grass: spirit-touched grass that gathers a bead of Spirit Dew on its blades now and then (much more often in
 * the Upper Realm's dense qi). Right-click a dewy tuft to collect the dew; the grass stays and gathers more. Shears take
 * the grass itself. Grows in soil, like any grass.
 */
public class SpiritDewGrassBlock extends BushBlock {
    public static final BooleanProperty DEW = BooleanProperty.create("dew");
    /** Outside the Upper Realm, a random tick forms dew with this chance (about every three minutes on average). */
    public static final float DEW_CHANCE = 1 / 3f;
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 13, 14);

    public SpiritDewGrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DEW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEW);
    }

    @Override public boolean isRandomlyTicking(BlockState state) { return !state.getValue(DEW); }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ModDimensions.isUpperRealm(level.dimension()) || random.nextFloat() < DEW_CHANCE) {
            level.setBlock(pos, state.setValue(DEW, true), Block.UPDATE_ALL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(DEW)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack dew = new ItemStack(ModItems.SPIRIT_DEW);
        if (!player.getInventory().add(dew)) player.drop(dew, false);
        level.setBlock(pos, state.setValue(DEW, false), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.6f);
        ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.0);
        return InteractionResult.CONSUME;
    }

    /** Client only: a dewy tuft glints now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(DEW) && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.5 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.005, 0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Shears keep the grass; dew on it always drops. */
    @Override
    @SuppressWarnings("deprecation")
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>();
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && tool.is(Items.SHEARS)) drops.add(new ItemStack(this));
        if (state.getValue(DEW)) drops.add(new ItemStack(ModItems.SPIRIT_DEW));
        return drops;
    }
}
