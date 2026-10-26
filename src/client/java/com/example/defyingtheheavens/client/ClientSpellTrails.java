package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.SpellProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Where each spell missile has been lately (once a client tick, newest first): a fire serpent's body follows its head along it, and
 * the trails of light, fire and smoke behind the others are drawn along it.
 */
public final class ClientSpellTrails {
	/** Points kept per missile. */
	public static final int LENGTH = 24;
	private static final Map<Integer, Deque<Vec3>> TRAILS = new HashMap<>();

	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.isPaused()) return;
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof SpellProjectile missile) || missile.forming()) continue;
			seen.add(e.getId());
			Deque<Vec3> trail = TRAILS.computeIfAbsent(e.getId(), id -> new ArrayDeque<>());
			// Where it is drawn as the next tick begins (its last place, eased from toward its place now): taken from where it is now, the newest
			// point lay ahead of its drawn head through every tick, and what hung on its trail (a serpent's body) folded back and forth.
			Vec3 at = new Vec3(e.xo, e.yo, e.zo);
			if (trail.isEmpty() || trail.peekFirst().distanceToSqr(at) > 1.0e-4) trail.addFirst(at);
			while (trail.size() > LENGTH) trail.removeLast();
		}
		for (Iterator<Integer> it = TRAILS.keySet().iterator(); it.hasNext(); ) {
			if (!seen.contains(it.next())) it.remove();
		}
	}

	/** {@code missile}'s trail, newest first (empty if it has none yet). */
	public static Deque<Vec3> of(Entity missile) {
		Deque<Vec3> trail = TRAILS.get(missile.getId());
		return trail == null ? new ArrayDeque<>() : trail;
	}

	public static void clear() {
		TRAILS.clear();
	}

	private ClientSpellTrails() {}
}
