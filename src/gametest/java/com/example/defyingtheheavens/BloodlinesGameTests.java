package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * The demon races (the user's ask, 2026-10-23), their first part: a bloodline chosen once on joining (Bloodlines) and kept for the
 * world, through every life; a fox spirit's tails by its realm; and the old "demonic" path now named devilish. The looks (a race's
 * skin, its head and tail) and the race picker are the client's, seen in game.
 */
public class BloodlinesGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper) {
		return FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "bloodline"));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBloodlineIsChosenOnceForGood(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer p = player(helper);
		try {
			helper.assertTrue(Bloodlines.entry(server, p.getUUID()) == null && Bloodlines.of(server, p.getUUID()) == Bloodline.HUMAN,
					"None chosen yet: a human's look");
			Bloodlines.choose(p, Bloodline.TIGER, 2);
			Bloodlines.Entry e = Bloodlines.entry(server, p.getUUID());
			helper.assertTrue(e != null && e.bloodline() == Bloodline.TIGER && e.variant() == 2, "Chosen: a black tiger (" + e + ")");
			Bloodlines.choose(p, Bloodline.FOX, 0);
			helper.assertTrue(Bloodlines.of(server, p.getUUID()) == Bloodline.TIGER, "A second choice is ignored");
			Bloodlines.set(server, p.getUUID(), Bloodline.FOX, 5);
			e = Bloodlines.entry(server, p.getUUID());
			helper.assertTrue(e.bloodline() == Bloodline.FOX && e.variant() == 2, "The command makes it a fox spirit (its colouring wrapped: " + e + ")");
			Bloodlines.forget(server, p.getUUID());
			helper.assertTrue(Bloodlines.entry(server, p.getUUID()) == null, "...or undoes the choice, to be asked again");
			helper.succeed();
		} finally {
			Bloodlines.forget(server, p.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bloodlinesAreKeptWithTheWorld(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		UUID dragon = UUID.randomUUID(), human = UUID.randomUUID();
		try {
			Bloodlines.set(server, dragon, Bloodline.DRAGON, 1);
			Bloodlines.set(server, human, Bloodline.HUMAN, 0);
			Bloodlines loaded = Bloodlines.load(Bloodlines.get(server).save(new CompoundTag()));
			CompoundTag again = loaded.save(new CompoundTag());
			Bloodlines reread = Bloodlines.load(again);
			helper.assertTrue(reread.save(new CompoundTag()).equals(again), "Saved and loaded alike");
			helper.assertTrue(Bloodlines.entry(server, dragon).equals(new Bloodlines.Entry(Bloodline.DRAGON, 1))
					&& Bloodlines.entry(server, human).equals(new Bloodlines.Entry(Bloodline.HUMAN, 0)), "A golden dragon, and one chosen human");
			helper.succeed();
		} finally {
			Bloodlines.forget(server, dragon);
			Bloodlines.forget(server, human);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aFoxSpiritGrowsATailEachRealm(GameTestHelper helper) {
		helper.assertTrue(Bloodline.tails(Realm.QI_REFINING) == 1 && Bloodline.tails(Realm.FOUNDATION_BUILDING) == 2
				&& Bloodline.tails(Realm.CORE_FORMATION) == 3, "One tail at Qi Refining, one more each realm");
		helper.assertTrue(Bloodline.tails(Realm.SACRED_VESSEL) == 9 && Bloodline.tails(Realm.TRUE_IMMORTAL) == 9, "Nine at Sacred Vessel, no more after");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyRaceWearsItsBeastsLook(GameTestHelper helper) {
		for (Bloodline b : Bloodline.values()) {
			boolean beast = b != Bloodline.HUMAN;
			helper.assertTrue(b.isBeast() == beast && (b.head() != null) == beast && b.variants() == (beast ? 3 : 0),
					b.getId() + ": a beast's head and three colourings, or a human's own look");
			helper.assertTrue(Bloodline.byId(b.getId()) == b && BeastHead.byId(beast ? b.head().getId() : "ox") != null, b.getId() + " by its id");
		}
		helper.assertTrue(Bloodline.byId("snake") == Bloodline.LIZARD, "A bloodline kept as a snake, the lizard's first name, reads as a lizard");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDemonicPathIsNowDevilish(GameTestHelper helper) {
		Alignment.Faction faction = Alignment.factionOf(Alignment.DEVILISH_BELOW - 1);
		helper.assertTrue(faction == Alignment.Faction.DEVILISH && faction.getId().equals("devilish")
				&& ClothingStyle.of(faction) == ClothingStyle.DEVILISH && ClothingStyle.DEVILISH.getId().equals("devilish"),
				"The path below the neutral, and its red and black, are the devilish");
		helper.succeed();
	}
}
