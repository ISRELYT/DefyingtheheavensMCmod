package com.example.defyingtheheavens;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.UUID;

/**
 * The user's calls on the third sweep's suggestions (2026-10-25), held to: a great beast judged against its caster (more than three times
 * its whole health; the old world's great beasts always); a guest pass sold made out to its buyer, letting them alone through; one caster's
 * wounds in the land waiting to be cut held to its share (a burst core's whole); a demon clan's chests now and then keeping its race's
 * blood, of its realms, never a sect of men's, and a sect library's slips of the same realms in each world; and nothing of True Immortal's
 * to be had in the worlds (Realm#taughtInTheWorld).
 */
public class SuggestionPicksGameTests implements FabricGameTest {
	/** A survival player at {@code realm}'s {@code stage}, out of the player list. */
	private static ServerPlayer at(GameTestHelper helper, Realm realm, Stage stage, String name) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(p).setState(realm, stage, 0);
		CultivationManager.refresh(p);
		return p;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGreatBeastIsJudgedAgainstItsCaster(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer dao = at(helper, Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION, "picks-dao"), weak = at(helper, Realm.QI_REFINING, Stage.EARLY, "picks-weak");
		Husk husk = EntityType.HUSK.create(level);
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dao.getMaxHealth() * 2);
		helper.assertTrue(dao.getMaxHealth() >= 300, "A Dao Integration caster's health is past what made a great beast before (" + dao.getMaxHealth() + ")");
		helper.assertTrue(!HeavenSpells.greatBeast(dao, husk) && !HeavenSpells.greatBeast(dao, at(helper, Realm.DAO_INTEGRATION, Stage.EARLY, "picks-dao-peer")),
				"One of twice its health, and another of its realm, are no great beasts to it (" + husk.getMaxHealth() + ")");
		helper.assertTrue(HeavenSpells.greatBeast(weak, husk), "...the first is to a Qi Refining caster");
		husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(dao.getMaxHealth() * (HeavenSpells.GREAT_BEYOND + 1));
		helper.assertTrue(HeavenSpells.greatBeast(dao, husk), "...and one of more than three times its health is to it too");
		Warden warden = EntityType.WARDEN.create(level);
		helper.assertTrue(HeavenSpells.greatBeast(at(helper, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, "picks-immortal"), warden),
				"The old world's great beasts are great to every caster, a True Immortal too");
		husk.discard();
		warden.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGuestPassIsSoldMadeOutToItsBuyerAndLetsThemAloneThrough(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Passing Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		SectData.get(level.getServer()).put(sect);
		CultivatorNpc seller = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		Formation formation = Formations.create(level, UUID.randomUUID(), Formation.Kind.CORE, helper.absolutePos(new BlockPos(6, 2, 6)), Formations.MIN_RADIUS);
		Formations.update(level, formation, PlayerCultivation.LAST_RANK, null, sect.id);
		ServerPlayer buyer = at(helper, Realm.FOUR_AXIS, Stage.EARLY, "picks-buyer"), other = at(helper, Realm.FOUR_AXIS, Stage.EARLY, "picks-other");
		try {
			seller.setNoAi(true);
			seller.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			seller.joinSect(sect, sect.members.size());
			sect.members.put(seller.getUUID(), new Sect.Member(seller.getUUID(), seller.getRank(), sect.members.size()));
			ItemStack pass = ItemStack.EMPTY;
			for (MerchantOffer offer : CultivatorTrades.offersFor(seller, buyer, new HashMap<>(), new IdentityHashMap<>())) {
				if (offer.getResult().is(ModItems.SECT_PASS)) pass = offer.getResult().copy();
			}
			helper.assertTrue(!pass.isEmpty() && buyer.getUUID().equals(SectPassItem.holder(pass)), "Its people sell a guest pass made out to the one buying it");
			buyer.getInventory().add(pass.copy());
			helper.assertTrue(formation.allows(buyer), "Carried by its buyer, the barrier lets them through");
			other.getInventory().add(pass.copy());
			helper.assertTrue(!formation.allows(other), "...not one it is handed to");
			other.getInventory().add(SectPassItem.of(sect.id, sect.name, level.getGameTime() + SectPassItem.LASTS));
			helper.assertTrue(formation.allows(other), "One made out to no one (a command's) lets anyone through");
			helper.succeed();
		} finally {
			Formations.dissolve(level, formation.id);
			sect.extinct = true;
			seller.discard();
		}
	}

