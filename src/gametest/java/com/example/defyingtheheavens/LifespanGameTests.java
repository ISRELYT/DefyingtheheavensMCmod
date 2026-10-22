package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * Lifespan (the user's ask, 2026-10-19; Lifespan): a year a game day online, a hundred years and every realm's on top, by the higher
 * path; one clock alive and dead (the user's ask, 2026-10-20); a life spent ended whole, a mortal born anew.
 */
public class LifespanGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(player);
		player.setHealth(player.getMaxHealth());
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachRealmAddsItsYears(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		helper.assertTrue(c.isMortal() && Lifespan.span(c) == 100 && c.age() == Lifespan.START_AGE, "A mortal: 100 years, from 16");
		// ...and the realms after Four Axis the user's (2026-10-21): a True Immortal's without end.
		double[] spans = {100, 300, 700, 1300, 2500, 12500, 112_500, 10_112_500, 110_112_500, 10_110_112_500.0, Double.POSITIVE_INFINITY};
		for (Realm realm : Realm.values()) {
			c.setState(realm, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(Lifespan.span(c) == spans[realm.ordinal()], realm.getId() + ": " + spans[realm.ordinal()] + " years (" + Lifespan.span(c) + ")");
		}
		// The higher path counts: a ghost path of Nascent Soul over a heaven path of Qi Refining.
		PlayerCultivation ghostly = new PlayerCultivation();
		ghostly.setState(Realm.QI_REFINING, Stage.EARLY, 0);
		ghostly.netherworld().setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		GhostMethods.setPath(ghostly.netherworld(), PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY));
		helper.assertTrue(Lifespan.span(ghostly) == 1300, "The ghost path's Nascent Soul gives its years (" + Lifespan.span(ghostly) + ")");
		ghostly.setExtraYears(250);
		helper.assertTrue(Lifespan.span(ghostly) == 1550, "...and years won besides on top");
		// Saved and loaded; a save from before lifespans is just come of age; one from before one clock adds its soul's years.
		ghostly.setAge(321.5);
		PlayerCultivation loaded = PlayerCultivation.load(ghostly.save());
		helper.assertTrue(loaded.age() == 321.5 && loaded.extraYears() == 250 && !ghostly.save().contains("SoulYears"), "Saved");
		net.minecraft.nbt.CompoundTag old = ghostly.save();
		old.remove("Age");
		helper.assertTrue(PlayerCultivation.load(old).age() == Lifespan.START_AGE, "An old save: 16");
		net.minecraft.nbt.CompoundTag twoClocks = ghostly.save();
		twoClocks.putDouble("SoulYears", 12);
		helper.assertTrue(PlayerCultivation.load(twoClocks).age() == 333.5, "A save from the two clocks: its years among the dead added to its age");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSpentLifeEndsWhole(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.CORE_FORMATION);
		PlayerCultivation c = CultivationManager.own(p);
		c.heaven().setMethod(HeavenMethod.FIRE_QI_REFINING, 3, 0);
		StatPoints.spend(p, StatPoints.Stat.STRENGTH, 3);
		c.journal().add(new Journal.Entry("test_page", 0, "", net.minecraft.network.chat.Component.literal("kept")));
		c.setAge(Lifespan.span(c) - 0.5);
		helper.assertTrue(!Lifespan.check(p) && p.isAlive() && !LifespanTribulation.isRunning(p.getUUID()), "Half a year left: alive, no tribulation");
		c.setAge(Lifespan.span(c));
		net.minecraft.world.level.GameRules.BooleanValue afterlife = helper.getLevel().getGameRules().getRule(ModGameRules.NETHERWORLD_AFTER_DEATH);
		boolean was = afterlife.get();
		afterlife.set(true, helper.getLevel().getServer()); // the dead go to the Netherworld: but not one whose life is spent
		try {
			helper.assertTrue(Lifespan.check(p) && LifespanTribulation.isRunning(p.getUUID()) && p.isAlive() && c.lifespanTrial() == 0,
					"The span reached: the heavens come to collect it (no death at once: the user's ask, 2026-10-20)");
			p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE); // a death of anything while it runs
			helper.assertTrue(p.isDeadOrDying() && !LifespanTribulation.isRunning(p.getUUID()) && CultivationManager.own(p).lifeEnded(),
					"Killed under it: the life is over");
		} finally {
			afterlife.set(was, helper.getLevel().getServer());
			LifespanTribulation.forget(p.getUUID());
		}
		helper.assertTrue(!CultivationManager.own(p).netherworld().boundBelow(),
				"Not bound for the Netherworld: born again alive at its own spawn point (the user's ask, 2026-10-20)");
		LifespanTribulation.onRespawn(p); // as its respawn does
		PlayerCultivation after = CultivationManager.own(p);
		helper.assertTrue(after != c && after.isMortal() && after.getRealm() == Realm.QI_REFINING && after.age() == Lifespan.START_AGE
						&& after.heaven().known().isEmpty() && after.statPointsSpent() == 0 && after.getPhysique() != null
						&& !after.lifeEnded() && after.lifespanTrial() == -1,
				"A mortal of 16 again: no realm, no method, no point, a body rolled anew");
		helper.assertTrue(after.journal().stream().anyMatch(e -> "test_page".equals(e.id())), "Only the journal remembers");
		helper.succeed();
	}

	/**
	 * The lifespan tribulation's curve (the user's ask, 2026-10-20): five minutes to full power, its strikes from one every 3 s to ten
	 * a second and from half a percent of max health to two fifths, its storm to three times its size; a Heavenly Thunder Body drinks
	 * its strikes whole to half power, nothing at full. And the tribulations' strikes: Heavenly Being 50, Four Axis 100 (the user's),
	 * the realms below in step, each tribulation's gap by its count.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theLifespanTribulationGrows(GameTestHelper helper) {
		helper.assertTrue(LifespanTribulation.power(0) == 0 && LifespanTribulation.power(3000) == 0.5 && LifespanTribulation.power(9000) == 1,
				"Five minutes to its full power");
		helper.assertTrue(LifespanTribulation.gap(0, false) == 60 && LifespanTribulation.gap(1, false) == 2 && LifespanTribulation.gap(0, true) == 40,
				"A strike every 3 s at first, ten a second at the last; a Primordial Chaos Body's half as often again");
		int last = 61;
		boolean faster = true;
		for (int t = 0; t <= LifespanTribulation.FULL_POWER_TICKS; t += 300) {
			int gap = LifespanTribulation.gap(LifespanTribulation.power(t), false);
			faster &= gap <= last;
			last = gap;
		}
		helper.assertTrue(faster, "Ever faster");
		helper.assertTrue(Math.abs(LifespanTribulation.share(0) - 0.005) < 1e-9 && Math.abs(LifespanTribulation.share(1) - 0.4) < 1e-6
				&& LifespanTribulation.share(0.5) < 0.06, "Half a percent of max health a strike at first, two fifths at the last, heavy only late");
		helper.assertTrue(LifespanTribulation.growth(0) == 1 && Math.abs(LifespanTribulation.growth(1) - 3) < 1e-6, "Its storm and bolts to three times their size");
		helper.assertTrue(LifespanTribulation.drunk(0.25) == 1 && LifespanTribulation.drunk(0.5) == 1 && Math.abs(LifespanTribulation.drunk(0.75) - 0.5) < 1e-9
				&& LifespanTribulation.drunk(1) == 0, "A Heavenly Thunder Body drinks it whole to half power, none at full");
		helper.assertTrue(TribulationManager.strikes(Realm.FOUNDATION_BUILDING, Stage.EARLY).count() == 2 && TribulationManager.strikes(Realm.CORE_FORMATION, Stage.EARLY).count() == 6
				&& TribulationManager.strikes(Realm.NASCENT_SOUL, Stage.LATE).count() == 18 && TribulationManager.strikes(Realm.HEAVENLY_BEING, Stage.EARLY).count() == 50
				&& TribulationManager.strikes(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION).count() == 100, "Strikes: 2, 6, 18, 50, 100");
		// None longer than 30 s (the user's ask, 2026-10-22): 3 s apart while they fit, else all fitted in, several a tick for the greatest.
		helper.assertTrue(TribulationManager.strikeSpan(1) == 0 && TribulationManager.strikeSpan(6) == 300 && TribulationManager.strikeSpan(18) == 520
				&& TribulationManager.strikeSpan(3200) == 520 && TribulationManager.duration(3200) == TribulationManager.MAX_TICKS
				&& TribulationManager.duration(6) == 380 && TribulationManager.MAX_TICKS == 600,
				"3 s apart while they fit, else fitted into 30 s from the gathering to the breakthrough");
		helper.assertTrue(TribulationManager.strikesBy(3200, 520, -1) == 0 && TribulationManager.strikesBy(3200, 520, 0) == 1
				&& TribulationManager.strikesBy(3200, 520, 1) == 7 && TribulationManager.strikesBy(3200, 520, 520) == 3200
				&& TribulationManager.strikesBy(6, 300, 59) == 1 && TribulationManager.strikesBy(6, 300, 60) == 2 && TribulationManager.strikesBy(1, 0, 0) == 1,
				"True Immortal's 3,200 some six a tick, the last at the span's end; Core Formation's a strike every 3 s");
		for (Realm realm : Realm.values()) {
			for (Stage stage : Stage.values()) {
				int count = TribulationManager.strikes(realm, stage, true).count();
				helper.assertTrue(TribulationManager.duration(count) <= TribulationManager.MAX_TICKS, "No tribulation past 30 s, a Primordial Chaos Body's neither: "
						+ realm.getId() + " " + stage.getId());
			}
		}
		helper.succeed();
	}

	/**
	 * The age stops at the span's end (the user's ask, 2026-10-20: years won after are won in full); the tribulation waits through a
	 * logout on the record and comes back as strong as it was; no Longevity Pill under it; years won again end it.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void itWaitsAndYearsEndIt(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.FOUNDATION_BUILDING);
		PlayerCultivation c = CultivationManager.own(p);
		try {
			double span = Lifespan.span(c);
			c.setAge(span - 0.0001);
			Lifespan.age(p, 1.0);
			helper.assertTrue(c.age() == span && LifespanTribulation.isRunning(p.getUUID()), "Aged to the span's end and no further: the heavens come");
			Lifespan.age(p, 5.0);
			helper.assertTrue(c.age() == span, "It counts no more while the life is spent");
			LifespanTribulation.forget(p.getUUID()); // logged out
			c.setLifespanTrial(1234); // as its power stood
			helper.assertTrue(PlayerCultivation.load(c.save()).lifespanTrial() == 1234 && !LifespanTribulation.isRunning(p.getUUID()), "It waits, saved");
			Lifespan.check(p);
			helper.assertTrue(LifespanTribulation.isRunning(p.getUUID()) && c.lifespanTrial() == 1234, "Back: as strong as it was");
			ItemStack pill = PillItem.create(ModItems.LONGEVITY_PILL, PillGrade.LOW, 1);
			p.setItemInHand(InteractionHand.MAIN_HAND, pill);
			helper.assertTrue(!pill.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "No Longevity Pill under it");
			c.setExtraYears(c.extraYears() + 40); // years taken from the grateful
			Lifespan.age(p, 1.0);
			helper.assertTrue(!LifespanTribulation.isRunning(p.getUUID()) && c.lifespanTrial() == -1 && Math.abs(Lifespan.left(c) - 39) < 1e-6,
					"Years won again: the heavens withdraw, and the 40 counted in full from where the age stopped");
			helper.assertTrue(pill.use(helper.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "...and pills may be taken again");
			p.stopUsingItem();
		} finally {
			LifespanTribulation.forget(p.getUUID());
		}
		helper.succeed();
	}

	/**
	 * The dead go on by the same clock (the user's ask, 2026-10-20): a death near the end gives no years back, and neither does coming
	 * back to life; run out among the dead, the soul scatters and is born again.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadKeepTheLivingsClock(GameTestHelper helper) {
		ServerPlayer first = cultivator(helper, Realm.NASCENT_SOUL);
		java.util.UUID id = first.getUUID();
		PlayerCultivation c = CultivationManager.own(first);
		c.setAge(200);
		GhostSystem.makeGhost(first);
		net.minecraft.server.players.PlayerList players = helper.getLevel().getServer().getPlayerList();
		try {
			helper.assertTrue(Lifespan.used(c) == 200 && Lifespan.span(c) == 1300, "Dead: the same 200 years used, of the same span");
			c.setAge(600);
			GhostSystem.revive(first);
			ServerPlayer living = players.getPlayer(id);
			helper.assertTrue(living != null && !GhostSystem.isGhost(living) && CultivationManager.own(living).age() == 600,
					"Back to life: still 600 years used, none given back");
			GhostSystem.makeGhost(living);
			c = CultivationManager.own(living);
			c.setAge(Lifespan.span(c));
			helper.assertTrue(Lifespan.check(living) && LifespanTribulation.isRunning(id) && GhostSystem.isGhost(living),
					"The span reached among the dead: the Netherworld comes to collect it");
			living.hurt(living.damageSources().genericKill(), Float.MAX_VALUE); // struck down under it
			helper.assertTrue(living.isAlive() && !LifespanTribulation.isRunning(id) && CultivationManager.own(living).lifeEnded(),
					"Held at a breath, to scatter at the next tick");
			LifespanTribulation.tick(helper.getLevel().getServer());
			ServerPlayer reborn = players.getPlayer(id);
			PlayerCultivation after = CultivationManager.own(reborn);
			helper.assertTrue(reborn != null && !GhostSystem.isGhost(reborn) && after.isMortal() && after.age() == Lifespan.START_AGE
							&& after.getPhysique() != null, "Scattered into the Soul River, born again a mortal of 16");
		} finally {
			GhostSystem.forget(id);
			ServerPlayer still = players.getPlayer(id);
			if (still != null && still != first) players.remove(still);
		}
		helper.succeed();
	}
}
