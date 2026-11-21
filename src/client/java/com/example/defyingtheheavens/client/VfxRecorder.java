package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.DevTools;
import com.example.defyingtheheavens.VfxCapture;
import com.example.defyingtheheavens.client.compat.ShaderPacks;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The client's side of /vfxcapture (VfxCapture): while a view is recorded, each tick's first frame is read back at the very end of the
 * frame (VfxFrameMixin: once the world, the post effects, a shader pack, the HUD and any screen are drawn, just before it is shown) and
 * written out on worker threads, named by the server's game time; every frame's time is logged, and a perf view logs nothing else, its
 * frame rate unbound. While it records, its camera, its aim and its HUD are as the server asked, no screen, chat line or toast covers it,
 * and everything is put back after. Dev only (DevTools): registered with -Ddth.dev alone.
 */
public final class VfxRecorder {
	/** Frames read back but not yet written past which more are dropped (each is some 3.7 MB at 1280x720, off the heap). */
	private static final int MAX_PENDING = 240;

	private static final class Run {
		String id, view, label;
		Path dir;
		/** The server's game time less this client's, as the view began: frames are named by the server's. */
		long offset;
		long start;
		boolean cells, perf, hud;
		/** Whether a breakthrough cinematic has held this view's player: from then on the aim is the film's, not held here. */
		boolean filmHeld;
		int camera;
		float yaw, pitch;
		final StringBuilder frames = new StringBuilder("file,gameTime,clientGameTime,partialTick,frame,msSinceBegin\n");
		/**
		 * Every frame's game time, partial tick, length (ms), whether it was read back, the fps then and the live particles (the briefs'
		 * budget: 2,000 at most): kept bare, written out at the end.
		 */
		long[] timeTick = new long[4096];
		float[] timePartial = new float[4096], timeMs = new float[4096];
		boolean[] timeGrabbed = new boolean[4096];
		int[] timeFps = new int[4096], timeParticles = new int[4096];
		final JsonArray marks = new JsonArray();
		final ArrayDeque<String> cellsWanted = new ArrayDeque<>();
		long lastTick = Long.MIN_VALUE, lastNanos, beganNanos;
		int frame, grabs, dropped;
		final AtomicInteger pending = new AtomicInteger(), saved = new AtomicInteger(), failed = new AtomicInteger();
		ExecutorService writer;
		boolean oldHideGui, oldPause;
		CameraType oldCamera;
		int oldFps;
		ChatVisiblity oldChat;
	}

	private static Run run;

