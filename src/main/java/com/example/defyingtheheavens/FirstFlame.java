package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The First Flame's spring (phase 3c of the realm plan, 2026-10-26): the eternal white-gold flame that burned before heaven and earth were
 * parted, on its own island in the Chaos Frontier some 4,600 blocks south of the Immortal Realm's heart (ModNoiseSettings#SPRING_X,
 * #SPRING_Z: one in every world, the same in each). Its basin is laid the first time someone comes within {@link #LAY_NEAR} of it
 * ({@link #tick}): a ring of pale stone about a bed of the First Flame (FirstFlameBlock), a post at each of its four quarters. Its light,
 * {@link #LIGHT} blocks about the flame ({@link #inLight}), shelters from the Frontier's storm (ChaosFrontier#sheltered), and a Chaos Fiend
 * that comes into it burns away ({@link #burnAway}; none hunts there, ChaosFiend); meditating in it comprehends the Law in focus
 * {@link #INSIGHT_PACE} times as fast (Laws#meditate).
 */
public final class FirstFlame {
	/** How far its light reaches about the flame (blocks). */
	public static final double LIGHT = 16;
	/** How many times as fast the Law in focus is comprehended meditating in its light. */
	public static final double INSIGHT_PACE = 3;
	/** Its basin is laid when someone first comes this near its island (sideways). */
	static final double LAY_NEAR = 96;
	/** The basin: its bed of flame within {@link #BED} of its middle, its pale floor to {@link #FLOOR}, its rim's ring out to {@link #RIM}. */
	static final double BED = 2.5, FLOOR = 3.6, RIM = 4.6;
	/** The flame's white-gold, for its embers in the air. */
	static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.86f, 0.5f), 1.2f);

	/** Where its light comes from: over the middle of its bed, a block above the island's top. */
	public static Vec3 heart() {
		return new Vec3(ModNoiseSettings.SPRING_X + 0.5, ModNoiseSettings.SPRING_Y + 1, ModNoiseSettings.SPRING_Z + 0.5);
	}

	/** Whether {@code at} in {@code dimension} lies in the First Flame's light (the Immortal Realm's, within {@link #LIGHT} of its heart). */
	public static boolean inLight(ResourceKey<Level> dimension, Vec3 at) {
		return ModDimensions.isImmortalRealm(dimension) && at.distanceToSqr(heart()) <= LIGHT * LIGHT;
	}

	/** Every two seconds while anyone is in the Immortal Realm: the basin laid when someone first comes near its island, its land loaded. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 40 != 21) return;
		ServerLevel level = server.getLevel(ModDimensions.IMMORTAL_REALM);
		if (level == null || level.players().isEmpty()) return;
		try {
			Data data = Data.get(server);
			if (data.laid || !someoneNear(level)) return;
			int x = ModNoiseSettings.SPRING_X, z = ModNoiseSettings.SPRING_Z, reach = (int) Math.ceil(RIM) + 2;
			for (int dx = -reach; dx <= reach; dx += 2 * reach) {
				for (int dz = -reach; dz <= reach; dz += 2 * reach) if (!level.isLoaded(new BlockPos(x + dx, 0, z + dz))) return;
			}
			BlockPos floor = lay(level, x, z, ModNoiseSettings.SPRING_Y - 1);
			data.laid = true;
			data.setDirty();
			DefyingTheHeavens.LOGGER.info("The First Flame's basin laid at {}", floor.toShortString());
			for (ServerPlayer player : level.players()) {
				double ddx = player.getX() - x, ddz = player.getZ() - z;
				if (ddx * ddx + ddz * ddz <= LAY_NEAR * LAY_NEAR * 2) {
					player.sendSystemMessage(Component.translatable(ModLang.MSG_FIRST_FLAME).withStyle(ChatFormatting.GOLD));
				}
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("First Flame tick failed", e);
		}
	}

	private static boolean someoneNear(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - ModNoiseSettings.SPRING_X, dz = player.getZ() - ModNoiseSettings.SPRING_Z;
			if (dx * dx + dz * dz <= LAY_NEAR * LAY_NEAR) return true;
		}
		return false;
	}

	/**
	 * The basin laid about column (x, z) on the land's top there (or, where there is none, the island's land gone in a world made before
	 * the Frontier was, on a floating floor of its own at {@code floorY}): the air over it cleared; a floor of smooth quartz, a ring of
	 * quartz bricks about it, a quartz post topped with gold at each quarter of the ring; the First Flame on the floor within {@link #BED},
	 * and over the bed's middle a second, so it rises the higher there. @return the basin's floor at its middle
	 */
	static BlockPos lay(ServerLevel level, int x, int z, int floorY) {
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		boolean ground = top > ModDimensions.IMMORTAL_CLOUD_Y + 4;
		int y0 = ground ? top - 1 : floorY;
		int reach = (int) Math.ceil(RIM) + 2;
		BlockState flame = ModBlocks.FIRST_FLAME.defaultBlockState();
		for (int dx = -reach; dx <= reach; dx++) {
			for (int dz = -reach; dz <= reach; dz++) {
				double r = Math.sqrt(dx * dx + dz * dz);
				if (r > RIM + 1.5) continue;
				BlockPos floor = new BlockPos(x + dx, y0, z + dz);
				for (int up = 1; up <= 8; up++) level.setBlock(floor.above(up), Blocks.AIR.defaultBlockState(), 2);
				if (!ground) {
					for (int down = 0; down <= 3; down++) level.setBlock(floor.below(down), (down == 0 ? Blocks.TUFF : Blocks.DEEPSLATE).defaultBlockState(), 2);
				}
				if (r > RIM) continue;
				level.setBlock(floor, Blocks.SMOOTH_QUARTZ.defaultBlockState(), 2);
				boolean post = (dx == 0 || dz == 0) && Math.round(r) == Math.round(RIM - 0.6);
				if (post) {
					level.setBlock(floor.above(), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
					level.setBlock(floor.above(2), Blocks.QUARTZ_PILLAR.defaultBlockState(), 2);
					level.setBlock(floor.above(3), Blocks.GOLD_BLOCK.defaultBlockState(), 2);
				} else if (r > FLOOR) {
					level.setBlock(floor.above(), Blocks.QUARTZ_BRICKS.defaultBlockState(), 2);
				} else if (r <= BED) {
					level.setBlock(floor.above(), flame, 2);
					if (dx == 0 && dz == 0) level.setBlock(floor.above(2), flame, 2);
				}
			}
		}
		return new BlockPos(x, y0, z);
	}

	/** A Chaos Fiend come into the First Flame's light: it burns away, white fire and smoke, nothing of it left. */
	static void burnAway(ServerLevel level, LivingEntity fiend) {
		level.sendParticles(GOLD, fiend.getX(), fiend.getY() + fiend.getBbHeight() * 0.5, fiend.getZ(), 30, 0.5, 0.8, 0.5, 0.02);
		level.sendParticles(ParticleTypes.END_ROD, fiend.getX(), fiend.getY() + fiend.getBbHeight() * 0.5, fiend.getZ(), 20, 0.4, 0.7, 0.4, 0.06);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, fiend.getX(), fiend.getY() + fiend.getBbHeight() * 0.5, fiend.getZ(), 16, 0.4, 0.7, 0.4, 0.02);
		level.playSound(null, fiend.getX(), fiend.getY(), fiend.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2f, 0.6f);
		fiend.discard();
	}

	/** Whether the basin is laid in this world. */
	public static boolean laid(MinecraftServer server) {
		return Data.get(server).laid;
	}

	/** Whether the basin has been laid (once in a world). */
	public static final class Data extends SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_first_flame";
		boolean laid;

		public static Data get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, NAME);
		}

		static Data load(CompoundTag tag) {
			Data data = new Data();
			data.laid = tag.getBoolean("Laid");
			return data;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			tag.putBoolean("Laid", laid);
			return tag;
		}
	}

	private FirstFlame() {}
}
