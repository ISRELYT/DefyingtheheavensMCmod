package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * The mod's own in the vanilla world's chests (the user's ask, 2026-11-16: "i also want end cities and all the other stuff to have loot from the mod
 * in it, and village chests to have stuff like low-grade spirit stones and that"): a pool of goods added to each table, and in all but the
 * villages' a chance at the writings, by how deep the place lies ({@link Tier}):
 * <ul>
 *   <li><b>villages</b>' houses and shops: low-grade spirit stones, young ginseng, now and then a low Qi Gathering Pill;</li>
 *   <li><b>the shallow places</b> (dungeons, mineshafts, the temples, shipwrecks, ruined portals, outposts, igloos, buried treasure, ocean ruins):
 *   low and a few mid stones, herbs, Qi Gathering and Cultivation Pills; a third of them a Method Manual or an Art Manual of Qi Refining to
 *   Foundation Building;</li>
 *   <li><b>the deep places</b> (strongholds, woodland mansions, ancient cities, the bastions, Nether fortresses): mid and a few high stones, older
 *   herbs, Foundation and Core Pills; nearly half a manual of Core Formation to Nascent Soul;</li>
 *   <li><b>end cities</b>: high stones, the oldest herbs, Core Pills; most a manual of Nascent Soul to Heavenly Being, and a quarter a Technique
 *   Jade Slip.</li>
 * </ul>
 * A Method Manual's method is any a manual holds (CultivationMethod#fromManual), of any path, written to one of the tier's realms its volumes span;
 * an Art Manual's a heaven art or a ghost art of those realms. Every one an entry of its own in the pool (the vanilla way: what a chest holds is
 * drawn as it is filled). Only the vanilla tables themselves: a data pack's own replacement is left as it is.
 */
public final class VanillaLoot {
	/** How deep a place lies: what its chests hold of the mod's. */
	enum Tier {
		VILLAGE(null, null, 0, 0),
		SHALLOW(Realm.QI_REFINING, Realm.FOUNDATION_BUILDING, 0.33f, 0),
		DEEP(Realm.CORE_FORMATION, Realm.NASCENT_SOUL, 0.45f, 0),
		END(Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, 0.6f, 0.25f);

		/** The realms its writings are of; how often a chest holds one, and a technique's slip. */
		final Realm low, high;
		final float writings, slip;

		Tier(Realm low, Realm high, float writings, float slip) {
			this.low = low;
			this.high = high;
			this.writings = writings;
			this.slip = slip;
		}
	}

	private static final Map<ResourceLocation, Tier> TABLES = new HashMap<>();

	static {
		for (ResourceLocation id : new ResourceLocation[] {BuiltInLootTables.VILLAGE_WEAPONSMITH, BuiltInLootTables.VILLAGE_TOOLSMITH,
				BuiltInLootTables.VILLAGE_ARMORER, BuiltInLootTables.VILLAGE_CARTOGRAPHER, BuiltInLootTables.VILLAGE_MASON, BuiltInLootTables.VILLAGE_SHEPHERD,
				BuiltInLootTables.VILLAGE_BUTCHER, BuiltInLootTables.VILLAGE_FLETCHER, BuiltInLootTables.VILLAGE_FISHER, BuiltInLootTables.VILLAGE_TANNERY,
				BuiltInLootTables.VILLAGE_TEMPLE, BuiltInLootTables.VILLAGE_DESERT_HOUSE, BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
				BuiltInLootTables.VILLAGE_TAIGA_HOUSE, BuiltInLootTables.VILLAGE_SNOWY_HOUSE, BuiltInLootTables.VILLAGE_SAVANNA_HOUSE}) {
			TABLES.put(id, Tier.VILLAGE);
		}
		for (ResourceLocation id : new ResourceLocation[] {BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.ABANDONED_MINESHAFT,
				BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.JUNGLE_TEMPLE, BuiltInLootTables.SHIPWRECK_SUPPLY, BuiltInLootTables.SHIPWRECK_TREASURE,
				BuiltInLootTables.RUINED_PORTAL, BuiltInLootTables.PILLAGER_OUTPOST, BuiltInLootTables.IGLOO_CHEST, BuiltInLootTables.BURIED_TREASURE,
				BuiltInLootTables.UNDERWATER_RUIN_SMALL, BuiltInLootTables.UNDERWATER_RUIN_BIG}) {
			TABLES.put(id, Tier.SHALLOW);
		}
		for (ResourceLocation id : new ResourceLocation[] {BuiltInLootTables.STRONGHOLD_LIBRARY, BuiltInLootTables.STRONGHOLD_CORRIDOR,
				BuiltInLootTables.STRONGHOLD_CROSSING, BuiltInLootTables.WOODLAND_MANSION, BuiltInLootTables.ANCIENT_CITY, BuiltInLootTables.BASTION_TREASURE,
				BuiltInLootTables.BASTION_OTHER, BuiltInLootTables.BASTION_BRIDGE, BuiltInLootTables.BASTION_HOGLIN_STABLE, BuiltInLootTables.NETHER_BRIDGE}) {
			TABLES.put(id, Tier.DEEP);
		}
		TABLES.put(BuiltInLootTables.END_CITY_TREASURE, Tier.END);
	}

	/** The tier the vanilla table {@code id} is of, or null for one the mod leaves alone. */
	static Tier tierOf(ResourceLocation id) {
		return TABLES.get(id);
	}

