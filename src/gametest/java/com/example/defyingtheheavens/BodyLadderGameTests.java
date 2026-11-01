package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The Body Path's ladder past the Indestructible Body (the user's "Go, all three parts", 2026-11-01, part two): six realms of its own names beside
 * Earth Immortal to Heavenly Lord, each breakthrough waiting on the Laws and the Immortal Realm; an immortal body comprehends the Laws (the greater
 * from the Imperishable Gold Body) and is judged by the Five Decays (a failed judgement a stage of the body), but holds no Law domain nor small
 * world; the Law of Time readies its techniques sooner; Body Tempering, the vigor pool and the endless years go on up; the lower realms hold a
 * Star-Forged Body no more than a Void Shattering cultivator; no body cultivator of the world's climbs past the Indestructible Body.
 */
public class BodyLadderGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	private static String key(Component line) {
		return line.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	private static PlayerCultivation body(Realm realm, Stage stage) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(true);
		c.setMortalState(MortalStage.PEAK, 0);
		int rank = PlayerCultivation.rank(realm, stage);
		c.body().set(rank, 0);
		return c;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLadderWaitsOnTheLawsAndTheImmortalRealm(GameTestHelper helper) {
		helper.assertTrue(BodyPath.LAST_RANK == PlayerCultivation.LAST_RANK, "The Body Path climbs to Heavenly Lord's Grand Perfection, as every path");
		helper.assertTrue(key(BodyPath.realmName(Realm.EARTH_IMMORTAL)).equals(ModLang.bodyRefiningKey("earth_bearing_body"))
				&& key(BodyPath.realmName(Realm.GOLDEN_IMMORTAL)).equals(ModLang.bodyRefiningKey("imperishable_gold_body"))
				&& key(BodyPath.realmName(Realm.HEAVENLY_LORD)).equals(ModLang.bodyRefiningKey("body_of_heaven_and_earth")), "Its ladder's own names");
		int top = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		PlayerCultivation c = body(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		c.body().set(top, BodyPath.required(top));
		helper.assertTrue(BodyPath.atBottleneck(c) && BodyPath.heldByLaws(c) && !BodyPath.canBreakthrough(c) && key(BodyPath.lawGateLine(c)).equals(ModLang.LAW_GATE),
				"The Indestructible Body's Grand Perfection: held by the Laws");
		helper.assertTrue(BodyPath.lawGateLine(c).getContents() instanceof TranslatableContents t && t.getArgs()[0] instanceof Component name
				&& key(name).equals(ModLang.bodyRefiningKey("earth_bearing_body")), "...the realm ahead named the body's way");
		c.laws().setLevel(Law.EARTH, 3);
		helper.assertTrue(!BodyPath.heldByLaws(c) && BodyPath.locked(c) && !BodyPath.canBreakthrough(c)
				&& key(BodyPath.lockLine(c)).equals(ModLang.MSG_IMMORTAL_REALM_LOCKED), "...three levels: open, but only in the Immortal Realm");
		c.setInImmortalRealm(true);
		helper.assertTrue(!BodyPath.locked(c) && BodyPath.canBreakthrough(c) && BodyPath.breakthrough(c)
				&& BodyPath.rank(c) == PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.EARLY), "...where it breaks into the Earth-Bearing Body");
		int earthTop = PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.GRAND_PERFECTION);
		c.body().set(earthTop, BodyPath.required(earthTop));
		helper.assertTrue(BodyPath.heldByLaws(c), "The Sky-Pillar Body wants seven levels");
		c.body().set(BodyPath.LAST_RANK + 5, 0);
		helper.assertTrue(BodyPath.rank(c) == BodyPath.LAST_RANK && BodyPath.isMaxed(c) && !BodyPath.heldByLaws(c), "No higher than the Body of Heaven and Earth's Grand Perfection");
		helper.assertTrue(near(BodyTechniques.maxVigor(c), 260), "Its vigor 260 there");
		helper.assertTrue(CultivationMethods.hasVolume(c, CultivationMethod.BODY_TEMPERING, Realm.HEAVENLY_LORD)
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.BODY), "Body Tempering tempers it all the way up");
		helper.assertTrue(Lifespan.endless(body(Realm.EARTH_IMMORTAL, Stage.EARLY)) && !Lifespan.endless(body(Realm.ENTERING_NIRVANA, Stage.EARLY)),
				"Its years without end from the Indestructible Body on");
		PlayerCultivation low = body(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
		low.body().set(low.body().rank(), BodyPath.required(low.body().rank()));
		helper.assertTrue(!BodyPath.heldByLaws(low), "No Laws below the ladder");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anImmortalBodyComprehendsTheLawsButHoldsNoDomain(GameTestHelper helper) {
		PlayerCultivation c = body(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
		helper.assertTrue(!Laws.isImmortal(c) && !Laws.mayComprehend(c, Law.FIRE) && !FiveDecays.decays(c), "The Undying Body: no immortal yet");
		c.body().set(BodyPath.IMMORTAL_RANK, 0);
		helper.assertTrue(Laws.isImmortal(c) && !Laws.isQiImmortal(c) && Laws.mayComprehend(c, Law.FIRE) && !Laws.greaterOpen(c)
				&& !Laws.mayComprehend(c, Law.TIME), "The Indestructible Body comprehends the five Laws, not yet the greater");
		helper.assertTrue(FiveDecays.decays(c), "...and the Five Decays come for it");
		c.laws().setLevel(Law.FIRE, 2);
		helper.assertTrue(!LawDomains.unlocked(c, Law.FIRE), "...but it has no immortal essence to light a domain with");
		c.body().set(PlayerCultivation.rank(Realm.GOLDEN_IMMORTAL, Stage.EARLY), 0);
		helper.assertTrue(Laws.greaterOpen(c) && Laws.mayComprehend(c, Law.SPACE), "The Imperishable Gold Body opens the greater Laws");
		c.netherworld().setGhost(true);
		helper.assertTrue(!Laws.isImmortal(c) && !FiveDecays.decays(c), "Dead, its body is nothing to the Laws");
		c.netherworld().setGhost(false);
		PlayerCultivation qi = new PlayerCultivation();
		qi.awaken(0);
		qi.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
		qi.laws().setLevel(Law.WATER, 1);
		helper.assertTrue(Laws.isQiImmortal(qi) && Laws.isImmortal(qi) && LawDomains.unlocked(qi, Law.WATER), "A True Immortal's domain as ever");
		// The Law of Time readies the body's techniques sooner, as it does every art.
		c.laws().setLevel(Law.TIME, 5);
		helper.assertTrue(BodyTechniques.cooldown(c, Ability.IRON_SKIN) == Math.round(BodyTechniques.cooldown(Ability.IRON_SKIN) * (1 - 5 * Laws.COOLDOWN_PER_LEVEL))
				&& BodyTechniques.cooldown(c, Ability.IRON_SKIN) < BodyTechniques.cooldown(Ability.IRON_SKIN), "Time: five levels, a technique's cooldown 15% shorter");
		// The lower realms hold a body no more than a qi path once it stands at Void Shattering's.
		PlayerCultivation held = body(Realm.FOUR_AXIS, Stage.EARLY);
		held.setLowerRealmBound(true);
		int cap = PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE);
		helper.assertTrue(held.sustained(held.body().rank()) == cap, "In the lower realms a Golden Blood body is held to the cap");
		held.body().set(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), 0);
		helper.assertTrue(held.sustained(held.body().rank()) == held.body().rank(), "...a Star-Forged Body is free of them, as a Void Shattering cultivator is");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFailedJudgementCostsAStageOfTheBody(GameTestHelper helper) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "ladder-decay")) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		try {
			p.setGameMode(GameType.SURVIVAL);
			BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
			p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
			PlayerCultivation c = CultivationManager.own(p);
			c.setMortal(true);
			c.setMortalState(MortalStage.PEAK, 0);
			BodyPath.begin(p);
			c.body().set(PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.MID), 0);
			Component fallen = FiveDecays.fall(p);
			helper.assertTrue(BodyPath.rank(c) == PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.EARLY) && c.isMortal(),
					"Struck down: the Earth-Bearing Body a stage lower, no qi path touched");
			helper.assertTrue(fallen.getString().equals(BodyPath.rankName(BodyPath.rank(c)).getString()), "...named the body's way (" + fallen.getString() + ")");
			helper.assertTrue(c.laws().decaySign() == 0 && c.laws().decayDue() > 0, "...and the Decay over, the next one due");
			// The ladder's milestones go by the highest of one's paths, the body's too: they open the Codex's chapters of what it meets.
			Milestones.sweep(p);
			net.minecraft.advancements.Advancement immortal = p.server.getAdvancements().getAdvancement(DefyingTheHeavens.id("cultivation/true_immortal"));
			net.minecraft.advancements.Advancement earth = p.server.getAdvancements().getAdvancement(DefyingTheHeavens.id("cultivation/earth_immortal"));
			net.minecraft.advancements.Advancement heaven = p.server.getAdvancements().getAdvancement(DefyingTheHeavens.id("cultivation/heaven_immortal"));
			helper.assertTrue(immortal != null && p.getAdvancements().getOrStartProgress(immortal).isDone() && p.getAdvancements().getOrStartProgress(earth).isDone()
					&& !p.getAdvancements().getOrStartProgress(heaven).isDone(), "The Earth-Bearing Body has the ladder's milestones up to Earth Immortal's, none past");
			helper.succeed();
		} finally {
			BodyPath.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noBodyCultivatorOfTheWorldsClimbsPastTheIndestructibleBody(GameTestHelper helper) {
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		npc.setNoAi(true);
		npc.setRank(PlayerCultivation.rank(Realm.EARTH_IMMORTAL, Stage.MID), true);
		npc.setBodyCultivator(true);
		helper.assertTrue(npc.isBodyCultivator() && npc.getRank() == BodyCultivators.LAST_RANK, "Made a body cultivator: held to the Indestructible Body's Grand Perfection ("
				+ npc.getRank() + ")");
		helper.assertTrue(BodyTechniques.rankOf(npc) <= BodyCultivators.LAST_RANK, "...and its techniques are of that realm at most");
		npc.discard();
		helper.succeed();
	}
}
