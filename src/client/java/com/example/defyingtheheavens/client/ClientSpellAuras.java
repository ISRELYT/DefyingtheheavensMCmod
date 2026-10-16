package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellAura;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * The spell auras on the beings this client sees (SpellAuras on the server tells it): which, since when and until when (in the
 * level's game time), so each is drawn growing in and fading out. Also how the client's own movement reads them: Crimson
 * Tempering and Azure Breathing let its player stand on lava and water (SpellAuras#clientView).
 */
public final class ClientSpellAuras {
	/** Each being's auras (by entity id): when each began and when it ends. */
	private static final Map<Integer, EnumMap<SpellAura, long[]>> AURAS = new HashMap<>();

	/** The server's word: entity {@code id} has these auras now ({@code left}: ticks each has left), and no others. */
	public static void set(int id, int[] auras, int[] left) {
		long now = now();
		EnumMap<SpellAura, long[]> old = AURAS.get(id);
		EnumMap<SpellAura, long[]> next = new EnumMap<>(SpellAura.class);
		for (int i = 0; i < auras.length; i++) {
			SpellAura aura = SpellAura.byIndex(auras[i]);
			if (aura == null) continue;
			long[] was = old == null ? null : old.get(aura);
			next.put(aura, new long[]{was != null ? was[0] : now, now + left[i]});
		}
		if (next.isEmpty()) AURAS.remove(id);
		else AURAS.put(id, next);
	}

	private static long now() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0 : mc.level.getGameTime();
	}

	/** Whether {@code e} has {@code aura} now. */
	public static boolean has(Entity e, SpellAura aura) {
		EnumMap<SpellAura, long[]> mine = AURAS.get(e.getId());
		long[] span = mine == null ? null : mine.get(aura);
		return span != null && now() < span[1];
	}

	/** Ticks since {@code aura} began on {@code e} (with the partial tick), or -1 if it hasn't it. */
	public static float age(Entity e, SpellAura aura, float partial) {
		EnumMap<SpellAura, long[]> mine = AURAS.get(e.getId());
		long[] span = mine == null ? null : mine.get(aura);
		return span == null ? -1 : now() - span[0] + partial;
	}

	/** Ticks {@code aura} has left on {@code e} (with the partial tick), or 0. */
	public static float left(Entity e, SpellAura aura, float partial) {
		EnumMap<SpellAura, long[]> mine = AURAS.get(e.getId());
		long[] span = mine == null ? null : mine.get(aura);
		return span == null ? 0 : Math.max(0, span[1] - now() - partial);
	}

	/** How much of {@code aura} shows on {@code e}: growing in over its first {@code in} ticks, fading over its last {@code out}. */
	public static float shown(Entity e, SpellAura aura, float partial, float in, float out) {
		float age = age(e, aura, partial);
		if (age < 0 || !has(e, aura)) return 0;
		return Math.min(in <= 0 ? 1 : Math.min(1, age / in), out <= 0 ? 1 : Math.min(1, left(e, aura, partial) / out));
	}

	/** Every being's auras (entity id to its auras), to draw. */
	public static Map<Integer, EnumMap<SpellAura, long[]>> all() {
		return AURAS;
	}

	/** Whether {@code e} has any aura at all. */
	public static boolean any(Entity e) {
		return AURAS.containsKey(e.getId());
	}

	/** Forgets the auras that have run out (the server's word that they ended may be lost with a being out of sight). */
	public static void tick() {
		long now = now();
		AURAS.values().removeIf(mine -> {
			mine.values().removeIf(span -> now > span[1] + 20);
			return mine.isEmpty();
		});
	}

	public static void clear() {
		AURAS.clear();
	}

	private ClientSpellAuras() {}
}
