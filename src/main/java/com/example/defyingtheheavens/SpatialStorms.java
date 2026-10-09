package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Spatial storms: real storm clouds raging in the Spatial Gap, the same for everyone in it, whose lightning strikes
 * cultivators who pass beneath or through them.
 * <p>
 * The weather is persistent and needs no saving: where every storm is follows from the world seed and the game time
 * alone. The sky has {@link #SLOTS} storm slots; each one cycles for ever (a storm gathers somewhere, drifts across at
 * 1-2.4 blocks/s for 3-7 minutes, dies away, then that part of the sky stays calm until the slot's next storm). So the
 * storms keep moving while nobody is in the gap and carry on where they were after a restart, and a crossing blocked by
 * a storm now may be clear a few minutes later. About a quarter to a third of the gap lies under some storm at any time.
 * <p>
 * The gap is a loop: the trial moves players {@link #PERIOD} blocks back whenever they leave its Y 64-320 band, keeping
 * their speed, so every storm repeats every PERIOD blocks up and down, keeping the loop seamless. The weather also
 * repeats every {@link #TILE} blocks east-west and north-south, so it covers the ascent at X/Z 0 and every place a
 * descending cultivator falls in alike. Each player is only ever shown and struck by the copy of a storm nearest them.
 * <p>
 * Clients draw the clouds from the SPATIAL_STORMS packet (the storms near each player, every second) and each bolt from
 * SPATIAL_STORM_STRIKE. The far-off storms on the gap's sky are separate scenery (client SpatialGapAmbience).
 */
public final class SpatialStorms {
	public static final int PERIOD = SpatialTrialHandler.LOOP_LIFT;
	/** The weather repeats every TILE blocks along X and Z. */
	private static final int TILE = 1024;
	private static final int SLOTS = 24;
	/** A slot's cycle (storm, then calm) lasts 5000-10000 ticks; its storm takes up 55-85% of it. */
	private static final int CYCLE_MIN_TICKS = 5000;
	private static final int CYCLE_EXTRA_TICKS = 5000;
	/** A storm gathers over this long before its first strike, and dies away (no strikes) over the last FADE_TICKS. */
	public static final int FORM_TICKS = 200;
	public static final int FADE_TICKS = 200;
	/** The clouds are StormCloudMesh's shape drawn this many times larger: 18-block cells, 9-block layers. */
	public static final float SCALE = 3.0f;
	/** The cloud spans its base -9 to +36 (StormCloudMesh's layers -1..3, 3 blocks each, times SCALE). */
	public static final double CLOUD_TOP = 12.0 * SCALE;
	/** How far below its base a storm's lightning reaches. */
	private static final double STRIKE_REACH = 40.0;
	/** Chance a strike goes for a player in reach rather than lashing the empty void. */
	private static final float AIMED_CHANCE = 0.6f;
	/** Storms whose edge is within this of a player are sent to them (well past the fog). */
	private static final double SYNC_DISTANCE = 450.0;
	/** Storms whose edge is within this of a player throw lightning, and players this close to a bolt see it. */
	private static final double STRIKE_DISTANCE = 360.0;

	/** One storm at one moment, at one of its copies. */
	record Storm(int id, int seed, double x, double baseY, double z, double vx, double vz, float radius, int age, int life) {
		boolean raging() {
			return age >= FORM_TICKS && life - age >= FADE_TICKS;
		}

		/** The copy of this storm nearest a point (the weather repeats every TILE blocks). */
		Storm nearestTo(double px, double pz) {
			return new Storm(id, seed, x + TILE * Math.rint((px - x) / TILE), baseY, z + TILE * Math.rint((pz - z) / TILE),
					vx, vz, radius, age, life);
		}

		/** How far a point is outside the cloud's edge, sideways (0 beneath it). */
		double distanceOutside(double px, double pz) {
			return Math.max(0.0, Math.sqrt(Mth.square(px - x) + Mth.square(pz - z)) - radius);
		}
	}

	/** Strike countdowns of the storms near someone in the gap; the only state here, and it needn't survive anything. */
	private static final Map<Integer, Integer> STRIKE_TIMERS = new HashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static final long[] CYCLE = new long[SLOTS];
	private static final long[] OFFSET = new long[SLOTS];
	private static long cyclesSeed;
	private static boolean cyclesReady;

	public static void tick(MinecraftServer server) {
		ServerLevel gap = server.getLevel(ModDimensions.SPATIAL_GAP);
		if (gap == null) return;
		List<ServerPlayer> players = gap.players();
		if (players.isEmpty()) {
			STRIKE_TIMERS.clear(); // the storms themselves need no ticking: they are a function of time
			return;
		}

		List<Storm> storms = stormsAt(gap.getSeed(), gap.getGameTime());
		Set<Integer> active = new HashSet<>();
		for (Storm s : storms) {
			if (!s.raging() || !nearAnyone(s, players, STRIKE_DISTANCE)) continue;
			active.add(s.id());
			int left = STRIKE_TIMERS.getOrDefault(s.id(), nextStrikeDelay()) - 1;
			if (left <= 0) {
				strike(gap, players, s);
				left = nextStrikeDelay();
			}
			STRIKE_TIMERS.put(s.id(), left);
		}
		STRIKE_TIMERS.keySet().retainAll(active);

		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer p : players) {
				ServerPlayNetworking.send(p, ModPackets.SPATIAL_STORMS, stormsBuf(storms, p));
			}
		}
	}

	/** Server stop: the next world may have another seed. */
	public static void clear() {
		STRIKE_TIMERS.clear();
		cyclesReady = false;
	}

	/** Every storm in the sky at this game time, at its copy in the tile around X/Z 0 to TILE. */
	static List<Storm> stormsAt(long worldSeed, long gameTime) {
		prepareCycles(worldSeed);
		List<Storm> storms = new ArrayList<>(SLOTS);
		for (int slot = 0; slot < SLOTS; slot++) {
			long shifted = gameTime + OFFSET[slot];
			long k = Math.floorDiv(shifted, CYCLE[slot]);
			int age = (int) (shifted - k * CYCLE[slot]);
			// Everything about the slot's k-th storm comes from this one generator, always drawn in the same order.
			RandomSource r = RandomSource.create(mix(worldSeed, slot, k));
			int life = (int) (CYCLE[slot] * (0.55 + 0.3 * r.nextDouble()));
			if (age >= life) continue; // this part of the sky is calm until the slot's next storm
			double heading = r.nextDouble() * Mth.TWO_PI;
			double speed = 0.05 + r.nextDouble() * 0.07;
			double vx = Math.cos(heading) * speed;
			double vz = Math.sin(heading) * speed;
			double x = Mth.positiveModulo(r.nextDouble() * TILE + vx * age, TILE);
			double z = Mth.positiveModulo(r.nextDouble() * TILE + vz * age, TILE);
			float radius = 48.0f + r.nextFloat() * 54.0f;
			double baseY = 64.0 + r.nextDouble() * PERIOD;
			int seed = r.nextInt();
			int id = slot * 100_000 + (int) (k % 100_000);
			storms.add(new Storm(id, seed, x, baseY, z, vx, vz, radius, age, life));
		}
		return storms;
	}

	private static void prepareCycles(long worldSeed) {
		if (cyclesReady && cyclesSeed == worldSeed) return;
		for (int slot = 0; slot < SLOTS; slot++) {
			RandomSource r = RandomSource.create(mix(worldSeed, slot, -1));
			CYCLE[slot] = CYCLE_MIN_TICKS + r.nextInt(CYCLE_EXTRA_TICKS);
			OFFSET[slot] = r.nextInt((int) CYCLE[slot]); // so the slots' storms don't all begin together
		}
		cyclesSeed = worldSeed;
		cyclesReady = true;
	}

	/** A well-mixed 64-bit hash of the seed, slot and storm number (SplitMix64's finaliser). */
	private static long mix(long seed, int slot, long k) {
		long h = seed ^ (slot + 1) * 0x9E3779B97F4A7C15L ^ (k + 7) * 0xC2B2AE3D27D4EB4FL;
		h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
		h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
		return h ^ (h >>> 31);
	}

	private static int nextStrikeDelay() {
		return 20 + RANDOM.nextInt(30);
	}

	private static boolean nearAnyone(Storm s, List<ServerPlayer> players, double distance) {
		for (ServerPlayer p : players) {
			if (s.nearestTo(p.getX(), p.getZ()).distanceOutside(p.getX(), p.getZ()) < distance) return true;
		}
		return false;
	}

	/**
	 * How far above the lowest point of the storm's reach a player is, folded into one period; within
	 * {@code STRIKE_REACH + CLOUD_TOP} means beneath the cloud or inside it. Negative when out from under it sideways.
	 * {@code s} must be the copy nearest the player.
	 */
	private static double heightInReach(Storm s, ServerPlayer p) {
		if (Mth.square(p.getX() - s.x()) + Mth.square(p.getZ() - s.z()) > s.radius() * s.radius()) return -1;
		return Mth.positiveModulo(p.getY() - (s.baseY() - STRIKE_REACH), PERIOD);
	}

	private static void strike(ServerLevel gap, List<ServerPlayer> players, Storm s) {
		List<ServerPlayer> inReach = new ArrayList<>();
		for (ServerPlayer p : players) {
			if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
			double h = heightInReach(s.nearestTo(p.getX(), p.getZ()), p);
			if (h >= 0 && h <= STRIKE_REACH + CLOUD_TOP) inReach.add(p);
		}

		if (!inReach.isEmpty() && RANDOM.nextFloat() < AIMED_CHANCE) {
			ServerPlayer target = inReach.get(RANDOM.nextInt(inReach.size()));
			Storm copy = s.nearestTo(target.getX(), target.getZ());
			double base = target.getY() - heightInReach(copy, target) + STRIKE_REACH; // the copy the target is under
			double tx = target.getX(), ty = target.getY() + target.getBbHeight() * 0.5, tz = target.getZ();
			// From inside the cloud, partway between its heart and the target; short if the target is in the cloud itself.
			double sx = Mth.lerp(0.5, copy.x(), tx) + (RANDOM.nextDouble() - 0.5) * 6.0;
			double sz = Mth.lerp(0.5, copy.z(), tz) + (RANDOM.nextDouble() - 0.5) * 6.0;
			double sy = Math.max(base + 2.0, ty + 6.0);
			long seed = RANDOM.nextLong();
			for (ServerPlayer p : players) {
				if (Mth.square(p.getX() - tx) + Mth.square(p.getZ() - tz) > STRIKE_DISTANCE * STRIKE_DISTANCE) continue;
				sendStrike(p, s.id(), sx, sy, sz, tx, ty, tz, seed, true);
			}
			hurt(gap, target);
		} else {
			// A bolt that hits nothing, lashing down from somewhere under the cloud. Each viewer sees it at the copy of the
			// storm nearest them, so everyone sees the storm they're looking at strike.
			double angle = RANDOM.nextDouble() * Mth.TWO_PI;
			double r = RANDOM.nextDouble() * s.radius() * 0.8;
			double ox = Math.cos(angle) * r, oz = Math.sin(angle) * r;
			double ex = (RANDOM.nextDouble() - 0.5) * 12.0, ez = (RANDOM.nextDouble() - 0.5) * 12.0;
			double length = 20.0 + RANDOM.nextDouble() * 30.0;
			long seed = RANDOM.nextLong();
			for (ServerPlayer p : players) {
				Storm copy = s.nearestTo(p.getX(), p.getZ());
				if (copy.distanceOutside(p.getX(), p.getZ()) > STRIKE_DISTANCE) continue;
				double y1 = s.baseY() + 2.0, y2 = s.baseY() - length;
				double shift = PERIOD * Math.rint((p.getY() - (y1 + y2) * 0.5) / PERIOD);
				double x1 = copy.x() + ox, z1 = copy.z() + oz;
				sendStrike(p, s.id(), x1, y1 + shift, z1, x1 + ex, y2 + shift, z1 + ez, seed, false);
			}
		}
	}

	/**
	 * A storm bolt hits as hard as one strike of the Heavenly Tribulation that brought the player to their realm and stage
	 * (Qi Refining, which none did, takes the first: Foundation Building's), worn Rings of Transcendence halving it as they
	 * do there. Ascension and descension are meant to be punishing: on top of the pressure, one unprotected hit is often
	 * the end.
	 */
	private static void hurt(ServerLevel gap, ServerPlayer target) {
		PlayerCultivation c = CultivationManager.get(target);
		TribulationManager.Strikes strike = TribulationManager.strikes(c.getRealm(), c.getStage());
		if (strike.count() == 0) strike = TribulationManager.strikes(Realm.FOUNDATION_BUILDING, Stage.EARLY);
		float multiplier = RingOfTranscendenceItem.damageMultiplier(target);

		// Both parts land in the same tick, magic first, like a tribulation's. The hurt cooldown is cleared around them so
		// neither part, nor the next second's pressure, is swallowed by the one before it.
		target.invulnerableTime = 0;
		if (strike.magic() > 0) {
			target.hurt(gap.damageSources().magic(), strike.magic() * multiplier);
			target.invulnerableTime = 0;
		}
		if (target.isAlive()) target.hurt(gap.damageSources().lightningBolt(), strike.bolt() * multiplier);
		target.invulnerableTime = 0;
	}

	private static void sendStrike(ServerPlayer p, int stormId, double x1, double y1, double z1, double x2, double y2, double z2,
			long seed, boolean hit) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(stormId);
		buf.writeDouble(x1);
		buf.writeDouble(y1);
		buf.writeDouble(z1);
		buf.writeDouble(x2);
		buf.writeDouble(y2);
		buf.writeDouble(z2);
		buf.writeLong(seed);
		buf.writeBoolean(hit);
		ServerPlayNetworking.send(p, ModPackets.SPATIAL_STORM_STRIKE, buf);
	}

	/** The storms near one player, each at its copy nearest them. */
	private static FriendlyByteBuf stormsBuf(List<Storm> storms, ServerPlayer p) {
		List<Storm> near = new ArrayList<>();
		for (Storm s : storms) {
			Storm copy = s.nearestTo(p.getX(), p.getZ());
			if (copy.distanceOutside(p.getX(), p.getZ()) <= SYNC_DISTANCE) near.add(copy);
		}
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(near.size());
		for (Storm s : near) {
			buf.writeVarInt(s.id());
			buf.writeInt(s.seed());
			buf.writeDouble(s.x());
			buf.writeDouble(s.baseY());
			buf.writeDouble(s.z());
			buf.writeFloat((float) s.vx());
			buf.writeFloat((float) s.vz());
			buf.writeFloat(s.radius());
			buf.writeVarInt(s.age());
			buf.writeVarInt(s.life());
		}
		return buf;
	}

	private SpatialStorms() {}
}
