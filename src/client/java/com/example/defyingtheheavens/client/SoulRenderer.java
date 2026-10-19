package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.client.mixin.HumanoidModelInvoker;
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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.example.defyingtheheavens.GhostCultivatorNpc;
import com.example.defyingtheheavens.HungryGhost;
import com.example.defyingtheheavens.SoulBodyEntity;
import com.example.defyingtheheavens.TwoPaths;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	/** The mist a hazily seen soul trails (violet), and the faint trail a ghost leaves (cyan). */
	private static final float[] SOUL_MIST = {0.55f, 0.3f, 1.0f};
	private static final float[] DEAD_MIST = {0.3f, 0.9f, 0.95f};
	/** How far over the ground one of the dead floats as it sits in meditation (blocks). */
	private static final float SEATED_HOVER = 0.25f;

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
				context.register(DefyingTheHeavens.id("soul"), DefaultVertexFormat.NEW_ENTITY, shader -> soulShader = shader));
	}

	/** The soul shader, or null if a shader pack has replaced the core shaders (see RiverSouls, which draws the dead with it). */
	static ShaderInstance shader() {
		return soulShader;
	}

	/** Anything to draw this frame. */
	public static boolean hasWork() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && (ClientSoul.isOut() || ClientSoul.isInside() || !ClientSoul.seenSouls().isEmpty() || ClientGhosts.any() || anyGhostly(mc));
	}

	/** How far off the Netherworld's dead (ghost cultivators, Hungry Ghosts) are drawn. */
	private static final double GHOSTLY_RANGE = 96;

	/** Any of the Netherworld's dead in this world (they are drawn here, not by their entity renderers). */
	private static boolean anyGhostly(Minecraft mc) {
		for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
			if (entity instanceof com.example.defyingtheheavens.Ghostly || inLight(entity)) return true;
		}
		return false;
	}

	/** A body its soul has left that sits on in the dead's light (its owner set out from a ghostly form: SoulBodyEntity#look). */
	private static boolean inLight(net.minecraft.world.entity.Entity entity) {
		return entity instanceof SoulBodyEntity body && body.look() != TwoPaths.Look.HUMAN;
	}

	/** Whether the player {@code id} wears the slim model (as the player list has it; broad if unknown). */
	private static boolean slim(java.util.UUID id) {
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.client.multiplayer.PlayerInfo info = id == null || mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
		return info != null && "slim".equals(info.getModelName());
	}

	/** The souls and tethers, into the sense layer (its blending and depth already set, see SenseOverlay#onWorldEnd). */
	public static void render(WorldRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		float partial = context.tickDelta();
		Camera camera = context.camera();
		List<Player> souls = new ArrayList<>();
		// Souls in a person's body (Possession): that body is veiled in violet for those who perceive souls, its tether running to it.
		List<Player> veiled = new ArrayList<>();
		if (ClientSoul.isOut()) souls.add(mc.player);
		for (java.util.Map.Entry<Integer, ClientSoul.Seen> entry : ClientSoul.seenSouls().entrySet()) {
			if (!(mc.level.getEntity(entry.getKey()) instanceof Player soul) || soul == mc.player) continue;
			(entry.getValue().inside() ? veiled : souls).add(soul);
		}
		for (Player soul : souls) {
			// One's own soul is seen from inside in first person; another's inside the body it possesses is hidden by it.
			if (soul == mc.player && !camera.isDetached() || soul.isPassenger()) continue;
			float clarity = ClientSoul.clarity(soul);
			if (clarity > 0) drawSoul(context, soul, clarity, partial);
		}
		for (Player body : veiled) {
			// Not round the view of one who watches through it (its held owner, or a spectator in it).
			if (body.isInvisible() || body == camera.getEntity() && !camera.isDetached()) continue;
			float clarity = ClientSoul.clarity(body);
			if (clarity > 0) drawVeil(context, body, clarity, partial);
		}
		// The dead: in the Netherworld seen by everyone, whole, in cyan, with their names; outside it (a Ghost King come up to the
		// living world) only as clearly as souls are seen (the server sends them only to those who perceive souls), but whole to
		// another of the dead; not while dispersed (hidden), nor one's own in first person. Drawn so by their look (TwoPaths), and
		// seen alike: the living in a Ghost King's form too, as clearly as the dead are; a living soul out of its body is drawn as a
		// soul above.
		boolean whole = com.example.defyingtheheavens.ModDimensions.isNetherworld(mc.level.dimension())
				|| com.example.defyingtheheavens.GhostSystem.isGhost(mc.player);
		for (Player ghost : mc.level.players()) {
			if (!ClientGhosts.drawnAsLight(ghost) || ghost.isInvisible() || ghost == mc.player && !camera.isDetached()) continue;
			boolean dead = com.example.defyingtheheavens.GhostSystem.isGhost(ghost);
			// Its soul out of its form (the dead's too, SoulProjection): drawn as a soul above, its form sitting where it left it (below).
			if (com.example.defyingtheheavens.SoulProjection.isSoul(ghost)) continue;
			float clarity = whole ? 1.0f : ClientSoul.clarity(ghost);
			if (clarity <= 0) continue;
			drawSoul(context, ghost, clarity, partial, true, ClientGhosts.crowned(ghost));
			if (ghost != mc.player) drawName(context, ghost, partial);
		}
		// The Netherworld's own dead, ghost cultivators and Hungry Ghosts, likewise (their plates are their renderers').
		Vec3 eye = camera.getPosition();
		for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof com.example.defyingtheheavens.Ghostly) || !(entity instanceof LivingEntity body)) continue;
			if (body.isInvisible() || body.isRemoved() || body.distanceToSqr(eye) > GHOSTLY_RANGE * GHOSTLY_RANGE) continue;
			drawDead(context, body, partial);
		}
		// A body its soul left from a ghostly form sits on in that light, a Ghost King's crowned, seen as its owner was: by all in the
		// Netherworld and by the dead, elsewhere as clearly as a soul is seen; its own owner always.
		for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
			if (!inLight(entity) || !(entity instanceof SoulBodyEntity body) || body.isInvisible() || body.isRemoved()) continue;
			boolean mine = mc.player.getUUID().equals(body.getOwnerId());
			float clarity = whole || mine ? 1.0f : ClientSoul.clarity(body);
			if (clarity <= 0) continue;
			drawBody(context, body, InnerBodyRenderer.skinOf(body.getOwnerId()), slim(body.getOwnerId()), clarity, partial, true, Look.DEAD, null,
					body.look() == TwoPaths.Look.KING);
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
		// In a person's body, one's own tether still runs back to one's own body (while that sits in this realm), and others' to theirs.
		if (ClientSoul.isInside() && ClientSoul.bodyHere()) {
			Vec3[] ends = ClientSoul.tetherEnds(mc.player, null, partial);
			drawTether(context, ends[0], ends[1], ClientSoul.tether(), mc.player.getId());
		}
		if (ClientSoul.seesTethers()) {
			for (Player body : veiled) {
				ClientSoul.Seen seen = ClientSoul.seenSoul(body.getId());
				if (seen == null || seen.tether() < 0) continue;
				Vec3[] ends = ClientSoul.tetherEnds(body, seen, partial);
				drawTether(context, ends[0], ends[1], seen.tether(), body.getId());
			}
		}
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}

	// --- The soul ---

	/** A projected soul, in a soul's violet. */
	private static void drawSoul(WorldRenderContext context, Player soul, float clarity, float partial) {
		drawSoul(context, soul, clarity, partial, false, false);
	}

	/** One drawn as the dead (see GhostSystem, TwoPaths#look), in their cyan rather than a soul's violet, a faint trail drifting from it. */
	private static void drawSoul(WorldRenderContext context, Player soul, float clarity, float partial, boolean dead, boolean crowned) {
		boolean slimModel = soul instanceof AbstractClientPlayer client && "slim".equals(client.getModelName());
		// A player wears a man's head (a beast's, should a demon race give one: its BeastHead here); a Ghost King, a crown on it.
		drawBody(context, soul, ((AbstractClientPlayer) soul).getSkinTextureLocation(), slimModel, clarity, partial, dead, Look.SOUL, null,
				dead && crowned);
	}

	/** How strongly a possessed body's veil shows at its clearest (it breathes a little about this), and how far it stands off the body. */
	private static final float VEIL_ALPHA = 1.0f, VEIL_SIZE = 1.1f;

	/**
	 * A person's body with another's soul in it (Possession), as those who perceive souls see it: a violet veil of the soul's own
	 * light laid over the body, breathing slowly, posed and moving as the body does (it is drawn by its renderer as ever, under the
	 * veil). As clear as a soul would be to them (ClientSoul#clarity: only under Qi Sense, or the Ghost Eye).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void drawVeil(WorldRenderContext context, Player body, float clarity, float partial) {
		if (soulShader == null || !(body instanceof AbstractClientPlayer client)) return; // a shader pack: only the tether shows
		PlayerModel model = model("slim".equals(client.getModelName()));
		if (model == null) return;
		float age = body.tickCount + partial;
		float bodyYaw = Mth.rotLerp(partial, body.yBodyRotO, body.yBodyRot);
		float headYaw = Mth.rotLerp(partial, body.yHeadRotO, body.yHeadRot);
		float pitch = Mth.lerp(partial, body.xRotO, body.getXRot());
		Vec3 cam = context.camera().getPosition();
		Vec3 at = body.getPosition(partial);
		PoseStack pose = context.matrixStack();
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y - (body.isCrouching() ? 0.125 : 0), at.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
		model.attackTime = body.getAttackAnim(partial);
		model.riding = body.isPassenger();
		model.young = false;
		model.crouching = body.isCrouching();
		model.rightLeg.resetPose();
		model.leftLeg.resetPose();
		model.setupAnim(body, body.walkAnimation.position(partial), Math.min(1, body.walkAnimation.speed(partial)), age,
				Mth.wrapDegrees(headYaw - bodyYaw), pitch);
		ResourceLocation skin = client.getSkinTextureLocation();
		RenderSystem.setShaderTexture(0, skin);
		RenderSystem.setShader(() -> soulShader);
		float veil = clarity * VEIL_ALPHA * (0.85f + 0.15f * Mth.sin(age * 0.12f));
		// Light over the body, not cover: the soul's violet edge light about it, a fainter haze beyond, and a wash of its violet on it.
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		drawModel(pose, model, skin, null, 0, 1, 1, veil, VEIL_SIZE);
		drawModel(pose, model, skin, null, 0, 1, 1, veil * 0.45f, VEIL_SIZE * 1.1f);
		drawModel(pose, model, skin, null, 1, 1, 1, veil * 0.5f, 1.02f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		pose.popPose();
	}

	/**
	 * One of the Netherworld's dead (a ghost cultivator, a Hungry Ghost), in its own skin, as the dead are drawn; Ox-Head and
	 * Horse-Face with their beasts' heads (BeastHeadModels).
	 */
	static void drawDead(WorldRenderContext context, LivingEntity body, float partial) {
		boolean hungry = body instanceof HungryGhost;
		ResourceLocation skin = hungry ? GhostlyRenderers.HUNGRY_GHOST_SKIN
				: body instanceof GhostCultivatorNpc ghost ? GhostlyRenderers.skin(ghost) : GhostlyRenderers.HUNGRY_GHOST_SKIN;
		BeastHeadModels.Head beast = body instanceof GhostCultivatorNpc ghost ? BeastHeadModels.get(ghost.getRole().beastHead()) : null;
		boolean king = body instanceof GhostCultivatorNpc ghost && ghost.getRole() == GhostCultivatorNpc.Role.GHOST_KING;
		drawBody(context, body, skin, false, 1.0f, partial, true, hungry ? Look.HUNGRY : Look.DEAD, beast, king);
	}

	/**
	 * How a body is posed and turned: a projected soul or a player's ghost turns whole to where it looks and leans into its
	 * flight; one of the Netherworld's dead turns by its body, its head free; a Hungry Ghost reaches out before it, grasping.
	 */
	private enum Look { SOUL, DEAD, HUNGRY }

	/**
	 * {@code beast}: a beast's head worn in place of the man's (see BeastHeadModels), or null. {@code king}: a Ghost King, its
	 * light deeper, edged with violet (the shader's king switch), a crown of bone and soul fire on its head ({@link #drawCrown}).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void drawBody(WorldRenderContext context, LivingEntity soul, ResourceLocation skin, boolean slimModel, float clarity, float partial,
								 boolean dead, Look look, BeastHeadModels.Head beast, boolean king) {
		Minecraft mc = Minecraft.getInstance();
		PlayerModel model = model(slimModel);
		if (model == null) return;
		float age = soul.tickCount + partial;
		float headYaw = Mth.rotLerp(partial, soul.yHeadRotO, soul.yHeadRot);
		// A soul turns whole to where it has been looking, nods as it has, and leans into its flight (up to 35 degrees at full
		// pace), each steadied (see #drift); one of the Netherworld's dead turns by its body.
		Drift drift = look == Look.SOUL ? drift(soul) : null;
		float yaw = drift != null ? Mth.rotLerp(partial, drift.yawBefore, drift.yawNow) : Mth.rotLerp(partial, soul.yBodyRotO, soul.yBodyRot);
		float pitch = drift != null ? Mth.lerp(partial, drift.pitchBefore, drift.pitchNow) : Mth.lerp(partial, soul.xRotO, soul.getXRot());
		Vec3 cam = context.camera().getPosition();
		Vec3 at = soul.getPosition(partial);
		float lean = drift != null ? Mth.lerp(partial, drift.leanBefore, drift.leanNow) : 0;
		// One of the dead in meditation sits cross-legged (HumanoidModelMixin), floating a little over the ground.
		boolean seated = dead && LotusPose.meditating(soul);

		PoseStack pose = context.matrixStack();
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y + Mth.sin(age * 0.08f) * 0.06f + (seated ? SEATED_HOVER : 0), at.z - cam.z);
		pose.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
		pose.mulPose(Axis.XP.rotationDegrees(-lean));
		// Its size (the Gate's keepers stand three times a man's height: GhostCultivatorNpc#getScale), from its feet.
		float scale = soul.getScale();
		if (scale != 1.0f) pose.scale(scale, scale, scale);

		// The dead strike as the living do, the arm swung (a projected soul's never is).
		model.attackTime = look == Look.SOUL && !dead ? 0 : soul.getAttackAnim(partial);
		model.riding = false;
		model.young = false;
		model.crouching = false;
		// The model is shared by every figure drawn: the legs' spread (which setupAnim leaves alone and the drifting pose narrows)
		// back to the model's own, so one sitting in meditation doesn't take it from whoever was drawn before.
		model.rightLeg.resetPose();
		model.leftLeg.resetPose();
		model.setupAnim(soul, 0, 0, age, look == Look.SOUL ? 0 : Mth.wrapDegrees(headYaw - yaw), pitch);
		if (!seated) {
			ghostPose(model, age);
			if (look == Look.HUNGRY) reachingPose(model, age);
			if (model.attackTime > 0) swingPose(model, soul, age);
		}
		// A beast's head in place of the man's: posed as his would be, his head and hat hidden while the body is drawn.
		if (beast != null) beast.follow(model.head);
		model.head.visible = model.hat.visible = beast == null;

		boolean shader = soulShader != null;
		RenderSystem.setShaderTexture(0, skin);
		if (shader) {
			RenderSystem.setShader(() -> soulShader);
			// The body, then its glow: the same, a little larger, added on (the shader draws only its edge light).
			float green = dead ? 0 : 1; // the shader's switch to the dead's cyan
			float blue = king ? 0 : 1; // and to a Ghost King's deeper light, edged with violet
			drawModel(pose, model, skin, beast, 1, green, blue, clarity, 1.0f);
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
					GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
			drawModel(pose, model, skin, beast, 0, green, blue, clarity, 1.07f);
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
			float red = king ? 0.55f : dead ? 0.35f : 0.6f, green = king ? 0.6f : dead ? 0.95f : 0.4f, alpha = dead ? 0.55f : 0.15f + 0.4f * clarity;
			BufferBuilder buffer = Tesselator.getInstance().getBuilder();
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
			model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, 1.0f, alpha);
			BufferUploader.drawWithShader(buffer.end());
			if (beast != null) {
				RenderSystem.setShaderTexture(0, beast.texture());
				buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
				beast.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, 1.0f, alpha);
				BufferUploader.drawWithShader(buffer.end());
			}
			pose.popPose();
			mc.gameRenderer.lightTexture().turnOffLightLayer();
			mc.gameRenderer.overlayTexture().teardownOverlayColor();
		}
		model.head.visible = model.hat.visible = true;
		// A Ghost King: its crown on its head.
		if (king) drawCrown(pose, model, age, clarity);
		// Bound by a Ghost-Quelling Talisman: the talisman on its brow.
		if (ClientQuelled.is(soul)) drawTalisman(pose, model, beast, age);
		pose.popPose();
		if (dead) drawMist(context, soul, 0.6f, partial, DEAD_MIST, scale);
		else if (clarity < 1) drawMist(context, soul, clarity, partial, SOUL_MIST, scale);
	}

	/** Plain white, for a ghost's arm drawn as flat light under a shader pack (see #drawGhostArm). */
	private static final ResourceLocation WHITE = new ResourceLocation("minecraft", "textures/misc/white.png");

	/**
	 * A ghost's own arm in first person (PlayerRendererMixin, the model already posed for it): the dead's cyan light with its
	 * glow, as everyone else sees the ghost. Without the shader, the skin tinted cyan and faded. Under a shader pack, which draws
	 * the hand in its own pass and keeps nothing the soul shader draws there (ShaderPacks): the dead's cyan, through a glowing
	 * render type the pack knows, at full light, flat (the skin tinted came out the skin's own greens and browns, not the light
	 * the dead are drawn as).
	 */
	public static void drawGhostArm(PoseStack pose, MultiBufferSource buffers, ResourceLocation skin, ModelPart arm, ModelPart sleeve, int light) {
		arm.xRot = 0;
		sleeve.xRot = 0;
		if (com.example.defyingtheheavens.client.compat.ShaderPacks.inUse()) {
			VertexConsumer glow = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
			arm.render(pose, glow, 0xF000F0, OverlayTexture.NO_OVERLAY, 0.3f, 0.85f, 1.0f, 0.6f);
			return;
		}
		if (soulShader == null) {
			VertexConsumer faded = buffers.getBuffer(RenderType.entityTranslucent(skin));
			arm.render(pose, faded, light, OverlayTexture.NO_OVERLAY, 0.35f, 0.95f, 1.0f, 0.55f);
			sleeve.render(pose, faded, light, OverlayTexture.NO_OVERLAY, 0.35f, 0.95f, 1.0f, 0.55f);
			return;
		}
		// As the sense layer draws the dead (SenseOverlay): blended, tested but not written, both faces.
		RenderSystem.enableBlend();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.setShader(() -> soulShader);
		RenderSystem.setShaderTexture(0, skin);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		drawArm(pose, arm, sleeve, 1, 1.0f);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		drawArm(pose, arm, sleeve, 0, 1.07f);
		RenderSystem.depthMask(true);
		RenderSystem.enableCull();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	/** The arm and its sleeve, {@code red} 1 the body pass and 0 the halo (see drawModel), {@code size} times as large. */
	private static void drawArm(PoseStack pose, ModelPart arm, ModelPart sleeve, float red, float size) {
		arm.xScale = arm.yScale = arm.zScale = size;
		sleeve.xScale = sleeve.yScale = sleeve.zScale = size;
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		arm.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, 0, 1, 1);
		sleeve.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, 0, 1, 1);
		BufferUploader.drawWithShader(buffer.end());
		arm.xScale = arm.yScale = arm.zScale = 1;
		sleeve.xScale = sleeve.yScale = sleeve.zScale = 1;
	}

	/**
	 * A ghost (or one in a Ghost King's form) as the Ghost Eye sees it behind walls (GhostEye, which sets the depth test to draw only
	 * what is hidden), crowned as it is drawn.
	 */
	static void drawGhostSeen(WorldRenderContext context, Player ghost, float partial) {
		drawSoul(context, ghost, 1.0f, partial, true, ClientGhosts.crowned(ghost));
	}

	/**
	 * A ghost's name over its head, in cyan, as a living player's would be; a Ghost King's (as drawn) over its crown, its title above.
	 * Leaves the sense layer's state as it found it (SenseOverlay#layerState).
	 */
	private static void drawName(WorldRenderContext context, Player ghost, float partial) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 cam = context.camera().getPosition();
		Vec3 at = ghost.getPosition(partial);
		boolean king = ClientGhosts.crowned(ghost);
		PoseStack pose = context.matrixStack();
		pose.pushPose();
		pose.translate(at.x - cam.x, at.y - cam.y + ghost.getBbHeight() + 0.5 + (king ? GhostKingCrown.HEIGHT : 0), at.z - cam.z);
		pose.mulPose(context.camera().rotation());
		pose.scale(-0.025f, -0.025f, 0.025f);
		net.minecraft.network.chat.Component name = ghost.getDisplayName();
		net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		mc.font.drawInBatch(name, -mc.font.width(name) / 2.0f, 0, 0xFF8AF4F0, false, pose.last().pose(), buffers,
				net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0x40000000, 0xF000F0);
		if (king) {
			net.minecraft.network.chat.Component title = net.minecraft.network.chat.Component.translatable(com.example.defyingtheheavens.ModLang.NPC_TITLE_BRACKETS,
					com.example.defyingtheheavens.NpcTitle.GHOST_KING.getDisplayName());
			mc.font.drawInBatch(title, -mc.font.width(title) / 2.0f, -10, 0xFF000000 | com.example.defyingtheheavens.NpcTitle.GHOST_KING.getColor(), false,
					pose.last().pose(), buffers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0x40000000, 0xF000F0);
		}
		buffers.endBatch();
		// The text's batch turns blending and the depth test off as it finishes: the layer's own state back for what follows.
		SenseOverlay.layerState();
		pose.popPose();
	}

	/**
	 * The model at the pose's origin (the soul's feet, facing set), scaled by {@code size} about its middle. {@code red} 1 is the
	 * body pass, 0 the halo pass; {@code green} 0 is the dead's cyan, 1 a soul's violet; {@code blue} 0 a Ghost King's deeper
	 * light (of the dead's); the clarity rides in the alpha. Full bright: the soul is its own light. A beast's head, if one is
	 * worn, after the body, in its own texture.
	 */
	private static void drawModel(PoseStack pose, PlayerModel<?> model, ResourceLocation skin, BeastHeadModels.Head beast, float red, float green,
								  float blue, float clarity, float size) {
		pose.pushPose();
		pose.translate(0, 0.95f, 0);
		pose.scale(size, size, size);
		pose.translate(0, -0.95f, 0);
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		model.renderToBuffer(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, blue, clarity);
		BufferUploader.drawWithShader(buffer.end());
		if (beast != null) {
			RenderSystem.setShaderTexture(0, beast.texture());
			buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
			beast.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, red, green, blue, clarity);
			BufferUploader.drawWithShader(buffer.end());
			RenderSystem.setShaderTexture(0, skin);
		}
		pose.popPose();
	}

	/**
	 * How many ticks of its gaze a soul's figure turns (and nods) by, how far it eases each tick toward their mean, and the most
	 * it turns in a tick (degrees): an even count, so a gaze flicking one way and back with each update evens out to a still
	 * figure rather than spinning it, and one flicking unevenly (as the network brings it) to a slow sway; a real turn settles in
	 * about half a second, the figure some 20 degrees behind the gaze while it turns.
	 */
	private static final int FACING_TICKS = 10;
	private static final float FACING_EASE = 0.4f;
	private static final float FACING_RATE = 15.0f;
	/**
	 * How many ticks' steps a soul's lean is taken over, how far it eases each tick toward the lean that pace gives, the most it
	 * changes in a tick (degrees), and the pace (blocks a tick) under which it stands upright: long and soft enough to ride out a
	 * stall of a few hundred milliseconds in another player's steps, settling in about half a second.
	 */
	private static final int LEAN_TICKS = 12;
	private static final float LEAN_EASE = 0.3f;
	private static final float LEAN_RATE = 2.5f;
	private static final float LEAN_FLOOR = 0.02f;
	/** Each soul's last gazes and steps, its facing, nod and lean, by entity id (see #drift). */
	private static final Map<Integer, Drift> DRIFTS = new HashMap<>();

	/** A soul's figure as drawn: last tick's facing, nod and lean ({@code before}) and this tick's ({@code now}), in degrees. */
	private static final class Drift {
		final float[] yaws = new float[FACING_TICKS];
		final float[] pitches = new float[FACING_TICKS];
		final float[] steps = new float[LEAN_TICKS];
		float yawBefore, yawNow, pitchBefore, pitchNow, leanBefore, leanNow;
		int tick;
		long seen;
	}

	/**
	 * How {@code soul}'s figure turns, nods and leans this tick, taken once a tick and drawn eased between ticks: facing and nod
	 * eased toward the mean of its gaze over its last {@link #FACING_TICKS} ticks; the lean from its pace over its last
	 * {@link #LEAN_TICKS}.
	 * Another player reaches this client only in the server's updates, every other tick, bunched by the network (a step, none, a
	 * double one), and whatever their gaze or pace does between updates comes as a jump. Taken from each tick alone, the lean
	 * snapped between upright and tipped over every tick or two (the user's recording of 2026-10-14), and the facing, the whole
	 * figure turning with the head, snapped some 60 degrees aside and back every 0.1 s (the recording of 2026-10-18, a Ghost King
	 * hosting); one's own gaze and steps are even.
	 */
	private static Drift drift(LivingEntity soul) {
		long time = soul.level().getGameTime();
		double dx = soul.getX() - soul.xo, dz = soul.getZ() - soul.zo;
		float step = (float) Math.sqrt(dx * dx + dz * dz);
		float yaw = soul.yHeadRot, pitch = soul.getXRot();
		Drift drift = DRIFTS.get(soul.getId());
		if (drift == null || soul.tickCount < drift.tick || soul.tickCount - drift.tick > 20) {
			// New (or another entity under its id, or long unseen): as it looks and moves now.
			if (drift == null) DRIFTS.put(soul.getId(), drift = new Drift());
			Arrays.fill(drift.yaws, yaw);
			Arrays.fill(drift.pitches, pitch);
			Arrays.fill(drift.steps, step);
			drift.yawBefore = drift.yawNow = yaw;
			drift.pitchBefore = drift.pitchNow = pitch;
			drift.leanBefore = drift.leanNow = leanAt(step);
			drift.tick = soul.tickCount;
		} else if (soul.tickCount != drift.tick) {
			drift.yaws[Math.floorMod(soul.tickCount, FACING_TICKS)] = yaw;
			drift.pitches[Math.floorMod(soul.tickCount, FACING_TICKS)] = pitch;
			drift.steps[Math.floorMod(soul.tickCount, LEAN_TICKS)] = step;
			// The gaze's mean, followed tick by tick from the oldest (so a quick spin is taken the way it went, not the short way).
			int oldest = Math.floorMod(soul.tickCount + 1, FACING_TICKS);
			float along = 0, turn = 0, nod = 0, pace = 0;
			for (int k = 0; k < FACING_TICKS; k++) {
				int i = (oldest + k) % FACING_TICKS;
				if (k > 0) along += Mth.wrapDegrees(drift.yaws[i] - drift.yaws[(i + FACING_TICKS - 1) % FACING_TICKS]);
				turn += along / FACING_TICKS;
				nod += drift.pitches[i] / FACING_TICKS;
			}
			for (float s : drift.steps) pace += s / LEAN_TICKS;
			float mean = drift.yaws[oldest] + turn;
			drift.yawBefore = drift.yawNow;
			drift.yawNow = Mth.wrapDegrees(drift.yawNow + Mth.clamp(Mth.wrapDegrees(mean - drift.yawNow) * FACING_EASE, -FACING_RATE, FACING_RATE));
			drift.pitchBefore = drift.pitchNow;
			drift.pitchNow += Mth.clamp((nod - drift.pitchNow) * FACING_EASE, -FACING_RATE, FACING_RATE);
			drift.leanBefore = drift.leanNow;
			drift.leanNow += Mth.clamp((leanAt(pace) - drift.leanNow) * LEAN_EASE, -LEAN_RATE, LEAN_RATE);
			drift.tick = soul.tickCount;
		}
		drift.seen = time;
		if (DRIFTS.size() > 64) DRIFTS.values().removeIf(d -> time - d.seen > 200);
		return drift;
	}

	/** The lean a pace gives (degrees): none while drifting slowly, up to 35 at full pace. */
	private static float leanAt(float pace) {
		return Mth.clamp((pace - LEAN_FLOOR) * 60, 0, 35);
	}

	/** Soul fire's own flame (its particle's picture), burning on a Ghost King's crown. */
	private static final ResourceLocation SOUL_FLAME = new ResourceLocation("minecraft", "textures/particle/soul_fire_flame.png");

	/**
	 * A Ghost King's crown (GhostKingCrown) on its head, turning and nodding with it: bone in its own colours, not the dead's
	 * light (faint as the king is to one who sees it only hazily), and on its tallest points soul fire, flickering, two crossed
	 * flames each as a fire burns. Drawn with depth, so its points hide one another; the sense layer's state put back after.
	 */
	private static void drawCrown(PoseStack pose, PlayerModel<?> model, float age, float clarity) {
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.client.model.geom.ModelPart crown = GhostKingCrown.part();
		crown.copyFrom(model.head);
		pose.pushPose();
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		boolean solid = clarity >= 1;
		RenderSystem.depthMask(solid);
		RenderSystem.enableCull();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
		RenderSystem.setShaderTexture(0, GhostKingCrown.TEXTURE);
		mc.gameRenderer.lightTexture().turnOnLightLayer();
		mc.gameRenderer.overlayTexture().setupOverlayColor();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
		crown.render(pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, Math.max(0.15f, clarity));
		BufferUploader.drawWithShader(buffer.end());
		mc.gameRenderer.lightTexture().turnOffLightLayer();
		mc.gameRenderer.overlayTexture().teardownOverlayColor();

		// The soul fire on its points: light, added on, no depth written.
		crown.translateAndRotate(pose);
		Matrix4f matrix = pose.last().pose();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
				GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShaderTexture(0, SOUL_FLAME);
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		for (int i = 0; i < GhostKingCrown.FLAMES.length; i++) {
			float[] tip = GhostKingCrown.FLAMES[i];
			float flicker = 0.8f + 0.2f * Mth.sin(age * 0.6f + i * 1.7f) * Mth.sin(age * 0.23f + i);
			float half = (i == 0 ? 1.4f : 1.0f) * flicker / 16.0f, tall = (i == 0 ? 3.6f : 2.6f) * flicker / 16.0f;
			float x = tip[0] / 16.0f, y = tip[1] / 16.0f, z = tip[2] / 16.0f, a = 0.9f * Math.max(0.2f, clarity);
			// Two flames crossed at the tip, rising (up is -y in the model's own space).
			for (int k = 0; k < 2; k++) {
				float dx = k == 0 ? half : 0, dz = k == 0 ? 0 : half;
				buffer.vertex(matrix, x - dx, y, z - dz).uv(0, 1).color(0.6f, 0.95f, 1.0f, a).endVertex();
				buffer.vertex(matrix, x + dx, y, z + dz).uv(1, 1).color(0.6f, 0.95f, 1.0f, a).endVertex();
				buffer.vertex(matrix, x + dx, y - tall, z + dz).uv(1, 0).color(0.6f, 0.95f, 1.0f, a).endVertex();
				buffer.vertex(matrix, x - dx, y - tall, z - dz).uv(0, 0).color(0.6f, 0.95f, 1.0f, a).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		pose.popPose();
	}

	/** The paper a Ghost-Quelling Talisman is painted on (its item picture). */
	private static final ResourceLocation TALISMAN = DefyingTheHeavens.id("textures/item/ghost_quelling_talisman.png");

	/**
	 * A Ghost-Quelling Talisman sealed to the brow of one it binds (ClientQuelled): the paper in its own colours, not the dead's
	 * light, hanging from the brow down the face (a man's, or a beast's: BeastHeadModels#Brow), lifted a little off it and
	 * stirring.
	 */
	private static void drawTalisman(PoseStack pose, PlayerModel<?> model, BeastHeadModels.Head beast, float age) {
		pose.pushPose();
		pose.scale(-1.0f, -1.0f, 1.0f);
		pose.scale(0.9375f, 0.9375f, 0.9375f);
		pose.translate(0.0f, -1.501f, 0.0f);
		float length;
		if (beast != null) {
			beast.toBrow(pose);
			length = beast.browLength();
		} else {
			BeastHeadModels.toHumanBrow(pose, model.head);
			length = BeastHeadModels.HUMAN_BROW.length();
		}
		pose.mulPose(Axis.XP.rotationDegrees(-6.0f + Mth.sin(age * 0.2f) * 3.0f));
		Matrix4f matrix = pose.last().pose();
		float halfWidth = 2.0f / 16.0f, hang = length / 16.0f;
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, TALISMAN);
		RenderSystem.disableCull();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		// The paper of the picture, framed: its columns 4 to 11 and rows 1 to 14 (of 16).
		buffer.vertex(matrix, -halfWidth, 0, 0).uv(4 / 16.0f, 1 / 16.0f).endVertex();
		buffer.vertex(matrix, -halfWidth, hang, 0).uv(4 / 16.0f, 15 / 16.0f).endVertex();
		buffer.vertex(matrix, halfWidth, hang, 0).uv(12 / 16.0f, 15 / 16.0f).endVertex();
		buffer.vertex(matrix, halfWidth, 0, 0).uv(12 / 16.0f, 1 / 16.0f).endVertex();
		BufferUploader.drawWithShader(buffer.end());
		// Both faces still, as the sense layer draws (SenseOverlay#layerState): turning culling back on here left every soul and
		// ghost drawn after a bound one in the frame with only its front faces.
		pose.popPose();
	}

	/**
	 * To those who see it only hazily, the soul trails a drifting violet mist: soft puffs around it, more and brighter the
	 * hazier it is. Drawn here rather than as particles, which Qi Sense would grey. {@code scale}: the body's size (a keeper's
	 * mist reaches as high and as wide as it stands).
	 */
	private static void drawMist(WorldRenderContext context, LivingEntity soul, float clarity, float partial, float[] rgb, float scale) {
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
			float reach = (0.25f + 0.45f * Mth.frac(seed * 7.13f)) * scale;
			float height = (0.15f + 1.7f * Mth.frac(seed * 3.71f + age * 0.004f)) * scale;
			float size = (0.28f + 0.3f * Mth.frac(seed * 5.3f)) * scale;
			float px = (float) (at.x - cam.x) + Mth.cos(angle) * reach;
			float py = (float) (at.y - cam.y) + height;
			float pz = (float) (at.z - cam.z) + Mth.sin(angle) * reach;
			for (int s = 0; s < segments; s++) {
				float a0 = s * Mth.TWO_PI / segments, a1 = (s + 1) * Mth.TWO_PI / segments;
				buffer.vertex(matrix, px, py, pz).color(rgb[0], rgb[1], rgb[2], alpha).endVertex();
				buffer.vertex(matrix, px + (left.x() * Mth.cos(a0) + up.x() * Mth.sin(a0)) * size, py + (left.y() * Mth.cos(a0) + up.y() * Mth.sin(a0)) * size,
						pz + (left.z() * Mth.cos(a0) + up.z() * Mth.sin(a0)) * size).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
				buffer.vertex(matrix, px + (left.x() * Mth.cos(a1) + up.x() * Mth.sin(a1)) * size, py + (left.y() * Mth.cos(a1) + up.y() * Mth.sin(a1)) * size,
						pz + (left.z() * Mth.cos(a1) + up.z() * Mth.sin(a1)) * size).color(rgb[0], rgb[1], rgb[2], 0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(buffer.end());
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
	}

	/** A Hungry Ghost's grasping: both arms reaching out before it, clawing a little as it drifts. */
	private static void reachingPose(PlayerModel<?> model, float age) {
		float claw = Mth.sin(age * 0.15f) * 0.12f;
		model.rightArm.xRot = -1.45f + claw;
		model.leftArm.xRot = -1.45f - claw;
		model.rightArm.zRot = 0.08f;
		model.leftArm.zRot = -0.08f;
		model.head.xRot += 0.3f; // hunched, staring down at what it hungers for
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.hat.copyFrom(model.head);
	}

	/**
	 * A blow (a fist, or what it holds) over the drifting pose: the arm swung as the living swing theirs (HumanoidModel's own
	 * swing, which the drifting pose took away), the outer layers following it.
	 */
	private static void swingPose(PlayerModel<?> model, LivingEntity soul, float age) {
		((HumanoidModelInvoker) (Object) model).dth$setupAttackAnimation(soul, age);
		model.leftSleeve.copyFrom(model.leftArm);
		model.rightSleeve.copyFrom(model.rightArm);
		model.jacket.copyFrom(model.body);
	}

	/** Upright and drifting: legs together and trailing behind like a ghost's, arms loose at its sides, gently swaying. */
	static void ghostPose(PlayerModel<?> model, float age) {
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

	static PlayerModel<Player> model(boolean slimModel) {
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