	public static void register() {
		if (!DevTools.ENABLED) return;
		ClientPlayNetworking.registerGlobalReceiver(VfxCapture.CHANNEL, (client, handler, buf, sender) -> {
			int op = buf.readByte();
			switch (op) {
				case VfxCapture.OP_BEGIN -> {
					String id = buf.readUtf(), view = buf.readUtf(), dir = buf.readUtf();
					long serverNow = buf.readLong(), start = buf.readLong();
					boolean cells = buf.readBoolean(), perf = buf.readBoolean(), hud = buf.readBoolean();
					int camera = buf.readByte();
					float yaw = buf.readFloat(), pitch = buf.readFloat();
					String label = buf.readUtf();
					client.execute(() -> begin(client, id, view, dir, serverNow, start, cells, perf, hud, camera, yaw, pitch, label));
				}
				case VfxCapture.OP_MARK -> {
					String name = buf.readUtf();
					long at = buf.readLong();
					client.execute(() -> mark(client, name, at));
				}
				case VfxCapture.OP_CELL -> {
					String name = buf.readUtf();
					client.execute(() -> {
						if (run != null) run.cellsWanted.add(name);
					});
				}
				case VfxCapture.OP_END -> {
					String record = buf.readUtf(4_000_000);
					client.execute(() -> end(client, record));
				}
				default -> { }
			}
		});
		ClientTickEvents.START_CLIENT_TICK.register(VfxRecorder::tick);
		// Fabric calls DISCONNECT on the network thread: the ending is the client thread's.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> end(client, null)));
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] the recorder is on");
	}

	private static void begin(Minecraft mc, String id, String view, String dir, long serverNow, long start, boolean cells, boolean perf,
			boolean hud, int camera, float yaw, float pitch, String label) {
		if (run != null) end(mc, null);
		Run r = new Run();
		r.id = id;
		r.view = view;
		r.label = label;
		r.dir = Path.of(dir).resolve(view);
		r.offset = mc.level == null ? 0 : serverNow - mc.level.getGameTime();
		r.start = start - r.offset;
		r.cells = cells;
		r.perf = perf;
		r.hud = hud;
		r.camera = camera;
		r.yaw = yaw;
		r.pitch = pitch;
		try {
			Files.createDirectories(r.dir.resolve(cells ? "cells" : "frames"));
		} catch (IOException e) {
			DefyingTheHeavens.LOGGER.warn("[DTH-VFX] can't make {}", r.dir, e);
			return;
		}
		r.oldHideGui = mc.options.hideGui;
		r.oldPause = mc.options.pauseOnLostFocus;
		r.oldCamera = mc.options.getCameraType();
		r.oldFps = mc.options.framerateLimit().get();
		r.oldChat = mc.options.chatVisibility().get();
		mc.options.hideGui = !hud;
		mc.options.pauseOnLostFocus = false;
		if (camera == VfxCapture.CAMERA_FIRST) mc.options.setCameraType(CameraType.FIRST_PERSON);
		if (camera == VfxCapture.CAMERA_BACK) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		mc.options.chatVisibility().set(ChatVisiblity.HIDDEN); // no chat line over a frame
		if (perf) mc.options.framerateLimit().set(260); // unbound: the perf runs' frame times are the effect's own
		mc.getToasts().clear();
		// A line left in the action bar by what came before the view (the last view's "You open your eyes...") would sit on its first frames.
		mc.gui.setOverlayMessage(net.minecraft.network.chat.Component.empty(), false);
		if (mc.screen != null) mc.setScreen(null);
		r.writer = Executors.newFixedThreadPool(4, task -> {
			Thread t = new Thread(task, "DTH VFX writer");
			t.setDaemon(true);
			return t;
		});
		r.beganNanos = System.nanoTime();
		run = r;
		DefyingTheHeavens.LOGGER.info("[DTH-VFX] recording {} of {} into {}", view, id, r.dir);
	}

	/** While a view is recorded: the aim held where the server set it, and nothing over the picture. */
	private static void tick(Minecraft mc) {
		Run r = run;
		if (r == null || mc.player == null) return;
		mc.options.pauseOnLostFocus = false;
		if (mc.screen != null) mc.setScreen(null);
		mc.getToasts().clear();
		// A cinematic holds the player: its aim, not ours; and once it has, the view the film hands back to is the one recorded (ours would
		// snap the camera back to where the view began, a cut no player sees).
		if (com.example.defyingtheheavens.client.cinematic.CinematicClient.holdsLocal()) r.filmHeld = true;
		if (r.filmHeld) return;
		mc.player.setYRot(r.yaw);
		mc.player.yRotO = r.yaw;
		mc.player.setXRot(r.pitch);
		mc.player.xRotO = r.pitch;
		mc.player.yHeadRot = mc.player.yHeadRotO = r.yaw;
		mc.player.yBodyRot = mc.player.yBodyRotO = r.yaw;
	}

	private static void mark(Minecraft mc, String name, long at) {
		Run r = run;
		if (r == null) return;
		JsonObject m = new JsonObject();
		m.addProperty("name", name);
		m.addProperty("gameTime", at);
		m.addProperty("seenAt", mc.level == null ? at : mc.level.getGameTime() + r.offset);
		r.marks.add(m);
	}

	/** The frame is finished (VfxFrameMixin): its time logged, and, when it is the tick's first, read back and written out. */
	public static void frameEnd(Minecraft mc) {
		Run r = run;
		if (r == null || mc.level == null) return;
		long nanos = System.nanoTime();
		double ms = r.lastNanos == 0 ? 0 : (nanos - r.lastNanos) / 1.0e6;
		r.lastNanos = nanos;
		long tick = mc.level.getGameTime();
		float partial = mc.getFrameTime();
		boolean grabbed = false;
		if (!r.perf) {
			if (!r.cellsWanted.isEmpty()) {
				grabbed = grab(mc, r, "cells/" + r.cellsWanted.poll() + ".png", tick, partial, nanos);
			} else if (!r.cells && tick >= r.start && tick != r.lastTick) {
				r.lastTick = tick;
				grabbed = grab(mc, r, "frames/t" + (tick + r.offset) + ".png", tick, partial, nanos);
			}
		}
		if (r.frame == r.timeTick.length) {
			int size = r.frame * 2;
			r.timeTick = java.util.Arrays.copyOf(r.timeTick, size);
			r.timePartial = java.util.Arrays.copyOf(r.timePartial, size);
			r.timeMs = java.util.Arrays.copyOf(r.timeMs, size);
			r.timeGrabbed = java.util.Arrays.copyOf(r.timeGrabbed, size);
			r.timeFps = java.util.Arrays.copyOf(r.timeFps, size);
			r.timeParticles = java.util.Arrays.copyOf(r.timeParticles, size);
		}
		r.timeTick[r.frame] = tick + r.offset;
		r.timePartial[r.frame] = partial;
		r.timeMs[r.frame] = (float) ms;
		r.timeGrabbed[r.frame] = grabbed;
		r.timeFps[r.frame] = mc.getFps();
		r.timeParticles[r.frame] = particles(mc);
		r.frame++;
	}

	/**
	 * The live particles: vanilla's, as the debug screen counts them (ParticleEngine#countParticles), with the cinematics' own sprites and
	 * streaks drawn in the last frame's effects layer (client/fx: worked out each frame, never in vanilla's engine); -1 if vanilla's can't be read.
	 */
	private static int particles(Minecraft mc) {
		try {
			return Integer.parseInt(mc.particleEngine.countParticles()) + com.example.defyingtheheavens.client.fx.Fx.spritesLastFrame();
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	/** The frame-time log, written out once the view is over. */
	private static String frameTimes(Run r) {
		StringBuilder sb = new StringBuilder(r.frame * 36 + 64).append("frame,gameTime,partialTick,ms,grabbed,fps,particles\n");
		for (int i = 0; i < r.frame; i++) {
			sb.append(i).append(',').append(r.timeTick[i]).append(',').append(String.format(Locale.ROOT, "%.3f", r.timePartial[i])).append(',')
					.append(String.format(Locale.ROOT, "%.3f", r.timeMs[i])).append(',').append(r.timeGrabbed[i] ? 1 : 0).append(',').append(r.timeFps[i])
					.append(',').append(r.timeParticles[i]).append('\n');
		}
		return sb.toString();
	}

	private static boolean grab(Minecraft mc, Run r, String file, long tick, float partial, long nanos) {
		if (r.pending.get() >= MAX_PENDING) {
			r.dropped++;
			return false;
		}
		NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
		Path to = r.dir.resolve(file);
		r.pending.incrementAndGet();
		r.writer.execute(() -> {
			try (image) {
				image.writeToFile(to);
				r.saved.incrementAndGet();
			} catch (IOException | RuntimeException e) {
				r.failed.incrementAndGet();
				DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't write {}", to, e);
			} finally {
				r.pending.decrementAndGet();
			}
		});
		r.grabs++;
		r.frames.append(file).append(',').append(tick + r.offset).append(',').append(tick).append(',').append(String.format(Locale.ROOT, "%.3f", partial))
				.append(',').append(r.frame).append(',').append(String.format(Locale.ROOT, "%.1f", (nanos - r.beganNanos) / 1.0e6)).append('\n');
		return true;
	}

	/**
	 * The view ends ({@code record}: the server's record of the run so far, or null when it ended otherwise): the options put back, and its
	 * frames, frame times and records written out once the last frame is.
	 */
	private static void end(Minecraft mc, String record) {
		Run r = run;
		if (r == null) return;
		run = null;
		mc.options.hideGui = r.oldHideGui;
		mc.options.pauseOnLostFocus = r.oldPause;
		mc.options.setCameraType(r.oldCamera);
		mc.options.chatVisibility().set(r.oldChat);
		if (r.perf) mc.options.framerateLimit().set(r.oldFps);
		JsonObject client = describe(mc, r, record == null);
		Thread finish = new Thread(() -> {
			r.writer.shutdown();
			try {
				if (!r.writer.awaitTermination(5, TimeUnit.MINUTES)) client.addProperty("writerTimedOut", true);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			client.addProperty("saved", r.saved.get());
			client.addProperty("failed", r.failed.get());
			try {
				Files.writeString(r.dir.resolve("frames.csv"), r.frames, StandardCharsets.UTF_8);
				Files.writeString(r.dir.resolve("frametimes.csv"), frameTimes(r), StandardCharsets.UTF_8);
				Files.writeString(r.dir.resolve("client.json"), new GsonBuilder().setPrettyPrinting().create().toJson(client), StandardCharsets.UTF_8);
				if (record != null) Files.writeString(r.dir.resolve("server.json"), record, StandardCharsets.UTF_8);
			} catch (IOException e) {
				DefyingTheHeavens.LOGGER.warn("[DTH-VFX] couldn't write {}'s records", r.dir, e);
			}
			DefyingTheHeavens.LOGGER.info("[DTH-VFX] done {} of {}: {} frames saved, {} failed, {} dropped", r.view, r.id, r.saved.get(), r.failed.get(), r.dropped);
		}, "DTH VFX finish");
		finish.start();
	}

	/** What this client was, as it recorded: its window, settings, graphics card and shader pack, for the checker and the reviewers. */
	private static JsonObject describe(Minecraft mc, Run r, boolean cutShort) {
		JsonObject o = new JsonObject();
		o.addProperty("id", r.id);
		o.addProperty("view", r.view);
		o.addProperty("label", r.label);
		o.addProperty("player", mc.getUser().getName());
		o.addProperty("cutShort", cutShort);
		o.addProperty("written", LocalDateTime.now().toString());
		o.addProperty("offset", r.offset);
		o.addProperty("startGameTime", r.start + r.offset);
		o.addProperty("perf", r.perf);
		o.addProperty("cellsOnly", r.cells);
		o.addProperty("hud", r.hud);
		o.addProperty("camera", r.camera == VfxCapture.CAMERA_BACK ? "third person, behind" : "first person");
		o.addProperty("aimYaw", r.yaw);
		o.addProperty("aimPitch", r.pitch);
		o.addProperty("frames", r.frame);
		o.addProperty("grabs", r.grabs);
		o.addProperty("dropped", r.dropped);
		o.addProperty("width", mc.getWindow().getWidth());
		o.addProperty("height", mc.getWindow().getHeight());
		o.addProperty("guiScale", mc.getWindow().getGuiScale());
		o.addProperty("fov", mc.options.fov().get());
		o.addProperty("fpsLimit", r.perf ? 260 : r.oldFps);
		o.addProperty("renderDistance", mc.options.renderDistance().get());
		o.addProperty("graphics", mc.options.graphicsMode().get().toString());
		o.addProperty("particles", mc.options.particles().get().toString());
		o.addProperty("glRenderer", GlUtil.getRenderer());
		o.addProperty("glVersion", GlUtil.getOpenGLVersion());
		o.addProperty("shaderPackInUse", ShaderPacks.inUse());
		JsonObject mods = new JsonObject();
		for (String mod : new String[] {"iris", "sodium", "veil", "defying-the-heavens", "fabric-api", "geckolib"}) {
			FabricLoader.getInstance().getModContainer(mod).ifPresent(c -> mods.addProperty(mod, c.getMetadata().getVersion().getFriendlyString()));
		}
		o.add("mods", mods);
		o.add("marks", r.marks);
		// A breakthrough cinematic begun while this view was recorded: what this client played, shot by shot (VfxCheck names its beats by them).
		var last = com.example.defyingtheheavens.client.cinematic.CinematicClient.lastBegun();
		if (last != null && last.start >= r.start + r.offset) {
			JsonObject c = new JsonObject();
			c.addProperty("realm", last.realm.getId());
			c.addProperty("version", last.version.name().toLowerCase(Locale.ROOT));
			c.addProperty("own", last.own);
			c.addProperty("seed", last.seed);
			c.addProperty("startGameTime", last.start);
			c.addProperty("length", last.length);
			c.addProperty("sequence", last.seq.file);
			c.addProperty("placeholder", last.seq.placeholder);
			// From which tick its shapes are still the grey box's (step B2 builds it act by act).
			if (last.seq.placeholderFrom != Integer.MAX_VALUE) c.addProperty("placeholderFrom", last.seq.placeholderFrom);
			JsonArray shots = new JsonArray();
			for (var s : last.seq.shots) {
				JsonObject shot = new JsonObject();
				shot.addProperty("name", s.name());
				shot.addProperty("from", s.from());
				shot.addProperty("to", s.to());
				shots.add(shot);
			}
			c.add("shots", shots);
			o.add("cinematic", c);
		}
		return o;
	}

	private VfxRecorder() {}
}
