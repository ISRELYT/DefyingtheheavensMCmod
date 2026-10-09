package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.CultivationBoost;
import com.example.defyingtheheavens.FruitAura;
import com.example.defyingtheheavens.SpiritPedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The formation a meditator makes with the fruit-bearing Spirit Pedestals around them: which pedestal feeds whom (worked
 * out here on the client the same way the server does, see {@link CultivationBoost#pedestalsAround}), the shape the qi
 * takes, and its particles.
 * <p>
 * Each fruit's qi arcs up off the fruit and sweeps in to join its own orbit around the meditator, then circles them.
 * The orbits ("lanes") sit at different heights and widths, lean slightly and alternate direction, like the rings of an
 * armillary sphere, and all stay below eye level and at least an arm's length away so they never fill the screen.
 * The glowing ribbons along the same paths are drawn by {@link SpiritPedestalRenderer}.
 */
public final class MeditationFormation {
	/**
	 * A pedestal's fruit or ginseng feeding a meditator on orbit {@code lane} (0 = the oldest's, innermost and lowest);
	 * {@code height} is where the treasure's centre sits above the pedestal's block.
	 */
	public record Link(Player player, BlockPos pedestal, int years, int lane, float height) {}

	/** Ticks qi takes to travel from a fruit to its orbit. */
	public static final int FLOW_TICKS = 30;
	/** How fast the qi circles, in radians a tick. */
	public static final double ORBIT_SPEED = 0.09;
	private static final int REFRESH_TICKS = 10;
	/** Points of light circling each orbit at once. */
	private static final int ORBIT_MOTES = 3;

	private static List<Link> links = List.of();
	private static Map<Long, List<Link>> byPedestal = Map.of();

	/** The meditators a pedestal is feeding right now (usually none). */
	public static List<Link> fedBy(BlockPos pedestal) {
		return byPedestal.getOrDefault(pedestal.asLong(), List.of());
	}

	public static void clear() {
		links = List.of();
		byPedestal = Map.of();
	}

	public static void tick(Minecraft client) {
		ClientLevel level = client.level;
		if (level == null) {
			clear();
			return;
		}
		if (client.isPaused()) return;
		long time = level.getGameTime();
		if (time % REFRESH_TICKS == 0) refresh(level);
		for (Link link : links) {
			if (!link.player().isRemoved()) particles(level, link, time);
		}
	}

	private static void refresh(ClientLevel level) {
		List<Link> found = new ArrayList<>();
		Map<Long, List<Link>> index = new HashMap<>();
		for (Player player : level.players()) {
			if (!ClientMeditationTracker.isMeditating(player.getUUID())) continue;
			List<SpiritPedestalBlockEntity> pedestals = CultivationBoost.pedestalsAround(level, player.blockPosition());
			for (int lane = 0; lane < pedestals.size(); lane++) {
				SpiritPedestalBlockEntity pedestal = pedestals.get(lane);
				Link link = new Link(player, pedestal.getBlockPos(), pedestal.fruitAge(), lane, pedestal.displayHeight());
				found.add(link);
				index.computeIfAbsent(pedestal.getBlockPos().asLong(), k -> new ArrayList<>()).add(link);
			}
		}
		links = found;
		byPedestal = index;
	}

	// ---- Geometry, shared with the renderer ----

	/** The centre of what a link's pedestal holds. */
	public static Vec3 fruitCentre(Link link) {
		return Vec3.atLowerCornerOf(link.pedestal()).add(0.5, link.height(), 0.5);
	}

	public static double laneRadius(int lane) { return 1.1 + 0.18 * lane; }

	/** Height of a lane's middle above the meditator's feet: waist to shoulder, under the eyes. */
	public static double laneHeight(int lane) { return 0.4 + 0.2 * lane; }

	/** +1 anticlockwise seen from above, -1 clockwise; neighbouring lanes turn opposite ways. */
	public static int laneDirection(int lane) { return lane % 2 == 0 ? 1 : -1; }

	/** A point on a lane, {@code angle} round from +X. Each lane leans a little, about its own axis. */
	public static Vec3 orbitPoint(Vec3 feet, int lane, double angle) {
		double radius = laneRadius(lane);
		double lean = (lane % 2 == 0 ? 0.16 : -0.16) * radius;
		double y = laneHeight(lane) + lean * Math.sin(angle - lane * 1.3);
		return feet.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
	}

	/** Direction of travel along a lane at {@code angle} (unit length, horizontal). */
	public static Vec3 orbitTangent(int lane, double angle) {
		int dir = laneDirection(lane);
		return new Vec3(-Math.sin(angle) * dir, 0, Math.cos(angle) * dir);
	}

	/** Where qi from {@code fruit} joins its lane: a little past the point facing the pedestal, in the lane's direction. */
	public static double entryAngle(Vec3 fruit, Vec3 feet, int lane) {
		return Math.atan2(fruit.z - feet.z, fruit.x - feet.x) + laneDirection(lane) * 0.8;
	}

	/**
	 * A point on the arc from the fruit ({@code t} = 0) to where it joins its lane ({@code t} = 1): it rises off the
	 * fruit, then sweeps in to meet the orbit moving along it, so the qi flows straight on round.
	 */
	public static Vec3 streamPoint(Vec3 fruit, Vec3 feet, int lane, double t) {
		double entry = entryAngle(fruit, feet, lane);
		Vec3 end = orbitPoint(feet, lane, entry);
		Vec3 c1 = fruit.add(0, 0.6, 0);
		Vec3 c2 = end.subtract(orbitTangent(lane, entry).scale(1.2));
		double s = 1 - t;
		return fruit.scale(s * s * s).add(c1.scale(3 * s * s * t)).add(c2.scale(3 * s * t * t)).add(end.scale(t * t * t));
	}

	/** Angle a mote that joined the lane at time 0 has reached {@code ticks} later. */
	public static double orbitAngle(Vec3 fruit, Vec3 feet, int lane, double ticks) {
		return entryAngle(fruit, feet, lane) + laneDirection(lane) * ORBIT_SPEED * ticks;
	}

	// ---- Particles ----

	private static void particles(ClientLevel level, Link link, long time) {
		Vec3 fruit = fruitCentre(link);
		Vec3 feet = link.player().position();
		FruitAura.Tier tier = FruitAura.of(link.years());
		float[] rgb = FruitAura.colour(tier);
		DustParticleOptions mote = new DustParticleOptions(new Vector3f(rgb[0], rgb[1], rgb[2]), 0.55f + 0.08f * tier.ordinal());
		int lane = link.lane();

		// The stream: one mote a tick at its travelling head, leaving the arc traced behind it.
		double head = ((time + lane * 7) % FLOW_TICKS) / (double) FLOW_TICKS;
		Vec3 at = streamPoint(fruit, feet, lane, head);
		level.addParticle(mote, at.x, at.y, at.z, 0, 0, 0);

		// The orbit: a few points of light circling, each leaving a short trail.
		double angle = orbitAngle(fruit, feet, lane, time);
		for (int i = 0; i < ORBIT_MOTES; i++) {
			Vec3 p = orbitPoint(feet, lane, angle + i * Math.PI * 2 / ORBIT_MOTES);
			level.addParticle(mote, p.x, p.y, p.z, 0, 0, 0);
		}

		// Now and then a glyph flies from the fruit to where its qi joins the orbit. Enchant glyphs travel from
		// (origin + offset) to the origin, dropping 1.2 blocks at the end of their flight, so the origin sits 1.2 higher.
		if ((time + lane * 3) % 8 == 0) {
			Vec3 end = orbitPoint(feet, lane, entryAngle(fruit, feet, lane));
			double ox = end.x, oy = end.y + 1.2, oz = end.z;
			level.addParticle(ParticleTypes.ENCHANT, ox, oy, oz, fruit.x - ox, fruit.y - oy, fruit.z - oz);
		}

		// A thousand-year fruit or older: now and then a wisp of light lifts off its orbit.
		if (tier.ordinal() >= FruitAura.Tier.THOUSAND_YEAR.ordinal() && level.random.nextInt(12) == 0) {
			Vec3 p = orbitPoint(feet, lane, level.random.nextDouble() * Math.PI * 2);
			level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0.03, 0);
		}
	}

	private MeditationFormation() {}
}
