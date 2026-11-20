package com.example.defyingtheheavens.client;

import com.example.defyingtheheavens.ArtifactGrade;
import com.example.defyingtheheavens.CircuitBoard;
import com.example.defyingtheheavens.DefyingTheHeavens;
import com.example.defyingtheheavens.PuppetBlueprint;
import com.example.defyingtheheavens.PuppetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * A puppet (PuppetEntity): its GeckoLib model (geo/puppet, made with the spells' shapes: tools/spell-art, Puppets), the humanoid's or the
 * bee's, of its frame's grade's matter (wood, bronze, dark iron and jade, silver-white, gold), its light whatever the light about it (its
 * `_glowmask`), a bee's wings seen through. Awake, its head turns to what it looks at. For one with Qi Sense on, its circuits glow on the
 * panel on its back ({@link Circuits}), its board as it stands now, cuts and all.
 */
public class PuppetRenderer extends GeoEntityRenderer<PuppetEntity> {
	private final boolean bee;

	/** A body of flesh's look (phase 2b): the player model a cultivator wears. */
	private final net.minecraft.client.model.PlayerModel<net.minecraft.world.entity.LivingEntity> flesh;

	public PuppetRenderer(EntityRendererProvider.Context context, boolean bee) {
		super(context, new Model(bee));
		this.bee = bee;
		this.shadowRadius = bee ? 0.4f : 0.45f;
		addRenderLayer(new AutoGlowingGeoLayer<>(this));
		addRenderLayer(new Circuits(this, bee));
		flesh = new net.minecraft.client.model.PlayerModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
	}

	/**
	 * A puppet made of a cultivator's body (Puppetry phase 2b: PuppetBodies) is drawn in the dead's likeness; one whose copy of a player's
	 * soul is whole (phase 2d) wears that player's look; any other as its frame.
	 */
	@Override
	public void render(PuppetEntity puppet, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		com.example.defyingtheheavens.PuppetBodies.Dead look = bee ? null : puppet.bodyLook();
		java.util.UUID wears = bee || look != null ? null : puppet.soulLook();
		if (look == null && wears == null) {
			super.render(puppet, yaw, partialTick, pose, buffers, light);
			return;
		}
		if (look != null) {
			com.example.defyingtheheavens.Bloodline race = look.race().isEmpty() ? null : com.example.defyingtheheavens.Bloodline.byId(look.race());
			ClientBloodlines.Look demon = race == null ? null : new ClientBloodlines.Look(race, look.raceVariant(), look.tails());
			ResourceLocation skin = demon != null ? ClientBloodlines.skinOf(demon) : CultivatorNpcRenderer.skinOf(look.skin());
			renderFlesh(puppet, skin, demon, race, partialTick, pose, buffers, light);
		} else {
			renderFlesh(puppet, playerSkin(wears), null, null, partialTick, pose, buffers, light);
		}
	}

	/** {@code player}'s skin, as this client knows it (from the player list while they are in the world), else Minecraft's default for them. */
	private static ResourceLocation playerSkin(java.util.UUID player) {
		net.minecraft.client.multiplayer.ClientPacketListener connection = net.minecraft.client.Minecraft.getInstance().getConnection();
		net.minecraft.client.multiplayer.PlayerInfo info = connection == null ? null : connection.getPlayerInfo(player);
		return info != null ? info.getSkinLocation() : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(player);
	}

