package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Inner Realm: after meditating a while, a cultivator's soul turns inward to a private island in a void, under a sky
 * that grows from near-black into something celestial as they advance (drawn by the client's InnerRealmSkyRenderer). The
 * body stays sitting where they meditated ({@link InnerBodyEntity}); getting up, or anything touching the body, brings the
 * soul back. Meditation inside is {@link #CULTIVATION_BONUS} times as fruitful.
 * <p>
 * Where the soul came from is kept in {@link PlayerCultivation} (saved), so logging out, the server stopping, or a crash
 * always end with the player back at their body.
 */
public final class InnerRealm {
	/** Meditating this long (ticks) turns the soul inward, if the Inner Realm ability is on. */
	public static final int ENTER_AFTER_TICKS = 200;
	public static final double CULTIVATION_BONUS = 1.5;
	public static final int ISLAND_Y = 128;
	/** Each player's island has its own spot along X, this far apart. */
	public static final int SLOT_SPACING = 4096;
	private static final int SLOTS = 2048;
	/** Chunks kept loaded around the body (radius), so whatever is near it keeps moving. */
	private static final int BODY_TICKET_RADIUS = 2;
	/** Hostile mobs this close to a body notice it. */
	private static final double BODY_NOTICE_RANGE = 16;
	private static final TicketType<ChunkPos> BODY_TICKET = TicketType.create("defying-the-heavens:inner_body",
			Comparator.comparingLong(ChunkPos::toLong));

	public static boolean isInside(ServerPlayer player) {
		return ModDimensions.isInnerRealm(player.level().dimension()) && CultivationManager.get(player).getInnerReturn() != null;
	}

	/** The dimension the player's body is in: where they are, unless their soul is in the Inner Realm. */
	public static ResourceKey<Level> bodyDimension(ServerPlayer player) {
		if (!ModDimensions.isInnerRealm(player.level().dimension())) return player.level().dimension();
		CompoundTag back = CultivationManager.get(player).getInnerReturn();
		return back == null ? Level.OVERWORLD : dimension(back);
	}

	/** The player's island centre (the top block they sit on). */
	public static BlockPos islandCentre(ServerPlayer player) {
		return new BlockPos(Math.floorMod(player.getUUID().hashCode(), SLOTS) * SLOT_SPACING, ISLAND_Y, 0);
	}

	public static boolean canEnter(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		return c.isAbilityActive(Ability.INNER_REALM) && !ModDimensions.isInnerRealm(player.level().dimension())
				&& !ModDimensions.isSpatialGap(player.level().dimension()) && !TribulationManager.isActive(player.getUUID())
				&& player.isAlive() && player.server.getLevel(ModDimensions.INNER_REALM) != null;
	}

	/** Leaves the body sitting here and takes the soul to the island. Returns false if it couldn't. */
	public static boolean enter(ServerPlayer player) {
		if (!canEnter(player)) return false;
		ServerLevel inner = player.server.getLevel(ModDimensions.INNER_REALM);
		ServerLevel here = player.serverLevel();
		PlayerCultivation c = CultivationManager.get(player);

		InnerBodyEntity body = new InnerBodyEntity(ModEntities.INNER_BODY, here);
		body.setOwner(player);
		body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
		body.setYBodyRot(player.getYRot());
		body.setYHeadRot(player.getYRot());
		here.addFreshEntity(body);
		ChunkPos chunk = new ChunkPos(player.blockPosition());
		here.getChunkSource().addRegionTicket(BODY_TICKET, chunk, BODY_TICKET_RADIUS, chunk);

		CompoundTag back = new CompoundTag();
		back.putString("Dimension", here.dimension().location().toString());
		back.putDouble("X", player.getX());
		back.putDouble("Y", player.getY());
		back.putDouble("Z", player.getZ());
		back.putFloat("YRot", player.getYRot());
		back.putFloat("XRot", player.getXRot());
		back.putUUID("Body", body.getUUID());
		c.setInnerReturn(back);
		CultivationManager.markDirty(player.server);

		BlockPos centre = islandCentre(player);
		buildIsland(inner, centre, c.getRealm());
		player.teleportTo(inner, centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5, player.getYRot(), 35.0f);
		inner.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);
		player.displayClientMessage(Component.translatable(ModLang.MSG_INNER_ENTER), true);
		return true;
	}

	/** Brings the soul back to the body (removing it). Safe to call when the player isn't inside. */
	public static void leave(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		CompoundTag back = c.getInnerReturn();
		if (back == null) {
			// Somehow in the Inner Realm with nowhere recorded to go back to (e.g. a crash mid-way): their own spawn point.
			if (ModDimensions.isInnerRealm(player.level().dimension())) {
				ServerLevel spawnLevel = player.server.getLevel(player.getRespawnDimension());
				if (spawnLevel == null) spawnLevel = player.server.overworld();
				BlockPos spawn = player.getRespawnPosition() != null ? player.getRespawnPosition() : spawnLevel.getSharedSpawnPos();
				player.teleportTo(spawnLevel, spawn.getX() + 0.5, spawn.getY() + 0.1, spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
			}
			return;
		}
		c.setInnerReturn(null);
		CultivationManager.markDirty(player.server);
		ServerLevel level = player.server.getLevel(dimension(back));
		if (level == null) level = player.server.overworld();
		Vec3 pos = new Vec3(back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z"));
		ChunkPos chunk = new ChunkPos(BlockPos.containing(pos));
		level.getChunkSource().removeRegionTicket(BODY_TICKET, chunk, BODY_TICKET_RADIUS, chunk);
		if (back.hasUUID("Body") && level.getEntity(back.getUUID("Body")) instanceof InnerBodyEntity body) body.discard();
		if (ModDimensions.isInnerRealm(player.level().dimension()) || player.level() != level) {
			player.teleportTo(level, pos.x, pos.y, pos.z, back.getFloat("YRot"), back.getFloat("XRot"));
		}
		player.fallDistance = 0;
	}

	/**
	 * Logging out (or the server stopping) with the soul inward: the body goes (it removes itself without its owner) and
	 * stops holding chunks loaded; the next join brings the player back to where it sat.
	 */
	public static void onDisconnect(ServerPlayer player) {
		CompoundTag back = CultivationManager.get(player).getInnerReturn();
		if (back == null) return;
		ServerLevel level = player.server.getLevel(dimension(back));
		if (level == null) return;
		ChunkPos chunk = new ChunkPos(BlockPos.containing(back.getDouble("X"), back.getDouble("Y"), back.getDouble("Z")));
		level.getChunkSource().removeRegionTicket(BODY_TICKET, chunk, BODY_TICKET_RADIUS, chunk);
	}

	/** Something hit the body: the soul snaps back and takes the blow. */
	public static void onBodyHurt(InnerBodyEntity body, ServerPlayer owner, DamageSource source, float amount) {
		owner.displayClientMessage(Component.translatable(ModLang.MSG_INNER_PULLED), true);
		MeditationManager.stop(owner, true); // leaves the Inner Realm
		leave(owner);
		owner.hasChangedDimension(); // just arrived: vanilla ignores damage until the client confirms, but this blow is real
		owner.hurt(source, amount);
	}

	/** The body was pushed or moved: the soul is jolted back. */
	public static void onBodyDisturbed(ServerPlayer owner) {
		owner.displayClientMessage(Component.translatable(ModLang.MSG_INNER_PULLED), true);
		MeditationManager.stop(owner, true);
		leave(owner);
	}

	/** Every tick: nobody stays inside without meditating, and hostile mobs notice an unguarded body. */
	public static void tick(MinecraftServer server) {
		boolean notice = server.getTickCount() % 20 == 0;
		List<ServerPlayer> strays = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ModDimensions.isInnerRealm(player.level().dimension()) && !MeditationManager.isMeditating(player.getUUID())) {
				strays.add(player);
			} else if (notice && isInside(player)) {
				CompoundTag back = CultivationManager.get(player).getInnerReturn();
				ServerLevel level = server.getLevel(dimension(back));
				if (level != null && back.hasUUID("Body") && level.getEntity(back.getUUID("Body")) instanceof InnerBodyEntity body) {
					AABB around = body.getBoundingBox().inflate(BODY_NOTICE_RANGE);
					for (Monster monster : level.getEntitiesOfClass(Monster.class, around, m -> m.getTarget() == null && m.isAlive())) {
						monster.setTarget(body);
					}
				}
			}
		}
		strays.forEach(InnerRealm::leave);
	}

	/** The island grows and brightens with the realm: dark stone at first, pale and celestial at the top. */
	static void buildIsland(ServerLevel level, BlockPos centre, Realm realm) {
		int tier = realm.ordinal();
		BlockState[] top = {Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
				Blocks.POLISHED_ANDESITE.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), Blocks.QUARTZ_BLOCK.defaultBlockState(),
				Blocks.WHITE_CONCRETE.defaultBlockState()};
		BlockState[] under = {Blocks.BLACKSTONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(),
				Blocks.DIORITE.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.CALCITE.defaultBlockState()};
		int t = Math.min(tier, top.length - 1);
		int radius = 3 + tier;
		int clear = 3 + Realm.values().length;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -clear; dx <= clear; dx++) {
			for (int dz = -clear; dz <= clear; dz++) {
				for (int dy = -clear; dy <= 4; dy++) {
					pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					double d = Math.sqrt(dx * dx + dz * dz);
					BlockState state = Blocks.AIR.defaultBlockState();
					if (dy == 0 && d <= radius + 0.3) state = top[t];
					else if (dy < 0 && d <= radius + 0.3 + dy * (radius / 3.0)) state = under[t];
					level.setBlock(pos, state, 2);
				}
			}
		}
		// A soft light at the seat from Core Formation on: the core's glow reaches the surface.
		if (tier >= Realm.CORE_FORMATION.ordinal()) level.setBlock(centre, Blocks.SEA_LANTERN.defaultBlockState(), 2);
	}

	private static ResourceKey<Level> dimension(CompoundTag back) {
		ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
		return id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
	}

	private InnerRealm() {}
}
