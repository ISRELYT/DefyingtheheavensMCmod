package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Natal treasures (the user's "Build as designed", 2026-11-13; NatalTreasures): a blade one forged, bound to one's core from Core Formation at
 * the forge; its might growing with its owner's realm; unbound only at the cost of a soul wound, as when it breaks; called back to hand from
 * the ground, a chest or another's pack; raised a grade at the forge with the next grade's materials.
 */
public class NatalTreasureGameTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void aBladeYouForgedIsBoundFromCoreFormation(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "natal-binder"), q = survivor(helper, made, "natal-other");
			at(p, Realm.FOUNDATION_BUILDING);
			ItemStack blade = forgedBy(p);
			helper.assertTrue(NatalTreasures.atForge(p, blade) == InteractionResult.CONSUME && !NatalTreasures.isNatal(blade),
					"At Foundation Building no treasure binds to one's core");
			at(p, Realm.CORE_FORMATION);
			helper.assertTrue(NatalTreasures.atForge(p, forgedBy(q)) == InteractionResult.CONSUME, "Another's forging won't bind to one's core");
			helper.assertTrue(NatalTreasures.atForge(p, Artifacts.create(ArtifactKind.SABER, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null))
					== InteractionResult.CONSUME, "Nor a blade of no known forger");
			helper.assertTrue(NatalTreasures.atForge(p, blade) == InteractionResult.SUCCESS && NatalTreasures.isNatal(blade)
					&& NatalTreasures.idOf(blade).equals(CultivationManager.own(p).natalTreasure()) && p.getUUID().equals(NatalTreasures.ownerOf(blade)),
					"At Core Formation one's own forging is bound: its id on it and on one's record");
			NatalTreasures.held(blade, p);
			helper.assertTrue(Math.abs(NatalTreasures.might(blade) - NatalTreasures.PER_REALM) < 1.0e-9, "In its owner's hand at Core Formation: a tenth to every blow");
			at(p, Realm.NASCENT_SOUL);
			NatalTreasures.held(blade, p);
			helper.assertTrue(Math.abs(NatalTreasures.might(blade) - NatalTreasures.PER_REALM * 2) < 1.0e-9, "...at Nascent Soul, two tenths");
			boolean natalModifier = false;
			for (AttributeModifier modifier : blade.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
				if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL && Math.abs(modifier.getAmount() - NatalTreasures.PER_REALM * 2) < 1.0e-9) {
					natalModifier = true;
				}
			}
			helper.assertTrue(natalModifier, "...on all its owner's blows (its attack modifier)");
			NatalTreasures.held(blade, q);
			helper.assertTrue(NatalTreasures.might(blade) == 0 && NatalTreasures.isNatal(blade), "In another's pack it is a plain blade's might, still bound");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void unbindingAsksTwiceAndWoundsTheSoul(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "natal-unbinder");
			at(p, Realm.CORE_FORMATION);
			ItemStack blade = forgedBy(p);
			NatalTreasures.atForge(p, blade);
			PlayerCultivation c = CultivationManager.own(p);
			double whole = c.maxQi();
			helper.assertTrue(NatalTreasures.atForge(p, blade) == InteractionResult.CONSUME && c.natalTreasure() != null, "Asked once: still bound");
			helper.assertTrue(NatalTreasures.atForge(p, blade) == InteractionResult.SUCCESS && c.natalTreasure() == null && !NatalTreasures.isNatal(blade),
					"Asked again: unbound, a plain blade");
			helper.assertTrue(p.hasEffect(ModEffects.SOUL_WOUND) && c.soulWoundUntil() > helper.getLevel().getGameTime() && c.soulWounded(),
					"...and the soul wounded");
			helper.assertTrue(Math.abs(c.maxQi() - whole * NatalTreasures.WOUNDED_POOL) < 1.0e-6, "Its qi pool half (" + c.maxQi() + " of " + whole + ")");
			p.removeAllEffects(); // milk
			NatalTreasures.second(p);
			helper.assertTrue(p.hasEffect(ModEffects.SOUL_WOUND) && c.soulWounded(), "Milk takes the effect, not the wound: it is back within the second");
			c.setSoulWoundUntil(1); // its time up
			NatalTreasures.second(p);
			helper.assertTrue(!p.hasEffect(ModEffects.SOUL_WOUND) && !c.soulWounded() && c.soulWoundUntil() == 0 && Math.abs(c.maxQi() - whole) < 1.0e-6,
					"Healed: the whole pool again");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void natalRecallCallsItFromTheGroundAChestAndAnothersPack(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		BlockPos chestAt = new BlockPos(5, 2, 5);
		try {
			ServerPlayer p = survivor(helper, made, "natal-caller"), q = survivor(helper, made, "natal-keeper");
			at(p, Realm.CORE_FORMATION);
			ItemStack blade = forgedBy(p);
			NatalTreasures.atForge(p, blade);
			UUID id = NatalTreasures.idOf(blade);
			PlayerCultivation c = CultivationManager.get(p);
			c.setQi(c.maxQi());
			helper.assertTrue(Ability.NATAL_RECALL.isUnlocked(c), "Bound, Natal Recall is open");

			ItemEntity lying = new ItemEntity(level, p.getX() + 6, p.getY(), p.getZ() + 6, blade.copy());
			level.addFreshEntity(lying);
			NatalTreasures.recall(p);
			helper.assertTrue(id.equals(NatalTreasures.idOf(p.getMainHandItem())) && lying.isRemoved(), "Off the ground to its owner's hand");
			helper.assertTrue(Math.abs(c.getQi() - c.maxQi() * (1 - NatalTreasures.RECALL_QI)) < 1.0e-6, "...for a tenth of the qi pool");

			helper.setBlock(chestAt, Blocks.CHEST);
			ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(chestAt));
			chest.setItem(4, p.getMainHandItem().copy());
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
			NatalTreasures.forget(p.getUUID()); // its wait over
			NatalTreasures.recall(p);
			helper.assertTrue(id.equals(NatalTreasures.idOf(p.getMainHandItem())) && chest.getItem(4).isEmpty() && p.getInventory().countItem(Items.STICK) == 1,
					"Out of a chest in loaded land to its owner's hand, the stick held put away");

			q.getInventory().setItem(10, p.getMainHandItem().copy());
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			NatalTreasures.forget(p.getUUID());
			NatalTreasures.recall(p);
			helper.assertTrue(id.equals(NatalTreasures.idOf(p.getMainHandItem())) && q.getInventory().getItem(10).isEmpty(), "Out of another's pack");

			ItemStack gone = p.getMainHandItem();
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			NatalTreasures.forget(p.getUUID());
			double qi = c.getQi();
			NatalTreasures.recall(p);
			helper.assertTrue(p.getMainHandItem().isEmpty() && c.getQi() == qi, "Nowhere in this world: nothing comes, nothing spent (" + gone.getHoverName().getString() + ")");
			helper.succeed();
		} finally {
			helper.setBlock(chestAt, Blocks.AIR);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNatalTreasureThatBreaksWoundsItsOwner(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "natal-breaker");
			at(p, Realm.CORE_FORMATION);
			ItemStack blade = forgedBy(p);
			NatalTreasures.atForge(p, blade);
			blade.setDamageValue(blade.getMaxDamage() - 1);
			p.setItemInHand(InteractionHand.MAIN_HAND, blade);
			Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
			spawned.add(zombie);
			blade.getItem().hurtEnemy(blade, zombie, p);
			PlayerCultivation c = CultivationManager.own(p);
			helper.assertTrue(blade.isEmpty() && c.natalTreasure() == null && p.hasEffect(ModEffects.SOUL_WOUND) && c.soulWounded(),
					"Worn out, it breaks: the bond gone, the soul wounded");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aNatalBladeRisesAGradeAtTheForge(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "natal-raiser");
			at(p, Realm.CORE_FORMATION);
			ItemStack blade = Artifacts.withQuality(forgedBy(p), Artifacts.Quality.FINE);
			ItemStack plain = forgedBy(p);
			helper.assertTrue(!ArtifactRecipes.canAdd(List.of(), plain), "A blade not bound is no material");
			NatalTreasures.atForge(p, blade);
			blade.setDamageValue(50);
			List<ItemStack> contents = new ArrayList<>();
			contents.add(blade.copy());
			helper.assertTrue(!ArtifactRecipes.canAdd(contents, new ItemStack(Items.IRON_INGOT)), "Its own grade's metal won't raise it");
			ItemStack[] spirit = {new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.LEATHER),
					new ItemStack(ArtifactGrade.SPIRIT.stone()), new ItemStack(ArtifactGrade.SPIRIT.stone()),
					DemonCoreItem.of(PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), QiElement.FIRE, null)};
			for (ItemStack material : spirit) {
				helper.assertTrue(ArtifactRecipes.canAdd(contents, material), "The next grade's " + material.getHoverName().getString() + " goes in");
				contents.add(material);
			}
			ArtifactRecipes.Recipe recipe = ArtifactRecipes.match(contents);
			helper.assertTrue(recipe != null && recipe.kind() == ArtifactKind.SABER && recipe.grade() == ArtifactGrade.SPIRIT, "A Spirit saber's working: " + recipe);
			ItemStack up = NatalTreasures.raised(contents.get(0), recipe);
			helper.assertTrue(up.is(ModItems.artifact(ArtifactKind.SABER, ArtifactGrade.SPIRIT)) && Artifacts.quality(up) == Artifacts.Quality.FINE
					&& NatalTreasures.idOf(blade).equals(NatalTreasures.idOf(up)) && up.getDamageValue() == 0,
					"Raised: a Spirit saber, its Fine work and its bond kept, as good as new");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	// --- Helpers ---

	/** A Mortal saber forged by {@code p}. */
	private static ItemStack forgedBy(ServerPlayer p) {
		ItemStack blade = Artifacts.create(ArtifactKind.SABER, ArtifactGrade.MORTAL, Artifacts.Quality.NORMAL, null);
		NatalTreasures.stampForger(blade, p);
		return blade;
	}

	private static void at(ServerPlayer p, Realm realm) {
		CultivationManager.get(p).setState(realm, Stage.EARLY, 0);
		CultivationManager.refresh(p);
	}

	/** A survival player in the player list (the helper's mock player is creative), remembered in {@code made}. */
	private static ServerPlayer survivor(GameTestHelper helper, List<ServerPlayer> made, String name) {
		ServerPlayer p = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), name)) {
			@Override
			public boolean isCreative() {
				return false;
			}
		};
		helper.getLevel().getServer().getPlayerList().placeNewPlayer(new Connection(PacketFlow.SERVERBOUND), p);
		p.setGameMode(GameType.SURVIVAL);
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		CultivationManager.own(p).awaken(0);
		made.add(p);
		return p;
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			NatalTreasures.forget(p.getUUID());
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
