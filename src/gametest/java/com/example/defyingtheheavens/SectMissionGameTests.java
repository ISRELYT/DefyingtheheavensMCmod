package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.UUID;

/**
 * The sect mission board (SectMissions, SectExchange): a disciple's own postings, fitted to their realm, and their saving; a gathering handed
 * in for Contribution and the sect's regard; a hunt's quarry come out for its taker alone, and theirs to slay; a purge's quarry a cultivator
 * of the other path; a letter delivered to another sect; the gate watch held through its waves, or lost; a mission lapsed; the exchange
 * selling by title, Contribution and the day's stock; leaving the sect leaving its Contribution behind; the board set up on a plaza; the sect
 * talking of its missions.
 */
public class SectMissionGameTests implements FabricGameTest {
	private static Sect newSect(GameTestHelper helper, int alignment) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Mission Sect";
		sect.dimension = level.dimension();
		sect.alignment = alignment;
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		SectData.get(level.getServer()).put(sect);
		return sect;
	}

	/** A member of {@code sect} on its rolls only (no body), of {@code rank}. */
	private static void rolls(Sect sect, int rank) {
		Sect.Member m = new Sect.Member(UUID.randomUUID(), rank, sect.members.size());
		sect.members.put(m.id, m);
	}

	/** A member standing in the test: an NPC of {@code sect} of {@code rank}, still. */
	private static CultivatorNpc member(GameTestHelper helper, Sect sect, int rank, int x) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1 + x, 2, 1));
		npc.setRank(rank);
		npc.setAlignment(sect.alignment);
		npc.joinSect(sect, sect.members.size());
		Sect.Member m = new Sect.Member(npc.getUUID(), rank, sect.members.size());
		m.title = NpcTitle.OUTER_DISCIPLE;
		sect.members.put(m.id, m);
		npc.setTitle(NpcTitle.OUTER_DISCIPLE);
		npc.setNoAi(true);
		return npc;
	}

	/** A player of {@code realm} and {@code stage}, survival in earnest (a mock player answers that it is creative), at the test's spot. */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "test-courier")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(p);
		c.setMortal(false);
		c.setState(realm, stage, 0);
		CultivationManager.refresh(p);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static void done(GameTestHelper helper, ServerPlayer p, Sect... sects) {
		Sect mine = SectMembership.sectOf(p.server, p.getUUID());
		if (mine != null) {
			Sect.Disciple d = mine.disciples.get(p.getUUID());
			if (d != null) for (SectMissions.Mission m : d.taken) SectMissions.discardFoes(p.server, m);
			mine.disciples.remove(p.getUUID());
		}
		// The sects done with, and their people in the test taken away (some sects' cores lie far off: the people stand here).
		java.util.Set<UUID> ids = new java.util.HashSet<>();
		for (Sect sect : sects) {
			sect.extinct = true;
			ids.add(sect.id);
		}
		for (CultivatorNpc npc : helper.getLevel().getEntitiesOfClass(CultivatorNpc.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(1, 1, 1))).inflate(32), n -> ids.contains(n.getSectId()))) {
			npc.discard();
		}
		p.server.getPlayerList().remove(p);
	}

	/** A mission of {@code kind} put up on {@code d}'s board by hand (its numbers as the test wants them). */
	private static SectMissions.Mission posted(Sect.Disciple d, SectMissions.Kind kind, int rank) {
		SectMissions.Mission m = new SectMissions.Mission(d.nextMission++, kind, SectMissions.Grade.FITTING, rank);
		m.reward = 12;
		m.standing = 1;
		d.postings.add(m);
		return m;
	}

	private static boolean carries(ServerPlayer p, net.minecraft.world.item.Item item) {
		for (ItemStack stack : p.getInventory().items) if (stack.is(item)) return true;
		return false;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void postingsAreTheDisciplesOwnAndFitTheirRealm(GameTestHelper helper) {
		Sect sect = newSect(helper, 100);
		sect.posts(Sect.Post.GATE).add(helper.absolutePos(new BlockPos(1, 1, 8)));
		ServerPlayer p = player(helper, Realm.FOUNDATION_BUILDING, Stage.MID);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			int rank = SectMissions.rankOf(p);
			SectMissions.refresh(p, sect, d);
			helper.assertTrue(d.postings.size() == SectMissions.POSTINGS, "The day's postings: " + SectMissions.POSTINGS + " (" + d.postings.size() + ")");
			helper.assertTrue(d.postings.stream().allMatch(m -> m.rank >= rank - 1 && m.rank <= rank + 2),
					"...each fitted to their realm, a stage below it to two above (" + rank + ")");
			helper.assertTrue(d.postings.stream().anyMatch(m -> m.grade == SectMissions.Grade.EASY)
					&& d.postings.stream().anyMatch(m -> m.grade == SectMissions.Grade.HARD), "...an Easy one and a Hard one among them");
			helper.assertTrue(d.postings.stream().allMatch(m -> m.reward > 0 && (m.standing == 1 || m.standing == 2) && !m.isTaken()),
					"...each paying Contribution and the sect's regard, none taken yet");
			helper.assertTrue(d.postings.stream().filter(m -> m.kind == SectMissions.Kind.GUARD).count() <= 1, "...one gate watch at most");
			List<SectMissions.Mission> first = List.copyOf(d.postings);
			SectMissions.refresh(p, sect, d);
			helper.assertTrue(d.postings.equals(first), "Looked at again the same day: the same postings");
			// Saved and loaded with the disciple's record.
			CompoundTag tag = new CompoundTag();
			d.contribution = 34;
			SectMissions.save(d, tag);
			Sect.Disciple copy = new Sect.Disciple(d.id, d.name, d.joined);
			SectMissions.load(copy, tag);
			helper.assertTrue(copy.contribution == 34 && copy.postedDay == d.postedDay && copy.postings.size() == d.postings.size()
					&& copy.postings.get(0).kind == d.postings.get(0).kind && copy.postings.get(0).reward == d.postings.get(0).reward
					&& copy.nextMission == d.nextMission, "Saved and loaded as they were");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGatheringIsHandedInForContributionAndTheSectsRegard(GameTestHelper helper) {
		Sect sect = newSect(helper, 0);
		CultivatorNpc fellow = member(helper, sect, PlayerCultivation.rank(Realm.QI_REFINING, Stage.LATE), 2);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.MID);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			d.postedDay = Regard.day(helper.getLevel().getGameTime()); // today's board is the test's
			SectMissions.Mission m = posted(d, SectMissions.Kind.GATHER, 1);
			m.what = "defying-the-heavens:ginseng";
			m.count = 2;
			m.quality = 10;
			int standing = sect.standing(p.getUUID());
			SectMissions.take(p, sect, d, m.id);
			helper.assertTrue(d.taken.contains(m) && m.isTaken() && !d.postings.contains(m), "Taken: theirs, off the postings");
			helper.assertTrue(carries(p, ModItems.MISSION_SLIP), "...its slip in their pack");
			SectMissions.handIn(p, sect, d, m.id);
			helper.assertTrue(d.taken.contains(m), "Nothing to hand in: still theirs");
			p.getInventory().add(GinsengItem.create(ModItems.GINSENG, 5));
			p.getInventory().add(GinsengItem.create(ModItems.GINSENG, 5));
			helper.assertTrue(SectMissions.carried(p, m) == 0, "Ginseng too young (5 years of the 10 asked) doesn't count");
			ItemStack old = GinsengItem.create(ModItems.GINSENG, 20);
			old.setCount(2);
			p.getInventory().add(old);
			helper.assertTrue(SectMissions.carried(p, m) == 2, "...twenty-year-old Ginseng does");
			SectMissions.handIn(p, sect, d, m.id);
			helper.assertTrue(!d.taken.contains(m) && d.contribution == 12 && d.earned == 12, "Handed in: seen through, 12 Contribution theirs");
			helper.assertTrue(sect.standing(p.getUUID()) == standing + 1, "...and the sect thinks the better of them");
			helper.assertTrue((d.kindsDone & 1 << SectMissions.Kind.GATHER.ordinal()) != 0, "...a gathering among the kinds they have seen through");
			helper.assertTrue(SectMissions.carried(p, m) == 0, "...the old Ginseng handed over, the young kept");
			ItemStack slip = ItemStack.EMPTY;
			for (ItemStack stack : p.getInventory().items) if (stack.is(ModItems.MISSION_SLIP)) slip = stack;
			helper.assertTrue(!slip.isEmpty() && SectMissions.ofSlip(p.server, slip) == null, "Its slip's mission is over: it crumbles");
			// The sect talks of it.
			Component remark = SectMissions.remark(fellow, p, Component.literal("Fellow"), "test-courier");
			helper.assertTrue(remark != null, "A fellow speaks to them of their deed");
			helper.assertTrue(SectMissions.chatSubject(sect, helper.getLevel().getGameTime(), helper.getLevel().getRandom()) != null,
					"...and the sect's people have it to talk over");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aHuntsQuarryComesOutForItsTakerAndIsTheirsToSlay(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.MID);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			d.postedDay = Regard.day(level.getGameTime());
			SectMissions.Mission m = posted(d, SectMissions.Kind.HUNT, 1);
			m.what = BeastKind.IRON_HIDE_BOAR.getId();
			m.at = helper.absolutePos(new BlockPos(5, 1, 5));
			m.dimension = level.dimension();
			SectMissions.take(p, sect, d, m.id);
			SectMissions.field(p);
			helper.assertTrue(m.foes.size() == 1, "Its taker near its ground: the quarry comes out (" + m.foes.size() + ")");
			Entity out = level.getEntity(m.foes.get(0));
			helper.assertTrue(out instanceof DemonBeast beast && beast.kind() == BeastKind.IRON_HIDE_BOAR && beast.getRank() == 1
					&& beast.isCurrentlyGlowing() && SectMissions.isFoe(beast) && p.getUUID().equals(SectMissions.takerOf(beast)),
					"...an Iron-Hide Boar of its rank, glowing, its taker's");
			DemonBeast beast = (DemonBeast) out;
			CultivatorNpc bystander = member(helper, sect, 0, 3);
			helper.assertTrue(beast.hunts(p) && !beast.hunts(bystander), "It goes for its taker, and for no one else");
			helper.assertTrue(!SectMissions.spoilsDrop(beast, level.damageSources().genericKill()) && SectMissions.spoilsDrop(beast, p.damageSources().playerAttack(p)),
					"Its spoils are its taker's alone");
			SectMissions.field(p);
			helper.assertTrue(m.foes.size() == 1, "Out already: no more come");
			beast.setHealth(1.0f);
			beast.hurt(p.damageSources().playerAttack(p), 50.0f);
			helper.assertTrue(!beast.isAlive(), "Slain by its taker");
			helper.assertTrue(!d.taken.contains(m) && d.contribution == 12, "...the hunt seen through, its Contribution theirs (" + d.contribution + ")");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPurgesQuarryIsACultivatorOfTheOtherPathAndGoesWithItsMission(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		ServerPlayer p = player(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			d.postedDay = Regard.day(level.getGameTime());
			SectMissions.Mission m = posted(d, SectMissions.Kind.PURGE, PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
			m.target = UUID.randomUUID();
			m.what = SectMissions.foePath(sect).getId();
			m.at = helper.absolutePos(new BlockPos(5, 1, 5));
			m.dimension = level.dimension();
			helper.assertTrue(SectMissions.foePath(sect) == Alignment.Faction.DEVILISH, "A righteous sect's people are preyed on by the devilish");
			SectMissions.take(p, sect, d, m.id);
			SectMissions.field(p);
			Entity out = m.foes.isEmpty() ? null : level.getEntity(m.foes.get(0));
			helper.assertTrue(out instanceof CultivatorNpc quarry && quarry.getUUID().equals(m.target) && quarry.getFaction() == Alignment.Faction.DEVILISH
					&& quarry.getRank() == m.rank, "Its quarry: the cultivator named on its posting, devilish, of its rank");
			CultivatorNpc quarry = (CultivatorNpc) out;
			helper.assertTrue(quarry.isEnemy(p) && quarry.holdsGround() && quarry.makesLastStand(), "...out for its taker, never running, never begging");
			// A few ticks on it is there still: it came in wanted (before the fix it was sent away the tick after it came).
			helper.runAfterDelay(5, () -> {
				try {
					helper.assertTrue(!quarry.isRemoved() && quarry.isAlive() && m.foes.contains(quarry.getUUID()), "A few ticks on, the quarry is out still");
					SectMissions.abandon(p, sect, d, m.id);
					helper.assertTrue(quarry.isRemoved() && !d.taken.contains(m), "Abandoned: the mission gone, its quarry with it");
					helper.succeed();
				} finally {
					done(helper, p, sect);
				}
			});
		} catch (RuntimeException e) {
			done(helper, p, sect);
			throw e;
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aLetterIsCarriedToAnotherSectAndDelivered(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect home = newSect(helper, 100);
		Sect other = newSect(helper, 0);
		other.name = "Far Peak Sect";
		other.core = helper.absolutePos(new BlockPos(1, 1, 1)).offset(800, 0, 0);
		other.posts(Sect.Post.GATE).add(other.core.south(20));
		Sect enemy = newSect(helper, -100);
		enemy.core = helper.absolutePos(new BlockPos(1, 1, 1)).offset(0, 0, 900);
		enemy.posts(Sect.Post.GATE).add(enemy.core.south(20));
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.LATE);
		try {
			// Only a sect of the other path's out of reach: the far neutral sect is the one written to.
			for (int i = 0; i < 8; i++) {
				Sect to = SectMissions.letterSect(level.getServer(), home, level.getRandom());
				helper.assertTrue(to != enemy, "A letter never goes to a sect of the other path");
			}
			SectMembership.join(p, home);
			Sect.Disciple d = home.disciples.get(p.getUUID());
			d.postedDay = Regard.day(level.getGameTime());
			SectMissions.Mission m = SectMissions.make(level, home, d, SectMissions.Kind.DELIVER, SectMissions.Grade.FITTING, SectMissions.rankOf(p), level.getRandom());
			helper.assertTrue(m != null && m.target != null, "A letter to write");
			// Whatever sect it found (others in the world may be in reach), write it to the far one.
			m.target = other.id;
			m.what = other.name;
			m.at = other.core;
			d.postings.add(m);
			SectMissions.take(p, home, d, m.id);
			helper.assertTrue(carries(p, ModItems.SEALED_LETTER), "Taken: the sealed letter in their pack");
			CultivatorNpc stranger = member(helper, enemy, 2, 4);
			CultivatorNpc recipient = member(helper, other, 2, 2);
			helper.assertTrue(SectMissions.letterFor(p, stranger) == null && SectMissions.letterFor(p, recipient) != null,
					"The letter is for the people of its own sect, no other");
			int standing = other.standing(p.getUUID());
			Component said = SectMissions.deliver(p, recipient);
			helper.assertTrue(said != null && !carries(p, ModItems.SEALED_LETTER) && !d.taken.contains(m) && d.contribution == m.reward,
					"Delivered: the letter handed over, the mission seen through");
			helper.assertTrue(other.standing(p.getUUID()) == standing + 2, "...and the sect it went to thinks the better of its courier");
			helper.succeed();
		} finally {
			done(helper, p, home, other, enemy);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGateWatchIsHeldThroughItsWavesOrLost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		BlockPos gate = helper.absolutePos(new BlockPos(2, 1, 2));
		sect.core = gate.north(30);
		sect.posts(Sect.Post.GATE).add(gate);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.LATE);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			d.postedDay = Regard.day(level.getGameTime());
			SectMissions.Mission watch = posted(d, SectMissions.Kind.GUARD, 2);
			watch.at = gate;
			watch.dimension = level.dimension();
			SectMissions.take(p, sect, d, watch.id);
			for (int s = 0; s < 10; s++) SectMissions.field(p);
			helper.assertTrue(watch.progress == 200 && watch.waves == 1, "Ten seconds at the gate: the watch kept so long, its first wave sent ("
					+ watch.progress + ", " + watch.waves + ")");
			watch.progress = SectMissions.WATCH_TICKS - 20;
			watch.waves = SectMissions.waves(watch.grade);
			int before = d.contribution;
			SectMissions.field(p);
			helper.assertTrue(!d.taken.contains(watch) && d.contribution == before + watch.reward && watch.foes.isEmpty(),
					"Its time out: the watch held, its pay theirs, its foes gone");
			// A second watch, begun and walked away from.
			SectMissions.Mission lost = posted(d, SectMissions.Kind.GUARD, 2);
			lost.at = gate;
			lost.dimension = level.dimension();
			SectMissions.take(p, sect, d, lost.id);
			lost.progress = 100;
			lost.waves = 1;
			p.teleportTo(level, gate.getX() + 40.5, gate.getY(), gate.getZ() + 0.5, 0, 0);
			int standing = sect.standing(p.getUUID());
			for (int s = 0; s < SectMissions.WATCH_AWAY / 20; s++) SectMissions.field(p);
			helper.assertTrue(!d.taken.contains(lost) && sect.standing(p.getUUID()) == standing - 1, "Left for twenty seconds: the watch lost, the sect thinking less of them");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aMissionLapsesAndLeavingTheSectLeavesItsContribution(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 0);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.MID);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			d.postedDay = Regard.day(level.getGameTime());
			SectMissions.Mission m = posted(d, SectMissions.Kind.GATHER, 1);
			m.what = "minecraft:iron_ingot";
			m.count = 4;
			SectMissions.take(p, sect, d, m.id);
			helper.assertTrue(m.deadline == level.getGameTime() + SectMissions.Kind.GATHER.days * HeavenRitual.DAY, "Taken: it stands its kind's days");
			int standing = sect.standing(p.getUUID());
			m.deadline = level.getGameTime() - 1;
			SectMissions.lapse(level.getServer());
			helper.assertTrue(!d.taken.contains(m) && sect.standing(p.getUUID()) == standing - 1, "Its time past: lapsed, the sect thinking less of them");
			d.contribution = 50;
			SectMissions.Mission kept = posted(d, SectMissions.Kind.GATHER, 1);
			kept.what = "minecraft:iron_ingot";
			SectMissions.take(p, sect, d, kept.id);
			ItemStack slip = ItemStack.EMPTY;
			for (ItemStack stack : p.getInventory().items) if (stack.is(ModItems.MISSION_SLIP) && SectMissions.ofSlip(p.server, stack) != null) slip = stack;
			helper.assertTrue(!slip.isEmpty(), "A slip for the mission they hold");
			SectMembership.leave(p, sect);
			helper.assertTrue(!sect.disciples.containsKey(p.getUUID()) && SectMissions.ofSlip(p.server, slip) == null,
					"Gone from the sect: its Contribution and its missions stay behind with it (the slip crumbles)");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theExchangeSellsByTitleContributionAndTheDaysStock(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		for (int rank : new int[] {20, 16, 15, 12, 11, 10, 6, 5, 4}) rolls(sect, rank);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.EARLY);
		try {
			SectMembership.join(p, sect);
			Sect.Disciple d = sect.disciples.get(p.getUUID());
			helper.assertTrue(SectMembership.titleOf(sect, p) == NpcTitle.OUTER_DISCIPLE, "The weakest of them: an Outer Disciple");
			List<SectExchange.Ware> wares = SectExchange.wares(sect, p);
			int qiPill = -1, foundation = -1;
			for (int i = 0; i < wares.size(); i++) {
				if (wares.get(i).key().equals("qi_pill")) qiPill = i;
				if (wares.get(i).key().equals("foundation_pill")) foundation = i;
			}
			helper.assertTrue(qiPill >= 0 && foundation >= 0 && wares.stream().anyMatch(w -> w.title() == NpcTitle.ELDER), "Wares for each title");
			SectExchange.buy(p, sect, d, qiPill);
			helper.assertTrue(d.contribution == 0 && !carries(p, ModItems.QI_GATHERING_PILL), "No Contribution: nothing had");
			d.contribution = 100;
			SectExchange.buy(p, sect, d, qiPill);
			helper.assertTrue(d.contribution == 100 - wares.get(qiPill).price() && carries(p, ModItems.QI_GATHERING_PILL),
					"Enough: a Qi Gathering Pill for its price");
			SectExchange.buy(p, sect, d, foundation);
			helper.assertTrue(!carries(p, ModItems.FOUNDATION_PILL) && d.contribution == 100 - wares.get(qiPill).price(),
					"A Foundation Pill is kept for Inner Disciples: not theirs");
			for (int i = 1; i < wares.get(qiPill).perDay(); i++) SectExchange.buy(p, sect, d, qiPill);
			int after = d.contribution;
			SectExchange.buy(p, sect, d, qiPill);
			helper.assertTrue(d.contribution == after, "The day's stock gone: no more of it today");
			d.boughtDay--;
			SectExchange.buy(p, sect, d, qiPill);
			helper.assertTrue(d.contribution == after - wares.get(qiPill).price(), "...and a new day's stock the next day");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSectSetsUpItsBoardOnItsPlazaFacingItsGate(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper, 100);
		BlockPos core = helper.absolutePos(new BlockPos(12, 4, 12));
		sect.core = core;
		sect.posts(Sect.Post.GATE).add(core.south(24));
		for (int[] around : new int[][] {{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) sect.posts(Sect.Post.CORE).add(core.offset(around[0], 0, around[1]));
		BlockState stone = Blocks.STONE.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
		for (int dx = -11; dx <= 11; dx++) {
			for (int dz = -11; dz <= 11; dz++) {
				level.setBlockAndUpdate(core.offset(dx, -1, dz), stone);
				for (int dy = 0; dy <= 2; dy++) level.setBlockAndUpdate(core.offset(dx, dy, dz), air);
			}
		}
		try {
			BlockPos board = SectMissions.lay(level, sect);
			helper.assertTrue(board != null && level.getBlockState(board).is(ModBlocks.MISSION_BOARD) && board.equals(sect.board), "The board set up");
			double out = Math.sqrt(board.distSqr(core));
			helper.assertTrue(out >= SectMissions.BOARD_FROM - 0.5 && out <= SectMissions.BOARD_TO + 1.5 && board.getZ() > core.getZ(),
					"...on the plaza, out from the core toward the gate (" + board.toShortString() + ")");
			helper.assertTrue(level.getBlockState(board).getValue(MissionBoardBlock.FACING) == Direction.SOUTH, "...its face to the gate");
			for (BlockPos post : sect.posts(Sect.Post.CORE)) helper.assertTrue(post.distSqr(board) > 4, "...clear of the posts");
			helper.assertTrue(SectMissions.sectAt(level, board) == sect, "The board is the sect's");
			helper.succeed();
		} finally {
			for (int dx = -11; dx <= 11; dx++) {
				for (int dz = -11; dz <= 11; dz++) {
					for (int dy = -1; dy <= 2; dy++) level.setBlockAndUpdate(core.offset(dx, dy, dz), air);
				}
			}
			sect.extinct = true;
		}
	}
}
