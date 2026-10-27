package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

/** Sects, NPC cultivators, formations, alignment and clothing. */
public class SectGameTests implements FabricGameTest {
	/**
	 * Before each run of the shared batch, and of the barrier's own (2026-10-26): no formation left by an earlier run stands over this run's
	 * ground. The test world keeps them, every one rising again as the server starts, and one whose shell crossed a test's cell now and
	 * then kept it barred, or barred a sect's own member from it, as the tests' places shifted with the batches.
	 */
	@net.minecraft.gametest.framework.BeforeBatch(batch = "defaultBatch")
	public static void clearOldFormations(ServerLevel level) {
		for (Formation old : new ArrayList<>(FormationData.get(level).all())) Formations.dissolve(level, old.id);
	}

	@net.minecraft.gametest.framework.BeforeBatch(batch = "barrier_gaps")
	public static void clearOldFormationsForTheGaps(ServerLevel level) {
		clearOldFormations(level);
	}

	// --- Titles ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void titleCountsCoverEveryMember(GameTestHelper helper) {
		for (int n = 1; n <= 20; n++) {
			EnumMap<NpcTitle, Integer> counts = Sect.titleCounts(n);
			int total = counts.values().stream().mapToInt(Integer::intValue).sum();
			helper.assertTrue(total == n, "Every one of " + n + " members gets a title (got " + total + ")");
			helper.assertTrue(counts.get(NpcTitle.SECT_MASTER) == 1, "Always exactly one Sect Master");
		}
		EnumMap<NpcTitle, Integer> twenty = Sect.titleCounts(20);
		helper.assertTrue(twenty.get(NpcTitle.GRAND_ELDER) == 2 && twenty.get(NpcTitle.ELDER) == 4 && twenty.get(NpcTitle.INNER_DISCIPLE) == 6
				&& twenty.get(NpcTitle.OUTER_DISCIPLE) == 7, "20 members: 1 master, 2 grand elders, 4 elders, 6 inner, 7 outer");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void strongestIsAlwaysSectMaster(GameTestHelper helper) {
		Sect sect = new Sect(UUID.randomUUID());
		int[] ranks = {3, 10, 7, 7, 1, 2};
		List<Sect.Member> members = new ArrayList<>();
		for (int i = 0; i < ranks.length; i++) {
			Sect.Member m = new Sect.Member(UUID.randomUUID(), ranks[i], i);
			members.add(m);
			sect.members.put(m.id, m);
		}
		members.get(2).cultivation = 50; // ties at rank 7 go to more cultivation
		sect.recalculateTitles();
		helper.assertTrue(sect.master() == members.get(1), "The rank-10 member is master");
		helper.assertTrue(members.get(4).title == NpcTitle.OUTER_DISCIPLE, "The weakest is an outer disciple");
		sect.members.remove(members.get(1).id);
		sect.recalculateTitles();
		helper.assertTrue(sect.master() == members.get(2), "The master gone, the stronger of the two rank-7s takes over");
		members.get(4).rank = 12;
		sect.recalculateTitles();
		helper.assertTrue(sect.master() == members.get(4), "A disciple who surpasses the master becomes Sect Master");
		helper.succeed();
	}

	// --- Population ---

