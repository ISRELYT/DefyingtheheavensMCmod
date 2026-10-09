package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The 60-second Spatial Pressure Trial in the Spatial Gap, for both directions:
 * ascension (Overworld rift -> Upper Realm) and descension (Upper Realm void -> Overworld sky).
 * <p>
 * The route alone decides which way the gap carries the player: ascending, they are lifted upward the whole way;
 * descending, they fall. Flying (creative or any other kind) and elytra gliding are cancelled inside the gap, and a
 * descending player loses any Levitation, so nobody can hover or go against the current.
 * <p>
 * The running timer is runtime-only and restarts from 60 s if the player relogs inside the gap; which direction they
 * were travelling is persisted in their cultivation data. Dying in the gap ends the trial.
 */
public final class SpatialTrialHandler {
	public enum Route {
		NONE, ASCEND, DESCEND;

		public static Route byName(String name) {
			for (Route route : values()) {
				if (route.name().equals(name)) return route;
			}
			return NONE;
		}
	}

	public static final int TRIAL_TICKS = 60 * 20;
	/**
	 * Per second; ignores armor, enchantments and effects (see ModDamageTypes). Health, regen, absorption and healing count.
	 * Against Qi Sustenance's 2 health/s regen (the ability switched on; off, the gap is harder), 60 hits net 131 damage: Heavenly Being - Grand Perfection (137 max
	 * health), the peak of the lower realms, is the first stage that endures it unaided; Late (128) falls at ~59 s.
	 * Four Axis (200+) passes with room to spare. A golden apple or two carries a Late cultivator through.
	 */
	public static final float PRESSURE_DAMAGE = 4.15f;

	private static final int ASCENT_ENTRY_Y = 96;
	private static final int DESCENT_ENTRY_Y = 300;
	private static final int OVERWORLD_DROP_Y = 350;
	/** The gap is floorless: below this Y the player is lifted back up mid-fall, keeping their speed, so they never hit the void. */
	private static final int LOOP_BELOW_Y = 64;
	/** ...and has no ceiling: above this Y a rising player is dropped back down mid-climb, keeping their speed. */
	private static final int LOOP_ABOVE_Y = 320;
	/** How far the loop moves a player; the gap repeats every this many blocks (see {@link SpatialStorms}). */
	public static final int LOOP_LIFT = 192;
	/**
	 * Ascending, the gap carries the player up with a hidden Levitation effect of this amplifier (a steady ~1.8 blocks per
	 * tick). Levitation is how the client is made to rise, and servers don't kick a levitating player for "flying".
	 */
	private static final int ASCENT_LIFT_AMPLIFIER = 39;
	/** Where to look for an island to land on around the arrival point in the Upper Realm. */
	private static final int[] ARRIVAL_SEARCH_RADII = {0, 8, 16, 24, 32, 40, 48};
	private static final int PLATFORM_Y = 256;

	private static final class Trial {
		final Route route;
		int ticksLeft = TRIAL_TICKS;

		Trial(Route route) {
			this.route = route;
		}
	}

	private static final Map<UUID, Trial> ACTIVE = new HashMap<>();

