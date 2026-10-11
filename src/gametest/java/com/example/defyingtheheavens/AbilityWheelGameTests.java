package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public class AbilityWheelGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void theWheelKeepsEachAbilityOnceAndIsSaved(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			Ability expected = i < Ability.values().length ? Ability.values()[i] : null;
			helper.assertTrue(c.getWheelSlot(i) == expected, "A new cultivator's wheel holds the abilities in menu order");
		}

		// Qi Flight (slot 1) onto Inner Realm's slot (5): they trade places.
		c.setWheelSlot(5, Ability.QI_FLIGHT);
		helper.assertTrue(c.getWheelSlot(5) == Ability.QI_FLIGHT && c.getWheelSlot(1) == Ability.INNER_REALM,
				"Moving an ability onto a full slot swaps the two");
		c.setWheelSlot(7, Ability.QI_SENSE);
		helper.assertTrue(c.getWheelSlot(7) == Ability.QI_SENSE && c.getWheelSlot(2) == null, "Moving onto an empty slot leaves its old slot empty");
		c.setWheelSlot(0, null);
		helper.assertTrue(c.getWheelSlot(0) == null, "A slot can be emptied");
		int count = 0;
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			if (c.getWheelSlot(i) == Ability.QI_FLIGHT) count++;
		}
		helper.assertTrue(count == 1, "An ability is never on the wheel twice");

		PlayerCultivation loaded = PlayerCultivation.load(c.save());
		for (int i = 0; i < PlayerCultivation.WHEEL_SLOTS; i++) {
			helper.assertTrue(loaded.getWheelSlot(i) == c.getWheelSlot(i), "The wheel survives saving and loading (slot " + i + ")");
		}
		CompoundTag legacy = c.save();
		legacy.remove("AbilityWheel");
		helper.assertTrue(PlayerCultivation.load(legacy).getWheelSlot(0) == Ability.values()[0], "Saves from before the wheel get the default one");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyAwakenedAbilitiesGoOnTheWheel(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		PlayerCultivation c = CultivationManager.get(player);
		c.setWheelSlot(7, null);
		c.setWheelSlot(6, null);
		CultivationManager.setWheelSlot(player, 7, Ability.QI_FLIGHT.ordinal());
		helper.assertTrue(c.getWheelSlot(7) == null, "A mortal can't place an ability it hasn't awakened");
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		CultivationManager.setWheelSlot(player, 7, Ability.QI_FLIGHT.ordinal());
		helper.assertTrue(c.getWheelSlot(7) == Ability.QI_FLIGHT, "An awakened ability can be placed");
		CultivationManager.setWheelSlot(player, PlayerCultivation.WHEEL_SLOTS, Ability.QI_SENSE.ordinal());
		CultivationManager.setWheelSlot(player, 6, Ability.values().length);
		helper.assertTrue(c.getWheelSlot(6) == null, "Unknown slots and abilities are ignored");
		CultivationManager.setWheelSlot(player, 7, -1);
		helper.assertTrue(c.getWheelSlot(7) == null, "-1 empties a slot");
		helper.succeed();
	}
}
