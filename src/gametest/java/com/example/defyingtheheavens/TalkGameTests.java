package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Talking to cultivators: names, regard, gifts, insults, directions (TalkSystem, Regard, NpcNames). */
public class TalkGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void namesAreStableAndWellFormed(GameTestHelper helper) {
		UUID id = UUID.fromString("3f2a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90");
		helper.assertTrue(NpcNames.of(id).equals(NpcNames.of(id)), "The same cultivator always has the same name");
		for (int i = 0; i < 500; i++) {
			String name = NpcNames.of(new UUID(i * 7919L, i * 104729L));
			String[] parts = name.split(" ");
			helper.assertTrue(parts.length == 2, "A surname and a given name: " + name);
			helper.assertTrue(Character.isUpperCase(parts[0].charAt(0)) && Character.isUpperCase(parts[1].charAt(0)), "Both capitalised: " + name);
			helper.assertTrue(!parts[0].equalsIgnoreCase(parts[1]), "Never the surname twice: " + name);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void regardDriftsTowardZeroAndSaves(GameTestHelper helper) {
		Regard.Entry entry = new Regard.Entry();
		entry.value = 30;
		entry.day = 10;
		helper.assertTrue(entry.current(10) == 30, "No drift the same day");
		helper.assertTrue(entry.current(15) == 25, "A point a day toward zero");
		helper.assertTrue(entry.current(100) == 0, "Never past zero");
		entry.value = -12;
		entry.day = 100;
		helper.assertTrue(entry.current(104) == -8, "Resentment fades too");
		entry.greetDay = 104;
		String ginseng = "defying-the-heavens:ginseng";
		entry.recordGift(ginseng, 104);
		entry.recordGift(ginseng, 104);
		Regard.Entry loaded = Regard.Entry.load(entry.save());
		helper.assertTrue(loaded.value == -8 && loaded.day == 104 && loaded.greetDay == 104 && loaded.giftDay == 104
				&& loaded.giftsToday(ginseng, 104) == 2, "An entry survives saving");
		CompoundTag older = new CompoundTag(); // saved before gifts were counted per item
		older.putLong("GiftDay", 104);
		older.putString("GiftItem", ginseng);
		older.putInt("GiftCount", 1);
		helper.assertTrue(Regard.Entry.load(older).giftsToday(ginseng, 104) == 2, "An older save's last gift carries over");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void giftsAreCountedPerItem(GameTestHelper helper) {
		Regard.Entry entry = new Regard.Entry();
		String ginseng = "defying-the-heavens:ginseng", pill = "defying-the-heavens:foundation_pill";
		for (int i = 0; i < 3; i++) {
			entry.recordGift(ginseng, 7);
			entry.recordGift(pill, 7);
		}
		helper.assertTrue(entry.giftsToday(ginseng, 7) == 3 && entry.giftsToday(pill, 7) == 3, "Turning between two gifts wears both down");
		helper.assertTrue(entry.giftsToday(ginseng, 8) == 0, "A new day, and they're fresh again");
		entry.recordGift(ginseng, 8);
		helper.assertTrue(entry.giftsToday(ginseng, 8) == 1 && entry.giftsToday(pill, 8) == 0, "...yesterday's forgotten");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSectRemembersTodaysRobberyThroughASave(GameTestHelper helper) {
		Sect sect = new Sect(UUID.randomUUID());
		UUID thief = UUID.randomUUID();
		sect.robbedDay.put(thief, 12L);
		Sect loaded = Sect.load(sect.save());
		helper.assertTrue(Long.valueOf(12L).equals(loaded.robbedDay.get(thief)), "A restart doesn't let the same day's robbery count twice");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void regardBaselineAndLevels(GameTestHelper helper) {
		helper.assertTrue(Regard.baseline(Alignment.Faction.RIGHTEOUS, 120) == 10, "The righteous warm to the righteous");
		helper.assertTrue(Regard.baseline(Alignment.Faction.RIGHTEOUS, -20) == -10, "...and cool to a demonic lean");
		helper.assertTrue(Regard.baseline(Alignment.Faction.DEMONIC, -150) == 10, "The demonic warm to their own");
		helper.assertTrue(Regard.baseline(Alignment.Faction.NEUTRAL, 150) == 0, "The neutral care for no one's path");
		helper.assertTrue(Regard.pips(45) == 2 && Regard.levelId(45).equals("courteous"), "45 is two pips, Courteous");
		helper.assertTrue(Regard.pips(-30) == 0 && Regard.levelId(-30).equals("resentful"), "Below zero: Resentful, no pips");
		helper.assertTrue(Regard.levelId(Regard.HOSTILE).equals("hostile"), "At the hostile line they turn on you");
		helper.assertTrue(Regard.pips(89) == 4 && Regard.levelId(89).equals("trusted"), "89 is four pips, Trusted");
		helper.assertTrue(Regard.pips(Regard.SWORN) == 5 && Regard.levelId(Regard.SWORN).equals("sworn") && Regard.levelId(Regard.MAX).equals("sworn"),
				"Sworn from 90 to the top, not only at 100");
		helper.assertTrue(Regard.clamp(500) == Regard.MAX && Regard.clamp(-500) == Regard.MIN, "Regard stays within bounds");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void giftsPleaseTheWeakMoreAndRepeatsLess(GameTestHelper helper) {
		ItemStack old = GinsengItem.create(ModItems.GINSENG, 600);
		double worth = TalkSystem.worth(old);
		helper.assertTrue(worth == 15, "A 600-year ginseng is worth 15 (got " + worth + ")");
		helper.assertTrue(TalkSystem.worth(GinsengItem.create(ModItems.SPIRIT_GINSENG, 600)) == 30, "Spirit Ginseng counts double");
		helper.assertTrue(TalkSystem.worth(new ItemStack(Items.DIRT)) == 0, "Dirt is no gift");
		int toQiRefining = TalkSystem.giftRegard(worth, Realm.QI_REFINING.ordinal(), 0);
		int toNascentSoul = TalkSystem.giftRegard(worth, Realm.NASCENT_SOUL.ordinal(), 0);
		int again = TalkSystem.giftRegard(worth, Realm.QI_REFINING.ordinal(), 1);
		helper.assertTrue(toQiRefining == 15 && toNascentSoul == 6, "Worth 15: +15 to Qi Refining, +6 to Nascent Soul (got " + toQiRefining + ", "
				+ toNascentSoul + ")");
		helper.assertTrue(again < toQiRefining, "The same gift twice in a day counts for less");
		helper.assertTrue(TalkSystem.giftRegard(1000, 0, 0) == Regard.GIFT_MAX && TalkSystem.giftRegard(0.1, 5, 0) == 1, "A day's first gift of a kind gives 1 to 25");
		// The same again can come to nothing: a Low Spirit Stone (worth 1) after the first is soon worth nothing more that day.
		helper.assertTrue(TalkSystem.giftRegard(0.1, 5, 3) == 0, "A trifle given again is worth nothing more");
		int stones = 0;
		for (int repeats = 0; repeats < 60; repeats++) stones += TalkSystem.giftRegard(1, Realm.QI_REFINING.ordinal(), repeats);
		helper.assertTrue(stones <= 3, "Sixty Low Spirit Stones one by one no longer buy sixty regard (" + stones + ")");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void insultsAndDirections(GameTestHelper helper) {
		int stages = Stage.values().length;
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.DEMONIC, 0, 3 * stages) == 0, "Far weaker cultivators swallow an insult");
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.DEMONIC, stages, 2 * stages) == 0, "Exactly a realm below you, they swallow it too");
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.DEMONIC, stages + 1, 2 * stages) > 0, "Three stages below, they may still lash out");
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.DEMONIC, 10, 5) > TalkSystem.provokeChance(Alignment.Faction.RIGHTEOUS, 10, 5),
				"The demonic are quicker to fight than the righteous");
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.NEUTRAL, 10, 5) > TalkSystem.provokeChance(Alignment.Faction.NEUTRAL, 5, 5),
				"A stronger cultivator is quicker to fight");
		BlockPos o = BlockPos.ZERO;
		helper.assertTrue(TalkSystem.direction(o, new BlockPos(0, 0, -100)).equals("north"), "-z is north");
		helper.assertTrue(TalkSystem.direction(o, new BlockPos(100, 0, 0)).equals("east"), "+x is east");
		helper.assertTrue(TalkSystem.direction(o, new BlockPos(-100, 0, 100)).equals("south_west"), "-x +z is south-west");
		helper.succeed();
	}
}