	/**
	 * The body standing and moving as a cultivator does in {@code skin} (a demon's race's skin and beast's head as CorpseRenderer lays them),
	 * a little pale; its head turned to its look, its arms swinging as it walks and strikes; slumped while still.
	 */
	private void renderFlesh(PuppetEntity puppet, ResourceLocation skin, @Nullable ClientBloodlines.Look demon,
			com.example.defyingtheheavens.@Nullable Bloodline race, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		pose.pushPose();
		float bodyYaw = Mth.rotLerp(partialTick, puppet.yBodyRotO, puppet.yBodyRot), headYaw = Mth.rotLerp(partialTick, puppet.yHeadRotO, puppet.yHeadRot);
		float pitch = Mth.lerp(partialTick, puppet.xRotO, puppet.getXRot());
		pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - bodyYaw));
		pose.scale(-0.9375f, -0.9375f, 0.9375f);
		pose.translate(0, -1.501f, 0);
		float swing = puppet.walkAnimation.position(partialTick), amount = Math.min(1, puppet.walkAnimation.speed(partialTick));
		flesh.attackTime = puppet.getAttackAnim(partialTick);
		flesh.riding = false;
		flesh.young = false;
		flesh.crouching = false;
		flesh.prepareMobModel(puppet, swing, amount, partialTick);
		flesh.setupAnim(puppet, swing, amount, puppet.tickCount + partialTick, Mth.wrapDegrees(headYaw - bodyYaw), pitch);
		if (puppet.isStill()) {
			// Stilled (inert, frozen, stalled): its head hung, its arms down.
			flesh.head.xRot = 0.6f;
			flesh.hat.xRot = 0.6f;
			flesh.rightArm.xRot = flesh.leftArm.xRot = 0.1f;
		}
		BeastHeadModels.Head beast = demon == null || race == null ? null : BeastHeadModels.get(race.head());
		flesh.head.visible = beast == null;
		flesh.hat.visible = beast == null;
		int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(puppet, 0);
		flesh.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, overlay, 0.86f, 0.86f, 0.9f, 1.0f);
		if (beast != null) {
			beast.on(puppet, demon);
			beast.follow(flesh);
			beast.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(beast.texture())), light, overlay, 0.86f, 0.86f, 0.9f, 1.0f);
		}
		pose.popPose();
	}

	@Override
	public RenderType getRenderType(PuppetEntity puppet, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
		return bee ? RenderType.entityTranslucent(texture) : super.getRenderType(puppet, texture, buffers, partialTick);
	}

	/** Its model, texture and animations by its shape and grade (geo/puppet/humanoid_spirit ...), its head turned to its look. */
	static final class Model extends GeoModel<PuppetEntity> {
		private final ResourceLocation[] geo = new ResourceLocation[5], texture = new ResourceLocation[5], animations = new ResourceLocation[5];

		Model(boolean bee) {
			for (ArtifactGrade grade : ArtifactGrade.values()) {
				String id = (bee ? "bee_" : "humanoid_") + grade.getId();
				geo[grade.ordinal()] = DefyingTheHeavens.id("geo/puppet/" + id + ".geo.json");
				texture[grade.ordinal()] = DefyingTheHeavens.id("textures/entity/puppet/" + id + ".png");
				animations[grade.ordinal()] = DefyingTheHeavens.id("animations/puppet/" + id + ".animation.json");
			}
		}

		@Override
		public ResourceLocation getModelResource(PuppetEntity puppet) { return geo[puppet.frameGrade().ordinal()]; }

		@Override
		public ResourceLocation getTextureResource(PuppetEntity puppet) { return texture[puppet.frameGrade().ordinal()]; }

		@Override
		public ResourceLocation getAnimationResource(PuppetEntity puppet) { return animations[puppet.frameGrade().ordinal()]; }

		/** The head bone the look last turned, and by how much (radians about x and y), to be taken off before the next frame's. */
		private @Nullable CoreGeoBone looked;
		private float lookX, lookY;
		private boolean customised;

		/**
		 * Last frame's look taken off the head before GeckoLib sets anything (as BeastModels does): a bone no playing animation keys keeps
		 * what was set last frame, so a look added to it piled up frame on frame and the head spun every way (the user's video, 2026-11-17).
		 * When GeckoLib draws the same frame again and sets nothing, the look goes back on as it was.
		 */
		@Override
		public void handleAnimations(PuppetEntity puppet, long instanceId, AnimationState<PuppetEntity> state) {
			CoreGeoBone last = looked;
			float lastX = lookX, lastY = lookY;
			if (last != null) {
				last.setRotX(last.getRotX() - lastX);
				last.setRotY(last.getRotY() - lastY);
			}
			looked = null;
			lookX = lookY = 0;
			customised = false;
			super.handleAnimations(puppet, instanceId, state);
			if (customised || last == null) return;
			last.setRotX(last.getRotX() + lastX);
			last.setRotY(last.getRotY() + lastY);
			looked = last;
			lookX = lastX;
			lookY = lastY;
		}

		@Override
		public void setCustomAnimations(PuppetEntity puppet, long instanceId, AnimationState<PuppetEntity> state) {
			customised = true;
			if (puppet.isStill()) return;
			CoreGeoBone head = getAnimationProcessor().getBone("head");
			EntityModelData look = state.getData(DataTickets.ENTITY_MODEL_DATA);
			if (head == null || look == null) return;
			// Wrapped: the head's and the body's turns can be a whole turn apart, read as the wrong side.
			float x = Mth.clamp(look.headPitch(), -30, 30) * Mth.DEG_TO_RAD, y = Mth.clamp(Mth.wrapDegrees(look.netHeadYaw()), -50, 50) * Mth.DEG_TO_RAD;
			head.setRotX(head.getRotX() + x);
			head.setRotY(head.getRotY() + y);
			looked = head;
			lookX = x;
			lookY = y;
		}
	}

	/**
	 * Its circuits on the panel on its back (the humanoid's, its torso's: tools/spell-art Puppets, back face at z 2.9 px; the bee's on its
	 * abdomen's back, top face at y 9.35 px), for one with Qi Sense on.
	 */
	static final class Circuits extends GeoRenderLayer<PuppetEntity> {
		private final boolean bee;

		Circuits(PuppetRenderer renderer, boolean bee) {
			super(renderer);
			this.bee = bee;
		}

		@Override
		public void renderForBone(PoseStack pose, PuppetEntity puppet, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
				float partialTick, int light, int overlay) {
			if (!bone.getName().equals(bee ? "abdomen" : "body") || !QiSenseClientHandler.isActive()) return;
			byte[] strokes = puppet.shownStrokes();
			int size = CircuitBoard.sizeFor(puppet.frameGrade().ordinal());
			if (strokes.length < size * size) return;
			float px = 1 / 16f;
			Vec3 corner, across, down, normal;
			if (bee) {
				// Its abdomen's back, seen from above: the board's top toward its head (-z).
				double s = 4.6 * px;
				corner = new Vec3(-s / 2, 9.42 * px, 4.9 * px - s / 2);
				across = new Vec3(s, 0, 0);
				down = new Vec3(0, 0, s);
				normal = new Vec3(0, 1, 0);
			} else {
				// Its torso's back panel, seen from behind: the board's top up.
				double s = 6.2 * px;
				corner = new Vec3(-s / 2, 19.6 * px + s / 2, 2.96 * px);
				across = new Vec3(s, 0, 0);
				down = new Vec3(0, -s, 0);
				normal = new Vec3(0, 0, 1);
			}
			float glow = 0.75f + 0.25f * Mth.sin((puppet.tickCount + partialTick) * 0.15f);
			CircuitGlow.draw(QiSenseTreasures.glow(buffers), pose.last().pose(), strokes, size, bee ? PuppetBlueprint.BEE : PuppetBlueprint.HUMANOID,
					corner, across, down, normal, glow);
		}
	}
}
