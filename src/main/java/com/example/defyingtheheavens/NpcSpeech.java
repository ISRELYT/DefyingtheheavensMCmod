package com.example.defyingtheheavens;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/**
 * A cultivator saying something aloud of its own accord (lines from NpcLines, see NpcLife): the players near enough to hear it
 * see it over its head, above its name, for as long as it takes to read (client: ClientNpcSpeech, drawn by
 * CultivatorNpcRenderer). Nothing goes to the chat.
 */
public final class NpcSpeech {
	/** Players this close hear it. */
	public static final double HEAR_RANGE = 20;
	/** A cultivator waits at least this long after it last spoke before remarking on anything else unprompted (ticks). */
	public static final int REMARK_GAP = 160;

	/** Says {@code line} (if anyone is near enough to hear it). @return false if there was nothing to say */
	public static boolean say(CultivatorNpc npc, Component line) {
		if (line == null || !(npc.level() instanceof ServerLevel level) || !npc.isAlive()) return false;
		int ticks = ticksFor(line);
		npc.spoke(level.getGameTime(), ticks);
		for (ServerPlayer player : PlayerLookup.tracking(npc)) {
			if (player.distanceToSqr(npc) <= HEAR_RANGE * HEAR_RANGE) ModPackets.sendNpcSay(player, npc.getId(), line, ticks);
		}
		return true;
	}

	/** Whether it may remark on something now: it isn't still saying something, and has been quiet a while. */
	public static boolean mayRemark(CultivatorNpc npc) {
		long now = npc.level().getGameTime();
		return now >= npc.speakingUntil() && now - npc.lastSpoke() >= REMARK_GAP;
	}

	/** Whether anyone can hear {@code npc}. */
	public static boolean heard(CultivatorNpc npc) {
		for (ServerPlayer player : PlayerLookup.tracking(npc)) {
			if (player.distanceToSqr(npc) <= HEAR_RANGE * HEAR_RANGE) return true;
		}
		return false;
	}

	/**
	 * How long {@code line} stays up, in ticks: a couple of seconds, and longer the more there is to read (about fifteen
	 * characters a second). The server can't read the mod's lang files, so the length is NpcLines' English, plus its arguments'.
	 */
	public static int ticksFor(Component line) {
		int chars = 30;
		if (line.getContents() instanceof TranslatableContents translatable) {
			int own = NpcLines.length(translatable.getKey());
			chars = own >= 0 ? own : 40;
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof Component component) {
					chars += component.getContents() instanceof TranslatableContents ? 60 : component.getString().length();
				} else {
					chars += String.valueOf(arg).length();
				}
			}
		} else {
			chars = line.getString().length();
		}
		return Mth.clamp(40 + chars * 4 / 3, 50, 240);
	}

	private NpcSpeech() {}
}
