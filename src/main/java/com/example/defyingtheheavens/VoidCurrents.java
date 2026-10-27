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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Great Void's currents (the realm plan, 2026-10-25: "drifting rifts that carry you somewhere random"): rifts of streaming
 * starlight (SpatialTearEntity, Leads#VOID_CURRENT: drawn lying along their flow) that come out of the void near whoever is about, now
 * and then ({@link #EVERY}, {@link #CHANCE}, no more than {@link #NEAR} round one), drift slowly the way they flow, and close after a
 * while ({@link #LIFE}). One who touches a current is swept off along it: blinded by the light of it while it bears them (until their
 * landing's land is loaded, {@link #CARRY_MIN_TICKS} at least and {@link #CARRY_MAX_TICKS} at most), they come out {@link #CARRY_MIN} to {@link #CARRY_MAX} blocks on along its
 * flow, onto the nearest continent there (SpatialArrivals#continentColumn), their landing harmless.
 * <p>
 * None comes near a sect's grounds, a Holy Land's or a Demon Palace's ({@link #nearSect}; the user, 2026-10-27: "they seem to spawn
 * or move into holy land areas / demon palaces. Which they shouldn't"): one is born only where neither it nor the way it will drift
 * over its life comes within {@link #CLEAR_OF_SECTS} of a sect's barrier, and one that drifts up against one all the same (a sect
 * raised since) shuts there.
 */
public final class VoidCurrents {
	/** A try round each player in the Great Void this often (ticks), this likely, while fewer than {@link #NEAR} stand within {@link #NEAR_REACH}. */
	static final int EVERY = 600;
	static final float CHANCE = 0.3f;
	static final int NEAR = 2;
	static final double NEAR_REACH = 160;
	/** A new current comes out of the void this far off (sideways) and about this high round the player. */
	static final int MIN_OUT = 40, MAX_OUT = 90;
	/** A current's life (ticks), and how far it drifts a tick along its flow. */
	static final int LIFE = 90 * 20;
	static final double DRIFT = 0.06;
	/** Touching one: this near its line along its flow, this far along it either way, this near its height. */
	static final double REACH_ACROSS = 1.6, REACH_ALONG = 4.5, REACH_UP = 1.8;
	/**
	 * How far a current bears one; and how long it bears them, at least (land that loads at once would make it a blink, the light of
	 * it never seen) and at most while their landing's land loads.
	 */
	static final int CARRY_MIN = 500, CARRY_MAX = 1400, CARRY_MIN_TICKS = 30, CARRY_MAX_TICKS = 100;
	/** Its heart's height over its foot (where SpatialTearRenderer draws it, lying about it). */
	public static final double HEART = 1.5;
	/** No current comes within this of a sect's barrier (its streak's length and more). */
	static final double CLEAR_OF_SECTS = 16;
	/** A new one's way over its life is looked along at steps of this many blocks. */
	private static final double PATH_STEP = 8;

	/** One being borne by a current: where they come out, and since when (game time). */
	private record Borne(BlockPos to, long since) {}

	private static final Map<UUID, Borne> BORNE = new HashMap<>();

	public static void tick(MinecraftServer server) {
		ServerLevel level = server.getLevel(ModDimensions.GREAT_VOID);
		if (level == null) return;
		long now = level.getGameTime();
		List<? extends SpatialTearEntity> currents = level.getEntities(ModEntities.SPATIAL_TEAR, tear -> tear.leads() == SpatialTearEntity.Leads.VOID_CURRENT);
		List<Sect> sects = sectsIn(level);
		for (SpatialTearEntity current : currents) {
			Vec3 flow = Vec3.directionFromRotation(0, current.getYRot());
			double x = current.getX() + flow.x * DRIFT, z = current.getZ() + flow.z * DRIFT;
			// Up against a sect's grounds (a Holy Land's, a Demon Palace's), it goes no further: it shuts there.
			if (nearSect(sects, x, z)) {
				current.closeSoon();
				continue;
			}
			current.setPos(x, current.getY(), z);
			if (current.openness(0) < 0.5f) continue;
			for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, current.getBoundingBox().inflate(REACH_ALONG + 2))) {
				if (!p.isSpectator() && touches(current, flow, p)) sweep(level, p, flow, now);
			}
		}
		setDown(level, now);
		if (server.getTickCount() % 100 != 37) return;
		RandomSource random = level.getRandom();
		for (ServerPlayer p : level.players()) {
			if (p.isSpectator() || BORNE.containsKey(p.getUUID()) || random.nextFloat() >= CHANCE * 100f / EVERY) continue;
			int near = 0;
			for (SpatialTearEntity current : currents) if (current.distanceToSqr(p) < NEAR_REACH * NEAR_REACH) near++;
			if (near < NEAR) spawnNear(level, p, random);
		}
	}

	/** Whether {@code p} touches {@code current} (flowing {@code flow}): within its streak. */
	static boolean touches(SpatialTearEntity current, Vec3 flow, ServerPlayer p) {
		Vec3 d = p.position().add(0, p.getBbHeight() / 2, 0).subtract(current.position().add(0, HEART, 0));
		double along = d.x * flow.x + d.z * flow.z, across = Math.abs(d.x * -flow.z + d.z * flow.x);
		return Math.abs(along) <= REACH_ALONG && across <= REACH_ACROSS && Math.abs(d.y) <= REACH_UP;
	}

	/** A current coming out of the void near {@code p}, flowing any way, in open air, where neither it nor its way comes near a sect. */
	static SpatialTearEntity spawnNear(ServerLevel level, ServerPlayer p, RandomSource random) {
		List<Sect> sects = sectsIn(level);
		for (int tries = 0; tries < 6; tries++) {
			double angle = random.nextDouble() * Math.PI * 2, out = MIN_OUT + random.nextDouble() * (MAX_OUT - MIN_OUT);
			Vec3 at = new Vec3(p.getX() + Math.cos(angle) * out, p.getY() - 6 + random.nextDouble() * 22, p.getZ() + Math.sin(angle) * out);
			BlockPos spot = BlockPos.containing(at);
			if (!level.isLoaded(spot)) continue;
			boolean open = true;
			for (BlockPos near : BlockPos.betweenClosed(spot.offset(-3, -1, -3), spot.offset(3, 3, 3))) open &= level.getBlockState(near).isAir();
			if (!open) continue;
			float yaw = random.nextFloat() * 360f;
			if (pathNearSect(sects, at, yaw)) continue;
			return spawn(level, at, yaw);
		}
		return null;
	}

	/** The Great Void's sects standing now (the Holy Lands, the Demon Palaces): what no current comes near. */
	static List<Sect> sectsIn(ServerLevel level) {
		List<Sect> sects = new java.util.ArrayList<>();
		for (Sect sect : SectData.get(level.getServer()).all()) {
			if (!sect.extinct && sect.dimension == level.dimension()) sects.add(sect);
		}
		return sects;
	}

	/** Whether (x, z) lies within {@link #CLEAR_OF_SECTS} of one of {@code sects}' barriers (or within one). */
	static boolean nearSect(List<Sect> sects, double x, double z) {
		for (Sect sect : sects) {
			double dx = x - sect.core.getX() - 0.5, dz = z - sect.core.getZ() - 0.5, reach = sect.barrierRadius + CLEAR_OF_SECTS;
			if (dx * dx + dz * dz < reach * reach) return true;
		}
		return false;
	}

	/** Whether a current born at {@code at} flowing toward {@code yaw} would come near a sect, there or on its way over its life. */
	static boolean pathNearSect(List<Sect> sects, Vec3 at, float yaw) {
		if (sects.isEmpty()) return false;
		Vec3 flow = Vec3.directionFromRotation(0, yaw);
		double way = LIFE * DRIFT + REACH_ALONG;
		for (double along = 0; along <= way + PATH_STEP; along += PATH_STEP) {
			double reach = Math.min(along, way);
			if (nearSect(sects, at.x + flow.x * reach, at.z + flow.z * reach)) return true;
		}
		return false;
	}

	/** A current at {@code at}, flowing toward {@code yaw}. */
	static SpatialTearEntity spawn(ServerLevel level, Vec3 at, float yaw) {
		SpatialTearEntity current = SpatialTearEntity.create(level, at, yaw, SpatialTearEntity.Leads.VOID_CURRENT);
		current.holdOpen(LIFE);
		level.addFreshEntity(current);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT, 1.4f, 0.5f);
		return current;
	}

	/** {@code p} touched a current flowing {@code flow}: swept off along it, to come out on a continent far on. */
	private static void sweep(ServerLevel level, ServerPlayer p, Vec3 flow, long now) {
		if (BORNE.containsKey(p.getUUID())) return;
		RandomSource random = level.getRandom();
		// The nearest continent to where it bears them, tried at a few reaches along its flow till one lies well away from where it
		// found them (the nearest land to a point over the void may lie back toward them).
		BlockPos to = null;
		for (int tries = 0; tries < 4 && to == null; tries++) {
			double distance = CARRY_MIN + random.nextDouble() * (CARRY_MAX - CARRY_MIN);
			int x = Mth.floor(p.getX() + flow.x * distance), z = Mth.floor(p.getZ() + flow.z * distance);
			BlockPos land = SpatialArrivals.continentColumn(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level, x, z);
			if (land != null) land = SpatialArrivals.clearOfSects(level, land); // never set down on a Holy Land's dome, nor within it
			if (land != null && Math.sqrt(land.distToCenterSqr(p.getX(), land.getY() + 0.5, p.getZ())) >= CARRY_MIN * 0.6) to = land;
			else if (tries == 3) to = land != null ? land : SpatialArrivals.target(level, x, z);
		}
		SpatialArrivals.prepare(level, to);
		BORNE.put(p.getUUID(), new Borne(to, now));
		p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, CARRY_MAX_TICKS + 20, 0, false, false));
		p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, CARRY_MAX_TICKS + 20, 0, false, false));
		level.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.0f, 1.2f);
		p.displayClientMessage(Component.translatable(ModLang.MSG_CURRENT_SWEPT).withStyle(ChatFormatting.AQUA), true);
	}

	/** Those borne by a current set down once their landing's land is loaded (or it has borne them long enough). */
	private static void setDown(ServerLevel level, long now) {
		for (Iterator<Map.Entry<UUID, Borne>> it = BORNE.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Borne> e = it.next();
			ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
			Borne borne = e.getValue();
			if (p == null || p.level() != level) {
				it.remove();
				continue;
			}
			level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 4, 0.6, 0.8, 0.6, 0.05);
			long borneFor = now - borne.since();
			if (borneFor < CARRY_MIN_TICKS || !SpatialArrivals.ready(level, borne.to()) && borneFor < CARRY_MAX_TICKS) continue;
			it.remove();
			BlockPos ground = VoidBreach.landing(level, borne.to());
			p.teleportTo(level, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, p.getYRot(), p.getXRot());
			p.connection.resetPosition();
			p.fallDistance = 0;
			p.setDeltaMovement(Vec3.ZERO);
			p.removeEffect(MobEffects.BLINDNESS);
			p.removeEffect(MobEffects.SLOW_FALLING);
			level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 40, 0.8, 1.2, 0.8, 0.15);
			p.displayClientMessage(Component.translatable(ModLang.MSG_CURRENT_LANDED).withStyle(ChatFormatting.AQUA), true);
		}
	}

	/** Whether {@code id} is being borne by a current now. */
	public static boolean isBorne(UUID id) {
		return BORNE.containsKey(id);
	}

	/** Disconnect, or the server stopping: let go. */
	public static void forget(UUID id) {
		BORNE.remove(id);
	}

	public static void clear() {
		BORNE.clear();
	}

	private VoidCurrents() {}
}
