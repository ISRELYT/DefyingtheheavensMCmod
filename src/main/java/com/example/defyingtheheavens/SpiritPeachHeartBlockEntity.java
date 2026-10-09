package com.example.defyingtheheavens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The heart of a Spirit Peach Tree, at the base of its trunk. It keeps the tree's age (a clock like a fruit's), grows up
 * to {@link #MAX_FRUIT} Cultivation Fruit under the tree's leaves, and takes in qi poured into it (see
 * SpiritPeachHeartBlock). New fruit start at 1 year, and feeding qi ages the tree and its fruit together, so a tree's
 * fruit is never older than the tree.
 */
public class SpiritPeachHeartBlockEntity extends BlockEntity {
    /** Fruit a tree carries at once. */
    public static final int MAX_FRUIT = 3;
    /** How often the tree considers growing a fruit, and the chance each time (1 in this): about one fruit per 5 minutes. */
    private static final int FRUIT_CHECK_TICKS = 200;
    private static final int FRUIT_CHANCE = 30;
    /** The tree's canopy, relative to the heart: where it looks for its leaves and fruit. */
    private static final int REACH = 3, REACH_UP = 9;

    /**
     * Qi cost of one year of growth at age {@code a}: COST_BASE * (1 + a / COST_SCALE) ^ COST_POWER. Rises steeply with
     * age: about 20 qi a year for a young tree, 175 at 750 years, 800 at 5,000 and 1,300 at 9,000.
     */
    public static final double COST_BASE = 20, COST_SCALE = 50, COST_POWER = 0.8;

    private int initialYears = 1;
    private long plantedAt = -1;

    public SpiritPeachHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPIRIT_PEACH_HEART, pos, state);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        // Only set the field here, never setChanged(): see CultivationFruitBlockEntity (new worlds hung while loading).
        if (!level.isClientSide && plantedAt < 0) plantedAt = level.getGameTime();
    }

    public int age() {
        return FruitAge.at(initialYears, plantedAt, level == null ? plantedAt : level.getGameTime());
    }

    /** Ages the tree by {@code years} (up to the cap) and restarts its clock from now. */
    public void addYears(int years) {
        initialYears = FruitAge.clamp(age() + years);
        plantedAt = level == null ? -1 : level.getGameTime();
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    // ---- Qi feeding ----

    /** Qi it takes to grow a tree from 0 to {@code years} old (the cost per year, added up). */
    public static double qiToReach(double years) {
        return COST_BASE * COST_SCALE / (COST_POWER + 1) * (Math.pow(1 + years / COST_SCALE, COST_POWER + 1) - 1);
    }

    /** Qi to grow from {@code age} by {@code years}. */
    public static double qiCost(int age, int years) {
        return qiToReach(age + years) - qiToReach(age);
    }

    /** Whole years {@code qi} buys a tree of {@code age}, never past the cap. */
    public static int yearsFor(int age, double qi) {
        double target = qiToReach(age) + qi;
        double reached = COST_SCALE * (Math.pow(target * (COST_POWER + 1) / (COST_BASE * COST_SCALE) + 1, 1 / (COST_POWER + 1)) - 1);
        return (int) Math.max(0, Math.min(FruitAge.MAX_YEARS - age, Math.floor(reached - age + 1.0e-9)));
    }

    /** The fruit hanging from this tree's leaves. */
    public List<CultivationFruitBlockEntity> fruit() {
        List<CultivationFruitBlockEntity> found = new ArrayList<>();
        if (level == null) return found;
        for (BlockPos pos : canopy()) {
            if (level.getBlockEntity(pos) instanceof CultivationFruitBlockEntity fruit) found.add(fruit);
        }
        return found;
    }

    private Iterable<BlockPos> canopy() {
        return BlockPos.betweenClosed(worldPosition.offset(-REACH, 1, -REACH), worldPosition.offset(REACH, REACH_UP, REACH));
    }

    // ---- Bearing fruit ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpiritPeachHeartBlockEntity heart) {
        if ((level.getGameTime() + pos.asLong()) % FRUIT_CHECK_TICKS != 0) return;
        if (level.random.nextInt(FRUIT_CHANCE) != 0) return;
        List<BlockPos> spots = new ArrayList<>();
        int fruit = 0;
        for (BlockPos p : heart.canopy()) {
            BlockState here = level.getBlockState(p);
            if (here.is(ModBlocks.CULTIVATION_FRUIT)) fruit++;
            else if (here.is(ModBlocks.SPIRIT_PEACH_LEAVES) && level.isEmptyBlock(p.below())) spots.add(p.below().immutable());
        }
        if (fruit >= MAX_FRUIT || spots.isEmpty()) return;
        BlockPos at = spots.get(level.random.nextInt(spots.size()));
        if (level.setBlock(at, ModBlocks.CULTIVATION_FRUIT.defaultBlockState(), Block.UPDATE_ALL)
                && level.getBlockEntity(at) instanceof CultivationFruitBlockEntity grown) {
            grown.setAge(1); // a new fruit: 1 year old, ageing from now
        }
    }

    // ---- Saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("InitialYears", initialYears);
        tag.putLong("PlantedAt", plantedAt);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        initialYears = FruitAge.clamp(tag.getInt("InitialYears"));
        plantedAt = tag.contains("PlantedAt") ? tag.getLong("PlantedAt") : -1;
    }

    /** Sent with the chunk so the client (Jade) can show the tree's age. */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
