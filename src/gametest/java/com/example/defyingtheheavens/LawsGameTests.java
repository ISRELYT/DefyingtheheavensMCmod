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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Phase 3a (2026-10-26): the ladder past True Immortal, Earth Immortal to Heavenly Lord, each twice the last; the Laws that open it
 * (Laws: the levels each breakthrough wants, on every path), how they are comprehended (meditation in the Immortal Realm, glimpses, a
 * weathered Decay's choice) and what they give; and the Five Decays (FiveDecays): their signs one by one, their judgement, weathered or
 * failed.
 */
public class LawsGameTests implements FabricGameTest {
	private static final Realm[] LADDER = {Realm.EARTH_IMMORTAL, Realm.HEAVEN_IMMORTAL, Realm.GOLDEN_IMMORTAL, Realm.TAIYI_GOLDEN_IMMORTAL,
			Realm.PROFOUND_SOVEREIGN, Realm.HEAVENLY_LORD};

	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} Early, at the test's corner. */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-laws")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void leave(ServerPlayer p) {
		FiveDecays.forget(p.getUUID());
		MeditationManager.forget(p.getUUID());
		BurnLifespan.onDisconnect(p);
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-4 * Math.max(1, Math.abs(b));
	}

	private static String key(Component c) {
		return c.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLadderPastTrueImmortal(GameTestHelper helper) {
		helper.assertTrue(Realm.TRUE_IMMORTAL.next() == Realm.EARTH_IMMORTAL && Realm.HEAVENLY_LORD.isLast(), "The ladder after True Immortal, Heavenly Lord the last");
		for (int i = 0; i < LADDER.length; i++) {
			Realm realm = LADDER[i], before = realm.previous();
			helper.assertTrue(realm.ordinal() == Realm.TRUE_IMMORTAL.ordinal() + 1 + i && realm.isPastTrueImmortal() && realm.isImmortal()
					&& realm.pastTrueImmortal() == i + 1 && !realm.taughtInTheWorld(), realm.getId() + ": in its place, an immortal's, taught in no world");
			// Each twice the last (the user's rule: "Each new realm should be 2X stronger than the last").
			helper.assertTrue(realm.getMaxHealth() == 2 * before.getMaxHealth() && realm.getAttackDamage() == 2 * before.getAttackDamage()
					&& realm.getMaxQi() == 2 * before.getMaxQi() && realm.getQiGather() == 2 * before.getQiGather()
					&& realm.getQiFlightCost() == 2 * before.getQiFlightCost() && realm.getSwordFlightCost() == 2 * before.getSwordFlightCost(),
					realm.getId() + ": twice the realm before, body, blows and qi");
			// Its cultivation eight times, its gain five: every stage still longer than the one before.
			helper.assertTrue(realm.getCultivationBase() == 8 * before.getCultivationBase() && realm.getCultivationPerSecond() == 5 * before.getCultivationPerSecond(),
					realm.getId() + ": eight times the cultivation, five times the gain");
			double time = realm.getCultivationBase() / realm.getCultivationPerSecond(), beforeTime = before.getCultivationBase() / before.getCultivationPerSecond();
			helper.assertTrue(time * Stage.EARLY.timeWeight() > beforeTime * Stage.GRAND_PERFECTION.timeWeight(), realm.getId() + ": its Early longer than the Grand Perfection before");
			helper.assertTrue(PlayerCultivation.isImmortalRealmRealm(realm) && PlayerCultivation.qiNeed(realm) == PlayerCultivation.IMMORTAL_QI_MULTIPLIER
					&& Double.isInfinite(Lifespan.realmYears(realm)), realm.getId() + ": the Immortal Realm's to cultivate, its years without end");
			helper.assertTrue(HeavenMethod.of(QiElement.FIRE, realm) == null && GhostMethod.foundationOf(realm) == null, realm.getId() + ": no method carries it");
			// Its tribulations: True Immortal's 3,200 strikes, each the heavier, borne by the body before.
			for (Stage stage : Stage.values()) {
				TribulationManager.Strikes strikes = TribulationManager.strikes(realm, stage);
				helper.assertTrue(strikes.count() == 3200 && strikes.bolt() > 0 && strikes.magic() > strikes.bolt()
						&& TribulationManager.duration(strikes.count()) <= TribulationManager.MAX_TICKS, realm.getId() + " " + stage.getId() + ": 3,200 strikes in 30 s");
			}
			helper.assertTrue(TribulationManager.strikes(realm, Stage.EARLY).magic() > TribulationManager.strikes(before, Stage.GRAND_PERFECTION).magic(),
					realm.getId() + ": its Early's strikes heavier than the Grand Perfection's before");
			helper.assertTrue(CultivatorTrades.bloodPrice(realm) > CultivatorTrades.bloodPrice(before), realm.getId() + ": its blood dearer than the realm before's");
		}
		helper.assertTrue(BodyCaps.MAX > 10 * CultivationStats.maxHealth(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION), "The body's cap far past a Heavenly Lord's");
		helper.assertTrue(DemonBloodItem.look(DemonBloodItem.of(Bloodline.TIGER, Realm.HEAVENLY_LORD)) == DemonBloodItem.LOOKS - 1, "The ladder's blood the golden phial");
		// Immortal essence twice as much for each realm past True Immortal.
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.EARTH_IMMORTAL, Stage.EARLY, 0);
		double earth = ImmortalEssence.max(c);
		c.setState(Realm.HEAVENLY_LORD, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(earth == 2 * ImmortalEssence.POOL[0] && ImmortalEssence.max(c) == 64 * ImmortalEssence.POOL[3], "Essence twice again each realm (" + earth + ")");
		// Few of them in the worlds: the Immortal Realm's handful, none below it, no Heavenly Lord anywhere.
		String immortal = ModDimensions.IMMORTAL_REALM.location().toString();
		helper.assertTrue(HighRealms.quota(Realm.EARTH_IMMORTAL, immortal) == 32 && HighRealms.quota(Realm.PROFOUND_SOVEREIGN, immortal) == 1
				&& HighRealms.quota(Realm.HEAVENLY_LORD, immortal) == 0 && HighRealms.quota(Realm.EARTH_IMMORTAL) == 0
				&& NpcSpawner.IMMORTAL_TOP == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), "A handful in the Immortal Realm, no wanderer of them");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLawsOpenTheLadder(GameTestHelper helper) {
		int[] needs = {3, 7, 12, 18, 25, 35};
		for (int i = 0; i < LADDER.length; i++) helper.assertTrue(Laws.needed(LADDER[i]) == needs[i], LADDER[i].getId() + " wants " + needs[i] + " levels");
		helper.assertTrue(Laws.needed(Realm.TRUE_IMMORTAL) == 0, "True Immortal wants none");
		// The heaven path: its True Immortal method opens no way past it; the Laws do.
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		c.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
		c.heaven().setMethod(HeavenMethod.FIRE_TRUE_IMMORTAL, HeavenMethod.FIRE_TRUE_IMMORTAL.layers(), 0);
		helper.assertTrue(c.isAtBottleneck() && c.isHeldByMethod() && key(HeavenMethods.gateLine(c)).equals(ModLang.LAW_GATE),
				"True Immortal Grand Perfection at its bottleneck: held by the Laws, a method opening no way");
		c.laws().setLevel(Law.FIRE, 2);
		helper.assertTrue(c.isHeldByMethod(), "Two levels: not yet");
		c.laws().setLevel(Law.WATER, 1);
		helper.assertTrue(!c.isHeldByMethod() && Laws.opensWay(c, Realm.EARTH_IMMORTAL) && !Laws.opensWay(c, Realm.HEAVEN_IMMORTAL),
				"Three open Earth Immortal, and no further");
		// The ghost path the same.
		PlayerCultivation g = new PlayerCultivation();
		g.awaken(0);
		g.netherworld().setMethod(GhostMethod.NETHERWORLD_IMMORTAL_CANON, 1, 0);
		GhostMethods.setPath(g.netherworld(), PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION));
		helper.assertTrue(!GhostMethods.opensWay(g, Realm.EARTH_IMMORTAL) && key(GhostMethods.gateLine(g)).equals(ModLang.LAW_GATE),
				"The ghost path's True Immortal foundation opens no way past it");
		g.laws().setLevel(Law.EARTH, 3);
		helper.assertTrue(GhostMethods.opensWay(g, Realm.EARTH_IMMORTAL), "...three levels of the Laws do");
		// The Demon Path: the ladder's blood alone climbs no further (the merchants sell it).
		PlayerCultivation d = new PlayerCultivation();
		d.demon().setBloodline(Bloodline.TIGER);
		int top = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
		d.demon().set(top, DemonPath.required(top));
		d.demon().setBloodRealm(Realm.EARTH_IMMORTAL.ordinal());
		helper.assertTrue(DemonPath.atBottleneck(d) && !DemonPath.heldByBlood(d) && DemonPath.heldByLaws(d) && !DemonPath.canBreakthrough(d),
				"An Earth Immortal's blood drunk: held by the Laws all the same");
		d.laws().setLevel(Law.METAL, 3);
		helper.assertTrue(!DemonPath.heldByLaws(d) && DemonPath.locked(d) && !DemonPath.canBreakthrough(d), "...three levels: open, but only in the Immortal Realm");
		d.setInImmortalRealm(true);
		helper.assertTrue(!DemonPath.locked(d) && DemonPath.canBreakthrough(d), "...where it may climb");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyTheImmortalRealmHoldsTheirBreakthroughs(GameTestHelper helper) {
		// The user's rule (2026-10-26): "make it so you can only do breakthroughs for true immortal and up in the immortal realm".
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		c.heaven().setMethod(HeavenMethod.FIRE_TRUE_IMMORTAL, HeavenMethod.FIRE_TRUE_IMMORTAL.layers(), 0);
		c.setState(Realm.ENTERING_NIRVANA, Stage.LATE, PlayerCultivation.required(Realm.ENTERING_NIRVANA, Stage.LATE));
		helper.assertTrue(c.isAtBottleneck() && !c.isBreakthroughLocked() && c.canBreakthrough(), "Entering Nirvana Late into Grand Perfection: anywhere it may be");
		// Into True Immortal itself: only in the Immortal Realm.
		c.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION));
		helper.assertTrue(c.isAtBottleneck() && c.isBreakthroughLocked() && c.isImmortalRealmLocked() && !c.canBreakthrough()
				&& key(c.lockLine()).equals(ModLang.MSG_IMMORTAL_REALM_LOCKED), "Into True Immortal outside the Immortal Realm: shut, the Immortal Realm named");
		c.setInImmortalRealm(true);
		helper.assertTrue(!c.isBreakthroughLocked() && c.canBreakthrough(), "...in it, open");
		// A True Immortal's own stages the same, and past them.
		c.setInImmortalRealm(false);
		c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, PlayerCultivation.required(Realm.TRUE_IMMORTAL, Stage.EARLY));
		helper.assertTrue(c.isBreakthroughLocked() && !c.canBreakthrough(), "True Immortal Early into Mid: shut outside it");
		c.setInImmortalRealm(true);
		helper.assertTrue(!c.isBreakthroughLocked() && c.canBreakthrough(), "...open in it");
		// The ghost path alike.
		PlayerCultivation g = new PlayerCultivation();
		g.awaken(0);
		g.netherworld().setMethod(GhostMethod.NETHERWORLD_IMMORTAL_CANON, 1, 0);
		int gp = PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
		GhostMethods.setPath(g.netherworld(), gp);
		g.netherworld().setPath(gp, GhostMethods.required(gp));
		helper.assertTrue(GhostMethods.atBottleneck(g) && GhostMethods.heldByPlace(g) && !GhostMethods.canBreakthrough(g), "The ghost path into True Immortal: shut outside it");
		g.setInImmortalRealm(true);
		helper.assertTrue(!GhostMethods.heldByPlace(g) && GhostMethods.canBreakthrough(g), "...open in it");
		// The lower realms' own lock keeps its own words.
		PlayerCultivation low = new PlayerCultivation();
		low.awaken(0);
		low.setState(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION));
		low.setLowerRealmBound(true);
		helper.assertTrue(low.isBreakthroughLocked() && !low.isImmortalRealmLocked() && key(low.lockLine()).equals(ModLang.MSG_REALM_LOCKED), "The lower realms' cap as before");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void comprehendingTheLaws(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL);
		try {
			PlayerCultivation c = CultivationManager.get(p);
			LawState s = c.laws();
			helper.assertTrue(Laws.isImmortal(c) && Laws.mayComprehend(c, Law.FIRE) && !Laws.greaterOpen(c) && !Laws.mayComprehend(c, Law.SPACE),
					"A True Immortal comprehends the elements' Laws, not yet the greater");
			PlayerCultivation below = new PlayerCultivation();
			below.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			helper.assertTrue(!Laws.isImmortal(below) && !Laws.mayComprehend(below, Law.FIRE), "None below True Immortal");
			// Meditation comprehends the Law in focus only in the Immortal Realm (the test's world is the Overworld).
			Laws.focus(p, Law.FIRE);
			Laws.meditate(p, c, 10000);
			helper.assertTrue(s.focus() == Law.FIRE && s.insight(Law.FIRE) == 0 && s.level(Law.FIRE) == 0, "Only the Immortal Realm's meditation comprehends");
			// Level L wants L x a quarter of an hour of it.
			helper.assertTrue(Laws.insightFor(1) == 900 && Laws.insightFor(9) == 8100, "The first level a quarter of an hour, the ninth two and a quarter hours");
			int gained = Laws.gain(p, c, Law.FIRE, Laws.insightFor(1) + Laws.insightFor(2) + 10);
			helper.assertTrue(gained == 2 && s.level(Law.FIRE) == 2 && near(s.insight(Law.FIRE), 10), "Two levels, ten seconds toward the third");
			Laws.gain(p, c, Law.FIRE, 1.0e9);
			helper.assertTrue(s.level(Law.FIRE) == Laws.MAX_LEVEL && s.insight(Law.FIRE) == 0 && !Laws.mayComprehend(c, Law.FIRE), "Nine levels, and no more");
			// A greater Law can't be focused below Golden Immortal; a weathered Decay's level not spent on one there either.
			Laws.focus(p, Law.SPACE);
			helper.assertTrue(s.focus() == Law.FIRE, "No greater Law in focus below Golden Immortal");
			s.setChoices(2);
			Laws.choose(p, Law.SPACE);
			Laws.choose(p, Law.WATER);
			helper.assertTrue(s.level(Law.SPACE) == 0 && s.level(Law.WATER) == 1 && s.choices() == 1, "A choice spent on Water, not on Space");
			c.setState(Realm.GOLDEN_IMMORTAL, Stage.EARLY, 0);
			helper.assertTrue(Laws.greaterOpen(c) && Laws.mayComprehend(c, Law.SPACE), "Golden Immortal opens the greater Laws");
			Laws.choose(p, Law.SPACE);
			Laws.focus(p, Law.TIME);
			helper.assertTrue(s.level(Law.SPACE) == 1 && s.choices() == 0 && s.focus() == Law.TIME && s.total() == Laws.MAX_LEVEL + 2,
					"Space chosen, Time in focus, eleven levels in all");
			// Kept as saved.
			LawState loaded = new LawState();
			loaded.load(s.save());
			helper.assertTrue(loaded.level(Law.FIRE) == Laws.MAX_LEVEL && loaded.level(Law.SPACE) == 1 && loaded.focus() == Law.TIME && loaded.total() == s.total(),
					"The Laws kept as saved");
			// A glimpse: a foe of Entering Nirvana slain by an Earth spell, now and then; one below it, never.
			ServerLevel level = helper.getLevel();
			CultivatorNpc nirvana = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1)), lesser = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
			nirvana.setRank(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), true);
			lesser.setRank(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION), true);
			for (int i = 0; i < 60; i++) {
				Laws.onDeath(nirvana, ModDamageTypes.qi(level, QiElement.EARTH, p));
				Laws.onDeath(lesser, ModDamageTypes.qi(level, QiElement.METAL, p));
			}
			helper.assertTrue(s.insight(Law.EARTH) > 0 || s.level(Law.EARTH) > 0, "Glimpses of Earth from a foe of Entering Nirvana");
			helper.assertTrue(s.insight(Law.METAL) == 0 && s.level(Law.METAL) == 0, "None from one below it");
			nirvana.discard();
			lesser.discard();
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void whatTheLawsGive(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.GOLDEN_IMMORTAL);
		try {
			PlayerCultivation c = CultivationManager.get(p);
			LawState s = c.laws();
			ServerLevel level = helper.getLevel();
			s.setLevel(Law.FIRE, 3);
			s.setLevel(Law.SPACE, 2);
			s.setLevel(Law.TIME, 5);
			s.setLevel(Law.LIFE_AND_DEATH, 4);
			float fire = Laws.outgoing(p, ModDamageTypes.qi(level, QiElement.FIRE, p), 100);
			float water = Laws.outgoing(p, ModDamageTypes.qi(level, QiElement.WATER, p), 100);
			float ghostly = Laws.outgoing(p, ModDamageTypes.ghostly(level, p), 100);
			float rift = Laws.outgoing(p, ModDamageTypes.qi(level, QiElement.WATER, Realm.VOID_SHATTERING, null, p), 100);
			float riftFire = Laws.outgoing(p, ModDamageTypes.qi(level, QiElement.FIRE, Realm.VOID_SHATTERING, null, p), 100);
			helper.assertTrue(near(fire, 130) && near(water, 100) && near(ghostly, 140) && near(rift, 120) && near(riftFire, 130 * 1.2),
					"Fire's spells by its Law, Void Shattering's by Space, the ghost path's by Life and Death (" + fire + ", " + water + ", " + ghostly + ", " + rift + ", " + riftFire + ")");
			double time = 1 - Laws.COOLDOWN_PER_LEVEL * 5;
			helper.assertTrue(Laws.cooldown(p, Spell.FLAME_BOLT) == Math.round(Spell.FLAME_BOLT.cooldownTicks() * time) && Laws.cooldown(p, Spell.SAMADHI_TRUE_FIRE) == 1190,
					"Time: every cooldown 15% shorter at five levels (" + Laws.cooldown(p, Spell.SAMADHI_TRUE_FIRE) + ")");
			helper.assertTrue(near(Laws.healed(p, 10), 14), "Life and Death: healing 40% greater at four levels");
			helper.assertTrue(near(Laws.tearWait(c), 0.94), "Space: the Spatial Tear 6% sooner at two levels");
			// Reach, while a spell is cast: its element's Law and, a spell of space, Space's.
			Laws.casting(p, Spell.FLAME_BOLT);
			double fireReach = Laws.reach(p);
			Laws.castDone();
			Laws.casting(p, Spell.NINE_NETHER_STEP);
			double stepReach = Laws.reach(p);
			Laws.castDone();
			Laws.casting(p, Spell.GOLDEN_CROW_DESCENDS);
			double crowReach = Laws.reach(p);
			Laws.castDone();
			helper.assertTrue(near(fireReach, 1.15) && near(stepReach, 1.10) && near(crowReach, 1.15 * 1.10) && Laws.reach(p) == 1,
					"Reach: Fire's 15%, a step through space 10%, the Golden Crow both; nothing when nothing is cast (" + fireReach + ", " + stepReach + ", " + crowReach + ")");
			// A cultivator's spells are its realm's alone.
			CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
			helper.assertTrue(Laws.cooldown(npc, Spell.FLAME_BOLT) == Spell.FLAME_BOLT.cooldownTicks(), "A cultivator's cooldowns its own");
			npc.discard();
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void theFiveDecaysComeAndJudge(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL);
		try {
			PlayerCultivation c = CultivationManager.own(p);
			LawState s = c.laws();
			long now = p.level().getGameTime();
			// Reckoned: the first in ten days.
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decayDue() == now + FiveDecays.PERIOD && s.decaySign() == 0, "The first Decay ten days on (" + s.decayDue() + ", " + now + ")");
			// Due: warned, and its first sign comes; the robes stain.
			double armour = p.getAttributeValue(Attributes.ARMOR);
			s.setDecayDue(now);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decayWarned() && s.decaySign() == 1 && near(p.getAttributeValue(Attributes.ARMOR), armour * FiveDecays.ARMOUR_KEPT),
					"Warned, the first sign: the armour " + p.getAttributeValue(Attributes.ARMOR) + " of " + armour);
			// A minute on, the second: nothing mends it.
			s.setDecay(1, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			p.setHealth(p.getMaxHealth() / 2);
			float half = p.getHealth();
			p.heal(100);
			helper.assertTrue(s.decaySign() == 2 && FiveDecays.withered(p) && p.getHealth() == half, "The second sign: no healing");
			// The third: its qi's gathering halved.
			double gather = c.qiGatherAt(1);
			s.setDecay(2, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decaySign() == 3 && near(c.qiGatherAt(1), gather * FiveDecays.GATHER_KEPT) && FiveDecays.drains(c), "The third sign: the qi falters, the essence drains");
			// The fourth: its blows weaker.
			s.setDecay(3, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decaySign() == 4 && near(FiveDecays.outgoing(p, 10), 10 * FiveDecays.HARM_KEPT), "The fourth sign: its blows weaker");
			// The fifth: no meditation.
			s.setDecay(4, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			MeditationManager.start(p);
			helper.assertTrue(s.decaySign() == 5 && FiveDecays.restless(p) && !MeditationManager.isMeditating(p.getUUID()), "The fifth sign: no meditation");
			// A minute on, the judgement. Struck down under it, it lives on a stage lower.
			s.setDecay(5, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decaySign() == FiveDecays.JUDGEMENT, "The judgement comes");
			helper.assertTrue(!FiveDecays.allowDeath(p) && p.getHealth() > 0 && c.getRealm() == Realm.ENTERING_NIRVANA && c.getStage() == Stage.GRAND_PERFECTION
					&& s.decaySign() == 0 && s.decayDue() == now + FiveDecays.PERIOD,
					"Struck down: alive, a stage lower, the Decay over, the next ten days on");
			helper.assertTrue(FiveDecays.allowDeath(p), "With no judgement under way a death goes ahead as any does");
			// Weathered: a level of the Laws to choose.
			c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
			CultivationManager.refresh(p);
			s.setDecay(5, now - FiveDecays.SIGN_TICKS);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decaySign() == FiveDecays.JUDGEMENT, "The judgement again");
			FiveDecays.weathered(p);
			helper.assertTrue(s.decaySign() == 0 && s.choices() == 1 && p.getHealth() == p.getMaxHealth() && s.decayDue() == now + FiveDecays.PERIOD,
					"Weathered: a level owed, the body whole, the next ten days on");
			Laws.choose(p, Law.WOOD);
			helper.assertTrue(s.level(Law.WOOD) == 1 && s.choices() == 0, "...chosen: Wood");
			// Away and back: a sign begins its minute again, a judgement comes again a minute on.
			s.setDecay(3, now - 600);
			FiveDecays.onJoin(p);
			helper.assertTrue(s.decaySign() == 3 && s.decayAt() == now, "A sign's minute begun again");
			s.setDecay(FiveDecays.JUDGEMENT, now - 600);
			FiveDecays.onJoin(p);
			helper.assertTrue(s.decaySign() == FiveDecays.SIGNS && s.decayAt() == now, "A judgement under way comes again after the fifth sign's minute");
			// Fallen below True Immortal, nothing of the Decays is left on it.
			c.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
			FiveDecays.tick(p.server);
			helper.assertTrue(s.decaySign() == 0 && s.decayDue() == 0 && !FiveDecays.decays(c), "No immortal, no Decay");
			helper.succeed();
		} finally {
			leave(p);
		}
	}
}
