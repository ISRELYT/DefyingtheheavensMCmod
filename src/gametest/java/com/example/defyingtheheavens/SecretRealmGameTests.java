package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * The secret realms (SecretRealms, 2026-10-31; the user's picks: a lowest realm and no upper limit; free flight for the vastly stronger): who
 * gets in and who flies freely; the Verdant Herb Valley's land from the world's seed; the law of its world inside it and its threefold qi; its
 * ledger mending what visitors broke and leaving the dead where they lie; its herbs ageing while it sleeps; its vaults sealed for the ancients;
 * the Gatekeeper matched to its challenger; Ancient Essence's point; the Departure Jade outside; its tales; the Pill Sage's Cauldron's fire;
 * the roll of those who come; and a whole opening, stirred, opened and closed ({@link SecretRealms#inTests}).
 */
public class SecretRealmGameTests implements FabricGameTest {
	private static final SecretRealm VALLEY = SecretRealm.VERDANT_HERB_VALLEY;

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLowestRealmGetsInAndNoOneIsTooStrong(GameTestHelper helper) {
		helper.assertTrue(!VALLEY.mayEnter(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID)), "Qi Refining Mid is turned back");
		helper.assertTrue(VALLEY.mayEnter(VALLEY.lowest()) && VALLEY.mayEnter(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE)), "Qi Refining Late gets in");
		helper.assertTrue(VALLEY.mayEnter(PlayerCultivation.rank(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION)), "...and so does anyone stronger: no upper limit");
		helper.assertTrue(!VALLEY.fliesFreely(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION)), "Core Formation is held near the ground");
		helper.assertTrue(VALLEY.fliesFreely(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY)),
				"Two whole realms past Foundation Building (Nascent Soul) flies freely");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theValleyIsLaidFromItsSeed(GameTestHelper helper) {
		HerbValley.Shape s = HerbValley.shape(helper.getLevel().getSeed());
		HerbValley.Column heart = HerbValley.column(s, 0, 0);
		helper.assertTrue(heart.kind() == HerbValley.Kind.ISLAND && heart.ground() == HerbValley.ISLAND_Y, "An island at its heart, level for the hall: " + heart);
		HerbValley.Column lake = HerbValley.column(s, 25, 0);
		helper.assertTrue(lake.wet() && lake.water() == HerbValley.WATER_Y, "A lake round it: " + lake);
		helper.assertTrue(HerbValley.column(s, 400, 0).kind() == HerbValley.Kind.NONE, "Nothing past its rim");
		helper.assertTrue(s.beds.size() == HerbValley.BEDS, "Its beds: " + s.beds.size());
		for (HerbValley.Bed bed : s.beds) {
			List<BlockPos> spots = HerbValley.herbSpots(s, bed);
			helper.assertTrue(!spots.isEmpty(), "Bed " + bed.index() + " has herbs");
			for (BlockPos spot : spots) {
				helper.assertTrue(HerbValley.column(s, spot.getX(), spot.getZ()).kind() == HerbValley.Kind.BED, "...each on its soil: " + spot);
			}
		}
		List<BlockPos> arrivals = HerbValley.arrivals(s);
		helper.assertTrue(arrivals.size() == HerbValley.PATHS, "An arrival terrace at each path's head");
		// The main way climbs from the lake's shore up the terraces a step at a time: walkable.
		int dx = (int) Math.round(Math.cos(s.pathTurn)), dz = (int) Math.round(Math.sin(s.pathTurn));
		int last = Integer.MIN_VALUE;
		for (int r = HerbValley.LAKE_R; r <= HerbValley.ARRIVAL_R; r++) {
			HerbValley.Column c = HerbValley.column(s, dx * r, dz * r);
			if (c.wet() || c.kind() == HerbValley.Kind.STREAM) continue;
			if (last != Integer.MIN_VALUE) helper.assertTrue(Math.abs(c.ground() - last) <= 1, "The path climbs a block at most a step, at " + r + ": " + last + " to " + c.ground());
			last = c.ground();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRealmKeepsItsWorldsLaw(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		helper.assertTrue(realms != null, "The secret realms' dimension is there");
		BlockPos c = VALLEY.centre();
		helper.assertTrue(SecretRealm.at(c.getX(), c.getZ()) == VALLEY, "Its slot is the valley's");
		helper.assertTrue(SecretRealms.lawOf(realms, c) == Level.OVERWORLD, "The Overworld's law holds in it (its Seal's world)");
		helper.assertTrue(Math.abs(SecretRealms.qiAt(realms, c) - SecretRealms.QI) < 1.0e-9, "Its qi three times the Overworld's");
		helper.assertTrue(SecretRealms.lawOf(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)) == helper.getLevel().dimension(), "Elsewhere, the level's own");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLedgerMendsWhatVisitorsBreakAndLeavesTheDead(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		HerbValley.Shape s = HerbValley.shape(realms.getSeed());
		BlockPos c = VALLEY.centre();
		BlockPos broken = c.offset(60, 0, 3).atY(HerbValley.column(s, 60, 3).ground());
		BlockPos lies = c.offset(61, 0, 5).atY(HerbValley.column(s, 61, 5).ground() + 1);
		realms.getChunk(broken.getX() >> 4, broken.getZ() >> 4);
		RealmLedger ledger = RealmLedger.get(realms.getServer());
		BlockState was = realms.getBlockState(broken);
		try {
			realms.setBlock(broken, Blocks.AIR.defaultBlockState(), 3); // a visitor's hands
			helper.assertTrue(ledger.original(broken) == was, "The ledger keeps what stood there: " + ledger.original(broken));
			realms.setBlock(lies, ModBlocks.CORPSE.defaultBlockState(), 3); // one who died there
			RealmLedger.Mending mending = ledger.mending(VALLEY);
			while (!mending.done()) mending.step(realms);
			helper.assertTrue(realms.getBlockState(broken) == was, "Mended as it first stood");
			helper.assertTrue(realms.getBlockState(lies).is(ModBlocks.CORPSE), "The dead stay where they lie");
			helper.assertTrue(ledger.original(broken) == null, "...and what is mended is struck off");
			helper.succeed();
		} finally {
			RealmLedger.quietly(() -> {
				realms.setBlock(lies, Blocks.AIR.defaultBlockState(), 3);
				realms.setBlock(broken, was, 3);
			});
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void unpickedHerbsAgeWhileTheValleySleeps(GameTestHelper helper) {
		ServerLevel realms = helper.getLevel().getServer().getLevel(ModDimensions.SECRET_REALM);
		HerbValley.Shape s = HerbValley.shape(realms.getSeed());
		BlockPos local = HerbValley.herbSpots(s, s.beds.get(0)).get(0);
		BlockPos at = VALLEY.centre().offset(local.getX(), 0, local.getZ()).atY(local.getY());
		int bonus = VALLEY.years() * HerbValleyWorks.YEARS_A_YEAR_SHUT;
		try {
			RealmLedger.quietly(() -> {
				realms.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
				HerbValleyWorks.renewHerb(realms, at, ModBlocks.GINSENG, 200, bonus, Blocks.PODZOL);
			});
			GinsengBlockEntity herb = (GinsengBlockEntity) realms.getBlockEntity(at);
			helper.assertTrue(herb != null && herb.age() == 200, "Grown again where it was picked, of its bed's age");
			RealmLedger.quietly(() -> HerbValleyWorks.renewHerb(realms, at, ModBlocks.GINSENG, 999, bonus, Blocks.PODZOL));
			herb = (GinsengBlockEntity) realms.getBlockEntity(at);
			helper.assertTrue(herb.age() == 200 + bonus, "Left unpicked, older by a century a year the valley slept: " + herb.age());
			helper.succeed();
		} finally {
			RealmLedger.quietly(() -> realms.setBlock(at, Blocks.AIR.defaultBlockState(), 3));
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aVaultIsSealedAnewForTheAncients(GameTestHelper helper) {
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		ServerLevel level = helper.getLevel();
		try {
			level.setBlock(at, ModBlocks.storageFormation(StorageFormations.Tier.LOW).defaultBlockState(), 3);
			StorageFormationBlockEntity vault = (StorageFormationBlockEntity) level.getBlockEntity(at);
			vault.sealForAncients(level, List.of(new ItemStack(ModItems.LOW_SPIRIT_STONE, 5)));
			helper.assertTrue(StorageFormationBlockEntity.ANCIENTS.equals(vault.getOwner()) && !vault.isBroken(), "The ancients' own, its seal whole");
			vault.breakSeal();
			vault.sealForAncients(level, List.of(new ItemStack(ModItems.DEPARTURE_JADE, 1)));
			StorageSpaces.Space held = StorageSpaces.forServer(level.getServer()).existing(vault.spaceId());
			helper.assertTrue(!vault.isBroken() && held != null && held.entries().size() == 1 && held.count(new ItemStack(ModItems.DEPARTURE_JADE)) == 1,
					"Sealed anew, it holds only what was laid in it this time");
			helper.succeed();
		} finally {
			level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGatekeeperMeetsItsChallengerAsAnEqual(GameTestHelper helper) {
		RealmGuardian keeper = helper.spawnWithNoFreeWill(ModEntities.BRONZE_PUPPET, new BlockPos(1, 2, 1));
		RealmGuardian other = helper.spawnWithNoFreeWill(ModEntities.BRONZE_PUPPET, new BlockPos(3, 2, 1));
		CultivatorNpc strong = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 3));
		CultivatorNpc weak = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 3));
		try {
			strong.setRank(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.MID));
			weak.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE));
			keeper.setUp(VALLEY, VALLEY.guardianRank(Stage.EARLY), keeper.blockPosition(), -1, "gatekeeper", true);
			other.setUp(VALLEY, VALLEY.guardianRank(Stage.EARLY), other.blockPosition(), -1, "gatekeeper", true);
			keeper.challenge(strong);
			helper.assertTrue(keeper.getRank() == strong.sustainedRank() && !keeper.isDormant(), "It wakes at its challenger's strength: " + keeper.getRank());
			other.challenge(weak);
			helper.assertTrue(other.getRank() == VALLEY.guardianRank(Stage.EARLY), "Never weaker than the realm it guards");
			float health = keeper.getHealth();
			keeper.hurt(keeper.damageSources().mobAttack(weak), 5);
			helper.assertTrue(keeper.getHealth() == health, "Another's blows don't touch it while it duels");
			helper.succeed();
		} finally {
			keeper.discard();
			other.discard();
			strong.discard();
			weak.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ancientEssenceIsOneMorePointForGood(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.FOUNDATION_BUILDING, Stage.MID, 0);
		int before = StatPoints.earned(c);
		c.addEssencePoint();
		helper.assertTrue(StatPoints.earned(c) == before + 1, "One more point to spend");
		PlayerCultivation loaded = PlayerCultivation.load(c.save());
		helper.assertTrue(loaded.essencePoints() == 1 && StatPoints.earned(loaded) == before + 1, "...kept with the record");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDepartureJadeStirsOnlyInsideARealm(GameTestHelper helper) {
		ServerPlayer p = helper.makeMockServerPlayerInLevel();
		try {
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.DEPARTURE_JADE));
			InteractionResultHolder<ItemStack> used = ModItems.DEPARTURE_JADE.use(helper.getLevel(), p, InteractionHand.MAIN_HAND);
			helper.assertTrue(!used.getResult().consumesAction() && !p.isUsingItem(), "Outside a secret realm nothing stirs in it");
			helper.succeed();
		} finally {
			p.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRealmsTalesAreToldTheirTime(GameTestHelper helper) {
		long now = helper.getLevel().getGameTime();
		Rumours.Rumour stirs = new Rumours.Rumour("realm:" + VALLEY.getId() + ":stirs", Rumours.Kind.REALM, Level.OVERWORLD, BlockPos.ZERO, VALLEY.getId(), 0, now, "");
		Rumours.Rumour fallen = new Rumours.Rumour("realm:" + VALLEY.getId() + ":fallen:Someone", Rumours.Kind.REALM, Level.OVERWORLD, BlockPos.ZERO, VALLEY.getId(), 0, now, "Someone");
		Rumours.Rumour heir = new Rumours.Rumour("realm:" + VALLEY.getId() + ":heir", Rumours.Kind.REALM, Level.OVERWORLD, BlockPos.ZERO, VALLEY.getId(), 0, now, "Someone");
		helper.assertTrue(stirs.current(now) && !stirs.current(now + Rumours.STIRRING_REMEMBERED + 1), "A stirring is talked of two days");
		helper.assertTrue(fallen.current(now + Rumours.STIRRING_REMEMBERED) && !fallen.current(now + Rumours.FALLEN_REMEMBERED + 1), "The fallen, three");
		helper.assertTrue(heir.current(now + 100 * 24_000L), "An inheritor, for good");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thePillSagesCauldronBurnsWithItsOwnFire(GameTestHelper helper) {
		BlockPos sage = helper.absolutePos(new BlockPos(1, 2, 1)), plain = helper.absolutePos(new BlockPos(3, 2, 1));
		ServerLevel level = helper.getLevel();
		try {
			level.setBlock(sage, ModBlocks.PILL_SAGE_CAULDRON.defaultBlockState(), 3);
			level.setBlock(plain, ModBlocks.ALCHEMY_CAULDRON.defaultBlockState(), 3);
			helper.assertTrue(AlchemyCauldronBlock.isHeated(level, sage) && !AlchemyCauldronBlock.isHeated(level, plain), "Its fire is its own; a plain one needs one");
			helper.assertTrue(((AlchemyCauldronBlockEntity) level.getBlockEntity(sage)).isSages(), "Known as the Pill Sage's");
			helper.succeed();
		} finally {
			level.setBlock(sage, Blocks.AIR.defaultBlockState(), 3);
			level.setBlock(plain, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thoseWhoComeAreOfTheRealmsRanks(GameTestHelper helper) {
		SecretRealms.Record r = new SecretRealms.Record(VALLEY);
		SecretRealmRivals.stir(helper.getLevel().getServer(), SecretRealms.Data.get(helper.getLevel().getServer()), r, helper.getLevel().getGameTime());
		ListTag roll = r.rivals.getList("Roll", Tag.TAG_COMPOUND);
		helper.assertTrue(roll.size() >= 2 && roll.size() <= SecretRealmRivals.MOST, "Some come, never more than " + SecretRealmRivals.MOST + ": " + roll.size());
		int high = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID);
		for (Tag t : roll) {
			int rank = ((CompoundTag) t).getInt("Rank");
			helper.assertTrue(rank >= VALLEY.lowest() && rank <= high, "Each of the realm's ranks: " + rank);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyThoseWhoPassedArePassed(GameTestHelper helper) {
		SecretRealms.Record r = new SecretRealms.Record(VALLEY);
		UUID passed = UUID.randomUUID(), not = UUID.randomUUID();
		ListTag list = new ListTag();
		list.add(StringTag.valueOf(passed.toString()));
		r.works.put("Passed", list);
		r.openedAt = 0;
		helper.assertTrue(HerbValleyWorks.passed(r, passed) && !HerbValleyWorks.passed(r, not), "The barrier knows who beat the Gatekeeper");
		r.openedAt = -1;
		helper.assertTrue(!HerbValleyWorks.passed(r, passed), "...for this opening only");
		helper.succeed();
	}

	/** A whole opening, out of turn: its Seal built, its land laid and readied, its gate open; then shut, its next opening set. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "secret_realm_cycle", timeoutTicks = 400)
	public void aRealmOpensAndCloses(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		SecretRealms.inTests = true;
		try {
			SecretRealms.Data data = SecretRealms.Data.get(server);
			SecretRealms.Record r = data.of(VALLEY);
			if (r.openedAt >= 0) SecretRealms.closeNow(server, VALLEY);
			SecretRealms.openNow(server, VALLEY);
			helper.assertTrue(r.openedAt >= 0 && r.laid && r.seal != null && r.sealBuilt, "Open, its land laid and its Seal built");
			ServerLevel home = server.getLevel(VALLEY.home());
			BlockState gate = home.getBlockState(r.seal.above());
			helper.assertTrue(gate.is(ModBlocks.SECRET_REALM_GATE) && gate.getValue(SecretRealmGateBlock.PHASE) == SecretRealmGateBlock.Phase.OPEN, "Its gate open: " + gate);
			ServerLevel realms = server.getLevel(ModDimensions.SECRET_REALM);
			HerbValley.Shape s = HerbValley.shape(realms.getSeed());
			BlockPos cauldron = HerbValleyWorks.trialCauldron(s, VALLEY.centre());
			helper.assertTrue(realms.getBlockState(cauldron).is(ModBlocks.ALCHEMY_CAULDRON), "The master's cauldron in his hall");
			long now = server.overworld().getGameTime();
			SecretRealms.closeNow(server, VALLEY);
			gate = home.getBlockState(r.seal.above());
			helper.assertTrue(r.openedAt < 0 && r.nextOpen > now && gate.getValue(SecretRealmGateBlock.PHASE) == SecretRealmGateBlock.Phase.SEALED,
					"Shut, sealed again, its next opening ahead");
			helper.succeed();
		} finally {
			SecretRealms.inTests = false;
		}
	}
}
