package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ginseng growing on the ground (Ginseng, and the rarer Spirit Ginseng), wild or planted from its seeds. It ages like a
 * Cultivation Fruit on its tree and shows the same age aura. Planted, it is a seedling, then a young plant, and a full
 * bush with berries from its 10th year (the STAGE property, caught up by random ticks). Right-click digs it up into
 * roots that keep its age; a full-grown plant can give an extra root or two, and 1-3 seeds. Breaking it, or the ground
 * under it, drops the same. A cultivator with a Golden Core can sneak and right-click it with an empty hand to pour qi
 * into it, ageing it (see QiFeeding). Drawn on crossed planes, like vanilla's sweet berry bush.
 */
public class GinsengBlock extends BaseEntityBlock {
    /** 0 seedling, 1 young, 2 full grown. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    /** The age a plant becomes young, and full grown (when it bears seeds and can give extra roots). */
    public static final int YOUNG_YEARS = 4;
    public static final int MATURE_YEARS = 10;
    /** A full-grown plant gives a second root with this chance, and a third (instead) with the smaller one. */
    public static final float EXTRA_ROOT_CHANCE = 0.35f;
    public static final float THIRD_ROOT_CHANCE = 0.10f;
    public static final int MIN_SEEDS = 1, MAX_SEEDS = 3;

    private static final VoxelShape[] SHAPES = {box(3, 0, 3, 13, 8, 13), box(2, 0, 2, 14, 13, 14), box(2, 0, 2, 14, 13, 14)};
    private static final QiFeeding.Messages MESSAGES = new QiFeeding.Messages(ModLang.GINSENG_NEED_CORE, ModLang.GINSENG_NOT_ENOUGH, ModLang.GINSENG_MAX);
    private final Supplier<Item> harvest;
    private final Supplier<Item> seeds;

    /**
     * @param harvest the item this plant is dug up as
     * @param seeds   the seeds it gives once full grown
     */
    public GinsengBlock(Supplier<Item> harvest, Supplier<Item> seeds, Properties properties) {
        super(properties);
        this.harvest = harvest;
        this.seeds = seeds;
        // Full grown by default: wild plants, and ginseng drawn on a pedestal. Planted seeds start as seedlings.
        registerDefaultState(stateDefinition.any().setValue(STAGE, 2));
    }

    public static int stageFor(int years) {
        return years >= MATURE_YEARS ? 2 : years >= YOUNG_YEARS ? 1 : 0;
    }

    /** What digging up a plant of this age gives: its roots (all of that age), then any seeds. */
    public List<ItemStack> harvested(int years, RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        ItemStack roots = root(years);
        if (years >= MATURE_YEARS) {
            float roll = random.nextFloat();
            roots.setCount(roll < THIRD_ROOT_CHANCE ? 3 : roll < EXTRA_ROOT_CHANCE ? 2 : 1);
        }
        out.add(roots);
        if (years >= MATURE_YEARS) out.add(new ItemStack(seeds.get(), random.nextIntBetweenInclusive(MIN_SEEDS, MAX_SEEDS)));
        return out;
    }

    public ItemStack root(int years) {
        return GinsengItem.create(harvest.get(), years);
    }

    /** Sets the plant's look from its age (after it ages, is fed, or has its age set). */
    public static void updateStage(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GinsengBlock && level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng) {
            int stage = stageFor(ginseng.age());
            if (stage != state.getValue(STAGE)) level.setBlock(pos, state.setValue(STAGE, stage), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /** Planted from seed. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(STAGE, 0);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GinsengBlockEntity(pos, state); }

    /** Client only: the age aura's particles and sounds (see FruitAura). The server needs no ticking; age is a clock. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.GINSENG, FruitAura::herbClientTick) : null;
    }

    /** Growing plants check now and then whether their age has moved them to the next stage. */
    @Override public boolean isRandomlyTicking(BlockState state) { return state.getValue(STAGE) < 2; }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { updateStage(level, pos); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[state.getValue(STAGE)]; }

    /** It grows in soil: grass, dirt, podzol, moss and the like. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /**
     * Sneaking with an empty hand pours qi into it; otherwise right-click digs it up into your inventory (what doesn't fit
     * is dropped at your feet). With no room even for the root, it stays in the ground.
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng)) return InteractionResult.PASS;
        if (hand == InteractionHand.MAIN_HAND && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) feed(sp, (ServerLevel) level, pos, ginseng);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        List<ItemStack> harvest = harvested(ginseng.age(), level.getRandom());
        Inventory inventory = player.getInventory();
        if (inventory.getSlotWithRemainingSpace(harvest.get(0)) < 0 && inventory.getFreeSlot() < 0) {
            player.displayClientMessage(Component.translatable(ModLang.GINSENG_FULL), true);
            return InteractionResult.CONSUME;
        }
        for (ItemStack stack : harvest) {
            if (!inventory.add(stack) && !stack.isEmpty()) player.drop(stack, false);
        }
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0f, 0.9f);
        level.removeBlock(pos, false);
        return InteractionResult.CONSUME;
    }

    /** Pours as much of the player's qi into the plant as buys whole years. */
    private static void feed(ServerPlayer player, ServerLevel level, BlockPos pos, GinsengBlockEntity ginseng) {
        QiFeeding.Fed fed = QiFeeding.pour(player, ginseng.age(), MESSAGES);
        if (fed == null) return;
        ginseng.setAge(ginseng.age() + fed.years());
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.9f);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 10, 0.25, 0.3, 0.25, 0.02);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.0);
        player.displayClientMessage(Component.translatable(ModLang.GINSENG_FED, (int) Math.round(fed.qi()), fed.years(), ginseng.age()), true);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return harvested(entity instanceof GinsengBlockEntity ginseng ? ginseng.age() : 1, params.getLevel().getRandom());
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return root(level.getBlockEntity(pos) instanceof GinsengBlockEntity ginseng ? ginseng.age() : 1);
    }
}
