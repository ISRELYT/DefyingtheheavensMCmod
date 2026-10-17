package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.Ability;
import com.example.defyingtheheavens.ModDimensions;
import com.example.defyingtheheavens.SpatialTrialHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * What one flying the Spatial Gap freely (SpatialTrialHandler's Route TRAVEL: the client knows it by the Spatial Tear switched
 * on) sees of its layers, so the danger is plain before it is felt: the calm shallows, and the stormy layers above
 * (SpatialTrialHandler#UPPER_LAYERS_Y up to the crossing) and below (LOWER_LAYERS_Y down to the floor).
 * <ul>
 *   <li>The sky, the fog and the motes take the layer's colour (SpatialGapSkyRenderer, UpperRealmClient, SpatialGapAmbience):
 *   cold indigo and cyan in the shallows, a bruised violet-red in the upper layers, smouldering crimson in the lower; through
 *   the stormy layers the fog closes in.</li>
 *   <li>A veil of storm-light lies across each boundary ({@link #renderVeils}), a glowing ceiling and floor seen from the
 *   shallows, drifting as clouds do.</li>
 *   <li>In a stormy layer the screen's edges pulse with its colour, and a gauge at the left ({@link #renderHud}) shows how
 *   high one is between the floor and the crossing, the stormy layers marked.</li>
 * </ul>
 * Trials (carried up or down, below Heavenly Being) loop through a band of the gap and have no layers: nothing here shows for them.
 */
public final class GapLayers {
	/** Over this many blocks either side of a boundary the look turns from the shallows' to the layer's. */
	private static final float BLEND = 12;
	/** The veils reach this far round the camera, fading out toward their rim. */
	private static final float VEIL_RADIUS = 220;
	private static final int VEIL_RINGS = 10, VEIL_SEGMENTS = 40;

	private static final float[] UPPER = {0.92f, 0.28f, 0.78f}, LOWER = {1.0f, 0.38f, 0.14f};
	private static final float[] FOG_SHALLOW = {0.015f, 0.022f, 0.065f}, FOG_UPPER = {0.11f, 0.012f, 0.085f}, FOG_LOWER = {0.12f, 0.03f, 0.008f};
	private static final float[] SKY_UPPER = {0.26f, 0.035f, 0.19f}, SKY_LOWER = {0.30f, 0.075f, 0.02f};
	private static final int[] MOTES_SHALLOW = {0xC9A8FF, 0x8FE3FF, 0xF1EAFF, 0xA06BFF};
	private static final int[] MOTES_UPPER = {0xFF6FD8, 0xD14BFF, 0xFFD6F5, 0xFF3D8B};
	private static final int[] MOTES_LOWER = {0xFF8A3D, 0xFF4A2A, 0xFFD2A0, 0xC22A1A};

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(GapLayers::renderVeils);
		HudRenderCallback.EVENT.register(GapLayers::renderHud);
	}

	/** In the gap, flying it freely (one's own tear, or the rift or a fall for a Heavenly Being). */
	public static boolean travelling(Minecraft mc) {
		return mc.level != null && mc.player != null && ModDimensions.isSpatialGap(mc.level.dimension())
				&& ClientCultivationData.get().isAbilityEnabled(Ability.SPATIAL_TEAR);
	}

	/** -1 deep in the lower layers, 0 in the shallows, +1 deep in the upper layers, blended across each boundary. */
	public static float layerAt(double y) {
		float up = SpellRender.ease((float) (y - (SpatialTrialHandler.UPPER_LAYERS_Y - BLEND)) / (2 * BLEND));
		float down = SpellRender.ease((float) ((SpatialTrialHandler.LOWER_LAYERS_Y + BLEND) - y) / (2 * BLEND));
		return up - down;
	}

	/** {@link #layerAt} for the camera, or 0 for anyone not {@link #travelling}. */
	public static float layer(Minecraft mc) {
		if (!travelling(mc)) return 0;
		return layerAt(mc.gameRenderer.getMainCamera().getPosition().y);
	}

	private static float[] mix(float[] shallow, float[] upper, float[] lower, float layer) {
		float[] to = layer >= 0 ? upper : lower;
		float k = Math.abs(layer);
		return new float[] {Mth.lerp(k, shallow[0], to[0]), Mth.lerp(k, shallow[1], to[1]), Mth.lerp(k, shallow[2], to[2])};
	}

	/** The gap's fog colour where the camera is (its own near-black for a trial). */
	public static Vec3 fogColour(Vec3 trial) {
		Minecraft mc = Minecraft.getInstance();
		if (!travelling(mc)) return trial;
		float[] c = mix(FOG_SHALLOW, FOG_UPPER, FOG_LOWER, layer(mc));
		return new Vec3(c[0], c[1], c[2]);
	}

	/** The fog's start and end (fractions of the render distance): closing in through the stormy layers. */
	public static float[] fog(float[] trial) {
		Minecraft mc = Minecraft.getInstance();
		if (!travelling(mc)) return trial;
		float k = Math.abs(layer(mc));
		return new float[] {Mth.lerp(k, trial[0], 0.02f), Mth.lerp(k, trial[1], 0.6f)};
	}

	/** A colour of the sky's gradient, turned toward the stormy layer's where the camera is. */
	public static float[] sky(float[] shallow) {
		Minecraft mc = Minecraft.getInstance();
		if (!travelling(mc)) return shallow;
		float layer = layer(mc);
		float[] to = layer >= 0 ? SKY_UPPER : SKY_LOWER;
		float k = Math.abs(layer) * 0.8f;
		return new float[] {Mth.lerp(k, shallow[0], to[0]), Mth.lerp(k, shallow[1], to[1]), Mth.lerp(k, shallow[2], to[2])};
	}

	/** The motes' colours where the camera is: the shallows', or the nearer stormy layer's once well into it. */
	public static int[] moteColours(Minecraft mc) {
		float layer = layer(mc);
		if (Math.abs(layer) < 0.5f) return MOTES_SHALLOW;
		return layer > 0 ? MOTES_UPPER : MOTES_LOWER;
	}

	// --- The veils over the boundaries ---

	/** WorldRenderEvents.AFTER_TRANSLUCENT: a veil of storm-light across each stormy layer's boundary, round the camera. */
	public static void renderVeils(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (!travelling(mc)) return;
		Vec3 cam = context.camera().getPosition();
		float time = mc.level.getGameTime() + context.tickDelta();
		PoseStack pose = context.matrixStack();
		MultiBufferSource buffers = SpellBuffers.begin(context.consumers() == null ? mc.renderBuffers().bufferSource() : context.consumers());
		try {
			veil(buffers, pose, cam, SpatialTrialHandler.UPPER_LAYERS_Y, UPPER, time, 1);
			veil(buffers, pose, cam, SpatialTrialHandler.LOWER_LAYERS_Y, LOWER, time, -1);
		} finally {
			SpellBuffers.end();
		}
	}

	/**
	 * One veil at height {@code y}: two drifting sheets of storm cloud a few blocks apart, glowing in {@code rgb}, brightest near
	 * the camera and fading to nothing at their rim; fixed in the world (the texture by world position) though drawn round the camera.
	 */
	private static void veil(MultiBufferSource buffers, PoseStack pose, Vec3 cam, int y, float[] rgb, float time, int side) {
		double dy = y - cam.y;
		// Strongest from the shallows looking at it; still there (fainter) from inside its layer, so one sees where the calm begins.
		boolean fromShallows = side > 0 ? dy > 0 : dy < 0;
		float near = 1 - Mth.clamp((float) Math.abs(dy) / 260f, 0, 1);
		float strength = (fromShallows ? 0.30f : 0.16f) * (0.35f + 0.65f * near);
		if (strength <= 0.01f) return;
		VertexConsumer cloud = buffers.getBuffer(SpellRender.glow(SpellRender.CLOUD));
		VertexConsumer ripple = buffers.getBuffer(SpellRender.glow(SpellRender.RIPPLE));
		for (int sheet = 0; sheet < 2; sheet++) {
			float h = (float) dy + (sheet == 0 ? 0 : side * 3.5f);
			float drift = time * (sheet == 0 ? 0.0011f : -0.0007f);
			float scale = sheet == 0 ? 1 / 72f : 1 / 41f;
			float a = strength * (sheet == 0 ? 1 : 0.6f) * (0.85f + 0.15f * Mth.sin(time * 0.03f + sheet));
			disc(sheet == 0 ? cloud : ripple, pose.last(), cam, h, scale, drift, rgb, a);
		}
	}

	private static void disc(VertexConsumer vc, PoseStack.Pose pose, Vec3 cam, float h, float scale, float drift, float[] rgb, float alpha) {
		for (int i = 0; i < VEIL_RINGS; i++) {
			float r0 = VEIL_RADIUS * i / VEIL_RINGS, r1 = VEIL_RADIUS * (i + 1) / VEIL_RINGS;
			float a0 = alpha * (1 - (float) i / VEIL_RINGS), a1 = alpha * (1 - (float) (i + 1) / VEIL_RINGS);
			for (int j = 0; j < VEIL_SEGMENTS; j++) {
				double t0 = Math.PI * 2 * j / VEIL_SEGMENTS, t1 = Math.PI * 2 * (j + 1) / VEIL_SEGMENTS;
				corner(vc, pose, cam, r0, t0, h, scale, drift, rgb, a0);
				corner(vc, pose, cam, r1, t0, h, scale, drift, rgb, a1);
				corner(vc, pose, cam, r1, t1, h, scale, drift, rgb, a1);
				corner(vc, pose, cam, r0, t1, h, scale, drift, rgb, a0);
			}
		}
	}

	private static void corner(VertexConsumer vc, PoseStack.Pose pose, Vec3 cam, float r, double t, float h, float scale, float drift, float[] rgb, float a) {
		float x = (float) Math.cos(t) * r, z = (float) Math.sin(t) * r;
		float u = (float) ((cam.x + x) * scale) + drift, v = (float) ((cam.z + z) * scale) + drift * 0.6f;
		SpellRender.vertex(vc, pose, x, h, z, rgb[0], rgb[1], rgb[2], a, u, v, SpellRender.FULL_BRIGHT, 0, 1, 0);
	}

	// --- On screen ---

	/** HudRenderCallback: the stormy layer's pulse at the screen's edges, and the gauge of one's height in the gap. */
	public static void renderHud(GuiGraphics graphics, float tickDelta) {
		Minecraft mc = Minecraft.getInstance();
		if (!travelling(mc) || mc.options.hideGui) return;
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		float time = mc.level.getGameTime() + tickDelta;
		float layer = layer(mc);
		float k = Math.abs(layer);
		if (k > 0.02f) {
			int rgb = colour(layer > 0 ? UPPER : LOWER);
			float pulse = 0.8f + 0.2f * Mth.sin(time * 0.18f);
			int depth = Math.max(20, Math.min(width, height) / 7);
			for (int i = 0; i < 12; i++) {
				int alpha = (int) (110 * k * pulse * (1 - i / 12f) * (1 - i / 12f));
				if (alpha <= 0) continue;
				int argb = alpha << 24 | rgb;
				int step = depth * i / 12, next = depth * (i + 1) / 12;
				graphics.fill(0, step, width, next, argb);
				graphics.fill(0, height - next, width, height - step, argb);
				graphics.fill(step, next, next, height - next, argb);
				graphics.fill(width - next, next, width - step, height - next, argb);
			}
		}
		gauge(graphics, mc, height);
	}

	/**
	 * The gauge at the left: the gap from its floor (the mortal world below, green) to the crossing (the Upper Realm above, gold),
	 * the stormy layers in their colours and the shallows between, and a mark at one's height.
	 */
	private static void gauge(GuiGraphics graphics, Minecraft mc, int height) {
		int x0 = 6, x1 = 10, top = height / 2 - 70, bottom = height / 2 + 70;
		int floor = SpatialTrialHandler.FLOOR_Y, crossing = SpatialTrialHandler.CROSSING_Y;
		int lowerTop = at(SpatialTrialHandler.LOWER_LAYERS_Y, floor, crossing, top, bottom);
		int upperBottom = at(SpatialTrialHandler.UPPER_LAYERS_Y, floor, crossing, top, bottom);
		graphics.fill(x0 - 1, top - 2, x1 + 1, bottom + 2, 0x90000000);
		graphics.fill(x0, top, x1, upperBottom, 0xD0000000 | colour(UPPER));
		graphics.fill(x0, upperBottom, x1, lowerTop, 0xC05A6CFF);
		graphics.fill(x0, lowerTop, x1, bottom, 0xD0000000 | colour(LOWER));
		graphics.fill(x0 - 1, top - 2, x1 + 1, top, 0xFFFFD45A); // the Upper Realm
		graphics.fill(x0 - 1, bottom, x1 + 1, bottom + 2, 0xFF6FCB5A); // the mortal world
		int mark = Mth.clamp(at(mc.player.getY(), floor, crossing, top, bottom), top, bottom);
		graphics.fill(x0 - 2, mark - 1, x1 + 4, mark + 1, 0xFFFFFFFF);
		graphics.fill(x1 + 4, mark - 2, x1 + 5, mark + 2, 0xFFFFFFFF);
	}

	private static int at(double y, int floor, int crossing, int top, int bottom) {
		return (int) Math.round(bottom - (y - floor) / (crossing - floor) * (bottom - top));
	}

	private static int colour(float[] rgb) {
		return (int) (rgb[0] * 255) << 16 | (int) (rgb[1] * 255) << 8 | (int) (rgb[2] * 255);
	}

	private GapLayers() {}
}