	public static void register() {
		LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (!source.isBuiltin()) return; // a data pack's own replacement, left as it is
			Tier tier = TABLES.get(id);
			if (tier == null) return;
			tableBuilder.withPool(goods(tier));
			if (tier.writings > 0) tableBuilder.withPool(writings(tier));
			if (tier.slip > 0) tableBuilder.withPool(slips(tier));
		});
	}

	// --- The pools ---

	/** Spirit stones, herbs and pills by the tier: one or two of them (a village's, now and then one). */
	static LootPool.Builder goods(Tier tier) {
		LootPool.Builder pool = LootPool.lootPool();
		switch (tier) {
			case VILLAGE -> {
				pool.setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(0.6f));
				pool.add(entry(new ItemStack(ModItems.LOW_SPIRIT_STONE), 10, 1, 4));
				pool.add(entry(GinsengItem.create(ModItems.GINSENG, 10), 4, 1, 2));
				pool.add(entry(PillItem.create(ModItems.QI_GATHERING_PILL, PillGrade.LOW, 30), 2, 1, 1));
			}
			case SHALLOW -> {
				pool.setRolls(UniformGenerator.between(1, 2));
				pool.add(entry(new ItemStack(ModItems.LOW_SPIRIT_STONE), 10, 2, 6));
				pool.add(entry(new ItemStack(ModItems.MID_SPIRIT_STONE), 3, 1, 2));
				pool.add(entry(GinsengItem.create(ModItems.GINSENG, 50), 5, 1, 2));
				pool.add(entry(GinsengItem.create(ModItems.SPIRIT_GINSENG, 50), 2, 1, 1));
				pool.add(entry(PillItem.create(ModItems.QI_GATHERING_PILL, PillGrade.LOW, 60), 3, 1, 2));
				pool.add(entry(PillItem.create(ModItems.CULTIVATION_PILL, PillGrade.LOW, 60), 2, 1, 1));
			}
			case DEEP -> {
				pool.setRolls(UniformGenerator.between(1, 2));
				pool.add(entry(new ItemStack(ModItems.MID_SPIRIT_STONE), 8, 2, 5));
				pool.add(entry(new ItemStack(ModItems.HIGH_SPIRIT_STONE), 2, 1, 1));
				pool.add(entry(GinsengItem.create(ModItems.SPIRIT_GINSENG, 200), 4, 1, 2));
				pool.add(entry(GinsengItem.create(ModItems.PURPLE_LINGZHI, 200), 3, 1, 1));
				pool.add(entry(PillItem.create(ModItems.FOUNDATION_PILL, PillGrade.MID, 100), 2, 1, 1));
				pool.add(entry(PillItem.create(ModItems.CORE_PILL, PillGrade.MID, 100), 1, 1, 1));
			}
			case END -> {
				pool.setRolls(UniformGenerator.between(1, 3));
				pool.add(entry(new ItemStack(ModItems.HIGH_SPIRIT_STONE), 8, 1, 4));
				pool.add(entry(GinsengItem.create(ModItems.SPIRIT_LOTUS, 800), 4, 1, 1));
				pool.add(entry(GinsengItem.create(ModItems.PURPLE_LINGZHI, 800), 3, 1, 1));
				pool.add(entry(PillItem.create(ModItems.CORE_PILL, PillGrade.HIGH, 300), 3, 1, 1));
			}
		}
		return pool;
	}

	/**
	 * One manual, at {@link Tier#writings}: a Method Manual of any method a manual holds, of any path, written to a realm of the tier's its
	 * volumes span (each method once a realm), or an Art Manual of a heaven art or a ghost art of those realms, as likely as a method's.
	 */
	static LootPool.Builder writings(Tier tier) {
		LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(tier.writings));
		for (CultivationMethod method : CultivationMethod.values()) {
			if (!method.fromManual()) continue;
			for (int r = tier.low.ordinal(); r <= tier.high.ordinal(); r++) {
				Realm realm = Realm.byIndex(r);
				if (method.spans(realm)) pool.add(entry(MethodManualItem.of(method, realm), 1, 1, 1));
			}
		}
		for (HeavenMethod art : HeavenMethod.values()) {
			int r = art.realm().ordinal();
			if (r >= tier.low.ordinal() && r <= tier.high.ordinal() && art.realm().taughtInTheWorld()) pool.add(entry(TechniqueManualItem.of(art), 2, 1, 1));
		}
		for (GhostMethod art : GhostMethod.values()) {
			// The fire's and the steps' (no realm of their own) with Qi Refining's.
			int r = art.realm() == null ? Realm.QI_REFINING.ordinal() : art.realm().ordinal();
			if (r >= tier.low.ordinal() && r <= tier.high.ordinal() && art.taughtInTheWorld()) pool.add(entry(TechniqueManualItem.of(art), 2, 1, 1));
		}
		return pool;
	}

	/** A technique's jade slip (TechniqueSlipItem) at {@link Tier#slip}: any technique of the tier's realms or below. */
	static LootPool.Builder slips(Tier tier) {
		LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1)).when(LootItemRandomChanceCondition.randomChance(tier.slip));
		for (Technique technique : Technique.values()) {
			if (technique.realm().ordinal() <= tier.high.ordinal()) pool.add(entry(TechniqueSlipItem.of(technique), 1, 1, 1));
		}
		return pool;
	}

	/** An entry of {@code stack} (its tag kept: a manual's method, a pill's grade, a herb's years), {@code min} to {@code max} of it. */
	private static LootPoolSingletonContainer.Builder<?> entry(ItemStack stack, int weight, int min, int max) {
		LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(stack.getItem()).setWeight(weight);
		if (stack.hasTag()) entry.apply(SetNbtFunction.setTag(stack.getTag().copy()));
		if (max > 1) entry.apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
		return entry;
	}

	private VanillaLoot() {}
}
