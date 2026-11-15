package com.example.defyingtheheavens;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sending one's disciples out (the user's "Build as designed", 2026-11-13). A player's sect has its own mission board on its plaza
 * (SectMissions#layBoards), and its founder a page on it (MissionBoardScreen's Send Out tab) to send one of its cultivators on an errand
 * ({@link #send}): to {@link Task#HUNT} a demon beast of a realm the founder picks, {@link Task#GATHER} spirit herbs, enter a secret realm of
 * its world while one is open ({@link Task#REALM}), or {@link Task#LETTER carry a letter} to the nearest other sect (its regard of the founder
 * the warmer for it). At most a third of its people are out at once ({@link #mostOut}); a task above them is warned of on the page.
 * <ul>
 *   <li>They really travel ({@link ErrandGoal}): through the air or on foot to where the task is, lingering there till their time is up, and
 *   home again. Out of everyone's sight they go on unseen, as NpcTravel's travellers do ({@link Errand#at}): taken out of the world, their
 *   body kept with the errand, and back in it wherever they have got to the moment a player comes near, to be met on the road (or at their
 *   hunting ground), talked to, or fought. One who goes into a secret realm is gone inside till its time is up.</li>
 *   <li>Its time up ({@link #tick}), the outcome comes of their strength against the task's ({@link #outcome}): done, its spoils brought back;
 *   wounded (half the spoils, and they rest {@link #REST_DAYS} days); a rare breakthrough besides; or they never come back: struck from the
 *   roll, the sect mourns them, and it is talked of near the sect (Rumours#noteLost). The rest come home, set down on their own mat if nobody
 *   is there to see them come.</li>
 *   <li>The spoils wait at the sect's Formation Core, collected with its tribute on its Sect page (PlayerSects#collect).</li>
 * </ul>
 */
public final class SectErrands {
	/** What a disciple may be sent to do. */
	public enum Task {
		HUNT("hunt", 2), GATHER("gather", 1), REALM("realm", 3), LETTER("letter", 1);

		private final String id;
		/** How long the trip takes (game days). */
		public final int days;

		Task(String id, int days) {
			this.id = id;
			this.days = days;
		}

		public String getId() { return id; }

		public Component getDisplayName() { return Component.translatable(ModLang.errandTask(id)); }

		public static @Nullable Task byIndex(int index) {
			return index >= 0 && index < values().length ? values()[index] : null;
		}
	}

	/** How it came out. */
	public enum Outcome { DONE, WOUNDED, BREAKTHROUGH, LOST }

	/** How one of the sect's people stands, as its founder's page shows it: at home and free, out, on the way back, resting, or a player's own disciple. */
	public enum State { HOME, OUT, RETURNING, RESTING, PERSONAL }

	/** One sent out: the task, its strength (a rank), where it goes (and the sect a letter is for), when it set off and is due back, how it came out. */
	public static final class Errand {
		public final Task task;
		public final int taskRank;
		public final BlockPos to;
		public final UUID letterFor;
		public final long departed, due;
		/** How it came out, once its time is up (null: still out). */
		public Outcome outcome;
		/** Back home (or gone for good): the record may go. */
		public boolean settled;
		/** Its body while it goes on unseen, out of everyone's sight (null: in the world, or stood where its land unloaded). */
		@Nullable CompoundTag body;
		/** Unseen: where the leg it is on began, and when. */
		BlockPos legFrom = BlockPos.ZERO;
		long legStart;
		/** One that never came back, stood where its land unloaded: where (its sect's roll no longer keeps it). */
		@Nullable BlockPos lastAt;
		/** When its land was last woken to find it, and how many times (not saved). */
		long wokeAt = -1;
		int looks;

		Errand(Task task, int taskRank, BlockPos to, @Nullable UUID letterFor, long departed, long due) {
			this.task = task;
			this.taskRank = taskRank;
			this.to = to;
			this.letterFor = letterFor;
			this.departed = departed;
			this.due = due;
		}

		/** Where it is bound: the task's place while out (and one lost goes on there, never to come back), home once its time is up. */
		BlockPos goal(Sect sect) {
			return outcome == null || outcome == Outcome.LOST ? to : sect.core;
		}

		/** Unseen, where it has got to at {@code now}: along its leg at about a flier's pace (NpcTravel#SPEED), then there. */
		Vec3 at(Sect sect, long now) {
			Vec3 from = Vec3.atBottomCenterOf(legFrom), goal = Vec3.atBottomCenterOf(goal(sect));
			double length = from.distanceTo(goal);
			if (length < 1) return goal;
			double done = Mth.clamp((now - legStart) * NpcTravel.SPEED / length, 0, 1);
			return from.add(goal.subtract(from).scale(done));
		}

		/** Unseen, whether it is where it is bound by {@code now}. */
		boolean there(Sect sect, long now) {
			return (now - legStart) * NpcTravel.SPEED >= Math.sqrt(legFrom.distSqr(goal(sect))) - 1;
		}

		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			tag.putString("Task", task.name());
			tag.putInt("Rank", taskRank);
			tag.putLong("To", to.asLong());
			if (letterFor != null) tag.putUUID("LetterFor", letterFor);
			tag.putLong("Departed", departed);
			tag.putLong("Due", due);
			if (outcome != null) tag.putString("Outcome", outcome.name());
			tag.putBoolean("Settled", settled);
			if (body != null) {
				tag.put("Body", body);
				tag.putLong("LegFrom", legFrom.asLong());
				tag.putLong("LegStart", legStart);
			}
			if (lastAt != null) tag.putLong("LastAt", lastAt.asLong());
			return tag;
		}

		static @Nullable Errand load(CompoundTag tag) {
			Task task = null;
			for (Task t : Task.values()) if (t.name().equals(tag.getString("Task"))) task = t;
			if (task == null) return null;
			Errand e = new Errand(task, tag.getInt("Rank"), BlockPos.of(tag.getLong("To")), tag.hasUUID("LetterFor") ? tag.getUUID("LetterFor") : null,
					tag.getLong("Departed"), tag.getLong("Due"));
			for (Outcome o : Outcome.values()) if (o.name().equals(tag.getString("Outcome"))) e.outcome = o;
			e.settled = tag.getBoolean("Settled");
			if (tag.contains("Body")) {
				e.body = tag.getCompound("Body");
				e.legFrom = BlockPos.of(tag.getLong("LegFrom"));
				e.legStart = tag.getLong("LegStart");
			}
			if (tag.contains("LastAt")) e.lastAt = BlockPos.of(tag.getLong("LastAt"));
			return e;
		}
	}

	/** A wounded one rests this long (game days) before it may be sent again. */
	public static final int REST_DAYS = 3;
	/** A hunt's quarry: from Qi Refining to Core Formation, the wild's realms. */
	public static final Realm HUNT_LEAST = Realm.QI_REFINING, HUNT_MOST = Realm.CORE_FORMATION;
	/** How far a hunt and a gathering go (blocks, either way at random). */
	public static final int HUNT_FROM = 400, HUNT_TO = 900, GATHER_FROM = 250, GATHER_TO = 600;
	/** A letter carried warms the sect it is for to the founder this much (Sect#standing). */
	public static final int LETTER_STANDING = 10;
	/** The spoils kept waiting at the core: at most this many stacks. */
	public static final int SPOILS_KEPT = 54;
	/** Within this of where it is bound (sideways), it is there: at its task, or home. */
	public static final double THERE = 12;
	/** Within this of where it is bound (sideways), a flier keeps at least at its height, to come in over it (ErrandGoal). */
	public static final double APPROACH = 96;
	/** An errand come out and never seen settled (its cultivator never found since) is forgotten this long after (ticks: thirty days). */
	public static final long FORGOTTEN = 30 * 24_000L;
	/** Land woken to find one (or set one down at home) at most this often, a few at a time. */
	private static final long WAKE_AGAIN = 600;
	private static final int WAKES = 3, LOOKS = 10;

	// --- Who may go ---

	/** How many of {@code sect}'s people may be out at once: a third, one at least. */
	public static int mostOut(Sect sect) {
		return Math.max(1, sect.members.size() / 3);
	}

	/** How many are out now (on the way back counts as home). */
	public static int out(Sect sect) {
		int n = 0;
		for (Errand e : sect.errands.values()) if (e.outcome == null && !e.settled) n++;
		return n;
	}

	/** How {@code member} of {@code sect} stands now. */
	public static State state(MinecraftServer server, Sect sect, UUID member, long now) {
		Errand e = sect.errands.get(member);
		if (e != null && !e.settled) return e.outcome == null ? State.OUT : State.RETURNING;
		if (personal(server, sect, member)) return State.PERSONAL;
		Long rest = sect.resting.get(member);
		return rest != null && now < rest ? State.RESTING : State.HOME;
	}

	/** Whether {@code member} is someone's personal disciple (PersonalDisciples): it keeps to its master's side, and is never sent. */
	static boolean personal(MinecraftServer server, Sect sect, UUID member) {
		ServerLevel home = server.getLevel(sect.dimension);
		if (home != null && home.getEntity(member) instanceof CultivatorNpc npc && PersonalDisciples.isDisciple(npc)) return true;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (member.equals(CultivationManager.own(player).personalDisciple())) return true;
		}
		return false;
	}

	/** {@code sect}'s people as its founder's page lists them (by seniority: Sect#SENIORITY). */
	public static List<Sect.Member> roll(Sect sect) {
		List<Sect.Member> members = new ArrayList<>(sect.members.values());
		members.sort(Sect.SENIORITY);
		return members;
	}

	/** The mark a page's row carries for its cultivator, so a roll changed since the page was sent sends nobody by mistake. */
	public static int check(UUID member) {
		return member.hashCode() & 0xFFFF;
	}

	/** The secret realm of {@code sect}'s world open now (its gate open), or null. */
	public static @Nullable SecretRealms.Record openRealm(MinecraftServer server, Sect sect) {
		long now = server.overworld().getGameTime();
		SecretRealms.Data data = SecretRealms.Data.get(server);
		for (SecretRealm realm : SecretRealm.values()) {
			SecretRealms.Record r = data.of(realm);
			if (r != null && r.seal != null && realm.home() == sect.dimension && SecretRealms.gateOpen(r, now)) return r;
		}
		return null;
	}

	/** The nearest other sect still standing in {@code sect}'s world (a letter's), or null. */
	public static @Nullable Sect letterTarget(MinecraftServer server, Sect sect) {
		Sect best = null;
		double nearest = Double.MAX_VALUE;
		for (Sect other : SectData.get(server).all()) {
			if (other == sect || other.extinct || other.dimension != sect.dimension) continue;
			double d = other.core.distSqr(sect.core);
			if (d < nearest) {
				nearest = d;
				best = other;
			}
		}
		return best;
	}

	/** A hunt's quarry's realm, kept to the wild's ({@link #HUNT_LEAST} to {@link #HUNT_MOST}). */
	public static Realm huntRealm(int index) {
		return Realm.byIndex(Mth.clamp(index, HUNT_LEAST.ordinal(), HUNT_MOST.ordinal()));
	}

	/** The strength a task stands at: a hunt its quarry's realm's middle; herbs Qi Refining's; a secret realm the realm it is meant for; a letter none. */
	public static int taskRank(MinecraftServer server, Sect sect, Task task, int huntRealm) {
		return switch (task) {
			case HUNT -> PlayerCultivation.rank(huntRealm(huntRealm), Stage.MID);
			case GATHER -> PlayerCultivation.rank(Realm.QI_REFINING, Stage.MID);
			case REALM -> {
				SecretRealms.Record r = openRealm(server, sect);
				yield r == null ? 0 : PlayerCultivation.rank(r.realm.meantFor(), Stage.MID);
			}
			case LETTER -> 0;
		};
	}

	// --- Sending ---

	/**
	 * The founder sends the {@code index}th of their sect's roll ({@code check}: its mark, {@link #check}) on {@code task} ({@code huntRealm}:
	 * a hunt's quarry's realm). @return the board's word on it
	 */
	public static Component send(ServerPlayer founder, Sect sect, int index, int check, @Nullable Task task, int huntRealm) {
		if (sect.extinct || !sect.isPlayers() || !SectMembership.isFounder(sect, founder.getUUID())) {
			return Component.translatable(ModLang.ERRAND_NOT_FOUNDER).withStyle(ChatFormatting.RED);
		}
		List<Sect.Member> roll = roll(sect);
		if (task == null || index < 0 || index >= roll.size() || check(roll.get(index).id) != check) {
			return Component.translatable(ModLang.ERRAND_NO_ONE).withStyle(ChatFormatting.RED);
		}
		Sect.Member member = roll.get(index);
		MinecraftServer server = founder.server;
		long now = server.overworld().getGameTime();
		String name = NpcNames.of(member.id);
		switch (state(server, sect, member.id, now)) {
			case OUT, RETURNING -> {
				return Component.translatable(ModLang.ERRAND_AWAY, name).withStyle(ChatFormatting.RED);
			}
			case RESTING -> {
				return Component.translatable(ModLang.ERRAND_RESTING, name).withStyle(ChatFormatting.RED);
			}
			case PERSONAL -> {
				return Component.translatable(ModLang.ERRAND_PERSONAL, name).withStyle(ChatFormatting.RED);
			}
			case HOME -> { }
		}
		if (out(sect) >= mostOut(sect)) return Component.translatable(ModLang.ERRAND_TOO_MANY, mostOut(sect)).withStyle(ChatFormatting.RED);
		ServerLevel home = server.getLevel(sect.dimension);
		CultivatorNpc npc = home != null && home.getEntity(member.id) instanceof CultivatorNpc loaded ? loaded : null;
		if (npc != null && (npc.isVacant() || !npc.isAlive())) return Component.translatable(ModLang.ERRAND_AWAY, name).withStyle(ChatFormatting.RED);
		BlockPos to;
		UUID letterFor = null;
		Component what;
		RandomSource random = server.overworld().getRandom();
		int rank = taskRank(server, sect, task, huntRealm);
		switch (task) {
			case HUNT -> {
				to = away(sect.core, HUNT_FROM, HUNT_TO, random);
				what = Component.translatable(ModLang.ERRAND_HUNT_OF, huntRealm(huntRealm).getDisplayName());
			}
			case GATHER -> {
				to = away(sect.core, GATHER_FROM, GATHER_TO, random);
				what = Component.translatable(ModLang.ERRAND_GATHER);
			}
			case REALM -> {
				SecretRealms.Record r = openRealm(server, sect);
				if (r == null) return Component.translatable(ModLang.ERRAND_NO_REALM).withStyle(ChatFormatting.RED);
				to = r.seal;
				what = Component.translatable(ModLang.ERRAND_REALM_OF, r.realm.displayName());
			}
			case LETTER -> {
				Sect other = letterTarget(server, sect);
				if (other == null) return Component.translatable(ModLang.ERRAND_NO_SECT).withStyle(ChatFormatting.RED);
				to = other.core;
				letterFor = other.id;
				what = Component.translatable(ModLang.ERRAND_LETTER_TO, other.name);
			}
			default -> {
				return Component.translatable(ModLang.ERRAND_NO_ONE).withStyle(ChatFormatting.RED);
			}
		}
		Errand errand = new Errand(task, rank, to, letterFor, now, now + task.days * 24_000L);
		errand.legFrom = sect.core;
		errand.legStart = now;
		sect.errands.put(member.id, errand);
		sect.resting.remove(member.id);
		SectManager.dirty(server);
		if (npc != null) {
			npc.setMeditating(false);
			NpcLife.end(npc);
		}
		Component days = task.days == 1 ? Component.translatable(ModLang.ERRAND_A_DAY) : Component.translatable(ModLang.ERRAND_DAYS, task.days);
		return Component.translatable(ModLang.ERRAND_SENT, name, what, days).withStyle(ChatFormatting.GOLD);
	}

	/** A spot {@code from} to {@code to} blocks from {@code centre}, any way. */
	private static BlockPos away(BlockPos centre, int from, int to, RandomSource random) {
		double angle = random.nextDouble() * Math.PI * 2, far = from + random.nextInt(Math.max(1, to - from));
		return new BlockPos(centre.getX() + (int) Math.round(Math.cos(angle) * far), centre.getY(), centre.getZ() + (int) Math.round(Math.sin(angle) * far));
	}

	/**
	 * The founder's page on their board (SectMissions#send, after the rest): how many are out of how many may be, whether its sect is a body
	 * sect (how its ranks read), the secret realm open (its name and strength) and the sect a letter would go to, the spoils waiting at the
	 * core, and its people: each one's mark, name, rank, state, errand and the ticks till they are due back or rested.
	 */
	public static void writePage(FriendlyByteBuf buf, MinecraftServer server, Sect sect) {
		long now = server.overworld().getGameTime();
		buf.writeVarInt(out(sect));
		buf.writeVarInt(mostOut(sect));
		buf.writeBoolean(sect.body);
		SecretRealms.Record realm = openRealm(server, sect);
		buf.writeBoolean(realm != null);
		if (realm != null) {
			buf.writeComponent(realm.realm.displayName());
			buf.writeVarInt(taskRank(server, sect, Task.REALM, 0));
		}
		Sect other = letterTarget(server, sect);
		buf.writeUtf(other == null ? "" : other.name);
		buf.writeVarInt(sect.spoils.size());
		List<Sect.Member> roll = roll(sect);
		buf.writeVarInt(roll.size());
		for (Sect.Member m : roll) {
			buf.writeVarInt(check(m.id));
			buf.writeUtf(NpcNames.of(m.id));
			buf.writeVarInt(m.rank);
			State state = state(server, sect, m.id, now);
			buf.writeVarInt(state.ordinal());
			Errand e = sect.errands.get(m.id);
			buf.writeVarInt(e == null || e.settled ? -1 : e.task.ordinal());
			Long rest = sect.resting.get(m.id);
			long left = state == State.OUT ? e.due - now : state == State.RESTING && rest != null ? rest - now : 0;
			buf.writeVarLong(Math.max(0, left));
		}
	}

	// --- Coming and going ---

	/**
	 * Twice a second, every player's sect's errands: those whose people have dropped off its roll (fallen on the road, cast out, gone) let go
	 * of; those out unseen back in the world where a player has come near them ({@link #bringBack}), or set down at home once back; those in
	 * the world but stood in land that no longer runs taken out of it to go on unseen ({@link #takeOut}). Every five seconds besides, those
	 * whose time is up come out ({@link #comeBack}), and those whose cultivator stood where its land unloaded are looked for (their land woken).
	 */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 10 != 3) return;
		boolean slow = server.getTickCount() % 100 == 53;
		long now = server.overworld().getGameTime();
		double reach = NpcTravel.reach(server);
		boolean changed = false;
		int wakes = 0;
		for (Sect sect : SectData.get(server).all()) {
			if (!sect.isPlayers() || sect.errands.isEmpty() && sect.resting.isEmpty()) continue;
			if (sect.extinct) {
				// Broken up while some were out: they never come back to it (they go their own ways, unseen).
				sect.errands.clear();
				sect.resting.clear();
				changed = true;
				continue;
			}
			ServerLevel home = server.getLevel(sect.dimension);
			for (Iterator<Map.Entry<UUID, Errand>> it = sect.errands.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Errand> entry = it.next();
				UUID id = entry.getKey();
				Errand e = entry.getValue();
				// Settled (home, or gone for good), or come out a month ago and never found since: let go of.
				if (e.settled || e.outcome != null && now - e.due > FORGOTTEN) {
					it.remove();
					changed = true;
					continue;
				}
				// Off the roll while out (fallen on the road, cast out, gone its own way): nothing more comes of it.
				if (e.outcome != Outcome.LOST && !sect.members.containsKey(id)) {
					it.remove();
					changed = true;
					continue;
				}
				if (slow && e.outcome == null && now >= e.due) {
					comeBack(server, sect, id, e, server.overworld().getRandom());
					changed = true;
				}
				if (home == null || e.settled) continue;
				if (e.body != null) {
					// A copy of it in the world while its body is on the road (saved with its land before a crash, say): gone.
					if (home.getEntity(id) instanceof CultivatorNpc stale && !stale.isRemoved()) {
						stale.travelling = true;
						stale.discard();
						changed = true;
					}
					if (e.outcome == Outcome.LOST) {
						e.body = null; // gone on the road, never to come back
						e.settled = true;
						changed = true;
						continue;
					}
					if (e.outcome != null && e.there(sect, now)) {
						// Home with nobody near to see it come: set down on its own mat, the land woken a moment for it (as NpcTravel's arrivals).
						BlockPos mat = dwelling(sect, id);
						if (home.isPositionEntityTicking(mat)) {
							if (setDown(home, sect, id, e, mat)) changed = true;
						} else if (slow && wakes < WAKES && (e.wokeAt < 0 || now - e.wokeAt >= WAKE_AGAIN)) {
							NpcLedger.wake(home, mat);
							e.wokeAt = now;
							wakes++;
						}
						continue;
					}
					// Gone into a secret realm: inside till its time is up.
					if (e.task == Task.REALM && e.outcome == null && e.there(sect, now)) continue;
					Vec3 at = e.at(sect, now);
					if (home.isPositionEntityTicking(BlockPos.containing(at)) && someoneNear(home, at, reach) && bringBack(home, sect, id, e, at)) changed = true;
					continue;
				}
				Entity loaded = home.getEntity(id);
				if (loaded instanceof CultivatorNpc npc) {
					// In loaded land that no longer runs (its player gone off fast): on its way unseen at once, or, one lost, gone.
					if (!npc.isVacant() && npc.isAlive() && !home.isPositionEntityTicking(npc.blockPosition())) {
						if (e.outcome == Outcome.LOST) {
							if (sect.members.containsKey(id)) gone(server, sect, id, e, now);
							lose(npc, e);
						} else {
							takeOut(npc, e, now);
						}
						changed = true;
					}
				} else if (loaded == null && slow && e.outcome != null && wakes < WAKES && (e.wokeAt < 0 || now - e.wokeAt >= WAKE_AGAIN)) {
					// Its time up while it stood where its land unloaded (a restart, say): that land woken to find it (#second does the rest),
					// a few times; never found, it is let be (one come back lives on where it stands, and finds its way home in time).
					Sect.Member m = sect.members.get(id);
					BlockPos where = e.lastAt != null ? e.lastAt : m != null && m.lastAt != null ? m.lastAt : null;
					if (where == null || e.looks >= LOOKS) {
						e.settled = true;
					} else {
						NpcLedger.wake(home, where);
						e.wokeAt = now;
						e.looks++;
						wakes++;
					}
					changed = true;
				}
			}
			if (slow && sect.resting.values().removeIf(until -> until <= now)) changed = true;
		}
		if (changed) SectManager.dirty(server);
	}

	/** {@code member}'s errand {@code e} comes out: the outcome reckoned, its spoils at the core, its founder told, a loss mourned and talked of. */
	static void comeBack(MinecraftServer server, Sect sect, UUID member, Errand e, RandomSource random) {
		Sect.Member m = sect.members.get(member);
		ServerLevel home = server.getLevel(sect.dimension);
		CultivatorNpc npc = home != null && home.getEntity(member) instanceof CultivatorNpc loaded && loaded.isAlive() ? loaded : null;
		int rank = npc != null ? npc.getRank() : m == null ? 0 : m.rank;
		long now = server.overworld().getGameTime();
		// Unseen, it turns for home from wherever it has got to.
		if (e.body != null) {
			e.legFrom = BlockPos.containing(e.at(sect, now));
			e.legStart = now;
		}
		e.outcome = outcome(e.task, rank, e.taskRank, random.nextDouble(), random.nextDouble());
		String name = NpcNames.of(member);
		if (e.outcome == Outcome.LOST) {
			// Seen by someone just now, it goes on its way and is gone once out of sight (#second); else gone at once, its body with it if it
			// was on the road unseen, or the moment it is next found where it stood (#second).
			boolean seen = e.body == null && npc != null && !npc.isVacant() && someoneNear(home, npc.position(), NpcTravel.reach(server) + NpcTravel.SLACK);
			if (!seen) {
				gone(server, sect, member, e, now);
				if (npc != null && !npc.isVacant()) lose(npc, e);
			}
		} else {
			List<ItemStack> spoils = spoils(e, rank, random);
			if (e.outcome == Outcome.WOUNDED) {
				spoils = half(spoils);
				sect.resting.put(member, now + REST_DAYS * 24_000L);
			}
			for (ItemStack stack : spoils) if (sect.spoils.size() < SPOILS_KEPT) sect.spoils.add(stack);
			if (e.task == Task.LETTER && e.letterFor != null && sect.founder != null) SectManager.changeStanding(server, e.letterFor, sect.founder, LETTER_STANDING);
			if (e.outcome == Outcome.BREAKTHROUGH && m != null) {
				m.rank = Math.min(PlayerCultivation.LAST_RANK, m.rank + 1);
				if (npc != null && npc.getRank() < m.rank) npc.setRank(m.rank);
			}
		}
		SectManager.dirty(server);
		ServerPlayer founder = sect.founder == null ? null : server.getPlayerList().getPlayer(sect.founder);
		if (founder != null) {
			String key = switch (e.outcome) {
				case DONE -> ModLang.ERRAND_BACK_DONE;
				case WOUNDED -> ModLang.ERRAND_BACK_WOUNDED;
				case BREAKTHROUGH -> ModLang.ERRAND_BACK_BREAKTHROUGH;
				case LOST -> ModLang.ERRAND_BACK_LOST;
			};
			founder.sendSystemMessage(Component.translatable(key, name, e.task.getDisplayName()).withStyle(e.outcome == Outcome.LOST ? ChatFormatting.RED : ChatFormatting.GOLD));
		}
	}

	/**
	 * How an errand of {@code task} at {@code taskRank} comes out for one of {@code rank} ({@code roll}, {@code lucky}: 0..1): lost the likelier the
	 * further the task stands above them (a letter or herbs hardly ever), wounded likewise, else done, now and then with a breakthrough.
	 */
	public static Outcome outcome(Task task, int rank, int taskRank, double roll, double lucky) {
		int gap = rank - taskRank; // stages above the task
		double lost = switch (task) {
			case LETTER -> 0.005;
			case GATHER -> Math.max(0.002, Math.min(0.2, 0.02 - 0.01 * gap));
			default -> Math.max(0.005, Math.min(0.5, 0.05 - 0.02 * gap));
		};
		double wounded = Math.max(0.03, Math.min(0.5, 0.15 - 0.03 * gap));
		if (roll < lost) return Outcome.LOST;
		if (roll < lost + wounded) return Outcome.WOUNDED;
		return lucky < 0.05 ? Outcome.BREAKTHROUGH : Outcome.DONE;
	}

	/** Whether a task at {@code taskRank} stands above one of {@code rank} (the page's warning). */
	public static boolean above(int rank, int taskRank) {
		return taskRank > rank;
	}

	/** What an errand brings back: a hunt its quarry's core, hide and meat, herbs (older for the stronger), a secret realm's stones and now and then a rarity, a letter a gift. */
	static List<ItemStack> spoils(Errand e, int rank, RandomSource random) {
		List<ItemStack> spoils = new ArrayList<>();
		switch (e.task) {
			case HUNT -> {
				spoils.add(DemonCoreItem.of(e.taskRank, QiElement.five()[random.nextInt(5)], null));
				spoils.add(new ItemStack(Items.LEATHER, 1 + random.nextInt(3)));
				spoils.add(new ItemStack(Items.BEEF, 1 + random.nextInt(3)));
			}
			case GATHER -> {
				boolean strong = rank >= PlayerCultivation.rank(Realm.FOUNDATION_BUILDING, Stage.EARLY);
				Item[] herbs = strong ? new Item[] {ModItems.SPIRIT_GINSENG, ModItems.PURPLE_LINGZHI, ModItems.OCHRE_HUANGJING}
						: new Item[] {ModItems.GINSENG, ModItems.SPIRIT_GINSENG};
				int years = rank >= PlayerCultivation.rank(Realm.CORE_FORMATION, Stage.EARLY) ? 100 : strong ? 30 : 10;
				for (int i = 0, n = 2 + random.nextInt(4); i < n; i++) {
					spoils.add(GinsengItem.create(herbs[random.nextInt(herbs.length)], years + random.nextInt(years * 2)));
				}
			}
			case REALM -> {
				int realm = 1 + CultivatorNpc.realmOf(e.taskRank).ordinal();
				spoils.addAll(SpiritStoneItem.stonesFor(40 * realm * realm));
				if (random.nextFloat() < 0.25f) spoils.add(new ItemStack(ModItems.SPRING_OF_THE_ANCIENTS));
			}
			case LETTER -> spoils.addAll(SpiritStoneItem.stonesFor(5));
		}
		return spoils;
	}

	private static List<ItemStack> half(List<ItemStack> spoils) {
		List<ItemStack> kept = new ArrayList<>();
		for (int i = 0; i < spoils.size(); i++) {
			ItemStack stack = spoils.get(i);
			if (stack.getCount() > 1) kept.add(stack.copyWithCount(Math.max(1, stack.getCount() / 2)));
			else if (i % 2 == 0) kept.add(stack);
		}
		return kept;
	}

	/** Its founder takes the spoils waiting (PlayerSects#collect, with the tribute). @return how many stacks */
	public static int collect(ServerPlayer founder, Sect sect) {
		int taken = 0;
		for (ItemStack stack : sect.spoils) {
			if (!founder.getInventory().add(stack.copy())) founder.drop(stack.copy(), false);
			taken++;
		}
		sect.spoils.clear();
		if (taken > 0) SectManager.dirty(founder.server);
		return taken;
	}

	// --- The cultivator's side ---

	/** {@code npc}'s errand, if its sect sent it on one not yet settled, else null. */
	public static @Nullable Errand errandOf(CultivatorNpc npc) {
		Sect sect = npc.isSectMember() ? npc.sect() : null;
		if (sect == null || !sect.isPlayers()) return null;
		Errand e = sect.errands.get(npc.getUUID());
		return e == null || e.settled ? null : e;
	}

	/**
	 * A second of a player's sect's cultivator (CultivatorNpc#secondTick): one whose body is out on the road unseen is a stale copy, gone; one
	 * taken as a player's own disciple on the road is out no more; one lost, gone the moment nobody sees it; a breakthrough on the road
	 * shown; and out of everyone's sight (or gone in at a secret realm's gate), it goes on unseen.
	 */
	static void second(ServerLevel level, CultivatorNpc npc) {
		Sect sect = npc.isSectMember() ? npc.sect() : null;
		if (sect == null || !sect.isPlayers() || sect.errands.isEmpty()) return;
		Errand e = sect.errands.get(npc.getUUID());
		if (e == null || e.settled || npc.isVacant()) return;
		MinecraftServer server = level.getServer();
		long now = level.getGameTime();
		if (e.body != null) {
			npc.travelling = true; // not a departure: its body is on its way elsewhere
			npc.discard();
			return;
		}
		if (PersonalDisciples.isDisciple(npc)) {
			e.settled = true;
			SectManager.dirty(server);
			return;
		}
		Sect.Member m = sect.members.get(npc.getUUID());
		if (m != null && npc.getRank() < m.rank && e.outcome == Outcome.BREAKTHROUGH) npc.setRank(m.rank);
		boolean watched = someoneNear(level, npc.position(), NpcTravel.reach(server) + NpcTravel.SLACK);
		if (e.outcome == Outcome.LOST) {
			if (!watched) {
				if (sect.members.containsKey(npc.getUUID())) gone(server, sect, npc.getUUID(), e, now);
				lose(npc, e);
				SectManager.dirty(server);
			}
			return;
		}
		boolean inside = e.task == Task.REALM && e.outcome == null && sideways(npc.position(), e.to) <= THERE;
		if (level.dimension() == sect.dimension && (!watched || inside)) {
			takeOut(npc, e, now);
			SectManager.dirty(server);
		}
	}

	/**
	 * {@code npc}'s land unloading under it while it is out (CultivatorNpc#onUnload: its player gone off at once, teleported or logged out, so
	 * it was never seen standing in land that no longer runs): it goes on unseen from there, its body kept with its errand and none saved with
	 * the land (CultivatorNpc#shouldBeSaved). Not one lost (it is found where it stood, and gone), nor one dying or discarded. @return whether
	 * it went on
	 */
	static boolean unloaded(CultivatorNpc npc) {
		if (npc.isVacant() || !npc.isAlive() || npc.travelling || !(npc.level() instanceof ServerLevel level)) return false;
		Entity.RemovalReason reason = npc.getRemovalReason();
		if (reason != null && !reason.shouldSave()) return false;
		Errand e = errandOf(npc);
		Sect sect = npc.sect();
		if (e == null || sect == null || e.body != null || e.outcome == Outcome.LOST || level.dimension() != sect.dimension) return false;
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return false;
		e.body = tag;
		e.legFrom = npc.blockPosition();
		e.legStart = level.getGameTime();
		npc.goneOnTheRoad = true;
		SectManager.dirty(level.getServer());
		return true;
	}

	/** Out of the world, its body kept with its errand, to go on unseen from where it stands. */
	static void takeOut(CultivatorNpc npc, Errand e, long now) {
		CompoundTag tag = new CompoundTag();
		if (!npc.saveAsPassenger(tag)) return;
		e.body = tag;
		e.legFrom = npc.blockPosition();
		e.legStart = now;
		npc.travelling = true; // not a departure (CultivatorNpc#remove): it goes on its way
		npc.discard();
	}

	/**
	 * {@code member}, lost on the road: struck from its sect's roll (where it last stood kept, to find it by), mourned there and talked of round
	 * it (Rumours#noteLost); its body gone with it if it was on the road unseen.
	 */
	private static void gone(MinecraftServer server, Sect sect, UUID member, Errand e, long now) {
		Sect.Member m = sect.members.remove(member);
		ServerLevel home = server.getLevel(sect.dimension);
		if (m != null) {
			e.lastAt = m.lastAt;
			if (home != null) SectManager.retitle(home, sect);
		}
		String name = NpcNames.of(member);
		if (home != null) NpcLife.mournAway(home, sect, name, now);
		Rumours.noteLost(sect, name, e.task, now);
		if (e.body != null) {
			e.body = null;
			e.settled = true;
		}
		SectManager.dirty(server);
	}

	/** One lost on the road, out of everyone's sight: gone for good. */
	private static void lose(CultivatorNpc npc, Errand e) {
		npc.travelling = true; // struck from its sect's roll already (#comeBack)
		npc.discard();
		e.settled = true;
	}

	/**
	 * Back in the world at {@code at} on its way, as NpcTravel's travellers are met: one that can keep itself up cruising above the land, still
	 * bound where it was; one that can't, on the land below (over open sky it can't, so it stays unseen for now). @return whether it is back
	 */
	private static boolean bringBack(ServerLevel level, Sect sect, UUID id, Errand e, Vec3 at) {
		Entity entity = EntityType.loadEntityRecursive(e.body, level, x -> x);
		if (!(entity instanceof CultivatorNpc npc)) {
			e.body = null; // nothing to bring back
			e.settled = true;
			return true;
		}
		Vec3 way = Vec3.atBottomCenterOf(e.goal(sect)).subtract(Vec3.atBottomCenterOf(e.legFrom));
		float yaw = way.horizontalDistanceSqr() < 1e-6 ? npc.getYRot() : (float) (Mth.atan2(way.z, way.x) * Mth.RAD_TO_DEG) - 90.0f;
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
		boolean land = ground > level.getMinBuildHeight() + 1;
		boolean flies = npc.canFlySustainably();
		double y;
		if (flies) y = land ? Mth.clamp(at.y, ground + 14, ground + 40) : at.y;
		else if (land) y = ground;
		else return false;
		npc.moveTo(at.x, y, at.z, yaw, 0);
		npc.setDeltaMovement(Vec3.ZERO);
		npc.travelling = false;
		caughtUp(sect, id, npc);
		if (!level.addFreshEntity(npc)) return false;
		if (flies) npc.setQiFlying(true);
		e.body = null;
		return true;
	}

	/** Home unseen: set down on its own mat ({@code mat}), its errand done with. @return whether it is back */
	private static boolean setDown(ServerLevel level, Sect sect, UUID id, Errand e, BlockPos mat) {
		Entity entity = EntityType.loadEntityRecursive(e.body, level, x -> x);
		e.settled = true;
		if (!(entity instanceof CultivatorNpc npc)) {
			e.body = null;
			return true;
		}
		npc.moveTo(mat.getX() + 0.5, mat.getY(), mat.getZ() + 0.5, npc.getYRot(), 0);
		npc.setDeltaMovement(Vec3.ZERO);
		npc.setQiFlying(false);
		npc.travelling = false;
		caughtUp(sect, id, npc);
		if (!level.addFreshEntity(npc)) {
			e.settled = false;
			return false;
		}
		e.body = null;
		return true;
	}

	/** A breakthrough it made on the road (the roll keeps it) shown on its body. */
	private static void caughtUp(Sect sect, UUID id, CultivatorNpc npc) {
		Sect.Member m = sect.members.get(id);
		if (m != null && npc.getRank() < m.rank) npc.setRank(m.rank);
	}

	/** Its own mat at home (Sect#dwelling), where one back unseen is set down. */
	private static BlockPos dwelling(Sect sect, UUID id) {
		Sect.Member m = sect.members.get(id);
		return m == null ? sect.core : sect.dwelling(m.home);
	}

	private static double sideways(Vec3 from, BlockPos to) {
		double dx = to.getX() + 0.5 - from.x, dz = to.getZ() + 0.5 - from.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Whether a player (not a spectator) is within {@code reach} of {@code at}, sideways. */
	private static boolean someoneNear(ServerLevel level, Vec3 at, double reach) {
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) continue;
			double dx = player.getX() - at.x, dz = player.getZ() - at.z;
			if (dx * dx + dz * dz <= reach * reach) return true;
		}
		return false;
	}

	/** Whether {@code npc} hangs over water or lava (it keeps up there rather than come down into it). */
	private static boolean overLiquid(CultivatorNpc npc) {
		Level level = npc.level();
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ());
		return !level.getFluidState(new BlockPos(npc.getBlockX(), ground - 1, npc.getBlockZ())).isEmpty();
	}

	/**
	 * One sent on an errand goes there (through the air if it can, else on foot), lingers there till its time is up, and comes home (one lost
	 * goes on, never to come back): above its sect's day, its seclusion and its journeys, below its fights, its flight and its talk.
	 */
	static final class ErrandGoal extends Goal {
		private final CultivatorNpc npc;

		ErrandGoal(CultivatorNpc npc) {
			this.npc = npc;
			setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return !npc.isVacant() && npc.getTarget() == null && !npc.shouldFlee() && npc.talkPartner() == null && !PersonalDisciples.isDisciple(npc)
					&& errandOf(npc) != null;
		}

		@Override
		public boolean canContinueToUse() {
			return canUse();
		}

		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void start() {
			npc.setMeditating(false);
		}

		@Override
		public void stop() {
			npc.getNavigation().stop();
		}

		@Override
		public void tick() {
			Errand e = errandOf(npc);
			Sect sect = npc.sect();
			if (e == null || sect == null) return;
			BlockPos goal = e.goal(sect);
			double dx = goal.getX() + 0.5 - npc.getX(), dz = goal.getZ() + 0.5 - npc.getZ();
			double sideways = Math.sqrt(dx * dx + dz * dz);
			Level level = npc.level();
			int ground = level.hasChunkAt(npc.blockPosition()) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, npc.getBlockX(), npc.getBlockZ())
					: level.getMinBuildHeight();
			boolean land = ground > level.getMinBuildHeight() + 1;
			if (sideways <= THERE) {
				// Down onto the land there; over water, or over nothing at all (an island world's sky), it keeps up in the air.
				if (npc.isQiFlying() && land && !overLiquid(npc)) npc.land();
				if (e.outcome != null && e.outcome != Outcome.LOST) {
					e.settled = true; // home
					if (level instanceof ServerLevel server) SectManager.dirty(server.getServer());
					return;
				}
				// There: about its task, a little this way and that.
				if (!npc.isQiFlying() && npc.getNavigation().isDone() && npc.getRandom().nextInt(60) == 0) {
					npc.getNavigation().moveTo(goal.getX() + npc.getRandom().nextInt(17) - 8, goal.getY(), goal.getZ() + npc.getRandom().nextInt(17) - 8, 0.8);
				}
				return;
			}
			if (!npc.isQiFlying() && npc.canFlySustainably()) npc.setQiFlying(true);
			if (npc.isQiFlying()) {
				// Above the land under it; over nothing at all, at least at the height it is bound for, and so too once it is near there
				// (a sect on a floating island, or high over the land round it), so it comes in over it, never under it.
				double low = land ? ground + 14 : goal.getY() + 4;
				if (sideways < APPROACH) low = Math.max(low, goal.getY() + 4);
				double cruise = Math.max(low, Math.min(npc.getY(), low + 26));
				npc.flyToward(new Vec3(npc.getX() + dx / sideways * 16, cruise, npc.getZ() + dz / sideways * 16), 0.45);
			} else if (npc.getNavigation().isDone()) {
				CultivatorGoals.walkToward(npc, new Vec3(npc.getX() + dx / sideways * 24, npc.getY(), npc.getZ() + dz / sideways * 24), 1.0);
			}
		}
	}

	private SectErrands() {}
}
