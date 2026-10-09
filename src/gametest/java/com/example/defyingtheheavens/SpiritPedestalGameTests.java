package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SpiritPedestalGameTests implements FabricGameTest {
    private static final BlockPos PEDESTAL = new BlockPos(2, 2, 2);

    private SpiritPedestalBlockEntity pedestal(GameTestHelper helper, BlockPos at) {
        helper.setBlock(at, ModBlocks.SPIRIT_PEDESTAL);
        return (SpiritPedestalBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
    }

    private void use(GameTestHelper helper, Player player, BlockPos at) {
        BlockPos pos = helper.absolutePos(at);
        helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void placeAndTakeBackFruit(GameTestHelper helper) {
        SpiritPedestalBlockEntity pedestal = pedestal(helper, PEDESTAL);
        Player player = helper.makeMockPlayer();
        ItemStack fruits = CultivationFruitItem.create(400);
        fruits.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, fruits);
        use(helper, player, PEDESTAL);
        helper.assertTrue(pedestal.hasFruit() && pedestal.fruitAge() == 400, "Using a fruit sets it on the pedestal with its age");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "Only one fruit is taken from the stack");
        use(helper, player, PEDESTAL);
        helper.assertTrue(!pedestal.hasFruit(), "Clicking a full pedestal takes the fruit back");
        helper.assertTrue(!pedestal.getUpdateTag().isEmpty(), "An emptied pedestal still sends an update, so clients stop drawing the fruit");
        int returned = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.CULTIVATION_FRUIT) && CultivationFruitItem.age(stack) == 400) returned += stack.getCount();
        }
        helper.assertTrue(returned == 2, "Both fruit end up back with the player, age intact");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void onlyFruitGoesOnPedestal(GameTestHelper helper) {
        SpiritPedestalBlockEntity pedestal = pedestal(helper, PEDESTAL);
        Player player = helper.makeMockPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        use(helper, player, PEDESTAL);
        helper.assertTrue(!pedestal.hasFruit() && player.getMainHandItem().is(Items.APPLE), "Other items are not placed");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void breakingDropsPedestalAndFruit(GameTestHelper helper) {
        pedestal(helper, PEDESTAL).setFruit(CultivationFruitItem.create(5_000));
        helper.getLevel().destroyBlock(helper.absolutePos(PEDESTAL), true);
        helper.assertBlockPresent(Blocks.AIR, PEDESTAL);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(PEDESTAL)).inflate(1.5));
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModBlocks.SPIRIT_PEDESTAL.asItem())),
                "Breaking a pedestal with a fruit on it still gives the pedestal back");
        helper.assertTrue(drops.stream().anyMatch(e -> e.getItem().is(ModItems.CULTIVATION_FRUIT) && CultivationFruitItem.age(e.getItem()) == 5_000),
                "Breaking the pedestal drops its fruit with the age intact");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void boostCountsMatAndStrongestFourPedestals(GameTestHelper helper) {
        BlockPos mat = new BlockPos(3, 2, 3);
        helper.setBlock(mat.below(), Blocks.STONE);
        helper.setBlock(mat, ModBlocks.RED_MEDITATION_MAT);
        BlockPos[] spots = {new BlockPos(1, 2, 3), new BlockPos(5, 2, 3), new BlockPos(3, 2, 1), new BlockPos(3, 2, 5), new BlockPos(1, 2, 1)};
        int[] ages = {1, 10, 100, 1_000, 10_000};
        for (int i = 0; i < spots.length; i++) pedestal(helper, spots[i]).setFruit(CultivationFruitItem.create(ages[i]));

        Player player = helper.makeMockPlayer();
        BlockPos seat = helper.absolutePos(mat);
        player.setPos(seat.getX() + 0.5, seat.getY() + 2.0 / 16, seat.getZ() + 0.5);
        CultivationBoost.Breakdown boost = CultivationBoost.of(player);

        helper.assertTrue(boost.mat() == ModBlocks.RED_MEDITATION_MAT && boost.matBonus() == CultivationBoost.RED_MAT, "Sitting on the red mat counts");
        helper.assertTrue(boost.pedestals().size() == CultivationBoost.MAX_PEDESTALS, "Only the strongest four pedestals count");
        helper.assertTrue(boost.pedestals().get(0).fruitAge() == 10_000, "Oldest fruit first");
        double expected = 0.14 + 0.26 + 0.38 + 0.50; // 10, 100, 1,000 and 10,000 years; the 1-year fruit is left out
        helper.assertTrue(Math.abs(boost.pedestalBonus() - expected) < 1.0e-9, "Pedestal shares grow with the fruit's age");
        helper.assertTrue(boost.multiplier() >= 1.0 + CultivationBoost.RED_MAT + expected - 1.0e-9, "Shares add up into the multiplier");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void pedestalShareRunsFromTwoToFiftyPercent(GameTestHelper helper) {
        helper.assertTrue(Math.abs(CultivationBoost.pedestalBonus(1) - 0.02) < 1.0e-9, "A 1-year fruit gives 2%");
        helper.assertTrue(Math.abs(CultivationBoost.pedestalBonus(10_000) - 0.50) < 1.0e-9, "A 10,000-year fruit gives 50%");
        helper.assertTrue(CultivationBoost.pedestalBonus(0) == 0, "An empty pedestal gives nothing");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void farPedestalsDoNotCount(GameTestHelper helper) {
        BlockPos far = new BlockPos(7, 2, 2);
        pedestal(helper, far).setFruit(CultivationFruitItem.create(10_000));
        Player player = helper.makeMockPlayer();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 2));
        player.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.assertTrue(CultivationBoost.of(player).pedestals().isEmpty(), "A pedestal six blocks away is out of reach");
        helper.succeed();
    }
}
