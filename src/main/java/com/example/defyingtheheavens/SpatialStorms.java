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
import java.util.Iterator;
import java.util.List;

/**
 * Spatial storms: real storm clouds raging in the Spatial Gap, whose lightning strikes cultivators who pass beneath or
 * through them.
 * <p>
 * The trial keeps players inside a {@link #PERIOD}-block band by moving them back whenever they leave it, but
 * {@link SpatialTrialHandler#loops} counts those moves, so each player has a true height ({@link #trueY}) that keeps on
 * climbing (or falling) as if the gap were an endless sky. Storms live at true heights: each one sits somewhere along
 * the way, 350-700 blocks ahead of the cultivator when it gathers, up to {@link #SPREAD} blocks to one side, and a
 * cultivator passes each storm once. A player with no storm ahead of them gets a new one, so about one is in sight at a
 * time. They are huge (radius 100-180) and drift at 3-6 blocks/s, while a player steers sideways at only ~4-5 blocks/s:
 * seeing one coming early is the way past it.
 * <p>
 * There is one list for the whole gap: cultivators crossing together climb through the same storms, and one who starts
 * later meets them further back. Each player is sent the storms in true heights along with their own loop count, and
 * draws them at the matching place in the band; bolts are sent already placed in each viewer's band.
 * <p>
 * The far-off storms on the gap's sky are separate scenery (client SpatialGapAmbience). Runtime only: storms are not
 * saved, and they vanish once nobody is in the gap.
 */
public final class SpatialStorms {
	public static final int PERIOD = SpatialTrialHandler.LOOP_LIFT;
	/** A storm gathers over this long before its first strike, and dies away (no strikes) over the last FADE_TICKS. */
	public static final int FORM_TICKS = 60;
	public static final int FADE_TICKS = 60;
	/** The clouds are StormCloudMesh's shape drawn this many times larger: 24-block cells, 12-block layers. */
	public static final float SCALE = 4.0f;
	/** The cloud spans its base -12 to +48 (StormCloudMesh's layers -1..3, 3 blocks each, times SCALE). */
	public static final double CLOUD_TOP = 12.0 * SCALE;
	/**
	 * How far below its base a storm's lightning reaches: a cultivator climbing at ~36 blocks/s spends ~4 s in reach of a
	 * storm they pass beneath (this plus the cloud's own thickness).
	 */
	private static final double STRIKE_REACH = 100.0;
	/**
	 * While anyone is in reach, the storm turns on them: it strikes every 15-30 ticks instead of 20-50, and this many
	 * strikes go for a player in reach rather than lashing the empty void. Passing under a storm means 2-3 strikes at you
	 * unless you steer out from under it.
	 */
	private static final float AIMED_CHANCE = 0.85f;
	/** A new storm gathers this far ahead of the player along their way (true height), beyond sight. */
	private static final double AHEAD_MIN = 350.0;
	private static final double AHEAD_MAX = 700.0;
	/** ...and up to this far to one side of their path (about half the storms end up over it). */
	private static final double SPREAD = 200.0;
	/** A player with no storm ahead of them within this (true height, and sideways) gets a new one. */
	private static final double AHEAD_RANGE = 800.0;
	private static final double SIDE_RANGE = 500.0;
	/** A storm this far behind every player, or this far to the side of all of them, is gone. */
	private static final double BEHIND_LIMIT = 300.0;
	private static final double SIDE_LIMIT = 700.0;
	private static final int MAX_STORMS = 8;
	/** Players within this of a bolt (true height and sideways) see it. */
	private static final double STRIKE_SEND_DISTANCE = 450.0;

	private static final class Storm {
		final int id;
		final int seed;
		double x;
		double z;
		/** The cloud's base, in true height. */
		final double baseY;
		final double vx;
		final double vz;
		final float radius;
		final int life;
		int age;
		int nextStrike;

		Storm(int id, double x, double baseY, double z, double vx, double vz, float radius, int life) {
			this.id = id;
			this.seed = RANDOM.nextInt();
			this.x = x;
			this.baseY = baseY;
			this.z = z;
			this.vx = vx;
			this.vz = vz;
			this.radius = radius;
			this.life = life;
			this.nextStrike = FORM_TICKS;
		}
	}

