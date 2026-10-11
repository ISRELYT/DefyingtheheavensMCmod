package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public class SwordFlightGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aFoundationBuilderRidesASwordInHand(GameTestHelper helper) {
		// The test helper's mock player always counts as creative, where vanilla keeps flight; a fake player is in survival.
		ServerPlayer player = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "sword-rider"));
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.QI_REFINING, Stage.GRAND_PERFECTION, 0);
		c.setQi(c.maxQi());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		QiFlight.tick(player, c);
		helper.assertTrue(!Ability.SWORD_FLIGHT.isUnlocked(c) && !player.getAbilities().mayfly, "Qi Refining can't ride a sword");

		c.setState(Realm.FOUNDATION_BUILDING, Stage.EARLY, 0);
		c.setQi(c.maxQi());
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		QiFlight.tick(player, c);
		helper.assertTrue(!player.getAbilities().mayfly, "Foundation Building can't fly without a sword in hand");

		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		player.setItemInHand(InteractionHand.MAIN_HAND, sword);
		QiFlight.tick(player, c);
		helper.assertTrue(SwordFlight.canRide(player, c), "With a sword in hand it can ride");
		helper.assertTrue(player.getAbilities().mayfly, "With a sword in hand it may fly");
		helper.assertTrue(player.getAbilities().getFlyingSpeed() == SwordFlight.SWORD_SPEED,
				"At the sword's speed (" + player.getAbilities().getFlyingSpeed() + ")");
		player.getAbilities().flying = true;
		double cost = 0;
		for (int i = 0; i < 200; i++) cost = QiFlight.tick(player, c);
		helper.assertTrue(cost == Realm.FOUNDATION_BUILDING.getSwordFlightCost() && SwordFlight.isRiding(player), "Riding costs the sword's price");
		helper.assertTrue(sword.getDamageValue() == 0, "The sword takes no wear");

		c.setAbilityEnabled(Ability.SWORD_FLIGHT, false);
		QiFlight.tick(player, c);
		helper.assertTrue(!player.getAbilities().flying && !SwordFlight.isRiding(player), "Switched off, the ride ends");
		c.setAbilityEnabled(Ability.SWORD_FLIGHT, true);

		// Core Formation: only one flight at a time. The newly formed core's Qi Flight comes on and Sword Flight goes off.
		c.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		c.setQi(c.maxQi());
		QiFlight.tick(player, c);
		helper.assertTrue(c.isAbilityEnabled(Ability.QI_FLIGHT) && !c.isAbilityEnabled(Ability.SWORD_FLIGHT),
				"When the golden core forms, Qi Flight comes on and Sword Flight goes off");
		player.getAbilities().flying = true;
		cost = QiFlight.tick(player, c);
		helper.assertTrue(cost == Realm.CORE_FORMATION.getQiFlightCost() && player.getAbilities().getFlyingSpeed() == SwordFlight.QI_FLIGHT_SPEED
				&& !SwordFlight.isRiding(player), "With Qi Flight on, a sword in hand is only held: it flies on qi");
		CultivationManager.toggleAbility(player, Ability.SWORD_FLIGHT.ordinal());
		helper.assertTrue(c.isAbilityEnabled(Ability.SWORD_FLIGHT) && !c.isAbilityEnabled(Ability.QI_FLIGHT),
				"Switching Sword Flight on switches Qi Flight off");
		cost = QiFlight.tick(player, c);
		helper.assertTrue(cost == Realm.CORE_FORMATION.getSwordFlightCost() && cost < Realm.CORE_FORMATION.getQiFlightCost()
				&& SwordFlight.isRiding(player), "Then the sword is ridden, for less qi than Qi Flight");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		QiFlight.tick(player, c);
		helper.assertTrue(!player.getAbilities().flying && !player.getAbilities().mayfly, "Putting the sword away mid-air drops the rider");
		CultivationManager.toggleAbility(player, Ability.QI_FLIGHT.ordinal());
		helper.assertTrue(c.isAbilityEnabled(Ability.QI_FLIGHT) && !c.isAbilityEnabled(Ability.SWORD_FLIGHT),
				"Switching Qi Flight on switches Sword Flight off");

		// A save from before the rule, with both on: Qi Flight keeps it.
		ServerPlayer old = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "old-save"));
		PlayerCultivation oldC = CultivationManager.get(old);
		oldC.setState(Realm.CORE_FORMATION, Stage.EARLY, 0);
		helper.assertTrue(oldC.isAbilityEnabled(Ability.QI_FLIGHT) && oldC.isAbilityEnabled(Ability.SWORD_FLIGHT), "Both start on by default");
		QiFlight.tick(old, oldC);
		helper.assertTrue(oldC.isAbilityEnabled(Ability.QI_FLIGHT) && !oldC.isAbilityEnabled(Ability.SWORD_FLIGHT),
				"Both on at Core Formation: Qi Flight stays on, Sword Flight goes off");

		// The sword's speed is the one it had on awakening, at every realm: Qi Flight stays the fast way to fly.
		helper.assertTrue(SwordFlight.SWORD_SPEED == SwordFlight.QI_FLIGHT_SPEED * 0.75f, "A sword flies at 75% of Qi Flight's speed");
		CultivationManager.toggleAbility(player, Ability.SWORD_FLIGHT.ordinal());
		// Highest first: the core is already formed, so no realm here newly forms it (which would switch the sword off).
		for (Realm realm : new Realm[] {Realm.NASCENT_SOUL, Realm.HEAVENLY_BEING, Realm.FOUR_AXIS, Realm.FOUNDATION_BUILDING}) {
			c.setState(realm, Stage.GRAND_PERFECTION, 0);
			c.setQi(c.maxQi());
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			QiFlight.tick(player, c);
			helper.assertTrue(player.getAbilities().getFlyingSpeed() == SwordFlight.SWORD_SPEED, "The sword is no faster at " + realm.getId());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void npcCultivatorsRideTheirSwords(GameTestHelper helper) {
		CultivatorNpc builder = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(1, 2, 1));
		builder.setRank(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY));
		builder.cultivation().setQi(builder.cultivation().maxQi());
		helper.assertTrue(!builder.canFlySustainably(), "A Foundation Builder without a sword can't fly");
		builder.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		helper.assertTrue(builder.canFlySustainably(), "With one it can");
		builder.setQiFlying(true);
		helper.assertTrue(builder.isQiFlying() && builder.isSwordFlying(), "It rides its sword");

		CultivatorNpc core = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 1));
		core.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
		core.cultivation().setQi(core.cultivation().maxQi());
		core.setQiFlying(true);
		helper.assertTrue(core.isQiFlying() && !core.isSwordFlying(), "Without a sword a Core Formation cultivator flies on qi");
		core.setQiFlying(false);
		core.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		core.setQiFlying(true);
		helper.assertTrue(core.isSwordFlying(), "With a sword and no hurry it rides the sword, the cheaper way");

		CultivatorNpc strong = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(5, 2, 1));
		strong.setRank(PlayerCultivation.rank(Realm.HEAVENLY_BEING, Stage.GRAND_PERFECTION));
		strong.cultivation().setQi(strong.cultivation().maxQi());
		strong.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		strong.setQiFlying(true);
		helper.assertTrue(strong.isSwordFlying() && strong.getFlyingSpeed() == builder.getFlyingSpeed(),
				"A Heavenly Being's sword is no faster than a Foundation Builder's");
		helper.succeed();
	}
}
