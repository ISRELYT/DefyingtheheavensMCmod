package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The crafts' mastery (CraftMastery, 2026-10-28): the ranks by practice, what a rank does in alchemy (fails, better grades, second pills),
 * never a grade past what the herbs can give, mastery kept and carried over, and a brew teaching its brewer.
 */
public class CraftMasteryGameTests implements FabricGameTest {
	private static final BlockPos CAULDRON = new BlockPos(2, 2, 2);

	@GameTest(template = EMPTY_STRUCTURE)
	public void ranksGrowWithPracticeAndDoMore(GameTestHelper helper) {
		helper.assertTrue(CraftMastery.Rank.of(0) == CraftMastery.Rank.APPRENTICE && CraftMastery.Rank.of(19) == CraftMastery.Rank.APPRENTICE
				&& CraftMastery.Rank.of(20) == CraftMastery.Rank.ADEPT && CraftMastery.Rank.of(600) == CraftMastery.Rank.SAINT
				&& CraftMastery.Rank.of(100_000) == CraftMastery.Rank.SAINT && CraftMastery.Rank.SAINT.next() == null, "Ranks by practice, Saint the last");
		AlchemyRecipes.Recipe elixir = AlchemyRecipes.forOutput(ModItems.MARROW_CLEANSING_ELIXIR);
		AlchemyRecipes.Recipe core = AlchemyRecipes.forOutput(ModItems.CORE_PILL);
		helper.assertTrue(CraftMastery.failChance(CraftMastery.Rank.APPRENTICE, elixir.difficulty()) == 0, "The elixir never fails, even for an apprentice");
		helper.assertTrue(Math.abs(CraftMastery.failChance(CraftMastery.Rank.APPRENTICE, core.difficulty()) - 3 * CraftMastery.FAIL_PER_RANK_SHORT) < 1e-9
				&& CraftMastery.failChance(CraftMastery.Rank.GRANDMASTER, core.difficulty()) == 0, "The Core Pill fails an apprentice most, a Grandmaster never");
		helper.assertTrue(CraftMastery.betterGrade(CraftMastery.Rank.APPRENTICE) == 0 && CraftMastery.betterGrade(CraftMastery.Rank.SAINT) > 0.3
				&& CraftMastery.secondPill(CraftMastery.Rank.MASTER) == 0 && CraftMastery.secondPill(CraftMastery.Rank.SAINT) > 0,
				"Better grades from Adept on, second pills from Grandmaster");
		helper.assertTrue(core.practice() > elixir.practice() && elixir.practice() > 0, "Harder pills teach more");
		for (AlchemyRecipes.Recipe recipe : AlchemyRecipes.all()) {
			helper.assertTrue(recipe.difficulty() >= 0 && recipe.difficulty() <= 3, recipe.output() + " has a difficulty a rank can reach");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBetterGradeNeverPassesTheHerbs(GameTestHelper helper) {
		helper.assertTrue(PillGrade.better(PillGrade.LOW, 1) == PillGrade.MID && PillGrade.better(PillGrade.MID, 1) == PillGrade.MID,
				"Fresh roots: a master's hand makes Mid at best, as the herbs can");
		helper.assertTrue(PillGrade.better(PillGrade.SUPREME, 999) == PillGrade.SUPREME && PillGrade.better(PillGrade.SUPREME, 1000) == PillGrade.IMMORTAL,
				"Immortal only from herbs of a thousand years");
		helper.assertTrue(PillGrade.better(PillGrade.IMMORTAL, FruitAge.MAX_YEARS) == PillGrade.IMMORTAL, "Nothing past Immortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void masteryIsKeptAndCarriedOver(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMastery(CraftMastery.Craft.ALCHEMY, 321);
		c.setMastery(CraftMastery.Craft.FORMATIONS, 7);
		PlayerCultivation loaded = PlayerCultivation.load(c.save());
		helper.assertTrue(loaded.getMastery(CraftMastery.Craft.ALCHEMY) == 321 && loaded.getMastery(CraftMastery.Craft.FORMATIONS) == 7
				&& loaded.getMastery(CraftMastery.Craft.FORGING) == 0, "Mastery is saved");
		PlayerCultivation reborn = new PlayerCultivation();
		reborn.carryOver(loaded);
		helper.assertTrue(reborn.getMastery(CraftMastery.Craft.ALCHEMY) == 321, "...and carried over a rebirth");
		helper.assertTrue(PlayerCultivation.load(new PlayerCultivation().save()).getMastery(CraftMastery.Craft.ALCHEMY) == 0,
				"None practised: none saved, none loaded");
		helper.succeed();
	}

	/** A Saint brews: never a failure, the pill's practice learned; an apprentice's elixir teaches them too. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBrewTeachesItsBrewer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.setBlock(CAULDRON.below(), Blocks.MAGMA_BLOCK);
		helper.setBlock(CAULDRON, ModBlocks.ALCHEMY_CAULDRON);
		BlockPos pos = helper.absolutePos(CAULDRON);
		AlchemyCauldronBlockEntity cauldron = (AlchemyCauldronBlockEntity) level.getBlockEntity(pos);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation own = CultivationManager.own(player);
		own.setMastery(CraftMastery.Craft.ALCHEMY, CraftMastery.Rank.SAINT.from);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
		for (ItemStack ingredient : List.of(GinsengItem.create(ModItems.SPIRIT_GINSENG, 100), GinsengItem.create(ModItems.SPIRIT_LOTUS, 100),
				CultivationFruitItem.create(100), new ItemStack(Items.BLAZE_POWDER))) {
			player.setItemInHand(InteractionHand.MAIN_HAND, ingredient);
			cauldron.interact(level.getBlockState(pos), player, InteractionHand.MAIN_HAND);
		}
		helper.assertTrue(level.getBlockState(pos).getValue(AlchemyCauldronBlock.BREWING), "The Core Pill brews");
		int before = own.getMastery(CraftMastery.Craft.ALCHEMY);
		int ticks = AlchemyRecipes.forOutput(ModItems.CORE_PILL).brewTicks();
		for (int i = 0; i < ticks + 5; i++) AlchemyCauldronBlockEntity.serverTick(level, pos, level.getBlockState(pos), cauldron);
		List<ItemEntity> out = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2), e -> e.getItem().is(ModItems.CORE_PILL));
		helper.assertTrue(!out.isEmpty() && out.size() <= 2, "A Saint's Core Pill never fails (one pill, or two)");
		helper.assertTrue(own.getMastery(CraftMastery.Craft.ALCHEMY) == before + AlchemyRecipes.forOutput(ModItems.CORE_PILL).practice(),
				"The brew taught its brewer");
		helper.succeed();
	}
}
