package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.GhostSystem;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;

/**
 * The ghosts this client knows of (the server tells it, ModPackets#GHOST, as soon as it starts seeing one), for drawing them as
 * cyan light (SoulRenderer) and for the dead's rules on this side (GhostSystem#clientGhost). For the local player when they are
 * one: a soft cyan vignette at the edges of their sight, holding jump drifts them down slowly, and the screen's fades as they
 * disperse or the Wheel turns for them.
 */
public final class ClientGhosts {
	private static final Set<Integer> GHOSTS = new HashSet<>();
	/** The fade: its colour, how long to close, hold and open, and how far along it is. */
	private static int fadeRgb, fadeClose, fadeHold, fadeOpen, fadeTick = -1;
	private static float fadeAlpha, fadePrevious;
	/** A ghost holding jump falls no faster than this (blocks a tick). */
	private static final double DRIFT_DOWN = 0.08;

	public static void register() {
		GhostSystem.clientGhost = player -> GHOSTS.contains(player.getId());
		ClientTickEvents.END_CLIENT_TICK.register(ClientGhosts::tick);
		HudRenderCallback.EVENT.register(ClientGhosts::overlay);
	}

	static void set(int entityId, boolean ghost) {
		if (ghost) GHOSTS.add(entityId);
		else GHOSTS.remove(entityId);
	}

	static boolean any() {
		return !GHOSTS.isEmpty();
	}

	static void fade(int rgb, int close, int hold, int open) {
		fadeRgb = rgb;
		fadeClose = Math.max(1, close);
		fadeHold = Math.max(0, hold);
		fadeOpen = Math.max(1, open);
		fadeTick = 0;
	}

	static void clear() {
		GHOSTS.clear();
		fadeTick = -1;
		fadeAlpha = fadePrevious = 0;
	}

	/** The local player, a ghost holding jump: the fall slows to a drift (SoulRiverMixin's travel hook, after vanilla's). */
	public static void drift(Player player) {
		if (!GhostSystem.isGhost(player) || player.getAbilities().flying || player.onGround()) return;
		if (!(player instanceof net.minecraft.client.player.LocalPlayer local) || !local.input.jumping) return;
		var motion = player.getDeltaMovement();
		if (motion.y < -DRIFT_DOWN) player.setDeltaMovement(motion.x, -DRIFT_DOWN, motion.z);
		player.fallDistance = 0;
	}

	private static void tick(Minecraft mc) {
		fadePrevious = fadeAlpha;
		if (fadeTick < 0) {
			fadeAlpha = 0;
			return;
		}
		fadeTick++;
		if (fadeTick <= fadeClose) fadeAlpha = fadeTick / (float) fadeClose;
		else if (fadeTick <= fadeClose + fadeHold) fadeAlpha = 1;
		else if (fadeTick <= fadeClose + fadeHold + fadeOpen) fadeAlpha = 1 - (fadeTick - fadeClose - fadeHold) / (float) fadeOpen;
		else {
			fadeAlpha = 0;
			fadeTick = -1;
		}
	}

	/** The cyan at the edges of a ghost's sight, and the fade over everything. */
	private static void overlay(GuiGraphics g, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		int w = g.guiWidth(), h = g.guiHeight();
		if (GhostSystem.isGhost(mc.player) && !mc.options.hideGui) {
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			Matrix4f m = g.pose().last().pose();
			BufferBuilder b = Tesselator.getInstance().getBuilder();
			b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			float reach = Math.min(w, h) * 0.22f;
			float pulse = 0.85f + 0.15f * Mth.sin((mc.player.tickCount + tickDelta) * 0.05f);
			float a = 0.32f * pulse;
			edge(b, m, 0, 0, w, 0, 0, 1, reach, a);
			edge(b, m, w, h, 0, h, 0, -1, reach, a);
			edge(b, m, 0, h, 0, 0, 1, 0, reach, a);
			edge(b, m, w, 0, w, h, -1, 0, reach, a);
			BufferUploader.drawWithShader(b.end());
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
		}
		float alpha = Mth.lerp(tickDelta, fadePrevious, fadeAlpha);
		if (alpha > 0.003f) g.fill(0, 0, w, h, (Math.round(Mth.clamp(alpha, 0, 1) * 255) << 24) | fadeRgb);
	}

	private static void edge(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float nx, float ny, float reach, float alpha) {
		b.vertex(m, x0, y0, 0).color(0.25f, 0.85f, 0.92f, alpha).endVertex();
		b.vertex(m, x0 + nx * reach, y0 + ny * reach, 0).color(0.25f, 0.85f, 0.92f, 0).endVertex();
		b.vertex(m, x1 + nx * reach, y1 + ny * reach, 0).color(0.25f, 0.85f, 0.92f, 0).endVertex();
		b.vertex(m, x1, y1, 0).color(0.25f, 0.85f, 0.92f, alpha).endVertex();
	}

	private ClientGhosts() {}
}
