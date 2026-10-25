package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.SoulRiverCourse;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The dead on the Soul River: the souls of villagers and traders, beasts of the field, the hostile things of the night and
 * men and women, drifting downstream in the river beside the traveller, each in its own body's shape but made of cyan light
 * (the soul shader's "dead" palette, see SoulRenderer). Some drift slower than the current that carries the traveller and fall
 * behind; some pass them by. Each turns slowly as it goes, a few tumbling end over end. Purely the client's: every traveller
 * sees the same procession at the same moment.
 */
final class RiverSouls {
	/** What the dead were. Repeats weight the procession toward people. */
	private static final EntityType<?>[] KINDS = {
			EntityType.VILLAGER, EntityType.VILLAGER, EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ZOMBIE,
			EntityType.SKELETON, EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN, EntityType.WOLF, EntityType.FOX,
			EntityType.CAT, EntityType.HORSE, EntityType.IRON_GOLEM, EntityType.PIGLIN, EntityType.ENDERMAN, EntityType.CREEPER,
			EntityType.SPIDER, EntityType.WITCH, EntityType.PILLAGER, EntityType.LLAMA, EntityType.GOAT, EntityType.PANDA,
			EntityType.DROWNED, EntityType.HUSK, EntityType.RABBIT, EntityType.PARROT};
	/** This share of the souls are people (a player's shape in a default skin). */
	private static final float HUMAN_SHARE = 0.3f;
	private static final int SOULS = 48;
	/** The procession loops over the river and a stretch past each end, so it never runs dry. */
	private static final double LOOP_START = -60, LOOP = SoulRiverCourse.LENGTH + 120;
	/** Seen this far off along the river, fading out over the last part. */
	private static final double SEEN = 90;

	private static ClientLevel madeFor;
	private static final Map<EntityType<?>, LivingEntity> BODIES = new HashMap<>();
	private static final Set<EntityType<?>> BROKEN = new HashSet<>();
	private static PlayerModel<Player> wide, slim;

	private RiverSouls() {}

	/** Draws the souls near the camera; {@code flow} is the way the river runs for this viewer. */
	static void render(PoseStack pose, Vec3 cam, int flow, float time) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		ShaderInstance shader = SoulRenderer.shader();
		if (level == null || shader == null) return; // a shader pack: the river's own light still shows
		if (level != madeFor) {
			BODIES.clear();
			madeFor = level;
		}
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		for (int i = 0; i < SOULS; i++) {
			float u0 = hash(i, 0), u1 = hash(i, 1), u2 = hash(i, 2), u3 = hash(i, 3), u4 = hash(i, 4), u5 = hash(i, 5);
			// Downstream at its own pace: from just over half the current's to half again as fast.
			double pace = SoulRiverCourse.FLOW * (0.55 + 0.95 * u1);
			double x = LOOP_START + Mth.positiveModulo(u0 * LOOP + time * pace * flow, LOOP);
			double far = Math.abs(x - cam.x);
			if (far > SEEN) continue;
			float clarity = (float) (0.9 * (1 - Mth.clamp((far - SEEN * 0.6) / (SEEN * 0.4), 0, 1)));
			if (clarity < 0.03f) continue;
			double radius = SoulRiverCourse.radius(SoulRiverCourse.progress(x, flow > 0)) * (0.15 + 0.65 * u2);
			double angle = u3 * Mth.TWO_PI + time * 0.002 * (u4 - 0.5);
			Vec3 at = SoulRiverCourse.centre(x).add(0, radius * Math.sin(angle) + 0.25 * Math.sin(time * 0.04 + u5 * 9), radius * Math.cos(angle));
			float yaw = (flow > 0 ? -90 : 90) + 30 * Mth.sin(time * 0.01f + u5 * 20);
			float tumble = u4 < 0.15f ? time * 0.6f * (u5 < 0.5f ? 1 : -1) : 8 * Mth.sin(time * 0.03f + u0 * 10);
			try {
				if (u5 < HUMAN_SHARE) drawPerson(pose, cam, at, yaw, tumble, clarity, time + i * 37, i);
				else beast(pose, cam, at, yaw, tumble, clarity, time + i * 37, KINDS[(int) (u4 * 997) % KINDS.length], 1);
			} catch (Exception e) {
				DefyingTheHeavens.LOGGER.debug("A river soul couldn't be drawn", e);
			}
		}
		RenderSystem.disableCull();
	}

	/**
	 * One of the dead in a beast's (or a villager's, a monster's) own shape and the dead's cyan light, {@code scale} times its size, wading on
	 * as though in a current (and drawn so by the spells that call up the dead too: SpellGhosts). The soul shader must be there.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static void beast(PoseStack pose, Vec3 cam, Vec3 at, float yaw, float tumble, float clarity, float age, EntityType<?> kind, float scale) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || SoulRenderer.shader() == null || BROKEN.contains(kind)) return;
		if (level != madeFor) {
			BODIES.clear();
			madeFor = level;
		}
		LivingEntity body = BODIES.computeIfAbsent(kind, type -> type.create(level) instanceof LivingEntity living ? living : null);
		if (body == null) {
			BROKEN.add(kind);
			return;
		}
		EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(body);
		if (!(renderer instanceof LivingEntityRenderer<?, ?> living)) {
			BROKEN.add(kind);
			return;
		}
		EntityModel model = living.getModel();
		ResourceLocation texture = renderer.getTextureLocation(body);
		try {
			model.attackTime = 0;
			model.riding = false;
			model.young = false;
			// A slow wading step, as though walking on in the current.
			model.prepareMobModel(body, age * 0.12f, 0.3f, 0);
			model.setupAnim(body, age * 0.12f, 0.3f, age, 0, 8);
		} catch (Exception e) {
			BROKEN.add(kind);
			DefyingTheHeavens.LOGGER.debug("No river soul for {}", kind, e);
			return;
		}
		draw(pose, cam, at, yaw, tumble, clarity, body.getBbHeight(), texture, model, scale);
	}

	private static void drawPerson(PoseStack pose, Vec3 cam, Vec3 at, float yaw, float tumble, float clarity, float age, int seed) {
		UUID id = new UUID(0x5011L, seed * 0x9E3779B97F4A7C15L);
		boolean slimModel = "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
		PlayerModel<Player> model = personModel(slimModel);
		model.attackTime = 0;
		model.riding = false;
		model.young = false;
		model.crouching = false;
		model.head.xRot = 0.25f;
		model.head.yRot = 0.15f * Mth.sin(age * 0.02f);
		model.head.zRot = 0;
		model.body.xRot = 0;
		model.body.yRot = 0;
		SoulRenderer.ghostPose(model, age);
		draw(pose, cam, at, yaw, tumble, clarity, 1.8f, DefaultPlayerSkin.getDefaultSkin(id), model, 1);
	}

	private static PlayerModel<Player> personModel(boolean slimModel) {
		Minecraft mc = Minecraft.getInstance();
		if (slimModel) {
			if (slim == null) slim = new PlayerModel<>(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM), true);
			return slim;
		}
		if (wide == null) wide = new PlayerModel<>(mc.getEntityModels().bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
		return wide;
	}

	/** The body, then its glow added on a little larger, both in the dead's cyan (vertex green 0). */
	private static void draw(PoseStack pose, Vec3 cam, Vec3 at, float yaw, float tumble, float clarity, float height,
							 ResourceLocation texture, EntityModel<?> model, float scale) {
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y - height / 2, at.z - cam.z);
		if (scale != 1) pose.scale(scale, scale, scale);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.translate(0, height / 2, 0);
		pose.mulPose(Axis.XP.rotationDegrees(tumble));
		pose.translate(0, -height / 2, 0);
		RenderSystem.setShader(SoulRenderer::shader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.depthMask(true);
		pass(pose, model, 1, clarity, 1.0f, height);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		pass(pose, model, 0, clarity, 1.07f, height);
		pose.popPose();
	}

	private static void pass(PoseStack pose, EntityModel<?> model, float red, float clarity, float size, float height) {
		pose.pushPose();
		pose.translate(0, height / 2, 0);
		pose.scale(size, size, size);
		pose.translate(0, -height / 2, 0);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.translate(0.0f, -1.501f, 0.0f);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, 0, 1, clarity);
		BufferUploader.drawWithShader(buffer.end());
		pose.popPose();
	}

	/** A steady pseudo-random number in [0, 1) for soul {@code i} and which of its numbers. */
	private static float hash(int i, int which) {
		long h = i * 0x9E3779B97F4A7C15L + which * 0xC2B2AE3D27D4EB4FL + 0x5011L;
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		return (h >>> 40) / (float) (1L << 24);
	}
}
