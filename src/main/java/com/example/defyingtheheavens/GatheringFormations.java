package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * The gathering formations (the user's ask, 2026-10-31: "make gathering formations a new formation that gathers qi to make it more dense in
 * the area around it, assisting in cultivation methods that require qi accumulation/circulation"; the methods overhaul's phase 2): a
 * Qi-Gathering Formation thickens the heavens' qi about it, a Yin-Gathering one the ghostly qi, in three tiers ({@link Tier}: x1.25, x1.5
 * and x2 within 8, 12 and 16 blocks), while spirit stones keep it lit (GatheringFormationBlockEntity: a tier's own burn). Only Qi Circulation
 * and Netherworld Breathing gather by it (MeditationManager), faster; the years their gains cost stay as they were (the talent retune of
 * phase 2: a High formation saving years too took a Low talent with everything from Core Formation's Grand Perfection to Dao Integration,
 * against the user's "with everything low grade still only reaches CF GP ... dont change the year stretching of stuff"). Formations of a
 * kind don't add up: the best one reaching a cultivator counts.
 */
public final class GatheringFormations {
	/** What it gathers: the heavens' qi, or the ghostly qi. */
	public enum Kind {
		QI("qi"), YIN("yin");

		private final String id;

		Kind(String id) { this.id = id; }

		public String getId() { return id; }
	}

	/** A tier: how thick it makes the qi, how far, and how many ticks of burning a low spirit stone's worth keeps it lit. */
	public enum Tier {
		LOW("low", 1.25, 8, 12_000), MID("mid", 1.5, 12, 1_200), HIGH("high", 2.0, 16, 1_200 / 9.0);

		private final String id;
		private final double factor;
		private final int radius;
		private final double ticksPerWorth;

		Tier(String id, double factor, int radius, double ticksPerWorth) {
			this.id = id;
			this.factor = factor;
			this.radius = radius;
			this.ticksPerWorth = ticksPerWorth;
		}

		public String getId() { return id; }
		public double factor() { return factor; }
		public int radius() { return radius; }

		/**
		 * Ticks of burning a low spirit stone's worth (SpiritStoneItem#valueOf) keeps it lit: a Low stone ten minutes at the Low tier, a Mid
		 * one nine at the Mid, a High one nine at the High.
		 */
		public double ticksPerWorth() { return ticksPerWorth; }
	}

	/** One lit, as its block entity tells it each second it burns (not saved: the block entity's own fuel is). */
	private record Lit(Kind kind, Tier tier, long until) {}

	private static final Map<ResourceKey<Level>, Map<BlockPos, Lit>> LIT = new HashMap<>();

	/** The formation at {@code pos} in {@code level} lit (until game time {@code until}: it says so again each second it burns), or put out. */
	static void lit(Level level, BlockPos pos, Kind kind, Tier tier, boolean lit) {
		Map<BlockPos, Lit> here = LIT.computeIfAbsent(level.dimension(), d -> new HashMap<>());
		if (lit) here.put(pos.immutable(), new Lit(kind, tier, level.getGameTime() + 40));
		else here.remove(pos);
	}

	/** What the best lit formation of {@code kind} reaching {@code at} in {@code level} thickens its qi by: x1 with none. */
	public static double factor(ServerLevel level, BlockPos at, Kind kind) {
		Map<BlockPos, Lit> here = LIT.get(level.dimension());
		if (here == null || here.isEmpty()) return 1.0;
		long now = level.getGameTime();
		double best = 1.0;
		for (Map.Entry<BlockPos, Lit> entry : here.entrySet()) {
			Lit lit = entry.getValue();
			if (lit.kind() != kind || lit.until() < now || lit.tier().factor() <= best) continue;
			if (entry.getKey().distSqr(at) <= (double) lit.tier().radius() * lit.tier().radius()) best = lit.tier().factor();
		}
		return best;
	}

	/** {@link #factor(ServerLevel, BlockPos, Kind)} where {@code p}'s body sits (in the Inner Realm, its body's place: InnerRealm#bodyPos). */
	public static double factor(ServerPlayer p, Kind kind) {
		ServerLevel level = p.server.getLevel(InnerRealm.bodyDimension(p));
		if (level == null) level = p.serverLevel();
		return factor(level, InnerRealm.bodyPos(p), kind);
	}

	/** The server stopped: nothing is lit. */
	public static void clear() {
		LIT.clear();
	}

	private GatheringFormations() {}
}
