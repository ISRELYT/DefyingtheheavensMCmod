package com.example.defyingtheheavens;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where the Vestiges of the Void come from (VoidVestige): out of the dark round whoever flies open space. After a little while
 * in space ({@link #GRACE}), every {@link #INTERVAL} ticks each flier there may draw one ({@link #CHANCE}), {@link #NEAR} to
 * {@link #FAR} blocks off and clear of the planets' air, up to {@link #PER_FLIER} round them and {@link #TOTAL} in all; none on
 * Peaceful.
 * <p>
 * The farther from the Overworld one has flown, the stronger they come ({@link #rankFor}): only of Nascent Soul Early in the
 * home reaches, then a stage stronger at most for every {@link #REACH_PER_STAGE} blocks out, up to Heavenly Being Grand
 * Perfection beyond some 210,000 (each one drawn from the three stages below the most there, now and then one above).
 */
public final class VestigeSpawner {
	public static final int INTERVAL = 200, GRACE = 600, PER_FLIER = 2, TOTAL = 32;
	public static final float CHANCE = 0.45f;
	public static final double NEAR = 100, FAR = 150, REACH_PER_STAGE = 30000;

	/** Ticks each flier has spent in space since they last came there. */
	private static final Map<UUID, Integer> IN_SPACE = new HashMap<>();

	static void tick(ServerLevel space, AstralPlanets planets) {
		IN_SPACE.keySet().retainAll(space.players().stream().map(ServerPlayer::getUUID).toList());
		if (space.getDifficulty() == Difficulty.PEACEFUL) return;
		for (ServerPlayer p : space.players()) {
			if (!p.isAlive() || p.isCreative() || p.isSpectator()) continue;
			int ticks = IN_SPACE.merge(p.getUUID(), 1, Integer::sum);
			if (ticks < GRACE || ticks % INTERVAL != 0 || p.getRandom().nextFloat() >= CHANCE) continue;
			if (planets.atmosphereAt(p.position(), VoidVestige.AIR_MARGIN * 2) != null) continue;
			int near = space.getEntitiesOfClass(VoidVestige.class, new AABB(p.blockPosition()).inflate(220)).size();
			if (near >= PER_FLIER) continue;
			if (space.getEntities(ModEntities.VOID_VESTIGE, v -> true).size() >= TOTAL) continue;
			spawnNear(space, planets, p);
		}
	}

	/**
	 * One out of the dark round {@code p}, of the strength of where they are; it is not seen yet, only felt. Within the server's
	 * simulation distance (less than {@link #NEAR}..{@link #FAR} where that is short), or it would hang there unmoving.
	 */
	static VoidVestige spawnNear(ServerLevel space, AstralPlanets planets, ServerPlayer p) {
		RandomSource r = p.getRandom();
		double far = Math.min(FAR, space.getServer().getPlayerList().getSimulationDistance() * 16 - 24), near = Math.min(NEAR, far - 20);
		for (int tries = 0; tries < 8; tries++) {
			double z = r.nextDouble() * 1.2 - 0.6, a = r.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - z * z);
			Vec3 at = p.position().add(new Vec3(s * Math.cos(a), z, s * Math.sin(a)).scale(near + r.nextDouble() * (far - near)));
			if (at.y < AstralPlanets.SPACE_FLOOR + 80 || at.y > AstralPlanets.SPACE_CEILING - 80) continue;
			if (planets.atmosphereAt(at, VoidVestige.AIR_MARGIN * 2) != null || planets.starNear(at, AstralRealm.STAR_HEAT + 0.5) != null) continue;
			VoidVestige vestige = ModEntities.VOID_VESTIGE.create(space);
			if (vestige == null) return null;
			vestige.setRank(rankFor(p.position(), r));
			vestige.moveTo(at.x, at.y, at.z, r.nextFloat() * 360, 0);
			space.addFreshEntity(vestige);
			p.displayClientMessage(Component.translatable(ModLang.MSG_VESTIGE_NEAR), true);
			space.playSound(null, at.x, at.y, at.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0f, 0.4f);
			return vestige;
		}
		return null;
	}

	/** The strength of one met at {@code at}: stronger the farther from the Overworld (see the class notes). */
	public static int rankFor(Vec3 at, RandomSource r) {
		double far = Math.sqrt(Mth.square(at.x - AstralPlanets.OVERWORLD_POS.x) + Mth.square(at.z - AstralPlanets.OVERWORLD_POS.z));
		int top = Mth.clamp(VoidVestige.MIN_RANK + (int) (far / REACH_PER_STAGE), VoidVestige.MIN_RANK, VoidVestige.MAX_RANK);
		int low = Math.max(VoidVestige.MIN_RANK, top - 3);
		int rank = low + r.nextInt(top - low + 1);
		if (r.nextFloat() < 0.05f) rank = Math.min(VoidVestige.MAX_RANK, top + 1);
		return rank;
	}

	public static void forget(UUID id) {
		IN_SPACE.remove(id);
	}

	public static void clear() {
		IN_SPACE.clear();
	}

	private VestigeSpawner() {}
}
