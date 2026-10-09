package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SpatialStorms;
import com.mojang.blaze3d.platform.GlStateManager;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws the Spatial Gap's physical storms ({@link SpatialStorms}): a blocky churning cloud mass ({@link StormCloudMesh}) in the
 * gap's violet, drawn {@link SpatialStorms#SCALE} times larger and fading into the dark toward the fog's end, and the
 * violet-white bolts they throw. Fed by the SPATIAL_STORMS packet (every storm in the gap at its true height, with the
 * player's loop count and height then, once a second; positions are carried forward between packets) and
 * SPATIAL_STORM_STRIKE (one bolt, already placed in this player's band).
 */
public final class ClientSpatialStorms {
	private static final int FLASH_TICKS = 8;
	/** A bolt lasts this long, flickering out twice, so it can be caught out of the corner of the eye. */
	private static final int BOLT_TICKS = 14;
	private static final int THUNDER_COOLDOWN = 40;
	/** Multiplies the cloud's dark grey: a deep violet. */
	private static final Vec3 TINT = new Vec3(0.85, 0.6, 1.3);
	/** The bolts' violet-white, lighting the cloud from within in a flash. */
	private static final float[] LIT = {0.55f, 0.38f, 0.95f};

	/** One storm as the server last described it. */
	public record StormState(int id, int seed, double x, double baseY, double z, float vx, float vz, float radius, int age, int life) {}

	private static final class Storm {
		int seed;
		double x, z, prevX, prevZ, baseY;
		float vx, vz, radius;
		int age, life, flash;
	}

	private static final class Bolt {
		final double x1, y1, z1, x2, y2, z2;
		final long seed;
		int age;

		Bolt(double x1, double y1, double z1, double x2, double y2, double z2, long seed) {
			this.x1 = x1;
			this.y1 = y1;
			this.z1 = z1;
			this.x2 = x2;
			this.y2 = y2;
			this.z2 = z2;
			this.seed = seed;
		}
	}

	private static final Map<Integer, Storm> STORMS = new HashMap<>();
	private static final List<Bolt> BOLTS = new ArrayList<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static int thunderCooldown;
	/** The player's loop count and height when the last SPATIAL_STORMS packet was sent. */
	private static int syncLoops;
	private static double syncY;

	/**
	 * From the SPATIAL_STORMS packet: the complete list, so storms missing from it are gone; their heights are true
	 * heights, placed in the band by {@link #loopsNow}.
	 */
	public static void update(int loops, double playerY, List<StormState> states) {
		syncLoops = loops;
		syncY = playerY;
		Set<Integer> seen = new HashSet<>();
		for (StormState state : states) {
			seen.add(state.id());
			Storm s = STORMS.get(state.id());
			if (s == null) {
				s = new Storm();
				s.prevX = state.x();
				s.prevZ = state.z();
				STORMS.put(state.id(), s);
			}
			s.seed = state.seed();
			s.x = state.x();
			s.z = state.z();
			s.baseY = state.baseY();
			s.vx = state.vx();
			s.vz = state.vz();
			s.radius = state.radius();
			s.age = state.age();
			s.life = state.life();
		}
		STORMS.keySet().retainAll(seen);
	}

	/** From the SPATIAL_STORM_STRIKE packet. */
	public static void strike(int stormId, double x1, double y1, double z1, double x2, double y2, double z2, long seed, boolean hit) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !ModDimensions.isSpatialGap(mc.level.dimension())) return;
		BOLTS.add(new Bolt(x1, y1, z1, x2, y2, z2, seed));
		Storm s = STORMS.get(stormId);
		if (s != null) s.flash = FLASH_TICKS;
		// A strike on someone always thunders, with the crack of the hit itself; other bolts rumble (out to ~64 blocks) at
		// most once every THUNDER_COOLDOWN ticks, so a few storms raging nearby don't become a wall of thunder.
		if (!hit && thunderCooldown > 0) return;
		thunderCooldown = THUNDER_COOLDOWN;
		mc.level.playLocalSound(x2, y2, z2, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 4.0f,
				0.6f + RANDOM.nextFloat() * 0.3f, false);
		if (hit) {
			mc.level.playLocalSound(x2, y2, z2, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0f,
					0.5f + RANDOM.nextFloat() * 0.2f, false);
		}
	}

	public static void tick(Minecraft mc) {
		if (STORMS.isEmpty() && BOLTS.isEmpty()) return;
		if (mc.level == null || !ModDimensions.isSpatialGap(mc.level.dimension())) {
			clear();
			return;
		}
		if (mc.isPaused()) return;
		for (Storm s : STORMS.values()) {
			s.prevX = s.x;
			s.prevZ = s.z;
			s.x += s.vx;
			s.z += s.vz;
			s.age++;
			if (s.flash > 0) s.flash--;
		}
		BOLTS.removeIf(b -> ++b.age > BOLT_TICKS);
		if (thunderCooldown > 0) thunderCooldown--;
	}

	public static void clear() {
		STORMS.clear();
		BOLTS.clear();
		thunderCooldown = 0;
		syncLoops = 0;
		syncY = 0;
	}

	// --- Drawing ---

	public static void render(WorldRenderContext context) {
		if (STORMS.isEmpty() && BOLTS.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !ModDimensions.isSpatialGap(mc.level.dimension())) return;
		float partial = context.tickDelta();
		float time = mc.level.getGameTime() + partial;
		Vec3 camera = context.camera().getPosition();
		PoseStack pose = context.matrixStack();
		float sightDistance = Math.max(64.0f, RenderSystem.getShaderFogEnd());
		int loops = loopsNow(mc);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true); // the depth-only pass must write depth
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		for (Storm s : STORMS.values()) {
			float form = form(s, partial);
			if (form <= 0) continue;
			form = form * form * (3 - 2 * form);
			// The cloud's shape, worked out at 1/SCALE size and drawn SCALE times larger.
			StormCloudMesh.Shape shape = StormCloudMesh.shape(s.seed, s.radius / SpatialStorms.SCALE * (0.35f + 0.65f * form), time);
			float flash = s.flash > 0 ? (s.flash - partial) / FLASH_TICKS : 0;
			double x = Mth.lerp(partial, s.prevX, s.x);
			double z = Mth.lerp(partial, s.prevZ, s.z);
			double y = s.baseY - (double) loops * SpatialStorms.PERIOD; // its true height, placed in the band
			// From the cloud's nearest edge, so a storm overhead doesn't fade just because its heart is far off.
			double outside = Math.max(0.0, Math.sqrt(Mth.square(x - camera.x) + Mth.square(z - camera.z)) - s.radius);
			double distance = Math.sqrt(Mth.square(outside) + Mth.square(y + SpatialStorms.CLOUD_TOP * 0.5 - camera.y));
			float sight = Mth.clamp((float) ((sightDistance - distance) / (sightDistance * 0.35)), 0.0f, 1.0f);
			if (sight <= 0) continue;
			pose.pushPose();
			pose.translate(x - camera.x, y - camera.y, z - camera.z);
			pose.scale(SpatialStorms.SCALE, SpatialStorms.SCALE, SpatialStorms.SCALE);
			Matrix4f matrix = pose.last().pose();
			RenderSystem.colorMask(false, false, false, false);
			StormCloudMesh.draw(matrix, shape, form * sight, flash, TINT, LIT);
			RenderSystem.colorMask(true, true, true, true);
			StormCloudMesh.draw(matrix, shape, form * sight, flash, TINT, LIT);
			pose.popPose();
		}

		if (!BOLTS.isEmpty()) {
			// Additive and not writing depth, but hidden inside the clouds they leave.
			RenderSystem.depthMask(false);
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			BufferBuilder buffer = Tesselator.getInstance().getBuilder();
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			Matrix4f matrix = pose.last().pose();
			for (Bolt b : BOLTS) {
				if (b.age == 4 || b.age == 9) continue; // flickers
				float alpha = 1.0f - Mth.clamp((b.age + partial) / (BOLT_TICKS + 1), 0.0f, 1.0f);
				bolt(buffer, matrix, camera, b, alpha);
			}
			BufferBuilder.RenderedBuffer rendered = buffer.endOrDiscardIfEmpty();
			if (rendered != null) BufferUploader.drawWithShader(rendered);
			RenderSystem.defaultBlendFunc();
			RenderSystem.depthMask(true);
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	/**
	 * The player's loop count now: the last packet's, plus one for each time the loop has moved the player since. A loop
	 * moves them a whole PERIOD while they travel well under half that between packets, so the difference in height
	 * since the packet, rounded to whole PERIODs, is exactly the loops missed. Nothing is counted on the client, so a
	 * loop arriving before or after a packet can't be counted twice.
	 */
	private static int loopsNow(Minecraft mc) {
		if (mc.player == null) return syncLoops;
		return syncLoops + (int) Math.round((syncY - mc.player.getY()) / SpatialStorms.PERIOD);
	}

	/** 0 while the storm gathers or after it has died away, 1 while it rages. */
	private static float form(Storm s, float partial) {
		float age = s.age + partial;
		return Mth.clamp(Math.min(age / SpatialStorms.FORM_TICKS, (s.life - age) / SpatialStorms.FADE_TICKS), 0.0f, 1.0f);
	}

	/**
	 * A jagged channel from the cloud to the end point, with the odd fork; the same shape every frame. A kink every ~7
	 * blocks (8-24 of them), each up to a third of a kink's length aside, so a long bolt stays jagged rather than smooth.
	 */
	private static void bolt(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, Bolt b, float alpha) {
		RandomSource random = RandomSource.create(b.seed);
		double length = Math.sqrt(Mth.square(b.x2 - b.x1) + Mth.square(b.y2 - b.y1) + Mth.square(b.z2 - b.z1));
		int steps = Mth.clamp((int) (length / 7.0), 8, 24);
		double kink = length / steps * 0.35;
		double px = b.x1, py = b.y1, pz = b.z1;
		for (int i = 1; i <= steps; i++) {
			double t = (double) i / steps;
			double jitter = i == steps ? 0.0 : kink;
			double nx = Mth.lerp(t, b.x1, b.x2) + (random.nextDouble() - 0.5) * 2 * jitter;
			double ny = Mth.lerp(t, b.y1, b.y2) + (random.nextDouble() - 0.5) * jitter;
			double nz = Mth.lerp(t, b.z1, b.z2) + (random.nextDouble() - 0.5) * 2 * jitter;
			segment(buffer, matrix, camera, px, py, pz, nx, ny, nz, 1.0f, alpha);
			if (i < steps - 1 && random.nextFloat() < 0.25f) {
				double fx = nx, fy = ny, fz = nz;
				double dx = (random.nextDouble() - 0.5) * 6.0 * kink, dz = (random.nextDouble() - 0.5) * 6.0 * kink;
				double dy = (b.y2 - b.y1) / steps * (0.6 + random.nextDouble() * 0.6);
				for (int j = 0; j < 3; j++) {
					double ex = fx + dx + (random.nextDouble() - 0.5) * 1.5;
					double ey = fy + dy;
					double ez = fz + dz + (random.nextDouble() - 0.5) * 1.5;
					segment(buffer, matrix, camera, fx, fy, fz, ex, ey, ez, 0.5f, alpha * 0.8f);
					fx = ex;
					fy = ey;
					fz = ez;
				}
			}
			px = nx;
			py = ny;
			pz = nz;
		}
	}

	/**
	 * A glow and a bright core, each a ribbon turned to face the camera. Beyond 40 blocks they widen with distance (up to
	 * 5x), so a bolt under a storm far off to the side still reads as a bolt rather than a sliver, while one striking
	 * close by stays thin.
	 */
	private static void segment(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, double x1, double y1, double z1,
			double x2, double y2, double z2, float width, float alpha) {
		double distance = Math.sqrt(Mth.square((x1 + x2) * 0.5 - camera.x) + Mth.square((y1 + y2) * 0.5 - camera.y)
				+ Mth.square((z1 + z2) * 0.5 - camera.z));
		// Wider far off so it still reads. Close by it is capped by how wide it looks (the glow at most ~5 degrees across),
		// so a bolt striking you, seen end-on, doesn't burst across the screen.
		float scaled = Math.min(width * Mth.clamp((float) (distance / 40.0), 1.0f, 5.0f), (float) (distance * 0.03));
		ribbon(buffer, matrix, camera, x1, y1, z1, x2, y2, z2, 3.0f * scaled, 0.55f, 0.35f, 1.0f, alpha * 0.45f);
		ribbon(buffer, matrix, camera, x1, y1, z1, x2, y2, z2, 0.6f * scaled, 0.95f, 0.92f, 1.0f, alpha);
	}

	/** Bolts stop this far short of the camera: a ribbon running into the eye would fill the screen. */
	private static final double NEAR_CLIP = 3.0;

	private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3 camera, double x1, double y1, double z1,
			double x2, double y2, double z2, float width, float r, float g, float b, float a) {
		Vec3 from = new Vec3(x1 - camera.x, y1 - camera.y, z1 - camera.z);
		Vec3 to = new Vec3(x2 - camera.x, y2 - camera.y, z2 - camera.z);
		boolean fromNear = from.length() < NEAR_CLIP, toNear = to.length() < NEAR_CLIP;
		if (fromNear && toNear) return;
		if (fromNear) from = clipToNear(to, from);
		if (toNear) to = clipToNear(from, to);
		Vec3 side = to.subtract(from).cross(from.add(to).scale(0.5));
		if (side.lengthSqr() < 1.0e-8) return;
		side = side.normalize().scale(width * 0.5);
		vertex(buffer, matrix, from.add(side), r, g, b, a);
		vertex(buffer, matrix, to.add(side), r, g, b, a);
		vertex(buffer, matrix, to.subtract(side), r, g, b, a);
		vertex(buffer, matrix, from.subtract(side), r, g, b, a);
	}

	/** The point on the way from {@code far} to {@code near} (camera-relative) where it comes within NEAR_CLIP of the eye. */
	private static Vec3 clipToNear(Vec3 far, Vec3 near) {
		double lo = 0.0, hi = 1.0;
		for (int i = 0; i < 12; i++) {
			double mid = (lo + hi) * 0.5;
			if (far.lerp(near, mid).length() < NEAR_CLIP) hi = mid;
			else lo = mid;
		}
		return far.lerp(near, lo);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 at, float r, float g, float b, float a) {
		buffer.vertex(matrix, (float) at.x, (float) at.y, (float) at.z).color(r, g, b, a).endVertex();
	}

	private ClientSpatialStorms() {}
}
