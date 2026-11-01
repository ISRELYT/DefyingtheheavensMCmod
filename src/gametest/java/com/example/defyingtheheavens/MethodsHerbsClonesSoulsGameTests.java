package com.example.defyingtheheavens;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The user's picks of the second brainstorm (2026-11-01): Spirit Herb Refining's herbs eaten for their essence, by their realm, resting
 * after; Clone Seclusion's clone banking its maker's stage, a challenger sent for it, drawn back in, and its bank lost as it is slain;
 * Borrowed Life's soul drawing on a possessed cultivator's life, the host drained, the soul thrown out at its hold's end and what it drew
 * settling in it.
 */
public class MethodsHerbsClonesSoulsGameTests implements FabricGameTest {
	private static boolean near(double a, double b) {
		return Math.abs(a - b) < 1.0e-6 * Math.max(1, Math.abs(b));
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void herbsAreEatenForTheirEssence(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		try {
			// A herb's realm: its age's, a rarer kind a realm higher (Four Axis passed over).
			helper.assertTrue(GinsengItem.grade(GinsengItem.create(ModItems.GINSENG, 40)) == Realm.FOUNDATION_BUILDING
					&& GinsengItem.grade(GinsengItem.create(ModItems.GINSENG, 150)) == Realm.CORE_FORMATION
					&& GinsengItem.grade(GinsengItem.create(ModItems.SPIRIT_GINSENG, 40)) == Realm.CORE_FORMATION
					&& GinsengItem.grade(GinsengItem.create(ModItems.LINGZHI, 6000)) == Realm.DAO_INTEGRATION
					&& GinsengItem.grade(GinsengItem.create(ModItems.PURPLE_LINGZHI, 1500)) == Realm.DAO_INTEGRATION
					&& GinsengItem.grade(GinsengItem.create(ModItems.SPIRIT_LOTUS, 6000)) == Realm.VOID_SHATTERING, "Herbs by their age and kind");
			helper.assertTrue(near(SpiritHerbRefining.worth(Realm.CORE_FORMATION, Realm.CORE_FORMATION), SpiritHerbRefining.SHARE)
					&& near(SpiritHerbRefining.worth(Realm.FOUNDATION_BUILDING, Realm.CORE_FORMATION), SpiritHerbRefining.SHARE / 2)
					&& near(SpiritHerbRefining.worth(Realm.DAO_INTEGRATION, Realm.CORE_FORMATION), SpiritHerbRefining.SHARE * 4), "x2 a realm above (at most two), /2 below");
			ServerPlayer p = survivor(helper, made, "herb-refiner");
			PlayerCultivation c = CultivationManager.own(p);
			c.awaken(0);
			TestMethods.setAt(c, Realm.CORE_FORMATION, Stage.EARLY);
			ItemStack herbs = GinsengItem.create(ModItems.GINSENG, 150);
			herbs.setCount(3);
			p.setItemInHand(InteractionHand.MAIN_HAND, herbs);
			helper.assertTrue(!SpiritHerbRefining.practises(c)
					&& ModItems.GINSENG.use(p.level(), p, InteractionHand.MAIN_HAND).getResult() == InteractionResult.PASS, "Not practised: a cultivator keeps its herbs");
			CultivationMethods.learn(p, CultivationMethod.SPIRIT_HERB_REFINING, Realm.DAO_INTEGRATION);
			CultivationMethods.practise(p, CultivationMethod.SPIRIT_HERB_REFINING);
			helper.assertTrue(SpiritHerbRefining.practises(c)
					&& ModItems.GINSENG.use(p.level(), p, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME, "Practised: eaten as food is");
			p.stopUsingItem();
			ModItems.GINSENG.finishUsingItem(herbs, p.level(), p);
			helper.assertTrue(herbs.getCount() == 2 && near(c.getCultivation(), c.cultivationRequired() * SpiritHerbRefining.SHARE),
					"A Core Formation herb eaten at Core Formation: a tenth of the stage (" + c.getCultivation() / c.cultivationRequired() + ")");
			helper.assertTrue(p.getCooldowns().isOnCooldown(ModItems.GINSENG) && p.getCooldowns().isOnCooldown(ModItems.NETHERWORLD_LOTUS),
					"Every herb rests while the essence settles");
			c.setState(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION, PlayerCultivation.required(Realm.CORE_FORMATION, Stage.GRAND_PERFECTION));
			ModItems.GINSENG.finishUsingItem(herbs, p.level(), p);
			helper.assertTrue(c.isStalled() && herbs.getCount() == 2, "The bottleneck: nothing eaten");
			helper.succeed();
		} finally {
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void aSecludedCloneBanksItsMakersStage(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		ServerPlayer p = survivor(helper, made, "clone-secluder");
		PlayerCultivation c = CultivationManager.own(p);
		MinecraftServer server = helper.getLevel().getServer();
		boolean waiting = false;
		try {
			c.awaken(0);
			TestMethods.setAt(c, Realm.NASCENT_SOUL, Stage.EARLY);
			CultivationMethods.learn(p, CultivationMethod.CLONE_SECLUSION, Realm.ENTERING_NIRVANA);
			CultivationMethods.practise(p, CultivationMethod.CLONE_SECLUSION);
			double perSecond = CloneSeclusion.perSecond(c);
			helper.assertTrue(CloneSeclusion.practises(c)
					&& near(perSecond, CloneSeclusion.PACE * c.cultivationPerSecond() * PlayerCultivation.qiNeed(Realm.NASCENT_SOUL) / c.cultivationRequired()),
					"Practised: half the realm's own pace a second (" + perSecond + ")");
			Clones.act(p);
			CloneBodyEntity clone = Clones.bodyNotPlayed(server, c);
			helper.assertTrue(clone != null && !clone.isSitting(), "A clone split off and left standing");
			waiting = true;
			helper.runAfterDelay(70, () -> {
				try {
					double banked = CloneSeclusion.banked(p);
					helper.assertTrue(clone.isSecluding() && banked > 0 && banked <= 4 * perSecond + 1.0e-9,
							"Seconds of seclusion banked, the clone in meditation (" + banked / perSecond + " s)");
					// A challenger: a beast of the maker's rank (grown past its kind: no wild tier holds Nascent Soul), hunting the clone.
					ServerLevel level = helper.getLevel();
					List<DemonBeast> before = level.getEntitiesOfClass(DemonBeast.class, clone.getBoundingBox().inflate(24));
					CloneSeclusion.challenge(p, c, level, clone);
					List<DemonBeast> after = level.getEntitiesOfClass(DemonBeast.class, clone.getBoundingBox().inflate(24));
					after.removeAll(before);
					helper.assertTrue(after.size() == 1 && after.get(0).getTarget() == clone
							&& after.get(0).getRank() == PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), "A challenger of its rank comes for the clone");
					after.forEach(DemonBeast::discard);
					// Drawn back in: what it banked is its maker's.
					double was = c.getCultivation(), held = CloneSeclusion.banked(p);
					p.setShiftKeyDown(true);
					Clones.act(p);
					helper.assertTrue(c.getClone() == null && near(c.getCultivation() - was, held * c.cultivationRequired()),
							"Drawn back in: its bank is the maker's (" + (c.getCultivation() - was) / c.cultivationRequired() + ")");
					// Slain where it stands: its bank lost with it.
					p.setShiftKeyDown(false);
					Clones.act(p);
					CloneBodyEntity second = Clones.bodyNotPlayed(server, c);
					helper.assertTrue(second != null, "Another split off");
					c.getClone().putDouble(CloneSeclusion.BANKED, 0.3);
					double kept = c.getCultivation();
					second.hurt(level.damageSources().magic(), 10000);
					helper.assertTrue(c.getClone() == null && near(c.getCultivation(), kept), "Slain: its bank lost, nothing gained");
					helper.succeed();
				} finally {
					cleanUp(p, made, server);
				}
			});
		} finally {
			if (!waiting) cleanUp(p, made, server);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aBorrowedLifeIsDrawnOffTillTheHostThrowsTheSoulOut(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		CultivatorNpc npc = null;
		try {
			helper.assertTrue(near(BorrowedLife.share(Realm.HEAVENLY_BEING, Realm.NASCENT_SOUL), BorrowedLife.SHARE * 2)
					&& near(BorrowedLife.share(Realm.CORE_FORMATION, Realm.NASCENT_SOUL), BorrowedLife.SHARE / 2)
					&& near(BorrowedLife.share(Realm.TRUE_IMMORTAL, Realm.NASCENT_SOUL), BorrowedLife.MOST), "x2 a realm above, /2 below, at most MOST");
			helper.assertTrue(BorrowedLife.holdFor(Realm.CORE_FORMATION, Realm.NASCENT_SOUL) == 2 * BorrowedLife.HOLD
					&& BorrowedLife.holdFor(Realm.HEAVENLY_BEING, Realm.NASCENT_SOUL) == BorrowedLife.HOLD / 2
					&& BorrowedLife.holdFor(Realm.QI_REFINING, Realm.NASCENT_SOUL) == BorrowedLife.MOST_HOLD
					&& BorrowedLife.holdFor(Realm.VOID_SHATTERING, Realm.NASCENT_SOUL) == BorrowedLife.LEAST_HOLD, "Held longer the weaker the host");
			PlayerCultivation record = new PlayerCultivation();
			record.setMortal(false);
			record.setState(Realm.CORE_FORMATION, Stage.EARLY, PlayerCultivation.required(Realm.CORE_FORMATION, Stage.EARLY) * 0.5);
			BorrowedLife.drain(record, 0.1);
			helper.assertTrue(near(record.getCultivation(), record.cultivationRequired() * 0.4), "Drained a share of its stage");
			BorrowedLife.drain(record, 1.0);
			helper.assertTrue(record.getCultivation() == 0 && record.getRealm() == Realm.CORE_FORMATION && record.getStage() == Stage.EARLY, "Never past the stage's start");

			npc = helper.spawnWithNoFreeWill(ModEntities.CULTIVATOR, new BlockPos(3, 2, 2));
			npc.setRank(PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY));
			npc.cultivation().setState(Realm.CORE_FORMATION, Stage.EARLY, PlayerCultivation.required(Realm.CORE_FORMATION, Stage.EARLY) * 0.5);
			ServerPlayer soul = projected(helper, made, "borrowed-life", r -> r.netherworld().setPath(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY), 0));
			CultivationMethods.learn(soul, CultivationMethod.BORROWED_LIFE, Realm.VOID_SHATTERING);
			CultivationMethods.practise(soul, CultivationMethod.BORROWED_LIFE);
			PlayerCultivation own = CultivationManager.own(soul);
			helper.assertTrue(BorrowedLife.practises(own), "Practised");
			helper.assertTrue(SoulProjection.tryPossess(soul, npc) == InteractionResult.SUCCESS && npc.isVacant(), "The soul takes the cultivator's body");
			MinecraftServer server = helper.getLevel().getServer();
			double share = BorrowedLife.share(Realm.CORE_FORMATION, Realm.NASCENT_SOUL);
			for (int i = 0; i < BorrowedLife.EVERY; i++) BorrowedLife.tick(server);
			PlayerCultivation body = Possession.borrowed(soul);
			helper.assertTrue(near(BorrowedLife.drawn(soul.getUUID()), share) && near(body.getCultivation(), body.cultivationRequired() * (0.5 - share)),
					"A draw: its share held, the host drained as much");
			int hold = BorrowedLife.holdFor(Realm.CORE_FORMATION, Realm.NASCENT_SOUL);
			for (int i = 0; i < hold; i++) BorrowedLife.tick(server);
			double drawn = (hold / BorrowedLife.EVERY - 1) * share;
			helper.assertTrue(!Possession.inhabits(soul.getUUID()) && !npc.isVacant() && soul.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
					"Its hold up, the host throws the soul out, reeling");
			helper.assertTrue(near(own.netherworld().pathCultivation(), drawn * GhostMethods.required(PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY))),
					"What it drew settles in its ghost path as it leaves (" + own.netherworld().pathCultivation() + ")");
			helper.assertTrue(near(npc.cultivation().getCultivation(), npc.cultivation().cultivationRequired() * (0.5 - drawn)),
					"The host keeps what it was drained to (" + npc.cultivation().getCultivation() / npc.cultivation().cultivationRequired() + ")");
			helper.assertTrue(npc.regard(soul) <= Regard.HOSTILE, "And remembers the soul (" + npc.regard(soul) + ")");
			helper.succeed();
		} finally {
			for (ServerPlayer p : made) if (Possession.inhabits(p.getUUID())) SoulProjection.letGo(p, null);
			for (ServerPlayer p : made) {
				BorrowedLife.forget(p.getUUID());
				SoulProjection.onDeath(p);
				CultivationManager.own(p).setPossession(null);
				p.server.getPlayerList().remove(p);
			}
			if (npc != null) npc.discard();
		}
	}

	// --- Helpers ---

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
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		p.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		p.setOnGround(true);
		made.add(p);
		return p;
	}

	/** A Nascent Soul Early cultivator ({@code setup} done to its record first) with its soul already out, full of qi. */
	private static ServerPlayer projected(GameTestHelper helper, List<ServerPlayer> made, String name, Consumer<PlayerCultivation> setup) {
		ServerPlayer p = survivor(helper, made, name);
		PlayerCultivation c = CultivationManager.get(p);
		c.setState(Realm.NASCENT_SOUL, Stage.EARLY, 0);
		setup.accept(c);
		CultivationStats.apply(p, c);
		p.setHealth(p.getMaxHealth());
		c.setQi(c.maxQi());
		long now = helper.getLevel().getGameTime();
		SoulProjection.toggle(p);
		for (int i = 0; i < SoulProjection.CHANNEL_TICKS; i++) SoulProjection.tick(p, now);
		helper.assertTrue(SoulProjection.isSoul(p), "The soul is out");
		return p;
	}

	/** The clone maker's clone (and any of its seclusion) taken away, and the players out. */
	private static void cleanUp(ServerPlayer p, List<ServerPlayer> made, MinecraftServer server) {
		PlayerCultivation c = CultivationManager.own(p);
		CloneBodyEntity left = c.getClone() == null ? null : Clones.bodyNotPlayed(server, c);
		if (left != null) left.discard();
		c.setClone(null);
		CloneSeclusion.forget(p.getUUID());
		leave(made);
	}

	private static void leave(List<ServerPlayer> made) {
		for (ServerPlayer p : made) {
			MeditationManager.forget(p.getUUID());
			HeavenMethods.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
