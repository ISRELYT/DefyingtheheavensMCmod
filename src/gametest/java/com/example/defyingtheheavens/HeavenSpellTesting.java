package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What the heaven spells' game tests share. Each test runs in a batch of its own (the spells reach far, and would strike other
 * tests' creatures), on a stone floor {@link #FLOOR_HEIGHT} above its own spot, where the chunks tick; the floor and everything
 * on it are cleared when it ends. A caster knows a method to its last layer, with qi to spare and hurtable at once; idle
 * creatures stand to be cast at.
 */
final class HeavenSpellTesting {
	/** How far above the test's spot its floor is laid: clear of the ground and anything on it. */
	static final int FLOOR_HEIGHT = 30;

	/** A test's floor: where, how wide, how much air above it. */
	record Floor(ServerLevel level, BlockPos centre, int radius, int height) {
		/** Takes the floor up again, and whatever was left on it. */
		void remove() {
			clearAround(level, centre, radius + 8);
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					for (int dy = -1; dy < height; dy++) level.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	/**
	 * A stone floor {@code radius} round a spot {@link #FLOOR_HEIGHT} above the test's (its top there), air above it to
	 * {@code height}, cleared first of whatever an earlier run left there (the test world outlives a run).
	 */
	static Floor floor(GameTestHelper helper, int radius, int height) {
		ServerLevel level = helper.getLevel();
		BlockPos centre = helper.absolutePos(new BlockPos(1, FLOOR_HEIGHT, 1));
		clearAround(level, centre, radius + 8);
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				level.setBlock(centre.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
				for (int dy = 0; dy < height; dy++) level.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
			}
		}
		return new Floor(level, centre, radius, height);
	}

	private static void clearAround(ServerLevel level, BlockPos centre, int radius) {
		for (Entity left : level.getEntities((Entity) null, new AABB(centre).inflate(radius, 12, radius), e -> !(e instanceof Player))) {
			left.discard();
		}
	}

	/** A Four Axis Grand Perfection caster who knows {@code method} to its last layer, with qi to spare, standing at {@code at}. */
	static ServerPlayer caster(GameTestHelper helper, HeavenMethod method, BlockPos at) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		PlayerCultivation c = CultivationManager.get(player);
		c.setState(Realm.FOUR_AXIS, Stage.GRAND_PERFECTION, 0);
		c.heaven().setMethod(method, method.layers(), 0);
		CultivationManager.refresh(player);
		c.setQi(c.maxQi());
		vulnerable(player);
		player.teleportTo(helper.getLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		player.setOnGround(true);
		player.setHealth(player.getMaxHealth());
		return player;
	}

