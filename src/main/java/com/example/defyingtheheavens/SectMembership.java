package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Players in sects (the user's ask, 2026-10-16; the treasury's rules are theirs from 2026-10-12).
 * <ul>
 *   <li><b>Joining.</b> Ask one of a sect's members (talk: Ask, "Ask to join the sect"). Its disciples send you to an elder; an
 *   Elder, a Grand Elder or the Sect Master judges ({@link #refusal}): not one of the dead, nor a mortal, of no other sect, not cast
 *   out of this one (nor just gone from it), not hated by it, and no stronger than its master. Then the <b>entrance trial</b>: a
 *   spar with that elder, holding back to your realm (SparSystem.Terms#TRIAL); win it, or stand your ground to the time, and you
 *   are taken in. A body of High grade or better (Physique), or an elder Warm to you, needs no trial. One trial a day.</li>
 *   <li><b>Belonging.</b> Titled by strength among its members as they are ({@link Sect#titleAmong}: the strongest player a Grand
 *   Elder, the master's seat being a cultivator's). The barrier lets you through (Formation#allows); its members are your
 *   fellows (CultivatorNpc#isAlly: never hostile, and they come to your aid when you are struck near them); on its grounds you
 *   cultivate a quarter faster ({@link #groundsMultiplier}); its elders teach its method once Courteous (others need Warm).</li>
 *   <li><b>Its rules.</b> The treasury is the elders' ({@link #treasury}): below Elder, the first try is a warning, the second casts
 *   you out, and a third (no longer a member) is theft as anyone's. Striking a fellow member outside a spar casts you out
 *   ({@link #onFellowStruck}). You may leave (Ask an elder, "Leave the sect"); the sect takes you back after {@link #LEFT_DAYS}
 *   days, or {@link #EXPELLED_DAYS} once cast out.</li>
 * </ul>
 */
public final class SectMembership {
	/** Cultivating on one's own sect's grounds (its territory): this much faster. */
	public static final double GROUNDS_BONUS = 1.25;
	/** Days before a sect takes back one who left it, and one it cast out. */
	public static final long LEFT_DAYS = 3, EXPELLED_DAYS = 30;
	/** The sect's standing with a player: on joining, on leaving, on being cast out. */
	public static final int WELCOME = 20, LEFT = -10, EXPELLED = -40;
	/** What a disciple's tries at the treasury below Elder come to: the first, a warning; the second, cast out. */
	public static final int TREASURY_WARNINGS = 1;

	/**
	 * Sent on to an elder by one of a sect's disciples, a player is its guest: its barrier lets them through for this long (ticks),
	 * and on for as long as they stay on its grounds after, so it never shuts them in (the user found the elders out of reach behind
	 * it, 2026-10-17).
	 */
	public static final long GUEST_TICKS = 10 * 60 * 20;
	/** Each player's guest passes: sect, and until when (game time). Runtime only. */
	private static final Map<UUID, Map<UUID, Long>> GUESTS = new HashMap<>();
	/** The day each player last took (and failed) an entrance trial with each sect, so it is once a day (runtime only). */
	private static final Map<UUID, Map<UUID, Long>> TRIED = new HashMap<>();
	/** The title last told to each online disciple, to tell them when it changes. */
	private static final Map<UUID, NpcTitle> TOLD = new HashMap<>();

	// --- Who belongs ---

	/** The sect {@code player} belongs to, or null. */
	public static Sect sectOf(MinecraftServer server, UUID player) {
		for (Sect sect : SectData.get(server).all()) {
			if (sect.disciples.containsKey(player)) return sect;
		}
		return null;
	}

	/** Whether {@code player} is a disciple of the sect {@code sectId}. */
	public static boolean isDisciple(MinecraftServer server, UUID sectId, UUID player) {
		if (server == null || sectId == null || player == null) return false;
		Sect sect = SectManager.get(server, sectId);
		return sect != null && sect.disciples.containsKey(player);
	}

	/** {@code npc} and {@code player} are of one sect ({@code player} by the body they are in, if their soul is in one: Possession). */
	public static boolean areFellows(CultivatorNpc npc, ServerPlayer player) {
		if (!npc.isSectMember()) return false;
		if (Possession.inhabits(player.getUUID())) return npc.getSectId().equals(Possession.sectOfBody(player.getUUID()));
		return isDisciple(player.server, npc.getSectId(), player.getUUID());
	}

	/** The sect of the body {@code player} is in, if their soul is in one (Possession), else their own as a disciple; or null. */
	private static Sect bodySect(ServerPlayer player) {
		if (!Possession.inhabits(player.getUUID())) return sectOf(player.server, player.getUUID());
		UUID id = Possession.sectOfBody(player.getUUID());
		return id == null ? null : SectManager.get(player.server, id);
	}

	/** {@code player}'s title in their sect now, by their strength among its members ({@link Sect#titleAmong}). */
	public static NpcTitle titleOf(Sect sect, ServerPlayer player) {
		PlayerCultivation c = CultivationManager.get(player);
		return sect.titleAmong(CultivatorNpc.rankOf(player), c.getCultivation());
	}

	/** An Elder or above of their sect: the treasury is theirs to open. */
	public static boolean isElder(Sect sect, ServerPlayer player) {
		return titleOf(sect, player).isElderOrAbove();
	}

	// --- Joining and leaving ---

	/**
	 * Why {@code npc} (an elder of {@code sect}) won't take {@code player} in: the talk line's key under "join." (with its
	 * argument, days or a sect's name, in {@code arg[0]}), or null if they would (on the trial, or without one).
	 */
	public static String refusal(ServerPlayer player, CultivatorNpc npc, Sect sect, Object[] arg) {
		PlayerCultivation c = CultivationManager.get(player);
		if (GhostSystem.isGhost(player)) return "join.ghost";
		if (c.isMortal()) return "join.mortal";
		Sect other = sectOf(player.server, player.getUUID());
		if (other != null) {
			arg[0] = other.name;
			return "join.other_sect";
		}
		long now = npc.level().getGameTime();
		Long barred = sect.barredUntil.get(player.getUUID());
		if (barred != null && now < barred) {
			arg[0] = Math.max(1, (barred - now + HeavenRitual.DAY - 1) / HeavenRitual.DAY);
			return "join.barred";
		}
		// Loathed (a thief of its treasury, a killer of its people): no waiting mends that.
		if (sect.standing(player.getUUID()) <= Regard.HOSTILE || npc.regard(player) <= Regard.HOSTILE) return "join.hated";
		Sect.Member master = sect.master();
		if (master != null && CultivatorNpc.rankOf(player) > master.rank) return "join.too_strong";
		return null;
	}

	/** {@code player}, sent on to an elder, is {@code sect}'s guest for {@link #GUEST_TICKS}: its barrier opens to them, on their side too. */
	public static void welcomeGuest(ServerPlayer player, Sect sect) {
		GUESTS.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(sect.id, player.level().getGameTime() + GUEST_TICKS);
		Formations.syncSoon();
	}

	/** A guest who turns on the sect (an alarm raised over them: SectManager#alert) is a guest no longer. */
	public static void revokeGuest(ServerPlayer player, Sect sect) {
		Map<UUID, Long> passes = GUESTS.get(player.getUUID());
		if (passes != null && passes.remove(sect.id) != null) Formations.syncSoon();
	}

	/**
	 * Whether {@code player} is the guest of the sect {@code sectId} now (see {@link #welcomeGuest}), or as good as one of its own: in
	 * the body of one of them (Possession), its barrier lets them through.
	 */
	public static boolean isGuest(UUID sectId, UUID player, long now) {
		if (sectId != null && sectId.equals(Possession.sectOfBody(player))) return true;
		Long until = GUESTS.getOrDefault(player, Map.of()).get(sectId);
		return until != null && now < until;
	}

	/** The guests of {@code sect} now (and those in its people's bodies), for the barrier's view on the client (ModPackets#sendFormations). */
	public static java.util.List<UUID> guestsOf(Sect sect, long now) {
		java.util.List<UUID> guests = new java.util.ArrayList<>();
		GUESTS.forEach((player, passes) -> {
			Long until = passes.get(sect.id);
			if (until != null && now < until) guests.add(player);
		});
		for (UUID soul : Possession.tenantsOf(sect.id)) if (!guests.contains(soul)) guests.add(soul);
		return guests;
	}

	/**
	 * Every 5 s: a guest still on the grounds as their pass runs out keeps it while they stay (the barrier mustn't shut them in);
	 * passes run out are dropped, and the barrier's view told.
	 */
	private static void tickGuests(MinecraftServer server) {
		boolean changed = false;
		for (java.util.Iterator<Map.Entry<UUID, Map<UUID, Long>>> players = GUESTS.entrySet().iterator(); players.hasNext(); ) {
			Map.Entry<UUID, Map<UUID, Long>> entry = players.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			for (java.util.Iterator<Map.Entry<UUID, Long>> passes = entry.getValue().entrySet().iterator(); passes.hasNext(); ) {
				Map.Entry<UUID, Long> pass = passes.next();
				Sect sect = SectManager.get(server, pass.getKey());
				long now = player != null ? player.level().getGameTime() : server.overworld().getGameTime();
				if (pass.getValue() - now > 200) continue;
				if (player != null && sect != null && !sect.extinct && sect.dimension == player.level().dimension()
						&& sect.inTerritory(player.blockPosition())) {
					pass.setValue(now + 400);
				} else if (now >= pass.getValue()) {
					passes.remove();
					changed = true;
				}
			}
			if (entry.getValue().isEmpty()) players.remove();
		}
		if (changed) Formations.syncSoon();
	}

	/** Taken in without the trial: a body of High grade or better, or an elder Warm to them. */
	public static boolean excused(ServerPlayer player, CultivatorNpc npc) {
		Physique physique = CultivationManager.get(player).getPhysique();
		return physique != null && physique.grade().ordinal() >= Physique.Grade.HIGH.ordinal() || npc.regard(player) >= Regard.WARM;
	}

	/** Already tried (and failed) the trial with {@code sect} today. */
	public static boolean triedToday(ServerPlayer player, Sect sect, long now) {
		Long day = TRIED.getOrDefault(player.getUUID(), Map.of()).get(sect.id);
		return day != null && day == Regard.day(now);
	}

	/** The entrance trial ended (SparSystem): won or held to a draw, taken in; else not today. @return the elder's answer's key */
	public static String onTrial(ServerPlayer player, CultivatorNpc npc, SparSystem.Outcome outcome) {
		Sect sect = npc.sect();
		if (sect == null) return "join.failed";
		if (outcome == SparSystem.Outcome.WON || outcome == SparSystem.Outcome.DRAW) {
			if (refusal(player, npc, sect, new Object[1]) != null) return "join.failed";
			join(player, sect);
			return "join.passed";
		}
		TRIED.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(sect.id, Regard.day(npc.level().getGameTime()));
		return "join.failed";
	}

	/** {@code player} becomes a disciple of {@code sect}: welcomed (its standing), told their title, the barrier opened to them. */
	public static void join(ServerPlayer player, Sect sect) {
		Sect old = sectOf(player.server, player.getUUID());
		if (old == sect) return;
		if (old != null) remove(player.server, old, player.getUUID());
		sect.disciples.put(player.getUUID(), new Sect.Disciple(player.getUUID(), player.getGameProfile().getName(), player.level().getGameTime()));
		sect.barredUntil.remove(player.getUUID());
		sect.changeStanding(player.getUUID(), Math.max(0, WELCOME - Math.min(WELCOME, sect.standing(player.getUUID()))));
		SectManager.dirty(player.server);
		NpcTitle title = titleOf(sect, player);
		TOLD.put(player.getUUID(), title);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_JOINED, sect.name, title.getDisplayName()).withStyle(ChatFormatting.GOLD));
		player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.8f);
		ModPackets.sendGesture(player, Gesture.BOW);
		Formations.syncSoon(); // its barrier lets them through now, on their side too
		sendStatus(player);
	}

	/** {@code player} leaves {@code sect} of their own will: it takes them back after {@link #LEFT_DAYS} days. */
	public static void leave(ServerPlayer player, Sect sect) {
		if (!remove(player.server, sect, player.getUUID())) return;
		sect.barredUntil.put(player.getUUID(), player.level().getGameTime() + LEFT_DAYS * HeavenRitual.DAY);
		sect.changeStanding(player.getUUID(), LEFT);
		SectManager.dirty(player.server);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_LEFT, sect.name).withStyle(ChatFormatting.GRAY));
		sendStatus(player);
	}

	/**
	 * {@code player} is cast out of {@code sect} (the reason's key: MSG_SECT_EXPELLED_TREASURY or _VIOLENCE): its standing falls,
	 * it won't hear them for {@link #EXPELLED_DAYS} days, and its barrier is shut to them again.
	 */
	public static void expel(ServerPlayer player, Sect sect, String reason) {
		if (!remove(player.server, sect, player.getUUID())) return;
		sect.barredUntil.put(player.getUUID(), player.level().getGameTime() + EXPELLED_DAYS * HeavenRitual.DAY);
		sect.changeStanding(player.getUUID(), EXPELLED);
		SectManager.dirty(player.server);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_EXPELLED, sect.name, Component.translatable(reason)).withStyle(ChatFormatting.RED));
		player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0f, 0.5f);
		sendStatus(player);
	}

	// --- As the body one is in ---

	/**
	 * Where {@code player} stands in a sect, as they act in it (the user's ask, 2026-10-23: a soul may do anything with the body it is
	 * in): in another's body (Possession) as that body, its NPC cultivator a member, its player a disciple; else as themselves.
	 * {@code who} is the one on its rolls, {@code npc} the cultivator whose body it is (null for a player). Null if none of them belongs.
	 */
	public record Seat(Sect sect, UUID who, NpcTitle title, CultivatorNpc npc, boolean inBody) {}

	public static Seat seat(ServerPlayer player) {
		if (!Possession.inhabits(player.getUUID())) {
			Sect sect = sectOf(player.server, player.getUUID());
			return sect == null ? null : new Seat(sect, player.getUUID(), titleOf(sect, player), null, false);
		}
		UUID id = Possession.sectOfBody(player.getUUID());
		Sect sect = id == null ? null : SectManager.get(player.server, id);
		if (sect == null || sect.extinct) return null;
		CultivatorNpc npc = Possession.npcBody(player);
		if (npc != null) {
			Sect.Member member = sect.members.get(npc.getUUID());
			return member == null ? null : new Seat(sect, npc.getUUID(), member.title, npc, true);
		}
		UUID body = Possession.identity(player);
		return sect.disciples.containsKey(body) ? new Seat(sect, body, titleOf(sect, player), null, true) : null;
	}

	/** A disciple's title in {@code sect} by their strength, online or not (one in no body's place: their own record's). */
	static NpcTitle discipleTitle(MinecraftServer server, Sect sect, UUID id) {
		ServerPlayer online = server.getPlayerList().getPlayer(id);
		if (online != null && !Possession.isHeld(id)) return titleOf(sect, online);
		ServerPlayer tenant = Possession.tenantOf(id);
		if (tenant != null) return titleOf(sect, tenant); // their body played by another's soul: its strength as it is now
		PlayerCultivation c = CultivationManager.own(server, id);
		return sect.titleAmong(c.isMortal() ? CultivatorNpc.MORTAL : TwoPaths.weighedRank(c), c.getCultivation());
	}

	/**
	 * Whether one seated as {@code seat} may cast out {@code target}, titled {@code title}: only a soul in the body of a Sect Master
	 * (anyone) or an elder (those below it), Possession; a player's own title, Elder or Grand Elder, casts out no one (the user's
	 * pick after the bug sweep of 2026-10-23: it let one player empty a sect of its lower ranks).
	 */
	public static boolean mayBanish(Seat seat, UUID target, NpcTitle title) {
		return seat != null && seat.inBody() && seat.title().isElderOrAbove() && title != null && !target.equals(seat.who())
				&& title.ordinal() > seat.title().ordinal();
	}

	/**
	 * {@code player} leaves the sect of the body their soul is in (C2S sect_act, or asking an elder: TalkSystem): its NPC cultivator
	 * leaves it, to make its own way once its body is its own again (its seat, the master's too, goes to the strongest left:
	 * SectManager#retitle), or its player is no disciple of it any more. @return whether the body left
	 */
	public static boolean leaveAsBody(ServerPlayer player) {
		Seat seat = seat(player);
		if (seat == null || !seat.inBody()) return false;
		Sect sect = seat.sect();
		if (seat.npc() != null && seat.npc().level() instanceof ServerLevel level) {
			seat.npc().leaveToWander(level);
		} else if (seat.npc() == null && remove(player.server, sect, seat.who())) {
			sect.barredUntil.put(seat.who(), player.level().getGameTime() + LEFT_DAYS * HeavenRitual.DAY);
			sect.changeStanding(seat.who(), LEFT);
		} else {
			return false;
		}
		Possession.bodyLeftSect(seat.who());
		SectManager.dirty(player.server);
		Formations.syncSoon(); // its barrier no longer takes the soul in it for one of its own
		Component name = Possession.displayName(player);
		player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_BODY_LEFT, name == null ? player.getDisplayName() : name, sect.name)
				.withStyle(ChatFormatting.GRAY));
		return true;
	}

	/**
	 * {@code player}, seated as a Sect Master or an elder (in such a one's body: Possession), casts {@code target} out of the sect (C2S
	 * sect_act, the Sect tab): a cultivator sets off as a rogue (SectManager#banish), a player is expelled as for violence, barred for
	 * {@link #EXPELLED_DAYS} days.
	 */
	public static void banish(ServerPlayer player, UUID target) {
		Seat seat = seat(player);
		if (seat == null) return;
		Sect sect = seat.sect();
		Sect.Member member = sect.members.get(target);
		Sect.Disciple disciple = sect.disciples.get(target);
		NpcTitle title = member != null ? member.title : disciple != null ? discipleTitle(player.server, sect, target) : null;
		if (!mayBanish(seat, target, title)) return;
		if (member != null) {
			SectManager.banish(player.server, sect, target);
			player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_BANISHED, NpcNames.of(target), sect.name).withStyle(ChatFormatting.GOLD));
		} else {
			ServerPlayer online = player.server.getPlayerList().getPlayer(target);
			if (online != null && !Possession.isHeld(target)) {
				expel(online, sect, ModLang.MSG_SECT_EXPELLED_BANISHED);
			} else if (remove(player.server, sect, target)) {
				sect.barredUntil.put(target, player.level().getGameTime() + EXPELLED_DAYS * HeavenRitual.DAY);
				sect.changeStanding(target, EXPELLED);
			}
			player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_BANISHED, disciple.name, sect.name).withStyle(ChatFormatting.GOLD));
		}
		Possession.bodyLeftSect(target);
		SectManager.dirty(player.server);
		player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0f, 0.5f);
		sendInfo(player);
	}

	/** Takes {@code player} off {@code sect}'s rolls. @return whether they were on them */
	private static boolean remove(MinecraftServer server, Sect sect, UUID player) {
		if (sect.disciples.remove(player) == null) return false;
		TOLD.remove(player);
		TRIED.remove(player);
		SectManager.dirty(server);
		Formations.syncSoon();
		return true;
	}

	// --- Its rules ---

	/** What came of a disciple's hand on their own sect's treasury. */
	public enum TreasuryTouch {
		/** An Elder or above: it opens to them, sealed or not, and nobody minds. */
		ALLOWED,
		/** Below Elder: stopped (warned, or now cast out), and no alarm. */
		REFUSED,
		/** Not one of its disciples: as anyone (theft, the seal). */
		OUTSIDER
	}

	/**
	 * {@code player} lays hands on the treasury of {@code sect} (opening, breaking): its elders' alone. Below Elder the first try
	 * is a warning and the second casts them out; once out, they are an outsider like any other.
	 */
	public static TreasuryTouch treasury(ServerPlayer player, Sect sect) {
		// In the body of one of its cultivators (Possession): as that one may (an elder's hands open it; anyone else's are held back).
		CultivatorNpc body = Possession.npcBody(player);
		if (body != null && sect.id.equals(body.getSectId())) {
			return body.getTitle().isElderOrAbove() ? TreasuryTouch.ALLOWED : TreasuryTouch.REFUSED;
		}
		Sect.Disciple disciple = sect.disciples.get(Possession.identity(player));
		if (disciple == null) return TreasuryTouch.OUTSIDER;
		if (isElder(sect, player)) return TreasuryTouch.ALLOWED;
		disciple.treasuryTries++;
		SectManager.dirty(player.server);
		if (disciple.treasuryTries <= TREASURY_WARNINGS) {
			player.displayClientMessage(Component.translatable(ModLang.MSG_SECT_TREASURY_WARNING).withStyle(ChatFormatting.RED), true);
			player.level().playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.6f, 1.4f);
		} else {
			expel(player, sect, ModLang.MSG_SECT_EXPELLED_TREASURY);
		}
		return TreasuryTouch.REFUSED;
	}

	/** {@code attacker} struck {@code victim}: a disciple of its sect, outside a spar, is cast out (before the sect answers). */
	public static void onFellowStruck(CultivatorNpc victim, ServerPlayer attacker) {
		if (!victim.isSectMember() || SparSystem.areSparring(victim, attacker) || attacker.isCreative() || attacker.isSpectator()) return;
		Sect sect = victim.sect();
		if (sect != null && sect.disciples.containsKey(attacker.getUUID())) expel(attacker, sect, ModLang.MSG_SECT_EXPELLED_VIOLENCE);
	}

	/**
	 * How much faster {@code player} cultivates where they are: {@link #GROUNDS_BONUS} on their own sect's grounds (the body's, when
	 * they meditate in another's: Possession), else 1.
	 */
	public static double groundsMultiplier(ServerPlayer player) {
		Sect sect = bodySect(player);
		if (sect == null || sect.extinct || sect.dimension != player.level().dimension() || !sect.inTerritory(player.blockPosition())) return 1;
		return GROUNDS_BONUS;
	}

	// --- Telling the player ---

	/** Every 5 s: a disciple whose title changed (a breakthrough, the sect's members changing) is told so. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 0) return;
		tickGuests(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Sect sect = sectOf(server, player.getUUID());
			if (sect == null) continue;
			NpcTitle title = titleOf(sect, player);
			NpcTitle told = TOLD.put(player.getUUID(), title);
			if (told != null && told != title) {
				player.sendSystemMessage(Component.translatable(ModLang.MSG_SECT_TITLE, title.getDisplayName(), sect.name).withStyle(ChatFormatting.GOLD));
				sendStatus(player);
			} else if (told == null) {
				sendStatus(player);
			}
			Sect.Disciple disciple = sect.disciples.get(player.getUUID());
			if (disciple != null && !disciple.name.equals(player.getGameProfile().getName())) {
				disciple.name = player.getGameProfile().getName();
				SectManager.dirty(server);
			}
		}
	}

	/** S2C sect_status: the player's sect's name and their title in it (empty and -1: none), for the Stats tab. */
	public static void sendStatus(ServerPlayer player) {
		Sect sect = sectOf(player.server, player.getUUID());
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeUtf(sect == null ? "" : sect.name);
		buf.writeVarInt(sect == null ? -1 : titleOf(sect, player).ordinal());
		ServerPlayNetworking.send(player, ModPackets.SECT_STATUS, buf);
	}

	/** How far off (blocks) a sect may lie and still be told of on a non-member's Sect tab. */
	public static final int TOLD_OF = 4000;

	/**
	 * S2C sect_info, the Sect tab's page (asked for as it opens): for a disciple, their sect (its name, path, their title and its
	 * standing with them, whether they stand on its grounds or how far off they lie, its barrier, the treasury's rule as it stands
	 * for them, how long they have belonged) and its members (cultivators by seniority, then its players); for one of no sect, the
	 * nearest sect in this world, and whether it would hear them now.
	 */
	public static void sendInfo(ServerPlayer player) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		// In another's body (Possession), the body's sect as the body sees it: its title, and a way out of it (and, a senior's, a way to
		// cast others out); else their own as its disciple.
		Seat seat = seat(player);
		Sect sect = seat == null ? null : seat.sect();
		buf.writeBoolean(sect != null);
		if (sect != null) {
			buf.writeUtf(sect.name);
			buf.writeInt(sect.alignment);
			buf.writeVarInt(seat.title().ordinal());
			buf.writeInt(sect.standing(seat.who()));
			boolean here = sect.dimension == player.level().dimension();
			buf.writeBoolean(here && sect.inTerritory(player.blockPosition()));
			buf.writeVarInt(here ? (int) Math.sqrt(sect.core.distSqr(player.blockPosition())) : -1);
			buf.writeUtf(here ? TalkSystem.direction(player.blockPosition(), sect.core) : "");
			buf.writeBoolean(!sect.isBarrierDown());
			Sect.Disciple disciple = sect.disciples.get(seat.who());
			buf.writeVarInt(disciple == null ? 0 : disciple.treasuryTries);
			buf.writeBoolean(seat.title().isElderOrAbove());
			buf.writeVarLong(disciple == null ? 0 : Math.max(0, player.level().getGameTime() - disciple.joined));
			buf.writeBoolean(seat.inBody());
			java.util.List<Sect.Member> members = new java.util.ArrayList<>(sect.members.values());
			members.sort(Sect.SENIORITY);
			buf.writeVarInt(members.size() + sect.disciples.size());
			for (Sect.Member member : members) {
				buf.writeUtf(NpcNames.of(member.id));
				buf.writeVarInt(member.title.ordinal() + 1);
				buf.writeVarInt(member.rank + 1);
				buf.writeBoolean(false);
				buf.writeUUID(member.id);
				buf.writeBoolean(mayBanish(seat, member.id, member.title));
			}
			for (Sect.Disciple d : sect.disciples.values()) {
				ServerPlayer online = player.server.getPlayerList().getPlayer(d.id);
				buf.writeUtf(d.name);
				buf.writeVarInt(online != null ? titleOf(sect, online).ordinal() + 1 : 0);
				buf.writeVarInt(online != null ? CultivatorNpc.rankOf(online) + 1 : 0);
				buf.writeBoolean(true);
				buf.writeUUID(d.id);
				buf.writeBoolean(mayBanish(seat, d.id, discipleTitle(player.server, sect, d.id)));
			}
		} else {
			Sect near = null;
			double best = (double) TOLD_OF * TOLD_OF;
			for (Sect s : SectData.get(player.server).all()) {
				if (s.extinct || s.dimension != player.level().dimension()) continue;
				double d = s.core.distSqr(player.blockPosition());
				if (d < best) {
					best = d;
					near = s;
				}
			}
			buf.writeBoolean(near != null);
			if (near != null) {
				buf.writeUtf(near.name);
				buf.writeInt(near.alignment);
				buf.writeVarInt((int) Math.sqrt(best));
				buf.writeUtf(TalkSystem.direction(player.blockPosition(), near.core));
				Long barred = near.barredUntil.get(player.getUUID());
				long left = barred == null ? 0 : barred - player.level().getGameTime();
				buf.writeVarInt(left <= 0 ? 0 : (int) Math.max(1, (left + HeavenRitual.DAY - 1) / HeavenRitual.DAY));
			}
		}
		ServerPlayNetworking.send(player, ModPackets.SECT_INFO, buf);
	}

	/** A player logged out: what is kept of them here goes. */
	public static void forget(UUID player) {
		TOLD.remove(player);
	}

	/** Server stopped. */
	public static void clear() {
		TRIED.clear();
		TOLD.clear();
		GUESTS.clear();
	}

	private SectMembership() {}
}
