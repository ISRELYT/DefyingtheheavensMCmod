package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * The Demon Path and the races' gifts (the user's design, 2026-10-23): a demon born with its core (its Demon Path and its heaven path at
 * Qi Refining Early, never a mortal), a human without one; the switch among the paths; the bar as long as the heaven path's, stepping to
 * its bottleneck; the bloodline's inheritance of its element's spells; the body, the qi pool and the lifespan it gives; its tribulation;
 * and the tiger's blows and roar, the lizard's venom and shed skin, the fox's charm, the dragon's health and early flight.
 */
public class DemonPathGameTests implements FabricGameTest {
	/** A player of {@code race}, born (DemonPath#born), in survival, out of the player list. */
	private static ServerPlayer born(GameTestHelper helper, Bloodline race) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "demon"));
		p.setGameMode(GameType.SURVIVAL);
		Bloodlines.set(helper.getLevel().getServer(), p.getUUID(), race, 0);
		DemonPath.born(p);
		return p;
	}

	private static void forget(MinecraftServer server, ServerPlayer... players) {
		for (ServerPlayer p : players) {
			Bloodlines.forget(server, p.getUUID());
			RaceGifts.readyAgain(p.getUUID());
			RaceGifts.forget(p.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonIsBornWithItsCore(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER), human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation t = CultivationManager.get(tiger), h = CultivationManager.get(human);
			helper.assertTrue(DemonPath.rank(t) == 0 && !t.isMortal() && TwoPaths.heavenRank(t, false) == 0,
					"A tiger is born with a demon core: its Demon Path and its heaven path at Qi Refining Early, no mortal");
			helper.assertTrue(DemonPath.rank(h) == -1 && h.isMortal(), "A human has no demon core, and is born a mortal");
			helper.assertTrue(TwoPaths.drawable(t).equals(java.util.List.of(TwoPaths.Path.HEAVEN, TwoPaths.Path.DEMON)) && TwoPaths.drawable(h).isEmpty(),
					"The tiger may draw in the heavens' qi or its core's; the mortal human neither");
			TwoPaths.switchPath(tiger);
			helper.assertTrue(TwoPaths.cultivatesDemon(t) && TwoPaths.drawnIn(t) == TwoPaths.Path.DEMON, "The switch: the Demon Path drawn in");
			TwoPaths.switchPath(tiger);
			helper.assertTrue(!TwoPaths.cultivatesDemon(t) && TwoPaths.drawnIn(t) == TwoPaths.Path.HEAVEN, "...and round again to the heaven path");
			DemonPath.born(tiger);
			helper.assertTrue(DemonPath.rank(t) == 0, "Born again into a record that has it: kept, not begun anew");
			helper.succeed();
		} finally {
			forget(server, tiger, human);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDemonPathClimbsAsTheHeavenPath(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer fox = born(helper, Bloodline.FOX);
		try {
			PlayerCultivation c = CultivationManager.get(fox);
			for (int rank = 0; rank <= PlayerCultivation.LAST_RANK; rank += 7) {
				helper.assertTrue(DemonPath.required(rank) == PlayerCultivation.required(DemonPath.realm(rank), DemonPath.stage(rank)),
						"Its bar as long as the heaven path's at " + DemonPath.rankName(rank).getString());
			}
			int gained = DemonPath.addCultivation(c, 1.0e9);
			int gp = PlayerCultivation.rank(Realm.QI_REFINING, Stage.GRAND_PERFECTION);
			helper.assertTrue(gained == 3 && DemonPath.rank(c) == gp && DemonPath.atBottleneck(c) && DemonPath.canBreakthrough(c),
					"Its stages step on to Grand Perfection's bottleneck, and wait there for the heavens (" + DemonPath.rank(c) + ")");
			helper.assertTrue(DemonPath.breakthrough(c) && DemonPath.rank(c) == gp + 1 && c.demon().cultivation() == 0, "Its tribulation survived: Foundation Building");
			DemonPath.fallOneStage(c);
			helper.assertTrue(DemonPath.rank(c) == gp, "...and failed: a stage down again");
			helper.assertTrue(DemonPath.racePace(Bloodline.DRAGON) == DemonPath.DRAGON_PACE && DemonPath.racePace(Bloodline.FOX) == 1.0,
					"The dragon the slowest of the races");
			helper.succeed();
		} finally {
			forget(server, fox);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBloodTeachesItsElementsSpells(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER), human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation t = CultivationManager.get(tiger), h = CultivationManager.get(human);
			h.awaken(0);
			h.setMortal(false);
			Spell metal = null, fire = null;
			for (Spell spell : Spell.values()) {
				if (spell.element() == QiElement.METAL && metal == null) metal = spell;
				if (spell.element() == QiElement.FIRE && fire == null) fire = spell;
			}
			helper.assertTrue(metal != null && fire != null, "There are Metal and Fire spells");
			t.demon().set(metal.realmNeeded(), 0);
			h.setState(DemonPath.realm(metal.realmNeeded()), DemonPath.stage(metal.realmNeeded()), 0);
			helper.assertTrue(metal.isKnown(t), "The tiger's Demon Path at the spell's stage: its White Tiger blood teaches " + metal.getId());
			helper.assertTrue(!DemonPath.inherits(t, fire), "...not another element's");
			helper.assertTrue(!metal.isKnown(h), "A human of that rank with no method knows it not");
			t.demon().set(Math.max(0, metal.realmNeeded() - 1), 0);
			helper.assertTrue(metal.realmNeeded() == 0 || !DemonPath.inherits(t, metal), "Below its stage, not yet");
			helper.succeed();
		} finally {
			forget(server, tiger, human);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonsBodyPoolAndYears(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER), human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation t = CultivationManager.get(tiger), h = CultivationManager.get(human);
			h.awaken(0);
			h.setMortal(false);
			int core = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			t.demon().set(core, 0);
			h.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
			TwoPaths.Body body = TwoPaths.body(t, false);
			helper.assertTrue(body.rank() == core && Math.abs(body.share() - DemonPath.BODY) < 1.0e-9 && !body.ghostPath(),
					"The Demon Path above the heaven path gives the body, the mightier (" + body + ")");
			helper.assertTrue(TwoPaths.stageGap(t) == 8 && TwoPaths.harmony(t) == 0,
					"Core Formation Early and Qi Refining Early: eight stages apart, no harmony (" + TwoPaths.harmony(t) + ")");
			t.setState(Realm.FOUNDATION_BUILDING, Stage.LATE, 0);
			helper.assertTrue(TwoPaths.stageGap(t) == 2 && TwoPaths.harmony(t) == TwoPaths.HARMONY[2],
					"The heaven path at Foundation Building Late: two stages apart, the least harmony (" + TwoPaths.harmony(t) + ")");
			t.setState(Realm.QI_REFINING, Stage.EARLY, 0);
			double demonPool = CultivationStats.maxQi(Realm.CORE_FORMATION, Stage.EARLY) * DemonPath.QI_SHARE;
			helper.assertTrue(Math.abs(DemonPath.poolBase(t) - demonPool) < 1.0e-6 && t.maxQi() < h.maxQi() && t.maxQi() > 0,
					"Its qi pool its realm's, the smaller share (" + Math.round(t.maxQi()) + " against a human's " + Math.round(h.maxQi()) + ")");
			double ratio = Lifespan.span(t) / Lifespan.span(h);
			helper.assertTrue(Math.abs(ratio - DemonPath.LIFESPAN) < 1.0e-6, "A demon of the same realm lives half again as long (" + ratio + ")");
			helper.assertTrue(TwoPaths.awakens(t, Realm.CORE_FORMATION, false) == TwoPaths.Path.DEMON && Ability.QI_FLIGHT.isUnlocked(t)
					&& Ability.TIGERS_ROAR.isUnlocked(t) && !Ability.ILLUSION.isUnlocked(t),
					"Core Formation on the Demon Path awakens the realm's abilities, Qi Flight too, and the tiger's roar (not the fox's illusion)");
			helper.succeed();
		} finally {
			forget(server, tiger, human);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDemonPathsTribulation(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer lizard = born(helper, Bloodline.LIZARD);
		try {
			PlayerCultivation c = CultivationManager.get(lizard);
			lizard.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
			DemonPath.addCultivation(c, 1.0e9);
			c.demon().setCultivating(true);
			int gp = DemonPath.rank(c);
			CultivationManager.tryBreakthrough(lizard);
			helper.assertTrue(TribulationManager.isActive(lizard.getUUID()) && c.demon().inTribulation(), "The Breakthrough button brings the Demon Path's tribulation");
			TribulationManager.forget(lizard.getUUID());
			TribulationManager.onJoin(lizard);
			helper.assertTrue(DemonPath.rank(c) == gp - 1 && !c.demon().inTribulation(), "Left mid-way: failed, a stage down");
			helper.succeed();
		} finally {
			TribulationManager.forget(lizard.getUUID());
			forget(server, lizard);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRacesGifts(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER), lizard = born(helper, Bloodline.LIZARD), fox = born(helper, Bloodline.FOX),
				dragon = born(helper, Bloodline.DRAGON);
		try {
			float blow = RaceGifts.outgoing(tiger, helper.getLevel().damageSources().playerAttack(tiger), 10);
			helper.assertTrue(Math.abs(blow - 10 * RaceGifts.TIGER_HARM) < 1.0e-4, "A tiger's blow the harder (" + blow + ")");
			helper.assertTrue(RaceGifts.outgoing(fox, helper.getLevel().damageSources().playerAttack(fox), 10) == 10, "...a fox's not");

			Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
			RaceGifts.onMeleeHit(lizard, pig);
			helper.assertTrue(pig.hasEffect(MobEffects.POISON), "A lizard's blow is venomous");

			lizard.setHealth(lizard.getMaxHealth() * 0.2f);
			RaceGifts.onHurt(lizard);
			helper.assertTrue(Math.abs(lizard.getHealth() - lizard.getMaxHealth() * RaceGifts.SHED_TO) < 0.01, "Brought low, a lizard sheds its skin, healed to half");
			lizard.setHealth(lizard.getMaxHealth() * 0.2f);
			RaceGifts.onHurt(lizard);
			helper.assertTrue(lizard.getHealth() < lizard.getMaxHealth() * 0.25f, "...once in ten minutes");

			helper.assertTrue(RaceGifts.regardShift(CultivationManager.get(fox)) == RaceGifts.CHARM && RaceGifts.regardShift(CultivationManager.get(tiger)) == 0,
					"A fox's charm warms everyone to it");

			PlayerCultivation d = CultivationManager.get(dragon);
			d.demon().set(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0);
			PlayerCultivation t = CultivationManager.get(tiger);
			t.demon().set(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), 0);
			helper.assertTrue(d.canFlyOnQi() && !t.canFlyOnQi() && d.qiFlightCostPerSecond() == Realm.CORE_FORMATION.getQiFlightCost(),
					"A dragon of Foundation Building flies on qi (at Core Formation's cost); a tiger not yet");
			helper.assertTrue(RaceGifts.healthFactor(d) == RaceGifts.DRAGON_HEALTH, "A dragon's health the more");

			t.demon().set(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
			t.setQi(t.maxQi());
			tiger.moveTo(pig.getX() + 2, pig.getY(), pig.getZ());
			pig.removeAllEffects();
			RaceGifts.roar(tiger);
			helper.assertTrue(pig.hasEffect(MobEffects.WEAKNESS) && pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "The tiger's roar cows what stands far below it");
			pig.discard();
			helper.succeed();
		} finally {
			forget(server, tiger, lizard, fox, dragon);
		}
	}
}
