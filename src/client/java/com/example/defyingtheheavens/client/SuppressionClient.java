package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.example.defyingtheheavens.ModParticles;
import com.example.defyingtheheavens.PlayerCultivation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * What Realm Suppress looks like from outside, for everyone nearby (no ability needed): heavy dark-red motes falling around
 * each pressed entity (more the heavier the pressure), scattering at its feet, and a dark-red double chevron pressing down
 * over its head. The pressed player also gets a status line under their Qi bar. Fed by the PRESSED_ENTITIES packet twice a
 * second; entries lapse if it stops.
 */
public final class SuppressionClient {
	private static final long EXPIRY_MS = 1500;
	/** Pressed entities further than this from the camera aren't drawn. */
	private static final double DRAW_RANGE = 48;
	private static final float MARKER_SIZE = 0.2f;
	private static final float[] MARKER_RGB = {0.55f, 0.05f, 0.07f};
	private static final int HUD_X = 4;
	private static final int HUD_Y = 17;

	/** An entity under pressure; {@code heavy} when the suppressor is a whole realm above. */
	public record Pressed(int entityId, float penalty, boolean heavy) {}

	private static final Int2ObjectOpenHashMap<Pressed> PRESSED = new Int2ObjectOpenHashMap<>();
	private static final RandomSource RANDOM = RandomSource.create();
	private static long lastUpdate;
	private static float carry;

	public static void update(List<Pressed> pressed) {
		PRESSED.clear();
		for (Pressed p : pressed) PRESSED.put(p.entityId(), p);
		lastUpdate = Util.getMillis();
	}

	public static void tick(Minecraft mc) {
		if (PRESSED.isEmpty()) return;
		if (mc.level == null || Util.getMillis() - lastUpdate > EXPIRY_MS) {
			PRESSED.clear();
			return;
		}
		if (mc.isPaused()) return;
		Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
		for (Pressed pressed : PRESSED.values()) {
			Entity entity = mc.level.getEntity(pressed.entityId());
			if (entity == null || !entity.isAlive() || entity.distanceToSqr(camera) > DRAW_RANGE * DRAW_RANGE) continue;
			// Mortals (half strength) and cultivators a realm below fall heavily; a stage below, a light drizzle.
			float perTick = pressed.heavy() ? 1.6f : pressed.penalty() >= 0.5f ? 1.0f : 0.25f + pressed.penalty() * 2;
			boolean self = entity == mc.player && mc.options.getCameraType().isFirstPerson();
			if (self) perTick *= 0.4f; // around the eyes it would hide the view
			carry += perTick;
			double width = entity.getBbWidth();
			while (carry >= 1) {
				carry--;
				double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * width * 1.8;
				double y = entity.getY() + entity.getBbHeight() * (0.75 + RANDOM.nextDouble() * 0.45);
				double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * width * 1.8;
				mc.level.addParticle(ModParticles.SUPPRESSION, x, y, z, 0, -0.15 - RANDOM.nextDouble() * 0.1, 0);
			}
			// The ground under it gives a little: motes scatter outward from its feet.
			if (!self && entity.tickCount % 8 == 0) {
				int ring = pressed.heavy() || pressed.penalty() >= 0.5f ? 6 : 3;
				for (int i = 0; i < ring; i++) {
					double angle = RANDOM.nextDouble() * Math.PI * 2;
					mc.level.addParticle(ModParticles.SUPPRESSION, entity.getX() + Math.cos(angle) * width * 0.5, entity.getY() + 0.05,
							entity.getZ() + Math.sin(angle) * width * 0.5, Math.cos(angle) * 0.08, 0.01, Math.sin(angle) * 0.08);
				}
			}
		}
	}

