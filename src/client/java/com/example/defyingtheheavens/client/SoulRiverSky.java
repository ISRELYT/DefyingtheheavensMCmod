package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.JudgeEye;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The sky over the Soul River: not an empty black but a deep indigo void, a sea of pale mist glowing far below, drifting
 * clouds of violet and teal nebula, thick with stars, and other rivers of souls arcing across it far away, their light pulsing
 * as the dead flow down them. And the Eye of the Chief Judge of the Netherworld (JudgeEye): vast, open in the void square to the
 * river's flow, so a traveller must turn to see it. Its white is pale as bone and veined red; it follows the souls drifting down
 * the river, now and then turns to look straight at the traveller (its slit pupil widening), and blinks. Met, it looks back,
 * and its iris bleeds to crimson as its judgement falls (ClientJudgeGaze).
 */
final class SoulRiverSky {
	private static final float RADIUS = 100.0f;
	private static final float[] ZENITH = {0.010f, 0.008f, 0.040f};
	private static final float[] HORIZON = {0.050f, 0.040f, 0.120f};
	/** Below: the far-off sea of mist, faintly lit. */
	private static final float[] NADIR = {0.030f, 0.085f, 0.110f};

	private static final int NEBULAE = 22;
	/** Each cloud: azimuth, elevation, size, red, green, blue, alpha. */
	private static final float[][] NEBULA = new float[NEBULAE][7];
	/** The far rivers: each a great circle (its pole's azimuth and tilt), how fast its pulses run, and its colour mix. */
	private static final float[][] FAR_RIVERS = {{0.4f, 1.15f, 1.0f, 0.0f}, {2.3f, 0.75f, -0.7f, 0.6f}, {4.1f, 1.35f, 0.8f, 0.3f}};
	private static final int FAR_STEPS = 96;

	/** The Eye's size on the sky (half its width and half its height at full open), and its iris. */
	private static final float EYE_W = 40.0f, EYE_H = 15.0f, IRIS = 11.5f;
	private static final int EYE_COLUMNS = 56;

	static {
		RandomSource random = RandomSource.create(9134L);
		for (int i = 0; i < NEBULAE; i++) {
			float[] n = NEBULA[i];
			n[0] = random.nextFloat() * Mth.TWO_PI;
			n[1] = -0.35f + random.nextFloat() * 1.3f;
			n[2] = 18 + random.nextFloat() * 40;
			int kind = random.nextInt(3);
			n[3] = kind == 0 ? 0.45f : kind == 1 ? 0.10f : 0.55f;
			n[4] = kind == 0 ? 0.20f : kind == 1 ? 0.55f : 0.18f;
			n[5] = kind == 0 ? 0.85f : kind == 1 ? 0.65f : 0.45f;
			n[6] = 0.05f + random.nextFloat() * 0.08f;
		}
	}

	private SoulRiverSky() {}