	public static boolean isInTrial(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** Called by the Spatial Rift. */
	public static void beginAscension(ServerPlayer player) {
		if (!canEnter(player)) return;
		// Someone just cast down from the Upper Realm is still falling from the sky drop; descending near X=0, Z=0
		// would otherwise drop them straight back through the rift into a new ascension.
		if (CultivationManager.get(player).isFallProtected()) return;
		enterGap(player, Route.ASCEND, player.getX(), ASCENT_ENTRY_Y, player.getZ());
	}

	/** Called by the VoidFallHandler when a player drops out of the bottom of the Upper Realm. */
	public static void beginDescension(ServerPlayer player) {
		if (!canEnter(player)) return;
		enterGap(player, Route.DESCEND, player.getX(), DESCENT_ENTRY_Y, player.getZ());
	}

	private static boolean canEnter(ServerPlayer player) {
		if (player == null || !player.isAlive() || player.isSpectator() || player.getServer() == null) return false;
		if (ACTIVE.containsKey(player.getUUID())) return false;
		if (TribulationManager.isActive(player.getUUID())) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_TRIB_BUSY), true);
			return false;
		}
		return true;
	}

	private static void enterGap(ServerPlayer player, Route route, double x, double y, double z) {
		ServerLevel gap = player.getServer().getLevel(ModDimensions.SPATIAL_GAP);
		if (gap == null) {
			DefyingTheHeavens.LOGGER.error("Spatial Gap dimension is missing; cannot start the {} trial for {}", route, player.getScoreboardName());
			return;
		}
		PlayerCultivation c = CultivationManager.get(player);
		c.setPendingTrial(route);
		CultivationManager.markDirty(player.server);
		ACTIVE.put(player.getUUID(), new Trial(route));
		MeditationManager.stop(player, false);

		player.teleportTo(gap, x, y, z, player.getYRot(), player.getXRot());
		resyncMovement(player);
		player.fallDistance = 0;
		RealmSuppressionHandler.update(player); // the gap lifts suppression immediately

		player.playNotifySound(SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 0.6f);
		player.sendSystemMessage(Component.translatable(route == Route.ASCEND ? ModLang.MSG_GAP_ASCEND : ModLang.MSG_GAP_DESCEND,
				TRIAL_TICKS / 20));
	}

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		ServerLevel gap = server.getLevel(ModDimensions.SPATIAL_GAP);

		// Drop trials for players who logged out (the route stays saved) or left the gap by other means.
		for (Iterator<Map.Entry<UUID, Trial>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Trial> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				it.remove();
			} else if (gap == null || p.level() != gap) {
				it.remove();
				CultivationManager.get(p).setPendingTrial(Route.NONE);
				CultivationManager.markDirty(server);
				removeLift(p);
			}
		}

		if (gap != null) {
			for (ServerPlayer p : new ArrayList<>(gap.players())) {
				try {
					tickPlayer(server, gap, p);
				} catch (Exception e) {
					DefyingTheHeavens.LOGGER.error("Spatial trial tick failed for {}", p.getScoreboardName(), e);
				}
			}
		}

		tickFallProtection(server);
	}

	private static void tickPlayer(MinecraftServer server, ServerLevel gap, ServerPlayer p) {
		if (!p.isAlive()) return;
		Trial t = ACTIVE.get(p.getUUID());
		if (t == null) {
			// Relogged inside the gap, or sent here by a command: the timer starts over.
			PlayerCultivation c = CultivationManager.get(p);
			Route route = c.getPendingTrial() == Route.NONE ? Route.ASCEND : c.getPendingTrial();
			c.setPendingTrial(route);
			CultivationManager.markDirty(server);
			t = new Trial(route);
			ACTIVE.put(p.getUUID(), t);
			RealmSuppressionHandler.update(p);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_GAP_RESTART, TRIAL_TICKS / 20));
		}

		// Vanilla shields a player from damage for 3 s after joining while health keeps regenerating, so relogging over
		// and over would let anyone outlast the gap. The pressure must never pause.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);

		t.ticksLeft--;
		forceDirection(p, t.route);

		// All axes relative: the client keeps its speed through the loop.
		if (p.getY() < LOOP_BELOW_Y) {
			p.connection.teleport(p.getX(), p.getY() + LOOP_LIFT, p.getZ(), p.getYRot(), p.getXRot(), EnumSet.allOf(RelativeMovement.class));
			resyncMovement(p);
		} else if (p.getY() > LOOP_ABOVE_Y) {
			p.connection.teleport(p.getX(), p.getY() - LOOP_LIFT, p.getZ(), p.getYRot(), p.getXRot(), EnumSet.allOf(RelativeMovement.class));
			resyncMovement(p);
		}

		if (t.ticksLeft % 20 == 0) {
			p.hurt(ModDamageTypes.spatialPressure(gap), PRESSURE_DAMAGE);
			if (!p.isAlive()) return; // AFTER_DEATH -> onDeath() cleans up
			p.displayClientMessage(Component.translatable(ModLang.MSG_GAP_TIMER, Math.max(0, t.ticksLeft / 20)), true);
		}

		if (t.ticksLeft <= 0) {
			complete(server, p, t.route);
		}
	}

	/**
	 * Makes the route decide the direction: no flying (creative or any other kind) and no elytra gliding in the gap;
	 * ascending, the hidden Levitation lift is kept topped up; descending, any Levitation (a potion, say) is removed.
	 */
	private static void forceDirection(ServerPlayer p, Route route) {
		if (p.isSpectator()) return;
		if (p.getAbilities().flying) {
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
		}
		if (p.isFallFlying()) p.stopFallFlying();

		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (route == Route.ASCEND) {
			if (levitation != null && isLift(levitation) && levitation.getDuration() >= 20) return;
			if (levitation != null && !isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
			p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, ASCENT_LIFT_AMPLIFIER, false, false, false));
			// Being carried up isn't levitating: keep vanilla from counting it toward "Great View From Up Here".
			((ServerPlayerAccessor) p).dth$setLevitationStartPos(null);
		} else if (levitation != null) {
			p.removeEffect(MobEffects.LEVITATION);
		}
	}

	/** The ascent lift: our amplifier, no particles, no icon. */
	private static boolean isLift(MobEffectInstance effect) {
		return effect.getAmplifier() == ASCENT_LIFT_AMPLIFIER && !effect.isVisible() && !effect.showIcon();
	}

	/** Takes the ascent lift off on arrival, or when the player leaves the gap some other way. */
	private static void removeLift(ServerPlayer p) {
		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (levitation != null && isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
	}

	private static void complete(MinecraftServer server, ServerPlayer p, Route route) {
		ACTIVE.remove(p.getUUID());
		removeLift(p);
		PlayerCultivation c = CultivationManager.get(p);
		c.setPendingTrial(Route.NONE);

		if (route == Route.ASCEND) {
			ServerLevel upper = server.getLevel(ModDimensions.UPPER_REALM);
			if (upper == null) {
				DefyingTheHeavens.LOGGER.error("Upper Realm dimension is missing; returning {} to the Overworld instead", p.getScoreboardName());
				dropIntoOverworld(server, p, c);
				return;
			}
			BlockPos arrival = findArrival(upper, p.getBlockX(), p.getBlockZ());
			p.teleportTo(upper, arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, p.getYRot(), p.getXRot());
			resyncMovement(p);
			p.setDeltaMovement(Vec3.ZERO);
			p.fallDistance = 0;
			RealmSuppressionHandler.update(p);
			p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_UPPER, (int) PlayerCultivation.UPPER_REALM_QI_MULTIPLIER));
		} else {
			dropIntoOverworld(server, p, c);
		}
		CultivationManager.markDirty(server);
	}

	private static void dropIntoOverworld(MinecraftServer server, ServerPlayer p, PlayerCultivation c) {
		ServerLevel overworld = server.overworld();
		if (overworld == null) return;
		p.teleportTo(overworld, p.getX(), OVERWORLD_DROP_Y, p.getZ(), p.getYRot(), p.getXRot());
		resyncMovement(p);
		p.fallDistance = 0;
		p.setOnGround(false); // nothing stale from the gap may count as a landing (see tickFallProtection)
		c.setFallProtected(true); // the first landing from the sky drop is harmless
		RealmSuppressionHandler.update(p);
		p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_LOWER));
	}

	/** Nearest solid, dry island surface around (x, z); builds a small platform if the abyss is empty there. */
	private static BlockPos findArrival(ServerLevel level, int x, int z) {
		try {
			for (int radius : ARRIVAL_SEARCH_RADII) {
				for (int i = 0; i < (radius == 0 ? 1 : 8); i++) {
					double angle = Math.PI * 2 * i / 8;
					int cx = x + (int) Math.round(Math.cos(angle) * radius);
					int cz = z + (int) Math.round(Math.sin(angle) * radius);
					level.getChunk(cx >> 4, cz >> 4);
					int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
					if (top <= level.getMinBuildHeight() + 1 || top >= level.getMaxBuildHeight() - 2) continue;
					BlockPos ground = new BlockPos(cx, top - 1, cz);
					BlockState state = level.getBlockState(ground);
					if (state.getFluidState().isEmpty() && state.isFaceSturdy(level, ground, Direction.UP)) {
						return ground.above();
					}
				}
			}
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Arrival search in the Upper Realm failed around {}, {}", x, z, e);
		}
		return buildPlatform(level, x, z);
	}

	private static BlockPos buildPlatform(ServerLevel level, int x, int z) {
		BlockPos center = new BlockPos(x, PLATFORM_Y, z);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				level.setBlock(center.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
				for (int dy = 0; dy < 3; dy++) {
					level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
				}
			}
		}
		DefyingTheHeavens.LOGGER.info("No island near the arrival point; built a landing platform at {}", center.toShortString());
		return center;
	}

	/**
	 * Clears the sky-drop fall protection once the player has landed (or is no longer falling into the Overworld).
	 * On ground alone is not enough: the server's own collision marks the player as grounded a tick or more before the
	 * client's landing packet applies the fall damage, so on a dedicated server the protection would be gone by then.
	 * A landing only counts once the fall itself is resolved (fall distance back to 0); a damaging one consumes the
	 * protection in {@link #consumeFallProtection} first.
	 */
	private static void tickFallProtection(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			PlayerCultivation c = CultivationManager.get(p);
			if (!c.isFallProtected()) continue;
			boolean landed = (p.onGround() && p.fallDistance <= 0) || p.isInWater() || p.getAbilities().flying || !p.isAlive()
					|| p.level().dimension() != Level.OVERWORLD;
			if (landed) {
				c.setFallProtected(false);
				CultivationManager.markDirty(server);
			}
		}
	}

	/**
	 * Re-baselines vanilla's movement check after the server moves a player. Otherwise, on a dedicated server, the client's
	 * first packets from the new spot are measured against the old one ("moved too quickly!") and rubber-banded back.
	 * Singleplayer hosts are exempt from that check, which is why this only shows on servers.
	 */
	private static void resyncMovement(ServerPlayer p) {
		p.connection.resetPosition();
	}

	/** Returns true if this fall damage should be cancelled (consumes the protection). */
	public static boolean consumeFallProtection(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		if (!c.isFallProtected()) return false;
		c.setFallProtected(false);
		CultivationManager.markDirty(player.server);
		return true;
	}

	/** Dying in the gap ends the trial; the player respawns at their spawn point as usual. */
	public static void onDeath(ServerPlayer player) {
		if (player == null) return;
		if (ACTIVE.remove(player.getUUID()) != null || CultivationManager.get(player).getPendingTrial() != Route.NONE) {
			CultivationManager.get(player).setPendingTrial(Route.NONE);
			CultivationManager.markDirty(player.server);
		}
	}

	/** Disconnect: forget the running timer; the saved route restarts a full trial on the next login. */
	public static void forget(UUID id) {
		ACTIVE.remove(id);
	}

	private SpatialTrialHandler() {}
}
