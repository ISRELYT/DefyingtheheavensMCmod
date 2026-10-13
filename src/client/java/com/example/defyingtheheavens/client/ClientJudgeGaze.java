package com.example.defyingtheheavens.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * What the Chief Judge's gaze does to a traveller who meets it (see JudgeEye; the server sends how far it has gone): whispers
 * that come faster and louder, a heartbeat quickening, a curse at half and a shriek near the end; the view rolling and
 * breathing, a crimson dark closing in from the edges in time with the heartbeat, the frame tearing, and the Eye's own ring
 * burning into the middle of the sight.
 */
public final class ClientJudgeGaze {
	private static final RandomSource RANDOM = RandomSource.create();
	/** The gaze as the server last sent it, and as shown (eased toward it). */
	private static float gaze, shown;
	private static int heartbeatIn;
	private static boolean cursed, shrieked;

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> tick(client));
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> overlay(graphics));
	}

	static void set(float value) {
		gaze = Mth.clamp(value, 0, 1);
	}

	static void clear() {
		gaze = 0;
		shown = 0;
		cursed = false;
		shrieked = false;
	}

	/** The gaze as felt right now, 0 to 1. */
	public static float strength() {
		return shown;
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			clear();
			return;
		}
		shown += (gaze - shown) * 0.25f;
		if (shown < 0.005f) {
			shown = 0;
			cursed = false;
			shrieked = false;
			return;
		}
		if (mc.isPaused()) return;
		// Whispers, more and louder the deeper it goes.
		if (RANDOM.nextFloat() < 0.01f + 0.09f * shown) {
			SoundEvent whisper = RANDOM.nextBoolean() ? SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value() : SoundEvents.AMBIENT_CAVE.value();
			play(whisper, 0.55f + 0.35f * RANDOM.nextFloat(), 0.35f + 0.65f * shown);
		}
		// A heartbeat, from a slow thud to racing.
		if (shown > 0.25f && --heartbeatIn <= 0) {
			play(SoundEvents.WARDEN_HEARTBEAT, 1.0f, 0.5f + 0.5f * shown);
			heartbeatIn = Math.round(Mth.lerp((shown - 0.25f) / 0.75f, 24, 7));
		}
		if (!cursed && shown >= 0.5f) {
			cursed = true;
			play(SoundEvents.ELDER_GUARDIAN_CURSE, 0.55f, 0.9f);
		}
		if (!shrieked && shown >= 0.85f) {
			shrieked = true;
			play(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.5f, 1.0f);
			play(SoundEvents.WARDEN_NEARBY_CLOSEST, 0.6f, 1.0f);
		}
	}

	private static void play(SoundEvent sound, float pitch, float volume) {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	/** The view rolling and lurching (GameRendererMixin, after vanilla's own hurt shake). */
	public static void distort(PoseStack pose, float partialTicks) {
		if (shown <= 0) return;
		float t = (Util.getMillis() % 1_000_000L) / 50.0f;
		float s = shown * shown;
		pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(t * 0.11f) * 9 * s + Mth.sin(t * 0.73f) * 1.5f * s));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.07f + 1.3f) * 4 * s));
	}

	/** The view breathing in and out, as though the world were being drawn toward the Eye. */
	public static double fovScale() {
		if (shown <= 0) return 1;
		float t = (Util.getMillis() % 1_000_000L) / 50.0f;
		return 1 + shown * shown * (0.12 * Mth.sin(t * 0.09f) - 0.08);
	}

	/** The crimson dark closing in with the heartbeat, the frame tearing, and the Eye's ring burnt into the sight. */
	private static void overlay(GuiGraphics g) {
		if (shown <= 0.01f) return;
		int w = g.guiWidth(), h = g.guiHeight();
		long now = Util.getMillis();
		float beat = 0.5f + 0.5f * Mth.sin(now / 1000.0f * Mth.TWO_PI * Mth.lerp(shown, 0.8f, 2.6f));
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		Matrix4f m = g.pose().last().pose();
		BufferBuilder b = Tesselator.getInstance().getBuilder();
		b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		// The dark: from every edge, reaching further and deeper as the gaze goes on, pulsing with the heartbeat.
		float reach = Math.min(w, h) * (0.15f + 0.45f * shown) * (0.9f + 0.1f * beat);
		float alpha = Mth.clamp(0.25f + 0.65f * shown, 0, 0.92f) * (0.85f + 0.15f * beat);
		edge(b, m, 0, 0, w, 0, 0, 1, reach, alpha);
		edge(b, m, w, h, 0, h, 0, -1, reach, alpha);
		edge(b, m, 0, h, 0, 0, 1, 0, reach, alpha);
		edge(b, m, w, 0, w, h, -1, 0, reach, alpha);
		// The frame tearing: thin bars flickering across it.
		if (shown > 0.4f) {
			RandomSource tear = RandomSource.create(now / 70);
			int bars = Math.round((shown - 0.4f) * 14);
			for (int i = 0; i < bars; i++) {
				float y = tear.nextFloat() * h, thick = 1 + tear.nextFloat() * 3;
				boolean light = tear.nextBoolean();
				float a = (0.08f + 0.22f * tear.nextFloat()) * shown;
				quad(b, m, 0, y, w, y + thick, light ? 0.9f : 0.05f, light ? 0.2f : 0.0f, light ? 0.25f : 0.02f, a);
			}
		}
		BufferUploader.drawWithShader(b.end());

		// The Eye's ring, burnt into the middle of the sight: an amber-red iris and its slit, faint, then not so faint.
		if (shown > 0.3f) {
			float a = (shown - 0.3f) / 0.7f * 0.35f * (0.7f + 0.3f * beat);
			float cx = w / 2.0f, cy = h / 2.0f, r = Math.min(w, h) * 0.22f;
			RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
			b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			ring(b, m, cx, cy, r * 0.92f, r, 0.9f, 0.25f, 0.08f, a);
			ring(b, m, cx, cy, r * 0.55f, r * 0.92f, 0.95f, 0.55f, 0.15f, a * 0.4f);
			BufferUploader.drawWithShader(b.end());
			RenderSystem.defaultBlendFunc();
			b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			float slit = r * Mth.lerp(shown, 0.08f, 0.3f);
			quad(b, m, cx - slit, cy - r * 0.85f, cx + slit, cy + r * 0.85f, 0.0f, 0.0f, 0.0f, a * 1.6f);
			BufferUploader.drawWithShader(b.end());
		}
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
	}

	private static void edge(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float nx, float ny, float reach, float alpha) {
		b.vertex(m, x0, y0, 0).color(0.12f, 0.0f, 0.02f, alpha).endVertex();
		b.vertex(m, x0 + nx * reach, y0 + ny * reach, 0).color(0.05f, 0.0f, 0.02f, 0).endVertex();
		b.vertex(m, x1 + nx * reach, y1 + ny * reach, 0).color(0.05f, 0.0f, 0.02f, 0).endVertex();
		b.vertex(m, x1, y1, 0).color(0.12f, 0.0f, 0.02f, alpha).endVertex();
	}

	private static void quad(BufferBuilder b, Matrix4f m, float x0, float y0, float x1, float y1, float r, float g, float bl, float a) {
		b.vertex(m, x0, y0, 0).color(r, g, bl, a).endVertex();
		b.vertex(m, x0, y1, 0).color(r, g, bl, a).endVertex();
		b.vertex(m, x1, y1, 0).color(r, g, bl, a).endVertex();
		b.vertex(m, x1, y0, 0).color(r, g, bl, a).endVertex();
	}

	private static void ring(BufferBuilder b, Matrix4f m, float cx, float cy, float inner, float outer, float r, float g, float bl, float a) {
		int segments = 48;
		for (int i = 0; i < segments; i++) {
			float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
			b.vertex(m, cx + Mth.cos(a0) * inner, cy + Mth.sin(a0) * inner, 0).color(r, g, bl, 0).endVertex();
			b.vertex(m, cx + Mth.cos(a0) * outer, cy + Mth.sin(a0) * outer, 0).color(r, g, bl, a).endVertex();
			b.vertex(m, cx + Mth.cos(a1) * outer, cy + Mth.sin(a1) * outer, 0).color(r, g, bl, a).endVertex();
			b.vertex(m, cx + Mth.cos(a1) * inner, cy + Mth.sin(a1) * inner, 0).color(r, g, bl, 0).endVertex();
		}
	}

	private ClientJudgeGaze() {}
}
