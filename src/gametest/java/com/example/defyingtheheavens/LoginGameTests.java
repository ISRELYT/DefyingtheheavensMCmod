package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * Logging back in (the sweep of 2026-11-06): Fabric's JOIN comes before the world has taken a player in, and one moved there was left in two worlds' lists
 * of players and then in none: told of the wrong world's chunks, unseen by the others, walking on the spot until they logged in again. Each test here saves
 * a player somewhere they can't stay, and logs them back in as the game does (a new player, their saved data read back): they are moved once they stand in
 * the world, and stand in one world, the one they say they are in. Each test in a batch of its own.
 */
public class LoginGameTests implements FabricGameTest {
	/** {@code profile} into the game, as a login does it. */
	private static ServerPlayer logIn(GameTestHelper helper, GameProfile profile) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer p = new ServerPlayer(server, helper.getLevel(), profile) {
			@Override
			public boolean isCreative() { return false; }
		};
		server.getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		return p;
	}

	/** A survival player in earnest, of {@code realm} Early, at the test's corner. */
	private static ServerPlayer player(GameTestHelper helper, Realm realm, GameProfile profile) {
		ServerPlayer p = logIn(helper, profile);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
		return p;
	}

	private static GameProfile profile(String name) {
		return new GameProfile(UUID.randomUUID(), name);
	}

	/** Out of the game as a logout does it: what they were doing ended, and their place saved. */
	private static void logOut(ServerPlayer p) {
		if (p.server.getPlayerList().getPlayer(p.getUUID()) != p) return;
		CultivationEvents.leaving(p);
		p.server.getPlayerList().remove(p);
	}

	/** The name of the one world that holds {@code p} and that they say they stand in, or what is wrong with it. */
	private static String standing(ServerPlayer p) {
		StringBuilder lists = new StringBuilder();
		int count = 0;
		for (ServerLevel level : p.server.getAllLevels()) {
			if (!level.players().contains(p)) continue;
			count++;
			lists.append(level.dimension().location()).append(' ');
		}
		if (count != 1) return "in " + count + " worlds' lists: " + lists;
		if (!p.level().players().contains(p)) return "in the list of " + lists + "but says it stands in " + p.level().dimension().location();
		if (p.serverLevel().getEntity(p.getUUID()) != p) return "not found as an entity in " + lists;
		return "";
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "login_secret_realm", timeoutTicks = 400)
	public void backFromASecretRealmInOneWorld(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		GameProfile profile = profile("test-login-s");
		ServerPlayer first = player(helper, Realm.QI_REFINING, profile);
		SecretRealms.Record r = SecretRealms.Data.get(server).of(SecretRealm.VERDANT_HERB_VALLEY);
		if (r.openedAt < 0) SecretRealms.openNow(server, SecretRealm.VERDANT_HERB_VALLEY);
		SecretRealms.enter(first, r);
		helper.assertTrue(ModDimensions.isSecretRealm(first.level().dimension()), "In the realm");
		logOut(first);
		ServerPlayer back = logIn(helper, profile);
		boolean came = ModDimensions.isSecretRealm(back.level().dimension());
		helper.runAfterDelay(2, () -> {
			String wrong = standing(back);
			boolean out = !ModDimensions.isSecretRealm(back.level().dimension()), listed = r.inside.contains(back.getUUID());
			logOut(back);
			helper.assertTrue(came, "Back where they left, the world taking them in");
			helper.assertTrue(wrong.isEmpty(), "Stands in one world: " + wrong);
			helper.assertTrue(out && !listed, "...out of the realm, once it had");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "login_inner_realm", timeoutTicks = 400)
	public void backFromTheInnerRealmInOneWorld(GameTestHelper helper) {
		GameProfile profile = profile("test-login-i");
		ServerPlayer first = player(helper, Realm.CORE_FORMATION, profile);
		ServerLevel level = helper.getLevel();
		helper.assertTrue(InnerRealm.enter(first) && InnerRealm.isInside(first), "Gone inward");
		logOut(first);
		ServerPlayer back = logIn(helper, profile);
		boolean came = InnerRealm.isInside(back);
		helper.runAfterDelay(2, () -> {
			String wrong = standing(back);
			boolean home = back.level() == level;
			logOut(back);
			helper.assertTrue(came, "Back where they left, the world taking them in");
			helper.assertTrue(wrong.isEmpty(), "Stands in one world: " + wrong);
			helper.assertTrue(home, "...back at their body, once it had");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "login_small_world", timeoutTicks = 400)
	public void outOfAWorldNotTheirsInOneWorld(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		SmallWorlds.Data data = SmallWorlds.Data.get(server);
		ServerPlayer owner = player(helper, Realm.TRUE_IMMORTAL, profile("test-login-o"));
		GameProfile profile = profile("test-login-g");
		ServerPlayer first = player(helper, Realm.TRUE_IMMORTAL, profile);
		helper.assertTrue(SmallWorlds.refine(owner), "Refined");
		SmallWorlds.World w = data.of(owner.getUUID());
		SmallWorlds.addGuest(owner, first);
		helper.assertTrue(SmallWorlds.enter(first, w), "A guest in the world");
		logOut(first);
		w.guests.clear(); // struck off its list while away
		ServerPlayer back = logIn(helper, profile);
		boolean came = ModDimensions.isSmallWorld(back.level().dimension());
		helper.runAfterDelay(2, () -> {
			String wrong = standing(back);
			boolean out = !ModDimensions.isSmallWorld(back.level().dimension());
			logOut(back);
			data.forget(owner.getUUID());
			logOut(owner);
			helper.assertTrue(came, "Back where they left, the world taking them in");
			helper.assertTrue(wrong.isEmpty(), "Stands in one world: " + wrong);
			helper.assertTrue(out, "...sent out of it, once it had");
			helper.succeed();
		});
	}
}
