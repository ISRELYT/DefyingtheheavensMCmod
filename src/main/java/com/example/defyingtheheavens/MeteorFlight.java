package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Meteor Flight ({@link Ability#METEOR_FLIGHT}; the user's ask, 2026-10-23, after a flight they saw in a video): Qi Flight's fast
 * mode, a standing switch. Off, Qi Flight and the free flight of ghostly creatures are creative flight at vanilla's speed at every
 * realm (QiFlight), for fine work. On, holding forward flies the cultivator where they look, laid out flat (the client steers,
 * MeteorFlightClient, and says here whether forward is held: {@link #setStreaking}), sprinting up to their realm's top speed
 * ({@link #topSpeed}: Core Formation 45 blocks a second up to True Immortal 200), slowed by the client where the land ahead hasn't
 * come yet. Past {@link #BARRIER_SHARE} of it (60 blocks a second at most) the sound barrier breaks: a boom, and a ring of air for all
 * to see.
 * <p>
 * From Nascent Soul, held at top speed for {@link #HEAT_AFTER} ticks (or flat out as fast as the land ahead lets one go, no slower than
 * {@link #HEAT_FLOOR}), the flier comes ablaze (only a sight: nothing burns). Ablaze,
 * they tear through what stands in their way ({@link #carve}), as the great spells' wounds do (HeavenSpells#carvable: nothing harder
 * than obsidian, nothing kept in it, nothing a barrier shelters, no ground they couldn't dig; gone for good, dropping nothing),
 * unless the gamerule meteorFlightSmashes is off. Slowing, they cool off: still glowing a while, then not.
 * <p>
 * At any realm, flying fast with the boost held, the way ahead is bulldozed ({@link Bulldozer}): the flier's client clears what it
 * would run into before it gets there and names it here ({@link #bulldoze}), so they keep their speed (the user's ask, 2026-10-23).
 * <p>
 * Flying fast costs {@link #STREAK_COST} times Qi Flight's qi; from Heavenly Being on never more than one gathers, so it never runs
 * one dry ({@link #streakCost}). The dead fly freely, for nothing, as they do. Not in space (its own flight), the Spatial Gap or on
 * the Soul River, nor on a sword. Flying punches are not part of it (the user's call).
 */
public final class MeteorFlight {
	/** Top speed, blocks a second, at each realm's Early stage (by Realm ordinal: none below Core Formation); each stage {@link #STAGE_STEP} more. */
	private static final double[] TOP_SPEEDS = {0, 0, 45, 60, 75, 95, 115, 135, 155, 175, 200};
	private static final double STAGE_STEP = 0.04;
	/** Forward without sprinting flies this share of the top speed, never slower than Qi Flight's own sprint ({@link #NORMAL_SPRINT}). */
	public static final double CRUISE_SHARE = 0.4;
	/** Qi Flight's sprint at vanilla's flying speed, blocks a tick (QiFlight#stepAt's across). */
	public static final double NORMAL_SPRINT = 22.2 * SwordFlight.QI_FLIGHT_SPEED;
	/** The sound barrier, as a share of the top speed, and never past {@link #BARRIER_CAP} (blocks a tick: 60 a second). */
	public static final double BARRIER_SHARE = 0.6, BARRIER_CAP = 3.0;
	/**
	 * At this share of the top speed or more the heat builds; or holding the boost down (sprinting: the client's word, {@link
	 * #setStreaking}; "if you continue to hold it down", as the video has it) no slower than {@link #HEAT_FLOOR} (blocks a tick: 40 a
	 * second), however the land ahead holds one back. A True Immortal's 90% is 180 blocks a second, faster than land never made before
	 * comes: held back by it, it caught alight and went out again for good (the user's report, 2026-10-23); a line that followed the
	 * land's own limit flickered with it and did no better (run 8).
	 */
	public static final double FLAME_SHARE = 0.9, HEAT_FLOOR = 2.0;
	/** Ticks held at top speed before the heat rises; then it rises {@link #HEAT_RISE} a tick, and falls {@link #HEAT_FALL} a tick below it. */
	public static final int HEAT_AFTER = 40;
	public static final float HEAT_RISE = 0.05f, HEAT_FALL = 0.006f;
	/** Ablaze from this much heat: the flames, and the smashing. Below it, still hot, the body glows as it cools. */
	public static final float ABLAZE = 0.5f;
	/** A boom at most this often (ticks); the barrier is there to break again once below this share of where it broke. */
	private static final int BOOM_COOLDOWN = 60;
	private static final double BOOM_RESET = 0.8;
	/** Flying fast costs this many times Qi Flight's qi. */
	public static final double STREAK_COST = 3;
	/** The ablaze tear a tube this round about the body's middle... */
	public static final double TUBE_RADIUS = 1.5;
	/** ...this many blocks at most a tick, out ahead of the body ({@link #ahead}) at least {@link #LEAST_AHEAD} and at most {@link #MOST_AHEAD}. */
	private static final int MOST_A_TICK = 1200;
	private static final double LEAST_AHEAD = 3, MOST_AHEAD = 48;
	/**
	 * A tick's movement past this many top speeds is a teleport (a rift, a command, the clone), not flight: many, since a server held up
	 * a moment (land being made) takes all the moves it missed in its next tick.
	 */
	private static final double TELEPORT_STEPS = 12;
	/** Those watching are told how hot a flier is this often (ticks) while it lasts, besides whenever it streaks, stops or catches. */
	private static final int SYNC_EVERY = 10;
	/** How many ticks' moves the speed is the mean of. */
	private static final int WINDOW = 10;

	private static final class State {
		/** The client says forward is held in Meteor Flight; and that the boost is held down with it (sprinting). */
		boolean wants, flatOut;
		/** It wants it where and as it may: this tick's word. */
		boolean streaking;
		Vec3 last;
		ResourceKey<Level> dimension;
		/**
		 * Blocks a tick, as the server sees the flier go: the mean of the last {@link #WINDOW} ticks' moves, since packets come
		 * bunched (two in one tick, none the next, when a client's frames stutter: 2026-10-23 run 5, where a shorter smoothing dipped
		 * under the heat's line every other tick and the heat never built).
		 */
		double speed;
		final double[] moves = new double[WINDOW];
		int nextMove;
		int fastTicks;
		float heat;
		boolean pastBarrier;
		long boomAt = Long.MIN_VALUE / 2;
		long soundAt = Long.MIN_VALUE / 2;
		/** Game time it last flew fast. */
		long streakedAt = Long.MIN_VALUE / 2;
		boolean sentStreaking, sentAblaze, sentSmashes;
		float sentHeat;
		int sinceSent;
		/** Booms so far (tests). */
		int booms;
		/** The bulldozer's blocks taken in the game tick {@link #dozedAt} ({@link #bulldoze}: one budget a tick, however many words come). */
		int dozed;
		long dozedAt = Long.MIN_VALUE;
	}

	/**
	 * The bulldozer's word ({@link #bulldoze}) is taken only from one the server sees going this share of Bulldozer#FROM_SPEED or faster
	 * (State#speed, the mean of its moves, which lags a flier setting off: a True Immortal's comes to a quarter of it as its client
	 * starts clearing), or diving steeply, burrowing (Bulldozer#burrowing), close by...
	 */
	private static final double DOZE_SPEED_SHARE = 0.25;
	/**
	 * ...and only for blocks in a cone ahead of where it looks: this far to the side of the line through its middle, its speed (a tick's
	 * legs go round an L, Bulldozer) and this much more for each block out along it (a look a tick stale, a turn); and no more than
	 * this (and a tick's flight) behind its middle.
	 */
	private static final double DOZE_SIDE = 4, DOZE_WIDENS = 0.6, DOZE_BEHIND = 4;

	private static final Map<UUID, State> STATES = new HashMap<>();

	// --- The numbers ---

	/** Top speed (blocks a second) of one who wields {@code realm} at {@code stage}: none below Core Formation. */
	public static double topSpeed(Realm realm, Stage stage) {
		return TOP_SPEEDS[realm.ordinal()] * (1 + STAGE_STEP * stage.ordinal());
	}

	/**
	 * {@link #topSpeed} of {@code c}'s higher path as the realm lets them wield it (TwoPaths#higherRank, as Qi Flight's own speed at
	 * the high realms went before), or 0 for a mortal.
	 */
	public static double topSpeed(PlayerCultivation c) {
		int rank = TwoPaths.higherRank(c, true);
		if (c.isMortal() || rank < 0) return 0;
		int stages = Stage.values().length;
		return topSpeed(Realm.byIndex(rank / stages), Stage.byIndex(rank % stages));
	}

	/** {@link #topSpeed} in blocks a tick. Both sides (the client steers by it). */
	public static double topPerTick(PlayerCultivation c) {
		return topSpeed(c) / 20.0;
	}

	/** Forward without sprinting, blocks a tick. */
	public static double cruisePerTick(PlayerCultivation c) {
		return Math.max(NORMAL_SPRINT, topPerTick(c) * CRUISE_SHARE);
	}

	/** From Nascent Soul (either path, as wielded) top speed sets one ablaze. */
	public static boolean canBurn(PlayerCultivation c) {
		return !c.isMortal() && TwoPaths.higherRank(c, true) >= PlayerCultivation.rank(Realm.NASCENT_SOUL, Stage.EARLY);
	}

	/**
	 * Qi a second flying fast costs, where Qi Flight costs {@code flight}: {@link #STREAK_COST} times it; from Heavenly Being on (as
	 * Qi Flight goes by the effective realm) never more than one gathers, so it never runs one dry, as Qi Flight never does there.
	 */
	public static double streakCost(PlayerCultivation c, double flight) {
		double cost = flight * STREAK_COST;
		if (c.getEffectiveRealm().ordinal() >= Realm.HEAVENLY_BEING.ordinal()) cost = Math.min(cost, Math.max(flight, c.qiGatherPerSecond()));
		return cost;
	}

	/** Where Meteor Flight is flown: not space (its own flight), the Spatial Gap or the Soul River. Both sides. */
	public static boolean flownIn(ResourceKey<Level> dimension) {
		return !ModDimensions.isSpace(dimension) && !ModDimensions.isSpatialGap(dimension) && !ModDimensions.isSoulRiver(dimension);
	}

	/** Whether {@code p} may fly fast now: switched on, in the air by Qi Flight or freely (not on a sword), and where it is flown. */
	public static boolean mayStreak(ServerPlayer p, PlayerCultivation c) {
		return c.isAbilityActive(Ability.METEOR_FLIGHT) && p.getAbilities().flying && p.getAbilities().mayfly && !p.isCreative() && !p.isSpectator()
				&& !p.isPassenger() && !SwordFlight.isRiding(p) && !SoulProjection.isSoul(p) && flownIn(p.level().dimension());
	}

	// --- Underway ---

	/**
	 * From the client (C2S meteor_streak): whether forward is held in Meteor Flight, and whether the boost is held down with it
	 * (sprinting: MeteorFlightClient#travel). Only a word for the heat, which still wants {@link #HEAT_FLOOR} as the server sees it.
	 */
	public static void setStreaking(ServerPlayer p, boolean on, boolean flatOut) {
		State s = STATES.computeIfAbsent(p.getUUID(), id -> new State());
		s.wants = on;
		s.flatOut = on && flatOut;
	}

	/** Whether {@code p} is flying fast this tick (its flight costs {@link #streakCost}). */
	public static boolean isStreaking(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s != null && s.streaking;
	}

	/** How hot {@code p} is, 0 to 1 ({@link #ABLAZE} and more: ablaze). */
	public static float heat(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s == null ? 0 : s.heat;
	}

	/** How fast {@code p} goes (blocks a tick) as the server sees it. */
	public static double speed(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s == null ? 0 : s.speed;
	}

	/**
	 * Whether those watching {@code p} are told where it is outright rather than how far it went (ServerEntityMixin): while it flies
	 * fast and for {@link #TOLD_OUTRIGHT_AFTER} ticks after, so the place it stops at is right on every watcher's screen.
	 */
	public static boolean toldOutright(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s != null && p.level().getGameTime() - s.streakedAt <= TOLD_OUTRIGHT_AFTER;
	}

	private static final int TOLD_OUTRIGHT_AFTER = 40;

	/** How many times {@code p} has broken the sound barrier (tests). */
	static int booms(ServerPlayer p) {
		State s = STATES.get(p.getUUID());
		return s == null ? 0 : s.booms;
	}

	/** Every tick (QiFlight#tick), each living player: how fast they go, the barrier, the heat, the smashing, the word to those near. */
	static void update(ServerPlayer p, PlayerCultivation c) {
		State s = STATES.get(p.getUUID());
		if (s == null) {
			if (!c.isAbilityActive(Ability.METEOR_FLIGHT)) return; // nothing to keep for one who has it off
			s = new State();
			STATES.put(p.getUUID(), s);
		}
		Vec3 at = p.position();
		boolean same = s.last != null && s.dimension == p.level().dimension();
		Vec3 from = same ? s.last : at;
		s.last = at;
		s.dimension = p.level().dimension();
		observe(p, c, s, mayStreak(p, c), from, at);
	}

	/** {@link #update} as if {@code p} had flown from {@code from} to {@code to} this tick, {@code may} fly fast (tests). */
	static void observe(ServerPlayer p, PlayerCultivation c, boolean may, Vec3 from, Vec3 to) {
		observe(p, c, STATES.computeIfAbsent(p.getUUID(), id -> new State()), may, from, to);
	}

	private static void observe(ServerPlayer p, PlayerCultivation c, State s, boolean may, Vec3 from, Vec3 to) {
		double top = topPerTick(c);
		double moved = to.distanceTo(from);
		if (moved > Math.max(top, NORMAL_SPRINT) * TELEPORT_STEPS + 8) {
			moved = 0; // a teleport
			from = to;
		}
		s.streaking = may && s.wants && top > 0;
		s.moves[s.nextMove] = moved;
		s.nextMove = (s.nextMove + 1) % WINDOW;
		double sum = 0;
		for (double m : s.moves) sum += m;
		s.speed = sum / WINDOW;
		double speed = s.streaking ? s.speed : 0;
		long now = p.level().getGameTime();
		if (s.streaking) s.streakedAt = now;
		// The sound barrier: a boom as it breaks (not again for a while, nor until well below it once more).
		double barrier = Math.min(top * BARRIER_SHARE, BARRIER_CAP);
		if (speed >= barrier) {
			if (!s.pastBarrier && now - s.boomAt >= BOOM_COOLDOWN) {
				s.boomAt = now;
				boom(p, s, from, to);
			}
			s.pastBarrier = true;
		} else if (speed < barrier * BOOM_RESET) {
			s.pastBarrier = false;
		}
		// The heat: from Nascent Soul, held at top speed, or with the boost held down, however the land holds one back.
		boolean flatOut = speed >= top * FLAME_SHARE || s.flatOut && speed >= HEAT_FLOOR;
		if (flatOut && canBurn(c)) {
			if (++s.fastTicks > HEAT_AFTER) s.heat = Math.min(1, s.heat + HEAT_RISE);
		} else {
			s.fastTicks = 0;
			s.heat = Math.max(0, s.heat - HEAT_FALL);
		}
		// Ablaze and still pressing on (stopped short by a wall too, the way ahead opened for it): through what stands in the way. Not
		// one of the dead the dead's rules hold (Incorporeal: they break nothing of the living world; they did, before the sweep of
		// 2026-10-23).
		if (s.streaking && s.heat >= ABLAZE && !Incorporeal.is(p) && p.serverLevel().getGameRules().getBoolean(ModGameRules.METEOR_FLIGHT_SMASHES)) {
			int cut = carve(p, p.serverLevel(), from, to, ahead(p, speed));
			if (cut > 0 && now - s.soundAt >= 5) {
				s.soundAt = now;
				p.serverLevel().playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.3f + p.getRandom().nextFloat() * 0.3f);
			}
		}
		sync(p, s);
	}

	/**
	 * How far ahead of the body the ablaze tear the way open (blocks): the blocks must be gone on the flier's own client before it
	 * gets there, so two ticks' flight and the time the word takes to reach it (its latency), at least {@link #LEAST_AHEAD}.
	 */
	private static double ahead(ServerPlayer p, double speed) {
		return Math.min(MOST_AHEAD, Math.max(LEAST_AHEAD, speed * (2 + p.latency / 50.0)));
	}

	/**
	 * Whatever stands in a tube {@link #TUBE_RADIUS} round the body's middle, from where it was ({@code from}) to where it is and
	 * {@code ahead} blocks on the way it goes, taken away for good, as the great spells' wounds are (HeavenSpells#carvable), at
	 * most {@link #MOST_A_TICK}; a piece or two flung off. Land not loaded is left as it is. @return how many went
	 */
	static int carve(ServerPlayer p, ServerLevel level, Vec3 from, Vec3 to, double ahead) {
		double middle = p.getBbHeight() * 0.5;
		Vec3 path = to.subtract(from);
		Vec3 unit = path.lengthSqr() > 1.0e-4 ? path.normalize() : p.getViewVector(1.0f);
		Vec3 a = from.add(0, middle, 0);
		Vec3 line = to.add(0, middle, 0).add(unit.scale(ahead)).subtract(a);
		double length = line.length();
		if (length < 1.0e-3) return 0;
		Vec3 along = line.scale(1 / length);
		int r = (int) Math.ceil(TUBE_RADIUS);
		double rr = TUBE_RADIUS * TUBE_RADIUS;
		Set<Long> seen = new HashSet<>();
		BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
		int cut = 0, flung = 0;
		for (double t = 0; t <= length + 0.5 && cut < MOST_A_TICK; t += 1.0) {
			BlockPos centre = BlockPos.containing(a.add(along.scale(Math.min(t, length))));
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						cell.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
						if (!seen.add(cell.asLong())) continue;
						double rx = cell.getX() + 0.5 - a.x, ry = cell.getY() + 0.5 - a.y, rz = cell.getZ() + 0.5 - a.z;
						double s = rx * along.x + ry * along.y + rz * along.z;
						if (s < -0.5 || s > length + 0.5) continue;
						double px = rx - along.x * s, py = ry - along.y * s, pz = rz - along.z * s;
						if (px * px + py * py + pz * pz > rr) continue;
						if (!level.isLoaded(cell)) continue;
						BlockState state = level.getBlockState(cell);
						if (state.isAir() || !HeavenSpells.carvableAnyway(level, cell, p)) continue;
						BlockPos pos = cell.immutable();
						level.removeBlock(pos, false);
						cut++;
						// A piece or two flung off to the side as it goes through.
						if (flung < 2 && level.random.nextInt(24) == 0) {
							flung++;
							Vec3 side = along.cross(new Vec3(level.random.nextGaussian(), level.random.nextGaussian(), level.random.nextGaussian()));
							side = side.lengthSqr() < 1.0e-4 ? new Vec3(0, 1, 0) : side.normalize();
							SpellBlock.show(level, HeavenSpells.asEarth(state), Vec3.atBottomCenterOf(pos), side.scale(4 + level.random.nextDouble() * 6)
									.add(along.scale(3)).add(0, 1 + level.random.nextDouble() * 2, 0), 0, 12 + level.random.nextInt(8), SpellBlock.Motion.FLY, 0);
						}
						if (cut >= MOST_A_TICK) return cut;
					}
				}
			}
		}
		return cut;
	}

	/**
	 * From the flier's client (C2S meteor_doze): the blocks its bulldozer cleared ahead of it (Bulldozer#sweep), already gone on its
	 * screen. Those it may take go here too (flying in Meteor Flight with forward and the boost held, the gamerule on, going fast as the
	 * server sees it ({@link #DOZE_SPEED_SHARE}), in the cone ahead of where it looks ({@link #DOZE_SIDE}) near enough to where it was
	 * last seen, at most Bulldozer#MOST_A_TICK a tick however many words come, and as the great spells' wounds may: HeavenSpells#carvable),
	 * a few crumbling for those near; then the word is acknowledged up to {@code sequence}, and whatever wasn't taken comes back on its
	 * screen (vanilla's block prediction). Before the sweep of 2026-10-23 any block within 72 of a hovering flier was taken on its
	 * word, 384 a packet, as many packets as it sent. @return how many went
	 */
	public static int bulldoze(ServerPlayer p, int sequence, List<BlockPos> asked) {
		int cut = 0;
		try {
			ServerLevel level = p.serverLevel();
			if (!mayStreak(p, CultivationManager.get(p)) || Incorporeal.is(p) || !level.getGameRules().getBoolean(ModGameRules.METEOR_FLIGHT_SMASHES)) return 0;
			State s = STATES.get(p.getUUID());
			Vec3 look = Vec3.directionFromRotation(p.getXRot(), p.getYRot()); // the look as its moves send it (not the head's, which lags)
			// Slower than the bulldozer wants, only a steep dive's burrow is taken (Bulldozer#burrowing: at any speed, as its client's,
			// MeteorFlightClient#travel; toast's of 2026-10-24), and only close: its way down and its lead at that speed, not the slack a
			// fast flier's lagging moves need (a hovering flier's word for a cone of land below it is still not taken).
			boolean slow = s != null && s.speed < Bulldozer.FROM_SPEED * DOZE_SPEED_SHARE;
			if (s == null || !s.wants || !s.flatOut || slow && !Bulldozer.burrowing(look)) return 0;
			long now = level.getGameTime();
			if (s.dozedAt != now) {
				s.dozedAt = now;
				s.dozed = 0;
			}
			Vec3 middle = p.position().add(0, p.getBbHeight() * 0.5, 0);
			double reach = slow ? Bulldozer.reach(Bulldozer.FROM_SPEED) : Math.min(Bulldozer.MOST_REACH, Bulldozer.reach(s.speed)) + Bulldozer.SLACK;
			int shown = 0;
			for (BlockPos pos : asked) {
				if (s.dozed >= Bulldozer.MOST_A_TICK) break;
				Vec3 rel = Vec3.atCenterOf(pos).subtract(middle);
				double along = rel.dot(look), side = DOZE_SIDE + s.speed + DOZE_WIDENS * Math.max(0, along);
				if (along < -DOZE_BEHIND - s.speed || along > reach || rel.lengthSqr() - along * along > side * side || !level.isLoaded(pos)) continue;
				BlockState state = level.getBlockState(pos);
				if (state.getCollisionShape(level, pos).isEmpty() || !HeavenSpells.carvableAnyway(level, pos, p)) continue;
				level.removeBlock(pos, false);
				cut++;
				s.dozed++;
				// Crumbling, for those near (the flier saw its own already): a few, not every block.
				if (shown < Bulldozer.SHOWN_A_TICK && level.random.nextInt(3) == 0) {
					shown++;
					level.levelEvent(p, 2001, pos, Block.getId(state));
				}
			}
			return cut;
		} finally {
			// Never below 0 (vanilla throws on it: a forged word's).
			if (p.connection != null) p.connection.ackBlockChangesUpTo(Math.max(0, sequence));
		}
	}

	/** The sound barrier broken: a boom where it broke, and a ring of air there for all near to see (MeteorFlightClient). */
	private static void boom(ServerPlayer p, State s, Vec3 from, Vec3 to) {
		s.booms++;
		Vec3 dir = to.subtract(from);
		dir = dir.lengthSqr() < 1.0e-6 ? p.getViewVector(1.0f) : dir.normalize();
		Vec3 at = to.add(0, p.getBbHeight() * 0.5, 0);
		ServerLevel level = p.serverLevel();
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 1.6f);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0f, 0.8f);
		ModPackets.broadcastMeteorBoom(p, at, dir);
	}

	/**
	 * Those watching (and the flier) told whether it streaks and how hot it is, and whether the gamerule lets its bulldozer clear the
	 * way: as that changes, and now and then while it lasts.
	 */
	private static void sync(ServerPlayer p, State s) {
		boolean ablaze = s.heat >= ABLAZE;
		boolean smashes = p.serverLevel().getGameRules().getBoolean(ModGameRules.METEOR_FLIGHT_SMASHES);
		s.sinceSent++;
		boolean changed = s.streaking != s.sentStreaking || ablaze != s.sentAblaze || s.heat == 0 && s.sentHeat > 0 || smashes != s.sentSmashes;
		boolean beat = (s.streaking || s.heat > 0) && s.sinceSent >= SYNC_EVERY;
		if (!changed && !beat) return;
		s.sentStreaking = s.streaking;
		s.sentAblaze = ablaze;
		s.sentHeat = s.heat;
		s.sentSmashes = smashes;
		s.sinceSent = 0;
		ModPackets.broadcastMeteorState(p, s.streaking, s.heat, smashes);
	}

	public static void forget(UUID id) {
		STATES.remove(id);
	}

	private MeteorFlight() {}
}
