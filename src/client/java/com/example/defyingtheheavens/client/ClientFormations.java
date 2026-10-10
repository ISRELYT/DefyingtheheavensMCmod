package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Formation;
import com.example.defyingtheheavens.Formations;
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
 * The raised formations near the player, as the server last told us ({@code formations} packet, every two seconds). They let
 * the client predict walking through its own barrier (see SectBarrierBlock#getCollisionShape), and under Qi Sense each
 * barrier shows on the sense layer as one unbroken membrane of qi (unlike a Consciousness Domain's lattice): faint where
 * you look straight through it, brightening towards its edges, with soft swells of light drifting across it, and a strong
 * tint over everything while you stand inside it. A core's barrier is purple; a secluded cultivator's Concealment Barrier
 * is green.
 */
public final class ClientFormations {
	/** Deep and light shades of each kind: the deep one where the membrane faces you, the light one at its edges. */
	private static final float[][] CORE = {{0.48f, 0.16f, 0.92f}, {0.86f, 0.62f, 1.0f}};
	private static final float[][] CONCEALMENT = {{0.10f, 0.62f, 0.28f}, {0.58f, 0.98f, 0.64f}};
	private static final int RINGS = 32;
	private static final int SECTORS = 64;
	private static final double DRAW_RANGE = 192;

	private static List<Formation> formations = List.of();

	public static void register() {
		Formations.client = ClientFormations::find;
	}

	public static void update(List<Formation> list) {
		formations = list;
	}

	/** The formation whose shell holds {@code pos}, or null. */
	public static Formation find(BlockPos pos) {
		for (Formation formation : formations) {
			if (formation.isOnShell(pos)) return formation;
		}
		return null;
	}

	/** The raised core formation centred on {@code pos}, or null. */
	public static Formation centredAt(BlockPos pos) {
		for (Formation formation : formations) {
			if (formation.kind == Formation.Kind.CORE && formation.center.equals(pos)) return formation;
		}
		return null;
	}

	static boolean hasDomes() {
		return !formations.isEmpty() && QiSenseClientHandler.isActive();
	}

	/** Draws every barrier in range on {@link SenseOverlay}, which has set up blending against the world's depth. */
	static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		Vec3 camera = context.camera().getPosition();
		Matrix4f matrix = context.matrixStack().last().pose();
		float time = mc.level.getGameTime() + context.tickDelta();
		List<Formation> near = new ArrayList<>();
		for (Formation formation : formations) {
			double reach = DRAW_RANGE + formation.getRadius();
			if (Vec3.atCenterOf(formation.center).distanceToSqr(camera) <= reach * reach) near.add(formation);
		}
		if (near.isEmpty()) return;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		for (Formation formation : near) {
			Vec3 c = Vec3.atCenterOf(formation.center).subtract(camera);
			float r = formation.getRadius() + 0.5f;
			float pulse = 0.85f + 0.15f * Mth.sin(time * 0.04f + formation.center.hashCode());
			// From inside, every line of sight crosses the membrane once, head on: a strong face tints everything in view, so
			// whoever stands within a barrier can't miss it.
			float face = c.lengthSqr() < r * r ? 0.2f : 0.08f;
			membrane(buffer, matrix, c, r, formation.kind == Formation.Kind.CORE ? CORE : CONCEALMENT, face * pulse, 0.42f * pulse,
					time + (formation.center.hashCode() & 1023));
		}
		BufferUploader.drawWithShader(buffer.end());
	}

	/**
	 * One continuous shell of radius {@code r} around {@code c} (camera-relative). Its colour and opacity are worked out
	 * per vertex and blended across each face, so there are no lines: {@code face} where it faces the camera, rising to
	 * {@code edge} where the camera sees it edge-on, and gentle swells of light drifting slowly over it.
	 */
	private static void membrane(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float[][] shades, float face, float edge, float time) {
		for (int i = 0; i < RINGS; i++) {
			float lat0 = -Mth.HALF_PI + Mth.PI * i / RINGS, lat1 = -Mth.HALF_PI + Mth.PI * (i + 1) / RINGS;
			for (int j = 0; j < SECTORS; j++) {
				float lon0 = Mth.TWO_PI * j / SECTORS, lon1 = Mth.TWO_PI * (j + 1) / SECTORS;
				vertex(buffer, matrix, c, r, lat0, lon0, shades, face, edge, time);
				vertex(buffer, matrix, c, r, lat1, lon0, shades, face, edge, time);
				vertex(buffer, matrix, c, r, lat1, lon1, shades, face, edge, time);
				vertex(buffer, matrix, c, r, lat0, lon1, shades, face, edge, time);
			}
		}
	}

	private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3 c, float r, float lat, float lon, float[][] shades, float face,
			float edge, float time) {
		float cosLat = Mth.cos(lat);
		float nx = cosLat * Mth.cos(lon), ny = Mth.sin(lat), nz = cosLat * Mth.sin(lon);
		float x = (float) (c.x + r * nx), y = (float) (c.y + r * ny), z = (float) (c.z + r * nz);
		// How edge-on the camera sees the membrane here: 0 looking straight through it, 1 grazing along it.
		float distance = Mth.sqrt(x * x + y * y + z * z);
		float facing = distance < 1.0e-4f ? 1 : Math.abs(nx * x + ny * y + nz * z) / distance;
		float rim = (1 - facing) * (1 - facing);
		// Two slow swells crossing each other, so the light moves over the surface without ever forming bands.
		float swell = 0.5f + 0.25f * Mth.sin(lat * 3 + lon * 2 + time * 0.03f) + 0.25f * Mth.sin(lat * 5 - lon * 3 - time * 0.021f);
		float alpha = (face + (edge - face) * rim) * (0.45f + 1.1f * swell);
		float mix = Math.min(1, rim + 0.3f * swell);
		buffer.vertex(matrix, x, y, z).color(Mth.lerp(mix, shades[0][0], shades[1][0]), Mth.lerp(mix, shades[0][1], shades[1][1]),
				Mth.lerp(mix, shades[0][2], shades[1][2]), alpha).endVertex();
	}

	public static void clear() {
		formations = List.of();
	}

	private ClientFormations() {}
}
