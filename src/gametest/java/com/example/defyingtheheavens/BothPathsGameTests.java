package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Both paths ways to the heights (the user's ask, 2026-10-18; TwoPaths#awakens): the realm's abilities from either path, paid
 * from that path's pool; the dead and the living in a Ghost King's form flying freely (TwoPaths#fliesFreely), swifter for
 * Wandering Soul Steps' fourth layer on (QiFlight#freeSpeed); the ghost path's own fall (GhostMethods#fallTo).
 */
public class BothPathsGameTests implements FabricGameTest {
	/** A living cultivator in survival (the helper's mock player always counts as creative, where vanilla keeps the flight). */
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "both-paths"));
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	/** The ghost path at {@code realm} Early: the foundations before it perfected, its own at its first layer of that realm. */
	private static void ghostPath(ServerPlayer player, Realm realm) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		for (GhostMethod method : GhostMethod.values()) {
			if (method.isFoundation()) s.setMethod(method, method.realm().ordinal() < realm.ordinal() ? method.layers()
					: method.realm() == realm ? MethodLayers.PER_REALM * realm.ordinal() + 1 : 0, 0);
		}
		CultivationManager.refresh(player);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRealmsAbilitiesComeFromEitherPath(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.LATE);
		PlayerCultivation c = CultivationManager.get(p);
		try {
			helper.assertTrue(!Ability.QI_SENSE.isUnlocked(c) && !Ability.CORE_DETONATION.isUnlocked(c), "Foundation Building on one path: no Qi Sense, no core");
			ghostPath(p, Realm.CORE_FORMATION);
			helper.assertTrue(Ability.QI_SENSE.isUnlocked(c) && Ability.QI_SENSE.awakenedBy(c) == TwoPaths.Path.GHOST,
					"A ghost path of Core Formation awakens Qi Sense");
			helper.assertTrue(Ability.CORE_DETONATION.isUnlocked(c) && CoreDetonation.corePath(c) == TwoPaths.Path.GHOST,
					"...and the core to detonate is the ghost path's");
			helper.assertTrue(Ability.QI_SUSTENANCE.awakenedBy(c) == TwoPaths.Path.HEAVEN, "Qi Sustenance stays the heaven path's, which reached it first");
			helper.assertTrue(!Ability.QI_FLIGHT.isUnlocked(c) && !Ability.SOUL_PROJECTION.isUnlocked(c),
					"Qi Flight is the heaven path's alone, and Soul Projection waits for Nascent Soul");
			ghostPath(p, Realm.NASCENT_SOUL);
			helper.assertTrue(Ability.SOUL_PROJECTION.isUnlocked(c) && c.soulPath() == TwoPaths.Path.GHOST, "A ghost path of Nascent Soul projects its own soul");
			helper.assertTrue(Math.abs(c.soulCostPerSecond(0, false) - SoulProjection.GHOST_SOUL_BASE * 0.5) < 1.0e-9,
					"...reckoned from the ghostly pool's gathering (" + c.soulCostPerSecond(0, false) + ")");
			ghostPath(p, Realm.HEAVENLY_BEING);
			helper.assertTrue(Ability.SPATIAL_TEAR.isUnlocked(c) && Ability.SPATIAL_TEAR.awakenedBy(c) == TwoPaths.Path.GHOST,
					"A ghost path of Heavenly Being tears space");
			// With the heaven path there as well, the heaven path awakens it, as it always did.
			c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
			CultivationManager.refresh(p);
			helper.assertTrue(c.soulPath() == TwoPaths.Path.HEAVEN && Ability.QI_SENSE.awakenedBy(c) == TwoPaths.Path.HEAVEN,
					"The heaven path at the realm awakens it first");
		} finally {
			TwoPaths.forget(p.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void eachPathPaysFromItsOwnPool(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(p);
		try {
			ghostPath(p, Realm.CORE_FORMATION);
			c.setQi(c.maxQi());
			c.netherworld().setGhostQi(100);
			double qi = c.getQi();
			helper.assertTrue(TwoPaths.spend(c, TwoPaths.Path.GHOST, 40) && Math.abs(c.netherworld().ghostQi() - 60) < 1.0e-9 && c.getQi() == qi,
					"The ghost path's cost comes out of the ghost qi, not the qi");
			helper.assertTrue(!TwoPaths.spend(c, TwoPaths.Path.GHOST, 61) && Math.abs(c.netherworld().ghostQi() - 60) < 1.0e-9,
					"...and isn't paid at all when the ghost qi is short");
			helper.assertTrue(TwoPaths.poolMax(c, TwoPaths.Path.GHOST) == GhostQi.poolMax(c) && TwoPaths.poolMax(c, TwoPaths.Path.HEAVEN) == c.maxQi(),
					"Each path's pool is its own");
		} finally {
			TwoPaths.forget(p.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void ghostlyCreaturesFlyFreelyAtEveryRealm(GameTestHelper helper) {
		ServerPlayer dead = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer king = cultivator(helper, Realm.QI_REFINING, Stage.LATE);
		try {
			// One of the dead, of the very first realm: flight at Qi Flight's speed, for nothing.
			GhostSystem.makeGhost(dead);
			PlayerCultivation d = CultivationManager.get(dead);
			double before = d.netherworld().ghostQi();
			double cost = QiFlight.tick(dead, d);
			dead.getAbilities().flying = true;
			for (int i = 0; i < 5; i++) cost += QiFlight.tick(dead, d);
			helper.assertTrue(dead.getAbilities().mayfly && dead.getAbilities().flying, "The dead fly at Qi Refining");
			helper.assertTrue(dead.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED, "...as swiftly as on qi (" + dead.getAbilities().getFlyingSpeed() + ")");
			helper.assertTrue(cost == 0 && d.netherworld().ghostQi() == before && !Ability.GHOST_FLIGHT.isUnlocked(d), "...for nothing, and Ghost Flight is no more");
			// One of the living whose ghost path stands above the heaven path, in its true form, a Ghost King's: flies freely too.
			ghostPath(king, Realm.CORE_FORMATION);
			PlayerCultivation k = CultivationManager.get(king);
			k.setQi(k.maxQi());
			helper.assertTrue(TwoPaths.inGhostForm(k), "In a Ghost King's form");
			double qi = k.getQi();
			cost = QiFlight.tick(king, k);
			king.getAbilities().flying = true;
			for (int i = 0; i < 5; i++) cost += QiFlight.tick(king, k);
			helper.assertTrue(king.getAbilities().mayfly && king.getAbilities().flying && cost == 0 && k.getQi() >= qi,
					"The living in a Ghost King's form fly freely at Qi Refining, spending nothing");
			// Its human form taken: the heaven path's Qi Refining has no flight.
			CultivationManager.toggleAbility(king, Ability.FORM_SHIFT.ordinal());
			QiFlight.tick(king, k);
			helper.assertTrue(!TwoPaths.inGhostForm(k) && !king.getAbilities().mayfly, "In its human form, the heaven path's flight or none");
		} finally {
			GhostSystem.forget(dead.getUUID());
			TwoPaths.forget(king.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void soulStepsSpeedTheFreeFlight(GameTestHelper helper) {
		ServerPlayer dead = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		ServerPlayer king = cultivator(helper, Realm.QI_REFINING, Stage.LATE);
		ServerPlayer walker = cultivator(helper, Realm.QI_REFINING, Stage.EARLY);
		try {
			GhostSystem.makeGhost(dead);
			PlayerCultivation d = CultivationManager.get(dead);
			d.netherworld().setMethod(GhostMethod.WANDERING_SOUL_STEPS, 3, 0);
			QiFlight.tick(dead, d);
			helper.assertTrue(dead.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED, "Three layers of the steps: Qi Flight's speed");
			d.netherworld().setMethod(GhostMethod.WANDERING_SOUL_STEPS, 4, 0);
			QiFlight.tick(dead, d);
			float fourth = dead.getAbilities().getFlyingSpeed();
			helper.assertTrue(Math.abs(fourth - SwordFlight.QI_FLIGHT_SPEED * 1.1f) < 1.0e-6, "The fourth: a tenth swifter (" + fourth + ")");
			d.netherworld().setMethod(GhostMethod.WANDERING_SOUL_STEPS, 9, 0);
			QiFlight.tick(dead, d);
			float ninth = dead.getAbilities().getFlyingSpeed();
			helper.assertTrue(Math.abs(ninth - SwordFlight.QI_FLIGHT_SPEED * 1.6f) < 1.0e-6, "The ninth: +60% (" + ninth + ")");
			// The living in a Ghost King's form fly as swiftly.
			ghostPath(king, Realm.CORE_FORMATION);
			PlayerCultivation k = CultivationManager.get(king);
			k.netherworld().setMethod(GhostMethod.WANDERING_SOUL_STEPS, 9, 0);
			QiFlight.tick(king, k);
			helper.assertTrue(TwoPaths.inGhostForm(k) && king.getAbilities().getFlyingSpeed() == ninth, "A Ghost King's form flies as swiftly");
			// A speed saved with the player and brought back after a relog (nothing remembered here) is still known: one without
			// flight now goes back to vanilla's speed, from the steps' flight and from space's alike. Another mod's is left alone.
			PlayerCultivation w = CultivationManager.get(walker);
			float[] saved = {ninth, AstralRealm.spaceSpeed(Realm.HEAVENLY_BEING, Stage.MID)};
			for (float speed : saved) {
				walker.getAbilities().setFlyingSpeed(speed);
				QiFlight.forget(walker.getUUID());
				QiFlight.tick(walker, w);
				helper.assertTrue(walker.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED && !walker.getAbilities().mayfly,
						"A saved " + speed + " goes back to Qi Flight's speed (" + walker.getAbilities().getFlyingSpeed() + ")");
			}
			walker.getAbilities().setFlyingSpeed(0.123f);
			QiFlight.forget(walker.getUUID());
			QiFlight.tick(walker, w);
			helper.assertTrue(walker.getAbilities().getFlyingSpeed() == 0.123f, "Another mod's speed is left alone");
		} finally {
			GhostSystem.forget(dead.getUUID());
			TwoPaths.forget(king.getUUID());
			QiFlight.forget(walker.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGhostPathFallsAsTheHeavenPathDoes(GameTestHelper helper) {
		ServerPlayer p = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		PlayerCultivation c = CultivationManager.get(p);
		try {
			ghostPath(p, Realm.HEAVENLY_BEING);
			int foundationGrand = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.GRAND_PERFECTION);
			GhostMethods.fallTo(c.netherworld(), foundationGrand);
			helper.assertTrue(TwoPaths.ghostRank(c, false) == foundationGrand, "The ghost path falls to Foundation Building Grand Perfection ("
					+ TwoPaths.ghostRank(c, false) + ")");
			for (GhostMethod method : c.netherworld().knownMethods()) {
				helper.assertTrue(c.netherworld().layer(method) <= MethodLayers.deepestAt(foundationGrand), method.getId() + " held no deeper than that");
			}
			int before = TwoPaths.ghostRank(c, false);
			GhostMethods.fallTo(c.netherworld(), PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
			helper.assertTrue(TwoPaths.ghostRank(c, false) == before, "A fall to a rank above it changes nothing");
		} finally {
			TwoPaths.forget(p.getUUID());
		}
		helper.succeed();
	}
}