	/** WorldRenderEvents.AFTER_TRANSLUCENT: a dark-red double chevron over each pressed head, sinking as if pushed down. */
	public static void renderMarkers(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (PRESSED.isEmpty() || mc.level == null) return;
		Camera camera = context.camera();
		Vec3 cam = camera.getPosition();
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		float partial = context.tickDelta();
		Matrix4f matrix = context.matrixStack().last().pose();
		float time = mc.level.getGameTime() + partial;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		for (Pressed pressed : PRESSED.values()) {
			Entity entity = mc.level.getEntity(pressed.entityId());
			if (entity == null || !entity.isAlive() || (entity == mc.player && !camera.isDetached())) continue;
			Vec3 pos = entity.getPosition(partial);
			if (pos.distanceToSqr(cam) > DRAW_RANGE * DRAW_RANGE) continue;
			float sink = (time * 0.04f + entity.getId() * 0.37f) % 1f; // drifts down a little, then starts over
			Vec3 at = pos.add(0, entity.getBbHeight() + 0.42 - sink * 0.12, 0).subtract(cam);
			float alpha = 0.85f * Mth.sin(sink * Mth.PI) + 0.15f;
			chevron(buffer, matrix, at, left, up, 0, alpha);
			chevron(buffer, matrix, at, left, up, -MARKER_SIZE * 0.55f, alpha * 0.8f);
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	/** A "V" facing the camera, {@code drop} below {@code at}: two thick strokes meeting at the bottom. */
	private static void chevron(BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vector3f left, Vector3f up, float drop, float alpha) {
		float s = MARKER_SIZE;
		float t = s * 0.28f; // stroke thickness
		float[][] points = {
				{-s, 0}, {-s + t, 0}, {0, -s * 0.6f + t}, {0, -s * 0.6f},
				{s, 0}, {s - t, 0}};
		// Left stroke: outer-top, inner-top, inner-bottom, outer-bottom; right stroke mirrors it.
		quad(buffer, matrix, at, left, up, drop, points[0], points[1], points[2], points[3], alpha);
		quad(buffer, matrix, at, left, up, drop, points[4], points[5], points[2], points[3], alpha);
	}

	private static void quad(BufferBuilder buffer, Matrix4f matrix, Vec3 at, Vector3f left, Vector3f up, float drop, float[] a, float[] b,
							 float[] c, float[] d, float alpha) {
		float[][] tris = {a, b, c, a, c, d};
		for (float[] p : tris) {
			// Camera space: x along "left" (negated, so +x is to the viewer's right), y along "up".
			float x = (float) at.x - left.x() * p[0] + up.x() * (p[1] + drop);
			float y = (float) at.y - left.y() * p[0] + up.y() * (p[1] + drop);
			float z = (float) at.z - left.z() * p[0] + up.z() * (p[1] + drop);
			buffer.vertex(matrix, x, y, z).color(MARKER_RGB[0], MARKER_RGB[1], MARKER_RGB[2], alpha).endVertex();
		}
	}

	/** Under the Qi bar while the local player is pressed: a small dark-red chevron and how much the pressure takes. */
	public static void renderHud(GuiGraphics graphics) {
		Minecraft mc = Minecraft.getInstance();
		PlayerCultivation c = ClientCultivationData.get();
		if (mc.player == null || mc.options.hideGui || mc.options.renderDebug || mc.player.isSpectator() || !c.isUnderPressure()) return;
		int x = HUD_X;
		int y = HUD_Y;
		// Two stacked chevrons, 7 pixels wide, in crimson over a dark edge.
		for (int stack = 0; stack < 2; stack++) {
			int top = y + stack * 3;
			for (int row = 0; row < 4; row++) {
				int color = stack == 0 ? 0xFFB02020 : 0xFF7A1414;
				graphics.fill(x + row, top + row, x + row + 1, top + row + 1, color);
				graphics.fill(x + 6 - row, top + row, x + 7 - row, top + row + 1, color);
			}
		}
		Component text = Component.translatable(ModLang.HUD_PRESSURE, Math.round(c.getPressurePenalty() * 100));
		graphics.drawString(mc.font, text, x + 10, y, 0xE07070);
	}

	public static void clear() {
		PRESSED.clear();
		carry = 0;
	}

	private SuppressionClient() {}
}
