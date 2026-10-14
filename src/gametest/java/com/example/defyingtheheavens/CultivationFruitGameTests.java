package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
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
        helper.assertTrue(cultivation.getMedicinalQi() == 20, "Two-year fruit gives 20 unrefined qi");
        helper.assertTrue(CultivationFruitItem.age(remaining) == 2, "Remaining fruit keeps its frozen age");
        cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
        CultivationFruitItem.create(10_000).finishUsingItem(helper.getLevel(), player);
        cultivation.addCultivation(cultivation.refine(1_000_000));
        cultivation.addCultivation(cultivation.refine(1_000_000));
        helper.assertTrue(cultivation.getRealm() == Realm.QI_REFINING && cultivation.isAtBottleneck() && cultivation.getMedicinalQi() > 0,
                "Fruit cannot bypass tribulations: the rest waits, unrefined");
        helper.getLevel().getServer().getPlayerList().remove(player);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void fruitThatWouldBeWastedIsNotEaten(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        try {
            player.getAbilities().instabuild = false;
            PlayerCultivation cultivation = CultivationManager.get(player);
            cultivation.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
            cultivation.addCultivation(cultivation.cultivationRequired()); // at the bottleneck
            ItemStack fruit = CultivationFruitItem.create(500);
            player.setItemInHand(InteractionHand.MAIN_HAND, fruit);
            helper.assertTrue(!ModItems.CULTIVATION_FRUIT.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "At a bottleneck the fruit isn't eaten");
            helper.assertTrue(fruit.finishUsingItem(helper.getLevel(), player) == fruit && fruit.getCount() == 1,
                    "Nor used up if the bottleneck came while it was being eaten");
            cultivation.setMortal(true);
            helper.assertTrue(!ModItems.CULTIVATION_FRUIT.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                    "A mortal can't eat it either");
            helper.succeed();
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
            player.discard();
        }
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

    @GameTest(template = EMPTY_STRUCTURE)
    public void wildFruitFavoursYoungAges(GameTestHelper helper) {
        RandomSource random = RandomSource.create(20261009L);
        int samples = 200_000;
        int young = 0, hundred = 0, thousand = 0, heavenly = 0, nearForty = 0;
        for (int i = 0; i < samples; i++) {
            int years = FruitAge.natural(random);
            helper.assertTrue(years >= 1 && years <= FruitAge.MAX_YEARS, "Wild age within bounds: " + years);
            if (years >= FruitAge.HEAVENLY_TIER) heavenly++;
            else if (years >= FruitAge.THOUSAND_YEAR_TIER) thousand++;
            else if (years >= FruitAge.HUNDRED_YEAR_TIER) hundred++;
            else young++;
            if (years >= 25 && years <= 55) nearForty++;
        }
        helper.assertTrue(young > hundred && hundred > thousand && thousand > heavenly && heavenly > 0,
                "Each older band is rarer: " + young + " / " + hundred + " / " + thousand + " / " + heavenly);
        helper.assertTrue(nearForty > samples / 2, "Most wild fruit is around forty years old");
        helper.assertTrue(heavenly < samples / 100, "Heavenly treasures stay very rare");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void auraTiersMatchAgeThresholds(GameTestHelper helper) {
        helper.assertTrue(FruitAura.of(99) == FruitAura.Tier.NONE, "No aura below a hundred years");
        helper.assertTrue(FruitAura.of(100) == FruitAura.Tier.HUNDRED_YEAR && FruitAura.of(499) == FruitAura.Tier.HUNDRED_YEAR,
                "Jade aura from a hundred years");
        helper.assertTrue(FruitAura.of(500) == FruitAura.Tier.FIVE_HUNDRED_YEAR && FruitAura.of(999) == FruitAura.Tier.FIVE_HUNDRED_YEAR,
                "Aqua aura from five hundred years");
        helper.assertTrue(FruitAura.of(1_000) == FruitAura.Tier.THOUSAND_YEAR && FruitAura.of(4_999) == FruitAura.Tier.THOUSAND_YEAR,
                "White aura from a thousand years");
        helper.assertTrue(FruitAura.of(5_000) == FruitAura.Tier.HEAVENLY && FruitAura.of(9_999) == FruitAura.Tier.HEAVENLY,
                "White-gold aura from five thousand years");
        helper.assertTrue(FruitAura.of(FruitAge.MAX_YEARS) == FruitAura.Tier.TEN_THOUSAND_YEAR, "Peak aura at ten thousand years");
        helper.assertTrue(FruitAura.heavenlyStrength(5_000) == 0.0f && FruitAura.heavenlyStrength(10_000) == 1.0f,
                "Heavenly strength runs from five to ten thousand years");
        helper.assertTrue(FruitAura.swell(0, FruitAura.BREATH_TICKS) < 0.01f
                && FruitAura.swell(FruitAura.BREATH_TICKS / 2.0f, FruitAura.BREATH_TICKS) > 0.99f, "Jade breath swells and settles");
        float lub = FruitAura.heartbeat(2), dub = FruitAura.heartbeat(FruitAura.SECOND_BEAT + 2), rest = FruitAura.heartbeat(FruitAura.HEARTBEAT_TICKS - 1);
        helper.assertTrue(lub > 0.99f && dub > 0.5f && dub < lub && rest < 0.05f, "Heartbeat: strong lub, softer dub, then stillness");
        helper.assertTrue(FruitAura.clock(1_000, new BlockPos(0, 64, 0)) != FruitAura.clock(1_000, new BlockPos(1, 64, 0))
                || FruitAura.clock(1_000, new BlockPos(0, 64, 0)) != FruitAura.clock(1_000, new BlockPos(0, 64, 1)),
                "Neighbouring fruit keep different time");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void worldgenFruitKeepsAgeAndClockOnLoad(GameTestHelper helper) {
        // Worldgen creates the block entity with no level and starts its clock from the world's time.
        long now = helper.getLevel().getGameTime();
        CultivationFruitBlockEntity generated = new CultivationFruitBlockEntity(helper.absolutePos(FRUIT), ModBlocks.CULTIVATION_FRUIT.defaultBlockState());
        generated.setWildAge(450, now);
        CompoundTag saved = generated.saveWithoutMetadata();
        CultivationFruitBlockEntity loaded = new CultivationFruitBlockEntity(helper.absolutePos(FRUIT), ModBlocks.CULTIVATION_FRUIT.defaultBlockState());
        loaded.load(saved);
        loaded.setLevel(helper.getLevel());
        helper.assertTrue(loaded.age() == 450, "Loaded wild fruit keeps its rolled age");
        helper.assertTrue(loaded.saveWithoutMetadata().getLong("PlantedAt") == now, "Loading keeps the worldgen clock");
        CultivationFruitBlockEntity unclocked = new CultivationFruitBlockEntity(helper.absolutePos(FRUIT), ModBlocks.CULTIVATION_FRUIT.defaultBlockState());
        unclocked.setLevel(helper.getLevel());
        helper.assertTrue(unclocked.saveWithoutMetadata().getLong("PlantedAt") == now, "Fruit without a clock starts one when it gets a level");
        CompoundTag sync = loaded.getUpdateTag();
        helper.assertTrue(sync.getInt("InitialYears") == 450 && sync.contains("PlantedAt"), "Clients receive the age for the aura");
        helper.succeed();
    }
}
