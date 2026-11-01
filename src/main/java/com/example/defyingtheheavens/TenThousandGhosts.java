package com.example.defyingtheheavens;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Ten Thousand Ghosts Union (CultivationMethod#TEN_THOUSAND_GHOSTS; the methods overhaul's phase 4): the ghost path's way at True Immortal, a
 * devilish one: a ghost slain by one's own hand is taken in. A Hungry Ghost gives {@link #HUNGRY} of the path's stage; one of the dead's
 * cultivators (GhostCultivatorNpc) {@link #CULTIVATOR} if it stood {@link #GAP} realms below the path, twice as much for each realm above that,
 * half for each below, at most {@link #MOST}; and each one taken in draws its taker {@link #SHIFT} toward the devilish path.
 */
public final class TenThousandGhosts {
	public static final double HUNGRY = 0.004, CULTIVATOR = 0.02, MOST = 0.5;
	public static final int GAP = 5, SHIFT = 2;

	/** Whether {@code p} takes in the ghosts it slays: its ghost path practising Ten Thousand Ghosts Union, holding its volume. */
	public static boolean unites(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		return GhostMethods.ghostRank(c.netherworld()) >= 0 && CultivationMethods.practised(c, TwoPaths.Path.GHOST) == CultivationMethod.TEN_THOUSAND_GHOSTS
				&& CultivationMethods.holdsVolume(c, TwoPaths.Path.GHOST);
	}

	/** The share of its path's stage a ghost cultivator of rank {@code rank} gives one whose ghost path stands in {@code pathRealm}. */
	public static double cultivatorShare(int rank, int pathRealm) {
		return Math.min(MOST, CULTIVATOR * Math.pow(2, Math.max(0, rank) / Stage.values().length - (pathRealm - GAP)));
	}

	/** AFTER_DEATH: {@code dead}, a ghost slain by one who practises it, is taken in. */
	static void onDeath(LivingEntity dead, DamageSource source) {
		if (!(dead instanceof HungryGhost) && !(dead instanceof GhostCultivatorNpc)) return;
		if (!(source.getEntity() instanceof ServerPlayer killer) || !unites(killer)) return;
		Realm realm = GhostMethods.ghostRealm(CultivationManager.get(killer).netherworld());
		if (realm == null) return;
		double share = dead instanceof GhostCultivatorNpc npc ? cultivatorShare(npc.getRank(), realm.ordinal()) : HUNGRY;
		if (CultivationMethods.grant(killer, TwoPaths.Path.GHOST, CultivationMethod.TEN_THOUSAND_GHOSTS, share) <= 0) return;
		PlayerCultivation own = CultivationManager.own(killer); // its own path's alignment, whatever body it is in
		own.setAlignment(own.getAlignment() - SHIFT);
		CultivationManager.markDirty(killer.server);
		CultivationManager.sync(killer);
		ServerLevel level = killer.serverLevel();
		Vec3 from = dead.position().add(0, dead.getBbHeight() / 2, 0), to = killer.getEyePosition();
		for (int i = 0; i <= 10; i++) {
			Vec3 at = from.lerp(to, i / 10.0);
			level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.01);
		}
		level.playSound(null, killer.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.5f);
	}

	private TenThousandGhosts() {}
}
