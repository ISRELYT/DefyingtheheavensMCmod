package com.example.defyingtheheavens;

import com.example.defyingtheheavens.mixin.ServerPlayerAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * The Soul River (魂河): the way between the Nether and the Netherworld, in its own passage dimension. A glowing river of souls
 * that carries a traveller for {@link SoulRiverCourse#TICKS} (60 s) either way: up from the Nether rift to the Ghost Gate, or
 * back down from the way down (or a fall into the abyss) to the Nether. It does them no harm, but it bends (see
 * {@link SoulRiverCourse}) and the current pushes straight on, so they must steer to keep to its flow: straying from it for
 * more than {@link #GRACE_TICKS} (or by more than {@link #HARD_MARGIN} past its edge) and their soul scatters ({@link #scatter}).
 * <p>
 * The client steers (see ClientSoulRiver); the server keeps the time, measures how far from the river the traveller is, and
 * decides where they come out. Flying and gliding are cancelled on the river, and a hidden Levitation lift keeps servers from
 * kicking the traveller for floating. The journey under way is saved (NetherworldState), so a relog starts it over from the
 * beginning; dying ends it.
 */
public final class SoulRiver {
	public enum Route {
		NONE, ASCEND, DESCEND;

		public static Route byName(String name) {
			for (Route route : values()) {
				if (route.name().equals(name)) return route;
			}
			return NONE;
		}
	}

	/** Out of the flow this long and the soul scatters: three quarters of a second to get back. */
	public static final int GRACE_TICKS = 15;
	/** This far past the river's edge, it scatters at once. */
	public static final double HARD_MARGIN = 3.0;
	/** The first second on the river isn't judged: the client is still arriving. */
	private static final int SETTLE_TICKS = 20;
	/**
	 * A scattered soul comes back together with this share of its body's health gone (never the last of it): a Nascent Soul
	 * Early's 65 down to 26, before armour (it ignores armour).
	 */
	public static final float SCATTER_HARM = 0.6f;
	/** ...dazed (nausea and weakness this long, slowness a little less, a moment's blindness)... */
	private static final int DAZE_TICKS = 12 * 20;
	/** ...and the rifts turn it away this long. */
	public static final int SCATTER_COOLDOWN_TICKS = 60 * 20;
	/** After any journey down, the rifts let the traveller be this long, so nobody is swept straight back up. */
	public static final int COOLDOWN_TICKS = 10 * 20;
	/** The hidden lift's amplifier: an otherwise unused one, so a real Levitation is never mistaken for it. */
	private static final int LIFT_AMPLIFIER = 63;

	private static final class Journey {
		final Route route;
		int tick;
		int outside;

		Journey(Route route) {
			this.route = route;
		}
	}

	private static final Map<UUID, Journey> ACTIVE = new HashMap<>();

	public static boolean isTravelling(UUID id) {
		return ACTIVE.containsKey(id);
	}

	/** Called by the Nether's rift: up the river to the Netherworld. */
	public static void beginAscent(ServerPlayer player) {
		if (!canEnter(player, true)) return;
		enter(player, Route.ASCEND, player.getBlockX(), player.getBlockZ());
		player.sendSystemMessage(Component.translatable(ModLang.MSG_RIVER_ASCEND, SoulRiverCourse.TICKS / 20).withStyle(ChatFormatting.AQUA));
	}

	/**
	 * Down the river to the Nether: by the way down on the Ghost Gate's plaza ({@code fromRift}), or by falling into the abyss,
	 * which takes anyone, whatever their cooldown (nobody is left to fall out of the world).
	 */
	public static void beginDescent(ServerPlayer player, boolean fromRift) {
		if (!canEnter(player, fromRift)) return;
		// The way down comes out by the Nether's rift, where the way up went in; a fall, under where they fell.
		enter(player, Route.DESCEND, fromRift ? SpatialRiftBlock.RIFT_X : player.getBlockX(), fromRift ? SpatialRiftBlock.RIFT_Z : player.getBlockZ());
		player.sendSystemMessage(Component.translatable(fromRift ? ModLang.MSG_RIVER_DESCEND : ModLang.MSG_RIVER_ABYSS,
				SoulRiverCourse.TICKS / 20).withStyle(ChatFormatting.AQUA));
	}

	private static boolean canEnter(ServerPlayer player, boolean checkCooldown) {
		if (player == null || !player.isAlive() || player.isSpectator() || player.getServer() == null) return false;
		if (SoulProjection.isActive(player.getUUID())) return false; // a soul is bound to its body's realm
		if (Incorporeal.isRuledGhost(player)) return false; // the dead take no rift: the Wheel is their way back
		if (ACTIVE.containsKey(player.getUUID()) || SpatialTrialHandler.isInTrial(player.getUUID())) return false;
		if (checkCooldown) {
			long left = CultivationManager.get(player).netherworld().riverCooldownUntil() - player.level().getGameTime();
			if (left > 0) {
				player.displayClientMessage(Component.translatable(ModLang.MSG_RIVER_COOLDOWN, (left + 19) / 20), true);
				return false;
			}
		}
		return true;
	}

	/** Onto the river, at the start of the route's journey. {@code fromX}, {@code fromZ}: where a journey down came out of the land. */
	private static void enter(ServerPlayer player, Route route, int fromX, int fromZ) {
		ServerLevel river = player.getServer().getLevel(ModDimensions.SOUL_RIVER);
		TribulationManager.abandon(player); // no-op without one
		if (river == null) {
			DefyingTheHeavens.LOGGER.error("Soul River dimension is missing; cannot carry {} {}", player.getScoreboardName(), route);
			return;
		}
		NetherworldState s = CultivationManager.get(player).netherworld();
		s.setRiver(route, fromX, fromZ);
		CultivationManager.markDirty(player.server);
		MeditationManager.stop(player, false);
		start(player, river, route);
		RealmSuppressionHandler.update(player); // the river is a passage: suppression lifts at once
		player.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.7f);
	}

	/** Places the traveller at the start of the journey, facing downstream, and starts the clock. */
	private static void start(ServerPlayer player, ServerLevel river, Route route) {
		boolean up = route == Route.ASCEND;
		Vec3 at = SoulRiverCourse.centre(SoulRiverCourse.startX(up));
		ACTIVE.put(player.getUUID(), new Journey(route));
		player.teleportTo(river, at.x, at.y - player.getBbHeight() / 2, at.z, up ? -90.0f : 90.0f, 0.0f);
		player.connection.resetPosition();
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0;
		ModPackets.sendSoulRiver(player, route, 0);
	}

	public static void tick(MinecraftServer server) {
		if (server == null) return;
		ServerLevel river = server.getLevel(ModDimensions.SOUL_RIVER);
		// Journeys of players who logged out (the route stays saved) or left the river some other way.
		for (Iterator<Map.Entry<UUID, Journey>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Journey> entry = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
			if (p == null) {
				it.remove();
			} else if (river == null || p.level() != river) {
				it.remove();
				JudgeEye.forget(p);
				CultivationManager.get(p).netherworld().setRiver(Route.NONE, 0, 0);
				CultivationManager.markDirty(server);
				removeLift(p);
				ModPackets.sendSoulRiver(p, Route.NONE, 0);
			}
		}
		if (river == null) return;
		for (ServerPlayer p : new ArrayList<>(river.players())) {
			try {
				tickPlayer(server, river, p);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Soul River tick failed for {}", p.getScoreboardName(), e);
			}
		}
	}

	private static void tickPlayer(MinecraftServer server, ServerLevel river, ServerPlayer p) {
		if (!p.isAlive() || p.isSpectator()) return;
		Journey j = ACTIVE.get(p.getUUID());
		if (j == null) {
			// Relogged on the river, or sent here by a command: the journey starts over.
			NetherworldState s = CultivationManager.get(p).netherworld();
			Route route = s.river() == Route.NONE ? Route.ASCEND : s.river();
			s.setRiver(route, s.riverFromX(), s.riverFromZ());
			CultivationManager.markDirty(server);
			start(p, river, route);
			RealmSuppressionHandler.update(p);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_RIVER_RESTART, SoulRiverCourse.TICKS / 20).withStyle(ChatFormatting.AQUA));
			return;
		}
		// Vanilla shields a joining player for 3 s; nothing on the river hurts, but nothing should hide behind that either.
		((ServerPlayerAccessor) p).dth$setSpawnInvulnerableTime(0);
		j.tick++;
		keepCarried(p);
		JudgeEye.tick(p); // the Chief Judge's eye, for anyone who looks at it
		if (!p.isAlive()) return; // it unmade them; AFTER_DEATH cleans up

		boolean up = j.route == Route.ASCEND;
		double progress = SoulRiverCourse.progress(p.getX(), up);
		double distance = SoulRiverCourse.distanceFromCentre(p.position().add(0, p.getBbHeight() / 2, 0));
		double radius = SoulRiverCourse.radius(progress);
		j.outside = distance > radius ? j.outside + 1 : 0;
		if (j.tick > SETTLE_TICKS && (j.outside > GRACE_TICKS || distance > radius + HARD_MARGIN)) {
			scatter(server, p, j.route);
			return;
		}
		if (j.tick >= SoulRiverCourse.TICKS || progress >= 1.0) arrive(server, p, j.route);
	}

	/** No flying or gliding on the river, no fall to count, and the hidden lift kept on. */
	private static void keepCarried(ServerPlayer p) {
		if (p.getAbilities().flying) {
			p.getAbilities().flying = false;
			p.onUpdateAbilities();
		}
		if (p.isFallFlying()) p.stopFallFlying();
		p.fallDistance = 0;
		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (levitation != null && isLift(levitation) && levitation.getDuration() >= 20) return;
		if (levitation != null && !isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
		p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, LIFT_AMPLIFIER, false, false, false));
		((ServerPlayerAccessor) p).dth$setLevitationStartPos(null); // being carried isn't levitating, for the advancement
	}

	private static boolean isLift(MobEffectInstance effect) {
		return effect.getAmplifier() == LIFT_AMPLIFIER && !effect.isVisible() && !effect.showIcon();
	}

	private static void removeLift(ServerPlayer p) {
		MobEffectInstance levitation = p.getEffect(MobEffects.LEVITATION);
		if (levitation != null && isLift(levitation)) p.removeEffect(MobEffects.LEVITATION);
	}

	/** The journey's end: the Ghost Gate going up, the Nether where they set out from going down. */
	private static void arrive(MinecraftServer server, ServerPlayer p, Route route) {
		NetherworldState s = finish(server, p);
		if (route == Route.ASCEND) {
			ServerLevel netherworld = server.getLevel(ModDimensions.NETHERWORLD);
			if (netherworld == null) {
				DefyingTheHeavens.LOGGER.error("Netherworld dimension is missing; returning {} to the Nether instead", p.getScoreboardName());
				NetherRift.land(server, p, SpatialRiftBlock.RIFT_X, SpatialRiftBlock.RIFT_Z);
				return;
			}
			BlockPos at = GhostGate.at(netherworld, GhostGate.ARRIVAL);
			p.teleportTo(netherworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0f, 0.0f); // facing the gate
			settle(p);
			p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 0.8f);
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_NETHERWORLD, (int) GhostQi.NETHERWORLD).withStyle(ChatFormatting.DARK_AQUA));
		} else {
			s.setRiverCooldownUntil(p.level().getGameTime() + COOLDOWN_TICKS);
			NetherRift.land(server, p, s.riverFromX(), s.riverFromZ());
			p.sendSystemMessage(Component.translatable(ModLang.MSG_ARRIVE_NETHER));
		}
		CultivationManager.markDirty(server);
	}

	/**
	 * Strayed from the flow: the soul scatters. It comes back together in the Nether, by the rift going up (the ascent fails) or
	 * where the journey set out from going down (spat out early), badly hurt and dazed, and the rifts turn it away for a minute.
	 */
	private static void scatter(MinecraftServer server, ServerPlayer p, Route route) {
		// Hurt here, before the move: right after crossing between dimensions vanilla shields a player from harm.
		float harm = Math.min(p.getHealth() - 1.0f, p.getMaxHealth() * SCATTER_HARM);
		if (harm > 0) p.hurt(ModDamageTypes.soulScatter(p.level()), harm);
		if (!p.isAlive()) return; // AFTER_DEATH cleans up
		p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DAZE_TICKS));
		p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DAZE_TICKS));
		p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAZE_TICKS * 3 / 4, 1));
		p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30));
		NetherworldState s = finish(server, p);
		s.setRiverCooldownUntil(p.level().getGameTime() + SCATTER_COOLDOWN_TICKS);
		if (route == Route.ASCEND) NetherRift.land(server, p, SpatialRiftBlock.RIFT_X, SpatialRiftBlock.RIFT_Z);
		else NetherRift.land(server, p, s.riverFromX(), s.riverFromZ());
		p.playNotifySound(SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
		p.sendSystemMessage(Component.translatable(route == Route.ASCEND ? ModLang.MSG_RIVER_SCATTERED_UP : ModLang.MSG_RIVER_SCATTERED_DOWN)
				.withStyle(ChatFormatting.RED));
		CultivationManager.markDirty(server);
	}

	/** Ends the journey's bookkeeping (the route, the lift, the client's river); returns the traveller's Netherworld state. */
	private static NetherworldState finish(MinecraftServer server, ServerPlayer p) {
		ACTIVE.remove(p.getUUID());
		removeLift(p);
		JudgeEye.forget(p);
		NetherworldState s = CultivationManager.get(p).netherworld();
		s.setRiver(Route.NONE, s.riverFromX(), s.riverFromZ());
		ModPackets.sendSoulRiver(p, Route.NONE, 0);
		return s;
	}

	/** After a move off the river: no stale motion or fall, and the realm's rules applied. */
	static void settle(ServerPlayer p) {
		p.connection.resetPosition();
		p.setDeltaMovement(Vec3.ZERO);
		p.fallDistance = 0;
		RealmSuppressionHandler.update(p);
	}

	/** Dying ends the journey. */
	public static void onDeath(ServerPlayer player) {
		if (player == null) return;
		JudgeEye.forget(player);
		ModPackets.sendSoulRiver(player, Route.NONE, 0);
		NetherworldState s = CultivationManager.get(player).netherworld();
		if (ACTIVE.remove(player.getUUID()) != null || s.river() != Route.NONE) {
			s.setRiver(Route.NONE, 0, 0);
			CultivationManager.markDirty(player.server);
		}
	}

	/** Disconnect: forget the clock; the saved route starts the journey over on the next login. */
	public static void forget(UUID id) {
		ACTIVE.remove(id);
		JudgeEye.forget(id);
	}

	private SoulRiver() {}
}
