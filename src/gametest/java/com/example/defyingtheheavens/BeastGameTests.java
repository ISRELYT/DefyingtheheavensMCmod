package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

/**
 * The wild demon beasts (BeastKind, DemonBeast, BeastSpawner) and their demon cores (DemonCoreItem): every kind in its tier's ranks up to
 * Core Formation Grand Perfection, as strong as its rank and larger for it, its rank kept; territorial, but not toward those far above
 * it; their cores by rank, a demon cultivator's always, a demon player's only to another player; and their lands.
 */
public class BeastGameTests implements FabricGameTest {
	private static final int CF_GP = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION);

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyKindKeepsToItsTierAndHasItsLands(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7);
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		int highest = -1;
		for (BeastKind kind : BeastKind.values()) {
			helper.assertTrue(DemonBeast.of(ModEntities.beast(kind)) == kind, "Its type is its own (" + kind.getId() + ")");
			helper.assertTrue(BeastKind.byId(kind.getId()) == kind && ModItems.beastEgg(kind) != null, "Found by its id, with an egg (" + kind.getId() + ")");
			helper.assertTrue(biomes.getTag(kind.lands).map(tag -> tag.size() > 0).orElse(false), "It has lands (" + kind.getId() + ")");
			for (boolean upper : new boolean[] {false, true}) {
				for (int i = 0; i < 300; i++) {
					int rank = DemonBeast.rollRank(kind, random, upper);
					helper.assertTrue(rank >= kind.tier.minRank && rank <= kind.tier.maxRank, "Within its tier's ranks (" + kind.getId() + " " + rank + ")");
					highest = Math.max(highest, rank);
				}
			}
		}
		helper.assertTrue(highest == CF_GP, "The strongest stand at Core Formation Grand Perfection, none higher (" + highest + ")");
		helper.assertTrue(BeastKind.Tier.COMMON.maxRank < PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY)
				&& BeastKind.Tier.RARE.maxRank == CF_GP, "The common are of Qi Refining; the rare reach the top of Core Formation");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBeastIsAsStrongAsItsRankAndKeepsIt(GameTestHelper helper) {
		DemonBeast low = helper.spawn(ModEntities.beast(BeastKind.BLOODFANG_TIGER), new BlockPos(1, 2, 1));
		DemonBeast high = helper.spawn(ModEntities.beast(BeastKind.BLOODFANG_TIGER), new BlockPos(3, 2, 1));
		CultivatorNpc man = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(5, 2, 1));
		try {
			for (var e : List.of(low, high, man)) e.setNoAi(true);
			low.setRank(BeastKind.Tier.UNCOMMON.minRank);
			high.setRank(BeastKind.Tier.UNCOMMON.maxRank);
			man.setRank(BeastKind.Tier.UNCOMMON.maxRank);
			helper.assertTrue(high.getCultivationRealm() == Realm.FOUNDATION_BUILDING && high.getCultivationStage() == Stage.GRAND_PERFECTION,
					"Its rank its realm and stage");
			helper.assertTrue(high.getMaxHealth() > low.getMaxHealth() + 4 && high.getHealth() == high.getMaxHealth(),
					"Stronger for its rank, and whole (" + low.getMaxHealth() + ", " + high.getMaxHealth() + ")");
			double ratio = high.getMaxHealth() / man.getMaxHealth();
			helper.assertTrue(Math.abs(ratio - BeastKind.BLOODFANG_TIGER.healthScale) < 0.05, "A cultivator's health of its rank, by its kind's (" + ratio + ")");
			helper.assertTrue(high.getAttributeValue(Attributes.ATTACK_DAMAGE) >= man.getAttributeValue(Attributes.ATTACK_DAMAGE) - 0.01, "A cultivator's blows");
			helper.assertTrue(high.getBbWidth() > low.getBbWidth() * 1.3 && high.scale() > low.scale(), "Larger the higher it stands");
			high.setRank(99);
			helper.assertTrue(high.getRank() == BeastKind.Tier.UNCOMMON.maxRank, "Held to its tier");
			high.hurt(helper.getLevel().damageSources().generic(), 5);
			float hurt = high.getHealth();
			CompoundTag saved = new CompoundTag();
			high.saveWithoutId(saved);
			DemonBeast again = helper.spawn(ModEntities.beast(BeastKind.BLOODFANG_TIGER), new BlockPos(1, 2, 3));
			again.setNoAi(true);
			again.load(saved);
			helper.assertTrue(again.getRank() == high.getRank() && Math.abs(again.getHealth() - hurt) < 0.01, "Its rank and wounds kept (" + again.getHealth() + ")");
			again.discard();
		} finally {
			low.discard();
			high.discard();
			man.discard();
		}
		helper.succeed();
	}

	private static ServerPlayer player(GameTestHelper helper, Bloodline race, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "test-beast")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		Bloodlines.set(p.server, p.getUUID(), race, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setMortal(false);
		c.setState(realm, stage, 0);
		if (race.isBeast()) DemonPath.born(p);
		CultivationManager.refresh(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static void gone(ServerPlayer p) {
		Bloodlines.forget(p.server, p.getUUID());
		p.server.getPlayerList().remove(p);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void beastsAreTerritorialButKnowTheirBetters(GameTestHelper helper) {
		DemonBeast wolf = helper.spawn(ModEntities.beast(BeastKind.SHADOW_WOLF), new BlockPos(1, 2, 1));
		DemonBeast turtle = helper.spawn(ModEntities.beast(BeastKind.ROCK_SHELL_TURTLE), new BlockPos(3, 2, 1));
		ServerPlayer weak = player(helper, Bloodline.HUMAN, Realm.QI_REFINING, Stage.LATE), strong = player(helper, Bloodline.HUMAN, Realm.FOUNDATION_BUILDING, Stage.MID);
		try {
			wolf.setNoAi(true);
			turtle.setNoAi(true);
			wolf.setRank(1);
			turtle.setRank(1);
			helper.assertTrue(wolf.hunts(weak), "A wolf goes for one near its own strength");
			helper.assertTrue(!wolf.hunts(strong) && wolf.farAbove(strong), "Not for one a realm and more above it");
			helper.assertTrue(!turtle.hunts(weak), "A turtle only fights back");
			helper.assertTrue(!wolf.hunts(turtle), "Nor one beast another");
			helper.assertTrue(wolf.isPreventingPlayerRest(weak) && !turtle.isPreventingPlayerRest(weak), "Only the hunters keep you from sleep");
		} finally {
			wolf.discard();
			turtle.discard();
			gone(weak);
			gone(strong);
		}
		helper.succeed();
	}

	private static List<ItemEntity> cores(GameTestHelper helper) {
		AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, item -> item.getItem().is(ModItems.DEMON_CORE));
	}

	private static void clearItems(GameTestHelper helper) {
		AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(12);
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box)) item.discard();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void demonsLeaveTheirCores(GameTestHelper helper) {
		clearItems(helper);
		// A Core Formation beast always leaves its own.
		DemonBeast rhino = helper.spawn(ModEntities.beast(BeastKind.HORNED_THUNDER_RHINO), new BlockPos(2, 2, 2));
		rhino.setNoAi(true);
		rhino.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
		rhino.kill();
		List<ItemEntity> left = cores(helper);
		helper.assertTrue(left.size() == 1, "A Core Formation beast leaves its core (" + left.size() + ")");
		ItemStack core = left.get(0).getItem();
		helper.assertTrue(DemonCoreItem.rank(core) == rhino.getRank() && DemonCoreItem.element(core) == QiElement.EARTH && DemonCoreItem.realm(core) == Realm.CORE_FORMATION
				&& DemonCoreItem.look(core) == 2, "Of its rank, its element, looking its realm");
		helper.assertTrue(Math.abs(DemonBeast.CORE_CHANCE[0] - 1 / 3f) < 1e-4 && Math.abs(DemonBeast.CORE_CHANCE[1] - 2 / 3f) < 1e-4 && DemonBeast.CORE_CHANCE[2] == 1f,
				"A Qi Refining beast's a third of the time, a Foundation Building one's two in three");
		clearItems(helper);
		// A demon cultivator always; a man never.
		CultivatorNpc fox = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2)), man = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(4, 2, 2));
		fox.setNoAi(true);
		man.setNoAi(true);
		fox.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY));
		fox.setRace(Bloodline.FOX, 0);
		man.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY));
		man.kill();
		helper.assertTrue(cores(helper).isEmpty(), "A man has no demon core");
		fox.kill();
		left = cores(helper);
		helper.assertTrue(left.size() == 1 && DemonCoreItem.realm(left.get(0).getItem()) == Realm.NASCENT_SOUL
				&& DemonCoreItem.element(left.get(0).getItem()) == QiElement.FIRE, "A demon cultivator's, above Core Formation too, its race's element");
		clearItems(helper);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonPlayersCoreIsOnlyAnotherPlayersSpoils(GameTestHelper helper) {
		clearItems(helper);
		ServerPlayer tiger = player(helper, Bloodline.TIGER, Realm.QI_REFINING, Stage.MID), man = player(helper, Bloodline.HUMAN, Realm.QI_REFINING, Stage.MID);
		try {
			ServerLevel level = helper.getLevel();
			DemonCoreItem.onPlayerDeath(tiger, level.damageSources().generic());
			DemonCoreItem.onPlayerDeath(tiger, level.damageSources().playerAttack(tiger));
			helper.assertTrue(cores(helper).isEmpty(), "Not by a fall or one's own hand");
			DemonCoreItem.onPlayerDeath(man, level.damageSources().playerAttack(tiger));
			helper.assertTrue(cores(helper).isEmpty(), "A man has none");
			int rank = DemonPath.rank(CultivationManager.get(tiger));
			DemonCoreItem.onPlayerDeath(tiger, level.damageSources().playerAttack(man));
			List<ItemEntity> left = cores(helper);
			helper.assertTrue(left.size() == 1 && DemonCoreItem.rank(left.get(0).getItem()) == rank && DemonCoreItem.element(left.get(0).getItem()) == QiElement.METAL,
					"Slain by another player, a demon leaves its core, of its Demon Path's rank");
			helper.assertTrue(DemonPath.rank(CultivationManager.get(tiger)) == rank, "And keeps its cultivation");
			// Struck by another, then finished by a fall: still theirs (the kill's credit, the bug sweep of 2026-10-23); never a machine's.
			clearItems(helper);
			tiger.setLastHurtByPlayer(man);
			DemonCoreItem.onPlayerDeath(tiger, level.damageSources().fall());
			helper.assertTrue(cores(helper).size() == 1, "Struck by another player and finished by a fall, it leaves its core");
			clearItems(helper);
			tiger.setLastHurtByPlayer(null);
			ServerPlayer machine = net.fabricmc.fabric.api.entity.FakePlayer.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "machine"));
			DemonCoreItem.onPlayerDeath(tiger, level.damageSources().playerAttack(machine));
			helper.assertTrue(cores(helper).isEmpty(), "Not to a machine's player (a FakePlayer)");
		} finally {
			clearItems(helper);
			gone(tiger);
			gone(man);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachLandHasItsOwnBeasts(GameTestHelper helper) {
		var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
		Holder<Biome> savanna = biomes.getHolderOrThrow(Biomes.SAVANNA), ocean = biomes.getHolderOrThrow(Biomes.DEEP_OCEAN), deepDark = biomes.getHolderOrThrow(Biomes.DEEP_DARK);
		RandomSource random = RandomSource.create(11);
		int rareLow = 0, rareHigh = 0;
		for (int i = 0; i < 4000; i++) {
			BeastKind low = BeastSpawner.pick(savanna, false, random), high = BeastSpawner.pick(savanna, true, random);
			helper.assertTrue(low != null && savanna.is(low.lands), "Only a land's own kinds (" + low + ")");
			if (low.tier == BeastKind.Tier.RARE) rareLow++;
			if (high.tier == BeastKind.Tier.RARE) rareHigh++;
		}
		helper.assertTrue(rareLow > 0 && rareHigh > rareLow * 2, "The rare are rare, the less so in the Upper Realm (" + rareLow + ", " + rareHigh + ")");
		helper.assertTrue(BeastSpawner.pick(ocean, false, random) == BeastKind.FLOOD_DRAGON, "The deep sea's dragon");
		helper.assertTrue(BeastSpawner.pick(deepDark, false, random) == null, "None where no beast keeps");
		helper.succeed();
	}

	/** Terralith's surface biomes (2.5.4; its cave biomes lie under the beasts' and the dens' ground). */
	private static final List<String> TERRALITH = List.of("alpha_islands", "alpha_islands_winter", "alpine_grove", "alpine_highlands", "amethyst_canyon",
			"amethyst_rainforest", "ancient_sands", "arid_highlands", "ashen_savanna", "basalt_cliffs", "birch_taiga", "blooming_plateau", "blooming_valley",
			"brushland", "bryce_canyon", "caldera", "cloud_forest", "cold_shrubland", "desert_canyon", "desert_oasis", "desert_spires", "emerald_peaks",
			"forested_highlands", "fractured_savanna", "frozen_cliffs", "glacial_chasm", "granite_cliffs", "gravel_beach", "gravel_desert", "haze_mountain",
			"highlands", "hot_shrubland", "ice_marsh", "jungle_mountains", "lavender_forest", "lavender_valley", "lush_desert", "lush_valley", "mirage_isles",
			"moonlight_grove", "moonlight_valley", "mountain_steppe", "orchid_swamp", "painted_mountains", "red_oasis", "rocky_jungle", "rocky_mountains",
			"rocky_shrubland", "sakura_grove", "sakura_valley", "sandstone_valley", "savanna_badlands", "savanna_slopes", "scarlet_mountains", "shield",
			"shield_clearing", "shrubland", "siberian_grove", "siberian_taiga", "skylands", "skylands_autumn", "skylands_spring", "skylands_summer",
			"skylands_winter", "snowy_badlands", "snowy_cherry_grove", "snowy_maple_forest", "snowy_shield", "steppe", "stony_spires", "temperate_highlands",
			"tropical_jungle", "valley_clearing", "volcanic_crater", "volcanic_peaks", "warm_river", "warped_mesa", "white_cliffs", "white_mesa",
			"windswept_spires", "wintry_forest", "wintry_lowlands", "yellowstone", "yosemite_cliffs", "yosemite_lowlands");

	/**
	 * Terralith's lands in the beasts' and the clans' (the user's ask, 2026-10-23): every one of its surface biomes some beasts' land, most
	 * of them a race's, none two races'; every entry one of its real biomes, and optional, so a world without Terralith loads as it did
	 * (this one has none: a required entry would have failed the tags' loading).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void terralithsLandsHaveTheirBeastsAndClans(GameTestHelper helper) {
		java.util.Map<String, Integer> beasts = new java.util.HashMap<>();
		java.util.Map<String, String> races = new java.util.HashMap<>();
		for (BeastKind kind : BeastKind.values()) {
			for (String biome : terralithIn(helper, "beast/" + kind.getId())) beasts.merge(biome, 1, Integer::sum);
		}
		for (Bloodline race : Bloodline.values()) {
			if (!race.isBeast()) continue;
			for (String biome : terralithIn(helper, "clan/" + race.getId())) {
				String before = races.put(biome, race.getId());
				helper.assertTrue(before == null, "One race's land at most (" + biome + ": " + before + ", " + race.getId() + ")");
			}
		}
		for (String biome : TERRALITH) helper.assertTrue(beasts.containsKey(biome), "Beasts in every Terralith land (" + biome + ")");
		helper.assertTrue(TERRALITH.containsAll(beasts.keySet()) && TERRALITH.containsAll(races.keySet()), "Only Terralith's real biomes named");
		helper.assertTrue(races.size() >= 50, "Most of its lands a clan's (" + races.size() + ")");
		for (Bloodline race : Bloodline.values()) {
			if (race.isBeast()) helper.assertTrue(races.containsValue(race.getId()), "Each race some of its lands (" + race.getId() + ")");
		}
		helper.succeed();
	}

	/** The Terralith biomes tag {@code tag} names (each an optional entry, else the test fails). */
	private static List<String> terralithIn(GameTestHelper helper, String tag) {
		var location = new net.minecraft.resources.ResourceLocation(DefyingTheHeavens.MOD_ID, "tags/worldgen/biome/" + tag + ".json");
		var resource = helper.getLevel().getServer().getResourceManager().getResource(location);
		helper.assertTrue(resource.isPresent(), "The tag " + tag + " is there");
		List<String> found = new java.util.ArrayList<>();
		try (java.io.Reader reader = resource.get().openAsReader()) {
			for (com.google.gson.JsonElement value : com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("values")) {
				if (value.isJsonPrimitive()) {
					helper.assertTrue(!value.getAsString().startsWith("terralith:"), "A Terralith biome only as an optional entry (" + tag + ": " + value + ")");
					continue;
				}
				com.google.gson.JsonObject entry = value.getAsJsonObject();
				String id = entry.get("id").getAsString();
				helper.assertTrue(id.startsWith("terralith:") && entry.has("required") && !entry.get("required").getAsBoolean(),
						"Optional Terralith entries only (" + tag + ": " + entry + ")");
				found.add(id.substring("terralith:".length()));
			}
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		return found;
	}
}
