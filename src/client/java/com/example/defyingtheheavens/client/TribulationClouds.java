package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Realm;
import com.example.defyingtheheavens.Stage;
import com.example.defyingtheheavens.TribulationManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Heavenly Tribulation clouds: a dark, churning mass of blocky cloud that gathers in the sky over a cultivator undergoing a
 * tribulation, wider the higher the realm and stage being broken into ({@link TribulationManager#cloudRadius}). Its core is
 * darkest and thickest and dips down like a funnel, the pattern swirls (faster toward the middle), and it flashes when a
 * strike falls. The cloud drifts after the cultivator and rises if they fly up toward it, but never sinks after them; when
 * the tribulation ends, or the cultivator falls out of the realm into the void, it stays where it is and fades away.
 * <p>
 * Fed by the TRIBULATION_CLOUD packet, which reaches the cultivator and every player tracking them, so everyone nearby
 * sees it. Drawn like vanilla clouds: a depth-only pass, then a colour pass, so it is translucent without seeing its own
 * inner faces.
 */
public final class TribulationClouds {
	/** The cloud gathers over the 3 s before the first strike. */
	private static final float FORM_TICKS = 60;
	private static final float FADE_TICKS = 40;
	/** The server repeats the cloud's state every second; this long without word means the tribulation is over (a disconnect). */
	private static final int TIMEOUT_TICKS = 60;
	/** Fraction of the way to the cultivator the cloud drifts each tick. */
	private static final double FOLLOW = 0.08;
	/** Never closer than this above the cultivator. */
	private static final double MIN_ABOVE = 16;
	/** Cloud cells: horizontal size and layer height in blocks. */
	private static final float CELL = 6;
	private static final float LAYER = 3;
	/** Layers below the cloud's base (the funnel under the core) and above it. */
	private static final int LOW_LAYER = -1;
	private static final int HIGH_LAYER = 3;
	private static final int FLASH_TICKS = 8;
	/** Radians per tick the pattern turns at the rim; the core turns 2.5x as fast. */
	private static final float SWIRL = 0.004f;

	private static final class Cloud {
		final ResourceKey<Level> dimension;
		final int seed;
		float radius;
		double x, y, z, prevX, prevY, prevZ;
		/** The base never sinks below where it formed. */
		final double floorY;
		boolean active = true;
		int silentTicks;
		float strength, prevStrength;
		int strikesLeft;
		int flash;

		Cloud(ResourceKey<Level> dimension, UUID id, Player cultivator, float radius) {
			this.dimension = dimension;
			this.seed = id.hashCode();
			this.radius = radius;
			this.x = prevX = cultivator.getX();
			this.z = prevZ = cultivator.getZ();
			this.floorY = cultivator.getY() + 24 + radius * 0.15;
			this.y = prevY = floorY;
		}
	}

	private static final Map<UUID, Cloud> CLOUDS = new HashMap<>();

	/** From the TRIBULATION_CLOUD packet. */
	public static void update(UUID id, boolean active, int strikesLeft, int realmIndex, int stageIndex) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Cloud cloud = CLOUDS.get(id);
		if (!active) {
			if (cloud != null) cloud.active = false; // stops where it is and fades
			return;
		}
		float radius = TribulationManager.cloudRadius(Realm.byIndex(realmIndex), Stage.byIndex(stageIndex));
		if (cloud == null || !cloud.dimension.equals(mc.level.dimension())) {
			Player cultivator = mc.level.getPlayerByUUID(id);
			if (cultivator == null) return; // not loaded here yet; the next heartbeat tries again
			cloud = new Cloud(mc.level.dimension(), id, cultivator, radius);
			cloud.strikesLeft = strikesLeft;
			CLOUDS.put(id, cloud);
		}
		cloud.active = true; // also revives a fading cloud when the next tribulation follows straight on
		cloud.silentTicks = 0;
		cloud.radius = radius;
		if (strikesLeft < cloud.strikesLeft) cloud.flash = FLASH_TICKS;
		cloud.strikesLeft = strikesLeft;
	}

	public static void tick(Minecraft mc) {
		if (CLOUDS.isEmpty()) return;
		if (mc.level == null) {
			CLOUDS.clear();
			return;
		}
		if (mc.isPaused()) return;
		for (Iterator<Map.Entry<UUID, Cloud>> it = CLOUDS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Cloud> entry = it.next();
			Cloud cloud = entry.getValue();
			// A cloud belongs to the sky it formed in: gone once the viewer is in another dimension.
			if (!cloud.dimension.equals(mc.level.dimension())) {
				it.remove();
				continue;
			}
			cloud.prevX = cloud.x;
			cloud.prevY = cloud.y;
			cloud.prevZ = cloud.z;
			cloud.prevStrength = cloud.strength;
			if (cloud.active && ++cloud.silentTicks > TIMEOUT_TICKS) cloud.active = false;

			Player cultivator = mc.level.getPlayerByUUID(entry.getKey());
			if (cloud.active && cultivator != null) {
				cloud.x += (cultivator.getX() - cloud.x) * FOLLOW;
				cloud.z += (cultivator.getZ() - cloud.z) * FOLLOW;
				cloud.y += (Math.max(cloud.floorY, cultivator.getY() + MIN_ABOVE) - cloud.y) * FOLLOW;
			}
			cloud.strength = cloud.active
					? Math.min(1, cloud.strength + 1 / FORM_TICKS)
					: Math.max(0, cloud.strength - 1 / FADE_TICKS);
			if (cloud.flash > 0) cloud.flash--;
			if (!cloud.active && cloud.strength <= 0 && cloud.prevStrength <= 0) it.remove();
		}
	}

	public static void clear() {
		CLOUDS.clear();
	}

	// --- Drawing ---

	/** One frame's cloud shape: per column, the lowest and highest layer, or no cloud. */
	private static final class Shape {
		final int n;
		final int[] low;
		final int[] high;
		final float[] core; // 1 at the centre, 0 at the rim
		final float[] mottle; // per-column light/dark variation, -1..1

		Shape(int n) {
			this.n = n;
			int size = (2 * n + 1) * (2 * n + 1);
			low = new int[size];
			high = new int[size];
			core = new float[size];
			mottle = new float[size];
		}

		int index(int i, int k) { return (i + n) * (2 * n + 1) + (k + n); }

		boolean filled(int i, int k, int layer) {
			if (i < -n || i > n || k < -n || k > n) return false;
			int idx = index(i, k);
			return high[idx] >= low[idx] && layer >= low[idx] && layer <= high[idx];
		}
	}

	public static void render(WorldRenderContext context) {
		if (CLOUDS.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		float partial = context.tickDelta();
		float time = mc.level.getGameTime() + partial;
		Vec3 camera = context.camera().getPosition();
		Vec3 daylight = mc.level.getCloudColor(partial); // darkens at night like vanilla clouds
		PoseStack pose = context.matrixStack();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true); // the depth-only pass must write depth, whatever state the last renderer left
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		for (Cloud cloud : CLOUDS.values()) {
			float strength = Mth.lerp(partial, cloud.prevStrength, cloud.strength);
			if (strength <= 0 || !cloud.dimension.equals(mc.level.dimension())) continue;
			strength = strength * strength * (3 - 2 * strength);
			Shape shape = shape(cloud, cloud.radius * (0.35f + 0.65f * strength), time);
			float flash = cloud.flash > 0 ? (cloud.flash - partial) / FLASH_TICKS : 0;

			pose.pushPose();
			pose.translate(Mth.lerp(partial, cloud.prevX, cloud.x) - camera.x, Mth.lerp(partial, cloud.prevY, cloud.y) - camera.y,
					Mth.lerp(partial, cloud.prevZ, cloud.z) - camera.z);
			Matrix4f matrix = pose.last().pose();
			RenderSystem.colorMask(false, false, false, false);
			draw(matrix, shape, strength, flash, daylight);
			RenderSystem.colorMask(true, true, true, true);
			draw(matrix, shape, strength, flash, daylight);
			pose.popPose();
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	/** Which cells are cloud this frame: a swirling noise pattern, always filled near the core and ragged at the rim. */
	private static Shape shape(Cloud cloud, float radius, float time) {
		int n = Mth.ceil(radius / CELL);
		Shape shape = new Shape(n);
		for (int i = -n; i <= n; i++) {
			for (int k = -n; k <= n; k++) {
				int idx = shape.index(i, k);
				shape.high[idx] = Integer.MIN_VALUE;
				float cx = i * CELL;
				float cz = k * CELL;
				float r = Mth.sqrt(cx * cx + cz * cz);
				if (r > radius) continue;
				float core = 1 - r / radius;
				// Sample the noise in a frame that turns with time, faster toward the core: the pattern swirls while the
				// cells stay square to the world like vanilla clouds.
				float angle = time * SWIRL * (1 + 1.5f * core);
				float sin = Mth.sin(angle);
				float cos = Mth.cos(angle);
				float sx = (cx * cos - cz * sin) / CELL;
				float sz = (cx * sin + cz * cos) / CELL;
				float density = fbm(sx * 0.35f, sz * 0.35f, cloud.seed);
				if (core < 0.75f && density < 0.62f - 0.7f * core) continue;
				shape.core[idx] = core;
				// The underside hangs in lumps, more and deeper toward the funnel at the core.
				float lump = fbm(sx * 0.5f - 7.9f, sz * 0.5f + 23.1f, cloud.seed);
				int low = core > 0.82f ? LOW_LAYER : core > 0.5f ? 0 : 1;
				if (core > 0.15f && lump > 0.58f) low = Math.min(low, 0);
				if (core > 0.4f && lump > 0.72f) low = LOW_LAYER;
				shape.low[idx] = low;
				int high = 1 + (core > 0.25f ? 1 : 0) + (fbm(sx * 0.6f + 31.7f, sz * 0.6f - 12.3f, cloud.seed) > 0.62f ? 1 : 0);
				shape.high[idx] = Math.min(HIGH_LAYER, high);
				shape.mottle[idx] = noise(sx * 0.9f + 50.3f, sz * 0.9f - 3.7f, cloud.seed + 7) * 2 - 1;
			}
		}
		return shape;
	}

	/** Every visible face of the cloud's cells: dark grey, darker toward the core, bottoms darkest, lit blue in a flash. */
	private static void draw(Matrix4f matrix, Shape shape, float strength, float flash, Vec3 daylight) {
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int n = shape.n;
		float half = CELL / 2;
		for (int i = -n; i <= n; i++) {
			for (int k = -n; k <= n; k++) {
				int idx = shape.index(i, k);
				if (shape.high[idx] < shape.low[idx]) continue;
				float core = shape.core[idx];
				// Nearly opaque, so the sky behind doesn't tint it blue: dark grey at the rim to near-black at the core.
				float grey = Mth.lerp(core, 0.22f, 0.055f) * (1 + 0.18f * shape.mottle[idx]);
				float alpha = (0.88f + 0.1f * core) * strength;
				float lit = flash * (0.45f + 0.55f * core);
				float x0 = i * CELL - half, x1 = x0 + CELL;
				float z0 = k * CELL - half, z1 = z0 + CELL;
				for (int layer = shape.low[idx]; layer <= shape.high[idx]; layer++) {
					float y0 = layer * LAYER, y1 = y0 + LAYER;
					if (!shape.filled(i, k, layer + 1)) {
						quad(buffer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, grey * 1.15f, lit * 0.3f, alpha, daylight);
					}
					if (!shape.filled(i, k, layer - 1)) {
						quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, grey * 0.7f, lit, alpha, daylight);
					}
					if (!shape.filled(i - 1, k, layer)) {
						quad(buffer, matrix, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, grey * 0.9f, lit * 0.7f, alpha, daylight);
					}
					if (!shape.filled(i + 1, k, layer)) {
						quad(buffer, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, grey * 0.9f, lit * 0.7f, alpha, daylight);
					}
					if (!shape.filled(i, k - 1, layer)) {
						quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, grey * 0.82f, lit * 0.7f, alpha, daylight);
					}
					if (!shape.filled(i, k + 1, layer)) {
						quad(buffer, matrix, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, grey * 0.82f, lit * 0.7f, alpha, daylight);
					}
				}
			}
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	private static void quad(BufferBuilder buffer, Matrix4f matrix, float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz, float grey, float lit, float alpha, Vec3 daylight) {
		// Daylight dims the grey; the lightning's blue lights the cloud from within whatever the time of day.
		float r = grey * (float) daylight.x + lit * 0.35f;
		float g = grey * (float) daylight.y + lit * 0.45f;
		float b = grey * (float) daylight.z + lit * 0.75f;
		buffer.vertex(matrix, ax, ay, az).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, bx, by, bz).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, cx, cy, cz).color(r, g, b, alpha).endVertex();
		buffer.vertex(matrix, dx, dy, dz).color(r, g, b, alpha).endVertex();
	}

	// --- Noise ---

	/** Two octaves of value noise, 0..1. */
	private static float fbm(float x, float z, int seed) {
		return 0.65f * noise(x, z, seed) + 0.35f * noise(x * 2.1f + 5.2f, z * 2.1f + 1.3f, seed + 101);
	}

	private static float noise(float x, float z, int seed) {
		int x0 = Mth.floor(x);
		int z0 = Mth.floor(z);
		float fx = x - x0;
		float fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		float top = Mth.lerp(fx, hash(x0, z0, seed), hash(x0 + 1, z0, seed));
		float bottom = Mth.lerp(fx, hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed));
		return Mth.lerp(fz, top, bottom);
	}

	private static float hash(int x, int z, int seed) {
		int h = x * 374761393 + z * 668265263 + seed * 1442695041;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private TribulationClouds() {}
}
