package com.example.defyingtheheavens;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * The everyday life of NPC cultivators, around their duties (CultivatorGoals): what makes a sect feel lived in.
 * <ul>
 *   <li><b>A sect's day</b> ({@link Phase}). A bell rings at dawn. In the morning an Elder (or, with none, the Sect Master)
 *   teaches in the main hall, standing before the dais, the disciples seated in rows before it; with no room there, on the plaza.
 *   Then the working day: duties, and between them talk and practice. A drum sounds at dusk, and at night the disciples sleep
 *   on their mats (from Core Formation they no longer need sleep, and meditate through the night). Where there are outer
 *   disciples enough, the gates have a night watch (Sect#guardPost), who sleep in after it.</li>
 *   <li><b>Among themselves.</b> Juniors bow to a passing Elder or the Sect Master, who may nod back. Two idle disciples stop
 *   to talk (their cultivation, their duties, their elders, a visitor, a rumour going round: real ones, from Rumours); two
 *   on neighbouring mats whisper; elders talk quietly at their posts; rogues who meet on the road pass a few words.
 *   Disciples practise sword forms alone, or spar in pairs on the plaza, and others stop to watch. When a member falls, those
 *   near cry out, and for half a day after they go to stand where it happened.</li>
 *   <li><b>Speech</b> (NpcSpeech; the lines are in NpcLines): a remark to a player passing close (calling them as NpcAddress
 *   has it: by their title, else by how far above or below it they stand; warmly if they are liked, coldly if not; with a bow
 *   to a senior, a deep one two realms beneath them, kneeling from four; a gate guard welcomes or halts you; one who needn't
 *   bow low speaks of the treasure in your hand, your wounds, your flight, your beast's head), on the rain and thunder, a
 *   tribulation nearby, at dawn and dusk, shouts when the sect is attacked, cheers for a breakthrough, and now and then a
 *   sleeper's mumble.</li>
 * </ul>
 * What each is doing is its {@link Activity}, carried out by CultivatorGoals.LifeGoal; anything more pressing (a fight, a
 * reason to run, a player's talk) interrupts it, and it picks it up again after. None of it is saved: it is worked out again
 * every second from the time of day. The routine (sleep, the lecture) goes on whoever is about; talk and practice only start
 * where a player is near enough to see them ({@link #WATCHED}).
 */
public final class NpcLife {
	/** A Minecraft day, in ticks. */
	private static final long DAY_LENGTH = 24_000L;
	/** Talk and practice start only within this distance of a player (sideways, from a sect's grounds or a rogue). */
	public static final double WATCHED = 64;
	/** How near a player passes for a cultivator to remark on them; a sneaking player, much nearer. */
	public static final double REMARK_RANGE = 6;
	public static final double SNEAK_REMARK_RANGE = 2.5;
	/** A cultivator remarks on the same player at most this often (ticks). */
	public static final long REMARK_EVERY = 3600;
	/** How far off a player's tribulation is cried out at by those who see it. */
	public static final double TRIBULATION_SEEN = 40;
	/** A junior bows to the same senior at most this often. */
	public static final long BOW_EVERY = 6000;
	/** How long the fallen are mourned where they fell. */
	public static final long MOURNING = 12_000;
	/** A walk to an activity that takes longer than this is given up. */
	public static final int WALK_LIMIT = 600;
	/** Ticks between the beats of a practice bout: one lunges, the other parries, then the other way round. */
	public static final int BEAT = 16;

	/** The parts of a sect's day, by the time of day (0 is sunrise). */
	public enum Phase {
		DAWN, LECTURE, DAY, EVENING, NIGHT;

		public static Phase of(long timeOfDay) {
			long t = Math.floorMod(timeOfDay, DAY_LENGTH);
			if (t < 1000) return DAWN;
			if (t < 4000) return LECTURE;
			if (t < 11000) return DAY;
			if (t < 13000) return EVENING;
			return NIGHT;
		}
	}

	/** What a cultivator can be doing of its everyday life. */
	public enum Kind {
		SLEEP, REST, LISTEN, LECTURE, CHAT, SPAR, DRILL, MOURN,
		/** Kneeling at the sect's Altar of Heaven for the Ritual to the Heavens (see HeavenRitual). */
		RITUAL;

		/** Something done with free time, which the routine (sleep, the lecture) or anything else replaces. */
		public boolean leisure() { return this == CHAT || this == SPAR || this == DRILL || this == MOURN; }
	}

	/** One cultivator's current activity (see CultivatorGoals.LifeGoal, which carries it out). */
	public static final class Activity {
		public final Kind kind;
		/** Where to be; null for a talk where it walks up to the other. */
		public BlockPos spot;
		/** What to face once there (null: wherever). */
		public Vec3 face;
		/** The other one, in a talk or a bout. */
		public UUID partner;
		/** Sitting there in meditation rather than standing. */
		public boolean seated;
		/** Game time it ends. */
		public long until;
		/** It has got there (set back if it is pulled away, or interrupted). */
		public boolean arrived;
		/** Ticks spent getting there. */
		public int walked;
		/** In a bout: when it begins (0 until both are in place), and whether this one leads it (keeps time, calls the result). */
		public long boutStart;
		public boolean lead;
		/** Mourning: who for. */
		public String mourned;
		/** The ritual: the altar it kneels before, when the kneeling began (0: not yet there), and its chance. */
		public BlockPos altar;
		public long ritualStart;
		public double chance;

		public Activity(Kind kind, BlockPos spot, long until) {
			this.kind = kind;
			this.spot = spot;
			this.until = until;
		}
	}

	/** Someone who fell on a sect's grounds, mourned there until {@code until}. */
	private record Fallen(BlockPos pos, String name, long until) {}

	/** Where a lecture is given: where the teacher stands, and the listeners' seats in rows before it. */
	record LectureLayout(BlockPos lectern, List<BlockPos> seats) {
		Vec3 middle() {
			double x = 0, y = 0, z = 0;
			for (BlockPos seat : seats) {
				x += seat.getX() + 0.5;
				y += seat.getY();
				z += seat.getZ() + 0.5;
			}
			return new Vec3(x / seats.size(), y / seats.size() + 1.0, z / seats.size());
		}
	}

	/** A morning's lecture in progress. */
	private static final class Lecture {
		UUID teacher;
		LectureLayout layout;
		final Map<UUID, BlockPos> seated = new HashMap<>();
		long nextLine;
		boolean opened;
		boolean answerDue;
		boolean questionAsked;
	}

	/** What a sect's day has come to (not saved). */
	static final class SectDay {
		Phase phase;
		long lectureDay = -1;
		Lecture lecture;
		int bells;
		int drums;
		long nextStrike;
		final List<Fallen> fallen = new ArrayList<>();
		String visitor;
		long visitorAt;
		long lastAlarm = -1000;
	}

	/** A talk between two: its lines' keys (the one who began first, then by turns), its subject, and how far it has got. */
	static final class Conversation {
		final CultivatorNpc a, b;
		final List<String> keys;
		final Object subject;
		final boolean seated;
		final long begun;
		int next;
		long nextAt;
		long endsAt = -1;

		Conversation(CultivatorNpc a, CultivatorNpc b, List<String> keys, Object subject, boolean seated, long begun) {
			this.a = a;
			this.b = b;
			this.keys = keys;
			this.subject = subject;
			this.seated = seated;
			this.begun = begun;
		}
	}

	private static final Map<UUID, SectDay> SECTS = new HashMap<>();
	private static final List<Conversation> CONVERSATIONS = new ArrayList<>();
	/** Each dimension's weather a second ago: bit 1 raining, bit 2 thundering. */
	private static final Map<ResourceKey<Level>, Integer> WEATHER = new HashMap<>();

	// =====================================================================================================================
	// Ticking
	// =====================================================================================================================

	public static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % 5 == 0) advanceConversations();
		if (tick % 20 != 0) return;
		for (ServerLevel level : server.getAllLevels()) {
			List<CultivatorNpc> npcs = CultivatorNpc.loaded(level);
			if (npcs.isEmpty()) continue;
			long now = level.getGameTime();
			Phase phase = Phase.of(timeOfDay(level));
			Map<UUID, List<CultivatorNpc>> bySect = new LinkedHashMap<>();
			List<CultivatorNpc> rogues = new ArrayList<>();
			for (CultivatorNpc npc : npcs) {
				expire(npc, now);
				if (npc.getLifecycle() == CultivatorNpc.Lifecycle.SECT && npc.getSectId() != null) {
					bySect.computeIfAbsent(npc.getSectId(), id -> new ArrayList<>()).add(npc);
				} else if (npc.getLifecycle() == CultivatorNpc.Lifecycle.ROGUE) {
					rogues.add(npc);
				}
			}
			weather(level, npcs);
			tribulations(level, npcs, now);
			for (Map.Entry<UUID, List<CultivatorNpc>> entry : bySect.entrySet()) {
				Sect sect = SectManager.get(server, entry.getKey());
				if (sect != null && !sect.extinct) live(level, sect, entry.getValue(), phase, now);
			}
			rogues(level, rogues, phase, now);
			for (CultivatorNpc npc : npcs) remark(level, npc, now);
		}
	}

	public static void clear() {
		SECTS.clear();
		CONVERSATIONS.clear();
		WEATHER.clear();
	}

	/** The time of day in {@code level} (a dimension whose sky stands still keeps its fixed hour). */
	public static long timeOfDay(Level level) {
		OptionalLong fixed = level.dimensionType().fixedTime();
		return Math.floorMod(fixed.isPresent() ? fixed.getAsLong() : level.getDayTime(), DAY_LENGTH);
	}

	public static boolean isNight(Level level) {
		return Phase.of(timeOfDay(level)) == Phase.NIGHT;
	}

	/** Ticks from {@code timeOfDay} to the next {@code target} time of day. */
	private static long ticksUntil(long timeOfDay, long target) {
		return Math.floorMod(target - timeOfDay, DAY_LENGTH);
	}

	/** An activity whose time is up ends; so does a walk to one that took too long. */
	private static void expire(CultivatorNpc npc, long now) {
		Activity a = npc.activity();
		if (a == null) return;
		if (a.walked > WALK_LIMIT && !a.arrived) {
			end(npc);
		} else if (now >= a.until) {
			if (a.kind == Kind.RITUAL && a.ritualStart > 0) answerRitual(npc, a);
			// A bout is the leader's to call (it ends for both); the other only gives up on it if the leader never does.
			if (a.kind == Kind.SPAR && !a.lead && now < a.until + 100) return;
			if (a.kind == Kind.SPAR && a.lead) finishBout(npc, a);
			if (a.kind == Kind.SLEEP && npc.level() instanceof ServerLevel level && level.random.nextInt(4) == 0 && NpcSpeech.heard(npc)) {
				NpcSpeech.say(npc, NpcLines.line("dawn", npc));
			}
			end(npc);
		}
	}

	// =====================================================================================================================
	// Who's free, and where they are
	// =====================================================================================================================

	/**
	 * Not busy with anything that matters more: a fight (or the moments after one), a reason to run, a flight, a player's talk
	 * or trade, begging or kneeling, a spar with a player, another's soul inside it; and on its sect's grounds, if it has one.
	 */
	public static boolean free(CultivatorNpc npc) {
		return free(npc, false);
	}

	/** As {@link #free(CultivatorNpc)}; but if {@code kneeling}, kneeling to one passing by needn't stop it (it may still speak to them). */
	private static boolean free(CultivatorNpc npc, boolean kneeling) {
		if (!npc.isAlive() || npc.isRemoved() || npc.getTarget() != null || npc.isInCombat() || npc.shouldFlee() || npc.talkPartner() != null
				|| npc.isBegging() || npc.isKneeling() && !kneeling || npc.getTradingPlayer() != null || SparSystem.isSparring(npc)
				|| SoulProjection.isPossessed(npc) || npc.isQiFlying() || npc.isConcealed()) return false;
		if (npc.getLifecycle() == CultivatorNpc.Lifecycle.ROGUE) return true;
		if (npc.getLifecycle() != CultivatorNpc.Lifecycle.SECT) return false;
		Sect sect = npc.sect();
		return sect != null && sect.inTerritory(npc.blockPosition());
	}

	/** Whether CultivatorGoals.LifeGoal should be carrying out its activity now. */
	public static boolean canLive(CultivatorNpc npc) {
		return npc.activity() != null && free(npc);
	}

	/** A disciple: an inner or outer one. */
	private static boolean disciple(CultivatorNpc npc) {
		return npc.getTitle() == NpcTitle.INNER_DISCIPLE || npc.getTitle() == NpcTitle.OUTER_DISCIPLE;
	}

	/** Free, with nothing else on, and not keeping a gate (a sect's, or the Ghost Gate). */
	private static boolean idle(CultivatorNpc npc) {
		return npc.activity() == null && !npc.isGateGuard() && !npc.keepsPost() && !npc.hasPose(Pose.SLEEPING) && free(npc);
	}

	/** Somewhere to stand: room for feet and head (a carpet or a mat underfoot will do), and something under the feet. */
	static boolean standable(Level level, BlockPos feet) {
		if (!level.isLoaded(feet) || !level.isLoaded(feet.above())) return false;
		BlockState at = level.getBlockState(feet);
		BlockState head = level.getBlockState(feet.above());
		if (!at.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
		VoxelShape shape = at.getCollisionShape(level, feet);
		if (!shape.isEmpty() && shape.max(Direction.Axis.Y) > 0.2) return false;
		if (!head.getCollisionShape(level, feet.above()).isEmpty()) return false;
		return !shape.isEmpty() || level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
	}

	/** Under a roof. */
	static boolean roofed(Level level, BlockPos feet) {
		return !level.canSeeSky(feet.above());
	}

	/** Whether a player (not a spectator) is within {@code range} of {@code pos}, sideways. */
	private static boolean watched(ServerLevel level, Vec3 pos, double range) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - pos.x, dz = player.getZ() - pos.z;
			if (dx * dx + dz * dz <= range * range) return true;
		}
		return false;
	}

	private static String name(CultivatorNpc npc) {
		return npc.getPersonalName();
	}

	private static CultivatorNpc partner(CultivatorNpc npc, Activity a) {
		if (a.partner == null || !(npc.level() instanceof ServerLevel level)) return null;
		return level.getEntity(a.partner) instanceof CultivatorNpc other && other.isAlive() ? other : null;
	}

	// =====================================================================================================================
	// Starting and ending
	// =====================================================================================================================

	static void begin(CultivatorNpc npc, Activity activity) {
		if (npc.activity() != null) end(npc);
		npc.setActivity(activity);
	}

	/** Ends what {@code npc} is doing; a talk or a bout ends for the other one too. */
	public static void end(CultivatorNpc npc) {
		Activity a = npc.activity();
		if (a == null) return;
		npc.setActivity(null);
		if (a.kind == Kind.CHAT || a.kind == Kind.SPAR) {
			CONVERSATIONS.removeIf(c -> c.a == npc || c.b == npc);
			CultivatorNpc other = partner(npc, a);
			if (other != null && other.activity() != null && npc.getUUID().equals(other.activity().partner)) other.setActivity(null);
		}
	}

	/** CultivatorGoals.LifeGoal stopped before the activity was over (a fight, a player's talk...): a talk or a bout is off. */
	public static void interrupted(CultivatorNpc npc) {
		Activity a = npc.activity();
		if (a == null) return;
		if (a.kind == Kind.CHAT || a.kind == Kind.SPAR || a.kind == Kind.RITUAL && a.ritualStart > 0) {
			end(npc); // a ritual broken off is lost, its offering with it
		} else {
			a.arrived = false; // back to its place when it resumes
		}
	}

	/** It has got to where its activity is. */
	static void arrived(CultivatorNpc npc, Activity a) {
		if (a.kind == Kind.MOURN && a.mourned != null && NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("mourn", npc, a.mourned));
		if (a.kind == Kind.RITUAL && a.ritualStart == 0 && npc.level() instanceof ServerLevel level) beginRitual(level, npc, a);
	}

	// =====================================================================================================================
	// A sect's day
	// =====================================================================================================================

	/** A second of a sect's life: its loaded {@code members}, in {@code phase}. Package-private for the game tests. */
	static void live(ServerLevel level, Sect sect, List<CultivatorNpc> members, Phase phase, long now) {
		SectDay day = SECTS.computeIfAbsent(sect.id, id -> new SectDay());
		if (day.phase != phase) {
			if (day.phase != null) turn(level, sect, day, members, day.phase, phase, now);
			day.phase = phase;
		}
		strike(level, sect, day, now);
		routine(level, sect, members, phase, now);
		rituals(level, sect, members, phase, now);
		if (phase == Phase.LECTURE) lecture(level, sect, day, members, now);
		else if (day.lecture != null) closeLecture(level, sect, day, members, now);
		boolean watched = watched(level, Vec3.atCenterOf(sect.core), sect.barrierRadius + WATCHED);
		if (!watched) return;
		mourning(level, sect, day, members, phase, now);
		if (phase != Phase.NIGHT && phase != Phase.LECTURE) social(level, sect, day, members, phase, now);
		bows(level, members, now);
	}

	/** The day turns: the bell at dawn, the drum at dusk. */
	private static void turn(ServerLevel level, Sect sect, SectDay day, List<CultivatorNpc> members, Phase from, Phase to, long now) {
		if (to == Phase.DAWN && from == Phase.NIGHT) {
			day.bells = 3;
			day.nextStrike = now;
		} else if (to == Phase.NIGHT) {
			day.drums = 3;
			day.nextStrike = now;
			for (CultivatorNpc npc : members) {
				if (level.random.nextInt(4) == 0 && !npc.hasPose(Pose.SLEEPING) && free(npc) && NpcSpeech.mayRemark(npc) && NpcSpeech.heard(npc)) {
					NpcSpeech.say(npc, NpcLines.line("dusk", npc));
				}
			}
		}
	}

	/** Strikes the dawn bell or the dusk drum, three times, from the main hall. */
	private static void strike(ServerLevel level, Sect sect, SectDay day, long now) {
		if (day.bells <= 0 && day.drums <= 0 || now < day.nextStrike) return;
		BlockPos at = sect.posts(Sect.Post.MASTER_SEAT).isEmpty() ? sect.core : sect.post(Sect.Post.MASTER_SEAT, 0);
		if (!level.isLoaded(at)) return;
		if (day.bells > 0) {
			level.playSound(null, at, SoundEvents.BELL_BLOCK, SoundSource.AMBIENT, 3.0f, 0.7f);
			day.bells--;
			day.nextStrike = now + 50;
		} else {
			level.playSound(null, at, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.AMBIENT, 3.0f, 0.6f);
			day.drums--;
			day.nextStrike = now + 15;
		}
	}

	/**
	 * What the routine asks of {@code member} now: sleep or rest at home at night (and for the night watch, through the
	 * morning after), or nothing (its duty, the lecture or free time).
	 */
	static Kind routineFor(Sect sect, Sect.Member member, CultivatorNpc npc, Phase phase) {
		if (member.title != NpcTitle.OUTER_DISCIPLE && member.title != NpcTitle.INNER_DISCIPLE) return null;
		boolean dayWatch = sect.guardPost(member, false) != null;
		boolean nightWatch = sect.guardPost(member, true) != null;
		Kind sleep = npc.getRank() < PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) ? Kind.SLEEP : Kind.REST;
		if (phase == Phase.NIGHT) return nightWatch ? null : sleep;
		if ((phase == Phase.DAWN || phase == Phase.LECTURE) && nightWatch && !dayWatch) return sleep; // sleeping in after the watch
		return null;
	}

	private static void routine(ServerLevel level, Sect sect, List<CultivatorNpc> members, Phase phase, long now) {
		long time = timeOfDay(level);
		for (CultivatorNpc npc : members) {
			Sect.Member member = sect.members.get(npc.getUUID());
			if (member == null) continue;
			Kind wanted = routineFor(sect, member, npc, phase);
			Activity a = npc.activity();
			if (a != null && (a.kind == Kind.SLEEP || a.kind == Kind.REST) && wanted == null) {
				// Morning (or the clock jumped): up within the next half a minute.
				a.until = Math.min(a.until, now + level.random.nextInt(600));
				continue;
			}
			if (wanted == null || a != null && !a.kind.leisure() || !free(npc)) continue;
			long wake = phase == Phase.NIGHT ? ticksUntil(time, 0) : ticksUntil(time, 4000);
			Activity rest = new Activity(wanted, sect.dwelling(member.home), now + wake + level.random.nextInt(800));
			rest.seated = wanted == Kind.REST;
			if (npc.isGateGuard()) {
				// Its watch is over: the next one takes the gate.
				npc.setGateGuard(false);
				if (NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("watch.end", npc));
			}
			begin(npc, rest);
		}
	}

	// --- The Ritual to the Heavens ---

	/**
	 * Members at Qi Refining Mid's gate (CultivatorNpc#awaitsHeavensLeave) go to the sect's Altar of Heaven, one at a time,
	 * outside the lecture and the night, and not again the day the heavens turned them away; there they kneel (HeavenRitual#DURATION)
	 * on an offering from the sect's store (Sect#takeOffering), as a player would (HeavenRitual). A sect laid out before the
	 * altar existed kneels by its Formation Core instead. Those already kneeling send up smoke and gathering light.
	 */
	private static void rituals(ServerLevel level, Sect sect, List<CultivatorNpc> members, Phase phase, long now) {
		for (CultivatorNpc npc : members) {
			Activity a = npc.activity();
			if (a != null && a.kind == Kind.RITUAL && a.ritualStart > 0 && a.altar != null) {
				double progress = Math.min(1.0, (now - a.ritualStart) / (double) HeavenRitual.DURATION);
				level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, a.altar.getX() + 0.5, a.altar.getY() + 1.0, a.altar.getZ() + 0.5, 1, 0.05, 0, 0.05, 0.01);
				for (int i = 0; i < 1 + (int) (progress * 6); i++) {
					double h = 3 + level.random.nextDouble() * (4 + 10 * progress);
					level.sendParticles(ParticleTypes.END_ROD, a.altar.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, a.altar.getY() + h,
							a.altar.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.6, 1, 0, -0.05, 0, 0.02);
				}
			}
		}
		if (phase == Phase.NIGHT || phase == Phase.LECTURE) return;
		BlockPos altar = sect.altar() != null ? sect.altar() : sect.core;
		if (!level.isLoaded(altar) || HeavenRitual.isAltarInUse(level, altar)) return;
		long today = HeavenRitual.today(level.getServer());
		for (CultivatorNpc npc : members) {
			if (!npc.awaitsHeavensLeave() || npc.ritualFailedDay() >= today || !free(npc)) continue;
			Activity a = npc.activity();
			if (a != null && !a.kind.leisure()) continue;
			BlockPos kneel = sect.altarKneel() != null ? sect.altarKneel() : besideCore(level, sect);
			if (kneel == null) return;
			Activity ritual = new Activity(Kind.RITUAL, kneel, now + WALK_LIMIT + HeavenRitual.DURATION);
			ritual.altar = altar;
			ritual.face = Vec3.atCenterOf(altar);
			ritual.seated = true;
			if (npc.isGateGuard()) npc.setGateGuard(false);
			begin(npc, ritual);
			return; // one at a time
		}
	}

	/** Somewhere to kneel by the Formation Core, for a sect with no altar. */
	private static BlockPos besideCore(ServerLevel level, Sect sect) {
		for (int[] d : new int[][] {{0, 3}, {3, 0}, {0, -3}, {-3, 0}, {2, 2}, {-2, -2}}) {
			for (int dy = -1; dy <= 1; dy++) {
				BlockPos at = sect.core.offset(d[0], dy, d[1]);
				if (standable(level, at)) return at;
			}
		}
		return null;
	}

	/** It kneels before the altar: the sect's offering is taken and the kneeling begins. */
	private static void beginRitual(ServerLevel level, CultivatorNpc npc, Activity a) {
		Sect sect = npc.sect();
		long now = level.getGameTime();
		double offered = sect == null ? 0 : sect.takeOffering(HeavenRitual.today(level.getServer()));
		HeavenAltar.Offering offering = HeavenAltar.Offering.of(offered);
		HeavenAltar.Grandeur grandeur = HeavenAltar.Grandeur.HUMBLE;
		if (a.altar != null && level.getBlockState(a.altar).is(ModBlocks.HEAVEN_ALTAR)) {
			HeavenAltar.Judgement judgement = HeavenAltar.judge(level, a.altar);
			if (judgement.sky()) grandeur = judgement.grandeur();
		}
		a.chance = HeavenAltar.chance(offering, grandeur, HeavenAltar.heightBonus(level, a.altar != null ? a.altar : npc.blockPosition()));
		a.ritualStart = now;
		a.until = now + HeavenRitual.DURATION;
		level.playSound(null, npc.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.5f, 0.7f);
		if (NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("ritual.begin", npc));
	}

	/** Three minutes knelt: the heavens answer (and a member granted their leave steps into Qi Refining Late). */
	private static void answerRitual(CultivatorNpc npc, Activity a) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		boolean granted = level.random.nextDouble() < a.chance;
		BlockPos at = a.altar != null ? a.altar : npc.blockPosition();
		if (granted) {
			for (int i = 0; i < 30; i++) {
				level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1 + i * 0.6, at.getZ() + 0.5, 2, 0.15, 0.2, 0.15, 0.01);
			}
			level.playSound(null, at, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8f, 1.2f);
			if (NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("ritual.granted", npc));
			npc.grantHeavensLeave();
		} else {
			npc.setRitualFailedDay(HeavenRitual.today(level.getServer()));
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1.2, at.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.01);
			level.playSound(null, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.4f, 0.7f);
			if (NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("ritual.denied", npc));
		}
	}

	/** For the tests: {@code a} and {@code b} go to spar, and {@code driller} to drill, now. */
	static void practiseForTest(ServerLevel level, Sect sect, CultivatorNpc a, CultivatorNpc b, CultivatorNpc driller, long now) {
		startBout(level, sect, a, b, now);
		startDrill(level, sect, driller, now);
	}

	/** For the game tests: the heavens answer {@code npc}'s ritual now, with {@code chance}, and it gets up. */
	static void answerRitualForTest(CultivatorNpc npc, double chance) {
		Activity a = npc.activity();
		if (a == null || a.kind != Kind.RITUAL) return;
		a.chance = chance;
		answerRitual(npc, a);
		end(npc);
	}

	/** A cultivator is kneeling (or on its way to kneel) at the altar at {@code pos}. */
	public static boolean isAltarInUse(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel server)) return false;
		for (CultivatorNpc npc : CultivatorNpc.loaded(server)) {
			Activity a = npc.activity();
			if (a != null && a.kind == Kind.RITUAL && pos.equals(a.altar)) return true;
		}
		return false;
	}

	/**
	 * A ritual worked out without being seen (a cultivator catching up on time spent unloaded, or a rogue at some roadside
	 * altar): a sect member offers from the sect's store at its altar (as grand as the sect was built), a rogue from what it
	 * carries at a humble one. Not on a day the heavens already turned it away. @return whether the heavens granted their leave
	 */
	public static boolean ritualOffscreen(ServerLevel level, CultivatorNpc npc) {
		return ritualOffscreen(level, npc, HeavenRitual.today(level.getServer()));
	}

	/** The same, worked out for {@code today} (a cultivator catching up asks once for each day it was away). */
	public static boolean ritualOffscreen(ServerLevel level, CultivatorNpc npc, long today) {
		if (!npc.awaitsHeavensLeave() || npc.ritualFailedDay() >= today) return false;
		Sect sect = npc.sect();
		HeavenAltar.Offering offering;
		HeavenAltar.Grandeur grandeur;
		BlockPos at;
		if (sect != null) {
			offering = HeavenAltar.Offering.of(sect.takeOffering(today));
			grandeur = sect.altarGrandeur();
			at = sect.altar() != null ? sect.altar() : sect.core;
		} else {
			offering = HeavenAltar.Offering.PROPER;
			grandeur = HeavenAltar.Grandeur.HUMBLE;
			at = npc.blockPosition();
		}
		double chance = HeavenAltar.chance(offering, grandeur, HeavenAltar.heightBonus(level, at));
		if (level.random.nextDouble() < chance) {
			npc.grantHeavensLeave();
			return true;
		}
		npc.setRitualFailedDay(today);
		return false;
	}

	// --- The lecture ---

	private static void lecture(ServerLevel level, Sect sect, SectDay day, List<CultivatorNpc> members, long now) {
		long today = level.getDayTime() / DAY_LENGTH;
		long end = now + ticksUntil(timeOfDay(level), 4000) + 100;
		if (day.lecture == null) {
			if (day.lectureDay == today) return; // given (or not to be had) today
			LectureLayout layout = lectureLayout(level, sect);
			CultivatorNpc teacher = layout == null ? null : teacher(sect, members, today);
			if (teacher == null) {
				// No room for one at all: none today. Otherwise (the hall not loaded yet, no teacher free yet) try again shortly.
				BlockPos hall = sect.posts(Sect.Post.MASTER_SEAT).isEmpty() ? sect.core : sect.post(Sect.Post.MASTER_SEAT, 0);
				if (layout == null && level.isLoaded(sect.core) && level.isLoaded(hall)) day.lectureDay = today;
				return;
			}
			day.lectureDay = today;
			Lecture lecture = new Lecture();
			lecture.teacher = teacher.getUUID();
			lecture.layout = layout;
			lecture.nextLine = now + 200;
			Activity teach = new Activity(Kind.LECTURE, layout.lectern(), end);
			teach.face = layout.middle();
			begin(teacher, teach);
			day.lecture = lecture;
		}
		Lecture lecture = day.lecture;
		// Disciples free to come take the empty seats, front rows first.
		for (CultivatorNpc npc : members) {
			if (lecture.seated.containsKey(npc.getUUID()) || lecture.seated.size() >= lecture.layout.seats().size()) continue;
			Sect.Member member = sect.members.get(npc.getUUID());
			if (member == null || !disciple(npc) || sect.guardPost(member, false) != null || routineFor(sect, member, npc, Phase.LECTURE) != null) continue;
			Activity a = npc.activity();
			if (a != null && !a.kind.leisure() || !free(npc)) continue;
			BlockPos seat = null;
			for (BlockPos s : lecture.layout.seats()) {
				if (!lecture.seated.containsValue(s)) {
					seat = s;
					break;
				}
			}
			if (seat == null) break;
			Activity listen = new Activity(Kind.LISTEN, seat, end);
			listen.seated = true;
			listen.face = Vec3.atBottomCenterOf(lecture.layout.lectern()).add(0, 1.5, 0);
			begin(npc, listen);
			lecture.seated.put(npc.getUUID(), seat);
		}
		// The teacher speaks once it stands at the lectern and someone sits before it.
		CultivatorNpc teacher = level.getEntity(lecture.teacher) instanceof CultivatorNpc t && t.isAlive() ? t : null;
		if (teacher == null || teacher.activity() == null || teacher.activity().kind != Kind.LECTURE || !teacher.activity().arrived) return;
		List<CultivatorNpc> listening = listeners(level, lecture);
		if (listening.isEmpty() || now < lecture.nextLine) return;
		RandomSource random = level.random;
		if (lecture.answerDue) {
			lecture.answerDue = false;
			CultivatorNpc listener = listening.get(random.nextInt(listening.size()));
			if (lecture.questionAsked) {
				lecture.questionAsked = false;
				NpcSpeech.say(teacher, NpcLines.line("lecture", teacher));
			} else {
				NpcSpeech.say(listener, NpcLines.line("lecture.answer", listener));
			}
		} else if (!lecture.opened) {
			lecture.opened = true;
			NpcSpeech.say(teacher, NpcLines.line("lecture.open", teacher));
		} else if (random.nextInt(8) == 0) {
			CultivatorNpc listener = listening.get(random.nextInt(listening.size()));
			NpcSpeech.say(listener, NpcLines.line("lecture.question", listener));
			lecture.answerDue = true;
			lecture.questionAsked = true;
		} else {
			NpcSpeech.say(teacher, NpcLines.line("lecture", teacher));
			lecture.answerDue = random.nextInt(4) == 0;
		}
		lecture.nextLine = now + (lecture.answerDue ? 90 : 200 + random.nextInt(100));
	}

	/** The lecture's listeners in their seats. */
	private static List<CultivatorNpc> listeners(ServerLevel level, Lecture lecture) {
		List<CultivatorNpc> found = new ArrayList<>();
		for (UUID id : lecture.seated.keySet()) {
			if (level.getEntity(id) instanceof CultivatorNpc npc && npc.isAlive() && npc.activity() != null && npc.activity().kind == Kind.LISTEN
					&& npc.activity().arrived) found.add(npc);
		}
		return found;
	}

	/** The morning's teacher: the Elders and Grand Elders take turns by the day; with none of them about, the Sect Master. */
	private static CultivatorNpc teacher(Sect sect, List<CultivatorNpc> members, long today) {
		List<CultivatorNpc> elders = new ArrayList<>();
		CultivatorNpc master = null;
		for (CultivatorNpc npc : members) {
			if (!free(npc) || npc.activity() != null && !npc.activity().kind.leisure()) continue;
			if (npc.getTitle() == NpcTitle.ELDER || npc.getTitle() == NpcTitle.GRAND_ELDER) elders.add(npc);
			else if (npc.getTitle() == NpcTitle.SECT_MASTER) master = npc;
		}
		if (elders.isEmpty()) return master;
		elders.sort(Comparator.comparing(npc -> npc.getUUID()));
		return elders.get((int) Math.floorMod(today, (long) elders.size()));
	}

	private static void closeLecture(ServerLevel level, Sect sect, SectDay day, List<CultivatorNpc> members, long now) {
		Lecture lecture = day.lecture;
		day.lecture = null;
		CultivatorNpc teacher = level.getEntity(lecture.teacher) instanceof CultivatorNpc t && t.isAlive() ? t : null;
		List<CultivatorNpc> listening = listeners(level, lecture);
		if (teacher != null && lecture.opened && !listening.isEmpty()) NpcSpeech.say(teacher, NpcLines.line("lecture.close", teacher));
		for (CultivatorNpc npc : listening) ModPackets.sendGesture(npc, Gesture.BOW);
		for (CultivatorNpc npc : members) {
			Activity a = npc.activity();
			if (a != null && (a.kind == Kind.LECTURE || a.kind == Kind.LISTEN)) end(npc);
		}
	}

	/**
	 * Where a lecture can be given: in the main hall, the teacher a few blocks before the dais facing the door and the
	 * listeners in rows of five down the hall; with no room there (an older sect's layout), on the plaza on the gate's side of
	 * the dais. Null if there is nowhere. Package-private for the game tests.
	 */
	static LectureLayout lectureLayout(ServerLevel level, Sect sect) {
		if (!sect.posts(Sect.Post.MASTER_SEAT).isEmpty()) {
			BlockPos seat = sect.post(Sect.Post.MASTER_SEAT, 0);
			Direction forward = toward(seat, sect.core);
			Direction side = forward.getClockWise();
			BlockPos lectern = seat.relative(forward, 4).below(2);
			if (standable(level, lectern) && roofed(level, lectern)) {
				List<BlockPos> seats = new ArrayList<>();
				for (int d = 6; d <= 30; d += 2) {
					boolean any = false;
					for (int s : new int[] {0, -2, 2, -4, 4}) {
						BlockPos p = seat.relative(forward, d).relative(side, s).below(2);
						if (standable(level, p) && roofed(level, p)) {
							seats.add(p);
							any = true;
						}
					}
					if (!any) break;
				}
				if (seats.size() >= 2) return new LectureLayout(lectern, seats);
			}
		}
		// The plaza: the teacher on the gate's side of the dais, facing the gate, the listeners beyond.
		BlockPos gate = sect.posts(Sect.Post.GATE).isEmpty() ? sect.core.south(20) : sect.post(Sect.Post.GATE, 0);
		Direction out = toward(sect.core, gate);
		Direction side = out.getClockWise();
		BlockPos lectern = sect.core.relative(out, 7).below();
		if (!standable(level, lectern)) return null;
		List<BlockPos> seats = new ArrayList<>();
		for (int d = 9; d <= 11; d += 2) {
			for (int s : new int[] {0, -2, 2, -4, 4}) {
				BlockPos p = sect.core.relative(out, d).relative(side, s).below();
				if (standable(level, p)) seats.add(p);
			}
		}
		return seats.size() >= 2 ? new LectureLayout(lectern, seats) : null;
	}

	/** The horizontal direction (of the four) most nearly from {@code from} to {@code to}. */
	private static Direction toward(BlockPos from, BlockPos to) {
		int dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
		if (Math.abs(dx) >= Math.abs(dz)) return dx >= 0 ? Direction.EAST : Direction.WEST;
		return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	// --- Mourning ---

	/** A cultivator died: if it was a sect's, those near cry out, and its grounds remember the place for a while. */
	public static void onDeath(ServerLevel level, CultivatorNpc npc) {
		if (npc.getSectId() == null) return;
		Sect sect = npc.sect();
		if (sect == null || sect.extinct || !sect.inTerritory(npc.blockPosition())) return;
		long now = level.getGameTime();
		String name = name(npc);
		SECTS.computeIfAbsent(sect.id, id -> new SectDay()).fallen.add(new Fallen(npc.blockPosition(), name, now + MOURNING));
		int cries = 0;
		for (CultivatorNpc other : CultivatorNpc.loadedMembers(level, sect.id)) {
			if (other == npc || other.distanceToSqr(npc) > 32 * 32 || now < other.speakingUntil()) continue;
			if (NpcSpeech.say(other, NpcLines.line("grief", other, name)) && ++cries >= 2) break;
		}
	}

	private static void mourning(ServerLevel level, Sect sect, SectDay day, List<CultivatorNpc> members, Phase phase, long now) {
		day.fallen.removeIf(f -> f.until() <= now);
		if (day.fallen.isEmpty() || phase == Phase.NIGHT || phase == Phase.LECTURE) return;
		for (Fallen fallen : day.fallen) {
			int mourners = 0;
			for (CultivatorNpc npc : members) {
				Activity a = npc.activity();
				if (a != null && a.kind == Kind.MOURN && a.spot.closerThan(fallen.pos(), 4)) mourners++;
			}
			if (mourners >= 2 || level.random.nextInt(15) != 0) continue;
			List<CultivatorNpc> near = new ArrayList<>();
			for (CultivatorNpc npc : members) {
				if (disciple(npc) && idle(npc) && npc.blockPosition().closerThan(fallen.pos(), 48)) near.add(npc);
			}
			if (near.isEmpty()) continue;
			CultivatorNpc mourner = near.get(level.random.nextInt(near.size()));
			BlockPos spot = beside(level, fallen.pos(), level.random);
			if (spot == null) continue;
			Activity mourn = new Activity(Kind.MOURN, spot, now + 300 + WALK_LIMIT);
			mourn.face = Vec3.atBottomCenterOf(fallen.pos()).add(0, 0.1, 0);
			mourn.mourned = fallen.name();
			begin(mourner, mourn);
		}
	}

	/** Somewhere to stand a block or three from {@code pos}. */
	private static BlockPos beside(ServerLevel level, BlockPos pos, RandomSource random) {
		for (int attempt = 0; attempt < 12; attempt++) {
			int r = 1 + attempt / 4;
			BlockPos p = pos.offset(random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
			for (int dy : new int[] {0, 1, -1}) {
				if (!p.equals(pos) && standable(level, p.above(dy))) return p.above(dy);
			}
		}
		return null;
	}

	// --- Talk, practice and greetings ---

	private static void social(ServerLevel level, Sect sect, SectDay day, List<CultivatorNpc> members, Phase phase, long now) {
		RandomSource random = level.random;
		boolean wet = level.isRaining();
		List<CultivatorNpc> standing = new ArrayList<>();
		List<CultivatorNpc> sitting = new ArrayList<>();
		List<CultivatorNpc> elders = new ArrayList<>();
		for (CultivatorNpc npc : members) {
			if (npc.getTitle() == NpcTitle.ELDER || npc.getTitle() == NpcTitle.GRAND_ELDER) {
				if (npc.isMeditating() && npc.activity() == null && free(npc)) elders.add(npc);
			} else if (disciple(npc) && idle(npc)) {
				(npc.isMeditating() ? sitting : standing).add(npc);
			}
		}
		int talks = 0, bouts = 0, drills = 0;
		for (CultivatorNpc npc : members) {
			Activity a = npc.activity();
			if (a == null) continue;
			if (a.kind == Kind.CHAT) talks++;
			else if (a.kind == Kind.SPAR) bouts++;
			else if (a.kind == Kind.DRILL) drills++;
		}
		talks /= 2;
		bouts /= 2;
		// Two disciples stop to talk (under a roof, if it rains).
		if (talks < Math.max(1, members.size() / 6) && random.nextInt(10) == 0 && !standing.isEmpty()) {
			CultivatorNpc a = standing.get(random.nextInt(standing.size()));
			CultivatorNpc b = nearest(a, standing, 24);
			if (b != null && (!wet || roofed(level, a.blockPosition()) && roofed(level, b.blockPosition()))) {
				startTalk(level, sect, day, a, b, false, now);
			}
		}
		// Two on neighbouring mats whisper.
		if (random.nextInt(40) == 0 && sitting.size() >= 2) {
			CultivatorNpc a = sitting.get(random.nextInt(sitting.size()));
			CultivatorNpc b = nearest(a, sitting, 3.5);
			if (b != null) startTalk(level, sect, day, a, b, true, now);
		}
		// Elders at their posts talk quietly.
		if (random.nextInt(60) == 0 && elders.size() >= 2) {
			CultivatorNpc a = elders.get(random.nextInt(elders.size()));
			CultivatorNpc b = nearest(a, elders, 14);
			if (b != null) startTalk(level, sect, day, a, b, true, now);
		}
		if (wet) return; // nobody practises in the rain
		// A bout in the training ground's sparring ring (on the plaza, for a sect without one).
		if (bouts == 0 && random.nextInt(25) == 0 && standing.size() >= 2) {
			CultivatorNpc a = standing.get(random.nextInt(standing.size()));
			CultivatorNpc b = nearest(a, standing, 40);
			if (b != null) startBout(level, sect, a, b, now);
		}
		// Sword forms alone, before the training ground's dummies (up to three at once there).
		if (drills < Math.max(2, Math.min(3, sect.drillSpots().size())) && random.nextInt(20) == 0) {
			List<CultivatorNpc> pool = !standing.isEmpty() ? standing : sitting;
			if (!pool.isEmpty()) {
				CultivatorNpc npc = pool.get(random.nextInt(pool.size()));
				if (npc.activity() == null) startDrill(level, sect, npc, now);
			}
		}
	}

	/** The nearest of {@code among} to {@code npc} within {@code range}, not itself and with nothing on. */
	private static CultivatorNpc nearest(CultivatorNpc npc, List<CultivatorNpc> among, double range) {
		CultivatorNpc best = null;
		double bestDistance = range * range;
		for (CultivatorNpc other : among) {
			if (other == npc || other.activity() != null) continue;
			double d = other.distanceToSqr(npc);
			if (d <= bestDistance) {
				bestDistance = d;
				best = other;
			}
		}
		return best;
	}

	/** {@code a} and {@code b} talk ({@code seated}: where they sit). Package-private for the game tests. */
	static Conversation startTalk(ServerLevel level, Sect sect, SectDay day, CultivatorNpc a, CultivatorNpc b, boolean seated, long now) {
		RandomSource random = level.random;
		String topic;
		Object subject = "";
		String voice = NpcLines.voice(a);
		if (sect == null) {
			topic = "chat.road";
			if (random.nextInt(3) == 0) {
				Component rumour = rumour(a);
				if (rumour != null) {
					topic = "chat.gossip";
					subject = rumour;
				}
			}
		} else if (seated && a.getTitle().isElderOrAbove()) {
			topic = "chat.elders";
		} else if (seated) {
			topic = "chat.whisper";
		} else {
			List<String> topics = new ArrayList<>(List.of("chat.progress", "chat.progress", "chat.duty", "chat.life", "chat.life"));
			String elder = elderName(sect, random);
			if (elder != null) topics.add("chat.elder");
			if (day != null && day.visitor != null && now - day.visitorAt < DAY_LENGTH) topics.add("chat.visitor");
			Component rumour = random.nextInt(2) == 0 ? rumour(a) : null;
			if (rumour != null) topics.add("chat.gossip");
			topic = topics.get(random.nextInt(topics.size()));
			if (topic.equals("chat.elder")) subject = elder;
			else if (topic.equals("chat.visitor")) subject = day.visitor;
			else if (topic.equals("chat.gossip")) subject = rumour;
		}
		List<String> keys = NpcLines.script(topic, voice, random);
		if (keys == null) return null;
		long until = now + 1200;
		Activity first = new Activity(Kind.CHAT, seated ? a.blockPosition() : null, until);
		first.partner = b.getUUID();
		first.seated = seated;
		Activity second = new Activity(Kind.CHAT, b.blockPosition(), until);
		second.partner = a.getUUID();
		second.seated = seated;
		begin(a, first);
		begin(b, second);
		Conversation talk = new Conversation(a, b, keys, subject, seated, now);
		CONVERSATIONS.add(talk);
		return talk;
	}

	/** A rumour {@code npc} could pass on, as it would tell it, or null. */
	private static Component rumour(CultivatorNpc npc) {
		List<Rumours.Rumour> found = Rumours.untold(npc, Set.of(), 1);
		return found.isEmpty() ? null : Rumours.tell(found.get(0), npc);
	}

	/** The name of the Sect Master or one of the elders, or null. */
	private static String elderName(Sect sect, RandomSource random) {
		List<Sect.Member> seniors = new ArrayList<>();
		for (Sect.Member m : sect.members.values()) if (m.title.isElderOrAbove()) seniors.add(m);
		return seniors.isEmpty() ? null : NpcNames.of(seniors.get(random.nextInt(seniors.size())).id);
	}

	/** Every few ticks: whoever's turn it is in each talk says their line, once the two are together. */
	private static void advanceConversations() {
		for (Iterator<Conversation> it = CONVERSATIONS.iterator(); it.hasNext(); ) {
			Conversation c = it.next();
			Activity aa = c.a.activity(), ba = c.b.activity();
			if (!c.a.isAlive() || !c.b.isAlive() || c.a.isRemoved() || c.b.isRemoved() || aa == null || ba == null
					|| aa.kind != Kind.CHAT || ba.kind != Kind.CHAT) {
				it.remove();
				if (aa != null && aa.kind == Kind.CHAT) c.a.setActivity(null);
				if (ba != null && ba.kind == Kind.CHAT) c.b.setActivity(null);
				continue;
			}
			long now = c.a.level().getGameTime();
			boolean together = c.seated || aa.arrived && c.a.distanceToSqr(c.b) <= 3.5 * 3.5;
			if (!together) {
				if (now - c.begun > WALK_LIMIT) {
					it.remove();
					c.a.setActivity(null);
					c.b.setActivity(null);
				}
				continue;
			}
			if (c.endsAt >= 0) {
				if (now >= c.endsAt) {
					it.remove();
					c.a.setActivity(null);
					c.b.setActivity(null);
				}
				continue;
			}
			if (now < c.nextAt) continue;
			CultivatorNpc speaker = c.next % 2 == 0 ? c.a : c.b;
			CultivatorNpc listener = speaker == c.a ? c.b : c.a;
			Component line = Component.translatable(c.keys.get(c.next), name(listener), c.subject);
			NpcSpeech.say(speaker, line);
			c.nextAt = now + NpcSpeech.ticksFor(line) + 10;
			if (++c.next >= c.keys.size()) c.endsAt = c.nextAt;
		}
	}

	/**
	 * Where two meet for a bout on {@code sect}'s grounds: two open spots four blocks apart in its training ground's sparring ring (a sect
	 * without one: on the plaza), or null if none is free to stand on. A practice bout's, and a player's spar with one of its people
	 * (SparSystem: it leads them there).
	 */
	static BlockPos[] boutSpots(ServerLevel level, Sect sect, RandomSource random) {
		BlockPos ring = sect.sparringRing();
		for (int attempt = 0; attempt < 8; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2, facing = random.nextDouble() * Math.PI;
			double cx, cz;
			int y;
			if (ring != null) {
				cx = ring.getX() + 0.5;
				cz = ring.getZ() + 0.5;
				y = ring.getY();
			} else {
				cx = sect.core.getX() + 0.5 + Math.cos(angle) * 7.5;
				cz = sect.core.getZ() + 0.5 + Math.sin(angle) * 7.5;
				y = sect.core.getY() - 1;
			}
			double ox = Math.cos(facing) * 2.0, oz = Math.sin(facing) * 2.0;
			BlockPos one = BlockPos.containing(cx + ox, y, cz + oz);
			BlockPos two = BlockPos.containing(cx - ox, y, cz - oz);
			if (one.equals(two) || !standable(level, one) || !standable(level, two) || roofed(level, one) || roofed(level, two)) continue;
			return new BlockPos[] {one, two};
		}
		return null;
	}

	/** Two disciples meet in the training ground's sparring ring for a practice bout (a sect without one: on the plaza). */
	private static void startBout(ServerLevel level, Sect sect, CultivatorNpc a, CultivatorNpc b, long now) {
		RandomSource random = level.random;
		BlockPos[] spots = boutSpots(level, sect, random);
		if (spots == null) return;
		BlockPos one = spots[0], two = spots[1];
		long until = now + WALK_LIMIT + 600 + random.nextInt(300);
		Activity first = new Activity(Kind.SPAR, one, until);
		first.partner = b.getUUID();
		first.lead = true;
		first.face = Vec3.atBottomCenterOf(two).add(0, 1.5, 0);
		Activity second = new Activity(Kind.SPAR, two, until);
		second.partner = a.getUUID();
		second.face = Vec3.atBottomCenterOf(one).add(0, 1.5, 0);
		begin(a, first);
		begin(b, second);
	}

	/**
	 * A disciple goes to practise its sword forms: at a free spot before a training dummy on the training ground, or (a sect
	 * laid out before it had one) on a courtyard or the plaza, in the open.
	 */
	private static void startDrill(ServerLevel level, Sect sect, CultivatorNpc npc, long now) {
		RandomSource random = level.random;
		List<BlockPos> spots = sect.drillSpots();
		if (!spots.isEmpty()) {
			List<BlockPos> free = new ArrayList<>(spots);
			for (CultivatorNpc other : CultivatorNpc.loaded(level)) {
				Activity a = other.activity();
				if (a != null && a.kind == Kind.DRILL) free.remove(a.spot);
			}
			if (free.isEmpty()) return;
			BlockPos spot = free.get(random.nextInt(free.size()));
			if (!standable(level, spot)) return;
			begin(npc, new Activity(Kind.DRILL, spot, now + WALK_LIMIT + 400 + random.nextInt(400)));
			return;
		}
		List<BlockPos> courtyards = sect.posts(Sect.Post.COURTYARD);
		for (int attempt = 0; attempt < 6; attempt++) {
			BlockPos spot;
			if (!courtyards.isEmpty() && random.nextBoolean()) {
				spot = courtyards.get(random.nextInt(courtyards.size()));
			} else {
				double angle = random.nextDouble() * Math.PI * 2;
				spot = BlockPos.containing(sect.core.getX() + 0.5 + Math.cos(angle) * 8.5, sect.core.getY() - 1, sect.core.getZ() + 0.5 + Math.sin(angle) * 8.5);
			}
			if (Math.abs(spot.getY() - npc.getBlockY()) > 3 || !standable(level, spot) || roofed(level, spot)) continue;
			begin(npc, new Activity(Kind.DRILL, spot, now + WALK_LIMIT + 400 + random.nextInt(400)));
			return;
		}
	}

	/** A juniors bows to a senior passing close by (an Elder, a Grand Elder, the Sect Master), who may nod back. */
	private static void bows(ServerLevel level, List<CultivatorNpc> members, long now) {
		List<CultivatorNpc> seniors = new ArrayList<>();
		for (CultivatorNpc npc : members) if (npc.getTitle().isElderOrAbove() && npc.isAlive()) seniors.add(npc);
		if (seniors.isEmpty()) return;
		for (CultivatorNpc junior : members) {
			if (!disciple(junior) || junior.activity() != null || junior.isMeditating() || !free(junior)) continue;
			for (CultivatorNpc senior : seniors) {
				if (senior.distanceToSqr(junior) > 5 * 5 || !junior.hasLineOfSight(senior)) continue;
				if (now - junior.remarkedAt(senior.getUUID()) < BOW_EVERY) continue;
				junior.remark(senior.getUUID(), now);
				junior.getLookControl().setLookAt(senior, 30, 30);
				ModPackets.sendGesture(junior, Gesture.BOW);
				if (level.random.nextBoolean() && NpcSpeech.mayRemark(junior) && NpcSpeech.heard(junior)) {
					String key = senior.getTitle() == NpcTitle.SECT_MASTER ? "bow.master" : "bow.elder";
					NpcSpeech.say(junior, NpcLines.line(key, junior, name(senior)));
				}
				if (level.random.nextInt(3) == 0 && NpcSpeech.mayRemark(senior) && NpcSpeech.heard(senior)) NpcSpeech.say(senior, NpcLines.line("nod", senior));
				break;
			}
		}
	}

	// --- Practice, step by step (called by CultivatorGoals.LifeGoal every tick once in place) ---

	/** A practice bout: the two face off, cup fists, then take turns to lunge and parry until it's called. */
	static void spar(CultivatorNpc npc, Activity a) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		CultivatorNpc other = partner(npc, a);
		if (other == null) {
			end(npc);
			return;
		}
		npc.getLookControl().setLookAt(other, 30, 30);
		npc.setYBodyRot(npc.getYHeadRot());
		long now = level.getGameTime();
		Activity theirs = other.activity();
		if (a.boutStart == 0) {
			if (!a.lead || theirs == null || !theirs.arrived) return;
			a.boutStart = theirs.boutStart = now + 40;
			NpcSpeech.say(npc, NpcLines.line("spar.begin", npc, name(other)));
			ModPackets.sendGesture(npc, Gesture.CUP_FIST);
			ModPackets.sendGesture(other, Gesture.CUP_FIST);
			watchers(level, npc, other);
			return;
		}
		if (now < a.boutStart) return;
		long t = now - a.boutStart;
		long beat = t % BEAT;
		boolean attacking = ((t / BEAT) % 2 == 0) == a.lead;
		Vec3 toward = other.position().subtract(npc.position()).multiply(1, 0, 1);
		Vec3 dir = toward.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toward.normalize();
		if (attacking && beat == 0) {
			npc.swing(InteractionHand.MAIN_HAND);
			npc.setDeltaMovement(dir.x * 0.22, npc.getDeltaMovement().y, dir.z * 0.22);
			level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.35f, 1.1f + level.random.nextFloat() * 0.3f);
			Vec3 front = npc.position().add(dir.scale(0.9)).add(0, 1.1, 0);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0, 0, 0, 0);
		} else if (!attacking && beat == 4) {
			npc.swing(InteractionHand.MAIN_HAND);
			npc.setDeltaMovement(-dir.x * 0.18, npc.getDeltaMovement().y, -dir.z * 0.18);
			Vec3 mid = npc.position().add(other.position()).scale(0.5).add(0, 1.2, 0);
			level.playSound(null, BlockPos.containing(mid), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.1f, 1.9f + level.random.nextFloat() * 0.2f);
			level.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 4, 0.15, 0.15, 0.15, 0.1);
		} else if (beat > 6 && npc.position().distanceToSqr(Vec3.atBottomCenterOf(a.spot)) > 0.25) {
			// Back to its mark between exchanges, facing off again.
			npc.getMoveControl().setWantedPosition(a.spot.getX() + 0.5, a.spot.getY(), a.spot.getZ() + 0.5, 0.9);
		}
	}

	/** Someone idle nearby remarks on the bout. */
	private static void watchers(ServerLevel level, CultivatorNpc a, CultivatorNpc b) {
		if (level.random.nextInt(5) >= 2) return;
		for (CultivatorNpc npc : CultivatorNpc.loaded(level)) {
			if (npc == a || npc == b || npc.distanceToSqr(a) > 12 * 12 || !idle(npc) || !NpcSpeech.mayRemark(npc)) continue;
			NpcSpeech.say(npc, NpcLines.line("spar.watch", npc, name(a), name(b)));
			return;
		}
	}

	/** The bout's leader calls it: the stronger usually wins. Both cup fists and say so. */
	private static void finishBout(CultivatorNpc npc, Activity a) {
		CultivatorNpc other = partner(npc, a);
		if (other == null || a.boutStart == 0) return;
		RandomSource random = npc.getRandom();
		int edge = Integer.compare(npc.getRank(), other.getRank());
		boolean won = random.nextFloat() < (edge > 0 ? 0.75f : edge < 0 ? 0.25f : 0.5f);
		CultivatorNpc winner = won ? npc : other, loser = won ? other : npc;
		ModPackets.sendGesture(npc, Gesture.CUP_FIST);
		ModPackets.sendGesture(other, Gesture.CUP_FIST);
		NpcSpeech.say(winner, NpcLines.line("spar.win", winner, name(loser)));
		NpcSpeech.say(loser, NpcLines.line("spar.lose", loser, name(winner)));
	}

	/** Sword forms: a cut, a turn, a step, again; drifting back to its spot. */
	static void drill(CultivatorNpc npc, Activity a) {
		if (!(npc.level() instanceof ServerLevel level)) return;
		long now = level.getGameTime();
		if (Math.floorMod(now + npc.getId() * 7L, 18) != 0) return;
		npc.swing(InteractionHand.MAIN_HAND);
		float yaw = npc.getYRot();
		Vec3 look = Vec3.directionFromRotation(0, yaw);
		Vec3 front = npc.position().add(look.scale(0.9)).add(0, 1.1, 0);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0, 0, 0, 0);
		level.playSound(null, npc.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.25f, 1.2f + level.random.nextFloat() * 0.4f);
		float turn = (30 + level.random.nextInt(90)) * (level.random.nextBoolean() ? 1 : -1);
		float next = Mth.wrapDegrees(yaw + turn);
		npc.setYRot(next);
		npc.setYBodyRot(next);
		npc.setYHeadRot(next);
		if (npc.position().distanceToSqr(Vec3.atBottomCenterOf(a.spot)) > 2.25) {
			npc.getNavigation().moveTo(a.spot.getX() + 0.5, a.spot.getY(), a.spot.getZ() + 0.5, 0.8);
		} else {
			Vec3 step = Vec3.directionFromRotation(0, next).scale(0.12);
			npc.setDeltaMovement(step.x, npc.getDeltaMovement().y, step.z);
		}
	}

	// =====================================================================================================================
	// Rogues
	// =====================================================================================================================

	private static void rogues(ServerLevel level, List<CultivatorNpc> rogues, Phase phase, long now) {
		long time = timeOfDay(level);
		for (CultivatorNpc npc : rogues) {
			Activity a = npc.activity();
			// A wandering mortal lies down where it is for the night (cultivators meditate through it: WanderGoal).
			if (npc.isMortal()) {
				if (phase == Phase.NIGHT && a == null && free(npc) && npc.onGround() && !npc.isInWater()) {
					begin(npc, new Activity(Kind.SLEEP, npc.blockPosition(), now + ticksUntil(time, 0) + level.random.nextInt(800)));
				} else if (phase != Phase.NIGHT && a != null && a.kind == Kind.SLEEP) {
					a.until = Math.min(a.until, now + level.random.nextInt(600));
				}
			}
		}
		// Two rogues who meet pass a few words (by day; the Netherworld's dead in its endless night).
		if (phase == Phase.NIGHT && !ModDimensions.isNetherworld(level.dimension()) || level.random.nextInt(15) != 0) return;
		List<CultivatorNpc> idle = new ArrayList<>();
		for (CultivatorNpc npc : rogues) {
			if (idle(npc) && !npc.isMeditating() && npc.onGround() && watched(level, npc.position(), WATCHED / 2)) idle.add(npc);
		}
		if (idle.size() < 2) return;
		CultivatorNpc a = idle.get(level.random.nextInt(idle.size()));
		CultivatorNpc b = nearest(a, idle, 16);
		if (b != null && !a.isHostileTo(b) && !b.isHostileTo(a)) startTalk(level, null, null, a, b, false, now);
	}

	// =====================================================================================================================
	// Remarks: on players passing, the weather, sleepers' mumbles
	// =====================================================================================================================

	/** A second of {@code npc} keeping an eye on who passes: it may say something to a player who comes close. */
	private static void remark(ServerLevel level, CultivatorNpc npc, long now) {
		if (npc.hasPose(Pose.SLEEPING)) {
			if (level.random.nextInt(90) == 0 && NpcSpeech.mayRemark(npc) && NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line("sleep", level.random));
			return;
		}
		// One kneeling to a player passing (TalkSystem#kneelsBefore) still speaks to them, and to no one else.
		boolean kneeling = npc.isKneeling();
		if (npc.isMeditating() || !free(npc, kneeling) || !NpcSpeech.mayRemark(npc)) return;
		Activity a = npc.activity();
		if (a != null && (a.kind == Kind.CHAT || a.kind == Kind.SPAR || a.kind == Kind.LECTURE || a.kind == Kind.LISTEN || a.kind == Kind.MOURN || a.kind == Kind.RITUAL)) return;
		// Patrolling in the rain gets grumbled about.
		if (!kneeling && level.isRaining() && npc.getTitle() == NpcTitle.OUTER_DISCIPLE && !npc.isGateGuard() && a == null && level.random.nextInt(120) == 0
				&& !roofed(level, npc.blockPosition()) && NpcSpeech.heard(npc)) {
			NpcSpeech.say(npc, NpcLines.line("rain.patrol", npc));
			return;
		}
		ServerPlayer player = null;
		if (kneeling) {
			if (npc.kneelingBefore() instanceof ServerPlayer p && p.isAlive() && p.distanceToSqr(npc) <= REMARK_RANGE * REMARK_RANGE) player = p;
		} else {
			double best = REMARK_RANGE * REMARK_RANGE;
			for (ServerPlayer p : level.players()) {
				// Not one it can't sense (a soul, the dead, a Ghost King's form: CultivatorNpc#senses).
				if (p.isSpectator() || !p.isAlive() || p.isInvisible() || !npc.senses(p)) continue;
				double d = p.distanceToSqr(npc);
				double range = p.isShiftKeyDown() ? SNEAK_REMARK_RANGE : REMARK_RANGE;
				if (d <= range * range && d <= best) {
					best = d;
					player = p;
				}
			}
		}
		if (player == null || npc.isHostileTo(player) || now - npc.remarkedAt(player.getUUID()) < REMARK_EVERY || !npc.hasLineOfSight(player)) return;
		npc.remark(player.getUUID(), now);
		Sect sect = npc.sect();
		boolean outsider = sect != null && sect.inTerritory(player.blockPosition()) && !npc.isAlly(player);
		if (outsider) {
			SectDay day = SECTS.computeIfAbsent(sect.id, id -> new SectDay());
			// Talked of after by their title, if they have taken one (chat.visitor).
			day.visitor = NpcAddress.name(player);
			day.visitorAt = now;
		}
		Component line = remarkOn(npc, player, outsider);
		if (line == null) return;
		npc.getLookControl().setLookAt(player, 30, 30);
		NpcSpeech.say(npc, line);
	}

	/**
	 * What {@code npc} says to {@code player} passing by, or null for nothing. It calls them as NpcAddress has it (by their title, from
	 * Void Shattering on; else by the rung they stand on before it, from a junior's to an Ancestor's), and the higher above it they stand
	 * the lower it bows: one realm, a bow; two or three, a deep one; four or more, it kneels (TalkSystem#kneelsBefore). Only one it
	 * needn't bow low to ({@link #mayRemarkOn}) hears what it makes of them: the treasure in their hand, their wounds, their flight.
	 * Package-private for the game tests.
	 */
	static Component remarkOn(CultivatorNpc npc, ServerPlayer player, boolean outsider) {
		RandomSource random = npc.getRandom();
		NpcAddress.Rung rung = NpcAddress.rung(player, npc);
		Component address = NpcAddress.address(player, npc);
		String name = NpcAddress.name(player);
		int regard = npc.regard(player);
		if (npc.isGateGuard()) {
			if (rung.above()) return NpcLines.line(regard >= Regard.COURTEOUS ? "gate.welcome_senior" : "gate.halt_senior", npc, address, name);
			return NpcLines.line(regard >= Regard.COURTEOUS ? "gate.welcome" : "gate.halt", npc, name);
		}
		// The dead marvel at the living's warmth (and warn them off), now and then.
		if (npc.isGhost() && !GhostSystem.isGhost(player) && random.nextInt(3) == 0) return NpcLines.line("pass.living", npc);
		// Those waiting on the Shore of Nirvana speak of the Gate, as often as not.
		if (NirvanaCamp.isCamper(npc) && random.nextBoolean()) return NpcLines.line("shore.wait", npc);
		boolean bold = mayRemarkOn(rung, npc);
		ItemStack held = player.getMainHandItem();
		if (bold && held.getItem() instanceof SpiritStoneItem stone && stone.grade().ordinal() >= SpiritStoneItem.Grade.SUPREME.ordinal()) {
			return NpcLines.line("pass.stone", npc, held.getHoverName());
		}
		if (bold && held.getItem() instanceof CultivationFruitItem) return NpcLines.line("pass.fruit", npc, held.getHoverName());
		if (regard <= -30) return bold ? NpcLines.line("pass.disliked", npc, name) : NpcLines.line("pass.disliked_senior", npc, address, name);
		if (regard >= Regard.WARM) return rung.above() ? NpcLines.line("pass.liked_senior", npc, address, name) : NpcLines.line("pass.liked", npc, name);
		// Something about them it would speak of, two times in three.
		String seen = bold ? seen(npc, player, outsider) : null;
		if (seen != null && NpcLines.has(seen, npc) && random.nextInt(3) != 0) return NpcLines.line(seen, npc, address, name);
		switch (rung) {
			case REVERED, ANCESTOR -> {
				return NpcLines.line("pass." + rung.id(), npc, address, name);
			}
			case SENIOR, ESTEEMED -> {
				if (NpcAddress.ofItsSect(player, npc) && random.nextBoolean()) return NpcLines.line("pass.fellow", npc, address, name);
				ModPackets.sendGesture(npc, Gesture.BOW);
				return NpcLines.line("pass." + rung.id(), npc, address, name);
			}
			case FAR_ABOVE -> {
				return NpcLines.line(random.nextBoolean() ? "pass.far_above" : "pass.junior", npc, address, name);
			}
			case MORTAL -> {
				return NpcLines.line("pass.mortal", npc, address, name);
			}
			default -> { }
		}
		if (NpcAddress.ofItsSect(player, npc) && random.nextBoolean()) return NpcLines.line("pass.fellow", npc, address, name);
		if (outsider && random.nextBoolean()) return NpcLines.line("pass.outsider", npc);
		if (random.nextInt(3) != 0) return null;
		return NpcLines.line(rung == NpcAddress.Rung.ABOVE ? "pass.above" : "pass.peer", npc, address, name);
	}

	/**
	 * Whether {@code npc} would speak its mind of what it sees on one standing on {@code rung} before it: not to one it must bow low to, or
	 * kneel to (a mortal's words are humble anyway, but it kneels too).
	 */
	private static boolean mayRemarkOn(NpcAddress.Rung rung, CultivatorNpc npc) {
		return rung.ordinal() <= NpcAddress.Rung.SENIOR.ordinal() || rung == NpcAddress.Rung.MORTAL || npc.isMortal() && rung == NpcAddress.Rung.ESTEEMED;
	}

	/** Something about {@code player} that {@code npc} would speak of rather than only greet them, as the situation of its line; or null. */
	private static String seen(CultivatorNpc npc, ServerPlayer player, boolean outsider) {
		PlayerCultivation c = CultivationManager.get(player);
		if (player.getHealth() < player.getMaxHealth() * 0.35f) return "pass.hurt";
		if (FiveDecays.sign(c) > 0) return "pass.decays";
		if (LawDomains.lit(c) != null) return "pass.domain";
		if (player.getAbilities().flying && !player.isCreative() || SwordFlight.isRiding(player) || player.isFallFlying()) return "pass.flying";
		if (MeditationManager.isMeditating(player.getUUID())) return "pass.meditating";
		if (DemonPath.isDemon(c)) return npc.isDemon() ? "pass.kin" : "pass.demon";
		if (outsider && npc.level().isNight()) return "pass.night";
		return null;
	}

	/** Now and then, one of those near a player facing their tribulation cries out at it (once in {@link #REMARK_EVERY}, as remarks). */
	private static void tribulations(ServerLevel level, List<CultivatorNpc> npcs, long now) {
		for (ServerPlayer p : level.players()) {
			if (!TribulationManager.isActive(p.getUUID())) continue;
			for (CultivatorNpc npc : npcs) {
				if (npc.distanceToSqr(p) > TRIBULATION_SEEN * TRIBULATION_SEEN || npc.hasPose(Pose.SLEEPING) || npc.isMeditating() || !free(npc)
						|| now - npc.remarkedAt(p.getUUID()) < REMARK_EVERY || level.random.nextInt(6) != 0) continue;
				if (!NpcSpeech.mayRemark(npc) || !NpcSpeech.heard(npc)) continue;
				npc.remark(p.getUUID(), now);
				npc.getLookControl().setLookAt(p, 30, 30);
				NpcSpeech.say(npc, NpcLines.line("tribulation.watch", npc));
			}
		}
	}

	/** When the rain or the thunder starts, some of those out in it say so. */
	private static void weather(ServerLevel level, List<CultivatorNpc> npcs) {
		int now = (level.isRaining() ? 1 : 0) | (level.isThundering() ? 2 : 0);
		Integer before = WEATHER.put(level.dimension(), now);
		if (before == null || before == now) return;
		String key = (now & 2) != 0 && (before & 2) == 0 ? "thunder" : (now & 1) != 0 && (before & 1) == 0 ? "rain" : null;
		if (key == null) return;
		for (CultivatorNpc npc : npcs) {
			if (npc.hasPose(Pose.SLEEPING) || npc.isMeditating() || !free(npc) || roofed(level, npc.blockPosition())) continue;
			if (level.random.nextInt(4) == 0 && NpcSpeech.mayRemark(npc) && NpcSpeech.heard(npc)) NpcSpeech.say(npc, NpcLines.line(key, npc));
		}
	}

	// =====================================================================================================================
	// Calls from elsewhere
	// =====================================================================================================================

	/** The sect raised an alarm (SectManager#alert): one or two of those nearest shout it. */
	public static void onAlarm(ServerLevel level, Sect sect, SectManager.Alarm alarm, BlockPos where) {
		long now = level.getGameTime();
		SectDay day = SECTS.computeIfAbsent(sect.id, id -> new SectDay());
		if (now - day.lastAlarm < 200) return;
		day.lastAlarm = now;
		String key = switch (alarm) {
			case MEMBER_ATTACKED -> "alarm.attack";
			case BARRIER_ASSAILED, BARRIER_BROKEN -> "alarm.barrier";
			case CRITICAL_BREACH, CORE_DESTROYED -> "alarm.thief";
			case TRESPASS -> "alarm.trespass";
		};
		List<CultivatorNpc> near = new ArrayList<>(CultivatorNpc.loadedMembers(level, sect.id));
		near.removeIf(npc -> npc.blockPosition().distSqr(where) > 48 * 48 || now < npc.speakingUntil() || npc.isConcealed());
		near.sort(Comparator.comparingDouble(npc -> npc.blockPosition().distSqr(where)));
		for (int i = 0; i < Math.min(2, near.size()); i++) NpcSpeech.say(near.get(i), NpcLines.line(key, near.get(i)));
	}

	/** {@code npc} broke through to a new stage: it says so, and sect fellows near it congratulate it. */
	public static void onBreakthrough(CultivatorNpc npc) {
		if (!(npc.level() instanceof ServerLevel level) || npc.isConcealed() || !NpcSpeech.heard(npc)) return;
		NpcSpeech.say(npc, NpcLines.line("breakthrough", npc));
		if (npc.getSectId() == null) return;
		int cheers = 0;
		for (CultivatorNpc other : CultivatorNpc.loadedMembers(level, npc.getSectId())) {
			if (other == npc || other.distanceToSqr(npc) > 16 * 16 || other.hasPose(Pose.SLEEPING) || !free(other)) continue;
			if (NpcSpeech.say(other, NpcLines.line("congratulate", other, name(npc))) && ++cheers >= 2) break;
		}
	}

	/** Package-private for the game tests: the talks going on. */
	static List<Conversation> conversations() {
		return CONVERSATIONS;
	}

	/** Package-private for the game tests: one round of the talks' pacing. */
	static void advanceTalksForTest() {
		advanceConversations();
	}

	/** Package-private for the game tests: a second of mourning on a sect's grounds. */
	static void mournForTest(ServerLevel level, Sect sect, List<CultivatorNpc> members, Phase phase, long now) {
		mourning(level, sect, SECTS.computeIfAbsent(sect.id, id -> new SectDay()), members, phase, now);
	}

	/** Package-private for the game tests: a sect's day, fresh. */
	static void forget(UUID sect) {
		SECTS.remove(sect);
	}

	private NpcLife() {}
}
