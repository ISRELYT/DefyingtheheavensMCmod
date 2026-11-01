package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Judgement Hall Service (CultivationMethod#JUDGEMENT_HALL_SERVICE; the methods overhaul's phase 4): the ghost path's way from Heavenly Being to
 * Dao Integration, the Judgement Hall's errand run: a wandering soul led to the Wheel of Reincarnation. A wandering ghost (a GhostCultivatorNpc
 * of no post: CultivatorNpc#isWanderingRogue) right-clicked sneaking follows the one who leads it ({@link #MOST_LED} at once; again, it is let
 * go), walking after it, drifting after it when it falls behind ({@link EscortedGoal}); brought within {@link #WHEEL_REACH} of the Wheel (the
 * Ghost Gate's) it passes on into it, and gives {@link #SHARE} of the path's stage. Left behind ({@link #LOST} blocks off, or in another
 * world), it wanders on; so it does when its guide logs out, and at a restart.
 */
public final class JudgementHallService {
	public static final int MOST_LED = 2, WHEEL_REACH = 8;
	public static final double SHARE = 0.08, LOST = 48, DRIFT_AFTER = 16;

	/** Each soul led, and its guide. */
	private static final Map<UUID, UUID> LED = new HashMap<>();

	/** Whether {@code p} serves the Judgement Hall: its ghost path begun, practising Judgement Hall Service, holding its volume. */
	public static boolean serves(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.JUDGEMENT_HALL_SERVICE
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** Whether {@code soul} is being led to the Wheel. */
	public static boolean isLed(CultivatorNpc soul) {
		return LED.containsKey(soul.getUUID());
	}

	/**
	 * {@code p} right-clicks {@code npc} sneaking (CultivatorNpc#mobInteract): a wandering ghost taken to be led, or one it leads let go; told.
	 * @return whether it was this way's click (else the talk opens as ever)
	 */
	static boolean lead(ServerPlayer p, CultivatorNpc npc) {
		if (!(npc instanceof GhostCultivatorNpc soul) || !serves(p)) return false;
		prune(p);
		UUID guide = LED.get(soul.getUUID());
		if (p.getUUID().equals(guide)) {
			LED.remove(soul.getUUID());
			p.displayClientMessage(Component.translatable(ModLang.MSG_ESCORT_LET_GO, soul.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
			return true;
		}
		Component refusal = guide != null ? Component.translatable(ModLang.MSG_ESCORT_LED_ALREADY, soul.getDisplayName())
				: !soul.isWanderingRogue() || soul.getTarget() != null ? Component.translatable(ModLang.MSG_ESCORT_NOT_WANDERING, soul.getDisplayName())
				: led(p) >= MOST_LED ? Component.translatable(ModLang.MSG_ESCORT_FULL, MOST_LED) : null;
		if (refusal != null) {
			p.displayClientMessage(refusal.copy().withStyle(ChatFormatting.GRAY), true);
			return true;
		}
		LED.put(soul.getUUID(), p.getUUID());
		soul.getNavigation().stop();
		p.displayClientMessage(Component.translatable(ModLang.MSG_ESCORT_TAKEN, soul.getDisplayName()).withStyle(ChatFormatting.DARK_AQUA), true);
		p.serverLevel().playSound(null, soul.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.NEUTRAL, 0.8f, 1.2f);
		return true;
	}

	/** The souls {@code p} led that are gone meanwhile (slain, wandered out of loaded land, removed) let go, so they hold no place. */
	private static void prune(ServerPlayer p) {
		LED.entrySet().removeIf(led -> led.getValue().equals(p.getUUID()) && !(find(p.server, led.getKey()) instanceof GhostCultivatorNpc soul && soul.isAlive()));
	}

	private static Entity find(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity found = level.getEntity(id);
			if (found != null) return found;
		}
		return null;
	}

	/** How many souls {@code p} leads. */
	static int led(ServerPlayer p) {
		int n = 0;
		for (UUID guide : LED.values()) if (guide.equals(p.getUUID())) n++;
		return n;
	}

	/** Whether a Wheel of Reincarnation stands within {@link #WHEEL_REACH} of {@code pos}. */
	static boolean nearWheel(ServerLevel level, BlockPos pos) {
		for (BlockPos at : BlockPos.betweenClosed(pos.offset(-WHEEL_REACH, -WHEEL_REACH, -WHEEL_REACH), pos.offset(WHEEL_REACH, WHEEL_REACH, WHEEL_REACH))) {
			if (level.getBlockState(at).is(ModBlocks.WHEEL_OF_REINCARNATION)) return true;
		}
		return false;
	}

	/** {@code soul}, led by {@code guide}, passes on into the Wheel: gone, the gain its guide's; told. */
	static void passOn(ServerPlayer guide, GhostCultivatorNpc soul) {
		LED.remove(soul.getUUID());
		ServerLevel level = (ServerLevel) soul.level();
		level.sendParticles(ParticleTypes.SOUL, soul.getX(), soul.getY() + 1, soul.getZ(), 40, 0.3, 0.9, 0.3, 0.05);
		level.sendParticles(ParticleTypes.END_ROD, soul.getX(), soul.getY() + 1, soul.getZ(), 20, 0.3, 1.2, 0.3, 0.08);
		level.playSound(null, soul.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.NEUTRAL, 1.0f, 1.4f);
		guide.displayClientMessage(Component.translatable(ModLang.MSG_ESCORT_PASSED, soul.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
		soul.discard();
		CultivationMethods.grant(guide, TwoPaths.Path.GHOST, CultivationMethod.JUDGEMENT_HALL_SERVICE, SHARE);
	}

	/** {@code guide} logged out: the souls it led wander on. */
	public static void onDisconnect(ServerPlayer guide) {
		LED.values().removeIf(id -> id.equals(guide.getUUID()));
	}

	/** The server stopped. */
	public static void clear() {
		LED.clear();
	}

	/**
	 * A soul led to the Wheel (CultivatorNpc's goals): it walks after its guide, drifts after it when it falls {@link #DRIFT_AFTER} behind,
	 * and passes on at the Wheel; left {@link #LOST} behind, or its guide gone or in another world, it is let go.
	 */
	public static class EscortedGoal extends Goal {
		private final CultivatorNpc npc;
		/** Counted by its own ticks (a running goal is ticked every other tick only: Mob#serverAiStep). */
		private int repath, ticks;

		public EscortedGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		private ServerPlayer guide() {
			UUID id = LED.get(npc.getUUID());
			return id == null || npc.getServer() == null ? null : npc.getServer().getPlayerList().getPlayer(id);
		}

		@Override
		public boolean canUse() {
			return npc instanceof GhostCultivatorNpc && LED.containsKey(npc.getUUID()) && npc.getTarget() == null;
		}

		@Override
		public void start() {
			repath = ticks = 0;
			npc.setMeditating(false);
		}

		@Override
		public void tick() {
			ServerPlayer guide = guide();
			if (guide == null || guide.level() != npc.level() || !guide.isAlive() || npc.distanceTo(guide) > LOST) {
				LED.remove(npc.getUUID());
				if (guide != null) guide.displayClientMessage(Component.translatable(ModLang.MSG_ESCORT_LOST, npc.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
				return;
			}
			npc.getLookControl().setLookAt(guide, 30, 30);
			if (++ticks % 10 == 0 && npc instanceof GhostCultivatorNpc soul && nearWheel((ServerLevel) npc.level(), npc.blockPosition())) {
				passOn(guide, soul);
				return;
			}
			double d = npc.distanceTo(guide);
			if (d > DRIFT_AFTER) {
				// Fallen far behind: it drifts after its guide, as the dead can.
				Vec3 behind = guide.position().subtract(guide.getLookAngle().multiply(2, 0, 2));
				((ServerLevel) npc.level()).sendParticles(ParticleTypes.SOUL, npc.getX(), npc.getY() + 1, npc.getZ(), 10, 0.3, 0.6, 0.3, 0.02);
				npc.teleportTo(behind.x, guide.getY(), behind.z);
				npc.getNavigation().stop();
			} else if (d > 3 && --repath <= 0) {
				repath = 10;
				npc.getNavigation().moveTo(guide, 1.15);
			} else if (d <= 3) {
				npc.getNavigation().stop();
			}
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
		}
	}

	private JudgementHallService() {}
}
