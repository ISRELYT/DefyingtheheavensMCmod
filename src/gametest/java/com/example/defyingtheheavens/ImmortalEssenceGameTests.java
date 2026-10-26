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

import java.util.UUID;

/**
 * Immortal essence (phase 2, 2026-10-25; the user's pick for a True Immortal below the Gate: "Keep power, lose essence"): a True
 * Immortal's pool by its stage, filled only in the Immortal Realm (the faster meditating), leaking away anywhere else; its switch, half
 * again as hard while lit, burning the pool, out when it runs dry, refused on too little, out at a logout; nothing short of True Immortal.
 */
public class ImmortalEssenceGameTests implements FabricGameTest {
	/** A survival player in earnest (a mock player answers that it is creative), of {@code realm} {@code stage}, at the test's corner. */
	private static ServerPlayer survivor(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "test-essence")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		CultivationManager.get(p).setState(realm, stage, 0);
		CultivationManager.refresh(p);
		p.setHealth(p.getMaxHealth());
		return p;
	}

	private static void leave(ServerPlayer... players) {
		for (ServerPlayer p : players) {
			ImmortalEssence.onDisconnect(p);
			BurnLifespan.onDisconnect(p);
			Lifespan.forget(p.getUUID());
			LifespanTribulation.forget(p.getUUID());
			if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
	}

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1e-6;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTrueImmortalsPoolByItsStage(GameTestHelper helper) {
		for (Stage stage : Stage.values()) {
			PlayerCultivation c = new PlayerCultivation();
			c.setMortal(false);
			c.setState(Realm.TRUE_IMMORTAL, stage, 0);
			helper.assertTrue(ImmortalEssence.max(c) == ImmortalEssence.POOL[stage.ordinal()] && Ability.IMMORTAL_ESSENCE.isUnlocked(c),
					"True Immortal " + stage.getId() + ": a pool of " + ImmortalEssence.POOL[stage.ordinal()] + ", its switch awakened");
			c.setEssence(1000);
			helper.assertTrue(c.essence() == ImmortalEssence.max(c), "Never more than its pool");
		}
		PlayerCultivation nirvana = new PlayerCultivation();
		nirvana.setMortal(false);
		nirvana.setState(Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION, 0);
		nirvana.setEssence(50);
		helper.assertTrue(ImmortalEssence.max(nirvana) == 0 && nirvana.essence() == 0 && !Ability.IMMORTAL_ESSENCE.isUnlocked(nirvana),
				"Nothing short of True Immortal");
		helper.assertTrue(!Ability.IMMORTAL_ESSENCE.isUnlocked(new PlayerCultivation()), "Nor for a mortal");
		helper.assertTrue(Ability.offByDefault().contains(Ability.IMMORTAL_ESSENCE), "Off to begin with");
		// Saved and loaded.
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(false);
		c.setState(Realm.TRUE_IMMORTAL, Stage.MID, 0);
		c.setEssence(77.5);
		helper.assertTrue(PlayerCultivation.load(c.save()).essence() == 77.5, "Kept in the save");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itFillsOnlyInTheImmortalRealm(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		c.setMortal(false);
		c.setState(Realm.TRUE_IMMORTAL, Stage.EARLY, 0);
		helper.assertTrue(near(ImmortalEssence.perSecond(c, true, false), 100.0 / 600), "The Immortal Realm fills it, empty to full in ten minutes");
		helper.assertTrue(near(ImmortalEssence.perSecond(c, true, true), 4 * 100.0 / 600), "...four times as fast meditating");
		helper.assertTrue(near(ImmortalEssence.perSecond(c, false, false), -100.0 / 1200) && near(ImmortalEssence.perSecond(c, false, true), -100.0 / 1200),
				"Anywhere else it leaks away, a full pool in twenty minutes, meditating or not");
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, Stage.EARLY);
		try {
			PlayerCultivation own = CultivationManager.get(p);
			own.setEssence(50);
			ImmortalEssence.second(p);
			helper.assertTrue(near(own.essence(), 50 - 100.0 / 1200), "A second below the Gate: leaked (" + own.essence() + ")");
			own.setEssence(0.01);
			ImmortalEssence.second(p);
			helper.assertTrue(own.essence() == 0, "...never below empty");
		} finally {
			leave(p);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void itsSwitchStrikesHarderForThePool(GameTestHelper helper) {
		ServerPlayer p = survivor(helper, Realm.TRUE_IMMORTAL, Stage.EARLY), low = survivor(helper, Realm.ENTERING_NIRVANA, Stage.GRAND_PERFECTION);
		try {
			PlayerCultivation c = CultivationManager.get(p);
			helper.assertTrue(Ability.IMMORTAL_ESSENCE.isUnlocked(c) && !ImmortalEssence.isLit(p), "Awakened, and off to begin with");
			// Too little to light: under five seconds of it.
			c.setEssence(100.0 / 90 * 5 - 0.1);
			CultivationManager.toggleAbility(p, Ability.IMMORTAL_ESSENCE.ordinal());
			helper.assertTrue(!ImmortalEssence.isLit(p), "Won't light on too little");
			c.setEssence(50);
			CultivationManager.toggleAbility(p, Ability.IMMORTAL_ESSENCE.ordinal());
			helper.assertTrue(ImmortalEssence.isLit(p), "Lit");
			helper.assertTrue(near(Physiques.outgoing(p.damageSources().playerAttack(p), 10), 15), "Its blows half again as hard");
			ImmortalEssence.second(p);
			helper.assertTrue(near(c.essence(), 50 - 100.0 / 1200 - 100.0 / 90), "A second of it lit: a ninetieth of the pool, and the leak (" + c.essence() + ")");
			// With its lifespan burning too (which costs an immortal nothing): both.
			CultivationManager.toggleAbility(p, Ability.BURN_LIFESPAN.ordinal());
			helper.assertTrue(near(Physiques.outgoing(p.damageSources().playerAttack(p), 10), 22.5), "With Burn Lifespan: half again, and half again");
			CultivationManager.toggleAbility(p, Ability.BURN_LIFESPAN.ordinal());
			// Run dry, it goes out.
			c.setEssence(0.5);
			ImmortalEssence.second(p);
			helper.assertTrue(!ImmortalEssence.isLit(p) && c.essence() == 0, "Out when the pool runs dry");
			helper.assertTrue(near(Physiques.outgoing(p.damageSources().playerAttack(p), 10), 10), "...its blows as they were");
			// A logout puts it out.
			c.setEssence(80);
			CultivationManager.toggleAbility(p, Ability.IMMORTAL_ESSENCE.ordinal());
			helper.assertTrue(ImmortalEssence.isLit(p), "Lit again");
			ImmortalEssence.onDisconnect(p);
			helper.assertTrue(!ImmortalEssence.isLit(p), "Out at a logout");
			// Short of True Immortal: no switch to throw.
			CultivationManager.toggleAbility(low, Ability.IMMORTAL_ESSENCE.ordinal());
			helper.assertTrue(!CultivationManager.get(low).isAbilityEnabled(Ability.IMMORTAL_ESSENCE), "Nothing to light short of True Immortal");
		} finally {
			leave(p, low);
		}
		helper.succeed();
	}
}
