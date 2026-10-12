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
		intruder.becomeRogue(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), -150, helper.getLevel().getRandom());
		intruder.setNoAi(true);
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
		// Being in a fight is worked out on the elder's own tick.
		helper.runAfterDelay(5, () -> {
			helper.assertTrue(elder.shouldFlee(), "Even an elder falls back once it is losing badly");
			helper.assertTrue(!master.shouldFlee(), "The master never runs: it makes its last stand");
			helper.succeed();
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
		helper.assertTrue(Alignment.factionOf(-51) == Alignment.Faction.DEMONIC && Alignment.factionOf(-50) == Alignment.Faction.NEUTRAL
				&& Alignment.factionOf(50) == Alignment.Faction.NEUTRAL && Alignment.factionOf(51) == Alignment.Faction.RIGHTEOUS, "Thresholds at +-50");
		helper.assertTrue(Alignment.killShift(-150, true, Realm.CORE_FORMATION, false) > 0, "Slaying the demonic raises alignment");
		helper.assertTrue(Alignment.killShift(0, false, Realm.QI_REFINING, false) < 0, "Killing a peaceful neutral lowers it");
		helper.assertTrue(Alignment.killShift(120, true, Realm.QI_REFINING, false) == 0, "Killing one who attacked first changes nothing");
		helper.assertTrue(Alignment.Faction.RIGHTEOUS.isEnemyOf(Alignment.Faction.DEMONIC) && !Alignment.Faction.NEUTRAL.isEnemyOf(Alignment.Faction.DEMONIC),
				"Righteous and demonic are enemies; the neutral are nobody's");
		helper.assertTrue(Alignment.clamp(999) == Alignment.MAX && Alignment.clamp(-999) == Alignment.MIN, "Alignment stays within -200..200");
		helper.succeed();
	}

	// --- NPC cultivation ---

	@GameTest(template = EMPTY_STRUCTURE)
	public void npcsSkipTribulationsButKeepTheLowerRealmCap(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION));
		npc.advance(1_000);
		helper.assertTrue(npc.getRank() >= PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), "A bottleneck gives way without a tribulation or pill");
		npc.advance(1.0e12);
		helper.assertTrue(npc.getRank() == PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION), "The lower realms hold it at HB Grand Perfection");
		CultivatorNpc fresh = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
		fresh.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		PlayerCultivation player = new PlayerCultivation();
		player.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		helper.assertTrue(Math.abs(fresh.meditationRate() * 10 - player.cultivationPerSecond()) < 1.0e-9, "NPCs cultivate ten times slower than players");
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
			helper.assertTrue(countBarriers(level, core, Formations.MIN_RADIUS) == 0, "Breaking the core takes every barrier block with it at once");
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

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
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
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius - 1, -radius - 1, -radius - 1), center.offset(radius + 1, radius + 1, radius + 1))) {
			if (level.getBlockState(pos).is(ModBlocks.SECT_BARRIER)) count++;
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

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void barrierLetsMembersThroughAndHoldsTheWeak(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos center = helper.absolutePos(new BlockPos(4, 3, 4));
		UUID sectId = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		Formation formation = Formations.create(level, id, Formation.Kind.CORE, center, Formations.MIN_RADIUS);
		Formations.update(level, formation, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, sectId);
		Formations.raise(level, formation);
		helper.runAfterDelay(3, () -> {
			BlockPos cell = center.offset(Formations.MIN_RADIUS, 0, 0);
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
		helper.assertTrue(dye.matches(grid, helper.getLevel()), "Red and black dye makes demonic robes");
		helper.assertTrue(ClothingItem.style(dye.assemble(grid, helper.getLevel().registryAccess())) == ClothingStyle.DEMONIC, "... red and black");

		TransientCraftingContainer up = new TransientCraftingContainer(player.inventoryMenu, 3, 3);
		ItemStack demonic = ClothingItem.create(ModItems.clothing(ClothingTier.MORTAL, ArmorItem.Type.CHESTPLATE), ClothingStyle.DEMONIC);
		up.setItem(0, demonic);
		up.setItem(1, new ItemStack(ModItems.SPIRIT_DEW));
		up.setItem(2, new ItemStack(ModItems.SPIRIT_DEW));
		up.setItem(3, new ItemStack(Items.GOLD_INGOT));
		ClothingRecipes.UpgradeRecipe upgrade = new ClothingRecipes.UpgradeRecipe(DefyingTheHeavens.id("test_upgrade"), net.minecraft.world.item.crafting.CraftingBookCategory.EQUIPMENT);
		helper.assertTrue(upgrade.matches(up, helper.getLevel()), "Spirit Dew and gold raise a Mortal robe");
		ItemStack result = upgrade.assemble(up, helper.getLevel().registryAccess());
		helper.assertTrue(result.getItem() == ModItems.clothing(ClothingTier.SPIRIT, ArmorItem.Type.CHESTPLATE) && ClothingItem.style(result) == ClothingStyle.DEMONIC,
				"... to Spirit grade, keeping its colours");
		up.setItem(3, new ItemStack(Items.IRON_INGOT));
		helper.assertTrue(!upgrade.matches(up, helper.getLevel()), "The wrong materials don't");
		helper.succeed();
	}
}