	private static final List<Storm> STORMS = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static int nextId;

	/** How far the player has really climbed: their height plus everything the loop has taken off it. */
	public static double trueY(ServerPlayer p) {
		return p.getY() + (double) SpatialTrialHandler.loops(p) * PERIOD;
	}

	public static void tick(MinecraftServer server) {
		ServerLevel gap = server.getLevel(ModDimensions.SPATIAL_GAP);
		if (gap == null) return;
		List<ServerPlayer> players = gap.players();
		if (players.isEmpty()) {
			STORMS.clear();
			return;
		}

		for (Iterator<Storm> it = STORMS.iterator(); it.hasNext(); ) {
			Storm s = it.next();
			s.x += s.vx;
			s.z += s.vz;
			if (++s.age >= s.life || !stillAhead(s, players)) {
				it.remove();
				continue;
			}
			if (--s.nextStrike <= 0) strike(gap, players, s);
		}

		if (server.getTickCount() % 20 == 0) {
			for (ServerPlayer p : players) {
				if (p.isSpectator() || STORMS.size() >= MAX_STORMS) continue;
				if (!stormAhead(p)) STORMS.add(spawnAhead(p));
			}
			for (ServerPlayer p : players) {
				ServerPlayNetworking.send(p, ModPackets.SPATIAL_STORMS, stormsBuf(p));
			}
		}
	}

	/** Server stop: storms belong to the running server. */
	public static void clear() {
		STORMS.clear();
	}

	/** How far ahead of the player, along the way the gap carries them, the storm's base is (negative: passed). */
	private static double ahead(Storm s, ServerPlayer p) {
		return (s.baseY - trueY(p)) * SpatialTrialHandler.direction(p);
	}

	private static double sideways(Storm s, ServerPlayer p) {
		return Math.sqrt(Mth.square(p.getX() - s.x) + Mth.square(p.getZ() - s.z));
	}

	/** Not yet left far behind by every player, nor drifted far off to the side of all of them. */
	private static boolean stillAhead(Storm s, List<ServerPlayer> players) {
		for (ServerPlayer p : players) {
			if (ahead(s, p) > -BEHIND_LIMIT && sideways(s, p) < SIDE_LIMIT) return true;
		}
		return false;
	}

	private static boolean stormAhead(ServerPlayer p) {
		for (Storm s : STORMS) {
			double a = ahead(s, p);
			if (a > 0 && a < AHEAD_RANGE && sideways(s, p) < SIDE_RANGE) return true;
		}
		return false;
	}

	/** A storm (radius 100-180) gathering 350-700 blocks ahead of the player, up to 250 to one side, drifting 3-6 blocks/s. */
	private static Storm spawnAhead(ServerPlayer p) {
		float radius = 100.0f + RANDOM.nextFloat() * 80.0f;
		double angle = RANDOM.nextDouble() * Mth.TWO_PI;
		double side = Math.sqrt(RANDOM.nextDouble()) * SPREAD; // even over the disc, not bunched at its middle
		double x = p.getX() + Math.cos(angle) * side;
		double z = p.getZ() + Math.sin(angle) * side;
		double baseY = trueY(p) + SpatialTrialHandler.direction(p) * (AHEAD_MIN + RANDOM.nextDouble() * (AHEAD_MAX - AHEAD_MIN));
		double heading = RANDOM.nextDouble() * Mth.TWO_PI;
		double speed = 0.15 + RANDOM.nextDouble() * 0.15;
		return new Storm(nextId++, x, baseY, z, Math.cos(heading) * speed, Math.sin(heading) * speed, radius,
				1200 + RANDOM.nextInt(1200));
	}

	/** Beneath the cloud (within its lightning's reach) or inside it, at the player's true height. */
	private static boolean inReach(Storm s, ServerPlayer p) {
		if (sideways(s, p) > s.radius) return false;
		double h = trueY(p) - (s.baseY - STRIKE_REACH);
		return h >= 0 && h <= STRIKE_REACH + CLOUD_TOP;
	}