	static void render(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null) return;
		try {
			PoseStack pose = context.matrixStack();
			float time = level.getGameTime() + context.tickDelta();
			RenderSystem.depthMask(false);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			SkyDome.drawGradient(pose.last().pose(), ZENITH, HORIZON, NADIR, 60.0f);
			for (float[] n : NEBULA) {
				pose.pushPose();
				pose.mulPose(Axis.YP.rotation(n[0] + time * 0.00004f));
				pose.mulPose(Axis.XP.rotation(Mth.HALF_PI - n[1]));
				float breathe = 0.85f + 0.15f * Mth.sin(time * 0.004f + n[0] * 3);
				SkyDome.drawHalo(pose.last().pose(), n[2], n[3], n[4], n[5], n[6] * breathe);
				SkyDome.drawHalo(pose.last().pose(), n[2] * 0.45f, n[3] + 0.2f, n[4] + 0.2f, n[5] + 0.1f, n[6] * 0.6f * breathe);
				pose.popPose();
			}
			pose.pushPose();
			pose.mulPose(Axis.YP.rotationDegrees(time * 0.003f));
			SkyDome.drawStars(pose.last().pose(), 0.9f, 0.78f, 0.88f, 1.0f, time);
			pose.popPose();
			farRivers(pose.last().pose(), time);
			eye(pose.last().pose(), time, ClientSoulRiver.flow());
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Soul River sky rendering failed", e);
		} finally {
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.depthMask(true);
		}
	}

	// --- Far rivers ---

	/** Other rivers of souls, far off: faint bands of light along great circles of the sky, bright pulses running along them. */
	private static void farRivers(Matrix4f m, float time) {
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		additive();
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int k = 0; k < FAR_RIVERS.length; k++) {
			float[] river = FAR_RIVERS[k];
			Vector3f pole = new Vector3f(Mth.cos(river[0]) * Mth.sin(river[1]), Mth.cos(river[1]), Mth.sin(river[0]) * Mth.sin(river[1]));
			Vector3f a = new Vector3f(pole).cross(0, 1, 0);
			if (a.lengthSquared() < 1.0e-4f) a.set(1, 0, 0);
			a.normalize();
			Vector3f c = new Vector3f(pole).cross(a).normalize();
			float red = Mth.lerp(river[3], 0.30f, 0.60f), green = Mth.lerp(river[3], 0.85f, 0.45f), blue = 1.0f;
			for (int band = 0; band < 2; band++) {
				float width = band == 0 ? 0.085f : 0.022f;
				float strength = band == 0 ? 0.35f : 1.0f;
				for (int i = 0; i < FAR_STEPS; i++) {
					float p0 = Mth.TWO_PI * i / FAR_STEPS, p1 = Mth.TWO_PI * (i + 1) / FAR_STEPS;
					float a0 = strength * farGlow(p0, time, river[2], k), a1 = strength * farGlow(p1, time, river[2], k);
					Vector3f c0 = onCircle(a, c, p0), c1 = onCircle(a, c, p1);
					for (int side = -1; side <= 1; side += 2) {
						vertex(b, m, edge(c0, pole, side * width), red, green, blue, 0);
						vertex(b, m, edge(c1, pole, side * width), red, green, blue, 0);
						vertex(b, m, new Vector3f(c1).mul(RADIUS), red, green, blue, a1);
						vertex(b, m, new Vector3f(c0).mul(RADIUS), red, green, blue, a0);
					}
				}
			}
		}
		BufferUploader.drawWithShader(b.end());
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
	}

	private static float farGlow(float phi, float time, float speed, int k) {
		float pulse = 0.5f + 0.5f * Mth.sin(phi * 11 - time * 0.015f * speed + k * 2.1f);
		return 0.035f + 0.12f * pulse * pulse * pulse;
	}

	private static Vector3f onCircle(Vector3f a, Vector3f c, float phi) {
		return new Vector3f(a).mul(Mth.cos(phi)).add(new Vector3f(c).mul(Mth.sin(phi)));
	}

	private static Vector3f edge(Vector3f onCircle, Vector3f pole, float offset) {
		return new Vector3f(onCircle).add(new Vector3f(pole).mul(offset)).normalize().mul(RADIUS);
	}

	// --- The Eye ---

	/** Where the Eye is drawn, and its plane: a point on the sky, its right and its up as the traveller sees them. */
	private record Plane(Vector3f centre, Vector3f right, Vector3f up) {
		Vector3f at(float x, float y) {
			return new Vector3f(centre).add(new Vector3f(right).mul(x)).add(new Vector3f(up).mul(y));
		}
	}

	private static void eye(Matrix4f m, float time, int flow) {
		// Square to the river's flow, off to one side (JudgeEye): a traveller has to turn to see it.
		Vector3f dir = new Vector3f((float) JudgeEye.DIRECTION.x, (float) JudgeEye.DIRECTION.y, (float) JudgeEye.DIRECTION.z).normalize();
		Vector3f right = new Vector3f(dir).cross(0, 1, 0).normalize();
		Vector3f up = new Vector3f(right).cross(dir).normalize();
		Plane plane = new Plane(new Vector3f(dir).mul(RADIUS * 0.95f), right, up);

		float open = JudgeEye.openness(time);
		// Met, it looks back, and its iris burns crimson as its judgement falls.
		float judging = ClientJudgeGaze.strength();
		float attention = Math.max(JudgeEye.attention(time), Mth.clamp(judging * 3, 0, 1));
		// It watches the souls go by below it, its gaze drifting along the river; or, when it attends, the traveller.
		Vector3f along = new Vector3f(flow, 0, 0);
		float fx = along.dot(right), fy = along.dot(up);
		float lengthF = Math.max(1.0e-3f, Mth.sqrt(fx * fx + fy * fy));
		float sweep = Mth.sin(time * 0.0065f) + 0.25f * Mth.sin(time * 0.031f);
		float gazeX = Mth.lerp(attention, fx / lengthF * 10.0f * sweep, 0);
		float gazeY = Mth.lerp(attention, -4.2f + fy / lengthF * 3.0f * sweep, 0);
		float pupil = Mth.lerp(attention, 0.16f, 0.42f) * IRIS * (0.95f + 0.05f * Mth.sin(time * 0.05f));

		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.disableCull();
		BufferBuilder b = Tesselator.getInstance().getBuilder();

		// Its presence: a wide, faint violet aura, and a teal one within it.
		additive();
		b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		ellipse(b, m, plane, 0, 0, EYE_W * 1.9f, EYE_H * 5.0f, 0.32f, 0.12f, 0.55f, 0.13f * (0.8f + 0.2f * attention), 0);
		ellipse(b, m, plane, 0, 0, EYE_W * 1.25f, EYE_H * 2.6f, 0.15f, 0.55f, 0.65f, 0.07f + 0.05f * attention, 0);
		BufferUploader.drawWithShader(b.end());

		// The white of the eye, pale as bone, shadowed toward the lids.
		RenderSystem.defaultBlendFunc();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < EYE_COLUMNS; i++) {
			float x0 = Mth.lerp(i / (float) EYE_COLUMNS, -EYE_W, EYE_W), x1 = Mth.lerp((i + 1) / (float) EYE_COLUMNS, -EYE_W, EYE_W);
			float corner0 = Math.abs(x0) / EYE_W, corner1 = Math.abs(x1) / EYE_W;
			float shade = 1 - 0.3f * Math.max(corner0, corner1) * Math.max(corner0, corner1);
			quad(b, m, plane, x0, lower(x0, open), x1, lower(x1, open), x1, 0, x0, 0, 0.58f * shade, 0.55f * shade, 0.57f * shade, 0.99f,
					0.96f * shade, 0.95f * shade, 0.91f * shade, 0.99f);
			quad(b, m, plane, x0, 0, x1, 0, x1, upper(x1, open), x0, upper(x0, open), 0.96f * shade, 0.95f * shade, 0.91f * shade, 0.99f,
					0.50f * shade, 0.47f * shade, 0.50f * shade, 0.99f);
		}
		BufferUploader.drawWithShader(b.end());

		// Fine red veins reaching from the iris toward the corners, darker and thicker as its judgement falls.
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		RandomSource veins = RandomSource.create(77L);
		for (int v = 0; v < 16; v++) {
			float side = v % 2 == 0 ? 1 : -1;
			float y = (veins.nextFloat() - 0.5f) * EYE_H * 0.9f;
			float px = gazeX + side * IRIS * 1.02f, py = gazeY + y * 0.4f;
			for (int s = 0; s < 7; s++) {
				float nx = px + side * (EYE_W - IRIS) / 7, ny = py + (veins.nextFloat() - 0.5f) * 2.4f + (y - py) * 0.3f;
				float fade = (0.35f + 0.4f * judging) * (1 - s / 7.0f);
				line(b, m, plane, px, py, nx, ny, 0.62f, 0.04f, 0.07f, 0.28f + 0.2f * judging, fade);
				px = nx;
				py = ny;
			}
		}
		BufferUploader.drawWithShader(b.end());

		iris(b, m, plane, gazeX, gazeY, pupil, time, attention, judging);

		// The lids: the void's own dark closing over everything outside the eye's opening, soft at their far edge.
		RenderSystem.defaultBlendFunc();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < EYE_COLUMNS + 12; i++) {
			float reach = EYE_W * 1.3f;
			float x0 = Mth.lerp(i / (float) (EYE_COLUMNS + 12), -reach, reach), x1 = Mth.lerp((i + 1) / (float) (EYE_COLUMNS + 12), -reach, reach);
			lid(b, m, plane, x0, x1, upper(x0, open), upper(x1, open), outerUpper(x0), outerUpper(x1));
			lid(b, m, plane, x0, x1, lower(x0, open), lower(x1, open), outerLower(x0), outerLower(x1));
		}
		BufferUploader.drawWithShader(b.end());

		// The lids' rims glow: a bright cold line where they meet the eye (all that shows of it, closed).
		additive();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (int i = 0; i < EYE_COLUMNS; i++) {
			float x0 = Mth.lerp(i / (float) EYE_COLUMNS, -EYE_W, EYE_W), x1 = Mth.lerp((i + 1) / (float) EYE_COLUMNS, -EYE_W, EYE_W);
			float taper0 = 1 - Math.abs(x0) / EYE_W, taper1 = 1 - Math.abs(x1) / EYE_W;
			line(b, m, plane, x0, upper(x0, open), x1, upper(x1, open), 0.55f, 0.85f, 1.0f, 0.45f, 0.55f * Math.min(taper0, taper1) + 0.1f);
			line(b, m, plane, x0, lower(x0, open), x1, lower(x1, open), 0.45f, 0.70f, 1.0f, 0.30f, 0.35f * Math.min(taper0, taper1) + 0.08f);
		}
		BufferUploader.drawWithShader(b.end());
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
	}

	/**
	 * The iris (amber rim, cyan middle, pale gold heart, fine rays turning slowly; all bleeding to crimson as {@code judging}
	 * rises), the slit pupil, and two glints of light.
	 */
	private static void iris(BufferBuilder b, Matrix4f m, Plane plane, float gx, float gy, float pupil, float time, float attention,
							 float judging) {
		RenderSystem.defaultBlendFunc();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float[] radii = {0.0f, 0.32f, 0.62f, 0.9f, 1.0f, 1.07f};
		float[][] colours = {{1.0f, 0.86f, 0.50f}, {0.95f, 0.72f, 0.30f}, {0.20f, 0.78f, 0.88f}, {0.06f, 0.32f, 0.45f}, {0.55f, 0.32f, 0.08f},
				{0.02f, 0.01f, 0.04f}};
		float[][] crimson = {{1.0f, 0.55f, 0.35f}, {0.95f, 0.25f, 0.12f}, {0.75f, 0.05f, 0.08f}, {0.35f, 0.02f, 0.04f}, {0.50f, 0.10f, 0.04f},
				{0.02f, 0.0f, 0.0f}};
		int segments = 48;
		for (int ring = 0; ring < radii.length - 1; ring++) {
			float r0 = radii[ring] * IRIS, r1 = radii[ring + 1] * IRIS;
			float[] c0 = SkyDome.lerp(colours[ring], crimson[ring], judging), c1 = SkyDome.lerp(colours[ring + 1], crimson[ring + 1], judging);
			for (int i = 0; i < segments; i++) {
				float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
				quad(b, m, plane, gx + Mth.cos(a0) * r0, gy + Mth.sin(a0) * r0, gx + Mth.cos(a1) * r0, gy + Mth.sin(a1) * r0,
						gx + Mth.cos(a1) * r1, gy + Mth.sin(a1) * r1, gx + Mth.cos(a0) * r1, gy + Mth.sin(a0) * r1,
						c0[0], c0[1], c0[2], 1.0f, c1[0], c1[1], c1[2], ring == radii.length - 2 ? 0.0f : 1.0f);
			}
		}
		BufferUploader.drawWithShader(b.end());

		// Fine rays turning slowly, and a glow that swells as it looks at the traveller.
		additive();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int rays = 72;
		float turn = time * 0.0015f;
		for (int i = 0; i < rays; i++) {
			float a = Mth.TWO_PI * i / rays + turn;
			float bright = 0.10f + 0.16f * (0.5f + 0.5f * Mth.sin(i * 2.7f + time * 0.02f));
			line(b, m, plane, gx + Mth.cos(a) * IRIS * 0.3f, gy + Mth.sin(a) * IRIS * 0.3f, gx + Mth.cos(a) * IRIS * 0.95f,
					gy + Mth.sin(a) * IRIS * 0.95f, Mth.lerp(judging, 0.45f, 1.0f), Mth.lerp(judging, 1.0f, 0.3f), Mth.lerp(judging, 1.0f, 0.2f),
					0.12f, bright);
		}
		BufferUploader.drawWithShader(b.end());
		b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		ellipse(b, m, plane, gx, gy, IRIS * 1.15f, IRIS * 1.15f, Mth.lerp(judging, 0.40f, 1.0f), Mth.lerp(judging, 0.95f, 0.2f),
				Mth.lerp(judging, 1.0f, 0.1f), 0.12f + 0.18f * attention, 0);
		BufferUploader.drawWithShader(b.end());

		// The pupil: an upright slit of black, rimmed in violet.
		RenderSystem.defaultBlendFunc();
		b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		ellipse(b, m, plane, gx, gy, pupil, IRIS * 0.86f, 0.0f, 0.0f, 0.01f, 1.0f, 1.0f);
		BufferUploader.drawWithShader(b.end());
		additive();
		b.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		ellipse(b, m, plane, gx, gy, pupil * 1.6f, IRIS * 0.95f, 0.55f, 0.20f, 0.95f, 0.0f, 0.25f);
		ellipse(b, m, plane, gx - IRIS * 0.38f, gy + IRIS * 0.42f, IRIS * 0.14f, IRIS * 0.14f, 1.0f, 1.0f, 1.0f, 0.55f, 0);
		ellipse(b, m, plane, gx + IRIS * 0.30f, gy - IRIS * 0.32f, IRIS * 0.06f, IRIS * 0.06f, 0.85f, 1.0f, 1.0f, 0.35f, 0);
		BufferUploader.drawWithShader(b.end());
	}

	/** The opening's upper edge at {@code x}, and its lower; closed (both 0) past the corners. */
	private static float upper(float x, float open) {
		float t = 1 - (x / EYE_W) * (x / EYE_W);
		return t <= 0 ? 0 : EYE_H * open * (float) Math.pow(t, 0.85);
	}

	private static float lower(float x, float open) {
		float t = 1 - (x / EYE_W) * (x / EYE_W);
		return t <= 0 ? 0 : -EYE_H * 0.82f * open * (float) Math.pow(t, 0.95);
	}

	private static float outerUpper(float x) {
		float t = 1 - (x / (EYE_W * 1.3f)) * (x / (EYE_W * 1.3f));
		return EYE_H * 2.4f * (float) Math.pow(Math.max(0, t), 0.6) + 0.5f;
	}

	private static float outerLower(float x) {
		float t = 1 - (x / (EYE_W * 1.3f)) * (x / (EYE_W * 1.3f));
		return -EYE_H * 2.0f * (float) Math.pow(Math.max(0, t), 0.6) - 0.5f;
	}

	/** One column of a lid, from the opening's edge out to its soft far edge: nearly solid most of the way, then fading. */
	private static void lid(BufferBuilder b, Matrix4f m, Plane plane, float x0, float x1, float in0, float in1, float out0, float out1) {
		float mid0 = Mth.lerp(0.65f, in0, out0), mid1 = Mth.lerp(0.65f, in1, out1);
		quad(b, m, plane, x0, in0, x1, in1, x1, mid1, x0, mid0, 0.012f, 0.008f, 0.030f, 0.99f, 0.012f, 0.008f, 0.032f, 0.97f);
		quad(b, m, plane, x0, mid0, x1, mid1, x1, out1, x0, out0, 0.012f, 0.008f, 0.032f, 0.97f, 0.012f, 0.008f, 0.035f, 0.0f);
	}

	// --- Drawing on the Eye's plane ---

	/** A quad with its first two corners in one colour and its last two in another. */
	private static void quad(BufferBuilder b, Matrix4f m, Plane p, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
							 float r0, float g0, float b0, float a0, float r1, float g1, float b1, float a1) {
		vertex(b, m, p.at(x0, y0), r0, g0, b0, a0);
		vertex(b, m, p.at(x1, y1), r0, g0, b0, a0);
		vertex(b, m, p.at(x2, y2), r1, g1, b1, a1);
		vertex(b, m, p.at(x3, y3), r1, g1, b1, a1);
	}

	/** A filled ellipse: {@code alphaCentre} at its middle shading to {@code alphaEdge} at its rim. */
	private static void ellipse(BufferBuilder b, Matrix4f m, Plane p, float cx, float cy, float rx, float ry, float r, float g, float bl,
								float alphaCentre, float alphaEdge) {
		int segments = 40;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			vertex(b, m, p.at(cx, cy), r, g, bl, alphaCentre);
			vertex(b, m, p.at(cx + Mth.cos(a0) * rx, cy + Mth.sin(a0) * ry), r, g, bl, alphaEdge);
			vertex(b, m, p.at(cx + Mth.cos(a1) * rx, cy + Mth.sin(a1) * ry), r, g, bl, alphaEdge);
		}
	}

	/** A line {@code width} wide on the plane, brightest along its middle. */
	private static void line(BufferBuilder b, Matrix4f m, Plane p, float x0, float y0, float x1, float y1, float r, float g, float bl,
							 float width, float alpha) {
		float dx = x1 - x0, dy = y1 - y0;
		float length = Mth.sqrt(dx * dx + dy * dy);
		if (length < 1.0e-4f) return;
		float nx = -dy / length * width / 2, ny = dx / length * width / 2;
		quad(b, m, p, x0 - nx, y0 - ny, x1 - nx, y1 - ny, x1, y1, x0, y0, r, g, bl, 0, r, g, bl, alpha);
		quad(b, m, p, x0, y0, x1, y1, x1 + nx, y1 + ny, x0 + nx, y0 + ny, r, g, bl, alpha, r, g, bl, 0);
	}

	private static void vertex(BufferBuilder b, Matrix4f m, Vector3f at, float r, float g, float bl, float a) {
		b.vertex(m, at.x, at.y, at.z).color(Mth.clamp(r, 0, 1), Mth.clamp(g, 0, 1), Mth.clamp(bl, 0, 1), Mth.clamp(a, 0, 1)).endVertex();
	}

	private static void additive() {
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
	}
}
