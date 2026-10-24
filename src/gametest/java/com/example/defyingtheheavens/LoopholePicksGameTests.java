package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The loopholes the user chose to close after the second sweep (2026-10-24): a tribulation's strikes past every ward, bell and rebirth;
 * a Jade Poison Body's pills paying their years; a settlement's fortune shared no further than the axis wants; the void's beasts and
 * the gap's storms reaching the dead, and a heart demon's blows landing on them.
 */
public class LoopholePicksGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper, String name) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		return p;
	}

	/**
	 * A survival player in the server's list (a spar needs one: SparSystem#tick), at the test's spot, of {@code realm}; taken off again
	 * with {@link #gone}. As in TalkPhasesGameTests.
	 */
	private static ServerPlayer survivor(GameTestHelper helper, String name, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void gone(ServerPlayer p) {
		SparSystem.abandon(p);
		p.server.getPlayerList().remove(p);
	}

	/** TribulationManager's strike landing on {@code p} (or none): as its strike sets it. */
	private static void striking(ServerPlayer p) throws ReflectiveOperationException {
		java.lang.reflect.Field field = TribulationManager.class.getDeclaredField("striking");
		field.setAccessible(true);
		field.set(null, p == null ? null : p.getUUID());
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "loophole_picks_strikes")
	public void aTribulationsStrikePassesEveryWard(GameTestHelper helper) throws ReflectiveOperationException {
		HeavenSpellTesting.Floor floor = HeavenSpellTesting.floor(helper, 6, 6);
		ServerPlayer p = HeavenSpellTesting.caster(helper, HeavenMethod.values()[0], floor.centre());
		try {
			SpellAuras.put(p, SpellAura.MOUNTAIN_STANCE, 200);
			DamageSource magic = helper.getLevel().damageSources().magic();
			float warded = HeavenSpells.incoming(p, magic, 10);
			striking(p);
			float struck = HeavenSpells.incoming(p, magic, 10);
			helper.assertTrue(warded < 9.9f && Math.abs(struck - 10) < 1.0e-4, "A ward takes from a blow (" + warded + "), not from a tribulation's strike (" + struck + ")");
			helper.assertTrue(HeavenSpells.allowDeath(p, magic), "No rebirth or ward holds off a tribulation's killing strike");
			helper.succeed();
		} finally {
			striking(null);
			SpellAuras.forget(p);
			HeavenSpellTesting.cleanUp(p, HeavenSpellTesting.spawned(), floor);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aJadePoisonBodysPillsPayTheirYears(GameTestHelper helper) {
		ServerPlayer jade = player(helper, "loophole-jade"), plain = player(helper, "loophole-plain");
		for (ServerPlayer p : new ServerPlayer[] {jade, plain}) {
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			c.setMortal(false);
			c.setAge(20);
		}
		CultivationManager.own(jade).setPhysique(Physique.JADE_POISON_BODY);
		CultivationManager.own(plain).setPhysique(Physique.FIVE_ELEMENT_ROOTS);
		PlayerCultivation j = CultivationManager.own(jade), q = CultivationManager.own(plain);
		double dose = j.cultivationRequired() * 0.5;
		Physiques.refinedAtOnce(jade, j, () -> j.takeMedicine(dose));
		Physiques.refinedAtOnce(plain, q, () -> q.takeMedicine(dose));
		helper.assertTrue(j.getCultivation() > 0 && j.age() > 20, "A Jade Poison Body refines a pill at once, and pays its years (age " + j.age() + ")");
		helper.assertTrue(q.getCultivation() == 0 && q.getMedicinalQi() > 0 && q.age() == 20, "Another's waits for meditation, which pays them then");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSettlementsFortuneIsSharedNoFurtherThanTheAxisWants(GameTestHelper helper) throws ReflectiveOperationException {
		ServerPlayer p = player(helper, "loophole-fortune");
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		try {
			npc.setNoAi(true);
			PlayerCultivation c = CultivationManager.own(p);
			int need = FourAxis.need(c, FourAxis.Axis.FORTUNE);
			// What lapses: a tenth of the axis' need a day.
			c.setFortuneOwed(need / 2.0, 10);
			helper.assertTrue(Math.abs(Wishes.owedFortune(c, 10) - need / 2.0) < 1e-9 && Math.abs(Wishes.owedFortune(c, 15)) < 1e-9,
					"What is owed lapses over ten days");
			java.lang.reflect.Method give = Wishes.class.getDeclaredMethod("give", ServerPlayer.class, CultivatorNpc.class, CompoundTag.class);
			give.setAccessible(true);
			CompoundTag wish = new CompoundTag();
			wish.putInt("Axis", FourAxis.Axis.FORTUNE.ordinal());
			wish.putString("Item", "minecraft:bread");
			wish.putInt("Count", 1);
			long day = helper.getLevel().getGameTime() / HeavenRitual.DAY;
			c.setFortuneOwed(0, day);
			p.getInventory().add(new ItemStack(Items.BREAD, 2));
			give.invoke(null, p, npc, wish.copy());
			int stones = p.getInventory().countItem(ModItems.MID_SPIRIT_STONE);
			helper.assertTrue(stones >= Wishes.SHARE_STONES && Math.abs(Wishes.owedFortune(c, day) - stones * SpiritStoneItem.Grade.MID.value()) < 1e-9,
					"A share given (" + stones + "), and owed");
			c.setFortuneOwed(need, day);
			give.invoke(null, p, npc, wish.copy());
			helper.assertTrue(p.getInventory().countItem(Items.BREAD) == 1 && p.getInventory().countItem(ModItems.MID_SPIRIT_STONE) == stones,
					"Carrying enough unabsorbed to complete the axis: no more shared, and their goods left them");
			Wishes.absorbedFortune(p, 9);
			helper.assertTrue(Math.abs(Wishes.owedFortune(c, day) - (need - 9)) < 1e-9, "What one absorbs is owed no more");
			helper.succeed();
		} finally {
			npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theVoidsBeastsAndAHeartDemonReachTheDead(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		VoidVestige vestige = ModEntities.VOID_VESTIGE.create(level);
		HeartDemonEntity demon = new HeartDemonEntity(ModEntities.HEART_DEMON, level);
		net.minecraft.world.entity.monster.Zombie zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
		DamageSource bite = level.damageSources().mobAttack(vestige), blow = level.damageSources().mobAttack(demon), claw = level.damageSources().mobAttack(zombie);
		helper.assertTrue(GhostSystem.reaches(bite) && GhostSystem.scaled(bite, 8) == 8, "A Vestige's bite reaches the dead, in full");
		helper.assertTrue(GhostSystem.reaches(blow) && GhostSystem.scaled(blow, 8) == 8, "A heart demon's blow too (it landed as nothing)");
		helper.assertTrue(!GhostSystem.reaches(claw), "An ordinary mob's still passes through");
		helper.assertTrue(GhostSystem.reaches(ModDamageTypes.yinTribulation(level)), "The storms strike the dead as the Netherworld's tribulation does");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpellStopsAtABarrierItsCasterCouldNotBreak(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerPlayer p = player(helper, "loophole-walled");
		PlayerCultivation c = CultivationManager.own(p);
		c.awaken(0);
		BlockPos centre = helper.absolutePos(new BlockPos(1, 1, 1));
		net.minecraft.world.entity.monster.Zombie zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 1, 1));
		UUID id = UUID.randomUUID();
		try {
			zombie.setNoAi(true);
			zombie.setPos(centre.getX() + 0.5, centre.getY(), centre.getZ() + 0.5);
			p.setPos(centre.getX() + 9.5, centre.getY(), centre.getZ() + 0.5);
			helper.assertTrue(!HeavenSpells.spared(p, zombie), "With no barrier between them, a spell reaches it");
			Formation formation = Formations.create(level, id, Formation.Kind.values()[0], centre, Formations.MIN_RADIUS);
			Formations.update(level, formation, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), null, null);
			Formations.raise(level, formation);
			helper.assertTrue(HeavenSpells.walled(p, zombie) && HeavenSpells.spared(p, zombie),
					"Behind a raised barrier the caster couldn't break, no spell reaches it");
			helper.assertTrue(HeavenSpells.strike(p, zombie, 5, QiElement.FIRE) == 0 && zombie.getHealth() == zombie.getMaxHealth(),
					"Nor does a strike");
			p.setPos(centre.getX() + 1.5, centre.getY(), centre.getZ() + 0.5);
			helper.assertTrue(!HeavenSpells.walled(p, zombie), "On the same side of it, it does");
			p.setPos(centre.getX() + 9.5, centre.getY(), centre.getZ() + 0.5);
			p.setGameMode(GameType.CREATIVE);
			helper.assertTrue(!HeavenSpells.walled(p, zombie), "A creative caster's does");
			p.setGameMode(GameType.SURVIVAL);
			c.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
			helper.assertTrue(!HeavenSpells.walled(p, zombie), "So does one strong enough to break the barrier");
			c.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			Formations.update(level, formation, formation.getRank(), p.getUUID(), null);
			helper.assertTrue(!HeavenSpells.walled(p, zombie), "And one the barrier lets through");
			Formations.update(level, formation, formation.getRank(), null, null);
			Formations.lower(level, formation);
			helper.assertTrue(!HeavenSpells.walled(p, zombie), "A lowered barrier holds nothing back");
			helper.succeed();
		} finally {
			Formations.dissolve(level, id);
			zombie.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSectFellowFightsASparInEarnest(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Sparring Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		SectData.get(level.getServer()).put(sect);
		CultivatorNpc elder = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		ServerPlayer p = survivor(helper, "loophole-fellow", Realm.CORE_FORMATION);
		try {
			elder.setNoAi(true);
			elder.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			elder.joinSect(sect, sect.members.size());
			sect.members.put(elder.getUUID(), new Sect.Member(elder.getUUID(), elder.getRank(), sect.members.size()));
			elder.setHealth(elder.getMaxHealth());
			SectMembership.join(p, sect);
			helper.assertTrue(elder.isAlly(p) && !CultivatorGoals.CombatGoal.fights(elder, p), "It fights no fellow disciple");
			SparSystem.start(p, elder, SparSystem.Terms.FRIENDLY);
			helper.assertTrue(SparSystem.areSparring(elder, p) && CultivatorGoals.CombatGoal.fights(elder, p), "...but one it spars with, it does");
			helper.succeed();
		} finally {
			gone(p);
			sect.disciples.remove(p.getUUID());
			sect.extinct = true;
			elder.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpellSpoilingASectsGroundsIsTrespass(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Spoiled Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos low = helper.absolutePos(BlockPos.ZERO), high = helper.absolutePos(new BlockPos(6, 6, 6));
		sect.bounds = new net.minecraft.world.level.levelgen.structure.BoundingBox(low.getX(), low.getY(), low.getZ(), high.getX(), high.getY(), high.getZ());
		SectData.get(level.getServer()).put(sect);
		CultivatorNpc guard = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(4, 2, 4));
		ServerPlayer outsider = survivor(helper, "loophole-spoiler", Realm.CORE_FORMATION);
		ServerPlayer member = survivor(helper, "loophole-member", Realm.CORE_FORMATION);
		BlockPos[] ground = {helper.absolutePos(new BlockPos(2, 1, 2)), helper.absolutePos(new BlockPos(3, 1, 2)), helper.absolutePos(new BlockPos(2, 1, 3))};
		try {
			guard.setNoAi(true);
			guard.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			guard.joinSect(sect, sect.members.size());
			sect.members.put(guard.getUUID(), new Sect.Member(guard.getUUID(), guard.getRank(), sect.members.size()));
			guard.setTitle(NpcTitle.OUTER_DISCIPLE);
			SectMembership.join(member, sect);
			for (BlockPos pos : ground) level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			helper.assertTrue(HeavenSpells.carvable(level, ground[0], member) && guard.getTarget() == null,
					"A spell of one of its own disciples takes from its grounds unanswered");
			helper.assertTrue(HeavenSpells.carvable(level, ground[1], outsider) && guard.getTarget() == outsider,
					"An outsider's spell taking from a sect's grounds is trespass: its disciple comes for them");
			guard.setTarget(null);
			helper.assertTrue(HeavenSpells.carvable(level, ground[2], outsider) && guard.getTarget() == null,
					"...answered once a second, however many blocks the blow takes");
			helper.succeed();
		} finally {
			for (BlockPos pos : ground) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
			gone(outsider);
			gone(member);
			sect.disciples.remove(member.getUUID());
			sect.extinct = true;
			guard.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSparringFlierComesDownBeforeItsQiGivesOut(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		ServerPlayer p = survivor(helper, "loophole-flier", Realm.CORE_FORMATION);
		try {
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			PlayerCultivation q = npc.cultivation();
			q.setQi(q.maxQi());
			npc.setQiFlying(true);
			helper.assertTrue(npc.isQiFlying(), "Full of qi, it takes to the air");
			helper.assertTrue(q.qiGatherPerSecond() < q.qiFlightCostPerSecond(), "(Its flight costs more than it gathers here)");
			q.setQi(q.qiFlightCostPerSecond() * 0.1);
			helper.assertTrue(!npc.runningLowAloft(), "In earnest it flies on till its qi gives out, as a player would");
			SparSystem.start(p, npc, SparSystem.Terms.FULL);
			helper.assertTrue(npc.runningLowAloft(), "In a spar it comes down while its qi still bears it");
			q.setQi(q.maxQi());
			helper.assertTrue(!npc.runningLowAloft(), "...and not before");
			npc.fallDistance = 20; // the way down so far
			npc.tickFlight(helper.getLevel());
			helper.assertTrue(npc.fallDistance == 0, "Borne up, the way down is no fall: it falls only from where it leaves the air");
			helper.succeed();
		} finally {
			gone(p);
			npc.discard();
		}
	}
}
