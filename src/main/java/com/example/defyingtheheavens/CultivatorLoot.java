package com.example.defyingtheheavens;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/**
 * What a cultivator has of its own to stake in a spar or to hand over as tribute (see SparSystem, TalkSystem): what its sect
 * keeps for one of its station, drawn from the sect chests' loot tables (a disciple's dwelling, an elder's chest, the Sect
 * Master's treasury; rogues carry a disciple's things, or an elder's from Nascent Soul on).
 */
public final class CultivatorLoot {
	private static final ResourceLocation DWELLING = DefyingTheHeavens.id("chests/sect_dwelling");
	private static final ResourceLocation ELDER = DefyingTheHeavens.id("chests/sect_elder");
	private static final ResourceLocation TREASURY = DefyingTheHeavens.id("chests/sect_treasury");
	/** Draws from the table before giving up on finding a stake of like worth. */
	private static final int STAKE_DRAWS = 8;

	public static ResourceLocation tableFor(CultivatorNpc npc) {
		return switch (npc.getTitle()) {
			case SECT_MASTER -> TREASURY;
			case GRAND_ELDER, ELDER -> ELDER;
			case INNER_DISCIPLE, OUTER_DISCIPLE, WANDERER -> DWELLING;
			case ROGUE -> npc.getRank() >= PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY) ? ELDER : DWELLING;
		};
	}

	/** One draw of what it has. */
	public static List<ItemStack> roll(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level)) return List.of();
		LootTable table = level.getServer().getLootData().getLootTable(tableFor(npc));
		LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, npc.position()).create(LootContextParamSets.CHEST);
		return new ArrayList<>(table.getRandomItems(params));
	}

	/**
	 * A stake to match {@code worth} (TalkSystem#worth): one thing from what it has worth half to twice as much, the closest
	 * found; failing that, spirit stones of that worth.
	 */
	public static List<ItemStack> stakeFor(CultivatorNpc npc, double worth) {
		ItemStack best = ItemStack.EMPTY;
		double bestGap = Double.MAX_VALUE;
		for (int i = 0; i < STAKE_DRAWS; i++) {
			for (ItemStack stack : roll(npc)) {
				ItemStack one = stack.copyWithCount(1);
				double w = TalkSystem.worth(one);
				if (w < worth * 0.5 || w > worth * 2) continue;
				double gap = Math.abs(w - worth);
				if (gap < bestGap) {
					best = one;
					bestGap = gap;
				}
			}
		}
		if (!best.isEmpty()) return List.of(best);
		return SpiritStoneItem.stonesFor(Math.max(1, (int) Math.round(worth)));
	}

	private CultivatorLoot() {}
}