	/** A new player can't be hurt for its first three seconds (vanilla's spawn protection), and a mock one is never ticked: this one can, at once. */
	static void vulnerable(ServerPlayer player) {
		try {
			java.lang.reflect.Field field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
			field.setAccessible(true);
			field.setInt(player, 0);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** A creature of {@code type} standing idle (no will of its own) at {@code at}, of {@code health} (0: its own). */
	static <T extends Mob> T idle(ServerLevel level, EntityType<T> type, Vec3 at, double health, List<LivingEntity> spawned) {
		T mob = type.create(level);
		mob.moveTo(at.x, at.y, at.z, 0, 0);
		mob.setNoAi(true);
		mob.setPersistenceRequired();
		if (health > 0) {
			mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
			mob.setHealth((float) health);
		}
		level.addFreshEntity(mob);
		spawned.add(mob);
		return mob;
	}

	/** Turns {@code player} to look at {@code point}. */
	static void lookAt(ServerPlayer player, Vec3 point) {
		Vec3 d = point.subtract(player.getEyePosition());
		float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		player.setYRot(yaw);
		player.setXRot(pitch);
		player.setYHeadRot(yaw);
	}

	/**
	 * What Return to the Bough wants to take hold (a Cultivation Fruit held, a tree looked at): a fruit in {@code player}'s hand and a bough of
	 * leaves over the floor a few blocks before {@code home}, {@code player} looking at it. Returns the bough, for {@link #unBough}.
	 */
	static BlockPos bough(ServerPlayer player, Vec3 home) {
		BlockPos leaves = BlockPos.containing(home.add(0, 3, 3));
		ServerLevel level = (ServerLevel) player.level();
		level.setBlock(leaves, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 2);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, CultivationFruitItem.create(10));
		lookAt(player, Vec3.atCenterOf(leaves));
		return leaves;
	}

	/** The bough {@link #bough} set, and whatever was hung round it, taken away again; the caster's hands emptied. */
	static void unBough(ServerPlayer player, BlockPos leaves) {
		ServerLevel level = (ServerLevel) player.level();
		for (BlockPos pos : BlockPos.betweenClosed(leaves.offset(-6, -6, -6), leaves.offset(6, 6, 6))) {
			if (pos.equals(leaves) || level.getBlockState(pos).is(ModBlocks.CULTIVATION_FRUIT)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
	}

	/** Casts {@code spell}: true if it was paid for (it took hold). */
	static boolean cast(ServerPlayer player, Spell spell) {
		PlayerCultivation c = CultivationManager.get(player);
		double before = c.getQi();
		Spells.setCooldown(player, spell, 0);
		Spells.cast(player, spell);
		return c.getQi() < before;
	}

	/** Clears up after a test: the caster's spells (and their blocks), the creatures spawned, the caster, and the floor. */
	static void cleanUp(ServerPlayer player, List<LivingEntity> spawned, Floor floor) {
		Spells.forget(player.getUUID());
		for (LivingEntity e : spawned) e.discard();
		player.server.getPlayerList().remove(player);
		floor.remove();
	}

	/**
	 * Ends a test whose spells wound the land: it succeeds once every wound is cut (Devastation goes on cutting after the casts, a few
	 * thousand blocks a tick), what they left drained away ({@link #drain}) and the test cleaned up. Before 2026-10-24 the tests that cast
	 * every spell ended at once, their wounds still cutting and a lake still pouring through the cut floor onto the ground the later tests
	 * stand on (the same spot, batch after batch): a later floor was cut, and a fleeing cultivator's husk carried off (one run in three).
	 */
	static void succeedOnceHealed(GameTestHelper helper, ServerPlayer player, List<LivingEntity> spawned, Floor floor) {
		succeedOnceHealed(helper, player, spawned, floor, () -> {});
	}

	/** As {@link #succeedOnceHealed(GameTestHelper, ServerPlayer, List, Floor)}, {@code after} run before the floor goes (a thickening's taking up). */
	static void succeedOnceHealed(GameTestHelper helper, ServerPlayer player, List<LivingEntity> spawned, Floor floor, Runnable after) {
		helper.succeedWhen(() -> {
			helper.assertTrue(Devastation.pending() == 0, "...its spells' wounds still being cut");
			drain(floor.level(), floor.centre(), floor.radius() + 36);
			after.run();
			cleanUp(player, spawned, floor);
		});
	}

	/**
	 * The floor made {@code depth} deeper {@code radius} round {@code x, z}: for a spell whose wound would cut through the one block of it and
	 * drop what stands there out of its reach (the Avatar's palm crater did, 2026-10-24). @return what takes the thickening up again
	 */
	static Runnable thicken(Floor floor, double x, double z, int radius, int depth) {
		BlockPos top = BlockPos.containing(x, floor.centre().getY() - 1, z);
		java.util.function.Consumer<net.minecraft.world.level.block.state.BlockState> fill = state -> {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx * dx + dz * dz > radius * radius) continue;
					for (int dy = 1; dy <= depth; dy++) floor.level().setBlock(top.offset(dx, -dy, dz), state, 2);
				}
			}
		};
		fill.accept(Blocks.STONE.defaultBlockState());
		return () -> fill.accept(Blocks.AIR.defaultBlockState());
	}

	/**
	 * Takes up every fluid and fire within {@code radius} round a floor's {@code centre}, from the ground under it (its spot's) to its air's
	 * height: what its spells left (a lake in a basin, the water falling from it through the floor, a burning trench).
	 */
	static void drain(ServerLevel level, BlockPos centre, int radius) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = -FLOOR_HEIGHT - 1; dy <= 16; dy++) {
					pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
					if (state.getFluidState().isEmpty() && !state.is(net.minecraft.tags.BlockTags.FIRE)) continue;
					// A waterlogged block keeps itself, its water gone; anything else holding fluid (water, lava, kelp) goes.
					var wet = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
					level.setBlock(pos, state.hasProperty(wet) ? state.setValue(wet, false) : Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	static List<LivingEntity> spawned() {
		return new ArrayList<>();
	}

	/** Whether {@code a} and {@code b} differ by no more than a hair. */
	static boolean near(double a, double b) {
		return Math.abs(a - b) < 0.05;
	}

	private HeavenSpellTesting() {}
}