	/** In a batch of its own: no other test's wounds are underway while it measures, and it lets go of its own before any is cut. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "suggestion_picks_wounds")
	public void oneCastersWoundsWaitingAreHeldToItsShare(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer spammer = at(helper, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, "picks-spammer"), other = at(helper, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, "picks-other-caster");
		Vec3 at = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
		try {
			Devastation.clear();
			Devastation.crater(spammer, level, at, 40, 20, Devastation.Floor.NONE);
			int one = Devastation.pending();
			helper.assertTrue(one > 20_000 && one < Devastation.MOST_WAITING, "One great crater is queued whole (" + one + ")");
			for (int i = 0; i < 9; i++) Devastation.crater(spammer, level, at, 40, 20, Devastation.Floor.NONE);
			int spammed = Devastation.pending();
			helper.assertTrue(spammed == Devastation.MOST_WAITING, "Ten of them, one caster's: held to its share (" + spammed + " of " + 10L * one + ")");
			Devastation.crater(other, level, at, 40, 20, Devastation.Floor.NONE);
			helper.assertTrue(Devastation.pending() == spammed + one, "...another caster's still queued whole");
			Devastation.crater(spammer, level, at, 40, 20, Devastation.Floor.NONE, true);
			helper.assertTrue(Devastation.pending() > spammed + one, "...and a burst golden core's goes whole, its share spent or not");
			helper.succeed();
		} finally {
			Devastation.clear();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonClansChestsKeepItsBloodNowAndThen(GameTestHelper helper) {
		RandomSource random = RandomSource.create(7);
		int lower = 0, upper = 0, deep = 0, lowerBeyond = 0, upperBelow = 0;
		for (int i = 0; i < 20_000; i++) {
			ItemStack low = DemonBlood.hoarded(Bloodline.TIGER, false, SectPiece.TREASURY_LOOT, random);
			if (!low.isEmpty()) {
				lower++;
				if (DemonBloodItem.realm(low).ordinal() > Realm.CORE_FORMATION.ordinal()) lowerBeyond++;
			}
			ItemStack high = DemonBlood.hoarded(Bloodline.TIGER, true, SectPiece.TREASURY_LOOT, random);
			if (!high.isEmpty()) {
				upper++;
				Realm realm = DemonBloodItem.realm(high);
				if (realm.ordinal() < Realm.CORE_FORMATION.ordinal()) upperBelow++;
				if (realm.ordinal() > Realm.SACRED_VESSEL.ordinal()) deep++;
				helper.assertTrue(realm.taughtInTheWorld() && DemonBloodItem.race(high) == Bloodline.TIGER, "Its own race's, never a True Immortal's (" + realm + ")");
			}
			helper.assertTrue(DemonBlood.hoarded(Bloodline.HUMAN, true, SectPiece.TREASURY_LOOT, random).isEmpty()
					&& DemonBlood.hoarded(Bloodline.FOX, true, SectPiece.LIBRARY_LOOT, random).isEmpty(), "A sect of men's chests keep none, nor a library");
		}
		helper.assertTrue(lower > 1800 && lower < 3000 && upper > 1800 && upper < 3000, "About one hoard chest in eight keeps a phial (" + lower + ", " + upper + " in 20,000)");
		helper.assertTrue(lowerBeyond == 0 && upperBelow == 0, "A lower realm clan's no deeper than Core Formation's; an Upper Realm one's none below Core Formation's");
		helper.assertTrue(deep > 0 && deep < upper / 10, "Entering Nirvana's now and then in the Upper Realm, seldom (" + deep + " of " + upper + ")");
		helper.succeed();
	}

	/**
	 * A sect library's slips are its world's, of the realms a clan's chests there keep blood of, as often (the user, 2026-10-25: "you can
	 * get EN phials from clans but you can't get EN methods from sects"). The game-test server has no Upper Realm: there the table's Upper
	 * Realm pool is read from the table itself (in game it is rolled there, as here in the Overworld).
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void aSectsLibraryKeepsItsWorldsSlipsAsAClansHoardItsBlood(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		int[] lower = libraryRealms(server, helper.getLevel(), helper.absoluteVec(new Vec3(1, 2, 1)));
		helper.assertTrue(alike(lower, DemonBlood.LOWER_REALMS_BLOOD), "A library of the lower realms keeps the slips of the realms a clan's hoard there keeps blood of, as often ("
				+ Arrays.toString(lower) + ")");
		ServerLevel upperRealm = server.getLevel(ModDimensions.UPPER_REALM);
		int[] upper = upperRealm != null ? libraryRealms(server, upperRealm, new Vec3(0, 120, 0)) : upperRealmSlipWeights(server);
		helper.assertTrue(alike(upper, DemonBlood.UPPER_REALM_BLOOD) && upper[Realm.ENTERING_NIRVANA.ordinal()] > 0,
				"...and one of the Upper Realm the Upper Realm's, up to Entering Nirvana's (" + Arrays.toString(upper) + ")");
		helper.succeed();
	}

	/** The slips 4000 of a sect library's chests opened in {@code level} keep, counted by realm. */
	private static int[] libraryRealms(MinecraftServer server, ServerLevel level, Vec3 at) {
		LootTable library = server.getLootData().getLootTable(SectPiece.LIBRARY_LOOT);
		int[] realms = new int[Realm.values().length];
		for (int i = 0; i < 4000; i++) {
			for (ItemStack stack : library.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, at).create(LootContextParamSets.CHEST))) {
				if (stack.is(ModItems.JADE_SLIP)) realms[JadeSlipItem.method(stack).realm().ordinal()] += stack.getCount();
			}
		}
		return realms;
	}

	/** The weights of the library table's Upper Realm slips (its pool kept only there), summed by realm, read from the table as loaded. */
	private static int[] upperRealmSlipWeights(MinecraftServer server) {
		int[] realms = new int[Realm.values().length];
		try (Reader reader = server.getResourceManager().getResourceOrThrow(DefyingTheHeavens.id("loot_tables/chests/sect_library.json")).openAsReader()) {
			for (JsonElement p : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("pools")) {
				JsonObject pool = p.getAsJsonObject();
				String when = String.valueOf(pool.get("conditions"));
				if (!when.contains("minecraft:location_check") || when.contains("minecraft:inverted") || !when.contains(ModDimensions.UPPER_REALM.location().toString())) continue;
				for (JsonElement e : pool.getAsJsonArray("entries")) {
					JsonObject entry = e.getAsJsonObject();
					String tag = entry.getAsJsonArray("functions").get(0).getAsJsonObject().get("tag").getAsString();
					realms[HeavenMethod.byId(TagParser.parseTag(tag).getString("HeavenMethod")).realm().ordinal()] += entry.get("weight").getAsInt();
				}
			}
		} catch (Exception unreadable) {
			throw new IllegalStateException("The library's loot table can't be read", unreadable);
		}
		return realms;
	}

	/** Whether {@code counts} by realm fall in the shares {@code weights} (from Qi Refining up) give them, within 2.5 points; none past them. */
	private static boolean alike(int[] counts, int[] weights) {
		int total = 0, sum = 0;
		for (int n : counts) total += n;
		for (int w : weights) sum += w;
		if (total == 0) return false;
		for (int i = 0; i < counts.length; i++) {
			double want = i < weights.length ? weights[i] / (double) sum : 0;
			if (want == 0 ? counts[i] > 0 : Math.abs(counts[i] / (double) total - want) > 0.025) return false;
		}
		return true;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void nothingOfTrueImmortalsIsToBeHadInTheWorlds(GameTestHelper helper) {
		RandomSource random = RandomSource.create(11);
		boolean heavenTi = false, ghostTi = false, heavenEn = false;
		for (int i = 0; i < 200_000; i++) {
			Realm stele = DaoSteleFeature.pickRealm(random, DaoSteleFeature.Lands.UPPER_REALM_RARE);
			heavenTi |= stele == Realm.TRUE_IMMORTAL;
			heavenEn |= stele == Realm.ENTERING_NIRVANA;
			ghostTi |= MethodSteleFeature.pick(random, true) == GhostMethod.NETHERWORLD_IMMORTAL_CANON;
		}
		helper.assertTrue(!heavenTi && heavenEn, "No Dao Stele carves a True Immortal method; Entering Nirvana's now and then");
		helper.assertTrue(!ghostTi && !GhostMethod.NETHERWORLD_IMMORTAL_CANON.taughtInTheWorld(), "...nor a Method Stele the ghost path's True Immortal foundation");
		helper.assertTrue(Realm.TRUE_IMMORTAL.atMostTaught() == Realm.ENTERING_NIRVANA && Realm.SACRED_VESSEL.atMostTaught() == Realm.SACRED_VESSEL,
				"A demon's blood beyond what the worlds hold leaves Entering Nirvana's");
		// A True Immortal rogue (none is born in the worlds; one made by command) sells no slip of its method.
		CultivatorNpc rogue = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		ServerPlayer player = at(helper, Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, "picks-shopper");
		try {
			rogue.setNoAi(true);
			rogue.setTitle(NpcTitle.ROGUE);
			rogue.setRank(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
			rogue.changeRegard(player, Regard.TRUSTED);
			for (MerchantOffer offer : CultivatorTrades.offersFor(rogue, player, new HashMap<>(), new IdentityHashMap<>())) {
				ItemStack sold = offer.getResult();
				helper.assertTrue(!sold.is(ModItems.JADE_SLIP) || JadeSlipItem.method(sold).realm().taughtInTheWorld(), "...no True Immortal's slip on sale (" + sold + ")");
			}
			helper.succeed();
		} finally {
			rogue.discard();
		}
	}
}
