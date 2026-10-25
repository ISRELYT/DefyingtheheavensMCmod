package com.example.defyingtheheavens.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
		// A little of its own each time it is heard (the user's ask, 2026-10-25: "better, synced, varied sounds"): the same cast twice is
		// never quite the same sound.
		float heard = loop ? pitch : pitch * (1 + (mc.level.random.nextFloat() - 0.5f) * 0.08f);
		Voiced instance = new Voiced(sound, handle, at, followed, volume, heard, loop);
		VOICES.computeIfAbsent(voice, k -> new ArrayList<>()).add(instance);
		mc.getSoundManager().play(instance);
		if (!loop) weigh(voice, handle, sound, at, followed, volume, heard);
	}

	/**
	 * A great blow's weight, heard after it (under its voice, hushed with it): a great blast's low thud and the debris raining down after it,
	 * and the thunder of the greatest rolling away; earth landing hard, stone breaking and gravel settling; a great splash, the sea's rush and
	 * its water falling back. Each a moment after the blow itself, as what follows a blow comes.
	 */
	private static void weigh(long voice, int handle, SoundEvent sound, Vec3 at, Entity followed, float volume, float pitch) {
		if (volume < 2.5f) return;
		if (sound == SoundEvents.GENERIC_EXPLODE || sound == SoundEvents.DRAGON_FIREBALL_EXPLODE) {
			after(voice, handle, SoundEvents.GENERIC_EXPLODE, at, followed, volume * 0.55f, pitch * 0.5f, 3);
			after(voice, handle, SoundEvents.STONE_FALL, at, followed, volume * 0.45f, 0.55f, 7);
			after(voice, handle, SoundEvents.GRAVEL_FALL, at, followed, volume * 0.35f, 0.6f, 12);
			if (volume >= 4) after(voice, handle, SoundEvents.LIGHTNING_BOLT_THUNDER, at, followed, volume * 0.25f, 0.45f, 5);
		} else if (sound == SoundEvents.ANVIL_LAND) {
			after(voice, handle, SoundEvents.STONE_BREAK, at, followed, volume * 0.6f, 0.45f, 2);
			after(voice, handle, SoundEvents.GRAVEL_BREAK, at, followed, volume * 0.45f, 0.5f, 6);
		} else if (sound == SoundEvents.GENERIC_SPLASH) {
			after(voice, handle, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, at, followed, volume * 0.5f, 0.6f, 2);
			after(voice, handle, SoundEvents.WEATHER_RAIN, at, followed, volume * 0.4f, 0.7f, 8);
		}
	}

	/** {@code sound} played under {@code voice}, {@code ticks} after now. */
	private static void after(long voice, int handle, SoundEvent sound, Vec3 at, Entity followed, float volume, float pitch, int ticks) {
		Voiced layer = new Voiced(sound, handle, at, followed, volume, pitch, false);
		VOICES.computeIfAbsent(voice, k -> new ArrayList<>()).add(layer);
		Minecraft.getInstance().getSoundManager().playDelayed(layer, ticks);
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
