package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class AlchemyGameTests implements FabricGameTest {
    private static final BlockPos CAULDRON = new BlockPos(2, 2, 2);

    /** The test server's mock players start in creative, where nothing gets used up. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Eats {@code stack} as the player would (the use animation skipped). */
    private static ItemStack eat(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack.finishUsingItem(player.level(), player);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void mortalsCannotCultivateUntilTheElixir(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        PlayerCultivation c = CultivationManager.get(player);
        helper.assertTrue(c.isMortal(), "New players start mortal");
        helper.assertTrue(!c.addCultivation(10_000) && c.getCultivation() == 0, "A mortal gains no cultivation");
        helper.assertTrue(c.maxQi() == 0 && c.qiGatherPerSecond() == 0, "A mortal has no qi");
        for (Ability ability : Ability.values()) helper.assertTrue(!ability.isUnlocked(c), "A mortal has no abilities");
        MeditationManager.start(player);
        helper.assertTrue(!MeditationManager.isMeditating(player.getUUID()), "A mortal can't meditate");

        ItemStack pill = PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.LOW, 1);
        eat(player, pill);
        helper.assertTrue(pill.getCount() == 1, "A mortal can't take a Cultivation Pill: it isn't eaten");

        ItemStack elixir = PillItem.create(ModItems.MARROW_CLEANSING_ELIXIR, PillGrade.IMMORTAL, FruitAge.MAX_YEARS);
        ItemStack left = eat(player, elixir);
        helper.assertTrue(!c.isMortal() && c.getRealm() == Realm.QI_REFINING && c.getStage() == Stage.EARLY,
                "The elixir makes a mortal a Qi Refining cultivator");
        helper.assertTrue(Math.abs(c.getCultivation() - c.cultivationRequired() * PillItem.MAX_HEAD_START * 0.9) < 1e-6,
                "An Immortal-grade, 10,000-year elixir starts 45% into Qi Refining Early");
        helper.assertTrue(left.is(Items.GLASS_BOTTLE), "The empty bottle is left");
        helper.assertTrue(c.maxQi() > 0 && Ability.CONSCIOUSNESS_DOMAIN.isUnlocked(c), "A cultivator has qi and abilities");

        ItemStack second = PillItem.create(ModItems.MARROW_CLEANSING_ELIXIR, PillGrade.LOW, 1);
        eat(player, second);
        helper.assertTrue(second.getCount() == 1, "A second elixir isn't drunk");

        PlayerCultivation cultivator = new PlayerCultivation();
        cultivator.setState(Realm.CORE_FORMATION, Stage.MID, 5);
        PlayerCultivation old = PlayerCultivation.load(cultivator.save());
        helper.assertTrue(!old.isMortal(), "A cultivator saved and loaded stays a cultivator");
        net.minecraft.nbt.CompoundTag legacy = old.save();
        legacy.remove("Mortal");
        helper.assertTrue(!PlayerCultivation.load(legacy).isMortal(), "Saves from before mortals existed load as cultivators");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void foundationBuildingNeedsAFoundationPill(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        PlayerCultivation c = CultivationManager.get(player);
        c.setState(Realm.QI_REFINING, Stage.LATE, 0);

        ItemStack early = PillItem.create(ModItems.FOUNDATION_PILL, PillGrade.LOW, 1);
        eat(player, early);
        helper.assertTrue(early.getCount() == 1 && c.getPreparedRealm() == null, "Too early: the pill isn't eaten");

        c.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
        c.addCultivation(c.cultivationRequired());
        helper.assertTrue(c.isAtBottleneck() && c.isMissingBreakthroughPill() && !c.canBreakthrough(),
                "At the bottleneck, the breakthrough waits for a Foundation Pill");

        ItemStack core = PillItem.create(ModItems.CORE_PILL, PillGrade.LOW, 1);
        eat(player, core);
        helper.assertTrue(core.getCount() == 1, "A Core Pill does nothing at Qi Refining");

        ItemStack pill = PillItem.create(ModItems.FOUNDATION_PILL, PillGrade.SUPREME, 1000);
        eat(player, pill);
        helper.assertTrue(pill.isEmpty() && c.getPreparedRealm() == Realm.FOUNDATION_BUILDING && c.canBreakthrough(),
                "The Foundation Pill opens the way");
        double headStart = Math.min(PillItem.MAX_HEAD_START, PillItem.HEAD_START_PER_POTENCY * (PillGrade.SUPREME.potency(1000) - 1));
        helper.assertTrue(c.breakthrough(), "The breakthrough goes ahead");
        helper.assertTrue(c.getRealm() == Realm.FOUNDATION_BUILDING && Math.abs(c.getCultivation() - c.cultivationRequired() * headStart) < 1e-6,
                "Foundation Building starts with the pill's head start");
        helper.assertTrue(c.getPreparedRealm() == null, "The pill is used up");

        c.setState(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION, 0);
        c.addCultivation(c.cultivationRequired());
        helper.assertTrue(c.isMissingBreakthroughPill(), "Core Formation needs a Core Pill");
        c.prepareBreakthrough(Realm.CORE_FORMATION, 0);
        c.fallOneStage();
        helper.assertTrue(c.getPreparedRealm() == null, "A failed tribulation spends the pill");

        c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
        c.addCultivation(c.cultivationRequired());
        helper.assertTrue(!c.isMissingBreakthroughPill() && c.canBreakthrough(), "Nascent Soul needs no pill");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void qiAndCultivationPillsScaleWithGradeAndAge(GameTestHelper helper) {
        helper.assertTrue(Math.abs(PillGrade.LOW.potency(1) - 1) < 1e-9, "Low grade, fresh ingredients: potency 1");
        helper.assertTrue(Math.abs(PillGrade.IMMORTAL.potency(FruitAge.MAX_YEARS) - 10) < 1e-9, "Immortal grade, 10,000 years: potency 10");
        helper.assertTrue(PillGrade.MID.potency(100) > PillGrade.MID.potency(10) && PillGrade.HIGH.potency(10) > PillGrade.MID.potency(10),
                "Both grade and age raise a pill's strength");
        for (int age : new int[] {1, 10, 100, 500, 1000, 5000, 10_000}) {
            int sum = 0;
            for (int odds : PillGrade.odds(age)) sum += odds;
            helper.assertTrue(sum == 100, "Grade odds add up to 100% at " + age + " years");
        }
        helper.assertTrue(PillGrade.odds(999)[PillGrade.IMMORTAL.ordinal()] == 0, "No Immortal grade below 1,000 years");

        ServerPlayer player = survivalPlayer(helper);
        PlayerCultivation c = CultivationManager.get(player);
        c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
        double base = c.qiGatherPerSecond();
        eat(player, PillItem.create(ModItems.QI_GATHERING_PILL, PillGrade.HIGH, 100));
        double boost = PillItem.QI_BOOST_PER_POTENCY * PillGrade.HIGH.potency(100);
        helper.assertTrue(Math.abs(c.qiGatherPerSecond() - base * (1 + boost)) < 1e-9, "The Qi Gathering Pill raises qi gathering by its potency");
        helper.assertTrue(player.hasEffect(ModEffects.QI_GATHERING), "Its effect shows the timer");
        player.removeEffect(ModEffects.QI_GATHERING);
        QiManager.tick(helper.getLevel().getServer());
        helper.assertTrue(c.getQiBoost() == 0, "Removing the effect (milk) ends the boost");

        double required = c.cultivationRequired();
        eat(player, PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.LOW, 1));
        double first = c.getCultivation();
        helper.assertTrue(Math.abs(first - required * PillItem.CULTIVATION_PER_POTENCY) < 1e-6, "A Low, fresh Cultivation Pill gives 15% of a stage");
        eat(player, PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.LOW, 1));
        helper.assertTrue(Math.abs((c.getCultivation() - first) - first * (1 - PlayerCultivation.PILL_RESISTANCE_PER_PILL)) < 1e-6,
                "Pill resistance: the second pill does 25% less");
        helper.assertTrue(c.decayPillResistance(600) && c.getPillResistance() < PlayerCultivation.PILL_RESISTANCE_PER_PILL * 2,
                "Pill resistance wears off over time");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void cauldronBrewsAGradedPill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(CAULDRON.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(CAULDRON, ModBlocks.ALCHEMY_CAULDRON);
        BlockPos pos = helper.absolutePos(CAULDRON);
        AlchemyCauldronBlockEntity cauldron = (AlchemyCauldronBlockEntity) level.getBlockEntity(pos);
        ServerPlayer player = survivalPlayer(helper);

        player.setItemInHand(InteractionHand.MAIN_HAND, GinsengItem.create(ModItems.GINSENG, 500));
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!player.getMainHandItem().isEmpty(), "Nothing goes in before the water");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getBlockState(pos).getValue(AlchemyCauldronBlock.FILLED) && player.getMainHandItem().is(Items.BUCKET),
                "A water bucket fills it");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "An ingredient no recipe uses is refused");

        for (ItemStack ingredient : List.of(GinsengItem.create(ModItems.GINSENG, 500), new ItemStack(Items.HONEY_BOTTLE),
                new ItemStack(Items.BONE_MEAL, 5))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ingredient);
            cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(player.getMainHandItem().getCount() == 4, "Ingredients go in one at a time");
        helper.assertTrue(!level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "Not a recipe yet");
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "The last bone meal completes the elixir");
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "Nothing more goes in while it brews");

        int ticks = AlchemyRecipes.forOutput(ModItems.MARROW_CLEANSING_ELIXIR).brewTicks();
        for (int i = 0; i < ticks + 5; i++) {
            BlockState state = level.getBlockState(pos);
            AlchemyCauldronBlockEntity.serverTick(level, pos, state, cauldron);
        }
        BlockState after = level.getBlockState(pos);
        helper.assertTrue(!after.getValue(AlchemyCauldronBlock.FILLED) && !after.getValue(AlchemyCauldronBlock.BREWING),
                "Brewing uses up the water");
        List<ItemEntity> out = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                e -> e.getItem().is(ModItems.MARROW_CLEANSING_ELIXIR));
        helper.assertTrue(out.size() == 1, "The elixir pops out");
        helper.assertTrue(PillItem.age(out.get(0).getItem()) == 500, "It carries the ginseng's age");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void brewingWaitsForTheFire(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(CAULDRON, ModBlocks.ALCHEMY_CAULDRON);
        BlockPos pos = helper.absolutePos(CAULDRON);
        AlchemyCauldronBlockEntity cauldron = (AlchemyCauldronBlockEntity) level.getBlockEntity(pos);
        ServerPlayer player = survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        for (ItemStack ingredient : List.of(GinsengItem.create(ModItems.GINSENG, 1), new ItemStack(ModItems.SPIRIT_DEW), new ItemStack(Items.GLOWSTONE_DUST))) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ingredient);
            cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        }
        for (int i = 0; i < 2000; i++) AlchemyCauldronBlockEntity.serverTick(level, pos, level.getBlockState(pos), cauldron);
        helper.assertTrue(level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "Without a fire beneath, the brew waits");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "A brew can't be tipped out");

        helper.setBlock(CAULDRON.below(), Blocks.MAGMA_BLOCK);
        for (int i = 0; i < 2000; i++) AlchemyCauldronBlockEntity.serverTick(level, pos, level.getBlockState(pos), cauldron);
        helper.assertTrue(!level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "With heat, it finishes");
        helper.succeed();
    }

    /** Mortals look mortal to Realm Suppress; cultivators keep their rank. */
    @GameTest(template = EMPTY_STRUCTURE)
    public void mortalPlayersAreWeighedAsMortals(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        PlayerCultivation c = CultivationManager.get(player);
        player.addEffect(new MobEffectInstance(ModEffects.QI_GATHERING, 100));
        helper.assertTrue(c.isMortal() && CultivationStats.maxHealth(c.getEffectiveRealm(), c.getEffectiveStage()) > 0,
                "A Qi Refining realm still has bonuses on paper");
        CultivationManager.refresh(player);
        helper.assertTrue(player.getMaxHealth() == 20, "...but a mortal's body gets none of them");
        c.awaken(0);
        CultivationManager.refresh(player);
        helper.assertTrue(player.getMaxHealth() > 20, "Once awakened, the realm's bonuses apply");
        helper.succeed();
    }
    @GameTest(template = EMPTY_STRUCTURE)
    public void rareHerbsStandInAtTwiceTheirAge(GameTestHelper helper) {
        List<ItemStack> foundation = new java.util.ArrayList<>(List.of(GinsengItem.create(ModItems.GINSENG, 100),
                GinsengItem.create(ModItems.PURPLE_LINGZHI, 300), GinsengItem.create(ModItems.SPIRIT_GINSENG, 100),
                new ItemStack(Items.AMETHYST_SHARD)));
        helper.assertTrue(AlchemyRecipes.match(foundation) != null && AlchemyRecipes.match(foundation).output() == ModItems.FOUNDATION_PILL,
                "Purple Lingzhi stands in for Lingzhi in the Foundation Pill");
        helper.assertTrue(AlchemyRecipes.averageAge(foundation) == Math.round((100 + 600 + 100) / 3.0), "...counting as twice its age");
        List<ItemStack> core = List.of(GinsengItem.create(ModItems.SPIRIT_GINSENG, 1), GinsengItem.create(ModItems.OCHRE_HUANGJING, 1),
                new ItemStack(ModItems.CULTIVATION_FRUIT), new ItemStack(Items.BLAZE_POWDER));
        helper.assertTrue(AlchemyRecipes.match(core) != null && AlchemyRecipes.match(core).output() == ModItems.CORE_PILL,
                "Ochre Huangjing stands in for Huangjing in the Core Pill");
        helper.assertTrue(GinsengBlock.plantFor(ModItems.SPIRIT_LOTUS) == ModBlocks.SPIRIT_LOTUS
                && GinsengBlock.plantFor(ModItems.PURPLE_LINGZHI) == ModBlocks.PURPLE_LINGZHI, "Each herb knows the plant it came from");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void lotusFloatsAndHerbsDigUpWithTheirAge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos water = new BlockPos(2, 1, 2);
        helper.setBlock(water, Blocks.WATER);
        helper.setBlock(water.above(), ModBlocks.SPIRIT_LOTUS);
        BlockPos lotusPos = helper.absolutePos(water.above());
        helper.assertTrue(level.getBlockState(lotusPos).canSurvive(level, lotusPos), "The Spirit Lotus floats on water");
        GinsengBlockEntity lotus = (GinsengBlockEntity) level.getBlockEntity(lotusPos);
        lotus.setAge(2_000);
        helper.assertTrue(level.getBlockState(lotusPos).getValue(GinsengBlock.STAGE) == 2, "A 2,000-year lotus is in flower");
        ServerPlayer player = survivalPlayer(helper);
        level.getBlockState(lotusPos).use(level, player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(lotusPos), net.minecraft.core.Direction.UP, lotusPos, false));
        helper.assertTrue(level.getBlockState(lotusPos).isAir(), "Picking it takes the lotus");
        ItemStack picked = player.getInventory().getItem(0);
        helper.assertTrue(picked.is(ModItems.SPIRIT_LOTUS) && GinsengItem.age(picked) == 2_000, "The lotus keeps its age");
        helper.assertTrue(player.getInventory().countItem(ModItems.SPIRIT_LOTUS_SEEDS) >= 1, "A full-grown lotus gives seeds");

        BlockPos dry = new BlockPos(4, 2, 4);
        helper.setBlock(dry.below(), Blocks.STONE);
        helper.assertTrue(!ModBlocks.SPIRIT_LOTUS.defaultBlockState().canSurvive(level, helper.absolutePos(dry)), "...and not on stone");

        BlockPos soil = new BlockPos(5, 2, 1);
        helper.setBlock(soil.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(soil, ModBlocks.LINGZHI);
        ((GinsengBlockEntity) level.getBlockEntity(helper.absolutePos(soil))).setAge(700);
        helper.assertTrue(((GinsengBlock) ModBlocks.LINGZHI).root(700).is(ModItems.LINGZHI), "Lingzhi digs up as Lingzhi");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dewGrassGathersDewThatRestoresQi(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos grass = new BlockPos(2, 2, 2);
        helper.setBlock(grass.below(), Blocks.GRASS_BLOCK);
        helper.setBlock(grass, ModBlocks.SPIRIT_DEW_GRASS);
        BlockPos pos = helper.absolutePos(grass);
        for (int i = 0; i < 100 && !level.getBlockState(pos).getValue(SpiritDewGrassBlock.DEW); i++) {
            level.getBlockState(pos).randomTick(level, pos, level.random);
        }
        helper.assertTrue(level.getBlockState(pos).getValue(SpiritDewGrassBlock.DEW), "Dew gathers on the grass");
        ServerPlayer player = survivalPlayer(helper);
        level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
        helper.assertTrue(player.getInventory().countItem(ModItems.SPIRIT_DEW) == 1 && !level.getBlockState(pos).getValue(SpiritDewGrassBlock.DEW)
                && level.getBlockState(pos).is(ModBlocks.SPIRIT_DEW_GRASS), "Collecting the dew leaves the grass");

        ItemStack mortalDew = new ItemStack(ModItems.SPIRIT_DEW);
        eat(player, mortalDew);
        helper.assertTrue(mortalDew.getCount() == 1, "A mortal can't drink it");
        PlayerCultivation c = CultivationManager.get(player);
        c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
        c.setQi(0);
        ItemStack dew = new ItemStack(ModItems.SPIRIT_DEW);
        eat(player, dew);
        helper.assertTrue(dew.isEmpty() && Math.abs(c.getQi() - c.maxQi() * SpiritDewItem.QI_RESTORED) < 1e-6, "A drop refills a quarter of the qi pool");
        helper.succeed();
    }
    @GameTest(template = EMPTY_STRUCTURE)
    public void wildHerbsTakeRootOnTheirGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // A pond the lotus feature can find, and a grass patch for the soil herbs, all around the origin.
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            helper.setBlock(new BlockPos(x, 1, z), x < 4 ? Blocks.WATER : Blocks.GRASS_BLOCK);
            helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
        }
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42);
        java.util.function.Function<net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>, Boolean> place =
                feature -> feature.place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(java.util.Optional.empty(), level,
                        level.getChunkSource().getGenerator(), random, origin,
                        net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        int lotus = 0, dewGrass = 0;
        for (int i = 0; i < 40; i++) {
            if (place.apply(ModFeatures.SPIRIT_LOTUS)) lotus++;
            if (place.apply(ModFeatures.SPIRIT_DEW_GRASS)) dewGrass++;
        }
        int onWater = 0, onSoil = 0;
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            net.minecraft.world.level.block.state.BlockState at = level.getBlockState(helper.absolutePos(new BlockPos(x, 2, z)));
            if (at.is(ModBlocks.SPIRIT_LOTUS)) { helper.assertTrue(x < 4, "Lotus only on the water"); onWater++; }
            if (at.is(ModBlocks.SPIRIT_DEW_GRASS)) { helper.assertTrue(x >= 4, "Dew grass only on the grass"); onSoil++; }
        }
        helper.assertTrue(lotus > 0 && onWater > 0, "Wild Spirit Lotus takes root on still water");
        helper.assertTrue(dewGrass > 0 && onSoil > 0, "Wild Spirit Dew Grass takes root on grass");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void blueSpiritTreeGrowsAsARareSpruce(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
        net.minecraft.core.RegistryAccess registries = level.registryAccess();
        var tree = registries.registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ModConfiguredFeatures.BLUE_SPIRIT_TREE).value();
        var placed = registries.registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        helper.assertTrue(placed.containsKey(ModPlacedFeatures.BLUE_SPIRIT_TREE.location())
                && placed.containsKey(ModPlacedFeatures.BLUE_SPIRIT_TREE_UPPER_REALM.location()), "The tree's placements load");
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.assertTrue(tree.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(7), base), "The tree grows");
        int logs = 0, leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-4, 0, -4), base.offset(4, 12, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.BLUE_SPIRIT_LOG)) logs++;
            if (state.is(Blocks.SPRUCE_LEAVES)) leaves++;
        }
        helper.assertTrue(logs >= 6 && leaves > 10, "A spruce of Blue Spirit Log: " + logs + " logs, " + leaves + " leaves");
        helper.assertTrue(level.getBlockState(base).is(ModBlocks.BLUE_SPIRIT_LOG), "Its trunk starts on the ground");
        helper.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void blueSpiritTreeTakesRootUnderACanopy(GameTestHelper helper) {
        // In a taiga the Blue Spirit Tree comes after the biome's own trees, so most spots are under leaves. Its Overworld
        // placement (less the chance, spread and biome checks) must find the ground there, not the top of the canopy.
        ServerLevel level = helper.getLevel();
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
            helper.setBlock(new BlockPos(x, 7, z), Blocks.SPRUCE_LEAVES.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
        net.minecraft.core.RegistryAccess registries = level.registryAccess();
        var placed = registries.registryOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).getOrThrow(ModPlacedFeatures.BLUE_SPIRIT_TREE);
        List<net.minecraft.world.level.levelgen.placement.PlacementModifier> local = placed.placement().stream()
                .filter(m -> !(m instanceof net.minecraft.world.level.levelgen.placement.RarityFilter)
                        && !(m instanceof net.minecraft.world.level.levelgen.placement.InSquarePlacement)
                        && !(m instanceof net.minecraft.world.level.levelgen.placement.BiomeFilter))
                .toList();
        BlockPos origin = helper.absolutePos(new BlockPos(4, 2, 4));
        boolean grew = new net.minecraft.world.level.levelgen.placement.PlacedFeature(placed.feature(), local)
                .place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(3), origin);
        helper.assertTrue(grew && level.getBlockState(origin).is(ModBlocks.BLUE_SPIRIT_LOG), "The tree grows from the ground up through the canopy");
        helper.succeed();
    }
}
