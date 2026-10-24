package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.UUID;

/**
 * What the second bug sweep of 2026-10-23 put right, held to it: spell and heart demon cooldowns kept on the record (a relog made them
 * ready at once); no demon left with Shattered Meridians; no Storage Ring folded into another inside a box; a wager's cultivator never
 * keeping its own stake; the devoured losing a stage of their highest path, the Demon Path's too; a golden core on any path feeding a
 * spirit tree; a tiger's roar sparing its own pet and weighing a demon beast by its rank.
 */
public class SweepGameTests implements FabricGameTest {
	private static ServerPlayer player(GameTestHelper helper, String name) {
		ServerPlayer p = FakePlayer.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), name));
		p.setGameMode(GameType.SURVIVAL);
		return p;
	}

	/** A demon of {@code race}, born (DemonPath#born). */
	private static ServerPlayer demon(GameTestHelper helper, Bloodline race) {
		ServerPlayer p = player(helper, "sweep-demon");
		Bloodlines.set(helper.getLevel().getServer(), p.getUUID(), race, 0);
		DemonPath.born(p);
		return p;
	}

	private static void forget(MinecraftServer server, ServerPlayer... players) {
		for (ServerPlayer p : players) {
			Bloodlines.forget(server, p.getUUID());
			RaceGifts.forget(p.getUUID());
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cooldownsOutlastALogout(GameTestHelper helper) {
		ServerPlayer p = player(helper, "sweep-cooldowns");
		Spells.setCooldown(p, Spell.GHOST_FIRE, 400);
		Spells.forget(p.getUUID()); // what a logout forgets
		helper.assertTrue(Spells.cooldownLeft(p, Spell.GHOST_FIRE) > 300, "A spell's cooldown outlasts a logout");
		PlayerCultivation own = CultivationManager.own(p);
		NetherworldState copy = new NetherworldState();
		copy.load(own.netherworld().save());
		helper.assertTrue(copy.spellReady().get(Spell.GHOST_FIRE) != null && copy.spellReady().get(Spell.GHOST_FIRE).equals(own.netherworld().spellReady().get(Spell.GHOST_FIRE)),
				"...kept on the record, and saved with it");
		own.setHeartDemonAt(123456);
		helper.assertTrue(PlayerCultivation.load(own.save()).heartDemonAt() == 123456, "The heart demon's last coming is saved too");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void noDemonKeepsShatteredMeridians(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer fresh = player(helper, "sweep-shattered"), old = player(helper, "sweep-shattered-old");
		try {
			// Born a demon with Shattered Meridians rolled before its blood was chosen.
			PlayerCultivation c = CultivationManager.get(fresh);
			c.setPhysique(Physique.SHATTERED_MERIDIANS);
			helper.assertTrue(c.isMortal() && c.hasShatteredMeridians(), "A mortal with Shattered Meridians to begin with");
			Bloodlines.set(server, fresh.getUUID(), Bloodline.TIGER, 0);
			DemonPath.born(fresh);
			helper.assertTrue(!c.hasShatteredMeridians() && c.getPhysique() != null && !c.isMortal() && DemonPath.rank(c) == 0 && TwoPaths.heavenRank(c, false) == 0,
					"Its core opens them: another physique, never a mortal, both paths at Qi Refining Early");
			// One left so by an older build: its Demon Path begun, still a mortal with them.
			PlayerCultivation o = CultivationManager.get(old);
			o.setPhysique(Physique.SHATTERED_MERIDIANS);
			o.demon().setBloodline(Bloodline.FOX);
			o.demon().set(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID), 0);
			Bloodlines.set(server, old.getUUID(), Bloodline.FOX, 0);
			DemonPath.born(old);
			helper.assertTrue(!o.hasShatteredMeridians() && !o.isMortal() && DemonPath.rank(o) == PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID),
					"An older one is mended as it is born again, its Demon Path as it was");
			helper.succeed();
		} finally {
			forget(server, fresh, old);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRingHoldsNoRingInABox(GameTestHelper helper) {
		ItemStack ring = new ItemStack(ModItems.STORAGE_RING);
		ItemStack box = new ItemStack(Items.SHULKER_BOX);
		CompoundTag held = new CompoundTag();
		ListTag items = new ListTag();
		CompoundTag one = ring.save(new CompoundTag());
		one.putByte("Slot", (byte) 0);
		items.add(one);
		held.put("Items", items);
		box.getOrCreateTag().put("BlockEntityTag", held);
		helper.assertTrue(!StorageRings.mayHold(ring) && !StorageRings.mayHold(box), "Neither a ring nor a box with a ring in it goes into a ring");
		helper.assertTrue(StorageRings.mayHold(new ItemStack(Items.SHULKER_BOX)) && StorageRings.mayHold(new ItemStack(Items.DIAMOND, 3)), "...anything else does");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aWagersCultivatorNeverKeepsItsOwnStake(GameTestHelper helper) throws ReflectiveOperationException {
		ServerPlayer p = player(helper, "sweep-wager");
		CultivatorNpc npc = helper.spawn(ModEntities.CULTIVATOR, new BlockPos(2, 2, 2));
		try {
			npc.setNoAi(true);
			npc.setRank(PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID));
			p.moveTo(npc.getX() + 2, npc.getY(), npc.getZ());
			p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HIGH_SPIRIT_STONE, 2));
			SparSystem.start(p, npc, SparSystem.Terms.WAGER);
			helper.assertTrue(SparSystem.isSparring(p) && p.getMainHandItem().getCount() == 1, "A wager begun, a stone staked");
			SparSystem.abandon(p); // walked out of it: a forfeit
			java.lang.reflect.Field field = CultivatorNpc.class.getDeclaredField("winnings");
			field.setAccessible(true);
			List<?> winnings = (List<?>) field.get(npc);
			helper.assertTrue(winnings.size() == 1 && ((ItemStack) winnings.get(0)).is(ModItems.HIGH_SPIRIT_STONE),
					"It keeps the player's stake, never its own as well (" + winnings + ")");
			helper.succeed();
		} finally {
			npc.discard();
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theDevouredLoseTheirHighestPath(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer slain = demon(helper, Bloodline.TIGER), killer = player(helper, "sweep-devourer");
		try {
			PlayerCultivation v = CultivationManager.get(slain);
			v.demon().set(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID), 0);
			Physiques.devoured(slain, v, killer);
			helper.assertTrue(DemonPath.rank(v) == PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) && TwoPaths.heavenRank(v, false) == 0,
					"A demon devoured loses a stage of its Demon Path, its highest, not of its heaven path");
			helper.succeed();
		} finally {
			forget(server, slain);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aGoldenCoreOnAnyPathFeedsQi(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = demon(helper, Bloodline.TIGER);
		try {
			PlayerCultivation c = CultivationManager.get(tiger);
			QiFeeding.Messages messages = new QiFeeding.Messages(ModLang.PEACH_NEED_CORE, ModLang.PEACH_NOT_ENOUGH, ModLang.PEACH_MAX);
			c.setQi(c.maxQi());
			helper.assertTrue(QiFeeding.pour(tiger, 0, messages) == null, "No golden core on any path: nothing fed");
			c.demon().set(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
			CultivationManager.refresh(tiger);
			c.setQi(c.maxQi());
			QiFeeding.Fed fed = QiFeeding.pour(tiger, 0, messages);
			helper.assertTrue(fed != null && fed.years() >= 1 && TwoPaths.heavenRank(c, false) == 0,
					"A Demon Path's golden core feeds a spirit tree as the heaven path's does, the heaven path at Qi Refining");
			helper.succeed();
		} finally {
			forget(server, tiger);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void theRoarSparesItsOwnAndKnowsABeastsRank(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer tiger = demon(helper, Bloodline.TIGER);
		Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
		Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(3, 2, 1));
		DemonBeast rhino = helper.spawn(ModEntities.beast(BeastKind.HORNED_THUNDER_RHINO), new BlockPos(5, 2, 3));
		try {
			pig.setNoAi(true);
			wolf.setNoAi(true);
			rhino.setNoAi(true);
			wolf.tame(tiger);
			rhino.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.MID));
			PlayerCultivation t = CultivationManager.get(tiger);
			t.demon().set(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY), 0);
			t.setQi(t.maxQi());
			tiger.moveTo(pig.getX() + 2, pig.getY(), pig.getZ() + 1);
			RaceGifts.roar(tiger);
			helper.assertTrue(pig.hasEffect(MobEffects.WEAKNESS), "The roar cows what stands far below it");
			helper.assertTrue(!wolf.hasEffect(MobEffects.WEAKNESS), "...not its own tamed wolf");
			helper.assertTrue(!rhino.hasEffect(MobEffects.WEAKNESS), "...nor a demon beast of a stage above it (a mortal to it before)");
			helper.succeed();
		} finally {
			pig.discard();
			wolf.discard();
			rhino.discard();
			forget(server, tiger);
		}
	}
}
