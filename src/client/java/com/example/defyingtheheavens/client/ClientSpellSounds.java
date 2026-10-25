package com.example.defyingtheheavens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The spells' sounds as this client hears them (SpellSounds): each under its spell's voice, at its place or following the thing it was
 * given (a spell's living shape, a being), once or over and over; hushed with its spell, it fades out in the moment it was given and stops,
 * as its shape fades. A sound following a thing that is gone (its shape taken away) fades out too.
 */
public final class ClientSpellSounds {
	/** Each voice's sounds still sounding. */
	private static final Map<Long, List<Voiced>> VOICES = new HashMap<>();
	/** How long a sound fades whose thing is gone without its voice being hushed (ticks). */
	private static final int LOST_FADE = 8;

	private ClientSpellSounds() {}

	/** The server's word: a sound to play. */
	public static void play(long voice, int handle, ResourceLocation id, Vec3 at, int follow, float volume, float pitch, boolean loop) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);
		if (sound == null) return;
		Entity followed = follow == 0 ? null : mc.level.getEntity(follow);
		Voiced instance = new Voiced(sound, handle, at, followed, volume, pitch, loop);
		VOICES.computeIfAbsent(voice, k -> new ArrayList<>()).add(instance);
		mc.getSoundManager().play(instance);
	}

	/** The server's word: a voice hushed ({@code handle} 0: all its sounds), fading out over {@code fade} ticks. */
	public static void hush(long voice, int handle, int fade) {
		List<Voiced> sounds = VOICES.get(voice);
		if (sounds == null) return;
		for (Voiced sound : sounds) if (handle == 0 || sound.handle == handle) sound.fade(fade);
		if (handle == 0) VOICES.remove(voice);
	}

	/** Every client tick: what has stopped forgotten. */
	public static void tick() {
		if (VOICES.isEmpty()) return;
		VOICES.values().forEach(list -> list.removeIf(Voiced::isStopped));
		VOICES.values().removeIf(List::isEmpty);
	}

	/** Leaving a world: all of it stopped. */
	public static void clear() {
		for (List<Voiced> sounds : VOICES.values()) for (Voiced sound : sounds) sound.fade(0);
		VOICES.clear();
	}

	/** One sound under a voice. */
	private static final class Voiced extends AbstractTickableSoundInstance {
		final int handle;
		private final Entity follow;
		private final float full;
		/** Ticks left of its fading out, of how many (-1: not fading). */
		private int fadeLeft = -1, fadeOf = 1;

		Voiced(SoundEvent sound, int handle, Vec3 at, Entity follow, float volume, float pitch, boolean loop) {
			super(sound, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
			this.handle = handle;
			this.follow = follow;
			this.full = volume;
			this.volume = volume;
			this.pitch = pitch;
			this.looping = loop;
			this.delay = 0;
			this.attenuation = SoundInstance.Attenuation.LINEAR;
			Vec3 from = follow != null ? follow.position() : at;
			this.x = from.x;
			this.y = from.y;
			this.z = from.z;
		}

		void fade(int ticks) {
			if (ticks <= 0) {
				stop();
				return;
			}
			if (fadeLeft >= 0 && fadeLeft <= ticks) return; // already going sooner
			fadeLeft = fadeOf = ticks;
		}

		@Override
		public void tick() {
			if (follow != null) {
				if (follow.isRemoved()) fade(LOST_FADE);
				else {
					x = follow.getX();
					y = follow.getY() + follow.getBbHeight() * 0.5;
					z = follow.getZ();
				}
			}
			if (fadeLeft >= 0) {
				volume = full * Mth.clamp(fadeLeft / (float) fadeOf, 0, 1);
				if (fadeLeft-- <= 0) stop();
			}
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
