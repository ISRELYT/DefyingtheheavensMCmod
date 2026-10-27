package com.example.defyingtheheavens;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The bug sweep of 2026-10-27 (after the pedestals' rings, drawn as fruit, went unseen so long: things nobody happened to look at). Whatever
 * the mod registers has its English words (its items, blocks, entities, effects, biomes, its harms' death messages, its gamerules) and its
 * looks (an item's model, a block's states), checked against the live registries so a thing added later without them is caught here; a
 * block with an item gives something back when broken; a death by one who dealt it names them (vanilla's plain message never did, and a
 * renamed blade in their hand showed a raw key); the fruit the trades sell is as old as its price says.
 */
public class SweepFourGameTests implements FabricGameTest {
	private static final String NS = DefyingTheHeavens.MOD_ID;

	/** The English words, as the game's own resources hold them. */
	private static JsonObject lang() {
		try (InputStream in = DefyingTheHeavens.class.getResourceAsStream("/assets/" + NS + "/lang/en_us.json")) {
			if (in == null) return new JsonObject();
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (IOException e) {
			return new JsonObject();
		}
	}

	private static boolean exists(String path) {
		return DefyingTheHeavens.class.getResource(path) != null;
	}

	private static boolean ours(ResourceLocation id) {
		return id != null && id.getNamespace().equals(NS);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everythingHasAName(GameTestHelper helper) {
		JsonObject lang = lang();
		helper.assertTrue(lang.size() > 1000, "The English words are there to check against");
		List<String> missing = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (ours(BuiltInRegistries.ITEM.getKey(item)) && !lang.has(item.getDescriptionId())) missing.add(item.getDescriptionId());
		}
		for (Block block : BuiltInRegistries.BLOCK) {
			if (ours(BuiltInRegistries.BLOCK.getKey(block)) && !lang.has(block.getDescriptionId())) missing.add(block.getDescriptionId());
		}
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (ours(BuiltInRegistries.ENTITY_TYPE.getKey(type)) && !lang.has(type.getDescriptionId())) missing.add(type.getDescriptionId());
		}
		for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
			if (ours(BuiltInRegistries.MOB_EFFECT.getKey(effect)) && !lang.has(effect.getDescriptionId())) missing.add(effect.getDescriptionId());
		}
		// A biome's name is told in rumours (Rumours#biomeName) and on the debug screen.
		Registry<Biome> biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		for (ResourceLocation id : biomes.keySet()) {
			String key = "biome." + id.getNamespace() + "." + id.getPath();
			if (ours(id) && !lang.has(key)) missing.add(key);
		}
		// Each harm's death message, and the one for a death with someone who last hurt them.
		Registry<DamageType> harms = helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
		for (Map.Entry<ResourceKey<DamageType>, DamageType> harm : harms.entrySet()) {
			if (!ours(harm.getKey().location())) continue;
			String key = "death.attack." + harm.getValue().msgId();
			if (!lang.has(key)) missing.add(key);
			if (!lang.has(key + ".player")) missing.add(key + ".player");
		}
		// The gamerules, as the Edit Game Rules screen names them.
		for (Field field : ModGameRules.class.getDeclaredFields()) {
			if (!GameRules.Key.class.isAssignableFrom(field.getType())) continue;
			try {
				field.setAccessible(true);
				String key = "gamerule." + ((GameRules.Key<?>) field.get(null)).getId();
				if (!lang.has(key)) missing.add(key);
			} catch (IllegalAccessException e) {
				missing.add(field.getName());
			}
		}
		helper.assertTrue(missing.isEmpty(), "Named in English: " + String.join(", ", missing));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everythingHasItsLooks(GameTestHelper helper) {
		List<String> missing = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			if (ours(id) && !exists("/assets/" + NS + "/models/item/" + id.getPath() + ".json")) missing.add("item model " + id.getPath());
		}
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if (ours(id) && !exists("/assets/" + NS + "/blockstates/" + id.getPath() + ".json")) missing.add("blockstates " + id.getPath());
		}
		helper.assertTrue(missing.isEmpty(), "Drawn by its own model: " + String.join(", ", missing));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void blocksWithItemsGiveSomethingBack(GameTestHelper helper) {
		LootDataManager loot = helper.getLevel().getServer().getLootData();
		List<String> none = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if (!ours(id) || block.asItem() == Items.AIR) continue;
			ResourceLocation table = block.getLootTable();
			if (table.equals(BuiltInLootTables.EMPTY)) continue; // nothing, of its own making (noLootTable: the First Flame)
			if (loot.getLootTable(table) != LootTable.EMPTY || ownDrops(block)) continue;
			none.add(id.getPath());
		}
		helper.assertTrue(none.isEmpty(), "Broken, a block that has an item gives something back: " + String.join(", ", none));
		helper.succeed();
	}

	/** Whether {@code block}'s own class gives its drops (Block#getDrops of its own: the Spirit Pedestal's, its treasure with it). */
	private static boolean ownDrops(Block block) {
		for (Class<?> c = block.getClass(); c != null && c != Block.class && c != BlockBehaviour.class; c = c.getSuperclass()) {
			if (!c.getName().startsWith("com.example.defyingtheheavens")) continue;
			try {
				c.getDeclaredMethod("getDrops", BlockState.class, LootParams.Builder.class);
				return true;
			} catch (NoSuchMethodException e) {
				// not here: its parent, then
			}
		}
		return false;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDeathByAnotherNamesThem(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		JsonObject lang = lang();
		Zombie victim = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
		Zombie killer = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
		killer.setCustomName(Component.literal("Steve"));
		// A renamed blade in hand: vanilla would tell it with ".item", which none of the mod's harms has.
		ItemStack blade = new ItemStack(Items.IRON_SWORD);
		blade.setHoverName(Component.literal("Azure Frost"));
		killer.setItemInHand(InteractionHand.MAIN_HAND, blade);
		List<DamageSource> dealt = List.of(ModDamageTypes.qi(level, QiElement.FIRE, killer), ModDamageTypes.qi(level, QiElement.WOOD, Realm.CORE_FORMATION, null, killer),
				ModDamageTypes.heavenDemon(level, killer), ModDamageTypes.ghostly(level, killer), ModDamageTypes.soulSeized(level, killer));
		for (DamageSource source : dealt) {
			Component message = source.getLocalizedDeathMessage(victim);
			TranslatableContents told = message.getContents() instanceof TranslatableContents t ? t : null;
			String key = told == null ? "" : told.getKey();
			helper.assertTrue(key.endsWith(".player") && lang.has(key), "A death by one who dealt it is told of them: " + key);
			Object[] args = told.getArgs();
			helper.assertTrue(args.length == 2 && args[1] instanceof Component name && name.getString().equals("Steve"), "...naming them (" + key + ")");
		}
		// With none who dealt it, vanilla's own way: the plain message.
		Component alone = ModDamageTypes.chaosStorm(level).getLocalizedDeathMessage(victim);
		String key = alone.getContents() instanceof TranslatableContents t ? t.getKey() : "";
		helper.assertTrue(key.equals("death.attack." + NS + ".chaos_storm") && lang.has(key), "With none who dealt it, the plain message: " + key);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theFruitSoldIsAsOldAsItsPrice(GameTestHelper helper) {
		CultivatorNpc master = ModEntities.CULTIVATOR.create(helper.getLevel());
		CultivatorNpc merchant = ModEntities.CULTIVATOR.create(helper.getLevel());
		try {
			master.becomeRogue(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 0, helper.getLevel().getRandom());
			master.setTitle(NpcTitle.SECT_MASTER);
			merchant.becomeRogue(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0, helper.getLevel().getRandom());
			merchant.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), true);
			merchant.setTitle(NpcTitle.IMMORTAL_MERCHANT);
			for (int n = 0; n < 8; n++) {
				CultivatorTrades.Goods masters = fruitIn(CultivatorTrades.goodsFor(master, RandomSource.create(n)));
				int age = masters == null ? 0 : CultivationFruitItem.age(masters.item());
				helper.assertTrue(age >= 400 && age <= 900, "A Sect Master's fruit is centuries old (" + age + ")");
				helper.assertTrue(masters.price() == (int) Math.ceil(TalkSystem.worth(masters.item()) * 1.5), "...priced by its worth, as a herb is");
				CultivatorTrades.Goods merchants = fruitIn(CultivatorTrades.goodsFor(merchant, RandomSource.create(n)));
				int old = merchants == null ? 0 : CultivationFruitItem.age(merchants.item());
				helper.assertTrue(old >= 5000 && old <= 10_000, "An immortal merchant's as old as its herbs (" + old + ")");
			}
		} finally {
			master.discard();
			merchant.discard();
		}
		helper.succeed();
	}

	private static CultivatorTrades.Goods fruitIn(List<CultivatorTrades.Goods> goods) {
		for (CultivatorTrades.Goods g : goods) if (g.item().is(ModItems.CULTIVATION_FRUIT)) return g;
		return null;
	}
}
