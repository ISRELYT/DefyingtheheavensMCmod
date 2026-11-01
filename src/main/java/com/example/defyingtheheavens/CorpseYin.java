package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Corpse Yin Refining (CultivationMethod#CORPSE_YIN; the methods overhaul's phase 2, the user's pick of 2026-10-31): one whose ghost path
 * practises it draws a body's yin into it, an empty hand to the body (Corpses#use), once a body. The richer the body, the more: a share
 * of the ghost path's stage ({@link #SHARE}) doubled for each realm the dead stood above the path and halved for each below, the most
 * {@link #MOST}; fresh the whole, rotting {@link #ROTTING}, bones {@link #BONES}; a mortal's {@link #MORTAL}. Its price: the drainer drawn
 * toward the devilish path ({@link #SHIFT}), the living's body filling with yin ({@link #CORRUPTION}: YinCorruption), and the dead's sect's
 * people within {@link #SECT_SEES} blocks turned on whoever drains their dead.
 */
public final class CorpseYin {
	public static final double SHARE = 0.15, MOST = 0.5, ROTTING = 0.6, BONES = 0.25, MORTAL = 0.02;
	public static final int SHIFT = 3;
	public static final double CORRUPTION = 0.08;
	public static final int SECT_SEES = 32;

	/** The share of the ghost path's stage {@code body}'s yin gives one whose path stands in {@code pathRealm}, lying as {@code rot}. */
	public static double share(int deadRank, int pathRealm, CorpseBlock.Rot rot) {
		double share = deadRank < 0 ? MORTAL : Math.min(MOST, SHARE * Math.pow(2, deadRank / Stage.values().length - pathRealm));
		return share * (rot == CorpseBlock.Rot.FRESH ? 1.0 : rot == CorpseBlock.Rot.ROTTING ? ROTTING : BONES);
	}

	/** Whether {@code player} draws a body's yin with an empty hand now: its ghost path begun, practising Corpse Yin Refining. */
	public static boolean draws(ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.CORPSE_YIN;
	}

	/** {@code player} draws {@code body}'s yin (lying as {@code state}), once a body, its price paid; told. */
	static InteractionResult draw(ServerPlayer player, CorpseBlockEntity body, BlockState state) {
		if (body.yinDrawn()) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_CORPSE_YIN_DRAWN, body.name()).withStyle(ChatFormatting.GRAY), true);
			return InteractionResult.SUCCESS;
		}
		PlayerCultivation c = CultivationManager.get(player);
		NetherworldState s = c.netherworld();
		Realm realm = GhostMethods.ghostRealm(s);
		double won = CultivationMethods.grant(player, TwoPaths.Path.GHOST, CultivationMethod.CORPSE_YIN,
				share(body.rank(), realm == null ? 0 : realm.ordinal(), state.getValue(CorpseBlock.STAGE)));
		if (won <= 0) return InteractionResult.SUCCESS; // no volume, or at the bottleneck: told, the body kept
		body.markYinDrawn();
		PlayerCultivation own = CultivationManager.own(player); // their own path's alignment and body, whatever body they are in
		own.setAlignment(own.getAlignment() - SHIFT);
		if (!own.netherworld().ghost()) own.netherworld().setYinCorruption(Math.min(1.0, own.netherworld().yinCorruption() + CORRUPTION));
		CultivationManager.markDirty(player.server);
		CultivationManager.sync(player);
		ServerLevel level = player.serverLevel();
		Vec3 from = Vec3.atCenterOf(body.getBlockPos()), to = player.getEyePosition();
		for (int i = 0; i <= 12; i++) {
			Vec3 at = from.lerp(to, i / 12.0);
			level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.01);
		}
		level.playSound(null, body.getBlockPos(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f);
		player.displayClientMessage(Component.translatable(ModLang.MSG_CORPSE_YIN, body.name()).withStyle(ChatFormatting.DARK_AQUA), true);
		// The dead's own sect's people about it turn on whoever drains their dead.
		if (!body.sect().isEmpty()) {
			for (CultivatorNpc npc : level.getEntitiesOfClass(CultivatorNpc.class, new AABB(body.getBlockPos()).inflate(SECT_SEES),
					n -> n.isAlive() && n.isSectMember() && n.sect() != null && body.sect().equals(n.sect().name) && n.hasLineOfSight(player))) {
				npc.regardEntry(player).value = Math.min(npc.regardEntry(player).value, Regard.HOSTILE);
				npc.setTarget(player);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** How far a devilish cultivator looks for a body to drain once its fight is over, and how long it draws (ticks). */
	public static final int NPC_SEEKS = 16, NPC_DRAWS = 60;

	/**
	 * The methods overhaul's phase 4 (the user: "devilish ones visibly drain corpses after battles"): a devilish cultivator, its fight over,
	 * goes to the nearest body within {@link #NPC_SEEKS} whose yin is undrawn (fresh or rotting; not its own sect's dead) and draws it out
	 * where all can see, the yin streaming into it for {@link #NPC_DRAWS} ticks, the body's yin drawn for good.
	 */
	public static class DrainGoal extends Goal {
		private final CultivatorNpc npc;
		private BlockPos body;
		private int drawing, ticks;

		public DrainGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		/** Whether a body at {@code pos} is one it would drain. */
		private boolean drainable(ServerLevel level, BlockPos pos) {
			BlockState state = level.getBlockState(pos);
			if (!state.is(ModBlocks.CORPSE) || state.getValue(CorpseBlock.STAGE) == CorpseBlock.Rot.BONES) return false;
			if (!(level.getBlockEntity(pos) instanceof CorpseBlockEntity dead) || dead.yinDrawn()) return false;
			Sect sect = npc.sect();
			return sect == null || dead.sect().isEmpty() || !dead.sect().equals(sect.name);
		}

		@Override
		public boolean canUse() {
			// Two ticks running: a mob's goals are tried every other tick only (Mob#serverAiStep), so a single tick could never come round.
			// Never a body cultivator's: the yin is drawn in as qi is, and it has none (CultivatorNpc#isBodyCultivator).
			if (npc.tickCount % 40 > 1 || npc.getFaction() != Alignment.Faction.DEVILISH || npc.isMortal() || npc.isBodyCultivator() || npc.getTarget() != null
					|| npc.shouldFlee() || !(npc.level() instanceof ServerLevel level)) return false;
			for (BlockPos pos : Corpses.lyingNear(level, npc.blockPosition(), NPC_SEEKS)) {
				if (drainable(level, pos)) {
					body = pos;
					return true;
				}
			}
			return false;
		}

		@Override
		public void start() {
			drawing = ticks = 0;
			npc.setMeditating(false);
		}

		@Override
		public boolean canContinueToUse() {
			return body != null && npc.getTarget() == null && ticks < 400 && npc.level() instanceof ServerLevel level && drainable(level, body);
		}

		@Override
		public void tick() {
			ticks++;
			ServerLevel level = (ServerLevel) npc.level();
			Vec3 at = Vec3.atCenterOf(body);
			npc.getLookControl().setLookAt(at.x, at.y, at.z);
			if (npc.position().distanceToSqr(at) > 2.5 * 2.5) {
				if (ticks % 20 == 1) CultivatorGoals.walkToward(npc, at, 1.0);
				return;
			}
			npc.getNavigation().stop();
			if (++drawing % 4 == 0) {
				Vec3 to = npc.getEyePosition();
				for (int i = 0; i <= 6; i++) {
					Vec3 step = at.lerp(to, i / 6.0);
					level.sendParticles(ParticleTypes.SOUL, step.x, step.y, step.z, 1, 0.06, 0.06, 0.06, 0.01);
				}
			}
			if (drawing == 1) npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			if (drawing >= NPC_DRAWS && level.getBlockEntity(body) instanceof CorpseBlockEntity dead) {
				dead.markYinDrawn();
				level.playSound(null, body, SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.0f, 0.6f);
				level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.3, at.z, 12, 0.3, 0.2, 0.3, 0.02);
				body = null;
			}
		}

		@Override
		public void stop() {
			body = null;
			npc.getNavigation().stop();
		}
	}

	private CorpseYin() {}
}
