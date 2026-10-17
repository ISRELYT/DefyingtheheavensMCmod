package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Players in sects (SectMembership): the title a player holds among its members, joining and leaving and being barred, who an elder
 * turns away, the treasury's rule (a warning, then cast out), striking a fellow, the barrier, spells sparing fellows, and the grounds.
 */
public class SectMembershipGameTests implements FabricGameTest {
	private static Sect newSect(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = new Sect(UUID.randomUUID());
		sect.name = "Trial Sect";
		sect.dimension = level.dimension();
		sect.core = helper.absolutePos(new BlockPos(1, 1, 1));
		SectData.get(level.getServer()).put(sect);
		return sect;
	}

	/** A member of {@code sect} on its rolls only (no body), of {@code rank}. */
	private static Sect.Member rolls(Sect sect, int rank) {
		Sect.Member m = new Sect.Member(UUID.randomUUID(), rank, sect.members.size());
		sect.members.put(m.id, m);
		return m;
	}

	/** A member standing in the test: an NPC of {@code sect} of {@code rank}, titled. */
	private static CultivatorNpc member(GameTestHelper helper, Sect sect, int rank, NpcTitle title, int x) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1 + x, 2, 1));
		npc.setRank(rank);
		npc.joinSect(sect, sect.members.size());
		Sect.Member m = new Sect.Member(npc.getUUID(), rank, sect.members.size());
		m.title = title;
		sect.members.put(m.id, m);
		npc.setTitle(title);
		npc.setNoAi(true);
		return npc;
	}

	/**
	 * A player of {@code realm} and {@code stage}, survival in earnest (a mock player answers that it is creative, and the sect's
	 * rules pass a creative player by), at the test's spot. Taken off again by {@link #done}.
	 */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "test-disciple")) {
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

	private static void done(GameTestHelper helper, ServerPlayer p, Sect sect) {
		Sect mine = SectMembership.sectOf(p.server, p.getUUID());
		if (mine != null) mine.disciples.remove(p.getUUID());
		drop(helper, sect);
		p.server.getPlayerList().remove(p);
	}

	/** A test's sect done with: marked gone and its members taken away, so none of them wanders into another test as a rogue. */
	private static void drop(GameTestHelper helper, Sect sect) {
		sect.extinct = true;
		for (CultivatorNpc npc : helper.getLevel().getEntitiesOfClass(CultivatorNpc.class, new net.minecraft.world.phys.AABB(sect.core).inflate(24),
				n -> sect.id.equals(n.getSectId()))) {
			npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aPlayerIsTitledAmongTheMembersButNeverMaster(GameTestHelper helper) {
		Sect sect = newSect(helper);
		for (int rank : new int[] {20, 16, 15, 12, 11, 10, 6, 5, 4, 2, 1}) rolls(sect, rank);
		helper.assertTrue(sect.titleAmong(40, 0) == NpcTitle.GRAND_ELDER, "Stronger than the master: a Grand Elder, the master's seat being a cultivator's");
		helper.assertTrue(sect.titleAmong(16, 1.0e9) == NpcTitle.GRAND_ELDER, "Among the strongest: a Grand Elder");
		helper.assertTrue(sect.titleAmong(0, 0) == NpcTitle.OUTER_DISCIPLE, "The weakest: an Outer Disciple");
		NpcTitle middle = sect.titleAmong(9, 0);
		helper.assertTrue(middle == NpcTitle.ELDER || middle == NpcTitle.INNER_DISCIPLE, "In the middle: an Elder or an Inner Disciple (" + middle + ")");
		Sect small = newSect(helper);
		rolls(small, 5);
		helper.assertTrue(small.titleAmong(30, 0) != NpcTitle.SECT_MASTER, "A sect of one member: still not its master");
		sect.extinct = true;
		small.extinct = true;
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void joiningLeavingAndBeingBarred(GameTestHelper helper) {
		Sect sect = newSect(helper);
		CultivatorNpc elder = member(helper, sect, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), NpcTitle.SECT_MASTER, 0);
		ServerPlayer p = player(helper, Realm.FOUNDATION_BUILDING, Stage.MID);
		try {
			helper.assertTrue(SectMembership.refusal(p, elder, sect, new Object[1]) == null, "A Foundation Building cultivator of no sect may try to join");
			SectMembership.join(p, sect);
			helper.assertTrue(SectMembership.sectOf(p.server, p.getUUID()) == sect && SectMembership.areFellows(elder, p), "Joined: one of its disciples, the elder's fellow");
			helper.assertTrue(sect.standing(p.getUUID()) >= SectMembership.WELCOME, "...welcomed (its standing raised)");
			Sect other = newSect(helper);
			CultivatorNpc otherElder = member(helper, other, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), NpcTitle.SECT_MASTER, 3);
			Object[] arg = new Object[1];
			helper.assertTrue("join.other_sect".equals(SectMembership.refusal(p, otherElder, other, arg)) && sect.name.equals(arg[0]),
					"Another sect turns away one of this sect's disciples, naming it");
			drop(helper, other);
			SectMembership.leave(p, sect);
			helper.assertTrue(SectMembership.sectOf(p.server, p.getUUID()) == null, "Left: of no sect");
			arg = new Object[1];
			helper.assertTrue("join.barred".equals(SectMembership.refusal(p, elder, sect, arg)) && ((Number) arg[0]).longValue() == SectMembership.LEFT_DAYS,
					"...and the sect won't take them back for " + SectMembership.LEFT_DAYS + " days (" + arg[0] + ")");
			sect.barredUntil.put(p.getUUID(), helper.getLevel().getGameTime() - 1);
			helper.assertTrue(SectMembership.refusal(p, elder, sect, new Object[1]) == null, "The days passed, it hears them again");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anElderTurnsAwayTheMortalTheHatedAndTheTooStrong(GameTestHelper helper) {
		Sect sect = newSect(helper);
		CultivatorNpc master = member(helper, sect, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), NpcTitle.SECT_MASTER, 0);
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.LATE);
		try {
			PlayerCultivation c = CultivationManager.get(p);
			c.setMortal(true);
			helper.assertTrue("join.mortal".equals(SectMembership.refusal(p, master, sect, new Object[1])), "A mortal is turned away");
			c.setMortal(false);
			c.setState(Realm.NASCENT_SOUL, Stage.MID, 0);
			CultivationManager.refresh(p);
			helper.assertTrue("join.too_strong".equals(SectMembership.refusal(p, master, sect, new Object[1])), "One stronger than its master is turned away");
			c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
			CultivationManager.refresh(p);
			helper.assertTrue(SectMembership.refusal(p, master, sect, new Object[1]) == null, "A weaker cultivator of no standing either way may try");
			sect.changeStanding(p.getUUID(), -200);
			helper.assertTrue("join.hated".equals(SectMembership.refusal(p, master, sect, new Object[1])), "One it loathes is turned away");
			sect.changeStanding(p.getUUID(), -sect.standing(p.getUUID()));
			Physique low = null, high = null;
			for (Physique physique : Physique.values()) {
				if (physique.grade().ordinal() < Physique.Grade.HIGH.ordinal() && low == null) low = physique;
				if (physique.grade().ordinal() >= Physique.Grade.HIGH.ordinal() && high == null) high = physique;
			}
			c.setPhysique(low);
			helper.assertTrue(!SectMembership.excused(p, master), "A body below High grade, the elder not Warm to them: the trial (" + low + ")");
			c.setPhysique(high);
			helper.assertTrue(SectMembership.excused(p, master), "A body of High grade or better needs no trial (" + high + ")");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theTreasuryWarnsThenCastsOut(GameTestHelper helper) {
		Sect sect = newSect(helper);
		for (Realm realm : new Realm[] {Realm.NASCENT_SOUL, Realm.CORE_FORMATION, Realm.CORE_FORMATION, Realm.FOUNDATION_BUILDING, Realm.FOUNDATION_BUILDING,
				Realm.QI_REFINING, Realm.QI_REFINING}) {
			rolls(sect, PlayerCultivation.rank(realm, Stage.MID));
		}
		ServerPlayer p = player(helper, Realm.QI_REFINING, Stage.EARLY);
		try {
			SectMembership.join(p, sect);
			helper.assertTrue(!SectMembership.isElder(sect, p), "A weak disciple is below Elder (" + SectMembership.titleOf(sect, p) + ")");
			helper.assertTrue(SectMembership.treasury(p, sect) == SectMembership.TreasuryTouch.REFUSED && SectMembership.sectOf(p.server, p.getUUID()) == sect,
					"The first hand on the treasury: stopped and warned, still a disciple");
			helper.assertTrue(SectMembership.treasury(p, sect) == SectMembership.TreasuryTouch.REFUSED && SectMembership.sectOf(p.server, p.getUUID()) == null,
					"The second: cast out");
			long barred = sect.barredUntil.getOrDefault(p.getUUID(), 0L) - helper.getLevel().getGameTime();
			helper.assertTrue(barred > (SectMembership.EXPELLED_DAYS - 1) * HeavenRitual.DAY, "...barred " + SectMembership.EXPELLED_DAYS + " days");
			helper.assertTrue(sect.standing(p.getUUID()) < 0, "...its standing fallen");
			helper.assertTrue(SectMembership.treasury(p, sect) == SectMembership.TreasuryTouch.OUTSIDER, "A third: an outsider's (theft, as anyone's)");
			// An elder's hands: a strong disciple opens it, as often as they like.
			CultivationManager.get(p).setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, 0);
			CultivationManager.refresh(p);
			sect.barredUntil.remove(p.getUUID());
			SectMembership.join(p, sect);
			helper.assertTrue(SectMembership.isElder(sect, p), "A strong disciple is an Elder or above (" + SectMembership.titleOf(sect, p) + ")");
			for (int i = 0; i < 3; i++) {
				helper.assertTrue(SectMembership.treasury(p, sect) == SectMembership.TreasuryTouch.ALLOWED, "An elder opens the treasury (try " + (i + 1) + ")");
			}
			helper.assertTrue(SectMembership.sectOf(p.server, p.getUUID()) == sect, "...and stays");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void strikingAFellowCastsOneOut(GameTestHelper helper) {
		Sect sect = newSect(helper);
		CultivatorNpc fellow = member(helper, sect, PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), NpcTitle.ELDER, 0);
		ServerPlayer p = player(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		try {
			SectMembership.join(p, sect);
			helper.assertTrue(fellow.isAlly(p), "A disciple is the members' ally");
			helper.assertTrue(HeavenSpells.spared(p, fellow), "...and the disciple's spells spare them");
			fellow.hurt(helper.getLevel().damageSources().playerAttack(p), 1);
			helper.assertTrue(SectMembership.sectOf(p.server, p.getUUID()) == null, "Striking a fellow outside a spar casts one out");
			helper.assertTrue(!HeavenSpells.spared(p, fellow), "...and then its members are no longer spared");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBarrierAndTheGroundsAreADisciples(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Sect sect = newSect(helper);
		ServerPlayer p = player(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		Formation formation = new Formation(UUID.randomUUID(), Formation.Kind.CORE, sect.core, 12);
		formation.setSect(sect.id);
		try {
			helper.assertTrue(!formation.allows(p), "The barrier keeps out one of no sect");
			helper.assertTrue(SectMembership.groundsMultiplier(p) == 1, "No sect: no help from its grounds");
			SectMembership.join(p, sect);
			helper.assertTrue(formation.allows(p), "...and lets its disciple through");
			helper.assertTrue(sect.inTerritory(p.blockPosition()), "The player stands on its grounds");
			helper.assertTrue(SectMembership.groundsMultiplier(p) == SectMembership.GROUNDS_BONUS, "...and cultivates a quarter faster there");
			BlockPos far = sect.core.offset(5000, 0, 0);
			p.teleportTo(level, far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0, 0);
			helper.assertTrue(SectMembership.groundsMultiplier(p) == 1, "...but not far from them");
			helper.succeed();
		} finally {
			done(helper, p, sect);
		}
	}
}
