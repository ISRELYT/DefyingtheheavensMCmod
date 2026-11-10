package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Talismans, the fourth craft (the user's "Build as designed", 2026-11-10; Talismans): drawn with the brush, paper, cinnabar and qi; as strong as
 * their drawer, at most a realm above weaker hands; what each pattern does where it lands or on whoever burns it.
 */
public class TalismanGameTests implements FabricGameTest {
	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTalismanIsDrawnWithPaperCinnabarAndQi(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "talisman-drawer");
			PlayerCultivation c = CultivationManager.get(p);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			c.setQi(c.maxQi());
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.INSCRIPTION_BRUSH));
			p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.TALISMAN_PAPER, 3));
			helper.assertTrue(key(Talismans.refusal(p, TalismanPattern.FIRE)).equals(ModLang.MSG_TALISMAN_NEEDS), "No redstone: no cinnabar to draw with");
			p.getInventory().add(new ItemStack(Items.REDSTONE, 4));
			Talismans.choose(p, TalismanPattern.FIRE.ordinal());
			helper.assertTrue(Talismans.chosen(p.getMainHandItem()) == TalismanPattern.FIRE, "The brush is set to the Fire pattern");
			double qi = c.getQi();
			int practice = CultivationManager.own(p).getMastery(CraftMastery.Craft.TALISMANS);
			Talismans.draw(p, InteractionHand.MAIN_HAND);
			int slot = p.getInventory().findSlotMatchingItem(firstOf(p, ModItems.FIRE_TALISMAN));
			ItemStack drawn = slot >= 0 ? p.getInventory().getItem(slot) : ItemStack.EMPTY;
			helper.assertTrue(drawn.is(ModItems.FIRE_TALISMAN) && Talismans.drawnAt(drawn) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY)
					&& "talisman-drawer".equals(drawn.getTag().getString(Talismans.DRAWER)), "A Fire Talisman drawn, its drawer's realm and name on it");
			helper.assertTrue(p.getOffhandItem().getCount() == 2 && countOf(p, Items.REDSTONE) == 3, "A sheet of paper and a redstone spent");
			helper.assertTrue(Math.abs(qi - c.getQi() - c.maxQi() * TalismanPattern.FIRE.qiShare) < 1.0e-6, "Its share of the qi pool spent");
			helper.assertTrue(CultivationManager.own(p).getMastery(CraftMastery.Craft.TALISMANS) == practice + TalismanPattern.FIRE.practice(),
					"Drawing teaches the craft of talismans");
			c.setQi(0);
			helper.assertTrue(key(Talismans.refusal(p, TalismanPattern.FIRE)).equals(ModLang.MSG_TALISMAN_LOW_QI), "No qi: no drawing");
			helper.assertTrue(Math.abs(CraftMastery.failChance(CraftMastery.Rank.APPRENTICE, TalismanPattern.FIVE_THUNDER.difficulty) - 0.24) < 1.0e-9
					&& CraftMastery.failChance(CraftMastery.Rank.MASTER, TalismanPattern.FIVE_THUNDER.difficulty) == 0,
					"Five Thunder fails an Apprentice a quarter of the time, a Master never");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aTalismanInWeakerHandsWorksAtMostARealmAbove(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "talisman-weak");
			TestMethods.setAt(CultivationManager.get(p), Realm.FOUNDATION_BUILDING, Stage.EARLY);
			int own = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY), immortal = PlayerCultivation.rank(Realm.TRUE_IMMORTAL, Stage.EARLY);
			ItemStack strong = Talismans.drawnBy(TalismanPattern.FIRE, immortal, "an immortal", 1);
			helper.assertTrue(Talismans.effectiveRank(strong, p) == own + Talismans.ABOVE_BURNER, "A True Immortal's talisman in a Foundation Building hand: a realm above it");
			ItemStack weak = Talismans.drawnBy(TalismanPattern.FIRE, 1, "a beginner", 1);
			helper.assertTrue(Talismans.effectiveRank(weak, p) == 1, "A weaker hand's talisman: its own drawer's");
			helper.assertTrue(Talismans.effectiveRank(new ItemStack(ModItems.FIRE_TALISMAN), p) == own, "One of no known hand: its user's own");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void thrownTalismansBurnFreezeStrikeAndBind(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			ServerPlayer p = survivor(helper, made, "talisman-thrower");
			int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			TestMethods.setAt(CultivationManager.get(p), Realm.CORE_FORMATION, Stage.EARLY);
			Zombie burnt = zombie(helper, spawned, new BlockPos(4, 2, 4)), frozen = zombie(helper, spawned, new BlockPos(9, 2, 4)),
					bound = zombie(helper, spawned, new BlockPos(4, 2, 9));
			land(level, p, TalismanPattern.FIRE, rank, burnt);
			helper.assertTrue(burnt.getHealth() < burnt.getMaxHealth() && burnt.isOnFire(), "Fire: the one it lands on burns");
			land(level, p, TalismanPattern.FROST, rank, frozen);
			helper.assertTrue(frozen.getHealth() < frozen.getMaxHealth() && frozen.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "Frost: harmed and slowed");
			land(level, p, TalismanPattern.BINDING, rank, bound);
			helper.assertTrue(bound.hasEffect(ModEffects.BOUND), "Binding: held where it stands");
			DemonBeast great = ModEntities.beast(BeastKind.GOLDEN_WINGED_ROC).create(level);
			Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(9, 2, 9)));
			great.moveTo(at.x, at.y, at.z, 0, 0);
			great.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.LATE));
			great.setNoAi(true);
			level.addFreshEntity(great);
			spawned.add(great);
			helper.assertTrue(!Talismans.bind(great, PlayerCultivation.rank(Realm.QI_REFINING, Stage.EARLY), 1, p) && !great.hasEffect(ModEffects.BOUND),
					"A Qi Refining talisman binds nothing a realm or more above it");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void burntTalismansShieldSpeedAndConceal(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer p = survivor(helper, made, "talisman-burner");
			TestMethods.setAt(CultivationManager.get(p), Realm.FOUNDATION_BUILDING, Stage.EARLY);
			helper.assertTrue(Talismans.burn(p, new ItemStack(ModItems.GOLDEN_LIGHT_TALISMAN), TalismanPattern.GOLDEN_LIGHT) && p.getAbsorptionAmount() > 0
					&& p.hasEffect(ModEffects.GOLDEN_LIGHT), "Golden Light: a shell that soaks up harm (" + p.getAbsorptionAmount() + ")");
			p.removeEffect(ModEffects.GOLDEN_LIGHT);
			helper.assertTrue(p.getAbsorptionAmount() == 0, "Its time up, the shell is gone");
			helper.assertTrue(Talismans.burn(p, new ItemStack(ModItems.DIVINE_SPEED_TALISMAN), TalismanPattern.DIVINE_SPEED)
					&& p.hasEffect(MobEffects.MOVEMENT_SPEED), "Divine Speed: swift feet");
			Zombie zombie = zombie(helper, spawned, new BlockPos(5, 2, 5));
			helper.assertTrue(Talismans.burn(p, new ItemStack(ModItems.CONCEALMENT_TALISMAN), TalismanPattern.CONCEALMENT) && p.hasEffect(ModEffects.CONCEALED),
					"Concealment burnt");
			zombie.setTarget(p);
			helper.assertTrue(zombie.getTarget() == null, "Concealed, a monster doesn't take you up");
			zombie.hurt(p.damageSources().playerAttack(p), 1);
			helper.assertTrue(!p.hasEffect(ModEffects.CONCEALED), "You strike it: concealed no more");
			zombie.setTarget(p);
			helper.assertTrue(zombie.getTarget() == p, "And it can come for you again");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theThousandLiEscapeCarriesYouTheWayYouFace(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerLevel level = helper.getLevel();
		try {
			ServerPlayer p = survivor(helper, made, "talisman-escaper");
			p.setYRot(0); // facing south (+z)
			Vec3 from = p.position();
			ItemStack escape = Talismans.drawnBy(TalismanPattern.THOUSAND_LI_ESCAPE, 0, "test", 1);
			escape.getTag().putInt(Talismans.REACH, 48);
			// Ground to land on 40 blocks south, whatever the test world's own land is there.
			BlockPos pad = BlockPos.containing(from.x, from.y - 1, from.z + 40);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					level.setBlock(pad.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
					for (int dy = 1; dy <= 2; dy++) level.setBlock(pad.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				}
			}
			BlockPos landing = Talismans.landing(level, from, new Vec3(0, 0, 1), 48);
			helper.assertTrue(landing != null, "Safe ground lies that way");
			helper.assertTrue(Talismans.burn(p, escape, TalismanPattern.THOUSAND_LI_ESCAPE), "The talisman carries them");
			double moved = p.getZ() - from.z;
			helper.assertTrue(moved >= 12 && moved <= 49 && Math.abs(p.getX() - from.x) < 1.5, "They flash " + String.format("%.1f", moved) + " blocks south");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	// --- Helpers ---

	private static void land(ServerLevel level, ServerPlayer thrower, TalismanPattern pattern, int rank, Entity target) {
		TalismanEntity t = new TalismanEntity(level, thrower);
		t.setItem(new ItemStack(pattern.item()));
		t.setStrength(rank, 1);
		t.moveTo(target.getX(), target.getY() + 1, target.getZ());
		Talismans.land(t, new EntityHitResult(target));
		t.discard();
	}

	private static Zombie zombie(GameTestHelper helper, List<Entity> spawned, BlockPos at) {
		Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, at);
		zombie.setInvulnerable(false);
		spawned.add(zombie);
		return zombie;
	}

	private static ItemStack firstOf(ServerPlayer p, net.minecraft.world.item.Item item) {
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) return p.getInventory().getItem(i);
		return new ItemStack(item);
	}

	private static int countOf(ServerPlayer p, net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
		return n;
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
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
