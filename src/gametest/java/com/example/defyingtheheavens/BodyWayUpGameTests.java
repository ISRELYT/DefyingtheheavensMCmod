package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Body Path's way up the worlds (the user's "Go, all three parts", 2026-11-01, part one): Cloud-Treading Steps, awakened at Silver Marrow,
 * a switch that grants flight at its own pace where no qi carries one, its vigor burnt only aloft and no rest there, out when it runs dry; the
 * gates counting the body (TwoPaths#travelRank): a Diamond Heart rides a Void Breach, an Undying Body passes the Immortal Gate and bears its
 * pressure by its body, a ghost's body counts for nothing; Comet Charge tears the void only high in the Jade Firmament's sky.
 */
public class BodyWayUpGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cloudTreadingStridesOnTheAirForVigorWithNoRestThere(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer low = bodyCultivator(helper, made, "tread-low", Realm.NASCENT_SOUL);
			helper.assertTrue(!Ability.CLOUD_TREADING.isUnlocked(CultivationManager.own(low)), "Jade Bone: no Cloud-Treading Steps yet");
			ServerPlayer p = bodyCultivator(helper, made, "tread", Realm.HEAVENLY_BEING);
			PlayerCultivation c = CultivationManager.own(p);
			helper.assertTrue(Ability.CLOUD_TREADING.isUnlocked(c) && !c.isAbilityEnabled(Ability.CLOUD_TREADING), "Silver Marrow: awake, off to begin with");
			QiFlight.tick(p, c);
			helper.assertTrue(!p.getAbilities().mayfly, "Off: the body has no flight");
			CultivationManager.toggleAbility(p, Ability.CLOUD_TREADING.ordinal());
			helper.assertTrue(c.isAbilityEnabled(Ability.CLOUD_TREADING) && near(c.body().vigor(), BodyTechniques.VIGOR), "Switched on, for nothing yet");
			QiFlight.tick(p, c);
			helper.assertTrue(p.getAbilities().mayfly && p.getAbilities().getFlyingSpeed() == BodyTechniques.TREAD_SPEED && !QiFlight.treading(p),
					"On: flight at its own pace, two thirds of Qi Flight's, not yet aloft");
			p.tickCount = 1000; // nothing given or taken this long: resting
			c.body().setVigor(50);
			BodyTechniques.second(p);
			helper.assertTrue(near(c.body().vigor(), 50 + BodyTechniques.maxVigor(c) * BodyTechniques.REST_SHARE), "On the ground it burns nothing, and rest refills");
			p.getAbilities().flying = true;
			QiFlight.tick(p, c);
			helper.assertTrue(QiFlight.treading(p) && p.getAbilities().flying, "Aloft on it");
			double before = c.body().vigor();
			BodyTechniques.second(p);
			helper.assertTrue(near(c.body().vigor(), before - BodyTechniques.TREAD_PER_SECOND), "Aloft: its vigor a second, and no rest");
			helper.assertTrue(BodyTechniques.maxVigor(c) / BodyTechniques.TREAD_PER_SECOND * 7.5 * BodyTechniques.TREAD_SPEED * 20
					> SpatialTrialHandler.CROSSING_Y - SpatialTrialHandler.TEAR_IN_Y, "A full pool carries the body up the Spatial Gap, rift to crossing");
			c.body().setVigor(BodyTechniques.TREAD_PER_SECOND + 1);
			BodyTechniques.second(p);
			helper.assertTrue(!c.isAbilityEnabled(Ability.CLOUD_TREADING), "Run dry aloft: the clouds give way");
			QiFlight.tick(p, c);
			helper.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying, "...and the body falls");
			CultivationManager.toggleAbility(p, Ability.CLOUD_TREADING.ordinal());
			helper.assertTrue(!c.isAbilityEnabled(Ability.CLOUD_TREADING), "Too little vigor for a second of it: refused");
			c.body().setVigor(80);
			CultivationManager.toggleAbility(p, Ability.CLOUD_TREADING.ordinal());
			CultivationManager.toggleAbility(p, Ability.CLOUD_TREADING.ordinal());
			QiFlight.tick(p, c);
			helper.assertTrue(!c.isAbilityEnabled(Ability.CLOUD_TREADING) && !p.getAbilities().mayfly && near(c.body().vigor(), 80),
					"On and off again by its switch, for nothing");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theGatesCountTheBody(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = bodyCultivator(helper, made, "way-up", Realm.FOUR_AXIS);
			PlayerCultivation c = CultivationManager.own(p);
			helper.assertTrue(TwoPaths.higherRank(c, false) < 0 && TwoPaths.travelRank(c) == BodyPath.rank(c), "A mortal on the Body Path travels by its body");
			helper.assertTrue(!VoidBreach.mayPass(p) && !ImmortalGate.mayPass(p), "Golden Blood: neither a breach nor the Gate");
			c.body().set(PlayerCultivation.rank(Realm.DAO_INTEGRATION, Stage.EARLY), 0);
			helper.assertTrue(VoidBreach.mayPass(p) && VoidBreach.mightFor(p) == VoidBreach.RIDE_RANK && !ImmortalGate.mayPass(p),
					"Diamond Heart: it rides a Void Breach, not yet the Gate");
			c.body().set(PlayerCultivation.rank(Realm.SACRED_VESSEL, Stage.GRAND_PERFECTION), 0);
			helper.assertTrue(!ImmortalGate.mayPass(p) && VoidBreach.mightFor(p) == BodyPath.rank(c), "The Heaven-Bone Body's Grand Perfection: still turned back at the Gate");
			c.body().set(PlayerCultivation.rank(Realm.ENTERING_NIRVANA, Stage.EARLY), 0);
			helper.assertTrue(ImmortalGate.mayPass(p) && near(ImmortalGate.carriedShare(p), ImmortalGate.pressureShare(ImmortalGate.PASS_RANK))
					&& ImmortalGate.carriedShare(p) < 1, "The Undying Body: through the Gate, its pressure weighed by the body");
			c.body().set(PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY), 0);
			helper.assertTrue(ImmortalGate.carriedShare(p) == 0, "The Indestructible Body bears none of it");
			c.netherworld().setGhost(true);
			helper.assertTrue(TwoPaths.travelRank(c) < 0 && !VoidBreach.mayPass(p) && !ImmortalGate.mayPass(p), "Dead, its body carries it nowhere");
			c.netherworld().setGhost(false);
			PlayerCultivation qi = new PlayerCultivation();
			qi.awaken(0);
			qi.setState(Realm.VOID_SHATTERING, Stage.MID, 0);
			helper.assertTrue(TwoPaths.travelRank(qi) == TwoPaths.higherRank(qi, false), "A qi cultivator travels by its qi as ever");
			c.body().set(PlayerCultivation.rank(Realm.VOID_SHATTERING, Stage.EARLY), 0);
			p.moveTo(p.getX(), VoidBreach.TEAR_Y + 12, p.getZ());
			helper.assertTrue(!VoidBreach.bodyTears(p, c) && !VoidBreach.tearsBreach(p, c), "Over the lower realms, however high, Comet Charge only charges");
			p.setOnGround(false);
			BodyTechniques.readyAgain(p.getUUID());
			c.body().setVigor(BodyTechniques.VIGOR);
			CultivationManager.toggleAbility(p, Ability.COMET_CHARGE.ordinal());
			helper.assertTrue(near(c.body().vigor(), BodyTechniques.VIGOR), "...and needs ground under its feet there");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	private static ServerPlayer bodyCultivator(GameTestHelper helper, List<ServerPlayer> made, String name, Realm realm) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		PlayerCultivation c = CultivationManager.own(p);
		c.setMortal(true);
		c.setMortalState(MortalStage.PEAK, 0);
		BodyPath.begin(p);
		c.body().set(PlayerCultivation.rank(realm, Stage.EARLY), 0);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			BodyTechniques.end(p);
			BodyTechniques.forget(p.getUUID());
			BodyPath.forget(p.getUUID());
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
