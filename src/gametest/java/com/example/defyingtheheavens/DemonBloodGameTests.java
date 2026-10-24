package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * A demon race's blood (DemonBlood, DemonBloodItem; the user's ask, 2026-10-24): a human drinks it and is a half-blood (a demon core, the
 * Demon Path, the race's gifts at half strength, its look its own); too much, or blood two realms above its other paths, turns it, but
 * only drunk sneaking (warned else); a demon's own race's blood opens its realms as a method does the heaven path's; no other race's
 * blood takes once one has a race; never a mortal's, the dead's, or in another's body; an old record's blood awake as far as its path.
 */
public class DemonBloodGameTests implements FabricGameTest {
	/** A player of {@code race}, born (DemonPath#born), in survival, out of the player list. */
	private static ServerPlayer born(GameTestHelper helper, Bloodline race) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "blood"));
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

	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aHumanDrinksAndIsAHalfBlood(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer human = born(helper, Bloodline.HUMAN), fox = born(helper, Bloodline.HUMAN), dragon = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation h = CultivationManager.get(human);
			helper.assertTrue(h.isMortal() && !DemonBlood.drink(human, Bloodline.TIGER, Realm.QI_REFINING) && DemonPath.rank(h) == -1,
					"A mortal's sealed meridians take no blood");
			h.awaken(0);
			helper.assertTrue(DemonBlood.drink(human, Bloodline.TIGER, Realm.QI_REFINING), "Its meridians open, it drinks the tiger's blood");
			helper.assertTrue(DemonPath.isHalfBlood(h) && !DemonPath.isDemon(h) && DemonPath.race(h) == Bloodline.TIGER && DemonPath.rank(h) == 0,
					"...a half-blood of the tiger: a demon core, its Demon Path at Qi Refining Early");
			helper.assertTrue(Bloodlines.of(server, human.getUUID()) == Bloodline.HUMAN && h.demon().bloodline() == Bloodline.HUMAN,
					"...its look and its blood's record a human's still");
			helper.assertTrue(near(h.demon().purity(), DemonBlood.PURITY_LEVEL) && h.demon().bloodRealm() == Realm.QI_REFINING.ordinal(),
					"...its blood that pure (" + h.demon().purity() + "), awake to Qi Refining");
			helper.assertTrue(DemonPath.giftStrength(h) == DemonPath.HALF_BLOOD_GIFTS, "...its race's gifts at half strength");
			helper.assertTrue(!DemonBlood.drink(human, Bloodline.FOX, Realm.QI_REFINING) && near(h.demon().purity(), DemonBlood.PURITY_LEVEL),
					"Another race's blood is rejected, and changes nothing");
			helper.assertTrue(DemonBlood.drink(human, Bloodline.TIGER, Realm.QI_REFINING) && near(h.demon().purity(), DemonBlood.PURITY_LEVEL + DemonBlood.PURITY_HELD),
					"Blood it already holds only thickens it (" + h.demon().purity() + ")");

			// A fox half-blood's charm and a dragon half-blood's health, half a demon's.
			PlayerCultivation f = CultivationManager.get(fox), d = CultivationManager.get(dragon);
			f.awaken(0);
			d.awaken(0);
			helper.assertTrue(DemonBlood.drink(fox, Bloodline.FOX, Realm.QI_REFINING) && RaceGifts.regardShift(f) == Math.round(RaceGifts.CHARM * 0.5),
					"A fox half-blood's charm is half a fox's (" + RaceGifts.regardShift(f) + ")");
			helper.assertTrue(DemonBlood.drink(dragon, Bloodline.DRAGON, Realm.QI_REFINING) && near(RaceGifts.healthFactor(d), 1 + (RaceGifts.DRAGON_HEALTH - 1) * 0.5),
					"A dragon half-blood's health half a dragon's more (" + RaceGifts.healthFactor(d) + ")");
			helper.succeed();
		} finally {
			forget(server, human, fox, dragon);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tooMuchOrTooStrongBloodTurnsThem(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer strong = born(helper, Bloodline.HUMAN), much = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation s = CultivationManager.get(strong), m = CultivationManager.get(much);
			s.awaken(0);
			m.awaken(0);
			DemonBlood.Outcome warned = DemonBlood.judge(s, Bloodline.TIGER, Realm.CORE_FORMATION, false, false, false);
			helper.assertTrue(warned.verdict() == DemonBlood.Verdict.WARN && warned.turns(), "Core Formation blood, two realms over its Qi Refining: a warning");
			helper.assertTrue(!DemonBlood.drink(strong, Bloodline.TIGER, Realm.CORE_FORMATION) && DemonPath.rank(s) == -1 && Bloodlines.of(server, strong.getUUID()) == Bloodline.HUMAN,
					"...not drunk, it unsneaking: nothing changes");
			strong.setShiftKeyDown(true);
			helper.assertTrue(DemonBlood.drink(strong, Bloodline.TIGER, Realm.CORE_FORMATION), "Drunk sneaking, it means to");
			helper.assertTrue(Bloodlines.of(server, strong.getUUID()) == Bloodline.TIGER && DemonPath.isDemon(s) && !DemonPath.isHalfBlood(s) && s.demon().halfBlood() == null,
					"...and it is a tiger now, wholly");
			helper.assertTrue(DemonPath.rank(s) == 0 && s.demon().bloodRealm() == Realm.CORE_FORMATION.ordinal() && DemonPath.giftStrength(s) == 1.0,
					"...its Demon Path as it was, its blood awake to Core Formation, its gifts whole");
			helper.assertTrue(!DemonBlood.drink(strong, Bloodline.FOX, Realm.QI_REFINING), "A tiger takes no fox's blood");

			helper.assertTrue(DemonBlood.drink(much, Bloodline.LIZARD, Realm.QI_REFINING), "A half-blood of the lizard");
			m.demon().setPurity(0.95);
			helper.assertTrue(DemonBlood.judge(m, Bloodline.LIZARD, Realm.QI_REFINING, false, false, false).verdict() == DemonBlood.Verdict.WARN,
					"Its blood nearly whole: one more draught warns");
			much.setShiftKeyDown(true);
			helper.assertTrue(DemonBlood.drink(much, Bloodline.LIZARD, Realm.QI_REFINING) && Bloodlines.of(server, much.getUUID()) == Bloodline.LIZARD
					&& DemonPath.isDemon(m), "...and drunk sneaking, too much of it turns it a lizard");
			helper.succeed();
		} finally {
			forget(server, strong, much);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aDemonsBloodOpensItsRealms(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = born(helper, Bloodline.TIGER);
		try {
			PlayerCultivation c = CultivationManager.get(tiger);
			helper.assertTrue(c.demon().bloodRealm() == Realm.QI_REFINING.ordinal(), "A tiger is born with its blood awake to Qi Refining");
			DemonPath.addCultivation(c, 1.0e9);
			helper.assertTrue(DemonPath.atBottleneck(c) && DemonPath.heldByBlood(c) && !DemonPath.canBreakthrough(c),
					"At Qi Refining's Grand Perfection, Foundation Building is shut to it");
			helper.assertTrue(!DemonBlood.drink(tiger, Bloodline.TIGER, Realm.QI_REFINING), "Blood it already holds is refused, not wasted");
			helper.assertTrue(!DemonBlood.drink(tiger, Bloodline.FOX, Realm.FOUNDATION_BUILDING), "...and another race's");
			helper.assertTrue(DemonBlood.drink(tiger, Bloodline.TIGER, Realm.FOUNDATION_BUILDING) && c.demon().bloodRealm() == Realm.FOUNDATION_BUILDING.ordinal()
					&& !DemonPath.heldByBlood(c) && DemonPath.canBreakthrough(c), "Foundation Building tiger blood drunk: the way open");
			helper.assertTrue(near(c.demon().purity(), 0), "A demon's blood has no purity to grow");
			helper.succeed();
		} finally {
			forget(server, tiger);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadAndTheElsewhereDrinkNothing(GameTestHelper helper) {
		ServerPlayer human = born(helper, Bloodline.HUMAN);
		try {
			PlayerCultivation c = CultivationManager.get(human);
			c.awaken(0);
			helper.assertTrue(DemonBlood.judge(c, Bloodline.TIGER, Realm.QI_REFINING, false, true, false).verdict() == DemonBlood.Verdict.REFUSE, "The dead drink no blood");
			helper.assertTrue(DemonBlood.judge(c, Bloodline.TIGER, Realm.QI_REFINING, false, false, true).verdict() == DemonBlood.Verdict.REFUSE, "...nor one in another's body");
			helper.assertTrue(DemonBlood.judge(c, null, Realm.QI_REFINING, false, false, false).verdict() == DemonBlood.Verdict.REFUSE, "...nor blood of no race");
			helper.succeed();
		} finally {
			forget(helper.getLevel().getServer(), human);
		}
	}

	/** An old record (before blood) has its blood awake as far as its Demon Path came; the blood's own stack keeps its race and realm. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void oldRecordsAndTheBloodsOwnStack(GameTestHelper helper) {
		CompoundTag old = new CompoundTag();
		old.putInt("Rank", PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID));
		old.putString("Bloodline", Bloodline.FOX.getId());
		DemonState s = new DemonState();
		s.load(old);
		helper.assertTrue(s.bloodRealm() == Realm.FOUNDATION_BUILDING.ordinal() && s.halfBlood() == null, "An old fox's blood awake to Foundation Building, where it stands");
		DemonState again = new DemonState();
		s.setHalfBlood(Bloodline.TIGER);
		s.setPurity(0.4);
		again.load(s.save());
		helper.assertTrue(again.halfBlood() == Bloodline.TIGER && near(again.purity(), 0.4) && again.bloodRealm() == s.bloodRealm(), "...and what it drank saved and loaded");
		ItemStack blood = DemonBloodItem.of(Bloodline.DRAGON, Realm.NASCENT_SOUL);
		helper.assertTrue(DemonBloodItem.race(blood) == Bloodline.DRAGON && DemonBloodItem.realm(blood) == Realm.NASCENT_SOUL && DemonBloodItem.look(blood) == 3,
				"Nascent Soul dragon blood: its race, realm and look");
		helper.assertTrue(DemonBloodItem.race(new ItemStack(ModItems.DEMON_BLOOD)) == null, "A bare stack's blood is of no race");
		helper.assertTrue(DemonBlood.kin(BeastKind.BLOODFANG_TIGER) == Bloodline.TIGER && DemonBlood.kin(BeastKind.FLOOD_DRAGON) == Bloodline.DRAGON
				&& DemonBlood.kin(BeastKind.JADE_VIPER) == Bloodline.LIZARD && DemonBlood.kin(BeastKind.EMBER_FOX) == Bloodline.FOX, "Its kin beasts leave a race's blood");
		helper.succeed();
	}
}