	private static void strike(ServerLevel gap, List<ServerPlayer> players, Storm s) {
		s.nextStrike = 20 + RANDOM.nextInt(30);
		if (s.age < FORM_TICKS || s.life - s.age < FADE_TICKS) return; // still gathering, or dying away

		List<ServerPlayer> targets = new ArrayList<>();
		for (ServerPlayer p : players) {
			if (p.isAlive() && !p.isSpectator() && !p.isCreative() && inReach(s, p)) targets.add(p);
		}
		if (!targets.isEmpty()) s.nextStrike = 15 + RANDOM.nextInt(15); // someone beneath it: it strikes faster

		long seed = RANDOM.nextLong();
		if (!targets.isEmpty() && RANDOM.nextFloat() < AIMED_CHANCE) {
			ServerPlayer target = targets.get(RANDOM.nextInt(targets.size()));
			double ty = trueY(target) + target.getBbHeight() * 0.5;
			// From inside the cloud, partway between its heart and the target, and always at least 25 blocks above them, so
			// even a cultivator inside the cloud sees the bolt come down at them.
			double sx = Mth.lerp(0.5, s.x, target.getX()) + (RANDOM.nextDouble() - 0.5) * 6.0;
			double sz = Mth.lerp(0.5, s.z, target.getZ()) + (RANDOM.nextDouble() - 0.5) * 6.0;
			double sy = Math.max(s.baseY + 2.0, ty + 25.0);
			sendStrike(players, s, sx, sy, sz, target.getX(), ty, target.getZ(), seed, true);
			hurt(gap, target);
		} else {
			// A bolt that hits nothing, lashing 60-150 blocks down into the void from inside the cloud: long against a cloud
			// this size, and slanting up to 40 blocks aside on the way.
			double angle = RANDOM.nextDouble() * Mth.TWO_PI;
			double r = RANDOM.nextDouble() * s.radius * 0.8;
			double sx = s.x + Math.cos(angle) * r;
			double sz = s.z + Math.sin(angle) * r;
			double length = 60.0 + RANDOM.nextDouble() * 90.0;
			sendStrike(players, s, sx, s.baseY + 2.0, sz, sx + (RANDOM.nextDouble() - 0.5) * 80.0, s.baseY - length,
					sz + (RANDOM.nextDouble() - 0.5) * 80.0, seed, false);
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

	/** To every player within range of the bolt, given in true heights and moved into each viewer's own band. */
	private static void sendStrike(List<ServerPlayer> players, Storm s, double x1, double y1, double z1, double x2, double y2,
			double z2, long seed, boolean hit) {
		for (ServerPlayer p : players) {
			double offset = (double) SpatialTrialHandler.loops(p) * PERIOD;
			if (Math.abs((y1 + y2) * 0.5 - trueY(p)) > STRIKE_SEND_DISTANCE) continue;
			if (Mth.square(p.getX() - x2) + Mth.square(p.getZ() - z2) > STRIKE_SEND_DISTANCE * STRIKE_SEND_DISTANCE) continue;
			FriendlyByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(s.id);
			buf.writeDouble(x1);
			buf.writeDouble(y1 - offset);
			buf.writeDouble(z1);
			buf.writeDouble(x2);
			buf.writeDouble(y2 - offset);
			buf.writeDouble(z2);
			buf.writeLong(seed);
			buf.writeBoolean(hit);
			ServerPlayNetworking.send(p, ModPackets.SPATIAL_STORM_STRIKE, buf);
		}
	}

	/**
	 * Every storm, in true heights, with the player's own loop count and height at this moment: the client works out
	 * where in its band each storm is from those, so a loop between packets moves nothing (see ClientSpatialStorms).
	 */
	private static FriendlyByteBuf stormsBuf(ServerPlayer p) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeInt(SpatialTrialHandler.loops(p));
		buf.writeDouble(p.getY());
		buf.writeVarInt(STORMS.size());
		for (Storm s : STORMS) {
			buf.writeVarInt(s.id);
			buf.writeInt(s.seed);
			buf.writeDouble(s.x);
			buf.writeDouble(s.baseY);
			buf.writeDouble(s.z);
			buf.writeFloat((float) s.vx);
			buf.writeFloat((float) s.vz);
			buf.writeFloat(s.radius);
			buf.writeVarInt(s.age);
			buf.writeVarInt(s.life);
		}
		return buf;
	}

	private SpatialStorms() {}
}
