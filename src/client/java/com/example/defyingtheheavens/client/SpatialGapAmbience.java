package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Spatial Gap's ambience, for ascension and descension alike:
 * <ul>
 *   <li>glowing motes out to {@link #FAR_RADIUS} blocks, fixed in the world so they stream past as the gap carries the
 *   player (vanilla drops particles over 32 blocks from the camera, so these go straight to the particle engine);</li>
 *   <li>background spatial storms: {@link #BACKGROUND_STORMS} churning violet clouds fixed far off in the sky, flashing with
 *   lightning, drawn by {@link SpatialGapSkyRenderer}; now and then one's thunder rolls in from its direction a moment
 *   after the flash (the near, physical storms are {@link ClientSpatialStorms});</li>
 *   <li>the pressure: a warping drone, a howling void wind and a muffled crush, and a deep "vwooom" each time it hurts.</li>
 * </ul>
 * Client only and purely cosmetic; the server's trial decides everything that matters.
 * The look before this was added is kept in {@code backups/ascension-visuals-2026-10-09/} with revert steps.
 */
public final class SpatialGapAmbience {
	private static final float NEAR_MIN = 3.0f;
	private static final float NEAR_MAX = 24.0f;
	private static final float FAR_RADIUS = 80.0f;
	private static final int FAR_PER_TICK = 5;
	private static final int NEAR_PER_TICK = 2;
	/** Motes spawned at once on arrival, and after the gap's loop moves the player 192 blocks, so the void is never empty. */
	private static final int FAR_BURST = 325;
	private static final int NEAR_BURST = 75;
	private static final double JUMP_DISTANCE = 40.0;
	private static final int[] MOTE_COLORS = {0xC9A8FF, 0x8FE3FF, 0xF1EAFF, 0xA06BFF};
	private static final int MOTE_FADE = 0x2A1259;

	/** Storms sit just inside the star dome (radius 100), so they cover the stars behind them. */
	private static final float SKY_DISTANCE = 95.0f;
	private static final int BACKGROUND_STORMS = 8;
	private static final int FLASH_TICKS = 6;
	private static final int FADE_TICKS = 60;
	/** Only some flashes are heard, and never two rumbles within THUNDER_COOLDOWN ticks: the storms are far away. */
	private static final float THUNDER_CHANCE = 0.25f;
	private static final int THUNDER_COOLDOWN = 40;
	/** Thunder's delay after the flash: about the speed of sound (343 m/s) over the storm's notional distance. */
	private static final double SOUND_BLOCKS_PER_TICK = 17.0;

	private static final List<Storm> STORMS = new ArrayList<>();
	private static final List<Thunder> THUNDER = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static boolean inGap;
	private static Vec3 lastEye;
	private static int lastHurtTime;
	private static int thunderCooldown;
	private static PressureLoop warp;
	private static PressureLoop wind;
	private static PressureLoop crush;

	/** Called every client tick. */
	public static void tick(Minecraft mc) {
		if (mc.level == null || mc.player == null || !ModDimensions.isSpatialGap(mc.level.dimension())) {
			if (inGap) clear();
			return;
		}
		if (mc.isPaused()) return;
		try {
			Vec3 eye = mc.player.getEyePosition();
			boolean arrived = !inGap || lastEye == null || eye.distanceTo(lastEye) > JUMP_DISTANCE;
			if (!inGap) {
				// Fixed in the sky for the whole visit, already raging when the player arrives.
				for (int i = 0; i < BACKGROUND_STORMS; i++) STORMS.add(new Storm(pickDirection()));
			}
			inGap = true;
			lastEye = eye;
			spawnMotes(mc, eye, arrived);
			tickStorms(mc);
			tickPressure(mc);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Spatial Gap ambience failed", e);
		}
	}

	/** Called on leaving the gap and on disconnect. The pressure loops fade themselves out. */
	public static void clear() {
		inGap = false;
		lastEye = null;
		lastHurtTime = 0;
		thunderCooldown = 0;
		STORMS.clear();
		THUNDER.clear();
	}

	// ---- Motes ----

	private static void spawnMotes(Minecraft mc, Vec3 center, boolean refill) {
		float amount = switch (mc.options.particles().get()) {
			case ALL -> 1.0f;
			case DECREASED -> 0.5f;
			case MINIMAL -> 0.2f;
		};
		int far = Math.round((refill ? FAR_BURST : FAR_PER_TICK) * amount);
		int near = Math.round((refill ? NEAR_BURST : NEAR_PER_TICK) * amount);
		for (int i = 0; i < far; i++) spawnMote(mc, center, NEAR_MAX, FAR_RADIUS, 3.5f, 7.0f);
		for (int i = 0; i < near; i++) spawnMote(mc, center, NEAR_MIN, NEAR_MAX, 1.0f, 2.5f);
	}

	private static void spawnMote(Minecraft mc, Vec3 center, float minRadius, float maxRadius, float minScale, float maxScale) {
		Vector3f dir = randomDirection();
		// Uniform through the shell's volume, so the motes aren't crowded close in.
		float min3 = minRadius * minRadius * minRadius;
		float r = (float) Math.cbrt(min3 + RANDOM.nextFloat() * (maxRadius * maxRadius * maxRadius - min3));
		Particle p = mc.particleEngine.createParticle(ParticleTypes.END_ROD,
				center.x + dir.x * r, center.y + dir.y * r, center.z + dir.z * r,
				(RANDOM.nextFloat() - 0.5f) * 0.02f, (RANDOM.nextFloat() - 0.5f) * 0.02f, (RANDOM.nextFloat() - 0.5f) * 0.02f);
		if (p == null) return;
		p.scale(minScale + RANDOM.nextFloat() * (maxScale - minScale));
		p.setLifetime(60 + RANDOM.nextInt(60));
		if (p instanceof SimpleAnimatedParticle mote) {
			mote.setColor(MOTE_COLORS[RANDOM.nextInt(MOTE_COLORS.length)]);
			mote.setFadeColor(MOTE_FADE);
		}
	}

	private static Vector3f randomDirection() {
		Vector3f v = new Vector3f();
		do {
			v.set(RANDOM.nextFloat() * 2 - 1, RANDOM.nextFloat() * 2 - 1, RANDOM.nextFloat() * 2 - 1);
		} while (v.lengthSquared() < 0.01f || v.lengthSquared() > 1.0f);
		return v.normalize();
	}

	// ---- Storms ----

	/** A direction away from the storms already raging, so they don't pile up in one patch of sky. */
	private static Vector3f pickDirection() {
		Vector3f dir = randomDirection();
		for (int tries = 0; tries < 8 && tooClose(dir); tries++) dir = randomDirection();
		return dir;
	}

	private static boolean tooClose(Vector3f dir) {
		for (Storm s : STORMS) {
			if (s.dir.dot(dir) > 0.85f) return true;
		}
		return false;
	}

	private static void tickStorms(Minecraft mc) {
		for (Storm s : STORMS) {
			s.age++;
			if (s.flash > 0) s.flash--;
			if (--s.nextStrike <= 0) strike(s);
		}
		if (thunderCooldown > 0) thunderCooldown--;

		Iterator<Thunder> rumbles = THUNDER.iterator();
		while (rumbles.hasNext()) {
			Thunder t = rumbles.next();
			if (--t.delay > 0) continue;
			mc.getSoundManager().play(new DistantSound(SoundEvents.LIGHTNING_BOLT_THUNDER, t.dir, t.volume, t.pitch));
			rumbles.remove();
		}
	}

	private static void strike(Storm s) {
		s.nextStrike = 40 + RANDOM.nextInt(100);
		s.flash = FLASH_TICKS;
		// Most strikes show a bolt; the rest light the cloud from inside (sheet lightning).
		s.bolt = RANDOM.nextFloat() < 0.65f ? makeBolt() : new float[0];
		if (thunderCooldown > 0 || RANDOM.nextFloat() >= THUNDER_CHANCE) return;
		thunderCooldown = THUNDER_COOLDOWN;
		float far = (s.distance - 300.0f) / 600.0f; // 0 nearest .. 1 farthest
		THUNDER.add(new Thunder(new Vector3f(s.dir), (int) (s.distance / SOUND_BLOCKS_PER_TICK),
				0.3f - 0.15f * far, 0.85f - 0.3f * far + RANDOM.nextFloat() * 0.1f));
	}

	/** A jagged bolt out of the cloud's base with a fork or two, as segments (x1, y1, x2, y2, width) in cloud units. */
	private static float[] makeBolt() {
		FloatArrayList out = new FloatArrayList();
		float lean = (RANDOM.nextFloat() - 0.5f) * 1.2f;
		branch(out, (RANDOM.nextFloat() - 0.5f) * 1.2f, -0.3f, lean, 7 + RANDOM.nextInt(4), 1.0f, true);
		return out.toFloatArray();
	}

	private static void branch(FloatArrayList out, float x, float y, float angle, int steps, float width, boolean canFork) {
		for (int i = 0; i < steps; i++) {
			float a = angle + (RANDOM.nextFloat() - 0.5f) * 1.1f;
			float length = 0.22f + RANDOM.nextFloat() * 0.18f;
			float nx = x + Mth.sin(a) * length;
			float ny = y - Mth.cos(a) * length;
			out.add(x);
			out.add(y);
			out.add(nx);
			out.add(ny);
			out.add(width);
			if (canFork && i > 1 && RANDOM.nextFloat() < 0.25f) {
				branch(out, nx, ny, angle + (RANDOM.nextBoolean() ? 0.7f : -0.7f), 2 + RANDOM.nextInt(3), width * 0.55f, false);
			}
			x = nx;
			y = ny;
			width *= 0.93f;
		}
	}

	/** Brightness 0-1 of the strongest flash right now, so the whole sky can light up a little with it. */
	static float skyFlash(float partialTick) {
		float max = 0;
		for (Storm s : STORMS) max = Math.max(max, s.flash(partialTick) * s.fade(partialTick));
		return max;
	}

	/** Draws the storms into the sky. {@code pose} is the sky's pose (the camera's rotation only). */
	static void renderStorms(Matrix4f pose, float time, float partialTick) {
		if (STORMS.isEmpty()) return;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();

		// Behind: a faint violet glow that backlights each cloud, much brighter while it flashes.
		additive();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Storm s : STORMS) {
			Vector3f[] frame = frame(s.dir);
			float alpha = s.fade(partialTick) * (0.16f + 0.05f * Mth.sin(time * 0.05f + s.seed) + 0.5f * s.flash(partialTick));
			disc(buffer, pose, frame, 0, 0.1f * s.size, 2.4f * s.size, 1.5f * s.size, 0.45f, 0.25f, 0.85f, alpha);
		}
		draw(buffer);

		// The clouds: blocky masses slowly churning, dark against their glow, lit pale violet by a flash.
		RenderSystem.defaultBlendFunc();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Storm s : STORMS) {
			Vector3f[] frame = frame(s.dir);
			float fade = s.fade(partialTick);
			float flash = s.flash(partialTick);
			for (int i = 0; i < s.blobX.length; i++) {
				float phase = s.blobPhase[i];
				boolean back = s.blobBack[i];
				float cx = (s.blobX[i] + 0.06f * Mth.sin(time * 0.011f + phase)) * s.size;
				float cy = (s.blobY[i] + 0.05f * Mth.cos(time * 0.014f + phase)) * s.size;
				float half = s.blobSize[i] * (1.0f + 0.08f * Mth.sin(time * 0.02f + phase * 2)) * s.size;
				float lit = flash * (back ? 0.45f : 0.7f);
				rect(buffer, pose, frame, cx, cy, half, half * 0.8f,
						Mth.lerp(lit, back ? 0.10f : 0.035f, 0.62f),
						Mth.lerp(lit, back ? 0.05f : 0.018f, 0.52f),
						Mth.lerp(lit, back ? 0.21f : 0.08f, 0.95f),
						fade * (back ? 0.7f : 0.92f));
			}
		}
		draw(buffer);

		// In front: the flash glowing through the cloud, and the bolts (flickering off once mid-flash).
		additive();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Storm s : STORMS) {
			if (s.flash <= 0) continue;
			Vector3f[] frame = frame(s.dir);
			float fade = s.fade(partialTick);
			float flash = s.flash(partialTick);
			disc(buffer, pose, frame, 0, 0, 1.7f * s.size, 1.1f * s.size, 0.75f, 0.65f, 1.0f, flash * fade * 0.4f);
			if (s.flash == FLASH_TICKS / 2) continue;
			float alpha = fade * Math.min(1.0f, flash * 1.5f);
			for (int i = 0; i + 4 < s.bolt.length; i += 5) {
				float x1 = s.bolt[i] * s.size, y1 = s.bolt[i + 1] * s.size;
				float x2 = s.bolt[i + 2] * s.size, y2 = s.bolt[i + 3] * s.size;
				float width = s.bolt[i + 4];
				segment(buffer, pose, frame, x1, y1, x2, y2, 1.4f * width, 0.55f, 0.4f, 1.0f, alpha * 0.35f);
				segment(buffer, pose, frame, x1, y1, x2, y2, 0.35f * width, 0.92f, 0.95f, 1.0f, alpha);
			}
		}
		draw(buffer);

		RenderSystem.defaultBlendFunc();
		RenderSystem.enableCull();
	}

	private static void additive() {
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE,
				GlStateManager.DestFactor.ZERO);
	}

	private static void draw(BufferBuilder buffer) {
		BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
		if (rendered != null) BufferUploader.drawWithShader(rendered);
	}

	/** {center, right, up} of a storm's plane, facing the camera. */
	private static Vector3f[] frame(Vector3f dir) {
		Vector3f right = Math.abs(dir.y) > 0.97f ? new Vector3f(1, 0, 0) : new Vector3f(dir).cross(0, 1, 0).normalize();
		Vector3f up = new Vector3f(right).cross(dir).normalize();
		return new Vector3f[] {new Vector3f(dir).mul(SKY_DISTANCE), right, up};
	}

	private static void vertex(BufferBuilder buffer, Matrix4f pose, Vector3f[] frame, float x, float y,
			float r, float g, float b, float a) {
		Vector3f c = frame[0], right = frame[1], up = frame[2];
		buffer.vertex(pose, c.x + right.x * x + up.x * y, c.y + right.y * x + up.y * y, c.z + right.z * x + up.z * y)
				.color(r, g, b, a).endVertex();
	}

	private static void quad(BufferBuilder buffer, Matrix4f pose, Vector3f[] frame, float x0, float y0, float x1, float y1,
			float x2, float y2, float x3, float y3, float r, float g, float b, float a) {
		vertex(buffer, pose, frame, x0, y0, r, g, b, a);
		vertex(buffer, pose, frame, x1, y1, r, g, b, a);
		vertex(buffer, pose, frame, x2, y2, r, g, b, a);
		vertex(buffer, pose, frame, x0, y0, r, g, b, a);
		vertex(buffer, pose, frame, x2, y2, r, g, b, a);
		vertex(buffer, pose, frame, x3, y3, r, g, b, a);
	}

	private static void rect(BufferBuilder buffer, Matrix4f pose, Vector3f[] frame, float cx, float cy, float halfW, float halfH,
			float r, float g, float b, float a) {
		quad(buffer, pose, frame, cx - halfW, cy - halfH, cx + halfW, cy - halfH, cx + halfW, cy + halfH, cx - halfW, cy + halfH,
				r, g, b, a);
	}

	private static void segment(BufferBuilder buffer, Matrix4f pose, Vector3f[] frame, float x1, float y1, float x2, float y2,
			float width, float r, float g, float b, float a) {
		float dx = x2 - x1, dy = y2 - y1;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if (length < 1.0e-4f) return;
		float nx = -dy / length * width * 0.5f, ny = dx / length * width * 0.5f;
		quad(buffer, pose, frame, x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, r, g, b, a);
	}

	/** A soft ellipse, full {@code a} at its center fading to nothing at its rim. */
	private static void disc(BufferBuilder buffer, Matrix4f pose, Vector3f[] frame, float cx, float cy, float rx, float ry,
			float r, float g, float b, float a) {
		if (a <= 0.005f) return;
		int segments = 24;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments;
			float a1 = Mth.TWO_PI * (i + 1) / segments;
			vertex(buffer, pose, frame, cx, cy, r, g, b, a);
			vertex(buffer, pose, frame, cx + Mth.cos(a0) * rx, cy + Mth.sin(a0) * ry, r, g, b, 0.0f);
			vertex(buffer, pose, frame, cx + Mth.cos(a1) * rx, cy + Mth.sin(a1) * ry, r, g, b, 0.0f);
		}
	}

	/** One distant storm cell, fixed in the sky. Blob and bolt coordinates are in units of {@link #size}. */
	private static final class Storm {
		final Vector3f dir;
		/** Notional distance in blocks: sets the cloud's size and its thunder's delay and loudness. */
		final float distance = 300.0f + RANDOM.nextFloat() * 600.0f;
		/** Half the cloud's height, in sky units. */
		final float size = 3.0f + 2400.0f / distance;
		final float seed = RANDOM.nextFloat() * 100.0f;
		final float[] blobX = new float[16];
		final float[] blobY = new float[16];
		final float[] blobSize = new float[16];
		final float[] blobPhase = new float[16];
		final boolean[] blobBack = new boolean[16];
		/** Starts fully formed: the storms are already raging when the player arrives. */
		int age = FADE_TICKS;
		int nextStrike = 10 + RANDOM.nextInt(120);
		int flash;
		float[] bolt = new float[0];

		Storm(Vector3f dir) {
			this.dir = dir;
			for (int i = 0; i < blobX.length; i++) {
				boolean back = i < 6; // drawn first, wider and paler, so the dark front masses stand out against them
				blobBack[i] = back;
				blobX[i] = (RANDOM.nextFloat() * 2 - 1) * (back ? 1.6f : 1.3f);
				blobY[i] = (RANDOM.nextFloat() * 2 - 1) * (back ? 0.55f : 0.45f) + 0.1f;
				blobSize[i] = (back ? 0.55f : 0.35f) + RANDOM.nextFloat() * 0.3f;
				blobPhase[i] = RANDOM.nextFloat() * Mth.TWO_PI;
			}
		}

		float fade(float partialTick) {
			return Mth.clamp((age + partialTick) / FADE_TICKS, 0.0f, 1.0f);
		}

		float flash(float partialTick) {
			return flash <= 0 ? 0.0f : Mth.clamp((flash - partialTick) / FLASH_TICKS, 0.0f, 1.0f);
		}
	}

	private static final class Thunder {
		final Vector3f dir;
		final float volume;
		final float pitch;
		int delay;

		Thunder(Vector3f dir, int delay, float volume, float pitch) {
			this.dir = dir;
			this.delay = delay;
			this.volume = volume;
			this.pitch = pitch;
		}
	}

	// ---- Sounds ----

	/**
	 * Keeps the pressure loops going. The first version (a 0.6 underwater drone and a 0.5 beacon hum) went unheard under
	 * the thunder, so these are louder, higher and more distinct.
	 */
	private static void tickPressure(Minecraft mc) {
		warp = keepPlaying(mc, warp, SoundEvents.PORTAL_AMBIENT, 0.7f, 0.8f);
		wind = keepPlaying(mc, wind, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_LOOP.value(), 0.75f, 1.0f);
		crush = keepPlaying(mc, crush, SoundEvents.AMBIENT_UNDERWATER_LOOP, 0.85f, 1.0f);
		// hurtTime jumps up when the player is hit; in the gap that is the pressure, once a second.
		int hurtTime = mc.player.hurtTime;
		if (hurtTime > lastHurtTime) {
			// A deep descending "vwooom" as the pressure bears down.
			mc.getSoundManager().play(new SimpleSoundInstance(SoundEvents.BEACON_DEACTIVATE.getLocation(), SoundSource.PLAYERS,
					0.9f, 0.85f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
		}
		lastHurtTime = hurtTime;
	}

	private static PressureLoop keepPlaying(Minecraft mc, PressureLoop loop, SoundEvent sound, float pitch, float maxVolume) {
		if (loop != null && mc.getSoundManager().isActive(loop)) return loop;
		PressureLoop fresh = new PressureLoop(sound, pitch, maxVolume);
		mc.getSoundManager().play(fresh);
		return fresh;
	}

	/** A loop at the listener that swells in while in the gap and fades out after leaving it. */
	private static final class PressureLoop extends AbstractTickableSoundInstance {
		private final float maxVolume;

		PressureLoop(SoundEvent sound, float pitch, float maxVolume) {
			super(sound, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
			this.maxVolume = maxVolume;
			this.looping = true;
			this.delay = 0;
			this.volume = 0.0f;
			this.pitch = pitch;
			this.relative = true;
			this.attenuation = SoundInstance.Attenuation.NONE;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}

		@Override
		public void tick() {
			volume = inGap ? Math.min(maxVolume, volume + maxVolume / 40.0f) : volume - maxVolume / 30.0f;
			if (volume <= 0.0f) {
				volume = 0.0f;
				stop();
			}
		}
	}

	/** A sound that stays a fixed way off in one direction, however fast the gap carries the listener. */
	private static final class DistantSound extends AbstractTickableSoundInstance {
		private final Vector3f dir;

		DistantSound(SoundEvent sound, Vector3f dir, float volume, float pitch) {
			super(sound, SoundSource.WEATHER, SoundInstance.createUnseededRandom());
			this.dir = dir;
			this.volume = volume;
			this.pitch = pitch;
			this.attenuation = SoundInstance.Attenuation.NONE;
			follow();
		}

		private void follow() {
			Vec3 at = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
			x = at.x + dir.x * 16.0;
			y = at.y + dir.y * 16.0;
			z = at.z + dir.z * 16.0;
		}

		@Override
		public void tick() {
			if (!inGap) {
				stop();
				return;
			}
			follow();
		}
	}

	private SpatialGapAmbience() {}
}
