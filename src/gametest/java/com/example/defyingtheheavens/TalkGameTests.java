package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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
		entry.giftItem = "defying-the-heavens:ginseng";
		entry.giftCount = 2;
		Regard.Entry loaded = Regard.Entry.load(entry.save());
		helper.assertTrue(loaded.value == -8 && loaded.day == 104 && loaded.greetDay == 104 && loaded.giftItem.equals(entry.giftItem)
				&& loaded.giftCount == 2, "An entry survives saving");
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
		helper.assertTrue(TalkSystem.giftRegard(1000, 0, 0) == Regard.GIFT_MAX && TalkSystem.giftRegard(0.1, 5, 3) == 1, "Gifts give 1 to 25");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void insultsAndDirections(GameTestHelper helper) {
		int stages = Stage.values().length;
		helper.assertTrue(TalkSystem.provokeChance(Alignment.Faction.DEMONIC, 0, 3 * stages) == 0, "Far weaker cultivators swallow an insult");
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
