package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The realms after Four Axis (the user's ask, 2026-10-22): Dao Integration, Void Shattering, Sacred Vessel, Entering Nirvana and True
 * Immortal, in that order and the Upper Realm's alone; bodies past vanilla's caps; a True Immortal's endless years; their
 * tribulations; their methods on both paths; and how few of them the world's cultivators count (HighRealms).
 */
public class NewRealmsGameTests implements FabricGameTest {
	private static final Realm[] AFTER = {Realm.DAO_INTEGRATION, Realm.VOID_SHATTERING, Realm.SACRED_VESSEL, Realm.ENTERING_NIRVANA, Realm.TRUE_IMMORTAL};

	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} Early, at the test's corner. */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-realms")) {
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
		BurnLifespan.onDisconnect(p);
		Lifespan.forget(p.getUUID());
		LifespanTribulation.forget(p.getUUID());
		AstralRealm.forget(p.getUUID());
		if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRealmsAfterFourAxisInOrder(GameTestHelper helper) {
		helper.assertTrue(Realm.values().length == 11 && Realm.FOUR_AXIS.next() == Realm.DAO_INTEGRATION && Realm.TRUE_IMMORTAL.isLast()
				&& PlayerCultivation.LAST_RANK == PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION), "Four Axis, then the five, True Immortal the last");
		for (int i = 0; i < AFTER.length; i++) {
			Realm realm = AFTER[i];
			// Its needs ten times again; the Great Void's realms (Void Shattering on: 2026-10-25) a hundred times, offsetting its qi; True
			// Immortal, the Immortal Realm's, a thousand (phase 2).
			double need = PlayerCultivation.isImmortalRealmRealm(realm) ? PlayerCultivation.IMMORTAL_QI_MULTIPLIER
					: PlayerCultivation.isGreatVoidRealm(realm) ? PlayerCultivation.GREAT_VOID_QI_MULTIPLIER : PlayerCultivation.UPPER_REALM_QI_MULTIPLIER;
			helper.assertTrue(realm.ordinal() == Realm.FOUR_AXIS.ordinal() + 1 + i && PlayerCultivation.isUpperRealmOnly(realm)
					&& PlayerCultivation.isGreatVoidRealm(realm) == (realm.ordinal() >= Realm.VOID_SHATTERING.ordinal())
					&& PlayerCultivation.required(realm, Stage.EARLY) == realm.getCultivationBase() * Stage.EARLY.getCultivationMultiplier() * need,
					realm.getId() + ": in its place, the Upper Realm's alone, its needs ten times again (the Great Void's a hundred)");
			Realm before = realm.previous();
			helper.assertTrue(realm.getMaxHealth() > before.getMaxHealth() && realm.getAttackDamage() > before.getAttackDamage() && realm.getMaxQi() > before.getMaxQi()
					&& realm.getQiGather() > before.getQiGather() && realm.getCultivationBase() > before.getCultivationBase()
					&& realm.getQiFlightCost() < realm.getQiGather() && realm.getSwordFlightCost() < realm.getQiFlightCost(),
					realm.getId() + ": mightier than the realm before, its flight never outrunning its gathering");
			// Every stage takes longer than the one before it, across the realms too (the user's rule, 2026-10-19).
			double time = realm.getCultivationBase() / realm.getCultivationPerSecond(), beforeTime = before.getCultivationBase() / before.getCultivationPerSecond();
			helper.assertTrue(time * Stage.EARLY.timeWeight() > beforeTime * Stage.GRAND_PERFECTION.timeWeight(), realm.getId() + ": its Early longer than the Grand Perfection before");
		}
		// The user's leaps (2026-10-22): Void Shattering four times Dao Integration's might, True Immortal ten times Entering Nirvana's.
		helper.assertTrue(Realm.VOID_SHATTERING.getMaxHealth() == 4 * Realm.DAO_INTEGRATION.getMaxHealth()
				&& Realm.VOID_SHATTERING.getAttackDamage() == 4 * Realm.DAO_INTEGRATION.getAttackDamage()
				&& Realm.TRUE_IMMORTAL.getMaxHealth() == 10 * Realm.ENTERING_NIRVANA.getMaxHealth()
				&& Realm.TRUE_IMMORTAL.getAttackDamage() == 10 * Realm.ENTERING_NIRVANA.getAttackDamage()
				&& Realm.DAO_INTEGRATION.getMaxHealth() == 2 * Realm.FOUR_AXIS.getMaxHealth(), "Void Shattering's leap and True Immortal's");
		for (int rank = 1; rank <= PlayerCultivation.LAST_RANK; rank++) {
			helper.assertTrue(TwoPaths.might(rank) > TwoPaths.might(rank - 1), "Every rank mightier than the one before: " + rank);
		}
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(!c.isMaxed() && c.breakthroughRealm() == Realm.DAO_INTEGRATION, "Four Axis Grand Perfection is no longer the top");
		c.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
		helper.assertTrue(c.isMaxed(), "True Immortal Grand Perfection is");
		// Four Axis's last axis of virtue holds its Grand Perfection's breakthrough into Dao Integration, as each holds its stage.
		helper.assertTrue(FourAxis.wanted(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION)) == FourAxis.Axis.values()[FourAxis.Axis.values().length - 1]
				&& FourAxis.wanted(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY)) == null, "Into Dao Integration through the last axis; none after it");
		// Past Four Axis Grand Perfection a body is Quasi-Dao Integration, not "beyond" any more; only past True Immortal's is.
		TwoPaths.Effective past = new TwoPaths.Effective(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION), true, 0.5);
		TwoPaths.Effective top = new TwoPaths.Effective(PlayerCultivation.LAST_RANK, true, 0);
		helper.assertTrue(TwoPaths.effectiveName(past).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents quasi
				&& quasi.getKey().equals(ModLang.EFFECTIVE_QUASI_REALM)
				&& TwoPaths.effectiveName(top).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents beyond
				&& beyond.getKey().equals(ModLang.EFFECTIVE_BEYOND), "Quasi-Dao Integration past Four Axis; beyond only past True Immortal");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theBodyGoesPastVanillasCaps(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL);
		try {
			PlayerCultivation c = CultivationManager.get(p);
			c.setState(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION, 0);
			c.setLowerRealmBound(false); // as in the Upper Realm: the test world's lower-realm suppression set aside for the look
			CultivationManager.refresh(p);
			double health = CultivationStats.BASE_HEALTH + CultivationStats.maxHealth(Realm.TRUE_IMMORTAL, Stage.GRAND_PERFECTION);
			helper.assertTrue(p.getMaxHealth() > 1024 && Math.abs(p.getMaxHealth() - health) < health * 0.01,
					"A True Immortal's health past vanilla's 1,024 (" + p.getMaxHealth() + " of " + health + ")");
			helper.assertTrue(p.getAttributeValue(Attributes.ATTACK_DAMAGE) > 2048, "...and its strength past 2,048 (" + p.getAttributeValue(Attributes.ATTACK_DAMAGE) + ")");
			p.setHealth(p.getMaxHealth());
			helper.assertTrue(p.getHealth() > 1024, "Its health fills to the whole of it");
			helper.succeed();
		} finally {
			leave(p);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTrueImmortalsYearsHaveNoEnd(GameTestHelper helper) {
		helper.assertTrue(Lifespan.realmYears(Realm.DAO_INTEGRATION) == 100_000 && Lifespan.realmYears(Realm.VOID_SHATTERING) == 10_000_000
				&& Lifespan.realmYears(Realm.SACRED_VESSEL) == 100_000_000 && Lifespan.realmYears(Realm.ENTERING_NIRVANA) == 10_000_000_000.0
				&& Double.isInfinite(Lifespan.realmYears(Realm.TRUE_IMMORTAL)), "The user's years: Dao Integration 100,000 to Entering Nirvana 10 billion, True Immortal without end");
		helper.assertTrue(Lifespan.realmSeclusion(Realm.DAO_INTEGRATION) == 800_000 && Lifespan.realmSeclusion(Realm.VOID_SHATTERING) == 120_000_000
				&& Lifespan.realmSeclusion(Realm.SACRED_VESSEL) == 1_600_000_000.0 && Lifespan.realmSeclusion(Realm.ENTERING_NIRVANA) == 205_000_000_000.0
				&& Lifespan.realmSeclusion(Realm.TRUE_IMMORTAL) == 0, "Their shares of the years: 8, 12, 16, 20.5; a True Immortal's cultivation costs none");
		ServerPlayer immortal = survivor(helper, Realm.TRUE_IMMORTAL), drawer = survivor(helper, Realm.NASCENT_SOUL);
		try {
			PlayerCultivation c = CultivationManager.own(immortal);
			c.setAge(5000);
			helper.assertTrue(Lifespan.endless(c) && Double.isInfinite(Lifespan.span(c)) && !Lifespan.endless(CultivationManager.own(drawer)), "Endless: only the True Immortal");
			Lifespan.age(immortal, 1000);
			Lifespan.seclude(immortal, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0, PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY),
					PlayerCultivation.required(Realm.TRUE_IMMORTAL, Stage.EARLY) / 2,
					rank -> PlayerCultivation.required(Realm.byIndex(rank / Stage.values().length), Stage.byIndex(rank % Stage.values().length)), 1, 1);
			helper.assertTrue(c.age() == 5000 && !Lifespan.check(immortal) && !LifespanTribulation.isRunning(immortal.getUUID()),
					"It doesn't age, its meditation costs nothing, no tribulation comes for its years (" + c.age() + ")");
			helper.assertTrue(Lifespan.draw(drawer, immortal, 10) == 0, "Nothing to draw out of it");
			helper.assertTrue(BurnLifespan.yearsPerSecond(c) == 0, "Its lifespan burns for nothing (left burning for it: the user's call)");
			c.setAbilityEnabled(Ability.BURN_LIFESPAN, true);
			BurnLifespan.second(immortal);
			helper.assertTrue(c.age() == 5000 && CultivationManager.get(immortal).isAbilityEnabled(Ability.BURN_LIFESPAN), "...and burns on");
			helper.assertTrue(Double.isInfinite(Lifespan.left(c)), "Its years left are without end too");
			helper.succeed();
		} finally {
			leave(immortal);
			leave(drawer);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirTribulations(GameTestHelper helper) {
		int[] counts = {200, 400, 800, 1600, 3200};
		for (int i = 0; i < AFTER.length; i++) {
			for (Stage stage : Stage.values()) {
				TribulationManager.Strikes strikes = TribulationManager.strikes(AFTER[i], stage);
				helper.assertTrue(strikes.count() == counts[i] && strikes.bolt() > 0 && strikes.magic() > strikes.bolt(),
						AFTER[i].getId() + " " + stage.getId() + ": " + counts[i] + " strikes, more of each magic than lightning");
				helper.assertTrue(TribulationManager.duration(strikes.count()) <= TribulationManager.MAX_TICKS, "...in 30 s at most");
			}
		}
		// Each realm's own stages strike harder than its Early: Early is borne by the Grand Perfection before (Void Shattering's and True
		// Immortal's the most beside their own, after their leaps).
		helper.assertTrue(TribulationManager.strikes(Realm.VOID_SHATTERING, Stage.MID).magic() > 2 * TribulationManager.strikes(Realm.VOID_SHATTERING, Stage.EARLY).magic()
				&& TribulationManager.strikes(Realm.TRUE_IMMORTAL, Stage.MID).magic() > 5 * TribulationManager.strikes(Realm.TRUE_IMMORTAL, Stage.EARLY).magic(),
				"Void Shattering's and True Immortal's own stages strike as their leaps are mighty");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirMethodsOnBothPaths(GameTestHelper helper) {
		helper.assertTrue(HeavenMethod.values().length == 55, "Fifty-five heaven methods");
		for (Realm realm : AFTER) {
			for (QiElement element : QiElement.five()) {
				HeavenMethod method = HeavenMethod.of(element, realm);
				helper.assertTrue(method != null && method.layers() == MethodLayers.PER_REALM * (realm.ordinal() + 1) && HeavenMethod.byId(method.getId()) == method,
						realm.getId() + " " + element + ": its method, nine layers a realm");
				helper.assertTrue(HeavenPassives.amount(onlyKnowing(method), element, method.layers()) > HeavenPassives.amount(onlyKnowing(HeavenMethod.of(element, realm.previous())), element, method.layers()),
						method.getId() + ": its deeper layers strengthen the passive further");
			}
			// True Immortal's are carved on no stele: the Immortal Realm's to teach (Realm#taughtInTheWorld, the user's rule of 2026-10-25).
			boolean taught = realm.taughtInTheWorld();
			GhostMethod foundation = GhostMethod.foundationOf(realm);
			helper.assertTrue(foundation != null && foundation.isFoundation() && foundation.layers() == MethodLayers.layers(realm) && foundation.peakWeight() > 0 == taught
					&& foundation.steleWeight() == 0 && GhostMethod.foundationOf(realm.previous()).next() == foundation,
					realm.getId() + ": a ghost foundation of its own, carved only on the Peaks (True Immortal's on none)");
			helper.assertTrue(DaoSteleFeature.WEIGHTS[DaoSteleFeature.Lands.UPPER_REALM_RARE.ordinal()][realm.ordinal()] > 0 == taught
					&& DaoSteleFeature.WEIGHTS[DaoSteleFeature.Lands.OVERWORLD.ordinal()][realm.ordinal()] == 0,
					realm.getId() + ": its steles in the Upper Realm's wondrous lands alone (True Immortal's on none)");
		}
		for (int layer = 1; layer <= MethodLayers.layers(Realm.TRUE_IMMORTAL); layer++) {
			helper.assertTrue(MethodLayers.cost(layer) > 0 && HeavenMethods.layerCost(layer) > 0, "Layer " + layer + " has its cost");
		}
		// The gate: Dao Integration's way wants a method of it; Four Axis's carries no further.
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		c.heaven().setMethod(HeavenMethod.FIRE_FOUR_AXIS, HeavenMethod.FIRE_FOUR_AXIS.layers(), 0);
		helper.assertTrue(!HeavenMethods.opensWay(c, Realm.DAO_INTEGRATION), "A Four Axis method opens no way into Dao Integration");
		c.heaven().setMethod(HeavenMethod.WATER_DAO_INTEGRATION, 1, 0);
		helper.assertTrue(HeavenMethods.opensWay(c, Realm.DAO_INTEGRATION) && !HeavenMethods.opensWay(c, Realm.VOID_SHATTERING)
				&& HeavenMethods.deepestRealm(c.heaven()) == Realm.DAO_INTEGRATION, "...a Dao Integration method does, and no further");
		// The ghost path to True Immortal Grand Perfection.
		NetherworldState s = c.netherworld();
		GhostMethods.setPath(s, PlayerCultivation.LAST_RANK);
		helper.assertTrue(GhostMethods.ghostRank(s) == PlayerCultivation.LAST_RANK && GhostMethods.isMaxed(s)
				&& GhostMethods.LAST_RANK == PlayerCultivation.LAST_RANK, "The ghost path goes to True Immortal Grand Perfection");
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION));
		helper.assertTrue(!GhostMethods.isMaxed(s), "...and Four Axis's is no longer its top");
		for (int rank = 1; rank <= PlayerCultivation.LAST_RANK; rank++) {
			helper.assertTrue(GhostMethods.required(rank) > GhostMethods.required(rank - 1), "The ghost path's every stage longer than the one before: " + rank);
		}
		helper.succeed();
	}

	/** A heaven state knowing only {@code method}, to its last layer. */
	private static HeavenState onlyKnowing(HeavenMethod method) {
		HeavenState s = new HeavenState();
		s.setMethod(method, method.layers(), 0);
		return s;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "new_realms_census")
	public void fewOfThemAmongTheWorldsCultivators(GameTestHelper helper) {
		List<CultivatorNpc> made = new ArrayList<>();
		try {
			var level = helper.getLevel();
			// The world's own (it persists between runs): what it holds already counts.
			int vessels = HighRealms.holding(level, Realm.SACRED_VESSEL), shattering = HighRealms.holding(level, Realm.VOID_SHATTERING);
			helper.assertTrue(HighRealms.quota(Realm.DAO_INTEGRATION) > HighRealms.quota(Realm.VOID_SHATTERING) && HighRealms.quota(Realm.VOID_SHATTERING) == 5
					&& HighRealms.quota(Realm.SACRED_VESSEL) == 1 && HighRealms.quota(Realm.ENTERING_NIRVANA) == 0 && HighRealms.quota(Realm.TRUE_IMMORTAL) == 0
					&& HighRealms.quota(Realm.FOUR_AXIS) == Integer.MAX_VALUE, "Dao Integration rare, Void Shattering a handful, Sacred Vessel one, none higher");
			int vesselRank = PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.EARLY);
			for (int i = 0; i < 3; i++) {
				CultivatorNpc npc = CultivatorNpc.createFor(level);
				if (npc == null) throw new net.minecraft.gametest.framework.GameTestAssertException("No cultivator made");
				BlockPos at = helper.absolutePos(new BlockPos(1 + i, 2, 1));
				npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
				npc.becomeRogue(vesselRank, 0, RandomSource.create(i));
				made.add(npc);
			}
			int asVessel = (int) made.stream().filter(n -> n.getRank() == vesselRank).count();
			helper.assertTrue(asVessel == Math.max(0, 1 - vessels), "Only one Sacred Vessel in a world (" + asVessel + " made so, " + vessels + " held before)");
			for (CultivatorNpc npc : made) {
				helper.assertTrue(npc.getRank() <= vesselRank && (npc.getRank() == vesselRank || npc.getRank() % Stage.values().length == Stage.GRAND_PERFECTION.ordinal()),
						"The rest wait at the Grand Perfection below: " + npc.getRank());
			}
			// (The world may hold more than its room already, of runs before: cultivators of the tests left standing when it last shut down.)
			helper.assertTrue(HighRealms.holding(level, Realm.SACRED_VESSEL) == Math.max(vessels, 1)
					&& HighRealms.holding(level, Realm.VOID_SHATTERING) <= Math.max(shattering, 5), "The census holds their places (Sacred Vessel " + vessels + " before, "
					+ HighRealms.holding(level, Realm.SACRED_VESSEL) + " now; Void Shattering " + shattering + " before, " + HighRealms.holding(level, Realm.VOID_SHATTERING) + " now)");
			// Entering Nirvana's live elsewhere: none rises to it here.
			CultivatorNpc nirvana = CultivatorNpc.createFor(level);
			nirvana.moveTo(helper.absolutePos(new BlockPos(5, 2, 1)).getCenter());
			nirvana.becomeRogue(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.LATE), 0, RandomSource.create(9));
			made.add(nirvana);
			helper.assertTrue(nirvana.getRank() < PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), "No Entering Nirvana among the world's own: " + nirvana.getRank());
			// One taken out of the world frees its place.
			int held = HighRealms.holding(level, Realm.SACRED_VESSEL);
			for (CultivatorNpc npc : made) if (npc.getRank() == vesselRank) HighRealms.gone(level.getServer(), npc.getUUID());
			helper.assertTrue(HighRealms.holding(level, Realm.SACRED_VESSEL) == held - asVessel, "Gone, its place free again");
			helper.succeed();
		} finally {
			for (CultivatorNpc npc : made) {
				HighRealms.gone(helper.getLevel().getServer(), npc.getUUID());
				npc.discard();
			}
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noLowerRealmHoldsVoidShattering(GameTestHelper helper) {
		int cap = PlayerCultivation.rank(PlayerCultivation.LOWER_REALM_CAP_REALM, PlayerCultivation.LOWER_REALM_CAP_STAGE);
		PlayerCultivation c = new PlayerCultivation();
		c.setState(Realm.DAO_INTEGRATION, Stage.GRAND_PERFECTION, 0);
		c.setLowerRealmBound(true);
		helper.assertTrue(c.isSuppressed() && c.effectiveRank() == cap && c.meditationCultivationPerSecond(0) == 0,
				"Dao Integration in a lower realm: held to Heavenly Being Grand Perfection, gathering nothing");
		c.setState(Realm.VOID_SHATTERING, Stage.EARLY, 0);
		helper.assertTrue(!c.isSuppressed() && c.effectiveRank() == PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY)
				&& c.wielded(PlayerCultivation.LAST_RANK) == PlayerCultivation.LAST_RANK && c.meditationCultivationPerSecond(0) > 0,
				"Void Shattering: held by no lower realm (the user's ask, 2026-10-22), gathering its thin qi");
		c.setState(Realm.VOID_SHATTERING, Stage.EARLY, PlayerCultivation.required(Realm.VOID_SHATTERING, Stage.EARLY));
		helper.assertTrue(c.isAtBottleneck() && c.isBreakthroughLocked() && !c.canBreakthrough(), "...but it breaks through only in the Upper Realm");
		// Either path: a heaven path of Four Axis is held by nothing once the ghost path stands at Void Shattering.
		PlayerCultivation two = new PlayerCultivation();
		two.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		two.setLowerRealmBound(true);
		helper.assertTrue(two.isSuppressed() && two.effectiveRank() == cap, "Four Axis alone: held");
		two.netherworld().setMethod(GhostMethod.SIX_PATHS_CANON, 1, 0);
		GhostMethods.setPath(two.netherworld(), PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY));
		helper.assertTrue(!two.isSuppressed() && two.effectiveRank() == PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION),
				"...but not once its ghost path is Void Shattering");
		// An NPC cultivator the same, in the test's world (a lower realm).
		CultivatorNpc npc = CultivatorNpc.createFor(helper.getLevel());
		if (npc == null) throw new net.minecraft.gametest.framework.GameTestAssertException("No cultivator made");
		try {
			npc.setRank(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.MID), true);
			helper.assertTrue(npc.sustainedRank() == PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.MID), "A Void Shattering NPC is held by no lower realm either");
			npc.setRank(PlayerCultivation.rank(Realm.FOUR_AXIS, Stage.EARLY), true);
			helper.assertTrue(npc.sustainedRank() == cap, "...a Four Axis one is, as ever");
		} finally {
			HighRealms.gone(helper.getLevel().getServer(), npc.getUUID());
			npc.discard();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theirCultivatorsCarryAndBearMore(GameTestHelper helper) {
		RandomSource random = RandomSource.create(3);
		for (Realm realm : AFTER) {
			int rank = PlayerCultivation.rank(realm, Stage.EARLY);
			helper.assertTrue(!CultivatorNpc.carriedStones(rank, random).isEmpty(), realm.getId() + ": its cultivators carry spirit stones");
		}
		PlayerCultivation c = new PlayerCultivation();
		c.awaken(0);
		c.setState(Realm.FOUR_AXIS, Stage.EARLY, 0);
		double axis = YinCorruption.fullStaySeconds(c, 0);
		c.setState(Realm.DAO_INTEGRATION, Stage.EARLY, 0);
		double dao = YinCorruption.fullStaySeconds(c, 0);
		c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
		helper.assertTrue(Math.abs(dao - 2 * axis) < 1e-6 && YinCorruption.fullStaySeconds(c, 0) > dao, "The Netherworld's yin borne the longer, twice a realm");
		helper.succeed();
	}
}
