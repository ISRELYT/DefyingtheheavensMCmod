package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SoulRiverCourse;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Soul River, drawn (see SoulRiverCourse for where it runs, SoulRiverSky for the void around it, RiverSouls for the dead
 * it carries). From the inside out:
 * <ul>
 *   <li>a luminous current along its heart, pulsing downstream;</li>
 *   <li>strands of cyan, blue and violet light twisting round it, bright comets of light running along them;</li>
 *   <li>its skin: currents of light swirling along the walls of the flow, which is also how a traveller sees where it ends;</li>
 *   <li>motes of light drifting in it, red petals of the spider lily tumbling in the current, and paper lanterns floating
 *       down it, warm gold among the cold light;</li>
 *   <li>a ghostly gate spanning the river every {@link #GATE_EVERY} blocks, brightening as a traveller passes beneath;</li>
 *   <li>and around it, far off on both sides, banks of violet and teal mist drifting by and sky lanterns rising into the dark.</li>
 * </ul>
 * Everything glows (drawn added on, not covering), and only the stretch near the camera is drawn, fading out ahead and behind.
 */
public final class SoulRiverRenderer {
	private static final int STRANDS = 16;
	/** Each strand: share of the reach, base angle, twist (radians a block), spin (radians a tick), width, colour (0 cyan, 1 blue, 2 violet). */
	private static final float[][] STRAND = new float[STRANDS][6];
	/** Drawn this far behind and ahead of the camera along the flow; faded out over the last part. */
	private static final double BEHIND = 48, AHEAD = 160;
	private static final double STEP = 1.5;
	/** Past either end the river runs on this far into the dark, fading as it goes. */
	private static final double END_FADE = 40;
	private static final int WALL_SEGMENTS = 28;
	private static final double WALL_STEP = 2.0;
	private static final int GATE_EVERY = 96;
	private static final float[] CYAN = {0.35f, 0.95f, 1.0f};
	private static final float[] BLUE = {0.15f, 0.45f, 1.0f};
	private static final float[] VIOLET = {0.62f, 0.38f, 1.0f};
	private static final float[] PALE = {0.80f, 0.98f, 1.0f};
	private static final float[] GOLD = {1.0f, 0.78f, 0.38f};
	private static final float[] EMBER = {1.0f, 0.55f, 0.18f};
	private static final float[] LILY = {0.95f, 0.10f, 0.16f};
	private static final float[] MIST_VIOLET = {0.40f, 0.22f, 0.75f};
	private static final float[] MIST_TEAL = {0.10f, 0.50f, 0.60f};

	static {
		RandomSource random = RandomSource.create(5150L);
		for (int i = 0; i < STRANDS; i++) {
			float sign = i % 2 == 0 ? 1 : -1;
			STRAND[i][0] = 0.15f + 0.8f * random.nextFloat();
			STRAND[i][1] = random.nextFloat() * Mth.TWO_PI;
			STRAND[i][2] = sign * (0.015f + 0.035f * random.nextFloat());
			STRAND[i][3] = -sign * (0.004f + 0.008f * random.nextFloat());
			STRAND[i][4] = 0.05f + 0.13f * random.nextFloat();
			STRAND[i][5] = random.nextInt(3);
		}
	}

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(SoulRiverRenderer::render);
		DimensionRenderingRegistry.registerSkyRenderer(ModDimensions.SOUL_RIVER, SoulRiverSky::render);
	}

	private static void render(WorldRenderContext context) {
		ClientLevel level = context.world();
		if (level == null || !ModDimensions.isSoulRiver(level.dimension())) return;
		try {
			Camera camera = context.camera();
			Vec3 cam = camera.getPosition();
			float time = level.getGameTime() + context.tickDelta();
			int flow = ClientSoulRiver.flow();
			boolean up = flow > 0;
			Matrix4f pose = context.matrixStack().last().pose();
			double from = Math.max(-END_FADE, cam.x - (up ? BEHIND : AHEAD));
			double to = Math.min(SoulRiverCourse.LENGTH + END_FADE, cam.x + (up ? AHEAD : BEHIND));

			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			RenderSystem.depthMask(false);
			RenderSystem.disableCull();
			BufferBuilder buffer = Tesselator.getInstance().getBuilder();
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			// The far things first: the mist banks and sky lanterns, all along the river, whatever is drawn of the river itself.
			mist(buffer, pose, camera, cam, flow, time);
			skyLanterns(buffer, pose, camera, cam, time);
			if (to > from) {
				wall(buffer, pose, cam, from, to, up, flow, time);
				core(buffer, pose, cam, from, to, up, flow, time);
				for (int k = 0; k < STRANDS; k++) strand(buffer, pose, cam, from, to, up, flow, time, STRAND[k], k);
				sparkles(buffer, pose, camera, cam, from, to, up, flow, time);
				petals(buffer, pose, cam, from, to, up, flow, time);
				lanterns(buffer, pose, camera, cam, up, flow, time);
				gates(buffer, pose, cam, up, time);
			}
			BufferUploader.drawWithShader(buffer.end());
			RiverSouls.render(context.matrixStack(), cam, flow, time);
		} catch (Exception e) {
			DefyingTheHeavens.LOGGER.error("Soul River rendering failed", e);
		} finally {
			RenderSystem.enableCull();
			RenderSystem.depthMask(true);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
		}
	}

	/** How strongly the river shows at {@code x}: fading out past its ends and toward the far edges of what is drawn. */
	private static float fade(double x, Vec3 cam, boolean up) {
		double ends = Mth.clamp((x + END_FADE) / END_FADE, 0, 1) * Mth.clamp((SoulRiverCourse.LENGTH + END_FADE - x) / END_FADE, 0, 1);
		double ahead = (x - cam.x) * (up ? 1 : -1);
		double reach = ahead >= 0 ? AHEAD : BEHIND;
		double far = 1 - Mth.clamp((Math.abs(ahead) - reach * 0.55) / (reach * 0.45), 0, 1);
		return (float) (ends * far * far);
	}

	private static double radius(double x, boolean up) {
		return SoulRiverCourse.radius(SoulRiverCourse.progress(x, up));
	}

	/**
	 * Light right at the camera would fill the view with flat smears: everything fades out within a few blocks of the eye
	 * ({@code p} relative to the camera).
	 */
	private static float nearFade(Vec3 p) {
		return (float) Mth.clamp((p.length() - 1.0) / 3.0, 0, 1);
	}

	private static Vec3 around(double x, double r, double angle) {
		return SoulRiverCourse.centre(x).add(0, r * Math.sin(angle), r * Math.cos(angle));
	}

	/**
	 * The river's skin: faint, but with bands of light swirling along it downstream, cyan shading to violet round the river.
	 * It is where the flow ends, so a traveller can always see how far they may go.
	 */
	private static void wall(BufferBuilder b, Matrix4f m, Vec3 cam, double from, double to, boolean up, int flow, float time) {
		for (double x = from; x < to; x += WALL_STEP) {
			double x1 = Math.min(to, x + WALL_STEP);
			float f0 = fade(x, cam, up), f1 = fade(x1, cam, up);
			double r0 = radius(x, up), r1 = radius(x1, up);
			for (int i = 0; i < WALL_SEGMENTS; i++) {
				double a0 = Mth.TWO_PI * i / WALL_SEGMENTS, a1 = Mth.TWO_PI * (i + 1) / WALL_SEGMENTS;
				float[] colour = mix(CYAN, VIOLET, (float) (0.5 + 0.5 * Math.sin(a0)));
				vertex(b, m, around(x, r0, a0).subtract(cam), colour, wallGlow(x, a0, flow, time) * f0);
				vertex(b, m, around(x, r0, a1).subtract(cam), colour, wallGlow(x, a1, flow, time) * f0);
				vertex(b, m, around(x1, r1, a1).subtract(cam), colour, wallGlow(x1, a1, flow, time) * f1);
				vertex(b, m, around(x1, r1, a0).subtract(cam), colour, wallGlow(x1, a0, flow, time) * f1);
			}
		}
	}

	private static float wallGlow(double x, double angle, int flow, float time) {
		double swirl = angle + x * 0.06 - time * 0.012 * flow;
		double band = Math.pow(0.5 + 0.5 * Math.cos(swirl * 4), 12);
		double run = 0.55 + 0.45 * Math.sin(x * 0.17 - time * 0.22 * flow);
		return (float) (0.018 + 0.16 * band * run);
	}

	/** The luminous current at the river's heart: a soft wide glow and a brighter thread, pulsing downstream. */
	private static void core(BufferBuilder b, Matrix4f m, Vec3 cam, double from, double to, boolean up, int flow, float time) {
		Vec3 previous = null;
		float previousAlpha = 0;
		for (double x = from; x <= to + 1e-6; x += STEP) {
			Vec3 p = SoulRiverCourse.centre(x).subtract(cam);
			double pulse = Math.pow(0.5 + 0.5 * Math.sin(x * 0.12 - flow * time * 0.3), 4);
			float alpha = (float) ((0.10 + 0.25 * pulse) * fade(x, cam, up)) * nearFade(p);
			if (previous != null) {
				ribbon(b, m, previous, p, 2.4f, BLUE, previousAlpha * 0.35f, alpha * 0.35f);
				ribbon(b, m, previous, p, 0.35f, PALE, previousAlpha, alpha);
			}
			previous = p;
			previousAlpha = alpha;
		}
	}

	/** One strand of light winding round the river's centre, with comets of light running downstream along it. */
	private static void strand(BufferBuilder b, Matrix4f m, Vec3 cam, double from, double to, boolean up, int flow, float time,
							   float[] s, int k) {
		float[] color = s[5] == 0 ? CYAN : s[5] == 1 ? BLUE : VIOLET;
		Vec3 previous = null;
		float previousAlpha = 0;
		for (double x = from; x <= to + 1e-6; x += STEP) {
			double theta = s[1] + s[2] * x + s[3] * time;
			double r = radius(x, up) * s[0] * (1 + 0.08 * Math.sin(x * 0.13 + k + time * 0.02));
			Vec3 p = around(x, r, theta).subtract(cam);
			// A comet: a sharp bright head and a tail behind it, running downstream.
			double phase = Mth.positiveModulo(x * 0.05 * flow - time * 0.04 + k * 0.37, 1.0);
			double comet = Math.pow(phase, 6);
			float alpha = (float) ((0.10 + 0.75 * comet) * fade(x, cam, up)) * nearFade(p);
			if (previous != null) ribbon(b, m, previous, p, s[4], color, previousAlpha, alpha);
			previous = p;
			previousAlpha = alpha;
		}
	}

	/** Motes of light inside the river, each drifting downstream across its own block and twinkling in and out; a few gold. */
	private static void sparkles(BufferBuilder b, Matrix4f m, Camera camera, Vec3 cam, double from, double to, boolean up, int flow,
								 float time) {
		Vector3f left = camera.getLeftVector(), upVector = camera.getUpVector();
		for (int cell = (int) Math.floor(from); cell <= (int) Math.ceil(to); cell++) {
			for (int j = 0; j < 4; j++) {
				float u1 = hash(cell, j, 1), u2 = hash(cell, j, 2), u3 = hash(cell, j, 3), u4 = hash(cell, j, 4), u5 = hash(cell, j, 5);
				double along = (u1 + time * (0.01 + 0.03 * u2)) % 1.0;
				double x = cell + (flow > 0 ? along : 1 - along);
				double r = radius(x, up) * Math.sqrt(u3) * 0.95;
				Vec3 p = around(x, r, u4 * Mth.TWO_PI + time * 0.01).subtract(cam);
				float twinkle = (float) (Math.sin(Math.PI * along) * (0.5 + 0.5 * Math.sin(time * 0.3 + u5 * 20)));
				float alpha = 0.95f * twinkle * fade(x, cam, up) * nearFade(p);
				if (alpha < 0.02f) continue;
				float size = 0.04f + 0.09f * u5;
				disc(b, m, p, left, upVector, size, u2 < 0.1f ? GOLD : PALE, alpha, 6);
				if (u5 > 0.8f) disc(b, m, p, left, upVector, size * 4, CYAN, alpha * 0.2f, 8); // the brightest bloom
			}
		}
	}

	/** Red petals of the spider lily, the flower of the other shore, tumbling slowly in the current. */
	private static void petals(BufferBuilder b, Matrix4f m, Vec3 cam, double from, double to, boolean up, int flow, float time) {
		int cellSize = 6;
		for (int cell = (int) Math.floor(from / cellSize); cell <= (int) Math.ceil(to / cellSize); cell++) {
			for (int j = 0; j < 2; j++) {
				float u1 = hash(cell, 20 + j, 1), u2 = hash(cell, 20 + j, 2), u3 = hash(cell, 20 + j, 3), u4 = hash(cell, 20 + j, 4);
				double along = (u1 + time * 0.004 * (0.6 + u2)) % 1.0;
				double x = cell * cellSize + (flow > 0 ? along : 1 - along) * cellSize;
				double r = radius(x, up) * (0.2 + 0.7 * u3);
				Vec3 p = around(x, r, u4 * Mth.TWO_PI + time * 0.006).subtract(cam);
				float alpha = (float) (0.85 * Math.sin(Math.PI * along)) * fade(x, cam, up) * nearFade(p);
				if (alpha < 0.03f) continue;
				// Turning over and over about an axis of its own.
				float spin = time * (0.05f + 0.06f * u2) + u1 * 10;
				Vector3f axisA = new Vector3f(Mth.cos(spin), Mth.sin(spin * 0.7f), Mth.sin(spin)).normalize().mul(0.11f);
				Vector3f axisB = new Vector3f(-Mth.sin(spin * 1.3f), Mth.cos(spin), Mth.cos(spin * 0.6f)).cross(axisA).normalize().mul(0.045f);
				vertex(b, m, p.add(-axisA.x, -axisA.y, -axisA.z), LILY, alpha * 0.6f);
				vertex(b, m, p.add(axisB.x, axisB.y, axisB.z), LILY, alpha);
				vertex(b, m, p.add(axisA.x, axisA.y, axisA.z), LILY, alpha * 0.8f);
				vertex(b, m, p.add(-axisB.x, -axisB.y, -axisB.z), LILY, alpha);
			}
		}
	}

	/** Paper lanterns floating down the river near its edge, slower than the current: warm light among the cold. */
	private static void lanterns(BufferBuilder b, Matrix4f m, Camera camera, Vec3 cam, boolean up, int flow, float time) {
		Vector3f left = camera.getLeftVector(), upVector = camera.getUpVector();
		double loop = SoulRiverCourse.LENGTH + 2 * END_FADE;
		int count = 26;
		for (int i = 0; i < count; i++) {
			float u1 = hash(i, 40, 1), u2 = hash(i, 40, 2), u3 = hash(i, 40, 3);
			double x = -END_FADE + Mth.positiveModulo(u1 * loop + time * SoulRiverCourse.FLOW * (0.35 + 0.3 * u2) * flow, loop);
			float alpha = fade(x, cam, up);
			if (alpha < 0.02f) continue;
			// Riding low on the flow, as on water.
			double angle = -Mth.HALF_PI + (u3 - 0.5) * 2.2 + 0.1 * Math.sin(time * 0.03 + i);
			Vec3 p = around(x, radius(x, up) * 0.78, angle).add(0, 0.05 * Math.sin(time * 0.05 + i * 2), 0).subtract(cam);
			alpha *= nearFade(p);
			if (alpha < 0.02f) continue;
			float flicker = 0.85f + 0.15f * Mth.sin(time * 0.4f + i * 7);
			box(b, m, p, 0.16f, 0.22f, EMBER, 0.55f * alpha * flicker);
			disc(b, m, p, left, upVector, 0.28f, GOLD, 0.9f * alpha * flicker, 10);
			disc(b, m, p, left, upVector, 1.1f, GOLD, 0.25f * alpha * flicker, 14);
			disc(b, m, p, left, upVector, 2.6f, EMBER, 0.08f * alpha, 16);
		}
	}

	/**
	 * A ghostly gate (a pailou of light: two pillars, a double lintel with upturned eaves and a plaque) spanning the river every
	 * {@link #GATE_EVERY} blocks, brightening as a traveller passes beneath it.
	 */
	private static void gates(BufferBuilder b, Matrix4f m, Vec3 cam, boolean up, float time) {
		for (int gate = GATE_EVERY / 2; gate < SoulRiverCourse.LENGTH; gate += GATE_EVERY) {
			float alpha = fade(gate, cam, up);
			if (alpha < 0.02f) continue;
			double near = Math.abs(gate - cam.x);
			alpha *= (float) (0.55 + 0.45 * Math.max(0, 1 - near / 12)) * (0.9f + 0.1f * Mth.sin(time * 0.05f + gate));
			Vec3 c = SoulRiverCourse.centre(gate);
			double r = radius(gate, up);
			double half = r + 1.4, bottom = c.y - r - 2.0, top = c.y + r + 2.2;
			double eave = half + 1.6;
			double[][] lines = {
					{-half, bottom, -half, top}, {half, bottom, half, top},                     // pillars
					{-eave, top, eave, top}, {-half - 0.6, top - 0.9, half + 0.6, top - 0.9},     // lintels
					{-eave, top, -eave - 0.9, top + 0.8}, {eave, top, eave + 0.9, top + 0.8},     // upturned eaves
					{-1.1, top - 0.9, -1.1, top - 2.3}, {1.1, top - 0.9, 1.1, top - 2.3}, {-1.1, top - 2.3, 1.1, top - 2.3}, // the plaque
					{-half - 0.7, bottom, -half + 0.7, bottom}, {half - 0.7, bottom, half + 0.7, bottom}}; // feet
			for (double[] line : lines) {
				Vec3 a = new Vec3(gate, line[1], c.z + line[0]).subtract(cam), z = new Vec3(gate, line[3], c.z + line[2]).subtract(cam);
				ribbon(b, m, a, z, 0.7f, CYAN, 0.10f * alpha, 0.10f * alpha);
				ribbon(b, m, a, z, 0.14f, GOLD, 0.65f * alpha, 0.65f * alpha);
			}
		}
	}

	/**
	 * Banks of violet and teal mist far off on both sides of the river, above and below, drifting slowly downstream: the void
	 * isn't empty, and their slow passing shows how fast the river runs.
	 */
	private static void mist(BufferBuilder b, Matrix4f m, Camera camera, Vec3 cam, int flow, float time) {
		Vector3f left = camera.getLeftVector(), upVector = camera.getUpVector();
		int cellSize = 24;
		int fromCell = (int) Math.floor((cam.x - 160) / cellSize), toCell = (int) Math.ceil((cam.x + 160) / cellSize);
		for (int cell = fromCell; cell <= toCell; cell++) {
			for (int j = 0; j < 3; j++) {
				float u1 = hash(cell, 60 + j, 1), u2 = hash(cell, 60 + j, 2), u3 = hash(cell, 60 + j, 3), u4 = hash(cell, 60 + j, 4);
				double along = (u1 + time * 0.0006) % 1.0;
				double x = (cell + (flow > 0 ? along : 1 - along)) * cellSize;
				double side = (u2 < 0.5 ? -1 : 1) * (22 + 70 * u3);
				double height = SoulRiverCourse.BASE_Y - 45 + 90 * u4;
				Vec3 p = new Vec3(x, height, SoulRiverCourse.centre(x).z + side).subtract(cam);
				double distance = p.length();
				float alpha = (float) (Math.sin(Math.PI * along) * Mth.clamp(1 - (distance - 110) / 60, 0, 1) * (0.05 + 0.05 * u1));
				if (alpha < 0.005f) continue;
				float size = 9 + 22 * u3;
				disc(b, m, p, left, upVector, size, u4 < 0.5f ? MIST_VIOLET : MIST_TEAL, alpha, 12);
			}
		}
	}

	/** Sky lanterns far off, rising slowly into the dark, swaying. */
	private static void skyLanterns(BufferBuilder b, Matrix4f m, Camera camera, Vec3 cam, float time) {
		Vector3f left = camera.getLeftVector(), upVector = camera.getUpVector();
		int cellSize = 20;
		int fromCell = (int) Math.floor((cam.x - 150) / cellSize), toCell = (int) Math.ceil((cam.x + 150) / cellSize);
		for (int cell = fromCell; cell <= toCell; cell++) {
			for (int j = 0; j < 2; j++) {
				float u1 = hash(cell, 80 + j, 1), u2 = hash(cell, 80 + j, 2), u3 = hash(cell, 80 + j, 3);
				double rise = (u1 + time * 0.00025) % 1.0;
				double side = (u2 < 0.5 ? -1 : 1) * (30 + 90 * u3);
				double x = cell * cellSize + 20 * u2 + Math.sin(time * 0.01 + u1 * 30) * 1.5;
				Vec3 p = new Vec3(x, SoulRiverCourse.BASE_Y - 70 + 150 * rise, SoulRiverCourse.centre(x).z + side).subtract(cam);
				float alpha = (float) (Math.sin(Math.PI * rise) * Mth.clamp(1 - (p.length() - 120) / 50, 0, 1));
				if (alpha < 0.02f) continue;
				float flicker = 0.8f + 0.2f * Mth.sin(time * 0.3f + u3 * 40);
				disc(b, m, p, left, upVector, 0.45f, GOLD, 0.95f * alpha * flicker, 8);
				disc(b, m, p, left, upVector, 2.2f, EMBER, 0.14f * alpha * flicker, 12);
			}
		}
	}

	// --- Shapes ---

	/** A flat band from {@code p0} to {@code p1}, turned to face the camera (at the origin), {@code width} wide. */
	private static void ribbon(BufferBuilder b, Matrix4f m, Vec3 p0, Vec3 p1, float width, float[] color, float a0, float a1) {
		if (a0 < 0.004f && a1 < 0.004f) return;
		Vec3 side = p1.subtract(p0).cross(p0);
		double length = side.length();
		if (length < 1.0e-6) return;
		side = side.scale(width / 2 / length);
		vertex(b, m, p0.subtract(side), color, a0);
		vertex(b, m, p1.subtract(side), color, a1);
		vertex(b, m, p1.add(side), color, a1);
		vertex(b, m, p0.add(side), color, a0);
	}

	/** A soft round glow facing the camera: bright in the middle, nothing at its rim (as quads, so it shares the buffer). */
	private static void disc(BufferBuilder b, Matrix4f m, Vec3 p, Vector3f left, Vector3f up, float size, float[] color, float alpha, int segments) {
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			Vec3 e0 = p.add(left.x() * Mth.cos(a0) * size + up.x() * Mth.sin(a0) * size, left.y() * Mth.cos(a0) * size + up.y() * Mth.sin(a0) * size,
					left.z() * Mth.cos(a0) * size + up.z() * Mth.sin(a0) * size);
			Vec3 e1 = p.add(left.x() * Mth.cos(a1) * size + up.x() * Mth.sin(a1) * size, left.y() * Mth.cos(a1) * size + up.y() * Mth.sin(a1) * size,
					left.z() * Mth.cos(a1) * size + up.z() * Mth.sin(a1) * size);
			vertex(b, m, p, color, alpha);
			vertex(b, m, e0, color, 0);
			vertex(b, m, e1, color, 0);
			vertex(b, m, p, color, alpha);
		}
	}

	/** A small upright box (a lantern's body), {@code half} wide and {@code halfHeight} tall each way from {@code p}. */
	private static void box(BufferBuilder b, Matrix4f m, Vec3 p, float half, float halfHeight, float[] color, float alpha) {
		double x0 = p.x - half, x1 = p.x + half, y0 = p.y - halfHeight, y1 = p.y + halfHeight, z0 = p.z - half, z1 = p.z + half;
		double[][][] faces = {
				{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}}, {{x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}},
				{{x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}}, {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}},
				{{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, {{x0, y0, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}}};
		for (double[][] face : faces) {
			for (double[] corner : face) vertex(b, m, new Vec3(corner[0], corner[1], corner[2]), color, alpha);
		}
	}

	private static void vertex(BufferBuilder b, Matrix4f m, Vec3 p, float[] c, float alpha) {
		b.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(c[0], c[1], c[2], Mth.clamp(alpha, 0, 1)).endVertex();
	}

	private static float[] mix(float[] a, float[] c, float t) {
		return new float[]{Mth.lerp(t, a[0], c[0]), Mth.lerp(t, a[1], c[1]), Mth.lerp(t, a[2], c[2])};
	}

	/** A steady pseudo-random number in [0, 1) for a cell of the river, a thing in it, and which of its numbers. */
	private static float hash(int cell, int item, int which) {
		long h = cell * 0x9E3779B97F4A7C15L + item * 0xC2B2AE3D27D4EB4FL + which * 0x165667B19E3779F9L;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return (h >>> 40) / (float) (1L << 24);
	}

	private SoulRiverRenderer() {}
}
