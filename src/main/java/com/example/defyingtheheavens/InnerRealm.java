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
 * body stays sitting where they meditated ({@link InnerBodyEntity}). The soul can walk the island freely (qi surges there are
 * played on foot, see QiSurges); stopping meditation, or anything touching the body, brings it back. Meditation inside is
 * {@link #CULTIVATION_BONUS} times as fruitful. The island is Soul Crystal, see-through so the leylines show beneath it.
 * <p>
 * Where the soul came from is kept in {@link PlayerCultivation} (saved), so logging out, the server stopping, or a crash
 * always end with the player back at their body. Every player's island lies in a slot of its own ({@link Islands}).
 */
public final class InnerRealm {
	/** Meditating this long (ticks) turns the soul inward, if the Inner Realm ability is on. */
	public static final int ENTER_AFTER_TICKS = 200;
	/** The screen fades to black over this long before the soul turns inward (see the client's InnerRealmFade). */
	public static final int FADE_TICKS = 30;
	/** A soul that walks off the island's edge is caught this far below it and set back on the seat. */
	private static final int FALL_CATCH_DEPTH = 24;
	public static final double CULTIVATION_BONUS = 1.5;
	public static final int ISLAND_Y = 128;
	/** Each player's island has its own spot, this far apart. */
	public static final int SLOT_SPACING = 4096;
	/** Islands in a row along X before the next row begins along Z: a compact grid, near the origin for the first players. */
	private static final int SLOTS_PER_ROW = 64;
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
		return islandCentre(player.server, player.getUUID());
	}

	/**
	 * Each player's island has a slot of its own, handed out in turn the first time they need one and kept for good (see
	 * {@link Islands}), so no two players ever share one. Slots fill a grid from the origin, {@link #SLOTS_PER_ROW} to a row.
	 */
	public static BlockPos islandCentre(MinecraftServer server, java.util.UUID player) {
		int slot = Islands.get(server).slotOf(player);
		return new BlockPos((slot % SLOTS_PER_ROW) * SLOT_SPACING, ISLAND_Y, (slot / SLOTS_PER_ROW) * SLOT_SPACING);
	}

	/** Who has which island slot, saved with the world (in the Overworld's data). */
	public static final class Islands extends net.minecraft.world.level.saveddata.SavedData {
		private static final String NAME = DefyingTheHeavens.MOD_ID + "_inner_islands";
		private final java.util.Map<java.util.UUID, Integer> slots = new java.util.HashMap<>();
		private int next;

		public static Islands get(MinecraftServer server) {
			return server.overworld().getDataStorage().computeIfAbsent(Islands::load, Islands::new, NAME);
		}

		/** The player's slot, handing them the next free one if they have none yet. */
		public int slotOf(java.util.UUID player) {
			Integer slot = slots.get(player);
			if (slot == null) {
				slot = next++;
				slots.put(player, slot);
				setDirty();
			}
			return slot;
		}

		@Override
		public CompoundTag save(CompoundTag tag) {
			net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
			for (java.util.Map.Entry<java.util.UUID, Integer> entry : slots.entrySet()) {
				CompoundTag one = new CompoundTag();
				one.putUUID("Player", entry.getKey());
				one.putInt("Slot", entry.getValue());
				list.add(one);
			}
			tag.put("Islands", list);
			tag.putInt("Next", next);
			return tag;
		}

		public static Islands load(CompoundTag tag) {
			Islands islands = new Islands();
			for (net.minecraft.nbt.Tag entry : tag.getList("Islands", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
				CompoundTag one = (CompoundTag) entry;
				if (one.hasUUID("Player")) islands.slots.put(one.getUUID("Player"), one.getInt("Slot"));
			}
			islands.next = Math.max(tag.getInt("Next"), islands.slots.values().stream().mapToInt(s -> s + 1).max().orElse(0));
			return islands;
		}
	}

	/** The island's radius in blocks, which grows with the realm. */
	public static int islandRadius(Realm realm) {
		return 3 + realm.ordinal();
	}

	/** No digging or building in the Inner Realm (creative players aside). */
	public static void registerEvents() {
		net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
				!ModDimensions.isInnerRealm(level.dimension()) || player.isCreative());
		net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				ModDimensions.isInnerRealm(level.dimension()) && !player.isCreative()
						? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				ModDimensions.isInnerRealm(level.dimension()) && !player.isCreative()
						? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
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
			} else if (isInside(player) && player.getY() < ISLAND_Y - FALL_CATCH_DEPTH) {
				// Walked off the edge: the soul drifts back to its seat.
				BlockPos centre = islandCentre(player);
				player.teleportTo(centre.getX() + 0.5, centre.getY() + 1, centre.getZ() + 0.5);
				player.setDeltaMovement(Vec3.ZERO);
				player.fallDistance = 0;
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

	/**
	 * The island grows with the realm: a disc of Soul Crystal with a shallow keel beneath, see-through so the leylines and
	 * the core show below. Its colour follows the realm too, dark and smoky at first, pale and celestial at the top (the
	 * client tints it, see SoulCrystalBlock).
	 */
	static void buildIsland(ServerLevel level, BlockPos centre, Realm realm) {
		BlockState crystal = ModBlocks.SOUL_CRYSTAL.defaultBlockState();
		int radius = islandRadius(realm);
		int clear = 3 + Realm.values().length;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -clear; dx <= clear; dx++) {
			for (int dz = -clear; dz <= clear; dz++) {
				for (int dy = -clear; dy <= 4; dy++) {
					pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
					double d = Math.sqrt(dx * dx + dz * dz);
					BlockState state = Blocks.AIR.defaultBlockState();
					if (dy == 0 && d <= radius + 0.3) state = crystal;
					else if (dy < 0 && dy >= -2 && d <= radius + 0.3 + dy * (radius / 3.0)) state = crystal;
					level.setBlock(pos, state, 2);
				}
			}
		}
	}

	private static ResourceKey<Level> dimension(CompoundTag back) {
		ResourceLocation id = ResourceLocation.tryParse(back.getString("Dimension"));
		return id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
	}

	private InnerRealm() {}
}
