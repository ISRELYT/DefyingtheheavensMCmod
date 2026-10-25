package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.SpellConstruct;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The ghost path's spells' dead, drawn as the mod's own ghosts are (SpellConstruct Form GHOSTS; the user's ask, 2026-10-25: "make ghost dash
 * my own players ghost not this random shitty model ... if ur adding stuff like ghosts why tf would u make these shitty ghost models? we
 * already have ghosts in the mod"): each a body in its own shape made of the soul shader's cyan light (SoulRenderer, RiverSouls), its edges
 * glowing, a halo added on; drawn on the sense layer with the rest of the dead (SenseOverlay), so Qi Sense's grey never dulls them. Their
 * spells' own:
 * <ul>
 *   <li>{@link SpellConstruct.Kind#SOUL_AFTERIMAGE}: a Soul Dash's trail of its caster's own ghost, lunging, left along its way and thinning;</li>
 *   <li>{@link SpellConstruct.Kind#WAIL_SPECTRE}: its caster's ghost rising giant over them and wailing, rings of its cry running out and the
 *       heads of the dead flung out on it;</li>
 *   <li>{@link SpellConstruct.Kind#GHOST_PROCESSION}: the dead pouring out along the look, men and women, villagers, beasts and the night's
 *       things, in a river of mist;</li>
 *   <li>{@link SpellConstruct.Kind#NETHER_DESCENT}: nine rings of ghost fire turning down into the Nine Nethers and the dead dragged in,
 *       spiralling down out of sight;</li>
 *   <li>{@link SpellConstruct.Kind#SOUL_ASCENT}: two strands of the dead winding up about one another round a column of mist, then plunging;</li>
 *   <li>{@link SpellConstruct.Kind#WHITE_WUCHANG}, {@link SpellConstruct.Kind#BLACK_WUCHANG}: the White and the Black Impermanence, tall in
 *       their hats, the White's tongue hanging and its staff of paper streamers, the Black's chain; and the soul they come for drawn out of its
 *       body toward them.</li>
 * </ul>
 * Without the soul shader (a shader pack replacing the core shaders) they are not drawn; their spells' mist and fire still show.
 */
final class SpellGhosts {
	private static ShaderInstance spiritShader;
	/** How far off they are drawn. */
	private static final double RANGE = 256;

	private SpellGhosts() {}

	static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(DefyingTheHeavens.id("spirit"), DefaultVertexFormat.NEW_ENTITY, shader -> spiritShader = shader));
	}

	/** Any of them to draw this frame. */
	static boolean hasWork() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof SpellConstruct construct && construct.kind().form == SpellConstruct.Form.GHOSTS) return true;
		}
		return false;
	}

	/** The colours of a spirit not of the dead's cyan (the spirit shader: SpellGhosts' own), and whether it is of smoke. */
	record Spirit(float[] core, float[] pale, float[] edge, boolean smoke) {}

	static final Spirit WHITE = new Spirit(new float[] {0.86f, 0.9f, 0.93f}, new float[] {1.0f, 1.0f, 1.0f}, new float[] {0.72f, 0.95f, 1.0f}, false);
	static final Spirit BLACK = new Spirit(new float[] {0.03f, 0.035f, 0.05f}, new float[] {0.1f, 0.12f, 0.17f}, new float[] {0.16f, 0.9f, 0.82f}, true);

	/** How a body is posed ({@code age}: ticks, for its sway), its parts already set to their rest. */
	interface Posing {
		void pose(PlayerModel<?> model, float age);
	}

	/** The sense layer's turn (SenseOverlay#onWorldEnd, its state set): every ghost spell's dead near the camera. */
	static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || SoulRenderer.shader() == null) return;
		float partial = context.tickDelta();
		Vec3 cam = context.camera().getPosition();
		PoseStack pose = context.matrixStack();
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof SpellConstruct c) || c.kind().form != SpellConstruct.Form.GHOSTS || c.isRemoved()) continue;
			if (c.distanceToSqr(cam) > RANGE * RANGE) continue;
			try {
				switch (c.kind()) {
					case SOUL_AFTERIMAGE -> afterimage(c, pose, cam, partial);
					case WAIL_SPECTRE -> spectre(c, pose, cam, partial);
					case GHOST_PROCESSION -> procession(c, pose, cam, partial);
					case NETHER_DESCENT -> descent(c, pose, cam, partial);
					case SOUL_ASCENT -> ascent(c, pose, cam, partial);
					case WHITE_WUCHANG, BLACK_WUCHANG -> wuchang(c, pose, cam, partial);
					case AVATAR_PROJECTION -> avatar(c, pose, cam, partial);
					case AVATAR_HAND -> avatarHand(c, pose, cam, partial);
					default -> {}
				}
			} catch (RuntimeException e) {
				DefyingTheHeavens.LOGGER.debug("A spell's dead couldn't be drawn", e);
			}
			SenseOverlay.layerState();
		}
		TRAILS.keySet().removeIf(id -> !(mc.level.getEntity(id) instanceof SpellConstruct));
	}

	// --- Drawing a body of the dead ---

	/** A construct's fading in and out (0 to 1) at {@code age}: over {@link SpellConstruct#FADE} each way. */
	private static float fade(SpellConstruct c, float age) {
		return Mth.clamp(Math.min(age / SpellConstruct.FADE, (c.life() - age) / SpellConstruct.FADE), 0, 1);
	}

	/** {@code e}'s skin and whether it wears the slim model: a player's own; any other's as its renderer draws it; a man's by default. */
	private static ResourceLocation skinOf(Entity e) {
		if (e instanceof AbstractClientPlayer player) return player.getSkinTextureLocation();
		if (e instanceof LivingEntity living) {
			try {
				EntityRenderer<? super LivingEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
				ResourceLocation texture = renderer.getTextureLocation(living);
				if (texture != null) return texture;
			} catch (RuntimeException ignored) {
				// whatever draws it can't say: a man's skin
			}
		}
		return DefaultPlayerSkin.getDefaultSkin();
	}

	private static boolean slimOf(Entity e) {
		return e instanceof AbstractClientPlayer player && "slim".equals(player.getModelName());
	}

	/** Every part of {@code model} back to its rest, shown. */
	private static void rest(PlayerModel<?> model) {
		for (ModelPart part : new ModelPart[] {model.head, model.hat, model.body, model.jacket, model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve,
				model.rightLeg, model.leftLeg, model.rightPants, model.leftPants}) {
			part.resetPose();
			part.visible = true;
		}
		model.attackTime = 0;
		model.riding = false;
		model.young = false;
		model.crouching = false;
	}

	/** The outer layers (hat, jacket, sleeves, trousers) laid over the parts as they were posed. */
	private static void layers(PlayerModel<?> model) {
		model.hat.copyFrom(model.head);
		model.jacket.copyFrom(model.body);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.leftPants.copyFrom(model.leftLeg);
		model.rightPants.copyFrom(model.rightLeg);
	}

	/**
	 * One of the dead in a man's shape: {@code skin} (slim or broad) at {@code feet}, facing {@code yaw}, leaning {@code lean} degrees forward
	 * about its middle (rolled {@code roll}), {@code scale} times a man's size, posed by {@code posing}, as clear as {@code alpha}; in the dead's
	 * cyan (the soul shader) or, with a {@code spirit}, in its colours. Returns the model as posed (for what it holds) and leaves {@code pose}
	 * as it found it; {@code after}, if not null, is drawn in the body's own space (its feet, scaled) before it is left.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void person(PoseStack pose, Vec3 cam, Vec3 feet, float yaw, float lean, float roll, float scale, ResourceLocation skin, boolean slim,
			Posing posing, float age, float alpha, Spirit spirit, java.util.function.BiConsumer<PoseStack, PlayerModel<?>> after) {
		if (alpha <= 0.01f) return;
		ShaderInstance shader = spirit != null && spiritShader != null ? spiritShader : SoulRenderer.shader();
		if (shader == null) return;
		PlayerModel model = SoulRenderer.model(slim);
		if (model == null) return;
		rest(model);
		posing.pose(model, age);
		layers(model);
		pose.pushPose();
		pose.translate(feet.x - cam.x, feet.y - cam.y, feet.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.scale(scale, scale, scale);
		if (lean != 0 || roll != 0) {
			pose.translate(0, 0.9, 0);
			if (lean != 0) pose.mulPose(Axis.XP.rotationDegrees(-lean));
			if (roll != 0) pose.mulPose(Axis.ZP.rotationDegrees(roll));
			pose.translate(0, -0.9, 0);
		}
		if (spirit != null && shader == spiritShader) {
			shader.safeGetUniform("Core").set(spirit.core()[0], spirit.core()[1], spirit.core()[2]);
			shader.safeGetUniform("Pale").set(spirit.pale()[0], spirit.pale()[1], spirit.pale()[2]);
			shader.safeGetUniform("Edge").set(spirit.edge()[0], spirit.edge()[1], spirit.edge()[2]);
			shader.safeGetUniform("Core2").set(spirit.core()[0], spirit.core()[1], spirit.core()[2]);
			shader.safeGetUniform("Pale2").set(spirit.pale()[0], spirit.pale()[1], spirit.pale()[2]);
			shader.safeGetUniform("Edge2").set(spirit.edge()[0], spirit.edge()[1], spirit.edge()[2]);
			shader.safeGetUniform("Smoke").set(spirit.smoke() ? 1.0f : 0.0f);
		}
		ShaderInstance chosen = shader;
		RenderSystem.setShader(() -> chosen);
		RenderSystem.setShaderTexture(0, skin);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		// The body (the dead's cyan: vertex green 0), then its glow added on a little larger (red 0: the halo pass).
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		pass(pose, model, 1, spirit == null ? 0 : 1, alpha, 1.0f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		pass(pose, model, 0, spirit == null ? 0 : 1, alpha, 1.07f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		if (after != null) after.accept(pose, model);
		pose.popPose();
	}

	/** One pass of a body at the pose's origin (its feet), {@code size} times as large about its middle; {@code red} 1 the body, 0 the halo. */
	private static void pass(PoseStack pose, PlayerModel<?> model, float red, float green, float alpha, float size) {
		pose.pushPose();
		pose.translate(0, 0.95f, 0);
		pose.scale(size, size, size);
		pose.translate(0, -0.95f, 0);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, 1, Mth.clamp(alpha, 0, 1));
		BufferUploader.drawWithShader(buffer.end());
		pose.popPose();
	}

	/** One of the dead in a beast's, a villager's or a monster's shape (RiverSouls#beast), its middle at {@code middle}. */
	private static void beast(PoseStack pose, Vec3 cam, Vec3 middle, float yaw, float tumble, float alpha, float age, EntityType<?> kind, float scale) {
		if (alpha <= 0.01f) return;
		RiverSouls.beast(pose, cam, middle, yaw, tumble, Mth.clamp(alpha, 0, 1), age, kind, scale);
		SenseOverlay.layerState();
	}

	/** A steady number in [0, 1) for {@code seed} and which of its numbers. */
	private static float hash(long seed, int which) {
		return SpellRender.hash(seed * 0x9E3779B97F4A7C15L + which * 0x632BE59BD9B4E019L + 0x5011L);
	}

	/** A man's default skin (one of the game's), steady for {@code seed}, and whether it is slim. */
	private static ResourceLocation defaultSkin(long seed) {
		return DefaultPlayerSkin.getDefaultSkin(new UUID(0x6057L, seed * 0x9E3779B97F4A7C15L));
	}

	private static boolean defaultSlim(long seed) {
		return "slim".equals(DefaultPlayerSkin.getSkinModelName(new UUID(0x6057L, seed * 0x9E3779B97F4A7C15L)));
	}

	/** The dead of other shapes that walk with men in a procession or fall into the dark: villagers, traders, beasts, the night's things. */
	private static final EntityType<?>[] KINDS = {EntityType.VILLAGER, EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ZOMBIE, EntityType.SKELETON,
			EntityType.PILLAGER, EntityType.WITCH, EntityType.HORSE, EntityType.WOLF, EntityType.FOX, EntityType.GOAT, EntityType.DROWNED, EntityType.HUSK};

	// --- Poses ---

	/** Drifting upright, legs trailing (the dead as they drift: SoulRenderer#ghostPose). */
	private static final Posing DRIFT = SoulRenderer::ghostPose;

	/** Lunging on the yin wind: arms swept back, legs trailing out behind. */
	private static final Posing LUNGE = (model, age) -> {
		model.rightArm.xRot = 0.95f;
		model.leftArm.xRot = 0.95f;
		model.rightArm.zRot = 0.35f;
		model.leftArm.zRot = -0.35f;
		model.rightLeg.xRot = 0.75f;
		model.leftLeg.xRot = 0.55f;
		model.head.xRot = -0.35f;
	};

	/** Reaching out before it, clawing a little (as a Hungry Ghost reaches). */
	private static final Posing REACH = (model, age) -> {
		SoulRenderer.ghostPose(model, age);
		float claw = Mth.sin(age * 0.17f) * 0.14f;
		model.rightArm.xRot = -1.4f + claw;
		model.leftArm.xRot = -1.4f - claw;
		model.rightArm.zRot = 0.1f;
		model.leftArm.zRot = -0.1f;
		model.head.xRot = 0.25f;
	};

	/** Flying on headlong: arms out ahead, reaching, legs trailing straight. */
	private static final Posing FLY = (model, age) -> {
		float sway = Mth.sin(age * 0.2f) * 0.1f;
		model.rightArm.xRot = -2.6f + sway;
		model.leftArm.xRot = -2.6f - sway;
		model.rightArm.zRot = 0.25f;
		model.leftArm.zRot = -0.25f;
		model.rightLeg.xRot = 0.15f + sway;
		model.leftLeg.xRot = 0.15f - sway;
		model.head.xRot = -0.9f;
	};

	/** Falling into the dark: arms thrown up over it, clawing at nothing, legs kicking. */
	private static final Posing FALLING = (model, age) -> {
		float flail = Mth.sin(age * 0.45f), kick = Mth.sin(age * 0.38f + 1.3f);
		model.rightArm.xRot = -2.7f + flail * 0.35f;
		model.leftArm.xRot = -2.7f - flail * 0.35f;
		model.rightArm.zRot = 0.45f + flail * 0.2f;
		model.leftArm.zRot = -0.45f + flail * 0.2f;
		model.rightLeg.xRot = 0.5f * kick;
		model.leftLeg.xRot = -0.5f * kick;
		model.head.xRot = -0.6f;
	};

	// --- Soul Dash: its caster's own ghost along its way ---

	/** Each trail's places, by its construct's id: when (its tick) and where its caster was (feet x, y, z). */
	private static final Map<Integer, List<float[]>> TRAILS = new HashMap<>();
	/** How many ticks a dash's ghosts are left along it, and how long each takes to thin away. */
	private static final int TRAIL_TICKS = 8;
	private static final float TRAIL_FADES = 12;

	/**
	 * A Soul Dash's trail (SOUL_AFTERIMAGE, the user's ask, 2026-10-25: "make ghost dash my own players ghost"): its caster's own ghost, in
	 * their own skin, left where they were each tick of the dash, lunging the way it went, the newest brightest, each thinning away on its own.
	 */
	private static void afterimage(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		Entity owner = c.owner();
		if (owner == null) return;
		List<float[]> trail = TRAILS.computeIfAbsent(c.getId(), k -> {
			List<float[]> first = new ArrayList<>();
			first.add(new float[] {0, (float) c.getX(), (float) c.getY(), (float) c.getZ()});
			return first;
		});
		float[] last = trail.get(trail.size() - 1);
		if (c.tickCount <= TRAIL_TICKS && c.tickCount > last[0]) {
			// Only where it has gone on from the last (a dash stopped short leaves no heap of them in one place).
			Vec3 now = owner.position();
			if (now.distanceToSqr(last[1], last[2], last[3]) > 0.5 * 0.5) trail.add(new float[] {c.tickCount, (float) now.x, (float) now.y, (float) now.z});
		}
		float age = c.tickCount + partial;
		ResourceLocation skin = skinOf(owner);
		boolean slim = slimOf(owner);
		for (int i = 0; i < trail.size(); i++) {
			float[] at = trail.get(i);
			float since = age - at[0];
			float alpha = (0.55f + 0.3f * i / Math.max(1, trail.size() - 1)) * Mth.clamp(1 - since / TRAIL_FADES, 0, 1);
			person(pose, cam, new Vec3(at[1], at[2], at[3]), c.getYRot(), 32, 0, 1.0f, skin, slim, LUNGE, age + i * 3, alpha, null, null);
		}
	}

	// --- Ghostly Wail: its caster's ghost risen giant over them ---

	/** When (ticks in) the wail's cry breaks out, how far its rings run (Spells#WAIL_RADIUS), how many heads of the dead it flings out. */
	private static final float WAIL_AT = 8, WAIL_REACH = 8;
	private static final int WAIL_HEADS = 10;

	/** The wailing ghost's pose: rising, its arms lifting wide and its head thrown back; then, the cry out, arms flung back and head thrust on. */
	private static Posing wailing(float age) {
		float rising = SpellRender.ease(age / WAIL_AT), crying = SpellRender.ease((age - WAIL_AT) / 3);
		return (model, a) -> {
			float shake = crying > 0 ? Mth.sin(a * 2.3f) * 0.04f : 0;
			model.rightArm.zRot = 0.2f + 1.9f * rising + 0.5f * crying;
			model.leftArm.zRot = -0.2f - 1.9f * rising - 0.5f * crying;
			model.rightArm.xRot = -0.2f * rising + 0.6f * crying + shake;
			model.leftArm.xRot = -0.2f * rising + 0.6f * crying - shake;
			model.head.xRot = -0.7f * rising + 1.0f * crying + shake;
			model.rightLeg.xRot = 0.55f;
			model.leftLeg.xRot = 0.55f;
			model.rightLeg.zRot = 0.08f * rising;
			model.leftLeg.zRot = -0.08f * rising;
		};
	}

	/**
	 * Ghostly Wail (WAIL_SPECTRE, held on its caster: SpellAnchor BODY): their own ghost rises out of them, swelling to near three times their
	 * height at their back, lifting its arms wide and throwing back its head; then it wails, leaning on over them, and its cry runs out in
	 * rings to the wail's reach, the heads of the dead flung out on it every way, crying.
	 */
	private static void spectre(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		Entity owner = c.owner();
		if (owner == null) return;
		float age = c.tickCount + partial, fade = fade(c, age);
		Vec3 feet = c.getPosition(partial).add(c.anchorShift(partial));
		float yaw = c.drawnYaw(partial);
		float rising = SpellRender.ease(age / WAIL_AT), crying = SpellRender.ease((age - WAIL_AT) / 3);
		float scale = 1.0f + 1.8f * rising;
		Vec3 back = com.example.defyingtheheavens.SpellAnchor.turned(new Vec3(0, -0.6 + 0.6 * rising, -0.9 * rising), yaw);
		person(pose, cam, feet.add(back), yaw, 18 * crying, 0, scale, skinOf(owner), slimOf(owner), wailing(age), age, 0.85f * fade, null, null);
		if (age < WAIL_AT) return;
		Vec3 head = feet.add(back).add(0, 1.6 * scale, 0);
		// Its cry's rings, running out to the wail's reach.
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			VertexConsumer rings = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
			for (int k = 0; k < 3; k++) {
				float t = (age - WAIL_AT - k * 4) / 14;
				if (t <= 0 || t >= 1) continue;
				float r = 1 + SpellRender.easeOut(t) * WAIL_REACH * 1.3f;
				pose.pushPose();
				pose.translate(head.x - cam.x, head.y - cam.y - 0.6 * t * scale, head.z - cam.z);
				SpellRender.disc(rings, pose.last(), r, 0, age * 0.05f + k, new float[] {0.55f, 1.0f, 0.95f}, 0.65f * (1 - t) * fade, SpellRender.FULL_BRIGHT);
				pose.mulPose(Axis.YP.rotationDegrees(-yaw));
				pose.mulPose(Axis.XP.rotationDegrees(90));
				SpellRender.disc(rings, pose.last(), r * 0.8f, 0, -age * 0.04f, new float[] {0.4f, 0.95f, 1.0f}, 0.4f * (1 - t) * fade, SpellRender.FULL_BRIGHT);
				pose.popPose();
			}
		} finally {
			SpellBuffers.end();
		}
		SenseOverlay.layerState();
		// The heads of the dead flung out on its cry, each crying out as it goes, every way round.
		for (int i = 0; i < WAIL_HEADS; i++) {
			float t = (age - WAIL_AT - i * 0.7f) / 16;
			if (t <= 0 || t >= 1) continue;
			double a = Math.PI * 2 * i / WAIL_HEADS + hash(c.getId(), i) * 0.5;
			float out = 1.2f + SpellRender.easeOut(t) * WAIL_REACH * 1.15f;
			Vec3 at = head.add(Math.cos(a) * out, (hash(c.getId(), i + 40) - 0.3) * 2.5 * t - 1.2, Math.sin(a) * out);
			float faceYaw = (float) Math.toDegrees(Math.atan2(-Math.cos(a), Math.sin(a)));
			person(pose, cam, at, faceYaw, -20, 0, 1.15f, defaultSkin(c.getId() * 31L + i), defaultSlim(c.getId() * 31L + i), (model, x) -> {
				for (ModelPart part : new ModelPart[] {model.body, model.jacket, model.rightArm, model.leftArm, model.rightSleeve, model.leftSleeve, model.rightLeg,
						model.leftLeg, model.rightPants, model.leftPants}) part.visible = false;
				model.head.xRot = -0.4f + Mth.sin(x * 0.6f) * 0.15f;
			}, age + i * 5, 0.8f * (1 - t) * fade, null, null);
		}
	}

	// --- Heaven-Demon Avatar ---

	/** Heaven's gold and the demon's crimson (the spirit shader's two sets of colours: its first and its Second). */
	private static final Spirit HEAVEN_GOLD = new Spirit(new float[] {1.0f, 0.74f, 0.22f}, new float[] {1.0f, 0.93f, 0.62f}, new float[] {1.0f, 0.86f, 0.4f}, false);
	private static final Spirit DEMON_CRIMSON = new Spirit(new float[] {0.7f, 0.04f, 0.1f}, new float[] {1.0f, 0.3f, 0.22f}, new float[] {1.0f, 0.18f, 0.12f}, false);
	/** How long its arm takes to rise and to come down as its hand falls (HeavenDemonSpells#AVATAR_FALL), and to go back after (ticks). */
	private static final float AVATAR_RAISE = 7, AVATAR_SWING = 14, AVATAR_BACK = 34;

	/**
	 * Heaven-Demon Avatar (AVATAR_PROJECTION; the user's ask, 2026-10-25, of its model: redo it): a colossal projection of its caster's own body
	 * (their own skin, its size their height in men) risen out of a cloud of gold and crimson behind them, waist up: its right side heaven's,
	 * gold, its left the demon's, crimson, a seam of fire burning down the middle of it where the two meet; a halo of gold behind its head, a
	 * horn of crimson fire on its demon side, flames on that shoulder. Its right hand held up before it in heaven's sign; its left low, clawed.
	 * As its spell sends a hand down ({@code strike_palm}, {@code strike_claw}) it raises that arm high and brings it down at where its caster
	 * looks, as the hand itself falls there out of the sky (AVATAR_HAND).
	 */
	private static void avatar(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		Entity owner = c.target() != null ? c.target() : c.owner();
		if (owner == null) return;
		float age = c.tickCount + partial, fade = fade(c, age);
		float scale = Math.max(1, c.size());
		float risen = SpellRender.easeOut(age / 24);
		Vec3 feet = c.getPosition(partial).add(0, -scale * 0.55 * (1 - risen), 0);
		float yaw = c.drawnYaw(partial);
		String move = c.phase();
		float moving = c.phaseAge(partial);
		Posing posing = (model, x) -> {
			for (ModelPart part : new ModelPart[] {model.rightLeg, model.leftLeg, model.rightPants, model.leftPants}) part.visible = false;
			model.head.xRot = 0.12f + Mth.sin(x * 0.04f) * 0.04f;
			// Heaven's sign: its right hand up before it; the demon's: its left low and out, its fingers hooked.
			model.rightArm.xRot = -1.75f + Mth.sin(x * 0.05f) * 0.05f;
			model.rightArm.yRot = 0.35f;
			model.leftArm.xRot = -0.55f + Mth.sin(x * 0.06f + 1) * 0.06f;
			model.leftArm.zRot = -0.45f;
			if (move != null) {
				ModelPart arm = "strike_palm".equals(move) ? model.rightArm : model.leftArm;
				float up = SpellRender.ease(moving / AVATAR_RAISE), down = SpellRender.ease((moving - AVATAR_RAISE) / (AVATAR_SWING - AVATAR_RAISE));
				float back = SpellRender.ease((moving - AVATAR_SWING - 6) / (AVATAR_BACK - AVATAR_SWING - 6));
				float raised = -3.0f * up + 2.25f * down;
				float rest = arm == model.rightArm ? -1.75f : -0.55f;
				arm.xRot = moving < AVATAR_SWING + 6 ? Mth.lerp(up, rest, raised) : Mth.lerp(back, raised, rest);
				arm.yRot = 0;
				arm.zRot = arm == model.rightArm ? 0.1f : -0.1f;
				model.body.xRot = 0.25f * down * (1 - back);
			}
		};
		personSides(pose, cam, feet, yaw, scale, skinOf(owner), slimOf(owner), posing, age, 0.9f * fade);
		// Its halo, its horn and its demon side's flames, and the cloud it rises out of.
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
			Vec3 head = feet.add(0, scale * 1.62, 0).add(com.example.defyingtheheavens.SpellAnchor.turned(new Vec3(0, 0, -scale * 0.18), yaw));
			Vec3 left = feet.add(0, scale * 1.38, 0).add(com.example.defyingtheheavens.SpellAnchor.turned(new Vec3(-scale * 0.36, 0, 0), yaw));
			pose.pushPose();
			pose.translate(head.x - cam.x, head.y - cam.y, head.z - cam.z);
			VertexConsumer ring = buffers.getBuffer(SpellRender.glow(SpellRender.RING));
			SpellRender.billboard(ring, pose, facing, 0, 0, 0, scale * 1.15f, age * 0.01f, HEAVEN_GOLD.edge(), 0.85f * fade, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			SpellRender.billboard(ring, pose, facing, 0, 0, 0, scale * 1.4f, -age * 0.008f, HEAVEN_GOLD.core(), 0.45f * fade, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
			SpellRender.billboard(glow, pose, facing, 0, 0, 0, scale * 2.2f, 0, HEAVEN_GOLD.pale(), 0.25f * fade, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			pose.popPose();
			VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
			// The horn on its demon side, and the fire leaping from that shoulder.
			Vec3 horn = feet.add(0, scale * 1.86, 0).add(com.example.defyingtheheavens.SpellAnchor.turned(new Vec3(-scale * 0.16, 0, 0), yaw));
			pose.pushPose();
			pose.translate(horn.x - cam.x, horn.y - cam.y, horn.z - cam.z);
			SpellRender.flameTongue(flames, pose, cam.subtract(horn), 0, 0, 0, scale * 0.16f, scale * 0.42f, age, DEMON_CRIMSON.edge(), 0.95f * fade, SpellRender.FLAME_FRAMES);
			pose.popPose();
			pose.pushPose();
			pose.translate(left.x - cam.x, left.y - cam.y, left.z - cam.z);
			for (int i = 0; i < 4; i++) {
				float sx = (hash(c.getId(), i) - 0.5f) * scale * 0.25f, sz = (hash(c.getId(), i + 9) - 0.5f) * scale * 0.25f;
				SpellRender.flameTongue(flames, pose, cam.subtract(left), sx, 0, sz, scale * 0.22f, scale * (0.35f + 0.15f * Mth.sin(age * 0.3f + i)), age + i * 4,
						DEMON_CRIMSON.pale(), 0.85f * fade, SpellRender.FLAME_FRAMES);
			}
			pose.popPose();
			// The cloud of the two it rises out of, at its waist.
			VertexConsumer cloud = buffers.getBuffer(SpellRender.clear(SpellRender.CLOUD));
			Vec3 waist = feet.add(0, scale * 0.72, 0);
			pose.pushPose();
			pose.translate(waist.x - cam.x, waist.y - cam.y, waist.z - cam.z);
			Vec3 rightWay = com.example.defyingtheheavens.SpellAnchor.turned(new Vec3(1, 0, 0), yaw);
			for (int i = 0; i < 14; i++) {
				double a = Math.PI * 2 * i / 14 + age * 0.004;
				float r = scale * (0.45f + 0.35f * hash(c.getId(), i + 20));
				// Gold on its side of heaven, crimson on the demon's.
				float[] rgb = Math.cos(a) * rightWay.x + Math.sin(a) * rightWay.z > 0 ? HEAVEN_GOLD.pale() : DEMON_CRIMSON.pale();
				SpellRender.billboard(cloud, pose, facing, (float) Math.cos(a) * r, (hash(c.getId(), i + 40) - 0.5f) * scale * 0.2f, (float) Math.sin(a) * r,
						scale * (0.7f + 0.3f * hash(c.getId(), i + 60)), age * 0.003f + i, rgb, 0.7f * fade * risen, 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			}
			pose.popPose();
		} finally {
			SpellBuffers.end();
		}
		SenseOverlay.layerState();
	}

	/**
	 * The Avatar's hand come down out of the sky (AVATAR_HAND, its value 1 heaven's palm, 2 the demon's claw; its size its height in men): its
	 * caster's own arm, as large as the Avatar's, of gold or of crimson, plunging down hand first onto where it lands, a column of its light
	 * above it; pressing ({@code press}) it sinks a little into what it struck and fades. The claw's fingers burn as claws of fire.
	 */
	private static void avatarHand(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		Entity owner = c.target() != null ? c.target() : c.owner();
		if (owner == null) return;
		boolean palm = c.value() < 1.5f;
		float age = c.tickCount + partial, fade = fade(c, age);
		float scale = Math.max(1, c.size());
		float fall = com.example.defyingtheheavens.HeavenDemonSpells.AVATAR_FALL;
		float f = Mth.clamp(age / fall, 0, 1);
		boolean pressing = "press".equals(c.phase());
		float sink = pressing ? Math.min(1, c.phaseAge(partial) / 6) * scale * 0.08f : 0;
		// Its hand's tip on the land as it lands: the arm's length (its model's twelve pixels) above that, and higher yet as it falls.
		double arm = scale * 12 / 16.0 * 0.9375;
		Vec3 ground = c.getPosition(partial);
		Vec3 shoulder = ground.add(0, arm + (1 - f * f) * scale * 4 - sink, 0);
		float yaw = c.drawnYaw(partial);
		Spirit spirit = palm ? HEAVEN_GOLD : DEMON_CRIMSON;
		armOf(pose, cam, shoulder, yaw, scale, skinOf(owner), slimOf(owner), palm, age, 0.95f * fade, spirit);
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			// A column of its light above it, fading as it comes.
			pose.pushPose();
			pose.translate(shoulder.x - cam.x, shoulder.y - cam.y, shoulder.z - cam.z);
			SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.BEAM)), pose.last(), scale * 3, h -> scale * 0.16f * (1 - h * 0.5f), 6, 16, spirit.edge(),
					0.45f * fade * (1 - f * 0.6f), 1, 2, 0, -age * 0.08f, 0, SpellRender.FULL_BRIGHT, h -> 1 - h);
			pose.popPose();
			if (!palm) {
				// Claws of fire at its fingers' ends.
				VertexConsumer flames = buffers.getBuffer(SpellRender.glow(SpellRender.FLAME));
				Vec3 tip = shoulder.subtract(0, arm, 0);
				pose.pushPose();
				pose.translate(tip.x - cam.x, tip.y - cam.y, tip.z - cam.z);
				for (int i = 0; i < 4; i++) {
					Vec3 off = com.example.defyingtheheavens.SpellAnchor.turned(new Vec3((i - 1.5) * scale * 0.06, 0, scale * 0.05), yaw);
					SpellRender.flameTongue(flames, pose, cam.subtract(tip), (float) off.x, -scale * 0.3f, (float) off.z, scale * 0.07f, scale * 0.32f, age + i * 3,
							DEMON_CRIMSON.edge(), fade, SpellRender.FLAME_FRAMES);
				}
				pose.popPose();
			}
		} finally {
			SpellBuffers.end();
		}
		SenseOverlay.layerState();
	}

	/** {@link #person}'s drawing in two sets of colours: heaven's gold on its right side, the demon's crimson on its left, a seam of fire between. */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void personSides(PoseStack pose, Vec3 cam, Vec3 feet, float yaw, float scale, ResourceLocation skin, boolean slim, Posing posing, float age, float alpha) {
		if (alpha <= 0.01f || spiritShader == null) return;
		PlayerModel model = SoulRenderer.model(slim);
		if (model == null) return;
		rest(model);
		posing.pose(model, age);
		layers(model);
		pose.pushPose();
		pose.translate(feet.x - cam.x, feet.y - cam.y, feet.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.scale(scale, scale, scale);
		spiritColours(HEAVEN_GOLD, DEMON_CRIMSON);
		ShaderInstance chosen = spiritShader;
		RenderSystem.setShader(() -> chosen);
		RenderSystem.setShaderTexture(0, skin);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		sides(pose, model, 1, alpha, 1.0f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		sides(pose, model, 0, alpha, 1.06f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		pose.popPose();
	}

	/** The spirit shader's two sets of colours made {@code first} and {@code second}. */
	private static void spiritColours(Spirit first, Spirit second) {
		ShaderInstance shader = spiritShader;
		shader.safeGetUniform("Core").set(first.core()[0], first.core()[1], first.core()[2]);
		shader.safeGetUniform("Pale").set(first.pale()[0], first.pale()[1], first.pale()[2]);
		shader.safeGetUniform("Edge").set(first.edge()[0], first.edge()[1], first.edge()[2]);
		shader.safeGetUniform("Core2").set(second.core()[0], second.core()[1], second.core()[2]);
		shader.safeGetUniform("Pale2").set(second.pale()[0], second.pale()[1], second.pale()[2]);
		shader.safeGetUniform("Edge2").set(second.edge()[0], second.edge()[1], second.edge()[2]);
		shader.safeGetUniform("Smoke").set(0.0f);
	}

	/**
	 * One pass of a body in its two sides ({@link #pass}'s, part by part): its right arm and leg wholly the first colours (vertex green 1), its left
	 * the second (0), its head mostly the first, its body half and half (the seam of fire burning down it).
	 */
	private static void sides(PoseStack pose, PlayerModel<?> model, float red, float alpha, float size) {
		pose.pushPose();
		pose.translate(0, 0.95f, 0);
		pose.scale(size, size, size);
		pose.translate(0, -0.95f, 0);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		float a = Mth.clamp(alpha, 0, 1);
		Object[][] parts = {{model.head, 0.72f}, {model.hat, 0.72f}, {model.body, 0.5f}, {model.jacket, 0.5f}, {model.rightArm, 1.0f}, {model.rightSleeve, 1.0f},
				{model.leftArm, 0.0f}, {model.leftSleeve, 0.0f}, {model.rightLeg, 1.0f}, {model.rightPants, 1.0f}, {model.leftLeg, 0.0f}, {model.leftPants, 0.0f}};
		for (Object[] p : parts) {
			ModelPart part = (ModelPart) p[0];
			if (!part.visible) continue;
			part.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, (Float) p[1], 1, a);
		}
		BufferUploader.drawWithShader(buffer.end());
		pose.popPose();
	}

	/**
	 * One arm of {@code skin}'s body, alone, hanging straight down from {@code shoulder} (heaven's right, or the demon's left), {@code scale}
	 * times a man's size, facing {@code yaw}, in {@code spirit}'s colours.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void armOf(PoseStack pose, Vec3 cam, Vec3 shoulder, float yaw, float scale, ResourceLocation skin, boolean slim, boolean right, float age, float alpha,
			Spirit spirit) {
		if (alpha <= 0.01f || spiritShader == null) return;
		PlayerModel model = SoulRenderer.model(slim);
		if (model == null) return;
		rest(model);
		for (ModelPart part : new ModelPart[] {model.head, model.hat, model.body, model.jacket, model.rightLeg, model.leftLeg, model.rightPants, model.leftPants,
				right ? model.leftArm : model.rightArm, right ? model.leftSleeve : model.rightSleeve}) part.visible = false;
		ModelPart arm = right ? model.rightArm : model.leftArm;
		arm.x = 0;
		arm.y = 0;
		arm.z = 0;
		arm.xRot = Mth.sin(age * 0.2f) * 0.03f;
		layers(model);
		pose.pushPose();
		pose.translate(shoulder.x - cam.x, shoulder.y - cam.y, shoulder.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.scale(scale, scale, scale);
		spiritColours(spirit, spirit);
		ShaderInstance chosen = spiritShader;
		RenderSystem.setShader(() -> chosen);
		RenderSystem.setShaderTexture(0, skin);
		RenderSystem.enableDepthTest();
		RenderSystem.disableCull();
		for (int halo = 0; halo < 2; halo++) {
			if (halo == 0) RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			else RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO,
					GlStateManager.DestFactor.ONE);
			pose.pushPose();
			float size = halo == 0 ? 1.0f : 1.08f;
			pose.scale(-size, -size, size);
			pose.scale(0.9375f, 0.9375f, 0.9375f);
			BufferBuilder buffer = Tesselator.getInstance().getBuilder();
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
			arm.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, halo == 0 ? 1 : 0, 1, 1, Mth.clamp(alpha, 0, 1));
			(right ? model.rightSleeve : model.leftSleeve).render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, halo == 0 ? 1 : 0, 1, 1, Mth.clamp(alpha, 0, 1));
			BufferUploader.drawWithShader(buffer.end());
			pose.popPose();
		}
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		pose.popPose();
	}

	// --- Procession of Ten Thousand Ghosts ---

	/** How many of the dead are drawn in the procession at once, how long its head takes to reach its end (GhostSpells#PROCESSION_TICKS). */
	private static final int PROCESSION_DEAD = 64, PROCESSION_REACHES = 40;

	/**
	 * Procession of Ten Thousand Ghosts (GHOST_PROCESSION: its reach its value, its width its size): the dead pour out of its caster along the
	 * look, each at its own pace and in its own lane of the column (men and women in their own skins, and villagers, traders, beasts and the
	 * night's things), drifting headlong or reaching ahead, some tumbling; its head runs out to its reach as the spell's does, and on past
	 * it they thin away. A river of mist and ghost fire runs with them, brightest at its head.
	 */
	private static void procession(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		float age = c.tickCount + partial, fade = fade(c, age);
		Vec3 from = c.getPosition(partial);
		float yaw = c.getYRot(), pitch = c.getXRot();
		Vec3 way = Vec3.directionFromRotation(pitch, yaw);
		Vec3 side = way.cross(new Vec3(0, 1, 0));
		side = side.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : side.normalize();
		Vec3 up = side.cross(way).normalize();
		float length = Math.max(4, c.value()), width = Math.max(2, c.size());
		float head = length * Math.min(1, age / PROCESSION_REACHES);
		long seed = c.getId();
		for (int k = 0; k < PROCESSION_DEAD; k++) {
			float u0 = hash(seed, k * 8), u1 = hash(seed, k * 8 + 1), u2 = hash(seed, k * 8 + 2), u3 = hash(seed, k * 8 + 3), u4 = hash(seed, k * 8 + 4);
			float emitted = k * (PROCESSION_REACHES * 1.25f / PROCESSION_DEAD) * (0.85f + 0.3f * u0);
			float pace = length / PROCESSION_REACHES * (1.05f + 0.6f * u1);
			float d = (age - emitted) * pace;
			if (d < 0 || d > length + 8) continue;
			d = Math.min(d, head + 1.5f * u2);
			double lane = width * 0.5 * Math.sqrt(u3), around = u4 * Math.PI * 2;
			Vec3 middle = from.add(way.scale(d)).add(side.scale(Math.cos(around) * lane)).add(up.scale(Math.sin(around) * lane * 0.75));
			float alpha = 0.85f * fade * Mth.clamp(d / 2.5f, 0, 1) * Mth.clamp(1 - (d - length) / 8, 0, 1);
			float sway = Mth.sin(age * 0.11f + k) * 8;
			float a = age + k * 17;
			if (hash(seed, k * 8 + 5) < 0.7f) {
				Posing posing = hash(seed, k * 8 + 6) < 0.5f ? REACH : FLY;
				float lean = (posing == FLY ? 62 : 32) - pitch;
				person(pose, cam, middle.subtract(0, 0.9, 0), yaw + sway, lean, 0, 1.0f, defaultSkin(seed * 131 + k), defaultSlim(seed * 131 + k), posing, a, alpha, null, null);
			} else {
				float tumble = hash(seed, k * 8 + 7) < 0.25f ? age * 9 : 15 * Mth.sin(age * 0.07f + k);
				beast(pose, cam, middle, yaw + sway, tumble, alpha, a, KINDS[(int) (u2 * 997) % KINDS.length], 1.0f);
			}
		}
		// The river of mist and ghost fire it pours out in, brightest at its head.
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		Quaternionf facing = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
		try {
			VertexConsumer mist = buffers.getBuffer(SpellRender.glow(SpellRender.MIST));
			VertexConsumer glow = buffers.getBuffer(SpellRender.glow(SpellRender.GLOW));
			VertexConsumer sparks = buffers.getBuffer(SpellRender.glow(SpellRender.SPARK));
			for (float d = 1.5f; d < Math.min(head, length + 4); d += 2.2f) {
				float shimmer = 0.5f + 0.5f * Mth.sin(age * 0.2f + d);
				Vec3 at = from.add(way.scale(d)).subtract(cam);
				float a = 0.09f * fade * (0.7f + 0.3f * shimmer) * Mth.clamp(1 - (d - length) / 4, 0, 1);
				SpellRender.billboard(mist, pose, facing, (float) at.x, (float) at.y, (float) at.z, width * 1.4f, age * 0.01f + d, new float[] {0.35f, 0.95f, 0.9f}, a, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
			Vec3 front = from.add(way.scale(Math.min(head, length))).subtract(cam);
			SpellRender.billboard(glow, pose, facing, (float) front.x, (float) front.y, (float) front.z, width * 1.6f, 0, new float[] {0.4f, 1.0f, 0.95f},
					0.35f * fade * (age < PROCESSION_REACHES ? 1 : 0.5f), 0, 0, 1, 1, SpellRender.FULL_BRIGHT);
			for (int i = 0; i < 40; i++) {
				float t = (age * 0.03f + hash(seed, 900 + i)) % 1;
				float d = t * Math.min(head, length);
				double around = hash(seed, 950 + i) * Math.PI * 2, lane = width * 0.55 * hash(seed, 990 + i);
				Vec3 at = from.add(way.scale(d)).add(side.scale(Math.cos(around) * lane)).add(up.scale(Math.sin(around) * lane)).subtract(cam);
				SpellRender.billboard(sparks, pose, facing, (float) at.x, (float) at.y, (float) at.z, 0.35f, 0, new float[] {0.6f, 1.0f, 0.95f}, 0.8f * fade, 0, 0, 1, 1,
						SpellRender.FULL_BRIGHT);
			}
		} finally {
			SpellBuffers.end();
		}
	}

	// --- Rift to the Nine Nethers ---

	/** Minecraft's own soul fire (the Nether's ghost fire): its strip of 32 frames. */
	private static final ResourceLocation SOUL_FIRE = new ResourceLocation("minecraft", "textures/block/soul_fire_0.png");
	private static final int SOUL_FIRE_FRAMES = 32;
	/** How many of the dead are dragged down at once, and how long each takes from the rim to the depths (ticks). */
	private static final int FALLEN = 30;
	private static final float FALL_TIME = 46;

	/**
	 * Rift to the Nine Nethers (NETHER_DESCENT, at the rift's heart: its size the rift's round, its value how far round it drags): nine rings
	 * of ghost fire turning down into the abyss, each narrower and deeper, turning against the next; and the dead dragged in from round about,
	 * spiralling in and down, clawing at the air, shrinking as they go down out of sight (with a beast or a villager among them). Sealing
	 * ({@code seal}), the rings close in on its heart and the last of the dead go down.
	 */
	private static void descent(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		float age = c.tickCount + partial, fade = fade(c, age);
		Vec3 heart = c.getPosition(partial);
		float rift = Math.max(2, c.size()), pull = Math.max(rift, c.value());
		float open = SpellRender.ease(age / 16);
		boolean sealing = "seal".equals(c.phase());
		float sealed = sealing ? SpellRender.ease(c.phaseAge(partial) / 12) : 0;
		long seed = c.getId();
		// The dead, spiralling in and down.
		for (int k = 0; k < FALLEN; k++) {
			float s = ((age / FALL_TIME) + k / (float) FALLEN) % 1;
			// None begin their fall once it is sealing: each goes on down, the rest unseen.
			float began = age - s * FALL_TIME;
			if (sealing && began > c.tickCount - c.phaseAge(partial)) continue;
			float u = hash(seed, k * 5), v = hash(seed, k * 5 + 1);
			double turn = Math.PI * 2 * hash(seed, k * 5 + 2) + s * Math.PI * 3.2 * (k % 2 == 0 ? 1 : -1);
			double radius = pull * (0.55 + 0.45 * u) * Math.pow(1 - s, 1.4);
			double height = (1 - s) * (1.2 + 2.8 * v) - Math.max(0, s - 0.62) * 9;
			Vec3 middle = heart.add(Math.cos(turn) * radius, height + 0.9, Math.sin(turn) * radius);
			float scale = (1.0f - 0.6f * s) * (1.0f + 0.35f * v);
			float alpha = 0.9f * fade * open * Mth.clamp(s / 0.08f, 0, 1) * Mth.clamp((1 - s) / 0.12f, 0, 1);
			float faceYaw = (float) Math.toDegrees(Math.atan2(Math.sin(turn), Math.cos(turn))) - 90;
			if (hash(seed, k * 5 + 3) < 0.78f) {
				person(pose, cam, middle.subtract(0, 0.9 * scale, 0), faceYaw, 25 + 45 * s, 20 * Mth.sin(age * 0.2f + k), scale, defaultSkin(seed * 37 + k),
						defaultSlim(seed * 37 + k), FALLING, age + k * 11, alpha, null, null);
			} else {
				beast(pose, cam, middle, faceYaw, age * 6 + k * 40, alpha, age + k * 11, KINDS[(int) (u * 997) % KINDS.length], scale);
			}
		}
		// The nine rings of ghost fire, turning down into it.
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			VertexConsumer fire = buffers.getBuffer(SpellRender.glow(SOUL_FIRE));
			Vec3 eye = cam.subtract(heart);
			pose.pushPose();
			pose.translate(heart.x - cam.x, heart.y - cam.y, heart.z - cam.z);
			for (int j = 0; j < 9; j++) {
				float r = rift * 0.5f * (1 - j * 0.1f) * open * (1 - sealed);
				if (r < 0.2f) continue;
				float y = 0.08f - j * 0.16f;
				int count = Math.max(8, (int) (Math.PI * 2 * r / 0.9f));
				float spin = age * (0.025f + 0.006f * j) * (j % 2 == 0 ? 1 : -1);
				for (int i = 0; i < count; i++) {
					double a = Math.PI * 2 * i / count + spin;
					float x = (float) (Math.cos(a) * r), z = (float) (Math.sin(a) * r);
					float tall = (1.1f - j * 0.07f) * (0.8f + 0.3f * Mth.sin(age * 0.4f + i * 1.7f + j));
					SpellRender.flameTongue(fire, pose, eye, x, y, z, 0.8f - j * 0.04f, tall, age + i * 3 + j * 7, new float[] {1, 1, 1}, 0.9f * fade, SOUL_FIRE_FRAMES);
				}
			}
			pose.popPose();
		} finally {
			SpellBuffers.end();
		}
	}

	// --- Ten Thousand Souls Ascend ---

	/** How many of the dead wind up each strand, and how many wheel loose round it; how long one takes from the ground to its top (ticks). */
	private static final int STRAND = 26, LOOSE = 16;
	private static final float CLIMB = 90, RISES = 24;

	/**
	 * Ten Thousand Souls Ascend (SOUL_ASCENT: its round its size, its height its value): two strands of the dead winding up about one another,
	 * each soul rising and wheeling round as it goes, headlong, reaching up; more wheeling loose about them; a column of the dead's mist at its
	 * heart, ghost fire round its foot. It grows up out of the ground as it rises; going back down ({@code descend}), every soul in it plunges
	 * down into the ground, the column with them.
	 */
	private static void ascent(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		float age = c.tickCount + partial, fade = fade(c, age);
		Vec3 base = c.getPosition(partial);
		float radius = Math.max(2, c.size()), height = Math.max(8, c.value());
		float grown = SpellRender.ease(age / RISES);
		boolean down = "descend".equals(c.phase());
		float plunge = down ? c.phaseAge(partial) : 0;
		long seed = c.getId();
		for (int k = 0; k < STRAND * 2 + LOOSE; k++) {
			boolean loose = k >= STRAND * 2;
			int strand = k % 2;
			float u = ((age / CLIMB) + (k / 2) / (float) STRAND + hash(seed, k) * 0.03f) % 1;
			double turn, r;
			if (loose) {
				u = ((age / (CLIMB * 1.6f)) + hash(seed, k * 3) ) % 1;
				turn = Math.PI * 2 * hash(seed, k * 3 + 1) + age * 0.035;
				r = radius * (1.4 + 0.6 * hash(seed, k * 3 + 2));
			} else {
				turn = strand * Math.PI + u * Math.PI * 5 + age * 0.05;
				r = radius * (0.7 + 0.5 * u);
			}
			double h = u * height * grown - plunge * plunge * 0.35 - plunge * 1.2;
			Vec3 middle = base.add(Math.cos(turn) * r, h + 1, Math.sin(turn) * r);
			float alpha = 0.9f * fade * Mth.clamp(u / 0.06f, 0, 1) * Mth.clamp((1 - u) / 0.12f, 0, 1) * (down ? Mth.clamp(1 - plunge / 14, 0, 1) : 1);
			if (h < -1.5) continue;
			// Facing along its way round (and up), headlong.
			float faceYaw = (float) Math.toDegrees(Math.atan2(Math.sin(turn), Math.cos(turn))) + (loose ? 90 : -90);
			float scale = 1.5f + 0.5f * hash(seed, k * 7);
			person(pose, cam, middle.subtract(0, 0.9 * scale, 0), faceYaw, down ? 150 : 48, 0, scale, defaultSkin(seed * 53 + k), defaultSlim(seed * 53 + k), FLY,
					age + k * 13, alpha, null, null);
		}
		// Its column of mist, its foot's ring of ghost fire.
		MultiBufferSource.BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		MultiBufferSource buffers = SpellBuffers.begin(source);
		try {
			pose.pushPose();
			pose.translate(base.x - cam.x, base.y - cam.y - (down ? plunge * 1.5 : 0), base.z - cam.z);
			float column = height * grown * (down ? Mth.clamp(1 - plunge / 12, 0, 1) : 1);
			if (column > 0.5f) {
				SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.MIST)), pose.last(), column, t -> radius * (0.4f + 0.25f * t), 8, 16,
						new float[] {0.35f, 0.95f, 0.9f}, 0.22f * fade, 2, 3, age * 0.01f, -age * 0.02f, 1.6f, SpellRender.FULL_BRIGHT, t -> Mth.clamp((1 - t) * 3, 0, 1) * Mth.clamp(t * 6, 0, 1));
				SpellRender.cylinder(buffers.getBuffer(SpellRender.glow(SpellRender.SMOKE)), pose.last(), column * 0.9f, t -> radius * 0.22f, 6, 12,
						new float[] {0.6f, 1.0f, 0.95f}, 0.25f * fade, 1, 2, 0, -age * 0.04f, -2.2f, SpellRender.FULL_BRIGHT, t -> Mth.clamp((1 - t) * 2, 0, 1));
			}
			VertexConsumer fire = buffers.getBuffer(SpellRender.glow(SOUL_FIRE));
			Vec3 eye = cam.subtract(base);
			int count = (int) (Math.PI * 2 * radius * 1.2 / 1.1);
			for (int i = 0; i < count; i++) {
				double a = Math.PI * 2 * i / count + age * 0.02;
				float rr = radius * 1.2f * grown;
				float tall = 1.4f * (0.75f + 0.3f * Mth.sin(age * 0.35f + i * 1.9f));
				SpellRender.flameTongue(fire, pose, eye, (float) Math.cos(a) * rr, 0, (float) Math.sin(a) * rr, 1.0f, tall, age + i * 3, new float[] {1, 1, 1},
						0.85f * fade * (down ? Mth.clamp(1 - plunge / 10, 0, 1) : 1), SOUL_FIRE_FRAMES);
			}
			pose.popPose();
		} finally {
			SpellBuffers.end();
		}
	}

	// --- Black and White Impermanence ---

	/** How long they take to rise out of the mist, how long the White's swing and the Black's lash take, how long they draw the soul out (ticks). */
	private static final float WUCHANG_RISES = 20, WUCHANG_STRIKES = 8, WUCHANG_TAKES = 20;
	private static final float[] HAT_WHITE = {0.93f, 0.93f, 0.9f}, HAT_BLACK = {0.06f, 0.06f, 0.08f}, BAND_RED = {0.75f, 0.08f, 0.1f},
			BAND_TEAL = {0.18f, 0.95f, 0.85f}, TONGUE = {0.85f, 0.12f, 0.16f}, STAFF = {0.32f, 0.24f, 0.18f}, PAPER = {0.97f, 0.97f, 0.94f};

	/**
	 * The White or the Black Impermanence (WHITE_WUCHANG, BLACK_WUCHANG: its size its height in men, its target the soul it comes for): a tall
	 * ghost in the White's bone-white light or the Black's ink smoke, edged in the dead's teal, in the tall hat of its office (white banded red,
	 * black banded teal), rising out of the mist beside its quarry. The White's long red tongue hangs to its chest and it carries the staff of
	 * paper streamers that calls the dead; its blow ({@code strike}) is that staff swung down from high over its head. The Black's is its arm
	 * thrust out at its quarry as its chain lashes (a chain of hell: the spell's own). Then both reach for the soul and draw it ({@code take}):
	 * the soul itself is pulled half out of its body toward them, in the dead's light, and torn away.
	 */
	private static void wuchang(SpellConstruct c, PoseStack pose, Vec3 cam, float partial) {
		boolean white = c.kind() == SpellConstruct.Kind.WHITE_WUCHANG;
		float age = c.tickCount + partial, fade = fade(c, age);
		float scale = Math.max(1.5f, c.size());
		float risen = SpellRender.ease(age / WUCHANG_RISES);
		Vec3 feet = c.getPosition(partial).subtract(0, (1 - risen) * 2.1 * scale, 0);
		float yaw = c.drawnYaw(partial);
		String phase = c.phase();
		float struck = "strike".equals(phase) ? SpellRender.ease(c.phaseAge(partial) / WUCHANG_STRIKES) : "take".equals(phase) ? 1 : 0;
		float taking = "take".equals(phase) ? SpellRender.ease(c.phaseAge(partial) / WUCHANG_TAKES) : 0;
		Posing posing = (model, a) -> {
			SoulRenderer.ghostPose(model, a);
			float sway = Mth.sin(a * 0.08f) * 0.05f;
			if (white) {
				// The staff raised high, then swung down at its quarry.
				float raise = Mth.clamp((age - WUCHANG_RISES + 14) / 10, 0, 1);
				model.rightArm.xRot = Mth.lerp(struck, -2.9f * raise, -0.85f) + sway;
				model.rightArm.zRot = 0.15f;
				model.leftArm.xRot = -0.3f + sway;
			} else {
				// The arm thrust out at its quarry as the chain lashes.
				model.rightArm.xRot = Mth.lerp(struck, -0.25f, -1.55f) + sway;
				model.rightArm.zRot = Mth.lerp(struck, 0.15f, 0.0f);
				model.leftArm.xRot = -0.35f - sway;
			}
			if (taking > 0) {
				// Both reach for the soul and draw it back toward them.
				float pull = Mth.sin(Math.min(1, taking) * Mth.PI * 0.5f);
				model.leftArm.xRot = Mth.lerp(pull, model.leftArm.xRot, -1.7f);
				model.rightArm.xRot = Mth.lerp(pull, model.rightArm.xRot, -1.5f);
				model.leftArm.zRot = -0.25f;
				model.head.xRot = 0.3f;
			}
			model.head.yRot = Mth.sin(a * 0.05f) * 0.08f;
		};
		person(pose, cam, feet, yaw, -6 * taking + 4, 0, scale, DefaultPlayerSkin.getDefaultSkin(), false, posing, age, 0.95f * fade * risen, white ? WHITE : BLACK,
				(p, model) -> regalia(p, model, white, age, fade * risen));
		// The soul drawn out of its body toward them.
		Entity target = c.target();
		if (taking > 0 && target instanceof LivingEntity living && living.isAlive()) {
			Vec3 to = c.getPosition(partial).add(0, scale * 1.3, 0);
			Vec3 body = living.getPosition(partial);
			float out = Mth.sin(taking * Mth.PI) * 0.45f;
			Vec3 soul = body.add(to.subtract(body).scale(out * 0.5)).add(0, 0.4 + out, 0);
			float alpha = 0.75f * Mth.sin(taking * Mth.PI) * fade;
			if (living instanceof Player player) {
				person(pose, cam, soul, Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot), -15, 0, living.getScale(), skinOf(player), slimOf(player), FALLING, age, alpha,
						null, null);
			} else {
				beast(pose, cam, soul.add(0, living.getBbHeight() * 0.5, 0), Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot), -20, alpha, age, living.getType(), 1.0f);
			}
		}
	}

	/**
	 * The Impermanence's regalia, in its body's own space (the model posed): its tall hat on its head, the White's tongue and staff, drawn in
	 * flat colour as cloth and paper are, faintly lit by its own light.
	 */
	private static void regalia(PoseStack pose, PlayerModel<?> model, boolean white, float age, float alpha) {
		if (alpha <= 0.02f) return;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		pose.pushPose();
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		// Its hat: tall, tapering, banded at its brim.
		pose.pushPose();
		model.head.translateAndRotate(pose);
		Matrix4f m = pose.last().pose();
		float[] hat = white ? HAT_WHITE : HAT_BLACK, band = white ? BAND_RED : BAND_TEAL;
		taperedBox(buffer, m, -4.6f, -7.9f, -4.6f, 4.6f, -10.5f, 4.6f, 1, hat, alpha * 0.95f);
		taperedBox(buffer, m, -4.6f, -10.5f, -4.6f, 4.6f, -24f, 4.6f, 0.55f, hat, alpha * 0.95f);
		taperedBox(buffer, m, -4.75f, -9.4f, -4.75f, 4.75f, -10.6f, 4.75f, 1, band, alpha);
		if (white) {
			// Its long red tongue, hanging from its mouth down onto its chest.
			float sway = Mth.sin(age * 0.09f) * 0.5f;
			quad(buffer, m, -0.9f + sway * 0.2f, -2.2f, -4.15f, 0.9f + sway * 0.2f, -2.2f, -4.15f, 0.7f + sway, 7.5f, -4.9f, -0.7f + sway, 7.5f, -4.9f, TONGUE, alpha);
		}
		pose.popPose();
		if (white) {
			// Its staff, held in its right hand, running on past it, hung with long paper streamers that stir.
			pose.pushPose();
			model.rightArm.translateAndRotate(pose);
			Matrix4f a = pose.last().pose();
			taperedBox(buffer, a, -1.6f, 6f, -0.6f, -0.4f, 40f, 0.6f, 1, STAFF, alpha);
			for (int i = 0; i < 6; i++) {
				float y = 28 + i * 2f, flutter = Mth.sin(age * 0.25f + i * 1.3f) * 2.5f, flutter2 = Mth.sin(age * 0.21f + i * 1.7f + 1) * 2.5f;
				float off = (i % 2 == 0 ? 1 : -1) * 0.9f;
				quad(buffer, a, -1.0f, y, off, -1.0f, y + 1.6f, off, -1.0f + flutter, y + 1.6f - 9, off * 3 + flutter2, -1.0f + flutter, y - 9, off * 3 + flutter2, PAPER, alpha * 0.9f);
			}
			pose.popPose();
		}
		pose.popPose();
		BufferUploader.drawWithShader(buffer.end());
	}

	/** A box from (x0, y0, z0) to (x1, y1, z1) in a part's pixels, its far end (y1) narrowed to {@code taper} of its width, flat-shaded. */
	private static void taperedBox(BufferBuilder buffer, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float taper, float[] rgb, float alpha) {
		float s = 1 / 16f;
		float cx = (x0 + x1) / 2, cz = (z0 + z1) / 2;
		float tx0 = cx + (x0 - cx) * taper, tx1 = cx + (x1 - cx) * taper, tz0 = cz + (z0 - cz) * taper, tz1 = cz + (z1 - cz) * taper;
		float[][] bottom = {{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}}, top = {{tx0, tz0}, {tx1, tz0}, {tx1, tz1}, {tx0, tz1}};
		for (int i = 0; i < 4; i++) {
			int j = (i + 1) % 4;
			float shade = 0.72f + 0.12f * i;
			float[] c = {rgb[0] * shade, rgb[1] * shade, rgb[2] * shade};
			vertex(buffer, m, bottom[i][0] * s, y0 * s, bottom[i][1] * s, c, alpha);
			vertex(buffer, m, bottom[j][0] * s, y0 * s, bottom[j][1] * s, c, alpha);
			vertex(buffer, m, top[j][0] * s, y1 * s, top[j][1] * s, c, alpha);
			vertex(buffer, m, top[i][0] * s, y1 * s, top[i][1] * s, c, alpha);
		}
		for (int i = 3; i >= 0; i--) vertex(buffer, m, top[i][0] * s, y1 * s, top[i][1] * s, rgb, alpha);
	}

	/** A quad through four points in a part's pixels. */
	private static void quad(BufferBuilder buffer, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3,
			float z3, float[] rgb, float alpha) {
		float s = 1 / 16f;
		vertex(buffer, m, x0 * s, y0 * s, z0 * s, rgb, alpha);
		vertex(buffer, m, x1 * s, y1 * s, z1 * s, rgb, alpha);
		vertex(buffer, m, x2 * s, y2 * s, z2 * s, rgb, alpha);
		vertex(buffer, m, x3 * s, y3 * s, z3 * s, rgb, alpha);
	}

	private static void vertex(BufferBuilder buffer, Matrix4f m, float x, float y, float z, float[] rgb, float alpha) {
		buffer.vertex(m, x, y, z).color(rgb[0], rgb[1], rgb[2], Mth.clamp(alpha, 0, 1)).endVertex();
	}
}
