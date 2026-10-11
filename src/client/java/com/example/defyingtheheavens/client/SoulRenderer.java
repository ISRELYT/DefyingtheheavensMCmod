package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws projected nascent souls (see ClientSoul) and their tethers on the sense layer ({@link SenseOverlay}), so they keep their
 * colour under Qi Sense's grey: the soul in the player's own skin, but as violet light (shader {@code soul}, below), the colour
 * drifting through deeper and paler violets, brightest at its edges where it is seen side-on (a Fresnel glow) and fading toward
 * the middle; faint and grainy, a mist, to the Nascent Soul cultivators who see it hazily, clearer with every stage. It floats
 * upright, legs together and trailing like a ghost's, leaning into its flight. The tether is an elastic thread of bright blue
 * light from the body's chest to the soul, sagging and swaying, thinner and flickering as it is worn through.
 * <p>
 * Without the shader (a shader pack replacing core shaders), souls fall back to vanilla's translucent entity shader, tinted.
 */
public final class SoulRenderer {
	private static ShaderInstance soulShader;
	private static PlayerModel<Player> wide;
	private static PlayerModel<Player> slim;
	private static final int TETHER_SEGMENTS = 28;
	private static final float[] TETHER_CORE = {0.55f, 0.88f, 1.0f};
	private static final float[] TETHER_GLOW = {0.2f, 0.55f, 1.0f};

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(DefyingTheHeavens.id("soul"), DefaultVertexFormat.NEW_ENTITY, shader -> soulShader = shader));
	}

	/** Anything to draw this frame. */
	public static boolean hasWork() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && (ClientSoul.isOut() || !ClientSoul.seenSouls().isEmpty());
	}

	/** The souls and tethers, into the sense layer (its blending and depth already set, see SenseOverlay#onWorldEnd). */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		float partial = context.tickDelta();
		Camera camera = context.camera();
		List<Player> souls = new ArrayList<>();
		if (ClientSoul.isOut()) souls.add(mc.player);
		for (Integer id : ClientSoul.seenSouls().keySet()) {
			if (mc.level.getEntity(id) instanceof Player soul && soul != mc.player) souls.add(soul);
		}
		for (Player soul : souls) {
			// One's own soul is seen from inside in first person; another's inside the body it possesses is hidden by it.
			if (soul == mc.player && !camera.isDetached() || soul.isPassenger()) continue;
			float clarity = ClientSoul.clarity(soul);
			if (clarity > 0) drawSoul(context, soul, clarity, partial);
		}
		// Additive from here: light, not cover.
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		for (Player soul : souls) {
			boolean own = soul == mc.player;
			if (!own && !ClientSoul.seesTethers()) continue;
			ClientSoul.Seen seen = own ? null : ClientSoul.seenSoul(soul.getId());
			if (!own && seen == null) continue;
			Vec3[] ends = ClientSoul.tetherEnds(soul, seen, partial);
			drawTether(context, ends[0], ends[1], own ? ClientSoul.tether() : seen.tether(), soul.getId());
		}
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}

	// --- The soul ---

	private static void drawSoul(WorldRenderContext context, Player soul, float clarity, float partial) {
		Minecraft mc = Minecraft.getInstance();
		boolean slimModel = soul instanceof AbstractClientPlayer client && "slim".equals(client.getModelName());
		PlayerModel<Player> model = model(slimModel);
		if (model == null) return;
		float age = soul.tickCount + partial;
		float yaw = Mth.rotLerp(partial, soul.yHeadRotO, soul.yHeadRot);
		float pitch = Mth.lerp(partial, soul.xRotO, soul.getXRot());
		Vec3 cam = context.camera().getPosition();
		Vec3 at = soul.getPosition(partial);
		// It leans into its flight: up to 35 degrees at full pace.
		Vec3 motion = soul.position().subtract(soul.xo, soul.yo, soul.zo);
		double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
		float lean = (float) Math.min(35, horizontal * 60);

		PoseStack pose = context.matrixStack();
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y + Mth.sin(age * 0.08f) * 0.06f, at.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.mulPose(Axis.XP.rotationDegrees(-lean));

		model.attackTime = 0;
		model.riding = false;
		model.young = false;
		model.crouching = false;
		model.setupAnim(soul, 0, 0, age, 0, pitch);
		ghostPose(model, age);

		boolean shader = soulShader != null;
		RenderSystem.setShaderTexture(0, ((AbstractClientPlayer) soul).getSkinTextureLocation());
		if (shader) {
			RenderSystem.setShader(() -> soulShader);
			// The body, then its glow: the same, a little larger, added on (the shader draws only its edge light).
			drawModel(pose, model, 1, clarity, 1.0f);
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			drawModel(pose, model, 0, clarity, 1.07f);
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		} else {
			RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
			mc.gameRenderer.lightTexture().turnOnLightLayer();
			mc.gameRenderer.overlayTexture().setupOverlayColor();
			pose.pushPose();
			pose.scale(-1.0f, -1.0f, 1.0f);
			pose.scale(0.9375f, 0.9375f, 0.9375f);
			pose.translate(0.0f, -1.501f, 0.0f);
			BufferBuilder buffer = Tesselator.getInstance().getBuilder();
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
			model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.6f, 0.4f, 1.0f, 0.15f + 0.4f * clarity);
			BufferUploader.drawWithShader(buffer.end());
			pose.popPose();
			mc.gameRenderer.lightTexture().turnOffLightLayer();
			mc.gameRenderer.overlayTexture().teardownOverlayColor();
		}
		pose.popPose();
		if (clarity < 1) drawMist(context, soul, clarity, partial);
	}

	/**
	 * The model at the pose's origin (the soul's feet, facing set), scaled by {@code size} about its middle. {@code red} 1 is the
	 * body pass, 0 the halo pass; the clarity rides in the alpha. Full bright: the soul is its own light.
	 */
	private static void drawModel(PoseStack pose, PlayerModel<Player> model, float red, float clarity, float size) {
		pose.pushPose();
		pose.translate(0, 0.95f, 0);
		pose.scale(size, size, size);
		pose.translate(0, -0.95f, 0);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, 1, 1, clarity);
		BufferUploader.drawWithShader(buffer.end());
		pose.popPose();
	}

	/**
	 * To those who see it only hazily, the soul trails a drifting violet mist: soft puffs around it, more and brighter the
	 * hazier it is. Drawn here rather than as particles, which Qi Sense would grey.
	 */
	private static void drawMist(WorldRenderContext context, Player soul, float clarity, float partial) {
		Camera camera = context.camera();
		Vec3 cam = camera.getPosition();
		Vector3f left = camera.getLeftVector();
		Vector3f up = camera.getUpVector();
		Matrix4f matrix = context.matrixStack().last().pose();
		Vec3 at = soul.getPosition(partial);
		float age = soul.tickCount + partial;
		int puffs = 4 + Math.round(10 * (1 - clarity));
		float alpha = 0.06f + 0.22f * (1 - clarity);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		int segments = 10;
		for (int i = 0; i < puffs; i++) {
			// Each puff keeps its own slow orbit and bob around the soul, from a seed of the soul and the puff.
			float seed = Mth.frac(Mth.sin(soul.getId() * 12.9898f + i * 78.233f) * 43758.547f);
			float angle = seed * Mth.TWO_PI + age * (0.015f + 0.02f * (i % 3));
			float reach = 0.25f + 0.45f * Mth.frac(seed * 7.13f);
			float height = 0.15f + 1.7f * Mth.frac(seed * 3.71f + age * 0.004f);
			float size = 0.28f + 0.3f * Mth.frac(seed * 5.3f);
			float px = (float) (at.x - cam.x) + Mth.cos(angle) * reach;
			float py = (float) (at.y - cam.y) + height;
			float pz = (float) (at.z - cam.z) + Mth.sin(angle) * reach;
			for (int s = 0; s < segments; s++) {
				float a0 = s * Mth.TWO_PI / segments, a1 = (s + 1) * Mth.TWO_PI / segments;
				buffer.vertex(matrix, px, py, pz).color(0.55f, 0.3f, 1.0f, alpha).endVertex();
				buffer.vertex(matrix, px + (left.x() * Mth.cos(a0) + up.x() * Mth.sin(a0)) * size, py + (left.y() * Mth.cos(a0) + up.y() * Mth.sin(a0)) * size,
						pz + (left.z() * Mth.cos(a0) + up.z() * Mth.sin(a0)) * size).color(0.55f, 0.3f, 1.0f, 0f).endVertex();
				buffer.vertex(matrix, px + (left.x() * Mth.cos(a1) + up.x() * Mth.sin(a1)) * size, py + (left.y() * Mth.cos(a1) + up.y() * Mth.sin(a1)) * size,
						pz + (left.z() * Mth.cos(a1) + up.z() * Mth.sin(a1)) * size).color(0.55f, 0.3f, 1.0f, 0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}

	/** Upright and drifting: legs together and trailing behind like a ghost's, arms loose at its sides, gently swaying. */
	private static void ghostPose(PlayerModel<Player> model, float age) {
		float sway = Mth.sin(age * 0.09f);
		model.rightLeg.xRot = 0.6f + sway * 0.07f;
		model.leftLeg.xRot = 0.6f + sway * 0.07f;
		model.rightLeg.yRot = 0;
		model.leftLeg.yRot = 0;
		model.rightLeg.zRot = 0.035f;
		model.leftLeg.zRot = -0.035f;
		model.rightLeg.x = -1.2f;
		model.leftLeg.x = 1.2f;
		model.rightArm.xRot = 0.15f + Mth.sin(age * 0.07f) * 0.08f;
		model.leftArm.xRot = 0.15f - Mth.sin(age * 0.07f) * 0.08f;
		model.rightArm.yRot = 0;
		model.leftArm.yRot = 0;
		model.rightArm.zRot = 0.22f + sway * 0.04f;
		model.leftArm.zRot = -0.22f - sway * 0.04f;
		model.leftPants.copyFrom(model.leftLeg);
		model.rightPants.copyFrom(model.rightLeg);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.jacket.copyFrom(model.body);
		model.hat.copyFrom(model.head);
	}

	private static PlayerModel<Player> model(boolean slimModel) {
		Minecraft mc = Minecraft.getInstance();
		if (slimModel) {
			if (slim == null) slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
			return slim;
		}
		if (wide == null) wide = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
		return wide;
	}

	// --- The tether ---

	/**
	 * A camera-facing ribbon from {@code a} to {@code b}: a bright core in a wider glow, sagging under its own length and swaying,
	 * thinner (and flickering) as {@code strength} (0-1) is struck away.
	 */
	private static void drawTether(WorldRenderContext context, Vec3 a, Vec3 b, float strength, int seed) {
		Vec3 cam = context.camera().getPosition();
		Matrix4f matrix = context.matrixStack().last().pose();
		float time = (Minecraft.getInstance().level.getGameTime() + context.tickDelta()) * 0.08f;
		double length = a.distanceTo(b);
		double sag = Math.min(3.0, length * 0.06);
		Vec3 along = b.subtract(a).normalize();
		Vec3 side = along.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3[] points = new Vec3[TETHER_SEGMENTS + 1];
		for (int i = 0; i <= TETHER_SEGMENTS; i++) {
			double t = i / (double) TETHER_SEGMENTS;
			double hang = 4 * t * (1 - t);
			double wave = Math.sin(t * Math.PI * 3 + time + seed) * 0.12 * hang;
			points[i] = a.add(b.subtract(a).scale(t)).add(0, -sag * hang, 0).add(side.scale(wave)).subtract(cam);
		}
		float flicker = strength < 0.5f ? 0.6f + 0.4f * Mth.sin(time * 9) : 1;
		float width = 0.035f + 0.03f * strength;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		ribbon(buffer, matrix, points, width * 4, TETHER_GLOW, 0.35f * flicker);
		ribbon(buffer, matrix, points, width, TETHER_CORE, 0.95f * flicker);
		BufferUploader.drawWithShader(buffer.end());
	}

	/** A strip through {@code points} (camera-relative), {@code width} across, facing the camera, fading at its edges. */
	private static void ribbon(BufferBuilder buffer, Matrix4f matrix, Vec3[] points, float width, float[] rgb, float alpha) {
		for (int i = 0; i < points.length - 1; i++) {
			Vec3 p0 = points[i], p1 = points[i + 1];
			Vec3 dir = p1.subtract(p0);
			Vec3 mid = p0.add(p1).scale(0.5);
			Vec3 across = dir.cross(mid).normalize().scale(width);
			if (!Double.isFinite(across.x)) continue;
			Vector3f a0 = p0.add(across).toVector3f(), a1 = p0.subtract(across).toVector3f();
			Vector3f b0 = p1.add(across).toVector3f(), b1 = p1.subtract(across).toVector3f();
			// Two halves, each fading from the bright centre line to nothing at the edge.
			buffer.vertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
			buffer.vertex(matrix, a0.x, a0.y, a0.z).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
			buffer.vertex(matrix, b0.x, b0.y, b0.z).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
			buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
			buffer.vertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
			buffer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
			buffer.vertex(matrix, b1.x, b1.y, b1.z).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
			buffer.vertex(matrix, a1.x, a1.y, a1.z).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
		}
	}

	private SoulRenderer() {}
}