	private CultivatorNpc member(GameTestHelper helper, Sect sect, int rank, int home) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1 + home, 2, 1));
		npc.setRank(rank);
		npc.joinSect(sect, home);
		Sect.Member m = new Sect.Member(npc.getUUID(), rank, home);
		sect.members.put(m.id, m);
		return npc;
	}

	private Sect newSect(GameTestHelper helper, int alignment) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Test Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		sect.alignment = alignment;
		SectData.get(level.getServer()).put(sect);
		return sect;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eldersHoldTheirGroundWhileDisciplesRun(GameTestHelper helper) {
		Sect sect = newSect(helper, 0);
		CultivatorNpc master = member(helper, sect, 10, 0);
		CultivatorNpc elder = member(helper, sect, 8, 1);
		CultivatorNpc disciple = member(helper, sect, 1, 2);
		master.setTitle(NpcTitle.SECT_MASTER);
		elder.setTitle(NpcTitle.ELDER);
		disciple.setTitle(NpcTitle.OUTER_DISCIPLE);
		CultivatorNpc intruder = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(4, 2, 4));
		// Short of Grand Perfection: a Heavenly Being at it goes to ascend, and a traveller with no player near (in the test world,
		// often none within the simulation distance) is taken out of the world unseen (NpcTravel's sweep, twice a second). Swept
		// before the sect's first tick, it was gone before anyone had been in a fight with it, and the test timed out.
		intruder.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.LATE), -150, helper.getLevel().getRandom());
		intruder.setNoAi(true);
		helper.assertTrue(!CultivatorNpc.travellers().contains(intruder), "The intruder stays: it is no traveller, to go on its way unseen");
		for (CultivatorNpc npc : new CultivatorNpc[] {master, elder, disciple}) npc.startFleeing(intruder);
		helper.assertTrue(!master.shouldFlee() && !elder.shouldFlee(), "The master and its elders stand against a far stronger intruder");
		helper.assertTrue(disciple.shouldFlee(), "A disciple runs from it");
		BlockPos refuge = disciple.getRefuge();
		if (refuge != null) {
			// Only ground the world is still simulating counts, so in a test world the refuge may be drawn in (or be none).
			double distance = Math.sqrt(refuge.distSqr(disciple.blockPosition()));
			double dot = (refuge.getX() - disciple.getX()) * (disciple.getX() - intruder.getX()) + (refuge.getZ() - disciple.getZ()) * (disciple.getZ() - intruder.getZ());
			helper.assertTrue(distance >= 20 && distance <= CultivatorNpc.FLEE_DISTANCE + CultivatorNpc.FLEE_SPREAD + 2,
					"Its refuge is a good way off (" + Math.round(distance) + " blocks)");
			helper.assertTrue(dot > 0, "... away from the intruder, not past it");
		}
		disciple.reachRefuge();
		helper.assertTrue(!disciple.shouldFlee(), "Safe at its refuge, it stops running");
		elder.setHealth(elder.getMaxHealth() * 0.1f);
		elder.setLastHurtByMob(intruder);
		master.setHealth(master.getMaxHealth() * 0.1f);
		master.setLastHurtByMob(intruder);
		// Being in a fight is worked out on the elder's own tick: waited for, not counted (just after the test server starts, its
		// entities may not tick for the first few ticks, and a fixed five failed now and then).
		helper.succeedWhen(() -> {
			helper.assertTrue(elder.isInCombat() && elder.shouldFlee(), "Even an elder falls back once it is losing badly");
			helper.assertTrue(master.isInCombat() && !master.shouldFlee(), "The master never runs: it makes its last stand");
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theWholeSectTurnsOutForAnAttackOrABreach(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		CultivatorNpc master = member(helper, sect, 10, 0);
		CultivatorNpc elder = member(helper, sect, 8, 1);
		CultivatorNpc disciple = member(helper, sect, 1, 2);
		CultivatorNpc victim = member(helper, sect, 2, 3);
		master.setTitle(NpcTitle.SECT_MASTER);
		elder.setTitle(NpcTitle.ELDER);
		disciple.setTitle(NpcTitle.OUTER_DISCIPLE);
		victim.setTitle(NpcTitle.INNER_DISCIPLE);
		CultivatorNpc[] others = {master, elder, disciple};
		for (CultivatorNpc npc : others) npc.setNoAi(true);
		// The test's mock intruder counts as creative, whom sects ignore, so a rogue cultivator plays the intruder.
		CultivatorNpc intruder = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(5, 2, 5));
		intruder.becomeRogue(0, 0, level.getRandom());
		intruder.setNoAi(true);
		Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
		zombie.setNoAi(true);
		// Far across the territory: well past where a disciple would once have heard it.
		BlockPos far = sect.core.offset(60, 0, 0);
		helper.assertTrue(sect.inTerritory(far), "The spot is in the sect's territory");

		SectManager.alert(level, sect, zombie, far, SectManager.Alarm.MEMBER_ATTACKED, victim.getTitle(), victim);
		for (CultivatorNpc npc : others) helper.assertTrue(npc.getTarget() == null, "A monster far off is only the nearby disciples' business");
		SectManager.alert(level, sect, intruder, far, SectManager.Alarm.MEMBER_ATTACKED, victim.getTitle(), victim);
		for (CultivatorNpc npc : others) helper.assertTrue(npc.getTarget() == intruder, npc.getTitle() + " turns out against an intruder who attacked a member");

		for (CultivatorNpc npc : others) npc.setTarget(null);
		SectManager.alert(level, sect, intruder, far, SectManager.Alarm.BARRIER_ASSAILED, null, null);
		for (CultivatorNpc npc : others) helper.assertTrue(npc.getTarget() == null, "Striking the barrier far off without breaking it rouses no one there");
		SectManager.alert(level, sect, intruder, far, SectManager.Alarm.BARRIER_BROKEN, null, null);
		for (CultivatorNpc npc : others) helper.assertTrue(npc.getTarget() == intruder, npc.getTitle() + " turns out against an intruder who broke the barrier");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void playerKillsAreNeverReplacedAndExtinctionIsFinal(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		sect.capacity = 3;
		CultivatorNpc a = member(helper, sect, 5, 0);
		CultivatorNpc b = member(helper, sect, 3, 1);
		CultivatorNpc c = member(helper, sect, 1, 2);
		sect.recalculateTitles();
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		int before = CultivationManager.get(player).getAlignment();

		a.hurt(level.damageSources().playerAttack(player), 10_000);
		helper.assertTrue(!sect.members.containsKey(a.getUUID()), "A dead member leaves the roll");
		helper.assertTrue(sect.capacity == 2 && sect.replacements.isEmpty(), "A player's kill is never replaced: capacity drops for good");
		helper.assertTrue(sect.master() != null && sect.master().id.equals(b.getUUID()), "The next strongest becomes master");
		helper.assertTrue(CultivationManager.get(player).getAlignment() < before, "Killing a peaceful cultivator stains the player's alignment");

		b.hurt(level.damageSources().generic(), 10_000);
		helper.assertTrue(sect.replacements.size() == 1, "A death to anything else brings a replacement, in a few days");
		helper.assertTrue(sect.replacements.get(0) - level.getGameTime() >= Sect.REPLACEMENT_MIN, "The replacement takes days, not ticks");

		c.hurt(level.damageSources().generic(), 10_000);
		helper.assertTrue(sect.extinct && sect.replacements.isEmpty(), "The last member's death ends the sect for good");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void departureLeavesRoomForAReplacement(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		sect.capacity = 2;
		member(helper, sect, 4, 0);
		CultivatorNpc leaving = member(helper, sect, 2, 1);
		sect.recalculateTitles();
		leaving.leaveSect(level, true);
		helper.assertTrue(!leaving.isSectMember() && leaving.getTitle() == NpcTitle.ROGUE, "It leaves as a rogue cultivator");
		helper.assertTrue(sect.members.size() == 1 && sect.replacements.size() == 1 && sect.capacity == 2, "A departure is replaced later");
		helper.succeed();
	}

	// --- Alignment ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void alignmentRules(GameTestHelper helper) {
		helper.assertTrue(Alignment.factionOf(-51) == Alignment.Faction.DEVILISH && Alignment.factionOf(-50) == Alignment.Faction.NEUTRAL
				&& Alignment.factionOf(50) == Alignment.Faction.NEUTRAL && Alignment.factionOf(51) == Alignment.Faction.RIGHTEOUS, "Thresholds at +-50");
		helper.assertTrue(Alignment.killShift(-150, true, Realm.CORE_FORMATION, false) > 0, "Slaying the devilish raises alignment");
		helper.assertTrue(Alignment.killShift(0, false, Realm.QI_REFINING, false) < 0, "Killing a peaceful neutral lowers it");
		helper.assertTrue(Alignment.killShift(120, true, Realm.QI_REFINING, false) == 0, "Killing one who attacked first changes nothing");
		helper.assertTrue(Alignment.Faction.RIGHTEOUS.isEnemyOf(Alignment.Faction.DEVILISH) && !Alignment.Faction.NEUTRAL.isEnemyOf(Alignment.Faction.DEVILISH),
				"Righteous and devilish are enemies; the neutral are nobody's");
		helper.assertTrue(Alignment.clamp(999) == Alignment.MAX && Alignment.clamp(-999) == Alignment.MIN, "Alignment stays within -200..200");
		helper.succeed();
	}

	// --- NPC cultivation ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void npcsSkipTribulationsButKeepTheLowerRealmCap(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
		npc.advance(PlayerCultivation.required(Realm.QI_REFINING, Stage.GRAND_PERFECTION) + 1);
		helper.assertTrue(npc.getRank() >= PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), "A bottleneck gives way without a tribulation or pill");
		npc.advance(1.0e12);
		helper.assertTrue(npc.getRank() == PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), "The lower realms hold it at HB Grand Perfection");
		CultivatorNpc fresh = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
		fresh.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		PlayerCultivation player = new PlayerCultivation();
		player.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		// (Their rate x PlayerCultivation#STAGE_LENGTH since 2026-10-27: their stages as long as before, a player's that many times as long.)
		helper.assertTrue(Math.abs(fresh.meditationRate() * 10 - player.cultivationPerSecond() * PlayerCultivation.STAGE_LENGTH) < 1.0e-9,
				"NPCs cultivate ten times slower than players did before the stages grew longer");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void spawnRatesFallFromTenPercentToATenthOfOne(GameTestHelper helper) {
		int steps = 21; // mortal + QR Early .. HB Grand Perfection
		helper.assertTrue(Math.abs(NpcSpawner.chance(0, steps) - 0.10) < 1.0e-12, "A mortal: 10%");
		helper.assertTrue(Math.abs(NpcSpawner.chance(steps - 1, steps) - 0.001) < 1.0e-12, "Heavenly Being Grand Perfection: 0.1%");
		for (int i = 1; i < steps; i++) helper.assertTrue(NpcSpawner.chance(i, steps) < NpcSpawner.chance(i - 1, steps), "Each stage rarer than the last");
		net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(1);
		for (int i = 0; i < 2000; i++) {
			int rank = NpcSpawner.rollRank(random, false, true);
			helper.assertTrue(rank >= CultivatorNpc.MORTAL && rank <= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION),
					"Nothing above HB Grand Perfection in the lower realms");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyWanderingRoguesCountAndNoneDespawn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		CultivatorNpc member = member(helper, sect, 3, 0);
		CultivatorNpc rogue = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		rogue.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0, level.getRandom());
		CultivatorNpc named = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
		named.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0, level.getRandom());
		named.setCustomName(net.minecraft.network.chat.Component.literal("Kept"));
		CultivatorNpc heavenly = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 3));
		heavenly.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY), 0, level.getRandom());
		CultivatorNpc wanderer = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 3));
		wanderer.becomeRogue(CultivatorNpc.MORTAL, 0, level.getRandom());

		helper.assertTrue(rogue.isWanderingRogue(), "A rogue about the land counts toward the one near each player");
		helper.assertTrue(named.isWanderingRogue(), "So does one a player has named: naming it makes no room for another");
		helper.assertTrue(heavenly.getLifecycle() == CultivatorNpc.Lifecycle.SEEKING_SECLUSION && !heavenly.isWanderingRogue(),
				"Nor a Heavenly Being off to seclusion");
		helper.assertTrue(!member.isWanderingRogue(), "Nor a sect member");
		helper.assertTrue(wanderer.isWanderingMortal() && !wanderer.isWanderingRogue(), "A mortal wanderer is a kind of its own");
		Vec3 here = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
		// Close in: a cultivator of the next test over may wander within a few blocks.
		helper.assertTrue(CultivatorNpc.wanderingRoguesNear(level, here, 1.5) == 2, "Counted: the rogue and the named one, no others");
		for (CultivatorNpc npc : List.of(member, rogue, named, heavenly, wanderer)) {
			helper.assertTrue(npc.requiresCustomPersistence(), "No cultivator despawns");
			RogueIndex.note(npc); // as the index's next refresh would: each was a plain rogue when it spawned
		}
		helper.assertTrue(RogueIndex.anyWithin(level, here.x, here.z, 0.6, false), "The rogue is in the index");
		helper.assertTrue(RogueIndex.anyWithin(level, here.x + 1, here.z, 0.6, false), "and the named one");
		helper.assertTrue(!RogueIndex.anyWithin(level, here.x, here.z + 1, 0.6, false), "but not the Heavenly Being off to seclusion");
		helper.assertTrue(RogueIndex.anyWithin(level, here.x, here.z, 4, true), "The wanderer is in it as a wanderer");
		helper.assertTrue(NpcSpawner.wouldCrowd(level, here.add(NpcSpawner.SPACING - 10, 0, 0), false), "No new rogue within " + (int) NpcSpawner.SPACING + " of another");
		helper.assertTrue(NpcSpawner.wouldCrowd(level, here.add(NpcSpawner.SPACING - 10, 0, 0), true), "Nor a new wanderer of another wanderer");
		rogue.discard();
		helper.assertTrue(!RogueIndex.anyWithin(level, here.x, here.z, 0.6, false), "One gone for good leaves the index");
		helper.assertTrue(RogueIndex.anyWithin(level, here.x, here.z, 4, true), "The wanderer stays: the kinds are kept apart");
		helper.succeed();
	}

	// --- The Upper Realm's sects ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void upperRealmSectsLeanTowardTheHighRealms(GameTestHelper helper) {
		net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(5);
		int floor = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
		helper.assertTrue(SectManager.floorRank(true) == floor && SectManager.floorRank(false) == 0,
				"New disciples come at Core Formation in the Upper Realm, Qi Refining below");
		double upperSum = 0, lowerSum = 0;
		int upperCount = 0, lowerCount = 0;
		for (int s = 0; s < 50; s++) {
			int[] upper = SectManager.foundingRanks(random, 20, true);
			helper.assertTrue(upper[0] >= PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY), "An Upper Realm master is a Heavenly Being or Four Axis");
			for (int i = 1; i < upper.length; i++) {
				helper.assertTrue(upper[i] >= floor && upper[i] < upper[0], "Its members are Core Formation and up, below the master");
				upperSum += upper[i] - floor;
				upperCount++;
			}
			int[] lower = SectManager.foundingRanks(random, 20, false);
			for (int i = 1; i < lower.length; i++) {
				lowerSum += lower[i];
				lowerCount++;
			}
		}
		// Measured from each realm's floor, as a share of the way up to the master.
		helper.assertTrue(upperSum / upperCount > lowerSum / lowerCount, "Upper Realm members sit higher above their floor than lower-realm ones");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anAscendedCultivatorTakesTheSlotANewDiscipleWouldHave(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		sect.capacity = 3;
		member(helper, sect, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION), 0);
		member(helper, sect, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 1);
		sect.recalculateTitles();
		sect.replacements.add(level.getGameTime() + Sect.REPLACEMENT_MIN); // the slot a lost member left, a disciple on the way
		CultivatorNpc newcomer = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 4));
		newcomer.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0, level.getRandom());

		helper.assertTrue(SectManager.findOpening(level, newcomer) == sect, "It finds the sect of its path with room");
		SectManager.reserve(level, sect, newcomer.getUUID());
		helper.assertTrue(sect.replacements.isEmpty() && sect.openSlots() == 0, "The slot is held for it: the disciple on the way is called off");
		CultivatorNpc another = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 4));
		another.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0, level.getRandom());
		helper.assertTrue(SectManager.findOpening(level, another) != sect, "Nobody else can take a held slot");

		helper.assertTrue(SectManager.admit(level, sect, newcomer), "On arriving it joins");
		helper.assertTrue(sect.id.equals(newcomer.getSectId()) && sect.members.size() == 3 && sect.reserved.isEmpty(), "in the slot it held");
		helper.assertTrue(newcomer.getTitle() == NpcTitle.SECT_MASTER, "Stronger than the Sect Master, it takes the seat");

		Sect other = newSect(helper, 0);
		other.capacity = 2;
		member(helper, other, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
		SectManager.reserve(level, other, another.getUUID());
		SectManager.release(level, other.id, another.getUUID());
		helper.assertTrue(other.reserved.isEmpty() && other.replacements.size() == 1, "A slot given up (it died on the way) goes to a new disciple");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void aMemberLetGoWhileUnloadedIsWokenAndLeavesAtOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		sect.capacity = 2;
		CultivatorNpc master = member(helper, sect, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), 0);
		// A member whose land is unloaded: on the roll, not in the world, last seen at the far corner of the test.
		UUID unloaded = UUID.randomUUID();
		Sect.Member away = new Sect.Member(unloaded, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 1);
		away.lastAt = helper.absolutePos(new BlockPos(6, 2, 6));
		sect.members.put(unloaded, away);
		sect.recalculateTitles();
		CultivatorNpc newcomer = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 4));
		newcomer.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0, level.getRandom());

		helper.assertTrue(SectManager.admit(level, sect, newcomer), "The newcomer joins");
		helper.assertTrue(!sect.members.containsKey(unloaded) && sect.letGo.containsKey(unloaded)
				&& away.lastAt.equals(sect.letGo.get(unloaded).lastAt), "The one let go is off the roll, to be woken where it last stood");
		Sect saved = Sect.load(sect.save());
		helper.assertTrue(saved.letGo.containsKey(unloaded) && away.lastAt.equals(saved.letGo.get(unloaded).lastAt), "That survives a save");
		SectManager.rouse(level.getServer(), sect);
		helper.assertTrue(sect.letGo.get(unloaded).looks == 1, "Its land is woken, and looked at again till it turns up");

		// Its land loads (woken, or anyone's): the body, still thinking itself a member, sets off at once, ticking or not.
		CultivatorNpc body = ModEntities.CULTIVATOR.create(level);
		body.setUUID(unloaded);
		body.setRank(away.rank);
		body.joinSect(sect, 1);
		body.moveTo(away.lastAt.getX() + 0.5, away.lastAt.getY(), away.lastAt.getZ() + 0.5, 0, 0);
		level.addFreshEntity(body);
		helper.assertTrue(body.getSectId() == null && body.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING, "It sets off the moment it loads");
		helper.assertTrue(!sect.letGo.containsKey(unloaded), "and needn't be looked for any more");
		// Heavenly Beings and up leave a lower-realm sect on their own: gone before they can, so they don't wander off into other tests.
		body.discard();
		master.discard();
		newcomer.discard();

		Sect other = newSect(helper, 0);
		CultivatorNpc disciple = member(helper, other, PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 0);
		helper.runAfterDelay(30, () -> {
			Sect.Member kept = other.members.get(disciple.getUUID());
			// Kept every second it ticks: within a second's stroll of where it is now.
			helper.assertTrue(kept != null && kept.lastAt != null && kept.lastAt.distSqr(disciple.blockPosition()) <= 16,
					"A loaded member's place is kept up as it goes (" + (kept == null ? null : kept.lastAt) + " vs " + disciple.blockPosition() + ")");
			disciple.discard();
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anUnseenJourneyKeepsToARealWay(GameTestHelper helper) {
		BlockPos from = new BlockPos(0, 100, 0), to = new BlockPos(400, 100, 300); // 500 blocks
		long length = (long) (500 / NpcTravel.SPEED);
		var journey = new NpcTravel.Journey(net.minecraft.world.level.Level.OVERWORLD, from, to, 1000, 1000 + length, false, new net.minecraft.nbt.CompoundTag());
		helper.assertTrue(journey.at(1000).distanceTo(Vec3.atBottomCenterOf(from)) < 0.01, "Setting off, it is where it left");
		Vec3 half = journey.at(1000 + length / 2);
		helper.assertTrue(half.distanceTo(new Vec3(200.5, 100, 150.5)) < 1, "Halfway there, it is halfway along the line (" + half + ")");
		helper.assertTrue(journey.at(1000 + length * 3).distanceTo(Vec3.atBottomCenterOf(to)) < 0.01, "Done, it is there, and no further");
		var back = journey.shortOfTheEnd(1000 + length * 2, 160);
		double left = back.at(1000 + length * 2).distanceTo(Vec3.atBottomCenterOf(to));
		helper.assertTrue(Math.abs(left - 160) < 1 && back.readyAt() > 1000 + length * 2,
				"Someone waiting at its end: it is put back " + Math.round(left) + " blocks along its way, to fly in in their sight");

		// Saved, and as saved before journeys had a way (at their end already).
		net.minecraft.nbt.CompoundTag old = new net.minecraft.nbt.CompoundTag();
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		net.minecraft.nbt.CompoundTag j = new net.minecraft.nbt.CompoundTag();
		j.putString("Dimension", "minecraft:overworld");
		j.putLong("Target", to.asLong());
		j.putLong("ReadyAt", 5000);
		j.put("Entity", new net.minecraft.nbt.CompoundTag());
		list.add(j);
		old.put("Journeys", list);
		net.minecraft.nbt.CompoundTag again = NpcTravel.load(old).save(new net.minecraft.nbt.CompoundTag()).getList("Journeys", 10).getCompound(0);
		helper.assertTrue(again.getLong("Origin") == to.asLong() && again.getLong("DepartedAt") == 5000 && !again.getBoolean("Ascent"),
				"An old journey is simply at its end, and keeps its way when saved again");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void anUnseenTravellerIsMetWhereItHasGotTo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos here = helper.absolutePos(new BlockPos(2, 10, 2));
		// A Heavenly Being leaving its sect, unseen, its way running right across the test from 300 blocks off either side.
		CultivatorNpc traveller = ModEntities.CULTIVATOR.create(level);
		traveller.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.MID), 0, level.getRandom());
		BlockPos from = here.offset(-300, 0, 0), to = here.offset(300, 0, 0);
		traveller.moveTo(from.getX() + 0.5, from.getY(), from.getZ() + 0.5, 0, 0);
		traveller.setJourneyTarget(to);
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		traveller.saveAsPassenger(tag);
		tag.getCompound("Npc").putString("Lifecycle", CultivatorNpc.Lifecycle.DEPARTING.name()); // as one leaving its sect
		long now = level.getGameTime();
		long length = (long) (600 / NpcTravel.SPEED);
		// Set off long enough ago to be just short of the test now.
		long departed = now - (long) (290 / NpcTravel.SPEED);
		NpcTravel.journeys(level.getServer()).add(new NpcTravel.Journey(level.dimension(), from, to, departed, departed + length, false, tag));
		helper.makeMockServerPlayerInLevel().moveTo(here.getX() + 0.5, here.getY(), here.getZ() + 0.5);
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(NpcTravel.journeys(level.getServer()).stream().noneMatch(journey -> journey.entity() == tag), "It is no longer on its way unseen");
			net.minecraft.world.entity.Entity met = level.getEntity(traveller.getUUID());
			helper.assertTrue(met instanceof CultivatorNpc, "It is back in the world");
			CultivatorNpc npc = (CultivatorNpc) met;
			double off = Math.abs(npc.getX() - here.getX());
			helper.assertTrue(off < NpcTravel.reach(level.getServer()) + 8, "Where it had got to on its way, near the player (" + Math.round(npc.getX() - here.getX()) + " blocks off)");
			helper.assertTrue(npc.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING && to.equals(npc.getJourneyTarget()), "Still bound where it was");
			helper.assertTrue(npc.isQiFlying(), "In the air, on its way");
			npc.discard();
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aTravellerStopsToFightWhoeverStrikesItThenGoesOn(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer player = new ServerPlayer(level.getServer(), level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "wayfarer")) {
			@Override
			public boolean isSpectator() {
				return false;
			}

			@Override
			public boolean isCreative() {
				return false;
			}
		};
		level.getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
		player.teleportTo(level, stand.x, stand.y, stand.z, 0, 0);
		CultivatorNpc traveller = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(4, 2, 4));
		traveller.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.MID), 0, level.getRandom());
		traveller.dismiss(level); // sets off 200-1,000 blocks
		helper.assertTrue(traveller.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING, "On its way");
		// Struck once it has ticked (a blow in its very first tick is stamped 0, which vanilla's retaliation takes for none).
		helper.runAfterDelay(5, () -> traveller.hurt(level.damageSources().playerAttack(player), 1.0f));
		helper.runAfterDelay(15, () -> {
			helper.assertTrue(player.equals(traveller.getTarget()), "Struck on its way, it turns on the one who struck it");
			helper.assertTrue(traveller.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING, "its journey only waiting");
			level.getServer().getPlayerList().remove(player);
			helper.runAfterDelay(40, () -> {
				boolean unseen = NpcTravel.journeys(level.getServer()).removeIf(j -> j.entity().hasUUID("UUID") && j.entity().getUUID("UUID").equals(traveller.getUUID()));
				helper.assertTrue(unseen || traveller.getTarget() == null && traveller.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING,
						"Its foe gone, it goes on its way (" + (unseen ? "unseen" : "in the world") + ")");
				traveller.discard();
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTreasuryHoldsWhileItsEldersKeepIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		sect.barrierRadius = 16;
		BlockPos vault = helper.absolutePos(new BlockPos(2, 2, 2));
		sect.treasury = new net.minecraft.world.level.levelgen.structure.BoundingBox(vault.getX() - 1, vault.getY() - 1, vault.getZ() - 1, vault.getX() + 1, vault.getY() + 1, vault.getZ() + 1);
		sect.bounds = sect.treasury.inflatedBy(8);
		CultivatorNpc disciple = member(helper, sect, PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 1);
		disciple.setTitle(NpcTitle.OUTER_DISCIPLE);
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault) == null, "A disciple alone doesn't hold the seal");
		CultivatorNpc elder = member(helper, sect, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
		elder.setTitle(NpcTitle.ELDER);
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault) == sect, "An Elder in the territory holds it shut");
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault.offset(3, 0, 0)) == null, "Only the treasury itself");

		// The seal is as strong as its strongest keeper; only one a realm above can break it.
		CultivatorNpc grandElder = member(helper, sect, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.LATE), 2);
		grandElder.setTitle(NpcTitle.GRAND_ELDER);
		helper.assertTrue(SectManager.treasurySeal(level, sect) == Realm.CORE_FORMATION, "The seal is of its strongest keeper's realm (Core Formation)");
		ServerPlayer thief = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(thief);
		c.setMortal(true);
		helper.assertTrue(!SectManager.breakTreasurySeal(level, sect, thief, vault), "A mortal can't break it");
		c.setMortal(false);
		c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(!SectManager.breakTreasurySeal(level, sect, thief, vault), "Nor one of the seal's own realm, however far along");
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		helper.assertTrue(SectManager.breakTreasurySeal(level, sect, thief, vault), "One a realm above breaks it");
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault) == null, "and the treasury lies open");
		sect.sealBrokenUntil = level.getGameTime();
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault) == sect, "until its keepers cast it anew (" + SectManager.SEAL_RECAST / 1200 + " minutes later)");
		sect.sealBrokenUntil = -1;

		// A blast: glass in the sealed treasury stands; once nobody keeps it, it goes.
		helper.setBlock(new BlockPos(2, 2, 2), Blocks.GLASS);
		java.util.List<BlockPos> blast = new java.util.ArrayList<>(List.of(vault, vault.offset(4, 0, 0)));
		SectManager.spareSealedTreasuries(level, blast);
		helper.assertTrue(blast.size() == 1 && blast.get(0).equals(vault.offset(4, 0, 0)), "A blast spares the sealed treasury, not what's outside it");
		// (Both go at once: one leaving, the sect retitles the rest, and its strongest left would be Sect Master, no keeper.)
		grandElder.discard();
		elder.discard();
		helper.assertTrue(SectManager.sealedTreasuryAt(level, vault) == null, "Its Elders gone, the treasury is anyone's");
		blast = new java.util.ArrayList<>(List.of(vault));
		SectManager.spareSealedTreasuries(level, blast);
		helper.assertTrue(blast.size() == 1, "and a blast breaks into it");
		disciple.discard();
		// Its grounds cover this spot: put it out of the way, or a later run's test laid out here would find it first
		// (SectManager#sectAt), as the game-test world is kept between runs.
		sect.extinct = true;
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFullSectLetsItsWeakestGoForANewcomer(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		sect.capacity = 2;
		CultivatorNpc master = member(helper, sect, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), 0);
		CultivatorNpc weakest = member(helper, sect, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 1);
		sect.recalculateTitles();
		CultivatorNpc newcomer = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 4));
		newcomer.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0, level.getRandom());

		helper.assertTrue(sect.openSlots() == 0 && sect.canTakeIn(newcomer.getRank()), "Full, but with a member weaker than the newcomer to let go");
		helper.assertTrue(SectManager.findOpening(level, newcomer) == sect, "So the newcomer can choose it");
		SectManager.reserve(level, sect, newcomer.getUUID());
		CultivatorNpc another = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 4));
		another.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), 0, level.getRandom());
		helper.assertTrue(!sect.canTakeIn(another.getRank()), "Only one newcomer at a time pushes a member out");

		helper.assertTrue(SectManager.admit(level, sect, newcomer), "It joins");
		helper.assertTrue(!sect.members.containsKey(weakest.getUUID()) && weakest.getSectId() == null
				&& weakest.getLifecycle() == CultivatorNpc.Lifecycle.DEPARTING, "The weakest member is let go, and sets off to make its own way");
		helper.assertTrue(sect.members.size() == 2 && sect.replacements.isEmpty() && sect.reserved.isEmpty(), "No replacement is sent for it: its place is taken");
		helper.assertTrue(sect.master() != null && sect.master().id.equals(master.getUUID()), "The Four Axis master, stronger than the newcomer, keeps the seat");

		Sect strong = newSect(helper, 0);
		strong.capacity = 1;
		member(helper, strong, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION), 0);
		strong.recalculateTitles();
		helper.assertTrue(!strong.canTakeIn(another.getRank()), "A sect with no one weaker than the newcomer (the master never counts) has no place for it");
		helper.succeed();
	}

	// --- Lives off-screen (NpcLedger) ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void catchUpHasNoLimitAndKeepsTheLedgersTime(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CultivatorNpc far = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		far.becomeRogue(PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), 0, level.getRandom());
		far.cultivation().setState(Realm.QI_REFINING, Stage.LATE, 0);
		far.cultivation().setHeavensLeave(true);
		far.catchUpForTest(level, 100 * HeavenRitual.DAY, 1000);
		helper.assertTrue(far.getRank() >= PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY),
				"A hundred days away: through Qi Refining, Foundation and Core, no three-day limit (rank " + far.getRank() + ")");

		// The ledger reckons as the catch-up does: a Foundation Building rogue reaches Heavenly Being just when it says.
		int from = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
		int target = PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.EARLY);
		long ticks = (long) Math.ceil(NpcLedger.secondsAway(from, 0, true, target, new boolean[1]) * 20);
		CultivatorNpc early = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
		CultivatorNpc onTime = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		for (CultivatorNpc npc : List.of(early, onTime)) {
			npc.becomeRogue(from, 0, level.getRandom());
			npc.cultivation().setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
			npc.cultivation().setHeavensLeave(true);
		}
		early.catchUpForTest(level, ticks - 200, 1000);
		onTime.catchUpForTest(level, ticks + 200, 1000);
		helper.assertTrue(early.getRank() == target - 1, "Ten seconds short of the ledger's time: still Nascent Soul (rank " + early.getRank() + ")");
		helper.assertTrue(onTime.getRank() == target, "Ten seconds past it: a Heavenly Being (rank " + onTime.getRank() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heldAtTheGateItAsksOnceForEachDayAway(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		npc.becomeRogue(PlayerCultivation.RITUAL_RANK, 0, level.getRandom());
		npc.cultivation().setHeavensLeave(false);
		npc.cultivation().setState(Realm.QI_REFINING, Stage.MID, 0);
		npc.cultivation().setState(Realm.QI_REFINING, Stage.MID, npc.cultivation().cultivationRequired());
		helper.assertTrue(npc.awaitsHeavensLeave(), "At Qi Refining Mid's gate, cultivation full, no leave");
		Long due = NpcLedger.dueAt(npc, level);
		helper.assertTrue(due != null && due - level.getGameTime() == NpcLedger.GATE_RETRY, "Unloaded, the ledger wakes it in three days to ask again");
		npc.catchUpForTest(level, 40 * HeavenRitual.DAY, 1000);
		helper.assertTrue(npc.cultivation().hasHeavensLeave() && npc.getRank() > PlayerCultivation.RITUAL_RANK,
				"Forty days away, asking once a day: granted, and it cultivated on (rank " + npc.getRank() + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anEmptiedHermitRegionFillsAgainInTwoWeeks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		long ascendedRegion = net.minecraft.world.level.ChunkPos.asLong(900_001, 900_001);
		long slainRegion = net.minecraft.world.level.ChunkPos.asLong(900_002, 900_002);
		CultivatorNpc ascended = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		CultivatorNpc slain = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		CultivatorNpc stranger = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(5, 2, 1));
		HermitSites.seatForTest(level.getServer(), ascended, ascendedRegion);
		HermitSites.seatForTest(level.getServer(), slain, slainRegion);

		HermitSites.onLeft(level, ascended, false);
		helper.assertTrue(!HermitSites.isHermit(level.getServer(), ascended)
						&& HermitSites.vacantUntil(level.getServer(), ascendedRegion) == level.getGameTime() + HermitSites.REFILL,
				"A hermit gone to ascend: its region gets a new one in two weeks");
		HermitSites.onLeft(level, slain, true);
		helper.assertTrue(!HermitSites.isHermit(level.getServer(), slain) && HermitSites.vacantUntil(level.getServer(), slainRegion) == -1,
				"One killed by a player is never replaced");
		HermitSites.onLeft(level, stranger, false);
		helper.assertTrue(!HermitSites.isHermit(level.getServer(), stranger), "A cultivator that was no region's hermit changes nothing");

		HermitSites.reopenDue(level, level.getGameTime() + HermitSites.REFILL - 1);
		helper.assertTrue(HermitSites.isDecided(level.getServer(), ascendedRegion), "A tick before the two weeks are up, it still waits");
		HermitSites.reopenDue(level, level.getGameTime() + HermitSites.REFILL);
		helper.assertTrue(!HermitSites.isDecided(level.getServer(), ascendedRegion) && HermitSites.vacantUntil(level.getServer(), ascendedRegion) == -1,
				"Two weeks on, the region is open again: a new hermit is seated as its land next loads");
		helper.assertTrue(HermitSites.isDecided(level.getServer(), slainRegion), "The slain one's region stays as it is");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void hermitRegionsAreDecidedByTheSeed(GameTestHelper helper) {
		int with = 0;
		for (int rx = -20; rx < 20; rx++) {
			for (int rz = -20; rz < 20; rz++) {
				net.minecraft.world.level.ChunkPos chunk = HermitSites.siteChunk(12345L, rx, rz);
				if (chunk == null) continue;
				with++;
				helper.assertTrue(Math.floorDiv(chunk.x, HermitSites.REGION) == rx && Math.floorDiv(chunk.z, HermitSites.REGION) == rz,
						"A region's hermit chunk lies in that region");
				helper.assertTrue(chunk.equals(HermitSites.siteChunk(12345L, rx, rz)), "and is the same every time");
			}
		}
		helper.assertTrue(with > 1600 * (HermitSites.CHANCE - 0.06) && with < 1600 * (HermitSites.CHANCE + 0.06),
				"About " + Math.round(HermitSites.CHANCE * 100) + "% of regions have a hermit (" + with + " of 1600)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sectSitesAreTakenNearestFirstAndRemembered(GameTestHelper helper) {
		// As the Overworld's sects are spread (structure set "sects").
		var placement = new net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement(32, 16,
				net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType.LINEAR, 1874662921);
		BlockPos anchor = new BlockPos(-1234, 64, 5678);
		java.util.Set<Long> done = new java.util.HashSet<>();
		double last = -1;
		long[] next;
		while ((next = SectSites.nearestUndecided(done, placement, 12345L, anchor, SectSites.SPAWN_REACH)) != null) {
			net.minecraft.world.level.ChunkPos region = new net.minecraft.world.level.ChunkPos(next[0]);
			net.minecraft.world.level.ChunkPos site = new net.minecraft.world.level.ChunkPos(next[1]);
			helper.assertTrue(Math.floorDiv(site.x, 32) == region.x && Math.floorDiv(site.z, 32) == region.z, "A site lies in its region");
			double distance = Math.hypot(site.getMiddleBlockX() - anchor.getX(), site.getMiddleBlockZ() - anchor.getZ());
			helper.assertTrue(distance <= SectSites.SPAWN_REACH, "Only sites within reach are taken (" + Math.round(distance) + ")");
			helper.assertTrue(distance >= last, "Nearest first (" + Math.round(distance) + " after " + Math.round(last) + ")");
			last = distance;
			helper.assertTrue(done.add(next[0]), "Each region once");
		}
		// About pi * 2048^2 / 512^2 (50) regions' sites lie within reach.
		helper.assertTrue(done.size() > 35 && done.size() < 65, "Every site within reach is taken in the end (" + done.size() + ")");

		SectSites sites = new SectSites();
		long region = net.minecraft.world.level.ChunkPos.asLong(-3, 11);
		sites.decide(ModDimensions.UPPER_REALM, region);
		SectSites loaded = SectSites.load(sites.save(new net.minecraft.nbt.CompoundTag()));
		helper.assertTrue(loaded.isDecided(ModDimensions.UPPER_REALM, region), "A decided site is remembered through a save");
		helper.assertTrue(!loaded.isDecided(net.minecraft.world.level.Level.OVERWORLD, region), "in its own dimension only");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void upperRealmSectsKeepToTheirOwnCells(GameTestHelper helper) {
		var set = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET)
				.get(DefyingTheHeavens.id("sects_upper_realm"));
		helper.assertTrue(set != null && set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement,
				"The Upper Realm's sects are spread at random");
		var spread = (net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement) set.placement();
		helper.assertTrue(spread.spacing() - spread.separation() == 1, "A start chunk exactly every " + spread.spacing() + " chunks, at its region's corner");
		int cell = spread.spacing() * 16;
		int reach = SectStructure.ISLAND_SEARCH + SectStructure.largestHalfWidth();
		helper.assertTrue(reach <= cell / 2, "A sect moved as far as it may for an island (" + reach + ") stays in its " + cell + "-block cell: two never overlap");
		helper.assertTrue(reach <= 8 * 16, "and within the 8 chunks of its start where worldgen places its pieces");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNewcomerLooksFurtherWhenNoSectIsNear(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Far off where no other test's sects stand. The newcomer isn't added to the world: only where it is matters.
		int x0 = 3_000_000;
		CultivatorNpc newcomer = ModEntities.CULTIVATOR.create(level);
		newcomer.setPos(x0, 100, 0);
		newcomer.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.MID), 0, level.getRandom());
		helper.assertTrue(Alignment.factionOf(-300) == Alignment.Faction.DEVILISH, "-300 is devilish");
		Sect otherPath = farSect(helper, x0 + 100, -300, 3);
		Sect nearest = farSect(helper, x0 + 3000, 0, 3);
		Sect further = farSect(helper, x0 - 6000, 0, 3);
		Sect beyond = farSect(helper, x0 + (int) SectManager.JOIN_RANGE + 500, 0, 3);

		helper.assertTrue(SectManager.findOpening(level, newcomer) == nearest, "None of its path within 2,048: it looks further, nearest first (3,000 off)");
		nearest.extinct = true;
		helper.assertTrue(SectManager.findOpening(level, newcomer) == further, "then the next (6,000 off)");
		further.extinct = true;
		helper.assertTrue(SectManager.findOpening(level, newcomer) == null, "but not beyond " + (int) SectManager.JOIN_RANGE
				+ " (" + (beyond.core.getX() - x0) + "), nor one of another path (" + otherPath.name + ")");
		otherPath.extinct = true;
		beyond.extinct = true;
		helper.succeed();
	}

	/** A founded sect of {@code alignment} with a free slot, its core at x, 100, 0. */
	private Sect farSect(GameTestHelper helper, int x, int alignment, int capacity) {
		Sect sect = newSect(helper, alignment);
		sect.name = "Far Sect " + x;
		sect.core = new BlockPos(x, 100, 0);
		sect.capacity = capacity;
		return sect;
	}

	// --- Formations ---

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void destroyingTheCoreClearsEveryBarrierBlock(GameTestHelper helper) {
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(coreRel);
		FormationCoreBlockEntity entity = (FormationCoreBlockEntity) level.getBlockEntity(core);
		entity.bindToSect(UUID.randomUUID(), "Test", Formations.MIN_RADIUS, 0);
		helper.runAfterDelay(45, () -> {
			int barriers = countBarriers(level, core, Formations.MIN_RADIUS);
			helper.assertTrue(barriers > 50, "The barrier rises (" + barriers + " blocks)");
			helper.setBlock(coreRel, Blocks.AIR);
			int left = countBarriers(level, core, Formations.MIN_RADIUS, true);
			helper.assertTrue(left == 0, "Breaking the core takes every barrier block with it at once (" + left + " left)");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void formationsKeepMonstersOut(GameTestHelper helper) {
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(coreRel);
		((FormationCoreBlockEntity) level.getBlockEntity(core)).bindToSect(UUID.randomUUID(), "Test", Formations.MIN_RADIUS, 0);
		helper.runAfterDelay(45, () -> {
			helper.assertTrue(Formations.shelters(level, core.above()), "Inside a raised barrier is sheltered ground");
			// (High above: other tests' barriers stand around it at its own height.)
			helper.assertTrue(!Formations.shelters(level, core.offset(0, 150, 0)), "Outside it is not");
			// A wild zombie (the world's own, so not kept), one that must stay in the world (the test's spawns are kept), a named
			// one, and a cow (no monster).
			Zombie wild = EntityType.ZOMBIE.create(level);
			wild.moveTo(core.getX() + 1.5, core.getY() + 1, core.getZ() + 0.5, 0, 0);
			level.addFreshEntity(wild);
			Zombie kept = helper.spawn(EntityType.ZOMBIE, coreRel.offset(0, 1, -1));
			Zombie named = helper.spawn(EntityType.ZOMBIE_VILLAGER, coreRel.offset(0, 1, 1));
			named.setCustomName(net.minecraft.network.chat.Component.literal("Gerald"));
			var cow = helper.spawn(EntityType.COW, coreRel.offset(-1, 1, 0));
			helper.assertTrue(Formations.isMonster(wild) && Formations.isMonster(named) && !Formations.isMonster(cow),
					"Zombies are monsters, cows aren't");
			helper.runAfterDelay(25, () -> {
				helper.assertTrue(wild.isRemoved(), "A wild monster inside the barrier is dispersed");
				helper.assertTrue(kept.isAlive() && !kept.isRemoved(), "One that must stay is kept in the world");
				helper.assertTrue(kept.blockPosition().distSqr(core) > Formations.MIN_RADIUS * Formations.MIN_RADIUS,
						"... but set down outside the barrier (" + kept.blockPosition().toShortString() + ")");
				helper.assertTrue(named.isAlive() && !named.isRemoved() && Formations.shelters(level, named.blockPosition()),
						"A named one is left where it is, inside (" + named.blockPosition().toShortString() + ")");
				helper.assertTrue(cow.isAlive() && !cow.isRemoved(), "Animals are left in peace");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void theWeakAreCastOutOfABarrier(GameTestHelper helper) {
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(coreRel);
		// As strong as a Qi Refining Early master: mortals can't break it, cultivators can.
		FormationCoreBlockEntity entity = (FormationCoreBlockEntity) level.getBlockEntity(core);
		entity.bindToSect(UUID.randomUUID(), "Test", Formations.MIN_RADIUS, 0);
		helper.runAfterDelay(45, () -> {
			Formation formation = Formations.get(level, entity.getFormationId());
			// The test's mock players are always in creative, which the guard passes over: its two halves are checked here.
			ServerPlayer mortal = helper.makeMockServerPlayerInLevel();
			mortal.teleportTo(core.getX() + 1.5, core.getY() + 1, core.getZ() + 0.5);
			ServerPlayer cultivator = helper.makeMockServerPlayerInLevel();
			CultivationManager.get(cultivator).setState(Realm.QI_REFINING, Stage.EARLY, 0);
			helper.assertTrue(!Formations.canBreak(mortal, formation), "A mortal is too weak for it");
			helper.assertTrue(Formations.canBreak(cultivator, formation), "A cultivator at its strength may come and go");
			helper.assertTrue(Formations.expel(level, formation, mortal), "The ground outside is loaded");
			double limit = Formations.MIN_RADIUS + 0.5;
			helper.assertTrue(mortal.blockPosition().distSqr(core) > limit * limit,
					"The one too weak is set down outside (" + mortal.blockPosition().toShortString() + ")");
			helper.succeed();
		});
	}

	// In a batch of its own (2026-10-26): run beside the other tests' formations, one's shell now and then crossed this one's water cell and
	// kept it barred after this barrier came down (Formations#clearShell leaves a cell another raised shell holds); its batch's ground
	// cleared of old formations first (clearOldFormations).
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200, batch = "barrier_gaps")
	public void theBarrierClosesEveryGap(GameTestHelper helper) {
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		ServerLevel level = helper.getLevel();
		BlockPos core = helper.absolutePos(coreRel);
		// Cells on the radius-4 shell, each holding something the old barrier left open (or something it must keep).
		BlockPos water = core.offset(4, 0, 0), torch = core.offset(0, 0, 4), poppy = core.offset(0, 0, -4), fence = core.offset(-4, 0, 0);
		BlockPos stone = core.offset(0, 4, 0), slab = core.offset(3, 0, 3), seal = core.offset(-3, 0, -3);
		for (BlockPos ground : new BlockPos[] {water, torch, seal}) level.setBlockAndUpdate(ground.below(), Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(poppy.below(), Blocks.GRASS_BLOCK.defaultBlockState());
		level.setBlockAndUpdate(water, Blocks.WATER.defaultBlockState());
		level.setBlockAndUpdate(torch, Blocks.TORCH.defaultBlockState());
		level.setBlockAndUpdate(poppy, Blocks.POPPY.defaultBlockState());
		level.setBlockAndUpdate(fence, Blocks.OAK_FENCE.defaultBlockState());
		level.setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(slab, Blocks.STONE_SLAB.defaultBlockState());
		level.setBlockAndUpdate(seal, ModBlocks.SEAL.defaultBlockState());
		FormationCoreBlockEntity entity = (FormationCoreBlockEntity) level.getBlockEntity(core);
		entity.bindToSect(UUID.randomUUID(), "Test", Formations.MIN_RADIUS, 0);
		helper.runAfterDelay(45, () -> {
			BlockState held = level.getBlockState(water);
			helper.assertTrue(held.is(ModBlocks.SECT_BARRIER) && held.getValue(SectBarrierBlock.FLUID) == SectBarrierBlock.Held.WATER
					&& held.getFluidState().isSource(), "Still water on the shell is closed by a barrier that holds the water (" + held + ")");
			for (BlockPos closed : new BlockPos[] {torch, poppy, fence}) {
				helper.assertTrue(level.getBlockState(closed).is(ModBlocks.SECT_BARRIER), "What stood in the way is replaced: " + closed.toShortString());
			}
			helper.assertTrue(itemNear(level, core, Items.TORCH) && itemNear(level, core, Items.OAK_FENCE), "Built things drop as items");
			helper.assertTrue(!itemNear(level, core, Items.POPPY), "Wild flowers simply give way");
			helper.assertTrue(level.getBlockState(stone).is(Blocks.STONE) && level.getBlockState(slab).is(Blocks.STONE_SLAB),
					"Solid ground and slabs already close their cells and are kept");
			helper.assertTrue(level.getBlockState(seal).is(ModBlocks.SEAL), "Seals are kept, so an array can cross the shell");
			Zombie zombie = EntityType.ZOMBIE.create(level);
			zombie.moveTo(Vec3.atCenterOf(seal));
			helper.assertTrue(!level.getBlockState(seal).getCollisionShape(level, seal, CollisionContext.of(zombie)).isEmpty(),
					"... and a seal on the shell is solid to those the barrier keeps out");
			Formations.lower(level, Formations.get(level, entity.getFormationId()));
			helper.assertTrue(level.getBlockState(water).is(Blocks.WATER), "When the barrier comes down the water is left behind");
			helper.succeed();
		});
	}

	private static boolean itemNear(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
		// Anywhere around the test's barrier: an item is pushed out of the cell the barrier takes, and falls.
		return !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(9),
				e -> e.getItem().is(item)).isEmpty();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void anExtinctSectsCoreCanBeClaimed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		CultivatorNpc last = member(helper, sect, 5, 0);
		sect.recalculateTitles();
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(helper.absolutePos(coreRel));
		core.bindToSect(sect.id, sect.name, Formations.MIN_RADIUS, 5);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(core.getSect() != null, "A living sect's core answers to the sect");
		last.hurt(level.damageSources().generic(), 10_000);
		helper.assertTrue(sect.extinct, "Its last member dead, the sect is gone");
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(core.getSect() == null && Formations.get(level, core.getFormationId()).getSect() == null,
					"Within a second the core lets go of the fallen sect");
			helper.assertTrue(core.canControl(player), "The first to lay hands on it may take it up");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void trustedCultivatorsPassTheBarrier(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos coreRel = new BlockPos(4, 2, 4);
		helper.setBlock(coreRel.below(), Blocks.STONE);
		helper.setBlock(coreRel, ModBlocks.FORMATION_CORE);
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(helper.absolutePos(coreRel));
		ServerPlayer owner = helper.makeMockServerPlayerInLevel();
		ServerPlayer friend = helper.makeMockServerPlayerInLevel();
		ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
		core.setOwner(owner);
		core.trust(friend.getUUID(), "Friend");
		Formation formation = Formations.get(level, core.getFormationId());
		helper.assertTrue(formation.allows(owner) && formation.allows(friend) && !formation.allows(stranger),
				"The owner and those they trust pass; others don't");
		helper.assertTrue(Formations.canBreak(friend, formation), "A trusted cultivator is never cast out or kept from digging");
		helper.assertTrue(Formation.load(formation.save()).getTrusted().contains(friend.getUUID()), "The list is saved with the formation");
		core.distrust(owner, friend.getUUID());
		helper.assertTrue(!formation.allows(friend) && core.getTrusted().isEmpty(), "Struck off the list, they pass no more");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyCultivatorHasAnIslandOfTheirOwn(GameTestHelper helper) {
		net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
		// Two players whose ids landed on the same island before islands were handed out (hash modulo 2048).
		UUID a = UUID.randomUUID(), b;
		do {
			b = UUID.randomUUID();
		} while (Math.floorMod(b.hashCode(), 2048) != Math.floorMod(a.hashCode(), 2048));
		BlockPos islandA = InnerRealm.islandCentre(server, a);
		helper.assertTrue(!islandA.equals(InnerRealm.islandCentre(server, b)), "Two cultivators never share an island");
		helper.assertTrue(islandA.equals(InnerRealm.islandCentre(server, a)), "A cultivator's island stays where it is");
		helper.succeed();
	}

	private static int countBarriers(ServerLevel level, BlockPos center, int radius) {
		return countBarriers(level, center, radius, false);
	}

	/**
	 * Barrier blocks in the box round {@code center}; {@code strayOnly}: just those on no raised formation's shell, left behind
	 * (the tests beside this one raise barriers of their own close by, and one of theirs reaching into the box failed it now and then).
	 */
	private static int countBarriers(ServerLevel level, BlockPos center, int radius, boolean strayOnly) {
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius - 1, -radius - 1, -radius - 1), center.offset(radius + 1, radius + 1, radius + 1))) {
			if (level.getBlockState(pos).is(ModBlocks.SECT_BARRIER) && (!strayOnly || Formations.at(level, pos) == null)) count++;
		}
		return count;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sealsCarryAVeinsQiToTheCore(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		for (int x = 1; x <= 7; x++) helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
		helper.setBlock(new BlockPos(1, 2, 2), ModBlocks.FORMATION_CORE);
		helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.SEAL.defaultBlockState().setValue(SealBlock.FACE, Direction.DOWN));
		helper.setBlock(new BlockPos(3, 2, 2), ModBlocks.SEAL.defaultBlockState().setValue(SealBlock.FACE, Direction.DOWN));
		helper.setBlock(new BlockPos(4, 2, 2), ModBlocks.QI_VEIN);
		helper.setBlock(new BlockPos(7, 2, 2), ModBlocks.QI_VEIN); // not touching the line
		FormationCoreBlockEntity core = (FormationCoreBlockEntity) level.getBlockEntity(helper.absolutePos(new BlockPos(1, 2, 2)));
		core.scanNetwork(level);
		helper.assertTrue(core.getVeins() == 1, "One vein on the line counts; the loose one doesn't (got " + core.getVeins() + ")");
		helper.assertTrue(core.supply() == QiVeinBlock.QI_PER_SECOND, "Each vein feeds the core its share");
		helper.setBlock(new BlockPos(3, 2, 2), Blocks.AIR);
		core.scanNetwork(level);
		helper.assertTrue(core.getVeins() == 0, "Breaking the line cuts the vein off");
		helper.succeed();
	}

	/** Runs {@code then} once a barrier fills {@code cell}, or after {@code ticks} ticks without it (for it to say so). */
	private static void whenFilled(GameTestHelper helper, BlockPos cell, int ticks, Runnable then) {
		if (ticks <= 0 || helper.getLevel().getBlockState(cell).is(ModBlocks.SECT_BARRIER)) then.run();
		else helper.runAfterDelay(1, () -> whenFilled(helper, cell, ticks - 1, then));
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void barrierLetsMembersThroughAndHoldsTheWeak(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 3, 4));
		UUID sectId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		Formation formation = Formations.create(level, id, Formation.Kind.CORE, center, Formations.MIN_RADIUS);
		Formations.update(level, formation, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, sectId);
		Formations.raise(level, formation);
		BlockPos cell = center.offset(Formations.MIN_RADIUS, 0, 0);
		// Up within a tick or two, or a while longer: the level's other formations share the budget, and the test world keeps those
		// left by earlier runs, every one rising again (first in line) once the server starts.
		whenFilled(helper, cell, 80, () -> {
			BlockState state = level.getBlockState(cell);
			helper.assertTrue(state.is(ModBlocks.SECT_BARRIER), "The shell is filled");
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			CultivatorNpc member = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(2, 2, 1));
			Sect sect = new Sect(sectId);
			member.joinSect(sect, 0);
			helper.assertTrue(!state.getCollisionShape(level, cell, CollisionContext.of(zombie)).isEmpty(), "An outsider meets a wall");
			helper.assertTrue(state.getCollisionShape(level, cell, CollisionContext.of(member)).isEmpty(), "A member of the sect walks through");
			ServerPlayer player = helper.makeMockServerPlayerInLevel();
			helper.assertTrue(Formations.breakTicks(player, formation) < 0, "A mortal can't break a Core Formation master's barrier");
			CultivationManager.get(player).setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			int ticks = Formations.breakTicks(player, formation);
			helper.assertTrue(ticks > 0 && ticks < Formations.BREAK_TICKS_AT_EQUAL, "A stronger cultivator breaks it, faster the stronger (" + ticks + ")");
			Formations.dissolve(level, id);
			helper.assertTrue(!level.getBlockState(cell).is(ModBlocks.SECT_BARRIER), "Dissolving the formation clears it");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void shellGeometryIsOneBlockThick(GameTestHelper helper) {
		int r = 10;
		int[] shell = Formation.shell(r);
		java.util.Set<Long> cells = new java.util.HashSet<>();
		for (int packed : shell) {
			int dx = Formation.dx(packed), dy = Formation.dy(packed), dz = Formation.dz(packed);
			helper.assertTrue(Formation.isShell(dx, dy, dz, r), "Every cell lies on the shell");
			helper.assertTrue(cells.add(BlockPos.asLong(dx, dy, dz)), "No cell twice");
		}
		int brute = 0;
		for (int x = -r - 1; x <= r + 1; x++) for (int y = -r - 1; y <= r + 1; y++) for (int z = -r - 1; z <= r + 1; z++) {
			if (Formation.isShell(x, y, z, r)) brute++;
		}
		helper.assertTrue(brute == shell.length, "The column walk finds every shell cell (" + shell.length + " of " + brute + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void groundIsFoundThroughABarrierDome(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos floor = helper.absolutePos(new BlockPos(1, 1, 1));
		level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
		level.setBlockAndUpdate(floor.above(6), ModBlocks.SECT_BARRIER.defaultBlockState());
		int vanilla = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, floor.getX(), floor.getZ());
		int ground = Formations.surface(level, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, floor.getX(), floor.getZ());
		helper.assertTrue(vanilla == floor.getY() + 7, "Vanilla's heightmap stops at the barrier (" + vanilla + ")");
		helper.assertTrue(ground == floor.getY() + 1, "Looking through it finds the stone (" + ground + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noSpringCanOpenInASectsWalls(GameTestHelper helper) {
		// Springs are placed after structures: one in a hall's wall would pour water or lava over the grounds.
		List<net.minecraft.core.HolderSet<net.minecraft.world.level.block.Block>> springs = new ArrayList<>();
		helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.CONFIGURED_FEATURE).entrySet().forEach(entry -> {
			if (entry.getValue().config() instanceof net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration spring
					&& !entry.getKey().location().getPath().contains("nether")) springs.add(spring.validBlocks); // no sect in the Nether
		});
		helper.assertTrue(springs.size() >= 2, "Found the water and lava springs (" + springs.size() + ")");
		for (Alignment.Faction faction : Alignment.Faction.values()) {
			for (boolean upper : new boolean[] {false, true}) {
				SectPalette p = SectPalette.of(faction, upper);
				net.minecraft.world.level.block.Block[] blocks = {p.foundation, p.paving, p.path, p.wallBase, p.wall, p.pillar, p.beam, p.floor, p.roof,
						p.roofTrim, p.accent, p.window, p.roofStairs, p.baseStairs, p.roofSlab, p.baseSlab, p.baseWall, p.trimStairs, p.trimSlab};
				for (net.minecraft.world.level.block.Block block : blocks) {
					for (var valid : springs) {
						helper.assertTrue(!block.defaultBlockState().is(valid), faction + (upper ? " (Upper Realm)" : "") + " builds with "
								+ net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block) + ", which a spring can open in");
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyMeditationHallHasMats(GameTestHelper helper) {
		// The meditation halls of SectStructure's three sizes, across the front by front to back, as plan() lays them.
		int[][] halls = {{19, 13}, {19, 15}, {19, 15}};
		int[] expected = {12, 18, 18};
		for (int i = 0; i < halls.length; i++) {
			int w = halls[i][0], d = halls[i][1];
			SectPiece hall = new SectPiece(SectPiece.Kind.MEDITATION_HALL, new net.minecraft.world.level.levelgen.structure.BoundingBox(0, 0, 0, d - 1, 18, w - 1),
					Direction.EAST, 0, Alignment.Faction.NEUTRAL, false, null);
			int mats = hall.meditationMats().length;
			helper.assertTrue(mats == expected[i], "A " + w + "x" + d + " hall seats " + expected[i] + " (" + mats + ")");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theStrongestOuterDisciplesKeepTheGates(GameTestHelper helper) {
		Sect sect = new Sect(UUID.randomUUID());
		List<Sect.Member> outer = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			Sect.Member m = new Sect.Member(UUID.randomUUID(), i, i);
			m.title = NpcTitle.OUTER_DISCIPLE;
			sect.members.put(m.id, m);
			outer.add(m);
		}
		Sect.Member inner = new Sect.Member(UUID.randomUUID(), 9, 9);
		inner.title = NpcTitle.INNER_DISCIPLE;
		sect.members.put(inner.id, inner);
		helper.assertTrue(sect.guardPost(outer.get(5)) == null, "A sect laid out without gate-guard posts has no gate guards");
		for (int i = 0; i < 4; i++) sect.posts(Sect.Post.GATE_GUARD).add(new BlockPos(i, 0, 0));
		for (int rank = 5; rank >= 2; rank--) {
			helper.assertTrue(new BlockPos(5 - rank, 0, 0).equals(sect.guardPost(outer.get(rank))), "The strongest outer disciples take the posts in order");
			helper.assertTrue(sect.guardPost(outer.get(rank)).equals(sect.dutyPost(outer.get(rank))), "A gate guard's duty is its gate");
		}
		helper.assertTrue(sect.guardPost(outer.get(1)) == null && sect.guardPost(outer.get(0)) == null, "Once the posts are filled the rest patrol");
		helper.assertTrue(sect.guardPost(inner) == null, "Only outer disciples keep the gates");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everySectHousesItsMembersInsideItsBarrier(GameTestHelper helper) {
		// Every size, a run of seeds and every facing, as worldgen lays them out: a house for every elder and no disciples' huts
		// (the disciples live on the meditation hall's mats), a home for every member but the master, no two buildings on the
		// same ground, every one of them inside the barrier, and a way in through the gate.
		for (int size = 0; size < 3; size++) {
			for (long seed = 0; seed < 8; seed++) {
				for (Direction facing : Direction.Plane.HORIZONTAL) {
					List<net.minecraft.world.level.levelgen.structure.StructurePiece> pieces = SectStructure.layOut(seed * 7919 + size, size, facing, seed % 2 == 0);
					String sect = "size " + size + ", seed " + seed + ", facing " + facing;
					List<SectPiece> buildings = new ArrayList<>();
					net.minecraft.nbt.CompoundTag blueprint = null;
					BlockPos core = null;
					int houses = 0, huts = 0, altars = 0, yards = 0;
					for (var piece : pieces) {
						SectPiece sp = (SectPiece) piece;
						if (sp.kind() == SectPiece.Kind.TERRACE) continue;
						if (sp.kind() == SectPiece.Kind.ELDER_HOUSE) houses++;
						if (sp.kind() == SectPiece.Kind.DWELLING) huts++;
						if (sp.kind() == SectPiece.Kind.ALTAR) altars++;
						if (sp.kind() == SectPiece.Kind.TRAINING) yards++;
						if (sp.kind() == SectPiece.Kind.PAGODA) {
							// Every storey it was planned with fits, so its top chamber (and the mat in it) is on the top floor.
							int tiers = sp.extra().getInt("Tiers");
							helper.assertTrue(sp.pagodaSeat()[1] == 1 + 5 * (tiers - 1), "The pagoda raises all " + tiers + " storeys (" + sect + ")");
						}
						if (sp.kind() == SectPiece.Kind.PLAZA) {
							blueprint = sp.extra().getCompound("Blueprint");
							core = sp.toWorld(SectPiece.PLAZA_RADIUS, 2, SectPiece.PLAZA_RADIUS);
						}
						buildings.add(sp);
					}
					helper.assertTrue(blueprint != null, "A plaza with the sect's blueprint (" + sect + ")");
					EnumMap<NpcTitle, Integer> titles = Sect.titleCounts(SectStructure.maxMembers(size));
					int elders = titles.get(NpcTitle.GRAND_ELDER) + titles.get(NpcTitle.ELDER);
					helper.assertTrue(houses == elders && huts == 0, "A house for each elder and no huts (" + houses + " houses, " + huts + " huts, " + sect + ")");
					int homes = blueprint.getCompound("Posts").getLongArray("DWELLING").length;
					helper.assertTrue(homes >= SectStructure.maxMembers(size) - 1, "A home for every member but the master (" + homes + ", " + sect + ")");
					for (int i = 0; i < buildings.size(); i++) {
						for (int j = i + 1; j < buildings.size(); j++) {
							// The plaza is round: houses may stand in the corners of its square.
							if (buildings.get(i).kind() == SectPiece.Kind.PLAZA || buildings.get(j).kind() == SectPiece.Kind.PLAZA) continue;
							helper.assertTrue(!buildings.get(i).getBoundingBox().intersects(buildings.get(j).getBoundingBox()),
									buildings.get(i).kind() + " and " + buildings.get(j).kind() + " overlap (" + sect + ")");
						}
					}
					int radius = blueprint.getInt("Radius");
					helper.assertTrue(radius <= Formations.MAX_RADIUS, "The barrier is no larger than any (" + radius + ", " + sect + ")");
					int gates = 0;
					for (SectPiece building : buildings) {
						var box = building.getBoundingBox();
						if (building.kind() == SectPiece.Kind.GATE || building.kind() == SectPiece.Kind.TORII) {
							// The gates stand wholly outside the barrier: even the nearest point of each is beyond it.
							gates++;
							double nx = Math.max(box.minX(), Math.min(core.getX(), box.maxX())), nz = Math.max(box.minZ(), Math.min(core.getZ(), box.maxZ()));
							helper.assertTrue(Math.sqrt(Math.pow(nx - core.getX(), 2) + Math.pow(nz - core.getZ(), 2)) > radius + 1,
									building.kind() + " stands inside the barrier (" + sect + ")");
							continue;
						}
						for (int[] c : new int[][] {{box.minX(), box.minZ()}, {box.maxX(), box.minZ()}, {box.minX(), box.maxZ()}, {box.maxX(), box.maxZ()}}) {
							double reach = Math.sqrt(Math.pow(c[0] - core.getX(), 2) + Math.pow(c[1] - core.getZ(), 2));
							helper.assertTrue(reach <= radius, building.kind() + " reaches outside the barrier (" + sect + ")");
						}
					}
					helper.assertTrue(gates == 3, "A gate and two torii (" + gates + ", " + sect + ")");
					// The Altar of Heaven, for the Ritual to the Heavens: its altar block and the spot to kneel before it.
					helper.assertTrue(altars == 1 && blueprint.getCompound("Posts").getLongArray("ALTAR").length == 2,
							"An Altar of Heaven (" + altars + ", " + sect + ")");
					// The training ground: its sparring ring and four drill spots; and points along the ways to walk by.
					helper.assertTrue(yards == 1 && blueprint.getCompound("Posts").getLongArray("TRAINING").length == 5,
							"A training ground (" + yards + ", " + sect + ")");
					helper.assertTrue(blueprint.getCompound("Posts").getLongArray("WAY").length >= 8, "Way-points along the ways (" + sect + ")");
					helper.assertTrue(blueprint.getCompound("Posts").getLongArray("GATE").length == 1, "A gate post (" + sect + ")");
					long[] guards = blueprint.getCompound("Posts").getLongArray("GATE_GUARD");
					helper.assertTrue(guards.length == 6, "Six gate-guard posts (" + guards.length + ", " + sect + ")");
					for (long g : guards) {
						BlockPos guard = BlockPos.of(g);
						helper.assertTrue(Math.sqrt(Math.pow(guard.getX() - core.getX(), 2) + Math.pow(guard.getZ() - core.getZ(), 2)) > radius + 1,
								"A gate guard stands outside the barrier (" + sect + ")");
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGroundsFollowTheLand(GameTestHelper helper) {
		// The grounds lie on the land as it lies, not levelled (the user's ask, 2026-10-25, as Minecraft's villages do): open ground keeps
		// its own floor and what grows on it (podzol under a fern, low); a way is paved on its own column's top, whatever its height (a
		// knoll of dirt three up), and over water as a causeway of the same stone.
		ServerLevel level = helper.getLevel();
		SectPiece grounds = null;
		for (var piece : SectStructure.layOut(17, 0, Direction.NORTH, false)) {
			if (((SectPiece) piece).kind() == SectPiece.Kind.TERRACE) grounds = (SectPiece) piece;
		}
		helper.assertTrue(grounds != null && grounds.extra().getBoolean("Follow"), "A sect has grounds that follow the land");
		int w = grounds.width();
		byte[] map = grounds.extra().getByteArray("Map");
		// Open ground, then two paved columns of a way just beyond it, in one row.
		int open = -1, way = -1;
		for (int i = 0; i + 5 < map.length && open < 0; i++) {
			if (map[i] != SectLayout.GRASS) continue;
			for (int k = 1; k <= 3; k++) {
				if ((i + k) % w + 1 < w && (i + k) / w == i / w && map[i + k] == SectLayout.PATH && map[i + k + 1] == SectLayout.PATH) {
					open = i;
					way = i + k;
					break;
				}
			}
		}
		helper.assertTrue(open >= 0, "Open ground beside a way on the grounds");
		// The grounds moved so those columns come down in the test's space.
		BlockPos ground = helper.absolutePos(new BlockPos(1, 6, 2)), first = grounds.toWorld(open % w, 0, open / w);
		grounds.move(ground.getX() - first.getX(), ground.getY() - first.getY(), ground.getZ() - first.getZ());
		BlockPos[] columns = {grounds.toWorld(open % w, 0, open / w), grounds.toWorld(way % w, 0, way / w), grounds.toWorld(way % w + 1, 0, way / w)};
		try {
			for (int dy = -5; dy <= -3; dy++) level.setBlock(columns[0].above(dy), Blocks.STONE.defaultBlockState(), 2);
			level.setBlock(columns[0].below(2), Blocks.PODZOL.defaultBlockState(), 2);
			level.setBlock(columns[0].below(), Blocks.FERN.defaultBlockState(), 2);
			for (int dy = -5; dy <= 2; dy++) level.setBlock(columns[1].above(dy), Blocks.DIRT.defaultBlockState(), 2);
			for (int dy = -5; dy <= -4; dy++) level.setBlock(columns[2].above(dy), Blocks.STONE.defaultBlockState(), 2);
			for (int dy = -3; dy <= -1; dy++) level.setBlock(columns[2].above(dy), Blocks.WATER.defaultBlockState(), 2);
			var box = net.minecraft.world.level.levelgen.structure.BoundingBox.encapsulatingPositions(
					List.of(columns[0].below(20), columns[2].above(45))).orElseThrow();
			grounds.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box,
					new net.minecraft.world.level.ChunkPos(ground), ground);
			helper.assertTrue(level.getBlockState(columns[0].below(2)).is(Blocks.PODZOL) && level.getBlockState(columns[0].below()).is(Blocks.FERN),
					"Open ground keeps its floor and its fern where they lie (" + level.getBlockState(columns[0].below(2)) + ")");
			BlockState knoll = level.getBlockState(columns[1].above(2)), causeway = level.getBlockState(columns[2].below());
			helper.assertTrue(!knoll.is(Blocks.DIRT) && level.getBlockState(columns[1].above(3)).isAir() && level.getBlockState(columns[1].above(1)).is(Blocks.DIRT),
					"The way is paved on the knoll's own top (" + knoll + ")");
			helper.assertTrue(causeway.getBlock() == knoll.getBlock() && level.getFluidState(columns[2].below(2)).isSource(),
					"Over water the way is a causeway of its own stone (" + causeway + ")");
			helper.succeed();
		} finally {
			for (BlockPos column : columns) {
				for (int dy = -5; dy <= 26; dy++) level.setBlock(column.above(dy), Blocks.AIR.defaultBlockState(), 2);
			}
		}
	}

	// --- Clothing ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void clothingBuffsAndTheFlightInvariant(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			player.setItemSlot(type.getSlot(), ClothingItem.create(ModItems.clothing(ClothingTier.IMMORTAL, type), ClothingStyle.RIGHTEOUS));
		}
		double qi = ClothingItem.qiGatherBonus(player);
		helper.assertTrue(Math.abs(qi - 0.60) < 1.0e-9, "A full Immortal set gathers +60% qi (got " + qi + ")");
		double nsGather = CultivationStats.qiGather(Realm.NASCENT_SOUL, Stage.GRAND_PERFECTION) * (1 + qi);
		helper.assertTrue(nsGather < Realm.NASCENT_SOUL.getQiFlightCost(), "Even so, Nascent Soul flight stays finite in the lower realms");
		helper.assertTrue(ClothingItem.meditationBonus(player) > 0.4, "And meditation goes faster");
		for (ArmorItem.Type type : ArmorItem.Type.values()) {
			ClothingItem item = (ClothingItem) ModItems.clothing(ClothingTier.EARTH, type);
			ArmorItem diamond = (ArmorItem) switch (type) {
				case HELMET -> Items.DIAMOND_HELMET;
				case CHESTPLATE -> Items.DIAMOND_CHESTPLATE;
				case LEGGINGS -> Items.DIAMOND_LEGGINGS;
				case BOOTS -> Items.DIAMOND_BOOTS;
			};
			helper.assertTrue(item.getDefense() == diamond.getDefense() && item.getToughness() == diamond.getToughness(),
					"Earth grade protects like diamond, no more (tribulations are tuned on vanilla armor)");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void clothingDyesAndUpgrades(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack robe = ClothingItem.create(ModItems.clothing(ClothingTier.MORTAL, ArmorItem.Type.CHESTPLATE), ClothingStyle.NEUTRAL);

		TransientCraftingContainer grid = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
		grid.setItem(0, robe.copy());
		grid.setItem(1, new ItemStack(Items.RED_DYE));
		grid.setItem(2, new ItemStack(Items.BLACK_DYE));
		ClothingRecipes.DyeRecipe dye = new ClothingRecipes.DyeRecipe(DefyingTheHeavens.id("test_dye"), net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
		helper.assertTrue(dye.matches(grid, helper.getLevel()), "Red and black dye makes devilish robes");
		helper.assertTrue(ClothingItem.style(dye.assemble(grid, helper.getLevel().registryAccess())) == ClothingStyle.DEVILISH, "... red and black");

		TransientCraftingContainer up = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
		ItemStack devilish = ClothingItem.create(ModItems.clothing(ClothingTier.MORTAL, ArmorItem.Type.CHESTPLATE), ClothingStyle.DEVILISH);
		up.setItem(0, devilish);
		up.setItem(1, new ItemStack(ModItems.SPIRIT_DEW));
		up.setItem(2, new ItemStack(ModItems.SPIRIT_DEW));
		up.setItem(3, new ItemStack(Items.GOLD_INGOT));
		ClothingRecipes.UpgradeRecipe upgrade = new ClothingRecipes.UpgradeRecipe(DefyingTheHeavens.id("test_upgrade"), net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
		helper.assertTrue(upgrade.matches(up, helper.getLevel()), "Spirit Dew and gold raise a Mortal robe");
		ItemStack result = upgrade.assemble(up, helper.getLevel().registryAccess());
		helper.assertTrue(result.getItem() == ModItems.clothing(ClothingTier.SPIRIT, ArmorItem.Type.CHESTPLATE) && ClothingItem.style(result) == ClothingStyle.DEVILISH,
				"... to Spirit grade, keeping its colours");
		up.setItem(3, new ItemStack(Items.IRON_INGOT));
		helper.assertTrue(!upgrade.matches(up, helper.getLevel()), "The wrong materials don't");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void wallPillarsStandEvenlyAndMirroredRoundTheDoor(GameTestHelper helper) {
		for (int length = 4; length <= 30; length++) {
			for (int bay : new int[] {2, 3, 4}) {
				for (boolean front : new boolean[] {true, false}) {
					int[] spots = SectPiece.pillarSpots(length, bay, front);
					String what = "length " + length + ", bay " + bay + (front ? ", front" : ", side") + ": " + java.util.Arrays.toString(spots);
					helper.assertTrue(spots[0] == 0 && spots[spots.length - 1] == length, "A pillar at each corner (" + what + ")");
					int bays = spots.length - 1;
					for (int i = 1; i <= bays; i++) helper.assertTrue(spots[i] > spots[i - 1], "In order, apart (" + what + ")");
					for (int i = 0; i <= bays; i++) {
						if (2 * i != bays) helper.assertTrue(spots[i] == length - spots[bays - i], "Mirrored about the middle (" + what + ")");
					}
					if (front) {
						helper.assertTrue(bays % 2 == 1 && java.util.Arrays.binarySearch(spots, length / 2) < 0,
								"The door, in the middle of the front, is in a bay's middle, not on or beside one pillar only (" + what + ")");
					}
				}
			}
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void walkersGoByTheWaysEvenWhereTheyFirstLeadAway(GameTestHelper helper) {
		// An L of way points round a hall: from its door (the L's start) to a gate past the far end.
		java.util.List<BlockPos> ways = java.util.List.of(new BlockPos(0, 0, 0), new BlockPos(10, 0, 0), new BlockPos(20, 0, 0),
				new BlockPos(20, 0, 10), new BlockPos(20, 0, 20));
		net.minecraft.world.phys.Vec3 gate = new net.minecraft.world.phys.Vec3(20.5, 0, 25.5);
		double[] cost = CultivatorGoals.alongTheWays(ways, gate);
		for (int i = 1; i < ways.size(); i++) helper.assertTrue(cost[i] < cost[i - 1], "Each point is nearer the gate along the ways: "
				+ java.util.Arrays.toString(cost));
		helper.assertTrue(cost[0] > net.minecraft.world.phys.Vec3.atBottomCenterOf(ways.get(0)).distanceTo(gate) + 1,
				"Reckoned along the ways, not as the crow flies (through the hall): " + cost[0]);
		helper.succeed();
	}

	/**
	 * A walker in the back corner of a closed hall, its goal a few blocks behind the back wall, the way there out of the door
	 * at the front and round: more than vanilla's 32 blocks of walking, so its own search only ever reached the corner nearest
	 * the goal (where gate guards stood for good). The far search finds the way (CultivatorNavigation, CultivatorGoals#walkToward).
	 * On a floor high over its spot, in a batch of its own.
	 */
	@GameTest(template = EMPTY_STRUCTURE, batch = "sect_hall_way_out", timeoutTicks = 60)
	public void aWalkerFindsTheWayOutOfAHallAndRoundIt(GameTestHelper helper) {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 24, 8);
		ServerLevel level = helper.getLevel();
		BlockPos c = floor.centre();
		CultivatorNpc npc = null;
		try {
			// The hall: stone walls 4 high under a roof, 17 across and 11 deep inside, a door 3 wide in the middle of its front (north).
			int x0 = c.getX() - 9, x1 = c.getX() + 9, z0 = c.getZ() - 8, z1 = c.getZ() + 4;
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
					for (int y = 0; y < 4; y++) if (wall) level.setBlock(new BlockPos(x, c.getY() + y, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
					level.setBlock(new BlockPos(x, c.getY() + 4, z), Blocks.STONE_BRICKS.defaultBlockState(), 2);
				}
			}
			for (int x = c.getX() - 1; x <= c.getX() + 1; x++) {
				for (int y = 0; y < 3; y++) level.setBlock(new BlockPos(x, c.getY() + y, z0), Blocks.AIR.defaultBlockState(), 2);
			}
			npc = ModEntities.CULTIVATOR.create(level);
			npc.moveTo(x1 - 0.5, c.getY(), z1 - 0.5, 0, 0);
			npc.setNoAi(true);
			level.addFreshEntity(npc);
			npc.setOnGround(true);
			BlockPos goal = new BlockPos(c.getX() + 5, c.getY(), c.getZ() + 14);
			net.minecraft.world.level.pathfinder.Path straight = npc.getNavigation().createPath(goal, 1);
			helper.assertTrue(straight == null || !straight.canReach(), "Vanilla's own search ends short of the goal (the corner nearest it)");
			CultivatorGoals.walkToward(npc, Vec3.atBottomCenterOf(goal), 1.0);
			net.minecraft.world.level.pathfinder.Path path = npc.getNavigation().getPath();
			helper.assertTrue(path != null && path.canReach(), "The walker's way reaches the goal");
			boolean byTheDoor = false;
			for (int i = 0; i < path.getNodeCount(); i++) {
				BlockPos node = path.getNodePos(i);
				if (node.getZ() == z0 && Math.abs(node.getX() - c.getX()) <= 1) byTheDoor = true;
			}
			helper.assertTrue(byTheDoor, "...out by the door");
			helper.succeed();
		} finally {
			if (npc != null) npc.discard();
			floor.remove();
		}
	}
}
