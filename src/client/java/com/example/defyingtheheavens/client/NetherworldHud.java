package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ModLang;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The Netherworld on the HUD (see ClientNetherworldData), in the top-left stack under the qi bar, as the mockups' "G" draws its thin bars
 * (QiHud.Pool, in the artifact's pixels):
 * <ul>
 *   <li>Ghost Qi: the ghostly qi pool, in teal;</li>
 *   <li>Yin Corruption: how full of the Netherworld's yin a living body is, in a cold blue, its percentage and a small arrow for
 *       which way it is going (up rising, down ebbing, a bar while a pill holds it still) written across it;</li>
 *   <li>and as the corruption passes a quarter, a cold cyan-black frost creeping in from the edges of the screen, ragged and
 *       slowly shifting, thickest at full.</li>
 * </ul>
 * The readied spell and the selected ability, once rows here, are slots by the hotbar now (HotbarSlots).
 */
public final class NetherworldHud {
	private static final QiHud.Pool GHOST = new QiHud.Pool(HallHud.GHOST, HallHud.GHOST_LIGHT);
	private static final QiHud.Pool CORRUPTION = new QiHud.Pool(HallHud.YIN, HallHud.YIN_LIGHT);

	/** The frost starts at this corruption and is thickest at full. */
	private static final float FROST_FROM = 0.25f;
	private static final int FROST_SEGMENTS = 28;

	/** The corruption the frost shows, easing toward the synced value. */
	private static float shown = -1;
	private static long lastFrame;

	public static void register() {
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
			float corruption = ClientNetherworldData.corruption();
			long now = Util.getMillis();
			if (corruption < 0) {
				shown = -1;
			} else if (shown < 0) {
				shown = corruption;
			} else {
				float seconds = Math.min(0.25f, (now - lastFrame) / 1000.0f);
				shown += (corruption - shown) * Math.min(1.0f, seconds * 3.0f);
			}
			lastFrame = now;
			if (shown > FROST_FROM) frost(graphics, (shown - FROST_FROM) / (1 - FROST_FROM), now);
			if (mc.options.renderDebug) return;
			HallArt.beginHud(graphics);
			try {
				float y = QiHud.top() + QiHud.rows() * QiHud.ROW; // under the health and qi, and a soul's own pool under them (Possession)
				if (ClientNetherworldData.ghostQiMax() > 0) {
					GHOST.draw(graphics, y, Component.translatable(ModLang.HUD_GHOST_QI_LABEL), ClientNetherworldData.ghostQi(),
							ClientNetherworldData.ghostQiMax(), true);
					y += QiHud.ROW;
				}
				if (corruption >= 0) {
					String percent = Component.translatable(ModLang.HUD_PERCENT, Math.round(corruption * 100)).getString() + trend();
					CORRUPTION.draw(graphics, y, corruption, Component.translatable(ModLang.HUD_YIN_CORRUPTION_LABEL).getString() + " " + percent, false);
				} else {
					CORRUPTION.reset();
				}
			} finally {
				HallArt.end(graphics);
			}
		});
	}

	/** The rows it draws itself, under the qi bars (TribulationHud's lines go below them all). */
	static int rows() {
		return (ClientNetherworldData.ghostQiMax() > 0 ? 1 : 0) + (ClientNetherworldData.corruption() >= 0 ? 1 : 0);
	}

	/** Which way the corruption goes, after its percentage: up while it rises, down while it ebbs, a bar while a pill holds it still. */
	private static String trend() {
		return switch (ClientNetherworldData.trend()) {
			case 1 -> " ▲";
			case -1 -> " ▼";
			default -> ClientNetherworldData.heldSeconds() > 0 ? " =" : "";
		};
	}

	/**
	 * A cold frost creeping in from the screen's edges, {@code strength} 0 to 1: deepest at the edge, gone at its ragged inner
	 * reach, which shifts slowly as though the cold were feeling its way in.
	 */
	private static void frost(GuiGraphics g, float strength, long now) {
		int w = g.guiWidth(), h = g.guiHeight();
		float depth = Math.min(w, h) * (0.06f + 0.26f * strength);
		float alpha = 0.18f + 0.5f * strength;
		float t = (now % 600000L) / 1000.0f;
		Matrix4f m = g.pose().last().pose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		// Each edge in turn: from (x0, y0) to (x1, y1) along the screen's border, the frost reaching inward along (nx, ny).
		edge(b, m, 0, 0, w, 0, 0, 1, depth, alpha, t, 0);
		edge(b, m, w, h, 0, h, 0, -1, depth, alpha, t, 1);
		edge(b, m, 0, h, 0, 0, 1, 0, depth, alpha, t, 2);
		edge(b, m, w, 0, w, h, -1, 0, depth, alpha, t, 3);
		BufferUploader.drawWithShader(b.end());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void edge(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float nx, float ny, float depth,
							 float alpha, float t, int side) {
		for (int i = 0; i < FROST_SEGMENTS; i++) {
			float u0 = i / (float) FROST_SEGMENTS, u1 = (i + 1) / (float) FROST_SEGMENTS;
			float ax = Mth.lerp(u0, x0, x1), ay = Mth.lerp(u0, y0, y1), bx = Mth.lerp(u1, x0, x1), by = Mth.lerp(u1, y0, y1);
			float d0 = depth * reach(u0, t, side), d1 = depth * reach(u1, t, side);
			b.vertex(m, ax, ay, 0).color(0.02f, 0.12f, 0.14f, alpha).endVertex();
			b.vertex(m, ax + nx * d0, ay + ny * d0, 0).color(0.05f, 0.25f, 0.28f, 0).endVertex();
			b.vertex(m, bx + nx * d1, by + ny * d1, 0).color(0.05f, 0.25f, 0.28f, 0).endVertex();
			b.vertex(m, bx, by, 0).color(0.02f, 0.12f, 0.14f, alpha).endVertex();
		}
	}

	/** How far in the frost reaches at {@code u} along an edge: ragged and slowly shifting, never past its depth. */
	private static float reach(float u, float t, int side) {
		float ragged = 0.72f + 0.16f * Mth.sin(u * 23.0f + t * 0.21f + side * 1.9f) + 0.12f * Mth.sin(u * 61.0f - t * 0.13f + side * 4.1f);
		return Mth.clamp(ragged, 0.35f, 1.0f);
	}

	private NetherworldHud() {}
}
