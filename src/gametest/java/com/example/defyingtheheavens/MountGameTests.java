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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A bound beast ridden by its owner (the user's "Build as designed", 2026-11-10; SpiritBonds#mount): big enough to bear them or not, steered by
 * them and quiet while they ride, a flier kept to the ground where its rider couldn't fly, and a rider taken far off let go.
 */
public class MountGameTests implements FabricGameTest {
	private static String key(Component message) {
		return message != null && message.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anOwnersEmptyHandPutsThemOnItsBack(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer owner = survivor(helper, made, "rider"), stranger = survivor(helper, made, "stranger");
			int rank = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
			DemonBeast tiger = beast(helper, spawned, BeastKind.BLOODFANG_TIGER, new BlockPos(3, 2, 3), rank);
			tiger.bindTo(owner.getUUID());
			helper.assertTrue(tiger.bearsARider(), "A bloodfang tiger is big enough to bear a rider (" + tiger.getBbWidth() + " wide)");
			helper.assertTrue(SpiritBonds.interact(tiger, stranger, InteractionHand.MAIN_HAND) == InteractionResult.PASS && !stranger.isPassenger(),
					"Another's empty hand gets them nowhere");
			helper.assertTrue(SpiritBonds.interact(tiger, owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && owner.getVehicle() == tiger,
					"Its owner's empty hand puts them on its back");
			helper.assertTrue(tiger.rider() == owner && tiger.getControllingPassenger() == owner, "Its rider steers it");
			Vec3 seat = tiger.seat();
			helper.assertTrue(seat.y > tiger.getY() + tiger.getBbHeight() * 0.5 && seat.y < tiger.getY() + tiger.getBbHeight() * 1.01,
					"Its rider sits on its back (" + (seat.y - tiger.getY()) + " up of " + tiger.getBbHeight() + ")");
			owner.stopRiding();
			helper.assertTrue(!owner.isPassenger() && tiger.rider() == null, "Off again");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aSmallBeastBearsNoOneUntilItGrows(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer owner = survivor(helper, made, "wolf-rider");
			DemonBeast wolf = beast(helper, spawned, BeastKind.SHADOW_WOLF, new BlockPos(3, 2, 3), 0);
			wolf.bindTo(owner.getUUID());
			helper.assertTrue(!wolf.bearsARider() && SpiritBonds.interact(wolf, owner, InteractionHand.MAIN_HAND) == InteractionResult.CONSUME
					&& !owner.isPassenger() && key(SpiritBonds.mountRefusal(wolf, owner)).equals(ModLang.MSG_MOUNT_TOO_SMALL),
					"A shadow wolf at Qi Refining Early is too small to bear a rider (" + wolf.getBbWidth() + " wide)");
			SpiritBonds.grow(wolf, 3);
			helper.assertTrue(!wolf.bearsARider(), "Three stages grown, still too small (" + wolf.getBbWidth() + ")");
			SpiritBonds.grow(wolf, 4);
			helper.assertTrue(wolf.bearsARider() && SpiritBonds.interact(wolf, owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS
					&& owner.getVehicle() == wolf, "Four stages grown it bears its owner (" + wolf.getBbWidth() + " wide)");
			DemonBeast hawk = beast(helper, spawned, BeastKind.WIND_HAWK, new BlockPos(5, 2, 5), 0);
			helper.assertTrue(!hawk.bearsARider(), "A wind hawk is far too small at first");
			SpiritBonds.grow(hawk, 6);
			helper.assertTrue(hawk.bearsARider(), "Grown six stages (to Foundation Building), it bears one (" + hawk.getBbWidth() + ")");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void riddenItFightsNothingAndAFlierKeepsToTheGroundWhereItsRiderCouldntFly(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer owner = survivor(helper, made, "roc-rider");
			int rank = PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY);
			DemonBeast roc = beast(helper, spawned, BeastKind.GOLDEN_WINGED_ROC, new BlockPos(3, 2, 3), rank);
			roc.bindTo(owner.getUUID());
			Entity prey = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 6));
			spawned.add(prey);
			roc.setTarget((net.minecraft.world.entity.LivingEntity) prey);
			helper.assertTrue(SpiritBonds.interact(roc, owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && roc.getTarget() == null,
					"Mounted, its fight is left off");
			roc.setTarget((net.minecraft.world.entity.LivingEntity) prey);
			roc.tendRidden();
			helper.assertTrue(roc.getTarget() == null, "Ridden, it takes up no fight");
			helper.assertTrue(!SpiritBonds.groundedFor(owner) && roc.fliesWithItsRider() && roc.isNoGravity(),
					"Where its rider could fly, it takes to the air with them, its wings holding it up");
			double pace = BeastKind.GOLDEN_WINGED_ROC.speed * SpiritBonds.FLIGHT_PACE * (1 + SpiritBonds.PACE_PER_STAGE * (rank - BeastKind.GOLDEN_WINGED_ROC.minRank()));
			helper.assertTrue(Math.abs(SpiritBonds.flightPace(roc) - pace) < 1.0e-9 && pace > 0.6,
					"A roc two stages above its kind's least flies at twice its kind's speed a tick and a little more (" + SpiritBonds.flightPace(roc) + ")");
			QiFlight.weighDown(owner, 200);
			for (int i = 0; i < 10; i++) {
				roc.tickCount++;
				roc.tendRidden();
			}
			helper.assertTrue(SpiritBonds.groundedFor(owner) && !roc.fliesWithItsRider() && !roc.isNoGravity(),
					"Weighed down (the Weak Water, a Killing Formation), its rider can't fly: neither can it");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void aRiderTakenFarOffIsLetGo(GameTestHelper helper) {
		List<ServerPlayer> made = new ArrayList<>();
		List<Entity> spawned = new ArrayList<>();
		try {
			ServerPlayer owner = survivor(helper, made, "torn-rider");
			int rank = PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
			DemonBeast bear = beast(helper, spawned, BeastKind.EARTHQUAKE_BEAR, new BlockPos(3, 2, 3), rank);
			bear.bindTo(owner.getUUID());
			SpiritBonds.interact(bear, owner, InteractionHand.MAIN_HAND);
			helper.assertTrue(owner.getVehicle() == bear, "On its back");
			bear.positionRider(owner);
			helper.assertTrue(owner.getVehicle() == bear && owner.distanceTo(bear) < 3, "Sat on its back, kept there");
			owner.setPos(owner.getX() + 40, owner.getY(), owner.getZ());
			bear.positionRider(owner);
			helper.assertTrue(!owner.isPassenger(), "Taken 40 blocks off by a teleport, the rider is let go, not dragged back");
			helper.succeed();
		} finally {
			spawned.forEach(Entity::discard);
			leave(made);
		}
	}

	// --- Helpers ---

	private static DemonBeast beast(GameTestHelper helper, List<Entity> spawned, BeastKind kind, BlockPos at, int rank) {
		ServerLevel level = helper.getLevel();
		DemonBeast beast = ModEntities.beast(kind).create(level);
		Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(at));
		beast.moveTo(pos.x, pos.y, pos.z, 0, 0);
		beast.setRank(rank);
		beast.setNoAi(true);
		level.addFreshEntity(beast);
		spawned.add(beast);
		return beast;
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
			p.stopRiding();
			QiFlight.forget(p.getUUID());
			p.server.getPlayerList().remove(p);
		}
	}
}
