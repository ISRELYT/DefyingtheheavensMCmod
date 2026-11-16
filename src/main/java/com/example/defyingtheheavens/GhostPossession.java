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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * The dead's own possession (the user, 2026-11-13: "ghosts should have their own unique ability to posses that doesnt rely on a nascent soul.
 * no tether"; their picks: the living and the dead alike, every ghost): Ability#GHOST_POSSESSION. One of the dead (GhostSystem#isGhost), of
 * any realm, a mortal's ghost too, slips into a body as it is, no soul sent out: the body it looks at within {@link #REACH} blocks, a body of
 * the dead lying where it fell (CorpseWalk) or a living player or cultivator (Possession#enter), at least a realm below the ghost's highest
 * path, as possession weighs any (SoulProjection#refusal, CorpseWalk#refusal: a mortal's ghost takes only mortals' bodies).
 * <ul>
 *   <li><b>Its price</b>: {@link SoulProjection#POSSESS_QI_FRACTION} of its ghostly qi to go in, and {@link #UPKEEP} a second to stay, the
 *   same wherever it goes: nothing tethers it, so nothing draws it back, however far it walks (a Nascent Soul's projected soul in a body stays
 *   tethered to its own: the user's word).</li>
 *   <li><b>In it</b> it is the body, as a soul in one is (Possession): no ghost to the world (seen by all, touching and touched, its rules the
 *   body's record's), the ability itself on the body's wheel to leave by ({@link #isTenant}: Possession's tenancy marked a ghost's).</li>
 *   <li><b>Out of it</b>: the ability again, its ghostly qi run dry, logging out, or the body's death (a living body dies as its owner's own death,
 *   a body of the dead falls down again where it stands); the ghost a ghost again there, its own things back.</li>
 * </ul>
 */
public final class GhostPossession {
	/** How far a ghost reaches for a body: as far as a projected soul does. */
	public static final double REACH = SoulProjection.POSSESS_REACH + 1;
	/** Ghostly qi a second for staying in a body: a soul's at its body, and the whole of that again for being in one, wherever it goes. */
	public static final double UPKEEP = SoulProjection.GHOST_SOUL_BASE * 1.5;

	/** Whether {@code player} is one of the dead in a body by its own possession (not a soul sent out). */
	public static boolean isTenant(ServerPlayer player) {
		return Possession.isGhostTenant(player.getUUID());
	}

	// --- The ability ---

	/** The ability (CultivationManager#toggleAbility): in a body, out of it; else into the body it looks at. */
	public static void act(ServerPlayer ghost) {
		if (isTenant(ghost)) {
			leave(ghost, Possession.inCorpse(ghost.getUUID()) ? ModLang.MSG_CORPSE_LEFT : ModLang.MSG_POSSESS_LET_GO);
			return;
		}
		if (!GhostSystem.isGhost(ghost) || Possession.inhabits(ghost.getUUID()) || SoulProjection.isActive(ghost.getUUID())) return;
		Object target = target(ghost);
		if (target instanceof LivingEntity living) enter(ghost, living);
		else if (target instanceof BlockPos pos) enterCorpse(ghost, pos);
		else ghost.displayClientMessage(Component.translatable(ModLang.MSG_GHOST_POSSESS_NONE, Math.round(REACH)), true);
	}

	/**
	 * What {@code ghost} looks at within {@link #REACH}: the nearest living player or cultivator along its sight (a LivingEntity), else a body of
	 * the dead lying there (its BlockPos), else null.
	 */
	private static Object target(ServerPlayer ghost) {
		Vec3 eye = ghost.getEyePosition(), look = ghost.getViewVector(1.0f), end = eye.add(look.scale(REACH));
		BlockHitResult block = ghost.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, ghost));
		double far = block.getType() == HitResult.Type.MISS ? REACH : eye.distanceTo(block.getLocation());
		AABB box = ghost.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(ghost, eye, eye.add(look.scale(far)), box,
				e -> e.isAlive() && (e instanceof ServerPlayer || e instanceof CultivatorNpc npc && !npc.isGhost()), far * far);
		if (hit != null) return hit.getEntity();
		if (block.getType() == HitResult.Type.BLOCK && ghost.level().getBlockState(block.getBlockPos()).is(ModBlocks.CORPSE)) return block.getBlockPos();
		return null;
	}

	/**
	 * {@code ghost} takes {@code target}'s living body (a player's, or a cultivator's): within reach, at least a realm below its highest path,
	 * as a soul's possession weighs it (SoulProjection#refusal), for its ghostly qi.
	 */
	public static InteractionResult enter(ServerPlayer ghost, LivingEntity target) {
		if (!(target instanceof ServerPlayer || target instanceof CultivatorNpc npc && !npc.isGhost())) return InteractionResult.PASS;
		if (ghost.distanceTo(target) > REACH + target.getBbWidth()) return InteractionResult.FAIL;
		PlayerCultivation own = CultivationManager.own(ghost);
		int realm = Math.max(0, TwoPaths.higherRank(own, true)) / Stage.values().length;
		String why = SoulProjection.refusal(ghost, target, realm);
		if (why != null) {
			ghost.displayClientMessage(Component.translatable(why), true);
			return InteractionResult.FAIL;
		}
		if (!paid(ghost, own) || !Possession.enter(ghost, target, true)) return InteractionResult.FAIL;
		TwoPaths.spend(own, TwoPaths.Path.GHOST, cost(own));
		settledIn(ghost);
		return InteractionResult.SUCCESS;
	}

	/**
	 * {@code ghost} rises in the body of the dead lying at {@code pos} (CorpseWalk: what the dead was, its rot going on): within reach, as
	 * CorpseWalk#refusal weighs it (one's own body whatever its realm), for its ghostly qi; drawn toward the devilish path for it (CorpseWalk#risen).
	 * Right-clicking a body does this too (CultivationEvents).
	 */
	public static InteractionResult enterCorpse(ServerPlayer ghost, BlockPos pos) {
		ServerLevel level = ghost.serverLevel();
		if (!(level.getBlockEntity(pos) instanceof CorpseBlockEntity corpse)) return InteractionResult.PASS;
		if (ghost.position().distanceTo(Vec3.atCenterOf(pos)) > REACH) return InteractionResult.FAIL;
		String why = CorpseWalk.refusal(ghost, corpse);
		if (why != null) {
			ghost.displayClientMessage(Component.translatable(why), true);
			return InteractionResult.FAIL;
		}
		PlayerCultivation own = CultivationManager.own(ghost);
		if (!paid(ghost, own)) return InteractionResult.FAIL;
		if (!Possession.enterCorpse(ghost, level, pos, true)) return InteractionResult.FAIL;
		TwoPaths.spend(own, TwoPaths.Path.GHOST, cost(own));
		settledIn(ghost);
		CorpseWalk.risen(ghost);
		return InteractionResult.SUCCESS;
	}

	/** What going in costs {@code own}: {@link SoulProjection#POSSESS_QI_FRACTION} of its ghostly qi pool. */
	private static double cost(PlayerCultivation own) {
		if (Lingering.lingers(own)) return 0; // a soul lingering after its death pays with its time instead (Lingering: the pull)
		return TwoPaths.poolMax(own, TwoPaths.Path.GHOST) * SoulProjection.POSSESS_QI_FRACTION;
	}

	/** Whether {@code ghost} has what going in costs (told if not). */
	private static boolean paid(ServerPlayer ghost, PlayerCultivation own) {
		if (TwoPaths.pool(own, TwoPaths.Path.GHOST) >= cost(own)) return true;
		ghost.displayClientMessage(Component.translatable(ModLang.MSG_POSSESS_GHOST_QI, (int) (SoulProjection.POSSESS_QI_FRACTION * 100)), true);
		return false;
	}

	/**
	 * In a body now: the body walks (no ghost's flight: the body's own, QiFlight, from the next tick), seen by everyone as the body (no ghost to the
	 * world: GhostSystem#broadcast, the tracker asked again, SoulProjection#broadcastsTo), the way out on its wheel lit, its client told.
	 */
	private static void settledIn(ServerPlayer ghost) {
		ghost.noPhysics = false;
		Abilities abilities = ghost.getAbilities();
		abilities.flying = false;
		ghost.onUpdateAbilities();
		CultivationManager.get(ghost).setAbilityEnabled(Ability.GHOST_POSSESSION, true);
		CultivationManager.sync(ghost);
		GhostSystem.broadcast(ghost);
		SoulProjection.refreshTracking(ghost);
		ModPackets.sendGhostTenant(ghost, true);
		ServerLevel level = ghost.serverLevel();
		level.playSound(null, ghost.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
		level.sendParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 1.0, ghost.getZ(), 24, 0.3, 0.6, 0.3, 0.02);
	}

	// --- Leaving ---

	/**
	 * {@code ghost} leaves the body it is in ({@code why}, a lang key, told; null for none): the body is given back where it stands (a living
	 * one to its owner, a body of the dead lain down again: Possession#leave), and the ghost a ghost there again, its own things back, seen only
	 * by those who see the dead.
	 */
	public static void leave(ServerPlayer ghost, String why) {
		if (!isTenant(ghost)) return;
		Possession.leave(ghost);
		CultivationManager.own(ghost).setAbilityEnabled(Ability.GHOST_POSSESSION, false);
		CultivationManager.sync(ghost);
		GhostSystem.broadcast(ghost);
		SoulProjection.refreshTracking(ghost);
		ModPackets.sendGhostTenant(ghost, false);
		CorpseWalk.forget(ghost);
		ServerLevel level = ghost.serverLevel();
		level.playSound(null, ghost.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.6f);
		level.sendParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 1.0, ghost.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
		if (why != null) ghost.displayClientMessage(Component.translatable(why), true);
	}

	/**
	 * The body {@code ghost} is in spends its last health (CultivationEvents' ALLOW_DEATH: never the ghost's own death): out of it, unharmed; a
	 * living body dies as its owner's own death by the same blow (Possession#kill), a body of the dead falls down where it stands.
	 */
	public static void bodyDying(ServerPlayer ghost, DamageSource source) {
		LivingEntity body = Possession.bodyEntity(ghost);
		UUID away = Possession.awayOwner(ghost); // its player logged out, the body held on: they die as they come back
		boolean corpse = Possession.inCorpse(ghost.getUUID());
		leave(ghost, corpse ? ModLang.MSG_CORPSE_FELL : ModLang.MSG_POSSESS_BODY_DIED);
		if (body != null) Possession.kill(body, source, ghost);
		else if (away != null) Possession.diedAway(ghost, away);
	}

	/** A ghost in a body logging out (or the server stopping): the body is given back first. */
	public static void onDisconnect(ServerPlayer player) {
		if (isTenant(player)) leave(player, null);
	}

	// --- Each tick ---

	/** Each server tick: every ghost in a body kept in it (Possession#tick: an owner gone, a body lost, out); each second, its upkeep. */
	public static void tick(MinecraftServer server) {
		for (ServerPlayer ghost : Possession.ghostTenants()) {
			if (!ghost.isAlive()) continue;
			try {
				if (!Possession.tick(ghost)) {
					leave(ghost, ModLang.MSG_POSSESS_LOST);
					continue;
				}
				if (server.getTickCount() % 20 == 0) second(ghost);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.error("Ghost possession tick failed for {}", ghost.getScoreboardName(), e);
			}
		}
	}

	/** A ghost's second in a body: its upkeep from its own ghostly qi ({@link #UPKEEP}, the same however far it is from anywhere); out of it, out. */
	static void second(ServerPlayer ghost) {
		PlayerCultivation own = CultivationManager.own(ghost);
		if (Lingering.lingers(own)) return; // no upkeep for a soul lingering after its death: its pull fills meanwhile, a third as fast (Lingering)
		if (TwoPaths.spend(own, TwoPaths.Path.GHOST, UPKEEP)) return;
		own.netherworld().setGhostQi(0);
		leave(ghost, Possession.inCorpse(ghost.getUUID()) ? ModLang.MSG_CORPSE_NO_GHOST_QI : ModLang.MSG_GHOST_POSSESS_DRY);
	}

	private GhostPossession() {}
}
