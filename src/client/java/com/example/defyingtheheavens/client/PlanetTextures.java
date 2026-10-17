package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.AstralPlanets;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Astral Realm's painted textures: each planet's face at the size it is seen (PlanetPainter, on two threads of their own, the
 * nearest and largest first), its rings, the nebula behind everything, a star's face and a soft glow. A planet keeps its best
 * texture until a sharper one is ready (no flicker as it grows nearer); those not seen for a while are let go, the largest first
 * when they take too much memory.
 */
final class PlanetTextures {
	/** Sizes a planet is painted at (width; height half that). */
	static final int[] SIZES = {128, 256, 512, 1024, 2048};
	/** Most pixels kept for planets at once (about 80 MB); past this, those least lately seen go. */
	private static final long BUDGET = 20_000_000L;
	/** Unseen this long (ms), a planet's texture goes. */
	private static final long FORGET_AFTER = 45_000;

	private static final AtomicInteger THREADS = new AtomicInteger();
	private static final ThreadPoolExecutor PAINTERS = new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), runnable -> {
		Thread thread = new Thread(runnable, "DTH planet painter " + THREADS.incrementAndGet());
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY + 1);
		return thread;
	});

	/** A planet's texture as it stands. */
	private static final class Entry {
		ResourceLocation id;
		int width;
		/** The width being painted now (0: none). */
		int painting;
		long seen;
	}

	private record Done(long seed, int width, NativeImage image, int generation) {}

	private static final Map<Long, Entry> PLANETS = new HashMap<>();
	private static final Map<Long, ResourceLocation> RINGS = new HashMap<>();
	private static final ConcurrentLinkedQueue<Done> DONE = new ConcurrentLinkedQueue<>();
	/** Bumped when everything is let go (a new world), so paintings from before land nowhere. */
	private static volatile int generation;
	private static ResourceLocation nebula, starFace, glow;
	private static long nebulaSeed;
	private static volatile NativeImage nebulaImage;
	private static boolean nebulaAsked;

	/** A painting job, the one most needed first. */
	private record Job(double priority, Runnable work) implements Runnable, Comparable<Job> {
		@Override public void run() { work.run(); }

		@Override public int compareTo(Job other) { return Double.compare(other.priority, priority); }
	}

	/** The width a planet {@code pixels} across on screen should be painted at: about as many texels round its face as it shows. */
	static int widthFor(double pixels) {
		double want = pixels * 2.2;
		for (int size : SIZES) if (size >= want) return size;
		return SIZES[SIZES.length - 1];
	}

	/**
	 * The best texture {@code planet} has now (null: none yet), asking for one {@code width} wide if it lacks it. {@code sun}: the
	 * way to its star; {@code priority}: how much it's needed (its size on screen).
	 */
	static ResourceLocation planet(AstralPlanets.Planet planet, Vec3 sun, int width, double priority) {
		Entry entry = PLANETS.computeIfAbsent(planet.seed(), s -> new Entry());
		entry.seen = System.currentTimeMillis();
		if (entry.width < width && entry.painting < width) {
			// Smallest first while there is nothing at all, so something shows at once; then straight to the size wanted.
			int paint = entry.id == null && width > SIZES[2] ? SIZES[2] : width;
			entry.painting = paint;
			int gen = generation;
			PAINTERS.execute(new Job(priority + (entry.id == null ? 1e6 : 0), () -> {
				try {
					NativeImage image = PlanetPainter.paint(planet, sun, paint);
					DONE.add(new Done(planet.seed(), paint, image, gen));
				} catch (Throwable t) {
					DefyingTheHeavens.LOGGER.error("Painting planet {} failed", planet.name(), t);
				}
			}));
		}
		return entry.id;
	}

	/** {@code planet}'s rings (painted at once: a strip). */
	static ResourceLocation rings(AstralPlanets.Planet planet, PlanetLooks.Look look) {
		return RINGS.computeIfAbsent(planet.seed(), seed -> register("astral/rings_" + Long.toHexString(seed), PlanetPainter.rings(planet, look)));
	}

	/** The nebula behind everything, for this realm's seed (painted on a painter thread; null until it's ready). */
	static ResourceLocation nebula(long seed) {
		if (nebula != null && nebulaSeed == seed) return nebula;
		NativeImage ready = nebulaImage;
		if (ready != null && nebulaSeed == seed) {
			nebulaImage = null;
			if (nebula != null) Minecraft.getInstance().getTextureManager().release(nebula);
			nebula = register("astral/nebula_" + Long.toHexString(seed), ready);
			return nebula;
		}
		if (!nebulaAsked || nebulaSeed != seed) {
			nebulaAsked = true;
			nebulaSeed = seed;
			PAINTERS.execute(new Job(2e6, () -> {
				try {
					nebulaImage = PlanetPainter.nebula(seed, 2048);
				} catch (Throwable t) {
					DefyingTheHeavens.LOGGER.error("Painting the nebula failed", t);
				}
			}));
		}
		return null;
	}

	static ResourceLocation starFace() {
		if (starFace == null) starFace = register("astral/star_face", PlanetPainter.starFace());
		return starFace;
	}

	static ResourceLocation glow() {
		if (glow == null) glow = register("astral/glow", PlanetPainter.glow());
		return glow;
	}

	/** Each frame, on the render thread: what the painters finished goes up to the GPU, and what's long unseen goes. */
	static void frame() {
		Done done;
		while ((done = DONE.poll()) != null) {
			if (done.generation != generation) {
				done.image.close();
				continue;
			}
			Entry entry = PLANETS.get(done.seed);
			if (entry == null || done.width <= entry.width) {
				done.image.close();
				if (entry != null && entry.painting == done.width) entry.painting = 0;
				continue;
			}
			ResourceLocation old = entry.id;
			entry.id = register("astral/planet_" + Long.toHexString(done.seed) + "_" + done.width, done.image);
			entry.width = done.width;
			if (entry.painting == done.width) entry.painting = 0;
			if (old != null) Minecraft.getInstance().getTextureManager().release(old);
		}
		long now = System.currentTimeMillis();
		long pixels = 0;
		for (Iterator<Map.Entry<Long, Entry>> it = PLANETS.entrySet().iterator(); it.hasNext(); ) {
			Entry entry = it.next().getValue();
			if (now - entry.seen > FORGET_AFTER && entry.painting == 0) {
				if (entry.id != null) Minecraft.getInstance().getTextureManager().release(entry.id);
				it.remove();
				continue;
			}
			pixels += (long) entry.width * entry.width / 2;
		}
		if (pixels > BUDGET) shrink(pixels);
	}

	/** Over the budget: the least lately seen go first. */
	private static void shrink(long pixels) {
		java.util.List<Map.Entry<Long, Entry>> all = new java.util.ArrayList<>(PLANETS.entrySet());
		all.sort((a, b) -> Long.compare(a.getValue().seen, b.getValue().seen));
		for (Map.Entry<Long, Entry> e : all) {
			if (pixels <= BUDGET * 3 / 4) break;
			Entry entry = e.getValue();
			if (entry.id == null || entry.painting != 0) continue;
			pixels -= (long) entry.width * entry.width / 2;
			Minecraft.getInstance().getTextureManager().release(entry.id);
			PLANETS.remove(e.getKey());
		}
	}

	private static ResourceLocation register(String path, NativeImage image) {
		DynamicTexture texture = new DynamicTexture(image);
		texture.setFilter(true, false);
		ResourceLocation id = DefyingTheHeavens.id(path);
		Minecraft.getInstance().getTextureManager().register(id, texture);
		return id;
	}

	/** Leaving the world: every painted texture goes. */
	static void clear() {
		generation++;
		var textures = Minecraft.getInstance().getTextureManager();
		for (Entry entry : PLANETS.values()) if (entry.id != null) textures.release(entry.id);
		for (ResourceLocation id : RINGS.values()) textures.release(id);
		PLANETS.clear();
		RINGS.clear();
		Done done;
		while ((done = DONE.poll()) != null) done.image.close();
		if (nebula != null) textures.release(nebula);
		nebula = null;
		nebulaAsked = false;
		NativeImage pending = nebulaImage;
		nebulaImage = null;
		if (pending != null) pending.close();
	}

	private PlanetTextures() {}
}
