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

	/** {@code npc} and {@code player} are of one sect. */
	public static boolean areFellows(CultivatorNpc npc, ServerPlayer player) {
		return npc.isSectMember() && isDisciple(player.server, npc.getSectId(), player.getUUID());
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
		Sect.Disciple disciple = sect.disciples.get(player.getUUID());
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

	/** How much faster {@code player} cultivates where they are: {@link #GROUNDS_BONUS} on their own sect's grounds, else 1. */
	public static double groundsMultiplier(ServerPlayer player) {
		Sect sect = sectOf(player.server, player.getUUID());
		if (sect == null || sect.extinct || sect.dimension != player.level().dimension() || !sect.inTerritory(player.blockPosition())) return 1;
		return GROUNDS_BONUS;
	}

	// --- Telling the player ---

	/** Every 5 s: a disciple whose title changed (a breakthrough, the sect's members changing) is told so. */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 100 != 0) return;
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

	/** A player logged out: what is kept of them here goes. */
	public static void forget(UUID player) {
		TOLD.remove(player);
	}

	/** Server stopped. */
	public static void clear() {
		TRIED.clear();
		TOLD.clear();
	}

	private SectMembership() {}
}
