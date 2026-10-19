package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Who sees whom among the dead and the living, and what reaches the dead: the living world's mobs and NPCs don't see a Ghost
 * King's form or the dead (the dead's own kind do); a ghost drinks the Marrow Cleansing Elixir; an ascender, come up the Soul
 * River as one of the dead, fights off the Hungry Ghosts that go for it; and an NPC's hand reaches the dead only from Nascent
 * Soul (or with a Peach Wood Sword), its arrows never.
 */
public class GhostSightGameTests implements FabricGameTest {
	/**
	 * A survival player in earnest (a mock player answers that it is creative, and a creative ghost keeps no rule of the dead),
	 * at the test's corner; mortal. Remove it with {@link #leave}.
	 */
	private static ServerPlayer survivor(GameTestHelper helper) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-survivor")) {
			@Override
			public boolean isSpectator() { return false; }

			@Override
			public boolean isCreative() { return false; }
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		return p;
	}

	private static ServerPlayer cultivator(GameTestHelper helper, Realm realm, Stage stage) {
		ServerPlayer player = survivor(helper);
		CultivationManager.get(player).setState(realm, stage, 0);
		CultivationManager.refresh(player);
		return player;
	}

	private static void leave(ServerPlayer player) {
		GhostSystem.forget(player.getUUID());
		player.server.getPlayerList().remove(player);
	}

	/** A living cultivator NPC of {@code realm} Early, standing still. */
	private static CultivatorNpc npc(GameTestHelper helper, Realm realm, int x) {
		CultivatorNpc npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(x, 2, 3));
		npc.becomeRogue(PlayerCultivation.rank(realm, Stage.EARLY), 0, helper.getLevel().getRandom());
		return npc;
	}

	/** The ghost path at Nascent Soul Early (its own bar, GhostMethods): the Nine Nether Soul Sutra known, understood as far. */
	private static void nascentGhostPath(ServerPlayer player) {
		NetherworldState s = CultivationManager.get(player).netherworld();
		s.setMethod(GhostMethod.NINE_NETHER_SOUL_SUTRA, 1, 0);
		GhostMethods.setPath(s, PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY));
		CultivationManager.refresh(player);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theLivingWorldDoesntSeeAGhostKingsForm(GameTestHelper helper) {
		ServerPlayer subject = cultivator(helper, Realm.CORE_FORMATION, Stage.EARLY);
		try {
			nascentGhostPath(subject); // the ghost path above the heaven path: the true form a Ghost King's
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			CultivatorNpc heavenly = npc(helper, Realm.HEAVENLY_BEING, 2);
			HungryGhost hungry = helper.spawnWithNoFreeWill(ModEntities.HUNGRY_GHOST, new BlockPos(3, 2, 1));
			GhostCultivatorNpc ghost = helper.spawnWithNoFreeWill(ModEntities.GHOST_CULTIVATOR, new BlockPos(4, 2, 1));
			ghost.becomeGhost(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0, helper.getLevel().getRandom());
			helper.assertTrue(TwoPaths.inGhostForm(subject) && GhostSystem.seenAsDead(subject), "In a Ghost King's form");
			helper.assertTrue(!zombie.canAttack(subject) && !TargetingConditions.forCombat().test(zombie, subject), "No mob takes it for prey");
			helper.assertTrue(!heavenly.senses(subject) && !heavenly.isEnemy(subject), "Not even a Heavenly Being of the living sees it");
			helper.assertTrue(ModLang.TALK_UNSEEN.equals(TalkSystem.refusal(subject, heavenly)), "...nor answers it");
			helper.assertTrue(hungry.canAttack(subject) && TargetingConditions.forCombat().test(hungry, subject), "A Hungry Ghost sees it, as it sees the dead");
			helper.assertTrue(ghost.senses(subject) && ghost.canAttack(subject), "And the dead's own cultivators");
			CultivationManager.toggleAbility(subject, Ability.FORM_SHIFT.ordinal()); // the human form
			helper.assertTrue(zombie.canAttack(subject) && heavenly.senses(subject), "In its human form, seen by all again");
		} finally {
			leave(subject);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDeadAreHuntedByTheDeadAndSeenByTheLivingFromNascentSoul(GameTestHelper helper) {
		ServerPlayer dead = cultivator(helper, Realm.FOUNDATION_BUILDING, Stage.EARLY);
		try {
			GhostSystem.makeGhost(dead);
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
			HungryGhost hungry = helper.spawnWithNoFreeWill(ModEntities.HUNGRY_GHOST, new BlockPos(3, 2, 1));
			GhostCultivatorNpc ghost = helper.spawnWithNoFreeWill(ModEntities.GHOST_CULTIVATOR, new BlockPos(4, 2, 1));
			ghost.becomeGhost(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0, helper.getLevel().getRandom());
			helper.assertTrue(!zombie.canAttack(dead), "No living-world mob hunts the dead");
			helper.assertTrue(hungry.canAttack(dead) && TargetingConditions.forCombat().test(hungry, dead), "A Hungry Ghost does");
			helper.assertTrue(ghost.canAttack(dead), "A ghost cultivator can turn on one of the dead (and strike back at one)");
			CultivatorNpc foundation = npc(helper, Realm.FOUNDATION_BUILDING, 1), nascent = npc(helper, Realm.NASCENT_SOUL, 2);
			helper.assertTrue(!foundation.senses(dead) && nascent.senses(dead), "The living see the dead from Nascent Soul, as they see souls");
		} finally {
			leave(dead);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void aGhostTakesUpAndDrinksTheMarrowCleansingElixir(GameTestHelper helper) {
		ServerPlayer player = survivor(helper); // a mortal
		PlayerCultivation c = CultivationManager.get(player);
		c.setMortalState(MortalStage.PEAK, 0);
		GhostSystem.makeGhost(player);
		player.getInventory().clearContent();
		ItemStack elixir = PillItem.create(ModItems.MARROW_CLEANSING_ELIXIR, PillGrade.LOW, 1);
		ItemEntity handed = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), elixir.copy());
		ItemEntity bread = new ItemEntity(helper.getLevel(), player.getX(), player.getY(), player.getZ(), new ItemStack(Items.BREAD));
		handed.setNoPickUpDelay();
		bread.setNoPickUpDelay();
		helper.getLevel().addFreshEntity(handed);
		helper.getLevel().addFreshEntity(bread);
		helper.runAfterDelay(2, () -> {
			try {
				// What a player's tick does with whatever it walks into (a test's player isn't ticked): Player#touch, the dead's rules on it.
				java.lang.reflect.Method touch = net.minecraft.world.entity.player.Player.class.getDeclaredMethod("touch", net.minecraft.world.entity.Entity.class);
				touch.setAccessible(true);
				touch.invoke(player, handed);
				touch.invoke(player, bread);
				helper.assertTrue(player.getInventory().countItem(ModItems.MARROW_CLEANSING_ELIXIR) == 1, "The ghost takes up the elixir handed to it");
				helper.assertTrue(player.getInventory().countItem(Items.BREAD) == 0, "...and nothing else");
				player.setItemInHand(InteractionHand.MAIN_HAND, elixir.copy());
				helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS,
						"It may lift the elixir to drink");
				player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
				helper.assertTrue(UseItemCallback.EVENT.invoker().interact(player, helper.getLevel(), InteractionHand.OFF_HAND).getResult() == InteractionResult.FAIL,
						"Not bread");
				ItemStack drunk = player.getMainHandItem();
				drunk.getItem().finishUsingItem(drunk, helper.getLevel(), player);
				helper.assertTrue(!c.isMortal() && GhostSystem.isGhost(player) && GhostMethods.canLearn(c, GhostMethod.NETHERWORLD_BREATHING),
						"Drunk: its meridians open, in death; the ghost path is its to walk");
				helper.succeed();
			} catch (ReflectiveOperationException e) {
				helper.fail("Player#touch: " + e);
			} finally {
				handed.discard();
				bread.discard();
				leave(player);
			}
		});
	}

	// Its own batch: the room reaches past the empty template into the neighbours' ground, and a neighbour building there walled
	// the two apart (the Hungry Ghost saw the ascender at the first tick, never after).
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400, batch = "ghost_sight_ascender")
	public void anAscenderComeUpTheSoulRiverFightsOffAHungryGhost(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// A closed stone room in the test's own ground: a Hungry Ghost goes for the living first, but only those it sees, so the other
		// tests' players outside are nothing to it.
		for (int x = 0; x <= 6; x++) for (int y = 1; y <= 5; y++) for (int z = 0; z <= 6; z++) {
			boolean shell = x == 0 || x == 6 || y == 1 || y == 5 || z == 0 || z == 6;
			helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
		}
		// A living cultivator of Foundation Building, gone to ascend from the Nether: up the Soul River, one of the dead now.
		CultivatorNpc living = ModEntities.CULTIVATOR.create(level);
		living.becomeRogue(PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.MID), 0, level.getRandom());
		GhostCultivatorNpc ascender = GhostCultivatorNpc.shedBody(living, level);
		living.discard();
		BlockPos a = helper.absolutePos(new BlockPos(1, 2, 3)), b = helper.absolutePos(new BlockPos(5, 2, 3));
		ascender.moveTo(a.getX() + 0.5, a.getY(), a.getZ() + 0.5, -90, 0);
		level.addFreshEntity(ascender);
		HungryGhost hungry = ModEntities.HUNGRY_GHOST.create(level);
		hungry.moveTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 90, 0);
		level.addFreshEntity(hungry);
		boolean[] went = {false};
		helper.onEachTick(() -> went[0] |= hungry.getTarget() == ascender);
		helper.succeedWhen(() -> {
			helper.assertTrue(went[0], "The Hungry Ghost goes for the ascender");
			helper.assertTrue(!hungry.isAlive() || hungry.getHealth() < hungry.getMaxHealth(), "The ascender strikes back (" + ascender.getTarget() + ")");
			helper.assertTrue(ascender.isAlive(), "...and lives");
			ascender.discard();
			hungry.discard();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anNpcsHandReachesTheDeadOnlyFromNascentSoul(GameTestHelper helper) {
		HungryGhost hungry = helper.spawnWithNoFreeWill(ModEntities.HUNGRY_GHOST, new BlockPos(1, 2, 1));
		CultivatorNpc foundation = npc(helper, Realm.FOUNDATION_BUILDING, 1), nascent = npc(helper, Realm.NASCENT_SOUL, 2),
				heavenly = npc(helper, Realm.HEAVENLY_BEING, 3), armed = npc(helper, Realm.FOUNDATION_BUILDING, 4);
		armed.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.PEACH_WOOD_SWORD));
		var sources = helper.getLevel().damageSources();
		helper.assertTrue(!GhostSystem.reaches(sources.mobAttack(foundation)), "A Foundation Building NPC's blow passes through the dead");
		helper.assertTrue(GhostSystem.reaches(sources.mobAttack(nascent)) && GhostSystem.scaled(sources.mobAttack(nascent), 10) < 10,
				"A Nascent Soul's lands, as through a haze");
		helper.assertTrue(GhostSystem.scaled(sources.mobAttack(heavenly), 10) == 10, "A Heavenly Being's lands in full");
		helper.assertTrue(GhostSystem.reaches(sources.mobAttack(armed)), "A Peach Wood Sword reaches the dead in any hand");
		float full = hungry.getHealth();
		hungry.hurt(sources.mobAttack(foundation), 6);
		helper.assertTrue(hungry.getHealth() == full, "The Hungry Ghost takes nothing from it");
		Skeleton skeleton = helper.spawnWithNoFreeWill(EntityType.SKELETON, new BlockPos(5, 2, 1));
		Arrow arrow = new Arrow(helper.getLevel(), skeleton);
		helper.assertTrue(!GhostSystem.reaches(sources.arrow(arrow, nascent)) && !GhostSystem.reaches(sources.arrow(arrow, skeleton)),
				"Arrows of the living never reach the dead, whoever looses them");
		hungry.invulnerableTime = 0;
		hungry.hurt(sources.mobAttack(heavenly), 6);
		helper.assertTrue(hungry.getHealth() < full, "A Heavenly Being's blow lands on it");
		helper.succeed();
	}
}
