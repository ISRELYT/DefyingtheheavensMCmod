package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * Immortal Jade and the Immortal Realm's markets (phase 2, 2026-10-25: the user's "Immortal jade + cities"): the coin nine Immortal stones
 * make, crafted up and back, carried by True Immortals and paid out by the trades; a market's merchant, who sells for it and keeps its
 * stall; and the user's rule that none below Entering Nirvana is born in the Immortal Realm (its wanderers' roll, its census). The markets
 * peopled in the realm itself are seen in game (the tests' server has no Immortal Realm).
 */
public class ImmortalMarketsGameTests implements FabricGameTest {
	private static final int JADE = SpiritStoneItem.Grade.IMMORTAL_JADE.value();

	@GameTest(template = EMPTY_STRUCTURE)
	public void immortalJadeIsNineImmortalStones(GameTestHelper helper) {
		SpiritStoneItem.Grade jade = SpiritStoneItem.Grade.IMMORTAL_JADE;
		helper.assertTrue(jade.value() == 9 * SpiritStoneItem.Grade.IMMORTAL.value() && SpiritStoneItem.Grade.IMMORTAL.next() == jade && jade.next() == null
				&& SpiritStoneItem.of(jade) == ModItems.IMMORTAL_JADE, "Nine Immortal stones make one, the last grade");
		RegistryAccess access = helper.getLevel().registryAccess();
		Recipe<?> up = helper.getLevel().getRecipeManager().byKey(DefyingTheHeavens.id("immortal_jade_from_immortal")).orElse(null);
		Recipe<?> down = helper.getLevel().getRecipeManager().byKey(DefyingTheHeavens.id("immortal_spirit_stone_from_jade")).orElse(null);
		helper.assertTrue(up != null && up.getResultItem(access).is(ModItems.IMMORTAL_JADE) && up.getIngredients().size() == 9
				&& up.getIngredients().get(0).test(new ItemStack(ModItems.IMMORTAL_SPIRIT_STONE)), "Crafted from nine Immortal stones");
		helper.assertTrue(down != null && down.getResultItem(access).is(ModItems.IMMORTAL_SPIRIT_STONE) && down.getResultItem(access).getCount() == 9,
				"...and back into nine");
		helper.assertTrue(TalkSystem.worth(new ItemStack(ModItems.IMMORTAL_JADE)) == JADE && SpiritStoneItem.valueOf(new ItemStack(ModItems.IMMORTAL_JADE, 3)) == 3 * JADE,
				"Worth its nine stones");
		// The trades pay and price in it.
		ItemStack[] costs = CultivatorTrades.costsFor(60 * JADE);
		helper.assertTrue(costs[0].is(ModItems.IMMORTAL_JADE) && costs[0].getCount() == 60 && costs[1].isEmpty(), "Sixty jade's price asked in jade");
		ItemStack[] mixed = CultivatorTrades.costsFor(2 * JADE + 4 * SpiritStoneItem.Grade.IMMORTAL.value());
		helper.assertTrue(mixed[0].is(ModItems.IMMORTAL_JADE) && mixed[0].getCount() == 2 && mixed[1].is(ModItems.IMMORTAL_SPIRIT_STONE) && mixed[1].getCount() == 4,
				"Two jade and four stones");
		List<ItemStack> stones = SpiritStoneItem.stonesFor(JADE + 1);
		helper.assertTrue(stones.size() == 2 && stones.get(0).is(ModItems.IMMORTAL_JADE) && stones.get(1).is(ModItems.LOW_SPIRIT_STONE), "Made up of the fewest stones");
		// A True Immortal carries it (and an Entering Nirvana none).
		RandomSource random = RandomSource.create(7);
		boolean immortalJade = false, nirvanaJade = false;
		for (int i = 0; i < 20; i++) {
			for (ItemStack stack : CultivatorNpc.carriedStones(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), random)) immortalJade |= stack.is(ModItems.IMMORTAL_JADE);
			for (ItemStack stack : CultivatorNpc.carriedStones(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION), random)) nirvanaJade |= stack.is(ModItems.IMMORTAL_JADE);
		}
		helper.assertTrue(immortalJade && !nirvanaJade, "True Immortals carry Immortal Jade, those below none");
		helper.assertTrue(CultivatorNpc.carriedStones(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), random).get(0).is(ModItems.IMMORTAL_SPIRIT_STONE),
				"Their stones first (a gift of what they carry is the stones)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anImmortalMerchantSellsForJade(GameTestHelper helper) {
		boolean heavenSlip = false, ghostSlip = false;
		for (int n = 0; n < 12; n++) {
			CultivatorNpc npc = ModEntities.CULTIVATOR.create(helper.getLevel());
			try {
				npc.becomeRogue(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0, helper.getLevel().getRandom());
				npc.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), true); // a True Immortal, whatever the census here
				npc.setTitle(NpcTitle.IMMORTAL_MERCHANT);
				helper.assertTrue(!npc.keepsPost(), "No stall, no post");
				npc.setSettledAt(helper.absolutePos(new BlockPos(1, 2, 1)));
				helper.assertTrue(npc.keepsPost() && !CityMarkets.isMerchant(npc), "Keeps its stall (one of the markets' merchants only in the Immortal Realm)");
				boolean slip = false, vessel = false, pills = false;
				for (CultivatorTrades.Goods g : CultivatorTrades.goodsFor(npc, RandomSource.create(n))) {
					if (g.item().is(ModItems.JADE_SLIP)) {
						HeavenMethod method = JadeSlipItem.method(g.item());
						slip = method != null && method.realm() == Realm.TRUE_IMMORTAL && g.price() == CultivatorTrades.heavenSlipPrice(method) && g.regard() == Regard.COURTEOUS;
						heavenSlip |= slip;
					}
					if (g.item().is(ModItems.GHOST_JADE_SLIP)) {
						slip = GhostJadeSlipItem.method(g.item()) == GhostMethod.NETHERWORLD_IMMORTAL_CANON;
						ghostSlip |= slip;
					}
					if (g.item().getItem() instanceof SacredVesselItem) vessel = g.price() == 30 * JADE && g.regard() == Regard.WARM;
					if (g.item().getItem() instanceof PillItem && PillItem.grade(g.item()) == PillGrade.IMMORTAL) pills = true;
				}
				helper.assertTrue(slip && vessel && pills, "Sells a True Immortal's slip, a Sacred Vessel and Immortal-grade pills (" + slip + ", " + vessel + ", " + pills + ")");
			} finally {
				npc.discard();
			}
		}
		helper.assertTrue(heavenSlip, "Its element's True Immortal heaven method, from most");
		helper.assertTrue(CultivatorTrades.heavenSlipPrice(HeavenMethod.of(QiElement.FIRE, Realm.TRUE_IMMORTAL)) == 60 * JADE, "Sixty jade a True Immortal's slip");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noneBelowEnteringNirvanaIsBornInTheImmortalRealm(GameTestHelper helper) {
		String immortal = ModDimensions.IMMORTAL_REALM.location().toString();
		int floor = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY);
		helper.assertTrue(NpcSpawner.IMMORTAL_FLOOR == floor, "Its wanderers from Entering Nirvana Early up");
		RandomSource random = RandomSource.create(3);
		for (int i = 0; i < 200; i++) {
			int rank = NpcSpawner.rollImmortalRank(random, true);
			helper.assertTrue(rank >= floor && rank <= PlayerCultivation.LAST_RANK, "Every roll of them Entering Nirvana or above (" + rank + ")");
		}
		helper.assertTrue(HighRealms.quota(Realm.ENTERING_NIRVANA, immortal) == Integer.MAX_VALUE && HighRealms.quota(Realm.TRUE_IMMORTAL, immortal) > 0,
				"Entering Nirvana without limit, so the census never holds one born there back below it");
		helper.succeed();
	}
}
