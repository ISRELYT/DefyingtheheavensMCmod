package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The dead (phase 3, see GhostSystem): what a ghost is saved as, what can touch one, dispersing instead of dying, the yin
 * taking the living where they stand, and what a ghost leaves behind when it is born again. The test server has no
 * Netherworld, so the Gate's respawn and the Wheel's turning are checked in game instead.
 */
public class NetherworldDeadGameTests implements FabricGameTest {
	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		CultivationManager.get(player).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(player);
		player.setHealth(player.getMaxHealth());
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadAreSavedThroughACrash(GameTestHelper helper) {
		PlayerCultivation c = new PlayerCultivation();
		NetherworldState s = c.netherworld();
		s.setGhost(true);
		s.setBoundBelow(true);
		s.setReforming(true);
		s.setMemoryCleansed(true);
		s.setDeathPlace("minecraft:overworld", new BlockPos(12, 64, -30));
		CompoundTag respawn = new CompoundTag();
		respawn.putBoolean("Has", true);
		respawn.putString("Dimension", "minecraft:overworld");
		respawn.putInt("X", 5);
		s.setSavedRespawn(respawn);
		NetherworldState loaded = PlayerCultivation.load(c.save()).netherworld();
		helper.assertTrue(loaded.ghost() && loaded.boundBelow() && loaded.reforming() && loaded.memoryCleansed(), "Ghost, bound below, reforming, the soup");
		helper.assertTrue(loaded.hasDeathPlace() && loaded.deathDimension().equals("minecraft:overworld") && loaded.deathPos().equals(new BlockPos(12, 64, -30)),
				"Where they died");
		helper.assertTrue(loaded.savedRespawn() != null && loaded.savedRespawn().getInt("X") == 5, "The respawn point set aside");
		helper.assertTrue(helper.getLevel().getGameRules().getBoolean(ModGameRules.NETHERWORLD_AFTER_DEATH), "The dead go below by default");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyTheDeadAndThoseWhoSeeThemTouchAGhost(GameTestHelper helper) {
		ServerPlayer ghost = cultivator(helper, Realm.NASCENT_SOUL);
		float living = ghost.getMaxHealth();
		GhostSystem.makeGhost(ghost);
		try {
			helper.assertTrue(GhostSystem.isGhost(ghost), "A ghost");
			helper.assertTrue(Math.abs(ghost.getMaxHealth() - living * GhostSystem.HEALTH_SHARE) < 0.01, "Half its living hearts ("
					+ ghost.getMaxHealth() + " of " + living + ")");
			// The rule itself (the test's mock players count as creative, and a creative ghost has none of the rules).
			var sources = helper.getLevel().damageSources();
			helper.assertTrue(!GhostSystem.reaches(sources.fall()) && !GhostSystem.reaches(sources.lava()) && !GhostSystem.reaches(sources.drown())
					&& !GhostSystem.reaches(sources.inWall()) && !GhostSystem.reaches(sources.freeze()), "Falls, lava, water, walls and frost pass through it");
			Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
			helper.assertTrue(!GhostSystem.reaches(sources.mobAttack(zombie)), "...and an ordinary mob's blow");
			helper.assertTrue(GhostSystem.reaches(ModDamageTypes.ghostly(helper.getLevel(), null)), "The dead's harm reaches it");
			helper.assertTrue(GhostSystem.reaches(sources.genericKill()), "...as does what reaches anything");
			ServerPlayer mortal = helper.makeMockServerPlayerInLevel();
			helper.assertTrue(!GhostSystem.reaches(sources.playerAttack(mortal)), "A mortal's fist goes through it");
			ServerPlayer seer = cultivator(helper, Realm.HEAVENLY_BEING);
			float share = SoulProjection.harmFactor(seer);
			helper.assertTrue(share > 0 && GhostSystem.reaches(sources.playerAttack(seer))
					&& Math.abs(GhostSystem.scaled(sources.playerAttack(seer), 10) - 10 * share) < 1.0e-4,
					"A Heavenly Being's blow lands, by how clearly they see it");
			helper.assertTrue(!YinCorruption.affects(ghost) && GhostQi.poolMax(CultivationManager.get(ghost)) == GhostQi.BASE_POOL,
					"No yin to fill a ghost; a ghostly qi pool of its own");
			helper.assertTrue(!Incorporeal.is(ghost) && GhostSystem.allowDamage(ghost, sources.fall()),
					"A creative ghost keeps the look, not the rules (vanilla's own then apply)");
		} finally {
			GhostSystem.forget(ghost.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGhostDispersesAndReformsInsteadOfDying(GameTestHelper helper) {
		ServerPlayer ghost = cultivator(helper, Realm.CORE_FORMATION);
		GhostSystem.makeGhost(ghost);
		NetherworldState s = CultivationManager.get(ghost).netherworld();
		s.setGhostQi(20);
		try {
			helper.assertTrue(!GhostSystem.allowDeath(ghost), "No death for the dead");
			helper.assertTrue(ghost.isInvisible() && ghost.isInvulnerable() && s.reforming() && ghost.getHealth() > 0,
					"It scatters: gone from sight, untouchable, reforming");
			helper.assertTrue(!GhostSystem.allowDamage(ghost, ModDamageTypes.ghostly(helper.getLevel(), null)), "Nothing is left to strike meanwhile");
			GhostSystem.reform(ghost);
			helper.assertTrue(!ghost.isInvisible() && !ghost.isInvulnerable() && !s.reforming(), "Back together");
			helper.assertTrue(Math.abs(ghost.getHealth() - ghost.getMaxHealth() / 2) < 0.01 && Math.abs(s.ghostQi() - 10) < 1.0e-9,
					"With half its health and half its ghostly qi");
		} finally {
			GhostSystem.forget(ghost.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theYinTakesTheLivingWhereTheyStandWithEverythingTheyCarry(GameTestHelper helper) {
		ServerPlayer visitor = cultivator(helper, Realm.NASCENT_SOUL);
		visitor.getInventory().add(new ItemStack(Items.DIAMOND, 5));
		BlockPos where = visitor.blockPosition();
		NetherworldState s = CultivationManager.get(visitor).netherworld();
		s.setYinCorruption(1.0);
		try {
			GhostSystem.overtakenByYin(visitor);
			helper.assertTrue(GhostSystem.isGhost(visitor), "Full of yin, they walk on as one of the dead");
			helper.assertTrue(visitor.isAlive() && visitor.blockPosition().equals(where), "...where they stood, no death");
			helper.assertTrue(visitor.getInventory().countItem(Items.DIAMOND) == 5, "...with everything they carried");
			helper.assertTrue(s.yinCorruption() == 0 && s.hasDeathPlace() && s.deathPos().equals(where), "The yin spent; where it took them remembered");
		} finally {
			GhostSystem.forget(visitor.getUUID());
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bornAgainAGhostLeavesWhatItCarriedAtTheWheel(GameTestHelper helper) {
		ServerPlayer visitor = cultivator(helper, Realm.NASCENT_SOUL);
		visitor.getInventory().add(new ItemStack(Items.DIAMOND, 5));
		visitor.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		ItemStack cursed = new ItemStack(Items.GOLDEN_SWORD);
		cursed.enchant(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE, 1);
		visitor.getInventory().add(cursed);
		RingContainer.of(visitor).setItem(0, new ItemStack(ModItems.RING_OF_POWER));
		CultivationManager.get(visitor).netherworld().setYinCorruption(1.0);
		net.minecraft.world.level.GameRules.BooleanValue keep = helper.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY);
		boolean kept = keep.get();
		try {
			keep.set(false, helper.getLevel().getServer());
			GhostSystem.overtakenByYin(visitor);
			GhostSystem.leaveBelongings(visitor);
			helper.assertTrue(visitor.getInventory().isEmpty() && RingContainer.of(visitor).isEmpty(), "Nothing it carried goes back with it");
			java.util.List<net.minecraft.world.entity.item.ItemEntity> left = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					visitor.getBoundingBox().inflate(4));
			java.util.function.Predicate<net.minecraft.world.item.Item> lying = item -> left.stream().anyMatch(e -> e.getItem().is(item));
			helper.assertTrue(left.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 5
					&& lying.test(Items.IRON_HELMET) && lying.test(ModItems.RING_OF_POWER), "It all lies where the ghost stood: the diamonds, the helmet, the ring");
			helper.assertTrue(!lying.test(Items.GOLDEN_SWORD), "What bore the Curse of Vanishing is gone");
			left.forEach(net.minecraft.world.entity.Entity::discard);
			// With keepInventory, as at a death, everything is kept.
			ServerPlayer keeper = cultivator(helper, Realm.NASCENT_SOUL);
			keeper.getInventory().add(new ItemStack(Items.EMERALD, 3));
			keep.set(true, helper.getLevel().getServer());
			GhostSystem.makeGhost(keeper);
			GhostSystem.leaveBelongings(keeper);
			helper.assertTrue(keeper.getInventory().countItem(Items.EMERALD) == 3, "With keepInventory it is all kept");
			GhostSystem.forget(keeper.getUUID());
		} finally {
			keep.set(kept, helper.getLevel().getServer());
			GhostSystem.forget(visitor.getUUID());
		}
		helper.succeed();
	}
}
