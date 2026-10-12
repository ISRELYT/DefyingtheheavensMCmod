package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Formation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A formation's barrier, struck (Formations#struck: a blow, a projectile, a cell broken), ripples, for everyone near, Qi
 * Sense or not. The unseen wall is cells of barrier blocks (what stops whoever can't pass), but what ripples is the true
 * sphere they stand for, as Qi Sense shows it (ClientFormations): for a moment the whole of it shows, faintly, and from the
 * blow bright rings run out over its surface and fade, in the barrier's own colour (a core's purple, a concealment's green).
 */
public final class BarrierRipples {
	/** How long a ripple lasts (ticks), how fast its rings run over the surface (blocks a tick), and how wide they are. */
	private static final float LIFE = 36;
	private static final float SPEED = 0.6f;
	private static final float RING_WIDTH = 1.4f;
	/** The ripple's patch of the sphere: rings from the blow out to its edge, and sectors round it. */
	private static final int RINGS = 30;
	private static final int SECTORS = 56;
	/** The faint whole sphere shown for a moment. */
	private static final int SHELL_RINGS = 24;
	private static final int SHELL_SECTORS = 48;
	private static final int MAX_RIPPLES = 24;
	private static final double DRAW_RANGE = 192;

	private record Ripple(Vec3 centre, float radius, float[][] shades, Vec3 way, float strength, float born) {}

	private static final List<Ripple> RIPPLES = new ArrayList<>();

	/** From the server: the barrier of the formation at {@code centre} was struck where {@code way} meets it. */
	public static void add(BlockPos centre, int radius, Formation.Kind kind, Vec3 way, float strength) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || way.lengthSqr() < 1.0e-6) return;
		if (RIPPLES.size() >= MAX_RIPPLES) RIPPLES.remove(0);
		RIPPLES.add(new Ripple(Vec3.atCenterOf(centre), radius + 0.5f, kind == Formation.Kind.CORE ? ClientFormations.CORE : ClientFormations.CONCEALMENT,
				way.normalize(), strength, mc.level.getGameTime() + mc.getFrameTime()));
	}

	public static void clear() {
		RIPPLES.clear();
	}

	/** WorldRenderEvents.AFTER_TRANSLUCENT: the ripples under way, added onto the world (light, tested against its depth). */
	static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || RIPPLES.isEmpty()) return;
		float time = mc.level.getGameTime() + context.tickDelta();
		RIPPLES.removeIf(ripple -> time - ripple.born() > LIFE || time < ripple.born() - 40); // done, or from before a time jump
		if (RIPPLES.isEmpty()) return;
		Vec3 camera = context.camera().getPosition();
		Matrix4f matrix = context.matrixStack().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Ripple ripple : RIPPLES) {
			double reach = DRAW_RANGE + ripple.radius();
			if (ripple.centre().distanceToSqr(camera) > reach * reach) continue;
			float age = Math.max(0, time - ripple.born());
			float fade = 1 - age / LIFE;
			Vec3 c = ripple.centre().subtract(camera);
			shell(buffer, matrix, c, ripple, 0.09f * ripple.strength() * fade * fade);
			patch(buffer, matrix, c, ripple, age, fade);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** The whole sphere, faint, brighter towards its edges as you see it (the way a soap bubble shows). */
	private static void shell(BufferBuilder buffer, Matrix4f matrix, Vec3 c, Ripple ripple, float alpha) {
		if (alpha <= 0.002f) return;
		float r = ripple.radius();
		for (int i = 0; i < SHELL_RINGS; i++) {
			float lat0 = -Mth.HALF_PI + Mth.PI * i / SHELL_RINGS, lat1 = -Mth.HALF_PI + Mth.PI * (i + 1) / SHELL_RINGS;
			for (int j = 0; j < SHELL_SECTORS; j++) {
				float lon0 = Mth.TWO_PI * j / SHELL_SECTORS, lon1 = Mth.TWO_PI * (j + 1) / SHELL_SECTORS;
				shellVertex(buffer, matrix, c, r, lat0, lon0, ripple.shades(), alpha);
				shellVertex(buffer, matrix, c, r, lat1, lon0, ripple.shades(), alpha);
				shellVertex(buffer, matrix, c, r, lat1, lon1, ripple.shades(), alpha);
				shellVertex(buffer, matrix, c, r, lat0, lon1, ripple.shades(), alpha);
			}
		}
	}

	private static void shellVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float lat, float lon, float[][] shades, float alpha) {
		float cosLat = Mth.cos(lat);
		float nx = cosLat * Mth.cos(lon), ny = Mth.sin(lat), nz = cosLat * Mth.sin(lon);
		float x = (float) (c.x + r * nx), y = (float) (c.y + r * ny), z = (float) (c.z + r * nz);
		float distance = Mth.sqrt(x * x + y * y + z * z);
		float facing = distance < 1.0e-4f ? 1 : Math.abs(nx * x + ny * y + nz * z) / distance;
		float rim = (1 - facing) * (1 - facing);
		buffer.vertex(matrix, x, y, z).color(shades[1][0], shades[1][1], shades[1][2], alpha * (0.3f + 0.7f * rim)).endVertex();
	}

	/**
	 * The patch of the sphere round the blow, in rings and sectors about it: two bright rings running out from it over the
	 * surface (the second a little behind and fainter), and a flash where it landed, all fading as the ripple ends.
	 */
	private static void patch(BufferBuilder buffer, Matrix4f matrix, Vec3 c, Ripple ripple, float age, float fade) {
		float r = ripple.radius();
		Vec3 n = ripple.way();
		// Two directions across the sphere at the blow.
		Vec3 helper = Math.abs(n.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
		Vec3 u = n.cross(helper).normalize(), w = n.cross(u);
		float reach = (SPEED * LIFE + 3 * RING_WIDTH) * Math.max(1, ripple.strength());
		float maxAngle = Math.min(Mth.PI, reach / r);
		for (int i = 0; i < RINGS; i++) {
			float a0 = maxAngle * i / RINGS, a1 = maxAngle * (i + 1) / RINGS;
			for (int j = 0; j < SECTORS; j++) {
				float b0 = Mth.TWO_PI * j / SECTORS, b1 = Mth.TWO_PI * (j + 1) / SECTORS;
				patchVertex(buffer, matrix, c, r, n, u, w, a0, b0, ripple, age, fade);
				patchVertex(buffer, matrix, c, r, n, u, w, a1, b0, ripple, age, fade);
				patchVertex(buffer, matrix, c, r, n, u, w, a1, b1, ripple, age, fade);
				patchVertex(buffer, matrix, c, r, n, u, w, a0, b1, ripple, age, fade);
			}
		}
	}

	private static void patchVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, Vec3 n, Vec3 u, Vec3 w, float angle, float around,
			Ripple ripple, float age, float fade) {
		float sin = Mth.sin(angle), cos = Mth.cos(angle);
		double dx = cos * n.x + sin * (Mth.cos(around) * u.x + Mth.sin(around) * w.x);
		double dy = cos * n.y + sin * (Mth.cos(around) * u.y + Mth.sin(around) * w.y);
		double dz = cos * n.z + sin * (Mth.cos(around) * u.z + Mth.sin(around) * w.z);
		float x = (float) (c.x + r * dx), y = (float) (c.y + r * dy), z = (float) (c.z + r * dz);
		float along = angle * r; // how far over the surface from the blow
		float front = SPEED * age * Math.max(1, ripple.strength());
		float lead = gauss((along - front) / RING_WIDTH);
		float trail = 0.55f * gauss((along - front * 0.62f) / (RING_WIDTH * 0.8f));
		float flash = 0.75f * gauss(along / 1.8f) * Math.max(0, 1 - age / 6);
		float glow = (lead + trail) * fade + flash;
		float alpha = Math.min(1, glow * 0.75f * Math.min(1.6f, ripple.strength()));
		if (alpha < 0.003f) alpha = 0;
		float[][] shades = ripple.shades();
		// The front runs whiter, the light behind it in the barrier's colour.
		float white = Math.min(1, 0.35f * lead + flash * 0.25f);
		buffer.vertex(matrix, x, y, z).color(Mth.lerp(white, shades[1][0], 1), Mth.lerp(white, shades[1][1], 1), Mth.lerp(white, shades[1][2], 1), alpha)
				.endVertex();
	}

	private static float gauss(float x) {
		return (float) Math.exp(-x * x);
	}

	private BarrierRipples() {}
}
