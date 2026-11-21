package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The breakthrough cinematics' server side (Cinematics, the VFX plan's step B1, 2026-11-21): which version a breakthrough plays; the
 * record of those seen, kept with the player by path; none at all in a game without the dev flag (every player's, until step B2); and
 * the hold: the player kept where they sat and harmed by nothing that can be warded off, let go when its length is up, on a skip or at
 * their death; at the blast the hostile mobs near thrown back unharmed. The two that throw mobs about run in a batch of their own, so no
 * other test's mobs are within their reach.
 */
public class CinematicsGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void whichVersionABreakthroughPlays(GameTestHelper helper) {
		Realm hb = Realm.HEAVENLY_BEING;
		Cinematics.Version later = Cinematics.SHORT_READY ? Cinematics.Version.SHORT : Cinematics.Version.FULL;
		helper.assertTrue(Cinematics.versionFor(hb, Stage.EARLY, Cinematics.Pref.FULL, false) == Cinematics.Version.FULL,
				"Into Heavenly Being the first time by a path: the full version");
		helper.assertTrue(Cinematics.versionFor(hb, Stage.EARLY, Cinematics.Pref.FULL, true) == later,
				"...seen before: the short one once it is made (until then the full)");
		helper.assertTrue(Cinematics.versionFor(hb, Stage.EARLY, Cinematics.Pref.SHORT, false) == later, "...asked for short: the same");
		helper.assertTrue(Cinematics.versionFor(hb, Stage.EARLY, Cinematics.Pref.OFF, false) == null, "...turned off: none");
		helper.assertTrue(Cinematics.versionFor(hb, Stage.MID, Cinematics.Pref.FULL, false) == null
				&& Cinematics.versionFor(hb, Stage.GRAND_PERFECTION, Cinematics.Pref.FULL, false) == null, "A new stage within a realm: none");
		helper.assertTrue(Cinematics.versionFor(Realm.CORE_FORMATION, Stage.EARLY, Cinematics.Pref.FULL, false) == null,
				"A realm without a cinematic yet: none (its old burst)");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theSeenRecordIsKeptByPath(GameTestHelper helper) {
		String heaven = Cinematics.key(TwoPaths.Path.HEAVEN, Realm.HEAVENLY_BEING), ghost = Cinematics.key(TwoPaths.Path.GHOST, Realm.HEAVENLY_BEING);
		PlayerCultivation c = new PlayerCultivation();
		helper.assertTrue(!c.hasSeenCinematic(heaven), "A new cultivator has seen none");
		c.markCinematicSeen(heaven);
		PlayerCultivation loaded = PlayerCultivation.load(c.save());
		helper.assertTrue(loaded.hasSeenCinematic(heaven), "One seen is remembered through a save and a load");
		helper.assertTrue(!loaded.hasSeenCinematic(ghost), "...by its path: the same realm by another path is still new");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRealBreakthroughPlaysNoneWithoutTheDevFlag(GameTestHelper helper) {
		boolean command = helper.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("breakthroughfx") != null;
		helper.assertTrue(command == DevTools.ENABLED, "/breakthroughfx is there exactly when the dev flag is on (flag: " + DevTools.ENABLED + ")");
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = player(helper, made);
			boolean played = Cinematics.onBreakthrough(p, Realm.HEAVENLY_BEING, Stage.EARLY, TwoPaths.Path.HEAVEN);
			helper.assertTrue(played == DevTools.ENABLED && Cinematics.isPlaying(p) == DevTools.ENABLED,
					"A breakthrough into Heavenly Being plays its cinematic only with the dev flag until step B2: everyone else keeps the old burst");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "cinematics")
	public void theBlastThrowsHostilesBackUnharmed(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		Vec3 at = helper.absoluteVec(new Vec3(2.5, 2, 2.5));
		helper.setBlock(new BlockPos(5, 1, 2), Blocks.STONE);
		helper.setBlock(new BlockPos(2, 1, 5), Blocks.STONE);
		Spider spider = helper.spawn(EntityType.SPIDER, new BlockPos(5, 2, 2));
		Cow cow = helper.spawn(EntityType.COW, new BlockPos(2, 2, 5));
		try {
			float health = spider.getHealth();
			Vec3 cowWas = cow.getDeltaMovement();
			List<Mob> thrown = Cinematics.throwBack(level, at);
			Vec3 push = spider.getDeltaMovement(), away = spider.position().subtract(at).multiply(1, 0, 1).normalize();
			helper.assertTrue(thrown.contains(spider) && !thrown.contains(cow), "The hostile mob near is thrown back; the cow isn't");
			helper.assertTrue(push.x * away.x + push.z * away.z > 1.5 && push.y > 0, "...away from the player, hard (" + push + ")");
			helper.assertTrue(cow.getDeltaMovement().equals(cowWas), "...the cow left as it was");
			helper.assertTrue(spider.isAlive() && spider.getHealth() == health, "...and the spider unharmed");
			helper.succeed();
		} finally {
			spider.discard();
			cow.discard();
			helper.setBlock(new BlockPos(5, 1, 2), Blocks.AIR);
			helper.setBlock(new BlockPos(2, 1, 5), Blocks.AIR);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "cinematics", timeoutTicks = 400)
	public void theHoldKeepsThePlayerUnharmedAndLetsGo(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ServerPlayer> made = new ArrayList<>();
		ServerPlayer p = player(helper, made);
		Vec3 at = p.position();
		// The short timing (240 ticks) to wait less; the server holds either the same way.
		int length = CinematicTimes.of(Realm.HEAVENLY_BEING).of(false).length();
		guarded(made, () -> {
			Cinematics.start(p, Realm.HEAVENLY_BEING, Stage.EARLY, TwoPaths.Path.HEAVEN, Cinematics.Version.SHORT, 7L);
			helper.assertTrue(Cinematics.isPlaying(p), "It plays");
			float health = p.getHealth();
			p.hurt(level.damageSources().generic(), 6);
			helper.assertTrue(p.getHealth() == health, "Held, the player is harmed by nothing that can be warded off");
			p.hurt(level.damageSources().fellOutOfWorld(), 2);
			helper.assertTrue(p.getHealth() < health, "...but the void still harms them");
			p.setHealth(p.getMaxHealth());
			p.teleportTo(at.x + 3, at.y, at.z); // pushed off where they sat
		});
		helper.startSequence()
				.thenExecuteAfter(3, () -> guarded(made, () ->
						helper.assertTrue(p.position().distanceTo(at) < 0.6, "...and put back where they sat (" + p.position().distanceTo(at) + " off)")))
				.thenExecuteAfter(length - 30, () -> guarded(made, () -> helper.assertTrue(Cinematics.isPlaying(p), "Still held just short of its length")))
				.thenExecuteAfter(40, () -> guarded(made, () -> {
					helper.assertTrue(!Cinematics.isPlaying(p), "Let go once its length is up");
					Cinematics.start(p, Realm.HEAVENLY_BEING, Stage.EARLY, TwoPaths.Path.HEAVEN, Cinematics.Version.FULL, 8L);
					Cinematics.end(p.server, p.getUUID(), "skipped");
					helper.assertTrue(!Cinematics.isPlaying(p), "Let go at once on a skip");
					Cinematics.start(p, Realm.HEAVENLY_BEING, Stage.EARLY, TwoPaths.Path.HEAVEN, Cinematics.Version.FULL, 9L);
					p.kill();
					helper.assertTrue(!Cinematics.isPlaying(p), "Let go at their death");
				}))
				.thenExecute(() -> leave(made))
				.thenSucceed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "cinematics")
	public void theHeldActOnNothing(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<ServerPlayer> made = new ArrayList<>();
		helper.setBlock(new BlockPos(2, 1, 4), Blocks.STONE);
		Cow cow = helper.spawn(EntityType.COW, new BlockPos(2, 2, 4));
		try {
			ServerPlayer p = player(helper, made);
			BlockPos block = helper.absolutePos(new BlockPos(2, 1, 2));
			boolean[] ran = {false};
			ModPackets.unlessHeld(level.getServer(), p, () -> ran[0] = true);
			helper.assertTrue(ran[0], "Not held, what a player sends to act is done");
			Cinematics.start(p, Realm.HEAVENLY_BEING, Stage.EARLY, TwoPaths.Path.HEAVEN, Cinematics.Version.FULL, 3L);
			helper.assertTrue(AttackEntityCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND, cow, null) == InteractionResult.FAIL,
					"Held, they strike no blow");
			helper.assertTrue(AttackBlockCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND, block, Direction.UP) == InteractionResult.FAIL,
					"...hit no block");
			helper.assertTrue(UseItemCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
					"...use nothing");
			helper.assertTrue(UseBlockCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND,
					new BlockHitResult(Vec3.atCenterOf(block), Direction.UP, block, false)) == InteractionResult.FAIL, "...use no block, place nothing");
			helper.assertTrue(UseEntityCallback.EVENT.invoker().interact(p, level, InteractionHand.MAIN_HAND, cow, null) == InteractionResult.FAIL,
					"...use no creature");
			helper.assertTrue(!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, p, block, level.getBlockState(block), null),
					"...break no block");
			ran[0] = false;
			ModPackets.unlessHeld(level.getServer(), p, () -> ran[0] = true);
			helper.assertTrue(!ran[0], "...and what they send to act (a cast, an ability, a strike, a breakthrough) is dropped");
			helper.succeed();
		} finally {
			leave(made);
			cow.discard();
			helper.setBlock(new BlockPos(2, 1, 4), Blocks.AIR);
		}
	}

	/** A survival player on the player list (the hold looks them up there), standing at (2, 2, 2) on stone. */
	private static ServerPlayer player(GameTestHelper helper, List<ServerPlayer> made) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "cinematic")) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		made.add(p);
		p.setGameMode(GameType.SURVIVAL);
		helper.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		return p;
	}

	/** A step that, failing, still takes its players off the list (and so lets any hold of theirs go). */
	private static void guarded(List<ServerPlayer> made, Runnable step) {
		try {
			step.run();
		} catch (RuntimeException | AssertionError e) {
			leave(made);
			throw e;
		}
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			Cinematics.end(p.server, p.getUUID(), "the test ended");
			if (p.server.getPlayerList().getPlayer(p.getUUID()) == p) p.server.getPlayerList().remove(p);
		}
		made.clear();
	}
}
