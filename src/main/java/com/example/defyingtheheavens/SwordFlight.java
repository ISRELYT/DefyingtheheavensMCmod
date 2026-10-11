package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Sword Flight, the ability of Foundation Building and above: a cultivator holding a sword in the main hand rides it through
 * the air, standing sideways on the blade, which leaves a trail of light behind it. It costs less qi than Qi Flight
 * ({@link Realm#getSwordFlightCost}) and is slower ({@link #SPEED_FACTOR}), and the sword takes no wear.
 * <p>
 * Its speed is fixed from the day it awakens: no realm, stage or gear makes the sword faster, so Qi Flight stays the way
 * to fly fast (player flight ignores the movement speed attribute; NPCs ride on their base walking speed, see
 * {@link CultivatorNpc#getFlyingSpeed}).
 * <p>
 * Players take off the way Qi Flight does (double-tap jump; {@link QiFlight} runs both). Only one of the two can be
 * switched on at a time ({@link Ability#exclusiveWith}): with Sword Flight on they need a sword in hand to fly, and
 * letting go of it mid-air drops them. NPC cultivators ride their swords too, choosing between the two as they go
 * ({@link CultivatorNpc#isSwordFlying}). The client draws the sword, the stance and the trail.
 */
public final class SwordFlight {
	/** Vanilla's flying speed, which Qi Flight keeps. */
	public static final float QI_FLIGHT_SPEED = 0.05f;
	/** A sword carries its rider at this share of Qi Flight's speed, at every realm. */
	public static final float SPEED_FACTOR = 0.75f;
	public static final float SWORD_SPEED = QI_FLIGHT_SPEED * SPEED_FACTOR;
	/** While a player rides, everyone in sight is told again this often (in ticks), so players coming into range see it too. */
	private static final int HEARTBEAT = 20;

	/** Players riding their swords right now (as last told to the clients). */
	private static final Set<UUID> RIDING = new HashSet<>();

	/** Anything that is a sword: vanilla's and modded swords alike (SwordItem, or the "swords" item tag). */
	public static boolean isSword(ItemStack stack) {
		return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.is(ItemTags.SWORDS));
	}

	/** Sword Flight awakened (by the effective realm) and switched on, with a sword in the main hand to ride. */
	public static boolean canRide(LivingEntity entity, PlayerCultivation c) {
		return Ability.SWORD_FLIGHT.isUnlocked(c) && c.isAbilityEnabled(Ability.SWORD_FLIGHT) && isSword(entity.getMainHandItem());
	}

	/**
	 * Called by {@link QiFlight} every tick with whether the player is riding their sword now: tells them and everyone
	 * tracking them when that changes, and every {@link #HEARTBEAT} ticks while it lasts.
	 */
	static void update(ServerPlayer player, boolean riding) {
		UUID id = player.getUUID();
		boolean changed = riding ? RIDING.add(id) : RIDING.remove(id);
		if (changed || riding && player.tickCount % HEARTBEAT == 0) ModPackets.broadcastSwordFlight(player, riding);
	}

	public static boolean isRiding(ServerPlayer player) { return RIDING.contains(player.getUUID()); }

	public static void forget(UUID id) {
		RIDING.remove(id);
	}

	private SwordFlight() {}
}
