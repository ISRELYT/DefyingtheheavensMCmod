package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Beast Blood Bathing (CultivationMethod#BEAST_BLOOD_BATHING, the user's "Go, all four", 2026-11-01): the body bathed in the blood of demon
 * beasts. A demon beast that dies spills its blood on the ground where it fell ({@link #spilled}, AFTER_DEATH), a pool that soaks away in
 * {@link #POOL_TICKS}; one drawn in by this way standing in it ({@link #REACH}) bathes ({@link #bathe}): every second {@link #PACE} seconds of
 * the Body Path's own pace, twice for each realm the beast stood above the body (at most {@link #MOST_ABOVE}), half for each below (at least
 * an eighth), the richest pool if several. A pool isn't spent by bathing: it only soaks away. Meditation tempers nothing on it.
 */
public final class BeastBlood {
	public static final int POOL_TICKS = 1200, MOST_ABOVE = 2;
	public static final double PACE = 3.0, REACH = 1.6;
	/** A beast below the body is worth half for each realm, an eighth at the least. */
	static final int MOST_BELOW = 3;
	/** At most this many pools lie at once (a slaughter's oldest soak away first). */
	static final int MOST_POOLS = 64;
	/** How far down a beast that died in the air is looked under for ground to spill on. */
	static final int FALL = 8;

	private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55f, 0.0f, 0.03f), 1.8f);

	/** A pool of a beast's blood: where, how strong (the beast's realm), until when (game time). */
	private record Pool(ResourceKey<Level> dimension, Vec3 at, Realm realm, long until) {}

	private static final List<Pool> POOLS = new ArrayList<>();

	/** What a second's bath in the blood of a beast of {@code beast}'s realm is worth to a body in {@code body}: twice a realm above, half below. */
	public static double worth(Realm beast, Realm body) {
		return Math.pow(2, Math.max(-MOST_BELOW, Math.min(MOST_ABOVE, beast.ordinal() - body.ordinal())));
	}

	/** AFTER_DEATH: a demon beast's blood spilled on the ground where it fell. */
	public static void spilled(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof DemonBeast beast) || !(entity.level() instanceof ServerLevel level)) return;
		BlockPos ground = beast.blockPosition();
		for (int i = 0; i < FALL && level.getBlockState(ground.below()).isAir(); i++) ground = ground.below();
		if (level.getBlockState(ground.below()).isAir()) ground = beast.blockPosition(); // no ground near it: where it died
		Vec3 at = new Vec3(beast.getX(), ground.getY(), beast.getZ());
		if (POOLS.size() >= MOST_POOLS) POOLS.remove(0);
		POOLS.add(new Pool(level.dimension(), at, beast.getCultivationRealm(), level.getGameTime() + POOL_TICKS));
		level.sendParticles(BLOOD, at.x, at.y + 0.1, at.z, 30, 0.7, 0.05, 0.7, 0);
		level.playSound(null, ground, SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 0.8f, 0.6f);
	}

	/** Every tick: the pools soaking away and seen (their blood on the ground); once a second, the baths. */
	public static void tick(MinecraftServer server) {
		if (POOLS.isEmpty()) return;
		long now = server.overworld().getGameTime();
		POOLS.removeIf(pool -> now >= pool.until());
		// Dense and often, so the pool reads as a pool on the ground (sparse, run 1 showed it barely at all).
		if (now % 2 == 0) {
			for (Pool pool : POOLS) {
				ServerLevel level = server.getLevel(pool.dimension());
				if (level != null) level.sendParticles(BLOOD, pool.at().x, pool.at().y + 0.06, pool.at().z, 6, 0.55, 0.01, 0.55, 0);
			}
		}
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer p : server.getPlayerList().getPlayers()) bathe(p);
	}

	/** A second in the blood, for {@code p} drawn in by this way standing in a pool: tempered by the richest, told. */
	static void bathe(ServerPlayer p) {
		PlayerCultivation c = CultivationManager.get(p);
		if (!p.isAlive() || !BodyPath.practises(c, CultivationMethod.BEAST_BLOOD_BATHING)) return;
		Realm body = BodyPath.realm(BodyPath.rank(c));
		Pool best = null;
		for (Pool pool : POOLS) {
			if (pool.dimension() != p.level().dimension()) continue;
			double dx = p.getX() - pool.at().x, dz = p.getZ() - pool.at().z;
			if (dx * dx + dz * dz > REACH * REACH || Math.abs(p.getY() - pool.at().y) > 1.5) continue;
			if (best == null || pool.realm().ordinal() > best.realm().ordinal()) best = pool;
		}
		if (best == null) return;
		double fraction = BodyPath.temperBy(p, CultivationMethod.BEAST_BLOOD_BATHING, PACE * worth(best.realm(), body));
		if (fraction <= 0) return;
		p.displayClientMessage(Component.translatable(ModLang.MSG_BLOOD_BATHING, best.realm().getDisplayName(), BodyPath.percent(fraction))
				.withStyle(ChatFormatting.RED), true);
		p.serverLevel().sendParticles(BLOOD, p.getX(), p.getY() + 0.4, p.getZ(), 8, 0.4, 0.3, 0.4, 0);
	}

	/** How many pools lie now (the game tests). */
	static int pools() {
		return POOLS.size();
	}

	/** The server stopped: the pools go with it. */
	public static void clear() {
		POOLS.clear();
	}

	private BeastBlood() {}
}
