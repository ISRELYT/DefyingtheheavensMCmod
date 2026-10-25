package com.example.defyingtheheavens;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * A spell's sounds kept to the spell (the user's ask, 2026-10-25: "the sound effects don't match up a lot of the time with the moves, like a
 * move will be over and you can still hear it playing"). Each lingering spell has a voice of its own (HeavenSpells.Timed#sound, #loop): what
 * it plays is played on its watchers' clients as their own sounds under that voice (ClientSpellSounds), at a place or following a thing (its
 * living shape, a being), once or over and over; when the spell is over its voice is hushed, and everything it still sounds fades out in a
 * moment, as its shape fades, rather than ringing on after it (a beacon's hum, a warden's rising, a dragon's growl several seconds long). A
 * loop can be hushed alone ({@link #hush} with its handle) as the part of the spell it belongs to ends.
 */
public final class SpellSounds {
	/** How far a sound of {@code volume} is heard (as vanilla's: 16 blocks a unit of it, no less than 16). */
	public static double reach(float volume) {
		return Math.max(16, volume * 16);
	}

	private static long nextVoice = 1;
	private static int nextHandle = 1;

	/** A voice of its own, for one spell's sounds. */
	public static long voice() {
		return nextVoice++;
	}

	/**
	 * {@code sound} played under {@code voice} at {@code at} (or following {@code follow}, if it isn't null), to everyone in {@code level} near
	 * enough to hear it, who are added to {@code heard}; {@code loop}: over and over until hushed. Returns its handle (to hush it alone).
	 */
	public static int play(ServerLevel level, long voice, Set<ServerPlayer> heard, Vec3 at, Entity follow, SoundEvent sound, float volume, float pitch, boolean loop) {
		int handle = nextHandle++;
		double reach = reach(volume) + (loop ? 24 : 0); // a loop goes on as its listener comes nearer
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) > reach * reach) continue;
			heard.add(player);
			ModPackets.sendSpellSound(player, voice, handle, sound, at, follow == null ? 0 : follow.getId(), volume, pitch, loop);
		}
		return handle;
	}

	/** {@code voice}'s sounds faded out over {@code fade} ticks for everyone who heard it ({@code handle} 0: all of them; else that one). */
	public static void hush(long voice, int handle, Set<ServerPlayer> heard, int fade) {
		for (ServerPlayer player : heard) {
			if (!player.hasDisconnected()) ModPackets.sendSpellSoundHush(player, voice, handle, fade);
		}
		if (handle == 0) heard.clear();
	}

	private SpellSounds() {}
}
