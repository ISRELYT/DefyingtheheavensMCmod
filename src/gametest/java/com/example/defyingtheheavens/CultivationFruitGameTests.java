package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CultivationFruitGameTests implements FabricGameTest {
    private static final BlockPos FRUIT = new BlockPos(2, 2, 2);

    private CultivationFruitBlockEntity plant(GameTestHelper helper, int years) {
        helper.setBlock(FRUIT.above(), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        helper.setBlock(FRUIT, ModBlocks.CULTIVATION_FRUIT);
        CultivationFruitBlockEntity fruit = (CultivationFruitBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(FRUIT));
        fruit.setAge(years);
        return fruit;
    }

    private void harvest(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(FRUIT);
        helper.getLevel().getBlockState(pos).use(helper.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void harvestFreezesAgeAndCannotReplant(GameTestHelper helper) {
        plant(helper, 400);
        Player player = helper.makeMockPlayer();
        harvest(helper, player);
        helper.assertBlockPresent(Blocks.AIR, FRUIT);
        ItemStack stack = player.getInventory().getItem(0);
        helper.assertTrue(stack.is(ModItems.CULTIVATION_FRUIT) && stack.getCount() == 1, "Harvest gives exactly one fruit");
        helper.assertTrue(CultivationFruitItem.age(stack) == 400, "Harvest preserves attached age");
        helper.assertTrue(!(stack.getItem() instanceof BlockItem), "Fruit cannot be placed back on leaves");
        ItemStack reloaded = ItemStack.of(stack.save(new CompoundTag()));
        helper.assertTrue(CultivationFruitItem.age(reloaded) == 400, "Item age survives save/load");
        harvest(helper, player);
        helper.assertTrue(stack.getCount() == 1, "Second click must not duplicate harvested fruit");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fullInventoryLeavesFruitAttached(GameTestHelper helper) {
        plant(helper, 90);
        Player player = helper.makeMockPlayer();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) player.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 64));
        harvest(helper, player);
        helper.assertBlockPresent(ModBlocks.CULTIVATION_FRUIT, FRUIT);
        helper.assertTrue(((CultivationFruitBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(FRUIT))).age() == 90,
                "Failed harvest must preserve age");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void removingLeavesDropsAgedFruit(GameTestHelper helper) {
        plant(helper, 5_000);
        helper.setBlock(FRUIT.above(), Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, FRUIT);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FRUIT)).inflate(1));
        helper.assertTrue(drops.size() == 1 && CultivationFruitItem.age(drops.get(0).getItem()) == 5_000,
                "Removing support drops exactly one fruit with its age intact");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void agingPersistenceAndBounds(GameTestHelper helper) {
        helper.assertTrue(FruitAge.at(1, 50, 24_050) == 11, "Ten years per Minecraft day");
        helper.assertTrue(FruitAge.at(1, 50, 2_449) == 1, "Partial year is not counted early");
        helper.assertTrue(FruitAge.at(1, 50, 2_450) == 2, "A year advances at 2400 ticks");
        helper.assertTrue(FruitAge.at(9_999, 0, 24_000) == 10_000, "Ages cap at ten thousand");
        helper.assertTrue(FruitAge.at(100, 500, 0) == 100, "Time rollback cannot decrease age");
        helper.assertTrue(FruitAge.cultivation(10_000) > FruitAge.cultivation(1), "Older fruit grants more cultivation");
        CultivationFruitBlockEntity fruit = plant(helper, 700);
        CompoundTag saved = fruit.saveWithoutMetadata();
        CultivationFruitBlockEntity restored = new CultivationFruitBlockEntity(helper.absolutePos(FRUIT), ModBlocks.CULTIVATION_FRUIT.defaultBlockState());
        restored.load(saved);
        restored.setLevel(helper.getLevel());
        helper.assertTrue(restored.age() == 700 && restored.saveWithoutMetadata().getLong("PlantedAt") == saved.getLong("PlantedAt"),
                "Reload must preserve the age clock rather than restart it");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void eatingAwardsCultivationAndConsumesOne(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        PlayerCultivation cultivation = CultivationManager.get(player);
        cultivation.setState(Realm.QI_REFINING, Stage.EARLY, 0);
        ItemStack food = CultivationFruitItem.create(2);
        food.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, food);
        // Always edible even when hunger is full.
        helper.assertTrue(ModItems.CULTIVATION_FRUIT.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                .getResult().consumesAction(), "Fruit can be eaten at full hunger");
        ItemStack remaining = food.finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(remaining.getCount() == 1, "Eating consumes exactly one fruit");
        helper.assertTrue(cultivation.getCultivation() == 20, "Two-year fruit awards 20 cultivation");
        helper.assertTrue(CultivationFruitItem.age(remaining) == 2, "Remaining fruit keeps its frozen age");
        cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
        CultivationFruitItem.create(10_000).finishUsingItem(helper.getLevel(), player);
        helper.assertTrue(cultivation.getRealm() == Realm.QI_REFINING && cultivation.isAtBottleneck(),
                "Fruit cannot bypass tribulations");
        helper.getLevel().getServer().getPlayerList().remove(player);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void breakingFruitPreservesAge(GameTestHelper helper) {
        plant(helper, 10_000);
        helper.getLevel().destroyBlock(helper.absolutePos(FRUIT), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(FRUIT)).inflate(1));
        helper.assertTrue(drops.size() == 1 && CultivationFruitItem.age(drops.get(0).getItem()) == 10_000,
                "Breaking fruit also preserves the harvested age without duplicate drops");
        helper.succeed();
    }
}
